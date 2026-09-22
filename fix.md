# 도메인 네이밍·메서드 책임·SOLID 개선 체크리스트

> 현재 상태: 계획. 전체 코드의 파일·선언·명명 패턴 검색은 수행했지만, 모든 메서드 본문의 의미·책임 전수 판정과 구현은 완료되지 않음.

## 목표

- Java 운영·테스트 코드와 TypeScript/Vue 코드의 이름을 도메인 역할에 맞게 정리
- 메서드 이름과 실제 수행 내용 일치
- 메서드가 현재 클래스의 상태·책임과 맞는지 검증
- 함께 변하는 메서드가 별도 개념을 이루면 클래스로 추출
- Java 백엔드와 TypeScript의 SOLID 위반 개선
- 네이밍·책임 검사를 이후 모든 작업에서 반복하도록 `AGENTS.md`에 반영

## 적용 범위

- [ ] Java 운영 코드 234개 파일 검사
- [ ] Java 테스트 코드 82개 파일 검사
- [ ] 운영 TypeScript 24개 파일 검사
- [ ] Vue 25개 파일의 이름·메서드 책임 검사
- [ ] 프런트 테스트 29개 파일 검사
- [ ] `build/`, `front/dist/`, `front/node_modules/` 제외
- [ ] Vue 컴포넌트 자체는 SOLID 판정에서 제외
- [ ] Java 백엔드와 TypeScript에 SOLID 적용
- [ ] 프레임워크 필수 메서드명과 Spring Data 사용자 정의 Repository의 `Impl` 규칙은 예외로 기록
- [ ] 외부 HTTP·JSON 계약은 실제 도메인 오류가 없으면 유지

## 핵심 판단 기준

### 이름

- [ ] 타입과 객체 이름이 같은 역할을 가리키는지 확인
- [ ] Repository 객체가 엔티티 컬렉션처럼 보이는 복수 이름을 사용하지 않는지 확인
- [ ] Service·Port·Policy·Client·Mapper·Provider 이름에 대상과 역할이 드러나는지 확인
- [ ] 클래스명·메서드명·필드명·매개변수명·지역 변수명이 도메인 용어와 일치하는지 확인
- [ ] `data`, `result`, `item`, `value`, `response`, `process`, `handle`, `execute`, `load`, `submit`, `transition`이 넓은 문맥에서 의미를 숨기지 않는지 확인
- [ ] 컬렉션은 복수 도메인 명사, 단일 객체는 단수 도메인 명사 사용
- [ ] 이름 변경 시 선언부뿐 아니라 모든 호출부와 테스트 이름도 함께 검사

### 메서드 책임

- [ ] 메서드 이름만 보고 대상·행위·주요 부수 효과를 예상할 수 있는지 확인
- [ ] 실제 수행 작업을 검증·조회·계산·저장·상태 변경·외부 호출·로깅으로 분해
- [ ] 여러 작업이 하나의 유스케이스를 완성하는 응집된 단계인지 확인
- [ ] 서로 다른 변경 이유가 한 메서드에 섞였으면 메서드 분리
- [ ] boolean 인자로 서로 다른 동작을 선택하면 의도별 메서드 분리
- [ ] 조회처럼 보이는 메서드가 상태를 변경하지 않는지 확인
- [ ] 단일 대상 메서드가 다른 aggregate나 다른 버전까지 바꾸면 이름으로 부수 효과를 표현
- [ ] 단순히 길거나 private 메서드가 많다는 이유만으로 분리하지 않음

### 클래스 배치와 추출

- [ ] 판단에 필요한 상태를 현재 클래스가 소유하는지 확인
- [ ] 결정에 필요한 정보를 가장 잘 아는 객체가 판단하는지 확인
- [ ] 다른 객체의 상태를 가져와 대신 판단하는 메서드는 상태 소유 객체로 이동 검토
- [ ] 같은 데이터·정책·변경 이유를 공유하는 메서드 묶음은 별도 클래스 추출 검토
- [ ] 외부 I/O와 도메인 판단이 결합됐으면 Port/Adapter 또는 정책 객체로 분리
- [ ] 테스트할 때 관련 없는 의존성까지 준비해야 하면 책임 경계 재검토
- [ ] 유스케이스 조정 메서드는 여러 협력 객체 호출 자체를 SRP 위반으로 판정하지 않음
- [ ] 추출 후 새 클래스의 입력·출력·의존성을 이름만으로 설명할 수 있는지 확인

### SOLID

- [ ] SRP: 클래스가 하나의 변경 이유를 갖는지 확인
- [ ] OCP: 새 정책·provider 추가가 기존 orchestration 수정으로 번지지 않는지 확인
- [ ] LSP: 구현체가 인터페이스 계약과 예외·반환 의미를 지키는지 확인
- [ ] ISP: 호출자가 사용하지 않는 계약에 의존하지 않는지 확인
- [ ] DIP: 상위 유스케이스가 구체 외부 구현이나 직접 생성한 정책에 의존하지 않는지 확인

## 전수 감사 산출물

- [ ] 모든 대상 파일을 감사 목록에 기록
- [ ] 단순 DTO·enum·예외 클래스도 이름과 위치를 확인하고 `유지` 근거 기록
- [ ] 모든 비단순 메서드에 아래 판정 항목 기록
  - [ ] 이름이 약속하는 동작
  - [ ] 실제 수행 작업
  - [ ] 읽는 상태와 변경하는 상태
  - [ ] 호출하는 Repository·Service·Port·외부 I/O
  - [ ] 현재 클래스 배치가 적절한 이유 또는 부적절한 이유
  - [ ] `유지 / 이름 변경 / 메서드 분리 / 다른 객체로 이동 / 클래스 추출` 판정
  - [ ] 필요한 테스트와 실패 조건
