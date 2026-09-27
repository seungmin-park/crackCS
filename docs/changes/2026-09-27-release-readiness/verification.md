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
- 생성·수정·개념 교체·검수·공개·폐기가 같은 계열 잠금 사용. 최초 행은 RETIRED여도 잠금 대상으로 유지
- DB 최종 방어: Question `(version_series_id, question_version)` UNIQUE
- 추가 회귀: 최초 버전 폐기 후 현재 공개본에서 동시 초안 8개 생성
- H2 전체: 484개 성공, 실패·오류·skip 0
- PostgreSQL: 44개 성공, 실패·오류·skip 0. 콘텐츠 동시성 7개 포함
- 실행: `./gradlew test postgresTest --console=plain`
- 로그: `build/reports/readiness/version-red.log`, `version-green.log`, `task1-suite.log`
- XML: `build/test-results/{test,postgresTest}/`
- 경계: 공개본에서만 다음 버전 생성 가능. 공개본이 없는 계열에 공개본 1개를 강제로 생성하는 계약 아님
- 잠금 범위: 서비스의 기존 콘텐츠 변경 전체. 직접 SQL로 상태를 바꾸는 운영 도구는 같은 계약 준수 필요

## 빈 환경 실행·복구

- Docker Compose로 별도 PostgreSQL 17.11 volume 생성, V001 SQL 적용 후 Hibernate validate 기동 성공
- 브라우저: cmux workspace:1, surface:15. 서버 로그 surface:14
- 일반 회원 가입·로그인 → 추천 문제 → 답변 → 평가·근거 → 후속 답변 → 지식 지도 확인
- 기본 평가·후속 질문: stub. 이 검증은 모델 품질 증거 아님
- 지식 지도: 미평가 1 → 학습 중 1, 평가 2회·숙련도 100·신뢰도 50
- USER의 관리자 직접 URL 진입: 접근 불가 화면
- backup: `build/backups/readiness-001.dump`, SHA-256 `a36efae4325fe7083cf74fafc08f0d47bf4cac123c0a1766b5f247884c070850`
- 새 DB: `crackcs_restore_readiness001`. 원본/복구 모두 회원 2, 문제 2, 답변 2, 평가 2, Evidence 2
- 평가 상태 EVALUATED 2, Answer·Evidence 끊어진 참조 각각 0
- 원본 DB 대상 restore·기존 복구 DB 재사용·기존 archive 덮어쓰기 모두 거절
- 복구 DB에 앱 재기동 → 회원 로그인 → `/answers/1` 원문·평가·근거 조회 성공
- 로그: `build/reports/readiness/{backup-restore,restored-backend}.log`
- 범위: 같은 PC의 논리 backup/restore. 외부 장애 영역·운영 RPO/RTO 검증 아님

## 발견 결함과 회귀

- PostgreSQL 학습 홈: 날짜 매개변수 null 타입 추론 실패. 기존 H2 테스트는 통과했으나 PostgreSQL RED 확인
- 수정: 전체 답변 개수와 기간별 개수 쿼리 분리. `LearningProgressServiceTest` PostgreSQL GREEN
- AC-007: 문서 v2 공개로 v1 폐기 → 신규 검색은 v2만 → 과거 평가 응답은 v1 Chunk·내용·버전 유지. 단일 통합 시나리오 추가, 기존 동작 확인
- 신규 답변 요청 제한과 로그인 차단 만료 오류: 2개 RED 확인 후 GREEN
- 답변 한도: 기본 회원별 1분 10개, 기존 회원 행 잠금 안에서 검사. 동일 요청 재전송은 허용. 12개 동시 신규 요청 중 10개 저장·2개 차단
- HTTP 429 변환: 별도 RED → Retry-After 60 포함 GREEN
- 한도 설정: `crackcs.answer.max-submissions-per-minute`. 기존 Service 미세 측정은 이력 준비 때문에 1000 적용, 일반 실행·HTTP 측정은 기본 10 유지
- 답변 상세: 최초 로딩 표시 누락·네트워크 오류 시 원문 숨김 2개 RED → GREEN
- 권한·404 오류는 원문 숨김 유지. 일시 연결 오류만 원문 유지
- 현재 H2 490개, PostgreSQL 54개, 프런트 293개 성공. 이후 변경 최종 실행은 아래 갱신

