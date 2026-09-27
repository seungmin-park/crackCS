# crackCS

질문에 직접 답하며 컴퓨터 과학 개념을 학습하는 애플리케이션. 구현 현황은 [작업 목록](docs/planning/tasks.md) 참조.

## 필수 도구

- Java 21
- Node.js 24
- npm 11 이상

Gradle: Wrapper 사용. 별도 설치 불필요.

최소 버전은 CI와 동일하다. 프런트 Node 버전의 기준 파일은 `front/.nvmrc`, npm 의존성 기준은 `front/package-lock.json`이다.

## 로컬 실행

백엔드: local profile로 실행. 개발 schema 자동 갱신, 화면 확인용 예제 데이터 준비. DB 파일 위치: `data/`.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

로컬 `gpt-oss:20b` 평가 후보 실행(Mac의 Homebrew 예시):

```bash
brew install ollama
brew services start ollama
ollama pull gpt-oss:20b
./gradlew bootRun --args='--spring.profiles.active=local,ollama'
```

- 평가: `ollama` profile에서만 로컬 모델 사용. 후속 질문 생성은 local stub 유지
- 환경: 모델 가중치 약 13GB, 통합 메모리 16GB는 공식 권장 하한. 설치·서버 확인: `ollama list`, `ollama ps`
- 추론: 기본 `medium`. `OLLAMA_REASONING_EFFORT`로 `low`·`medium`·`high` 선택. 결과의 평가기 버전에 선택값을 덧붙여 구분
- 시간: `ollama` profile의 평가 lease 4분, 호출 제한 3분. 제한 시간 변경 시 lease가 더 길어야 함
- 비용: USD 토큰 단가 0. 컴퓨터 사용 비용은 별도
- 실패: 모델 미설치·서버 중단 시 Worker가 재시도하고 최대 시도 후 실패 처리. OpenAI 평가와 동시 활성화 금지
- 출시: 실제 판정 품질과 20초 p95 목표는 [실측 Gate](docs/planning/tasks.md#phase-5-실제-모델-품질-gate)에서 확인 필요
- 연결 근거: [Ollama 후보 검증](docs/changes/2026-09-26-local-ollama/verification.md)

`ollama` profile 선택 환경 변수:

| 이름 | 기본값 | 용도 |
|---|---|---|
| `OLLAMA_CHAT_ENDPOINT` | `http://127.0.0.1:11434/api/chat` | 로컬 채팅 API 주소 |
| `OLLAMA_MODEL` | `gpt-oss:20b` | 평가 모델 태그 |
| `OLLAMA_EVALUATOR_VERSION` | `os-evaluator-v1` | 평가 규칙 버전. 저장 시 추론 강도 접미사 추가 |
| `OLLAMA_REASONING_EFFORT` | `medium` | 추론 강도: `low`, `medium`, `high` |
| `OLLAMA_TIMEOUT` | `3m` | 로컬 모델 HTTP 제한 시간. 평가 lease 4분 이내 유지 |

local seed: 화면 확인용 예시 문제·Concept·검색 가능한 근거 문서·관리자 계정.

- 관리자 이메일: `admin@crackcs.local`
- 관리자 비밀번호: `local admin passphrase`

계정 사용 범위: local H2 전용. 운영 사용 금지.

후속 질문 Worker 설정:

- 기본 활성. local은 stub 생성기 사용, 실제 OpenAI 품질 검증과 구분
- 생성기 없이 Worker만 활성화하면 시작 단계에서 설정 오류로 차단
- 후속 생성을 사용하지 않을 때: `--crackcs.followup.worker-enabled=false`
- 실제 생성 사용 시: `--crackcs.followup.openai.enabled=true` 및 기존 evaluation OpenAI 키 설정 필요
- 스케줄러 기본 2개 스레드: 평가와 후속 생성의 상호 대기 완화. 처리량 보장은 별도 측정 필요

프런트: 다른 터미널에서 실행. `/api` 요청은 Vite proxy를 거쳐 `http://localhost:8080`으로 전달.

```bash
cd front
npm install
npm run dev
```

접속: `http://127.0.0.1:5173/`. USER 로그인 후 학습 홈, `/knowledge-map`에서 지식 지도 확인.

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

운영 PostgreSQL profile 환경 변수:

| 이름 | 필수 조건 | 용도 |
|---|---|---|
| `DATABASE_URL` | 기본값 변경 시 | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | 기본값 변경 시 | DB 계정 |
| `DATABASE_PASSWORD` | 항상 | DB 비밀번호 |
| `SESSION_COOKIE_SECURE` | HTTPS 운영 | session cookie의 Secure 속성. 운영 profile은 기본 `true` |
| `OPENAI_ENABLED` | 선택 | 기본 `false`. 실제 provider 검증에서만 `true` |
| `OPENAI_API_KEY` | `OPENAI_ENABLED=true` | provider API key |
| `OPENAI_MODEL` | 선택 | 평가 모델 이름 |
| `EVALUATOR_VERSION` | 선택 | 평가 규칙 버전 |

비밀 값은 설정 파일, 실행 명령 인자, 로그에 기록하지 않는다. 실제 OpenAI를 사용하지 않는 실행은 `OPENAI_ENABLED=false`를 유지한다.

버전 기반 DB migration 도구: 미사용. 로컬 PostgreSQL은 명시적 V001 SQL + `validate` 사용. [0원 로컬 실행·복구](docs/changes/2026-09-27-release-readiness/local-runbook.md) 참조. 운영에서 `update` 사용 금지.

Phase 4 답변·평가: USER로 회원가입 후 문제 상세에서 제출. 기본 local/test는 모의 평가, `local,ollama`는 로컬 실제 모델 평가 후보.
결과·실패 재현 설정과 검증 범위: [Phase 4 기록](docs/changes/2026-09-07-phase-4/verification.md).

## 검증

```bash
./gradlew test
./gradlew ollamaLiveEvaluation # Ollama 서비스와 gpt-oss:20b 설치 후 4건 스모크 측정

cd front
npm run test
npm run type-check
npm run build-only
```

GitHub Actions는 push와 pull request에서 백엔드 전체 테스트, 프런트 테스트·type-check·production build를 같은 버전 기준으로 실행한다. `postgresTest`는 Docker가 필요한 별도 운영 검증이며 기본 CI에는 포함하지 않는다.

## 문서

- [문서 지도](docs/README.md): 전체 문서의 역할·존재 여부·상태·위치·갱신 기준
- [제품 명세](docs/product/spec.md): 요구사항·인수 조건
- [작업 목록](docs/planning/tasks.md): 진행·검증 상태
- [OpenAPI](openapi.yml): HTTP 계약
- [작업 지침](AGENTS.md): 협업·설계·테스트 규칙
