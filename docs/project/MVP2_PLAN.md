# MVP2 Plan

## 목표

MVP2는 MVP1의 Upbit 롱 전용 PAPER 자동매매 검증과 분리해서, 여러 거래소와 여러 매매 방법을 동시에 비교하는 실험 플랫폼으로 확장한다.

핵심 목표는 실제 주문이 아니라, 여러 전략을 같은 시장 조건에서 PAPER/SIMULATION으로 돌려 보고 가장 높은 수익률과 가장 안정적인 조건을 계속 갱신하는 것이다.

## MVP1과의 구분

MVP1:

- Upbit 중심
- 현물 롱 전용
- 하나의 기본 전략을 개선
- PAPER_TRADING으로 자동 실행 검증
- 웹은 모니터링 전용

MVP2:

- Upbit와 Binance를 모두 지원
- 거래소별 대시보드를 분리
- 여러 전략을 동시에 실행
- 안정형, 공격형, 수비형 전략 세트를 같은 시간에 비교
- 현물, 선물 롱, 선물 숏을 시뮬레이션 계층으로 확장
- 전략별 손익, 승률, 최대 낙폭, 반복 손실 원인을 비교

## 절대 유지할 제약

- MVP2 초기 버전도 기본은 `PAPER_TRADING` 또는 `SIMULATION`이다.
- 실제 주문 API는 구현하지 않는다.
- `REAL_TRADING`은 구현하지 않는다.
- Binance API Key, Secret Key는 코드에 하드코딩하지 않는다.
- Upbit Access Key, Secret Key도 계속 사용하지 않는다.
- 선물 롱/숏은 실제 주문이 아니라 시뮬레이션 포지션으로 먼저 구현한다.
- 웹에 수동 BUY/SELL 버튼을 만들지 않는다.
- 전략 비교 결과가 좋더라도 자동으로 실거래로 전환하지 않는다.

## 핵심 개념

### Exchange

거래소 단위를 명확히 분리한다.

- `UPBIT`
- `BINANCE`

각 거래소는 같은 인터페이스를 통해 시세, 캔들, market 목록을 제공한다.

### Venue Dashboard

웹에는 거래소 선택 UI를 둔다.

- Upbit 버튼
- Binance 버튼

각 버튼은 별도 대시보드 상태를 보여준다.

- 거래소 연결 상태
- 수집 중인 market
- 실행 중인 전략 세트
- PAPER/SIMULATION 포트폴리오
- 전략별 손익
- 최근 BUY/SELL/HOLD
- 손절/익절 기록

### Strategy Profile

전략은 하나씩 바꾸는 대신 프로필 단위로 동시에 실행한다.

- 안정형(Stable)
  - 낮은 진입 빈도
  - 강한 추세 확인
  - 낮은 손실 허용
  - 목표: 손실 최소화와 완만한 수익

- 공격형(Aggressive)
  - 높은 진입 빈도
  - 빠른 변동성 포착
  - 비교적 넓은 손절
  - 목표: 높은 수익률 후보 탐색

- 수비형(Defensive)
  - 거래대금, 추세, market별 노출 제한을 강하게 적용
  - 특정 market 쏠림을 낮게 유지
  - 목표: 횡보장과 급락장에서 방어

### Market Type

MVP2는 market type을 분리한다.

- `SPOT_LONG`
- `FUTURES_LONG_SIM`
- `FUTURES_SHORT_SIM`

초기에는 모두 PAPER/SIMULATION만 지원한다.

## 백엔드 설계 방향

### 신규 도메인

- `exchange`
  - 거래소 식별자
  - 거래소별 market symbol 정규화
  - 시세 provider 라우팅

- `experiment`
  - 전략 실험 실행 단위
  - 거래소, market type, 전략 profile, 실행 기간을 묶는다.

- `strategy.profile`
  - 안정형, 공격형, 수비형 설정
  - threshold, take profit, stop loss, max positions, max market exposure 등을 profile별로 분리한다.

- `simulation`
  - spot/futures simulation position 관리
  - long/short PnL 계산
  - 수수료, 슬리피지, 레버리지 설정은 시뮬레이션 값으로만 다룬다.

- `leaderboard`
  - 전략별 성과 순위
  - 수익률, 승률, 최대 낙폭, 손실 빈도, 거래 수를 집계한다.

