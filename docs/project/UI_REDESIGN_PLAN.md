# UI Redesign Plan

이 문서는 comebot 웹을 전면 개편하기 위한 기준 문서다.
기존 `WEB_UX_UPGRADE_PLAN.md`은 누적 개선 이력에 가깝고, 이 문서는 앞으로 바꿀 화면 구조와 구현 순서를 정의한다.

## 목표

현재 웹은 기능은 많지만 정보 우선순위가 약하고, 카드가 비슷한 무게로 반복되어 운영 판단이 늦어진다.
새 디자인은 자동 `PAPER_TRADING` 운영자가 첫 화면에서 다음 질문에 바로 답할 수 있게 만든다.

- 지금 자동 PAPER 운영이 가능한가?
- 데이터가 충분히 쌓였는가, 아직 수집 중인가?
- 현재 위험한 포지션이나 market 쏠림이 있는가?
- 후보 BUY와 보유 PAPER SELL 흐름이 정상적으로 움직이는가?
- 사용자가 직접 할 수 있는 행동과 금지된 행동이 명확히 분리되어 있는가?

## 유지할 안전 제약

- 기본 거래 모드는 항상 `PAPER_TRADING`이다.
- `REAL_TRADING` UI, 설정, 상태 토글을 만들지 않는다.
- 실제 거래소 주문 API 입력 폼을 만들지 않는다.
- 수동 BUY 버튼을 만들지 않는다.
- 후보 실행, trading-flow 실행 같은 수동 BUY 성격의 버튼을 만들지 않는다.
- 사용자가 명시 선택한 보유 PAPER 포지션 SELL만 예외적으로 허용한다.
- 선택 PAPER SELL은 Portfolio 화면의 보유 포지션 행 또는 상세 패널에서만 제공한다.
- 위험 액션은 확인 모달과 선택 포지션 요약을 거친다.
- 서버의 전략, 리스크, 포트폴리오 판단을 React에서 재구현하지 않는다.
- 민감 정보 입력, 표시, 저장 UI를 만들지 않는다.

## 디자인 방향

새 웹은 마케팅 사이트나 일반 관리자 페이지가 아니라 트레이딩 운영 콘솔이어야 한다.
장식용 hero, 큰 소개 문구, 중첩 카드, 과한 그라데이션은 사용하지 않는다.

### 핵심 톤

- 정보 밀도가 높은 운영 도구
- 차분한 배경과 명확한 상태 색상
- 정상, 주의, 차단, 손익 상태가 즉시 구분되는 색 체계
- 반복 확인에 피로하지 않은 낮은 채도의 UI
- 표와 패널 중심의 구조

### 색상 원칙

- 배경: 아주 옅은 회색 또는 청흑 계열의 낮은 채도
- 표면: 흰색 또는 미세한 회색 패널
- 정상: green
- 정보: blue
- 주의: amber
- 차단/손실: red
- 중립/HOLD: gray

한 화면이 특정 색 하나로만 보이지 않게 한다.
특히 보라색, 베이지색, 진한 남색, 갈색 계열이 전체 화면을 지배하지 않게 한다.

### 타이포그래피

- 운영 숫자와 market 이름은 읽기 쉬운 고정 폭 느낌의 숫자 정렬을 우선한다.
- hero급 큰 제목은 사용하지 않는다.
- Dashboard 상단 제목도 기능 설명보다 상태 판단을 우선한다.
- 버튼과 배지는 짧게 유지한다.
- 긴 market 목록, 사유 문구, 경로는 줄바꿈 또는 축약 규칙을 둔다.

## 새 정보 구조

### App Shell

전체 화면은 좌측 navigation, 상단 상태바, 본문 영역으로 구성한다.

- 좌측 Sidebar
  - Dashboard
  - Markets
  - Candidates
  - Portfolio
  - History
  - Risk
  - System

- Top Status Bar
  - 현재 exchange
  - `PAPER_TRADING`
  - DB 연결 상태
  - price provider 상태
  - candidate scheduler 상태
  - exit scheduler 상태
  - kill switch 상태
  - 마지막 갱신 시각

- Main Content
  - 페이지별 핵심 상태
  - 테이블 또는 분석 패널
  - 페이지 고유 액션

### Dashboard: Control Room

Dashboard는 첫 화면이며, “현재 켜둬도 되는가”를 판단하는 화면이다.

상단 영역:

- Operational Readiness
  - DB
  - price provider
  - candidate scheduler
  - exit scheduler
  - kill switch

- Data Readiness
  - 데이터 부족
  - 수집 중
  - 진입 검증 중
  - 검증 가능

