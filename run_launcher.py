import os
import sys
import webview
from client_manager import SpaceRustClientManager

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
        width=1140,
        height=740,
        resizable=False,
        frameless=True,
        easy_drag=True,
        background_color='#0c0e12',
        js_api=api
    )
    api.set_window(window)

    webview.start(debug=False)

if __name__ == '__main__':
    main()
