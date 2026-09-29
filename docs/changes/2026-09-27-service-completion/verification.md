# AI 연결을 제외한 서버·제출 검증

- 확인일: 2026-09-27
- 범위: 당시 추가 비용 없는 로컬 실행 검증. 실제 AI 호출·추가 모델 설치·유료 배포 없음. [후속 GPT 검증](../2026-09-29-openai-completion/verification.md) 별도
- 실행: macOS, Java 21, Node 24, PostgreSQL 17.11, 독립 worktree
- 실제 사람 검수·참가자 파일럿·공개 운영 출시 판정은 미실행

## 변경과 설계

```text
정규화 이메일+접속 주소 → SHA-256 키
                           ↓
앱 A / 앱 B → LoginAttemptService → 같은 DB 행 잠금
                           ↓
                    LoginAttempt 도메인
          10분 실패 창 / 5회 실패 / 15분 차단
                           ↓
          성공 시 삭제 / 만료 후 최대 200개씩 정리
```

- 상태와 실패 창 규칙: LoginAttempt
- DB 순서와 transaction: DefaultLoginAttemptService. 기존 행 비관적 잠금, 최초 UNIQUE 경쟁만 최대 3회 재시도
- 잠금 획득 후 현재 시각으로 실패 기록. 실패 transaction의 재시도는 rollback 이후 새 transaction
- SHA-256은 원문 저장 축소 수단이며 익명화·공격자 추측 방지 보장 아님
- 인증 session은 메모리 유지. 로그인 제한만 DB 공유; 무중단 세션 확장으로 표현하지 않음
- 스케줄 활성화: 공통 SchedulingConfiguration. 평가·후속 Worker 모두 비활성이어도 인증 기록 정리 유지
- V002는 새 테이블·인덱스 추가만 수행. 기존 데이터·V001 baseline 보존

## RED와 GREEN

| 요구 동작 | 관찰한 RED | GREEN 근거 |
|---|---|---|
| 독립 Service 인스턴스의 차단 공유 | 기존 메모리 구현에서 차단 예외 없음, 4개 중 1개 실패 | DB aggregate 도입 후 4개 통과, 후속 9개 Service 경계·경쟁 테스트 통과 |
| AI Worker 비활성 시 만료 기록 정리 | 3초 대기 후 만료 행 1개 남음, 스케줄 실행 없음 | 공통 스케줄 활성화 후 자동 삭제. 별도 테스트 context 종료로 짧은 주기 격리 |

- 도메인 9개: 초기 시각·잘못된 키·null 입력 무변경·10분 포함 경계·15분 만료·차단 연장 방지·목록 보호
- Service 9개: DB 공유·동시 최초 5건·정규화·주소 범위·성공 삭제·정리·차단·만료
- AuthenticationFlowTest: 메모리 제한 초기화를 위한 매 테스트 context 재생성 제거. 생성한 DB 기록을 FK 역순 정리

## 전체 회귀

| 명령 | 결과 |
|---|---|
| `./gradlew test postgresTest bootJar --console=plain` | H2 508 / PostgreSQL 65, 실패·오류·skip 0, JAR 생성 성공 |
| `npm --prefix front test` | 37개 파일, 326개 통과 |
| `npm --prefix front run build` | TypeScript 검사·production build 성공 |
| `python3 -m unittest discover -s scripts -p 'test_content_bundle.py'` | 14개 통과 |
| `python3 scripts/content_bundle.py` | 25문항·50개 Concept·5문서·20개 출처 구조 통과 |
| `python3 scripts/render_content_review.py` | 25문항·5문서 읽기 사본, 원본 hash 표시 |

- 실 모델 live task는 실행하지 않음
- PostgreSQL Testcontainers는 별도 DB 사용. 화면 seed에 의존하는 회귀 테스트 없음
- build 산출물의 로컬 로그: `build/reports/service-completion/`
- 상세 자동 보고서: `build/reports/tests/test/`, `build/reports/tests/postgresTest/`

## DB 적용과 복구

1. 원본 `crackcs_local` 백업: `build/backups/pre-login-v002.dump`
2. 새 `crackcs_restore_login_v002`에 복구
3. 복구 DB에 V002 적용, 앱 `ddl-auto=validate`·health UP 확인
4. 원본에 V002 적용, 회원·답변·평가·문제·문서 건수 전후 동일 확인
5. 원본과 격리된 복구 DB에서 HTTP·브라우저 검증

- 백업 SHA-256: `de5fc2a7ad6d3af9d1ddb47ab2af1242c7de08c242f0e500c97ec89008f007d8`
- schema_version: 001, 002 각 한 건
- 기존 데이터 삭제·volume 초기화·역방향 DDL 없음
- 다른 checkout의 구버전 앱은 새 테이블을 사용하지 않는 추가 방식
- 새 checkout 실행과 기존 V001 volume의 수동 적용을 구분: [실행 안내](../2026-09-27-release-readiness/local-runbook.md)

