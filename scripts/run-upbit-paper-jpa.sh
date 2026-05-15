#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "scripts/run-upbit-paper-jpa.sh is a compatibility alias."
echo "Use scripts/run-paper-jpa.sh for long-running Upbit/Binance PAPER operation."
echo

exec scripts/run-paper-jpa.sh "$@"
