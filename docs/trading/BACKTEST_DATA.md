# 백테스트 캔들 데이터 수집 정책

## 목적

전략 리서치는 Upbit KRW와 Binance USDT를 동시에 확장하고, 1m/3m/5m/15m 원본 캔들을 별도 수집해 진행한다.
파생 변환 캔들은 편하지만 거래소 원본 기준봉의 체결 시각, 거래대금 집계, 캔들 경계 차이를 숨길 수 있으므로 사용하지 않는다.

## 저장 위치

- 기본 저장 경로: `.backtest_cache/`
- 이 디렉터리는 git에 커밋하지 않는다.
- 파일명: `{MARKET}_{unit}m_{YYYYMMDD}_{YYYYMMDD}.json`
- 예시:
  - `KRW-BTC_1m_20250612_20260612.json`
  - `BTCUSDT_15m_20250612_20260612.json`
- 수집 메타데이터: `.backtest_cache/manifest.json`

## JSON 스키마

모든 거래소의 캔들은 Java 백테스트 로더가 바로 읽을 수 있도록 Upbit minute candle 호환 필드로 정규화한다.

```json
{
  "market": "KRW-BTC",
  "candle_date_time_utc": "2026-06-12T00:00:00",
  "opening_price": 100,
  "high_price": 110,
  "low_price": 95,
  "trade_price": 105,
  "candle_acc_trade_price": 1000000,
  "candle_acc_trade_volume": 10.5
}
```

Binance의 `candle_acc_trade_price`는 quote asset volume, `candle_acc_trade_volume`은 base asset volume으로 매핑한다.

## 수집 명령

1년치 원본 캔들 수집:

```bash
python3 scripts/collect-backtest-candles.py \
  --since 2025-06-12T00:00:00Z \
  --until 2026-06-12T00:00:00Z \
  --units 1,3,5,15 \
  --upbit-markets ALL_KRW \
  --binance-symbols ALL_USDT \
  --upbit-top 30 \
  --binance-top 30
```

실제 수집 전 작업량 확인:

```bash
python3 scripts/collect-backtest-candles.py \
  --since 2025-06-12T00:00:00Z \
  --until 2026-06-12T00:00:00Z \
  --dry-run
```

특정 마켓만 재수집:

```bash
python3 scripts/collect-backtest-candles.py \
  --since 2025-06-12T00:00:00Z \
  --until 2026-06-12T00:00:00Z \
  --units 1,3,5,15 \
  --upbit-markets KRW-BTC,KRW-ETH \
  --binance-symbols BTCUSDT,ETHUSDT
```

이미 존재하는 파일은 기본적으로 건너뛴다. 같은 기간/기준봉을 강제로 다시 받을 때만 `--overwrite`를 붙인다.

## 유니버스 기준

- 확장 수집 기본값은 거래대금 상위 30개다.
- Upbit는 KRW 마켓의 24시간 누적 거래대금(`acc_trade_price_24h`) 순서로 고른다.
- Binance는 spot USDT 심볼 중 거래 가능 상태의 24시간 quote volume 순서로 고른다.
- 수집 시점 상위 30개를 고정 유니버스로 삼고, 모든 전략 후보를 같은 유니버스/기간/기준봉으로 비교한다.
- 리더보드 판정은 Upbit와 Binance를 분리한 뒤 같은 컬럼으로 비교한다.
- BTC/ETH 시드 실험의 반복 탈락 원인은 표본 부족과 ETH 편중이므로, 다음 확장 수집은 `ALL_KRW --upbit-top 30`과 `ALL_USDT --binance-top 30`을 동시에 수행한다.
- 이 기준은 생존편향을 완전히 제거하지 못한다. 운영 후보가 나오면 월별 거래대금 재산정 유니버스와 고정 유니버스 결과를 별도로 비교한다.

## 검증 절차

수집 후 다음을 확인한다.

```bash
python3 scripts/collect-backtest-candles.py --since 2025-06-12T00:00:00Z --dry-run
ls -lh .backtest_cache | head
```

백테스트 실행 전에는 최소한 다음을 본다.

