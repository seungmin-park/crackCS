# CrackCS 구현 작업

## 현재 상태

현재 제출: 무료 기본 실행과 실제 GPT 선택 실행. [최신 검증](../changes/2026-09-29-openai-completion/verification.md)에서 품질 후보·PostgreSQL 브라우저 흐름 확인. 유료 시연은 OpenAI 프로젝트의 월 $2 강제 한도 아래 수행.

| 범위 | 상태 | 남은 경계 |
|---|---|---|
| 개발 기반·인증·콘텐츠 운영·답변·개인화·후속 흐름 | 구현·로컬 회귀 완료 | 공개 운영 규모·인프라 별도 |
| Retrieval·평가 처리 골격 | 실제 GPT 후보 42/42 호출·지표 목표 통과, 앱 흐름 확인 | 대표 사용자 표본·공개 운영 품질 인증 별도 |
| 운영 안정화 | 로컬 시연 범위 검증 완료 | 실제 참가자 파일럿·공개 출시 미실행 |
| initial-v1 | 초안·출처·검수 자료 준비 완료 | 실제 사람 검수·공개 승인 대기 |

로컬 시연과 실제 학습 서비스 출시 구분:

```text
코드·DB·화면·실제 GPT 후보 검증·시연 영상 → 현재 로컬 시연 범위
사람 콘텐츠 승인 + 대표 표본 재검증 → 실제 참가자 파일럿 → 공개 출시 판단
```

미체크 항목은 실제 provider 장애·콘텐츠 사람 승인·파일럿·공개 운영 조건. 로컬 검증으로 해당 사실을 대체하지 않음.

제품 요구의 단일 기준은 [제품 명세](../product/spec.md), 구현 순서와 의존성은 [개발 계획](plan.md), 평가 정답과 실행법은 [reference-v1](../evaluation/reference-v1/README.md) 참조.

## 완료 Phase 요약

| Phase | 결과 | 검증 근거 |
|---|---|---|
| 1 | 공개 문제 목록·상세, 공개 상태 필터, 내부 평가 필드 차단 | 제품 명세와 자동 테스트 |
| 2 | 세션 인증, 회원가입·로그인·로그아웃, USER·ADMIN 경계 | [ADR-0001](../adr/0001-session-based-authentication.md), [ADR-0002](../adr/0002-password-policy.md), [ADR-0003](../adr/0003-authentication-security-baseline.md) |
| 3 | Topic·Concept·문서·문제의 관리자 등록·검수·공개·폐기 | 제품 명세와 자동 테스트 |
| 4 | 답변 저장, 평가 상태, 멱등 접수, 이력·상세 화면 | [Phase 4 검증](../changes/2026-09-07-phase-4/verification.md) |
| 5 구현 기반 | Chunk, retrieval, 구조화 평가, worker, 근거 저장, 관리자 실패 조회 | [Phase 5 검증](../changes/2026-09-08-phase-5/verification.md), [ADR-0005](../adr/0005-phase-5-evaluation-runtime.md) |
| 6 | Knowledge State, 개인 추천, 학습 현황·지도, 동시 반영 방어 | [Phase 6 검증](../changes/2026-09-13-phase-6/verification.md) |
| 7 | 후속 질문 생성·조회·답변·재평가, 다음 기본 문제 연결 | 제품 명세와 관련 자동 테스트 |

완료 Phase의 상세 체크리스트는 반복하지 않는다. 현재 계약은 코드·테스트·제품 명세, 당시 핵심 증거는 `docs/changes/`가 소유한다.

## 다음 작업

### Phase 5 실제 모델 품질 Gate

**로컬 시연 범위의 오프라인 후보 검증 완료.** 강제 월 $2 한도 아래 [실측 결과](../changes/2026-09-29-openai-completion/verification.md)를 기록. 이전 로컬 모델의 미달 결과는 역사적 비교 기준으로 유지.

GPT-5.6 Terra의 개발 138건과 후보 provider 대상 42건을 측정. 이전 로컬 모델 동일 12사례는 [비교 기록](../changes/2026-09-27-release-readiness/verification.md)에 보존. `INSUFFICIENT_EVIDENCE` 14건은 모델 호출 전 차단 경로로, 후보 provider 분모에 포함하지 않음. 후보는 공개 서비스의 독립·대표 표본이 아님.

