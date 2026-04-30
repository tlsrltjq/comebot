# Operations

## 로컬 실행

PostgreSQL:

```bat
docker compose up -d postgres
```

기본 실행:

```bat
gradlew.bat bootRun
```

Upbit PAPER 실행:

```bat
scripts\run-upbit-paper.bat
```

## 상태 확인

```http
GET /api/system/status
GET /api/database/status
GET /api/market-provider/status
GET /api/risk/status
GET /api/scheduler/status
```

`/api/scheduler/status`는 기존 trading flow scheduler와 candidate scheduler 상태를 함께 보여준다.

## 트레이딩 플로우

```http
GET /api/trading-flow/run?market=KRW-BTC
GET /api/trading-flow/history?market=KRW-BTC
```

## 포트폴리오

```http
GET /api/portfolio/status
GET /api/portfolio/positions
GET /api/portfolio/valuation
```

## Telegram

필수 설정:

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

연결 확인:

```powershell
Test-NetConnection api.telegram.org -Port 443
```

## JPA 스키마

JPA 저장소 사용 전:

```bat
scripts\apply-schema.bat
```