- [ ] 명명 후보를 기계적 변경과 의미 변경으로 구분
- [ ] 책임 변경 후보를 확정 후보와 추가 확인 후보로 구분
- [ ] 유지하기로 결정한 큰 클래스·긴 메서드의 유지 이유 기록
- [ ] 감사 완료 전 `전체 검사 완료`로 표시하지 않음

## 작업 0. 변경 전 기준선

- [ ] `git status --short`로 기존 사용자 변경 확인
- [ ] 기존 `AGENTS.md`, `docs/retrospectives/`, `tobyteam/` 변경 보존
- [ ] 백엔드 기본 테스트 실행 및 테스트 수·성공·실패 기록
- [ ] 프런트 타입 검사 실행 및 결과 기록
- [ ] 프런트 테스트 실행 및 테스트 수·성공·실패 기록
- [ ] PostgreSQL·retrieval benchmark처럼 별도 환경이 필요한 검증 경계 기록

## 작업 1. Repository와 주입 객체 이름 정리

### 확정 후보

- [x] `RetrievalBenchmarkTest.retrieval` → `knowledgeRetrievalService`
- [x] `RetrievalBenchmarkTest.chunkService` → `knowledgeChunkService`
- [x] `RetrievalBenchmarkTest.chunks` → `knowledgeChunkRepository`
- [x] `RetrievalBenchmarkTest.documents` → `knowledgeDocumentRepository`
- [x] `RetrievalBenchmarkTest.topics` → `topicRepository`
- [x] `RetrievalBenchmarkTest.members` → `memberRepository`
- [x] `PostgresPersistenceTest.topics` → `topicRepository`
- [x] `JdkOpenAiResponsesClient.client` → `httpClient`
- [x] `OpenAiEvaluationAdapter.client` → `openAiResponsesClient`
- [x] `OpenAiFollowUpQuestionAdapter.client` → `openAiResponsesClient`
- [x] `OpenAiFollowUpQuestionAdapter.mapper` → `objectMapper`
- [x] `DefaultEvaluationBudgetGuard.policy` → `evaluationCostPolicy`
- [x] Follow-up Service의 `policy` → `followUpSourcePolicy`
- [x] `DefaultFollowUpQuestionProcessor.policy` → `followUpSourcePolicy`
- [x] `EvaluationAttemptExecutor.evaluationPorts` → `evaluationPortProvider`
- [x] `DefaultFollowUpQuestionProcessor.generators` → `followUpQuestionGeneratorProvider`

### 검증

- [x] Repository 타입 필드 중 엔티티 복수 이름 재검색
- [x] `client`, `mapper`, `policy`, `provider`, `service`, `repository` 단독 이름 재검색
- [x] Java 컴파일
- [x] 영향받은 테스트 실행

검증 결과(2026-09-22):

- `./gradlew compileJava compileTestJava --console=plain`: 성공
- `./gradlew test --tests 'com.example.crackcs.evaluation.adapter.openai.*' --tests 'com.example.crackcs.evaluation.service.*' --tests 'com.example.crackcs.learning.followup.adapter.*' --tests 'com.example.crackcs.learning.followup.service.*' --console=plain`: 성공
- `./gradlew test --rerun-tasks --console=plain`: 442개 성공, 실패 0개
- `./gradlew retrievalBenchmark --rerun-tasks --console=plain`: 1개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개
- 테스트의 `mapper`, `client`, `policy` 지역·필드 이름은 작업 2 범위로 유지

## 작업 2. 테스트 코드 도메인 이름 정리

### 인증·회원

- [x] `result`, `value`, `data`, `account`, `member`가 시나리오 역할을 드러내는지 검사
- [x] 생성 회원·인증 회원·차단 회원·관리자 이름 구분
- [x] `mapper` 같은 협력 객체를 `objectMapper`로 구체화

### 콘텐츠

- [x] 원본 문제·새 버전 문제·후속 문제 이름 구분
- [x] 작성자·검수자·학습자 역할 이름 구분
- [x] `data()` helper를 반환 개념이 드러나는 이름으로 변경
- [x] `saved`, `found`, `result`가 여러 대상을 가리키지 않는지 검사

### 평가·검색

- [x] `result`를 `evaluationResult`, `retrievalResult`, `benchmarkResult` 등으로 구분
- [x] 기준 자료와 DB 저장 자료 이름 구분
- [x] `rows`, `values`, `selected`를 실제 데이터 의미로 변경
- [x] benchmark helper 이름이 읽기·매핑·요약 책임을 드러내는지 확인

### 학습

- [x] 답변 제출 결과·평가 결과·후속 질문 생성 결과 이름 구분
- [x] 반복되는 `result()` helper를 생성하는 도메인 결과 이름으로 변경
- [x] `owner(source)`처럼 내부 구현을 읽어야 이해되는 helper 제거 또는 이름 변경
- [x] `complete(...)` helper가 준비·실행·검증을 함께 숨기는지 검사

### 검증

- [x] 패키지별 테스트 실행
- [x] 테스트 메서드 수·성공·실패 기록
- [x] 이름 변경 외 동작 변경이 없는지 diff 확인

검증 결과(2026-09-22):

- RED: helper·변수 선언 이름을 먼저 변경한 뒤 `./gradlew compileTestJava --console=plain` 실행, 남은 기존 참조로 컴파일 오류 62개 확인
- GREEN: 모든 참조를 역할 이름으로 맞춘 뒤 `./gradlew compileTestJava --console=plain` 성공
- 인증·회원·공통 패키지: 54개 성공, 실패 0개
- 콘텐츠 패키지: 119개 성공, 실패 0개
- 평가·검색 패키지: 78개 성공, 실패 0개
- 학습 패키지: 190개 성공, 실패 0개
- `./gradlew test --rerun-tasks --console=plain`: 442개 성공, 실패 0개
- `./gradlew localServiceLatencyBenchmark --rerun-tasks --console=plain`: 1개 성공, 실패 0개
- `./gradlew retrievalBenchmark --rerun-tasks --console=plain`: 1개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개
- 테스트 코드 45개 파일의 역할 이름만 변경. 운영 코드·fixture 값·assertion 의미 변경 없음
- `MemberTest`의 `member`는 단일 테스트 대상 자체를 가리키므로 유지

