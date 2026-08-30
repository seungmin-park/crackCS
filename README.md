# crackCS

질문에 직접 답하며 컴퓨터 과학 개념을 학습하는 애플리케이션입니다. Phase 1은 공개 상태(`PUBLISHED`)인 문제의 목록과 상세 조회를 제공합니다.

## 필수 도구

- Java 21
- Node.js 24
- npm 11 이상

Gradle은 Wrapper를 사용하므로 별도 설치가 필요하지 않습니다.

## 로컬 실행

터미널 하나에서 local profile로 백엔드를 실행합니다. Flyway가 schema와 Phase 1 예제 데이터를 준비하며, 로컬 DB 파일은 `data/`에 생성됩니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

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
Vite proxy ─────────▶ Spring Boot :8080 ─▶ H2 + Flyway seed
```

## 검증

```bash
./gradlew test

cd front
npm run test
npm run type-check
npm run build-only
```

API 계약은 루트의 `openapi.yml`, 구현 작업 현황은 `docs/tasks.md`에서 확인할 수 있습니다.
