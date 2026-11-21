@echo off
title SpaceRust Server - UNLIMITED X2
cd /d "%~dp0"

echo ========================================================
echo   SPACERUST LOCAL SERVER [UNLIMITED X2] (1.16.5)
echo ========================================================

set JAVA_CMD=java
if exist "C:\Program Files\Java\jre1.8.0_351\bin\java.exe" (
    set JAVA_CMD="C:\Program Files\Java\jre1.8.0_351\bin\java.exe"
)

echo Starting Purpur 1.16.5 server with 4GB RAM...
%JAVA_CMD% -Xmx4G -Xms1G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=50 -jar server.jar nogui

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Server stopped.
    pause
)
