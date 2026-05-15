#!/usr/bin/env bash
set -euo pipefail

TIMEOUT_SECONDS="${MARKET_DIAG_TIMEOUT_SECONDS:-10}"

echo "comebot market network diagnostics"
echo "Timestamp: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo "Host: $(hostname 2>/dev/null || echo unknown)"
echo "OS: $(uname -srm 2>/dev/null || echo unknown)"
echo

command_status() {
  local command_name="$1"
  if command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name: $(command -v "$command_name")"
  else
    echo "$command_name: missing"
  fi
}

command_status curl
command_status openssl
command_status nslookup
command_status dig
echo

run_with_timeout() {
  local seconds="$1"
  shift
  "$@" &
  local pid=$!
  local waited=0
  while kill -0 "$pid" >/dev/null 2>&1; do
    if (( waited >= seconds )); then
      kill "$pid" >/dev/null 2>&1 || true
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 1
    waited=$((waited + 1))
  done
  wait "$pid"
}

dns_check() {
  local host="$1"
  echo "== DNS $host =="
  if command -v dig >/dev/null 2>&1; then
    dig +time="$TIMEOUT_SECONDS" +tries=1 "$host" A
    dig +time="$TIMEOUT_SECONDS" +tries=1 "$host" AAAA
  elif command -v nslookup >/dev/null 2>&1; then
    nslookup "$host"
  else
    echo "SKIP: dig/nslookup not available"
  fi
  echo
}

tcp_tls_check() {
  local host="$1"
  echo "== TLS $host:443 =="
  if ! command -v openssl >/dev/null 2>&1; then
    echo "SKIP: openssl not available"
    echo
    return
  fi
  if run_with_timeout "$TIMEOUT_SECONDS" openssl s_client -connect "$host:443" -servername "$host" -brief </dev/null; then
    echo "TLS_OK $host"
  else
    echo "TLS_FAIL $host"
  fi
  echo
}

http_check() {
  local name="$1"
  local url="$2"
  echo "== HTTPS $name =="
  if ! command -v curl >/dev/null 2>&1; then
    echo "SKIP: curl not available"
    echo
    return
  fi
  local status
  status="$(curl --silent --show-error --location --max-time "$TIMEOUT_SECONDS" \
    --output /dev/null \
    --write-out 'http_code=%{http_code} remote_ip=%{remote_ip} ssl_verify=%{ssl_verify_result} time_connect=%{time_connect} time_appconnect=%{time_appconnect} time_total=%{time_total}\n' \
    "$url" 2>&1)" || {
      echo "$status"
      echo "HTTPS_FAIL $name"
      echo
      return
    }
  echo "$status"
  echo "HTTPS_OK $name"
  echo
}

websocket_tls_check() {
  local name="$1"
  local host="$2"
  echo "== WSS TLS $name =="
  if ! command -v openssl >/dev/null 2>&1; then
    echo "SKIP: openssl not available"
    echo
    return
  fi
  if run_with_timeout "$TIMEOUT_SECONDS" openssl s_client -connect "$host:443" -servername "$host" -brief </dev/null; then
    echo "WSS_TLS_OK $name"
  else
    echo "WSS_TLS_FAIL $name"
  fi
  echo
}

dns_check api.upbit.com
dns_check api.binance.com
dns_check stream.binance.com

tcp_tls_check api.upbit.com
tcp_tls_check api.binance.com
tcp_tls_check stream.binance.com

http_check "Upbit market list" "https://api.upbit.com/v1/market/all"
http_check "Upbit ticker all" "https://api.upbit.com/v1/ticker/all"
http_check "Binance time" "https://api.binance.com/api/v3/time"
http_check "Binance 24h ticker" "https://api.binance.com/api/v3/ticker/24hr"

websocket_tls_check "Upbit WebSocket" "api.upbit.com"
websocket_tls_check "Binance WebSocket" "stream.binance.com"

echo "Interpretation:"
echo "- DNS_FAIL or no IP: local DNS/network resolver issue."
echo "- TLS_FAIL: TLS interception, firewall, proxy, certificate, or regional network issue."
echo "- HTTPS_FAIL with curl code 35/56/reset: remote connection reset or middlebox reset."
echo "- HTTPS_OK but app snapshots are empty: inspect application WebSocket/universe bootstrap logs."
echo "- This script does not read .env and does not print secrets."
