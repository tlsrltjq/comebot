# Project History

## 완료 기록

1. 하네스 문서 구조 생성
2. PAPER_TRADING 주문 실행 골격
3. 리스크 검증 계층
4. 주문 후보 생성 계층
5. InMemory 시세 공급자와 트레이딩 플로우
6. 수동 실행 REST API
7. 테스트 가격 변경 API
8. 트레이딩 플로우 history
9. Scheduler
10. 상태 조회 API
11. Notification 계층
12. Telegram outbound/inbound
13. Upbit 공개 Ticker Provider
14. PostgreSQL 준비와 DB status API
15. JPA history 선택 저장소
16. Telegram offset 저장소
17. PAPER 포트폴리오
18. 포트폴리오 평가 API
19. 익절/손절 정책
20. 일일 리스크 제한
21. Kill Switch
22. Telegram risk/safety 명령
23. Upbit PAPER_TRADING 실행 환경
24. 하네스 문서 재정비
25. 보안 린트 테스트
26. Upbit 공개 Candle Provider
27. 변동성 계산 서비스
28. 롱 후보 스캔
29. 후보 조회 REST API
30. 후보 PAPER 주문 실행 API
31. Telegram 후보 조회/실행 명령
32. 롱 전용 변동성 진입 전략 선택 구조
33. 후보 PAPER 자동 실행 스케줄러
34. 과열 회피와 재진입 제한 조건
35. market별 전략 override 설정
36. candidate scheduler 실행 요약
37. candidate scheduler Telegram 요약 알림
38. Telegram 후보 조회와 실행 결과 요약 개선
39. candidate scheduler 요약과 history 저장 검증
40. React 모니터링 웹 UI
41. 전체 KRW ticker polling과 ALL_KRW 자동 후보 범위
42. 5,000 KRW 단위 PAPER 자동 매수
43. 매매 조건과 PAPER 운용 결과 기록 문서
44. 마이너스 손익 원인 분석과 다음 개선 기준 문서화
45. Telegram 기본 조회 전용화와 웹 수동 실행 API 제거
46. 10,000 KRW 단위 PAPER 자동 매수 설정
47. Analytics API와 대시보드 손익/신호 요약 UX
48. 포트폴리오 자산 배분과 포지션 손익 UX 개선
49. History 분석 화면 필터와 손실 원인 UX 개선
50. 같은 market 추가 진입 기본 허용과 자금 활용률 개선 계획
51. 포트폴리오 자금 사용률과 남은 매수 가능 횟수 표시
52. 포트폴리오 market별 비중 TOP 표시와 2026-05-06 마이너스 손익 분석 문서화
53. 거래소 모드, 선택 PAPER 매도, WebSocket 시세 개선 계획 문서화

## 최근 검증

- `./gradlew test` 통과
- Java 21
- 최신 커밋은 작업 완료 후 갱신한다.
