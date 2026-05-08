# Order Lifecycle

## 기본 흐름

```text
RUN_REQUESTED
-> KILL_SWITCH_CHECKED
-> MARKET_PRICE_CAPTURED
-> SIGNAL_DETECTED
-> ORDER_REQUEST_CREATED
-> RISK_CHECKED
-> PAPER_ORDER_EXECUTED
-> PORTFOLIO_UPDATED
-> HISTORY_SAVED
-> OPTIONAL_NOTIFICATION_SENT
```

## 웹 선택 PAPER SELL 흐름

```text
SELL_SELECTION_REQUESTED
-> KILL_SWITCH_CHECKED
-> POSITION_OWNERSHIP_CHECKED
-> MARKET_PRICE_CAPTURED
-> SELL_ORDER_REQUEST_CREATED
-> RISK_CHECKED
-> PAPER_ORDER_EXECUTED
-> PORTFOLIO_UPDATED
-> HISTORY_SAVED
-> OPTIONAL_NOTIFICATION_SENT
```

이 흐름은 사용자가 웹 포트폴리오에서 명시 선택한 보유 PAPER 포지션 매도에만 사용한다. 수동 BUY, 실제 주문 API, `REAL_TRADING`은 포함하지 않는다.

## Kill Switch 차단

kill switch가 켜져 있으면 시세 조회 전에 차단한다.

- `orderCreated=false`
- `orderStatus=REJECTED`
- `message=Kill switch enabled: trading flow blocked`
- history 저장 가능
- portfolio 변경 없음

## SIGNAL_DETECTED

- BUY: 주문 요청 생성 가능
- SELL: 보유 포지션 청산 요청 가능
- HOLD: 주문 요청 생성 금지

현재 `SimpleThresholdStrategy`는 테스트용이다. 앞으로 변동성 추적 전략으로 교체한다.

후보 실행 API는 `SELECTED` 후보만 BUY 신호로 변환한다.

## ORDER_REQUEST_CREATED

BUY 또는 SELL 신호만 `OrderRequest`로 변환한다.

HOLD는 주문 실행으로 이어지면 안 된다.

웹 선택 PAPER SELL은 전략 신호가 아니라 사용자 선택 이벤트에서 시작하지만, 서버에서 보유 포지션과 현재가를 다시 확인한 뒤 SELL `OrderRequest`를 만든다. 프론트가 수량, 가격, 체결 성공 여부를 결정하면 안 된다.

## RISK_CHECKED

리스크 검증 실패 시:

- `OrderStatus=REJECTED`
- ExecutionGateway 호출 금지
- 실패한 주문을 성공으로 처리 금지

## PAPER_ORDER_EXECUTED

- 정상 주문은 `FILLED`
- 잘못된 요청은 `FAILED` 또는 `REJECTED`
- 실제 거래소 주문 API 호출 금지

## PORTFOLIO_UPDATED

포트폴리오 반영은 PAPER_TRADING 체결 이후에만 수행한다.

- BUY FILLED: 현금 차감, 수량 증가, 평균 매수가 갱신
- SELL FILLED: 수량 감소, 실현손익 계산
- 웹 선택 PAPER SELL FILLED: 선택한 보유 포지션 수량만 감소, 실현손익 계산
- HOLD, REJECTED, FAILED: 변경 없음
- 기존 자동매매 흐름은 `UPBIT` PAPER 포트폴리오에 반영한다.
- exchange-aware PAPER 흐름은 선택한 exchange의 cash, position, realized profit만 변경한다.

## HISTORY_SAVED

모든 결과를 history에 저장한다.

- 기존 history 저장 wrapper는 `UPBIT`으로 저장한다.
- exchange-aware 저장은 `TradingFlowHistory.exchange`에 선택한 exchange를 보존한다.

## OPTIONAL_NOTIFICATION_SENT

알림은 history 저장 이후에만 실행한다.

알림 실패는 TradingFlowResult, 주문 상태, history, portfolio를 변경하면 안 된다.
