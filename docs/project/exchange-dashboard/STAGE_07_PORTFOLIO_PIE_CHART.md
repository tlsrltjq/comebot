# Stage 7: Portfolio Pie Chart

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

포트폴리오/자산 화면에서 현금, market별 포지션, 거래소별 비중을 원형 그래프로 확인할 수 있게 한다.

기존 포트폴리오 화면의 metric card, 자산 배분 막대, market exposure 목록은 유지하고, 원형 그래프는 같은 정보를 더 빠르게 파악하는 보조 시각화로 추가한다.

## 포함 기능

- 현금/포지션 비중 원형 그래프
- market별 포지션 비중 원형 그래프
- 거래소별 비중 원형 그래프 준비
- 선택 거래소 기준 통화 표시
  - Upbit: `KRW`
  - Binance: `USDT`
- position이 없을 때 빈 상태 UI
- 비중이 작은 market을 `기타(Other)`로 묶는 기준
- 기존 포트폴리오 metric/card와 충돌하지 않는 배치
- 모바일에서도 텍스트와 차트가 겹치지 않는 반응형 UI
- 기존 Recharts 의존성 재사용

## 제외 기능

- KRW/USDT 통합 환산
- 외부 환율 API
- 새로운 거래 실행 기능
- 차트 기반 자동매매 판단
- 차트 클릭으로 주문 실행
- 실시간 tick 단위 animation
- Stage 5 전 Binance/Upbit portfolio를 합산하는 화면

## 데이터 설계

Stage 7은 새 backend aggregation API를 만들기보다 기존 portfolio valuation 응답을 우선 사용한다.

입력 데이터:

- `cash`
- `totalPositionValue`
- `totalEquity`
- `positions[].market`
- `positions[].positionValue`
- `positions[].unrealizedProfitRate`

프론트 계산 데이터:

```ts
type AllocationSlice = {
  id: string;
  label: string;
  value: number;
  rate: number;
  tone: 'cash' | 'position' | 'market' | 'other';
};
```

계산 기준:

- `cashRate = cash / totalEquity * 100`
- `positionRate = totalPositionValue / totalEquity * 100`
- market별 비중은 `positionValue / totalEquity * 100`
- `totalEquity <= 0`이면 차트를 그리지 않고 빈 상태를 표시한다.
- market slice가 너무 많으면 상위 5개만 표시하고 나머지는 `기타(Other)`로 합친다.

## UI 배치 계획

- metric grid 아래, 기존 `portfolio-overview` 영역 상단에 차트 panel을 배치한다.
- 첫 번째 chart panel:
  - 제목: `자산 비중(Asset Mix)`
  - 현금과 전체 포지션 비중 표시
- 두 번째 chart panel:
  - 제목: `마켓 비중(Market Allocation)`
  - position value 기준 상위 market 표시
- 세 번째 chart panel은 Stage 5 이후 활성화:
  - 제목: `거래소 비중(Exchange Allocation)`
  - 현재 선택 거래소만 있으면 단일 slice 또는 준비 상태 표시
- chart 아래에는 legend를 둔다.
- legend에는 한글(영어), 금액, 비중을 함께 표시한다.
- 기존 막대형 allocation/exposure는 당장 제거하지 않는다. 원형 그래프 도입 후 중복도가 높으면 후속 UX 정리 stage에서 줄인다.

## 시각 디자인 기준

- `recharts`의 `PieChart`, `Pie`, `Cell`, `ResponsiveContainer`, `Tooltip`을 사용한다.
- 새 차트 라이브러리는 추가하지 않는다.
- 카드 안에 카드가 들어가는 구조를 만들지 않는다.
- chart panel은 고정 최소 높이를 둬 loading/empty/data 상태 전환 시 레이아웃이 튀지 않게 한다.
- 색상은 현금, 포지션, market별 slice가 구분되도록 4~6개 색상 팔레트를 사용한다.
- dominant purple/blue 단색 테마가 되지 않게 기존 UI 색상과 균형을 맞춘다.
- 모바일에서는 chart와 legend를 세로 배치한다.
- 긴 market명과 큰 금액은 줄바꿈 또는 ellipsis로 처리한다.

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `frontend/src/features/portfolio/PortfolioPage.tsx` | 원형 그래프 UI |
| `frontend/src/features/portfolio/PortfolioAllocationChart.tsx` | 차트 컴포넌트 분리 후보 |
| `frontend/src/features/portfolio/portfolioAllocation.ts` | allocation 계산 helper 분리 후보 |
| `frontend/src/shared/api/types.ts` | allocation 표시 타입 필요 여부 |
| `frontend/src/styles.css` | 차트 배치와 responsive 스타일 |
| `docs/harness/ARCHITECTURE.md` | 자산 비중 표시 위치 |

