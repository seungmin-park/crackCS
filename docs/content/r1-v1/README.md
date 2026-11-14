# R1 기존 Topic 보강 콘텐츠

- 독자·질문: 콘텐츠 제작자·검수자 / 기존 Topic의 문항을 어떤 사례로 넓히고 무엇으로 대조했는가?
- 상태: DRAFT. 미등록·사람 검수 대기
- 수정 원본: [bundle.json](bundle.json). 읽기 사본: [review.md](review.md). 실행 결과: [verification.md](verification.md)
- 공개 기준: [콘텐츠 정책](../../product/content-and-ai-policy.md). 범위 기준: [커리큘럼](../backend-curriculum/README.md)

## 범위

17개 Topic·156문항·312개 Concept·17문서·120개 출처. BASIC 33, INTERMEDIATE 114, ADVANCED 9.

| Topic | 문항 | 기준 | 다루는 내용 |
|---|---|---|---|
| `JAVA` | 29 | Java 21 | 제네릭, 컬렉션, 문자 인코딩·입출력, 리플렉션·애노테이션, equals·String·박싱·enum, 날짜와 시간, 예외, 람다·스트림·Optional |
| `JVM_AND_CONCURRENCY` | 13 | Java 21, HotSpot | join·yield, 경쟁 상태, 락과 Condition, BlockingQueue, CAS, 동기화 컬렉션, 스레드 풀, 종료 절차, GC |
| `OBJECT_ORIENTED_DESIGN` | 9 | Java 21 설계 사례 | 의존관계 주입, 캡슐화, 책임 할당, 결합도·응집도, DIP, 상속과 조합, 컬렉션 캡슐화 |
| `SOFTWARE_ENGINEERING` | 7 | 언어 중립 | 가드 절, 추상화 수준, 이름, 상수, 주석, 과도한 설계, 변경 단위 |
| `SPRING_FRAMEWORK` | 12 | Spring Framework 7.0.x | DispatcherServlet 협력, 메시지 컨버터, PRG, 바인딩·검증, 필터와 인터셉터, 인자 리졸버, 주입 방식, AOP와 프록시 |
| `JPA` | 11 | Jakarta Persistence 3.2 | 연관 매핑, flush, 준영속, 스키마 생성, 컬럼 속성, 임베디드 타입, DTO 조회, 벌크 연산, fetch join, 소유 측 |
| `DATABASE` | 5 | PostgreSQL 17 | 다대다, 허용 값 제약, 삭제 표시, 커넥션 풀, 복제 |
| `TRANSACTIONS_AND_CONCURRENCY` | 5 | PostgreSQL 17 | 원자성, 교착, MVCC, 저장점, 행 잠금 |
| `SQL_AND_INDEXES` | 10 | PostgreSQL 17 | 조인, 인덱스 비용·선택도·구조, WHERE와 HAVING, 커버링 인덱스, COUNT, UNION, EXPLAIN, LIKE |
| `DATA_STRUCTURES` | 6 | Algorithms 4판 | 두 스택 큐, BST 높이·순회·검증, LRU, 균형 트리 |
| `ALGORITHMS` | 5 | Algorithms 4판 | 복잡도, 유니온-파인드, 다익스트라, 연결 요소, 이분 탐색 |
| `OPERATING_SYSTEM` | 9 | OSTEP | 프로세스 상태, 프로세스와 스레드, IPC, 세마포어, 교착 조건, 페이징, 스왑, 시스템 콜, 문맥 전환 |
| `NETWORKS` | 6 | RFC 1122·9293·768·1034 | 계층, TCP와 UDP, 연결 수립·종료, 접속 과정, 소켓 쌍 |
| `HTTP` | 5 | RFC 9110·9112·9113·6265 | 무상태, 안전한 메서드, 상태 코드, 쿠키와 세션, HTTP/2 다중화 |
| `TESTING` | 7 | 테스트 설계 일반 | 테스트 대역, 커버리지, TDD, 이음새, 격리, 시간 제어 |
| `SECURITY` | 8 | OWASP·RFC | 세션 고정, 비밀번호 저장, 파일 업로드, SQL 인젝션, JWT, OAuth 2.0, PKCE, CORS |
| `DELIVERY_AND_MIGRATION` | 9 | Docker·Kubernetes 문서 | 컨테이너, 리버스 프록시, 롤링·블루-그린·카나리, 스키마 변경, 빌드 캐시, CI, 코드형 인프라 |

