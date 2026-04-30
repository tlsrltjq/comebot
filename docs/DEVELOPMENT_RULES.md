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
- 리스크 검증
- 주문 실행
- 포트폴리오 반영
- history 저장
- Telegram 처리

Controller와 Scheduler는 비즈니스 로직을 직접 갖지 않는다.

## 전략 변경 규칙

전략 변경 시 `docs/STRATEGY_POLICY.md`를 먼저 확인한다.

테스트 필수 항목:

- BUY 생성
- SELL 생성
- HOLD 유지
- 익절/손절 조건
- 리스크 실패 시 미실행
- 포트폴리오 상태 변경 여부

## 시세 Provider 규칙

- InMemory provider는 테스트용이다.
- Upbit provider는 공개 Ticker API만 사용한다.
- Upbit candle provider는 공개 Candle API만 사용한다.
- Upbit Access Key, Secret Key는 추가하지 않는다.
- 실제 주문 API는 추가하지 않는다.
- 실제 시세를 사용해도 주문은 PAPER_TRADING으로만 처리한다.

## Risk 규칙

- Risk는 주문 실행 전 검증해야 한다.
- kill switch는 risk보다 상위 안전장치다.
- 일일 리스크는 주문 실행 전 검증해야 한다.
- 포트폴리오 현금 부족과 보유 수량 부족은 주문 전 차단해야 한다.

## Portfolio 규칙

- 포트폴리오 변경은 PAPER_TRADING FILLED 이후에만 가능하다.
- HOLD, REJECTED, FAILED는 포트폴리오를 변경하지 않는다.
- Valuation은 조회 기능이며 상태를 변경하면 안 된다.

## History 규칙

- 기본 저장소는 `IN_MEMORY`이다.
- `JPA`는 선택 가능하다.
- JPA 사용 시 `ddl-auto=none`을 유지한다.
- JPA 사용 전 `schema.sql`을 적용한다.
- 현재 history 응답 형식을 깨지 않는다.

## Telegram 규칙

- Telegram inbound는 기본 비활성이다.
- `telegram.enabled=true`, `telegram.inbound.enabled=true`, configured 상태일 때만 polling한다.
- configured chatId와 일치하는 요청만 처리한다.
- callback 처리 후 가능한 경우 `answerCallbackQuery`를 호출한다.
- Telegram offset 변경 시 polling 테스트를 수정한다.
- Telegram 버튼 흐름 변경 시 `docs/TELEGRAM_UX.md`를 수정한다.

## Scheduler 규칙

- Scheduler는 기본 비활성이다.
- Scheduler는 `TradingFlowService`만 호출한다.
- Scheduler 안에 전략, 주문, 리스크 로직을 넣지 않는다.

## 작업 계획 규칙

- 완료된 작업과 다음 작업은 `docs/PROJECT_PLAN.md`에 기록한다.
- 새 기능을 시작하기 전에 해당 단계의 목표, 범위, 금지 사항을 확인한다.
- 단계가 완료되면 완료 기록에 날짜와 검증 결과를 남긴다.

## 테스트 규칙

아래 변경은 테스트를 추가하거나 수정한다.

- 전략 판단
- 리스크 정책
- 주문 상태 흐름
- 포트폴리오 반영
- history 저장
- Telegram 명령/버튼
- scheduler 실행
- offset 저장소

## 보안 린트 규칙

`SecurityLintTest`는 `gradlew test`에서 실행된다.

검사 항목:

- `.env`가 git에 추적되지 않는지
- 민감 설정이 환경 변수 placeholder를 사용하는지
- 민감 설정에 non-empty 기본값이 없는지
- Telegram Bot Token 형식의 문자열이 추적 파일에 없는지
- private key, access key 형태의 문자열이 없는지
- 로그 문장에 token, chatId, password, secret, accessKey를 직접 출력하지 않는지
- Upbit 인증키 설정이 추가되지 않았는지
- `REAL_TRADING` 구현체가 추가되지 않았는지

보안 린트 실패 시 커밋하지 않는다.

## 문서 변경 규칙

- 전략 변경: `STRATEGY_POLICY.md`
- 리스크 변경: `RISK_POLICY.md`
- 주문 흐름 변경: `ORDER_LIFECYCLE.md`
- Telegram 변경: `TELEGRAM_UX.md`
- 장애 처리 변경: `RELIABILITY.md`
- 구조 변경: `ARCHITECTURE.md`
- 계획 변경: `PROJECT_PLAN.md`
