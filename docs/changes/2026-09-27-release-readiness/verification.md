# 실행·정합성 검증

## 범위

- 브랜치: `feat/release-readiness`, 기준 `0276bc5`
- 추가 지출: 0원, 기존 Docker·Ollama 사용
- 실행 환경: Java 21.42.21, Node 24.21.0, PostgreSQL 17 Testcontainers
- 실행 계획: [plan.md](plan.md)

## 콘텐츠 버전 원자성 — 2026-09-27

- RED: 동시성 테스트 5개 모두 요구 동작 부재로 실패
  - Question 동시 생성: 같은 버전 번호 중복 저장
  - KnowledgeDocument 동시 생성: UNIQUE 충돌로 요청 실패
  - 두 콘텐츠 유형 동시 공개: 공개본 2개 잔존
  - Question Repository 직접 저장: 중복 버전 허용
- 환경·컴파일 오류는 RED 근거에서 제외
- 변경: 계열 ID scalar 조회 → 최초 버전 행 PESSIMISTIC_WRITE → 대상 조회·변경 → commit
- 생성·공개·폐기가 같은 계열 잠금 사용. 최초 행은 RETIRED여도 잠금 대상으로 유지
- DB 최종 방어: Question `(version_series_id, question_version)` UNIQUE
- 추가 회귀: 최초 버전 폐기 후 현재 공개본에서 동시 초안 8개 생성
- H2 전체: 484개 성공, 실패·오류·skip 0
- PostgreSQL: 44개 성공, 실패·오류·skip 0. 콘텐츠 동시성 7개 포함
- 실행: `./gradlew test postgresTest --console=plain`
- 로그: `build/reports/readiness/version-red.log`, `version-green.log`, `task1-suite.log`
- XML: `build/test-results/{test,postgresTest}/`
- 경계: 공개본에서만 다음 버전 생성 가능. 공개본이 없는 계열에 공개본 1개를 강제로 생성하는 계약 아님
- 잠금 범위: 서비스의 버전 생성·공개·폐기. 직접 SQL로 상태를 바꾸는 운영 도구는 같은 계약 준수 필요