- [x] 평가 지시문 `v1`·`v2`·`v3`를 개발 사례에서 비교, `v3` 채택
- [x] GPT-5.6 Terra·`os-evaluator-v3`·`reference-v1 1.0.0` 고정
- [x] 후보 provider 42건 판정 일치율·false-correct 측정
- [x] 후보 42건 근거·schema·timeout·429·5xx 관측 기록. 실제 장애 응답은 발생하지 않아 계약 테스트로 별도 검증
- [x] 추정 비용·실제 호출 시간 측정
- [x] 제품 명세 목표와 후보 실측 비교, 오프라인 후보 통과 기록
- [x] `AC-002`의 실제 GPT 정상 평가·근거·학습 상태 확인
- [ ] `AC-003` 실제 provider 장애·`AC-007` GPT 평가 뒤 콘텐츠 교체 경로 확인 — 계약·통합 회귀만 완료
- [x] 골든 세트의 호출 대상 후보 42건 오프라인 품질 Gate 판정
- [ ] 독립 대표 답변과 실제 provider 장애·콘텐츠 교체 경로를 포함한 공개 출시 Gate 판정

완료 조건:

- 정답 라벨과 provider 입력 분리
- development로 조정한 뒤 evaluation-candidate로 최종 측정
- 자동 지표를 사람 검수 기준과 대조
- 실패 사례를 새 회귀 사례로 반영할 때 새 버전·재검수 적용
- 대표 표본·사람 콘텐츠 승인 전 파일럿 진행 보류

### Phase 0 운영 기반 부채

#### 실행 환경

- [x] Actuator health·Prometheus 관리 포트와 공개 범위 결정
- [x] Java·Node·npm 최소 버전과 로컬 실행 명령을 루트 README에 기록
- [x] 깨끗한 checkout에서 문서만으로 백엔드·프런트 실행 확인 — `93b6cbd` 새 checkout·npm ci·PostgreSQL validate·브라우저 로그인
- [x] 프런트에서 백엔드 연결 방식 확정 — Vite `/api` proxy, 실제 PostgreSQL 앱 연결 검증
- [x] `local`, `test`, 운영 profile 책임 확인
- [x] H2 개발 DB와 테스트 DB 격리 확인
- [x] 운영 비밀정보 환경 변수 이름과 예시 제공
- [x] API key·비밀번호·사용자 답변의 기본 로그 제외 확인
- [x] 테스트가 개발 DB를 읽거나 변경하지 않는지 확인

#### 공통 HTTP 오류

- [x] `code`, `message`, `fieldErrors`, `requestId` 계약 확정
- [x] validation, not found, conflict, unexpected error 변환 통일
- [x] 내부 예외 정보 비노출
- [x] 대표 오류 응답 API 문서화
- [x] `400`, `404`, `409`, `500` 계약 테스트

#### 프런트 API 경계

- [x] 공통 API client와 오류 타입 확정
- [x] loading, empty, validation, server error 처리 기준 통일 — [자동·cmux 검증](../changes/2026-09-27-ui-content/verification.md)
- [x] 화면별 HTTP 오류 변환 중복 제거 — 공통 presentation, 인증·제출 정책 분리
- [x] 인증·재시도 UI의 상태 전이와 실패 경계 테스트 보강 — 답변 로딩·연속 연결 실패 시 원문 보존 회귀 추가

#### 자동화

- [x] 백엔드 테스트 CI
- [x] 프런트 type-check·production build CI
- [x] 실패 로그와 dependency cache 정책 확인
- [x] ADR 템플릿과 작성 기준 정리 — 문서 지도의 결정 기록 섹션

### 구현 품질 보강

- [x] Question·KnowledgeDocument 버전 생성과 공개 전환의 동시 요청 원자성 — [H2·PostgreSQL 검증](../changes/2026-09-27-release-readiness/verification.md)
- [x] 로그인 시도 제한의 DB 공유 — H2·PostgreSQL 및 두 JVM 5건 동시 실패·429 검증
- [x] 로그인 시도 제한의 차단 만료 경계 — 만료 뒤 실패 기록 NPE 재현·수정
- [x] 격리 PostgreSQL 17에서 Knowledge State UNIQUE·낙관적 잠금 경쟁 검증 — `KnowledgeCompletionTest`
- [x] 두 로컬 앱에서 Knowledge State 경쟁 재검증 — 동시 10개 답변·적용 기록 10개·누적 10회
- [ ] 실제 운영 규모의 Knowledge State 부하 검증 — 공개 운영 후속 범위

## Phase 8

