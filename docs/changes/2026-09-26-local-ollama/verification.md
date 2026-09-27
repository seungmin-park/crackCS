# 로컬 gpt-oss 평가 후보

## 현재 상태

- 범위: 답변 평가 `EvaluationPort`의 로컬 Ollama 후보
- 기본 실행: 기존 stub. `local,ollama`에서만 `gpt-oss:20b` 선택
- 후속 질문 생성: local stub 유지
- 출시 모델 결정: 보류. `reference-v1` 전체 품질·지연 실측 필요

## 동작과 책임

```text
평가 Worker
   ↓ EvaluationPort
OllamaEvaluationAdapter ── /api/chat ──▶ Ollama / gpt-oss:20b
   │
   ├─ EvaluationPrompt: 지시와 평가 대상 분리
   ├─ EvaluationOutputSchema: OpenAI·Ollama 공통 출력 계약
   └─ EvaluationResultParser: 판정·개념·근거 필드 검증
```

- Ollama 요청: `stream=false`, JSON schema를 `format`에 전달
- Ollama 응답: `done`, 중단 사유, 모델 이름, 토큰 수, JSON 결과 확인 후 도메인 결과로 변환
- provider 장애: 기존 Worker의 timeout·invalid-result·provider-error 재시도 경로 사용
- 모델·평가기 버전: 결과에 저장. 공통 prompt·schema의 기본 버전은 `os-evaluator-v1`; 로컬 추론 강도를 접미사로 추가해 `os-evaluator-v1-medium` 또는 `os-evaluator-v1-low`로 구분
- local API 호출 비용: 토큰 USD 단가 0. 전력·하드웨어 운영비 제외
- 기본 3분 HTTP 제한과 4분 평가 lease: 느린 로컬 추론 중 lease 선점 방지

## 재현 명령

```bash
brew install ollama
brew services start ollama
ollama pull gpt-oss:20b
ollama list
./gradlew bootRun --args='--spring.profiles.active=local,ollama'
./gradlew ollamaLiveEvaluation
```

전체 분할 측정 명령(스모크 결과 확인과 prompt 고정 후):

```bash
./gradlew ollamaLiveEvaluation -PollamaSplit=development -PollamaCaseLimit=138 -PollamaReport=build/reports/evaluation/ollama-development.json
./gradlew ollamaLiveEvaluation -PollamaSplit=evaluation-candidate -PollamaCaseLimit=42 -PollamaReport=build/reports/evaluation/ollama-candidate.json
```

- MacBook Air M2, 통합 메모리 16GB: 공식 권장 하한
- 모델 파일: 약 13GB. 최초 다운로드와 추론 속도는 네트워크·메모리 상황에 영향
- 버전·주소·제한 시간 변경: 루트 [README](../../../README.md)의 환경 변수 표 참조
- 기본 스모크 출력: `build/reports/evaluation/ollama-smoke.json`. `reference-v1` development의 provider 대상 첫 4건만 사용. 라벨은 모델 입력과 별도로 읽음
- provider 대상: development 138건, evaluation-candidate 42건. `INSUFFICIENT_EVIDENCE` 60건은 실제 실행에서 검색 단계가 모델 호출 전에 차단하므로 제외. 해당 경로는 retrieval·Worker 검증이 소유
- 측정 출력: 사례별 기대·실제 판정, 시간, 토큰 수, 오류 종류. 요약의 판정 비율은 기존 `GoldenSetMetrics` 사용. 누락 응답은 `completed`와 `schemaSuccessRate`에 반영
- 첫 4건의 p95: 연결 상태 참고값. 출시 목표 판정에는 전체 분할과 실제 배포 환경 측정 필요

## 자동 검증

- Ollama HTTP 왕복: 요청 스키마·입력과 판정·근거·토큰 반환
- 미완료·길이 제한·토큰 누락·깨진 JSON 응답: invalid-result 차단
- local 어댑터 선택: stub 중복 없이 Ollama 하나만 등록
- `test,ollama` Spring profile: 실제 컨텍스트에서 Ollama 어댑터 선택
- OpenAI 회귀: 공통 prompt·schema·결과 파서 추출 후 관련 테스트 통과
- Java 전체 테스트: 477건 성공, 실패·오류·건너뜀 0건. `ollama-live` 태그 제외. Qwen 비교용 코드 제거 후 재실행

## 실제 모델 초기 측정

