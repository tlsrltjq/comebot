# Project Next Steps

이 문서는 다음 작업만 관리한다.
완료 이력은 Git history와 `docs/project/PROJECT_HISTORY.md`의 최근 항목을 기준으로 본다.

## 완료: Dashboard 데이터 준비 상태 표시

목표:

- 운영 정상 여부와 데이터 충분 여부를 대시보드에서 분리해서 보여준다.
- 데이터가 쌓이지 않은 상태에서는 손익 판단을 보류해야 함을 명확히 표시한다.

작업:

- 24시간 실행, 체결, BUY/SELL, 보유 포지션, 청산 대상 수를 한 화면에 표시한다.
- `데이터 부족`, `수집 중`, `진입 검증 중`, `검증 가능` 상태를 구분한다.
- 운영 준비 상태와 데이터 준비 상태를 각각 테스트한다.

완료 기준:

- PAPER 주문 실행 로직은 변경하지 않는다.
- 데이터가 없어도 화면이 정상 렌더링된다.
- 데이터 부족 상태가 매수/매도 실행 버튼을 만들지 않는다.

## 완료: UI 전면 개편 Stage 1

목표:

- `docs/project/UI_REDESIGN_PLAN.md` 기준으로 App Shell, Sidebar, Top Status Bar를 만든다.
- 기존 PAPER 안전 제약을 유지하면서 화면 구조부터 운영 콘솔 형태로 바꾼다.

작업:

- 전역 레이아웃을 좌측 navigation과 상단 status bar 구조로 교체한다.
- Dashboard, Markets, Candidates, Portfolio, History, Risk, System route를 새 shell에 연결한다.
- exchange, `PAPER_TRADING`, DB, 시세, scheduler, kill switch, 마지막 갱신 상태를 상단에서 표시한다.

완료 기준:

- 기존 페이지 접근 경로가 깨지지 않는다.
- 실제 주문 API, `REAL_TRADING`, 수동 BUY UI가 추가되지 않는다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.

## 완료: UI 전면 개편 Stage 2

목표:

- Dashboard를 Control Room 구조로 재배치한다.
- 운영 준비 상태, 데이터 준비 상태, 리스크 요약을 첫 화면 상단에서 바로 판단하게 한다.

작업:

- Operational Readiness, Data Readiness, Risk Summary를 상단 3분할 영역으로 정리한다.
- PnL, signal, order status metric을 중앙 영역으로 재배치한다.
- 최근 손실, 최근 신호, OS guide를 하단 운영 보조 영역으로 이동한다.
- 빈 데이터 상태를 읽기 쉬운 공통 패턴으로 맞춘다.

완료 기준:

- Dashboard에는 거래 실행 버튼이 없다.
- 운영 가능 상태와 데이터 충분 상태가 섞이지 않는다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.

## 완료: UI 전면 개편 Stage 3

목표:

- Portfolio에서 선택 PAPER SELL UX를 안전하게 고립한다.
- 보유 포지션 리스크와 선택 액션 경계를 한 화면에서 명확히 보여준다.

작업:

- 포지션 테이블을 exposure, TP/SL distance, risk flags 중심으로 정리한다.
- 선택 PAPER SELL 확인 모달에서 market, quantity, estimated value, current PnL을 다시 보여준다.
- 모바일 카드형 포지션 리스트의 정보 우선순위를 개선한다.
- 수동 BUY와 실제 주문 UI가 없음을 테스트로 유지한다.

완료 기준:

- 선택한 보유 PAPER 포지션만 SELL 가능하다.
- PAPER SELL 확인 문구가 실제 주문이 아님을 명확히 표시한다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.

## 완료: UI 전면 개편 Stage 4

목표:

- Candidates와 History를 분석 가능한 테이블 중심 화면으로 정리한다.
- 후보 판단 사유와 주문 상태 흐름을 더 빠르게 감사할 수 있게 한다.

작업:

