# 백엔드 커리큘럼 조사·문서 검증

- 확인일: 2026-10-06
- 범위: 국내 경력까지의 채용·교육 조사, 콘텐츠 범위·제작 계획, 제품·정책·작업 목록·문서 지도 연결
- 작업 경로: `.worktrees/backend-curriculum`, `docs/backend-curriculum`
- 기준 main: `9ca58c79380ebb587ed9cfed0aae7de143ff3e45`

## 변경 책임·검토

- [README](README.md): 핵심28개·선택2개 Topic, 학습 깊이·선수 지식·문항 방향·제작 순서
- [시장조사](market-research.md): 목적 표본·공고 요건 해석·교육 공급·범위 결정의 근거와 한계
- [sources.json](sources.json): 확인 URL·용도·본문 해시·공고 구분. 원문 전문은 Git 제외 로컬 조사 자료
- 제품 명세: 경력 사용자와 확장 범위 연결. 콘텐츠 정책: 실제 출시 범위·최소량·설계 문항 검수 경계
- 작업 목록·문서 지도·initial-v1 안내: 현재 공개·기존 초안·신규 제작 계획의 상태 연결
- production·테스트 코드·초기 bundle·기술 의존성 변경 없음
- 객체 책임: 현재 Topic 분류·Concept 상태·Question 평가 연결 유지. 다른 Topic의 Concept 거부 조건을 문항 제작 제약으로 반영
- TDD 적용 대상 동작 변경 없음. 새 구현을 검증했다고 주장하는 테스트 추가 없음
- 원 저장소의 사용자 수정 `docs/README.md`, 로컬 회고·tobyteam 변경과 분리

## 검사·실행 결과

| 검사 | 실제 결과 |
|---|---|
| 문서 정합성 일회성 검사 | 출처45개·본문 해시45개 일치, 공식 상세9개·채용 사이트5개·교육4개·기술/CS31개·모집 안내1개, 핵심28개·선택2개, 내부 링크169개, 기존 bundle 원본 동일. exit 0 |
| `bash scripts/verify.sh all` | Python57·backend557·frontend339·PostgreSQL75, 총1,028개 통과. 필수 suite·JAR·type-check·프런트 production build 통과. exit 0 |
| `python3 scripts/check_stack_docs.py` | 설치 선언과 공식 문서19항목 일치 |
| `python3 scripts/check_test_reports.py backend` | 557개·99 suite, 실패0·오류0·skip0. exit 0 |
| `python3 scripts/check_test_reports.py frontend` | 339개 통과·실패0·pending0. exit 0 |
| `python3 scripts/check_test_reports.py postgres` | 75개·14 suite, 실패0·오류0·skip0. exit 0 |
| `git diff --check` | 공백 오류 없음. exit 0 |

- 문서 검사: `/private/tmp/crackcs-backend-curriculum-audit.py`. 새 runtime 검사기를 설치한 작업은 아님
- 도구 선택: Java `zulu-21.42.21`, Node `24.21.0`, Docker `29.8.2`. Java·Node bin 경로와 `JAVA_HOME`을 공용 실행에 명시
- 첫 공용 시도: Python57·스택19항목 이후 sandbox의 Gradle wrapper lock 쓰기 제한으로 중단. 승인된 재실행에서 전체 exit 0
- 로그 주의: 처음 지정한 `build/reports/backend-curriculum/shared-verify.log`는 backend `clean`이 삭제. 전체 원문 로그 보존 실패. 종료 코드·새 JUnit XML·Vitest JSON·필수 suite 검사·산출물로 결과 확인
- 보존 보고서: `build/test-results/test/`, `build/test-results/postgresTest/`, `front/test-results/vitest.json`. 요약·문서 검사: `build/reports/backend-curriculum/`
- 기존 제품 명세의 삭제된 `API URI 구현 체크리스트` 앵커 발견·수정. 현재 작업 상태로 연결하고 내부 링크 재검사
- diff 검토: 기존5개 Topic code·승인 상태 유지, 신규 콘텐츠는 계획 표시, 연차/우대/스택 구분, 전체 로드맵과 출시 묶음 분리, 원문 전문·비밀 미포함
- 최종 표본 단위 검토: 같은 채용 사이트의 서로 다른 법인을 한 기업군으로 집계하지 않도록 공식 사이트5개·직무9개로 정정. 기술 코드·원문 해시 변경 없음. 정정 뒤 문서 정합성·docs job 재검사

## 전달 경계

- main 보호 확인: strict 최신 main, 필수 `CrackCS verify`, GitHub Actions app `15368`, 관리자에게도 보호 적용, 강제 push 금지
- native auto-merge·squash 허용 확인. 현재 Git SSH 서명 설정 유지
- 실제 PR·머지 여부·main CI는 이 변경의 연결 PR과 Actions가 소유. 로컬 통과를 원격 완료로 간주하지 않음

## 검증 한계·다음 단계

- 채용9개·5개 채용 사이트은 플랫폼·금융 중심의 목적 표본. 국내 전체 수요 비율·취업률 추정 불가
- 원문 해시는 수집 본문 추적 수단. 출처의 정확성·대표성 인증이 아님
- 핵심28개는 기존5개 포함. 새23개 핵심·선택2개 콘텐츠 초안·등록·사람 검수·공개 미실행
- 표의 주제·문항 방향은 계획. 완성된 Concept 목록·reference answer·독립 대표 평가 표본 아님
- 기존 승인 OS·Java 10문항과 나머지3개 Topic 검수 대기 상태 유지
- 브라우저 E2E·새 분야 모델 호출·실제 참가자 파일럿·운영 배포는 이번 문서 검증 범위 밖
- 후속 콘텐츠 묶음마다 세부 공식 근거·이용 조건·허용 대안·필수 Concept·실제 모델 품질 확인 필요
- 전체 회귀 성공을 새 콘텐츠 승인 또는 공개 운영 승인으로 바꾸어 표시하지 않음
