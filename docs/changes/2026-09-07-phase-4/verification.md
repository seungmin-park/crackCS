# Phase 4 답변·평가 검증

- 대상: P4-T01~T07, Phase 4 Gate
- 독자·질문: 개발자·리뷰어 — 어떤 계약과 책임으로 구현했고 무엇을 검증했는가?
- 기준: [작업 목록](../../planning/tasks.md), [제품 명세](../../product/spec.md), [OpenAPI](../../../openapi.yml)
- 상태: 구현 완료·Phase 4 Gate 통과 (2026-09-07)

## 결정과 동작

```text
USER + Idempotency-Key + 답변 원문
  ↓ 회원 행 잠금, 동일 키·payload 확인
Answer + Evaluation(EVALUATING) 저장·커밋
  ↓ DB 대기 행 주기 탐색 (기본 1초, 최대 20건)
평가 행 잠금 → Port 호출 (최대 3회)
  ├─ 유효 결과 → EVALUATED + 전체·개념별 verdict/score
  └─ 최종 오류 → FAILED + 안전한 진단 코드
```

- Answer 책임: 활성 USER·공개 문제·유효 원문·표준 UUID 검증, 제출 사실 보존
- Evaluation 책임: 상태 전이·Concept 완전성·판정 점수·부분 수정 방지
- AnswerService 책임: 회원별 멱등 처리와 제출 트랜잭션, 소유권 조회
- EvaluationProcessor 책임: 제출과 분리된 트랜잭션, 잠금·제한 재시도·최종 확정
- Worker 책임: DB 대기 행 재발견; 서버 중단으로 미커밋 작업은 다음 실행에서 재처리
- 화면 책임: 미확정 제출의 키·원문 보존, 같은 payload 재전송, 페이지·평가 상태 복구

