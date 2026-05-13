# Binance PAPER Smoke Test

## 목적

Binance candidate scheduler가 `PAPER_TRADING` 범위 안에서만 동작하고, Binance portfolio/history에만 결과를 남기는지 확인한다.
실제 Binance 주문 API, API key, secret, REAL_TRADING은 사용하지 않는다.

## 안전 실행 설정

한 번에 하나의 Binance symbol만 테스트한다.
Upbit scheduler와 exit scheduler는 끈다.

```properties
MARKET_PRICE_PROVIDER=SNAPSHOT
MARKET_WEBSOCKET_ENABLED=true
MARKET_WEBSOCKET_UPBIT_ENABLED=false
MARKET_WEBSOCKET_BINANCE_ENABLED=true
MARKET_UPBIT_KRW_TICKER_POLLING_ENABLED=false
MARKET_BINANCE_USDT_TICKER_POLLING_ENABLED=false
TRADING_SCHEDULER_ENABLED=false
TRADING_SCHEDULER_MARKETS=BTCUSDT
TRADING_CANDIDATE_SCHEDULER_ENABLED=true
TRADING_CANDIDATE_SCHEDULER_EXCHANGE=BINANCE
TRADING_CANDIDATE_SCHEDULER_MARKETS=BTCUSDT
TRADING_CANDIDATE_SCHEDULER_FIXED_DELAY_MS=60000
TRADING_EXIT_SCHEDULER_ENABLED=false
```

스모크 테스트 후에는 `TRADING_CANDIDATE_SCHEDULER_ENABLED=false`로 되돌려 반복 HOLD history 적재를 막는다.

## 확인 API

```http
GET /api/scheduler/status
GET /api/market-provider/status
GET /api/trading-flow/history?exchange=binance&limit=5
GET /api/trading-flow/history?exchange=upbit&limit=5
GET /api/portfolio/status?exchange=binance
GET /api/portfolio/status?exchange=upbit
```

## 2026-05-08 결과

- 설정:
  - candidate exchange: `BINANCE`
  - candidate markets: `BTCUSDT`
  - candidate scheduler: enabled during smoke test, disabled after test
  - trading scheduler: disabled
  - exit scheduler: disabled
- 실행 결과:
  - scheduler summary: `requested=1, executed=1, filled=0, rejected=0, hold=1, failed=0`
  - Binance history: `BTCUSDT` HOLD 1건 저장
  - Upbit history: 신규 기록 없음
  - Binance portfolio: `1000 USDT`, realized profit `0`
  - Upbit portfolio: `1000000 KRW`, realized profit `0`
- 확인:
  - Binance PAPER 후보 실행 결과가 Binance history에만 저장됐다.
  - Upbit portfolio/history와 섞이지 않았다.
  - 실제 주문 API는 호출하지 않았다.

## 발견한 이슈

`BTCUSDT` 후보 스캔은 `Candidate scan failed: IllegalArgumentException - accumulatedTradePrice must be positive`로 HOLD 처리됐다.
즉, exchange 분리와 history 저장은 정상이나, Binance candle 응답 중 일부 캔들의 quote volume이 0인 경우 volatility 계산이 실패할 수 있다.

## 2026-05-08 추가 개선 결과

후보 스캐너가 거래대금 0 캔들을 지표 계산 전에 제외하도록 수정했다.
필터링 후 유효 캔들이 2개 미만이면 예외가 아니라 `Not enough valid trade amount candles` HOLD 사유로 처리한다.

재검증 결과:

- Binance candidate scheduler: `BTCUSDT` 1개
- scheduler summary: `requested=1, executed=1, filled=0, rejected=0, hold=1, failed=0`
- Binance history: `BTCUSDT` HOLD 1건 저장
- HOLD 사유: `Price change rate is below threshold`
- 이전 `accumulatedTradePrice must be positive` 예외 로그는 재현되지 않았다.
- Binance portfolio: `1000 USDT`, realized profit `0`

다음 개선 후보:

- Binance smoke test용 symbol을 `BTCUSDT` 하나에 고정하지 말고, `ALL_USDT` 상위 거래대금 symbol 중 최근 캔들 거래대금이 유효한 symbol을 선택한다.
- Binance 후보 스캔 실패 사유를 history와 UI에서 더 짧게 표시한다.

## 2026-05-09 운영형 확인 결과

Upbit/Binance 동시 PAPER 실행과 JPA 포트폴리오 저장을 켠 상태에서 Binance 흐름을 확인했다.

설정:

- history storage: `JPA`
- paper portfolio storage: `JPA`
- market provider: `SNAPSHOT`
- WebSocket: Upbit/Binance enabled
- candidate exchanges: `UPBIT, BINANCE`
- candidate markets: `ALL_KRW, ALL_USDT`
- candidate delay: `30000`
- exit exchanges: `UPBIT, BINANCE`
- exit delay: `5000`
- Upbit order amount: `10000 KRW`
- Binance order amount: `10 USDT`
- REAL_TRADING/실제 주문 API: 사용 안 함

확인 API:

```http
GET /api/analytics/summary?exchange=binance&range=1h
GET /api/trading-flow/history?exchange=binance&limit=20
GET /api/portfolio/valuation?exchange=binance
GET /api/system/status?exchange=binance
GET /api/market-provider/status
```

결과:

- Binance 1시간 이력: `total=3016`
- BUY 신호: `115`
- SELL 신호: `16`
- HOLD: `2885`
- FILLED: `121`
- REJECTED: `10`
- FAILED: `0`
- 손절 SELL: `11`
- 익절 SELL: `5`
- 평균 손절률: `-0.79133155%`
- 평균 익절률: `1.70066040%`
- Binance 포트폴리오:
  - cash: `899.726194915109190500 USDT`
  - position value: `100.084822511675`
  - total equity: `999.8110174267841905`
  - realized profit: `-0.2738050895083335`
  - unrealized profit: `0.0848178231409074`
  - total profit: `-0.1889872663674261`
- Binance 보유 포지션: `ENAUSDT`, `GALAUSDT`, `NILUSDT`, `STRKUSDT`, `WLFIUSDT`
- market provider snapshot:
  - total snapshots: `100`
  - Upbit snapshots: `50`
  - Binance snapshots: `50`

판단:

- Binance 후보 실행, PAPER 매수, 청산 평가, history 분리, portfolio 분리 저장은 정상 동작한다.
- `PAPER_PORTFOLIO_STORAGE_TYPE=JPA`에서 재시작 후에도 포트폴리오가 유지된다.
- 현재 단기 손익은 소폭 마이너스다.
- 손실의 주 원인은 손절 11건이 익절 5건보다 많고, 잦은 후보 진입으로 작은 손절이 누적되는 흐름이다.

다음 개선 후보:

- 거래소별 손절/익절/재진입 조건을 분리한다.
- Binance는 주문 단위가 작아 작은 가격 흔들림에도 잦은 청산이 발생하므로, 최소 보유 시간 또는 청산 확인 조건을 추가한다.
- 자동매매 제어 설정은 아직 런타임 메모리라 재시작 시 환경변수 기본값으로 돌아간다. 다음 작업에서 설정 저장소에 영속화한다.
