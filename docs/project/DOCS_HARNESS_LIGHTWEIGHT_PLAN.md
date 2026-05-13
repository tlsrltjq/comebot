# Docs Harness Lightweight Plan

## 목표

문서와 하네스는 안전 제약을 유지하되, 매 작업마다 너무 많은 파일을 읽고 수정하지 않도록 줄인다.
린트와 테스트는 유지하고, 중복 계획/완료 기록/상태 문서를 정리해 작업 토큰과 시간을 줄인다.

## 현재 문제

- `docs` 파일이 33개로 늘어났다.
- 최근 구현 작업마다 5~9개 문서를 함께 수정했다.
- `PROJECT_HISTORY`, `PROJECT_NEXT_STEPS`, `HARNESS_STATUS`가 완료 기록과 다음 작업을 중복해서 들고 있다.
- 완료된 Stage 1-10 계획 문서가 여전히 현재 계획처럼 읽힌다.
- `DEVELOPMENT_RULES`가 "기능 변경 시 관련 문서를 함께 수정"을 넓게 요구해, 작은 UI 작업도 문서 수정으로 이어진다.
- 린트 자체보다 문서 정합성 확인 비용이 더 크다.

## 유지할 문서

항상 최신이어야 하는 문서:

- `AGENTS.md`
- `README.md`
- `docs/harness/DEVELOPMENT_RULES.md`
- `docs/harness/HARNESS_STATUS.md`
- `docs/harness/SECURITY_RULES.md`
- `docs/trading/STRATEGY_POLICY.md`
- `docs/trading/RISK_POLICY.md`
- `docs/trading/ORDER_LIFECYCLE.md`
- `docs/operations/OPERATIONS.md`
- `docs/operations/RELIABILITY.md`
- `docs/operations/TELEGRAM_UX.md`

다음 작업만 관리하는 문서:

- `docs/project/PROJECT_NEXT_STEPS.md`

이 문서는 완료된 전체 이력을 길게 보관하지 않는다.
다음 작업 1개와 보류 목록만 유지한다.

## 축소할 문서

### `docs/project/PROJECT_HISTORY.md`

역할을 "최근 완료 10개"로 줄인다.
오래된 번호 목록은 삭제하거나 archive로 이동한다.

완료 기록은 Git history를 source of truth로 본다.
문서에는 사용자가 현재 맥락을 잡는 데 필요한 최근 작업만 둔다.

### `docs/harness/HARNESS_STATUS.md`

다음 내용만 유지한다.

- 현재 기준
- 최근 완료 5개
- 다음 작업 1개
- 검증 기준
- 금지 사항

긴 완료 목록은 제거한다.

### `docs/project/PROJECT_PLAN.md`

상위 인덱스 역할만 남긴다.
완료된 단계 설명과 오래된 "현재 단계" 문단은 제거한다.

## 아카이브 후보

다음 문서는 현재 구현의 active source가 아니라 완료된 계획 기록이다.
`docs/archive/`로 이동하거나 파일 상단에 `Historical plan` 표시를 붙인다.

- `docs/project/EXCHANGE_DASHBOARD_UPGRADE_PLAN.md`
- `docs/project/WEB_UX_UPGRADE_PLAN.md`
- `docs/project/CONCENTRATION_WARNING_AND_COOLDOWN_PLAN.md`
- `docs/project/exchange-dashboard/STAGE_01_CONSTRAINTS_AND_PLAN.md`
- `docs/project/exchange-dashboard/STAGE_02_BACKEND_EXCHANGE_MODE.md`
- `docs/project/exchange-dashboard/STAGE_03_FRONTEND_EXCHANGE_MODE.md`
- `docs/project/exchange-dashboard/STAGE_04_BINANCE_REST_PROVIDER.md`
- `docs/project/exchange-dashboard/STAGE_05_EXCHANGE_PORTFOLIO_HISTORY.md`
- `docs/project/exchange-dashboard/STAGE_06_SELECTED_PAPER_SELL.md`
- `docs/project/exchange-dashboard/STAGE_07_PORTFOLIO_PIE_CHART.md`
- `docs/project/exchange-dashboard/STAGE_08_WEBSOCKET_PRICE_FEED.md`
- `docs/project/exchange-dashboard/STAGE_09_SCHEDULER_CADENCE.md`
- `docs/project/exchange-dashboard/STAGE_10_BTC_CHANGE_CHART.md`

추천은 이동보다 "Historical plan" 표시다.
파일 이동은 링크 깨짐과 검색 비용이 생기므로, 1차 경량화에서는 상단 표시와 인덱스 축소만 한다.

## 갱신 규칙 변경

문서 갱신은 변경 범위에 따라 최소화한다.

반드시 갱신:

- 전략 판단 변경: `docs/trading/STRATEGY_POLICY.md`
- 리스크 정책 변경: `docs/trading/RISK_POLICY.md`
- 주문 상태 흐름 변경: `docs/trading/ORDER_LIFECYCLE.md`
- Telegram 명령/버튼 변경: `docs/operations/TELEGRAM_UX.md`
- 장애 처리, WebSocket, REST fallback 변경: `docs/operations/RELIABILITY.md`
- 운영 명령, 환경변수, 실행 절차 변경: `docs/operations/OPERATIONS.md`
- 보안/린트 규칙 변경: `docs/harness/SECURITY_RULES.md`

선택 갱신:

- 일반 UI 개선: 테스트만 갱신하고 문서는 생략 가능
- 문구/레이아웃 변경: 문서 생략 가능
- 내부 refactor: 문서 생략 가능
- 테스트 보강: 문서 생략 가능

항상 갱신하지 않는다:

- `PROJECT_HISTORY` 긴 목록
- 완료된 계획 문서
- 모든 하네스 문서 동시 갱신

## 린트 유지 계획

유지:

- `SecurityLintTest`
- `frontend/eslint.config.js`

이유:

- 실행 비용은 낮고 안전 효과가 크다.
- 실제 주문 API, `REAL_TRADING`, 민감 정보 하드코딩을 자동으로 막는다.

변경하지 않을 것:

- 린트를 약화하지 않는다.
- 보안 린트 실패 시 커밋 금지 규칙은 유지한다.

## 적용 단계

### 1단계: 규칙 완화

- `docs/harness/DEVELOPMENT_RULES.md`의 "기능 변경 시 관련 문서를 함께 수정"을 구체 조건으로 변경한다.
- `PROJECT_HISTORY`와 `HARNESS_STATUS`는 매 작업 필수 갱신 대상에서 제외한다.

### 2단계: 상태 문서 축소

- `HARNESS_STATUS`를 현재 기준, 최근 완료 5개, 다음 작업 1개로 축소한다.
- `PROJECT_NEXT_STEPS`는 다음 작업 1개와 보류 목록만 남긴다.

### 3단계: 완료 계획 표시

- 완료된 project plan 문서 상단에 `Historical plan`을 붙인다.
- Stage 1-10 문서는 active next-step 검색 대상에서 제외한다고 `PROJECT_PLAN`에 적는다.

### 4단계: 검증

- `git diff --check`
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew test`
- 문서만 바뀐 경우 frontend 테스트는 생략 가능

## 완료 기준

- 다음 작업을 파악할 때 읽어야 하는 문서가 `AGENTS.md`, `HARNESS_STATUS`, `PROJECT_NEXT_STEPS` 정도로 줄어든다.
- 일반 UI/테스트 작업에서 문서 수정이 필수가 아니다.
- 리스크/전략/주문/보안 변경의 문서 갱신 의무는 유지된다.
- 린트와 보안 테스트는 그대로 유지된다.
