# tasks/current.md — 현재 작업 컨텍스트

## 단계

지정가(maker) 진입 구현 완료 → PAPER 관찰 대기
ADR-013은 보류/조건부. 검증된 엣지 채택이 아니라 PAPER 관찰 대상이다.

## 현재 목표

봇 재기동 후 2~4주 동안 실제 운용 지표를 관찰하고, 백테스트 기대치와 비교해 maker 진입을 채택/폐기한다.

## 현재 상태 요약

- 진입: `PendingLimitOrderService` 기반 maker 지정가, limit@signal close, 5분 유효
- 체결 조건: `capturedAt > createdAt`, fresh price only; same-candle fill 방지
- 청산: TP +4.0%, SL -2.0%, trailing off
- 실행 모드: `PAPER_TRADING` 전용, 실제 주문 API 금지
- 백테스트 선택안: maker@close w5, train PF 1.047 / test PF 1.076 / fill_rate 96.8%
- 판정: strict 기준 train PF ≥ 1.05 미달. break-even 대비 구조 개선 가능성만 확인

## 완료된 최근 작업

- maker 지정가 진입 구현: pending order 생성, fresh fill, risk+portfolio 검증
- 2차 감사 수정: `firstCheckAt` 과보수 제거, stale price 체결 가드 추가
- 단위 테스트 6종: same-candle 0건, stale 거부, 정상 체결, 중복 방지, 교차 격리
- ADR-011: trailing 제거 + TP/SL 4:2 청산 구조
- ADR-012: 신규 진입 필터 OOS 게이트 실패, 현행 1m×20 유지
- ADR-013: maker 진입은 PAPER 관찰 후 결정

## 다음 액션

1. 봇 실행 상태 확인: `lsof -nP -iTCP:18080 -sTCP:LISTEN`
2. 필요 시 `bash scripts/run-local-dev.sh` 또는 Docker app 재기동
3. 2~4주 PAPER 관찰:
   - 실제 fill_rate
   - 실체결 PF
   - same-candle fill 0건
   - stale skip 발생 여부
4. 실 PAPER 수치와 백테스트 기대치(fill_rate 96.8%, test PF 1.076) 비교
5. 채택/폐기 결정 전 실거래 전환 금지

## 중단 기준

- fill_rate < 80%
- 누적 PF < 0.95
- same-candle fill 1건 이상
- 실제 주문 API 또는 `REAL_TRADING` 필요성이 생기는 방향의 요구

## 완료 기준

- `./gradlew test checkstyleMain` 통과
- 프론트 변경 시 `cd frontend && npm run lint && npm run build && npm test` 통과
- 실제 주문 API, `REAL_TRADING`, 수동 BUY UI 없음

## 다음 작업 후보

- PAPER 관찰 데이터가 1주 이상 쌓인 뒤 candidate-scan-log SKIPPED 사유 분포 분석
- 장기 보유 사례 확인 시 시간 기반 청산 검토
- 단일 market 쏠림 발생 시 concentration risk 활성화 검토

## 참고

- 프로젝트 원칙: `HARNESS.md`
- 상세 구조/정책: `docs/architecture.md`, `docs/spec.md`, `docs/decisions.md`
- 운용 기록: `docs/trading/condition-records/`

## 이전 세션에서 멈춘 곳

2026-06-05 — Java parity 백테스트 하니스 구축 + 전략 진단(커밋 완료, push 대기/완료).
- 하니스: `src/test/.../backtest/` — 운영 CandidateScanner·PositionExit·PaperPortfolio를 수정 없이
  과거 캔들로 리플레이(ReplayCandleProvider가 UpbitCandleProvider 상속). opt-in 테스트
  (`-Dbacktest.run` / `-Dbacktest.sweep` / `-Dbacktest.entrysweep`), 기본 suite는 skip.
- 진단(180일 캐시): 현행 설정 gross PF<1.0 → 비용 문제 아닌 **신호 문제**.
  TP×SL 12칸·진입 변형 12종 전부 일반화 gross 엣지 ≥1.0 없음. V1(최소 눌림폭 0.5%)만 0.91→0.97 리드.
- backtest.py는 운영 거울 아님(시장가·off-by-one 진입·캡 없음·raw PF) 확인 — 수치 신뢰 불가.
- 연구 로그 3건 추가: java-parity-backtest / exit-sweep / entry-sweep (2026-06-05).
다음 세션: V1 출발점으로 새 진입 피처(다중 TF·레짐 게이트·유동성·변동성 사이징) 가설을 하니스로 검증
(PFgross 1차 게이트 ≥~1.15, PFnet train/test OOS 채택 게이트). 또는 풀백군 폐기 후 전략 전환 검토.