`API_DESIGN`, `SPRING_BOOT`, `CS_FOUNDATIONS`, `COMPUTER_ARCHITECTURE`는 이 묶음에 없음. 기존 [initial-v1](../initial-v1/README.md)·[a1-v1](../a1-v1/README.md)·[a2-v1](../a2-v1/README.md) 문항만 있음. 관측·성능, 캐시, 메시징, 시스템 설계는 커리큘럼 B·C 묶음의 미제작 범위.

## 문항 형식

- 질문: 구체 상황 한 가지와 답할 내용. 합쇼체
- 모범 답안: 질문에 직접 답하는 서술. 평서체
- 필수 개념 2개, 가중치 각 0.50. 개념 설명은 답변이 갖춰야 할 내용
- 근거 문단: 문항마다 1개. 출처의 해당 절을 자기 문장으로 정리
- 혼동 주의: 흔한 오답 한 가지
- 출처 위치: 절 제목 또는 본문 문구. 작성 시점의 출처 본문에 그 문구가 있는지 대조
- rubric·진단 사례는 없음. A1·A2 수준의 판정 경계는 검수 단계에서 추가

## 기존 자료와의 중첩

같은 개념을 다른 사례로 다룬 문항. 문장 완전 중복은 없음.

| R1 문항 | 기존 자료 | 공유하는 판단 |
|---|---|---|
| R1-JPA-11 | reference-v1 JPA-08 | 컬렉션 fetch join과 결과 범위 지정 |
| R1-JPA-09 | reference-v1 JPA-07 | 지연 로딩 반복 조회 |
| R1-SPR-11 | reference-v1 SPRING-03 | 프록시 내부 호출 |
| R1-SPR-08 | initial-v1 SF-102 | 같은 타입 빈 여러 개의 주입 |
| R1-SQL-01, R1-SQL-07 | A1-SQL-01 | 외부 조인의 짝 없는 행과 COUNT |
| R1-TX-03, R1-TX-05 | A1-TX-02, A1-TX-05 | 스냅샷 읽기, FOR UPDATE |
| R1-DB-01 | A1-DB-01 | 연결 테이블의 키 구성 |
| R1-ALG-05 | A2-ALG-01 | 이분 탐색의 정렬 전제 |
| R1-NET-02 | A2-NET-02 | UDP의 앱 책임 |
| R1-TEST-06, R1-TEST-07 | A1-TEST-03, A1-TEST-04 | 테스트 정리 책임, 비결정적 실패 |
| R1-OS-09 | initial-v1 OS-105 | 타임 슬라이스와 전환 비용 |
| R1-JVM-05, R1-JVM-06 | initial-v1 OS-104 | 깨어난 뒤 대기 조건의 재확인 |

reference-v1과 겹치는 세 문항은 평가 표본과 학습 문항을 분리해야 할 때 제외 후보.

## 검사·읽기 자료 재생성

저장소 루트, Python 3 표준 라이브러리:

```bash
python3 scripts/content_bundle.py --bundle docs/content/r1-v1/bundle.json --report build/reports/r1-v1/bundle.json
python3 scripts/render_content_review.py --bundle docs/content/r1-v1/bundle.json
```

- 구조 검사: Topic·문항·문서·개념 연결, 출처 연결, Topic당 5문항 이상, 문장 중복, 가중치
- `--apply` 없는 실행은 앱에 등록하지 않음
- 검사 통과는 사실 정확도·사람 승인·검색·모델 품질을 대신하지 않음

## 사람 검수·공개 순서

1. [review.md](review.md)에서 질문 전제·답안·필수 개념·혼동 주의 확인
2. 출처의 해당 절과 답안·근거 문단 대조. 버전 의존 문항은 기준 버전 확인
3. 겹치는 기존 문항과 함께 볼지, 하나만 남길지 결정
4. 출시 Topic 선택. 기존 관리자 API로 DRAFT 등록 → 검수 → 공개 → 검색 문단 생성
5. 검색으로 근거가 제공되는지와 실제 모델 판정을 선택한 Topic에서 확인

승인 기록: **미확정**. PR 머지는 콘텐츠 공개 승인이 아님.
