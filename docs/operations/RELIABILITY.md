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
- REST fallback 실패는 주문 성공으로 처리하지 않고 실패 상태 또는 조회 실패로 드러낸다.
- WebSocket은 기본 disabled이며, `market.websocket.enabled=true`와 거래소별 `market.websocket.<exchange>.enabled=true`가 모두 켜진 경우에만 연결한다.
- Upbit 전체 KRW REST polling scheduler는 `ALL_KRW` 후보 universe bootstrap/refresh 용도이며 가격 수집 경로가 아니다.
  기본값은 부팅 시 1회와 10분 간격 refresh다.
  WebSocket/SNAPSHOT 기반 운영에서 명시 market만 구독하는 경우 `market.upbit-krw-ticker-polling.enabled=false`로 끌 수 있다.
- Binance 전체 USDT REST polling scheduler는 `ALL_USDT` 후보 universe bootstrap/refresh 용도이며 가격 수집 경로가 아니다.
  기본값은 disabled다.
  Binance 전체 후보 테스트가 필요할 때만 `market.binance-usdt-ticker-polling.enabled=true`로 켜고, 실패해도 Upbit 흐름을 중단시키지 않는다.

## Strategy 장애

- 전략 판단 실패는 주문 실행으로 이어지면 안 된다.
- HOLD와 실패를 혼동하지 않는다.
- 전략 실패가 포트폴리오 상태를 변경하면 안 된다.
- candidate scheduler는 `trading.candidate-scheduler.exchange`를 사용해 후보 스캔, PAPER 주문, history 저장 거래소를 일치시킨다.
- Binance candidate 실행 실패는 Binance run summary의 failed count로 남기고 Upbit scheduler/provider 흐름을 중단시키지 않는다.

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

## Public API Rate Limit

- 거래소 공개 API 제한을 넘기지 않도록 scheduler 주기와 universe 크기를 함께 본다.
- WebSocket fresh snapshot이 있는 market은 REST fallback을 호출하지 않는 것이 정상이다.
- stale snapshot, missing snapshot, WebSocket disabled 상태에서는 REST fallback 호출이 늘어난다.
- candidate scheduler는 후보 universe 크기와 candle/price 조회 방식에 따라 호출량이 커질 수 있다.
- exit scheduler는 보유 PAPER position market만 평가하므로 보유 position 수가 호출량 상한이다.
- `ALL_KRW`와 `ALL_USDT`를 동시에 켜고 candidate 주기를 30초로 낮출 때는 429, failed summary, provider status를 먼저 확인한다.
- 429 또는 REST fallback 실패가 발생하면 scheduler 결과는 failed/rejected로 남아야 하며, stale 가격으로 FILLED 주문을 만들면 안 된다.
- 반복 장애는 `docs/operations/INCIDENT_LOG.md`에 기록한다.

## System 화면 장애

- System 화면은 `/api/system/status`와 `/api/market-provider/status` 조회 결과만 표시하고 상태를 변경하지 않는다.
- OS 감지 실패 또는 알 수 없는 OS는 실행 안내 표시만 보수적으로 바꾸며 거래 기능에는 영향을 주지 않는다.
- provider status 조회가 실패해도 시스템 조회 자체가 민감 정보나 실제 주문 설정을 노출하면 안 된다.

## Web API polling

- 웹은 활성 탭에서만 주기 polling을 수행하고, 백그라운드 탭에서는 polling을 중단한다.
- 빠른 상태 확인이 필요한 Top status bar, Dashboard, Portfolio, Auto Run만 5~15초 범위로 갱신한다.
- Risk/System 같은 읽기 전용 설정 화면과 Market chart는 30초 주기로 낮춘다.
- history와 analytics는 분리해서 history는 10초, analytics는 15초 주기로 조회한다.
- polling 주기를 줄여도 자동 PAPER 실행, exit scheduler, WebSocket 시세 수신 주기는 변경하지 않는다.

## 대용량 조회 방어

- Candidates 전체 조회는 서버에서 최대 50개로 제한하고, market 단건 조회는 명시 검색일 때만 수행한다.
- History 조회는 서버에서 최대 200개로 제한한다.
- 웹 Candidates/History 화면은 market 입력 중 매 키마다 API를 호출하지 않고 `조회(Search)` 제출 시에만 필터를 적용한다.
- History analytics는 history row를 프론트에서 재집계하지 않고 서버 analytics API 결과를 사용한다.
