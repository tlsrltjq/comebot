# Project Plan

작업 기록과 다음 작업은 분리해서 관리한다.

## 문서

- `docs/project/PROJECT_HISTORY.md`: 완료 작업 기록
- `docs/project/PROJECT_NEXT_STEPS.md`: 다음 구현 단계
- `docs/project/WEB_UX_UPGRADE_PLAN.md`: 웹 UX/UI 개선 계획
- `docs/trading/STRATEGY_POLICY.md`: 전략 방향
- `docs/trading/condition-records/`: 매매 조건과 PAPER 운용 기록
- `docs/harness/DEVELOPMENT_RULES.md`: 작업 규칙
- `docs/harness/HARNESS_STATUS.md`: 현재 하네스 상태와 다음 작업 요약

## 현재 목표

Upbit 공개 시세와 캔들 데이터를 사용해 변동성 기반 롱 전용 PAPER_TRADING 전략을 검증한다.

실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.

## 현재 단계

캔들 Provider, 변동성 계산, 후보 실행, Telegram 후보 명령, 전략 선택, 후보 자동 실행, 과열 회피, 재진입 제한, market별 override, scheduler 요약 알림, Telegram 후보 요약 개선, scheduler/history 검증, React 모니터링 웹 UI, 기본 자동 PAPER 실행 설정, 매매 조건 기록 문서, analytics API, 대시보드 손익 요약, 포트폴리오 자산 배분/손익 UX, History 분석 화면까지 추가했다.

다음 단계는 중복 진입 제한 강화다. History에서 확인한 손실 원인과 반복 HOLD 사유를 근거로 재진입 cooldown을 설계한다.

## 작업 진행 규칙

- 작업 시작 시 `AGENTS.md`를 확인한다.
- 해당 단계의 목표와 금지 사항을 확인한다.
- 코드 변경 시 테스트를 실행한다.
- 테스트 실패 시 커밋하지 않는다.
- 완료 후 `docs/project/PROJECT_HISTORY.md`를 갱신한다.
- 다음 단계가 바뀌면 `docs/project/PROJECT_NEXT_STEPS.md`를 갱신한다.
