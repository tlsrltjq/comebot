# Stage 10: BTC Change Chart

## 목표

Market Overview 성격의 페이지에서 비트코인 등락률 그래프를 제공한다.

이 페이지는 시장 상태를 보기 위한 조회 전용 화면이다. 그래프는 자동매매 조건을 직접 바꾸거나 주문 실행으로 이어지지 않는다.

## 포함 기능

- 신규 Market Overview 페이지
- 사이드바 nav 추가
- Upbit 모드: `KRW-BTC`
- Binance 모드: `BTCUSDT`
- 시간 범위 선택: `1h`, `24h`, `3d`, `7d`
- 선택 거래소에 맞는 candle source 사용
- 등락률 기준 그래프
- 현재가, 시작가, 기간 등락률, 고가/저가 요약
- candle API 실패/빈 응답 상태 표시
- 기존 Recharts 의존성 재사용

## 제외 기능

- 실제 주문 API
- 그래프 기반 자동매매 실행
- 선물/숏/레버리지
- 거래소 간 통합 BTC 가격 환산
- 외부 환율 API
- 차트 클릭 주문
- 실시간 tick chart
- 기술적 지표 대량 추가

## API 설계

신규 API 후보:

```http
GET /api/market/btc-change?exchange=upbit&range=24h
```

응답 후보:

```json
{
  "exchange": "UPBIT",
  "market": "KRW-BTC",
  "range": "24h",
  "basePrice": "100000000",
  "latestPrice": "103000000",
  "changeRate": "3.00000000",
  "highPrice": "104000000",
  "lowPrice": "99000000",
  "points": [
    {
      "time": "2026-05-08T00:00:00Z",
      "price": "100000000",
      "changeRate": "0.00000000"
    },
    {
      "time": "2026-05-08T00:01:00Z",
      "price": "100100000",
      "changeRate": "0.10000000"
    }
  ]
}
```

## Range/Candle 기준

| Range | Candle unit | Count 후보 | 목적 |
| --- | --- | --- | --- |
| `1h` | 1분 | 60 | 단기 변동 확인 |
| `24h` | 15분 | 96 | 하루 흐름 확인 |
| `3d` | 60분 | 72 | 3일 추세 확인 |
| `7d` | 240분 | 42 | 1주 흐름 확인 |

Upbit는 기존 `UpbitCandleProvider`의 minute candle unit을 사용한다. Binance는 Stage 4의 `BinanceCandleProvider` interval mapper를 사용한다.

## 계산 기준

- candle은 시간 오름차순으로 정렬한다.
- `basePrice`는 범위 내 가장 오래된 candle의 trade price로 둔다.
- 각 point의 `changeRate`는 `(price - basePrice) / basePrice * 100`으로 계산한다.
- `latestPrice`는 가장 최신 candle의 trade price다.
- `highPrice`, `lowPrice`는 범위 내 candle high/low 기준으로 계산한다.
- candle이 2개 미만이면 그래프 대신 빈 상태 또는 계산 불가 상태를 반환한다.

## UI 설계

- nav label: `시장(Market)`
- page title: `시장 개요(Market Overview)`
- 상단 segmented range control: `1h`, `24h`, `3d`, `7d`
- ExchangeMode는 Stage 3의 사이드바 모드를 그대로 사용한다.
- 그래프 제목:
  - Upbit: `BTC 등락률(KRW-BTC Change)`
  - Binance: `BTC 등락률(BTCUSDT Change)`
- metric cards:
  - 현재가(Current)
  - 기간 등락률(Change)
  - 고가(High)
  - 저가(Low)
- chart:
  - `LineChart`, `ResponsiveContainer`, `Tooltip`, `XAxis`, `YAxis`
  - y축은 등락률 %
  - tooltip은 시간, 가격, 등락률 표시
- 빈 상태:
  - candle 없음
  - API 미지원
  - provider 실패
- chart panel에는 고정 높이를 둬 빈 canvas 문제가 없게 한다.

