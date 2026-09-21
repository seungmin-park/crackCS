# Phase 8 장애·정합성 자동 증거

상태: 로컬 test provider 검증

## 상태 흐름

```text
Answer 저장
   ↓ Evaluation EVALUATING
provider 호출
   ├─ 성공 ──────────→ EVALUATED → Evidence·Concept → Knowledge State 1회
   ├─ timeout ───────→ 재시도 → FAILED(PROVIDER_TIMEOUT)
   ├─ runtime 오류 ─→ 재시도 → FAILED(PROVIDER_ERROR)
   └─ schema 오류 ──→ 재시도 → FAILED(INVALID_RESULT)

실패 공통: Answer 보존 · Evidence/Concept 없음 · Knowledge State 불변
```

## 자동 증거

| 실패·경쟁 조건 | 대표 테스트 | 확인 결과 |
|---|---|---|
| timeout | `AnswerServiceTest.recordsTimeoutSeparately` | 세 번 뒤 `PROVIDER_TIMEOUT` |
| 429·5xx에 해당하는 provider runtime 오류 | `KnowledgeCompletionFailureTest.preservesProviderFailureRetries` | 재시도 뒤 `PROVIDER_ERROR`, 학습 상태 불변 |
| structured output 위반 | `KnowledgeCompletionFailureTest.rejectsInvalidProviderResultWithoutPartialState` | `INVALID_RESULT`, 원문 보존, 부분 결과 없음 |
| worker 종료와 lease 만료 | `EvaluationTest.claimsWorkAndRecoversExpiredLease`, `AnswerServiceTest.persistsFailureWithoutFourthProviderCall` | 다른 worker 재선점, 최대 3회 |
| 중복 HTTP 요청 | `AnswerFlowTest.deduplicatesConcurrentRequests` | Answer·Evaluation 각 1건 |
| 중복 Evaluation worker | `AnswerServiceTest.serializesDuplicateWorkers` | provider·완료 각 1회 |
| 동시 Knowledge State 갱신 | `KnowledgeCompletionTest.completesConcurrentEvaluationsAtomically` | 두 유효 평가 모두 원자적 반영 |
| 영구 저장 오류 | `KnowledgeCompletionFailureTest.failsPermanentStorageErrorsWithoutAnotherProviderCall` | `PERSISTENCE_ERROR`, 부분 상태 rollback |

## 실행

```bash
./gradlew test --tests '*EvaluationTest' --tests '*AnswerFlowTest' \
  --tests '*AnswerServiceTest' --tests '*KnowledgeCompletionTest' \
  --tests '*KnowledgeCompletionFailureTest' --console=plain
```

## 운영 처리

- `FAILED`: 자동 재시도 종료. `failureReason`, evaluation ID, answer ID로 관리자 확인
- `NEEDS_REVIEW`: 근거 부족·충돌. 학습 상태 반영 금지, 콘텐츠 보강 후 새 답변으로 재평가
- `PERSISTENCE_ERROR`: 자동 provider 재호출 금지. DB constraint·용량·연결 상태 확인 뒤 운영자 조치
- `ATTEMPTS_EXHAUSTED`: worker 중단 또는 장기 처리 여부 확인. 기존 Answer 삭제 금지
- 운영자가 Evaluation 상태를 직접 수정하지 않음. 원인 수정 후 새 요청 또는 승인된 복구 절차 사용

## 제외

- 실제 OpenAI 호출
- 실제 외부 네트워크의 429·5xx body와 header
- 운영 다중 인스턴스에서의 장시간 장애 복구
