# Living Documentation and Evaluation Harness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 기준 문서, 완료 증거, 평가 정답 데이터, 생성물을 분리해 `docs/`를 프로젝트와 함께 갱신되는 구조로 정리한다.

**Architecture:** 제품·구조·결정·계획·완료 증거는 기존 목적별 디렉터리에 유지하고 중복 문서만 최종 기준에 병합한다. 평가 자료는 `docs/evaluation/reference-v1/` 아래에서 원본 데이터, manifest, 기준 결과, 실행 도구를 분리하며 대용량 검수본은 `build/reports/`에 생성한다.

**Tech Stack:** Markdown, Python 3 `unittest`, Gradle, JUnit 5, Java 21

**Spec:** `docs/superpowers/specs/2026-09-21-living-documentation-and-evaluation-harness-design.md`

## Global Constraints

- 검수 완료 JSON/JSONL의 내용과 SHA-256은 변경하지 않는다.
- 평가 알고리즘과 Phase 8 제품 동작은 변경하지 않는다.
- `docs/retrospectives/`는 로컬 보존하며 staging하지 않는다.
- 관련 없는 기존 사용자 변경을 복원하거나 정리하지 않는다.
- 생성 가능한 `questions.md`는 저장소가 아니라 `build/reports/evaluation/reference-v1/`에 둔다.
- 삭제·이동과 같은 작업에서 `docs/README.md`와 모든 참조 경로를 함께 갱신한다.

## Review Focus

- 새 데이터 경로에서 manifest 해시 검증이 동일한 원본 바이트를 검사해야 한다. Task 1의 전체 manifest 검사로 고정한다.
- `--render` 없이 검사할 때 추적된 `questions.md`가 없어도 성공해야 한다. Task 1의 CLI 테스트로 고정한다.
- `--render`는 소스 디렉터리를 오염시키지 않고 `build/reports/`에만 써야 한다. Task 1의 출력 경로 테스트로 고정한다.
- IDE에서 Java 벤치마크를 직접 실행해도 새 기본 경로를 사용해야 한다. Task 2의 기본 경로 assertion과 벤치마크 실행으로 고정한다.
- 삭제한 문서나 옛 평가 경로를 가리키는 링크가 남지 않아야 한다. Task 5의 전수 검색과 링크 검사로 고정한다.

---

## File Map

### 생성

- `docs/evaluation/reference-v1/README.md`: 평가 기준의 소유권, 구성, 실행법, 갱신 조건
- `docs/evaluation/reference-v1/data/*`: 검수 완료 원본 데이터
- `docs/evaluation/reference-v1/manifest.json`: 기준 버전, 해시, 사람 검수 기록
- `docs/evaluation/reference-v1/benchmarks/*`: 비교 가능한 기준·개선 결과
- `docs/evaluation/reference-v1/tools/bundle.py`: 무결성 검사, 실험 export, 파생 검수본 생성
- `docs/evaluation/reference-v1/tools/test_bundle.py`: 하네스 계약 회귀 테스트

### 수정

- `build.gradle`: 새 평가 디렉터리와 Python 도구 경로
- `src/test/java/com/example/crackcs/evaluation/retrieval/benchmark/RetrievalBenchmarkTest.java`: manifest와 data 하위 경로
- `docs/README.md`: 문서 목록, 상태, 갱신 계기
- `docs/planning/tasks.md`: 완료 이력 압축, 남은 작업 중심 재구성
- `docs/changes/2026-09-13-phase-6/verification.md`: 계획·리팩터링의 고유 정보 병합
- `docs/changes/2026-09-06-ui/verification.md`: UI 계획의 고유 정보 병합
- `docs/changes/2026-09-06-ui/proposal.md`: 삭제되는 프롬프트 링크 제거
- 새 위치를 참조하는 제품·ADR·변경 문서: 실제 검색 결과에 한정해 경로 수정

### 삭제

