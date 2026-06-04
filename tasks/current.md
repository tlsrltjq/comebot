# tasks/current.md — 현재 작업 컨텍스트

## 단계: 전략 재설계 — UI 리뉴얼 완료, 진입 신호 OOS 결과 대기

## 2026-06-02 청산 재설계 (완료, ADR-011)

- [x] ADR-011 기록 (파라미터 튜닝 중단, OOS 게이트)
- [x] 청산 변형 × timeframe 실험 → 트레일링 제거 + 넓은 TP = 승자 run이 압도
- [x] 채택: TP 2→4, SL -1.5→-2, 트레일링 OFF (OOS train 0.94 / test 1.02)
- [x] .env + application.properties 기본값 반영, 봇 재기동
- [x] condition-records/2026-06-02-exit-redesign-let-winners-run.md

→ 현 상태: "손실 구조 → break-even". robust 흑자엔 진입 신호 개선 필요.

## 진입 신호 재설계 [진행 중, ADR-012 후보]

목표: train·test 모두 PF ≥ 1.05 (현 청산구조에선 train 0.94 / test 1.02)

### 구현 완료 (2026-06-02)
- [x] `volumeCooldownRatio` 지표 추가 — VolatilitySnapshot 필드, VolatilityIndicatorService 계산
- [x] `maxVolumeCooldownRatio` 필터 — CandidateScannerProperties + CandidateScannerService
  - 기본값 0 (비활성), 환경변수: `STRATEGY_CANDIDATE_MAX_VOLUME_COOLDOWN_RATIO`
  - per-exchange override: `STRATEGY_CANDIDATE_UPBIT_MAX_VOLUME_COOLDOWN_RATIO`
- [x] `bt_entry_redesign.py` 작성 — 8개 변형(V0~V7) OOS 백테스트 스크립트
- [x] `consecutiveBullishCandles` 지표 추가 (윈도우 끝 연속 양봉 수)
- [x] `minConsecutiveBullishCandles` 필터 (기본값 1, 환경변수: `STRATEGY_CANDIDATE_MIN_CONSECUTIVE_BULLISH_CANDLES`)
- [x] `priceRecoveryRate` 지표 추가 ((close-low)/(high-low)×100)
- [x] `minPriceRecoveryRate` 필터 (기본값 0=비활성, 환경변수: `STRATEGY_CANDIDATE_MIN_PRICE_RECOVERY_RATE`)

### 완료 (2026-06-03)
- [x] UI 전면 다크모드 리뉴얼 (트레이딩 콘솔 스타일)
  - styles.css 전면 재작성: GitHub dark 팔레트, CSS 커스텀 프로퍼티 기반 토큰 체계
  - 사이드바 compact (200px), 모노스페이스 숫자 (font-variant-numeric: tabular-nums)
  - Recharts 차트 다크모드 색상 통일 (stroke/fill/tooltip/grid)
  - nav 레이블 영문화, 테스트 업데이트
  - 빌드/린트/유닛테스트 전부 통과

### 결과 (2026-06-04, ADR-012)
- [x] `bt_entry_redesign.py` 실행 (8종 기본 + 조합 포함 총 20변형)
- [x] ADR-012 기록 — OOS 게이트 통과 변형 없음
- **결론**: V0 (1m×20 현행)이 test PF=1.018로 가장 안정적. 신규 필터 미적용.

### 다음 결정 (미정)
현 전략 구조(눌림목 반등)의 한계 재확인. 세 가지 선택지:
- a. 신규 진입 가설로 전환
- b. 거래 비용 구조 변경 (빈도 축소 / maker)
- c. PAPER 유지 + 데이터 축적 관찰

### 백테스트 변형 목록
| 변형 | 캔들 | volumeCooldown |
|---|---|---|
| V0 | 1분봉 × 20 (현행) | 비활성 |
| V1 | 5분봉 × 10 | 비활성 |
| V2 | 5분봉 × 6 | 비활성 |
| V3 | 15분봉 × 6 | 비활성 |
| V4 | 5분봉 × 10 | vcr ≤ 0.5 | - |
| V5 | 5분봉 × 10 | vcr ≤ 0.3 | - |
| V6 | 5분봉 × 10 | - | consec ≥ 2 |
| V7 | 5분봉 × 10 | - | recovery ≥ 30% |

---

## (이전) 엣지 부재 확인 기록

## 이번 세션 결과 (OOS 검증)