## 작업 3. 운영 코드 도메인 타입과 필드 이름 정리

- [x] `KnowledgeDocumentData`가 초안 입력·수정 명령 중 어떤 개념인지 확정
- [x] 역할이 입력 명세라면 `KnowledgeDocumentDraft` 또는 명확한 command 이름 검토
- [x] `QuestionConceptData`가 평가 기준 입력인지 연결 정보인지 확정
- [x] 필요 시 `QuestionConceptCriterion` 등 도메인 이름으로 변경
- [x] `EvaluationAttempt`가 시도인지 시도 결과인지 확인하고 이름 확정
- [x] `ClaimedEvaluationWork`가 선점된 실행 스냅샷 역할을 충분히 표현하는지 확인
- [x] `FollowUpQuestionResult`와 `FollowUpResult`의 역할 차이가 이름으로 드러나는지 확인
- [x] Service의 `response(...)`를 `toXxxResult(...)`로 변경
- [x] `unavailable()`을 반환 도메인이 드러나는 이름으로 변경
- [x] `model`을 `modelName`으로 변경할지 외부 계약과 함께 확인
- [x] 타입 이름 변경 시 production·test·직렬화 호출부 전부 수정

결정 결과(2026-09-22):

- `KnowledgeDocumentData` → `KnowledgeDocumentDraft`: 생성·수정·새 버전에 공통으로 전달하는 문서 초안
- `QuestionConceptData` → `QuestionConceptCriterion`: 개념 ID와 평가 가중치·필수 여부를 묶은 평가 기준
- `EvaluationAttempt` → `EvaluationAttemptOutcome`: 평가 시도 자체가 아니라 완료·검토·재시도 결과
- `ClaimedEvaluationWork` 유지: 선점된 평가의 식별자·입력 스냅샷·시도 횟수를 함께 표현
- `FollowUpResult` → `FollowUpGenerationResult`: 생성기 출력과 조회 유스케이스의 `FollowUpQuestionResult` 구분
- Service의 `response(...)` → `toAnswerResult(...)`, `toTopicState(...)`, `toRecommendationResult(...)`
- 추천 Service의 `unavailable()` → `noAvailableQuestionResult()`
- `FollowUpGeneration.unavailable(...)`, `FollowUpQuestionResult.unavailable(...)` 유지: 각각 상태 변경과 반환 타입이 호출부에 드러남
- OpenAI adapter·평가 logger의 내부 `model` → `modelName`; 외부 JSON 키 `model`과 설정 키 유지

검증 결과(2026-09-22):

- RED: `KnowledgeDocumentDraft`를 요구하는 테스트 컴파일에서 누락 타입 오류 1개 확인
- RED: `FollowUpGenerationResult`를 요구하는 테스트 컴파일에서 누락 타입 오류 6개 확인
- `./gradlew compileJava compileTestJava --console=plain`: 성공
- 콘텐츠·평가·학습 패키지 테스트: 387개 성공, 실패 0개
- `./gradlew test --rerun-tasks --console=plain`: 442개 성공, 실패 0개
- `./gradlew retrievalBenchmark --rerun-tasks --console=plain`: 1개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개
- `./gradlew localServiceLatencyBenchmark --rerun-tasks --console=plain`: 1개 성공, 실패 0개
- 네 개의 이름 변경 타입 구현과 전체 문자열 리터럴 동일 확인

## 작업 4. Idempotency-Key 검증 책임 이동

### RED

- [x] body DTO 없이도 헤더 형식 검증 계약을 보여주는 최소 테스트 작성
- [x] 잘못된 UUID, 대문자 UUID, 누락 헤더 실패 테스트 작성
- [x] 기존 `AnswerSubmitRequest`가 외부 header를 검증하지 않아야 한다는 구조 확인
- [x] 테스트가 누락된 책임 때문에 실패하는지 확인

### GREEN

- [x] `AnswerSubmitRequest.validatedIdempotencyKey(String)` 제거
- [x] Idempotency-Key 형식·정규화를 소유할 값 객체 또는 HTTP 경계 validator 추가
- [x] `AnswerController`가 body와 header를 독립적으로 검증해 Service에 전달
- [x] 최소 구현으로 관련 Controller 테스트 통과

### REFACTOR

- [x] 값 객체 이름과 패키지가 HTTP 전용인지 도메인 공용인지 책임에 맞게 조정
- [x] Controller에 정규식·오류 메시지가 다시 분산되지 않았는지 확인
- [x] 관련 테스트 재실행

결정 결과(2026-09-22):

- `controller.request.IdempotencyKeyHeader` 추가: HTTP 헤더의 소문자 canonical UUID 계약 소유
- `AnswerSubmitRequest`: body의 `content` validation만 소유
- `AnswerController`: header 값 객체와 body DTO를 독립적으로 검증한 뒤 Service에 문자열 전달
- `Answer`: HTTP 경계를 우회한 생성도 막도록 기존 canonical UUID 불변식 유지
- 누락 헤더: 기존 `MissingRequestHeaderException` 공통 처리 유지
- Controller·DTO의 정규식과 헤더 오류 메시지 제거

검증 결과(2026-09-22):

