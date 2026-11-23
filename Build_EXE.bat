@echo off
title Building SpaceRust.exe
cd /d "%~dp0"

echo ========================================================
echo   COMPILING STANDALONE SpaceRust.exe
echo ========================================================

echo [1/3] Building latest Fabric mod JAR...
if not defined JAVA_HOME if exist "%USERPROFILE%\.gradle\jdks\temurin-17\bin\javac.exe" set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\temurin-17"
cd spacerust-mod
call gradlew.bat build -x test
cd ..

echo [2/3] Syncing client files...
python client_manager.py

echo [3/3] Compiling standalone SpaceRust.exe with custom icon...
python -m PyInstaller --noconfirm --onefile --windowed --name "SpaceRust" --icon "spacerust_icon.ico" --collect-data minecraft_launcher_lib --add-data "index.html;." --add-data "css;css" --add-data "js;js" --add-data "assets;assets" --add-data "client_manager.py;." --add-data "spacerust-mod\build\libs\spacerust-mod-1.0.0.jar;." run_launcher.py

if exist "dist\SpaceRust.exe" (
    copy /Y "dist\SpaceRust.exe" "SpaceRust.exe"
    echo.
    echo ========================================================
    echo   SUCCESS! SpaceRust.exe generated in:
    echo   %CD%\SpaceRust.exe
    echo ========================================================
)

pause
