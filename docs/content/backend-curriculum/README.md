# 백엔드 기반 지식 확장 커리큘럼

- 확인일: 2026-10-06
- 독자·질문: 학습자·콘텐츠 제작자 / 국내 Java·Spring 백엔드 입문부터 경력까지 무엇을 어떤 순서·깊이로 공부할 것인가?
- 상태: **범위·제작 계획**. 새 Topic·Concept·문항·문서의 관리자 등록·검수·공개 미실행
- 범위 근거: [시장조사](market-research.md), [출처 기록](sources.json)
- 품질·공개 기준: [콘텐츠 정책](../../product/content-and-ai-policy.md). 진행 상태: [작업 목록](../../planning/tasks.md)

## 목표와 현재 상태

Java·Spring·JPA 사용법에 CS·웹·DB·객체 설계·테스트·운영 원리를 연결. 사용자가 **현상 → 원인 → 선택 이유 → 실패 조건 → 확인 방법**을 자기 언어로 설명할 수 있는 범위.

현재와 계획 구분:

| 구분 | 상태 |
|---|---|
| initial-v1 원본 | 기존 5개 Topic·25문항·50개 Concept·5문서의 DRAFT 등록 템플릿. [원본·등록 안내](../initial-v1/README.md) |
| 로컬 승인·공개 | OS·Java 각5문항·1문서, 총10문항·20개 Concept·2문서. [OS 실제 GPT·버전 보존](../../changes/2026-10-05-os-content/verification.md), [Java 합성 provider 흐름](../../changes/2026-10-05-java-content-flow/verification.md) |
| 기존 검수 대기 | Spring Framework·Spring Boot·JPA 15문항·3문서 |
| 확장 핵심 | 기존5개를 포함한 **28개 말단 Topic**의 제작 계획. 추가23개 Topic의 콘텐츠 미제작 |
| 선택 확장 | Kotlin 상호 운용·AI 개발 검증 **2개 Topic**. 핵심28개와 별도 |

이 문서는 제작할 지식 범위의 기준. 전체28개 완성이 제한 파일럿의 자동 선행 조건은 아님. 출시 대상으로 선택한 leaf Topic만 OQ-006 수량·검수·근거·평가 Gate 적용. 신규 Topic을 단순 활성 등록하면 지식 지도에 미평가 범위가 늘 수 있으므로 등록·노출도 출시 묶음과 함께 검토.

자료구조·알고리즘은 선택 이유, 실행 과정, 복잡도, 경계 입력을 설명하는 서술형 범위. 코드 실행·온라인 저지·실제 개발 경력 인증은 별도 기능·검증 범위.

## 학습 구조와 깊이

```text
수학·컴퓨터구조 ─→ OS ─→ JVM·동시성 ───────────┐
      └─→ 자료구조 ─→ 알고리즘                │
네트워크 ─→ HTTP ─→ API·보안 ──────────────────┤
Java ─→ 객체 설계 ─→ Spring ─→ Boot           ├─→ 테스트·배포·관측
DB 모델 ─→ SQL·인덱스 ─→ 트랜잭션 ─→ JPA ────┘          │
                    └─→ 캐시·메시징 ─→ 분산·시스템 설계 ─┘
```

화살표는 권장 선수 지식. 학습자가 전부 순서대로 통과해야 하는 기능은 현재 미구현. 테스트는 마지막 단계에만 배우는 항목이 아니라 모든 층의 가정을 확인하는 공통 축.

| 학습 깊이 | 답변에서 확인할 결과 | 예: 트랜잭션 |
|---|---|---|
| BASIC·기초 | 개념·상태·실행 과정·반례를 정확히 설명 | 원자성과 격리 차이, 동시에 잔액을 읽고 쓸 때 생기는 현상 |
| INTERMEDIATE·적용 | 요구조건을 읽고 대안을 선택·검증 | 원자적 SQL·낙관/비관 잠금을 충돌률·재시도 비용과 비교 |
| ADVANCED·심화 | 장애·규모·팀 제약에서 트레이드오프와 복구 설계 | DB commit 전후 외부 결제 실패, 멱등 처리·보상·대사 선택 |

