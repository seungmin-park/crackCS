# crackCS

질문에 직접 답하며 컴퓨터 과학 개념을 학습하는 애플리케이션. 구현 현황은 [작업 목록](docs/planning/tasks.md) 참조.

## 필수 도구

- Java 21
- Node.js 24
- npm 11 이상

Gradle: Wrapper 사용. 별도 설치 불필요.

## 로컬 실행

백엔드: local profile로 실행. 개발 schema 자동 갱신, 화면 확인용 예제 데이터 준비. DB 파일 위치: `data/`.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

local seed: 화면 확인용 예시 문제·관리자 계정.

- 관리자 이메일: `admin@crackcs.local`
- 관리자 비밀번호: `local admin passphrase`

계정 사용 범위: local H2 전용. 운영 사용 금지.

프런트: 다른 터미널에서 실행. `/api` 요청은 Vite proxy를 거쳐 `http://localhost:8080`으로 전달.

```bash
cd front
npm install
npm run dev
```

접속: `http://127.0.0.1:5173/questions`.

```text
Browser :5173
      │ /api/questions
      ▼
Vite proxy ─────────▶ Spring Boot :8080 ─▶ H2 + local SQL seed
```

profile별 schema 정책:

- 기본 profile: `ddl-auto=validate` — 외부 schema와 JPA mapping 검증.
- local profile: `ddl-auto=update` — 개발용 H2 schema 자동 갱신.
- test profile: `ddl-auto=create-drop` — 테스트용 schema 생성·종료 시 삭제.

버전 기반 DB migration 도구: 미사용. 운영에서 `update` 사용 금지. 운영 DB 도입 전 schema 변경·배포 절차 결정 필요.

## 검증

```bash
./gradlew test

cd front
npm run test
npm run type-check
npm run build-only
```

## 문서

- [문서 지도](docs/README.md): 전체 문서의 역할·존재 여부·상태·위치·갱신 기준
- [제품 명세](docs/product/spec.md): 요구사항·인수 조건
- [작업 목록](docs/planning/tasks.md): 진행·검증 상태
- [OpenAPI](openapi.yml): HTTP 계약
- [작업 지침](AGENTS.md): 협업·설계·테스트 규칙
