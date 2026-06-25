# Crypto Operations

## Ports

- backend host port: `18082`
- web host port: `5176`
- PostgreSQL host port: `5434`

## Docker Restart

Use the dedicated Binance Session Volatility profile restart path when restoring crypto
observation.

Known scripts:

- `scripts/restart-session-volatility-docker.bat`
- `scripts/restart-session-volatility-docker.sh`
- `scripts/resume-paper-auto.sh`
- `scripts/resume-paper-auto.bat`

## Readiness Checks

Before enabling PAPER automation:

1. Confirm `/api/system/status` has no `candidateReadinessWarnings`.
2. Confirm candidate scope is Binance-only for Session Volatility observation.
3. Confirm exit scheduler scope includes Binance.
4. Confirm no contaminated or unintended positions are open.

## Records

Crypto operation and strategy observations remain under:

- `docs/trading/condition-records/`

New records should use a clear prefix such as `crypto-session-volatility-...`.
