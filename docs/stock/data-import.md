# Stock Data Import

## Cache Path

Stock CSV files use:

```text
.backtest_cache/stock/us/{provider}/{interval}/{symbol}.csv
```

## Required CSV Columns

```text
timestamp,open,high,low,close,volume
```

## Validation Rules

- `timestamp` must be an ISO-8601 instant.
- `open`, `high`, `low`, and `close` must be positive.
- `volume` must be zero or positive.
- `high >= low`.
- `open` and `close` must be inside the high/low range.
- rows must be strictly increasing by timestamp.
- row timestamps must satisfy `since <= timestamp < until` from the manifest.
- at least one data row is required.

## Implemented Java Types

- `StockCandleImportManifest`
- `StockCandleRow`
- `StockCandleCsvImporter`

## Sample Fixture

The bundled test fixture is:

```text
src/test/resources/stock/us/sample/15m/AAPL.csv
```
