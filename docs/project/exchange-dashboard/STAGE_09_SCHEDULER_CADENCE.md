# Stage 9: Scheduler Cadence Separation

## 목표

후보 탐색 주기와 보유 포지션 exit 평가 주기를 분리하고, 너무 잦은 실행으로 인한 중복 주문과 history 과다 적재를 줄인다.

현재는 후보 자동 실행 scheduler와 trading-flow scheduler가 분리되어 있지만, 보유 포지션의 익절/손절 SELL 평가만 빠르게 돌리는 전용 exit scheduler는 없다. Stage 9에서는 신규 진입과 보유 포지션 청산 평가를 다른 주기로 운영할 수 있게 만든다.

## 포함 기능

- 후보 탐색 scheduler와 exit 평가 scheduler 분리
- 권장 주기 설정
  - 후보 탐색: 30초에서 60초
  - 보유 포지션 exit 평가: 5초에서 10초
- market별 실행 간격 유지
- 반복 HOLD history 적재 제한 검토
- scheduler 상태 API와 웹 표시 정리
- scheduler별 중복 실행 방지
- scheduler별 거래소 모드 반영
- scheduler별 enable/disable 설정
- exit scheduler는 보유 포지션 market만 평가
- candidate scheduler는 신규 진입 후보 탐색에 집중
- 기존 `fixedDelay` 유지

## 제외 기능

- 실제 주문 API
- 실시간 고빈도 트레이딩
- WebSocket 구현 자체
- strategy 조건 대규모 변경
- 익절/손절 기준 변경
- 선물, 숏, 레버리지
- 외부 queue 도입
- scheduler가 직접 주문/리스크/포트폴리오 로직을 구현하는 구조

## 현재 구조 요약

- `ScheduledCandidateExecutionRunner`
  - `trading.candidate-scheduler.fixed-delay-ms`
  - 후보 market 목록을 순회하며 `CandidateExecutionService.execute(market)` 호출
  - market별 실패는 다음 market 실행을 막지 않음
  - summary notification 지원
- `ScheduledTradingFlowRunner`
  - `trading.scheduler.fixed-delay-ms`
  - market 목록을 `TradingFlowService.runAll(markets)`로 실행
  - `TradingFlowService` 안에서 strategy HOLD 후 보유 포지션 exit 신호를 평가
- 문제점
  - 신규 BUY 후보 탐색과 보유 포지션 SELL 평가가 같은 흐름에 섞일 수 있다.
  - 손절 기준 도달 후 다음 평가까지 지연되어 실제 손절률이 커질 수 있다.
  - 전체 market을 자주 돌리면 HOLD history가 과다 적재될 수 있다.

## 목표 주기

권장 기본값:

| Scheduler | 목적 | 권장 fixedDelay | 대상 |
| --- | --- | --- | --- |
| Candidate scheduler | 신규 진입 후보 탐색과 PAPER BUY | `30000`-`60000` ms | 후보/허용 market |
| Exit scheduler | 보유 포지션 익절/손절 PAPER SELL | `5000`-`10000` ms | 보유 position market |
| Market ticker feed | 시세 snapshot 갱신 | WebSocket 지속 수신 | 거래소별 전체 또는 관심 market |
| REST fallback polling | WebSocket 장애 보조 | 장애/bootstrapping 중심 | 거래소별 |

Stage 9 구현 기본값 후보:

```properties
trading.candidate-scheduler.fixed-delay-ms=60000
trading.exit-scheduler.enabled=true
trading.exit-scheduler.fixed-delay-ms=5000
trading.exit-scheduler.per-market-delay-ms=0
trading.exit-scheduler.save-hold-history=false
```

## Exit Scheduler 설계

- 보유 PAPER position 목록을 조회한다.
- position이 없으면 실행하지 않는다.
- 보유 market의 현재가를 batch로 조회한다.
- `PositionExitSignalService`로 익절/손절 SELL 신호만 평가한다.
- SELL 신호가 있으면 기존 `OrderRequestFactory`, `OrderExecutionService`, `TradingFlowHistoryService` 흐름을 재사용한다.
- HOLD는 기본적으로 history에 저장하지 않는다.
- 단, debugging 설정이 켜진 경우에만 HOLD history를 저장할 수 있다.
- kill switch가 켜져 있으면 시세 조회 전에 차단하고, 필요 시 market별 REJECTED history를 남긴다.

