# 2026-06-25 Session Volatility observation

## Context

- Observation profile: `SESSION_VOLATILITY_BREAKOUT`
- Scope: Binance only, `ALL_USDT`
- Candidate interval: 30s
- Exit interval: 5s
- Observation window checked: 2026-06-24 10:59:57 KST to 2026-06-25 08:46:20 KST
- Scheduler status at review: candidate ON, exit ON
- Readiness warnings: none

## Candidate Scan Result

From `candidate_scan_log` since automation resumed:

| Metric | Count |
|---|---:|
| Total scans | 74,520 |
| `SELECTED` | 67 |
| `SKIPPED` | 74,453 |

Selected markets:

| Market | SELECTED count | First selected UTC | Last selected UTC |
|---|---:|---|---|
| `TRXUSDT` | 28 | 2026-06-24 08:45:21 | 2026-06-24 08:59:32 |
| `AAVEUSDT` | 39 | 2026-06-24 11:39:32 | 2026-06-24 11:59:33 |

Top skip reasons:

| Reason | Count |
|---|---:|
| Outside UTC session window | 47,256 |
| Close did not break prior high | 17,660 |
| Market is excluded from session volatility universe | 9,424 |
| Range ratio is below session volatility threshold | 57 |
| Close location is below session volatility threshold | 29 |
| Failed to fetch Binance candle data | 27 |

The candle fetch failures occurred around 2026-06-24 14:06 UTC, outside the entry session,
and did not stop scheduler operation.

## PAPER Trades

Observed PAPER trades:

| Time UTC | Market | Side | Quantity | Price | Gross | Realized PnL |
|---|---|---:|---:|---:|---:|---:|
| 2026-06-24 08:45:27 | `TRXUSDT` | BUY | 30.23888720 | 0.3307 | 9.9999999970 | |
| 2026-06-24 11:57:27 | `AAVEUSDT` | BUY | 0.12992074 | 76.97 | 9.9999993578 | |
| 2026-06-24 13:12:43 | `AAVEUSDT` | SELL | 0.12992074 | 75.20 | 9.7700396480 | -0.2299597098 |

At review time:

| Market | Quantity | Average buy | Current price | Unrealized PnL | Unrealized rate |
|---|---:|---:|---:|---:|---:|
| `TRXUSDT` | 30.23888720 | 0.3307 | 0.3272 | -0.1058361052 | -1.058361% |

Binance PAPER portfolio at review:

- Cash: 984.525023416840400000 USDT
- Position value: 9.894163891840000000 USDT
- Total equity: 994.419187308680400000 USDT
- Cumulative realized PnL: -5.474975039896817100 USDT
- Current unrealized PnL: -0.105836105200000000 USDT
- Cumulative total PnL including open position: -5.580811145096817100 USDT

This observation period's incremental result:

- Realized PnL: -0.229959709800000000 USDT
- Current unrealized PnL: -0.105836105200000000 USDT
- Realized + current unrealized: -0.335795815000000000 USDT

## Repeated Market Behavior

No repeated FILLED BUY occurred for an already held market.

However, `AAVEUSDT` placed repeated pending limit entries before the order filled:

| Market | Time UTC | Status |
|---|---|---|
| `AAVEUSDT` | 2026-06-24 11:39 | REQUESTED |
| `AAVEUSDT` | 2026-06-24 11:44 | REQUESTED |
| `AAVEUSDT` | 2026-06-24 11:50 | REQUESTED |
| `AAVEUSDT` | 2026-06-24 11:55 | REQUESTED |
| `AAVEUSDT` | 2026-06-24 11:57 | FILLED |

Root interpretation:

- The pending-order guard prevented duplicate concurrent pending entries.
- After an unfilled pending order expired, the next scheduler scan could request the same market again.
- This created unnecessary same-session retry noise even though it did not create duplicate filled positions.

## Action

Added session market cooldown in `CandidateExecutionService`:

- Once candidate execution places a limit entry for an `exchange+market`, that market cannot
  place another limit entry until the current UTC session ends.
- Candidate scans are still recorded; only repeated limit entry requests are blocked.
- The cooldown is in-memory PAPER observation state and resets on app restart or next session.

Validation:

- `./gradlew.bat test --tests com.giseop.comebot.strategy.candidate.CandidateExecutionServiceTest` passed.
- `./gradlew.bat test checkstyleMain` passed.

## Decision

Do not expand the strategy yet.

The observation produced valid candidates and PAPER trades, but the realized result is negative
and the only open position was also negative at review time. Continue Binance-only PAPER
observation with the new session market cooldown, then reassess after the open `TRXUSDT`
position closes and at least one additional UTC 06-12 session is observed.
