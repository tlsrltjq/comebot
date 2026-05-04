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
- 재진입 제한 조건
- 리스크 검증
- 주문 실행
- 포트폴리오 반영
- history 저장
- Telegram 처리
- Web 화면 표시와 사용자 입력 처리

Controller와 Scheduler는 비즈니스 로직을 직접 갖지 않는다.
React 컴포넌트도 비즈니스 판단, 리스크 검증, 주문 상태 전이를 직접 구현하지 않는다.

## 변경별 문서

- 전략 변경: `docs/trading/STRATEGY_POLICY.md`
- 매매 조건 변경과 운용 기록: `docs/trading/condition-records/`
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
- Web 거래 실행 UX
- Scheduler 실행 주기와 중복 실행 방지

## Web 규칙

- React 앱은 `frontend/`에 둔다.
- 웹은 기존 REST API를 호출하고, 서버 도메인 로직을 중복 구현하지 않는다.
- 웹은 모니터링 전용이며 거래 실행 버튼을 추가하지 않는다.
- 웹에서 실제 주문 API, `REAL_TRADING`, Upbit 인증 설정을 추가하지 않는다.
- API 응답 타입이 바뀌면 frontend 타입과 관련 화면 테스트를 함께 수정한다.
- 프론트 변경 시 `frontend`에서 `npm run lint`, `npm run build`, `npm test`를 실행한다.

## Web 금지사항

- API client 외부에서 `fetch`를 직접 호출하지 않는다.
- React 화면에서 후보 실행 API나 trading-flow 실행 API를 호출하지 않는다.
- `localStorage`와 `sessionStorage`에 거래, 알림, 토큰, 상태 값을 저장하지 않는다.
- 프론트 코드에 `REAL_TRADING` 문자열을 추가하지 않는다.
- 프론트 코드에 access key, secret, token, password, chat id 성격의 식별자를 추가하지 않는다.
- 실패한 실행 결과를 성공처럼 표시하지 않는다.

## Telegram 규칙

- Telegram 기본 동작은 조회 전용이다.
- `/run`, `/candidate-run`, 실행 callback은 `TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=true`일 때만 PAPER 실행을 호출한다.
- 기본 inline menu에는 실행 버튼을 노출하지 않는다.
- Telegram 메시지는 현재 자동 실행 조건, 손익, 포트폴리오를 확인할 수 있어야 한다.
- Telegram은 실제 주문 API, `REAL_TRADING`, 거래소 인증 설정을 추가하지 않는다.

## Scheduler 금지사항

- `@Scheduled`에 `fixedRate`를 사용하지 않는다.
- 외부 API polling scheduler는 이전 호출이 끝나기 전 다음 호출이 겹치지 않도록 중복 실행 방지 장치를 둔다.
- 외부 API 실패로 애플리케이션이 종료되게 예외를 밖으로 흘리지 않는다.

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
