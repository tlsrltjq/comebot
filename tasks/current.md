# 현재 작업 컨텍스트

## 지금 단계: 데이터 축적 대기

## 목표

2026-05-20 기준으로 모든 계획된 구현이 완료되었다.
지금은 PAPER 운용 데이터를 충분히 쌓고, 전략 성과를 관찰하는 단계다.

## 완료된 구현 (이번 세션)

- [x] CandidateScanLogService, InMemoryCandidateScanLogRepository 구현
- [x] CandidateScanLogController (`GET /api/candidate-scan-log`)
- [x] CandidateScannerService scan 결과 SELECTED/SKIPPED 전량 기록
- [x] max-buys-per-run (1회 스케줄 주기당 최대 BUY 수 제한, 기본 2)
- [x] minLatestCandleTradeAmount 진입 필터 (KRW 1천만 / USDT 5만)
- [x] maxDistanceFromHighRate 진입 필터 (최근 5분 고점 대비 2% 이내)
- [x] VolatilitySnapshot에 latestCandleTradeAmount, distanceFromHighRate 추가
- [x] PositionLimitRiskValidationService 테스트 (4개)
- [x] CandidateScanLogService 테스트 (6개), CandidateScanLogController 테스트 (4개)
- [x] 전체 문서 최신화 (README, HARNESS, ARCHITECTURE, PROJECT_HISTORY, PROJECT_NEXT_STEPS, OPERATIONS, decisions)

## 완료 기준 충족

- `./gradlew test` 통과
- 보안 린트 통과
- 실제 주문 API, REAL_TRADING, 수동 BUY UI 추가 없음

## 다음 작업

데이터 축적 후 결정:
- trailing stop / 시간 기반 청산 (2–3개월 PAPER 이력 필요)
- 진입 필터 수치 조정 (scan log 통계로 filter-out rate 분석 후 결정)
