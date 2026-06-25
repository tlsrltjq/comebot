# 2026-06-26 Crypto PAPER Observation

## Scope

- Asset class: crypto
- Runtime: Docker app/web/postgres
- Backend: `http://127.0.0.1:18082`
- Web: `http://127.0.0.1:5176`
- PostgreSQL host port: `5434`
- Observation target: Binance Session Volatility PAPER path

## Runtime Status

Docker services were running:

- `comebot-app`: up, host `18082 -> 18080`
- `comebot-web`: up, host `5176 -> 80`
- `comebot-postgres`: up and healthy, host `5434 -> 5432`

The app was not running the Session Volatility override profile at the time of review.
`/api/system/status` reported:

- `strategyName=VolatilityBreakoutLongStrategy`
- `candidateEnabled=false`
- `exitEnabled=false`
- `candidateReadinessWarnings=[]`
- candidate exchanges: `UPBIT`, `BINANCE`
- candidate markets: `ALL_KRW`, `ALL_USDT`

Persisted scheduler control still had `auto_trading_enabled=true`, but Docker startup keeps
`SCHEDULER_CONTROL_RESTORE_ENABLED=false`, so automation was not restored automatically.

## Last 24h Candidate Activity

Database summary for the last 24 hours:

| exchange | decision | count | first UTC | last UTC |
|---|---:|---:|---|---|
| BINANCE | SELECTED | 29 | 2026-06-25 10:15:12 | 2026-06-25 10:29:52 |
| BINANCE | SKIPPED | 64,957 | 2026-06-24 15:33:25 | 2026-06-25 10:32:30 |

All recent `SELECTED` rows were `SPCXBUSDT` with reason:

```text
Session volatility breakout selected: Binance 15m UTC06-12 close-limit
```

The repeated selected rows did not create repeated BUY fills during this observation window;
there was one `SPCXBUSDT` BUY in the last 24 hours.

## Recent Trade Activity

Last 24h trade log:

| exchange | market | side | quantity | price | gross amount | realized profit | executed UTC |
|---|---|---|---:|---:|---:|---:|---|
| BINANCE | SPCXBUSDT | BUY | 0.06287726 | 159.04 | 9.9999994304 |  | 2026-06-25 10:15:13 |

Current open Binance positions:

| market | quantity | avg buy | current price | unrealized profit | unrealized rate |
|---|---:|---:|---:|---:|---:|
| SPCXBUSDT | 0.06287726 | 159.04 | 152.98 | -0.3810361956 | -3.810362% |
| TRXUSDT | 30.23888720 | 0.3307 | 0.3232 | -0.2267916540 | -2.267917% |

Binance portfolio valuation:

- cash: `974.5250239864404 USDT`
- total position value: `19.39217157784 USDT`
- total equity: `993.9171955642804 USDT`
- realized profit: `-5.4749750398968171 USDT`
- unrealized profit: `-0.6078278496 USDT`
- total profit: `-6.0828028894968171 USDT`

## Decision

Do not resume automation from the current default Docker state.

Reasons:

- The app is not currently running the dedicated Session Volatility profile.
- Scheduler automation is currently OFF, which is safer with two open Binance positions.
- `SPCXBUSDT` is below the configured -2% stop-loss style threshold, but exit automation is
  not currently enabled.
- `TRXUSDT` is also below -2% unrealized.

Next operational choice should be explicit:

1. Restart with the dedicated Binance Session Volatility profile and enable exit handling, or
2. Manually close the two open Binance PAPER positions through the selected PAPER SELL path,
   then restart observation cleanly.
