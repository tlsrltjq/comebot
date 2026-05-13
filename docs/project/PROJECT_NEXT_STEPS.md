# Project Next Steps

이 문서는 다음 작업만 관리한다.
완료 이력은 Git history와 `docs/project/PROJECT_HISTORY.md`의 최근 항목을 기준으로 본다.

## 완료: Candidate 리스크 사유 서버 응답 구조화

목표:

- Candidates 화면이 reason 문자열 추론에 덜 의존하도록 서버 응답에 리스크 사유 타입을 추가한다.
- React는 여전히 거래/리스크 판단을 재구현하지 않는다.

작업:

- `TradingCandidateResponse`에 `reasonType`, `riskReasonType`을 추가한다.
- concentration/cooldown 사유를 enum 필드로 내려준다.
- backend/frontend 테스트를 추가한다.

완료 기준:

- 기존 candidate API 호환성이 깨지지 않는다.
- UI 경고가 BUY 실행 버튼처럼 보이지 않는다.
- 서버 리스크 판단과 프론트 표시가 분리된다.

## 다음 우선순위: Candidate 전체 조회 성능 정리

목표:

- `ALL_KRW` 후보 전체 조회가 운영 화면을 막지 않도록 한다.
- 자동 PAPER 실행 흐름과 수동 BUY 금지 원칙은 유지한다.

작업:

- `/api/candidates?exchange=upbit` 전체 스캔 타임아웃 원인을 분리한다.
- scheduler 최근 결과 재사용, limit, pagination, cache 중 최소 변경안을 고른다.
- 필요하면 Candidates 화면이 전체 동기 스캔 대신 최근 snapshot을 조회하게 한다.

완료 기준:

- 전체 후보 화면이 10초 이상 멈추지 않는다.
- BUY/SELL 주문 판단과 리스크 검증 로직은 변경하지 않는다.
- Upbit/Binance 응답이 섞이지 않는다.

## 보류

- 실제 주문 API와 `REAL_TRADING`
- 수동 BUY
- 완료된 Stage 계획 문서의 추가 상세화
