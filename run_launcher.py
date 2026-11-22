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

class MONITORINFO(ctypes.Structure):
    _fields_ = [
        ("cbSize", wintypes.DWORD),
        ("rcMonitor", RECT),
        ("rcWork", RECT),
        ("dwFlags", wintypes.DWORD),
    ]

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
user32.MonitorFromWindow.argtypes = [wintypes.HWND, wintypes.DWORD]
user32.MonitorFromWindow.restype = wintypes.HANDLE
user32.GetMonitorInfoW.argtypes = [wintypes.HANDLE, ctypes.POINTER(MONITORINFO)]
user32.GetMonitorInfoW.restype = wintypes.BOOL

SWP_NOSIZE = 0x0001
SWP_NOZORDER = 0x0004
SWP_NOACTIVATE = 0x0010
SWP_FLAGS = SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE
SWP_RESIZE_FLAGS = SWP_NOZORDER | SWP_NOACTIVATE
MONITOR_DEFAULTTONEAREST = 2

MIN_WIN_W = 840
MIN_WIN_H = 540
DEFAULT_WIN_W = 1160
DEFAULT_WIN_H = 760
WINDOW_PRESETS = {
    'compact': (920, 600),
    'normal': (1160, 760),
    'large': (1400, 880),
}

class LauncherApi:
    def __init__(self):
        self.window = None
        self.manager = SpaceRustClientManager()
        self.is_dragging = False
        self.is_resizing = False
        self.is_maximized = False
        self.restore_rect = None

    def set_window(self, window):
        self.window = window

    def _get_hwnd(self):
        if not self.window or not hasattr(self.window, 'native') or not self.window.native:
            return None
        return wintypes.HWND(int(self.window.native.Handle.ToInt64()))

    def _get_scale(self):
        try:
            scale = float(getattr(self.window.native, '_scale', 1.0))
            return scale if scale > 0 else 1.0
        except Exception:
            return 1.0

    def _get_monitor_rects(self, hwnd):
        hmon = user32.MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST)
        mi = MONITORINFO()
        mi.cbSize = ctypes.sizeof(MONITORINFO)
        if user32.GetMonitorInfoW(hmon, ctypes.byref(mi)):
            return mi.rcWork, mi.rcMonitor
        r = RECT(0, 0, user32.GetSystemMetrics(0), user32.GetSystemMetrics(1))
        return r, r

    def _sync_pywebview_geometry(self, rect):
        try:
            scale = self._get_scale()
            self.window.x = int(round(rect.left / scale))
            self.window.y = int(round(rect.top / scale))
            self.window.width = int(round((rect.right - rect.left) / scale))
            self.window.height = int(round((rect.bottom - rect.top) / scale))
        except Exception:
            pass

    def _notify_window_state(self, maximized, width, height):
        if self.window:
            try:
                max_js = 'true' if maximized else 'false'
                self.window.evaluate_js(
                    f"window.onWindowStateChange && window.onWindowStateChange({max_js}, {int(width)}, {int(height)})"
                )
            except Exception:
                pass

    def get_window_state(self):
        hwnd = self._get_hwnd()
        if not hwnd:
            return {"status": "ok", "maximized": self.is_maximized, "width": DEFAULT_WIN_W, "height": DEFAULT_WIN_H}
        rect = RECT()
        user32.GetWindowRect(hwnd, ctypes.byref(rect))
        scale = self._get_scale()
        w = int(round((rect.right - rect.left) / scale))
        h = int(round((rect.bottom - rect.top) / scale))
        return {"status": "ok", "maximized": self.is_maximized, "width": w, "height": h}

    def minimize_window(self):
        if self.window:
            self.window.minimize()

    def close_window(self):
        if self.window:
            self.window.destroy()

    def _apply_rect_and_notify(self, hwnd, left, top, width, height, maximized, save_size=False):
        user32.SetWindowPos(hwnd, 0, int(left), int(top), int(width), int(height), SWP_RESIZE_FLAGS)
        rect = RECT()
        user32.GetWindowRect(hwnd, ctypes.byref(rect))
        self._sync_pywebview_geometry(rect)
        scale = self._get_scale()
        log_w = int(round((rect.right - rect.left) / scale))
        log_h = int(round((rect.bottom - rect.top) / scale))
        self.is_maximized = bool(maximized)
        updates = {'window_maximized': self.is_maximized}
        if save_size and not self.is_maximized:
            updates['window_width'] = log_w
            updates['window_height'] = log_h
        self.manager._save_config(updates)
        self._notify_window_state(self.is_maximized, log_w, log_h)
        return {"status": "ok", "maximized": self.is_maximized, "width": log_w, "height": log_h}

    def toggle_maximize_window(self):
        hwnd = self._get_hwnd()
        if not hwnd:
            return {"status": "error"}
        work, _ = self._get_monitor_rects(hwnd)
        if not self.is_maximized:
            cur = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(cur))
            self.restore_rect = (cur.left, cur.top, cur.right - cur.left, cur.bottom - cur.top)
            return self._apply_rect_and_notify(
                hwnd,
                work.left,
                work.top,
                work.right - work.left,
                work.bottom - work.top,
                maximized=True,
                save_size=False,
            )
        else:
            scale = self._get_scale()
            if self.restore_rect:
                rl, rt, rw, rh = self.restore_rect
            else:
                rw = int(DEFAULT_WIN_W * scale)
                rh = int(DEFAULT_WIN_H * scale)
                rl = work.left + max(0, (work.right - work.left - rw) // 2)
                rt = work.top + max(0, (work.bottom - work.top - rh) // 2)
            rw = max(int(MIN_WIN_W * scale), min(rw, work.right - work.left))
            rh = max(int(MIN_WIN_H * scale), min(rh, work.bottom - work.top))
            rl = max(work.left, min(rl, work.right - rw))
            rt = max(work.top, min(rt, work.bottom - rh))
            return self._apply_rect_and_notify(hwnd, rl, rt, rw, rh, maximized=False, save_size=True)

    def set_window_preset(self, preset):
        hwnd = self._get_hwnd()
        if not hwnd:
            return {"status": "error"}
        mode = str(preset or '').strip().lower()
        work, _ = self._get_monitor_rects(hwnd)
        if mode == 'fullscreen':
            if self.is_maximized:
                return self.get_window_state()
            return self.toggle_maximize_window()
        if mode not in WINDOW_PRESETS:
            return {"status": "error", "message": f"Unknown preset: {preset}"}

        scale = self._get_scale()
        target_w, target_h = WINDOW_PRESETS[mode]
        phys_w = min(int(round(target_w * scale)), work.right - work.left)
        phys_h = min(int(round(target_h * scale)), work.bottom - work.top)
        phys_w = max(int(round(MIN_WIN_W * scale)), phys_w)
        phys_h = max(int(round(MIN_WIN_H * scale)), phys_h)

        cur = RECT()
        user32.GetWindowRect(hwnd, ctypes.byref(cur))
        if self.is_maximized:
            new_left = work.left + max(0, (work.right - work.left - phys_w) // 2)
            new_top = work.top + max(0, (work.bottom - work.top - phys_h) // 2)
        else:
            cx = (cur.left + cur.right) // 2
            cy = (cur.top + cur.bottom) // 2
            new_left = cx - phys_w // 2
            new_top = cy - phys_h // 2
            new_left = max(work.left, min(new_left, work.right - phys_w))
            new_top = max(work.top, min(new_top, work.bottom - phys_h))

        self.restore_rect = (new_left, new_top, phys_w, phys_h)
        return self._apply_rect_and_notify(hwnd, new_left, new_top, phys_w, phys_h, maximized=False, save_size=True)

    def drag_window(self):
        hwnd = self._get_hwnd()
        if not hwnd:
            return False
        if self.is_dragging or self.is_resizing:
            return True

        self.is_dragging = True
        try:
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            start_x, start_y = pt.x, pt.y
            rect = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))
            offset_x = pt.x - rect.left
            offset_y = pt.y - rect.top
            vkey = 0x02 if user32.GetSystemMetrics(23) else 0x01

            # Wait slightly before breaking out of maximized state so dblclick does not flicker
            restored_on_drag = False
            while user32.GetAsyncKeyState(vkey) & 0x8000:
                user32.GetCursorPos(ctypes.byref(pt))
                if self.is_maximized and not restored_on_drag:
                    if abs(pt.x - start_x) < 6 and abs(pt.y - start_y) < 6:
                        time.sleep(0.005)
                        continue
                    work, _ = self._get_monitor_rects(hwnd)
                    scale = self._get_scale()
                    rw = self.restore_rect[2] if self.restore_rect else int(DEFAULT_WIN_W * scale)
                    rh = self.restore_rect[3] if self.restore_rect else int(DEFAULT_WIN_H * scale)
                    rw = max(int(MIN_WIN_W * scale), min(rw, work.right - work.left))
                    rh = max(int(MIN_WIN_H * scale), min(rh, work.bottom - work.top))
                    ratio = max(0.1, min(0.9, (pt.x - work.left) / max(1, work.right - work.left)))
                    offset_x = int(rw * ratio)
                    offset_y = min(max(12, pt.y - work.top), 42)
                    new_left = pt.x - offset_x
                    new_top = pt.y - offset_y
                    user32.SetWindowPos(hwnd, 0, new_left, new_top, rw, rh, SWP_RESIZE_FLAGS)
                    self.is_maximized = False
                    restored_on_drag = True
                    self.manager._save_config({'window_maximized': False})
                    self._notify_window_state(False, int(round(rw / scale)), int(round(rh / scale)))
                else:
                    new_x = pt.x - offset_x
                    new_y = pt.y - offset_y
                    user32.SetWindowPos(hwnd, 0, new_x, new_y, 0, 0, SWP_FLAGS)
                time.sleep(0.005)

            user32.GetWindowRect(hwnd, ctypes.byref(rect))
            self._sync_pywebview_geometry(rect)
            return True
        except Exception as e:
            print(f"[Drag Error] {e}")
            return False
        finally:
            self.is_dragging = False

    def resize_window(self, edge):
        hwnd = self._get_hwnd()
        if not hwnd or self.is_maximized or self.is_resizing or self.is_dragging:
            return False
        edge = str(edge or '').lower()
        if not edge or not any(c in edge for c in ('n', 's', 'e', 'w')):
            return False

        self.is_resizing = True
        try:
            start_pt = POINT()
            user32.GetCursorPos(ctypes.byref(start_pt))
            orig = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(orig))
            _, mon = self._get_monitor_rects(hwnd)
            scale = self._get_scale()
            min_w_px = int(round(MIN_WIN_W * scale))
            min_h_px = int(round(MIN_WIN_H * scale))
            max_w_px = max(min_w_px, mon.right - mon.left)
            max_h_px = max(min_h_px, mon.bottom - mon.top)
            vkey = 0x02 if user32.GetSystemMetrics(23) else 0x01

            cur_pt = POINT()
            while user32.GetAsyncKeyState(vkey) & 0x8000:
                user32.GetCursorPos(ctypes.byref(cur_pt))
                dx = cur_pt.x - start_pt.x
                dy = cur_pt.y - start_pt.y

                left, top, right, bottom = orig.left, orig.top, orig.right, orig.bottom
                if 'w' in edge:
                    left = max(orig.right - max_w_px, min(orig.right - min_w_px, orig.left + dx))
                if 'e' in edge:
                    right = min(orig.left + max_w_px, max(orig.left + min_w_px, orig.right + dx))
                if 'n' in edge:
                    top = max(orig.bottom - max_h_px, min(orig.bottom - min_h_px, orig.top + dy))
                if 's' in edge:
                    bottom = min(orig.top + max_h_px, max(orig.top + min_h_px, orig.bottom + dy))

                user32.SetWindowPos(hwnd, 0, left, top, right - left, bottom - top, SWP_RESIZE_FLAGS)
                time.sleep(0.005)

            final_rect = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(final_rect))
            self._sync_pywebview_geometry(final_rect)
            log_w = int(round((final_rect.right - final_rect.left) / scale))
            log_h = int(round((final_rect.bottom - final_rect.top) / scale))
            self.restore_rect = (
                final_rect.left,
                final_rect.top,
                final_rect.right - final_rect.left,
                final_rect.bottom - final_rect.top,
            )
            self.manager._save_config({
                'window_width': log_w,
                'window_height': log_h,
                'window_maximized': False,
            })
            self._notify_window_state(False, log_w, log_h)
            return True
        except Exception as e:
            print(f"[Resize Error] {e}")
            return False
        finally:
            self.is_resizing = False

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

    def get_language(self):
        return {"status": "ok", "lang": self.manager.get_language()}

    def set_language(self, lang):
        return self.manager.set_language(lang)

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
    cfg = api.manager._load_config()
    try:
        init_w = max(MIN_WIN_W, int(cfg.get('window_width') or DEFAULT_WIN_W))
    except Exception:
        init_w = DEFAULT_WIN_W
    try:
        init_h = max(MIN_WIN_H, int(cfg.get('window_height') or DEFAULT_WIN_H))
    except Exception:
        init_h = DEFAULT_WIN_H
    start_maximized = bool(cfg.get('window_maximized', False))

    window = webview.create_window(
        title='SpaceRust Launcher',
        url=f'file:///{html_file}',
        width=init_w,
        height=init_h,
        min_size=(MIN_WIN_W, MIN_WIN_H),
        resizable=True,
        frameless=True,
        easy_drag=False,
        background_color='#0c0e12',
        js_api=api
    )
    api.set_window(window)

    def on_started():
        if start_maximized:
            for _ in range(30):
                if api._get_hwnd():
                    break
                time.sleep(0.05)
            api.toggle_maximize_window()

    webview.start(on_started, debug=False)

if __name__ == '__main__':
    main()