### 기존 모듈 확장

- `market`
  - Upbit provider와 Binance provider를 같은 인터페이스로 묶는다.
  - exchange별 ticker/candle fetcher를 분리한다.

- `portfolio`
  - MVP1 PAPER portfolio와 MVP2 experiment portfolio를 분리한다.
  - 전략 profile별 독립 portfolio를 둔다.

- `history`
  - history에 exchange, strategyProfile, marketType, experimentId를 추가한다.

- `analytics`
  - strategyProfile별 성과 비교 API를 추가한다.
  - exchange별 성과 비교 API를 추가한다.

## 프론트엔드 설계 방향

### 화면 구조

- MVP1 Dashboard
  - 현재 Upbit PAPER 자동매매 상태 유지

- MVP2 Experiment Dashboard
  - Upbit / Binance 선택 버튼
  - Spot / Futures Long / Futures Short 선택
  - 안정형 / 공격형 / 수비형 전략 비교
  - 전략별 포트폴리오, 손익, 승률, 최대 낙폭 표시

- Strategy Lab
  - 실험 생성
  - 전략 profile 설정 확인
  - 실험 ON/OFF 상태 확인
  - 실험 결과 비교

- Leaderboard
  - 기간별 최고 수익률 전략
  - 가장 낮은 최대 낙폭 전략
  - 가장 좋은 손익비 전략
  - 반복 손실 market과 제외 후보

### UX 원칙

- 거래소별 화면은 명확히 분리한다.
- 실험 화면에는 실제 주문 버튼을 두지 않는다.
- 전략 실행 상태와 결과를 한글(English)로 표시한다.
- 수익률만 보지 않고 최대 낙폭, 손실 빈도, 거래 수를 같이 보여준다.
- 전략 결과가 좋은 경우에도 "실거래 전환" 버튼은 만들지 않는다.

## API 초안

### Exchange

```http
GET /api/mvp2/exchanges
GET /api/mvp2/exchanges/{exchange}/status
GET /api/mvp2/exchanges/{exchange}/markets
```

### Experiments

```http
GET /api/mvp2/experiments
POST /api/mvp2/experiments
GET /api/mvp2/experiments/{experimentId}
GET /api/mvp2/experiments/{experimentId}/history
GET /api/mvp2/experiments/{experimentId}/portfolio
GET /api/mvp2/experiments/{experimentId}/analytics
```

초기 `POST /api/mvp2/experiments`는 실제 거래가 아니라 실험 설정 저장과 PAPER/SIMULATION 실행 준비만 한다.

### Leaderboard

```http
GET /api/mvp2/leaderboard?range=24h
GET /api/mvp2/leaderboard?exchange=BINANCE&range=7d
GET /api/mvp2/leaderboard?marketType=FUTURES_SHORT_SIM&range=7d
```

## 구현 단계

## 권장 진행 순서

MVP2는 아래 순서대로 진행한다. 각 항목은 가능한 한 하나의 PR 또는 하나의 커밋 단위로 끝낼 수 있게 자른다.

1. MVP2 패키지와 용어 경계 만들기
   - `exchange`, `experiment`, `simulation`, `leaderboard` 패키지 초안
   - enum 초안: `Exchange`, `MarketType`, `StrategyProfile`
   - 실제 주문 금지 테스트 유지
   - 상태: 완료

2. Exchange 공통 모델 만들기
   - 거래소별 symbol 정규화 규칙
   - 공통 ticker/candle DTO
   - Upbit adapter부터 기존 provider와 연결
   - 상태: 완료

3. Binance public market data 추가
   - Binance ticker 조회
   - Binance candle 조회
   - 인증키 없는 public API만 사용
   - API 실패 예외 처리와 테스트
   - 상태: 완료

4. 거래소별 상태 API 만들기
   - `GET /api/mvp2/exchanges`
   - `GET /api/mvp2/exchanges/{exchange}/status`
   - 웹에서 Upbit/Binance 버튼을 만들 수 있는 최소 데이터 제공
   - 상태: 완료

5. 실험 엔진의 저장 모델 만들기
   - `experimentId`
   - `exchange`
   - `marketType`
   - `strategyProfile`
   - 실행 상태, 시작/종료 시각
   - 처음에는 DB 저장보다 in-memory로 시작 가능

