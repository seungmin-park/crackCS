# Phase 8 검증

> 운영 경계·평가 관측·성능 task 이름의 현재 기준은 [후속 설계](../2026-09-22-operability/design.md) 참조. 아래 결과는 당시 검증 이력.

## 결론

- 상태: 부분 완료
- 구현 범위: request ID·health, 공통 오류 진단, CI, 인수/보안 회귀, 장애 정합성, 평가 운영 로그, query 회귀, 로컬 성능 기준선, PostgreSQL 통합 확인, 운영 runbook
- 제외 범위: 실제 OpenAI 호출과 품질·비용·latency Gate
- 외부 미완료: production-equivalent backup·restore, production-size 성능, 실제 참가자 파일럿과 P0 출시 결정
- Git: commit 없음

## 동작 구조

```text
HTTP 요청
  └─ RequestIdFilter ── header + request attribute + MDC
       ├─ Controller 성공
       ├─ 공통 예외 오류 body
       └─ Security 401/403 body
          모두 같은 X-Request-Id

답변 접수 → Evaluation lease → Retrieval → 통제 provider
                                  ├─ 완료 transaction 적용=true  → completed 로그
                                  ├─ lease 상실 적용=false        → 성공 로그 없음
                                  └─ 안전한 실패                  → failed 로그 + 원문 보존
```

상태를 가진 `Evaluation`이 완료 가능 여부를 판단하고, processor는 transaction의 실제 적용 결과를 받아 운영 이벤트를 결정한다. 로그 객체는 원문을 입력받지 않고 ID·건수·버전·latency·failure code만 받는다.

## TDD와 검토 증거

| 범위 | RED | GREEN |
|---|---|---|
| request ID·health | 신규 경계 테스트 18개 중 요구 동작 5개 실패 | 관련 18/18, 인증 흐름 8/8 |
| 프런트 오류 진단 | `ApiClientError`에 `code`, `requestId` 없음 | client 5/5와 type-check 성공 |
| 운영 로그 | `EvaluationOperationLogger` 타입 없음 | logger·평가·worker 관련 suite 성공 |
| 성능 작업 | `localServiceLatencyBenchmark` task 없음 | tagged task와 `baseline.json` 생성 |
| lease 상실 로그 | `PROCESSING`인데 completed/failed 이벤트 기록 | transaction 실제 적용 결과가 `true`일 때만 성공·실패 로그 |

기존에 이미 만족한 보안·장애 동작은 characterization test로 고정했으며 production 코드를 불필요하게 바꾸지 않았다.

## 최종 자동 검증

확인일: 2026-09-21, Java 21 toolchain, 실제 OpenAI 비활성

| 명령 | 실행 | 성공 | 실패 | skipped | 결과 |
|---|---:|---:|---:|---:|---|
| `./gradlew test --rerun-tasks --console=plain` | 437 | 437 | 0 | 0 | 성공 |
| `./gradlew retrievalBenchmark` | 1 | 1 | 0 | 0 | 성공 |
| `./gradlew localServiceLatencyBenchmark` | 1 | 1 | 0 | 0 | 성공 |
| `./gradlew postgresTest` | 37 | 37 | 0 | 0 | 성공 |
| `npm run test` | 285 | 285 | 0 | 0 | 30 files 성공 |
| `npm run type-check` | - | - | - | - | exit 0 |
| `npm run build-only` | - | - | - | - | exit 0, 93 modules |

PostgreSQL 검증 환경:

- Docker Engine 29.8.0
- Testcontainers PostgreSQL 17 Alpine
- reference 검사 14 groups, 질문 60, golden cases 240, 문서 60, 원본 chunk 120
- provider 호출 0

## 성능과 query

- 고정 fixture: 기존 답변 25건
- warmup 3, sample 20, single fork, H2 in-memory
- 보고서: `build/reports/local-service-latency/baseline.json`
- 세부 수치와 한계: [성능 기준선](performance.md)
- 답변 이력 1건/25건에서 답변 목록 최대 7 statements
- 지식 지도 3 statements, 추천 3 statements로 동일
- production p95 아님

## 운영 준비

| 항목 | 상태 | 근거 |
|---|---|---|
| AC-001~AC-006 연결 | 자동 회귀 연결 | [인수 조건 표](acceptance-matrix.md) |
| AC-007 전체 흐름 | 부분 증거, 통합 시나리오 필요 | [인수 조건 표](acceptance-matrix.md) |
| 장애 정합성 | 통제 provider와 실제 Repository 검증 | [장애 표](failure-consistency.md) |
| 관측 query | 구현·문서화 | [운영 로그](operations.md) |
| backup·restore | 절차만 준비 | [복구 절차](backup-restore.md) |
| 콘텐츠 rollback/OQ-006 | 동작 검증·기준 확정 | [콘텐츠 준비](content-readiness.md) |
| 제한 파일럿 | 실행안만 준비 | [파일럿 runbook](pilot-runbook.md) |

`pg_dump --version`과 `pg_restore --version`은 로컬에서 `command not found`였다. 따라서 실제 backup archive 생성과 빈 DB restore는 완료로 보지 않는다. 반면 Gradle의 `retrievalBenchmark`, `localServiceLatencyBenchmark`, `postgresTest` 등록과 실행은 확인했다.

## 정적 점검

- Java main/test `var` 선언: 0건
- 변경 테스트: 모든 test method에 한글 `@DisplayName`, class-level 없음
- `git diff --check`: 성공
- 신규 문서 상대 링크 대상: 확인
- `docs/retrospectives/`, `tobyteam/`: 사용자 untracked 파일, 변경·staging 없음

## 미검증 경계와 실패 조건

- 실제 OpenAI: schema 준수, 정답 일치율, false-correct, timeout·429·5xx, 비용, latency 미측정
- backup: PostgreSQL client 설치와 production-equivalent 격리 환경에서 실제 archive/restore 필요
- 성능: 동시 사용자·network·운영 데이터 크기·장시간 JVM 예열 미포함
- 파일럿: 날짜·참가자·결과·P0/P1 결정 미실행
- CI: workflow 파일 구현, 원격 GitHub Actions 실행 결과는 아직 없음
- OpenAPI: reusable request/response header는 health와 공통 오류에 연결됨. 모든 성공 operation의 생성 client 계약에는 아직 개별 연결되지 않음

다음 중 하나면 Phase 8 완료로 승격하지 않는다.

- 실제 모델 품질 Gate 미달
- production-equivalent 복구 후 핵심 row/API 불일치
- 운영 크기 p95 목표와 대응 계획 부재
- 보안 P0/P1 존재
- 실제 파일럿 중단 조건 충족
