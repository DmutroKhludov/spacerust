@echo off
title SpaceRust Website (http://127.0.0.1:8765/site/)
cd /d "%~dp0"
echo Starting SpaceRust website at http://127.0.0.1:8765/site/ ...
start "" "http://127.0.0.1:8765/site/"
python -m http.server 8765 --bind 127.0.0.1
if %ERRORLEVEL% NEQ 0 (
    echo Python HTTP server failed. Opening site\index.html directly...
    start "" "site\index.html"
    pause
)