현재 사용자 요청에 따라 5단계보다 먼저 Binance PAPER 자동매매 기능을 MVP1 Upbit PAPER 기능과 유사한 범위로 확장한다.

- Binance 전용 PAPER 포트폴리오
- Binance public candle 기반 후보 판단
- Binance public ticker 기반 익절/손절
- Binance PAPER 이력 조회
- Binance PAPER 현재가 기준 평가액, 미실현손익, 총손익 조회
- 웹 `/mvp2` Binance 화면에 상태/후보/평가 포지션/이력 표시
- 실제 Binance 주문 API 없음

6. 전략 profile 3종 설정 분리
   - `STABLE`
   - `AGGRESSIVE`
   - `DEFENSIVE`
   - 같은 market data로 각 profile이 독립 신호를 내도록 구성

7. Profile별 PAPER portfolio 분리
   - profile별 현금, 포지션, 실현/미실현 손익 분리
   - MVP1 portfolio와 섞지 않음

8. Profile 동시 실행 scheduler 추가
   - Upbit 기준으로 먼저 3개 profile 동시 실행
   - 이후 Binance로 확장
   - fixedDelay 사용
   - 중복 실행 방지

9. MVP2 실험 대시보드 1차
   - Upbit / Binance 선택 버튼
   - profile별 손익 카드
   - profile별 BUY/SELL/HOLD count
   - profile별 최근 history

10. Analytics와 Leaderboard 추가
    - 수익률
    - 승률
    - 최대 낙폭
    - 손실 빈도
    - 거래 수
    - profile별 순위

11. 선물 long/short simulation 추가
    - `FUTURES_LONG_SIM`
    - `FUTURES_SHORT_SIM`
    - 수수료, 슬리피지, 레버리지 가정값
    - 실제 Binance futures order API 없음

12. Strategy Lab 화면 추가
    - 실험 목록
    - 실험 상세
    - profile 설정 확인
    - Leaderboard와 반복 손실 market 확인

13. 자동 개선 후보 문서화
    - 수익률만 기준으로 자동 변경하지 않음
    - 손실 빈도, 최대 낙폭, 거래 수를 함께 기준으로 사용
    - 변경 전후 운용 시간과 손익을 `docs/trading/condition-records/`에 기록

## 우선순위

MVP2를 바로 시작한다면 첫 작업은 `1. MVP2 패키지와 용어 경계 만들기`다.

다만 MVP1 자금 활용률을 먼저 올릴 경우에는 `docs/project/PROJECT_NEXT_STEPS.md`의 자금 활용률과 포지션 분산 개선을 먼저 끝낸 뒤 MVP2로 넘어간다.

추천 순서:

1. MVP1 자금 활용률과 포지션 분산 개선
2. MVP2 패키지와 용어 경계 만들기
3. Exchange 공통 모델 만들기
4. Binance public market data 추가
5. 거래소별 상태 API와 웹 버튼
6. 전략 profile 3종 동시 실행
7. MVP2 실험 대시보드
8. Leaderboard
9. 선물 simulation

### 0단계: MVP2 경계 만들기

목표:

- MVP1 코드와 MVP2 실험 코드를 섞지 않는다.
- 실제 주문 금지 규칙을 유지한다.

작업:

- `docs/project/MVP2_PLAN.md` 유지
- MVP2 패키지 경계 초안 작성
- exchange, experiment, simulation 용어를 문서화

완료 기준:

- MVP1 자동매매가 기존대로 동작한다.
- MVP2 문서와 패키지 경계가 명확하다.

### 1단계: Exchange 추상화

목표:

- Upbit와 Binance 시세 provider를 같은 인터페이스로 다룬다.

작업:

- `Exchange` enum 추가
- exchange별 market symbol 정규화
- Binance public ticker/candle provider 추가
- Upbit provider를 exchange 인터페이스에 맞게 adapter화
- 실제 인증키 없이 public market data만 사용

완료 기준:

- `UPBIT`, `BINANCE` public ticker/candle 조회가 테스트된다.
- API 실패 시 앱이 죽지 않는다.

진행 상태:

