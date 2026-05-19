# Project Plan

이 문서는 프로젝트 문서 인덱스다.
다음 작업은 `docs/project/PROJECT_NEXT_STEPS.md`를 우선한다.
최근 완료 맥락은 `docs/project/PROJECT_HISTORY.md`와 Git history를 기준으로 본다.

## 현재 목표

Upbit/Binance 공개 시세를 사용해 롱 전용 `PAPER_TRADING` 전략과 운영 UI를 검증한다.
실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.

## 세션 핵심 파일 (매 세션 시작 시 읽음)

- `HARNESS.md`: 프로젝트 전체 요약 진입점
- `tasks/current.md`: 현재 세션 작업 컨텍스트
- `CHANGELOG.md`: 변경 이력 한 줄 누적
- `GC_ROUTINE.md`: 가비지 컬렉션 루틴

## Active Documents

- `docs/decisions.md`: 기술 결정 ADR
- `docs/harness/HARNESS_STATUS.md`: 현재 기준과 다음 작업 요약
- `docs/harness/ARCHITECTURE.md`: 시스템 구조와 모듈 경계
- `docs/harness/DEVELOPMENT_RULES.md`: 작업 규칙
- `docs/harness/SECURITY_RULES.md`: 보안/린트 규칙
- `docs/trading/STRATEGY_POLICY.md`: 전략 정책
- `docs/trading/RISK_POLICY.md`: 리스크 정책
- `docs/trading/ORDER_LIFECYCLE.md`: 주문 상태 흐름
- `docs/operations/OPERATIONS.md`: 실행/운영 절차
- `docs/operations/RELIABILITY.md`: 장애 처리와 복구
- `docs/operations/TELEGRAM_UX.md`: Telegram UX
- `docs/operations/INCIDENT_LOG.md`: 운영 중 장애와 이상 사례 기록
- `docs/trading/condition-records/`: PAPER 운용 기록
- `docs/project/CLAUDE_FEEDBACK_ROADMAP.md`: 클로드 피드백 기반 다음 작업 로드맵

## Historical Plans

완료된 계획 문서는 Git history와 `CHANGELOG.md`로 대체됐다.
현재 구현 지침은 코드, 테스트, active documents를 기준으로 한다.
