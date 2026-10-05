# OS 독립 답변 검수

- 독자·질문: 검수자·평가 작업자 / 모델 결과를 보기 전에 기대 판정을 고정하고 실패까지 포함해 비교하는 방법
- 상태: 검수 자료·동결·비교 도구 구현. 이번 사용자 위임 진단과 독립 사람 검수 구분
- 도구: [independent_review.py](../../../scripts/independent_review.py)
- 현재 실행·표본 출처: [검증 기록](../../changes/2026-10-05-learner-review/verification.md)
- 실제 원문·회원 정보·사람 라벨: Git 제외 `.firecrawl/learner-review/packet/`에 로컬 보관

```text
원문·문항·개념 → 모델 결과 없는 읽기 사본 → 사람 판정·이유
                                              ↓ 실제 출처·독립 검수 확인
입력 SHA-256 + 라벨 SHA-256 ← 동결 ←────────────┘
                  ↓ 동결 이후 새 모델 실행
        종합·개념 판정 비교 + 실패·누락·표본 유형 보고
```

## 상태 소유자와 한계

- inputs.json: 원문, 당시 문항 ID·버전·기준 답안·개념, 수집 범위·한계
- labels.json: 사람이 작성한 판정·이유·유형, 작성자·시간·출처 확인·사전 독립 검수 확인
- frozen.json: 두 파일의 정규화 JSON SHA-256와 동결 시각
- 관측 결과: 동결 이후 실행한 terminal 상태·모델·평가기 버전·근거 수·개념 판정
- comparison.json: 기술 지표. `DESCRIPTIVE_ONLY`; 표본 대표성·출시 승인 자동 판정 없음
- 확인란: 검수자의 사실 확인 주장. 해시로 사람이 실제 작성했는지, 과거 결과를 보지 않았는지 증명 불가
- 기존 모델 결과를 이미 본 대리 검수·시연 답변: 독립 사람 표본으로 동결 금지
- 같은 사용자·같은 개념의 기본·후속 답변: 독립 관측으로 일반화 금지

## 실행

Python 3 표준 라이브러리, 저장소 루트. 기존 파일·판정 덮어쓰기 거부.

```bash
mkdir -p .firecrawl/learner-review
# 기본 DB Answer 1~4만 선택하는 읽기 전용 SQL. 다른 표본은 ID·수집 범위 함께 수정
docker exec -i crackcs-local-postgres-1 psql -X -qAt -v ON_ERROR_STOP=1 \
  -U crackcs_local -d crackcs_local < scripts/export_review_samples.sql \
  > .firecrawl/learner-review/samples.json

python3 scripts/independent_review.py prepare \
  --samples .firecrawl/learner-review/samples.json \
  --output .firecrawl/learner-review/packet

# 같은 cmux workspace에 브라우저 표시. 실제 사람만 판정 입력
python3 scripts/independent_review.py serve \
  --inputs .firecrawl/learner-review/packet/inputs.json \
  --output .firecrawl/learner-review/packet --port 18084

# 파일로 판정한 경우
python3 scripts/independent_review.py freeze \
  --inputs .firecrawl/learner-review/packet/inputs.json \
  --labels .firecrawl/learner-review/packet/labels.json \
  --output .firecrawl/learner-review/packet/frozen.json

python3 scripts/independent_review.py compare \
  --inputs .firecrawl/learner-review/packet/inputs.json \
  --labels .firecrawl/learner-review/packet/labels-final.json \
  --frozen .firecrawl/learner-review/packet/frozen.json \
  --results .firecrawl/learner-review/results.json \
  --output .firecrawl/learner-review/comparison.json
```

- 실행 전 출력 폴더 생성. SQL은 평가·피드백·이름·이메일 제외, 기본 DB만 조회
- `prepare`: 원문을 변경하지 않고 기존 모델 결과 제거. `DRAFT` 라벨 생성
- `serve`: loopback 전용, Host·Origin·nonce 확인. 빈 판정·확인란 기본값, 완료 시 라벨·동결 저장
- `freeze`: 누락 라벨·개념·이유·유형·출처·검수자·시간·독립 검수 확인 거부
- `compare`: 동결 이후 수정·과거 관측·중복·모르는 case·다른 모델/평가기 혼합 거부
- 결과 누락·FAILED: 기대 판정 분모에 유지. 분모 0인 지표는 null
- `NEEDS_REVIEW` 기대값: 삼종·이진·false-correct 지표와 별도 집계
- 모델·유료 호출·앱 상태 변경: 도구에서 수행하지 않음. 실제 실행은 기존 앱과 검증 경로 사용

관측 결과 한 건의 형식:

```json
{
  "caseId": "CASE-ID", "status": "EVALUATED", "verdict": "CORRECT",
  "observedAt": "2026-10-05T10:00:00+00:00",
  "concepts": {"CONCEPT_CODE": "CORRECT"},
  "modelName": "recorded-provider-model", "evaluatorVersion": "recorded-version",
  "evidenceCount": 1
}
```

## 검증과 다음 순서

```bash
python3 -m unittest discover -s scripts -p 'test_independent_review.py' -v
bash scripts/verify.sh all
```

- 이번 4개: 사용자 선택 과거 시연 이력, 대리 진단만 진행. 독립 검수 완료·대표 표본 Gate 표시 금지
- 다음 수집: 실제 학습자의 짧은 정답·부정확한 용어·혼합 오류. GPT 결과를 보지 않은 검수자가 개념별 판정
- 순차 작업: 대리 진단 → 통제 장애 cmux 검증 → 사용자 위임한 다음 Topic 검수·로컬 공개
- 파일럿 조건: 실제 독립·대표 표본 검증은 별도로 유지
