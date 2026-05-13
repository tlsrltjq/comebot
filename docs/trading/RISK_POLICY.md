# Risk Policy

## 원칙

리스크 정책은 전략보다 우선한다. 전략이 BUY 또는 SELL 신호를 만들더라도 리스크 검증을 통과하지 못하면 주문은 실행하지 않는다.

## Kill Switch

- 기본값은 `safety.kill-switch-enabled=false`다.
- 켜져 있으면 신규 트레이딩 플로우 실행을 차단한다.
- 차단 대상은 REST `/run`, candidate scheduler, exit scheduler, 수동 PAPER 실행이 허용된 Telegram `/run`이다.
- 웹 선택 PAPER SELL도 kill switch가 켜져 있으면 시세 조회 전에 차단한다.
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
- Binance PAPER SELL은 `USDT` 현물 symbol만 허용한다.

실패 시 `REJECTED`로 처리한다.

## PAPER 포트폴리오 검증

- BUY는 PAPER 현금이 충분해야 한다.
- SELL은 보유 수량이 충분해야 한다.
- 웹 선택 PAPER SELL은 사용자가 체크한 보유 포지션에 대해서만 허용한다.
- 웹 선택 PAPER SELL 1차 범위는 선택 market 전량 매도다.
- 웹 선택 PAPER SELL 가격은 프론트가 결정하지 않고 서버가 현재가 provider에서 캡처한다.
- 웹 선택 PAPER SELL 요청에서 중복 market은 한 번만 처리하고, 일부 실패는 market별 결과로 반환한다.
- HOLD는 포트폴리오를 변경하지 않는다.
- REJECTED, FAILED 주문은 포트폴리오를 변경하지 않는다.

## 익절/손절

- 기본값은 `risk.position-exit-enabled=true`다.
- 켜져 있을 때만 보유 포지션 기준 SELL 신호를 만든다.
- 현재 PAPER 자동 실행 기본값은 `risk.take-profit-rate=1.5`, `risk.stop-loss-rate=-0.7`다.
- 미실현 수익률이 `risk.take-profit-rate` 이상이면 익절 SELL 신호를 만든다.
- 미실현 수익률이 `risk.stop-loss-rate` 이하이면 손절 SELL 신호를 만든다.
- SELL 수량은 보유 수량을 초과할 수 없다.
- Stage 9 이후 보유 포지션 익절/손절 평가는 `trading.exit-scheduler`가 전담한다.
- exit scheduler는 보유 position market만 평가하고, HOLD는 기본적으로 history에 저장하지 않는다.
- 신규 BUY 후보 탐색은 candidate scheduler가 담당한다.

## 일일 제한

- 기본값은 `risk.daily-risk-enabled=false`다.
- 켜져 있을 때만 일일 주문 횟수와 일일 실현 손실 한도를 검증한다.
- 오늘 FILLED 주문 수가 `risk.daily-order-limit` 이상이면 신규 주문을 거절한다.
- 오늘 실현 손실이 `risk.daily-loss-limit` 이상이면 신규 주문을 거절한다.
- HOLD, REJECTED, FAILED는 일일 주문 횟수에 포함하지 않는다.

## Market별 쏠림 기준

기본값은 `risk.concentration.enabled=false`다.
켜져 있을 때만 신규 BUY 주문에 market별 쏠림 차단 기준을 적용한다.
기준 산정 원자료는 `docs/trading/condition-records/2026-05-13-jpa-paper-data-snapshot.md`다.

### 노출 비중 계산

- 기본 계산식은 `market cost basis / estimated equity * 100`이다.
- `market cost basis`는 `paper_position.quantity * paper_position.average_buy_price`로 계산한다.
- `estimated equity`는 `paper_portfolio_state.cash + SUM(market cost basis)`로 계산한다.
- 현재가 평가가 가능한 화면에서는 `positionValue / totalEquity * 100`을 보조 지표로 함께 본다.
- exchange별 현금 규모와 market universe가 다르므로 UPBIT과 BINANCE 기준을 분리한다.

### UPBIT 기준

- 경고: 단일 market 추정 비중이 7% 이상이면 쏠림 경고 대상으로 본다.
- 신규 BUY 차단: 단일 market 추정 비중이 10% 이상이면 해당 market 신규 BUY를 거절한다.
- 반복 손절 주의: 최근 7일 같은 market 손절 2회 이상이면 노출 비중과 무관하게 cooldown 검토 대상으로 본다.
- 2026-05-13 스냅샷 기준 `KRW-BLEND`는 9.0104%, `KRW-XPL`은 8.0092%로 경고 대상이다.
- 같은 스냅샷 기준 `KRW-DEEP`, `KRW-ICP`, `KRW-CHIP`, `KRW-SPK`, `KRW-XPL`은 반복 손절 상위권이다.

### BINANCE 기준

- 경고: 단일 symbol 추정 비중이 25% 이상이면 쏠림 경고 대상으로 본다.
- 신규 BUY 차단: 단일 symbol 추정 비중이 40% 이상이면 해당 symbol 신규 BUY를 거절한다.
- 2026-05-13 스냅샷 기준 `UTKUSDT`는 94.1687%로 차단 후보 수준이다.
- Binance는 초기 PAPER 현금과 universe가 UPBIT과 달라 UPBIT의 7%/10% 기준을 그대로 적용하지 않는다.

### 반복 손절 기준

- 기준 기간은 최근 7일 rolling window를 1차 기준으로 둔다.
- 같은 `exchange + market`에서 FILLED SELL history의 `signalReason`에 `Stop loss`가 2회 이상 있으면 cooldown 대상으로 본다.
- cooldown 기본 기간은 마지막 손절 체결 시각부터 24시간이다.
- cooldown은 신규 BUY 후보 또는 신규 BUY 주문에만 적용한다.
- SELL, 익절, 손절, 선택 PAPER SELL은 cooldown으로 막지 않는다.
- 실패 또는 거절된 손절 주문은 cooldown count에 포함하지 않는다.
- rolling window 안 손절 count가 2회 미만으로 내려가거나 마지막 손절 후 24시간이 지나면 cooldown 대상에서 빠진다.
- 최초 구현 기본값은 비활성화로 둔다.

### 구현 범위

- 현재 구현은 신규 BUY 주문 차단만 적용한다.
- 쏠림 기준은 SELL, 익절, 손절 흐름을 막지 않는다.
- 반복 손절 cooldown과 dashboard/portfolio/candidates 경고 표시는 `docs/project/CONCENTRATION_WARNING_AND_COOLDOWN_PLAN.md` 기준으로 별도 구현한다.
- 기준을 추가로 바꾸면 이 문서와 condition record를 함께 갱신한다.

## 시세 기준

- InMemory 시세는 테스트용이다.
- Upbit 시세는 실제 공개 현재가지만 주문은 PAPER_TRADING으로만 처리한다.
- Upbit 시세를 사용해도 Access Key, Secret Key, 실제 주문 API는 사용하지 않는다.
- Binance 시세를 추가하더라도 공개 시세만 사용하며 API Key, Secret Key, 실제 주문 API는 사용하지 않는다.
- WebSocket 시세가 실패하면 REST fallback 또는 최근 snapshot을 사용하되, 현재가가 불명확한 주문은 성공으로 처리하지 않는다.
- 실제 시세 기반 결과가 수익을 보장하지 않는다.

## 상태 조회

```http
GET /api/risk/status
```

응답에는 maxOrderAmount, allowedMarkets, 익절/손절, 일일 제한 설정이 포함된다.
다음 구현에서는 concentration threshold와 반복 손절 cooldown 요약을 추가한다.
