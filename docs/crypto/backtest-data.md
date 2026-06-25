# Crypto Backtest Data

## Current Layout

Existing crypto backtest JSON files remain flat under:

```text
.backtest_cache/
```

Do not move the existing crypto cache yet. The current Java backtest cache loader expects
flat JSON files directly under `.backtest_cache`.

## Compatibility Documents

The detailed legacy data document remains:

- `docs/trading/BACKTEST_DATA.md`

The leaderboard document remains:

- `docs/trading/BACKTEST_LEADERBOARD.md`

## Deferred Layout

The future target layout is:

```text
.backtest_cache/crypto/upbit/
.backtest_cache/crypto/binance/
```

This is deferred until all loaders and collection scripts support nested crypto paths.
