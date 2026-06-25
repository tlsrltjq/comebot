# Stock Operations

## Current Allowed Flow

1. Place local CSV files under `.backtest_cache/stock/us/{provider}/{interval}/{symbol}.csv`.
2. Create a `StockCandleImportManifest`.
3. Validate with `StockCandleCsvImporter`.
4. Run offline backtest code against imported rows.
5. Record results before considering PAPER observation.

## Current Forbidden Flow

- no live stock API scheduler
- no stock PAPER automation
- no broker order API
- no real order mode
- no credentials in source code

## Records

When stock backtests or future PAPER observation starts, add new condition records under:

```text
docs/trading/condition-records/
```

Use a `stock-` prefix for new files.
