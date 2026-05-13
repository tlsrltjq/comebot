# Stage 2: Backend ExchangeMode Skeleton

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

백엔드가 거래소 모드를 이해할 수 있는 최소 골격을 만든다. 기존 Upbit 기반 API는 `exchange` 파라미터가 없어도 지금처럼 동작해야 한다.

## 포함 기능

- `ExchangeMode` enum 추가
  - 값: `UPBIT`, `BINANCE`
  - 기본값: `UPBIT`
  - query parameter parsing 지원
  - `upbit`, `binance` 같은 소문자 입력 허용
  - 잘못된 값은 `400 Bad Request`
- `exchange=binance` 미구현 응답은 `501 Not Implemented`
- 거래소별 market symbol 용어 정리
  - Upbit: `KRW-BTC`
  - Binance: `BTCUSDT`
- exchange-aware 요청 helper 추가
- 기존 API 호환성 유지
  - `/api/system/status`
  - `/api/candidates`
  - `/api/portfolio/status`
  - `/api/portfolio/valuation`
  - `/api/trading-flow/history`

## 제외 기능

- Binance REST provider 구현
- Binance WebSocket 구현
- 포트폴리오 거래소별 분리
- history DB schema 변경
- 프론트 사이드바 mode switch
- 선택 PAPER SELL
- 실제 주문 API

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/exchange/ExchangeMode.java` | 거래소 모드 enum |
| `src/main/java/com/giseop/comebot/exchange/ExchangeModeResolver.java` | query parameter 기본값/검증 |
| `src/main/java/com/giseop/comebot/exchange/ExchangeSymbolMapper.java` | 거래소별 symbol 용어 분리 준비 |
| `src/main/java/com/giseop/comebot/system/controller/SystemStatusController.java` | `exchange` 파라미터 수용 |
| `src/main/java/com/giseop/comebot/strategy/controller/CandidateController.java` | `exchange` 파라미터 수용 |
| `src/main/java/com/giseop/comebot/portfolio/controller/PortfolioStatusController.java` | `exchange` 파라미터 수용 |
| `src/main/java/com/giseop/comebot/history/controller/TradingFlowHistoryController.java` | `exchange` 파라미터 수용 |

## API 설계

```http
GET /api/candidates?exchange=upbit
GET /api/portfolio/valuation?exchange=upbit
GET /api/trading-flow/history?exchange=upbit&limit=20
```

`exchange=binance`는 Stage 4 전까지 실제 Binance 데이터를 반환하지 않고 `501 Not Implemented`로 응답한다.

## 테스트

- `ExchangeMode` parsing
- exchange 누락 시 `UPBIT` 기본값
- 소문자 입력 허용
- 잘못된 exchange 값은 400
- 기존 API가 exchange 없이 기존처럼 동작
- `exchange=upbit`도 기존과 같은 결과
- `exchange=binance`는 미지원 응답
- `./gradlew test`

## 완료 기준

- 기존 Upbit API 호출이 깨지지 않는다.
- 모든 신규 exchange parsing은 테스트된다.
- Binance provider, WebSocket, 포트폴리오 분리 코드는 아직 없다.
- 실제 주문 API와 `REAL_TRADING`은 추가되지 않는다.

## 구현 결과

- `ExchangeMode` enum을 추가했다.
- `exchange` query parameter는 누락 시 `UPBIT`으로 처리한다.
- `upbit`, `binance` 같은 소문자 입력을 허용한다.
- 잘못된 exchange 값은 `400 Bad Request`로 처리한다.
- Stage 2 기준 `exchange=binance`는 `501 Not Implemented`로 처리한다.
- 기존 Upbit API는 `exchange` 없이 기존 응답을 유지한다.
- Binance REST provider, WebSocket, 거래소별 portfolio/history 분리는 아직 추가하지 않았다.
