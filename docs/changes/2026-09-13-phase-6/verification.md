# Phase 6 구현·검증

## 현재 상태

- 구현 완료: Knowledge State, 개인 추천, 학습 현황, 학습 홈, 지식 지도
- 구조 정리 완료: 학습 패키지, JPA 식별자, 조회 책임, 완료 트랜잭션, 테스트 격리
- 후속 검토 완료: lease 시도 상한, 답변 페이지 전체 건수, 동시성 테스트 종료
- 기준: [제품 명세](../../product/spec.md), [작업 목록](../../planning/tasks.md), [도메인·ERD](../../architecture/domain-model-and-erd.md)
- 평가 정답 자료: [reference-v1](../../evaluation/reference-v1/README.md)

## 구현 범위

```text
content.knowledge 문서·Chunk
             ↓ 평가 근거
평가 완료 + 유효한 개념 판정
             ↓ 같은 트랜잭션
AppliedEvaluationConcept + KnowledgeState(@Version)
             ↓
Topic 집계 → 개인 추천 → 학습 홈·지식 지도
```

HTTP 계약:

- `GET /api/members/me/knowledge-states`: 주제·개념별 상태, 숙련도, 신뢰도, 시도 횟수
- `GET /api/recommendations/next-question`: 다음 문제와 추천 이유
- `GET /api/members/me/progress`: 최근 풀이·평가, 주제 상태, 추천
- 인증된 USER의 principal 회원 ID만 사용
- 비로그인 `401`, ADMIN `403`

상태 계약:

- `UNKNOWN`: `masteryScore=null`, `confidenceScore=0`, `attemptCount=0`
- 유효 판정 점수: 정답 100, 부분 정답 50, 오답 0
- `NEEDS_REVIEW`와 미완료 평가는 상태 계산에서 제외
- 숙련도: 유효 판정 점수 평균
- 신뢰도: `min(100, 유효 평가 수 × 25)`
- `STABLE`: 숙련도 80 이상이며 신뢰도 75 이상
- 나머지 평가 상태: `LEARNING`
- 동률 최신 판정: `evaluatedAt`, 이후 EvaluationConcept ID

추천 계약:

1. 미평가 개념 우선
2. 낮은 숙련도 우선
3. 가장 오래 풀지 않은 문제 우선
4. 문제 ID, 개념 ID 순서
5. 공개 문제·활성 주제·활성 개념만 후보

## 유지되는 계약과 설계 결정

패키지 책임:

- `content.knowledge`: 평가 근거 문서와 청크
- `learning.mastery`: 회원별 개념 상태와 조회
- `learning.recommendation`: 추천 후보와 우선순위
- `learning.progress`: 학습 현황 조합
- `evaluation.service`: 평가 완료 트랜잭션과 재시도 조정

상태 소유:

- 점수·상태 계산: `KnowledgeState`
- 주제 집계: `TopicKnowledgeSummary`
- 현재 transaction의 판정 반영: `KnowledgeStateService`
- 완료 transaction 전체 재시도: `EvaluationCompletionTransaction`
- 조회 조합: `KnowledgeQueryService`, `RecommendationService`, `LearningProgressService`

JPA 계약:

- `KnowledgeState`: 자동 생성 PK, `UNIQUE(member_id, concept_id)`, `@Version`
- `AppliedEvaluationConcept`: 자동 생성 PK, `UNIQUE(evaluation_concept_id)`
- 적용 기록 매핑: `ManyToOne` + 명시적 UNIQUE
- 이유: Hibernate/H2의 자동 UNIQUE 이름에 의존하지 않고 충돌 제약을 식별
- 둘 이상의 to-many fetch join 금지
- 평가 상세 조회: 두 번의 fetch 결과를 같은 영속성 컨텍스트에서 조합

동시성 방어:

