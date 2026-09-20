# CrackCS 구현 작업

## 현재 상태

| Phase | 상태 | 남은 핵심 |
|---|---|---|
| 0 개발 기반 | 부분 완료 | 공통 오류 계약, 실행 환경 문서, CI, health |
| 1 공개 문제 | 완료 | Phase 8 회귀만 남음 |
| 2 인증·권한 | 완료 | 운영 보안 점검과 분산 rate limit |
| 3 콘텐츠 운영 | 완료 | 동시 버전 생성·공개 경계 보강 |
| 4 답변·평가 골격 | 완료 | 실제 provider 장애 E2E |
| 5 Retrieval·평가 | 구현 기반 완료 | 실제 모델 비교와 출시 품질 실측 |
| 6 개인화 | 완료 | 운영 PostgreSQL 동시성 검증 |
| 7 후속 질문 | 완료 | 실제 provider·운영 흐름 검증 |
| 8 운영 안정화 | 미착수 | E2E, 보안, 장애, 관측, 성능, 복구, 파일럿 |

현재 우선순위:

```text
Phase 5 실제 모델 품질 실측
             ↓
Phase 0 운영 기반 부채 정리
             ↓
Phase 8 운영 검증과 제한 파일럿
```

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

검수 완료 원본과 자동 지표 계산은 준비됨. 실제 provider 호출이 필요한 항목만 미완료.

- [ ] 평가 모델 후보를 같은 `reference-v1` 입력으로 비교
- [ ] 모델·프롬프트·평가 규칙 버전 고정
- [ ] 전체 판정 일치율과 false-correct 비율 측정
- [ ] 근거 인용 유효성, schema 위반, timeout·429·5xx 기록
- [ ] 비용과 처리 시간 측정
- [ ] 출시 임계값과 실측 결과 기록
- [ ] `AC-002`, `AC-003`, `AC-007`의 실제 provider 경로 확인
- [ ] 골든 평가 세트 출시 품질 Gate 판정

완료 조건:

- 정답 라벨과 provider 입력 분리
- development로 조정한 뒤 evaluation-candidate로 최종 측정
- 자동 지표를 사람 검수 기준과 대조
- 실패 사례를 새 회귀 사례로 반영할 때 새 버전·재검수 적용
- 결과가 기준 미달이면 Phase 8 파일럿 진행 중단

### Phase 0 운영 기반 부채

#### 실행 환경

- [ ] `GET /api/health` 구현과 공개 범위 결정
- [ ] Java·Node·npm 최소 버전과 로컬 실행 명령을 루트 README에 기록
- [ ] 깨끗한 checkout에서 문서만으로 백엔드·프런트 실행 확인
- [ ] 프런트에서 백엔드 연결 방식 확정
- [ ] `local`, `test`, 운영 profile 책임 확인
- [ ] H2 개발 DB와 테스트 DB 격리 확인
- [ ] 운영 비밀정보 환경 변수 이름과 예시 제공
- [ ] API key·비밀번호·사용자 답변의 기본 로그 제외 확인
- [ ] 테스트가 개발 DB를 읽거나 변경하지 않는지 확인

#### 공통 HTTP 오류

- [ ] `code`, `message`, `fieldErrors`, `requestId` 계약 확정
- [ ] validation, not found, conflict, unexpected error 변환 통일
- [ ] 내부 예외 정보 비노출
- [ ] 대표 오류 응답 API 문서화
- [ ] `400`, `404`, `409`, `500` 계약 테스트

#### 프런트 API 경계

- [ ] 공통 API client와 오류 타입 확정
- [ ] loading, empty, validation, server error 처리 기준 통일
- [ ] 화면별 HTTP 오류 변환 중복 제거
- [ ] 인증·재시도 UI의 상태 전이와 실패 경계 테스트 보강

#### 자동화

- [ ] 백엔드 테스트 CI
- [ ] 프런트 type-check·production build CI
- [ ] 실패 로그와 dependency cache 정책 확인
- [ ] ADR 템플릿과 작성 기준 정리

### 구현 품질 보강

- [ ] Question·KnowledgeDocument 버전 생성과 공개 전환의 동시 요청 원자성
- [ ] 로그인 시도 제한의 다중 인스턴스 저장소와 시간 경계
- [ ] 운영 PostgreSQL에서 Knowledge State UNIQUE·낙관적 잠금 경쟁 검증

## Phase 8

### P8-T01 전체 E2E 회귀

- [ ] 관리자 Topic·Concept·문서·문제 공개 흐름
- [ ] 회원가입·로그인·문제 풀이·평가 결과 흐름
- [ ] Knowledge State·추천·후속 질문 흐름
- [ ] 다른 회원 데이터와 관리자 기능 접근 차단
- [ ] provider 실패·재시도 이후 화면 복구
- [ ] `AC-001`~`AC-007`과 자동 테스트 1:1 연결

### P8-T02 보안 점검

