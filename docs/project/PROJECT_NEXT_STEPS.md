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
- OS별 운영 화면/가이드 대응 계획 문서화 완료
- market별 쏠림 리스크 기준 문서화 완료

## 향후 개선 후보: OS별 운영 화면/가이드 대응

목표:

- 사용자의 운영체제가 macOS인지 Windows인지 파악해서 화면 안내와 실행 가이드를 맞춘다.
- 웹 기능은 동일하게 유지하되, OS별 실행 스크립트, 경로 표기, 단축키, 스크롤/폰트 차이를 고려한다.
- 민감 정보 입력 방식은 OS와 무관하게 `.env` 또는 환경 변수만 사용한다.

작업:

- 프론트에서 `navigator.userAgentData` 또는 `navigator.userAgent` 기반으로 OS를 추정하는 helper를 추가한다.
- 운영/설정 화면에서 macOS는 `scripts/*.sh`, Windows는 `scripts\\*.bat` 예시를 우선 표시한다.
- 파일 경로 예시는 macOS `/Users/...`, Windows `%USERPROFILE%\\...` 형식으로 분기한다.
- 단축키가 생기면 macOS는 `Cmd`, Windows는 `Ctrl` 기준으로 표기한다.
- 브라우저/OS별 폰트와 스크롤바 때문에 카드/버튼 텍스트가 깨지지 않는지 UI 테스트 계획을 추가한다.

완료 기준:

- 사용자가 현재 OS에 맞는 실행/운영 안내를 웹에서 볼 수 있다.
- OS 감지는 안내 표시 용도에만 사용하고, 거래/리스크 판단 로직에는 영향을 주지 않는다.
- macOS와 Windows 모두에서 텍스트 overflow, 버튼 폭, 표 스크롤이 깨지지 않는다.

## 다음 우선순위: market별 쏠림 제한 구현 검토

목표:

- 문서화된 쏠림 기준을 실제 PAPER 신규 BUY 제한에 적용할지 검토한다.
- 단일 market 비중 경고/차단과 반복 손절 cooldown을 분리해서 설계한다.
- 기본 거래 모드는 계속 `PAPER_TRADING`으로 유지한다.
- 실제 주문 API와 `REAL_TRADING`은 추가하지 않는다.

작업:

- `docs/trading/RISK_POLICY.md`의 market별 쏠림 기준을 코드 적용 범위로 나눈다.
- 신규 BUY 차단은 risk validator 계층에서 처리하는 방향을 우선 검토한다.
- 경고 표시는 Dashboard/Portfolio/Candidates 중 어디에 둘지 정한다.
- 반복 손절 cooldown은 기간 window와 reset 조건을 먼저 설계한다.

완료 기준:

- 구현 대상 파일과 테스트 범위가 명확하다.
- 정책 문서와 코드 적용 범위가 충돌하지 않는다.
- SELL, 익절, 손절 흐름이 쏠림 기준으로 막히지 않는다는 점이 명확하다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
