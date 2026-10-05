# Security Cloud 발견 항목 개선

- 독자·질문: CrackCS 작업자 / 스캔의 5건 중 어떤 동작을 수정했고 무엇으로 확인했는가?
- 스캔: `wfr_57216964238acfde443f6e2b9de55b52de4cd1bdeae81ff83363067f881636ce`
- 기준 커밋: `747012f0f67240780c00955e4c155417cdbd6fc4`
- 작업 경로: `.worktrees/security-hardening`, 원본 checkout의 사용자 변경 보존

## 변경 단위와 상태

| 대상 | 상태 | 검증 목표 |
|---|---|---|
| 기존 관리자 세션의 상태 회수 우회 (high) | 구현·로컬 검증 완료 | 차단·탈퇴·재활성화 뒤 옛 세션 거부, 자기 재활성화 차단 |
| 로그인 제한 우회·실패 기록 증가 (medium) | 조사 | 주소·이메일 교체로 제한 우회 불가, 인증 전에 요청 수 제한 |
| 공개 회원가입 남용 (medium) | 조사 | source·전체 요청 제한 뒤 hashing·DB 생성 |
| 회원가입 이메일 존재 노출 (low) | 조사 | 신규·중복 요청의 같은 공개 응답 |
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
