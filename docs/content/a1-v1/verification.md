# A1 제작·검증 기록

- 실행일: 2026-10-07 KST
- 독자·질문: 검수자·작업자 / 무엇이 만들어졌고 어느 조건에서 실제로 확인됐는가?
- 기반: main `1c4622b150b9982d5f49fcaa98b2dad36656e287`, 격리 worktree `.worktrees/database-content`, 작업 브랜치 `docs/a1-content`
- 기준 원본: [bundle.json](bundle.json), 검수 경로: [README.md](README.md), 생성 사본: [review.md](review.md)
- 상태: A1 DRAFT 제작·구조·SQL 예시 확인. 사람 승인·등록·공개·검색·실제 모델 미실행

## 새로 남긴 결과와 책임

1. 신규6개 Topic의30문항·64개 Concept·6문서. 22개 원문 본문의 상태·hash·절 대조
2. 필수 개념별4가지 판정 경계와150개 작성자 진단 사례. 독립 golden·실제 모델 결과와 구분
3. 기존 읽기 사본 renderer의 입력 경로 확장. 기본 initial-v1 결과 유지, 원본 덮어쓰기 거부
4. [SQL 예시](postgres-examples.sql)의17개 assertion을 별도 PostgreSQL17에서 실행
5. 커리큘럼·작업 목록·문서 목록을 제작 후 상태로 갱신. root checkout의 사용자 수정·로컬 회고·다른 작업 파일 제외

```text
bundle.json — 콘텐츠와 작성자 기대값 소유
   ├→ content_bundle.py — 등록 구조 확인, --apply 없는 읽기 실행
   └→ render_content_review.py — 선택한 묶음의 읽기 사본 생성

출처 본문·SQL 실행 → 사실 확인 증거
사람 검수·실제 모델 → 이번 작업에서 미실행인 별도 승인·품질 경계
```

renderer의 책임은 읽기 자료 생성. 앱 Entity·HTTP 계약·DB schema·Service·의존성 변경 없음. importer는 진단 사례를 자동 평가하지 않으므로 이150개를 실행 테스트 수에 합산하지 않음.

## TDD와 도구 검증

```bash
python3 -m unittest scripts/test_render_content_review.py -v
python3 scripts/render_content_review.py
python3 scripts/render_content_review.py --bundle docs/content/a1-v1/bundle.json
```

- 최초 RED: 3개 중2개 실패, 기존 기본 경로1개 성공. 선택 출력 파일 미생성이 실패 원인; 문법·fixture·환경 실패 아님
- 최소 GREEN: `--bundle`·`--output`, 기본 출력은 선택 묶음 옆 `review.md`. rubric·진단 기대값 표시
- 추가 RED: 출력 경로를 원본과 같게 지정했을 때 종료0·원본 덮어쓰기 재현. 테스트의 TemporaryDirectory에서만 실행
- 최종 GREEN: 원본과 같은 출력 경로를 argparse 오류로 거부. **4개 통과·실패0·skip0**
- fixture는 실제 initial-v1 JSON과 실제 renderer를 임시 디렉터리에 복사. mock·별도 가짜 renderer 없음. 종료·파일 생성·선택 콘텐츠·기본 문서 보존·원본 보존을 검사
- 기본 renderer 실행 후 `docs/content/initial-v1/review.md` diff 없음
- 이름·책임 검토: 입력은 bundle, 출력은 review. 상태·판정 원본은 JSON. 이 작은 CLI 확장에 별도 클래스·서비스·검증 framework 불필요

## 콘텐츠 구조·출처 확인

```bash
python3 scripts/content_bundle.py --bundle docs/content/a1-v1/bundle.json --report /private/tmp/crackcs-a1-bundle-report.json
```

- 종료0. Topic별5문항·총30문항·64개 Concept·6문서, 모든 질문·문서·Concept 동일 Topic 연결, 문서의 필수 Concept 덮임, 가중치 합1.00 확인
- 원본 SHA-256: `4c08549e6563d13fd92dea9a1fded94eab675d283d28c1079ab18d0599e67beb`
- 로컬 본문 캐시 대조:22개 모두 HTTP200·Markdown UTF-8 hash 일치. Firecrawl 수집 확인일은2026-10-06 또는07로 원본에 기록
- 별도 작성 자료 구조 확인:30개 rubric의 코드 집합과 문항 기준 일치,150개 사례의 전체·개념 판정 값/코드 일치, 근거 누락 사례의 빈 제공 근거와 동일 답안 조건 확인
- 구조 확인은 진단 기대값의 의미 정확성·사람 승인·원문 이용 조건 확정이 아님
- `reference-v1` 정확한 문장 중복 없음. 최대 텍스트 유사도0.600. `A1-SQL-04`/`CS-07`, `A1-TX-02`/`CS-06`의 의미·개념 중첩 직접 확인. 새 독립 표본으로 간주하지 않음
- DB 기초의 두 교육 근거는 Berkeley CS186과 BCcampus 교재. 교재의 일부 단순화 표현을 그대로 옮기지 않고 함수 종속성·현재 사실·거래 이력 전제로 작성
- 404인 첫 교재/JUnit URL은 콘텐츠 출처에서 제외하고 실제 본문을 반환한 URL로 대체. CMU syllabus는 정규화 사실의 직접 근거로 사용하지 않음

