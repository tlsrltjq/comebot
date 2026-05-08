# Architecture

## 목표 구조

`comebot`은 실제 시세를 관찰하고, 변동성과 추세를 기준으로 롱 전용 매수 후보를 자동 판단한 뒤, `PAPER_TRADING`으로 주문과 포트폴리오 결과를 검증하는 시스템이다. 실제 주문 API와 `REAL_TRADING`은 포함하지 않는다.

## 전체 흐름

```text
web
-> market
-> analytics
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
- `exchange`: 거래소 모드와 거래소별 market symbol 경계. 초기 계획 값은 `UPBIT`, `BINANCE`
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
- `analytics`: history와 PAPER 포트폴리오 기반 운영 집계 API
- `notification`: 선택적 알림 발송
- `telegram`: 명령과 버튼 기반 운영 보조
- `web`: React 기반 모니터링 화면. 기존 REST API를 호출해 상태, 후보, 포트폴리오, history, analytics를 표시한다.
- Telegram 후보 명령: `CandidateScannerService`, `CandidateExecutionService`를 호출해 PAPER 흐름만 실행
- candidate scheduler: 설정된 market 후보를 PAPER 실행하고 선택 설정 시 요약 알림 발송

## 현재 운영 범위

- 기본 거래 모드: `PAPER_TRADING`
- 실제 주문: 구현하지 않음
- 웹 역할: 모니터링 전용
- Telegram 기본 역할: 조회 전용
- 자동 실행 대상: `ALL_KRW`
- 자동 실행 주기: 30초
- 1회 PAPER 주문 금액: 10,000 KRW
- 익절/손절 기준: 1.5 / -0.7
- 같은 market 추가 진입: 기본 허용
- 계획 중인 예외: 웹 포트폴리오에서 사용자가 명시 선택한 보유 PAPER 포지션 SELL

## Market

- `InMemoryMarketPriceProvider`: 테스트용 가격 provider
- `UpbitMarketPriceProvider`: Upbit 공개 Ticker API provider
- `UpbitCandleProvider`: Upbit 공개 Candle API provider
- `BinanceMarketPriceProvider`: Binance 공개 Spot Ticker API provider. `USDT` 현물 symbol만 지원한다.
- `BinanceCandleProvider`: Binance 공개 Spot Kline API provider. `USDT` 현물 symbol과 지원 interval만 사용한다.
- Access Key, Secret Key는 사용하지 않는다.

거래소 모드는 `UPBIT`과 `BINANCE`를 분리한다. 프론트 사이드바 상단의 mode switch가 선택한 거래소를 API 요청 파라미터로 전달하고, 백엔드는 같은 화면 구조에 필요한 데이터를 거래소별 provider, portfolio, history에서 조회한다. 기본값은 `UPBIT`이다. Stage 5 기준 portfolio와 history는 exchange별로 분리되며, Binance PAPER 포트폴리오는 `USDT` 기준 통화로 표시한다.

WebSocket 시세 수신은 REST provider와 분리한다.

- Upbit WebSocket client와 Binance WebSocket client를 별도 구현한다.
- 수신한 ticker는 거래소별 snapshot store에 저장한다.
- `MarketPriceProviderType.SNAPSHOT` 설정 시 현재가 조회는 fresh WebSocket snapshot을 우선 사용한다.
- snapshot이 없거나 주문용 stale 기준을 넘으면 REST fallback을 사용한다.
- fallback 성공 가격은 `REST_FALLBACK` source로 snapshot store에 저장한다.
- WebSocket과 REST가 모두 실패하면 주문 실행은 차단하고 조회 화면에는 실패 또는 stale 상태를 표시한다.
- 기본 설정은 WebSocket disabled로 유지해서 기존 REST provider 동작을 보존한다.
- 기존 Upbit 전체 KRW REST polling scheduler는 가격 수집용이 아니라 `ALL_KRW` 후보 universe bootstrap/refresh 용도로만 사용한다.
  기본 동작은 부팅 시 1회와 `market.upbit-krw-ticker-polling.fixed-delay-ms` 간격 refresh이며, 기본값은 10분이다.
  WebSocket/SNAPSHOT 운영에서 명시 market만 사용하는 경우 `market.upbit-krw-ticker-polling.enabled=false`로 끌 수 있다.
- `/api/market-provider/status`는 WebSocket 활성 여부와 거래소별 snapshot 개수를 반환한다.

## Strategy

기본 전략은 `VolatilityBreakoutLongStrategy`다. 후보 스캔 기반 롱 전용 PAPER 전략을 사용한다.

현재 매매 조건과 조건 변경 후 PAPER 운용 결과는 `docs/trading/condition-records/`에 기록한다.

## Safety와 Risk

kill switch는 시세 조회와 전략 판단보다 먼저 확인한다. Risk는 허용 market, 주문 금액, 수량, 가격, 일일 제한, PAPER 현금과 보유 수량을 검증한다.

## Execution과 Portfolio

현재 실행 Gateway는 PAPER_TRADING 전용이며 PAPER 체결 결과만 포트폴리오에 반영한다. 기존 자동매매/수동 wrapper는 하위 호환을 위해 `UPBIT` portfolio에 반영하고, exchange-aware service API는 `UPBIT`과 `BINANCE` portfolio를 분리해 조회/저장한다.

## History와 Notification

HOLD, REJECTED, FILLED, FAILED 결과를 모두 저장한다. History에는 `exchange`를 저장하며, 기존 exchange 없는 호출과 기존 row는 `UPBIT`으로 취급한다. 알림은 history 저장 이후 선택적으로 실행한다.

## Analytics

Analytics는 React 화면이 직접 history를 재계산하지 않도록 서버에서 집계한다. 최근 실행 수, BUY/SELL/HOLD 개수, 체결/거절/실패 개수, 익절/손절 평균, PAPER 포트폴리오 손익, 반복 손실 market을 제공한다.

현재 웹이 사용하는 analytics API는 다음과 같다.

- `GET /api/analytics/summary?range=24h`
- `GET /api/analytics/pnl?range=24h`
- `GET /api/analytics/losses?range=24h`

## Web

웹 화면은 `frontend/`의 Vite React 앱으로 관리한다. 웹은 비즈니스 판단, 리스크 검증, 주문 상태 변경을 직접 구현하지 않고 Spring REST API만 호출한다.

웹은 기본적으로 모니터링 전용이다. 자동 실행 상태, 후보, 포트폴리오, history, analytics를 표시한다.

포트폴리오 화면은 선택 거래소 기준으로 현금/포지션/market/거래소 비중을 원형 그래프와 legend로 표시한다. `UPBIT`과 `BINANCE` 자산은 환산 없이 각각의 기준 통화(`KRW`, `USDT`) 안에서만 보여준다.

예외적으로 포트폴리오 화면에서는 사용자가 체크박스로 명시 선택한 보유 PAPER 포지션만 SELL 할 수 있게 계획한다. 이 예외는 실제 주문 API가 아니라 기존 `PAPER_TRADING` 주문, 리스크 검증, 포트폴리오 반영, history 저장 흐름을 통과해야 한다. 수동 BUY, 후보 실행, trading-flow 실행 버튼은 계속 제공하지 않는다.

자동 실행은 scheduler가 담당한다. 후보 실행은 `CandidateExecutionService`, 전략 실행과 익절/손절 평가는 `TradingFlowService`를 통해 PAPER 흐름만 실행한다.

웹은 실제 주문 API, `REAL_TRADING`, Upbit 인증 설정을 추가하지 않는다.

## 작업 관리

완료 기록과 다음 작업 계획은 `docs/project/PROJECT_PLAN.md`에서 시작한다.

작업 순서는 `docs/project/PROJECT_NEXT_STEPS.md`를 우선한다. 현재 MVP1 다음 작업은 자금 활용률과 포지션 분산 개선이다.
