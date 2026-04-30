# Project Next Steps

## 1단계: 자동 PAPER 후보 실행

목표:

- scheduler로 후보 scan과 PAPER 실행을 자동화한다.
- 기본값은 계속 비활성화다.

검증:

- kill switch 차단
- risk 검증
- history 저장
- portfolio 반영

## 2단계: 전략 조건 고도화

목표:

- market별 전략 설정을 분리한다.
- 급등 과열 회피 조건을 추가한다.
- 진입 후 재진입 제한 조건을 추가한다.

## 3단계: 운영 UX 정리

목표:

- Telegram 후보 조회와 실행 결과를 더 짧게 요약한다.
- 포트폴리오, 리스크, safety 상태를 한 화면에서 확인한다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
