# 2026-06-12 Volume Surge Continuation

## 목적

상대강도 모멘텀 이후 두 번째 대체 전략 후보로 거래대금 급증 후 추세 지속을 검증했다.
현재 캔들의 거래대금이 직전 평균 대비 크게 증가하고, 동시에 양봉/상승률 조건을 만족하면 다음 캔들 open에 진입한다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: `KRW-BTC`, `KRW-ETH`
- Binance: `BTCUSDT`, `ETHUSDT`
- 기준봉: 1m, 3m, 5m, 15m
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `volume-surge-continuation`
- 진입:
  - 직전 N개 캔들 평균 거래대금 대비 현재 캔들 거래대금 비율 필터
  - 현재 캔들 상승률 필터
  - 조건 통과 마켓 중 `volumeRatio * candleReturn` 점수 1위 진입
- 진입 가격: next open 시장가 모델
- 청산: TP +4.0%, SL -2.0%, maxHold 96 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - averageWindow: 10, 20, 60 candles
  - minVolumeRatio: 3.0, 5.0, 8.0
  - minCandleReturn: 0.3%, 0.7%, 1.2%
  - exchange/timeframe 조합 포함 총 216 rows

## 결과

- 생존 후보: 0
- 판정 분포:
  - `reject:sample-too-small`: 102
  - `reject:market-concentration`: 51
  - `reject:weak-train-gross-edge`: 42
  - `reject:no-train-gross-edge`: 21
- best test PFgross:
  - Binance 1m, window=10, minVolRatio=3.0, minCandleReturn=1.2
  - test PFgross = Infinity지만 test 표본이 작아 `reject:sample-too-small`
- 비교적 좋아 보이는 Binance 3m 조합은 train/test PF는 양호하나 ETHUSDT 편중이 80%+로 높아 탈락했다.

## 결론

BTC/ETH 시드 데이터 기준 거래대금 급증 후 추세 지속 단독 전략은 채택하지 않는다.
test 구간에서 강한 결과가 나온 조합들은 표본이 부족하거나 특정 ETHUSDT/KRW-ETH에 과도하게 의존한다.

다음 후보는 계획 순서대로 변동성 수축 후 돌파 전략을 검토한다.

## 산출물

- `build/backtest-leaderboard/volume-surge-continuation.csv`
- `build/backtest-leaderboard/volume-surge-continuation.md`
