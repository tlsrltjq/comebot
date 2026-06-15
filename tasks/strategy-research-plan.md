# strategy-research-plan.md — 전략 리서치 실행 계획

## 목적

V1 풀백 반등군의 엣지 없음 결론 이후, 자동매매 봇을 수익 가능한 연구/검증 시스템으로 전환한다.
완료된 TODO는 이 문서에서 삭제하고, 핵심 결정과 결과만 "진행 로그"에 남긴다.

## 불변 원칙

- 거래 모드는 항상 `PAPER_TRADING`이다.
- 실제 Upbit/Binance 주문 API와 `REAL_TRADING`은 구현하지 않는다.
- 수익 신호가 확인되기 전까지 UI/텔레그램/운영 편의 기능 확장은 보류한다.
- 비즈니스 로직 변경은 테스트 없이 진행하지 않는다.
- 전략 채택은 백테스트 수익률이 아니라 비용 전 신호 엣지와 OOS 재현성을 우선한다.

## 기본 게이트

| 구분 | 기준 |
|---|---|
| 1차 생존 | train PFgross >= 1.10 |
| 강한 후보 | train PFgross >= 1.15 |
| 즉시 폐기 | train PFgross < 1.00 |
| 과적합 폐기 | train 양호, test 붕괴 또는 train/test 괴리 과대 |
| 표본 폐기 | 거래 수 과소, 1~2개 코인 의존, 특정 단일 기간 의존 |
| 운영 검토 | PFnet OOS 양호 + MDD/체결 현실성 통과 |

## 확정 결정

| 항목 | 결정 |
|---|---|
| 운영 목표 | 공격적인 수익률 탐색 우선. MDD는 생존/운영 검토 단계에서 방어 지표로 본다. |
| 데이터 확장 | Upbit KRW와 Binance USDT를 동시에 확장한다. |
| 기준봉 | 1m 파생 변환이 아니라 1m/3m/5m/15m 원본 캔들을 별도 수집하고 문서화한다. |
| 전략 순서 | 상대강도 모멘텀 -> 거래대금 추세 지속 -> 변동성 돌파 -> 과매도 평균회귀 순서로 시작한다. |
| 운영 상태 | 새 전략 후보가 나오기 전까지 기존 PAPER 자동매매는 관찰/대시보드 전용으로 멈춘다. 새 전략이 나오면 PAPER 자동매매로 운영 관찰한다. |

## TODO

### 1. 데이터 범위 확장

- [ ] Upbit와 Binance를 분리 평가하고, 이후 통합 리더보드에서 비교한다.
- [ ] 상승/하락/횡보 레짐 라벨링 기준을 만든다.

### 2. 전략 후보군 실험

- [ ] Session Volatility Breakout PAPER 관찰 결과를 보고 강한 코인 선별 후 pullback 진입 하이브리드 전략 후보를 정의한다.

### 3. 전략 리더보드

- [ ] 후보 전략별 파라미터 민감도 요약을 남긴다.
- [ ] 폐기된 전략은 폐기 사유를 짧게 기록한다.

### 4. 체결/비용 현실성 강화

- [ ] 수수료 모델을 백테스트 결과에 명시적으로 반영한다.
- [ ] 슬리피지 가정치를 전략별로 적용 가능하게 만든다.
- [ ] maker 지정가 미체결률과 체결 지연을 보수적으로 모델링한다.
- [ ] 부분 체결 또는 미체결 후 취소 시나리오 처리 방식을 정한다.
- [ ] 저유동성 코인의 호가 공백/가격 충격 방어 기준을 만든다.
- [ ] WebSocket 지연/누락 상황의 운영 차단 조건을 검토한다.

### 5. 운영 방어 정책

- [ ] V1에서 손실/MDD를 낮춘 M1(coin1h==UP) 적용 여부를 결정한다.
- [ ] V1에서 손실/MDD를 낮춘 F1(BTC!=DOWN) 적용 여부를 결정한다.
- [ ] PAPER 운영 최소 관찰 기간과 채택 기준을 정한다.
- [ ] Session Volatility Breakout PAPER 관찰 운영의 market 목록, scheduler ON 범위, 최대 BUY 수를 확정한다.

### 6. 문서와 세션 운영

- [ ] 전략 실험 결과를 `CHANGELOG.md`와 관련 ADR/정책 문서에 남기는 기준을 정한다.
- [ ] 각 논리 단위 완료 시 테스트 명령과 결과를 진행 로그에 남긴다.

## 진행 로그

