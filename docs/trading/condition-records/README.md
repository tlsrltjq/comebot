# Trading Condition Records

이 폴더는 자동 PAPER_TRADING 매매 조건과 조건 변경 후 운용 결과를 함께 기록한다.

실제 주문 API와 `REAL_TRADING`은 포함하지 않는다. 모든 결과는 로컬 `PAPER_TRADING` 검증 기준이다.

## 현재 매매 조건

- 거래 모드: `PAPER_TRADING`
- 전략: `VolatilityBreakoutLongStrategy`
- 대상 market: `ALL_KRW`
- 후보 범위: 전체 KRW 중 24시간 거래대금 상위 50개
- 현재가 수집: Upbit 공개 KRW ticker, 1초 fixedDelay
- 자동 실행 주기: 30초 fixedDelay
- 후보 자동 실행 주기: 30초 fixedDelay
- 1회 매수 금액: 5,000 KRW
- 매수 방향: 롱 전용 BUY
- 매도 방향: 보유 PAPER 포지션에 대한 익절/손절 SELL
- 중복 진입: 같은 market PAPER 포지션 보유 시 재진입 차단

## BUY 조건

아래 조건을 모두 통과한 후보만 PAPER BUY로 연결한다.

- 단기 추세가 `UP`
- 최근 캔들 수: 5
- 가격 변화율이 0.3 이상
- 거래대금 변화율이 20 이상
- 가격 변화율이 10 이하
- 고가/저가 범위가 20 이하
- kill switch가 꺼져 있음
- 허용 market 조건 통과
- 최대 주문 금액과 PAPER 현금 조건 통과
- 같은 market의 기존 PAPER 포지션 없음

## HOLD 조건

아래 중 하나라도 해당하면 주문을 만들지 않고 HOLD로 기록한다.

- 추세가 `UP`이 아님
- 가격 변화율이 기준보다 낮음
- 거래대금 변화율이 기준보다 낮음
- 가격 변화율이 과열 기준을 초과함
- 고가/저가 범위가 과열 기준을 초과함
- 이미 같은 market PAPER 포지션이 있음
- 리스크 검증 또는 safety 검증에서 차단됨

## SELL 조건

SELL은 보유 PAPER 포지션에 대해서만 만든다.

- 익절: 미실현 수익률이 1.5 이상
- 손절: 미실현 수익률이 -0.7 이하
- 보유 수량을 초과하는 SELL은 금지

## 변경 기록

- [2026-05-04 PAPER run](./2026-05-04-paper-run.md)

## 손실 분석 규칙

PAPER run의 총 손익이 마이너스이면 해당 run 문서에 아래 내용을 추가한다.

- 손절 SELL 개수와 익절 SELL 개수
- 평균 손절률과 평균 익절률
- 손실이 컸던 SELL 목록
- 손실 원인 가설
- 다음 조건 변경 때 검증할 개선 기준