## Backend 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/market/controller/MarketOverviewController.java` | BTC chart data API |
| `src/main/java/com/giseop/comebot/market/service/BtcChangeChartService.java` | candle -> 등락률 point 변환 |
| `src/main/java/com/giseop/comebot/market/dto/BtcChangeChartResponse.java` | chart 응답 DTO |
| `src/main/java/com/giseop/comebot/market/dto/BtcChangePointResponse.java` | chart point DTO |
| `src/main/java/com/giseop/comebot/market/candle/provider/...` | 거래소별 candle source 사용 |
| `src/main/java/com/giseop/comebot/exchange/ExchangeSymbolMapper.java` | `UPBIT -> KRW-BTC`, `BINANCE -> BTCUSDT` |
| `src/main/java/com/giseop/comebot/exchange/ExchangeModeResolver.java` | query parsing 재사용 |

## Frontend 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `frontend/src/app/App.tsx` | Market Overview nav 추가 |
| `frontend/src/main.tsx` | `/market` route 추가 |
| `frontend/src/features/market/MarketOverviewPage.tsx` | BTC 등락률 그래프 페이지 |
| `frontend/src/features/market/MarketOverviewPage.test.tsx` | chart page 테스트 |
| `frontend/src/shared/api/client.ts` | chart API 함수 |
| `frontend/src/shared/api/types.ts` | chart response 타입 |
| `frontend/src/styles.css` | chart/range control responsive 스타일 |

## Backend 테스트

- `1h`, `24h`, `3d`, `7d` 범위 요청
- Upbit 모드에서 `KRW-BTC` 사용
- Binance 모드에서 `BTCUSDT` 사용
- 잘못된 range는 `400 Bad Request`
- candle이 시간 오름차순으로 정렬되는지 확인
- base price 기준 등락률 계산 확인
- candle 2개 미만이면 계산 불가 처리
- provider 예외는 애플리케이션을 죽이지 않고 명확한 오류 응답
- 실제 주문 API 호출 없음
- `./gradlew test`

## Frontend 테스트

- Market nav 표시
- range button 표시와 선택 상태
- `exchange`와 `range`가 API query에 포함됨
- chart metric 표시
- 빈 candle 응답 처리
- 프론트 차트 빈 상태 표시
- 그래프가 조회 전용이며 실행/매수/매도 버튼 없음
- `npm run lint`
- `npm run build`
- `npm test`

## 완료 기준

- 거래소 모드에 맞는 BTC 등락률 그래프를 볼 수 있다.
- 시간 범위를 바꿔도 UI가 깨지지 않는다.
- 그래프는 조회 전용이며 주문 실행과 연결되지 않는다.
- Upbit/Binance의 BTC market symbol이 명확히 분리된다.
- API 실패나 빈 candle 상태가 화면에서 명확히 표시된다.
- 실제 주문 API, 수동 BUY, `REAL_TRADING`은 추가되지 않는다.

## 리스크

- Upbit와 Binance candle interval이 정확히 일치하지 않아 range별 point 수가 달라질 수 있다.
- candle 정렬을 잘못하면 등락률이 반대로 계산될 수 있다.
- 너무 많은 point를 그리면 모바일 성능과 가독성이 떨어질 수 있다.
- Binance provider가 Stage 4 이후에만 동작하므로, Stage 4 전 구현 시 `BINANCE`는 `501` 또는 미지원 상태를 보여야 한다.
- 그래프가 trading signal처럼 오해되지 않도록 조회 전용 UI로 유지해야 한다.

## 사용자 확인 필요

- 없음. Stage 10은 조회 전용 Market Overview 페이지로 진행한다.

## 구현 결과

- `GET /api/market/btc-change?exchange=upbit&range=24h` API를 추가했다.
- Upbit 모드는 `KRW-BTC`, Binance 모드는 `BTCUSDT` candle source를 사용한다.
- `1h`, `24h`, `3d`, `7d` 범위를 지원하며 잘못된 range는 `400 Bad Request`로 응답한다.
- candle을 시간 오름차순으로 정렬하고 시작가 기준 등락률, 최신가, 고가, 저가, point별 등락률을 계산한다.
- 프론트에 `시장(Market)` nav와 Market Overview 페이지를 추가했다.
- 페이지는 조회 전용이며 매수/매도/실행 버튼을 제공하지 않는다.

검증:

```text
./gradlew test --tests BtcChangeChartServiceTest --tests MarketOverviewControllerTest
npm run lint
npm test
npm run build
```
