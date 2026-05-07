# Exchange Dashboard Upgrade Plan

## 목표

Comebot 웹 대시보드를 `PAPER_TRADING` 범위 안에서 확장한다. Upbit 중심 구조를 유지하되, 거래소 모드, 선택 포지션 PAPER 매도, WebSocket 시세 수신, 자산 비중 차트, BTC 등락률 그래프를 단계적으로 추가한다.

## 고정 제약

- 기본 거래 모드는 `PAPER_TRADING`이다.
- `REAL_TRADING`은 구현하지 않는다.
- 실제 주문 API는 구현하지 않는다.
- Upbit/Binance 인증키, secret, token은 코드와 문서 예시에 넣지 않는다.
- 수동 BUY는 계속 금지한다.
- 수동 SELL은 사용자가 웹 포트폴리오에서 명시 선택한 보유 PAPER 포지션에만 허용한다.
- Telegram은 이번 계획에서 조회 전용으로 유지한다.

## 설계 가정

- 수동 PAPER SELL은 1차 구현에서 선택 market 전량 매도만 제공한다.
- 매도 가격과 수량 검증은 서버가 수행한다.
- kill switch가 켜져 있으면 수동 PAPER SELL도 차단한다. 손실 축소 목적의 reduce-only 예외는 별도 승인 후 검토한다.
- 거래소 모드는 `UPBIT`을 기본값으로 둔다.
- 새로고침 유지가 필요하면 localStorage 대신 URL query를 우선 사용한다.

## ExchangeMode

`ExchangeMode`는 `UPBIT`, `BINANCE` 두 값으로 시작한다. 프론트 사이드바 상단에 `UPBIT / BINANCE` 버튼을 두고, 선택값을 API 요청 파라미터와 백엔드 도메인 모델에 일관되게 전달한다.

같은 화면 구조를 유지하고 데이터 소스만 바꾼다.

- Dashboard
- Candidates
- Auto Run
- Portfolio
- History
- Market Overview

## WebSocket 시세 수신

시세 수신은 거래소별로 분리한다.

- Upbit WebSocket client
- Binance WebSocket client
- 거래소별 ticker snapshot store
- REST fallback provider

조회 우선순위는 다음과 같다.

1. WebSocket 최신 snapshot
2. 최근 snapshot 유지, stale 표시
3. REST fallback
4. 실패 응답, 주문 실행 차단

## 자동매매 주기 권장안

- WebSocket ticker: 지속 수신
- REST fallback polling: 장애 시 보조 경로
- 후보 탐색: 30초에서 60초
- 보유 포지션 exit 평가: 5초에서 10초
- market별 실행 간격: API 제한을 고려해 유지
- 반복 HOLD history는 과다 적재 방지 정책을 검토한다.

## UI 계획

- 사이드바 상단에 `UPBIT / BINANCE` mode switch를 둔다.
- 포트폴리오에는 선택 체크박스와 선택 PAPER 매도 toolbar를 둔다.
- 포트폴리오 metric 아래에 원형 그래프를 둔다.
- 원형 그래프는 현금 비중, market별 비중, 거래소별 비중 순으로 확장한다.
- Market Overview 페이지에 BTC 등락률 그래프를 둔다.
- Upbit 모드는 `KRW-BTC`, Binance 모드는 `BTCUSDT`를 사용한다.
- 범위는 `1h`, `24h`, `3d`, `7d`를 지원한다.

## Stage

1. 문서 제약 정리와 ExchangeMode 계획 문서화
2. ExchangeMode 백엔드 도메인/API 골격
3. 사이드바 ExchangeMode 버튼과 프론트 API 파라미터 준비
4. Binance REST market/candle provider 추가
5. Exchange별 PAPER 포트폴리오와 history 분리
6. 선택 포지션 PAPER 수동 매도
7. 포트폴리오 원형 그래프 UI
8. WebSocket 시세 수신 기반
9. 자동매매 주기 분리와 설정 정리
10. BTC 등락률 그래프 페이지

## Stage 1 완료 기준

- PAPER 선택 매도 예외와 실제 주문 금지가 문서상 충돌하지 않는다.
- ExchangeMode, WebSocket, REST fallback, scheduler 주기 방향이 문서화된다.
- 코드 구현은 하지 않는다.

## Stage 2 상세 계획: ExchangeMode 백엔드 도메인/API 골격

### 목표

