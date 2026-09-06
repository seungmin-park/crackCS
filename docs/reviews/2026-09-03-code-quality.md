# crackCS Code Quality Report

**Date:** 2026-09-03 02:57 KST\
**Target:** `/Users/seungmin/Desktop/repo/crackCS` at `9a0218d` plus current working-tree changes\
**Scale:** 173 implementation/test files, 10,750 LOC (`src/main`, `src/test`, `front/src`)\
**Stack:** Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA, Vue 3, TypeScript, Vite, Vitest

## Score Summary

| Dimension | Score | Grade | Trend |
|---|---:|:---:|:---:|
| Readability | 76 | B | → |
| Consistency | 78 | B | → |
| Maintainability | 74 | C | → |
| Extensibility | 72 | C | → |
| Testability | 82 | B | → |
| Test Quality | 80 | B | → |
| Performance | 68 | C | → |
| Security | 78 | B | → |
| Dependency Mgmt | 72 | C | → |
| **Overall** | **76** | **B** | **→** |

Overall은 명세의 가중치에 따라 계산했다. 이전 `quality-*.md` 보고서가 없어 Trend는 모두 기준선 `→`로 표시했다.

## 1. Readability & Consistency

### 잘된 점

- `Question.update`는 모든 입력과 Topic 변경 조건을 먼저 검증한 후 필드를 대입한다. 검증 실패 시 일부 필드와 `updatedAt`만 바뀌는 부분 수정을 막는다 (`src/main/java/com/example/crackcs/content/question/domain/Question.java:136-156`).
- 상태 변경을 `review`, `publish`, `retire`, `createNextVersion`처럼 의도가 드러나는 도메인 메서드로 표현한다 (`Question.java:199-277`, `KnowledgeDocument.java:222-245`).
- 인증 상태와 동시 복구 요청을 한 composable이 소유한다. `restoring` Promise가 같은 시점의 중복 `/me` 요청을 합친다 (`front/src/composables/useAuth.ts:12-35`).
- Controller는 구현 클래스가 아니라 Service 인터페이스에 의존하고, Entity를 응답으로 직접 내보내지 않는다 (`src/main/java/com/example/crackcs/content/question/controller/QuestionController.java:18-38`).

### 개선점

- 관리자 Vue 화면의 여러 문장이 한 줄에 압축되어 상태 변경 순서를 읽기 어렵다. 특히 선택 객체 갱신, Concept 매핑, 재조회가 한 줄에 결합되어 있다 (`front/src/views/admin/AdminQuestionView.vue:34-35,50-58,68-80,97-111,119-145`; `AdminTaxonomyView.vue:43-67,84-106`). 한 문장에 한 상태 변경을 두고 폼과 criteria 편집을 작은 컴포넌트 또는 composable로 분리하는 편이 안전하다.
- 모든 관리자 타입과 26개 API 함수가 한 파일에 모여 있다 (`front/src/api/admin.ts:4-219`). Topic, Concept, KnowledgeDocument, Question, Member 단위 모듈로 나누면 변경 이유와 소유권이 선명해진다.
- Question 공개 검증 메시지가 한국어와 영어를 섞는다 (`Question.java:206-225`). API 계약과 로그 분석을 위해 오류 코드 또는 한 언어의 메시지 정책으로 통일할 필요가 있다.
- 활성 ADMIN 판정이 Question과 KnowledgeDocument Service에 복제되어 있다 (`DefaultQuestionService.java:177-183`, `DefaultKnowledgeDocumentService.java:168-175`). 역할이나 상태 규칙이 바뀌면 두 구현이 서로 달라질 수 있다.
- 프런트에는 build/type-check/test 스크립트는 있지만 lint/format 검사 항목이 없다 (`front/package.json:6-12`). 현재 보이는 긴 한 줄 표현과 import 순서 차이를 자동으로 잡지 못한다.

## 2. Maintainability & Extensibility

### 책임 구조

```text
HTTP DTO
   ↓
Controller → Service interface → Default Service → Repository
                                      ↓
                              Domain entity
                         상태 + 불변식 + 상태 전이
```

이 구조는 호출자가 구체 구현보다 유스케이스 계약에 의존하게 하고, 상태를 소유한 객체가 변경 규칙도 소유하게 한다. `@Getter`, protected 기본 생성자, private builder 생성자 관례도 일관된다 (`Question.java:35-45,103-134`, `KnowledgeDocument.java:29-46,105-158`).

### 가장 복잡한 생산 코드 5개

