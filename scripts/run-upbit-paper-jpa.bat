@echo off
setlocal

cd /d "%~dp0.."

echo scripts\run-upbit-paper-jpa.bat is a compatibility alias.
echo Use scripts\run-paper-jpa.bat for long-running Upbit/Binance PAPER operation.
echo.

call scripts\run-paper-jpa.bat %*
