# Harness Status

## 현재 기준

- Java: 21
- 거래 모드: `PAPER_TRADING`
- 실제 주문 API: 없음
- 웹: React 모니터링 전용
- Telegram: 기본 조회 전용
- 자동 실행 대상: `ALL_KRW`
- 자동 실행 주기: 30초
- 1회 PAPER 주문 금액: 10,000 KRW
- 익절/손절 기준: 1.5 / -0.7
- 같은 market 추가 진입: 기본 허용
- MVP2: 패키지와 용어 경계 생성 시작

## 최근 완료

- 전체 KRW ticker polling
- 자동 PAPER 후보 실행
- Telegram 수동 실행 기본 차단
- React 모니터링 웹 UI
- Analytics API
- 대시보드 손익/신호 요약
- 포트폴리오 자산 배분과 포지션 손익 UX
- History 분석 화면 필터와 손실 원인 UX
- 포트폴리오 자금 사용률과 남은 매수 가능 횟수 표시
- 포트폴리오 market별 비중 TOP과 쏠림 경고 표시
- 2026-05-06 PAPER 매매기록 기준 마이너스 손익 원인 분석 문서화
- MVP2 `exchange`, `experiment`, `simulation`, `strategy`, `leaderboard` 패키지 경계 생성

## 다음 작업

1. MVP2 Exchange 공통 모델과 Upbit adapter
2. Binance public market data
3. 거래소별 상태 API와 웹 선택 버튼
4. 전략 profile 3종 동시 실행

세부 작업과 완료 기준은 `docs/project/PROJECT_NEXT_STEPS.md`를 따른다.

## MVP2 방향

MVP2 계획은 `docs/project/MVP2_PLAN.md`에 분리한다.

- Upbit / Binance 멀티 거래소 지원
- 거래소별 대시보드 분리
- 안정형 / 공격형 / 수비형 전략 profile 동시 테스트
- spot, futures long simulation, futures short simulation 분리
- 실제 주문 없이 PAPER/SIMULATION으로 먼저 검증

## 검증 기준

- Backend 변경: `./gradlew test`
- Frontend 변경: `npm run lint`, `npm run build`, `npm test`
- 보안 린트 실패 시 커밋하지 않는다.
- 테스트 실패 시 커밋하지 않는다.

## 금지

- `REAL_TRADING` 구현
- 실제 Upbit 주문 API 구현
- 웹 수동 BUY/SELL 버튼
- 웹에서 후보 실행 API 또는 trading-flow 실행 API 호출
- 민감 정보 하드코딩
- 실패한 주문을 성공으로 처리
