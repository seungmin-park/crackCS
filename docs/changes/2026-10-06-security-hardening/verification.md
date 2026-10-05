# Security Cloud 발견 항목 개선

- 독자·질문: CrackCS 작업자 / 스캔의 5건 중 어떤 동작을 수정했고 무엇으로 확인했는가?
- 스캔: `wfr_57216964238acfde443f6e2b9de55b52de4cd1bdeae81ff83363067f881636ce`
- 기준 커밋: `747012f0f67240780c00955e4c155417cdbd6fc4`
- 작업 경로: `.worktrees/security-hardening`, 원본 checkout의 사용자 변경 보존

## 변경 단위와 상태

| 대상 | 상태 | 검증 목표 |
|---|---|---|
| 기존 관리자 세션의 상태 회수 우회 (high) | 구현·로컬 검증 완료 | 차단·탈퇴·재활성화 뒤 옛 세션 거부, 자기 재활성화 차단 |
| 로그인 제한 우회·실패 기록 증가 (medium) | 구현·로컬 검증 완료 | 주소·이메일 교체로 제한 우회 불가, 인증 전에 요청 수 제한 |
| 공개 회원가입 남용 (medium) | 요청 제한 구현·로컬 검증 완료 | source·전체 요청 제한 뒤 hashing·DB 생성 |
| 회원가입 이메일 존재 노출 (low) | 공개 응답 통일·로컬 검증 완료 | 신규·중복 요청의 같은 공개 응답 |
| provider 응답 무제한 buffering (medium) | 조사 | Content-Length와 chunked 응답의 실제 byte 제한 |

## 세션 회수

- 상태 소유자: `Member`, 상태가 바뀔 때 `authenticationVersion` 증가
- 세션: 로그인 당시 회원 ID·역할·인증 버전 보관
- 요청 경계: `MemberSessionValidationFilter` → `MemberService` → DB 재조회
- 거부 조건: 비활성 회원, 삭제된 회원, 역할 불일치, 인증 버전 불일치
- 거부 결과: SecurityContext 제거·해당 세션 무효화·401
- 로그인 순서: 비밀번호 인증 → 최종 로그인 저장 → 세션 발급. 저장 실패 때 인증 상태 미발급
- 진행 중인 요청의 DB 조회 이후 상태 변경까지 원자적으로 봉쇄하는 transaction lock은 미구현
- `V003__member_authentication_version.sql`: 기존 DB에 명시적 추가 적용 필요. 새 Compose volume 자동 적용

```text
Member.changeStatus → DB 인증 버전 증가
                                  ↓ 비교
세션 버전 → 보안 필터 → 현재 회원 상태·버전 → 인가 → Controller
                ↓ 불일치
          세션 무효화 + 401
```

### TDD 증거

- 기존 전체 backend baseline: 518건, 종료 0
- `./gradlew test --tests '*AuthenticationFlowTest' --console=plain`: 19건 중 추가 회수 검증 10건 실패, 옛 세션의 200·자기 재활성화 통과 재현
- `./gradlew test --tests '*SessionControllerTest' --console=plain`: 1건 실패, 최종 저장 실패 뒤 세션에 SecurityContext 잔존 재현
- `./gradlew test --tests '*AuthenticationFlowTest.rejectsOldSessionAfterMemberReactivation' --console=plain`: 1건 실패, 차단→활성 뒤 옛 세션의 200 재현
- 수정 뒤 AuthenticationFlow·SessionController·Member·SecurityConfiguration: 종료 0
- 로그: `/private/tmp/crackcs-security-20261006/`, JUnit XML·HTML: `build/test-results/test/`, `build/reports/tests/test/`

### 전체·화면 검증

- `bash scripts/verify.sh all`: Python 57건·문서19항목, backend 530건, frontend 338건, PostgreSQL 65건 모두 통과·종료 0
- 첫 전체 실행: HTTP slice 3개 클래스에서 MemberService 의존성 누락으로 39건 실패. Controller 테스트에 Service mock·현재 회원 반환 계약 추가 후 전체 재검증 통과
- 최종 전체 로그: `/private/tmp/crackcs-security-20261006/session-all-final.log`
- 호출 cmux: workspace:2 / surface:2, 보조 pane:12 / surface:14
- 실제 cmux 브라우저 surface:15: 관리자 로그인 → 회원 화면 → 자신의 상태 BLOCKED 선택 → 목록 재조회에서 401 → 로그인 화면 복귀
- 브라우저 공개 API assertion: `/api/admin/members` 응답 401 확인
- DB 재조회: 관리자 BLOCKED, 인증 버전 1. 자기 차단 전 상태 변경 요청은 정상 허용, 다음 요청부터 회수
- 화면: `/private/tmp/crackcs-security-20261006/session-revoked.png`
- PostgreSQL 17 새 테스트 DB: V001→local seed→V002→V003 명시적 SQL 적용, schema_version 001·002·003 확인, 새 JAR의 `ddl-auto=validate` 기동·health UP
- runtime 로그: `/private/tmp/crackcs-security-20261006/runtime-start.log`, `backend.log`, `frontend.log`
- HTTP 통합·도메인 테스트와 실제 브라우저 관찰의 범위 구분. 브라우저 전용 자동 Playwright suite는 기존 저장소에 없음

