# Project Next Steps

이 문서는 현재 작업만 관리한다.
완료 이력은 `CHANGELOG.md`와 Git history를 기준으로 본다.
현재 세션 컨텍스트(목표·완료 기준·멈춘 곳)는 `tasks/current.md`에 있다.

---

## 현재 상태: 데이터 축적 대기

2026-05-20 기준으로 진입 품질 필터와 scan log가 모두 구현되었다.
지금은 충분한 PAPER 운용 데이터가 쌓일 때까지 전략 조건을 유지하고 관찰한다.

다음 기술 작업은 아래 보류 항목에서 데이터가 충분히 쌓인 후 선택한다.

---

## 보류 (데이터 축적 필요)

- trailing stop / 시간 기반 청산 (2–3개월 PAPER 이력 필요)
- 진입 필터 수치 조정 (scan log 통계로 filter-out rate 분석 후 결정)
- 전략 plugin 구조 확장

## 영구 보류

- 실제 주문 API와 `REAL_TRADING`
- 수동 BUY
