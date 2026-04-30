# AGENTS.md

이 프로젝트는 Upbit 공개 시세를 기반으로 코인 변동성을 추적하고, 롱 전용 매수 후보를 판단한 뒤 `PAPER_TRADING`으로 검증하는 `comebot` 프로젝트다.
기본 패키지는 `com.giseop.comebot`이다.

## 핵심 규칙

- 기본 거래 모드는 항상 `PAPER_TRADING`이다.
- `REAL_TRADING`은 구현하지 않는다.
- 실제 주문 API는 아직 구현하지 않는다.
- Upbit Access Key, Secret Key, API Key, Bot Token, Chat ID는 코드에 하드코딩하지 않는다.
- 민감 정보는 `.env` 또는 환경 변수로만 관리한다.
- 현재 전략 방향은 롱 전용이다. 숏, 레버리지, 마진 거래는 다루지 않는다.
- 매수 판단은 변동성, 추세, 거래대금, 리스크 조건을 분리해서 검증한다.
- 익절/손절 조건은 주문 실행 전후 흐름과 함께 테스트해야 한다.
- 매수/매도 판단 로직을 수정하면 테스트를 추가하거나 수정해야 한다.
- 주문 상태 흐름을 바꾸면 `docs/ORDER_LIFECYCLE.md`도 함께 수정해야 한다.
- 리스크 정책을 바꾸면 `docs/RISK_POLICY.md`도 함께 수정해야 한다.
- 텔레그램 명령이나 버튼 흐름을 바꾸면 `docs/TELEGRAM_UX.md`도 함께 수정해야 한다.
- 실패한 주문을 성공으로 처리하면 안 된다.
- 예외를 무시하면 안 된다.
- 하나의 클래스에 시세 조회, 전략 판단, 주문 실행, 포트폴리오, 텔레그램 처리를 모두 넣으면 안 된다.
- Java 기준 버전은 21이다.
- 테스트 성공 전에는 커밋하지 않는다.
- 테스트 실패 시 커밋하지 않고 실패 원인을 보고한다.
- 보안 린트 실패 시 커밋하지 않는다.

## 참조 문서

- `README.md`: 실행 방법과 현재 지원 범위
- `docs/ARCHITECTURE.md`: 시스템 구조와 모듈 경계
- `docs/STRATEGY_POLICY.md`: 롱 전용 변동성 추적 전략 원칙
- `docs/RISK_POLICY.md`: 거래 제한과 손실 방지 정책
- `docs/ORDER_LIFECYCLE.md`: 주문 상태 흐름
- `docs/TELEGRAM_UX.md`: 텔레그램 명령과 버튼 흐름
- `docs/RELIABILITY.md`: 장애 처리와 복구 원칙
- `docs/DEVELOPMENT_RULES.md`: 개발, 테스트, 변경 규칙
- `docs/PROJECT_PLAN.md`: 완료 기록과 다음 구현 순서
