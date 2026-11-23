import os
import sys
import shutil
import zipfile
import time

def log(msg):
    print(f"[*] {msg}", flush=True)

def copy_folder(src, dst):
    if not os.path.exists(src):
        log(f"Warning: Source folder {src} not found!")
        return False
    log(f"Copying {src} -> {dst} ...")
    shutil.copytree(src, dst, dirs_exist_ok=True)
    return True

def main():
    root_dir = os.path.dirname(os.path.abspath(__file__))
    dist_dir = os.path.join(root_dir, "SpaceRust_Client")
    desktop_dir = os.path.expandvars(r"%USERPROFILE%\Desktop")
    appdata_spacerust = os.path.expandvars(r"%APPDATA%\.spacerust")
    java_src = r"C:\Program Files\Java\jre1.8.0_351"

    log("==================================================")
    log("  SPACERUST CLIENT PACKAGER FOR FRIENDS")
    log("==================================================")

    # 1. Ensure destination folder exists
    os.makedirs(dist_dir, exist_ok=True)

    # 2. Copy SpaceRust.exe
    exe_src = os.path.join(root_dir, "dist", "SpaceRust.exe")
    if not os.path.isfile(exe_src):
        exe_src = os.path.join(root_dir, "SpaceRust.exe")
    if os.path.isfile(exe_src):
        log(f"Copying SpaceRust.exe ({os.path.getsize(exe_src) / (1024*1024):.1f} MB)...")
        shutil.copy2(exe_src, os.path.join(dist_dir, "SpaceRust.exe"))
        # Also copy to root if it was from dist
        if "dist" in exe_src:
            shutil.copy2(exe_src, os.path.join(root_dir, "SpaceRust.exe"))
    else:
        log("ERROR: SpaceRust.exe not found! Please build it first.")
        return

    # 3. Copy Portable Java (JRE 8)
    jre_dst = os.path.join(dist_dir, "jre")
    if os.path.isfile(os.path.join(jre_dst, "bin", "javaw.exe")):
        log("Portable JRE already present, skipping re-copy.")
    elif os.path.isdir(java_src):
        copy_folder(java_src, jre_dst)
    else:
        log(f"Warning: Java source {java_src} not found. Friend will need system Java installed.")

    # 4. Copy Game Data from %APPDATA%\.spacerust
    data_dst = os.path.join(dist_dir, "data")
    os.makedirs(data_dst, exist_ok=True)

    items_to_copy = ["assets", "libraries", "versions", "mods", "menu_videos", "options.txt"]
    for item in items_to_copy:
        src_path = os.path.join(appdata_spacerust, item)
        dst_path = os.path.join(data_dst, item)
        if os.path.isdir(src_path):
            copy_folder(src_path, dst_path)
        elif os.path.isfile(src_path):
            shutil.copy2(src_path, dst_path)

    # 5. Create INSTRUCTION file
    readme_path = os.path.join(dist_dir, "ИНСТРУКЦИЯ.txt")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(
            "====================================================\n"
            "   SPACERUST 1.16.5 - ИНСТРУКЦИЯ ПО ЗАПУСКУ ДЛЯ ДРУГА\n"
            "====================================================\n\n"
            "1. Распакуйте весь этот архив в любую папку на компьютере.\n"
            "2. Запустите файл SpaceRust.exe\n"
            "3. Введите свой ник и нажмите кнопку «НАЧАТЬ ИГРАТЬ»!\n\n"
            "ПРИМЕЧАНИЯ:\n"
            "• При первом запуске клиент распакует файлы игры (~10-15 сек).\n"
            "• Java и все необходимые моды уже встроены в эту папку.\n"
            "• Для игры с друзьями подключитесь к сети Radmin VPN хоста\n"
            "  и нажмите «ИГРАТЬ» на сервере SpaceRust в главном меню.\n"
        )
    log("Created instruction file: ИНСТРУКЦИЯ.txt")

    # 6. Create ZIP archive
    zip_path = os.path.join(root_dir, "SpaceRust_Client.zip")
    desktop_zip = os.path.join(desktop_dir, "SpaceRust_Client.zip")
    log(f"Creating ZIP archive {zip_path} (this might take 1-2 minutes)...")

    start_time = time.time()
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, _, files in os.walk(dist_dir):
            for file in files:
                abs_file = os.path.join(root, file)
                rel_file = os.path.relpath(abs_file, os.path.dirname(dist_dir))
                zf.write(abs_file, rel_file)

    elapsed = time.time() - start_time
    zip_mb = os.path.getsize(zip_path) / (1024 * 1024)
    log(f"ZIP created successfully! Size: {zip_mb:.1f} MB in {elapsed:.1f}s")

    # Copy to Desktop
    try:
        shutil.copy2(zip_path, desktop_zip)
        log(f"Copied ZIP directly to Desktop: {desktop_zip}")
    except Exception as e:
        log(f"Could not copy to Desktop: {e}")

    # Ensure SpaceRust Desktop Shortcut
    try:
        ico = os.path.join(root_dir, "spacerust_icon.ico")
        sc_path = os.path.join(desktop_dir, "SpaceRust.lnk")
        ps_cmd = f'$ws=New-Object -ComObject WScript.Shell;$s=$ws.CreateShortcut("{sc_path}");$s.TargetPath="C:\\Python312\\pythonw.exe";$s.Arguments="run_launcher.py";$s.WorkingDirectory="{root_dir}";$s.IconLocation="{ico}, 0";$s.Description="SpaceRust 1.16.5";$s.Save()'
        import subprocess
        subprocess.run(["powershell", "-NoProfile", "-Command", ps_cmd], check=False)
        log(f"Created/Updated Desktop Shortcut: {sc_path}")
    except Exception as e:
        log(f"Shortcut warning: {e}")

    log("==================================================")
    log("  PACKAGING FINISHED SUCCESSFULLY!")
    log(f"  Send 'SpaceRust_Client.zip' to your friend!")
    log("==================================================")

if __name__ == "__main__":
    main()
