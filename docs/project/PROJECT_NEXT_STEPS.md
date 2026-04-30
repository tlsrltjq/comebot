# Project Next Steps

## 1단계: Telegram 자동 실행 요약

목표:

- candidate scheduler 실행 요약을 Telegram 알림으로 보낼지 정책을 분리한다.
- 기본값은 비활성화한다.
- 주문 결과와 history 저장 결과는 바꾸지 않는다.

## 2단계: 중복 진입 제한 강화

목표:

- 보유 포지션뿐 아니라 최근 실행 이력을 기준으로 재진입 쿨다운을 둔다.
- market별 쿨다운 설정을 지원한다.

## 3단계: 운영 UX 정리

목표:

- Telegram 후보 조회와 실행 결과를 더 짧게 요약한다.
- 포트폴리오, 리스크, safety 상태를 한 화면에서 확인한다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
