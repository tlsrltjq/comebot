# Stage 5: Exchange Portfolio and History Separation

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

`UPBIT`과 `BINANCE`의 PAPER 포트폴리오, 포지션, 실현손익, 거래 이력을 분리한다. 같은 화면 구조를 유지하되 선택한 거래소의 PAPER 데이터만 조회되도록 한다.

## 배경

현재 portfolio는 전역 cash, realized profit, positions를 하나만 가진다. history도 market 기준 조회만 있고 거래소 구분 값이 없다. Binance 데이터를 붙인 뒤 이 분리를 하지 않으면 `BINANCE` 화면에 Upbit PAPER 포지션과 이력이 섞일 수 있다.

## 포함 기능

- `PaperPortfolio` 조회와 저장을 `ExchangeMode` 기준으로 분리
- `PaperPosition` 조회와 저장을 `ExchangeMode + market` 기준으로 분리
- realized profit 조회와 이벤트 저장을 `ExchangeMode` 기준으로 분리
- `TradingFlowHistory`에 `exchange` 필드 추가
- history 조회를 `exchange` 기준으로 필터링
- 기존 데이터는 `UPBIT`으로 간주하는 하위 호환 정책
- `exchange=upbit` 화면은 기존 포트폴리오와 history를 그대로 보여준다.
- `exchange=binance` 화면은 Binance PAPER 데이터만 보여준다.
- Stage 5에서는 Binance PAPER 자동매매 전체 연결보다 데이터 분리와 조회 정확성을 우선한다.

## 제외 기능

- 실제 주문 API
- `REAL_TRADING`
- 수동 BUY
- 선택 포지션 PAPER 매도
- Binance WebSocket
- 선물, 숏, 레버리지
- 거래소 간 통합 손익 계산
- KRW/USDT 환산 손익 통합

## 도메인 설계

- `ExchangeMode`는 portfolio/history domain에 명시적으로 포함한다.
- Upbit portfolio 기준 통화는 `KRW`로 둔다.
- Binance portfolio 기준 통화는 `USDT`로 둔다.
- total equity와 profit은 거래소별 기준 통화 안에서만 계산한다.
- 통합 자산 화면에서 KRW와 USDT를 합산하는 기능은 이후 별도 stage로 둔다.

## Repository/API 설계

Portfolio repository는 기존 메서드를 exchange-aware 형태로 확장한다.

```java
BigDecimal getCash(ExchangeMode exchange);
void saveCash(ExchangeMode exchange, BigDecimal cash);
Optional<PaperPosition> findPosition(ExchangeMode exchange, String market);
List<PaperPosition> findPositions(ExchangeMode exchange);
PaperPortfolio getPortfolio(ExchangeMode exchange);
```

History repository는 exchange filter를 지원한다.

```java
TradingFlowHistory save(TradingFlowHistory history);
List<TradingFlowHistory> findRecent(ExchangeMode exchange, int limit);
List<TradingFlowHistory> findRecentByMarket(ExchangeMode exchange, String market, int limit);
```

기존 exchange 없는 호출은 Stage 5 구현 중 제거하지 않고 `UPBIT` default wrapper로 유지해 하위 호환성을 확보한다.

## DB 마이그레이션 계획

