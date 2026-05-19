#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

BACKEND_PORT="${BACKEND_PORT:-18080}"
FRONTEND_PORT="${FRONTEND_PORT:-5176}"
LOG_DIR="${COMEBOT_LOG_DIR:-/tmp/comebot-logs}"
mkdir -p "$LOG_DIR"

stop_owned_listener() {
  local port="$1"
  local pids
  pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -z "$pids" ]]; then
    return 0
  fi

  local pid command
  for pid in $pids; do
    command="$(ps -p "$pid" -o command= 2>/dev/null || true)"
    if [[ "$command" == *"$PWD"* || "$command" == *"com.giseop.comebot.ComebotApplication"* ]]; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
  done
}

if ! command -v screen >/dev/null 2>&1; then
  echo "screen is required to run backend and frontend together."
  echo "Run backend: scripts/run-paper-jpa.sh"
  echo "Run frontend: cd frontend && VITE_API_TARGET=http://localhost:$BACKEND_PORT npm run dev -- --host 127.0.0.1 --port $FRONTEND_PORT"
  exit 1
fi

if [[ ! -d "frontend/node_modules" ]]; then
  echo "Installing frontend dependencies with npm ci..."
  (cd frontend && npm ci)
fi

screen -S comebot-backend -X quit >/dev/null 2>&1 || true
screen -S comebot-frontend -X quit >/dev/null 2>&1 || true
stop_owned_listener "$BACKEND_PORT"
stop_owned_listener "$FRONTEND_PORT"

for _ in {1..10}; do
  if ! lsof -nP -iTCP:"$BACKEND_PORT" -sTCP:LISTEN >/dev/null 2>&1 \
    && ! lsof -nP -iTCP:"$FRONTEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if lsof -nP -iTCP:"$BACKEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Backend port $BACKEND_PORT is already in use."
  echo "Set BACKEND_PORT to another value or stop the existing process."
  exit 1
fi

if lsof -nP -iTCP:"$FRONTEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Frontend port $FRONTEND_PORT is already in use."
  echo "Set FRONTEND_PORT to another value or stop the existing process."
  exit 1
fi

screen -dmS comebot-backend bash -lc \
  "cd '$PWD' && SERVER_PORT='$BACKEND_PORT' scripts/run-paper-jpa.sh > '$LOG_DIR/backend-$BACKEND_PORT.log' 2>&1"

for _ in {1..30}; do
  if curl -fsS "http://127.0.0.1:$BACKEND_PORT/api/system/status" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

screen -dmS comebot-frontend bash -lc \
  "cd '$PWD/frontend' && VITE_API_TARGET='http://localhost:$BACKEND_PORT' ./node_modules/.bin/vite --host 127.0.0.1 --port '$FRONTEND_PORT' > '$LOG_DIR/frontend-$FRONTEND_PORT.log' 2>&1"

echo "Backend:  http://127.0.0.1:$BACKEND_PORT"
echo "Web UI:   http://127.0.0.1:$FRONTEND_PORT"
echo "Logs:     $LOG_DIR"
echo
echo "Stop:"
echo "  screen -S comebot-backend -X quit"
echo "  screen -S comebot-frontend -X quit"
