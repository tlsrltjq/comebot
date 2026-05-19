# AGENTS.md
# Claude Code가 세션 시작 시 자동으로 읽는 파일

이 프로젝트는 Upbit/Binance 공개 시세를 기반으로 코인 변동성을 추적하고, 롱 전용 매수 후보를 판단한 뒤 `PAPER_TRADING`으로 검증하는 `comebot` 프로젝트다.
기본 패키지는 `com.giseop.comebot`이다.

---

## 세션 시작 시 필수 작업 (자동 실행)

1. `HARNESS.md` 읽기 — 프로젝트 전체 맥락 파악
2. `tasks/current.md` 확인 — 현재 단계와 완료 기준 파악
3. 아래 형식으로 한 줄 요약 출력 후 작업 시작:
   ```
   현재 단계: [단계명] / 목표: [목표] / 완료 기준: [기준]
   ```
4. `git status --short --branch`로 브랜치와 변경 파일 확인

## 세션 종료 시 필수 작업 (사용자가 '마무리해줘' 또는 '세션 종료' 요청 시)

1. `HARNESS.md` "현재 상태" 섹션 업데이트 (완료/다음/보류)
2. `CHANGELOG.md`에 한 줄 추가 (형식: `날짜 | 단계 | feat/fix/chore/docs: 내용`)
3. `tasks/current.md` "이전 세션에서 멈춘 곳" 업데이트
4. `docs/harness/HARNESS_STATUS.md` "최근 완료"와 "다음 작업" 동기화

## GC 트리거

사용자가 `gc 실행해` 라고 하면 `GC_ROUTINE.md`의 실행 순서를 따른다.

---

## 핵심 규칙

- 기본 거래 모드는 항상 `PAPER_TRADING`이다.
- `REAL_TRADING`은 구현하지 않는다.
- 실제 주문 API는 구현하지 않는다.
- 웹 포트폴리오에서 사용자가 명시 선택한 보유 PAPER 포지션 매도만 예외적으로 허용한다.
- 수동 BUY와 실제 거래소 주문은 계속 금지한다.
- Upbit Access Key, Secret Key, API Key, Bot Token, Chat ID는 코드에 하드코딩하지 않는다.
- 민감 정보는 `.env` 또는 환경 변수로만 관리한다.
- 현재 전략 방향은 롱 전용이다. 숏, 레버리지, 마진 거래는 다루지 않는다.
- 매수 판단은 변동성, 추세, 거래대금, 리스크 조건을 분리해서 검증한다.
- 익절/손절 조건은 주문 실행 전후 흐름과 함께 테스트해야 한다.
- 매수/매도 판단 로직을 수정하면 테스트를 추가하거나 수정해야 한다.
- 실패한 주문을 성공으로 처리하면 안 된다.
- 예외를 무시하면 안 된다.
- 하나의 클래스에 시세 조회, 전략 판단, 주문 실행, 포트폴리오, 텔레그램 처리를 모두 넣으면 안 된다.
- Java 기준 버전은 21이다.

## 작업 중 행동 규칙

- 코드 수정 전: 영향 받는 파일 목록 먼저 보고
- 새 패키지 설치 전: 반드시 사용자에게 확인 요청
- 완료 기준 달성 시: 멈추고 결과 보고 (추가 구현 금지)
- 불확실한 부분: 가정하고 진행하지 말고 질문

## Git 관리 규칙

- 사용자가 만들었을 수 있는 변경 파일은 되돌리거나 덮어쓰지 않는다.
- `git reset --hard`, `git checkout -- <path>` 같은 파괴적 정리 명령은 사용자 명시 승인 없이는 실행하지 않는다.
- 커밋에는 작업 범위와 직접 관련된 파일만 포함한다.
- 테스트 성공 전에는 커밋하지 않는다.
- 테스트 실패 시 커밋하지 않고 실패 원인을 보고한다.
- 보안 린트 실패 시 커밋하지 않는다.

## 문서 갱신 규칙

| 변경 범위 | 함께 수정할 문서 |
|---|---|
| 주문 상태 흐름 | `docs/trading/ORDER_LIFECYCLE.md` |
| 리스크 정책 | `docs/trading/RISK_POLICY.md` |
| 전략 조건 | `docs/trading/STRATEGY_POLICY.md` |
| Telegram 명령/버튼 | `docs/operations/TELEGRAM_UX.md` |
| 거래소·WebSocket·REST fallback | `docs/harness/ARCHITECTURE.md` + `docs/operations/RELIABILITY.md` |
| 구조/모듈 경계 | `docs/harness/ARCHITECTURE.md` |
| 보안 변경 | `docs/harness/SECURITY_RULES.md` |
| 기술 결정 | `docs/decisions.md` ADR 추가 |
| 완료 기록 | `CHANGELOG.md` 한 줄 추가 |

---

## 참조 문서 인덱스

| 문서 | 역할 |
|---|---|
| `HARNESS.md` | 프로젝트 전체 요약 (진입점) |
| `tasks/current.md` | 현재 세션 작업 컨텍스트 |
| `CHANGELOG.md` | 변경 이력 |
| `GC_ROUTINE.md` | 가비지 컬렉션 루틴 |
| `docs/decisions.md` | 기술 결정 ADR |
| `docs/harness/HARNESS_STATUS.md` | 현재 기준값, 검증 명령, 금지 목록 |
| `docs/harness/ARCHITECTURE.md` | 시스템 구조와 모듈 경계 |
| `docs/harness/DEVELOPMENT_RULES.md` | 개발, 테스트, 변경 규칙 |
| `docs/harness/SECURITY_RULES.md` | 보안 린트 항목 |
| `docs/trading/STRATEGY_POLICY.md` | 전략 조건과 기본값 |
| `docs/trading/RISK_POLICY.md` | 거래 제한과 손실 방지 정책 |
| `docs/trading/ORDER_LIFECYCLE.md` | 주문 상태 흐름 |
| `docs/operations/OPERATIONS.md` | 실행 스크립트, 상태 확인 API |
| `docs/operations/RELIABILITY.md` | 장애 처리와 복구 원칙 |
| `docs/operations/TELEGRAM_UX.md` | Telegram 명령과 버튼 흐름 |
| `docs/project/PROJECT_PLAN.md` | 문서 인덱스 |
| `docs/project/PROJECT_NEXT_STEPS.md` | 완료 기록과 다음 작업 상세 |
