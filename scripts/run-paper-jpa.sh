#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
elif [[ -d "/opt/homebrew/opt/openjdk@21" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
  export PATH="$JAVA_HOME/bin:$PATH"
elif [[ -z "${JAVA_HOME:-}" ]] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA_21_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    export JAVA_HOME="$JAVA_21_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

if [[ -f ".env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

COMEBOT_PAPER_PROFILE="${COMEBOT_PAPER_PROFILE:-default}"
if [[ "$COMEBOT_PAPER_PROFILE" == "session-volatility" ]]; then
  export STRATEGY_SELECTED="SESSION_VOLATILITY_BREAKOUT"
  export TRADING_ALLOWED_MARKETS="ALL_USDT"
  export TRADING_CANDIDATE_SCHEDULER_ENABLED="false"
  export TRADING_CANDIDATE_SCHEDULER_MARKETS="ALL_USDT"
  export TRADING_CANDIDATE_SCHEDULER_EXCHANGES="BINANCE"
  export TRADING_CANDIDATE_SCHEDULER_MAX_BUYS_PER_RUN="1"
  export TRADING_EXIT_SCHEDULER_ENABLED="false"
  export TRADING_EXIT_SCHEDULER_EXCHANGES="BINANCE"
  export SCHEDULER_CONTROL_RESTORE_ENABLED="false"
fi

export MARKET_PRICE_PROVIDER="${MARKET_PRICE_PROVIDER:-SNAPSHOT}"
export HISTORY_STORAGE_TYPE="JPA"
export PAPER_PORTFOLIO_STORAGE_TYPE="JPA"
export TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE="${TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE:-JPA}"
export SAFETY_KILL_SWITCH_ENABLED="${SAFETY_KILL_SWITCH_ENABLED:-false}"
export MARKET_WEBSOCKET_ENABLED="${MARKET_WEBSOCKET_ENABLED:-true}"
export MARKET_WEBSOCKET_UPBIT_ENABLED="${MARKET_WEBSOCKET_UPBIT_ENABLED:-true}"
export MARKET_WEBSOCKET_BINANCE_ENABLED="${MARKET_WEBSOCKET_BINANCE_ENABLED:-true}"
export MARKET_UPBIT_KRW_TICKER_POLLING_ENABLED="${MARKET_UPBIT_KRW_TICKER_POLLING_ENABLED:-true}"
export MARKET_BINANCE_USDT_TICKER_POLLING_ENABLED="${MARKET_BINANCE_USDT_TICKER_POLLING_ENABLED:-true}"
export TRADING_ALLOWED_MARKETS="${TRADING_ALLOWED_MARKETS:-ALL_KRW,ALL_USDT}"
export TRADING_CANDIDATE_SCHEDULER_ENABLED="${TRADING_CANDIDATE_SCHEDULER_ENABLED:-false}"
export TRADING_CANDIDATE_SCHEDULER_MARKETS="${TRADING_CANDIDATE_SCHEDULER_MARKETS:-ALL_KRW,ALL_USDT}"
export TRADING_CANDIDATE_SCHEDULER_EXCHANGES="${TRADING_CANDIDATE_SCHEDULER_EXCHANGES:-UPBIT,BINANCE}"
export TRADING_EXIT_SCHEDULER_ENABLED="${TRADING_EXIT_SCHEDULER_ENABLED:-false}"
export TRADING_EXIT_SCHEDULER_EXCHANGES="${TRADING_EXIT_SCHEDULER_EXCHANGES:-UPBIT,BINANCE}"
export TELEGRAM_ENABLED="${TELEGRAM_ENABLED:-false}"
export TELEGRAM_INBOUND_ENABLED="${TELEGRAM_INBOUND_ENABLED:-false}"

CONTAINER_NAME="${CONTAINER_NAME:-comebot-postgres}"

if [[ -z "${POSTGRES_PASSWORD:-}" ]] && docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  container_password="$(docker inspect "$CONTAINER_NAME" --format '{{range .Config.Env}}{{println .}}{{end}}' \
    | awk -F= '$1 == "POSTGRES_PASSWORD" {print substr($0, index($0, "=") + 1)}' \
    | tail -n 1)"
  POSTGRES_PASSWORD=${container_password}
  export POSTGRES_PASSWORD
fi

if [[ -z "${POSTGRES_PASSWORD:-}" ]]; then
  echo "POSTGRES_PASSWORD is required in .env or an existing $CONTAINER_NAME container."
  exit 1
fi

if [[ -z "${POSTGRES_PORT:-}" ]]; then
  existing_port="$(docker inspect "$CONTAINER_NAME" \
    --format '{{with (index .NetworkSettings.Ports "5432/tcp")}}{{(index . 0).HostPort}}{{end}}' 2>/dev/null || true)"
  if [[ -n "$existing_port" ]]; then
    POSTGRES_PORT="$existing_port"
  elif lsof -nP -iTCP:5432 -sTCP:LISTEN >/dev/null 2>&1; then
    POSTGRES_PORT=""
    for candidate_port in 5433 5434 5435 5436 5437 5438 5439; do
      if ! lsof -nP -iTCP:"$candidate_port" -sTCP:LISTEN >/dev/null 2>&1; then
        POSTGRES_PORT="$candidate_port"
        break
      fi
    done
    if [[ -z "$POSTGRES_PORT" ]]; then
      echo "No free PostgreSQL host port found in 5433-5439."
      exit 1
    fi
  else
    POSTGRES_PORT="5432"
  fi
  export POSTGRES_PORT
fi

SERVER_PORT="${SERVER_PORT:-18080}"

echo "Starting comebot with Upbit/Binance public market data, PAPER_TRADING, and JPA history/portfolio storage."
echo "REAL_TRADING and real order APIs are not used."
echo "Paper profile: $COMEBOT_PAPER_PROFILE"
echo "Market provider: $MARKET_PRICE_PROVIDER"
echo "Strategy selected: ${STRATEGY_SELECTED:-SIMPLE_THRESHOLD}"
echo "Auto PAPER trading: candidate=$TRADING_CANDIDATE_SCHEDULER_ENABLED exit=$TRADING_EXIT_SCHEDULER_ENABLED"
echo "Candidate exchanges: $TRADING_CANDIDATE_SCHEDULER_EXCHANGES"
echo "Candidate markets: $TRADING_CANDIDATE_SCHEDULER_MARKETS"
echo "Candidate max buys/run: ${TRADING_CANDIDATE_SCHEDULER_MAX_BUYS_PER_RUN:-2}"
echo "Exit exchanges: $TRADING_EXIT_SCHEDULER_EXCHANGES"
echo

docker compose up -d postgres
"$(dirname "$0")/apply-schema.sh"

./gradlew bootRun --args="--server.port=$SERVER_PORT"
