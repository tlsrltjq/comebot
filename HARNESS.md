# HARNESS.md — comebot 에이전트 하네스

## 목적

Upbit(KRW)·Binance(USDT) 공개 시세로 눌림목 반등 롱 후보를 스캔하고
`PAPER_TRADING`에서 주문·포트폴리오·손익을 검증하는 자동매매 봇.
실제 주문 API, `REAL_TRADING`, 인증키 사용은 영구 금지(ADR-001).

## 현재 상태

| 항목 | 값 |
|---|---|
| 거래 모드 | `PAPER_TRADING` 전용 |
| 거래소 | Upbit(KRW) + Binance(USDT), 공개 API만 |
| 운영 전략 | 기본값은 `SIMPLE_THRESHOLD`, 기존 PullbackBounce / bean `VOLATILITY_BREAKOUT_LONG`은 관찰 대상. 자동 진입 스케줄러는 OFF |
| PAPER 후보 전략 | `SESSION_VOLATILITY_BREAKOUT` 구현됨. Binance 전용 15m UTC 06-12, no-pegged 기본 제외, 신호 캔들 close 지정가 |
| 리서치 전략 | 기존 5개 BTC/ETH 시드 후보 생존 0. Session Volatility Breakout Binance 15m UTC 06-12 후보가 5m/1m 하위 캔들 maker 감사 통과 |
| 진입 | 운영 엔진은 maker 지정가: 신호 캔들 close 가격, 5분 유효 (ADR-013 보류/조건부). 리서치 후보는 next open 모델 |
| 청산 | TP +4.0%, SL -2.0%, trailing off (ADR-011) |
| 주기 | candidate/exit scheduler 기본 OFF, 관찰 대시보드 전용 |
| 포지션 상한 | Upbit 8 / Binance 8 / 합계 12 |
| 포트 | backend 18080 / frontend dev 5176 / PostgreSQL 5433 |
| 최근 작업 | Session Volatility 재진입 방지 설정 강제, BNB 오염 포지션 정리 후 automation 재개 |
| 다음 단계 | 다음 KST 15:00-21:00 신규 세션에서 same-market 반복 BUY 차단 여부 확인 |

## 구현 요약

- Backend: Java 21, Spring Boot 4.0.5, Gradle, JPA/Hibernate 7
- Frontend: React, TypeScript, Vite
- Market: Upbit/Binance ticker WebSocket + REST fallback, BTC 1h EMA trend cache
- Strategy/Risk: per-exchange scanner override, 포지션 상한, 손절 cooldown, 비정상 시세 차단, kill switch
- Execution: PAPER 주문 실행 + `PendingLimitOrderService` maker 지정가 체결(`capturedAt > createdAt`, fresh price only)
- Storage/UI: history, scanlog, analytics, React 운영 화면, 조회 전용 Telegram
- Backtest: `.backtest_cache` 원본 캔들, last-60d OOS split, 공통 리더보드 CSV/Markdown, 전략별 운용 기록
- Stock expansion: 기존 코인 기능 유지, 미국 주식 PAPER 리서치 라인부터 검토
- 자세한 구조와 정책은 `docs/architecture.md`, `docs/spec.md`, `docs/decisions.md` 참조

## 불변 규칙

- `REAL_TRADING` 구현 금지
- 실제 Upbit/Binance 주문 API 호출 금지
- 웹 수동 BUY 버튼 및 `/run`, `/candidate-run` 호출 UI 금지
- 민감 정보(API 키, Bot Token, Chat ID, DB 비밀번호) 하드코딩 금지
- 실패·거절 주문을 체결로 처리 금지
- 테스트 또는 Checkstyle/ESLint 실패 상태로 커밋 금지

## 작업 절차

1. 시작 시 `tasks/current.md`를 확인하고 현재 단계/목표를 한 줄로 보고
2. 코드 수정 전 영향 파일 목록 보고
3. 비즈니스 로직 변경은 테스트와 함께 진행
4. 완료 기준 달성 후 범위 확장 금지
5. 관련 문서와 `tasks/current.md` 갱신 후 논리 단위로 커밋