## Candidate Scheduler 설계

- 신규 진입 후보 탐색과 PAPER BUY 실행을 담당한다.
- 보유 포지션 exit 평가를 주 목적으로 사용하지 않는다.
- candidate scheduler 주기는 exit scheduler보다 길게 둔다.
- `ALL_KRW` 또는 거래소별 후보 universe를 사용한다.
- market별 실패는 summary에 남기고 다음 market을 계속 처리한다.
- 중복 매수 방지는 Stage 5 이후 exchange-aware risk/portfolio 기준을 따른다.

## HOLD History 적재 정책

- exit scheduler HOLD는 기본 저장하지 않는다.
- candidate scheduler의 HOLD history는 기존 analytics 요구를 고려해 유지하되, 과다 적재 제한을 둔다.
- 제한 후보:
  - 동일 market/동일 reason HOLD는 N분 이내 재저장 금지
  - scheduler summary에는 HOLD count를 남기되 개별 history는 sampling
  - BUY/SELL/REJECTED/FAILED는 항상 저장
- Stage 9 1차 권장안:
  - exit scheduler HOLD 저장 안 함
  - candidate scheduler HOLD는 기존 유지
  - 이후 history 증가량을 보고 candidate HOLD dedupe stage를 별도로 검토

## 변경 후보

| 파일 | 변경 이유 |
| --- | --- |
| `src/main/java/com/giseop/comebot/scheduler/ScheduledCandidateExecutionRunner.java` | 후보 탐색 주기 |
| `src/main/java/com/giseop/comebot/scheduler/ScheduledTradingFlowRunner.java` | exit 평가 주기 |
| `src/main/java/com/giseop/comebot/scheduler/...Properties.java` | 설정 분리 |
| `src/main/java/com/giseop/comebot/scheduler/ScheduledPositionExitRunner.java` | 보유 포지션 exit 전용 scheduler |
| `src/main/java/com/giseop/comebot/scheduler/PositionExitSchedulerProperties.java` | exit scheduler 설정 |
| `src/main/java/com/giseop/comebot/trading/service/PositionExitExecutionService.java` | 보유 포지션 SELL 평가/실행 use case |
| `src/main/java/com/giseop/comebot/portfolio/service/PaperPortfolioService.java` | 보유 position market 조회 |
| `src/main/java/com/giseop/comebot/risk/service/PositionExitSignalService.java` | 익절/손절 평가 재사용 |
| `src/main/java/com/giseop/comebot/history/service/TradingFlowHistoryService.java` | HOLD history 저장 정책 적용 후보 |
| `src/main/java/com/giseop/comebot/scheduler/controller/SchedulerStatusController.java` | 상태 조회 |
| `src/main/java/com/giseop/comebot/system/dto/SystemStatusResponse.java` | exit scheduler 상태 포함 |
| `frontend/src/features/trading/TradePage.tsx` | 주기 표시 |
| `frontend/src/shared/api/types.ts` | scheduler status 타입 확장 |
| `docs/trading/RISK_POLICY.md` | 중복 주문과 주기 정책 |
| `docs/operations/RELIABILITY.md` | scheduler 중복 실행 방지 |

## API/화면 표시 계획

상태 API에는 scheduler를 세 구분으로 보여준다.

- Candidate scheduler
  - enabled
  - fixedDelayMs
  - market count
  - notifySummary
- Trading scheduler 또는 legacy scheduler
  - enabled
  - fixedDelayMs
  - markets
- Exit scheduler
  - enabled
  - fixedDelayMs
  - saveHoldHistory
  - position market count

웹 `자동 실행(Auto Run)` 화면은 신규 매수와 청산 평가 주기를 분리해서 표시한다.

## 중복 실행 방지

- 모든 scheduler는 `fixedDelay`를 유지한다.
- scheduler 내부에도 `AtomicBoolean running` 또는 동등한 guard를 둔다.
- 이전 실행이 끝나지 않았으면 다음 실행은 skip하고 debug/info log를 남긴다.
- exit scheduler와 candidate scheduler가 같은 market에서 동시에 SELL/BUY를 만들 수 있으므로 portfolio/risk 검증이 최종 방어선이어야 한다.
- Stage 9에서 cross-scheduler global lock은 기본 도입하지 않는다. 필요하면 market-level lock을 별도 stage로 검토한다.