### 전달 결과

- [PR #9](https://github.com/seungmin-park/crackCS/pull/9) MERGED
- main: `8d562f2d4fd8782115ba231bfc68296fadbdc295`
- [main CI](https://github.com/seungmin-park/crackCS/actions/runs/37356454398) success
- Actions `CrackCS verify` app 15368·strict main 보호·관리자 적용 확인 뒤 native auto-merge. 강제 push·보호 우회 없음

## 로그인·회원가입 요청 제한과 공개 응답

- 사용자 선택: 요청 제한·응답 숨김 우선, 현재 가입 흐름 유지. 이메일 인증·발송·미인증 계정 만료 미도입
- 현재 정책: [ADR-0003](../../adr/0003-authentication-security-baseline.md)
- 상태 소유자: AuthenticationRequestBucket의 창·요청 수, LoginAttempt의 계정 실패 창·차단
- 요청 예산: DB capacity guard → 전역 quota → source quota, 인증·회원 생성 전 예약
- 동시 생성 상한: guard 잠금 아래 bucket·실패 행 수 확인. 각각 기본 최대 5000행, 초과 시 429
- 계정 실패 key: 정규화 이메일만 사용. 주소 교체 시 실패 누적 유지
- 성공 시 계정 실패 제거, 주소·전체 요청 예산은 유지
- 만료: source는 2분 경과 뒤 bulk 삭제, 계정 실패는 200행씩 반복 삭제. 삭제된 managed 객체 재사용 없음
- V004: 요청 budget schema + 기존 임시 조합 key 실패 상태 초기화. 계정·회원 상태 보존
- 가입 HTTP: 신규·중복 모두 202와 같은 중립 메시지, Location·회원 정보·중복 코드 미노출
- 중복 검사 전 같은 hashing 경로 실행, 실제 처리 시간 분포·이메일 소유 여부 인증은 별도 경계
- 기본 quota 및 상한의 운영 적정성·trusted ingress·대규모 부하 측정은 미검증

### TDD와 실행

- `auth-red.log`: 8건 중 7건 실패. 주소·전체 제한 4건, 계정별 주소 우회 1건, 신규·중복 응답 2건 재현
- `auth-state-red.log`: 3건 실패. 용량 상한·201개 만료 기록 정리·중복 검사 전 hashing 재현
- `sign-up-view-red.log`: 9건 중 중립 안내 1건 실패
- 수정 뒤 관련 backend `auth-green.log`·동시성/TTL `auth-additional-green.log` 통과
- 첫 전체: backend 541건·frontend 338건·PostgreSQL 74건·Python 57건·문서19항목 통과
- 실제 신규·중복 응답 비교·원래 비밀번호 보존 통합 테스트 추가 후 최종 전체: backend 542건·frontend 338건·PostgreSQL 75건·Python 57건·문서19항목 통과, 종료 0
- 최종 로그: `/private/tmp/crackcs-security-20261006/auth-all-final.log`
- 새 중요 suite 3개: backend·postgres 필수 목록과 postgresTest 실행 목록에 추가

### 실제 cmux 화면·DB

- 같은 workspace:2 / runner surface:14 / 실제 브라우저 surface:15
- V004를 이번 테스트 전용 PostgreSQL 17 DB에 명시적 적용, 새 JAR validate 기동·health UP
- UI 검증 quota: 로그인 source 3/global 8, 가입 source 3/global 6. 기본 운영 값의 부하 인증 아님
- 신규 가입 → 중립 안내 → 동일 이메일·다른 비밀번호 가입 → 동일 안내 → 원래 비밀번호 로그인 성공
- 이메일 교체 로그인: 정상 로그인 1건 뒤 실패 2건 허용, 다음 실패는 요청 제한 안내. 주소 예산은 성공 뒤에도 유지
- 중복 가입 연속 3건 같은 중립 안내, 4번째 요청 제한 안내·입력 보존 assertion 통과
- DB 재조회: 관리자 1명·새 회원 1명 유지, 중복 회원 추가 없음. 실패 상태 2행·request bucket 5행
- 화면: `/private/tmp/crackcs-security-20261006/login-rate-limited.png`, `sign-up-rate-limited.png`
- 로그: `/private/tmp/crackcs-security-20261006/auth-runtime-start.log`, `backend.log`
