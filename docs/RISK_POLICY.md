# Risk Policy

## 기본 원칙

- 기본 거래 모드는 `PAPER_TRADING`이다.
- 실제 자금이 이동하는 주문은 초기 버전에서 금지한다.
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
