# tasks/stock-expansion-plan.md — 주식 PAPER 확장 계획

## 목적

코인 PAPER 자동매매 기능은 그대로 유지하면서, 주식 시장에서도 전략 후보를 찾고 검증할 수 있는 별도 PAPER 리서치 라인을 추가한다.
초기 범위는 실제 주문이 없는 `PAPER_TRADING` 전용이며, 주식 실주문 API와 실거래 모드는 구현하지 않는다.

## 현실 판단

- 코인 시장 부진 구간에서는 전략 후보가 적거나 손익 변동성이 커질 수 있다.
- 주식 시장은 장 시간, 휴장일, 갭, 섹터/지수 레짐 영향이 커서 코인 전략을 그대로 복사하면 안 된다.
- 확장 자체는 가능하지만, 기존 `UPBIT`/`BINANCE` 경로와 섞으면 리스크·포트폴리오·스케줄러 해석이 꼬인다.
- 따라서 주식은 별도 market universe, candle cache, PAPER portfolio, scanner profile로 분리한다.

## 불변 규칙

- `REAL_TRADING` 구현 금지.
- 주식 브로커 실주문 API 호출 금지.
- API 키, 계좌번호, 토큰, 비밀번호 하드코딩 금지.
- 기존 코인 PAPER 기능과 스케줄러 기본값을 깨지 않는다.
- 주식 자동매매도 초기에는 scheduler 기본 OFF, 관찰 전용으로 둔다.

## 1차 시장 선택

초기 추천은 미국 주식이다.

| 후보 | 장점 | 리스크 |
|---|---|---|
| 미국 주식 | 유동성 높음, 대형주/ETF 데이터 품질 좋음, 전략 표본 확보 쉬움 | 데이터 공급자 선택 필요, 장 시간/휴장일 처리 필요 |
| 국내 주식 | 원화 기준 이해 쉬움 | 데이터 접근 제약, 체결/호가/거래대금 해석 차이, 자동화 제약 큼 |

1차 universe는 미국 대형주/ETF 소수로 시작한다.

- ETF: `SPY`, `QQQ`, `IWM`
- 대형주: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`, `GOOGL`
- 초기 목표는 수익 극대화가 아니라 데이터 파이프라인과 검증 하니스 안정화다.

## 데이터는 어디에 쓰는가

수집한 주식 데이터는 운영 매매에 바로 쓰기보다 전략을 살리거나 버리는 증거로 사용한다.

1. 백테스트
   - 원본 캔들로 진입/청산 규칙을 재생한다.
   - PFgross, PFnet, MDD, 승률, 거래 수, 종목 집중도를 계산한다.

2. OOS 검증
   - train 구간에서 좋아 보이는 전략이 test 구간에서도 유지되는지 확인한다.
   - train만 좋고 test가 무너지면 과적합으로 탈락시킨다.

3. 비용/슬리피지 스트레스
   - 주식 수수료, 스프레드, 갭 리스크를 보수적으로 반영한다.
   - 비용을 넣으면 무너지는 전략은 PAPER 후보로 올리지 않는다.

4. PAPER 관찰 비교
   - 백테스트상 체결 가능했던 신호가 PAPER에서는 미체결/지연/갭으로 깨지는지 확인한다.
   - 후보 스캔 로그, 주문 로그, 포트폴리오 손익을 같이 본다.

5. 레짐 필터 연구
   - `SPY`, `QQQ`, 섹터 ETF, VIX 유사 지표를 이용해 시장 상태별 성능 차이를 본다.
   - 단, 레짐 필터는 OOS 통과 전까지 운영 기본값으로 올리지 않는다.

## 캐시/저장 구조 원칙

주식 데이터는 코인 데이터와 분리한다.

```text
.backtest_cache/
  crypto/
    upbit/
    binance/
  stock/
    us/
      1m/
      5m/
      15m/
      1d/
