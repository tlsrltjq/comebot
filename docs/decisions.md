# Architecture Decision Records (ADR)

기술 결정 이유를 기록한다.
에이전트가 "왜 이렇게 구현했지?"를 파악하고 다른 방향으로 리팩토링하는 것을 방지한다.

---

## ADR-001: PAPER_TRADING 전용 — 실제 주문 없음

- **결정**: `REAL_TRADING` 구현과 실제 거래소 주문 API 호출을 영구 금지한다.
- **이유**: 전략 검증 목적 봇이며 실제 자산 손실 위험을 제거하는 것이 최우선.
  Upbit/Binance 공개 시세는 사용하지만 인증키(Access Key, Secret Key)는 사용하지 않는다.
- **트레이드오프**: 실제 수익을 올릴 수 없다. PAPER 체결가와 실제 시장가 슬리피지를 반영하지 못한다.
- **재검토 조건**: 이 결정은 재검토하지 않는다. 실제 주문이 필요하면 별도 프로젝트로 분리한다.

---

## ADR-002: Upbit(KRW) + Binance(USDT) 이중 거래소, 포트폴리오 분리

- **결정**: `ExchangeMode.UPBIT`과 `ExchangeMode.BINANCE`를 분리한다.
  포트폴리오, 포지션, history, analytics는 거래소별로 독립 저장한다.
- **이유**: KRW와 USDT는 환산 없이 비교할 수 없다. 거래소별 슬리피지, 유니버스, 전략 결과가 다르므로 섞으면 손익 분석이 의미 없어진다.
- **트레이드오프**: 구현 복잡도 증가. 같은 market이 두 거래소에 있을 때 중복 관리.
- **재검토 조건**: 세 번째 거래소 추가 시 공통 추상화 도입 여부 검토.

---

## ADR-003: WebSocket + REST fallback — SNAPSHOT provider

- **결정**: `MarketPriceProviderType.SNAPSHOT` 설정 시 WebSocket snapshot을 우선 사용하고, stale/missing이면 거래소별 REST provider로 fallback한다.
  기본 설정은 WebSocket disabled(REST only).
- **이유**: Upbit/Binance WebSocket은 공개이며 낮은 지연으로 시세를 수신한다. REST fallback은 WebSocket 장애 시 주문 차단 없이 운영을 유지하기 위한 안전망.
- **트레이드오프**: SNAPSHOT provider는 fresh snapshot이 0개이면 candidate/exit scheduler 실행을 차단한다. REST-only 설정보다 초기 설정 복잡도가 높다.
- **재검토 조건**: Upbit/Binance가 WebSocket 요금 정책을 변경하거나, REST-only 운영에서 rate limit 문제가 반복되면 재검토.

---

## ADR-004: JPA + InMemory 이중 저장소, 환경변수로 선택

- **결정**: history, portfolio, Telegram offset 저장소를 `HISTORY_STORAGE_TYPE`, `PAPER_PORTFOLIO_STORAGE_TYPE`, `TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE` 환경변수로 JPA/InMemory 중 선택한다.
  기본값은 InMemory(재시작 시 초기화), JPA는 PostgreSQL 필요.
- **이유**: 단기 smoke 실행은 DB 없이 InMemory로 간단하게, 장기 PAPER 운용은 JPA로 재시작 후에도 데이터 유지.
- **트레이드오프**: 런타임 분기로 인해 코드 경로 두 배. 저장소 타입이 다른 상태로 두 인스턴스를 동시에 올리면 데이터 불일치.
- **재검토 조건**: Redis 같은 외부 캐시 도입이 필요한 규모가 되면 재검토.

---

## ADR-005: React + Vite 프론트엔드, 별도 프로세스

- **결정**: 웹 UI를 `frontend/` 하위 Vite React 앱으로 분리하고, Spring Boot와 별도 프로세스로 실행한다.
  웹은 기존 Spring REST API만 호출하고 비즈니스 로직을 중복 구현하지 않는다.
- **이유**: 프론트와 백엔드 독립 개발, 린트와 테스트 분리. Spring Static Resource로 묶으면 빌드 의존성이 복잡해진다.
- **트레이드오프**: 로컬 실행 시 두 프로세스 필요 (`run-local-dev.sh`로 자동화).
  프론트엔드 빌드 없이는 웹 UI를 사용할 수 없다.
- **재검토 조건**: 팀 규모가 커지고 SSR이 필요해지면 Next.js 전환 검토.

---

