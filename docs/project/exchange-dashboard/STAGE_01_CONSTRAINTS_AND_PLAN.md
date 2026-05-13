# Stage 1: Constraints and Plan

> Historical plan. Current behavior is defined by code, tests, Git history, and active policy documents.

## 목표

거래소 모드 확장, 선택 PAPER 매도, WebSocket 시세 수신, 자산 차트, BTC 등락률 그래프의 전체 방향을 문서화한다.

## 포함 기능

- 현재 문서 제약 확인
- `PAPER_TRADING` 범위 고정
- 실제 주문 API와 `REAL_TRADING` 제외 명시
- 수동 BUY 금지 명시
- 선택 보유 PAPER 포지션 SELL 예외 정의
- `ExchangeMode` 방향 정의
- WebSocket/REST fallback 방향 정의
- 자동매매 주기 분리 방향 정의

## 제외 기능

- 코드 구현
- API 변경
- UI 변경
- DB schema 변경

## 완료 기준

- PAPER 선택 매도 예외와 실제 주문 금지가 문서상 충돌하지 않는다.
- ExchangeMode, WebSocket, REST fallback, scheduler 주기 방향이 문서화된다.
- 이후 stage가 독립적으로 구현 가능한 단위로 나뉜다.
