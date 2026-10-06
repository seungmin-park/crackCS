# A2 제작·검증 기록

- 독자·질문: 제작자·검수자 / 어떤 원본을 무엇으로 확인했고 아직 무엇을 확인하지 않았는가?
- 확인일: 2026-10-07
- 작업 경로: `/Users/seungmin/Desktop/repo/crackCS/.worktrees/a2-content`
- 브랜치·기준: `docs/a2-content`, 최신 origin/main `f803b11`에서 분리
- 기준 원본: [bundle.json](bundle.json), SHA-256 `8077c1a2388e9e454ad72d98b8db6f71dd92f7b2e3213c5786bf038b5f648292`
- 산출물·검수 절차: [README](README.md), [읽기 사본](review.md)
- 전달 commit·PR·최종 CI: 해당 변경의 Git/PR 이력에서 관리. 이 문서의 로컬 검증과 콘텐츠 공개 승인은 별개

## 실제 제작 결과

- A2의 신규6개 Topic에 각5문항·10개 필수 Concept·1문서
- 총30문항·60개 Concept·6문서, BASIC9·INTERMEDIATE21
- 필수 개념 가중치0.50+0.50, 같은 Topic 안에서 연결. 미사용 개념·문서 근거 연결 누락 없음
- 문항별 사실 충족·일부 누락·모순·판정 보류 경계, 허용 대안과150개 작성자 진단 답변
- 26개 1차 출처의 본문·세부 절 대조. 수집 markdown과 기록한 SHA-256 대조
- 원문은 `.firecrawl/a2-20261007/`에 로컬 보존, Git 제외. 수집 도구의 분당 제한 발생분은 간격을 두고 재수집해 HTTP200·본문 존재 확인
- 최초 실행 가능한 번들 검사부터 통과. 콘텐츠 제작으로 앱·importer·renderer 동작 변경 없음. Production 기능의 RED/GREEN을 수행했다고 주장하지 않음

## 실행 명령·판정

모든 명령은 작업 경로의 저장소 루트. Java21·Node24.21.0 선택, Python3·Docker PostgreSQL 사용.

| 명령·검사 | 실제 결과 | 범위 |
|---|---|---|
| `python3 -m unittest discover -s scripts -p 'test_content_bundle.py' -v` | 14/14, exit0 | 변경 전 기존 validator 기준 확인 |
| `python3 scripts/content_bundle.py --bundle docs/content/a2-v1/bundle.json --report build/reports/a2-v1/bundle.json` | exit0, 30·60·6 | 문항 최소량·가중치·출처·문서·Topic 연결·문장 중복 |
| `python3 scripts/render_content_review.py --bundle docs/content/a2-v1/bundle.json` | exit0, 30문항·6문서 | 선택 묶음의 읽기 사본 생성 |
| 원본·진단·수집본 대조 | 26/26 hash·150/150 진단 연결·원본 hash 일치 | 구조·라벨 연결. 사실 정확도·예상 판정의 사람 승인 아님 |
| `python3 docs/content/a2-v1/worked-examples.py` | 45 assertion, exit0 | 작은 입력의 계산·알고리즘 사례 |
| `java --source 21 docs/content/a2-v1/JavaExamples.java` | 실제 Java21.0.7, 3 assertion, exit0 | int 연산 후 확대와 계산 전 long 확대 |
| `bash scripts/verify.sh all` | 총1,032개 성공, exit0 | Python61·backend557·frontend339·PostgreSQL75, 실패·오류·skip0 |
| `python3 scripts/check_stack_docs.py` | 19개 PASS, exit0 | 공식 문서·기술 스택 목록 일치. 의존성 변경 없음 |
| 읽기 사본 재생성·로컬 링크·`git diff --check` | 내용 일치·참조 파일 존재·공백 오류0 | 문서 재현성과 diff 확인 |

Java 실행 경로: `/Users/seungmin/Library/Java/JavaVirtualMachines/azul-21.0.7/Contents/Home/bin/java`. source-launch 예제를 기존 Gradle JUnit 개수에 합산하지 않음. Python45·Java3 assertion은 공용1,032개와 별도.

공용 검증: clean backend build·구조/HTTP/H2 테스트, frontend test·type-check·production build, 별도 PostgreSQL 계약 검사 포함. 앱 브라우저 사용자 흐름·실제 모델 실행을 포함하지 않음.

### cmux 표시와 로그 복구