## ADR-006: 롱 전용 변동성 돌파 전략 (VolatilityBreakoutLong)

- **결정**: 기본 전략은 `VolatilityBreakoutLongStrategy`다. 숏, 마진, 레버리지는 구현하지 않는다.
  후보 선정 조건: 추세 UP, 마지막 캔들 양봉(`lastCandleBullish`), 가격 변화율(`priceChangeRate` ≥ 1%), 거래대금 증가율(`tradeAmountChangeRate` ≥ 30%), 과열 회피, 최근 고점 대비 거리(`distanceFromHighRate` ≤ 2%), 최소 캔들 거래대금(`minLatestCandleTradeAmount`).
  1회 스케줄 주기당 최대 BUY 수는 `max-buys-per-run`(기본 2)으로 제한한다.
- **이유**: 변동성이 검증된 코인의 단기 상승 모멘텀을 포착하는 것이 PAPER 환경에서 빠르게 가설을 검증하기 좋다. 숏 전략은 리스크 복잡도가 크게 올라간다.
  `lastCandleBullish`: 직전 캔들이 음봉이면 모멘텀 약화 신호이므로 진입 차단.
  `distanceFromHighRate`: 최근 5분 고점에서 이미 많이 내려온 경우 추세 반전 후 재진입이어서 리스크가 높다.
  `minLatestCandleTradeAmount`: 상대적 증가율(tradeAmountChangeRate)만으로는 기저 거래대금이 너무 낮아도 통과될 수 있다. 절댓값 필터로 실질 유동성 하한을 보장한다.
  `max-buys-per-run`: 동일 주기에 여러 BUY가 실행되면 포지션 집중도가 급격히 올라가고 stop-loss cooldown을 동시에 트리거할 위험이 있다.
- **트레이드오프**: 하락장에서 후보가 거의 없어 자동 매수가 드물다. 전략 단순화로 실제 알파 포착 능력이 제한될 수 있다.
- **재검토 조건**: 장기 PAPER 손익비(profitLossRatio)가 1 미만으로 유지되면 전략 조건 조정 검토. 숏은 별도 전략 모듈로만 검토.

---

## ADR-007: 스케줄러 기반 자동화, UI/Telegram 수동 실행 금지

- **결정**: BUY/SELL 자동 실행은 candidate scheduler와 exit scheduler가 전담한다.
  웹 UI에는 수동 BUY, 후보 실행, trading-flow 실행 버튼을 제공하지 않는다.
  Telegram `/run`, `/candidate-run` callback은 코드 레벨에서 실행 서비스를 호출하지 않는다.
  유일한 예외: 웹 포트폴리오에서 사용자가 명시 선택한 보유 PAPER 포지션 SELL.
- **이유**: 수동 실행 경로를 열면 안전 검증(kill switch, risk, portfolio) 우회 가능성이 생긴다. 스케줄러가 단일 진입점이어야 일관성 있는 history와 portfolio를 유지할 수 있다.
- **트레이드오프**: 긴급 매수를 즉시 실행할 수단이 없다. 후보가 나타나도 다음 scheduler 주기(최소 30초)를 기다려야 한다.
- **재검토 조건**: Telegram을 통한 검증된 수동 PAPER BUY가 명확히 필요해지면 `telegram.inbound.manual-paper-execution-enabled=true` 경로를 별도 ADR로 검토.

---

## ADR-008: Kill Switch + Risk Validation 순서

- **결정**: 모든 거래 실행 흐름에서 kill switch → 리스크 검증 → 포트폴리오 검증 순서를 지킨다.
  kill switch가 켜져 있으면 시세 조회 이전에 차단한다.
- **이유**: kill switch가 시세 조회 이후에 있으면 시세 API 호출이 먼저 발생해 장애 상황에서 불필요한 외부 요청이 쌓인다. 리스크 검증 실패 시 ExecutionGateway를 호출하면 안 된다는 원칙을 코드 구조로 강제.
- **트레이드오프**: kill switch 체크 레이어가 여러 서비스에 중복 삽입될 수 있다.
- **재검토 조건**: kill switch 체크 로직을 AOP나 공통 필터로 추출할 필요가 생기면 재검토.

---

## ADR-009: 후보 스캔 결과 별도 테이블 기록 (candidate_scan_log)

- **결정**: SELECTED/SKIPPED 포함 모든 후보 스캔 결과를 `candidate_scan_log` 테이블에 append-only로 기록한다.
  `paper_trade_log` 확장이나 `trading_flow_history` 확장을 선택하지 않았다.
