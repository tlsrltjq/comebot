# Incident Log

운영 중 발생한 장애와 이상 사례를 기록한다.
실제 주문 API와 `REAL_TRADING`은 구현하지 않으므로, 이 문서는 PAPER 운영과 공개 시세 수집 장애만 다룬다.

## 기록 원칙

- 민감 정보 원문을 적지 않는다.
- Bot Token, Chat ID, DB 비밀번호, Access Key, Secret Key를 기록하지 않는다.
- 실패한 주문을 성공으로 적지 않는다.
- 추정과 확인된 사실을 구분한다.
- 재발 방지 작업이 필요하면 `docs/project/PROJECT_NEXT_STEPS.md` 또는 issue/commit으로 연결한다.

## 템플릿

```md
## YYYY-MM-DD HH:mm KST - 제목

- 현상:
- 영향:
- 확인된 원인:
- 조치:
- 재발 방지:
- 관련 확인:
```

## 기록 대상 예시

- WebSocket 단절 또는 메시지 미수신
- REST fallback 실패
- 거래소 공개 API 429 또는 일시 장애
- DB 연결 실패
- scheduler 지연 또는 중복 실행 skip 증가
- Telegram getUpdates/sendMessage 실패
- PAPER portfolio/history 불일치
- 웹 운영 화면에서 상태가 실제 backend 상태와 다르게 보인 경우

## 2026-05-14 00:00 KST - 문서 생성

- 현상: 클로드 피드백에서 운영 중 장애/이상 사례 기록 문서 부재가 지적됨.
- 영향: 과거 WebSocket, REST fallback, scheduler 이상 사례를 한 곳에서 추적하기 어려움.
- 확인된 원인: 운영 절차 문서는 있었지만 incident 기록 문서는 별도로 없었음.
- 조치: `docs/operations/INCIDENT_LOG.md`를 생성하고 기록 템플릿을 추가함.
- 재발 방지: 장애 처리나 fallback 동작을 변경할 때 이 문서에 실제 운영 사례를 남김.
- 관련 확인: 실제 주문 API와 `REAL_TRADING`은 추가하지 않음.
