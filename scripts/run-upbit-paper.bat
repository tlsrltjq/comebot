@echo off
setlocal

cd /d "%~dp0.."

if exist "%USERPROFILE%\.jdks\ms-21.0.10\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.10"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if exist ".env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
        if not "%%A"=="" set "%%A=%%B"
    )
)

set "MARKET_PRICE_PROVIDER=UPBIT"
set "HISTORY_STORAGE_TYPE=IN_MEMORY"
set "PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY"
set "SAFETY_KILL_SWITCH_ENABLED=false"

if not defined TRADING_SCHEDULER_ENABLED set "TRADING_SCHEDULER_ENABLED=false"
if not defined TELEGRAM_ENABLED set "TELEGRAM_ENABLED=false"
if not defined TELEGRAM_INBOUND_ENABLED set "TELEGRAM_INBOUND_ENABLED=false"
if not defined SERVER_PORT set "SERVER_PORT=8081"

echo Starting comebot with Upbit public ticker and PAPER_TRADING.
echo REAL_TRADING and real order APIs are not used.
echo Backend: http://127.0.0.1:%SERVER_PORT%
echo.

docker compose up -d postgres
if errorlevel 1 (
    echo PostgreSQL container could not be started. Check Docker Desktop and docker-compose.yml.
    exit /b 1
)

call gradlew.bat bootRun --args="--server.port=%SERVER_PORT%"
