#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

BACKEND_PORT="${BACKEND_PORT:-8081}"
FRONTEND_PORT="${FRONTEND_PORT:-5176}"
LOG_DIR="${COMEBOT_LOG_DIR:-/tmp/comebot-logs}"
mkdir -p "$LOG_DIR"

if ! command -v screen >/dev/null 2>&1; then
  echo "screen is required to run backend and frontend together."
  echo "Run backend: scripts/run-upbit-paper.sh"
  echo "Run frontend: cd frontend && VITE_API_TARGET=http://localhost:$BACKEND_PORT npm run dev -- --host 127.0.0.1 --port $FRONTEND_PORT"
  exit 1
fi

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

if [[ ! -d "frontend/node_modules" ]]; then
  echo "Installing frontend dependencies with npm ci..."
  (cd frontend && npm ci)
fi

screen -S comebot-backend -X quit >/dev/null 2>&1 || true
screen -S comebot-frontend -X quit >/dev/null 2>&1 || true

screen -dmS comebot-backend bash -lc \
  "cd '$PWD' && SERVER_PORT='$BACKEND_PORT' scripts/run-upbit-paper.sh > '$LOG_DIR/backend-$BACKEND_PORT.log' 2>&1"

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