## 보이는 PostgreSQL 예시 검증

- 호출 env: `CMUX_WORKSPACE_ID=9DAB6064-4C08-4296-BFF8-44946D90A5E0`, `CMUX_SURFACE_ID=F9735764-B074-452C-AA8A-E29F3D0F669B`
- `cmux identify --json`: caller workspace:2/surface:2. 시각 포커스 workspace:3는 대상으로 쓰지 않음
- caller workspace에 보조 pane 없어 오른쪽 pane:9/terminal surface:17을 `--focus false`로 생성·재사용. 명령은 `--workspace workspace:2 --surface surface:17` 명시
- scratch container: `crackcs-a1-evidence-20261007`, image `postgres:17-alpine`, 실제 버전 **PostgreSQL17.11 aarch64**. 호스트 port·운영 데이터 사용 없음

실행한 핵심 명령:

```bash
docker run --rm --name crackcs-a1-evidence-20261007 -e POSTGRES_HOST_AUTH_METHOD=trust -d postgres:17-alpine
docker exec crackcs-a1-evidence-20261007 pg_isready -U postgres
docker exec crackcs-a1-evidence-20261007 psql -U postgres -Atc 'select version()'
docker exec -i crackcs-a1-evidence-20261007 psql -U postgres -v ON_ERROR_STOP=1 < docs/content/a1-v1/postgres-examples.sql
docker stop crackcs-a1-evidence-20261007
```

- 결과: **17 assertion PASS·실패0·SQL_EXIT=0**. CHECK/UNIQUE의NULL, 필수값·FK 실패, LEFT JOIN0건, NOT IN/NOT EXISTS,120/20/40 집계, 고유 보조 정렬 확인
- 예시는 BEGIN·TEMP TABLE·ROLLBACK 사용. runner는 자신의 컨테이너만 종료·자동 삭제. 기존 다른 프로젝트 컨테이너 조작 없음
- 로그 `/private/tmp/crackcs-a1-postgres.log`. surface:17 화면에서 실제 NOTICE·ROLLBACK·SQL_EXIT 확인, 보조 pane 유지
- 미검증: 다중 session 경쟁·query 성능·API·앱 흐름·등록·검색·모델. 순차 SQL 예시를 concurrency 테스트나 앱 E2E로 표현하지 않음

## 공용 검증·PR 전달

- 공용 명령 `bash scripts/verify.sh all`: Java21·Node24.21.0·Docker 환경, 같은 cmux surface:17에서 실행. **ALL_EXIT=0**
- Python61·backend557·frontend339·PostgreSQL75 = **1,032 테스트 통과**, 실패·오류·skip0. renderer4개는 Python61에 포함; 중복 합산 없음
- 공식 스택 문서19개 PASS, backend clean build·ArchUnit·JAR, frontend type-check·production build, 실제 PostgreSQL 필수 suite 확인
- JUnit XML 재집계: backend557/0/0/0, PostgreSQL75/0/0/0. Vitest JSON:339 total·passed339·failed0·pending0·success true
- 로그 `/private/tmp/crackcs-a1-verify-all.log`. Gradle clean에 지워지지 않는 위치 사용. 완료 뒤 `build/reports/a1-v1/`에 공용 로그·SQL 로그·구조 보고서 복사
- 변경한 문서의 링크273개 검사·잘못된 로컬 대상0. 기본 initial-v1 원본·읽기 사본 변경 없음
- PR·main CI 결과는 전달 이후 관찰. native 보호 규칙 확인: PR·Actions app15368의 `CrackCS verify`, strict 최신 main·관리자 적용·강제 push/삭제 금지. 승인 인원0은 콘텐츠 사람 승인과 별개

## 미검증 경계·다음 행동

- 사람의 사실·허용 대안·150개 기대 판정·출처 이용 조건 검수
- 출시 Topic·활성 등록·노출 범위 결정, 관리자 DRAFT 등록·검수·공개
- 검색 chunk의 근거 덮임·실제 모델·독립 대표 답변·신고와 후속 학습
- 운영 host/TLS/비밀/비용/관측·schema version·배포/backup/restore·실제 참가자 파일럿

공용 검증·PR 머지는 위 승인·품질·운영 결과를 대신하지 않음. 다음 상태는 [tasks.md](../../planning/tasks.md)에서 관리.
