# Project Next Steps

## 1단계: scheduler 요약과 history 기록 점검

목표:

- candidate scheduler 요약 수치와 history 저장 결과가 서로 맞는지 점검한다.
- FILLED, REJECTED, HOLD, FAILED 케이스별 검증을 보강한다.
- 알림 실패가 실행 결과와 history를 바꾸지 않는지 유지한다.

## 2단계: 중복 진입 제한 강화

목표:

- 보유 포지션뿐 아니라 최근 실행 이력을 기준으로 재진입 쿨다운을 둔다.
- market별 쿨다운 설정을 지원한다.

## 3단계: 운영 상태 화면 정리

목표:

- status API와 Telegram 상태 메시지의 항목 이름을 맞춘다.
- 설정 변경 기능은 만들지 않는다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