- `docs/content/2026-09-09-market-and-golden-set/`: 이동 후 빈 옛 디렉터리
- `docs/archive/functional-specification.md`
- `docs/changes/2026-09-13-phase-6/plan.md`
- `docs/changes/2026-09-13-phase-6/refactoring.md`
- `docs/changes/2026-09-06-ui/plan.md`
- `docs/changes/2026-09-06-ui/assets/prompts.md`
- `docs/changes/2026-09-06-ui/assets/implemented/detail-mobile-dark.jpg`
- 추적된 `docs/content/2026-09-09-market-and-golden-set/questions.md`

---

### Task 1: 평가 하네스를 버전화된 구조로 이동

**Files:**
- Create: `docs/evaluation/reference-v1/data/*`
- Create: `docs/evaluation/reference-v1/manifest.json`
- Create: `docs/evaluation/reference-v1/benchmarks/*`
- Create: `docs/evaluation/reference-v1/tools/bundle.py`
- Create: `docs/evaluation/reference-v1/tools/test_bundle.py`
- Create: `docs/evaluation/reference-v1/README.md`
- Delete: `docs/content/2026-09-09-market-and-golden-set/*`

**Interfaces:**
- Consumes: 검수 완료 JSON/JSONL 5종, 기준 manifest, 벤치마크 JSON 2종
- Produces: `bundle.DATA_DIRECTORY`, `bundle.RENDER_PATH`, `python3 .../tools/bundle.py [--render]`

- [ ] **Step 1: 새 생성물 경계를 나타내는 실패 테스트 작성**

`test_bundle.py`에 다음 계약을 추가한다.

```python
class RenderOutputTest(unittest.TestCase):
    def test_render_path_is_under_build_reports(self):
        expected = bundle.REPOSITORY_ROOT / "build/reports/evaluation/reference-v1/questions.md"

        self.assertEqual(bundle.RENDER_PATH, expected)
        self.assertFalse((bundle.REFERENCE_ROOT / "questions.md").exists())

    def test_cli_validation_does_not_require_rendered_markdown(self):
        result = subprocess.run(
            [sys.executable, str(bundle.BASE / "bundle.py")],
            check=True,
            capture_output=True,
            text=True,
        )

        self.assertEqual(json.loads(result.stdout)["status"], "PASS")
```

- [ ] **Step 2: 새 위치에서 테스트가 기존 경로 가정 때문에 실패하는지 확인**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s docs/evaluation/reference-v1/tools -p 'test_*.py'
```

Expected: `DATA_DIRECTORY`, `RENDER_PATH` 또는 새 경로가 아직 정의되지 않아 FAIL.

- [ ] **Step 3: 원본·manifest·benchmark·도구를 책임별 디렉터리로 이동**

이동 매핑:

```text
questions.jsonl             -> data/questions.jsonl
golden-set.jsonl            -> data/golden-set.jsonl
knowledge-documents.jsonl   -> data/knowledge-documents.jsonl
sources.json                -> data/sources.json
experiment-splits.json      -> data/experiment-splits.json
reference-standard.json     -> manifest.json
retrieval-baseline.json     -> benchmarks/retrieval-baseline.json
retrieval-improved.json     -> benchmarks/retrieval-improved.json
bundle.py                    -> tools/bundle.py
test_bundle.py               -> tools/test_bundle.py
report.md                    -> README.md로 압축
```

이동 전후 각 원본 파일의 `shasum -a 256` 결과를 비교한다. manifest의 `artifacts` 키는 파일명 그대로 유지한다.

- [ ] **Step 4: 도구의 경로 책임과 render 동작을 최소 수정**

`bundle.py`의 경로 상수를 다음 계약으로 바꾼다.

```python
BASE = Path(__file__).resolve().parent
REFERENCE_ROOT = BASE.parent
DATA_DIRECTORY = REFERENCE_ROOT / "data"
REPOSITORY_ROOT = REFERENCE_ROOT.parents[2]
RENDER_PATH = REPOSITORY_ROOT / "build/reports/evaluation/reference-v1/questions.md"


def read_jsonl(name):
    return [
        json.loads(line)
        for line in (DATA_DIRECTORY / name).read_text().splitlines()
        if line.strip()
    ]
