# PostgreSQL backup·restore 절차

## 상태와 책임

- 상태: 절차 확정, 로컬 `pg_dump`/`pg_restore` 실행 파일 미설치 확인
- 미실행: production-equivalent backup·빈 환경 restore·복구 판정
- 실행 책임: DB 운영 담당자
- 판정 책임: 애플리케이션 운영 담당자
- 증거 위치: 실행 일시별 `backup-manifest.txt`, restore 로그, 검증 query 결과

2026-09-21 로컬 사전 확인에서 `pg_dump --version`은 `command not found`로 종료됐다. Docker 기반 PostgreSQL 통합 테스트 성공과 별개로, backup·restore 작업은 PostgreSQL client 설치 후 production-equivalent 격리 환경에서 실행해야 한다.

## 보존 결정

- 일간 backup: 7일
- 주간 backup: 4주
- 월간 backup: 3개월
- 파일럿 기준의 프로젝트 결정. 법적·사업상 보존 요구가 생기면 별도 결정으로 연장
- 암호화 저장소, 운영 DB와 다른 장애 영역, 접근 최소화 필수

## 필요한 환경 변수

값을 문서·로그·shell history에 기록하지 않는다.

```text
CRACKCS_BACKUP_HOST
CRACKCS_BACKUP_PORT
CRACKCS_BACKUP_DATABASE
CRACKCS_BACKUP_USER
CRACKCS_BACKUP_PASSWORD
CRACKCS_BACKUP_FILE
CRACKCS_RESTORE_DATABASE
```

## backup

```bash
PGPASSWORD="${CRACKCS_BACKUP_PASSWORD}" pg_dump \
  --host="${CRACKCS_BACKUP_HOST}" \
  --port="${CRACKCS_BACKUP_PORT}" \
  --username="${CRACKCS_BACKUP_USER}" \
  --dbname="${CRACKCS_BACKUP_DATABASE}" \
  --format=custom \
  --no-owner \
  --no-privileges \
  --file="${CRACKCS_BACKUP_FILE}"

pg_restore --list "${CRACKCS_BACKUP_FILE}"
shasum -a 256 "${CRACKCS_BACKUP_FILE}"
```

성공 조건:

- `pg_dump` exit 0
- custom-format archive 목록 조회 성공
- checksum, 시작·종료 시각, PostgreSQL 버전, 원본 DB 식별자를 manifest에 기록
- 비밀번호·connection string은 manifest에서 제외

## 빈 환경 restore

`CRACKCS_RESTORE_DATABASE`는 기존 데이터가 없는 복구 검증 전용 DB 이름이어야 한다. 운영 DB 이름을 사용하지 않는다.

```bash
PGPASSWORD="${CRACKCS_BACKUP_PASSWORD}" createdb \
  --host="${CRACKCS_BACKUP_HOST}" \
  --port="${CRACKCS_BACKUP_PORT}" \
  --username="${CRACKCS_BACKUP_USER}" \
  "${CRACKCS_RESTORE_DATABASE}"

PGPASSWORD="${CRACKCS_BACKUP_PASSWORD}" pg_restore \
  --host="${CRACKCS_BACKUP_HOST}" \
  --port="${CRACKCS_BACKUP_PORT}" \
  --username="${CRACKCS_BACKUP_USER}" \
  --dbname="${CRACKCS_RESTORE_DATABASE}" \
  --no-owner \
  --no-privileges \
  --exit-on-error \
  "${CRACKCS_BACKUP_FILE}"
```

## row-level 검증

backup 전 원본과 restore 후 복구 DB에서 같은 query를 실행하고 값을 비교한다.

```sql
SELECT COUNT(*) AS members FROM member;
SELECT COUNT(*) AS questions FROM question;
SELECT COUNT(*) AS answers FROM answer;
SELECT COUNT(*) AS evaluations FROM evaluation;
SELECT COUNT(*) AS evidence FROM evaluation_evidence;

SELECT e.status, COUNT(*)
FROM evaluation e
GROUP BY e.status
ORDER BY e.status;

SELECT COUNT(*) AS broken_answer_evaluation
FROM evaluation e
LEFT JOIN answer a ON a.id = e.answer_id
WHERE a.id IS NULL;

SELECT COUNT(*) AS broken_evidence
FROM evaluation_evidence ee
LEFT JOIN evaluation e ON e.id = ee.evaluation_id
LEFT JOIN knowledge_chunk kc ON kc.id = ee.chunk_id
WHERE e.id IS NULL OR kc.id IS NULL;
```

성공 조건:

- 다섯 핵심 table count가 원본 snapshot과 일치
- evaluation 상태별 count 일치
- 끊어진 Answer→Evaluation, EvaluationEvidence→Evaluation/Chunk 참조 모두 0

## API 검증

복구 DB를 바라보는 격리 애플리케이션에서 수행한다.

```text
GET http://127.0.0.1:8081/actuator/health    → 200
GET /api/admin/questions                     → 대표 문제 조회
GET /api/admin/knowledge-documents           → 대표 문서·버전 조회
GET /api/admin/evaluations/{evaluationId}    → 답변·판정·과거 Evidence 조회
GET /api/members/me/answers                  → 대표 회원 답변 이력 조회
GET /api/answers/{answerId}/evaluation       → 회원 소유 평가 조회
```

대표 ID는 backup 전 manifest에 암호화되지 않은 내부 식별자만 기록한다. API 응답의 비밀번호 hash·session·provider key 비노출도 확인한다.

## 실패 처리

```text
backup 실패 → 불완전 archive 격리 → 재시도 → 연속 실패 시 운영 담당자 호출
restore 실패 → 대상 DB 사용 중지 → 로그·archive checksum 보존 → 새 빈 DB에서 재시도
검증 불일치 → 복구본 승격 금지 → 최초 불일치 table 식별 → 새 restore
```

- 실패한 복구 DB를 운영 트래픽에 연결하지 않음
- archive를 수정하거나 같은 DB 위에 부분 재실행하지 않음
- 원본 DB와 마지막 정상 backup을 삭제하지 않음
- RPO·RTO는 실제 production-equivalent 복구 시간을 측정한 뒤 확정