- **이유**:
  `paper_trade_log`는 FILLED 체결 원장이며 SKIPPED 결과를 포함하면 테이블 의미가 훼손된다.
  `trading_flow_history`는 주문 실행 결과 중심이라 스캔 단계의 SKIPPED 이유 필드와 구조가 맞지 않는다.
  별도 테이블이면 스캔 로그와 주문 이력을 독립적으로 쿼리하고 각각 보존 정책을 달리 적용할 수 있다.
- **트레이드오프**: InMemory 저장소는 ring buffer(max 10,000건)로 제한하고, JPA 저장소는 외부 DB 비용이 추가된다.
  스키마 분리로 조인 없이 스캔 로그만 조회할 수 있지만, 같은 market의 스캔 결과와 체결 이력을 연결하려면 애플리케이션 레이어에서 market+time으로 매칭해야 한다.
- **재검토 조건**: 스캔 로그가 대용량이 되어 별도 시계열 DB가 필요해지면 재검토.

---

## ADR-010: 전략 개편 — 펌프 추격 → 눌림목 반등 진입 (2026-05-22)

- **배경**:
  기존 전략의 수학적 기댓값 분석 결과:
  `E[trade] = 0.35 × 1.5% − 0.65 × 0.7% − 0.1% − 0.3% = −0.33%/거래`
  원인:
  1. "거래대금 20배 폭발 + 고가 2% 이내" = 펌프 꼭지 추격 매수 → 승률 구조적 30~40%
  2. SL 0.7% = 펌프 직후 자연 되돌림(1~2%)에 즉시 손절
  3. BTC 추세 무관 진입 → 알트 페이크 펌프 다수 편입
  4. 피라미딩(+0.5% 후 추가 진입)이 평균단가 올려 SL 확률 상승

- **결정**:
  1. **진입 조건 전면 개편** (펌프 추격 → 눌림목 반등)
     - 10캔들 윈도우 내 `windowHighChangeRate ≥ minPriceChangeRate` 로 펌프 발생 확인
     - `peakTradeAmountChangeRate ≥ minTradeAmountChangeRate` 로 거래량 폭발 확인
       (최신 캔들 기준이 아닌 윈도우 내 피크 기준 → 눌림목 구간에서도 선택 가능)
     - `distanceFromHighRate ∈ [0.5%, 2%]` 로 눌림목 구간 확인
     - `lastCandleBullish = true` 로 반등 캔들 확인
     - `priceChangeRate < 0` → weak filter ("Trend is not UP", 순수 하락 시장 차단)
  2. **BTC 추세 필터 추가** (`BtcTrendCacheService`)
     - KRW-BTC 1h 캔들 20개로 EMA5 vs EMA10 비교, 5분마다 갱신, 앱 시작 시 bootstrap
     - UPBIT 마켓: BTC 추세가 DOWN이면 진입 차단
  3. **R:R 구조 수정** (.env)
     - TP: 1.5% → 2.5%, SL: -0.7% → -1.5%
     - Trailing stop: activation 1.5%, trail 0.8%
     - 피라미딩 비활성화 (`PREVENT_REENTRY_WITH_POSITION=true`)
  4. **캔들 윈도우 확대**: 5개 → 10개 (펌프+눌림 패턴 캡처)

- **목표 기댓값**:
  `E[trade] = 0.50 × 2.5% − 0.50 × 1.5% − 0.1% − 0.05% = +0.35%/거래`

- **트레이드오프**:
  진입 빈도가 줄어든다 (눌림목 조건이 더 까다롭다).
  BTC 필터로 알트 상승장에서 기회를 놓칠 수 있다 (BTC 하락 중 알트 강세 구간).
  파라미터(activation/trail/거리 범위)는 실운영 데이터로 지속 조정이 필요하다.

- **재검토 조건**:
  2주 이상 운영 후 SELECTED 비율과 실현 수익률을 분석해 파라미터 재조정.
  BINANCE 마켓에도 BTCUSDT 기반 추세 필터 적용 필요 시 별도 ADR.

---

## ADR-011: 전략 구조 재설계 — 파라미터 튜닝 중단, OOS 게이트 도입 (2026-06-02)

