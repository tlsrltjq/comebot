@echo off
setlocal

cd /d "%~dp0.."

if not defined BACKEND_PORT set "BACKEND_PORT=18080"
if not defined FRONTEND_PORT set "FRONTEND_PORT=5176"

where java >nul 2>&1
if errorlevel 1 (
    echo Java is not available on PATH. Install Java 21 or set JAVA_HOME.
    exit /b 1
)

if not exist "frontend\node_modules" (
    echo Installing frontend dependencies with npm ci...
    pushd frontend
    call npm ci
    if errorlevel 1 exit /b 1
    popd
)

echo Starting backend and frontend in separate windows.
echo Backend: http://127.0.0.1:%BACKEND_PORT%
echo Web UI:  http://127.0.0.1:%FRONTEND_PORT%
echo.

start "comebot-backend" cmd /k "set SERVER_PORT=%BACKEND_PORT%&& scripts\run-paper-jpa.bat"
start "comebot-web" cmd /k "cd frontend && set VITE_API_TARGET=http://localhost:%BACKEND_PORT%&& npm run dev -- --host 127.0.0.1 --port %FRONTEND_PORT%"
