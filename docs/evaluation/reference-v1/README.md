# 평가 정답 기준 v1

## 현재 상태

- 기준 버전: `1.0.0`
- 상태: `FINALIZED`
- 검수: 프로젝트 소유자 독립 사람 검수 완료
- 범위: 질문 60개, 판정 사례 240개, 지식 문서 60개, 근거 청크 120개
- 용도: 오프라인 품질 회귀와 retrieval 비교
- 제외: 운영 공개 승인, 실제 모델 품질 인증, 운영 부하 검증

`manifest.json`이 이 버전의 파일 해시와 문항별 검토 결과를 고정한다. 원본을 수정하면 기존 버전의 확정 상태가 자동으로 유지되지 않는다.

## 구성과 소유권

```text
reference-v1/
├── README.md
├── manifest.json
├── data/
│   ├── questions.jsonl
│   ├── golden-set.jsonl
│   ├── knowledge-documents.jsonl
│   ├── sources.json
│   └── experiment-splits.json
├── benchmarks/
│   ├── retrieval-baseline.json
│   └── retrieval-improved.json
└── tools/
    ├── bundle.py
    └── test_bundle.py
```

| 대상 | 책임 |
|---|---|
| [manifest.json](manifest.json) | 버전, 확정 상태, 원본 해시, 문항별 검토 기록 |
| [data/questions.jsonl](data/questions.jsonl) | 질문, 모범 답안, 필수 평가 기준 |
| [data/golden-set.jsonl](data/golden-set.jsonl) | 답변 사례와 기대 판정 |
| [data/knowledge-documents.jsonl](data/knowledge-documents.jsonl) | retrieval 평가용 문서와 근거 범위 |
| [data/sources.json](data/sources.json) | 참고 출처와 확인 정보 |
| [data/experiment-splits.json](data/experiment-splits.json) | development와 evaluation-candidate 분할 |
| [benchmarks/retrieval-baseline.json](benchmarks/retrieval-baseline.json) | 개선 전 역사적 측정값 |
| [benchmarks/retrieval-improved.json](benchmarks/retrieval-improved.json) | 현재 구현의 H2·PostgreSQL 측정값과 한계 |
| [tools/bundle.py](tools/bundle.py) | 무결성 검사, 파생본 생성, 오프라인 export |
| [tools/test_bundle.py](tools/test_bundle.py) | 데이터와 도구 계약 회귀 테스트 |

정답 상태는 `data/`와 `manifest.json`이 함께 소유한다. README나 생성된 검수본은 정답 원본이 아니다.

## 무결성 검사

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s docs/evaluation/reference-v1/tools -p 'test_*.py'
python3 docs/evaluation/reference-v1/tools/bundle.py
```

검사 항목:

- 원본 개수와 분야 분포
- ID, 질문, URL 중복
- 필수 필드와 enum
- 개념 가중치와 근거 연결
- 네 가지 판정 사례
- 실험 분할 중복과 누락
- 사람 검수 메타데이터
- manifest 해시와 문항별 검토 범위
- README의 로컬 링크

외부 네트워크 요청은 수행하지 않는다.

## 파생 검수본 생성

```bash
python3 docs/evaluation/reference-v1/tools/bundle.py --render
```

출력:

```text
build/reports/evaluation/reference-v1/questions.md
```

검수본은 원본 JSONL에서 결정적으로 다시 만들 수 있으므로 저장소에서 추적하지 않는다.

## 오프라인 export

```bash
python3 docs/evaluation/reference-v1/tools/bundle.py --export inputs --split development
python3 docs/evaluation/reference-v1/tools/bundle.py --export labels --split development
python3 docs/evaluation/reference-v1/tools/bundle.py --export inputs --split evaluation-candidate
python3 docs/evaluation/reference-v1/tools/bundle.py --export labels --split evaluation-candidate
python3 docs/evaluation/reference-v1/tools/bundle.py --export corpus
python3 docs/evaluation/reference-v1/tools/bundle.py --export retrieval --split evaluation-candidate
```

`inputs`와 `labels`는 분리한다. 모델 입력에 기대 판정, 사례 유형, 판정 이유를 포함하지 않는다. `corpus`는 전체 기준 문서를 사용한다.

## Retrieval benchmark

```bash
./gradlew retrievalBenchmark --console=plain
```

현재 스냅샷은 실제 production chunk 저장과 retrieval 경로를 사용한다. `INSUFFICIENT_EVIDENCE` 사례는 생성 단계 통제 사례이므로 검색 정답 180건에서 제외한다.

결과 해석 시 다음 한계를 함께 본다.

- 기준 문서 60개가 현재 chunk 정책에서 60개 청크로 저장됨
- 답변 사례 180개가 질문·참조 답안 기준으로는 60개 검색 입력을 공유함
- 참조 답안과 근거 요약의 어휘 중복이 큼
- 공개 evaluation-candidate도 반복 측정되어 비공개 holdout이 아님
- 현재 측정은 외부 모델 호출이나 운영 부하를 포함하지 않음

## 변경·재검수 조건

다음 변경은 같은 버전의 manifest 해시만 갱신하지 않는다.

- 질문, 모범 답안, 필수 평가 기준 수정
- 기대 판정이나 판정 이유 수정
- 근거 문서와 출처 수정
- 분할 정책 변경
- 문항 추가·삭제

변경 순서:

```text
원본 수정
   ↓
구조·연결 검사
   ↓
문항별 사람 검수
   ↓
새 버전과 manifest 확정
   ↓
retrieval 및 평가 회귀 실행
```

도구 구현이나 문서 설명만 바뀌고 원본 5종의 바이트가 유지되면 기존 정답 해시는 유지할 수 있다.
