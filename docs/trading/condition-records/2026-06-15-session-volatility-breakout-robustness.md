# 2026-06-15 Session Volatility Breakout Robustness

## 목적

Session Volatility Breakout 1차 리더보드에서 나온 생존 후보 13개를 PAPER 구현 전 다시 검증했다.
검증 관점은 비용/슬리피지 민감도, stable/pegged 제외, 이벤트성 신규 마켓 제외, 세션별 후보 안정성이다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- 대상 후보: 1차 리더보드의 `candidate:paper-observation` 6개 + `candidate:weak-paper-observation` 7개
- 기준봉: 후보별 3m 또는 15m 원본 캔들
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `session-volatility-breakout-robustness`
- 비용 시나리오:
  - `base`: maker 0.05%, taker 0.05%, slippage 0.05%
  - `conservative`: maker 0.10%, taker 0.10%, slippage 0.10%
  - `stress`: maker 0.10%, taker 0.10%, slippage 0.20%
- 유니버스 시나리오:
  - `all`: 전체 상위 30
  - `no-pegged`: `USD1USDT`, `USDCUSDT`, `USDEUSDT`, `XAUTUSDT`, `KRW-USDT` 제외
  - `no-event`: `BABYUSDT`, `MEGAUSDT`, `NIGHTUSDT`, `TRUMPUSDT`, `KRW-MEGA`, `KRW-TRUMP` 제외
  - `no-pegged-event`: 위 두 조건 동시 적용
- 총 156 rows

## 결과

- 판정 분포:
  - `reject:weak-train-gross-edge`: 60
  - `watch:gross-edge-net-weak`: 28
  - `candidate:paper-observation`: 23
  - `candidate:weak-paper-observation`: 20
  - `watch:strong-gross-net-weak`: 19
  - `reject:oos-collapse`: 6
- 비용별 후보 수:
  - `base`: paper 13 + weak 13
  - `conservative`: paper 6 + weak 4
  - `stress`: paper 4 + weak 3
- Upbit 15m UTC 00-06 약한 후보는 보수 비용부터 net weak로 강등된다.
- Binance 3m UTC 12-18 후보는 일부 stress까지 생존하지만 net 여유가 얇다.
- Binance 15m UTC 06-12 후보는 비용/유니버스 스트레스에서 가장 안정적이다.

## 1순위 후보

- exchange/timeframe/session: Binance 15m UTC 06-12
- params: `breakout=20,avg=60,minRangeRatio=2.5,minVolRatio=1.5,minCloseLocation=70.0,tp=4.0,sl=-2.0,maxHold=96`
- no-pegged-event + stress:
  - full PFgross/PFnet: 1.207 / 0.873
  - train PFgross/PFnet: 1.120 / 0.807
  - test PFgross/PFnet: 1.576 / 1.153
  - full/test trades: 248 / 51
  - topMarket: `STGUSDT` 12.9%
  - decision: `candidate:weak-paper-observation`
- no-event + stress:
  - full PFgross/PFnet: 1.248 / 0.871
  - train PFgross/PFnet: 1.155 / 0.808
  - test PFgross/PFnet: 1.720 / 1.179
  - topMarket: `ZECUSDT` 11.3%
  - decision: `candidate:paper-observation`

## 결론

Session Volatility Breakout은 현재까지 가장 설득력 있는 후보이다.
Ranked Rotation과 달리 비용 스트레스에서도 후보가 완전히 사라지지 않았고, no-pegged-event 조건에서도 Binance 15m UTC 06-12 조합이 살아남았다.

다만 full/train PFnet은 stress 비용에서 1.0 미만이다.
즉, 신호 엣지는 있지만 실제 PAPER 운영으로 옮기려면 maker 지정가 체결 지연/미체결 모델을 반드시 통과해야 한다.
다음 단계는 이 1순위 후보에 운영 엔진의 maker 체결 모델을 붙여 재검증하는 것이다.

## 산출물

- `build/backtest-leaderboard/session-volatility-breakout-robustness.csv`
- `build/backtest-leaderboard/session-volatility-breakout-robustness.md`