## HTTP 성능·최종 회귀 — 2026-09-27

- 실행: `./gradlew httpLatencyBenchmark test postgresTest localServiceLatencyBenchmark --console=plain`
- 종료 코드: 0, H2 490개·PostgreSQL 54개·HTTP 측정 1개·Service 측정 1개 성공
- 프런트: `npm test` 293개 성공, `npm run build` 타입 검사·Vite 빌드 성공
- PostgreSQL HTTP fixture: 문제 100·회원 6·답변 500·평가 500. seed SQL 미사용
- 조회 동시성 4, 각 경로 예열 5회·측정 40회. 접수 동시성 5, 회원별 예열 1회·측정 5회(총 25회)
- 요청: 실제 Tomcat HTTP·Security·session·CSRF·JSON·DB 경유. 조회 전부 200, 접수 전부 202
- p95: 문제 목록 10.297ms, 답변 이력 17.895ms, 학습 홈 21.214ms, 답변 접수 8.562ms
- 종료 후 답변·평가 각각 530개 확인. 거절 요청을 성공 표본에 섞지 않음
- 측정 경계: loopback·짧은 부하·소규모 데이터. 운영 SLO·최대 처리량·실제 AI 완료 시간으로 확대 해석 금지
- 원본: `build/reports/http-latency/result.json`, 실행 로그 `build/reports/readiness/final-backend.log`
- 관리자 브라우저: 복구 DB에서 문서 초안 → 검수 → 공개 → 검색 문단 1개(KEYWORD_SEARCHABLE) 확인
- 기록: `build/reports/readiness/admin-document-browser.{txt,png}`
- 탭 정리: 사용이 끝난 모델 탭·브라우저 종료, 검증 터미널 재사용

## 독립 리뷰와 수정

- 전체 변경 독립 리뷰: Critical 0, Important 1, Minor 1
- Important: 수정·검수 경로의 계열 잠금 누락. 오래된 DRAFT 상태가 공개 상태를 덮어쓸 가능성
- RED: 문제·문서 각각 검수 transaction을 유지한 상태에서 공개가 먼저 완료. 2개 모두 실패
- 수정: 모든 기존 콘텐츠 변경에서 계열 잠금 후 대상 조회. 도메인은 변경 가능 상태를 판단하고 Service는 transaction 순서를 조정
- GREEN: 콘텐츠 동시성 9개 성공. 최초 버전 폐기·동시 버전 할당·공개 경쟁 회귀 포함
- UI finding은 Important로 상향: 연결 오류에서 보존한 원문이 재시도 버튼 클릭 후 다시 사라지는 문제
- RED: 실패한 재조회 뒤 원문 부재 1개. 테스트 종료 후 컴포넌트가 남은 격리 오류는 RED 근거에서 제외하고 자동 unmount 적용
- 수정: 같은 답변의 일시 조회 실패는 원문 유지. 다른 답변 이동·401·403·404는 기존 원문 숨김
- GREEN: 답변 상세 16개 성공. 재시도 실패→복구, 권한 오류, 라우트 전환 포함
- 로그: `build/reports/readiness/review-{red,green,ui-red-isolated,ui-green}.log`
- 최종 회귀: H2 492개·PostgreSQL 56개·프런트 298개 성공, 실패·오류·skip 0. 타입 검사·Vite 빌드 성공, `suite_exit=0`
- 실행: `./gradlew test postgresTest --console=plain`, `npm --prefix front test`, `npm --prefix front run build`
- 전체 로그: `build/reports/readiness/review-{suite,front-suite,front-build}.log`