- **배경**:
  ADR-010 눌림목 전략을 train/test 분리(walk-forward, 120d/60d)로 검증한 결과
  **비용 차감 후 검증된 엣지가 없음**을 확인했다 (condition-records/2026-06-02-oos-validation-and-edge-analysis.md).
  - train PF 0.896 / test PF 0.785 (두 기간 모두 손실)
  - 시간대 필터·진입조건 스윕 어떤 조합도 test PF < 1.0
  - 근본 원인: 거래당 gross edge +0.066% < 왕복 수수료 0.1%

- **결정**:
  1. **파라미터 튜닝 중단.** 진입 임계값 미세조정은 in-sample 과최적화만 유발.
  2. **구조적 레버 3개를 OOS로 실험**한 뒤 채택:
     - 청산 재설계: 현 trailing이 승자를 +1.1%로 캡 → 승자 run 허용(넓은 TP / 시간기반 청산)
     - 신호 timeframe: 1분봉(노이즈) → 5/15분봉
     - 비용: 거래 빈도 축소로 gross edge가 왕복 0.1%를 넘게
  3. **OOS 게이트 (채택 기준)**: 어떤 구조 변경도 train에서 도출 → test에서
     `test PF ≥ 1.05` 且 train/test PF 괴리 < 0.15 를 만족해야 프로덕션 반영.
     충족 못 하면 채택하지 않고 PAPER 유지.

- **방법론**:
  - 모든 실험은 로컬 `bt_*.py`(gitignored) + 캐시 1분봉 기반.
  - 상위 timeframe은 1분봉 OHLCV 리샘플링으로 생성.
  - 검증 통과한 구조만 소스 반영 + condition-record 기록.

- **트레이드오프**:
  진입 빈도가 크게 줄어 자금 유휴가 늘 수 있음(단, 음의 엣지로 매매하는 것보다 나음).
  상위 timeframe은 반응 지연 → 빠른 청산 기회 상실 가능.

- **재검토 조건**:
  OOS 게이트를 통과하는 구조가 나오면 ADR-012로 채택 기록.
  3개 레버 모두 실패하면 전략 자체를 폐기하고 신규 가설로 전환.

---

## ADR-012: 진입 신호 재설계 실험 결과 — OOS 게이트 전체 실패 (2026-06-04)

- **실험 배경**: ADR-011에서 TP4/SL-2/no-trail 청산 구조 채택 후 진입 신호 개선 시도.
  새 지표(volumeCooldownRatio, consecutiveBullishCandles, priceRecoveryRate) + timeframe 변경.

- **실험 결과** (`bt_entry_redesign.py`, 2026-11-23~2026-05-22, train<2026-03-23 / test≥2026-03-23):
  - V0 (1m×20 현행): train PF=0.937 / test PF=1.018 / gap=0.081 → test < 1.05 실패
  - 5m/15m 리샘플링: test PF 오히려 하락 (0.926~0.954)
  - volumeCooldown, consecutiveBullish, priceRecovery 필터 단독: test PF < 1.05
  - 조합 필터 C1 (5m×10 vcr≤0.5+consec≥2): test PF=1.025 / gap=0.217 → gap 초과 실패
  - **OOS 게이트 통과 변형 없음** (기준: test PF≥1.05 AND |train-test|<0.15)

- **결정**:
  1. 현행 진입 파라미터(1m×20, 기존 필터값)를 유지한다.
  2. 새 필터(volumeCooldown 등)는 적용하지 않는다 — OOS 개선 없음.
  3. 전략 전체 구조(눌림목 반등)를 재검토 대상으로 분류한다.

- **근본 원인 재확인**:
  train PF가 모든 변형에서 1.0 미만 → gross edge가 왕복 비용(~0.1%)을 넘지 못함.
  파라미터 튜닝/필터 추가로는 해결 불가. 신호 구조 자체의 한계.

- **다음 선택지** (우선순위 미정):
  a. 신규 진입 가설 (다른 시장 구조 — 예: 브레이크아웃, 모멘텀 다이버전스)
  b. 거래 비용 구조 변경 (빈도 대폭 축소, maker 주문 전환) → ADR-013에서 채택

---

## ADR-013: 지정가(Limit) 진입 구현 — 조건부 채택, PAPER 관찰 단계 (2026-06-04)

- **배경**: ADR-012에서 taker 수수료(~0.1% 왕복)가 gross edge(~0.066%)를 잡아먹는다는 것을 확인.
  maker 비용 가정으로 재실행 시 신호가 살아남아, 지정가(maker) 진입을 PAPER에 구현하고 검증.