| 파일 | 규모/복잡성 | 판단 |
|---|---|---|
| `KnowledgeDocument.java` | 354 LOC | JPA 매핑, 정규화, checksum, 검수, 공개, 폐기, 버전 생성. 책임은 많지만 대부분 상태 소유 객체의 불변식이다. |
| `Question.java` | 331 LOC | 연관 관리, Topic 일치, 가중치, 검수, 상태 전이, 버전 복사를 소유한다. `replaceConcepts`와 `publish`의 규칙 밀도가 높다. |
| `DefaultQuestionService.java` | 185 LOC | 4개 Repository와 9개 유스케이스, Topic/Concept/Admin 정책을 조정한다. |
| `DefaultKnowledgeDocumentService.java` | 181 LOC | 생성부터 공개까지의 유스케이스와 checksum/Topic/Admin 정책을 조정한다. |
| `AdminQuestionView.vue` | 150 LOC | API 12개, 타입 7개, 목록·선택·폼·criteria·상태 전이·버전 생성을 한 화면에서 처리한다. |

### 핵심 설계 문제: 불변식의 소유권

`DefaultQuestionService.toAssignment`는 Concept이 활성 상태인지, Question과 같은 Topic인지 검사한다 (`DefaultQuestionService.java:147-156`). 그러나 공개 도메인 메서드인 `Question.addConcept`와 `replaceConcepts`는 null과 중복만 검사한다 (`Question.java:159-191`).

```text
현재 HTTP 경로
DTO → DefaultQuestionService(활성/Topic 검사) → Question
                                                   ✓

새 배치/새 Service
직접 호출 ───────────────────────────────→ Question.addConcept
                                                   ✗ 최종 방어 없음
```

현재 Controller 경로가 동작하는 이유는 Service가 빠진 규칙을 대신 검사하기 때문이다. 다른 생성 경로가 생기면 비활성 Concept 또는 다른 Topic의 Concept을 연결할 수 있다. Question이 자신의 Topic과 입력 Concept을 모두 알고 있으므로 이 최종 결정은 Question이 소유해야 한다.

### 중복과 확장 지점

- 공개 후 이전 버전을 폐기하는 흐름이 두 Service에 복제되어 있다 (`DefaultQuestionService.java:108-116`, `DefaultKnowledgeDocumentService.java:122-130`).
- 다음 버전 계산도 동일한 `MAX + 1` 흐름이다 (`DefaultQuestionService.java:129-144`, `DefaultKnowledgeDocumentService.java:89-109`).
- KnowledgeDocument의 콘텐츠 정규화와 checksum 계산이 Service와 Entity에 중복된다 (`DefaultKnowledgeDocumentService.java:146-156`, `KnowledgeDocument.java:300-309`). 정규화 규칙이 바뀌면 사전 중복 검사와 저장 값이 달라질 수 있다. 정규화와 hash를 하나의 값 객체가 함께 소유하는 편이 안전하다.
- 새 예외마다 `GlobalExceptionHandler`에 import와 handler가 늘어난다 (`GlobalExceptionHandler.java:4-14,31-185`). 현재 규모에서는 명확하지만 증가 추세를 지켜보고 애플리케이션 예외 계층별 변환으로 묶을 수 있다.

## 3. Testability & Test Quality

### 잘된 점

- 도메인 규칙은 Spring/JPA 없이 직접 검증한다. Question의 검수, 필수 Concept, 가중치 합계와 경계값이 순수 단위 테스트에 있다 (`src/test/java/com/example/crackcs/content/question/domain/QuestionTest.java:141-175,212-284`).
- Service 테스트는 실제 Spring, Repository, 테스트 DB와 rollback을 사용한다 (`QuestionServiceTest.java:29-31`). Controller 테스트는 Service를 mock으로 대체하고 validation 실패 시 호출 없음까지 검증한다 (`QuestionControllerTest.java:186-205,276-352`).
- 핵심 관리자 흐름은 HTTP, 보안, Service, DB를 연결한 별도 통합 테스트로 검증한다 (`AdminContentFlowTest.java:37-40,49-105,144-198`).
- 표본으로 확인한 Java 테스트는 메서드에 한글 `@DisplayName`이 있고 클래스에는 `@DisplayName`이 없다.
- 현재 작업 트리의 격리 변경은 singleton 로그인 시도 상태까지 초기화하도록 `@DirtiesContext`를 필요한 테스트에 적용하고 (`AuthenticationFlowTest.java:34-38`), LoginAttemptService 테스트 컨텍스트를 최소화한다 (`LoginAttemptServiceTest.java:23-26`).