- `CMUX_WORKSPACE_ID=9DAB6064-4C08-4296-BFF8-44946D90A5E0`, `CMUX_SURFACE_ID` 미설정
- `cmux identify --json`: caller=`workspace:2`/`surface:2`, focused도 동일. 포커스만으로 타 workspace 선택하지 않음
- 기존 오른쪽 보조 `pane:9`/`surface:17`의 종료된 검증 터미널 재사용. `--workspace workspace:2 --surface surface:17` 명시, 포커스 변경·pane 폐쇄 없음
- 실행: `cmux send --workspace workspace:2 --surface surface:17 'bash /private/tmp/crackcs-a2-verify-all.sh\n'`
- wrapper가 Java21·Node24를 선택해 `bash scripts/verify.sh all`의 명령·로그 표시
- 검증 중 `clean`이 로그 폴더를 삭제해 wrapper 마지막 exit 파일 저장 실패. 러너 종료0과 세 job의 PASS는 화면에 남음
- `cmux read-screen --workspace workspace:2 --surface surface:17 --scrollback --lines 20000`으로 로그 확보. 이번 wrapper 호출 뒤만 잘라 `verify-all.log`로 저장하고 화면의 종료0을 근거로 exit 파일 복구
- 최신 JUnit XML·Vitest JSON 직접 재확인: backend557·frontend339·PG75, 실패·오류·skip0. 이전 화면의 테스트 수로 대체하지 않음
- 최초 소켓 읽기의 샌드박스 접근 제한은 승인된 재실행으로 해소. 종료된 보조 pane 유지

로컬 보고서:

- `build/reports/a2-v1/bundle.json`, `audit.json`: 구조·hash·진단 연결·문자 유사도
- `build/reports/a2-v1/examples.json`, `java-examples.log`: 예제 실제 assertion
- `build/reports/a2-v1/verify-all.log`, `verify-all.exit`: 이번 공용 검증 화면·종료 코드
- `build/test-results/test/`, `build/test-results/postgresTest/`, `front/test-results/vitest.json`: 실제 자동 테스트 결과

## 중복·설계 검토

- initial-v1·A1·reference-v1과 정규화한 질문 문장 완전 중복0건
- reference-v1의 최근접 문자 유사도 최대0.491. 의미 독립성의 증거 아님
- CS-10/08/09/04/03과 자료구조·BFS·TCP·주소 변환 개념 중첩. 기존 Java 불변성·A1 업무 중복 계약과도 공유 개념 존재. [중첩 목록](README.md#기존-자료와의-중첩)에서 관리
- 공유 개념·이번 합성 답변을 독립 golden·대표 학습자 표본에서 분리. OS 모델 결과로 A2 품질을 추정하지 않음
- 상태·정보 소유: 기간/주문의 불변식은 상태 소유 객체, 할인 계산은 정책, 전송 형식은 adapter, 조회 유스케이스는 업무 계약으로 구분
- 객체 설계의 허용 대안: 불변 값 교체·전체 검증 후 수정, 함수/객체 정책, 원계약 보존·별도 능력 계약. 패턴 이름 하나를 유일한 정답으로 강제하지 않음
- example 이름과 수행 내용 대조: `first_match` 첫 위치/없음, `minimum_coins` 최소 개수, `bfs_distances` 간선 거리, `stable_merge` 동점 순서 유지. 학습 코드이며 앱 책임 구조의 리팩터링 아님

## 미검증·실패 조건

- 사람 사실·이용 조건 검수, 진단 예상값의 독립 승인, 실제 학습자 표본 미확보
- 앱 taxonomy·DRAFT 등록·검수·공개·검색 문단 생성 미실행. 기존 앱 데이터 변경 없음
- 실제 모델 판정·검색 chunk의 필수 근거 덮임·후속 학습·브라우저 E2E·운영 품질 미검증
- 사례 코드는 작은 입력의 관찰. 전체 입력의 정확성·복잡도 증명·실제 네트워크·캐시 benchmark·앱 도메인 실행 검사 아님
- version·가중치·Topic 연결·필수 근거·hash 불일치: 구조 확인 실패 후 원본/출처 보강
- 질문 밖 언어·ISA·TTL 정책·TLS 모드·업무 계약: 기존 정답을 자동 적용하지 않고 전제·근거 재검토
- 필수 근거 없음: NEEDS_REVIEW. 명백한 모순: INCORRECT. 일부 누락과 모순을 같은 상태로 취급하지 않음
- 원형 큐·정책·adapter 등 유효한 대안의 조건이 불명확하면 수정·보류. 구조·CI 통과만으로 공개하지 않음
