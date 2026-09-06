# AI 모델 선택 검토 기록

- 상태: 재검증 필요
- 원본 조사일: 미기재. 2026-09-06 정책 문서에서 분리
- 용도: 모델 선택 당시 후보·비용·트레이드오프 보존
- 갱신 계기: AI 연동 구현 또는 모델 교체 전 공식 제공 여부·가격·자체 평가 확인
- 현재 운영 정책: [콘텐츠·AI 정책](../product/content-and-ai-policy.md)

### 3.2 OpenAI 모델 비교

OpenAI 공식 문서의 현재 GPT-5.6 계열 비교는 다음과 같다. 가격은 100만 토큰당 API 정가이며 변경될 수 있다.

| 모델 | 공식 포지션 | 입력/출력 가격 | 이 서비스에서의 트레이드오프 |
|---|---|---:|---|
| `gpt-5.6-sol` | 복잡한 추론·코딩용 플래그십 | $4 / $20 | 가장 어려운 답안과 기준 평가에 유리하지만 기본 평가에 쓰면 비용·지연이 커질 수 있음 |
| `gpt-5.6-terra` | 지능과 비용의 균형 | $2 / $12 | 정오 판정 품질과 운영비의 균형점으로 초기 기본 평가에 적합 |
| `gpt-5.6-luna` | 비용 민감·대량 처리 | $0.20 / $1.20 | 매우 저렴하지만 미묘한 오개념 판정은 자체 평가 세트로 검증한 뒤 사용해야 함 |

세 모델 모두 Responses API와 Structured Outputs를 지원한다. 출처: [OpenAI 모델 목록](https://developers.openai.com/api/docs/models), [OpenAI 모델 비교](https://developers.openai.com/api/docs/models/compare).

### 3.3 권장 모델 운영안

```text
초기 기본 평가
  gpt-5.6-terra + Structured Outputs
              │
              ├─ 판정 확신이 낮음 ──▶ NEEDS_REVIEW
              │
              └─ 운영 품질 검증용 ──▶ gpt-5.6-sol 결과와 표본 비교

비용 최적화 단계
  gpt-5.6-luna가 동일 평가 세트를 통과할 때만 일부 트래픽 전환
```

권장 시작점은 `gpt-5.6-terra`이다. `gpt-5.6-sol`은 운영 요청마다 재호출하는 자동 심판보다, 골든 평가 세트 작성과 애매한 사례의 내부 검토에 우선 사용한다. `gpt-5.6-luna` 전환 여부는 가격만으로 정하지 않고 실제 CrackCS 답변 세트의 판정 일치율로 결정한다.

모델명은 코드에 고정하지 않고 설정으로 관리하며, Evaluation에 `model_name`, `evaluator_version`을 저장한다. 제공자를 바꾸더라도 `AnswerEvaluationService`의 입력과 출력 계약은 유지한다.

### 3.4 Retrieval 임베딩 모델

OpenAI의 `text-embedding-3-small`은 검색·추천 등에 사용하는 소형 임베딩 모델이며 100만 입력 토큰당 $0.02이다. `text-embedding-3-large`는 $0.13이다. 출처: [OpenAI text-embedding-3-small](https://developers.openai.com/api/docs/models/text-embedding-3-small).

초기 문서량이 작고 Topic·Concept로 먼저 후보를 줄일 수 있으므로 `text-embedding-3-small`로 시작한다. 한국어 기술 용어 검색 누락률이 허용 기준을 넘을 때만 large 모델을 비교한다. 이는 가격이 아니라 실제 검색 정답 세트의 Recall@K로 결정한다.
