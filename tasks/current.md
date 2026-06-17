# tasks/current.md — 현재 작업 컨텍스트

## 단계

PAPER 후보 전략 구현 — Session Volatility Breakout을 운영 후보 스캐너에 연결하고 관찰 운영 준비 단계로 전환.

## 현재 목표

Binance 15m UTC 06-12 Session Volatility Breakout 후보를 `PAPER_TRADING` 경로에서 조회/실행 가능하게 만들고,
스케줄러 기본 OFF 상태에서 관찰 운영 전 설정/문서/테스트를 정리한다.

## 현재 상태 요약

- 검증 도구: `src/test/.../backtest/` 운영 엔진 리플레이 parity 하니스 (opt-in 플래그로 실행, 기본 suite skip)
  - `-Dbacktest.run`(parity), `-Dbacktest.sweep`(TP×SL), `-Dbacktest.entrysweep`(진입), `-Dbacktest.regime`(레짐), `-Dbacktest.mtf`(다중TF)
  - 결정론적 엔진 단위테스트 6종은 기본 suite 상시 실행(회귀 가드)
- 지표: PFgross(비용 전=신호 엣지) 1차 게이트, PFnet train/test OOS 채택 게이트
- **메타 결론(180일 Upbit 캐시)**: 청산 TP×SL·진입 변형·레짐 게이트·다중TF 모든 레버가 gross PF 0.94~0.97에 수렴
  → V1 풀백 반등군은 엣지 없음. 필터는 음의 엣지를 다듬을 뿐 부호를 못 바꿈. 방향성 필터 2개+ 스택은 OOS 붕괴.
- 최선 방어 필터(수익 전환 아님): M1(coin1h==UP)·F1(BTC≠DOWN) — MDD 5.0→2.5% 반감
- 운영 봇: `PAPER_TRADING` 전용 유지, 실제 주문 API/`REAL_TRADING` 금지 불변
- 운영 상태: 새 전략 후보 전까지 candidate/exit scheduler 기본값 OFF, 관찰/대시보드 전용
- 확장 유니버스 생존 후보:
  - Ranked Rotation: 기본 비용에서는 후보가 있으나, 보수 비용/슬리피지 스트레스에서는 전부 net weak로 강등.
  - Session Volatility Breakout: Binance 15m UTC 06-12 대표 후보가 15m 신호 + 5m/1m 5분 유효 maker 감사 통과. no-pegged-event는 train gross < 1.10으로 탈락, no-event/no-pegged는 stress 비용에서도 후보 유지.
- 운영 코드 반영:
  - `SESSION_VOLATILITY_BREAKOUT` 전략 선택값 추가.
  - 선택 시 `CandidateScannerService`가 Session Volatility Breakout 스캐너로 위임하므로 후보 API/후보 실행/스케줄러 실행 경로가 같은 조건을 사용한다.
  - 기본 설정값은 변경하지 않았다. 실제 활성화는 `STRATEGY_SELECTED=SESSION_VOLATILITY_BREAKOUT` 같은 외부 설정으로만 수행한다.
  - candidate/exit scheduler는 계속 OFF가 기본이다.
  - `/api/scheduler/status`, `/api/system/status`가 `candidateReadinessWarnings`를 반환한다. Session Volatility Breakout 선택 시 Binance 전용, USDT market, 초기 max BUY 1, exit scheduler ON/Binance 포함 여부를 경고한다.
  - `scripts/run-session-volatility-paper-jpa.sh`는 `.env`를 수정하지 않고 Session Volatility Breakout 관찰 프로필로 기동한다. 시작 시 scheduler는 OFF이고, persisted scheduler restore도 비활성화한다.
  - `scripts/resume-paper-auto.sh`는 `candidateReadinessWarnings=[]`가 아니면 scheduler를 켜지 않는다.
  - 관찰 프로필 추천 주기: candidate 30초, exit 5초.
  - Docker Compose는 `.env` 수정 없이 환경변수 prefix로 Session Volatility 관찰값을 주입해 app/web을 재기동한다.

## 완료된 최근 작업

