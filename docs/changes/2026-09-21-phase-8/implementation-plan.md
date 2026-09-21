# Phase 8 운영 안정화 Implementation Plan

> 운영 경계·평가 관측·성능 task 이름은 [후속 구현 계획](../2026-09-22-operability/implementation-plan.md)으로 대체됨. 아래 내용은 당시 실행 계획 이력으로 보존.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 실제 OpenAI 호출을 제외하고 Phase 8의 로컬 자동 검증 기반, 운영 절차, 콘텐츠·파일럿 준비 상태를 구현하고 증거가 확인된 체크리스트만 완료 처리한다.

**Architecture:** HTTP 요청 경계가 request ID 수명주기를 소유하고 공통 오류·보안 응답이 같은 ID를 사용한다. 기존 도메인과 Service의 정합성 규칙은 유지하며 통제 가능한 provider와 실제 Repository를 사용한 회귀 테스트로 장애를 검증한다. 운영환경이 필요한 성능·복구·파일럿 항목은 실행 가능한 명령과 판정표를 제공하되 실제 증거가 없으면 완료 처리하지 않는다.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA, JUnit 5, MockMvc, H2, PostgreSQL 17/Testcontainers, Vue 3, TypeScript 6, Vitest 4, GitHub Actions

**Spec:** `docs/changes/2026-09-21-phase-8/design.md`

## Global Constraints

- 실제 OpenAI provider 호출, 품질·비용·p95 측정 제외
- Production 코드보다 실패 테스트 먼저 작성
- Java 운영·테스트 코드에서 `var` 금지
- Service 인터페이스와 `Default...` 구현 구조 유지
- Service 통합 테스트는 실제 Repository와 `@AfterEach` FK 역순 정리 사용
- `docs/retrospectives/`, `tobyteam/` 변경 금지
- Git commit 생성 금지
- 실행 증거 없는 Phase 8 항목 완료 처리 금지

## Review Focus

- 유효하지 않은 `X-Request-Id` 입력: 서버가 새 UUID를 발급하고 요청값을 로그·응답에 사용하지 않아야 함
- Security filter에서 끝나는 401·403: 응답 헤더와 오류 body가 같은 request ID를 가져야 함
- provider timeout·일반 실패·잘못된 결과: Answer 보존, Evaluation 안전한 실패 사유, Knowledge State 불변이어야 함
- 다른 회원의 ID를 query/path에 넣은 요청: 인증 principal의 회원 범위를 벗어나지 않아야 함
- 운영 증거가 없는 backup·성능·파일럿: 준비 문서와 실제 완료 체크가 구분되어야 함

---

### Task 1: request ID와 health 경계

**Files:**
- Create: `src/main/java/com/example/crackcs/common/web/RequestIdFilter.java`
- Create: `src/main/java/com/example/crackcs/common/web/RequestIds.java`
- Create: `src/main/java/com/example/crackcs/common/web/HealthController.java`
- Create: `src/test/java/com/example/crackcs/common/web/RequestIdFilterTest.java`
- Create: `src/test/java/com/example/crackcs/common/web/HealthControllerTest.java`
- Modify: `src/main/java/com/example/crackcs/common/web/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/example/crackcs/auth/security/SecurityErrorResponseWriter.java`
- Modify: `src/test/java/com/example/crackcs/common/web/GlobalExceptionHandlerTest.java`
- Modify: `src/test/java/com/example/crackcs/auth/security/SecurityConfigurationTest.java`
- Modify: `openapi.yml`

**Interfaces:**
- Consumes: incoming optional `X-Request-Id` header
- Produces: `RequestIds.current(HttpServletRequest)`, request attribute `crackcs.requestId`, MDC key `requestId`, response header `X-Request-Id`
- Produces: `GET /api/health` → `200 {"status":"UP"}` without authentication

- [x] **Step 1: Write failing filter and error-contract tests**

Add tests with Korean `@DisplayName` for these literal outcomes:

```java
mockMvc.perform(get("/api/health").header("X-Request-Id", requestId))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Request-Id", requestId))
        .andExpect(jsonPath("$.status").value("UP"));

mockMvc.perform(get("/api/questions").header("X-Request-Id", requestId))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Request-Id", requestId))
        .andExpect(jsonPath("$.requestId").value(requestId));
```

Also assert that `not-a-uuid` is replaced by a canonical UUID and that the same value appears in the response header and body.

- [x] **Step 2: Verify RED**

Run:

