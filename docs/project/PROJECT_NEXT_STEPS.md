# Project Next Steps

이 문서는 다음 작업만 관리한다.
완료 이력은 Git history와 `docs/project/PROJECT_HISTORY.md`의 최근 항목을 기준으로 본다.

## 완료: Candidate 전체 조회 성능 정리

목표:

- `ALL_KRW` 후보 전체 조회가 운영 화면을 막지 않도록 한다.
- 자동 PAPER 실행 흐름과 수동 BUY 금지 원칙은 유지한다.

작업:

- `/api/candidates?exchange=upbit` 전체 스캔 타임아웃 원인을 분리한다.
- Candidates API에 제한된 전체 조회 기본값과 짧은 TTL 캐시를 둔다.
- Candidates 화면은 전체 동기 스캔 대신 상위 후보 제한 조회를 사용한다.

완료 기준:

- 후보 화면이 10초 이상 멈추지 않는다.
- BUY/SELL 주문 판단과 리스크 검증 로직은 변경하지 않는다.
- Upbit/Binance 응답이 섞이지 않는다.

## 다음 우선순위: PAPER 포지션 청산 흐름 스모크 테스트

목표:

- 현재 쌓인 PAPER 포지션으로 익절/손절/선택 PAPER SELL 흐름을 검증한다.
- 실제 주문 API와 `REAL_TRADING` 없이 PAPER 상태 전이만 확인한다.

작업:

- `/api/portfolio/valuation`, `/api/analytics/losses`, `/api/trading-flow/history` 상태를 기준선으로 기록한다.
- 손절/익절 조건 도달 포지션의 exit scheduler 처리 결과를 확인한다.
- 웹 선택 PAPER SELL이 선택한 보유 포지션에만 적용되는지 재확인한다.

완료 기준:

- 실패한 주문을 성공으로 처리하지 않는다.
- PAPER 현금/포지션/history가 서로 일치한다.
- 실제 주문 API와 수동 BUY는 추가되지 않는다.

## 보류

- 실제 주문 API와 `REAL_TRADING`
- 수동 BUY
- 완료된 Stage 계획 문서의 추가 상세화
