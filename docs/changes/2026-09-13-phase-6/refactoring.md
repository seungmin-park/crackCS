# Phase 6 구조 정리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. 기존 Phase 6 기능을 보존하면서 작업별 구현·검토.

**Goal:** 사용자 승인 13개 리뷰 항목 반영, 이름·JPA 매핑·조회·트랜잭션·테스트 책임 명확화.

**Architecture:** 근거 문서는 content.knowledge 유지. 회원 상태 learning.mastery, 답변 learning.answer, 추천 learning.recommendation, 현황 learning.progress. 자동 생성 PK와 업무 UNIQUE 분리. 평가 완료와 상태 반영은 한 트랜잭션. 아래 Task 1~3은 이전 구조 정리 기록이며 현재 패키지 변경은 [패키지 책임 정리](#패키지-책임-정리) 기준.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Data JPA, Hibernate, JUnit, H2.

**Spec:** 이 문서의 승인 요구사항 및 [기존 기능 계약](plan.md). 구조 차이는 이 문서 우선.

## Global Constraints

- 기존 API 경로·JSON·권한·계산식·추천 순서 유지.
- 사용자 변경·docs/content·골든 셋 보존. benchmark 재생성 금지. commit·push·branch 변경 없음.
- Java 운영·테스트 코드 var 금지, 타입 완전 수식명은 import로 이동. JPQL enum은 파라미터 사용.
- Service 인터페이스 + Default 구현. 도메인이 상태·계산 규칙 소유. builder private 생성자, JPA protected 기본 생성자.
- Service 테스트 실제 DB·Repository, test-level Transactional 및 Service mock 금지. 각 클래스가 fixture와 AfterEach 정리 직접 관리. KnowledgeIntegrationSupport 제거.
- 기존 동작은 GREEN 기준선에서 refactor. 새 오류 방어·매핑 불변식은 의미 있는 RED 먼저. 관련 테스트 후 전체 테스트.
- 모든 테스트 메서드에 한글 DisplayName, 클래스에는 없음. 소스문자열·annotation 존재 검사 대신 저장 결과/실패 동작 검증.

## Task 1: 회원 학습 도메인·조회·완료 트랜잭션 정리

**Files:** src/main/java/com/example/crackcs/knowledge/** → learning/knowledge/**, learning/recommendation/**, learning/progress/**. 기존 evaluation/service/DefaultEvaluationProcessor.java, evaluation/repository/EvaluationRepository.java, learning/service/DefaultAnswerService.java, content/{topic,concept,question}/repository/*Repository.java, learning/repository/AnswerRepository.java. src/test/java의 영향받는 호출·패키지와 신규 경계 테스트.

**Interfaces and names:**
- KnowledgeState: 회원+개념 상태로 의미 충분하므로 이름 유지, package learning.knowledge.domain. KnowledgeStatus 유지(API enum 계약).
- KnowledgeApplication → AppliedEvaluationConcept: 이미 반영한 개념 판정이라는 목적 명시, table knowledge_application 유지. 중간 이름 KnowledgeStateApplication은 Task 2에서 사용자 승인 이름으로 교체.
- KnowledgeStateId 제거. state.id 자동 생성 IDENTITY, member/concept 일반 ManyToOne FK NOT NULL, UNIQUE(member_id,concept_id), Version Long NOT NULL 유지. builder(member,concept), repository findByMemberIdAndConceptId, findByMemberId.
- AppliedEvaluationConcept: id 자동 생성 IDENTITY, evaluationConcept 일반 ManyToOne FK NOT NULL + Table의 명시적 이름 UNIQUE, builder(evaluationConcept), existsByEvaluationConceptId. 중복 scalar FK 없음. DB의 논리적 1:1은 UNIQUE로 보장. Hibernate/H2 OneToOne의 inline 자동 UNIQUE 이름 문제 때문에 매핑은 ManyToOne 선택.
- KnowledgeReadRepository 제거. Topic/Concept/Question/Answer의 기존 Repository에 명시적 Query 및 typed projection 배치. Controller response 의존·Object[] 캐스팅 제거.
- KnowledgeReadService → KnowledgeQueryService(knowledgeStates), RecommendationService(recommendation), LearningProgressService(progress), 각 Default 구현. Controller/response를 유스케이스 package로 배치. 기존 HTTP JSON 유지.
- Topic 집계 도메인 객체/값으로 계산 책임 분리, 추천 Candidate/RecommendationPriority는 recommendation.domain. 단계를 이름 있는 메서드로 표현. 정책 수치·미평가와0·평균·동률 그대로.
- EvaluationRepository 상세 조회 fragment: List<Evaluation> findAllDetailsByAnswerIds(List<Long> answerIds). 내부 두 fetch 조회 반환값을 사용해 상세 결과 반환, Service에서 반환값을 무시하지 않음. 같은 persistence context 필요 명시. 둘 이상의 to-many를 한 SQL로 fetch하지 않음.
- KnowledgeStateService는 현재 트랜잭션에서 적용하는 계약으로 단순화. 외부 완료 유스케이스가 트랜잭션/재시도 소유. 기존 독립 적용용 호출은 적절한 평가 완료/반영 진입점으로 전환, 테스트도 production 경계 검증.
- KnowledgeTransactionRetry 제거/이동 → evaluation.service.EvaluationCompletionTransaction (인프라 조정 책임 명시). MAX_ATTEMPTS=8, 새 tx에서 전체 완료+적용 재실행, AI 결과 획득은 바깥. 모든 DataIntegrityViolationException 재시도 금지. 잠금 충돌 및 명시적 상태/적용 유일키 경쟁만 재시도, FK/NOT NULL 등은 즉시 실패. DB별 SQLState/constraint 검증 없이 문자열 광범위 판정 금지.

- [x] 기준선 ./gradlew test --console=plain 확인 (기존 문법오류 masteryScore 뒤 진 제거 후 UP-TO-DATE 성공).
- [x] RED: 영구 무결성 오류에서 시도1회/부분 저장 없음, 연관관계 입력 누락 거부. 기존 동시성·멱등성·rollback·조회비용 테스트 유지.
```java
assertThatThrownBy(() -> completionTransaction.execute(operation))
        .isInstanceOf(DataIntegrityViolationException.class);
assertThat(attempts.get()).isEqualTo(1);
```
- [x] 매핑·package 이동과 의존 호출 교체. 소스파일 이동은 apply_patch Move to 또는 기계적 bulk rewrite 허용, 관련 없는 파일 보존.
- [x] 조회 repository projection 및 service 책임 분리, 상세 fetch 계약 명시.
- [x] 완료 transaction 범위·재시도 분류 구현, 오류 정책 테스트 GREEN.
- [x] 검토 보완: processor도 영구 DB 오류를 PERSISTENCE_ERROR로 종료, 일시적 저장 충돌은 별도 코드. PROVIDER_ERROR와 분리. 실제 test-owned DB 제약 실패로 FAILED·재호출 없음 검증, 제약은 테스트가 복구.
- [x] 관련 테스트와 전체 ./gradlew test --console=plain 성공. 284개 성공, 실패·오류·스킵 0. 상세 보고 /private/tmp/phase6-refactor-task1-report.md (RED 명령/실패 이유, GREEN, 파일 목록, 테스트 수).

## Task 2: 테스트 책임·삭제 방식·스타일 정리

**Files:** src/test/java/com/example/crackcs/knowledge/** 또는 이동된 learning 패키지 테스트, src/test/java/com/example/crackcs/learning/{service,controller}/*Test.java 및 var/완전 수식 타입명을 사용하는 모든 Java 소스. AGENTS.md는 controller 담당.

- [x] 사용자 추가 승인: KnowledgeStateApplication → AppliedEvaluationConcept, KnowledgeStateApplicationRepository → AppliedEvaluationConceptRepository. Java 소스/테스트 전부 참조 교체, table knowledge_application·컬럼·제약 이름은 그대로. 메서드/변수명도 이미 적용한 판정이라는 의미로 맞춤. 현재 문서 이름 교체는 root 담당.
- [x] KnowledgeIntegrationSupport 삭제. 각 테스트 클래스가 필요한 Repository·fixture helper·AfterEach 소유. 상속 대체용 만능 fixture/cleanup 객체 금지.
- [x] teardown: FK 역순 혼용. appliedConcepts.deleteAllInBatch(); states.deleteAllInBatch(); evaluations.deleteAll(); answers.deleteAllInBatch(); questions.deleteAll(); chunks.deleteAllInBatch(); documents.deleteAllInBatch(); concepts.deleteAllInBatch(); topics.deleteAllInBatch(); members.deleteAllInBatch(); 각 클래스 실제 생성한 종류만 포함. Evaluation/Question은 소유 자식 cascade 필요, 나머지는 추가 owned collection/callback 없는지 확인 후 bulk. 테스트 전체 transaction 또는 공유 EntityManager로 묶지 않음. 정리용 JDBC SQL 제거, 운영 repo에 테스트 전용 cleanup 메서드 추가 금지.
- [x] Fake reset은 DB 정리 실패와 독립적으로 보장. 서비스 통합 테스트에서 test-level Transactional 금지 유지.
- [x] SQL 통계는 전용 조회비용 테스트로 격리 (KnowledgeReadServiceTest와 AnswerServiceTest의 통계 검증 모두). EntityManagerFactory는 Hibernate 통계 측정에만 사용, 일반 준비·저장·조회는 Repository. 시간 fixture SQL은 제거 가능성 검토하되 시간 검증 자체 삭제/약화 금지. 기존 QuestionConceptRepositoryTest의 직접 DB UNIQUE 검증은 독립 JPA mapping 테스트로 구분, 무관한 테스트 광범위 재작성 금지.
- [x] 기존 KnowledgeReadServiceTest는 상태조회·추천·현황별 테스트로 분리. 이동된 TopicKnowledgeSummary의 순수 집계 규칙은 Spring 없는 단위 테스트로 검증(미평가/0점 구분·빈 주제·신뢰도 미평가 포함).
- [x] 전체 Java var → 실제 타입, 완전 수식 타입 → import. 와일드카드 import도 수정 파일에서 명시형으로 정리.
- [x] 동시성·멱등성·rollback·직렬화·조회비용 테스트 누락 없음 확인 후 전체 test. 빈 package 디렉터리만 rmdir.
- [x] 보고 /private/tmp/phase6-refactor-task2-report.md.

## Task 3: 공식 근거·문서·최종 검증

**Files:** AGENTS.md, docs/architecture/domain-model-and-erd.md, docs/changes/2026-09-13-phase-6/{plan,verification,refactoring}.md, docs/README.md 및 현재 코드 안내 참조.

- [x] Spring Data JpaRepository 공식 계약 및 SimpleJpaRepository 구현 확인: deleteAll은 엔티티 삭제 lifecycle, batch는 bulk 및 cascade/callback 비적용·1차 캐시 불일치 경계. 인프런 박우빈 Test Fixture 클렌징, 김영한 JPA 벌크 연산 참고. 소유 자식 cascade 필요한 aggregate는 deleteAll, 단순 테이블은 FK 순서 준수한 deleteAllInBatch 혼용.
- [x] @Version UPDATE WHERE id AND version, 영향 행0 충돌, 새로운 transaction 재시도 예시와 실제 동시 테스트 근거 설명. UNIQUE는 최초 생성 중복, Version은 기존 상태 갱신 충돌 담당.
- [x] 현재 ERD/plan/검증 문서에 새 이름·PK·FK·책임 반영, 이력 보존 구분, 문서 목록 링크.
- [x] 독립 spec/quality review 후 지적 수정, 전체 test bootJar, frontend API 무변경 회귀 확인, var/FQN/빈디렉터리/diff/link 검증. 최종 Approved, 문서 상태 Minor 수정.
- [x] 완료 보고: 변경·RED/GREEN·성공/실패/스킵 수·PG 미검증 경계·schema 전환 주의. 자료 변경 없음 확인. [검증 기록](verification.md#구조-정리-검증)에 최종 백엔드 289개·프런트 123개 근거.

## 삭제 방식 선택 근거

- 박우빈, [Test Fixture 클렌징](https://www.inflearn.com/courses/lecture?courseId=329295&unitId=152677): bulk 삭제의 효율, FK 삭제 순서, cascade 삭제의 추가 조회 비용, 상황별 혼용. 강의 본문 조회 결과 확인
- 김영한, [JPA 기본편 — 벌크 연산](https://www.inflearn.com/courses/lecture?courseId=324109&unitId=21732): 벌크 연산은 영속성 컨텍스트를 우회. 기존 관리 객체를 계속 사용하면 DB와 불일치. 강의 본문 조회 결과 확인
- [Spring Data JpaRepository API](https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/JpaRepository.html): batch 삭제의 lifecycle/cascade 비적용과 1차 캐시 불일치 계약
- [SimpleJpaRepository 4.1.1 구현](https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.java): deleteAll은 findAll 후 개별 delete, deleteAllInBatch는 bulk query.executeUpdate
- 프로젝트 판단: Evaluation·Question은 소유 자식을 cascade로 정리, 단순 테이블은 FK 역순 bulk. 양쪽 모두 무조건적인 최선은 아님
- 테스트 경계: Service의 실제 commit과 여러 트랜잭션을 검증하므로 테스트 전체 rollback 방식으로 전환하지 않음. 각 Repository 정리 호출의 transaction 종료 후 이전 관리 객체를 재사용하지 않음
- 제외: 같은 DB의 병렬 테스트에서 전체 테이블 삭제. 병렬화 시 테스트 데이터·schema 격리부터 필요

## Version을 유지하는 이유

`@Version`은 엔티티를 읽은 뒤 다른 transaction이 먼저 갱신했는지 확인하는 값. JPA가 관리하며 직접 증가시키지 않음. [Jakarta Persistence Version 계약](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/version)

```text
A: version=3 읽기       B: version=3 읽기
        ↓                     ↓
version=3 조건 갱신 성공    version=3 조건 갱신 실패
DB version=4             이미 4이므로 영향 행 0
                              ↓
                        충돌 예외·rollback
                              ↓
                   새 transaction에서 4 재조회
```

개념상 UPDATE 조건:

```sql
update knowledge_state
set attempt_count = ?, version = 4
where id = ? and version = 3;
```

- UNIQUE(member_id, concept_id): 아직 없는 상태를 동시에 INSERT하는 경쟁 방어
- Version: 이미 있는 상태를 동시에 UPDATE하는 유실 방어
- 적용 기록 UNIQUE(evaluation_concept_id): 같은 판정의 재전달 방어
- 재시도: 실패 transaction 전체를 버리고 최신 상태부터 계산. AI 호출 반복 아님
- 한계: Version을 우회하는 직접 SQL/bulk UPDATE에는 이 조건이 자동 적용되지 않음. 운영 상태 수정 경로에서 임의 bulk 갱신 금지
- PK 전환: 기존 local DB의 복합 PK를 ddl-auto=update가 안전하게 바꾼다고 가정하지 않음. 기존 DB는 백업·schema 전환 검증 후 사용. 이번 실행은 test create-drop DB만 대상

## 코드 리뷰 후속 수정

- [x] 평가 lease 만료 후 최대 시도 횟수 초과 방지
- [x] 범위를 벗어난 답변 페이지의 전체 건수 유지
- [x] 동시성 테스트의 제한 시간·작업 취소·종료 처리 개선
- [x] 수정 범위의 이름·메서드 가독성 정리
- [x] 관련 테스트 및 전체 테스트 검증

- 제외: PostgreSQL 관련 코드·schema 변경 및 실 DB 검증
- 완료 기준: 재현 테스트 실패 확인 → 최소 수정 → 관련·전체 테스트 통과
- 검증 근거: [코드 리뷰 후속 검증](verification.md#코드-리뷰-후속-검증)
- 평가: 최초 포함 3회 제한을 Evaluation이 소유. 만료 후 소진 시 FAILED, 살아 있는 lease와 종료 상태는 유지
- 페이지: Page.map으로 원래 전체 건수 유지. 빈 ID 목록은 상세 Repository에서 조회 생략
- 동시 작업: 두 작업에 공통 15초 deadline, 완료 순서로 오류 확인, 취소 후 최대 5초 종료 대기. 최초 예외 보존
- 한계: interrupt에 응답하지 않는 작업은 강제 종료 보장 불가. 종료 확인 실패를 예외로 보고하며, 해당 실패 시 후속 DB 정리의 안전성 보장 불가

## 패키지 책임 정리

- 승인 범위: learning.mastery / learning.answer, HTTP DTO 분리, 평가 결과 소유권, 완료 반영 의존 방향, 답변 API 입력 소유권
- 유지: DB 테이블·컬럼·JSON·URL·계산식·평가 완료와 상태 반영의 원자성. PostgreSQL 변경 제외
- 실행: 계획 작성·병렬 작업 스킬 적용. HTTP 결과 분리는 독립 작업자로 분담, 평가 경계 정리는 주 작업자 담당. 패키지 이동은 통합 시 순차 실행
- [x] 기준선 297개 테스트 재확인
- [x] learning 서비스별 service.result: AnswerResult / AnswerEvaluationResult / KnowledgeStatesResult / RecommendationResult / LearningProgressResult 추가. Controller에서 기존 response로 변환, Service의 controller import 제거
- [x] evaluation.port의 EvaluationResult / ConceptResult를 evaluation.domain으로 이동. 도메인 결과를 port와 adapter가 사용, domain → port 의존 제거
- [x] evaluation이 소유한 완료 반영 port와 mastery adapter로 의존 역전. processor가 학습 Service 직접 참조하지 않도록 정리. 기존 완료 transaction 안에서 동기 실행
- [x] 답변 Controller의 QuestionIdRequest를 해당 답변 request 패키지에서 소유
- [x] production/test의 learning.knowledge → learning.mastery, learning 루트 answer 계층 → learning.answer 이동. 이전 디렉터리 중 비어 있는 것만 제거
- [x] 관련 API 계약·실제 저장·롤백·동시성 테스트와 전체 test bootJar 재검증
- [x] 현재 문서의 패키지 경로·책임 갱신. 검증 결과는 verification.md에 기록

검증 명령: `./gradlew test bootJar --rerun-tasks --console=plain`. 동작 유지 이동은 GREEN → REFACTOR → GREEN으로 검증. 새로운 오류 방어가 필요하면 해당 동작의 RED부터 추가. commit 없음.

## 미사용 코드·포맷·import 정리

- [x] 호출 없는 메서드·생성자 제거. 프레임워크 진입점·테스트 사용 메서드 유지
- [x] 운영·테스트 Java에 기존 Wooteco 스타일 적용
- [x] 미사용·중복·같은 패키지 import 제거, static 우선 정렬
- [x] 전체 test bootJar 및 포맷 재검증
- 범위: Java 소스. API·DB 동작 변경, 자료 삭제, commit 없음
