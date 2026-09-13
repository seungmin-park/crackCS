# Phase 6 구현·검증

## 작업별 커밋 전 재검증 — 2026-09-13 20:16 KST

- `./gradlew test bootJar --rerun-tasks --console=plain`: 62개 클래스, 297개 테스트 성공, 실패·오류·skip 0. 7개 task 실행
- `front`에서 `npm test`: 21개 파일, 123개 테스트 성공
- `front`에서 `npm run build`: 타입 검사·production build 성공
- 기존 deprecated API·JVM CDS·Node localstorage 경고 유지. 이번 작업에서 해결하지 않은 경고
- 검증 범위: 현재 작업 트리. 분리된 각 중간 커밋을 별도 checkout하여 실행한 검증은 아님
- 커밋 제외: `docs/retrospectives/`, `tobyteam/`. 골든 셋·문제 자료 변경 없음

## 미사용 코드·포맷·import 정리 검증

- 제거: TopicRepository.existsByParentId, EvaluationResponse의 6인자 편의 생성자. 소스·테스트 호출 없음 확인
- 보존: Spring 스케줄러·Controller·예외 처리기·Security 콜백·JPA/Lombok 생성 경로·테스트에서 사용하는 메서드
- 포맷: IntelliJ IDEA 2024.2.6 CLI, 기존 `/Users/seungmin/Downloads/intellij-java-wooteco-style.xml` 적용. 운영·테스트 Java 270개 처리
- 실행 격리: 실행 중 IDE와 충돌해 임시 config/system으로 포맷터 실행. 사용자 IDE 종료·설정 변경 없음
- import: 미사용/같은 패키지 import 5개 제거, 중복 제거 및 static 우선 사전순 정렬. 정리 스크립트 재실행 시 제거 0개·변경 0개
- 포맷 검증: 같은 포맷터의 `-r -d -m '*.java' src/main/java src/test/java` 검사 결과 270개 모두 well formed, exit 0
- 전체 검증: `./gradlew test bootJar --rerun-tasks --console=plain` 성공. 62개 클래스 / 297개 성공, 실패·오류·skip 0
- 정적 검증: git diff --check 통과, wildcard import·var 없음
- 경계: 메서드 참조 검색은 간접 호출을 완전히 증명하지 못하므로 확실한 미사용 항목만 제거. 테스트 사용 메서드까지 운영 호출이 없다는 이유로 제거하지 않음
- 동작 변경 없음: API·DB·자료 보존. 프런트·PostgreSQL 별도 검증 및 commit 없음
- 경고: 기존 deprecated API/JVM CDS, 독립 IntelliJ 시작 시 plugin 관련 경고. 빌드·포맷 검증 종료 코드 0

## 패키지 책임 정리 검증

