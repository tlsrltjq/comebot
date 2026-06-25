# 2026-06-26 Crypto PAPER Position Cleanup

## Scope

- Asset class: crypto
- Exchange: `BINANCE`
- Action: selected PAPER SELL cleanup
- Reason: open PAPER positions were already below the -2% stop-loss style threshold while
  automation was OFF.

## Positions Closed

Selected PAPER SELL was executed through:

```text
POST /api/portfolio/positions/sell-selected?exchange=BINANCE
```

Request markets:

- `SPCXBUSDT`
- `TRXUSDT`

Result:

| market | status | executed UTC |
|---|---|---|
| SPCXBUSDT | FILLED | 2026-06-25 23:26:14 |
| TRXUSDT | FILLED | 2026-06-25 23:26:14 |

## Trade Log

| market | side | quantity | price | gross amount | realized profit |
|---|---|---:|---:|---:|---:|
| SPCXBUSDT | SELL | 0.06287726 | 153.29 | 9.6384551854 | -0.3615442450 |
| TRXUSDT | SELL | 30.23888720 | 0.3243 | 9.80647111896 | -0.19352887808 |

## Post-Cleanup State

Portfolio valuation after cleanup:

- cash: `993.9699502908004 USDT`
- total position value: `0`
- total equity: `993.9699502908004 USDT`
- realized profit: `-6.0300481629768171 USDT`
- unrealized profit: `0`
- total profit: `-6.0300481629768171 USDT`

Database check:

```text
select exchange, market, quantity from paper_position where quantity <> 0;
```

Result: 0 rows.

## Decision

The Binance PAPER portfolio is now clean. The next observation run should start from the
dedicated Binance Session Volatility profile, not the default Docker profile.
