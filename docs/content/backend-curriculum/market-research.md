# 국내 Java·Spring 백엔드 학습 범위 시장조사

- 확인일: 2026-10-06, 한국 시간
- 독자·질문: 콘텐츠 기획자 / 취업 준비부터 국내 경력 백엔드까지 어떤 지식을 어떤 깊이로 다룰 것인가?
- 상태: 공개 자료 조사 완료, 수요·학습 효과에 대한 사용자 검증 미실행
- 범위 결정·제작 순서의 기준: [커리큘럼](README.md)
- 출처 확인 기록: [sources.json](sources.json). 본문 해시·확인일·용도·공고 요건 분류 포함

## 조사 방법과 해석 범위

1. 공식 채용 사이트에서 Java/JVM·Spring 서버 직무 탐색
2. 상세 본문·자격 요건·근무 위치 확인. 검색 결과만 남은 공고 제외
3. 요구 경력, 필수·기대 역량, 우대 사항, 업무, 사용 기술 분리
4. 교육 공급자의 직접 소개와 공식 CS 교육 체계 비교
5. 채용 신호를 학습 목표로 번역하고, 기술 설명은 표준·공식 문서로 대조

분석 표본: **5개 채용 사이트의 9개 직무 상세 페이지**. 토스·LINE·당근·카카오뱅크·NHN의 국내 근무 또는 국내 조직 대상. LINE Pay Thailand의 한국 오피스 포함. 토스 통합 모집 안내는 상세 공고와 중복되므로 직무 수에서 제외.

5개는 공식 채용 사이트 단위. 같은 사이트에도 소속 법인이 다른 직무가 있어 기업·기업군 수로 사용하지 않음. 법인명과 경력 조건은 직무별 기록.

표본 선택 기준: 국내 Java/JVM·Spring 백엔드와 경력 깊이를 비교할 수 있는 공개 본문. 플랫폼·금융·결제에 치우친 목적 표본. 전체 채용 시장의 수요 비율, 채용 인원, 기업 순위, 연봉, 취업 성공률 추정에 사용 불가. 신입 전용 공고를 충분히 확보하지 못했으므로 입문 범위는 CS 교육 체계와 교육 공급 자료로 보완.

확인일에 본문이 읽힌 사실과 채용 진행 상태 구분. 게시일 미표시 공고의 신규 여부 추정 금지. 마감일이 명시된 경우도 재지원·채용 규모 추정 금지. 카카오뱅크는 화면 상단 날짜와 본문 수시 마감 안내가 함께 있어 마감 시점 단정 제외.

출처별 책임:

| 자료 | 답할 질문 | 답하지 못하는 질문 |
|---|---|---|
| 공식 채용 상세 | 그 팀이 어떤 경험과 역량을 표현했나? | 전체 시장에서 몇 %가 요구하나? |
| 교육 서비스 소개 | 어떤 내용·방식의 학습을 공급하나? | 실제 학습 효과·매출·시장 점유율은? |
| ACM CS2023·대학 강의 | CS 기반에 어떤 영역이 필요한가? | 국내 기업의 실제 채용 빈도는? |
| RFC·언어 명세·공식 기술 문서 | 기술 설명의 조건과 적용 범위는? | 해당 기술이 모든 직무에 필수인가? |

수집 중 분당 요청 한도 발생. 실패한 요청은 간격을 두고 재수집. HTTP 200이어도 오류 본문인 카카오모빌리티·무신사 페이지는 제외. 확인 실패를 채용 부재로 해석하지 않음. 원문 전체 재배포 없이 직접 요약·링크·해시만 저장.

## 채용 상세에서 확인한 요구

표의 필수·기대 열은 자격 요건 또는 그에 해당하는 안내의 요약. ‘모든 기술을 완벽히 갖추지 않아도 지원 가능’ 등의 원문 조건 유지. 우대·업무·사용 스택을 필수 자격으로 승격하지 않음.

