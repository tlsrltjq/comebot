# Project Next Steps

## 현재 우선순위: MVP2 시작

MVP1의 최소 리스크 제한은 보류하고 MVP2를 시작한다.

완료:

- `com.giseop.comebot.mvp2` 패키지 경계 생성
- `Exchange`, `MarketType`, `StrategyProfile`, `ExperimentStatus` 용어 추가
- MVP2도 public market data와 PAPER/SIMULATION만 사용한다는 테스트 추가
- Exchange 공통 ticker/candle 모델 추가
- 거래소별 symbol 정규화 추가
- Upbit 기존 provider를 MVP2 exchange adapter로 연결
- Binance public ticker/candle provider 추가
- Binance API 실패 예외 처리와 테스트 추가
- 거래소별 상태 API 추가
- React `/mvp2` 화면에 Upbit/Binance 선택 버튼 추가
- Binance 전용 PAPER 포트폴리오, 후보 판단, 익절/손절, 이력 조회 API 추가
- React `/mvp2` Binance 화면에 PAPER 상태/후보/포지션/이력 표시
- Binance PAPER 현재가 기준 평가액, 미실현손익, 총손익 API 추가
- React `/mvp2` Binance 화면에 PAPER 총자산/평가손익 표시
- Binance PAPER scheduler 운영 기본값을 켜고 심볼별 실패를 격리

다음:

- 이후 실험 엔진 저장 모델로 `exchange`, `marketType`, `strategyProfile`을 분리한다.

완료 기준:

- MVP1 자동매매 흐름은 그대로 유지된다.
- MVP2 코드는 MVP1 portfolio/history와 섞이지 않는다.
- 실제 주문 API와 `REAL_TRADING`은 계속 금지된다.
- Upbit 기존 provider를 MVP2 공통 모델로 조회할 수 있다.
- Binance public ticker/candle을 MVP2 공통 모델로 조회할 수 있다.
- 웹에서 Upbit/Binance 대시보드 상태를 분리해서 볼 수 있다.
- Binance도 public 시세 기반 PAPER 후보, 포트폴리오, 이력을 조회할 수 있다.
- Binance도 public 현재가 기준 평가액과 손익을 조회할 수 있다.
- Binance PAPER scheduler가 기본 켜짐으로 동작하고, 한 심볼 실패가 다음 심볼 실행을 막지 않는다.

## 1단계: 자금 활용률과 포지션 분산 개선

목표:

- 1,000,000 KRW PAPER 현금을 더 적극적으로 사용한다.
- 같은 market 보유 중 추가 진입은 기본 허용한다.
- 특정 코인에 과도하게 몰리지 않도록 포지션 분산과 최대 노출만 관리한다.
- History 분석 화면에서 현금 미사용 비율과 반복 HOLD 사유를 근거로 매수 기회를 늘린다.

작업:

- `STRATEGY_ENTRY_PREVENT_REENTRY_WITH_POSITION=false`를 기본값으로 둔다.
- 완료: 현금 사용률, 포지션 비중, 남은 매수 가능 횟수를 portfolio 화면에서 확인한다.
- 완료: market별 평가액 비중과 쏠림 TOP을 portfolio 화면에서 확인한다.
- 다음: 특정 market 쏠림을 막을 최소 리스크 기준을 문서화한다.
- 필요하면 market별 최대 평가액 또는 최대 추가 매수 횟수만 제한한다.
- 후보 scanner 조건을 너무 보수적으로 막는 HOLD 사유를 확인한다.
- 전략/후보 실행 테스트에 재진입 허용 케이스를 유지한다.
- 리스크 정책 변경이면 `docs/trading/RISK_POLICY.md`를 갱신한다.

완료 기준:

- 같은 market 포지션 보유 중에도 설정 기본값에서 추가 매수가 가능하다.
- PAPER 현금 사용률을 웹에서 판단할 수 있다.
- market별 평가액 비중과 TOP exposure를 웹에서 판단할 수 있다.
- 특정 market 쏠림을 막을 최소 리스크 기준이 문서화된다.
- `./gradlew test`가 성공한다.

## 2단계: 포트폴리오 모바일 카드형 UX

목표:

- 작은 화면에서 포지션 표가 가로 스크롤에만 의존하지 않게 한다.
- 보유 market, 평가액, 손익률, 익절/손절 근접 상태를 카드로 빠르게 확인한다.

작업:

- 모바일 viewport에서 포지션을 카드형 리스트로 표시한다.
- desktop 표는 유지한다.
- 긴 market명과 큰 KRW 숫자가 겹치지 않도록 고정 폭과 줄바꿈을 정리한다.
- 포트폴리오 화면 테스트를 모바일 표시 기준으로 보강한다.

완료 기준:

- 모바일 폭에서도 텍스트가 겹치지 않는다.
- `npm run lint`, `npm run build`, `npm test`가 성공한다.

## 3단계: 후보 화면 개선

목표:

- 전체 KRW 후보 중 어떤 market이 선택되고 제외되는지 빠르게 본다.
- 후보 제외 사유를 전략 개선 근거로 쓴다.

작업:

- SELECTED / SKIPPED 요약을 추가한다.
- 주요 제외 사유 TOP 5를 추가한다.
- 선택 후보만 보기 필터를 추가한다.
- 보유 포지션 여부를 표시한다.
- 포지션 보유 여부와 market별 노출 비중 표시를 준비한다.

완료 기준:

- 후보 화면에서 선택 후보와 제외 사유를 바로 확인할 수 있다.
- 수동 후보 실행 버튼은 만들지 않는다.

## 4단계: 운영 상태 화면과 Telegram 용어 정리

목표:

- status API, 웹, Telegram 상태 메시지의 항목 이름을 맞춘다.
- 설정 변경 기능은 만들지 않는다.
- 웹과 Telegram 모두 자동 실행 상태, 매매 조건, 손익을 같은 용어로 보여준다.
- Telegram은 기본 조회 전용으로 유지하고 수동 PAPER 실행은 명시 설정이 켜진 경우에만 허용한다.

완료 기준:

- 웹 API client에 수동 실행 함수가 없다.
- 웹 린터가 실행 endpoint 추가를 차단한다.
- Telegram menu에 실행 버튼이 없다.
- `/auto`, `/conditions`, `/pnl`로 자동 실행 상태와 손익을 확인할 수 있다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
