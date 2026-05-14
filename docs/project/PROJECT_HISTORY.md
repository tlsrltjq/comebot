# Project History

완료 이력의 source of truth는 Git history다.
이 문서는 최근 맥락 파악용 항목만 유지한다.

## 최근 완료

1. Dashboard 운영 준비 상태 중심 UX 개선
2. Trade 자동매매 제어 UX 정리
3. JPA PAPER 누적 실행 스크립트와 확인 절차 정리
4. JPA PAPER 누적 데이터 스냅샷 기록
5. OS별 운영 화면/가이드 대응 계획 문서화
6. market별 쏠림 리스크 기준 문서화
7. market별 쏠림 신규 BUY 차단 구현
8. 쏠림 경고 UI 1차 구현
9. 반복 손절 cooldown 구현
10. OS별 운영 화면/가이드 대응 구현
11. PAPER 체결 append-only 원장 추가
12. Telegram 수동 PAPER 실행 코드 레벨 차단
13. scheduler 중복 실행 방어 테스트 보강
14. PAPER 실행 runtime guard 추가
15. 실제 주문 API security lint 강화
16. WebSocket snapshot freshness 운영 상태 보강
17. Incident Log와 API rate limit 운영 기준 문서화
18. PAPER 현금 부족 read-only 경고 추가
19. WebSocket reconnect/backoff 구현과 테스트 보강

## 최근 검증

- Java 21 기준 `./gradlew test` 통과
- `npm run lint`, `npm test`, `npm run build` 통과
