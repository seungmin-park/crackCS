# 근거 충돌 검사 수정

- 독자·질문: 평가·검색 작업자 / 서로 다른 설명을 왜 충돌로 오인했고 어떤 반대 주장을 계속 감지하는가?
- 상태: 구현·검색 관련 25개·전체 952개 테스트 성공. 현재 Codex 브라우저에서 실제 GPT 후속 답변 성공
- 범위: KnowledgeEvidenceSelector의 충돌 비교. 검색 점수·한도·문서 공개·평가 상태 전이 계약 유지

## 현상과 책임

- 실제 OS-101의 fork 단일 개념 후속 답변: 선택 Chunk 3·5, 모델 호출 전 EVIDENCE_CONFLICT. Answer 2 NEEDS_REVIEW, 토큰·모델 값 없음, 지식 상태 미반영
- 근거 3: fork·exec 설명. 근거 5: 상속한 파일 디스크립터 설명. 사실 관계의 충돌 없음
- 원인: 공통 단어 2개와 문단 전체 부정 표현 유무만 비교. 문단 안의 서로 다른 주장까지 하나의 긍정·부정으로 축약
- 반대 누락: 긍정 주장과 무관한 부정문이 섞인 문단을 부정 주장 문단과 비교하면 양쪽 모두 부정으로 분류
- 책임: KnowledgeEvidenceSelector가 같은 토큰 정책으로 검색 후보·선택·충돌 상태 계산. 이번 변경은 해당 파생 판정 내부에 배치; 별도 저장 상태·Service 책임 추가 없음

```text
공개 문단 → 관련 근거 선택 → 문장으로 분리
                                 ↓
             겹치는 주장 단어 + 부정 여부 비교
                                 ↓
           충돌: NEEDS_REVIEW / 충돌 없음: provider 평가
```

## 수정과 경계

- 비교 단위: 마침표·물음표·느낌표·줄바꿈으로 구분한 문장
- 충돌 후보: 부정 표현 아니다·않는다·없다 유무가 서로 다름
- 유사도: 위 부정 토큰을 제외한 짧은 문장의 단어 80% 이상 공유, 공유 단어 최소 2개
- 이유: 주제 단어 일부가 같다는 이유만으로 서로 다른 주장을 반대로 취급하지 않게 비교 범위 축소
- 남는 한계: 어휘·부정 표현 휴리스틱. 형태 변화·동의어·문맥적 부정·모든 의미적 모순 감지 보장 없음
- 이름·배치: 문장 비교 statementsConflict, 문장 분리 statements. 기존 selector의 토큰·선택 계약과 연결된 내부 함수; 독립 객체·추가 API 불필요

## 검증

- RED: 검색 입력을 실제 생성된 질문·모범 답안·fork 단일 개념으로 일치. 2개 테스트가 충돌 오탐 true·충돌 누락 false 때문에 실패, exit 1
- GREEN: KnowledgeEvidenceSelectorTest 7개 + KnowledgeRetrievalServiceTest 18개, 합계 25개 성공·실패 0·skip 0, exit 0
- 기존 짧은 반대 근거 보존·명시적 충돌·문단 무관 후보 제외 테스트 성공
- 공용 전체 검증: Java 21·Node 24·Docker, `bash scripts/verify.sh all` exit 0. Python 31·backend 518·frontend 338·PostgreSQL 65, 합계 952개. 실패·skip 0, 타입·production 빌드·스택 문서 19개 PASS
- 실제 GPT 수정 후: 동일 후속 질문에 새 Answer 3 제출. gpt-5.6-terra·os-evaluator-v3, EVALUATED·100점, 시도 1회, 입력 1,096·출력 222토큰. 실제 근거 Chunk 3·v1. 원래 Answer 2 NEEDS_REVIEW 보존, FOLLOW_UP_LIMIT 확인
- [실제 성공 화면](assets/follow-up-fixed.jpg). 모델 평가 입력은 에이전트의 검증용 답변; 사람 학습자 품질·파일럿 결과가 아님
- PostgreSQL 재측정: 기존 180개 reference-v1 사례, K=1·3·5 및 모든 범위의 요약이 기존 측정과 일치. K=5 recall 1.0·무관 문단 비율 0·빈 결과 0. 충돌 질의 0개이므로 의미적 충돌 전체 품질 증거로 확대 금지. [측정 요약](retrieval-postgres.json)
- 로컬 로그: ignored .firecrawl/os-content/retrieval-red.log, retrieval-green.log, verify-retrieval-fix.log
