#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

export COMEBOT_PAPER_PROFILE="session-volatility"

echo "Starting Session Volatility Breakout PAPER observation profile."
echo "Schedulers start disabled; use scripts/resume-paper-auto.sh after readiness checks pass."
echo

exec scripts/run-paper-jpa.sh "$@"
