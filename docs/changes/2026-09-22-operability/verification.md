# 운영 경계와 평가 처리 책임 개선 검증

## 결론

- 상태: 구현 완료
- 확인일: 2026-09-22
- 환경: Java 21, Spring Boot 4.1.1, H2 in-memory, Testcontainers PostgreSQL 17 Alpine
- 범위: Actuator 운영 경계, 답변 멱등성 이름, 평가 Timer, 평가 처리 책임 분리, 성능·쿼리 테스트 이름
- Git: 기존 미커밋 변경 보존을 위해 commit 없음

## 동작 구조

```text
업무 요청 :8080 ── RequestIdFilter ── /api/**
                                   └─ X-Request-Id + MDC

운영 수집 :8081 ── Actuator ── health / liveness / readiness
                           └─ prometheus

EvaluationProcessor.process(evaluationId)  [Micrometer @Timed]
  ├─ lease 점유와 결과 상태 전이
  ├─ EvaluationAttemptExecutor
  │    ├─ 근거 검색
  │    ├─ 예산 확인
  │    └─ provider 호출
  └─ EvaluationOperationLogger
       └─ 사건·ID·실패 코드만 기록
```

`RequestIdFilter`는 한 HTTP 요청의 응답·오류·로그를 연결한다. `Answer.idempotencyKey`는 같은 답변 제출을 중복 생성하지 않도록 한다. 이름을 분리하되 기존 DB 컬럼 `answer.request_id`는 유지한다.

## TDD 증거

| 범위 | RED | GREEN |
|---|---|---|
| Actuator | 관리 포트 속성·endpoint 부재, 기존 `/api/health` 계속 응답 | 실제 관리 포트 health·probe·Prometheus 응답, `beans` 비노출 |
| 멱등성 이름 | `idempotencyKey` getter·builder·검증 메서드 부재로 compile 실패 | Java 이름 변경, HTTP header와 DB 컬럼 호환 유지 |
| 평가 Timer | logger의 기존 latency 인자와 Timer 부재 | public Spring proxy 경계 Timer 증가, 수동 시간 측정 제거 |
| 처리 책임 | 기존 성공·검토·재시도·lease 테스트로 동작 고정 | 시도 실행을 별도 객체로 이동한 뒤 동일 테스트 통과 |
| 성능 이름 | `localServiceLatencyBenchmark` task 부재 | 새 task와 `baseline.json` 생성 |

## 자동 검증

| 명령 | 실행 | 성공 | 실패 | skipped | 결과 |
|---|---:|---:|---:|---:|---|
| 변경 범위 집중 테스트 | 66 | 66 | 0 | 0 | 성공 |
| `./gradlew test --rerun-tasks --console=plain` | 442 | 442 | 0 | 0 | 성공 |
| `./gradlew localServiceLatencyBenchmark --rerun-tasks --console=plain` | 1 | 1 | 0 | 0 | 성공 |
| `./gradlew postgresTest --rerun-tasks --console=plain` | 37 | 37 | 0 | 0 | 성공 |

집중 테스트 범위:

- Actuator·요청 ID·보안
- Answer 도메인·Controller·Service
- 평가 로그·Timer·완료·검토·재시도·영구 실패
- 답변 이력·지식 지도 Hibernate query 회귀

## 로컬 지연시간 기준선

- fixture 25건, warmup 3회, sample 20회
- 결과 파일: `build/reports/local-service-latency/baseline.json`
- H2 단일 JVM 비교값이며 운영 p95 인증값 아님
- Hibernate `Statistics`는 SQL statement 수와 entity load 수가 데이터 건수에 따라 증가하는 회귀를 탐지하기 위해 query 테스트에 유지

## 정적 점검

- `openapi.yml`: YAML parse 성공
- 업무 OpenAPI의 `/api/health`, `HealthResponse`, `Operations` tag 제거
- 현재 실행 문서의 옛 `phase8Performance` 이름 제거
- `EvaluationOperationLogger`의 `latencyMillis` 제거
- 답변 코드의 `requestId` Java 이름을 `idempotencyKey`로 변경

## 미검증 경계

- 실제 Prometheus scrape와 Grafana dashboard: 외부 인프라 미연결
- 관리 포트 외부 차단: 배포 방화벽·보안 그룹 설정 필요
- 운영 데이터 크기·동시 사용자·network를 포함한 성능: 미측정
- Timer p95: histogram 설정 전 미제공

다음 조건이면 운영 적용 완료로 보지 않는다.

- 관리 포트가 인터넷에 직접 노출됨
- 배포 환경에서 `beans`, `env` 등 비허용 Actuator endpoint가 노출됨
- Prometheus가 `crackcs_evaluation_process_seconds_*`를 수집하지 못함
- PostgreSQL schema validation에서 기존 `request_id` 매핑이 실패함
