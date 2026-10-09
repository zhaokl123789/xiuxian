@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\start_pack.ps1"
if errorlevel 1 pause
