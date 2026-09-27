# 실행·검증 마무리 계획

> 후속 결정: 사용자 지시에 따라 실제 AI 연결·품질 Gate를 이번 제출 범위에서 제외. AI 외 남은 작업은 [서버·제출 마무리](../2026-09-27-service-completion/plan.md)로 이관. 이 문서는 이전 실행 범위의 이력으로 보존.

> 실행: `superpowers:executing-plans` 순차 진행. 동작 변경은 실패 테스트 → 최소 구현 → 회귀 검증.

**목표:** 기존 CS 학습 흐름의 정합성·재현성·실제 AI 검증 근거 완성.

**구조:** 기존 Spring 서비스·도메인·Repository 경계 유지. 문제·문서의 버전 계열별 DB 잠금으로 변경 순서 제어. 로컬 PostgreSQL과 기존 Ollama를 사용한 추가 과금 없는 검증.

**기술:** Java 21, Spring Boot 4.1.1, PostgreSQL 17, Vue/TypeScript, Node.js 24, Ollama `gpt-oss:20b`.

**기준:** [제품 명세](../../product/spec.md), [작업 목록](../../planning/tasks.md), 루트 `AGENTS.md`.

## 제약과 완료 판단

- 추가 지출 0원. 유료 호스팅·외부 모델 API 호출·유료 계정 생성 제외
- 기존 Mac·설치된 Docker·Ollama 사용
- 원격 `main`과 `feat/phase8`: `0276bc5`까지 반영. 이후 변경은 `feat/release-readiness`에서 진행
- 로컬 E2E: 호출 cmux `workspace:1`, 검증 터미널 `surface:14`. 사용자 터미널 `surface:1` 사용 금지
- 테스트용 DB와 기존 local 파일 DB 분리
- 사용자 기존 회고·에이전트 파일 보존, 커밋 제외
- 자동 검증·실제 모델 품질·사람 검수·사용자 파일럿을 각각 판정. 실행하지 않은 항목은 미체크 유지
- 로컬 모델의 20초 p95는 기존 미달 상태. 코드 수정만으로 달성한다고 약속하지 않음

## 검토할 실패 경계

1. 같은 버전 계열에서 서로 다른 원본 버전을 통한 동시 생성
2. 두 검수본의 동시 공개 후 공개본 하나 유지
3. 폐기된 문서의 과거 평가 근거 보존
4. 긴 평가 중 새로고침·네트워크 오류 후 원문과 결과 복원
5. 실행·복구 명령이 기존 사용자 DB를 덮어쓰는 사고 방지

## Task 1: 버전 생성·공개 원자성

**파일:** `content/question` 및 `content/knowledge`의 Service·Repository·domain, 각각의 Service 통합 테스트, `build.gradle`.

**계약:** 기존 Service 인터페이스 유지. 버전 계열의 최초 행을 DB 잠금 기준으로 사용. 생성·수정·개념 교체·검수·공개·폐기가 같은 계열 잠금 사용. `Question`에 계열+버전 UNIQUE 최종 방어 추가.

- [x] 실제 Repository를 이용한 동시 생성 테스트 추가. 현재 공개본에서 버전 번호 중복 금지. 최초 버전 폐기 후에도 검증
- [x] 동시 공개 테스트 추가. 완료 후 계열별 공개본 1개, 이전 내용 보존
- [x] 현재 코드에서 요구 동작 부재로 실패하는 RED 확인
- [x] Repository의 scalar 계열 조회 후 최초 행 잠금. 잠금 이후 대상과 최대 버전 재조회
- [x] H2 관련 테스트 → PostgreSQL 동시성 테스트 → 전체 테스트
- [x] 결과를 작업 목록과 검증 기록에 반영 후 커밋

## Task 2: 0원 실행 구성과 콘텐츠

**파일:** local seed, 실행 관련 설정·스크립트, README, DB 정책 ADR.

**계약:** 기존 로컬 DB 파괴 없이 별도 검증 DB 사용. 공개 문제·필수 Concept·검색 가능한 근거가 연결된 학습 흐름.

