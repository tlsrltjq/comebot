# Telegram UX

## 원칙

- Telegram은 운영 보조 UI다.
- 기본값은 비활성화다.
- configured chatId와 일치하는 요청만 처리한다.
- 기본 명령과 버튼은 조회 전용이다.
- 수동 PAPER 실행은 코드 레벨에서 차단한다.
- 웹 선택 PAPER SELL은 Telegram으로 확장하지 않는다.
- 실제 주문 API, `REAL_TRADING`, 거래소 인증키는 연결하지 않는다.

## 활성 조건

```properties
TELEGRAM_ENABLED=true
TELEGRAM_INBOUND_ENABLED=true
TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

`TELEGRAM_MANUAL_PAPER_EXECUTION_ENABLED`는 과거 호환 설정으로 남아 있지만, `/run`과 `/candidate-run`은 이 값과 무관하게 실행 서비스를 호출하지 않는다.

`/api/telegram/status`와 `/api/system/status`에서 enabled, configured, inbound 상태를 확인한다.

## 명령

- `/help`: 사용 가능한 명령 목록
- `/menu`: inline button 메뉴
- `/status`: 시스템 상태 요약
- `/auto`: 자동매매 상태
- `/conditions`: 현재 매매 조건
- `/pnl`: 손익 요약
- `/candidates`: 롱 후보 목록 조회
- `/history KRW-BTC`: 최근 이력
- `/portfolio`: PAPER 포트폴리오 평가 요약
- `/positions`: 보유 포지션 목록
- `/risk`: 리스크 설정 요약
- `/safety`: kill switch 상태

`/run KRW-BTC`와 `/candidate-run KRW-BTC`는 호환성을 위해 파싱하지만 PAPER 실행 명령으로 동작하지 않는다.

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

## 용어 기준

- 자동매매 상태는 전략 스케줄러, 후보 스케줄러, 청산 스케줄러를 분리해서 표시한다.
- 매매 조건은 거래 모드, 전략, 허용 마켓, 후보 범위, 스케줄러 주기, 1회 거래 금액, 익절/손절, 실제 주문 API 여부를 표시한다.
- 손익은 현금, 총 평가금, 실현 손익, 미실현 손익, 총 손익을 웹과 같은 이름으로 표시한다.
- 포트폴리오와 보유 포지션은 camelCase 필드명 대신 한글 라벨과 괄호 영문 라벨을 함께 사용한다.
- 리스크와 안전장치는 최대 주문 금액, 허용 마켓, 익절/손절, 청산 평가, 일일 리스크, 긴급 정지 라벨을 사용한다.

## 실행 흐름

기본 메뉴에는 실행 버튼을 노출하지 않는다.
자동 실행은 scheduler가 담당한다.
조회 명령은 자동매매 상태, 후보, 손익, 포트폴리오, history를 보여준다.
`/status`와 `/auto`는 candidate scheduler와 exit scheduler 거래소를 함께 보여준다.

포트폴리오 선택 PAPER SELL은 웹 전용 기능이다. Telegram inline menu에는 선택 매도 버튼을 추가하지 않는다.

`/run`과 `/candidate-run`은 항상 차단 메시지를 반환하고 `TradingFlowService` 또는 `CandidateExecutionService`를 호출하지 않는다.
후보 조회 결과는 한글 라벨로 짧게 요약한다.
candidate scheduler 요약 알림은 별도 설정이 켜진 경우에만 발송된다.

## 보안

- 요청 chatId가 configured chatId와 다르면 처리하지 않는다.
- unauthorized 요청에는 거래 결과, history, status를 응답하지 않는다.
- Bot Token, Chat ID 원문은 메시지, 로그, API 응답에 포함하지 않는다.
- Telegram은 수동 주문 실행 UI가 아니다.

## Polling

- Webhook이 아니라 getUpdates polling 방식이다.
- 기본 offset 저장소는 InMemory다.
- 앱 재시작 시 InMemory offset은 사라질 수 있다.
- JPA offset을 사용하면 재시작 후 중복 처리 가능성을 줄일 수 있다.
- callback query 처리 후 가능한 경우 `answerCallbackQuery`를 호출한다.