- `manifest.json`의 수집 기간, 기준봉, 마켓 목록이 의도와 맞는지 확인한다.
- 각 파일의 캔들 수가 기준봉별 기대치와 크게 어긋나지 않는지 확인한다.
- 기준봉 간격 누락이 있는지 확인한다. Upbit 원본에는 거래소 점검/데이터 공백으로 보이는 gap이 있을 수 있으므로, 전략 결과에 영향이 있으면 별도 레짐/제외 처리가 필요하다.
- Upbit와 Binance는 분리 평가하고, 이후 리더보드에서 같은 컬럼으로 비교한다.

## 현재 로컬 시드 캐시

2026-06-12 기준으로 `.backtest_cache`에 다음 1년치 시드 데이터를 수집했다.

- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- 기준봉: 1m, 3m, 5m, 15m
- Upbit: `KRW-BTC`, `KRW-ETH`
- Binance: `BTCUSDT`, `ETHUSDT`
- 크기: 약 866MB, 16개 캔들 파일 + `manifest.json`
- 검증: Binance는 기준봉 간격 연속. Upbit는 일부 gap 존재 확인.

## 현재 확장 수집 상태

2026-06-15에 거래대금 상위 30 고정 유니버스 수집을 완료했다.

- 수집 대상: Upbit KRW 24h 거래대금 상위 30 + Binance spot USDT quote volume 상위 30
- 기간: 2025-06-12T00:00:00Z ~ 2026-06-12T00:00:00Z
- 기준봉: 1m, 3m, 5m, 15m
- 완료 파일: 240개 JSON
- 크기: 약 9.0GB
- 수집 완료 시각: 2026-06-15T00:58:53Z
- manifest: `.backtest_cache/manifest.json`
- 수집기 보강:
  - 진행률 로그 출력
  - Upbit 상장 이전 구간에서 cursor가 충분히 이동하지 않으면 중단
  - HTTP 재시도 10회, `Connection: close`, 최대 60초 backoff

Upbit 30개:

- `KRW-WLD`, `KRW-BTC`, `KRW-MEGA`, `KRW-TRUMP`, `KRW-XRP`
- `KRW-USDT`, `KRW-ETH`, `KRW-TAO`, `KRW-ZKP`, `KRW-SAHARA`
- `KRW-ONDO`, `KRW-NEAR`, `KRW-VVV`, `KRW-IRYS`, `KRW-DOGE`
- `KRW-XLM`, `KRW-B3`, `KRW-AXL`, `KRW-SOL`, `KRW-AKT`
- `KRW-ENSO`, `KRW-SUI`, `KRW-OPEN`, `KRW-XPL`, `KRW-ZKC`
- `KRW-CHIP`, `KRW-IP`, `KRW-ID`, `KRW-ZK`, `KRW-JTO`

Binance 30개:

- `USDCUSDT`, `BTCUSDT`, `ETHUSDT`, `WLDUSDT`, `XAUTUSDT`
- `USD1USDT`, `SOLUSDT`, `TAOUSDT`, `ZECUSDT`, `XRPUSDT`
- `NEARUSDT`, `BNBUSDT`, `TRUMPUSDT`, `DOGEUSDT`, `NIGHTUSDT`
- `MEGAUSDT`, `FETUSDT`, `BABYUSDT`, `RIFUSDT`, `TRXUSDT`
- `SUIUSDT`, `STGUSDT`, `ENAUSDT`, `PEPEUSDT`, `USDEUSDT`
- `XPLUSDT`, `ICPUSDT`, `ADAUSDT`, `TONUSDT`, `DEXEUSDT`

재수집 명령:

```bash
python3 scripts/collect-backtest-candles.py \
  --since 2025-06-12T00:00:00Z \
  --until 2026-06-12T00:00:00Z \
  --units 1,3,5,15 \
  --upbit-markets ALL_KRW \
  --binance-symbols ALL_USDT \
  --upbit-top 30 \
  --binance-top 30 \
  --request-delay-sec 0.5
```

이미 완료된 파일은 자동으로 skip된다. 같은 기간/기준봉을 강제로 다시 받을 때만 `--overwrite`를 붙인다.

## 운영 자동매매 정책

새 전략 후보가 나오기 전까지 기본 실행 경로의 candidate/exit scheduler는 꺼 둔다.
대시보드, 시세, 포트폴리오, 히스토리 조회는 유지하되 자동 BUY/SELL 평가는 수행하지 않는다.
새 전략이 PFgross/OOS/체결 현실성 게이트를 통과하면 `/api/scheduler/control`로 PAPER 자동매매를 켜고 관찰한다.
