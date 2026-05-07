# Stage 8: WebSocket Price Feed

## 목표

REST 반복 호출 중심 시세 조회를 거래소별 WebSocket 수신 구조로 개선한다.

현재 Upbit 전체 KRW ticker는 1초 REST polling으로 수집되고, `MarketPriceProvider`는 요청 시 REST를 직접 호출한다. Stage 8에서는 거래소별 WebSocket snapshot store를 만들고, provider가 최신 snapshot을 우선 사용한 뒤 REST fallback으로 내려가게 한다.

## 포함 기능

- Upbit WebSocket client
- Binance WebSocket client
- 거래소별 ticker snapshot store
- snapshot stale 판단
- REST fallback 또는 최근 snapshot 유지
- WebSocket 장애가 주문 실행을 위험하게 만들지 않도록 차단 정책
- WebSocket reconnect backoff
- snapshot 기반 `MarketPriceProvider` adapter
- WebSocket 상태 조회용 health/status 정보
- 기존 Upbit REST polling scheduler의 역할 축소 또는 fallback 전환 계획

## 제외 기능

- 실제 주문 API
- 선물/숏/레버리지
- WebSocket으로 받은 데이터를 DB에 전부 저장
- WebSocket 장애 시 무조건 주문 진행
- WebSocket tick 전체 history 저장
- 외부 message broker 도입
- 브라우저로 거래소 WebSocket을 직접 연결
- Stage 8에서 자동매매 전략 조건 변경

## 설계 원칙

- WebSocket client는 거래소별로 분리한다.
- snapshot store는 거래소, market, capturedAt, source를 함께 저장한다.
- provider는 WebSocket 구현 세부사항을 알지 못하고 snapshot/fallback provider만 사용한다.
- WebSocket 장애는 해당 거래소에만 격리한다.
- WebSocket과 REST가 모두 실패하면 주문 실행을 차단한다.
- 조회 화면은 stale snapshot을 표시할 수 있지만, 주문 실행은 stale 허용 기준을 더 보수적으로 둔다.

## 데이터 모델

공통 snapshot 모델 후보:

```java
public record TickerSnapshot(
        ExchangeMode exchange,
        String market,
        BigDecimal tradePrice,
        BigDecimal accTradePrice24h,
        Instant capturedAt,
        PriceSource source
) {
}
```

`PriceSource` 후보:

```java
public enum PriceSource {
    WEBSOCKET,
    REST_FALLBACK
}
```

stale 기준 후보:

- dashboard 표시 stale: 5초 초과
- 주문 실행 stale: 3초 초과
- REST fallback 실패 후 stale snapshot만 있으면 조회는 가능하지만 주문은 차단

## 시세 조회 우선순위

1. fresh WebSocket snapshot
2. REST fallback 호출
3. stale snapshot 반환과 stale 표시
4. 주문 실행에서는 실패 처리

조회 화면과 주문 실행의 처리 차이:

- Dashboard/Portfolio valuation: stale 여부를 표시하거나 평가 실패로 표시
- Candidate scan/trading flow: fresh snapshot 또는 REST fallback 성공만 사용
- REST fallback도 실패하면 주문 생성 금지

## Upbit WebSocket 설계

- 대상: KRW market 전체 또는 설정된 후보 market 목록
- message type: ticker
- market code 예: `KRW-BTC`
- 수신 시 `TickerSnapshot(exchange=UPBIT, market=KRW-BTC, source=WEBSOCKET)` 저장
- 연결 실패 시 backoff 후 reconnect
- 기존 `/v1/ticker/all?quote_currencies=KRW` polling은 Stage 8 구현 중 fallback 또는 bootstrap 용도로 재배치한다.

## Binance WebSocket 설계

- 대상: Stage 4에서 지원하는 `USDT` 현물 symbol
- stream 예: `btcusdt@ticker` 또는 combined stream
- 내부 market code는 `BTCUSDT`
- 수신 시 `TickerSnapshot(exchange=BINANCE, market=BTCUSDT, source=WEBSOCKET)` 저장
- 연결 실패는 Binance provider에만 영향
- API key/secret은 사용하지 않는다.

## REST fallback 설계

- Upbit fallback:
  - 기존 `UpbitMarketPriceProvider`
  - 기존 전체 KRW ticker REST polling 결과
- Binance fallback:
  - Stage 4의 `BinanceMarketPriceProvider`
- fallback 성공 시 snapshot store에 `REST_FALLBACK` source로 저장할 수 있다.
- fallback 실패는 exception을 삼키지 않고 provider 호출자에게 명확히 전달한다.

## 설정 계획

설정 후보:

