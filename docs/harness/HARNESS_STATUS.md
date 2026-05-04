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

## 최근 완료

- 전체 KRW ticker polling
- 자동 PAPER 후보 실행
- Telegram 수동 실행 기본 차단
- React 모니터링 웹 UI
- Analytics API
- 대시보드 손익/신호 요약
- 포트폴리오 자산 배분과 포지션 손익 UX

## 다음 작업

1. History 분석 화면 개선
2. 중복 진입 제한 강화
3. 포트폴리오 모바일 카드형 UX
4. 후보 화면 개선
5. 운영 상태 화면과 Telegram 용어 정리

세부 작업과 완료 기준은 `docs/project/PROJECT_NEXT_STEPS.md`를 따른다.

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
