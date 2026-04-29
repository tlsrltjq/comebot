# Development Rules

## Notification 규칙

- 알림 발송은 `NotificationSender` 인터페이스를 통해 처리한다.
- `LoggingNotificationSender`는 테스트용 구현체다.
- `TelegramNotificationSender`는 설정이 활성화되고 Telegram 설정이 완료된 경우에만 Telegram API를 호출한다.
- Bot Token, Chat ID, API Key는 코드에 하드코딩하지 않는다.
- Scheduler나 Controller에 알림 메시지 생성 로직을 넣지 않는다.
- 알림 실패가 주문 상태나 트레이딩 결과를 변경하면 안 된다.
- 알림 실패가 history 저장 결과를 변경하면 안 된다.
- 알림은 history 저장 이후에만 호출한다.

## Telegram Inbound 규칙

- Telegram inbound polling은 기본 비활성이다.
- Webhook은 사용하지 않고 로컬 개발 기준 `getUpdates` polling만 사용한다.
- `telegram.enabled=true`, `telegram.inbound.enabled=true`, configured 상태일 때만 polling을 실행한다.
- Inbound 명령과 callback은 configured `telegram.chat-id`와 일치하는 요청만 처리한다.
- Bot Token, Chat ID 원문을 로그나 응답에 노출하지 않는다.
- Telegram 명령 처리 실패가 애플리케이션 전체를 중단시키면 안 된다.
- `/run` 명령은 기존 `PAPER_TRADING` 트레이딩 플로우만 실행한다.
- 인라인 버튼 callback 처리도 기존 `PAPER_TRADING` 트레이딩 플로우만 사용한다.
- Telegram 포트폴리오 조회 명령과 callback은 조회 전용이며 포트폴리오 상태를 변경하면 안 된다.
- Telegram update offset 저장 방식을 변경하면 성공/실패 offset 처리 테스트를 함께 수정한다.
- Telegram offset 저장소를 변경하면 polling 테스트와 저장소 테스트를 함께 수정한다.
- JPA offset 저장소 사용 시 `ddl-auto=none`을 유지하고 `schema.sql`을 적용한다.

## History 저장소 규칙

- 기본 history 저장소는 `IN_MEMORY`다.
- InMemory history는 애플리케이션 재시작 시 사라진다.
- `history.storage-type=JPA` 설정 시 PostgreSQL/JPA 저장소를 사용할 수 있다.
- JPA 사용 시 `spring.jpa.hibernate.ddl-auto=none`을 유지한다.
- JPA 사용 전 `src/main/resources/schema.sql`을 DB에 적용해야 한다.
- History Controller에는 조회 검증, service 호출, 응답 변환 외의 비즈니스 로직을 넣지 않는다.

## 테스트용 가격 API 규칙

- `InMemoryMarketPriceProvider`는 실제 거래소 API가 아니다.
- 가격 변경 REST API는 테스트용 시세를 바꾸는 엔드포인트다.
- 가격 변경 REST API를 운영용 실제 가격 변경 API처럼 구현하거나 설명하지 않는다.
- Controller에는 가격 변경 검증, provider 호출, 응답 변환 외의 비즈니스 로직을 넣지 않는다.

## 시세 Provider 규칙

- 기본 시세 provider는 `IN_MEMORY`다.
- Upbit provider는 공개 Ticker API만 사용한다.
- Upbit 공개 Ticker API 사용은 허용한다.
- Upbit Access Key, Secret Key는 추가하지 않는다.
- 인증키 기반 계정 API는 추가하지 않는다.
- 실제 거래소 주문 API는 추가하지 않는다.
- 실제 시세를 사용하더라도 주문 실행은 `PAPER_TRADING` 흐름만 사용한다.

## Position Exit 규칙

