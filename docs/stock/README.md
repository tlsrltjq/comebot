# Stock PAPER Research Documentation

Stock-specific documentation for US stock PAPER research.

## Scope

- Asset class: `STOCK`
- Venue: `US_STOCK`
- Quote currency: `USD`
- Timezone: `America/New_York`
- Trading mode: `PAPER_TRADING` only
- Live broker/order APIs: forbidden
- Automation: OFF

## Documents

- `architecture.md`: stock model and import/backtest boundary
- `spec.md`: stock universe, sessions, intervals, and risk assumptions
- `data-import.md`: CSV format and manifest validation
- `backtest-plan.md`: first offline strategy experiments
- `provider-decision.md`: provider policy and deferred live API adapters
- `operations.md`: allowed stock research procedures

## Current Status

The implemented stock path is local CSV import only:

- `MarketAssetClass`
- `MarketVenue`
- `MarketIdentity`
- `CandleInterval`
- `StockCandleImportManifest`
- `StockCandleRow`
- `StockCandleCsvImporter`

Backtest loading is currently test-only through `BacktestSeriesLoader.loadStockSeries(...)`.
