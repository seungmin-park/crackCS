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
- Java 전체 테스트: 477건 성공, 실패·오류·건너뜀 0건. `ollama-live` 태그 제외

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