```

manifest는 `REFERENCE_ROOT / "manifest.json"`, 데이터는 `DATA_DIRECTORY / name`에서 읽는다. `--render`일 때만 다음을 수행한다.

```python
RENDER_PATH.parent.mkdir(parents=True, exist_ok=True)
RENDER_PATH.write_text(expected)
```

검사 전용 실행에서는 생성물 존재·최신성을 요구하지 않는다. 로컬 링크 검사는 `README.md`만 대상으로 하고 새 상대 경로를 검사한다.

- [ ] **Step 5: 평가 README를 운영 책임 중심으로 작성**

다음 순서만 유지하고 과거 작업 일지는 제거한다.

```markdown
# 평가 정답 기준 v1

## 현재 상태
## 구성과 소유권
## 무결성 검사
## 파생 검수본 생성
## 오프라인 export
## retrieval benchmark
## 변경·재검수 조건
```

- [ ] **Step 6: Python 테스트와 무결성 검사를 GREEN으로 확인**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s docs/evaluation/reference-v1/tools -p 'test_*.py'
python3 docs/evaluation/reference-v1/tools/bundle.py
python3 docs/evaluation/reference-v1/tools/bundle.py --render
test -f build/reports/evaluation/reference-v1/questions.md
```

Expected: 모든 Python 테스트 성공, 검사 JSON의 `status`가 `PASS`, 생성 파일 존재.

- [ ] **Step 7: 평가 하네스 이동 커밋**

```bash
git add -A docs/evaluation docs/content/2026-09-09-market-and-golden-set
git commit -m "refactor: version evaluation reference harness"
```

`build/` 생성물은 staging하지 않는다.

---

### Task 2: Gradle과 Java 벤치마크를 새 계약에 연결

**Files:**
- Modify: `build.gradle`
- Modify: `src/test/java/com/example/crackcs/evaluation/retrieval/benchmark/RetrievalBenchmarkTest.java`

**Interfaces:**
- Consumes: Task 1의 `docs/evaluation/reference-v1/manifest.json`, `data/`, `tools/bundle.py`
- Produces: `verifyReferenceStandard`, `retrievalBenchmark`가 새 경로만 사용하는 빌드 계약

- [ ] **Step 1: Java 테스트의 기본 경로를 먼저 새 위치로 바꾸고 실패 확인**

```java
private final Path reference = Path.of(System.getProperty(
        "reference.directory", "docs/evaluation/reference-v1"));
private final Path referenceData = reference.resolve("data");
```

manifest는 `reference.resolve("manifest.json")`, 원본은 `referenceData.resolve(filename)`으로 읽도록 바꾼다.

Run:

```bash
./gradlew retrievalBenchmark --console=plain
```

Expected: Gradle이 아직 옛 Python 경로를 사용해 preflight 단계에서 FAIL.

- [ ] **Step 2: Gradle의 새 root와 도구 경로 적용**

```groovy
def referenceDirectory = layout.projectDirectory.dir('docs/evaluation/reference-v1')

tasks.register('verifyReferenceStandard', Exec) {
    commandLine 'python3', referenceDirectory.file('tools/bundle.py').asFile.absolutePath
}
```

`reference.directory`는 `referenceDirectory` root를 계속 전달한다.

- [ ] **Step 3: 벤치마크 GREEN과 결과 불변 확인**

Run:

```bash
./gradlew retrievalBenchmark --console=plain
```

Expected: 1개 benchmark 테스트 성공. 기존 기준 대비 `Recall@5`, `MRR`, `nDCG@5`, `zeroHitRate`의 예상치 못한 변화 없음.

- [ ] **Step 4: 경로 소비자 변경 커밋**

```bash
git add build.gradle src/test/java/com/example/crackcs/evaluation/retrieval/benchmark/RetrievalBenchmarkTest.java
git commit -m "test: load retrieval reference from versioned harness"
```

---

### Task 3: 완료 작업 문서를 최종 검증 증거로 압축

