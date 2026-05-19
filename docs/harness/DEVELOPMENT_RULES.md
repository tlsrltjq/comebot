# Development Rules

세션 자동화 규칙은 `AGENTS.md`에 있다. 이 문서는 기술 구현 규칙만 다룬다.

## 기본 원칙

- Java 21을 기준으로 개발한다.
- 기본 거래 모드는 `PAPER_TRADING`이다.
- 실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.
- execution gateway는 PAPER 전용임을 코드에서 명시하고, 런타임에서 차단한다.
- 사용자가 웹에서 명시 선택한 보유 PAPER 포지션 SELL만 거래 실행 UX 예외로 허용한다.
- 수동 BUY는 계속 금지한다.
- 민감 정보는 코드, 로그, 응답에 노출하지 않는다.
- 테스트 성공 전에는 커밋하지 않는다.

## 모듈 경계

한 클래스에 아래 책임을 함께 넣지 않는다.

- 시세 조회 / 전략 판단 / 전략 선택 Bean 구성 / 재진입 제한 조건
- 리스크 검증 / 주문 실행 / 포트폴리오 반영 / history 저장
- Telegram 처리 / Web 화면 표시와 사용자 입력 처리

Controller와 Scheduler는 비즈니스 로직을 직접 갖지 않는다.
React 컴포넌트도 비즈니스 판단, 리스크 검증, 주문 상태 전이를 직접 구현하지 않는다.

## 문서 갱신 기준

변경 범위별 필수 갱신 문서는 `AGENTS.md`의 "문서 갱신 규칙" 표를 따른다.

추가로 아래 변경은 해당 문서를 반드시 갱신한다.

- 매매 조건 변경: `docs/trading/condition-records/`에 기록 추가
- 기술 결정 변경: `docs/decisions.md`에 ADR 추가 또는 수정

아래 변경은 문서 갱신을 생략할 수 있다.

- 일반 UI 레이아웃/문구 개선
- 내부 refactor
- 테스트 보강
- 완료 기록만 늘어나는 변경

## 테스트 필수 변경

- 전략 판단 / 리스크 정책 / 주문 상태 흐름
- 포트폴리오 반영 / history 저장
- Telegram 명령/버튼 / scheduler 실행 / offset 저장소
- 보안 린트 / Web 거래 실행 UX / Web 모니터링·analytics UX
- Scheduler 실행 주기와 중복 실행 방지
- WebSocket parser, reconnect, stale snapshot, REST fallback

## Web 규칙

- React 앱은 `frontend/`에 둔다.
- 웹은 기존 REST API를 호출하고, 서버 도메인 로직을 중복 구현하지 않는다.
- 예외적으로 선택 보유 포지션 PAPER SELL 버튼만 허용한다.
  허용 endpoint: `POST /api/portfolio/positions/sell-selected` (market 목록만 전송)
- 자동매매 켜기/끄기와 candidate scheduler 주기 변경은 `/api/scheduler/control`만 사용한다.
- API 응답 타입이 바뀌면 frontend 타입과 관련 화면 테스트를 함께 수정한다.
- 프론트 변경 시: `npm run lint`, `npm run build`, `npm test`
- 화면 구조·반응형·위험 액션 UX 변경 시: `npm run test:e2e` 추가
- 새 환경에서 E2E 첫 실행 전: `cd frontend && npx playwright install chromium`

## Web 금지사항

- API client 외부에서 `fetch` 직접 호출
- `localStorage` / `sessionStorage`에 거래·알림·토큰·상태 값 저장
- `REAL_TRADING` 문자열, real order endpoint, 인증키 식별자 추가
- 후보 실행 API, trading-flow 실행 API, 수동 BUY API 호출
- 실패 결과를 성공처럼 표시
- React에서 손익·손절/익절을 임의 규칙으로 재계산 (서버 analytics API 사용)

## Scheduler 금지사항

- `@Scheduled`에 `fixedRate` 사용 금지 → `fixedDelay` 사용
- 외부 API polling scheduler: 이전 호출이 끝나기 전 다음 호출이 겹치지 않도록 중복 실행 방지
- 외부 API 실패로 애플리케이션이 종료되게 예외를 밖으로 흘리지 않음

## Telegram 규칙

- Telegram 기본 동작은 조회 전용이다.
- `/run`, `/candidate-run`, 실행 callback은 수동 PAPER 실행 서비스를 호출하지 않는다.
- 기본 inline menu에는 실행 버튼을 노출하지 않는다.
- 실제 주문 API, `REAL_TRADING`, 거래소 인증 설정을 추가하지 않는다.

## 작업 흐름

- 완료 기록: `CHANGELOG.md` 한 줄 추가 + Git history (source of truth)
- 현재 작업: `tasks/current.md`
- 다음 작업 변경 시만: `docs/project/PROJECT_NEXT_STEPS.md` 갱신
- 새 기능 시작 전: `AGENTS.md` → `HARNESS.md` → `tasks/current.md` 확인
