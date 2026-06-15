# tasks/current.md — 현재 작업 컨텍스트

## 단계

전략 리서치 — V1 풀백 반등군 엣지 없음 결론 이후, 공격적 수익률 탐색을 위한 데이터 확장/전략 후보 실험으로 전환.

## 현재 목표

`tasks/strategy-research-plan.md` 기준으로 Upbit/Binance 원본 캔들 데이터를 확장하고,
상대강도 모멘텀부터 새 전략 후보를 순차 검증한다.

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
- 수집기 안정화: 진행률 로그, Upbit 상장 이전 cursor 가드, HTTP 재시도 10회, `Connection: close`, 최대 60초 backoff 추가
- maker 지정가 진입 구현: pending order 생성, fresh fill, risk+portfolio 검증
- 2차 감사 수정: `firstCheckAt` 과보수 제거, stale price 체결 가드 추가
- 단위 테스트 6종: same-candle 0건, stale 거부, 정상 체결, 중복 방지, 교차 격리
- ADR-011: trailing 제거 + TP/SL 4:2 청산 구조
- ADR-012: 신규 진입 필터 OOS 게이트 실패, 현행 1m×20 유지
- ADR-013: maker 진입은 PAPER 관찰 후 결정

## 다음 액션 (나중에 할 일 — 등록됨)

1. 종목 랭킹 기반 로테이션 전략 후보를 정의하고 확장 유니버스 리더보드 산출.
2. 기존 다섯 후보의 공통 탈락 패턴(train gross edge 부족, ETH 편중, OOS 표본 부족)을 확장 유니버스에서 재검증할지 결정.
3. 상승/하락/횡보 레짐 라벨링 기준을 만들고 리더보드에 연결.

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
- 다음 세션: 종목 랭킹 기반 로테이션 후보를 구현하고 확장 유니버스 리더보드를 산출.
