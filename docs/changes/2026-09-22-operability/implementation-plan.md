# Operability and Evaluation Responsibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 운영 관례를 도입하고, HTTP 상관 ID와 멱등성 키를 구분하며, 평가 처리의 관측과 책임을 명확히 분리한다.

**Architecture:** Actuator는 별도 관리 포트에서 health와 Prometheus만 노출한다. 평가 processor는 lease와 상태 전이를 조정하고, 근거 검색·예산·provider 호출은 `EvaluationAttemptExecutor`가 수행한다. 실행 시간은 public processor 경계의 Micrometer AOP가 측정하고 도메인 결과는 명시적 로그가 기록한다.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Security, Spring AOP, Micrometer, Prometheus registry, JUnit 5, AssertJ

**Spec:** `docs/changes/2026-09-22-operability/design.md`

## Global Constraints

- 기존 사용자 변경 보존
- production과 test Java 모두 명시적 타입 사용, `var` 금지
- 테스트 메서드마다 자연스러운 한글 `@DisplayName`, 테스트 클래스 `@DisplayName` 금지
- 동작 변경마다 RED 확인 후 최소 GREEN, 이후 REFACTOR
- Service 통합 테스트에서 test-level `@Transactional` 금지
- 고유 ID를 Micrometer tag로 등록하지 않음
- `docs/retrospectives/` staging 금지

## Review Focus

- 별도 관리 포트에서 `health`와 `prometheus`만 노출되고 `beans`는 노출되지 않는가
- custom `SecurityFilterChain` 때문에 Actuator 접근이 차단되지 않는가
- 내부 호출이 아니라 실제 `EvaluationProcessor` 프록시 경계에서 Timer가 증가하는가
- lease를 잃은 평가의 성공·실패 로그가 기록되지 않는가
- Java 필드명 변경 후에도 기존 `answers.request_id` 컬럼을 사용하는가

---

### Task 1: Actuator 운영 경계

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/java/com/example/crackcs/auth/config/SecurityConfiguration.java`
- Delete: `src/main/java/com/example/crackcs/common/web/HealthController.java`
- Replace test: `src/test/java/com/example/crackcs/common/web/HealthControllerTest.java`
- Modify: `src/test/java/com/example/crackcs/auth/security/SecurityConfigurationTest.java`

**Interfaces:**
- Produces: `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus` on the management port
- Removes: public `GET /api/health`

- [ ] **Step 1: Write the failing management endpoint tests**

Create `ActuatorEndpointTest` with a random main port and random management port. Use `@LocalManagementPort` and Java `HttpClient`; assert health and Prometheus return 200 and `/actuator/beans` is rejected by the fallback security chain. Change the security test so unauthenticated `/api/health` no longer returns 200.

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew test --tests '*ActuatorEndpointTest' --tests '*SecurityConfigurationTest' --console=plain
```

Expected: compilation fails because Actuator test annotations are absent or management endpoints do not exist; the old health route expectation also fails after the test contract changes.

- [ ] **Step 3: Add the minimal Actuator implementation**

