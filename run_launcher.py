import os
import sys
import ctypes
from ctypes import wintypes
import webview
from client_manager import SpaceRustClientManager

# Initialize Win32 APIs with explicit 64-bit argument/return types for smooth zero-lag native window dragging
user32 = ctypes.windll.user32
user32.ReleaseCapture.restype = wintypes.BOOL
user32.SendMessageW.argtypes = [wintypes.HWND, wintypes.UINT, wintypes.WPARAM, wintypes.LPARAM]
user32.SendMessageW.restype = wintypes.LPARAM
user32.PostMessageW.argtypes = [wintypes.HWND, wintypes.UINT, wintypes.WPARAM, wintypes.LPARAM]
user32.PostMessageW.restype = wintypes.BOOL

WM_NCLBUTTONDOWN = 0x00A1
HTCAPTION = 2
WM_SYSCOMMAND = 0x0112
SC_DRAGMOVE = 0xF012

class LauncherApi:
    def __init__(self):
        self.window = None
        self.manager = SpaceRustClientManager()

    def set_window(self, window):
        self.window = window

    def minimize_window(self):
        if self.window:
            self.window.minimize()

    def close_window(self):
        if self.window:
            self.window.destroy()

    def drag_window(self):
        if self.window and hasattr(self.window, 'native') and self.window.native:
            try:
                hwnd_val = int(self.window.native.Handle.ToInt64())
                hwnd = wintypes.HWND(hwnd_val)
                user32.ReleaseCapture()
                user32.SendMessageW(hwnd, WM_NCLBUTTONDOWN, HTCAPTION, 0)
                return True
            except Exception as e:
                print(f"[Drag Error SendMessage] {e}")
                try:
                    hwnd_val = int(self.window.native.Handle.ToInt64())
                    hwnd = wintypes.HWND(hwnd_val)
                    user32.ReleaseCapture()
                    user32.PostMessageW(hwnd, WM_SYSCOMMAND, SC_DRAGMOVE, 0)
                    return True
                except Exception as e2:
                    print(f"[Drag Error PostMessage] {e2}")
        return False

    def open_game_folder(self):
        try:
            target = self.manager.game_dir
            os.makedirs(target, exist_ok=True)
            os.startfile(target)
            return {"status": "ok", "path": target}
        except Exception as e:
            print(f"[Error opening folder] {e}")
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

    def report_progress(self, stage, percent):
        if self.window:
            try:
                safe_stage = stage.replace("'", "\\'").replace('"', '\\"')
                self.window.evaluate_js(f"window.updateLaunchProgress && window.updateLaunchProgress('{safe_stage}', {percent})")
            except Exception:
                pass

    def launch_client(self, ram_mb=None, nickname=None):
        try:
            res = self.manager.launch(nickname=nickname, ram_mb=ram_mb, progress_callback=self.report_progress)
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
