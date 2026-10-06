# A1 서버 기본 지식 콘텐츠

- 독자·질문: 콘텐츠 제작자·검수자 / DB·SQL·트랜잭션·HTTP·API·테스트의 첫 문항을 무엇으로 검수할 것인가?
- 상태: **DRAFT 초안 제작**. 관리자 등록·사람 검수·공개·실제 모델 실행 미실행
- 기준: [확장 커리큘럼](../backend-curriculum/README.md), [콘텐츠 정책](../../product/content-and-ai-policy.md)
- 수정 원본: [bundle.json](bundle.json). 읽기 사본: [review.md](review.md). 실제 검사: [verification.md](verification.md)
- 기존 OS·Java 승인 이력과 Spring Framework·Boot·JPA 검수 대기본은 [initial-v1](../initial-v1/README.md)에서 유지

## 이번 묶음의 범위

| Topic | 문항·개념·문서 | 답변에서 확인할 직관 |
|---|---|---|
| `DATABASE` | 5·12·1 | 행 식별·업무 중복·NULL·참조 수명·현재 사실과 거래 이력 |
| `SQL_AND_INDEXES` | 5·12·1 | LEFT JOIN 필터·NULL 비교·순서·B-tree 비용·팬아웃 집계 |
| `TRANSACTIONS_AND_CONCURRENCY` | 5·10·1 | 참여 자원·snapshot·덮어쓰기 경쟁·전체 재시도·부재 행 잠금 |
| `HTTP` | 5·10·1 | 효과의 멱등성·안전성·캐시·사전 조건·오류·redirect |
| `API_DESIGN` | 5·10·1 | REST 제약·업무 전이·멱등 키·cursor·소비자 호환 계약 |
| `TESTING` | 5·10·1 | 검증 수준·double·서버 transaction·조건 대기·증거의 범위 |
| 합계 | **30·64·6** | BASIC 6·INTERMEDIATE 16·ADVANCED 8 |

주제당5문항은 제작·검수의 첫 묶음. DB의 RDB/NoSQL 선택, SQL의 윈도 함수, HTTP 쿠키, 부하·concurrency 테스트 등 커리큘럼 전체 범위의 완성은 아님. 전체28개 Topic 중 기존5개·A1 신규6개·[A2 신규6개](../a2-v1/README.md)에 초안이 있고, B·C의 신규11개와 선택2개는 콘텐츠 미제작. 승인·공개 상태는 별도.

```text
DB       어떤 상태를 저장해도 되는가?
  ↓
SQL      어떤 행을 얻으며 얼마를 읽는가?
  ↓
DB tx    동시에 실행·실패하면 무엇이 남는가?

HTTP → API 계약 → 실제 업무 결과
          ↑
테스트: 어느 경계에서 무엇을 실제로 확인했는가?
```

DB 제약의 업무 유일성과 API의 요청 중복 계약, HTTP 멱등 의미와 DB 전체 재시도는 관련되지만 동일한 평가 개념을 복제한 것이 아님. 문항의 평가 대상과 판단 근거를 해당 Topic 안에 배치. 다른 Topic의 Concept를 한 문항에 연결하지 않음. 향후 Concept 재사용·범위 변경은 기준 소유자를 먼저 검토.

## 원본·근거·진단 사례의 책임

- `bundle.json`: Topic·Concept·문항·근거 문서·가중치·출처·작성자 기대 판정의 단일 원본
- `review.md`: 기존 renderer가 생성하는 읽기 사본. 직접 수정 대신 원본 수정·재생성
- `postgres-examples.sql`: DB·SQL의17개 명시적 assertion. transaction 경쟁·앱 저장·모델 품질 검사 아님
- 출처22개: PostgreSQL 17 세부 문서, Berkeley CS186, BCcampus 공개 교재, RFC 9110/9111, Fielding, OpenAPI3.1.0, Spring7.0/Boot4.1/JUnit6.1.3. 본문 hash·확인일·절·URL은 원본의 `sources`·`sourceRefs`에서 관리
- 출처를 여러 개 표기하는 것만으로 모든 Concept의 근거 확보를 확정하지 않음. API의 멱등 키 저장·cursor·호환 이관은 **문항에서 정한 자체 계약에 대한 설계 제안**. 표준의 유일한 구현처럼 채점하지 않음
- DB 교재 교차 확인은 함수 종속성·갱신 이상·정규화 목적에 사용. 정규형을 단일 열 PK와 동일시하는 단순화·상품 가격과 주문 당시 가격 혼동 배제
- JUnit6.1.3은 참조 문서 버전. 저장소 build/lockfile·실제 설치 버전 변경 없음

