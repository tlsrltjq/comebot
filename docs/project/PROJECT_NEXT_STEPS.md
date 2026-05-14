# Project Next Steps

이 문서는 다음 작업만 관리한다.
완료 이력은 Git history와 `docs/project/PROJECT_HISTORY.md`의 최근 항목을 기준으로 본다.

## 완료: Dashboard 데이터 준비 상태 표시

목표:

- 운영 정상 여부와 데이터 충분 여부를 대시보드에서 분리해서 보여준다.
- 데이터가 쌓이지 않은 상태에서는 손익 판단을 보류해야 함을 명확히 표시한다.

작업:

- 24시간 실행, 체결, BUY/SELL, 보유 포지션, 청산 대상 수를 한 화면에 표시한다.
- `데이터 부족`, `수집 중`, `진입 검증 중`, `검증 가능` 상태를 구분한다.
- 운영 준비 상태와 데이터 준비 상태를 각각 테스트한다.

완료 기준:

- PAPER 주문 실행 로직은 변경하지 않는다.
- 데이터가 없어도 화면이 정상 렌더링된다.
- 데이터 부족 상태가 매수/매도 실행 버튼을 만들지 않는다.

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
