# 2026-06-12 Oversold Mean Reversion

## 목적

변동성 수축 후 돌파 이후 네 번째 대체 전략 후보로 과매도 평균회귀를 검증했다.
직전 구간 대비 급락했고, 현재 close가 직전 평균 close보다 충분히 낮으면 다음 캔들 open에 진입한다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: `KRW-BTC`, `KRW-ETH`
- Binance: `BTCUSDT`, `ETHUSDT`
- 기준봉: 1m, 3m, 5m, 15m
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `oversold-mean-reversion`
- 진입:
  - 현재 close가 lookback 시작 close 대비 최소 하락률 이상 하락
  - 현재 close가 lookback 평균 close 대비 최소 괴리율 이상 낮음
  - 현재 캔들 high-low range가 5.0% 이하
  - 조건 통과 마켓 중 `abs(dropPct) + deviationPct` 점수 1위 진입
- 진입 가격: next open 시장가 모델
- 청산: TP +2.0%, SL -2.5%, maxHold 48 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - lookback: 10, 20, 60 candles
  - minDrop: 2.0%, 4.0%, 7.0%
  - minDeviation: 1.0%, 2.0%, 4.0%
  - exchange/timeframe 조합 포함 총 216 rows

## 결과

- 생존 후보: 0
- 판정 분포:
  - `reject:sample-too-small`: 178
  - `reject:no-train-gross-edge`: 25
  - `reject:weak-train-gross-edge`: 10
  - `reject:market-concentration`: 3
- best leaderboard rows:
  - Upbit 3m, lookback=60, minDrop=4.0, minDeviation=1.0
  - full PFgross 1.489, train PFgross 1.397
  - test PFgross = Infinity지만 test trades = 3으로 `reject:sample-too-small`
- 일부 조합은 full/train gross edge가 이전 후보보다 좋아 보이나, OOS 표본이 대부분 10건 미만이고 `KRW-ETH`/`ETHUSDT` 비중이 90%+ 수준이다.

## 결론

BTC/ETH 시드 데이터 기준 과매도 평균회귀 단독 전략은 채택하지 않는다.
이번 후보는 gross edge 가능성이 가장 많이 보였지만, 현재 유니버스에서는 ETH 계열 이벤트성 반등에 의존하고 OOS 표본이 부족하다.

다음 단계는 계획 순서대로 BTC/마켓 레짐 기반 조건부 전략을 검토하되, 동시에 상위 유니버스 확장 기준을 고정해 표본 부족과 ETH 편중 문제를 먼저 줄인다.

## 산출물

- `build/backtest-leaderboard/oversold-mean-reversion.csv`
- `build/backtest-leaderboard/oversold-mean-reversion.md`
