# Harness Status

## 현재 기준

- Java: 21
- 거래 모드: `PAPER_TRADING`
- 실제 주문 API: 없음
- 웹: 조회 중심 React 운영 화면. 자동매매 제어와 선택 보유 PAPER 포지션 SELL만 허용
- Telegram: 기본 조회 전용
- candidate scheduler 기본 주기: 60초
- exit scheduler 기본 주기: 5초
- 1회 PAPER 주문 금액: 10,000 KRW
- 익절/손절 기준: 1.5 / -0.7
- 같은 market 추가 진입: 기본 허용

## 최근 완료 (상세 이력: CHANGELOG.md)

- candidate scan log — 모든 스캔 결과 기록, GET /api/candidate-scan-log
- TradingCandidate.lastCandleBullish 필드, distanceFromHighRate/latestCandleTradeAmount 스냅샷 필드
- max-distance-from-high-rate 필터 (default 2%) — 피크 이후 하락 진입 차단
- min-latest-candle-trade-amount 필터 (KRW 1천만 / USDT 5만) — 유동성 하한 보장
- max-buys-per-run=2 — 스케줄러 1회 최대 BUY 수 제한
- PositionLimitRiskValidationService RiskValidationService 파이프라인 연결 (이전에는 미연결)
- stop-loss-cooldown 기본값 정정: enabled=true, window=1d, duration=6h
- MarketSelectionProperties 실제 연결 (top-20 KRW / top-30 USDT)
- PAPER 포지션 청산 흐름 스모크 테스트 (익절/손절/선택 SELL)
- strategy performance analytics (winRate, profitLossRatio, averageHoldingSeconds)

## 다음 작업

1. 2–3개월 운용 데이터 축적 후 trailing stop 설계 검토

## 검증 기준

- Backend 변경: `./gradlew test`
- Frontend 변경: `npm run lint`, `npm run build`, `npm test`
- Frontend 화면/반응형/위험 액션 변경: `npm run test:e2e`
- 문서만 변경: `git diff --check`, 필요 시 `./gradlew test`
- 보안 린트 또는 테스트 실패 시 커밋하지 않는다.

## 금지

- `REAL_TRADING` 구현
- 실제 Upbit/Binance 주문 API 구현
- 웹 수동 BUY/SELL 버튼
- 웹에서 후보 실행 API 또는 trading-flow 실행 API 호출
- 민감 정보 하드코딩
- 실패한 주문을 성공으로 처리
