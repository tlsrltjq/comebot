# Project Next Steps

## 완료된 큰 흐름

- Stage 1-10 거래소 대시보드 확장 완료
  - ExchangeMode 백엔드/API 골격
  - 프론트 ExchangeMode 전환
  - Binance 공개 REST 현재가/캔들 provider
  - Exchange별 PAPER 포트폴리오와 history 분리
  - 선택 보유 PAPER 포지션 수동 매도
  - 포트폴리오 원형 그래프
  - WebSocket snapshot store와 REST fallback provider
  - candidate/exit scheduler 주기 분리
  - BTC 등락률 Market Overview 페이지
- 포트폴리오 모바일 카드형 UX 완료
- 후보 화면 SELECTED/SKIPPED 요약, 제외 사유 TOP 5, 선택 후보 필터, 보유 포지션 표시 완료

## 다음 우선순위: 운영 상태 화면과 Telegram 용어 정리

목표:

- status API, 웹, Telegram 상태 메시지의 항목 이름을 맞춘다.
- 웹과 Telegram 모두 자동 실행 상태, 매매 조건, 손익을 같은 용어로 보여준다.
- Telegram은 기본 조회 전용으로 유지한다.
- Telegram 수동 PAPER 실행은 `TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=true`일 때만 허용한다.

작업:

- System, Trade, Dashboard 화면의 scheduler/strategy/risk 용어를 Telegram `/auto`, `/conditions`, `/pnl` 표현과 대조한다.
- 용어 차이가 있으면 UI 문구 또는 Telegram 메시지를 한쪽 기준으로 맞춘다.
- 웹 API client에 후보 실행/trading-flow 실행 함수가 없는 상태를 유지한다.
- 웹 lint의 실행 endpoint 차단 규칙을 유지한다.
- Telegram 기본 menu에 실행 버튼이 없는 상태를 유지한다.

완료 기준:

- 웹과 Telegram에서 자동 실행 상태, 매매 조건, 손익 용어가 일치한다.
- 웹 API client에 수동 실행 함수가 없다.
- 웹 lint가 실행 endpoint 추가를 차단한다.
- Telegram menu에 실행 버튼이 없다.
- `/auto`, `/conditions`, `/pnl`로 자동 실행 상태와 손익을 확인할 수 있다.
- `./gradlew test`, `npm run lint`, `npm run build`, `npm test`가 성공한다.

## 보류: market별 쏠림 리스크 기준

현재 문서화된 PAPER 기록만으로 초기 기준은 잡을 수 있지만, 장기 기준은 JPA history/portfolio 데이터를 더 쌓은 뒤 확정한다.

보류 사유:

- 현재 `.env` 기본값은 `HISTORY_STORAGE_TYPE=IN_MEMORY`, `PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY`라 장기 원자료가 충분하지 않다.
- 2026-05-06 기록 기준 단일 market TOP exposure는 7.06%로, 10% 초과 쏠림 사례가 아직 없다.

재개 조건:

- JPA history/portfolio로 며칠 이상 PAPER 운용 데이터를 누적한다.
- 단일 market 10% 초과 또는 반복 손절 market이 관찰된다.
- 기준 확정 시 `docs/trading/RISK_POLICY.md`와 `docs/trading/condition-records/`를 함께 갱신한다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
