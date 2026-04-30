# Development Rules

## 기본 원칙

- Java 21을 기준으로 개발한다.
- 기본 거래 모드는 `PAPER_TRADING`이다.
- 실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.
- 민감 정보는 코드, 로그, 응답에 노출하지 않는다.
- 기능 변경 시 관련 문서를 함께 수정한다.
- 테스트 성공 전에는 커밋하지 않는다.

## 모듈 경계

한 클래스에 아래 책임을 함께 넣지 않는다.

- 시세 조회
- 전략 판단
- 전략 선택 Bean 구성
- 리스크 검증
- 주문 실행
- 포트폴리오 반영
- history 저장
- Telegram 처리

Controller와 Scheduler는 비즈니스 로직을 직접 갖지 않는다.

## 변경별 문서

- 전략 변경: `docs/trading/STRATEGY_POLICY.md`
- 리스크 변경: `docs/trading/RISK_POLICY.md`
- 주문 흐름 변경: `docs/trading/ORDER_LIFECYCLE.md`
- Telegram 변경: `docs/operations/TELEGRAM_UX.md`
- 장애 처리 변경: `docs/operations/RELIABILITY.md`
- 구조 변경: `docs/harness/ARCHITECTURE.md`
- 계획 변경: `docs/project/PROJECT_PLAN.md`
- 보안 변경: `docs/harness/SECURITY_RULES.md`

## 테스트 필수 변경

- 전략 판단
- 리스크 정책
- 주문 상태 흐름
- 포트폴리오 반영
- history 저장
- Telegram 명령/버튼
- scheduler 실행
- offset 저장소
- 보안 린트

## 작업 계획

- 완료된 작업은 `docs/project/PROJECT_HISTORY.md`에 기록한다.
- 다음 작업은 `docs/project/PROJECT_NEXT_STEPS.md`에 기록한다.
- 새 기능 시작 전 `AGENTS.md`와 해당 계획 문서를 확인한다.

## 세부 규칙

- 보안: `docs/harness/SECURITY_RULES.md`
- 운영: `docs/operations/OPERATIONS.md`
- Telegram: `docs/operations/TELEGRAM_UX.md`
- 전략: `docs/trading/STRATEGY_POLICY.md`
- 리스크: `docs/trading/RISK_POLICY.md`
