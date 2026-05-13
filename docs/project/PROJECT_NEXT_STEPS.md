# Project Next Steps

## 완료된 큰 흐름

- Stage 1-10 거래소 대시보드 확장 완료
  - ExchangeMode 백엔드/API 골격
  - 프론트 ExchangeMode 전환
  - Binance 공개 REST 현재가/캔들 provider
  - Exchange별 PAPER 포트폴리오와 history 분리
  - 선택 보유 PAPER 포지션 수동 매도
  - 포트폴리오 원형 그래프
  - WebSocket snapshot store와 REST fallback provider
  - candidate/exit scheduler 주기 분리
  - BTC 등락률 Market Overview 페이지
- 포트폴리오 모바일 카드형 UX 완료
- 후보 화면 SELECTED/SKIPPED 요약, 제외 사유 TOP 5, 선택 후보 필터, 보유 포지션 표시 완료
- 운영 상태 화면과 Telegram 용어 정리 완료
- Dashboard 운영 준비 상태 중심 UX 개선 완료
- Trade 자동매매 제어 UX 정리 완료
- JPA PAPER 누적 실행 스크립트와 확인 절차 정리 완료
- JPA PAPER 누적 데이터 스냅샷 기록 완료

## 다음 우선순위: market별 쏠림 리스크 기준 문서화

목표:

- JPA PAPER 데이터 스냅샷을 기준으로 market별 쏠림 리스크 기준을 문서화한다.
- 단일 market 비중 경고/차단 기준과 반복 손절 market 제한 기준을 분리한다.
- 기본 거래 모드는 계속 `PAPER_TRADING`으로 유지한다.
- 실제 주문 API와 `REAL_TRADING`은 추가하지 않는다.

작업:

- `docs/trading/condition-records/2026-05-13-jpa-paper-data-snapshot.md`의 노출/손절 데이터를 기준으로 초기 threshold를 정한다.
- UPBIT과 BINANCE의 초기 현금과 market universe 차이를 반영한다.
- 기준은 먼저 `docs/trading/RISK_POLICY.md`에 문서화한다.
- 구현은 문서 기준 확정 후 별도 작업으로 진행한다.

완료 기준:

- 단일 market 비중 경고/차단 기준이 명확하다.
- 반복 손절 market 제한 기준이 명확하다.
- `RISK_POLICY.md`와 condition record가 같은 기준을 설명한다.
- 아직 코드 제한을 추가하지 않는다는 범위가 명확하다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
