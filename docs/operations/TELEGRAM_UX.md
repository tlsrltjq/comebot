# Telegram UX

## 원칙

- Telegram은 운영 보조 UI다.
- 기본값은 비활성화다.
- configured chatId와 일치하는 요청만 처리한다.
- 명령과 버튼은 설정 조회 또는 PAPER_TRADING 실행만 수행한다.
- 실제 주문 API, `REAL_TRADING`, 거래소 인증키는 연결하지 않는다.

## 활성 조건

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

`/api/telegram/status`에서 `enabled=true`, `configured=true`여야 한다.
`/api/system/status`에서 `telegram.inboundEnabled=true`여야 명령을 읽는다.

## 명령

- `/help`: 사용 가능한 명령 목록
- `/menu`: inline button 메뉴
- `/status`: 시스템 상태 요약
- `/candidates`: 롱 후보 목록 조회
- `/candidate-run KRW-BTC`: 후보 PAPER 실행
- `/run KRW-BTC`: 기존 트레이딩 플로우 실행
- `/history KRW-BTC`: 최근 실행 이력
- `/portfolio`: PAPER 포트폴리오 평가 요약
- `/positions`: 보유 포지션 목록
- `/risk`: 리스크 설정 요약
- `/safety`: kill switch 상태

## 버튼

- 상태 보기
- 후보 보기
- BTC 후보 실행
- ETH 후보 실행
- KRW-BTC 실행
- KRW-ETH 실행
- BTC 이력
- ETH 이력
- 포트폴리오
- 보유 포지션
- 리스크
- 안전장치
- 도움말

## 실행 흐름

`/run`과 RUN 버튼은 `TradingFlowService`만 호출한다.
`/candidate-run`과 후보 실행 버튼은 `CandidateExecutionService`만 호출한다.
후보 실행은 선택된 후보에 대해서만 PAPER BUY 주문을 만든다.
kill switch가 켜져 있으면 실행 명령은 차단된다.

## 보안

- 요청 chatId가 configured chatId와 다르면 처리하지 않는다.
- unauthorized 요청에는 거래 결과, history, status를 응답하지 않는다.
- Bot Token, Chat ID 원문은 메시지, 로그, API 응답에 포함하지 않는다.

## Polling

- Webhook이 아니라 getUpdates polling 방식이다.
- 기본 offset 저장소는 InMemory다.
- 앱 재시작 시 InMemory offset은 사라질 수 있다.
- JPA offset을 사용하면 재시작 후 중복 처리 가능성을 줄일 수 있다.
- callback query 처리 후 가능한 경우 `answerCallbackQuery`를 호출한다.
