# 공개 OS 판정과 학습 상태 검증

- 독자·질문: 프로젝트 소유자 / 승인된 OS 5문항에서 정답·핵심 누락·모순을 구분하고 학습 상태까지 일관되게 반영하는가?
- 상태: 작업 목록 갱신·실행 전 사례 고정·실제 cmux/GPT 흐름·최종 로컬 공용 검증 완료. 원격 전달 상태는 해당 PR·CI 기록이 소유
- 기준 코드: main `b1b1b35c07e9a2e6dcf59137d61400e3be0a0959`, `test/os-verdict-flow`
- 범위: 기존 승인 OS 콘텐츠·문서 v2·새 로컬 검증 회원 ID 3. 기존 회원·과거 평가·승인 원본 보존
- 입력·결과: [15개 사례](cases.json), [저장·API·UI assertion 결과](runtime-results.json), [계획](plan.md)

## 기대값과 검증 범위

- 기본 입력: OS-101~105 각각 정답·부분 정답·오답. 원문·개념별 기대 판정·이유를 실행 전에 고정
- 실행 전 SHA-256: `caeb8f668fa6345e3abf617e12c8f076b7f92db1dc61db3e65c7f7d51ac85367`
- 라벨: `AGENT_AUTHORED_NOT_HUMAN_REVIEWED`. 사람 독립 검수 골든 세트·대표 학습자 표본·출시 인증과 구분
- 모델 입력: 화면의 답변만. 기대 판정·사례 유형·진단 이유 전달 없음
- 모델·평가기·프롬프트·reference-v1 변경 없음. 불일치를 없애기 위한 입력 변경·반복 선별 없음
- 부분 정답: 질문이 요구한 핵심 설명 누락. 추가 예시·전문 용어만 생략한 정답을 감점하지 않는 기존 정책 적용

## 실행 환경과 표시

- worktree: `/Users/seungmin/Desktop/repo/crackCS/.worktrees/os-verdict-flow`
- Java 21.0.7, Node 24.21.0, PostgreSQL 17.11. Node 선언 `.nvmrc` 24.20.0과 실행 patch 구분; 공용 검사 요구 major 24 충족
- 호출: 환경 변수·`cmux identify --json`으로 `workspace:2 / surface:2` 확인. 당시 시각적 포커스 workspace:5를 대상으로 사용하지 않음
- E2E pane:8: surface:8 서버 명령·로그, surface:14 러너·assertion, surface:10 실제 클릭·입력·제출·재조회
- 포트: frontend 5174 → backend 18080/18081 → 기존 OS PostgreSQL 55432. loopback 키 입력 18083
- 키: 사용자가 직접 입력·프로젝트 예산/잔액 확인. 프로세스 메모리만 사용, 추출·채팅·파일·Git 보관 없음
- 새 회원: cmux 실제 가입·로그인, OS 10개 Concept 초기 UNKNOWN/NULL/0회 확인. 보조 API 읽기는 같은 회원의 별도 세션

## 실제 결과

| 검사 | 결과 |
|---|---|
| 기본 판정 | 정답 5·부분 정답 5·오답 5, 전체 기대값 15/15 일치 |
| 이진 판정 | 정답·오답 10/10 일치, false-correct 0/5 |
| 개념별 진단 | 30/30 기대 판정 일치, 점수 100·50·0 확인 |
| 평가 저장 | Answer 9~24, GPT 16/16 EVALUATED, 각 시도 1회, `os-evaluator-v3` |
| 근거 | 기본 15건·후속 1건 모두 문서 v2의 Chunk 9~15만 참조 |
| 지식 반영 | EvaluationConcept·KnowledgeApplication 31건. 횟수·최신 가중 평균·신뢰도·상태 일치, 무관한 개념 변경 0 |
| 기본 후속 생성 | 15/15 READY, GPT·`follow-up-v1`, 각 1회 시도 |
| 보강 후속 | 부분 정답 Answer 10의 부족 exec(ID 3)만 대상으로 Question 14 생성. Answer 24 정답100 |
| 후속 제한 | Answer 24 API `UNAVAILABLE / FOLLOW_UP_LIMIT`. 추가 생성 행·모델 호출 없음, 다음 기본 문제 링크 실제 클릭 |
| 화면 대조 | 기본 15개 결과의 정확한 판정·점수 제목, 개념별 판정, 원문/평가 API 재조회 일치. 중복 상태 적용 없음 |
| 러너 | 기본 exit0: 상태 검사240·진단45. 후속/UI exit0: 상태20·진단2·후속/UI49. 기록된 검사356개 실패0 |
| 최종 공용 검증 | Java21·Node24·Docker에서 `bash scripts/verify.sh all`: Python31 + backend518 + frontend338 + PostgreSQL65 = 952개 성공, 실패·오류·skip0, exit0. 타입·빌드·스택19항목 PASS |

