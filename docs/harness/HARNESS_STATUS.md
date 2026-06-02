# Harness Status

## 현재 기준

- Java: 21
- 거래 모드: `PAPER_TRADING`
- 실제 주문 API: 없음
- 웹: 조회 중심 React 운영 화면. 자동매매 제어와 선택 보유 PAPER 포지션 SELL만 허용
- Telegram: 기본 조회 전용
- candidate scheduler 기본 주기: 60초 (운영 `.env`: 30초)
- exit scheduler 기본 주기: 1초
- 1회 PAPER 주문 금액: 10,000 KRW (운영 `.env`: 동일)
- 익절/손절 기준: 1.5 / -0.7 (운영 `.env`: 2.0 / -1.5)
- 트레일링 스톱: 기본 비활성 (운영 `.env`: 활성, 활성화 2%, trail 1.5%)
- 같은 market 추가 진입: 기본 허용

## 최근 완료 (상세 이력: CHANGELOG.md)

- KST 시간대 진입 필터 메커니즘 — `strategy.entry.allowed-hours-kst` (기본 **비활성**: OOS 검증서 과최적화 확인, 튜닝 도구로만 유지)
- KRW-IRYS 스캔 제외 추가 (OOS에서도 견고하게 최악 마켓)
- ⚠️ OOS 검증 결론: 현 전략은 비용 차감 후 검증된 엣지 없음 — 청산구조/timeframe 재설계 필요 (condition-records/2026-06-02-oos-validation-and-edge-analysis.md)
- per-exchange scanner override — Upbit/Binance 캔들 수·필터값 독립 설정 (`strategy.candidate-scanner.upbit.*` / `.binance.*`)
- 미완성 최신 캔들 제외 — 진행 중인 1분봉 드롭 후 완성 캔들만 판단에 사용
- market exclusion list — `market.selection.excluded-markets` 로 특정 마켓 스캔 제외
- guard abnormal paper exits — 비정상 급락 시세로 자동 SELL 차단 (`risk.abnormal-exit-price-drop-rate`)
- pullback bounce strategy 전면 교체 — 펌프 추격 → 눌림목 반등 진입 (ADR-010)
- BTC 1h 트렌드 필터 — `BtcTrendCacheService` EMA 기반, DOWN이면 진입 차단
- trailing stop — 활성화율·trail 비율 설정, `PositionExitSignalService`에서 peak profit 추적
- exit scheduler 주기 5초 → 1초 단축
- profit-based re-entry guard — `strategy.entry.min-reentry-profit-rate` 이상 미실현 수익 시에만 동일 market 재진입 허용
- Docker 전체 스택 지원 (docker-compose, 18080 포트)
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

1. 운용 데이터 축적 후 pullback bounce 전략 파라미터 튜닝 (per-exchange 기준값 개선)

## 검증 기준

- Backend 변경: `./gradlew test checkstyleMain`
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
