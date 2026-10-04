# 초기 콘텐츠와 rollback 준비 기준

## 상태

- 구현·자동 검증 완료: 문서/문제 버전 보존, 폐기 콘텐츠 검색 제외, 기존 Evidence 참조 구조
- 결정 완료: OQ-006 최소 공개량
- 초안 준비·등록 완료: [initial-v1](../../content/initial-v1/README.md), 25문항·50개 Concept·5문서
- 운영체제: 사람 검수·이번 로컬 앱 공개 승인, 5문항·10개 Concept·문서 v2·Chunk 7개 공개. v1·기존 Chunk 보존, 실제 GPT 학습·AC-007 로컬 검증 완료. [승인·실행 기록](../2026-10-05-os-content/verification.md) 참조
- 미완료: 나머지 4개 Topic의 사람 전수 검수·승인, 전체 파일럿 승인

## OQ-006 공개 최소량

각 leaf Topic은 아래 조건을 모두 충족해야 파일럿 대상이 된다.

- `PUBLISHED` 기본 문제 5개 이상
- leaf Topic의 모든 필수 Concept을 근거로 덮는 `PUBLISHED` KnowledgeDocument 1개 이상
- 각 문제의 필수 Concept과 reference answer 검수 완료
- 관련 문서의 출처·라이선스·검수 메타데이터 완료

상위 Topic의 합계로 leaf Topic 부족분을 대신하지 않는다. 문서 한 개가 여러 Concept을 덮을 수 있지만, 각 필수 Concept의 근거 위치를 추적할 수 있어야 한다.

## 공개 전 metadata

| 대상 | 필수 값 |
|---|---|
| 버전 의존 문서 | Java 21, Spring Boot 4.1.x, Spring Framework 7.0.x, Jakarta Persistence 3.2 중 해당 label |
| 모든 문서 | source URL 또는 문헌 locator, license note, reviewer, review date |
| 모든 문제 | 필수 Concept, 가중치, reference answer, reviewer, review date |
| reference 비교 | reference-v1 질문·표현·근거와 중복/편향 점검 결과 |

reviewer와 review date가 현재 엔티티에 영속 필드로 모두 존재한다는 뜻은 아니다. 영속 모델이 지원하지 않는 값은 공개 전 검수 기록에 남기며, 추적 가능한 저장 위치가 마련되기 전에는 해당 콘텐츠를 파일럿 승인하지 않는다.

## 잘못 공개한 문서 rollback

```text
잘못된 PUBLISHED v2 발견
        ↓
v2 RETIRED ── 새 retrieval에서 제외
        ↓
v1을 덮어쓰지 않음
        ↓
정정본 v3 DRAFT → REVIEWED → PUBLISHED

과거 EvaluationEvidence ── 기존 v1/v2 Chunk ID 계속 참조
새 Evaluation           ── 현재 PUBLISHED Chunk만 검색
```

- 기존 행과 Chunk 삭제 금지
- 공개 문서 본문 직접 수정 금지
- 이전 문서를 되살려 덮어쓰기보다 정정된 새 버전 발행
- 문제 오류도 같은 원칙으로 이전 공개본 `RETIRED`, 새 DRAFT 버전 검수 후 공개
- 과거 판정 자체를 자동 재작성하지 않음. 사용자 영향이 있으면 별도 재평가 결정과 감사 기록 필요

## 자동 검증 근거

```bash
./gradlew test \
  --tests '*KnowledgeDocumentTest' \
  --tests '*KnowledgeDocumentServiceTest' \
  --tests '*KnowledgeChunkServiceTest' \
  --tests '*KnowledgeRetrievalServiceTest' \
  --tests '*AdminContentFlowTest' \
  --tests '*QuestionServiceTest' \
  --tests '*KnowledgeAnswerSerializationTest' \
  --console=plain
```

| 보장 | 테스트 근거 |
|---|---|
| 공개 문서는 직접 수정하지 않고 다음 DRAFT 버전 생성 | `KnowledgeDocumentTest`, `KnowledgeDocumentServiceTest` |
| 다음 버전 공개 시 이전 공개본 보존 | `AdminContentFlowTest`, `QuestionServiceTest` |
| 같은 공개 문서의 기존 Chunk 보존 | `KnowledgeChunkServiceTest` |
| RETIRED 문서 Chunk는 새 retrieval에서 제외 | `KnowledgeRetrievalServiceTest` |
| 과거 응답에서 Evidence와 문서 버전 직렬화 | `KnowledgeAnswerSerializationTest` |

## 실제 콘텐츠 승인표

| leaf Topic | 공개 문제 ≥5 | Concept 근거 100% | 버전 label | 출처·license | 검수자·일자 | reference 중복 점검 | 승인 |
|---|---:|---:|---|---|---|---|---|
| 운영체제 | 로컬 공개 5 | 10/10 대응·문단 생성 | OSTEP·xv6 rev5 | 원문 대조·이용 메모 승인 | 프로젝트 소유자, 2026-10-05 | 공유 개념·문항 의미 점검 | 이번 로컬 공개 승인 |
| Java | 0 / 초안 5 | 초안 10개 대응 | Java 21 | 링크·메모 있음, 검수 대기 | 미검수 | 공유 개념 기록 | 보류 |
| Spring Framework | 0 / 초안 5 | 초안 10개 대응 | 7.0.x | 링크·메모 있음, 검수 대기 | 미검수 | 공유 개념 기록 | 보류 |
| Spring Boot | 0 / 초안 5 | 초안 10개 대응 | 4.1.x | 링크·메모 있음, 검수 대기 | 미검수 | 공유 개념 기록 | 보류 |
| JPA | 0 / 초안 5 | 초안 10개 대응 | 3.2 | 링크·메모 있음, 검수 대기 | 미검수 | 공유 개념 기록 | 보류 |

위 수치는 initial-v1 범위. 기존 화면 확인용 공개 seed는 승인된 초기 콘텐츠로 계산하지 않음. [등록·재등록·비공개 검증](../2026-09-27-ui-content/verification.md), [중복·편향 기록](../../content/initial-v1/README.md#reference-v1과의-관계) 참조. 운영체제의 이번 로컬 공개 승인은 공개 서비스·전체 파일럿 승인으로 확대하지 않음. 모든 Topic의 사람 검수와 공개 수 조건 충족 전에는 P8-T07 전체 완료 표시 금지.
