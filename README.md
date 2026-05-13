# comebot

`comebot` is a coin trading bot project focused on safe PAPER_TRADING validation.
It uses public market data, evaluates long-only trading signals, applies risk checks, records history, and manages a paper portfolio.

This project does not implement real exchange orders or `REAL_TRADING`.

## Project Status

- Language: Java 21
- Base package: `com.giseop.comebot`
- Default trading mode: `PAPER_TRADING`
- Market price providers: `IN_MEMORY`, `UPBIT`, `BINANCE`
- Candle providers: Upbit public Candle API, Binance public spot Kline API
- History storage: `IN_MEMORY` by default, optional `JPA`
- Portfolio storage: `IN_MEMORY`, separated by exchange mode
- Scheduler: candidate and exit schedulers are enabled by default for PAPER_TRADING automation
- Telegram inbound/outbound: disabled by default

## What This Project Does

- Fetches test prices, Upbit public prices, or Binance public spot prices
- Fetches recent Upbit minute candles or Binance spot klines
- Scans long-only paper trading candidates automatically
- Creates BUY, SELL, HOLD signals
- Runs risk validation before paper orders
- Executes paper orders only
- Updates paper cash, positions, realized profit
- Calculates portfolio valuation from current prices
- Stores trading flow history
- Sends optional notifications
- Supports a React monitoring web UI
- Supports Telegram commands and inline buttons

## What This Project Does Not Do

- No real order API
- No `REAL_TRADING`
- No Upbit Access Key or Secret Key
- No account balance sync from an exchange
- No margin, leverage, or short selling
- No profit guarantee

## Strategy Direction

The default `SimpleThresholdStrategy` is only a test strategy.
`VolatilityBreakoutLongStrategy` can be selected for long-only PAPER_TRADING entry validation.

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

Run with Upbit public ticker data, PAPER_TRADING, and JPA history/portfolio storage:

```bash
scripts/run-upbit-paper-jpa.sh
```

Run the web UI:

```bat
cd frontend
npm install
npm run dev
```

The web UI is available at `http://localhost:5173/`.

## Main APIs

Status:

```http
GET /api/system/status
GET /api/database/status
GET /api/market-provider/status
GET /api/strategy/status
GET /api/risk/status?exchange=upbit
```

Trading flow:

```http
GET /api/candidates
GET /api/candidates?market=KRW-BTC
POST /api/candidates/execute?market=KRW-BTC
GET /api/trading-flow/run?market=KRW-BTC
GET /api/trading-flow/history
GET /api/trading-flow/history?market=KRW-BTC
GET /api/trading-flow/history?exchange=binance
```

Portfolio:

```http
GET /api/portfolio/status
GET /api/portfolio/positions
GET /api/portfolio/valuation
GET /api/portfolio/status?exchange=binance
```

Analytics:

```http
GET /api/analytics/summary?range=24h
GET /api/analytics/pnl?range=24h
GET /api/analytics/losses?range=24h
```

Telegram:

```http
GET /api/telegram/status
POST /api/telegram/test-message
```

Telegram commands:

```text
/help
/menu
/status
/auto
/conditions
/pnl
/candidates
/history KRW-BTC
/portfolio
/positions
/risk
/safety
```

Telegram messages and inline button labels are Korean.
Telegram is monitoring-first by default. `/run` and `/candidate-run` are parsed for compatibility, but they execute only when `TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=true`.

## Configuration

Use `.env` or environment variables.

```properties
MARKET_PRICE_PROVIDER=UPBIT
HISTORY_STORAGE_TYPE=IN_MEMORY
PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY
PAPER_INITIAL_CASH=1000000
TRADING_ALLOWED_MARKETS=ALL_KRW
TRADING_MAX_ORDER_AMOUNT=100000
STRATEGY_BUY_PRICE=90000000
STRATEGY_SELL_PRICE=110000000
STRATEGY_ORDER_QUANTITY=0.01
STRATEGY_ORDER_AMOUNT=10000
STRATEGY_SELECTED=VOLATILITY_BREAKOUT_LONG
STRATEGY_CANDIDATE_CANDLE_UNIT_MINUTES=1
STRATEGY_CANDIDATE_CANDLE_COUNT=5
STRATEGY_CANDIDATE_MIN_PRICE_CHANGE_RATE=0.3
STRATEGY_CANDIDATE_MIN_TRADE_AMOUNT_CHANGE_RATE=20
STRATEGY_CANDIDATE_MAX_PRICE_CHANGE_RATE=10
STRATEGY_CANDIDATE_MAX_HIGH_LOW_RANGE_RATE=20
STRATEGY_ENTRY_PREVENT_REENTRY_WITH_POSITION=false
SAFETY_KILL_SWITCH_ENABLED=false
RISK_POSITION_EXIT_ENABLED=true
RISK_CONCENTRATION_ENABLED=false
RISK_CONCENTRATION_UPBIT_BLOCK_EXPOSURE_RATE=10
RISK_CONCENTRATION_BINANCE_BLOCK_EXPOSURE_RATE=40
RISK_TAKE_PROFIT_RATE=1.5
RISK_STOP_LOSS_RATE=-0.7
TRADING_SCHEDULER_ENABLED=true
TRADING_CANDIDATE_SCHEDULER_ENABLED=true
TRADING_SCHEDULER_FIXED_DELAY_MS=30000
TRADING_SCHEDULER_MARKETS=ALL_KRW
TRADING_CANDIDATE_SCHEDULER_FIXED_DELAY_MS=30000
TRADING_CANDIDATE_SCHEDULER_MARKETS=ALL_KRW
```