- 손절/익절 정책은 `PAPER_TRADING` SELL 신호만 생성한다.
- 손절/익절 정책은 실제 주문 API 또는 `REAL_TRADING` 경로를 만들면 안 된다.
- 기본값은 `risk.position-exit-enabled=false`다.
- 기존 전략이 HOLD인 경우에만 position exit 정책을 평가한다.
- SELL 수량은 보유 포지션 수량을 초과하면 안 된다.
- 손절/익절 기준이나 우선순위를 변경하면 테스트와 `docs/RISK_POLICY.md`, `docs/ORDER_LIFECYCLE.md`를 함께 수정한다.

## Daily Risk 규칙

- 일일 리스크 제한은 주문 실행 전에 검증해야 한다.
- 일일 리스크 제한 실패는 `REJECTED`로 처리하고 실행 게이트웨이를 호출하면 안 된다.
- HOLD, REJECTED, FAILED 결과를 일일 주문 횟수 제한에 포함하면 안 된다.
- 일일 실현 손실 계산은 페이퍼 포트폴리오의 실현손익 이벤트를 기준으로 한다.
- 일일 리스크 기준을 변경하면 테스트와 `docs/RISK_POLICY.md`, `docs/ORDER_LIFECYCLE.md`를 함께 수정한다.

## Portfolio 규칙

- 포트폴리오 변경은 `PAPER_TRADING` 주문이 `FILLED` 된 이후에만 수행한다.
- HOLD, REJECTED, FAILED 결과로 포트폴리오를 변경하지 않는다.
- 현금 부족 BUY와 보유 수량 부족 SELL은 `REJECTED`로 처리한다.
- 기본 포트폴리오 저장소는 `IN_MEMORY`다.
- JPA 포트폴리오 저장소는 별도 단계 전까지 구현하지 않는다.
- 포트폴리오 valuation은 조회 전용이어야 하며 포트폴리오 상태를 변경하면 안 된다.
- Telegram 포트폴리오 조회도 조회 전용이어야 하며 포트폴리오 상태를 변경하면 안 된다.
- valuation은 기존 `MarketPriceProvider`를 통해 현재가를 조회한다.

## 작업 원칙

- 기능 추가 전에 관련 정책 문서를 확인한다.
- 코드 변경은 테스트와 함께 진행한다.
- 매수/매도 판단 로직 변경 시 테스트를 추가하거나 수정한다.
- 설정값과 민감 정보는 코드에 하드코딩하지 않는다.
- Controller는 요청 검증, 서비스 호출, 응답 변환만 담당한다.
- Controller에 전략 판단, 주문 생성, 리스크 검증, 주문 실행 로직을 넣지 않는다.

## Java 기준

- 프로젝트 기준 Java 버전은 21이다.
- Gradle toolchain 설정과 테스트 실행 Java 버전이 일치해야 한다.

## 테스트 기준

- 전략 판단은 단위 테스트로 검증한다.
- 리스크 정책은 성공과 실패 케이스를 모두 테스트한다.
- 주문 상태 전이는 실패 케이스를 포함해 테스트한다.
- 실제 API 연결 전에는 provider 인터페이스를 통해 테스트 흐름을 먼저 검증한다.
- REST 엔드포인트는 Controller 테스트로 상태 코드, 응답 필드, 서비스 위임을 검증한다.
- 텔레그램 버튼 흐름 변경 시 문서와 테스트를 함께 수정한다.

## 구현 제한

- 초기 버전은 `PAPER_TRADING`만 구현한다.
- 인증이 필요한 실제 시세 API는 별도 승인 전까지 구현하지 않는다.
- 실제 주문 API는 별도 승인 전까지 구현하지 않는다.
- `REAL_TRADING` 활성화는 명시적 설정과 별도 검증 없이는 허용하지 않는다.

## 변경 시 문서 동기화

- 텔레그램 흐름 변경: `docs/TELEGRAM_UX.md`
- 주문 상태 변경: `docs/ORDER_LIFECYCLE.md`
- 리스크 정책 변경: `docs/RISK_POLICY.md`
- 구조 변경: `docs/ARCHITECTURE.md`