Add:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'org.springframework.boot:spring-boot-starter-aspectj'
runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
```

Configure:

```yaml
management:
  server:
    port: ${MANAGEMENT_SERVER_PORT:8081}
  endpoints:
    web:
      exposure:
        include: health,prometheus
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
```

Add an ordered Actuator `SecurityFilterChain` using `EndpointRequest.toAnyEndpoint()` and `permitAll()`. Delete `HealthController` and remove `/api/health` from the business chain.

- [ ] **Step 4: Verify GREEN**

Run the Task 1 test command and require exit code 0.

### Task 2: HTTP 상관 ID와 답변 멱등성 키 구분

**Files:**
- Modify: `src/main/java/com/example/crackcs/learning/answer/domain/Answer.java`
- Modify: `src/main/java/com/example/crackcs/learning/answer/controller/request/AnswerSubmitRequest.java`
- Modify: `src/main/java/com/example/crackcs/learning/answer/controller/AnswerController.java`
- Modify: `src/main/java/com/example/crackcs/learning/answer/repository/AnswerRepository.java`
- Modify: `src/main/java/com/example/crackcs/learning/answer/service/DefaultAnswerService.java`
- Modify: answer domain, controller, service tests that use the old Java name

**Interfaces:**
- Keeps: HTTP header `Idempotency-Key`
- Keeps: database column `answers.request_id`
- Produces: Java name `idempotencyKey`

- [ ] **Step 1: Write/modify failing behavior tests**

Update answer tests to call `getIdempotencyKey()` and `validatedIdempotencyKey(...)`; keep assertions that reuse returns the existing answer and malformed UUID input is rejected.

- [ ] **Step 2: Verify RED**

```bash
./gradlew test --tests '*AnswerTest' --tests '*AnswerControllerTest' --tests '*AnswerServiceTest' --console=plain
```

Expected: compilation fails because the domain and request DTO still expose `requestId` names.

- [ ] **Step 3: Implement the naming change**

Map the renamed field explicitly:

```java
@Column(name = "request_id", nullable = false, updatable = false)
private String idempotencyKey;
```

Rename repository query methods and Java variables without changing the HTTP header or schema column.

- [ ] **Step 4: Verify GREEN**

Run the Task 2 test command and require exit code 0.

### Task 3: 평가 시간 측정을 Micrometer AOP로 이동

**Files:**
- Create: `src/main/java/com/example/crackcs/common/observability/ObservabilityConfiguration.java`
- Modify: `src/main/java/com/example/crackcs/evaluation/service/DefaultEvaluationProcessor.java`
- Modify: `src/main/java/com/example/crackcs/evaluation/service/EvaluationOperationLogger.java`
- Create: `src/test/java/com/example/crackcs/evaluation/service/EvaluationProcessMetricsTest.java`
- Modify: `src/test/java/com/example/crackcs/evaluation/service/EvaluationOperationLoggerTest.java`

**Interfaces:**
- Produces: Timer `crackcs.evaluation.process`
- Keeps: semantic events `evaluation_retrieval_completed`, `evaluation_completed`, `evaluation_failed`
- Removes: `latencyMillis` from semantic log method signatures

- [ ] **Step 1: Write the failing metric and logger tests**

In `EvaluationProcessMetricsTest`, read the timer count, call `evaluationProcessor.process(Long.MAX_VALUE)`, and assert the count increased by one. Update the logger test to call methods without latency and assert IDs/results remain present.

- [ ] **Step 2: Verify RED**

```bash
./gradlew test --tests '*EvaluationProcessMetricsTest' --tests '*EvaluationOperationLoggerTest' --console=plain
```

Expected: no `TimedAspect` bean and no `crackcs.evaluation.process` Timer; logger test fails to compile against old signatures.

- [ ] **Step 3: Implement minimal metrics**

Register:

```java
@Bean
public TimedAspect timedAspect(MeterRegistry meterRegistry) {
    return new TimedAspect(meterRegistry);
}
```

Annotate only the externally invoked method:

```java
@Timed(value = "crackcs.evaluation.process", description = "Evaluation processing time")
public void process(Long evaluationId)
```

Remove `System.nanoTime()`, `elapsedMillis(...)`, and latency arguments from `EvaluationOperationLogger`. Do not add IDs as metric tags.

- [ ] **Step 4: Verify GREEN**

Run the Task 3 test command and require exit code 0.

### Task 4: 평가 시도 책임 분리

**Files:**
- Create: `src/main/java/com/example/crackcs/evaluation/service/ClaimedEvaluationWork.java`
- Create: `src/main/java/com/example/crackcs/evaluation/service/EvaluationAttempt.java`
- Create: `src/main/java/com/example/crackcs/evaluation/service/EvaluationAttemptExecutor.java`
- Modify: `src/main/java/com/example/crackcs/evaluation/service/DefaultEvaluationProcessor.java`
- Modify: `src/test/java/com/example/crackcs/learning/mastery/service/KnowledgeCompletionTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/mastery/service/KnowledgeCompletionFailureTest.java`

**Interfaces:**
- `EvaluationAttemptExecutor.execute(ClaimedEvaluationWork)` returns exactly one of completed, review-required, retry-required
- `DefaultEvaluationProcessor` owns lease checks and persistence transitions

- [ ] **Step 1: Confirm the characterization safety net is GREEN**

Run the existing success, provider timeout, invalid result, missing evidence, conflicting evidence, exhausted retry, lost lease and persistence conflict scenarios before moving code:

```bash
./gradlew test --tests '*KnowledgeCompletionTest' --tests '*KnowledgeCompletionFailureTest' --console=plain
```

Expected: all existing behavior tests pass. This task is a behavior-preserving REFACTOR performed after Task 3 GREEN, so it does not invent a compile-failure test for private structure.

- [ ] **Step 2: Implement the minimal responsibility split**

`EvaluationAttemptExecutor` owns retrieval, evidence sufficiency, budget check, provider discovery and provider invocation. It returns values; it does not mutate `Evaluation`.

`DefaultEvaluationProcessor` owns local lock, lease claim, result-to-state transition, completion transaction and explicit semantic logging. Replace `processSerially` with small methods named by intent.

- [ ] **Step 3: Verify behavior preservation**

```bash
./gradlew test --tests '*KnowledgeCompletionTest' --tests '*KnowledgeCompletionFailureTest' --tests '*EvaluationProcessMetricsTest' --console=plain
```

Expected: all success, review, retry, exhausted retry, lease and persistence conflict cases pass.

### Task 5: 성능·쿼리 회귀 테스트 이름 정리

**Files:**
- Rename: `src/test/java/com/example/crackcs/performance/Phase8PerformanceBaselineTest.java` to `LocalServiceLatencyBenchmarkTest.java`
- Modify: `build.gradle`
- Modify: `src/test/java/com/example/crackcs/learning/answer/service/AnswerQueryCostTest.java`
- Modify: `src/test/java/com/example/crackcs/learning/mastery/service/KnowledgeQueryCostTest.java`
- Modify: phase-8 documents that contain executable task/file paths

**Interfaces:**
- Produces Gradle task: `localServiceLatencyBenchmark`
- Produces JUnit tag: `local-service-latency-benchmark`
- Produces report directory: `build/reports/local-service-latency`

- [ ] **Step 1: Rename the test contract first**

Change the test class, tag and task references, then run the new task before the Gradle task exists.

- [ ] **Step 2: Verify RED**

```bash
./gradlew localServiceLatencyBenchmark --console=plain
```

Expected: Gradle reports that the task is not found.

- [ ] **Step 3: Implement task and diagnostic naming**

Rename the Gradle task, description, tag and output path. Rename local `Statistics statistics` variables to `hibernateStatistics`; retain delta-based query/entity-load assertions because they protect N+1 regressions.

- [ ] **Step 4: Verify GREEN**

Run the new benchmark task and both query-cost test classes.

### Task 6: HTTP 계약과 기준 문서 동기화

**Files:**
- Modify: `openapi.yml`
- Modify: `docs/product/spec.md`
- Modify: `docs/planning/tasks.md`
- Modify: `docs/changes/2026-09-21-phase-8/operations.md`
- Modify: `docs/changes/2026-09-21-phase-8/backup-restore.md`
- Modify: `docs/README.md`

**Interfaces:**
- Removes `/api/health` from business API documentation
- Documents management endpoints separately from OpenAPI
- Links this design and implementation plan from the document map

- [ ] **Step 1: Update current-contract documents**

Describe the management port, endpoint allowlist, Prometheus scrape path, internal-network responsibility and renamed benchmark command. Preserve phase documents as history while replacing executable stale commands.

- [ ] **Step 2: Check links and stale executable names**

```bash
rg -n '/api/health|phase8Performance|Phase8PerformanceBaselineTest|phase8-performance' README.md openapi.yml docs build.gradle src
```

Expected: historical prose may identify the former name, but no current command, class, route contract or task points to it.

### Task 7: 전체 검증

**Files:**
- Verify only

**Interfaces:**
- Confirms all preceding contracts together

- [ ] **Step 1: Run focused suites**

```bash
./gradlew test --tests '*ActuatorEndpointTest' --tests '*RequestIdFilterTest' --tests '*AnswerTest' --tests '*AnswerControllerTest' --tests '*AnswerServiceTest' --tests '*EvaluationOperationLoggerTest' --tests '*EvaluationProcessMetricsTest' --tests '*KnowledgeCompletionTest' --tests '*KnowledgeCompletionFailureTest' --tests '*AnswerQueryCostTest' --tests '*KnowledgeQueryCostTest' --console=plain
```

- [ ] **Step 2: Run full default suite**

```bash
./gradlew test --rerun-tasks --console=plain
```

- [ ] **Step 3: Run the renamed benchmark**

```bash
./gradlew localServiceLatencyBenchmark --console=plain
```

- [ ] **Step 4: Record exact counts and boundaries**

Record passed, failed and skipped test counts. State that PostgreSQL/Testcontainers and external Prometheus/Grafana scraping remain separate environment verification unless run explicitly.
