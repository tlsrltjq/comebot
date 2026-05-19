# Project History

완료 이력의 source of truth는 `CHANGELOG.md`와 Git history다.
이 문서는 현재 운영 중인 기능의 맥락만 요약한다.

## 현재 운영 기능

- **시세 수집**: Upbit/Binance WebSocket + REST fallback (SNAPSHOT provider)
- **후보 스캔**: VolatilityBreakoutLong — 변동성·거래대금·추세 기반 롱 후보 판단
- **자동 실행**: candidate scheduler (BUY) + exit scheduler (익절/손절)
- **리스크**: concentration risk (쏠림 차단), stop-loss cooldown, daily limit, kill switch
- **포트폴리오**: Upbit(KRW) + Binance(USDT) 분리, JPA/InMemory 이중 저장
- **이력**: `trading_flow_history` + `paper_trade_log` append-only 원장
- **analytics**: summary (winRate, profitLossRatio, averageHoldingSeconds), pnl, losses
- **웹 UI**: React+Vite, 조회 전용 + 선택 PAPER SELL 예외
- **Telegram**: 조회 전용 (수동 BUY·실행 경로 코드 레벨 차단)

## 최근 검증

- `./gradlew test` 통과 (Java 21)
- `npm run lint`, `npm test`, `npm run build`, `npm run test:e2e` 통과
