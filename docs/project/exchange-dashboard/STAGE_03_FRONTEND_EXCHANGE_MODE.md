# Stage 3: Frontend Exchange Mode

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

웹 화면 구조는 그대로 유지하고, 사이드바 상단에서 `UPBIT / BINANCE` 모드를 선택할 수 있게 한다. 선택한 거래소 모드는 프론트 상태, React Query cache key, API 요청 파라미터에 일관되게 반영한다.

## 포함 기능

- 사이드바 상단 `UPBIT / BINANCE` segmented button
- 기본값 `UPBIT`
- `exchange=upbit`, `exchange=binance` query parameter 사용
- 새로고침 후 선택 모드 유지를 위한 URL query 우선 사용
- Dashboard, Candidates, Auto Run, Portfolio, History 요청에 `exchange` 전달
- React Query `queryKey`에 `exchange` 포함
- `BINANCE` 선택 시 아직 미지원인 API의 `501 Not Implemented` 응답을 화면에서 명확히 표시

## 제외 기능

- Binance REST API 연동
- Binance WebSocket 연동
- Binance 포트폴리오/history 분리
- 선택 포지션 PAPER 매도
- 자산 원형 그래프
- BTC 등락률 그래프
- 자동매매 주기 변경
- 실제 주문 API, 수동 BUY, `REAL_TRADING`

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `frontend/src/app/App.tsx` | 사이드바 거래소 모드 버튼과 하위 페이지 전달 구조 |
| `frontend/src/shared/api/types.ts` | `ExchangeMode = 'UPBIT' | 'BINANCE'` 타입 |
| `frontend/src/shared/api/client.ts` | 주요 API 함수와 query key에 `exchange` 추가 |
| `frontend/src/features/system/DashboardPage.tsx` | system/analytics 조회에 exchange 반영 또는 제한 표시 |
| `frontend/src/features/candidates/CandidatesPage.tsx` | 후보 조회에 exchange 전달 |
| `frontend/src/features/trading/TradePage.tsx` | 자동 실행 상태와 history 조회에 exchange 전달 |
| `frontend/src/features/portfolio/PortfolioPage.tsx` | portfolio 조회에 exchange 전달 |
| `frontend/src/features/history/HistoryPage.tsx` | history 조회에 exchange 전달 |
| `frontend/src/styles.css` | 사이드바 모드 버튼 스타일 |

## 백엔드 전제

- Stage 2가 먼저 구현되어 있어야 한다.
- `exchange` 누락 시 `UPBIT`
- `exchange=upbit` 허용
- `exchange=binance`는 Stage 4 전까지 `501 Not Implemented`
- 잘못된 exchange 값은 `400 Bad Request`

## 테스트

- 기본 모드가 `UPBIT`인지 확인
- `UPBIT` 선택 시 API URL에 exchange parameter가 포함되는지 확인
- `BINANCE` 선택 시 query key가 분리되는지 확인
- `BINANCE`의 `501` 응답이 화면 에러로 표시되는지 확인
- 기존 수동 실행 차단 테스트 유지
- `npm run lint`
- `npm test` 또는 현재 프론트 테스트 명령

## 완료 기준

- 사이드바에서 `UPBIT / BINANCE`를 바꿀 수 있다.
- 선택한 거래소가 모든 주요 조회 API에 전달된다.
- `UPBIT` 화면은 기존과 동일하게 동작한다.
- `BINANCE` 화면은 아직 미구현 상태를 명확히 보여준다.
- 거래소 변경 시 이전 거래소 cache가 섞이지 않는다.
- 실제 주문 API, 수동 BUY, `REAL_TRADING`은 추가되지 않는다.

## 구현 결과

- 사이드바 상단에 `UPBIT / BINANCE` mode switch를 추가했다.
- 기본값은 `UPBIT`이다.
- URL query의 `exchange=binance`를 읽어 새로고침 후에도 선택 모드를 유지한다.
- 메뉴 이동 시 현재 `exchange` query를 유지한다.
- 주요 조회 API에 `exchange=upbit` 또는 `exchange=binance` 파라미터를 전달한다.
- React Query key에 `exchange`를 포함해 거래소별 cache가 섞이지 않게 했다.
- `BINANCE` 선택 시 Stage 2 backend의 `501 Not Implemented` 응답을 화면 에러로 표시한다.
- 실제 주문 API, 수동 BUY, `REAL_TRADING`은 추가하지 않았다.

## 리스크

- Stage 2 없이 Stage 3을 먼저 구현하면 backend 응답 기준과 맞지 않는다.
- query key에 exchange를 넣지 않으면 Upbit 데이터가 Binance 화면에 남을 수 있다.
- analytics API가 아직 거래소 분리되지 않으면 일부 지표가 Upbit 기준으로 보일 수 있다.
- URL query와 내부 state가 따로 움직이면 새로고침 시 모드가 바뀔 수 있다.
