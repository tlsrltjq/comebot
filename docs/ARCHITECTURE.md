# Architecture

## 목표

`comebot`은 코인 시세를 수집하고, 테스트용 전략 조건을 평가한 뒤, `PAPER_TRADING` 기준으로 주문 결과와 텔레그램 알림을 생성한다.

## 기본 흐름

1. Market Data가 `MarketPrice`를 만든다.
2. Strategy가 `MarketPrice`를 평가해 `TradingSignal`을 만든다.
3. `OrderRequestFactory`가 BUY 또는 SELL 신호만 `OrderRequest`로 변환한다.
4. Risk가 `OrderRequest`를 검증한다.
5. Execution이 승인된 주문 요청만 페이퍼 트레이딩으로 처리한다.

## 기본 모듈 경계

- Market Data: 거래소 또는 외부 공급자에서 시세를 조회한다.
- Strategy: 테스트용 기준으로 BUY, SELL, HOLD 신호를 만든다.
- Risk: 주문 요청이 정책을 통과하는지 검증한다.
- Execution: 주문 실행을 추상화하고 페이퍼 트레이딩 결과를 생성한다.
- Telegram: 사용자 명령, 버튼, 알림 메시지를 처리한다.
- Configuration: 실행 모드, 전략 기준값, 거래 제한 설정을 관리한다.

## 계층 구성

- `market.domain`: 시세 스냅샷을 정의한다.
- `strategy.domain`: 전략 신호와 신호 타입을 정의한다.
- `strategy.service`: 테스트용 전략 평가와 주문 요청 변환을 담당한다.
- `execution.domain`: 실행 모드, 주문 방향, 주문 상태, 주문 요청, 주문 결과를 정의한다.
- `execution.gateway`: 주문 실행 인터페이스와 페이퍼 트레이딩 구현체를 둔다.
- `execution.service`: 주문 요청을 리스크 검증 후 실행 게이트웨이에 위임한다.
- `risk.domain`: 리스크 판단 결과와 승인/거절 상태를 정의한다.
- `risk.service`: 주문 요청의 입력값, 주문 금액, 허용 마켓을 검증한다.
- `config`: 거래 모드, 최대 주문 금액, 허용 마켓, 전략 기준값을 관리한다.

## 주문 실행 흐름

- HOLD 신호는 주문 요청으로 변환하지 않는다.
- BUY 또는 SELL 신호만 주문 요청으로 변환한다.
- 전략 신호가 있어도 리스크 검증을 통과해야 실행된다.
- 리스크 검증 결과가 `REJECTED`이면 게이트웨이를 호출하지 않고 `OrderResult.REJECTED`를 반환한다.

## 금지 구조

- 시세 조회, 전략 판단, 주문 실행, 텔레그램 처리를 하나의 클래스에 합치지 않는다.
- 실제 주문 API 호출 코드를 초기 버전에 넣지 않는다.
- 실제 시세 조회 API 호출 코드를 초기 버전에 넣지 않는다.
- API Key, Secret Key, Access Token 관련 코드를 만들지 않는다.
- 설정값 없이 `REAL_TRADING`으로 전환되는 경로를 만들지 않는다.

## 초기 실행 모드

초기 버전은 `PAPER_TRADING`만 사용한다. `REAL_TRADING` 설정값이 존재하더라도 실제 거래소 주문 실행 로직은 만들지 않는다.
