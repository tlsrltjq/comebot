# 백테스트 리더보드 정책

## 목적

새 전략 후보는 동일한 컬럼으로 비교한다. 전략별 콘솔 출력만 보고 판단하지 않고,
CSV/Markdown 리더보드에 남긴 뒤 PFgross, PFnet, OOS, MDD, 거래 수, 코인 편중을 함께 본다.

## 기본 산출물

- CSV: `build/backtest-leaderboard/leaderboard.csv`
- Markdown: `build/backtest-leaderboard/leaderboard.md`
- `build/` 산출물은 git에 커밋하지 않는다.

## 공통 컬럼

- 전략 메타: `strategy`, `exchange`, `timeframe`, `marketSet`, `entryModel`, `params`
- 샘플: `sampleStartUtc`, `sampleEndUtc`
- 진입 퍼널: `signals`, `fills`, `expiries`, `fillRatePct`
- 전체 성과: `fullTrades`, `fullPFgross`, `fullPFnet`, `fullWinRatePct`, `fullNetPnl`, `fullMddPct`, `fullAvgHoldMin`
- OOS: `trainTrades`, `trainPFgross`, `trainPFnet`, `trainMddPct`, `testTrades`, `testPFgross`, `testPFnet`, `testMddPct`
- 안정성: `monthlyPnl`, `topMarket`, `topMarketTradePct`, `regimeSplit`
- 판단: `decision`

## 판단 기준

- 분할: 시간순 단일 OOS. `train = sample start..last 60d`, `test = last 60d`.
- 표본 폐기: `fullTrades < 30` 또는 `testTrades < 10`.
- 즉시 폐기: `trainPFgross < 1.00`.
- 약한 엣지 폐기: `trainPFgross < 1.10`.
- 편중 폐기: `topMarketTradePct > 60%`.
- OOS 붕괴 폐기: `testPFgross < 1.00` 또는 `trainPFgross - testPFgross > 0.25`.
- 관찰 후보: gross edge는 있으나 `testPFnet < 1.00`.
- PAPER 후보: `testPFnet >= 1.00`이고 위 탈락 조건을 모두 통과.

## 다음 구현 메모

현재 구조는 `BacktestLeaderboardRow`와 `BacktestLeaderboardWriter`만 제공한다.
각 전략 실험 테스트는 실행 결과인 `BacktestEngine.Result`를 row로 변환해 위 경로에 저장하면 된다.

## 현재 산출

- `relative-strength-momentum.csv/md`: BTC/ETH 시드 기준 72개 조합 모두 탈락.
  상세 결과는 `docs/trading/condition-records/2026-06-12-relative-strength-momentum.md`를 본다.
- `volume-surge-continuation.csv/md`: BTC/ETH 시드 기준 216개 조합 모두 탈락.
  상세 결과는 `docs/trading/condition-records/2026-06-12-volume-surge-continuation.md`를 본다.
- `volatility-contraction-breakout.csv/md`: BTC/ETH 시드 기준 216개 조합 모두 탈락.
  상세 결과는 `docs/trading/condition-records/2026-06-12-volatility-contraction-breakout.md`를 본다.
- `oversold-mean-reversion.csv/md`: BTC/ETH 시드 기준 216개 조합 모두 탈락.
  상세 결과는 `docs/trading/condition-records/2026-06-12-oversold-mean-reversion.md`를 본다.
