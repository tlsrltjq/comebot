# Architecture

## Notification

- `notification` 계층은 Trading Flow 실행 결과를 알림 메시지로 변환한다.
- `LoggingNotificationSender`는 테스트용 알림 구현체다.
- `TelegramNotificationSender`는 설정이 활성화되고 Telegram 설정이 완료된 경우 실제 Telegram sendMessage를 호출한다.
- Trading Flow 결과는 history 저장 이후에만 선택적으로 알림 처리한다.
- `notification.enabled=false`가 기본값이다.
- 알림 실패가 주문 상태나 트레이딩 결과를 변경하면 안 된다.
- Scheduler와 Controller에는 알림 메시지 생성 로직을 넣지 않는다.

## 실행 이력

- Trading Flow 실행 결과는 `history` 계층에 저장한다.
- 기본 저장소는 `InMemoryTradingFlowHistoryRepository`다.
- `history.storage-type=JPA` 설정 시 PostgreSQL/JPA 저장소를 사용할 수 있다.
- JPA 저장소 사용 시 `spring.jpa.hibernate.ddl-auto=none`을 유지하고 `schema.sql`을 먼저 적용한다.
- InMemory 저장소는 애플리케이션 재시작 시 이력이 사라진다.
- `GET /api/trading-flow/history`는 최근 실행 이력을 조회한다.
- `GET /api/trading-flow/history/{id}`는 단건 실행 이력을 조회한다.
- History Controller는 조회 요청 검증, service 호출, 응답 DTO 변환만 담당한다.

## 테스트용 가격 변경 API

- `GET /api/market-prices/{market}`는 `InMemoryMarketPriceProvider`에 저장된 테스트 가격을 반환한다.
- `PUT /api/market-prices/{market}`는 요청 본문의 `price`로 테스트 가격을 변경한다.
- 가격 변경 API는 실제 거래소 가격 수정 API가 아니다.
- 가격 변경 후 `GET /api/trading-flow/run?market=...`으로 BUY, SELL, HOLD 흐름을 수동 검증할 수 있다.
- MarketPriceController는 요청 검증, provider 호출, 응답 DTO 변환만 담당한다.

## 시세 Provider 선택

- `market.price-provider=IN_MEMORY`가 기본값이다.
- `IN_MEMORY`는 테스트용 메모리 가격을 사용한다.
- `UPBIT`은 Upbit 공개 Ticker API에서 현재가를 조회한다.
- Upbit provider는 인증키를 사용하지 않는다.
- Upbit provider는 실제 주문 API를 호출하지 않는다.
- 어떤 provider를 사용해도 주문 실행은 기존 `PAPER_TRADING` 흐름만 사용한다.
- `GET /api/market-provider/status`는 현재 provider 설정을 조회한다.
- Provider 상태 조회 API는 설정 변경 기능을 제공하지 않는다.

## 전략 설정 상태 조회

- `GET /api/strategy/status`는 현재 `SimpleThresholdStrategy` 설정값을 조회한다.
- 응답에는 전략 이름, 매수 기준가, 매도 기준가, 주문 수량만 포함한다.
- Strategy 상태 조회 API는 설정 변경 기능을 제공하지 않는다.
- Strategy Controller에는 전략 판단이나 주문 실행 로직을 넣지 않는다.

## 리스크 정책 상태 조회

- `GET /api/risk/status`는 현재 리스크 정책 설정값을 조회한다.
- 응답에는 최대 주문 금액, 허용 market 목록, 손절/익절 설정을 포함한다.
- Risk 상태 조회 API는 설정 변경 기능을 제공하지 않는다.
- Risk Controller에는 리스크 검증이나 주문 실행 로직을 넣지 않는다.

## Position Exit Policy

- `risk.position-exit-enabled=false`가 기본값이다.
- 활성화된 경우 기존 전략이 HOLD일 때 보유 포지션의 미실현 수익률을 평가한다.
- 수익률이 `risk.take-profit-rate` 이상이면 익절 SELL 신호를 만든다.
- 수익률이 `risk.stop-loss-rate` 이하이면 손절 SELL 신호를 만든다.
- Position exit 정책은 실제 주문 API가 아니라 기존 `PAPER_TRADING` SELL 흐름만 사용한다.
- SELL 수량은 보유 수량을 초과하지 않는다.

## PAPER_TRADING 포트폴리오

- `portfolio` 계층은 페이퍼 체결 결과를 기반으로 현금, 보유 수량, 평균 매수가, 실현 손익을 관리한다.
- 기본 저장소는 `paper.portfolio-storage-type=IN_MEMORY`다.
- JPA 포트폴리오 저장소는 아직 구현하지 않는다.
- 포트폴리오 변경은 `PAPER_TRADING` 주문이 `FILLED` 된 이후에만 수행한다.
- `GET /api/portfolio/status`는 현금과 실현 손익을 조회한다.
- `GET /api/portfolio/positions`는 현재 보유 포지션 목록을 조회한다.
- `GET /api/portfolio/valuation`은 현재 Market Provider 가격으로 평가금액과 미실현 손익을 계산한다.
- 포트폴리오 평가는 조회 전용이며 portfolio 상태를 변경하지 않는다.
- Telegram `/portfolio`와 `PORTFOLIO` callback은 포트폴리오 평가 요약을 조회한다.
- Telegram `/positions`와 `POSITIONS` callback은 보유 포지션 목록을 조회한다.
- Telegram 포트폴리오 조회는 portfolio 상태를 변경하지 않는다.

