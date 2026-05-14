# Operations

## 로컬 실행

백엔드와 웹 UI를 함께 실행:

macOS/Linux:

```bash
scripts/run-local-dev.sh
```

Windows:

```bat
scripts\run-local-dev.bat
```

기본 접속 주소:

- 웹 UI: `http://127.0.0.1:5176`
- 백엔드 API: `http://127.0.0.1:8081`

PostgreSQL:

```bat
docker compose up -d postgres
```

기본 실행:

```bat
gradlew.bat bootRun
```

Upbit PAPER 실행:

```bash
scripts/run-upbit-paper.sh
```

Windows:

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
운영 중 장애와 이상 사례는 `docs/operations/INCIDENT_LOG.md`에 기록한다.

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

프론트엔드는 React Query로 활성 탭에서만 주기 재조회한다.
백그라운드 탭 polling은 API 부하를 줄이기 위해 끄고, 네트워크 재연결과 브라우저 포커스 복귀 시에는 자동 재조회한다.
각 주요 화면은 `실시간 운영(Live)` 배지에 최근 갱신 시각, 갱신 중 상태, 현재 화면의 polling 주기를 표시한다.

웹 polling 기준:

- Top status bar: 15초
- Dashboard: 10초, Risk 보조 조회 30초
- Candidates: 10초
- Portfolio: 5초, System/Risk 보조 조회 30초
- History: 10초, analytics 15초
- Market chart: 30초
- Risk/System 읽기 전용 화면: 30초
- Auto Run 화면: 5초

PAPER 현금 경고 기준:

- `/api/system/status`는 선택 exchange의 PAPER 현금, 초기 현금, 1회 주문 금액, 남은 주문 가능 횟수, 현금 경고 여부를 반환한다.
- 현금이 1회 주문 금액보다 작거나 초기 현금의 10% 이하이면 read-only 경고로 표시한다.
- 이 경고는 Dashboard와 Top Status Bar에 표시되며 BUY 실행 로직을 바꾸지 않는다.

공개 API 호출량 점검 기준:

- WebSocket/SNAPSHOT 운영에서는 fresh snapshot이 있으면 주문용 현재가 REST 호출이 발생하지 않는다.
- stale snapshot 또는 missing snapshot일 때만 거래소별 REST fallback을 호출한다.
- candidate scheduler에서 `ALL_KRW` 또는 `ALL_USDT`를 사용하면 universe refresh 결과를 기준으로 market별 candle/price 조회가 발생할 수 있다.
- exit scheduler는 보유 PAPER position market만 평가하므로 포지션 수가 REST fallback 호출량의 상한이다.
- candidate 주기를 줄이거나 `ALL_KRW,ALL_USDT`를 동시에 켤 때는 `/api/market-provider/status`, scheduler summary, 429 로그를 함께 확인한다.
- 429 또는 fallback 실패가 발생하면 주문 성공으로 처리하지 말고 history의 `FAILED`/`REJECTED`와 Incident Log를 확인한다.

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

History 조회는 대용량 렌더링과 DB 부하를 막기 위해 `limit`을 최대 200개로 제한한다.
웹 History 화면은 20/50/100/200 단위 조회만 제공하고, market 필터는 입력 후 `조회(Search)`를 눌렀을 때 적용한다.

## 후보 모니터링

```http
GET /api/candidates?exchange=upbit&limit=20
GET /api/candidates?exchange=upbit&market=KRW-BTC
```

전체 후보 조회는 서버에서 최대 50개로 제한한다.
웹 Candidates 화면은 20/50 단위 조회만 제공하고, market 필터는 입력 후 `조회(Search)`를 눌렀을 때 적용한다.
후보 화면에는 수동 BUY나 후보 실행 버튼을 제공하지 않는다.

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
- `paper_trade_log`: 거래소별 FILLED PAPER BUY/SELL append-only 체결 원장
- `scheduler_control_setting`: 자동매매 켜기/끄기와 후보 조회 주기

## OS별 운영 안내

웹 Dashboard와 System 화면의 `운영 환경(OS Guide)` 패널은 브라우저의 OS 정보를 안내 표시 용도로만 사용한다.
거래소 선택, 전략 판단, 리스크 검증, 주문 실행에는 OS 감지 결과를 사용하지 않는다.
System 화면은 OS별 실행 스크립트, schema script, workspace path, shell, scheduler, provider, notification/Telegram 상태를 읽기 전용으로 보여준다.

- macOS: `scripts/run-local-dev.sh`, `scripts/run-upbit-paper.sh`, `scripts/apply-schema.sh`, `/Users/<user>/workspace/comebot`
- Windows: `scripts\run-local-dev.bat`, `scripts\run-upbit-paper.bat`, `scripts\apply-schema.bat`, `%USERPROFILE%\workspace\comebot`
- Linux: `scripts/run-local-dev.sh`, `scripts/run-upbit-paper.sh`, `scripts/apply-schema.sh`, `$HOME/workspace/comebot`
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

전략 성과 판단은 `/api/analytics/summary`의 승률(`winRate`), 평균 보유 시간(`averageHoldingSeconds`), 손익비(`profitLossRatio`)를 함께 본다.
`winRate`는 선택 범위의 FILLED BUY 대비 익절 SELL 비율이며, `averageHoldingSeconds`는 같은 market의 FILLED BUY->FILLED SELL FIFO 매칭 기준이다.
`profitLossRatio`는 평균 익절률을 평균 손절률의 절댓값으로 나눈 값이다.

DB 누적 확인:

```sql
SELECT exchange, COUNT(*) FROM trading_flow_history GROUP BY exchange;
SELECT exchange, COUNT(*) FROM paper_position WHERE quantity > 0 GROUP BY exchange;
SELECT exchange, side, COUNT(*) FROM paper_trade_log GROUP BY exchange, side;
SELECT exchange, cash, realized_profit FROM paper_portfolio_state ORDER BY exchange;
```

재시작 후 같은 API와 SQL 결과가 유지되면 JPA 누적 조건을 만족한다.
