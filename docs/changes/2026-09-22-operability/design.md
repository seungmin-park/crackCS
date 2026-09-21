# 운영 경계와 평가 처리 책임 개선 설계

## 상태

- 구현 완료
- 적용 범위: HTTP 상관관계, Actuator, 평가 관측, 평가 처리 조정, 성능·쿼리 회귀 테스트 이름
- 기존 `docs/changes/2026-09-21-phase-8/` 문서는 당시 작업 이력으로 보존

## 현상

```text
업무 HTTP
  ├─ /api/health 직접 구현
  ├─ RequestIdFilter
  └─ Answer.requestId = 멱등성 키

평가 처리
  └─ processSerially
       ├─ lease 점유
       ├─ 근거 검색
       ├─ 예산 확인
       ├─ provider 호출
       ├─ 재시도 판단
       ├─ 완료 저장
       ├─ 지식 상태 반영
       ├─ 시간 측정
       └─ 운영 로그
```

- HTTP 요청 상관 ID와 답변 제출 멱등성 키가 모두 `requestId`로 표현됨
- 정적 `/api/health`가 Spring Boot 운영 관례와 별도로 존재
- 평가 처리 메서드가 조정·외부 호출·상태 전이·측정까지 함께 소유
- `System.nanoTime()` 측정이 핵심 흐름 곳곳에 섞임
- `Phase8PerformanceBaselineTest`가 검증 대상보다 개발 단계 이름을 표현
- Hibernate `Statistics statistics`가 테스트 목적을 이름만으로 설명하지 못함

## 결정 기준

- 상태를 소유한 객체가 상태 규칙 소유
- 유스케이스 조정과 외부 평가 시도 분리
- 횡단 관심사는 public Spring bean 경계에서만 AOP 적용
- 도메인 사건과 고유 식별자는 구조화 로그로 기록
- 집계 가능한 시간·횟수는 Micrometer 메트릭으로 기록
- 운영 엔드포인트와 업무 API 계약 분리
- 단계 이름 대신 측정 대상과 책임으로 명명

## 목표 구조

```text
외부 사용자 ── :8080 ── /api/**
                    ├─ RequestIdFilter
                    └─ 업무 SecurityFilterChain

내부 운영망 ── :8081 ── /actuator/health
                    ├─ /actuator/health/liveness
                    ├─ /actuator/health/readiness
                    └─ /actuator/prometheus

EvaluationProcessor.process(evaluationId)  [@Timed]
  ├─ evaluation lease 점유
  ├─ EvaluationAttemptExecutor.execute(work)
  │    ├─ 근거 검색
  │    ├─ 예산 확인
  │    └─ provider 호출
  └─ 결과에 따른 상태 전이·완료 저장
       └─ EvaluationOperationLogger
            ├─ ID·결과·실패 코드
            └─ 수동 latency 없음
```

## Actuator 결정

- `spring-boot-starter-actuator` 추가
- `micrometer-registry-prometheus` 추가
- 기본 관리 포트 `${MANAGEMENT_SERVER_PORT:8081}`
- HTTP 노출 대상 `health,prometheus` 한정
- health 상세 정보 `never`
- 기존 `HealthController`와 `/api/health` 계약 제거
- Actuator 경로는 OpenAPI 업무 계약에서 제외
- 내부망 차단은 배포 방화벽·보안 그룹 책임
- 코드의 Actuator 보안 체인은 노출된 관리 엔드포인트 접근 허용

## AOP 결정

### 적용

- `EvaluationProcessor.process(...)` 전체 처리 시간
- Micrometer `@Timed`와 `TimedAspect` 사용
- Timer가 호출 횟수도 제공하므로 같은 경계에 `@Counted` 중복 적용 금지

### 미적용

- `private processSerially(...)` 같은 내부 호출
- `evaluationCompleted`, `evaluationFailed` 같은 도메인 사건 판정
- `evaluationId`, `answerId`, `memberId` 같은 고카디널리티 값의 메트릭 태그
- 메서드 인자를 자동 출력하는 범용 로깅 Aspect

### 근거

- 김영한 강사 [AOP 적용](https://www.inflearn.com/courses/lecture?courseId=325630&unitId=49601): 시간 측정 같은 공통 관심사를 핵심 로직에서 분리
- 김영한 강사 [프록시와 내부 호출 - 문제](https://www.inflearn.com/courses/lecture?courseId=327901&unitId=94534): 같은 객체의 내부 호출은 프록시를 통과하지 않아 Spring AOP 미적용
- 김영한 강사 [프록시와 내부 호출 - 대안3 구조 변경](https://www.inflearn.com/courses/lecture?courseId=327901&unitId=94537): 별도 책임을 Spring bean으로 분리하여 외부 호출 경계 형성
- 김영한 강사 [메트릭 등록4 - @Timed](https://www.inflearn.com/courses/lecture?courseId=330459&unitId=148168): `TimedAspect` 등록과 public 메서드 시간 측정
- 김영한 강사 [메트릭 정리](https://www.inflearn.com/courses/lecture?courseId=330459&unitId=148170): 낮은 카디널리티는 태그, 고유 ID는 로그 사용

## 이름 결정

| 현재 | 변경 | 이유 |
|---|---|---|
| `Answer.requestId` | `Answer.idempotencyKey` | 답변 제출 중복 방지 역할 표현 |
| `validatedRequestId` | `validatedIdempotencyKey` | HTTP 상관 ID와 구분 |
| `RequestIds` | 유지 | HTTP 요청 상관 ID 유틸리티로 패키지 문맥이 명확함 |
| `processSerially` | 제거 | 구현 방식이 아닌 책임 단위 객체로 분리 |
| `PendingEvaluation` | `ClaimedEvaluationWork` | lease를 점유한 평가 작업이라는 상태 표현 |
| `Phase8PerformanceBaselineTest` | `LocalServiceLatencyBenchmarkTest` | 측정 대상과 환경 표현 |
| `phase8Performance` | `localServiceLatencyBenchmark` | Gradle task도 동일한 의미 사용 |
| `Statistics statistics` | `Statistics hibernateStatistics` | Hibernate 계측 객체임을 표현 |

DB 컬럼 `answer.request_id`는 기존 데이터 호환을 위해 이번 변경에서 유지하고 Java 필드만 `idempotencyKey`로 명명한다.

## 실패 조건

- `/api/health`가 계속 200을 반환함
- `beans`, `env` 등 허용하지 않은 Actuator endpoint가 HTTP로 노출됨
- 평가 처리가 실행되어도 `crackcs.evaluation.process` Timer가 증가하지 않음
- 재시도 예약을 AOP가 정상 성공으로 해석했다는 이유로 도메인 실패 로그가 사라짐
- 평가 ID를 메트릭 태그로 사용하여 시계열이 평가 건수만큼 증가함
- 리팩터링 후 lease 소유자가 아닌 worker가 상태를 변경함
- Java 필드명 변경으로 기존 `request_id` 컬럼 호환이 깨짐

## 검증 범위

- Actuator 실제 관리 포트 HTTP 계약
- 업무 포트에서 `/api/health` 제거
- 평가 Timer 증가
- 기존 평가 성공·검토·재시도·영구 실패·동시성 테스트
- 답변 멱등성 API와 저장 동작
- 로컬 성능 benchmark task 발견·실행
- 전체 단위·통합 테스트

구현·검증 결과는 [검증 문서](verification.md)에서 확인한다.
