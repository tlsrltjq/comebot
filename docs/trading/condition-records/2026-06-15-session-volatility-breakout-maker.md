# 2026-06-15 Session Volatility Breakout Maker Audit

## 목적

Session Volatility Breakout 1순위 후보를 next-open 진입이 아니라 maker 지정가 모델로 1차 감사했다.
운영 엔진은 신호 캔들 close 가격에 maker 지정가를 걸고 5분 유효로 관찰한다.
이번 감사는 15m 원본 캔들만 사용한 coarse 모델이므로, 신호 close 지정가가 다음 15m 캔들 low에 닿는 경우만 체결로 보았다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- 거래소: Binance spot USDT 상위 30
- 기준봉: 15m 원본 캔들
- 대상 전략: Session Volatility Breakout 1순위 후보
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `session-volatility-breakout-maker`
- 진입:
  - 신호 조건은 기존 1순위 후보와 동일
  - 신호 캔들 close 가격에 maker 지정가 생성
  - 같은 신호 캔들에서는 체결 금지
  - 다음 15m 캔들 low가 limit price 이하이면 체결
  - 다음 15m 캔들에서 미체결이면 만료
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
  - 총 12 rows

## 결과

- 판정 분포:
  - `candidate:paper-observation`: 6
  - `candidate:weak-paper-observation`: 6
- 체결률:
  - all: 277 / 278 = 99.64%
  - no-pegged: 269 / 270 = 99.63%
  - no-event: 255 / 256 = 99.61%
  - no-pegged-event: 247 / 247 = 100.00%
- no-pegged-event + stress:
  - full PFgross/PFnet: 1.192 / 0.862
  - train PFgross/PFnet: 1.102 / 0.793
  - test PFgross/PFnet: 1.577 / 1.154
  - signals/fills/expiries: 247 / 247 / 0
  - topMarket: `STGUSDT` 13.0%
  - decision: `candidate:weak-paper-observation`
- all + stress:
  - full PFgross/PFnet: 1.257 / 0.886
  - train PFgross/PFnet: 1.214 / 0.856
  - test PFgross/PFnet: 1.435 / 1.007
  - signals/fills/expiries: 278 / 277 / 1
  - decision: `candidate:paper-observation`

## 결론

15m coarse maker 감사에서는 후보가 유지된다.
신호 close 지정가가 다음 15m 캔들에서 거의 항상 닿아 체결률이 99.6% 이상으로 나왔다.

하지만 이 결과는 실제 5분 유효 지정가보다 낙관적일 수 있다.
15m 캔들의 low가 언제 발생했는지 모르기 때문에, 다음 단계에서는 5m 또는 1m 하위 캔들을 연결해 신호 후 5분 안에 실제로 limit price를 터치했는지 재검증해야 한다.
이 하위 캔들 감사까지 통과하면 PAPER 자동매매 후보 전략 구현으로 넘어간다.

## 캐시 메모

현재 `.backtest_cache`는 약 8.9GB다.
하위 캔들 maker 감사와 PAPER 전략 구현 여부가 결정되기 전까지는 삭제하지 않는다.
후보가 확정되면 Binance 15m 신호용 데이터와 5m/1m 체결 감사용 데이터만 남기는 prune을 검토한다.

## 산출물

- `build/backtest-leaderboard/session-volatility-breakout-maker.csv`
- `build/backtest-leaderboard/session-volatility-breakout-maker.md`
