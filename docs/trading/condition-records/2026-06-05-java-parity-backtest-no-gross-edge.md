# 2026-06-05 Java parity 백테스트 — 현행 설정은 gross 엣지 없음 (신호 문제)

## 배경

"수익 모델 확정 → 실거래 전환" 목표의 전제는 **검증 엔진 = 운영 엔진**이다.
기존 `backtest.py`는 운영 코드를 Python으로 재구현해 parity가 깨져 있었다(진입·청산·포지션관리 상이).
운영 Java 엔진(`CandidateScannerService`·`PositionExitSignalService`·`PaperPortfolioService`)을
**수정 없이** 과거 캔들로 리플레이하는 하니스를 신설했다.

- 위치: `src/test/java/com/giseop/comebot/backtest/` (CandleSeries, ReplayCandleProvider, BacktestEngine, BacktestReport)
- 실행: `./gradlew test -Dbacktest.run=true --tests "*ParityBacktestTest"` (기본 suite는 skip; `.backtest_cache` 없으면 assume-skip)
- 데이터: `.backtest_cache` 1m 캔들 10마켓 × 180일 (2025-11-23 ~ 2026-05-22)
- 설정: 현행 .env Upbit (1m×20, minPriceChg 0.15, dist 0–5%, minAmt 1M) + TP+4/SL-2/trailing off (ADR-011)
- 비용: maker 0.05% + taker 0.05% + slippage 0.05%
- 분리: train 120d / test 60d. `PFgross`=비용 전(신호 엣지), `PFnet`=수수료·슬리피지 후(실제)

## 결과 — 진입 FILL 모델만 바꾼 동일 조건 비교

| 진입 모델 | trades | win% | PFgross (ALL/test) | PFnet (ALL/test) | netPnL |
|---|---:|---:|---:|---:|---:|
| **maker-limit @signal-close** (현행 운영, ADR-013) | 1337 | 29.8 | 0.851 / 0.883 | 0.762 / 0.791 | −48,023 |
| **market @next-open** (backtest.py 모델) | 1325 | 31.4 | 0.915 / 0.961 | 0.819 / 0.860 | −35,256 |

- fillRate(maker) 95.7% — backtest.py 기대 96.8%와 근접(진입·체결 모델 충실).
- avgWin +3.844% / avgLoss −2.147% = TP/SL 임계 − 비용. (지표 정합 확인)
- intrabar 낙관(TP 우선)·비관(SL 우선) 결과 동일 → 1분봉이 −2%~+4%(6%폭)를 동시에 찍는 사례 없음.

## 해석

1. **gross PF < 1.0.** 수수료·슬리피지를 0으로 놓아도 현행 진입+TP4/SL2 조합은 진다(test 0.88~0.96).
   → **신호 문제이지 비용 문제가 아니다.** maker 전환(ADR-013) 같은 실행 최적화로는 흑자 전환 불가.
2. **maker 역선택은 부차적.** maker 체결은 가격이 신호 종가로 되돌아온 케이스만 잡아 약한 진입을 선택 →
   gross PF를 ~0.08 깎음(market 0.961 → maker 0.883). 실재하지만 주원인 아님.
3. **backtest.py 수치 신뢰 불가 확인.** backtest.py는 ① 시장가 다음-시가 진입(maker 아님) ② 윈도우가
   현재 캔들 제외(off-by-one 진입) ③ 마켓별 독립(글로벌 포지션 캡·max-buys 없음) ④ `profit_factor`를
   raw(비용 전)로 보고 — 운영과 다름. 저장 결과들도 net은 모두 음수(예: TP2/SL1.5 설정 raw PF 1.204 ↔ net −58,640).
   ADR-011 V2(TP4/SL2)의 문서 "test PF 1.018"은 raw·시장가 기준이며, 하니스의 market gross test 0.961과 근접.

## 결론 / 다음

- **현행 설정은 실거래 전환 대상이 아니다.** 실거래 금지선(ADR-001) 유효.
- 우선순위 재설정: 실행(maker/taker) 튜닝 중단 → **gross PF가 비용을 흡수할 만큼(≈≥1.15) 나오는
  진입+청산 조합을 먼저 탐색**한다. 이 하니스로 PFgross를 1차 게이트, PFnet의 train/test OOS를 채택 게이트로.
- 후속: 하니스에 파라미터 스윕(진입 필터 × TP/SL) 추가, gross 엣지가 존재하는 영역 탐색.
- 참고: 본 하니스는 운영 코드를 그대로 리플레이하므로, 운영 로직 변경 시 별도 동기화 불필요.