```text
콘텐츠 변경: 계열 잠금 → 최신 엔티티 조회 → 도메인 상태 검사 → 변경·commit
답변 재조회: 같은 답변 + 일시 오류 → 원문 유지 + 재시도 안내
             다른 답변 / 접근 거절 → 원문 제거
```

## 실제 모델 candidate 표본 — 2026-09-27

- 모델: Ollama 0.34.4, `gpt-oss:20b` ID `17052f91a42e`, `os-evaluator-v1-medium`
- 입력: `reference-v1` 1.0.0, `evaluation-candidate`의 provider 대상 첫 12건. 전체 분할·독립 holdout 인증 아님
- 근거 부족 사례: 모델 호출 전 중단 경로라 이번 provider 표본에서 제외
- 환경: M2·16GB, 모델 13GB·context 4096, Ollama 표시 CPU/GPU 18%/82%. 검증 앱·로컬 DB 중지, 본 작업의 JVM 테스트와 순차 실행. 다른 workspace의 host 부하는 통제하지 않음
- 이전 동시 실행은 메모리 압박으로 중단. 최종 집계에서 제외하고 완료 실행만 아래 기록
- 실행: `./gradlew ollamaLiveEvaluation -PollamaSplit=evaluation-candidate -PollamaCaseLimit=12 -PollamaReasoningEffort=medium -PollamaReport=build/reports/evaluation/ollama-candidate-12.json --console=plain`
- 종료 코드 0, 21분 16초. 호출 12/12 완료, timeout·응답 오류 0, schema 성공 12/12, 근거 ID 범위 유효 12/12
- 입력 7,159토큰·출력 7,428토큰, 호출 시간 합계 1,271.212초. 외부 API 과금 0원; 전력·기존 장비 비용 제외
- 상세 판정 9/12(75%), 이진 판정 8/8(100%), false-correct 0/4(0%)
- 부분 정답 4건 중 3건을 INCORRECT로 판정. 제공 근거 ID 유효성은 근거 내용의 의미적 타당성·전체 앱 Evidence 연결 검증과 구분
- p95 156.701초(12건 nearest-rank의 최댓값), 범위 70.885~156.701초
- 판정: 상세 일치 85%·p95 20초 목표 미달. 표본의 이진·schema 성공만으로 출시 Gate 통과 처리 금지
- 원본: `build/reports/evaluation/ollama-candidate-12.json`, 로그 `build/reports/readiness/ollama-candidate.log`. `.partial.json`은 진행 중 기록

| 사례 | 기준 | 실제 | 초 |
|---|---|---|---:|
| CS-07-G01 | CORRECT | CORRECT | 139.465 |
| CS-07-G02 | PARTIALLY_CORRECT | INCORRECT | 86.202 |
| CS-07-G03 | INCORRECT | INCORRECT | 103.293 |
| JAVA-09-G01 | CORRECT | CORRECT | 91.578 |
| JAVA-09-G02 | PARTIALLY_CORRECT | PARTIALLY_CORRECT | 70.885 |
| JAVA-09-G03 | INCORRECT | INCORRECT | 156.701 |
| SPRING-07-G01 | CORRECT | CORRECT | 108.944 |
| SPRING-07-G02 | PARTIALLY_CORRECT | INCORRECT | 106.771 |
| SPRING-07-G03 | INCORRECT | INCORRECT | 105.508 |
| JPA-06-G01 | CORRECT | CORRECT | 118.577 |
| JPA-06-G02 | PARTIALLY_CORRECT | INCORRECT | 84.888 |
| JPA-06-G03 | INCORRECT | INCORRECT | 98.400 |

### 미달 대응

