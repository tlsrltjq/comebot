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

Upbit PAPER JPA 누적 실행:

```bash
scripts/run-upbit-paper-jpa.sh
```

이 스크립트는 PostgreSQL을 시작하고 `schema.sql`을 적용한 뒤 `HISTORY_STORAGE_TYPE=JPA`, `PAPER_PORTFOLIO_STORAGE_TYPE=JPA`로 앱을 실행한다.
실제 주문 API와 `REAL_TRADING`은 사용하지 않는다.

Binance PAPER 후보 스모크 테스트 절차:

- `docs/operations/BINANCE_PAPER_SMOKE_TEST.md`

## 상태 확인

```http
GET /api/system/status
GET /api/database/status
GET /api/market-provider/status
GET /api/risk/status?exchange=upbit
GET /api/scheduler/status
```

`/api/scheduler/status`는 기존 trading flow scheduler와 candidate scheduler 상태를 함께 보여준다.
candidate scheduler의 `candidateExchanges` 값으로 현재 후보 실행 거래소 목록을 확인한다.

자동 PAPER 매매 런타임 제어:

```http
PUT /api/scheduler/control
Content-Type: application/json

{
  "autoTradingEnabled": true,
  "candidateFixedDelayMs": 30000
}
```

- `autoTradingEnabled`는 candidate scheduler와 exit scheduler를 함께 켜거나 끈다.
- `candidateFixedDelayMs`는 `30000` 또는 `60000`만 허용한다.
- 후보 스케줄러는 30초마다 상태를 확인하고, 설정값이 60초이면 내부에서 다음 실행을 건너뛰어 런타임 변경을 반영한다.
- 변경된 자동매매 설정은 `scheduler_control_setting` 테이블에 저장되고, 백엔드 재시작 시 환경변수 기본값보다 우선 적용된다.

## 웹 운영 화면

프론트엔드는 React Query 기본값으로 백그라운드 탭, 네트워크 재연결, 브라우저 포커스 복귀 시 자동 재조회한다.
각 주요 화면은 `실시간 운영(Live)` 배지에 최근 갱신 시각과 갱신 중 상태를 표시한다.

운영형 PAPER 확인 시에는 실제 공개 시세와 스케줄러를 켜고, 주문/자산만 PAPER로 유지한다.
실제 주문 API와 REAL_TRADING은 계속 금지한다.
장시간 손익 확인 시에는 히스토리와 PAPER 포트폴리오를 모두 JPA로 저장한다.

권장 로컬 확인값:

```properties
MARKET_PRICE_PROVIDER=SNAPSHOT
HISTORY_STORAGE_TYPE=JPA
PAPER_PORTFOLIO_STORAGE_TYPE=JPA
MARKET_WEBSOCKET_ENABLED=true
MARKET_WEBSOCKET_UPBIT_ENABLED=true
MARKET_UPBIT_KRW_TICKER_POLLING_ENABLED=true
TRADING_SCHEDULER_ENABLED=true
TRADING_SCHEDULER_MARKETS=KRW-BTC,KRW-ETH
TRADING_CANDIDATE_SCHEDULER_ENABLED=true
TRADING_CANDIDATE_SCHEDULER_MARKETS=ALL_KRW,ALL_USDT
TRADING_CANDIDATE_SCHEDULER_EXCHANGES=UPBIT,BINANCE
TRADING_EXIT_SCHEDULER_ENABLED=true
TRADING_EXIT_SCHEDULER_EXCHANGES=UPBIT,BINANCE
```

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

macOS/Linux:

```bash
scripts/apply-schema.sh
```

`schema.sql`은 다음 PAPER 저장 테이블도 생성한다.

- `trading_flow_history`: 거래소별 자동 실행 이력
- `paper_portfolio_state`: 거래소별 PAPER 현금과 누적 실현손익
- `paper_position`: 거래소별 PAPER 보유 포지션
- `paper_realized_profit_event`: 거래소별 실현손익 이벤트
- `scheduler_control_setting`: 자동매매 켜기/끄기와 후보 조회 주기

## OS별 운영 안내

웹 Dashboard의 `운영 환경(OS Guide)` 패널은 브라우저의 OS 정보를 안내 표시 용도로만 사용한다.
거래소 선택, 전략 판단, 리스크 검증, 주문 실행에는 OS 감지 결과를 사용하지 않는다.

- macOS: `scripts/run-upbit-paper.sh`, `scripts/apply-schema.sh`, `/Users/<user>/workspace/comebot`
- Windows: `scripts\run-upbit-paper.bat`, `scripts\apply-schema.bat`, `%USERPROFILE%\workspace\comebot`
- 민감 정보는 OS와 무관하게 `.env` 또는 환경 변수로만 관리한다.

## JPA PAPER 누적 확인

장기 PAPER 운용 전 현재 `.env`에서 storage type을 확인한다. 민감 정보는 출력하지 않는다.

```bash
awk -F= '/^(HISTORY_STORAGE_TYPE|PAPER_PORTFOLIO_STORAGE_TYPE|TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE)=/ {print $1"="$2}' .env
```

권장 누적값:

```properties
HISTORY_STORAGE_TYPE=JPA
PAPER_PORTFOLIO_STORAGE_TYPE=JPA
TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE=JPA
```

운용 중 확인 API:

```http
GET /api/trading-flow/history?exchange=upbit&limit=20
GET /api/portfolio/valuation?exchange=upbit
GET /api/analytics/pnl?exchange=upbit&range=24h
GET /api/analytics/losses?exchange=upbit&range=24h
```

DB 누적 확인:

```sql
SELECT exchange, COUNT(*) FROM trading_flow_history GROUP BY exchange;
SELECT exchange, COUNT(*) FROM paper_position WHERE quantity > 0 GROUP BY exchange;
SELECT exchange, cash, realized_profit FROM paper_portfolio_state ORDER BY exchange;
```

재시작 후 같은 API와 SQL 결과가 유지되면 JPA 누적 조건을 만족한다.
