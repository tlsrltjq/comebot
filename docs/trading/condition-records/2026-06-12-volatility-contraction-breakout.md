# 2026-06-12 Volatility Contraction Breakout

## 목적

거래대금 추세 지속 이후 세 번째 대체 전략 후보로 변동성 수축 후 돌파를 검증했다.
직전 구간의 평균 high-low range가 낮고, 현재 종가가 최근 고점을 일정 비율 이상 돌파하면 다음 캔들 open에 진입한다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: `KRW-BTC`, `KRW-ETH`
- Binance: `BTCUSDT`, `ETHUSDT`
- 기준봉: 1m, 3m, 5m, 15m
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `volatility-contraction-breakout`
- 진입:
  - 직전 N개 캔들의 평균 high-low range가 상한 이하
  - 현재 close가 직전 N개 캔들의 high를 최소 돌파율 이상 상향 돌파
  - 조건 통과 마켓 중 `breakoutPct / avgRange` 점수 1위 진입
- 진입 가격: next open 시장가 모델
- 청산: TP +4.0%, SL -2.0%, maxHold 96 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - contractionWindow: 10, 20, 60 candles
  - maxAvgRange: 0.4%, 0.8%, 1.2%
  - minBreakout: 0.2%, 0.5%, 1.0%
  - exchange/timeframe 조합 포함 총 216 rows

## 결과

- 생존 후보: 0
- 판정 분포:
  - `reject:no-train-gross-edge`: 100
  - `reject:sample-too-small`: 72
  - `reject:weak-train-gross-edge`: 31
  - `reject:market-concentration`: 9
  - `reject:oos-collapse`: 4
- best leaderboard rows:
  - Binance 1m, window=10, maxAvgRange=0.8, minBreakout=1.0
  - full PFgross 0.605, train PFgross 0.564
  - test PFgross = Infinity지만 test trades = 1로 `reject:sample-too-small`
- 비교적 좋아 보이는 Binance 3m 일부 조합은 gross edge가 1.18~1.22 수준까지 올라가지만 ETHUSDT 편중이 78%+라 탈락했다.

## 결론

BTC/ETH 시드 데이터 기준 변동성 수축 후 돌파 단독 전략은 채택하지 않는다.
OOS 일부 구간이 강하게 보이는 조합은 표본이 작고, 표본이 충분한 조합은 train gross edge가 부족하거나 특정 ETHUSDT/KRW-ETH에 과도하게 의존한다.

다음 후보는 계획 순서대로 과매도 평균회귀 전략을 검토한다.

## 산출물

- `build/backtest-leaderboard/volatility-contraction-breakout.csv`
- `build/backtest-leaderboard/volatility-contraction-breakout.md`
