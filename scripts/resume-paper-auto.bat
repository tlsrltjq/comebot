@echo off
setlocal

cd /d "%~dp0.."

if not defined COMEBOT_API_BASE set "COMEBOT_API_BASE=http://127.0.0.1:18080"
if not defined CANDIDATE_INTERVAL_MS set "CANDIDATE_INTERVAL_MS=30000"
if not defined MARKET_DIAG_TIMEOUT_SECONDS set "MARKET_DIAG_TIMEOUT_SECONDS=10"
if not defined SKIP_NETWORK_DIAG set "SKIP_NETWORK_DIAG=false"

if not "%CANDIDATE_INTERVAL_MS%"=="30000" if not "%CANDIDATE_INTERVAL_MS%"=="60000" (
    echo CANDIDATE_INTERVAL_MS must be 30000 or 60000.
    exit /b 1
)

where curl >nul 2>&1
if errorlevel 1 (
    echo curl is required.
    exit /b 1
)

echo Checking comebot backend: %COMEBOT_API_BASE%
set "SYSTEM_FILE=%TEMP%\comebot-system-%RANDOM%.json"
curl --silent --fail --max-time 5 "%COMEBOT_API_BASE%/api/system/status" > "%SYSTEM_FILE%"
if errorlevel 1 (
    del "%SYSTEM_FILE%" >nul 2>&1
    echo Backend is not reachable. Start it with scripts\run-local-dev.bat first.
    exit /b 1
)
type "%SYSTEM_FILE%"
echo.
findstr /C:"\"candidateReadinessWarnings\":[]" "%SYSTEM_FILE%" >nul
if errorlevel 1 (
    del "%SYSTEM_FILE%" >nul 2>&1
    echo Candidate scheduler readiness warnings are present. Auto PAPER remains disabled.
    echo Fix /api/system/status scheduler.candidateReadinessWarnings before resuming.
    exit /b 1
)
del "%SYSTEM_FILE%" >nul 2>&1

if /i not "%SKIP_NETWORK_DIAG%"=="true" (
    echo Running market network diagnostics before enabling automation...
    set "DIAG_FILE=%TEMP%\comebot-market-diagnostic-%RANDOM%.txt"
    call scripts\diagnose-market-network.bat > "%DIAG_FILE%" 2>&1
    if errorlevel 1 (
        type "%DIAG_FILE%"
        del "%DIAG_FILE%" >nul 2>&1
        echo Market network diagnostics failed. Auto PAPER remains disabled.
        exit /b 1
    )
    findstr /R /C:"^DNS_FAIL" /C:"^TLS_FAIL" /C:"^HTTPS_FAIL" /C:"^WSS_TLS_FAIL" "%DIAG_FILE%" >nul
    if not errorlevel 1 (
        type "%DIAG_FILE%"
        del "%DIAG_FILE%" >nul 2>&1
        echo Market network diagnostics found failures. Auto PAPER remains disabled.
        exit /b 1
    )
    del "%DIAG_FILE%" >nul 2>&1
)

set "PROVIDER_FILE=%TEMP%\comebot-provider-%RANDOM%.json"
curl --silent --fail --max-time 5 "%COMEBOT_API_BASE%/api/market-provider/status" > "%PROVIDER_FILE%"
if errorlevel 1 (
    del "%PROVIDER_FILE%" >nul 2>&1
    echo Provider status is not reachable. Auto PAPER remains disabled.
    exit /b 1
)
type "%PROVIDER_FILE%"
echo.
findstr /C:"\"automationReady\":true" "%PROVIDER_FILE%" >nul
if errorlevel 1 (
    del "%PROVIDER_FILE%" >nul 2>&1
    echo Market provider is not ready for automation. Auto PAPER remains disabled.
    exit /b 1
)
del "%PROVIDER_FILE%" >nul 2>&1

echo Enabling candidate and exit schedulers...
curl --silent --fail --max-time 5 -X PUT "%COMEBOT_API_BASE%/api/scheduler/control" -H "Content-Type: application/json" -d "{\"autoTradingEnabled\":true,\"candidateFixedDelayMs\":%CANDIDATE_INTERVAL_MS%}"
echo.
echo Auto PAPER resumed.
