# docs/spec.md — 기능 SSOT (전략·신호·주문·리스크 규칙)

이 파일은 코드 동작의 단일 진실 공급원(SSOT)이다.
코드를 변경하면 이 파일도 반드시 갱신한다.

---

## 1. 전략 개요

| 항목 | 값 |
|---|---|
| 전략명 | PullbackBounce (bean: `VOLATILITY_BREAKOUT_LONG`) |
| 방향 | 롱 전용 (숏·마진·레버리지 없음) |
| 거래소 | Upbit (KRW) + Binance (USDT), 공개 시세만 |
| 거래 모드 | `PAPER_TRADING` 전용 |
| 후보 주기 | 30초 (운영 `.env`; 기본 60초) |
| 1회 최대 BUY | 2건/주기 (`max-buys-per-run`) |
| 1회 주문 금액 | 10,000 KRW / (금액 기반 수량 산정, Binance는 별도) |

---

## 2. 후보 선정 — 진입 필터 파이프라인

순서대로 평가하며, 하나라도 실패하면 `SKIPPED` 반환. 전체 통과 시 `SELECTED`.

### 2-1. BTC 1h 트렌드 필터 (Upbit 전용)

- `BtcTrendCacheService`가 KRW-BTC 1h 캔들 20개로 EMA5 vs EMA10 계산
- 5분마다 갱신, 앱 시작 시 bootstrap
- BTC 추세가 `DOWN`이면 UPBIT 마켓 진입 차단
- BINANCE 마켓은 해당 없음

### 2-2. 미완성 최신 캔들 제외

- 현재 진행 중인 1분봉은 평가에서 제외 (`removeIncompleteLatestCandle`)
- 완성된 캔들만 사용하여 거래대금·양봉 판단 노이즈 제거

### 2-3. 유효 캔들 수 검증

- 유효 캔들(거래대금 > 0) 수 ≥ 2개 필요
- 미달 시 SKIPPED

### 2-4. 추세 (trend UP)

- `priceChangeRate` > 0 (전체 윈도우 기간 가격 변화율)
- 음수이면 순수 하락장 — SKIPPED

### 2-5. 펌프 발생 확인 (pumpDetected)

- 윈도우 내 최고 캔들 기준 `windowHighChangeRate ≥ minPriceChangeRate`
- 윈도우 내 피크 기준 `peakTradeAmountChangeRate ≥ minTradeAmountChangeRate`

### 2-6. 과열 차단

- `priceChangeRate > maxPriceChangeRate` → SKIPPED (과도한 급등)
- `highLowRangeRate > maxHighLowRangeRate` → SKIPPED (캔들 진폭 과열)

### 2-7. 눌림목 구간 확인 (distanceFromHigh)

- `distanceFromHighRate ≥ minDistanceFromHighRate` — 최소 눌림폭 확인
- `distanceFromHighRate ≤ maxDistanceFromHighRate` — 최대 눌림폭 초과 차단

### 2-8. 마지막 캔들 양봉 (lastCandleBullish)

- `tradePrice > openingPrice` — 반등 캔들 확인
- 음봉이면 SKIPPED

### 2-9. 최신 캔들 최소 거래대금

- KRW 마켓: `latestCandleTradeAmount ≥ minLatestCandleTradeAmountKrw`
- USDT 마켓: `latestCandleTradeAmount ≥ minLatestCandleTradeAmountUsdt`
- 절댓값 유동성 하한 보장

### 2-10. 진입 시간대 필터 (KST)

- 현재 KST 시각이 `strategy.entry.allowed-hours-kst` 화이트리스트에 없으면 SKIPPED (`Outside allowed trading hours (KST)`)
- 빈 목록이면 비활성(전 시간 허용)
- **기본값 = 빈 값(비활성)**. 시간대 화이트리스트는 train/test OOS 검증에서 일반화 실패(과최적화) 확인 — 메커니즘만 튜닝 도구로 유지
- 근거: `condition-records/2026-06-02-oos-validation-and-edge-analysis.md`

---

## 3. 파라미터 기본값

### 전역 (`application.properties` 기본값)

| 파라미터 | 기본값 | 환경변수 |
|---|---|---|
| 캔들 수 | 5 | `STRATEGY_CANDIDATE_CANDLE_COUNT` |
| 캔들 단위 (분) | 1 | `STRATEGY_CANDIDATE_CANDLE_UNIT_MINUTES` |
| 최소 가격 변화율 | 1.0% | `STRATEGY_CANDIDATE_MIN_PRICE_CHANGE_RATE` |
| 최소 거래대금 변화율 | 30% | `STRATEGY_CANDIDATE_MIN_TRADE_AMOUNT_CHANGE_RATE` |
| 최대 가격 변화율 | 10% | `STRATEGY_CANDIDATE_MAX_PRICE_CHANGE_RATE` |
| 최대 고저 범위 | 20% | `STRATEGY_CANDIDATE_MAX_HIGH_LOW_RANGE_RATE` |
| 최근 고점 대비 최대 거리 | 2% | `STRATEGY_CANDIDATE_MAX_DISTANCE_FROM_HIGH_RATE` |
| 최근 고점 대비 최소 거리 | 0% | `STRATEGY_CANDIDATE_MIN_DISTANCE_FROM_HIGH_RATE` |
| 최신 캔들 최소 거래대금 (KRW) | 10,000,000 | `STRATEGY_CANDIDATE_MIN_LATEST_CANDLE_TRADE_AMOUNT_KRW` |
| 최신 캔들 최소 거래대금 (USDT) | 50,000 | `STRATEGY_CANDIDATE_MIN_LATEST_CANDLE_TRADE_AMOUNT_USDT` |
| 스캔 제외 마켓 | KRW-DOGE, KRW-PROVE, KRW-TRAC, KRW-IRYS | `MARKET_SELECTION_EXCLUDED_MARKETS` |
| 진입 허용 시간대 (KST) | (빈 값=비활성) | `STRATEGY_ENTRY_ALLOWED_HOURS_KST` |

