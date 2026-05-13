# Exchange Dashboard Upgrade Plan

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

Comebot 웹 대시보드를 `PAPER_TRADING` 범위 안에서 확장한다. Upbit 중심 구조를 유지하되, 거래소 모드, 선택 포지션 PAPER 매도, WebSocket 시세 수신, 자산 비중 차트, BTC 등락률 그래프를 단계적으로 추가한다.

## 고정 제약

- 기본 거래 모드는 `PAPER_TRADING`이다.
- `REAL_TRADING`은 구현하지 않는다.
- 실제 주문 API는 구현하지 않는다.
- Upbit/Binance 인증키, secret, token은 코드와 문서 예시에 넣지 않는다.
- 수동 BUY는 계속 금지한다.
- 수동 SELL은 사용자가 웹 포트폴리오에서 명시 선택한 보유 PAPER 포지션에만 허용한다.
- Telegram은 이번 계획에서 조회 전용으로 유지한다.

## 설계 가정

- 수동 PAPER SELL은 1차 구현에서 선택 market 전량 매도만 제공한다.
- 매도 가격과 수량 검증은 서버가 수행한다.
- kill switch가 켜져 있으면 수동 PAPER SELL도 차단한다. 손실 축소 목적의 reduce-only 예외는 별도 승인 후 검토한다.
- 거래소 모드는 `UPBIT`을 기본값으로 둔다.
- `exchange` query parameter는 `upbit`, `binance` 같은 소문자 입력도 허용한다.
- Binance가 아직 구현되지 않은 API는 `501 Not Implemented`로 응답한다.
- Binance REST 1차 범위는 `USDT` 현물 마켓만 지원한다.
- Binance 초기 종목은 Upbit KRW 후보와 매핑 가능한 코인으로 제한한다.
- 새로고침 유지가 필요하면 localStorage 대신 URL query를 우선 사용한다.

## ExchangeMode

`ExchangeMode`는 `UPBIT`, `BINANCE` 두 값으로 시작한다. 프론트 사이드바 상단에 `UPBIT / BINANCE` 버튼을 두고, 선택값을 API 요청 파라미터와 백엔드 도메인 모델에 일관되게 전달한다.

같은 화면 구조를 유지하고 데이터 소스만 바꾼다.

- Dashboard
- Candidates
- Auto Run
- Portfolio
- History
- Market Overview

## WebSocket 시세 수신

시세 수신은 거래소별로 분리한다.

- Upbit WebSocket client
- Binance WebSocket client
- 거래소별 ticker snapshot store
- REST fallback provider

조회 우선순위는 다음과 같다.

1. WebSocket 최신 snapshot
2. 최근 snapshot 유지, stale 표시
3. REST fallback
4. 실패 응답, 주문 실행 차단

## 자동매매 주기 권장안

- WebSocket ticker: 지속 수신
- REST fallback polling: 장애 시 보조 경로
- 후보 탐색: 30초에서 60초
- 보유 포지션 exit 평가: 5초에서 10초
- market별 실행 간격: API 제한을 고려해 유지
- 반복 HOLD history는 과다 적재 방지 정책을 검토한다.

## UI 계획

- 사이드바 상단에 `UPBIT / BINANCE` mode switch를 둔다.
- 포트폴리오에는 선택 체크박스와 선택 PAPER 매도 toolbar를 둔다.
- 포트폴리오 metric 아래에 원형 그래프를 둔다.
- 원형 그래프는 현금 비중, market별 비중, 거래소별 비중 순으로 확장한다.
- Market Overview 페이지에 BTC 등락률 그래프를 둔다.
- Upbit 모드는 `KRW-BTC`, Binance 모드는 `BTCUSDT`를 사용한다.
- 범위는 `1h`, `24h`, `3d`, `7d`를 지원한다.

## Stage

1. 문서 제약 정리와 ExchangeMode 계획 문서화
2. ExchangeMode 백엔드 도메인/API 골격
3. 사이드바 ExchangeMode 버튼과 프론트 API 파라미터 준비
4. Binance REST market/candle provider 추가
5. Exchange별 PAPER 포트폴리오와 history 분리
6. 선택 포지션 PAPER 수동 매도
7. 포트폴리오 원형 그래프 UI
8. WebSocket 시세 수신 기반
9. 자동매매 주기 분리와 설정 정리
10. BTC 등락률 그래프 페이지

## Stage 상세 계획

- Stage 1: `docs/project/exchange-dashboard/STAGE_01_CONSTRAINTS_AND_PLAN.md`
- Stage 2: `docs/project/exchange-dashboard/STAGE_02_BACKEND_EXCHANGE_MODE.md`
- Stage 3: `docs/project/exchange-dashboard/STAGE_03_FRONTEND_EXCHANGE_MODE.md`
- Stage 4: `docs/project/exchange-dashboard/STAGE_04_BINANCE_REST_PROVIDER.md`
- Stage 5: `docs/project/exchange-dashboard/STAGE_05_EXCHANGE_PORTFOLIO_HISTORY.md`
- Stage 6: `docs/project/exchange-dashboard/STAGE_06_SELECTED_PAPER_SELL.md`
- Stage 7: `docs/project/exchange-dashboard/STAGE_07_PORTFOLIO_PIE_CHART.md`
- Stage 8: `docs/project/exchange-dashboard/STAGE_08_WEBSOCKET_PRICE_FEED.md`
- Stage 9: `docs/project/exchange-dashboard/STAGE_09_SCHEDULER_CADENCE.md`
- Stage 10: `docs/project/exchange-dashboard/STAGE_10_BTC_CHANGE_CHART.md`
