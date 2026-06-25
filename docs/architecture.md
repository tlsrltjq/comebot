# docs/architecture.md — 모듈 구조·데이터 흐름·운영 인프라

## 모듈 구조

```
src/main/java/com/giseop/comebot/
├── ComebotApplication.java          진입점 (Spring Boot)
│
├── market/                          시세 수집
│   ├── candle/provider/             UpbitCandleProvider, BinanceCandleProvider (CandleProvider 구현)
│   ├── provider/                    SnapshotMarketPriceProvider (WebSocket 우선, REST fallback)
│   │                                UpbitMarketPriceProvider, BinanceMarketPriceProvider
│   ├── service/                     BtcTrendCacheService (EMA5/10, 5분 갱신)
│   │                                MarketSelectionService (거래대금 Top-N, excluded-markets)
│   ├── websocket/                   UpbitTickerWebSocketClient, BinanceTickerWebSocketClient
│   └── scheduler/                   UpbitKrwTickerPollingScheduler, BinanceUsdtTickerPollingScheduler
│
├── exchange/                        ExchangeMode (UPBIT / BINANCE)
│
├── strategy/
│   ├── candidate/                   CandidateScannerService (진입 필터 파이프라인)
│   │                                CandidateExecutionService (SELECTED → PAPER BUY 실행)
│   │                                CandidateScannerProperties, CandidateScannerProperties.ExchangeSettings
│   ├── indicator/                   VolatilityIndicatorService, VolatilitySnapshot
│   └── service/                     StrategyMarketSettingsService, StrategyEntryProperties
│
├── scanlog/                         CandidateScanLogService (모든 스캔 결과 append-only 기록)
│
├── safety/                          KillSwitchService (kill switch 차단)
│
├── risk/
│   ├── service/                     PositionExitSignalService (익절/손절/trailing stop 신호)
│   │                                PositionLimitRiskValidationService
│   │                                StopLossCooldownValidationService
│   │                                ConcentrationRiskValidationService
│   │                                DailyRiskValidationService
│   └── domain/                      RiskCheckResult, RiskDecision, PositionExitPolicy
│
├── execution/                       PaperTradingExecutionGateway (PAPER 전용, 실제 주문 없음)
│                                    PendingLimitOrderService (maker 지정가 대기 주문, ADR-013)
│
├── portfolio/                       PaperPortfolioService (현금·포지션·손익)
│                                    PaperPortfolioValuationService (현재가 기반 평가)
│
├── history/                         TradingFlowHistoryService (JPA / InMemory 선택)
│
├── analytics/                       AnalyticsService (승률·손익비·보유시간 집계)
│
├── scheduler/
│   ├── ScheduledCandidateExecutionRunner   candidate scheduler (30초, enabled)
│   ├── ScheduledPositionExitRunner         exit scheduler (1초, enabled)
│   ├── ScheduledTradingFlowRunner          @Deprecated legacy scheduler (disabled)
│   └── SchedulerControlService             자동매매 ON/OFF, candidate 주기 변경
│
├── notification/                    TradingFlowNotificationService (선택적 알림)
│
├── telegram/                        TelegramPollingRunner, TelegramCommandService (조회 전용)
│
└── system/                          SystemStatusController, MarketProviderStatusController

frontend/src/
├── features/
│   ├── system/          DashboardPage, SystemPage
│   ├── candidates/      CandidatesPage
│   ├── portfolio/       PortfolioPage (선택 SELL 포함)
│   ├── history/         HistoryPage
│   ├── risk/            RiskPage
│   ├── market/          MarketOverviewPage
│   └── trading/         TradePage
└── shared/
    ├── api/             client.ts, types.ts, polling.ts
    └── exchange/        ExchangeModeContext (UPBIT / BINANCE 전환)
```

---

## 데이터 흐름

### 후보 선정 → BUY 실행

```
[WebSocket / REST]
  → SnapshotMarketPriceProvider (시세 캐시)
  → CandidateScannerService.scan(market, exchange)
      ├─ BtcTrendCacheService.trend() → DOWN이면 SKIPPED (UPBIT만)
      ├─ removeIncompleteLatestCandle (진행 중 캔들 제외)
      ├─ VolatilityIndicatorService.calculate(candles)
      │     → VolatilitySnapshot (priceChangeRate, tradeAmountChangeRate,
      │                          distanceFromHighRate, lastCandleBullish, ...)
      ├─ 진입 필터 순차 평가 (모두 통과해야 SELECTED)
      │     trend UP / lastCandleBullish / pumpDetected / priceChangeRate /
      │     tradeAmountChangeRate / highLowRange / distanceFromHigh /
      │     minLatestCandleTradeAmount / allowedMarkets / marketOverride
      └─ CandidateScanLogService.record(result)
  → CandidateExecutionService (SELECTED일 때만)
      ├─ KillSwitchService.check()
      ├─ RiskValidationService 파이프라인
      │     PositionLimitRiskValidationService
      │     StopLossCooldownValidationService
      │     ConcentrationRiskValidationService
      │     DailyRiskValidationService
      ├─ PaperPortfolioService.checkCash()
      └─ PaperTradingExecutionGateway.execute() → TradingFlowHistory 저장
```

