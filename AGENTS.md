# AGENTS.md

이 프로젝트는 코인 시세 수집, 매수/매도 조건 판단, 텔레그램 알림을 다루는 `comebot` 프로젝트다.
기본 패키지는 `com.giseop.comebot`이다.

## 핵심 규칙

- 기본 거래 모드는 항상 `PAPER_TRADING`이다.
- `REAL_TRADING`은 명시적인 설정값 없이는 절대 활성화하지 않는다.
- 실제 주문 API는 아직 구현하지 않는다.
- API Key, Secret Key, Access Token은 코드에 하드코딩하지 않는다.
- 민감 정보는 `.env` 또는 환경 변수로만 관리한다.
- 매수/매도 판단 로직을 수정하면 테스트를 추가하거나 수정해야 한다.
- 텔레그램 버튼 흐름을 바꾸면 `docs/TELEGRAM_UX.md`도 함께 수정해야 한다.
- 주문 상태 흐름을 바꾸면 `docs/ORDER_LIFECYCLE.md`도 함께 수정해야 한다.
- 리스크 정책을 바꾸면 `docs/RISK_POLICY.md`도 함께 수정해야 한다.
- 실패한 주문을 성공으로 처리하면 안 된다.
- 예외를 무시하면 안 된다.
- 하나의 클래스에 시세 조회, 전략 판단, 주문 실행, 텔레그램 처리를 모두 넣으면 안 된다.

## 참조 문서

- `docs/ARCHITECTURE.md`: 시스템 구조와 모듈 경계
- `docs/RISK_POLICY.md`: 거래 제한과 손실 방지 정책
- `docs/ORDER_LIFECYCLE.md`: 주문 상태 흐름
- `docs/TELEGRAM_UX.md`: 텔레그램 명령과 버튼 흐름
- `docs/RELIABILITY.md`: 장애 처리와 복구 원칙
- `docs/DEVELOPMENT_RULES.md`: 개발, 테스트, 변경 규칙
