# CrackCS 작업 지침

## 범위와 우선순위

- 적용 범위: 저장소 전체
- 하위 `AGENTS.md`: 해당 디렉터리에서 함께 적용
- 우선순위: 현재 사용자 요구 > 가까운 `AGENTS.md` > 이 문서
- 보호 대상: 관련 없는 사용자 변경. 임의 복원·정리 금지

## 설명과 협업

- 설명 순서: 현상 → 필요한 이유 → 동작 원리 → 검증 결과
- 공개할 과정: 관찰, 가정, 설계 선택, 실행 명령, 테스트 증거
- 제외할 내용: private 내부 추론의 원문
- 설계 판단 질문:
  - 누가 상태를 소유하는가?
  - 결정에 필요한 정보를 누가 아는가?
  - 요구사항이 바뀌면 어떤 객체가 바뀌는가?
- 의미 있는 흐름·상태·의존성: 작은 텍스트 도식 사용
- 완료 보고: 변경, 테스트 수와 결과, 미검증 경계, 실패 조건

## 문서 관리

- 생성 기준: 독자·답할 질문·기존 문서와 다른 책임이 명확할 때만 별도 문서 추가
- 위치: 목적별 폴더 사용. 한 변경의 계획·검증·시안은 같은 폴더
- 유지 책임: 해당 요구·코드·결정을 변경한 작업자가 기준 문서와 링크 함께 갱신
- 커밋 제외: `docs/retrospectives/` 회고 문서. 로컬 보존, staging에 포함 금지
- 이력: 현재 기준과 구분. 대체 문서는 후속 문서 링크와 보존 이유 명시
- 문체: `~한다` 대신 짧은 항목형 표현. 한 항목에 한 가지 핵심
- 내용: 현재 상태, 결정 이유, 다음 행동 중심. 배경은 이해에 필요한 만큼
- 갱신 시점: 코드·요구사항·설계 변경과 같은 작업에서 관련 문서 수정
- 상태 구분: 구현 완료 / 계획 / 미확정. 완료 표시는 코드·테스트 근거 확인 후
- 중복 방지: 주제별 기준 문서 한 곳. 다른 문서는 링크로 참조
- 문서 목록: `docs/README.md`에서 역할·존재 여부·상태·위치·갱신 계기 관리. 루트 README는 주요 진입 링크만 유지
- 생성·이동·삭제: 같은 작업에서 문서 목록과 참조 링크 갱신. 작성 예정은 예정 경로 명시
- 오래된 내용: 현재 안내에서 제거·수정. 필요한 결정 이력은 ADR에 보존
- 미확정 항목: 확인할 질문과 다음 행동 명시
- 완료 확인: 변경 내용과 문서 일치, 링크·실행 명령 유효성 점검

## 개발 흐름: TDD

기능 추가, 버그 수정, 동작을 바꾸는 리팩터링의 기본 순서:

```text
RED      요구사항을 보여주는 최소 테스트 작성
  ↓      기대한 이유로 실패하는지 확인
GREEN    테스트를 통과하는 최소 구현
  ↓
REFACTOR 중복·이름·책임 개선
  ↓      관련 테스트를 다시 실행해 GREEN 유지
전체 테스트
```

- Production 코드보다 실패 테스트가 먼저
- 테스트 하나당 하나의 동작 또는 실패 원인
- RED 인정 조건: 누락된 동작 때문에 실패. 문법·fixture·환경 오류는 제외
- 처음부터 통과한 테스트: 기존 동작을 검증한 것. 요구사항 재검토
- GREEN 범위: 현재 실패를 해결하는 최소 코드. 예상 확장 선구현 금지
- REFACTOR 조건: GREEN 이후. 동작 변경 금지
- 버그 수정: 증상 재현 테스트부터 시작
- 완료 조건: 관련 테스트 → 가능한 범위의 전체 테스트

## 도메인과 엔티티

