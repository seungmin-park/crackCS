# 로컬 PostgreSQL 실행·복구

## 실행 환경

- 기본 실행 추가 서비스 요금 0원. 실제 GPT 시연을 켜면 사용량 과금
- Java 21, Node.js 24, npm 11 이상, Docker Compose
- 기본 평가·후속 질문은 모의 응답. 실제 GPT 시연은 아래 환경 변수로 선택
- 공개 데모 계정·비밀번호: 인터넷 노출 금지
- PostgreSQL과 앱 모두 loopback 바인딩. 기존 `data/crackcs-local` H2 파일 미사용

## 빈 환경 실행

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

- 화면: `http://127.0.0.1:5173`
- 관리자: `admin@crackcs.local` / `local admin passphrase`
- 일반 회원: 화면에서 회원가입 후 로그인
- 예시: 운영체제 Topic·프로세스/스레드 Concept·공개 문제·공개 문서·검색 Chunk
- 자동 테스트는 예시 데이터에 의존하지 않음
- 최초 volume 초기화에서만 SQL 실행. 재시작 시 기존 데이터 유지
- 포트 충돌 시 기존 프로세스 임의 종료 금지. 원인 확인 후 포트·Vite proxy 함께 변경

## 실제 GPT 시연

- OpenAI Platform 프로젝트의 월 강제 지출 한도와 잔액을 먼저 확인. 내부 예산은 추정치이며 외부 강제 한도를 대체하지 않음
- 키는 1Password에서 복사해 백엔드 터미널의 숨김 입력에 붙여넣기. 채팅·명령 인자·파일·Git에 값 기록 금지
- 새 백엔드 터미널에서 아래 명령 사용. 첫 `read`는 화면에 입력을 표시하지 않음. 평가와 후속 질문을 별도로 끌 수 있음

```bash
read -r -s OPENAI_API_KEY
export OPENAI_API_KEY
export OPENAI_EVALUATION_ENABLED=true
export OPENAI_FOLLOWUP_ENABLED=true
export OPENAI_MONTHLY_BUDGET_USD=2
./gradlew bootRun --args='--spring.profiles.active=local,local-postgres'
```

- 평가 전용: `OPENAI_FOLLOWUP_ENABLED=false`. 무료 기본 흐름: 두 스위치 모두 미설정
- 한정된 실제 API 평가 재측정: [검증 기록](../2026-09-29-openai-completion/verification.md). 기본 라이브 러너는 1건·추정 $0.25 중단 한도
- 실행 터미널 종료 시 셸에 남은 키도 사라짐. 시연 후 로컬 앱·DB는 아래 종료 절차 사용

```text
Browser :5173 → Vite /api proxy → Spring :8080
                                    ↓ validate
                          PostgreSQL :55432
                          명시적 V001 + V002 + V003 schema
```

## backup·새 DB restore

```bash
mkdir -p build/backups
bash scripts/local-postgres.sh backup build/backups/local-001.dump
bash scripts/local-postgres.sh restore build/backups/local-001.dump crackcs_restore_001
LOCAL_DATABASE_NAME=crackcs_restore_001 ./gradlew bootRun --args='--spring.profiles.active=local,local-postgres'
```

- backend는 원본 실행 종료 후 복구 DB로 실행
- backup 파일이 이미 존재하면 거절
- `crackcs_restore_` 접두어의 새 DB만 허용. 이미 존재하면 `createdb` 실패
- restore는 단일 transaction. 실패 DB는 사용 중지하고 다른 새 이름으로 재시도
- 원본 DB·volume·archive 자동 삭제 없음
- 회원 로그인·답변·평가·근거 조회와 원본/복구 row 비교: [복구 절차](../2026-09-21-phase-8/backup-restore.md)
- archive에는 비밀번호 hash·학습 이력 포함. Git 업로드 금지, 로컬 `build/` 안에 보관
- 같은 Docker 저장소 안의 복구 검증. 원격 장애 복구·암호화 외부 보관 검증을 대체하지 않음

## schema 변경

- 기준: [ADR-0006](../../adr/0006-local-postgres-schema.md)
- V001·V002 수정 재적용 금지. 다음 변경은 V003 이상의 새 SQL
- 배포 순서: backup → 새 DB restore 리허설 → 새 SQL을 transaction으로 적용 → 앱 validate·회귀 확인 → 원본에 동일 적용
- 실패 시 앱 배포 중지. 파괴적 역방향 DDL 대신 이전 앱과 검증된 복구 DB로 전환
- 운영 배포 자동화·migration 도구는 미도입

### 기존 V001 DB에 V002 적용

새 volume은 Compose가 V001→local sample→V002 순서로 자동 초기화. 기존 volume은 자동 변경 없음.

1. 앱 종료 후 위 backup·새 DB restore 수행
2. 복구 DB 이름으로 아래 명령의 `-d crackcs_local`을 바꿔 먼저 리허설
3. 복구 앱 `ddl-auto=validate`·health·회귀 확인 후 원본에 같은 SQL 적용

```bash
docker compose -p crackcs-local -f compose.local.yaml exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U crackcs_local -d crackcs_local \
  < src/main/resources/db/postgres/V002__shared_login_attempt.sql
```

- 적용 확인: `schema_version`에 `001`, `002` 각 한 행
- 재적용 시 테이블 중복 오류로 transaction 중단. 임의 `IF NOT EXISTS`·버전 행 수정 금지
- V002 이전 백업을 새 앱에 연결하려면 복구 DB에도 V002 적용 필요
- schema drift는 앱 시작 검증에서 중단. 이전 앱은 추가 테이블을 사용하지 않아 호환
- 적용·보존 근거: [V002 검증](../2026-09-27-service-completion/verification.md)

### 기존 V002 DB에 V003 적용

- backup·새 DB restore에서 먼저 리허설. 원본 적용 전 앱 종료
- 새 volume은 Compose에서 V003까지 적용. 기존 volume 자동 변경 없음
- 복구 DB 이름으로 `-d crackcs_local`을 바꿔 같은 SQL 적용 후 새 앱 `validate` 확인

```bash
docker compose -p crackcs-local -f compose.local.yaml exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U crackcs_local -d crackcs_local \
  < src/main/resources/db/postgres/V003__member_authentication_version.sql
```

- `schema_version` 003 행, `member.authentication_version` NOT NULL·기본값 0 확인
- V003 없는 기존 DB는 새 앱의 schema validate에서 거부
- [검증 기록](../2026-10-06-security-hardening/verification.md)

## 종료

```bash
bash scripts/local-postgres.sh stop
```

- 컨테이너 정지만 수행. 데이터 volume 보존
