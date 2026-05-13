# Project Next Steps

## 완료된 큰 흐름

- Stage 1-10 거래소 대시보드 확장 완료
  - ExchangeMode 백엔드/API 골격
  - 프론트 ExchangeMode 전환
  - Binance 공개 REST 현재가/캔들 provider
  - Exchange별 PAPER 포트폴리오와 history 분리
  - 선택 보유 PAPER 포지션 수동 매도
  - 포트폴리오 원형 그래프
  - WebSocket snapshot store와 REST fallback provider
  - candidate/exit scheduler 주기 분리
  - BTC 등락률 Market Overview 페이지
- 포트폴리오 모바일 카드형 UX 완료
- 후보 화면 SELECTED/SKIPPED 요약, 제외 사유 TOP 5, 선택 후보 필터, 보유 포지션 표시 완료
- 운영 상태 화면과 Telegram 용어 정리 완료
- Dashboard 운영 준비 상태 중심 UX 개선 완료

## 다음 우선순위: Trade 자동매매 제어 UX 정리

목표:

- 후보 스케줄러와 청산 스케줄러 제어 흐름을 더 명확하게 보여준다.
- 자동매매 ON/OFF가 실제로 후보 BUY와 보유 PAPER SELL 평가에 어떤 영향을 주는지 한 화면에서 확인하게 한다.
- 수동 BUY와 실제 거래소 주문은 계속 노출하지 않는다.
- 선택 보유 PAPER SELL은 Portfolio 화면 전용으로 유지한다.

작업:

- 자동매매 제어 영역에 후보 스케줄러, 청산 스케줄러, 후보 주기 상태를 한 흐름으로 배치한다.
- 버튼 옆에 현재 영향 범위와 차단 조건을 짧게 표시한다.
- 최근 자동 실행 테이블은 유지하되, 상태 필터나 핵심 결과를 더 빠르게 볼 수 있는 구성을 검토한다.
- 웹 API client에 후보 실행/trading-flow 실행 함수가 없는 상태를 유지한다.

완료 기준:

- Trade 화면에서 자동매매 상태와 제어 범위가 명확하다.
- 수동 실행, 수동 BUY, 실제 주문 API 버튼이 없다.
- 기존 scheduler control 테스트가 통과하고 필요한 테스트가 보강된다.
- `npm run lint`, `npm test`, `npm run build`가 성공한다.

## 다음 후보: JPA PAPER 데이터 누적 운영

목표:

- JPA history/portfolio 저장소로 며칠 이상 PAPER 운용 데이터를 쌓는다.
- market별 쏠림 리스크 기준을 확정하기 전에 원자료를 확보한다.
- 기본 거래 모드는 계속 `PAPER_TRADING`으로 유지한다.
- 실제 주문 API와 `REAL_TRADING`은 추가하지 않는다.

작업:

- 로컬/운영 `.env`에서 `HISTORY_STORAGE_TYPE=JPA`, `PAPER_PORTFOLIO_STORAGE_TYPE=JPA` 사용 여부를 확인한다.
- JPA 저장소로 실행한 PAPER history, portfolio valuation, position exposure를 점검한다.
- 충분한 데이터가 쌓이면 condition-records에 기준 산정 문서를 추가한다.
- 기준 확정 전까지 market별 쏠림 제한 로직은 보류한다.

완료 기준:

- JPA 저장소에 PAPER 실행 이력과 포트폴리오 상태가 누적된다.
- 하루 이상 데이터가 유지되는지 재시작 후 확인한다.
- market별 exposure와 반복 손절 후보를 확인할 수 있다.
- 다음 기준 문서화 작업에 사용할 원자료 위치가 명확하다.

## 보류: market별 쏠림 리스크 기준

현재 문서화된 PAPER 기록만으로 초기 기준은 잡을 수 있지만, 장기 기준은 JPA history/portfolio 데이터를 더 쌓은 뒤 확정한다.

보류 사유:

- 현재 `.env` 기본값은 `HISTORY_STORAGE_TYPE=IN_MEMORY`, `PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY`라 장기 원자료가 충분하지 않다.
- 2026-05-06 기록 기준 단일 market TOP exposure는 7.06%로, 10% 초과 쏠림 사례가 아직 없다.

재개 조건:

- JPA history/portfolio로 며칠 이상 PAPER 운용 데이터를 누적한다.
- 단일 market 10% 초과 또는 반복 손절 market이 관찰된다.
- 기준 확정 시 `docs/trading/RISK_POLICY.md`와 `docs/trading/condition-records/`를 함께 갱신한다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