- RED: `IdempotencyKeyHeader`가 없어 테스트 컴파일 오류 4개 확인
- `IdempotencyKeyHeaderTest`: 3개 성공, 실패 0개
- `AnswerControllerTest`: 23개 성공, 실패 0개
- 답변 패키지 테스트: 52개 성공, 실패 0개
- `./gradlew test --rerun-tasks --console=plain`: 446개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개

## 작업 5. Question·KnowledgeDocument 공개 버전 책임 정리

### 설계 확인

- [x] `publish`가 대상 공개와 이전 공개 버전 폐기를 함께 수행한다는 부수 효과 기록
- [x] 두 상태 변경이 하나의 원자적 유스케이스임을 테스트로 고정
- [x] public 메서드를 둘로 나눠 불변식을 깨지 않도록 설계
- [x] `publishAsCurrentVersion` 이름 변경과 version publication 객체 추출 비교
- [x] Question과 KnowledgeDocument 사이의 성급한 공용 추상화 금지

### 구현

- [x] 선택한 이름이 기존 공개 버전 폐기까지 표현
- [x] 필요 시 Question version publication 책임 추출
- [x] 필요 시 KnowledgeDocument version publication 책임 추출
- [x] domain 상태 전이는 각 aggregate가 계속 소유
- [x] Service는 조회·트랜잭션·유스케이스 조정만 소유

### 검증

- [x] 새 버전 공개 시 이전 버전 폐기 테스트
- [x] 공개 실패 시 이전 버전 상태가 유지되는 테스트
- [x] 동시 공개 경계의 미검증 범위 기록

결정 결과(2026-09-22):

- Service 공개 계약: `publish` → `publishAsCurrentVersion`
- 의미: 대상 공개와 같은 버전 계열의 기존 공개본 폐기를 한 번에 수행
- 공개·폐기 분리 public 메서드 미도입: 호출자가 일부 단계만 실행해 현재 공개본 불변식을 깨는 경로 차단
- `@Transactional` Service 메서드 하나가 조회·대상 공개·이전 공개본 폐기를 원자적 유스케이스로 조정
- `Question.publish`·`Question.retire`, `KnowledgeDocument.publish`·`KnowledgeDocument.retire` 유지: 상태와 검증 규칙을 소유한 aggregate가 상태 전이 담당
- Question·KnowledgeDocument publication 객체 미추출: 현재 조정 흐름이 짧고 독립 정책이나 재사용 경계가 없음
- 공용 publication 추상화 미도입: 두 aggregate의 상태 타입과 공개 조건이 달라 변경 이유가 같지 않음

```text
Controller
    ↓
publishAsCurrentVersion(id)  [한 트랜잭션]
    ├─ target.publish()      [aggregate 검증 + 상태 전이]
    ├─ 같은 계열 PUBLISHED 조회
    └─ previous.retire()     [aggregate 상태 전이]
    ↓
전체 commit 또는 rollback
```

검증 결과(2026-09-22):

- RED: 두 Service에 `publishAsCurrentVersion(Long)` 계약이 없어 테스트 컴파일 오류 9개 확인
- `QuestionServiceTest`·`KnowledgeDocumentServiceTest`: 15개 성공, 실패 0개
- 성공 경로: 새 버전 `PUBLISHED`, 이전 공개본 `RETIRED`를 트랜잭션 종료 후 DB 재조회로 확인
- 실패 경로: 검수되지 않은 새 버전 공개 실패 후 새 버전 `DRAFT`, 이전 공개본 `PUBLISHED` 유지 확인
- `./gradlew test --rerun-tasks --console=plain`: 448개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개
- 미검증 경계: 같은 계열의 두 초안을 동시에 공개하는 경쟁 조건
- 현재 위험: 계열 단위 잠금과 현재 공개본 유일 제약이 없어 두 트랜잭션이 각각 `PUBLISHED`로 완료될 가능성
- 후속 선택지: 버전 계열 단위 비관적 잠금 또는 DB 유일 제약·재시도 정책을 schema migration 도입과 함께 결정

## 작업 6. Question Concept 연결 책임 정리

- [x] `DefaultQuestionService.toAssignment`의 실제 책임 목록 작성
- [x] Service와 `Question.replaceConcepts`의 활성 Concept·Topic 일치 검증 중복 확인
- [x] Service는 Concept 조회와 assignment 조립만 수행하도록 축소
- [x] 연결 가능성 최종 판단은 `Question`이 소유
- [x] `toAssignment`를 의도가 드러나는 이름으로 변경
- [x] 비활성 Concept 실패 테스트 유지
- [x] 다른 Topic Concept 실패 테스트 유지
- [x] 중복 Concept 실패 테스트 유지

결정 결과(2026-09-22):

- 기존 `toAssignment`: Concept 조회, 미존재 예외 변환, 활성 상태 검사, Question과 Topic 일치 검사, assignment 조립 담당
- 검증 중복 확인: Service와 `Question.requireAssignableConcept`가 활성 상태·Topic 일치를 같은 메시지로 각각 검사
- `toAssignment` → `resolveConceptAssignment`: criterion의 Concept ID 조회와 `QuestionConceptAssignment` 조립만 담당
- `Question.replaceConcepts`: DRAFT 상태, null, Concept 활성 상태, Topic 일치, 중복 Concept, 가중치 검증 후 전체 교체 담당
- `Question.hasSameTopicAs`: Service용 public 보조 계약 제거, aggregate 내부 검증 메서드로 축소
- 별도 연결 정책 객체 미추출: 판단에 Question의 Topic과 기존 상태가 필요하므로 상태 소유 aggregate가 규칙도 소유

```text
QuestionConceptCriterion
        ↓ ID 해석
DefaultQuestionService.resolveConceptAssignment
        ↓ Concept + weight + required 조립
Question.replaceConcepts
        ├─ 연결 가능성 전체 검증
        └─ 검증 성공 후 기존 연결 교체
```

