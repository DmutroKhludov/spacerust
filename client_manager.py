import os
import sys
import shutil
import subprocess
import json
import threading
import traceback
import minecraft_launcher_lib

SUPPORTED_LANGS = ('ru', 'uk', 'en')
GITHUB_REPO = 'DmutroKhludov/spacerust'
LAUNCHER_VERSION = '2.5.1'

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
        'updating_launcher': 'Загрузка обновления лаунчера...',
        'updating_mod': 'Загрузка обновления клиента...',
        'update_restarting': 'Обновление загружено! Перезапуск лаунчера...',
        'update_done': 'Обновление успешно установлено!',
        'update_none': 'Установлена последняя версия!',
        'update_err': 'Ошибка обновления: {err}',
        'repair_start': 'Проверка и восстановление файлов игры...',
        'repair_downloading': 'Загрузка файлов восстановления...',
        'repair_extracting': 'Восстановление файлов клиента...',
        'repair_done': 'Клиент восстановлен и готов к запуску!',
        'repair_failed': 'Не удалось восстановить игру:\n{err}',
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
        'updating_launcher': 'Завантаження оновлення лаунчера...',
        'updating_mod': 'Завантаження оновлення клієнта...',
        'update_restarting': 'Оновлення завантажено! Перезапуск лаунчера...',
        'update_done': 'Оновлення успішно встановлено!',
        'update_none': 'Встановлено останню версію!',
        'update_err': 'Помилка оновлення: {err}',
        'repair_start': 'Перевірка та відновлення файлів гри...',
        'repair_downloading': 'Завантаження файлів відновлення...',
        'repair_extracting': 'Відновлення файлів клієнта...',
        'repair_done': 'Клієнт відновлено та готово до запуску!',
        'repair_failed': 'Не вдалося відновити гру:\n{err}',
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
        'updating_launcher': 'Downloading launcher update...',
        'updating_mod': 'Downloading client update...',
        'update_restarting': 'Update downloaded! Restarting launcher...',
        'update_done': 'Update installed successfully!',
        'update_none': 'You are already on the latest version!',
        'update_err': 'Update error: {err}',
        'repair_start': 'Verifying and repairing game files...',
        'repair_downloading': 'Downloading repair files...',
        'repair_extracting': 'Restoring client files...',
        'repair_done': 'Client repaired and ready to launch!',
        'repair_failed': 'Failed to repair game:\n{err}',
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

    def sync_latest_mod(self, force=False):
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
                        force
                        or not os.path.isfile(target)
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
        # Never overwrite a locally built workspace/bundled mod JAR with an older GitHub Release
        local_dev_mod = os.path.join(self.exe_dir, 'spacerust-mod', 'build', 'libs', 'spacerust-mod-1.0.0.jar')
        if not os.path.isfile(local_dev_mod):
            self.check_cloud_updates()

    def _parse_github_time(self, iso_str):
        if not iso_str:
            return 0.0
        try:
            import datetime
            clean = str(iso_str).strip()
            if clean.endswith('Z'):
                clean = clean[:-1] + '+00:00'
            return datetime.datetime.fromisoformat(clean).timestamp()
        except Exception:
            return 0.0

    def _http_get(self, url, timeout=6, accept=None):
        import urllib.request
        import ssl
        headers = {'User-Agent': 'SpaceRustLauncher/1.0'}
        if accept:
            headers['Accept'] = accept
        req = urllib.request.Request(url, headers=headers)
        ctx = None
        try:
            import certifi
            ctx = ssl.create_default_context(cafile=certifi.where())
        except Exception:
            ctx = None
        with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
            return resp.read().decode('utf-8', errors='replace'), resp.geturl()

    def _fetch_github_json(self, path, timeout=4):
        raw, _ = self._http_get(
            f"https://api.github.com/repos/{GITHUB_REPO}{path}",
            timeout=timeout,
            accept='application/vnd.github+json',
        )
        return json.loads(raw)

    def _fetch_commits_atom(self, limit=8):
        import xml.etree.ElementTree as ET
        xml_text, _ = self._http_get(f"https://github.com/{GITHUB_REPO}/commits/main.atom", timeout=6)
        root = ET.fromstring(xml_text)
        ns = {'a': 'http://www.w3.org/2005/Atom'}
        commits = []
        for entry in root.findall('a:entry', ns)[:limit]:
            eid = entry.findtext('a:id', '', ns).strip()
            sha = eid.split('/')[-1] if '/' in eid else eid
            title = (entry.findtext('a:title', '', ns) or '').strip() or 'Update'
            updated = (entry.findtext('a:updated', '', ns) or '').strip()
            author = (entry.findtext('a:author/a:name', '', ns) or '').strip() or 'SpaceRust'
            link_el = entry.find('a:link', ns)
            url = link_el.attrib.get('href', '') if link_el is not None else f"https://github.com/{GITHUB_REPO}/commit/{sha}"
            if sha:
                commits.append({
                    'sha': sha,
                    'short_sha': sha[:7],
                    'title': title,
                    'details': [],
                    'author': author,
                    'date': updated,
                    'url': url,
                })
        return commits

    def _fetch_latest_release(self):
        """Fetches latest GitHub Release & assets via REST API, falling back to rate-limit-free Atom + HTML."""
        try:
            rel = self._fetch_github_json('/releases/latest', timeout=4)
            if isinstance(rel, dict):
                return rel
        except Exception:
            pass

        # Fallback without GitHub REST API rate limits
        import xml.etree.ElementTree as ET
        import re
        tag_name = ''
        rel_name = ''
        published_at = ''
        body = ''
        rel_url = f"https://github.com/{GITHUB_REPO}/releases"

        try:
            atom_text, _ = self._http_get(f"https://github.com/{GITHUB_REPO}/releases.atom", timeout=6)
            root = ET.fromstring(atom_text)
            ns = {'a': 'http://www.w3.org/2005/Atom'}
            entry = root.find('a:entry', ns)
            if entry is not None:
                eid = (entry.findtext('a:id', '', ns) or '').strip()
                link_el = entry.find('a:link', ns)
                if link_el is not None and link_el.attrib.get('href'):
                    rel_url = link_el.attrib['href']
                    tag_name = rel_url.rstrip('/').split('/')[-1]
                elif '/' in eid:
                    tag_name = eid.rstrip('/').split('/')[-1]
                rel_name = (entry.findtext('a:title', '', ns) or '').strip() or tag_name
                published_at = (entry.findtext('a:updated', '', ns) or '').strip()
                raw_html = (entry.findtext('a:content', '', ns) or '').strip()
                body = re.sub(r'<[^>]+>', ' ', raw_html).strip()
        except Exception:
            pass

        if not tag_name:
            try:
                _, final_url = self._http_get(f"https://github.com/{GITHUB_REPO}/releases/latest", timeout=6)
                if '/releases/tag/' in final_url:
                    tag_name = final_url.rstrip('/').split('/')[-1]
                    rel_url = final_url
                    rel_name = rel_name or tag_name
            except Exception:
                pass

        if not tag_name:
            return None

        assets = []
        try:
            exp_html, _ = self._http_get(
                f"https://github.com/{GITHUB_REPO}/releases/expanded_assets/{tag_name}",
                timeout=6,
            )
            seen_names = set()
            pattern = re.compile(rf'href="(/{re.escape(GITHUB_REPO)}/releases/download/[^"]+)"')
            for m in pattern.finditer(exp_html):
                href = m.group(1)
                name = href.split('/')[-1]
                if not name or name in seen_names:
                    continue
                seen_names.add(name)
                assets.append({
                    'id': f"{tag_name}:{name}:{published_at}",
                    'name': name,
                    'updated_at': published_at,
                    'browser_download_url': f"https://github.com{href}",
                    'size': 0,
                })
        except Exception as e:
            self.log(f"Expanded assets fetch failed: {e}")

        return {
            'tag_name': tag_name,
            'name': rel_name or tag_name,
            'body': body,
            'published_at': published_at,
            'html_url': rel_url,
            'assets': assets,
        }

    def _get_bundled_mod_mtime(self):
        bundled_mtime = 0.0
        for cand in [
            os.path.join(getattr(sys, '_MEIPASS', self.exe_dir), 'spacerust-mod-1.0.0.jar'),
            os.path.join(self.exe_dir, 'spacerust-mod', 'build', 'libs', 'spacerust-mod-1.0.0.jar'),
        ]:
            if os.path.isfile(cand):
                bundled_mtime = max(bundled_mtime, os.path.getmtime(cand))
        return bundled_mtime

    def _get_current_launcher_mtime(self):
        try:
            if getattr(sys, 'frozen', False):
                return os.path.getmtime(sys.executable)
            return os.path.getmtime(os.path.abspath(__file__))
        except Exception:
            return 0.0

    def mark_updates_seen(self, commit_sha=None):
        if commit_sha:
            self._save_config({'seen_commit_sha': str(commit_sha)})
            return {"status": "ok", "seen_commit_sha": str(commit_sha)}
        try:
            commits = self._fetch_commits_atom(limit=1)
            if commits:
                sha = commits[0]['sha']
                self._save_config({'seen_commit_sha': sha})
                return {"status": "ok", "seen_commit_sha": sha}
        except Exception:
            pass
        return {"status": "ok"}

    def check_github_updates(self, force=False):
        """Checks GitHub Releases and main branch commits without failing on REST API rate limits."""
        import time
        cache = getattr(self, '_github_cache', None)
        if not force and cache and (time.monotonic() - cache[0]) < 45.0:
            return cache[1]

        cfg = self._load_config()
        commits = []
        latest_release = None
        release_obj = None
        release_err = None
        commits_err = None

        # 1. Fetch commits (prefer rate-limit-free Atom feed, try REST API only for extra details when unthrottled)
        try:
            commits = self._fetch_commits_atom(limit=8)
        except Exception as e:
            commits_err = e
            self.log(f"GitHub Atom commits failed: {e}")

        if not commits:
            try:
                raw_commits = self._fetch_github_json('/commits?sha=main&per_page=8', timeout=4)
                if isinstance(raw_commits, list):
                    for item in raw_commits:
                        sha = item.get('sha') or ''
                        cinfo = item.get('commit') or {}
                        raw_msg = (cinfo.get('message') or '').strip()
                        lines = [ln.strip() for ln in raw_msg.splitlines() if ln.strip()]
                        author_info = cinfo.get('author') or {}
                        commits.append({
                            'sha': sha,
                            'short_sha': sha[:7],
                            'title': lines[0] if lines else 'Update',
                            'details': lines[1:4],
                            'author': author_info.get('name') or 'SpaceRust',
                            'date': author_info.get('date') or '',
                            'url': item.get('html_url') or f"https://github.com/{GITHUB_REPO}/commit/{sha}",
                        })
                    commits_err = None
            except Exception as e:
                commits_err = e
                self.log(f"GitHub REST commits failed: {e}")

        # 2. Fetch latest release & assets (REST API with Atom+HTML rate-limit-free fallback)
        try:
            release_obj = self._fetch_latest_release()
        except Exception as e:
            release_err = e
            self.log(f"GitHub release check failed: {e}")

        launcher_update = {'available': False}
        mod_update = {'available': False}

        if isinstance(release_obj, dict):
            latest_release = {
                'tag_name': release_obj.get('tag_name') or '',
                'name': release_obj.get('name') or release_obj.get('tag_name') or '',
                'body': (release_obj.get('body') or '').strip(),
                'published_at': release_obj.get('published_at') or '',
                'url': release_obj.get('html_url') or f"https://github.com/{GITHUB_REPO}/releases",
            }
            assets = {a.get('name'): a for a in release_obj.get('assets', []) if isinstance(a, dict)}

            # Check updated SpaceRust.exe
            exe_asset = assets.get('SpaceRust.exe')
            if exe_asset:
                asset_id = exe_asset.get('id')
                updated_at = exe_asset.get('updated_at') or ''
                dl_url = exe_asset.get('browser_download_url') or ''
                remote_ts = self._parse_github_time(updated_at)
                local_exe_ts = self._get_current_launcher_mtime()
                is_frozen = bool(getattr(sys, 'frozen', False))
                exe_newer = remote_ts > (local_exe_ts + 120.0)
                not_applied = (
                    cfg.get('remote_launcher_asset_id') != asset_id
                    or cfg.get('remote_launcher_updated_at') != updated_at
                )
                if dl_url and is_frozen and exe_newer and not_applied:
                    launcher_update = {
                        'available': True,
                        'asset_name': 'SpaceRust.exe',
                        'asset_id': asset_id,
                        'updated_at': updated_at,
                        'download_url': dl_url,
                        'size': int(exe_asset.get('size') or 0),
                        'tag_name': latest_release['tag_name'],
                    }

            # Check updated spacerust-mod-1.0.0.jar
            mod_asset = assets.get('spacerust-mod-1.0.0.jar')
            local_dev_mod = os.path.join(self.exe_dir, 'spacerust-mod', 'build', 'libs', 'spacerust-mod-1.0.0.jar')
            if mod_asset and not os.path.isfile(local_dev_mod):
                asset_id = mod_asset.get('id')
                updated_at = mod_asset.get('updated_at') or ''
                dl_url = mod_asset.get('browser_download_url') or ''
                remote_ts = self._parse_github_time(updated_at)
                bundled_ts = self._get_bundled_mod_mtime()
                remote_newer = remote_ts > (bundled_ts + 120.0)
                not_applied = (
                    cfg.get('remote_mod_asset_id') != asset_id
                    or cfg.get('remote_mod_updated_at') != updated_at
                )
                if dl_url and remote_newer and not_applied:
                    mod_update = {
                        'available': True,
                        'asset_name': 'spacerust-mod-1.0.0.jar',
                        'asset_id': asset_id,
                        'updated_at': updated_at,
                        'download_url': dl_url,
                        'size': int(mod_asset.get('size') or 0),
                        'tag_name': latest_release['tag_name'],
                    }

        latest_sha = commits[0]['sha'] if commits else ''
        seen_sha = cfg.get('seen_commit_sha') or ''
        has_new_commit = bool(latest_sha and latest_sha != seen_sha)
        has_binary_update = bool(launcher_update.get('available') or mod_update.get('available'))
        is_offline = bool(not commits and not latest_release)

        result = {
            'status': 'offline' if is_offline else 'ok',
            'repo': GITHUB_REPO,
            'launcher_version': LAUNCHER_VERSION,
            'is_frozen': bool(getattr(sys, 'frozen', False)),
            'commits': commits,
            'latest_release': latest_release,
            'latest_commit_sha': latest_sha,
            'latest_commit_short': commits[0]['short_sha'] if commits else '',
            'has_new_commit': has_new_commit,
            'launcher_update': launcher_update,
            'mod_update': mod_update,
            'has_binary_update': has_binary_update,
            'has_any_update': bool(has_new_commit or has_binary_update),
        }
        if not is_offline:
            self._github_cache = (time.monotonic(), result)
        return result

    def apply_github_update(self, progress_callback=None):
        """Downloads and applies available mod and/or SpaceRust.exe updates from GitHub Releases."""
        self._pause_event.set()
        def notify(stage, percent):
            self.log(f"{stage} ({percent}%)")
            if progress_callback:
                progress_callback(stage, percent)

        try:
            info = self.check_github_updates(force=True)
            if info.get('status') == 'offline':
                return {'status': 'error', 'message': self._msg('update_err', err='GitHub offline')}

            updated_mod = False
            mod_upd = info.get('mod_update') or {}
            if mod_upd.get('available') and mod_upd.get('download_url'):
                notify(self._msg('updating_mod'), 10)
                mods_dir = os.path.join(self.game_dir, 'mods')
                os.makedirs(mods_dir, exist_ok=True)
                target = os.path.join(mods_dir, 'spacerust-mod-1.0.0.jar')
                tmp_mod = target + '.tmp'
                self.download_file_with_progress(mod_upd['download_url'], tmp_mod, notify)
                if os.path.isfile(tmp_mod) and os.path.getsize(tmp_mod) > 1024:
                    shutil.move(tmp_mod, target)
                    self._save_config({
                        'remote_mod_asset_id': mod_upd.get('asset_id'),
                        'remote_mod_updated_at': mod_upd.get('updated_at'),
                    })
                    updated_mod = True
                    self.log(f"Updated client mod from GitHub: {target}")

            exe_upd = info.get('launcher_update') or {}
            if exe_upd.get('available') and exe_upd.get('download_url') and getattr(sys, 'frozen', False):
                notify(self._msg('updating_launcher'), 15)
                current_exe = os.path.abspath(sys.executable)
                new_exe = current_exe + '.new'
                self.download_file_with_progress(exe_upd['download_url'], new_exe, notify)
                if os.path.isfile(new_exe) and os.path.getsize(new_exe) > 100000:
                    updates_cfg = {
                        'remote_launcher_asset_id': exe_upd.get('asset_id'),
                        'remote_launcher_updated_at': exe_upd.get('updated_at'),
                    }
                    if info.get('latest_commit_sha'):
                        updates_cfg['seen_commit_sha'] = info['latest_commit_sha']
                    self._save_config(updates_cfg)

                    import tempfile
                    bat_path = os.path.join(tempfile.gettempdir(), 'spacerust_self_update.bat')
                    pid = os.getpid()
                    bat_body = (
                        "@echo off\r\n"
                        "chcp 65001 >nul\r\n"
                        ":waitloop\r\n"
                        f"tasklist /FI \"PID eq {pid}\" 2>NUL | find /I \"{pid}\" >NUL\r\n"
                        "if \"%ERRORLEVEL%\"==\"0\" (\r\n"
                        "    timeout /t 1 /nobreak >nul\r\n"
                        "    goto waitloop\r\n"
                        ")\r\n"
                        f"move /y \"{new_exe}\" \"{current_exe}\" >nul\r\n"
                        f"start \"\" \"{current_exe}\"\r\n"
                        "del \"%~f0\"\r\n"
                    )
                    with open(bat_path, 'w', encoding='utf-8') as bf:
                        bf.write(bat_body)

                    flags = getattr(subprocess, 'CREATE_NO_WINDOW', 0) | getattr(subprocess, 'DETACHED_PROCESS', 0)
                    subprocess.Popen(['cmd', '/c', bat_path], creationflags=flags)
                    notify(self._msg('update_restarting'), 100)
                    return {'status': 'restart_required', 'message': self._msg('update_restarting')}

            if info.get('latest_commit_sha'):
                self._save_config({'seen_commit_sha': info['latest_commit_sha']})

            if updated_mod:
                notify(self._msg('update_done'), 100)
                return {'status': 'ok', 'updated': 'mod', 'message': self._msg('update_done')}

            return {'status': 'up_to_date', 'message': self._msg('update_none')}
        except Exception as e:
            self.log(f"Update error: {e}")
            return {'status': 'error', 'message': self._msg('update_err', err=str(e))}

    def check_cloud_updates(self):
        """Check GitHub Releases (latest) for updated mod JAR or menu_videos.zip."""
        import urllib.request
        release = self._fetch_latest_release()
        if not isinstance(release, dict):
            return

        cfg = self._load_config()
        assets = {a.get('name'): a for a in release.get('assets', []) if isinstance(a, dict)}

        # 1. Check updated mod jar asset (only if strictly newer than local bundled mod)
        mod_asset = assets.get('spacerust-mod-1.0.0.jar')
        if mod_asset:
            asset_id = mod_asset.get('id')
            updated_at = mod_asset.get('updated_at')
            dl_url = mod_asset.get('browser_download_url')
            bundled_mtime = 0.0
            for cand in [
                os.path.join(getattr(sys, '_MEIPASS', self.exe_dir), 'spacerust-mod-1.0.0.jar'),
                os.path.join(self.exe_dir, 'spacerust-mod', 'build', 'libs', 'spacerust-mod-1.0.0.jar'),
            ]:
                if os.path.isfile(cand):
                    bundled_mtime = max(bundled_mtime, os.path.getmtime(cand))
            remote_newer = False
            if updated_at:
                try:
                    import datetime
                    dt = datetime.datetime.strptime(updated_at, '%Y-%m-%dT%H:%M:%SZ').replace(tzinfo=datetime.timezone.utc)
                    remote_newer = dt.timestamp() > (bundled_mtime + 120.0)
                except Exception:
                    remote_newer = False
            if dl_url and remote_newer and (cfg.get('remote_mod_asset_id') != asset_id or cfg.get('remote_mod_updated_at') != updated_at):
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
        cloud_url = self._get_client_zip_url()
        notify(self._msg('first_launch'), 5)
        self.log(f"Downloading game client from {cloud_url}...")

        temp_zip = os.path.join(self.game_dir, 'client_download.tmp.zip')
        try:
            self.download_file_with_progress(cloud_url, temp_zip, notify)
            notify(self._msg('extracting'), 90)
            self.log("Extracting client package...")
            self._extract_zip_package(temp_zip, preserve_options=True)
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

    def _get_client_zip_url(self):
        default_url = f"https://github.com/{GITHUB_REPO}/releases/download/v1.0.0/SpaceRust-Files.zip"
        try:
            rel = self._fetch_latest_release()
            if isinstance(rel, dict):
                for a in rel.get('assets', []):
                    if isinstance(a, dict) and a.get('name') == 'SpaceRust-Files.zip' and a.get('browser_download_url'):
                        return a['browser_download_url']
        except Exception:
            pass
        return default_url

    def _extract_zip_package(self, zip_path, preserve_options=True):
        import zipfile
        options_path = os.path.join(self.game_dir, 'options.txt')
        has_options = preserve_options and os.path.isfile(options_path)
        with zipfile.ZipFile(zip_path, 'r') as zf:
            for member in zf.infolist():
                norm = member.filename.replace('\\', '/').lstrip('/')
                if has_options and norm == 'options.txt':
                    continue
                zf.extract(member, self.game_dir)

    def repair_client(self, progress_callback=None):
        """Re-verifies and repairs SpaceRust client files, mod JAR, libraries, assets, and runtime."""
        self._pause_event.set()
        def notify(stage, percent):
            self.log(f"[Repair] {stage} ({percent}%)")
            if progress_callback:
                progress_callback(stage, percent)

        temp_zip = os.path.join(self.game_dir, 'client_repair.tmp.zip')
        try:
            notify(self._msg('repair_start'), 5)
            self.ensure_directories()

            # Remove stale/corrupt version folder so clean client files are restored
            ver_dir = os.path.join(self.game_dir, 'versions', self.version_id)
            if os.path.isdir(ver_dir):
                shutil.rmtree(ver_dir, ignore_errors=True)
            os.makedirs(ver_dir, exist_ok=True)

            self._save_config({
                'last_bundled_mod_hash': None,
                'remote_mod_asset_id': None,
                'remote_mod_updated_at': None,
            })

            portable_data = os.path.join(self.exe_dir, 'data')
            repaired_from_portable = False
            if os.path.isdir(portable_data):
                notify(self._msg('unpacking_dir'), 30)
                self.log(f"Repairing client from portable data: {portable_data}")
                for item in os.listdir(portable_data):
                    if item == 'options.txt' and os.path.isfile(os.path.join(self.game_dir, 'options.txt')):
                        continue
                    src_item = os.path.join(portable_data, item)
                    dst_item = os.path.join(self.game_dir, item)
                    if os.path.isdir(src_item):
                        shutil.copytree(src_item, dst_item, dirs_exist_ok=True)
                    else:
                        shutil.copy2(src_item, dst_item)
                ver_jar = os.path.join(ver_dir, f"{self.version_id}.jar")
                ver_json = os.path.join(ver_dir, f"{self.version_id}.json")
                repaired_from_portable = os.path.isfile(ver_jar) and os.path.isfile(ver_json)

            if not repaired_from_portable:
                cloud_url = self._get_client_zip_url()
                notify(self._msg('repair_downloading'), 8)
                self.log(f"Repairing client from {cloud_url}...")
                self.download_file_with_progress(cloud_url, temp_zip, notify)
                notify(self._msg('repair_extracting'), 90)
                self.log("Extracting repair package...")
                self._extract_zip_package(temp_zip, preserve_options=True)
                if os.path.isfile(temp_zip):
                    os.remove(temp_zip)

            self.sync_latest_mod(force=True)
            notify(self._msg('repair_done'), 100)
            return {'status': 'ok', 'message': self._msg('repair_done')}
        except Exception as e:
            if os.path.isfile(temp_zip):
                try:
                    os.remove(temp_zip)
                except Exception:
                    pass
            self.log(f"Repair failed: {e}")
            return {'status': 'error', 'message': self._msg('repair_failed', err=e)}

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