```text
Answer 원문 → 공개 v2 근거 → GPT 개념별 판정
                                 ↓
             Evidence + KnowledgeApplication → 정확히 한 번 상태 반영
                                 ↓
             지식 지도·추천 → 부족 exec 후속 → FOLLOW_UP_LIMIT
```

- OS-101-P: 전체50, fork100·exec50. 전체 점수를 각 개념에 복사하지 않음
- 최종 exec: 100 → 50 → 0 → 보강100. 숙련도 `(100+50+0+100+최신100)/5 = 50`, 신뢰도100·4회·LEARNING
- 나머지 OS 개념: 각 3회·신뢰도75·LEARNING. 개념별 실제 점수로 숙련도 계산
- 기존 seed Concept1 UNKNOWN 유지. 추천 API·화면은 미평가 우선 정책에 따라 seed Question1 선택. seed 제거·추천 정책 변경 없음
- 화면: [정답](assets/correct.png), [부분 정답](assets/partial.png), [오답](assets/incorrect.png), [보강](assets/follow-up.png), [지식 지도](assets/knowledge-map.png), [추천](assets/recommendation.png)

## 명령과 실행 문제

선행: 기존 실행 안내로 GPT 앱·승인 OS 공개 DB 연결. surface는 현재 호출 workspace에서 새로 확인; 회원은 새 로컬 전용 주소 사용.

```bash
python3 -u docs/changes/2026-10-05-os-verdict-flow/verify_flow.py \
  --workspace workspace:2 --surface surface:10 --email NEW_LOCAL_ONLY_EMAIL
python3 -u docs/changes/2026-10-05-os-verdict-flow/verify_follow_up.py \
  --surface surface:10 --email SAME_LOCAL_ONLY_EMAIL
bash scripts/verify.sh all
```

- 두 러너는 실제 UI 제출. 기존 `LocalAdminClient`는 API 읽기·보조 확인에 재사용, 답변 POST 대체 없음
- `--existing-member`: UI 가입이 완료됐으나 이동이 중단된 회원만 재개. OS 초기 UNKNOWN/NULL/0회 전제 확인
- 관리자 기본 문제 API는 NORMAL 필터. 후속 대상 FK는 DB로 보조 확인; 후속 답변은 실제 UI에서 제출
- 실패 assertion·판정 차이·환경 실패에서 종료코드를 0으로 바꾸지 않음
- 로컬 로그: `.firecrawl/os-verdict-flow/flow.log`, `follow-up-and-ui-audit.log`, `gpt-server.log`, `verify-all.log`; 개별 API·화면은 `results/`
- 첫 전체 검증: clean이 build 아래 러너/로그를 삭제. 검사 완료·파일 로그 보존 실패. 최종 실행은 Git 제외 `.firecrawl/` 사용
- 첫 가입(exit1): 이전 DOM 조건 조기 통과·화면 이동 대기 실패. 새 문서 complete·main frame 대기 추가. 이미 생성된 계정의 초기 상태 확인 후 로그인 재개; 모델 호출 전 실패 로그 보존
- 후속 준비(exit1): 관리자 API Question14 조회404. NORMAL 계약 확인 후 후속 대상 Concept FK만 읽도록 러너 수정
- 위 실패는 러너·환경 경계. 운영 코드 변경 없음

## 사용량과 한계

- 실제 모델 응답: 평가16 + 후속 생성15 = 31건. 입력24,294·출력9,714 tokens
- 앱 설정 단가(입력$2·출력$12/백만 tokens)의 추정 $0.165156. 실제 가격·provider 청구서·외부 강제 상한 증거 아님
- 내부$0.25는 평가 기록 기준의 기존 추정 예산. 후속 생성까지 강제로 제한하는 장치로 해석 금지
- 합성 답변: 누락을 명시하거나 핵심을 명백히 뒤집은 진단. 자연스러운 학습자 표현·혼합 오류·대표 품질·학습 효과 미검증
- 실제 provider 장애·다른 Topic 검수·전체 파일럿 미완료 유지

## 정리와 전달

- 이번 Vite·키 입력 도구·Java 앱·로그 감시·실행 셸 종료. 해당 PID와 포트18080·18081·18083·5174·55432 재조회에서 잔존 없음
- PostgreSQL 컨테이너 정지, 기존 volume·회원·평가·콘텐츠 보존. 다른 프로젝트 프로세스 종료 없음
- cmux pane·브라우저 최종 화면·로그 유지. 서버 종료 뒤 새로고침은 연결 실패가 정상
- 공용 전체 검증·원본 해시·증거 집계·러너 문법·변경 문서의 로컬 링크·diff 점검 완료
- 원격 전달: 해당 `test/os-verdict-flow` PR의 head·필수 check·머지 SHA와 main CI 기록으로 확인. 로컬 성공을 원격 머지 증거로 대체하지 않음