- 완료: exchange 공통 ticker/candle 모델
- 완료: exchange별 symbol 정규화
- 완료: Upbit 기존 ticker/candle provider adapter
- 완료: Binance public ticker/candle provider
- 완료: 거래소별 상태 API
- 완료: React MVP2 Upbit/Binance 선택 UI
- 완료: Binance PAPER 포트폴리오/후보/이력/스케줄러 골격
- 완료: Binance PAPER 평가손익 웹 지표 보강
- 완료: Binance PAPER 자동 실행 운영 기본값 켜짐과 심볼별 실패 격리
- 완료: React `/mvp2` 단일 화면의 Upbit/Binance 모드 전환 UI

### 2단계: Strategy Profile 3종 동시 실행

목표:

- 안정형, 공격형, 수비형을 같은 market 데이터로 동시에 PAPER 평가한다.

작업:

- `STABLE`, `AGGRESSIVE`, `DEFENSIVE` profile 추가
- profile별 진입/청산/risk 설정 분리
- profile별 독립 paper portfolio 생성
- scheduler가 profile별로 같은 market을 평가

완료 기준:

- 같은 market에 대해 3개 profile의 결과가 따로 저장된다.
- history에 strategyProfile이 남는다.

### 3단계: MVP2 실험 화면

목표:

- 웹에서 거래소별, profile별 실험 결과를 본다.

작업:

- Upbit / Binance 선택 버튼
- profile별 손익 카드
- profile별 BUY/SELL/HOLD count
- profile별 포트폴리오
- profile별 최근 손실 원인

완료 기준:

- Upbit와 Binance 대시보드를 따로 볼 수 있다.
- 안정형/공격형/수비형 결과를 한 화면에서 비교할 수 있다.

### 4단계: 선물 시뮬레이션 계층

목표:

- 실제 선물 주문 없이 long/short PnL을 시뮬레이션한다.

작업:

- `FUTURES_LONG_SIM`, `FUTURES_SHORT_SIM` marketType 추가
- short position PnL 계산
- 레버리지 설정은 simulation config로만 저장
- 강제청산 위험 지표는 실제 청산이 아니라 risk metric으로 계산
- 수수료와 슬리피지 가정값 추가

완료 기준:

- 선물 long/short simulation 결과가 spot paper와 분리된다.
- 실제 Binance futures order API는 없다.

### 5단계: Leaderboard와 자동 개선 후보

목표:

- 어떤 전략이 가장 좋은지 자동으로 비교한다.

작업:

- 수익률 순위
- 최대 낙폭 순위
- 승률 순위
- 손익비 순위
- 반복 손실 market 제외 후보
- 다음 설정 변경 후보 추천

완료 기준:

- 기간별 최고 성과 profile을 확인할 수 있다.
- 단순 수익률뿐 아니라 위험 지표도 같이 비교한다.

## 안정형/공격형/수비형 초기 기준

초기값은 실험용이며 실제 성과를 보장하지 않는다.

| Profile | 진입 빈도 | 추세 확인 | 손절 | 익절 | market별 노출 | 목표 |
| --- | --- | --- | --- | --- | --- | --- |
| STABLE | 낮음 | 강함 | 좁음 | 보통 | 보통 | 안정 수익 |
| AGGRESSIVE | 높음 | 보통 | 넓음 | 높음 | 높음 | 높은 수익률 탐색 |
| DEFENSIVE | 매우 낮음 | 매우 강함 | 매우 좁음 | 낮음 | 낮음 | 손실 방어 |

## 주요 리스크

- Binance와 Upbit symbol 체계가 다르다.
- 거래소별 캔들, ticker 응답 형식이 다르다.
- 선물 시뮬레이션은 수수료, 펀딩비, 슬리피지, 청산 위험을 단순화할 수 있다.
- 여러 전략을 동시에 돌리면 history와 portfolio 저장 구조가 복잡해진다.
- 수익률만 기준으로 자동 개선하면 과최적화 위험이 있다.

## MVP2 완료 기준

- Upbit와 Binance public market data를 모두 조회한다.
- 거래소별 대시보드를 분리해서 볼 수 있다.
- 안정형, 공격형, 수비형 profile을 동시에 PAPER/SIMULATION 실행한다.
- strategyProfile, exchange, marketType별 history와 portfolio가 분리된다.
- Leaderboard에서 수익률, 승률, 최대 낙폭, 손실 빈도를 비교한다.
- 실제 주문 API와 `REAL_TRADING`은 여전히 없다.