- [ ] 인증 우회와 수평 권한 상승
- [ ] 관리자 API 전체의 서버 인가
- [ ] 입력 길이, HTML 출력, script injection
- [ ] prompt injection 입력과 시스템 지침 경계
- [ ] session, API key, DB 비밀번호 노출
- [ ] rate limit 우회와 과도한 provider 호출
- [ ] 발견 사항, 위험도, 수정·수용 결과 기록

### P8-T03 장애와 데이터 정합성

- [ ] provider timeout, `429`, `5xx`
- [ ] 평가 처리 중 worker 종료
- [ ] 중복 작업과 중복 HTTP 요청
- [ ] 동시 Knowledge State 갱신
- [ ] 실패 후 Answer, Evaluation, Knowledge State 정합성
- [ ] 재시도 불가능 실패의 운영 처리 절차

### P8-T04 관측 가능성

- [ ] 모든 API 응답과 로그의 `requestId` 연결
- [ ] `memberId`, `answerId`, `evaluationId` 상관관계
- [ ] retrieval 시간, 후보 수, Evidence ID
- [ ] 모델·평가 규칙 버전, latency, 실패 코드
- [ ] 원문 답변과 비밀번호의 일반 로그 제외
- [ ] 실패율과 latency 확인용 dashboard 또는 query

### P8-T05 성능

- [ ] 일반 API p95 시나리오와 데이터 크기
- [ ] 평가 접수 응답 p95
- [ ] 평가 완료 p95
- [ ] 지식 지도·추천 query 수와 실행 시간
- [ ] N+1과 전체 이력 조회 점검
- [ ] 목표 미달 원인과 대응 계획

### P8-T06 백업·복구와 콘텐츠 rollback

- [ ] 운영 DB backup 주기와 보존 기간
- [ ] 빈 환경 restore
- [ ] 복구 데이터의 회원·문제·답변·평가 조회
- [ ] 잘못 공개한 문서 폐기와 이전 버전 복구
- [ ] 과거 EvaluationEvidence 조회 유지
- [ ] 절차와 담당 책임 기록

### P8-T07 초기 콘텐츠

- [ ] 초기 Topic·Concept 체계 확정
- [ ] Topic별 최소 문제·문서 수 `OQ-006` 확정
- [ ] 출처와 라이선스 검수
- [ ] Java 21, Spring Boot 4.1.x, Spring Framework 7.0.x, Jakarta Persistence 3.2 표시
- [ ] 문제별 필수 Concept와 reference answer 검수
- [ ] reference-v1과 실제 공개 문제의 편향·중복 점검

### P8-T08 제한 파일럿

- [ ] 대상과 기간
- [ ] 오판정 신고와 관리자 검토 절차
- [ ] 실패율, 처리 시간, 콘텐츠 부족률
- [ ] 추천 반복·막힘 사례
- [ ] 사용자 피드백과 운영 병목 우선순위
- [ ] P0 출시 여부와 P1 착수 조건

## 출시 전 공통 조건

### 기능과 데이터

- [ ] 제품 명세의 P0 기능 요구사항 구현 확인
- [ ] `AC-001`~`AC-007` 전체 통과
- [ ] 정상·빈 값·경계값·없는 ID 처리
- [ ] 상태 전이와 DB constraint 일치
- [ ] rollback, 중복 요청, 동시 요청 검증
- [ ] 과거 평가와 콘텐츠 버전 보존

### 보안과 소유권

- [ ] 비로그인 요청 차단
- [ ] USER의 ADMIN API 차단
- [ ] 회원 데이터 소유권 차단
- [ ] 비밀정보와 개인정보의 응답·로그 비노출
- [ ] 치명적·높은 우선순위 보안 결함 0건

### API와 화면

- [ ] 요청·응답·오류 계약 문서화
- [ ] loading, empty, success, error 상태
- [ ] 새로고침·네트워크 재시도 일관성
- [ ] 접근 불가 데이터의 프런트 응답 비포함

### 운영 Gate

- [ ] 실제 모델 평가 품질 기준 충족
- [ ] 관리자의 실패 추적 가능
- [ ] backup·restore 실제 검증
- [ ] 성능 결과와 미달 대응 계획
- [ ] 백엔드 자동 테스트 성공
- [ ] 프런트 테스트·type-check·production build 성공
- [ ] 관련 제품 명세·ERD·ADR·문서 목록 최신 상태
- [ ] 코드 작성 문제, 사용자 문제 게시, 결제 기능의 P0 제외 유지

## 상태 갱신 규칙

- 구현 완료: 코드와 자동 테스트 근거 확인 후 표시
- 계획: 아직 실행하지 않은 항목
- 미확정: 확인할 질문과 다음 행동 함께 기록
- Phase 완료: Gate 전체 충족 후 표시
- 세부 과정: 이 파일에 누적하지 않고 최종 검증 문서 또는 Git 이력에 보존
