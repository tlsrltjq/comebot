# Stage 6: Selected PAPER Position Sell

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

포트폴리오 화면에서 사용자가 체크박스로 선택한 보유 PAPER 포지션만 수동 매도할 수 있게 한다.

이 stage는 웹에서 허용되는 유일한 거래 실행 UX 예외다. 수동 BUY, 실제 거래소 주문, `REAL_TRADING`은 계속 금지한다.

## 포함 기능

- 포트폴리오 보유 종목 체크박스
- 선택된 종목 전량 PAPER SELL
- 여러 market 선택 후 한 번에 SELL 요청
- 거래소 모드별 portfolio 기준 적용
- 서버 측 보유 수량, 가격, kill switch 검증
- 중복 market 요청은 서버에서 중복 제거 후 처리
- 서버 측 현재가 캡처
- 서버 측 SELL `OrderRequest` 생성
- PAPER execution gateway와 portfolio 반영 흐름 재사용
- 매도 결과 history 저장
- 선택 market별 성공/실패 결과 반환
- 실패한 매도를 성공으로 처리하지 않음
- 실제 주문 API와 명확히 분리된 PAPER 전용 endpoint

## 제외 기능

- 수동 BUY
- 실제 주문 API
- `REAL_TRADING`
- 부분 수량 매도
- reduce-only kill switch 예외
- 선물/숏/레버리지
- 프론트가 가격, 수량, 체결 상태를 결정하는 구조
- 텔레그램 수동 SELL 버튼

## API 설계

endpoint는 portfolio 영역 아래에 둔다. trading-flow 수동 실행 API처럼 보이지 않게 이름을 명확히 한다.

```http
POST /api/portfolio/positions/sell-selected?exchange=upbit
Content-Type: application/json

{
  "markets": ["KRW-BTC", "KRW-ETH"]
}
```

응답은 market별 결과를 반환한다.

```json
{
  "exchange": "UPBIT",
  "requestedCount": 2,
  "succeededCount": 1,
  "failedCount": 1,
  "results": [
    {
      "market": "KRW-BTC",
      "signalType": "SELL",
      "orderCreated": true,
      "orderStatus": "FILLED",
      "message": "Selected PAPER position sold",
      "executedAt": "2026-05-07T00:00:00Z"
    },
    {
      "market": "KRW-ETH",
      "signalType": "SELL",
      "orderCreated": false,
      "orderStatus": "REJECTED",
      "message": "Position not found",
      "executedAt": "2026-05-07T00:00:00Z"
    }
  ]
}
```

## 서버 검증 순서

1. `exchange` 파라미터 파싱
2. request body의 `markets` null/empty/blank 검증
3. 중복 market 제거
4. kill switch 확인
5. exchange별 보유 포지션 조회
6. 선택 market이 실제 보유 포지션인지 확인
7. 현재가 provider에서 서버가 가격 캡처
8. 보유 수량 전량으로 SELL `OrderRequest` 생성
9. 기존 risk validation 실행
10. PAPER execution gateway 실행
11. `FILLED`일 때만 portfolio 반영
12. market별 history 저장
13. market별 결과 반환

kill switch가 켜져 있으면 시세 조회 전에 차단한다.

## 실패 처리

- 요청 market 목록이 비어 있으면 전체 요청을 `400 Bad Request`로 거절한다.
- 알 수 없는 exchange는 `400 Bad Request`다.
- 아직 지원되지 않는 exchange 흐름은 `501 Not Implemented`다.
- 특정 market이 미보유 상태면 해당 market만 `REJECTED` 결과로 남긴다.
- 현재가 조회 실패는 해당 market을 `FAILED` 또는 명확한 실패 결과로 남긴다.
- 하나의 market 실패가 다른 market SELL 결과를 성공으로 바꾸면 안 된다.
- 여러 market 중 일부 실패는 허용하고, 나머지 market 처리는 계속 진행한다.
- 중복 market 요청은 전체 요청 실패로 보지 않고 한 번만 처리한다.
- `REJECTED`, `FAILED`는 portfolio를 변경하지 않는다.
- 모든 결과는 history에 저장한다.

## 프론트 UX 설계