- **후보 비교** (maker-entry + taker-exit 0.05%, 2025-11-23~2026-05-22, train<2026-03-23):

  | 조건 | train PF | test PF | gap | 거래(tr/te) | 체결률 | 판단 |
  |---|---|---|---|---|---|---|
  | 1 taker entry/exit | 0.942 | 1.009 | 0.067 | 876/380 | 100% | FAIL |
  | 2 maker@close w1 | 1.041 | 1.071 | 0.030 | 861/389 | 91.8% | 보류 |
  | 3 maker@close w3 | 1.043 | 1.064 | 0.020 | 874/388 | 95.8% | 보류 |
  | **4 maker@close w5** | **1.047** | **1.076** | **0.028** | **869/388** | **96.8%** | **보류(선택)** |
  | 5 maker@-0.1% w3 | 0.936 | 1.045 | 0.109 | 844/381 | 48.8% | FAIL |
  | 6 maker@-0.1% w5 | 0.972 | 1.034 | 0.063 | 853/375 | 58.9% | FAIL |
  | 7 maker@-0.2% w5 | 1.009 | 1.076 | 0.067 | 806/346 | 32.2% | 보류 |

- **정직한 판정 — 보류(조건부), PASS 아님**:
  - strict 기준(train PF ≥ 1.05 AND test PF ≥ 1.05)을 깨끗이 통과하는 후보는 **없음**.
  - 선택한 maker@close w5는 train PF=1.047로 1.05 바로 **아래** (0.003 미달). test/gap/체결률은 양호.
  - 따라서 "검증된 엣지"가 아니라 **"break-even 대비 구조적 개선이 확인된, PAPER 관찰 대상"** 으로 판정.

- **선택: maker@close, 5분 유효 (후보 4)**. 가장 높은 수익이 아니라 **가장 덜 의심스러운** 조건이라서:
  - 체결률 96.8% (의미 있는 후보 중 최고) → adverse selection 노출 최소
  - 거래 수 충분 (869/388), gap 0.028로 train/test 안정
  - 규칙 단순: limit = 신호 캔들 close, 5분 유효 (추가 필터 없음)
  - offset(-0.1%/-0.2%) 후보는 체결률 급락(32~59%)·불안정 → 탈락

- **taker 대비 개선점**: train PF 0.942→1.047, test PF 1.009→1.076, train MDD 190%→101%.
  손실 구조(taker)에서 marginal 흑자 구조로 이동. 단, 흑자 폭은 얇음.

- **구현/감사 수정 내용**:
  - `PendingLimitOrder(exchange, market, limitPrice, quantity, reason, createdAt, expiresAt)`
  - `PendingLimitOrderService` — ConcurrentHashMap 보관, `checkAndFillAll()` per exit tick
  - `OrderExecutionService.fillLimitOrder()` — 체결 시 full risk + portfolio 검증
  - `CandidateExecutionService` — 즉시 체결 → limit 등록 (동일 마켓 중복 pending 방지)
  - `ScheduledPositionExitRunner` — readiness OK인 exchange에 한해 fill 체크

- **2차 감사에서 수정한 버그**:
  1. **firstCheckAt 과보수 버그 제거**: 이전 `firstCheckAt = now + candleUnit`은 첫 체결을 candle i+2로 밀어
     백테스트(i+1)와 불일치. → 제거하고 `capturedAt > createdAt` 조건으로 대체.
  2. **stale price 체결 위험 제거**: `find()` → `findFresh(orderStaleDuration)`로 교체.

- **검증된 안전 불변식**:
  1. **same-candle fill 0건**: 신호는 closed candle 기반 → `createdAt`은 신호 캔들 close 이후.
     체결은 `capturedAt > createdAt`인 snapshot에서만 → 신호 캔들 가격으로는 절대 체결 불가.
  2. **stale 방지**: `findFresh` + 스케줄러 `readiness.ready()` 이중 가드.
  3. **미완성 캔들 제외**: `removeIncompleteLatestCandle()`로 closed candle만 신호에 사용.

- **한계 및 아직 실거래가 아닌 이유**:
  - train PF가 1.05 미달 → 통계적으로 "확정 엣지" 아님. 더 많은 OOS 데이터 필요.
  - 실거래 maker 주문은 **체결 보장 없음** (adverse selection). PAPER는 fresh snapshot이면 항상 체결 → 낙관 편향 가능.
  - MDD 100%+ (train) → 포지션 크기·동시 노출 관리 필수. 기존 일일 손실·쏠림 리스크 정책 유지.
  - 앱 재시작 시 in-memory pending 소실 → PAPER에선 다음 스캔(60초)에 재생성되므로 허용. 실거래 전 영속화 필요.
  - REAL_TRADING / 실제 주문 API 미구현 (불변).

