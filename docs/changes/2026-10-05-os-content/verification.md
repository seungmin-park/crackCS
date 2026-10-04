# 운영체제 콘텐츠 검수와 버전 보존 검증

- 독자·질문: 프로젝트 소유자·콘텐츠 검수자 / OS-101~105를 공개해도 되는가, 공개 이후 학습과 문서 교체가 어떻게 동작하는가?
- 상태: 출처 재대조·수정·프로젝트 소유자 승인·OS 로컬 공개·실제 GPT 학습·AC-007·최종 로컬 전달 검증 완료
- 기준 브랜치: `docs/os-content-review`. 최초 검증 main `1869a8c`; 테마 수정 `32d917a`와 검색 근거 충돌 수정 `a42fe826` 반영. 사용자 지정 네이밍 적용
- 범위: 운영체제 5문항·필수 Concept 10개·근거 문서 1개. 다른 20문항의 승인 제외
- 표시 환경: 사용자의 이번 요청에 따라 cmux 대신 현재 Codex 세션
- 브랜치 규칙: 사용자 지시에 따라 작업자 접두어 대신 변경 종류와 목적. AGENTS와 공용 전달 안내 함께 수정

## 출처 대조와 수정

| 문항 | 확인·수정 | 원문 위치 |
|---|---|---|
| OS-101 | fork·exec 성공 전제 명시. 생성과 프로그램 교체 분리 | [OSTEP 5](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-api.pdf) §5.1·5.3; [xv6 rev5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf) §1.1 |
| OS-102 | 대상 자식의 종료 확인·정상 회수 전제. 시간 경과·오류 반환과 구분 | OSTEP §5.2; xv6 §1.1·9.4 |
| OS-103 | 일반 파일의 상속 디스크립터로 범위 한정. 오프셋 공유와 프로세스별 close 구분 | xv6 §1.2, 인쇄 쪽 13–15 |
| OS-104 | POSIX 전제. while·같은 mutex·wait의 원자적 해제/대기·재획득을 근거 문서에도 명시 | [OSTEP 30](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-cv.pdf) §30.1·30.2, 쪽 2–3·9–14; xv6 §9.1·9.2 |
| OS-105 | 응답 시간을 첫 CPU 실행까지로 정의. 비용·처리량과 구분. xv6의 RR 설명 위치 보완 | [OSTEP 7](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-sched.pdf) §7.6·7.7; xv6 §8.2·8.3·8.6 |

- 재대조일: 2026-10-05 (Asia/Seoul)
- 자료: 원저자 공개 PDF 4개. 로컬 추출본 `.firecrawl/os-content/sources/`, 원문·코드·그림 Git 업로드 제외
- source의 기존 `checkedAt`·`bodySha256`: 2026-09-27 수집 기록 유지. 이번 추출본과 동일한 바이트라는 주장 제외
- 구조: 문항별 필수 Concept 2개, 가중치 0.50씩. reference-v1 원본·모델 평가 분할 변경 없음
- [등록용 원본](../../content/initial-v1/bundle.json)과 [생성된 읽기 사본](../../content/initial-v1/review.md) 함께 갱신

## 출처 이용 방식

