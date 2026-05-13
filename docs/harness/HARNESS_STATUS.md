# Harness Status

## 현재 기준

- Java: 21
- 거래 모드: `PAPER_TRADING`
- 실제 주문 API: 없음
- Git 관리: 작업 전 상태 확인, 사용자 변경 보호, 파괴적 정리 명령 금지
- 웹: React 운영 화면. 조회 중심이며 자동매매 제어와 선택 보유 PAPER 포지션 SELL만 허용
- Telegram: 기본 조회 전용
- 자동 실행 대상: `ALL_KRW`
- candidate scheduler 기본 주기: 60초
- exit scheduler 기본 주기: 5초
- 1회 PAPER 주문 금액: 10,000 KRW
- 익절/손절 기준: 1.5 / -0.7
- 같은 market 추가 진입: 기본 허용

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
- 포트폴리오 market별 비중 TOP 표시
- 2026-05-06 PAPER 매매기록 기준 마이너스 손익 원인 분석 문서화
- 운영 상태 화면과 Telegram 용어 정리
- Dashboard 운영 준비 상태 중심 UX 개선
- Trade 자동매매 제어 UX 정리
- JPA PAPER 누적 실행 스크립트와 확인 절차 정리
- JPA PAPER 누적 데이터 스냅샷 기록
- OS별 운영 화면/가이드 대응 계획 문서화
- market별 쏠림 리스크 기준 문서화
- market별 쏠림 신규 BUY 차단 구현
- 쏠림 경고 UI와 반복 손절 cooldown 설계
- 쏠림 경고 UI 1차 구현
- 반복 손절 cooldown 구현
- OS별 운영 화면/가이드 대응 구현

## 다음 작업

1. Dashboard/Candidates 리스크 경고 요약

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
