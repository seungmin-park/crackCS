# Phase 6 구현 계획

> 최초 구현 당시 계약·RED/GREEN 이력. 사용자 리뷰 후 PK·패키지·트랜잭션 구조는 [후속 구조 정리](refactoring.md)로 대체. 보존 이유: 최초 구현의 검증 과정 추적.

**Goal:** tasks.md의 P6-T01~06과 Phase 6 Gate 구현·검증.

**Architecture:** content.knowledge는 평가 근거 자료, knowledge는 회원×개념 학습 상태 소유. 도메인에서 상태 계산, Service에서 원자적 저장·중복 방지, 조회 Service에서 현재 상태·추천 조합.

**Tech Stack:** Java 21 / Spring Boot 4.1.1 / JPA / H2 test / Vue / TypeScript / Vitest.

**Spec:** [제품 명세](../../product/spec.md), [작업 목록](../../planning/tasks.md#phase-6--knowledge-state와-개인-추천).

## 공통 제약

- 현재 `/Users/seungmin/Desktop/repo/crackCS`, `feat/phase6`에서 구현. 자료·회고 보존. 커밋·push 없음.
- production 구현 전 동작 테스트 실패 확인. 컴파일·fixture 오류는 RED로 집계하지 않음.
- Service 인터페이스 + Default 구현; 실제 DB Service 테스트, AfterEach FK 역순 정리, test-level Transactional/Service mock 금지.
- 엔티티 private Builder 생성자, protected JPA 생성자, public setter 없음. 전체 입력 검증 후 변경.
- 테스트 영어 메서드명·한글 DisplayName, 클래스 DisplayName 없음.
- AI 실호출·골든 셋 정답 인증 없음. Phase 5 외부 품질 Gate와 이번 기능 검증 분리.

## Task 1: 백엔드 상태·조회·추천

Files: `src/main/java/com/example/crackcs/knowledge/`, 대응 테스트. 기존 평가 처리기·보안 설정·필요 Repository만 연동 수정. 프런트·문서는 controller 담당.

### 상태 계약

- KnowledgeStateService: `void apply(Long evaluationId)`, `void applyInCurrentTransaction(Long evaluationId)`.
- 상태 PK=(memberId,conceptId), @Version. 적용 기록 PK=EvaluationConcept ID.
- UNKNOWN: masteryScore NULL, confidenceScore 0, attemptCount 0. algorithmVersion `knowledge-v1`.
- 유효 Concept 판정 CORRECT=100, PARTIALLY_CORRECT=50, INCORRECT=0. NEEDS_REVIEW 제외. 부모 Evaluation도 적격 완료여야 함.
- mastery=(누적 점수 합+최신 점수)/(유효 평가 수+1), confidence=min(100,수*25).
- 최신: evaluatedAt, 동일 시각이면 EvaluationConcept ID. STABLE mastery>=80 및 confidence>=75. 나머지 LEARNING.
- invalid 입력에서 상태·시각 보존. 오답 후 STABLE→LEARNING 허용.
- 완료와 상태 반영 동일 트랜잭션. 충돌 시 새 트랜잭션에서 최대 8회 재시도; 이미 받은 AI 결과 재사용. 중복·동시 최초 생성·동시 기존 갱신 방어.

### HTTP 계약

USER 전용, principal 회원 ID만 사용:

- GET `/api/members/me/knowledge-states`: `{topics:[{topicId,topicName,status,masteryScore,confidenceScore,unknownCount,learningCount,stableCount,concepts:[{conceptId,conceptName,status,masteryScore,confidenceScore,attemptCount,lastEvaluatedAt}]}]}`.
- GET `/api/recommendations/next-question`: `{questionId,title,conceptId,conceptName,reason,reasonText}`. reason `UNASSESSED_CONCEPT`, `LOW_MASTERY`, `NO_AVAILABLE_QUESTION`. 후보 없으면 ID/title/name NULL, reasonText 설명.
- GET `/api/members/me/progress`: `{totalAnswers,recentAnswerCount,recentEvaluations:[{answerId,questionTitle,status,verdict,score,submittedAt}],topics,recommendation}`. 최근 풀이=7일, 최신 평가 목록=5개 답변의 현재 평가 상태.
- 활성 Topic/Concept만 조회. Topic mastery=평가된 Concept 평균, confidence=미평가0 포함 평균. 전부 미평가/빈 Topic UNKNOWN, 전부 STABLE이면 STABLE, 나머지 LEARNING.
- 공개 기본 문제 + 활성 Topic + 모든 연결 Concept 활성만 추천. 후보 내 미평가 우선→낮은 mastery→문제 마지막 풀이가 오래된 순(미풀이 먼저)→문제ID→개념ID. 문제별 최우선 개념 선택.
- 현재 상태 조회가 전체 Answer 이력을 재계산하지 않도록 Repository query 사용.

### 실행·검증

- [x] 순수 도메인 RED: 없음/0점/정답3회/정답3회+오답=60점/역순 및 동률/입력 거부.
- [x] 도메인 GREEN.
- [x] 저장 RED: 반복 적용=1회, 동시 다른 평가=2회, FAILED/NEEDS_REVIEW 제외, rollback 원자성.
- [x] 저장·완료 연동 GREEN. DB 재조회로 검증.
- [x] 조회 RED: UNKNOWN과 0, 회원 격리, Topic 집계, 최근 요약, 추천 우선순위·폐기/비활성 제외·후보 없음.
- [x] 조회 GREEN, HTTP binding/직렬화·비로그인401·ADMIN403 검증. MVC mock은 Service 계약만.
- [x] 관련 테스트 및 전체 백엔드 테스트 성공. RED/GREEN 증거는 verification.md 보고.

## Task 2: 학습 홈·지식 지도

Files: `front/src/api/learning.ts`, `components/KnowledgeTopics.vue`, `views/LearningHomeView.vue`, `views/KnowledgeMapView.vue`, 기존 App/router/Login 및 테스트.

- 위 HTTP 계약 그대로 사용. `/` 학습 홈, `/knowledge-map` 지도. USER 로그인 필요, ADMIN은 `/admin`으로 이동.
- 상태 요약·개념별 상태/점수/신뢰도/횟수/최근 시각, 최근 풀이·평가, 추천 문제와 이유 표시.
- UNKNOWN을 0점과 구분. 신뢰도는 관측량이며 정답 확률이 아니라는 설명.
- 로딩/실패/재시도/빈 상태/신규/부분 평가 테스트 먼저. HTTP 계층만 mock.
- [x] 기능 없는 화면의 RED → 구현 → GREEN.
- [x] 메뉴·로그인 경로·관리자 접근 테스트.
- [x] 프런트 전체 테스트 및 type-check/build.

## Task 3: 검토·문서·최종 검증

- [x] 독립 코드 검토(명세 적합성+품질), 지적 수정 후 재검증.
- [x] API→DB 핵심 흐름 검증, 전체 테스트 수·오류 기록.
- [x] 정책·ERD·OpenAPI·문서 목록·tasks 갱신. 성공한 항목만 체크.
- [x] 자료 변경 없음 확인. 운영 PostgreSQL·실모델·소급 재구축 미검증 경계 명시.

## 진행 기록

- 시작 HEAD `d7815cf`. 기존 수정 코드 없음, 미추적 회고·검색 임시 파일 보존.
- Task 1↔2 공유 경계: 위 JSON 계약만. 수정 파일 충돌 없음.
- Task 1 상태 규칙↔테스트: 100+100+100+0에 최신0 추가 /5 =60, 신뢰도100 LEARNING.
- Task 2 상태 규칙↔UI: NULL은 미평가, 숫자0은 숙련도0 표시. 동일 스케일0~100.
- Task 3 증거↔체크: 과거 검증 기록을 이번 통과 근거로 사용하지 않음.
