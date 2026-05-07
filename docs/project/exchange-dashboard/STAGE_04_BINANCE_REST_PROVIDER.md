# Stage 4: Binance REST Provider

## 목표

Binance 공개 REST API로 현재가와 캔들 데이터를 가져오는 provider를 추가한다. 기존 Upbit provider 구조를 유지하면서 `ExchangeMode=BINANCE`일 때 Binance 데이터를 사용할 수 있는 백엔드 기반을 만든다.

## 확정 범위

- Binance는 1차로 `USDT` 현물 마켓만 지원한다.
- 초기 종목은 Upbit KRW 후보와 매핑 가능한 코인으로 제한한다.
- 예: `KRW-BTC` -> `BTCUSDT`, `KRW-ETH` -> `ETHUSDT`
- Binance 인증키, secret, 계정 API는 사용하지 않는다.

## 포함 기능

- Binance 현재가 REST provider
- Binance 캔들 REST provider
- Binance symbol 형식 지원: `BTCUSDT`, `ETHUSDT`
- Upbit/Binance symbol 변환 준비
- Stage 2의 `ExchangeMode`와 provider 선택 연결
- `exchange=binance` 요청 시 조회 가능한 API부터 `501` 대신 실제 응답 반환
- REST API 실패 시 애플리케이션이 죽지 않도록 예외 처리
- provider 단위 테스트

## 제외 기능

- Binance WebSocket
- Binance 실제 주문 API
- Binance API Key/Secret
- 선물, 숏, 레버리지
- 거래소별 portfolio/history DB 분리
- 자동매매를 Binance로 완전히 실행
- 프론트 대규모 UI 변경

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/market/provider/BinanceMarketPriceProvider.java` | Binance 현재가 조회 provider |
| `src/main/java/com/giseop/comebot/market/candle/provider/BinanceCandleProvider.java` | Binance 캔들 조회 provider |
| `src/main/java/com/giseop/comebot/market/provider/MarketPriceProviderType.java` | `BINANCE` provider type 추가 여부 검토 |
| `src/main/java/com/giseop/comebot/exchange/ExchangeSymbolMapper.java` | Upbit/Binance symbol 변환 |
| `src/main/java/com/giseop/comebot/strategy/controller/CandidateController.java` | Binance candidate 조회 연결 |
| `src/main/java/com/giseop/comebot/portfolio/controller/PortfolioStatusController.java` | Binance valuation 조회 제한 또는 연결 준비 |
| `src/main/java/com/giseop/comebot/history/controller/TradingFlowHistoryController.java` | Stage 5 전까지 Binance history 제한 |
| `src/test/java/com/giseop/comebot/market/provider/BinanceMarketPriceProviderTest.java` | 현재가 provider 테스트 |
| `src/test/java/com/giseop/comebot/market/candle/provider/BinanceCandleProviderTest.java` | 캔들 provider 테스트 |

## Binance API 설계

현재가:

```http
GET https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT
```

캔들:

```http
GET https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=30
```

내부 모델:

- 현재가는 기존 `MarketPrice`로 변환
- 캔들은 기존 `Candle`로 변환
- 외부 응답 DTO는 Binance 전용으로 분리

## API 동작 계획

- `exchange=upbit`
  - 기존 Upbit provider 사용
  - 기존 화면과 응답 유지
- `exchange=binance`
  - 현재가/캔들 조회가 필요한 후보 탐색 API는 Binance provider 사용 가능하게 준비
  - portfolio/history는 Stage 5 전까지 데이터 분리 전이므로 제한적으로 처리
  - 지원하지 않는 흐름은 `501 Not Implemented` 유지
- 잘못된 exchange
  - `400 Bad Request`

## 테스트

- Binance 현재가 응답을 `MarketPrice`로 변환
- Binance 캔들 응답을 `Candle`로 변환
- Binance API 실패 시 명확한 exception으로 감싸기
- 빈 응답 처리
- symbol 변환
  - `KRW-BTC` -> `BTCUSDT`
  - `KRW-ETH` -> `ETHUSDT`
  - 지원 불가 quote 실패
- 기존 Upbit provider 테스트 유지
- `./gradlew test`

## 완료 기준

- Binance 공개 REST API provider가 추가된다.
- API Key/Secret 없이 동작한다.
- `exchange=binance`가 현재가/캔들 기반 조회 흐름에 연결될 준비가 된다.
- 기존 Upbit 기본 동작은 그대로 유지된다.
- 실제 주문 API와 `REAL_TRADING`은 추가되지 않는다.
- 모든 테스트가 통과한다.

## 리스크

- Binance는 `USDT` 기준이고 Upbit는 `KRW` 기준이라 수익률/자산 평가를 바로 섞으면 안 된다.
- Stage 5 전에는 portfolio/history가 거래소별로 분리되지 않았으므로 Binance 화면에 Upbit PAPER 데이터가 섞이지 않게 제한이 필요하다.
- Binance candle interval과 Upbit minute candle unit이 다르므로 mapper를 명확히 둬야 한다.
- Binance API 제한에 걸릴 수 있으므로 batch 조회와 fallback 정책은 Stage 8/WebSocket 전까지 보수적으로 둔다.
