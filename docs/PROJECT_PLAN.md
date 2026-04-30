# Project Plan

이 문서는 완료된 작업 기록과 앞으로 진행할 작업을 분리해서 관리한다.

작업을 시작하기 전 이 문서에서 다음 단계의 목표, 범위, 금지 사항을 확인한다. 작업이 끝나면 완료 기록에 결과를 남긴다.

## 현재 목표

실제 Upbit 공개 시세를 사용해서 변동성 기반 롱 전용 PAPER_TRADING 전략을 검증한다.

실제 주문 API와 `REAL_TRADING`은 구현하지 않는다.

## 완료된 작업 기록

### 1. 하네스 문서 구조 생성

- `AGENTS.md` 생성
- `docs` 문서 생성
- PAPER_TRADING 기본 원칙 수립

### 2. PAPER_TRADING 주문 실행 골격

- Execution domain/gateway/service 추가
- PaperTradingExecutionGateway 추가
- 주문 요청 검증과 기본 테스트 추가

### 3. 리스크 검증 계층

- RiskValidationService 추가
- 최대 주문 금액과 허용 market 검증
- 리스크 실패 시 ExecutionGateway 미호출

### 4. 주문 후보 생성 계층

- MarketPrice, TradingSignal, TradingStrategy 추가
- SimpleThresholdStrategy 추가
- OrderRequestFactory 추가

### 5. 테스트용 시세 공급자와 트레이딩 플로우

- InMemoryMarketPriceProvider 추가
- TradingFlowService 추가
- 시세 -> 전략 -> 주문 -> 리스크 -> PAPER 실행 연결

### 6. 수동 실행 REST API

- `/api/trading-flow/run`
- Controller는 service 호출과 응답 변환만 담당

### 7. 테스트 가격 변경 API

- `/api/market-prices/{market}`
- InMemory 테스트 가격 조회/변경

### 8. 트레이딩 플로우 history

- InMemory history 저장소
- history 조회 API
- market 필터

### 9. Scheduler

- 기본 비활성 scheduler
- 설정된 market 목록 주기 실행
- history 저장 유지

### 10. 상태 조회 API

- scheduler, notification, telegram, market provider, strategy, risk, system status API 추가

### 11. Notification

- NotificationSender 구조
- Logging/Telegram sender 구조
- 알림 필터 정책
- 기본 비활성

### 12. Telegram outbound/inbound

- Telegram 설정 상태 조회
- 테스트 메시지 API
- getUpdates polling
- 명령어와 inline button
- chatId 제한
- callback answer 처리
- offset 저장소 분리와 JPA 선택 구조

### 13. Upbit 시세 Provider

- Upbit 공개 Ticker API provider
- 기본값은 InMemory 유지
- Access Key, Secret Key 미사용

### 14. PostgreSQL 준비

- docker-compose PostgreSQL
- datasource 설정
- DB status API
- history JPA 저장소 선택 구조
- schema 적용 스크립트

### 15. PAPER 포트폴리오

- PAPER 현금과 포지션 관리
- BUY/SELL 체결 반영
- 실현손익 계산
- 포트폴리오 평가 API

### 16. 익절/손절과 일일 리스크

- position exit policy
- daily order limit
- daily loss limit
- risk status 확장

### 17. Kill Switch

- 신규 트레이딩 플로우 실행 차단
- REST, scheduler, Telegram 실행 경로에 적용
- 조회 API는 허용

### 18. Upbit PAPER_TRADING 실행 환경

- `scripts/run-upbit-paper.bat`
- `.env` 기반 설정
- Upbit 실제 시세 + PAPER_TRADING 검증 가능

### 19. Upbit 캔들 데이터 Provider

- Upbit 공개 Candle API provider 추가
- 최근 분봉 조회 인터페이스 추가
- 캔들 응답 domain 매핑 테스트
- API 실패와 잘못된 요청 검증 테스트
- Access Key, Secret Key 미사용

## 진행 중인 작업

### 변동성 계산 서비스 준비

목표:

- 캔들 Provider 결과를 기반으로 변동성, 거래대금 증가율, 단기 추세 계산을 준비한다.

완료 기준:

- 전략에서 사용할 수 있는 지표 snapshot 설계
- 단위 테스트 기준 확정

## 다음 작업

### 1단계: 변동성 계산 서비스

목표:

- 최근 캔들 기준 변동성, 거래대금 증가율, 단기 추세를 계산한다.

생성/수정 후보:

- `strategy.indicator`
- `VolatilitySnapshot`
- `VolatilityIndicatorService`
- `TrendIndicatorService`

테스트:

- 가격 변동률 계산
- 고가/저가 범위 계산
- 거래대금 증가율 계산
- 상승/하락/횡보 판정

완료 기준:

- 전략이 사용할 수 있는 지표 snapshot 생성

### 2단계: 롱 후보 스캐너

목표:

- 허용 market 목록을 순회하며 매수 후보를 만든다.
- 후보 생성은 주문 실행과 분리한다.

생성/수정 후보:

- `strategy.candidate`
- `TradingCandidate`
- `CandidateScannerService`
- 후보 조회 REST API
- Telegram 후보 조회 명령

테스트:

- 상승 후보 생성
- 과열 후보 제외
- 거래대금 부족 후보 제외
- 허용 market만 scan

완료 기준:

- 주문 없이 후보 목록만 확인 가능

### 3단계: 롱 전용 진입 전략

목표:

- SimpleThresholdStrategy와 분리된 실제 PAPER 전략을 만든다.

생성/수정 후보:

- `VolatilityBreakoutLongStrategy`
- 마켓별 전략 설정
- 전략 선택 설정

테스트:

- BUY 신호 생성
- HOLD 유지
- 과열 회피
- 리스크 조건 연계

완료 기준:

- 실제 시세 기반 BUY 후보가 PAPER 주문으로 연결 가능

### 4단계: 자동 PAPER 운영

목표:

- scheduler로 후보 scan과 PAPER 실행을 자동화한다.
- 기본값은 계속 비활성이다.

테스트:

- scheduler disabled 기본값
- enabled일 때 market scan
- kill switch 차단
- history와 portfolio 저장

완료 기준:

- 실제 시세 기반 자동 PAPER_TRADING 가능

### 5단계: Telegram 운영 UX 개선

목표:

- 후보 목록, 포트폴리오, 리스크 상태를 Telegram에서 쉽게 확인한다.

후보 기능:

- `/candidates`
- 후보 보기 버튼
- 후보별 수동 PAPER 실행 버튼

완료 기준:

- Telegram에서 후보 확인과 PAPER 실행 가능

## 보류 작업

### 실제 주문 API

보류 사유:

- 현재 목표는 PAPER_TRADING 검증이다.
- 전략과 리스크가 충분히 검증되지 않았다.
- 실제 주문 API는 손실 위험이 크다.

실제 주문 API 검토 전 필수 조건:

- 최소 며칠 이상의 PAPER_TRADING history
- 포트폴리오 손익 검증
- kill switch 검증
- Telegram 장애 처리 검증
- 주문 Gateway 설계 문서
- 별도 승인 절차
