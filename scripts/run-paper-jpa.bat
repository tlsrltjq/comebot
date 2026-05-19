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

if not defined MARKET_PRICE_PROVIDER set "MARKET_PRICE_PROVIDER=SNAPSHOT"
set "HISTORY_STORAGE_TYPE=JPA"
set "PAPER_PORTFOLIO_STORAGE_TYPE=JPA"
if not defined TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE set "TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE=JPA"
if not defined SAFETY_KILL_SWITCH_ENABLED set "SAFETY_KILL_SWITCH_ENABLED=false"
if not defined MARKET_WEBSOCKET_ENABLED set "MARKET_WEBSOCKET_ENABLED=true"
if not defined MARKET_WEBSOCKET_UPBIT_ENABLED set "MARKET_WEBSOCKET_UPBIT_ENABLED=true"
if not defined MARKET_WEBSOCKET_BINANCE_ENABLED set "MARKET_WEBSOCKET_BINANCE_ENABLED=true"
if not defined MARKET_UPBIT_KRW_TICKER_POLLING_ENABLED set "MARKET_UPBIT_KRW_TICKER_POLLING_ENABLED=true"
if not defined MARKET_BINANCE_USDT_TICKER_POLLING_ENABLED set "MARKET_BINANCE_USDT_TICKER_POLLING_ENABLED=true"
if not defined TRADING_ALLOWED_MARKETS set "TRADING_ALLOWED_MARKETS=ALL_KRW,ALL_USDT"
if not defined TRADING_CANDIDATE_SCHEDULER_ENABLED set "TRADING_CANDIDATE_SCHEDULER_ENABLED=true"
if not defined TRADING_CANDIDATE_SCHEDULER_MARKETS set "TRADING_CANDIDATE_SCHEDULER_MARKETS=ALL_KRW,ALL_USDT"
if not defined TRADING_CANDIDATE_SCHEDULER_EXCHANGES set "TRADING_CANDIDATE_SCHEDULER_EXCHANGES=UPBIT,BINANCE"
if not defined TRADING_EXIT_SCHEDULER_ENABLED set "TRADING_EXIT_SCHEDULER_ENABLED=true"
if not defined TRADING_EXIT_SCHEDULER_EXCHANGES set "TRADING_EXIT_SCHEDULER_EXCHANGES=UPBIT,BINANCE"
if not defined TELEGRAM_ENABLED set "TELEGRAM_ENABLED=false"
if not defined TELEGRAM_INBOUND_ENABLED set "TELEGRAM_INBOUND_ENABLED=false"
if not defined SERVER_PORT set "SERVER_PORT=18080"

echo Starting comebot with Upbit/Binance public market data, PAPER_TRADING, and JPA history/portfolio storage.
echo REAL_TRADING and real order APIs are not used.
echo Backend: http://127.0.0.1:%SERVER_PORT%
echo Market provider: %MARKET_PRICE_PROVIDER%
echo Candidate exchanges: %TRADING_CANDIDATE_SCHEDULER_EXCHANGES%
echo Candidate markets: %TRADING_CANDIDATE_SCHEDULER_MARKETS%
echo Exit exchanges: %TRADING_EXIT_SCHEDULER_EXCHANGES%
echo.

docker compose up -d postgres
if errorlevel 1 (
    echo PostgreSQL container could not be started. Check Docker Desktop and docker-compose.yml.
    exit /b 1
)

call scripts\apply-schema.bat
if errorlevel 1 exit /b 1

call gradlew.bat bootRun --args="--server.port=%SERVER_PORT%"