- `trading_flow_history.exchange` 컬럼 추가
- 기존 row의 `exchange`는 `UPBIT`으로 채운다.
- 컬럼은 `nullable=false`로 최종 고정한다.
- 조회 성능을 위해 `(exchange, created_at)` 또는 `(exchange, market, created_at)` index를 검토한다.
- portfolio가 현재 in-memory 중심이면 Stage 5에서는 in-memory exchange 분리를 먼저 구현한다.
- portfolio 영속화가 추가되어 있다면 별도 migration 문서를 작성한다.

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/portfolio/domain/PaperPortfolio.java` | 필요 시 exchange/currency 메타 포함 |
| `src/main/java/com/giseop/comebot/portfolio/repository/PaperPortfolioRepository.java` | exchange-aware portfolio API |
| `src/main/java/com/giseop/comebot/portfolio/repository/InMemoryPaperPortfolioRepository.java` | 거래소별 cash/position/profit 저장소 분리 |
| `src/main/java/com/giseop/comebot/portfolio/service/PaperPortfolioService.java` | exchange-aware service API |
| `src/main/java/com/giseop/comebot/portfolio/service/PaperPortfolioValuationService.java` | 거래소별 valuation |
| `src/main/java/com/giseop/comebot/portfolio/controller/PortfolioStatusController.java` | `exchange` 기준 portfolio 조회 |
| `src/main/java/com/giseop/comebot/history/domain/TradingFlowHistory.java` | exchange 필드 추가 |
| `src/main/java/com/giseop/comebot/history/repository/TradingFlowHistoryRepository.java` | exchange filter API |
| `src/main/java/com/giseop/comebot/history/repository/InMemoryTradingFlowHistoryRepository.java` | 거래소별 in-memory filtering |
| `src/main/java/com/giseop/comebot/history/persistence/TradingFlowHistoryEntity.java` | JPA entity exchange 컬럼 |
| `src/main/java/com/giseop/comebot/history/persistence/SpringDataTradingFlowHistoryJpaRepository.java` | exchange 조회 method |
| `src/main/java/com/giseop/comebot/history/persistence/JpaTradingFlowHistoryRepository.java` | JPA exchange filter 구현 |
| `src/main/java/com/giseop/comebot/history/controller/TradingFlowHistoryController.java` | `exchange` 기준 history 조회 |
| `src/main/resources/db/migration/...` | history exchange 컬럼 추가가 필요한 경우 |
| `frontend/src/shared/api/types.ts` | portfolio currency 또는 exchange 표시 타입 |
| `frontend/src/features/portfolio/PortfolioPage.tsx` | 선택 거래소 portfolio만 표시 |
| `frontend/src/features/history/HistoryPage.tsx` | 선택 거래소 history만 표시 |

## API 동작 계획

- `GET /api/portfolio/status?exchange=upbit`
  - Upbit PAPER cash/profit 반환
- `GET /api/portfolio/status?exchange=binance`
  - Binance PAPER cash/profit 반환
- `GET /api/portfolio/positions?exchange=binance`
  - Binance PAPER positions만 반환
- `GET /api/portfolio/valuation?exchange=binance`
  - Binance provider로 현재가 평가
- `GET /api/trading-flow/history?exchange=binance`
  - Binance history만 반환

## 기존 데이터 호환성

- exchange 필드가 없는 기존 history는 `UPBIT`으로 취급한다.
- 기존 in-memory portfolio 초기값은 `UPBIT` portfolio로 유지한다.
- Binance portfolio 초기 현금은 별도 설정값으로 둔다.
- 기존 API에서 `exchange`를 생략하면 `UPBIT`으로 동작한다.

## 설정 계획

- Upbit PAPER 초기 현금은 기존 설정 유지
- Binance PAPER 초기 현금은 별도 설정 추가
- 설정 예시:

```properties
paper.portfolio.upbit.initial-cash=1000000
paper.portfolio.binance.initial-cash=1000
```

금액 단위가 다르므로 UI에는 기준 통화를 표시한다.

## 테스트

- `UPBIT` portfolio와 `BINANCE` portfolio cash가 분리되는 테스트
- 같은 market 문자열이어도 exchange가 다르면 position이 섞이지 않는 테스트
- `exchange` 생략 시 기존 `UPBIT` 동작 유지 테스트
- realized profit event가 exchange별로 집계되는 테스트
- history 저장 시 exchange가 보존되는 테스트
- history 조회 시 exchange별 필터링 테스트
- 기존 JPA history row를 `UPBIT`으로 읽는 migration/호환 테스트
- portfolio valuation이 exchange별 provider를 사용하는 테스트
- `./gradlew test`
- frontend 영향이 있으면 `npm run lint`, `npm test`

## 완료 기준

- `UPBIT`과 `BINANCE` portfolio가 서로 섞이지 않는다.
- `UPBIT`과 `BINANCE` history가 서로 섞이지 않는다.
- 기존 Upbit 데이터와 API 호출은 깨지지 않는다.
- Binance portfolio는 `USDT` 기준으로 표시된다.
- 실제 주문 API, 수동 BUY, `REAL_TRADING`은 추가되지 않는다.
- 모든 테스트가 통과한다.

## 리스크

- 기존 history schema 변경은 migration 누락 시 운영 시작에 실패할 수 있다.
- KRW와 USDT를 같은 total profit으로 합산하면 잘못된 숫자가 된다.
- service와 repository 일부만 exchange-aware로 바꾸면 화면에 데이터가 섞일 수 있다.
- 기존 테스트 fixture에 exchange 필드가 추가되면 많은 테스트 수정이 필요할 수 있다.
- portfolio 초기 현금을 거래소별로 나누지 않으면 Binance 화면이 빈 화면처럼 보이거나 Upbit 현금을 재사용할 수 있다.

## 사용자 확인 필요

- Binance PAPER 초기 현금은 기본 `1000 USDT`로 시작한다.
- 통합 손익은 Stage 5에서 만들지 않고 거래소별 손익만 표시한다.

## 구현 결과

- `PaperPortfolio`, repository, service API를 `ExchangeMode` 기준으로 확장했다.
- 기존 exchange 없는 포트폴리오 호출은 `UPBIT` wrapper로 유지했다.
- `InMemoryPaperPortfolioRepository`는 exchange별 cash, position, realized profit, realized profit event를 분리한다.
- `TradingFlowHistory`에 `exchange` 필드를 추가했다.
- in-memory/JPA history repository는 exchange별 recent, market, since 조회를 지원한다.
- `schema.sql`에 `trading_flow_history.exchange` 컬럼과 exchange 조회 index를 추가했다.
- `GET /api/portfolio/status`, `/positions`, `/valuation`은 `exchange=binance` 요청을 허용하고 Binance PAPER 데이터만 반환한다.
- `GET /api/trading-flow/history`는 `exchange` 기준으로 history를 필터링한다.
- `GET /api/system/status?exchange=binance`는 Binance 모드 화면의 공통 상태 조회를 위해 허용한다.
- 포트폴리오 API 응답과 프론트 포트폴리오 화면은 `currency`를 표시하고, Binance는 `USDT` 기준으로 보여준다.
- Candidate 조회와 자동매매 전체 Binance 연결은 Stage 5 범위 밖이므로 기존처럼 제한한다.

## 검증 결과

- `PaperPortfolioServiceTest`
- `PaperPortfolioValuationServiceTest`
- `PortfolioStatusControllerTest`
- `TradingFlowHistoryServiceTest`
- `TradingFlowHistoryControllerTest`
- `JpaTradingFlowHistoryRepositoryTest`
- `SystemStatusControllerTest`
- `npm run lint`
- `npm test`
- `npm run build`
- `./gradlew test`
