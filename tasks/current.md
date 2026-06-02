# tasks/current.md — 현재 작업 컨텍스트

## 단계: 시간대 필터 적용 후 검증 대기

## 이번 세션 목표 (완료)

- [x] backtest.py에 KST 시간대 필터 + 현 .env 파라미터 반영, 재백테스트
- [x] 시간 필터(0,12,17,21,23) + IRYS 제외 코드 구현
- [x] 분석을 condition-records/2026-06-02-time-filter-and-irys-exclusion.md 기록

## 다음 세션 검증 항목

1. 실거래 1주 후 우량 시간대 실제 승률이 백테스트(50~58%)와 일치하는지 재집계
2. **DOGE/PROVE 제외 해제안 재검증** — 현 파라미터에선 PROVE(+31,211)가 최고 마켓인데 제외 중. `excluded-markets`를 `KRW-TRAC,KRW-IRYS`로 축소 검토
3. 시간대 화이트리스트 과최적화 여부 out-of-sample 검증

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