- Candidates 필터와 정렬 구조를 decision, reason, risk flag 중심으로 정리한다.
- 제외 사유 TOP 요약과 cooldown/concentration 표시를 더 눈에 띄게 배치한다.
- History range/filter UI와 order lifecycle 상태 표시를 통일한다.
- failed/rejected 주문이 성공처럼 보이지 않도록 badge와 문구를 재점검한다.

완료 기준:

- Candidates 화면에 실행 버튼이 없다.
- History에서 FILLED/REJECTED/FAILED 구분이 명확하다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.

## 완료: UI 전면 개편 Stage 5

목표:

- Risk와 System 페이지를 읽기 전용 운영 기준 화면으로 더 다듬는다.
- 문서의 리스크 정책과 운영 환경 안내가 화면 표현과 어긋나지 않게 한다.

작업:

- Risk page에서 리스크 정책, concentration, cooldown, 금지된 실거래 범위를 더 명확히 묶는다.
- System page에서 OS guide, scheduler, provider, notification/Telegram 상태를 더 스캔하기 쉽게 정리한다.
- Mac/Windows/Linux 실행 안내가 깨지지 않는지 테스트한다.
- 실제 주문 API, `REAL_TRADING`, 수동 BUY 설정 UI가 없음을 유지한다.

완료 기준:

- `docs/trading/RISK_POLICY.md`와 Risk page 표현이 충돌하지 않는다.
- `docs/operations/RELIABILITY.md`와 System page 표현이 충돌하지 않는다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.

## 완료: UI 전면 개편 Stage 6

목표:

- 새 디자인의 화면 깨짐, 불필요한 CSS, 테스트 공백을 점검한다.
- desktop/mobile에서 텍스트 겹침과 위험 액션 노출 문제를 확인한다.

작업:

- 주요 화면의 반응형 grid와 긴 텍스트 줄바꿈 규칙을 점검한다.
- 사용하지 않는 CSS나 중복된 UI 패턴을 정리한다.
- 필요하면 Testing Library 회귀 테스트를 보강한다.
- 최종적으로 수동 BUY, 실제 주문, `REAL_TRADING` UI가 없음을 재확인한다.

완료 기준:

- desktop/mobile 레이아웃에서 명백한 겹침이 없다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.
- 최종 UI 개편 진행 상황이 문서에 반영된다.

## 완료: Frontend route lazy loading

목표:

- Vite production build의 초기 JavaScript chunk 크기를 줄인다.
- Recharts 기반 화면을 첫 로딩 번들에서 분리한다.

작업:

- React Router route `lazy`를 사용해 Dashboard, Markets, Candidates, Trade, Portfolio, History, Risk, System 페이지를 동적 import로 전환한다.
- 페이지 정적 import를 `main.tsx`에서 제거한다.
- build output에서 초기 `index` chunk와 page/chart chunk가 분리되는지 확인한다.

완료 기준:

- `npm run build`에서 500kB 초과 chunk 경고가 사라진다.
- 초기 `index` JS chunk가 약 795kB에서 약 332kB로 줄어든다.
- `npm run lint`, `npm test`, `npm run build`가 통과한다.

## 다음 우선순위: PAPER 포지션 청산 흐름 스모크 테스트

목표:

- 현재 쌓인 PAPER 포지션으로 익절/손절/선택 PAPER SELL 흐름을 검증한다.
- 실제 주문 API와 `REAL_TRADING` 없이 PAPER 상태 전이만 확인한다.

작업:

- `/api/portfolio/valuation`, `/api/analytics/losses`, `/api/trading-flow/history` 상태를 기준선으로 기록한다.
- 손절/익절 조건 도달 포지션의 exit scheduler 처리 결과를 확인한다.
- 웹 선택 PAPER SELL이 선택한 보유 포지션에만 적용되는지 재확인한다.

완료 기준:

- 실패한 주문을 성공으로 처리하지 않는다.
- PAPER 현금/포지션/history가 서로 일치한다.
- 실제 주문 API와 수동 BUY는 추가되지 않는다.

## 보류

- 실제 주문 API와 `REAL_TRADING`
- 수동 BUY
- 완료된 Stage 계획 문서의 추가 상세화
