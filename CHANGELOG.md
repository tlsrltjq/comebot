# 변경 이력

2026-06-15 | backtest | test: session volatility 하위 캔들 maker 감사 — 15m 신호+5m/1m 300초 유효성 통과, no-event/no-pegged stress 후보 유지
2026-06-15 | backtest | test: session volatility maker 1차 감사 — 15m coarse 지정가 모델 통과, 5m/1m 실제 5분 체결 감사 필요
2026-06-15 | backtest | test: session volatility 정밀 검증 — 후보 13개 비용/유니버스 156개 시나리오, Binance 15m UTC 06-12 stress 후보 유지
2026-06-15 | backtest | feat: session volatility breakout 하니스 추가 — 확장 유니버스 640개 조합, Binance 중심 PAPER 후보 12개 + Upbit 약한 후보 1개 산출
2026-06-15 | backtest | test: ranked rotation 정밀 검증 — 비용/유니버스 72개 시나리오, 보수 비용에서 전부 net weak로 PAPER 전환 보류
2026-06-15 | backtest | feat: ranked rotation 확장 유니버스 하니스 추가 — Binance 15m 약한 PAPER 관찰 후보 6개 산출, exchange 필터 회귀 보정
2026-06-05 | backtest | docs: 세션 종료 — V1 풀백군 전면 진단 결론(모든 레버 gross 0.94~0.97, 엣지 없음) HARNESS/tasks 갱신, 다음=전략 전환/데이터 재검토 등록
2026-06-05 | backtest | feat: 다중 TF 확인(MultiTimeframeTest) — 코인 1h 정렬 M1이 OOS 최선이나 gross 0.97<1.05, 필터 스택 OOS 붕괴 재확인
2026-06-05 | backtest | feat: 레짐 필터(RegimeAnalysisTest, EntryGate, RegimeContext) — V1 손실 BTC 하락 집중 확인, F1(BTC≠DOWN) 최선이나 ≥1.05 미달, 결합 감산
2026-06-05 | backtest | test: 결정론적 엔진 단위테스트(DeterministicBacktestEngineTest) 6종 — maker 체결·만료·시장가 진입·TP/SL 임계 청산·gross/net 비용 산술 (캐시 불필요, 기본 suite 상시)
2026-06-05 | backtest | feat: 진입 신호 스윕(EntrySweepTest) — 풀백 전략군 수익 구간 없음 확인, V1 눌림폭 하한만 리드(gross 0.91→0.97, 여전히 <1.0)
2026-06-05 | backtest | feat: 청산 구조 스윕(ParameterSweepTest) — TP×SL 12칸 전부 train gross<1.0, 신호 문제 확정
2026-06-05 | backtest | feat: 운영 Java 엔진 리플레이 parity 백테스트 하니스(src/test/.../backtest) + gross/net PF·maker vs market 진단 — 현행 설정 gross PF<1.0(신호 문제) 발견, 연구 로그 기록
2026-06-04 | ui      | feat: 매매 일지 — BUY→SELL FIFO 매칭 API(/api/analytics/matched-trades) + /trade-journal 페이지 (매수/매도 시각·가격, 보유시간, 수익률, 청산방식)
2026-06-04 | harness | docs: 토큰 절약을 위해 HARNESS.md와 tasks/current.md를 현재 상태 중심으로 압축
2026-06-04 | entry | fix: 2차 감사 — firstCheckAt 과보수(i+2) 제거→capturedAt>createdAt, stale 체결 방지→findFresh, 단위테스트 6종 (ADR-013 보류/조건부)
2026-06-04 | entry | fix: same-candle fill guard (firstCheckAt), ADR-013 검증 — maker-entry+taker-exit
2026-06-04 | entry | feat: limit order entry (PendingLimitOrderService, fillLimitOrder)
2026-06-04 | strategy | docs: ADR-012 — 진입 신호 재설계 OOS 전체 실패, 현행 1m×20 유지 결정
2026-06-03 | ui | feat: UI 전면 다크모드 리뉴얼 — 트레이딩 콘솔 스타일 (GitHub dark 팔레트, 고밀도 레이아웃, 모노스페이스 숫자, Recharts 다크모드 색상 통일)

## 형식
`날짜 | 단계 | feat/fix/chore/docs: 내용`

2026-06-03 | 진입 신호 재설계 | feat: consecutiveBullishCandles·priceRecoveryRate 진입 필터 추가 (기본값 비활성, ADR-012 후보)
2026-06-02 | 진입 신호 재설계 | feat: volumeCooldownRatio 진입 필터 추가 (기본값 비활성, ADR-012 후보)

