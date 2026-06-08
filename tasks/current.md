# tasks/current.md — 현재 작업 컨텍스트

## 단계

전략 리서치 — Java parity 백테스트 하니스로 V1 풀백 반등군을 전면 진단 완료.
**결론: 이 데이터에서 tradeable 엣지 없음.** 다음은 추가 튜닝이 아니라 전략 전환 또는 데이터 범위 재검토.

## 현재 목표

V1 풀백군은 소진(아래 메타 결론). 새 국면 결정 대기 — (A) 다른 전략 가설 또는 (B) 데이터/범위 확장.

## 현재 상태 요약

- 검증 도구: `src/test/.../backtest/` 운영 엔진 리플레이 parity 하니스 (opt-in 플래그로 실행, 기본 suite skip)
  - `-Dbacktest.run`(parity), `-Dbacktest.sweep`(TP×SL), `-Dbacktest.entrysweep`(진입), `-Dbacktest.regime`(레짐), `-Dbacktest.mtf`(다중TF)
  - 결정론적 엔진 단위테스트 6종은 기본 suite 상시 실행(회귀 가드)
- 지표: PFgross(비용 전=신호 엣지) 1차 게이트, PFnet train/test OOS 채택 게이트
- **메타 결론(180일 Upbit 캐시)**: 청산 TP×SL·진입 변형·레짐 게이트·다중TF 모든 레버가 gross PF 0.94~0.97에 수렴
  → V1 풀백 반등군은 엣지 없음. 필터는 음의 엣지를 다듬을 뿐 부호를 못 바꿈. 방향성 필터 2개+ 스택은 OOS 붕괴.
- 최선 방어 필터(수익 전환 아님): M1(coin1h==UP)·F1(BTC≠DOWN) — MDD 5.0→2.5% 반감
- 운영 봇: `PAPER_TRADING` 전용 유지, 실제 주문 API/`REAL_TRADING` 금지 불변

## 완료된 최근 작업

- maker 지정가 진입 구현: pending order 생성, fresh fill, risk+portfolio 검증
- 2차 감사 수정: `firstCheckAt` 과보수 제거, stale price 체결 가드 추가
- 단위 테스트 6종: same-candle 0건, stale 거부, 정상 체결, 중복 방지, 교차 격리
- ADR-011: trailing 제거 + TP/SL 4:2 청산 구조
- ADR-012: 신규 진입 필터 OOS 게이트 실패, 현행 1m×20 유지
- ADR-013: maker 진입은 PAPER 관찰 후 결정

## 다음 액션 (나중에 할 일 — 등록됨)

다음 중 하나를 골라 시작. 모두 기존 하니스(BacktestEngine/EntryGate/RegimeContext) 재사용 가능.

1. **(A) 다른 전략 가설** — 풀백 반등이 아닌 진입 메커니즘으로 gross 엣지 탐색:
   - 추세추종 돌파(전고 돌파 + 거래량), 또는 다른 평균회귀(과매도 반등 등)
   - 새 `Strategy`/스캐너 변형을 하니스에 물려 PFgross 1차 게이트(≥~1.15) → PFnet OOS 채택 게이트
2. **(B) 데이터/범위 재검토** — 같은 V1을 더 긴 기간·다른 마켓 유니버스·다른 base TF(3m/5m)에서 재측정.
   엣지가 데이터 특정인지 구조적 부재인지 분리. `.backtest_cache` 확장 필요(backtest.py로 캔들 수집).
3. **(보조) 운영 방어 적용 검토** — 수익 전환은 아니지만 손실/MDD 감소용으로 M1(coin1h==UP) 또는
   F1(BTC≠DOWN)을 운영 스캐너에 게이트로 넣을지 결정(코드 변경 = src/main, 사용자 확인 후).

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
다음 세션: 전략 리서치 자체는 여전히 위 "다음 액션"의 (A) 다른 전략 가설 또는 (B) 데이터/범위 재검토 중 택1로 시작.
