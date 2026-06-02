# HARNESS.md — comebot 프로젝트 에이전트 하네스

## 프로젝트 목적

Upbit(KRW)·Binance(USDT) 공개 시세를 수신해 눌림목 반등 롱 후보를 자동 스캔하고,
**PAPER_TRADING**으로 주문·포트폴리오·손익을 검증하는 자동매매 봇.
실제 주문 API, REAL_TRADING, 인증키 사용은 영구 금지(ADR-001).

---

## 현재 상태

| 항목 | 값 |
|---|---|
| 거래 모드 | `PAPER_TRADING` 전용 |
| 거래소 | Upbit (KRW) + Binance (USDT), 공개 API만 |
| 전략 | PullbackBounce (ADR-010) — bean명 `VOLATILITY_BREAKOUT_LONG` |
| 시드 | Upbit 500,000 KRW / Binance 200 USDT (운영 `.env`) |
| 익절 / 손절 | +4.0% / -2.0% (R:R 2:1, 청산 재설계 ADR-011) |
| 트레일링 스톱 | **비활성** (승자 run; trailing이 R:R 훼손 확인) |
| candidate 주기 | 30초 (운영 `.env`; 기본 60초) |
| exit 주기 | 1초 |
| 포지션 상한 | Upbit 8 / Binance 8 / 합계 12 (운영 `.env`) |
| 서버 포트 | 18080 (backend) / 5176 (frontend dev) |
| DB | PostgreSQL 5433 (운영), InMemory (단기 smoke) |
| 최근 작업 | volumeCooldownRatio 진입 필터 추가 (기본값 비활성, ADR-012 후보) |
| 다음 작업 | 로컬 bt_entry_redesign.py 실행 → OOS 게이트 통과 변형 채택 → ADR-012 기록 |

---

## 현재 구현 범위

| 도메인 | 주요 기능 |
|---|---|
| **market** | Upbit/Binance WebSocket ticker + REST fallback; snapshot store; BTC 1h EMA 트렌드 캐시 |
| **strategy** | PullbackBounce 후보 스캔 (BTC 트렌드·양봉·가격변화율·거래대금·눌림폭 필터); per-exchange override |
| **risk** | 포지션 수 상한; 익절/손절/트레일링 스톱; stop-loss cooldown; 쏠림 차단; 비정상 시세 차단; kill switch |
| **execution** | PAPER_TRADING 주문 실행 (체결·거절·실패) |
| **portfolio** | PAPER 현금·포지션·손익; exchange별 분리 |
| **history** | 거래 이력 저장 (JPA / InMemory); exchange별 분리 |
| **scanlog** | 모든 스캔 결과 append-only 기록 (`GET /api/candidate-scan-log`) |
| **analytics** | 승률·평균 보유시간·손익비·반복 손실 market 집계 |
| **scheduler** | candidate (30초); exit (1초); legacy trading (disabled, @Deprecated) |
| **web** | React 운영 화면 — Dashboard·Portfolio·Candidates·History·Analytics·System·Risk |
| **telegram** | 조회 전용 inbound/outbound (수동 실행 금지) |
| **infra** | Docker Compose (app + postgres); scripts/run-*.sh |

---

## 기술 스택

| 계층 | 기술 | 상태 |
|---|---|---|
| Backend | Java 21 / Spring Boot 4.0.5 / Gradle | 운영 중 |
| Frontend | React + TypeScript / Vite | 운영 중 |
| DB | PostgreSQL (JPA, Hibernate 7) | 운영 중 |
| 린트 | Checkstyle 10.21.4 (백엔드) / ESLint (프론트) | 적용 중 |
| 테스트 | JUnit 5 + AssertJ / Vitest + Playwright | 통과 중 |

---

## 작업 원칙

1. **소스코드가 진실** — 문서·하네스보다 코드가 우선. 코드 변경 시 반드시 관련 문서도 갱신.
2. **테스트 먼저** — 비즈니스 로직 변경 시 테스트를 먼저 추가하거나 동시에 추가한다.
3. **작게 커밋** — 논리 단위 하나 = 커밋 하나. 무관한 변경을 섞지 않는다.
4. **완료 기준 준수** — 완료 기준 달성 후 추가 구현 금지. 범위 확장은 사용자 확인 후 진행.
5. **민감 정보 보호** — API 키·토큰·비밀번호는 `.env`로만 관리, 코드·로그·커밋 메시지에 포함 금지.

---

## 절대 금지 사항

| 대상 | 이유 |
|---|---|
| `REAL_TRADING` 구현 | 실제 자산 손실 위험 (ADR-001, 재검토 없음) |
| 실제 Upbit/Binance 주문 API 호출 | 위와 동일 |
| 웹 수동 BUY 버튼 | 의도치 않은 주문 실행 위험 |
| 웹에서 `/run`, `/candidate-run` API 호출 | 위와 동일 |
| 민감 정보 하드코딩 | 보안 사고 위험 |
| 실패·거절 주문을 체결로 처리 | 포트폴리오 오염 |
| 테스트 미통과 상태로 커밋 | 코드 품질 보장 필수 |
| Checkstyle / ESLint 미통과 상태로 커밋 | 위와 동일 |

---

## 기능 추가 절차

1. `tasks/current.md`에 작업 목표·완료 기준·영향 파일 목록 작성
2. 영향 파일 목록 사용자에게 보고 후 승인
3. 테스트 먼저 작성(TDD) 또는 구현과 동시 작성
4. 구현 → `./gradlew test checkstyleMain` 통과 확인
5. 프론트 변경 시: `npm run lint && npm run build && npm test` 통과 확인
6. 관련 문서(`docs/spec.md`, `docs/architecture.md`, `HARNESS.md`) 갱신
7. `CHANGELOG.md` 한 줄 추가
8. `tasks/current.md` 갱신 (다음 세션 시작점)
9. `git commit -m "..."` & `git push`

---

## 테스트 기준

| 구분 | 도구 | 기준 |
|---|---|---|
| Backend 유닛·통합 | JUnit 5 + AssertJ | `./gradlew test` 전체 통과 |
| Backend 린트 | Checkstyle 10.21.4 | `./gradlew checkstyleMain` 전체 통과 |
| Frontend 유닛 | Vitest | `npm test` 전체 통과 |
| Frontend 린트 | ESLint | `npm run lint` 경고 없음 |
| Frontend 빌드 | Vite | `npm run build` 오류 없음 |
| Frontend E2E | Playwright | 화면·위험 액션 변경 시 `npm run test:e2e` 통과 |
| 문서만 변경 | — | `git diff --check` 공백 오류 없음 |

---

## AI 작업 결과 보고 형식

세션 종료 시 아래 형식으로 보고한다:

```
## 작업 완료 보고

**완료한 작업**
- (변경 항목 목록)

**테스트 결과**
- ./gradlew test checkstyleMain: PASS / FAIL
- npm run lint + test: PASS / FAIL (해당 시)

**커밋**
- {커밋 해시} {커밋 메시지}

**다음 세션 시작점**
- (tasks/current.md 갱신 내용 요약)
```
