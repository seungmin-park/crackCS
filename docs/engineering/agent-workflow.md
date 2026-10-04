# 작업자 검증 경로

- 독자·질문: CrackCS 작업자·에이전트 / 버전이 바뀔 때 공식 문서 기준과 실제 동작을 어떻게 함께 확인할 것인가?
- 범위: 기술 스택 동기화·CI 검증·PR 전달 경로. 앱 전체 기능 지도·전체 E2E 인증은 아님

## 기능 지도: 의존성 변경 뒤 공식 문서 검토

| 항목 | 현재 경로 |
|---|---|
| 목적 | 새 Java/Spring/Vue/TS API를 사용할 때 다른 버전의 문서를 따라 코드를 바꾸지 않도록 버전 변화 감지 |
| 선행 조건 | Python 3, repo checkout. 앱 서버·계정·시드·Docker 불필요 |
| 입력 | `build.gradle`, Gradle wrapper, `front/.nvmrc`, `front/package.json`, `front/package-lock.json` |
| 시작점 | `python3 scripts/check_stack_docs.py` |
| 성공 관찰 | 19개 항목 PASS·종료 코드 0. 사용자 데이터 변경 없음 |
| 실패 관찰 | manifest·lockfile 버전 불일치, 목록 누락·중복, 미검증 문서 호스트, Spring Boot 관리 버전 검토 누락이면 FAIL·종료 코드 1 |
| 상태 소유자 | build/lockfile이 설치 버전 소유. `stack-docs.json`은 사람이 확인한 공식 문서 주소와 적용 범위 소유 |
| 의존 경계 | 앱 runtime은 문서 목록에 의존하지 않음. 문서 검사는 빌드 설정을 읽고 CI에서 변경을 차단 |
| 검증 명령 | `python3 -m unittest discover -s scripts -p 'test_check_stack_docs.py'` 및 `python3 scripts/check_stack_docs.py` |
| 증거 | [이번 실행](../changes/2026-09-29-agent-engineering/verification.md), [문서 기준](stack-docs.md) |

```text
build.gradle / package-lock.json → 문서 목록 → 검사 → CI
            실제 버전          확인 기록      불일치 거부
```

- Boot BOM 관리 버전의 정확한 resolved graph는 Gradle 의존성 조회의 책임. 로컬 목록은 4.1.1 BOM에서 확인한 값을 기록. Boot 플러그인 버전 변경 시 모든 managed 항목에 재검토 요구
- 문서 링크의 도메인·버전 표기를 기계적으로 확인. 문서 내용과 코드 사용법의 적합성은 해당 변경의 테스트·리뷰 책임
- 예외 처리: 공식 문서가 리다이렉트하거나 `current`가 새 버전으로 움직이면 적용 범위를 확인한 뒤 목록에 기록. 다른 도메인을 예외로 추가할 때는 `check_stack_docs.py`의 host 규칙과 출처를 함께 리뷰

검증 스킬: [verify-crackcs](../../.agents/skills/verify-crackcs/SKILL.md). 앱 사용자 흐름은 [기존 실행 안내](../changes/2026-09-27-release-readiness/local-runbook.md)와 현재 cmux E2E 규칙 사용.


## 기능 지도: 작은 변경의 PR 전달과 머지

| 항목 | 현재 경로 |
|---|---|
| 목적 | 테스트 누락·구조 위반·DB 계약 실패가 있는 변경의 main 진입 차단 |
| 사용자 시작점 | 구현 요청 → 목적 하나의 `codex/` 브랜치 |
| 로컬·CI 시작점 | `bash scripts/verify.sh [all 또는 job 이름]` |
| 성공 관찰 | 네 job 모두 성공 → `CrackCS verify` 성공 → 보호 조건 충족 시 squash 자동 머지 |
| 실패 관찰 | 필수 테스트 없음·0건·실패·오류·skip·타입·빌드·문서·DB 검사 실패 또는 job 취소·생략 → gate 실패 |
| 상태 소유자 | 코드·manifest가 요구 동작과 필수 테스트 목록 소유. GitHub main 보호 규칙이 머지 결정 소유 |
| 경계 | 검증 workflow token은 `contents: read`. PR 생성·머지는 인증된 에이전트가 별도 수행 |
| 실행 증거 | [도입 검증](../changes/2026-10-05-ci-delivery/verification.md), CI의 테스트 보고서 artifact |

```text
요청한 동작 → TDD·책임 검토 → 서명 커밋·push → PR 생성
                                              ↓
                문서 ─ Java·구조·JAR 빌드 ─ Vue·타입·빌드 ─ PostgreSQL
                                              ↓ 모두 성공
                                    CrackCS verify
                                              ↓ 보호 규칙·최신 main
                                   GitHub squash 자동 머지
```

### 검사 책임과 실패 조건