백엔드가 거래소 모드를 이해할 수 있는 최소 골격을 만든다. 기존 Upbit 기반 API는 `exchange` 파라미터가 없어도 지금처럼 동작해야 한다.

### 포함 기능

- `ExchangeMode` enum 추가
  - 값: `UPBIT`, `BINANCE`
  - 기본값: `UPBIT`
  - query parameter parsing 지원
  - 잘못된 값은 400 응답 또는 명확한 validation error
- 거래소별 market symbol 용어 정리
  - Upbit: `KRW-BTC`
  - Binance: `BTCUSDT`
- exchange-aware 요청 모델 또는 helper 추가
  - controller가 `exchange` query parameter를 반복 파싱하지 않게 한다.
  - service 내부로 exchange 값을 전달할 수 있는 통로만 만든다.
- 기존 API 호환성 유지
  - `/api/system/status`
  - `/api/candidates`
  - `/api/portfolio/status`
  - `/api/portfolio/valuation`
  - `/api/trading-flow/history`
  - 위 API는 `exchange` 파라미터가 없어도 기존 Upbit 결과를 반환한다.

### 제외 기능

- Binance REST provider 구현
- Binance WebSocket 구현
- 포트폴리오를 거래소별로 분리
- history DB schema 변경
- 프론트 사이드바 mode switch
- 선택 PAPER SELL
- 실제 주문 API

### 백엔드 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/exchange/ExchangeMode.java` | 거래소 모드 enum |
| `src/main/java/com/giseop/comebot/exchange/ExchangeModeResolver.java` | query parameter 기본값/검증 처리 |
| `src/main/java/com/giseop/comebot/exchange/ExchangeSymbolMapper.java` | Upbit/Binance symbol 용어 분리 준비 |
| `src/main/java/com/giseop/comebot/system/controller/SystemStatusController.java` | `exchange` 파라미터 수용 준비 |
| `src/main/java/com/giseop/comebot/strategy/controller/CandidateController.java` | `exchange` 파라미터 수용 준비 |
| `src/main/java/com/giseop/comebot/portfolio/controller/PortfolioStatusController.java` | `exchange` 파라미터 수용 준비 |
| `src/main/java/com/giseop/comebot/history/controller/TradingFlowHistoryController.java` | `exchange` 파라미터 수용 준비 |

### API 설계

Stage 2에서는 기존 응답 구조를 바꾸지 않는다. 요청 파라미터만 준비한다.

```http
GET /api/candidates?exchange=UPBIT
GET /api/portfolio/valuation?exchange=UPBIT
GET /api/trading-flow/history?exchange=UPBIT&limit=20
```

Stage 2에서 `exchange=BINANCE`는 아직 실제 Binance 데이터를 반환하지 않는다. 선택지는 둘 중 하나로 한다.

1. `501 Not Implemented`로 명확히 반환
2. 아직 provider가 없는 상태이므로 `400 Bad Request`와 지원 전 메시지 반환

권장안은 `501 Not Implemented`다. 잘못된 값과 아직 미지원인 값을 분리할 수 있기 때문이다.

### 테스트

- `ExchangeMode` parsing 테스트
- `exchange` 누락 시 `UPBIT` 기본값 테스트
- 소문자/공백 입력 처리 기준 테스트
- 잘못된 exchange 값은 실패하는 테스트
- 기존 API가 `exchange` 없이 기존처럼 동작하는 controller 테스트
- `exchange=UPBIT`도 기존과 같은 결과를 내는 controller 테스트
- `exchange=BINANCE`는 아직 미지원 응답을 내는 controller 테스트

### 완료 기준

- 기존 Upbit API 호출이 깨지지 않는다.
- 모든 신규 exchange parsing은 테스트된다.
- Binance provider, WebSocket, 포트폴리오 분리 코드는 아직 없다.
- 실제 주문 API와 `REAL_TRADING`은 추가되지 않는다.
- `./gradlew test`가 성공한다.

### 리스크

- controller마다 파라미터 처리를 직접 넣으면 중복이 생긴다.
- `BINANCE`를 조기 허용하면 실제 데이터가 없는데 화면이 성공처럼 보일 수 있다.
- 기존 프론트가 `exchange` 없이 호출하므로 하위 호환성이 필수다.

### 사용자 확인 필요

- `exchange=BINANCE` 미지원 응답은 `501 Not Implemented`로 해도 되는지 확인 필요.
- `exchange` query 값은 대문자만 허용할지, `upbit` 같은 소문자도 허용할지 확인 필요.
