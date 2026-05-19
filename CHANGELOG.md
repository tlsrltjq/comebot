# 변경 이력

## 형식
`날짜 | 단계 | feat/fix/chore/docs: 내용`

---

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