- `docs`: Python 검사기 전체 테스트 + 공식 스택 문서 버전·호스트 대조
- `backend`: Java 21 `clean build` + H2·HTTP 계약 테스트 + ArchUnit 8개 검사 + JUnit XML 검사
- `frontend`: `.nvmrc`의 Node 24 + `npm ci` + Vitest JSON·필수 파일 검사 + type-check + production build
- `postgres`: Docker의 실제 PostgreSQL 17에서 기존 `postgresTest` + 별도 XML·필수 클래스 검사. H2 결과로 대체 불가
- 필수 목록: [required-tests.json](required-tests.json). 테스트 클래스·파일 단위의 핵심 경로. 총 테스트 건수 고정·JaCoCo·coverage 최소 비율 없음
- 결과 파일: `build/test-results/test/`, `build/test-results/postgresTest/`, `front/test-results/vitest.json`
- CI: main 대상 PR·main push·수동 실행. 경로 필터·`continue-on-error` 없음. 최종 gate는 모든 job의 `success`만 허용
- 생성한 보고서는 성공·실패 모두 artifact로 7일 보관. 최종 실패 원인은 GitHub job 로그와 보고서 함께 확인

| 판정 | 소유 검증 |
|---|---|
| 입력·예외·상태 보존·저장·사용자 흐름의 정확성 | 기존 도메인·Service·API·프런트 테스트 |
| Controller→Repository/JPA, Domain→Service/HTTP/외부 adapter, Service→Controller | 컴파일된 운영 클래스의 ArchUnit 검사 |
| 엔티티 공개 setter, Service 계약이 concrete class인 경우 | ArchUnit 검사 |
| ServiceTest의 실제 Spring 실행, mock·테스트 transaction 금지 | 테스트 클래스 구조 검사 |
| 테스트 메서드 한글 설명·클래스 설명 금지 | 현재 컴파일된 테스트의 메타데이터 검사 |
| 규칙이 올바른 객체에 있는지, 이름·수행 내용·추출 필요성, TypeScript 책임 배치 | PR diff 리뷰·동작 테스트. 구조 통과만으로 의미의 적합성 판정 불가 |

- HTTP 요청의 `Pageable` 변환은 기존 Service 계약에 맞는 허용 의존성. `org.springframework.data` 전체를 저장소로 간주하지 않음
- 테스트 구조 규칙의 Service 통합 대상: `*ServiceTest`. 다른 이름의 유스케이스 테스트는 별도 리뷰·실행 경로의 책임
- 구조 검사 범위 밖: 모든 AGENTS 스타일 규칙, 의미 있는 테스트 누락 전체, private 메서드 로직, TDD 실행 순서
- 기본 CI에서 유료 OpenAI 호출·부하 benchmark 제외는 기존 Gradle 태그 계약. 모의 외부 시스템 결과로 실제 모델 품질·운영 성능 인증 불가
- 로컬 브라우저 E2E는 기존 cmux 표시 규칙 유지. 서버·브라우저 미실행을 E2E 통과로 보고하지 않음

### 에이전트의 자동 PR·머지 순서

1. 최신 main에서 목적 하나의 `codex/` 브랜치 또는 기존 해당 브랜치 재사용
2. 동작·실패 조건을 설명하고 TDD 적용. 이름→수행 내용→소유 클래스·추출 필요성 순서로 diff 리뷰
3. Java 21·Node 24·Docker 환경에서 `bash scripts/verify.sh all` 실행. cmux 필요 경로는 사용자 표시 정책 적용
4. 관련 변경만 서명 커밋하고 해당 브랜치 push. 인증·서명 실패는 설정을 끄지 않고 원인 확인
5. 열린 PR 재사용 또는 `gh pr create --base main --head <branch> --title <title> --body-file <file>`. 생성한 PR을 Codex 작업에 첨부
6. GitHub API에서 main 보호를 조회: PR 필수, Actions check `CrackCS verify`(app 15368), `strict: true`, 관리자 적용, 강제 push·삭제 금지. 저장소 native auto-merge·squash 활성 상태도 확인
7. `gh pr merge <number> --auto --squash --match-head-commit <reviewed-head-sha>`. `--admin` 우회 금지
8. 실패·충돌·base 변경이면 수정·main 반영 뒤 새 head 검증. 실제 PR 머지 SHA와 main CI 성공까지 확인

- 단독 개발 저장소: 필수 승인 인원 0. 본인 PR 승인 불가로 멈추지 않게 하면서 PR·필수 CI·최신 main 조건 유지
- main 보호는 저장소 설정, auto-merge 신청은 PR별 설정. 신청 전 현재 보호 상태 재조회
- 에이전트가 승인된 작업을 PR로 전달하는 절차. 별도 주기 실행·새 기능을 계속 만드는 무인 봇은 생성하지 않음
- 공개 범위·유료 요금제 변경 없이 설정. 공개 CrackCS에서 native auto-merge 사용
- 공식 근거: [자동 머지](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/automatically-merging-a-pull-request), [보호 브랜치](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches), [ArchUnit](https://www.archunit.org/userguide/html/000_Index.html)
