# Project Next Steps

## 1단계: 롱 전용 진입 전략

목표:

- `SimpleThresholdStrategy`와 별도로 실전 검증용 PAPER 전략을 만든다.
- 변동성, 추세, 거래대금 조건을 조합해 BUY 신호를 만든다.
- 실제 주문 API는 만들지 않는다.

구현 후보:

- `VolatilityBreakoutLongStrategy`
- market별 전략 설정
- 전략 선택 설정
- 후보 스캔 결과와 전략 진입 조건 연결

## 2단계: 자동 PAPER 후보 실행

목표:

- scheduler로 후보 scan과 PAPER 실행을 자동화한다.
- 기본값은 계속 비활성화다.

검증:

- kill switch 차단
- risk 검증
- history 저장
- portfolio 반영

## 3단계: 운영 UX 정리

목표:

- Telegram 후보 조회와 실행 결과를 더 짧게 요약한다.
- 포트폴리오, 리스크, safety 상태를 한 화면에서 확인한다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
