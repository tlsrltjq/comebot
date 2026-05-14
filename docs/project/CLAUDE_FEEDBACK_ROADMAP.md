# Claude Feedback Roadmap

이 문서는 클로드 피드백을 바탕으로 앞으로 진행할 작업을 정리한다.
바로 구현할 항목과 데이터가 더 쌓인 뒤 판단할 항목을 분리한다.
실제 작업 순서는 `docs/project/PROJECT_NEXT_STEPS.md`에 반영한다.

## 원칙

- 기본 거래 모드는 계속 `PAPER_TRADING`이다.
- `REAL_TRADING`과 실제 거래소 주문 API는 구현하지 않는다.
- 웹 수동 BUY와 Telegram 수동 실행 경로는 계속 금지한다.
- 웹 선택 PAPER SELL은 보유 PAPER 포지션에 한해서만 유지한다.
- 전략 성과 판단은 감각이 아니라 저장된 PAPER 데이터와 analytics 기준으로 한다.
- 새 DB 저장은 분석 가치가 명확할 때만 추가한다.

## 우선순위

| 순서 | 작업 | 성격 | 이유 |
| --- | --- | --- | --- |
| 1 | 전략 성과 측정 API | 코드/API | 승률, 평균 보유 시간, 손익비 없이는 전략 개선 방향을 판단하기 어렵다. |
| 2 | WebSocket 장애/fallback 검증 | 코드/테스트 | 시세가 불명확한 상태에서 주문이 성공처럼 보이면 안 된다. |
| 3 | PAPER 실행 runtime guard | 코드 안전장치 | 실제 주문 경로가 나중에 실수로 열리는 것을 런타임에서 막는다. |
| 4 | security lint 강화 | 테스트/하네스 | 실제 주문 API, real trading 성격의 코드 추가를 자동으로 감지한다. |
| 5 | Incident Log 문서 추가 | 문서 | 운영 중 장애와 이상 사례를 축적해 재발 방지 근거로 삼는다. |
| 6 | API rate limit 문서화 | 문서 | scheduler 주기와 거래소 공개 API 호출량의 근거를 남긴다. |
| 7 | PAPER 현금 부족 경고 | API/UI | 현금 부족으로 BUY가 조용히 계속 막히는 상태를 빠르게 드러낸다. |
| 8 | 후보 선정 수치 기록 | DB/분석 | BUY 시점의 변동성, 거래대금, 추세를 나중에 역추적한다. |
| 9 | trailing stop | 전략 | 고정 익절보다 추세 지속 구간을 더 오래 따라갈 수 있다. |
| 10 | 시간 기반 청산 | 전략 | 장시간 무성과 포지션의 자본 묶임을 줄인다. |

## 1. 전략 성과 측정 API

목표:

- 현재 PAPER 전략이 잘 동작하는지 운영자가 수치로 판단할 수 있게 한다.
- 감각적 판단 대신 history와 trade log 기반 지표를 제공한다.

지표:

- 승률: 완료된 PAPER 진입/청산 쌍 중 이익 청산 비율
- 평균 보유 시간: BUY 체결부터 대응 SELL 체결까지 평균 시간
- 손익비: 평균 이익 청산 수익률 또는 금액 / 평균 손실 청산 손실률 또는 금액
- 보조 지표: 총 거래 수, 익절 수, 손절 수, 미청산 포지션 수

데이터 후보:

- `paper_trade_log`: FILLED PAPER BUY/SELL 원장
- `trading_flow_history`: signal reason, order status, take profit/stop loss 사유
- `paper_position`: 현재 미청산 포지션

완료 기준:

- API 응답은 exchange와 range를 지원한다.
- realized/unrealized 판단을 섞지 않는다.
- 실제 주문 API, `REAL_TRADING`, 수동 BUY UI를 추가하지 않는다.
- backend service/controller 테스트를 추가한다.

## 2. WebSocket 장애/fallback 검증

목표:

- WebSocket 단절, stale snapshot, REST fallback 실패가 주문 성공으로 보이지 않게 한다.
- 운영자가 시세 공급 상태를 판단할 수 있게 한다.

체크리스트:

- WebSocket 연결은 살아 있지만 메시지가 오지 않는 경우 stale로 판정되는지 확인한다.
- 마지막 수신 시각 기준 stale threshold가 코드와 문서에 명시되어 있는지 확인한다.
- stale snapshot이면 REST fallback으로 전환되는지 확인한다.
- REST fallback 실패 시 history에 `FAILED` 또는 명확한 실패 상태로 남는지 확인한다.
- Upbit/Binance WebSocket이 동시에 불안정할 때 scheduler가 빈 시세를 성공처럼 처리하지 않는지 확인한다.
- `/api/market-provider/status`가 snapshot count와 freshness 판단에 충분한 정보를 주는지 확인한다.

완료 기준:

- 현재가가 불명확한 주문은 성공 처리되지 않는다.
- fallback 실패 경로가 테스트로 고정된다.
- 운영/reliability 문서가 실제 구현과 맞는다.

