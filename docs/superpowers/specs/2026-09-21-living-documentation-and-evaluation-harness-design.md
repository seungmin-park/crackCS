# 살아있는 문서와 평가 하네스 구조 설계

## 상태

- 승인: 2026-09-21 사용자 선택안 2
- 범위: `docs/`, 평가 자료 경로를 참조하는 Gradle·테스트
- 제외: 평가 알고리즘, 제품 기능, 검수 완료 데이터 내용 변경

## 문제

현재 `docs/`에는 수명이 다른 자료가 같은 수준에 놓여 있다.

```text
현재 제품 기준 + 완료된 계획 + 작업 과정 + 평가 원본 + 생성물 + 실행 결과
                                  ↓
                   무엇이 현재 기준인지 즉시 판단하기 어려움
```

대표 문제:

- `planning/tasks.md`: 완료 이력과 남은 작업 혼재
- 평가 `report.md`: 운영법·조사·변경 이력·벤치마크 결과 혼재
- `questions.md`: JSONL에서 다시 만들 수 있는 대용량 파생 출력
- Phase 6: 계획·리팩터링·검증 문서의 책임 중복
- `archive/functional-specification.md`: 현재 제품 명세로 대체됨
- `docs/content/...`: 평가 데이터와 하네스라는 실제 책임을 드러내지 못하는 경로

## 설계 목표

- 현재 기준 문서와 역사적 증거의 명확한 구분
- 주제별 단일 기준 문서 유지
- 평가 정답 데이터와 실행 도구의 재현 가능한 공동 버전 관리
- 생성 가능한 큰 문서의 저장소 추적 제거
- 코드·데이터·문서 변경 시 함께 실패하는 자동 검증 유지
- 완료 작업의 유효한 검증 증거 보존

## 비목표

- 모든 과거 기록 제거
- 검수 완료 JSONL 데이터 재작성
- Phase 8 기능 구현
- 원격 AI API 연동
- 회고 문서 공개 또는 staging

## 적용 원칙

### 항상 읽는 지침

- 루트 `AGENTS.md`: 저장소 공통 규칙만 유지
- 특정 영역의 상세 절차: 필요한 경우 가까운 하위 지침으로 분리
- 같은 규칙을 여러 문서에 복제하지 않음

Codex는 루트에서 작업 위치까지 지침을 합성하며 기본 프로젝트 문서 한도가 있다. Claude Code도 짧고 구체적인 프로젝트 지침과 범위별 규칙을 권장한다. 따라서 일반 설명과 역사 기록을 에이전트 지침에 넣지 않는다.

### 현재 기준

- 제품 요구: `docs/product/`
- 현재 구조: `docs/architecture/`
- 결정과 이유: `docs/adr/`
- 앞으로 할 일: `docs/planning/`
- 전체 탐색과 상태: `docs/README.md`

### 완료 증거

- `docs/changes/`: 완료 작업의 최종 범위·핵심 결정·실행 명령·검증 결과
- 작업 전 계획과 중간 리팩터링 메모: 최종 검증에 고유 정보만 병합
- 단순 시간순 작업 일지: Git 이력으로 보존

### 평가 하네스

```text
검수된 원본(data) ──┐
manifest ───────────┼─> 검사 도구 ─> 벤치마크 ─> 결과
실행 설정 ──────────┘                    │
                                        └─> build/reports의 파생 검수본
```

- 검수된 JSON/JSONL: 저장소 추적
- 기준 버전·해시·검수 주체: manifest에 기록
- 벤치마크 기준 결과: 비교에 필요한 메타데이터와 함께 추적
- 실행 도구와 회귀 테스트: 데이터와 같은 버전으로 추적
- 사람이 읽는 대용량 파생본: 필요 시 `build/reports/`에 생성

## 목표 디렉터리

```text
docs/
├── README.md
├── product/
├── architecture/
├── adr/
├── planning/
├── changes/
├── evaluation/
│   └── reference-v1/
│       ├── README.md
│       ├── data/
│       │   ├── questions.jsonl
│       │   ├── golden-set.jsonl
│       │   ├── knowledge-documents.jsonl
│       │   ├── sources.json
│       │   └── experiment-splits.json
│       ├── manifest.json
│       ├── benchmarks/
│       │   ├── retrieval-baseline.json
│       │   └── retrieval-improved.json
│       └── tools/
│           ├── bundle.py
│           └── test_bundle.py
├── superpowers/specs/
└── retrospectives/                 # 로컬 전용, staging 제외
```

`reference-v1`은 데이터 계약의 명시적 버전이다. 내용 또는 판정 기준을 호환되지 않게 바꾸면 기존 자료를 덮어쓰지 않고 새 버전을 검토한다.