## 두 JVM과 실제 HTTP

- 앱 A 8080, 앱 B 8082, 같은 복구 PostgreSQL
- 앱별 독립 session cookie·로그인. 로드밸런서나 공유 session 사용 없음
- 검증 전용 Topic·Concept·문서·문제·회원은 관리자/가입 API로 생성. seed 문제 건수에 의존하지 않음
- 통제 콘텐츠의 review/publish API 검증이며 initial-v1 사람 검수와 무관

| 검사 | 관찰 |
|---|---|
| 두 앱에서 최초 로그인 실패 5건 동시 기록 | 한 키로 누적, 두 앱 모두 다음 요청 429·Retry-After 반환 |
| 이메일 대소문자·X-Forwarded-For 변경 | 같은 접속 주소의 제한 우회 실패 |
| 같은 회원의 서로 다른 답변 10건 동시 접수 | Answer 10개, Evaluation 완료 10개 |
| 다른 앱에서 기존 Idempotency-Key 재전송 | 동일 answerId 반환, 제한 수 추가 소모 없음 |
| 11번째 새 요청 | 429, provider 호출 전에 접수 거절 |
| 동시 완료 | 두 앱의 완료 로그 모두 존재. Knowledge State attempt_count=10, score_sum=1000, 적용 기록 10개 |
| 일반 조회·권한 | 공개 문제 referenceAnswer 없음, USER 관리자 API 403, 다른 회원 답변 404 |

- HTTP 자동 판정 14개 통과. 모의 판정 100점은 동시성 검증용 고정값
- 로컬 10건 표본이며 운영 규모 처리량·분산 장애·다중 리전 검증 아님

## 보이는 브라우저 흐름

- `CMUX_WORKSPACE_ID`와 `cmux identify --json` 대조: caller workspace:1 / surface:1
- 기존 보조 pane:11 / surface:14의 서버 로그, 임시 surface:25의 명령 결과, surface:26의 실제 클릭 사용
- 답변 3: 브라우저 제출 → PROVIDER_ERROR → 답변·EVALUATING 보존 → 서버 정상 모의 응답으로 전환 → 같은 평가 두 번째 시도 EVALUATED
- 재시작 때 연결 복구·재로그인 후 원래 `/answers/3` 복귀. 원문·근거·후속 질문 표시
- DB 확인: evaluation 3 attempt_count=2, status=EVALUATED
- 별도 최종 실패: evaluation 15 attempt_count=3, status=FAILED, failure_reason=PROVIDER_ERROR. 원문 보존, 관리자 API에서 안전한 실패 코드·원래 답변 조회
- 관리자 실제 클릭: FAILED 목록에서 #15 선택, PROVIDER_ERROR·원래 질문·답변 표시 확인
- 모델 메타데이터가 없는 실패의 화면 문구를 `모델 정보 없음`으로 정정. 호출 여부를 메타데이터 부재로 단정하지 않음
- 화면 증거: `retry-pending.png`, `retry-completed.png`, `admin-failure.png` — 로컬 보고서 폴더

재현 시 `local,local-postgres`, `crackcs.evaluation.stub-outcome=FAILURE`, `crackcs.evaluation.retry-base-delay=60s`로 첫 실패 관찰 후 CORRECT로 재시작. 최대 실패 확인은 delay=1s, FAILURE 유지. 실제 AI 품질·실제 외부 503 응답 측정 아님.

실행 중 발견한 환경 문제:

- cmux 임시 터미널 기본 Java 17로 JAR 실행 실패 → Java 21 경로 명시 후 정상 시작
- 검증 스크립트가 서버 시작 전에 접근 → health·API 준비 확인 후 재실행
- 최종 bootJar가 실행 중 JAR를 교체해 class loading 실패 → 서버 종료 후 고정 사본으로 재실행. 실행 JAR 덮어쓰기 금지
- 관리자 상세의 `failureCode`를 스크립트에서 `failureReason`으로 잘못 가정 → OpenAPI/DTO와 맞춘 뒤 재실행. 앱 계약 변경 없음

## 보안 점검과 수용 경계

