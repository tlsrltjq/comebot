# Stock Spec

## Fixed Rules

- Stock research is PAPER-only.
- Stock automation remains OFF.
- Broker order APIs are forbidden.
- API keys and provider credentials must not be hardcoded.

## Initial Universe

ETF candidates:

- `SPY`
- `QQQ`
- `IWM`

Large-cap stock candidates:

- `AAPL`
- `MSFT`
- `NVDA`
- `TSLA`
- `AMZN`
- `META`
- `GOOGL`

## Intervals

Supported import intervals:

- `1m`
- `5m`
- `15m`
- `1d`

## Session Metadata

Every stock candle import must record:

- provider
- symbol
- interval
- timezone
- regular-session-only flag
- adjusted/raw flag
- since/until range
- collectedAt timestamp
- dataFile path

## Promotion Gate

Stock strategies must pass offline backtest and OOS checks before any PAPER automation
work starts.
