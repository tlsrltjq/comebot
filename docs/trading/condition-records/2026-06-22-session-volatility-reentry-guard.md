# 2026-06-22 Session Volatility re-entry guard incident

## Context

- Check time: 2026-06-22 08:38 KST
- Profile: `SESSION_VOLATILITY_BREAKOUT`
- Scope: Binance only, `ALL_USDT`
- Candidate interval: 30s
- Exit interval: 5s
- Automation status before intervention: ON

## Runtime status at check

- `candidateEnabled=true`
- `candidateFixedDelayMs=30000`
- `candidateMarkets=[ALL_USDT]`
- `candidateExchanges=[BINANCE]`
- `exitEnabled=true`
- `exitFixedDelayMs=5000`
- `candidateReadinessWarnings=[]`
- Market provider: `SNAPSHOT`, automation ready

Open position at check:

| Market | Quantity | Average buy | Unrealized PnL |
|---|---:|---:|---:|
| `BNBUSDT` | 0.238630840000000000 | 586.680000000000000000 | -0.477261680000000000 |

## Observation result

`FDUSDUSDT` did not re-enter after the 2026-06-17 exclusion patch.

However, the scheduler continued to enter the same selected market repeatedly while a PAPER position already existed.
The observed repeated entries after the FDUSD patch were:

| Market | SELECTED count | BUY trades | BUY gross |
|---|---:|---:|---:|
| `WLDUSDT` | 28 | 27 | 269.999999920080000000 |
| `REUSDT` | 8 | 3 | 29.999999975748000000 |
| `BNBUSDT` | 29 | 14 | 139.999941211200000000 |
| `AVAXUSDT` | 29 | 28 | 279.999998535600000000 |
| `ASTERUSDT` | 57 | 45 | 449.999999916570000000 |
| `SYNUSDT` | 28 | 7 | 69.999999993938000000 |

Confirmed realized outcome before cleanup:

- Binance cash: 854.401031108106400000 USDT
- Realized profit: -5.599026134470817100 USDT
- Total equity: 993.923710639306400000 USDT
- Total profit including open BNB: -6.076287814470817100 USDT

## Root cause

`PositionEntryGuardService` already supports blocking same-market re-entry, but the default setting is:

- `STRATEGY_ENTRY_PREVENT_REENTRY_WITH_POSITION=false`

The Session Volatility observation profile did not override this setting, so repeated scheduler ticks could keep placing BUY orders for a market that was already held after the previous pending limit order filled.

## Actions

1. Stopped candidate/exit automation immediately.
2. Added `STRATEGY_ENTRY_PREVENT_REENTRY_WITH_POSITION=true` to the Session Volatility run profile.
3. Added the same setting to the Docker Compose observation override path.
4. Added readiness warning when `SESSION_VOLATILITY_BREAKOUT` is selected but same-market re-entry prevention is disabled.
5. Rebuilt/restarted the patched Docker `app` container with schedulers OFF.
6. Verified readiness warnings are empty with the patched configuration.
7. Closed the contaminated `BNBUSDT` PAPER position via selected PAPER SELL.
8. Resumed PAPER automation after readiness and network checks passed.

BNB cleanup:

- SELL time: 2026-06-21 23:44:32 UTC
- Quantity: 0.238630840000000000
- Sell price: 584.440000000000000000
- Realized profit: -0.534533081600000000 USDT

Post-cleanup state:

- Binance open positions: 0
- Binance cash: 993.866439237706400000 USDT
- Binance realized profit: -6.133559216070817100 USDT

Automation resumed with:

- `candidateEnabled=true`
- `candidateFixedDelayMs=30000`
- `candidateMarkets=[ALL_USDT]`
- `candidateExchanges=[BINANCE]`
- `exitEnabled=true`
- `exitFixedDelayMs=5000`
- `exitExchanges=[BINANCE]`
- `candidateReadinessWarnings=[]`

## Decision

The repeated BUY observation from 2026-06-19 to 2026-06-20 is contaminated and should not be treated as valid strategy evidence.

Next session check must verify:

- no `FDUSDUSDT` re-entry,
- no same-market repeated BUY while a PAPER position exists,
- valid candidates, if any, produce at most one open position per market.