| ID·공식 공고 | 명시 경력 | 필수·기대 역량 | 우대·업무·스택에서 확인한 확장 신호 |
|---|---|---|---|
| J01 [토스뱅크 Server Developer Product](https://toss.im/career/job-detail?job_id=4076109003) | 최소 연차 미명시 | Java/Kotlin·Spring 서비스 경험, 요구사항을 데이터 모델·API로 설계, 협업 | 대용량 운영·Redis·Kafka 경험을 선호하는 표현. JPA·MySQL·K8s 등은 별도 사용 스택 |
| J02 [토스 Server Developer Product, 3년 이하](https://toss.im/career/job-detail?job_id=8006406003) | 실무 1년 이상·3년 이하 | Java/Kotlin 서버 개발, DB·네트워크·동시성을 코드에 적용, 원인 분석과 논리적 설명 | 운영 문제·쿼리·캐시·성능 개선 경험. 사용 스택 전부를 알아야 하는 조건은 아님 |
| J03 [LINE Pay Thailand 한국 오피스 Backend Software Engineer](https://careers.linecorp.com/ko/jobs/2430/) | 4년 이상·12년 미만 | Java 8+·Spring, RDB·SQL 설계, 자료구조·알고리즘·OS·네트워크, REST API, 단위·통합 테스트. 기술 요건 완전 충족 없이도 지원 가능 안내 | 결제·정산·보안, 대용량 운영, NoSQL 우대. 경력기술서에 문제·접근·결과 설명 요청 |
| J04 [LINE Pay Server Engineer](https://careers.linecorp.com/ko/jobs/3099/) | 3년 이상 또는 동등 깊이 | Java/Kotlin·Spring, HTTP/API·RDBMS·Transaction, 운영 정합성·성능 개선, AI 개발 활용, 코드·설계 논의 | 분산·외부 연동, 장애 예방·배포 안정성·멘토링 우대. 한국 오피스 판교 |
| J05 [당근 Backend 커뮤니티·아파트팀](https://careers.daangn.com/jobs/role/7762062003/) | 3년 이상 또는 동등 실력, 한 제품 1년 이상 책임 경험 | JVM·Spring, RDB·캐시, 모니터링 지표 개선, 문제 해결 | 스키마 변경·쿼리/인덱스 개선·장애 대응은 업무. AI 개발 워크플로우는 우대 |
| J06 [당근 Backend 나의당근](https://careers.daangn.com/jobs/role/7659289003/) | 3년 이상 또는 동등 역량 | 제품 맥락의 기술 선택, 문제 발견·해결, 제품 완성 경험 | 트래픽 고려·행동 지표 개선 우대. Kotlin·Spring Boot·Batch·DB·Kafka·K8s는 사용 스택 |
| J07 [카카오뱅크 서버 개발자·뱅킹](https://recruit.kakaobank.com/jobs/260505) | 서버 개발 5년 이상. JVM·Boot·ORM 실무 1년 이상은 별도 조건 | Java/Kotlin·Spring Boot·ORM, 문제 해결·팀 협업. 10년 이상은 설계부터 운영·장애 대응까지 추가 기대 | AI 개발 프로세스, Virtual Thread·Coroutine·Redis·Kafka 내부 원리, 모놀리스/MSA 전환 경험 우대 |
| J08 [NHN PAYCO Java 서버 개발](https://careers.nhn.com/recruits/4002243453274688309) | Java·Spring 3년 이상, RDB 3년 이상 | Java/Spring·RDB 경험, JavaScript 개발 경험 | Redis, UML·기능 명세·코드 리뷰·협업 우대 |
| J09 [NHN Cloud PaaS Backend](https://careers.nhn.com/recruits/4345782544040635308) | Java/Spring 10년 이상 | Java/Spring, K8s 포함 CI/CD 구성, GitOps | 아키텍처·기술 검증은 업무. LLM 업무 개선·하네스 엔지니어링은 우대 |

### 판단 1 — 프레임워크 아래의 기반 지식부터 확장

DB·네트워크·동시성을 실제 코드에 적용하는 능력은 저연차 공고에도 명시. 경력 공고에는 CS·SQL·HTTP·테스트가 직접 등장. Java·Spring·JPA만으로 해당 요구를 충분히 설명하기 어려움. DB·SQL·트랜잭션, HTTP/API, 자료구조·알고리즘·네트워크, 객체 설계·테스트를 우선 제작할 근거. [토스 J02](https://toss.im/career/job-detail?job_id=8006406003), [LINE J03](https://careers.linecorp.com/ko/jobs/2430/), [LINE J04](https://careers.linecorp.com/ko/jobs/3099/)

학습 범위 선택의 추론: 프레임워크 사용법 문제에 앞서 ‘왜 DB가 필요한가, 왜 재시도가 중복 실행을 만들 수 있는가, 왜 상태를 객체 안에서 보호하는가’를 설명할 수 있어야 실무 현상을 해석 가능. 이 우선순위는 조사자의 설계 제안이며 채용 빈도 순위가 아님.

### 판단 2 — 경력 학습은 판단·측정·실패 대응까지

운영 지표 개선과 한 제품의 지속 책임, 설계·배포·장애 대응, 기술적 의사결정이 공고에 등장. ‘정의를 맞혔다’만으로 이러한 역량을 검증하기 어려움. 제약·대안·측정 지표·실패 시 복구를 포함한 사례형 서술 문제가 필요하다는 추론. [당근 J05](https://careers.daangn.com/jobs/role/7762062003/), [당근 J06](https://careers.daangn.com/jobs/role/7659289003/), [카카오뱅크 J07](https://recruit.kakaobank.com/jobs/260505)

경력 연차와 학습 난이도는 별개. 3년차가 분산 시스템 경험이 많을 수 있고 10년차가 새 JVM 기능을 처음 배울 수 있음. BASIC/INTERMEDIATE/ADVANCED는 문제 복잡도, 채용 표의 연차는 출처에 기록된 조건.

### 판단 3 — Kafka·K8s는 목적·직무와 함께

Redis·Kafka·K8s는 표본에서 사용 기술·우대·필수로 서로 다르게 등장. NHN Cloud의 10년 경력 PaaS 직무는 K8s·CI/CD·GitOps를 자격 조건으로 표현. 당근 나의당근의 기술 목록은 사용 스택. 모든 입문자에게 같은 설치·암기 항목으로 요구하는 근거는 부족. [NHN J09](https://careers.nhn.com/recruits/4345782544040635308), [당근 J06](https://careers.daangn.com/jobs/role/7659289003/)

제작 방향: 캐시 일관성, 메시지 중복·순서·재처리, 배포 실패·스키마 호환성 문제를 먼저 제시하고 도구를 대안으로 비교. 기본 CRUD 설명 위에 필요한 경우 확장.

### 판단 4 — Kotlin·AI 개발 활용은 선택 확장에 포함

Java/Kotlin을 함께 표현한 직무, AI를 자격 요건에 둔 직무, AI 워크플로우를 우대로 둔 직무 확인. Kotlin 상호 운용과 AI 개발 결과 검증을 선택 트랙에 포함할 근거. 일부 공고의 표현으로 모든 국내 백엔드 직무에 필수라고 일반화 불가. [LINE J04](https://careers.linecorp.com/ko/jobs/3099/), [당근 J05](https://careers.daangn.com/jobs/role/7762062003/), [NHN J09](https://careers.nhn.com/recruits/4345782544040635308)

AI 트랙은 도구 사용법과 검증·정보 보호 중심. LLM 서비스 운영·RAG·모델 튜닝은 별도 전문 범위로 보류. 조사 대상이 Java·Spring 백엔드 기본 지식이라는 목적 유지.

## 교육 공급과 비교

동일 분야의 모든 사업자·상품을 포괄한 경쟁 분석은 아님. 직접 소개에서 읽힌 내용과 CrackCS에 가져올 시사점 분리. 가격·수강생 수·홍보 성과는 비교 기준에서 제외.

| 직접 소개 자료 | 확인한 구성·학습 방식 | CrackCS 콘텐츠에 대한 시사점 |
|---|---|---|
| [인프런 향로 CS 전공 로드맵](https://www.inflearn.com/roadmaps/1641) | 컴퓨터구조·OS·알고리즘·자료구조·네트워크·DB. 특정 언어·프레임워크와 구분 | CS를 Spring의 부속 목록으로 축소하지 않고 독립 기반으로 구성 |
| [roadmap.sh Backend](https://roadmap.sh/backend) | HTTP·DB·API·보안·테스트·캐시·메시징·CI/CD·컨테이너·관측 등. 프로젝트·진행 추적·AI 학습 기능도 제시 | 넓은 목록을 시작점으로 사용. 순서만 제시하는 것으로 이해 진단을 대신하기 어려움 |
| [프로그래머스 클라우드 기반 백엔드 엔지니어링](https://school.programmers.co.kr/learn/courses/26207) | 확인 본문은 14기. Java/CS/SQL → Spring/API/JPA/Test → 도메인·보안 프로젝트 → Kotlin/Docker/AWS/CI/CD. 팀 학습·성장 진단 소개 | 기반·프레임워크·프로젝트·배포를 연결. 코드 구현 능력은 CrackCS 서술 평가와 별도 검증 필요 |
| [패스트캠퍼스 Spring 테스트 과정](https://fastcampus.co.kr/dev_online_test) | 테스트 개념과 게시판·재고·결제·배치 사례, 단위·통합·성능·운영 문제와 CI 자동화 | 테스트를 JUnit 문법 한 Topic으로 축소하지 않고 수준 선택·격리·실패 재현까지 구성 |

공급 측 결론: CS 기반과 프로젝트·배포·테스트 연결은 이미 학습 시장에 존재. ‘AI 또는 개인화 학습이 없다’는 차별화 주장은 근거 부족. 위 소개만으로 실제 품질·완주율 비교도 불가.

검증할 제품 가설: 승인 문서에 근거한 서술형 진단 → 누락 Concept 확인 → 보강 질문 → 버전별 학습 이력 보존의 연결이 학습자의 설명 능력 점검을 도울 수 있음. 경쟁 우위 확정이 아닌 파일럿 가설. 대표 학습자의 오판정 신고, 반복 추천, 같은 개념의 후속 설명 개선으로 확인 필요.

## 기술 범위를 정할 때 지킬 구분

| 흔한 축약 | 학습·검수 기준 |
|---|---|
| DB = JPA | 모델·제약·SQL·인덱스·격리 수준 먼저. JPA는 매핑·영속성 경계. MyBatis는 SQL 매퍼로 비교. [PostgreSQL DDL](https://www.postgresql.org/docs/17/ddl.html), [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2), [MyBatis](https://mybatis.org/mybatis-3/) |
| HTTP API = REST | HTTP 메서드·상태·캐시 의미와 REST의 아키텍처 제약 분리. URI/CRUD만으로 REST 전체 충족 판정 금지. [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), [Fielding 5장](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm) |
| 객체 지향 = Spring DI | 상태·불변식·책임·다형성은 객체 설계. DI는 생성·의존성 조립 경계. SOLID를 이름 암기로 채점하지 않음. [Java 언어 명세](https://docs.oracle.com/javase/specs/jls/se21/html/index.html), [Spring IoC](https://docs.spring.io/spring-framework/reference/7.0/core/beans.html) |
| 테스트 통과 = 운영 문제 없음 | 테스트 수준·대체한 경계·실제 DB/HTTP 검증 범위 명시. 실무 능력은 코드·운영 증거로 별도 확인. [Spring Testing](https://docs.spring.io/spring-framework/reference/7.0/testing.html), [JUnit](https://docs.junit.org/current/user-guide/) |
| 트랜잭션 = 모든 실패 rollback | DB 내부 격리·원자성과 외부 HTTP 부수 효과 구분. proxy 모드의 같은 객체 내부 호출은 interceptor를 통과하지 않는 조건 설명. [PostgreSQL 격리](https://www.postgresql.org/docs/17/transaction-iso.html), [Spring Transaction](https://docs.spring.io/spring-framework/reference/7.0/data-access/transaction/declarative/annotations.html) |
| Kafka exactly-once = 외부 결제도 한 번 | 보장 대상·producer/consumer 설정·offset·외부 저장소 연동 조건 명시. [Kafka Design](https://kafka.apache.org/41/design/design/) |
| Redis lock = 정합성 완성 | TTL·중단·복제·장애·작업 재개 조건 점검. 고위험 쓰기의 최종 정합성 경계 별도 설계. [Redis 분산 잠금](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/) |

컴퓨터구조·수학 기반·분산·보안·소프트웨어 공학을 포함한 영역 구분은 [ACM/IEEE-CS/AAAI CS2023](https://csed.acm.org/knowledge-areas/)로 교차 확인. Backend 목적에 맞게 일부 영역 선택. 그래픽스·HCI·AI 전체 등의 CS 학위 교육을 모두 제공하는 계획은 아님.

## 다음 검증과 재조사 조건

| 미확인 질문 | 다음 행동 |
|---|---|
| 국내 다른 업종에서도 우선순위가 유지되는가? | SI·B2B SaaS·커머스·제조/기업 IT의 공식 상세 표본 추가. 업종·직무·경력을 분리해 비교 |
| 초보자에게 어느 순서가 덜 막히는가? | 취업 준비·주니어 파일럿에서 선수 지식 부족·설명 실패 위치 기록 |
| 경력자의 실제 문제 해결 능력과 연결되는가? | 경력 파일럿에서 제약·대안·측정·복구 답변을 독립 검수. 실제 경험 여부는 서술만으로 인증 불가 |
| 선택 트랙의 제작 비용 대비 사용성이 있는가? | Kotlin·AI 관심과 완주·오판정 신호를 확인한 뒤 본격 제작 여부 결정 |
| 공고·기술 기준이 바뀌었는가? | 콘텐츠 묶음 제작 시작 또는 반기 재조사 때 링크·요건·버전 재확인. 범위 변경 시 커리큘럼·sources.json 함께 갱신 |

조사 완료가 새 콘텐츠 검수·공개 완료를 뜻하지 않음. 출시 대상별 남은 작업은 [작업 목록](../../planning/tasks.md), 공개 기준은 [콘텐츠 정책](../../product/content-and-ai-policy.md#파일럿-공개-준비--oq-006) 참조.