- [x] train/test 분리(120/60일) walk-forward 검증
- [x] 시간대 필터 OOS 무효 확인 → 기본값 비활성으로 되돌림
- [x] 진입조건 스윕(min_vol/min_px/dist) — 어떤 조합도 test PF < 1.0
- [x] 근본원인 규명: gross edge 0.066%/거래 < 왕복 비용 0.1%
- [x] DOGE/PROVE 해제안 기각 — OOS에선 우량 아님(직전 "PROVE 최고"는 in-sample 누수)
- [x] condition-records/2026-06-02-oos-validation-and-edge-analysis.md 기록

## 결론

현 pullback-bounce + TP/SL/trailing 구조는 비용 차감 후 검증된 엣지가 없다.
**파라미터 튜닝은 답이 아님.** PAPER 유지, 실거래 금지.

## 다음 작업 (우선순위)

1. **청산 구조 재설계** — 현 trailing은 승자를 +1.1%로 캡. 승자 run 허용(넓은 TP/시간기반 청산)으로 평균 승폭 확대 실험 → train/test OOS 검증
2. **신호 timeframe 재검토** — 1분봉 pullback은 노이즈. 5/15분봉 백테스트
3. **비용 구조** — 거래 빈도 대폭 축소 또는 maker 주문 (gross edge가 0.1% 비용을 넘어야 함)
4. (권장) `.env`: `STRATEGY_CANDIDATE_UPBIT_MIN_TRADE_AMOUNT_CHANGE_RATE=100` — 과매매·수수료 드래그 감소 (흑자 전환 아님, 손실 축소)
5. 모든 변경은 train/test OOS로만 채택. backtest.py는 로컬(gitignored)

---

## 완료된 구현 (2026-06-02 기준)

### 전략
- [x] PullbackBounce 전면 교체 (ADR-010) — BTC 1h EMA 트렌드 필터 포함
- [x] per-exchange scanner override (Upbit/Binance 독립 필터값)
- [x] 미완성 최신 캔들 제외 (removeIncompleteLatestCandle)
- [x] market exclusion list (`KRW-DOGE`, `KRW-PROVE`, `KRW-TRAC`)

### 리스크
- [x] trailing stop (activation-rate / trail-rate)
- [x] profit-based re-entry guard (min-reentry-profit-rate)
- [x] guard abnormal paper exits (abnormal-exit-price-drop-rate=-20)
- [x] stop-loss cooldown (1d/2회 → 6h 차단)

### 인프라·품질
- [x] exit scheduler 1초 단축
- [x] Docker Compose 전체 스택
- [x] 서버 포트 18080 통일
- [x] Checkstyle 10.21.4 (config/checkstyle/checkstyle.xml)
- [x] BtcTrendCacheService 의존성 역전 (CandleProvider 인터페이스)
- [x] TradingFlowService / ScheduledTradingFlowRunner @Deprecated
- [x] 미사용 import 3건 제거 (Checkstyle 검출)

### 하네스
- [x] CLAUDE.md 신규 생성
- [x] HARNESS.md 전면 재구성
- [x] docs/decisions.md (ADR-001~010)
- [x] docs/architecture.md
- [x] docs/spec.md

---

## 완료 기준

- `./gradlew test checkstyleMain` 통과
- `npm run lint && npm run build && npm test` 통과
- 실제 주문 API, REAL_TRADING, 수동 BUY UI 없음

---

## 건드리면 안 되는 파일

| 파일 | 이유 |
|---|---|
| `.env` | 운영 시크릿 포함 |
| `application.properties` | 기본값 변경 시 사용자 확인 필요 |
| `docs/trading/condition-records/` | 운용 기록 — 추가만 허용 |
| `frontend/eslint.config.js` | 보안 린트 규칙 |

---

## 다음 작업 후보 (우선순위 미정)

1. **파라미터 튜닝** — Upbit/Binance per-exchange 필터 수치 조정
   - 트리거: candidate-scan-log SKIPPED 사유 분포 분석 후
   - 필요 데이터: 최소 1주 이상 운용 이력
2. **시간 기반 청산** — 포지션 보유 시간 상한 (예: 24h 이상 보유 시 강제 청산)
   - 트리거: 익절/손절 미달 포지션 장기 보유 사례 확인 후
3. **concentration risk 활성화** — `RISK_CONCENTRATION_ENABLED=true`
   - 트리거: 단일 market 쏠림 패턴 실제 발생 확인 후

---

## 이전 세션 중단 지점

2026-06-02 — 하네스 신규 구축 완료.
봇은 `scripts/run-local-dev.sh`로 기동 가능 (포트 18080).
현재 봇 실행 상태: 확인 필요 (`lsof -nP -iTCP:18080 -sTCP:LISTEN`).
