# Order Lifecycle

## 실행 결과 저장

- Trading Flow 실행 결과는 이력 저장소에 저장한다.
- HOLD, REJECTED, FILLED 결과를 모두 저장한다.
- 기본 이력 저장소는 InMemory이며 애플리케이션 재시작 시 사라진다.
- 설정에 따라 PostgreSQL/JPA 저장소를 사용할 수 있다.
- 저장 이후 최근 이력 또는 단건 이력 조회 API로 확인할 수 있다.

## 초기 단계

- `PRICE_CAPTURED`: 테스트용 시세 공급자가 시세를 제공한 단계
- `SIGNAL_DETECTED`: 전략 또는 position exit 정책이 BUY, SELL, HOLD 신호를 만든 단계
- `REQUESTED`: BUY 또는 SELL 신호가 주문 요청으로 변환된 상태
- `REJECTED`: 입력값 또는 리스크 검증에서 거절된 상태
- `FILLED`: 페이퍼 트레이딩 주문이 체결 처리된 상태
- `FAILED`: 주문 처리 중 실패한 상태

## 기본 흐름

1. Kill switch 검증
2. 테스트용 시세 조회
3. 테스트용 전략 판단
4. 기존 전략이 HOLD이고 position exit 설정이 활성화되어 있으면 보유 포지션 손절/익절 조건 평가
5. `SIGNAL_DETECTED` 생성
6. HOLD 신호면 주문 요청을 만들지 않고 흐름 종료
7. BUY 또는 SELL 신호면 주문 요청 생성
8. 기본 리스크 검증
9. 일일 리스크 검증
10. 포트폴리오 사전 검증
11. 현금 부족 BUY 또는 보유 수량 부족 SELL이면 `REJECTED` 결과 생성
12. 리스크와 포트폴리오 검증 승인 시 페이퍼 실행 게이트웨이 호출
13. 리스크 거절 시 게이트웨이를 호출하지 않고 `REJECTED` 결과 생성
14. `FILLED`, `REJECTED`, `FAILED` 중 하나로 결과 생성
15. `FILLED` 결과만 포트폴리오에 반영
16. 실행 결과 history 저장
17. 알림 설정과 필터 정책을 통과하면 optional notification 실행

## Kill Switch 처리

- kill switch가 활성화되면 시세 조회, 전략 판단, 주문 생성 전에 실행을 차단한다.
- 차단 결과는 `REJECTED` 상태와 명확한 메시지로 기록한다.
- status, history, portfolio 조회는 차단하지 않는다.

## HOLD 처리

- HOLD 신호는 주문 요청으로 변환하지 않는다.
- HOLD 신호는 리스크 검증이나 주문 실행으로 이어지지 않는다.
- HOLD 결과는 주문 없음 상태로 기록한다.
- HOLD 이력 저장 후 알림 필터 정책에 따라 알림 여부를 판단한다.

## SIGNAL_DETECTED 처리

- BUY 신호는 매수 주문 요청으로 변환할 수 있다.
- SELL 신호는 매도 주문 요청으로 변환할 수 있다.
- HOLD 신호는 주문 요청으로 변환하지 않는다.
- signalType이 null이면 주문 요청으로 변환하지 않는다.
- Position exit 정책은 활성화된 경우에만 손절/익절 SELL 신호를 만들 수 있다.
- Position exit SELL 수량은 보유 수량을 초과하지 않는다.
- Position exit 정책은 실제 주문 API가 아니라 기존 `PAPER_TRADING` 주문 흐름으로만 이어진다.

## 리스크 검증 단계

- null 요청은 `REJECTED` 결과를 반환한다.
- 잘못된 마켓, 주문 방향, 수량, 가격은 `REJECTED` 결과를 반환한다.
- 최대 주문 금액 초과는 `REJECTED` 결과를 반환한다.
- 허용되지 않은 마켓은 `REJECTED` 결과를 반환한다.
- 일일 리스크 제한이 활성화된 경우 주문 실행 전에 오늘 FILLED 주문 수와 오늘 실현 손실을 검증한다.
- 오늘 FILLED 주문 수가 제한 이상이면 `REJECTED` 결과를 반환한다.
- 오늘 실현 손실이 제한 이상이면 `REJECTED` 결과를 반환한다.
- HOLD, REJECTED, FAILED 결과는 일일 주문 횟수 제한에 포함하지 않는다.

## 페이퍼 트레이딩 처리

- 리스크 검증을 통과한 유효한 주문 요청은 `FILLED` 결과를 반환한다.
- 실제 거래소 주문 API는 호출하지 않는다.
- BUY `FILLED` 결과는 페이퍼 현금을 차감하고 보유 수량과 평균 매수가를 갱신한다.
- SELL `FILLED` 결과는 보유 수량을 차감하고 실현 손익을 계산한다.
- `REJECTED`, `FAILED`, HOLD 결과는 포트폴리오를 변경하지 않는다.

## 처리 원칙

- 실패 상태를 성공 상태로 변환하지 않는다.
- 거절 또는 실패 사유를 메시지로 남긴다.
- `REAL_TRADING` 상태 흐름은 별도 승인 전까지 정의하지 않는다.

## 변경 규칙

주문 상태나 전이 흐름을 변경하면 이 문서와 관련 테스트를 함께 수정한다.