- 전략 리서치 계획 문서 추가: `tasks/strategy-research-plan.md`
- 사용자 결정 반영: 공격적 수익률 탐색, Upbit+Binance 동시 확장, 1m/3m/5m/15m 원본 수집, 추천 전략 순서, 관찰 전용 운영
- 시드 데이터 수집: `.backtest_cache`에 Upbit `KRW-BTC`,`KRW-ETH` + Binance `BTCUSDT`,`ETHUSDT` 1년치 1m/3m/5m/15m 원본 캔들 저장
- 데이터 품질 확인: Binance 기준봉 간격 연속, Upbit 일부 gap 존재
- 리더보드 기반 추가: `BacktestLeaderboardRow/Writer`로 CSV/Markdown 공통 산출 구조와 단위 테스트 추가
- 실험 기준 고정: `BacktestSplitPolicy` last-60d 단일 OOS, `BacktestDecisionPolicy` 탈락/후보 판정 기준 코드화
- 상대강도 모멘텀 1차 실험: BTC/ETH 시드 기준 72개 조합 모두 탈락, 생존 후보 0
- 거래대금 추세 지속 1차 실험: BTC/ETH 시드 기준 216개 조합 모두 탈락, 생존 후보 0
- 변동성 수축 후 돌파 1차 실험: BTC/ETH 시드 기준 216개 조합 모두 탈락, 생존 후보 0
- 과매도 평균회귀 1차 실험: BTC/ETH 시드 기준 216개 조합 모두 탈락, 생존 후보 0
- 유니버스 확장 기준 고정: Upbit KRW 24h 거래대금 상위 30 + Binance spot USDT quote volume 상위 30
- BTC/마켓 레짐 모멘텀 1차 실험: BTC/ETH 시드 기준 432개 조합 모두 탈락, 생존 후보 0
- 상위 30 확장 수집 완료: Upbit 30 + Binance 30, 1m/3m/5m/15m 원본 캔들 총 240개 JSON, `.backtest_cache` 약 9.0GB
- 종목 랭킹 기반 로테이션: 확장 유니버스 288개 조합 중 Binance 15m 6개 약한 PAPER 관찰 후보 산출. Upbit은 train gross edge 부족으로 전부 탈락
- 백테스트 로더 보정: Binance 유니버스에 Upbit `KRW-USDT`가 섞이지 않도록 exchange 필터 회귀 테스트 추가
- Ranked Rotation 정밀 검증: 후보 6개를 비용 3종 x 유니버스 4종으로 재검증. 기본 비용에서는 16개 후보/관찰 후보가 남지만, 보수/스트레스 비용에서는 모든 조합이 `watch:*net-weak` 또는 탈락
- Session Volatility Breakout: 확장 유니버스 640개 조합 중 `candidate:paper-observation` 6개, `candidate:weak-paper-observation` 7개. 핵심 구간은 Binance 15m UTC 06-12, Binance 3m UTC 12-18/18-24
- Session Volatility Breakout 정밀 검증: 후보 13개 x 비용 3종 x 유니버스 4종 총 156개 시나리오. stress 비용에서도 candidate 7개 유지, 핵심은 Binance 15m UTC 06-12
- Session Volatility Breakout maker 1차 감사: 신호 close 지정가, 다음 15m 캔들 low 체결, 1 candle 유효 모델에서 12개 시나리오 모두 후보 유지. no-pegged-event + stress test PFnet 1.154, 체결률 100%
- Session Volatility Breakout 하위 캔들 maker 감사: 15m 신호 후 300초 안에 5m/1m low가 limit price를 터치하는지 검증. 5m/1m 결과 동일, fillRate 98.8~99.2%, no-event + stress weak 후보, no-pegged + stress paper 후보 유지
- Session Volatility Breakout 운영 후보 스캐너 구현: Binance 전용, 15m 신호, UTC 06-12, breakout=20, average=60, minRangeRatio=2.5, minVolumeRatio=1.5, minCloseLocation=70.0, no-pegged 기본 제외. `./gradlew test checkstyleMain` 통과
- Session Volatility Breakout scheduler readiness 경고 추가: 상태 API에서 잘못된 거래소/market/max BUY/exit scheduler scope를 표시. `./gradlew test checkstyleMain` 통과
- Session Volatility Breakout PAPER 관찰 실행 프로필 추가: 전용 run script, persisted scheduler restore 비활성화 옵션, resume 전 readiness 가드. `./gradlew test checkstyleMain` 통과
- Session Volatility Breakout 관찰 프로필 주기 조정: candidate 30초 유지, exit 5초로 완화
- 수집기 안정화: 진행률 로그, Upbit 상장 이전 cursor 가드, HTTP 재시도 10회, `Connection: close`, 최대 60초 backoff 추가
- maker 지정가 진입 구현: pending order 생성, fresh fill, risk+portfolio 검증
- 2차 감사 수정: `firstCheckAt` 과보수 제거, stale price 체결 가드 추가
- 단위 테스트 6종: same-candle 0건, stale 거부, 정상 체결, 중복 방지, 교차 격리
- ADR-011: trailing 제거 + TP/SL 4:2 청산 구조
- ADR-012: 신규 진입 필터 OOS 게이트 실패, 현행 1m×20 유지
- ADR-013: maker 진입은 PAPER 관찰 후 결정