검증 결과(2026-09-22):

- 순수 책임 이동으로 새 외부 동작 없음: private 구조를 검사하는 인위적 RED 테스트 미추가
- mutation 확인: Question의 활성·Topic·중복 방어를 임시 제거했을 때 대응 테스트 3개가 각각 예상 실패
- 임시 mutation 복구 후 `QuestionTest` 25개, `QuestionServiceTest` 11개 성공
- 비활성 Concept: 도메인 단위 테스트와 Service 통합 테스트 유지
- 다른 Topic·중복 Concept: 도메인 단위 테스트 유지
- 부분 수정 방지: 여러 교체 입력 중 후속 검증 실패 시 기존 연결·수정 시각 유지 테스트 유지
- `./gradlew test --rerun-tasks --console=plain`: 447개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개

## 작업 7. 문서 내용 정규화·checksum 책임 통합

### RED

- [x] 줄바꿈 차이와 앞뒤 공백이 같은 문서로 판정되는 테스트 확인 또는 추가
- [x] 생성·수정·중복 조회가 같은 정규화 규칙을 쓰는 테스트 추가
- [x] 잘못된 내용에서 checksum만 먼저 계산해 부분 처리되지 않는 테스트 확인

### GREEN/REFACTOR

- [x] `DefaultKnowledgeDocumentService.ensureUniqueContent`의 정규화 중복 제거
- [x] 정규화된 내용과 checksum을 함께 소유할 값 객체 또는 도메인 정책 검토
- [x] `KnowledgeDocument`와 Service가 동일한 계산 경로 사용
- [x] Service는 checksum 중복 조회만 조정
- [x] 기존 DB column과 API 계약 유지

결정 결과(2026-09-22):

- `KnowledgeDocumentContent` 값 객체 추가: 정규화된 원문과 그 값으로 계산한 checksum을 한 쌍으로 소유
- 생성 경로 제한: private 생성자와 `from(rawContent)` 정적 팩터리 사용
- 정규화 규칙: CRLF·CR을 LF로 통일한 뒤 앞뒤 공백 제거
- 잘못된 원문: null·공백 입력은 checksum 계산·Repository 조회·Entity 변경 전에 거부
- `KnowledgeDocument`: 생성과 수정 모두 값 객체가 제공한 content/checksum을 함께 반영
- `DefaultKnowledgeDocumentService`: 값 객체 생성 후 checksum 중복 조회만 조정
- `ensureUniqueContent` → `ensureUniqueChecksum`: 문자열 정규화·hash 계산 제거
- `ContentChecksum` 유지: KnowledgeChunk도 사용하는 범용 SHA-256 도구이며 문서 정규화 정책은 소유하지 않음
- DB `content`·`checksum` column과 HTTP 요청·응답 필드 변경 없음

```text
raw content
    ↓
KnowledgeDocumentContent.from
    ├─ null·blank 검증
    ├─ 줄바꿈·앞뒤 공백 정규화
    └─ 정규화된 값의 checksum 계산
          ↓
    content + checksum
       ├─ Service: checksum 중복 조회
       └─ Entity: 두 필드 함께 저장·수정
```

검증 결과(2026-09-22):

- RED: `KnowledgeDocumentContent`가 없어 값 객체 테스트 컴파일 오류 3개 확인
- `KnowledgeDocumentContentTest`: 정규화·고정 SHA-256·공백 입력 거부 2개 성공
- `KnowledgeDocumentTest`: 생성·수정 정규화와 checksum 일치, 실패 시 content·checksum·updatedAt 유지 확인
- `KnowledgeDocumentServiceTest`: 생성·수정 시 줄바꿈·앞뒤 공백이 같은 중복으로 판정되고 실패 대상 상태 유지 확인
- 관련 값 객체·도메인·Service 테스트: 17개 성공, 실패 0개
- `./gradlew test --rerun-tasks --console=plain`: 450개 성공, 실패 0개
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개

## 작업 8. KnowledgeChunk 분할 정책 추출

- [x] `generateChunks`의 조회·검증·재사용·분할·저장 단계를 구분
- [x] `new KnowledgeChunker(1000, 150)` 직접 생성 제거
- [x] max length·overlap·policy version을 한 정책 객체가 소유
- [x] `KnowledgeChunker` 또는 새 Port를 Service에 주입
- [x] Service는 유스케이스 조정과 저장만 수행
- [x] 동일 generation key 재사용 테스트
- [x] 정책 변경 시 새 generation key 생성 테스트
- [x] 문단 경계·overlap 단위 테스트

결정 결과(2026-09-22):

- `KnowledgeChunker` → `KnowledgeChunkPolicy`: max length·overlap·policy version·분할·generation key 계산 소유
- 기본 정책 Bean: max length 1000, overlap 150, `paragraph-1000-overlap-150-v1`
- `KnowledgeChunkConfiguration`: 운영 기본 정책 조립
- `DefaultKnowledgeChunkService`: 정책 Bean 주입, 직접 생성과 정책 상수 제거
- Service 흐름: 공개 문서 조회·검증 → 기존 결과 재사용 → 정책 분할 → Chunk 조립 → 저장
- 기존 Chunk가 있는 문서: 평가 근거 보존을 위해 저장된 generation key와 Chunk 재사용
- 정책 변경: 아직 Chunk가 없는 문서는 새 policy version을 포함한 새 generation key 사용
- 기존 문서의 여러 정책 세대 동시 저장 미도입: DB 유일 제약과 검색 대상 세대 선택 변경이 함께 필요하므로 범위 제외

```text
KnowledgeChunkConfiguration
        ↓ 기본값 조립
KnowledgeChunkPolicy
        ├─ maxLength
        ├─ overlap
        ├─ policyVersion
        ├─ split(content)
        └─ generationKey(documentChecksum)
                 ↓ 주입
DefaultKnowledgeChunkService
        ├─ 문서 조회·공개 상태 검증
        ├─ 기존 결과 재사용
        ├─ Chunk 조립
        └─ 저장
```

