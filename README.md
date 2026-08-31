# crackCS

질문에 직접 답하며 컴퓨터 과학 개념을 학습하는 애플리케이션입니다. Phase 1은 공개 상태(`PUBLISHED`)인 문제의 목록과 상세 조회를 제공합니다.

## 필수 도구

- Java 21
- Node.js 24
- npm 11 이상

Gradle은 Wrapper를 사용하므로 별도 설치가 필요하지 않습니다.

## 로컬 실행

터미널 하나에서 local profile로 백엔드를 실행합니다. Hibernate가 개발 schema를 갱신하고 Spring SQL 초기화가 화면 확인용 예제 데이터를 준비하며, 로컬 DB 파일은 `data/`에 생성됩니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

local profile은 API와 Vue 화면 확인용 예시 문제와 관리자 계정을 seed한다.

- 관리자 이메일: `admin@crackcs.local`
- 관리자 비밀번호: `local admin passphrase`

이 계정은 local H2에서만 생성되며 운영 환경에 사용하지 않는다.

다른 터미널에서 프런트를 실행합니다. `/api` 요청은 Vite proxy를 통해 `http://localhost:8080`으로 전달됩니다.

```bash
cd front
npm install
npm run dev
```

브라우저에서 `http://127.0.0.1:5173/questions`를 엽니다.

```text
Browser :5173
      │ /api/questions
      ▼
Vite proxy ─────────▶ Spring Boot :8080 ─▶ H2 + local SQL seed
```

schema 정책은 profile별로 다릅니다.

- 기본 profile: `ddl-auto=validate` — 외부에서 준비한 schema와 JPA mapping만 검증한다.
- local profile: `ddl-auto=update` — 개발 편의를 위해 Hibernate가 H2 schema를 갱신한다.
- test profile: `ddl-auto=create-drop` — 각 테스트 context가 독립 schema를 사용한다.

현재는 버전 기반 DB migration 도구를 사용하지 않는다. 따라서 local의 `update`를 운영에 사용하지 않으며, 운영 DB를 도입하기 전에 별도의 schema 변경·배포 절차를 결정해야 한다.

## 검증

```bash
./gradlew test

cd front
npm run test
npm run type-check
npm run build-only
```

API 계약은 루트의 `openapi.yml`, 구현 작업 현황은 `docs/tasks.md`에서 확인할 수 있습니다.
