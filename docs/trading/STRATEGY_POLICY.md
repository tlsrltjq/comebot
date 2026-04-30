# Strategy Policy

## 목표

변동성과 추세가 확인된 코인을 롱 전용 PAPER_TRADING으로 검증한다.

실제 주문 API와 `REAL_TRADING`은 전략 코드에 넣지 않는다.

## 전략 종류

- `SIMPLE_THRESHOLD`: 기본값, 테스트용 가격 기준 전략
- `VOLATILITY_BREAKOUT_LONG`: 변동성 후보 스캔 결과로 BUY 신호 생성

설정:

`STRATEGY_SELECTED=SIMPLE_THRESHOLD` 또는 `VOLATILITY_BREAKOUT_LONG`을 사용한다.

## 거래 방향

- 롱만 허용한다.
- 숏, 마진, 레버리지는 만들지 않는다.
- BUY는 PAPER_TRADING 주문으로만 이어진다.
- SELL은 보유 포지션에 대한 익절/손절 정책으로만 만든다.

## 후보 기준

롱 후보는 아래 조건을 분리해서 평가한다.

- 최근 캔들 기준 가격 변화율
- 최근 캔들 기준 고가/저가 범위
- 거래대금 증가율
- 단기 추세 `MarketTrend.UP`
- 허용 market 목록

후보 조회 API는 주문을 실행하지 않는다.

후보 실행 API와 Telegram 후보 실행은 `SELECTED` 후보만 PAPER BUY로 연결한다.

## 진입 조건

`VOLATILITY_BREAKOUT_LONG`은 `CandidateScannerService` 결과가 `SELECTED`일 때만 BUY 신호를 만든다.

후보가 `SKIPPED`이면 HOLD 신호를 만든다.

최종 주문은 kill switch, risk, PAPER portfolio 검증을 통과해야 한다.

## 청산 조건

SELL 신호는 보유 포지션에 대해서만 만든다.

- 익절: 미실현 수익률이 `risk.take-profit-rate` 이상
- 손절: 미실현 수익률이 `risk.stop-loss-rate` 이하
- 보유 수량 초과 SELL 금지

## 금지 사항

- 상승 근거 없이 변동성만 보고 매수하지 않는다.
- 손절 기준을 무시하지 않는다.
- 실패한 주문을 체결로 간주하지 않는다.
- 전략 코드에서 Telegram, DB, Controller 로직을 처리하지 않는다.
- 전략 코드에서 실제 주문 API를 호출하지 않는다.

## 테스트 기준

전략 변경 시 최소 테스트:

- BUY 신호 생성
- HOLD 신호 유지
- 후보 미선택 시 주문 미생성
- scanner 실패 시 HOLD 처리
- 기존 PAPER_TRADING 흐름 유지
