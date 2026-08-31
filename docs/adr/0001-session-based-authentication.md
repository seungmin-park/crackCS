# ADR-0001: 동일 출처 웹의 서버 세션 인증

- 상태: 승인
- 결정일: 2026-08-31
- 관련 항목: OQ-001, P2-T01

## 배경

CrackCS의 초기 배포 구조는 Vue 애플리케이션과 Spring API가 같은 출처에서 제공되는 웹 애플리케이션이다. 브라우저 새로고침 뒤에도 인증을 복구하고, 로그아웃과 회원 상태 변경 시 기존 인증을 통제해야 한다.

## 검토한 선택지

### 서버 세션

- 세션 ID만 cookie로 전달하고 인증 상태는 서버가 관리한다.
- `HttpOnly` cookie를 사용하면 JavaScript가 세션 ID를 읽지 못한다.
- 서버에서 세션을 무효화하면 로그아웃을 즉시 반영할 수 있다.
- 여러 서버로 확장하면 공용 session store 또는 sticky session이 필요하다.
- cookie가 자동 전송되므로 CSRF 방어가 필요하다.

### Access token과 refresh token

- API 서버를 stateless하게 구성하기 쉽고 여러 종류의 client에서 재사용하기 좋다.
- 브라우저의 안전한 token 보관, refresh rotation, 폐기 목록과 탈취 대응이 추가로 필요하다.
- localStorage에 인증 token을 보관하면 XSS 한 번으로 token이 유출될 수 있다.
- 현재 같은 출처 웹 하나에는 운영 복잡도가 이점보다 크다.

## 결정

서버 세션 인증을 사용한다.

```text
로그인 성공
   ↓
Spring Security Authentication 생성
   ↓
session ID 교체 (session fixation 방어)
   ↓
SecurityContext를 HttpSession에 저장
   ↓
브라우저에는 session cookie만 전달
```

session cookie 정책은 다음과 같다.

- 이름: `CRACKCS_SESSION`
- `HttpOnly=true`
- `SameSite=Lax`
- `Path=/`
- 운영 HTTPS: `Secure=true`
- local HTTP: `Secure=false`
- JavaScript 저장소에 session ID 또는 인증 token을 저장하지 않는다.

`SameSite=Lax`는 외부 링크로 들어온 사용자의 로그인 상태를 유지하면서 cross-site 상태 변경 요청을 줄이는 절충안이다. SameSite는 CSRF의 보조 방어일 뿐이므로 CSRF token을 함께 사용한다.

## CSRF 정책

- Spring Security CSRF 보호를 활성화한다.
- SPA는 공개 CSRF endpoint에서 token을 받고 상태 변경 요청의 `X-XSRF-TOKEN` header로 전송한다.
- 회원가입, 로그인, 로그아웃을 포함한 모든 unsafe method에 token을 요구한다.
- API와 프런트는 같은 출처로 운영하고 임의의 cross-origin credential 요청을 허용하지 않는다.

```text
공격 사이트가 session cookie를 자동 전송
              +
공격 사이트는 CSRF token을 읽거나 custom header로 보내지 못함
              ↓
Spring Security가 상태 변경 요청 거부
```

## 결과

- 새로고침 시 `/api/members/me`로 서버 세션을 확인해 상태를 복구한다.
- 로그아웃은 서버 세션과 SecurityContext를 무효화한다.
- 수평 확장 시 Redis 같은 공용 session store 도입을 검토해야 한다.
- 네이티브 앱이나 외부 API client가 추가되면 해당 client용 token 인증을 별도 SecurityFilterChain으로 검토한다.

