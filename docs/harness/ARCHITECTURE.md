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

## Market

- `InMemoryMarketPriceProvider`: 테스트용 가격 provider
- `UpbitMarketPriceProvider`: Upbit 공개 Ticker API provider
- `UpbitCandleProvider`: Upbit 공개 Candle API provider
- Access Key, Secret Key는 사용하지 않는다.

## Strategy

기본 전략은 `VolatilityBreakoutLongStrategy`다. 후보 스캔 기반 롱 전용 PAPER 전략을 사용한다.

현재 매매 조건과 조건 변경 후 PAPER 운용 결과는 `docs/trading/condition-records/`에 기록한다.

## Safety와 Risk

kill switch는 시세 조회와 전략 판단보다 먼저 확인한다. Risk는 허용 market, 주문 금액, 수량, 가격, 일일 제한, PAPER 현금과 보유 수량을 검증한다.

## Execution과 Portfolio

현재 실행 Gateway는 PAPER_TRADING 전용이며 PAPER 체결 결과만 포트폴리오에 반영한다.

## History와 Notification

HOLD, REJECTED, FILLED, FAILED 결과를 모두 저장한다. 알림은 history 저장 이후 선택적으로 실행한다.

## Analytics

Analytics는 React 화면이 직접 history를 재계산하지 않도록 서버에서 집계한다. 최근 실행 수, BUY/SELL/HOLD 개수, 체결/거절/실패 개수, 익절/손절 평균, PAPER 포트폴리오 손익, 반복 손실 market을 제공한다.

현재 웹이 사용하는 analytics API는 다음과 같다.

- `GET /api/analytics/summary?range=24h`
- `GET /api/analytics/pnl?range=24h`
- `GET /api/analytics/losses?range=24h`

## Web

웹 화면은 `frontend/`의 Vite React 앱으로 관리한다. 웹은 비즈니스 판단, 리스크 검증, 주문 상태 변경을 직접 구현하지 않고 Spring REST API만 호출한다.

웹은 모니터링 전용이다. 자동 실행 상태, 후보, 포트폴리오, history, analytics를 표시하며 수동 BUY/SELL 버튼을 제공하지 않는다.

자동 실행은 scheduler가 담당한다. 후보 실행은 `CandidateExecutionService`, 전략 실행과 익절/손절 평가는 `TradingFlowService`를 통해 PAPER 흐름만 실행한다.

웹은 실제 주문 API, `REAL_TRADING`, Upbit 인증 설정을 추가하지 않는다.

## 작업 관리

완료 기록과 다음 작업 계획은 `docs/project/PROJECT_PLAN.md`에서 시작한다.

작업 순서는 `docs/project/PROJECT_NEXT_STEPS.md`를 우선한다. 현재 MVP1 다음 작업은 자금 활용률과 포지션 분산 개선이다.

## MVP2 확장 방향

MVP2는 `docs/project/MVP2_PLAN.md`에 별도 관리한다.

MVP2에서도 실제 주문 API와 `REAL_TRADING`은 구현하지 않는다. Binance, 선물 롱, 선물 숏은 public market data와 PAPER/SIMULATION 계층에서 먼저 검증한다.

초기 MVP2 코드 경계는 `com.giseop.comebot.mvp2` 아래에 둔다.

- `mvp2.exchange`: 거래소 식별자와 public market data 전용 경계
- `mvp2.experiment`: 전략 실험 실행 단위와 상태
- `mvp2.strategy`: 안정형, 공격형, 수비형 strategy profile 용어
- `mvp2.simulation`: spot/futures long/futures short simulation market type
- `mvp2.leaderboard`: 실험 결과 비교와 순위 집계 경계

MVP1의 `market`, `strategy`, `portfolio`, `history` 구현은 MVP2 실험 저장소와 섞지 않는다. MVP2가 기존 Upbit 시세를 재사용하더라도 adapter 계층을 통해 연결한다.

MVP2 exchange 공통 시세 경계는 다음 모델을 사용한다.

- `ExchangeMarketDataProvider`: 거래소별 ticker/candle 조회 adapter 인터페이스
- `ExchangeTicker`: 거래소, symbol, 현재가, 수집 시각
- `ExchangeCandle`: 거래소, symbol, OHLCV, 누적 거래대금/거래량
- `ExchangeSymbolNormalizer`: 거래소별 symbol 정규화
- `UpbitExchangeMarketDataAdapter`: 기존 Upbit ticker/candle provider를 MVP2 공통 모델로 변환
- `BinanceExchangeMarketDataProvider`: Binance public ticker/kline API를 MVP2 공통 모델로 변환

Binance는 인증키 없이 public endpoint만 사용한다.

- ticker: `GET https://api.binance.com/api/v3/ticker/price`
- candle: `GET https://api.binance.com/api/v3/klines`

MVP2 exchange 상태 API는 다음과 같다.

- `GET /api/mvp2/exchanges`: 웹에서 선택 가능한 거래소 목록
- `GET /api/mvp2/exchanges/{exchange}/status`: 거래소별 public data/PAPER-only 상태

React 웹은 `/mvp2` 경로에서 Upbit/Binance 선택 버튼과 strategy profile 준비 상태를 표시한다. 이 화면도 모니터링 전용이며 수동 BUY/SELL 버튼을 제공하지 않는다.

Binance PAPER 흐름은 MVP1 Upbit PAPER 자동매매와 같은 기능 범위를 목표로 하되, MVP1 포트폴리오와 분리한다.

- Binance public ticker/kline 기반 후보 판단
- Binance 전용 PAPER 현금, 포지션, 실현손익
- 익절/손절 기반 PAPER SELL
- Binance PAPER history 저장
- 자동 scheduler는 `mvp2.paper.binance-scheduler-enabled=false`가 기본값
- 웹 `/mvp2`에서 Binance PAPER 상태, 후보, 포지션, 이력을 조회

초기 Binance PAPER API는 다음과 같다.

- `GET /api/mvp2/binance/paper/status`
- `GET /api/mvp2/binance/paper/candidates`
- `GET /api/mvp2/binance/paper/portfolio`
- `GET /api/mvp2/binance/paper/history`