## 다음 액션 (나중에 할 일 — 등록됨)

1. 첫 24시간 후보/체결/손익 관찰 로그를 `docs/trading/condition-records/`에 새 파일로 추가한다.
2. 다음 15:00-21:00 KST(UTC 06-12) 신규 진입 세션의 후보/체결/미체결을 확인한다.
3. 관찰 결과에 따라 Binance `ALL_USDT` 유지, market 축소, 또는 scheduler OFF를 결정한다.
4. 주식 PAPER 확장 계획(`tasks/stock-expansion-plan.md`)에 따라 미국 주식 데이터 provider 후보와 모델링 방식을 결정한다.
5. PAPER 관찰 시작 후 `.backtest_cache` prune/delete 범위를 정한다. 이때 crypto/stock 캐시 분리 계획도 함께 반영한다.

## 중단/탈락 기준 (전략 실험)

- PFgross가 train에서 1.0 미만이면 신호 엣지 없음 → 폐기
- train만 좋고 test 붕괴(|train−test| 큼) → 과적합 탈락
- 거래 수 과소(표본 빈약) 또는 1~2 코인 의존 → 탈락
- 방향성 필터 2개+ 단순 스택은 OOS 붕괴 확인됨 → 지양

## 완료 기준

- `./gradlew test checkstyleMain` 통과
- 프론트 변경 시 `cd frontend && npm run lint && npm run build && npm test` 통과
- 실제 주문 API, `REAL_TRADING`, 수동 BUY UI 없음

## 참고

- 프로젝트 원칙: `HARNESS.md`
- 상세 구조/정책: `docs/architecture.md`, `docs/spec.md`, `docs/decisions.md`
- 운용 기록: `docs/trading/condition-records/`

## 이전 세션에서 멈춘 곳

2026-06-08 — 노트북 이전 후 로컬 운영 환경 복구 + 프론트 검증 보정.
- `.env`를 사용자 예외 승인으로 재생성: `POSTGRES_PASSWORD=comebot`, PostgreSQL 5433, backend 18080, web 5176, Telegram off.
- Docker stack 정상화: `comebot-postgres`, `comebot-app`, `comebot-web` 기동 및 `/api/system/status` 정상 확인.
- 프론트 검증 실패 보정:
  - stale E2E heading 기대값을 현재 UI 문구로 갱신.
  - 모바일 root overflow를 막도록 shell/status bar/page 공통 CSS 보정.
  - Portfolio 선택 PAPER SELL 확인 문구와 lint 미사용 항목 정리.
- 검증 통과: `./gradlew test checkstyleMain`, frontend `lint/build/test/test:e2e`.
2026-06-12 — 거래대금 상위 30 고정 유니버스 수집 중간 중단.
- 완료 캐시: 기존 BTC/ETH 16개 + `KRW-WLD` 1m/3m/5m/15m 4개 = 총 20개 JSON, `.backtest_cache` 약 955MB.
- 미완료: `KRW-XRP` 1m 수집 중 중단. 파일 단위 저장 전이므로 다음 실행 때 처음부터 다시 받는다.
- 다음 재개 명령:
  `python3 scripts/collect-backtest-candles.py --since 2025-06-12T00:00:00Z --until 2026-06-12T00:00:00Z --units 1,3,5,15 --upbit-markets ALL_KRW --binance-symbols ALL_USDT --upbit-top 30 --binance-top 30 --request-delay-sec 0.5`
