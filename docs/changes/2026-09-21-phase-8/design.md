# Phase 8 운영 안정화 설계

상태: 구현 전 승인 설계

운영 경계·평가 관측 부분은 [2026-09-22 운영 개선 설계](../2026-09-22-operability/design.md)로 대체됨. 이 문서는 당시 결정 이력으로 보존.

## 목표

- 제한된 실제 사용자가 안전하게 사용할 수 있는 운영 검증 기반 확보
- `AC-001`~`AC-007`과 출시 전 공통 조건을 자동 테스트·실측·운영 절차에 연결
- 코드로 검증 가능한 완료와 외부 환경에서만 검증 가능한 완료를 분리
- 증거 없는 체크박스 완료 처리 방지

## 현재 현상

- Phase 1~7 기능 기반 구현
- Phase 5 실제 provider 품질 Gate 미실행
- Phase 0의 health, 공통 오류 계약, 실행 문서, CI 미완료
- Phase 8의 E2E, 보안, 장애, 관측, 성능, 복구, 콘텐츠, 파일럿 미착수
- 운영 DB와 실제 파일럿 참여자 없이 검증할 수 없는 항목 존재

## 범위

### 이번 작업에서 구현·검증할 범위

- Phase 0 운영 기반 부채 중 Phase 8 검증에 필요한 항목
- `GET /api/health`와 공개 범위
- 공통 HTTP 오류의 `code`, `message`, `fieldErrors`, `requestId` 계약
- API 요청과 서버 로그를 연결하는 request ID
- 민감정보를 일반 로그에서 제외하는 구조
- 백엔드·프런트 자동 검증 CI
- AC-001~AC-007 자동 테스트 대응표와 누락 회귀 테스트
- 인증·소유권·관리자 인가·입력 경계의 자동 보안 회귀
- provider timeout, 429, 5xx와 중복 처리·정합성 회귀
- 성능 시나리오·측정 도구와 결과 기록 형식
- PostgreSQL 백업·복구와 콘텐츠 rollback 실행 절차
- 초기 콘텐츠 품질·버전·라이선스 점검 기준
- 제한 파일럿 실행·중단·판정 절차

### 이번 작업에서 제외할 범위

- 실제 OpenAI provider 호출, 품질·비용·p95 측정

실제 OpenAI를 사용하지 않아도 timeout, `429`, `5xx`, schema 오류와 재시도 경계는 통제 가능한 test provider로 검증한다. 실제 모델 품질 Gate와 관련 체크박스는 미완료로 유지한다.

### 외부 실행 증거가 있어야 완료할 범위

- 운영과 동등한 PostgreSQL의 backup·restore 실연
- 운영 크기 데이터의 API·평가 p95 실측
- 실제 사용자를 대상으로 한 제한 파일럿
- 파일럿 피드백과 P0 출시 판정

위 항목은 도구와 절차가 준비되어도 실제 실행 결과가 없으면 체크하지 않는다.

## 단계와 의존성

```text
Phase 5 실제 모델 Gate
        │ 이번 작업 제외
        ▼
Phase 0 운영 기반
health · 오류 계약 · requestId · CI
        ▼
Phase 8 자동 회귀 기반
E2E · 보안 · 장애/정합성
        ▼
운영 검증 도구
관측 · 성능 · backup/restore
        ▼
초기 콘텐츠 검수
        ▼
제한 파일럿과 출시 판정
```

실제 OpenAI Gate가 미완료여도 다음 단계의 코드·테스트·절차 준비는 가능하다. 실제 파일럿 시작과 전체 Phase 8 완료 선언은 해당 Gate 통과 뒤에만 가능하다.

## 구성 요소와 책임

### HTTP 운영 경계

- `HealthController`: 프로세스 생존 확인만 소유
- request ID filter: 요청 ID 검증·생성, 응답 헤더와 MDC 수명주기 소유
- `GlobalExceptionHandler`: 애플리케이션 예외를 공통 오류 계약으로 변환
- Security 설정: health 공개와 나머지 인증·인가 경계 소유

```text
HTTP 요청
   ↓
request ID filter ── MDC(requestId)
   ↓                    ↓
Security → Controller → Service 로그
   ↓
응답 X-Request-Id + 오류 body.requestId
```

필터가 상관관계 상태를 소유하는 이유: 요청 시작과 종료를 모두 알고 있어 MDC 정리를 보장할 수 있는 유일한 경계이기 때문이다.

### 도메인·서비스 정합성

- Answer, Evaluation, Knowledge State: 각 불변식과 상태 전이를 소유
- Service: 저장 순서와 트랜잭션 조정
- Worker: lease 획득과 재시도 조정
- Repository/DB constraint: 중복·동시 요청의 최종 방어

```text
중복 HTTP 요청 ── idempotency key ── Answer UNIQUE
                                      ↓
                               Evaluation 1건
                                      ↓ 성공만
                              Knowledge State 1회
```

