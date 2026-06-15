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
- 백엔드 API: `http://127.0.0.1:18080`

`run-local-dev`는 백엔드를 `scripts/run-paper-jpa.*`로 실행한다. 기본 로컬 운영값은 Upbit/Binance 동시 PAPER, `SNAPSHOT` provider, JPA history/portfolio 저장이다.

PostgreSQL:

```bat
docker compose up -d postgres
```

기본 실행:

```bat
gradlew.bat bootRun
```

Upbit-only PAPER 단기 smoke 실행:

```bash
scripts/run-upbit-paper.sh
```

Windows:

```bat
scripts\run-upbit-paper.bat
```

이 legacy smoke 스크립트는 `HISTORY_STORAGE_TYPE=IN_MEMORY`, `PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY`를 강제한다. 장기 PAPER 누적 운영이나 Upbit/Binance 동시 실행에는 사용하지 않는다.

Upbit/Binance PAPER JPA 누적 실행:

```bash
scripts/run-paper-jpa.sh
```

Windows:

```bat
scripts\run-paper-jpa.bat
```

이 스크립트는 PostgreSQL을 시작하고 `schema.sql`을 적용한 뒤 `HISTORY_STORAGE_TYPE=JPA`, `PAPER_PORTFOLIO_STORAGE_TYPE=JPA`로 앱을 실행한다.
`.env`에서 별도로 override하지 않으면 `MARKET_PRICE_PROVIDER=SNAPSHOT`, `TRADING_CANDIDATE_SCHEDULER_EXCHANGES=UPBIT,BINANCE`, `TRADING_CANDIDATE_SCHEDULER_MARKETS=ALL_KRW,ALL_USDT`, `TRADING_EXIT_SCHEDULER_EXCHANGES=UPBIT,BINANCE`를 사용한다.
기존 `scripts/run-upbit-paper-jpa.*` 이름은 호환용 alias로만 유지한다.
실제 주문 API와 `REAL_TRADING`은 사용하지 않는다.

Session Volatility Breakout PAPER 관찰 실행:

```bash
scripts/run-session-volatility-paper-jpa.sh
```

Windows:

```bat
scripts\run-session-volatility-paper-jpa.bat
```

이 프로필은 `.env`를 수정하지 않고 런타임 환경만 다음처럼 제한한다.

- `STRATEGY_SELECTED=SESSION_VOLATILITY_BREAKOUT`
- candidate scheduler: 시작 시 OFF, `BINANCE`, `ALL_USDT`, `max-buys-per-run=1`
- exit scheduler: 시작 시 OFF, `BINANCE`
- persisted scheduler control restore 비활성화

백엔드가 뜬 뒤 `/api/system/status`의 `scheduler.candidateReadinessWarnings`가 빈 배열인지 확인하고,
문제가 없을 때만 `scripts/resume-paper-auto.sh`로 candidate/exit scheduler를 함께 켠다.

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
`candidateReadinessWarnings`는 선택 전략과 scheduler 설정이 맞지 않을 때 경고를 반환한다.
`SESSION_VOLATILITY_BREAKOUT` 관찰 운영 전에는 이 배열이 비어 있어야 한다.
운영 중 장애와 이상 사례는 `docs/operations/INCIDENT_LOG.md`에 기록한다.

자동 PAPER 매매 런타임 제어:

```http
PUT /api/scheduler/control
Content-Type: application/json

{
  "autoTradingEnabled": false,
  "candidateFixedDelayMs": 30000
}
```

- `autoTradingEnabled`는 candidate scheduler와 exit scheduler를 함께 켜거나 끈다.
- V1 풀백군 엣지 없음 결론 이후 기본 운영은 관찰/대시보드 전용이며, 새 전략 후보가 게이트를 통과하기 전까지 `false`를 유지한다.
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
- `SNAPSHOT` provider와 WebSocket 운영 중 거래소별 fresh snapshot이 0개이면 candidate/exit scheduler는 해당 거래소 실행을 건너뛴다.
- `/api/market-provider/status`의 `automationReady=false`와 `automationBlockReason`은 자동 PAPER 실행 guard가 동작 중임을 의미한다.
- candidate scheduler에서 `ALL_KRW` 또는 `ALL_USDT`를 사용하면 universe refresh 결과를 기준으로 market별 candle/price 조회가 발생할 수 있다.
- exit scheduler는 보유 PAPER position market만 평가하므로 포지션 수가 REST fallback 호출량의 상한이다.
- candidate 주기를 줄이거나 `ALL_KRW,ALL_USDT`를 동시에 켤 때는 `/api/market-provider/status`, scheduler summary, 429 로그를 함께 확인한다.
- 429 또는 fallback 실패가 발생하면 주문 성공으로 처리하지 말고 history의 `FAILED`/`REJECTED`와 Incident Log를 확인한다.

