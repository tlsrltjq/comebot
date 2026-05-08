# Exchange Dashboard Implementation Stages

이 폴더는 Comebot PAPER_TRADING 웹 대시보드의 거래소 모드 확장 작업을 stage 단위로 관리한다.

## 고정 원칙

- 기본 거래 모드는 `PAPER_TRADING`이다.
- `REAL_TRADING`은 구현하지 않는다.
- 실제 주문 API는 구현하지 않는다.
- 수동 BUY는 금지한다.
- 수동 SELL은 사용자가 웹 포트폴리오에서 명시 선택한 보유 PAPER 포지션에만 허용한다.
- Binance 인증키, secret, 계정 API는 사용하지 않는다.

## Stage Index

| Stage | 문서 | 상태 |
| --- | --- | --- |
| 1 | `STAGE_01_CONSTRAINTS_AND_PLAN.md` | 계획 완료 |
| 2 | `STAGE_02_BACKEND_EXCHANGE_MODE.md` | 구현 완료 |
| 3 | `STAGE_03_FRONTEND_EXCHANGE_MODE.md` | 구현 완료 |
| 4 | `STAGE_04_BINANCE_REST_PROVIDER.md` | 구현 완료 |
| 5 | `STAGE_05_EXCHANGE_PORTFOLIO_HISTORY.md` | 구현 완료 |
| 6 | `STAGE_06_SELECTED_PAPER_SELL.md` | 구현 완료 |
| 7 | `STAGE_07_PORTFOLIO_PIE_CHART.md` | 구현 완료 |
| 8 | `STAGE_08_WEBSOCKET_PRICE_FEED.md` | 구현 완료 |
| 9 | `STAGE_09_SCHEDULER_CADENCE.md` | 계획 완료 |
| 10 | `STAGE_10_BTC_CHANGE_CHART.md` | 계획 완료 |

## 구현 순서

1. Stage 2 백엔드 ExchangeMode 골격
2. Stage 3 프론트 ExchangeMode 버튼과 API 파라미터
3. Stage 4 Binance REST provider
4. Stage 5 거래소별 PAPER portfolio/history 분리
5. Stage 6 선택 포지션 PAPER 수동 매도
6. Stage 7 포트폴리오 원형 그래프
7. Stage 8 WebSocket 시세 수신
8. Stage 9 자동매매 주기 분리
9. Stage 10 BTC 등락률 그래프
