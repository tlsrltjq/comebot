# Project Next Steps

이 문서는 현재 작업만 관리한다.
완료 이력은 `CHANGELOG.md`와 Git history를 기준으로 본다.
현재 세션 컨텍스트(목표·완료 기준·멈춘 곳)는 `tasks/current.md`에 있다.

---

## 다음 우선순위: 후보 선정 수치 기록 설계

목표:

- BUY 시점에 어떤 전략 판단 수치를 남겨야 나중에 분석 가능한지 확정한다.
- 데이터 축적 없이 문서와 DTO/도메인 경계만 먼저 정리한다.

작업:

- 후보 선정 시점에 필요한 수치 목록 정리 (변동성, 가격 변화율, 거래대금, trend, risk reject/hold reason)
- 저장 위치 설계: `paper_trade_log` 확장 vs 별도 `scan_log` 테이블 비교
- 바로 구현할 항목과 데이터 축적 후 구현할 항목 분리

완료 기준:

- 실제 주문 API, `REAL_TRADING`, 수동 BUY UI가 추가되지 않는다.
- DB migration은 설계가 확정되기 전까지 보류한다.
- `CLAUDE_FEEDBACK_ROADMAP.md`와 같은 방향을 가리킨다.

---

## 보류

- 실제 주문 API와 `REAL_TRADING`
- 수동 BUY
- trailing stop / 시간 기반 청산 (먼저 2~3개월 PAPER 데이터 필요)
- 전략 plugin 구조
