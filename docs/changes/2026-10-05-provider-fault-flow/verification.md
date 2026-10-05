# 통제 provider 장애·복구 검증

- 독자·질문: 프로젝트 소유자·운영자 / timeout·429·5xx에서 원문·실패 화면·재시도·학습 상태가 보존되는가?
- 상태: 현재 cmux의 9개 시나리오·116개 assertion 완료. 실제 OpenAI 외부 장애 발생 검증과 구분
- 기준: main `43e4649` / `test/provider-fault-flow`, worktree `.worktrees/learner-review`
- 선행: [OS 검수 자료·사용자 검수](../2026-10-05-learner-review/verification.md), 기존 승인 OS 문서 v2·OS-101
- 러너: [verify_flow.py](verify_flow.py), 외부 경계: [provider.py](provider.py), [결과](runtime-results.json)

## 상태와 책임

```text
실제 화면 제출 → Answer 저장 + Evaluation 접수
                                      ↓
                           실제 JDK HTTP adapter
                                      ↓
               로컬 주입기: timeout / 429 / 503
                    ↓                         ↓
           3회 이내 합성 복구          3회 상한 소진 → FAILED
                    ↓                         ↓
    완료 transaction·근거·지식 1회       원문 유지·점수 없음·지식 반영 없음
                    ↓                         ↓
    같은 접수 키 → 같은 답변          새 UI 제출 → 새 답변·옛 실패 보존
```

- Answer: 원문·접수 키 소유. provider 요청 전에 저장
- Evaluation: lease·attempt count·재시도 시각·최종 상태 소유, 기존 상한3 유지
- JDK adapter: 실제 HTTP status·timeout을 기존 provider 오류로 변환
- 완료 transaction·KnowledgeApplication: 성공 결과만 개념당 1회 적용, 중복 unique 제약 유지
- 주입기: 외부 경계만 통제. 도메인·Service·DB 규칙 대체 없음

## 실제 실행

- 호출: 환경 workspace UUID와 identify `workspace:2 / surface:2` 일치. 다른 workspace 포커스 사용 없음
- pane:10 재사용. 최종 surface:25 서버·러너·로그, surface:26 실제 클릭·입력·새로고침, surface:27 최종 공용 검증
- 실행 중 닫힌 이전 보조 surface는 재조회 후 재사용 금지. 새 split 생성 없이 같은 pane에 필요한 탭 생성
- Java21·Node24·PostgreSQL17.11, loopback frontend5174·backend18080/18081·provider18082
- 전용 새 회원ID4. 기본 Question2 / OS-101 / Concept2·3, 문서 v2. 기존 회원 답변에 쓰기 없음
- 명령: 로컬 실행 안내의 `local,local-postgres` profile, `OpenAiResponsesClient` endpoint를 로컬 주입기로 지정
- 설정: timeout2s·retry-base-delay2s. 주입기는4s 지연으로 실제 JDK timeout 발생. worker의 상한3·저장 계약 변경 없음
- 모델 기록: `controlled-openai-contract / controlled-fault-flow-v1`. 실제 GPT와 구분
- 후속 생성: 기존 무료 stub 사용. 이번 검증의 대상은 답변 평가 장애, 후속 모델 품질 미포함

```bash
# 기존 OS PostgreSQL·앱·Vite를 loopback으로 시작, local provider 18082 사용
python3 -u docs/changes/2026-10-05-provider-fault-flow/provider.py
python3 -u docs/changes/2026-10-05-provider-fault-flow/verify_flow.py \
  --workspace workspace:2 --surface surface:26 --email NEW_LOCAL_ONLY_EMAIL
bash scripts/verify.sh all
```

- 러너는 실제 UI 제출9개. API는 결과 재조회·같은 접수 키 반복 검증, DB는 횟수·모델·적용 행 확인
- 브라우저의 네트워크 mock 미지원에 의존하지 않음. 서버 endpoint에서 실제 transport 장애 주입
- 주입기 요청21회: timeout5·4295·5035·합성 정상6. 실제 외부 OpenAI 호출0, 실제 provider 장애0

## 결과

| 시나리오 | 결과 |
|---|---|
| timeout·429·503 각 2회 뒤 복구 | 각 attempt3·EVALUATED, v2 근거, 필수 개념당 지식 적용1회 |
| timeout·429·503 각 3회 소진 | 각 FAILED·점수/판정null·원문 유지·실패 안내·근거0·지식 변경0 |
| 실패 뒤 새 답변으로 재시도 | 각 새 Answer·attempt1·EVALUATED, 옛 FAILED 유지 |
| 9개 접수 키 반복 | 원래 Answer·Evaluation 반환, 추가 provider 호출·지식 반영0 |
| 새로고침 | 원문·최종 상태·실패 안내 복원 |
| DB 재조회 | Answer9·Evaluation9·EvaluationConcept12·KnowledgeApplication12 |
| 러너 | assertion116/116 PASS, exit0 |
| 전체 공용 검증 | Python57 + backend518 + frontend338 + PostgreSQL65 = 978개 성공, 실패·오류·skip0, exit0. 타입·빌드·스택19항목 PASS |

- 실패 상태 `PROVIDER_TIMEOUT` 또는 `PROVIDER_ERROR`; 오답 판정으로 변환 없음
- 성공6·실패3. Concept2·3 각 유효6회, 성공 판정의 반복 반영 없음
- 화면: [상한 실패](assets/timeout-exhausted.png), [429 뒤 복구](assets/429-recover.png), [최종 지식 지도](assets/knowledge-final.png)
- 로그: `.firecrawl/provider-fault-flow/{flow,provider,backend,frontend,verify-all}.log`, `flow.exit`, `verify-all.exit`
- 준비 수정: API가 model·attemptCount를 제공하지 않아 DB 보조 조회 사용. KnowledgeApplication은 EvaluationConcept FK로 회원 범위 지정. 운영 API 변경 없음
- Vite 임시 설정의 선택적 esbuild unresolved 경고는 시작 로그에 보존. 앱 실행·클릭·최종 type/build 결과와 구분

## 한계·다음

- 정상 복구 응답은 합성. 실제 GPT의 채점 정확도·OpenAI의 Retry-After·외부 장애 빈도·운영 SLO 증거 아님
- `AC-003` 통제 장애 경로 완료. 실제 외부 장애 발생 항목은 미완료 유지
- 다음 Topic: 기존 Java21 초안5문항·10개 Concept·문서1개를 사용자 검수·관리자 공개·답변·근거·지식 상태로 연결
- 이 작업에서 생성한 프로세스만 종료, DB volume·평가 원문·현재 cmux 결과 pane 보존