연차에 따른 강제 학습 등급 없음. 취업 준비자는 기초 설명과 작은 사례, 주니어는 코드·SQL·HTTP·테스트 경계 해석, 경력자는 운영 증거·변경 위험·대안 판단에 중점. 경력자도 부족한 기초부터 보강 가능.

## 핵심 28개 Topic

표의 code는 등록 때 사용할 제안. 기존 `OPERATING_SYSTEM`, `JAVA`, `SPRING_FRAMEWORK`, `SPRING_BOOT`, `JPA` code 유지. 새 code의 중복 여부·등록 가능성은 실제 등록 전에 확인. 각 Concept의 구체 ID·가중치·문항 수는 후속 제작에서 확정.

### CS 기반 — 7개

| code·주제 | 다룰 개념 | 설명·판단 목표와 문항 방향 | 주요 출처 |
|---|---|---|---|
| `CS_FOUNDATIONS` 수학·표현 기반 | 비트·바이트·수의 표현, 논리·집합, 로그·지수, 확률·평균·백분위, 단위 | 메모리 크기·시간복잡도·p95 해석의 단위와 가정 설명. 평균이 같은 두 지연 분포가 다른 이유 | [CS2023 수학·시스템 영역](https://csed.acm.org/knowledge-areas/) |
| `COMPUTER_ARCHITECTURE` 컴퓨터구조 | CPU·명령·메모리 계층, 캐시·지역성, 저장장치·I/O, 가상화 기반 | 연속 배열과 포인터 연결 순회의 차이를 지역성으로 설명. CPU·메모리·I/O 병목 구분 | [CS2023 구조·시스템 영역](https://csed.acm.org/knowledge-areas/) |
| `DATA_STRUCTURES` 자료구조 | 배열·연결리스트, 스택·큐·덱, 해시, 트리·힙, 그래프 | 접근·검색·삽입 비용과 충돌·메모리 비용 비교. 상위 K개를 유지할 자료구조 선택 | [MIT 6.006](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/) |
| `ALGORITHMS` 알고리즘 | 복잡도·상환 분석, 정렬·탐색, 재귀, BFS/DFS, 그리디·DP, 정확성·경계 입력 | ‘빠르다’ 대신 입력 크기·최악 조건 설명. 최단 경로에서 BFS가 유효한 간선 조건과 반례 | [MIT 6.006](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/) |
| `OPERATING_SYSTEM` 운영체제·기존 | 프로세스·스레드·스케줄링, 동기화·교착, 가상 메모리·페이지, 파일·I/O | 경쟁 상태의 실행 순서, 병렬성·동시성 차이, CPU/대기 시간 설명. 기존5문항 위에 심화 확장 | [OSTEP](https://pages.cs.wisc.edu/~remzi/OSTEP/) |
| `NETWORKS` 네트워크 | 계층·패킷, IP·라우팅, TCP/UDP·연결·흐름/혼잡 제어, DNS·TLS, 프록시·LB | DNS 실패·연결 실패·응답 지연 구분. 연결을 다시 열면 비용과 실패 위치가 어떻게 달라지는가? | [Stanford CS144](https://cs144.github.io/), [HTTP 의미](https://www.rfc-editor.org/rfc/rfc9110.html) |
| `DATABASE` DB 모델·기반 | 관계형 모델, 키·제약·정규화, 관계·참조 무결성, 데이터 소유·보존, RDB/NoSQL 선택 | 중복 주문과 고아 레코드를 어느 제약으로 막는가? 애플리케이션 검사와 DB 최종 방어 구분 | [PostgreSQL 17 DDL](https://www.postgresql.org/docs/17/ddl.html), [CS2023 데이터 관리](https://csed.acm.org/knowledge-areas/) |

### Java·객체 설계 — 3개

| code·주제 | 다룰 개념 | 설명·판단 목표와 문항 방향 | 주요 출처 |
|---|---|---|---|
| `JAVA` Java·기존 | 타입·값/참조, 동등성·해시, 컬렉션·제네릭, 예외, 불변성, Stream·자원 관리 | equals/hashCode 계약과 가변 키의 문제, checked/unchecked 처리 경계. 기존5문항에서 범위 확대 | [Java 21 JLS](https://docs.oracle.com/javase/specs/jls/se21/html/index.html) |
| `JVM_AND_CONCURRENCY` JVM·동시성 | 로딩·실행·메모리·GC, Java Memory Model, 가시성·원자성, lock·executor, virtual thread·pool | GC/CPU/대기 병목 구분. volatile과 복합 연산, virtual thread와 DB connection 제한의 차이 | [JLS 21](https://docs.oracle.com/javase/specs/jls/se21/html/index.html), [JVMS 21](https://docs.oracle.com/javase/specs/jvms/se21/html/index.html) |
| `OBJECT_ORIENTED_DESIGN` 객체 지향·도메인 설계 | 캡슐화·불변식, 책임·협력, 다형성·합성, 응집·결합, SOLID·DDD 기초, 계층·포트 경계 | 누가 상태와 판단 정보를 아는가? 할인 정책 변경 때 어떤 객체가 바뀌는가? setter 대신 의도 기반 변경 선택 | [JLS 타입·클래스](https://docs.oracle.com/javase/specs/jls/se21/html/index.html), [Spring IoC](https://docs.spring.io/spring-framework/reference/7.0/core/beans.html). 설계 판단은 사례별 검수 |

### HTTP·API·Spring·보안 — 5개

| code·주제 | 다룰 개념 | 설명·판단 목표와 문항 방향 | 주요 출처 |
|---|---|---|---|
| `HTTP` HTTP | 요청·응답, 메서드·상태, 안전성·멱등성, 헤더·쿠키, 캐시·조건부 요청, 버전별 의미/전송 구분 | 멱등성은 응답 동일성과 같은가? timeout 후 재시도 가능 조건, ETag·Cache-Control 해석 | [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), [RFC 9111](https://www.rfc-editor.org/rfc/rfc9111.html) |
| `API_DESIGN` API·REST | 리소스·표현·REST 제약, URI·오류 계약, pagination·필터, 호환성·version, 멱등 키, OpenAPI·RPC 비교 | REST의 uniform interface와 CRUD 구분. 생성 요청 재전송·cursor 변경·오류 상태 선택의 이유 | [Fielding REST](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm), [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html) |
| `SPRING_FRAMEWORK` Spring Framework·기존 | IoC/DI·Bean scope·생명주기, MVC·validation, AOP·proxy, transaction·전파·rollback | DI가 생성 책임을 어떻게 바꾸는가? proxy 모드 내부 호출의 적용 경계. 기존5문항에 MVC·tx 확장 | [Spring 7.0 IoC](https://docs.spring.io/spring-framework/reference/7.0/core/beans.html), [Transaction](https://docs.spring.io/spring-framework/reference/7.0/data-access/transaction/declarative/annotations.html) |
| `SPRING_BOOT` Spring Boot·기존 | 자동 구성·조건·설정 우선순위, profile·외부 설정, 서버 기동·종료, health·운영 설정·테스트 구성 | 자동 구성이 언제 물러나는가? 준비 상태와 생존 상태를 잘못 연결한 장애. 기존5문항 검수와 확장 | [Spring Boot 4.1](https://docs.spring.io/spring-boot/4.1/reference/index.html), [현행 스택 기준](../../engineering/stack-docs.md) |
| `SECURITY` 백엔드 보안 | 인증/인가·소유권, session/token·CSRF·CORS, XSS·SQL injection, 비밀·로그·입력 제한, 의존성·위협 모델 | 로그인됐지만 타인의 데이터 요청이면? CORS와 인증의 역할, 서버 최종 방어·실패 응답 선택 | [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/), [REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html) |

### 데이터 접근·정합성·처리 — 6개

| code·주제 | 다룰 개념 | 설명·판단 목표와 문항 방향 | 주요 출처 |
|---|---|---|---|
| `SQL_AND_INDEXES` SQL·인덱스·쿼리 | JOIN·집계·서브쿼리·윈도 함수, NULL, B-tree·복합 인덱스·선택도, 실행 계획, pagination·쿼리 비용 | 인덱스를 추가해도 느린 조건은? 필터·정렬·데이터 분포·쓰기 비용을 함께 비교 | [PostgreSQL Query](https://www.postgresql.org/docs/17/queries.html), [Indexes](https://www.postgresql.org/docs/17/indexes.html) |
| `TRANSACTIONS_AND_CONCURRENCY` DB 트랜잭션·경쟁 | ACID, 격리·MVCC, 이상 현상·잠금, 낙관/비관·재시도·교착, DB와 외부 부수 효과 경계 | 동시에 읽은 재고를 갱신할 때 누락되는 조건은? 정합성 보장과 처리량·경쟁 비용 비교 | [PostgreSQL 17 격리](https://www.postgresql.org/docs/17/transaction-iso.html), [Spring tx 경계](https://docs.spring.io/spring-framework/reference/7.0/data-access/transaction/declarative/annotations.html) |
| `JPA` ORM·영속성·기존 | 매핑·연관 관계 소유, persistence context·변경 감지, flush·fetch·N+1, locking·bulk·pagination, SQL 매퍼와 비교 | 조회 결과·SQL·transaction 종료 시점을 연결. fetch join·batch·projection 선택의 제약. 기존5문항 검수·확장 | [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2), [MyBatis 비교](https://mybatis.org/mybatis-3/) |
| `CACHE_AND_REDIS` 캐시·Redis | cache-aside·TTL·eviction, hit·stale·stampede, invalidation, 자료구조·원자 연산, 복제·잠금 한계 | 캐시 삭제와 DB commit 사이 경쟁, TTL 종료 뒤 작업 재개. 최종 원본과 정합성 소유자 설명 | [Redis 잠금·장애 조건](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/), [HTTP 캐시와 구분](https://www.rfc-editor.org/rfc/rfc9111.html) |
| `MESSAGING_AND_KAFKA` 메시징·Kafka | queue/pub-sub·log, partition·순서, consumer group·offset, 재처리·중복·DLQ, delivery semantics·outbox | DB 저장 뒤 offset commit 전 중단되면? 외부 효과의 멱등성과 Kafka 보장 범위 구분 | [Kafka 4.1 Design](https://kafka.apache.org/41/design/design/) |
| `BATCH_AND_DATA_PROCESSING` 배치·데이터 처리 | job/step·chunk, checkpoint·재시작·skip/retry, 중복·대사, 스케줄·대량 데이터·자원 제한 | 정산 중간 실패 뒤 재실행 시 이미 처리한 건의 중복 방지. 처리 단위·commit·복구 지점 선택 | [Spring Batch](https://docs.spring.io/spring-batch/reference/) |

### 개발 품질·운영 — 5개

| code·주제 | 다룰 개념 | 설명·판단 목표와 문항 방향 | 주요 출처 |
|---|---|---|---|
| `TESTING` 테스트 | 단위·통합·HTTP 계약·E2E, TDD·회귀, mock/fake 경계, 데이터 격리·정리, concurrency·load·fault, 테스트 신뢰성 | 어떤 실패를 어느 수준에서 잡는가? 전부 mock인 green 결과가 실제 DB 계약을 증명하지 못하는 이유 | [Spring Testing](https://docs.spring.io/spring-framework/reference/7.0/testing.html), [JUnit](https://docs.junit.org/current/user-guide/) |
| `SOFTWARE_ENGINEERING` 변경·협업·빌드 | Git·branch·conflict, build·의존성·버전, 코드 리뷰·문서·ADR, 리팩터링·모듈 경계, 품질·기술 부채 | 작은 변경의 검증·되돌리기 단위 선택. 충돌 해결 뒤 의미를 다시 확인할 방법 | [Git Book](https://git-scm.com/book/en/v2), [CS2023 SE](https://csed.acm.org/knowledge-areas/) |
| `LINUX_AND_TROUBLESHOOTING` Linux·진단 | 프로세스·signal·파일/권한, socket·port·로그, CPU·메모리·디스크·네트워크 관찰, 제한·종료 | 앱이 느릴 때 무엇을 먼저 관찰하는가? 추정과 관측을 분리하고 장애 층 좁히기 | [Linux admin guide](https://www.kernel.org/doc/html/latest/admin-guide/index.html), [OSTEP](https://pages.cs.wisc.edu/~remzi/OSTEP/) |
| `DELIVERY_AND_MIGRATION` 배포·변경·복구 | CI/CD·artifact·container, cloud/IAM·네트워크 기본, health·rolling, schema expand/contract·데이터 이관, backup/restore·rollback | 구버전 앱이 남은 배포 중 column 삭제 가능 조건. 앱 rollback과 데이터 복구 차이 | [Docker Build](https://docs.docker.com/build/building/best-practices/), [Kubernetes Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/), [AWS Reliability](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html) |
| `OBSERVABILITY_AND_PERFORMANCE` 관측·성능 | logs/metrics/traces·상관관계, 지연·처리량·오류·포화, SLI/SLO·알림, profile·pool·query·부하 조건 | p95 상승 원인이 CPU인가 connection 대기인가? 측정 분모·환경·전후 비교로 가설 검증 | [OpenTelemetry Signals](https://opentelemetry.io/docs/concepts/signals/), [AWS Reliability](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html) |

### 경력 설계 심화 — 2개

| code·주제 | 다룰 개념 | 설명·판단 목표와 문항 방향 | 주요 출처 |
|---|---|---|---|
| `DISTRIBUTED_SYSTEMS` 분산·신뢰성 | 부분 실패·timeout·retry/backoff, 멱등·중복, 복제·일관성, CAP 적용 조건, 분산 transaction·보상, circuit breaker·bulkhead | 결제 timeout이 실패 확정인가? 재시도 폭주·불확실 상태·복구 책임을 설계 | [AWS Reliability](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html), [Kafka](https://kafka.apache.org/41/design/design/), [Redis](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/) |
| `SYSTEM_DESIGN` 시스템 설계 | 요구·부하 추정, 데이터·도메인 경계, monolith/MSA, 수평 확장·replication/sharding, 일관성·가용성·비용, 점진 이관·리뷰 | 주문 서비스 요구가 변할 때 병목·책임·대안·운영 비용 설명. MSA가 필요한 조건과 불필요한 조건 | [AWS Reliability](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html), [CS2023 분산·SE](https://csed.acm.org/knowledge-areas/). 정답 하나 대신 제약별 rubric |

### 선택 확장 — 2개

| code·주제 | 범위·채택 이유 | 제작 전에 필요한 근거 |
|---|---|---|
| `KOTLIN_INTEROP` Kotlin·JVM 상호 운용 | nullability·Java interop·data class·coroutine·transaction/thread 경계. Java/Kotlin 병행 공고 대응 | 선택 트랙 수요 확인, 사용할 Kotlin·coroutine 공식 문서 버전 선정·본문 대조. [채용 신호](market-research.md#판단-4--kotlinai-개발-활용은-선택-확장에-포함) |
| `AI_ASSISTED_ENGINEERING` AI 개발·검증 | 생성 코드의 요구·테스트·의존성 검토, 비밀·데이터 경계, 재현 가능한 검증·리뷰·도구 권한 | 실제 사용자 수요, 사용할 도구별 공식 계약·정보 정책 확인. LLM 전문 서비스 구축은 별도. [채용 신호](market-research.md#판단-4--kotlinai-개발-활용은-선택-확장에-포함) |

핵심 표의 공식 문서는 해당 원리의 시작 자료. 모든 나열 개념의 근거 문장이 확보됐다는 의미는 아님. 실제 KnowledgeDocument 제작에서는 세부 절·버전·조건을 추가 확인. Git·Java 명세만으로 모든 설계 원칙의 유효성을 주장하지 않음.

## 문항을 만들 때의 직관

개념 이름 암기 → 실제 상황의 원인 설명으로 전환. 예시 아래는 **문항 설계 초안**, 승인된 reference answer 또는 평가 정답이 아님.

```text
현상·입력·실행 순서 제시
         ↓
상태를 누가 소유하고 어디서 변하는지 설명
         ↓
왜 발생하는지·안 발생하는 조건 구분
         ↓
대안 선택 + 비용·실패 조건 + 확인 방법
```

| 사례·평가 Topic | 제시 상황 | 필수 Concept 후보·답변에서 볼 근거 | 허용 대안·추가 조건 |
|---|---|---|---|
| Q01 `TRANSACTIONS_AND_CONCURRENCY` | 두 요청이 재고1을 읽고 각각 주문 후 재고0을 저장 | 읽기·쓰기 interleaving, invariant, 격리·원자 갱신, 충돌 결과 확인 | 조건부 UPDATE·잠금·version 대안 비교. DB/격리 수준 명시 없이 특정 잠금 동작 단정 금지 |
| Q02 `HTTP` | DELETE 재전송의 첫 응답204, 두 번째404 | 의도한 서버 효과의 멱등성과 응답 동일성 구분, 재전송 조건 | 응답이 다르다는 이유만으로 비멱등 판정 금지. 로그 등 부수 기록과 의도한 효과 구분. [RFC 9110 §9.2.2](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.2) |
| Q03 `API_DESIGN` | 주문 생성 응답이 유실돼 클라이언트가 POST 재전송 | 요청 식별·동일 키/내용·저장 결과·충돌·재조회 계약 | API 멱등 키 전략은 설계 제안. 고유성·동시 요청·보존 기간·외부 결제까지 별도 검증 |
| Q04 `SPRING_FRAMEWORK` | proxy 모드에서 한 Bean의 메서드가 같은 객체의 tx 메서드 호출 | 호출 경로·proxy·interceptor·기존 transaction 유무 | 내부 호출이 새 advice를 적용하지 않는다는 의미. 기존 tx 안에서 실행 가능한 조건 구분. [Spring tx](https://docs.spring.io/spring-framework/reference/7.0/data-access/transaction/declarative/annotations.html) |
| Q05 `OBJECT_ORIENTED_DESIGN` | Service가 setter 여러 번 호출하다 입력 오류로 중간 상태 노출 | 불변식 소유자, 전체 검증 뒤 변경, 의도 메서드·예외 후 상태 | 불변 객체·도메인 행위 대안 허용. 클래스 수·패턴 이름으로 품질 판정 금지 |
| Q06 `TESTING` | Repository를 전부 mock한 테스트 통과, 실제 DB에서 FK 오류 | 대체한 경계, 테스트 목적·수준, 실제 DB 계약·fixture/teardown | 단위 테스트의 가치와 DB 미검증 구분. 같은 구현을 복제한 assertion만으로 보강 완료 판정 금지 |
| Q07 `JPA` | 목록 조회에서 항목 수에 따라 연관 조회 SQL 증가 | lazy/fetch 경계·query 수·pagination 제약·transaction 범위 | fetch join·batch·projection을 요구에 따라 비교. ORM·DB/provider 버전·계획 추가 확인 |
| Q08 `CACHE_AND_REDIS` | DB 변경과 캐시 무효화 사이 이전 값이 재적재 | 원본·캐시 소유, 실행 순서·stale 기간, 실패·관측 | TTL·version·무효화 순서별 비용 비교. 모든 쓰기의 강한 정합성을 캐시만으로 보장한다고 채점 금지 |
| Q09 `MESSAGING_AND_KAFKA` | 외부 DB 저장 성공 뒤 offset commit 전 consumer 중단 | 재전달 가능성·offset·처리 기록·외부 효과 멱등 | unique 처리 키·원자적 저장 등 선택 가능. Kafka 내부 보장을 외부 결제 전체 보장으로 확장 금지 |
| Q10 `DISTRIBUTED_SYSTEMS` | 결제 서버가 처리했지만 응답이 timeout, 호출자가 재시도 | 결과 불확실·중복·멱등 키·상태 조회·보상·대사, 시간·횟수 제한 | ‘timeout=실패 확정’ 오개념 탐지. 제공자 계약과 실제 중단 지점 명시 |

Topic을 넘는 맥락은 사례 설명·권장 학습 링크로 연결. **현재 `Question.addConcept`는 다른 Topic의 Concept 연결을 거부**: [코드](../../../src/main/java/com/example/crackcs/content/question/domain/Question.java). 한 문항의 필수 Concept·근거 문서는 해당 Topic 안에서 구성. 같은 개념을 다른 Topic에 복제해 지식 상태를 갈라놓지 않음. 복합 Topic 동시 채점이 필요하면 별도 기능 요구·설계·검증부터 진행.

## 콘텐츠 제작 순서

우선순위는 제작 비용과 현재 누락 범위를 고려한 제안. 권장 공부 순서와 동일하지 않음. 한 번에 전체 묶음을 등록·공개하지 않고 검수 가능한 작은 묶음으로 진행.

| 묶음 | 대상 | 다음 결과·완료 조건 |
|---|---|---|
| 기존 콘텐츠 정리 | OS·Java 승인본과 Spring Framework·Boot·JPA 검수 대기본 | 승인 이력 유지. 기존3개 Topic 사실·출처·라이선스·필수 Concept 검수. 실제 모델 품질은 합성 흐름과 구분 |
| A1·기본 서버 동작 | DB 모델, SQL/인덱스, DB tx, HTTP, API 설계, 테스트 — 신규6개 | 필요한 선수 설명 보강 → Concept → 근거 문서 → 문항·rubric 초안 → 사람 검수 → 선택한 출시 묶음 공개·평가 검증 |
| A2·CS와 설계 기반 | 수학/표현, 컴퓨터구조, 자료구조, 알고리즘, 네트워크, 객체 설계 — 신규6개 | 기초 설명·실행 과정·반례를 사례형 문제에 연결. A1과 병행 제작 가능하나 무검수 자동 공개 없음 |
| B·품질·운영 | JVM/동시성, 보안, 변경/협업, Linux/진단, 배포/이관, 관측/성능 — 신규6개 | 코드·로그·측정 자료를 읽는 서술형 사례. 설치 명령 암기와 실제 운영 능력 인증 구분 |
| C·경력 심화 | 캐시, 메시징, 분산, 시스템 설계, 배치 — 신규5개 | 장애 위치·제약·보장 범위·대안·복구가 명확한 rubric. 복수 정답·조건부 정답 검수 |
| D·선택 | Kotlin·AI 개발 — 별도2개 | 수요·공식 근거 확인 뒤 제작. 핵심 출시의 자동 조건으로 추가하지 않음 |

기존5 + A1 6 + A2 6 + B 6 + C 5 = 핵심28. 선택 D2 별도.

### 묶음마다 필요한 산출물

1. Concept 목록: 정의·구분·흔한 오개념·판단 조건. 개념 하나의 상태 소유 Topic 지정
2. KnowledgeDocument: 자기 문장으로 정리한 근거, 절 위치·적용 버전·출처·이용 조건·검수자·검수일
3. 문항: 입력·제약·기대 설명·필수/보조 Concept·난이도·허용 대안
4. reference answer/rubric: 필수 사실, 핵심 누락, 모순, 조건부 허용·복수 대안, 판단 보류 조건
5. 평가 사례: 정답·부분 정답·오답·근거 부족·다른 표현·조건 차이. 개발에 쓴 사례와 독립 대표 표본 분리
6. 관리자 등록·검수·공개: 기존 API 사용, 원본 버전·과거 평가 근거 보존
7. 공개 검증: 선택 Topic OQ-006·실제 학습 흐름·모델 품질·신고 처리. 로컬 E2E는 현재 cmux에서 표시

OQ-006의 Topic당5문항·1문서는 **공개 최소량**. 주제 전체를 충분히 학습·평가했다는 보장 아님. 28개 전체를 대상으로 선택하면 최소140문항·28문서가 필요한 산술 하한이나, 필수 Concept 덮임·난이도·오개념·대표 품질에 따라 추가 제작 필요. 기존 승인10문항을 빼서 나머지의 제작·검수 완료를 추정하지 않음.

### 버전과 실패 조건

- 기존 Java 21·Framework 7.0.x·Boot 4.1.x·Persistence 3.2 기준 유지. 정확한 설치 버전은 [stack-docs](../../engineering/stack-docs.md) 소유
- PostgreSQL 17 설명은 해당 DB의 동작. MySQL 등의 격리·인덱스 차이는 별도 공식 근거·버전 확인 후 비교
- JUnit `current` 조사 본문은 6.1.3. Kafka4.1·Batch·Linux 등 조사 자료를 저장소 의존성 도입 또는 변경으로 해석하지 않음
- HTTP 의미, ORM 구현, Spring proxy/AspectJ, test-level/server transaction의 조건을 생략하면 정답이 달라질 수 있으므로 문항 수정 또는 판단 보류
- 단일 정답을 강제할 수 없는 설계 문제: 조건·허용 대안·비용을 rubric에 명시. 유효한 대안을 오답 처리하면 공개 보류
- 공식 출처라도 전문 복사·번역 재배포를 자동 허용하지 않음. 문서별 이용 조건 검수
- 출처는 있으나 필수 Concept 근거가 부족한 경우 공개·자동 정답 확정 보류. 문항·문서 보강 후 재검증

## 배포·운영으로 넘어가기 전의 관계

```text
이번 작업: 수요 근거 + 확장 범위 + 제작 순서 확정
                         ↓
출시 묶음 선택 → 해당 콘텐츠 초안·사람 검수·모델 품질 확인
                         ↓
운영 DB 변경·배포·복구·비밀·비용·관측 준비
                         ↓
실제 참가자 파일럿 → 오판정·추천 막힘·운영 지표 → 공개 출시 판단
```

새 Topic을 많이 나열하는 것만으로 출시 Gate 통과 불가. 다음 작업은 A1의 작은 콘텐츠 묶음 제작·검수 또는 기존 승인 범위의 대표 표본·운영 준비처럼 새로운 근거를 남기는 단위로 선택. 같은 전체 회귀 실행만 반복한 결과는 새 콘텐츠·시장·모델 품질 검증으로 계산하지 않음.

세부 진행과 미확정 운영 조건은 [tasks.md](../../planning/tasks.md)·[파일럿 실행안](../../changes/2026-09-21-phase-8/pilot-runbook.md)에서 관리. 이번 문서 작업의 검사·전달 증거는 [verification.md](verification.md) 참조.
