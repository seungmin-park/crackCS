# ADR-0003: Phase 2 인증 보안 최소 기준

- 상태: 승인
- 결정일: 2026-08-31
- 관련 항목: P2-T07

## 로그인 시도 제한

- 같은 정규화 이메일과 원격 주소 조합에서 10분 안에 5회 실패하면 15분 동안 차단한다.
- 차단 중 요청은 `429 Too Many Requests`와 `Retry-After`를 반환한다.
- 성공하면 해당 조합의 실패 상태를 제거한다.
- key는 이메일과 원격 주소 원문이 아니라 SHA-256 digest로 메모리에 보관한다.
- 현재 구현은 단일 프로세스 메모리 방식이므로 재시작하면 초기화되고 여러 서버 사이에 공유되지 않는다.
- 운영을 수평 확장할 때 Redis 같은 공용 저장소와 trusted proxy의 실제 client IP 해석을 도입한다.

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

