# 2026-06-15 Ranked Rotation Robustness

## 목적

이전 Ranked Rotation 리더보드에서 나온 Binance 15m 약한 PAPER 관찰 후보 6개를 운영 전환 전에 더 보수적으로 검증했다.
검증 관점은 비용/슬리피지 민감도, `ZECUSDT` 편중, stable/pegged 자산 제외, 신규/이벤트성 마켓 제외다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- 거래소: Binance spot USDT 상위 30
- 기준봉: 15m 원본 캔들
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `ranked-rotation-robustness`
- 대상 파라미터:
  - lookback: 60, 240 candles
  - rankCount: 1
  - rebalance: 20 candles
  - minReturn: 0.5%, 1.5%, 3.0%
  - TP +4.0%, SL -2.0%, maxHold 240 candles
- 비용 시나리오:
  - `base`: maker 0.05%, taker 0.05%, slippage 0.05%
  - `conservative`: maker 0.10%, taker 0.10%, slippage 0.10%
  - `stress`: maker 0.10%, taker 0.10%, slippage 0.20%
- 유니버스 시나리오:
  - `all`: 전체 Binance 30
  - `no-zec`: `ZECUSDT` 제외
  - `no-pegged`: `USD1USDT`, `USDCUSDT`, `USDEUSDT`, `XAUTUSDT` 제외
  - `no-zec-pegged-recent`: `ZECUSDT`, pegged/stable, `BABYUSDT`, `MEGAUSDT`, `NIGHTUSDT`, `TRUMPUSDT` 제외
- 총 72 rows

## 결과

- 판정 분포:
  - `watch:gross-edge-net-weak`: 30
  - `reject:weak-train-gross-edge`: 24
  - `candidate:weak-paper-observation`: 15
  - `watch:strong-gross-net-weak`: 2
  - `candidate:paper-observation`: 1
- 비용별 결론:
  - `base`: 후보가 남는다. `no-zec`에서도 lookback=60/minReturn=3.0은 `candidate:paper-observation`.
  - `conservative`: 모든 후보가 `watch:*net-weak` 또는 탈락으로 강등된다.
  - `stress`: 모든 후보가 `watch:*net-weak` 또는 탈락으로 강등된다.
- 유니버스별 결론:
  - `no-zec`: 기본 비용에서는 후보가 남고 topMarket이 `DEXEUSDT` 또는 `STGUSDT`로 이동한다.
  - `no-zec-pegged-recent`: 기본 비용에서는 lookback=60 계열 약한 후보가 남지만, lookback=240 계열은 train gross edge가 1.10 미만으로 탈락한다.

## 대표 수치

- best base/no-zec:
  - params: `universe=no-zec,cost=base,lookback=60,rankCount=1,rebalance=20,minReturn=3.0`
  - full PFgross/PFnet: 1.175 / 1.024
  - train PFgross/PFnet: 1.158 / 1.009
  - test PFgross/PFnet: 1.210 / 1.057
  - topMarket: `STGUSDT` 12.7%
  - decision: `candidate:paper-observation`
- same structure under conservative cost:
  - full PFnet: 0.897
  - train PFnet: 0.883
  - test PFnet: 0.926
  - decision: `watch:strong-gross-net-weak`
- base/no-zec-pegged-recent lookback=60,minReturn=3.0:
  - full PFgross/PFnet: 1.162 / 1.015
  - train PFgross/PFnet: 1.142 / 0.995
  - test PFgross/PFnet: 1.225 / 1.075
  - topMarket: `DEXEUSDT` 13.9%
  - decision: `candidate:weak-paper-observation`

## 결론

Ranked Rotation은 비용 전 신호 엣지는 있다. `ZECUSDT` 하나만의 착시는 아니며, 기본 비용에서는 제외 유니버스에서도 후보가 남는다.
하지만 비용 여유가 매우 얇다. 보수 비용/슬리피지에서 모든 후보가 net weak로 강등되므로 지금 단계에서 PAPER 자동매매로 켜지 않는다.

다음 조치는 두 갈래다.
첫째, 이 전략을 살리려면 maker 지정가 체결 지연/미체결 모델을 반영해 실제 운영 체결 비용이 base에 가까운지 검증해야 한다.
둘째, 전략 리서치 순서상 시간대/세션별 변동성 후보를 새로 실험한다.

## 산출물

- `build/backtest-leaderboard/ranked-rotation-robustness.csv`
- `build/backtest-leaderboard/ranked-rotation-robustness.md`
