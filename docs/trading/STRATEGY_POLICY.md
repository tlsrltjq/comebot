# Strategy Policy

변동성과 추세가 확인된 코인을 롱 전용 PAPER_TRADING으로 검증한다.

실제 주문 API와 `REAL_TRADING`은 전략 코드에 넣지 않는다.

## 전략 종류

- `SIMPLE_THRESHOLD`: 기본값, 테스트용 가격 기준 전략
- `VOLATILITY_BREAKOUT_LONG`: 후보 스캔 결과로 BUY 신호 생성
- `SESSION_VOLATILITY_BREAKOUT`: Binance 15m UTC 06-12 세션 변동성 돌파 PAPER 후보

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

`SESSION_VOLATILITY_BREAKOUT`을 선택하면 `CandidateScannerService`가 별도 세션 변동성 스캐너로 위임한다.
후보 조회 API, 후보 실행 API, candidate scheduler는 같은 스캐너 결과를 사용한다.
기본 application 설정은 변경하지 않았으므로 외부 설정으로 선택하기 전까지 운영 동작은 바뀌지 않는다.

Session Volatility Breakout 기본 조건:

- 거래소: Binance 전용
- 기준봉: 15m
- 세션: UTC 06:00 이상 12:00 미만
- 돌파: 현재 close가 직전 20개 15m 캔들 high를 초과
- 평균 기준: 직전 60개 15m 캔들
- range ratio: 현재 range% / 평균 range% >= 2.5
- volume ratio: 현재 거래대금 / 평균 거래대금 >= 1.5
- close location: (close-low)/(high-low) >= 70%
- limit price: 신호 캔들 close
- 기본 제외 market: `USD1USDT`, `USDCUSDT`, `USDEUSDT`, `XAUTUSDT`

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

2026-05-28부터 UPBIT과 BINANCE는 후보 스캔 필터를 분리한다.
전역 `strategy.candidate-scanner.*` 값은 fallback으로 유지하되, 운영 `.env`에서는 아래 exchange별 override를 우선 사용한다.

| 항목 | UPBIT | BINANCE |
|---|---:|---:|
| 캔들 수 | 20 | 10 |
| 최소 가격 변화율 | 0.15% | 0.8% |
| 최소 거래대금 변화율 | 0% | 30% |
| 최신 캔들 최소 거래대금 | KRW 1,000,000 | USDT 50,000 |
| 최근 고가 대비 최대 거리 | 5% | 2% |
| 최근 고가 대비 최소 거리 | 0% | 0.5% |

분리 이유:
- UPBIT은 24시간 후보 `SELECTED`가 2건뿐이라 기존 조건이 지나치게 보수적이었다.
- BINANCE는 24시간 후보 `SELECTED`가 2,657건, BUY 92건, SELL 80건으로 과매매와 반복 손절이 발생했다.
- 따라서 UPBIT은 느슨하게, BINANCE는 보수적으로 운용한다.

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
- 스캔 제외 market: `KRW-DOGE`, `KRW-PROVE`, `KRW-TRAC` (`market.selection.excluded-markets`)
- 최소 가격 변화율: 1.0
- 최소 거래대금 변화율: 30
- 최대 가격 변화율: 10
- 최대 고가/저가 범위: 20
- 최근 고가 대비 현재가 최대 거리: 2% (`max-distance-from-high-rate`)
- 최신 캔들 최소 거래대금(KRW): 10,000,000 (`min-latest-candle-trade-amount-krw`)
- 최신 캔들 최소 거래대금(USDT): 50,000 (`min-latest-candle-trade-amount-usdt`)
- 스케줄러 1회 최대 BUY: 2 (`trading.candidate-scheduler.max-buys-per-run`)
- BTC 1h 트렌드 필터: EMA5 vs EMA10, DOWN이면 Upbit 진입 차단 (`BtcTrendCacheService`, 5분 주기 갱신)

로컬 `.env`에서 명시 설정한 값이 application.properties 기본값보다 우선 적용된다.
운영 `.env` 기준 실제 적용값은 per-exchange override 섹션과 condition-records를 참고한다.

조건 변경 후 운용 기록과 손익 스냅샷은 `docs/trading/condition-records/`에 남긴다.
