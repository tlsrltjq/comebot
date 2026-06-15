@echo off
setlocal

cd /d "%~dp0.."

set "COMEBOT_PAPER_PROFILE=session-volatility"

echo Starting Session Volatility Breakout PAPER observation profile.
echo Schedulers start disabled; use scripts\resume-paper-auto.bat after readiness checks pass.
echo.

call scripts\run-paper-jpa.bat %*
