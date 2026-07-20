@echo off
REM Demo batch file - simply calls the PowerShell script
REM This allows running: demo.bat from cmd.exe

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo.ps1"
pause

@REM .\demo.bat