## 실행 설정

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew bootRun --args='--spring.profiles.active=local --crackcs.evaluation.stub-outcome=NEEDS_REVIEW'
./gradlew bootRun --args='--spring.profiles.active=local --crackcs.evaluation.stub-outcome=TIMEOUT'
```

- Stub 결과 설정: CORRECT(기본), PARTIALLY_CORRECT, INCORRECT, NEEDS_REVIEW, TIMEOUT, FAILURE
- Stub·Worker 활성 조건: `(local | test) & !prod & !production`
- 기본·prod·production·혼합 운영 profile: Stub 미등록
- `crackcs.evaluation.worker-enabled=false`: 자동 Worker 정지, 통합 테스트의 직접 처리 검증에 사용
- `crackcs.evaluation.poll-delay`: DB 탐색 주기(ms), 기본 1000
- TIMEOUT: 실제 네트워크 지연이 아닌 시간 초과 예외의 재현
- 진단 코드: PROVIDER_TIMEOUT, PROVIDER_ERROR, INVALID_RESULT, PROVIDER_UNAVAILABLE

## 관찰한 RED → GREEN

| 요구·문제 | 실제 실패 증거 | 해결·검증 |
|---|---|---|
| 답변 제출 경로 부재 | 202 예상, 500 반환 | Controller·Service·저장 모델 연결 |
| 평가 처리 부재 | EVALUATED 예상, EVALUATING 유지 | 별도 트랜잭션 Processor |
| 실패·재시도·중복 worker 미구현 | Service 7개 중 5개 실패 | 최대 3회 재시도·행 잠금 |
| 자동 실행 미연결 | 평가 커밋 신호 미발생 | DB 주기 탐색 Worker |
| 모순된 정답·빈 피드백 허용 | 도메인 12개 중 2개 실패 | 입력 전체 검증 강화 |
| Concept 참조 미보호 | 평가된 Concept 삭제 성공 | Concept FK 연결 |
| 시간 초과 구분 부재 | PROVIDER_TIMEOUT 예상, PROVIDER_ERROR | 전용 예외·안전한 코드 |
| 명세 헤더 계약 불일치 | 헤더-only 제출 거부 | Idempotency-Key 및 answerId/evaluationId 반영 |
| 이력 페이지 이동 누락 | 다음 페이지 버튼 없음 | URL page와 이전·다음 이동 |
| 문제 route 재사용 | 질문 8 이동 후 질문 7 유지 | 경로 감시·늦은 응답 무시 |
| 미확정 요청·polling 경계 | 계정 간 키 공유, 5xx 키 소실, unmount 후 재예약 | 회원별 sessionStorage·세대 검사 |

초기 타입 누락·fixture 컴파일 오류는 유효 RED에서 제외. 실행 가능한 행동 실패를 별도로 확인.

## 검증 결과

| 검증 | 명령·범위 | 결과 |
|---|---|---|
| 전체 백엔드 | `./gradlew test` | 35개 클래스, 197개 성공, 실패·오류·skip 0 |
| Phase 4 백엔드 | 아래 7개 클래스 | 54개 성공 |
| 전체 프런트 | `cd front && npm test` | 17개 파일, 102개 성공 |
| 타입 검사·빌드 | `cd front && npm run build` | vue-tsc·Vite 성공 |
| 공백 오류 | `git diff --check` | 오류 없음 |
| OpenAPI 구문·참조 | Ruby YAML 파싱·로컬 $ref 확인 | 32개 path, 38개 schema, 182개 참조 누락 없음 |
| 문서 링크 | 변경 문서의 로컬 링크 존재 확인 | 누락 없음 |
| 실제 화면 | cmux WebKit, 아래 흐름 | 통과, 브라우저 오류 없음 |

Phase 4 테스트 분포:

- AnswerTest: 6개 — 원문·공개 상태·UUID·길이·회원 상태
- EvaluationTest: 8개 — 상태 전이·Concept 완전성·점수·부분 수정 방지
- StubEvaluationAdapterTest: 4개 — 성공·검토 필요·실패와 profile 경계
- AnswerServiceTest: 10개 — 저장 커밋·멱등·이력·중복 worker·재시도·FK·timeout
- AnswerControllerTest: 22개 — HTTP 계약·validation·401/403/404/409
- AnswerFlowTest: 3개 — 제출·동시 멱등 요청·다른 회원 소유권
- EvaluationWorkerTest: 1개 — 알림 없이 DB 대기 작업 발견·커밋 확인

관찰된 환경 경고: JVM class sharing, Vitest `--localstorage-file` 경고. 테스트·빌드 실패 없음.

## 실제 화면 확인

- 환경: `local` profile + `jdbc:h2:mem:phase4preview` + `ddl-auto=create-drop`
- 기존 `data/` 로컬 DB 변경 없음
- Vite: `127.0.0.1:5173`, Backend: `8080`
- cmux: 호출 작업 공간 오른쪽 브라우저 패널, 기존 작업 포커스 유지
- 임시 회원가입 → 로그인 → 공개 문제 → 원문 입력 → 제출 → `정답 · 100점` 모의 결과 확인
- `/answers/1` 새로고침 후 동일 원문·평가 결과 복구
- 내 답변 이력에 질문·원문·정답 표시
- 데스크톱 1280×900, 모바일 390×844 확인
- 모바일: document 폭 373 ≤ viewport 폭 390, 가로 넘침 없음
- [데스크톱 평가 화면](assets/evaluation-desktop.png)
- [모바일 평가 화면](assets/evaluation-mobile.png)

화면 확인용 서버는 메모리 DB 사용. 서버 종료 시 임시 회원·답변 데이터 제거.

## 미검증·다음 Phase 경계

- 외부 AI·검색 근거·Knowledge State 실제 갱신: Phase 5~6 범위
- 운영 DB·다중 서버·프로세스 강제 종료 실험: 미검증; 현재 DB 잠금 검증은 H2의 동시 스레드 기준
- 실제 AI 전환 전: 긴 호출의 DB 잠금 분리, 작업 lease·호출 timeout·영속 재시도 횟수 검토
- 동일 평가의 최종 DB 확정은 한 번; 프로세스 중단 직전 외부 호출의 중복 자체는 보장하지 않음
- 브라우저 탭 종료·로그아웃: 미확정 제출 복구 저장소 제거. 저장된 답변은 이력에서 확인
- 기본 profile에 실제 평가 provider 없음: Phase 4는 local/test 개발 흐름만 제공
