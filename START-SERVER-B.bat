@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title RMIDataSync Server Node B (Port 1100, DB: rmisync_db_b)

echo ========================================================
echo   RMIDataSync Server Node B (Port 1100, DB: rmisync_db_b)
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pserver-b

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong Server Node B. Vui long kiem tra JDK 21 va MySQL.
    pause
)
