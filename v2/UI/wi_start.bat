@echo off
setlocal

set ROOT=%~dp0

echo.
echo ========================================
echo   Edge AI Wearable Health Monitor
echo   Starting Windows System
echo ========================================
echo.

echo Starting Frontend...
start "Health Monitor - Frontend" cmd /k "cd /d "%ROOT%frontend" && npm install && npm start"

timeout /t 2 /nobreak >nul

echo Starting Windows Backend...
start "Health Monitor - Backend" cmd /k "cd /d "%ROOT%wi_backend" && python -m pip install -r requirements.txt && python main.py"

timeout /t 2 /nobreak >nul

echo Starting BLE Bridge...
start "Health Monitor - BLE Bridge" cmd /k "cd /d "%ROOT%ble_bridge" && python -m pip install -r requirements.txt && python bridge.py"

echo.
echo ========================================
echo   All services started
echo ========================================
echo.
echo Frontend:   http://localhost:3000
echo Backend:    http://localhost:8000
echo.

pause
