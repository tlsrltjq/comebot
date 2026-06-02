# Entry Relaxation — 2026-05-27

## 배경

2026-05-27 운영 API 기준 최근 24시간 UPBIT 후보 스캔은 8,229건 모두 `SKIPPED`였다.
후보가 `SELECTED`까지 도달하지 않아 PAPER BUY가 실행되지 않는 상태였고, 주요 차단 사유는 아래와 같았다.

| 사유 | 건수 |
|------|------|
| Latest candle trade amount is below minimum threshold | 4,778 |
| Trend is not UP | 1,728 |
| No significant pump detected in window | 717 |
| Price has not pulled back sufficiently from high | 662 |
| Last candle is not bullish | 179 |
| Trade amount change rate is below threshold | 107 |
| BTC 1h trend is DOWN | 47 |
| Price is too far below the recent high | 11 |

기능 중단이 아니라 진입 필터가 평범한 장에서 지나치게 보수적으로 작동한다고 판단했다.

## 변경 사항

운영 `.env`의 후보 필터를 완화했다.

| 항목 | 변경 전 유효값 | 변경 후 |
|------|----------------|---------|
| `STRATEGY_CANDIDATE_MIN_LATEST_CANDLE_TRADE_AMOUNT_KRW` | 10,000,000 (`application.properties` 기본값) | 3,000,000 |
| `STRATEGY_CANDIDATE_MIN_LATEST_CANDLE_TRADE_AMOUNT_USDT` | 50,000 (`application.properties` 기본값) | 20,000 |
| `STRATEGY_CANDIDATE_MIN_DISTANCE_FROM_HIGH_RATE` | 0.5 | 0 |
| `STRATEGY_CANDIDATE_MAX_DISTANCE_FROM_HIGH_RATE` | 2 | 3 |

후보 스캐너는 현재 진행 중인 마지막 캔들을 제외하고 마지막으로 완성된 캔들까지만 진입 판단에 사용하도록 수정했다.
Upbit/Binance 1분봉 API는 최신 진행 중 캔들을 포함할 수 있는데, 이 캔들을 그대로 쓰면 분 초반의 낮은 거래대금 또는 미확정 양봉/음봉 때문에 `Latest candle trade amount is below minimum threshold`, `Last candle is not bullish`가 과도하게 발생한다.

이미 운영 `.env`에 적용되어 있던 아래 완화값은 유지했다.

| 항목 | 유지값 |
|------|--------|
| `STRATEGY_CANDIDATE_CANDLE_COUNT` | 10 |
| `STRATEGY_CANDIDATE_MIN_PRICE_CHANGE_RATE` | 0.3 |
| `STRATEGY_CANDIDATE_MIN_TRADE_AMOUNT_CHANGE_RATE` | 8 |
| `STRATEGY_CANDIDATE_MAX_PRICE_CHANGE_RATE` | 10 |
| `STRATEGY_CANDIDATE_MAX_HIGH_LOW_RANGE_RATE` | 20 |

## 의도

- 최신 1분봉 거래대금 하한을 낮춰 상위 거래대금 종목이 아니어도 후보로 평가될 수 있게 한다.
- 최소 눌림폭 조건을 꺼서 고점 근처에서 이어지는 완만한 상승장도 진입 후보로 허용한다.
- 고점 대비 최대 이탈폭을 3%로 넓혀 2%를 조금 넘는 정상 눌림을 과도하게 배제하지 않는다.
- 현재 진행 중인 미완성 캔들 대신 마지막 완성 캔들을 사용해 거래대금/양봉 판단의 흔들림을 줄인다.

## 롤백

후보가 과도하게 늘거나 손절이 빠르게 증가하면 아래 값으로 되돌린다.

```env
STRATEGY_CANDIDATE_MIN_LATEST_CANDLE_TRADE_AMOUNT_KRW=10000000
STRATEGY_CANDIDATE_MIN_LATEST_CANDLE_TRADE_AMOUNT_USDT=50000
STRATEGY_CANDIDATE_MAX_DISTANCE_FROM_HIGH_RATE=2
STRATEGY_CANDIDATE_MIN_DISTANCE_FROM_HIGH_RATE=0.5
```

롤백 후에는 app 컨테이너를 재생성해야 한다.

```bash
docker compose up -d --force-recreate app
```

미완성 캔들 제외 로직까지 되돌려야 하면 `CandidateScannerService.removeIncompleteLatestCandle` 호출과 관련 테스트를 되돌린다.

## 관찰 기준

24시간 단위로 아래를 확인한다.

- `GET /api/candidate-scan-log?range=24h&exchange=UPBIT`
- `GET /api/candidate-scan-log?range=24h&exchange=UPBIT&decision=SELECTED`
- `GET /api/trading-flow/history?exchange=UPBIT&limit=100`
- `GET /api/analytics/summary?exchange=UPBIT&range=24h`

후보가 늘었는데 `Stop loss rate reached` 또는 stop-loss cooldown 거절이 급증하면 되돌린다.

## 2026-05-28 후속 조정

24시간 관찰 결과 UPBIT과 BINANCE의 동작이 크게 갈렸다.

- UPBIT: 후보 `SELECTED` 2건, 실제 BUY 1건으로 여전히 과소 진입
- BINANCE: 후보 `SELECTED` 2,657건, BUY 92건, SELL 80건으로 과매매
- BINANCE 최근 24시간 실현손익: `-1.49184318 USDT`
- BINANCE 반복 손절 종목 다수 발생

이에 따라 공통 후보 필터를 exchange별 override로 분리한다.

```env
STRATEGY_CANDIDATE_UPBIT_CANDLE_COUNT=20
STRATEGY_CANDIDATE_UPBIT_MIN_PRICE_CHANGE_RATE=0.15
STRATEGY_CANDIDATE_UPBIT_MIN_TRADE_AMOUNT_CHANGE_RATE=0
STRATEGY_CANDIDATE_UPBIT_MIN_LATEST_CANDLE_TRADE_AMOUNT_KRW=1000000
STRATEGY_CANDIDATE_UPBIT_MAX_DISTANCE_FROM_HIGH_RATE=5
STRATEGY_CANDIDATE_UPBIT_MIN_DISTANCE_FROM_HIGH_RATE=0

STRATEGY_CANDIDATE_BINANCE_CANDLE_COUNT=10
STRATEGY_CANDIDATE_BINANCE_MIN_PRICE_CHANGE_RATE=0.8
STRATEGY_CANDIDATE_BINANCE_MIN_TRADE_AMOUNT_CHANGE_RATE=30
STRATEGY_CANDIDATE_BINANCE_MIN_LATEST_CANDLE_TRADE_AMOUNT_USDT=50000
STRATEGY_CANDIDATE_BINANCE_MAX_DISTANCE_FROM_HIGH_RATE=2
STRATEGY_CANDIDATE_BINANCE_MIN_DISTANCE_FROM_HIGH_RATE=0.5
```

UPBIT은 후보를 늘리는 방향, BINANCE는 후보를 줄이는 방향이다.
공통 전역값은 fallback으로만 사용한다.
