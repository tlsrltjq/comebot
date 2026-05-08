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

다음 개선 후보:

- Binance candle에서 0 거래대금 캔들을 필터링하거나, 전략 계산에서 0 거래대금 구간을 명확한 HOLD 사유로 처리한다.
- Binance smoke test용 symbol을 `BTCUSDT` 하나에 고정하지 말고, `ALL_USDT` 상위 거래대금 symbol 중 최근 캔들 거래대금이 유효한 symbol을 선택한다.
- Binance 후보 스캔 실패 사유를 history와 UI에서 더 짧게 표시한다.