**Files:**
- Modify: `docs/changes/2026-09-13-phase-6/verification.md`
- Delete: `docs/changes/2026-09-13-phase-6/plan.md`
- Delete: `docs/changes/2026-09-13-phase-6/refactoring.md`
- Modify: `docs/changes/2026-09-06-ui/verification.md`
- Modify: `docs/changes/2026-09-06-ui/proposal.md`
- Delete: `docs/changes/2026-09-06-ui/plan.md`
- Delete: `docs/changes/2026-09-06-ui/assets/prompts.md`
- Delete: `docs/changes/2026-09-06-ui/assets/implemented/detail-mobile-dark.jpg`
- Delete: `docs/archive/functional-specification.md`

**Interfaces:**
- Consumes: 각 계획·리팩터링 문서의 현재 코드 이해에 필요한 고유 결정
- Produces: 완료 작업별 하나의 검증 문서와 현재 UI 제안서

- [ ] **Step 1: 삭제 대상의 고유 정보와 참조를 목록화**

Run:

```bash
rg -n "phase-6/(plan|refactoring)|2026-09-06-ui/plan|assets/prompts|functional-specification|detail-mobile-dark" \
  docs build.gradle src
```

Expected: 현재 참조 목록을 보관하고, 병합할 항목을 `verification.md`별로 구분.

- [ ] **Step 2: Phase 6 최종 증거로 병합**

`verification.md`에 다음 책임만 남긴다.

```markdown
## 구현 범위
## 유지되는 계약과 설계 결정
## 패키지·트랜잭션·테스트 책임
## 실행한 검증
## 남은 경계
```

코드에 존재하지 않는 예정형 표현을 제거하고, `plan.md`와 `refactoring.md`의 고유 결정만 위 항목에 병합한 뒤 두 파일을 삭제한다.

- [ ] **Step 3: UI 문서를 제안과 검증 두 책임으로 정리**

- `proposal.md`: 사용자 경험과 시각 방향
- `verification.md`: 실제 구현 범위, 반응형·테마 검증, 미구현 경계

프롬프트 링크와 오래된 계획 링크를 제거하고, 사용되지 않는 프롬프트·이미지·계획 파일을 삭제한다.

- [ ] **Step 4: 대체된 초기 명세 제거**

`archive/functional-specification.md`의 현재 유효한 요구가 `product/spec.md`에 존재하는지 제목·핵심 키워드로 대조한 뒤 삭제한다. Git 이력이 역사 보존 수단이다.

- [ ] **Step 5: 삭제 참조가 0건인지 확인**

Run:

```bash
rg -n "phase-6/(plan|refactoring)|2026-09-06-ui/plan|assets/prompts|functional-specification|detail-mobile-dark" docs
```

Expected: 0건.

- [ ] **Step 6: 완료 증거 압축 커밋**

```bash
git add docs/changes docs/archive
git commit -m "docs: consolidate completed change evidence"
```

---

### Task 4: 현재 계획과 문서 색인을 살아있는 기준으로 압축

**Files:**
- Modify: `docs/planning/tasks.md`
- Modify: `docs/README.md`
- Modify: 검색으로 확인된 평가 경로 참조 문서

**Interfaces:**
- Consumes: 새 평가 README, 유지되는 verification, 현재 제품 명세
- Produces: 남은 작업 중심 계획과 모든 기준 문서의 탐색 색인

- [ ] **Step 1: tasks의 완료 이력과 미완료 작업을 분류**

Run:

```bash
rg -n '^#{2,4} |^- \[[ x]\]' docs/planning/tasks.md
```

다음 형태로 책임을 나눈다.

```markdown
# 구현 작업

## 현재 상태
## 완료 Phase 요약
| Phase | 결과 | 검증 근거 |
## 다음 작업
## Phase 8
## 출시 전 공통 조건
```

완료 Phase의 세부 체크리스트는 결과 한 줄과 verification 링크로 교체한다. 아직 구현하지 않은 acceptance criteria와 의존성은 유지한다.

- [ ] **Step 2: docs README를 짧은 탐색 색인으로 재작성**

각 행은 다음 네 필드만 갖는다.

