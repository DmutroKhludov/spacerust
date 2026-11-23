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
SWP_ASYNCWINDOWPOS = 0x4000
SWP_FLAGS = SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_ASYNCWINDOWPOS
SWP_RESIZE_FLAGS = SWP_NOZORDER | SWP_NOACTIVATE | SWP_ASYNCWINDOWPOS
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
        self._window = None
        self._manager = SpaceRustClientManager()
        self._is_dragging = False
        self._is_resizing = False
        self._is_maximized = False
        self._closing = False
        self._restore_rect = None

    def _set_window(self, window):
        self._window = window

    def _on_closing(self):
        self._closing = True

    def _get_hwnd(self):
        if self._closing or not self._window or not hasattr(self._window, 'native') or not self._window.native:
            return None
        try:
            hwnd = wintypes.HWND(int(self._window.native.Handle.ToInt64()))
            return hwnd if user32.IsWindow(hwnd) else None
        except Exception:
            return None

    def _get_scale(self):
        try:
            scale = float(getattr(self._window.native, '_scale', 1.0))
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
            self._window.x = int(round(rect.left / scale))
            self._window.y = int(round(rect.top / scale))
            self._window.width = int(round((rect.right - rect.left) / scale))
            self._window.height = int(round((rect.bottom - rect.top) / scale))
        except Exception:
            pass

    def _safe_eval_js(self, script):
        if self._closing or not self._window:
            return
        try:
            self._window.evaluate_js(script)
        except Exception:
            pass

    def _notify_window_state(self, maximized, width, height):
        if self._closing or not self._window:
            return
        max_js = 'true' if maximized else 'false'
        script = f"window.onWindowStateChange && window.onWindowStateChange({max_js}, {int(width)}, {int(height)})"
        import threading
        threading.Thread(target=self._safe_eval_js, args=(script,), daemon=True).start()

    def get_window_state(self):
        hwnd = self._get_hwnd()
        if not hwnd:
            return {"status": "ok", "maximized": self._is_maximized, "width": DEFAULT_WIN_W, "height": DEFAULT_WIN_H}
        rect = RECT()
        user32.GetWindowRect(hwnd, ctypes.byref(rect))
        scale = self._get_scale()
        w = int(round((rect.right - rect.left) / scale))
        h = int(round((rect.bottom - rect.top) / scale))
        return {"status": "ok", "maximized": self._is_maximized, "width": w, "height": h}

    def minimize_window(self):
        if self._window and not self._closing:
            self._window.minimize()

    def close_window(self):
        self._closing = True
        if self._window:
            self._window.destroy()

    def _apply_rect_and_notify(self, hwnd, left, top, width, height, maximized, save_size=False, notify=True):
        user32.SetWindowPos(hwnd, 0, int(left), int(top), int(width), int(height), SWP_RESIZE_FLAGS)
        scale = self._get_scale()
        log_w = int(round(int(width) / scale))
        log_h = int(round(int(height) / scale))
        self._sync_pywebview_geometry(RECT(int(left), int(top), int(left) + int(width), int(top) + int(height)))
        self._is_maximized = bool(maximized)
        updates = {'window_maximized': self._is_maximized}
        if save_size and not self._is_maximized:
            updates['window_width'] = log_w
            updates['window_height'] = log_h
        self._manager._save_config(updates)
        if notify:
            self._notify_window_state(self._is_maximized, log_w, log_h)
        return {"status": "ok", "maximized": self._is_maximized, "width": log_w, "height": log_h}

    def toggle_maximize_window(self):
        hwnd = self._get_hwnd()
        if not hwnd:
            return {"status": "error"}
        work, _ = self._get_monitor_rects(hwnd)
        if not self._is_maximized:
            cur = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(cur))
            self._restore_rect = (cur.left, cur.top, cur.right - cur.left, cur.bottom - cur.top)
            return self._apply_rect_and_notify(
                hwnd,
                work.left,
                work.top,
                work.right - work.left,
                work.bottom - work.top,
                maximized=True,
                save_size=False,
                notify=False,
            )
        else:
            scale = self._get_scale()
            if self._restore_rect:
                rl, rt, rw, rh = self._restore_rect
            else:
                rw = int(DEFAULT_WIN_W * scale)
                rh = int(DEFAULT_WIN_H * scale)
                rl = work.left + max(0, (work.right - work.left - rw) // 2)
                rt = work.top + max(0, (work.bottom - work.top - rh) // 2)
            rw = max(int(MIN_WIN_W * scale), min(rw, work.right - work.left))
            rh = max(int(MIN_WIN_H * scale), min(rh, work.bottom - work.top))
            rl = max(work.left, min(rl, work.right - rw))
            rt = max(work.top, min(rt, work.bottom - rh))
            return self._apply_rect_and_notify(hwnd, rl, rt, rw, rh, maximized=False, save_size=True, notify=False)

    def set_window_preset(self, preset):
        hwnd = self._get_hwnd()
        if not hwnd:
            return {"status": "error"}
        mode = str(preset or '').strip().lower()
        work, _ = self._get_monitor_rects(hwnd)
        if mode == 'fullscreen':
            if self._is_maximized:
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
        if self._is_maximized:
            new_left = work.left + max(0, (work.right - work.left - phys_w) // 2)
            new_top = work.top + max(0, (work.bottom - work.top - phys_h) // 2)
        else:
            cx = (cur.left + cur.right) // 2
            cy = (cur.top + cur.bottom) // 2
            new_left = cx - phys_w // 2
            new_top = cy - phys_h // 2
            new_left = max(work.left, min(new_left, work.right - phys_w))
            new_top = max(work.top, min(new_top, work.bottom - phys_h))

        self._restore_rect = (new_left, new_top, phys_w, phys_h)
        return self._apply_rect_and_notify(hwnd, new_left, new_top, phys_w, phys_h, maximized=False, save_size=True, notify=False)

    def drag_window(self):
        hwnd = self._get_hwnd()
        if not hwnd:
            return False
        if self._is_dragging or self._is_resizing:
            return True

        vkey = 0x02 if user32.GetSystemMetrics(23) else 0x01
        if not (user32.GetAsyncKeyState(vkey) & 0x8000):
            return True

        self._is_dragging = True
        try:
            pt = POINT()
            user32.GetCursorPos(ctypes.byref(pt))
            start_x, start_y = pt.x, pt.y
            rect = RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))
            offset_x = pt.x - rect.left
            offset_y = pt.y - rect.top

            restored_on_drag = False
            last_pos = (rect.left, rect.top)
            while not self._closing and user32.IsWindow(hwnd) and (user32.GetAsyncKeyState(vkey) & 0x8000):
                user32.GetCursorPos(ctypes.byref(pt))
                if self._is_maximized and not restored_on_drag:
                    if abs(pt.x - start_x) < 6 and abs(pt.y - start_y) < 6:
                        time.sleep(0.008)
                        continue
                    work, _ = self._get_monitor_rects(hwnd)
                    scale = self._get_scale()
                    rw = self._restore_rect[2] if self._restore_rect else int(DEFAULT_WIN_W * scale)
                    rh = self._restore_rect[3] if self._restore_rect else int(DEFAULT_WIN_H * scale)
                    rw = max(int(MIN_WIN_W * scale), min(rw, work.right - work.left))
                    rh = max(int(MIN_WIN_H * scale), min(rh, work.bottom - work.top))
                    ratio = max(0.1, min(0.9, (pt.x - work.left) / max(1, work.right - work.left)))
                    offset_x = int(rw * ratio)
                    offset_y = min(max(12, pt.y - work.top), 42)
                    new_left = pt.x - offset_x
                    new_top = pt.y - offset_y
                    user32.SetWindowPos(hwnd, 0, new_left, new_top, rw, rh, SWP_RESIZE_FLAGS)
                    last_pos = (new_left, new_top)
                    self._is_maximized = False
                    restored_on_drag = True
                    self._manager._save_config({'window_maximized': False})
                    self._notify_window_state(False, int(round(rw / scale)), int(round(rh / scale)))
                else:
                    new_x = pt.x - offset_x
                    new_y = pt.y - offset_y
                    if (new_x, new_y) != last_pos:
                        user32.SetWindowPos(hwnd, 0, new_x, new_y, 0, 0, SWP_FLAGS)
                        last_pos = (new_x, new_y)
                time.sleep(0.008)

            if user32.IsWindow(hwnd):
                user32.GetWindowRect(hwnd, ctypes.byref(rect))
                self._sync_pywebview_geometry(rect)
            return True
        except Exception as e:
            print(f"[Drag Error] {e}")
            return False
        finally:
            self._is_dragging = False

    def resize_window(self, edge):
        hwnd = self._get_hwnd()
        if not hwnd or self._is_maximized or self._is_resizing or self._is_dragging:
            return False
        edge = str(edge or '').lower()
        if not edge or not any(c in edge for c in ('n', 's', 'e', 'w')):
            return False

        vkey = 0x02 if user32.GetSystemMetrics(23) else 0x01
        if not (user32.GetAsyncKeyState(vkey) & 0x8000):
            return False

        self._is_resizing = True
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

            cur_pt = POINT()
            last_box = (orig.left, orig.top, orig.right - orig.left, orig.bottom - orig.top)
            while not self._closing and user32.IsWindow(hwnd) and (user32.GetAsyncKeyState(vkey) & 0x8000):
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

                box = (left, top, right - left, bottom - top)
                if box != last_box:
                    user32.SetWindowPos(hwnd, 0, box[0], box[1], box[2], box[3], SWP_RESIZE_FLAGS)
                    last_box = box
                time.sleep(0.008)

            final_rect = RECT(last_box[0], last_box[1], last_box[0] + last_box[2], last_box[1] + last_box[3])
            if user32.IsWindow(hwnd):
                user32.GetWindowRect(hwnd, ctypes.byref(final_rect))
                self._sync_pywebview_geometry(final_rect)
            log_w = int(round((final_rect.right - final_rect.left) / scale))
            log_h = int(round((final_rect.bottom - final_rect.top) / scale))
            self._restore_rect = (
                final_rect.left,
                final_rect.top,
                final_rect.right - final_rect.left,
                final_rect.bottom - final_rect.top,
            )
            self._manager._save_config({
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
            self._is_resizing = False

    def open_game_folder(self):
        try:
            target = self._manager.game_dir
            os.makedirs(target, exist_ok=True)
            os.startfile(target)
            return {"status": "ok", "path": target}
        except Exception as e:
            print(f"[Error opening folder] {e}")
            return {"status": "error", "message": str(e)}

    def get_game_dir(self):
        return {
            "status": "ok",
            "path": self._manager.game_dir,
            "default_path": self._manager.default_game_dir
        }

    def set_game_dir(self, new_path):
        return self._manager.set_game_dir(new_path)

    def reset_game_dir(self):
        return self._manager.reset_game_dir()

    def browse_game_folder(self):
        if not self._window:
            return {"status": "error", "message": "Окно не готово"}
        try:
            initial_dir = self._manager.game_dir if os.path.isdir(self._manager.game_dir) else ""
            result = self._window.create_file_dialog(webview.FileDialog.FOLDER, directory=initial_dir)
            if result and len(result) > 0:
                return self._manager.set_game_dir(result[0])
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
        return {"status": "ok", "lang": self._manager.get_language()}

    def set_language(self, lang):
        return self._manager.set_language(lang)

    def get_playtime(self):
        return {"status": "ok", "seconds": self._manager.get_playtime()}

    def report_playtime(self, total_seconds):
        self._safe_eval_js(f"window.updatePlaytime && window.updatePlaytime({int(total_seconds)})")

    def report_progress(self, stage, percent):
        safe_stage = str(stage).replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", " ")
        self._safe_eval_js(f"window.updateLaunchProgress && window.updateLaunchProgress('{safe_stage}', {percent})")

    def pause_download(self):
        try:
            self._manager.pause_download()
            return True
        except Exception:
            return False

    def resume_download(self):
        try:
            self._manager.resume_download()
            return True
        except Exception:
            return False

    def check_updates(self, force=False):
        try:
            return self._manager.check_github_updates(force=bool(force))
        except Exception as e:
            return {"status": "error", "message": str(e)}

    def mark_updates_seen(self, commit_sha=None):
        try:
            return self._manager.mark_updates_seen(commit_sha)
        except Exception:
            return {"status": "ok"}

    def apply_update(self):
        try:
            res = self._manager.apply_github_update(progress_callback=self.report_progress)
            if isinstance(res, dict) and res.get('status') == 'restart_required':
                import threading
                threading.Timer(0.6, self.close_window).start()
            return res
        except Exception as e:
            return {"status": "error", "message": str(e)}

    def repair_game(self):
        try:
            return self._manager.repair_client(progress_callback=self.report_progress)
        except Exception as e:
            return {"status": "error", "message": str(e)}

    def launch_client(self, ram_mb=None, nickname=None):
        try:
            res = self._manager.launch(
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
    cfg = api._manager._load_config()
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
    api._set_window(window)
    window.events.closing += api._on_closing

    def on_started():
        import json
        import threading
        if start_maximized:
            for _ in range(30):
                if api._get_hwnd():
                    break
                time.sleep(0.05)
            api.toggle_maximize_window()
            st = api.get_window_state()
            api._notify_window_state(st['maximized'], st['width'], st['height'])

        def bg_update_loop():
            time.sleep(1.2)
            while not api._closing:
                try:
                    data = api.check_updates()
                    payload = json.dumps(data, ensure_ascii=False)
                    api._safe_eval_js(f"window.onGithubUpdates && window.onGithubUpdates({payload})")
                except Exception:
                    pass
                for _ in range(300):
                    if api._closing:
                        return
                    time.sleep(1)

        threading.Thread(target=bg_update_loop, daemon=True).start()

    webview.start(on_started, debug=False)

if __name__ == '__main__':
    main()
