# comebot

`comebot` is a coin trading bot project focused on safe PAPER_TRADING validation.
It uses public market data, evaluates long-only trading signals, applies risk checks, records history, and manages a paper portfolio.

This project does not implement real exchange orders or `REAL_TRADING`.

## Project Status

- Language: Java 21
- Base package: `com.giseop.comebot`
- Default trading mode: `PAPER_TRADING`
- Market price providers: `IN_MEMORY`, `UPBIT`
- Candle provider: Upbit public Candle API
- History storage: `IN_MEMORY` by default, optional `JPA`
- Portfolio storage: `IN_MEMORY`
- Scheduler: disabled by default
- Telegram inbound/outbound: disabled by default

## What This Project Does

- Fetches test prices or Upbit public prices
- Fetches recent Upbit minute candles
- Scans long-only paper trading candidates
- Creates BUY, SELL, HOLD signals
- Runs risk validation before paper orders
- Executes paper orders only
- Updates paper cash, positions, realized profit
- Calculates portfolio valuation from current prices
- Stores trading flow history
- Sends optional notifications
- Supports Telegram commands and inline buttons

## What This Project Does Not Do

- No real order API
- No `REAL_TRADING`
- No Upbit Access Key or Secret Key
- No account balance sync from an exchange
- No margin, leverage, or short selling
- No profit guarantee

## Strategy Direction

The current `SimpleThresholdStrategy` is only a test strategy.

The target strategy is:

1. Watch public market data.
2. Track volatility and short-term trend.
3. Select long-only candidates.
4. Enter only through PAPER_TRADING.
5. Take profit after a configured gain.
6. Stop loss after a configured loss.
7. Reject orders that fail risk checks.

See [Strategy Policy](docs/trading/STRATEGY_POLICY.md).

## Quick Start

Start PostgreSQL:

```bat
docker compose up -d postgres
```

Run the app:

```bat
gradlew.bat bootRun
```

Run with Upbit public ticker data and PAPER_TRADING:

```bat
scripts\run-upbit-paper.bat
```

## Main APIs

Status:

```http
GET /api/system/status
GET /api/database/status
GET /api/market-provider/status
GET /api/strategy/status
GET /api/risk/status
```

Trading flow:

```http
GET /api/trading-flow/run?market=KRW-BTC
GET /api/trading-flow/history
GET /api/trading-flow/history?market=KRW-BTC
```

Portfolio:

```http
GET /api/portfolio/status
GET /api/portfolio/positions
GET /api/portfolio/valuation
```

Telegram:

```http
GET /api/telegram/status
POST /api/telegram/test-message
```

## Configuration

Use `.env` or environment variables.

```properties
MARKET_PRICE_PROVIDER=UPBIT
HISTORY_STORAGE_TYPE=IN_MEMORY
PAPER_INITIAL_CASH=1000000
TRADING_ALLOWED_MARKETS=KRW-BTC,KRW-ETH
TRADING_MAX_ORDER_AMOUNT=100000
STRATEGY_BUY_PRICE=90000000
STRATEGY_SELL_PRICE=110000000
STRATEGY_ORDER_QUANTITY=0.001
STRATEGY_CANDIDATE_CANDLE_UNIT_MINUTES=1
STRATEGY_CANDIDATE_CANDLE_COUNT=20
STRATEGY_CANDIDATE_MIN_PRICE_CHANGE_RATE=1.5
STRATEGY_CANDIDATE_MIN_TRADE_AMOUNT_CHANGE_RATE=0
SAFETY_KILL_SWITCH_ENABLED=false
```

Telegram:

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

Never commit real tokens, chat IDs, passwords, access keys, or secret keys.

## Documentation

- [Architecture](docs/harness/ARCHITECTURE.md)
- [Development Rules](docs/harness/DEVELOPMENT_RULES.md)
- [Security Rules](docs/harness/SECURITY_RULES.md)
- [Operations](docs/operations/OPERATIONS.md)
- [Reliability](docs/operations/RELIABILITY.md)
- [Telegram UX](docs/operations/TELEGRAM_UX.md)
- [Order Lifecycle](docs/trading/ORDER_LIFECYCLE.md)
- [Risk Policy](docs/trading/RISK_POLICY.md)
- [Strategy Policy](docs/trading/STRATEGY_POLICY.md)
- [Project Plan](docs/project/PROJECT_PLAN.md)
- [Project History](docs/project/PROJECT_HISTORY.md)
- [Next Steps](docs/project/PROJECT_NEXT_STEPS.md)

## Testing

Run:

```bat
gradlew.bat test
```

The test suite includes security lint checks.
If tests or security lint fail, do not commit.