### 포지션 청산 평가 → SELL 실행

```
[exit scheduler, 1초]
  → ScheduledPositionExitRunner.run(exchange)
      → PaperPortfolioService.getPositions(exchange)
      → PositionExitSignalService.evaluate(position, marketPrice)
            ├─ abnormal price drop check (≤ -20% → 차단)
            ├─ trailing stop (peakProfit 추적, activation 2%, trail 1.5%)
            ├─ take profit (≥ +2.0%)
            └─ stop loss (≤ -1.5%)
      → CandidateExecutionService.sell() → TradingFlowHistory 저장
```

---

## 주요 REST API

| 엔드포인트 | 설명 |
|---|---|
| `GET /api/system/status` | 시스템 전체 상태 |
| `GET /api/market-provider/status` | WebSocket snapshot 현황 |
| `GET /api/candidate-scan-log` | 스캔 로그 조회 (`?range=24h&exchange=UPBIT`) |
| `GET /api/portfolio?exchange=UPBIT` | PAPER 포트폴리오 |
| `POST /api/portfolio/sell` | 선택 PAPER 포지션 SELL (유일한 수동 실행 예외) |
| `GET /api/history` | 거래 이력 |
| `GET /api/analytics/summary` | 집계 요약 |
| `GET /api/risk/status` | 리스크 설정 상태 |
| `POST /api/scheduler/control` | 자동매매 ON/OFF, candidate 주기 변경 |

---

## 운영 인프라

### 로컬 실행

```bash
# 백엔드 + 프론트 동시 (권장)
bash scripts/run-local-dev.sh

# 백엔드만 (JPA + Upbit/Binance)
bash scripts/run-paper-jpa.sh

# PostgreSQL만
docker compose up -d postgres
```

### Docker 전체 스택

```bash
docker compose up --build
# 백엔드: 18080, PostgreSQL: 5433
```

### 포트 및 접속

| 서비스 | 포트 | 주소 |
|---|---|---|
| Backend API | 18080 | `http://127.0.0.1:18080` |
| Frontend Dev | 5176 | `http://127.0.0.1:5176` |
| PostgreSQL | 5433 | `localhost:5433/comebot` |

### 환경 설정 파일

| 파일 | 용도 |
|---|---|
| `.env` | 운영 환경 오버라이드 (시크릿 포함, `.gitignore`에 포함) |
| `.env.example` | 로컬 개발용 템플릿 (시크릿 제외) |
| `src/main/resources/application.properties` | Spring Boot 기본값 (보수적) |

### 빌드 및 검증

```bash
# 백엔드 테스트 + 린트
./gradlew test checkstyleMain

# 프론트엔드
cd frontend && npm run lint && npm run build && npm test

# E2E
cd frontend && npm run test:e2e
```
## Current Source Notes (2026-06-24)

### Trade Journal Source of Truth

- `/api/analytics/matched-trades` is backed by `paper_trade_log`, not by FIFO matching of `trading_flow_history`.
- SELL rows in `paper_trade_log` are treated as realized close events.
- Profit rate is calculated from the actual SELL row:
  - `costBasis = grossAmount - realizedProfit`
  - `profitRatePct = realizedProfit / costBasis * 100`
- Exit reason is classified from realized profit:
  - `realizedProfit > 0`: `TAKE_PROFIT`
  - `realizedProfit < 0`: `STOP_LOSS`
  - `realizedProfit == 0` or null: `MANUAL`
- The displayed buy timestamp/price is the latest prior BUY log for the same market and is informational. PnL and exit label come from the SELL log.

### PAPER Exit Path

- Selected PAPER position sell uses `OrderExecutionService.executePaperPositionExit(...)`.
- This path only accepts `SELL` orders and still validates held quantity.
- It bypasses new-entry allow-list and max-order risk checks so contaminated or legacy held PAPER positions can be closed even when the current observation profile allows only another market universe.
- It never calls real exchange order APIs; execution remains `PAPER_TRADING` only.

### Docker Observation Profile

- Compose no longer restores scheduler control state by default: `SCHEDULER_CONTROL_RESTORE_ENABLED` defaults to `false`.
- Use `scripts/restart-session-volatility-docker.bat` or `scripts/restart-session-volatility-docker.sh` to restart app/web with the Binance Session Volatility PAPER profile.
- That restart profile sets strategy/market scope to `SESSION_VOLATILITY_BREAKOUT`, `ALL_USDT`, and `BINANCE`, with candidate/exit schedulers initially disabled. Enable automation only after readiness checks pass.

## Stock Research Identity Boundary

- `market/domain/MarketAssetClass` defines `CRYPTO` and `STOCK`.
- `market/domain/MarketVenue` defines `UPBIT`, `BINANCE`, and `US_STOCK` with quote
  currency and timezone metadata.
- `market/domain/MarketIdentity` identifies a market as `(assetClass, venue, symbol)`.
- `ExchangeMode` remains the crypto execution mode for `UPBIT` and `BINANCE`; it is not
  extended with `US_STOCK`.
