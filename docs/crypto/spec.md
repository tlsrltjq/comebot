# Crypto Spec

## Fixed Rules

- Trading mode is always `PAPER_TRADING`.
- `REAL_TRADING` is forbidden.
- Actual Upbit/Binance order APIs are forbidden.
- Manual BUY UI and direct run endpoints remain forbidden for production operation.

## Strategy Policy

The crypto strategy source of truth remains:

- `docs/trading/STRATEGY_POLICY.md`
- `docs/trading/condition-records/`

Current PAPER observation strategy:

- `SESSION_VOLATILITY_BREAKOUT`
- Binance only
- 15m signal candles
- UTC 06:00-12:00 session
- no-pegged market exclusions
- same-market re-entry guard required during observation

## Risk Policy

The crypto risk source of truth remains:

- `docs/trading/RISK_POLICY.md`

Key runtime rules:

- exchange-level position limits
- total position limit
- daily loss and order count limits
- kill switch
- duplicate pending order guard
- PAPER-only selected position exit for cleanup

## Order Lifecycle

The crypto order lifecycle source of truth remains:

- `docs/trading/ORDER_LIFECYCLE.md`

Trade journal matched trades are derived from `paper_trade_log` SELL rows and realized
profit, not FIFO reconstruction from `trading_flow_history`.
