# API 시각을 UTC로 내보내기 검증

- 독자·질문: 개발자·운영자 / 화면에 시각이 9시간 이르게 보이던 원인이 무엇이고 무엇으로 고쳤는가?
- 확인일: 2026-11-15
- 브랜치: `fix/api-time-offset`
- 계약: [OpenAPI](../../../openapi.yml), [제품 명세](../../product/spec.md)

## 증상과 원인

- 운영에서 2026-11-15 16:42(KST)에 낸 답변의 제출 시각이 화면에 "오전 7:42"로 표시됨
- 서버는 `LocalDateTime`을 시간대 표시 없이 내보냈다(`2026-11-15T07:42:40`). 운영 JVM의 시간대는 UTC다
- 브라우저는 시간대 없는 문자열을 자기 시간대의 시각으로 읽는다. 한국에서는 UTC 숫자를 그대로 한국 시각으로 표시해 9시간 이르다
- 로컬 실행에서는 JVM과 브라우저가 같은 시간대라 드러나지 않았다

## 변경

| 위치 | 내용 |
|---|---|
| `ApiTimestamp` | 서버 로컬 시각을 JVM 시간대 기준 `Instant`로 변환. 없는 값은 `null` |
| 응답 8종 | 시각 필드 13개를 `Instant`로 직렬화(`...Z`). `AnswerResponse`, `ProgressResponse`, `KnowledgeStatesResponse`, `QuestionResponse`, `QuestionSummaryResponse`, `KnowledgeDocumentResponse`, `AdminEvaluationDetailResponse`, `AdminEvaluationSummaryResponse` |
| `KnowledgeTopics.vue` | 최근 평가 시각을 문자열 자르기 대신 보는 사람의 시간대로 변환해 표시 |
| 답변 상세·이력 화면 | 변경 없음. `new Date()`가 `Z`가 붙은 값을 그대로 해석 |

- 저장 값, 시각 비교, 정렬 등 서버 내부 동작은 바꾸지 않았다. 응답 직렬화와 화면 표시만 바뀐다
- JVM 기본 시간대를 한국으로 바꾸는 방법은 쓰지 않았다. 인증 쪽 `Clock.systemUTC()`와 DB의 `CURRENT_TIMESTAMP` 기본값이 UTC라 저장 값의 기준이 섞인다

## RED → GREEN

- RED(백엔드): 빈 구현과 바꾼 기대값으로 8개 실패. `ApiTimestampTest` 3개, `AnswerControllerTest`·`KnowledgeControllerTest`·`AdminEvaluationControllerTest`·`QuestionControllerTest`·`KnowledgeFlowTest` 각 1개
- RED(프런트): `LearningViews.test.ts`의 새 테스트가 "최근 2026-09-13 10:05"를 받아 실패(기대 19:05, 한국 시간대 실행)
- GREEN: 구현 뒤 모두 통과

## 실행 결과

| 명령 | 결과 |
|---|---|
| `bash scripts/verify.sh backend` | 589개 통과, exit 0 |
| `front`: `npm test`, `npm run type-check`, `npm run build-only` | 355개 통과, 타입 검사·빌드 통과 |

- `postgres` 검증은 로컬에서 실행하지 않음. CI 결과로 확인
- 운영 화면 확인은 배포 뒤 답변 상세의 제출 시각으로 한다

## 범위 밖

- 이미 저장된 값의 변환. 저장 값은 그대로 두고 내보낼 때만 바꾼다
- 프런트 테스트 fixture 가운데 시각을 화면에 쓰지 않는 값의 형식 정리
