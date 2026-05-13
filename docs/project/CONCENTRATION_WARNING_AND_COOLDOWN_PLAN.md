# Concentration Warning And Cooldown Plan

## 목표

market별 쏠림 경고와 반복 손절 cooldown은 신규 BUY 판단을 더 보수적으로 만들기 위한 운영 보조 장치다.
기본 거래 모드는 계속 `PAPER_TRADING`이며, 실제 주문 API와 `REAL_TRADING`은 추가하지 않는다.

## 쏠림 경고 UI

### 표시 위치

1차 구현 위치는 Portfolio 화면의 `market별 비중(Market Exposure)` 패널이다.

- 이미 `portfolio/valuation` 응답으로 `positionValue / totalEquity`를 계산할 수 있다.
- 사용자는 보유 PAPER 포지션의 단일 market 쏠림을 가장 빠르게 확인할 수 있다.
- 선택 PAPER SELL 예외 흐름과 같은 화면에 있으므로, 수동 BUY 없이도 운영 판단이 가능하다.

2차 표시 위치는 Candidates 화면이다.

- 후보 market이 현재 보유 중이면 보유 뱃지를 유지한다.
- 후보 market의 현재 추정 노출이 경고 또는 차단 기준에 가까우면 reason 옆에 경고 뱃지를 표시한다.
- React는 리스크 판단을 재구현하지 않고 서버가 내려준 threshold와 exposure 값을 표시만 한다.

Dashboard는 요약만 표시한다.

- 가장 큰 market 노출률
- 경고 기준 이상 market 수
- 차단 기준 이상 market 수
- 반복 손절 cooldown 대상 market 수

### API 범위

구현된 `GET /api/risk/status?exchange={exchange}`는 선택 exchange의 concentration 설정을 반환한다.

- `concentration.enabled`
- `concentration.warningExposureRate`
- `concentration.blockExposureRate`
- `concentration.exchange`

Portfolio 화면은 기존 `GET /api/portfolio/valuation`의 포지션 평가값과 risk status threshold를 조합해 경고 UI를 만든다.
후보 화면에서 서버 산출 사유가 필요해지면 별도 `GET /api/risk/exposure` 또는 candidate 응답 확장을 검토한다.

### 경고 기준

- UPBIT: 7% 이상 경고, 10% 이상 신규 BUY 차단
- BINANCE: 25% 이상 경고, 40% 이상 신규 BUY 차단
- SELL, 익절, 손절은 쏠림 기준으로 막지 않는다.
- `risk.concentration.enabled=false`이면 차단은 꺼져도 UI는 기준을 참고 경고로 보여줄 수 있다.

## 반복 손절 Cooldown

### 산정 기준

초기 설계는 JPA history 기반 rolling window를 사용한다.

- 단위: `exchange + market`
- 원천: `trading_flow_history`
- 대상: `orderStatus=FILLED`, `signalType=SELL`, `signalReason`에 `Stop loss` 포함
- 기본 window: 최근 7일
- cooldown trigger: window 안에서 같은 market 손절 2회 이상
- 기본 cooldown: 마지막 손절 체결 시각부터 24시간

### Reset 조건

- cooldown은 마지막 손절 체결 시각 기준 24시간이 지나면 해제된다.
- rolling window 안 손절 count가 trigger 미만으로 내려가면 cooldown 대상에서 빠진다.
- 익절 SELL, 선택 PAPER SELL, HOLD는 cooldown reset 이벤트로 보지 않는다.
- 실패 또는 거절된 손절 주문은 cooldown count에 포함하지 않는다.

### 적용 범위

- 신규 BUY 후보 또는 신규 BUY 주문만 제한한다.
- 보유 PAPER SELL, 익절, 손절은 cooldown으로 막지 않는다.
- 최초 구현 기본값은 비활성화로 둔다.
- 설정 후보:
  - `risk.stop-loss-cooldown.enabled=false`
  - `risk.stop-loss-cooldown.window=7d`
  - `risk.stop-loss-cooldown.trigger-count=2`
  - `risk.stop-loss-cooldown.duration=24h`

## 구현 대상

Backend:

- `RiskStatusResponse`: concentration threshold 표시 필드 추가 완료
- `RiskStatusController`: `ConcentrationRiskProperties` 주입 완료
- 신규 cooldown properties/service: JPA history에서 반복 손절 market 산출 완료
- 신규 BUY guard: cooldown이 켜져 있으면 해당 market BUY 거절 완료

Frontend:

- `shared/api/types.ts`: risk status concentration 타입 추가 완료
- `PortfolioPage.tsx`: exchange별 경고/차단 threshold 기반 exposure 뱃지 표시 완료
- `CandidatesPage.tsx`: 보유/쏠림/cooldown 후보 warning 표시 범위 검토
- `DashboardPage.tsx`: 위험 market 수 요약 표시

Tests:

- backend: risk status 응답, cooldown 산정, 신규 BUY 차단, SELL 미차단 완료
- frontend: Portfolio threshold 뱃지, exchange별 기준 표시, Candidates warning 표시

## 완료 기준

- UPBIT과 BINANCE 경고 기준이 UI에서 섞이지 않는다.
- BUY 차단은 설정이 켜진 경우에만 동작한다.
- SELL, 익절, 손절 흐름은 쏠림과 cooldown 기준으로 막히지 않는다.
- 문서, 하네스, 테스트가 같은 기준을 말한다.