검증 결과(2026-09-22):

- RED: `KnowledgeChunkPolicy`가 없어 정책 테스트 컴파일 오류 1개 확인
- `KnowledgeChunkPolicyTest`: 문단 경계·overlap·고정 generation key·버전 변경 key 4개 성공
- `KnowledgeChunkServiceTest`: 생성 key와 저장 Chunk key 일치, 동일 요청의 key·Chunk 재사용 확인
- 관련 정책·도메인·Service 테스트: 9개 성공, 실패 0개
- `./gradlew test --rerun-tasks --console=plain`: 452개 성공, 실패 0개
- `./gradlew retrievalBenchmark --rerun-tasks --console=plain`: 1개 성공, 실패 0개
- H2 retrieval: policy version 유지, Recall@K 1.0, 무관 Chunk 비율 0, 빈 결과 0
- `./gradlew postgresTest --rerun-tasks --console=plain`: 37개 성공, 실패 0개

## 작업 9. Evaluation 완료·실패 처리 책임 분리

### RED

- [x] 성공 완료 시 평가 결과와 지식 상태가 함께 반영되는 테스트 유지
- [x] 무결성 오류가 영구 실패로 바뀌는 테스트 확인
- [x] 동시성 충돌이 재시도 또는 최종 실패로 바뀌는 테스트 확인
- [x] invalid result가 재시도 정책을 따르는 테스트 확인
- [x] lease 소유자가 아니면 적용하지 않는 테스트 확인

### 책임 분리

- [x] `DefaultEvaluationProcessor.complete`가 수행하는 성공·예외·상태 전이·로그 작업 분해
- [x] `complete`보다 실제 의도가 드러나는 이름으로 변경
- [x] `EvaluationCompletionTransaction`의 기존 책임과 새 책임 경계 확정
- [x] 필요 시 `EvaluationCompletionCoordinator` 추출
- [x] 필요 시 실패 코드와 retry/fail 결정을 담당하는 정책 추출
- [x] Processor는 선점 → 시도 → 결과 적용 흐름만 조정
- [x] Evaluation 상태 불변식은 `Evaluation`이 계속 소유

### 검증

- [x] 평가 관련 단위 테스트
- [x] 평가 Service 통합 테스트
- [x] 지식 상태 원자성 테스트
- [x] worker scheduling 테스트

결정 결과(2026-09-22):

- `EvaluationOutcomeCoordinator` 추출: 완료·검토·재시도 결과를 lease 소유자에게만 적용하고 결과 로그 기록
- `DefaultEvaluationProcessor`: 로컬 중복 실행 방지 → 선점 → 외부 평가 시도 → 결과 적용 위임만 조정
- `EvaluationCompletionTransaction`: 새 트랜잭션과 학습 상태 저장 충돌 재시도만 소유
- `Evaluation`: 완료·검토·재시도·실패 상태 전이와 시도 횟수 불변식 계속 소유
- `complete` 제거, 결과 종류를 드러내는 `applyCompleted`로 변경
- worker ID: 선점 주체인 Processor가 생성하고 결과 적용 시 명시적으로 전달
- 별도 실패 정책 미추출: 세 저장 예외의 코드·처리가 완료 적용 흐름 한 곳에서만 사용되며 아직 독립 변경 축이나 공유 요구 없음

```text
DefaultEvaluationProcessor
        선점 → 평가 시도 → 결과 적용 위임
                              ↓
                EvaluationOutcomeCoordinator
                ├─ lease 소유권 확인
                ├─ 완료·검토·재시도 적용
                ├─ 저장 예외 분류
                └─ 성공·실패 로그
                     ↓             ↓
EvaluationCompletionTransaction  Evaluation
새 트랜잭션·충돌 재시도          상태 불변식·전이
```

검증 결과(2026-09-22):

- 기존 공개 동작을 먼저 실행해 책임 이동 전 기준선 GREEN 확인
- `EvaluationTest`, `EvaluationCompletionTransactionTest`, `KnowledgeCompletionTest`, `KnowledgeCompletionFailureTest`, `WorkerSchedulingTest`: 31개 성공, 실패 0개
- 성공 결과와 지식 상태의 같은 트랜잭션 반영, 영구 무결성 오류, 저장 충돌, invalid result, lease 상실 계약 유지
- 새 내부 객체의 존재가 아니라 `EvaluationProcessor.process`에서 관찰되는 상태·로그를 검증
- `./gradlew test postgresTest --rerun-tasks --console=plain`: 기본 452개, PostgreSQL 37개 성공, 실패·오류·건너뜀 0개
- 기준 데이터 검증: 14개 검사 그룹 PASS

## 작업 10. Follow-up 선점·외부 실행·완료 책임 분리

### RED

- [ ] provider 없음·timeout·invalid result·provider error별 결과 테스트
- [ ] 저장 충돌 재시도 테스트
- [ ] source가 완료 전에 바뀌면 CONTENT_UNAVAILABLE이 되는 테스트
- [ ] lease 만료·다른 token·시도 횟수 소진 테스트

### 책임 분리

- [ ] `DefaultFollowUpQuestionProcessor.process`의 모든 수행 작업 목록 확정
- [ ] 선점과 `FollowUpRequest` 생성을 담당하는 객체 추출 검토
- [ ] generator 선택·호출·예외 분류를 `FollowUpGenerationAttemptExecutor`로 추출
- [ ] 저장 재시도와 완료를 `FollowUpCompletionTransaction`으로 추출
- [ ] `failed(..., boolean retry)` 제거
- [ ] 재시도와 영구 실패를 의도별 메서드 또는 결과 타입으로 분리
- [ ] Processor는 선점 → 생성 시도 → 결과 적용만 조정
- [ ] 임대·시도 횟수·상태 전이는 `FollowUpGeneration`이 계속 소유

