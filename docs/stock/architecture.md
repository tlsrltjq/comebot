# Stock Architecture

## Boundary

Stock research is isolated from crypto execution.

- `ExchangeMode` is not extended with stocks.
- Stock identity uses `(assetClass, venue, symbol)`.
- US stock symbols remain plain tickers such as `AAPL`, `MSFT`, and `SPY`.
- US stock currency is `USD`.
- US stock timezone is `America/New_York`.

## Implemented Components

- `MarketAssetClass`: `CRYPTO`, `STOCK`
- `MarketVenue`: `UPBIT`, `BINANCE`, `US_STOCK`
- `MarketIdentity`: normalized market identity tuple
- `CandleInterval`: `1m`, `5m`, `15m`, `1d`
- `StockCandleImportManifest`: provider and file metadata
- `StockCandleRow`: validated OHLCV row
- `StockCandleCsvImporter`: local CSV loader and validator

## Data Flow

```text
local CSV file
  -> StockCandleImportManifest
  -> StockCandleCsvImporter
  -> StockCandleRow list
  -> test-only BacktestSeriesLoader.loadStockSeries(...)
  -> CandleSeries
```

No live stock data provider is wired into runtime automation yet.