---

2026-06-02 | strategy | feat: exit redesign — let winners run (TP 2→4, SL -1.5→-2, trailing off); OOS-validated train 0.94/test 1.02 (ADR-011)
2026-06-02 | strategy | docs: ADR-011 strategy redesign initiative (param tuning halted, OOS gate)
2026-06-02 | strategy | chore: disable time-of-day filter by default (OOS train/test validation showed it overfits; mechanism kept as tunable tool)
2026-06-02 | strategy | feat: add KST time-of-day entry filter (strategy.entry.allowed-hours-kst, default 0,12,17,21,23) + exclude KRW-IRYS
2026-06-02 | harness | chore: 하네스 초기 구축 (CLAUDE.md, HARNESS.md, tasks/current.md, docs/decisions.md, docs/architecture.md, docs/spec.md)
2026-06-02 | refactor | refactor: DI inversion, @Deprecated legacy, Checkstyle, unused imports
2026-06-02 | docs  | docs: sync HARNESS_STATUS, ARCHITECTURE, RISK_POLICY with implemented features
2026-05-28 | strategy | feat: split candidate scanner filters by exchange (upbit.* / binance.* overrides)
2026-05-27 | strategy | fix: ignore in-progress latest candle during candidate scan
2026-05-27 | strategy | chore: relax PAPER entry filters for ordinary market conditions and document rollback values
2026-05-27 | risk | chore: loosen PAPER position limits without disabling risk validation
2026-05-22 | risk    | feat: guard abnormal paper exits (abnormal-exit-price-drop-rate=-20)
2026-05-22 | market  | feat: add market exclusion list to MarketSelectionService
2026-05-22 | strategy | docs: add ADR-010 for pullback bounce entry strategy overhaul
2026-05-22 | strategy | feat: overhaul entry strategy from pump-chasing to pullback bounce with BTC trend filter
2026-05-22 | risk    | feat: add trailing stop loss to position exit (activation-rate, trail-rate)
2026-05-22 | perf    | perf: reduce position exit scheduler interval to 1s
2026-05-22 | risk    | feat: add profit-based re-entry guard (min-reentry-profit-rate)
2026-05-22 | ops     | feat: add Docker support for full stack deployment (18080 port)
2026-05-20 | quality | feat: add max-distance-from-high-rate filter (최근 고가 대비 2% 초과 하락 시 진입 거부)
2026-05-20 | quality | feat: add min-latest-candle-trade-amount filter (KRW 1천만/USDT 5만 미만 거래대금 진입 거부)
2026-05-20 | quality | feat: add max-buys-per-run=2 — 스케줄러 1회 최대 BUY 수 제한
2026-05-20 | scanlog | feat: implement candidate scan log — 모든 스캔 결과 기록, GET /api/candidate-scan-log
2026-05-20 | scanlog | feat: TradingCandidate에 lastCandleBullish 필드 추가, 스캔 로그에 포함
2026-05-20 | risk    | fix: PositionLimitRiskValidationService RiskValidationService 파이프라인에 주입 (이전에는 미연결)
2026-05-20 | config  | feat: MarketSelectionProperties 실제 연결 (top-20 KRW / top-30 USDT)
2026-05-19 | binance | fix: BinanceCandleProvider 죽은 @Component 제거, price-provider 기본값 UPBIT→SNAPSHOT (Binance exit 정상화)
2026-05-19 | risk  | fix: enable stop-loss cooldown (1d/2회→6h차단), lastCandleBullish 필터 추가, minPriceChangeRate 0.3→1.0
2026-05-19 | sync  | docs: optimize docs — delete 5 stale plan files, trim completed history to CHANGELOG
2026-05-19 | sync  | chore: align orderQuantity default, register WebSocket config, update HARNESS_STATUS next task
2026-05-19 | sync  | docs: restructure harness — add HARNESS.md, CHANGELOG, decisions.md, tasks/current.md, GC_ROUTINE
2026-05-15 | ops   | feat: guard candidate/exit scheduler on market data readiness (automationReady=false blocks run)
2026-05-15 | ops   | chore: normalize paper run scripts — run-paper-jpa.* as primary, run-upbit-paper-jpa.* as alias
2026-05-14 | analytics | feat: add strategy performance analytics — winRate, averageHoldingSeconds, profitLossRatio
2026-05-14 | smoke | test: add PAPER position close smoke tests (take-profit / stop-loss / missing price)
2026-05-14 | ws    | test: harden WebSocket reconnect/backoff unit tests
2026-05-14 | ux    | feat: add PAPER cash warning status to /api/system/status
2026-05-14 | safety| feat: harden data-independent operations — runtime gateway guard, security lint expansion
2026-05-14 | docs  | docs: document Claude feedback roadmap
2026-05-14 | safety| feat: harden paper audit (paper_trade_log), block telegram manual execution at code level
2026-05-14 | ux    | feat: prepare Candidates/History for large data — server limit 200, search-on-submit
2026-06-08 | ops   | fix: restore local env, verify app/web Docker stack, harden frontend UI tests, and require web rebuild on session end
2026-06-12 | research | feat: add candle cache, backtest leaderboard, and three rejected strategy candidates
2026-06-12 | research | feat: add rejected oversold mean-reversion strategy candidate
2026-06-12 | research | feat: add rejected BTC market regime strategy candidate and fixed top-30 universe policy
2026-06-12 | research | chore: harden candle collector and document partial top-30 universe collection
2026-06-15 | research | chore: complete top-30 Upbit/Binance candle collection
2026-05-14 | ux    | chore: reduce frontend API polling load, stop background tab polling
2026-05-14 | docs  | docs: align Risk and System page docs with code
2026-05-14 | ux    | test: add Playwright UI regression tests (desktop/mobile)
2026-05-14 | ux    | perf: lazy load frontend routes (reduce initial bundle ~795kB→~332kB)
2026-05-14 | ux    | chore: clean up redesigned UI styles, fix mobile overflow
2026-05-14 | ux    | feat: harden Risk/System pages as read-only policy view
2026-05-14 | ux    | feat: clarify Candidate/History audit UI (decision/reason/risk flag)
2026-05-14 | ux    | feat: strengthen Portfolio PAPER SELL UX (confirm modal, exposure badges)
2026-05-14 | ux    | feat: rework Dashboard as control room (readiness, risk summary, PnL)
2026-05-14 | ux    | feat: UI redesign Stage 1–6 — App Shell, Sidebar, Top Status Bar, route restructure
2026-05-13 | risk  | feat: add concentration risk (쏠림) BUY block + stop-loss cooldown
2026-05-13 | risk  | feat: add concentration warning UI to Dashboard and Portfolio
2026-05-13 | risk  | docs: document market concentration risk policy and data snapshot
2026-05-13 | ops   | feat: JPA PAPER accumulated run scripts + schema apply scripts
2026-05-07 | safety| feat: add PAPER trade log (append-only, FILLED BUY/SELL only)
2026-05-06 | ux    | feat: add OS-aware system guide to Dashboard and System page
2026-05-05 | ws    | feat: add Binance WebSocket ticker client + SNAPSHOT provider
2026-05-05 | ws    | feat: add Upbit WebSocket ticker client with snapshot store
2026-05-05 | ws    | feat: add /api/market-provider/status with WebSocket snapshot counts
2026-05-04 | paper | chore: first JPA PAPER run and condition record
2026-04-30 | exit  | feat: add exit scheduler (position take-profit/stop-loss auto SELL)
2026-04-30 | exit  | feat: add candidate scheduler (periodic BUY candidate scan + execute)
2026-04-30 | risk  | feat: add daily risk validation (order limit, loss limit)
2026-04-30 | risk  | feat: add concentration risk validation service
2026-04-29 | tg    | feat: extract Telegram update offset repository (JPA + InMemory)
2026-04-27 | ops   | feat: add system/risk/strategy/market-provider status APIs
2026-04-27 | tg    | feat: add Telegram inbound command polling, inline button handling
2026-04-27 | tg    | feat: add Telegram notification sender
2026-04-27 | db    | feat: add JPA history repository, PostgreSQL docker setup
2026-04-27 | mvp   | feat: initial PAPER trading MVP — strategy, execution, portfolio, history, notification
2026-06-15 | paper | feat: add Binance session volatility breakout PAPER candidate scanner
2026-06-15 | ops | feat: expose scheduler readiness warnings for PAPER observation
2026-06-15 | ops | feat: add session volatility PAPER observation run profile
2026-06-16 | ops | chore: tune session volatility observation scheduler intervals
2026-06-16 | ops | docs: record session volatility PAPER observation startup
2026-06-16 | ops | chore: allow Docker Compose session volatility observation profile overrides