### Upbit 운영 override (`.env`)

| 파라미터 | 운영값 |
|---|---|
| 캔들 수 | 20 |
| 최소 가격 변화율 | 0.15% |
| 최소 거래대금 변화율 | 0% |
| 최신 캔들 최소 거래대금 (KRW) | 1,000,000 |
| 최근 고점 대비 최대 거리 | 5% |
| 최근 고점 대비 최소 거리 | 0% |

### Binance 운영 override (`.env`)

| 파라미터 | 운영값 |
|---|---|
| 캔들 수 | 10 |
| 최소 가격 변화율 | 0.8% |
| 최소 거래대금 변화율 | 30% |
| 최신 캔들 최소 거래대금 (USDT) | 50,000 |
| 최근 고점 대비 최대 거리 | 2% |
| 최근 고점 대비 최소 거리 | 0.5% |

---

## 4. 주문 신호

| 신호 | 조건 | 결과 |
|---|---|---|
| `BUY` | CandidateScannerService → SELECTED | PAPER 매수 주문 |
| `SELL` (익절) | 미실현 수익률 ≥ take-profit-rate | PAPER 매도 주문 |
| `SELL` (손절) | 미실현 수익률 ≤ stop-loss-rate | PAPER 매도 주문 |
| `SELL` (trailing) | peak profit 달성 후 trail만큼 하락 | PAPER 매도 주문 |
| `HOLD` | 위 조건 미달 | 포트폴리오 변경 없음 |
| `REJECTED` | 리스크 검증 실패 | 포트폴리오 변경 없음, history 기록 |
| `FAILED` | 실행 오류 | 포트폴리오 변경 없음, history 기록 |

---

## 5. 리스크 규칙

### 익절/손절

| 항목 | 기본값 | 운영 `.env` |
|---|---|---|
| 익절 기준 | +1.5% | +2.0% |
| 손절 기준 | -0.7% | -1.5% |
| 비정상 급락 차단 | -20% 이하 시 SELL 차단 | 동일 |

### 트레일링 스톱

| 항목 | 기본값 | 운영 `.env` |
|---|---|---|
| 활성화 여부 | false | true |
| 활성화 수익률 | +0.5% | +2.0% |
| Trail 비율 | 0.3% | 1.5% |

### 포지션 수 상한

| 항목 | 기본값 | 운영 `.env` |
|---|---|---|
| Upbit 최대 포지션 | 3 | 8 |
| Binance 최대 포지션 | 3 | 8 |
| 합계 최대 포지션 | 5 | 12 |

### Stop-Loss Cooldown

| 항목 | 값 |
|---|---|
| 활성화 | true |
| 윈도우 | 1일 |
| 트리거 횟수 | 2회 손절 |
| 차단 기간 | 6시간 |

### Kill Switch

- `safety.kill-switch-enabled=true` 시 모든 자동 실행 차단
- history, status, portfolio 조회는 차단하지 않음

### 일일 제한 (기본 비활성)

- `risk.daily-risk-enabled=false` (기본)
- 활성화 시: 일일 주문 10건, 일일 손실 50,000 KRW 초과 시 차단

### 쏠림 차단 (기본 비활성)

- `risk.concentration.enabled=false` (기본)
- Upbit: 경고 7%, 차단 10%
- Binance: 경고 25%, 차단 40%

---

## 6. 주문 생명주기

```
신호 생성
  → kill switch 확인
  → risk 검증 파이프라인 (포지션 수 → cooldown → concentration → daily)
  → 포트폴리오 검증 (현금·보유 수량)
  → PaperTradingExecutionGateway.execute()
      → 체결: FILLED (포트폴리오 반영, history 저장, 알림 발송)
      → 거절: REJECTED (포트폴리오 변경 없음, history 저장)
      → 실패: FAILED (포트폴리오 변경 없음, history 저장)
```

---

## 7. 웹 UI 허용 작업

| 작업 | 허용 여부 | 비고 |
|---|---|---|
| 시스템 상태 조회 | ✅ | |
| 후보 조회 | ✅ | |
| 포트폴리오 조회 | ✅ | |
| 거래 이력 조회 | ✅ | |
| Analytics 조회 | ✅ | |
| 자동매매 ON/OFF | ✅ | `/api/scheduler/control` |
| candidate 주기 변경 | ✅ | 30초 또는 60초만 허용 |
| 선택 PAPER 포지션 SELL | ✅ | 유일한 수동 거래 예외 |
| 수동 BUY | ❌ | ADR-007 |
| 후보 수동 실행 | ❌ | ADR-007 |
| REAL_TRADING 전환 | ❌ | ADR-001, 영구 금지 |
