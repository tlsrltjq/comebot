# Telegram UX

## 현재 단계

- Telegram 발송과 inbound polling 구조가 있다.
- 알림과 inbound polling은 기본 비활성이다.
- 알림 활성화 여부는 `notification.enabled` 설정으로 관리한다.
- 명령어 수신은 Webhook이 아니라 `getUpdates` polling 방식이다.
- Bot Token, Chat ID, Access Token은 코드에 하드코딩하지 않는다.
- 실제 텔레그램 전송 실패가 주문 성공/실패 상태를 바꾸면 안 된다.

## 목적

텔레그램은 봇 상태 확인, 페이퍼 주문 알림, 사용자 승인 또는 취소 흐름을 제공한다.

## 현재 지원 명령

- `/help`: 사용 가능한 명령어 목록 응답
- `/menu`: 인라인 버튼 메뉴 응답
- `/status`: telegram, notification, scheduler 상태 요약 응답
- `/run KRW-BTC`: 해당 market의 `PAPER_TRADING` 트레이딩 플로우 실행 후 결과 응답
- `/history KRW-BTC`: 해당 market의 최근 이력 요약 응답
- 알 수 없는 명령어: `/help` 안내 응답
- 명령어는 configured `telegram.chat-id`와 일치하는 채팅에서 온 경우만 처리한다.

## 버튼 흐름

- `/menu` 명령은 인라인 버튼 메뉴를 응답한다.
- `상태 보기`: `/status`와 같은 상태 요약을 응답한다.
- `KRW-BTC 실행`: `KRW-BTC` market으로 기존 `PAPER_TRADING` 트레이딩 플로우를 실행한다.
- `KRW-ETH 실행`: `KRW-ETH` market으로 기존 `PAPER_TRADING` 트레이딩 플로우를 실행한다.
- `BTC 이력 보기`: `KRW-BTC` 최근 이력 요약을 응답한다.
- `ETH 이력 보기`: `KRW-ETH` 최근 이력 요약을 응답한다.
- `도움말`: `/help`와 같은 안내를 응답한다.
- 알 수 없는 callback data는 `/help` 안내를 응답한다.
- 미허용 chatId의 callback은 기능을 실행하지 않는다.
- callback query는 버튼 로딩이 남지 않도록 가능한 경우 `answerCallbackQuery`로 응답한다.

## 금지 사항

- 버튼 클릭만으로 실제 주문을 실행하지 않는다.
- 민감 정보를 텔레그램 메시지에 노출하지 않는다.
- 실패한 주문을 성공 메시지로 안내하지 않는다.
- `telegram.enabled=true`, `telegram.inbound.enabled=true`, configured 상태가 아니면 polling을 실행하지 않는다.
- configured `telegram.chat-id`와 일치하지 않는 요청에는 거래 결과, history, status 정보를 응답하지 않는다.
- 버튼 callback으로도 실제 거래소 주문을 실행하지 않는다.

## 변경 규칙

명령어, 버튼, 메시지 흐름을 변경하면 이 문서를 함께 수정한다.