- Risk Summary
  - market concentration warning/block 기준
  - 반복 손절 cooldown 상태
  - 가장 큰 exposure market
  - 최근 손절 여부

중앙 영역:

- Total equity
- Total PnL
- Realized / Unrealized PnL
- Position value
- 24h BUY / SELL / HOLD
- Filled / Rejected / Failed

하단 영역:

- 최근 후보 신호
- 최근 PAPER 주문 상태 전이
- 최근 손실 매도
- 운영 환경 OS guide

Dashboard에는 거래 실행 버튼을 두지 않는다.

### Markets

Markets는 시세와 market 상태를 보는 화면이다.

- exchange별 market coverage
- ticker snapshot count
- BTC 기준 변화율 차트
- 거래대금 상위 market
- REST fallback 또는 WebSocket 상태
- stale price 여부

시장 선택은 조회 필터일 뿐, 수동 BUY 실행으로 이어지지 않는다.

### Candidates

Candidates는 자동 후보 판단 결과를 읽는 화면이다.

- SELECTED / SKIPPED / HOLD 요약
- 주요 제외 사유 TOP 5
- market별 후보 상태
- 추세, 변동성, 거래대금, 리스크 조건 분리 표시
- 보유 포지션 여부
- concentration warning 대상 표시
- repeated stop loss cooldown 대상 표시

Candidates 화면에는 후보 실행 버튼을 두지 않는다.
필터, 정렬, 상세 확인만 허용한다.

### Portfolio

Portfolio는 보유 PAPER 포지션과 선택 PAPER SELL의 유일한 진입점이다.

상단:

- cash
- position value
- total equity
- unrealized PnL
- realized PnL
- position count

본문:

- 포지션 테이블
  - market
  - exchange
  - quantity
  - average price
  - current price
  - valuation
  - unrealized PnL
  - exposure rate
  - take profit / stop loss distance
  - risk flags

액션:

- 선택 PAPER SELL만 허용
- SELL 버튼은 포지션 행 또는 상세 패널에만 둔다.
- 클릭 시 확인 모달에서 market, quantity, estimated value, current PnL을 보여준다.
- 확인 모달 문구는 실제 주문이 아닌 PAPER SELL임을 명확히 표시한다.
- 수동 BUY는 없다.

모바일:

- 테이블 대신 포지션 카드 리스트를 사용한다.
- SELL 액션은 카드 하단의 명확한 위험 액션으로 둔다.

### History

History는 PAPER 판단과 주문 상태 흐름을 감사하는 화면이다.

- time range tabs
- exchange filter
- market filter
- signal filter: BUY / SELL / HOLD
- order status filter: FILLED / REJECTED / FAILED
- reason filter
- stop loss / take profit filter

요약:

- range별 총 실행 수
- BUY / SELL / HOLD count
- Filled / Rejected / Failed count
- average take profit
- average stop loss
- worst trades
- repeated stop loss markets

History는 원인을 추적하는 화면이며, 거래 실행 액션을 두지 않는다.

### Risk

Risk는 현재 리스크 정책과 실제 적용 상태를 한 화면에 둔다.

- max order amount
- allowed markets
- take profit rate
- stop loss rate
- daily order limit
- daily loss limit
- market concentration warning/block threshold
- repeated stop loss cooldown
- kill switch 상태

정책값을 바꾸는 UI는 아직 만들지 않는다.
먼저 읽기 전용으로 문서와 코드 상태를 일치시킨다.

### System

System은 운영 환경, 스크립트, scheduler, notification을 다룬다.

- OS 감지 결과: macOS / Windows / Linux / Unknown
- OS별 run script
- OS별 schema script
- workspace path
- shell
- backend/frontend 실행 주소
- scheduler interval
- notification status
- Telegram status

Mac과 Windows는 같은 기능을 유지하되, 경로와 실행 스크립트 표기를 OS별로 바꾼다.

## 공통 컴포넌트 계획

새 UI는 화면별 임시 CSS를 줄이고 공통 컴포넌트를 정리한다.

- `AppShell`
- `Sidebar`
- `TopStatusBar`
- `PageHeader`
- `StatusBadge`
- `RiskBadge`
- `ProfitValue`
- `MetricTile`
- `InfoPanel`
- `DataTable`
- `EmptyState`
- `ConfirmDialog`
- `SegmentedControl`
- `TimeRangeTabs`
- `ExchangeSelector`
- `LiveRefreshStatus`

아이콘은 기존처럼 `lucide-react`를 우선 사용한다.
텍스트 버튼보다 아이콘과 짧은 라벨을 조합한다.
낯선 아이콘에는 tooltip을 붙인다.

