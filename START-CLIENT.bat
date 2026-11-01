@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title RMIDataSync Client Desktop App

echo ========================================================
echo   RMIDataSync Client Desktop App
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pclient

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong RMI Client. Vui long kiem tra JDK 21.
    pause
)
