# a1-v1 검수용 읽기 자료

> 자동 생성 읽기 사본. 수정 기준은 [bundle.json](bundle.json). 사람 검수·공개 승인 대기.

- 원본 SHA-256: `4c08549e6563d13fd92dea9a1fded94eab675d283d28c1079ab18d0599e67beb`
- 재생성: 저장소 루트에서 `python3 scripts/render_content_review.py --bundle docs/content/a1-v1/bundle.json`
- 검수 절차·승인 기록: [콘텐츠 안내](README.md#사람-검수공개-순서)
- 이 자료를 읽었다는 사실만으로 앱의 검수·공개 상태가 바뀌지 않음

## 문항과 판정 기준

### A1-DB-01 · BASIC

팀 가입 테이블 membership(member_id, team_id, joined_at)이 있다. 한 회원은 여러 팀에 가입할 수 있고, 같은 회원·팀 조합은 한 번만 허용한다. 두 ID는 필수다. 기본키를 member_id 하나로 정하면 왜 요구를 어기는가? 대리키를 쓰는 설계도 포함해 유효한 제약 구성을 설명하라.

**모범 답안**

member_id 하나를 기본키로 두면 한 회원의 두 번째 팀 가입까지 막는다. (member_id, team_id)를 복합 기본키로 두면 각 조합을 식별하며 두 열의 NULL도 막는다. 별도 membership_id를 기본키로 선택한다면 업무 중복은 UNIQUE(member_id, team_id)로, 두 ID의 필수성은 각각 NOT NULL로 보장해야 한다. 대리키는 업무상 조합의 유일성을 대신하지 않는다.

**필수 개념**

- 행 식별자 선택 · 가중치 0.40: 한 회원의 여러 팀 가입을 허용할 행 식별자를 선택. member_id 단독 기본키의 과도한 제한 설명.
- 업무 조합의 유일성 · 가중치 0.30: 같은 회원·팀 조합의 중복 금지와 대리키의 유일성은 별도 조건. 복합 PK 또는 별도 UNIQUE 조합으로 보장.
- 키와 필수값의 결합 · 가중치 0.30: 복합 PK는 각 구성 열의 NOT NULL 포함. 대리키+UNIQUE 설계에서는 두 업무 ID에 NOT NULL을 별도 지정.

**개념별 판정 경계**

- `DB_01_01`
  - CORRECT: 한 회원의 여러 팀 가입을 허용할 행 식별자를 선택. member_id 단독 기본키의 과도한 제한 설명.
  - PARTIALLY_CORRECT: 유효한 식별자만 제시하고 단독 키의 문제를 생략.
  - INCORRECT: member_id 단독 키로 여러 팀 가입을 허용한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_01_02`
  - CORRECT: 같은 회원·팀 조합의 중복 금지와 대리키의 유일성은 별도 조건. 복합 PK 또는 별도 UNIQUE 조합으로 보장.
  - PARTIALLY_CORRECT: 중복 금지 방향만 설명하고 대리키와 업무 키 구분 누락.
  - INCORRECT: 대리키만 있으면 같은 회원·팀 조합의 중복도 자동 차단된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_01_03`
  - CORRECT: 복합 PK는 각 구성 열의 NOT NULL 포함. 대리키+UNIQUE 설계에서는 두 업무 ID에 NOT NULL을 별도 지정.
  - PARTIALLY_CORRECT: 필수성은 언급하나 대리키 설계의 NOT NULL 위치 누락.
  - INCORRECT: UNIQUE만으로 nullable 두 ID의 필수성도 보장한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-DB-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: member_id 하나를 기본키로 두면 한 회원의 두 번째 팀 가입까지 막는다. (member_id, team_id)를 복합 기본키로 두면 각 조합을 식별하며 두 열의 NULL도 막는다. 별도 membership_id를 기본키로 선택한다면 업무 중복은 UNIQUE(member_id, team_id)로, 두 ID의 필수성은 각각 NOT NULL로 보장해야 한다. 대리키는 업무상 조합의 유일성을 대신하지 않는다.
  - 예상 개념 판정: DB_01_01: CORRECT, DB_01_02: CORRECT, DB_01_03: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-DB-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 행의 신원을 가입 관계로 잡으면 복합 PK가 맞다. 신원을 별도 번호로 잡는 것도 가능하지만 회원·팀 UNIQUE와 두 NOT NULL을 추가해야 한다. 회원 번호 하나만 키로 쓰면 회원당 한 행밖에 못 넣는다.
  - 예상 개념 판정: DB_01_01: CORRECT, DB_01_02: CORRECT, DB_01_03: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-DB-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: member_id 하나면 회원의 두 번째 팀도 막으므로 조합 키를 쓰거나 대리키와 조합 UNIQUE를 함께 둔다. 두 ID는 필수로 관리해야 한다.
  - 예상 개념 판정: DB_01_01: CORRECT, DB_01_02: CORRECT, DB_01_03: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-DB-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: member_id를 단독 기본키로 두어도 여러 팀에 가입할 수 있다. 대리키가 있으면 업무 조합 중복도 막히고 UNIQUE가 NULL 입력도 금지한다.
  - 예상 개념 판정: DB_01_01: INCORRECT, DB_01_02: INCORRECT, DB_01_03: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-DB-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: member_id 하나를 기본키로 두면 한 회원의 두 번째 팀 가입까지 막는다. (member_id, team_id)를 복합 기본키로 두면 각 조합을 식별하며 두 열의 NULL도 막는다. 별도 membership_id를 기본키로 선택한다면 업무 중복은 UNIQUE(member_id, team_id)로, 두 ID의 필수성은 각각 NOT NULL로 보장해야 한다. 대리키는 업무상 조합의 유일성을 대신하지 않는다.
  - 예상 개념 판정: DB_01_01: NEEDS_REVIEW, DB_01_02: NEEDS_REVIEW, DB_01_03: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 대리키 도입을 업무 중복 방지와 동일시하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 5.5. Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — §5.5.2 NOT NULL, §5.5.3 UNIQUE, §5.5.4 Primary Keys
- [DB Design - Database Systems](https://cs186berkeley.net/notes/note13/) — Keys·Functional Dependencies: candidate key와 superkey

근거 문서: `A1-DB-DOC`. 검수 상태: **PENDING**

### A1-DB-02 · BASIC

PostgreSQL 17에서 email text UNIQUE, age integer CHECK(age >= 18)만 선언했다. email은 선택값이고 NULL인 회원 여러 명을 허용한다. age는 반드시 있어야 하며 18 이상이어야 한다. 현재 선언은 무엇을 보장하고 무엇이 부족한가? 다른 DB에도 그대로 적용되는 규칙인지 구분하라.

**모범 답안**

CHECK(age >= 18)는 age가 NULL이면 unknown이 되어 통과하므로 필수성을 보장하지 않는다. age NOT NULL과 CHECK를 함께 둔다. PostgreSQL 17의 기본 UNIQUE는 NULL들을 서로 다른 것으로 다뤄 여러 NULL email을 허용한다. NULLS NOT DISTINCT를 선택하면 동작이 바뀌며, NULL의 UNIQUE 처리는 DB 구현별 차이가 있어 일반화하면 안 된다.

**필수 개념**

- 값 존재와 범위 제약 · 가중치 0.50: PostgreSQL CHECK의 unknown 통과와 NOT NULL의 별도 책임 구분. 필수 나이에 NOT NULL+CHECK 조합 제시.
- UNIQUE의 NULL 전제 · 가중치 0.50: PostgreSQL 17 기본 UNIQUE는 여러 NULL 허용. NULLS NOT DISTINCT 및 DB별 차이를 조건으로 구분.

**개념별 판정 경계**

- `DB_02_01`
  - CORRECT: PostgreSQL CHECK의 unknown 통과와 NOT NULL의 별도 책임 구분. 필수 나이에 NOT NULL+CHECK 조합 제시.
  - PARTIALLY_CORRECT: 필수값 제약을 추가하되 CHECK가 NULL을 통과시키는 이유 생략.
  - INCORRECT: CHECK(age>=18)만으로 NULL까지 거부한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_02_02`
  - CORRECT: PostgreSQL 17 기본 UNIQUE는 여러 NULL 허용. NULLS NOT DISTINCT 및 DB별 차이를 조건으로 구분.
  - PARTIALLY_CORRECT: 기본 UNIQUE가 여러 NULL을 허용한다고 설명하나 변경 옵션·DB 차이 누락.
  - INCORRECT: 기본 UNIQUE가 두 번째 NULL을 막으며 모든 DB에서 같다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-DB-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: CHECK(age >= 18)는 age가 NULL이면 unknown이 되어 통과하므로 필수성을 보장하지 않는다. age NOT NULL과 CHECK를 함께 둔다. PostgreSQL 17의 기본 UNIQUE는 NULL들을 서로 다른 것으로 다뤄 여러 NULL email을 허용한다. NULLS NOT DISTINCT를 선택하면 동작이 바뀌며, NULL의 UNIQUE 처리는 DB 구현별 차이가 있어 일반화하면 안 된다.
  - 예상 개념 판정: DB_02_01: CORRECT, DB_02_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-DB-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 필수 나이는 존재 제약과 범위 제약 두 개가 필요하다. PG17에서 선택 email의 NULL 여러 개는 기본 UNIQUE와 양립하지만 NULLS NOT DISTINCT를 붙이거나 DB를 바꾸면 다시 확인해야 한다.
  - 예상 개념 판정: DB_02_01: CORRECT, DB_02_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-DB-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: NULL age는 CHECK에서 unknown이라 통과하니 NOT NULL을 더해야 한다. PostgreSQL의 기본 email UNIQUE에서는 NULL을 여러 번 넣을 수 있다.
  - 예상 개념 판정: DB_02_01: CORRECT, DB_02_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-DB-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: CHECK가 NULL age를 막아 추가 제약이 필요 없다. 기본 UNIQUE는 NULL도 한 번만 허용하고 모든 DB가 같은 방식이다.
  - 예상 개념 판정: DB_02_01: INCORRECT, DB_02_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-DB-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: CHECK(age >= 18)는 age가 NULL이면 unknown이 되어 통과하므로 필수성을 보장하지 않는다. age NOT NULL과 CHECK를 함께 둔다. PostgreSQL 17의 기본 UNIQUE는 NULL들을 서로 다른 것으로 다뤄 여러 NULL email을 허용한다. NULLS NOT DISTINCT를 선택하면 동작이 바뀌며, NULL의 UNIQUE 처리는 DB 구현별 차이가 있어 일반화하면 안 된다.
  - 예상 개념 판정: DB_02_01: NEEDS_REVIEW, DB_02_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** NULL의 SQL 비교 원리 상세는 SQL Topic이 소유. 여기서는 저장 제약의 책임과 DB별 동작만 평가.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 5.5. Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — §5.5.1 CHECK의 true/unknown 허용, §5.5.2 NOT NULL, §5.5.3 NULLS NOT DISTINCT

근거 문서: `A1-DB-DOC`. 검수 상태: **PENDING**

### A1-DB-03 · INTERMEDIATE

orders.customer_id가 customers.id를 참조하는 nullable FK다. 모든 주문에는 고객이 필요하고, 고객을 삭제해도 주문 이력은 자동 삭제되면 안 된다. 없는 고객 ID와 NULL을 각각 누가 막는가? PostgreSQL 17의 기본 비지연 FK를 전제로 삭제 정책을 설명하라.

**모범 답안**

FK는 존재하지 않는 non-NULL 고객 ID를 거부하지만 nullable 열의 NULL은 허용하므로 NOT NULL을 추가한다. 부모 삭제에 주문까지 CASCADE시키면 이력 보존 요구를 어긴다. 기본 비지연 NO ACTION 또는 RESTRICT로 참조 중인 고객 삭제를 거부하고 비활성화·보관 같은 수명 정책을 선택할 수 있다. NO ACTION은 지연 제약과 함께 쓰면 검사 시점이 달라질 수 있어 RESTRICT와 모든 조건에서 동일하다고 말하지 않는다.

**필수 개념**

- 참조의 존재와 필수성 · 가중치 0.50: FK의 유효한 부모 참조 보장과 NOT NULL의 필수 참조 보장을 분리. 없는 ID·NULL의 차이 설명.
- 참조 수명과 보존 정책 · 가중치 0.50: 주문 이력 보존에 CASCADE 삭제를 피하고 참조 중 부모 삭제 거부·비활성화 등 선택. 비지연 NO ACTION과 지연 가능성 조건 구분.

**개념별 판정 경계**

- `DB_03_01`
  - CORRECT: FK의 유효한 부모 참조 보장과 NOT NULL의 필수 참조 보장을 분리. 없는 ID·NULL의 차이 설명.
  - PARTIALLY_CORRECT: FK와 필수값 제약을 제시하지만 NULL 통과 이유 생략.
  - INCORRECT: nullable FK가 NULL도 거부하거나 없는 부모 ID를 허용한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_03_02`
  - CORRECT: 주문 이력 보존에 CASCADE 삭제를 피하고 참조 중 부모 삭제 거부·비활성화 등 선택. 비지연 NO ACTION과 지연 가능성 조건 구분.
  - PARTIALLY_CORRECT: 삭제 거부·보관 방향은 맞지만 NO ACTION/RESTRICT의 조건 차이 생략.
  - INCORRECT: CASCADE가 주문 이력을 유지하고 NO ACTION은 항상 부모만 삭제한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-DB-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: FK는 존재하지 않는 non-NULL 고객 ID를 거부하지만 nullable 열의 NULL은 허용하므로 NOT NULL을 추가한다. 부모 삭제에 주문까지 CASCADE시키면 이력 보존 요구를 어긴다. 기본 비지연 NO ACTION 또는 RESTRICT로 참조 중인 고객 삭제를 거부하고 비활성화·보관 같은 수명 정책을 선택할 수 있다. NO ACTION은 지연 제약과 함께 쓰면 검사 시점이 달라질 수 있어 RESTRICT와 모든 조건에서 동일하다고 말하지 않는다.
  - 예상 개념 판정: DB_03_01: CORRECT, DB_03_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-DB-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 주문은 실제 고객을 반드시 가리키게 FK와 NOT NULL을 조합한다. 연결된 고객의 물리 삭제를 막고 보관 상태로 전환하는 정책도 가능하다. 이 문제의 비지연 NO ACTION은 삭제를 막지만 지연 제약까지 고려하면 RESTRICT와 동일한 규칙은 아니다.
  - 예상 개념 판정: DB_03_01: CORRECT, DB_03_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-DB-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 없는 non-NULL ID는 FK, NULL은 NOT NULL로 막는다. 주문 이력을 남기려면 고객 삭제에 CASCADE 대신 삭제 거부와 비활성화를 쓴다.
  - 예상 개념 판정: DB_03_01: CORRECT, DB_03_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-DB-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: nullable FK만으로 NULL도 금지되고 없는 고객 ID는 허용한다. CASCADE로 고객을 지우면 주문은 보존되며 NO ACTION은 부모를 무조건 삭제한다.
  - 예상 개념 판정: DB_03_01: INCORRECT, DB_03_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-DB-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: FK는 존재하지 않는 non-NULL 고객 ID를 거부하지만 nullable 열의 NULL은 허용하므로 NOT NULL을 추가한다. 부모 삭제에 주문까지 CASCADE시키면 이력 보존 요구를 어긴다. 기본 비지연 NO ACTION 또는 RESTRICT로 참조 중인 고객 삭제를 거부하고 비활성화·보관 같은 수명 정책을 선택할 수 있다. NO ACTION은 지연 제약과 함께 쓰면 검사 시점이 달라질 수 있어 RESTRICT와 모든 조건에서 동일하다고 말하지 않는다.
  - 예상 개념 판정: DB_03_01: NEEDS_REVIEW, DB_03_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** FK 추가를 필수 참조 또는 자동 보존 정책 결정으로 오해하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 5.5. Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — §5.5.5 Foreign Keys: NULL, ON DELETE NO ACTION·RESTRICT·CASCADE

근거 문서: `A1-DB-DOC`. 검수 상태: **PENDING**

### A1-DB-04 · INTERMEDIATE

서로 다른 서버 두 대가 같은(resource_id, slot_at)의 예약 존재 여부를 각각 조회해 모두 없다고 확인한 뒤, 서로 다른 예약 ID로 INSERT한다. 각 ID와 슬롯은 non-NULL이다. 서버의 사전 조회만으로 중복을 막을 수 있는가? 공유 DB에서 최종 방어할 제약과 PostgreSQL 17 CHECK의 한계를 설명하라.

**모범 답안**

사전 조회 이후 다른 쓰기가 들어올 수 있어 두 서버의 확인이 모두 성공한다. DB UNIQUE(resource_id, slot_at)가 모든 삽입 경로의 업무 조합 중복을 막고, 애플리케이션은 충돌 결과를 처리해야 한다. 단일 서버 메모리 잠금은 다른 인스턴스·직접 SQL까지 보호하지 않는다. PostgreSQL CHECK에서 다른 행의 예약 수를 검사하는 방식은 지원되는 안정적 제약이 아니다. UNIQUE의 경쟁 검사는 미완료 쓰기를 기다렸다가 다시 판단할 수 있다.

**필수 개념**

- 공유 저장소의 업무 불변식 · 가중치 0.50: 조회와 삽입 사이 경쟁을 설명하고 DB의 조합 UNIQUE 및 충돌 결과 처리로 모든 쓰기 경로를 보호.
- 행 CHECK의 적용 범위 · 가중치 0.50: PostgreSQL CHECK의 행 조건과 다른 행·테이블 상태 제약을 구분. 교차 행 중복에 CHECK 대신 UNIQUE 사용.

**개념별 판정 경계**

- `DB_04_01`
  - CORRECT: 조회와 삽입 사이 경쟁을 설명하고 DB의 조합 UNIQUE 및 충돌 결과 처리로 모든 쓰기 경로를 보호.
  - PARTIALLY_CORRECT: UNIQUE를 제시하나 사전 조회의 경쟁·충돌 처리 중 일부 생략.
  - INCORRECT: 조회 후 삽입만으로 여러 서버의 중복이 완전히 차단된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_04_02`
  - CORRECT: PostgreSQL CHECK의 행 조건과 다른 행·테이블 상태 제약을 구분. 교차 행 중복에 CHECK 대신 UNIQUE 사용.
  - PARTIALLY_CORRECT: CHECK 대신 UNIQUE를 선택하지만 교차 행 CHECK의 한계 설명 누락.
  - INCORRECT: CHECK 안의 타 행 개수 조회가 UNIQUE와 동일한 안정적 중복 방어라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-DB-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 사전 조회 이후 다른 쓰기가 들어올 수 있어 두 서버의 확인이 모두 성공한다. DB UNIQUE(resource_id, slot_at)가 모든 삽입 경로의 업무 조합 중복을 막고, 애플리케이션은 충돌 결과를 처리해야 한다. 단일 서버 메모리 잠금은 다른 인스턴스·직접 SQL까지 보호하지 않는다. PostgreSQL CHECK에서 다른 행의 예약 수를 검사하는 방식은 지원되는 안정적 제약이 아니다. UNIQUE의 경쟁 검사는 미완료 쓰기를 기다렸다가 다시 판단할 수 있다.
  - 예상 개념 판정: DB_04_01: CORRECT, DB_04_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-DB-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 예약 ID가 달라도 슬롯 조합은 같을 수 있다. 모든 writer가 통과하는 DB에 그 조합의 유일성을 두고 경쟁에서 진 요청을 처리한다. 현재 행 범위를 넘어선 CHECK는 PG17에서 그 보장을 제공하는 수단이 아니다.
  - 예상 개념 판정: DB_04_01: CORRECT, DB_04_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-DB-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 두 서버가 조회를 통과할 수 있으니 DB의 슬롯 조합 UNIQUE와 위반 처리가 필요하다. 중복 조건은 CHECK보다 UNIQUE가 적절하다.
  - 예상 개념 판정: DB_04_01: CORRECT, DB_04_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-DB-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 각 서버가 먼저 없음을 확인하면 중복은 생기지 않는다. 다른 행을 세는 CHECK가 UNIQUE와 동일하게 경쟁까지 안전하게 막는다.
  - 예상 개념 판정: DB_04_01: INCORRECT, DB_04_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-DB-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 사전 조회 이후 다른 쓰기가 들어올 수 있어 두 서버의 확인이 모두 성공한다. DB UNIQUE(resource_id, slot_at)가 모든 삽입 경로의 업무 조합 중복을 막고, 애플리케이션은 충돌 결과를 처리해야 한다. 단일 서버 메모리 잠금은 다른 인스턴스·직접 SQL까지 보호하지 않는다. PostgreSQL CHECK에서 다른 행의 예약 수를 검사하는 방식은 지원되는 안정적 제약이 아니다. UNIQUE의 경쟁 검사는 미완료 쓰기를 기다렸다가 다시 판단할 수 있다.
  - 예상 개념 판정: DB_04_01: NEEDS_REVIEW, DB_04_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 잠금 구현 전체는 tx Topic. 이 문항은 최종 불변식 소유자와 제약의 표현 범위 평가.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 5.5. Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — §5.5.1 cross-row CHECK 경고, §5.5.3 UNIQUE
- [PostgreSQL: Documentation: 17: 62.5. Index Uniqueness Checks](https://www.postgresql.org/docs/17/index-unique-checks.html) — §62.5 동시 삽입의 wait·재검사

근거 문서: `A1-DB-DOC`. 검수 상태: **PENDING**

### A1-DB-05 · ADVANCED

주문 행의 키는(order_id, line_no)다. 같은 상품은 여러 주문에 나타난다. 업무 규칙상 product_id→현재 product_name, category_id이며 category_id→현재 category_name이다. unit_price는 주문 당시 가격이다. 모든 상품명은 현재명을 보여줘야 한다. 갱신 이상을 줄이면서 과거 가격을 보존하는 분리를 제안하고, 조회용 중복을 무조건 금지할 수 있는지 설명하라.

**모범 답안**

현재 상품명·분류는 상품 ID가 결정하고 분류명은 분류 ID가 결정한다. 이 현재 사실을 각 주문 행에 반복하면 이름 변경 시 일부 행만 바뀌는 이상이 생긴다. Product와 Category에 현재 사실을 모으고 OrderLine에는 키, product_id, 수량, 주문 당시 unit_price를 남기는 분리가 가능하다. unit_price는 역사적 거래 사실이라 현재 상품 가격으로 대체하면 안 된다. 조회용 중복도 가능하지만 일관성 유지·변경 책임과 비용을 명시해야 하며, 주어진 현재명 요구에서 예고 없이 과거명 스냅샷으로 바꾸면 의미가 달라진다.

**필수 개념**

- 함수 종속성과 갱신 이상 · 가중치 0.40: 주어진 업무 함수 종속성으로 현재 사실의 반복과 부분 갱신 위험을 설명. product_id를 주문 행 자체의 키로 오인하지 않음.
- 정규화 선택의 비용·책임 · 가중치 0.30: Product·Category 분리로 현재 사실의 소유자 지정. 조회용 중복은 동기화 책임·비용에 따라 허용 가능.
- 현재 사실과 거래 이력 · 가중치 0.30: 주문 당시 unit_price는 역사적 사실. 현재 상품 가격으로 치환하지 않고 OrderLine에 보존하며 현재명 요구와 구분.

**개념별 판정 경계**

- `DB_05_01`
  - CORRECT: 주어진 업무 함수 종속성으로 현재 사실의 반복과 부분 갱신 위험을 설명. product_id를 주문 행 자체의 키로 오인하지 않음.
  - PARTIALLY_CORRECT: 종속성과 중복을 언급하되 부분 갱신의 발생 원인 누락.
  - INCORRECT: product_id가 주문 행을 유일하게 식별하며 이름 반복은 갱신 이상과 무관하다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_05_02`
  - CORRECT: Product·Category 분리로 현재 사실의 소유자 지정. 조회용 중복은 동기화 책임·비용에 따라 허용 가능.
  - PARTIALLY_CORRECT: 분리 방향은 맞지만 중복 허용 조건과 유지 책임 누락.
  - INCORRECT: 모든 중복을 금지해야 하거나 현재명 요구를 무시하고 이름을 주문별로 독립 수정하자고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `DB_05_03`
  - CORRECT: 주문 당시 unit_price는 역사적 사실. 현재 상품 가격으로 치환하지 않고 OrderLine에 보존하며 현재명 요구와 구분.
  - PARTIALLY_CORRECT: 과거 가격을 남긴다고 설명하나 현재 가격과 다른 사실인 이유 누락.
  - INCORRECT: 주문 당시 가격을 삭제하고 현재 상품 가격으로 과거 금액을 계산해도 된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-DB-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 현재 상품명·분류는 상품 ID가 결정하고 분류명은 분류 ID가 결정한다. 이 현재 사실을 각 주문 행에 반복하면 이름 변경 시 일부 행만 바뀌는 이상이 생긴다. Product와 Category에 현재 사실을 모으고 OrderLine에는 키, product_id, 수량, 주문 당시 unit_price를 남기는 분리가 가능하다. unit_price는 역사적 거래 사실이라 현재 상품 가격으로 대체하면 안 된다. 조회용 중복도 가능하지만 일관성 유지·변경 책임과 비용을 명시해야 하며, 주어진 현재명 요구에서 예고 없이 과거명 스냅샷으로 바꾸면 의미가 달라진다.
  - 예상 개념 판정: DB_05_01: CORRECT, DB_05_02: CORRECT, DB_05_03: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-DB-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 상품과 분류의 현재 정보를 한 곳에서 바꾸고 주문 행은 상품을 참조하게 할 수 있다. 반복 저장이 조회에 필요하면 누가 언제 갱신하는지 정해야 한다. 거래 시점의 가격은 현재 가격과 다른 값의 소유자라 주문 행에 유지하고 현재명 조회 요구는 별도로 지킨다.
  - 예상 개념 판정: DB_05_01: CORRECT, DB_05_02: CORRECT, DB_05_03: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-DB-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 상품 ID가 현재 상품명·분류를 결정하고 분류 ID가 분류명을 결정하니 Product·Category로 모으면 부분 갱신을 줄인다. 조회 중복은 동기화 책임과 비용을 정해 허용할 수 있다. 주문 당시 가격은 주문 행에 남긴다.
  - 예상 개념 판정: DB_05_01: CORRECT, DB_05_02: CORRECT, DB_05_03: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-DB-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: product_id는 모든 주문 행의 키라 반복된 이름을 따로 수정해도 갱신 이상이 없다. 현재명을 주문별로 독립 관리하고 모든 중복을 금지하며 과거 단가는 지워 현재 가격으로 계산한다.
  - 예상 개념 판정: DB_05_01: INCORRECT, DB_05_02: INCORRECT, DB_05_03: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-DB-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 현재 상품명·분류는 상품 ID가 결정하고 분류명은 분류 ID가 결정한다. 이 현재 사실을 각 주문 행에 반복하면 이름 변경 시 일부 행만 바뀌는 이상이 생긴다. Product와 Category에 현재 사실을 모으고 OrderLine에는 키, product_id, 수량, 주문 당시 unit_price를 남기는 분리가 가능하다. unit_price는 역사적 거래 사실이라 현재 상품 가격으로 대체하면 안 된다. 조회용 중복도 가능하지만 일관성 유지·변경 책임과 비용을 명시해야 하며, 주어진 현재명 요구에서 예고 없이 과거명 스냅샷으로 바꾸면 의미가 달라진다.
  - 예상 개념 판정: DB_05_01: NEEDS_REVIEW, DB_05_02: NEEDS_REVIEW, DB_05_03: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 작은 표본의 일치만으로 함수 종속성을 추정하지 않음. 2NF를 단일 열 PK와 동일시하지 않음. 역사적 가격을 중복이라는 이유로 제거하지 않음.

**출처 대조 위치**

- [DB Design - Database Systems](https://cs186berkeley.net/notes/note13/) — Functional Dependencies·Keys·Decomposition·BCNF
- [Chapter 12 Normalization – Database Design – 2nd Edition](https://opentextbc.ca/dbdesign01/chapter/chapter-12-normalization/) — Chapter 12: 정규화 목적·갱신 이상 예시

근거 문서: `A1-DB-DOC`. 검수 상태: **PENDING**

### A1-SQL-01 · INTERMEDIATE

고객마다 결제 완료 주문 수를 구하되 0건 고객도 남겨야 한다. SELECT c.id, COUNT(*) FROM customers c LEFT JOIN orders o ON o.customer_id=c.id WHERE o.status='PAID' GROUP BY c.id의 문제와 수정 방향을 설명하라. orders.id는 NOT NULL PK다.

**모범 답안**

WHERE의 o.status 조건은 주문이 없어 NULL 확장된 고객 행을 탈락시킨다. 결제 조건을 ON의 매칭 조건으로 옮기면 매칭되는 주문이 없어도 고객을 남긴다. 그런 고객은 결과 행 하나가 생기므로 COUNT(*)는 1이다. COUNT(o.id)를 쓰면 non-NULL 주문 ID만 세어 0이 된다. 각 고객 그룹으로 묶는 조건을 유지한다.

**필수 개념**

- OUTER JOIN의 필터 위치 · 가중치 0.40: 결제 조건을 ON에 두어 매칭 기준으로 적용. 오른쪽 WHERE 필터는 NULL 확장 행까지 제거할 수 있음 설명.
- 미매칭 행의 NULL 확장 · 가중치 0.30: LEFT JOIN의 미매칭 고객은 오른쪽 NULL인 결과 행 하나를 남김.
- 집계 대상 값의 존재 · 가중치 0.30: COUNT(*)는 NULL 확장 행도 셈. non-NULL PK인 COUNT(o.id)로 실제 매칭 주문만 세어 0 표현.

**개념별 판정 경계**

- `SQL_01_01`
  - CORRECT: 결제 조건을 ON에 두어 매칭 기준으로 적용. 오른쪽 WHERE 필터는 NULL 확장 행까지 제거할 수 있음 설명.
  - PARTIALLY_CORRECT: 조건 위치를 바꾸되 WHERE가 제거하는 이유 누락.
  - INCORRECT: WHERE의 오른쪽 결제 조건을 그대로 두어도 0건 고객이 남는다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_01_02`
  - CORRECT: LEFT JOIN의 미매칭 고객은 오른쪽 NULL인 결과 행 하나를 남김.
  - PARTIALLY_CORRECT: 고객이 남는다고만 설명하고 NULL 확장 행 누락.
  - INCORRECT: 미매칭 고객의 결과 행이 아예 없다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_01_03`
  - CORRECT: COUNT(*)는 NULL 확장 행도 셈. non-NULL PK인 COUNT(o.id)로 실제 매칭 주문만 세어 0 표현.
  - PARTIALLY_CORRECT: COUNT(o.id)로 바꾸지만 COUNT(*)가 1인 이유 생략.
  - INCORRECT: COUNT(*)도 미매칭 그룹에서 0이며 COUNT(o.id)는 NULL도 센다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-SQL-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: WHERE의 o.status 조건은 주문이 없어 NULL 확장된 고객 행을 탈락시킨다. 결제 조건을 ON의 매칭 조건으로 옮기면 매칭되는 주문이 없어도 고객을 남긴다. 그런 고객은 결과 행 하나가 생기므로 COUNT(*)는 1이다. COUNT(o.id)를 쓰면 non-NULL 주문 ID만 세어 0이 된다. 각 고객 그룹으로 묶는 조건을 유지한다.
  - 예상 개념 판정: SQL_01_01: CORRECT, SQL_01_02: CORRECT, SQL_01_03: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-SQL-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 결제 주문과의 매칭 여부는 JOIN ON에서 결정한다. 고객을 남긴 뒤 생긴 NULL 주문 자리는 실제 주문이 아니므로 행 전체가 아닌 주문 PK를 세면 0건을 얻는다.
  - 예상 개념 판정: SQL_01_01: CORRECT, SQL_01_02: CORRECT, SQL_01_03: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-SQL-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 결제 조건을 ON으로 옮겨야 NULL 확장된 고객 행을 WHERE가 버리지 않는다. 주문이 없으면 오른쪽 값이 NULL인 행 하나가 남는다. 주문 수는 COUNT(o.id)로 구한다.
  - 예상 개념 판정: SQL_01_01: CORRECT, SQL_01_02: CORRECT, SQL_01_03: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-SQL-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: WHERE를 유지해도 0건 고객이 남고 미매칭 고객은 결과 행이 없다. COUNT(*)가 0이고 COUNT(o.id)는 NULL까지 센다.
  - 예상 개념 판정: SQL_01_01: INCORRECT, SQL_01_02: INCORRECT, SQL_01_03: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-SQL-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: WHERE의 o.status 조건은 주문이 없어 NULL 확장된 고객 행을 탈락시킨다. 결제 조건을 ON의 매칭 조건으로 옮기면 매칭되는 주문이 없어도 고객을 남긴다. 그런 고객은 결과 행 하나가 생기므로 COUNT(*)는 1이다. COUNT(o.id)를 쓰면 non-NULL 주문 ID만 세어 0이 된다. 각 고객 그룹으로 묶는 조건을 유지한다.
  - 예상 개념 판정: SQL_01_01: NEEDS_REVIEW, SQL_01_02: NEEDS_REVIEW, SQL_01_03: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** INNER JOIN으로 바꾸거나 결과 NULL을 COALESCE하는 것만으로 탈락한 고객을 되살릴 수 없음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 7.2. Table Expressions](https://www.postgresql.org/docs/17/queries-table-expressions.html) — §7.2.1.1 outer join·ON/WHERE 차이, §7.2.3 GROUP BY
- [PostgreSQL: Documentation: 17: 9.21. Aggregate Functions](https://www.postgresql.org/docs/17/functions-aggregate.html) — §9.21 count(*)·count(expression)

근거 문서: `A1-SQL-DOC`. 검수 상태: **PENDING**

### A1-SQL-02 · INTERMEDIATE

members.id는 non-NULL이고 값은 1,2다. blocked.member_id에는 1,NULL이 있다. WHERE id NOT IN (SELECT member_id FROM blocked)는 어느 회원을 남기는가? 차단되지 않은 회원을 얻는 수정과 NULL 전제를 설명하라.

**모범 답안**

1은 일치해 false이고 2는 NULL 비교 영향으로 unknown이므로 WHERE가 모두 제외해 아무 회원도 남지 않는다. NOT EXISTS로 blocked.member_id=members.id인 행이 없음을 검사하면 2가 남는다. 이 문항의 non-NULL 외부 ID 전제에서는 서브쿼리에서 NULL을 제외한 NOT IN도 가능하다. NULL을 0 같은 임의 값으로 치환하면 업무 값과 충돌할 수 있다.

**필수 개념**

- WHERE의 unknown 탈락 · 가중치 0.50: 주어진 NOT IN에서 1은 false, 2는 unknown이므로 WHERE 결과가 0행인 이유 설명.
- 반대 집합의 조건 표현 · 가중치 0.50: correlated NOT EXISTS 또는 외부 ID non-NULL 전제의 서브쿼리 NULL 제거로 2를 얻음. NULL 전제 구분.

**개념별 판정 경계**

- `SQL_02_01`
  - CORRECT: 주어진 NOT IN에서 1은 false, 2는 unknown이므로 WHERE 결과가 0행인 이유 설명.
  - PARTIALLY_CORRECT: 0행을 맞히나 unknown의 비교 원인 누락.
  - INCORRECT: NULL은 비교에서 무조건 무시되어 원래 NOT IN이 2를 남긴다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_02_02`
  - CORRECT: correlated NOT EXISTS 또는 외부 ID non-NULL 전제의 서브쿼리 NULL 제거로 2를 얻음. NULL 전제 구분.
  - PARTIALLY_CORRECT: NOT EXISTS로 2를 얻으나 NULL 제거 대안의 외부 ID 전제 설명 누락.
  - INCORRECT: IS NULL 행만 남기거나 임의 치환만으로 모든 NULL 조건에 보장된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-SQL-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 1은 일치해 false이고 2는 NULL 비교 영향으로 unknown이므로 WHERE가 모두 제외해 아무 회원도 남지 않는다. NOT EXISTS로 blocked.member_id=members.id인 행이 없음을 검사하면 2가 남는다. 이 문항의 non-NULL 외부 ID 전제에서는 서브쿼리에서 NULL을 제외한 NOT IN도 가능하다. NULL을 0 같은 임의 값으로 치환하면 업무 값과 충돌할 수 있다.
  - 예상 개념 판정: SQL_02_01: CORRECT, SQL_02_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-SQL-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 차단 목록에 없음을 NOT EXISTS의 ID 동등 조건으로 검사한다. 내부 NULL은 일치 행을 만들지 않아 2가 남는다. 바깥 ID가 NULL이 아니라는 이 문제에서는 내부 NULL을 빼고 NOT IN을 사용해도 된다. 수정 전에는 false와 unknown밖에 없어 결과가 없다.
  - 예상 개념 판정: SQL_02_01: CORRECT, SQL_02_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-SQL-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 1은 false, 2는 unknown이라 true만 남기는 WHERE에서 모두 제외된다. blocked와 ID가 일치하는 행이 없다는 NOT EXISTS를 사용하면 2다.
  - 예상 개념 판정: SQL_02_01: CORRECT, SQL_02_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-SQL-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: NULL을 무시하므로 원래 NOT IN이 2를 남긴다. 차단되지 않은 ID는 blocked의 NULL 행만 조회하거나 0으로 치환하면 모든 경우 안전하다.
  - 예상 개념 판정: SQL_02_01: INCORRECT, SQL_02_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-SQL-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 1은 일치해 false이고 2는 NULL 비교 영향으로 unknown이므로 WHERE가 모두 제외해 아무 회원도 남지 않는다. NOT EXISTS로 blocked.member_id=members.id인 행이 없음을 검사하면 2가 남는다. 이 문항의 non-NULL 외부 ID 전제에서는 서브쿼리에서 NULL을 제외한 NOT IN도 가능하다. NULL을 0 같은 임의 값으로 치환하면 업무 값과 충돌할 수 있다.
  - 예상 개념 판정: SQL_02_01: NEEDS_REVIEW, SQL_02_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** NULL을 빈 문자열·0과 같은 값으로 정의하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 9.24. Subquery Expressions](https://www.postgresql.org/docs/17/functions-subquery.html) — §9.24.1 EXISTS, §9.24.3 NOT IN의 NULL 결과
- [PostgreSQL: Documentation: 17: 7.2. Table Expressions](https://www.postgresql.org/docs/17/queries-table-expressions.html) — §7.2.2 WHERE는 true인 행만 유지

근거 문서: `A1-SQL-DOC`. 검수 상태: **PENDING**

### A1-SQL-03 · BASIC

데이터가 변경되지 않는 orders에서 ORDER BY created_at DESC LIMIT 10으로 최근 주문을 읽는다. 여러 주문의 created_at이 같다. 매번 같은 상위 10개와 순서를 계약하려면 무엇이 필요한가? PK가 있다는 이유로 자동 정렬되는지, 동시 변경 중 페이지 안정성까지 보장되는지 설명하라.

**모범 답안**

created_at 동률 사이 순서는 이 정렬만으로 정해지지 않는다. ORDER BY created_at DESC, id DESC처럼 고유한 보조 키까지 명시하여 전체 순서를 정한다. PK의 존재만으로 SELECT의 결과 순서가 보장되지 않는다. 이 답은 고정 데이터의 결정적 상위 집합에 관한 것이며, 조회 사이 삽입·삭제가 생기는 페이지 안정성은 snapshot·cursor 계약 등 추가 조건이 필요하다.

**필수 개념**

- 명시적인 결과 순서 · 가중치 0.50: 정렬 계약은 ORDER BY가 소유하며 PK 존재만으로 결과 순서 보장되지 않음.
- 동률 해소와 범위 전제 · 가중치 0.50: 고유 보조 키까지 명시해 동률을 해소. 고정 데이터의 결정성과 동시 변경 페이지 안정성을 구분.

**개념별 판정 경계**

- `SQL_03_01`
  - CORRECT: 정렬 계약은 ORDER BY가 소유하며 PK 존재만으로 결과 순서 보장되지 않음.
  - PARTIALLY_CORRECT: ORDER BY 필요성만 언급하고 PK 오해 구분 누락.
  - INCORRECT: PK가 있으면 ORDER BY 없이 자동으로 같은 순서라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_03_02`
  - CORRECT: 고유 보조 키까지 명시해 동률을 해소. 고정 데이터의 결정성과 동시 변경 페이지 안정성을 구분.
  - PARTIALLY_CORRECT: 보조 PK 정렬은 제시하나 데이터 변경 시 보장 한계 누락.
  - INCORRECT: 동률을 해소할 필요 없고 LIMIT만으로 동시 변경까지 같은 페이지를 보장한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-SQL-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: created_at 동률 사이 순서는 이 정렬만으로 정해지지 않는다. ORDER BY created_at DESC, id DESC처럼 고유한 보조 키까지 명시하여 전체 순서를 정한다. PK의 존재만으로 SELECT의 결과 순서가 보장되지 않는다. 이 답은 고정 데이터의 결정적 상위 집합에 관한 것이며, 조회 사이 삽입·삭제가 생기는 페이지 안정성은 snapshot·cursor 계약 등 추가 조건이 필요하다.
  - 예상 개념 판정: SQL_03_01: CORRECT, SQL_03_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-SQL-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 시간만으로 순서를 다 정하지 못하니 유일한 ID를 다음 정렬 기준으로 넣는다. 물리 저장 순서나 PK 인덱스는 계약이 아니다. 같은 데이터에서는 상위 10개를 결정할 수 있지만, 변경 중 페이지에는 별도 스냅샷·커서 조건이 필요하다.
  - 예상 개념 판정: SQL_03_01: CORRECT, SQL_03_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-SQL-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: PK만으로 정렬되지 않으니 ORDER BY가 필요하다. created_at과 고유 id를 함께 내림차순으로 정렬하면 동률도 해소된다.
  - 예상 개념 판정: SQL_03_01: CORRECT, SQL_03_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-SQL-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: PK가 있으면 자동 순서가 보장된다. created_at 동률은 신경 쓸 필요 없고 LIMIT 10이면 데이터가 바뀌어도 같은 페이지가 나온다.
  - 예상 개념 판정: SQL_03_01: INCORRECT, SQL_03_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-SQL-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: created_at 동률 사이 순서는 이 정렬만으로 정해지지 않는다. ORDER BY created_at DESC, id DESC처럼 고유한 보조 키까지 명시하여 전체 순서를 정한다. PK의 존재만으로 SELECT의 결과 순서가 보장되지 않는다. 이 답은 고정 데이터의 결정적 상위 집합에 관한 것이며, 조회 사이 삽입·삭제가 생기는 페이지 안정성은 snapshot·cursor 계약 등 추가 조건이 필요하다.
  - 예상 개념 판정: SQL_03_01: NEEDS_REVIEW, SQL_03_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 고유 ID 정렬을 동시 변경에 대한 스냅샷 보장과 혼동하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 7.5. Sorting Rows (ORDER BY)](https://www.postgresql.org/docs/17/queries-order.html) — §7.5 정렬 지정과 보조 정렬 식
- [PostgreSQL: Documentation: 17: 7.6. LIMIT and OFFSET](https://www.postgresql.org/docs/17/queries-limit.html) — §7.6 LIMIT에서 unique order 필요

근거 문서: `A1-SQL-DOC`. 검수 상태: **PENDING**

### A1-SQL-04 · ADVANCED

PostgreSQL 17 B-tree 인덱스(tenant_id, created_at)가 있다. tenant_id=? AND created_at>=?와 created_at>=?만 있는 쿼리를 비교하라. 선두 열이 없으면 인덱스를 절대 못 쓴다는 주장과 인덱스가 있으면 항상 빠르다는 주장을 검토하고, 측정할 조건을 제시하라.

**모범 답안**

선행 tenant_id 동등 조건과 created_at 범위 조건은 인덱스에서 읽을 구간을 좁히는 데 유리하다. PG17 B-tree는 일부 열 조건에도 사용할 수 있어 선두 열 부재를 절대 사용 불가로 단정하면 틀리지만 더 넓은 인덱스 스캔이 필요할 수 있다. 사용 가능성과 선택·속도는 다르며 행 수, 분포, 선택도, 통계, 정렬·힙 접근, 추가 쓰기 비용을 비교한다. 대표 데이터의 실행 계획과 실제 시간·버퍼를 확인하고 EXPLAIN ANALYZE는 SQL을 실행한다는 조건에서 안전한 환경과 쿼리 종류를 선택한다.

**필수 개념**

- 복합 B-tree의 스캔 구간 · 가중치 0.40: PG17에서 선행 동등·다음 범위 조건의 구간 축소와 일부 열만으로도 사용 가능한 조건 구분.
- 실행 계획의 비용 판단 · 가중치 0.30: 인덱스 존재·사용 가능·실제 선택·속도를 구분. 분포·선택도·힙 접근과 쓰기 비용 비교.
- 실측의 실행 조건 · 가중치 0.30: 대표 데이터에서 계획·실측을 비교. EXPLAIN ANALYZE는 실제 실행하므로 변경 SQL의 효과와 환경 확인.

**개념별 판정 경계**

- `SQL_04_01`
  - CORRECT: PG17에서 선행 동등·다음 범위 조건의 구간 축소와 일부 열만으로도 사용 가능한 조건 구분.
  - PARTIALLY_CORRECT: 선두 열의 이점만 설명하고 사용 가능 범위 생략.
  - INCORRECT: 선두 열 없이 인덱스 사용이 절대 불가 또는 모든 열 순서가 같다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_04_02`
  - CORRECT: 인덱스 존재·사용 가능·실제 선택·속도를 구분. 분포·선택도·힙 접근과 쓰기 비용 비교.
  - PARTIALLY_CORRECT: 무조건 빠르지 않다고만 설명하고 비용 조건 생략.
  - INCORRECT: 인덱스만 있으면 항상 더 빠르고 쓰기 비용도 없다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_04_03`
  - CORRECT: 대표 데이터에서 계획·실측을 비교. EXPLAIN ANALYZE는 실제 실행하므로 변경 SQL의 효과와 환경 확인.
  - PARTIALLY_CORRECT: 계획과 시간 측정을 제시하나 ANALYZE 실행 효과 누락.
  - INCORRECT: EXPLAIN ANALYZE는 실행하지 않으므로 운영 변경 SQL에도 부수 효과 없이 사용 가능하다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-SQL-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 선행 tenant_id 동등 조건과 created_at 범위 조건은 인덱스에서 읽을 구간을 좁히는 데 유리하다. PG17 B-tree는 일부 열 조건에도 사용할 수 있어 선두 열 부재를 절대 사용 불가로 단정하면 틀리지만 더 넓은 인덱스 스캔이 필요할 수 있다. 사용 가능성과 선택·속도는 다르며 행 수, 분포, 선택도, 통계, 정렬·힙 접근, 추가 쓰기 비용을 비교한다. 대표 데이터의 실행 계획과 실제 시간·버퍼를 확인하고 EXPLAIN ANALYZE는 SQL을 실행한다는 조건에서 안전한 환경과 쿼리 종류를 선택한다.
  - 예상 개념 판정: SQL_04_01: CORRECT, SQL_04_02: CORRECT, SQL_04_03: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-SQL-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: tenant로 먼저 범위를 자른 뒤 시간을 좁히는 쿼리가 유리할 수 있다. 시간만 조건이어도 PG17은 인덱스 사용을 허용하지만 넓게 훑어 순차 스캔보다 비쌀 수 있다. 실제 분포와 쓰기 부하를 포함해 계획·버퍼·시간을 비교하고 ANALYZE가 명령을 실행하므로 검증 환경을 정한다.
  - 예상 개념 판정: SQL_04_01: CORRECT, SQL_04_02: CORRECT, SQL_04_03: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-SQL-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 선행 tenant 조건이 있으면 스캔 구간이 줄지만 그 열이 없어도 PG17 인덱스를 사용할 수는 있다. 선택도·분포·힙 접근과 쓰기 비용에 따라 실제 속도가 달라지므로 대표 데이터의 EXPLAIN ANALYZE와 시간을 비교한다.
  - 예상 개념 판정: SQL_04_01: CORRECT, SQL_04_02: CORRECT, SQL_04_03: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-SQL-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 선두 열이 없으면 절대 인덱스를 못 쓴다. 인덱스만 있으면 항상 더 빠르고 쓰기 비용은 없다. EXPLAIN ANALYZE는 SQL을 실행하지 않아 운영 UPDATE에도 영향이 없다.
  - 예상 개념 판정: SQL_04_01: INCORRECT, SQL_04_02: INCORRECT, SQL_04_03: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-SQL-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 선행 tenant_id 동등 조건과 created_at 범위 조건은 인덱스에서 읽을 구간을 좁히는 데 유리하다. PG17 B-tree는 일부 열 조건에도 사용할 수 있어 선두 열 부재를 절대 사용 불가로 단정하면 틀리지만 더 넓은 인덱스 스캔이 필요할 수 있다. 사용 가능성과 선택·속도는 다르며 행 수, 분포, 선택도, 통계, 정렬·힙 접근, 추가 쓰기 비용을 비교한다. 대표 데이터의 실행 계획과 실제 시간·버퍼를 확인하고 EXPLAIN ANALYZE는 SQL을 실행한다는 조건에서 안전한 환경과 쿼리 종류를 선택한다.
  - 예상 개념 판정: SQL_04_01: NEEDS_REVIEW, SQL_04_02: NEEDS_REVIEW, SQL_04_03: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 다른 DB나 PostgreSQL 후속 버전의 skip scan을 PG17의 고정 규칙으로 가져오지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 11.3. Multicolumn Indexes](https://www.postgresql.org/docs/17/indexes-multicolumn.html) — §11.3 PostgreSQL 17 B-tree: any subset·leading columns
- [PostgreSQL: Documentation: 17: 14.1. Using EXPLAIN](https://www.postgresql.org/docs/17/using-explain.html) — §14.1 비용·실측, ANALYZE 실행 경고

근거 문서: `A1-SQL-DOC`. 검수 상태: **PENDING**

### A1-SQL-05 · ADVANCED

고객 한 명에 orders가 2행이며 각 amount=20이다. comments는 3행이다. 두 자식을 고객 ID로 동시에 JOIN한 뒤 SUM(o.amount)를 하면 120이 된다. 실제 주문 합계 40을 얻으려면 어떻게 해야 하는가? SUM(DISTINCT o.amount)가 답인지 설명하라.

**모범 답안**

주문 2행과 댓글 3행의 조합 6행이 생겨 주문 금액이 각각 3번 합산된다. orders는 고객별 합계 40으로, 댓글도 필요한 집계 단위에 맞춰 고객당 한 행으로 먼저 줄인 뒤 고객에 결합하면 팬아웃을 피한다. SUM(DISTINCT amount)는 같은 금액 20인 서로 다른 주문 둘을 하나로 합쳐 20을 내므로 틀리다. 거래 행의 정체성과 금액 값의 동일성은 다르다.

**필수 개념**

- JOIN 팬아웃과 집계 중복 · 가중치 0.50: 부모에 매달린 두 자식의 조합 2×3=6과 주문 금액 반복으로 120이 나오는 과정 설명.
- 집계 단위의 보존 · 가중치 0.50: 각 자식을 부모당 먼저 집계 후 결합하여 40 보존. SUM(DISTINCT amount)는 서로 다른 같은 금액 주문을 지워 20임 설명.

**개념별 판정 경계**

- `SQL_05_01`
  - CORRECT: 부모에 매달린 두 자식의 조합 2×3=6과 주문 금액 반복으로 120이 나오는 과정 설명.
  - PARTIALLY_CORRECT: 6행 또는 120만 제시하고 반복 원인 생략.
  - INCORRECT: 주문이 6건으로 늘었거나 JOIN으로 합계가 자동 40이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `SQL_05_02`
  - CORRECT: 각 자식을 부모당 먼저 집계 후 결합하여 40 보존. SUM(DISTINCT amount)는 서로 다른 같은 금액 주문을 지워 20임 설명.
  - PARTIALLY_CORRECT: 부모별 선집계는 제시하나 DISTINCT가 잘못된 이유 누락.
  - INCORRECT: SUM(DISTINCT amount)로 40이 복원되며 값 동일성과 거래 동일성이 같다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-SQL-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 주문 2행과 댓글 3행의 조합 6행이 생겨 주문 금액이 각각 3번 합산된다. orders는 고객별 합계 40으로, 댓글도 필요한 집계 단위에 맞춰 고객당 한 행으로 먼저 줄인 뒤 고객에 결합하면 팬아웃을 피한다. SUM(DISTINCT amount)는 같은 금액 20인 서로 다른 주문 둘을 하나로 합쳐 20을 내므로 틀리다. 거래 행의 정체성과 금액 값의 동일성은 다르다.
  - 예상 개념 판정: SQL_05_01: CORRECT, SQL_05_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-SQL-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 금액이 아니라 행의 곱셈이 문제다. 고객당 각 자식을 한 행으로 만든 후 결합하면 두 주문의 합계 40을 지킨다. 값에 DISTINCT를 적용하면 두 주문의 동일한 20까지 하나로 줄어 20이 된다.
  - 예상 개념 판정: SQL_05_01: CORRECT, SQL_05_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-SQL-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 주문 2개와 댓글 3개 조합으로 각 주문이 3번 더해져 120이다. orders와 comments를 고객당 먼저 집계한 뒤 합치면 주문 합계 40을 유지한다.
  - 예상 개념 판정: SQL_05_01: CORRECT, SQL_05_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-SQL-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: JOIN하면 주문이 실제로 6건이 된다. SUM(DISTINCT amount)는 거래를 구분해 40을 복원하며 같은 금액은 같은 거래다.
  - 예상 개념 판정: SQL_05_01: INCORRECT, SQL_05_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-SQL-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 주문 2행과 댓글 3행의 조합 6행이 생겨 주문 금액이 각각 3번 합산된다. orders는 고객별 합계 40으로, 댓글도 필요한 집계 단위에 맞춰 고객당 한 행으로 먼저 줄인 뒤 고객에 결합하면 팬아웃을 피한다. SUM(DISTINCT amount)는 같은 금액 20인 서로 다른 주문 둘을 하나로 합쳐 20을 내므로 틀리다. 거래 행의 정체성과 금액 값의 동일성은 다르다.
  - 예상 개념 판정: SQL_05_01: NEEDS_REVIEW, SQL_05_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 중복 행의 원인을 값 DISTINCT로 숨기지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 7.2. Table Expressions](https://www.postgresql.org/docs/17/queries-table-expressions.html) — §7.2.1 joined tables·§7.2.3 GROUP BY
- [PostgreSQL: Documentation: 17: 9.21. Aggregate Functions](https://www.postgresql.org/docs/17/functions-aggregate.html) — §9.21 SUM·COUNT 집계

근거 문서: `A1-SQL-DOC`. 검수 상태: **PENDING**

### A1-TX-01 · INTERMEDIATE

로컬 DB transaction에서 주문을 저장하고 외부 결제 HTTP 호출이 성공했다. 그 뒤 DB 제약 위반으로 rollback됐다. 외부 결제는 이 DB transaction에 참여하지 않으며 XA도 없다. 결제도 자동 취소되는가? 남은 상태를 어떻게 다뤄야 하는지 설명하라.

**모범 답안**

rollback은 참여한 DB 변경을 되돌리고, 이미 성공한 외부 결제는 자동 취소하지 않는다. 주문 저장 실패와 결제 성공이 함께 남을 수 있으므로 요청·결제 식별자로 상태를 확인하고 복구해야 한다. 제공자 계약에 따라 멱등 재처리·보상 취소·대사를 설계할 수 있으나 취소 성공까지 자동 보장되지는 않는다. 호출 결과가 불확실한 경우도 별도로 추적하며, transaction 어노테이션만으로 HTTP를 DB 원자성에 포함했다고 판단하지 않는다.

**필수 개념**

- 트랜잭션 참여 자원 범위 · 가중치 0.50: 로컬 DB rollback의 범위와 미참여 외부 HTTP 성공 효과를 분리. 주문 저장 실패·결제 성공 공존 설명.
- 범위 밖 효과의 복구 조건 · 가중치 0.50: 식별자·상태 확인 후 제공자 계약에 맞는 재처리·보상·대사 선택. 복구 성공·중복 방어는 별도 보장임 설명.

**개념별 판정 경계**

- `TX_01_01`
  - CORRECT: 로컬 DB rollback의 범위와 미참여 외부 HTTP 성공 효과를 분리. 주문 저장 실패·결제 성공 공존 설명.
  - PARTIALLY_CORRECT: 외부 효과가 남는다는 결론만 설명하고 참여 자원 이유 생략.
  - INCORRECT: 로컬 DB rollback이 미참여 HTTP 결제까지 자동 취소한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TX_01_02`
  - CORRECT: 식별자·상태 확인 후 제공자 계약에 맞는 재처리·보상·대사 선택. 복구 성공·중복 방어는 별도 보장임 설명.
  - PARTIALLY_CORRECT: 보상·대사 방향은 제시하나 제공자 계약과 실패 가능성 생략.
  - INCORRECT: 남은 결제를 확인할 필요 없고 어떤 보상도 항상 자동 성공한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TX-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: rollback은 참여한 DB 변경을 되돌리고, 이미 성공한 외부 결제는 자동 취소하지 않는다. 주문 저장 실패와 결제 성공이 함께 남을 수 있으므로 요청·결제 식별자로 상태를 확인하고 복구해야 한다. 제공자 계약에 따라 멱등 재처리·보상 취소·대사를 설계할 수 있으나 취소 성공까지 자동 보장되지는 않는다. 호출 결과가 불확실한 경우도 별도로 추적하며, transaction 어노테이션만으로 HTTP를 DB 원자성에 포함했다고 판단하지 않는다.
  - 예상 개념 판정: TX_01_01: CORRECT, TX_01_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TX-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 결제 제공자는 이 transaction의 참가자가 아니다. 로컬 저장이 실패해도 제공자의 성공 기록은 남을 수 있다. 식별자를 보존해 확인·대사하고 제공자가 지원하는 취소나 멱등 재시도를 선택하되 그 복구 자체의 실패도 처리해야 한다.
  - 예상 개념 판정: TX_01_01: CORRECT, TX_01_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TX-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: DB rollback은 DB 참여 자원만 되돌려 외부 결제가 남을 수 있다. 주문 실패와 결제 성공은 별개 상태다. 결제 식별자로 확인하고 보상·대사로 복구한다.
  - 예상 개념 판정: TX_01_01: CORRECT, TX_01_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TX-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: DB가 rollback하면 미참여 외부 HTTP 결제도 자동 취소된다. 결제 상태를 볼 필요 없고 보상은 계약과 무관하게 항상 자동 성공한다.
  - 예상 개념 판정: TX_01_01: INCORRECT, TX_01_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TX-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: rollback은 참여한 DB 변경을 되돌리고, 이미 성공한 외부 결제는 자동 취소하지 않는다. 주문 저장 실패와 결제 성공이 함께 남을 수 있으므로 요청·결제 식별자로 상태를 확인하고 복구해야 한다. 제공자 계약에 따라 멱등 재처리·보상 취소·대사를 설계할 수 있으나 취소 성공까지 자동 보장되지는 않는다. 호출 결과가 불확실한 경우도 별도로 추적하며, transaction 어노테이션만으로 HTTP를 DB 원자성에 포함했다고 판단하지 않는다.
  - 예상 개념 판정: TX_01_01: NEEDS_REVIEW, TX_01_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 분산 복구 패턴 전체는 심화 Topic에서 확장. 여기서는 로컬 transaction의 참여 범위와 복구 조건만 평가.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 3.4. Transactions](https://www.postgresql.org/docs/17/tutorial-transactions.html) — §3.4 transaction 블록·ROLLBACK의 DB 변경 범위

근거 문서: `A1-TX-DOC`. 검수 상태: **PENDING**

### A1-TX-02 · INTERMEDIATE

PostgreSQL 17에서 A가 transaction을 시작해 일반 SELECT로 가격10을 읽었다. B가 20으로 UPDATE하고 commit했다. A가 같은 transaction에서 다시 일반 SELECT한다. A가 Read Committed일 때와 Repeatable Read일 때의 결과를 비교하라. A 자신의 변경은 없고 다른 쓰기도 없다.

**모범 답안**

Read Committed의 일반 SELECT는 문장 시작 시점의 snapshot을 사용하므로 두 번째 조회는 B가 commit한 20을 볼 수 있다. Repeatable Read는 첫 비트랜잭션 제어 문장이 만든 transaction snapshot을 유지하므로 같은 조건에서 10을 본다. BEGIN 시점 자체와 첫 데이터 문장 시점을 혼동하지 않는다. 자신의 쓰기는 볼 수 있다는 예외가 있지만 이 문항에는 자신의 변경이 없다.

**필수 개념**

- 문장 단위 snapshot · 가중치 0.50: PG17 Read Committed 일반 SELECT의 문장 시작 snapshot으로 두 번째 값20 설명.
- 트랜잭션 snapshot의 시점 · 가중치 0.50: PG17 Repeatable Read는 첫 비트랜잭션 제어 문장 snapshot으로10 유지. BEGIN 시점·자신의 변경 예외 구분.

**개념별 판정 경계**

- `TX_02_01`
  - CORRECT: PG17 Read Committed 일반 SELECT의 문장 시작 snapshot으로 두 번째 값20 설명.
  - PARTIALLY_CORRECT: 20을 맞히나 문장 snapshot과 commit 관계 누락.
  - INCORRECT: 같은 transaction이면 Read Committed도 무조건10이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TX_02_02`
  - CORRECT: PG17 Repeatable Read는 첫 비트랜잭션 제어 문장 snapshot으로10 유지. BEGIN 시점·자신의 변경 예외 구분.
  - PARTIALLY_CORRECT: 10을 맞히지만 첫 데이터 문장 시점과 자신의 변경 예외 누락.
  - INCORRECT: Repeatable Read도 매번 최신20을 보거나 자신의 쓰기도 절대 볼 수 없다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TX-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: Read Committed의 일반 SELECT는 문장 시작 시점의 snapshot을 사용하므로 두 번째 조회는 B가 commit한 20을 볼 수 있다. Repeatable Read는 첫 비트랜잭션 제어 문장이 만든 transaction snapshot을 유지하므로 같은 조건에서 10을 본다. BEGIN 시점 자체와 첫 데이터 문장 시점을 혼동하지 않는다. 자신의 쓰기는 볼 수 있다는 예외가 있지만 이 문항에는 자신의 변경이 없다.
  - 예상 개념 판정: TX_02_01: CORRECT, TX_02_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TX-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: A의 첫 읽기 후 B가 확정한 값은 RC의 다음 문장에 반영될 수 있다. RR은 BEGIN이 아니라 첫 데이터 문장의 snapshot으로 같은10을 읽는다. 자기 변경은 볼 수 있으나 여기서는 없다는 전제를 지킨다.
  - 예상 개념 판정: TX_02_01: CORRECT, TX_02_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TX-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: Read Committed는 다음 문장 시작 전에 B가 commit해 새 snapshot에서20을 본다. Repeatable Read는 같은 transaction의 snapshot을 유지해10이다.
  - 예상 개념 판정: TX_02_01: CORRECT, TX_02_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TX-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: Read Committed도 transaction 전체에10을 고정한다. Repeatable Read는 문장마다 최신20을 보고 자신의 쓰기는 절대 보지 못한다.
  - 예상 개념 판정: TX_02_01: INCORRECT, TX_02_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TX-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: Read Committed의 일반 SELECT는 문장 시작 시점의 snapshot을 사용하므로 두 번째 조회는 B가 commit한 20을 볼 수 있다. Repeatable Read는 첫 비트랜잭션 제어 문장이 만든 transaction snapshot을 유지하므로 같은 조건에서 10을 본다. BEGIN 시점 자체와 첫 데이터 문장 시점을 혼동하지 않는다. 자신의 쓰기는 볼 수 있다는 예외가 있지만 이 문항에는 자신의 변경이 없다.
  - 예상 개념 판정: TX_02_01: NEEDS_REVIEW, TX_02_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** MVCC·격리 동작을 PostgreSQL 17 조건으로 한정. 모든 DB에 같은 값 보장한다고 일반화하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 13.2. Transaction Isolation](https://www.postgresql.org/docs/17/transaction-iso.html) — §13.2.1 Read Committed, §13.2.2 Repeatable Read

근거 문서: `A1-TX-DOC`. 검수 상태: **PENDING**

### A1-TX-03 · INTERMEDIATE

PostgreSQL 17 Read Committed에서 재고10을 A와 B가 일반 SELECT로 읽고, 각각 UPDATE stock SET quantity=9 WHERE id=1을 실행해 둘 다 commit한다. 최종9가 의도한 두 번 차감 결과8이 아닌 이유와, 재고가 음수가 되지 않게 차감하는 대안을 설명하라.

**모범 답안**

두 요청이 같은 과거 값을 읽어 계산한 고정값9를 덮어써 한 번의 차감이 사라진다. UPDATE stock SET quantity=quantity-1 WHERE id=1 AND quantity>=1처럼 현재 행에 대한 조건부 원자 연산을 쓰고 영향 행 수가 1인지 확인하면 성공한 각 차감이 반영된다. PG17 RC의 경쟁 갱신은 새 행 버전에 조건을 재검사한다. version 조건 갱신과 충돌 처리, 실제 행 잠금을 쓰기까지 유지하는 대안도 가능하다. transaction을 붙이기만 해서는 고정값 계산 경쟁이 없어지지 않는다.

**필수 개념**

- 읽기·계산·덮어쓰기 경쟁 · 가중치 0.50: 같은10을 읽고 고정9를 두 번 쓰는 실행 순서로 최종9 설명. 두 commit이 각 차감 반영을 의미하지 않음.
- 조건부 원자 변경과 결과 확인 · 가중치 0.50: 현재 값의 원자 차감+quantity>=1+영향 행 수 확인 또는 동등한 version/행 잠금 방어. 충돌·재고 부족 결과 처리.

**개념별 판정 경계**

- `TX_03_01`
  - CORRECT: 같은10을 읽고 고정9를 두 번 쓰는 실행 순서로 최종9 설명. 두 commit이 각 차감 반영을 의미하지 않음.
  - PARTIALLY_CORRECT: 누락 갱신을 언급하나 고정값 덮어쓰기 과정 누락.
  - INCORRECT: 두 commit이면 주어진 고정9 UPDATE만으로 최종8이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TX_03_02`
  - CORRECT: 현재 값의 원자 차감+quantity>=1+영향 행 수 확인 또는 동등한 version/행 잠금 방어. 충돌·재고 부족 결과 처리.
  - PARTIALLY_CORRECT: 원자 차감·재고 조건은 제시하지만 실패·영향 행 수 확인 생략.
  - INCORRECT: 어노테이션 또는 읽기 전 메모리 검사만으로 여러 writer의 음수·경쟁이 차단된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TX-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 두 요청이 같은 과거 값을 읽어 계산한 고정값9를 덮어써 한 번의 차감이 사라진다. UPDATE stock SET quantity=quantity-1 WHERE id=1 AND quantity>=1처럼 현재 행에 대한 조건부 원자 연산을 쓰고 영향 행 수가 1인지 확인하면 성공한 각 차감이 반영된다. PG17 RC의 경쟁 갱신은 새 행 버전에 조건을 재검사한다. version 조건 갱신과 충돌 처리, 실제 행 잠금을 쓰기까지 유지하는 대안도 가능하다. transaction을 붙이기만 해서는 고정값 계산 경쟁이 없어지지 않는다.
  - 예상 개념 판정: TX_03_01: CORRECT, TX_03_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TX-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 읽은 값이 아니라 DB의 현재 행을 quantity-1로 바꾸고 부족 조건을 같은 SQL에서 검사한다. 영향 행 수0이면 실패로 처리한다. 또는 version 충돌 처리·실제 행 잠금을 쓰기까지 유지할 수 있다. 원래 두 writer는 둘 다9를 기록해 결과가9다.
  - 예상 개념 판정: TX_03_01: CORRECT, TX_03_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TX-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 둘 다10을 읽고9로 덮어써 한 차감이 사라졌다. 현재 값에서 원자적으로 빼는 UPDATE와 quantity>=1 조건을 사용한다.
  - 예상 개념 판정: TX_03_01: CORRECT, TX_03_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TX-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 두 요청이 commit하면 고정9를 저장해도 결과는8이다. transaction 어노테이션과 메모리 검사만 있으면 여러 서버의 음수 재고가 자동 차단된다.
  - 예상 개념 판정: TX_03_01: INCORRECT, TX_03_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TX-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 두 요청이 같은 과거 값을 읽어 계산한 고정값9를 덮어써 한 번의 차감이 사라진다. UPDATE stock SET quantity=quantity-1 WHERE id=1 AND quantity>=1처럼 현재 행에 대한 조건부 원자 연산을 쓰고 영향 행 수가 1인지 확인하면 성공한 각 차감이 반영된다. PG17 RC의 경쟁 갱신은 새 행 버전에 조건을 재검사한다. version 조건 갱신과 충돌 처리, 실제 행 잠금을 쓰기까지 유지하는 대안도 가능하다. transaction을 붙이기만 해서는 고정값 계산 경쟁이 없어지지 않는다.
  - 예상 개념 판정: TX_03_01: NEEDS_REVIEW, TX_03_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 대안별 격리·재시도·잠금 유지 조건을 제시하면 특정 해법과 다르다는 이유로 감점하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 13.2. Transaction Isolation](https://www.postgresql.org/docs/17/transaction-iso.html) — §13.2.1 경쟁 UPDATE의 wait와 WHERE 재평가
- [PostgreSQL: Documentation: 17: 13.3. Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html) — §13.3.2 Row-Level Locks

근거 문서: `A1-TX-DOC`. 검수 상태: **PENDING**

### A1-TX-04 · ADVANCED

PostgreSQL 17 Serializable transaction이 SQLSTATE 40001로 실패했다. 마지막 UPDATE만 같은 transaction에서 다시 실행하면 되는가? 올바른 재시도 범위와 무한 재시도·외부 부수 효과의 한계를 설명하라.

**모범 답안**

40001 직렬화 실패는 새 transaction과 snapshot에서 전체 읽기·판단·쓰기를 다시 수행해야 한다. 같은 실패 transaction의 마지막 문장만 반복하면 이전 snapshot의 판단을 그대로 쓰며 정상 복구하지 못한다. 시도 횟수·총 시간·backoff와 종료 응답을 제한하고 재시도 중 외부 효과가 있다면 멱등성·상태 확인·보상 조건을 별도 설계한다. Serializable은 모든 경쟁 요청이 항상 성공하는 설정이 아니다.

**필수 개념**

- 직렬화 실패의 재시도 범위 · 가중치 0.50: PG17 40001은 새 transaction에서 전체 읽기·판단·쓰기 재수행. 같은 실패 transaction의 마지막 SQL만 반복하는 방법 배제.
- 재시도 예산과 부수 효과 · 가중치 0.50: 횟수·시간·backoff·종료 결과 제한과 외부 효과 중복 방어 조건 제시. 항상 성공·무한 재시도 보장 금지.

**개념별 판정 경계**

- `TX_04_01`
  - CORRECT: PG17 40001은 새 transaction에서 전체 읽기·판단·쓰기 재수행. 같은 실패 transaction의 마지막 SQL만 반복하는 방법 배제.
  - PARTIALLY_CORRECT: 전체 재시도를 제시하나 새 snapshot 이유 생략.
  - INCORRECT: 같은 실패 transaction의 마지막 UPDATE만 반복하면 항상 복구된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TX_04_02`
  - CORRECT: 횟수·시간·backoff·종료 결과 제한과 외부 효과 중복 방어 조건 제시. 항상 성공·무한 재시도 보장 금지.
  - PARTIALLY_CORRECT: 재시도 제한은 언급하지만 외부 효과 조건 누락.
  - INCORRECT: 무한 재시도가 항상 안전하며 외부 결제도 무조건 한 번만 발생한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TX-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 40001 직렬화 실패는 새 transaction과 snapshot에서 전체 읽기·판단·쓰기를 다시 수행해야 한다. 같은 실패 transaction의 마지막 문장만 반복하면 이전 snapshot의 판단을 그대로 쓰며 정상 복구하지 못한다. 시도 횟수·총 시간·backoff와 종료 응답을 제한하고 재시도 중 외부 효과가 있다면 멱등성·상태 확인·보상 조건을 별도 설계한다. Serializable은 모든 경쟁 요청이 항상 성공하는 설정이 아니다.
  - 예상 개념 판정: TX_04_01: CORRECT, TX_04_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TX-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: Serializable은 충돌을 취소로 드러낼 수 있다. 이전 snapshot의 결정을 버리고 새 transaction에서 전체 작업을 재수행한다. 재시도 예산을 정하고 외부 호출이 포함되면 그 제공자의 식별·중복 방어·대사 조건도 확인한다.
  - 예상 개념 판정: TX_04_01: CORRECT, TX_04_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TX-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 40001이면 새 transaction에서 전체 읽기와 판단부터 다시 실행한다. 같은 실패 transaction의 마지막 SQL만 반복하면 안 된다. 횟수와 시간·backoff를 제한한다.
  - 예상 개념 판정: TX_04_01: CORRECT, TX_04_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TX-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 같은 실패 transaction의 마지막 UPDATE만 반복하면 항상 복구된다. 무한 재시도는 안전하고 외부 결제는 무조건 한 번만 일어난다.
  - 예상 개념 판정: TX_04_01: INCORRECT, TX_04_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TX-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 40001 직렬화 실패는 새 transaction과 snapshot에서 전체 읽기·판단·쓰기를 다시 수행해야 한다. 같은 실패 transaction의 마지막 문장만 반복하면 이전 snapshot의 판단을 그대로 쓰며 정상 복구하지 못한다. 시도 횟수·총 시간·backoff와 종료 응답을 제한하고 재시도 중 외부 효과가 있다면 멱등성·상태 확인·보상 조건을 별도 설계한다. Serializable은 모든 경쟁 요청이 항상 성공하는 설정이 아니다.
  - 예상 개념 판정: TX_04_01: NEEDS_REVIEW, TX_04_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 이 문항의 retry는 DB 전체 transaction 단위. HTTP 재전송의 의미는 별도 Topic 소유.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 13.2. Transaction Isolation](https://www.postgresql.org/docs/17/transaction-iso.html) — §13.2.3 Serializable: serialization failure와 whole transaction retry

근거 문서: `A1-TX-DOC`. 검수 상태: **PENDING**

### A1-TX-05 · ADVANCED

PostgreSQL 17 Read Committed에서 예약이 아직 없는 슬롯을 SELECT ... FOR UPDATE로 조회해 0행을 얻었다. 이것만으로 같은 슬롯의 동시 INSERT를 막을 수 있는가? 예약 유일성을 보장할 대안과 조건을 설명하라.

**모범 답안**

FOR UPDATE는 실제 조회한 행을 잠그므로 0행이면 존재하지 않는 예약 행에 행 잠금이 생기지 않는다. PG17 RC에서 이를 다른 DB의 gap lock처럼 미래 삽입 금지로 해석하면 틀리다. non-NULL 슬롯 조합 UNIQUE와 충돌 처리로 유일성을 보장하거나, 모든 writer가 동일한 기존 슬롯·부모 행을 잠그고 확인·삽입하도록 설계할 수 있다. Serializable의 전체 재시도도 요구에 따라 검토할 수 있으나 실패 가능성까지 처리해야 한다.

**필수 개념**

- 행 잠금의 대상 범위 · 가중치 0.50: PG17 RC SELECT FOR UPDATE는 조회된 실제 행 대상. 0행을 미래 예약 삽입에 대한 행 잠금으로 오인하지 않음.
- 부재 상태 보호의 조건 · 가중치 0.50: 슬롯 UNIQUE+충돌 처리 또는 모든 writer의 같은 기존 행 잠금 등 방어 조건 제시. 격리·실패 처리 범위 구분.

**개념별 판정 경계**

- `TX_05_01`
  - CORRECT: PG17 RC SELECT FOR UPDATE는 조회된 실제 행 대상. 0행을 미래 예약 삽입에 대한 행 잠금으로 오인하지 않음.
  - PARTIALLY_CORRECT: 0행이면 못 막는다고 답하나 실제 행 대상 설명 누락.
  - INCORRECT: 없는 행에도 자동 gap lock이 걸려 모든 INSERT를 막는다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TX_05_02`
  - CORRECT: 슬롯 UNIQUE+충돌 처리 또는 모든 writer의 같은 기존 행 잠금 등 방어 조건 제시. 격리·실패 처리 범위 구분.
  - PARTIALLY_CORRECT: UNIQUE 또는 부모 잠금을 제시하나 충돌 처리·모든 writer 참여 조건 누락.
  - INCORRECT: 존재 확인만 하면 충분하거나 일부 writer만 부모를 잠가도 모든 경로가 보호된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TX-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: FOR UPDATE는 실제 조회한 행을 잠그므로 0행이면 존재하지 않는 예약 행에 행 잠금이 생기지 않는다. PG17 RC에서 이를 다른 DB의 gap lock처럼 미래 삽입 금지로 해석하면 틀리다. non-NULL 슬롯 조합 UNIQUE와 충돌 처리로 유일성을 보장하거나, 모든 writer가 동일한 기존 슬롯·부모 행을 잠그고 확인·삽입하도록 설계할 수 있다. Serializable의 전체 재시도도 요구에 따라 검토할 수 있으나 실패 가능성까지 처리해야 한다.
  - 예상 개념 판정: TX_05_01: CORRECT, TX_05_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TX-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 잠글 예약이 없다는 것이 문제다. DB의 조합 유일성과 위반 처리를 두거나 공통의 기존 슬롯 행을 모든 writer가 같은 방식으로 잠근 뒤 판단하게 만든다. Serializable을 선택해도 취소·재시도를 처리해야 한다.
  - 예상 개념 판정: TX_05_01: CORRECT, TX_05_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TX-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 조회한 실제 행에만 행 잠금이 걸려 0행이면 미래 INSERT를 막지 못한다. 슬롯 조합 UNIQUE나 기존 슬롯 행 잠금을 사용할 수 있다.
  - 예상 개념 판정: TX_05_01: CORRECT, TX_05_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TX-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 없는 행에도 PG17 RC의 자동 gap lock이 걸린다. 사전 존재 확인만 있거나 일부 writer만 부모를 잠가도 모든 INSERT가 차단된다.
  - 예상 개념 판정: TX_05_01: INCORRECT, TX_05_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TX-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: FOR UPDATE는 실제 조회한 행을 잠그므로 0행이면 존재하지 않는 예약 행에 행 잠금이 생기지 않는다. PG17 RC에서 이를 다른 DB의 gap lock처럼 미래 삽입 금지로 해석하면 틀리다. non-NULL 슬롯 조합 UNIQUE와 충돌 처리로 유일성을 보장하거나, 모든 writer가 동일한 기존 슬롯·부모 행을 잠그고 확인·삽입하도록 설계할 수 있다. Serializable의 전체 재시도도 요구에 따라 검토할 수 있으나 실패 가능성까지 처리해야 한다.
  - 예상 개념 판정: TX_05_01: NEEDS_REVIEW, TX_05_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** DB 종류·격리 수준을 생략한 채 gap lock을 일반화하지 않음.

**출처 대조 위치**

- [PostgreSQL: Documentation: 17: 13.3. Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html) — §13.3.2 FOR UPDATE는 조회된 행에 대한 잠금
- [PostgreSQL: Documentation: 17: 13.2. Transaction Isolation](https://www.postgresql.org/docs/17/transaction-iso.html) — §13.2.1 RC, §13.2.3 Serializable
- [PostgreSQL: Documentation: 17: 5.5. Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — §5.5.3 UNIQUE

근거 문서: `A1-TX-DOC`. 검수 상태: **PENDING**

### A1-HTTP-01 · BASIC

동일한 DELETE /items/7 요청의 첫 응답은204, 재전송 응답은404였다. 두 요청 모두 결과적으로 해당 항목이 존재하지 않게 만들었다. 응답이 달라서 비멱등인가? 안전성과 멱등성의 차이를 설명하라.

**모범 답안**

멱등성은 같은 요청을 반복했을 때 의도한 서버 효과가 한 번 실행한 것과 같은지에 관한 성질이다. 항목이 없는 최종 효과가 같으므로 204와404 차이만으로 비멱등이라고 하지 않는다. 응답·로그가 모두 같아야 하는 것은 아니다. DELETE는 상태 제거를 의도하므로 안전하지 않지만 HTTP에서 멱등 메서드다. 실제 구현이 다른 의도한 효과를 추가하는 경우는 별도로 확인한다.

**필수 개념**

- 의도한 효과의 멱등성 · 가중치 0.50: 반복 DELETE의 의도한 최종 효과와 응답 동일성을 분리. 204/404 차이만으로 비멱등 판정 금지.
- 안전성과 변경 의도 · 가중치 0.50: 안전성은 클라이언트의 상태 변경 의도 기준. DELETE는 멱등이어도 안전하지 않음.

**개념별 판정 경계**

- `HTTP_01_01`
  - CORRECT: 반복 DELETE의 의도한 최종 효과와 응답 동일성을 분리. 204/404 차이만으로 비멱등 판정 금지.
  - PARTIALLY_CORRECT: 효과가 같다는 결론만 제시하고 응답·부수 기록 차이 설명 누락.
  - INCORRECT: 응답 코드가 다르면 의도한 효과와 무관하게 비멱등이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `HTTP_01_02`
  - CORRECT: 안전성은 클라이언트의 상태 변경 의도 기준. DELETE는 멱등이어도 안전하지 않음.
  - PARTIALLY_CORRECT: DELETE가 안전하지 않다고 답하나 변경 의도 기준 누락.
  - INCORRECT: 멱등이면 반드시 안전하고 DELETE는 상태 변경을 의도하지 않는다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-HTTP-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 멱등성은 같은 요청을 반복했을 때 의도한 서버 효과가 한 번 실행한 것과 같은지에 관한 성질이다. 항목이 없는 최종 효과가 같으므로 204와404 차이만으로 비멱등이라고 하지 않는다. 응답·로그가 모두 같아야 하는 것은 아니다. DELETE는 상태 제거를 의도하므로 안전하지 않지만 HTTP에서 멱등 메서드다. 실제 구현이 다른 의도한 효과를 추가하는 경우는 별도로 확인한다.
  - 예상 개념 판정: HTTP_01_01: CORRECT, HTTP_01_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-HTTP-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 두 번째 제거가 최종 상태를 추가 변경하지 않으므로 반복 효과는 같다. HTTP 멱등성은 응답 복제 계약이 아니다. 제거를 요청하는 DELETE는 안전성의 무변경 의도를 충족하지 않으면서 멱등일 수 있다.
  - 예상 개념 판정: HTTP_01_01: CORRECT, HTTP_01_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-HTTP-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 둘 다 항목이 없는 같은 효과라 응답 코드와 로그가 달라도 멱등이다. DELETE는 안전하지 않다.
  - 예상 개념 판정: HTTP_01_01: CORRECT, HTTP_01_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-HTTP-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 204와404가 다르면 효과와 무관하게 비멱등이다. 멱등 메서드는 반드시 안전하고 DELETE는 상태 변경을 의도하지 않는다.
  - 예상 개념 판정: HTTP_01_01: INCORRECT, HTTP_01_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-HTTP-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 멱등성은 같은 요청을 반복했을 때 의도한 서버 효과가 한 번 실행한 것과 같은지에 관한 성질이다. 항목이 없는 최종 효과가 같으므로 204와404 차이만으로 비멱등이라고 하지 않는다. 응답·로그가 모두 같아야 하는 것은 아니다. DELETE는 상태 제거를 의도하므로 안전하지 않지만 HTTP에서 멱등 메서드다. 실제 구현이 다른 의도한 효과를 추가하는 경우는 별도로 확인한다.
  - 예상 개념 판정: HTTP_01_01: NEEDS_REVIEW, HTTP_01_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 응답 동일성·멱등성·안전성 세 성질을 한 가지로 판단하지 않음.

**출처 대조 위치**

- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §9.2.1 Safe Methods, §9.2.2 Idempotent Methods

근거 문서: `A1-HTTP-DOC`. 검수 상태: **PENDING**

### A1-HTTP-02 · INTERMEDIATE

응답 Cache-Control에 필드명 없는 no-cache, no-store, private를 각각 사용할 때 저장과 재사용 규칙은 어떻게 다른가? private이면 브라우저도 저장하지 않는지, no-store가 모든 개인정보 복사까지 막는지 설명하라.

**모범 답안**

no-cache는 저장 자체를 막지 않고 재사용 전에 원 서버의 성공적인 검증을 요구한다. no-store는 이를 준수하는 캐시가 요청·응답을 저장하거나 다른 요청에 재사용하지 못하게 한다. private는 공유 캐시의 저장을 제한하지만 사용자별 private cache의 저장을 전부 막는 지시자는 아니다. no-store도 이미 복사된 정보나 악성·비준수 저장까지 모두 막는 개인정보 보장이 아니다.

**필수 개념**

- 저장과 재검증의 구분 · 가중치 0.50: 필드명 없는 no-cache의 저장 가능·재검증 요구와 no-store의 저장·재사용 금지를 구분.
- 캐시 적용 범위 · 가중치 0.50: private의 공유/개별 캐시 구분과 no-store의 비준수 저장·기존 복사에 대한 보장 한계 설명.

**개념별 판정 경계**

- `HTTP_02_01`
  - CORRECT: 필드명 없는 no-cache의 저장 가능·재검증 요구와 no-store의 저장·재사용 금지를 구분.
  - PARTIALLY_CORRECT: no-cache와 no-store를 구분하되 검증 시점 일부 누락.
  - INCORRECT: no-cache가 모든 저장을 금지하고 no-store는 저장 후 재검증만 요구한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `HTTP_02_02`
  - CORRECT: private의 공유/개별 캐시 구분과 no-store의 비준수 저장·기존 복사에 대한 보장 한계 설명.
  - PARTIALLY_CORRECT: private가 공유 캐시를 제한한다고 설명하나 no-store의 정보 보호 한계 누락.
  - INCORRECT: private는 브라우저도 저장 금지이고 no-store면 모든 복사가 사라진다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-HTTP-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: no-cache는 저장 자체를 막지 않고 재사용 전에 원 서버의 성공적인 검증을 요구한다. no-store는 이를 준수하는 캐시가 요청·응답을 저장하거나 다른 요청에 재사용하지 못하게 한다. private는 공유 캐시의 저장을 제한하지만 사용자별 private cache의 저장을 전부 막는 지시자는 아니다. no-store도 이미 복사된 정보나 악성·비준수 저장까지 모두 막는 개인정보 보장이 아니다.
  - 예상 개념 판정: HTTP_02_01: CORRECT, HTTP_02_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-HTTP-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 캐시에 넣어도 되는지와 넣은 값을 바로 써도 되는지는 별개다. no-cache는 후자의 검증을 요구하고 no-store는 저장·재사용을 금한다. private는 공유 캐시용 제한이며 악성 저장이나 기존 정보 복사까지 HTTP 헤더가 통제하지는 못한다.
  - 예상 개념 판정: HTTP_02_01: CORRECT, HTTP_02_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-HTTP-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: no-cache는 저장 가능하지만 재사용 전 검증이 필요하고 no-store는 준수하는 캐시의 저장과 재사용을 막는다. private는 공유 캐시를 제한한다.
  - 예상 개념 판정: HTTP_02_01: CORRECT, HTTP_02_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-HTTP-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: no-cache는 저장을 모두 금지하고 no-store는 저장 후 검증만 요구한다. private는 브라우저도 금지하며 no-store를 쓰면 모든 사본이 삭제된다.
  - 예상 개념 판정: HTTP_02_01: INCORRECT, HTTP_02_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-HTTP-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: no-cache는 저장 자체를 막지 않고 재사용 전에 원 서버의 성공적인 검증을 요구한다. no-store는 이를 준수하는 캐시가 요청·응답을 저장하거나 다른 요청에 재사용하지 못하게 한다. private는 공유 캐시의 저장을 제한하지만 사용자별 private cache의 저장을 전부 막는 지시자는 아니다. no-store도 이미 복사된 정보나 악성·비준수 저장까지 모두 막는 개인정보 보장이 아니다.
  - 예상 개념 판정: HTTP_02_01: NEEDS_REVIEW, HTTP_02_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 이 문항은 field-name 없는 지시자 조건. qualified 지시자의 세부 의미를 생략하고 일반화하지 않음.

**출처 대조 위치**

- [RFC 9111: HTTP Caching](https://www.rfc-editor.org/rfc/rfc9111.html) — RFC 9111 §5.2.2.4 no-cache, §5.2.2.5 no-store, §5.2.2.7 private

근거 문서: `A1-HTTP-DOC`. 검수 상태: **PENDING**

### A1-HTTP-03 · INTERMEDIATE

클라이언트는 ETag "v3"인 표현을 갖고 있다. GET에 If-None-Match: "v3"를 보낸 경우와 변경 요청에 If-Match: "v3"를 보낸 경우를 비교하라. GET 시 현재 태그가 v3이고, 변경 시 현재 태그는 v4다. 다른 오류나 선행 조건은 없으며 서버는 중복 성공 요청으로 확인하지 않았다. 서버의 검사·변경 경계도 설명하라.

**모범 답안**

GET에서 태그가 같으면 If-None-Match 조건이 실패해304로 기존 표현을 재사용할 수 있다. 변경 요청에서 현재 v4와 If-Match v3가 불일치하면 사전 조건 실패412로 변경을 수행하지 않는다. 다른 오류·조건 우선순위나 이미 처리된 요청의 예외가 없는 이 문항의 전제다. 변경 시 태그 비교와 갱신은 원자적으로 보장해 중간 변경 경쟁을 막아야 하며, ETag 조건은 인증·인가를 대체하지 않는다.

**필수 개념**

- 조건부 읽기 검증 · 가중치 0.50: 주어진 GET의 If-None-Match 일치 조건에서304와 기존 표현 재사용 설명.
- 변경 사전 조건의 방어 · 가중치 0.50: 주어진 If-Match 불일치에서412·미변경. 검사와 변경을 함께 보장하고 인증과 구분.

**개념별 판정 경계**

- `HTTP_03_01`
  - CORRECT: 주어진 GET의 If-None-Match 일치 조건에서304와 기존 표현 재사용 설명.
  - PARTIALLY_CORRECT: 304를 맞히나 조건부 읽기 이유 누락.
  - INCORRECT: 일치 GET이 반드시 새 본문의200이나412라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `HTTP_03_02`
  - CORRECT: 주어진 If-Match 불일치에서412·미변경. 검사와 변경을 함께 보장하고 인증과 구분.
  - PARTIALLY_CORRECT: 412·미변경을 맞히나 비교와 변경의 원자성 조건 누락.
  - INCORRECT: 불일치에도 무조건 수정하며 분리된 검사만으로 경쟁을 막거나 인증이 대체된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-HTTP-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: GET에서 태그가 같으면 If-None-Match 조건이 실패해304로 기존 표현을 재사용할 수 있다. 변경 요청에서 현재 v4와 If-Match v3가 불일치하면 사전 조건 실패412로 변경을 수행하지 않는다. 다른 오류·조건 우선순위나 이미 처리된 요청의 예외가 없는 이 문항의 전제다. 변경 시 태그 비교와 갱신은 원자적으로 보장해 중간 변경 경쟁을 막아야 하며, ETag 조건은 인증·인가를 대체하지 않는다.
  - 예상 개념 판정: HTTP_03_01: CORRECT, HTTP_03_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-HTTP-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 읽기에는 표현이 그대로인지 확인해304를 받는 조건을 쓰고, 쓰기에는 내가 아는 버전이 현재와 같아야 한다는 조건을 쓴다. 이 전제에서 낡은 v3 쓰기는412로 거부하며 비교와 수정 사이에 다른 쓰기가 끼지 않게 보장한다. 인증은 별도다.
  - 예상 개념 판정: HTTP_03_01: CORRECT, HTTP_03_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-HTTP-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: GET 태그가 같으니304로 기존 표현을 재사용한다. 변경은 v3와 현재v4가 달라412로 거부하며 ETag는 인증을 대신하지 않는다.
  - 예상 개념 판정: HTTP_03_01: CORRECT, HTTP_03_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-HTTP-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 태그가 일치하는 GET은412다. 변경 태그가 달라도 수정하고, 태그를 먼저 조회만 하면 경쟁은 막히며 인증도 필요 없다.
  - 예상 개념 판정: HTTP_03_01: INCORRECT, HTTP_03_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-HTTP-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: GET에서 태그가 같으면 If-None-Match 조건이 실패해304로 기존 표현을 재사용할 수 있다. 변경 요청에서 현재 v4와 If-Match v3가 불일치하면 사전 조건 실패412로 변경을 수행하지 않는다. 다른 오류·조건 우선순위나 이미 처리된 요청의 예외가 없는 이 문항의 전제다. 변경 시 태그 비교와 갱신은 원자적으로 보장해 중간 변경 경쟁을 막아야 하며, ETag 조건은 인증·인가를 대체하지 않는다.
  - 예상 개념 판정: HTTP_03_01: NEEDS_REVIEW, HTTP_03_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** HTTP 사전 조건의 우선순위·이미 적용한 요청 예외를 무시한 항상412 일반화를 피함.

**출처 대조 위치**

- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §13.1.1 If-Match, §13.1.2 If-None-Match, §13.2.2·§15.4.5·§15.5.13

근거 문서: `A1-HTTP-DOC`. 검수 상태: **PENDING**

### A1-HTTP-04 · INTERMEDIATE

API 요청에서 자격 증명이 없거나 무효일 때와, 요청을 이해했지만 수행을 거부할 때 각각401·403을 어떻게 해석하는가? 403이 로그인 성공을 항상 증명하는지, 자원 존재를 숨기려404를 사용할 수 있는지 설명하라.

**모범 답안**

401은 요청에 유효한 인증 자격이 부족함을 나타내며 WWW-Authenticate challenge와 연결된다. 403은 서버가 요청을 이해했지만 수행을 거부함을 뜻하고, 자격은 유효하지만 권한이 부족한 경우도 포함한다. 403 자체가 항상 인증 완료를 증명하지 않는다. 금지 자원의 존재를 숨기려404로 응답하는 정책도 가능하다. 상태 코드 선택과 실제 인증·인가 검사는 별개 책임이다.

**필수 개념**

- 인증 challenge와 수행 거부 · 가중치 0.50: 401의 유효 인증 자격 부족·challenge와403의 이해 후 수행 거부를 구분.
- 오류 응답의 정보 공개 정책 · 가중치 0.50: 403은 인증 완료의 항상 보장이 아님. 존재 은폐를 위한404 가능성과 실제 인가 검사 별도 설명.

**개념별 판정 경계**

- `HTTP_04_01`
  - CORRECT: 401의 유효 인증 자격 부족·challenge와403의 이해 후 수행 거부를 구분.
  - PARTIALLY_CORRECT: 401/403 방향은 맞지만 challenge 또는 거부 의미 일부 누락.
  - INCORRECT: 401은 인증 성공·권한 부족이고403은 항상 자원 없음이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `HTTP_04_02`
  - CORRECT: 403은 인증 완료의 항상 보장이 아님. 존재 은폐를 위한404 가능성과 실제 인가 검사 별도 설명.
  - PARTIALLY_CORRECT: 404로 숨길 수 있다고 답하나403의 인증 보장 한계 누락.
  - INCORRECT: 403이면 로그인 성공이 확정되고 금지 자원에404는 절대 불가능하다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-HTTP-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 401은 요청에 유효한 인증 자격이 부족함을 나타내며 WWW-Authenticate challenge와 연결된다. 403은 서버가 요청을 이해했지만 수행을 거부함을 뜻하고, 자격은 유효하지만 권한이 부족한 경우도 포함한다. 403 자체가 항상 인증 완료를 증명하지 않는다. 금지 자원의 존재를 숨기려404로 응답하는 정책도 가능하다. 상태 코드 선택과 실제 인증·인가 검사는 별개 책임이다.
  - 예상 개념 판정: HTTP_04_01: CORRECT, HTTP_04_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-HTTP-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 인증 자격을 요구하는401과 수행을 거부하는403의 의미를 먼저 구분한다.403이 언제나 로그인 완료를 알려주지는 않는다. 서버는 금지 자원의 존재를 숨기는404 정책도 선택할 수 있지만 인가 검사를 생략하는 근거는 아니다.
  - 예상 개념 판정: HTTP_04_01: CORRECT, HTTP_04_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-HTTP-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 401은 유효 인증 자격 부족과 challenge,403은 이해한 요청 수행 거부다. 금지 자원의 존재를 숨기려404를 선택할 수 있다.
  - 예상 개념 판정: HTTP_04_01: CORRECT, HTTP_04_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-HTTP-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 401은 로그인 성공 후 권한 부족이고403은 항상 없는 자원이다.403이면 로그인 성공이 확정되며 금지 자원에404는 불가능하다.
  - 예상 개념 판정: HTTP_04_01: INCORRECT, HTTP_04_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-HTTP-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 401은 요청에 유효한 인증 자격이 부족함을 나타내며 WWW-Authenticate challenge와 연결된다. 403은 서버가 요청을 이해했지만 수행을 거부함을 뜻하고, 자격은 유효하지만 권한이 부족한 경우도 포함한다. 403 자체가 항상 인증 완료를 증명하지 않는다. 금지 자원의 존재를 숨기려404로 응답하는 정책도 가능하다. 상태 코드 선택과 실제 인증·인가 검사는 별개 책임이다.
  - 예상 개념 판정: HTTP_04_01: NEEDS_REVIEW, HTTP_04_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 상태 코드가 인증·인가 구현을 대신하거나 403이 항상 인증 완료라는 암기식 답변 금지.

**출처 대조 위치**

- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §15.5.2 401, §15.5.4 403의404 대안

근거 문서: `A1-HTTP-DOC`. 검수 상태: **PENDING**

### A1-HTTP-05 · INTERMEDIATE

POST 결제 요청 처리 뒤 서버가 Location: /receipts/8과 함께303 또는307을 반환할 수 있다고 하자. 자동 redirect에서 목적지 요청 메서드는 어떻게 달라지는가? redirect만으로 결제 중복을 방지했다고 할 수 있는지 설명하라.

**모범 답안**

303은 원 요청의 결과를 다른 자원에서 조회하게 하며 목적지에는 GET 또는 HEAD 조회를 사용한다. 307은 자동 redirect에서 메서드를 바꾸지 않아 POST와 본문이 목적지로 다시 전송될 수 있다. 따라서 목적지의 POST 계약과 부수 효과를 확인해야 한다.303으로 영수증을 조회하게 하더라도 원 POST 재전송·결제 처리의 중복 방어가 자동 구현되는 것은 아니다. 요청 식별과 서버 API 계약은 별도다.

**필수 개념**

- redirect의 메서드 의미 · 가중치 0.50: 303의 다른 자원 GET/HEAD 조회와307의 자동 redirect 메서드 유지 구분.
- redirect와 업무 효과의 경계 · 가중치 0.50: 307의 목적지 POST 효과와 원 요청 재전송 계약 확인.303도 원 결제의 중복 방어를 자동 보장하지 않음.

**개념별 판정 경계**

- `HTTP_05_01`
  - CORRECT: 303의 다른 자원 GET/HEAD 조회와307의 자동 redirect 메서드 유지 구분.
  - PARTIALLY_CORRECT: 303/307 방향만 제시하고 목적지 의미 생략.
  - INCORRECT: 303은 항상 POST 유지하고307은 항상 GET으로 바뀐다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `HTTP_05_02`
  - CORRECT: 307의 목적지 POST 효과와 원 요청 재전송 계약 확인.303도 원 결제의 중복 방어를 자동 보장하지 않음.
  - PARTIALLY_CORRECT: 목적지 계약 확인은 제시하나303의 원 요청 중복 한계 누락.
  - INCORRECT: redirect만 있으면 어느 목적지 계약에서도 결제가 무조건 한 번만 처리된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-HTTP-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 303은 원 요청의 결과를 다른 자원에서 조회하게 하며 목적지에는 GET 또는 HEAD 조회를 사용한다. 307은 자동 redirect에서 메서드를 바꾸지 않아 POST와 본문이 목적지로 다시 전송될 수 있다. 따라서 목적지의 POST 계약과 부수 효과를 확인해야 한다.303으로 영수증을 조회하게 하더라도 원 POST 재전송·결제 처리의 중복 방어가 자동 구현되는 것은 아니다. 요청 식별과 서버 API 계약은 별도다.
  - 예상 개념 판정: HTTP_05_01: CORRECT, HTTP_05_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-HTTP-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 영수증을 조회하게 할 목적이면303의 다른 자원 조회 의미를 쓸 수 있다.307 자동 이동은 기존 POST와 본문을 유지하므로 목적지 계약을 봐야 한다. 어떤 코드도 원 결제 재전송에 대한 요청 식별·중복 처리를 대신하지 않는다.
  - 예상 개념 판정: HTTP_05_01: CORRECT, HTTP_05_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-HTTP-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 303은 GET/HEAD로 결과를 조회하고307은 POST 메서드를 유지한다.307 목적지에서 POST가 만드는 효과를 계약으로 확인해야 한다.
  - 예상 개념 판정: HTTP_05_01: CORRECT, HTTP_05_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-HTTP-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 303은 POST를 유지하고307은 항상 GET이다. redirect만 있으면 어떤 목적지 계약이어도 결제가 무조건 한 번만 발생한다.
  - 예상 개념 판정: HTTP_05_01: INCORRECT, HTTP_05_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-HTTP-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 303은 원 요청의 결과를 다른 자원에서 조회하게 하며 목적지에는 GET 또는 HEAD 조회를 사용한다. 307은 자동 redirect에서 메서드를 바꾸지 않아 POST와 본문이 목적지로 다시 전송될 수 있다. 따라서 목적지의 POST 계약과 부수 효과를 확인해야 한다.303으로 영수증을 조회하게 하더라도 원 POST 재전송·결제 처리의 중복 방어가 자동 구현되는 것은 아니다. 요청 식별과 서버 API 계약은 별도다.
  - 예상 개념 판정: HTTP_05_01: NEEDS_REVIEW, HTTP_05_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** POST가 재전송될 수 있다는 조건과 실제 중복 결제가 발생했다는 관찰을 구분.

**출처 대조 위치**

- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §15.4.4 303 See Other, §15.4.8 307 Temporary Redirect

근거 문서: `A1-HTTP-DOC`. 검수 상태: **PENDING**

### A1-API-01 · BASIC

한 서비스가 명사형 URI와 GET·POST·PUT·DELETE, JSON을 사용한다는 정보만 있다. 이를 REST의 모든 제약을 충족한 API라고 확정할 수 있는가? Uniform interface에 필요한 내용과 확인하지 못한 제약을 설명하라.

**모범 답안**

URI·CRUD·JSON은 REST 전체 충족의 충분한 증거가 아니다. Uniform interface는 자원 식별, 표현을 통한 조작, 자기 설명 메시지, hypermedia로 이끄는 애플리케이션 상태 전이를 포함한다. client-server, stateless, cache, layered system 제약도 실제 동작에서 확인해야 하고 code-on-demand는 선택적이다. 제시되지 않은 동작을 추정해 REST 충족 또는 불충족을 확정하지 않는다.

**필수 개념**

- REST의 uniform interface · 가중치 0.50: 자원 식별·표현 조작·자기 설명 메시지·hypermedia 상태 전이를 포함. URI·JSON만으로 충분하다고 판단하지 않음.
- 아키텍처 제약의 관찰 범위 · 가중치 0.50: client-server·stateless·cache·layered system의 추가 관찰과 선택 code-on-demand 구분. 정보 부족 상태에서 전체 충족/불충족 확정 금지.

**개념별 판정 경계**

- `API_01_01`
  - CORRECT: 자원 식별·표현 조작·자기 설명 메시지·hypermedia 상태 전이를 포함. URI·JSON만으로 충분하다고 판단하지 않음.
  - PARTIALLY_CORRECT: uniform interface 일부만 설명하고 나머지 누락.
  - INCORRECT: JSON·명사 URI·CRUD만으로 uniform interface의 모든 요구가 증명된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `API_01_02`
  - CORRECT: client-server·stateless·cache·layered system의 추가 관찰과 선택 code-on-demand 구분. 정보 부족 상태에서 전체 충족/불충족 확정 금지.
  - PARTIALLY_CORRECT: 추가 제약 확인은 제시하지만 선택 code-on-demand 또는 정보 부족 구분 누락.
  - INCORRECT: 추가 제약 확인이 불필요하고 code-on-demand가 필수 또는 JSON 부재만으로 비REST라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-API-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: URI·CRUD·JSON은 REST 전체 충족의 충분한 증거가 아니다. Uniform interface는 자원 식별, 표현을 통한 조작, 자기 설명 메시지, hypermedia로 이끄는 애플리케이션 상태 전이를 포함한다. client-server, stateless, cache, layered system 제약도 실제 동작에서 확인해야 하고 code-on-demand는 선택적이다. 제시되지 않은 동작을 추정해 REST 충족 또는 불충족을 확정하지 않는다.
  - 예상 개념 판정: API_01_01: CORRECT, API_01_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-API-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 표현 형식이 아키텍처의 증거를 대신하지 않는다. 통일된 인터페이스의 네 조건과 상태 없는 상호 작용·계층·캐시·클라이언트 서버 분리를 봐야 한다. 코드 전송은 선택 사항이다. 현재 정보만으로 전체 REST 여부를 확정하지 않는다.
  - 예상 개념 판정: API_01_01: CORRECT, API_01_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-API-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: Uniform interface에는 자원 식별·표현 조작·자기 설명·hypermedia가 있어 JSON·CRUD만으로 부족하다. client-server·stateless·cache·layered system도 확인해야 한다.
  - 예상 개념 판정: API_01_01: CORRECT, API_01_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-API-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: JSON·명사 URI·CRUD면 uniform interface가 전부 충족된다. 다른 제약 확인은 불필요하고 code-on-demand는 필수이며 JSON이 없으면 REST가 아니다.
  - 예상 개념 판정: API_01_01: INCORRECT, API_01_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-API-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: URI·CRUD·JSON은 REST 전체 충족의 충분한 증거가 아니다. Uniform interface는 자원 식별, 표현을 통한 조작, 자기 설명 메시지, hypermedia로 이끄는 애플리케이션 상태 전이를 포함한다. client-server, stateless, cache, layered system 제약도 실제 동작에서 확인해야 하고 code-on-demand는 선택적이다. 제시되지 않은 동작을 추정해 REST 충족 또는 불충족을 확정하지 않는다.
  - 예상 개념 판정: API_01_01: NEEDS_REVIEW, API_01_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** REST 용어 확인 문항이지 HATEOAS 없는 모든 실제 서비스를 나쁜 설계로 판정하는 문항이 아님.

**출처 대조 위치**

- [
Fielding Dissertation: CHAPTER 5: Representational State Transfer (REST)
](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm) — §5.1.1–§5.1.6, 특히 §5.1.5 Uniform Interface

근거 문서: `A1-API-DOC`. 검수 상태: **PENDING**

### A1-API-02 · INTERMEDIATE

주문은 배송 전 상태에서만 취소 가능하다. POST /orders/42/cancellations와 POST /orders/42/cancel 중 어느 하나만 유효한 설계인가? 클라이언트가 status=CANCELLED를 보내 배송 완료도 취소할 수 있게 한 API의 문제와, 성공·거부 결과 계약을 설명하라.

**모범 답안**

취소 기록을 자원으로 생성하거나 명시적인 업무 command로 표현하는 두 접근 모두 요구조건에 따라 가능하다. URI에 동사가 있다는 사실만으로 모든 설계 판단을 끝내지 않는다. 서버는 실제 주문 상태와 권한을 확인하고 허용된 취소 전이만 수행해야 한다. 클라이언트의 status 문자열은 업무 규칙을 우회할 권한이 아니다. 이 서비스에서 배송 완료의 취소 요구는 충돌로 거부하고 일관된 오류 계약을 정할 수 있으며, 성공 결과·재요청 처리도 정의한다.

**필수 개념**

- 업무 동작의 외부 계약 · 가중치 0.50: 취소 자원 생성 또는 명시 command를 요구에 따라 허용. URI 모양 하나보다 동작·성공·거부 계약을 평가.
- 상태 전이 조건의 서버 검증 · 가중치 0.50: 서버의 실제 상태·권한에 따라 배송 전 취소만 허용. 입력 status로 규칙 우회 금지, 배송 완료 거부·오류 계약 제시.

**개념별 판정 경계**

- `API_02_01`
  - CORRECT: 취소 자원 생성 또는 명시 command를 요구에 따라 허용. URI 모양 하나보다 동작·성공·거부 계약을 평가.
  - PARTIALLY_CORRECT: 두 URI를 허용하나 동작 계약 이유 누락.
  - INCORRECT: 동사 URI라는 이유만으로 어떤 command API도 무효 또는 어느 URI든 계약이 불필요하다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `API_02_02`
  - CORRECT: 서버의 실제 상태·권한에 따라 배송 전 취소만 허용. 입력 status로 규칙 우회 금지, 배송 완료 거부·오류 계약 제시.
  - PARTIALLY_CORRECT: 상태 검증·거부는 제시하나 클라이언트 입력의 권한 한계·오류 계약 일부 누락.
  - INCORRECT: 클라이언트 status만 믿어 배송 완료를 취소하거나 입력 문자열이 업무 권한을 부여한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-API-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 취소 기록을 자원으로 생성하거나 명시적인 업무 command로 표현하는 두 접근 모두 요구조건에 따라 가능하다. URI에 동사가 있다는 사실만으로 모든 설계 판단을 끝내지 않는다. 서버는 실제 주문 상태와 권한을 확인하고 허용된 취소 전이만 수행해야 한다. 클라이언트의 status 문자열은 업무 규칙을 우회할 권한이 아니다. 이 서비스에서 배송 완료의 취소 요구는 충돌로 거부하고 일관된 오류 계약을 정할 수 있으며, 성공 결과·재요청 처리도 정의한다.
  - 예상 개념 판정: API_02_01: CORRECT, API_02_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-API-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 취소를 기록의 생성으로 볼지 업무 명령으로 볼지에 따라 표현을 고를 수 있다. 어떤 경로든 서버가 허용 전이를 판단하고 클라이언트 status 대입으로 우회하지 못하게 해야 한다. 배송 완료 충돌의 응답과 성공·반복 요청 결과도 계약으로 정한다.
  - 예상 개념 판정: API_02_01: CORRECT, API_02_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-API-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 취소 자원을 만들거나 command로 표현하는 두 URI 모두 가능하며 동작·성공·거부 계약이 중요하다. 서버가 실제 상태·권한을 보고 배송 완료 취소를 거부해야 한다.
  - 예상 개념 판정: API_02_01: CORRECT, API_02_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-API-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 동사 URI는 무조건 무효이고 자원 URI만 있으면 동작 계약은 필요 없다. 클라이언트 status가 권한을 부여하므로 배송 완료도 그대로 취소한다.
  - 예상 개념 판정: API_02_01: INCORRECT, API_02_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-API-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 취소 기록을 자원으로 생성하거나 명시적인 업무 command로 표현하는 두 접근 모두 요구조건에 따라 가능하다. URI에 동사가 있다는 사실만으로 모든 설계 판단을 끝내지 않는다. 서버는 실제 주문 상태와 권한을 확인하고 허용된 취소 전이만 수행해야 한다. 클라이언트의 status 문자열은 업무 규칙을 우회할 권한이 아니다. 이 서비스에서 배송 완료의 취소 요구는 충돌로 거부하고 일관된 오류 계약을 정할 수 있으며, 성공 결과·재요청 처리도 정의한다.
  - 예상 개념 판정: API_02_01: NEEDS_REVIEW, API_02_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 409는 이 사례의 허용 설계 선택. 모든 취소 거부가 반드시409여야 한다는 표준으로 주장하지 않음.

**출처 대조 위치**

- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §9.3.3 POST의 대상별 의미, §15.5.10 409
- [
Fielding Dissertation: CHAPTER 5: Representational State Transfer (REST)
](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm) — §5.1.5 Uniform Interface; URI 형태만으로 모든 판단을 정하는 규칙 아님

근거 문서: `A1-API-DOC`. 검수 상태: **PENDING**

### A1-API-03 · ADVANCED

자체 주문 API 계약을 다음처럼 정했다: 보존 기간 내 같은 요청자·작업의 같은 멱등 키와 같은 내용은 업무 효과를 한 번 만들고 기록된 결과로 연결한다. 같은 키에 다른 내용은 충돌로 거부한다. 요청 두 개가 동시에 들어와도 이 계약을 지키려면 어떤 상태를 저장·검사해야 하는가? 기간 만료·외부 결제까지 자동 보장되는지 설명하라.

**모범 답안**

요청자·작업을 포함한 키 범위, 정규화한 요청 내용의 fingerprint, 진행·완료 상태, 기록 결과를 내구성 있게 관리한다. 같은 키의 동시 최초 처리를 유일성·원자 상태 변경 등으로 조정하고 다른 내용은 비교 후 거부하며, 진행 중 요청의 대기·응답 정책도 정한다. 헤더만 받거나 처리 후 키를 따로 저장하면 중복 효과 경쟁이 남는다. 키 만료 후 재전송의 의미와 외부 결제 중복 방어는 별도 제공자·보존 계약이 필요하다. 이 문항은 자체 서비스 계약이지 모든 POST의 기본 HTTP 보장이 아니다.

**필수 개념**

- 요청 식별·내용·결과의 연결 · 가중치 0.50: 키 범위·내용 fingerprint·진행/완료 상태·내구 결과 연결. 같은 키의 다른 내용 거부와 동시 최초 처리의 원자 조정 설명.
- 멱등 계약의 보존·효과 범위 · 가중치 0.50: 진행 중 정책·보존 기간을 명시하고 만료 후 의미·외부 결제는 별도 계약. POST 표준 자체의 자동 보장과 구분.

**개념별 판정 경계**

- `API_03_01`
  - CORRECT: 키 범위·내용 fingerprint·진행/완료 상태·내구 결과 연결. 같은 키의 다른 내용 거부와 동시 최초 처리의 원자 조정 설명.
  - PARTIALLY_CORRECT: 키와 결과만 설명하고 내용 비교·경쟁 조정 일부 누락.
  - INCORRECT: 키 헤더만 받으면 내용 비교나 동시 조정 없이 업무 효과가 한 번으로 보장된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `API_03_02`
  - CORRECT: 진행 중 정책·보존 기간을 명시하고 만료 후 의미·외부 결제는 별도 계약. POST 표준 자체의 자동 보장과 구분.
  - PARTIALLY_CORRECT: 진행 중·보존 기간을 언급하나 만료·외부 제공자 범위 한계 누락.
  - INCORRECT: 키가 있으면 기간이 지나도 모든 외부 결제가 자동 한 번이며 모든 POST의 기본 보장이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-API-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 요청자·작업을 포함한 키 범위, 정규화한 요청 내용의 fingerprint, 진행·완료 상태, 기록 결과를 내구성 있게 관리한다. 같은 키의 동시 최초 처리를 유일성·원자 상태 변경 등으로 조정하고 다른 내용은 비교 후 거부하며, 진행 중 요청의 대기·응답 정책도 정한다. 헤더만 받거나 처리 후 키를 따로 저장하면 중복 효과 경쟁이 남는다. 키 만료 후 재전송의 의미와 외부 결제 중복 방어는 별도 제공자·보존 계약이 필요하다. 이 문항은 자체 서비스 계약이지 모든 POST의 기본 HTTP 보장이 아니다.
  - 예상 개념 판정: API_03_01: CORRECT, API_03_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-API-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 요청 식별자에 범위와 내용을 묶고 최초 진행 권한을 원자적으로 확보한 뒤 완료 결과에 연결한다. 내용 충돌과 진행 중 응답도 정의한다. 보존 기간 밖의 요청·외부 제공자의 효과는 새 조건이므로 별도 확인해야 하며 이 보장은 서비스가 구현할 계약이다.
  - 예상 개념 판정: API_03_01: CORRECT, API_03_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-API-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 요청자·작업별 키와 fingerprint, 진행/완료 상태·결과를 저장하고 유일성과 원자 처리로 동시 최초 효과를 조정한다. 내용이 다른 반복은 거부한다. 진행 중 응답과 키 보존 기간도 정한다.
  - 예상 개념 판정: API_03_01: CORRECT, API_03_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-API-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 키 헤더만 받으면 fingerprint·결과 저장·경쟁 조정은 필요 없다. 만료 후나 외부 결제도 자동 한 번이고 이것은 모든 POST의 기본 HTTP 보장이다.
  - 예상 개념 판정: API_03_01: INCORRECT, API_03_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-API-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 요청자·작업을 포함한 키 범위, 정규화한 요청 내용의 fingerprint, 진행·완료 상태, 기록 결과를 내구성 있게 관리한다. 같은 키의 동시 최초 처리를 유일성·원자 상태 변경 등으로 조정하고 다른 내용은 비교 후 거부하며, 진행 중 요청의 대기·응답 정책도 정한다. 헤더만 받거나 처리 후 키를 따로 저장하면 중복 효과 경쟁이 남는다. 키 만료 후 재전송의 의미와 외부 결제 중복 방어는 별도 제공자·보존 계약이 필요하다. 이 문항은 자체 서비스 계약이지 모든 POST의 기본 HTTP 보장이 아니다.
  - 예상 개념 판정: API_03_01: NEEDS_REVIEW, API_03_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 서버 저장·경쟁 설계는 원문 표준의 특정 구현 규칙이 아니라 문제에서 정의한 계약의 허용 대안. 사람 설계 검수 대상.

**출처 대조 위치**

- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §9.2.2 멱등성·비멱등 요청 재시도 조건
- [OpenAPI Specification v3.1.0](https://spec.openapis.org/oas/v3.1.0.html) — §4.8.12 Parameter Object: 헤더 기술과 실제 실행 보장은 구분; 저장 설계는 주어진 자체 계약에 대한 제안

근거 문서: `A1-API-DOC`. 검수 상태: **PENDING**

### A1-API-04 · INTERMEDIATE

목록 API의 cursor가 created_at·id 내림차순 위치와 필터 status=PAID에 연결돼 있다. 다음 요청에서 필터를 CANCELLED로 바꾸고 같은 cursor를 보냈다. API는 이를 어떻게 계약해야 하는가? cursor 인코딩이 권한 검사와 모든 조회 성능을 보장하는지도 설명하라.

**모범 답안**

cursor가 전제한 정렬·고유 보조 키·필터와 새 요청이 일치하는지 확인해야 한다. 필터가 바뀌면 거부하거나 새 조회로 초기화한다는 정책을 명시하여 다른 집합에 이전 위치를 조용히 적용하지 않는다. opaque 인코딩은 내용을 숨기는 표현 방법이며 서버의 인증·인가·소유권 검사를 대신하지 않는다. cursor만으로 동시 변경 안정성이나 모든 쿼리의 빠른 성능이 보장되지 않으며 유효기간·위치 해석·데이터 변경 정책을 정한다.

**필수 개념**

- 페이지 위치의 계약 전제 · 가중치 0.50: 정렬·고유 보조 키·필터가 cursor 위치 의미의 전제. 필터 변경 시 거부/초기화 정책으로 다른 집합에 조용히 적용하지 않음.
- cursor의 보장 한계 · 가중치 0.50: opaque 표현은 권한 검사를 대체하지 않음. 성능·동시 변경·유효기간 정책의 별도 조건 설명.

**개념별 판정 경계**

- `API_04_01`
  - CORRECT: 정렬·고유 보조 키·필터가 cursor 위치 의미의 전제. 필터 변경 시 거부/초기화 정책으로 다른 집합에 조용히 적용하지 않음.
  - PARTIALLY_CORRECT: 변경 시 초기화 방향만 설명하고 위치 전제 누락.
  - INCORRECT: 다른 필터에 이전 위치를 무조건 적용해도 동일 계약이라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `API_04_02`
  - CORRECT: opaque 표현은 권한 검사를 대체하지 않음. 성능·동시 변경·유효기간 정책의 별도 조건 설명.
  - PARTIALLY_CORRECT: 권한 검사 별도는 설명하나 성능·변경·유효기간 한계 일부 누락.
  - INCORRECT: 인코딩만으로 인증·인가와 모든 조회의 빠른 속도·변경 안정성이 보장된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-API-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: cursor가 전제한 정렬·고유 보조 키·필터와 새 요청이 일치하는지 확인해야 한다. 필터가 바뀌면 거부하거나 새 조회로 초기화한다는 정책을 명시하여 다른 집합에 이전 위치를 조용히 적용하지 않는다. opaque 인코딩은 내용을 숨기는 표현 방법이며 서버의 인증·인가·소유권 검사를 대신하지 않는다. cursor만으로 동시 변경 안정성이나 모든 쿼리의 빠른 성능이 보장되지 않으며 유효기간·위치 해석·데이터 변경 정책을 정한다.
  - 예상 개념 판정: API_04_01: CORRECT, API_04_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-API-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 이 cursor가 어느 집합의 어느 순서 위치인지 계약해야 한다. PAID 위치를 CANCELLED에 쓰는 요청은 정책상 거부 또는 재시작한다. 토큰이 불투명해도 권한은 따로 확인하며 수명·변경 중 의미·쿼리 비용도 별도 검증한다.
  - 예상 개념 판정: API_04_01: CORRECT, API_04_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-API-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: cursor는 정렬·id·필터에 묶인 위치라 필터 변경은 거부하거나 초기화해야 한다. 인코딩과 별도로 서버 권한 검사를 수행한다.
  - 예상 개념 판정: API_04_01: CORRECT, API_04_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-API-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 필터가 달라도 같은 위치를 조용히 적용하면 된다. cursor 인코딩이 인증·인가·모든 조회 속도와 동시 변경 안정성을 보장한다.
  - 예상 개념 판정: API_04_01: INCORRECT, API_04_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-API-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: cursor가 전제한 정렬·고유 보조 키·필터와 새 요청이 일치하는지 확인해야 한다. 필터가 바뀌면 거부하거나 새 조회로 초기화한다는 정책을 명시하여 다른 집합에 이전 위치를 조용히 적용하지 않는다. opaque 인코딩은 내용을 숨기는 표현 방법이며 서버의 인증·인가·소유권 검사를 대신하지 않는다. cursor만으로 동시 변경 안정성이나 모든 쿼리의 빠른 성능이 보장되지 않으며 유효기간·위치 해석·데이터 변경 정책을 정한다.
  - 예상 개념 판정: API_04_01: NEEDS_REVIEW, API_04_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** cursor 안정성과 SQL 인덱스의 효율은 별도 책임. 이 사례의 정책을 모든 API의 표준으로 강제하지 않음.

**출처 대조 위치**

- [OpenAPI Specification v3.1.0](https://spec.openapis.org/oas/v3.1.0.html) — §4.8.12 Parameter Object: query 인자·스키마·설명; cursor 정책은 자체 API 설계
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — RFC 9110 §4.1 URI의 자원 식별

근거 문서: `A1-API-DOC`. 검수 상태: **PENDING**

### A1-API-05 · ADVANCED

OpenAPI 3.1.0 응답 스키마는 필수 memberId를 integer로 선언한다. 실제 HTTP 응답은 이를 생략하거나 문자열로 보낸다. 명세 파일과 Service 단위 테스트만 있으면 계약이 검증된 것인가? 기존 소비자가 memberId를 쓰는 상태에서 userId로 바꾸는 이관도 설명하라.

**모범 답안**

OpenAPI는 약속을 기술하며 파일 자체가 실제 응답을 자동 검증하지 않는다. 필요한 실제 HTTP 응답의 필수 필드·타입·상태 코드를 스키마와 대조하는 계약 검증이 있어야 한다. Service 단위 테스트만으로 직렬화·HTTP 계약은 증명되지 않는다. 기존 memberId 소비자가 있다면 일방 삭제·이름 변경은 깨뜨릴 수 있어 구·신 필드를 함께 지원하고 이관·폐기 시점을 정하거나 호환성 없는 변경의 새 버전 계약을 선택한다. 실제 소비자와 배포 상태를 확인한 뒤 제거한다.

**필수 개념**

- 명세와 runtime의 일치 · 가중치 0.50: OpenAPI 기술과 실제 응답 검증을 구분. HTTP 필수 필드·타입·직렬화를 확인하며 Service green만으로 대체하지 않음.
- 소비자 계약의 점진 이관 · 가중치 0.50: memberId 소비자가 남은 동안 일방 제거의 호환 위험 설명. 병행 필드·이관/폐기 또는 새 버전의 계약과 실제 소비자 상태 확인.

**개념별 판정 경계**

- `API_05_01`
  - CORRECT: OpenAPI 기술과 실제 응답 검증을 구분. HTTP 필수 필드·타입·직렬화를 확인하며 Service green만으로 대체하지 않음.
  - PARTIALLY_CORRECT: 실제 응답 검증 필요성만 언급하고 HTTP 경계 일부 누락.
  - INCORRECT: OpenAPI 파일 또는 Service 테스트가 응답을 자동 보정해 항상 계약 일치라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `API_05_02`
  - CORRECT: memberId 소비자가 남은 동안 일방 제거의 호환 위험 설명. 병행 필드·이관/폐기 또는 새 버전의 계약과 실제 소비자 상태 확인.
  - PARTIALLY_CORRECT: 병행 지원 또는 새 버전은 제시하지만 실제 소비자·폐기 조건 누락.
  - INCORRECT: memberId를 예고 없이 지워도 모든 기존 소비자는 자동 userId로 전환된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-API-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: OpenAPI는 약속을 기술하며 파일 자체가 실제 응답을 자동 검증하지 않는다. 필요한 실제 HTTP 응답의 필수 필드·타입·상태 코드를 스키마와 대조하는 계약 검증이 있어야 한다. Service 단위 테스트만으로 직렬화·HTTP 계약은 증명되지 않는다. 기존 memberId 소비자가 있다면 일방 삭제·이름 변경은 깨뜨릴 수 있어 구·신 필드를 함께 지원하고 이관·폐기 시점을 정하거나 호환성 없는 변경의 새 버전 계약을 선택한다. 실제 소비자와 배포 상태를 확인한 뒤 제거한다.
  - 예상 개념 판정: API_05_01: CORRECT, API_05_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-API-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 약속한 integer 필수 필드를 실제 응답이 지키는지 HTTP 경계에서 확인해야 한다. 이름 변경은 기존 소비자의 약속을 바꾸므로 함께 지원하며 소비자 이관 뒤 폐기하거나 별도 버전을 택한다. 제거 시점은 실제 소비자·배포 상태로 판단한다.
  - 예상 개념 판정: API_05_01: CORRECT, API_05_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-API-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 명세만으로 실제 응답이 보정되지는 않는다. HTTP 필수 필드·타입·직렬화 결과를 계약과 대조해야 하고 Service green은 그 증거가 아니다. memberId와 userId를 병행 지원해 이관할 수 있다.
  - 예상 개념 판정: API_05_01: CORRECT, API_05_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-API-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 명세 파일과 Service 테스트가 실제 HTTP 응답을 자동 보정한다. memberId를 예고 없이 지워도 모든 소비자는 자동으로 userId를 사용한다.
  - 예상 개념 판정: API_05_01: INCORRECT, API_05_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-API-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: OpenAPI는 약속을 기술하며 파일 자체가 실제 응답을 자동 검증하지 않는다. 필요한 실제 HTTP 응답의 필수 필드·타입·상태 코드를 스키마와 대조하는 계약 검증이 있어야 한다. Service 단위 테스트만으로 직렬화·HTTP 계약은 증명되지 않는다. 기존 memberId 소비자가 있다면 일방 삭제·이름 변경은 깨뜨릴 수 있어 구·신 필드를 함께 지원하고 이관·폐기 시점을 정하거나 호환성 없는 변경의 새 버전 계약을 선택한다. 실제 소비자와 배포 상태를 확인한 뒤 제거한다.
  - 예상 개념 판정: API_05_01: NEEDS_REVIEW, API_05_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** OpenAPI 도구 생태계에서 별도 validation을 연결할 수 있다는 점과 명세 자체의 자동 실행을 구분.

**출처 대조 위치**

- [OpenAPI Specification v3.1.0](https://spec.openapis.org/oas/v3.1.0.html) — §4.8.16 Responses Object, §4.8.17 Response Object, §4.8.24 Schema Object
- [Testing Spring Boot Applications :: Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html) — Testing Spring Boot Applications: 실제 서버·HTTP와 제한 테스트 경계

근거 문서: `A1-API-DOC`. 검수 상태: **PENDING**

### A1-TEST-01 · BASIC

주문 수량의 불변식, Service commit 후 저장 결과, HTTP 입력 validation·JSON 이름, 로그인부터 답변 제출까지의 연결을 검증하려 한다. 각각 어느 수준의 테스트가 적절한가? HTTP 계약 테스트의 Service double이 전체 DB 흐름을 증명하는지도 설명하라.

**모범 답안**

순수 수량 규칙은 Spring 없는 도메인 단위 테스트로 정상·경계를 확인한다. commit 후 저장은 실제 Repository/DB의 Service 통합 테스트에서 재조회한다. HTTP binding·validation·상태·직렬화는 Service double을 둔 Controller/API 계약 테스트로 좁게 확인할 수 있다. 로그인부터 제출의 핵심 연결은 실제 연결 전체 통합·E2E로 확인한다. 각 수준은 실행한 경계만 증명하며 Controller의 Service double은 실제 DB 저장을 증명하지 않는다. 이 프로젝트의 실제 DB Service 규칙은 프로젝트 지침이다.

**필수 개념**

- 요구 실패에 맞는 검증 수준 · 가중치 0.50: 도메인·Service 저장·HTTP 계약·핵심 연결별 최소 충분한 수준 선택과 기대 assertion 설명.
- 실행 경계와 주장 범위 · 가중치 0.50: 각 테스트의 실제/double 경계를 명시. Controller의 Service double은 실제 DB 저장·전체 연결을 증명하지 않음.

**개념별 판정 경계**

- `TEST_01_01`
  - CORRECT: 도메인·Service 저장·HTTP 계약·핵심 연결별 최소 충분한 수준 선택과 기대 assertion 설명.
  - PARTIALLY_CORRECT: 수준만 나열하고 검증하려는 실패 연결 일부 누락.
  - INCORRECT: 수량 규칙은 모두 브라우저로만 검증하거나 HTTP 직렬화를 순수 Service 테스트가 증명한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TEST_01_02`
  - CORRECT: 각 테스트의 실제/double 경계를 명시. Controller의 Service double은 실제 DB 저장·전체 연결을 증명하지 않음.
  - PARTIALLY_CORRECT: double이 DB를 증명 못한다고 답하나 다른 수준의 실행 범위 설명 누락.
  - INCORRECT: Controller의 Service double만으로 실제 DB와 로그인 제출 전체 연결도 증명된다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TEST-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 순수 수량 규칙은 Spring 없는 도메인 단위 테스트로 정상·경계를 확인한다. commit 후 저장은 실제 Repository/DB의 Service 통합 테스트에서 재조회한다. HTTP binding·validation·상태·직렬화는 Service double을 둔 Controller/API 계약 테스트로 좁게 확인할 수 있다. 로그인부터 제출의 핵심 연결은 실제 연결 전체 통합·E2E로 확인한다. 각 수준은 실행한 경계만 증명하며 Controller의 Service double은 실제 DB 저장을 증명하지 않는다. 이 프로젝트의 실제 DB Service 규칙은 프로젝트 지침이다.
  - 예상 개념 판정: TEST_01_01: CORRECT, TEST_01_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TEST-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 실패가 생기는 경계를 직접 실행한다. 도메인은 작은 규칙 테스트, Service 저장은 commit 뒤 실제 DB 재조회, HTTP는 요청/응답 계약, 핵심 연결은 전체 통합을 둔다. 대체한 협력자의 내부까지 검증했다고 주장하지 않으며 각 결과의 실제 실행 범위를 기록한다.
  - 예상 개념 판정: TEST_01_01: CORRECT, TEST_01_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TEST-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 수량은 순수 도메인 경계 테스트, commit 뒤 저장은 실제 DB 재조회, HTTP validation과 JSON은 Controller 계약 테스트, 로그인 제출은 실제 전체 연결로 확인한다. Service double은 DB 저장을 증명하지 못한다.
  - 예상 개념 판정: TEST_01_01: CORRECT, TEST_01_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TEST-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 순수 수량은 브라우저로만 확인해야 하고 Service 단위 테스트가 HTTP 직렬화를 증명한다. Controller의 Service double이 실제 DB와 로그인 제출 전체 흐름도 증명한다.
  - 예상 개념 판정: TEST_01_01: INCORRECT, TEST_01_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TEST-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 순수 수량 규칙은 Spring 없는 도메인 단위 테스트로 정상·경계를 확인한다. commit 후 저장은 실제 Repository/DB의 Service 통합 테스트에서 재조회한다. HTTP binding·validation·상태·직렬화는 Service double을 둔 Controller/API 계약 테스트로 좁게 확인할 수 있다. 로그인부터 제출의 핵심 연결은 실제 연결 전체 통합·E2E로 확인한다. 각 수준은 실행한 경계만 증명하며 Controller의 Service double은 실제 DB 저장을 증명하지 않는다. 이 프로젝트의 실제 DB Service 규칙은 프로젝트 지침이다.
  - 예상 개념 판정: TEST_01_01: NEEDS_REVIEW, TEST_01_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 모든 테스트를 E2E로 키우거나 빠른 테스트의 green을 모든 연결 증거로 확대하지 않음.

**출처 대조 위치**

- [Unit Testing :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/unit.html) — Unit Testing: POJO·Controller의 독립 검증
- [Testing Spring Boot Applications :: Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html) — Testing Spring Boot Applications: mock·실제 서버·test slices

근거 문서: `A1-TEST-DOC`. 검수 상태: **PENDING**

### A1-TEST-02 · INTERMEDIATE

Repository를 mock한 Service 테스트가 모두 green이다. 운영에서는 FK 위반과 잘못된 JOIN query가 발견됐다. 기존 테스트는 무엇을 증명했고 무엇을 못 했는가? 필요한 보강을 설명하되 mock이 항상 쓸모없다는 결론을 피하라.

**모범 답안**

mock이 반환하도록 정한 값에 대한 Service의 분기·협력 동작은 확인했을 수 있지만 실제 SQL·FK·JPA mapping·transaction commit은 실행하지 않았다. 따라서 실제 DB 계약의 보장이 아니다. DB와 버전을 명시한 통합 테스트에서 FK 경계·실제 query 결과·commit 뒤 재조회를 확인해야 한다. 빠른 호출자 테스트의 목적은 유지할 수 있으나 실제 도메인 규칙을 mock으로 대체해 성공시킨 결과는 그 규칙의 증거가 아니다. 프로젝트는 Service 저장 검증에 실제 DB를 요구한다.

**필수 개념**

- 테스트 double의 관찰 범위 · 가중치 0.50: mock 응답에 대한 호출자 분기와 실제 SQL·FK·mapping·commit의 미실행을 구분. mock이 항상 무용하다는 단정 금지.
- 저장 계약의 실제 실행 · 가중치 0.50: 실제 DB·버전에서 FK 경계·query 결과·commit 뒤 재조회 보강. 실제 규칙을 double로 대체한 증거의 한계 설명.

**개념별 판정 경계**

- `TEST_02_01`
  - CORRECT: mock 응답에 대한 호출자 분기와 실제 SQL·FK·mapping·commit의 미실행을 구분. mock이 항상 무용하다는 단정 금지.
  - PARTIALLY_CORRECT: DB를 실행하지 않았다고만 설명하고 호출자 검증 목적 누락.
  - INCORRECT: Repository mock이 실제 DB SQL·FK·commit도 실행해 보장한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TEST_02_02`
  - CORRECT: 실제 DB·버전에서 FK 경계·query 결과·commit 뒤 재조회 보강. 실제 규칙을 double로 대체한 증거의 한계 설명.
  - PARTIALLY_CORRECT: 실제 DB 보강 방향은 제시하나 버전·commit 뒤 확인·규칙 대체 한계 일부 누락.
  - INCORRECT: mock 반환값을 늘리기만 하면 실제 저장 제약과 query까지 인증되거나 모든 mock을 무조건 없애야 한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TEST-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: mock이 반환하도록 정한 값에 대한 Service의 분기·협력 동작은 확인했을 수 있지만 실제 SQL·FK·JPA mapping·transaction commit은 실행하지 않았다. 따라서 실제 DB 계약의 보장이 아니다. DB와 버전을 명시한 통합 테스트에서 FK 경계·실제 query 결과·commit 뒤 재조회를 확인해야 한다. 빠른 호출자 테스트의 목적은 유지할 수 있으나 실제 도메인 규칙을 mock으로 대체해 성공시킨 결과는 그 규칙의 증거가 아니다. 프로젝트는 Service 저장 검증에 실제 DB를 요구한다.
  - 예상 개념 판정: TEST_02_01: CORRECT, TEST_02_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TEST-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 통제된 Repository의 답을 처리하는 능력과 실제 저장소 계약은 다르다. 필요한 빠른 테스트는 남기되 DB 버전·FK·query를 실제 통합에서 실행하고 transaction 종료 뒤 재조회한다. 도메인 규칙 자체를 mock한 성공도 규칙의 증거로 쓰지 않는다.
  - 예상 개념 판정: TEST_02_01: CORRECT, TEST_02_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TEST-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: mock 반환에 대한 Service 분기는 확인했지만 SQL·FK·mapping·commit은 미실행이라 DB 증거가 아니다. 호출자 테스트 목적은 있다. 실제 DB에서 제약과 query를 통합 검증한다.
  - 예상 개념 판정: TEST_02_01: CORRECT, TEST_02_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TEST-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: Repository mock도 실제 SQL·FK·commit을 실행해 보장한다. 반환값만 늘리면 저장 계약이 인증되거나 모든 mock을 무조건 없애야 한다.
  - 예상 개념 판정: TEST_02_01: INCORRECT, TEST_02_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TEST-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: mock이 반환하도록 정한 값에 대한 Service의 분기·협력 동작은 확인했을 수 있지만 실제 SQL·FK·JPA mapping·transaction commit은 실행하지 않았다. 따라서 실제 DB 계약의 보장이 아니다. DB와 버전을 명시한 통합 테스트에서 FK 경계·실제 query 결과·commit 뒤 재조회를 확인해야 한다. 빠른 호출자 테스트의 목적은 유지할 수 있으나 실제 도메인 규칙을 mock으로 대체해 성공시킨 결과는 그 규칙의 증거가 아니다. 프로젝트는 Service 저장 검증에 실제 DB를 요구한다.
  - 예상 개념 판정: TEST_02_01: NEEDS_REVIEW, TEST_02_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 이 프로젝트의 ServiceTest mock 금지와 테스트 double 일반론을 구분.

**출처 대조 위치**

- [Unit Testing :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/unit.html) — Unit Testing: standalone unit과 test double 경계
- [Testing Spring Boot Applications :: Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html) — Testing Spring Boot Applications: @DataJpaTest·실제 서버와 slices의 범위

근거 문서: `A1-TEST-DOC`. 검수 상태: **PENDING**

### A1-TEST-03 · INTERMEDIATE

@SpringBootTest(webEnvironment=RANDOM_PORT)의 HTTP 요청으로 서버가 데이터를 commit했다. 테스트 메서드의 test-level @Transactional rollback이 서버 데이터까지 지우는가? 실패 중 부분 준비와 공유 DB 병렬 실행도 고려해 정리 책임을 설명하라.

**모범 답안**

HTTP 클라이언트 테스트와 실제 서버는 다른 스레드·transaction이므로 테스트 rollback이 서버 commit을 자동 되돌리지 않는다. 데이터를 만든 테스트가 생성 범위를 알고 @AfterEach에서 자식→부모 순서로 정리하며 assertion 실패·부분 준비 뒤에도 동작해야 한다. 공유 DB 병렬 실행에서 전역 deleteAll을 쓰면 다른 테스트 데이터를 지울 수 있어 실행별 데이터·schema 격리 또는 직렬화가 필요하다. Spring test transaction의 스레드 경계와 이 프로젝트의 Service 테스트 정리 규칙을 구분한다.

**필수 개념**

- 테스트와 서버 transaction 경계 · 가중치 0.50: RANDOM_PORT의 실제 HTTP 서버·클라이언트는 별도 스레드/transaction. test rollback이 서버 commit을 되돌리지 않음.
- 데이터 생성자의 정리 책임 · 가중치 0.50: 데이터 생성 테스트가 실패·부분 준비 후에도 자식→부모 정리. 공유 병렬 DB에서 전역 삭제 대신 격리/직렬화 선택.

**개념별 판정 경계**

- `TEST_03_01`
  - CORRECT: RANDOM_PORT의 실제 HTTP 서버·클라이언트는 별도 스레드/transaction. test rollback이 서버 commit을 되돌리지 않음.
  - PARTIALLY_CORRECT: rollback되지 않는 결론만 설명하고 스레드·transaction 이유 누락.
  - INCORRECT: test-level transaction이 실제 서버 commit까지 항상 자동 rollback한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TEST_03_02`
  - CORRECT: 데이터 생성 테스트가 실패·부분 준비 후에도 자식→부모 정리. 공유 병렬 DB에서 전역 삭제 대신 격리/직렬화 선택.
  - PARTIALLY_CORRECT: 생성자 teardown과 FK 순서는 제시하나 병렬 공유 전역 삭제 위험 누락.
  - INCORRECT: 이전 테스트가 BeforeEach 전역 삭제로 정리하면 병렬 실행까지 안전하고 FK 순서는 무관하다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TEST-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: HTTP 클라이언트 테스트와 실제 서버는 다른 스레드·transaction이므로 테스트 rollback이 서버 commit을 자동 되돌리지 않는다. 데이터를 만든 테스트가 생성 범위를 알고 @AfterEach에서 자식→부모 순서로 정리하며 assertion 실패·부분 준비 뒤에도 동작해야 한다. 공유 DB 병렬 실행에서 전역 deleteAll을 쓰면 다른 테스트 데이터를 지울 수 있어 실행별 데이터·schema 격리 또는 직렬화가 필요하다. Spring test transaction의 스레드 경계와 이 프로젝트의 Service 테스트 정리 규칙을 구분한다.
  - 예상 개념 판정: TEST_03_01: CORRECT, TEST_03_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TEST-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 서버 요청의 transaction은 테스트 스레드에 묶인 rollback 밖이다. fixture를 만든 테스트가 부분 생성·assertion 실패를 포함해 FK 역순 정리하고, 공유 병렬 실행에는 자체 데이터·schema 격리나 직렬화를 적용한다.
  - 예상 개념 판정: TEST_03_01: CORRECT, TEST_03_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TEST-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 실제 서버와 테스트는 별도 스레드·transaction이라 test rollback이 서버 commit을 되돌리지 않는다. 데이터 생성 테스트가 실패 뒤에도 AfterEach에서 자식부터 부모를 정리한다.
  - 예상 개념 판정: TEST_03_01: CORRECT, TEST_03_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TEST-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 테스트 transaction이 서버 commit까지 항상 rollback한다. 다음 테스트 BeforeEach의 전역 삭제는 병렬 실행에서도 안전하고 FK 삭제 순서는 무관하다.
  - 예상 개념 판정: TEST_03_01: INCORRECT, TEST_03_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TEST-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: HTTP 클라이언트 테스트와 실제 서버는 다른 스레드·transaction이므로 테스트 rollback이 서버 commit을 자동 되돌리지 않는다. 데이터를 만든 테스트가 생성 범위를 알고 @AfterEach에서 자식→부모 순서로 정리하며 assertion 실패·부분 준비 뒤에도 동작해야 한다. 공유 DB 병렬 실행에서 전역 deleteAll을 쓰면 다른 테스트 데이터를 지울 수 있어 실행별 데이터·schema 격리 또는 직렬화가 필요하다. Spring test transaction의 스레드 경계와 이 프로젝트의 Service 테스트 정리 규칙을 구분한다.
  - 예상 개념 판정: TEST_03_01: NEEDS_REVIEW, TEST_03_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 다른 스레드의 서버 commit을 테스트 rollback의 자동 정리 범위로 계산하지 않음.

**출처 대조 위치**

- [Testing Spring Boot Applications :: Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html) — RANDOM_PORT/DEFINED_PORT: separate threads·separate transactions·server does not roll back
- [Transaction Management :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/testcontext-framework/tx.html) — Transaction Management: test-managed transaction·ThreadLocal·preemptive timeout 경고

근거 문서: `A1-TEST-DOC`. 검수 상태: **PENDING**

### A1-TEST-04 · INTERMEDIATE

비동기 작업 제출 뒤 sleep(1000) 후 성공을 검사하는 테스트가 빠른 머신에서는 통과하고 느린 환경에서는 실패한다. sleep을 무조건 늘리거나 무한 polling으로 바꾸면 충분한가? 완료 조건·제한 시간·assertion을 연결한 방법을 설명하라.

**모범 답안**

시간 경과는 작업 완료의 증거가 아니다. 완료 이벤트·latch 또는 관찰 가능한 상태를 기다리고 정한 deadline 안에 실제 성공 결과를 assertion한다. 완료되지 않거나 다른 terminal 상태면 실패해야 한다. 제한 없는 polling은 영원히 멈출 수 있고 긴 sleep은 느리며 완료를 보장하지 않는다. timeout의 별도 스레드 실행이 test transaction 등 ThreadLocal 경계에 미치는 영향도 필요 시 확인한다.

**필수 개념**

- 관찰 조건에 따른 대기 · 가중치 0.50: 시간 경과와 완료를 분리. 이벤트/latch/상태를 기다린 뒤 실제 terminal 결과 assertion.
- 대기의 실패·시간 경계 · 가중치 0.50: deadline·timeout·미완료/다른 terminal 실패 정의. 무한 polling과 큰 sleep의 한계·timeout 실행 경계 확인.

**개념별 판정 경계**

- `TEST_04_01`
  - CORRECT: 시간 경과와 완료를 분리. 이벤트/latch/상태를 기다린 뒤 실제 terminal 결과 assertion.
  - PARTIALLY_CORRECT: 조건 대기만 제시하고 실제 성공 assertion 누락.
  - INCORRECT: sleep 시간이 지났거나 polling을 시작한 사실 자체가 성공을 보장한다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TEST_04_02`
  - CORRECT: deadline·timeout·미완료/다른 terminal 실패 정의. 무한 polling과 큰 sleep의 한계·timeout 실행 경계 확인.
  - PARTIALLY_CORRECT: 시간 제한은 제시하지만 실패 결과·timeout 스레드 경계 설명 일부 누락.
  - INCORRECT: 무한 polling이 항상 종료되고 timeout은 어떤 스레드 경계에도 영향이 없다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TEST-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 시간 경과는 작업 완료의 증거가 아니다. 완료 이벤트·latch 또는 관찰 가능한 상태를 기다리고 정한 deadline 안에 실제 성공 결과를 assertion한다. 완료되지 않거나 다른 terminal 상태면 실패해야 한다. 제한 없는 polling은 영원히 멈출 수 있고 긴 sleep은 느리며 완료를 보장하지 않는다. timeout의 별도 스레드 실행이 test transaction 등 ThreadLocal 경계에 미치는 영향도 필요 시 확인한다.
  - 예상 개념 판정: TEST_04_01: CORRECT, TEST_04_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TEST-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 느린 환경의 임의 시간 대신 관찰 가능한 완료를 기준으로 동기화한다. latch나 제한 polling 뒤 결과를 검사하고 deadline 안에 끝나지 않거나 실패 상태면 테스트도 실패시킨다. 선점 timeout을 쓰면 실행 스레드·transaction 경계를 확인한다.
  - 예상 개념 판정: TEST_04_01: CORRECT, TEST_04_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TEST-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 완료 상태·이벤트를 기다리고 실제 성공 terminal 결과를 assertion해야 한다. 시간 경과는 성공이 아니다. 정한 deadline으로 대기를 제한한다.
  - 예상 개념 판정: TEST_04_01: CORRECT, TEST_04_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TEST-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: sleep이 지났거나 polling을 시작하면 작업 성공이다. 무한 polling은 항상 종료되고 timeout은 transaction 스레드 경계에도 영향이 없다.
  - 예상 개념 판정: TEST_04_01: INCORRECT, TEST_04_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TEST-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 시간 경과는 작업 완료의 증거가 아니다. 완료 이벤트·latch 또는 관찰 가능한 상태를 기다리고 정한 deadline 안에 실제 성공 결과를 assertion한다. 완료되지 않거나 다른 terminal 상태면 실패해야 한다. 제한 없는 polling은 영원히 멈출 수 있고 긴 sleep은 느리며 완료를 보장하지 않는다. timeout의 별도 스레드 실행이 test transaction 등 ThreadLocal 경계에 미치는 영향도 필요 시 확인한다.
  - 예상 개념 판정: TEST_04_01: NEEDS_REVIEW, TEST_04_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 조건 대기를 assertion 생략이나 무한 대기로 바꾸지 않음. 여기서 JUnit 문서 참조는 의존성 업그레이드가 아님.

**출처 대조 위치**

- [JUnit](https://docs.junit.org/6.1.3/_exports/junit-user-guide-6.1.3.html) — User Guide: Using @Timeout for Polling Tests; Preemptive Timeouts with assertTimeoutPreemptively
- [Transaction Management :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/testcontext-framework/tx.html) — Preemptive timeouts and test-managed transactions

근거 문서: `A1-TEST-DOC`. 검수 상태: **PENDING**

### A1-TEST-05 · ADVANCED

fake 평가 provider가 timeout·429·503을 내도록 설정하고 retry·취소·학습 화면 오류 처리가 모두 통과했다. 이것으로 실제 제공자 장애 대응과 모델 채점 품질까지 검증했다고 발표할 수 있는가? 작성자가 만든 답변 사례의 역할과 추가 증거를 설명하라.

**모범 답안**

통제한 실패 주입은 해당 응답·시점에서 클라이언트 retry·취소·오류 상태와 assertion을 확인한다. 실제 제공자의 프로토콜·지연·인증·장애와 모델 판단은 실행하지 않았다. 따라서 실제 연동 확인과 독립 대표 학습자 답변에 대한 사람 기준·모델 결과 대조가 추가로 필요하다. 작성자 사례는 오개념·누락·근거 부족 경계를 설계하는 개발 진단 자료이며, 같은 사례를 통과했다고 독립 평가 정확도가 되는 것은 아니다. 실행 환경·입력·assertion·미검증 경계를 분리해 보고한다.

**필수 개념**

- 통제한 실패 주입의 역할 · 가중치 0.50: fake timeout·429·503에서 확인한 클라이언트 경로·assertion과 실제 provider 미실행 경계를 명시.
- 품질 표본과 증거의 독립성 · 가중치 0.50: 작성자 진단 사례와 독립 대표 표본·사람 기준·실제 모델 대조를 구분. 실행 환경·입력·assertion 한계 기록.

**개념별 판정 경계**

- `TEST_05_01`
  - CORRECT: fake timeout·429·503에서 확인한 클라이언트 경로·assertion과 실제 provider 미실행 경계를 명시.
  - PARTIALLY_CORRECT: fake 검증이라고만 설명하고 확인한 경로와 미실행 경계 누락.
  - INCORRECT: fake 통과가 실제 제공자의 인증·지연·장애·모델 판단을 모두 실행한 증거라고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.
- `TEST_05_02`
  - CORRECT: 작성자 진단 사례와 독립 대표 표본·사람 기준·실제 모델 대조를 구분. 실행 환경·입력·assertion 한계 기록.
  - PARTIALLY_CORRECT: 실제 모델 추가 확인은 제시하나 독립 표본과 작성자 사례 구분 누락.
  - INCORRECT: 개발에 쓴 작성자 사례 통과만으로 독립 대표 평가 정확도를 인증할 수 있다고 주장.
  - NEEDS_REVIEW: 해당 개념을 뒷받침하는 근거 문서가 제공되지 않았거나 전제·버전이 불명확해 답변과 대조할 수 없는 상태. 명백한 오답과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A1-TEST-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 통제한 실패 주입은 해당 응답·시점에서 클라이언트 retry·취소·오류 상태와 assertion을 확인한다. 실제 제공자의 프로토콜·지연·인증·장애와 모델 판단은 실행하지 않았다. 따라서 실제 연동 확인과 독립 대표 학습자 답변에 대한 사람 기준·모델 결과 대조가 추가로 필요하다. 작성자 사례는 오개념·누락·근거 부족 경계를 설계하는 개발 진단 자료이며, 같은 사례를 통과했다고 독립 평가 정확도가 되는 것은 아니다. 실행 환경·입력·assertion·미검증 경계를 분리해 보고한다.
  - 예상 개념 판정: TEST_05_01: CORRECT, TEST_05_02: CORRECT
  - 이유: 문항에 주어진 전제와 모든 필수 개념의 조건을 충족.
- `A1-TEST-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 통제된 실패는 클라이언트 동작의 재현 가능한 증거다. 실제 연동과 품질은 별개로 실행해야 한다. 작성자 예시는 개발 진단용으로 남기고 독립 학습자 표본·사람 판정·실제 모델 결과를 대조하며 실행한 범위와 빠진 경계를 보고한다.
  - 예상 개념 판정: TEST_05_01: CORRECT, TEST_05_02: CORRECT
  - 이유: 표현 또는 해결 방법이 달라도 필수 조건과 보장 범위가 일치. 특정 단어·해법 일치로만 채점 금지.
- `A1-TEST-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: fake 응답에서 retry·취소·오류 상태 assertion만 확인했고 실제 제공자 프로토콜·인증·장애·모델 판단은 미실행이다. 모델 품질은 실제 연동을 추가 확인해야 한다.
  - 예상 개념 판정: TEST_05_01: CORRECT, TEST_05_02: PARTIALLY_CORRECT
  - 이유: 마지막 필수 개념의 조건·이유 일부가 빠짐. 이미 충족한 앞 개념을 전체 판정에 맞춰 낮추지 않음.
- `A1-TEST-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: fake 통과가 실제 제공자의 인증·지연·장애·모델 판단을 모두 실행했다는 증거다. 작성자 사례 통과만으로 독립 대표 사용자 평가 정확도를 인증할 수 있다.
  - 예상 개념 판정: TEST_05_01: INCORRECT, TEST_05_02: INCORRECT
  - 이유: 각 필수 개념의 핵심을 뒤집거나 문항 전제와 충돌. 단순 생략과 구분.
- `A1-TEST-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 통제한 실패 주입은 해당 응답·시점에서 클라이언트 retry·취소·오류 상태와 assertion을 확인한다. 실제 제공자의 프로토콜·지연·인증·장애와 모델 판단은 실행하지 않았다. 따라서 실제 연동 확인과 독립 대표 학습자 답변에 대한 사람 기준·모델 결과 대조가 추가로 필요하다. 작성자 사례는 오개념·누락·근거 부족 경계를 설계하는 개발 진단 자료이며, 같은 사례를 통과했다고 독립 평가 정확도가 되는 것은 아니다. 실행 환경·입력·assertion·미검증 경계를 분리해 보고한다.
  - 예상 개념 판정: TEST_05_01: NEEDS_REVIEW, TEST_05_02: NEEDS_REVIEW
  - 이유: 답변 내용은 정답 사례와 동일하나 필요한 근거가 전부 누락된 진단 조건. 근거 없이 정답 확정하는 평가기를 탐지하는 작성자 기대값.

**혼동 주의:** 이 묶음의150개 작성자 진단 사례도 실제 모델 실행·독립 품질 평가를 대체하지 않음.

**출처 대조 위치**

- [Unit Testing :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/unit.html) — Unit Testing: test double을 사용한 독립 실행의 범위
- [Testing Spring Boot Applications :: Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html) — Testing Spring Boot Applications: 실제 환경과 제한된 테스트 범위; 대표 품질 표본 설계는 본 사례의 조건

근거 문서: `A1-TEST-DOC`. 검수 상태: **PENDING**

## 검색에 제공할 근거 문서

### A1-DB-DOC

DB 모델·기반 — 전제·원리·실패 조건 · 관계형 모델; PostgreSQL 17의 제약 동작

# 관계를 저장 가능한 상태로 만드는 제약

DB 모델은 행을 구별하는 기준과 행 사이 관계를 명시하는 작업. 기본키는 선택한 행 식별자이고, 업무상 유일한 조합은 별도 UNIQUE로 표현 가능. 대리키를 추가해도 업무 중복 금지 조건이 저절로 생기지 않음.

```text
업무 규칙 → 애플리케이션의 빠른 안내
        └→ DB의 PK·UNIQUE·NOT NULL·FK·CHECK → 모든 쓰기 경로의 최종 방어
```

PostgreSQL 17 CHECK는 식의 결과가 false일 때 거부. NULL로 unknown이 되면 통과하므로 필수값은 NOT NULL로 분리. UNIQUE의 기본 동작은 NULL들을 서로 구별하므로 여러 NULL 허용. NULLS NOT DISTINCT를 명시한 경우와 다른 DB의 규칙은 별도 조건. FK도 nullable 참조의 존재를 필수로 만들지 않음. 참조 열이 NOT NULL이어야 반드시 부모를 가리킴. 부모 삭제는 보존 정책에 맞춰 결정하며 CASCADE가 항상 적절하지 않음.

서버의 존재 확인과 삽입 사이에는 다른 요청이 들어올 수 있음. 서로 다른 인스턴스와 직접 SQL도 같은 DB 제약을 통과해야 한다는 점이 핵심. 한 행의 CHECK로 다른 행·전체 테이블의 중복을 안정적으로 막는 설계는 PostgreSQL 지원 범위를 벗어남.

정규화는 함수 종속성과 업무 의미에 따라 반복된 현재 사실을 분리하여 갱신 이상을 줄이는 선택. X→Y는 업무상 허용되는 모든 상태에서 같은 X가 같은 Y를 결정한다는 조건이며, 작은 표본의 우연한 일치만으로 확정하지 않음. 주문 당시 가격은 현재 상품 가격과 다른 역사적 사실이므로 과거 행에서 제거하면 안 됨. 의도적인 조회용 중복은 유지·동기화 책임과 비용을 명시해 판단. 정규형 이름만으로 모든 조회·저장 비용을 최적화했다고 단정하지 않음.

## 개념별 판단 근거

### 행 식별자 선택

한 회원의 여러 팀 가입을 허용할 행 식별자를 선택. member_id 단독 기본키의 과도한 제한 설명.

### 업무 조합의 유일성

같은 회원·팀 조합의 중복 금지와 대리키의 유일성은 별도 조건. 복합 PK 또는 별도 UNIQUE 조합으로 보장.

### 키와 필수값의 결합

복합 PK는 각 구성 열의 NOT NULL 포함. 대리키+UNIQUE 설계에서는 두 업무 ID에 NOT NULL을 별도 지정.

### 값 존재와 범위 제약

PostgreSQL CHECK의 unknown 통과와 NOT NULL의 별도 책임 구분. 필수 나이에 NOT NULL+CHECK 조합 제시.

### UNIQUE의 NULL 전제

PostgreSQL 17 기본 UNIQUE는 여러 NULL 허용. NULLS NOT DISTINCT 및 DB별 차이를 조건으로 구분.

### 참조의 존재와 필수성

FK의 유효한 부모 참조 보장과 NOT NULL의 필수 참조 보장을 분리. 없는 ID·NULL의 차이 설명.

### 참조 수명과 보존 정책

주문 이력 보존에 CASCADE 삭제를 피하고 참조 중 부모 삭제 거부·비활성화 등 선택. 비지연 NO ACTION과 지연 가능성 조건 구분.

### 공유 저장소의 업무 불변식

조회와 삽입 사이 경쟁을 설명하고 DB의 조합 UNIQUE 및 충돌 결과 처리로 모든 쓰기 경로를 보호.

### 행 CHECK의 적용 범위

PostgreSQL CHECK의 행 조건과 다른 행·테이블 상태 제약을 구분. 교차 행 중복에 CHECK 대신 UNIQUE 사용.

### 함수 종속성과 갱신 이상

주어진 업무 함수 종속성으로 현재 사실의 반복과 부분 갱신 위험을 설명. product_id를 주문 행 자체의 키로 오인하지 않음.

### 정규화 선택의 비용·책임

Product·Category 분리로 현재 사실의 소유자 지정. 조회용 중복은 동기화 책임·비용에 따라 허용 가능.

### 현재 사실과 거래 이력

주문 당시 unit_price는 역사적 사실. 현재 상품 가격으로 치환하지 않고 OrderLine에 보존하며 현재명 요구와 구분.

## 확인·판정 한계

직접 작성한 학습 근거 초안. 이 문서의 범위 밖 버전·숨은 요구조건·다른 시스템의 보장까지 추정하지 않음. 필수 개념의 근거가 검색 결과에서 빠지면 판정 보류하고 문서를 보강. 출처 절과 실제 관찰의 일치는 사람 검수 대상. 문서가 존재한다는 사실만으로 검색 적합성·모델 평가 품질·공개 승인을 증명하지 않음.

### A1-SQL-DOC

SQL·인덱스·쿼리 — 전제·원리·실패 조건 · PostgreSQL 17

# SQL 결과와 조회 비용을 구분

JOIN은 관련 행의 조합을 만든 뒤 WHERE·GROUP BY·집계·정렬이 적용되는 의미로 해석. 실행 계획의 실제 순서와 SQL의 논리적 결과 의미는 구분. LEFT JOIN의 매칭 실패 행은 오른쪽 NULL 확장 행을 남기지만 WHERE에서 오른쪽 조건을 요구하면 그 행을 다시 탈락시킬 수 있음. COUNT(*)는 결과 행 수, COUNT(non-null 식)은 값이 있는 행 수.

```text
자식 A 여러 행 × 자식 B 여러 행 → JOIN 팬아웃 → 잘못된 SUM
           각 자식을 부모당 먼저 집계 → 1행씩 JOIN → 의도한 집계
```

WHERE는 true만 남김. NOT IN의 비교 대상에 NULL이 있으면 매칭 없는 값도 unknown이 되어 탈락할 수 있음. 조건에 맞는 행 존재를 직접 검사하는 NOT EXISTS는 NULL 비교의 전제를 함께 해석. DISTINCT로 같은 금액을 지우는 것은 서로 다른 거래까지 합쳐 원래 합계를 복원하지 못할 수 있음.

순서가 계약이면 ORDER BY 명시. 같은 정렬값이 있으면 고유한 보조 키가 필요. 고정 데이터에서 결정적인 정렬과 동시 변경 중 페이지의 누락·중복 방지는 다른 문제.

PostgreSQL 17 다중 열 B-tree는 일부 열 조건에도 사용 가능하지만 선행 열 동등 조건과 뒤 범위 조건이 스캔 구간을 좁히는 데 유리. 선두 조건이 없다고 항상 사용 불가도 아니며, 사용 가능하다고 항상 빠른 것도 아님. 선택도·통계·행 수·저장 비용·쓰기 비용을 실제 계획과 비교. EXPLAIN ANALYZE는 대상 문장을 실행하므로 변경 SQL에는 부수 효과와 검증 환경을 먼저 확인.

## 개념별 판단 근거

### OUTER JOIN의 필터 위치

결제 조건을 ON에 두어 매칭 기준으로 적용. 오른쪽 WHERE 필터는 NULL 확장 행까지 제거할 수 있음 설명.

### 미매칭 행의 NULL 확장

LEFT JOIN의 미매칭 고객은 오른쪽 NULL인 결과 행 하나를 남김.

### 집계 대상 값의 존재

COUNT(*)는 NULL 확장 행도 셈. non-NULL PK인 COUNT(o.id)로 실제 매칭 주문만 세어 0 표현.

### WHERE의 unknown 탈락

주어진 NOT IN에서 1은 false, 2는 unknown이므로 WHERE 결과가 0행인 이유 설명.

### 반대 집합의 조건 표현

correlated NOT EXISTS 또는 외부 ID non-NULL 전제의 서브쿼리 NULL 제거로 2를 얻음. NULL 전제 구분.

### 명시적인 결과 순서

정렬 계약은 ORDER BY가 소유하며 PK 존재만으로 결과 순서 보장되지 않음.

### 동률 해소와 범위 전제

고유 보조 키까지 명시해 동률을 해소. 고정 데이터의 결정성과 동시 변경 페이지 안정성을 구분.

### 복합 B-tree의 스캔 구간

PG17에서 선행 동등·다음 범위 조건의 구간 축소와 일부 열만으로도 사용 가능한 조건 구분.

### 실행 계획의 비용 판단

인덱스 존재·사용 가능·실제 선택·속도를 구분. 분포·선택도·힙 접근과 쓰기 비용 비교.

### 실측의 실행 조건

대표 데이터에서 계획·실측을 비교. EXPLAIN ANALYZE는 실제 실행하므로 변경 SQL의 효과와 환경 확인.

### JOIN 팬아웃과 집계 중복

부모에 매달린 두 자식의 조합 2×3=6과 주문 금액 반복으로 120이 나오는 과정 설명.

### 집계 단위의 보존

각 자식을 부모당 먼저 집계 후 결합하여 40 보존. SUM(DISTINCT amount)는 서로 다른 같은 금액 주문을 지워 20임 설명.

## 확인·판정 한계

직접 작성한 학습 근거 초안. 이 문서의 범위 밖 버전·숨은 요구조건·다른 시스템의 보장까지 추정하지 않음. 필수 개념의 근거가 검색 결과에서 빠지면 판정 보류하고 문서를 보강. 출처 절과 실제 관찰의 일치는 사람 검수 대상. 문서가 존재한다는 사실만으로 검색 적합성·모델 평가 품질·공개 승인을 증명하지 않음.

### A1-TX-DOC

DB 트랜잭션·경쟁 — 전제·원리·실패 조건 · PostgreSQL 17; 외부 HTTP는 로컬 DB transaction에 미참여

# 동시 실행과 실패 범위를 정하는 트랜잭션

원자성은 참여한 트랜잭션의 변경을 함께 성공·실패시키는 범위. 로컬 DB rollback이 이미 외부 HTTP로 성공한 결제까지 취소하지 않음. 시스템 사이 원자성이 필요하면 실제 참여 자원·프로토콜·제공자 계약을 따로 확인. 단순 어노테이션으로 모든 외부 효과가 묶이지 않음.

```text
A: 재고10 읽음 ─────────────→ 9 기록
B:       재고10 읽음 ─────────────→ 9 기록
결과: 차감 두 번의 의도는 8, 고정값 덮어쓰기 결과는 9
```

PostgreSQL 17 Read Committed의 일반 SELECT는 문장마다 snapshot. 다른 transaction이 중간에 commit하면 다음 SELECT에서 보일 수 있음. Repeatable Read는 첫 비트랜잭션 제어 문장의 snapshot을 같은 transaction에서 유지하지만 자신의 변경은 보임. 격리 이름만으로 모든 DB의 이상 현상·잠금 구현이 같다고 추정하지 않음.

재고 같은 불변식은 조건부 원자 UPDATE와 영향 행 수 확인, version 기반 조건 갱신, 기존 행 잠금을 쓰기까지 유지하는 방법 등으로 보호 가능. PostgreSQL Read Committed에서 경쟁 중 조건부 UPDATE는 갱신된 행에 조건을 재검사하는 동작도 고려. 잠금으로 읽을 실제 행이 없는 상태에서 SELECT FOR UPDATE를 실행하면 미래 삽입까지 막는 gap lock이라고 가정하면 안 됨. UNIQUE 제약·기존 부모 행에 대한 일관된 잠금·Serializable와 재시도 등 요구에 맞는 방어를 선택.

Serializable은 모든 요청을 항상 성공시키는 설정이 아님. 직렬화 실패가 나면 새 snapshot에서 전체 transaction을 다시 시도하는 것이 기본. 횟수·시간·backoff·종료 결과를 정의하고 재시도에 외부 부수 효과가 포함되면 중복 방어·대사 계약을 따로 확인. PostgreSQL serialization failure SQLSTATE는 40001. 이미 실패 상태인 transaction의 마지막 SQL만 반복하는 것은 전체 판단을 재수행하지 못함.

## 개념별 판단 근거

### 트랜잭션 참여 자원 범위

로컬 DB rollback의 범위와 미참여 외부 HTTP 성공 효과를 분리. 주문 저장 실패·결제 성공 공존 설명.

### 범위 밖 효과의 복구 조건

식별자·상태 확인 후 제공자 계약에 맞는 재처리·보상·대사 선택. 복구 성공·중복 방어는 별도 보장임 설명.

### 문장 단위 snapshot

PG17 Read Committed 일반 SELECT의 문장 시작 snapshot으로 두 번째 값20 설명.

### 트랜잭션 snapshot의 시점

PG17 Repeatable Read는 첫 비트랜잭션 제어 문장 snapshot으로10 유지. BEGIN 시점·자신의 변경 예외 구분.

### 읽기·계산·덮어쓰기 경쟁

같은10을 읽고 고정9를 두 번 쓰는 실행 순서로 최종9 설명. 두 commit이 각 차감 반영을 의미하지 않음.

### 조건부 원자 변경과 결과 확인

현재 값의 원자 차감+quantity>=1+영향 행 수 확인 또는 동등한 version/행 잠금 방어. 충돌·재고 부족 결과 처리.

### 직렬화 실패의 재시도 범위

PG17 40001은 새 transaction에서 전체 읽기·판단·쓰기 재수행. 같은 실패 transaction의 마지막 SQL만 반복하는 방법 배제.

### 재시도 예산과 부수 효과

횟수·시간·backoff·종료 결과 제한과 외부 효과 중복 방어 조건 제시. 항상 성공·무한 재시도 보장 금지.

### 행 잠금의 대상 범위

PG17 RC SELECT FOR UPDATE는 조회된 실제 행 대상. 0행을 미래 예약 삽입에 대한 행 잠금으로 오인하지 않음.

### 부재 상태 보호의 조건

슬롯 UNIQUE+충돌 처리 또는 모든 writer의 같은 기존 행 잠금 등 방어 조건 제시. 격리·실패 처리 범위 구분.

## 확인·판정 한계

직접 작성한 학습 근거 초안. 이 문서의 범위 밖 버전·숨은 요구조건·다른 시스템의 보장까지 추정하지 않음. 필수 개념의 근거가 검색 결과에서 빠지면 판정 보류하고 문서를 보강. 출처 절과 실제 관찰의 일치는 사람 검수 대상. 문서가 존재한다는 사실만으로 검색 적합성·모델 평가 품질·공개 승인을 증명하지 않음.

### A1-HTTP-DOC

HTTP — 전제·원리·실패 조건 · RFC 9110·RFC 9111; HTTP 의미, 전송 버전과 구분

# 요청·응답의 의미와 조건

HTTP 메서드의 안전성은 클라이언트가 서버 상태 변경을 의도하는지에 관한 성질. 멱등성은 같은 요청을 반복했을 때 의도한 서버 효과가 한 번과 같은지에 관한 성질. 로그 등 부수 기록과 응답 코드까지 완전히 같아야 한다는 뜻이 아님. DELETE는 멱등 메서드지만 안전 메서드가 아님. 실제 API가 메서드 의미를 지키는지도 별도 확인.

```text
캐시 저장 여부 → 재사용 전 검증 여부 → 조건에 맞는 표현 제공
      no-store       no-cache          ETag·If-None-Match
```

아무 필드명을 지정하지 않은 no-cache는 저장을 금지하지 않고 재사용 전 성공적인 검증을 요구. no-store는 규칙을 따르는 캐시의 저장을 금지. private는 공유 캐시를 제한하며 사용자별 private cache 자체를 모두 금지하지 않음. HTTP 캐시 지시자가 악성 프로그램·이미 복사된 자료까지 삭제하거나 모든 개인정보 보호를 보장하는 수단은 아님.

GET에 If-None-Match를 보내 현재 표현과 맞으면 조건부 조회의 304 흐름으로 기존 표현을 재사용 가능. 변경 요청 If-Match는 낡은 버전으로 갱신하지 않도록 사전 조건을 검사. 이 조건 검사와 실제 변경이 떨어져 있으면 중간 경쟁을 막지 못하므로 서버에서 함께 보장. 인증·인가의 대체가 아님.

401은 유효한 인증 자격 부족과 challenge, 403은 요청 이해 후 수행 거부의 의미. 403이 항상 인증 완료를 증명하지 않음. 자원 존재를 숨기기 위한 404는 정책상 가능하지만 그 선택을 인증 성공으로 오해하지 않음.

303은 결과를 다른 자원에서 GET/HEAD로 조회하는 의미. 307은 자동 redirect에서 기존 메서드를 바꾸지 않아 POST 본문 재전송 가능. redirect 자체가 결제 중복 방어를 만들지는 않음. 원 요청·목적지 API 계약과 의도한 효과를 함께 확인.

## 개념별 판단 근거

### 의도한 효과의 멱등성

반복 DELETE의 의도한 최종 효과와 응답 동일성을 분리. 204/404 차이만으로 비멱등 판정 금지.

### 안전성과 변경 의도

안전성은 클라이언트의 상태 변경 의도 기준. DELETE는 멱등이어도 안전하지 않음.

### 저장과 재검증의 구분

필드명 없는 no-cache의 저장 가능·재검증 요구와 no-store의 저장·재사용 금지를 구분.

### 캐시 적용 범위

private의 공유/개별 캐시 구분과 no-store의 비준수 저장·기존 복사에 대한 보장 한계 설명.

### 조건부 읽기 검증

주어진 GET의 If-None-Match 일치 조건에서304와 기존 표현 재사용 설명.

### 변경 사전 조건의 방어

주어진 If-Match 불일치에서412·미변경. 검사와 변경을 함께 보장하고 인증과 구분.

### 인증 challenge와 수행 거부

401의 유효 인증 자격 부족·challenge와403의 이해 후 수행 거부를 구분.

### 오류 응답의 정보 공개 정책

403은 인증 완료의 항상 보장이 아님. 존재 은폐를 위한404 가능성과 실제 인가 검사 별도 설명.

### redirect의 메서드 의미

303의 다른 자원 GET/HEAD 조회와307의 자동 redirect 메서드 유지 구분.

### redirect와 업무 효과의 경계

307의 목적지 POST 효과와 원 요청 재전송 계약 확인.303도 원 결제의 중복 방어를 자동 보장하지 않음.

## 확인·판정 한계

직접 작성한 학습 근거 초안. 이 문서의 범위 밖 버전·숨은 요구조건·다른 시스템의 보장까지 추정하지 않음. 필수 개념의 근거가 검색 결과에서 빠지면 판정 보류하고 문서를 보강. 출처 절과 실제 관찰의 일치는 사람 검수 대상. 문서가 존재한다는 사실만으로 검색 적합성·모델 평가 품질·공개 승인을 증명하지 않음.

### A1-API-DOC

API·REST — 전제·원리·실패 조건 · Fielding REST; RFC 9110; OpenAPI 3.1.0; 사례별 서비스 계약

# HTTP 위에 서비스의 약속을 설계

HTTP는 통신 의미를 정의하고 API는 어떤 자원·업무 동작·오류·호환성을 약속하는지 정의. JSON과 명사 URI, CRUD 메서드만으로 REST의 모든 제약을 증명하지 못함. Fielding의 REST는 client-server, stateless, cache, uniform interface, layered system 제약과 선택적 code-on-demand를 설명. Uniform interface는 자원 식별, 표현을 통한 조작, 자기 설명 메시지, hypermedia에 의한 상태 전이를 포함. 알려진 정보가 부족하면 REST 전체 충족 여부를 보류.

```text
클라이언트 요청 → 입력·인증·인가 경계 → 업무 상태 전이 → 저장·효과
      API 계약은 성공·거부·중복·버전 변경 결과까지 연결
```

취소 같은 업무 동작은 취소 자원 생성이나 명시적인 command endpoint 등으로 표현 가능. 특정 URI의 모양 하나보다 상태 전이·권한·실패 결과의 계약이 중요. 배송 완료 주문 취소를 클라이언트 상태 문자열로 우회하게 해서는 안 됨. 내부 도메인 설계 패턴 전부는 객체 설계 Topic에서 다루고 여기서는 외부 동작 계약을 평가.

멱등 키 예시는 이 묶음이 정의한 자체 서비스 계약. 같은 키·내용의 반복은 같은 업무 효과·기록 결과로 연결하고, 내용이 다른 반복은 거부하며 보존 기간을 명시. 키 이름을 헤더에 썼다는 사실이 보장을 만들지 않음. 요청자·작업별 키 범위, fingerprint, 진행/완료 상태, 결과 저장과 경쟁 방어가 필요. 기간 만료 뒤의 의미·외부 효과의 중복 방어는 별도 계약. 본 문서가 업계 전체의 Idempotency-Key 표준 구현을 단정하는 것은 아님.

Cursor는 정렬·필터·위치·유효기간 같은 페이지 계약을 담는 식별자. 인코딩은 인증·인가를 대신하지 않음. 동일 정렬에서 고유 보조 키와 필터 일치를 유지하고 변경 시 거부·초기화 정책을 정함. cursor 사용만으로 모든 쿼리 비용·동시 변경 안정성이 자동 보장되지 않음.

OpenAPI는 입력·응답·스키마를 기술하는 명세. 문서만 추가해서 runtime 응답이 검사되는 것은 아니며 별도 validation·계약 테스트가 필요. 소비자에게 약속한 필드 타입·필수성을 바꾸는 경우 실제 호환성·이관 기간·버전 계약을 확인. 서비스 단위 테스트 green과 실제 HTTP 응답의 계약 일치는 다른 관찰.

## 개념별 판단 근거

### REST의 uniform interface

자원 식별·표현 조작·자기 설명 메시지·hypermedia 상태 전이를 포함. URI·JSON만으로 충분하다고 판단하지 않음.

### 아키텍처 제약의 관찰 범위

client-server·stateless·cache·layered system의 추가 관찰과 선택 code-on-demand 구분. 정보 부족 상태에서 전체 충족/불충족 확정 금지.

### 업무 동작의 외부 계약

취소 자원 생성 또는 명시 command를 요구에 따라 허용. URI 모양 하나보다 동작·성공·거부 계약을 평가.

### 상태 전이 조건의 서버 검증

서버의 실제 상태·권한에 따라 배송 전 취소만 허용. 입력 status로 규칙 우회 금지, 배송 완료 거부·오류 계약 제시.

### 요청 식별·내용·결과의 연결

키 범위·내용 fingerprint·진행/완료 상태·내구 결과 연결. 같은 키의 다른 내용 거부와 동시 최초 처리의 원자 조정 설명.

### 멱등 계약의 보존·효과 범위

진행 중 정책·보존 기간을 명시하고 만료 후 의미·외부 결제는 별도 계약. POST 표준 자체의 자동 보장과 구분.

### 페이지 위치의 계약 전제

정렬·고유 보조 키·필터가 cursor 위치 의미의 전제. 필터 변경 시 거부/초기화 정책으로 다른 집합에 조용히 적용하지 않음.

### cursor의 보장 한계

opaque 표현은 권한 검사를 대체하지 않음. 성능·동시 변경·유효기간 정책의 별도 조건 설명.

### 명세와 runtime의 일치

OpenAPI 기술과 실제 응답 검증을 구분. HTTP 필수 필드·타입·직렬화를 확인하며 Service green만으로 대체하지 않음.

### 소비자 계약의 점진 이관

memberId 소비자가 남은 동안 일방 제거의 호환 위험 설명. 병행 필드·이관/폐기 또는 새 버전의 계약과 실제 소비자 상태 확인.

## 확인·판정 한계

직접 작성한 학습 근거 초안. 이 문서의 범위 밖 버전·숨은 요구조건·다른 시스템의 보장까지 추정하지 않음. 필수 개념의 근거가 검색 결과에서 빠지면 판정 보류하고 문서를 보강. 출처 절과 실제 관찰의 일치는 사람 검수 대상. 문서가 존재한다는 사실만으로 검색 적합성·모델 평가 품질·공개 승인을 증명하지 않음.

### A1-TEST-DOC

테스트 — 전제·원리·실패 조건 · Spring Framework 7.0.x; Spring Boot 4.1.x; JUnit 6.1.3 문서 참조

# 무엇을 검증했는지 경계를 읽는 테스트

테스트는 특정 입력·실행 조건에서 기대 결과를 자동 판정하는 증거. 통과 수가 많아도 실행하지 않은 경계의 정확성을 증명하지 않음. 순수 도메인 규칙은 작은 단위 테스트, 저장·transaction 경계는 실제 DB 통합, HTTP binding·validation·직렬화는 HTTP 계약 테스트, 핵심 연결 흐름은 소수의 전체 통합에서 확인. 테스트 대상과 실제 협력자 범위를 함께 설명.

```text
순수 객체 → 도메인 규칙
Service + 실제 DB → commit 뒤 저장·제약
Controller + Service double → HTTP 계약
실제 연결 흐름 → 연결·환경 경계
```

Repository double은 통제한 응답에 대한 호출자 동작을 확인할 수 있지만 실제 FK·SQL·mapping·commit을 실행하지 않음. double이 필요 없는 도메인 규칙까지 대체하면 실제 규칙을 검증하지 못함. 이 프로젝트의 Service 통합 테스트는 실제 Repository/DB와 test-owned teardown을 사용하는 로컬 지침이며 모든 프로젝트의 universal mock 금지 규칙이 아님.

Spring의 test-managed transaction은 실행 스레드에 연결. RANDOM_PORT 실제 HTTP 서버의 transaction은 클라이언트 테스트와 별개로 commit되며 테스트 rollback에 따라 자동 정리되지 않음. 선점 timeout으로 별도 스레드에서 코드를 실행하는 경우도 경계 확인 필요. 데이터를 만든 테스트가 실패·부분 준비 후에도 자식→부모 순서로 정리. 공유 DB의 병렬 테스트에서 전역 deleteAll로 다른 실행 데이터를 지우지 않도록 격리.

비동기 작업은 sleep 시간이 지났다는 사실보다 완료 조건을 관찰해야 함. latch·이벤트·제한 시간 polling 등으로 정한 deadline 안에 실제 최종 상태를 assertion하고, 끝나지 않으면 실패. 무한 polling은 suite를 멈추므로 제한 필요. 큰 timeout은 환경 문제를 숨기거나 완료 자체를 보장하지 않음.

timeout·429·503을 내는 fake provider는 실패 조건을 재현하여 클라이언트 retry·취소·상태·응답을 확인하는 수단. 실제 제공자의 장애 동작·모델 정확도·대표 사용자 답변의 품질을 실행한 결과와 구분. 작성자가 만든 정답 사례를 개발과 평가 양쪽에 써도 독립 대표 표본이 되지는 않음. 어떤 환경·데이터·assertion이 실행됐는지 기록하고 미실행 경계는 남김.

## 개념별 판단 근거

### 요구 실패에 맞는 검증 수준

도메인·Service 저장·HTTP 계약·핵심 연결별 최소 충분한 수준 선택과 기대 assertion 설명.

### 실행 경계와 주장 범위

각 테스트의 실제/double 경계를 명시. Controller의 Service double은 실제 DB 저장·전체 연결을 증명하지 않음.

### 테스트 double의 관찰 범위

mock 응답에 대한 호출자 분기와 실제 SQL·FK·mapping·commit의 미실행을 구분. mock이 항상 무용하다는 단정 금지.

### 저장 계약의 실제 실행

실제 DB·버전에서 FK 경계·query 결과·commit 뒤 재조회 보강. 실제 규칙을 double로 대체한 증거의 한계 설명.

### 테스트와 서버 transaction 경계

RANDOM_PORT의 실제 HTTP 서버·클라이언트는 별도 스레드/transaction. test rollback이 서버 commit을 되돌리지 않음.

### 데이터 생성자의 정리 책임

데이터 생성 테스트가 실패·부분 준비 후에도 자식→부모 정리. 공유 병렬 DB에서 전역 삭제 대신 격리/직렬화 선택.

### 관찰 조건에 따른 대기

시간 경과와 완료를 분리. 이벤트/latch/상태를 기다린 뒤 실제 terminal 결과 assertion.

### 대기의 실패·시간 경계

deadline·timeout·미완료/다른 terminal 실패 정의. 무한 polling과 큰 sleep의 한계·timeout 실행 경계 확인.

### 통제한 실패 주입의 역할

fake timeout·429·503에서 확인한 클라이언트 경로·assertion과 실제 provider 미실행 경계를 명시.

### 품질 표본과 증거의 독립성

작성자 진단 사례와 독립 대표 표본·사람 기준·실제 모델 대조를 구분. 실행 환경·입력·assertion 한계 기록.

## 확인·판정 한계

직접 작성한 학습 근거 초안. 이 문서의 범위 밖 버전·숨은 요구조건·다른 시스템의 보장까지 추정하지 않음. 필수 개념의 근거가 검색 결과에서 빠지면 판정 보류하고 문서를 보강. 출처 절과 실제 관찰의 일치는 사람 검수 대상. 문서가 존재한다는 사실만으로 검색 적합성·모델 평가 품질·공개 승인을 증명하지 않음.

## 출처 이용 조건 검수

원문·코드·그림 미포함. 아래 메모는 법률 검토나 공개 승인 결과가 아님.

- [PostgreSQL: Documentation: 17: 5.5. Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 62.5. Index Uniqueness Checks](https://www.postgresql.org/docs/17/index-unique-checks.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [DB Design - Database Systems](https://cs186berkeley.net/notes/note13/): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [Chapter 12 Normalization – Database Design – 2nd Edition](https://opentextbc.ca/dbdesign01/chapter/chapter-12-normalization/): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 7.2. Table Expressions](https://www.postgresql.org/docs/17/queries-table-expressions.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 9.24. Subquery Expressions](https://www.postgresql.org/docs/17/functions-subquery.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 7.5. Sorting Rows (ORDER BY)](https://www.postgresql.org/docs/17/queries-order.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 7.6. LIMIT and OFFSET](https://www.postgresql.org/docs/17/queries-limit.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 11.3. Multicolumn Indexes](https://www.postgresql.org/docs/17/indexes-multicolumn.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 14.1. Using EXPLAIN](https://www.postgresql.org/docs/17/using-explain.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 9.21. Aggregate Functions](https://www.postgresql.org/docs/17/functions-aggregate.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 3.4. Transactions](https://www.postgresql.org/docs/17/tutorial-transactions.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 13.2. Transaction Isolation](https://www.postgresql.org/docs/17/transaction-iso.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [PostgreSQL: Documentation: 17: 13.3. Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [RFC 9111: HTTP Caching](https://www.rfc-editor.org/rfc/rfc9111.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [
Fielding Dissertation: CHAPTER 5: Representational State Transfer (REST)
](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [OpenAPI Specification v3.1.0](https://spec.openapis.org/oas/v3.1.0.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [Unit Testing :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/unit.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [Transaction Management :: Spring Framework](https://docs.spring.io/spring-framework/reference/7.0/testing/testcontext-framework/tx.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [Testing Spring Boot Applications :: Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
- [JUnit](https://docs.junit.org/6.1.3/_exports/junit-user-guide-6.1.3.html): 직접 작성한 한국어 설명과 독자적인 사례. 원문·코드·그림 미포함. 사실 확인용 링크만 제공. 원 출처의 이용 조건과 공개 적합성은 사람 검수 대기.
