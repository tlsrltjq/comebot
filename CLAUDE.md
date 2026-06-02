# CLAUDE.md — comebot AI 에이전트 운영 규칙

## 세션 시작 체크리스트

1. `HARNESS.md` 읽기 — 프로젝트 목적, 현재 상태, 금지 사항 파악
2. `tasks/current.md` 읽기 — 이번 세션 목표, 완료 기준, 이전 세션 중단 지점 파악
3. 현재 단계와 목표를 **한 줄**로 출력한 뒤 작업 시작

   예: `[단계: 전략 파라미터 튜닝] 목표: Upbit min-price-change-rate 0.15→0.1 조정 후 테스트 통과`

---

## 세션 종료 체크리스트

다음 순서를 **반드시** 지킨다. 하나라도 실패하면 중단하고 사용자에게 알린다.

1. `./gradlew test checkstyleMain` 통과 확인
2. `HARNESS.md` → `## 현재 상태` 갱신 (바뀐 설정값·완료 항목 반영)
3. `CHANGELOG.md` 맨 위에 한 줄 추가 (`YYYY-MM-DD | 단계 | feat/fix/chore/docs: 내용`)
4. `tasks/current.md` 갱신 (다음 세션 시작점, 미완료 항목, 주의사항)
5. `git add -A && git commit -m "..." && git push origin master`

---

## 행동 규칙

### 시작 전
- 코드 수정 전 **영향 받는 파일 목록을 먼저 보고**하고 계속 진행한다.
- 새 패키지(디렉터리) 추가 전에는 반드시 사용자에게 확인 요청한다.
- 불확실한 사항은 가정하지 말고 질문한다.

### 작업 중
- 완료 기준이 달성되면 추가 구현을 하지 않는다.
- 한 번에 하나의 논리 단위만 변경한다 (커밋 단위 = 논리 단위).
- 테스트 없이 비즈니스 로직을 커밋하지 않는다.
- 보안 린트(`eslint`) 또는 `checkstyleMain` 실패 시 커밋하지 않는다.

### 코드 원칙
- 거래 모드는 항상 `PAPER_TRADING`. `REAL_TRADING`은 구현하지 않는다.
- 실제 Upbit/Binance 주문 API는 호출하지 않는다.
- 민감 정보(API 키, Bot Token, Chat ID, DB 비밀번호)는 코드에 하드코딩하지 않는다.

---

## 수정 금지 파일

| 파일 | 이유 |
|---|---|
| `.env` | 운영 환경 시크릿 포함. 코드로 수정 금지, 직접 편집만 허용 |
| `src/main/resources/application.properties` | 기본값 변경은 반드시 사용자 확인 후 진행 |
| `frontend/eslint.config.js` | 보안 린트 규칙. 완화 시 사용자 확인 필요 |
| `config/checkstyle/checkstyle.xml` | 린트 규칙. 완화 시 사용자 확인 필요 |
| `docs/trading/condition-records/` | 운용 기록. 수정 금지, 추가만 허용 |

---

## 검증 명령

```bash
# 백엔드 테스트 + 린트 (커밋 전 필수)
./gradlew test checkstyleMain

# 프론트엔드 린트 + 빌드 + 유닛 테스트
cd frontend && npm run lint && npm run build && npm test

# 프론트엔드 E2E (화면/반응형/위험 액션 변경 시)
cd frontend && npm run test:e2e

# 서버 기동 (백엔드 + 프론트 동시)
bash scripts/run-local-dev.sh
# 백엔드: http://127.0.0.1:18080
# 웹 UI:  http://127.0.0.1:5176
```

---

## 참고 파일 빠른 색인

| 목적 | 파일 |
|---|---|
| 프로젝트 현황·원칙 | `HARNESS.md` |
| 현재 작업 컨텍스트 | `tasks/current.md` |
| 변경 이력 | `CHANGELOG.md` |
| 기술 결정 이유 (ADR) | `docs/decisions.md` |
| 모듈 구조·데이터 흐름 | `docs/architecture.md` |
| 전략·리스크 SSOT | `docs/spec.md` |
| 리스크 정책 상세 | `docs/trading/RISK_POLICY.md` |
| 전략 정책 상세 | `docs/trading/STRATEGY_POLICY.md` |
| 운용 이력 | `docs/trading/condition-records/` |
| 운영 절차 | `docs/operations/OPERATIONS.md` |
