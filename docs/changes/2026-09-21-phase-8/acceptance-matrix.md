# Phase 8 인수 조건 자동 증거

상태: 로컬 자동 회귀 연결. AC-007 교체·과거 근거 단일 회귀 추가. [기존 소프트웨어 회귀](../2026-09-27-service-completion/verification.md), [실제 GPT 정상 사용자 흐름](../2026-09-29-openai-completion/verification.md) 구분

## 연결 원칙

- 제품 인수 조건의 사용자 관찰 결과와 직접 연결되는 테스트만 대표 증거로 선택
- 하위 규칙은 관련 테스트 묶음으로 보완
- 실제 OpenAI 품질은 자동 증거에서 제외
- 전체 통과 여부는 `verification.md`의 새 실행 결과로 판정

## AC 연결

| AC | 대표 자동 테스트 | 수준 | 보장 범위 |
|---|---|---|---|
| AC-001 최초 학습 | `KnowledgeFlowTest.progressesFromRecommendationThroughCommittedEvaluation` | API 통합 | UNKNOWN 신규 회원 추천, 답변, 완료 평가, 지식 지도·학습 홈 연결 |
| AC-002 정상 평가 | `AnswerServiceTest.processesCommittedAnswer` | Service 통합 | Answer·Evaluation 보존, 전체·개념 판정, Evidence, Knowledge State 1회 반영 |
| AC-003 평가 실패 | `KnowledgeCompletionFailureTest.preservesProviderFailureRetries` | Service 통합 | provider 실패 재시도, Answer 보존, FAILED, Knowledge State 불변 |
| AC-004 미평가와 취약 구분 | `KnowledgeFlowTest.progressesFromRecommendationThroughCommittedEvaluation` | API 통합 | UNKNOWN/NULL에서 평가 후 LEARNING으로 구분 |
| AC-005 후속 질문 | `FollowUpQuestionServiceTest.completesLearningLoopOnce` | Service 통합 | 일반 평가, 후속 질문 최대 1개, 후속 평가, 다음 기본 문제 |
| AC-006 관리자 권한 | `AuthenticationFlowTest.rejectsUserSessionFromAdminApi` | 인증 통합 | USER session의 관리자 API 거부와 관리자 데이터 비노출 |
| AC-007 콘텐츠 버전 | `KnowledgeAnswerSerializationTest.preservesHistoricalEvidenceAfterDocumentReplacement`, `AdminContentFlowTest.preservesDocumentVersionThroughApi` | API·Service 통합 | v2 공개 → v1 폐기 → 새 검색은 v2만 반환 → 과거 평가의 v1 Chunk·내용·버전·직렬화 유지. H2·PostgreSQL 검증 |

## P0 요구사항 연결

| 요구사항 | 대표 실행 증거 | 경계 |
|---|---|---|
| FR-AUTH-001~003 | AuthServiceTest, AuthenticationFlowTest, SecurityConfigurationTest | 가입·세션·권한, 로그인 DB 제한 |
| FR-ADMIN-001~004 | AdminTaxonomyServiceTest, AdminContentFlowTest, KnowledgeChunkServiceTest, VersionConcurrencyTest | 분류·문서·문제·검수·공개·Chunk·동시 버전 |
| FR-ADMIN-005 | AdminEvaluationControllerTest, 실제 cmux 평가 검토 | 원문 조회는 관리자 권한에 한정 |
| FR-QUESTION-001~002 | RecommendationServiceTest, PublicQuestionServiceTest, PublicQuestionControllerTest | 추천·공개 필터·내부 답안 비노출 |
| FR-ANSWER-001~003 | AnswerFlowTest, AnswerServiceTest, 실제 두 JVM HTTP | 접수·멱등 키·이력·회원 소유권 |
| FR-EVAL-001~005 | AnswerServiceTest, KnowledgeRetrievalServiceTest, EvaluationTest, KnowledgeCompletionFailureTest | 모의 Port·HTTP adapter 계약까지. 실제 AI 판정 품질 제외 |
| FR-KNOWLEDGE-001~003 | KnowledgeCompletionTest, KnowledgeFlowTest, KnowledgeQueryServiceTest | DB 원자적 반영·UNKNOWN 구분·지도 |
| FR-FOLLOWUP-001~002 | FollowUpQuestionServiceTest, FollowUpQuestionControllerTest | 모의 생성·본인 소유·최대 1개·후속 답변 |
| FR-PROGRESS-001 | LearningProgressServiceTest, KnowledgeFlowTest | 현재 학습·빈 상태·다음 학습 |

- HTTP 경계: Controller 테스트의 null·빈 문자열·길이·enum·존재하지 않는 ID·400/401/403/404/409/429/500
- 상태·DB 경계: 도메인 불변식 + Repository UNIQUE + H2/PostgreSQL 동시 생성·완료·rollback
- 범위: [최신 전체 회귀 및 한계](../2026-09-29-openai-completion/verification.md). GPT 정상 경로는 실제 브라우저로 확인, 실제 provider 장애·콘텐츠 교체는 계약·통합 테스트에 한정

## 보안 보완 증거

| 경계 | 자동 증거 |
|---|---|
| 관리자 API 전체 계열 | `SecurityConfigurationTest.rejectsUserFromEveryAdminApiFamily` |
| 다른 회원 답변·평가 | `AnswerFlowTest.hidesAnotherMembersAnswer` |
| query parameter를 이용한 회원 위장 | `KnowledgeFlowTest.progressesFromRecommendationThroughCommittedEvaluation`의 다른 principal 조회 |
| prompt injection 데이터 격리 | `OpenAiEvaluationAdapterTest.sendsIsolatedStrictSchemaRequest`, `OpenAiFollowUpQuestionAdapterTest.isolatesPromptInjectionAsUntrustedData` |
| HTML/script 출력 | `EvaluationPanel.test.ts`의 “평가와 근거의 HTML 문자열을 실행하지 않고 텍스트로 표시한다” |
| 인증 응답 비밀정보 제외 | `AuthenticationFlowTest.logsInAndRestoresCurrentMemberFromSession` |

## 실행

```bash
./gradlew test --tests '*KnowledgeFlowTest' --tests '*AnswerServiceTest' \
  --tests '*KnowledgeCompletionFailureTest' --tests '*FollowUpQuestionServiceTest' \
  --tests '*AuthenticationFlowTest' --tests '*AdminContentFlowTest' \
  --tests '*KnowledgeDocumentServiceTest' --console=plain

cd front
npm run test -- src/components/EvaluationPanel.test.ts
```

## 미검증 경계

- 실제 OpenAI 모델 판정 품질과 prompt injection 저항성
- 실제 브라우저 CSP와 배포 reverse proxy header
- 모의 provider 실패→재시도→성공 화면은 최신 검증 완료. 실제 모델 provider의 성공 복구 E2E는 제외 — [503 재시도 뒤 INVALID_RESULT 안전 실패](../2026-09-27-release-readiness/verification.md) 관찰. 정상 평가 복구는 미완료
- 운영 데이터·운영 PostgreSQL에서의 전체 인수 흐름
