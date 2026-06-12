# 2026-06-12 BTC Market Regime Momentum

## 목적

과매도 평균회귀 이후 다섯 번째 대체 전략 후보로 BTC/마켓 레짐 기반 조건부 모멘텀을 검증했다.
BTC가 일정 lookback에서 상승 레짐이고, 개별 마켓도 같은 방향의 상승 모멘텀을 보일 때만 다음 캔들 open에 진입한다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: `KRW-BTC`, `KRW-ETH`
- Binance: `BTCUSDT`, `ETHUSDT`
- 기준봉: 1m, 3m, 5m, 15m
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `btc-market-regime-momentum`
- 진입:
  - BTC lookback return이 최소 기준 이상
  - 개별 마켓 lookback return이 최소 기준 이상
  - 조건 통과 마켓 중 `btcReturn + marketReturn` 점수 1위 진입
- 진입 가격: next open 시장가 모델
- 청산: TP +4.0%, SL -2.0%, maxHold 96 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - btcLookback: 20, 60, 240 candles
  - marketLookback: 20, 60 candles
  - minBtcReturn: 0.5%, 1.5%, 3.0%
  - minMarketReturn: 1.0%, 2.0%, 4.0%
  - exchange/timeframe 조합 포함 총 432 rows

## 결과

- 생존 후보: 0
- 판정 분포:
  - `reject:sample-too-small`: 270
  - `reject:no-train-gross-edge`: 73
  - `reject:weak-train-gross-edge`: 51
  - `reject:market-concentration`: 38
- best leaderboard rows:
  - Upbit 5m, btcLookback=20, marketLookback=60, minBtcReturn=1.5, minMarketReturn=4.0
  - full PFgross 1.118, train PFgross 1.067
  - test PFgross = Infinity지만 test trades = 1로 `reject:sample-too-small`
- Binance 1m 일부 조합은 full/train PFgross가 높지만 test 표본이 1~5건 수준이거나 `ETHUSDT` 편중이 90% 안팎이다.

## 결론

BTC/ETH 시드 데이터 기준 BTC/마켓 레짐 조건부 모멘텀 단독 전략은 채택하지 않는다.
레짐 필터는 일부 수치를 크게 만들지만, 현재 유니버스에서는 거래 수를 지나치게 줄이고 ETH 계열 편중을 더 강화한다.

다음 단계는 거래대금 상위 30개 고정 유니버스를 실제 수집해 표본 부족과 단일 마켓 편중 문제를 먼저 줄인다.

## 산출물

- `build/backtest-leaderboard/btc-market-regime-momentum.csv`
- `build/backtest-leaderboard/btc-market-regime-momentum.md`