## 구현 순서

1. `PositionExitSchedulerProperties` 추가
2. `PositionExitExecutionService` 설계
3. `ScheduledPositionExitRunner` 추가
4. HOLD history 저장 정책 적용
5. scheduler status API 확장
6. 웹 Auto Run 화면 표시 확장
7. reliability/risk 문서 업데이트
8. 기존 scheduler 테스트 보강

## 테스트 계획

- 후보 scheduler와 exit scheduler가 별도 설정을 사용
- `fixedDelay` 사용 유지
- 이전 실행 중이면 중복 실행 방지
- HOLD history 과다 적재 방지 정책 테스트
- position이 없으면 exit scheduler가 시세 조회/주문 실행을 하지 않음
- 익절 조건이면 SELL 주문 생성
- 손절 조건이면 SELL 주문 생성
- HOLD면 기본적으로 history 저장하지 않음
- kill switch enabled면 exit scheduler 차단
- market 하나 실패가 다른 position 평가를 막지 않음
- scheduler status API가 candidate/exit 주기를 모두 반환
- 웹 Auto Run 화면이 candidate/exit 주기를 구분해 표시
- `./gradlew test`
- frontend 변경 시 `npm run lint`, `npm run build`, `npm test`

## 완료 기준

- 후보 탐색과 exit 평가 주기를 따로 조정할 수 있다.
- 중복 매수/매도 위험이 늘지 않는다.
- history 적재량이 통제된다.
- 손절/익절 지연을 줄이기 위해 보유 포지션은 더 짧은 주기로 평가된다.
- `fixedDelay`와 중복 실행 guard가 유지된다.
- 실제 주문 API, 수동 BUY, `REAL_TRADING`은 추가되지 않는다.

## 리스크

- exit scheduler 주기를 너무 짧게 잡으면 API 호출량과 DB/history 부담이 커질 수 있다.
- candidate scheduler와 exit scheduler가 동시에 같은 portfolio를 변경하려 할 수 있다.
- HOLD history를 완전히 제거하면 analytics에서 HOLD trend가 줄어들 수 있다.
- exit scheduler가 전체 market을 돌면 후보 scheduler와 다를 바 없어지므로 반드시 보유 position market만 대상으로 해야 한다.
- WebSocket이 없는 상태에서 exit 주기를 5초로 낮추면 REST 호출량이 늘 수 있다.

## 사용자 확인 필요

- 없음. Stage 9 1차 권장안은 exit scheduler HOLD history 저장 안 함, candidate scheduler HOLD history 기존 유지로 진행한다.

## 구현 결과

- `PositionExitSchedulerProperties`를 추가해 `trading.exit-scheduler.*` 설정을 분리했다.
- `PositionExitExecutionService`를 추가해 보유 PAPER position market만 익절/손절 SELL 평가 대상으로 삼는다.
- `ScheduledPositionExitRunner`를 추가했고 `@Scheduled(fixedDelayString = "${trading.exit-scheduler.fixed-delay-ms:5000}")`와 `AtomicBoolean` guard를 사용한다.
- candidate scheduler와 legacy trading scheduler에도 내부 중복 실행 guard를 추가했다.
- exit scheduler는 HOLD를 기본적으로 history에 저장하지 않고, `save-hold-history=true`일 때만 저장한다.
- `/api/scheduler/status`와 `/api/system/status`에 exit scheduler 상태, 주기, HOLD 저장 여부, 대상 거래소, 보유 market 수를 추가했다.
- 웹 자동 실행 화면과 운영 대시보드에 후보/청산 주기를 분리해서 표시했다.
- 기본 설정은 신규 진입 candidate scheduler 60초, 보유 position exit scheduler 5초, legacy trading scheduler disabled로 정리했다.

검증:

```text
./gradlew test --tests PositionExitExecutionServiceTest --tests ScheduledPositionExitRunnerTest --tests SchedulerStatusControllerTest --tests SystemStatusControllerTest --tests PositionExitSignalServiceTest --tests ScheduledCandidateExecutionRunnerTest --tests ScheduledTradingFlowRunnerTest
```