## 컴포넌트 분리 기준

- 단순 UI만 있으면 `PortfolioPage.tsx` 내부 helper로 시작한다.
- 계산 로직이 테스트 대상이 되면 `portfolioAllocation.ts`로 분리한다.
- chart JSX가 길어지면 `PortfolioAllocationChart.tsx`로 분리한다.
- Stage 6 선택 매도 UI와 섞이지 않도록 차트 상태와 선택 상태를 분리한다.

## 테스트 계획

- 현금/포지션 비중이 올바르게 계산되는지 확인
- market별 비중이 `positionValue / totalEquity` 기준으로 계산되는지 확인
- 상위 5개 초과 market은 `기타(Other)`로 합쳐지는지 확인
- `totalEquity=0`일 때 빈 상태 표시
- position이 없을 때 빈 상태 표시
- Recharts가 test 환경에서 깨지지 않도록 차트 label/legend 중심으로 검증
- 선택 거래소가 바뀌면 해당 거래소 valuation 기준으로 차트가 바뀌는지 확인
- 모바일에서 차트와 텍스트가 겹치지 않음
- `npm run lint`
- `npm run build`
- `npm test`

## 완료 기준

- 포트폴리오 화면에서 자산 비중을 한눈에 볼 수 있다.
- `UPBIT`과 `BINANCE` 모드에서 각각 해당 거래소 기준 비중만 표시한다.
- 기준 통화가 명확히 표시된다.
- 빈 포트폴리오에서도 화면이 깨지지 않는다.
- 원형 그래프는 조회 전용이며 주문 실행과 연결되지 않는다.

## 리스크

- Stage 5 전에는 거래소별 portfolio 분리가 완료되지 않았으므로 Binance 화면에서 차트를 활성화하면 데이터가 섞일 수 있다.
- Recharts responsive chart는 container 높이가 없으면 빈 화면이 될 수 있다.
- market slice가 너무 많으면 legend가 길어져 모바일에서 화면을 밀어낼 수 있다.
- KRW와 USDT를 합산하면 잘못된 총자산 비중이 된다.
- 기존 막대형 allocation과 새 원형 그래프가 중복되어 화면이 복잡해질 수 있다.

## 사용자 확인 필요

- 없음. Stage 7은 거래소별 portfolio가 분리된 뒤 선택 거래소 기준으로만 원형 그래프를 표시한다.

## 구현 결과

- 포트폴리오 화면에 원형 그래프 panel 3개를 추가했다.
  - `자산 비중(Asset Mix)`: 현금과 전체 포지션 비중
  - `마켓 비중(Market Allocation)`: market별 포지션 평가액 비중
  - `거래소 비중(Exchange Allocation)`: 현재 선택 거래소 기준 단일 비중
- 새 backend API 없이 기존 portfolio valuation 응답에서 차트 데이터를 계산한다.
- 기준 통화는 선택 거래소 portfolio 응답의 `currency`를 사용한다.
- market slice는 상위 5개를 표시하고 나머지는 `기타(Other)`로 합친다.
- `totalEquity <= 0`이거나 보유 포지션이 없으면 빈 상태를 표시한다.
- 기존 metric card, 자산 배분 막대, market exposure 목록은 유지했다.
- Recharts 기존 의존성을 재사용했다.
- 체크박스가 전역 input width 영향을 받지 않도록 checkbox 스타일을 고정했다.

## 검증 결과

- `npm run lint`
- `npm test`
- `npm run build`
- `./gradlew test`
