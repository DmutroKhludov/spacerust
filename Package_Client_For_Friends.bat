@echo off
title Packaging SpaceRust for Friends...
cd /d "%~dp0"

echo ========================================================
echo   CREATING PORTABLE SPACERUST CLIENT PACKAGE (.ZIP)
echo ========================================================
echo.

python package_client.py

echo.
pause
