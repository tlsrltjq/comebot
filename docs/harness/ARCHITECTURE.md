# Architecture

## 목표 구조

`comebot`은 실제 시세를 관찰하고, 변동성과 추세를 기준으로 롱 전용 매수 후보를 판단한 뒤, `PAPER_TRADING`으로 주문과 포트폴리오 결과를 검증하는 시스템이다.

실제 주문 API와 `REAL_TRADING`은 포함하지 않는다.

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

## 핵심 모듈

- `market`: 현재가와 캔들 데이터 제공
- `strategy`: BUY, SELL, HOLD 신호 판단
- `strategy.indicator`: 캔들 기반 변동성 지표 계산
- `strategy.candidate`: 롱 후보 스캔
- `strategy.controller`: 후보 조회 API
- `strategy.candidate.CandidateExecutionService`: 선택 후보 PAPER 주문 실행
- `safety`: kill switch 차단
- `risk`: 주문 실행 전 리스크 검증
- `execution`: PAPER_TRADING 주문 실행
- `portfolio`: PAPER 현금, 포지션, 손익 관리
- `history`: 트레이딩 플로우 결과 저장
- `notification`: 선택적 알림 발송
- `telegram`: 명령과 버튼 기반 운영 보조
- Telegram 후보 명령: `CandidateScannerService`, `CandidateExecutionService`를 호출해 PAPER 흐름만 실행

## Market

- `InMemoryMarketPriceProvider`: 테스트용 가격 provider
- `UpbitMarketPriceProvider`: Upbit 공개 Ticker API provider
- `UpbitCandleProvider`: Upbit 공개 Candle API provider
- Access Key, Secret Key는 사용하지 않는다.

## Strategy

기본 전략은 테스트용 `SimpleThresholdStrategy`다.

`STRATEGY_SELECTED=VOLATILITY_BREAKOUT_LONG`이면 후보 스캔 기반 롱 전용 PAPER 전략을 사용한다.

## Safety와 Risk

kill switch는 시세 조회와 전략 판단보다 먼저 확인한다.

Risk는 허용 market, 주문 금액, 수량, 가격, 일일 제한, PAPER 현금과 보유 수량을 검증한다.

## Execution과 Portfolio

현재 실행 Gateway는 PAPER_TRADING 전용이다.

PAPER 체결 결과만 포트폴리오에 반영한다.

## History와 Notification

HOLD, REJECTED, FILLED, FAILED 결과를 모두 저장한다.

알림은 history 저장 이후 선택적으로 실행한다.

## 작업 관리

완료 기록과 다음 작업 계획은 `docs/project/PROJECT_PLAN.md`에서 시작한다.
