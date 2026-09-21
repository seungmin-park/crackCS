# 관측 가능성 운영 안내

상태: Actuator·로컬 계측 구현 완료, 운영 Prometheus·Grafana 미연결

현재 기준: [운영 경계와 평가 처리 책임 개선](../2026-09-22-operability/design.md)

## 관리 엔드포인트

```text
업무 포트 :8080 → /api/**
관리 포트 :8081 → /actuator/health, /actuator/prometheus
```

- 관리 포트: `${MANAGEMENT_SERVER_PORT:8081}`
- HTTP 노출: `health`, `prometheus`만 허용
- health 상세 정보: 비노출
- 관리 포트 외부 인터넷 차단: 방화벽·보안 그룹·리버스 프록시 책임
- Prometheus scrape path: `/actuator/prometheus`

## 상관관계

```text
X-Request-Id
    └─ MDC requestId
          └─ evaluationId
               ├─ answerId
               ├─ memberId
               └─ evidenceIds
```

- HTTP 요청: `X-Request-Id`가 canonical UUID이면 유지, 아니면 서버 발급
- HTTP 응답: 같은 `X-Request-Id` 반환
- 오류 응답: body의 `requestId`와 응답 header 일치
- 일반 로그: level 옆 `[requestId:<uuid>]`
- worker 로그: HTTP 요청 밖에서 실행되므로 request ID 없음. evaluation ID로 연결

## 평가 이벤트

| event | 필드 | 의미 |
|---|---|---|
| `evaluation_retrieval_completed` | `evaluationId`, `answerId`, `memberId`, `candidateCount`, `evidenceIds` | 평가에 전달할 근거 검색 종료 |
| `evaluation_completed` | `evaluationId`, `answerId`, `memberId`, `model`, `evaluatorVersion` | 평가 결과와 지식 상태 transaction 완료 |
| `evaluation_failed` | `evaluationId`, `answerId`, `memberId`, `failureCode` | 재시도 예약·최종 실패·검토 필요를 포함한 안전한 실패 결정 |

`evaluation_failed`는 시도 단위 이벤트다. lease 소유자가 재시도·검토 필요·최종 실패 상태를 실제 transaction에 반영한 경우에만 기록한다. 동일 evaluation의 최종 상태는 DB 또는 관리자 평가 API에서 확인한다.

## 조회 명령

로그 경로 예시: `logs/application.log`. 배포 환경에서는 실제 수집 경로로 바꾼다.

요청 하나 추적:

```bash
rg 'requestId:1e85b909-2114-47be-a1c3-1fa47a4a7235' logs/application.log
```

평가 하나 추적:

```bash
rg 'evaluationId=42([[:space:]]|$)' logs/application.log
```

평가 완료·실패 이벤트 수:

```bash
rg -o 'event=evaluation_(completed|failed)' logs/application.log | sort | uniq -c
```

실패 코드별 시도 수:

```bash
rg 'event=evaluation_failed' logs/application.log | rg -o 'failureCode=[A-Z_]+' | sort | uniq -c
```

평가 처리 시간과 호출 수:

```promql
rate(crackcs_evaluation_process_seconds_count[5m])
rate(crackcs_evaluation_process_seconds_sum[5m])
  / rate(crackcs_evaluation_process_seconds_count[5m])
crackcs_evaluation_process_seconds_max
```

- 현재 Timer: 호출 수, 총 실행 시간, 최근 구간 최대 실행 시간 제공
- p95: percentile histogram 설정 전에는 제공하지 않음

retrieval 후보 수와 Evidence ID:

```bash
rg 'event=evaluation_retrieval_completed' logs/application.log
```

## 로그 금지 데이터

- Answer 원문
- KnowledgeChunk 원문
- 비밀번호와 password hash
- OpenAI API key
- DB 비밀번호
- session ID와 CSRF token

`EvaluationOperationLogger`의 public 메서드는 위 값을 받지 않는다. 안전한 숫자 식별자·건수·버전·failure code만 받는다.

## dashboard 연결 기준

- 수집 키: `event`, `requestId`, `evaluationId`, `answerId`, `memberId`
- 경고 후보: 최종 FAILED 비율, `PROVIDER_TIMEOUT`, `PERSISTENCE_ERROR`, 평가 처리 평균·최대 시간
- 실제 임계값: 운영 크기 성능 측정과 제한 파일럿 이후 확정
- 현재 상태: query 제공, dashboard 미연결
