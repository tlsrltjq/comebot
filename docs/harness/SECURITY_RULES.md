# Security Rules

## 원칙

- 민감 정보는 `.env` 또는 환경 변수로만 관리한다.
- Bot Token, Chat ID, DB 비밀번호, Access Key, Secret Key는 코드에 하드코딩하지 않는다.
- 민감 정보 원문은 로그와 API 응답에 포함하지 않는다.
- 실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.
- Binance를 추가하더라도 public market data만 사용하며 API Key, Secret Key 설정을 추가하지 않는다.
- WebSocket 시세 수신에도 인증키를 사용하지 않는다.

## 보안 린트

`SecurityLintTest`는 `gradlew test`에서 실행된다.

검사 항목:

- `.env`가 git에 추적되지 않는지
- 민감 설정이 환경 변수 placeholder를 사용하는지
- 민감 설정에 non-empty 기본값이 없는지
- Telegram Bot Token 형식 문자열이 추적 파일에 없는지
- private key, access key 형태 문자열이 없는지
- 로그에 token, chatId, password, secret, accessKey를 직접 출력하지 않는지
- Upbit 인증키 설정이 추가되지 않았는지
- `REAL_TRADING` 구현체가 추가되지 않았는지

## 실패 처리

보안 린트가 실패하면 커밋하지 않는다.

실패 원인을 제거한 뒤 전체 테스트를 다시 실행한다.