## 시스템 상태 조회

- `GET /api/system/status`는 주요 설정 상태를 한 번에 조회한다.
- 응답은 database, market provider, strategy, risk, scheduler, notification, telegram 섹션으로 구성한다.
- DB 연결 실패는 전체 API 실패로 처리하지 않고 `database.connected=false`로 응답한다.
- Telegram은 enabled/configured/inboundEnabled만 노출한다.
- System Status API는 설정 변경 기능을 제공하지 않는다.

## 목표

`comebot`은 코인 시세를 수집하고, 테스트용 전략 조건을 평가한 뒤, `PAPER_TRADING` 기준으로 주문 결과와 텔레그램 알림을 생성한다.

## 기본 흐름

1. Market Provider가 `MarketPrice`를 제공한다.
2. Strategy가 `MarketPrice`를 평가해 `TradingSignal`을 만든다.
3. 기존 전략이 HOLD이고 position exit 설정이 활성화된 경우 보유 포지션 손절/익절 조건을 평가한다.
4. `OrderRequestFactory`가 BUY 또는 SELL 신호만 `OrderRequest`로 변환한다.
5. Risk가 `OrderRequest`를 검증한다.
6. Execution이 승인된 주문 요청만 페이퍼 트레이딩으로 처리한다.
7. `FILLED` 결과만 Paper Portfolio에 반영한다.
8. Trading Flow 결과를 history 저장소에 저장한다.
9. 알림 설정과 필터 정책을 통과하면 Notification 계층이 알림을 보낸다.
10. 수동 REST 엔드포인트와 Telegram 명령은 이 흐름을 호출한다.

## 기본 모듈 경계

- Market Provider: InMemory 테스트 시세 또는 Upbit 공개 시세를 제공한다.
- Strategy: 테스트용 기준으로 BUY, SELL, HOLD 신호를 만든다.
- Risk: 주문 요청이 정책을 통과하는지 검증하고, 선택적으로 position exit SELL 신호 정책을 제공한다.
- Execution: 주문 실행을 추상화하고 페이퍼 트레이딩 결과를 생성한다.
- Portfolio: 페이퍼 체결 결과로 현금, 보유 수량, 실현 손익을 관리한다.
- Trading Flow: 시세, 전략, 주문 요청 생성, 리스크 검증, 페이퍼 실행을 한 번에 연결한다.
- REST Controller: 수동 실행 요청을 받고 Trading Flow 결과를 응답 DTO로 변환한다.
- Telegram: 사용자 명령, 버튼, 포트폴리오 조회, 알림 메시지를 처리한다.
- Configuration: 실행 모드, 전략 기준값, 거래 제한 설정을 관리한다.

## 계층 구성

- `market.domain`: 시세 스냅샷을 정의한다.
- `market.provider`: 시세 공급자 인터페이스와 InMemory/Upbit 구현체를 둔다.
- `strategy.domain`: 전략 신호와 신호 타입을 정의한다.
- `strategy.service`: 테스트용 전략 평가와 주문 요청 변환을 담당한다.
- `trading.service`: market provider → strategy → order request → risk → execution 흐름을 조합한다.
- `trading.controller`: `GET /api/trading-flow/run?market=KRW-BTC` 요청을 처리한다.
- `trading.dto`: 수동 실행 API 응답 DTO를 정의한다.
- `execution.domain`: 실행 모드, 주문 방향, 주문 상태, 주문 요청, 주문 결과를 정의한다.
- `execution.gateway`: 주문 실행 인터페이스와 페이퍼 트레이딩 구현체를 둔다.
- `execution.service`: 주문 요청을 리스크 검증 후 실행 게이트웨이에 위임한다.
- `risk.domain`: 리스크 판단 결과와 승인/거절 상태를 정의한다.
- `risk.service`: 주문 요청의 입력값, 주문 금액, 허용 마켓을 검증하고 position exit 신호를 평가한다.
- `portfolio`: PAPER_TRADING 포트폴리오 상태와 저장소, 조회 API를 제공한다.
- `config`: 거래 모드, 최대 주문 금액, 허용 마켓, 전략 기준값을 관리한다.

## 주문 실행 흐름

- REST 컨트롤러는 `TradingFlowService`를 호출하고 결과를 DTO로 변환한다.
- REST 컨트롤러에는 전략 판단, 주문 생성, 리스크 검증, 주문 실행 로직을 넣지 않는다.
- HOLD 신호는 주문 요청으로 변환하지 않는다.
- BUY 또는 SELL 신호만 주문 요청으로 변환한다.
- 전략 신호가 있어도 리스크 검증을 통과해야 실행된다.
- 리스크 검증 결과가 `REJECTED`이면 게이트웨이를 호출하지 않고 `OrderResult.REJECTED`를 반환한다.

## 금지 구조

- 시세 조회, 전략 판단, 주문 실행, 텔레그램 처리를 하나의 클래스에 합치지 않는다.
- 실제 주문 API 호출 코드를 초기 버전에 넣지 않는다.
- 인증이 필요한 실제 시세 API 또는 주문 API 호출 코드를 초기 버전에 넣지 않는다.
- API Key, Secret Key, Access Token 관련 코드를 만들지 않는다.
- 설정값 없이 `REAL_TRADING`으로 전환되는 경로를 만들지 않는다.

## 초기 실행 모드

초기 버전은 `PAPER_TRADING`만 사용한다. `REAL_TRADING` 설정값이 존재하더라도 실제 거래소 주문 실행 로직은 만들지 않는다.
