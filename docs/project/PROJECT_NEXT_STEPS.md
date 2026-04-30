# Project Next Steps

## 1단계: 후보 조회 REST API

목표:

- 롱 후보 스캐너 결과를 HTTP로 조회한다.
- 조회 API는 주문을 실행하지 않는다.

구현 후보:

- 후보 조회 REST API
- 후보 응답 DTO

## 2단계: Telegram 후보 조회

목표:

- Telegram에서 후보 목록을 확인한다.
- 후보 조회는 상태 변경 없는 조회 기능이다.

## 3단계: 롱 전용 진입 전략

목표:

- `SimpleThresholdStrategy`와 분리된 실제 PAPER 전략을 만든다.

구현 후보:

- `VolatilityBreakoutLongStrategy`
- 마켓별 전략 설정
- 전략 선택 설정

## 4단계: 자동 PAPER 운영

목표:

- scheduler로 후보 scan과 PAPER 실행을 자동화한다.
- 기본값은 계속 비활성이다.

검증:

- kill switch 차단
- history 저장
- portfolio 반영

## 보류: 실제 주문 API

실제 주문 API는 별도 승인 전까지 구현하지 않는다.