- [x] 기존 콘텐츠·Chunk 공개 조건 확인 후 최소 콘텐츠 구성
- [x] 새 환경에서 backend/frontend 실행과 `/api` 연결 확인
- [x] 로컬 PostgreSQL의 명시적 schema 생성·변경·backup/restore 경로 구성 및 실제 검증
- [x] 유료 API 비활성, Ollama 연결·후속 질문 생성 범위 명시
- [x] 실행 명령·미검증 경계 반영 후 커밋

## Task 3: 전체 흐름·실패·성능 회귀

**파일:** 통합·Controller·프런트 테스트, 발견된 동작의 운영 코드, `acceptance-matrix.md`.

**계약:** AC-001~007의 자동 증거와 cmux 실제 브라우저 증거 분리.

- [x] 기존 API·Service 증거와 미체크 항목 대조
- [x] AC-007 콘텐츠 교체·폐기 이후 과거 Evidence 조회 단일 통합 테스트
- [x] PostgreSQL Knowledge State 충돌·중복 반영 검증
- [ ] 인증·관리자·소유권·긴 대기·실패 복구 E2E, 실패 발견 시 RED부터 수정
- [x] HTTP 처리 시간과 데이터 규모·동시성 조건 기록. 실제 AI 지연과 통제 provider 부하 분리
- [x] 전체 backend/frontend 테스트·type-check·build 후 근거 기록 및 커밋

## Task 4: 실제 모델 평가와 최종 문서 정합성

**파일:** `reference-v1` 실행 도구·기존 모델 측정 task, 검증 기록, 작업 목록·문서 지도.

**계약:** development 조정과 evaluation-candidate 최종 판정 분리. 오류·미응답을 누락하지 않는 보고.

- [x] 비용 0의 기존 모델·prompt 버전 고정, 실행 시간·메모리 조건 확인
- [x] candidate provider 첫 12건 실측·미달 판정 기록. 전체 품질 Gate는 미통과 유지
- [x] `tasks.md` 미체크 항목에 완료 근거 또는 아래 범위·검증 기록의 남은 조건 연결
- [x] 사람 검수·실제 참가자 파일럿은 자동 실행으로 대체하지 않고 미실행 유지
- [x] 변경 전체 독립 코드 리뷰와 중요 결함 RED→GREEN 수정 — `93b6cbd`, H2 492·PostgreSQL 56·프런트 298 성공
- [x] 결과·실패 조건·실행법·남은 조건 정리. 실제 provider 성공 복구는 미완료 유지

## 미체크 항목 처리 범위

| 기존 작업 | 처리 |
|---|---|
| Phase 5 실제 모델 Gate 전체 | Task 4. 0원 모델만 사용, 목표 미달이면 미완료 |
| Phase 0 실행 환경·API 경계·ADR | Task 2~3. 코드·실행 근거로 판정 |
| 화면별 공통 오류·loading·empty·validation 통일 | 답변 상세만 보강. 전체 화면 정리는 별도 구현·회귀 필요 |
| 콘텐츠 버전 동시성 | Task 1 |
| 로그인 다중 인스턴스 저장소 | 배포 구조 결정 후 판정. 단일 프로세스 검증으로 완료 처리 금지 |
| PostgreSQL Knowledge State 경쟁 | Task 3 |
| P8-T01 E2E·AC 연결 | Task 3 |
| P8-T02 rate limit·provider 호출 제한 | Task 3. 무제한 호출·우회 경계 확인 |
| P8-T05 성능 미완료 | Task 3~4. H2 Service 시간을 HTTP·실제 AI p95로 대체 금지 |
| P8-T06 restore·데이터 조회 | Task 2 |
| P8-T07 초기 콘텐츠·출처·검수 | Task 2. 사람 검수와 코드 준비 분리 |
| P8-T08 실제 파일럿 | 참가자·기간·실제 결과가 필요한 외부 단계. 미실행 유지 |
| 출시 전 공통 조건 | Task 3~4 결과로 항목별 판정. 일괄 체크 금지 |
| 전체 보안 결함 0·실제 모델 성공 복구·전 화면 일관성 | 현재 증거만으로 전체 통과 불가. 남은 검증 범위를 유지 |