For long-running PAPER data accumulation, use:

```properties
HISTORY_STORAGE_TYPE=JPA
PAPER_PORTFOLIO_STORAGE_TYPE=JPA
TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE=JPA
```

Telegram:

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

Never commit real tokens, chat IDs, passwords, access keys, or secret keys.

`POST /api/candidates/execute` only executes PAPER_TRADING BUY orders for selected candidates.
It does not call any real exchange order API.

`STRATEGY_SELECTED=VOLATILITY_BREAKOUT_LONG` lets `/api/trading-flow/run` use the volatility candidate scanner for BUY signals.
The default is `VOLATILITY_BREAKOUT_LONG`.

`TRADING_CANDIDATE_SCHEDULER_ENABLED=true` lets the legacy candidate scheduler run candidate PAPER execution automatically.
The default is enabled for local monitoring, while the trading flow scheduler remains the main automatic PAPER order path.
Candidate scheduler runs are summarized as filled, rejected, hold, and failed counts in logs when enabled.
Set `TRADING_CANDIDATE_SCHEDULER_NOTIFY_SUMMARY=true` and `NOTIFICATION_ENABLED=true` only when you want that summary sent through the configured notification sender.

`TRADING_SCHEDULER_ENABLED=true` lets the scheduler run the selected strategy automatically.
With `RISK_POSITION_EXIT_ENABLED=true`, take-profit and stop-loss SELL signals can be evaluated automatically for existing PAPER positions.

## Web UI

The React web UI is operations-focused and keeps trading execution constrained to PAPER_TRADING.

- Korean labels include English in parentheses.
- Manual BUY buttons are not exposed.
- The only manual trade execution UX is selected held PAPER position SELL.
- Auto trading can be turned on/off and the candidate interval can be set to 30s or 60s from the operations screen.
- Automatic execution status, candidates, portfolio, and history are visible.
- All trading remains PAPER_TRADING.
- The UI does not add real order APIs or `REAL_TRADING`.

Market-specific strategy overrides can be added in a properties file:

```properties
strategy.market-overrides.markets.KRW-BTC.order-quantity=0.002
strategy.market-overrides.markets.KRW-BTC.min-price-change-rate=2
strategy.market-overrides.markets.KRW-BTC.max-price-change-rate=12
```

## Documentation

- [Architecture](docs/harness/ARCHITECTURE.md)
- [Development Rules](docs/harness/DEVELOPMENT_RULES.md)
- [Harness Status](docs/harness/HARNESS_STATUS.md)
- [Security Rules](docs/harness/SECURITY_RULES.md)
- [Operations](docs/operations/OPERATIONS.md)
- [Reliability](docs/operations/RELIABILITY.md)
- [Telegram UX](docs/operations/TELEGRAM_UX.md)
- [Order Lifecycle](docs/trading/ORDER_LIFECYCLE.md)
- [Risk Policy](docs/trading/RISK_POLICY.md)
- [Strategy Policy](docs/trading/STRATEGY_POLICY.md)
- [Project Plan](docs/project/PROJECT_PLAN.md)
- [Web UX Upgrade Plan](docs/project/WEB_UX_UPGRADE_PLAN.md)
- [Project History](docs/project/PROJECT_HISTORY.md)
- [Next Steps](docs/project/PROJECT_NEXT_STEPS.md)

## Testing

Run:

```bat
gradlew.bat test
```

The test suite includes security lint checks.
If tests or security lint fail, do not commit.

Frontend:

```bat
cd frontend
npm run lint
npm run build
npm test
```
