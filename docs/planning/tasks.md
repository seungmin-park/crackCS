# CrackCS 구현 작업 목록

> 기준 문서: [개발 계획](plan.md)\
> 제품 요구사항: [제품 기능 명세서](../product/spec.md)\
> 데이터 설계: [도메인 모델 및 ERD](../architecture/domain-model-and-erd.md)\

## 1. 사용 방법

- `[ ]`는 미완료, `[x]`는 완료를 의미한다.
- 각 작업 ID는 커밋, 이슈와 PR에서 그대로 사용한다. 예: `P2-T04 로그인 구현`.
- 하위 항목과 검증 항목이 모두 끝난 뒤 상위 작업을 완료 처리한다.
- `Phase Gate`를 모두 통과하기 전에는 다음 Phase의 기능 구현을 시작하지 않는다.
- 요구사항이 바뀌면 `spec.md`를 먼저 수정하고 이 문서의 요구사항 매핑을 갱신한다.
- 기술 선택이 바뀌어도 기능의 인수 조건은 임의로 변경하지 않는다.

```text
작업 구현
   ↓
자동 테스트
   ↓
브라우저 또는 API 재현
   ↓
문서·schema 정책 갱신
   ↓
Phase Gate 체크
```

## 2. 현재 기준선

- [x] Java 21과 Spring Boot 4.1.1 프로젝트 골격이 존재한다.
- [x] Spring MVC, Spring Data JPA와 H2 의존성이 존재한다.
- [x] Vue 3.5, TypeScript 6과 Vite 8 프로젝트 골격이 존재한다.
- [x] 백엔드 기본 context-load 테스트 파일이 존재한다.
- [x] 현재 백엔드 테스트가 실제로 통과하는지 확인한다.
- [x] 현재 프런트 type-check와 production build가 통과하는지 확인한다.
- [x] 로컬 실행 절차와 필수 도구 버전을 README에 기록한다.

## 3. 전체 진행 현황

- [ ] Phase 0 — 개발 기반과 결정 기록
- [x] Phase 1 — 공개 문제 조회 최소 제품
- [x] Phase 2 — 회원 인증과 관리자 경계
- [x] Phase 3 — 관리자 콘텐츠 운영
- [x] Phase 4 — 답변과 평가 상태 골격
- [ ] Phase 5 — Knowledge Retrieval과 실제 AI 평가
- [ ] Phase 6 — Knowledge State와 개인 추천
- [ ] Phase 7 — 후속 질문 학습 루프
- [ ] Phase 8 — 운영 안정화와 파일럿

## API URI 구현 체크리스트

