#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

API_BASE="${COMEBOT_API_BASE:-http://127.0.0.1:18080}"
CANDIDATE_INTERVAL_MS="${CANDIDATE_INTERVAL_MS:-30000}"
DIAG_TIMEOUT_SECONDS="${MARKET_DIAG_TIMEOUT_SECONDS:-10}"
SKIP_NETWORK_DIAG="${SKIP_NETWORK_DIAG:-false}"

if [[ "$CANDIDATE_INTERVAL_MS" != "30000" && "$CANDIDATE_INTERVAL_MS" != "60000" ]]; then
  echo "CANDIDATE_INTERVAL_MS must be 30000 or 60000."
  exit 1
fi

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required."
    exit 1
  fi
}

require_command curl

echo "Checking comebot backend: $API_BASE"
system_status="$(curl --silent --fail --max-time 5 "$API_BASE/api/system/status" || true)"
if [[ -z "$system_status" ]]; then
  echo "Backend is not reachable. Start it with scripts/run-local-dev.sh first."
  exit 1
fi
echo "System status: $system_status"
if [[ "$system_status" != *'"candidateReadinessWarnings":[]'* ]]; then
  echo "Candidate scheduler readiness warnings are present. Auto PAPER remains disabled."
  echo "Fix /api/system/status scheduler.candidateReadinessWarnings before resuming."
  exit 1
fi

if [[ "$SKIP_NETWORK_DIAG" != "true" ]]; then
  echo "Running market network diagnostics before enabling automation..."
  diag_file="$(mktemp)"
  MARKET_DIAG_TIMEOUT_SECONDS="$DIAG_TIMEOUT_SECONDS" scripts/diagnose-market-network.sh >"$diag_file" 2>&1 || {
    cat "$diag_file"
    rm -f "$diag_file"
    echo "Market network diagnostics failed. Auto PAPER remains disabled."
    exit 1
  }
  if grep -E "(DNS_FAIL|TLS_FAIL|HTTPS_FAIL|WSS_TLS_FAIL)" "$diag_file" >/dev/null 2>&1; then
    cat "$diag_file"
    rm -f "$diag_file"
    echo "Market network diagnostics found failures. Auto PAPER remains disabled."
    exit 1
  fi
  rm -f "$diag_file"
fi

provider_status="$(curl --silent --fail --max-time 5 "$API_BASE/api/market-provider/status")"
echo "Provider status: $provider_status"
if [[ "$provider_status" != *'"automationReady":true'* ]]; then
  echo "Market provider is not ready for automation. Auto PAPER remains disabled."
  exit 1
fi

echo "Enabling candidate and exit schedulers..."
curl --silent --fail --max-time 5 \
  -X PUT "$API_BASE/api/scheduler/control" \
  -H "Content-Type: application/json" \
  -d "{\"autoTradingEnabled\":true,\"candidateFixedDelayMs\":$CANDIDATE_INTERVAL_MS}"
echo
echo "Auto PAPER resumed."