- 도메인 규칙의 위치: 상태를 소유한 도메인 객체
- 최종 방어: DTO·Service를 우회해도 불변식 유지
- 상태 변경: `update`, `publish`, `retire` 같은 의도 기반 메서드
- 변경 순서: 모든 입력 검증 → 필드 변경 → `updatedAt` 갱신
- 부분 수정 금지: 검증 실패 시 필드와 시각 모두 유지
- JPA 기본 형태: `@Getter`, public setter 없음
- 외부 생성: Lombok `@Builder` + 값을 받는 `private` 생성자
- JPA 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 빌더 노출: 외부 결정 값만. 초기 상태·유형·출처는 생성자가 결정
- Enum 저장: 특별한 이유가 없으면 `@Enumerated(EnumType.STRING)`
- API 응답: Entity 직접 반환 금지. 응답 DTO로 변환

### 날짜

```text
builder().build()
        ↓ 검증
createdAt = updatedAt = now

update(...)
        ↓ 전체 검증
상태 변경 + updatedAt = now
```

- 생성 시 `createdAt`과 `updatedAt`: 같은 시각
- 수정 성공 시: `createdAt` 유지, `updatedAt` 갱신
- JPA 생명주기 콜백(`@PrePersist`, `@PreUpdate`)에 시각 생성 위임 금지

## Service

- 구조: `{ServiceName}` 인터페이스 + `Default{ServiceName}` 구현
- 금지 이름: `{ServiceName}Impl`
- 인터페이스 책임: 유스케이스 계약
- 구현 책임: `@Service`, production `@Transactional`, Repository 의존성, 유스케이스 조정
- 호출자와 테스트의 의존 타입: Service 인터페이스
- 도메인이 판단할 수 있는 상태 규칙을 Service에만 두지 않음

## HTTP 경계

- 요청 DTO: Controller 하위 `request`
- 응답 DTO: Controller 하위 `response`
- 공용 DTO: 역할에 따라 공통 `request`, `response`
- 입력 필수값·범위·형식·메시지: request DTO가 소유
- Controller에 validation 규칙과 메시지 분산 금지
- 사용자 정의 예외: `com.example.crackcs.exception`
- 공통 예외 처리기: 애플리케이션 예외를 HTTP 오류 응답으로 변환

## 테스트 작성

- 기본 성질: 빠름, 독립적, 반복 가능, 자동 판정
- 본문 구조: 준비 → 실행 → 검증이 눈에 보이도록 작성
- 메서드명: 영어 Java 명명 관례
- `@DisplayName`: 모든 테스트 메서드에 자연스러운 한글 문장
- 테스트 클래스의 `@DisplayName`: 금지
- 정상 흐름과 의미 있는 경계·실패 흐름 검증
- 공통 fixture: 모든 테스트가 쓰는 최소 준비만 `@BeforeEach`
- 테스트별 성공·실패 조건: 해당 테스트 본문에서 준비
- 반복 생성: private fixture helper 허용. 호출은 본문에 노출
- `@BeforeEach`에 핵심 실행·검증·일부 테스트 전용 stubbing 금지
- Fake: 시계처럼 제어할 외부 조건만 대체. 실제 도메인 규칙 대체 금지
- 상태 있는 Fake: 테스트 사이 공유 금지
- `if`·`for`: 요구사항 반복 표현에는 허용. 검증 분기와 상태 추적에는 사용 자제
- 단순 DTO: 자체 단위 테스트 생략
- DTO의 validation·변환·기본값·정규화·계산: 해당 동작 테스트
- JSON 이름·직렬화·validation 메시지·HTTP 상태: Controller/API 테스트
- 로컬 seed SQL: 수동 화면 확인 전용. 자동화 테스트 의존·건수 검증 금지

## 테스트 격리와 teardown

