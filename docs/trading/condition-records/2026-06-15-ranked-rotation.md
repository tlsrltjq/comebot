# 2026-06-15 Ranked Rotation

## 목적

상위 30 고정 유니버스로 확장한 뒤, 단일 코인 진입 신호가 아니라 종목 간 상대 강도 순위를 이용하는 로테이션 전략을 검증했다.
각 리밸런싱 시점마다 lookback 수익률 상위 N개 마켓만 보유하고, 순위권에서 밀린 마켓은 close 기준으로 회전 청산한다.

## 데이터

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- Upbit: KRW 24h 거래대금 상위 30
- Binance: spot USDT quote volume 상위 30
- 기준봉: 1m, 3m, 5m, 15m 원본 캔들
- 원본 파일: `.backtest_cache` 240개 JSON
- 분할: last-60d 단일 OOS (`BacktestSplitPolicy`)

## 실험

- 전략: `ranked-rotation`
- 진입: lookback 수익률 상위 `rankCount` 마켓을 다음 캔들 open에 진입
- 회전: 리밸런싱 시점에 목표 순위권에서 벗어난 보유 마켓은 close 기준 청산
- 청산: TP +4.0%, SL -2.0%, maxHold 240 candles
- 비용: maker 0.05%, taker 0.05%, slippage 0.05%
- 주문금액: Upbit 10,000 KRW, Binance 10 USDT
- 스윕:
  - timeframe: 1m, 3m, 5m, 15m
  - lookback: 20, 60, 240 candles
  - rankCount: 1, 3
  - rebalance: 20, 60 candles
  - minReturn: 0.5%, 1.5%, 3.0%
  - exchange/timeframe 조합 포함 총 288 rows

## 결과

- 생존 후보: 6
- 판정 분포:
  - `reject:no-train-gross-edge`: 159
  - `reject:weak-train-gross-edge`: 95
  - `reject:oos-collapse`: 18
  - `watch:gross-edge-net-weak`: 8
  - `candidate:weak-paper-observation`: 6
  - `watch:strong-gross-net-weak`: 2
- 생존 후보는 모두 Binance 15m, rankCount=1, rebalance=20 조합이다.
- 대표 후보:
  - params: `lookback=60,rankCount=1,rebalance=20,minReturn=0.5,tp=4.0,sl=-2.0,maxHold=240`
  - full PFgross/PFnet: 1.163 / 1.009
  - train PFgross/PFnet: 1.116 / 0.968
  - test PFgross/PFnet: 1.282 / 1.114
  - full/test trades: 2415 / 694
  - MDD: 1.8%
  - topMarket: `ZECUSDT` 14.7%
- Upbit 최상위 15m 조합도 train PFgross 0.961로 `reject:no-train-gross-edge`다.

## 결론

확장 유니버스에서 처음으로 PAPER 관찰 후보가 나왔다. 다만 후보 강도는 약하다.
대표 후보의 train PFnet은 1.0 미만이고, 수익이 Binance 15m의 top1 로테이션과 `ZECUSDT` 구간에 일부 기대고 있다.

바로 운영 전략으로 채택하지 않고, 다음 단계에서 비용/슬리피지 민감도, `ZECUSDT` 편중, stable/신규상장 제외, 15m top1 파라미터 안정성을 먼저 검증한다.
이 검증을 통과하면 관찰 대시보드 전용 상태에서 별도 PAPER 자동매매 후보로 켠다.

## 하니스 메모

리더보드 재산출 전 `BacktestSeriesLoader`의 exchange 필터를 보정했다.
이전 필터는 파일명 기준 `market.endsWith("USDT")`만 검사해 Upbit `KRW-USDT`가 Binance 유니버스에 섞일 수 있었다.
현재는 Binance 조건을 `!market.startsWith("KRW-") && market.endsWith("USDT")`로 제한하고 회귀 테스트를 추가했다.

## 산출물

- `build/backtest-leaderboard/ranked-rotation.csv`
- `build/backtest-leaderboard/ranked-rotation.md`
