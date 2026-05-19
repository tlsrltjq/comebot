# Claude Feedback Roadmap

클로드 피드백 기반으로 작업 우선순위를 관리한다.
실제 작업 순서는 `docs/project/PROJECT_NEXT_STEPS.md`와 `tasks/current.md`에 반영한다.

## 원칙

- 기본 거래 모드는 계속 `PAPER_TRADING`이다.
- `REAL_TRADING`과 실제 거래소 주문 API는 구현하지 않는다.
- 전략 성과 판단은 저장된 PAPER 데이터와 analytics 기준으로 한다.
- 새 DB 저장은 분석 가치가 명확할 때만 추가한다.

## 완료 항목 (상세 이력: CHANGELOG.md)

| 항목 | 상태 |
|---|---|
| 전략 성과 측정 API (winRate, profitLossRatio, averageHoldingSeconds) | 완료 |
| WebSocket reconnect/backoff 테스트 보강 | 완료 |
| PAPER 실행 runtime guard | 완료 |
| security lint 강화 | 완료 |
| Incident Log 문서 추가 | 완료 |
| API rate limit 운영 기준 문서화 | 완료 |
| PAPER 현금 부족 경고 | 완료 |

## 다음 설계 후보

### 후보 선정 수치 기록

목표: BUY 당시 전략 판단 수치를 나중에 역추적한다.

설계 후보:
- `paper_trade_log`에 strategy snapshot 컬럼 추가
- 별도 `candidate_scan_log` append-only 테이블 추가

보류 이유: 저장할 수치와 조회 방식이 아직 확정되지 않았다.

완료 기준:
- Candidates/History 대용량 방어 기준을 깨지 않는다.
- DB migration은 설계 확정 후 진행한다.

## 장기 보류

| 항목 | 보류 이유 |
|---|---|
| trailing stop / 시간 기반 청산 | 현재 고정 익절/손절 PAPER 데이터 2~3개월 필요 |
| 분할 청산 | 위와 동일 |
| 전략 plugin 구조 | 현재 `TradingStrategy` 경계가 유지되는 동안 불필요 |
| 실제 주문 API / `REAL_TRADING` | 영구 금지 (ADR-001 참고) |
| 수동 BUY | 영구 금지 (ADR-007 참고) |
