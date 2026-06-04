# HARNESS.md — comebot 에이전트 하네스

## 목적

Upbit(KRW)·Binance(USDT) 공개 시세로 눌림목 반등 롱 후보를 스캔하고
`PAPER_TRADING`에서 주문·포트폴리오·손익을 검증하는 자동매매 봇.
실제 주문 API, `REAL_TRADING`, 인증키 사용은 영구 금지(ADR-001).

## 현재 상태

| 항목 | 값 |
|---|---|
| 거래 모드 | `PAPER_TRADING` 전용 |
| 거래소 | Upbit(KRW) + Binance(USDT), 공개 API만 |
| 전략 | PullbackBounce / bean `VOLATILITY_BREAKOUT_LONG` |
| 진입 | maker 지정가: 신호 캔들 close 가격, 5분 유효 (ADR-013 보류/조건부) |
| 청산 | TP +4.0%, SL -2.0%, trailing off (ADR-011) |
| 주기 | candidate 30초(운영 `.env`; 기본 60초), exit 1초 |
| 포지션 상한 | Upbit 8 / Binance 8 / 합계 12 |
| 포트 | backend 18080 / frontend dev 5176 / PostgreSQL 5433 |
| 최근 작업 | 매매 일지 기능 추가: BUY→SELL FIFO 매칭 API + `/trade-journal` 프론트 페이지 |
| 다음 단계 | 봇 재기동 후 2~4주 PAPER 관찰: fill_rate, 실 PF, same-candle 0건, stale skip |

## 구현 요약

- Backend: Java 21, Spring Boot 4.0.5, Gradle, JPA/Hibernate 7
- Frontend: React, TypeScript, Vite
- Market: Upbit/Binance ticker WebSocket + REST fallback, BTC 1h EMA trend cache
- Strategy/Risk: per-exchange scanner override, 포지션 상한, 손절 cooldown, 비정상 시세 차단, kill switch
- Execution: PAPER 주문 실행 + `PendingLimitOrderService` maker 지정가 체결(`capturedAt > createdAt`, fresh price only)
- Storage/UI: history, scanlog, analytics, React 운영 화면, 조회 전용 Telegram
- 자세한 구조와 정책은 `docs/architecture.md`, `docs/spec.md`, `docs/decisions.md` 참조

## 불변 규칙

- `REAL_TRADING` 구현 금지
- 실제 Upbit/Binance 주문 API 호출 금지
- 웹 수동 BUY 버튼 및 `/run`, `/candidate-run` 호출 UI 금지
- 민감 정보(API 키, Bot Token, Chat ID, DB 비밀번호) 하드코딩 금지
- 실패·거절 주문을 체결로 처리 금지
- 테스트 또는 Checkstyle/ESLint 실패 상태로 커밋 금지

## 작업 절차

1. 시작 시 `tasks/current.md`를 확인하고 현재 단계/목표를 한 줄로 보고
2. 코드 수정 전 영향 파일 목록 보고
3. 비즈니스 로직 변경은 테스트와 함께 진행
4. 완료 기준 달성 후 범위 확장 금지
5. 관련 문서와 `tasks/current.md` 갱신 후 논리 단위로 커밋

## 수정 주의 파일

| 파일 | 규칙 |
|---|---|
| `.env` | 수정 금지 |
| `src/main/resources/application.properties` | 기본값 변경 전 사용자 확인 |
| `frontend/eslint.config.js` | 린트 완화 전 사용자 확인 |
| `config/checkstyle/checkstyle.xml` | 린트 완화 전 사용자 확인 |
| `docs/trading/condition-records/` | 기존 기록 수정 금지, 추가만 허용 |

## 검증 명령

```bash
./gradlew test checkstyleMain
cd frontend && npm run lint && npm run build && npm test
cd frontend && npm run test:e2e  # 화면/반응형/위험 액션 변경 시
bash scripts/run-local-dev.sh    # backend 18080, frontend 5176
```

문서만 변경한 경우 `git diff --check`로 공백 오류를 확인한다.
