# Crypto Architecture

## Boundary

Crypto execution remains separate from stock research.

- `ExchangeMode` is crypto-only and supports `UPBIT` and `BINANCE`.
- `MarketIdentity` can describe crypto markets, but existing scheduler/execution APIs still
  use crypto exchange semantics.
- Crypto PAPER portfolio, history, risk, and scheduler logic must not be reused for stocks
  without an explicit stock boundary.

## Runtime Flow

1. Market data comes from Upbit/Binance public ticker WebSocket clients and REST fallback.
2. Candidate scanners evaluate configured crypto markets.
3. PAPER entry creates simulated orders only.
4. Pending limit orders use maker-style fresh-price checks.
5. Exit scheduler handles PAPER take-profit/stop-loss exits.
6. History, trade journal, scan logs, and portfolio endpoints expose read-only audit data.

## Current Observation

The active crypto research path is Binance Session Volatility Breakout:

- signal interval: 15m
- target session: UTC 06:00-12:00
- candidate market scope: Binance USDT universe
- entry mode: PAPER maker-style pending limit
- automation profile: dedicated Session Volatility Docker restart scripts

Stock automation is OFF and is not part of this runtime path.
