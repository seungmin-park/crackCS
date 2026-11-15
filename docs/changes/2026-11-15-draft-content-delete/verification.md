# 초안 콘텐츠 삭제 API 검증

- 독자·질문: 개발자·운영자 / 초안 문제와 문서를 어떤 조건에서 지울 수 있고 무엇으로 확인했는가?
- 확인일: 2026-11-15
- 브랜치: `feat/delete-draft-content`
- 계약: [OpenAPI](../../../openapi.yml), [제품 명세](../../product/spec.md)

## 동작

| 요청 | 조건 | 결과 |
|---|---|---|
| `DELETE /api/admin/questions/{questionId}` | DRAFT | 204. 문제와 평가 Concept 연결 삭제, Concept 유지 |
| 같은 요청 | PUBLISHED·RETIRED | 409 `INVALID_CONTENT_STATE`, 변경 없음 |
| 같은 요청 | 없는 문제 | 404 |
| `DELETE /api/admin/knowledge-documents/{documentId}` | DRAFT | 204 |
| 같은 요청 | PUBLISHED·RETIRED | 409 `INVALID_CONTENT_STATE`, 변경 없음 |

- 삭제 가능 여부는 `Question.ensureDeletable()`, `KnowledgeDocument.ensureDeletable()`이 판단
- DRAFT는 학습자에게 공개된 적이 없는 상태. 답변·평가·검색 문단이 참조하지 않음
- 공개된 콘텐츠는 기존대로 폐기(`retire`)만 가능

## RED → GREEN

- RED: 빈 구현 상태에서 새 테스트 12개 중 10개 실패(삭제되지 않음, 예외 없음, 매핑 없음). 나머지 2개는 DRAFT 허용 확인
- GREEN: 도메인 규칙, Service, Controller 구현 뒤 12개 통과

| 수준 | 테스트 |
|---|---|
| 도메인 | `QuestionTest` 2개, `KnowledgeDocumentTest` 2개 |
| Service(실제 Repository) | `QuestionServiceTest` 3개, `KnowledgeDocumentServiceTest` 2개 |
| HTTP 계약 | `QuestionControllerTest` 2개 |
| API 통합 | `AdminContentFlowTest` 1개 |

## 실행 결과

| 명령 | 결과 |
|---|---|
| `./gradlew test --tests '*QuestionTest' --tests '*QuestionServiceTest' --tests '*QuestionControllerTest' --tests '*KnowledgeDocumentTest' --tests '*KnowledgeDocumentServiceTest' --tests '*AdminContentFlowTest'` | 123개 통과 |
| `bash scripts/verify.sh backend` | 580개 통과, exit 0 |
| 로컬 서버(H2 메모리 DB)에서 초안 문제·문서 등록 후 `DELETE` 호출 | 둘 다 삭제, 공개 콘텐츠는 그대로 |

- `frontend`·`postgres` 검증은 로컬에서 실행하지 않음. CI 결과로 확인

## Concept 재활성화 (같은 날 추가)

- `POST /api/admin/concepts/{conceptId}/activate`: 비활성 Concept을 다시 활성화, 204. 소속 Topic이 비활성이면 409
- 이유: 비활성화는 있었지만 되돌리는 방법이 없어, 비활성 상태로 남은 Concept을 다시 쓰려면 DB를 직접 고쳐야 했다
- RED: 빈 구현에서 새 테스트 5개 실패 → GREEN: 통과. `ConceptTest` 2개, `AdminTaxonomyServiceTest` 2개, `AdminContentFlowTest` 1개

## 범위 밖

- 관리자 화면의 삭제 버튼. 현재는 API로만 제공
- Concept 삭제. 기존 비활성화(`deactivate`) 사용