### 검증

- [ ] Follow-up processor 테스트
- [ ] Follow-up domain 테스트
- [ ] worker 테스트
- [ ] 외부 adapter 실패 계약 테스트

## 작업 11. OpenAI 평가 Adapter 분리

- [ ] 현재 `OpenAiEvaluationAdapter`의 요청 생성·호출·응답 파싱 책임 테스트로 고정
- [ ] `OpenAiEvaluationRequestFactory` 추출
- [ ] JSON Schema 생성 메서드를 RequestFactory로 이동
- [ ] `OpenAiEvaluationResponseParser` 추출
- [ ] output text 탐색·필드 검증·도메인 변환을 ResponseParser로 이동
- [ ] Adapter는 검증된 요청 생성 → Client 호출 → 결과 파싱만 조정
- [ ] `client`, `model` 등 필드 이름 구체화
- [ ] 정상 응답·누락 필드·추가 필드·잘못된 usage 테스트

## 작업 12. OpenAI Follow-up Adapter 분리

- [ ] 현재 요청 JSON·schema·응답 파싱 계약 테스트로 고정
- [ ] `OpenAiFollowUpRequestFactory` 추출
- [ ] `OpenAiFollowUpResponseParser` 추출
- [ ] 허용 Concept·Evidence 검증 위치를 도메인 결과와 중복되지 않게 정리
- [ ] Adapter는 요청 생성 → Client 호출 → 결과 파싱만 조정
- [ ] 복수 output text·잘못된 ID·허용되지 않은 evidence 테스트

## 작업 13. 검색 근거 선택 정책 추출

### RED

- [ ] relevance scoring 규칙 테스트
- [ ] Concept별 근거 보존 테스트
- [ ] 상대 점수 하한 테스트
- [ ] 충돌 근거 보존 테스트
- [ ] 최종 정렬 안정성 테스트

### 책임 분리

- [ ] Repository 조회와 검색 알고리즘을 분리
- [ ] tokenization·scoring·coverage·conflict·selection을 `KnowledgeEvidenceSelector` 후보로 묶음
- [ ] `DefaultKnowledgeRetrievalService`는 후보 조회와 결과 반환만 조정
- [ ] 정책 객체는 DB 없는 순수 단위 테스트 가능하게 구성
- [ ] `result` 지역 변수를 `retrievedChunk` 등으로 구체화

## 작업 14. TypeScript HTTP·CSRF·세션 만료 책임 분리

### RED

- [ ] GET과 write 요청의 인증 정책 테스트 유지
- [ ] CSRF token 캐시·초기화 테스트 유지
- [ ] 401 session expiration 처리 테스트 유지
- [ ] expiration side effect 실패가 원래 HTTP 오류를 가리지 않는 테스트 유지

### 책임 분리

- [ ] `api/client.ts`의 전송·오류 변환·CSRF·세션 만료 책임 구분
- [ ] `csrfTokenStore.ts` 또는 동등한 작은 모듈 추출
- [ ] session expiration callback 소유 모듈 추출 검토
- [ ] `request`는 HTTP 요청과 응답 변환에 집중
- [ ] 전역 mutable 상태 소유 위치 명확화
- [ ] API 함수의 `id`를 `questionId`, `documentId`, `evaluationId`, `memberId`로 구체화

## 작업 15. 인증 세션 데이터 정리 책임 분리

- [ ] `useAuth`가 `clearPendingAnswerSubmissions`와 `clearCsrfToken` 구체 구현을 아는 문제 테스트로 고정
- [ ] 작은 `clearSessionData()` 조정 모듈 또는 동등한 경계 설계
- [ ] `useAuth`는 인증 상태와 인증 경쟁 제어에 집중
- [ ] logout·401 만료·새 login 경쟁 테스트 유지
- [ ] 불필요한 이벤트 버스나 확장 시스템 도입 금지
- [ ] `clearAuthenticationStateWithoutInvalidation`을 의도 중심 이름으로 변경

## 작업 16. 관리자 Question 편집 책임 분리

- [ ] `useAdminQuestionEditor`의 선택·폼·기준·상태 전이 책임 목록 확정
- [ ] 질문 초안 편집과 Concept 평가 기준 편집의 변경 이유 비교
- [ ] 별도 개념이면 `useQuestionCriteriaEditor` 추출
- [ ] `selected` → `selectedQuestion`
- [ ] `select` → `selectQuestionForEditing`
- [ ] `submit` → `saveQuestionDraft`
- [ ] 문자열 `transition(action)` 대신 검수·공개·폐기 의도별 함수 제공
- [ ] stale detail response 차단 테스트
- [ ] 선택 변경 중 저장 결과 처리 테스트
- [ ] 기준 저장 실패·성공 테스트

## 작업 17. 관리자 KnowledgeDocument 편집 책임 분리

- [ ] `useKnowledgeDocumentEditor`의 편집·버전·상태 전이·Chunk 책임 목록 확정
- [ ] Chunk 조회·생성·stale response 제어를 `useKnowledgeDocumentChunks`로 추출
- [ ] `selected` → `selectedDocument`
- [ ] `select` → `selectDocumentForEditing`
- [ ] `submit` → `saveDocumentDraft`
- [ ] 문자열 `transition(action)` 대신 검수·공개·폐기 의도별 함수 제공
- [ ] 문서 선택 변경 중 이전 Chunk 응답 무시 테스트
- [ ] Chunk 생성과 목록 갱신 테스트
- [ ] 편집 저장과 Chunk 상태가 불필요하게 결합되지 않았는지 확인

