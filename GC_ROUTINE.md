# GC_ROUTINE.md

## 실행 방법

```
gc 실행해
```

이 한 마디를 세션에 입력하면 아래 루틴을 순서대로 실행한다.

---

## 실행 순서

### 1단계 — 미사용 코드 탐지
```bash
# 미참조 Java 클래스/메서드 후보 검색
grep -r "class\|interface\|@Service\|@Component" src/main/java --include="*.java" -l | \
  while read f; do
    classname=$(grep -oP '(?<=class |interface )\w+' "$f" | head -1)
    [ -n "$classname" ] && grep -rq "$classname" src/ --include="*.java" \
      && echo "참조 있음: $f" || echo "참조 의심: $f — $classname"
  done 2>/dev/null | grep "참조 의심" | head -30

# 미참조 프론트엔드 컴포넌트/유틸 검색
cd frontend && npx ts-unused-exports tsconfig.app.json 2>/dev/null | head -30; cd ..
```

### 2단계 — 하네스 정합성 확인
```bash
# HARNESS.md 디렉토리 구조와 실제 src 구조 비교
diff \
  <(grep -oP '(?<=  )[\w/]+(?=/)' HARNESS.md | sort) \
  <(ls src/main/java/com/giseop/comebot/ | sort) \
  | head -20

# docs/ 에서 30일 이상 미수정 파일
find docs/ -name "*.md" -not -newer <(date -v-30d +%Y%m%d 2>/dev/null || date -d "30 days ago" +%Y%m%d 2>/dev/null) \
  -exec ls -la {} \; 2>/dev/null | head -20

# CHANGELOG.md와 git log 최신 커밋 일치 확인 (최근 5개)
echo "=== CHANGELOG 최근 5줄 ==="
head -10 CHANGELOG.md
echo "=== git log 최근 5개 ==="
git log --oneline -5
```

### 3단계 — 오래된 문서 플래그
```bash
# docs/project/ 내 Historical Plans 목록 확인
grep -A 20 "Historical Plans" docs/project/PROJECT_PLAN.md

# condition-records 가장 오래된 파일
ls -lt docs/trading/condition-records/ | tail -5

# tasks/current.md 마지막 수정일
git log --format="%ad %s" --date=short -- tasks/current.md | head -3
```

---

## 보고서 형식

GC 실행 후 아래 형식으로 결과를 보고한다.

```
## GC 보고서 — YYYY-MM-DD

### 삭제 후보
- 파일명: 이유 (미참조, 30일+ 미수정 등)

### 수정 필요
- 파일명: 불일치 내용 (HARNESS.md vs 실제 구조 등)

### 확인 필요
- 파일명: 마지막 수정일, 검토 이유

### 조치 불필요
- 확인했으나 현행 유지가 맞는 항목 목록
```

---

## GC 실행 주기 권장

| 상황 | 주기 |
|---|---|
| 기능 5개 이상 추가 후 | 즉시 |
| 정기 점검 | 월 1회 |
| 에이전트 응답이 느려진 경우 | 즉시 (하네스 경량화 병행) |

---

## 하네스 경량화 기준 (별도 판단)

GC 후 아래 기준으로 문서 경량화 여부를 판단한다.

- 두 파일에 같은 내용이 있으면 → 한쪽을 참조 링크로 교체
- docs/project/ 내 Historical Plans는 Git history로 대체 가능하면 삭제 검토
- tasks/current.md가 완료된 작업 내용만 있으면 → 다음 단계로 즉시 교체
- HARNESS.md가 500자를 크게 초과하면 → 상세 내용을 해당 docs/ 파일로 이동
