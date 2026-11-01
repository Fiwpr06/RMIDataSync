@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title RMI Data Sync Client - Lab 5

echo ========================================================
echo   RMI Data Sync Client - Lab 5
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pclient

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong RMI Client. Vui long kiem tra JDK 21.
    pause
)
