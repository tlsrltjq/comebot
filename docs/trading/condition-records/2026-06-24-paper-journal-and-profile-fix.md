# 2026-06-24 PAPER journal and profile fix

## Summary

- Closed contaminated Upbit PAPER positions that were created during a short default-profile restart window.
- Added a SELL-only PAPER position exit path so held positions can be closed even when the active entry allow-list excludes that market.
- Changed the trade journal source of truth from `trading_flow_history` FIFO matching to `paper_trade_log` realized SELL rows.
- Added Docker restart scripts for the Binance Session Volatility PAPER observation profile.

## Position Cleanup

The contaminated Upbit PAPER positions were closed through:

```http
POST /api/portfolio/positions/sell-selected?exchange=UPBIT
{"markets":["KRW-ARX","KRW-BORA","KRW-MOVE","KRW-TT"]}
```

Result:

- requested: 4
- succeeded: 4
- failed: 0
- nonzero PAPER positions after cleanup: 0

## Trade Journal Fix

Previous behavior:

- `/api/analytics/matched-trades` matched `trading_flow_history` BUY and SELL events by market using a loose FIFO model.
- When a position had multiple buys and one average-cost SELL, the displayed buy price could be unrelated to the actual cost basis.
- This produced rows where profit rate was positive while the SELL reason showed stop loss.

Current behavior:

- `/api/analytics/matched-trades` reads `paper_trade_log`.
- SELL rows with `realized_profit` are treated as closed trades.
- Profit rate is derived from realized PnL:

```text
costBasis = grossAmount - realizedProfit
profitRatePct = realizedProfit / costBasis * 100
```

- Exit label is derived from `realized_profit` sign:
  - positive: `TAKE_PROFIT`
  - negative: `STOP_LOSS`
  - zero/null: `MANUAL`

Verified example:

- Old `DEXEUSDT` display: positive profit rate with `STOP_LOSS`
- New `DEXEUSDT` display: `-2.2028%` with `STOP_LOSS`

## Docker Profile Fix

Plain `docker compose up -d app web` can start the app with compose defaults unless the required observation environment is present. To avoid accidental default-profile automation:

- `SCHEDULER_CONTROL_RESTORE_ENABLED` compose default is now `false`.
- Use the dedicated scripts:

```powershell
scripts\restart-session-volatility-docker.bat
```

```bash
scripts/restart-session-volatility-docker.sh
```

Schedulers start disabled after restart. Enable automation only after `/api/system/status` has empty `candidateReadinessWarnings`.

## Validation

- `./gradlew.bat test checkstyleMain` passed.
- App image was rebuilt and restarted with the Binance Session Volatility PAPER profile.
- `/api/analytics/matched-trades?exchange=BINANCE&limit=100` verified corrected realized-PnL output.