```markdown
| 답할 질문 | 상태 | 기준 문서 | 갱신 계기 |
```

삭제된 문서·생성된 `questions.md` 직접 링크를 제거한다. 평가 항목은 `evaluation/reference-v1/README.md`와 manifest, 도구 테스트를 가리킨다.

- [ ] **Step 3: 새 평가 경로를 문서 전체에 반영**

Run:

```bash
rg -n "docs/content/2026-09-09-market-and-golden-set|content/2026-09-09-market-and-golden-set" docs build.gradle src
```

모든 실제 참조를 `docs/evaluation/reference-v1` 구조에 맞게 수정한다.

- [ ] **Step 4: 압축 결과를 수치로 확인**

Run:

```bash
wc -lc docs/README.md docs/planning/tasks.md docs/evaluation/reference-v1/README.md
```

Expected: 세 문서가 각각 한 책임만 가지며, `tasks.md`와 평가 README가 기존 1,066줄·375줄보다 명확하게 감소.

- [ ] **Step 5: 현재 문서 색인 커밋**

```bash
git add docs/README.md docs/planning/tasks.md docs/evaluation/reference-v1/README.md
git commit -m "docs: make project documentation current and navigable"
```

기존 사용자 변경이 있는 파일은 이번 작업에서 실제로 수정한 hunk만 검토 후 staging한다.

---

### Task 5: 전체 참조와 실행 가능성 검증

**Files:**
- Modify: 검증에서 발견된 잘못된 링크 또는 경로만 수정

**Interfaces:**
- Consumes: Tasks 1–4의 최종 트리
- Produces: 링크·하네스·벤치마크·전체 테스트 증거

- [ ] **Step 1: 옛 경로와 삭제 대상 참조 전수 검사**

Run:

```bash
rg -n "docs/content/2026-09-09-market-and-golden-set|content/2026-09-09-market-and-golden-set|questions\.md|phase-6/(plan|refactoring)|assets/prompts|functional-specification|detail-mobile-dark" \
  docs build.gradle src
```

Expected: `questions.md`는 생성 명령·출력 경로 설명에서만 등장하고 그 외 옛 참조는 0건.

- [ ] **Step 2: 로컬 Markdown 링크 검사**

저장소 내 Markdown 파일의 상대 링크를 해석하는 기존 검사 또는 짧은 읽기 전용 검사 명령을 실행한다. HTTP 링크와 anchor는 파일 존재 검사에서 제외한다.

Expected: 깨진 로컬 파일 링크 0건.

- [ ] **Step 3: Python 하네스 최종 검증**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s docs/evaluation/reference-v1/tools -p 'test_*.py'
python3 docs/evaluation/reference-v1/tools/bundle.py
python3 docs/evaluation/reference-v1/tools/bundle.py --render
```

Expected: 전체 성공, 무결성 `PASS`, 파생 출력 생성.

- [ ] **Step 4: 관련 Gradle 검증**

Run:

```bash
./gradlew retrievalBenchmark --console=plain
```

Expected: benchmark 1개 성공.

- [ ] **Step 5: 전체 테스트**

Run:

```bash
./gradlew test --console=plain
```

Expected: 전체 성공. 외부 PostgreSQL 태그 테스트는 기본 `test` 범위에서 제외됨을 보고한다.

- [ ] **Step 6: staging 안전성 확인**

Run:

```bash
git status --short
git diff --check
git diff --cached --name-only
```

Expected: `docs/retrospectives/`, `.DS_Store`, `build/`, `tobyteam/`이 staging에 없음. 관련 없는 사용자 변경이 커밋에 없음.

- [ ] **Step 7: 검증 수정과 최종 문서 커밋**

```bash
git add docs/README.md docs/evaluation/reference-v1/README.md build.gradle \
  src/test/java/com/example/crackcs/evaluation/retrieval/benchmark/RetrievalBenchmarkTest.java
git commit -m "docs: verify living documentation references"
```

최종 보고에 변경 구조, 삭제·이동 목록, 테스트 수와 결과, 미검증 경계, 실패 조건을 포함한다.
