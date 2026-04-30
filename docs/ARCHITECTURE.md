# Architecture

## 목표 구조

`comebot`은 실제 시세를 관찰하고, 변동성과 추세를 기준으로 롱 전용 매수 후보를 판단한 뒤, `PAPER_TRADING`으로 주문과 포트폴리오 결과를 검증하는 시스템이다.

실제 주문 API와 `REAL_TRADING`은 현재 구조에 포함하지 않는다.

## 전체 흐름

```text
market
-> strategy
-> safety
-> risk
-> execution
-> portfolio
-> history
-> notification
-> telegram
```

실행 전에는 kill switch를 가장 먼저 확인한다.

## Market

- `InMemoryMarketPriceProvider`: 테스트용 가격 provider
- `UpbitMarketPriceProvider`: Upbit 공개 Ticker API 현재가 provider
- `UpbitCandleProvider`: Upbit 공개 Candle API 최근 분봉 provider
- Access Key, Secret Key는 사용하지 않는다.
- 실제 주문 API는 호출하지 않는다.

앞으로 추가할 구조:

- 거래대금 provider
- 변동성 계산 서비스
- market scanner

## Strategy

- 현재 `SimpleThresholdStrategy`는 테스트용이다.
- 목표 전략은 롱 전용 변동성 추적 전략이다.
- BUY 후보는 변동성, 추세, 거래대금, 리스크 조건을 분리해서 판단한다.
- SELL 후보는 보유 포지션의 익절/손절 기준으로 만든다.
- HOLD는 주문 실행으로 이어지면 안 된다.

세부 원칙은 `docs/STRATEGY_POLICY.md`를 따른다.

## Safety

- kill switch는 시세 조회와 전략 판단보다 먼저 확인한다.
- 켜져 있으면 신규 트레이딩 플로우를 차단한다.
- 상태 조회, history 조회, portfolio 조회는 차단하지 않는다.

## Risk

- 주문 실행 전 요청을 검증한다.
- 허용 market, 주문 금액, 수량, 가격, 일일 제한을 검증한다.
- PAPER 현금 부족과 보유 수량 부족은 주문 전 차단한다.
- 실패 시 `REJECTED`로 처리한다.

## Execution

- 현재 실행 Gateway는 PAPER_TRADING 전용이다.
- 실제 거래소 주문 Gateway는 만들지 않는다.
- 실패한 주문을 성공으로 처리하지 않는다.

## Portfolio

- PAPER_TRADING 체결 결과만 반영한다.
- BUY FILLED: 현금 차감, 수량 증가, 평균 매수가 갱신
- SELL FILLED: 수량 감소, 실현 손익 계산
- HOLD, REJECTED, FAILED: 변경 없음
- Valuation은 조회 기능이며 상태를 변경하지 않는다.

## History

- HOLD, REJECTED, FILLED, FAILED 결과를 모두 저장한다.
- 기본 저장소는 `IN_MEMORY`이다.
- `JPA` 저장소는 선택 가능하며 `schema.sql` 적용이 필요하다.

## Notification

- 알림은 history 저장 이후 선택적으로 실행한다.
- 알림 실패는 주문 상태, history, portfolio 결과를 변경하지 않는다.
- 기본값은 비활성이다.

## Telegram

- getUpdates polling 방식이다.
- 기본값은 비활성이다.
- configured chatId와 일치하는 요청만 처리한다.
- `/run`과 RUN 버튼은 기존 `TradingFlowService` 경로만 사용한다.

## Scheduler

- 기본값은 비활성이다.
- 켜져 있을 때만 설정된 market 목록을 주기적으로 실행한다.
- Scheduler는 `TradingFlowService`만 호출한다.

## 작업 관리

완료 기록과 다음 작업 계획은 `docs/PROJECT_PLAN.md`에서 관리한다.