- 재개 후 완료 파일은 자동 skip된다. 전체 수집 완료 전 `manifest.json`은 이전 시드 상태일 수 있다.
2026-06-15 — 거래대금 상위 30 고정 유니버스 수집 완료.
- 완료 캐시: Upbit 30 + Binance 30, 1m/3m/5m/15m 총 240개 JSON, `.backtest_cache` 약 9.0GB.
- `manifest.json` 수집 완료 시각: 2026-06-15T00:58:53Z.
- 종목 랭킹 기반 로테이션 후보 구현 및 리더보드 산출.
- 산출: 288개 조합 중 Binance 15m rankCount=1/rebalance=20 조합 6개가 `candidate:weak-paper-observation`.
- 대표 후보: lookback=60,minReturn=0.5, full PFgross/PFnet 1.163/1.009, train 1.116/0.968, test 1.282/1.114, full trades 2415, topMarket ZECUSDT 14.7%.
- 정밀 검증: 72개 비용/유니버스 시나리오 산출. 기본 비용에서는 no-zec 조합도 후보가 남지만, 보수 비용에서는 전부 net weak로 강등.
- 시간대/세션별 변동성 돌파 후보 구현 및 리더보드 산출.
- 산출: 640개 조합 중 후보 13개. Binance 15m UTC 06-12, Binance 3m UTC 12-18/18-24에 집중.
- 대표 후보: Binance 3m UTC 12-18 breakout=60,avg=20,minRangeRatio=1.5,minVolRatio=3.0, full PFgross/PFnet 1.263/1.040, train 1.216/1.000, test 1.497/1.234, trades 569, topMarket TRXUSDT 10.5%.
- 정밀 검증: 156개 비용/유니버스 시나리오 산출. 보수 비용 후보 10개, stress 비용 후보 7개 유지.
- 1순위 후보: Binance 15m UTC 06-12, breakout=20,avg=60,minRangeRatio=2.5,minVolRatio=1.5. no-pegged-event + stress에서도 full PFgross/PFnet 1.207/0.873, train 1.120/0.807, test 1.576/1.153.
- 15m coarse maker 감사: 신호 close 지정가, 다음 15m 캔들 low 체결, 1 candle 유효. no-pegged-event + stress full PFgross/PFnet 1.192/0.862, train 1.102/0.793, test 1.577/1.154, signals/fills/expiries 247/247/0.
- 하위 캔들 maker 감사: 15m 신호 후 300초 안의 5m/1m low 터치 기준. no-event + stress full PFgross/PFnet 1.216/0.847, train 1.116/0.778, test 1.730/1.194, fillRate 98.84%, weak 후보. no-pegged + stress full 1.220/0.884, train 1.168/0.843, test 1.407/1.033, fillRate 98.90%, paper 후보.
- no-pegged-event는 test PFnet은 좋지만 train PFgross 1.082로 `reject:weak-train-gross-edge`.
- 캐시 상태: `.backtest_cache` 8.9GB, JSON 240개 + manifest. PAPER 후보 구현 후 prune/delete 계획 실행.
- 다음 세션: Session Volatility Breakout PAPER 전략 구현 범위 확정 및 코드 구현 시작.
2026-06-15 — Session Volatility Breakout PAPER 후보 스캐너 구현.
- 코드 연결: `StrategyType.SESSION_VOLATILITY_BREAKOUT`, `SessionVolatilityBreakoutScannerService`, `SessionVolatilityBreakoutProperties` 추가.
- 선택 전략이 `SESSION_VOLATILITY_BREAKOUT`이면 `CandidateScannerService`가 새 스캐너로 위임한다. 후보 조회 API, 후보 실행, 스케줄러 실행 경로가 같은 조건을 사용한다.
- 기본 조건: Binance 전용, 15m, UTC 06-12, breakout=20, avg=60, minRangeRatio=2.5, minVolumeRatio=1.5, minCloseLocation=70.0, no-pegged(`USD1USDT`,`USDCUSDT`,`USDEUSDT`,`XAUTUSDT`) 제외.
- 기본값은 바꾸지 않음: `STRATEGY_SELECTED` 기본은 `SIMPLE_THRESHOLD`, candidate/exit scheduler 기본 OFF.
- 검증: `./gradlew test checkstyleMain` 통과.
- 다음 세션: PAPER 관찰 운영 설정값 확정 후 스케줄러 ON 범위/시장 목록을 제한적으로 적용.
2026-06-15 — Session Volatility Breakout scheduler readiness 경고 추가.
- `/api/scheduler/status`와 `/api/system/status`에 `candidateReadinessWarnings` 추가.
- `SESSION_VOLATILITY_BREAKOUT` 선택 시 candidate exchanges가 BINANCE 전용인지, candidate markets가 ALL_USDT/USDT 심볼인지, 초기 maxBuysPerRun이 1인지, candidate ON 시 exit scheduler가 ON이고 BINANCE를 포함하는지 경고한다.
- 이 경고는 자동 제어가 아니라 상태 표시다. scheduler 기본 OFF와 기본 전략값은 변경하지 않았다.
- 검증: `./gradlew test checkstyleMain` 통과.
- 다음 세션: 외부 설정 적용 후 readiness 경고가 빈 배열인지 확인하고 제한적으로 PAPER 관찰 운영 시작.
2026-06-15 — Session Volatility Breakout PAPER 관찰 실행 프로필 추가.
- `scripts/run-session-volatility-paper-jpa.sh/.bat` 추가. `.env` 수정 없이 `SESSION_VOLATILITY_BREAKOUT`, Binance `ALL_USDT`, maxBuysPerRun=1, exit BINANCE로 실행한다.
- 실행 프로필은 candidate/exit scheduler를 시작 시 OFF로 강제하고, `SCHEDULER_CONTROL_RESTORE_ENABLED=false`로 이전 DB 자동매매 ON 상태 복원을 막는다.
- `scripts/resume-paper-auto.sh/.bat`는 `/api/system/status`의 `candidateReadinessWarnings=[]`를 확인한 뒤에만 scheduler를 켠다.
- 검증: `bash -n scripts/run-paper-jpa.sh scripts/run-session-volatility-paper-jpa.sh scripts/resume-paper-auto.sh`, `./gradlew test checkstyleMain` 통과.
- 다음 세션: 전용 프로필로 백엔드 기동, provider readiness 확인, scheduler 제한 ON, 첫 관찰 기록 추가.
2026-06-16 — Session Volatility Breakout 관찰 주기 조정.
- 현재 확인값: candidate 30초, exit 1초.
- 판단: 15m 신호 후보 스캔은 30초 유지, 포지션/체결 확인은 관찰 운영 부하를 줄이기 위해 exit 5초로 완화.
- 전용 프로필에 `TRADING_EXIT_SCHEDULER_FIXED_DELAY_MS=5000` 추가.
2026-06-16 — Session Volatility Breakout PAPER 관찰 운영 시작.
- 전용 프로필로 백엔드 기동 완료: `scripts/run-session-volatility-paper-jpa.sh`.
- readiness/provider 확인: `candidateReadinessWarnings=[]`, market provider `automationReady=true`.
- `scripts/resume-paper-auto.sh`로 candidate/exit scheduler 제한 ON.
- 현재 운영값: `candidateEnabled=true`, `candidateFixedDelayMs=30000`, `candidateMarkets=[ALL_USDT]`, `candidateExchanges=[BINANCE]`, `exitEnabled=true`, `exitFixedDelayMs=5000`, `exitExchanges=[BINANCE]`.
- Binance PAPER 스냅샷: cash `987.102394939955400000 USDT`, realizedProfit `17.102396003703575300 USDT`.
- 이전 Binance 포지션 3개(`ZECUSDT`, `WLDUSDT`, `HOMEUSDT`)는 exit scheduler가 SELL FILLED로 정리. 남은 포지션은 `DASHUSDT` 1개.
- 현재 시간대는 KST 21시 이후라 신규 진입 세션(UTC 06-12, KST 15-21)은 거의 종료. 다음 유의미한 신규 진입 관찰은 다음 KST 15:00부터.
- 다음 세션: 첫 관찰 기록 파일 추가, 24시간 후보/체결/손익 확인, cache prune/delete 범위 결정.
