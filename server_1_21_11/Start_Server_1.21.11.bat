@echo off
cd /d "%~dp0"
title SpaceRust 1.21.11 Fabric Server
"C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -Xmx4G -Xms2G -XX:+UseG1GC -jar fabric-server-launch.jar nogui
pause