- 배포 대상: 새로 작성한 한국어 질문·설명·답안과 원문 URL. 원문 PDF·코드·그림 재배포 제외
- [OSTEP 저자 안내](https://pages.cs.wisc.edu/~remzi/OSTEP/): 무료 공개 교재이며, 교육 자료에서는 사본 제공 대신 원문 링크 권장
- [MIT xv6 안내](https://pdos.csail.mit.edu/6.1810/2025/xv6.html): 책·소스·라이선스의 대상 구분 필요. xv6 코드 라이선스를 책에 자동 적용하지 않음
- 이용 메모는 법률 검토 완료나 포괄적 이용 허락 증거가 아님

## 사람 검수 기록

- 검수자: 프로젝트 소유자 (현재 대화의 사용자)
- 검수 일시: 2026-10-05T03:38:45+09:00
- 승인한 정확한 원본 SHA-256: `cba7aae00b69c39fd4a4b6a0659acd50f121e5d889e5ae14cadc0b41c6295333`
- 승인 범위: OS-101~105, 운영체제 근거 문서, 10개 Concept, 위 출처·이용 메모
- 상태: APPROVED_FOR_LOCAL. 사용자 응답 “수정본을 검수했으며 로컬 공개 승인” 확인. 공개 서비스·다른 Topic 승인으로 확대 금지
- 공개 결과: 승인된 OS 묶음만 관리자 화면에서 검수·공개. 등록용 bundle의 DRAFT는 템플릿 상태이며, 현재 앱의 공개 상태와 승인 사실은 이 기록과 저장된 관리자 metadata가 소유

## 순차 실행·완료 조건

```text
출처 대조·수정 → 사람 승인 → DRAFT 등록
                              ↓
                  관리자 검수 → 공개 → 검색 문단 생성
                              ↓
          답변 → 실제 GPT → 근거·후속 평가·지식 지도
                              ↓
             문서 v2 공개 → 과거 v1 근거·새 v2 근거 확인
```

1. 콘텐츠: 원본 해시·승인자·일자 기록. 출처·필수 개념·답안의 의미 확인
2. 공개: 문제 5개·문서 1개 PUBLISHED, 필수 Concept 10개 근거 연결. 상태 전이는 기존 관리자 API/UI 사용
3. 학습: 실제 UI 제출, 모델/평가기 버전·Answer/Evaluation ID·Evidence·지식 상태의 저장 결과 확인
4. AC-007: v1 Chunk·과거 Evidence 보존, v2 공개 후 새 평가의 v2 참조 확인. 과거 콘텐츠 삭제·직접 수정 금지

## 실행·검증 결과

| 검사 | 결과 |
|---|---|
| 콘텐츠 구조 | `python3 scripts/content_bundle.py`: 25문항·5문서·50개 Concept, 모든 가중치·출처·문서 연결 PASS, exit 0 |
| 관리자 공개 | 현재 Codex 브라우저의 검수→공개 실행. API 재조회: 기본 문제 5개 PUBLISHED·reviewedAt, Concept 10개·문항별 가중치 합 1.00, 문서 검수 metadata·Chunk 생성 PASS |
| 공용 전체 검증 | 최종 OS 변경에서 Java 21·Node 24·Docker로 `bash scripts/verify.sh all` 재실행: Python 31 + backend 518 + frontend 338 + PostgreSQL 65 = 952개 성공, 실패·오류·skip 0, exit 0. 타입·빌드·스택 문서 19항목 PASS |
| 실제 GPT | 모델 `gpt-5.6-terra`, 평가 `os-evaluator-v3`: 성공 7건, 각 1회 시도·100점. 후속 생성 `follow-up-v1`: READY 6건, 각 1회 시도 |
| 지식 반영 | EvaluationConcept 13건·KnowledgeApplication 13건, 평가 횟수 합 13. 실패 평가 반영 0건 |
| AC-007 | 기존 v1 Chunk 7개 동일, 과거 성공 Answer 6개의 전체 API 응답 동일. 새 Answer 8은 v2 Chunk 10만 참조 |

- 실행 명령·로그: `.firecrawl/os-content/verify-all.log`(최초 938개), `verify-all-resumed.log`(테마 반영 950개), `verify-retrieval-fix.log`(검색 수정 952개), `verify-os-final.log`(최종 전달 검증)
- 구조 검사·공용 검증과 실제 사용자 흐름 구분. 브라우저 흐름은 현재 Codex IAB에서 실제 로그인·답변 제출·평가 조회·후속 답변·지식 지도·관리자 문서 버전 공개로 진행. 숨겨진 러너를 보이는 E2E로 보고하지 않음
- 최소 저장 증거: [runtime-results.json](runtime-results.json). 검증 전용 학습자의 ID·모델·토큰·문서 버전·지식 상태만 포함. API 키·로그인 정보 제외

## 실제 학습 결과

| Answer / Evaluation | 대상 | 결과 | 근거 Chunk / 버전 |
|---|---|---|---|
| 1 / 1 | OS-101 | 실제 GPT EVALUATED · 100 | 3 / v1 |
| 2 / 2 | OS-101 후속 첫 시도 | NEEDS_REVIEW · EVIDENCE_CONFLICT, 모델 호출 전 중단 | 없음 |
| 3 / 3 | 같은 후속, 검색 수정 후 새 제출 | 실제 GPT EVALUATED · 100 | 3 / v1 |
| 4 / 4 | OS-102 | 실제 GPT EVALUATED · 100 | 4 / v1 |
| 5 / 5 | OS-103 | 실제 GPT EVALUATED · 100 | 5 / v1 |
| 6 / 6 | OS-104 | 실제 GPT EVALUATED · 100 | 6 / v1 |
| 7 / 7 | OS-105 | 실제 GPT EVALUATED · 100 | 7 / v1 |
| 8 / 8 | OS-101, v2 공개 후 같은 답안 | 실제 GPT EVALUATED · 100 | 10 / v2 |

- 첫 후속 실패: fork/exec 설명과 파일 디스크립터 설명의 일부 공통 단어·부정 표현을 문단 단위로 비교해 모순으로 오판. 실패 기록 Answer 2 보존; 모델·토큰 null, 지식 반영 없음
- 수정: `KnowledgeEvidenceSelector`의 문장 단위 비교. 실제 선택 입력의 오판과 반대 주장 누락을 재현하는 RED 2건 → GREEN. [수정·검증 기록](../2026-10-05-evidence-conflict/verification.md), [PR #3](https://github.com/seungmin-park/crackCS/pull/3), main `a42fe826` [CI 성공](https://github.com/seungmin-park/crackCS/actions/runs/37229362244)
- 비교의 한계: 단어 겹침·부정 표현 기반 휴리스틱. 의미상 모순 전체 검출 보장 없음. PostgreSQL reference-v1 180개·K 1/3/5 결과는 이전과 동일; 해당 데이터의 충돌 질의 0개로 충돌 탐지 품질 증명 제외
- 후속 Question 7: fork Concept 2 하나를 대상으로 생성. 성공 Answer 3 뒤 `UNAVAILABLE / FOLLOW_UP_LIMIT` 확인. 후속의 후속 생성 제한 유지
- 정상 기본 답변에서 후속 Question 7~11(v1)과 12(v2) 생성. 이번 실제 후속 답변 검증 대상은 Question 7 한 개
- 저장된 모델 응답 13건: 평가 7 + 후속 생성 6, 입력 10,014·출력 3,562 tokens. API/DB 사용 기록이며 provider 청구서·실제 비용 확인 증거는 아님
- 검증 답안은 의도적으로 올바른 설명. 100점 7건으로 모델 판정 정확도·오답 구별·실제 사용자 학습 효과를 주장하지 않음. 생성 Question 7 reference의 비한국어 단어 1개도 품질 한계로 보존

### 지식 상태

| 대상 | 평가 횟수 | 숙련도 / 신뢰도 | 상태 |
|---|---:|---|---|
| fork Concept 2 | 3 | 100 / 75 | STABLE |
| exec Concept 3 | 2 | 100 / 50 | LEARNING |
| 나머지 승인 OS Concept 4~11 | 각 1 | 각 100 / 25 | LEARNING |

- 승인 Concept 10개 모두 실제 평가 결과 반영. 숙련도는 판정 결과, 신뢰도는 누적 평가량. 한 번 정답으로 안정 상태를 만들지 않는 기존 계산 확인
- UI의 미평가 1개는 이번 묶음에 포함하지 않은 기존 seed Concept 1. Topic 화면에는 미평가 1·학습 중 9·안정 1 표시
- 화면: [최종 지식 지도](assets/knowledge-final.jpg)

## AC-007: 교체와 보존의 책임

```text
관리자: 새 문서 v2 검수·공개
              ↓
v1 RETIRED + 기존 Chunk 유지       v2 PUBLISHED + 새 Chunk 생성
              ↓                               ↓
과거 EvaluationEvidence → v1 Chunk     새 retrieval → v2 Chunk
              ↓                               ↓
과거 평가 API·화면 동일                새 Answer 8 평가 → v2

후속 질문 조회 → FollowUpSourcePolicy → 현재 PUBLISHED 근거 여부
```

- 상태 소유자: 문서가 버전·공개 상태 소유; 검색은 현재 공개된 문단 선택; EvaluationEvidence는 평가 당시의 Chunk 연결 소유. 최신 문서로 과거 연결 재작성 없음
- v2 변경 범위: 제목과 첫 머리의 버전 표현만 정리. 사실·출처·승인·이용 메모 동일. 등록 템플릿 bundle은 승인된 SHA 그대로 유지
- 같은 version series: `de07dc0e-d69d-47d3-ac6c-6a326601f684`

| 문서 | 현재 상태 | Chunk | 본문 SHA-256 |
|---|---|---|---|
| ID 2 / v1 | RETIRED | ID 2~8, 7개 보존 | `484b2562829ab937a86ddfd0a15d5c18e6e587bde27e2abf9d4df964cdf99720` |
| ID 3 / v2 | PUBLISHED | ID 9~15, 7개 생성 | `fc6bbc2e26e2cd5c0a38a4f1d2f9060b8d77504b15ce862e3223cfceb1983ad8` |

- 과거 성공 Answer `[1,3,4,5,6,7]`: 교체 전후 전체 API 응답 비교 동일. Evidence의 문단 ID·본문·문서 버전·offset·기존 판정 보존
- v1 Chunk 배열: 최초 공개 재조회와 교체 후 재조회가 동일. v2 Chunk ID와 겹치지 않음
- 새 Answer 8: Chunk 10 / 문서 3 / v2 / offset 114–353. 과거 Answer 1: Chunk 3 / 문서 2 / v1 / offset 116–355. 머리 표현 길이 변화로 offset 차이 발생
- 화면: [v2 관리자 공개](assets/document-v2-published.jpg), [새 평가 v2](assets/new-evaluation-v2.jpg), [과거 평가 v1](assets/old-evaluation-v1.jpg)
- 현재 후속 이용 경계: v1 기반 Answer `[1,4,5,6,7]`의 생성 기록은 READY로 보존되지만, v1 폐기 후 API는 `UNAVAILABLE / CONTENT_UNAVAILABLE`. `FollowUpSourcePolicy`가 현재 공개된 근거만 허용하기 때문. 과거 평가 근거 보존과 후속 질문 이용 가능성은 별도 계약. 새 Answer 8의 v2 후속은 READY

## 로컬 공개·실행 경계

- 원본 OS 묶음만 기존 importer로 DRAFT 등록, 관리자 검수·공개. 기본 Question ID 2~6, 각 필수 Concept 2개·가중치 0.50. 이번 전용 DB의 나머지 20문항 미등록·미승인
- 최초 문서 v1 공개: 승인 대기 안내를 실제 승인으로 동기화한 metadata 변경. 본문 사실·답안 변경 없음. `.firecrawl/os-content/published-v1.json`; v2 공개 결과 `published-v2.json`
- 초기 seed 문제·문서·Concept ID 1: 승인된 OS 묶음 건수에서 제외
- 이번 백엔드 loopback 18080·관리 18081, PostgreSQL compose project `crackcs-os-validation` loopback 55432, 프런트엔드 5174. 다른 프로젝트의 8080 등 프로세스 유지
- 재개 시 중단된 전용 PostgreSQL을 기존 볼륨으로 복구. v1·OS 5문항 상태 보존, 평가 0회에서 실제 검증 시작. 모의 서버는 실제 답변 제출 전에 종료
- 키 입력창 오류: `Referrer-Policy: no-referrer`와 엄격한 Origin 검사 충돌, 브라우저 POST Origin `null`로 403. `same-origin`으로 수정 후 키 없는 동일 경로·출처 검사 성공. Host·Origin·nonce 검사 유지
- 키 연결: 사용자가 기존 키와 예산 확인을 loopback 18083 입력창에서 직접 입력. 키는 프로세스 환경·메모리만 사용, 채팅·파일·로그 저장 제외. 서버 교체 뒤 직접 재입력 완료
- 자동 승인 검토: 기존 프로세스 환경에서 키를 추출해 재시작하는 제안 거절. 실행·스크립트 생성 없음. 해당 방식 재시도 없이 사용자의 입력창 연결로 해결
- 로컬 비용 설정 $0.25: 앱의 추정 예산 설정. 외부 프로젝트 잔액 조회나 provider의 강제 지출 상한 검증으로 간주하지 않음
- 이번 검증: 실제 승인된 로컬 OS 경로·한 단계 후속·버전 보존. 파일럿, 외부 배포, 다른 Topic 공개, 전체 모델 품질 평가 범위 제외
