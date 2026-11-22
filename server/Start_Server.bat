@echo off
title SpaceRust Server - UNLIMITED X2 (Radmin VPN)
cd /d "%~dp0"

echo ========================================================
echo   SPACERUST SERVER [UNLIMITED X2] (1.16.5 - Radmin VPN)
echo ========================================================

set RADMIN_IP=26.226.103.84
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias '*Radmin*' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty IPAddress)"`) do set RADMIN_IP=%%i
echo   Radmin VPN IP: %RADMIN_IP%:25565
echo.

set JAVA_CMD=java
if exist "D:\Games\SpaceRust\runtime\jre\bin\java.exe" (
    set JAVA_CMD="D:\Games\SpaceRust\runtime\jre\bin\java.exe"
) else if exist "C:\Program Files\Java\jre-1.8\bin\java.exe" (
    set JAVA_CMD="C:\Program Files\Java\jre-1.8\bin\java.exe"
) else if exist "C:\Program Files\Java\jre1.8.0_351\bin\java.exe" (
    set JAVA_CMD="C:\Program Files\Java\jre1.8.0_351\bin\java.exe"
)

echo Starting Purpur 1.16.5 server with 4GB RAM...
%JAVA_CMD% -Xmx4G -Xms1G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=50 -jar server.jar nogui

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Server stopped.
    pause
)
