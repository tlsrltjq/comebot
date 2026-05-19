# 2026-05-13 JPA PAPER Data Snapshot

## 목적

JPA history/portfolio 저장소에 market별 쏠림 리스크 기준을 산정할 수 있는 원자료가 있는지 확인했다.

실제 주문 API와 `REAL_TRADING`은 사용하지 않는다.

## 확인 환경

- 확인일: 2026-05-13
- DB: local PostgreSQL container `comebot-postgres`
- schema: `src/main/resources/schema.sql` 적용 완료
- 현재 `.env` 확인값:
  - `HISTORY_STORAGE_TYPE=IN_MEMORY`
  - `PAPER_PORTFOLIO_STORAGE_TYPE=IN_MEMORY`
  - `TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE=IN_MEMORY`
- JPA 누적 실행은 현재 `scripts/run-paper-jpa.sh`를 사용한다. 당시 실행 경로였던 `scripts/run-upbit-paper-jpa.sh`는 호환 alias로 유지한다.

## 누적 데이터

| Exchange | History rows | First row | Last row |
| --- | ---: | --- | --- |
| BINANCE | 54,383 | 2026-05-09 01:50:42 UTC | 2026-05-11 05:45:34 UTC |
| UPBIT | 179,529 | 2026-05-09 01:50:29 UTC | 2026-05-11 05:45:34 UTC |

Portfolio state:

| Exchange | Open positions | Cost basis | Cash | Realized profit |
| --- | ---: | ---: | ---: | ---: |
| BINANCE | 5 | 1000.0000001041604014 USDT | 8.828168877079838 USDT | 8.9210992324442944 USDT |
| UPBIT | 27 | 989999.9996570944011711 KRW | 8848.3272601148 KRW | -1151.6730866872 KRW |

## 추정 노출 상위

현재가 평가가 아니라 `quantity * average_buy_price` 기준 추정 비중이다.

| Exchange | Market | Cost basis | Estimated exposure |
| --- | --- | ---: | ---: |
| BINANCE | UTKUSDT | 950.00000000 | 94.1687% |
| UPBIT | KRW-BLEND | 89999.99999269 | 9.0104% |
| UPBIT | KRW-XPL | 79999.99999322 | 8.0092% |
| UPBIT | KRW-ENA | 59999.99999697 | 6.0069% |
| UPBIT | KRW-MEGA | 59999.99999643 | 6.0069% |
| UPBIT | KRW-ARB | 59999.99999485 | 6.0069% |

## 반복 손절 상위

`signal_type = SELL`이고 `signal_reason`에 `Stop loss`가 포함된 history 기준이다.

| Exchange | Market | Stop loss count |
| --- | --- | ---: |
| UPBIT | KRW-DEEP | 3,204 |
| UPBIT | KRW-ICP | 2,962 |
| UPBIT | KRW-CHIP | 2,603 |
| UPBIT | KRW-SPK | 2,594 |
| UPBIT | KRW-XPL | 2,473 |
| UPBIT | KRW-CFG | 2,455 |
| UPBIT | KRW-NEAR | 2,435 |
| UPBIT | KRW-ARB | 2,431 |
| UPBIT | KRW-APT | 2,374 |
| UPBIT | KRW-ZBT | 2,284 |
| UPBIT | KRW-ENA | 2,274 |
| UPBIT | KRW-ONDO | 2,174 |

## 판단

- JPA history와 portfolio에는 market별 쏠림 리스크 기준을 잡을 수 있는 원자료가 있다.
- UPBIT은 단일 market 추정 비중이 10%에 근접한 `KRW-BLEND`와 8%대 `KRW-XPL`이 관찰됐다.
- BINANCE는 `UTKUSDT` 한 종목에 90% 이상 집중되어 exchange별 별도 기준이 필요하다.
- 반복 손절 market은 노출 비중 기준과 함께 제한 후보로 검토해야 한다.

## 다음 기준 후보

- 단일 market 추정 비중 10% 이상: 신규 BUY 차단 후보
- 단일 market 추정 비중 7% 이상: 경고 후보
- 반복 손절 count 상위 market: cooldown 또는 후보 제외 기준 검토
- UPBIT과 BINANCE는 초기 현금 규모와 market universe가 달라 별도 threshold를 둔다.
