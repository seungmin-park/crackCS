# OS 답변 독립 검수 준비·대리 진단

- 독자·질문: 프로젝트 소유자·검수자 / 기존 답변으로 무엇을 검증했고, 독립 표본 확인에 무엇이 남았는가?
- 상태: 검수 도구·사용자 위임 대리 진단 완료. 독립 사람 검수·대표 표본·신규 GPT 품질 측정 미완료
- 기준: main `f9648ca`, `test/learner-review`; worktree `.worktrees/learner-review`
- 절차·소유권·형식: [독립 검수 안내](../../evaluation/independent-review/README.md)
- 승인 주체: 현재 사용자의 “너가 대신 진행”, “사용자 검수로 하셈” 지시. 작성자 Codex, `사용자 검수` 표시
- 직접 사람 판정·독립 검수 확인: 기록하지 않음. 대리 수행을 사람 작성으로 변환 금지

## 표본 조사와 실제 진단

- 기본 PostgreSQL: Answer 1~4, 회원 2명. 기본 문항 1개·후속 문항 2개, PROCESS_THREAD 1개 Concept
- OS 전용 PostgreSQL: 24개 모두 기존 [OS 공개 시연](../2026-10-05-os-content/verification.md)·[합성 판정 검증](../2026-10-05-os-verdict-flow/verification.md)의 ID와 일치
- H2: 8개 테이블, Answer 테이블 없음. 조회 오류를 “답변 0건 검증 성공”으로 처리하지 않음
- 읽기 전용 SQL: 원문·문항 버전·기준 답안·개념 추출. 평가·피드백·회원 이름·이메일 제외
- 원문: 로컬 `.firecrawl/learner-review/packet/`; Git·PR 업로드 제외
- [대리 진단](agent-diagnostic.json): 종합·개념 판정 4/4 일치. 기존 GPT 2/2, stub 2/2 별도 집계. 모두 CORRECT
- false-correct: 오답 분모 0, null. 오답 판별 통과·대표 품질 주장 없음
- 부족한 유형: 부정확한 용어·맞고 틀린 설명 혼합·명백한 오답. 새 수집과 독립 검수 필요

## 도구와 화면 검증

- 입력·사람 라벨 분리, 미검수·누락 이유·출처 미확인·기대 개념 누락·동결 후 변경·기존 모델 결과 재사용 거부
- 실패·응답 누락은 기대값 분모에 유지. NEEDS_REVIEW 별도 집계, 분모 0은 null
- local 폼: loopback 18084, Host·Origin·nonce 검사. 선택값·독립 검수 확인란 자동 입력 없음
- 호출 workspace: 환경 `CMUX_WORKSPACE_ID`와 identify의 `workspace:2 / surface:2` 일치. 시각적 다른 workspace 선택 없음
- 기존 보조 pane 없음: 호출 오른쪽 pane:10 생성, surface:17 서버·로그, surface:20 실제 원문 검수 폼, surface:21 검증 로그, surface:22 합성 폼 검증
- 브라우저 명령은 발견한 surface 명시. 설치 CLI의 browser 하위 `--workspace` 미지원 오류 1회, 동일 workspace에서 발견한 surface로 수정. 다른 workspace 이동 없음
- [폼 러너](verify_review_form.py): 실제 cmux navigate·fill·select·check·click, 합성 1건의 assertion 9개 PASS, exit0. 빈 폼·확인 누락 저장 차단, 입력 저장·해시 동결·완료 표시 확인
- 폼 합성 검증 결과: 사람 품질 검증이 아닌 도구 동작. `UI_TOOLING_TEST_ONLY` 검수자·별도 파일·providerCalls0 명시

## TDD·회귀

- 최초 RED: 준비 인터페이스만 존재해 생성·거부 동작 누락. failures4·연쇄 errors17; 연쇄 오류를 유효한 RED로 포함하지 않음
- 준비 구현 뒤: 준비4 GREEN, 동결 거부·비교 검사11 failures·미구현 반환 필드 errors6
- GREEN: 최초22개 통과. 폼 저장·escaping·기본값 RED4 failures → 추가4 GREEN, 총26개
- 최초 공용 검증: Python53 + backend518 + frontend338 + PostgreSQL65 = 974개 성공, skip·failure·error0, exit0
- 폼 추가 후 최종 공용 검증: Python57 + backend518 + frontend338 + PostgreSQL65 = 978개 성공, skip·failure·error0, exit0. 타입·빌드·스택19항목 PASS
- 주요 로그: `.firecrawl/learner-review/red.log`, `red-freeze.log`, `red-form.log`, `verify-all.log`, `ui-test/flow.log`·`flow.exit`·`results.json`

## 다음 순서·한계

```text
사용자 위임 진단·도구 준비 → 통제 timeout·429·503 → Java 대리 검수·로컬 공개
          ↓ 별도 유지
독립·대표 학습자 수집·사람 판정 → 신규 GPT 비교 → 제한 파일럿 판단
```

- 유료 신규 OpenAI 호출 없음. 기존 GPT 결과는 과거 관측으로만 사용
- 실제 외부 OpenAI 장애·파일럿·공개 운영 승인: 이번 진단으로 대체하지 않음
- 전달: 이번 도구·문서만 서명 커밋·PR·보호 Gate·auto-merge·main CI 확인. 사용자 루트 변경·로컬 회고 제외