## 3. PAPER 실행 runtime guard

상태: 완료

목표:

- 실수로 real trading 성격의 실행 경로가 생겨도 런타임에서 즉시 차단한다.

검토 방향:

- 실행 gateway가 PAPER 전용임을 코드에서 명시한다.
- 실제 주문 mode나 real executor 성격의 호출이 들어오면 `UnsupportedOperationException` 또는 명확한 예외를 던진다.
- guard는 PAPER 주문 정상 흐름을 방해하지 않아야 한다.

완료 기준:

- PAPER BUY/SELL 기존 테스트가 유지된다.
- real trading 성격의 실행 시도가 테스트에서 실패한다.

## 4. security lint 강화

상태: 완료

목표:

- 실제 거래소 주문 API와 `REAL_TRADING` 성격의 코드 추가를 자동으로 막는다.

검토 키워드:

- `RealTrading`
- `RealOrder`
- `ExchangeOrderApi`
- `UpbitOrder`
- `BinanceOrder`
- `accessKey`
- `secretKey`

주의:

- 문서에서 금지어를 설명하는 경우까지 실패하면 오탐이 생긴다.
- source/test/resource별 허용 범위를 분리해서 설계한다.

완료 기준:

- 실제 주문 실행 코드 추가 시 테스트가 실패한다.
- 기존 보안 문서와 README는 오탐 없이 유지된다.

## 5. Incident Log

상태: 완료

목표:

- 운영 중 발생한 실제 장애와 이상 사례를 축적한다.
- WebSocket 단절, REST fallback 실패, DB 장애, scheduler 지연, Telegram 오류를 나중에 추적할 수 있게 한다.

문서 위치:

- `docs/operations/INCIDENT_LOG.md`

기록 형식:

- 날짜/시간
- 현상
- 영향
- 원인
- 조치
- 재발 방지

완료 기준:

- 빈 템플릿과 첫 사용 예시를 제공한다.
- 운영 문서에서 Incident Log 위치를 참조한다.

## 6. API rate limit 문서화

상태: 완료

목표:

- candidate/exit/market polling 주기가 거래소 공개 API 제한과 충돌하지 않는지 운영자가 판단할 수 있게 한다.

문서화할 내용:

- candidate scan 1사이클의 REST 호출 수 추정
- exit scheduler 1사이클의 REST 호출 수 추정
- WebSocket snapshot 사용 시 REST fallback 호출 조건
- 429 또는 fallback 실패 시 기대 동작
- 주기 변경 전 확인해야 할 API

완료 기준:

- `docs/operations/RELIABILITY.md` 또는 `docs/operations/OPERATIONS.md`에 기준을 추가한다.
- 실제 scheduler 설정값과 충돌하지 않는다.

## 7. PAPER 현금 부족 경고

상태: 완료

목표:

- PAPER 현금 부족으로 신규 BUY가 계속 막히는 상태를 조용한 장애로 방치하지 않는다.

검토 방향:

- 현금 비율 warning 기준을 설정한다.
- Dashboard 또는 Top Status Bar에 경고를 표시한다.
- Telegram `/status` 또는 `/pnl` 요약은 필요 시 별도 작업으로 검토한다.

완료 기준:

- BUY 실행 로직 자체는 바꾸지 않는다.
- 현금 부족 경고는 read-only 상태 표시로 시작한다.

## 8. 후보 선정 수치 기록

목표:

- BUY 당시 후보로 선정된 근거 수치를 나중에 분석할 수 있게 한다.

후보 설계:

- `paper_trade_log`에 strategy snapshot JSON 컬럼 추가
- 또는 별도 `candidate_scan_log` append-only 테이블 추가

보류 이유:

- 저장할 수치와 조회 방식이 아직 확정되지 않았다.
- 먼저 전략 성과 측정 API로 부족한 분석 지점을 확인한다.

완료 기준:

- 저장량 증가가 통제된다.
- Candidates/History 대용량 방어 기준을 깨지 않는다.

## 9. Exit 전략 고도화

추천 순서:

1. trailing stop
2. 시간 기반 청산
3. 분할 청산

보류 기준:

- 현재 고정 익절/손절 PAPER 데이터가 충분히 쌓이기 전에는 전략 변경을 서두르지 않는다.
- 먼저 승률, 평균 보유 시간, 손익비를 볼 수 있어야 한다.

완료 기준:

- exit 정책 변경 시 `docs/trading/RISK_POLICY.md`와 `docs/trading/STRATEGY_POLICY.md`를 함께 수정한다.
- 익절/손절 흐름 테스트를 추가하거나 수정한다.

## 10. 장기 보류

- 전략 plugin 구조
- 분할 청산
- 실제 주문 API
- `REAL_TRADING`
- 수동 BUY

전략 plugin 구조는 현재 `TradingStrategy` 경계가 유지되는 동안 보류한다.
2~3개월 PAPER 데이터가 쌓이고 병행 전략의 구체적 필요가 생기면 다시 검토한다.