전체 계약과 상태 코드는 [제품 기능 명세서의 API URI](../product/spec.md#12-api-uri)를 기준으로 한다.

### Phase 0 시스템

- [ ] `GET /api/health` — PUBLIC, 애플리케이션 상태 확인

### Phase 1·2 학습자와 인증

- [x] `POST /api/auth/sign-up` — PUBLIC, 회원가입
- [x] `POST /api/auth/login` — PUBLIC, 로그인
- [x] `POST /api/auth/logout` — USER, 로그아웃
- [x] `GET /api/members/me` — USER, 현재 회원 조회
- [x] `GET /api/questions` — USER, 공개 문제 목록
- [x] `GET /api/questions/{questionId}` — USER, 공개 문제 상세

Phase 1에서는 문제 조회 URI만 임시 PUBLIC으로 구현하고 Phase 2 Gate 전에 USER 권한으로 전환한다.

### Phase 3 관리자 콘텐츠

- [x] `GET /api/admin/members` — ADMIN, 회원 목록
- [x] `PATCH /api/admin/members/{memberId}/status` — ADMIN, 회원 상태 변경
- [x] `GET /api/admin/topics` — ADMIN, Topic 목록
- [x] `POST /api/admin/topics` — ADMIN, Topic 등록
- [x] `GET /api/admin/topics/{topicId}` — ADMIN, Topic 상세
- [x] `PATCH /api/admin/topics/{topicId}` — ADMIN, Topic 수정
- [x] `POST /api/admin/topics/{topicId}/deactivate` — ADMIN, Topic 비활성화
- [x] `GET /api/admin/concepts` — ADMIN, Concept 목록
- [x] `POST /api/admin/concepts` — ADMIN, Concept 등록
- [x] `GET /api/admin/concepts/{conceptId}` — ADMIN, Concept 상세
- [x] `PATCH /api/admin/concepts/{conceptId}` — ADMIN, Concept 수정
- [x] `POST /api/admin/concepts/{conceptId}/deactivate` — ADMIN, Concept 비활성화
- [x] `GET /api/admin/knowledge-documents` — ADMIN, 문서 목록
- [x] `POST /api/admin/knowledge-documents` — ADMIN, 문서 등록
- [x] `GET /api/admin/knowledge-documents/{documentId}` — ADMIN, 문서 상세
- [x] `PATCH /api/admin/knowledge-documents/{documentId}` — ADMIN, DRAFT 수정
- [x] `POST /api/admin/knowledge-documents/{documentId}/versions` — ADMIN, 새 버전
- [x] `POST /api/admin/knowledge-documents/{documentId}/review` — ADMIN, 검수
- [x] `POST /api/admin/knowledge-documents/{documentId}/publish` — ADMIN, 공개
- [x] `POST /api/admin/knowledge-documents/{documentId}/retire` — ADMIN, 폐기
- [x] `GET /api/admin/questions` — ADMIN, 문제 목록
- [x] `POST /api/admin/questions` — ADMIN, 문제 등록
- [x] `GET /api/admin/questions/{questionId}` — ADMIN, 문제 상세
- [x] `PATCH /api/admin/questions/{questionId}` — ADMIN, DRAFT 수정
- [x] `PUT /api/admin/questions/{questionId}/concepts` — ADMIN, 평가 Concept 교체
- [x] `POST /api/admin/questions/{questionId}/review` — ADMIN, 검수
- [x] `POST /api/admin/questions/{questionId}/publish` — ADMIN, 공개
- [x] `POST /api/admin/questions/{questionId}/retire` — ADMIN, 폐기
- [x] `POST /api/admin/questions/{questionId}/versions` — ADMIN, 새 문제 버전

### Phase 4 답변과 평가

- [x] `POST /api/questions/{questionId}/answers` — USER, 답변 제출과 평가 접수
- [x] `GET /api/members/me/answers` — USER, 내 답변 목록
- [x] `GET /api/answers/{answerId}` — USER, 내 답변 상세
- [x] `GET /api/answers/{answerId}/evaluation` — USER, 평가 상태·결과

### Phase 5 근거와 운영

- [ ] `POST /api/admin/knowledge-documents/{documentId}/chunks` — ADMIN, Chunk 생성 접수
- [ ] `GET /api/admin/knowledge-documents/{documentId}/chunks` — ADMIN, Chunk·작업 상태
- [ ] `GET /api/admin/evaluations` — ADMIN, 실패·검토 평가 목록
- [ ] `GET /api/admin/evaluations/{evaluationId}` — ADMIN, 평가 상세

### Phase 6 개인화

- [ ] `GET /api/recommendations/next-question` — USER, 다음 추천 문제
- [ ] `GET /api/members/me/knowledge-states` — USER, 지식 지도
- [ ] `GET /api/members/me/progress` — USER, 학습 홈 요약

### Phase 7 후속 질문

- [ ] `GET /api/answers/{answerId}/follow-up-question` — USER, 후속 질문 조회

---

## Phase 0 — 개발 기반과 결정 기록

목표: 모든 개발자가 같은 명령, schema와 오류 계약으로 기능을 구현할 수 있게 한다.

### P0-T01 빌드 기준선 확인

- [ ] `./gradlew test`를 실행하고 실패가 있으면 원인을 해결한다.
- [ ] `front`에서 `npm run type-check`를 실행하고 실패를 해결한다.
- [ ] `front`에서 `npm run build-only`를 실행하고 실패를 해결한다.
- [ ] Java, Node와 npm 최소 버전을 README에 기록한다.
- [ ] 백엔드와 프런트 로컬 실행 명령을 README에 기록한다.
- [ ] 프런트에서 백엔드 health endpoint까지 호출하는 방법을 기록한다.

검증:

- [ ] 깨끗한 checkout에서 문서의 명령만으로 두 애플리케이션을 실행할 수 있다.

### P0-T02 실행 환경과 설정 분리

- [ ] `local`, `test` profile의 책임을 정한다.
- [ ] H2 개발 DB와 테스트 DB 설정을 분리한다.
- [ ] 운영 비밀정보를 환경 변수로 주입하는 이름과 규칙을 정한다.
- [ ] 실제 비밀값 없이 `.env` 또는 설정 예시를 제공한다.
- [ ] JPA schema 자동 생성 정책을 profile별로 명시한다.
- [ ] API key, 비밀번호와 사용자 답변이 기본 로그에 출력되지 않게 한다.

검증:

- [ ] `test` 실행이 개발 DB 내용을 읽거나 변경하지 않는다.
- [ ] 저장소에 실제 비밀값이 없음을 확인한다.

### P0-T03 DB schema 관리 기반 구축

- [x] 운영 DB 확정 전에는 버전 기반 migration 도구를 보류한다.
- [x] 선택 이유와 대안을 ADR로 기록한다.
- [x] 기본, local과 test profile의 Hibernate schema 정책을 분리한다.
- [x] local seed를 운영 schema 관리와 분리한다.
- [x] 기본 profile이 외부 schema와 JPA mapping을 검증하도록 설정한다.
- [x] 빈 DB와 기존 local DB에서 애플리케이션 기동을 확인한다.

검증:

- [x] 빈 local DB에 같은 엔티티 mapping을 반복 적용할 수 있다.
- [x] 기본 profile에서 외부 schema와 엔티티가 다르면 기동이 실패한다.

### P0-T04 공통 API 오류 계약

- [ ] 오류 응답에 사용할 필드를 확정한다: `code`, `message`, `fieldErrors`, `requestId`.
- [ ] 공통 오류 응답 DTO를 구현한다.
- [ ] validation 오류를 공통 형식으로 변환한다.
- [ ] 존재하지 않는 리소스와 비즈니스 규칙 위반을 구분한다.
- [ ] 예상하지 못한 예외의 내부 정보가 클라이언트에 노출되지 않게 한다.
- [ ] 대표 오류 응답을 API 문서에 기록한다.

검증:

- [ ] validation, 404, 409와 500 응답 계약 테스트가 통과한다.

### P0-T05 프런트 API 기반

- [ ] Vite proxy와 서버 CORS 중 로컬 연결 방식을 결정한다.
- [ ] 공통 API client를 만든다.
- [ ] 성공 응답과 공통 오류 응답의 TypeScript 타입을 정의한다.
- [ ] loading, empty, validation error와 server error 처리 기준을 정한다.
- [ ] 화면마다 HTTP 오류 변환을 반복하지 않게 한다.

검증:

- [ ] 샘플 API의 성공·실패 응답이 Vue 화면에서 구분되어 보인다.

### P0-T06 미결정 사항과 ADR 준비

- [ ] ADR 템플릿과 저장 위치를 정한다.
- [ ] `OQ-001` 인증 상태 유지 방식의 결정 조건을 기록한다.
- [ ] `OQ-002` 운영 DB와 벡터 저장 방식의 결정 시점을 기록한다.
- [ ] `OQ-008` 비밀번호 정책을 Phase 2 전에 확정한다.
- [ ] 결정되지 않은 항목과 확정된 항목을 구분해 추적한다.

### P0-T07 CI 구성

- [ ] 백엔드 테스트 작업을 CI에 추가한다.
- [ ] 프런트 type-check 작업을 CI에 추가한다.
- [ ] 프런트 production build 작업을 CI에 추가한다.
- [ ] 실패한 작업의 로그를 확인할 수 있게 한다.
- [ ] dependency cache가 결과 정확성을 해치지 않는지 확인한다.

### Phase 0 Gate

- [ ] 새 환경에서 백엔드와 프런트가 실행된다.
- [x] 빈 local DB에 Hibernate schema와 local seed를 적용할 수 있다.
- [ ] 백엔드 테스트, 프런트 type-check와 build가 통과한다.
- [ ] 공통 오류 응답 예제가 문서화되어 있다.
- [ ] 실제 비밀정보가 저장소와 로그에 없다.
- [ ] Phase 0 작업을 독립 커밋 단위로 정리했다.

---

## Phase 1 — 공개 문제 조회 최소 제품

대상: `FR-QUESTION-002` 일부. Phase 2 전까지 외부 환경에 배포하지 않는다.

### P1-T01 Topic 도메인과 schema

- [x] Topic ID, parent, code와 name 규칙을 구현한다.
- [x] 상위 Topic이 자기 자신을 가리키지 못하게 한다.
- [x] Topic code의 유일성을 DB에서 보장한다.
- [x] Topic의 DB constraint와 JPA mapping을 작성한다.
- [x] 계층 조회에 필요한 repository query를 작성한다.
- [x] Topic 도메인·repository 테스트를 작성한다.

### P1-T02 Concept 도메인과 schema

- [x] Concept ID, Topic, code, name과 description 규칙을 구현한다.
- [x] Concept code의 유일성을 DB에서 보장한다.
- [x] 존재하는 Topic에만 Concept를 연결할 수 있게 한다.
- [x] Concept의 DB constraint와 JPA mapping을 작성한다.
- [x] Topic별 Concept 조회 query와 테스트를 작성한다.

### P1-T03 Question과 QuestionConcept 최소 모델

- [x] Question의 Topic, origin, type, difficulty, content, reference answer와 status를 구현한다.
- [x] QuestionConcept 식별자와 Question·Concept 조합의 유일성, weight, required를 구현한다.
- [x] weight 범위와 필수 Concept 규칙을 정의한다.
- [x] PUBLISHED 문제는 Concept를 하나 이상 가져야 한다는 규칙을 구현한다.
- [x] 공개 조회에서 reference answer, Concept와 weight가 노출되지 않도록 DTO를 분리한다.
- [x] JPA mapping, DB constraint와 도메인 테스트를 작성한다.

### P1-T04 초기 문제 데이터

- [x] 네트워크, 운영체제 또는 Java 중 하나의 작은 Topic 구조를 선택한다.
- [x] DRAFT, PUBLISHED와 RETIRED 문제를 각각 준비한다.
- [x] PUBLISHED 문제에 하나 이상의 필수 Concept를 연결한다.
- [x] seed가 운영 데이터와 혼동되지 않도록 local profile로 구분한다.
- [x] seed를 반복 적용해도 데이터가 중복되지 않는지 확인한다.

### P1-T05 공개 문제 조회 API

- [x] 문제 목록 요청·응답 계약을 정의한다.
- [x] 문제 상세 요청·응답 계약을 정의한다.
- [x] PUBLISHED만 조회하는 application service를 구현한다.
- [x] 목록 pagination 또는 초기 조회 한도를 적용한다.
- [x] 존재하지 않거나 비공개인 문제는 공개 API에서 구분 없이 노출하지 않는다.
- [x] 엔티티를 직접 직렬화하지 않는다.

검증:

- [x] PUBLISHED 목록·상세 API 테스트가 통과한다.
- [x] DRAFT와 RETIRED가 조회되지 않는 테스트가 통과한다.
- [x] 응답에 reference answer와 평가 가중치가 없는지 테스트한다.

### P1-T06 문제 목록·상세 화면

- [x] 문제 목록 route와 화면을 만든다.
- [x] 문제 상세 route와 화면을 만든다.
- [x] Topic, 난이도와 문제 본문을 표시한다.
- [x] loading, empty, not found와 server error 상태를 표시한다.
- [x] 화면에서 모범 답안과 내부 평가 정보에 접근하지 않는다.
- [x] 주요 화면 상태의 프런트 테스트를 작성한다.

### Phase 1 Gate

- [x] 브라우저에서 PUBLISHED 문제 목록과 상세를 조회할 수 있다.
- [x] DRAFT·RETIRED 문제는 API와 화면에 노출되지 않는다.
- [x] 평가용 비공개 필드가 응답에 포함되지 않는다.
- [x] repository와 API 통합 테스트가 통과한다.
- [x] 공통 Phase 완료 정의를 통과한다.

---

## Phase 2 — 회원 인증과 관리자 경계

대상: `FR-AUTH-001`, `FR-AUTH-002`, `FR-AUTH-003`, `AC-006`.

### P2-T01 인증 방식과 비밀번호 정책 결정

- [x] 동일 출처 웹 배포를 기준으로 세션과 토큰을 비교한다.
- [x] `OQ-001`을 결정하고 ADR을 작성한다.
- [x] 비밀번호 최소 길이와 허용 규칙을 결정한다.
- [x] `OQ-008`을 결정하고 명세에 반영한다.
- [x] 세션 cookie를 쓴다면 Secure, HttpOnly와 SameSite 정책을 정한다.
- [x] CSRF 처리 방식을 정한다.

### P2-T02 Member와 AuthAccount 모델

- [x] Member의 nickname, role, status와 audit 필드를 구현한다.
- [x] AuthAccount의 provider, loginId, passwordHash와 lastLoginAt을 구현한다.
- [x] `(provider, login_id)` unique constraint를 JPA schema에 추가한다.
- [x] Member와 LOCAL AuthAccount 생성을 한 트랜잭션으로 묶는다.
- [x] BLOCKED와 WITHDRAWN 회원의 인증 규칙을 구현한다.
- [x] repository와 트랜잭션 통합 테스트를 작성한다.

### P2-T03 Spring Security 기반

- [x] Spring Security 의존성을 추가한다.
- [x] SecurityFilterChain을 구성한다.
- [x] 공개, 로그인 필요와 ADMIN endpoint 규칙을 명시한다.
- [x] PasswordEncoder를 구성한다.
- [x] 인증 실패와 접근 거부 응답을 공통 오류 계약으로 변환한다.
- [x] 테스트 profile에서도 실제 인가 규칙을 우회하지 않게 한다.

### P2-T04 회원가입 API와 화면

- [x] 이메일, 비밀번호와 닉네임 입력 DTO를 정의한다.
- [x] 이메일 형식과 비밀번호 정책 validation을 구현한다.
- [x] 중복 LOCAL 이메일을 409로 처리한다.
- [x] 비밀번호를 해시한 뒤 저장하고 원문 참조를 남기지 않는다.
- [x] 회원가입 Vue 화면과 필드 오류 표시를 구현한다.
- [x] 정상, 중복, 잘못된 입력과 트랜잭션 rollback 테스트를 작성한다.

### P2-T05 로그인·로그아웃·현재 회원

- [x] 로그인 API 또는 Security 인증 endpoint를 구현한다.
- [x] 로그인 성공 시 lastLoginAt을 갱신한다.
- [x] 로그아웃 후 기존 인증 상태를 무효화한다.
- [x] 현재 회원과 역할 조회 API를 구현한다.
- [x] 로그인 화면과 인증 상태 composable을 구현한다.
- [x] 새로고침 후 인증 상태 복구를 구현한다.
- [x] 실패, 차단 회원과 로그아웃 테스트를 작성한다.

### P2-T06 관리자 인가와 라우팅

- [x] `/admin/**` API를 ADMIN으로 제한한다.
- [x] 일반 회원 데이터 API는 본인 소유권을 기준으로 조회하도록 기반을 만든다.
- [x] Vue 관리자 route guard를 구현한다.
- [x] 비로그인 사용자는 로그인 화면으로 안내한다.
- [x] USER에게 관리자 링크를 숨기되 서버 인가를 최종 기준으로 유지한다.
- [x] `AC-006` 통합 테스트를 작성한다.

### P2-T07 인증 보안 최소 기준

- [x] 로그인 시도 제한 기준과 구현 방식을 정한다.
- [x] 인증 관련 로그에서 이메일 마스킹 여부를 정한다.
- [x] 비밀번호와 session/token 값이 로그에 남지 않는지 확인한다.
- [x] session fixation, CSRF와 CORS 경계를 테스트한다.
- [x] 인증 오류 메시지가 계정 존재 여부를 과도하게 노출하지 않게 한다.

### Phase 2 Gate

- [x] 브라우저에서 회원가입·로그인·로그아웃을 완료할 수 있다.
- [x] BLOCKED·WITHDRAWN 회원은 로그인할 수 없다.
- [x] USER의 관리자 API 요청이 서버에서 거부된다.
- [x] DB와 로그에 비밀번호 원문이 없다.
- [x] `AC-006`이 통과한다.
- [x] Phase 1 무인증 문제 API에 최종 인증 정책을 적용했다.

---

## Phase 3 — 관리자 콘텐츠 운영

대상: `FR-ADMIN-001`, `FR-ADMIN-002`, `FR-ADMIN-004`.

### P3-T01 관리자 레이아웃과 공통 목록

- [x] 관리자 레이아웃, 메뉴와 route를 구성한다.
- [x] 공통 pagination 요청·응답 규칙을 정한다.
- [x] ADMIN 여부 확인 중 loading과 접근 거부 화면을 구현한다.
- [x] 등록·수정 성공과 validation 실패 피드백 방식을 통일한다.

### P3-T02 Topic·Concept 관리

- [x] Topic 등록, 수정, 조회와 비활성화 API를 구현한다.
- [x] Concept 등록, 수정, 조회와 비활성화 API를 구현한다.
- [x] 참조 중인 Topic·Concept의 물리 삭제를 차단한다.
- [x] 계층 순환과 중복 code를 차단한다.
- [x] 관리자 Topic·Concept 목록과 편집 화면을 구현한다.
- [x] ADMIN 인가와 상태 변경 통합 테스트를 작성한다.

### P3-T03 KnowledgeDocument 모델과 버전

- [x] KnowledgeDocument 전체 컬럼과 상태를 ERD에 맞게 구현한다.
- [x] checksum 계산과 중복 원문 감지 규칙을 구현한다.
- [x] 새 documentVersion 생성 규칙을 구현한다.
- [x] PUBLISHED 문서를 덮어쓰지 못하게 한다.
- [x] 검수자, reviewedAt과 출처 정보를 공개 조건으로 검증한다.
- [x] RETIRED 전환이 과거 Evaluation 근거를 삭제하지 않게 한다.

### P3-T04 KnowledgeDocument API와 화면

- [x] 문서 등록, 상세, 목록과 수정 API를 구현한다.
- [x] 새 버전 생성, 검수, 공개와 폐기 API를 구현한다.
- [x] 출처 유형, URL, 기술 버전과 라이선스 메모 validation을 구현한다.
- [x] 관리자 문서 목록·등록·검수 화면을 구현한다.
- [x] DRAFT, PUBLISHED와 RETIRED 필터를 구현한다.
- [x] 콘텐츠 버전 보존 통합 테스트를 작성한다.

### P3-T05 Question 관리 도메인

- [x] Question의 DRAFT → PUBLISHED → RETIRED 상태 전이를 구현한다.
- [x] PUBLISHED 전 검수자와 reviewedAt을 요구한다.
- [x] PUBLISHED 전 QuestionConcept가 하나 이상인지 검증한다.
- [x] 필수 Concept 존재와 weight 합계 정책을 확정한다.
- [x] Answer가 있는 문제를 덮어쓰지 않고 새 버전 또는 폐기로 처리한다.
- [x] NORMAL 문제의 관리자 생성자 필수 규칙을 구현한다.

### P3-T06 Question API와 화면

- [x] 문제 등록, 상세, 목록과 수정 API를 구현한다.
- [x] QuestionConcept 추가·수정·삭제 API를 구현한다.
- [x] 문제 검수, 공개와 폐기 API를 구현한다.
- [x] 관리자 문제 목록·등록·검수 화면을 구현한다.
- [x] 모범 답안, 필수 Concept와 weight 입력 UI를 구현한다.
- [x] 공개 조건과 버전 보존 API 테스트를 작성한다.

### P3-T07 콘텐츠 공개 경계 검증

- [x] 공개 문제 API에는 PUBLISHED Question만 포함되는지 확인한다.
- [x] Retrieval 후보에는 PUBLISHED KnowledgeDocument만 포함되도록 query를 준비한다.
- [x] ADMIN이 아닌 사용자가 DRAFT 상세를 조회하지 못하게 한다.
- [x] 비활성 Topic·Concept의 신규 연결을 차단한다.
- [x] `AC-007`의 문서 버전 시나리오 기반 테스트 골격을 작성한다.

### Phase 3 Gate

- [x] ADMIN이 Topic → Concept → 문서·문제를 등록하고 공개할 수 있다.
- [x] 검수 조건을 충족하지 않은 콘텐츠는 공개할 수 없다.
- [x] 일반 사용자는 PUBLISHED 콘텐츠만 볼 수 있다.
- [x] 공개된 콘텐츠의 과거 버전이 보존된다.
- [x] 관리자 핵심 흐름의 브라우저 E2E 테스트가 통과한다.

---

## Phase 4 — 답변과 평가 상태 골격

대상: `FR-ANSWER-001`, `FR-ANSWER-002`, `FR-ANSWER-003`, `FR-EVAL-001`, `FR-EVAL-004`, `FR-EVAL-005`.

검증 근거: [Phase 4 결정·검증 기록](../changes/2026-09-07-phase-4/verification.md). 백엔드 197개·프런트 102개 성공, 타입 검사·빌드·실제 화면 흐름 통과.

### P4-T01 Answer 모델과 제출 규칙

- [x] Answer schema와 immutable 제출 모델을 구현한다.
- [x] 공백 답변과 최대 길이 validation을 구현한다.
- [x] 로그인 회원과 Answer 소유권을 연결한다.
- [x] PUBLISHED Question에만 답변할 수 있게 한다.
- [x] 재답변은 기존 row 수정이 아니라 새 Answer로 저장한다.
- [x] Answer repository와 도메인 테스트를 작성한다.

### P4-T02 멱등 제출

- [x] 멱등 키의 전달 위치와 유효 범위를 정의한다.
- [x] 회원·요청별 unique constraint 또는 동등한 저장 구조를 설계한다.
- [x] 같은 멱등 키와 같은 payload는 기존 결과를 반환한다.
- [x] 같은 멱등 키와 다른 payload는 충돌로 처리한다.
- [x] 동시에 같은 요청이 들어오는 통합 테스트를 작성한다.

### P4-T03 Evaluation 모델과 상태 전이

- [x] Evaluation schema와 Answer 1:0..1 unique constraint를 구현한다.
- [x] EVALUATING, EVALUATED와 FAILED 상태를 구현한다.
- [x] NEEDS_REVIEW는 성공 status의 verdict인지 별도 status인지 최종 확정한다.
- [x] CORRECT, PARTIALLY_CORRECT, INCORRECT와 NEEDS_REVIEW verdict를 구현한다.
- [x] verdict → 100·50·0·NULL 변환을 서버 규칙으로 구현한다.
- [x] 허용되지 않는 상태 전이와 필드 조합을 차단한다.
- [x] 상태 전이 단위 테스트를 작성한다.

### P4-T04 평가 Port와 Stub adapter

- [x] AI 제공자와 무관한 Evaluation 요청 모델을 정의한다.
- [x] 전체·Concept별 판정과 feedback을 담는 결과 모델을 정의한다.
- [x] EvaluationPort 인터페이스를 정의한다.
- [x] 성공, NEEDS_REVIEW, timeout과 실패를 재현하는 Stub adapter를 구현한다.
- [x] Stub adapter를 local/test profile에서만 활성화한다.
- [x] 운영 profile에 Stub이 활성화되면 기동 실패하도록 검토한다.

### P4-T05 답변 제출 유스케이스와 트랜잭션

- [x] Answer 저장과 Evaluation 생성의 트랜잭션 경계를 정의한다.
- [x] Answer가 저장된 뒤 평가 실패해도 Answer를 보존한다.
- [x] Evaluation 시작 작업이 유실되지 않는 방식을 정한다.
- [x] 중복 worker 실행에도 Evaluation이 한 번만 확정되게 한다.
- [x] 성공, 실패와 재시도 통합 테스트를 작성한다.

### P4-T06 답변·평가 API

- [x] Answer 제출 API 계약을 정의하고 구현한다.
- [x] Evaluation 상태·결과 조회 API를 구현한다.
- [x] 내 Answer 목록과 상세 조회 API를 구현한다.
- [x] 다른 회원의 Answer·Evaluation 조회를 차단한다.
- [x] 최신순 pagination을 구현한다.
- [x] 소유권과 실패 상태 API 테스트를 작성한다.

### P4-T07 문제 풀이·평가 화면

- [x] 문제 상세에 답변 입력과 제출 UI를 구현한다.
- [x] 중복 클릭과 네트워크 재시도에서 같은 멱등 키를 사용한다.
- [x] EVALUATING polling 또는 상태 갱신 방식을 구현한다.
- [x] EVALUATED, NEEDS_REVIEW와 FAILED 화면을 구분한다.
- [x] 답변·평가 이력 목록과 상세 화면을 구현한다.
- [x] 새로고침 후에도 평가 상태를 복구한다.

### Phase 4 Gate

- [x] 문제 조회 → 답변 제출 → 평가 결과 확인 흐름을 완주한다.
- [x] 같은 멱등 요청으로 Answer와 Evaluation이 중복 생성되지 않는다.
- [x] 평가 실패에도 Answer가 보존된다.
- [x] 다른 회원의 답변과 평가를 조회할 수 없다.
- [x] Stub adapter가 운영 환경에 노출되지 않는다.

---

## Phase 5 — Knowledge Retrieval과 실제 AI 평가

대상: `FR-ADMIN-003`, `FR-ADMIN-005`, `FR-EVAL-002`, `FR-EVAL-003`, `FR-EVAL-004`, `FR-EVAL-005`, `AC-002`, `AC-003`, `AC-007`.

### P5-T01 검색 기준선과 저장 기술 결정

- [ ] 검색 품질을 평가할 질문·문서·정답 dataset을 준비한다.
- [ ] 관계형 필터와 전문/키워드 검색 기준선을 구현한다.
- [ ] Recall@K와 관련 없는 Chunk 포함률을 측정한다.
- [ ] embedding 검색 실험이 필요한 기준을 정한다.
- [ ] H2, PostgreSQL과 pgvector 선택을 비교한다.
- [ ] `OQ-002`를 결정하고 ADR과 환경 구성을 갱신한다.
- [ ] PostgreSQL 선택 시 Testcontainers 통합 테스트를 추가한다.

### P5-T02 KnowledgeChunk 생성

- [ ] Chunk 크기, overlap과 구분 기준을 문서화한다.
- [ ] KnowledgeChunk schema와 `(document_id, sequence_no)` unique constraint를 구현한다.
- [ ] PUBLISHED 대상 chunking 유스케이스를 구현한다.
- [ ] checksum 또는 작업 키로 중복 분할을 방지한다.
- [ ] embedding 생성 실패와 검색 가능 상태를 구분한다.
- [ ] 문서 순서와 원문 추적 테스트를 작성한다.

### P5-T03 Retrieval pipeline

- [ ] Question의 Topic과 QuestionConcept로 후보 문서를 제한한다.
- [ ] 질문, 모범 답안과 Answer로 검색 query를 구성한다.
- [ ] 상위 K개 Chunk와 relevance score를 반환한다.
- [ ] RETIRED와 DRAFT 문서를 검색 대상에서 제외한다.
- [ ] 근거 없음과 상충 근거를 감지하는 규칙을 정의한다.
- [ ] 검색 결과 재현과 품질 측정 테스트를 작성한다.

### P5-T04 구조화 AI 평가 계약

- [ ] 전체 verdict와 Concept별 verdict JSON schema를 정의한다.
- [ ] 강점, 누락, 오개념과 Evidence Chunk ID 필드를 정의한다.
- [ ] 필수 Concept 누락을 검증한다.
- [ ] 존재하지 않는 Chunk ID 인용을 거부한다.
- [ ] 최종 점수는 AI가 아니라 서버가 계산한다.
- [ ] 정상·누락·잘못된 타입·추가 필드 fixture로 계약 테스트를 작성한다.

### P5-T05 외부 AI adapter

- [ ] 평가 모델 후보를 골든 세트로 비교한다.
- [ ] 모델 선택과 fallback 기준을 ADR로 기록한다.
- [ ] 외부 AI client를 EvaluationPort adapter로 구현한다.
- [ ] timeout, 재시도 횟수와 backoff를 설정한다.
- [ ] provider 오류를 내부 실패 코드로 변환한다.
- [ ] prompt에서 지식 문서와 사용자 답변을 데이터 영역으로 격리한다.
- [ ] API key와 원문 답변을 일반 로그에 남기지 않는다.

### P5-T06 비동기 평가 실행

- [ ] 동기·비동기 방식을 비교하고 `OQ-005`를 결정한다.
- [ ] Answer 저장 이후 평가 작업이 유실되지 않는 구조를 선택한다.
- [ ] worker 재시작과 중복 실행 시나리오를 처리한다.
- [ ] 최대 재시도 이후 FAILED와 실패 원인을 저장한다.
- [ ] 클라이언트가 진행 상태를 안정적으로 조회할 수 있게 한다.
- [ ] timeout, 중복 처리와 재시작 통합 테스트를 작성한다.

### P5-T07 EvaluationEvidence와 결과 확정

- [ ] EvaluationConcept와 EvaluationEvidence schema를 구현한다.
- [ ] 평가에 실제 전달한 Chunk만 Evidence로 저장한다.
- [ ] 평가 모델명, evaluator version과 처리 시간을 저장한다.
- [ ] schema와 필수 Concept 검증 후에만 EVALUATED로 전환한다.
- [ ] 근거 부족·상충은 NEEDS_REVIEW로 처리한다.
- [ ] FAILED·NEEDS_REVIEW가 Knowledge State 후보가 되지 않게 한다.

### P5-T08 평가 결과·관리자 실패 화면

- [ ] 학습자 결과에 전체·Concept별 판정과 근거를 표시한다.
- [ ] 근거 문서의 제목, 버전과 인용 범위를 표시한다.
- [ ] 관리자 FAILED·NEEDS_REVIEW 목록 API를 구현한다.
- [ ] 모델명, 규칙 버전, 실패 코드와 발생 시각을 제공한다.
- [ ] 관리자 실패 상세 화면과 필터를 구현한다.
- [ ] 답변 원문 접근에 ADMIN 인가를 적용한다.

### P5-T09 골든 평가 세트 검증

- [ ] 초기 Topic별 정답·부분 정답·오답 fixture를 작성한다.
- [ ] 표현은 다르지만 의미가 같은 정답을 포함한다.
- [ ] 자연스럽지만 핵심이 틀린 오답을 포함한다.
- [ ] 근거 부족으로 NEEDS_REVIEW가 필요한 사례를 포함한다.
- [ ] 판정 일치율과 false-correct 비율을 자동 계산한다.
- [ ] 확정된 출시 품질 기준과 측정 결과를 기록한다.

### Phase 5 Gate

- [ ] EVALUATED 결과의 Evidence 연결률이 100%다.
- [ ] schema 위반과 근거 부족이 성공 평가로 저장되지 않는다.
- [ ] AI 장애에도 Answer가 보존된다.
- [ ] `AC-002`, `AC-003`과 `AC-007`이 통과한다.
- [ ] 골든 평가 세트의 확정 기준을 통과한다.
- [ ] 비밀정보와 답변 원문이 로그에 노출되지 않는다.

---

## Phase 6 — Knowledge State와 개인 추천

대상: `FR-KNOWLEDGE-001`, `FR-KNOWLEDGE-002`, `FR-KNOWLEDGE-003`, `FR-QUESTION-001`, `FR-PROGRESS-001`, `AC-001`, `AC-004`.

### P6-T01 Knowledge State 공식 결정

- [ ] PARTIALLY_CORRECT의 Concept 충족 기준인 `OQ-003`을 확정한다.
- [ ] mastery, confidence와 STABLE 임계값인 `OQ-004`를 확정한다.
- [ ] 최신 평가와 반복 평가의 가중 방식을 정의한다.
- [ ] 알고리즘 버전과 변경 시 재계산 정책을 정의한다.
- [ ] 예시 평가 이력으로 예상 상태를 계산해 문서화한다.

### P6-T02 KnowledgeState schema와 도메인

- [ ] `(member_id, concept_id)` 복합 키를 구현한다.
- [ ] masteryScore NULL과 UNKNOWN 의미를 보존한다.
- [ ] confidenceScore, attemptCount, status와 lastEvaluatedAt을 구현한다.
- [ ] 낙관적 잠금 version을 구현한다.
- [ ] 허용되는 상태 전이와 범위 검증을 구현한다.
- [ ] 상태 계산 단위 테스트를 작성한다.

### P6-T03 정확히 한 번 반영

- [ ] 어떤 EvaluationConcept가 반영됐는지 추적하는 구조를 결정한다.
- [ ] 유일 제약으로 중복 반영을 차단한다.
- [ ] 평가 완료와 상태 반영의 트랜잭션 또는 이벤트 경계를 정의한다.
- [ ] 낙관적 잠금 충돌 재시도를 구현한다.
- [ ] 같은 평가 재처리와 동시 다른 평가 처리 테스트를 작성한다.
- [ ] lost update가 발생하지 않는지 최종 DB 값으로 확인한다.

### P6-T04 Knowledge State 조회 API

- [ ] Topic별 집계 규칙을 정의한다.
- [ ] Concept별 상태·점수·신뢰도·횟수·최근 평가 조회를 구현한다.
- [ ] UNKNOWN과 낮은 mastery를 다른 응답 상태로 제공한다.
- [ ] 전체 Answer 이력을 매번 읽지 않는 query를 구현한다.
- [ ] 다른 회원의 상태 조회를 차단한다.
- [ ] 조회 query와 소유권 테스트를 작성한다.

### P6-T05 추천 규칙

- [ ] 미평가 Concept 우선 규칙을 구현한다.
- [ ] 낮은 mastery Concept 차순위 규칙을 구현한다.
- [ ] 같은 우선순위에서 최근에 풀지 않은 Question을 선택한다.
- [ ] RETIRED와 이미 사용할 수 없는 문제를 제외한다.
- [ ] 후보 없음 상태와 이유를 정의한다.
- [ ] 결정적 fixture를 사용한 추천 단위·통합 테스트를 작성한다.

### P6-T06 지식 지도와 학습 홈

- [ ] Topic별 상태 요약 UI를 구현한다.
- [ ] Concept별 UNKNOWN, LEARNING과 STABLE 표시를 구현한다.
- [ ] 숙련도와 신뢰도를 혼동하지 않게 설명한다.
- [ ] 최근 풀이 수와 평가 결과를 표시한다.
- [ ] 다음 추천 문제와 추천 이유를 표시한다.
- [ ] empty, 신규 회원과 일부 평가 상태 화면을 테스트한다.

### Phase 6 Gate

- [ ] UNKNOWN이 0점 취약 상태와 구분된다.
- [ ] 같은 평가가 두 번 반영되지 않는다.
- [ ] 동시 완료된 평가가 유실되지 않는다.
- [ ] 추천 결과와 추천 이유가 함께 제공된다.
- [ ] `AC-001`과 `AC-004`가 통과한다.

---

## Phase 7 — 후속 질문 학습 루프

대상: `FR-FOLLOWUP-001`, `FR-FOLLOWUP-002`, `AC-005`.

### P7-T01 후속 질문 도메인 불변식

- [ ] NORMAL과 FOLLOW_UP Question의 차이를 도메인 규칙으로 구현한다.
- [ ] FOLLOW_UP은 sourceAnswer가 필수임을 검증한다.
- [ ] NORMAL은 sourceAnswer를 가질 수 없게 한다.
- [ ] `source_answer_id` unique constraint로 답변당 최대 한 개를 보장한다.
- [ ] FOLLOW_UP에서 또 FOLLOW_UP을 만들지 못하게 한다.
- [ ] 도메인과 DB constraint 테스트를 작성한다.

### P7-T02 후속 질문 생성 규칙

- [ ] INCORRECT는 가장 중요한 오개념을 묻도록 한다.
- [ ] PARTIALLY_CORRECT는 가장 중요한 누락을 묻도록 한다.
- [ ] CORRECT는 동일 Concept의 적용 질문을 만들도록 한다.
- [ ] FAILED와 NEEDS_REVIEW는 생성 대상에서 제외한다.
- [ ] 생성 결과에 평가 가능한 reference answer와 Concept를 포함한다.
- [ ] 질문 생성 규칙 버전을 기록한다.

### P7-T03 후속 질문 생성 adapter

- [ ] 외부 모델과 무관한 FollowUpQuestionPort를 정의한다.
- [ ] 개발·테스트용 Stub을 구현한다.
- [ ] 실제 AI adapter에 structured output 검증을 적용한다.
- [ ] 원본 질문, 평가 결과와 승인된 근거만 입력으로 사용한다.
- [ ] timeout과 생성 실패가 기존 평가를 변경하지 않게 한다.
- [ ] 같은 Answer 재처리 시 기존 후속 질문을 반환한다.

### P7-T04 기존 학습 파이프라인 재사용

- [ ] 후속 Question도 기존 조회 DTO로 표시할 수 있게 한다.
- [ ] 후속 Answer가 기존 제출·멱등 처리 흐름을 사용하게 한다.
- [ ] 후속 Evaluation이 기존 Retrieval·평가 흐름을 사용하게 한다.
- [ ] 후속 평가도 Knowledge State에 반영할지 정책을 확정한다.
- [ ] 후속 평가 후 다음 기본 Question을 추천한다.

### P7-T05 후속 질문 화면

- [ ] 일반 평가 결과 화면에서 후속 질문을 표시한다.
- [ ] 생성 중, 생성 실패와 질문 없음 상태를 처리한다.
- [ ] 후속 답변과 평가 결과 화면을 구현한다.
- [ ] 후속 평가 후 다음 기본 문제 이동을 제공한다.
- [ ] 새로고침해도 동일한 후속 질문을 조회한다.

### Phase 7 Gate

- [ ] 일반 Answer 하나당 후속 Question이 최대 하나다.
- [ ] FAILED·NEEDS_REVIEW 평가에서는 후속 질문이 없다.
- [ ] 후속 질문이 다시 후속 질문을 만들지 않는다.
- [ ] 기본 문제 → 후속 질문 → 다음 기본 문제를 완주한다.
- [ ] `AC-005`가 통과한다.

---

## Phase 8 — 운영 안정화와 파일럿

대상: 전체 P0, 비기능 요구사항, `AC-001`~`AC-007`.

### P8-T01 전체 E2E 회귀 테스트

- [ ] 관리자 Topic·Concept·문서·문제 공개 흐름을 자동화한다.
- [ ] 회원가입·로그인·문제 풀이·평가 결과 흐름을 자동화한다.
- [ ] Knowledge State·추천·후속 질문 흐름을 자동화한다.
- [ ] 다른 회원 데이터와 관리자 기능 접근 차단을 자동화한다.
- [ ] AI 실패와 재시도 후 화면 복구를 자동화한다.
- [ ] `AC-001`~`AC-007`을 테스트와 1:1로 연결한다.

### P8-T02 보안 점검

- [ ] 인증 우회와 수평 권한 상승을 점검한다.
- [ ] 관리자 API 전체에 서버 인가가 있는지 점검한다.
- [ ] 입력 길이, HTML 출력과 script injection을 점검한다.
- [ ] prompt injection 답변이 시스템 지침으로 처리되지 않는지 점검한다.
- [ ] session/token, AI key와 DB 비밀번호 노출을 점검한다.
- [ ] rate limit 우회와 과도한 AI 호출을 점검한다.
- [ ] 발견 사항과 조치 결과를 기록한다.

### P8-T03 장애와 데이터 정합성 점검

- [ ] AI timeout, provider 429와 5xx를 재현한다.
- [ ] worker가 평가 도중 종료되는 상황을 재현한다.
- [ ] 중복 작업과 중복 HTTP 요청을 재현한다.
- [ ] 동시 Knowledge State 갱신을 재현한다.
- [ ] 실패 후 Answer, Evaluation과 Knowledge State 정합성을 확인한다.
- [ ] 재시도 불가능한 실패의 운영 처리 절차를 문서화한다.

### P8-T04 관측 가능성

- [ ] requestId를 모든 API 응답과 로그에 연결한다.
- [ ] memberId, answerId와 evaluationId 상관관계를 기록한다.
- [ ] Retrieval 시간, 후보 수와 Evidence ID를 기록한다.
- [ ] AI 모델, evaluator version, latency와 실패 코드를 기록한다.
- [ ] 원문 답변과 비밀번호를 일반 로그에서 제외한다.
- [ ] 실패율과 latency를 확인할 운영 dashboard 또는 query를 준비한다.

### P8-T05 성능 측정

- [ ] 일반 API p95 측정 시나리오와 데이터 크기를 정한다.
- [ ] 평가 접수 응답 p95를 측정한다.
- [ ] AI 평가 완료 p95를 측정한다.
- [ ] 지식 지도와 추천 query 수·실행 시간을 측정한다.
- [ ] N+1 query와 불필요한 전체 이력 조회를 점검한다.
- [ ] 목표 미달 항목의 원인과 대응 계획을 기록한다.

### P8-T06 백업·복구와 콘텐츠 rollback

- [ ] 운영 DB backup 주기와 보존 기간을 정한다.
- [ ] 빈 환경에 backup을 restore한다.
- [ ] 복구 데이터로 회원, 문제, 답변과 Evaluation을 조회한다.
- [ ] 잘못 공개한 문서를 RETIRED 처리하고 이전 버전으로 복구한다.
- [ ] 과거 EvaluationEvidence가 계속 조회되는지 확인한다.
- [ ] 복구 절차와 담당 책임을 문서화한다.

### P8-T07 초기 콘텐츠 준비

- [ ] 초기 Topic과 Concept 체계를 확정한다.
- [ ] Topic별 최소 문제·문서 수인 `OQ-006`을 확정한다.
- [ ] CS 기본 지식 문서의 출처와 라이선스를 검수한다.
- [ ] Java 21, Spring Boot 4.1.x, Spring Framework 7.0.x와 Jakarta Persistence 3.2 버전을 표시한다.
- [ ] 문제별 필수 Concept와 reference answer를 검수한다.
- [ ] 골든 평가 세트와 실제 공개 문제의 편향·중복을 점검한다.

### P8-T08 제한 사용자 파일럿

- [ ] 파일럿 대상과 기간을 정한다.
- [ ] 평가 오판정 신고와 관리자 검토 절차를 정한다.
- [ ] AI 실패율, 평균 처리 시간과 콘텐츠 부족률을 수집한다.
- [ ] 추천이 반복되거나 막히는 사례를 수집한다.
- [ ] 사용자 피드백과 운영 병목을 우선순위화한다.
- [ ] P0 출시 여부와 P1 착수 조건을 결정한다.

### Phase 8 Gate — P0 완료

- [ ] `spec.md`의 기능 요구사항 24개를 모두 구현했다.
- [ ] `AC-001`~`AC-007`이 모두 통과한다.
- [ ] 치명적·높은 우선순위 보안 결함이 없다.
- [ ] 데이터 정합성 결함이 없다.
- [ ] AI 평가 품질 기준을 충족한다.
- [ ] 실패를 관리자가 추적할 수 있다.
- [ ] backup·restore를 실제로 검증했다.
- [ ] 성능 측정 결과와 미달 대응 계획이 있다.
- [ ] 코드 작성 문제, 사용자 문제 게시와 결제 기능이 P0에 포함되지 않았다.

---

## 요구사항 추적 체크리스트

Phase 작업을 완료해도 아래 항목을 다시 확인해야 한다. 이 표는 기능 누락을 찾기 위한 최종 인덱스다.

### 인증과 관리자

- [x] `FR-AUTH-001` 회원가입 — P2-T02, P2-T04
- [x] `FR-AUTH-002` 로그인·로그아웃 — P2-T03, P2-T05
- [x] `FR-AUTH-003` 관리자 인가 — P2-T06
- [x] `FR-ADMIN-001` Topic·Concept 관리 — P3-T02
- [x] `FR-ADMIN-002` KnowledgeDocument 관리 — P3-T03, P3-T04
- [ ] `FR-ADMIN-003` KnowledgeChunk 생성 — P5-T02
- [x] `FR-ADMIN-004` Question 관리 — P3-T05, P3-T06
- [ ] `FR-ADMIN-005` 평가 실패 조회 — P5-T08

### 문제, 답변과 평가

- [ ] `FR-QUESTION-001` 추천 문제 조회 — P6-T05
- [x] `FR-QUESTION-002` 문제 표시 — P1-T05, P1-T06
- [ ] `FR-ANSWER-001` Answer 제출 — P4-T01, P4-T05, P4-T06
- [ ] `FR-ANSWER-002` 중복 제출 방지 — P4-T02
- [ ] `FR-ANSWER-003` 답변 이력 — P4-T06, P4-T07
- [ ] `FR-EVAL-001` 평가 시작 — P4-T03, P4-T05
- [ ] `FR-EVAL-002` 평가 근거 검색 — P5-T03, P5-T07
- [ ] `FR-EVAL-003` 구조화 평가 — P5-T04, P5-T05
- [ ] `FR-EVAL-004` 정오 판정 — P4-T03, P5-T04
- [ ] `FR-EVAL-005` 평가 완료와 실패 — P4-T05, P5-T06, P5-T07

### 개인화와 후속 학습

- [ ] `FR-KNOWLEDGE-001` Concept별 상태 갱신 — P6-T02, P6-T03
- [ ] `FR-KNOWLEDGE-002` 상태 구분 — P6-T01, P6-T02
- [ ] `FR-KNOWLEDGE-003` 지식 지도 조회 — P6-T04, P6-T06
- [ ] `FR-FOLLOWUP-001` 후속 질문 생성 — P7-T01, P7-T02, P7-T03
- [ ] `FR-FOLLOWUP-002` 후속 답변 — P7-T04, P7-T05
- [ ] `FR-PROGRESS-001` 학습 홈 — P6-T04, P6-T06

## 공통 검증 체크리스트

모든 Phase 또는 기능 PR에서 필요한 항목만 복사해 사용한다.

### 정확성

- [ ] 정상 흐름이 요구사항대로 동작한다.
- [ ] 빈 값, 경계값과 존재하지 않는 ID를 처리한다.
- [ ] 허용되지 않는 상태 전이를 차단한다.
- [ ] DB constraint와 애플리케이션 규칙이 일치한다.

### 보안과 소유권

- [ ] 인증이 필요한 API는 비로그인 요청을 거부한다.
- [ ] ADMIN API는 USER 요청을 거부한다.
- [ ] 회원 데이터는 소유자만 조회할 수 있다.
- [ ] 비밀정보와 개인정보가 응답·로그에 노출되지 않는다.

### 데이터와 트랜잭션

- [ ] 실패 시 어느 데이터가 저장되고 rollback되는지 테스트한다.
- [ ] 중복 요청과 동시 요청 결과를 테스트한다.
- [ ] 새 schema가 현재 profile별 schema 정책으로 재현된다.
- [ ] 과거 평가와 콘텐츠 버전이 보존된다.

### API와 화면

- [ ] 요청·응답·오류 계약이 문서화되어 있다.
- [ ] 화면에 loading, empty, success와 error 상태가 있다.
- [ ] 새로고침과 네트워크 재시도 후 상태가 일관된다.
- [ ] 접근할 수 없는 데이터가 프런트 응답에 포함되지 않는다.

### 완료

- [ ] 백엔드 자동 테스트가 통과한다.
- [ ] 프런트 type-check가 통과한다.
- [ ] 프런트 production build가 통과한다.
- [ ] 필요한 브라우저 또는 API 재현 절차를 확인했다.
- [ ] 관련 `spec.md`, `plan.md`, ERD와 ADR을 갱신했다.

## 품질 개선 작업

근거: [2026-09-03 품질 진단](../reviews/2026-09-03-code-quality.md). 기존 체크 상태 유지; 이번 문서 정리에서 구현 완료 재판정 없음.

- [x] **Question이 Concept 불변식을 최종 방어하도록 개선**
  - [x] `addConcept`가 비활성 Concept을 거부한다.
  - [x] `addConcept`가 다른 Topic의 Concept을 거부한다.
  - [x] `replaceConcepts`가 비활성 또는 다른 Topic의 Concept을 거부한다.
  - [x] 교체 검증 실패 시 기존 Concept과 `updatedAt`을 유지한다.
  - [x] 순수 도메인 테스트와 전체 테스트가 통과한다.
- [ ] **Question과 KnowledgeDocument의 버전 생성·공개 전환을 동시 요청에도 원자적으로 보장**
  - [ ] series/version 중복을 DB 제약으로 차단한다.
  - [ ] 동시 버전 생성 충돌을 명시적인 애플리케이션 오류로 변환한다.
  - [ ] series당 공개본 하나를 잠금 또는 DB 모델로 보장한다.
  - [ ] 동시성 통합 테스트로 경쟁 조건을 검증한다.
- [ ] **로그인 시도 제한 저장소의 메모리와 다중 인스턴스 경계를 개선**
  - [ ] 만료와 최대 크기가 있는 저장소로 무제한 메모리 증가를 막는다.
  - [ ] 4회/5회, 10분 window, 14분 59초/15분 경계를 검증한다.
  - [ ] 성공 초기화, 계정/IP 격리와 로그인 ID 정규화를 검증한다.
  - [ ] 다중 인스턴스 도입 전 공유 저장소 전환 조건을 문서화한다.
- [ ] **관리자 UI의 책임을 분리하고 비동기 실패에서도 상태를 복구**
  - [ ] 초기 로드가 실패해도 loading 상태가 종료된다.
  - [ ] Question 폼과 criteria 상태를 composable 또는 하위 컴포넌트로 분리한다.
  - [ ] 관리자 API를 Topic, Concept, KnowledgeDocument, Question, Member 단위로 나눈다.
  - [ ] 생성·수정·검수·공개·폐기·새 버전의 실패 흐름을 테스트한다.
- [ ] **인증과 재시도 UI의 상태 전이·실패·경계 테스트를 보강**
  - [ ] `useAuth`의 401, 예상 밖 오류, 동시 restore, login/logout, CSRF 초기화를 검증한다.
  - [ ] 모듈 전역 인증 상태를 테스트마다 격리한다.
  - [ ] Question 목록·상세의 실패 → 재시도 → 복구 흐름을 검증한다.
  - [ ] 운영 DB 동시성, 다중 인스턴스와 브라우저 E2E의 미검증 경계를 유지한다.
