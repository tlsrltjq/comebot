# Architecture Decision Records (ADR)

기술 결정 이유를 기록한다.
에이전트가 "왜 이렇게 구현했지?"를 파악하고 다른 방향으로 리팩토링하는 것을 방지한다.

---

## ADR-001: PAPER_TRADING 전용 — 실제 주문 없음

- **결정**: `REAL_TRADING` 구현과 실제 거래소 주문 API 호출을 영구 금지한다.
- **이유**: 전략 검증 목적 봇이며 실제 자산 손실 위험을 제거하는 것이 최우선.
  Upbit/Binance 공개 시세는 사용하지만 인증키(Access Key, Secret Key)는 사용하지 않는다.
- **트레이드오프**: 실제 수익을 올릴 수 없다. PAPER 체결가와 실제 시장가 슬리피지를 반영하지 못한다.
- **재검토 조건**: 이 결정은 재검토하지 않는다. 실제 주문이 필요하면 별도 프로젝트로 분리한다.

---

## ADR-002: Upbit(KRW) + Binance(USDT) 이중 거래소, 포트폴리오 분리

- **결정**: `ExchangeMode.UPBIT`과 `ExchangeMode.BINANCE`를 분리한다.
  포트폴리오, 포지션, history, analytics는 거래소별로 독립 저장한다.
- **이유**: KRW와 USDT는 환산 없이 비교할 수 없다. 거래소별 슬리피지, 유니버스, 전략 결과가 다르므로 섞으면 손익 분석이 의미 없어진다.
- **트레이드오프**: 구현 복잡도 증가. 같은 market이 두 거래소에 있을 때 중복 관리.
- **재검토 조건**: 세 번째 거래소 추가 시 공통 추상화 도입 여부 검토.

---

## ADR-003: WebSocket + REST fallback — SNAPSHOT provider

- **결정**: `MarketPriceProviderType.SNAPSHOT` 설정 시 WebSocket snapshot을 우선 사용하고, stale/missing이면 거래소별 REST provider로 fallback한다.
  기본 설정은 WebSocket disabled(REST only).
- **이유**: Upbit/Binance WebSocket은 공개이며 낮은 지연으로 시세를 수신한다. REST fallback은 WebSocket 장애 시 주문 차단 없이 운영을 유지하기 위한 안전망.
- **트레이드오프**: SNAPSHOT provider는 fresh snapshot이 0개이면 candidate/exit scheduler 실행을 차단한다. REST-only 설정보다 초기 설정 복잡도가 높다.
- **재검토 조건**: Upbit/Binance가 WebSocket 요금 정책을 변경하거나, REST-only 운영에서 rate limit 문제가 반복되면 재검토.

---

## ADR-004: JPA + InMemory 이중 저장소, 환경변수로 선택

- **결정**: history, portfolio, Telegram offset 저장소를 `HISTORY_STORAGE_TYPE`, `PAPER_PORTFOLIO_STORAGE_TYPE`, `TELEGRAM_INBOUND_OFFSET_STORAGE_TYPE` 환경변수로 JPA/InMemory 중 선택한다.
  기본값은 InMemory(재시작 시 초기화), JPA는 PostgreSQL 필요.
- **이유**: 단기 smoke 실행은 DB 없이 InMemory로 간단하게, 장기 PAPER 운용은 JPA로 재시작 후에도 데이터 유지.
- **트레이드오프**: 런타임 분기로 인해 코드 경로 두 배. 저장소 타입이 다른 상태로 두 인스턴스를 동시에 올리면 데이터 불일치.
- **재검토 조건**: Redis 같은 외부 캐시 도입이 필요한 규모가 되면 재검토.

---

## ADR-005: React + Vite 프론트엔드, 별도 프로세스

- **결정**: 웹 UI를 `frontend/` 하위 Vite React 앱으로 분리하고, Spring Boot와 별도 프로세스로 실행한다.
  웹은 기존 Spring REST API만 호출하고 비즈니스 로직을 중복 구현하지 않는다.
- **이유**: 프론트와 백엔드 독립 개발, 린트와 테스트 분리. Spring Static Resource로 묶으면 빌드 의존성이 복잡해진다.
- **트레이드오프**: 로컬 실행 시 두 프로세스 필요 (`run-local-dev.sh`로 자동화).
  프론트엔드 빌드 없이는 웹 UI를 사용할 수 없다.
- **재검토 조건**: 팀 규모가 커지고 SSR이 필요해지면 Next.js 전환 검토.

---

## ADR-006: 롱 전용 변동성 돌파 전략 (VolatilityBreakoutLong)

- **결정**: 기본 전략은 `VolatilityBreakoutLongStrategy`다. 숏, 마진, 레버리지는 구현하지 않는다.
  후보 선정 조건: 최근 캔들 가격 변화율, 거래대금 증가율, 추세 UP, 과열 회피.
- **이유**: 변동성이 검증된 코인의 단기 상승 모멘텀을 포착하는 것이 PAPER 환경에서 빠르게 가설을 검증하기 좋다. 숏 전략은 리스크 복잡도가 크게 올라간다.
- **트레이드오프**: 하락장에서 후보가 거의 없어 자동 매수가 드물다. 전략 단순화로 실제 알파 포착 능력이 제한될 수 있다.
- **재검토 조건**: 장기 PAPER 손익비(profitLossRatio)가 1 미만으로 유지되면 전략 조건 조정 검토. 숏은 별도 전략 모듈로만 검토.

---

## ADR-007: 스케줄러 기반 자동화, UI/Telegram 수동 실행 금지

- **결정**: BUY/SELL 자동 실행은 candidate scheduler와 exit scheduler가 전담한다.
  웹 UI에는 수동 BUY, 후보 실행, trading-flow 실행 버튼을 제공하지 않는다.
  Telegram `/run`, `/candidate-run` callback은 코드 레벨에서 실행 서비스를 호출하지 않는다.
  유일한 예외: 웹 포트폴리오에서 사용자가 명시 선택한 보유 PAPER 포지션 SELL.
- **이유**: 수동 실행 경로를 열면 안전 검증(kill switch, risk, portfolio) 우회 가능성이 생긴다. 스케줄러가 단일 진입점이어야 일관성 있는 history와 portfolio를 유지할 수 있다.
- **트레이드오프**: 긴급 매수를 즉시 실행할 수단이 없다. 후보가 나타나도 다음 scheduler 주기(최소 30초)를 기다려야 한다.
- **재검토 조건**: Telegram을 통한 검증된 수동 PAPER BUY가 명확히 필요해지면 `telegram.inbound.manual-paper-execution-enabled=true` 경로를 별도 ADR로 검토.

---

## ADR-008: Kill Switch + Risk Validation 순서

- **결정**: 모든 거래 실행 흐름에서 kill switch → 리스크 검증 → 포트폴리오 검증 순서를 지킨다.
  kill switch가 켜져 있으면 시세 조회 이전에 차단한다.
- **이유**: kill switch가 시세 조회 이후에 있으면 시세 API 호출이 먼저 발생해 장애 상황에서 불필요한 외부 요청이 쌓인다. 리스크 검증 실패 시 ExecutionGateway를 호출하면 안 된다는 원칙을 코드 구조로 강제.
- **트레이드오프**: kill switch 체크 레이어가 여러 서비스에 중복 삽입될 수 있다.
- **재검토 조건**: kill switch 체크 로직을 AOP나 공통 필터로 추출할 필요가 생기면 재검토.
