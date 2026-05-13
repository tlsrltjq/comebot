#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ -d "/opt/homebrew/opt/openjdk@21" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -f ".env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

export MARKET_PRICE_PROVIDER="${MARKET_PRICE_PROVIDER:-UPBIT}"
export HISTORY_STORAGE_TYPE="JPA"
export PAPER_PORTFOLIO_STORAGE_TYPE="JPA"
export TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE="${TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE:-JPA}"
export SAFETY_KILL_SWITCH_ENABLED="${SAFETY_KILL_SWITCH_ENABLED:-false}"
export TRADING_CANDIDATE_SCHEDULER_ENABLED="${TRADING_CANDIDATE_SCHEDULER_ENABLED:-true}"
export TRADING_EXIT_SCHEDULER_ENABLED="${TRADING_EXIT_SCHEDULER_ENABLED:-true}"
export TELEGRAM_ENABLED="${TELEGRAM_ENABLED:-false}"
export TELEGRAM_INBOUND_ENABLED="${TELEGRAM_INBOUND_ENABLED:-false}"

echo "Starting comebot with Upbit public ticker, PAPER_TRADING, and JPA history/portfolio storage."
echo "REAL_TRADING and real order APIs are not used."
echo

docker compose up -d postgres
"$(dirname "$0")/apply-schema.sh"

./gradlew bootRun
