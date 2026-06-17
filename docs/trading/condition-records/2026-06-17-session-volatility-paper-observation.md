# 2026-06-17 Session Volatility PAPER observation

## Context

- Profile: `SESSION_VOLATILITY_BREAKOUT`
- Scope: Binance only, `ALL_USDT`
- Candidate interval: 30s
- Exit interval: 5s
- Observation time: 2026-06-17 21:46 KST
- Target session: UTC 06:00-12:00 / KST 15:00-21:00

## Runtime status

- Docker containers: `app`, `web`, `postgres` up for about 24h
- Market provider: `SNAPSHOT`
- Automation readiness: ready
- Candidate scheduler before issue stop: ON
- Exit scheduler before issue stop: ON
- Readiness warnings: none

After the issue below was confirmed, automation was stopped:

- `candidateEnabled=false`
- `exitEnabled=false`
- `candidateFixedDelayMs=30000`
- `exitFixedDelayMs=5000`

## Scan and trade summary

KST day window queried as UTC `2026-06-16T15:00:00Z` onward.

| Metric | Value |
|---|---:|
| Binance candidate scan rows | 68,760 |
| `SELECTED` rows | 28 |
| `SKIPPED` rows | 68,732 |
| Latest scan | 2026-06-17 12:46:49 UTC |
| PAPER BUY fills | 28 |
| PAPER SELL fills | 0 |
| BUY gross amount | 279.999999965 USDT |

All `SELECTED` rows were `FDUSDUSDT`:

| Market | Selected | First selected UTC | Latest selected UTC |
|---|---:|---|---|
| `FDUSDUSDT` | 28 | 2026-06-17 10:45:24 | 2026-06-17 10:59:43 |

Open Binance PAPER positions after observation:

| Market | Quantity | Average buy | Peak price |
|---|---:|---:|---:|
| `DASHUSDT` | 0.803858520000000000 | 37.320000000000000000 | 38.200000000000000000 |
| `FDUSDUSDT` | 280.140070000000000000 | 0.999500000000000000 | 0.999200000000000000 |

Portfolio snapshot:

- Cash: 707.102394974955400000 USDT
- Realized profit: 17.102396003703575300 USDT

## Finding

`FDUSDUSDT` is a pegged/stable-style market and should not be eligible for a volatility breakout strategy.
The strategy already excluded `USD1USDT`, `USDCUSDT`, `USDEUSDT`, and `XAUTUSDT`, but the exclusion list missed `FDUSDUSDT`.

This produced repeated PAPER BUY fills during the session. It is PAPER only, but the observation result is not a valid strategy win.

## Action

- Stopped candidate/exit automation to prevent the same market from being bought again tomorrow.
- Added `FDUSDUSDT` to the Session Volatility Breakout default excluded market list.
- Added a regression test that `FDUSDUSDT` is skipped before candle lookup.
- Rebuilt/restarted the patched Docker `app` container with Session Volatility settings and schedulers OFF.
- Verified `GET /api/candidates?exchange=binance&market=FDUSDUSDT` returns `SKIPPED` with `Market is excluded from session volatility universe`.

## Decision

Do not resume Session Volatility PAPER automation until the `FDUSDUSDT` PAPER position handling is decided.
