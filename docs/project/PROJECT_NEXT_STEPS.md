# Project Next Steps

## 현재 우선순위: 거래소 모드와 포트폴리오 운영 UX 계획

목표:

- `UPBIT / BINANCE` 모드 버튼으로 같은 화면 구조에서 데이터 소스만 전환한다.
- 사용자가 웹 포트폴리오에서 명시 선택한 보유 PAPER 포지션만 수동 매도할 수 있게 설계한다.
- WebSocket 시세 수신과 REST fallback을 거래소별로 분리한다.
- 포트폴리오 원형 그래프와 BTC 등락률 그래프를 추가할 설계를 준비한다.

작업:

- 완료: Stage 1 문서 제약 정리와 계획 문서화
- 완료: Stage 2 ExchangeMode 백엔드 도메인/API 골격 구현
- 완료: Stage 3 사이드바 ExchangeMode 버튼과 프론트 API 파라미터 구현
- 완료: Stage 4 Binance REST market/candle provider 추가
- 완료: Stage 5 Exchange별 PAPER 포트폴리오와 history 분리
- 완료: Stage 6 선택 포지션 PAPER 수동 매도
- 완료: Stage 7 포트폴리오 원형 그래프 UI
- 완료: Stage 8 WebSocket 시세 수신 snapshot store와 REST fallback provider
- 완료: Stage 9 자동매매 주기 분리와 설정 정리
- 완료: Stage 10 BTC 등락률 그래프 페이지
- 준비 완료: Stage 2 ExchangeMode 백엔드 도메인/API 골격 상세 계획
- 준비 완료: Stage 3 사이드바 ExchangeMode 버튼과 프론트 API 파라미터 상세 계획
- 준비 완료: Stage 4 Binance REST market/candle provider 상세 계획
- 준비 완료: Stage 5 Exchange별 PAPER 포트폴리오와 history 분리 상세 계획
- 준비 완료: Stage 6 선택 포지션 PAPER 수동 매도 상세 계획
- 준비 완료: Stage 7 포트폴리오 원형 그래프 UI 상세 계획
- 준비 완료: Stage 8 WebSocket 시세 수신 기반 상세 계획
- 준비 완료: Stage 9 자동매매 주기 분리와 설정 정리 상세 계획
- 준비 완료: Stage 10 BTC 등락률 그래프 페이지 상세 계획
- 계획 완료: Stage 1-10 거래소 대시보드 확장 계획
- 다음 구현: MVP2 계획 후속 정리 또는 운영 안정화 항목 선정

완료 기준:

- 실제 주문 API와 `REAL_TRADING`을 추가하지 않는다.
- 수동 BUY는 계속 금지한다.
- 수동 SELL은 선택된 보유 PAPER 포지션에만 제한한다.
- WebSocket 실패 시 REST fallback 또는 stale snapshot 유지 전략을 가진다.
- 세부 계획은 `docs/project/EXCHANGE_DASHBOARD_UPGRADE_PLAN.md`를 따른다.
- Stage 상세 계획은 `docs/project/exchange-dashboard/` 아래 문서를 따른다.
- 이번 구현의 단계별 상세 파일은 `docs/project/exchange-dashboard/README.md`에서 관리한다.

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