- 채점: 부분 정답을 오답으로 낮추는 경계가 관측됨. 다음 조정은 development에서 최소 인정 기준 검토 후 회귀; 이번 candidate 사례를 독립 검증 자료로 재사용하지 않음
- 지연: CPU offload·host 상태와 출력 생성이 관측 조건. 각각의 인과 기여는 분리 측정 전 미확정
- 0원 실행: 접수와 모델 완료의 비동기 분리·대기 표시·새로고침 복원을 유지. 20초 목표 자체를 달성한 것으로 변경하지 않음
- 다음 측정: 모델·prompt 고정 후 앱을 포함한 동일 환경에서 전체 대상 재측정. 비용 없이 지연·품질을 함께 만족할 후보가 없으면 실제 모델 출시 Gate 미통과 유지
- 파일럿: 명세의 모델 Gate 미달 시 중단 조건 유지. 이번 표본 결과로 실제 사용자 운영 승인 금지

## 관리자·소유권 브라우저 추가 검증

- 동일 복구 DB, cmux workspace:1·surface:19. 앞서 사용을 마친 surface:15는 종료
- 관리자: 새 Topic `REVIEW_FLOW` → Concept `SERIES_LOCK` → 문제 #3 초안 → 필수 Concept·가중치 1.00 저장 → 검수 → PUBLISHED 확인
- 공개 뒤 직접 초안 수정 대신 새 버전·폐기 동작 표시. 문서 초안·검수·공개·Chunk 흐름은 앞선 브라우저 증거와 연결
- 별도 회원 #3 가입·로그인 → 다른 회원의 `/answers/1` 접근 → 원문 없는 오류 화면
- 같은 세션의 `/api/answers/1`: `ANSWER_NOT_FOUND`, 원문·평가 비포함. HTTP 상태 계약은 자동 API 회귀에서 확인
- USER의 `/admin/questions`: 관리자 권한 안내와 접근 거절
- 화면 기록: `build/reports/readiness/admin-question-browser.{txt,png}`, `ownership-browser.txt`
- 기다릴 성공 문구를 잘못 지정한 두 자동화 timeout은 실제 DOM에서 저장 성공 확인. 앱 저장 실패로 집계하지 않음

## 깨끗한 checkout 실행

- 커밋 `93b6cbd`의 새 detached worktree `/private/tmp/crackcs-clean-20260927-93b6cbd`, 시작·종료 시 tracked/untracked 변경 없음
- README·로컬 실행 문서의 Java 21·Node 24 환경, `npm ci`, `scripts/local-postgres.sh start`, `bootRun --spring.profiles.active=local,local-postgres`, Vite 실행
- 설치 종료 0, 211개 package 설치. npm의 선택적 watcher 설치 스크립트 2개 미승인 경고는 유지; 별도 권한 설정 변경 없음
- Hibernate validate 기동 → 실제 브라우저 관리자 로그인 → PostgreSQL 공개 문제 목록 조회 성공
- 범위: 새 소스 checkout·새 node_modules, 기존 Gradle cache·PostgreSQL volume 재사용. 빈 DB SQL 초기화는 앞선 별도 검증
- 기록: `build/reports/readiness/clean-{install,backend,frontend}.log`, `clean-checkout-browser.txt`
- 종료: 검증 앱·Vite·장애 proxy·로컬 DB 컨테이너 정지, Ollama 모델 메모리 해제, 임시 checkout 제거
- 탭: 사용 종료한 surface:19 브라우저 닫음. 결과 확인용 surface:14 터미널 유지, 다른 workspace 탭 보존

## 실제 provider 장애 브라우저 검증

