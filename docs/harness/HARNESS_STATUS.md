# Harness Status

## 현재 기준

- Java: 21
- 거래 모드: `PAPER_TRADING`
- 실제 주문 API: 없음
- 웹: 조회 중심 React 운영 화면. 자동매매 제어와 선택 보유 PAPER 포지션 SELL만 허용
- Telegram: 기본 조회 전용
- candidate scheduler 기본 주기: 60초
- exit scheduler 기본 주기: 5초
- 1회 PAPER 주문 금액: 10,000 KRW
- 익절/손절 기준: 1.5 / -0.7
- 같은 market 추가 진입: 기본 허용

## 최근 완료

- market별 쏠림 신규 BUY 차단 구현
- 쏠림 경고 UI 1차 구현
- 반복 손절 cooldown 구현
- OS별 운영 화면/가이드 대응 구현
- 문서/하네스 경량화 계획 수립
- Playwright 기반 실제 브라우저 화면 회귀 테스트 추가
- 모바일 sidebar/nav 가로 overflow 방지
- Risk/System 문서 정합성 재점검 및 보정
- Web API polling 주기 경량화 및 백그라운드 polling 중단

## 다음 작업

1. PAPER 포지션 청산 흐름 스모크 테스트

상세 목표와 완료 기준은 `docs/project/PROJECT_NEXT_STEPS.md`를 따른다.

## 검증 기준

- Backend 변경: `./gradlew test`
- Frontend 변경: `npm run lint`, `npm run build`, `npm test`
- Frontend 화면/반응형/위험 액션 변경: `npm run test:e2e`
- 문서만 변경: `git diff --check`, 필요 시 `./gradlew test`
- 보안 린트 또는 테스트 실패 시 커밋하지 않는다.

## 금지

- `REAL_TRADING` 구현
- 실제 Upbit/Binance 주문 API 구현
- 웹 수동 BUY/SELL 버튼
- 웹에서 후보 실행 API 또는 trading-flow 실행 API 호출
- 민감 정보 하드코딩
- 실패한 주문을 성공으로 처리