```text
최초 상태 동시 생성 → 회원·개념 UNIQUE
기존 상태 동시 갱신 → @Version
같은 판정 재전달     → 평가 개념 UNIQUE
충돌                 → transaction rollback → 새 transaction에서 최대 8회 재시도
```

- 외부 평가 결과 획득은 재시도 밖에서 한 번만 수행
- 허용된 유일키 경쟁과 잠금 충돌만 재시도
- FK·NOT NULL·무관 UNIQUE 위반은 즉시 `PERSISTENCE_ERROR`
- 재시도 소진은 `PERSISTENCE_CONFLICT`
- provider 실패와 저장 실패를 별도 오류로 분류

## 패키지·트랜잭션·테스트 책임

- Service 인터페이스와 `Default...` 구현 분리
- Service 통합 테스트: 실제 Repository와 DB 사용
- test-level `@Transactional`, Service mock, 공통 통합 테스트 부모 미사용
- 각 테스트 클래스가 fixture와 `@AfterEach` 정리 소유
- 정리 순서: 적용 기록 → 상태 → 평가 → 답변 → 문제 → 청크 → 문서 → 개념 → 주제 → 회원
- 자식 cascade가 필요한 Evaluation·Question: `deleteAll()`
- 단순 테이블: FK 역순 `deleteAllInBatch()`
- 조회 비용 검증: `KnowledgeQueryCostTest`, `AnswerQueryCostTest`
- 순수 집계 검증: `TopicKnowledgeSummaryTest`
- 실제 JPA 저장·UNIQUE 검증: `QuestionConceptMappingTest`

## 실행한 검증

### TDD 증거

- 도메인: 미평가, 0점, 안정 상태, 오답 후 하락, 역순·동률, 잘못된 입력
- 저장: 반복 적용, 동시 최초 생성, 동시 기존 갱신, rollback 원자성
- 조회: 회원 격리, 주제 집계, 최근 요약, 추천 우선순위, 비활성 후보 제외
- HTTP: binding, 직렬화, principal 소유권, `401`, `403`
- 프런트: 로딩, 실패, 재시도, 빈 상태, 신규·부분 평가, 메뉴와 관리자 이동
- 후속 검토: 네 번째 lease 선점 차단, 빈 페이지의 전체 건수, 동시 작업 취소·종료

### 최종 결과

| 범위 | 결과 |
|---|---|
| 백엔드 구조 정리 | 62개 클래스·289개 성공, 실패·오류·스킵 0 |
| 백엔드 후속 검토 | 62개 클래스·297개 성공, 실패·오류·스킵 0 |
| 실행 JAR | `./gradlew test bootJar --rerun-tasks --console=plain` 성공 |
| 프런트 | 21개 파일·123개 성공 |
| 프런트 타입·빌드 | `npm run build` 성공 |
| API→DB 흐름 | 추천 → 답변 → 평가 → 상태·현황 조회 성공 |
| OpenAPI | YAML 파싱과 내부 참조 확인 |
| 정적 점검 | 이전 패키지명, `var`, wildcard import, 테스트 정리 SQL 0건 |

조회 비용:

- 지식 지도: 현재 상태·활성 개념·활성 주제 3개 SQL
- 답변 상세 페이지: 최대 7개 SQL 유지
- 학습 현황: 독립 조회 조합으로 회원 상태 SELECT 1개 추가

## 남은 경계

- 실제 모델 평가 정확도·비용
- 운영 PostgreSQL의 잠금·제약 동작
- 운영 schema 변경·rollback 절차
- 도입 전 완료 평가의 소급 반영과 재계산 명령
- 충돌 8회 소진 이후 운영 복구 정책
- interrupt에 응답하지 않는 작업의 강제 종료
- 실제 모바일 기기·전체 브라우저·스크린리더 사용자 검증

H2 `create-drop` 결과를 운영 migration 증거로 사용하지 않는다. 평가 정답 자료의 사람 검수와 Phase 6 기능 검증도 서로 다른 증거로 유지한다.
