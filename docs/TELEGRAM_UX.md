# Telegram UX

## 현재 단계

- Telegram 발송과 inbound polling 구조가 있다.
- 알림과 inbound polling은 기본 비활성이다.
- 알림 활성화 여부는 `notification.enabled` 설정으로 관리한다.
- 명령어 수신은 Webhook이 아니라 `getUpdates` polling 방식이다.
- getUpdates offset은 `TelegramUpdateOffsetRepository`로 관리한다.
- 기본 offset 저장소는 InMemory 구현이다.
- `telegram.inbound.offset-storage-type=JPA` 설정 시 PostgreSQL에 offset을 저장할 수 있다.
- 처리 성공 후 `updateId + 1`을 저장한다.
- polling 또는 명령 처리 실패 시 offset을 증가시키지 않는다.
- InMemory 사용 시 애플리케이션 재시작 후 일부 update가 중복 처리될 수 있다.
- JPA 사용 시 재시작 후에도 마지막 offset을 유지해 중복 처리 가능성을 줄인다.
- Bot Token, Chat ID, Access Token은 코드에 하드코딩하지 않는다.
- 실제 텔레그램 전송 실패가 주문 성공/실패 상태를 바꾸면 안 된다.

## 목적

텔레그램은 봇 상태 확인, 페이퍼 주문 알림, 사용자 승인 또는 취소 흐름을 제공한다.

## 현재 지원 명령

- `/help`: 사용 가능한 명령어 목록 응답
- `/menu`: 인라인 버튼 메뉴 응답
- `/status`: DB, Market Provider, Strategy, Risk, Scheduler, Notification, Telegram 상태 요약 응답
- `/run KRW-BTC`: 해당 market의 `PAPER_TRADING` 트레이딩 플로우 실행 후 결과 응답
- `/history KRW-BTC`: 해당 market의 최근 이력 요약 응답
- `/portfolio`: PAPER_TRADING 포트폴리오 현금, 평가자산, 손익 요약 응답
- `/positions`: PAPER_TRADING 보유 포지션 목록 응답
- 알 수 없는 명령어: `/help` 안내 응답
- 명령어는 configured `telegram.chat-id`와 일치하는 채팅에서 온 경우만 처리한다.

## 버튼 흐름

- `/menu` 명령은 인라인 버튼 메뉴를 응답한다.
- `상태 보기`: `/status`와 같은 상태 요약을 응답한다.
- `KRW-BTC 실행`: `KRW-BTC` market으로 기존 `PAPER_TRADING` 트레이딩 플로우를 실행한다.
- `KRW-ETH 실행`: `KRW-ETH` market으로 기존 `PAPER_TRADING` 트레이딩 플로우를 실행한다.
- `BTC 이력 보기`: `KRW-BTC` 최근 이력 요약을 응답한다.
- `ETH 이력 보기`: `KRW-ETH` 최근 이력 요약을 응답한다.
- `포트폴리오 보기`: `/portfolio`와 같은 포트폴리오 요약을 응답한다.
- `보유 포지션 보기`: `/positions`와 같은 보유 포지션 목록을 응답한다.
- `도움말`: `/help`와 같은 안내를 응답한다.
- 알 수 없는 callback data는 `/help` 안내를 응답한다.
- 미허용 chatId의 callback은 기능을 실행하지 않는다.
- callback query는 버튼 로딩이 남지 않도록 가능한 경우 `answerCallbackQuery`로 응답한다.

## Portfolio 표시 항목

- 현금
- 총 평가자산
- 실현손익
- 미실현손익
- 총손익
- 보유 포지션 market, 수량, 평균 매수가
- 현재가 조회 실패 시 실패 사유를 안내한다.
- 포트폴리오 조회는 상태 변경 없는 조회 기능이다.

## Status 표시 항목

- DB 연결 여부
- Market Provider
- Strategy 이름
- Buy/Sell 기준가
- 주문 수량
- 최대 주문 금액
- 허용 Market
- Scheduler enabled
- Notification enabled
- Telegram enabled
- Telegram inbound enabled
- Bot Token, Chat ID, DB 비밀번호 원문은 표시하지 않는다.

## 금지 사항

- 버튼 클릭만으로 실제 주문을 실행하지 않는다.
- 민감 정보를 텔레그램 메시지에 노출하지 않는다.
- 실패한 주문을 성공 메시지로 안내하지 않는다.
- `telegram.enabled=true`, `telegram.inbound.enabled=true`, configured 상태가 아니면 polling을 실행하지 않는다.
- configured `telegram.chat-id`와 일치하지 않는 요청에는 거래 결과, history, status 정보를 응답하지 않는다.
- 버튼 callback으로도 실제 거래소 주문을 실행하지 않는다.

## 변경 규칙

명령어, 버튼, 메시지 흐름을 변경하면 이 문서를 함께 수정한다.