### 검증 계층

- 도메인 단위 테스트: 상태 전이와 부분 수정 방지
- Service 통합 테스트: 실제 Repository와 트랜잭션 결과
- MVC/Security 테스트: 오류 JSON, 헤더, 인증·인가·소유권
- API 인수 테스트: AC-001~AC-007의 서버 전체 흐름
- 프런트 테스트: loading, empty, success, error, retry 화면 상태
- 성능 harness: 고정 데이터 크기와 p95 산출
- 운영 runbook: backup, restore, rollback, 실패 처리

브라우저 E2E는 프런트와 서버를 함께 실행하는 별도 테스트 층으로 둔다. 단위·MVC 테스트를 브라우저 E2E로 중복하지 않고 핵심 사용자 흐름만 검증한다.

## 작업 순서

### 1. 운영 기반

- RED: health 공개 범위, request ID 전파, 오류 계약 테스트
- GREEN: 최소 controller, filter, 오류 응답 구현
- REFACTOR: 오류 생성과 request ID 조회 책임 정리
- 실행 환경·profile·비밀정보 문서 갱신
- 백엔드·프런트 CI 추가

### 2. 인수·보안 회귀

- AC-001~AC-007 기존 테스트 대응표 작성
- 자동 증거 없는 시나리오만 최소 회귀 테스트 추가
- 관리자 API 전체와 회원 소유 데이터의 거부 경계 검증
- 입력 길이·HTML/script 문자열·prompt injection 데이터 구획 검증

### 3. 장애·정합성

- provider timeout, 429, 5xx의 최종 상태와 retry 가능성 검증
- worker lease 만료 후 재수령 검증
- 중복 요청·중복 worker·동시 Knowledge State 갱신 검증
- 실패 시 Answer 보존, Evaluation 실패, Knowledge State 불변 검증

### 4. 관측·성능

- 로그 상관 키와 민감정보 제외 규칙 적용
- retrieval 후보 수·시간, 모델·규칙 버전·latency·실패 코드 기록
- 실패율·latency 조회 방법 문서화
- 고정 데이터 크기의 일반 API·평가 처리 성능 harness 추가
- N+1과 전체 이력 조회를 query 수 또는 실행 계획으로 점검

### 5. 복구·콘텐츠·파일럿

- PostgreSQL backup·빈 환경 restore·핵심 조회 검증 runbook
- 문서 폐기와 이전 버전 사용, 과거 Evidence 유지 절차
- 초기 Topic별 공개 문제 5개 이상 기준과 라이선스 검수표
- 기술 버전 표기·reference answer 검수·중복 점검 절차
- 파일럿 대상·기간·신고·지표·중단 조건·P0 판정 템플릿

### 6. 문서 상태 갱신

- 새 실행 결과만 `verification.md`에 기록
- 코드·테스트 증거가 있는 항목만 `tasks.md`에서 완료 처리
- 외부 실행 대기 항목은 미확정 이유와 다음 행동 유지
- `docs/README.md`의 Phase 8 상태와 링크 갱신

## 실패 처리

- 자동 테스트 실패: 관련 항목 미완료 유지
- 실제 모델 품질 기준 미달: 파일럿 중단
- 치명적·높은 보안 결함: 파일럿 중단
- backup restore 실패: 운영 배포와 파일럿 중단
- p95 목표 미달: 원인과 대응 계획 없이는 완료 처리 금지
- 데이터 정합성 위반: 해당 상태를 소유한 도메인·DB 제약부터 수정

## 완료 판정

### 로컬 구현 완료

- 백엔드 전체 테스트 통과
- PostgreSQL 선택 테스트는 Docker 가능 환경에서 통과하거나 미검증 경계로 명시
- 프런트 테스트, type-check, production build 통과
- AC-001~AC-007 자동 증거 연결
- 보안·장애·관측·성능·복구 절차 문서와 실행 명령 유효

### Phase 8 완료

- 실제 provider 품질 Gate 통과. 이번 작업 범위에서는 미검증 유지
- 치명적·높은 보안·정합성 결함 0건
- 운영 동등 환경의 backup·restore 성공
- 운영 데이터 크기의 성능 결과와 미달 대응 기록
- 초기 콘텐츠 기준 충족
- 제한 파일럿 결과와 P0 출시 판정 기록

## 제약

- Production 코드보다 실패 테스트 먼저 작성
- Java 운영·테스트 코드에서 `var` 금지
- Service는 인터페이스와 `Default...` 구현 구조 유지
- 테스트가 만든 DB 데이터는 해당 테스트가 `@AfterEach`에서 정리
- 관련 없는 사용자 변경 복원·정리 금지
- `docs/retrospectives/` staging 제외
- 이번 작업에서 Git commit 생성 금지