- 현재 패키지: content.knowledge는 근거 콘텐츠, learning.answer는 답변, learning.mastery는 숙련 상태·판정 반영, recommendation/progress는 기존 책임 유지
- HTTP 경계: service.result 5종 소유권 분리, Controller에서 기존 JSON 응답으로 변환. Service → controller import 0건
- 평가 경계: EvaluationResult / ConceptResult는 evaluation.domain으로 이동. domain → evaluation.port import 0건
- 완료 반영: Processor → EvaluatedConceptApplicationPort ← mastery Adapter → KnowledgeStateService. 평가의 mastery 직접 import 0건, 동기·동일 transaction 반영 유지
- 답변 입력: AnswerQuestionIdRequest를 learning.answer.controller.request에서 소유. 기존 경로·검증 메시지 유지
- 기준선: `./gradlew test --console=plain` 성공, 기존 297개 테스트
- 중간 검증: `./gradlew test --tests 'com.example.crackcs.learning.*' --console=plain` — 19개 클래스 / 103개 성공
- 최종 검증: `./gradlew test bootJar --rerun-tasks --console=plain` — 62개 클래스 / 297개 성공, 실패·오류·skip 0, 실행 JAR 빌드 성공
- 방식: 동작 유지 GREEN → REFACTOR → GREEN. 신규 동작 RED로 집계하지 않음. 학습 현황 MVC의 최근 평가·토픽·개념 JSON 검증 보강
- 정적 점검: 이전 Java 패키지 참조 0건, package 선언과 파일 경로 불일치 0건, 빈 production/test 디렉터리 0건, git diff --check 통과
- JAR 점검: 이전 learning/knowledge·루트 learning 계층·port 결과 클래스 잔존 없음
- 독립 검토: 승인한 패키지·DTO·Port 변경에 Critical / Important / Minor 지적 없음. JSON 변환·MANDATORY 참여·AI 결과 재사용·테이블 유지 확인
- 유지: DB 테이블·컬럼·계산식·외부 URL·JSON. 골든 셋·문제 데이터 보존. PostgreSQL 변경·실 DB 검증·commit 없음
- 경계: 기존 Answer/Evaluation 엔티티 참조 유지. 기능 패키지 정리이며 독립 배포 모듈 분리 아님. 프런트 변경·재실행 없음
- 기존 경고: deprecated API / JVM CDS 경고 유지
- 이력 안내: 아래 최초 구현·구조 정리 기록의 예전 package 이름은 당시 증거. 현재 위치는 이 절과 [패키지 책임 정리](refactoring.md#패키지-책임-정리) 기준

## 코드 리뷰 후속 검증

- 범위: lease 복구 시 시도 상한, 빈 답변 페이지 전체 건수, 동시성 테스트 종료·가독성
- 제외: PostgreSQL 관련 코드·schema 변경 및 실 DB 검증. 기존 자료·사용자 변경 보존, commit 없음
- RED 1: EvaluationTest + 페이지 재현 테스트 17개 실행, 2개 실패. 네 번째 선점 true / 전체 건수 0 확인
- RED 2: 두 동시성 테스트의 작업 실패·상대 작업 취소 재현 2개 실행, 2개 실패. 상대 작업이 interrupt 없이 종료
- GREEN: 관련 네 클래스 전체 통과. 추가 경계 검증은 최초부터 통과한 회귀 검증으로 구분
- 추가 검증: 세 번째 유효 lease 유지, 만료 복구 FAILED commit·추가 provider 호출 없음, 종료 상태 재처리 무효, 제한 시간 초과 시 취소
- 전체 실행: `./gradlew test bootJar --rerun-tasks --console=plain`
- 결과: 62개 클래스, 297개 테스트 성공, 실패·오류·skip 0. 실행 JAR 빌드 성공
- 경고: 기존 deprecated API 및 JVM CDS 경고 유지
- 정적 점검: `git diff --check` 통과. Java 본문의 JUnit·AssertJ 완전 수식명 및 var 없음
- API 문서: ATTEMPTS_EXHAUSTED 추가, 기존 저장 실패 코드 PERSISTENCE_ERROR / PERSISTENCE_CONFLICT 누락 보완
- 실행 경계: H2 test create-drop DB. 프런트 변경·재실행 없음
- 종료 경계: interrupt에 응답하지 않는 작업은 강제 종료 불가. 최대 5초 종료 확인 실패를 원래 예외의 suppressed 예외로 보고. 이 경우 DB teardown 안전성 미보장
- 체크리스트: [코드 리뷰 후속 수정](refactoring.md#코드-리뷰-후속-수정)

- 상태: Phase 6 기능·후속 [구조 정리](refactoring.md) 구현 및 검증 완료 (2026-09-13)
- 기준: [작업 목록](../../planning/tasks.md#phase-6--knowledge-state와-개인-추천), [구현 계획](plan.md)
- 시작: `feat/phase6`, `d7815cf` (Phase 5 코드 + 문제·골든 셋 자료)
- 원본 자료: 변경 없음. 골든 셋 독립 검수·실모델 품질 검증과 기능 검증 구분

## 최초 구현 검증 이력

아래 최초 실행 명령·클래스명은 당시 증거. 후속 package 변경 후 현재 실행 명령과 결과는 이 문서의 구조 정리 검증에 기록.

### 책임과 흐름

```text
content.knowledge 문서/Chunk → 평가 근거
                             ↓
평가 완료 + 유효한 개념 판정
          ↓ 하나의 트랜잭션
적용 기록(판정 ID 유일) + KnowledgeState(@Version)
          ↓
현재 회원 상태 → Topic 집계 → 추천·학습 홈·지식 지도
```

- 상태 계산·유효 입력: KnowledgeState 책임
- 저장·중복 방지·충돌 재시도: KnowledgeStateService 책임
- 현재 상태·답변 집계·추천 조합: 조회 Service 책임
- 기준 공식·재계산 정책: [콘텐츠·AI 정책](../../product/content-and-ai-policy.md#지식-상태-공식--oq-004)
- HTTP: [OpenAPI](../../../openapi.yml). 사용자 ID는 현재 인증에서 획득

## 프런트 검증

- RED: 새 화면·경로·로그인 이동 12개 실패 / 기존 6개 통과. 컴파일 성공 상태에서 기능 누락 확인
- GREEN: 같은 범위 18개 통과
- 메뉴 RED: 학습 홈·지식 지도 링크 누락 1개 실패 → 추가 후 통과
- 전체: `npm test` — 21개 파일·123개 통과
- 빌드: `npm run build` — 타입 검사·Vite 빌드 성공
- API 2개 테스트: 동일 출처 세션 GET·401 전파의 기존 동작 확인. 초기 통과이며 RED로 집계하지 않음
- 독립 검토: 미평가 Concept 신뢰도0 표시 누락, 메뉴 접근성 이름 수정
- 검토 회귀: 2개 실패/14개 성공 → 수정 후 16개 성공. 수정 후 타입 검사·빌드 성공, 재검토 PASS
- 우선 학습 개념 표시: 신규 회원 테스트 1개 RED → 개념명 명시 후 전체 123개·타입 검사·빌드 재통과

## 백엔드 검증

- 신규 관련 테스트: `./gradlew test --tests 'com.example.crackcs.knowledge.*' --console=plain` — 41개 성공
- 도메인 RED: 초기 7개 중 기능 누락 6개 실패 → 7개 성공
- 저장 RED: 초기 5개 중 4개 실패 → 저장·멱등 처리 후 성공
- 평가 완료 RED: 신규/기존 상태의 동시 완료 2개 실패 → 원자적 연동 후 성공
- 조회 RED: 초기 9개 중 유효 RED 8개. fixture 오류 1건은 수정 후 요구 동작 누락으로 실패 재확인
- MVC RED: 9개 중 API 누락·권한 경계 5개 실패 → 사용자별 계약 구현 후 성공
- 추천 정책 RED: 순수 비교 정책 3개 실패 → 우선순위 구현 후 성공
- 추가 검증: 같은 버전의 충돌 후 재시도2회·최종 관측3개, 선택 개념 NEEDS_REVIEW 제외, 비활성 개념 포함 문제 제외
- 조회 비용: 답변 이력이 늘어도 지식 지도는 현재 상태·활성 개념·활성 주제 3개 SQL, Answer entity 추가 로딩0
- API 통합: 실제 principal·DB로 추천 GET → 답변 POST → 평가 처리 → 상태/progress GET. 다른 회원 ID를 요청에 넣어도 소유권 유지
- 기존 DTO 회귀: Service 종료 후 설명 목록 직렬화 LazyInitializationException RED → 트랜잭션 내 List.copyOf로 해결
- 전체 첫 실행: 274개 중 답변 목록 SQL 제한 1개 실패. 설명 목록 초기화로 조회10회 발생
- 수정: 페이지 답변 ID에 해당하는 평가 근거·Chunk·문서를 일괄 조회. 기존 제한7회 유지, 임계값 완화 없음
- 수정 후 관련 검증: Phase 6 41개 + AnswerServiceTest12개 =53개 성공
- 전체 최종 실행: `./gradlew test --console=plain` — 274개 성공, 실패·오류·스킵0. 최종 XML 직접 집계 확인
- 실행 JAR: `./gradlew test bootJar --console=plain` 성공. 이 명령의 test는 직전 성공 결과 재사용, bootJar 실제 실행
- 스키마: member_id/concept_id 복합PK와 FK, version NOT NULL 확인
- 적격 판단: 기존 `Evaluation.isKnowledgeStateEligible()`에 위임. Service의 중복 상태 규칙 제거

## IntelliJ import 인식 점검

- 증상: `DefaultKnowledgeReadService`에서 knowledge 타입 미해결 오류 78개, 기타 문제 3개
- 분리 검증: `./gradlew compileJava --rerun-tasks --console=plain` 성공. 실제 소스·패키지 선언 존재
- IDE 조치: Repair IDE의 Refresh Indexable Files 후 증상 유지 → Rescan Project Indexes 후 오류 78개 소멸, 기타 문제 3개 유지
- 판단: IDE 심볼 인덱스 불일치. 최초 발생 계기는 미확정
- 추가 확인 한계: 평가 처리기 파일 열기 확인. 이후 UI 도구 시간 초과로 심볼 이동 최종 확인 미완료
- 코드 정리: KnowledgeState·KnowledgeApplication 필드의 완전 수식 타입명을 일반 import로 교체. PK·연관관계 동작 변경 없음
- 빈 디렉터리 제거: 테스트 benchmark·postgresql, 리소스 static·db/migration·templates. 포함 파일 없음
- 정리 후 검증: `./gradlew test --console=plain` 274개 성공, 실패·오류·스킵 0. `git diff --check` 통과

## 독립 검토 결과

- 프런트 명세·접근성: 누락 2건 보완 후 PASS
- 백엔드·계층 간 계약: 멱등성·경쟁·rollback·후보 제외·principal·NULL 계약 검토 PASS
- 최종 회귀 수정: 페이지 범위의 근거 일괄 조회·version NOT NULL·도메인 적격 정책 위임 재검토 PASS
- 커밋·push 없음. `feat/phase6` 작업 폴더에 변경 보존

## Gate 증거

| 기준 | 테스트 |
|---|---|
| UNKNOWN과 0 구분 | KnowledgeStateTest, KnowledgeReadServiceTest, LearningViews.test.ts |
| 중복 적용 차단 | KnowledgeStateServiceTest 반복·동시 재전달 |
| 동시 완료 유실 방지 | KnowledgeCompletionTest 신규/기존 상태 경쟁, KnowledgeStateServiceTest 버전 충돌 재시도 |
| 추천 결과·이유 | RecommendationPriorityTest, KnowledgeReadServiceTest, KnowledgeControllerTest |
| AC-001 최초 학습 / AC-004 미평가·취약 구분 | KnowledgeFlowTest의 실제 API→DB 흐름, LearningViews.test.ts |

## 문서·자료 확인

- OpenAPI YAML 파싱 및 내부 참조 213개 확인
- 수정한 기준·검증 문서의 로컬 링크 68개 확인
- `git diff --exit-code -- docs/content` 성공: 기준 자료 변경 없음

## 미검증 경계

- 실제 OpenAI 평가 정확도·비용·독립 골든 셋 검수
- 운영 PostgreSQL의 잠금·제약 동작, schema 배포·rollback
- 충돌이 8회 연속 발생하면 예외 전파. 평가 처리기의 기존 재시도·실패 정책으로 이어짐
- 도입 전 완료 평가 자동 소급 반영·운영 재계산 명령
- 실제 브라우저 전체 흐름: 이번 기록에서 실행 증거로 주장하지 않음
- 기존 경고: Node localStorage 관련 경고. 테스트 실패와 구분

## 구조 정리 검증

- 기준: [승인된 구조 정리 요구사항](refactoring.md). 위 최초 구현 기록의 이름·매핑·책임을 대체
- 상태: 도메인·조회·완료 트랜잭션·테스트 책임·이름 정리 구현 완료. 작업별 독립 검토 승인
- 패키지: 근거 자료는 content.knowledge, 회원 학습 상태는 learning.knowledge, 추천은 learning.recommendation, 현황은 learning.progress
- 식별: KnowledgeState의 자동 생성 PK + 회원·개념 UNIQUE. AppliedEvaluationConcept도 자동 생성 PK + 평가 개념 UNIQUE. 복합 ID와 중복 scalar FK 제거
- 조회: 명시적 Repository query와 typed projection. 평가 상세 두 fetch 조회의 반환값을 조합해 반환, 호출 Service도 반환값 사용
- 책임: 상태 계산은 KnowledgeState, 주제 집계는 TopicKnowledgeSummary. 완료 재시도는 EvaluationCompletionTransaction, 현재 transaction 반영은 KnowledgeStateService
- 오류: provider 실패와 저장 실패 분리. 영구 FK·NOT NULL·무관 UNIQUE는 즉시 PERSISTENCE_ERROR. 허용 UNIQUE·잠금 경쟁의 소진은 PERSISTENCE_CONFLICT
- 원자성: 실패한 완료 transaction 전체 rollback 후 새 transaction에서 재시도. 획득한 AI 결과 재사용
- RED: 연관관계 필수값·영구 제약 오류 방어, 실제 CHECK 저장 실패와 잠금 소진의 processor 오류 분류, 허용 UNIQUE 소진의 동시성 예외 계약
- GREEN: `./gradlew test --console=plain` — 284개 성공, 실패·오류·스킵 0 (테스트 정리 전 기준선)
- 독립 검토: 허용 UNIQUE 소진이 영구 실패로 오분류되는 지적 보완 후 재검토 승인
- UNIQUE 검증 범위: 실제 H2 UNIQUE 8회 소진의 예외·원인·rollback + 실제 processor 행 잠금 소진의 공통 동시성 catch. UNIQUE→processor 전체 경로 별도 재현은 없음
- 조회 비용: 지식 지도 3개 SQL, 답변 상세 페이지 최대 7개 유지. 현황은 독립 조회 Service 조합으로 회원 상태 SELECT 1개 증가
- 매핑 선택: 반영 기록은 ManyToOne + 명시적 이름 UNIQUE. Hibernate/H2의 OneToOne 자동 UNIQUE 이름 때문에 명시적 충돌 분류가 불안정한 문제 회피. DB의 논리적 1:1 유지
- 경계: 실제 PostgreSQL 동시 실행, 기존 local DB의 PK 전환은 미검증. test create-drop 검증을 운영 migration 증거로 간주하지 않음
- 이름: AppliedEvaluationConcept·AppliedEvaluationConceptRepository로 교체. 기존 knowledge_application 테이블·제약 이름 유지
- 테스트 책임: KnowledgeIntegrationSupport 제거. 각 클래스가 fixture·Repository·AfterEach 소유, Evaluation·Question cascade 삭제와 나머지 FK 역순 bulk 삭제 혼용
- 테스트 분리: KnowledgeQueryServiceTest·RecommendationServiceTest·LearningProgressServiceTest. 통계는 KnowledgeQueryCostTest·AnswerQueryCostTest에 격리
- 집계 단위 검증: TopicKnowledgeSummaryTest 5개. 미평가와 0점·빈 주제·평균 분모·전체 안정 상태 확인. 기존 동작 검증이므로 신규 기능 RED로 집계하지 않음
- JPA 경계: QuestionConceptMappingTest에 기존 저장·UNIQUE 검증 2개 보존. 실제 DB 매핑 복원·도메인 우회 제약 확인 목적 명시
- 프런트 재검증: `npm test -- --run` — 21개 파일·123개 성공. `npm run build` — 타입 검사·빌드 성공. 기존 localstorage-file 경고 유지
- 최종 재실행: `./gradlew test bootJar --rerun-tasks --console=plain` — 7개 task 실제 실행, BUILD SUCCESSFUL (2026-09-13 18:21 KST)
- XML 직접 집계: 62개 클래스·289개 성공, 실패·오류·스킵 0. 기존 284개 + 집계 단위 5개
- 정적 점검: 이전 타입·package 참조, var, wildcard import, 정리용 JDBC DELETE 0개. git diff --check 통과, docs/content diff 없음
- 경고 구분: 기존 OpenAiEvaluationAdapter deprecated API와 테스트 deprecated API, JVM CDS 경고. 이번 이름·테스트 책임 정리의 실패와 구분
- 최종 전체 검토: Approved, Critical 0·Important 0. Minor 1개(문서 진행 상태) 완료 문구로 수정
- 마지막 문서 점검: 로컬 링크 55개 대상 존재, git diff --check 통과. feat/phase6 유지, staging·commit·push 없음