## 반응형 계획

### Desktop

- Sidebar 고정
- Top status bar 고정 또는 상단 유지
- Dashboard는 12-column grid
- Portfolio, History, Candidates는 테이블 중심
- 상세 정보는 우측 drawer 또는 하단 detail panel

### Tablet

- Sidebar는 접을 수 있게 한다.
- 핵심 상태는 2-column grid
- 테이블은 주요 컬럼을 우선 표시하고 보조 컬럼은 detail row로 이동한다.

### Mobile

- Sidebar는 bottom navigation 또는 drawer로 전환한다.
- 테이블은 카드 리스트로 전환한다.
- metric은 2-column 또는 1-column으로 정리한다.
- 긴 숫자와 market 이름은 줄바꿈 없이 깨지지 않게 한다.

## OS별 화면 계획

사용자 OS를 감지해서 실행 가이드를 다르게 보여준다.

- macOS
  - shell: `zsh`
  - run script: `scripts/run-upbit-paper.sh`
  - workspace 예시: `/Users/<user>/workspace/comebot`

- Windows
  - shell: `PowerShell` 또는 `cmd`
  - run script: `scripts\run-upbit-paper.bat`
  - workspace 예시: `%USERPROFILE%\workspace\comebot`

- Linux
  - shell: `bash`
  - run script: `scripts/run-upbit-paper.sh`
  - workspace 예시: `$HOME/workspace/comebot`

OS별 차이는 실행 안내, 경로, 단축키, shell 표기만 바꾼다.
거래 기능과 안전 제약은 OS에 따라 달라지지 않는다.

## 구현 단계

### Stage 1: Design Foundation

목표:

- 새 App Shell과 디자인 토큰을 만든다.
- 기존 페이지 기능은 유지한다.

작업:

- 전역 CSS 색상, 간격, 표면, typography 규칙 정리
- `AppShell`, `Sidebar`, `TopStatusBar` 추가
- 기존 route를 새 shell 안으로 이동
- active navigation, exchange selector, live status 위치 정리

완료 기준:

- 기존 페이지 접근 경로가 깨지지 않는다.
- manual BUY, real trading UI가 추가되지 않는다.
- `npm run lint`, `npm test`, `npm run build` 통과

### Stage 2: Dashboard Control Room

목표:

- Dashboard를 운영 판단 중심 화면으로 전면 재배치한다.

작업:

- Operational Readiness, Data Readiness, Risk Summary를 상단에 배치
- PnL, signal, order status metric을 중앙에 재배치
- 최근 손실, 최근 신호, OS guide를 하단으로 이동
- 빈 데이터 상태를 공통 `EmptyState`로 통일

완료 기준:

- 데이터 부족 상태가 명확히 표시된다.
- 운영 가능 상태와 데이터 충분 상태가 섞이지 않는다.
- Dashboard에는 거래 실행 버튼이 없다.

### Stage 3: Portfolio Action Boundary

목표:

- 선택 PAPER SELL UX를 안전하게 고립한다.

작업:

- 포지션 테이블 재설계
- 선택 PAPER SELL 확인 모달 추가 또는 개선
- 모바일 카드형 포지션 리스트 추가
- exposure, TP/SL distance, risk flags를 한 행에서 확인 가능하게 한다.

완료 기준:

- 선택한 보유 PAPER 포지션만 SELL 가능하다.
- 수동 BUY가 없다.
- PAPER 현금/포지션/history 상태 전이가 테스트된다.

### Stage 4: Candidates and History Tables

목표:

- 후보 판단과 실행 이력을 분석 가능한 테이블로 정리한다.

작업:

- Candidates 필터/정렬 정리
- 제외 사유 TOP 요약
- History range/filter UI 정리
- order lifecycle 상태 표시 통일

완료 기준:

- strategy 판단 사유가 UI에서 분리되어 보인다.
- failed/rejected order를 성공처럼 보이지 않는다.
- 후보 화면에 실행 버튼이 없다.

### Stage 5: Risk and System Pages

목표:

- 리스크 정책과 운영 환경을 독립 화면으로 분리한다.

작업:

- Risk page 읽기 전용 정책 표 추가
- concentration/cooldown 상태 요약
- System page OS guide와 scheduler/notification 상태 정리
- Mac/Windows/Linux 실행 가이드 자동 분기 표시

완료 기준:

- `docs/trading/RISK_POLICY.md`와 Risk page 표현이 일치한다.
- `docs/operations/RELIABILITY.md`와 System page 표현이 일치한다.
- OS별 표시가 테스트된다.

