# Telegram UX

## 원칙

- Telegram은 운영 보조 UI다.
- 기본값은 비활성화다.
- configured chatId와 일치하는 요청만 처리한다.
- 기본 명령과 버튼은 조회 전용이다.
- 수동 PAPER 실행은 별도 설정이 켜진 경우에만 허용한다.
- 웹 선택 PAPER SELL 계획은 Telegram으로 확장하지 않는다.
- 실제 주문 API, `REAL_TRADING`, 거래소 인증키는 연결하지 않는다.

## 활성 조건

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

`/api/telegram/status`와 `/api/system/status`에서 enabled, configured, inbound 상태를 확인한다.

## 명령

- `/help`: 사용 가능한 명령 목록
- `/menu`: inline button 메뉴
- `/status`: 시스템 상태 요약
- `/auto`: 자동 실행 상태
- `/conditions`: 현재 매매 조건
- `/pnl`: 손익 요약
- `/candidates`: 롱 후보 목록 조회
- `/history KRW-BTC`: 최근 이력
- `/portfolio`: PAPER 포트폴리오 평가 요약
- `/positions`: 보유 포지션 목록
- `/risk`: 리스크 설정 요약
- `/safety`: kill switch 상태

`/run KRW-BTC`와 `/candidate-run KRW-BTC`는 호환성을 위해 파싱하지만 기본값에서는 실행하지 않는다.
`TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=true`일 때만 PAPER 실행 명령으로 동작한다.

## 버튼

- 상태 보기
- 자동 실행
- 매매 조건
- 손익
- 후보 보기
- BTC/ETH 이력
- 포트폴리오
- 보유 포지션
- 리스크
- 안전장치
- 도움말

## 실행 흐름

기본 메뉴에는 실행 버튼을 노출하지 않는다.
자동 실행은 scheduler가 담당한다.
조회 명령은 자동 실행 상태, 후보, 손익, 포트폴리오, history를 보여준다.

포트폴리오 선택 PAPER SELL은 웹 전용 계획이다. Telegram inline menu에는 선택 매도 버튼을 추가하지 않는다.

수동 PAPER 실행 설정이 켜진 경우에만 `/run`은 `TradingFlowService`를 호출하고, `/candidate-run`은 `CandidateExecutionService`를 호출한다.
후보 실행은 선택된 후보에 대해서만 PAPER BUY 주문을 만든다.
후보 조회와 후보 실행 결과는 한글 라벨로 짧게 요약한다.
kill switch가 켜져 있으면 실행 명령은 차단된다.
candidate scheduler 요약 알림은 별도 설정이 켜진 경우에만 발송된다.

## 보안

- 요청 chatId가 configured chatId와 다르면 처리하지 않는다.
- unauthorized 요청에는 거래 결과, history, status를 응답하지 않는다.
- Bot Token, Chat ID 원문은 메시지, 로그, API 응답에 포함하지 않는다.
- 기본값에서 Telegram은 수동 주문 실행 UI가 아니다.

## Polling

- Webhook이 아니라 getUpdates polling 방식이다.
- 기본 offset 저장소는 InMemory다.
- 앱 재시작 시 InMemory offset은 사라질 수 있다.
- JPA offset을 사용하면 재시작 후 중복 처리 가능성을 줄일 수 있다.
- callback query 처리 후 가능한 경우 `answerCallbackQuery`를 호출한다.
