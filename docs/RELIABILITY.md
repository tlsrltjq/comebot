# Reliability

## 기본 원칙

- 예외를 무시하지 않는다.
- 실패 사유를 로그 또는 알림으로 확인할 수 있어야 한다.
- 외부 API 장애가 전체 프로세스를 중단시키지 않도록 경계를 둔다.
- Bot Token, Chat ID, DB 비밀번호, Access Key, Secret Key 원문을 로그나 응답에 노출하지 않는다.

## 시세 수집

- 응답 누락, 지연, 비정상 가격을 검증한다.
- 오래된 시세는 전략 판단에 사용하지 않는다.
- 공급자 장애 시 재시도 또는 실패 상태를 기록한다.
- Upbit API 호출 실패는 명확한 실패로 처리한다.
- Upbit API 실패가 애플리케이션 전체를 중단시키면 안 된다.

## 주문 처리

- 페이퍼 주문도 실패 상태를 명확히 기록한다.
- 중복 처리 방지를 위해 주문 식별자를 사용한다.
- 재시도는 멱등성을 해치지 않아야 한다.
- 실제 주문 API는 아직 구현하지 않는다.

## 텔레그램 알림

- 알림 실패가 주문 상태를 성공으로 바꾸면 안 된다.
- 같은 이벤트에 대한 중복 알림을 제한한다.
- Telegram sendMessage 실패는 트레이딩 결과와 history 저장 결과를 바꾸면 안 된다.
- Telegram polling 실패는 다음 polling 주기에 복구 가능해야 한다.
- callback query는 가능한 경우 `answerCallbackQuery`를 호출해 버튼 로딩을 해제한다.
- callback answer 실패가 명령 처리 결과를 변경하면 안 된다.
- getUpdates offset은 update 처리 성공 후에만 저장한다.
- polling 실패 또는 명령 처리 실패 시 offset을 증가시키지 않는다.
- 기본 offset 저장소는 InMemory다.
- JPA offset 저장소 사용 시 앱 재시작 후에도 마지막 처리 offset을 유지해 중복 처리 가능성을 줄인다.
- JPA offset 저장소 사용 전 `schema.sql` 적용 여부를 확인한다.

## DB 연결

- DB 연결 상태 조회 실패는 `connected=false`로 응답한다.
- DB 연결 실패가 시스템 상태 API 전체를 500으로 만들면 안 된다.
- JPA history 사용 전 `schema.sql` 적용 여부를 확인한다.

## Scheduler

- Scheduler는 기본 비활성이다.
- Scheduler 실행 중 market 단위 실패가 전체 애플리케이션 중단으로 이어지면 안 된다.
- Scheduler에는 전략, 주문, 리스크 로직을 직접 넣지 않는다.