- 경로: 브라우저 → PostgreSQL 앱·worker → loopback 장애 proxy → 실제 Ollama. 후속 질문은 stub 유지
- proxy: 최초 두 호출만 통제된 HTTP 503, 세 번째부터 실제 모델로 전달. 모델 판정 내용은 proxy에서 변경하지 않음
- 답변 #3 저장 → 503 두 번·자동 재시도 → 세 번째 실제 HTTP 200(121.184초)
- 완료 transaction은 `INVALID_RESULT`로 결과 거절. 최종 FAILED, 원문 유지, 화면에 오답 판정과 구분된 실패 안내
- 평가 중 새로고침: 동일 답변·진행 상태 복원. 무한 polling 대신 최종 실패 상태 표시
- 최초 실패 응답 원문은 미수집. 개념 집합·판정 일관성·빈 목록 항목 중 정확한 거절 원인은 미확정
- 별도 진단 재실행: 직접 작성한 검증 답변의 요청·응답만 로컬 `build/`에 기록. 운영 로그나 Git에 사용자 원문을 추가하지 않음
- 답변 #4 진단 재실행: 통제 503 없이 실제 모델 3회 호출, 모두 3분 제한 초과. 최종 FAILED / PROVIDER_TIMEOUT, 원문 보존
- DB 확인: #3·#4 모두 attempt_count 3·FAILED, Answer 2개 유지, 해당 EvaluationEvidence 0개, 회원 #3 Knowledge State 0개
- 재실행에서 정상 응답을 얻지 못해 최초 INVALID_RESULT의 정확한 원인 진단은 미완료. 재시도 상한·실패 상태의 안전성만 확인
- 원인 추정만으로 schema·도메인 불변식을 완화하지 않음. 다음 행동은 host 여유 확보 후 합성 입력의 원문 응답을 확보하고 결과 검증 규칙별 대조
- 첫 실행 증거: `build/reports/readiness/provider-first-{proxy,backend}.log`, `provider-failure-browser.{txt,png}`
- 진단 실행 증거: `build/reports/readiness/provider-{proxy,backend}.log`, `provider-timeout-browser.txt`
- 판정: 장애·재시도·안전한 최종 실패는 관찰. 실제 모델 결과까지 성공한 복구 E2E로 체크하지 않음

## 당시 남은 조건 — 이전 검증 시점의 이력

아래 표는 `97b8b0e`까지의 상태 보존. 현재 완료·후속 범위는 [서버·제출 마무리](../2026-09-27-service-completion/verification.md), [화면·콘텐츠 검증](../2026-09-27-ui-content/verification.md), [작업 목록](../../planning/tasks.md) 참조.

- 후속 완료: 로그인 DB 공유·두 JVM 검증, 초기 25문항·5문서 DRAFT, 화면 공통 오류 표현
- 실제 AI Gate는 이후 사용자 지시로 제출 범위 제외. 사람 콘텐츠 승인·실제 파일럿은 미실행 유지

| 항목 | 당시 경계·다음 행동 |
|---|---|
| 전체 관리자 E2E | Topic·Concept·문제·문서 공개 브라우저 흐름 완료. 운영 콘텐츠 사람 검수와 구분 |
| provider 실패·긴 대기 E2E | 503 두 번 뒤 실제 응답이 INVALID_RESULT로 거절, 원문·실패 상태 보존. 결과 거절 원인 진단 및 성공 복구 확인 필요 |
| 로그인 다중 인스턴스 | 만료 경계 수정. 공유 저장소는 실제 배포 구조 결정 후 검증 |
| 요청 남용 | 회원별 DB 기반 접수 제한·멱등성·재시도 상한 검증. 다중 계정·전역 admission 제한 미구현 |
| 운영 성능·복구 | 로컬 PostgreSQL 결과 확보. 운영 규모·원격 장애 복구·RPO/RTO는 별도 |
| 초기 콘텐츠 | 화면 확인용 문제·문서 한 쌍. leaf Topic당 5문제·출처·라이선스·필수 Concept 검수는 미완료 |
| 실제 모델 Gate | 아래 실측과 별도 판정. 20초 p95 목표 기존 미달, 전체 reference·독립 holdout 인증 없음 |
| 사용자 파일럿 | 실제 참가자·기간·피드백 필요. 자동 테스트로 대체하지 않음 |
| 모든 화면의 오류 표현 통일 | 답변 상세 경계 보완 완료. 전 화면의 공통 오류 변환 정리는 후속 범위 |
| 출시 공통 체크 | 개별 증거만 체크. 위 Gate가 남아 있어 전체 출시 완료로 표시하지 않음 |
