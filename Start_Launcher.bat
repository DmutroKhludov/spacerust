@echo off
title SpaceRust Launcher
cd /d "%~dp0"
python run_launcher.py
if %ERRORLEVEL% NEQ 0 (
    echo Python launch failed. Opening in browser...
    start "" "index.html"
)
