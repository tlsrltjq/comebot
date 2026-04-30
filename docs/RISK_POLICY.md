# Risk Policy

## 원칙

리스크 정책은 전략보다 우선한다. 전략이 BUY 또는 SELL 신호를 만들더라도 리스크 검증을 통과하지 못하면 주문은 실행하지 않는다.

## Kill Switch

- 기본값은 `safety.kill-switch-enabled=false`다.
- 켜져 있으면 신규 트레이딩 플로우 실행을 차단한다.
- 차단 대상은 REST `/run`, scheduler 실행, Telegram `/run`, RUN 버튼이다.
- history, status, portfolio 조회는 차단하지 않는다.
- kill switch는 시세 조회와 전략 판단보다 먼저 확인한다.

## 주문 요청 검증

- 요청이 null이 아님
- market이 비어 있지 않음
- side가 null이 아님
- quantity가 0보다 큼
- price가 0보다 큼
- 주문 금액이 `trading.max-order-amount` 이하
- market이 `trading.allowed-markets`에 포함됨

실패 시 `REJECTED`로 처리한다.

## PAPER 포트폴리오 검증

- BUY는 PAPER 현금이 충분해야 한다.
- SELL은 보유 수량이 충분해야 한다.
- HOLD는 포트폴리오를 변경하지 않는다.
- REJECTED, FAILED 주문은 포트폴리오를 변경하지 않는다.

## 익절/손절

- 기본값은 `risk.position-exit-enabled=false`다.
- 켜져 있을 때만 보유 포지션 기준 SELL 신호를 만든다.
- 미실현 수익률이 `risk.take-profit-rate` 이상이면 익절 SELL 신호를 만든다.
- 미실현 수익률이 `risk.stop-loss-rate` 이하이면 손절 SELL 신호를 만든다.
- SELL 수량은 보유 수량을 초과할 수 없다.

## 일일 제한

- 기본값은 `risk.daily-risk-enabled=false`다.
- 켜져 있을 때만 일일 주문 횟수와 일일 실현 손실 한도를 검증한다.
- 오늘 FILLED 주문 수가 `risk.daily-order-limit` 이상이면 신규 주문을 거절한다.
- 오늘 실현 손실이 `risk.daily-loss-limit` 이상이면 신규 주문을 거절한다.
- HOLD, REJECTED, FAILED는 일일 주문 횟수에 포함하지 않는다.

## 시세 기준

- InMemory 시세는 테스트용이다.
- Upbit 시세는 실제 공개 현재가지만 주문은 PAPER_TRADING으로만 처리한다.
- Upbit 시세를 사용해도 Access Key, Secret Key, 실제 주문 API는 사용하지 않는다.
- 실제 시세 기반 결과가 수익을 보장하지 않는다.

## 상태 조회

```http
GET /api/risk/status
```

응답에는 maxOrderAmount, allowedMarkets, 익절/손절, 일일 제한 설정이 포함된다.
