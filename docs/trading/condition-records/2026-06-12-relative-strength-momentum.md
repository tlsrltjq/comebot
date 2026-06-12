# 2026-06-12 Relative Strength Momentum

## 목적

V1 풀백 반등군 이후 첫 대체 전략 후보로 상대강도 기반 모멘텀을 검증했다.
각 시점에서 lookback return이 가장 높은 마켓을 매수하고, TP/SL/time-stop으로 청산한다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: `KRW-BTC`, `KRW-ETH`
- Binance: `BTCUSDT`, `ETHUSDT`
- 기준봉: 1m, 3m, 5m, 15m
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `relative-strength-momentum`
- 진입: lookback return 1위 마켓, next open 시장가 모델
- 청산: TP +4.0%, SL -2.0%, maxHold 96 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - lookback: 20, 60, 240 candles
  - minReturn: 0.5%, 1.0%, 2.0%
  - exchange/timeframe 조합 포함 총 72 rows

## 결과

- 생존 후보: 0
- 판정 분포:
  - `reject:no-train-gross-edge`: 34
  - `reject:weak-train-gross-edge`: 31
  - `reject:market-concentration`: 7
- best test PFgross:
  - Upbit 1m, lookback=20, minReturn=2.0
  - test PFgross 3.837, test PFnet 2.404
  - train PFgross 1.031로 1차 생존 기준 1.10 미달
  - top market `KRW-ETH` 95.5% 편중
- Binance 최상위권도 ETHUSDT 편중이 90%+ 수준으로 높아 일반화 후보로 보지 않는다.

## 결론

BTC/ETH 시드 데이터 기준 상대강도 모멘텀 단독 전략은 채택하지 않는다.
test 구간 일부 성과는 있으나 train gross edge가 약하거나 특정 ETH 계열로 편중되어,
현재 기준에서는 OOS 생존 후보가 아니다.

다음 후보는 계획 순서대로 거래대금 급증 후 추세 지속 전략을 검토한다.

## 산출물

- `build/backtest-leaderboard/relative-strength-momentum.csv`
- `build/backtest-leaderboard/relative-strength-momentum.md`
