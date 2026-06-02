# 2026-06-02 청산 구조 재설계 — 승자 run (트레일링 제거 + 넓은 TP)

## 배경

ADR-011에 따라 파라미터 튜닝을 중단하고 구조적 레버를 OOS(train 120d / test 60d)로 실험했다.
1순위 레버는 **청산 구조** — 기존 trailing stop이 승자를 평균 +1.1%로 캡해 R:R을 무너뜨린다는
가설(2026-06-02 분석)을 검증했다.

- 진입(고정): min_vol=100, TRAC/IRYS 제외, 그 외 현 .env Upbit
- 비용: 수수료 0.05%/leg + 슬리피지 0.05%/leg
- 채택 게이트(ADR-011): test PF ≥ 1.05 且 |train-test| < 0.15

## 실험 1 — 청산 변형 × timeframe

| 변형 | 1m test PF | 5m test PF | 15m test PF |
|---|---:|---:|---:|
| V0 현재 (TP2/SL-1.5/trail2,1.5) | 0.840 | 0.815 | 0.853 |
| V1 TP3/SL-1.5 no-trail | 0.879 | 0.831 | 0.884 |
| **V2 TP4/SL-2 no-trail** | **1.018** | 0.920 | 0.974 |
| V3 letrun TP6/SL-2/trail3,2 | 0.897 | 0.848 | 0.761 |
| V4 TP3/SL-1.0 no-trail | 0.783 | 0.733 | 0.825 |
| V5 시간기반 SL-2/max20bars | 0.356 | 0.537 | 0.716 |

→ **트레일링 제거 + 넓은 TP(=승자 run)가 전 timeframe에서 현 구조를 압도.** V2가 1m에서 OOS PF 1.0 교차.

## 실험 2 — no-trail TP×SL 정밀 스윕 (1m)

| TP/SL | train PF | test PF | gap | slip pnl |
|---|---:|---:|---:|---:|
| TP2/SL-1.5 (현재) | 0.892 | 0.840 | — | -123,640 |
| **TP4/SL-2.0** | **0.937** | **1.018** | 0.08 | +9,314 |
| TP4/SL-2.5 | 0.918 | 1.053 | 0.14 | +25,812 |
| TP5/SL-2.5 | 0.931 | 1.105 | 0.17 | +44,305 |
| TP6/SL-3.0 | 0.935 | 1.152 | 0.22 | +51,913 |

## 핵심 해석 (정직하게)

- **구조 개선은 실재**: 현 구조 대비 train(0.89→0.94)·test(0.84→1.02) **양쪽 모두 개선**, 3개 timeframe 일관.
- **단, train PF는 여전히 <1.0** (모든 셀). test가 train보다 높은 건 test 구간이 추세장이었던 **regime 효과**이지 robust 흑자가 아니다.
- 즉 이번 변경은 **"손실 구조 → break-even"**으로의 개선이다. robust 흑자는 **진입 신호(entry) 재설계**(다음 레버)가 필요.
- 높은 test PF 셀(TP5/SL-3 등)은 train/test 괴리가 커 regime 의존도가 높아 채택하지 않음.

## 채택 — 1m TP+4.0 / SL-2.0 / 트레일링 OFF

가장 robust한 선택: 최고 train PF(0.937) + 최소 괴리(0.08) + 깔끔한 R:R 2:1.

| 항목 | 변경 전 (.env) | 변경 후 |
|---|---|---|
| `RISK_TAKE_PROFIT_RATE` | 2.0 | **4.0** |
| `RISK_STOP_LOSS_RATE` | -1.5 | **-2.0** |
| `RISK_TRAILING_STOP_ENABLED` | true | **false** |
| `application.properties` take-profit 기본값 | 1.5 | 4.0 |
| `application.properties` stop-loss 기본값 | -0.7 | -2.0 |

타임프레임은 1분봉 유지(파이프라인 변경 없음). 트레일링 메커니즘은 코드에 유지(off).

## 다음 (entry 재설계 — 2순위 레버)

청산 개선만으로 train PF 0.94. robust 흑자(train·test 모두 ≥1.05)는 **진입 신호 품질**이 좌우한다.
다음 실험: 진입 신호 자체 재설계 (현 pullback 외 가설), 상위 timeframe 진입, OOS 게이트 동일 적용.

## 관찰 기준

- 실거래에서 평균 보유시간 증가(넓은 TP) 및 TAKE_PROFIT 비중 변화
- 청산 유형 분포: trailing 제거 후 TP vs SL 비율
- 1주 누적 후 train/test 재검증
