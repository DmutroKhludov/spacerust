import os
import sys
import shutil
import subprocess
import json
import threading
import traceback
import minecraft_launcher_lib

SUPPORTED_LANGS = ('ru', 'uk', 'en')

BACKEND_MESSAGES = {
    'ru': {
        'mb': 'МБ',
        'mbs': 'МБ/с',
        'kbs': 'КБ/с',
        'paused': 'Пауза ({pct}%)',
        'checking': 'Проверка папки SpaceRust...',
        'ready': 'Клиент готов к запуску!',
        'ready_short': 'Клиент готов!',
        'unpacking_dir': 'Распаковка файлов игры из папки...',
        'init_cache': 'Инициализация файлов из локального кэша...',
        'first_launch': 'Первый запуск: подключение к серверу...',
        'extracting': 'Распаковка файлов игры...',
        'done_launch': 'Установка завершена! Запуск...',
        'dl_failed': 'Не удалось автоматически загрузить файлы игры:\n{err}\n\nПроверьте подключение к интернету.',
        'no_java': 'Java не найдена на вашем компьютере!\n\nДля запуска игры требуется Java 8 (x64).\nУстановите Java с официального сайта: https://www.java.com',
        'launch_err': 'Ошибка запуска игры: {err}',
    },
    'uk': {
        'mb': 'МБ',
        'mbs': 'МБ/с',
        'kbs': 'КБ/с',
        'paused': 'Пауза ({pct}%)',
        'checking': 'Перевірка папки SpaceRust...',
        'ready': 'Клієнт готовий до запуску!',
        'ready_short': 'Клієнт готовий!',
        'unpacking_dir': 'Розпакування файлів гри з папки...',
        'init_cache': 'Ініціалізація файлів із локального кешу...',
        'first_launch': 'Перший запуск: підключення до сервера...',
        'extracting': 'Розпакування файлів гри...',
        'done_launch': 'Встановлення завершено! Запуск...',
        'dl_failed': 'Не вдалося автоматично завантажити файли гри:\n{err}\n\nПеревірте підключення до інтернету.',
        'no_java': 'Java не знайдено на вашому комп’ютері!\n\nДля запуску гри потрібна Java 8 (x64).\nВстановіть Java з офіційного сайту: https://www.java.com',
        'launch_err': 'Помилка запуску гри: {err}',
    },
    'en': {
        'mb': 'MB',
        'mbs': 'MB/s',
        'kbs': 'KB/s',
        'paused': 'Paused ({pct}%)',
        'checking': 'Checking SpaceRust folder...',
        'ready': 'Client is ready to launch!',
        'ready_short': 'Client ready!',
        'unpacking_dir': 'Extracting game files from folder...',
        'init_cache': 'Initializing files from local cache...',
        'first_launch': 'First launch: connecting to server...',
        'extracting': 'Extracting game files...',
        'done_launch': 'Installation complete! Launching...',
        'dl_failed': 'Failed to download game files automatically:\n{err}\n\nCheck your internet connection.',
        'no_java': 'Java was not found on your computer!\n\nJava 8 (x64) is required to launch the game.\nInstall Java from the official site: https://www.java.com',
        'launch_err': 'Game launch error: {err}',
    },
}