## 작업 18. Vue 이름과 메서드 책임 정리

- [ ] `load()`를 `loadKnowledgeMap`, `loadLearningProgress`, `loadMembers` 등으로 구체화
- [ ] `result`를 `questionPage`, `memberPage`, `evaluationPage`, `learningProgress` 등으로 구체화
- [ ] `selected`를 선택 대상이 드러나는 이름으로 변경
- [ ] `submit()`을 `submitLogin`, `submitSignUp` 등으로 구체화
- [ ] `item`을 `topic`, `concept`, `member`, `question`으로 변경
- [ ] `change`를 `changeMemberStatus`처럼 구체화
- [ ] `filterBy`가 난이도 필터 변경임을 이름으로 표현
- [ ] Vue 메서드가 여러 화면 작업을 함께 수행하면 private 함수 또는 composable 추출 검토
- [ ] Vue 컴포넌트 자체에는 SOLID를 강제하지 않음
- [ ] 컴포넌트 테스트와 template binding 확인

## 작업 19. 유지 판정이 필요한 주요 클래스

- [ ] `GlobalExceptionHandler`: 예외를 HTTP 오류 응답으로 변환하는 단일 책임인지 확인
- [ ] `DefaultLearningProgressService.progress`: 하나의 학습 현황 read model 조립 책임인지 확인
- [ ] `Evaluation.apply`: 완료 상태의 원자적 적용이므로 분리하지 않을 근거 확인
- [ ] `Question.publish`: 공개 불변식 검사와 상태 변경이 하나의 도메인 전이인지 확인
- [ ] `KnowledgeState.observe`: KnowledgeState가 계산 상태와 알고리즘을 소유하는 것이 맞는지 확인
- [ ] `SecurityConfiguration`: 설정 조립 책임 범위 안인지 확인
- [ ] 유지 판정도 감사 결과에 근거와 함께 기록

## 작업 20. `AGENTS.md` 상시 규칙 반영

- [ ] 기존 사용자 수정 보존
- [ ] `네이밍과 책임 점검` 절 추가
- [ ] Repository 인스턴스 기본 이름을 `xxxRepository`로 명시
- [ ] 협력 객체 이름에 대상과 역할 포함 규칙 추가
- [ ] 클래스·메서드·필드·매개변수·지역 변수 도메인 명명 규칙 추가
- [ ] 메서드 이름과 수행 내용 일치 검사 규칙 추가
- [ ] 메서드의 현재 클래스 배치 적합성 검사 규칙 추가
- [ ] 메서드 묶음의 별도 클래스 추출 여부 검사 규칙 추가
- [ ] 호출부와 테스트까지 함께 검사하는 규칙 추가
- [ ] 1~3번 검사를 모든 코드 작업의 완료 조건으로 추가
- [ ] SOLID는 Java·TypeScript에 적용하고 Vue 자체에는 강제하지 않는다고 명시
- [ ] 완료 체크리스트에 이름·책임·배치·추출 판정 추가

## 작업 21. 최종 검증

### 정적 재검사

- [ ] Repository 복수 객체 이름 0건 확인
- [ ] 일반적인 협력 객체의 `client`, `mapper`, `policy`, `provider` 단독 이름 재검토
- [ ] 넓은 범위의 `result`, `data`, `item`, `value` 잔여 항목을 파일별로 판정
- [ ] `process`, `handle`, `execute`, `load`, `submit`, `transition` 메서드 잔여 항목 판정
- [ ] 새 클래스와 이동한 메서드의 패키지·의존 방향 확인
- [ ] 사용하지 않는 이전 타입·메서드·import가 없는지 확인

### 백엔드

- [ ] Java 컴파일 통과
- [ ] 변경 도메인 단위 테스트 통과
- [ ] 변경 Service 통합 테스트 통과
- [ ] Controller/API 테스트 통과
- [ ] 전체 기본 테스트 통과
- [ ] 가능한 경우 PostgreSQL 테스트 통과
- [ ] 검색 정책 변경 시 retrieval benchmark 실행

### 프런트

- [ ] TypeScript 타입 검사 통과
- [ ] 변경 composable 테스트 통과
- [ ] 변경 component/view 테스트 통과
- [ ] 전체 프런트 테스트 통과
- [ ] production build 통과

### 문서와 보고

- [ ] 코드·테스트·`AGENTS.md` 규칙 일치 확인
- [ ] 문서 링크와 실행 명령 유효성 확인
- [ ] 검사 파일 수 보고
- [ ] 이름 변경 수 보고
- [ ] 메서드 이름 변경 수 보고
- [ ] 메서드 분리 수 보고
- [ ] 다른 객체로 이동한 메서드 수 보고
- [ ] 새로 추출한 클래스 수와 각 책임 보고
- [ ] 유지 판정한 주요 후보와 이유 보고
- [ ] 테스트 수·성공·실패 보고
- [ ] 미검증 환경과 실패 조건 보고

## 완료 조건

- [ ] 전체 대상 파일의 감사 상태가 기록됨
- [ ] 명명 위반이 수정되거나 유지 근거가 기록됨
- [ ] 메서드 이름과 실제 수행 내용이 일치함
- [ ] 각 메서드가 상태·정보·변경 이유에 맞는 클래스에 위치함
- [ ] 독립 개념을 이루는 메서드 묶음의 클래스 추출 여부가 판정됨
- [ ] Java·TypeScript SOLID 위반이 수정되거나 유지 근거가 기록됨
- [ ] Vue는 이름·메서드 책임 검사를 통과함
- [ ] 관련 테스트와 가능한 전체 테스트가 통과함
- [ ] `AGENTS.md`에 1~3번 상시 점검 규칙이 반영됨
- [ ] 완료 보고에 변경·검증·미검증 경계·실패 조건이 포함됨
