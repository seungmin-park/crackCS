# 현재 기술 스택의 공식 문서

- 독자·질문: CrackCS 작업자·에이전트 / 지금 선택된 버전에서 어느 공식 문서를 참고할 것인가?
- 기준: [버전·공식 URL 목록](stack-docs.json), 확인일 2026-09-29
- 버전의 소유자: Java·Spring Boot·PostgreSQL은 `build.gradle`, Gradle은 wrapper, Node는 `front/.nvmrc`, 프런트 실제 설치 버전은 `front/package-lock.json`
- 범위: 주요 직접 사용 스택과 Spring Boot BOM이 관리하는 핵심 라이브러리 18개. 모든 전이 의존성의 문서 목록 아님

```text
빌드 설정·lockfile (실제 선택)
       ↓ 버전 대조
stack-docs.json (확인한 공식 문서의 범위·URL)
       ↓ CI 검사
코드·의존성 변경 시 문서 재검토 누락을 차단
```

## 바로 읽을 문서

| 작업 | 현재 선택 버전 | 공식 문서 |
|---|---|---|
| Java API·언어 | 21 | [Java SE 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/) |
| Spring Boot 설정·웹·테스트 | 4.1.1 | [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/) |
| Spring MVC·transaction | Framework 7.0.9 | [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/) |
| 인증·인가 | Security 7.1.1 | [Spring Security Reference](https://docs.spring.io/spring-security/reference/) |
| Repository·JPA | Data JPA 4.1.1, Hibernate 7.4.5.Final | [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/), [Hibernate 7.4](https://docs.hibernate.org/orm/7.4/userguide/html_single/) |
| 영속성 표준·DB 테스트 | Jakarta Persistence 3.2.0, Testcontainers 2.0.5 | [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/), [Testcontainers for Java](https://java.testcontainers.org/) |
| DB | PostgreSQL 17 | [PostgreSQL 17](https://www.postgresql.org/docs/17/) |
| 빌드·런타임 | Gradle 9.7.1, Node 24.20.0 지정 | [Gradle 9.7.1 User Manual](https://docs.gradle.org/9.7.1/userguide/), [Node 24 API](https://nodejs.org/docs/latest-v24.x/api/) |
| Vue 화면·라우팅 | Vue 3.5.42, Router 5.3.0 | [Vue 3 Guide](https://vuejs.org/guide/introduction.html), [Router 5 변경점](https://router.vuejs.org/guide/migration/v4-to-v5.html) |
| TypeScript·번들·테스트 | TypeScript 6.0.3, Vite 8.2.2, Vitest 4.1.11 | [TypeScript 6.0 변경점](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-6-0.html), [Vite Guide](https://vite.dev/guide/), [Vitest 4 Guide](https://v4.vitest.dev/guide/) |
| UI 컴포넌트·스타일 | Element Plus 2.14.5, Bootstrap 5.3.8 | [Element Plus Guide](https://element-plus.org/en-US/guide/design.html), [Bootstrap 5.3](https://getbootstrap.com/docs/5.3/getting-started/introduction/) |

- Spring Boot·Framework·Security·Data JPA의 공식 현재 페이지는 확인일에 각각 4.1.1·7.0.9·7.1.1·4.1.1을 표시. `current` URL이 다른 버전으로 이동하면 당시 목록만 믿지 말고 사용 중인 버전의 공식 보존 문서·릴리스 노트 재확인
- Hibernate·Vitest·PostgreSQL·Java·Node·Bootstrap은 URL이 버전 계열을 고정. 같은 계열의 patch 차이는 별도 릴리스 노트 확인
- Vue·Router·Vite·Element Plus는 공식 가이드가 이동 가능. 목록의 실제 설치 patch는 lockfile, 가이드의 적용 범위는 `docScope`에서 구분
- Gradle 문서는 wrapper의 9.7.1 링크로 고정. Wrapper가 오르면 문서 링크도 함께 변경
- `front/.nvmrc`의 24.20.0은 요구 버전. 실행 중인 `node --version`은 별도로 확인. CI는 `.nvmrc`로 Node 설치

선택적 평가 연결: 기본 실행은 모의 평가. `ollama` profile의 [Ollama Chat API](https://docs.ollama.com/api/chat)와 활성화 조건이 있는 [OpenAI 구조화 출력](https://developers.openai.com/api/docs/guides/structured-outputs)은 실제 모델 작업에서만 해당 provider 설정·계약과 함께 확인. 이 목록의 기본 실행 버전 검사에는 포함하지 않음.

## 동기화 절차

1. 빌드 설정이나 package/lockfile 변경
2. `python3 scripts/check_stack_docs.py` 실행. 버전 변경 시 실패가 정상
3. 해당 버전의 공식 페이지 확인 후 `stack-docs.json`의 `version`, `officialUrl`, `docScope`, `checkedOn` 갱신
4. 이 문서의 빠른 목록도 갱신. 검사와 테스트 재실행
5. 실제 코드 동작이 바뀌면 해당 Java·Vue 테스트도 실행. 문서 검사 통과로 사용자 흐름을 대체하지 않음

오프라인 검사는 선언된 버전·URL의 공식 호스트·문서 범위 존재를 확인. 사이트의 현재 내용이나 URL 생존 여부는 네트워크 없이 인증할 수 없음. 공식 페이지를 읽은 날짜와 확인한 버전을 목록에 남기고, 의존성 변경 때 다시 확인.
