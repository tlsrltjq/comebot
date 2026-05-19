# 현재 작업 컨텍스트

## 지금 단계: 후보 선정 수치 기록 설계

## 목표
- [ ] BUY 시점에 남겨야 할 전략 판단 수치 목록 확정
      (변동성, 가격 변화율, 거래대금 변화율, trend, risk reject/hold reason)
- [ ] 저장 위치 설계: `paper_trade_log` 확장 vs 별도 `scan_log` 테이블 비교
- [ ] 데이터 축적 전 바로 구현할 항목과 축적 후 구현할 항목 분리
- [ ] `CLAUDE_FEEDBACK_ROADMAP.md`와 방향 일치 확인

## 완료 기준
- 실제 DB migration 없이 문서와 DTO/도메인 경계만 정리됨
- `paper_trade_log` 확장 또는 별도 scan log 중 설계 방향 하나가 확정됨
- 실제 주문 API, `REAL_TRADING`, 수동 BUY UI가 추가되지 않음
- 설계 결과를 `docs/trading/condition-records/` 또는 `docs/project/CLAUDE_FEEDBACK_ROADMAP.md`에 기록

## 건드리면 안 되는 파일
- `src/main/resources/schema.sql`: DB migration 확정 전 변경 금지
- `frontend/src/shared/api/types.ts`: API 계약 변경 전 수정 금지
- `REAL_TRADING` 관련 코드: 추가 금지

## 이전 세션에서 멈춘 곳
하네스 재구성 완료 (HARNESS.md, CHANGELOG.md, docs/decisions.md, tasks/current.md, GC_ROUTINE.md 신규 생성, AGENTS.md 업데이트).
다음 세션에서 후보 선정 수치 기록 설계를 시작한다.
참고: `docs/project/PROJECT_NEXT_STEPS.md` "다음 우선순위: 후보 선정 수치 기록 설계" 섹션.

## 다음 단계 예고
설계 확정 후 → `paper_trade_log` 확장 또는 신규 테이블 schema.sql 작성 및 마이그레이션
