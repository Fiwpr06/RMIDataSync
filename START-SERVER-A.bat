@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title RMI Server Node A (Port 1099, DB: rmisync_db_a) - Lab 5

echo ========================================================
echo   RMI Server Node A (Port 1099, DB: rmisync_db_a) - Lab 5
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pserver-a

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong Server Node A. Vui long kiem tra JDK 21 va MySQL.
    pause
)