### 개선점

- `useAuth`는 모듈 전역 상태와 Promise를 가진 상태 기계지만 (`front/src/composables/useAuth.ts:12-53`), 테스트는 restore 성공 한 건뿐이다 (`front/src/composables/useAuth.test.ts:19-34`). 401, 예상 밖 오류, 동시 restore 중복 제거, login/logout, CSRF 초기화를 검증해야 한다. 테스트 간에는 module reset 또는 명시적인 상태 reset seam이 필요하다.
- AdminQuestion 화면은 로드·저장·criteria·검수·공개·폐기·새 버전의 비동기 흐름을 직접 처리하지만 (`AdminQuestionView.vue:48-112`), 테스트는 두 정상 흐름만 다룬다 (`AdminQuestionView.test.ts:65-93`). `load()`는 성공 경로에서만 `loading=false`로 바꾸므로 요청 실패 시 영구 loading이 될 수 있다 (`AdminQuestionView.vue:48-60`).
- 로그인 제한의 핵심 경계 중 5회 차단과 15분 후 허용만 검증한다 (`LoginAttemptServiceTest.java:37-59`). 4회 허용, 10분 창 밖 실패 제외, 14분 59초 차단, 성공 시 초기화, 계정/IP 격리와 이메일 정규화가 빠져 있다.
- 도메인이 `LocalDateTime.now()`를 직접 호출해 테스트가 시간 전후 범위로 검증한다 (`Question.java:131-236`, `QuestionTest.java:35-66`). 시계를 주입하거나 상태 전이에 시간을 전달하면 정확하고 반복 가능한 테스트가 된다.
- `AuthServiceTest`의 `deleteAll()` teardown은 현재 직렬 실행에서는 동작하지만 공유 DB 병렬 실행에서는 다른 테스트의 fixture를 지울 수 있다 (`AuthServiceTest.java:42-46`). 일반 경로는 rollback, 실제 rollback 검증만 독립 정리 전략을 사용하는 편이 낫다.
- 재시도 UI 테스트는 버튼 문구만 확인하고 실제 클릭 후 복구를 검증하지 않는다 (`QuestionDetailView.test.ts:57-65`, `QuestionListView.test.ts:64-72`).

## 4. Performance, Security & Dependencies

### Performance

1. `Question.replaceConcepts`는 새 항목마다 기존 replacement 전체를 stream으로 다시 훑어 O(n²) 중복 검사를 한다 (`Question.java:175-191`). Service도 Concept마다 `findById`를 호출한다 (`DefaultQuestionService.java:89-93,147-149`). 요청 DTO에는 목록 크기 제한이 없다 (`QuestionConceptReplaceRequest.java:14-35`). 관리자 API이지만 큰 입력에서 DB 왕복과 CPU 사용이 함께 증가한다. `@Size(max=...)`, ID 일괄 조회, `Set<Long>` 중복 검사가 적절하다.
2. 공개 시 같은 series의 모든 PUBLISHED 엔티티를 List로 읽어 하나씩 retire한다 (`DefaultQuestionService.java:108-116`, `DefaultKnowledgeDocumentService.java:122-130`). 버전 수가 커지면 bulk update 또는 현재 공개본만 찾는 쿼리가 더 안정적이다.
3. 페이지 최대 크기를 100으로 제한하고 (`src/main/resources/application.yaml:4-7`), 정렬 속성을 allowlist로 제한한다 (`PageRequestFactory.java:19-50`). 무제한 목록과 임의 정렬 속성으로 인한 비용/주입 위험을 줄이는 좋은 경계다.

### Security

