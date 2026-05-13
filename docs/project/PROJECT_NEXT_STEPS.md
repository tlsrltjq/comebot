# Project Next Steps

이 문서는 다음 작업만 관리한다.
완료 이력은 Git history와 `docs/project/PROJECT_HISTORY.md`의 최근 항목을 기준으로 본다.

## 다음 우선순위: Dashboard/Candidates 리스크 경고 요약

목표:

- Dashboard와 Candidates에서 쏠림/cooldown 상태를 운영자가 더 빨리 확인하게 한다.
- 서버 기준을 React에서 재구현하지 않고 risk/analytics API 응답을 표시한다.

작업:

- Dashboard에 concentration threshold와 stop-loss cooldown 설정 요약을 표시한다.
- Candidates에서 현재 보유/쏠림/cooldown 관련 warning 표시 범위를 확정한다.
- 필요한 경우 서버 API 응답을 확장하고 테스트를 추가한다.

완료 기준:

- UI 경고가 BUY 실행 버튼처럼 보이지 않는다.
- SELL, 익절, 손절 흐름은 변경하지 않는다.
- UPBIT/BINANCE 기준이 섞이지 않는다.

## 보류

- 실제 주문 API와 `REAL_TRADING`
- 수동 BUY
- 완료된 Stage 계획 문서의 추가 상세화
