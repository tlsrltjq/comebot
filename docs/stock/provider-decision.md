# Stock Provider Decision

## Decision

Start stock research with local CSV import, not a live API-backed provider.

## Reasoning

- Provider plans, credentials, rate limits, and historical intraday access differ by vendor.
- Strategy/backtest code should not depend on one paid API shape.
- Local CSV import lets the repository validate identity, session, timezone, adjusted/raw,
  and cost assumptions first.

## Deferred Providers

Potential later API adapters:

- Alpha Vantage
- Polygon/Massive
- other US market-data vendors

Any future API provider must adapt into the same manifest and row shape before strategy code
consumes it.

## Forbidden

- hardcoded API keys
- broker order APIs
- stock `REAL_TRADING`
- enabling stock automation before offline evidence exists
