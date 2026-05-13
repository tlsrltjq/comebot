# Web UX Upgrade Plan

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

현재 웹은 자동 PAPER_TRADING 상태를 확인하는 기본 모니터링 화면이다. 다음 단계에서는 수동 매매 기능을 추가하지 않고, 자동 실행을 오래 켜두면서 판단하기 쉬운 운영 대시보드로 발전시킨다.

## 유지할 원칙

- 웹은 조회 중심 운영 화면이다.
- 자동매매 켜기/끄기와 candidate scheduler 주기 변경은 허용한다.
- 사용자가 명시 선택한 보유 PAPER 포지션 SELL만 거래 실행 UX 예외로 허용한다.
- 수동 BUY, 후보 실행, trading-flow 실행 버튼을 만들지 않는다.
- 선택 보유 PAPER 포지션 SELL 외의 임의 SELL 버튼을 만들지 않는다.
- 실제 주문 API, `REAL_TRADING`, Upbit 인증 설정을 웹에 추가하지 않는다.
- 서버의 전략, 리스크, 포트폴리오 판단을 React에서 재구현하지 않는다.
- 모든 신규 화면은 한글(English) 라벨을 유지한다.

## 추천 기술 방향

- Vite React 기반 유지
  - 현재 구조와 맞고, 빠른 dev server와 production build 흐름을 그대로 쓸 수 있다.
- TanStack Query 고도화
  - polling query를 화면 목적별로 조정하고, 실패/재시도/백그라운드 상태를 더 명확히 표시한다.
- React Router data route 정리
  - 현재 `createBrowserRouter`를 유지하되, route 단위 lazy loading과 error boundary를 추가한다.
- 접근성 UI primitive 도입
  - Radix Primitives 또는 Base UI 계열을 검토한다.
  - Dialog, Tooltip, Tabs, Select, Switch, Popover 같은 상호작용 컴포넌트를 직접 만들지 않는다.
- 차트 라이브러리 도입
  - 손익, BUY/SELL/HOLD 비율, 포지션 비중, 시간대별 손익을 시각화한다.
  - 후보: Recharts, Tremor, Visx 중 하나를 비교 후 선택한다.
- 서버 기반 집계 API 추가
  - React에서 history 전체를 계산하지 않고, Spring API가 손익/체결/손절/익절 집계를 제공한다.

## 1단계: 정보 구조 개선

목표는 첫 화면에서 현재 상태를 바로 판단하게 하는 것이다.

- 상단 운영 바
  - 백엔드 연결 상태
  - Upbit ticker 수집 상태
  - 자동 실행 ON/OFF
  - 후보 스케줄러 ON/OFF
  - Telegram 수동 PAPER 실행 차단 여부
  - 마지막 갱신 시각

- 대시보드 재구성
  - 총 평가 자산
  - 총 손익
  - 실현 손익
  - 미실현 손익
  - 현재 보유 포지션 수
  - 최근 24시간 BUY/SELL/HOLD 개수
  - 손절/익절 비율

- 매매 조건 패널
  - 1회 BUY 금액: 10,000 KRW
  - 대상: `ALL_KRW`
  - 후보 범위: 거래대금 상위 50개
  - 실행 주기: 30초
  - 익절/손절: 1.5 / -0.7

## 2단계: 포트폴리오 UX 개선

- 포지션 테이블 개선
  - 손익률 색상
  - 평가액 기준 정렬
  - 손실률 기준 정렬
  - 익절/손절 근접 상태 표시

- 포트폴리오 요약
  - 현금 비중
  - 포지션 비중
  - 가장 큰 손실 포지션
  - 가장 큰 수익 포지션
  - exchange별 market 쏠림 경고 기준 표시

- 모바일 대응
  - 표를 카드형 리스트로 전환
  - 긴 market 목록과 숫자가 줄바꿈으로 깨지지 않게 고정 폭/축약 표기 적용

## 3단계: 실행 이력 분석 화면

- History 필터
  - Market
  - BUY/SELL/HOLD
  - FILLED/REJECTED/FAILED
  - 손절/익절 사유
  - 시간 범위

- 집계 카드
  - 최근 1시간, 24시간, 3일, 7일 선택
  - BUY 수
  - SELL 수
  - HOLD 수
  - 손절 수
  - 익절 수
  - 평균 손절률
  - 평균 익절률