### Stage 6: Visual Regression and Cleanup

목표:

- 새 디자인이 화면 크기별로 깨지지 않게 고정한다.

작업:

- Testing Library 기반 주요 상태 테스트 확대
- 필요하면 Playwright 스크린샷 테스트 추가
- 사용하지 않는 CSS와 컴포넌트 제거
- 문서와 실제 UI 명칭 정합성 점검

완료 기준:

- desktop/tablet/mobile에서 텍스트 겹침이 없다.
- 버튼/배지/테이블 셀이 부모 영역을 넘지 않는다.
- 사용하지 않는 UI 코드가 남지 않는다.

## 화면별 변경 목록

| 화면 | 현재 문제 | 변경 방향 |
| --- | --- | --- |
| Dashboard | 카드가 많고 중요도가 비슷함 | Control Room으로 재구성 |
| Markets | 시세 상태와 provider 상태가 분산됨 | market coverage와 provider 상태 중심 |
| Candidates | 후보 판단 사유가 표 안에서 읽기 어려움 | decision/reason/risk flag 중심 테이블 |
| Portfolio | 액션 경계와 분석 정보가 약함 | 선택 PAPER SELL만 고립하고 포지션 리스크 강조 |
| History | 필터와 원인 분석 밀도가 낮음 | 감사 로그와 손실 원인 분석 중심 |
| Risk | 정책과 실제 상태가 여러 화면에 흩어짐 | 읽기 전용 리스크 정책 화면 |
| System | 실행 안내와 운영 상태가 섞임 | OS guide, scheduler, notification 분리 |

## 금지 목록

- hero landing page
- marketing copy 중심 화면
- 중첩 카드 레이아웃
- 수동 BUY 버튼
- 실제 주문 버튼
- `REAL_TRADING` 선택 UI
- 민감 정보 입력 폼
- localStorage에 거래 상태 저장
- React에서 서버 리스크 정책 재계산
- 실패 주문을 성공처럼 보이게 하는 표현

## 검증 기준

각 stage는 다음을 통과해야 한다.

- `npm run lint`
- `npm test`
- `npm run build`
- 화면 구조, 반응형 레이아웃, 위험 액션 UX 변경 시 `npm run test:e2e`
- Java 코드 변경이 있으면 `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew clean build`
- 보안/민감 정보 린트 기준 위반 없음
- 수동 BUY와 실제 주문 API 추가 없음

## 다음 실행 후보

1. PAPER 포지션 청산 흐름 스모크 테스트
2. 데이터 누적 후 전략 조건 재검토
3. 필요 시 추가 chunk/manualChunks 세부 조정

현재 우선순위는 PAPER 포지션 청산 흐름 스모크 테스트다.

## 진행 상황

- 완료: Stage 1 `AppShell`, `Sidebar`, `TopStatusBar` 구현
- 완료: Risk 읽기 전용 route 추가
- 완료: System 읽기 전용 route 추가
- 완료: 기존 `/trade` route 유지
- 완료: Stage 2 Dashboard Control Room 상단 3분할 재배치
- 완료: Dashboard 리스크 요약 상단 배치
- 완료: Dashboard 중복 운영 status strip 제거
- 완료: Stage 3 Portfolio 선택 PAPER SELL 확인 모달 개선
- 완료: Portfolio 선택 평가액/현재 손익/처리 범위 표시
- 완료: Portfolio 테이블과 모바일 카드에 exposure와 TP/SL distance 표시
- 완료: Stage 4 Candidates reason type/risk flag 분리
- 완료: Candidates 조회 전용 audit strip 추가
- 완료: History order lifecycle 요약과 명확한 order status badge 추가
- 완료: Stage 5 Risk/System 읽기 전용 제약 표시
- 완료: Risk page 정책/금지 UI 회귀 테스트 추가
- 완료: System page OS별 실행 안내 회귀 테스트 추가
- 완료: Stage 6 미사용 status strip CSS 제거
- 완료: Stage 6 mobile 520px 이하 보강
- 완료: Stage 6 전체 lint/test/build와 Java build 검증
- 완료: Frontend route lazy loading 적용
- 완료: 초기 `index` JS chunk 약 795kB에서 약 332kB로 축소
- 완료: Recharts chart chunk 분리로 build chunk 경고 제거
- 완료: Playwright 실제 브라우저 화면 회귀 테스트 추가
- 완료: desktop/mobile Chromium route 렌더링, 루트 overflow, 선택 PAPER SELL 경계 검증
- 완료: 모바일 sidebar/nav min-width 보강으로 가로 overflow 제거
