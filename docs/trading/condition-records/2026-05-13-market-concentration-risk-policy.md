# 2026-05-13 Market Concentration Risk Policy

## 목적

JPA PAPER 누적 데이터 기반으로 market별 쏠림 리스크의 초기 문서 기준을 정한다.

이번 작업은 문서 기준 확정과 신규 BUY 차단 구현 범위 확정 단계다.
기본값은 꺼져 있으며, `risk.concentration.enabled=true`일 때만 적용한다.

## 원자료

- 기준 스냅샷: `docs/trading/condition-records/2026-05-13-jpa-paper-data-snapshot.md`
- 누적 history:
  - UPBIT 179,529 rows
  - BINANCE 54,383 rows
- open positions:
  - UPBIT 27개
  - BINANCE 5개

## 기준

### UPBIT

| 기준 | Threshold | 의미 |
| --- | ---: | --- |
| 경고 | 7% 이상 | Dashboard/Portfolio에서 쏠림 경고 대상 |
| 신규 BUY 차단 | 10% 이상 | 해당 market 신규 PAPER BUY 거절 |
| 반복 손절 주의 | 손절 count 상위 10개 | cooldown 또는 후보 제외 검토 |

이유:

- 2026-05-13 기준 `KRW-BLEND`가 9.0104%, `KRW-XPL`이 8.0092%로 이미 7%를 넘었다.
- 10%는 단일 market이 10,000 KRW 단위 반복 BUY를 통해 과도하게 커졌는지 판단하기 쉬운 초기 차단 후보선이다.
- UPBIT은 `ALL_KRW` universe가 넓어 단일 market 10% 초과를 허용할 이유가 크지 않다.

### BINANCE

| 기준 | Threshold | 의미 |
| --- | ---: | --- |
| 경고 | 25% 이상 | Binance symbol 쏠림 경고 대상 |
| 신규 BUY 차단 | 40% 이상 | 해당 symbol 신규 PAPER BUY 거절 |
| 반복 손절 주의 | 손절 count 상위 10개 | cooldown 또는 후보 제외 검토 |

이유:

- 2026-05-13 기준 `UTKUSDT`가 94.1687%로 매우 높다.
- Binance PAPER 초기 현금은 1,000 USDT 기준이고, 현재 누적 상태는 UPBIT과 universe/현금 규모가 다르다.
- 즉시 UPBIT의 7%/10% 기준을 적용하면 기존 Binance 스냅샷 대부분이 차단 상태가 될 수 있어 별도 기준이 필요하다.

## 반복 손절 제한 후보

UPBIT 반복 손절 상위:

| Market | Stop loss count |
| --- | ---: |
| KRW-DEEP | 3,204 |
| KRW-ICP | 2,962 |
| KRW-CHIP | 2,603 |
| KRW-SPK | 2,594 |
| KRW-XPL | 2,473 |
| KRW-CFG | 2,455 |
| KRW-NEAR | 2,435 |
| KRW-ARB | 2,431 |
| KRW-APT | 2,374 |
| KRW-ZBT | 2,284 |

초기 판단:

- `KRW-XPL`, `KRW-ARB`, `KRW-ENA`는 노출 비중과 반복 손절 측면을 함께 봐야 한다.
- 반복 손절 상위 10개 market은 곧바로 영구 제외하지 않는다.
- cooldown 1차 기준은 최근 7일 같은 `exchange + market`에서 FILLED Stop loss SELL 2회 이상으로 둔다.
- cooldown 기본 기간은 마지막 손절 체결 시각부터 24시간이다.

## 구현 전제

- 적용 대상은 PAPER 신규 BUY 주문 제한이다.
- 보유 PAPER SELL, 익절, 손절은 쏠림 기준으로 막지 않는다.
- 실패한 주문을 성공으로 처리하지 않는다.
- 실제 주문 API와 `REAL_TRADING`은 추가하지 않는다.
- Portfolio 쏠림 경고 UI 1차 구현은 완료됐다.
- 반복 손절 cooldown과 Dashboard/Candidate 경고 표시는 `docs/project/CONCENTRATION_WARNING_AND_COOLDOWN_PLAN.md` 기준으로 후속 구현한다.

## 다음 구현 후보

- Portfolio/Dashboard에 경고 market 표시
- Candidate 화면에서 쏠림 차단/반복 손절 cooldown 사유 표시
- 관련 테스트:
  - UPBIT 7% 이상 경고 표시
  - 반복 손절 cooldown 후보 표시