## 파일별 처리

| 현재 대상 | 처리 | 이유 |
|---|---|---|
| `docs/content/2026-09-09-market-and-golden-set/` | `docs/evaluation/reference-v1/`로 재구성 | 날짜·작성 행위보다 운영 책임과 계약 버전 표현 |
| `questions.md` | 추적 제거, `build/reports/evaluation/reference-v1/questions.md` 생성 | 원본에서 결정적으로 재생성 가능 |
| 평가 `report.md` | 간결한 `README.md`로 압축 | 사용법·소유권·갱신 조건 중심 |
| Phase 6 `plan.md`, `refactoring.md` | 고유 정보를 `verification.md`에 병합 후 삭제 | 완료 작업의 중복 제거 |
| UI `plan.md` | 고유 정보를 `verification.md`에 병합 후 삭제 | 계획보다 최종 검증이 현재 가치 보유 |
| UI `assets/prompts.md` | 삭제 | 제품 기준이 아닌 생성 과정 기록 |
| 미참조 `detail-mobile-dark.jpg` | 삭제 | 현재 문서에서 사용되지 않는 산출물 |
| `archive/functional-specification.md` | 삭제 | `product/spec.md`가 현재 기준, 이력은 Git 보존 |
| `planning/tasks.md` | 남은 작업 중심으로 압축 | 작업 목록의 현재성 회복 |

## 책임과 변경 방향

### 상태 소유

- 평가 정답 상태: `data/`와 `manifest.json`
- 평가 자료의 구조 규칙: `tools/bundle.py`
- 빌드에서의 실행 경로: `build.gradle`
- retrieval 실행 fixture 경로: benchmark 테스트
- 문서 탐색 정보: `docs/README.md`

경로 문자열을 아는 객체와 문서는 새 위치로 함께 이동한다. 경로만 바꾸고 검사 도구 또는 테스트를 남겨 두지 않는다.

### 생성물 경계

`bundle.py --render`는 소스 디렉터리에 쓰지 않고 빌드 출력 경로에 쓴다. 기본 검사에서는 원본 자료와 manifest만 검증한다. 생성물 최신성 검사는 “저장소 파일과 비교”가 아니라 “같은 입력에서 정상 생성되는지”로 바꾼다.

### 문서 압축 기준

- 완료 상태: 한 줄 결과와 검증 문서 링크
- 현재 할 일: 완료 조건·의존성·다음 행동 유지
- 반복 배경: 기준 문서 링크로 대체
- 명령: 실제 실행 가능한 형태만 유지
- 오래된 경로와 예정 상태: 제거 또는 현재 상태로 수정

## 검증 전략

```text
경로 변경
   ↓
문서·Gradle·테스트 참조 전수 검색
   ↓
Python 하네스 단위 테스트
   ↓
자료 무결성 검사 및 파생본 생성
   ↓
retrieval benchmark
   ↓
전체 Gradle 테스트
   ↓
문서 링크·고아 파일 검사
```

필수 검증:

- 옛 `docs/content/2026-09-09-market-and-golden-set` 참조 0건
- 삭제 대상 참조 0건
- 로컬 Markdown 링크 오류 0건
- `test_bundle.py` 전체 성공
- `bundle.py` 무결성 검사 성공
- 파생 `questions.md`가 `build/reports/`에 생성
- retrieval benchmark 성공
- 가능한 범위의 전체 Gradle 테스트 성공
- `docs/retrospectives/` staging 제외 확인

## 실패 조건과 대응

- 검수 데이터의 해시 불일치: 이동 중 내용 변형 여부 확인, 임의 재승인 금지
- 경로 참조 누락: `rg` 결과가 0이 될 때까지 소비자 수정
- 파생본 생성 실패: 출력 디렉터리 생성 책임을 도구가 소유하도록 수정
- 벤치마크 결과 변화: 경로 변경 외 동작 변화 여부를 조사하고 기준값 자동 갱신 금지
- 전체 테스트 환경 실패: 관련 테스트 결과와 환경 경계를 분리 보고

## 근거

- OpenAI Codex: `AGENTS.md` 계층과 프로젝트 지침 크기 제한
- Anthropic Claude Code: 간결한 프로젝트 메모리와 범위별 규칙
- OpenAI Evaluations: 대표 데이터·사람 기준·지속 평가
- LM Evaluation Harness: 태스크·설정·샘플·결과의 재현 가능한 분리
- Google Documentation Guide: 디렉터리 README의 짧은 탐색 책임
- Diátaxis: 독자의 목적에 따른 문서 책임 분리