- 정리 책임: 데이터를 만든 테스트 클래스
- Service 통합 테스트의 test-level `@Transactional`: 금지
- Service 테스트 정리: `@AfterEach`에서 DB 데이터를 FK 역순으로 삭제
- `@BeforeEach`에 이전 테스트 데이터 정리 금지
- bulk 삭제 순서 예시: 연결 엔티티 → 본 엔티티 → 참조 엔티티
- `deleteAllInBatch()`: cascade를 실행하지 않으므로 연결 테이블을 먼저 삭제
- teardown: assertion 실패와 부분 fixture 생성 후에도 실행 가능한 순서
- 공유 DB 병렬 실행: 전체 `deleteAll*()` 금지. 테스트별 데이터·schema 격리 또는 직렬화 필요
- DB 밖 공유 상태: static, singleton, `ThreadLocal`, 보안 컨텍스트, 시스템 속성, 파일을 원래 상태로 복원
- Spring singleton 재생성이 필요한 테스트: 필요한 클래스에만 `@DirtiesContext`
- 테스트마다 새로 만든 일반 객체: 불필요한 teardown 금지

```text
Service 호출(transaction commit)
            ↓
Repository 재조회로 저장 결과 검증
            ↓
@AfterEach: child → parent 삭제
```

## 테스트 수준 선택

```text
도메인 규칙        → Spring/JPA 없는 순수 단위 테스트
Service 유스케이스 → @SpringBootTest + 실제 Repository/DB + @AfterEach
JPA 매핑·Query    → JPA 통합 테스트
HTTP 계약         → MVC/API 테스트 + Service mock
핵심 사용자 흐름   → 소수의 전체 통합 테스트
```

- Service 테스트: Mockito `@Mock`, `@InjectMocks`, `@MockBean` 금지
- Service 저장 검증: Service transaction 종료 후 Repository로 재조회
- JPA 저장: 기본 `save()`; SQL 즉시 실행이 검증 대상일 때만 `flush()`/`saveAndFlush()`
- Repository query fixture: `save()` 후 조회. 관례적인 `EntityManager.clear()` 금지
- mapping 복원이 검증 대상일 때만 이유를 드러내고 `flush()` + `clear()`
- 객체 참조 차이(`isNotSameAs`) 자체는 검증하지 않음
- Controller 테스트: 실제 DB 금지. binding, validation, Service 계약, 상태 코드, 직렬화에 집중
- Controller 반환 fixture: mock 객체 사용. ID 주입을 위한 `ReflectionTestUtils` 금지
- JSON 요청: 기본적으로 요청 객체를 `ObjectMapper`로 직렬화
- 원문 JSON: 문법 오류처럼 객체로 표현할 수 없는 요청만 허용
- Controller부터 DB까지의 검증: 별도 API 통합 테스트
- test-level `@Transactional`이 필요한 JPA/API 테스트: rollback만으로 검증 목적이 가려지지 않는지 확인

## DB schema

- migration 도구: 현재 미사용
- 기본 profile: `ddl-auto=validate`
- local profile: `ddl-auto=update` + 화면 확인용 seed
- test profile: `ddl-auto=create-drop`, local DB/seed 미사용
- 운영 schema 변경 수단으로 `ddl-auto=update` 사용 금지
- 운영 DB 도입 전 결정: version 관리, 배포 순서, rollback 절차

## 완료 점검

- [ ] 요구 동작 반영
- [ ] RED 실패 원인 확인
- [ ] 최소 GREEN 구현
- [ ] REFACTOR 후 GREEN 유지
- [ ] 상태 소유 객체가 규칙도 소유
- [ ] 잘못된 객체와 부분 수정 차단
- [ ] 테스트 수준 적절
- [ ] 모든 테스트 메서드에 한글 `@DisplayName`, 클래스에는 없음
- [ ] 관련 테스트와 가능한 범위의 전체 테스트 통과
- [ ] 테스트 수·성공·실패·미검증 경계 보고
- [ ] 동작 이유와 실패 조건 설명 가능
