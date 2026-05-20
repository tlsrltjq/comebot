# Strategy Policy

변동성과 추세가 확인된 코인을 롱 전용 PAPER_TRADING으로 검증한다.

실제 주문 API와 `REAL_TRADING`은 전략 코드에 넣지 않는다.

## 전략 종류

- `SIMPLE_THRESHOLD`: 기본값, 테스트용 가격 기준 전략
- `VOLATILITY_BREAKOUT_LONG`: 후보 스캔 결과로 BUY 신호 생성

설정은 `STRATEGY_SELECTED`로 선택한다.

## 거래 방향

- 롱만 허용한다.
- 숏, 마진, 레버리지는 만들지 않는다.
- BUY는 PAPER_TRADING 주문으로만 이어진다.
- SELL은 보유 포지션에 대한 익절/손절 정책으로만 만든다.

롱 후보는 아래 조건을 분리해서 평가한다.

- 단기 추세 `MarketTrend.UP` (가격 변화율 > 0)
- 마지막 캔들 양봉 (`lastCandleBullish`)
- 최근 캔들 기준 가격 변화율 (`min-price-change-rate` 이상)
- 최근 캔들 기준 거래대금 증가율 (`min-trade-amount-change-rate` 이상)
- 과도한 가격 변화율 회피 (`max-price-change-rate` 이하)
- 최근 캔들 기준 고가/저가 범위 (`max-high-low-range-rate` 이하)
- 최근 고가 대비 현재가 거리 (`max-distance-from-high-rate` 이하, 0이면 비활성)
- 최신 캔들 최소 거래대금 절대값 (`min-latest-candle-trade-amount-krw`/`-usdt`, 0이면 비활성)
- 허용 market 목록
- market별 override 설정

후보 조회 API는 주문을 실행하지 않는다.
후보 실행 API와 Telegram 후보 실행은 `SELECTED` 후보만 PAPER BUY로 연결한다.

`VOLATILITY_BREAKOUT_LONG`은 `CandidateScannerService` 결과가 `SELECTED`일 때만 BUY 신호를 만든다.

후보가 `SKIPPED`이면 HOLD 신호를 만든다.

현재 기본값에서는 같은 market PAPER 포지션을 보유 중이어도 추가 진입을 허용한다.
이 설정은 1,000,000 KRW PAPER 현금을 더 적극적으로 사용하기 위한 것이다.
같은 market 재진입 차단이 필요하면 `STRATEGY_ENTRY_PREVENT_REENTRY_WITH_POSITION=true`로 명시 설정한다.

최종 주문은 kill switch, risk, PAPER portfolio 검증을 통과해야 한다.

공통 기본값은 유지하고 필요한 market만 override한다.

- 주문 수량
- 최소 가격 변화율
- 최소 거래대금 증가율
- 최대 가격 변화율
- 최대 고가/저가 범위

스케줄러 1회 실행당 신규 BUY 수는 `trading.candidate-scheduler.max-buys-per-run`(기본값 2)으로 제한한다.
0으로 설정하면 제한 없음.

SELL 신호는 보유 포지션에 대해서만 만든다.

- 익절: 미실현 수익률이 `risk.take-profit-rate` 이상
- 손절: 미실현 수익률이 `risk.stop-loss-rate` 이하
- 보유 수량 초과 SELL 금지

- 하락 추세에서는 매수하지 않는다.
- 손절 기준을 무시하지 않는다.
- 실패한 주문을 체결로 간주하지 않는다.
- 전략 코드에서 Telegram, DB, Controller 로직을 처리하지 않는다.
- 전략 코드에서 실제 주문 API를 호출하지 않는다.
전략 변경 시 BUY, HOLD, 후보 미선택, scanner 실패, 재진입 허용/차단 설정을 테스트한다.

거래대금 상위 코인에서 상승과 거래대금 증가가 같이 확인된 경우만 매수한다.

`application.properties` 기준 기본값:

- 최근 캔들 수: 5 (1분 캔들)
- 전체 KRW 중 24시간 거래대금 상위 20개 (`market.selection.top-krw-market-limit`)
- 전체 USDT 중 24시간 거래대금 상위 30개 (`market.selection.top-usdt-symbol-limit`)
- 최소 가격 변화율: 1.0
- 최소 거래대금 변화율: 30
- 최대 가격 변화율: 10
- 최대 고가/저가 범위: 20
- 최근 고가 대비 현재가 최대 거리: 2% (`max-distance-from-high-rate`)
- 최신 캔들 최소 거래대금(KRW): 10,000,000 (`min-latest-candle-trade-amount-krw`)
- 최신 캔들 최소 거래대금(USDT): 50,000 (`min-latest-candle-trade-amount-usdt`)
- 스케줄러 1회 최대 BUY: 2 (`trading.candidate-scheduler.max-buys-per-run`)

`.env.example`은 보수적인 로컬 테스트용 값이며 위 기본값과 다르다.
로컬 `.env`에서 명시 설정한 값이 application.properties 기본값보다 우선 적용된다.

조건 변경 후 운용 기록과 손익 스냅샷은 `docs/trading/condition-records/`에 남긴다.
