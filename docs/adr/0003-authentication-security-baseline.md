# ADR-0003: Phase 2 인증 보안 최소 기준

- 상태: 승인
- 결정일: 2026-08-31
- 관련 항목: P2-T07

## 로그인 시도 제한

- 같은 정규화 이메일과 원격 주소 조합에서 10분 안에 5회 실패하면 15분 동안 차단한다.
- 차단 중 요청은 `429 Too Many Requests`와 `Retry-After`를 반환한다.
- 성공하면 해당 조합의 실패 상태를 제거한다.
- key: 정규화 이메일+원격 주소의 SHA-256 digest. `login_attempt`에 보관, 원문 이메일·주소 컬럼 없음
- 2026-09-27 변경: 프로세스 메모리 대신 PostgreSQL 공유 저장. `LoginAttempt`가 실패 창·차단 규칙 소유
- 기존 행은 비관적 잠금, 최초 행은 UNIQUE 경쟁 시 독립 transaction 최대 3회 재시도
- 만료 기록: 1분마다 최대 200개 재확인·삭제. 즉시 TTL 삭제나 익명화 보장 아님
- digest는 추측 가능한 조합을 대조할 수 있는 가명 값. 접근 권한·백업 보호 필요
- proxy header는 신뢰하지 않고 `getRemoteAddr()` 사용. trusted proxy 도입 시 주소 정책 재설계 필요
- 제한 범위는 계정+주소 조합. 분산 주소·여러 계정의 전역 공격 제한은 미구현, loopback 데모 외 공개 운영 전 필요
- 로그인 제한 상태와 인증 session은 별개. session은 여전히 서버 메모리, 재시작·다른 앱에서는 재로그인 필요
- [변경 검증](../changes/2026-09-27-service-completion/verification.md), [schema 절차](0006-local-postgres-schema.md)

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

