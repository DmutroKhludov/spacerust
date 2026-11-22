import os
import sys
import time
import ctypes
from ctypes import wintypes
import webview
from client_manager import SpaceRustClientManager

# Initialize Win32 APIs for hardware-accelerated, zero-lag window dragging
user32 = ctypes.windll.user32

class POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]

class RECT(ctypes.Structure):
    _fields_ = [("left", ctypes.c_long), ("top", ctypes.c_long), ("right", ctypes.c_long), ("bottom", ctypes.c_long)]

user32.GetCursorPos.argtypes = [ctypes.POINTER(POINT)]
user32.GetCursorPos.restype = wintypes.BOOL
user32.GetWindowRect.argtypes = [wintypes.HWND, ctypes.POINTER(RECT)]
user32.GetWindowRect.restype = wintypes.BOOL
user32.SetWindowPos.argtypes = [wintypes.HWND, wintypes.HWND, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int, wintypes.UINT]
user32.SetWindowPos.restype = wintypes.BOOL
user32.GetAsyncKeyState.argtypes = [ctypes.c_int]
user32.GetAsyncKeyState.restype = wintypes.SHORT
user32.GetSystemMetrics.argtypes = [ctypes.c_int]
user32.GetSystemMetrics.restype = ctypes.c_int

SWP_NOSIZE = 0x0001
SWP_NOZORDER = 0x0004
SWP_NOACTIVATE = 0x0010
SWP_FLAGS = SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE

class LauncherApi:
    def __init__(self):
        self.window = None
        self.manager = SpaceRustClientManager()
        self.is_dragging = False

    def set_window(self, window):
        self.window = window

    def minimize_window(self):
        if self.window:
            self.window.minimize()

    def close_window(self):
        if self.window:
            self.window.destroy()

    def drag_window(self):
        if not self.window or not hasattr(self.window, 'native') or not self.window.native:
            return False
        if self.is_dragging:
            return True

        self.is_dragging = True
        try:
            hwnd = wintypes.HWND(int(self.window.native.Handle.ToInt64()))

            # Record initial physical cursor and window positions
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            rect = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))

            offset_x = pt.x - rect.left
            offset_y = pt.y - rect.top

            # Determine primary mouse button (support swapped mouse buttons)
            vkey = 0x02 if user32.GetSystemMetrics(23) else 0x01

            # High-performance 200 FPS drag loop directly via Win32 SetWindowPos
            # Completely avoids IPC message bottleneck and DPI scaling mismatch
            while user32.GetAsyncKeyState(vkey) & 0x8000:
                user32.GetCursorPos(ctypes.byref(pt))
                new_x = pt.x - offset_x
                new_y = pt.y - offset_y
                user32.SetWindowPos(hwnd, 0, new_x, new_y, 0, 0, SWP_FLAGS)
                time.sleep(0.005)

            # Sync position back to pywebview state when drag is released
            try:
                user32.GetWindowRect(hwnd, ctypes.byref(rect))
                scale = getattr(self.window.native, '_scale', 1.0)
                self.window.x = int(rect.left / scale)
                self.window.y = int(rect.top / scale)
            except Exception:
                pass

            return True
        except Exception as e:
            print(f"[Drag Error] {e}")
            return False
        finally:
            self.is_dragging = False

    def open_game_folder(self):
        try:
            target = self.manager.game_dir
            os.makedirs(target, exist_ok=True)
            os.startfile(target)
            return {"status": "ok", "path": target}
        except Exception as e:
            print(f"[Error opening folder] {e}")
            return {"status": "error", "message": str(e)}

    def get_game_dir(self):
        return {
            "status": "ok",
            "path": self.manager.game_dir,
            "default_path": self.manager.default_game_dir
        }

    def set_game_dir(self, new_path):
        return self.manager.set_game_dir(new_path)

    def reset_game_dir(self):
        return self.manager.reset_game_dir()

    def browse_game_folder(self):
        if not self.window:
            return {"status": "error", "message": "Окно не готово"}
        try:
            initial_dir = self.manager.game_dir if os.path.isdir(self.manager.game_dir) else ""
            result = self.window.create_file_dialog(webview.FileDialog.FOLDER, directory=initial_dir)
            if result and len(result) > 0:
                return self.manager.set_game_dir(result[0])
            return {"status": "cancelled"}
        except Exception as e:
            print(f"[Browse Folder Error] {e}")
            return {"status": "error", "message": str(e)}

    def get_system_username(self):
        try:
            import getpass
            import re
            user = getpass.getuser()
            clean = re.sub(r'[^a-zA-Z0-9_]', '', user)
            if len(clean) >= 3:
                return clean[:16]
        except Exception:
            pass
        return None

    def get_playtime(self):
        return {"status": "ok", "seconds": self.manager.get_playtime()}

    def report_playtime(self, total_seconds):
        if self.window:
            try:
                self.window.evaluate_js(f"window.updatePlaytime && window.updatePlaytime({int(total_seconds)})")
            except Exception:
                pass

    def report_progress(self, stage, percent):
        if self.window:
            try:
                safe_stage = stage.replace("'", "\\'").replace('"', '\\"')
                self.window.evaluate_js(f"window.updateLaunchProgress && window.updateLaunchProgress('{safe_stage}', {percent})")
            except Exception:
                pass

    def pause_download(self):
        try:
            self.manager.pause_download()
            return True
        except Exception:
            return False

    def resume_download(self):
        try:
            self.manager.resume_download()
            return True
        except Exception:
            return False

    def launch_client(self, ram_mb=None, nickname=None):
        try:
            res = self.manager.launch(
                nickname=nickname,
                ram_mb=ram_mb,
                progress_callback=self.report_progress,
                playtime_callback=self.report_playtime
            )
            return res
        except Exception as e:
            print(f"[Launcher Launch Error] {e}")
            import traceback
            traceback.print_exc()
            return {
                "status": "error",
                "message": str(e)
            }

def get_base_dir():
    if getattr(sys, 'frozen', False):
        return getattr(sys, '_MEIPASS', os.path.dirname(sys.executable))
    return os.path.dirname(os.path.abspath(__file__))

def main():
    base_dir = get_base_dir()
    html_file = os.path.join(base_dir, 'index.html')

    api = LauncherApi()
    window = webview.create_window(
        title='SpaceRust Launcher',
        url=f'file:///{html_file}',
        width=1160,
        height=760,
        resizable=False,
        frameless=True,
        easy_drag=False,
        background_color='#0c0e12',
        js_api=api
    )
    api.set_window(window)

    webview.start(debug=False)

if __name__ == '__main__':
    main()
