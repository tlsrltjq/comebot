# 2026-05-19 손실 분석 및 개선

## 손실 원인 분석

### 핵심 데이터 (2026-05-13 JPA 스냅샷 기준)

| Exchange | 보유 포지션 | 실현손익 |
| --- | ---: | ---: |
| UPBIT | 27 | -1,151.67 KRW |
| BINANCE | 5 | +8.92 USDT |

반복 손절 상위 (UPBIT):

| Market | 손절 횟수 |
| --- | ---: |
| KRW-DEEP | 3,204 |
| KRW-ICP | 2,962 |
| KRW-CHIP | 2,603 |
| KRW-SPK | 2,594 |
| KRW-XPL | 2,473 |

### 근본 원인

1. **손절 재진입 무한 루프 (CRITICAL)**: `stop-loss-cooldown.enabled=false` 기본값으로 인해 손절 후 즉시 동일 market에 재진입. 10,000 KRW × -0.7% = 70 KRW 손실이 3,204번 반복되면 KRW-DEEP 한 종목에서만 약 224,000 KRW 손실.

2. **입력 필터 너무 약함**: `minPriceChangeRate=0.3%`는 5분 1분봉에서 노이즈 수준. 시장 정상 변동으로도 BUY 신호 발생.

3. **마지막 캔들 방향 미확인**: 5분 전체 추세가 UP이어도 마지막 캔들이 하락 중(bearish)에 진입 가능. FOMO 고점 매수 패턴.

4. **손절 슬리피지**: 30초 → 5초 exit scheduler로 개선됐으나 여전히 -0.7% 트리거 후 실제 체결은 더 깊음 (2026-05-06 기록: 평균 -1.4%).

## 적용한 개선

### 설정 변경 (application.properties 기본값)

| 항목 | 변경 전 | 변경 후 |
| --- | --- | --- |
| `risk.stop-loss-cooldown.enabled` | false | **true** |
| `risk.stop-loss-cooldown.window` | 7d | **1d** |
| `risk.stop-loss-cooldown.duration` | 24h | **6h** |
| `strategy.candidate-scanner.min-price-change-rate` | 0.3 | **1.0** |
| `strategy.candidate-scanner.min-trade-amount-change-rate` | 20 | **30** |

### 코드 변경

- `VolatilitySnapshot`에 `lastCandleBullish` 필드 추가
- `VolatilityIndicatorService`: 마지막 캔들 `close > open` 여부 계산
- `CandidateScannerService.toCandidate()`: 마지막 캔들이 bearish면 `SKIPPED` 반환

### 기대 효과

- stop-loss cooldown: 같은 market에서 1일 2회 손절 발생 시 6시간 차단 → 반복 손절 스파이럴 차단
- minPriceChangeRate 1.0%: 0.3% 대비 신호 수 약 70% 감소 예상, 진입 품질 향상
- lastCandleBullish 필터: 상승 기간 중 하락 전환 시점 매수 방지

## 다음 관찰 기준

- 손절 반복 market이 cooldown으로 실제로 차단되는지 확인
- 익절/손절 비율: 현재 4건 전부 손절, 목표는 최소 손익비 1:1
- BUY 신호 감소량: 일 41건 → 10~15건 예상
- 동일 기간 후 새 스냅샷과 비교