- Portfolio position table/card에 체크박스를 추가한다.
- 선택된 항목이 있을 때만 선택 매도 toolbar를 표시한다.
- toolbar에는 선택 개수, 예상 기준 통화, `선택 매도(Sell selected)` 버튼을 둔다.
- 버튼 클릭 시 확인 dialog를 띄운다.
- 확인 dialog에는 “PAPER 포지션 전량 매도”임을 표시한다.
- 가격과 수량은 프론트에서 입력받지 않는다.
- 요청 중에는 버튼을 disable 처리한다.
- 완료 후 portfolio valuation, positions, history query를 invalidate한다.
- 실패 market은 결과 summary로 표시한다.
- 수동 BUY, 후보 실행, trading-flow run 버튼은 추가하지 않는다.

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/portfolio/controller/PortfolioStatusController.java` | 선택 PAPER 매도 endpoint |
| `src/main/java/com/giseop/comebot/portfolio/service/PaperPortfolioService.java` | 선택 market 전량 매도 처리 |
| `src/main/java/com/giseop/comebot/portfolio/service/SelectedPaperSellService.java` | controller가 execution/risk/history를 직접 조합하지 않도록 전용 use case |
| `src/main/java/com/giseop/comebot/portfolio/dto/SelectedPaperSellRequest.java` | 선택 market 요청 DTO |
| `src/main/java/com/giseop/comebot/portfolio/dto/SelectedPaperSellResponse.java` | market별 매도 결과 응답 DTO |
| `src/main/java/com/giseop/comebot/execution/gateway/PaperTradingExecutionGateway.java` | PAPER SELL 처리 재사용 |
| `src/main/java/com/giseop/comebot/history/service/TradingFlowHistoryService.java` | market별 결과 history 저장 |
| `src/main/java/com/giseop/comebot/safety/KillSwitchService.java` | 선택 SELL 차단 |
| `src/main/java/com/giseop/comebot/risk/service/RiskValidationService.java` | SELL risk 검증 재사용 |
| `frontend/src/features/portfolio/PortfolioPage.tsx` | 체크박스와 선택 매도 toolbar |
| `frontend/src/shared/api/client.ts` | PAPER SELL API 함수 |
| `frontend/src/shared/api/types.ts` | 선택 매도 request/response 타입 |
| `frontend/eslint.config.js` | 실제 주문/수동 BUY endpoint 금지 유지 |
| `docs/trading/ORDER_LIFECYCLE.md` | PAPER 수동 SELL 흐름 |
| `docs/trading/RISK_POLICY.md` | kill switch와 매도 제한 |
| `docs/harness/DEVELOPMENT_RULES.md` | 허용되는 유일한 웹 거래 실행 endpoint 명시 |

## 백엔드 테스트

- 선택한 보유 포지션 전량 SELL 성공
- 여러 market 선택 시 market별 결과 반환
- 미보유 market은 `REJECTED`
- blank market은 `400 Bad Request`
- 중복 market은 서버에서 중복 제거 후 한 번만 처리
- kill switch enabled일 때 시세 조회 전 차단
- 현재가 조회 실패 시 portfolio 변경 없음
- risk validation 실패 시 execution gateway 호출 없음
- `FILLED`일 때만 cash, position, realized profit 변경
- `REJECTED`, `FAILED`도 history에 저장
- `exchange=binance`는 Stage 5 이후 Binance portfolio만 대상으로 처리
- 실제 주문 API class/client가 추가되지 않았는지 보안 린트 유지

## 프론트 테스트

- 보유 포지션 행에 체크박스 표시
- 선택 항목이 없으면 매도 버튼 disabled 또는 toolbar hidden
- 선택 후 확인 dialog 표시
- 확인 후 `sell-selected` API 호출
- 요청 payload에는 market 목록과 exchange만 포함
- 가격/수량 입력 UI 없음
- 성공 후 portfolio/history query invalidate
- 실패 결과 summary 표시
- 일부 실패가 있어도 성공/실패 개수를 함께 표시
- 수동 BUY, 후보 실행, trading-flow run 호출 없음

## 검증 명령

- `./gradlew test`
- `npm run lint`
- `npm run build`
- `npm test`

## 완료 기준

- 사용자가 명시 선택한 PAPER 포지션만 매도 가능하다.
- 매도 결과가 포트폴리오와 history에 반영된다.
- 선택 market별 성공/실패가 화면과 history에 남는다.
- 일부 market 실패가 있어도 나머지 market SELL은 진행된다.
- kill switch가 켜져 있으면 선택 PAPER SELL도 차단된다.
- 프론트는 가격, 수량, 체결 상태를 결정하지 않는다.
- 실제 주문 API, 수동 BUY, `REAL_TRADING`은 추가되지 않는다.

## 리스크

- 수동 SELL이 trading-flow 수동 실행 API처럼 확장되면 수동 BUY 금지 원칙이 약해진다.
- 프론트에서 가격이나 수량을 보내면 조작 가능한 주문이 된다.
- 여러 market 중 일부 실패 시 전체 성공처럼 보여주면 portfolio와 history 해석이 어려워진다.
- Stage 5의 exchange별 portfolio 분리 없이 Binance 선택 SELL을 열면 Upbit 포지션을 잘못 매도할 수 있다.

## 사용자 확인 필요

- 없음. 중복 market 요청은 서버에서 중복 제거하고, 여러 market 중 일부 실패는 허용한다.

## 구현 결과

- `POST /api/portfolio/positions/sell-selected?exchange=...` endpoint를 추가했다.
- request body는 `markets` 목록만 받는다. 가격, 수량, 체결 상태는 프론트에서 받지 않는다.
- `SelectedPaperSellService`가 market별로 보유 포지션 확인, kill switch 확인, 서버 현재가 캡처, SELL 주문 요청 생성, PAPER execution, portfolio 반영, history 저장을 수행한다.
- 중복 market 요청은 서버에서 중복 제거한다.
- 미보유 market, kill switch, 현재가 조회 실패는 market별 `REJECTED` 또는 `FAILED` 결과로 반환하고 history에 저장한다.
- `OrderExecutionService`, `RiskValidationService`, `DailyRiskValidationService`에 exchange-aware overload를 추가해 Binance PAPER SELL이 Binance 포트폴리오와 history에만 반영되게 했다.
- 포트폴리오 화면에 보유 포지션 체크박스, 선택 매도 toolbar, 확인 dialog, 결과 요약을 추가했다.
- 프론트 API client에는 선택 PAPER SELL 함수만 추가했고 수동 BUY endpoint는 추가하지 않았다.
- ESLint 금지 규칙에 수동 BUY 성격의 portfolio endpoint 금지를 추가했다.

## 검증 결과

- `SelectedPaperSellServiceTest`
- `PortfolioStatusControllerTest`
- `OrderExecutionServiceTest`
- `DailyRiskValidationServiceTest`
- `npm run lint`
- `npm test`
- `npm run build`
- `./gradlew test`