1. Security 설정은 public 경로를 명시하고, admin 경로에 ADMIN 역할을 요구하며, 나머지를 deny한다 (`SecurityConfiguration.java:34-42`). CSRF 세션 토큰, 세션 ID 변경, HttpOnly/SameSite 쿠키도 설정되어 있다 (`SecurityConfiguration.java:31-65`, `application.yaml:12-20`).
2. 로그인 제한 상태는 singleton 프로세스의 `ConcurrentHashMap`에만 저장된다 (`DefaultLoginAttemptService.java:27-28`). 만료 엔트리는 같은 key가 다시 조회될 때만 제거된다 (`:31-47,72-80`). 공격자가 계속 새 계정/IP 조합을 보내면 map이 계속 커지고, 서버가 여러 대면 각 인스턴스의 제한을 따로 우회할 수 있으며, 재시작하면 제한이 사라진다. 크기 제한/주기 정리 기능이 있는 cache나 공유 저장소가 필요하다.
3. 버전 생성은 `MAX(version) + 1`을 잠금 없이 계산한다 (`DefaultQuestionService.java:129-144`, `DefaultKnowledgeDocumentService.java:89-109`). Question 테이블에는 series/version unique constraint도 없다 (`Question.java:38-44`). 동시 요청 두 개가 같은 버전을 만들 수 있다. KnowledgeDocument는 unique constraint가 있어 중복 저장은 막지만 (`KnowledgeDocument.java:32-40`), 충돌이 정상 도메인 오류로 변환되지 않는다.
4. 공개 전환도 잠금 없이 현재 공개본을 조회해 폐기한다 (`DefaultQuestionService.java:108-116`, `DefaultKnowledgeDocumentService.java:122-130`). 서로 다른 draft를 동시에 공개하면 양쪽 transaction이 상대 변경을 못 보고 같은 series에 공개본 두 개를 남길 수 있다. series 단위 pessimistic lock, version column을 통한 optimistic lock, 또는 DB가 보장하는 current-version 모델이 필요하다.
5. session cookie의 `secure` 기본값이 false다 (`application.yaml:19`). 로컬 편의 기본값으로는 동작하지만 운영 환경 변수가 누락되면 HTTPS에서도 Secure 속성 없는 쿠키가 배포된다. 운영 profile에서 true를 강제하고 시작 시 설정을 검증해야 한다.
6. 예상하지 못한 예외는 내부 메시지를 숨기지만 로그도 남기지 않는다 (`GlobalExceptionHandler.java:173-180`). 클라이언트 노출을 막는 것은 좋지만 서버 원인과 응답의 trace ID를 연결할 수 없어 보안 사고와 장애 분석이 어렵다.

### Dependency Management

- Spring Boot dependency management로 backend 라이브러리 조합을 BOM에 맡기고 (`build.gradle:1-4,21-42`), frontend는 `package-lock.json`으로 실제 설치 버전을 고정한다.
- `npm audit --offline --omit=dev` 결과 production dependency 95개에서 캐시에 알려진 취약점은 0개였다. offline 결과이므로 최신 registry advisory까지 확인했다는 뜻은 아니다.
- Gradle dependency locking/verification과 backend 취약점 스캔 구성이 없고, 저장소에 CI/Dependabot/Renovate 설정도 확인되지 않았다. 재현 가능한 backend resolution과 정기 취약점 검사가 자동화되어 있지 않다.
- H2 console starter와 H2 runtime이 production classpath 구성에 포함된다 (`build.gradle:27-30`). console 활성화는 local profile에만 있지만 (`application-local.yaml:14-16`), 운영 artifact의 불필요한 공격 표면과 크기를 줄이려면 local 전용 구성 또는 배포 artifact 분리를 검토할 수 있다.

## 5. LSP Diagnostics

이 실행 환경에는 Java/TypeScript LSP 진단 결과가 제공되지 않았다. 대신 Gradle 컴파일, Vue TypeScript 검사와 Vite production build를 실행했다. `AuthenticationFlowTest`에서 deprecated API 사용 컴파일 안내가 한 건 있었고 빌드 오류는 없었다.

## 6. Top Issues & Recommendations

### 1. High — Question이 Concept 불변식을 최종 방어하지 않음

- **Affected:** `Question.java:159-191`, `DefaultQuestionService.java:147-156`
- **현상:** 정상 Service 경로만 활성 상태와 Topic 일치를 보장한다.
- **권장:** `Question.addConcept/replaceConcepts`가 Concept 활성 여부와 `this.topic` 일치를 검증하게 하고, Service 검증은 조회와 빠른 오류 변환에 집중한다. 다른 생성 경로에서도 같은 객체가 스스로 유효성을 보장해야 한다.

### 2. High — 버전 생성과 공개 전환이 동시 요청에서 원자적이지 않음

- **Affected:** `DefaultQuestionService.java:108-144`, `DefaultKnowledgeDocumentService.java:89-130`, `Question.java:38-44`, `KnowledgeDocument.java:32-40`
- **현상:** 단일 요청에서는 `MAX + 1`과 이전 공개본 retire가 순서대로 동작한다. 두 transaction이 같은 스냅샷을 읽으면 같은 version 또는 두 PUBLISHED 결과가 가능하다.
- **권장:** series row를 잠그거나 optimistic version을 사용하고, Question에도 `(version_series_id, question_version)` unique constraint를 추가한다. 충돌은 409 도메인 오류로 변환한다. “series당 공개본 하나”는 DB가 표현 가능한 모델로 보강한다.

