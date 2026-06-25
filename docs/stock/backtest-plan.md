# Stock Backtest Plan

## Goal

Use imported local stock CSV data to test whether a strategy has enough edge to justify
later PAPER observation work.

## First Experiments

1. Opening Range Breakout
   - Uses the first N minutes of the regular session as the breakout range.
   - Good first fit for liquid ETFs and large-cap stocks.

2. Session Volatility Breakout
   - Adapts the crypto session-volatility idea to the US regular session.
   - Requires separate parameter sweeps; do not reuse crypto parameters blindly.

3. Relative Strength Rotation
   - Compares candidates against `SPY` or `QQQ`.
   - Useful as a market-regime-aware filter.

4. Gap Fade / Gap Follow
   - Tests post-open gap behavior.
   - Requires conservative slippage and fee stress.

## Acceptance Rules

- Train gross edge must be positive enough to survive realistic costs.
- Test/OOS net result must remain above break-even.
- Results must not depend on one ticker only.
- Cost and slippage stress must be included before PAPER observation.

## Next Implementation Step

Build a small deterministic stock backtest around the bundled CSV loader, then replace the
fixture with local provider data when available.

## Implementation Update 2026-06-26

Added a deterministic offline Opening Range Breakout harness in test scope:

- `StockOpeningRangeBreakoutBacktest`
- `StockOpeningRangeBreakoutBacktestTest`

The first harness uses imported stock CSV rows, builds a `CandleSeries`, enters when close
breaks above the opening range high, exits by TP/SL/time, and applies round-trip bps cost.
It is intentionally test-only and does not enable stock PAPER automation.