| 날짜 | 단계 | 결과 |
|---|---|---|
| 2026-06-12 | 계획 수립 | V1 이후 전략 리서치 실행 계획 문서 생성 |
| 2026-06-12 | 결정 반영 | 공격적 수익률 탐색, Upbit+Binance 동시 확장, 1m/3m/5m/15m 원본 수집, 추천 전략 순서, 관찰 전용 운영 정책 확정 |
| 2026-06-12 | 데이터/운영 기반 | 원본 캔들 수집 스크립트와 `docs/trading/BACKTEST_DATA.md` 추가. 기본 실행 경로와 승인된 `.env`의 candidate/exit scheduler를 OFF로 변경 |
| 2026-06-12 | 시드 데이터 수집 | `.backtest_cache`에 BTC/ETH 1년치 1m/3m/5m/15m 원본 캔들 수집. Binance 연속, Upbit 일부 gap 확인 |
| 2026-06-12 | 리더보드 기반 | `BacktestLeaderboardRow/Writer` 추가. CSV/Markdown 공통 컬럼을 `docs/trading/BACKTEST_LEADERBOARD.md`에 고정 |
| 2026-06-12 | 실험 기준 고정 | `BacktestSplitPolicy`로 last-60d 단일 OOS 분할 중앙화, `BacktestDecisionPolicy`로 탈락/후보 판정 기준 코드화 |
| 2026-06-12 | 상대강도 모멘텀 | BTC/ETH 시드 기준 72개 조합 전부 탈락. 생존 후보 0, 주 사유=train gross edge 부족/ETH 편중 |
| 2026-06-12 | 거래대금 추세 지속 | BTC/ETH 시드 기준 216개 조합 전부 탈락. 생존 후보 0, 주 사유=표본 부족/ETH 편중/train gross edge 부족 |
| 2026-06-12 | 변동성 수축 후 돌파 | BTC/ETH 시드 기준 216개 조합 전부 탈락. 생존 후보 0, 주 사유=train gross edge 부족/표본 부족/ETH 편중 |
| 2026-06-12 | 과매도 평균회귀 | BTC/ETH 시드 기준 216개 조합 전부 탈락. 생존 후보 0, 주 사유=OOS 표본 부족/ETH 편중 |
| 2026-06-12 | 유니버스 기준 고정 | 확장 수집은 Upbit KRW 24h 거래대금 상위 30 + Binance spot USDT quote volume 상위 30 고정 유니버스로 비교 |
| 2026-06-12 | BTC/마켓 레짐 모멘텀 | BTC/ETH 시드 기준 432개 조합 전부 탈락. 생존 후보 0, 주 사유=OOS 표본 부족/ETH 편중 |
| 2026-06-12 | 상위 30 수집 시작 | 기존 BTC/ETH 16개 + `KRW-WLD` 4개 완료. `KRW-XRP` 1m 진행 중 중단되어 다음 세션에서 재개 |
| 2026-06-15 | 상위 30 수집 완료 | Upbit 30 + Binance 30, 1m/3m/5m/15m 원본 캔들 240개 JSON 수집 완료. `.backtest_cache` 약 9.0GB |
| 2026-06-15 | 종목 랭킹 로테이션 | 확장 유니버스 288개 조합 산출. Binance 15m rankCount=1/rebalance=20 조합 6개가 약한 PAPER 관찰 후보, Upbit은 train gross edge 부족으로 전부 탈락 |
| 2026-06-15 | 하니스 보정 | Binance 리더보드에 Upbit `KRW-USDT`가 섞이지 않도록 `BacktestSeriesLoader` 필터와 회귀 테스트 추가 |
| 2026-06-15 | 랭킹 로테이션 정밀 검증 | 비용 3종 x 유니버스 4종 총 72개 시나리오. 기본 비용에서는 후보 유지, 보수/스트레스 비용에서는 전부 net weak. PAPER 자동매매 전환 보류 |
| 2026-06-15 | 시간대/세션 변동성 돌파 | 확장 유니버스 640개 조합 산출. `candidate:paper-observation` 6개, `candidate:weak-paper-observation` 7개. Binance 15m UTC 06-12와 Binance 3m UTC 12-18/18-24에 집중 |
| 2026-06-15 | 세션 변동성 정밀 검증 | 후보 13개 x 비용 3종 x 유니버스 4종 총 156개 시나리오. stress 비용에서도 7개 후보 유지. 1순위는 Binance 15m UTC 06-12 breakout=20/avg=60/range=2.5/volume=1.5 |
| 2026-06-15 | 세션 변동성 maker 1차 감사 | 15m coarse maker 모델(신호 close 지정가, 다음 15m 캔들 low 체결) 12개 시나리오 모두 후보 유지. 실제 5분 유효성은 5m/1m 하위 캔들 감사 필요 |
| 2026-06-15 | 세션 변동성 하위 캔들 감사 | 15m 신호 + 5m/1m 300초 유효 maker 감사 통과. no-event/no-pegged stress 후보 유지, no-pegged-event는 train gross 1.10 미만으로 탈락 |
| 2026-06-15 | PAPER 후보 스캐너 구현 | `SESSION_VOLATILITY_BREAKOUT` 선택값과 Binance 15m UTC 06-12 후보 스캐너 추가. 후보 조회/실행/스케줄러 경로는 선택 전략 기준으로 새 스캐너에 위임. `./gradlew test checkstyleMain` 통과 |
| 2026-06-15 | 운영 readiness 경고 | 상태 API에 `candidateReadinessWarnings` 추가. Session Volatility Breakout 관찰 운영 전 Binance/USDT/max BUY/exit scheduler scope를 경고한다. `./gradlew test checkstyleMain` 통과 |
| 2026-06-15 | 관찰 실행 프로필 | Session Volatility Breakout 전용 PAPER JPA 실행 스크립트와 resume 전 readiness 가드 추가. persisted scheduler restore 비활성화로 이전 ON 상태 자동 복원을 방지 |

## 사용자 확인 필요

- 없음.
