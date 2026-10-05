# ADR-0006: 로컬 PostgreSQL의 명시적 schema와 복구 경로

- 상태: 승인, 로컬 검증 범위
- 결정일: 2026-09-27
- 대체: [ADR-0004](0004-defer-versioned-database-migrations.md)의 persistent 로컬 DB 생성·변경 절차 미정 부분

## 결정 이유

- 추가 비용 없는 실제 PostgreSQL 동작·복구 증거 필요
- 보존할 데이터에 Hibernate `update` 사용 방지
- 빈 환경 재현과 이후 변경의 구분 필요

## 결정

- `db/postgres/V001__baseline.sql`: PostgreSQL 17에서 추출·검증한 최초 schema
- Docker 새 volume에 한 번 적용. `schema_version`으로 적용 버전 기록
- `local,local-postgres`: 앱 시작 시 `ddl-auto=validate`, seed 자동 재실행 금지
- V002: 로그인 실패 aggregate·순서가 있는 실패 시각 목록·만료 조회 인덱스 추가. 기존 테이블 변경 없음
- V003: 회원 `authentication_version` 추가, 상태 변경 뒤 기존 세션 회수. 기존 행 기본값 0
- V004: 공유 인증 요청 예산 추가·기존 조합별 임시 로그인 실패 기록 초기화. 계정·회원 데이터 유지
- 변경: 기존 baseline 수정 대신 새 버전 SQL + 적용 transaction + 버전 기록
- 적용 전 backup·새 DB restore·회귀 리허설
- rollback: 이전 앱 + 검증한 복구 DB. 데이터를 잃는 역방향 DDL 자동 실행 금지
- Flyway 등 migration runner는 여전히 미도입. 새 SQL의 적용 순서·checksum 검토는 작업자 책임

## 범위와 한계

- 개인 PC PostgreSQL에 두 JVM을 연결한 제한 공유·학습 상태 동시 갱신 검증. 운영 규모·공유 session 검증 아님
- 데모 비밀번호는 공개 값. 외부 접근 용도 사용 금지
- local H2의 `update`와 테스트 DB의 `create-drop` 정책 유지
- 운영 도입 전: migration runner, 권한 분리, 외부 backup, RPO/RTO 재결정
- 실행 기준: [로컬 실행·복구](../changes/2026-09-27-release-readiness/local-runbook.md)
