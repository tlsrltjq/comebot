# Crypto PAPER Documentation

Crypto-specific documentation for Upbit/Binance PAPER trading.

## Scope

- Asset class: `CRYPTO`
- Venues: `UPBIT`, `BINANCE`
- Trading mode: `PAPER_TRADING` only
- Live order APIs: forbidden
- Current observation focus: Binance Session Volatility Breakout

## Documents

- `architecture.md`: crypto data flow, scheduler, PAPER portfolio, and execution modules
- `spec.md`: crypto strategy, risk, order, and scheduler policy
- `operations.md`: Docker restart, automation, and observation procedures
- `backtest-data.md`: crypto `.backtest_cache` layout and retention policy

## Legacy Documents

The existing `docs/trading/` documents remain valid for compatibility:

- `docs/trading/STRATEGY_POLICY.md`
- `docs/trading/RISK_POLICY.md`
- `docs/trading/ORDER_LIFECYCLE.md`
- `docs/trading/BACKTEST_DATA.md`
- `docs/trading/BACKTEST_LEADERBOARD.md`
- `docs/trading/condition-records/`

Do not move or rewrite existing condition records. Add new records with a clear crypto or
stock prefix when needed.