```properties
market.websocket.enabled=true
market.websocket.upbit.enabled=true
market.websocket.binance.enabled=true
market.websocket.snapshot-stale-ms=5000
market.websocket.order-stale-ms=3000
market.websocket.reconnect-initial-delay-ms=1000
market.websocket.reconnect-max-delay-ms=30000
```

기본값은 안전하게 잡는다.

- WebSocket disabled면 기존 REST provider로 동작
- WebSocket enabled여도 snapshot이 없으면 REST fallback
- 테스트 환경은 WebSocket disabled 기본 유지

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/market/websocket/...` | 거래소별 WebSocket client |
| `src/main/java/com/giseop/comebot/market/service/...TickerSnapshotStore.java` | 최신 시세 snapshot 저장 |
| `src/main/java/com/giseop/comebot/market/provider/...` | snapshot 우선, REST fallback provider |
| `src/main/java/com/giseop/comebot/market/domain/TickerSnapshot.java` | 공통 snapshot 모델 |
| `src/main/java/com/giseop/comebot/market/domain/PriceSource.java` | WebSocket/REST source 구분 |
| `src/main/java/com/giseop/comebot/market/provider/SnapshotMarketPriceProvider.java` | snapshot 우선 provider |
| `src/main/java/com/giseop/comebot/market/provider/ExchangeMarketPriceProviderRouter.java` | 거래소별 provider 선택 |
| `src/main/java/com/giseop/comebot/market/service/UpbitKrwTickerStore.java` | 공통 snapshot store로 대체 또는 adapter |
| `src/main/java/com/giseop/comebot/market/scheduler/UpbitKrwTickerPollingScheduler.java` | fallback/bootstrap 역할로 조정 |
| `src/main/java/com/giseop/comebot/market/controller/MarketProviderStatusController.java` | WebSocket 상태 표시 후보 |
| `src/main/java/com/giseop/comebot/config/...WebSocketProperties.java` | WebSocket 설정 |
| `docs/operations/RELIABILITY.md` | WebSocket 장애와 fallback 정책 |
| `docs/harness/ARCHITECTURE.md` | WebSocket 시세 수신 구조 |

## 단계 내 구현 순서

1. 공통 `TickerSnapshot` 모델과 snapshot store 추가
2. snapshot freshness/stale 판단 로직 추가
3. snapshot 우선 provider 추가
4. REST fallback provider 연결
5. Upbit WebSocket client 추가
6. Binance WebSocket client 추가
7. status/health 표시 추가
8. 기존 REST polling scheduler 역할 정리

WebSocket client 구현은 마지막에 붙인다. provider/fallback 테스트가 먼저 안정화되어야 한다.

## 테스트 계획

- snapshot 최신이면 WebSocket 가격 사용
- snapshot stale이면 REST fallback 사용
- REST fallback 성공 시 fallback 가격 사용
- REST fallback 실패 시 주문 실행 차단
- 조회 API에서는 stale 상태를 추적 가능하게 표시
- WebSocket 예외가 애플리케이션을 죽이지 않음
- reconnect backoff가 무한 tight loop를 만들지 않음
- Upbit WebSocket 장애가 Binance provider를 중단시키지 않음
- Binance WebSocket 장애가 Upbit provider를 중단시키지 않음
- WebSocket disabled면 기존 REST provider 동작 유지
- snapshot store가 market별 최신 값만 유지
- 오래된 snapshot이 새 snapshot을 덮어쓰지 않음
- `./gradlew test`

## 완료 기준

- 시세 조회 우선순위가 WebSocket snapshot -> stale 처리 -> REST fallback -> 실패 차단 순서로 동작한다.
- Upbit/Binance WebSocket 흐름이 분리되어 있다.
- WebSocket 장애가 애플리케이션을 죽이지 않는다.
- WebSocket과 REST fallback이 모두 실패하면 주문 실행은 차단된다.
- 기존 Upbit REST 기반 동작은 WebSocket disabled 상태에서 유지된다.
- API key/secret, 실제 주문 API, `REAL_TRADING`은 추가되지 않는다.

## 리스크

- WebSocket reconnect loop가 너무 빠르면 CPU와 네트워크를 낭비한다.
- stale snapshot을 주문에 사용하면 의도보다 나쁜 가격으로 PAPER 거래가 기록될 수 있다.
- Upbit와 Binance symbol 형식이 달라 snapshot store key 설계가 흔들릴 수 있다.
- 현재 Upbit 전체 KRW REST polling과 WebSocket이 동시에 같은 store를 쓰면 source 우선순위 충돌이 생길 수 있다.
- WebSocket 테스트가 실제 외부 네트워크에 의존하면 불안정해진다.

## 사용자 확인 필요

- 없음. Stage 8은 공개 WebSocket 시세만 사용하고, 실제 주문 API나 인증 정보는 추가하지 않는다.
