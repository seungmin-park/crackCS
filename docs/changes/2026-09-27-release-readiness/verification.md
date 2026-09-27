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

## 남은 조건

| 항목 | 현재 경계·다음 행동 |
|---|---|
| 전체 관리자 E2E | 문서 흐름 직접 확인. 새 Topic·Concept·Question 전체 브라우저 흐름은 후속 검증 |
| provider 실패·긴 대기 E2E | 자동 회귀 있음. 실제 로컬 모델 실패 복구 화면은 후속 검증 |
| 로그인 다중 인스턴스 | 만료 경계 수정. 공유 저장소는 실제 배포 구조 결정 후 검증 |
| 요청 남용 | 회원별 DB 기반 접수 제한·멱등성·재시도 상한 검증. 다중 계정·전역 admission 제한 미구현 |
| 운영 성능·복구 | 로컬 PostgreSQL 결과 확보. 운영 규모·원격 장애 복구·RPO/RTO는 별도 |
| 초기 콘텐츠 | 화면 확인용 문제·문서 한 쌍. leaf Topic당 5문제·출처·라이선스·필수 Concept 검수는 미완료 |
| 실제 모델 Gate | 아래 실측과 별도 판정. 20초 p95 목표 기존 미달, 전체 reference·독립 holdout 인증 없음 |
| 사용자 파일럿 | 실제 참가자·기간·피드백 필요. 자동 테스트로 대체하지 않음 |
| 모든 화면의 오류 표현 통일 | 답변 상세 경계 보완 완료. 전 화면의 공통 오류 변환 정리는 후속 범위 |
| 출시 공통 체크 | 개별 증거만 체크. 위 Gate가 남아 있어 전체 출시 완료로 표시하지 않음 |
