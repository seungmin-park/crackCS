# 보안 감사 후속 수정

- 독자·질문: CrackCS 작업자 / 2026-11-04 소스 감사에서 나온 항목 중 무엇을 고쳤고 무엇으로 확인했는가?
- 감사: 저장소 밖 `security-audit-skill/crackCS/run-1`(로컬 보관). 확정 0, 확인 필요 9, 기각 1. 대상 코드 실행 불가 환경에서 수행한 소스 추적 결과
- 전달: 목적 하나당 PR 하나. 각 PR이 아래 자기 절을 갱신
- 결과: 5건 모두 main 반영, PR·main CI 성공. 운영 반영은 배포 VM 디스크 부족으로 Production CD 실패 중(저장소 밖 조치 필요)

## 변경 단위와 상태

| 대상 | 상태 | 검증 목표 |
|---|---|---|
| 로컬 서버의 LAN 노출·DNS 리바인딩 | 구현·main 반영([#24](https://github.com/seungmin-park/crackCS/pull/24), `86e725b`) | `local` 프로필 loopback 바인딩, 허용 밖 Host 400 |
| 로그인 뒤 CSRF 토큰 유지 | 구현·main 반영([#25](https://github.com/seungmin-park/crackCS/pull/25), `15a7f91`) | 로그인 전 토큰 거부, 로그인 뒤 새 토큰 사용 |
| 포크 PR CI 완료의 운영 배포 대기 취소 | 구현·main 반영([#26](https://github.com/seungmin-park/crackCS/pull/26), `e611a1a`) | 배포 job만 동시 실행 그룹 참여 |
| 예상 밖 500의 원인 로그 누락 | 구현·main 반영([#27](https://github.com/seungmin-park/crackCS/pull/27), `2a552b6`) | requestId와 예외를 서버 로그에 기록 |
| 세션에 직렬화되는 비밀번호 해시 | 구현·main 반영([#28](https://github.com/seungmin-park/crackCS/pull/28), `7f764c7`) | 인증 뒤 principal의 해시 제거 |

## 로컬 서버 Host·바인딩 보호

```text
브라우저(rebind.example → 127.0.0.1) ─→ :8080
                                        ↓ StrictHttpFirewall
                      Host ∉ {localhost, 127.0.0.1, [::1]} → 400
LAN 다른 기기 ─→ 192.168.x.x:8080 → 수신 안 함(127.0.0.1 바인딩)
```

- 현상: H2 단독 `local` 프로필은 모든 인터페이스에서 수신. 두 로컬 프로필 모두 Host 제한 없음. 공개 데모 관리자 계정·실제 OpenAI 키가 같은 프로세스에 존재
- 상태 소유자: `crackcs.security.allowed-hosts` 설정값. `SecurityConfiguration.httpFirewall`이 Spring Security 방화벽에 허용 이름으로 전달
- 동작: 목록이 비면 Host 제한 없음(운영 기본값, 외부 프록시 책임). 목록이 있으면 그 밖의 Host를 필터 체인 진입 전에 거부
- `application-local.yaml`: `server.address`·`management.server.address` 127.0.0.1, 허용 Host `localhost,127.0.0.1,[::1]`. `local-postgres`는 `local`과 조합되어 같은 값 상속
- Vite 개발 서버 proxy는 기본 설정상 브라우저 Host(`127.0.0.1:5173`)를 유지하므로 허용 범위 안
- 남은 경계: 브라우저 자체의 Local Network Access 차단 여부와 무관하게 서버에서 거부. 운영 Host 정책은 nginx 설정 소유

### TDD 증거

- RED(빈 껍데기 bean 상태): `./gradlew test --tests '*AllowedHost*' --tests '*LocalProfileNetworkBoundaryTest'` 7건 중 4건 실패
  - 허용 밖 Host 미거부(`Expecting code to raise a throwable`), HTTP 400 기대·200 실제, 로컬 프로필 주소·허용 목록 null
- GREEN: 같은 명령 7건 통과
- 백엔드 전체: 아래 결과 참조

### 실행 결과

- 기준: 브랜치 `fix/local-loopback-host-guard`, main `3dfc131` 기반
- `bash scripts/verify.sh backend`: clean build·ArchUnit 포함 564건 통과(기존 557 + 신규 7), 실패·오류·skip 0, 종료 0
- `bash scripts/verify.sh docs`: Python 61건 통과, 스택 문서 19항목 PASS, 종료 0
- 필수 suite 추가: `AllowedHostFlowTest`, `LocalProfileNetworkBoundaryTest`(`docs/engineering/required-tests.json`). `check_test_reports.py backend` PASS
- 프런트: 변경 없음. 로컬 Node 26이라 Node 24 gate는 CI 결과로 판정
- PostgreSQL: schema·세션 저장소 변경 없음. CI `postgres` job 결과로 판정
- 미검증: 실제 브라우저의 DNS 리바인딩 재현, 로컬 앱 기동 후 LAN 기기 접속 거부 관찰

## 로그인 시 CSRF 토큰 교체

```text
GET /api/auth/csrf → 세션 S + 토큰 T
POST /api/auth/login (T) → S→S' 교체 + T 폐기
POST (T)  → 403        GET /api/auth/csrf → T' → POST (T') → 처리
```

- 현상: 로그인은 session ID만 교체(`ChangeSessionIdAuthenticationStrategy`). 로그인 전 CSRF 토큰이 인증 세션에 그대로 남고 SPA도 재사용
- 위험 조건: 같은 사이트의 다른 출처나 평문 HTTP 주입으로 세션 쿠키를 심을 수 있으면, 공격자가 아는 토큰으로 본문 없는 관리자 POST 위조 가능. 배포 도메인 구성에 따라 성립 여부가 갈림
- 상태 소유자: 세션의 CSRF 토큰은 공유 `HttpSessionCsrfTokenRepository` bean. 필터 체인과 로그인 전략이 같은 저장소 사용
- 변경: 로그인 `SessionAuthenticationStrategy` = session ID 교체 + `CsrfAuthenticationStrategy` 조합. SPA `login()`은 성공 직후 캐시 토큰 제거
- 기존 테스트 2곳(로그아웃·관리자 자기 재활성화)은 로그인 전 토큰 재사용을 전제로 작성되어 로그인 뒤 토큰을 새로 받도록 수정

### TDD 증거

- RED: `AuthenticationFlowTest`의 `rejectsPreLoginCsrfTokenAfterLogin` 실패(403 기대, 204 실제). 프런트 `src/api/auth.test.ts` 실패(토큰 발급 2회 기대, 1회 실제)
- GREEN: 전략 조합 뒤 새 테스트 통과, 로그인 전 토큰을 재사용하던 기존 3건이 403으로 실패 → 로그인 뒤 토큰 사용으로 수정 후 통과. 프런트 캐시 제거 뒤 통과

### 실행 결과

- 기준: 브랜치 `fix/rotate-csrf-on-login`, main `86e725b` 기반
- `bash scripts/verify.sh backend`: 566건 통과(이전 564 + 신규 2), 실패·skip 0, 종료 0
- `bash scripts/verify.sh docs`: Python 61건·스택 문서 19항목 통과
- 프런트(로컬 Node 26): `npm test` 40개 파일 353건 통과, `npm run type-check` 통과, `npm run build-only` 성공, `check_test_reports.py frontend` PASS. Node 24 gate는 CI 판정
- 필수 suite 추가: `src/api/auth.test.ts`
- 미검증: 같은 사이트 출처의 쿠키 심기 재현, 실제 브라우저 로그인 흐름

## 운영 배포 동시 실행 그룹

```text
변경 전: workflow 수준 그룹 → 포크 PR CI 완료도 Production CD 실행 생성 → 그룹 대기 칸 차지
         → 대기 중이던 정상 배포 취소 → 자기 job은 if로 생략 → 아무것도 배포 안 됨
변경 후: deploy job 수준 그룹 → 생략된 job은 그룹에 들어가지 않음
```

- 현상: `deploy.yml`의 `crackcs-production` 그룹이 workflow 수준, 배포 자격(push 이벤트·성공)은 job `if`에서만 확인
- 위험 조건: 공개 저장소의 포크 PR이 `main` 이름 브랜치에서 CI를 완료하면 `workflow_run` 필터와 일치할 수 있음. GitHub 큐 동작은 소스로 관찰 불가
- 변경: 그룹을 `deploy` job으로 이동. 그룹 이름·`cancel-in-progress: false` 유지
- 남은 경계: 실제 GitHub 큐에서의 재현, 포크 PR 실행 승인 설정은 저장소 설정 소유

### TDD 증거

- RED: `python3 -m unittest scripts/test_deploy_workflow.py` 2건 실패(workflow 수준 `concurrency:` 존재, job 수준 블록 없음)
- GREEN: 블록 이동 뒤 2건 통과

### 실행 결과

- 기준: 브랜치 `ci/deploy-job-concurrency`, main `15a7f91` 기반
- `bash scripts/verify.sh docs`: Python 63건 통과(신규 2), 스택 문서 19항목 PASS
- 백엔드·프런트·PostgreSQL: 코드 변경 없음. CI 결과로 판정
- 미검증: 실제 Production CD 실행에서 포크 PR CI 완료 시 그룹 미참여 관찰

## 예상 밖 500 오류의 원인 로그

- 현상: `GlobalExceptionHandler.handleUnexpectedException`이 일반 500 응답과 requestId만 반환하고 예외를 버림. 사용자가 전달한 requestId로 서버 원인을 찾을 수 없음
- 변경: 같은 처리기에서 `requestId`와 예외(stack trace 포함)를 ERROR로 기록. 응답 본문은 기존 일반 메시지 유지, 예외 메시지 미노출
- requestId 일치 조건: `RequestIdFilter`가 가장 먼저 실행되어 요청 속성에 ID를 저장. 로그와 응답이 같은 속성 값을 사용
- 남은 경계: 로그 수집·보관 위치는 운영 VM 설정 소유

### TDD 증거

- RED: `GlobalExceptionHandlerTest.logsUnexpectedExceptionWithRequestId` 실패(출력에 `requestId=` 없음)
- GREEN: 로그 추가 뒤 10건 통과

### 실행 결과

- 기준: 브랜치 `fix/log-unexpected-server-errors`, main `e611a1a` 기반
- `bash scripts/verify.sh backend`: 567건 통과(이전 566 + 신규 1), 실패·skip 0
- 프런트·PostgreSQL: 변경 없음. CI 판정

## 세션 principal의 비밀번호 해시 제거

- 현상: `AuthenticatedMember`가 bcrypt 해시를 보관하고 `CredentialsContainer`를 구현하지 않아, `postgres` 프로필의 JDBC 세션 속성에 해시가 직렬화됨
- 변경: `AuthenticatedMember`가 `CredentialsContainer` 구현. `ProviderManager`의 기본 인증 뒤 자격 증명 삭제가 principal의 해시를 null로 바꿈
- 영향 범위: 비밀번호 비교는 삭제 전 `DaoAuthenticationProvider`에서 끝남. 세션 검증 필터는 회원 ID·역할·상태·인증 버전만 사용
- 호환성: `serialVersionUID` 유지, 필드 구성 유지. 기존 저장 세션도 역직렬화 가능(다음 로그인부터 해시 없음)

### TDD 증거

- RED: `AuthenticationFlowTest.storesAuthenticatedMemberWithoutPasswordHash` 실패(principal 비밀번호가 `{bcrypt}...` 값)
- GREEN: `CredentialsContainer` 구현 뒤 통과, 백엔드 전체 통과

### 실행 결과

- 기준: 브랜치 `fix/erase-session-password-hash`, main `2a552b6` 기반
- `bash scripts/verify.sh backend`: 568건 통과(이전 567 + 신규 1), 실패·skip 0
- PostgreSQL 세션 직렬화: 로컬 Docker 미사용. CI `postgres` job의 `SharedSessionPersistenceTest`로 판정
