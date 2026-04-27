# Risk Policy

## 결과 이력 원칙

- HOLD, REJECTED, FILLED 결과를 모두 이력으로 남긴다.
- 거절된 주문도 사유를 포함해 이력으로 남긴다.
- 이력 저장은 주문 성공 처리가 아니며 상태를 변경하지 않는다.

## 테스트 가격 원칙

- 테스트 가격 변경 API로 설정한 가격은 실제 시장 가격이 아니다.
- 테스트 가격은 전략 판단과 리스크 검증 흐름을 검증하는 용도로만 사용한다.
- 테스트 가격으로 생성된 주문도 `PAPER_TRADING` 흐름만 사용한다.

## 실제 시세 사용 원칙

- Upbit provider를 사용하면 공개 Ticker API에서 실제 시세를 조회할 수 있다.
- 실제 시세를 사용해도 주문 실행은 `PAPER_TRADING`만 사용한다.
- 실제 거래소 주문 API와 인증키 기반 주문 실행은 금지한다.

## 기본 원칙

- 기본 거래 모드는 `PAPER_TRADING`이다.
- 실제 자금이 이동하는 주문은 초기 버전에서 금지한다.
- 테스트용 시세 공급자는 실제 거래소 데이터가 아니다.
- 수동 실행 REST 엔드포인트도 `PAPER_TRADING` 흐름만 사용한다.
- 전략 신호가 발생해도 리스크 검증을 통과해야 주문 실행으로 이어진다.
- 리스크 검증을 통과하지 못한 주문 요청은 실행 게이트웨이로 전달하지 않는다.
- 실패한 주문 또는 거절된 주문을 성공으로 처리하지 않는다.

## 초기 검증 조건

- 주문 요청이 null이면 `REJECTED`로 처리한다.
- `market`이 비어 있으면 `REJECTED`로 처리한다.
- `side`가 null이면 `REJECTED`로 처리한다.
- `quantity`가 null 또는 0 이하이면 `REJECTED`로 처리한다.
- `price`가 null 또는 0 이하이면 `REJECTED`로 처리한다.
- 주문 금액(`quantity * price`)이 `trading.max-order-amount`를 초과하면 `REJECTED`로 처리한다.
- `market`이 `trading.allowed-markets`에 없으면 `REJECTED`로 처리한다.

## 기본 설정

- `trading.mode=PAPER_TRADING`
- `trading.max-order-amount=100000`
- `trading.allowed-markets=KRW-BTC,KRW-ETH`

## 실패 처리

- 리스크 검증 실패는 명확한 사유를 남긴다.
- 리스크 검증 실패 시 실제 실행 게이트웨이를 호출하지 않는다.
- 예외 발생 시 알림 또는 로그로 확인 가능해야 한다.

## 변경 규칙

리스크 정책을 변경하면 이 문서와 관련 테스트를 함께 수정한다.