- **다음 PAPER 관찰 항목 / 중단 기준**:
  - 관찰: 실제 fill_rate(목표 ≥ 90%), 실체결 PF, same-candle fill 0건 유지, stale skip 빈도.
  - 중단: 실 fill_rate < 80%, 또는 누적 PF < 0.95, 또는 same-candle fill 1건이라도 발생 시 차단·재분석.
  - 재검토: 2~4주 후 실 PAPER 수치 vs 백테스트(96.8% / 1.076) 대조하여 채택/폐기 결정.

---

## ADR-014: US stock data expansion starts with file import, not direct API binding (2026-06-25)

- **Context**: The next expansion target is US stocks/ETFs for PAPER research only. The
  initial universe is small (`SPY`, `QQQ`, `IWM`, `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`,
  `META`, `GOOGL`), but intraday data access depends on provider accounts, plans, and
  market-data policy.

- **Decision**:
  1. Add stock support through a provider-neutral local candle import path first.
  2. Keep API-backed providers behind an interface and do not hardcode API keys or
     provider-specific assumptions into strategy/backtest logic.
  3. Store stock data separately from crypto data under `.backtest_cache/stock/us/`.
  4. Require manifest metadata for timezone, adjusted/raw mode, regular/extended hours,
     provider, symbol, interval, since/until, and collection time.
  5. Keep stock PAPER automation OFF until stock-specific backtest/OOS evidence is recorded.

- **Why**:
  - Alpha Vantage provides equity time-series APIs and documents intraday intervals, but
    historical/realtime intraday access is premium.
  - Polygon/Massive provides stock aggregate bars and is a suitable later API-backed
    provider, but plan/access constraints should not shape core domain boundaries.
  - A file import first path lets the project validate symbol modeling, cost modeling,
    timezone/session handling, and cache layout without introducing credentials or network
    dependency into the core PAPER flow.

- **Implications**:
  - The first stock implementation should focus on `MarketAssetClass`/venue/symbol modeling
    and candle import validation, not scheduler automation.
  - Backtest leaderboard rows need asset class, venue, symbol, timezone, adjusted/raw, and
    provider columns before stock strategies are compared with crypto strategies.
  - `REAL_TRADING` and broker order APIs remain forbidden.

---

## ADR-015: Stock symbols use asset class and venue, not `ExchangeMode` extension (2026-06-25)

- **Context**: `ExchangeMode` currently drives crypto exchange behavior across candidate
  scanning, PAPER portfolio state, history, risk, and scheduler scope. Adding `US_STOCK`
  directly to `ExchangeMode` would make stock research look like another crypto exchange
  and would increase the chance of accidental scheduler or portfolio coupling.

- **Decision**:
  1. Keep `ExchangeMode` as the crypto exchange mode for existing `UPBIT` and `BINANCE`
     behavior.
  2. Introduce a separate stock modeling boundary before code-level stock execution:
     `MarketAssetClass` (`CRYPTO`, `STOCK`) and `MarketVenue` (`UPBIT`, `BINANCE`,
     `US_STOCK`) or equivalent value objects.
  3. Stock symbols remain plain ticker symbols such as `AAPL`, `MSFT`, and `SPY`; market
     identity is the tuple `(assetClass, venue, symbol)`.
  4. Currency is explicit metadata: crypto uses `KRW`/`USDT` by venue, US stock research
     uses `USD`.
  5. Timezone/session metadata is explicit for stocks: `America/New_York`, regular-session
     versus extended-hours, and adjusted/raw candle mode.

- **Why**:
  - The current crypto execution path assumes exchange-specific public crypto market data,
    PAPER cash buckets, and scheduler readiness checks.
  - Stock research needs separate provider metadata, trading session semantics, adjusted/raw
    candle handling, and cost assumptions.
  - A tuple identity avoids overloading symbol strings. `AAPL` is not comparable to
    `BTCUSDT` or `KRW-BTC` without asset-class and venue context.

- **Implications**:
  - The first stock code change should add model types and import validation only; it should
    not add stock scheduler automation.
  - Existing crypto APIs can keep accepting `exchange=UPBIT|BINANCE`.
  - Future stock APIs should use asset class / venue parameters explicitly instead of
    reusing crypto `exchange` query semantics.
