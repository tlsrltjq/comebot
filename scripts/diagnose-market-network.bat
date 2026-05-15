@echo off
setlocal

if not defined MARKET_DIAG_TIMEOUT_SECONDS set "MARKET_DIAG_TIMEOUT_SECONDS=10"

echo comebot market network diagnostics
echo Timestamp: %DATE% %TIME%
echo Host: %COMPUTERNAME%
echo.

where curl >nul 2>&1
if errorlevel 1 (
    echo curl: missing
) else (
    for /f "delims=" %%P in ('where curl') do echo curl: %%P
)

where nslookup >nul 2>&1
if errorlevel 1 (
    echo nslookup: missing
) else (
    for /f "delims=" %%P in ('where nslookup') do echo nslookup: %%P
)
echo.

call :dns api.upbit.com
call :dns api.binance.com
call :dns stream.binance.com

call :https "Upbit market list" "https://api.upbit.com/v1/market/all"
call :https "Upbit ticker all" "https://api.upbit.com/v1/ticker/all"
call :https "Binance time" "https://api.binance.com/api/v3/time"
call :https "Binance 24h ticker" "https://api.binance.com/api/v3/ticker/24hr"

echo Interpretation:
echo - DNS failure or no IP: local DNS/network resolver issue.
echo - curl SSL/connect errors: TLS interception, firewall, proxy, certificate, or regional network issue.
echo - HTTPS OK but app snapshots are empty: inspect application WebSocket/universe bootstrap logs.
echo - This script does not read .env and does not print secrets.
exit /b 0

:dns
echo == DNS %~1 ==
where nslookup >nul 2>&1
if errorlevel 1 (
    echo SKIP: nslookup not available
) else (
    nslookup %~1
)
echo.
exit /b 0

:https
echo == HTTPS %~1 ==
where curl >nul 2>&1
if errorlevel 1 (
    echo SKIP: curl not available
) else (
    curl --silent --show-error --location --max-time %MARKET_DIAG_TIMEOUT_SECONDS% --output NUL --write-out "http_code=%%{http_code} remote_ip=%%{remote_ip} ssl_verify=%%{ssl_verify_result} time_connect=%%{time_connect} time_appconnect=%%{time_appconnect} time_total=%%{time_total}\n" %~2
    if errorlevel 1 (
        echo HTTPS_FAIL %~1
    ) else (
        echo HTTPS_OK %~1
    )
)
echo.
exit /b 0