- 환경: MacBook Air M2, 통합 메모리 16GB, Ollama 0.34.4, `gpt-oss:20b` ID `17052f91a42e`, 13GB, 컨텍스트 4096
- 짧은 구조화 출력 준비 요청: HTTP 200, JSON `{"ok": true}`, 최초 로드 포함 45.8초
- `medium` 기본 추론, development 4건: 완료 3건, 시간 37.8초·35.4초·57.2초. 네 번째 요청 120.2초 제한 초과
- 완료 3건: 기대 판정과 모두 일치, 제공 근거 ID 범위 준수. 실패 1건 포함 스모크 p95 120.2초
- 판정: 현재 Mac에서 출시 목표 20초 p95 미달. 4건 측정만으로 전체 품질 승인 불가
- 원본 보고서: 로컬 `build/reports/evaluation/ollama-medium-smoke.json`. 답변 원문 미포함
- `low` 추론, 동일 4건: 모두 완료. 판정 일치 3/4, 근거 ID 범위 준수 4/4. 73.2초·22.4초·73.1초·93.7초, p95 93.7초
- `low` 요약 지표: detailed agreement 0.75, binary agreement 0.667, false-correct 0, schema success 1.0. 원본 보고서: 로컬 `build/reports/evaluation/ollama-low-smoke.json`
- 3분 제한의 `medium` 재측정, 동일 4건: 모두 완료. 판정·근거 ID 범위 4/4 일치. 71.2초·40.8초·57.7초·122.7초, p95 122.7초
- `medium` 재측정 요약: detailed agreement 1.0, binary agreement 1.0, false-correct 0, schema success 1.0. 원본 보고서: 로컬 `build/reports/evaluation/ollama-medium-3m-smoke.json`
- 기본 추론 강도 `medium` 선택: 4건에서 판정 일치가 높았음. 작은 표본이므로 전체 품질 우위를 뜻하지 않음
- 두 추론 설정 모두 현 장비에서 20초 p95 목표 미달. `low`로 바꿔도 지연 목표 해결 불가

### Development 8건 추가 측정

- 설정: 같은 MacBook Air M2 16GB, Ollama 0.34.4, `gpt-oss:20b` ID `17052f91a42e`, `os-evaluator-v1-medium`, `reference-v1` 1.0.0
- 명령: `./gradlew ollamaLiveEvaluation -PollamaSplit=development -PollamaCaseLimit=8 -PollamaReport=build/reports/evaluation/ollama-development-8.json --console=plain`
- 실행: 8건 완료, provider 실패 0건, schema 성공 8/8, 제공 근거 ID 유효 8/8
- 판정: 상세 일치 7/8(87.5%), CORRECT·INCORRECT 이진 일치 4/5(80%), false-correct 0/2
- 시간: 사례별 45.5~62.6초, p95 62.6초. 20초 목표 미달
- 비용: 로컬 provider 토큰 단가 USD 0. 하드웨어·전력 비용 미포함
- 원본: 로컬 `build/reports/evaluation/ollama-development-8.json`. 답변 원문 미포함
- 한계: 첫 8건의 작은 표본. 전체 development 138건과 evaluation-candidate 42건의 품질·지연 인증 아님. 실제 HTTP 사용자 흐름과 부하 미포함

### 로컬 비교 검토 기록

