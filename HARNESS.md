# comebot

## 한 줄 설명
Upbit/Binance 공개 시세 기반 롱 전용 변동성 매수 후보를 판단하고 PAPER_TRADING으로 검증하는 자동 매매 봇.

## 기술 스택
- Backend: Java 21 / Spring Boot / Gradle
- Frontend: React + TypeScript / Vite (port 5173, run-local-dev.sh 실행 시 5176)
- DB: PostgreSQL (JPA) / InMemory (테스트·단기 smoke)
- 거래소: Upbit (KRW), Binance (USDT) — 공개 API만, 인증키 없음
- 통신: WebSocket (시세 수신) + REST fallback

## 디렉토리 구조 (핵심)
```
src/main/java/com/giseop/comebot/
  market/       → 시세 수집 (WebSocket + REST, snapshot store)
  strategy/     → 후보 스캔 + BUY/SELL/HOLD 신호
  risk/         → kill switch, 리스크 검증, 익절/손절
  execution/    → PAPER 주문 실행 (PAPER 전용, 실제 주문 없음)
  portfolio/    → PAPER 현금, 포지션, 손익 관리
  history/      → 거래 이력 저장
  analytics/    → 집계 API
  scheduler/    → candidate / exit / trading 스케줄러
  telegram/     → 조회 전용 inbound/outbound
  notification/ → 선택적 알림
frontend/src/
  features/     → 화면별 React 컴포넌트
  shared/api/   → REST 호출 클라이언트 (타입 포함)
```

## 현재 상태
- **완료**: PAPER 자동 매수·청산, WebSocket+REST fallback, Upbit/Binance 이중 거래소, concentration risk, stop-loss cooldown, 전략 성과 analytics, Playwright E2E, candidate scan log, 진입 품질 필터 (lastCandleBullish/distanceFromHigh/minTradeAmount/maxBuysPerRun)
- **다음**: 2–3개월 PAPER 데이터 축적 후 trailing stop 설계 검토
- **보류**: REAL_TRADING, 실제 주문 API, 수동 BUY

## 코딩 규칙
- 거래 모드는 항상 `PAPER_TRADING`
- 민감 정보는 `.env` 또는 환경 변수로만 관리
- 클래스 하나에 시세·전략·리스크·주문·포트폴리오·알림·텔레그램을 함께 넣지 않음
- `@Scheduled`에 `fixedRate` 사용 금지 → `fixedDelay` 사용
- 전략·리스크·주문 흐름 변경 시 반드시 테스트 추가
- 보안 린트 실패 시 커밋 금지

## 절대 건드리면 안 되는 것
- `REAL_TRADING` 구현체 추가 금지
- 실제 Upbit/Binance 주문 API 호출 금지
- 웹에서 수동 BUY, 후보 실행, trading-flow 실행 버튼 금지
- `.env` / `TELEGRAM_BOT_TOKEN` / `CHAT_ID` 코드 하드코딩 금지

## 참고 문서 (상세)
| 문서 | 역할 |
|---|---|
| `docs/harness/HARNESS_STATUS.md` | 현재 기준값, 검증 명령, 금지 목록 |
| `docs/harness/ARCHITECTURE.md` | 모듈 구조, 거래소 분리 정책 |
| `docs/harness/DEVELOPMENT_RULES.md` | Git·모듈·문서·테스트 규칙 |
| `docs/harness/SECURITY_RULES.md` | 보안 린트 항목 |
| `docs/trading/STRATEGY_POLICY.md` | 전략 조건과 기본값 |
| `docs/trading/RISK_POLICY.md` | 익절/손절, 쏠림, cooldown 정책 |
| `docs/trading/ORDER_LIFECYCLE.md` | 주문 상태 흐름 |
| `docs/operations/OPERATIONS.md` | 실행 스크립트, 상태 확인 API |
| `docs/operations/RELIABILITY.md` | 장애 처리, WebSocket 복구 |
| `docs/operations/TELEGRAM_UX.md` | Telegram 명령/버튼 흐름 |
| `docs/decisions.md` | 기술 결정 ADR |
| `tasks/current.md` | 현재 세션 작업 컨텍스트 |
| `CHANGELOG.md` | 변경 이력 |