```bash
./gradlew test --tests '*RequestIdFilterTest' --tests '*HealthControllerTest' --tests '*GlobalExceptionHandlerTest' --tests '*SecurityConfigurationTest' --console=plain
```

Expected: new test classes fail to compile because `RequestIdFilter`, `RequestIds`, and `HealthController` do not exist; existing random error IDs fail equality assertions after compilation scaffolding.

- [x] **Step 3: Implement request-scoped correlation**

`RequestIds` owns constants and lookup:

```java
public final class RequestIds {
    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "crackcs.requestId";
    public static final String MDC_KEY = "requestId";

    public static String current(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        return value instanceof String requestId ? requestId : UUID.randomUUID().toString();
    }
}
```

`RequestIdFilter` extends `OncePerRequestFilter`: accept only a canonical UUID, otherwise create one; set request attribute, response header and MDC before `filterChain.doFilter`; remove MDC in `finally`.

Inject `HttpServletRequest` into `GlobalExceptionHandler.error(...)` and `SecurityErrorResponseWriter.write(...)`, replacing `UUID.randomUUID()` with `RequestIds.current(request)`.

Implement health response as a record with only `status` so database/provider state and secrets are not exposed.

- [x] **Step 4: Verify GREEN and contract**

Run the Task 1 command again, then:

```bash
./gradlew test --tests '*AuthenticationFlowTest' --console=plain
```

Expected: all selected tests pass and 401/403/400/404/409/500 bodies keep the existing schema with the correlated ID.

- [x] **Step 5: Update OpenAPI**

Add `/api/health`, the optional reusable request header, and the response `X-Request-Id` header. Keep `ApiErrorResponse.requestId` as UUID and document that it equals the response header.

---

### Task 2: 프런트 오류 경계와 CI

**Files:**
- Modify: `front/src/api/client.ts`
- Modify: `front/src/api/client.test.ts`
- Create: `.github/workflows/ci.yml`
- Modify: `README.md`
- Modify: `front/README.md`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/resources/application-postgres.yaml`

**Interfaces:**
- Consumes: backend `ApiErrorResponse { code, message, fieldErrors, requestId }`
- Produces: `ApiClientError.status`, `.code`, `.requestId`, `.fieldErrors`
- Produces: CI jobs `backend` and `frontend`

- [x] **Step 1: Write the failing frontend error test**

```ts
it("서버 오류의 코드와 requestId를 진단 정보로 보존한다", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({
    code: "INTERNAL_SERVER_ERROR",
    message: "서버 오류",
    fieldErrors: [],
    requestId: "1e85b909-2114-47be-a1c3-1fa47a4a7235",
  }), { status: 500, headers: { "Content-Type": "application/json" } })));

  await expect(get("/api/questions")).rejects.toMatchObject({
    status: 500,
    code: "INTERNAL_SERVER_ERROR",
    requestId: "1e85b909-2114-47be-a1c3-1fa47a4a7235",
  });
});
```

- [x] **Step 2: Verify RED**

Run `cd front && npm run test -- src/api/client.test.ts`.

Expected: `ApiClientError` has no `code` or `requestId`.

- [x] **Step 3: Implement the minimal client contract**

Extend `ApiErrorBody` and the error constructor with optional `code` and `requestId`; preserve the current fallback message and session-expiration behavior.

- [x] **Step 4: Verify frontend GREEN**

Run:

```bash
cd front
npm run test -- src/api/client.test.ts
npm run type-check
```

- [x] **Step 5: Add deterministic CI**

Create one workflow triggered by `push` and `pull_request`:

```yaml
jobs:
  backend:
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "21", cache: gradle }
      - run: ./gradlew test --console=plain
  frontend:
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version-file: front/.nvmrc, cache: npm, cache-dependency-path: front/package-lock.json }
      - run: npm ci
        working-directory: front
      - run: npm run test
        working-directory: front
      - run: npm run type-check
        working-directory: front
      - run: npm run build-only
        working-directory: front
