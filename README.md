# comebot

`comebot`은 코인 시세를 수집하고, 변동성과 추세를 기준으로 롱 전용 매수 후보를 판단한 뒤 `PAPER_TRADING`으로 검증하는 프로젝트다.

현재 목표는 실제 매매가 아니다. 실제 Upbit 공개 시세를 사용한 모의 매매 환경을 만들고, 전략과 리스크 정책이 안전하게 동작하는지 검증하는 것이다.

## 현재 상태

- Java 21
- Spring Boot
- 기본 거래 모드: `PAPER_TRADING`
- 기본 시세 Provider: `IN_MEMORY`
- 선택 가능 시세 Provider: `UPBIT`
- 실제 주문 API: 미구현
- `REAL_TRADING`: 미구현
- 기본 history 저장소: `IN_MEMORY`
- 선택 가능 history 저장소: `JPA`
- 기본 Telegram inbound: 비활성
- 기본 scheduler: 비활성
- 기본 notification: 비활성

## 현재 지원 기능

- InMemory 테스트 시세 공급
- Upbit 공개 Ticker API 기반 현재가 조회
- Upbit 공개 Candle API 기반 최근 분봉 조회
- 수동 트레이딩 플로우 실행 REST API
- PAPER_TRADING 주문 실행
- 리스크 검증
- 익절/손절 PAPER SELL 신호 정책
- 일일 주문 횟수와 일일 손실 제한
- kill switch
- 실행 이력 저장과 조회
- PAPER 포트폴리오 현금, 포지션, 실현손익 관리
- 현재가 기준 포트폴리오 평가
- Telegram 발송 구조
- Telegram getUpdates polling 기반 명령 처리
- Telegram inline button 처리
- 주요 설정 상태 조회 API

## 아직 지원하지 않는 기능

- 실제 거래소 주문 API
- `REAL_TRADING`
- Upbit 인증키 기반 계정 잔고 조회
- Upbit 인증키 기반 주문/취소/체결 조회
- 숏, 마진, 레버리지
- 실시간 WebSocket 시세
- 캔들 기반 변동성/추세 전략
- 운영용 영구 포트폴리오 저장소
- 수익 보장 기능

## 목표 전략 방향

현재 단순 threshold 전략은 테스트용이다. 앞으로의 전략 방향은 다음과 같다.

1. Upbit 공개 시세와 캔들 데이터를 수집한다.
2. 변동성이 커지는 마켓을 찾는다.
3. 상승 흐름이 확인된 마켓만 매수 후보로 만든다.
4. 롱 포지션만 진입한다.
5. 목표 수익률에 도달하면 익절 SELL 신호를 만든다.
6. 손실률이 기준 이하로 내려가면 손절 SELL 신호를 만든다.
7. 모든 주문은 리스크 검증을 통과해야 한다.
8. 실제 주문 API를 붙이기 전까지는 PAPER_TRADING으로만 검증한다.

세부 원칙은 `docs/STRATEGY_POLICY.md`를 따른다.

완료된 작업 기록과 앞으로 진행할 단계는 `docs/PROJECT_PLAN.md`에서 관리한다.

## 전체 흐름

```text
MarketPriceProvider
-> Strategy
-> OrderRequestFactory
-> RiskValidationService
-> PaperTradingExecutionGateway
-> PaperPortfolioService
-> TradingFlowHistoryService
-> Optional Notification
```

실행 전에는 `KillSwitchService`가 가장 먼저 차단 여부를 확인한다.

## 로컬 실행

PostgreSQL 실행:

```bat
docker compose up -d postgres
```

기본 실행:

```bat
gradlew.bat bootRun
```

Upbit 실제 시세 기반 PAPER_TRADING 실행:

```bat
scripts\run-upbit-paper.bat
```

이 스크립트는 Upbit 공개 Ticker API를 사용하지만 실제 주문 API는 호출하지 않는다.
캔들 Provider도 Upbit 공개 Candle API만 사용하며 인증키를 사용하지 않는다.

## 주요 설정

`.env` 또는 환경 변수로 관리한다.

```properties
MARKET_PRICE_PROVIDER=UPBIT
HISTORY_STORAGE_TYPE=IN_MEMORY
PAPER_INITIAL_CASH=1000000
PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY

TRADING_ALLOWED_MARKETS=KRW-BTC,KRW-ETH
TRADING_MAX_ORDER_AMOUNT=100000

STRATEGY_BUY_PRICE=90000000
STRATEGY_SELL_PRICE=110000000
STRATEGY_ORDER_QUANTITY=0.001

RISK_TAKE_PROFIT_RATE=5
RISK_STOP_LOSS_RATE=-3
RISK_POSITION_EXIT_ENABLED=false

RISK_DAILY_RISK_ENABLED=false
RISK_DAILY_ORDER_LIMIT=10
RISK_DAILY_LOSS_LIMIT=50000

SAFETY_KILL_SWITCH_ENABLED=false
TRADING_SCHEDULER_ENABLED=false
```

현재 전략 설정은 테스트용 단순 threshold다. 여러 코인을 제대로 검증하려면 마켓별 전략 설정과 캔들 기반 전략이 필요하다.

## 주요 API

상태 조회:

```http
GET /api/system/status
GET /api/database/status
GET /api/market-provider/status
GET /api/strategy/status
GET /api/risk/status
GET /api/scheduler/status
GET /api/notifications/status
GET /api/telegram/status
```

트레이딩 플로우:

```http
GET /api/trading-flow/run?market=KRW-BTC
GET /api/trading-flow/history
GET /api/trading-flow/history?market=KRW-BTC
GET /api/trading-flow/history/{id}
```

테스트 시세:

```http
GET /api/market-prices/KRW-BTC
PUT /api/market-prices/KRW-BTC
```

포트폴리오:

```http
GET /api/portfolio/status
GET /api/portfolio/positions
GET /api/portfolio/valuation
```

Telegram:

```http
POST /api/telegram/test-message
```

## Telegram 명령

기본값은 비활성이다. 사용하려면 `.env`에 설정한다.

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

지원 명령:

- `/help`
- `/menu`
- `/status`
- `/run KRW-BTC`
- `/history KRW-BTC`
- `/portfolio`
- `/positions`
- `/risk`
- `/safety`

Telegram 명령은 configured `TELEGRAM_CHAT_ID`와 일치하는 채팅에서 온 요청만 처리한다.

## 테스트

```bat
gradlew.bat test
```

테스트는 Java 21 기준으로 실행한다.

## DB 스키마 적용

JPA history 또는 JPA Telegram offset 저장소를 사용할 때만 필요하다.

```bat
scripts\apply-schema.bat
```

기본 history 저장소는 `IN_MEMORY`이므로 애플리케이션 재시작 시 이력이 사라진다.

## 운영 주의사항

- 현재 프로젝트는 실제 매매 시스템이 아니다.
- 실제 주문 API는 구현하지 않는다.
- `REAL_TRADING`은 구현하지 않는다.
- Upbit Access Key, Secret Key는 사용하지 않는다.
- Bot Token, Chat ID, DB 비밀번호는 로그와 응답에 노출하지 않는다.
- 실제 시세를 사용해도 주문은 PAPER_TRADING으로만 처리한다.
- 전략 변경은 테스트와 문서 변경을 동반해야 한다.
- kill switch는 전략, 리스크, 주문보다 우선한다.