```

저장 시 원본 데이터와 변환 데이터를 구분한다.

- 원본 캔들: 데이터 공급자가 준 값을 그대로 저장한다.
- 정규화 캔들: 장 시간, timezone, split/dividend 조정 여부를 명시한다.
- manifest: symbol, interval, since/until, provider, adjusted 여부, 수집 시각을 기록한다.

## 구현 단계

### 0단계 — 설계 고정

- `MarketAssetClass` 또는 동등한 구분값으로 `CRYPTO`와 `STOCK` 경계를 만든다.
- `ExchangeMode`에 주식을 바로 억지로 넣을지, 별도 `MarketVenue`를 둘지 결정한다.
- 주식은 `PAPER_TRADING` 전용임을 문서와 테스트에 고정한다.

완료 기준:

- architecture/spec/tasks 문서에 주식 확장 원칙 반영.
- 기존 코인 테스트 영향 없음.

### 1단계 — 데이터 수집기

- 미국 주식 OHLCV 수집 provider를 선택한다.
- 초기 universe 10개 내외, interval `1m/5m/15m/1d`로 시작한다.
- 장 시간과 timezone을 manifest에 기록한다.

완료 기준:

- 원본 캔들 저장.
- 재실행 시 완료 파일 skip.
- 데이터 gap/중복/시간 역전 검사 통과.

### 2단계 — 백테스트 하니스 연결

- 기존 리더보드 구조를 재사용하되 asset class, venue, symbol 컬럼을 추가한다.
- 주식 비용 모델을 crypto 비용 모델과 분리한다.
- train/test OOS split을 유지한다.

완료 기준:

- 최소 2개 전략군 산출.
- 생존 후보 0이어도 결과를 기록한다.

### 3단계 — 주식 PAPER 포트폴리오

- `US_STOCK` PAPER 계좌를 crypto 계좌와 분리한다.
- 현금 통화는 우선 `USD`로 둔다.
- fractional share 허용 여부를 설정값으로 분리한다.

완료 기준:

- BUY/SELL 기록이 기존 crypto history와 구분된다.
- 포지션 상한, 일 손실, 집중도 리스크가 주식 계좌 기준으로 계산된다.

### 4단계 — 후보 API/대시보드

- 후보 조회에서 asset class 필터를 추가한다.
- 웹에서는 crypto와 stock을 명확히 전환한다.
- 수동 BUY UI는 계속 금지한다.

완료 기준:

- 주식 후보 조회 가능.
- 주식 PAPER 자동 실행은 아직 OFF.

### 5단계 — PAPER 관찰

- readiness 경고가 비어 있을 때만 scheduler를 켠다.
- 장 시간에만 후보 스캔을 실행한다.
- 첫 5거래일 관찰 후 계속 운용 여부를 결정한다.

완료 기준:

- `docs/trading/condition-records/`에 주식 PAPER 관찰 기록 추가.
- 후보/체결/미체결/손익/슬리피지 기록.

## 초기 전략 후보

주식은 코인과 다르게 갭과 장 시작 변동성이 크므로 다음 순서로 검증한다.

1. Opening Range Breakout
   - 장 시작 후 N분 고저 범위 돌파.
   - 유동성 큰 ETF/대형주에 적합.

2. Session Volatility Breakout
   - 코인 후보 전략을 주식 장 시간 구조에 맞게 재해석.
   - 그대로 이식하지 않고 별도 파라미터 sweep.

3. Relative Strength Rotation
   - `SPY`/`QQQ` 대비 강한 종목만 후보.
   - 시장 전체 약세장에서 방어 필터 역할 가능.

4. Gap Fade / Gap Follow
   - 전일 종가 대비 갭 이후 되돌림 또는 추세 지속.
   - 거래 수와 비용 스트레스가 충분할 때만 후보화.

## 중단/탈락 기준

- train PFgross < 1.05면 폐기.
- test PFnet < 1.00이면 PAPER 후보 제외.
- 거래 수가 너무 적거나 특정 1개 종목에 과도하게 집중되면 제외.
- 비용/슬리피지 stress에서 전부 무너지면 관찰 후보로 올리지 않는다.
- 장 시작 특정 몇 분에만 과최적화된 결과는 별도 보수 검증 전 제외한다.

## 다음 작업

1. 미국 주식 데이터 공급자 후보를 정한다.
2. 주식 symbol/venue/asset class 모델링 방식을 결정한다.
3. `.backtest_cache` crypto/stock 분리 계획을 기존 캐시 prune 계획과 함께 실행한다.
4. 초기 universe와 수집 기간을 고정한다.

## Decision Update 2026-06-25 - US Stock Data Provider

Current provider decision:

- Do not start the stock expansion by binding the app to a paid or account-specific
  market-data API.
- Build the first stock research path around a provider-neutral candle import interface
  and local cached files under `.backtest_cache/stock/us/`.
- API-backed providers can be added after the import format, asset-class model, and
  stock-specific cost model are stable.

Reasoning:

- Alpha Vantage documents intraday equity support for `1min`, `5min`, `15min`, `30min`,
  and `60min`, but historical/realtime intraday access is a premium endpoint. It is useful
  as a later provider, not as the first no-surprise implementation dependency.
- Polygon/Massive documents stock aggregate bars and is suitable as a later API-backed
  provider, but it should be kept behind an interface because access and plan constraints
  are external to this repository.
- The first repository change should therefore define the stock candle data shape, manifest,
  symbol model, and file import validation before any network provider is implemented.

Initial implementation direction:

1. Add explicit stock identity fields: asset class `STOCK`, venue `US_STOCK`, symbol such as
   `AAPL`, quote currency `USD`, timezone `America/New_York`.
2. Store imported raw candles with provider metadata:
   `.backtest_cache/stock/us/{provider}/{interval}/{symbol}.csv`.
3. Require manifest fields: provider, symbol, interval, timezone, regular/extended hours,
   adjusted/raw, since, until, collectedAt.
4. Start with daily and 15m import validation; add 1m/5m only after storage size and
   provider availability are confirmed.
5. Keep stock PAPER automation OFF until backtest/OOS evidence exists and the first
   condition record is written.

## Decision Update 2026-06-25 - Asset Class and Venue Modeling

Do not add `US_STOCK` directly to the existing crypto `ExchangeMode` execution path.

Use the following model boundary for the stock expansion:

- `ExchangeMode`: keep for existing crypto execution (`UPBIT`, `BINANCE`).
- `MarketAssetClass`: introduce `CRYPTO` and `STOCK` when code work starts.
- `MarketVenue`: introduce `UPBIT`, `BINANCE`, and `US_STOCK` or an equivalent value object
  when code work starts.
- Market identity: `(assetClass, venue, symbol)`.
- US stock metadata: quote currency `USD`, timezone `America/New_York`, regular/extended
  hours flag, adjusted/raw candle flag.

This keeps stock research from accidentally sharing crypto scheduler, risk, and portfolio
semantics before the stock PAPER path is explicitly separated.

## Decision Update 2026-06-25 - Cache Split and Prune

Current `.backtest_cache` is about 9.39 GB with 241 JSON files. The existing Java
backtest loader reads flat JSON files directly under `.backtest_cache`, so moving crypto
files now would break opt-in backtests.

Decision:

- Keep the existing flat crypto cache unchanged for now.
- Start stock cache files only under `.backtest_cache/stock/us/`.
- Defer crypto cache relocation to `.backtest_cache/crypto/{upbit,binance}/` until the
  backtest loader and collection scripts support nested paths.
- Do not prune 1m/5m/15m crypto files while Session Volatility maker-fill validation is
  still being observed.
- Treat accidental non-cache exports, such as database dump files outside `.backtest_cache`,
  as removable local artifacts.
