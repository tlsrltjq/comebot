# comebot

## PostgreSQL Docker

로컬 PostgreSQL은 Docker Compose로 실행할 수 있다.

```bat
docker compose up -d postgres
```

기본 데이터베이스 설정은 로컬 Docker 기준이다.

```properties
POSTGRES_DB=comebot
POSTGRES_USER=comebot
POSTGRES_PASSWORD=
POSTGRES_PORT=5432
```

Spring Boot datasource 설정은 환경 변수 기반이다.

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:comebot}}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:${POSTGRES_USER:comebot}}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:${POSTGRES_PASSWORD:}}
spring.jpa.hibernate.ddl-auto=none
```

기본 이력 저장소는 InMemory다. `history.storage-type=JPA`로 변경하면 PostgreSQL에 트레이딩 플로우 이력을 저장할 수 있다.

DB 연결 상태는 아래 API로 확인한다.

```http
GET /api/database/status
```

PostgreSQL 실행 후 응답 예시:

```json
{
  "connected": true,
  "database": "PostgreSQL"
}
```

연결 실패 시에도 서버 에러로 터뜨리지 않고 `connected=false`를 반환한다. 응답에는 DB 비밀번호, datasource URL, username 원문을 포함하지 않는다.

`comebot`은 코인 시세를 기준으로 테스트용 전략을 평가하고, 주문 요청을 리스크 검증한 뒤, `PAPER_TRADING` 결과와 실행 이력을 남기는 MVP 프로젝트다.

현재 프로젝트는 실제 매매 시스템이 아니다. 실제 거래소 주문 API와 `REAL_TRADING`은 구현하지 않는다.

## 프로젝트 개요

- 프로젝트 이름: `comebot`
- 기본 패키지: `com.giseop.comebot`
- 기본 거래 모드: `PAPER_TRADING`
- Java 버전: 21
- 저장소: 기본값은 InMemory, 설정으로 PostgreSQL/JPA 선택 가능
- 시세 Provider: 기본값은 InMemory, 설정으로 Upbit 공개 시세 조회 선택 가능

## 현재 지원 기능

- 테스트용 InMemory 시세 조회 및 가격 변경
- Upbit 공개 Ticker API 기반 현재가 조회 옵션
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
- 실제 포지션/잔고 동기화
- 수익 보장 또는 성과 예측

## 전체 흐름

1. `MarketPriceProvider`가 테스트용 시세를 제공한다.
2. `TradingStrategy`가 BUY, SELL, HOLD 신호를 생성한다.
3. `OrderRequestFactory`가 BUY/SELL 신호를 주문 요청으로 변환한다.
4. `RiskValidationService`가 주문 요청을 검증한다.
5. `OrderExecutionService`가 페이퍼 주문을 실행한다.
6. `TradingFlowHistoryService`가 실행 결과를 설정된 history 저장소에 저장한다.
7. 설정이 켜져 있고 알림 정책을 통과하면 알림을 보낸다.

## 로컬 실행 방법

```bat
gradlew.bat bootRun
```

기본 설정에서는 스케줄러와 알림, Telegram 발송이 모두 비활성이다.

## 시세 Provider 설정

기본값은 InMemory다. 테스트 가격 변경 API와 수동 검증 흐름은 기본적으로 이 provider를 사용한다.

```properties
market.price-provider=IN_MEMORY
```

Upbit 공개 Ticker API로 현재가를 조회하려면 아래처럼 설정한다.

```properties
market.price-provider=UPBIT
```

환경 변수로 실행할 수도 있다.

```bat
set MARKET_PRICE_PROVIDER=UPBIT
gradlew.bat bootRun
```

Upbit provider는 공개 시세 API만 호출한다. Access Key, Secret Key, 주문 API는 사용하지 않는다. 시세를 Upbit에서 조회하더라도 주문 실행은 기존 `PAPER_TRADING` 흐름만 사용한다.

현재 적용된 시세 provider는 아래 API로 확인한다.

```http
GET /api/market-provider/status
```

응답에는 provider, 외부 provider 여부, 설명 메시지만 포함한다. Access Key, Secret Key는 사용하지 않으며 응답에도 포함하지 않는다.

## 테스트 실행 방법

```bat
gradlew.bat test
```

Java 21 환경에서 실행한다.

## History 저장소 설정

기본값은 InMemory다. 애플리케이션 재시작 시 이력이 사라진다.

```properties
history.storage-type=IN_MEMORY
```

PostgreSQL에 이력을 저장하려면 PostgreSQL을 실행하고 테이블을 생성한 뒤 JPA 저장소로 변경한다. `spring.jpa.hibernate.ddl-auto=none`을 유지하므로 애플리케이션이 테이블을 자동 생성하지 않는다.

1. PostgreSQL 실행

```bat
docker compose up -d postgres
```

2. schema.sql 적용

```bat
scripts\apply-schema.bat
```

스크립트는 실행 중인 `comebot-postgres` 컨테이너에 [schema.sql](src/main/resources/schema.sql)을 적용한다. `POSTGRES_DB`, `POSTGRES_USER`는 `.env` 값이 있으면 사용하고, 없으면 `comebot`을 기본값으로 사용한다. DB 비밀번호는 출력하지 않는다.

3. JPA 저장소 설정

```properties
history.storage-type=JPA
```

환경 변수로 실행할 수도 있다.

```bat
set HISTORY_STORAGE_TYPE=JPA
gradlew.bat bootRun
```

4. 앱 실행 후 이력 저장 확인

```http
GET /api/trading-flow/run?market=KRW-BTC
GET /api/trading-flow/history
```

5. 앱 재시작 후 이력 유지 확인

앱을 종료했다가 다시 실행한 뒤 아래 API를 다시 호출한다.

```http
GET /api/trading-flow/history
```

JPA 저장소를 사용 중이면 PostgreSQL에 저장된 기존 이력이 조회된다. `history.storage-type=IN_MEMORY` 상태에서는 재시작 후 이력이 사라지는 것이 정상이다.

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
GET /api/market-provider/status
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

텔레그램 명령어 수신은 로컬 개발 기준 polling 방식이며 기본 비활성이다. Webhook은 사용하지 않는다.

```properties
telegram.inbound.enabled=false
telegram.inbound.fixed-delay-ms=3000
```

명령어 수신을 켜려면 Telegram 발송 설정도 함께 필요하다.

```bat
set TELEGRAM_BOT_TOKEN=...
set TELEGRAM_CHAT_ID=...
set TELEGRAM_INBOUND_ENABLED=true
gradlew.bat bootRun
```

지원 명령어:

- `/help`: 사용 가능한 명령어 목록
- `/menu`: 인라인 버튼 메뉴 표시
- `/status`: telegram, notification, scheduler 상태 요약
- `/run KRW-BTC`: 해당 market의 PAPER_TRADING 플로우 수동 실행
- `/history KRW-BTC`: 해당 market의 최근 이력 요약

명령어와 버튼 callback은 설정된 `telegram.chat-id`와 일치하는 채팅에서 온 요청만 처리한다. 다른 chatId의 요청은 실행하지 않으며 거래 결과, history, status 정보를 응답하지 않는다.

지원 버튼:

- 상태 보기
- KRW-BTC 실행
- KRW-ETH 실행
- BTC 이력 보기
- ETH 이력 보기
- 도움말

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
- Upbit provider는 공개 시세 조회만 수행하며 주문 API를 호출하지 않는다.
- `REAL_TRADING`은 구현하지 않는다.
- 기본 InMemory history는 애플리케이션 재시작 시 사라진다.
- JPA history를 사용하려면 PostgreSQL 테이블을 먼저 생성해야 한다.
- Bot Token, Chat ID는 코드에 하드코딩하지 않는다.
- Telegram inbound polling은 기본 비활성이다.
- Telegram 버튼 callback도 기존 `PAPER_TRADING` 플로우만 사용한다.
- 이 프로젝트는 실제 수익을 보장하지 않는다.
- 테스트용 가격 변경 API는 실제 시장 가격을 바꾸는 기능이 아니다.
