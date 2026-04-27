# comebot

`comebot`은 코인 시세를 기준으로 테스트용 전략을 평가하고, 주문 요청을 리스크 검증한 뒤, `PAPER_TRADING` 결과와 실행 이력을 남기는 MVP 프로젝트다.

현재 프로젝트는 실제 매매 시스템이 아니다. 실제 거래소 주문 API와 `REAL_TRADING`은 구현하지 않는다.

## 프로젝트 개요

- 프로젝트 이름: `comebot`
- 기본 패키지: `com.giseop.comebot`
- 기본 거래 모드: `PAPER_TRADING`
- Java 버전: 21
- 저장소: 현재 실행 이력은 InMemory 저장소 사용

## 현재 지원 기능

- 테스트용 InMemory 시세 조회 및 가격 변경
- 테스트용 단순 threshold 전략
- BUY, SELL, HOLD 신호 생성
- 주문 요청 생성
- 리스크 검증
- 페이퍼 주문 실행
- 트레이딩 플로우 수동 실행 REST API
- 트레이딩 플로우 실행 이력 저장 및 조회
- 스케줄러 설정 및 주기 실행 구조
- 알림 필터 정책
- Telegram 설정 상태 조회
- Telegram 테스트 메시지 발송 API
- TelegramNotificationSender 기반 발송 구조

## 아직 지원하지 않는 기능

- 실제 거래소 시세 API 연동
- 실제 거래소 주문 API 연동
- `REAL_TRADING`
- PostgreSQL/JPA 기반 이력 저장
- 실제 포지션/잔고 동기화
- 수익 보장 또는 성과 예측

## 전체 흐름

1. `MarketPriceProvider`가 테스트용 시세를 제공한다.
2. `TradingStrategy`가 BUY, SELL, HOLD 신호를 생성한다.
3. `OrderRequestFactory`가 BUY/SELL 신호를 주문 요청으로 변환한다.
4. `RiskValidationService`가 주문 요청을 검증한다.
5. `OrderExecutionService`가 페이퍼 주문을 실행한다.
6. `TradingFlowHistoryService`가 실행 결과를 InMemory 이력으로 저장한다.
7. 설정이 켜져 있고 알림 정책을 통과하면 알림을 보낸다.

## 로컬 실행 방법

```bat
gradlew.bat bootRun
```

기본 설정에서는 스케줄러와 알림, Telegram 발송이 모두 비활성이다.

## 테스트 실행 방법

```bat
gradlew.bat test
```

Java 21 환경에서 실행한다.

## 주요 API

### 트레이딩 플로우

```http
GET /api/trading-flow/run?market=KRW-BTC
GET /api/trading-flow/history
GET /api/trading-flow/history?market=KRW-BTC&limit=20
GET /api/trading-flow/history/{id}
```

### 테스트용 시세

```http
GET /api/market-prices/KRW-BTC
PUT /api/market-prices/KRW-BTC
Content-Type: application/json

{
  "price": 50000000
}
```

### 스케줄러

```http
GET /api/scheduler/status
```

### 알림

```http
GET /api/notifications/status
```

### Telegram

```http
GET /api/telegram/status
POST /api/telegram/test-message
Content-Type: application/json

{
  "message": "hello"
}
```

## Telegram 설정 방법

민감 정보는 코드에 하드코딩하지 않는다. 환경 변수로만 주입한다.

```properties
telegram.enabled=true
telegram.bot-token=${TELEGRAM_BOT_TOKEN:}
telegram.chat-id=${TELEGRAM_CHAT_ID:}
```

`.env.example`을 참고한다.

## 스케줄러 설정 방법

기본값은 비활성이다.

```properties
trading.scheduler.enabled=false
trading.scheduler.fixed-delay-ms=60000
trading.scheduler.markets=KRW-BTC,KRW-ETH
```

`trading.scheduler.enabled=true`일 때만 설정된 market 목록을 주기적으로 실행한다.

## 알림 설정 방법

기본값은 알림 비활성이다.

```properties
notification.enabled=false
notification.send-hold=false
notification.send-filled=true
notification.send-rejected=true
```

기본 정책에서는 HOLD 결과는 알림 대상이 아니고, FILLED/REJECTED 결과만 알림 대상이다. 실제 Telegram 발송에는 `notification.enabled=true`, `telegram.enabled=true`, Telegram 설정 완료가 모두 필요하다.

## 주의사항

- 기본 거래 모드는 항상 `PAPER_TRADING`이다.
- 실제 거래소 주문 API는 아직 없다.
- `REAL_TRADING`은 구현하지 않는다.
- InMemory history는 애플리케이션 재시작 시 사라진다.
- Bot Token, Chat ID는 코드에 하드코딩하지 않는다.
- 이 프로젝트는 실제 수익을 보장하지 않는다.
- 테스트용 가격 변경 API는 실제 시장 가격을 바꾸는 기능이 아니다.
