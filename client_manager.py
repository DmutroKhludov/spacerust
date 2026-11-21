import os
import sys
import shutil
import subprocess
import json
import traceback
import minecraft_launcher_lib

class SpaceRustClientManager:
    def __init__(self):
        self.game_dir = os.path.expandvars(r'%APPDATA%\.spacerust')
        self.version_id = 'SpaceRust-1.16.5'
        self.log_file = os.path.join(self.game_dir, 'launcher.log')
        if getattr(sys, 'frozen', False):
            self.exe_dir = os.path.dirname(sys.executable)
        else:
            self.exe_dir = os.path.dirname(os.path.abspath(__file__))
        self.ensure_directories()

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

    def sync_latest_mod(self):
        """Always ensure the latest spacerust-mod-1.0.0.jar is installed in .spacerust/mods"""
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

        for mod_path in candidates:
            if os.path.isfile(mod_path):
                target = os.path.join(dst_mods, 'spacerust-mod-1.0.0.jar')
                try:
                    shutil.copy2(mod_path, target)
                    self.log(f"Synced latest SpaceRust mod: {target}")
                except Exception as e:
                    self.log(f"Could not sync mod: {e}")
                break

    def verify_and_prepare(self, progress_callback=None):
        def notify(stage, percent):
            self.log(f"{stage} ({percent}%)")
            if progress_callback:
                progress_callback(stage, percent)

        notify("Проверка папки SpaceRust...", 10)
        self.ensure_directories()

        ver_dir = os.path.join(self.game_dir, 'versions', self.version_id)
        ver_jar = os.path.join(ver_dir, f"{self.version_id}.jar")
        ver_json = os.path.join(ver_dir, f"{self.version_id}.json")

        # 1. If files already exist in .spacerust, we are ready!
        if os.path.isfile(ver_jar) and os.path.isfile(ver_json):
            self.sync_latest_mod()
            notify("Клиент готов к запуску!", 100)
            return True

        # 2. Check if a portable 'data' folder exists next to the launcher exe/script
        portable_data = os.path.join(self.exe_dir, 'data')
        if os.path.isdir(portable_data):
            notify("Первый запуск: распаковка файлов игры (10-15 сек)...", 30)
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
                    notify("Клиент готов к запуску!", 100)
                    return True
            except Exception as e:
                self.log(f"Error copying portable data: {e}")

        # 3. If local .minecraft has Fabric 1.16.5, seed from it
        local_mc = os.path.expandvars(r'%APPDATA%\.minecraft')
        local_src_ver = os.path.join(local_mc, 'versions', 'Fabric 1.16.5')

        if os.path.isdir(local_src_ver):
            notify("Инициализация файлов из локального кэша...", 40)
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
            notify("Клиент готов!", 100)
            return True

        # 4. If no files exist at all on friend's PC
        err_msg = (
            "Файлы игры не найдены!\n\n"
            "Вы запустили только лаунчер (.exe), но на этом ПК нет файлов самой игры (~1 ГБ).\n\n"
            "КАК ЗАПУСТИТЬ ИГРУ ДРУГУ:\n"
            "1. Отправьте другу полную папку 'SpaceRust_Client' (или архив 'SpaceRust_Client.zip')\n"
            "2. Друг должен распаковать архив и запустить SpaceRust.exe из этой папки!\n"
            "3. В архиве уже будут лежать папки 'data' (файлы игры) и 'jre' (Java)."
        )
        self.log(err_msg)
        raise RuntimeError(err_msg)

    def launch(self, nickname, ram_mb):
        """Launches the isolated SpaceRust client directly into the Rust Main Menu."""
        try:
            # 1. Check Java FIRST
            java_exe = self.find_java()
            if not java_exe:
                err = (
                    "Java не найдена на вашем компьютере!\n\n"
                    "Для запуска игры требуется Java 8 (x64).\n\n"
                    "КАК ИСПРАВИТЬ:\n"
                    "1. Запускайте игру из распакованной папки 'SpaceRust_Client' (где рядом лежит папка 'jre')\n"
                    "2. Либо установите Java с официального сайта: https://www.java.com"
                )
                self.log(err)
                return {"status": "error", "message": err}

            # 2. Check and prepare client files
            try:
                self.verify_and_prepare()
            except Exception as e:
                return {"status": "error", "message": str(e)}

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
            proc = subprocess.Popen(cmd, cwd=self.game_dir)
            self.log(f"Process launched! PID: {proc.pid}")

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
                "message": f"Ошибка запуска игры: {str(e)}"
            }
