# Project Next Steps

## 1단계: 롱 후보 스캐너

목표:

- 허용 market 목록을 순회하며 매수 후보를 만든다.
- 후보 생성은 주문 실행과 분리한다.

구현 후보:

- `strategy.candidate`
- `TradingCandidate`
- `CandidateScannerService`
- 후보 조회 REST API
- Telegram 후보 조회 명령

## 2단계: 롱 전용 진입 전략

목표:

- `SimpleThresholdStrategy`와 분리된 실제 PAPER 전략을 만든다.

구현 후보:

- `VolatilityBreakoutLongStrategy`
- 마켓별 전략 설정
- 전략 선택 설정

## 3단계: 자동 PAPER 운영

목표:

- scheduler로 후보 scan과 PAPER 실행을 자동화한다.
- 기본값은 계속 비활성이다.

검증:

- kill switch 차단
- history 저장
- portfolio 반영

## 보류: 실제 주문 API

실제 주문 API는 별도 승인 전까지 구현하지 않는다.
