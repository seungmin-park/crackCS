# ADR-0003: Phase 2 인증 보안 최소 기준

- 상태: 승인
- 결정일: 2026-08-31
- 관련 항목: P2-T07

## 로그인 시도 제한

- 같은 정규화 이메일에서 10분 안에 5회 실패하면 주소와 무관하게 15분 동안 차단
- 차단 중 요청은 `429 Too Many Requests`와 `Retry-After`를 반환한다.
- 성공하면 해당 계정의 실패 상태 제거. 주소·전체 요청 예산은 성공해도 유지
- key: `account|정규화 이메일`의 SHA-256 digest. `login_attempt` 보관, 원문 이메일·주소 컬럼 없음
- 2026-09-27 변경: 프로세스 메모리 대신 PostgreSQL 공유 저장. `LoginAttempt`가 실패 창·차단 규칙 소유
- 기존 행은 비관적 잠금, 최초 행은 UNIQUE 경쟁 시 독립 transaction 최대 3회 재시도
- 만료 실패 기록: 1분마다 200개씩 최대 저장 상한까지 여러 페이지 재확인·삭제
- digest는 추측 가능한 조합을 대조할 수 있는 가명 값. 접근 권한·백업 보호 필요
- proxy header는 신뢰하지 않고 `getRemoteAddr()` 사용. trusted proxy 도입 시 주소 정책 재설계 필요
- 2026-10-06: 계정+주소 조합에서 계정별 실패 + 주소·전체 요청 예산으로 변경. V004 적용 시 이전 조합의 임시 실패 기록 초기화
- 로그인 제한 상태와 인증 session은 별개. session은 여전히 서버 메모리, 재시작·다른 앱에서는 재로그인 필요
- [변경 검증](../changes/2026-09-27-service-completion/verification.md), [schema 절차](0006-local-postgres-schema.md)

## 인증 요청 예산과 공개 가입 응답

| 기본 1분 창 | 주소별 | 전체 |
|---|---|---|
| 로그인 | 30 | 300 |
| 회원가입 | 5 | 30 |

- `crackcs.auth.requests`: `login-source-limit`, `login-global-limit`, `sign-up-source-limit`, `sign-up-global-limit`로 조정
- `max-state-count` 기본 5000: 요청 bucket과 계정 실패 기록 각각의 최대 행 수. bucket에는 고정 guard 포함
- 공유 capacity guard 잠금 → 전역 bucket → source bucket 예약. 실패 기록 생성도 같은 guard 잠금 아래 용량 확인
- 초과 시 429·Retry-After. 정상·실패·중복 요청 모두 예산 소비. 로그인 hashing·가입 생성 전에 확인
- source bucket: 사용 창이 2분 넘게 지난 뒤 삭제. 전역·capacity guard 3종은 유지
- 관계형 DB 고정 창 선택: 기존 PostgreSQL 공유·잠금 경로 재사용. Redis 의존성 추가 없음
- 전역 guard의 직렬화·DB 접근 비용, 창 경계의 burst는 남은 성능 경계
- 신규·중복 가입 요청: 동일 202·중립 안내·회원 정보 미포함. 중복 계정의 비밀번호 변경 금지
- 중복 확인 전에 같은 password hashing 경로 실행. 전체 처리 시간 분포의 동일성 인증 아님
- 사용자 선택 범위: 이메일 소유 인증·발송·미인증 계정 만료는 미도입, 현재 즉시 가입 흐름 유지
- [현재 검증](../changes/2026-10-06-security-hardening/verification.md)

## 로그 정책

- 잘못된 비밀번호 같은 일상적인 실패는 매번 기록하지 않는다.
- 제한에 걸린 보안 이벤트만 기록하며 이메일은 첫 글자와 domain만 남겨 마스킹한다.
- 비밀번호, password hash, session ID와 CSRF token은 로그 인자로 전달하지 않는다.
- HTTP request body 전체 logging은 인증 endpoint에 적용하지 않는다.

## 브라우저 경계

- 인증 성공 시 session ID를 교체해 session fixation을 방어한다.
- unsafe method에는 CSRF token header를 요구한다.
- credential을 허용하는 cross-origin CORS 정책을 구성하지 않는다.
- same-origin Vite proxy는 개발 편의 수단이며 운영 보안 판단은 Spring Security가 수행한다.
