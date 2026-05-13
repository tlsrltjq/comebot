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
- market별 쏠림 신규 BUY 차단 구현 완료

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

## 완료: 쏠림 경고 UI와 반복 손절 cooldown 설계

목표:

- market별 쏠림 기준 중 아직 남은 경고 표시와 반복 손절 cooldown을 설계한다.
- 신규 BUY 차단은 `risk.concentration.enabled=true`일 때만 적용되는 상태를 유지한다.
- 기본 거래 모드는 계속 `PAPER_TRADING`으로 유지한다.
- 실제 주문 API와 `REAL_TRADING`은 추가하지 않는다.

작업:

- Dashboard/Portfolio/Candidates 중 쏠림 경고를 표시할 화면과 API 범위를 정한다.
- 반복 손절 cooldown은 기간 window와 reset 조건을 먼저 설계한다.
- Binance와 UPBIT의 경고 threshold를 같은 UI에서 혼동 없이 보여주는 방식을 정한다.

완료 기준:

- 구현 대상 파일과 테스트 범위가 명확하다.
- 정책 문서와 코드 적용 범위가 충돌하지 않는다.
- SELL, 익절, 손절 흐름이 쏠림 기준으로 막히지 않는다는 점이 명확하다.

설계 문서:

- `docs/project/CONCENTRATION_WARNING_AND_COOLDOWN_PLAN.md`

## 완료: 쏠림 경고 UI 1차 구현

목표:

- Portfolio 화면에서 exchange별 쏠림 경고/차단 기준을 혼동 없이 표시한다.
- React가 리스크 판단을 재구현하지 않도록 서버 risk status에 concentration threshold를 포함한다.
- BUY 차단 설정과 UI 경고 표시의 차이를 명확히 유지한다.

작업:

- `GET /api/risk/status`에 concentration 설정 응답을 추가한다.
- Portfolio의 `market별 비중(Market Exposure)` 패널이 UPBIT 7%/10%, BINANCE 25%/40% 기준으로 뱃지를 표시하게 한다.
- 관련 backend/frontend 테스트를 추가한다.

완료 기준:

- UPBIT과 BINANCE 기준이 같은 화면에서 섞이지 않는다.
- `risk.concentration.enabled=false`여도 UI 경고 기준은 운영 참고로 표시된다.
- SELL, 익절, 손절 흐름은 변경하지 않는다.

## 다음 우선순위: 반복 손절 cooldown 구현

목표:

- 최근 7일 같은 `exchange + market`에서 FILLED Stop loss SELL이 2회 이상 발생한 market의 신규 BUY를 제한한다.
- 기본값은 비활성화로 두고, SELL/익절/손절/선택 PAPER SELL은 막지 않는다.

작업:

- `risk.stop-loss-cooldown.*` properties를 추가한다.
- JPA history 기반 cooldown 산정 service를 추가한다.
- 신규 BUY risk guard에 cooldown 검증을 연결한다.
- backend 테스트로 BUY 차단, SELL 미차단, 실패/거절 손절 제외를 검증한다.

완료 기준:

- cooldown이 꺼져 있으면 기존 BUY 흐름이 유지된다.
- cooldown이 켜져 있으면 대상 market 신규 BUY만 거절된다.
- 문서와 테스트가 7일/2회/24시간 기준을 동일하게 말한다.

## 보류: 실제 주문 API

실제 주문 API와 `REAL_TRADING`은 별도 승인 전까지 구현하지 않는다.
