# Phase 8 성능 기준선

## 상태

- 구현 완료: 반복 가능한 로컬 H2 Service 측정 작업
- 구현 완료: 답변 목록·지식 지도·추천 query 수 회귀 테스트
- 검증 완료: PostgreSQL 17 Testcontainers 통합 테스트
- 추가 검증: 로컬 PostgreSQL 실제 HTTP, 동시 조회 4·접수 5 — [최신 측정](../2026-09-27-release-readiness/verification.md)
- 미검증: 운영 환경 부하·규모, 실제 OpenAI 지연, 운영 p95

## 목적

- Service 기능 변경 전후의 로컬 상대 비교 기준 제공
- 데이터 건수에 비례하는 N+1 query 회귀 차단
- 운영 SLO나 용량 산정 수치로 사용 금지

## 실행

```bash
./gradlew test --tests '*AnswerQueryCostTest' --tests '*KnowledgeQueryCostTest' --console=plain
./gradlew localServiceLatencyBenchmark --console=plain
./gradlew postgresTest --console=plain
./gradlew httpLatencyBenchmark --console=plain
```

- 보고서: `build/reports/local-service-latency/baseline.json`
- `localServiceLatencyBenchmark`: 일반 `test`에서 제외된 `local-service-latency-benchmark` 태그만 실행
- provider: 평가·후속 질문 OpenAI 모두 비활성
- worker: 평가·후속 질문 background worker 모두 비활성
- database: 격리된 H2 in-memory
- fork: 1

## query 비용

| 흐름 | 비교 데이터 | 판정 |
|---|---:|---|
| 답변 목록 | 답변 1건 / 25건 | 각 호출 최대 7 statements |
| 지식 지도 | 답변 이력 1건 / 25건 | 각 호출 3 statements, Answer entity 추가 load 없음 |
| 추천 | 답변 이력 1건 / 25건 | 각 호출 3 statements, Answer entity 추가 load 없음 |

직관:

```text
이력 1건  ─┐
            ├─ 집계 query + 현재 상태 query ── 고정된 SQL 수
이력 25건 ─┘

행마다 추가 query를 실행하지 않음
```

## 2026-09-21 로컬 H2 Service 기준선

- 환경: Darwin arm64, Java 21 toolchain, H2 in-memory
- fixture: 기존 답변 25건
- warmup: 각 조회 3회, 접수·완료 3회
- sample: 각 흐름 20회

| 흐름 | median (ms) | local p95 (ms) |
|---|---:|---:|
| 답변 목록 | 4.051 | 7.641 |
| 지식 지도 | 0.503 | 0.666 |
| 추천 | 0.541 | 0.627 |
| 답변 접수 | 1.108 | 1.419 |
| 평가 완료 | 6.071 | 7.894 |

이 수치는 Controller·Security filter·HTTP binding·JSON 직렬화를 통과하지 않고 Service를 직접 호출한 로컬 비교값이다. 네트워크, 실제 PostgreSQL 부하, 실제 OpenAI 호출, 동시성, JVM 장시간 예열을 포함하지 않으므로 API 응답 p95나 production p95가 아니다.

## PostgreSQL 확인

- 환경: Docker Engine 29.8.0, Testcontainers PostgreSQL 17 Alpine
- 명령: `JAVA_HOME=... ./gradlew postgresTest --console=plain`
- 결과: 성공
- 포함 범위: persistence, chunk, retrieval, answer service, 고정 retrieval reference 검증
- 제외 범위: 실제 OpenAI 호출, 운영 데이터 규모, 운영 backup/restore

## 실패 판정

- 1건보다 25건에서 query 수 증가: N+1 가능성, 회귀 실패
- `baseline.json` 누락 또는 fixture·warmup·sample·median·p95 누락: 측정 계약 실패
- 이전 로컬 기준선보다 지연 증가: 같은 장비·JDK·fixture로 재측정 후 profiler와 SQL 통계 확인
- PostgreSQL 실패: Docker 상태와 Testcontainers 로그 확인 후 기능 실패와 환경 실패 구분
