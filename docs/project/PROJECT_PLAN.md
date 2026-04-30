# Project Plan

작업 기록과 다음 작업은 분리해서 관리한다.

## 문서

- `docs/project/PROJECT_HISTORY.md`: 완료된 작업 기록
- `docs/project/PROJECT_NEXT_STEPS.md`: 앞으로 진행할 작업
- `docs/trading/STRATEGY_POLICY.md`: 전략 방향
- `docs/harness/DEVELOPMENT_RULES.md`: 작업 규칙

## 현재 목표

실제 Upbit 공개 시세를 사용해서 변동성 기반 롱 전용 PAPER_TRADING 전략을 검증한다.

실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.

## 현재 단계

캔들 데이터 Provider, 변동성 계산 서비스, 롱 후보 스캐너는 추가됐다.

다음 단계는 후보 조회 REST API다.

## 작업 진행 규칙

- 작업 시작 전 `AGENTS.md`를 확인한다.
- 해당 단계의 목표와 금지 사항을 확인한다.
- 코드 변경 시 테스트를 실행한다.
- 테스트 실패 시 커밋하지 않는다.
- 완료 후 `docs/project/PROJECT_HISTORY.md`를 갱신한다.
- 다음 단계가 바뀌면 `docs/project/PROJECT_NEXT_STEPS.md`를 갱신한다.
