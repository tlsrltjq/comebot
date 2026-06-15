# 2026-06-15 Session Volatility Breakout Sub-Candle Maker Audit

## 목적

15m coarse maker 감사는 다음 15m 캔들 안에서 지정가가 닿았는지만 확인했다.
운영 엔진의 실제 유효 시간은 5분이므로, 이번 감사는 15m 신호 후 300초 안에 5m/1m 원본 캔들 low가 limit price를 터치했는지 확인했다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- 거래소: Binance spot USDT 상위 30
- 신호 기준봉: 15m
- 체결 감사 기준봉: 5m, 1m
- 대상 전략: Session Volatility Breakout 1순위 후보
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `session-volatility-breakout-submaker`
- 진입:
  - 15m 신호 조건은 기존 1순위 후보와 동일
  - 신호 close 가격에 maker 지정가 생성
  - 신호 close 시각 이후 300초 안의 5m/1m 캔들 low가 limit price 이하이면 체결
  - 300초 안에 미체결이면 만료
- 후보 파라미터:
  - session: UTC 06-12
  - breakout: 20 candles
  - average: 60 candles
  - minRangeRatio: 2.5
  - minVolRatio: 1.5
  - minCloseLocation: 70.0
  - TP +4.0%, SL -2.0%, maxHold 96 candles
- 비용/유니버스:
  - 비용: base, conservative, stress
  - 유니버스: all, no-pegged, no-event, no-pegged-event
  - fill timeframe: 5m, 1m
  - 총 24 rows

## 결과

- 판정 분포:
  - `candidate:paper-observation`: 12
  - `candidate:weak-paper-observation`: 6
  - `reject:weak-train-gross-edge`: 6
- 5m과 1m 결과는 동일했다. 신호 후 첫 5분 구간에서 체결 여부가 결정된다.
- 체결률:
  - all: 277 / 280 = 98.93%
  - no-pegged: 269 / 272 = 98.90%
  - no-event: 255 / 258 = 98.84%
  - no-pegged-event: 247 / 249 = 99.20%

## 핵심 시나리오

- no-event + stress:
  - full PFgross/PFnet: 1.216 / 0.847
  - train PFgross/PFnet: 1.116 / 0.778
  - test PFgross/PFnet: 1.730 / 1.194
  - signals/fills/expiries: 258 / 255 / 3
  - decision: `candidate:weak-paper-observation`
- no-pegged + stress:
  - full PFgross/PFnet: 1.220 / 0.884
  - train PFgross/PFnet: 1.168 / 0.843
  - test PFgross/PFnet: 1.407 / 1.033
  - signals/fills/expiries: 272 / 269 / 3
  - decision: `candidate:paper-observation`
- no-pegged-event + stress:
  - full PFgross/PFnet: 1.177 / 0.849
  - train PFgross/PFnet: 1.082 / 0.778
  - test PFgross/PFnet: 1.577 / 1.154
  - signals/fills/expiries: 249 / 247 / 2
  - decision: `reject:weak-train-gross-edge`

## 결론

하위 캔들 기준 5분 유효 maker 감사도 통과로 본다.
체결률은 98.8~99.2%로 충분히 높고, no-event/no-pegged 유니버스는 stress 비용에서도 후보를 유지했다.

가장 엄격한 no-pegged-event는 test net은 좋지만 train PFgross가 1.10 미만이라 현재 공통 게이트에서는 탈락이다.
따라서 PAPER 구현 후보는 Binance 전용, UTC 06-12, 15m 신호, maker close-limit 5분 유효, no-event 또는 no-pegged 유니버스로 시작하는 것이 현실적이다.

## 산출물

- `build/backtest-leaderboard/session-volatility-breakout-submaker.csv`
- `build/backtest-leaderboard/session-volatility-breakout-submaker.md`