## 수정 주의 파일

| 파일 | 규칙 |
|---|---|
| `.env` | 수정 금지 |
| `src/main/resources/application.properties` | 기본값 변경 전 사용자 확인 |
| `frontend/eslint.config.js` | 린트 완화 전 사용자 확인 |
| `config/checkstyle/checkstyle.xml` | 린트 완화 전 사용자 확인 |
| `docs/trading/condition-records/` | 기존 기록 수정 금지, 추가만 허용 |

## 검증 명령

```bash
./gradlew test checkstyleMain
cd frontend && npm run lint && npm run build && npm test
cd frontend && npm run test:e2e  # 화면/반응형/위험 액션 변경 시
bash scripts/run-local-dev.sh    # backend 18080, frontend 5176
```

문서만 변경한 경우 `git diff --check`로 공백 오류를 확인한다.

## Current Status Update (2026-06-24)

- Backend host port is `18082`, web UI is `5176`, and PostgreSQL host port is `5434`.
- PAPER observation remains scoped to Binance Session Volatility Breakout; candidate and exit automation should be verified after Docker restart.
- Pending limit entry placement now uses an atomic pending-order guard so duplicate same-market candidate runs are rejected before overwriting an existing pending order.
- Candidate scan logs now have a daily summary retention path: detailed candidate scan logs keep 30 days by default, trading flow history keeps 90 days by default, and older candidate rows are summarized into `candidate_scan_daily_summary`.

## Current Status Update (2026-06-24, trade journal)

- Trade journal matched trades now read from `paper_trade_log` instead of FIFO matching `trading_flow_history`.
- Profit rate is derived from SELL `realized_profit` and SELL `gross_amount` cost basis, so the profit sign and exit label are aligned.
- Exit label is classified from realized profit: positive = `TAKE_PROFIT`, negative = `STOP_LOSS`, zero/null = `MANUAL`.
- Selected PAPER position sell uses a PAPER exit-only path so held positions can be closed even when the current entry allow-list excludes that market.
- Docker scheduler restore defaults to disabled; use `scripts/restart-session-volatility-docker.bat` or `.sh` for Binance Session Volatility PAPER profile restarts.

## Session End Status (2026-06-24)

- Backend verification passed with `./gradlew.bat test checkstyleMain`.
- Binance Session Volatility PAPER profile is the expected Docker restart path; app/web should be restarted through the dedicated restart script or equivalent environment.
- Trade journal, PAPER cleanup, and Docker profile behavior are documented in architecture/spec/operations and the 2026-06-24 condition record.

## Current Status Update (2026-06-25, stock import foundation)

- Stock research remains PAPER-only and automation remains OFF.
- Stock market identity models are implemented with `MarketAssetClass`, `MarketVenue`, and `MarketIdentity`; `ExchangeMode` remains crypto-only.
- Stock candle import now uses `StockCandleImportManifest`, `StockCandleRow`, and `StockCandleCsvImporter`.
- Supported stock import intervals are `1m`, `5m`, `15m`, and `1d`; US stock timezone is `America/New_York`.
- Test-only backtest loading can convert validated stock CSV rows into `CandleSeries` through `BacktestSeriesLoader.loadStockSeries(...)`.
- Bundled sample stock fixture: `src/test/resources/stock/us/sample/15m/AAPL.csv`.
- Verification passed with `./gradlew.bat test checkstyleMain`.

## Session End Status (2026-06-25)

- Backend verification passed with `./gradlew.bat test checkstyleMain`.
- Commit `6a70240 Add stock candle import foundation` contains the stock identity/import/backtest-loader foundation.
- Documentation split plan is defined but not applied yet; next documentation work should separate `docs/crypto/` and `docs/stock/` while keeping existing condition records in place.