### 3. High — 로그인 제한기가 무제한 프로세스 메모리에 의존

- **Affected:** `DefaultLoginAttemptService.java:27-47,72-93`
- **현상:** 새 key가 계속 들어오면 자동 축출되지 않고, 다중 인스턴스와 재시작에서 제한 상태가 일관되지 않다.
- **권장:** 최대 크기와 expire-after-access/write를 지원하는 cache를 사용한다. 수평 확장이 시작되면 Redis 같은 공유 저장소와 원자적 카운터/TTL로 옮긴다. 메모리 크기와 차단 횟수 metric도 추가한다.

### 4. Medium — 관리자 UI가 많은 책임을 직접 조정하고 실패 흐름이 얕음

- **Affected:** `AdminQuestionView.vue:4-149`, `AdminTaxonomyView.vue:43-106`, `front/src/api/admin.ts:4-219`
- **현상:** 화면 하나가 API, 폼, selection, criteria와 상태 전이를 함께 소유한다. 초기 요청 하나가 실패하면 loading 해제가 보장되지 않는다.
- **권장:** `try/catch/finally`로 loading과 오류 상태를 항상 종료하고, Question form/criteria를 composable 또는 하위 컴포넌트로 옮긴다. API 모듈도 도메인별로 나눈다.

### 5. Medium — 인증·비동기 UI 테스트가 핵심 실패/경계를 증명하지 않음

- **Affected:** `useAuth.test.ts:19-34`, `AdminQuestionView.test.ts:65-93`, `LoginAttemptServiceTest.java:37-59`, Question list/detail retry tests
- **현상:** 현재 정상 흐름은 통과하지만 singleton 상태, 동시 restore, 401/500, 시간 경계와 실제 retry 복구가 회귀 테스트 밖에 있다.
- **권장:** 모듈 상태 reset seam을 마련하고, 각 상태 전이와 실패 원인별 테스트를 추가한다. 로그인 제한은 4/5회, 10분, 14:59/15:00, 성공 초기화와 key 격리를 각각 한 테스트로 검증한다.

## 7. Key Insights

★ Insight ─────────────────────────────────────\
좋은 도메인 객체는 “정상 호출자가 알아서 검사할 것”을 기대하지 않는다. `Question`이 Topic과 Concept을 모두 알고 있다면 관계 유효성도 Question이 최종 방어해야 새 Service와 배치가 추가되어도 규칙이 유지된다.\
─────────────────────────────────────────────────

★ Insight ─────────────────────────────────────\
transaction은 여러 SQL을 한 덩어리로 commit하지만, 다른 transaction과 같은 값을 읽지 못하게 자동 직렬화하지는 않는다. `MAX + 1`과 “기존 공개본 조회 후 폐기”에는 별도의 잠금 또는 DB 제약이 필요하다.\
─────────────────────────────────────────────────

★ Insight ─────────────────────────────────────\
테스트 격리는 DB rollback만으로 끝나지 않는다. `DefaultLoginAttemptService`의 singleton map과 `useAuth`의 module-level ref처럼 프로세스 메모리에 있는 상태는 context/module 재생성 또는 명시적 reset 경계가 필요하다.\
─────────────────────────────────────────────────

★ Insight ─────────────────────────────────────\
테스트가 버튼 문구를 확인하는 것과 사용자가 복구할 수 있음을 확인하는 것은 다르다. retry 계약은 첫 요청 실패 → 클릭 → 두 번째 요청 성공 → 정상 화면 복원까지 연결해야 증명된다.\
─────────────────────────────────────────────────

## Verification

```text
Backend: ./gradlew test --rerun-tasks
         139 tests, 0 failures, 0 errors, 0 skipped

Frontend: npm test -- --run
          11 files, 28 tests passed

Frontend: npm run build
          vue-tsc + Vite production build succeeded

Dependencies: npm audit --offline --omit=dev
              cached production advisories: 0 vulnerabilities
```

검증하지 않은 경계는 실제 운영 DB의 잠금/격리 동작, 다중 애플리케이션 인스턴스, 최신 온라인 dependency advisory, 브라우저 E2E와 부하 테스트다. 테스트 통과만으로 이 경계까지 안전하다고 판단하지 않았다.