문항당5개, 총150개의 진단 사례:

| 사례 | 입력·작성자 예상 판정 |
|---|---|
| 정답 | 모범 답안·모든 필수 개념 CORRECT |
| 다른 표현·대안 | 필수 조건은 동일, 문구·허용 해법이 달라도 CORRECT |
| 일부 누락 | 마지막 개념 PARTIALLY_CORRECT, 나머지는 CORRECT. 전체 PARTIALLY_CORRECT |
| 핵심 모순 | 각 개념의 핵심을 뒤집는 답변·전체 INCORRECT |
| 근거 누락 | 정답 사례와 **같은 답변**, 필수 근거는 모두 미제공. 작성자 기대 NEEDS_REVIEW |

개념별 rubric은 충족·일부 누락·모순·판정 보류 경계. 예상 판정은 작성자가 만든 개발 진단 값이며 사람 검수·실제 모델 결과가 아님. 현재 앱은 `reviewNotes`·`diagnosticCases`를 평가 입력으로 자동 실행하지 않음. 기존 importer는 문항·가중치·근거 문서를 DRAFT로 등록하며 진단·rubric 상세는 이 원본의 검수 자료. 모델 진단 runner 연결은 후속 작업.

`reference-v1`과 정확히 같은 문장은 없으나 `A1-SQL-04`와 `CS-07`, `A1-TX-02`와 `CS-06`은 의미·개념 중첩. 텍스트 유사도 최대0.600은 기계적 비교일 뿐 독립성 판정 아님. 이 묶음·작성자 답변을 독립 golden·대표 학습자 표본으로 재사용 금지. 기존 자료를 새 콘텐츠의 평가 정답으로 복사하지 않았더라도 같은 원리를 다루면 독립 표본 선정에서 제외·분리.

## 구조 검사·읽기 사본 재생성

저장소 루트에서 실행:

```bash
python3 scripts/content_bundle.py --bundle docs/content/a1-v1/bundle.json --report build/reports/a1-v1/bundle.json
python3 scripts/render_content_review.py --bundle docs/content/a1-v1/bundle.json
python3 -m unittest scripts/test_render_content_review.py -v
```

- 기본 명령 `python3 scripts/render_content_review.py`는 기존 initial-v1 유지
- `--output` 생략 시 선택한 묶음 옆에 `review.md` 생성. 원본과 같은 출력 경로는 거부
- 구조 검사는 문항 수·중복 문장·동일 Topic 연결·문서의 Concept 덮임·출처 식별·가중치 확인. 사실 정확성·의미 중복·진단 기대값·검색·모델 품질·검수 승인을 대신하지 않음
- 등록 도구는 `--apply` 없으면 읽기·보고서 생성만 수행. 이번 작업에서 `--apply` 미사용

## 사람 검수·공개 순서

1. [review.md](review.md)에서 문제 전제·모범 답안·허용 대안·필수 개념·판정 경계 확인
2. 각 출처 절에서 버전·사실·권리 조건 대조. 문서가 검색 chunk로 나뉘어도 필요한 근거가 함께 제공되는지 확인
3. 150개 작성자 기대값을 사람 기준으로 수정·승인. 부분 답안의 일부 정답을 전체 오답에 맞춰 덮어쓰지 않음
4. 출시 대상으로 선택할 Topic와 노출 범위 기록. 활성 taxonomy 등록도 지식 지도에 영향을 줄 수 있으므로 공개 범위와 함께 판단
5. 기존 관리자 경로로 DRAFT 등록·사람 검수·공개. 기존 승인 콘텐츠·과거 평가 버전 보존
6. 독립 대표 답변·실제 모델·검색·후속 학습을 실행해 품질 Gate 확인. 로컬 E2E는 현재 cmux에서 표시

검수 승인 기록: **미확정**. 검수자·일시·원본 hash·승인/수정/보류 결과를 실제 승인 이후 기록. 이 파일 작성·CI 통과·PR 머지는 콘텐츠 공개 승인 아님.

## 배포·운영까지 남은 관계

```text
이번 결과: A1 콘텐츠 초안 + SQL 사실 확인 + 검수 도구
                        ↓
선택 출시 범위·사람 검수 → 등록·공개 → 검색·실제 모델·독립 표본
                        ↓
운영 환경·schema 관리·배포/복구·관측 → 참가자 파일럿 → 출시 판단
```

운영 준비는 콘텐츠 검수와 병행 가능. 전체28개 제작을 운영 환경 준비의 자동 선행 조건으로 추가하지 않음. 기존3개 Topic 검수·A1/A2 승인·B/C 제작·운영 상태는 [tasks.md](../../planning/tasks.md)가 관리.
