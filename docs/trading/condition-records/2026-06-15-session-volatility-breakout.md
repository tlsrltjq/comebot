# 2026-06-15 Session Volatility Breakout

## 목적

시간대별로 변동성/거래대금이 커지는 구간에만 돌파 진입하는 후보를 검증했다.
이전 Ranked Rotation이 비용 민감도가 컸기 때문에, 이번 실험은 종목 순위가 아니라 세션별 변동성 확장과 돌파 자체에 엣지가 있는지 확인하는 데 목적이 있다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: KRW 24h 거래대금 상위 30
- Binance: spot USDT quote volume 상위 30
- 기준봉: 1m, 3m, 5m, 15m 원본 캔들
- 원본 파일: `.backtest_cache` 240개 JSON
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `session-volatility-breakout`
- 진입:
  - UTC 세션 필터 통과
  - 현재 close가 이전 `breakoutWindow` 고점 돌파
  - 현재 high-low range가 평균 range 대비 `minRangeRatio` 이상
  - 현재 거래대금이 평균 거래대금 대비 `minVolRatio` 이상
  - close 위치가 캔들 range 상단 70% 이상
  - 조건 통과 마켓 중 score 1위만 다음 캔들 open 진입
- 청산: TP +4.0%, SL -2.0%, maxHold 96 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - timeframe: 1m, 3m, 5m, 15m
  - session: all, UTC 00-06, 06-12, 12-18, 18-24
  - breakoutWindow: 20, 60 candles
  - averageWindow: 20, 60 candles
  - minRangeRatio: 1.5, 2.5
  - minVolRatio: 1.5, 3.0
  - exchange/timeframe/session 조합 포함 총 640 rows

## 결과

- 생존 후보: 13
- 판정 분포:
  - `reject:no-train-gross-edge`: 417
  - `reject:weak-train-gross-edge`: 148
  - `reject:oos-collapse`: 43
  - `watch:strong-gross-net-weak`: 10
  - `watch:gross-edge-net-weak`: 9
  - `candidate:weak-paper-observation`: 7
  - `candidate:paper-observation`: 6
- 후보 분포:
  - Binance: `candidate:paper-observation` 6, `candidate:weak-paper-observation` 6
  - Upbit: `candidate:weak-paper-observation` 1
  - Binance 핵심 구간: 15m UTC 06-12, 3m UTC 12-18, 3m UTC 18-24

## 대표 후보

- Binance 3m UTC 12-18:
  - params: `session=utc12-18,breakout=60,avg=20,minRangeRatio=1.5,minVolRatio=3.0`
  - full PFgross/PFnet: 1.263 / 1.040
  - train PFgross/PFnet: 1.216 / 1.000
  - test PFgross/PFnet: 1.497 / 1.234
  - full/test trades: 569 / 102
  - topMarket: `TRXUSDT` 10.5%
  - decision: `candidate:paper-observation`
- Binance 15m UTC 06-12:
  - params: `session=utc06-12,breakout=20,avg=60,minRangeRatio=2.5,minVolRatio=1.5`
  - full PFgross/PFnet: 1.270 / 1.110
  - train PFgross/PFnet: 1.230 / 1.076
  - test PFgross/PFnet: 1.435 / 1.250
  - full/test trades: 279 / 58
  - topMarket: `ZECUSDT` 9.7%
  - decision: `candidate:paper-observation`
- Upbit 15m UTC 00-06:
  - params: `session=utc00-06,breakout=60,avg=60,minRangeRatio=2.5,minVolRatio=3.0`
  - full PFgross/PFnet: 1.117 / 0.992
  - train PFgross/PFnet: 1.108 / 0.983
  - test PFgross/PFnet: 1.147 / 1.021
  - decision: `candidate:weak-paper-observation`

## 결론

Session Volatility Breakout은 Ranked Rotation보다 더 강한 1차 후보를 냈다.
특히 Binance 3m UTC 12-18과 Binance 15m UTC 06-12는 train/test gross가 모두 양호하고 topMarket 편중도 10% 안팎이라 단일 마켓 착시 가능성이 상대적으로 낮다.

다만 아직 기본 비용만 적용했다.
PAPER 자동매매로 켜기 전 비용/슬리피지 스트레스, pegged/stable 및 이벤트성 신규 마켓 제외, 세션별 안정성, maker 지정가 체결 지연/미체결 모델을 정밀 검증한다.

## 산출물

- `build/backtest-leaderboard/session-volatility-breakout.csv`
- `build/backtest-leaderboard/session-volatility-breakout.md`
