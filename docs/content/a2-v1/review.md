# a2-v1 검수용 읽기 자료

> 자동 생성 읽기 사본. 수정 기준은 [bundle.json](bundle.json). 사람 검수·공개 승인 대기.

- 원본 SHA-256: `8077c1a2388e9e454ad72d98b8db6f71dd92f7b2e3213c5786bf038b5f648292`
- 재생성: 저장소 루트에서 `python3 scripts/render_content_review.py --bundle docs/content/a2-v1/bundle.json`
- 검수 절차·승인 기록: [콘텐츠 안내](README.md#사람-검수공개-순서)
- 이 자료를 읽었다는 사실만으로 앱의 검수·공개 상태가 바뀌지 않음

## 문항과 판정 기준

### A2-FND-01 · BASIC

압축·헤더·재전송 없이 파일 10 MiB를 일정한 100 Mbit/s로 보낸다고 가정한다. 1바이트는 8비트다. 전송 시간은 얼마인가? 실제 다운로드 시간에도 같은 값을 단정할 수 있는지 설명하라.

**모범 답안**

10 MiB는 10×2^20바이트, 즉 83,886,080비트다. 100 Mbit/s는 초당 100,000,000비트이므로 이 모델의 시간은 0.8388608초다. MiB와 십진 Mbit/s를 구별하고 비트로 단위를 맞춰 나눈다. 실제 측정에서는 연결 준비·프로토콜 비용·재전송·다른 트래픽과 유효 전송률을 확인해야 하므로 이 이상화 값을 그대로 단정할 수 없다.

**필수 개념**

- 이진 용량과 십진 속도의 단위 · 가중치 0.50: MiB=2^20 B, Mbit/s=10^6 bit/s, 8 bit/B를 적용해 0.8388608초 계산.
- 이상화 계산과 실제 전송의 경계 · 가중치 0.50: 추가 비용 없는 일정 속도 모델과 실제 연결·헤더·재전송·유효 속도를 구별하며 현실 시간 단정 거부.

**개념별 판정 경계**

- `A2_FND_01_01`
  - CORRECT: MiB=2^20 B, Mbit/s=10^6 bit/s, 8 bit/B를 적용해 0.8388608초 계산.
  - PARTIALLY_CORRECT: 단위 변환 방향만 제시하고 비트·바이트 또는 접두어 구별 일부 누락.
  - INCORRECT: MiB·Mbit·바이트를 같은 단위로 취급하거나 0.1초 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_FND_01_02`
  - CORRECT: 추가 비용 없는 일정 속도 모델과 실제 연결·헤더·재전송·유효 속도를 구별하며 현실 시간 단정 거부.
  - PARTIALLY_CORRECT: 추가 비용 가능성만 언급하고 무엇을 측정할지 누락.
  - INCORRECT: 표기 대역폭만으로 현실 전송 시간을 항상 확정.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-FND-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 10 MiB는 10×2^20바이트, 즉 83,886,080비트다. 100 Mbit/s는 초당 100,000,000비트이므로 이 모델의 시간은 0.8388608초다. MiB와 십진 Mbit/s를 구별하고 비트로 단위를 맞춰 나눈다. 실제 측정에서는 연결 준비·프로토콜 비용·재전송·다른 트래픽과 유효 전송률을 확인해야 하므로 이 이상화 값을 그대로 단정할 수 없다.
  - 예상 개념 판정: A2_FND_01_01: CORRECT, A2_FND_01_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-FND-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 파일은 10,485,760바이트이고 회선 속도는 12,500,000바이트/초로 바꿀 수 있다. 둘을 나누면 약 0.839초다. 이 결과는 일정한 유효 속도와 추가 비용 없음이라는 전제의 계산이다. 현실에서는 그 전제가 맞는지부터 측정한다.
  - 예상 개념 판정: A2_FND_01_01: CORRECT, A2_FND_01_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-FND-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: MiB를 2^20바이트로, 100 Mbit/s를 100,000,000비트/초로 맞춰 계산하면 0.8388608초다. 실제 다운로드에는 추가 비용이 있을 수 있다.
  - 예상 개념 판정: A2_FND_01_01: CORRECT, A2_FND_01_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-FND-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: MiB도 Mbit도 모두 10^6바이트를 뜻하므로 0.1초다. 회선 표기만 알면 실제 완료 시간도 항상 0.1초로 확정된다.
  - 예상 개념 판정: A2_FND_01_01: INCORRECT, A2_FND_01_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-FND-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 10 MiB는 10×2^20바이트, 즉 83,886,080비트다. 100 Mbit/s는 초당 100,000,000비트이므로 이 모델의 시간은 0.8388608초다. MiB와 십진 Mbit/s를 구별하고 비트로 단위를 맞춰 나눈다. 실제 측정에서는 연결 준비·프로토콜 비용·재전송·다른 트래픽과 유효 전송률을 확인해야 하므로 이 이상화 값을 그대로 단정할 수 없다.
  - 예상 개념 판정: A2_FND_01_01: NEEDS_REVIEW, A2_FND_01_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 대문자 B와 소문자 bit, Mi와 M의 차이를 먼저 확인. 계산값은 실제 네트워크 실측이 아님.

**출처 대조 위치**

- [NIST — Prefixes for binary multiples](https://physics.nist.gov/cuu/Units/binary.html) — Prefixes for binary multiples: MiB·Mbit 비교

근거 문서: `A2-FND-DOC`. 검수 상태: **PENDING**

### A2-FND-02 · BASIC

Java 21에서 int count = 2147483647이다. long a = count + 1;과 long b = (long) count + 1;의 값이 왜 다른가? 계산 뒤 long으로 바꾸는 방식이 충분한지 설명하라.

**모범 답안**

a는 -2147483648L, b는 2147483648L이다. 첫 식의 덧셈은 두 피연산자가 int이므로 int로 실행되어 범위를 넘긴 뒤 그 결과를 long에 대입한다. 이미 잃은 상위 비트는 대입으로 복구되지 않는다. 두 번째 식은 계산 전에 한 피연산자를 long으로 바꾸어 long 덧셈을 한다. 이 입력은 long 범위 안이지만 long도 유한하므로 모든 입력의 오버플로가 사라지는 것은 아니다.

**필수 개념**

- 표현 범위와 연산 시점 · 가중치 0.50: int 덧셈의 범위 초과 후 long 확대를 구별하여 a=-2147483648, b=2147483648 설명.
- 계산 전 확대와 남은 한계 · 가중치 0.50: 피연산자를 계산 전에 long으로 확대하거나 1L 사용. 계산 뒤 확대의 복구 불가·long 범위 한계 명시.

**개념별 판정 경계**

- `A2_FND_02_01`
  - CORRECT: int 덧셈의 범위 초과 후 long 확대를 구별하여 a=-2147483648, b=2147483648 설명.
  - PARTIALLY_CORRECT: 범위 초과는 인식하나 두 값·연산 시점 일부 누락.
  - INCORRECT: 대입 대상이 long이면 int 덧셈도 자동으로 long 계산된다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_FND_02_02`
  - CORRECT: 피연산자를 계산 전에 long으로 확대하거나 1L 사용. 계산 뒤 확대의 복구 불가·long 범위 한계 명시.
  - PARTIALLY_CORRECT: 계산 전 확대는 설명하나 long도 유한하다는 경계 누락.
  - INCORRECT: 계산 뒤 캐스팅이 잃은 값을 복구하거나 long은 무한 범위라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-FND-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: a는 -2147483648L, b는 2147483648L이다. 첫 식의 덧셈은 두 피연산자가 int이므로 int로 실행되어 범위를 넘긴 뒤 그 결과를 long에 대입한다. 이미 잃은 상위 비트는 대입으로 복구되지 않는다. 두 번째 식은 계산 전에 한 피연산자를 long으로 바꾸어 long 덧셈을 한다. 이 입력은 long 범위 안이지만 long도 유한하므로 모든 입력의 오버플로가 사라지는 것은 아니다.
  - 예상 개념 판정: A2_FND_02_01: CORRECT, A2_FND_02_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-FND-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 결과 변수의 타입보다 연산 시점의 피연산자 타입이 먼저다. 첫 식은 int 범위에서 돌아 음수가 된 뒤 확대되고, 두 번째는 확대 후 더하므로 양수 2147483648이다. 1L을 더하는 것도 같은 입력에서 유효하다. 더 큰 long 입력의 범위 초과는 별도로 확인해야 한다.
  - 예상 개념 판정: A2_FND_02_01: CORRECT, A2_FND_02_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-FND-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: a는 int 덧셈 오버플로 뒤 확대되어 -2147483648이고 b는 먼저 long 연산으로 바뀌어 2147483648이다. long을 쓰면 더 큰 수를 담을 수 있다.
  - 예상 개념 판정: A2_FND_02_01: CORRECT, A2_FND_02_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-FND-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: long 변수에 대입하므로 a와 b 모두 2147483648이다. 덧셈을 마친 뒤 long으로 캐스팅하면 모든 오버플로가 복구되고 long은 크기 제한도 없다.
  - 예상 개념 판정: A2_FND_02_01: INCORRECT, A2_FND_02_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-FND-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: a는 -2147483648L, b는 2147483648L이다. 첫 식의 덧셈은 두 피연산자가 int이므로 int로 실행되어 범위를 넘긴 뒤 그 결과를 long에 대입한다. 이미 잃은 상위 비트는 대입으로 복구되지 않는다. 두 번째 식은 계산 전에 한 피연산자를 long으로 바꾸어 long 덧셈을 한다. 이 입력은 long 범위 안이지만 long도 유한하므로 모든 입력의 오버플로가 사라지는 것은 아니다.
  - 예상 개념 판정: A2_FND_02_01: NEEDS_REVIEW, A2_FND_02_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** Java의 규칙을 C/C++ 등 다른 언어의 signed overflow에 일반화하지 않음.

**출처 대조 위치**

- [Java Language Specification 21 — Chapter 4 Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — §4.2.1 Integer Types and Values; §4.2.2 Integer Operations
- [Java Language Specification 21 — Chapter 15 Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — §15.18.2 Additive Operators for Numeric Types

근거 문서: `A2-FND-DOC`. 검수 상태: **PENDING**

### A2-FND-03 · BASIC

접근 허용 조건은 “활성 회원이면서, 소유자이거나 관리자”다. 순수 boolean active, owner, admin만 사용한다. (active && owner) || admin이 조건을 만족하는가? 올바른 식과 반례 하나를 제시하라.

**모범 답안**

올바른 식은 active && (owner || admin)이다. 기존 식은 admin=true이면 active=false여도 허용한다. 예를 들어 active=false, owner=false, admin=true이면 기존 식은 true이고 요구한 식은 false다. 괄호는 활성 조건이 소유자·관리자 양쪽에 공통으로 적용됨을 드러낸다. 모든 입력 조합의 진리표로 두 식을 비교할 수 있다.

**필수 개념**

- 논리 조건의 공통 전제 · 가중치 0.50: active가 owner·admin 양쪽에 적용되는 식 또는 동치 식 제시.
- 반례로 조건 차이 검증 · 가중치 0.50: active=false, admin=true인 구체 입력에서 기존 true·요구 false 비교.

**개념별 판정 경계**

- `A2_FND_03_01`
  - CORRECT: active가 owner·admin 양쪽에 적용되는 식 또는 동치 식 제시.
  - PARTIALLY_CORRECT: 방향은 맞으나 식이나 역할별 적용 설명 누락.
  - INCORRECT: 관리자 분기가 active를 우회하는 식을 요구와 동일시.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_FND_03_02`
  - CORRECT: active=false, admin=true인 구체 입력에서 기존 true·요구 false 비교.
  - PARTIALLY_CORRECT: 진리표·테스트 필요성만 설명하고 구체 반례와 결과 누락.
  - INCORRECT: 차이가 없거나 비활성 관리자 허용이 요구와 일치한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-FND-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 올바른 식은 active && (owner || admin)이다. 기존 식은 admin=true이면 active=false여도 허용한다. 예를 들어 active=false, owner=false, admin=true이면 기존 식은 true이고 요구한 식은 false다. 괄호는 활성 조건이 소유자·관리자 양쪽에 공통으로 적용됨을 드러낸다. 모든 입력 조합의 진리표로 두 식을 비교할 수 있다.
  - 예상 개념 판정: A2_FND_03_01: CORRECT, A2_FND_03_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-FND-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: (active && owner) || (active && admin)도 같은 조건을 표현한다. 비활성 관리자는 거절해야 한다. false,false,true를 넣으면 잘못된 식만 true가 되므로 관리자 예외가 활성 검사를 우회함을 볼 수 있다.
  - 예상 개념 판정: A2_FND_03_01: CORRECT, A2_FND_03_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-FND-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: active && (owner || admin)이 맞다. 활성 조건을 두 역할 모두에 적용하며 진리표로 확인한다.
  - 예상 개념 판정: A2_FND_03_01: CORRECT, A2_FND_03_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-FND-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: (active && owner) || admin이 요구와 완전히 같다. 비활성 관리자도 요구상 활성 회원으로 간주하므로 두 식에 차이를 만드는 입력은 없다.
  - 예상 개념 판정: A2_FND_03_01: INCORRECT, A2_FND_03_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-FND-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 올바른 식은 active && (owner || admin)이다. 기존 식은 admin=true이면 active=false여도 허용한다. 예를 들어 active=false, owner=false, admin=true이면 기존 식은 true이고 요구한 식은 false다. 괄호는 활성 조건이 소유자·관리자 양쪽에 공통으로 적용됨을 드러낸다. 모든 입력 조합의 진리표로 두 식을 비교할 수 있다.
  - 예상 개념 판정: A2_FND_03_01: NEEDS_REVIEW, A2_FND_03_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 운영 인가 기능 구현이 아닌 순수 논리 사례. 단락 평가의 부수 효과는 문항에서 제외.

**출처 대조 위치**

- [Java Language Specification 21 — Chapter 15 Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — §15.23 Conditional-And; §15.24 Conditional-Or. 업무 허용식은 문항 자체 계약

근거 문서: `A2-FND-DOC`. 검수 상태: **PENDING**

### A2-FND-04 · BASIC

후보가 1,048,576개다. 단계마다 후보 수를 정확히 절반으로 줄여 1개가 될 때까지 반복한다. 몇 단계가 필요한가? 후보 수를 두 배로 늘릴 때 단계 수와, 매 단계의 실제 시간이 어떻게 달라지는지 구분하라.

**모범 답안**

1,048,576=2^20이므로 20번 줄이면 1개가 된다. 후보를 두 배로 늘린 2^21에서는 21단계로 하나만 늘어난다. 이는 반복 횟수의 로그 증가 설명이다. 매 단계 작업이 O(1)일 때 전체를 O(log n)으로 분석할 수 있다. 매번 후보를 복사·순회하면 단계 비용이 일정하지 않으므로 단계 수만으로 실행 시간이 같은 비율로 늘어난다고 단정할 수 없다.

**필수 개념**

- 절반 축소와 로그 단계 · 가중치 0.50: 2^20→1에 20단계, 입력 두 배는 한 단계 증가 설명.
- 단계 수와 단계 비용 · 가중치 0.50: O(log n) 실행 시간의 일정한 단계 비용 전제와 복사·순회 반례 구별.

**개념별 판정 경계**

- `A2_FND_04_01`
  - CORRECT: 2^20→1에 20단계, 입력 두 배는 한 단계 증가 설명.
  - PARTIALLY_CORRECT: 로그 증가 방향만 설명하고 단계 계산 누락.
  - INCORRECT: 두 배 입력에서 절반 축소 단계도 두 배라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_FND_04_02`
  - CORRECT: O(log n) 실행 시간의 일정한 단계 비용 전제와 복사·순회 반례 구별.
  - PARTIALLY_CORRECT: 구현에 따라 시간이 다름만 언급하고 단계 비용 전제 누락.
  - INCORRECT: 매 단계 O(n) 작업도 단계 수만으로 전체 O(log n)이라고 단정.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-FND-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 1,048,576=2^20이므로 20번 줄이면 1개가 된다. 후보를 두 배로 늘린 2^21에서는 21단계로 하나만 늘어난다. 이는 반복 횟수의 로그 증가 설명이다. 매 단계 작업이 O(1)일 때 전체를 O(log n)으로 분석할 수 있다. 매번 후보를 복사·순회하면 단계 비용이 일정하지 않으므로 단계 수만으로 실행 시간이 같은 비율로 늘어난다고 단정할 수 없다.
  - 예상 개념 판정: A2_FND_04_01: CORRECT, A2_FND_04_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-FND-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: k번 뒤 후보가 n/2^k개이므로 n/2^k=1을 풀면 k=log2 n이다. 20에서 21로 늘어난다. 작은 배열 부분을 만드는 데 전체를 훑는 구현이라면, 단계가 적어도 총 작업량은 로그라고 보장되지 않는다.
  - 예상 개념 판정: A2_FND_04_01: CORRECT, A2_FND_04_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-FND-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 2^20에서 절반 줄이기를 20번 하면 하나다. 두 배 입력은 21번이므로 단계는 한 번 늘어난다. 실제 시간은 구현을 확인해야 한다.
  - 예상 개념 판정: A2_FND_04_01: CORRECT, A2_FND_04_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-FND-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 후보를 두 배로 늘리면 필요한 줄이기도 40번으로 두 배다. 단계마다 전체 후보를 복사해도 단계 수가 적으니 실행 시간은 무조건 O(log n)이다.
  - 예상 개념 판정: A2_FND_04_01: INCORRECT, A2_FND_04_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-FND-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 1,048,576=2^20이므로 20번 줄이면 1개가 된다. 후보를 두 배로 늘린 2^21에서는 21단계로 하나만 늘어난다. 이는 반복 횟수의 로그 증가 설명이다. 매 단계 작업이 O(1)일 때 전체를 O(log n)으로 분석할 수 있다. 매번 후보를 복사·순회하면 단계 비용이 일정하지 않으므로 단계 수만으로 실행 시간이 같은 비율로 늘어난다고 단정할 수 없다.
  - 예상 개념 판정: A2_FND_04_01: NEEDS_REVIEW, A2_FND_04_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 후보 축소 20단계와 특정 이진 탐색 구현의 비교 횟수는 별개. 마지막 비교를 더하는 구현도 허용.

**출처 대조 위치**

- [Princeton Algorithms 4e — Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/) — Order-of-growth classifications; binary search 사례

근거 문서: `A2-FND-DOC`. 검수 상태: **PENDING**

### A2-FND-05 · INTERMEDIATE

각각 100건의 응답 시간이다. A는 전부 20ms, B는 94건이 10ms이고 6건이 530/3ms다. p95는 오름차순 ceil(0.95×100)번째 값으로 정한다. 평균과 p95를 비교하고, 이 표본으로 전체 사용자의 미래 품질을 확정할 수 있는지 설명하라.

**모범 답안**

A 평균과 p95는 20ms다. B의 합은 94×10+6×530/3=2000ms이므로 평균도 20ms지만 95번째 값은 530/3ms, 약176.67ms다. 같은 평균이 느린 꼬리를 숨긴다. p95는 95% 지점의 값이지 가장 느린 5%의 평균이 아니다. 주어진 100건·계산법의 결과이며 측정 기간·요청 종류·실패 포함 여부·표본 대표성을 확인하지 않고 미래 전체 사용자 품질을 확정할 수 없다.

**필수 개념**

- 평균과 백분위의 다른 정보 · 가중치 0.50: 두 평균20ms, A p95=20ms·B p95=530/3ms 계산. 순위값과 꼬리 평균 구별.
- 표본 통계의 적용 범위 · 가중치 0.50: 표본·기간·요청/실패 분모·대표성을 명시하고 미래 전체 사용자 품질 단정 거부.

**개념별 판정 경계**

- `A2_FND_05_01`
  - CORRECT: 두 평균20ms, A p95=20ms·B p95=530/3ms 계산. 순위값과 꼬리 평균 구별.
  - PARTIALLY_CORRECT: 느린 꼬리 차이는 설명하나 계산·정의 일부 누락.
  - INCORRECT: 같은 평균이면 p95도 같거나 p95를 느린5% 평균으로 정의.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_FND_05_02`
  - CORRECT: 표본·기간·요청/실패 분모·대표성을 명시하고 미래 전체 사용자 품질 단정 거부.
  - PARTIALLY_CORRECT: 추가 측정 필요성만 언급하고 적용 조건 누락.
  - INCORRECT: 작은 지정 표본을 미래 전체 사용자 품질의 확정 증거로 사용.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-FND-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: A 평균과 p95는 20ms다. B의 합은 94×10+6×530/3=2000ms이므로 평균도 20ms지만 95번째 값은 530/3ms, 약176.67ms다. 같은 평균이 느린 꼬리를 숨긴다. p95는 95% 지점의 값이지 가장 느린 5%의 평균이 아니다. 주어진 100건·계산법의 결과이며 측정 기간·요청 종류·실패 포함 여부·표본 대표성을 확인하지 않고 미래 전체 사용자 품질을 확정할 수 없다.
  - 예상 개념 판정: A2_FND_05_01: CORRECT, A2_FND_05_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-FND-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 두 합이 모두 2000이므로 평균은 같다. A의 95번째는20, B의 95번째는 약176.67이다. 백분위는 정렬 위치를 읽는 것이며 느린 값 여섯 개를 따로 평균한 정의가 아니다. 다음 기간·다른 요청 분포까지 보장하려면 별도 표본과 측정 조건이 필요하다.
  - 예상 개념 판정: A2_FND_05_01: CORRECT, A2_FND_05_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-FND-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 두 평균은20ms지만 p95는 A20ms, B약176.67ms다. p95는 정렬한95번째 값이라 평균만으로 꼬리를 알 수 없다. 더 측정해 보는 것이 좋다.
  - 예상 개념 판정: A2_FND_05_01: CORRECT, A2_FND_05_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-FND-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 평균이20ms로 같으므로 B의 p95도20ms다. p95는 상위5건 평균이라는 정의이며 이100건이면 미래의 모든 사용자도 같은 품질임이 확정된다.
  - 예상 개념 판정: A2_FND_05_01: INCORRECT, A2_FND_05_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-FND-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: A 평균과 p95는 20ms다. B의 합은 94×10+6×530/3=2000ms이므로 평균도 20ms지만 95번째 값은 530/3ms, 약176.67ms다. 같은 평균이 느린 꼬리를 숨긴다. p95는 95% 지점의 값이지 가장 느린 5%의 평균이 아니다. 주어진 100건·계산법의 결과이며 측정 기간·요청 종류·실패 포함 여부·표본 대표성을 확인하지 않고 미래 전체 사용자 품질을 확정할 수 없다.
  - 예상 개념 판정: A2_FND_05_01: NEEDS_REVIEW, A2_FND_05_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** nearest-rank는 이 문항의 명시적 계산 계약. 모든 관측 도구가 동일한 보간법을 쓰는 것은 아님.

**출처 대조 위치**

- [Google SRE Book — Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/) — Worrying About Your Tail (or, Instrumentation and Performance)

근거 문서: `A2-FND-DOC`. 검수 상태: **PENDING**

### A2-ARCH-01 · INTERMEDIATE

고정된 작업이 1코어에서 100초 걸린다. 그중20초는 병렬화할 수 없고80초는4코어에 완벽히 나눌 수 있다. 추가 비용은 없다. 4코어 시간·속도 향상과 코어를 무한히 늘릴 때의 한계를 설명하라.

**모범 답안**

4코어 시간은20+80/4=40초, 속도 향상은100/40=2.5배다. 직렬20초는 남으므로 코어를 무한히 늘려도 시간은20초에 접근하고 향상은5배에 접근한다. 고정 입력·직렬 비율·완벽한 분배·추가 비용 없음이라는 모델이다. 실제 동기화·스케줄링·메모리 대역폭 경쟁이 있으면 이 이상값대로 빨라진다고 보장하지 않는다.

**필수 개념**

- 직렬 구간과 병렬 속도 한계 · 가중치 0.50: 20+80/4=40초·2.5배, 무한 코어20초·5배 한계 계산.
- 성능 모델의 가정과 실측 · 가중치 0.50: 고정 입력·완벽 분배·추가 비용 없음과 실제 동기화·메모리 경쟁 구별.

**개념별 판정 경계**

- `A2_ARCH_01_01`
  - CORRECT: 20+80/4=40초·2.5배, 무한 코어20초·5배 한계 계산.
  - PARTIALLY_CORRECT: 직렬 한계는 설명하나4코어 또는 상한 계산 누락.
  - INCORRECT: 직렬 구간까지 코어 수로 나누거나 무한 향상 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ARCH_01_02`
  - CORRECT: 고정 입력·완벽 분배·추가 비용 없음과 실제 동기화·메모리 경쟁 구별.
  - PARTIALLY_CORRECT: 실측 필요성만 말하고 모델과 현실 비용 차이 누락.
  - INCORRECT: 자원 경쟁·동기화 비용이 있어도 이상값이 항상 실현된다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ARCH-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 4코어 시간은20+80/4=40초, 속도 향상은100/40=2.5배다. 직렬20초는 남으므로 코어를 무한히 늘려도 시간은20초에 접근하고 향상은5배에 접근한다. 고정 입력·직렬 비율·완벽한 분배·추가 비용 없음이라는 모델이다. 실제 동기화·스케줄링·메모리 대역폭 경쟁이 있으면 이 이상값대로 빨라진다고 보장하지 않는다.
  - 예상 개념 판정: A2_ARCH_01_01: CORRECT, A2_ARCH_01_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ARCH-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 직렬 비율0.2를 쓰면 speedup=1/(0.2+0.8/4)=2.5다. 무한 코어의 상한은1/0.2=5다. 병렬 부분만 코어로 나누며, 실측에서는 통신과 자원 경쟁의 추가 시간을 확인한다.
  - 예상 개념 판정: A2_ARCH_01_01: CORRECT, A2_ARCH_01_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ARCH-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 4코어에서40초로2.5배, 무한 코어에서20초에 접근해 최대5배다. 직렬 부분 때문에 한계가 있다. 실제 시간도 측정해야 한다.
  - 예상 개념 판정: A2_ARCH_01_01: CORRECT, A2_ARCH_01_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ARCH-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 4코어면 전체가25초라4배이며 코어를 무한히 늘리면 직렬 부분도0초가 된다. 동기화 비용이나 메모리 경쟁은 현실에서도 시간을 바꾸지 않는다.
  - 예상 개념 판정: A2_ARCH_01_01: INCORRECT, A2_ARCH_01_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ARCH-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 4코어 시간은20+80/4=40초, 속도 향상은100/40=2.5배다. 직렬20초는 남으므로 코어를 무한히 늘려도 시간은20초에 접근하고 향상은5배에 접근한다. 고정 입력·직렬 비율·완벽한 분배·추가 비용 없음이라는 모델이다. 실제 동기화·스케줄링·메모리 대역폭 경쟁이 있으면 이 이상값대로 빨라진다고 보장하지 않는다.
  - 예상 개념 판정: A2_ARCH_01_01: NEEDS_REVIEW, A2_ARCH_01_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 고정 작업량의 Amdahl 모델. 입력 자체를 늘리는 확장 모델·서비스 처리량과 구분.

**출처 대조 위치**

- [Cornell CS3410 Fall 2024 — Performance in Parallel Programming](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/parallel-perf.html) — Performance in Parallel Programming: Scalability; Amdahl’s law

근거 문서: `A2-ARCH-DOC`. 검수 상태: **PENDING**

### A2-ARCH-02 · BASIC

같은 수의 int 값을 한 번 합한다. A는 연속 메모리 배열을 앞에서부터 읽고 B는 메모리 여러 곳에 흩어진 연결리스트 노드를 따른다. 둘 다 O(n)인데도 시간이 다를 수 있는 이유와 비교할 때 확인할 조건을 설명하라.

**모범 답안**

연속 배열은 가까운 주소를 연이어 읽으므로 캐시 라인에 함께 들어온 다음 값도 활용하는 공간 지역성이 높다. 흩어진 노드는 포인터를 따라 다음 주소를 알아내며 새 캐시 라인을 읽을 수 있다. 같은 O(n)은 증가 차수만 같다는 뜻으로 캐시 미스·메모리 접근 비용까지 같지는 않다. 배치·원소 크기·캐시 상태·데이터 규모를 맞춰 미스와 시간을 측정해야 하며 특정 구현의 배열 승리를 언제나 보장하지 않는다.

**필수 개념**

- 차수와 메모리 지역성 · 가중치 0.50: O(n)의 동일 차수와 캐시 라인·공간 지역성·포인터 의존 접근 비용 차이 구별.
- 배치와 캐시 상태를 맞춘 비교 · 가중치 0.50: 노드 배치·원소 크기·캐시 상태·입력 규모를 확인하고 배열의 절대 우위 단정 거부.

**개념별 판정 경계**

- `A2_ARCH_02_01`
  - CORRECT: O(n)의 동일 차수와 캐시 라인·공간 지역성·포인터 의존 접근 비용 차이 구별.
  - PARTIALLY_CORRECT: 캐시가 다름만 말하고 지역성·방문 비용 연결 누락.
  - INCORRECT: 같은 차수는 같은 시간 또는 흩어진 노드의 높은 공간 지역성을 보장한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ARCH_02_02`
  - CORRECT: 노드 배치·원소 크기·캐시 상태·입력 규모를 확인하고 배열의 절대 우위 단정 거부.
  - PARTIALLY_CORRECT: 측정 필요성만 언급하고 통제 조건 누락.
  - INCORRECT: 배치·캐시와 무관한 절대 성능 순위 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ARCH-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 연속 배열은 가까운 주소를 연이어 읽으므로 캐시 라인에 함께 들어온 다음 값도 활용하는 공간 지역성이 높다. 흩어진 노드는 포인터를 따라 다음 주소를 알아내며 새 캐시 라인을 읽을 수 있다. 같은 O(n)은 증가 차수만 같다는 뜻으로 캐시 미스·메모리 접근 비용까지 같지는 않다. 배치·원소 크기·캐시 상태·데이터 규모를 맞춰 미스와 시간을 측정해야 하며 특정 구현의 배열 승리를 언제나 보장하지 않는다.
  - 예상 개념 판정: A2_ARCH_02_01: CORRECT, A2_ARCH_02_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ARCH-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 둘 다 n개를 방문하지만 방문당 비용이 다르다. A는 가져온 라인에 다음 원소가 있을 가능성이 높고 B는 주소 추적과 불규칙 접근 비용이 있다. 노드가 가까이 배치되거나 캐시에 전부 들어가면 차이가 줄 수 있으므로 같은 입력·환경에서 비교한다.
  - 예상 개념 판정: A2_ARCH_02_01: CORRECT, A2_ARCH_02_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ARCH-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 배열의 인접 접근은 공간 지역성으로 한 캐시 라인을 재사용하고 흩어진 노드는 더 자주 미스가 날 수 있다. 따라서 둘 다 O(n)이어도 시간은 다르다. 벤치마크로 확인한다.
  - 예상 개념 판정: A2_ARCH_02_01: CORRECT, A2_ARCH_02_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ARCH-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: O(n)이 같으면 메모리 배치와 관계없이 항상 같은 시간이 든다. 연결리스트는 모든 노드가 흩어져 있어도 공간 지역성이 배열보다 항상 높으므로 측정 조건은 필요 없다.
  - 예상 개념 판정: A2_ARCH_02_01: INCORRECT, A2_ARCH_02_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ARCH-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 연속 배열은 가까운 주소를 연이어 읽으므로 캐시 라인에 함께 들어온 다음 값도 활용하는 공간 지역성이 높다. 흩어진 노드는 포인터를 따라 다음 주소를 알아내며 새 캐시 라인을 읽을 수 있다. 같은 O(n)은 증가 차수만 같다는 뜻으로 캐시 미스·메모리 접근 비용까지 같지는 않다. 배치·원소 크기·캐시 상태·데이터 규모를 맞춰 미스와 시간을 측정해야 하며 특정 구현의 배열 승리를 언제나 보장하지 않는다.
  - 예상 개념 판정: A2_ARCH_02_01: NEEDS_REVIEW, A2_ARCH_02_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** JVM 객체의 실제 배치가 C의 연속 int 배열과 같다는 가정은 하지 않음. 문항에 저장 배치 명시.

**출처 대조 위치**

- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html) — The principle of locality: temporal/spatial locality; cache lines
- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf) — Array Sequence; Linked List Sequence: 저장 형태

근거 문서: `A2-ARCH-DOC`. 검수 상태: **PENDING**

### A2-ARCH-03 · INTERMEDIATE

단일 캐시를 순차로 확인한다. hit는 총1ns, miss는 캐시 확인1ns 뒤 메모리에서 추가50ns가 든다. hit율95%다. 평균 접근 시간은? hit율이 같아도 실제 프로그램 시간이 같지 않을 수 있는 조건을 설명하라.

**모범 답안**

평균은0.95×1+0.05×(1+50)=3.5ns다. miss의50ns는 캐시 확인 뒤 추가 비용이므로0.95×1+0.05×50=3.45ns로 계산하면 확인 비용을 빠뜨린다. 이 모델은 순차 접근의 평균이다. 실제 다층 캐시·중첩된 메모리 요청·접근 횟수·작업 종류가 다르면 같은 hit율도 전체 실행 시간을 정하지 못한다.

**필수 개념**

- 캐시 평균 비용과 추가 지연 · 가중치 0.50: 모든 확인1ns+miss5%×추가50ns=3.5ns. 총 miss 비용51ns 인식.
- 평균 접근 모델과 프로그램 시간 · 가중치 0.50: 순차 단일 캐시 모델을 다층·중첩·접근 횟수·작업 차이와 구분.

**개념별 판정 경계**

- `A2_ARCH_03_01`
  - CORRECT: 모든 확인1ns+miss5%×추가50ns=3.5ns. 총 miss 비용51ns 인식.
  - PARTIALLY_CORRECT: 가중 평균 방향만 말하고 추가 비용 해석·계산 누락.
  - INCORRECT: 추가50ns를 총 miss 비용으로 오인해3.45ns 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ARCH_03_02`
  - CORRECT: 순차 단일 캐시 모델을 다층·중첩·접근 횟수·작업 차이와 구분.
  - PARTIALLY_CORRECT: 실측 필요성만 언급하고 실행 시간 영향 조건 누락.
  - INCORRECT: hit율 하나가 모든 프로그램 완료 시간을 결정한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ARCH-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 평균은0.95×1+0.05×(1+50)=3.5ns다. miss의50ns는 캐시 확인 뒤 추가 비용이므로0.95×1+0.05×50=3.45ns로 계산하면 확인 비용을 빠뜨린다. 이 모델은 순차 접근의 평균이다. 실제 다층 캐시·중첩된 메모리 요청·접근 횟수·작업 종류가 다르면 같은 hit율도 전체 실행 시간을 정하지 못한다.
  - 예상 개념 판정: A2_ARCH_03_01: CORRECT, A2_ARCH_03_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ARCH-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 모든 접근의1ns에 miss 비율5%의 추가50ns를 더해1+2.5=3.5ns다. 같은 비율만 비교하지 말고 추가 지연·접근 수·병렬로 겹치는 요청을 함께 확인해야 프로그램 성능을 말할 수 있다.
  - 예상 개념 판정: A2_ARCH_03_01: CORRECT, A2_ARCH_03_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ARCH-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: miss도 먼저1ns 확인하므로1+0.05×50=3.5ns다. 실제 프로그램은 더 복잡하므로 측정한다.
  - 예상 개념 판정: A2_ARCH_03_01: CORRECT, A2_ARCH_03_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ARCH-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: miss의 총시간은50ns이므로3.45ns다. 같은 hit율이면 접근 횟수나 겹치는 요청이 달라도 프로그램 완료 시간은 항상 같다.
  - 예상 개념 판정: A2_ARCH_03_01: INCORRECT, A2_ARCH_03_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ARCH-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 평균은0.95×1+0.05×(1+50)=3.5ns다. miss의50ns는 캐시 확인 뒤 추가 비용이므로0.95×1+0.05×50=3.45ns로 계산하면 확인 비용을 빠뜨린다. 이 모델은 순차 접근의 평균이다. 실제 다층 캐시·중첩된 메모리 요청·접근 횟수·작업 종류가 다르면 같은 hit율도 전체 실행 시간을 정하지 못한다.
  - 예상 개념 판정: A2_ARCH_03_01: NEEDS_REVIEW, A2_ARCH_03_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 추가 miss penalty인지 miss 전체 시간인지 전제가 바뀌면 식도 달라짐.

**출처 대조 위치**

- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html) — Cache performance: average access time; multi-level caches

근거 문서: `A2-ARCH-DOC`. 검수 상태: **PENDING**

### A2-ARCH-04 · INTERMEDIATE

유효하고 읽기 가능한 페이지가 이미 RAM에 있다. 해당 가상 페이지의 주소 변환만 TLB에 없다. 하드웨어가 페이지 테이블을 조회하는 모델에서 이 접근은 반드시 디스크 읽기인가? TLB miss·데이터 캐시 miss·페이지 부재를 구별하라.

**모범 답안**

디스크 읽기가 필수인 상황이 아니다. TLB는 가상→물리 주소 변환을 캐시하므로 miss면 메모리에 있는 페이지 테이블에서 유효한 변환을 찾고 TLB를 채울 수 있다. 데이터 캐시는 해당 물리 주소의 데이터를 캐시하므로 변환 hit와 데이터 hit는 별개다. 페이지 자체가 RAM에 없는 상황의 처리는 별도다. TLB miss만으로 디스크 I/O·모든 page fault를 동일시하면 안 된다.

**필수 개념**

- 주소 변환 캐시의 miss 처리 · 가중치 0.50: TLB의 변환 책임·유효한 page table 조회·재시도 설명. 이 조건에서 디스크 필수 아님.
- 변환·데이터·페이지 부재의 경계 · 가중치 0.50: TLB hit와 데이터 hit 독립, RAM 페이지 부재는 별도 조건. fault와 디스크 읽기 일대일 단정 거부.

**개념별 판정 경계**

- `A2_ARCH_04_01`
  - CORRECT: TLB의 변환 책임·유효한 page table 조회·재시도 설명. 이 조건에서 디스크 필수 아님.
  - PARTIALLY_CORRECT: 주소 변환 캐시는 말하나 miss 복구 과정 누락.
  - INCORRECT: TLB를 데이터 캐시로 정의하거나 miss가 디스크 읽기를 필수로 만든다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ARCH_04_02`
  - CORRECT: TLB hit와 데이터 hit 독립, RAM 페이지 부재는 별도 조건. fault와 디스크 읽기 일대일 단정 거부.
  - PARTIALLY_CORRECT: 다른 miss가 다름만 언급하고 저장 대상·조건 구별 누락.
  - INCORRECT: 모든 캐시 miss·fault·디스크 읽기를 같은 사건으로 취급.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ARCH-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 디스크 읽기가 필수인 상황이 아니다. TLB는 가상→물리 주소 변환을 캐시하므로 miss면 메모리에 있는 페이지 테이블에서 유효한 변환을 찾고 TLB를 채울 수 있다. 데이터 캐시는 해당 물리 주소의 데이터를 캐시하므로 변환 hit와 데이터 hit는 별개다. 페이지 자체가 RAM에 없는 상황의 처리는 별도다. TLB miss만으로 디스크 I/O·모든 page fault를 동일시하면 안 된다.
  - 예상 개념 판정: A2_ARCH_04_01: CORRECT, A2_ARCH_04_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ARCH-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 번역 메모에 없다는 사실과 본문이 RAM에 없다는 사실은 다르다. 문항에서는 페이지 테이블을 읽어 변환을 얻고 재시도하면 된다. 그 뒤 데이터 캐시가 빗나가도 RAM에서 읽을 수 있다. 페이지 부재의 복구는 다른 조건이며 fault가 모두 디스크를 읽는 것도 아니다.
  - 예상 개념 판정: A2_ARCH_04_01: CORRECT, A2_ARCH_04_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ARCH-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: TLB는 주소 변환 캐시라 페이지 테이블을 조회해 채울 수 있다. RAM에 이미 페이지가 있으므로 TLB miss가 디스크 읽기를 뜻하지 않는다. 다른 종류의 miss는 따로 살펴본다.
  - 예상 개념 판정: A2_ARCH_04_01: CORRECT, A2_ARCH_04_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ARCH-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: TLB는 파일 데이터를 담으므로 miss면 해당 페이지가 RAM에서도 반드시 사라졌다. 데이터 캐시 miss와 모든 page fault도 같은 사건이며 언제나 디스크를 읽는다.
  - 예상 개념 판정: A2_ARCH_04_01: INCORRECT, A2_ARCH_04_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ARCH-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 디스크 읽기가 필수인 상황이 아니다. TLB는 가상→물리 주소 변환을 캐시하므로 miss면 메모리에 있는 페이지 테이블에서 유효한 변환을 찾고 TLB를 채울 수 있다. 데이터 캐시는 해당 물리 주소의 데이터를 캐시하므로 변환 hit와 데이터 hit는 별개다. 페이지 자체가 RAM에 없는 상황의 처리는 별도다. TLB miss만으로 디스크 I/O·모든 page fault를 동일시하면 안 된다.
  - 예상 개념 판정: A2_ARCH_04_01: NEEDS_REVIEW, A2_ARCH_04_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 문항은 유효·권한 허용·RAM 상주 전제. 특정 ISA의 fault 명칭·OS 정책까지 일반화하지 않음.

**출처 대조 위치**

- [OSTEP 1.10 — Chapter 19 Paging: Faster Translations (TLBs)](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-tlbs.pdf) — Chapter 19 §19.1 TLB Basic Algorithm; §19.4 TLB Contents
- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html) — Cache line and memory hierarchy: data accesses

근거 문서: `A2-ARCH-DOC`. 검수 상태: **PENDING**

### A2-ARCH-05 · INTERMEDIATE

큰 블록을 장치로 보낼 때 CPU가 상태 레지스터를 반복 조회하고 데이터도 한 단어씩 복사한다. interrupt와 DMA는 각각 어느 CPU 작업을 줄이는가? 두 기법을 쓰면 장치 자체 지연이 없어지는지 설명하라.

**모범 답안**

interrupt는 완료를 기다리며 상태를 계속 polling하는 CPU 비용을 줄이고, DMA는 설정 후 장치와 메모리 사이 데이터 이동을 담당해 CPU의 단어별 복사를 줄인다. CPU는 요청 설정·완료 처리·버퍼 수명 관리에 여전히 관여한다. 장치 지연·버스 대역폭은 남으므로 두 기법이 I/O를 즉시 완료시키지 않는다. 매우 짧은 작업·빈번한 interrupt에서는 polling이나 묶음 처리의 비용을 비교해야 한다.

**필수 개념**

- 완료 알림과 데이터 이동의 역할 · 가중치 0.50: interrupt는 polling 대기, DMA는 CPU의 단어별 복사 감소. 설정·완료 처리의 CPU 관여 유지.
- CPU 절감과 I/O 지연의 차이 · 가중치 0.50: 장치·버스 지연 유지, 짧은 작업·interrupt 빈도 등 비용 조건에서 polling·묶음 처리와 비교.

**개념별 판정 경계**

- `A2_ARCH_05_01`
  - CORRECT: interrupt는 polling 대기, DMA는 CPU의 단어별 복사 감소. 설정·완료 처리의 CPU 관여 유지.
  - PARTIALLY_CORRECT: 둘의 개선 방향은 말하나 대상 작업 또는 남는 CPU 역할 누락.
  - INCORRECT: interrupt와 DMA의 역할을 뒤집거나 CPU가 전혀 관여하지 않는다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ARCH_05_02`
  - CORRECT: 장치·버스 지연 유지, 짧은 작업·interrupt 빈도 등 비용 조건에서 polling·묶음 처리와 비교.
  - PARTIALLY_CORRECT: 장치 속도와 별개라는 방향만 말하고 비용·실패 조건 누락.
  - INCORRECT: 알림·복사 개선이 모든 장치 지연과 대역폭 한계를 제거한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ARCH-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: interrupt는 완료를 기다리며 상태를 계속 polling하는 CPU 비용을 줄이고, DMA는 설정 후 장치와 메모리 사이 데이터 이동을 담당해 CPU의 단어별 복사를 줄인다. CPU는 요청 설정·완료 처리·버퍼 수명 관리에 여전히 관여한다. 장치 지연·버스 대역폭은 남으므로 두 기법이 I/O를 즉시 완료시키지 않는다. 매우 짧은 작업·빈번한 interrupt에서는 polling이나 묶음 처리의 비용을 비교해야 한다.
  - 예상 개념 판정: A2_ARCH_05_01: CORRECT, A2_ARCH_05_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ARCH-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 완료 알림 문제와 바이트 이동 문제를 나눈다. 알림은 interrupt로, 큰 복사는 DMA로 넘겨 CPU가 다른 일을 할 여지를 만든다. 준비와 완료 처리는 남고 장치 속도는 그대로 제약이다. 모든 환경에서 두 방식이 더 빠르다고 단정하지 않고 알림 빈도와 복사 크기를 비교한다.
  - 예상 개념 판정: A2_ARCH_05_01: CORRECT, A2_ARCH_05_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ARCH-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: interrupt는 반복 상태 확인을, DMA는 CPU의 단어별 데이터 복사를 줄인다. CPU에 설정·완료 처리는 남는다. 장치가 빨라지는지는 별도로 본다.
  - 예상 개념 판정: A2_ARCH_05_01: CORRECT, A2_ARCH_05_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ARCH-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: interrupt가 큰 데이터 복사를 전부 대신하고 DMA는 상태 레지스터만 반복 조회한다. 둘을 켜면 장치 지연과 버스 한계가0이 되어 모든 I/O가 즉시 끝난다.
  - 예상 개념 판정: A2_ARCH_05_01: INCORRECT, A2_ARCH_05_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ARCH-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: interrupt는 완료를 기다리며 상태를 계속 polling하는 CPU 비용을 줄이고, DMA는 설정 후 장치와 메모리 사이 데이터 이동을 담당해 CPU의 단어별 복사를 줄인다. CPU는 요청 설정·완료 처리·버퍼 수명 관리에 여전히 관여한다. 장치 지연·버스 대역폭은 남으므로 두 기법이 I/O를 즉시 완료시키지 않는다. 매우 짧은 작업·빈번한 interrupt에서는 polling이나 묶음 처리의 비용을 비교해야 한다.
  - 예상 개념 판정: A2_ARCH_05_01: NEEDS_REVIEW, A2_ARCH_05_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** DMA가0비용이라는 결론 금지. 동시 접근·버퍼 재사용·cache coherence의 자세한 구현은 이 첫 문항 밖 범위.

**출처 대조 위치**

- [OSTEP 1.10 — Chapter 36 I/O Devices](https://pages.cs.wisc.edu/~remzi/OSTEP/file-devices.pdf) — Chapter 36 §36.3 Canonical Protocol; §36.4 Interrupts; §36.5 DMA

근거 문서: `A2-ARCH-DOC`. 검수 상태: **PENDING**

### A2-DS-01 · BASIC

n개 원소에서 인덱스 i의 값을 읽고 그 뒤에 하나를 삽입한다. 여유 용량이 있는 배열과 단일 연결리스트를 비교하라. 연결리스트는 머리만 아는 경우와 i번째 노드의 참조를 이미 가진 경우를 나누고, i는 중간 위치라고 가정한다.

**모범 답안**

배열은 인덱스 접근 O(1)이지만 중간 삽입은 뒤 원소를 옮겨야 하므로 O(n)이다. 여유 용량은 재할당을 피할 뿐 이동을 없애지 않는다. 단일 연결리스트는 머리에서 i번째 노드를 찾는 데 O(n), 찾은 노드 뒤 연결을 바꾸는 데 O(1)이다. 이미 그 노드를 알면 탐색 없이 O(1) 삽입이 가능하다. 위치 탐색과 실제 변경을 나눠 비교해야 한다.

**필수 개념**

- 배열 접근과 중간 삽입 비용 · 가중치 0.50: 배열 읽기 O(1)·중간 삽입 O(n), 여유 용량과 이동 비용 구별.
- 위치 탐색과 링크 변경의 분리 · 가중치 0.50: 머리부터 위치 탐색 O(n), 알려진 노드 뒤 링크 변경 O(1) 구별.

**개념별 판정 경계**

- `A2_DS_01_01`
  - CORRECT: 배열 읽기 O(1)·중간 삽입 O(n), 여유 용량과 이동 비용 구별.
  - PARTIALLY_CORRECT: 접근·삽입 중 하나 또는 여유 용량 조건 누락.
  - INCORRECT: 여유 용량이면 중간 삽입도 O(1)이거나 배열 읽기 O(n) 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_DS_01_02`
  - CORRECT: 머리부터 위치 탐색 O(n), 알려진 노드 뒤 링크 변경 O(1) 구별.
  - PARTIALLY_CORRECT: 링크 수정의 이점만 말하고 위치를 이미 아는 조건·탐색 비용 누락.
  - INCORRECT: 머리만 가진 리스트도 임의 위치 전체 삽입이 항상 O(1)이라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-DS-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 배열은 인덱스 접근 O(1)이지만 중간 삽입은 뒤 원소를 옮겨야 하므로 O(n)이다. 여유 용량은 재할당을 피할 뿐 이동을 없애지 않는다. 단일 연결리스트는 머리에서 i번째 노드를 찾는 데 O(n), 찾은 노드 뒤 연결을 바꾸는 데 O(1)이다. 이미 그 노드를 알면 탐색 없이 O(1) 삽입이 가능하다. 위치 탐색과 실제 변경을 나눠 비교해야 한다.
  - 예상 개념 판정: A2_DS_01_01: CORRECT, A2_DS_01_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-DS-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 배열의 주소 계산은 바로 되지만 빈칸을 만드는 이동은 남는다. 리스트의 링크 수정은 일정한 수의 포인터만 바꾸되, 머리부터 위치를 찾아야 하면 선형 시간이 든다. 알려진 노드 뒤 삽입이라는 조건에서만 리스트 삽입 O(1)을 말할 수 있다.
  - 예상 개념 판정: A2_DS_01_01: CORRECT, A2_DS_01_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-DS-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 배열은 읽기 O(1)·중간 삽입 O(n)이며 여유 용량이 있어도 이동한다. 리스트는 노드를 찾은 뒤 링크를 바꾸므로 삽입에 유리하다.
  - 예상 개념 판정: A2_DS_01_01: CORRECT, A2_DS_01_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-DS-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 여유 배열은 중간 삽입도 이동 없이 O(1)이고 인덱스 읽기는 O(n)이다. 연결리스트는 머리만 알더라도 임의 인덱스에 즉시 접근하므로 전체 삽입이 항상 O(1)이다.
  - 예상 개념 판정: A2_DS_01_01: INCORRECT, A2_DS_01_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-DS-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 배열은 인덱스 접근 O(1)이지만 중간 삽입은 뒤 원소를 옮겨야 하므로 O(n)이다. 여유 용량은 재할당을 피할 뿐 이동을 없애지 않는다. 단일 연결리스트는 머리에서 i번째 노드를 찾는 데 O(n), 찾은 노드 뒤 연결을 바꾸는 데 O(1)이다. 이미 그 노드를 알면 탐색 없이 O(1) 삽입이 가능하다. 위치 탐색과 실제 변경을 나눠 비교해야 한다.
  - 예상 개념 판정: A2_DS_01_01: NEEDS_REVIEW, A2_DS_01_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 삭제는 앞 노드 정보 등 다른 조건이 필요. 뒤 삽입의 결과를 모든 리스트 연산으로 확대하지 않음.

**출처 대조 위치**

- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf) — Array Sequence; Linked List Sequence: get_at·insert_at
- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — Linked lists: insertion and links

근거 문서: `A2-DS-DOC`. 검수 상태: **PENDING**

### A2-DS-02 · BASIC

단일 스레드 FIFO 대기열에 A,B,C를 넣고 하나를 꺼낸다. 용량4인 원형 배열에서 head와 tail이 같은 경우가 빈 상태인지 꽉 찬 상태인지 구별하려면 무엇이 필요한가? 크기 필드 또는 한 칸 비우기 정책을 허용한다.

**모범 답안**

FIFO이므로 A가 먼저 나온다. stack의 LIFO와 달리 먼저 들어온 항목을 먼저 꺼낸다. 원형 인덱스만으로 head=tail이면 빈 상태와 꽉 찬 상태가 겹칠 수 있다. size를 저장해0과4를 구별하거나 한 칸을 비워 next(tail)=head를 full로 정의할 수 있다. 한 칸 비우기를 택하면 물리 용량4의 사용 가능 용량은3이다. enqueue/dequeue 후 인덱스와 크기를 함께 갱신해야 한다.

**필수 개념**

- FIFO와 LIFO의 제거 순서 · 가중치 0.50: A,B,C 입력의 FIFO 첫 출력 A. stack의 최근 입력 우선과 구별.
- 원형 큐의 full·empty 상태 계약 · 가중치 0.50: size로0·4 구별 또는 한 칸 비우기의 usable3·full 조건 제시. 인덱스/크기 갱신 책임 설명.

**개념별 판정 경계**

- `A2_DS_02_01`
  - CORRECT: A,B,C 입력의 FIFO 첫 출력 A. stack의 최근 입력 우선과 구별.
  - PARTIALLY_CORRECT: 먼저 입력 우선만 설명하고 사례 결과 누락.
  - INCORRECT: FIFO 첫 출력 C 등 LIFO와 혼동.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_DS_02_02`
  - CORRECT: size로0·4 구별 또는 한 칸 비우기의 usable3·full 조건 제시. 인덱스/크기 갱신 책임 설명.
  - PARTIALLY_CORRECT: 크기·빈칸 정책만 제시하고 사용 가능 용량·조건·갱신 책임 누락.
  - INCORRECT: head=tail만으로 양 상태를 구별하거나 빈칸 정책도4개 저장 가능 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-DS-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: FIFO이므로 A가 먼저 나온다. stack의 LIFO와 달리 먼저 들어온 항목을 먼저 꺼낸다. 원형 인덱스만으로 head=tail이면 빈 상태와 꽉 찬 상태가 겹칠 수 있다. size를 저장해0과4를 구별하거나 한 칸을 비워 next(tail)=head를 full로 정의할 수 있다. 한 칸 비우기를 택하면 물리 용량4의 사용 가능 용량은3이다. enqueue/dequeue 후 인덱스와 크기를 함께 갱신해야 한다.
  - 예상 개념 판정: A2_DS_02_01: CORRECT, A2_DS_02_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-DS-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 출력은 A다. 물리 배열을 돌아 쓰는 것과 FIFO 순서는 별개다. count=0이면 empty, count=4이면 full로 정의하면 같은 인덱스도 구별된다. reserved-slot 설계도 가능하지만4칸 중3칸만 사용한다. 넣거나 꺼낼 때 인덱스와 count를 같은 상태 변경으로 갱신한다.
  - 예상 개념 판정: A2_DS_02_01: CORRECT, A2_DS_02_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-DS-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: FIFO에서는 A가 먼저 나온다. head=tail만으로 빈 상태와 full을 구별할 수 없으므로 크기를 따로 저장하거나 한 칸을 비워 두면 된다.
  - 예상 개념 판정: A2_DS_02_01: CORRECT, A2_DS_02_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-DS-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: FIFO에서도 마지막 C가 먼저 나온다. head=tail이면 무조건 empty이며 한 칸을 비워도4칸 모두 사용할 수 있어 full 상태 표시는 필요 없다.
  - 예상 개념 판정: A2_DS_02_01: INCORRECT, A2_DS_02_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-DS-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: FIFO이므로 A가 먼저 나온다. stack의 LIFO와 달리 먼저 들어온 항목을 먼저 꺼낸다. 원형 인덱스만으로 head=tail이면 빈 상태와 꽉 찬 상태가 겹칠 수 있다. size를 저장해0과4를 구별하거나 한 칸을 비워 next(tail)=head를 full로 정의할 수 있다. 한 칸 비우기를 택하면 물리 용량4의 사용 가능 용량은3이다. enqueue/dequeue 후 인덱스와 크기를 함께 갱신해야 한다.
  - 예상 개념 판정: A2_DS_02_01: NEEDS_REVIEW, A2_DS_02_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 멀티스레드의 visibility·경쟁·lock-free 보장은 이 문항의 단일 스레드 계약 밖.

**출처 대조 위치**

- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — FIFO queues; Array resizing: queue. 원형 상태 정책은 문항에서 허용한 설계

근거 문서: `A2-DS-DOC`. 검수 상태: **PENDING**

### A2-DS-03 · INTERMEDIATE

별도 연결리스트로 충돌을 처리하는 해시 테이블에 서로 다른 키 n개를 넣는다. 모든 키가 같은 bucket에 들어가고, 키 비교와 해시 계산은 O(1)이다. 해시가 같다는 이유로 앞 값을 덮어써도 되는가? 없는 키 조회 비용과 배열 확장만으로 해결되는지 설명하라.

**모범 답안**

같은 해시와 같은 키는 다르다. bucket 안에서 동등성 비교로 실제 키를 구별하고, 다른 키의 값을 덮어쓰면 안 된다. 없는 키가 같은 bucket에 대응하면 n개를 모두 비교하므로 최악 O(n)이다. 평균 O(1)은 분산과 load factor 등의 조건에 의존한다. 원래 hash 값 자체가 모두 같다면 배열을 크게 해도 같은 bucket으로 모이므로 확장만으로 해소되지 않는다. 해시 분산·충돌 정책을 함께 확인한다.

**필수 개념**

- 충돌과 키 동등성의 구별 · 가중치 0.50: 같은 hash인 다른 키를 보존하고 bucket 안의 실제 키 비교로 식별.
- 해시 조회 비용의 분산 조건 · 가중치 0.50: 한 bucket absent lookup O(n), 평균 O(1)의 분산/적재율 조건·동일 hash에 확장만으로 부족 설명.

**개념별 판정 경계**

- `A2_DS_03_01`
  - CORRECT: 같은 hash인 다른 키를 보존하고 bucket 안의 실제 키 비교로 식별.
  - PARTIALLY_CORRECT: 충돌 처리는 언급하나 동등성 판단·보존 이유 누락.
  - INCORRECT: hash 일치를 키 동일성으로 취급해 다른 값을 덮어쓰기.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_DS_03_02`
  - CORRECT: 한 bucket absent lookup O(n), 평균 O(1)의 분산/적재율 조건·동일 hash에 확장만으로 부족 설명.
  - PARTIALLY_CORRECT: 최악 O(n)은 설명하나 동일 hash 확장의 한계·평균 조건 누락.
  - INCORRECT: 동일 hash 몰림에서도 capacity만 늘리면 항상 O(1) 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-DS-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 같은 해시와 같은 키는 다르다. bucket 안에서 동등성 비교로 실제 키를 구별하고, 다른 키의 값을 덮어쓰면 안 된다. 없는 키가 같은 bucket에 대응하면 n개를 모두 비교하므로 최악 O(n)이다. 평균 O(1)은 분산과 load factor 등의 조건에 의존한다. 원래 hash 값 자체가 모두 같다면 배열을 크게 해도 같은 bucket으로 모이므로 확장만으로 해소되지 않는다. 해시 분산·충돌 정책을 함께 확인한다.
  - 예상 개념 판정: A2_DS_03_01: CORRECT, A2_DS_03_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-DS-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 충돌한 키는 목록에 함께 두고 equals에 해당하는 비교로 찾는다. 한 bucket에 n개면 absent lookup도 n번 검사할 수 있다. capacity를 늘려도 동일 hash를 가진 키의 구분 정보가 새로 생기지 않으므로 좋은 분산과 적절한 적재율이 필요하다.
  - 예상 개념 판정: A2_DS_03_01: CORRECT, A2_DS_03_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-DS-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 해시가 같은 다른 키는 따로 보관하고 실제 키를 비교해야 한다. 한 bucket의 없는 키 조회는 n개 비교로 O(n)이다. 확장과 해시 함수를 검토한다.
  - 예상 개념 판정: A2_DS_03_01: CORRECT, A2_DS_03_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-DS-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 해시가 같으면 키도 같으므로 기존 값을 덮어쓰는 것이 맞다. 모든 키가 같은 원래 hash를 가져도 capacity만 늘리면 없는 키 조회가 무조건 O(1)이 된다.
  - 예상 개념 판정: A2_DS_03_01: INCORRECT, A2_DS_03_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-DS-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 같은 해시와 같은 키는 다르다. bucket 안에서 동등성 비교로 실제 키를 구별하고, 다른 키의 값을 덮어쓰면 안 된다. 없는 키가 같은 bucket에 대응하면 n개를 모두 비교하므로 최악 O(n)이다. 평균 O(1)은 분산과 load factor 등의 조건에 의존한다. 원래 hash 값 자체가 모두 같다면 배열을 크게 해도 같은 bucket으로 모이므로 확장만으로 해소되지 않는다. 해시 분산·충돌 정책을 함께 확인한다.
  - 예상 개념 판정: A2_DS_03_01: NEEDS_REVIEW, A2_DS_03_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** Java HashMap의 treeification 계약을 이 문항의 연결리스트 구현에 가져와 O(log n)으로 대체하지 않음.

**출처 대조 위치**

- [Princeton Algorithms 4e — Hash Tables](https://algs4.cs.princeton.edu/34hash/) — Hash functions: consistency; Assumption J; Hashing with separate chaining

근거 문서: `A2-DS-DOC`. 검수 상태: **PENDING**

### A2-DS-04 · INTERMEDIATE

서로 다른 점수 8,2,6,10,4가 차례로 온다. 가장 큰3개만 보관하려고 용량을 미리 확보한 min-heap을 쓴다. 최종 점수 집합·루트의 의미·새 점수 처리와 비용을 설명하라. 힙 배열이 전체 오름차순인지도 답하라.

**모범 답안**

최종 집합은{6,8,10}이고 루트6은 보관한 큰3개 중 가장 작다. 처음3개는 넣고, 이후 루트보다 큰10은2를 교체하며4는6 이하라 버린다. 가득 찬 뒤 루트 비교는 O(1), 교체 후 복구는 O(log K)이고 저장은 O(K)다. min-heap은 부모≤자식 조건으로 루트 최소를 보장할 뿐 형제·전체 배열 정렬을 보장하지 않는다. 내림차순 출력은 별도 정렬이나 반복 추출이 필요하다.

**필수 개념**

- 상위 K개의 최소 경계 유지 · 가중치 0.50: 루트는 선택 집합 최소, 새 값과 비교·교체하여{6,8,10}. 비교O(1)·복구O(log K)·저장O(K).
- 힙 순서와 전체 정렬의 경계 · 가중치 0.50: 부모≤자식의 부분 순서·전체 정렬 불보장, 출력용 추가 작업 구별.

**개념별 판정 경계**

- `A2_DS_04_01`
  - CORRECT: 루트는 선택 집합 최소, 새 값과 비교·교체하여{6,8,10}. 비교O(1)·복구O(log K)·저장O(K).
  - PARTIALLY_CORRECT: 유지 방향은 맞으나 집합·비용 일부 누락.
  - INCORRECT: min-heap 루트를 최대값으로 취급하거나 작은 값만 유지.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_DS_04_02`
  - CORRECT: 부모≤자식의 부분 순서·전체 정렬 불보장, 출력용 추가 작업 구별.
  - PARTIALLY_CORRECT: 출력 가능성만 말하고 정렬 불보장·추가 작업 누락.
  - INCORRECT: 힙 배열은 항상 오름차순이거나 복구 없이 일정 비용 교체 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-DS-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 최종 집합은{6,8,10}이고 루트6은 보관한 큰3개 중 가장 작다. 처음3개는 넣고, 이후 루트보다 큰10은2를 교체하며4는6 이하라 버린다. 가득 찬 뒤 루트 비교는 O(1), 교체 후 복구는 O(log K)이고 저장은 O(K)다. min-heap은 부모≤자식 조건으로 루트 최소를 보장할 뿐 형제·전체 배열 정렬을 보장하지 않는다. 내림차순 출력은 별도 정렬이나 반복 추출이 필요하다.
  - 예상 개념 판정: A2_DS_04_01: CORRECT, A2_DS_04_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-DS-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 큰 값 집합의 탈락 경계만 빠르게 보려면 작은 경계를 루트에 둔다. 10이2를 밀어내고4는 탈락해6,8,10이 남는다. preallocated heap의 경로 복구는 log K, 비교만 하면 상수다. [6,10,8]도 유효한 min-heap이므로 배열 순회가 정렬 출력은 아니다. 용량이 찬 뒤 비교는 O(1), 교체 복구는 O(log K), 저장은 O(K)다.
  - 예상 개념 판정: A2_DS_04_01: CORRECT, A2_DS_04_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-DS-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: min-heap 루트는 현재 상위3개의 탈락 경계다. 10으로2를 바꾸고4는 버려{6,8,10}이 남는다. 비교 O(1)·교체 O(log K)·저장 O(K)다. 힙으로 결과를 출력할 수 있다.
  - 예상 개념 판정: A2_DS_04_01: CORRECT, A2_DS_04_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-DS-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 가장 큰3개를 보관하려면 min-heap 루트가 전체 최대10이어야 한다. min-heap의 모든 배열은 오름차순 정렬되어 있어 교체도 복구 없이 O(1)이다.
  - 예상 개념 판정: A2_DS_04_01: INCORRECT, A2_DS_04_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-DS-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 최종 집합은{6,8,10}이고 루트6은 보관한 큰3개 중 가장 작다. 처음3개는 넣고, 이후 루트보다 큰10은2를 교체하며4는6 이하라 버린다. 가득 찬 뒤 루트 비교는 O(1), 교체 후 복구는 O(log K)이고 저장은 O(K)다. min-heap은 부모≤자식 조건으로 루트 최소를 보장할 뿐 형제·전체 배열 정렬을 보장하지 않는다. 내림차순 출력은 별도 정렬이나 반복 추출이 필요하다.
  - 예상 개념 판정: A2_DS_04_01: NEEDS_REVIEW, A2_DS_04_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** K≥1·서로 다른 점수·용량 확보 전제. 동점 처리와 배열 재할당 비용은 별도.

**출처 대조 위치**

- [Princeton Algorithms 4e — Priority Queues](https://algs4.cs.princeton.edu/24pq/) — Heap definitions; heap-order maintenance; TopM client; resizing cost caveat

근거 문서: `A2-DS-DOC`. 검수 상태: **PENDING**

### A2-DS-05 · INTERMEDIATE

단순 무방향 그래프의 정점은1000개, 간선은2000개다. 인접 리스트와1000×1000 인접 행렬을 비교하라. 저장 규모와 한 정점의 모든 이웃 열거 비용, 특정 두 정점의 연결 검사 비용이 왜 다른가?

**모범 답안**

리스트는 정점1000개와 양방향 간선 항목4000개 등 O(V+E) 공간을 쓰고, 한 정점의 이웃 열거는 O(degree(v))다. 행렬은100만 칸의 O(V^2) 공간과 이웃 열거 O(V)를 쓴다. 반면 특정 연결은 행렬에서 O(1)이고 정렬되지 않은 리스트는 해당 이웃 목록을 훑어 O(degree(v))다. 이 희소 그래프의 전체 순회에는 리스트가 적합할 수 있지만 연결 검사 빈도·추가 인덱스·밀도를 함께 보고 선택한다.

**필수 개념**

- 그래프 표현과 이웃 열거 규모 · 가중치 0.50: 무방향 리스트2E·O(V+E)와 행렬V^2·O(V^2), 열거 O(degree)와 O(V) 구별.
- 연산 요구에 따른 표현 선택 · 가중치 0.50: 연결 검사 행렬O(1)·일반 리스트O(degree), 질의 빈도·밀도·추가 인덱스에 따른 대안 허용.

**개념별 판정 경계**

- `A2_DS_05_01`
  - CORRECT: 무방향 리스트2E·O(V+E)와 행렬V^2·O(V^2), 열거 O(degree)와 O(V) 구별.
  - PARTIALLY_CORRECT: 희소성 이점만 말하고2E·열거 비용 일부 누락.
  - INCORRECT: 리스트와 행렬 저장 규모를 뒤집거나 행렬이E칸이라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_DS_05_02`
  - CORRECT: 연결 검사 행렬O(1)·일반 리스트O(degree), 질의 빈도·밀도·추가 인덱스에 따른 대안 허용.
  - PARTIALLY_CORRECT: 용도에 따른 선택만 말하고 연결 검사 비용·조건 누락.
  - INCORRECT: 일반 리스트의 특정 연결 검사도 항상O(1) 또는 모든 상황에서 한 표현만 유효 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-DS-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 리스트는 정점1000개와 양방향 간선 항목4000개 등 O(V+E) 공간을 쓰고, 한 정점의 이웃 열거는 O(degree(v))다. 행렬은100만 칸의 O(V^2) 공간과 이웃 열거 O(V)를 쓴다. 반면 특정 연결은 행렬에서 O(1)이고 정렬되지 않은 리스트는 해당 이웃 목록을 훑어 O(degree(v))다. 이 희소 그래프의 전체 순회에는 리스트가 적합할 수 있지만 연결 검사 빈도·추가 인덱스·밀도를 함께 보고 선택한다.
  - 예상 개념 판정: A2_DS_05_01: CORRECT, A2_DS_05_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-DS-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 무방향 간선 하나가 두 이웃 항목이 되므로 리스트에는4000개 간선 항목이 있다. 행렬의100만 칸은 byte 수와 같은 말은 아니다. 목록은 실제 이웃만 읽고 행렬은 행 전체를 읽는다. 랜덤 연결 질의가 중심이라면 행렬의 상수 접근이나 집합을 붙인 목록도 비교할 수 있다. 리스트 공간은 O(V+E), 행렬은 O(V²)이며 이웃 열거는 각각 O(degree(v)), O(V)다. 일반 목록의 연결 질의는 O(degree(v)), 행렬은 O(1)이다.
  - 예상 개념 판정: A2_DS_05_01: CORRECT, A2_DS_05_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-DS-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 리스트는 O(V+E)·4000개 간선 항목, 행렬은 O(V^2)·100만 칸이다. 이웃을 전부 열거할 때 목록은 O(degree), 행렬은 O(V)다. 용도에 맞게 선택한다.
  - 예상 개념 판정: A2_DS_05_01: CORRECT, A2_DS_05_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-DS-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 간선 수가 작아도 행렬은2000칸만 저장하고 리스트는100만 칸을 항상 쓴다. 정렬·해시가 없는 이웃 리스트의 임의 연결 검사도 언제나 O(1)이다.
  - 예상 개념 판정: A2_DS_05_01: INCORRECT, A2_DS_05_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-DS-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 리스트는 정점1000개와 양방향 간선 항목4000개 등 O(V+E) 공간을 쓰고, 한 정점의 이웃 열거는 O(degree(v))다. 행렬은100만 칸의 O(V^2) 공간과 이웃 열거 O(V)를 쓴다. 반면 특정 연결은 행렬에서 O(1)이고 정렬되지 않은 리스트는 해당 이웃 목록을 훑어 O(degree(v))다. 이 희소 그래프의 전체 순회에는 리스트가 적합할 수 있지만 연결 검사 빈도·추가 인덱스·밀도를 함께 보고 선택한다.
  - 예상 개념 판정: A2_DS_05_01: NEEDS_REVIEW, A2_DS_05_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 행렬의 칸 수는 실제 메모리 byte 수가 아님. self-loop·평행 간선은 문항에서 제외.

**출처 대조 위치**

- [Princeton Algorithms 4e — Undirected Graphs](https://algs4.cs.princeton.edu/41graph/) — Graph representations: adjacency matrix and adjacency lists

근거 문서: `A2-DS-DOC`. 검수 상태: **PENDING**

### A2-ALG-01 · INTERMEDIATE

오름차순 배열[2,4,4,4,9]에서 값4의 첫 인덱스를0부터 찾는다. lower_bound를 반열린 구간[lo,hi)로 구현할 때 같은 값을 만나면 어디를 줄이는가? 없는 값·빈 배열을 처리하는 조건과 정렬되지 않은 입력의 문제를 설명하라.

**모범 답안**

lo=0, hi=n에서 mid 값을 비교한다. a[mid]<target이면 lo=mid+1, 그렇지 않으면 hi=mid로 줄여 첫 target 이상 위치를 찾는다. 같다고 즉시 반환하면 뒤의4를 반환할 수 있다. 종료 후 lo<n이고 a[lo]==target이면 이 배열에서1을 반환하고, 아니면 없음이다. 빈 배열은 처음부터 lo=hi=0이라 접근하지 않는다. 이 논리는 정렬된 값의 단조성에 의존하므로 무정렬 입력에는 그대로 적용할 수 없다.

**필수 개념**

- 단조성에 근거한 구간 축소 · 가중치 0.50: 오름차순 전제·반열린 구간에서 <면 lo=mid+1, 그 외hi=mid 설명.
- 첫 일치와 종료 경계 · 가중치 0.50: 동일 값 즉시 반환 금지·종료lo=1, lo<n 및 값 동일성 확인. 빈 배열 접근 없음.

**개념별 판정 경계**

- `A2_ALG_01_01`
  - CORRECT: 오름차순 전제·반열린 구간에서 <면 lo=mid+1, 그 외hi=mid 설명.
  - PARTIALLY_CORRECT: 이진 탐색 방향만 말하고 정렬·구간 유지 규칙 누락.
  - INCORRECT: 무정렬에도 동일한 절반 제거가 정당하다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ALG_01_02`
  - CORRECT: 동일 값 즉시 반환 금지·종료lo=1, lo<n 및 값 동일성 확인. 빈 배열 접근 없음.
  - PARTIALLY_CORRECT: 왼쪽 경계는 찾으나 빈·없음·인덱스 한계 확인 누락.
  - INCORRECT: 일치 즉시 반환이 첫 위치 보장 또는 빈/끝 인덱스 접근 안전 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ALG-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: lo=0, hi=n에서 mid 값을 비교한다. a[mid]<target이면 lo=mid+1, 그렇지 않으면 hi=mid로 줄여 첫 target 이상 위치를 찾는다. 같다고 즉시 반환하면 뒤의4를 반환할 수 있다. 종료 후 lo<n이고 a[lo]==target이면 이 배열에서1을 반환하고, 아니면 없음이다. 빈 배열은 처음부터 lo=hi=0이라 접근하지 않는다. 이 논리는 정렬된 값의 단조성에 의존하므로 무정렬 입력에는 그대로 적용할 수 없다.
  - 예상 개념 판정: A2_ALG_01_01: CORRECT, A2_ALG_01_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ALG-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 동일 값도 왼쪽 경계를 더 찾아야 하므로 오른쪽 끝을 mid로 옮긴다. 첫≥4가 인덱스1이며 실제로4인지 확인한다. 없는 값은 삽입 위치만 얻을 수 있고 n 위치를 접근하면 안 된다. 무정렬이라면 먼저 정렬하거나 선형으로 첫 위치를 찾되 원래 인덱스의 의미를 보존한다.
  - 예상 개념 판정: A2_ALG_01_01: CORRECT, A2_ALG_01_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ALG-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 정렬된 값의 단조성을 이용한다. 반열린 구간에서 작은 값이면 lo=mid+1, 같거나 크면 hi=mid로 줄이면 이 배열의 첫4는1이다.
  - 예상 개념 판정: A2_ALG_01_01: CORRECT, A2_ALG_01_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ALG-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 정렬 여부와 관계없이 가운데 절반을 버려도 된다. 같은4를 만나면 즉시 반환하면 언제나 첫 인덱스이며 빈 배열에서도 a[0]을 읽어 종료하면 안전하다.
  - 예상 개념 판정: A2_ALG_01_01: INCORRECT, A2_ALG_01_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ALG-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: lo=0, hi=n에서 mid 값을 비교한다. a[mid]<target이면 lo=mid+1, 그렇지 않으면 hi=mid로 줄여 첫 target 이상 위치를 찾는다. 같다고 즉시 반환하면 뒤의4를 반환할 수 있다. 종료 후 lo<n이고 a[lo]==target이면 이 배열에서1을 반환하고, 아니면 없음이다. 빈 배열은 처음부터 lo=hi=0이라 접근하지 않는다. 이 논리는 정렬된 값의 단조성에 의존하므로 무정렬 입력에는 그대로 적용할 수 없다.
  - 예상 개념 판정: A2_ALG_01_01: NEEDS_REVIEW, A2_ALG_01_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 선행 정렬의 시간과 원래 순서 보존 문제는 탐색 O(log n)과 별도. 정렬로 원래 첫 위치를 바꾸지 않도록 주의.

**출처 대조 위치**

- [Princeton Algorithms 4e — Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/) — Binary search examples. lower_bound 규칙·반열린 구간은 문항의 알고리즘 계약

근거 문서: `A2-ALG-DOC`. 검수 상태: **PENDING**

### A2-ALG-02 · INTERMEDIATE

연속한 두 반쪽을 재귀 정렬 후 O(n)에 병합하는 배열 merge sort다. 별도 O(n) 버퍼를 재사용한다. 점수만 비교하는 입력[(3,A),(1,B),(3,C),(1,D)]에서 동점의 입력 순서를 보존하려면 병합 중 동점일 때 어느 쪽을 먼저 가져오는가? 시간·추가 공간도 설명하라.

**모범 답안**

반쪽 둘을 정렬하는 비용과 선형 병합이 반복되어 O(n log n) 시간, 재사용 버퍼 O(n)과 재귀 O(log n)의 추가 공간이 든다. 양쪽이 내부 동점 순서를 유지한다면 동점에서 왼쪽을 먼저 가져와 전체 입력 순서를 보존한다. 결과는[(1,B),(1,D),(3,A),(3,C)]다. 오른쪽을 먼저 고르면 원래 뒤의 항목이 앞 항목을 넘어갈 수 있으므로 점수 정렬만으로 안정성을 보장하지 않는다.

**필수 개념**

- 분할·병합의 시간과 공간 · 가중치 0.50: 깊이log n·깊이당n으로 O(n log n), 재사용 버퍼O(n)·호출 스택O(log n).
- 정렬 안정성과 동점 처리 · 가중치 0.50: 내부 안정성+동점 왼쪽 우선, 결과1:B,D·3:A,C, 오른쪽 우선 반례 설명.

**개념별 판정 경계**

- `A2_ALG_02_01`
  - CORRECT: 깊이log n·깊이당n으로 O(n log n), 재사용 버퍼O(n)·호출 스택O(log n).
  - PARTIALLY_CORRECT: 시간 또는 버퍼 비용만 설명하고 구성 이유 누락.
  - INCORRECT: 전체O(log n)·버퍼 추가O(1) 등 병합/저장 비용 생략.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ALG_02_02`
  - CORRECT: 내부 안정성+동점 왼쪽 우선, 결과1:B,D·3:A,C, 오른쪽 우선 반례 설명.
  - PARTIALLY_CORRECT: 왼쪽 우선만 제시하고 결과·순서 보존 이유 누락.
  - INCORRECT: 오른쪽 우선도 항상 입력 순서를 유지한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ALG-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 반쪽 둘을 정렬하는 비용과 선형 병합이 반복되어 O(n log n) 시간, 재사용 버퍼 O(n)과 재귀 O(log n)의 추가 공간이 든다. 양쪽이 내부 동점 순서를 유지한다면 동점에서 왼쪽을 먼저 가져와 전체 입력 순서를 보존한다. 결과는[(1,B),(1,D),(3,A),(3,C)]다. 오른쪽을 먼저 고르면 원래 뒤의 항목이 앞 항목을 넘어갈 수 있으므로 점수 정렬만으로 안정성을 보장하지 않는다.
  - 예상 개념 판정: A2_ALG_02_01: CORRECT, A2_ALG_02_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ALG-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 각 깊이에서 총 n개를 병합하고 깊이가 log n이라 n log n이다. 한 버퍼를 돌려 쓰므로 추가 저장은 n이다. 같은 키면 먼저 등장한 왼쪽 항목을 먼저 출력하면1의 B,D와3의 A,C 순서가 남는다. 다른 정렬도 같은 안정성·비용 계약을 만족하면 대안이다. 재귀 호출 스택은 O(log n)이며 버퍼를 포함한 전체 추가 공간은 O(n)이다.
  - 예상 개념 판정: A2_ALG_02_01: CORRECT, A2_ALG_02_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ALG-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 각 깊이 O(n) 작업과 O(log n) 깊이로 시간 O(n log n)이다. 버퍼O(n), 호출 스택O(log n)이 필요하다. 동점에서는 왼쪽을 먼저 골라야 한다.
  - 예상 개념 판정: A2_ALG_02_01: CORRECT, A2_ALG_02_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ALG-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 두 반쪽이라도 전체 시간은 O(log n)이고 버퍼를 쓰므로 추가 공간은 O(1)이다. 동점에서 오른쪽부터 가져와도 원래 B,D·A,C 순서가 언제나 그대로 보존된다.
  - 예상 개념 판정: A2_ALG_02_01: INCORRECT, A2_ALG_02_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ALG-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 반쪽 둘을 정렬하는 비용과 선형 병합이 반복되어 O(n log n) 시간, 재사용 버퍼 O(n)과 재귀 O(log n)의 추가 공간이 든다. 양쪽이 내부 동점 순서를 유지한다면 동점에서 왼쪽을 먼저 가져와 전체 입력 순서를 보존한다. 결과는[(1,B),(1,D),(3,A),(3,C)]다. 오른쪽을 먼저 고르면 원래 뒤의 항목이 앞 항목을 넘어갈 수 있으므로 점수 정렬만으로 안정성을 보장하지 않는다.
  - 예상 개념 판정: A2_ALG_02_01: NEEDS_REVIEW, A2_ALG_02_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 이 문항의 배열+버퍼 모델. 리스트 merge sort 등 다른 공간 모델을 같은 것으로 단정하지 않음.

**출처 대조 위치**

- [Princeton Algorithms 4e — Mergesort](https://algs4.cs.princeton.edu/22mergesort/) — Abstract in-place merge; Top-down mergesort; Proposition; Faster merge의 비안정 변형

근거 문서: `A2-ALG-DOC`. 검수 상태: **PENDING**

### A2-ALG-03 · INTERMEDIATE

인접 리스트 그래프에서 BFS로 시작점의 최단 경로를 찾는다. 모든 간선 비용이1일 때 queue와 방문 표시는 어떻게 쓰는가? A→D 비용10, A→B 비용1, B→D 비용1인 다른 그래프에서도 같은 BFS가 최소 비용을 찾는지 설명하라.

**모범 답안**

BFS는 시작점을 enqueue 때 방문 표시하고, dequeue한 정점의 미방문 이웃도 enqueue 때 표시해 단계별로 확장한다. 모두 비용1이면 처음 발견한 단계 수가 최소 비용이자 최소 간선 수다. 인접 리스트 전체 탐색은 O(V+E)다. 주어진 가중 그래프는 직접 A→D가 한 간선이지만 비용10, A→B→D는 두 간선이지만 비용2다. 일반 BFS는 최소 간선 수를 찾으므로 가변 비용의 최소 합을 보장하지 않는다. 비음수 가중치에는 Dijkstra 등의 조건 맞는 대안을 선택한다.

**필수 개념**

- 단위 간선 BFS의 단계와 방문 · 가중치 0.50: queue의 단계 순서·enqueue 방문 표시·O(V+E), 단위 비용 최단 보장 설명.
- 최소 간선 수와 가중 비용의 경계 · 가중치 0.50: 직접10과 두 간선2 반례, 비음수 Dijkstra 등 조건 맞는 대안 허용.

**개념별 판정 경계**

- `A2_ALG_03_01`
  - CORRECT: queue의 단계 순서·enqueue 방문 표시·O(V+E), 단위 비용 최단 보장 설명.
  - PARTIALLY_CORRECT: queue 사용은 말하나 표시 시점·단계/비용 관계 누락.
  - INCORRECT: stack/무방문 재삽입이 BFS의 최단 보장이라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ALG_03_02`
  - CORRECT: 직접10과 두 간선2 반례, 비음수 Dijkstra 등 조건 맞는 대안 허용.
  - PARTIALLY_CORRECT: 가중치 차이만 언급하고 구체 반례·적용 조건 누락.
  - INCORRECT: 가변 가중치에서도 일반 BFS의 첫 발견이 항상 최소 비용이라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ALG-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: BFS는 시작점을 enqueue 때 방문 표시하고, dequeue한 정점의 미방문 이웃도 enqueue 때 표시해 단계별로 확장한다. 모두 비용1이면 처음 발견한 단계 수가 최소 비용이자 최소 간선 수다. 인접 리스트 전체 탐색은 O(V+E)다. 주어진 가중 그래프는 직접 A→D가 한 간선이지만 비용10, A→B→D는 두 간선이지만 비용2다. 일반 BFS는 최소 간선 수를 찾으므로 가변 비용의 최소 합을 보장하지 않는다. 비음수 가중치에는 Dijkstra 등의 조건 맞는 대안을 선택한다.
  - 예상 개념 판정: A2_ALG_03_01: CORRECT, A2_ALG_03_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ALG-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 큐가 거리0,1,2 순서를 유지하며 넣을 때 표시해 중복 확장을 막는다. 단위 가중치라면 V+E 작업으로 최단을 얻는다. 두 번째 그래프에서 한 단계로 D를 잡는 결과는 비용10이므로 비용2의 두 단계보다 나쁘다. 단계 순서와 비용 순서가 다르면 알고리즘을 바꿔야 한다.
  - 예상 개념 판정: A2_ALG_03_01: CORRECT, A2_ALG_03_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ALG-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 단위 비용에서는 queue의 단계 순서와 enqueue 방문 표시로 최소 간선 수가 최소 비용이다. 인접 리스트 탐색은 O(V+E)다. 가중치가 다르면 다른 알고리즘을 고려한다.
  - 예상 개념 판정: A2_ALG_03_01: CORRECT, A2_ALG_03_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ALG-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: BFS는 stack으로 깊게 먼저 가고 같은 정점을 계속 enqueue해야 최단을 찾는다. 가중치가 달라도 첫 D가 언제나 최소 비용이므로 비용10이 비용2보다 최적이다.
  - 예상 개념 판정: A2_ALG_03_01: INCORRECT, A2_ALG_03_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ALG-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: BFS는 시작점을 enqueue 때 방문 표시하고, dequeue한 정점의 미방문 이웃도 enqueue 때 표시해 단계별로 확장한다. 모두 비용1이면 처음 발견한 단계 수가 최소 비용이자 최소 간선 수다. 인접 리스트 전체 탐색은 O(V+E)다. 주어진 가중 그래프는 직접 A→D가 한 간선이지만 비용10, A→B→D는 두 간선이지만 비용2다. 일반 BFS는 최소 간선 수를 찾으므로 가변 비용의 최소 합을 보장하지 않는다. 비음수 가중치에는 Dijkstra 등의 조건 맞는 대안을 선택한다.
  - 예상 개념 판정: A2_ALG_03_01: NEEDS_REVIEW, A2_ALG_03_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 방문을 늦게 표시하는 변형도 올바른 중복 방어·거리 계약을 설명하면 검수 가능. 이름만으로 감점 금지.

**출처 대조 위치**

- [Princeton Algorithms 4e — Undirected Graphs](https://algs4.cs.princeton.edu/41graph/) — Breadth-first search: shortest path in number of edges; marked queue

근거 문서: `A2-ALG-DOC`. 검수 상태: **PENDING**

### A2-ALG-04 · INTERMEDIATE

동전1,3,4를 제한 없이 써 금액6을 최소 동전 수로 만든다. 남은 금액 이하의 가장 큰 동전부터 고르는 greedy가 최적인가? 이 입력의 반례와 모든 금액0..6에 대해 답을 만드는 DP 상태·초기값·전이를 설명하라. 시간·공간 비용도 설명하라.

**모범 답안**

greedy는4+1+1로3개지만3+3은2개이므로 이 체계에서 최적이 아니다. dp[x]를 금액x의 최소 개수로 정의하고 dp[0]=0, 아직 만들 수 없는 값은∞로 둔다. x=1부터6까지 dp[x]=min(dp[x-c]+1)로, c∈{1,3,4} 중 c≤x인 경우를 검사한다. 이전 최소 답에 동전 하나를 붙여 모든 마지막 선택을 비교하므로 dp[6]=2다. 양의 정수 동전·무제한 사용 전제이며 시간 O(A×m)·공간 O(A). 특정 화폐의 greedy 성질을 모든 동전에 일반화하지 않는다.

**필수 개념**

- greedy 최적성의 반례 · 가중치 0.50: 4+1+1의3개와3+3의2개를 비교하며 동전 체계별 최적성 전제 구별.
- DP 상태·전이·기준 조건 · 가중치 0.50: dp[x] 최소 개수·dp[0]=0·∞·양의c≤x 전이·모든 마지막 선택 비교, O(A m)/O(A) 또는 동등 memoization.

**개념별 판정 경계**

- `A2_ALG_04_01`
  - CORRECT: 4+1+1의3개와3+3의2개를 비교하며 동전 체계별 최적성 전제 구별.
  - PARTIALLY_CORRECT: greedy가 틀릴 수 있음만 말하고 입력의 반례 누락.
  - INCORRECT: 이 입력의greedy를 최적이라고 하거나 모든 체계에 일반화.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ALG_04_02`
  - CORRECT: dp[x] 최소 개수·dp[0]=0·∞·양의c≤x 전이·모든 마지막 선택 비교, O(A m)/O(A) 또는 동등 memoization.
  - PARTIALLY_CORRECT: 저장·DP 사용만 언급하고 상태·초기값·전이·조건 누락.
  - INCORRECT: 잘못된초기값·모든 후보 미검토로 최소 보장 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ALG-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: greedy는4+1+1로3개지만3+3은2개이므로 이 체계에서 최적이 아니다. dp[x]를 금액x의 최소 개수로 정의하고 dp[0]=0, 아직 만들 수 없는 값은∞로 둔다. x=1부터6까지 dp[x]=min(dp[x-c]+1)로, c∈{1,3,4} 중 c≤x인 경우를 검사한다. 이전 최소 답에 동전 하나를 붙여 모든 마지막 선택을 비교하므로 dp[6]=2다. 양의 정수 동전·무제한 사용 전제이며 시간 O(A×m)·공간 O(A). 특정 화폐의 greedy 성질을 모든 동전에 일반화하지 않는다.
  - 예상 개념 판정: A2_ALG_04_01: CORRECT, A2_ALG_04_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ALG-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 가장 큰 선택이 다음 선택을 제약해4를 먼저 고르면 두1이 필요하다. 금액별 최적 답을 저장하거나 같은 상태를 memoize해 마지막 동전의 모든 후보를 비교한다. 기준0개/금액0에서 시작하고 음수 상태는 제외한다. amount6의 최적은3 두 개다. dp[x]는 금액 x의 최소 개수다. dp[0]=0, 나머지 초기값은 ∞, 전이는 c≤x인 모든 동전 c에 대해 dp[x-c]+1의 최소다. 양의 정수·무제한 동전에서 시간 O(A×m)·공간 O(A)이며 top-down도 같은 상태를 재사용한다.
  - 예상 개념 판정: A2_ALG_04_01: CORRECT, A2_ALG_04_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ALG-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: greedy는4,1,1로3개라3,3의2개보다 나쁘다. 따라서 이 동전 체계에 greedy 최적 보장은 없다. 이전 결과를 저장하는 DP를 쓰면 된다.
  - 예상 개념 판정: A2_ALG_04_01: CORRECT, A2_ALG_04_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ALG-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: greedy의4,1,1이3,3보다 동전 수가 적어 항상 최적이다. DP는 dp[0]=1로 시작하고 마지막 동전 후보를 비교하지 않아도 모든 금액의 최소를 보장한다.
  - 예상 개념 판정: A2_ALG_04_01: INCORRECT, A2_ALG_04_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ALG-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: greedy는4+1+1로3개지만3+3은2개이므로 이 체계에서 최적이 아니다. dp[x]를 금액x의 최소 개수로 정의하고 dp[0]=0, 아직 만들 수 없는 값은∞로 둔다. x=1부터6까지 dp[x]=min(dp[x-c]+1)로, c∈{1,3,4} 중 c≤x인 경우를 검사한다. 이전 최소 답에 동전 하나를 붙여 모든 마지막 선택을 비교하므로 dp[6]=2다. 양의 정수 동전·무제한 사용 전제이며 시간 O(A×m)·공간 O(A). 특정 화폐의 greedy 성질을 모든 동전에 일반화하지 않는다.
  - 예상 개념 판정: A2_ALG_04_01: NEEDS_REVIEW, A2_ALG_04_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 입력 금액 A의 값에 비례하는 분석. 이진 인코딩 길이에 대해 무조건 polynomial이라는 결론 금지.

**출처 대조 위치**

- [MIT 6.006 Spring 2020 — Lecture 16 Dynamic Programming Subproblems](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/28461a74f81101874a13d9679a40584d_MIT6_006S20_lec16.pdf) — Dynamic Programming Review; SRT BOT Steps: state·recurrence·base·time. 최소 동전 사례는 자체 구성

근거 문서: `A2-ALG-DOC`. 검수 상태: **PENDING**

### A2-ALG-05 · INTERMEDIATE

처음 빈 동적 배열의 용량은1이다. 꽉 차면 용량을 두 배로 늘리며 기존 원소를 모두 복사하고, 끝에 하나를 넣는다. 삭제 없이 n개를 넣을 때 특정 append의 최악 비용과 전체·상환 비용을 구별하라.

**모범 답안**

용량이 충분한 append는 O(1), 꽉 찬 순간 append는 기존 원소를 복사하므로 그 시점 원소 수에 대해 O(n)이다. n회까지의 복사량은1+2+4+…로 마지막 용량 수준보다 작고 전체 O(n)이다. 원소 삽입 n회도 O(n)이므로 n회 합계 O(n), append당 상환 O(1)이다. 이는 확률적 평균이 아니라 해당 연산열의 비용을 분산한 분석이다. 삭제·축소·선할당 등 다른 정책의 보장은 별도다.

**필수 개념**

- 개별 append의 복사 비용 · 가중치 0.50: 여유 append O(1)·확장 시 기존 원소 수만큼 복사해 개별 최악 O(n).
- 연산열 전체와 상환 분석 · 가중치 0.50: 기하 복사 합O(n)+삽입O(n)→전체O(n)·상환O(1), 확률 평균과 구별·정책 전제 명시.

**개념별 판정 경계**

- `A2_ALG_05_01`
  - CORRECT: 여유 append O(1)·확장 시 기존 원소 수만큼 복사해 개별 최악 O(n).
  - PARTIALLY_CORRECT: 확장 비용 존재만 말하고 개별 최악과 평상시 구별 누락.
  - INCORRECT: 상환 상수를 매회 최악 O(1)로 오인.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_ALG_05_02`
  - CORRECT: 기하 복사 합O(n)+삽입O(n)→전체O(n)·상환O(1), 확률 평균과 구별·정책 전제 명시.
  - PARTIALLY_CORRECT: 합계/상환 계산은 맞으나 확률 평균과의 구별·정책 전제 누락.
  - INCORRECT: 매회n 복사로O(n²) 주장 또는 확률적 평균과 상환을 동일시.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-ALG-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 용량이 충분한 append는 O(1), 꽉 찬 순간 append는 기존 원소를 복사하므로 그 시점 원소 수에 대해 O(n)이다. n회까지의 복사량은1+2+4+…로 마지막 용량 수준보다 작고 전체 O(n)이다. 원소 삽입 n회도 O(n)이므로 n회 합계 O(n), append당 상환 O(1)이다. 이는 확률적 평균이 아니라 해당 연산열의 비용을 분산한 분석이다. 삭제·축소·선할당 등 다른 정책의 보장은 별도다.
  - 예상 개념 판정: A2_ALG_05_01: CORRECT, A2_ALG_05_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-ALG-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 비싼 복사는 드물게1,2,4,8 규모로 일어나 합계가 선형으로 묶인다. 한 번이 느릴 수 있어도 n번 합은 선형, 나눠 보면 상환 상수다. 임의 입력에서 확률적으로 대부분 빠르다는 주장과는 다르고, 매회 O(1) worst-case를 뜻하지 않는다.
  - 예상 개념 판정: A2_ALG_05_01: CORRECT, A2_ALG_05_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-ALG-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 용량이 찬 append는 기존 원소 복사로 O(n)이다. 복사량1+2+4+…와 n회 삽입을 합해 전체 O(n), 나눠서 상환 O(1)이다.
  - 예상 개념 판정: A2_ALG_05_01: CORRECT, A2_ALG_05_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-ALG-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 상환 O(1)이므로 용량이 찬 순간도 복사 없이 항상 O(1)이다. 복사량은 매회 n이라 합계 O(n²)이며 상환 분석은 무작위 입력의 확률적 평균을 뜻한다.
  - 예상 개념 판정: A2_ALG_05_01: INCORRECT, A2_ALG_05_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-ALG-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 용량이 충분한 append는 O(1), 꽉 찬 순간 append는 기존 원소를 복사하므로 그 시점 원소 수에 대해 O(n)이다. n회까지의 복사량은1+2+4+…로 마지막 용량 수준보다 작고 전체 O(n)이다. 원소 삽입 n회도 O(n)이므로 n회 합계 O(n), append당 상환 O(1)이다. 이는 확률적 평균이 아니라 해당 연산열의 비용을 분산한 분석이다. 삭제·축소·선할당 등 다른 정책의 보장은 별도다.
  - 예상 개념 판정: A2_ALG_05_01: NEEDS_REVIEW, A2_ALG_05_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 메모리 할당기·GC·wall-clock 지연 보장은 아님. O(1) 상환을 실시간 latency 상한으로 사용하지 않음.

**출처 대조 위치**

- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf) — Dynamic Array Sequence; Amortized Analysis
- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — Array resizing: doubling

근거 문서: `A2-ALG-DOC`. 검수 상태: **PENDING**

### A2-NET-01 · BASIC

같은 TCP 연결에서 송신 앱이 메시지 ABC와 DEF를 두 번 write했다. 연결은 정상이고 데이터 손실 없이 수신된다. 수신 앱이 두 번 read해서 정확히 ABC, DEF를 얻는다고 보장되는가? 메시지를 나누는 방법과 EOF의 의미를 설명하라.

**모범 답안**

TCP는 순서 있는 바이트 스트림이며 앱 write의 메시지 경계를 보존하지 않는다. 수신은 AB/CD/EF처럼 쪼개지거나 ABCDEF로 합쳐질 수 있다. 앱은 길이 prefix·구분자·고정 길이 등 framing 계약에 따라 누적 버퍼에서 완성 메시지를 꺼내고 남은 바이트를 보관해야 한다. 구분자는 escape와 길이 제한 등의 규칙도 필요하다. 정상 EOF는 송신 방향의 종료이지 마지막 read가 항상 메시지 하나라는 뜻이 아니며, 미완성 메시지 상태의 EOF는 앱 프로토콜에서 처리해야 한다.

**필수 개념**

- TCP 바이트 순서와 메시지 경계 · 가중치 0.50: 순서 있는 스트림과 write/read 경계 불일치·분할/합침 가능 구별.
- 앱 framing과 불완전 종료 · 가중치 0.50: 누적 버퍼·길이/구분자 등 계약·남은 바이트·미완성EOF 처리와 입력 한계 설명.

**개념별 판정 경계**

- `A2_NET_01_01`
  - CORRECT: 순서 있는 스트림과 write/read 경계 불일치·분할/합침 가능 구별.
  - PARTIALLY_CORRECT: 스트림은 말하나 분할/합침 사례·횟수 불보장 누락.
  - INCORRECT: write 횟수와 메시지 단위 read의 일대일 보장 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_NET_01_02`
  - CORRECT: 누적 버퍼·길이/구분자 등 계약·남은 바이트·미완성EOF 처리와 입력 한계 설명.
  - PARTIALLY_CORRECT: framing 방법만 제시하고 누적·잔여·EOF 경계 누락.
  - INCORRECT: EOF가 임의 메시지를 완성하거나 framing이 불필요하다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-NET-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: TCP는 순서 있는 바이트 스트림이며 앱 write의 메시지 경계를 보존하지 않는다. 수신은 AB/CD/EF처럼 쪼개지거나 ABCDEF로 합쳐질 수 있다. 앱은 길이 prefix·구분자·고정 길이 등 framing 계약에 따라 누적 버퍼에서 완성 메시지를 꺼내고 남은 바이트를 보관해야 한다. 구분자는 escape와 길이 제한 등의 규칙도 필요하다. 정상 EOF는 송신 방향의 종료이지 마지막 read가 항상 메시지 하나라는 뜻이 아니며, 미완성 메시지 상태의 EOF는 앱 프로토콜에서 처리해야 한다.
  - 예상 개념 판정: A2_NET_01_01: CORRECT, A2_NET_01_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-NET-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 전송 보장은 바이트의 순서에 대한 것이고 read 횟수에 대한 것이 아니다. 길이를 먼저 보내고 필요한 바이트를 모아 두 메시지를 파싱해도 된다. 한 read에 여러 메시지가 오거나 일부만 올 수 있으므로 나머지를 보존한다. 길이를 다 채우기 전에 EOF이면 정상 완성으로 취급하지 않는다. 길이 필드의 최대 허용 크기를 정해 무제한 버퍼 사용도 막는다.
  - 예상 개념 판정: A2_NET_01_01: CORRECT, A2_NET_01_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-NET-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: TCP는 byte stream이라 두 write가 두 read로 대응하지 않고 합쳐지거나 나뉠 수 있다. 길이 prefix로 메시지를 구분하면 된다.
  - 예상 개념 판정: A2_NET_01_01: CORRECT, A2_NET_01_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-NET-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: TCP는 write 경계를 유지하므로 두 read는 반드시 ABC와 DEF다. EOF는 다음 메시지 구분자이므로 덜 받은 메시지도 자동으로 정상 완성되며 누적 파싱은 필요 없다.
  - 예상 개념 판정: A2_NET_01_01: INCORRECT, A2_NET_01_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-NET-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: TCP는 순서 있는 바이트 스트림이며 앱 write의 메시지 경계를 보존하지 않는다. 수신은 AB/CD/EF처럼 쪼개지거나 ABCDEF로 합쳐질 수 있다. 앱은 길이 prefix·구분자·고정 길이 등 framing 계약에 따라 누적 버퍼에서 완성 메시지를 꺼내고 남은 바이트를 보관해야 한다. 구분자는 escape와 길이 제한 등의 규칙도 필요하다. 정상 EOF는 송신 방향의 종료이지 마지막 read가 항상 메시지 하나라는 뜻이 아니며, 미완성 메시지 상태의 EOF는 앱 프로토콜에서 처리해야 한다.
  - 예상 개념 판정: A2_NET_01_01: NEEDS_REVIEW, A2_NET_01_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 정상 연결의 스트림 성질이며 수신 API별0/EOF 반환 규약은 별도. TCP ACK는 업무 처리 완료 응답이 아님.

**출처 대조 위치**

- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — §2.2 Key TCP Concepts; §3.9.1.2 Send: push does not supply a record boundary; §3.9.1.3 Receive

근거 문서: `A2-NET-DOC`. 검수 상태: **PENDING**

### A2-NET-02 · INTERMEDIATE

한 UDP datagram씩 위치 갱신을 보낸다. 최신 위치가 오면 늦은 예전 위치를 버릴 수 있지만, 중복이나 역순 도착이 있어도 상태가 과거로 돌아가면 안 된다. UDP 자체가 이 요구를 보장하는가? 앱에 필요한 정보와, 이 선택을 결제 명령에 그대로 쓸 수 없는 이유를 설명하라.

**모범 답안**

UDP는 전달·중복 방지·순서를 보장하지 않는다. 앱은 보낸 순번이나 버전과 세션 식별을 포함하고, 현재 채택한 버전보다 새것만 적용하는 계약을 둘 수 있다. 누락은 다음 최신 값으로 보완할 수 있다는 위치 갱신 요구가 이 선택을 허용한다. 결제는 오래됐다는 이유로 명령을 버려도 되는 계약이 아니므로 요청 식별·처리 결과·재조회·중복 효과 방지 등을 따로 설계해야 한다. TCP로 바꾸는 것만으로 업무 중복이 사라지지도 않는다.

**필수 개념**

- UDP 보장 범위와 앱의 최신성 · 가중치 0.50: 손실·중복·역순 가능, 세션/버전 등 자체 최신성 계약으로 오래된 입력 거부.
- 데이터 의미에 따른 손실·중복 계약 · 가중치 0.50: 위치 snapshot의 손실 허용과 결제 명령의 결과/중복 효과 관리 구별, TCP의 업무 exactly-once 불보장.

**개념별 판정 경계**

- `A2_NET_02_01`
  - CORRECT: 손실·중복·역순 가능, 세션/버전 등 자체 최신성 계약으로 오래된 입력 거부.
  - PARTIALLY_CORRECT: UDP 불보장은 말하나 재시작·버전 비교 계약 누락.
  - INCORRECT: UDP가 전달·중복·순서를 자체 보장한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_NET_02_02`
  - CORRECT: 위치 snapshot의 손실 허용과 결제 명령의 결과/중복 효과 관리 구별, TCP의 업무 exactly-once 불보장.
  - PARTIALLY_CORRECT: 결제에는 다름만 말하고 요청·결과·중복 효과 조건 누락.
  - INCORRECT: 누락 결제 버리기나 TCP만으로 업무 exactly-once 보장 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-NET-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: UDP는 전달·중복 방지·순서를 보장하지 않는다. 앱은 보낸 순번이나 버전과 세션 식별을 포함하고, 현재 채택한 버전보다 새것만 적용하는 계약을 둘 수 있다. 누락은 다음 최신 값으로 보완할 수 있다는 위치 갱신 요구가 이 선택을 허용한다. 결제는 오래됐다는 이유로 명령을 버려도 되는 계약이 아니므로 요청 식별·처리 결과·재조회·중복 효과 방지 등을 따로 설계해야 한다. TCP로 바꾸는 것만으로 업무 중복이 사라지지도 않는다.
  - 예상 개념 판정: A2_NET_02_01: CORRECT, A2_NET_02_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-NET-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 데이터그램별 경계는 있어도 최신성은 앱이 판단한다. 같은 세션의 증가 버전만 반영하고 재시작 때 순번 재사용을 구별한다. 위치 스냅샷 손실 허용과 금전 명령의 처리 보장은 다른 요구이며, 전송 프로토콜 이름만으로 결제 성공이나 exactly-once를 선언할 수 없다.
  - 예상 개념 판정: A2_NET_02_01: CORRECT, A2_NET_02_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-NET-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: UDP는 누락·중복·역순이 가능하다. 위치의 세션·증가 버전을 보내고 새 버전만 적용해 과거로 돌아가는 것을 막는다. 결제에는 더 신중한 설계가 필요하다.
  - 예상 개념 판정: A2_NET_02_01: CORRECT, A2_NET_02_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-NET-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: UDP는 모든 datagram을 중복 없이 순서대로 확정 전달하므로 앱 버전은 불필요하다. 결제도 누락되면 버리면 되고 TCP를 쓰기만 하면 업무 효과 exactly-once가 자동 보장된다.
  - 예상 개념 판정: A2_NET_02_01: INCORRECT, A2_NET_02_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-NET-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: UDP는 전달·중복 방지·순서를 보장하지 않는다. 앱은 보낸 순번이나 버전과 세션 식별을 포함하고, 현재 채택한 버전보다 새것만 적용하는 계약을 둘 수 있다. 누락은 다음 최신 값으로 보완할 수 있다는 위치 갱신 요구가 이 선택을 허용한다. 결제는 오래됐다는 이유로 명령을 버려도 되는 계약이 아니므로 요청 식별·처리 결과·재조회·중복 효과 방지 등을 따로 설계해야 한다. TCP로 바꾸는 것만으로 업무 중복이 사라지지도 않는다.
  - 예상 개념 판정: A2_NET_02_01: NEEDS_REVIEW, A2_NET_02_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** UDP 기반 프로토콜이 별도 신뢰성·혼잡 제어를 구현할 수 있음. UDP라는 이유만으로 모든 앱을 불신뢰로 분류하지 않음.

**출처 대조 위치**

- [RFC 768 — User Datagram Protocol](https://www.rfc-editor.org/rfc/rfc768.html) — User Datagram Protocol: delivery and duplicate protection not guaranteed
- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — §2.2 Key TCP Concepts: transport byte-stream 보장. 앱 버전·결제 계약은 자체 설계

근거 문서: `A2-NET-DOC`. 검수 상태: **PENDING**

### A2-NET-03 · INTERMEDIATE

TCP 송신에서 수신 윈도 rwnd=64KiB, 혼잡 윈도 cwnd=16KiB, 이미 ACK되지 않은 데이터는12KiB다. 단순화한 모델에서 추가로 보낼 수 있는 양과 두 윈도의 책임을 설명하라. 수신 버퍼만 늘리면 항상 더 보낼 수 있는가?

**모범 답안**

총 미확인 데이터 한도는 min(rwnd,cwnd)=16KiB이고 이미12KiB이므로 추가4KiB다. rwnd는 수신자가 처리·보관할 여유를 알리는 흐름 제어, cwnd는 네트워크 경로의 혼잡을 고려한 송신자 제한이다. 이 경우 cwnd가 더 작으므로 수신 버퍼와 rwnd만 늘려도 추가 한도는 커지지 않는다. ACK·손실·송신 pacing 등 실제 조건도 확인해야 하며 둘 중 하나만 보고 전송 가능량을 확정하지 않는다.

**필수 개념**

- 흐름 제어와 혼잡 제어의 책임 · 가중치 0.50: rwnd 수신 여유·cwnd 경로 혼잡, min 한도에서in-flight 차감해4KiB.
- 최소 한도의 병목과 추가 조건 · 가중치 0.50: cwnd가 작은 현재 조건에서rwnd 증가만으로 한도 증가 불가, ACK/손실/pacing 등 별도 조건 명시.

**개념별 판정 경계**

- `A2_NET_03_01`
  - CORRECT: rwnd 수신 여유·cwnd 경로 혼잡, min 한도에서in-flight 차감해4KiB.
  - PARTIALLY_CORRECT: 두 윈도 역할 또는in-flight 차감 설명 일부 누락.
  - INCORRECT: 역할을 뒤집거나 두 제한을 더해 전송량 계산.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_NET_03_02`
  - CORRECT: cwnd가 작은 현재 조건에서rwnd 증가만으로 한도 증가 불가, ACK/손실/pacing 등 별도 조건 명시.
  - PARTIALLY_CORRECT: 두 제한 확인만 말하고 버퍼 증가 반례·실제 조건 누락.
  - INCORRECT: 수신 버퍼 증가만으로 항상 전송량 향상 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-NET-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 총 미확인 데이터 한도는 min(rwnd,cwnd)=16KiB이고 이미12KiB이므로 추가4KiB다. rwnd는 수신자가 처리·보관할 여유를 알리는 흐름 제어, cwnd는 네트워크 경로의 혼잡을 고려한 송신자 제한이다. 이 경우 cwnd가 더 작으므로 수신 버퍼와 rwnd만 늘려도 추가 한도는 커지지 않는다. ACK·손실·송신 pacing 등 실제 조건도 확인해야 하며 둘 중 하나만 보고 전송 가능량을 확정하지 않는다.
  - 예상 개념 판정: A2_NET_03_01: CORRECT, A2_NET_03_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-NET-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 두 제한 중 작은 쪽에서 현재 in-flight를 빼면4KiB다. 버퍼는 수신 쪽 과부하를, 혼잡 윈도는 경로 부하를 제어한다. rwnd가64에서128로 커져도 cwnd16이 병목이다. 실제 구현의 pacing 등은 이 계산 밖의 추가 제약이다.
  - 예상 개념 판정: A2_NET_03_01: CORRECT, A2_NET_03_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-NET-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: rwnd는 수신 여유, cwnd는 경로 혼잡 제한이다. min(64,16)-12=4KiB다. 두 제한을 함께 확인해야 한다.
  - 예상 개념 판정: A2_NET_03_01: CORRECT, A2_NET_03_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-NET-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: rwnd는 네트워크 혼잡이고 cwnd는 수신 버퍼다.64+16-12=68KiB를 더 보낼 수 있으며 수신 버퍼만 늘리면 cwnd와 관계없이 항상 전송량이 늘어난다.
  - 예상 개념 판정: A2_NET_03_01: INCORRECT, A2_NET_03_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-NET-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 총 미확인 데이터 한도는 min(rwnd,cwnd)=16KiB이고 이미12KiB이므로 추가4KiB다. rwnd는 수신자가 처리·보관할 여유를 알리는 흐름 제어, cwnd는 네트워크 경로의 혼잡을 고려한 송신자 제한이다. 이 경우 cwnd가 더 작으므로 수신 버퍼와 rwnd만 늘려도 추가 한도는 커지지 않는다. ACK·손실·송신 pacing 등 실제 조건도 확인해야 하며 둘 중 하나만 보고 전송 가능량을 확정하지 않는다.
  - 예상 개념 판정: A2_NET_03_01: NEEDS_REVIEW, A2_NET_03_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** KiB 단위의 단순 계산. 전체 전송 throughput·RTT·알고리즘별 정확한cwnd 조정까지 인증하는 예제가 아님.

**출처 대조 위치**

- [RFC 5681 — TCP Congestion Control](https://www.rfc-editor.org/rfc/rfc5681.html) — §2 Definitions: rwnd·cwnd; §3 Congestion Control Algorithms
- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — §3.8.6.1 Window Management

근거 문서: `A2-NET-DOC`. 검수 상태: **PENDING**

### A2-NET-04 · INTERMEDIATE

resolver가 t=0에 TTL300초인 A 레코드의 옛 IP를 저장했다. 권한 서버는 t=100에 새 IP로 바꾸며 새 레코드 TTL을30초로 정했다. prefetch·serve-stale·중간 캐시·기존 연결 없이 RFC1034 기본 TTL 모델만 쓴다. t=150 질의에서 옛 IP가 가능한 이유와, 새 TTL30이 기존 캐시를 즉시 없애는지 설명하라.

**모범 답안**

기존 캐시의 저장 수명은 t=0부터300초이므로 t=150에서 남은150초 동안 옛 레코드를 사용할 수 있다. 권한 서버의 갱신은 이미 저장된 모든 resolver 캐시를 즉시 밀어내지 않는다. 새 TTL30은 새 응답을 받아 저장할 때의 수명이지 옛 캐시에 소급되지 않는다. 이 모델에서는 t=300에 기존 항목이 만료되어 다음 조회에서 새 값을 얻을 수 있다. 변경 준비는 기존 TTL이 지나갈 시간을 고려해야 하며 DNS 갱신과 살아 있는 연결 전환을 같은 사건으로 다루지 않는다.

**필수 개념**

- DNS 캐시 수명과 권한 갱신 · 가중치 0.50: 기존t=0+300 수명·t=150의옛값 가능, 권한 갱신과resolver 캐시 즉시 삭제 구별.
- TTL 적용 시점과 전환 범위 · 가중치 0.50: 새30초는새로 저장하는 응답에 적용·소급 없음, 만료 후질의·기존 연결 전환 별개 설명.

**개념별 판정 경계**

- `A2_NET_04_01`
  - CORRECT: 기존t=0+300 수명·t=150의옛값 가능, 권한 갱신과resolver 캐시 즉시 삭제 구별.
  - PARTIALLY_CORRECT: 캐시 가능성만 말하고 남은 수명·시점 누락.
  - INCORRECT: 권한 변경이 모든 캐시에 즉시 전달된다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_NET_04_02`
  - CORRECT: 새30초는새로 저장하는 응답에 적용·소급 없음, 만료 후질의·기존 연결 전환 별개 설명.
  - PARTIALLY_CORRECT: TTL 준비 필요성만 언급하고 소급 여부·전환 경계 누락.
  - INCORRECT: 새TTL이 옛 캐시를 소급 만료하거나 기존연결이자동이동 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-NET-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 기존 캐시의 저장 수명은 t=0부터300초이므로 t=150에서 남은150초 동안 옛 레코드를 사용할 수 있다. 권한 서버의 갱신은 이미 저장된 모든 resolver 캐시를 즉시 밀어내지 않는다. 새 TTL30은 새 응답을 받아 저장할 때의 수명이지 옛 캐시에 소급되지 않는다. 이 모델에서는 t=300에 기존 항목이 만료되어 다음 조회에서 새 값을 얻을 수 있다. 변경 준비는 기존 TTL이 지나갈 시간을 고려해야 하며 DNS 갱신과 살아 있는 연결 전환을 같은 사건으로 다루지 않는다.
  - 예상 개념 판정: A2_NET_04_01: CORRECT, A2_NET_04_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-NET-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: resolver가 아는 것은0초에 받은300초 유효 기간이다.100초의 서버 변경을 아직 모르면150초에도 그 값을 답할 수 있다. 새 응답을 받은 뒤부터30초 계약이 생긴다. 다음 조회와 캐시 만료를 확인해야 하고 TTL만으로 모든 클라이언트의 즉시 전환을 선언할 수 없다.
  - 예상 개념 판정: A2_NET_04_01: CORRECT, A2_NET_04_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-NET-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: t=150은 기존300초 TTL 안이라 옛IP를 사용할 수 있다. 서버 변경이 resolver에 즉시 push되지 않아서다. 새TTL은 변경 준비에 고려한다.
  - 예상 개념 판정: A2_NET_04_01: CORRECT, A2_NET_04_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-NET-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 권한 서버가100초에 바꾸면 모든 resolver가 즉시 새IP를 받는다. 새TTL30이 옛 캐시에 소급되므로130초부터 옛IP는 불가능하며 기존 TCP 연결도 자동으로 새IP로 이동한다.
  - 예상 개념 판정: A2_NET_04_01: INCORRECT, A2_NET_04_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-NET-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 기존 캐시의 저장 수명은 t=0부터300초이므로 t=150에서 남은150초 동안 옛 레코드를 사용할 수 있다. 권한 서버의 갱신은 이미 저장된 모든 resolver 캐시를 즉시 밀어내지 않는다. 새 TTL30은 새 응답을 받아 저장할 때의 수명이지 옛 캐시에 소급되지 않는다. 이 모델에서는 t=300에 기존 항목이 만료되어 다음 조회에서 새 값을 얻을 수 있다. 변경 준비는 기존 TTL이 지나갈 시간을 고려해야 하며 DNS 갱신과 살아 있는 연결 전환을 같은 사건으로 다루지 않는다.
  - 예상 개념 판정: A2_NET_04_01: NEEDS_REVIEW, A2_NET_04_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 현행serve-stale·negative cache·브라우저 캐시는 이 모델에서 제외. 기본TTL 설명을 현실의 절대 전환 시각으로 확대하지 않음.

**출처 대조 위치**

- [RFC 1034 — Domain Names: Concepts and Facilities](https://www.rfc-editor.org/rfc/rfc1034.html) — §3.2.1 Resource records: TTL; §4.3.4 Caching; §5.3.3 resolver algorithm

근거 문서: `A2-NET-DOC`. 검수 상태: **PENDING**

### A2-NET-05 · INTERMEDIATE

브라우저는 신뢰 체인·호스트명·서명 검증을 정상 수행하는 인증서 기반 TLS1.3으로 프록시에 연결한다. 프록시에서 TLS를 종료하고 앱까지는 별도 네트워크의 평문 HTTP다. TLS가 보호하는 구간·프록시의 평문 관측·앱 구간과 사용자 인가의 책임을 설명하라.

**모범 답안**

TLS의 기밀성·무결성은 브라우저와 TLS endpoint인 프록시 사이에 적용된다. 검증을 정상 수행하므로 브라우저는 해당 서버 신원을 확인할 수 있지만 사용자 업무 권한을 확인한 것은 아니다. 프록시는 복호화 endpoint라 본문을 읽을 수 있다. 프록시→앱 평문 구간에는 앞 연결의 TLS 보호가 이어지지 않으므로 별도TLS·네트워크 통제 등 요구에 맞는 보호가 필요하다. 앱은 로그인·소유권·인가를 따로 확인해야 한다.

**필수 개념**

- TLS endpoint의 보호 구간 · 가중치 0.50: 브라우저→프록시의기밀성·무결성, endpoint복호화·프록시평문관측 가능 설명.
- 서버 신원과 앱·사용자 권한의 경계 · 가중치 0.50: 정상서버신원검증·사용자인가별도, 프록시→앱구간별도TLS/통제 대안과한계명시.

**개념별 판정 경계**

- `A2_NET_05_01`
  - CORRECT: 브라우저→프록시의기밀성·무결성, endpoint복호화·프록시평문관측 가능 설명.
  - PARTIALLY_CORRECT: 종료 위치만 말하고보호구간·관측권한 일부누락.
  - INCORRECT: 프록시도 읽지 못하거나 별도 평문 구간까지 자동 암호화 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_NET_05_02`
  - CORRECT: 정상서버신원검증·사용자인가별도, 프록시→앱구간별도TLS/통제 대안과한계명시.
  - PARTIALLY_CORRECT: 추가보호·권한확인방향만 말하고 서버 신원과 인가 구별·구간대안누락.
  - INCORRECT: 인증서가 사용자 업무 권한을 보장하거나 평문 다음 구간 보호 불필요 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-NET-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: TLS의 기밀성·무결성은 브라우저와 TLS endpoint인 프록시 사이에 적용된다. 검증을 정상 수행하므로 브라우저는 해당 서버 신원을 확인할 수 있지만 사용자 업무 권한을 확인한 것은 아니다. 프록시는 복호화 endpoint라 본문을 읽을 수 있다. 프록시→앱 평문 구간에는 앞 연결의 TLS 보호가 이어지지 않으므로 별도TLS·네트워크 통제 등 요구에 맞는 보호가 필요하다. 앱은 로그인·소유권·인가를 따로 확인해야 한다.
  - 예상 개념 판정: A2_NET_05_01: CORRECT, A2_NET_05_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-NET-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 암호화된 선의 끝이 프록시다. 그곳에서 읽은 내용을 HTTP로 다시 보내면 다음 선은 암호화되지 않는다. 프록시가 읽는 것은 endpoint의 정상 역할이다. 두 번째 구간을 보호하고, 서버 인증과 사용자의 주문 접근 권한도 별도 검사한다.
  - 예상 개념 판정: A2_NET_05_01: CORRECT, A2_NET_05_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-NET-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: TLS endpoint는 프록시라 브라우저→프록시만 기밀성·무결성이 적용되고 프록시는 평문을 읽는다. 앱 쪽 보호와 권한은 추가로 확인해야 한다.
  - 예상 개념 판정: A2_NET_05_01: CORRECT, A2_NET_05_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-NET-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: TLS가 프록시에 있으면 앱까지 평문이어도 자동으로 종단간 암호화된다. 프록시도 본문을 못 읽으며 서버 인증서만 검증하면 모든 사용자의 업무 접근 권한이 자동 승인된다.
  - 예상 개념 판정: A2_NET_05_01: INCORRECT, A2_NET_05_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-NET-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: TLS의 기밀성·무결성은 브라우저와 TLS endpoint인 프록시 사이에 적용된다. 검증을 정상 수행하므로 브라우저는 해당 서버 신원을 확인할 수 있지만 사용자 업무 권한을 확인한 것은 아니다. 프록시는 복호화 endpoint라 본문을 읽을 수 있다. 프록시→앱 평문 구간에는 앞 연결의 TLS 보호가 이어지지 않으므로 별도TLS·네트워크 통제 등 요구에 맞는 보호가 필요하다. 앱은 로그인·소유권·인가를 따로 확인해야 한다.
  - 예상 개념 판정: A2_NET_05_01: NEEDS_REVIEW, A2_NET_05_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** TLS1.3의 모든 모드가 같은 인증과 성질을 갖는다고 일반화하지 않음. PSK·0-RTT·client certificate는 이번 전제 밖.

**출처 대조 위치**

- [RFC 8446 — The Transport Layer Security (TLS) Protocol Version 1.3](https://www.rfc-editor.org/rfc/rfc8446.html) — §1 Introduction: authentication·confidentiality·integrity; §4.4 Authentication Messages. 프록시 배치는 자체 사례

근거 문서: `A2-NET-DOC`. 검수 상태: **PENDING**

### A2-OOD-01 · INTERMEDIATE

기간 객체가 start≤end를 항상 유지해야 한다. 현재(2,4)에서 replacePeriod(5,3)를 호출한다. 공개 setter 두 개로 순서대로 바꾸면 어떤 문제가 생기는가? 실패 시 두 값 모두(2,4)를 유지하는 변경 방법과 규칙의 소유자를 설명하라.

**모범 답안**

start부터5로 바꾸면 중간(5,4)이 불변식을 어기고, end 검증에서 실패해도 이미 바뀐 start가 남을 수 있다. 기간을 소유한 객체가 생성·수정에서 같은 불변식을 검사하고, replacePeriod는 새 쌍 전체를 검증한 뒤 두 필드를 바꿔야 한다. 잘못된(5,3)은 변경 전 거부하여 기존(2,4)를 유지한다. 불변 기간 값 객체를 새로 만들어 성공 시 교체하는 대안도 유효하다. Controller나 Service의 검사만으로 다른 호출 경로까지 방어되지는 않는다.

**필수 개념**

- 불변식과 상태 소유자의 책임 · 가중치 0.50: 기간 객체가 생성·수정 규칙 소유, 공개 독립 setter/외부 검사만으로 중간·우회 경로 방어 불가 설명.
- 실패 전 검증과 원상태 보존 · 가중치 0.50: 새 쌍 전체 검증 후 필드 변경 또는 유효한 불변 값 교체, (5,3) 실패 시 (2,4) 유지.

**개념별 판정 경계**

- `A2_OOD_01_01`
  - CORRECT: 기간 객체가 생성·수정 규칙 소유, 공개 독립 setter/외부 검사만으로 중간·우회 경로 방어 불가 설명.
  - PARTIALLY_CORRECT: 객체 검사 방향만 말하고 생성·우회 경로 누락.
  - INCORRECT: Service 검사만으로 우회 가능한 객체 불변식 보장 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_OOD_01_02`
  - CORRECT: 새 쌍 전체 검증 후 필드 변경 또는 유효한 불변 값 교체, (5,3) 실패 시 (2,4) 유지.
  - PARTIALLY_CORRECT: 일괄 메서드만 제안하고 검증→변경 순서·실패 상태 누락.
  - INCORRECT: 부분 필드 변경 후 실패 상태 (5,4) 허용 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-OOD-01-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: start부터5로 바꾸면 중간(5,4)이 불변식을 어기고, end 검증에서 실패해도 이미 바뀐 start가 남을 수 있다. 기간을 소유한 객체가 생성·수정에서 같은 불변식을 검사하고, replacePeriod는 새 쌍 전체를 검증한 뒤 두 필드를 바꿔야 한다. 잘못된(5,3)은 변경 전 거부하여 기존(2,4)를 유지한다. 불변 기간 값 객체를 새로 만들어 성공 시 교체하는 대안도 유효하다. Controller나 Service의 검사만으로 다른 호출 경로까지 방어되지는 않는다.
  - 예상 개념 판정: A2_OOD_01_01: CORRECT, A2_OOD_01_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-OOD-01-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 시작·끝은 한 규칙에 묶인 상태라 각각 setter를 외부에 열기보다 한 요청으로 받는다. 유효한 새 기간을 먼저 만들어 교체하거나 모든 검사를 먼저 마친다. 실패 전 필드에 쓰지 않으면 원상태가 보존된다. 이 책임은 기간 객체에 있고 외부 검사는 사용자 안내를 보조할 수 있다. 이 입력은 변경 전에 거부하고 두 값 모두 (2,4)를 유지한다.
  - 예상 개념 판정: A2_OOD_01_01: CORRECT, A2_OOD_01_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-OOD-01-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: start≤end 규칙은 기간 객체가 생성·변경 때 모두 지켜야 한다. setter로 중간 (5,4)을 만드는 것은 문제여서 기간을 한꺼번에 바꾸는 메서드가 필요하다.
  - 예상 개념 판정: A2_OOD_01_01: CORRECT, A2_OOD_01_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-OOD-01-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: Service만 검사하면 객체에는 공개 setter가 있어도 모든 호출 경로의 불변식이 보장된다. start를 먼저 5로 저장하고 실패 시 (5,4)를 남겨도 검증 실패의 원상태 보존 요구를 만족한다.
  - 예상 개념 판정: A2_OOD_01_01: INCORRECT, A2_OOD_01_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-OOD-01-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: start부터5로 바꾸면 중간(5,4)이 불변식을 어기고, end 검증에서 실패해도 이미 바뀐 start가 남을 수 있다. 기간을 소유한 객체가 생성·수정에서 같은 불변식을 검사하고, replacePeriod는 새 쌍 전체를 검증한 뒤 두 필드를 바꿔야 한다. 잘못된(5,3)은 변경 전 거부하여 기존(2,4)를 유지한다. 불변 기간 값 객체를 새로 만들어 성공 시 교체하는 대안도 유효하다. Controller나 Service의 검사만으로 다른 호출 경로까지 방어되지는 않는다.
  - 예상 개념 판정: A2_OOD_01_01: NEEDS_REVIEW, A2_OOD_01_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** Java private만으로 규칙이 자동 실현되는 것은 아님. DBrollback이이미변경한Java필드를 자동 복구한다는 설명도 금지.

**출처 대조 위치**

- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html) — Anemic Domain Model: behavior and domain logic
- [Java Language Specification 21 — Chapter 8 Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) — §8.3 Field Declarations; §8.8 Constructors. 기간불변식·실패계약은자체설계

근거 문서: `A2-OOD-DOC`. 검수 상태: **PENDING**

### A2-OOD-02 · INTERMEDIATE

주문은 소계·최종금액을 소유하며 0≤할인≤소계를 항상 유지해야 한다. 할인 정책은 등급·소계로 금액을 계산하며 캠페인마다 바뀐다. 주문에 모든 정책 분기를 넣는 대신 어떤 협력 경계를 둘 수 있는가? 정책 변경과 잘못된 반환값의 책임을 나눠 설명하라.

**모범 답안**

할인 계산을 DiscountPolicy 같은 작은 계약으로 분리하고 주문 또는 유스케이스가 선택한 정책을 합성할 수 있다. 필요한 등급·소계를 전달하면 정책별 계산 변경이 해당 정책으로 모인다. 주문은 정책 반환값을 신뢰해 무조건 저장하지 않고 자신의 금액 불변식을 최종 검사한다. 새 정책 때 주문 핵심 규칙까지 바꿀 필요는 줄지만 정책 선택/조립은 바뀔 수 있다. 함수 전략이나 명확히 분리한 규칙 모듈도 조건을 만족하면 유효하며 상속 계층이 필수는 아니다.

**필수 개념**

- 변경 이유에 따른 정책 협력 · 가중치 0.50: 계산 정책의 작은 계약·필요 정보 전달·합성, 계산 변경과 선택/조립 변경을 구별. 함수/분리 모듈도 허용.
- 전략 교체와 불변식의 최종 방어 · 가중치 0.50: 주문이 할인 범위 최종 검사, 잘못된 정책값 거부. 상속·특정 패턴이 필수 정답 아님.

**개념별 판정 경계**

- `A2_OOD_02_01`
  - CORRECT: 계산 정책의 작은 계약·필요 정보 전달·합성, 계산 변경과 선택/조립 변경을 구별. 함수/분리 모듈도 허용.
  - PARTIALLY_CORRECT: 정책 분리만 말하고 정보·선택 책임 누락.
  - INCORRECT: 정책에 필요한 소계·등급 없이 항상 정확한 할인 계산이 가능하거나 캠페인 추가가 계산·선택 코드의 변화를 전혀 요구하지 않는다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_OOD_02_02`
  - CORRECT: 주문이 할인 범위 최종 검사, 잘못된 정책값 거부. 상속·특정 패턴이 필수 정답 아님.
  - PARTIALLY_CORRECT: 주문 책임을 언급하나 잘못된 반환값 검사·허용 대안 조건 누락.
  - INCORRECT: 다형성을 위해 범위 밖 할인을 무조건 저장해야 한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-OOD-02-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 할인 계산을 DiscountPolicy 같은 작은 계약으로 분리하고 주문 또는 유스케이스가 선택한 정책을 합성할 수 있다. 필요한 등급·소계를 전달하면 정책별 계산 변경이 해당 정책으로 모인다. 주문은 정책 반환값을 신뢰해 무조건 저장하지 않고 자신의 금액 불변식을 최종 검사한다. 새 정책 때 주문 핵심 규칙까지 바꿀 필요는 줄지만 정책 선택/조립은 바뀔 수 있다. 함수 전략이나 명확히 분리한 규칙 모듈도 조건을 만족하면 유효하며 상속 계층이 필수는 아니다.
  - 예상 개념 판정: A2_OOD_02_01: CORRECT, A2_OOD_02_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-OOD-02-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 변하는 계산과 항상 유지할 금액 범위를 나눈다. 교체 가능한 함수가 소계·등급으로 할인을 제안하고 주문이 범위를 확인한 뒤 반영해도 된다. 캠페인 추가는 계산 함수와 선택 설정이 담당하며 주문은 불변식 소유를 유지한다. 유형 수가 작으면 명확한 조건 분기도 가능하므로 패턴 이름만 요구하지 않는다.
  - 예상 개념 판정: A2_OOD_02_01: CORRECT, A2_OOD_02_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-OOD-02-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 정책을 작은 계약으로 빼고 등급·소계를 전달하여 합성하면 계산 변경을 정책별로 모을 수 있다. 새 정책은 정책 구현과 선택 부분을 바꾸면 된다. 주문이 금액을 잘 관리해야 한다.
  - 예상 개념 판정: A2_OOD_02_01: CORRECT, A2_OOD_02_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-OOD-02-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 정책이 소계·등급을 전혀 몰라도 모든 할인 결과를 정확히 계산할 수 있고 새 캠페인은 계산 구현·선택 코드에 아무 변경도 필요 없다. 다형성을 지키려면 정책이 반환한 음수·소계 초과 할인도 무조건 저장해야 한다.
  - 예상 개념 판정: A2_OOD_02_01: INCORRECT, A2_OOD_02_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-OOD-02-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 할인 계산을 DiscountPolicy 같은 작은 계약으로 분리하고 주문 또는 유스케이스가 선택한 정책을 합성할 수 있다. 필요한 등급·소계를 전달하면 정책별 계산 변경이 해당 정책으로 모인다. 주문은 정책 반환값을 신뢰해 무조건 저장하지 않고 자신의 금액 불변식을 최종 검사한다. 새 정책 때 주문 핵심 규칙까지 바꿀 필요는 줄지만 정책 선택/조립은 바뀔 수 있다. 함수 전략이나 명확히 분리한 규칙 모듈도 조건을 만족하면 유효하며 상속 계층이 필수는 아니다.
  - 예상 개념 판정: A2_OOD_02_01: NEEDS_REVIEW, A2_OOD_02_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 모든 변경에 인터페이스가 필요하다는 규칙이 아님. 제시한변경축·불변식·협력조건으로판단.

**출처 대조 위치**

- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — Chapter 9 introduction: unrelated classes implement one contract
- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html) — Domain logic in behavior-rich objects. 정책분리·금액계약은자체설계

근거 문서: `A2-OOD-DOC`. 검수 상태: **PENDING**

### A2-OOD-03 · INTERMEDIATE

Withdrawal 계약은 잔액 충분한 양수 amount를 받으면 추가 수수료 없이 정확히 amount만 차감한다. 구현 LimitedWithdrawal은 amount>1000이면 거부하고 성공 시 추가 10을 차감한다. 컴파일은 된다. 이 구현을 원계약의 대체로 쓸 수 있는가? 요구를 보존할 대안을 설명하라.

**모범 답안**

대체로 쓸 수 없다. 원계약이 허용한 충분한 잔액의 2000 출금을 새로 거절해 전제조건을 강화하고, 성공 시 amount+10 차감해 후조건도 어긴다. 같은 메서드 시그니처와 컴파일 성공은 행동 계약 준수를 보장하지 않는다. 제한·수수료 없는 원계약을 그대로 지키는 구현을 쓰거나, 제한·수수료를 드러내는 별도 계약과 호출자 흐름을 설계해야 한다. 인터페이스 이름만 바꿔 원호출자를 그대로 두는 것은 해결이 아니다.

**필수 개념**

- 타입 호환과 행동 계약 · 가중치 0.50: 허용 2000 거부의 전제 강화·추가 10의 후조건 위반, 컴파일과 대체 가능성 구별.
- 명시적 계약 변경의 대안 · 가중치 0.50: 원계약 준수 구현 또는 제한/수수료의 별도 계약과 호출자 적응·검증. 이름 변경만으로 해결 불가.

**개념별 판정 경계**

- `A2_OOD_03_01`
  - CORRECT: 허용 2000 거부의 전제 강화·추가 10의 후조건 위반, 컴파일과 대체 가능성 구별.
  - PARTIALLY_CORRECT: 계약 위반만 말하고 입력·결과의 두 위반 누락.
  - INCORRECT: 같은 시그니처·컴파일만으로 행동 대체 가능성 보장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_OOD_03_02`
  - CORRECT: 원계약 준수 구현 또는 제한/수수료의 별도 계약과 호출자 적응·검증. 이름 변경만으로 해결 불가.
  - PARTIALLY_CORRECT: 설계 수정 필요성만 말하고 유효 대안·호출자 조건 누락.
  - INCORRECT: 계약 이름만 바꿔 기존 호출자 약속이 자동 보존된다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-OOD-03-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 대체로 쓸 수 없다. 원계약이 허용한 충분한 잔액의 2000 출금을 새로 거절해 전제조건을 강화하고, 성공 시 amount+10 차감해 후조건도 어긴다. 같은 메서드 시그니처와 컴파일 성공은 행동 계약 준수를 보장하지 않는다. 제한·수수료 없는 원계약을 그대로 지키는 구현을 쓰거나, 제한·수수료를 드러내는 별도 계약과 호출자 흐름을 설계해야 한다. 인터페이스 이름만 바꿔 원호출자를 그대로 두는 것은 해결이 아니다.
  - 예상 개념 판정: A2_OOD_03_01: CORRECT, A2_OOD_03_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-OOD-03-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 호출자는 2000이 가능하고 차감이 정확하다고 믿는데 새 객체는 그 두 약속을 깨뜨린다. 구현이 원약속을 따르게 고치거나 호출자가 제한·수수료를 알고 선택하도록 다른 능력 계약을 둔다. 수수료가 필요하다는 업무 변경도 원계약과 호환이 아니므로 호출자 변경·검증을 같이 해야 한다.
  - 예상 개념 판정: A2_OOD_03_01: CORRECT, A2_OOD_03_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-OOD-03-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: 같은 시그니처로 컴파일돼도 2000 거부는 입력 전제를 강화하고 추가 10 차감은 원후조건을 어긴다. 따라서 대체 가능하지 않으며 설계를 고쳐야 한다.
  - 예상 개념 판정: A2_OOD_03_01: CORRECT, A2_OOD_03_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-OOD-03-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 컴파일되면 입력 제한과 추가 차감이 달라도 원계약을 완전히 대체한다. 클래스·인터페이스 이름만 바꾸면 기존 호출자를 수정하지 않아도 2000·정확 차감 계약이 자동으로 보존된다.
  - 예상 개념 판정: A2_OOD_03_01: INCORRECT, A2_OOD_03_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-OOD-03-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 대체로 쓸 수 없다. 원계약이 허용한 충분한 잔액의 2000 출금을 새로 거절해 전제조건을 강화하고, 성공 시 amount+10 차감해 후조건도 어긴다. 같은 메서드 시그니처와 컴파일 성공은 행동 계약 준수를 보장하지 않는다. 제한·수수료 없는 원계약을 그대로 지키는 구현을 쓰거나, 제한·수수료를 드러내는 별도 계약과 호출자 흐름을 설계해야 한다. 인터페이스 이름만 바꿔 원호출자를 그대로 두는 것은 해결이 아니다.
  - 예상 개념 판정: A2_OOD_03_01: NEEDS_REVIEW, A2_OOD_03_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 잔액·한도·수수료는 문항의 명시적 업무 계약. 모든 금융 객체에 동일한 계약이 있다고 일반화하지 않음.

**출처 대조 위치**

- [Liskov and Wing 1994 — A Behavioral Notion of Subtyping](https://www.cs.cmu.edu/~wing/publications/LiskovWing94.pdf) — §1 Subtype Requirement; §3 subtype methods and behavior preservation
- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — Chapter 9 interface implementation: 언어 타입 계약과의 구별

근거 문서: `A2-OOD-DOC`. 검수 상태: **PENDING**

### A2-OOD-04 · INTERMEDIATE

배송 견적 유스케이스가 외부 HTTP의 인증 헤더·JSON 필드명·timeout과 배송 정책을 한 메서드에서 다룬다. 외부 필드명만 바뀌었는데 업무 코드도 수정된다. 작은 조회 계약과 adapter를 쓴다면 어떤 정보·실패를 경계에 두며, mock 통과가 무엇을 검증하지 못하는가?

**모범 답안**

유스케이스가 필요한 배송 요율 조회 계약을 업무 입력·출력으로 정하고 HTTP adapter가 인증·JSON 변환·통신을 소유한다. 업무 정책은 그 값을 받아 견적을 판단하므로 필드명 변경은 주로 adapter에 모인다. timeout·조회 불가·유효하지 않은 요율의 의미를 계약에 명시하고 모두 0원으로 숨기지 않는다. interface가 있어도 HTTP DTO가 계약에 새면 경계가 약해진다. mock 통과는 호출자 조정만 확인하며 실제 HTTP 인증·직렬화·외부 응답·DB나 네트워크 정상까지 증명하지 않는다.

**필수 개념**

- 업무 계약과 전송 adapter의 경계 · 가중치 0.50: 유스케이스 필요 정보의 업무 입출력·HTTP adapter의 인증/변환/통신 소유, 외부 DTO 누수 방지.
- 실패 의미와 검증 책임 · 가중치 0.50: timeout/조회 실패를 명시해 0원 성공과 구별, mock 조정 검사와 실제 HTTP 계약 검증 분리.

**개념별 판정 경계**

- `A2_OOD_04_01`
  - CORRECT: 유스케이스 필요 정보의 업무 입출력·HTTP adapter의 인증/변환/통신 소유, 외부 DTO 누수 방지.
  - PARTIALLY_CORRECT: 분리 방향만 말하고 전송 정보·업무 계약 역할 누락.
  - INCORRECT: interface 존재만으로 HTTP DTO의 외부 변경 누수가 없다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_OOD_04_02`
  - CORRECT: timeout/조회 실패를 명시해 0원 성공과 구별, mock 조정 검사와 실제 HTTP 계약 검증 분리.
  - PARTIALLY_CORRECT: 분리 후 테스트만 제안하고 실패 정책·실제 통신 검증 경계 누락.
  - INCORRECT: 모든 실패를 0원 처리하거나 mock이 실제 네트워크 정상까지 보장한다고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-OOD-04-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 유스케이스가 필요한 배송 요율 조회 계약을 업무 입력·출력으로 정하고 HTTP adapter가 인증·JSON 변환·통신을 소유한다. 업무 정책은 그 값을 받아 견적을 판단하므로 필드명 변경은 주로 adapter에 모인다. timeout·조회 불가·유효하지 않은 요율의 의미를 계약에 명시하고 모두 0원으로 숨기지 않는다. interface가 있어도 HTTP DTO가 계약에 새면 경계가 약해진다. mock 통과는 호출자 조정만 확인하며 실제 HTTP 인증·직렬화·외부 응답·DB나 네트워크 정상까지 증명하지 않는다.
  - 예상 개념 판정: A2_OOD_04_01: CORRECT, A2_OOD_04_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-OOD-04-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 변하는 전송 형식은 별도 클라이언트에서 업무 요율로 변환하고 호출자는 요율 또는 명시적 조회 실패를 받는다. 큰 프레임워크 없이 어댑터를 분리하는 방법도 가능하다. 실패를 무료배송으로 바꾸면 업무 결과를 왜곡한다. 실제 통신 계약은 그 경계의 통합/API 검사로 별도 확인해야 한다.
  - 예상 개념 판정: A2_OOD_04_01: CORRECT, A2_OOD_04_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-OOD-04-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: HTTP adapter가 인증·JSON·통신을 맡고 유스케이스는 업무 입출력의 조회 계약을 사용한다. 외부 DTO를 계약에 노출하지 않으며 필드 변경은 adapter에 모인다. mock으로 테스트해 보면 된다.
  - 예상 개념 판정: A2_OOD_04_01: CORRECT, A2_OOD_04_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-OOD-04-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: interface를 선언하면 그 반환값이 HTTP JSON DTO여도 모든 외부 변경이 자동 차단된다. timeout은 항상 0원으로 바꾸어야 하고 mock 통과는 실제 인증·직렬화·네트워크 정상까지 증명한다.
  - 예상 개념 판정: A2_OOD_04_01: INCORRECT, A2_OOD_04_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-OOD-04-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 유스케이스가 필요한 배송 요율 조회 계약을 업무 입력·출력으로 정하고 HTTP adapter가 인증·JSON 변환·통신을 소유한다. 업무 정책은 그 값을 받아 견적을 판단하므로 필드명 변경은 주로 adapter에 모인다. timeout·조회 불가·유효하지 않은 요율의 의미를 계약에 명시하고 모두 0원으로 숨기지 않는다. interface가 있어도 HTTP DTO가 계약에 새면 경계가 약해진다. mock 통과는 호출자 조정만 확인하며 실제 HTTP 인증·직렬화·외부 응답·DB나 네트워크 정상까지 증명하지 않는다.
  - 예상 개념 판정: A2_OOD_04_01: NEEDS_REVIEW, A2_OOD_04_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** 이문항은설계제안. 특정 아키텍처 이름만 쓰면 정답으로 보지 않고 변경축·실패의미·검증경계로판단.

**출처 대조 위치**

- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — Chapter 9: abstract contract and implementation
- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html) — Domain logic and service coordination. port·adapter·실패계약은자체설계

근거 문서: `A2-OOD-DOC`. 검수 상태: **PENDING**

### A2-OOD-05 · BASIC

Money는 amount와 currency로 업무상 같음을 판단한다. Order는 고유 orderId로 같음을 판단한다. Money(100,KRW) 둘과 Money(100,USD), 같은 총액의 서로 다른 orderId 주문을 비교하라. 값 객체를 불변으로 둘 이유와 final 필드만으로 충분한지도 설명하라.

**모범 답안**

두 Money(100,KRW)는 업무값이 같고 Money(100,USD)는 통화가 달라 같지 않다. 총액이 같아도 서로 다른 orderId 주문은 다른 엔티티다. 같은 값과 같은 대상 식별을 구별한다. 값 객체를 불변으로 두면 공유 참조의 변경으로 다른 사용자의 의미가 바뀌는 위험을 줄이고 새 값으로 교체할 수 있다. final은 참조 재대입만 막으므로 가변 내부 컬렉션·객체가 있다면 방어적 복사나 불변 구성까지 필요하다. 값 동등성 정의는 업무 전제에 맞춰 명시해야 한다.

**필수 개념**

- 값 동등성과 엔티티 식별 · 가중치 0.50: Money의 amount+currency와 Order의 orderId를 구별, 같은 값·다른 통화·다른 주문 사례 판정.
- 불변 값과 참조 변경의 경계 · 가중치 0.50: 공유 가변 위험·새 값 교체, final 재대입 금지와 내부 객체 가변성 구별·방어 복사/불변 구성 대안.

**개념별 판정 경계**

- `A2_OOD_05_01`
  - CORRECT: Money의 amount+currency와 Order의 orderId를 구별, 같은 값·다른 통화·다른 주문 사례 판정.
  - PARTIALLY_CORRECT: 값/신원 구별만 말하고 통화·주문 사례 누락.
  - INCORRECT: 수량만으로 통화 구별 삭제 또는 총액만으로 주문 신원 동일시.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.
- `A2_OOD_05_02`
  - CORRECT: 공유 가변 위험·새 값 교체, final 재대입 금지와 내부 객체 가변성 구별·방어 복사/불변 구성 대안.
  - PARTIALLY_CORRECT: 불변 이점만 말하고 final의 한계·구성 대안 누락.
  - INCORRECT: final 참조면 내부 가변 객체도 자동 불변이라고 주장.
  - NEEDS_REVIEW: 필수 근거가 없거나 질문의 전제와 제공 근거의 적용 조건이 달라 대조할 수 없음. 답변이 틀렸다는 판정과 구분.

**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**

- `A2-OOD-05-correct` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 두 Money(100,KRW)는 업무값이 같고 Money(100,USD)는 통화가 달라 같지 않다. 총액이 같아도 서로 다른 orderId 주문은 다른 엔티티다. 같은 값과 같은 대상 식별을 구별한다. 값 객체를 불변으로 두면 공유 참조의 변경으로 다른 사용자의 의미가 바뀌는 위험을 줄이고 새 값으로 교체할 수 있다. final은 참조 재대입만 막으므로 가변 내부 컬렉션·객체가 있다면 방어적 복사나 불변 구성까지 필요하다. 값 동등성 정의는 업무 전제에 맞춰 명시해야 한다.
  - 예상 개념 판정: A2_OOD_05_01: CORRECT, A2_OOD_05_02: CORRECT
  - 이유: 모든 필수 사실·조건·선택 이유 충족.
- `A2-OOD-05-alternative` · 근거 조건 `DOCUMENT` · 예상 전체 `CORRECT`
  - 답변: 금액은 수량과 통화의 쌍으로 같음을 판단하고 주문은 신원으로 판단한다. KRW100과 USD100은 교환 계산 전의 다른 값이다. 값의 수정 대신 새 Money를 만들어 교체하면 공유자의 뜻이 바뀌지 않는다. final List를 두는 것만으로 리스트 내용까지 고정되지는 않는다.
  - 예상 개념 판정: A2_OOD_05_01: CORRECT, A2_OOD_05_02: CORRECT
  - 이유: 표현이나 유효한 해법이 달라도 계약·조건 충족. 키워드 일치만으로 채점 금지.
- `A2-OOD-05-partial` · 근거 조건 `DOCUMENT` · 예상 전체 `PARTIALLY_CORRECT`
  - 답변: KRW100끼리는 같고 USD100은 다르다. 같은 총액도 orderId가 다르면 주문은 다르다. 값 객체는 불변으로 두면 공유 변경을 줄일 수 있다.
  - 예상 개념 판정: A2_OOD_05_01: CORRECT, A2_OOD_05_02: PARTIALLY_CORRECT
  - 이유: 첫 개념은 충족, 마지막 개념의 조건·경계 일부 누락. 이미 맞은 개념은 유지.
- `A2-OOD-05-incorrect` · 근거 조건 `DOCUMENT` · 예상 전체 `INCORRECT`
  - 답변: 숫자 100이 같으면 KRW와 USD도 같고 총액이 같은 주문은 orderId와 무관하게 동일 엔티티다. final 참조가 있으면 가변 리스트 원소도 자동으로 변경 불가가 되어 방어적 복사는 필요 없다.
  - 예상 개념 판정: A2_OOD_05_01: INCORRECT, A2_OOD_05_02: INCORRECT
  - 이유: 두 필수 개념의 핵심 사실을 각각 뒤집음. 단순 누락과 구분.
- `A2-OOD-05-missing-evidence` · 근거 조건 `MISSING_REQUIRED_CONCEPTS` · 예상 전체 `NEEDS_REVIEW`
  - 답변: 두 Money(100,KRW)는 업무값이 같고 Money(100,USD)는 통화가 달라 같지 않다. 총액이 같아도 서로 다른 orderId 주문은 다른 엔티티다. 같은 값과 같은 대상 식별을 구별한다. 값 객체를 불변으로 두면 공유 참조의 변경으로 다른 사용자의 의미가 바뀌는 위험을 줄이고 새 값으로 교체할 수 있다. final은 참조 재대입만 막으므로 가변 내부 컬렉션·객체가 있다면 방어적 복사나 불변 구성까지 필요하다. 값 동등성 정의는 업무 전제에 맞춰 명시해야 한다.
  - 예상 개념 판정: A2_OOD_05_01: NEEDS_REVIEW, A2_OOD_05_02: NEEDS_REVIEW
  - 이유: 정답 사례와 같은 답변이지만 필요한 근거가 전부 없음. 근거 없이 정답을 확정하는지 확인할 작성자 기대값.

**혼동 주의:** equals/hashCode·영속ID생성시점·환율/반올림의 구체 구현은 추가 검수 범위. initial-v1의record 가변 참조와 개념 중첩을 독립 표본에서 구분.

**출처 대조 위치**

- [Martin Fowler — Value Object](https://martinfowler.com/bliki/ValueObject.html) — ValueObject: value/reference identity; immutability
- [Java Language Specification 21 — Chapter 4 Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — §4.12.4 final Variables

근거 문서: `A2-OOD-DOC`. 검수 상태: **PENDING**

## 검색에 제공할 근거 문서

### A2-FND-DOC

표현·단위·조건부터 고정하는 설명 · 단위·논리·로그·표본 통계; 정수 사례는 Java 21

# 표현·단위·조건부터 고정하는 설명

기준: 단위·논리·로그·표본 통계; 정수 사례는 Java 21. 직접 작성한 학습 근거 초안. 사람 검수·공개 승인 대기.

```text
bit/B·Mi/M → 계산 단위 일치
논리식 → 반례 입력 → 허용 결과
평균·백분위 → 표본·분모 → 적용 범위
```

## 사례별 판단 근거

### A2-FND-01

전제: 압축·헤더·재전송 없이 파일 10 MiB를 일정한 100 Mbit/s로 보낸다고 가정한다. 1바이트는 8비트다. 전송 시간은 얼마인가? 실제 다운로드 시간에도 같은 값을 단정할 수 있는지 설명하라.

10 MiB는 10×2^20바이트, 즉 83,886,080비트다. 100 Mbit/s는 초당 100,000,000비트이므로 이 모델의 시간은 0.8388608초다. MiB와 십진 Mbit/s를 구별하고 비트로 단위를 맞춰 나눈다. 실제 측정에서는 연결 준비·프로토콜 비용·재전송·다른 트래픽과 유효 전송률을 확인해야 하므로 이 이상화 값을 그대로 단정할 수 없다.

판단 범위: 대문자 B와 소문자 bit, Mi와 M의 차이를 먼저 확인. 계산값은 실제 네트워크 실측이 아님.

개념별 핵심:

- A2_FND_01_01 · 이진 용량과 십진 속도의 단위: MiB=2^20 B, Mbit/s=10^6 bit/s, 8 bit/B를 적용해 0.8388608초 계산.
- A2_FND_01_02 · 이상화 계산과 실제 전송의 경계: 추가 비용 없는 일정 속도 모델과 실제 연결·헤더·재전송·유효 속도를 구별하며 현실 시간 단정 거부.

근거 대조:

- [NIST — Prefixes for binary multiples](https://physics.nist.gov/cuu/Units/binary.html) — Prefixes for binary multiples: MiB·Mbit 비교

### A2-FND-02

전제: Java 21에서 int count = 2147483647이다. long a = count + 1;과 long b = (long) count + 1;의 값이 왜 다른가? 계산 뒤 long으로 바꾸는 방식이 충분한지 설명하라.

a는 -2147483648L, b는 2147483648L이다. 첫 식의 덧셈은 두 피연산자가 int이므로 int로 실행되어 범위를 넘긴 뒤 그 결과를 long에 대입한다. 이미 잃은 상위 비트는 대입으로 복구되지 않는다. 두 번째 식은 계산 전에 한 피연산자를 long으로 바꾸어 long 덧셈을 한다. 이 입력은 long 범위 안이지만 long도 유한하므로 모든 입력의 오버플로가 사라지는 것은 아니다.

판단 범위: Java의 규칙을 C/C++ 등 다른 언어의 signed overflow에 일반화하지 않음.

개념별 핵심:

- A2_FND_02_01 · 표현 범위와 연산 시점: int 덧셈의 범위 초과 후 long 확대를 구별하여 a=-2147483648, b=2147483648 설명.
- A2_FND_02_02 · 계산 전 확대와 남은 한계: 피연산자를 계산 전에 long으로 확대하거나 1L 사용. 계산 뒤 확대의 복구 불가·long 범위 한계 명시.

근거 대조:

- [Java Language Specification 21 — Chapter 4 Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — §4.2.1 Integer Types and Values; §4.2.2 Integer Operations
- [Java Language Specification 21 — Chapter 15 Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — §15.18.2 Additive Operators for Numeric Types

### A2-FND-03

전제: 접근 허용 조건은 “활성 회원이면서, 소유자이거나 관리자”다. 순수 boolean active, owner, admin만 사용한다. (active && owner) || admin이 조건을 만족하는가? 올바른 식과 반례 하나를 제시하라.

올바른 식은 active && (owner || admin)이다. 기존 식은 admin=true이면 active=false여도 허용한다. 예를 들어 active=false, owner=false, admin=true이면 기존 식은 true이고 요구한 식은 false다. 괄호는 활성 조건이 소유자·관리자 양쪽에 공통으로 적용됨을 드러낸다. 모든 입력 조합의 진리표로 두 식을 비교할 수 있다.

판단 범위: 운영 인가 기능 구현이 아닌 순수 논리 사례. 단락 평가의 부수 효과는 문항에서 제외.

개념별 핵심:

- A2_FND_03_01 · 논리 조건의 공통 전제: active가 owner·admin 양쪽에 적용되는 식 또는 동치 식 제시.
- A2_FND_03_02 · 반례로 조건 차이 검증: active=false, admin=true인 구체 입력에서 기존 true·요구 false 비교.

근거 대조:

- [Java Language Specification 21 — Chapter 15 Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — §15.23 Conditional-And; §15.24 Conditional-Or. 업무 허용식은 문항 자체 계약

### A2-FND-04

전제: 후보가 1,048,576개다. 단계마다 후보 수를 정확히 절반으로 줄여 1개가 될 때까지 반복한다. 몇 단계가 필요한가? 후보 수를 두 배로 늘릴 때 단계 수와, 매 단계의 실제 시간이 어떻게 달라지는지 구분하라.

1,048,576=2^20이므로 20번 줄이면 1개가 된다. 후보를 두 배로 늘린 2^21에서는 21단계로 하나만 늘어난다. 이는 반복 횟수의 로그 증가 설명이다. 매 단계 작업이 O(1)일 때 전체를 O(log n)으로 분석할 수 있다. 매번 후보를 복사·순회하면 단계 비용이 일정하지 않으므로 단계 수만으로 실행 시간이 같은 비율로 늘어난다고 단정할 수 없다.

판단 범위: 후보 축소 20단계와 특정 이진 탐색 구현의 비교 횟수는 별개. 마지막 비교를 더하는 구현도 허용.

개념별 핵심:

- A2_FND_04_01 · 절반 축소와 로그 단계: 2^20→1에 20단계, 입력 두 배는 한 단계 증가 설명.
- A2_FND_04_02 · 단계 수와 단계 비용: O(log n) 실행 시간의 일정한 단계 비용 전제와 복사·순회 반례 구별.

근거 대조:

- [Princeton Algorithms 4e — Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/) — Order-of-growth classifications; binary search 사례

### A2-FND-05

전제: 각각 100건의 응답 시간이다. A는 전부 20ms, B는 94건이 10ms이고 6건이 530/3ms다. p95는 오름차순 ceil(0.95×100)번째 값으로 정한다. 평균과 p95를 비교하고, 이 표본으로 전체 사용자의 미래 품질을 확정할 수 있는지 설명하라.

A 평균과 p95는 20ms다. B의 합은 94×10+6×530/3=2000ms이므로 평균도 20ms지만 95번째 값은 530/3ms, 약176.67ms다. 같은 평균이 느린 꼬리를 숨긴다. p95는 95% 지점의 값이지 가장 느린 5%의 평균이 아니다. 주어진 100건·계산법의 결과이며 측정 기간·요청 종류·실패 포함 여부·표본 대표성을 확인하지 않고 미래 전체 사용자 품질을 확정할 수 없다.

판단 범위: nearest-rank는 이 문항의 명시적 계산 계약. 모든 관측 도구가 동일한 보간법을 쓰는 것은 아님.

개념별 핵심:

- A2_FND_05_01 · 평균과 백분위의 다른 정보: 두 평균20ms, A p95=20ms·B p95=530/3ms 계산. 순위값과 꼬리 평균 구별.
- A2_FND_05_02 · 표본 통계의 적용 범위: 표본·기간·요청/실패 분모·대표성을 명시하고 미래 전체 사용자 품질 단정 거부.

근거 대조:

- [Google SRE Book — Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/) — Worrying About Your Tail (or, Instrumentation and Performance)

## 확인·판정 한계

질문의 명시적 전제와 이 문서의 근거를 함께 확인. 다른 언어·버전·배치·업무 계약에 결과를 그대로 적용하지 않음. 설계 사례는 조건을 충족하는 여러 대안을 허용. 필요한 개념의 근거가 검색 결과에서 빠지면 판정 보류. 이 문서의 존재·구조 검사 통과는 사람 검수·검색 적합성·모델 품질을 증명하지 않음.

### A2-ARCH-DOC

성능을 하드웨어 작업과 비용으로 설명 · Cornell CS3410 2024; OSTEP 1.10의 하드웨어·I/O 모델

# 성능을 하드웨어 작업과 비용으로 설명

기준: Cornell CS3410 2024; OSTEP 1.10의 하드웨어·I/O 모델. 직접 작성한 학습 근거 초안. 사람 검수·공개 승인 대기.

```text
주소 변환: 가상 주소 → TLB/페이지 테이블 → 물리 주소
데이터 접근: 물리 주소 → 데이터 캐시 → RAM
I/O: CPU 요청 설정 → 장치/DMA 작업 → 완료 처리
```

## 사례별 판단 근거

### A2-ARCH-01

전제: 고정된 작업이 1코어에서 100초 걸린다. 그중20초는 병렬화할 수 없고80초는4코어에 완벽히 나눌 수 있다. 추가 비용은 없다. 4코어 시간·속도 향상과 코어를 무한히 늘릴 때의 한계를 설명하라.

4코어 시간은20+80/4=40초, 속도 향상은100/40=2.5배다. 직렬20초는 남으므로 코어를 무한히 늘려도 시간은20초에 접근하고 향상은5배에 접근한다. 고정 입력·직렬 비율·완벽한 분배·추가 비용 없음이라는 모델이다. 실제 동기화·스케줄링·메모리 대역폭 경쟁이 있으면 이 이상값대로 빨라진다고 보장하지 않는다.

판단 범위: 고정 작업량의 Amdahl 모델. 입력 자체를 늘리는 확장 모델·서비스 처리량과 구분.

개념별 핵심:

- A2_ARCH_01_01 · 직렬 구간과 병렬 속도 한계: 20+80/4=40초·2.5배, 무한 코어20초·5배 한계 계산.
- A2_ARCH_01_02 · 성능 모델의 가정과 실측: 고정 입력·완벽 분배·추가 비용 없음과 실제 동기화·메모리 경쟁 구별.

근거 대조:

- [Cornell CS3410 Fall 2024 — Performance in Parallel Programming](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/parallel-perf.html) — Performance in Parallel Programming: Scalability; Amdahl’s law

### A2-ARCH-02

전제: 같은 수의 int 값을 한 번 합한다. A는 연속 메모리 배열을 앞에서부터 읽고 B는 메모리 여러 곳에 흩어진 연결리스트 노드를 따른다. 둘 다 O(n)인데도 시간이 다를 수 있는 이유와 비교할 때 확인할 조건을 설명하라.

연속 배열은 가까운 주소를 연이어 읽으므로 캐시 라인에 함께 들어온 다음 값도 활용하는 공간 지역성이 높다. 흩어진 노드는 포인터를 따라 다음 주소를 알아내며 새 캐시 라인을 읽을 수 있다. 같은 O(n)은 증가 차수만 같다는 뜻으로 캐시 미스·메모리 접근 비용까지 같지는 않다. 배치·원소 크기·캐시 상태·데이터 규모를 맞춰 미스와 시간을 측정해야 하며 특정 구현의 배열 승리를 언제나 보장하지 않는다.

판단 범위: JVM 객체의 실제 배치가 C의 연속 int 배열과 같다는 가정은 하지 않음. 문항에 저장 배치 명시.

개념별 핵심:

- A2_ARCH_02_01 · 차수와 메모리 지역성: O(n)의 동일 차수와 캐시 라인·공간 지역성·포인터 의존 접근 비용 차이 구별.
- A2_ARCH_02_02 · 배치와 캐시 상태를 맞춘 비교: 노드 배치·원소 크기·캐시 상태·입력 규모를 확인하고 배열의 절대 우위 단정 거부.

근거 대조:

- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html) — The principle of locality: temporal/spatial locality; cache lines
- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf) — Array Sequence; Linked List Sequence: 저장 형태

### A2-ARCH-03

전제: 단일 캐시를 순차로 확인한다. hit는 총1ns, miss는 캐시 확인1ns 뒤 메모리에서 추가50ns가 든다. hit율95%다. 평균 접근 시간은? hit율이 같아도 실제 프로그램 시간이 같지 않을 수 있는 조건을 설명하라.

평균은0.95×1+0.05×(1+50)=3.5ns다. miss의50ns는 캐시 확인 뒤 추가 비용이므로0.95×1+0.05×50=3.45ns로 계산하면 확인 비용을 빠뜨린다. 이 모델은 순차 접근의 평균이다. 실제 다층 캐시·중첩된 메모리 요청·접근 횟수·작업 종류가 다르면 같은 hit율도 전체 실행 시간을 정하지 못한다.

판단 범위: 추가 miss penalty인지 miss 전체 시간인지 전제가 바뀌면 식도 달라짐.

개념별 핵심:

- A2_ARCH_03_01 · 캐시 평균 비용과 추가 지연: 모든 확인1ns+miss5%×추가50ns=3.5ns. 총 miss 비용51ns 인식.
- A2_ARCH_03_02 · 평균 접근 모델과 프로그램 시간: 순차 단일 캐시 모델을 다층·중첩·접근 횟수·작업 차이와 구분.

근거 대조:

- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html) — Cache performance: average access time; multi-level caches

### A2-ARCH-04

전제: 유효하고 읽기 가능한 페이지가 이미 RAM에 있다. 해당 가상 페이지의 주소 변환만 TLB에 없다. 하드웨어가 페이지 테이블을 조회하는 모델에서 이 접근은 반드시 디스크 읽기인가? TLB miss·데이터 캐시 miss·페이지 부재를 구별하라.

디스크 읽기가 필수인 상황이 아니다. TLB는 가상→물리 주소 변환을 캐시하므로 miss면 메모리에 있는 페이지 테이블에서 유효한 변환을 찾고 TLB를 채울 수 있다. 데이터 캐시는 해당 물리 주소의 데이터를 캐시하므로 변환 hit와 데이터 hit는 별개다. 페이지 자체가 RAM에 없는 상황의 처리는 별도다. TLB miss만으로 디스크 I/O·모든 page fault를 동일시하면 안 된다.

판단 범위: 문항은 유효·권한 허용·RAM 상주 전제. 특정 ISA의 fault 명칭·OS 정책까지 일반화하지 않음.

개념별 핵심:

- A2_ARCH_04_01 · 주소 변환 캐시의 miss 처리: TLB의 변환 책임·유효한 page table 조회·재시도 설명. 이 조건에서 디스크 필수 아님.
- A2_ARCH_04_02 · 변환·데이터·페이지 부재의 경계: TLB hit와 데이터 hit 독립, RAM 페이지 부재는 별도 조건. fault와 디스크 읽기 일대일 단정 거부.

근거 대조:

- [OSTEP 1.10 — Chapter 19 Paging: Faster Translations (TLBs)](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-tlbs.pdf) — Chapter 19 §19.1 TLB Basic Algorithm; §19.4 TLB Contents
- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html) — Cache line and memory hierarchy: data accesses

### A2-ARCH-05

전제: 큰 블록을 장치로 보낼 때 CPU가 상태 레지스터를 반복 조회하고 데이터도 한 단어씩 복사한다. interrupt와 DMA는 각각 어느 CPU 작업을 줄이는가? 두 기법을 쓰면 장치 자체 지연이 없어지는지 설명하라.

interrupt는 완료를 기다리며 상태를 계속 polling하는 CPU 비용을 줄이고, DMA는 설정 후 장치와 메모리 사이 데이터 이동을 담당해 CPU의 단어별 복사를 줄인다. CPU는 요청 설정·완료 처리·버퍼 수명 관리에 여전히 관여한다. 장치 지연·버스 대역폭은 남으므로 두 기법이 I/O를 즉시 완료시키지 않는다. 매우 짧은 작업·빈번한 interrupt에서는 polling이나 묶음 처리의 비용을 비교해야 한다.

판단 범위: DMA가0비용이라는 결론 금지. 동시 접근·버퍼 재사용·cache coherence의 자세한 구현은 이 첫 문항 밖 범위.

개념별 핵심:

- A2_ARCH_05_01 · 완료 알림과 데이터 이동의 역할: interrupt는 polling 대기, DMA는 CPU의 단어별 복사 감소. 설정·완료 처리의 CPU 관여 유지.
- A2_ARCH_05_02 · CPU 절감과 I/O 지연의 차이: 장치·버스 지연 유지, 짧은 작업·interrupt 빈도 등 비용 조건에서 polling·묶음 처리와 비교.

근거 대조:

- [OSTEP 1.10 — Chapter 36 I/O Devices](https://pages.cs.wisc.edu/~remzi/OSTEP/file-devices.pdf) — Chapter 36 §36.3 Canonical Protocol; §36.4 Interrupts; §36.5 DMA

## 확인·판정 한계

질문의 명시적 전제와 이 문서의 근거를 함께 확인. 다른 언어·버전·배치·업무 계약에 결과를 그대로 적용하지 않음. 설계 사례는 조건을 충족하는 여러 대안을 허용. 필요한 개념의 근거가 검색 결과에서 빠지면 판정 보류. 이 문서의 존재·구조 검사 통과는 사람 검수·검색 적합성·모델 품질을 증명하지 않음.

### A2-DS-DOC

연산 계약을 저장 방식과 연결 · MIT 6.006 Spring 2020; Princeton Algorithms 4th Edition

# 연산 계약을 저장 방식과 연결

기준: MIT 6.006 Spring 2020; Princeton Algorithms 4th Edition. 직접 작성한 학습 근거 초안. 사람 검수·공개 승인 대기.

```text
무엇을 빨리 할 것인가? → 저장 표현 선택
위치 탐색 + 실제 변경 → 전체 비용
불변식 유지 → 정상 조회·변경
```

## 사례별 판단 근거

### A2-DS-01

전제: n개 원소에서 인덱스 i의 값을 읽고 그 뒤에 하나를 삽입한다. 여유 용량이 있는 배열과 단일 연결리스트를 비교하라. 연결리스트는 머리만 아는 경우와 i번째 노드의 참조를 이미 가진 경우를 나누고, i는 중간 위치라고 가정한다.

배열은 인덱스 접근 O(1)이지만 중간 삽입은 뒤 원소를 옮겨야 하므로 O(n)이다. 여유 용량은 재할당을 피할 뿐 이동을 없애지 않는다. 단일 연결리스트는 머리에서 i번째 노드를 찾는 데 O(n), 찾은 노드 뒤 연결을 바꾸는 데 O(1)이다. 이미 그 노드를 알면 탐색 없이 O(1) 삽입이 가능하다. 위치 탐색과 실제 변경을 나눠 비교해야 한다.

판단 범위: 삭제는 앞 노드 정보 등 다른 조건이 필요. 뒤 삽입의 결과를 모든 리스트 연산으로 확대하지 않음.

개념별 핵심:

- A2_DS_01_01 · 배열 접근과 중간 삽입 비용: 배열 읽기 O(1)·중간 삽입 O(n), 여유 용량과 이동 비용 구별.
- A2_DS_01_02 · 위치 탐색과 링크 변경의 분리: 머리부터 위치 탐색 O(n), 알려진 노드 뒤 링크 변경 O(1) 구별.

근거 대조:

- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf) — Array Sequence; Linked List Sequence: get_at·insert_at
- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — Linked lists: insertion and links

### A2-DS-02

전제: 단일 스레드 FIFO 대기열에 A,B,C를 넣고 하나를 꺼낸다. 용량4인 원형 배열에서 head와 tail이 같은 경우가 빈 상태인지 꽉 찬 상태인지 구별하려면 무엇이 필요한가? 크기 필드 또는 한 칸 비우기 정책을 허용한다.

FIFO이므로 A가 먼저 나온다. stack의 LIFO와 달리 먼저 들어온 항목을 먼저 꺼낸다. 원형 인덱스만으로 head=tail이면 빈 상태와 꽉 찬 상태가 겹칠 수 있다. size를 저장해0과4를 구별하거나 한 칸을 비워 next(tail)=head를 full로 정의할 수 있다. 한 칸 비우기를 택하면 물리 용량4의 사용 가능 용량은3이다. enqueue/dequeue 후 인덱스와 크기를 함께 갱신해야 한다.

판단 범위: 멀티스레드의 visibility·경쟁·lock-free 보장은 이 문항의 단일 스레드 계약 밖.

개념별 핵심:

- A2_DS_02_01 · FIFO와 LIFO의 제거 순서: A,B,C 입력의 FIFO 첫 출력 A. stack의 최근 입력 우선과 구별.
- A2_DS_02_02 · 원형 큐의 full·empty 상태 계약: size로0·4 구별 또는 한 칸 비우기의 usable3·full 조건 제시. 인덱스/크기 갱신 책임 설명.

근거 대조:

- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — FIFO queues; Array resizing: queue. 원형 상태 정책은 문항에서 허용한 설계

### A2-DS-03

전제: 별도 연결리스트로 충돌을 처리하는 해시 테이블에 서로 다른 키 n개를 넣는다. 모든 키가 같은 bucket에 들어가고, 키 비교와 해시 계산은 O(1)이다. 해시가 같다는 이유로 앞 값을 덮어써도 되는가? 없는 키 조회 비용과 배열 확장만으로 해결되는지 설명하라.

같은 해시와 같은 키는 다르다. bucket 안에서 동등성 비교로 실제 키를 구별하고, 다른 키의 값을 덮어쓰면 안 된다. 없는 키가 같은 bucket에 대응하면 n개를 모두 비교하므로 최악 O(n)이다. 평균 O(1)은 분산과 load factor 등의 조건에 의존한다. 원래 hash 값 자체가 모두 같다면 배열을 크게 해도 같은 bucket으로 모이므로 확장만으로 해소되지 않는다. 해시 분산·충돌 정책을 함께 확인한다.

판단 범위: Java HashMap의 treeification 계약을 이 문항의 연결리스트 구현에 가져와 O(log n)으로 대체하지 않음.

개념별 핵심:

- A2_DS_03_01 · 충돌과 키 동등성의 구별: 같은 hash인 다른 키를 보존하고 bucket 안의 실제 키 비교로 식별.
- A2_DS_03_02 · 해시 조회 비용의 분산 조건: 한 bucket absent lookup O(n), 평균 O(1)의 분산/적재율 조건·동일 hash에 확장만으로 부족 설명.

근거 대조:

- [Princeton Algorithms 4e — Hash Tables](https://algs4.cs.princeton.edu/34hash/) — Hash functions: consistency; Assumption J; Hashing with separate chaining

### A2-DS-04

전제: 서로 다른 점수 8,2,6,10,4가 차례로 온다. 가장 큰3개만 보관하려고 용량을 미리 확보한 min-heap을 쓴다. 최종 점수 집합·루트의 의미·새 점수 처리와 비용을 설명하라. 힙 배열이 전체 오름차순인지도 답하라.

최종 집합은{6,8,10}이고 루트6은 보관한 큰3개 중 가장 작다. 처음3개는 넣고, 이후 루트보다 큰10은2를 교체하며4는6 이하라 버린다. 가득 찬 뒤 루트 비교는 O(1), 교체 후 복구는 O(log K)이고 저장은 O(K)다. min-heap은 부모≤자식 조건으로 루트 최소를 보장할 뿐 형제·전체 배열 정렬을 보장하지 않는다. 내림차순 출력은 별도 정렬이나 반복 추출이 필요하다.

판단 범위: K≥1·서로 다른 점수·용량 확보 전제. 동점 처리와 배열 재할당 비용은 별도.

개념별 핵심:

- A2_DS_04_01 · 상위 K개의 최소 경계 유지: 루트는 선택 집합 최소, 새 값과 비교·교체하여{6,8,10}. 비교O(1)·복구O(log K)·저장O(K).
- A2_DS_04_02 · 힙 순서와 전체 정렬의 경계: 부모≤자식의 부분 순서·전체 정렬 불보장, 출력용 추가 작업 구별.

근거 대조:

- [Princeton Algorithms 4e — Priority Queues](https://algs4.cs.princeton.edu/24pq/) — Heap definitions; heap-order maintenance; TopM client; resizing cost caveat

### A2-DS-05

전제: 단순 무방향 그래프의 정점은1000개, 간선은2000개다. 인접 리스트와1000×1000 인접 행렬을 비교하라. 저장 규모와 한 정점의 모든 이웃 열거 비용, 특정 두 정점의 연결 검사 비용이 왜 다른가?

리스트는 정점1000개와 양방향 간선 항목4000개 등 O(V+E) 공간을 쓰고, 한 정점의 이웃 열거는 O(degree(v))다. 행렬은100만 칸의 O(V^2) 공간과 이웃 열거 O(V)를 쓴다. 반면 특정 연결은 행렬에서 O(1)이고 정렬되지 않은 리스트는 해당 이웃 목록을 훑어 O(degree(v))다. 이 희소 그래프의 전체 순회에는 리스트가 적합할 수 있지만 연결 검사 빈도·추가 인덱스·밀도를 함께 보고 선택한다.

판단 범위: 행렬의 칸 수는 실제 메모리 byte 수가 아님. self-loop·평행 간선은 문항에서 제외.

개념별 핵심:

- A2_DS_05_01 · 그래프 표현과 이웃 열거 규모: 무방향 리스트2E·O(V+E)와 행렬V^2·O(V^2), 열거 O(degree)와 O(V) 구별.
- A2_DS_05_02 · 연산 요구에 따른 표현 선택: 연결 검사 행렬O(1)·일반 리스트O(degree), 질의 빈도·밀도·추가 인덱스에 따른 대안 허용.

근거 대조:

- [Princeton Algorithms 4e — Undirected Graphs](https://algs4.cs.princeton.edu/41graph/) — Graph representations: adjacency matrix and adjacency lists

## 확인·판정 한계

질문의 명시적 전제와 이 문서의 근거를 함께 확인. 다른 언어·버전·배치·업무 계약에 결과를 그대로 적용하지 않음. 설계 사례는 조건을 충족하는 여러 대안을 허용. 필요한 개념의 근거가 검색 결과에서 빠지면 판정 보류. 이 문서의 존재·구조 검사 통과는 사람 검수·검색 적합성·모델 품질을 증명하지 않음.

### A2-ALG-DOC

실행 순서·반례·복잡도로 알고리즘 검증 · MIT 6.006 Spring 2020; Princeton Algorithms 4th Edition

# 실행 순서·반례·복잡도로 알고리즘 검증

기준: MIT 6.006 Spring 2020; Princeton Algorithms 4th Edition. 직접 작성한 학습 근거 초안. 사람 검수·공개 승인 대기.

```text
입력 전제 → 유지할 조건 → 구간/상태 축소
종료 조건 → 기대 결과 → 반례 입력
전체 연산 수 × 연산 비용 → 시간·공간 분석
```

## 사례별 판단 근거

### A2-ALG-01

전제: 오름차순 배열[2,4,4,4,9]에서 값4의 첫 인덱스를0부터 찾는다. lower_bound를 반열린 구간[lo,hi)로 구현할 때 같은 값을 만나면 어디를 줄이는가? 없는 값·빈 배열을 처리하는 조건과 정렬되지 않은 입력의 문제를 설명하라.

lo=0, hi=n에서 mid 값을 비교한다. a[mid]<target이면 lo=mid+1, 그렇지 않으면 hi=mid로 줄여 첫 target 이상 위치를 찾는다. 같다고 즉시 반환하면 뒤의4를 반환할 수 있다. 종료 후 lo<n이고 a[lo]==target이면 이 배열에서1을 반환하고, 아니면 없음이다. 빈 배열은 처음부터 lo=hi=0이라 접근하지 않는다. 이 논리는 정렬된 값의 단조성에 의존하므로 무정렬 입력에는 그대로 적용할 수 없다.

판단 범위: 선행 정렬의 시간과 원래 순서 보존 문제는 탐색 O(log n)과 별도. 정렬로 원래 첫 위치를 바꾸지 않도록 주의.

개념별 핵심:

- A2_ALG_01_01 · 단조성에 근거한 구간 축소: 오름차순 전제·반열린 구간에서 <면 lo=mid+1, 그 외hi=mid 설명.
- A2_ALG_01_02 · 첫 일치와 종료 경계: 동일 값 즉시 반환 금지·종료lo=1, lo<n 및 값 동일성 확인. 빈 배열 접근 없음.

근거 대조:

- [Princeton Algorithms 4e — Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/) — Binary search examples. lower_bound 규칙·반열린 구간은 문항의 알고리즘 계약

### A2-ALG-02

전제: 연속한 두 반쪽을 재귀 정렬 후 O(n)에 병합하는 배열 merge sort다. 별도 O(n) 버퍼를 재사용한다. 점수만 비교하는 입력[(3,A),(1,B),(3,C),(1,D)]에서 동점의 입력 순서를 보존하려면 병합 중 동점일 때 어느 쪽을 먼저 가져오는가? 시간·추가 공간도 설명하라.

반쪽 둘을 정렬하는 비용과 선형 병합이 반복되어 O(n log n) 시간, 재사용 버퍼 O(n)과 재귀 O(log n)의 추가 공간이 든다. 양쪽이 내부 동점 순서를 유지한다면 동점에서 왼쪽을 먼저 가져와 전체 입력 순서를 보존한다. 결과는[(1,B),(1,D),(3,A),(3,C)]다. 오른쪽을 먼저 고르면 원래 뒤의 항목이 앞 항목을 넘어갈 수 있으므로 점수 정렬만으로 안정성을 보장하지 않는다.

판단 범위: 이 문항의 배열+버퍼 모델. 리스트 merge sort 등 다른 공간 모델을 같은 것으로 단정하지 않음.

개념별 핵심:

- A2_ALG_02_01 · 분할·병합의 시간과 공간: 깊이log n·깊이당n으로 O(n log n), 재사용 버퍼O(n)·호출 스택O(log n).
- A2_ALG_02_02 · 정렬 안정성과 동점 처리: 내부 안정성+동점 왼쪽 우선, 결과1:B,D·3:A,C, 오른쪽 우선 반례 설명.

근거 대조:

- [Princeton Algorithms 4e — Mergesort](https://algs4.cs.princeton.edu/22mergesort/) — Abstract in-place merge; Top-down mergesort; Proposition; Faster merge의 비안정 변형

### A2-ALG-03

전제: 인접 리스트 그래프에서 BFS로 시작점의 최단 경로를 찾는다. 모든 간선 비용이1일 때 queue와 방문 표시는 어떻게 쓰는가? A→D 비용10, A→B 비용1, B→D 비용1인 다른 그래프에서도 같은 BFS가 최소 비용을 찾는지 설명하라.

BFS는 시작점을 enqueue 때 방문 표시하고, dequeue한 정점의 미방문 이웃도 enqueue 때 표시해 단계별로 확장한다. 모두 비용1이면 처음 발견한 단계 수가 최소 비용이자 최소 간선 수다. 인접 리스트 전체 탐색은 O(V+E)다. 주어진 가중 그래프는 직접 A→D가 한 간선이지만 비용10, A→B→D는 두 간선이지만 비용2다. 일반 BFS는 최소 간선 수를 찾으므로 가변 비용의 최소 합을 보장하지 않는다. 비음수 가중치에는 Dijkstra 등의 조건 맞는 대안을 선택한다.

판단 범위: 방문을 늦게 표시하는 변형도 올바른 중복 방어·거리 계약을 설명하면 검수 가능. 이름만으로 감점 금지.

개념별 핵심:

- A2_ALG_03_01 · 단위 간선 BFS의 단계와 방문: queue의 단계 순서·enqueue 방문 표시·O(V+E), 단위 비용 최단 보장 설명.
- A2_ALG_03_02 · 최소 간선 수와 가중 비용의 경계: 직접10과 두 간선2 반례, 비음수 Dijkstra 등 조건 맞는 대안 허용.

근거 대조:

- [Princeton Algorithms 4e — Undirected Graphs](https://algs4.cs.princeton.edu/41graph/) — Breadth-first search: shortest path in number of edges; marked queue

### A2-ALG-04

전제: 동전1,3,4를 제한 없이 써 금액6을 최소 동전 수로 만든다. 남은 금액 이하의 가장 큰 동전부터 고르는 greedy가 최적인가? 이 입력의 반례와 모든 금액0..6에 대해 답을 만드는 DP 상태·초기값·전이를 설명하라. 시간·공간 비용도 설명하라.

greedy는4+1+1로3개지만3+3은2개이므로 이 체계에서 최적이 아니다. dp[x]를 금액x의 최소 개수로 정의하고 dp[0]=0, 아직 만들 수 없는 값은∞로 둔다. x=1부터6까지 dp[x]=min(dp[x-c]+1)로, c∈{1,3,4} 중 c≤x인 경우를 검사한다. 이전 최소 답에 동전 하나를 붙여 모든 마지막 선택을 비교하므로 dp[6]=2다. 양의 정수 동전·무제한 사용 전제이며 시간 O(A×m)·공간 O(A). 특정 화폐의 greedy 성질을 모든 동전에 일반화하지 않는다.

판단 범위: 입력 금액 A의 값에 비례하는 분석. 이진 인코딩 길이에 대해 무조건 polynomial이라는 결론 금지.

개념별 핵심:

- A2_ALG_04_01 · greedy 최적성의 반례: 4+1+1의3개와3+3의2개를 비교하며 동전 체계별 최적성 전제 구별.
- A2_ALG_04_02 · DP 상태·전이·기준 조건: dp[x] 최소 개수·dp[0]=0·∞·양의c≤x 전이·모든 마지막 선택 비교, O(A m)/O(A) 또는 동등 memoization.

근거 대조:

- [MIT 6.006 Spring 2020 — Lecture 16 Dynamic Programming Subproblems](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/28461a74f81101874a13d9679a40584d_MIT6_006S20_lec16.pdf) — Dynamic Programming Review; SRT BOT Steps: state·recurrence·base·time. 최소 동전 사례는 자체 구성

### A2-ALG-05

전제: 처음 빈 동적 배열의 용량은1이다. 꽉 차면 용량을 두 배로 늘리며 기존 원소를 모두 복사하고, 끝에 하나를 넣는다. 삭제 없이 n개를 넣을 때 특정 append의 최악 비용과 전체·상환 비용을 구별하라.

용량이 충분한 append는 O(1), 꽉 찬 순간 append는 기존 원소를 복사하므로 그 시점 원소 수에 대해 O(n)이다. n회까지의 복사량은1+2+4+…로 마지막 용량 수준보다 작고 전체 O(n)이다. 원소 삽입 n회도 O(n)이므로 n회 합계 O(n), append당 상환 O(1)이다. 이는 확률적 평균이 아니라 해당 연산열의 비용을 분산한 분석이다. 삭제·축소·선할당 등 다른 정책의 보장은 별도다.

판단 범위: 메모리 할당기·GC·wall-clock 지연 보장은 아님. O(1) 상환을 실시간 latency 상한으로 사용하지 않음.

개념별 핵심:

- A2_ALG_05_01 · 개별 append의 복사 비용: 여유 append O(1)·확장 시 기존 원소 수만큼 복사해 개별 최악 O(n).
- A2_ALG_05_02 · 연산열 전체와 상환 분석: 기하 복사 합O(n)+삽입O(n)→전체O(n)·상환O(1), 확률 평균과 구별·정책 전제 명시.

근거 대조:

- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf) — Dynamic Array Sequence; Amortized Analysis
- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — Array resizing: doubling

## 확인·판정 한계

질문의 명시적 전제와 이 문서의 근거를 함께 확인. 다른 언어·버전·배치·업무 계약에 결과를 그대로 적용하지 않음. 설계 사례는 조건을 충족하는 여러 대안을 허용. 필요한 개념의 근거가 검색 결과에서 빠지면 판정 보류. 이 문서의 존재·구조 검사 통과는 사람 검수·검색 적합성·모델 품질을 증명하지 않음.

### A2-NET-DOC

네트워크 계층의 보장과 앱의 책임 · RFC 9293·768·5681·1034·8446; TLS 1.3

# 네트워크 계층의 보장과 앱의 책임

기준: RFC 9293·768·5681·1034·8446; TLS 1.3. 직접 작성한 학습 근거 초안. 사람 검수·공개 승인 대기.

```text
DNS 이름 조회 → 연결 → TLS endpoint → 앱 요청
TCP 바이트 순서 ≠ 앱 메시지 경계
전송 성공 ≠ 업무 처리·인가 성공
```

## 사례별 판단 근거

### A2-NET-01

전제: 같은 TCP 연결에서 송신 앱이 메시지 ABC와 DEF를 두 번 write했다. 연결은 정상이고 데이터 손실 없이 수신된다. 수신 앱이 두 번 read해서 정확히 ABC, DEF를 얻는다고 보장되는가? 메시지를 나누는 방법과 EOF의 의미를 설명하라.

TCP는 순서 있는 바이트 스트림이며 앱 write의 메시지 경계를 보존하지 않는다. 수신은 AB/CD/EF처럼 쪼개지거나 ABCDEF로 합쳐질 수 있다. 앱은 길이 prefix·구분자·고정 길이 등 framing 계약에 따라 누적 버퍼에서 완성 메시지를 꺼내고 남은 바이트를 보관해야 한다. 구분자는 escape와 길이 제한 등의 규칙도 필요하다. 정상 EOF는 송신 방향의 종료이지 마지막 read가 항상 메시지 하나라는 뜻이 아니며, 미완성 메시지 상태의 EOF는 앱 프로토콜에서 처리해야 한다.

판단 범위: 정상 연결의 스트림 성질이며 수신 API별0/EOF 반환 규약은 별도. TCP ACK는 업무 처리 완료 응답이 아님.

개념별 핵심:

- A2_NET_01_01 · TCP 바이트 순서와 메시지 경계: 순서 있는 스트림과 write/read 경계 불일치·분할/합침 가능 구별.
- A2_NET_01_02 · 앱 framing과 불완전 종료: 누적 버퍼·길이/구분자 등 계약·남은 바이트·미완성EOF 처리와 입력 한계 설명.

근거 대조:

- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — §2.2 Key TCP Concepts; §3.9.1.2 Send: push does not supply a record boundary; §3.9.1.3 Receive

### A2-NET-02

전제: 한 UDP datagram씩 위치 갱신을 보낸다. 최신 위치가 오면 늦은 예전 위치를 버릴 수 있지만, 중복이나 역순 도착이 있어도 상태가 과거로 돌아가면 안 된다. UDP 자체가 이 요구를 보장하는가? 앱에 필요한 정보와, 이 선택을 결제 명령에 그대로 쓸 수 없는 이유를 설명하라.

UDP는 전달·중복 방지·순서를 보장하지 않는다. 앱은 보낸 순번이나 버전과 세션 식별을 포함하고, 현재 채택한 버전보다 새것만 적용하는 계약을 둘 수 있다. 누락은 다음 최신 값으로 보완할 수 있다는 위치 갱신 요구가 이 선택을 허용한다. 결제는 오래됐다는 이유로 명령을 버려도 되는 계약이 아니므로 요청 식별·처리 결과·재조회·중복 효과 방지 등을 따로 설계해야 한다. TCP로 바꾸는 것만으로 업무 중복이 사라지지도 않는다.

판단 범위: UDP 기반 프로토콜이 별도 신뢰성·혼잡 제어를 구현할 수 있음. UDP라는 이유만으로 모든 앱을 불신뢰로 분류하지 않음.

개념별 핵심:

- A2_NET_02_01 · UDP 보장 범위와 앱의 최신성: 손실·중복·역순 가능, 세션/버전 등 자체 최신성 계약으로 오래된 입력 거부.
- A2_NET_02_02 · 데이터 의미에 따른 손실·중복 계약: 위치 snapshot의 손실 허용과 결제 명령의 결과/중복 효과 관리 구별, TCP의 업무 exactly-once 불보장.

근거 대조:

- [RFC 768 — User Datagram Protocol](https://www.rfc-editor.org/rfc/rfc768.html) — User Datagram Protocol: delivery and duplicate protection not guaranteed
- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — §2.2 Key TCP Concepts: transport byte-stream 보장. 앱 버전·결제 계약은 자체 설계

### A2-NET-03

전제: TCP 송신에서 수신 윈도 rwnd=64KiB, 혼잡 윈도 cwnd=16KiB, 이미 ACK되지 않은 데이터는12KiB다. 단순화한 모델에서 추가로 보낼 수 있는 양과 두 윈도의 책임을 설명하라. 수신 버퍼만 늘리면 항상 더 보낼 수 있는가?

총 미확인 데이터 한도는 min(rwnd,cwnd)=16KiB이고 이미12KiB이므로 추가4KiB다. rwnd는 수신자가 처리·보관할 여유를 알리는 흐름 제어, cwnd는 네트워크 경로의 혼잡을 고려한 송신자 제한이다. 이 경우 cwnd가 더 작으므로 수신 버퍼와 rwnd만 늘려도 추가 한도는 커지지 않는다. ACK·손실·송신 pacing 등 실제 조건도 확인해야 하며 둘 중 하나만 보고 전송 가능량을 확정하지 않는다.

판단 범위: KiB 단위의 단순 계산. 전체 전송 throughput·RTT·알고리즘별 정확한cwnd 조정까지 인증하는 예제가 아님.

개념별 핵심:

- A2_NET_03_01 · 흐름 제어와 혼잡 제어의 책임: rwnd 수신 여유·cwnd 경로 혼잡, min 한도에서in-flight 차감해4KiB.
- A2_NET_03_02 · 최소 한도의 병목과 추가 조건: cwnd가 작은 현재 조건에서rwnd 증가만으로 한도 증가 불가, ACK/손실/pacing 등 별도 조건 명시.

근거 대조:

- [RFC 5681 — TCP Congestion Control](https://www.rfc-editor.org/rfc/rfc5681.html) — §2 Definitions: rwnd·cwnd; §3 Congestion Control Algorithms
- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — §3.8.6.1 Window Management

### A2-NET-04

전제: resolver가 t=0에 TTL300초인 A 레코드의 옛 IP를 저장했다. 권한 서버는 t=100에 새 IP로 바꾸며 새 레코드 TTL을30초로 정했다. prefetch·serve-stale·중간 캐시·기존 연결 없이 RFC1034 기본 TTL 모델만 쓴다. t=150 질의에서 옛 IP가 가능한 이유와, 새 TTL30이 기존 캐시를 즉시 없애는지 설명하라.

기존 캐시의 저장 수명은 t=0부터300초이므로 t=150에서 남은150초 동안 옛 레코드를 사용할 수 있다. 권한 서버의 갱신은 이미 저장된 모든 resolver 캐시를 즉시 밀어내지 않는다. 새 TTL30은 새 응답을 받아 저장할 때의 수명이지 옛 캐시에 소급되지 않는다. 이 모델에서는 t=300에 기존 항목이 만료되어 다음 조회에서 새 값을 얻을 수 있다. 변경 준비는 기존 TTL이 지나갈 시간을 고려해야 하며 DNS 갱신과 살아 있는 연결 전환을 같은 사건으로 다루지 않는다.

판단 범위: 현행serve-stale·negative cache·브라우저 캐시는 이 모델에서 제외. 기본TTL 설명을 현실의 절대 전환 시각으로 확대하지 않음.

개념별 핵심:

- A2_NET_04_01 · DNS 캐시 수명과 권한 갱신: 기존t=0+300 수명·t=150의옛값 가능, 권한 갱신과resolver 캐시 즉시 삭제 구별.
- A2_NET_04_02 · TTL 적용 시점과 전환 범위: 새30초는새로 저장하는 응답에 적용·소급 없음, 만료 후질의·기존 연결 전환 별개 설명.

근거 대조:

- [RFC 1034 — Domain Names: Concepts and Facilities](https://www.rfc-editor.org/rfc/rfc1034.html) — §3.2.1 Resource records: TTL; §4.3.4 Caching; §5.3.3 resolver algorithm

### A2-NET-05

전제: 브라우저는 신뢰 체인·호스트명·서명 검증을 정상 수행하는 인증서 기반 TLS1.3으로 프록시에 연결한다. 프록시에서 TLS를 종료하고 앱까지는 별도 네트워크의 평문 HTTP다. TLS가 보호하는 구간·프록시의 평문 관측·앱 구간과 사용자 인가의 책임을 설명하라.

TLS의 기밀성·무결성은 브라우저와 TLS endpoint인 프록시 사이에 적용된다. 검증을 정상 수행하므로 브라우저는 해당 서버 신원을 확인할 수 있지만 사용자 업무 권한을 확인한 것은 아니다. 프록시는 복호화 endpoint라 본문을 읽을 수 있다. 프록시→앱 평문 구간에는 앞 연결의 TLS 보호가 이어지지 않으므로 별도TLS·네트워크 통제 등 요구에 맞는 보호가 필요하다. 앱은 로그인·소유권·인가를 따로 확인해야 한다.

판단 범위: TLS1.3의 모든 모드가 같은 인증과 성질을 갖는다고 일반화하지 않음. PSK·0-RTT·client certificate는 이번 전제 밖.

개념별 핵심:

- A2_NET_05_01 · TLS endpoint의 보호 구간: 브라우저→프록시의기밀성·무결성, endpoint복호화·프록시평문관측 가능 설명.
- A2_NET_05_02 · 서버 신원과 앱·사용자 권한의 경계: 정상서버신원검증·사용자인가별도, 프록시→앱구간별도TLS/통제 대안과한계명시.

근거 대조:

- [RFC 8446 — The Transport Layer Security (TLS) Protocol Version 1.3](https://www.rfc-editor.org/rfc/rfc8446.html) — §1 Introduction: authentication·confidentiality·integrity; §4.4 Authentication Messages. 프록시 배치는 자체 사례

## 확인·판정 한계

질문의 명시적 전제와 이 문서의 근거를 함께 확인. 다른 언어·버전·배치·업무 계약에 결과를 그대로 적용하지 않음. 설계 사례는 조건을 충족하는 여러 대안을 허용. 필요한 개념의 근거가 검색 결과에서 빠지면 판정 보류. 이 문서의 존재·구조 검사 통과는 사람 검수·검색 적합성·모델 품질을 증명하지 않음.

### A2-OOD-DOC

상태·정보·변경 이유에 맞춘 객체 협력 · Java 21; Liskov·Wing 1994; 명시한 업무 계약의 설계 사례

# 상태·정보·변경 이유에 맞춘 객체 협력

기준: Java 21; Liskov·Wing 1994; 명시한 업무 계약의 설계 사례. 직접 작성한 학습 근거 초안. 사람 검수·공개 승인 대기.

```text
입력 → 객체의 전체 검증 → 상태 변경
정책 → 제안 값 → 상태 소유자의 최종 방어
업무 계약 ← adapter가 바깥 전송 형식 변환
```

## 사례별 판단 근거

### A2-OOD-01

전제: 기간 객체가 start≤end를 항상 유지해야 한다. 현재(2,4)에서 replacePeriod(5,3)를 호출한다. 공개 setter 두 개로 순서대로 바꾸면 어떤 문제가 생기는가? 실패 시 두 값 모두(2,4)를 유지하는 변경 방법과 규칙의 소유자를 설명하라.

start부터5로 바꾸면 중간(5,4)이 불변식을 어기고, end 검증에서 실패해도 이미 바뀐 start가 남을 수 있다. 기간을 소유한 객체가 생성·수정에서 같은 불변식을 검사하고, replacePeriod는 새 쌍 전체를 검증한 뒤 두 필드를 바꿔야 한다. 잘못된(5,3)은 변경 전 거부하여 기존(2,4)를 유지한다. 불변 기간 값 객체를 새로 만들어 성공 시 교체하는 대안도 유효하다. Controller나 Service의 검사만으로 다른 호출 경로까지 방어되지는 않는다.

판단 범위: Java private만으로 규칙이 자동 실현되는 것은 아님. DBrollback이이미변경한Java필드를 자동 복구한다는 설명도 금지.

개념별 핵심:

- A2_OOD_01_01 · 불변식과 상태 소유자의 책임: 기간 객체가 생성·수정 규칙 소유, 공개 독립 setter/외부 검사만으로 중간·우회 경로 방어 불가 설명.
- A2_OOD_01_02 · 실패 전 검증과 원상태 보존: 새 쌍 전체 검증 후 필드 변경 또는 유효한 불변 값 교체, (5,3) 실패 시 (2,4) 유지.

근거 대조:

- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html) — Anemic Domain Model: behavior and domain logic
- [Java Language Specification 21 — Chapter 8 Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) — §8.3 Field Declarations; §8.8 Constructors. 기간불변식·실패계약은자체설계

### A2-OOD-02

전제: 주문은 소계·최종금액을 소유하며 0≤할인≤소계를 항상 유지해야 한다. 할인 정책은 등급·소계로 금액을 계산하며 캠페인마다 바뀐다. 주문에 모든 정책 분기를 넣는 대신 어떤 협력 경계를 둘 수 있는가? 정책 변경과 잘못된 반환값의 책임을 나눠 설명하라.

할인 계산을 DiscountPolicy 같은 작은 계약으로 분리하고 주문 또는 유스케이스가 선택한 정책을 합성할 수 있다. 필요한 등급·소계를 전달하면 정책별 계산 변경이 해당 정책으로 모인다. 주문은 정책 반환값을 신뢰해 무조건 저장하지 않고 자신의 금액 불변식을 최종 검사한다. 새 정책 때 주문 핵심 규칙까지 바꿀 필요는 줄지만 정책 선택/조립은 바뀔 수 있다. 함수 전략이나 명확히 분리한 규칙 모듈도 조건을 만족하면 유효하며 상속 계층이 필수는 아니다.

판단 범위: 모든 변경에 인터페이스가 필요하다는 규칙이 아님. 제시한변경축·불변식·협력조건으로판단.

개념별 핵심:

- A2_OOD_02_01 · 변경 이유에 따른 정책 협력: 계산 정책의 작은 계약·필요 정보 전달·합성, 계산 변경과 선택/조립 변경을 구별. 함수/분리 모듈도 허용.
- A2_OOD_02_02 · 전략 교체와 불변식의 최종 방어: 주문이 할인 범위 최종 검사, 잘못된 정책값 거부. 상속·특정 패턴이 필수 정답 아님.

근거 대조:

- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — Chapter 9 introduction: unrelated classes implement one contract
- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html) — Domain logic in behavior-rich objects. 정책분리·금액계약은자체설계

### A2-OOD-03

전제: Withdrawal 계약은 잔액 충분한 양수 amount를 받으면 추가 수수료 없이 정확히 amount만 차감한다. 구현 LimitedWithdrawal은 amount>1000이면 거부하고 성공 시 추가 10을 차감한다. 컴파일은 된다. 이 구현을 원계약의 대체로 쓸 수 있는가? 요구를 보존할 대안을 설명하라.

대체로 쓸 수 없다. 원계약이 허용한 충분한 잔액의 2000 출금을 새로 거절해 전제조건을 강화하고, 성공 시 amount+10 차감해 후조건도 어긴다. 같은 메서드 시그니처와 컴파일 성공은 행동 계약 준수를 보장하지 않는다. 제한·수수료 없는 원계약을 그대로 지키는 구현을 쓰거나, 제한·수수료를 드러내는 별도 계약과 호출자 흐름을 설계해야 한다. 인터페이스 이름만 바꿔 원호출자를 그대로 두는 것은 해결이 아니다.

판단 범위: 잔액·한도·수수료는 문항의 명시적 업무 계약. 모든 금융 객체에 동일한 계약이 있다고 일반화하지 않음.

개념별 핵심:

- A2_OOD_03_01 · 타입 호환과 행동 계약: 허용 2000 거부의 전제 강화·추가 10의 후조건 위반, 컴파일과 대체 가능성 구별.
- A2_OOD_03_02 · 명시적 계약 변경의 대안: 원계약 준수 구현 또는 제한/수수료의 별도 계약과 호출자 적응·검증. 이름 변경만으로 해결 불가.

근거 대조:

- [Liskov and Wing 1994 — A Behavioral Notion of Subtyping](https://www.cs.cmu.edu/~wing/publications/LiskovWing94.pdf) — §1 Subtype Requirement; §3 subtype methods and behavior preservation
- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — Chapter 9 interface implementation: 언어 타입 계약과의 구별

### A2-OOD-04

전제: 배송 견적 유스케이스가 외부 HTTP의 인증 헤더·JSON 필드명·timeout과 배송 정책을 한 메서드에서 다룬다. 외부 필드명만 바뀌었는데 업무 코드도 수정된다. 작은 조회 계약과 adapter를 쓴다면 어떤 정보·실패를 경계에 두며, mock 통과가 무엇을 검증하지 못하는가?

유스케이스가 필요한 배송 요율 조회 계약을 업무 입력·출력으로 정하고 HTTP adapter가 인증·JSON 변환·통신을 소유한다. 업무 정책은 그 값을 받아 견적을 판단하므로 필드명 변경은 주로 adapter에 모인다. timeout·조회 불가·유효하지 않은 요율의 의미를 계약에 명시하고 모두 0원으로 숨기지 않는다. interface가 있어도 HTTP DTO가 계약에 새면 경계가 약해진다. mock 통과는 호출자 조정만 확인하며 실제 HTTP 인증·직렬화·외부 응답·DB나 네트워크 정상까지 증명하지 않는다.

판단 범위: 이문항은설계제안. 특정 아키텍처 이름만 쓰면 정답으로 보지 않고 변경축·실패의미·검증경계로판단.

개념별 핵심:

- A2_OOD_04_01 · 업무 계약과 전송 adapter의 경계: 유스케이스 필요 정보의 업무 입출력·HTTP adapter의 인증/변환/통신 소유, 외부 DTO 누수 방지.
- A2_OOD_04_02 · 실패 의미와 검증 책임: timeout/조회 실패를 명시해 0원 성공과 구별, mock 조정 검사와 실제 HTTP 계약 검증 분리.

근거 대조:

- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — Chapter 9: abstract contract and implementation
- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html) — Domain logic and service coordination. port·adapter·실패계약은자체설계

### A2-OOD-05

전제: Money는 amount와 currency로 업무상 같음을 판단한다. Order는 고유 orderId로 같음을 판단한다. Money(100,KRW) 둘과 Money(100,USD), 같은 총액의 서로 다른 orderId 주문을 비교하라. 값 객체를 불변으로 둘 이유와 final 필드만으로 충분한지도 설명하라.

두 Money(100,KRW)는 업무값이 같고 Money(100,USD)는 통화가 달라 같지 않다. 총액이 같아도 서로 다른 orderId 주문은 다른 엔티티다. 같은 값과 같은 대상 식별을 구별한다. 값 객체를 불변으로 두면 공유 참조의 변경으로 다른 사용자의 의미가 바뀌는 위험을 줄이고 새 값으로 교체할 수 있다. final은 참조 재대입만 막으므로 가변 내부 컬렉션·객체가 있다면 방어적 복사나 불변 구성까지 필요하다. 값 동등성 정의는 업무 전제에 맞춰 명시해야 한다.

판단 범위: equals/hashCode·영속ID생성시점·환율/반올림의 구체 구현은 추가 검수 범위. initial-v1의record 가변 참조와 개념 중첩을 독립 표본에서 구분.

개념별 핵심:

- A2_OOD_05_01 · 값 동등성과 엔티티 식별: Money의 amount+currency와 Order의 orderId를 구별, 같은 값·다른 통화·다른 주문 사례 판정.
- A2_OOD_05_02 · 불변 값과 참조 변경의 경계: 공유 가변 위험·새 값 교체, final 재대입 금지와 내부 객체 가변성 구별·방어 복사/불변 구성 대안.

근거 대조:

- [Martin Fowler — Value Object](https://martinfowler.com/bliki/ValueObject.html) — ValueObject: value/reference identity; immutability
- [Java Language Specification 21 — Chapter 4 Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — §4.12.4 final Variables

## 확인·판정 한계

질문의 명시적 전제와 이 문서의 근거를 함께 확인. 다른 언어·버전·배치·업무 계약에 결과를 그대로 적용하지 않음. 설계 사례는 조건을 충족하는 여러 대안을 허용. 필요한 개념의 근거가 검색 결과에서 빠지면 판정 보류. 이 문서의 존재·구조 검사 통과는 사람 검수·검색 적합성·모델 품질을 증명하지 않음.

## 출처 이용 조건 검수

원문·코드·그림 미포함. 아래 메모는 법률 검토나 공개 승인 결과가 아님.

- [RFC 5681 — TCP Congestion Control](https://www.rfc-editor.org/rfc/rfc5681.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. RFC 본문의 Copyright Notice와 IETF Trust 이용 조건을 공개 전 검수.
- [Cornell CS3410 Fall 2024 — Caches](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/caches.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Cornell CS3410 Fall 2024 — Performance in Parallel Programming](https://www.cs.cornell.edu/courses/cs3410/2024fa/notes/parallel-perf.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [RFC 1034 — Domain Names: Concepts and Facilities](https://www.rfc-editor.org/rfc/rfc1034.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. RFC 본문의 Copyright Notice와 IETF Trust 이용 조건을 공개 전 검수.
- [Martin Fowler — Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Martin Fowler — Value Object](https://martinfowler.com/bliki/ValueObject.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Java Language Specification 21 — Chapter 8 Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Java Language Specification 21 — Chapter 15 Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Java Language Specification 21 — Chapter 9 Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Java Language Specification 21 — Chapter 4 Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Liskov and Wing 1994 — A Behavioral Notion of Subtyping](https://www.cs.cmu.edu/~wing/publications/LiskovWing94.pdf): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. 저자 대학에 게시된 논문 본문이며 ACM 권리 표기를 공개 전 검수.
- [MIT 6.006 Spring 2020 — Lecture 2 Data Structures](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/79a07dc1cb47d76dae2ffedc701e3d2b_MIT6_006S20_lec2.pdf): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. MIT OCW의 CC BY-NC-SA 안내 확인. 원문 이용 시 비상업·동일조건 등 별도 검토 필요.
- [MIT 6.006 Spring 2020 — Lecture 16 Dynamic Programming Subproblems](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/28461a74f81101874a13d9679a40584d_MIT6_006S20_lec16.pdf): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. MIT OCW의 CC BY-NC-SA 안내 확인. 원문 이용 시 비상업·동일조건 등 별도 검토 필요.
- [NIST — Prefixes for binary multiples](https://physics.nist.gov/cuu/Units/binary.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [OSTEP 1.10 — Chapter 36 I/O Devices](https://pages.cs.wisc.edu/~remzi/OSTEP/file-devices.pdf): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [OSTEP 1.10 — Chapter 19 Paging: Faster Translations (TLBs)](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-tlbs.pdf): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Princeton Algorithms 4e — Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Princeton Algorithms 4e — Undirected Graphs](https://algs4.cs.princeton.edu/41graph/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Princeton Algorithms 4e — Hash Tables](https://algs4.cs.princeton.edu/34hash/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Princeton Algorithms 4e — Mergesort](https://algs4.cs.princeton.edu/22mergesort/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Princeton Algorithms 4e — Priority Queues](https://algs4.cs.princeton.edu/24pq/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Princeton Algorithms 4e — Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [Google SRE Book — Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님.
- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. RFC 본문의 Copyright Notice와 IETF Trust 이용 조건을 공개 전 검수.
- [RFC 8446 — The Transport Layer Security (TLS) Protocol Version 1.3](https://www.rfc-editor.org/rfc/rfc8446.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. RFC 본문의 Copyright Notice와 IETF Trust 이용 조건을 공개 전 검수.
- [RFC 768 — User Datagram Protocol](https://www.rfc-editor.org/rfc/rfc768.html): 원문 권리는 원저작자 소유. 본문·코드·도표의 복사나 번역 재배포 없이 직접 만든 상황·한국어 설명과 링크만 포함. 이용 조건·사실의 사람 검수 대기. 법률 검토·공개 승인 아님. RFC 본문의 Copyright Notice와 IETF Trust 이용 조건을 공개 전 검수.
