# Reliability

## 기본 원칙

- 예외를 무시하지 않는다.
- 실패 원인을 로그 또는 응답으로 추적 가능하게 남긴다.
- 장애가 전체 애플리케이션 중단으로 이어지지 않게 경계를 둔다.
- Bot Token, Chat ID, DB 비밀번호, Access Key, Secret Key 원문을 로그와 응답에 노출하지 않는다.
- 보안 린트 실패는 배포 차단 사유로 본다.

## Market 장애

- Upbit API 실패는 명확한 실패로 처리한다.
- Binance API 장애는 Upbit 흐름을 중단시키지 않도록 거래소별 provider에서 격리한다.
- Upbit API 실패가 애플리케이션 전체를 중단시키면 안 된다.
- 시세 조회 실패 시 주문을 성공으로 처리하면 안 된다.
- InMemory, Upbit, Binance provider 동작을 분리해서 테스트한다.
- Upbit candle API 테스트는 mock/stub을 사용하고 외부 API를 직접 호출하지 않는다.
- Binance ticker/kline API 테스트는 mock/stub을 사용하고 외부 API를 직접 호출하지 않는다.
- WebSocket 수신 실패는 애플리케이션 전체를 중단시키면 안 된다.
- WebSocket reconnect는 backoff를 둔다.
- WebSocket snapshot이 stale이면 REST fallback을 사용하거나 stale 상태를 명확히 표시한다.
- WebSocket과 REST가 모두 실패하면 주문 실행을 차단하고 실패를 추적 가능하게 남긴다.
- `SNAPSHOT` provider는 fresh snapshot을 먼저 사용하고, 없거나 stale이면 거래소별 REST provider로 fallback한다.
- REST fallback도 실패하면 stale snapshot만으로 주문 가격을 만들지 않는다.
- WebSocket은 기본 disabled이며, `market.websocket.enabled=true`와 거래소별 `market.websocket.<exchange>.enabled=true`가 모두 켜진 경우에만 연결한다.
- Upbit 전체 KRW REST polling scheduler는 `market.upbit-krw-ticker-polling.enabled`로 제어한다.
  WebSocket/SNAPSHOT 기반 운영에서 명시 market만 구독하는 경우 polling을 꺼서 1초 전체 REST 호출을 피한다.

## Strategy 장애

- 전략 판단 실패는 주문 실행으로 이어지면 안 된다.
- HOLD와 실패를 혼동하지 않는다.
- 전략 실패가 포트폴리오 상태를 변경하면 안 된다.

## Risk 장애

- 리스크 검증 실패는 `REJECTED`로 처리한다.
- 리스크 실패 시 ExecutionGateway를 호출하지 않는다.
- 일일 제한 계산 실패 시 안전한 방향으로 차단한다.

## Execution 장애

- PAPER 주문 실패를 성공으로 처리하지 않는다.
- 실제 주문 API는 현재 구현하지 않는다.
- 주문 결과와 포트폴리오 반영은 분리해서 검증한다.

## Portfolio 장애

- 현금 부족 BUY는 거절한다.
- 보유 수량 부족 SELL은 거절한다.
- 웹 선택 PAPER SELL은 선택 market 중 일부가 실패해도 다른 market 결과를 추적 가능하게 반환한다.
- 포트폴리오 평가 실패는 조회 실패로 처리하고 상태를 변경하지 않는다.

## Telegram 장애

- Telegram sendMessage와 scheduler 요약 알림 실패는 trading result, history, portfolio, scheduler 결과를 변경하지 않는다.
- Telegram polling 실패는 다음 polling 주기에 복구 가능해야 한다.
- callback query는 가능한 경우 `answerCallbackQuery`로 로딩을 해제한다.
- callback answer 실패가 명령 처리 결과를 바꾸면 안 된다.
- `.env`에 token/chatId만 있어도 polling은 실행되지 않는다.
- `TELEGRAM_ENABLED=true`, `TELEGRAM_INBOUND_ENABLED=true`, configured 상태가 모두 필요하다.
- `api.telegram.org:443` 연결이 실패하면 getUpdates와 sendMessage가 모두 실패한다.

## Offset

- getUpdates offset은 update 처리 성공 후에만 증가시킨다.
- polling 실패 또는 명령 처리 실패 시 offset을 부적절하게 증가시키지 않는다.
- 기본 offset 저장소는 InMemory다.
- JPA offset 저장소 사용 시 `schema.sql` 적용이 필요하다.
- JPA offset 저장소는 재시작 후 중복 처리 가능성을 줄인다.

## DB 장애

- `/api/database/status`는 연결 실패 시 `connected=false`를 반환한다.
- DB 연결 실패가 system status API 전체를 500으로 만들면 안 된다.
- DB 비밀번호와 datasource URL 전체를 응답에 노출하지 않는다.

## Scheduler 장애
- market 단위 실패가 전체 scheduler를 중단시키면 안 된다.
- Scheduler에는 전략, 주문, 리스크 로직을 직접 넣지 않는다.
- candidate scheduler는 `CandidateExecutionService`만 호출하고 결과를 filled, rejected, hold, failed로 요약한다.
- exit scheduler는 `PositionExitExecutionService`만 호출하고 보유 position market만 평가한다.
- candidate scheduler, legacy trading scheduler, exit scheduler는 모두 `fixedDelay`와 내부 중복 실행 guard를 유지한다.
- exit scheduler HOLD는 기본적으로 history에 저장하지 않는다. BUY/SELL/REJECTED/FAILED는 추적 가능하게 저장한다.