```

Document Java 21, Node 24, npm 11, profile responsibility, `DATABASE_*`, `SESSION_COOKIE_SECURE`, `OPENAI_ENABLED=false`, and verification commands. Do not print secret values.

---

### Task 3: AC-001~AC-007와 보안 회귀 연결

**Files:**
- Modify: existing acceptance tests under `src/test/java/com/example/crackcs/`
- Create: `docs/changes/2026-09-21-phase-8/acceptance-matrix.md`
- Modify: `src/test/java/com/example/crackcs/auth/security/SecurityConfigurationTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/answer/controller/AnswerControllerTest.java`
- Modify: `src/test/java/com/example/crackcs/evaluation/adapter/openai/OpenAiEvaluationAdapterTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/followup/adapter/OpenAiFollowUpQuestionAdapterTest.java`

**Interfaces:**
- Consumes: product acceptance scenarios `AC-001`~`AC-007`
- Produces: one named automated evidence row per AC with test class, method, level, and command

- [x] **Step 1: Audit existing acceptance evidence**

Map without changing behavior:

```text
AC-001 → recommendation/knowledge flow
AC-002 → answer + evaluation + evidence + knowledge completion
AC-003 → provider failure + answer preservation + no knowledge mutation
AC-004 → UNKNOWN versus LEARNING knowledge response
AC-005 → follow-up generation/answer/next recommendation
AC-006 → USER session rejected from admin API
AC-007 → document version/chunk/evidence preservation
```

Record an exact test method for each row. A row with no single adequate test becomes the next RED test.

- [x] **Step 2: Write only missing failing acceptance/security tests**

Required observable assertions:

- every `/api/admin/**` controller family rejects USER with 403;
- answer detail/history ignores supplied foreign member identifiers and uses principal ownership;
- HTML and `<script>` text is stored/returned as data, never inserted through `v-html`;
- provider request JSON places system instructions separately from answer/evidence strings containing `ignore previous instructions`;
- no API response contains password hash, API key, session ID, or CSRF token outside the CSRF endpoint.

Each new Java test gets a natural Korean `@DisplayName`; no class-level `@DisplayName`.

- [x] **Step 3: Verify RED for each actual gap**

Run the exact new test method with `./gradlew test --tests 'fully.qualified.Class.method' --console=plain`. If it passes initially, mark it as existing evidence rather than modifying production code.

- [x] **Step 4: Implement the smallest security fix for genuine failures**

Keep authorization in `SecurityConfiguration` and ownership in controllers/services using `AuthenticatedMember.memberId()`. Keep prompt instructions and untrusted content as separate JSON message/input fields; never sanitize by deleting user text because evaluation must preserve the submitted answer.

- [x] **Step 5: Verify the security slice**

Run:

```bash
./gradlew test --tests '*SecurityConfigurationTest' --tests '*AuthenticationFlowTest' --tests '*AnswerControllerTest' --tests '*OpenAiEvaluationAdapterTest' --tests '*OpenAiFollowUpQuestionAdapterTest' --console=plain
```

No network call is permitted; adapter tests use the existing fake `OpenAiResponsesClient`.

---

### Task 4: provider 장애와 데이터 정합성 회귀

**Files:**
- Modify: `src/test/java/com/example/crackcs/learning/mastery/service/KnowledgeCompletionFailureTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/answer/service/EvaluationWorkerTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/answer/service/AnswerServiceTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/mastery/service/KnowledgeStateServiceTest.java`
- Modify only if a RED test exposes a defect: `src/main/java/com/example/crackcs/evaluation/service/DefaultEvaluationProcessor.java`
- Modify only if a RED test exposes a defect: relevant domain object or Repository

**Interfaces:**
- Consumes: controlled `EvaluationPort` outcomes timeout, rate-limit-like failure, 5xx-like failure, invalid result
- Produces: durable Evaluation state and safe `failureReason`; never calls OpenAI

- [x] **Step 1: Add failing tests for uncovered branches**

Add separate tests for:

```text
EvaluationTimeoutException → PROVIDER_TIMEOUT → third attempt FAILED
runtime 429/5xx analogue  → PROVIDER_ERROR   → retry then FAILED
invalid structured result → INVALID_RESULT  → retry then FAILED
worker dies after claim    → expired lease is claimable again
same idempotency key twice → one Answer and one Evaluation
same Evaluation processed concurrently → one knowledge application
```

Every failure test asserts Answer count/content remains, `KnowledgeStateRepository.count()` remains zero until a valid completion, and no partial Evidence/Concept rows remain.

- [x] **Step 2: Verify each RED independently**

Use method-specific Gradle commands. Expected failures must be missing behavior or wrong durable state, not fixture/lock timing errors.

- [x] **Step 3: Implement minimal fixes in the state owner**

If a state transition is wrong, change `Evaluation`; if duplicate Answer creation is wrong, change the Answer repository/transaction boundary; if lease ownership is wrong, change Evaluation claim logic. Do not put domain decisions into controller code.

- [x] **Step 4: Verify related GREEN**

Run:

```bash
./gradlew test --tests '*KnowledgeCompletionFailureTest' --tests '*EvaluationWorkerTest' --tests '*AnswerServiceTest' --tests '*KnowledgeStateServiceTest' --console=plain
```

---

### Task 5: 관측 가능성과 민감정보 경계

**Files:**
- Create: `src/main/java/com/example/crackcs/evaluation/service/EvaluationOperationLogger.java`
- Create: `src/test/java/com/example/crackcs/evaluation/service/EvaluationOperationLoggerTest.java`
- Modify: `src/main/java/com/example/crackcs/evaluation/service/DefaultEvaluationProcessor.java`
- Modify: `src/main/java/com/example/crackcs/evaluation/service/EvaluationWorker.java`
- Modify: `src/main/java/com/example/crackcs/evaluation/retrieval/DefaultKnowledgeRetrievalService.java`
- Create: `docs/changes/2026-09-21-phase-8/operations.md`

**Interfaces:**
- Consumes: IDs, counts, durations, model/rule version, safe failure code
- Produces: structured key/value log events without answer/evidence/password/key contents

- [x] **Step 1: Write a failing logger contract test**

Use a Logback test appender and literal sensitive sentinel strings. Assert emitted messages contain `evaluationId`, `answerId`, `memberId`, candidate count, evidence IDs, latency and safe failure code, while they do not contain the answer sentinel, password sentinel, API key sentinel, or evidence content sentinel.

- [x] **Step 2: Verify RED**

Run `./gradlew test --tests '*EvaluationOperationLoggerTest' --console=plain`.

Expected: class does not exist.

- [x] **Step 3: Implement the focused logging collaborator**

Expose intent methods rather than a generic map:

```java
void retrievalCompleted(Long evaluationId, Long answerId, Long memberId,
                        int candidateCount, List<Long> evidenceIds, long latencyMillis);
void evaluationCompleted(Long evaluationId, Long answerId, Long memberId,
                         String model, String evaluatorVersion, long latencyMillis);
void evaluationFailed(Long evaluationId, Long answerId, Long memberId,
                      String failureCode, long latencyMillis);
```

The collaborator accepts no answer text, password, API key, session or CSRF value, making sensitive logging impossible through its API.

- [x] **Step 4: Integrate at transaction-safe boundaries and verify GREEN**

Log after retrieval result exists and after durable completion/failure decisions. Keep worker catch logging to identifiers and exception type only. Run the logger test plus `*KnowledgeCompletionFailureTest`.

- [x] **Step 5: Document operational queries**

In `operations.md`, give exact grep/JSON-log query fields for failure rate, p95 latency, evaluation trace and retrieval evidence trace. State that raw answer and credentials are prohibited log fields.

---

### Task 6: 성능과 PostgreSQL 검증

**Files:**
- Modify: `src/test/java/com/example/crackcs/learning/answer/service/AnswerQueryCostTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/mastery/service/KnowledgeQueryCostTest.java`
- Create: `src/test/java/com/example/crackcs/performance/LocalServiceLatencyBenchmarkTest.java`
- Modify: `build.gradle`
- Create: `docs/changes/2026-09-21-phase-8/performance.md`

**Interfaces:**
- Consumes: fixed H2 fixture size and optional PostgreSQL Testcontainers profile
- Produces: bounded query counts and a non-default `localServiceLatencyBenchmark` report under `build/reports/local-service-latency/`

- [x] **Step 1: Write failing scale-invariance tests**

Create 1 and 25 item fixtures and assert query counts do not grow per row for answer history, knowledge map and recommendation. Assert the report records fixture size, warmup count, sample count, median and p95 in milliseconds.

- [x] **Step 2: Verify RED**

Run `./gradlew localServiceLatencyBenchmark --console=plain` after registering the task shell; expected failure is the missing report-producing test behavior.

- [x] **Step 3: Add the non-gating measurement task**

Register a tagged JUnit task `localServiceLatencyBenchmark`, isolated H2 URL, worker disabled, fixed single fork, `outputs.upToDateWhen { false }`. Do not make wall-clock thresholds part of the normal unit suite; query-count regressions remain deterministic gates.

- [x] **Step 4: Verify query cost and measurement**

Run:

```bash
./gradlew test --tests '*AnswerQueryCostTest' --tests '*KnowledgeQueryCostTest' --console=plain
./gradlew localServiceLatencyBenchmark --console=plain
```

Record measured values and environment in `performance.md`; do not label them production p95.

- [x] **Step 5: Run PostgreSQL checks when Docker is available**

Run `./gradlew postgresTest --console=plain`. Record success or the exact Docker/environment blocker. No checkbox requiring PostgreSQL evidence is completed on a skipped run.

---

### Task 7: backup·restore, 콘텐츠 rollback, 초기 콘텐츠, 파일럿 runbook

**Files:**
- Create: `docs/changes/2026-09-21-phase-8/backup-restore.md`
- Create: `docs/changes/2026-09-21-phase-8/content-readiness.md`
- Create: `docs/changes/2026-09-21-phase-8/pilot-runbook.md`
- Modify: `docs/product/spec.md`
- Modify: `docs/product/content-and-ai-policy.md`
- Modify: `docs/evaluation/reference-v1/README.md`

**Interfaces:**
- Consumes: PostgreSQL `pg_dump`/`pg_restore`, domain retire/version behavior, reference-v1 manifest
- Produces: exact commands, expected row-level checks, owners, stop conditions, evidence locations

- [x] **Step 1: Write the backup/restore procedure**

Specify explicit environment variable names without values, custom-format `pg_dump`, restore into a named empty database, and SQL/API checks for Member, Question, Answer, Evaluation and EvaluationEvidence. Include restore failure handling and backup retention: daily 7 days, weekly 4 weeks, monthly 3 months as the proposed pilot baseline, labeled as a project decision.

- [x] **Step 2: Write content rollback verification**

Document and link the existing commands/tests proving: published document is retired, previous version/chunks remain, new evaluations use only current published chunks, historical EvaluationEvidence still resolves old chunks.

- [x] **Step 3: Fix OQ-006 and content readiness criteria**

Change OQ-006 from open to the existing proposal: each leaf Topic requires at least five published questions and one published knowledge document covering every required Concept. Require Java 21, Spring Boot 4.1.x, Spring Framework 7.0.x and Jakarta Persistence 3.2 labels where relevant; require source URL/locator, license note, reviewer and review date.

- [x] **Step 4: Write the pilot runbook**

Define pilot population, proposed duration, incident/report intake, admin review SLA, failure rate, completion latency, content-gap rate, repeated-recommendation rate, blocked-user cases, stop conditions and P0/P1 decision fields. Leave actual dates, participants and results explicitly unexecuted.

- [x] **Step 5: Validate document commands and links**

Run each read-only command locally where possible (`pg_dump --version`, Gradle task listing, relative-link scan). Record unavailable external dependencies rather than fabricating results.

---

### Task 8: 전체 검증과 근거 기반 체크

**Files:**
- Create: `docs/changes/2026-09-21-phase-8/verification.md`
- Modify: `docs/planning/tasks.md`
- Modify: `docs/planning/plan.md`
- Modify: `docs/README.md`

**Interfaces:**
- Consumes: fresh command outputs from Tasks 1~7
- Produces: test counts, pass/fail counts, unverified boundaries, checked task items with direct evidence links

- [x] **Step 1: Run backend verification**

```bash
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-21.42.21/zulu-21.jdk/Contents/Home ./gradlew test --rerun-tasks --console=plain
```

Record exact executed/passed/failed/skipped counts from XML or Gradle output.

- [x] **Step 2: Run optional operational suites**

```bash
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-21.42.21/zulu-21.jdk/Contents/Home ./gradlew retrievalBenchmark localServiceLatencyBenchmark --console=plain
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-21.42.21/zulu-21.jdk/Contents/Home ./gradlew postgresTest --console=plain
```

Run PostgreSQL only when Docker is available; record a blocker otherwise.

- [x] **Step 3: Run frontend verification**

```bash
cd front
npm run test
npm run type-check
npm run build-only
```

Record exact Vitest counts and command exit codes.

- [x] **Step 4: Run static completion checks**

```bash
rg -n "@DisplayName" src/test/java
rg -n "\bvar\b" src/main/java src/test/java
git diff --check
git status --short
```

Review all changed tests for a Korean method-level `@DisplayName`, no class-level `@DisplayName`, and no Java `var` declaration.

- [x] **Step 5: Update status without overclaiming**

Check only items directly supported by fresh evidence. Keep these incomplete unless actually executed outside this local implementation:

- actual OpenAI quality/cost/latency Gate;
- production-equivalent PostgreSQL backup restore;
- production-size p95;
- real participant pilot and P0 release decision.

Change Phase 8 overall status to `부분 완료` unless every Phase completion Gate has external evidence. Link `design.md`, this plan and `verification.md` from `docs/README.md`.

- [x] **Step 6: Confirm no commit**

Run `git status --short` and report the working-tree changes. Do not run `git add` or `git commit`.
