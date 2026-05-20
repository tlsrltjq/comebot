# Project History

완료 이력의 source of truth는 `CHANGELOG.md`와 Git history다.
이 문서는 현재 운영 중인 기능의 맥락만 요약한다.

## 현재 운영 기능

- **시세 수집**: Upbit/Binance WebSocket + REST fallback (SNAPSHOT provider)
- **후보 스캔**: VolatilityBreakoutLong — trend UP + lastCandleBullish + priceChangeRate(≥1%) + tradeAmountChangeRate(≥30%) + distanceFromHigh(≤2%) + minTradeAmount(KRW 1천만 / USDT 5만)
- **스캔 로그**: 모든 스캔 결과(SELECTED/SKIPPED) append-only 기록 (`GET /api/candidate-scan-log`)
- **자동 실행**: candidate scheduler (BUY, 1회 최대 2개) + exit scheduler (익절/손절)
- **리스크**: 포지션 수 상한 (UPBIT max 3, BINANCE max 3, 합산 max 5), concentration risk, stop-loss cooldown (enabled: 1d 내 2회 → 6h 차단), daily limit, kill switch
- **포트폴리오**: Upbit(KRW) + Binance(USDT) 분리, JPA/InMemory 이중 저장
- **이력**: `trading_flow_history` + `paper_trade_log` append-only 원장
- **analytics**: summary (winRate, profitLossRatio, averageHoldingSeconds), pnl, losses
- **웹 UI**: React+Vite, 조회 전용 + 선택 PAPER SELL 예외
- **Telegram**: 조회 전용 (수동 BUY·실행 경로 코드 레벨 차단)

## 최근 검증

- `./gradlew test` 통과 (Java 21)
- `npm run lint`, `npm test`, `npm run build`, `npm run test:e2e` 통과
