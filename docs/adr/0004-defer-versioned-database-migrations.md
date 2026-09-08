# ADR-0004: 버전 기반 DB migration 도구 도입 보류

- 상태: 승인
- 결정일: 2026-08-31
- 관련 항목: DB schema 관리

## 배경

현재 CrackCS는 local과 자동화 테스트에서 H2를 사용하며 보존할 공유·운영 DB가 없다. PostgreSQL을 운영 DB 후보로 선택했지만, 배포 절차가 확정되지 않은 단계에서 migration 이력을 유지하면 빠른 schema 변경과 중복 관리 비용이 생긴다.

## 결정

Flyway 의존성과 version migration SQL을 제거하고 profile별로 다음 책임을 적용한다.

```text
기본 profile → 외부 schema를 Hibernate validate
local        → Hibernate update + local SQL seed
test         → Hibernate create-drop + Java fixture
```

- local seed는 API와 Vue 화면을 빠르게 확인하기 위한 데이터만 포함한다.
- 자동화 테스트는 local seed에 의존하지 않는다.
- 운영 환경에서 Hibernate `update`를 사용하지 않는다.

## 결과와 한계

- 초기 개발 중 엔티티 변경을 H2 migration 파일에 반복 반영하는 비용이 사라진다.
- local 빈 DB는 엔티티 mapping으로 바로 생성할 수 있다.
- schema 변경 이력, 배포 순서와 rollback은 현재 자동화되지 않는다.
- 최초 persistent staging 또는 운영 DB를 만들기 전에 실제 DB에 맞는 version migration 또는 동등한 schema 배포 절차를 다시 결정해야 한다.
- H2 테스트 통과는 향후 운영 DB의 SQL, constraint와 동시성 동작을 보장하지 않는다.