공개 시세 네트워크 진단:

```bash
scripts/diagnose-market-network.sh
```

Windows:

```bat
scripts\diagnose-market-network.bat
```

- 이 스크립트는 `.env`를 읽지 않고 민감 정보를 출력하지 않는다.
- DNS, HTTPS, TLS, WebSocket TLS endpoint 연결을 분리해서 확인한다.
- `Connection reset by peer`, TLS 실패, DNS 실패가 나오면 앱 설정보다 로컬 네트워크, DNS, 프록시, 방화벽, TLS inspection, 지역 차단을 먼저 의심한다.
- 진단 결과 HTTPS는 정상인데 `/api/market-provider/status`의 `automationReady=false`가 유지되면 WebSocket bootstrap 또는 universe refresh 로그를 확인한다.

시세 장애 후 자동 PAPER 안전 재개:

```bash
scripts/resume-paper-auto.sh
```

Windows:

```bat
scripts\resume-paper-auto.bat
```

- 이 스크립트는 백엔드 reachable 여부, 공개 시세 네트워크 진단, `/api/market-provider/status.automationReady`를 모두 확인한 뒤 scheduler를 켠다.
- `/api/system/status`의 `scheduler.candidateReadinessWarnings`가 비어 있지 않으면 scheduler를 켜지 않는다.
- 하나라도 실패하면 candidate/exit scheduler를 켜지 않고 종료한다.
- 후보 주기는 `CANDIDATE_INTERVAL_MS=30000` 또는 `60000`으로 지정할 수 있다.
- 네트워크 진단을 별도로 이미 완료한 경우에만 `SKIP_NETWORK_DIAG=true`를 사용할 수 있다.

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

Session Volatility Breakout PAPER 관찰 권장값:

```properties
STRATEGY_SELECTED=SESSION_VOLATILITY_BREAKOUT
TRADING_CANDIDATE_SCHEDULER_EXCHANGES=BINANCE
TRADING_CANDIDATE_SCHEDULER_MARKETS=ALL_USDT
TRADING_CANDIDATE_SCHEDULER_MAX_BUYS_PER_RUN=1
TRADING_EXIT_SCHEDULER_EXCHANGES=BINANCE
```

초기 관찰에서는 candidate/exit scheduler를 동시에 켜고, `/api/scheduler/status`의
`candidateReadinessWarnings`가 빈 배열인지 먼저 확인한다. 이 값은 설정 경고만 제공하며
자동으로 scheduler를 켜거나 끄지는 않는다.

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
- `candidate_scan_log`: 모든 후보 스캔 결과 SELECTED/SKIPPED append-only 기록
- `scheduler_control_setting`: 자동매매 켜기/끄기와 후보 조회 주기
- `telegram_update_offset`: Telegram inbound polling offset (JPA 저장 시 재시작 후 중복 처리 방지)

## OS별 운영 안내

웹 Dashboard와 System 화면의 `운영 환경(OS Guide)` 패널은 브라우저의 OS 정보를 안내 표시 용도로만 사용한다.
거래소 선택, 전략 판단, 리스크 검증, 주문 실행에는 OS 감지 결과를 사용하지 않는다.
System 화면은 OS별 실행 스크립트, schema script, workspace path, shell, scheduler, provider, notification/Telegram 상태를 읽기 전용으로 보여준다.

- macOS: `scripts/run-local-dev.sh`, `scripts/run-paper-jpa.sh`, `scripts/run-upbit-paper.sh`, `scripts/apply-schema.sh`, `/Users/<user>/workspace/comebot`
- Windows: `scripts\run-local-dev.bat`, `scripts\run-paper-jpa.bat`, `scripts\run-upbit-paper.bat`, `scripts\apply-schema.bat`, `%USERPROFILE%\workspace\comebot`
- Linux: `scripts/run-local-dev.sh`, `scripts/run-paper-jpa.sh`, `scripts/run-upbit-paper.sh`, `scripts/apply-schema.sh`, `$HOME/workspace/comebot`
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
