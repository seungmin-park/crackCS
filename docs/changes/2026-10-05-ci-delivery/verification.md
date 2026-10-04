# CI·자동 PR·머지 도입 검증

- 독자·질문: 작업자·리뷰어 / 검사기가 실제 위반을 막으며 보호 규칙 아래 PR이 머지되는가?
- 원격 증거: [PR #1](https://github.com/seungmin-park/crackCS/pull/1). checks·최종 머지 SHA·main CI는 PR 본문과 GitHub Actions에서 확인
- 현재 기준: [작업자 검증·전달 경로](../../engineering/agent-workflow.md)
- 상태: 구현·로컬 검증 완료, main 보호 적용. 원격 결과는 PR·GitHub Actions에서 확인
- 범위: CI·구조/필수 테스트 검사·PR 전달. 운영 기능 변경 없음
- 분리 작업 공간: `/Users/seungmin/.codex/worktrees/ci-auto-delivery/crackCS`
- 브랜치: `codex/ci-auto-delivery`, 기준 main `4cfb324f4936e0b359d13fa06b335b07675fd4ec`
- 관련 없는 원래 작업 공간의 문서 변경·retrospectives·tobyteam 제외

## RED → GREEN

- 보고서 검사 11개 테스트: 누락·다른 테스트로 대체·0건·실패·오류·skip·불완전 frontend 결과를 받아들여 assertion 실패. 구현 후 전체 Python 31건 통과
- ArchUnit 버전 변경: 문서 재검토 없이 성공하던 현상을 assertion 실패로 확인. 선택 버전·공식 호스트 검사 추가 후 통과
- 구조 검사: 실제 Controller→Repository, Domain→Repository, 엔티티 공개 setter를 잠시 주입. assertion 실패와 XML을 확인하고 원본 복원
- 초기에 `org.springframework.data` 전체를 Controller 금지 대상으로 잡아 기존 Pageable 변환 16건을 잘못 거부. 저장소 API·JPA로 범위를 좁혀 기존 HTTP 계약 유지
- 테스트 작성 중 제네릭 컴파일 오류 1건 수정. 기능 RED로 계산하지 않음
- 검사기 이름·역할: `check_test_reports`는 실행 증거 판정, `ArchitectureTest`는 실제 클래스 구조 검사, `verify.sh`는 기존 검사 조정. 도메인 의미·사용자 결과 판정은 기존 동작 테스트와 리뷰에 유지

## 실행 결과

| 명령·검사 | 실제 결과 |
|---|---|
| `bash scripts/verify.sh docs` | Python 31건 성공, 19개 스택 문서 일치, exit 0 |
| `bash scripts/verify.sh backend` | Java 516건·94개 suite, 실패 0·오류 0·skip 0, clean build·JAR 생성, exit 0 |
| `bash scripts/verify.sh frontend` | Vitest 326건·37개 파일, type-check·production build·필수 JSON 검사, exit 0 |
| `bash scripts/verify.sh postgres` | PostgreSQL 17 Testcontainers 65건, 실패 0·오류 0·skip 0, exit 0 |
| 실제 운영 코드 위반 주입 | 구조 검사 8건 중 정확히 3건 assertion 실패, exit 1. 원본 복원 후 전체 516건 성공 |
| 실제 AnswerFlowTest XML 제거 | 다른 테스트 508건이 남아도 필수 클래스 누락으로 exit 1. 복원 후 exit 0 |
| 실제 workflow의 gate 코드 실행 | 모든 job success면 exit 0. failure·cancelled·skipped는 각각 exit 1 |
| shell 문법·workflow YAML·문서 로컬 링크·diff | 통과 |

- 실행 환경: Java 21.0.7, 로컬 Node 24.21.0 명시 선택, Docker 29.8.0. 원격은 `.nvmrc`의 Node 24.20.0으로 별도 확인
- 필수 목록: Java 23개 클래스, 프런트 14개 파일, PostgreSQL 6개 클래스. 전체 건수 할당 없음
- 실제 RED·GREEN 로그: worktree `build/verification/`에 `crackcs-ci-red-reports.log`, `crackcs-ci-red-architecture.log`, `crackcs-ci-green-backend.log`, `crackcs-ci-green-frontend.log`, `crackcs-ci-green-postgres.log`
- 실제 assertion: Java `build/test-results/test/`, PostgreSQL `build/test-results/postgresTest/`, 프런트 `front/test-results/vitest.json`
- 해석: PostgreSQL 65건은 기본 Java 516건과 일부 중복. 합산한 고유 테스트 수로 표현하지 않음

## 실제 main 보호 설정

2026-10-05 GitHub API 적용·재조회 결과:

- 공개 저장소 유지, native auto-merge 활성, squash merge 사용 가능
- main 변경은 PR 필수, `CrackCS verify` 출처는 GitHub Actions 앱 15368
- `strict: true`, 최신 main 반영 후 check 성공 필요
- 관리자에도 적용, 강제 push·main 삭제 금지, 미해결 리뷰 대화 해결 필요
- 필수 승인 인원 0. 단독 개발에서 본인 승인 불가 때문에 대기하지 않으며 필수 CI·PR 조건 유지
- 검증 workflow는 read-only token. PR·auto-merge는 별도 인증한 에이전트에서 실행
- HTTPS OAuth에는 workflow scope가 없어 첫 push 거부. 기존 SSH 인증의 정확한 `ssh://` 주소를 해당 push에만 사용해 성공. 영구 remote·인증·서명 설정 변경 없음
- 개별 PR auto-merge 요청과 실제 MERGED·main CI 결과는 원격 PR 기록에서 확인. 보호 설정만으로 PR 머지 완료 판정 불가

## 미검증 경계와 환경

- cmux env의 workspace/surface 없음. `cmux identify --json` 직접 실행: 소켓 미발견, 샌드박스 밖 확인: “cmux 내부에서 시작된 프로세스만 연결 가능” 접근 거부
- 로컬 브라우저·서버 E2E 미실행. terminal 테스트·DB 검사와 보이는 E2E 구분
- 실제 OpenAI 호출·유료 평가·운영 데이터·배포·부하 benchmark 미실행
- npm 설치가 기존 lockfile에 high vulnerability 2건 보고. 이번 CI 작업에서 의존성 자동 업그레이드 미수행
- 구조 검사·필수 클래스 목록은 모든 의미적 결함·각 테스트 메서드의 삭제·TDD 순서를 증명하지 않음