### P8-T01 전체 E2E 회귀

- [x] 관리자 Topic·Concept·문서·문제 공개 흐름 — PostgreSQL 복구 DB·cmux 실제 브라우저
- [x] 회원가입·로그인·문제 풀이·평가 결과 흐름 — cmux 브라우저, PostgreSQL·stub 평가
- [x] Knowledge State·추천·후속 질문 흐름 — cmux 브라우저, 미평가→학습 중·후속 답변 확인
- [x] 다른 회원 데이터와 관리자 기능 접근 차단 — 별도 회원의 답변 URL·실제 API·관리자 URL 거절
- [x] 모의 provider 실패·재시도 이후 화면 복구 — 같은 답변의 2번째 시도 완료, 재로그인·원래 경로 복귀. 실제 GPT 정상 경로는 [별도 검증](../changes/2026-09-29-openai-completion/verification.md)
- [x] `AC-001`~`AC-007`과 자동 테스트 1:1 연결 — AC-007 문서 교체 뒤 과거 Evidence 단일 회귀 포함

### P8-T02 보안 점검

- [x] 인증 우회와 수평 권한 상승
- [x] 관리자 API 전체의 서버 인가
- [x] 입력 길이, HTML 출력, script injection
- [x] prompt injection 입력과 시스템 지침 경계
- [x] session, API key, DB 비밀번호 노출
- [x] 계정+주소 로그인 제한·회원별 접수 제한·중복 키의 제한 예외 — 두 JVM 검증. 분산 계정·주소 전역 제한은 공개 운영 전 과제
- [x] 발견 사항, 위험도, 수정·수용 결과 기록 — 최신 검증의 보안 점검표

### P8-T03 장애와 데이터 정합성

- [x] provider timeout, `429`, `5xx`
- [x] 평가 처리 중 worker 종료
- [x] 중복 작업과 중복 HTTP 요청
- [x] 동시 Knowledge State 갱신
- [x] 실패 후 Answer, Evaluation, Knowledge State 정합성
- [x] 재시도 불가능 실패의 운영 처리 절차

### P8-T04 관측 가능성

- [x] 모든 API 응답과 로그의 `requestId` 연결
- [x] `memberId`, `answerId`, `evaluationId` 상관관계
- [x] retrieval 시간, 후보 수, Evidence ID
- [x] 모델·평가 규칙 버전, latency, 실패 코드
- [x] 원문 답변과 비밀번호의 일반 로그 제외
- [x] 실패율과 latency 확인용 dashboard 또는 query

### P8-T05 성능

- [x] 일반 API p95 시나리오와 데이터 크기 — PostgreSQL·HTTP, 문제 100·답변 500, 동시 조회 4
- [x] 평가 접수 응답 p95 — 로컬 HTTP 8.6ms, 동시 접수 5, 실제 AI 완료 지연 제외
- [x] 평가 완료 p95
- [x] 지식 지도·추천 query 수와 실행 시간
- [x] N+1과 전체 이력 조회 점검
- [x] 목표 미달 원인과 대응 계획 — candidate 부분 정답 오판정·지연 조건·재측정 계획, 인과 미확정 경계 포함

### P8-T06 백업·복구와 콘텐츠 rollback

- [x] 운영 DB backup 주기와 보존 기간
- [x] 빈 환경 restore — Docker PostgreSQL의 새 DB로 복구
- [x] 복구 데이터의 회원·문제·답변·평가 조회 — 복구 앱 로그인·기존 답변/근거 확인
- [x] 잘못 공개한 문서 폐기와 이전 버전 복구
- [x] 과거 EvaluationEvidence 조회 유지
- [x] 절차와 담당 책임 기록

### P8-T07 초기 콘텐츠

- [x] 초기 Topic·Concept 체계 구성 — [5개 Topic·50개 Concept 초안](../content/initial-v1/README.md), 사람 검수·공개는 별도
- [x] Topic별 최소 문제·문서 수 `OQ-006` 확정
- [ ] 출처와 라이선스 검수 — 20개 출처 본문·링크·이용 메모 준비, 사람 검수 대기
- [x] Java 21, Spring Boot 4.1.x, Spring Framework 7.0.x, Jakarta Persistence 3.2 표시 — 등록 문서·번들 일치 검증
- [ ] 문제별 필수 Concept와 reference answer 검수 — 25문항·각 필수 개념 2개와 답안 준비, 사람 검수 대기
- [ ] reference-v1과 실제 공개 문제의 편향·중복 점검 — 초안의 완전 중복 0·공유 개념 7항목·분포 한계 기록, 공개본 승인 전 재확인