| 후보 | Ollama 파일 크기 | 비교 이유 | 확인할 경계 |
|---|---:|---|---|
| [`qwen3.5:9b`](https://ollama.com/library/qwen3.5:9b) | 6.6GB | 16GB 장비에서 비교한 다국어 후보. [모델 카드](https://huggingface.co/Qwen/Qwen3.5-9B)는 201개 언어·방언 지원과 Apache 2.0 표시 | 현재 후보에서 제외, 설치 모델 삭제. 아래 8건에서 품질 미달, 추론 모드 4건 timeout |
| [`gemma4:12b`](https://ollama.com/library/gemma4:12b) | 7.6GB | 다른 계열의 비교 후보. [Google 모델 카드](https://ai.google.dev/gemma/docs/core/model_card_4)는 다국어 지원과 Apache 2.0 표시 | 현재 Ollama 버전의 실행 호환성·한국어 평가 성능 미측정 |

모델 파일 크기는 실행 중 메모리·KV cache·지연을 보장하지 않는다. [Ollama thinking 문서](https://docs.ollama.com/capabilities/thinking)의 `/api/show` 지원값을 후보별로 확인해야 한다. 현재 어댑터는 `gpt-oss:20b`의 level 문자열을 보낸다. Qwen 비교용 boolean 처리·테스트·실행 설정은 제거했다. Gemma는 다운로드·실행하지 않았다.

### Qwen3.5 9B 로컬 비교 이력 — 폐기

- 설정: 동일 Mac·Ollama 0.34.4, `qwen3.5:9b` ID `6488c96fa5fa`, Q4_K_M, `reference-v1` development의 같은 첫 8건, `think=false`
- 명령: `./gradlew ollamaLiveEvaluation -PollamaModel=qwen3.5:9b -PollamaReasoningEffort=false -PollamaSplit=development -PollamaCaseLimit=8 -PollamaReport=build/reports/evaluation/qwen35-9b-development-8-no-think.json --console=plain`
- 완료 8/8, schema·근거 ID 오류 0건. 상세 판정 일치 1/8(12.5%), 이진 일치 1/5(20%), false-correct 0/2
- 시간 33.7~58.9초, p95 58.9초. `gpt-oss:20b medium` 8건 p95 62.6초와 차이 3.7초로 20초 목표 미달
- `think=true` 동일 첫 4건: 모두 180초 HTTP 제한 초과, 완료 0/4. 명령은 위의 `false`, 8을 `true`, 4로 바꾸어 실행. 원본: `build/reports/evaluation/qwen35-9b-development-4-think.json`
- 결과 해석: 공개 범용 벤치마크 우위가 이 한국어 CS 평가와 현재 prompt·양자화·하드웨어의 우위로 이어지지 않음. 이 측정에서는 `gpt-oss:20b medium`이 판정 품질에서 우세. 전체 세트의 모델 우열 또는 호스팅 성능은 미확정
- 정리: `ollama rm qwen3.5:9b`로 설치 모델 삭제. 재실행 시 모델 재설치와 비교용 boolean `think` 지원 코드가 필요. 이 문단과 로컬 JSON 보고서는 당시 판단 근거로 보존

### 공개 벤치마크와 필요한 속도 계산

[Qwen3.5-9B 모델 카드](https://huggingface.co/Qwen/Qwen3.5-9B#benchmark-results)가 한 표에서 비교한 점수. 단위는 점수 또는 정답률의 백분점이며, `Δ`는 각 Qwen 점수에서 표의 `GPT-OSS-20B` 점수를 뺀 값이다. 이 표의 gpt-oss 추론 강도와 로컬 양자화 조건은 현재 측정과 일치한다고 확인되지 않았다.

| 공개 지표 | GPT-OSS-20B | Qwen3.5-9B | Δ | Qwen3.5-4B | Δ |
|---|---:|---:|---:|---:|---:|
| MMLU-Pro | 74.8 | 82.5 | +7.7 | 79.1 | +4.3 |
| GPQA Diamond | 71.5 | 81.7 | +10.2 | 76.2 | +4.7 |
| IFEval | 88.2 | 91.5 | +3.3 | 89.8 | +1.6 |
| IFBench | 65.1 | 64.5 | -0.6 | 59.2 | -5.9 |
| MMMLU | 69.7 | 81.2 | +11.5 | 76.1 | +6.4 |
| MMLU-ProX | 67.3 | 76.3 | +9.0 | 71.5 | +4.2 |
| LiveCodeBench v6 | 74.6 | 65.6 | -9.0 | 55.8 | -18.8 |

[Google Gemma 4 모델 카드](https://ai.google.dev/gemma/docs/core/model_card_4#benchmark-results)는 `gemma4:12b`에 해당하는 12B Unified의 MMLU-Pro 77.2, GPQA Diamond 78.8, MMMLU 83.4, LiveCodeBench v6 72.0을 보고한다. Qwen 표와 평가 실행 조건이 다르므로 위 표와 직접 뺄셈하거나 합산하지 않는다. [Artificial Analysis 비교](https://artificialanalysis.ai/models/comparisons/qwen3-5-9b-vs-gpt-oss-20b)의 Intelligence Index 14 대 9도 Qwen 값에 추정 표시가 있어 보조 신호로만 본다. 그 사이트의 응답 속도는 호스팅 provider 측정이며 이 Mac의 Ollama 속도가 아니다.

로컬 8건의 입력 합계 4,552토큰(평균 569), 출력 합계 5,228토큰(평균 653.5), 전체 호출 시간 421.09초(평균 52.64초). 출력 토큰/전체 시간은 약 12.42토큰/초다. 현재 p95 62.59초에서 20초를 달성하려면 이 작업의 총 지연이 약 3.13배 줄어야 한다. 입력만 30% 줄이는 가상의 경우 총 토큰 수는 약 14% 감소한다. 입력·출력 토큰의 처리 비용이 같고 번역 시간이 0이라는 단순 가정에서도 평균 호출 시간은 약 52.64초에서 45.29초로 줄어드는 수준이다. 이는 실측 예측값이 아니다. 영어 번역 호출은 지연과 의미 손실을 추가하므로 같은 8건에서 원문 경로와 번역 경로의 총 시간·판정·false-correct를 짝지어 비교해야 한다.

## 남은 검증

- `reference-v1` development와 evaluation-candidate 분리 실측
- 판정 일치율, false-correct, Evidence, schema 오류, 호출 지연·p95 기록
- 로컬 서버 동시 요청·메모리 압박과 실제 HTTP 사용자 흐름 확인
- 기존 OpenAI 후보와 같은 입력으로 비교 후 출시 모델 결정

[작업 목록](../../planning/tasks.md#phase-5-실제-모델-품질-gate)의 품질 Gate 체크는 위 실측·판정이 끝날 때까지 유지.

## 외부 계약

- [OpenAI의 Ollama 실행 가이드](https://developers.openai.com/cookbook/articles/gpt-oss/run-locally-ollama): 메모리 권장량·모델 설치
- [Ollama 채팅 API](https://docs.ollama.com/api/chat): 요청·응답 필드
- [Ollama 구조화 출력](https://docs.ollama.com/capabilities/structured-outputs): `format`의 JSON schema