class SpaceRustClientManager:
    def __init__(self):
        self.default_game_dir = os.path.expandvars(r'%APPDATA%\.spacerust')
        self.config_dir = os.path.expandvars(r'%LOCALAPPDATA%\SpaceRust')
        self.config_file = os.path.join(self.config_dir, 'launcher_config.json')
        self._config_lock = threading.Lock()
        cfg = self._load_config()
        saved_dir = (cfg.get('game_dir') or '').strip()
        self.game_dir = os.path.abspath(os.path.expandvars(saved_dir)) if saved_dir else self.default_game_dir
        self.playtime_seconds = int(cfg.get('playtime_seconds', 0) or 0)
        saved_lang = str(cfg.get('lang') or 'ru').strip().lower()
        self.lang = saved_lang if saved_lang in SUPPORTED_LANGS else 'ru'
        self.version_id = 'SpaceRust-1.16.5'
        self.log_file = os.path.join(self.game_dir, 'launcher.log')
        self._pause_event = threading.Event()
        self._pause_event.set()
        if getattr(sys, 'frozen', False):
            self.exe_dir = os.path.dirname(sys.executable)
        else:
            self.exe_dir = os.path.dirname(os.path.abspath(__file__))
        self.ensure_directories()

    def _msg(self, key, **kwargs):
        table = BACKEND_MESSAGES.get(self.lang, BACKEND_MESSAGES['ru'])
        text = table.get(key) or BACKEND_MESSAGES['ru'].get(key, key)
        return text.format(**kwargs) if kwargs else text

    def get_language(self):
        return self.lang

    def set_language(self, lang):
        clean = str(lang or '').strip().lower()
        if clean not in SUPPORTED_LANGS:
            return {"status": "error", "message": f"Unsupported language: {lang}"}
        self.lang = clean
        self._save_config({'lang': clean})
        return {"status": "ok", "lang": self.lang}

    def _load_config(self):
        try:
            if os.path.isfile(self.config_file):
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                if isinstance(data, dict):
                    return data
        except Exception:
            pass
        return {}

    def _save_config(self, updates):
        try:
            with self._config_lock:
                data = self._load_config()
                data.update(updates)
                os.makedirs(self.config_dir, exist_ok=True)
                with open(self.config_file, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            self.log(f"Could not save config: {e}")

    def get_playtime(self):
        return int(self.playtime_seconds)

    def add_playtime(self, delta_seconds):
        if delta_seconds <= 0:
            return self.playtime_seconds
        with self._config_lock:
            self.playtime_seconds += int(delta_seconds)
            total = self.playtime_seconds
            try:
                data = self._load_config()
                data['playtime_seconds'] = total
                os.makedirs(self.config_dir, exist_ok=True)
                with open(self.config_file, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
            except Exception as e:
                self.log(f"Could not save playtime: {e}")
        return total

    def _track_playtime(self, proc, playtime_callback=None):
        import time
        last_tick = time.monotonic()
        while proc.poll() is None:
            time.sleep(5)
            now = time.monotonic()
            whole = int(now - last_tick)
            if whole >= 1:
                last_tick += whole
                total = self.add_playtime(whole)
                if playtime_callback:
                    try:
                        playtime_callback(total)
                    except Exception:
                        pass
        whole = int(time.monotonic() - last_tick)
        if whole >= 1:
            total = self.add_playtime(whole)
            if playtime_callback:
                try:
                    playtime_callback(total)
                except Exception:
                    pass

    def set_game_dir(self, new_path):
        if not new_path or not str(new_path).strip():
            return {"status": "error", "message": "Путь к папке не может быть пустым"}
        cleaned = str(new_path).strip().strip('"').strip("'")
        target = os.path.abspath(os.path.expandvars(cleaned))
        try:
            os.makedirs(target, exist_ok=True)
            test_file = os.path.join(target, '.write_test')
            with open(test_file, 'w', encoding='utf-8') as tf:
                tf.write('ok')
            os.remove(test_file)
        except Exception as e:
            return {"status": "error", "message": f"Недоступная папка для записи: {e}"}

        self.game_dir = target
        self.log_file = os.path.join(self.game_dir, 'launcher.log')
        self.ensure_directories()
        self._save_config({'game_dir': target})
        self.log(f"Game directory changed to: {self.game_dir}")
        return {"status": "ok", "path": self.game_dir}

    def reset_game_dir(self):
        return self.set_game_dir(self.default_game_dir)

    def pause_download(self):
        self._pause_event.clear()

    def resume_download(self):
        self._pause_event.set()

    def log(self, text):
        msg = f"[SpaceRust] {text}"
        print(msg)
        try:
            with open(self.log_file, 'a', encoding='utf-8') as f:
                f.write(msg + '\n')
        except Exception:
            pass

    def ensure_directories(self):
        for sub in ['assets', 'libraries', 'versions', 'mods', 'resourcepacks', 'config', 'runtime']:
            os.makedirs(os.path.join(self.game_dir, sub), exist_ok=True)

    def find_java(self):
        # 1. Check portable JRE next to executable / launcher
        portable_candidates = [
            os.path.join(self.exe_dir, 'jre', 'bin', 'javaw.exe'),
            os.path.join(self.exe_dir, 'jre', 'bin', 'java.exe'),
            os.path.join(self.exe_dir, 'java', 'bin', 'javaw.exe'),
            os.path.join(self.exe_dir, '..', 'jre', 'bin', 'javaw.exe'),
            os.path.join(self.game_dir, 'runtime', 'jre', 'bin', 'javaw.exe'),
            os.path.join(self.game_dir, 'runtime', 'bin', 'javaw.exe'),
        ]
        for p in portable_candidates:
            if os.path.isfile(p):
                self.log(f"Found portable Java: {p}")
                return p

        # 2. Check installed Java paths
        system_candidates = [
            r'C:\Program Files\Java\jre1.8.0_351\bin\javaw.exe',
            r'C:\Program Files\Java\jre1.8.0_351\bin\java.exe',
            r'C:\Program Files\Java\jdk-17\bin\javaw.exe',
            r'C:\Program Files\Java\jdk-16\bin\javaw.exe',
            r'C:\Program Files\Java\jdk-23\bin\javaw.exe',
            r'C:\Program Files (x86)\Java\jre1.8.0_351\bin\javaw.exe',
            r'C:\Program Files (x86)\Common Files\Oracle\Java\javapath\javaw.exe'
        ]
        for p in system_candidates:
            if os.path.isfile(p):
                self.log(f"Found system Java: {p}")
                return p

        # 3. Check environment PATH
        which_javaw = shutil.which('javaw')
        if which_javaw and os.path.isfile(which_javaw):
            self.log(f"Found PATH Java: {which_javaw}")
            return which_javaw

        which_java = shutil.which('java')
        if which_java and os.path.isfile(which_java):
            self.log(f"Found PATH Java: {which_java}")
            return which_java

        return None

    def _file_sha256(self, path):
        import hashlib
        h = hashlib.sha256()
        with open(path, 'rb') as f:
            for chunk in iter(lambda: f.read(65536), b''):
                h.update(chunk)
        return h.hexdigest()

    def sync_latest_mod(self):
        """Always ensure the latest spacerust-mod-1.0.0.jar is installed in game_dir/mods"""
        if getattr(sys, 'frozen', False):
            base_dir = getattr(sys, '_MEIPASS', self.exe_dir)
        else:
            base_dir = self.exe_dir

        dst_mods = os.path.join(self.game_dir, 'mods')
        os.makedirs(dst_mods, exist_ok=True)

        candidates = [
            os.path.join(base_dir, 'spacerust-mod-1.0.0.jar'),
            os.path.join(self.exe_dir, 'data', 'mods', 'spacerust-mod-1.0.0.jar'),
            os.path.join(base_dir, 'spacerust-mod', 'build', 'libs', 'spacerust-mod-1.0.0.jar'),
            os.path.join(self.exe_dir, 'spacerust-mod', 'build', 'libs', 'spacerust-mod-1.0.0.jar')
        ]

        cfg = self._load_config()
        target = os.path.join(dst_mods, 'spacerust-mod-1.0.0.jar')

        for mod_path in candidates:
            if os.path.isfile(mod_path):
                try:
                    bundled_hash = self._file_sha256(mod_path)
                    need_copy = (
                        not os.path.isfile(target)
                        or cfg.get('last_bundled_mod_hash') != bundled_hash
                        or (not cfg.get('remote_mod_asset_id') and self._file_sha256(target) != bundled_hash)
                    )
                    if need_copy:
                        shutil.copy2(mod_path, target)
                        self._save_config({
                            'last_bundled_mod_hash': bundled_hash,
                            'remote_mod_asset_id': None,
                            'remote_mod_updated_at': None,
                        })
                        self.log(f"Synced latest SpaceRust mod: {target}")
                except Exception as e:
                    self.log(f"Could not sync mod: {e}")
                break

        self.sync_menu_videos(base_dir)
        self.check_cloud_updates()

    def check_cloud_updates(self):
        """Check GitHub Releases (latest) for updated mod JAR or menu_videos.zip."""
        import urllib.request
        try:
            api_url = "https://api.github.com/repos/DmutroKhludov/spacerust/releases/latest"
            req = urllib.request.Request(api_url, headers={'User-Agent': 'SpaceRustLauncher/1.0'})
            with urllib.request.urlopen(req, timeout=4) as resp:
                release = json.loads(resp.read().decode('utf-8'))
        except Exception:
            return

        cfg = self._load_config()
        assets = {a.get('name'): a for a in release.get('assets', []) if isinstance(a, dict)}

        # 1. Check updated mod jar asset
        mod_asset = assets.get('spacerust-mod-1.0.0.jar')
        if mod_asset:
            asset_id = mod_asset.get('id')
            updated_at = mod_asset.get('updated_at')
            dl_url = mod_asset.get('browser_download_url')
            if dl_url and (cfg.get('remote_mod_asset_id') != asset_id or cfg.get('remote_mod_updated_at') != updated_at):
                try:
                    target = os.path.join(self.game_dir, 'mods', 'spacerust-mod-1.0.0.jar')
                    tmp_mod = target + '.tmp'
                    req = urllib.request.Request(dl_url, headers={'User-Agent': 'SpaceRustLauncher/1.0'})
                    with urllib.request.urlopen(req, timeout=20) as r, open(tmp_mod, 'wb') as out:
                        shutil.copyfileobj(r, out)
                    if os.path.isfile(tmp_mod) and os.path.getsize(tmp_mod) > 1024:
                        shutil.move(tmp_mod, target)
                        self._save_config({
                            'remote_mod_asset_id': asset_id,
                            'remote_mod_updated_at': updated_at,
                        })
                        self.log(f"Downloaded cloud mod update ({release.get('tag_name')}): {target}")
                except Exception as e:
                    self.log(f"Cloud mod update failed: {e}")

        # 2. Check menu_videos.zip if local menu_videos is incomplete
        vid_dir = os.path.join(self.game_dir, 'menu_videos')
        has_mp4 = os.path.isdir(vid_dir) and any(f.lower().endswith('.mp4') for f in os.listdir(vid_dir))
        has_ffmpeg = os.path.isfile(os.path.join(vid_dir, 'ffmpeg.exe'))
        vid_asset = assets.get('menu_videos.zip')
        if vid_asset and (not has_mp4 or not has_ffmpeg):
            dl_url = vid_asset.get('browser_download_url')
            if dl_url:
                tmp_zip = os.path.join(self.game_dir, 'menu_videos.tmp.zip')
                try:
                    import zipfile
                    req = urllib.request.Request(dl_url, headers={'User-Agent': 'SpaceRustLauncher/1.0'})
                    with urllib.request.urlopen(req, timeout=120) as r, open(tmp_zip, 'wb') as out:
                        shutil.copyfileobj(r, out)
                    os.makedirs(vid_dir, exist_ok=True)
                    with zipfile.ZipFile(tmp_zip, 'r') as zf:
                        zf.extractall(vid_dir)
                    self.log(f"Downloaded cloud menu_videos update to: {vid_dir}")
                except Exception as e:
                    self.log(f"Cloud menu_videos update failed: {e}")
                finally:
                    if os.path.isfile(tmp_zip):
                        try:
                            os.remove(tmp_zip)
                        except Exception:
                            pass

    def sync_menu_videos(self, base_dir=None):
        """Ensure menu_videos (MP4 clips + ffmpeg.exe) are available inside game_dir/menu_videos"""
        if base_dir is None:
            base_dir = getattr(sys, '_MEIPASS', self.exe_dir) if getattr(sys, 'frozen', False) else self.exe_dir

        dst_videos = os.path.join(self.game_dir, 'menu_videos')
        os.makedirs(dst_videos, exist_ok=True)

        src_candidates = [
            os.path.join(self.exe_dir, 'menu_videos'),
            os.path.join(base_dir, 'menu_videos'),
            os.path.join(self.exe_dir, 'data', 'menu_videos'),
        ]

        for src_dir in src_candidates:
            if os.path.isdir(src_dir) and os.path.abspath(src_dir) != os.path.abspath(dst_videos):
                try:
                    for fname in os.listdir(src_dir):
                        low = fname.lower()
                        if not (low.endswith('.mp4') or low == 'ffmpeg.exe'):
                            continue
                        src_f = os.path.join(src_dir, fname)
                        dst_f = os.path.join(dst_videos, fname)
                        if os.path.isfile(src_f) and (not os.path.isfile(dst_f) or os.path.getsize(src_f) != os.path.getsize(dst_f)):
                            shutil.copy2(src_f, dst_f)
                    self.log(f"Synced menu_videos to: {dst_videos}")
                except Exception as e:
                    self.log(f"Could not sync menu_videos: {e}")
                break

    def create_desktop_shortcut(self):
        try:
            desktop = os.path.expandvars(r'%USERPROFILE%\Desktop')
            lnk_path = os.path.join(desktop, 'SpaceRust Launcher.lnk')
            if getattr(sys, 'frozen', False):
                exe_path = sys.executable
            else:
                exe_path = os.path.abspath(sys.argv[0])

            work_dir = os.path.dirname(exe_path)
            if not os.path.exists(lnk_path):
                ps_script = (
                    f'$ws = New-Object -ComObject WScript.Shell; '
                    f'$s = $ws.CreateShortcut("{lnk_path}"); '
                    f'$s.TargetPath = "{exe_path}"; '
                    f'$s.WorkingDirectory = "{work_dir}"; '
                    f'$s.IconLocation = "{exe_path},0"; '
                    f'$s.Description = "SpaceRust 1.16.5 Launcher"; '
                    f'$s.Save()'
                )
                subprocess.run(["powershell", "-NoProfile", "-Command", ps_script], capture_output=True, timeout=5)
                self.log(f"Desktop shortcut created: {lnk_path}")
        except Exception as e:
            self.log(f"Could not create shortcut: {e}")

    def download_file_with_progress(self, url, dest_path, notify):
        import time
        import urllib.request
        self.log(f"Connecting to {url}...")
        req = urllib.request.Request(url, headers={'User-Agent': 'SpaceRustLauncher/1.0'})
        with urllib.request.urlopen(req, timeout=30) as resp, open(dest_path, 'wb') as out_f:
            total_str = resp.headers.get('Content-Length')
            total_size = int(total_str) if total_str else 0
            downloaded = 0
            chunk_size = 1024 * 256
            last_pct = -1
            win_start = time.monotonic()
            win_bytes = 0
            last_report = 0.0
            speed_str = f"0.0 {self._msg('mbs')}"

            while True:
                if not self._pause_event.is_set():
                    pct_now = last_pct if last_pct > 0 else 0
                    notify(self._msg('paused', pct=pct_now), pct_now)
                    self._pause_event.wait()
                    win_start = time.monotonic()
                    win_bytes = 0
                    last_report = 0.0
                chunk = resp.read(chunk_size)
                if not chunk:
                    break
                out_f.write(chunk)
                n = len(chunk)
                downloaded += n
                win_bytes += n

                now = time.monotonic()
                elapsed = now - win_start
                if elapsed >= 0.4:
                    bps = win_bytes / elapsed
                    if bps >= 1024 * 1024:
                        speed_str = f"{bps / (1024 * 1024):.1f} {self._msg('mbs')}"
                    else:
                        speed_str = f"{bps / 1024:.0f} {self._msg('kbs')}"
                    win_start = now
                    win_bytes = 0

                pct = int((downloaded / total_size) * 85) + 5 if total_size > 0 else 50
                if pct != last_pct or (now - last_report) >= 0.4:
                    last_pct = pct
                    last_report = now
                    mb_done = downloaded / (1024 * 1024)
                    mb_unit = self._msg('mb')
                    if total_size > 0:
                        mb_total = total_size / (1024 * 1024)
                        notify(f"{mb_done:.0f} / {mb_total:.0f} {mb_unit} · {speed_str}", pct)
                    else:
                        notify(f"{mb_done:.0f} {mb_unit} · {speed_str}", pct)

    def verify_and_prepare(self, progress_callback=None):
        self._pause_event.set()
        def notify(stage, percent):
            self.log(f"{stage} ({percent}%)")
            if progress_callback:
                progress_callback(stage, percent)

        notify(self._msg('checking'), 10)
        self.ensure_directories()
        self.create_desktop_shortcut()

        ver_dir = os.path.join(self.game_dir, 'versions', self.version_id)
        ver_jar = os.path.join(ver_dir, f"{self.version_id}.jar")
        ver_json = os.path.join(ver_dir, f"{self.version_id}.json")

        # 1. If files already exist in .spacerust, we are ready!
        if os.path.isfile(ver_jar) and os.path.isfile(ver_json):
            self.sync_latest_mod()
            notify(self._msg('ready'), 100)
            return True

        # 2. Check if a portable 'data' folder exists next to the launcher exe/script
        portable_data = os.path.join(self.exe_dir, 'data')
        if os.path.isdir(portable_data):
            notify(self._msg('unpacking_dir'), 30)
            self.log(f"Copying portable data from {portable_data} to {self.game_dir}...")
            try:
                for item in os.listdir(portable_data):
                    src_item = os.path.join(portable_data, item)
                    dst_item = os.path.join(self.game_dir, item)
                    if os.path.isdir(src_item):
                        shutil.copytree(src_item, dst_item, dirs_exist_ok=True)
                    else:
                        shutil.copy2(src_item, dst_item)
                self.sync_latest_mod()
                if os.path.isfile(ver_jar) and os.path.isfile(ver_json):
                    notify(self._msg('ready'), 100)
                    return True
            except Exception as e:
                self.log(f"Error copying portable data: {e}")

        # 3. If local .minecraft has Fabric 1.16.5, seed from it
        local_mc = os.path.expandvars(r'%APPDATA%\.minecraft')
        local_src_ver = os.path.join(local_mc, 'versions', 'Fabric 1.16.5')

        if os.path.isdir(local_src_ver):
            notify(self._msg('init_cache'), 40)
            shutil.copytree(local_src_ver, ver_dir, dirs_exist_ok=True)
            for f in os.listdir(ver_dir):
                if 'Fabric 1.16.5' in f:
                    new_f = f.replace('Fabric 1.16.5', self.version_id)
                    old_p = os.path.join(ver_dir, f)
                    new_p = os.path.join(ver_dir, new_f)
                    if not os.path.exists(new_p):
                        os.rename(old_p, new_p)
            if os.path.isfile(ver_json):
                with open(ver_json, 'r', encoding='utf-8') as jf:
                    jd = json.load(jf)
                jd['id'] = self.version_id
                with open(ver_json, 'w', encoding='utf-8') as jf:
                    json.dump(jd, jf, indent=2)

            local_libs = os.path.join(local_mc, 'libraries')
            lib_dir = os.path.join(self.game_dir, 'libraries')
            if os.path.isdir(local_libs) and (not os.path.exists(lib_dir) or len(os.listdir(lib_dir)) == 0):
                shutil.copytree(local_libs, lib_dir, dirs_exist_ok=True)

            local_assets = os.path.join(local_mc, 'assets')
            asset_dir = os.path.join(self.game_dir, 'assets')
            if os.path.isdir(local_assets) and (not os.path.exists(asset_dir) or len(os.listdir(asset_dir)) == 0):
                shutil.copytree(local_assets, asset_dir, dirs_exist_ok=True)

            self.sync_latest_mod()
            notify(self._msg('ready_short'), 100)
            return True

        # 4. Auto-download from GitHub Release (RustMe-style cloud bootstrap)
        cloud_url = "https://github.com/DmutroKhludov/spacerust/releases/download/v1.0.0/SpaceRust-Files.zip"
        notify(self._msg('first_launch'), 5)
        self.log(f"Downloading game client from {cloud_url}...")

        temp_zip = os.path.join(self.game_dir, 'client_download.tmp.zip')
        try:
            self.download_file_with_progress(cloud_url, temp_zip, notify)
            notify(self._msg('extracting'), 90)
            self.log("Extracting client package...")
            import zipfile
            with zipfile.ZipFile(temp_zip, 'r') as zf:
                zf.extractall(self.game_dir)
            if os.path.isfile(temp_zip):
                os.remove(temp_zip)
            self.sync_latest_mod()
            notify(self._msg('done_launch'), 100)
            return True
        except Exception as e:
            if os.path.isfile(temp_zip):
                try:
                    os.remove(temp_zip)
                except Exception:
                    pass
            self.log(f"Cloud download failed: {e}")
            raise RuntimeError(self._msg('dl_failed', err=e))

    def launch(self, nickname, ram_mb, progress_callback=None, playtime_callback=None):
        """Launches the isolated SpaceRust client directly into the Rust Main Menu."""
        try:
            # 1. Check and prepare client files FIRST (downloads from cloud if needed)
            try:
                self.verify_and_prepare(progress_callback=progress_callback)
            except Exception as e:
                return {"status": "error", "message": str(e)}

            # 2. Check Java (after client preparation, runtime/jre is available)
            java_exe = self.find_java()
            if not java_exe:
                err = self._msg('no_java')
                self.log(err)
                return {"status": "error", "message": err}

            ram_mb = int(ram_mb) if ram_mb else 4096

            options = {
                'username': str(nickname) if nickname else 'SpacePlayer',
                'jvmArguments': [
                    f'-Xmx{ram_mb}M',
                    '-Xms1024M',
                    '-XX:+UnlockExperimentalVMOptions',
                    '-XX:+UseG1GC',
                    '-XX:G1NewSizePercent=20',
                    '-XX:G1ReservePercent=20',
                    '-XX:MaxGCPauseMillis=50',
                    '-XX:G1HeapRegionSize=32M',
                    f'-Dminecraft.launcher.brand=SpaceRustLauncher',
                    f'-Dminecraft.launcher.version=2.4.2'
                ],
                'executablePath': java_exe
            }

            self.log(f"Launching version: {self.version_id}")
            self.log(f"GameDir: {self.game_dir}")
            self.log(f"Player: {options['username']}, RAM: {ram_mb} MB")
            self.log(f"Java: {java_exe}")

            cmd = minecraft_launcher_lib.command.get_minecraft_command(self.version_id, self.game_dir, options)
            flags = 0
            if sys.platform == 'win32':
                flags = getattr(subprocess, 'CREATE_NEW_PROCESS_GROUP', 0) | getattr(subprocess, 'DETACHED_PROCESS', 0)
            proc = subprocess.Popen(cmd, cwd=self.game_dir, creationflags=flags)
            self.log(f"Process launched! PID: {proc.pid}")
            threading.Thread(target=self._track_playtime, args=(proc, playtime_callback), daemon=True).start()

            return {
                "status": "success",
                "pid": proc.pid,
                "game_dir": self.game_dir,
                "version": self.version_id
            }
        except Exception as e:
            err_details = traceback.format_exc()
            self.log(f"Fatal launch error:\n{err_details}")
            return {
                "status": "error",
                "message": self._msg('launch_err', err=str(e))
            }