### P8-T08 제한 파일럿

[실행안·신고 절차·지표·판정 서식](../changes/2026-09-21-phase-8/pilot-runbook.md) 준비 완료. 아래는 실제 참가자 실행 결과이므로 미체크 유지. AI Gate 제외 결정에 따라 후속 범위.

- [ ] 대상과 기간
- [ ] 오판정 신고와 관리자 검토 절차
- [ ] 실패율, 처리 시간, 콘텐츠 부족률
- [ ] 추천 반복·막힘 사례
- [ ] 사용자 피드백과 운영 병목 우선순위
- [ ] P0 출시 여부와 P1 착수 조건

## 출시 전 공통 조건

### 기능과 데이터

- [x] 제품 명세의 P0 기능 요구사항 구현 확인 — 요구사항 대응표·전체 회귀, GPT 후보 실측 별도 기록
- [x] `AC-001`~`AC-007` 소프트웨어 동작 통과 — 모의 Port 포함. GPT 정상 평가·후속 정상 흐름 추가 확인, 실제 장애·콘텐츠 교체는 별도
- [x] 정상·빈 값·경계값·없는 ID 처리 — Controller·도메인·API 회귀
- [x] 상태 전이와 DB constraint 일치 — H2/PG·V002 validate
- [x] rollback, 중복 요청, 동시 요청 검증 — 통합 회귀·두 JVM 실측
- [x] 과거 평가와 콘텐츠 버전 보존 — H2·PostgreSQL 단일 통합 회귀

### 보안과 소유권

- [x] 비로그인 요청 차단 — `SecurityConfigurationTest.rejectsAnonymousQuestionRequest`
- [x] USER의 ADMIN API 차단 — 관리자 API 전체 계열 자동 회귀·실제 USER 화면 거절
- [x] 회원 데이터 소유권 차단 — 자동 API 회귀·별도 회원 브라우저·실제 API 거절
- [x] 비밀정보·타인 데이터의 비인가 응답 및 일반 로그 비노출 — 인증·소유권·로그 회귀. 본인 정보·관리자 권한 원문 조회는 정상 계약
- [x] 검토한 loopback 시연 범위의 Critical·Important 결함 0건 — 독립 검토 완료. 공개 운영·전역 남용 방어는 별도 조건

### API와 화면

- [x] 요청·응답·오류 계약 문서화 — OpenAPI·Controller 회귀
- [x] loading, empty, success, error 상태 — 학습·관리자 화면 자동 회귀·실제 400/empty 확인
- [x] 새로고침·네트워크 재시도 일관성 — 초기 인증 복구·입력 보존·자동/수동 재조회 분리
- [x] 접근 불가 데이터의 프런트 응답 비포함 — 내부 답안·타인 답변·관리자 API 차단

### 운영 Gate

- [x] 실제 GPT의 고정 오프라인 후보 지표 목표 충족 — 42건, 상세 97.62%, 이진 96.43%, false-correct 0/14. 대표 사용자·공개 출시 인증 아님
- [x] 관리자의 실패 추적 가능 — 실패 코드·원본 답변·평가 ID 조회
- [x] backup·restore 실제 검증 — 로컬 논리 복구 범위, 원격 재해 복구는 별도
- [x] 성능 결과와 미달 대응 계획 — 로컬 HTTP·GPT 후보 p95 10.859초, 이전 로컬 모델 미달·환경 차이 기록
- [x] 백엔드 자동 테스트 성공 — 최신 실행은 마무리 검증 기록 참조
- [x] 프런트 테스트·type-check·production build 성공 — [326개 성공](../changes/2026-09-27-ui-content/verification.md)
- [x] 관련 제품 명세·ERD·ADR·문서 목록 최신 상태 — 로그인 DB·후속 생성·시연 범위 반영
- [x] 코드 작성 문제, 사용자 문제 게시, 결제 기능의 P0 제외 유지

## 상태 갱신 규칙

- 구현 완료: 코드와 자동 테스트 근거 확인 후 표시
- 계획: 아직 실행하지 않은 항목
- 미확정: 확인할 질문과 다음 행동 함께 기록
- Phase 완료: Gate 전체 충족 후 표시
- 세부 과정: 이 파일에 누적하지 않고 최종 검증 문서 또는 Git 이력에 보존
