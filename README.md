# crackCS

서술형 CS 답변을 저장하고, 근거를 붙여 평가한 결과를 개념별 학습 상태와 다음 문제로 연결하는 Java/Spring 백엔드 프로젝트.

현재 제출 범위: **외부 AI 연결 없이 실행 가능한 로컬 데모**. 기본 평가는 모의 응답이며 실제 정답 판정 품질을 의미하지 않음. 실제 모델 비교·출시 Gate는 별도 보류. [현재 작업 상태](docs/planning/tasks.md).

## 직접 실행

필수: Java 21, Node.js 24, npm 11 이상, Docker Compose. Gradle은 Wrapper 사용.

```bash
bash scripts/local-postgres.sh start
./gradlew bootRun --args='--spring.profiles.active=local,local-postgres'
```

다른 터미널:

```bash
cd front
npm ci
npm run dev -- --host 127.0.0.1 --strictPort
```

- 접속: `http://127.0.0.1:5173`
- 일반 회원: 화면에서 회원가입 → 문제집 → 답변 제출 → 평가·근거 → 지식 지도·후속 질문
- 관리자: `admin@crackcs.local` / `local admin passphrase` → 분류·문서·문제·평가 검토
- 공개 데모 자격증명, PC 내부 loopback 전용. 추가 서비스 요금 0원
- 처음 실행 시 예시 데이터 생성, 이후 재시작은 데이터 보존
- 기존 V001 DB: [V002 적용 절차](docs/changes/2026-09-27-release-readiness/local-runbook.md#기존-v001-db에-v002-적용) 먼저 확인
- 종료·백업·복구·포트 충돌: [실행 안내](docs/changes/2026-09-27-release-readiness/local-runbook.md)
- Docker 없는 개발 대안: `./gradlew bootRun --args='--spring.profiles.active=local'` — 별도 파일 H2, 개발용 `ddl-auto=update`

## 주요 설계

```text
Vue 화면 → HTTP·세션·검증 경계 → Service 유스케이스 → 도메인 규칙 → PostgreSQL
                                  │
답변 접수 transaction ─────────────┘
       ↓ 저장 후 202
평가 Worker의 DB lease → transaction 밖 평가 Port → 결과·근거·학습 상태 원자적 반영
                               모의 구현 / 외부 adapter
```

| 해결한 문제 | 구현과 확인 근거 |
|---|---|
| 재전송으로 답변·평가가 중복 생성 | 회원+요청 ID UNIQUE, 같은 요청은 기존 결과 반환. [멱등 접수](docs/changes/2026-09-07-phase-4/verification.md) |
| 느린 평가 중 transaction·연결 점유 | 접수·lease·외부 호출·완료 분리. [처리 책임](docs/changes/2026-09-22-operability/verification.md) |
| 여러 Worker의 중복 반영·학습 누적 손실 | DB lease, 적용 이력 UNIQUE, 잠금·충돌 재시도. [두 JVM 검증](docs/changes/2026-09-27-service-completion/verification.md) |
| 콘텐츠 교체 뒤 과거 평가 근거 소실 | 버전 생성·공개 직렬화, 폐기본과 Evidence 보존. [버전 검증](docs/changes/2026-09-27-release-readiness/verification.md) |
| 재시작·다른 서버에서 로그인 제한 초기화 | DB 공유 실패 창, 최초 생성 UNIQUE 경쟁 재시도, 독립 만료 정리. [인증 결정](docs/adr/0003-authentication-security-baseline.md) |
| 통신 실패 후 입력·이동 경로 유실 | 오류 표현 통일, 원문·멱등 키 보존, 안전한 수동 재시도. [화면 검증](docs/changes/2026-09-27-ui-content/verification.md) |

상태 규칙은 해당 객체가 소유. Service는 transaction·Repository·Port 호출 순서 조정. [도메인과 현재 ERD](docs/architecture/domain-model-and-erd.md).

## 검증과 확인 범위

```bash
./gradlew test postgresTest --console=plain
python3 -m unittest discover -s scripts -p 'test_content_bundle.py'
python3 scripts/content_bundle.py
cd front
npm test
npm run build
```

- H2 전체 회귀·PostgreSQL 17 Testcontainers·프런트·콘텐츠 구조 검사. 실행 수와 결과: [최신 검증](docs/changes/2026-09-27-service-completion/verification.md)
- 같은 PostgreSQL에 두 JVM: 동시 답변 10건, 10회 누적, 다른 앱의 중복 요청 재사용·로그인 차단 공유 확인
- cmux 실제 클릭: 답변 보존 → 모의 오류 → 재시도 완료·재로그인 뒤 원래 답변 복귀
- PostgreSQL HTTP 표본: 접수 p95 8.6ms, 주요 조회 p95 10~21ms. 데이터 크기·동시성·환경: [측정 근거](docs/changes/2026-09-27-release-readiness/verification.md). 운영 부하나 실제 AI 완료 지연의 보장 아님
- GitHub Actions: backend test, frontend test/type-check/build. PostgreSQL 검증은 Docker가 필요한 별도 명령
- [초기 콘텐츠](docs/content/initial-v1/README.md): 25문항·50개 Concept·5문서·20개 출처. 사람 검수 전 DRAFT, 자동 공개 없음

## 설정과 한계

- 기본 `ddl-auto=validate`; local H2 `update`; local-postgres `validate` + 명시적 V001/V002; test `create-drop`
- 인증 session은 서버 메모리. 두 앱 검증은 앱별 로그인 사용; 무중단 인증·로드밸런서·공유 session 검증 아님
- 로그인 제한은 계정+주소 조합, 답변 접수는 회원별 1분 10건. 분산 계정·주소의 전역 남용 방어는 공개 운영 전 과제
- 후속 Worker 비활성: `--crackcs.followup.worker-enabled=false`. 평가 비활성: `--crackcs.evaluation.worker-enabled=false`. 로그인 기록 정리는 독립 동작
- 실제 AI는 기본 비활성. 기존 실험·모델 설정: [로컬 모델 기록](docs/changes/2026-09-26-local-ollama/verification.md), [실행 안내](docs/changes/2026-09-27-release-readiness/local-runbook.md). 이번 마무리의 평가 품질 근거로 사용하지 않음
- 운영 profile의 DB·키 환경 변수는 `application-postgres.yaml` 참조. 실제 비밀 값은 파일·명령 인자·로그·Git에 기록 금지
- 사람 콘텐츠 승인·실제 참가자 파일럿·인터넷 배포는 미실행. [파일럿 실행안](docs/changes/2026-09-21-phase-8/pilot-runbook.md)

## 문서

- [문서 지도](docs/README.md): 문서별 책임과 현재 기준
- [제품 명세](docs/product/spec.md) · [인수 조건·P0 증거](docs/changes/2026-09-21-phase-8/acceptance-matrix.md)
- [OpenAPI](openapi.yml): 요청·응답·오류 계약
- [작업 목록](docs/planning/tasks.md) · [협업 규칙](AGENTS.md)
- [현재 스택 공식 문서](docs/engineering/stack-docs.md) · [프로젝트 검증 경로](docs/engineering/agent-workflow.md)