- 손실 원인 보기
  - 손실 큰 SELL 순위
  - 반복 손절 market
  - 손절 직후 재진입 후보

## 4단계: 후보 화면 개선

- 후보 요약
  - SELECTED 개수
  - SKIPPED 개수
  - 주요 제외 사유 TOP 5

- 후보 테이블
  - 결정 상태
  - 추세
  - 가격 변화율
  - 거래대금 변화율
  - 고가/저가 범위
  - 제외 사유
  - 보유 포지션 여부

- 후보 UX
  - 선택 후보만 보기
  - market별 노출 비중 보기
  - 거래대금 상위 순위 보기
  - 쏠림 경고와 반복 손절 cooldown 대상 표시

## 5단계: 실시간성 개선

현재는 polling 중심이다. 먼저 안정성을 위해 polling을 유지하고, 병목이 보이면 서버에서 SSE를 추가한다.

- 1차: TanStack Query polling 간격 정리
  - system: 5초
  - portfolio: 3~5초
  - history: 3초
  - candidates: 10~30초

- 2차: Server-Sent Events 검토
  - ticker snapshot
  - scheduler summary
  - filled order event
  - portfolio valuation event

## 6단계: 디자인 시스템 정리

- 컬러 토큰
  - 수익: green
  - 손실: red
  - HOLD/neutral: gray
  - warning: amber
  - system/upbit live: blue

- 컴포넌트
  - `AppShell`
  - `StatusBar`
  - `MetricCard`
  - `DataTable`
  - `SegmentedControl`
  - `TimeRangeTabs`
  - `ProfitBadge`
  - `ConnectionBadge`
  - `MarketFilter`

- 금지
  - 장식용 hero
  - 수동 매매 버튼
  - 실제 주문 관련 폼
  - API client 외부 직접 fetch
  - localStorage/sessionStorage 거래 상태 저장

## 필요한 백엔드 API

웹을 제대로 발전시키려면 React에서 직접 계산하지 않고 API를 추가하는 편이 좋다.

- `GET /api/analytics/summary?range=24h`
  - BUY/SELL/HOLD 개수
  - FILLED/REJECTED/FAILED 개수
  - 손절/익절 개수
  - 평균 손절률/익절률

- `GET /api/analytics/pnl?range=24h`
  - 총 손익
  - 실현 손익
  - 미실현 손익
  - 시간대별 손익

- `GET /api/analytics/losses?range=24h`
  - 손실 큰 SELL 목록
  - 반복 손절 market

- `GET /api/market/selection/status`
  - 전체 KRW 수
  - 후보 대상 상위 50개
  - 최신 ticker 수집 시각

## 구현 순서

1. API client에서 analytics endpoint 타입 추가
2. 백엔드 analytics summary API 추가
3. 대시보드 상단 운영 바 추가
4. 포트폴리오 화면 테이블 정리
5. History 분석 화면 추가
6. 후보 화면 필터와 제외 사유 집계 추가
7. 차트 라이브러리 도입
8. 접근성 UI primitive 도입
9. 모바일 레이아웃 검증
10. Playwright 또는 Testing Library 기반 화면 회귀 테스트 확대

## 진행 상황

- 완료: Recharts 도입
- 완료: `GET /api/analytics/summary?range=24h`
- 완료: `GET /api/analytics/pnl?range=24h`
- 완료: `GET /api/analytics/losses?range=24h`
- 완료: 대시보드 상단 운영 바
- 완료: 총 평가금, 총 손익, 보유 평가, 24시간 BUY/SELL/HOLD, 익절/손절 요약 카드
- 완료: 24시간 신호 분포 차트
- 완료: 최근 손절 기록 점검 패널
- 완료: 포트폴리오 자산 배분, 손익 리더, 정렬, 손익률 색상, 익절/손절 근접 상태 표시
- 완료: History range 선택, BUY/SELL/HOLD 필터, 주문 상태 필터, 사유 필터
- 완료: History 손실 원인, 반복 손절 market, 반복 HOLD 사유 요약
- 다음: 포트폴리오 모바일 카드형 리스트
- 다음: 후보 화면 필터와 제외 사유 집계

## 완료 기준

- `npm run lint`, `npm run build`, `npm test` 성공
- `./gradlew test` 성공
- 웹에서 수동 실행 endpoint 호출 없음
- 총 손익과 포지션 상태가 한 화면에서 판단 가능
- 손실 원인과 다음 조정 후보가 history 기반으로 보임