| 발견·경계 | 위험도/판정 | 처리·근거 |
|---|---|---|
| 프로세스별 로그인 실패 기록 손실 | 중간, 수정 | DB 공유·재시작 보존, H2/PG/실제 두 JVM |
| Worker 비활성 때 기록 정리 중단 | 낮음, 수정 | 독립 스케줄 회귀 |
| 여러 계정·분산 주소의 전역 남용 | 공개 운영 시 높음, 현재 노출 없음 | loopback 데모 한정. 인터넷 공개 전 전역 제한·trusted proxy 설계 필수 |
| 세션 메모리·재시작 로그인 만료 | 현재 데모 수용 | 앱별 로그인, 무중단 인증 미주장 |
| TTL 정리 backlog | 현재 데모 수용 | 매분 최대 200개, 즉시 삭제 보장 아님 |
| 비밀 값·타인 데이터 노출 | 검증 범위에서 미발견 | AuthenticationFlowTest, SecurityConfigurationTest, AnswerFlowTest, EvaluationOperationLoggerTest, RequestIdFilterTest |
| 실제 AI prompt injection·토큰 과금 | 이번 범위 제외 | adapter 입력 격리·구조 계약 테스트만. 실 모델 저항성 미주장 |

- 확인 범위: 앱 인증·인가·응답 필드·일반 로그·rate limit 경계. 전문 침투 테스트나 전체 공급망 무결성 인증 아님
- 자신의 닉네임·답변 및 관리자가 권한으로 보는 원문은 정상 계약. 비밀정보·타인 데이터의 비인가 응답 비노출과 구분
- 일반 로그에는 request/answer/evaluation/member ID·안전한 실패 코드. 비밀번호·session·CSRF·답변 원문 미포함 자동 회귀
- 이번 앱 로그 6개에서 검증용 비밀번호·답변 원문 marker 검색 0건
- 변경 문서의 로컬 링크 대상 138개 존재 확인

## 최종 독립 검토

- 읽기 전용 범위: `0276bc5..59cd175`, 전체 6개 커밋. 최신 로그인·schema·스케줄·문서뿐 아니라 이전 화면·콘텐츠 변경과 연결 확인
- 결과: Critical 0, Important 0, Minor 1. 병합 가능
- Minor 수정: 이전 검증 문서의 남은 조건 표를 당시 이력으로 표시하고 최신 완료 근거 연결
- H2 508·PG 65 XML, 프런트 326·build, DB 적용·두 JVM·콘텐츠 86개 결과 교차 확인
- 제외 판단 유지: 실제 AI 품질, 실제 사람 검수·참가자 파일럿, 인터넷 공개 운영·전역 남용·공유 session·원격 재해 복구. 현재 사용자 범위와 로컬 데모 경계에 따른 구분


## 초기 콘텐츠와 제출 문서

- 원본 DB의 OS 출처 위치, Java interrupt 상태 소거, Boot 설정 우선순위 근거 문단 정정
- 25문항·5문서 DRAFT·미검수 확인, 두 번 재등록 ID 동일, 공개 문제에 신규 문항 없음. 정정 후 API 판정 86개 통과
- [검수용 읽기 사본](../../content/initial-v1/review.md): 질문·답안·필수 개념·혼동 주의·출처·근거 문서 한곳에서 대조
- README: 0원 실행 → 설계 → 검증 → 한계 순서
- [P0/AC 대응표](../2026-09-21-phase-8/acceptance-matrix.md), OpenAPI, ADR, ERD, 문서 지도 동기화
- [파일럿 실행안](../2026-09-21-phase-8/pilot-runbook.md)은 준비 완료. 참가자 모집·실제 기간·피드백·출시 판정은 실행하지 않음

## 완료 범위 밖

- 사용자 제외: AI 모델/API 연결, 전체 모델 품질 Gate
- 실제 사람의 초기 콘텐츠 검수·공개 승인
- AI Gate와 콘텐츠 승인 이후 실제 참가자 파일럿
- 공개 운영 인프라·trusted proxy·공유 session·전역 남용 제한·외부 재해 복구·운영 규모 성능
- 위 항목을 완료로 체크하거나 취업 합격을 보장하지 않음

## CI 후속 정정

- 첫 main 실행: GitHub Actions `36317273071`, frontend 통과·backend 508개 중 1개 실패
- 실패: `QuestionServiceTest.updatesAndSavesQuestion`의 생성 시각 정확 일치 비교
- 원인 재현: 메모리 `11:56:39.123456789` → H2 timestamp(6) `11:56:39.123457`. 저장 전 객체와 DB 재조회 값을 비교한 테스트의 정밀도 가정
- 수정: 수정 전·후 모두 Repository 재조회 값 비교. 생성 시각 보존의 정확 일치 검증 유지, 허용 오차 확대·생산 시각 변경 없음
- CI 실패 진단 보강: Gradle Test의 전체 예외 메시지 출력
- 수정 후 로컬 전체 회귀: 508개 통과, 실패·오류·skip 0. 수정 후 CI 결과는 해당 커밋의 Actions 기록 참조
