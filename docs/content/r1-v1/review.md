# r1-v1 검수용 읽기 자료

> 자동 생성 읽기 사본. 수정 기준은 [bundle.json](bundle.json). 사람 검수·공개 승인 대기.

- 원본 SHA-256: `6d5a344ca2fc0a03d2ad177f576a014f2024ac4f20a5e015501181877c2005eb`
- 재생성: 저장소 루트에서 `python3 scripts/render_content_review.py --bundle docs/content/r1-v1/bundle.json`
- 검수 절차·승인 기록: [콘텐츠 안내](README.md#사람-검수공개-순서)
- 이 자료를 읽었다는 사실만으로 앱의 검수·공개 상태가 바뀌지 않음

## 문항과 판정 기준

### R1-JAVA-01 · BASIC

Object 필드 하나에 값을 보관하는 Box 클래스를 여러 타입에 재사용합니다. 정수를 넣은 Box에서 꺼낸 값을 String으로 형변환하는 코드가 컴파일됩니다. 이 방식의 문제와, Box<T>로 바꾸면 오류를 발견하는 시점이 어떻게 달라지는지 설명하세요.

**모범 답안**

Object로 보관하면 꺼낼 때마다 호출자가 직접 형변환해야 하고, 컴파일러는 실제로 무엇이 들어 있는지 알지 못한다. 그래서 잘못된 형변환은 실행 중 ClassCastException으로만 드러난다. Box<T>는 타입 인자를 선언에 적어 넣는 값과 꺼내는 값의 타입을 컴파일러가 대조하게 하므로, 같은 실수가 컴파일 오류로 바뀌고 형변환 코드도 사라진다. 타입마다 IntegerBox, StringBox를 따로 만드는 중복도 피한다.

**필수 개념**

- 형변환에 의존한 재사용의 위험 · 가중치 0.50: Object 보관은 호출자 형변환이 필요하고 잘못된 형변환이 실행 중 ClassCastException으로 드러난다는 설명.
- 타입 인자를 통한 컴파일 시점 검사 · 가중치 0.50: 타입 인자로 넣는 값과 꺼내는 값을 컴파일러가 대조해 오류 시점이 컴파일로 앞당겨진다는 설명.

**혼동 주의:** 제네릭을 실행 속도를 높이는 수단으로 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 4. Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — 4.5 Parameterized Types

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-02 · INTERMEDIATE

double sum(List<Number> values) 메서드에 List<Integer>를 넘기면 컴파일 오류가 납니다. Integer가 Number의 하위 타입인데도 거부되는 이유를 설명하고, 값을 읽기만 하는 이 메서드의 매개변수를 어떻게 선언하면 되는지 답하세요.

**모범 답안**

제네릭 타입은 불공변이어서 List<Integer>는 List<Number>의 하위 타입이 아니다. 허용된다면 메서드 안에서 그 리스트에 Double을 넣을 수 있어 원래의 List<Integer>가 깨진다. 값을 꺼내 읽기만 한다면 List<? extends Number>로 선언한다. 이 선언은 Number의 어떤 하위 타입 리스트든 받되, 원소 타입을 특정할 수 없으므로 null이 아닌 값을 추가하는 호출은 컴파일러가 막는다.

**필수 개념**

- 제네릭 타입의 불공변성 · 가중치 0.50: 타입 인자의 상하위 관계가 매개변수화 타입의 상하위 관계로 이어지지 않으며, 허용하면 잘못된 원소 추가가 가능해진다는 설명.
- 상한 와일드카드의 읽기 전용 사용 · 가중치 0.50: ? extends Number로 여러 하위 타입 리스트를 받고, 그 대가로 원소 추가가 제한된다는 설명.

**혼동 주의:** 배열의 공변성과 제네릭의 불공변성을 같은 규칙으로 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 4. Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — 4.5.1 Type Arguments of Parameterized Types; 4.10.2 Subtyping among Class and Interface Types

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-03 · INTERMEDIATE

제네릭 메서드 안에서 new T[10]으로 배열을 만들거나, 넘겨받은 Object가 List<String>인지 instanceof로 검사하려 했지만 컴파일되지 않습니다. 두 코드가 거부되는 공통 원인과 각각의 대안을 설명하세요.

**모범 답안**

타입 인자는 컴파일 후 소거되어 실행 중에는 T가 무엇인지, 리스트의 원소 타입이 무엇인지 남지 않는다. 배열은 생성할 때 원소 타입을 알아야 하므로 new T[10]을 만들 수 없고, Object가 List<String>인지는 실행 중에 확인할 방법이 없어 검사가 거부된다. 배열이 필요하면 Class<T>나 배열 생성 함수를 받아 만들거나 List<T>를 쓴다. 타입 검사는 List<?>까지만 확인하고 원소는 꺼낼 때 개별로 검사한다.

**필수 개념**

- 타입 소거와 실행 시점 타입 정보 · 가중치 0.50: 타입 인자가 실행 중에 남지 않아 T나 List<String>을 실행 시점에 구분할 수 없다는 설명.
- 소거 제약을 피하는 방법 · 가중치 0.50: Class<T>·배열 생성 함수·List<T> 사용과, List<?>까지 검사한 뒤 원소를 개별 확인하는 대안 제시.

**혼동 주의:** 소거 때문에 제네릭이 타입 안전성을 주지 못한다고 결론짓지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 4. Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html) — 4.6 Type Erasure; 4.7 Reifiable Types

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-04 · BASIC

회원이 본 상품 ID를 저장합니다. 하나는 본 순서대로 중복을 포함해 보여 주는 최근 조회 목록이고, 다른 하나는 한 번이라도 본 상품인지 판단하는 용도입니다. 각각 List와 Set 중 무엇이 맞는지, Set이 중복을 판단하는 기준과 함께 설명하세요.

**모범 답안**

최근 조회 목록은 순서와 중복이 의미를 가지므로 List가 맞다. 본 적이 있는지 판단하는 용도는 같은 ID를 한 번만 담으면 되므로 Set이 맞고, HashSet이면 포함 여부 확인이 평균적으로 상수 시간이다. Set은 equals로 같은 원소를 판단하며 해시 기반 구현은 hashCode도 함께 사용한다. 따라서 직접 만든 클래스를 원소로 쓰려면 두 메서드를 일관되게 재정의해야 한다.

**필수 개념**

- 순서·중복 요구에 따른 컬렉션 선택 · 가중치 0.50: 순서와 중복이 의미 있으면 List, 유일성과 포함 여부 확인이 목적이면 Set이라는 판단.
- Set의 중복 판단 기준 · 가중치 0.50: Set은 equals로 같은 원소를 판단하고 해시 기반 구현은 hashCode도 일관되어야 한다는 설명.

**혼동 주의:** Set을 ‘정렬되지 않는 List’ 정도로 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.Set](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html) — Set 인터페이스 설명: contains no duplicate elements

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-05 · BASIC

클래스에 인스턴스 필드 count가 있습니다. static 메서드 안에서 count를 바로 읽으려 하면 컴파일 오류가 납니다. 그 이유를 static 멤버와 인스턴스 멤버가 속하는 대상의 차이로 설명하고, static 메서드에서 그 값을 다루는 방법을 답하세요.

**모범 답안**

인스턴스 필드는 객체마다 따로 존재하고, 인스턴스 메서드는 호출 대상 객체를 this로 받아 그 필드에 접근한다. static 메서드는 클래스에 속해 객체 없이 호출되므로 this가 없고, 어느 객체의 count인지 정할 수 없다. 그래서 단순 이름으로 인스턴스 필드를 참조하면 컴파일 오류가 된다. static 메서드에서 값을 다루려면 대상 객체를 매개변수로 받아 그 참조를 통해 접근해야 한다.

**필수 개념**

- 클래스 소속과 인스턴스 소속 · 가중치 0.50: static 멤버는 클래스에, 인스턴스 필드는 각 객체에 속한다는 구분.
- this가 없는 static 문맥 · 가중치 0.50: static 메서드에는 대상 객체가 없어 인스턴스 멤버를 단순 이름으로 참조할 수 없고 객체 참조를 받아야 한다는 설명.

**혼동 주의:** static을 ‘메모리에 먼저 올라가서 빠르다’로만 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 8. Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) — 8.4.3.2 static Methods; 8.3.1.1 static Fields

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-06 · INTERMEDIATE

한글이 들어 있는 텍스트 파일을 UTF-8로 저장한 뒤, 다른 프로그램이 EUC-KR로 읽었더니 글자가 깨졌습니다. 문자 집합과 인코딩이 각각 무엇을 정하는지 구분하고, 깨지는 이유와 Java 21에서 EUC-KR 파일을 올바르게 읽는 방법을 설명하세요.

**모범 답안**

문자 집합은 문자마다 번호를 붙인 대응이고, 인코딩은 그 번호를 바이트열로 바꾸는 규칙이다. 같은 ‘가’도 UTF-8과 EUC-KR에서는 서로 다른 바이트가 되므로, 쓴 쪽과 다른 규칙으로 읽으면 바이트가 엉뚱한 문자로 해석된다. 잘못 해석한 결과를 다시 저장하면 원래 바이트를 되찾기 어렵다. Java 21의 기본 Charset은 UTF-8이므로, EUC-KR 파일을 읽을 때는 바이트를 문자로 바꾸는 지점에서 그 Charset을 직접 지정해야 한다.

**필수 개념**

- 문자 집합과 인코딩의 구분 · 가중치 0.50: 문자에 번호를 붙이는 대응과 그 번호를 바이트로 바꾸는 규칙을 구분.
- 인코딩 불일치와 명시적 지정 · 가중치 0.50: 쓴 규칙과 읽는 규칙이 다르면 바이트가 다른 문자로 해석되며 변환 지점에서 Charset을 명시해야 한다는 설명.

**혼동 주의:** ‘UTF-8로 통일하면 끝’으로만 답하고 변환 지점의 지정을 빠뜨리지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.nio.charset.Charset](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html) — Charset 클래스 설명: coded character set; character-encoding scheme; defaultCharset: default charset is UTF-8

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-07 · INTERMEDIATE

FileInputStream의 read()를 반복해 큰 파일을 한 바이트씩 읽었더니 매우 느립니다. BufferedInputStream으로 감싸면 같은 read() 호출이 빨라지는 이유를 설명하고, 버퍼를 무작정 크게 잡는다고 계속 빨라지지 않는 이유도 답하세요.

**모범 답안**

버퍼가 없으면 read() 한 번마다 운영체제에 읽기를 요청하게 되고, 이 호출 비용이 바이트 수만큼 반복된다. BufferedInputStream은 내부 배열에 한꺼번에 많이 읽어 두고 read()에는 그 배열에서 꺼내 주므로 운영체제 호출 횟수가 크게 줄어든다. 버퍼가 어느 정도 커지면 호출 횟수는 이미 충분히 줄어 추가 이득이 작고 메모리만 더 쓰게 된다. 실제 속도는 저장 장치와 운영체제 캐시에도 좌우되므로 크기는 측정해서 정한다.

**필수 개념**

- 바이트 단위 호출의 비용 · 가중치 0.50: 버퍼 없이 읽으면 호출마다 운영체제 요청이 발생해 비용이 누적된다는 설명.
- 버퍼링의 효과와 한계 · 가중치 0.50: 내부 배열에 모아 읽어 호출 횟수를 줄이며 버퍼 크기를 키울수록 이득이 줄어든다는 설명.

**혼동 주의:** 버퍼가 클수록 항상 빠르다고 단정하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.io.BufferedInputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/BufferedInputStream.html) — BufferedInputStream 클래스 설명: many bytes at a time

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-08 · INTERMEDIATE

설정 파일은 Files.readAllBytes로 한 번에 읽어 잘 동작했습니다. 같은 코드를 수 GB 크기의 로그 파일에 쓰려고 합니다. 어떤 문제가 생길 수 있는지 설명하고, 한 줄씩 처리하면 되는 작업에서 선택할 방법과 주의할 점을 답하세요.

**모범 답안**

readAllBytes는 파일 전체를 바이트 배열 하나에 올리므로 파일 크기만큼의 힙이 필요하다. 수 GB 파일이면 OutOfMemoryError가 나거나 배열 크기 한계에 걸린다. 한 줄씩 처리하면 되는 작업은 BufferedReader나 Files.lines처럼 필요한 만큼만 읽는 방식을 쓰면 메모리 사용이 파일 크기와 무관해진다. Files.lines가 돌려주는 스트림은 파일을 연 상태이므로 try-with-resources로 닫아야 한다.

**필수 개념**

- 전체 적재의 메모리 비용 · 가중치 0.50: 파일 전체를 한 배열에 올리면 파일 크기만큼의 힙이 필요하고 큰 파일에서는 실패한다는 설명.
- 순차 읽기와 자원 해제 · 가중치 0.50: 필요한 만큼만 읽는 방식으로 메모리 사용을 제한하고 열린 스트림은 닫아야 한다는 설명.

**혼동 주의:** ‘큰 파일은 무조건 NIO’처럼 API 이름만으로 답하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.nio.file.Files](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html) — Files.readAllBytes: not intended for reading in large files; Files.lines: try-with-resources

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-09 · INTERMEDIATE

프레임워크는 애플리케이션이 만든 클래스를 미리 알지 못한 채 객체를 만들고 메서드를 호출합니다. 리플렉션이 이 일을 가능하게 하는 방식을 설명하고, 일반 호출과 비교해 애플리케이션 코드에서 남용하면 생기는 문제를 답하세요.

**모범 답안**

리플렉션은 실행 중에 Class 객체로부터 생성자·메서드·필드 정보를 얻어, 이름으로 객체를 만들고 메서드를 호출하게 해 준다. 그래서 프레임워크는 컴파일 시점에 몰랐던 클래스도 설정이나 애노테이션만 보고 다룰 수 있다. 대신 컴파일러의 타입 검사를 받지 못해 이름이나 인자가 틀리면 실행 중에야 예외가 난다. 접근 제한을 풀어 private 멤버를 건드리면 캡슐화가 깨지고, 일반 호출보다 느리며 코드 추적도 어려워진다.

**필수 개념**

- 실행 시점의 클래스 정보 사용 · 가중치 0.50: Class 객체에서 얻은 생성자·메서드·필드 정보로 컴파일 시점에 모르던 클래스를 다룬다는 설명.
- 검사 누락과 캡슐화 우회의 비용 · 가중치 0.50: 컴파일 검사가 없어 오류가 실행 중에 드러나고 접근 제한 우회·성능·추적성 비용이 있다는 설명.

**혼동 주의:** 리플렉션을 ‘private에도 접근하는 편리한 기능’으로만 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.lang.reflect](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/package-summary.html) — java.lang.reflect 패키지 설명: reflective information about classes and objects

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-10 · INTERMEDIATE

메서드에 직접 만든 @Audit 애노테이션을 붙였지만 실행해도 아무 일도 일어나지 않습니다. 애노테이션만으로 동작이 생기지 않는 이유를 설명하고, 실행 중 리플렉션으로 이 애노테이션을 읽으려면 선언에 무엇이 필요한지 답하세요.

**모범 답안**

애노테이션은 코드에 붙이는 메타데이터일 뿐 스스로 실행되지 않는다. 컴파일러, 애노테이션 프로세서, 프레임워크처럼 그 정보를 읽어 처리하는 주체가 있어야 동작이 생긴다. 실행 중 리플렉션으로 읽으려면 선언에 @Retention(RetentionPolicy.RUNTIME)을 지정해야 한다. 지정하지 않으면 기본값 CLASS가 적용되어 클래스 파일에는 남지만 실행 중에는 조회되지 않고, SOURCE는 컴파일 후 버려진다.

**필수 개념**

- 메타데이터와 처리 주체의 분리 · 가중치 0.50: 애노테이션은 정보만 제공하며 이를 읽어 처리하는 컴파일러·도구·프레임워크가 있어야 동작한다는 설명.
- 보존 정책과 실행 시점 조회 · 가중치 0.50: RUNTIME 보존이어야 리플렉션으로 읽을 수 있고 기본값 CLASS와 SOURCE는 실행 중 조회되지 않는다는 설명.

**혼동 주의:** 애노테이션을 ‘붙이면 기능이 생기는 문법’으로 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 9. Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html) — 9.6.4.2 @Retention

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-11 · INTERMEDIATE

좌표만 비교하는 Point의 equals가 있고, 색을 더한 하위 클래스 ColorPoint는 상대가 ColorPoint일 때만 좌표와 색을 비교하도록 equals를 재정의했습니다. point.equals(colorPoint)와 colorPoint.equals(point)의 결과가 달라지는 이유를 equals 계약으로 설명하세요.

**모범 답안**

Point의 equals는 상대가 Point이기만 하면 좌표만 비교하므로 point.equals(colorPoint)는 true가 될 수 있다. 반대로 ColorPoint의 equals는 상대가 ColorPoint가 아니면 false이므로 colorPoint.equals(point)는 false다. 이는 x.equals(y)와 y.equals(x)가 같아야 한다는 대칭성 위반이다. 계약이 깨지면 컬렉션의 contains나 remove 결과가 호출 방향에 따라 달라진다. 상속 대신 Point를 필드로 갖는 구성으로 바꾸거나 같은 클래스끼리만 같다고 판단하도록 기준을 통일해야 한다.

**필수 개념**

- equals 계약의 대칭성 · 가중치 0.50: x.equals(y)와 y.equals(x)의 결과가 같아야 하며 예시에서 두 방향의 결과가 달라짐을 설명.
- 계약 위반의 영향과 해결 방향 · 가중치 0.50: 컬렉션 동작이 호출 방향에 따라 달라지는 문제와 구성 사용·비교 기준 통일 같은 대안 제시.

**혼동 주의:** equals 계약을 hashCode 재정의 규칙 하나로만 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.lang.Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) — Object.equals: symmetric

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-12 · BASIC

String a = "java"; String b = "java"; String c = new String("java");가 있습니다. a == b, a == c, a.equals(c)의 결과를 각각 설명하고, 문자열 내용을 비교할 때 ==에 의존하면 안 되는 이유를 답하세요.

**모범 답안**

==는 두 참조가 같은 객체를 가리키는지 비교한다. 같은 내용의 문자열 리터럴은 하나의 String 객체를 공유하므로 a == b는 true다. new String은 내용이 같아도 새 객체를 만들므로 a == c는 false다. equals는 문자 내용을 비교하므로 a.equals(c)는 true다. 실행 중에 입력이나 연산으로 만들어진 문자열은 리터럴과 다른 객체일 수 있으므로, 내용 비교에 ==를 쓰면 같은 내용인데도 false가 나올 수 있다.

**필수 개념**

- 참조 비교와 내용 비교 · 가중치 0.50: ==는 같은 객체인지, equals는 문자 내용이 같은지 비교한다는 구분과 세 식의 결과.
- 리터럴 공유와 새 객체 생성 · 가중치 0.50: 같은 리터럴은 한 객체를 공유하지만 new나 실행 중 생성된 문자열은 별도 객체일 수 있다는 설명.

**혼동 주의:** ‘문자열은 ==로 비교해도 된다’는 경험을 일반 규칙으로 쓰지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 3. Lexical Structure](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html) — 3.10.5 String Literals

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-13 · INTERMEDIATE

반복문 안에서 result += line으로 문자열 10만 개를 이어 붙였더니 느립니다. String의 불변성과 연결해 원인을 설명하고, StringBuilder를 쓰면 무엇이 달라지는지 답하세요.

**모범 답안**

String은 불변이어서 += 연산은 기존 객체를 고치지 않고, 지금까지의 내용과 새 조각을 합친 새 String을 만든다. 반복할 때마다 그동안 쌓인 문자 전체를 다시 복사하므로 전체 작업량은 이어 붙인 총 길이의 제곱에 비례해 늘어난다. StringBuilder는 변경 가능한 내부 버퍼 끝에 조각을 덧붙이고, 버퍼가 차면 더 크게 늘리므로 전체 복사량이 총 길이에 비례하는 수준에 머문다. 마지막에 toString으로 한 번만 String을 만든다.

**필수 개념**

- 불변 문자열 연결의 반복 복사 · 가중치 0.50: += 연산이 매번 새 String을 만들며 누적된 내용을 다시 복사해 비용이 제곱으로 늘어난다는 설명.
- 가변 버퍼를 이용한 누적 · 가중치 0.50: StringBuilder가 내부 버퍼에 덧붙여 전체 복사량을 총 길이 수준으로 줄인다는 설명.

**혼동 주의:** 모든 + 연결을 StringBuilder로 바꿔야 한다고 일반화하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.lang.StringBuilder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html) — StringBuilder 클래스 설명: mutable sequence of characters

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-14 · INTERMEDIATE

Integer a = 127, b = 127과 Integer c = 1000, d = 1000이 있습니다. a == b와 c == d의 결과가 다를 수 있는 이유를 설명하고, 값이 null인 Integer를 int 변수에 대입하면 어떻게 되는지 답하세요.

**모범 답안**

Integer끼리의 ==는 값이 아니라 참조를 비교한다. 오토박싱은 -128부터 127까지의 값에 대해 같은 객체를 돌려주도록 보장하므로 a == b는 true지만, 1000은 서로 다른 객체일 수 있어 c == d는 false가 될 수 있다. 값 비교는 equals를 쓰거나 int로 꺼내서 해야 한다. null인 Integer를 int에 대입하면 언박싱 과정에서 NullPointerException이 발생한다. 박싱은 객체를 만들 수 있으므로 큰 반복 계산에서는 기본형을 쓰는 편이 낫다.

**필수 개념**

- 래퍼 객체의 참조 비교와 캐시 범위 · 가중치 0.50: 래퍼 타입의 ==는 참조 비교이며 -128~127 범위만 같은 객체가 보장된다는 설명.
- 언박싱의 null 위험 · 가중치 0.50: null인 래퍼를 기본형으로 바꾸면 NullPointerException이 발생한다는 설명.

**혼동 주의:** ‘Integer는 127까지만 == 비교가 된다’를 사용 규칙처럼 외우지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 5. Conversions and Contexts](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html) — 5.1.7 Boxing Conversion; 5.1.8 Unboxing Conversion

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-15 · BASIC

주문 상태를 int 상수 0, 1, 2로 표현하던 코드를 enum OrderStatus로 바꾸려 합니다. void changeStatus(int status)와 void changeStatus(OrderStatus status)를 비교해, enum이 막아 주는 실수와 유지보수에서 얻는 이점을 설명하세요.

**모범 답안**

int 매개변수는 정의되지 않은 7 같은 값이나 다른 의미의 정수도 그대로 받아들이므로, 잘못된 값은 실행 중에야 드러난다. enum 매개변수는 선언된 상수만 받을 수 있어 이런 호출이 컴파일 오류가 된다. 로그나 디버거에 숫자 대신 이름이 보이고, 상태별 데이터나 동작을 enum 안에 함께 둘 수 있다. 상수를 추가했을 때 switch 식에서 빠진 경우를 컴파일러가 오류로 알려 주므로 누락도 줄어든다.

**필수 개념**

- 허용 값 집합의 컴파일 시점 제한 · 가중치 0.50: enum 타입은 선언된 상수만 받으므로 정의되지 않은 값 전달이 컴파일 오류가 된다는 설명.
- 이름·데이터·동작의 응집 · 가중치 0.50: 상수 이름이 드러나고 상태별 데이터와 동작을 enum에 함께 둘 수 있어 변경 시 누락을 줄인다는 설명.

**혼동 주의:** enum을 ‘상수에 이름을 붙인 것’으로만 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 8. Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) — 8.9 Enum Classes

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-16 · INTERMEDIATE

서울과 뉴욕 사용자가 함께 쓰는 서비스에서 주문 시각을 LocalDateTime으로 저장했더니 ‘언제 일어난 일인지’를 두고 혼선이 생겼습니다. LocalDateTime, ZonedDateTime, Instant가 각각 무엇을 표현하는지 구분하고, 주문 시각 저장에 무엇을 쓸지 설명하세요.

**모범 답안**

LocalDateTime은 시간대 정보 없이 날짜와 시각만 담으므로, 같은 값이 서울과 뉴욕에서 서로 다른 순간을 뜻하게 된다. Instant는 타임라인 위의 한 순간을 나타내고, ZonedDateTime은 날짜·시각에 시간대 규칙을 더해 특정 지역의 시각으로 순간을 표현한다. 주문 시각처럼 실제로 일어난 순간은 Instant로 저장하고, 화면에 보여 줄 때 사용자의 시간대로 변환한다. LocalDateTime은 매장 영업 시작 시각처럼 시간대와 무관하게 정한 값에 쓴다.

**필수 개념**

- 시간대 없는 시각과 순간의 구분 · 가중치 0.50: LocalDateTime은 시간대가 없어 순간을 특정하지 못하고 Instant·ZonedDateTime은 타임라인의 순간을 나타낸다는 구분.
- 저장과 표시의 분리 · 가중치 0.50: 일어난 순간은 Instant로 저장하고 표시할 때 사용자의 시간대로 변환한다는 기준.

**혼동 주의:** 서버 시간대를 UTC로 맞추면 LocalDateTime으로 충분하다고 단정하지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.time](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html) — java.time 패키지 설명: Dates and Times

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-17 · INTERMEDIATE

구독 만료일을 계산할 때 시작 시각에 Duration.ofDays(30)을 더하는 방법과 Period.ofMonths(1)을 더하는 방법이 있습니다. 두 타입이 표현하는 간격의 차이를 설명하고, ‘다음 달 같은 날’이 요구일 때 무엇을 써야 하는지 답하세요.

**모범 답안**

Duration은 초와 나노초로 표현하는 시간 기반의 양이고, Period는 연·월·일로 표현하는 날짜 기반의 양이다. Duration.ofDays(30)은 정확히 30×24시간을 더하므로 1월 31일에 더하면 3월 초가 되고, 달마다 결과 날짜가 어긋난다. Period.ofMonths(1)은 달력 규칙에 따라 다음 달의 같은 날로 옮기며, 그 날이 없으면 말일로 맞춘다. ‘다음 달 같은 날’이 요구라면 Period를 쓴다. 일광 절약 시간이 있는 지역에서는 하루가 24시간이 아닌 날이 있어 두 방식의 결과가 달라진다.

**필수 개념**

- 시간 기반 양과 날짜 기반 양 · 가중치 0.50: Duration은 초 단위의 정확한 길이, Period는 연·월·일의 달력 단위라는 구분.
- 달력 규칙이 필요한 계산의 선택 · 가중치 0.50: 월 단위 요구에는 Period를 쓰며 Duration의 고정 길이는 달 길이와 일광 절약 시간에서 어긋난다는 설명.

**혼동 주의:** Period와 Duration을 단위만 다른 같은 개념으로 보지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.time](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html) — java.time 패키지 설명: Duration and Period

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-18 · INTERMEDIATE

저장소 계층에서 SQLException을 잡은 뒤 throw new OrderSaveException("저장 실패")로 바꿔 던졌습니다. 운영 중 장애가 났는데 로그에는 원래의 SQL 오류가 보이지 않습니다. 무엇이 빠졌는지 설명하고, 예외를 잡아서 처리할지 그대로 전파할지 정하는 기준을 답하세요.

**모범 답안**

새 예외를 만들 때 원래 예외를 원인으로 넘기지 않아, 스택 트레이스에서 SQL 오류와 발생 위치가 사라졌다. 계층에 맞는 예외로 바꾸더라도 생성자에 원인 예외를 전달해 연결을 유지해야 한다. 예외는 복구하거나 의미 있는 대체 동작을 할 수 있는 곳에서만 잡는다. 그럴 수 없는 계층에서는 전파하거나 원인을 보존한 채 추상화 수준에 맞는 예외로 바꾸고, 잡은 뒤 아무 처리 없이 무시하지 않는다.

**필수 개념**

- 원인 예외의 보존 · 가중치 0.50: 예외를 바꿔 던질 때 원래 예외를 원인으로 연결해야 실패 지점과 내용을 추적할 수 있다는 설명.
- 예외를 잡는 위치의 기준 · 가중치 0.50: 복구나 대체 동작이 가능한 곳에서만 잡고 그렇지 않으면 전파하거나 원인을 보존해 변환한다는 기준.

**혼동 주의:** ‘예외는 무조건 런타임 예외로 감싸 던진다’로 답을 끝내지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.lang.Throwable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html) — Throwable 클래스 설명: chained exception

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-19 · INTERMEDIATE

Runnable의 run() 안에서 Thread.sleep()을 호출하려고 메서드 선언에 throws InterruptedException을 붙였더니 컴파일 오류가 납니다. 재정의하는 메서드의 throws 절에 어떤 제약이 있는지 설명하고, 이 상황에서 체크 예외를 어떻게 다뤄야 하는지 답하세요.

**모범 답안**

재정의하는 메서드는 재정의되는 메서드가 선언한 것보다 넓은 체크 예외를 throws 절에 선언할 수 없다. Runnable.run()은 체크 예외를 선언하지 않으므로 구현 쪽에서도 추가할 수 없다. 상위 타입으로 호출하는 코드는 그 선언만 보고 예외 처리를 생략했기 때문에, 하위 구현이 새 체크 예외를 던지면 컴파일러의 검사가 무너진다. 따라서 run() 안에서 try-catch로 직접 처리해야 하며, InterruptedException이라면 인터럽트 상태를 복구하고 작업을 정리하는 식으로 다룬다.

**필수 개념**

- 재정의 시 throws 절의 제약 · 가중치 0.50: 재정의하는 메서드는 상위 선언보다 넓은 체크 예외를 선언할 수 없다는 규칙과 그 이유.
- 선언할 수 없는 예외의 처리 · 가중치 0.50: 선언할 수 없는 체크 예외는 메서드 안에서 처리해야 하며 인터럽트는 상태 복구 등으로 다룬다는 설명.

**혼동 주의:** throws를 붙일 수 없으니 빈 catch로 삼키면 된다고 답하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 8. Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) — 8.4.8.3 Requirements in Overriding and Hiding

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-20 · BASIC

상품 목록을 ‘가격이 1만 원 이상’으로 거르는 메서드와 ‘재고가 있음’으로 거르는 메서드가 반복문은 같고 조건식만 다릅니다. 조건을 값이 아니라 동작으로 전달하도록 바꾸면 무엇이 달라지는지, Predicate와 람다를 사용해 설명하세요.

**모범 답안**

기준 금액 같은 값을 매개변수로 받으면 숫자만 바꿀 수 있고 비교 방식 자체는 메서드 안에 고정된다. 조건을 Predicate<Product>로 받으면 반복과 수집은 한 메서드에 두고, 무엇을 통과시킬지는 호출자가 람다로 전달한다. 새 조건이 생겨도 거르는 메서드를 추가하거나 고치지 않고 람다만 새로 넘기면 된다. 람다는 함수형 인터페이스의 구현을 그 자리에서 간결하게 적는 표현이어서 익명 클래스보다 조건이 눈에 잘 띈다.

**필수 개념**

- 값 전달과 동작 전달의 차이 · 가중치 0.50: 값만 받으면 로직은 고정되지만 동작을 받으면 판단 방식 자체를 호출자가 정한다는 설명.
- 변하는 부분의 분리 · 가중치 0.50: 반복과 수집은 한 곳에 두고 조건만 Predicate와 람다로 전달해 새 조건에 메서드 수정 없이 대응한다는 설명.

**혼동 주의:** 람다를 ‘코드가 짧아지는 문법’으로만 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 15. Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — 15.27 Lambda Expressions

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-21 · INTERMEDIATE

같은 람다 x -> x * 2를 Function<Integer, Integer> 변수에도, IntUnaryOperator 변수에도 대입할 수 있습니다. 람다 식 자체에는 타입이 적혀 있지 않은데 이것이 가능한 이유를 함수형 인터페이스의 요건과 함께 설명하세요.

**모범 답안**

함수형 인터페이스는 추상 메서드가 하나뿐인 인터페이스다. 람다 식은 스스로 타입을 갖지 않고, 대입되는 변수나 전달되는 매개변수의 타입, 즉 대상 타입에서 타입이 정해진다. 컴파일러는 대상 타입의 추상 메서드와 람다의 매개변수 개수·타입·반환값이 맞는지 검사한다. 그래서 같은 식이 Function에서는 Integer를 받아 Integer를 돌려주는 구현이 되고, IntUnaryOperator에서는 int를 받아 int를 돌려주는 구현이 된다. 대상 타입을 알 수 없는 자리에서는 람다를 쓸 수 없다.

**필수 개념**

- 함수형 인터페이스의 요건 · 가중치 0.50: 추상 메서드가 하나뿐인 인터페이스만 람다의 대상이 될 수 있다는 설명.
- 대상 타입에 의한 타입 결정 · 가중치 0.50: 람다는 대상 타입의 추상 메서드에 맞춰 매개변수와 반환 타입이 정해지고 검사된다는 설명.

**혼동 주의:** 람다에 고유한 ‘람다 타입’이 있다고 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 15. Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — 15.27.3 Type of a Lambda Expression

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-22 · INTERMEDIATE

Integer::parseInt, System.out::println, String::length는 모두 메서드 참조지만 인자를 다루는 방식이 다릅니다. 각각 같은 뜻의 람다로 바꿔 쓰고, 클래스 이름으로 인스턴스 메서드를 참조하는 String::length에서 첫 번째 인자가 어떤 역할을 하는지 설명하세요.

**모범 답안**

Integer::parseInt는 정적 메서드 참조로 s -> Integer.parseInt(s)와 같다. System.out::println은 특정 객체의 인스턴스 메서드 참조로 x -> System.out.println(x)와 같고, 호출 대상이 System.out으로 고정된다. String::length는 임의 객체의 인스턴스 메서드 참조로 s -> s.length()와 같다. 이 형태에서는 함수의 첫 번째 인자가 메서드를 호출할 대상 객체가 되고, 나머지 인자가 있으면 메서드의 인자로 전달된다.

**필수 개념**

- 메서드 참조의 세 형태 · 가중치 0.50: 정적 메서드·특정 객체의 인스턴스 메서드·임의 객체의 인스턴스 메서드 참조를 같은 뜻의 람다로 구분.
- 첫 인자가 호출 대상이 되는 형태 · 가중치 0.50: 클래스 이름으로 인스턴스 메서드를 참조하면 첫 번째 인자가 호출 대상 객체가 된다는 설명.

**혼동 주의:** 모든 메서드 참조를 ‘람다를 줄여 쓴 것’ 하나로 묶어 설명하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 15. Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — 15.13 Method Reference Expressions

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-23 · INTERMEDIATE

원소 100만 개인 리스트에 stream().filter(...).map(...).findFirst()를 실행했습니다. filter와 map이 100만 개 전체에 각각 먼저 적용된 뒤 첫 원소를 고르는 방식이 아닌 이유를, 중간 연산과 최종 연산의 구분으로 설명하세요.

**모범 답안**

filter와 map 같은 중간 연산은 호출 시점에 실행되지 않고 처리 단계를 연결한 새 스트림만 돌려준다. 실제 처리는 findFirst 같은 최종 연산이 호출될 때 시작되며, 원소는 한 개씩 filter와 map을 차례로 통과한다. findFirst는 결과가 하나 정해지면 더 처리할 필요가 없는 단락 연산이므로, 조건을 만족하는 첫 원소가 나온 순간 나머지 원소는 읽지 않는다. 그래서 전체에 단계별로 한 번씩 도는 방식보다 처리량이 줄어든다.

**필수 개념**

- 중간 연산의 지연과 최종 연산의 실행 · 가중치 0.50: 중간 연산은 단계만 구성하고 최종 연산이 호출될 때 처리가 시작된다는 설명.
- 원소 단위 처리와 단락 · 가중치 0.50: 원소가 한 개씩 전체 단계를 통과하며 단락 연산은 결과가 정해지면 나머지를 처리하지 않는다는 설명.

**혼동 주의:** 스트림을 ‘단계마다 컬렉션을 새로 만드는 반복문’으로 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.util.stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html) — java.util.stream 패키지 설명: Stream operations and pipelines

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-24 · INTERMEDIATE

List<Integer>의 합계를 stream().reduce(0, Integer::sum)으로 구하는 코드와 mapToInt(Integer::intValue).sum()으로 구하는 코드가 있습니다. 원소가 수천만 개일 때 두 방식의 비용 차이가 어디서 생기는지 설명하세요.

**모범 답안**

Stream<Integer>의 reduce는 원소와 누적값을 Integer 객체로 다룬다. Integer::sum은 int를 받고 돌려주므로 매 단계에서 언박싱과 박싱이 일어나고, 누적 결과마다 새 Integer가 만들어질 수 있다. mapToInt로 IntStream으로 바꾸면 이후 연산은 int 값 그대로 진행되어 박싱 객체와 그에 따른 메모리 할당·회수 부담이 없다. 원소가 많을수록 이 차이가 누적되고, IntStream은 sum과 average 같은 수치 연산도 직접 제공한다.

**필수 개념**

- 객체 스트림의 박싱 비용 · 가중치 0.50: Stream<Integer> 연산에서 단계마다 박싱과 언박싱이 일어나 객체 생성 비용이 누적된다는 설명.
- 기본형 특화 스트림의 역할 · 가중치 0.50: IntStream 등은 기본형 값을 그대로 처리해 박싱을 피하고 수치 연산을 제공한다는 설명.

**혼동 주의:** 기본형 스트림을 ‘메서드가 더 많은 스트림’ 정도로만 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.stream.IntStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html) — IntStream 인터페이스 설명: int primitive specialization of Stream

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-25 · INTERMEDIATE

findCached(id).orElse(loadFromDatabase(id))로 캐시에 없을 때만 DB에서 읽으려 했는데, 캐시에 값이 있어도 DB 조회 로그가 매번 찍힙니다. 원인을 설명하고 orElseGet으로 바꾸면 무엇이 달라지는지 답하세요.

**모범 답안**

orElse의 인자는 일반 메서드 인자이므로 orElse가 호출되기 전에 먼저 평가된다. 그래서 Optional에 값이 있든 없든 loadFromDatabase(id)가 실행되고, 값이 있을 때 그 결과는 버려진다. orElseGet은 값을 만드는 Supplier를 받아 Optional이 비어 있을 때만 호출한다. 대체 값을 구하는 데 비용이 들거나 DB 조회 같은 부수 효과가 있으면 orElseGet(() -> loadFromDatabase(id))를 써야 하고, 이미 만들어진 상수라면 orElse로 충분하다.

**필수 개념**

- 메서드 인자의 선평가 · 가중치 0.50: orElse의 인자는 호출 전에 평가되어 값이 있어도 대체 값 계산이 실행된다는 설명.
- Supplier를 통한 지연 계산 · 가중치 0.50: orElseGet은 비어 있을 때만 Supplier를 호출하므로 비용이나 부수 효과가 있는 대체 값에 적합하다는 설명.

**혼동 주의:** 두 메서드를 ‘성능이 조금 다른 같은 기능’으로만 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html) — Optional.orElseGet: supplying function

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-26 · INTERMEDIATE

회원 조회 메서드가 Optional<Member>를 반환합니다. 호출 코드가 곧바로 get()을 호출해 값이 없을 때 예외가 납니다. Optional을 반환 타입으로 쓰는 목적을 설명하고, 값이 없는 경우를 get() 없이 처리하는 방법을 두 가지 이상 답하세요.

**모범 답안**

Optional 반환은 결과가 없을 수 있다는 사실을 타입으로 드러내, 호출자가 그 경우를 처리하도록 만드는 것이 목적이다. 확인 없이 get()을 부르면 값이 없을 때 NoSuchElementException이 나므로, null을 그대로 쓰다 NullPointerException이 나는 것과 다르지 않다. 값이 없을 때의 동작은 호출자가 정해야 한다. 기본값이 있으면 orElse나 orElseGet을 쓰고, 없으면 안 되는 경우는 orElseThrow로 의미 있는 예외를 던지며, 있을 때만 이어서 처리하려면 map이나 ifPresent를 쓴다.

**필수 개념**

- 부재를 타입으로 드러내는 목적 · 가중치 0.50: Optional 반환은 결과 없음의 가능성을 호출자가 처리하게 하려는 것이며 무조건 get()은 그 목적을 버린다는 설명.
- 부재 처리 방법의 선택 · 가중치 0.50: 기본값·예외·조건부 처리에 맞춰 orElse/orElseGet·orElseThrow·map/ifPresent를 고른다는 설명.

**혼동 주의:** Optional을 쓰면 null 관련 오류가 자동으로 사라진다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html) — Optional 클래스 설명: method return type

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-27 · ADVANCED

ForkJoinPool은 큰 작업을 작은 작업으로 나눠 여러 스레드에서 처리합니다. 한 스레드의 작업이 먼저 끝났을 때 그 스레드가 놀지 않게 하는 방식을 설명하고, 공용 풀에서 실행되는 작업 안에 오래 걸리는 블로킹 호출을 넣으면 어떤 문제가 생기는지 답하세요.

**모범 답안**

각 작업자 스레드는 자기 큐를 갖고, 나눈 하위 작업을 거기에 쌓아 처리한다. 자기 큐가 비면 다른 스레드의 큐에 남은 작업을 가져와 실행하는 작업 훔치기로 스레드가 쉬지 않게 한다. 공용 풀의 스레드 수는 기본적으로 프로세서 수에 맞춰져 있어 CPU 계산을 전제로 한다. 작업이 블로킹 호출로 멈추면 그 스레드는 다른 작업을 처리하지 못하고, 병렬 스트림처럼 같은 공용 풀을 쓰는 다른 코드까지 함께 느려진다. 블로킹 작업은 별도 실행기로 분리해야 한다.

**필수 개념**

- 작업 훔치기 방식 · 가중치 0.50: 작업자마다 큐를 두고 자기 큐가 비면 다른 작업자의 작업을 가져와 실행한다는 설명.
- 공용 풀과 블로킹 작업의 충돌 · 가중치 0.50: 프로세서 수 기준의 공용 풀에서 블로킹이 스레드를 묶어 같은 풀을 쓰는 작업 전체를 지연시킨다는 설명.

**혼동 주의:** ForkJoinPool을 ‘스레드를 많이 쓰는 빠른 풀’로 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.ForkJoinPool](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ForkJoinPool.html) — ForkJoinPool 클래스 설명: work-stealing; common pool

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-28 · INTERMEDIATE

ArrayList의 정수 합계를 구하는 스트림에 parallel()을 붙였더니 원소가 100개일 때는 오히려 느려졌고, 같은 코드를 LinkedList에 쓰니 원소가 많아도 빨라지지 않았습니다. 두 현상의 원인을 설명하고, 병렬 스트림에서 람다가 외부 변수를 갱신하면 안 되는 이유도 답하세요.

**모범 답안**

병렬 스트림은 데이터를 나누고, 여러 스레드에서 처리한 뒤, 부분 결과를 합치는 비용을 더 낸다. 원소가 적거나 원소당 작업이 가벼우면 이 비용이 병렬 처리의 이득보다 커서 순차 실행보다 느리다. ArrayList는 인덱스로 반씩 고르게 나눌 수 있지만 LinkedList는 중간 지점을 찾으려면 순회해야 해 분할이 비효율적이다. 람다가 공유 변수를 갱신하면 여러 스레드가 동시에 쓰게 되어 결과가 틀리거나 실행마다 달라지므로, 결과는 reduce나 collect로 합쳐야 한다.

**필수 개념**

- 분할·결합 비용과 소스 구조 · 가중치 0.50: 병렬 처리에는 분할과 결합 비용이 들고 소스가 고르게 나뉘는 구조인지에 따라 이득이 달라진다는 설명.
- 공유 상태 갱신의 금지 · 가중치 0.50: 병렬 실행에서 람다가 공유 변수를 갱신하면 경쟁으로 결과가 틀려지므로 reduce·collect로 결과를 합친다는 설명.

**혼동 주의:** parallel()을 붙이면 코어 수만큼 빨라진다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.util.stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html) — java.util.stream 패키지 설명: Parallelism; Side-effects

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JAVA-29 · INTERMEDIATE

주문 목록의 총액을 계산하는 메서드가 계산 도중 인자로 받은 리스트를 정렬하고, 클래스 필드인 lastTotal도 갱신합니다. 이 메서드를 순수 함수로 바꾼다는 것이 무슨 뜻인지 설명하고, 바꿨을 때 테스트와 동시 실행에서 얻는 이점을 답하세요.

**모범 답안**

순수 함수는 같은 입력에 항상 같은 결과를 돌려주고, 인자나 외부 상태를 바꾸지 않는다. 이 메서드는 인자로 받은 리스트의 순서를 바꾸고 필드를 갱신하므로, 호출한 쪽의 데이터와 객체 상태가 호출 순서에 따라 달라진다. 순수하게 바꾸려면 입력을 수정하지 않고 필요한 경우 복사본을 정렬하며, 총액은 반환값으로만 전달한다. 그러면 입력과 기대 결과만으로 테스트할 수 있고, 공유 상태를 쓰지 않으므로 여러 스레드가 동시에 호출해도 서로 영향을 주지 않는다.

**필수 개념**

- 순수 함수의 조건 · 가중치 0.50: 같은 입력에 같은 결과를 내고 인자와 외부 상태를 바꾸지 않는다는 조건과 예시의 위반 지점.
- 부수 효과 제거의 이점 · 가중치 0.50: 입출력만으로 테스트할 수 있고 공유 상태가 없어 동시 호출에 안전하다는 설명.

**혼동 주의:** 순수 함수를 ‘static 메서드’나 ‘람다로 쓴 함수’와 같은 뜻으로 쓰지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.util.stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html) — java.util.stream 패키지 설명: Stateless behaviors; Side-effects

근거 문서: `r1-java`. 검수 상태: **PENDING**

### R1-JVM-01 · BASIC

메인 스레드가 작업 스레드 세 개를 시작한 뒤 결과를 합산해야 합니다. Thread.sleep(1000)으로 기다리는 코드와 각 스레드에 join()을 호출하는 코드를 비교해, join이 누구를 무엇이 끝날 때까지 기다리게 하는지 설명하세요.

**모범 답안**

join()을 호출한 스레드, 여기서는 메인 스레드가 대상 스레드가 종료할 때까지 대기한다. sleep(1000)은 작업이 끝났는지와 무관하게 정해진 시간만 멈추므로, 작업이 더 오래 걸리면 미완성 결과를 합산하고 일찍 끝나면 불필요하게 기다린다. join은 종료라는 조건을 기다리므로 걸린 시간에 상관없이 결과가 준비된 뒤에 진행한다. 무한 대기를 피하려면 시간 제한이 있는 join을 쓰고, 반환 뒤 스레드가 실제로 끝났는지 확인한다.

**필수 개념**

- join의 대기 주체와 대상 · 가중치 0.50: join을 호출한 스레드가 대상 스레드의 종료까지 대기한다는 설명.
- 시간 대기와 조건 대기의 차이 · 가중치 0.50: sleep은 완료와 무관한 고정 시간 대기이고 join은 종료 조건을 기다린다는 구분과 시간 제한의 필요.

**혼동 주의:** join을 ‘스레드를 합치는 것’처럼 모호하게 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.lang.Thread](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html) — Thread.join: Waits for this thread to terminate

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-02 · INTERMEDIATE

바쁜 대기 루프에서 CPU 사용을 줄이려고 Thread.yield()를 넣었습니다. yield()가 다른 스레드의 실행이나 실행 순서를 보장하는지 설명하고, 특정 조건이 될 때까지 기다려야 하는 코드에서 yield 대신 써야 할 방식을 답하세요.

**모범 답안**

yield()는 현재 스레드가 프로세서를 양보할 의사가 있다는 힌트를 스케줄러에 줄 뿐이며, 스케줄러는 이를 무시할 수 있다. 다른 스레드가 실행된다는 보장도, 어떤 순서로 실행된다는 보장도 없다. yield를 넣은 루프는 여전히 실행 가능한 상태로 조건을 반복 확인하므로 CPU를 계속 쓴다. 조건을 기다려야 한다면 락과 조건 변수, BlockingQueue, CountDownLatch처럼 조건이 만족될 때 깨워 주는 동기화 도구로 스레드를 대기 상태에 두어야 한다.

**필수 개념**

- yield의 힌트 성격 · 가중치 0.50: yield는 스케줄러에 주는 힌트일 뿐 다른 스레드의 실행이나 순서를 보장하지 않는다는 설명.
- 바쁜 대기와 조건 대기 · 가중치 0.50: yield 루프는 CPU를 계속 쓰며 조건 대기는 깨워 주는 동기화 도구로 구현해야 한다는 설명.

**혼동 주의:** yield를 ‘다른 스레드에게 순서를 넘기는 명령’으로 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.lang.Thread](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html) — Thread.yield: A hint to the scheduler

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-03 · INTERMEDIATE

두 스레드가 같은 계좌 객체에 대해 if (balance >= amount) balance -= amount;를 동시에 실행합니다. 잔액 100에서 각각 80을 출금하면 잔액이 음수가 될 수 있는 이유를 설명하고, 임계 영역을 어디까지 잡아야 하는지 답하세요.

**모범 답안**

잔액 확인과 차감은 별개의 동작이어서 그 사이에 다른 스레드가 끼어들 수 있다. 두 스레드가 모두 잔액 100을 읽어 조건을 통과한 뒤 각각 차감하면 잔액은 -60이 되거나, 차감 결과 하나가 덮어써질 수 있다. 결과가 실행 순서에 따라 달라지는 이 상황이 경쟁 상태다. 확인과 차감이 한 덩어리로 실행되도록 두 동작 전체를 같은 락으로 보호해야 한다. 차감만 보호하면 확인 시점의 값이 이미 낡았을 수 있고, 이 잔액을 읽고 쓰는 다른 코드도 같은 락을 써야 한다.

**필수 개념**

- 확인 후 실행의 경쟁 상태 · 가중치 0.50: 확인과 변경 사이에 다른 스레드가 끼어들어 결과가 실행 순서에 따라 달라진다는 설명.
- 임계 영역의 범위 · 가중치 0.50: 읽기·판단·쓰기 전체를 같은 락으로 보호하고 같은 상태에 접근하는 모든 코드가 그 락을 써야 한다는 설명.

**혼동 주의:** 변수를 volatile로 바꾸면 확인 후 실행 문제가 해결된다고 답하지 않기.

**출처 대조 위치**

- [JLS Java SE 21 — Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html) — 17.1 Synchronization

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-04 · INTERMEDIATE

synchronized 블록으로 보호하던 코드를 ReentrantLock으로 바꾸려 합니다. 락을 얻지 못했을 때 포기하거나 대기 중 인터럽트에 반응하는 기능처럼 ReentrantLock에서만 가능한 것을 설명하고, 대신 개발자가 직접 책임져야 하는 것을 답하세요.

**모범 답안**

synchronized는 락을 얻을 때까지 무조건 기다리고, 블록을 벗어나면 예외가 나도 자동으로 락을 푼다. ReentrantLock은 tryLock으로 즉시 또는 제한 시간 안에 얻지 못하면 포기할 수 있고, lockInterruptibly로 대기 중 인터럽트에 반응할 수 있으며, 공정성 옵션과 여러 Condition도 제공한다. 대신 해제가 자동이 아니므로 lock() 직후 try 블록을 열고 finally에서 unlock()을 호출해야 한다. 이를 빠뜨리면 예외 경로에서 락이 풀리지 않아 다른 스레드가 영원히 대기한다.

**필수 개념**

- 명시적 락의 추가 기능 · 가중치 0.50: tryLock의 시도·시간 제한, 인터럽트 가능한 대기, 공정성·Condition 등 synchronized에 없는 제어를 설명.
- 해제 책임과 finally · 가중치 0.50: ReentrantLock은 자동 해제가 없어 finally에서 unlock을 보장해야 한다는 설명.

**혼동 주의:** ReentrantLock이 synchronized보다 항상 빠르다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.locks.ReentrantLock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html) — ReentrantLock 클래스 설명: extended capabilities; try block

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-05 · INTERMEDIATE

크기가 제한된 버퍼를 synchronized와 wait()/notify()로 구현했습니다. 생산자와 소비자가 같은 모니터에서 기다리는데, notify()를 쓰면 버퍼에 빈자리가 있는데도 모든 스레드가 멈춰 버리는 경우가 생깁니다. 원인을 설명하고 해결 방법을 답하세요.

**모범 답안**

한 모니터의 대기 집합에는 빈자리를 기다리는 생산자와 데이터를 기다리는 소비자가 섞여 있다. notify()는 그중 아무 스레드 하나만 깨우므로, 소비자가 데이터를 꺼낸 뒤 보낸 신호가 생산자가 아닌 다른 소비자에게 갈 수 있다. 깨어난 소비자는 버퍼가 비어 다시 대기하고, 정작 일할 수 있는 생산자는 깨어나지 못해 전체가 멈춘다. notifyAll()로 모두 깨우고 각자 while로 조건을 다시 확인하게 하거나, 조건별로 대기 집합을 분리해야 한다.

**필수 개념**

- 섞인 대기 집합과 notify · 가중치 0.50: 한 모니터에 서로 다른 조건을 기다리는 스레드가 섞여 있어 notify가 엉뚱한 스레드를 깨울 수 있다는 설명.
- notifyAll과 조건 재확인 · 가중치 0.50: 모두 깨운 뒤 while로 조건을 재확인하거나 조건별 대기 집합을 분리해 해결한다는 설명.

**혼동 주의:** notifyAll은 느리니 notify가 낫다고만 답하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.lang.Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) — Object.notify: The choice is arbitrary; Object.wait: spurious wakeup

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-06 · INTERMEDIATE

같은 제한 버퍼를 ReentrantLock으로 다시 구현하면서 Condition 객체를 notFull과 notEmpty 두 개로 나눴습니다. 대기 공간을 나누면 신호 전달이 어떻게 달라지는지 설명하고, signal()을 받은 스레드가 곧바로 실행을 이어 가는지 답하세요.

**모범 답안**

Condition을 나누면 생산자는 notFull에서, 소비자는 notEmpty에서 기다린다. 소비자가 데이터를 꺼낸 뒤 notFull.signal()을 호출하면 빈자리를 기다리던 생산자만 깨어나므로, 엉뚱한 스레드를 깨우거나 모두를 깨워 다시 경쟁시키는 낭비가 없다. signal을 받은 스레드는 await에서 돌아오기 전에 락을 다시 얻어야 한다. 신호를 보낸 스레드가 락을 풀 때까지 기다리고 다른 스레드와 경쟁도 하므로, 깨어난 뒤에도 조건을 반복문으로 다시 확인해야 한다.

**필수 개념**

- 조건별 대기 공간 분리 · 가중치 0.50: Condition을 조건마다 나누면 필요한 종류의 스레드만 깨울 수 있다는 설명.
- 신호 이후의 락 재획득 · 가중치 0.50: 깨어난 스레드는 락을 다시 얻어야 await에서 돌아오며 그 뒤에도 조건을 재확인해야 한다는 설명.

**혼동 주의:** signal을 받으면 락 없이 바로 실행된다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.locks.Condition](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/Condition.html) — Condition 인터페이스 설명: multiple wait-sets per object

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-07 · INTERMEDIATE

용량이 가득 찬 ArrayBlockingQueue에 원소를 넣을 때 add, offer, put은 각각 어떻게 동작하나요? 큐가 비어 있을 때 remove, poll, take의 차이도 함께 설명하고, 생산자·소비자 구조에서 어떤 메서드를 고를지 답하세요.

**모범 답안**

가득 찬 큐에서 add는 IllegalStateException을 던지고, offer는 넣지 못했다는 뜻으로 false를 반환하며, put은 자리가 날 때까지 스레드를 대기시킨다. 빈 큐에서 remove는 NoSuchElementException을 던지고, poll은 null을 반환하며, take는 원소가 들어올 때까지 대기한다. offer와 poll에는 제한 시간만큼만 기다리는 형태도 있다. 생산자가 소비자보다 빠를 때 속도를 맞추려면 put과 take로 대기시키고, 기다릴 수 없는 요청 처리 경로라면 offer로 실패를 받아 거절이나 재시도를 결정한다.

**필수 개념**

- 예외·특수 값·대기의 세 동작 · 가중치 0.50: 가득 찬 큐와 빈 큐에서 add/remove는 예외, offer/poll은 false·null, put/take는 대기라는 구분.
- 요구에 따른 큐 메서드 선택 · 가중치 0.50: 속도 조절이 필요하면 대기형을, 기다릴 수 없으면 실패를 반환받는 형태를 고른다는 판단.

**혼동 주의:** 세 메서드를 ‘넣는 메서드의 다른 이름’으로 뭉뚱그리지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.BlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html) — BlockingQueue 인터페이스 설명: Throws exception; Special value; Blocks; Times out

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-08 · ADVANCED

AtomicInteger의 incrementAndGet()은 락 없이 여러 스레드에서 안전하게 값을 올립니다. compare-and-set이 어떤 조건에서 값을 바꾸는지 설명하고, 경쟁이 심할 때 이 방식의 비용이 어디서 생기는지 synchronized와 비교해 답하세요.

**모범 답안**

compare-and-set은 현재 값이 기대한 값과 같을 때에만 새 값으로 바꾸고, 비교와 교체를 하나의 원자적 동작으로 수행한다. 증가 연산은 현재 값을 읽고, 1을 더한 값으로 교체를 시도하고, 그 사이 다른 스레드가 값을 바꿔 실패하면 다시 읽어 재시도한다. 스레드를 대기시키지 않으므로 경쟁이 적을 때는 락보다 가볍다. 경쟁이 심하면 실패와 재시도가 반복되어 CPU를 소모한다. synchronized는 여러 변수를 함께 바꾸는 복합 동작을 보호할 수 있지만 CAS는 변수 하나의 갱신에 적합하다.

**필수 개념**

- 비교 후 교체의 원자성 · 가중치 0.50: 현재 값이 기대 값과 같을 때만 교체하며 비교와 교체가 원자적으로 수행된다는 설명.
- 재시도 비용과 적용 범위 · 가중치 0.50: 실패 시 재시도하므로 경쟁이 심하면 CPU를 소모하고 단일 변수 갱신에 적합하다는 설명.

**혼동 주의:** CAS를 ‘락이 없으니 언제나 더 빠른 방식’으로 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — package java.util.concurrent.atomic](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/package-summary.html) — java.util.concurrent.atomic 패키지 설명: lock-free thread-safe programming on single variables

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-09 · INTERMEDIATE

ArrayList를 Collections.synchronizedList로 감싸 여러 스레드가 공유합니다. 한 스레드가 for-each로 순회하는 동안 다른 스레드가 원소를 추가하자 ConcurrentModificationException이 발생했습니다. 메서드마다 동기화되어 있는데도 문제가 생긴 이유와 대안을 설명하세요.

**모범 답안**

동기화 래퍼는 add나 get 같은 개별 메서드 호출을 하나씩 락으로 보호할 뿐이다. 순회는 여러 번의 호출로 이루어진 복합 동작이어서, 호출 사이에 다른 스레드가 리스트를 바꾸면 반복자가 변경을 감지해 예외를 던진다. 래퍼를 계속 쓰려면 순회 전체를 그 리스트 객체에 대한 synchronized 블록으로 감싸야 하고, 그동안 다른 스레드는 모두 기다린다. 읽기가 많고 쓰기가 드물면 CopyOnWriteArrayList처럼 순회 중 변경을 허용하도록 설계된 동시성 컬렉션이 더 맞다.

**필수 개념**

- 개별 연산 동기화의 한계 · 가중치 0.50: 래퍼는 메서드 단위로만 보호하므로 순회 같은 복합 동작 사이에 변경이 끼어들 수 있다는 설명.
- 복합 동작 보호와 동시성 컬렉션 · 가중치 0.50: 순회 전체를 같은 락으로 감싸거나 순회 중 변경을 견디도록 설계된 동시성 컬렉션을 선택한다는 설명.

**혼동 주의:** 스레드 안전한 컬렉션을 쓰면 복합 동작도 자동으로 안전하다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html) — Collections.synchronizedList: manually synchronize on the returned list

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-10 · ADVANCED

ThreadPoolExecutor를 코어 4, 최대 8, 용량 제한 없는 LinkedBlockingQueue로 구성했습니다. 요청이 몰릴 때 스레드가 8개까지 늘어나지 않는 이유를 설명하고, 이 구성이 과부하에서 지연과 메모리에 미치는 영향 및 큐 용량과 거부 정책을 두는 이유를 답하세요.

**모범 답안**

스레드 풀은 코어 수만큼 스레드가 차면 새 작업을 큐에 넣고, 큐가 가득 차서 더 넣을 수 없을 때에만 최대 수까지 스레드를 늘린다. 용량 제한이 없는 큐는 가득 차지 않으므로 스레드는 4개에서 늘지 않고 최대 수 설정은 효과가 없다. 처리 속도보다 요청이 많으면 큐가 계속 길어져 대기 시간이 늘고, 결국 메모리가 부족해진다. 큐에 용량을 두고 거부 정책을 정하면 감당할 수 없는 요청을 빨리 실패시키거나 호출자 쪽에서 속도를 늦추게 해 시스템 전체를 보호한다.

**필수 개념**

- 코어·큐·최대 스레드의 적용 순서 · 가중치 0.50: 코어가 차면 큐에 넣고 큐가 가득 찰 때만 최대 수까지 늘리므로 무제한 큐에서는 최대 수가 무의미하다는 설명.
- 과부하에서의 큐 용량과 거부 정책 · 가중치 0.50: 무제한 큐는 지연과 메모리 증가를 부르며 용량 제한과 거부 정책으로 과부하를 드러내고 제한한다는 설명.

**혼동 주의:** 스레드 수를 늘리면 처리량이 계속 늘어난다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html) — ThreadPoolExecutor 클래스 설명: Core and maximum pool sizes; Queuing; Rejected tasks

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-11 · BASIC

외부 API를 호출해 가격을 받아 오는 작업을 스레드 풀에 제출하려 합니다. Runnable과 Callable 중 무엇으로 작성해야 하는지 반환값과 예외 처리 관점에서 설명하고, 제출 후 받은 Future에서 결과와 작업 중 발생한 예외를 어떻게 얻는지 답하세요.

**모범 답안**

Runnable의 run()은 값을 반환하지 않고 체크 예외도 선언할 수 없다. Callable의 call()은 결과를 반환하고 예외를 던질 수 있으므로, 가격처럼 결과가 필요하고 실패할 수 있는 작업에는 Callable이 맞다. submit은 작업을 큐에 넣고 바로 Future를 돌려주며, 호출자는 다른 일을 하다가 get()으로 결과를 받는다. get()은 작업이 끝날 때까지 대기하고, 작업이 예외로 끝났다면 그 예외를 원인으로 담은 ExecutionException을 던진다. 무한 대기를 피하려면 제한 시간이 있는 get을 쓴다.

**필수 개념**

- 반환값과 예외 선언의 차이 · 가중치 0.50: Runnable은 결과와 체크 예외 선언이 없고 Callable은 결과를 반환하며 예외를 던질 수 있다는 구분.
- Future를 통한 결과와 실패 전달 · 가중치 0.50: Future.get이 완료까지 대기해 결과를 돌려주고 작업의 예외는 ExecutionException의 원인으로 전달된다는 설명.

**혼동 주의:** Future.get()이 대기 없이 즉시 값을 준다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.Callable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Callable.html) — Callable 인터페이스 설명: returns a result and may throw an exception

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-12 · INTERMEDIATE

배포를 위해 애플리케이션을 종료할 때 ExecutorService를 정리해야 합니다. shutdown()과 shutdownNow()가 대기 중인 작업과 실행 중인 작업을 각각 어떻게 다루는지 비교하고, 진행 중인 작업을 최대한 마치게 하는 종료 절차를 설명하세요.

**모범 답안**

shutdown()은 새 작업을 더 받지 않고, 이미 제출된 작업은 대기 중인 것까지 모두 실행한 뒤 종료한다. shutdownNow()는 대기 중인 작업을 실행하지 않고 목록으로 돌려주며, 실행 중인 작업에는 인터럽트를 보내 중단을 시도한다. 인터럽트에 반응하지 않는 작업은 멈추지 않는다. 일반적인 절차는 shutdown()을 호출하고 awaitTermination으로 정해진 시간만큼 기다린 뒤, 그때까지 끝나지 않았으면 shutdownNow()로 중단을 시도하고 다시 기다리는 것이다. 작업 코드가 인터럽트를 확인하고 정리하도록 작성돼 있어야 이 절차가 통한다.

**필수 개념**

- 두 종료 방식의 차이 · 가중치 0.50: shutdown은 제출된 작업을 모두 실행하고 shutdownNow는 대기 작업을 반환하며 실행 중 작업에 인터럽트를 보낸다는 구분.
- 단계적 종료 절차 · 가중치 0.50: shutdown 후 제한 시간 대기, 미완료 시 shutdownNow로 이어지는 절차와 작업의 인터럽트 협조가 필요하다는 설명.

**혼동 주의:** shutdownNow()가 실행 중인 작업을 즉시 강제 종료한다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.concurrent.ExecutorService](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html) — ExecutorService.shutdown: previously submitted tasks are executed; ExecutorService.shutdownNow: Attempts to stop all actively executing tasks

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-JVM-13 · INTERMEDIATE

HotSpot JVM의 가비지 컬렉터가 수거 대상을 정하는 방식을 묻습니다. 객체가 살아 있다고 판단하는 기준과, 힙을 젊은 세대와 늙은 세대로 나눠 젊은 세대를 자주 수집하는 이유를 설명하세요. ‘참조 횟수가 0이 되면 수거한다’는 설명이 맞는지도 답하세요.

**모범 답안**

HotSpot은 참조 횟수를 세지 않는다. 스택의 지역 변수나 static 필드 같은 루트에서 참조를 따라 도달할 수 있는 객체를 살아 있다고 보고, 도달할 수 없는 객체를 수거한다. 그래서 서로를 참조하는 객체들도 루트에서 닿지 않으면 수거된다. 대부분의 객체는 만들어진 직후 쓸모없어진다는 관찰에 따라, 새 객체를 젊은 세대에 할당하고 이 영역만 자주 수집하면 적은 비용으로 많은 메모리를 회수할 수 있다. 여러 번 살아남은 객체는 늙은 세대로 옮겨 덜 자주 수집한다.

**필수 개념**

- 도달 가능성 기준의 생존 판단 · 가중치 0.50: 루트에서 참조로 도달할 수 있는지로 생존을 판단하며 참조 횟수 방식이 아니어서 순환 참조도 수거된다는 설명.
- 세대 분리의 근거 · 가중치 0.50: 대부분의 객체가 일찍 죽는다는 관찰에 따라 젊은 세대를 자주 수집해 비용을 줄이고 오래 산 객체는 승격한다는 설명.

**혼동 주의:** System.gc() 호출이나 null 대입으로 수거 시점을 직접 정할 수 있다고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 HotSpot VM Garbage Collection Tuning Guide — Garbage Collector Implementation](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-collector-implementation1.html) — Generational Garbage Collection; weak generational hypothesis

근거 문서: `r1-jvm-and-concurrency`. 검수 상태: **PENDING**

### R1-OOD-01 · INTERMEDIATE

OrderService가 DiscountPolicy 인터페이스 타입의 필드를 선언하고, 생성자 안에서 new FixedDiscountPolicy()로 직접 만들어 넣습니다. 할인 정책을 RateDiscountPolicy로 바꾸려면 OrderService를 고쳐야 합니다. 인터페이스를 도입했는데도 이렇게 되는 이유와, 구현을 고르는 책임을 어디로 옮겨야 하는지 설명하세요.

**모범 답안**

필드 타입은 인터페이스지만 new FixedDiscountPolicy()라는 코드 때문에 OrderService는 구체 클래스에도 의존한다. 사용할 구현을 스스로 고르고 만드는 책임까지 갖고 있어, 구현이 바뀌면 사용하는 쪽 코드가 함께 바뀐다. OrderService는 생성자 매개변수로 DiscountPolicy를 받기만 하고, 어떤 구현을 만들어 넣을지는 애플리케이션을 조립하는 별도 코드나 컨테이너가 맡게 한다. 그러면 OrderService의 컴파일 시점 의존은 인터페이스뿐이고, 실행 시점에 연결되는 객체는 조립 코드만 고쳐 바꿀 수 있으며 테스트에서는 가짜 구현을 넣을 수 있다.

**필수 개념**

- 구체 클래스 생성이 만드는 의존 · 가중치 0.50: 인터페이스 타입을 쓰더라도 직접 new 하면 구체 클래스에 의존하고 구현 선택 책임까지 갖게 된다는 설명.
- 사용과 조립의 분리 · 가중치 0.50: 구현 선택과 생성을 외부 조립 코드로 옮기고 생성자로 주입받아 사용 코드의 변경 없이 구현을 교체한다는 설명.

**혼동 주의:** 인터페이스를 만들기만 하면 결합이 사라진다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — Inversion of Control Containers and the Dependency Injection pattern](https://martinfowler.com/articles/injection.html) — Inversion of Control; Constructor Injection

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-02 · BASIC

Account 클래스의 balance 필드를 private으로 바꾸고 getBalance()와 setBalance()를 추가했습니다. 출금 코드는 여전히 밖에서 잔액을 꺼내 비교한 뒤 setBalance()로 덮어씁니다. 이 상태를 캡슐화라고 할 수 있는지 설명하고, 잔액이 음수가 되면 안 된다는 규칙을 지키려면 어떻게 바꿔야 하는지 답하세요.

**모범 답안**

필드를 private으로 숨겨도 getter와 setter로 값을 그대로 읽고 쓸 수 있다면, 잔액을 어떻게 바꿀지는 여전히 바깥 코드가 결정한다. 음수가 되면 안 된다는 규칙이 Account 밖의 호출 코드마다 흩어지고, 한 곳이라도 검사를 빠뜨리면 규칙이 깨진다. 캡슐화는 데이터와 그 데이터를 지키는 규칙을 한 객체 안에 두는 것이다. setBalance를 없애고 withdraw(amount)처럼 의도를 드러내는 메서드를 제공해, Account가 스스로 잔액을 확인하고 부족하면 거부하게 해야 한다.

**필수 개념**

- 필드 은닉과 캡슐화의 차이 · 가중치 0.50: private 필드에 getter·setter를 그대로 열면 상태 변경의 결정이 여전히 외부에 있다는 설명.
- 규칙을 가진 의도 기반 메서드 · 가중치 0.50: 규칙 검사를 객체 안의 withdraw 같은 메서드에 두어 모든 변경 경로에서 규칙이 지켜지게 한다는 설명.

**혼동 주의:** getter와 setter를 만들면 캡슐화가 끝난다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — TellDontAsk](https://martinfowler.com/bliki/TellDontAsk.html) — TellDontAsk: bundling data with the functions that operate on that data

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-03 · INTERMEDIATE

주문 금액 합계를 구하는 코드가 OrderService에 있고, 주문 항목 목록을 꺼내 단가와 수량을 곱해 더합니다. 이 계산 책임을 Order와 OrderLine으로 옮기려 합니다. 책임을 어느 객체에 둘지 판단하는 기준과, 옮겼을 때 변경의 영향이 어떻게 달라지는지 설명하세요.

**모범 답안**

책임은 그 일을 하는 데 필요한 정보를 가진 객체에 둔다. 한 항목의 금액은 단가와 수량을 아는 OrderLine이, 주문 합계는 항목 목록을 가진 Order가 계산하는 것이 맞다. 서비스가 내부 데이터를 꺼내 계산하면 항목의 구조를 서비스가 알아야 하고, 같은 계산이 필요한 다른 곳에도 코드가 복제된다. 책임을 옮기면 할인 필드가 추가되는 식으로 금액 계산 방식이 바뀌어도 OrderLine 안에서 끝나고, 호출하는 쪽은 order.totalAmount()만 알면 된다.

**필수 개념**

- 정보를 가진 객체에 책임 할당 · 가중치 0.50: 계산에 필요한 데이터를 가진 객체가 그 계산을 맡는다는 기준과 예시의 적용.
- 책임 이동과 변경 범위 · 가중치 0.50: 외부에서 데이터를 꺼내 계산하면 구조 지식과 중복이 퍼지고 책임을 옮기면 변경이 한 객체 안에 머문다는 설명.

**혼동 주의:** 모든 로직을 서비스 계층에 두는 것이 계층 분리라고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — TellDontAsk](https://martinfowler.com/bliki/TellDontAsk.html) — TellDontAsk: Rather than asking an object for data and acting on that data

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-04 · INTERMEDIATE

ReportPrinter가 order.getCustomer().getAddress().getZipCode()처럼 여러 객체의 내부를 차례로 거쳐 우편번호를 읽습니다. Address의 구조가 바뀌자 ReportPrinter도 수정해야 했습니다. 이 코드의 결합도가 높다고 판단하는 근거와 낮추는 방법을 설명하세요.

**모범 답안**

ReportPrinter는 Order뿐 아니라 Customer가 Address를 갖고 있고 Address에 우편번호 필드가 있다는 구조까지 알고 있다. 직접 협력하지 않는 객체의 내부 구조에 의존하므로, 그 가운데 어느 하나만 바뀌어도 ReportPrinter가 함께 바뀐다. 결합도는 한 요소가 다른 요소에 대해 얼마나 많이, 얼마나 자세히 알고 있는지로 판단한다. Order에 shippingZipCode()처럼 필요한 정보를 직접 돌려주는 메서드를 두면 ReportPrinter는 Order의 그 계약만 알게 되고, 내부 구조 변경은 Order 안에서 흡수된다.

**필수 개념**

- 내부 구조 지식으로 본 결합도 · 가중치 0.50: 직접 협력하지 않는 객체의 구조까지 알면 그중 하나의 변경이 전파된다는 설명.
- 계약으로 의존 범위 줄이기 · 가중치 0.50: 필요한 정보를 돌려주는 메서드를 협력 객체에 두어 내부 구조 변경을 그 객체가 흡수하게 한다는 설명.

**혼동 주의:** 결합도를 ‘import 문의 개수’ 같은 겉모습으로만 판단하지 않기.

**출처 대조 위치**

- [Refactoring catalog — Hide Delegate](https://refactoring.com/catalog/hideDelegate.html) — Hide Delegate

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-05 · INTERMEDIATE

MemberManager 클래스 하나에 회원 가입 검증, 비밀번호 암호화, 환영 메일 발송, 월별 가입 통계 계산이 모두 들어 있습니다. 이 클래스의 응집도가 낮다고 판단하는 기준을 설명하고, 서로 다른 변경 이유가 한 클래스에 섞여 있을 때 생기는 문제를 답하세요.

**모범 답안**

응집도는 한 모듈 안의 요소들이 같은 목적과 같은 변경 이유를 공유하는 정도다. 이 클래스는 가입 정책, 보안 요구, 메일 양식, 통계 기준이라는 서로 다른 이유로 바뀐다. 메일 문구를 고치는 변경이 가입 검증 코드와 같은 파일을 건드리므로, 관련 없는 기능을 깨뜨릴 위험이 생기고 서로 다른 작업을 하는 사람끼리 충돌도 잦아진다. 테스트를 하려면 필요 없는 의존까지 모두 준비해야 한다. 변경 이유가 같은 것끼리 모아 클래스를 나누면 한 요구의 변경이 한 곳에 머문다.

**필수 개념**

- 변경 이유로 본 응집도 · 가중치 0.50: 모듈의 요소들이 같은 목적과 변경 이유를 공유하는지로 응집도를 판단한다는 기준과 예시의 진단.
- 낮은 응집도의 비용 · 가중치 0.50: 무관한 변경이 같은 코드를 건드려 회귀 위험·충돌·테스트 준비 부담이 커지며 변경 이유별로 나눠 해결한다는 설명.

**혼동 주의:** 클래스의 줄 수가 적으면 응집도가 높다고 판단하지 않기.

**출처 대조 위치**

- [Robert C. Martin — The Single Responsibility Principle](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html) — Gather together the things that change for the same reasons

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-06 · INTERMEDIATE

주문 처리 모듈이 MySqlOrderRepository 클래스를 직접 참조합니다. 의존관계 역전 원칙을 적용해 OrderRepository 인터페이스를 도입하려 합니다. ‘상위 정책과 하위 구현이 모두 추상화에 의존한다’는 말의 의미와, 이 인터페이스를 어느 쪽 모듈이 소유해야 하는지 설명하세요.

**모범 답안**

주문 처리 규칙은 상위 정책이고 MySQL 접근 코드는 하위 구현이다. 정책이 구현 클래스를 직접 참조하면 저장 기술이 바뀔 때 정책 코드가 함께 바뀐다. 역전 원칙은 정책이 자신에게 필요한 저장·조회 동작을 인터페이스로 정의하고, 하위 구현이 그 인터페이스를 구현하게 하는 것이다. 인터페이스는 그것을 필요로 하는 상위 정책 쪽이 소유해야 한다. 그러면 소스 코드의 의존 방향이 구현에서 정책 쪽으로 향해 호출 흐름과 반대가 되고, 저장 기술을 바꿔도 정책 모듈은 고칠 필요가 없다.

**필수 개념**

- 추상화에 대한 양방향 의존 · 가중치 0.50: 상위 정책은 인터페이스를 사용하고 하위 구현은 그 인터페이스를 구현해 둘 다 추상화에 의존한다는 설명.
- 추상화의 소유와 의존 방향 · 가중치 0.50: 인터페이스를 상위 정책이 소유해 소스 의존 방향이 호출 흐름과 반대가 되고 구현 교체가 정책에 영향을 주지 않는다는 설명.

**혼동 주의:** 인터페이스를 구현 모듈 쪽에 두고도 의존이 역전됐다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — DIP in the Wild](https://martinfowler.com/articles/dipInTheWild.html) — DIP in the Wild: Dependency Inversion Principle

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-07 · INTERMEDIATE

java.util.Properties는 Hashtable을 상속했기 때문에 문자열이 아닌 키와 값을 넣는 put()도 호출할 수 있습니다. 구현을 재사용하려고 상속을 선택했을 때 생기는 이 문제를 설명하고, 조합으로 설계했다면 무엇이 달라지는지 답하세요.

**모범 답안**

상속하면 상위 클래스의 공개 메서드가 모두 하위 클래스의 API가 된다. Properties는 문자열 키와 값만 다뤄야 하지만 Hashtable의 put과 putAll이 그대로 노출되어, 호출자가 다른 타입을 넣어 규칙을 깨뜨릴 수 있다. 하위 클래스는 상위 클래스의 내부 구현 방식에도 묶여, 상위 클래스가 바뀌면 영향을 받는다. 조합은 Hashtable을 private 필드로 갖고 필요한 연산만 문자열 타입의 메서드로 공개하는 방식이다. 노출되는 API를 직접 정할 수 있고, 내부 자료구조를 바꿔도 사용하는 쪽에는 영향이 없다.

**필수 개념**

- 상속이 노출하는 상위 API · 가중치 0.50: 구현 재사용을 위한 상속은 상위 클래스의 공개 메서드를 모두 노출해 하위 클래스의 규칙이 깨질 수 있다는 설명.
- 조합을 통한 API 제어 · 가중치 0.50: 상위 타입을 필드로 갖고 필요한 연산만 공개하면 규칙을 지키고 내부 구현 변경을 숨길 수 있다는 설명.

**혼동 주의:** 상속은 나쁘고 조합은 좋다는 식으로 조건 없이 단정하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.Properties](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Properties.html) — Properties 클래스 설명: Their use is strongly discouraged

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-08 · INTERMEDIATE

Order가 List<OrderLine>을 getOrderLines()로 그대로 반환합니다. 외부 코드가 이 리스트에 직접 항목을 추가해 ‘주문 항목은 최대 20개’라는 규칙이 깨졌습니다. 컬렉션을 감싸 관리할 때 무엇을 캡슐화할 수 있는지와 조회용 접근을 어떻게 제공할지 설명하세요.

**모범 답안**

내부 리스트의 참조를 그대로 내주면 받은 쪽이 add나 remove를 직접 호출할 수 있어, Order는 자신의 항목이 바뀌는 것을 알지도 막지도 못한다. 항목 추가와 삭제는 Order의 addLine, removeLine 같은 메서드로만 가능하게 하고, 그 안에서 개수 제한 같은 규칙을 검사해야 한다. 컬렉션 자체를 OrderLines 같은 별도 클래스로 감싸면 합계 계산이나 중복 검사처럼 컬렉션에 딸린 연산과 규칙도 그 클래스에 모을 수 있다. 조회가 필요하면 수정할 수 없는 뷰나 복사본을 반환한다.

**필수 개념**

- 내부 컬렉션 노출의 문제 · 가중치 0.50: 내부 리스트 참조를 반환하면 외부에서 직접 변경해 객체가 규칙을 지킬 수 없게 된다는 설명.
- 변경 메서드와 읽기 전용 접근 · 가중치 0.50: 추가·삭제는 규칙을 검사하는 메서드로만 허용하고 조회에는 수정 불가 뷰나 복사본을 제공한다는 설명.

**혼동 주의:** final 필드로 선언하면 컬렉션 내용도 보호된다고 설명하지 않기.

**출처 대조 위치**

- [Refactoring catalog — Encapsulate Collection](https://refactoring.com/catalog/encapsulateCollection.html) — Encapsulate Collection

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-OOD-09 · INTERMEDIATE

여러 메서드가 (double latitude, double longitude) 두 매개변수를 항상 함께 받고, 값의 범위를 검사하는 코드와 두 지점 사이의 거리를 계산하는 코드가 곳곳에 반복됩니다. 이 반복에서 어떤 도메인 개념을 찾을 수 있는지와, 객체로 만들었을 때의 이점을 설명하세요.

**모범 답안**

항상 함께 다니는 값들과 그 값에 대해 반복되는 검사·계산은 아직 이름을 얻지 못한 개념이 있다는 신호다. 여기서는 위도와 경도를 묶은 ‘좌표’가 그 개념이다. Coordinate 클래스를 만들어 생성 시점에 범위를 한 번 검사하면 잘못된 좌표가 시스템 안에 존재할 수 없게 되고, 흩어져 있던 검사 코드가 사라진다. 거리 계산은 distanceTo(other) 메서드가 되어 데이터와 함께 있게 된다. 매개변수 순서를 바꿔 넘기는 실수도 타입으로 막히고, 메서드 시그니처가 의도를 드러낸다.

**필수 개념**

- 반복되는 데이터 묶음과 숨은 개념 · 가중치 0.50: 함께 다니는 값과 반복되는 검사·계산이 이름 없는 도메인 개념의 신호라는 설명.
- 개념을 객체로 만든 효과 · 가중치 0.50: 생성 시 검증으로 잘못된 값의 존재를 막고 관련 연산을 모으며 타입으로 실수를 줄인다는 설명.

**혼동 주의:** 필드만 묶고 검증과 연산은 밖에 그대로 두는 것으로 답을 끝내지 않기.

**출처 대조 위치**

- [Martin Fowler — DataClump](https://martinfowler.com/bliki/DataClump.html) — Data Clump

근거 문서: `r1-object-oriented-design`. 검수 상태: **PENDING**

### R1-SWE-01 · BASIC

결제 메서드가 if (user != null) { if (user.isActive()) { if (amount > 0) { 본 처리 } } }처럼 세 단계로 중첩되어 있고, 각 else에서 오류를 반환합니다. 조건을 앞에서 걸러 조기 반환하는 형태로 바꾸면 읽는 사람이 기억해야 할 내용이 어떻게 달라지는지 설명하고, 이 방식이 맞지 않는 경우도 답하세요.

**모범 답안**

중첩 구조에서는 본 처리를 읽는 동안 바깥의 조건 세 개가 모두 참이라는 사실을 기억해야 하고, 각 else가 어느 if와 짝인지 찾아야 한다. 예외적인 경우를 메서드 앞부분에서 하나씩 검사해 바로 반환하면, 그 아래 코드는 이미 걸러진 상태만 다루므로 들여쓰기가 줄고 본 처리가 한눈에 드러난다. 다만 두 분기가 모두 정상 흐름이고 비중이 비슷하다면 if-else로 나란히 두는 편이 의도를 더 잘 보여 준다. 반환 지점마다 자원 정리가 필요한 코드라면 정리 누락에도 주의해야 한다.

**필수 개념**

- 중첩이 만드는 기억 부담 · 가중치 0.50: 중첩된 조건에서는 바깥 조건과 else의 짝을 기억하며 읽어야 한다는 설명.
- 보호 구문의 효과와 적용 조건 · 가중치 0.50: 예외 경우를 앞에서 반환해 본 흐름을 드러내되 대등한 두 분기에는 if-else가 낫다는 판단.

**혼동 주의:** 반환문은 메서드에 하나만 있어야 한다는 규칙을 근거 없이 적용하지 않기.

**출처 대조 위치**

- [Refactoring catalog — Replace Nested Conditional with Guard Clauses](https://refactoring.com/catalog/replaceNestedConditionalWithGuardClauses.html) — Replace Nested Conditional with Guard Clauses

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SWE-02 · INTERMEDIATE

주문 접수 메서드 하나에 ‘재고 확인’, ‘결제 요청’ 같은 단계 호출과, 문자열을 잘라 날짜를 파싱하는 세부 코드가 섞여 있습니다. 한 메서드에 서로 다른 추상화 수준이 섞이면 읽기 어려워지는 이유를 설명하고, 어떤 기준으로 메서드를 나눌지 답하세요.

**모범 답안**

단계를 설명하는 호출은 무엇을 하는지를 말하고, 문자열을 자르는 코드는 어떻게 하는지를 말한다. 둘이 섞이면 읽는 사람은 전체 흐름을 따라가다 세부 구현으로 내려갔다가 다시 올라와야 해서 흐름을 놓친다. 세부 코드는 그것이 하는 일을 이름으로 드러내는 메서드로 추출해, 원래 메서드에는 같은 수준의 단계만 남긴다. 나누는 기준은 길이가 아니라 의도와 구현의 거리다. 코드를 읽고 무엇을 하는지 알아내야 한다면 추출해 이름을 붙이고, 이름만으로 충분하면 본문을 읽지 않아도 된다.

**필수 개념**

- 의도와 구현의 혼재 · 가중치 0.50: 단계 수준의 호출과 세부 구현이 섞이면 읽는 흐름이 수준 사이를 오르내리게 된다는 설명.
- 의도를 드러내는 이름으로 추출 · 가중치 0.50: 세부 구현을 하는 일을 나타내는 이름의 메서드로 추출해 한 메서드에 같은 수준만 남긴다는 기준.

**혼동 주의:** 메서드는 몇 줄 이하여야 한다는 숫자 기준만으로 답하지 않기.

**출처 대조 위치**

- [Martin Fowler — FunctionLength](https://martinfowler.com/bliki/FunctionLength.html) — FunctionLength: intention and implementation

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SWE-03 · INTERMEDIATE

getNextOrderNumber()라는 메서드가 번호를 반환하면서 내부 카운터도 1 증가시킵니다. 이 메서드를 로그 출력용으로 한 번 더 호출하자 번호가 건너뛰는 버그가 생겼습니다. 이름이 무엇을 감추고 있었는지 설명하고, 조회와 변경을 어떻게 드러내야 하는지 답하세요.

**모범 답안**

get으로 시작하는 이름은 값을 읽기만 하고 상태를 바꾸지 않는다고 읽힌다. 그런데 이 메서드는 호출할 때마다 카운터를 증가시키는 부수 효과가 있어, 호출 횟수에 따라 결과와 시스템 상태가 달라진다. 이름이 부수 효과를 감췄기 때문에 호출자는 여러 번 불러도 안전하다고 오해했다. 상태를 바꾸는 메서드는 issueOrderNumber()처럼 변경을 드러내는 동사로 짓고, 값을 묻기만 하는 메서드는 부수 효과 없이 몇 번을 호출해도 같게 만든다. 조회와 변경을 한 메서드에 섞지 않으면 호출 순서와 횟수를 걱정할 일이 줄어든다.

**필수 개념**

- 이름이 감춘 부수 효과 · 가중치 0.50: 조회처럼 보이는 이름의 메서드가 상태를 바꾸면 호출자가 반복 호출의 안전성을 오해한다는 설명.
- 조회와 변경의 분리 · 가중치 0.50: 상태를 바꾸는 메서드는 변경을 드러내는 이름으로, 조회 메서드는 부수 효과 없이 만든다는 기준.

**혼동 주의:** 이름 규칙을 ‘get/set 접두사를 맞추는 문제’로만 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — CommandQuerySeparation](https://martinfowler.com/bliki/CommandQuerySeparation.html) — CommandQuerySeparation: do not change the observable state of the system

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SWE-04 · BASIC

코드 여러 곳에 if (retryCount > 3)과 Thread.sleep(86400000)이 있습니다. 숫자를 이름 있는 상수로 바꾸면 무엇이 좋아지는지 설명하고, 반대로 int ZERO = 0처럼 상수로 만들 필요가 없는 경우를 구분하는 기준을 답하세요.

**모범 답안**

숫자만으로는 3이 최대 재시도 횟수인지, 86400000이 하루를 밀리초로 나타낸 값인지 알기 어렵다. MAX_RETRY_COUNT나 ONE_DAY_MILLIS처럼 이름을 붙이면 값의 의미가 코드에 드러나고, 같은 의미의 값이 한 곳에서 정의되어 정책이 바뀔 때 그 한 줄만 고치면 된다. 우연히 값이 같은 다른 3과 구분되는 효과도 있다. 반면 이름이 값 이상의 정보를 주지 못하는 ZERO, 반복문의 시작값 0, 공식에 원래 들어 있는 2 같은 값은 상수로 만들어도 읽기에 도움이 되지 않고 오히려 한 번 더 찾아보게 한다.

**필수 개념**

- 의미를 드러내는 이름과 단일 정의 · 가중치 0.50: 숫자에 이름을 붙여 뜻을 드러내고 같은 의미의 값을 한 곳에서 정의해 변경을 쉽게 한다는 설명.
- 상수화가 불필요한 값의 기준 · 가중치 0.50: 이름이 값 이상의 정보를 주지 못하거나 문맥상 자명한 값은 상수로 만들 필요가 없다는 판단.

**혼동 주의:** 숫자는 모두 상수로 빼야 한다고 일률적으로 답하지 않기.

**출처 대조 위치**

- [Refactoring catalog — Replace Magic Literal](https://refactoring.com/catalog/replaceMagicLiteral.html) — Replace Magic Literal

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SWE-05 · INTERMEDIATE

메서드 위에 ‘// 할인율 10% 적용’이라는 주석이 있는데, 코드는 얼마 전 15%로 바뀌었습니다. 이런 어긋남이 생기는 이유를 설명하고, 주석으로 남길 내용과 이름·코드 구조로 드러낼 내용을 어떻게 구분할지 답하세요.

**모범 답안**

주석은 실행되지 않아 코드가 바뀔 때 함께 고쳐지지 않아도 아무 오류가 나지 않는다. 코드가 하는 일을 그대로 되풀이하는 주석일수록 수정할 때 빠뜨리기 쉽고, 어긋난 주석은 읽는 사람을 잘못 이끈다. 무엇을 하는지는 변수·메서드 이름과 구조로 드러내는 편이 낫다. 예를 들어 할인율을 DISCOUNT_RATE 상수로 두면 설명 주석이 필요 없다. 주석에는 코드만으로 알 수 없는 것, 즉 왜 이 방식을 택했는지, 어떤 제약이나 외부 사정 때문인지, 주의할 함정이 무엇인지를 적는다.

**필수 개념**

- 코드와 주석이 어긋나는 원인 · 가중치 0.50: 주석은 실행·검사되지 않아 코드 변경 때 함께 갱신되지 않고, 코드를 반복하는 주석일수록 어긋나기 쉽다는 설명.
- 무엇은 코드로, 왜는 주석으로 · 가중치 0.50: 하는 일은 이름과 구조로 드러내고 주석에는 선택 이유·제약·주의점처럼 코드에 없는 정보를 적는다는 구분.

**혼동 주의:** 좋은 코드에는 주석이 전혀 없어야 한다고 단정하지 않기.

**출처 대조 위치**

- [Martin Fowler — CodeAsDocumentation](https://martinfowler.com/bliki/CodeAsDocumentation.html) — Code As Documentation

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SWE-06 · INTERMEDIATE

결제 수단이 카드 하나뿐인데, 나중에 늘어날 것에 대비해 PaymentStrategy 인터페이스와 팩토리, 설정 기반 플러그인 구조를 미리 만들자는 제안이 나왔습니다. 지금 필요하지 않은 확장 구조를 미리 만들 때의 비용을 설명하고, 그렇다고 설계를 미루기만 하면 안 되는 이유도 답하세요.

**모범 답안**

미리 만든 구조에는 만드는 비용, 그 시간에 다른 기능을 내지 못한 지연 비용, 이후 계속 읽고 유지해야 하는 복잡도 비용이 든다. 예상이 빗나가면 쓰이지 않는 구조가 남고, 예상대로 요구가 오더라도 그때는 요구를 더 잘 알게 되어 미리 만든 형태가 맞지 않는 경우가 많다. 그래서 실제 요구가 생길 때 확장 구조를 도입하는 편이 낫다. 다만 이것은 나중에 구조를 바꾸기 쉬울 때 성립한다. 테스트와 읽기 쉬운 코드처럼 변경을 쉽게 만드는 일은 지금 해야 하며, 두 번째 결제 수단이 실제로 들어올 때 인터페이스를 추출하면 된다.

**필수 개념**

- 추측한 확장 구조의 비용 · 가중치 0.50: 지금 필요 없는 구조는 구축·지연·유지 비용을 내고 예상이 틀리거나 형태가 맞지 않을 위험이 있다는 설명.
- 변경 용이성이라는 전제 · 가중치 0.50: 구조 도입을 미루는 판단은 나중에 바꾸기 쉬울 때 성립하므로 테스트와 단순한 코드는 지금 갖춰야 한다는 설명.

**혼동 주의:** YAGNI를 ‘설계와 테스트를 하지 않아도 된다’는 뜻으로 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — Yagni](https://martinfowler.com/bliki/Yagni.html) — Yagni: cost of build; cost of delay; cost of carry

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SWE-07 · INTERMEDIATE

한 개발자가 2주 동안 작업한 변경 3,000줄을 하나의 Pull Request로 올렸습니다. 리뷰어는 대충 훑어보고 승인했고, 배포 뒤 결함이 발견됐습니다. 변경 묶음이 클 때 리뷰가 제 역할을 못 하는 이유를 설명하고, 변경을 어떻게 나눠 올려야 하는지 답하세요.

**모범 답안**

변경이 크면 리뷰어가 전체 맥락을 머리에 담기 어렵고 검토에 긴 시간을 따로 내야 해서, 리뷰가 늦어지거나 형식적인 승인으로 끝나기 쉽다. 여러 목적의 변경이 섞여 있으면 문제가 생겼을 때 원인을 좁히기 어렵고 일부만 되돌릴 수도 없다. 한 Pull Request는 하나의 목적만 담고, 그 자체로 빌드와 테스트가 통과하는 독립된 단위여야 한다. 리팩터링과 기능 변경을 분리하고, 큰 기능은 순서대로 쌓이는 작은 변경들로 나눈다. 작게 나누면 리뷰가 빠르고 꼼꼼해지며 병합 충돌과 되돌리기 비용도 줄어든다.

**필수 개념**

- 큰 변경이 리뷰를 약하게 만드는 이유 · 가중치 0.50: 변경이 크면 맥락 파악과 시간 확보가 어려워 리뷰가 지연되거나 형식적이 되고 원인 추적과 부분 롤백이 어렵다는 설명.
- 독립적으로 검증되는 작은 변경 · 가중치 0.50: 하나의 목적만 담고 스스로 빌드·테스트가 통과하는 단위로 나누며 리팩터링과 기능 변경을 분리한다는 기준.

**혼동 주의:** 리뷰의 목적을 ‘버그를 모두 찾아내는 것’ 하나로만 설명하지 않기.

**출처 대조 위치**

- [Google Engineering Practices — Small CLs](https://google.github.io/eng-practices/review/developer/small-cls.html) — Small CLs: Why Write Small CLs?; What is Small?

근거 문서: `r1-software-engineering`. 검수 상태: **PENDING**

### R1-SPR-01 · INTERMEDIATE

Spring MVC에서 GET /orders/7 요청이 들어와 @GetMapping이 붙은 컨트롤러 메서드가 실행되기까지, DispatcherServlet은 HandlerMapping과 HandlerAdapter에 각각 무엇을 맡기나요? 두 역할을 하나로 합치지 않고 나눈 이유도 설명하세요.

**모범 답안**

DispatcherServlet은 모든 요청을 먼저 받는 프런트 컨트롤러이고 실제 일은 위임한다. HandlerMapping은 요청의 경로와 메서드 같은 조건으로 처리할 핸들러와 그 앞뒤에 적용할 인터셉터 목록을 찾아 준다. HandlerAdapter는 찾은 핸들러를 실제로 호출한다. 애노테이션 기반 컨트롤러라면 매개변수를 풀어 넣고 반환값을 처리하는 일이 여기에 속한다. 찾는 일과 호출하는 일을 나누면 DispatcherServlet은 핸들러가 어떤 형태인지 몰라도 되고, 새로운 형태의 핸들러는 어댑터를 추가해 지원할 수 있다.

**필수 개념**

- 핸들러 탐색과 호출의 분담 · 가중치 0.50: HandlerMapping은 요청에 맞는 핸들러와 인터셉터를 찾고 HandlerAdapter는 그 핸들러를 호출한다는 설명.
- 호출 방식을 감추는 어댑터 · 가중치 0.50: DispatcherServlet이 핸들러 형태를 몰라도 되도록 호출 세부를 HandlerAdapter가 감춘다는 설명.

**혼동 주의:** DispatcherServlet이 컨트롤러 메서드를 직접 찾아 호출한다고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Special Bean Types](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-servlet/special-bean-types.html) — Special Bean Types: Map a request to a handler along with a list of interceptors; Help the DispatcherServlet to invoke a handler mapped to a request

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-02 · INTERMEDIATE

같은 컨트롤러 클래스에서 한 메서드는 문자열 ‘orders/list’를 반환해 HTML 화면을 보여 주고, @ResponseBody가 붙은 다른 메서드는 주문 객체를 반환해 JSON을 내려 줍니다. 반환값이 응답이 되기까지의 경로가 두 경우에 어떻게 다른지 설명하세요.

**모범 답안**

@ResponseBody가 없으면 반환한 문자열은 뷰 이름으로 해석된다. 뷰 리졸버가 이름에 맞는 뷰를 찾고 그 뷰가 모델 데이터를 사용해 HTML을 만든다. @ResponseBody가 붙으면 뷰를 거치지 않고 반환 객체가 HttpMessageConverter를 통해 응답 본문으로 직렬화된다. 어떤 컨버터가 쓰일지는 반환 타입과 요청의 Accept 헤더 등으로 정해지며, 객체를 JSON으로 쓰는 컨버터가 선택되면 JSON이 나간다. @RestController는 클래스의 모든 메서드에 @ResponseBody를 적용한 것과 같다.

**필수 개념**

- 뷰 이름 해석과 렌더링 · 가중치 0.50: @ResponseBody가 없을 때 문자열 반환값이 뷰 이름으로 해석되어 뷰가 응답을 만든다는 설명.
- 메시지 컨버터를 통한 본문 직렬화 · 가중치 0.50: @ResponseBody가 있으면 반환 객체가 HttpMessageConverter로 응답 본문에 직접 쓰인다는 설명.

**혼동 주의:** @ResponseBody를 ‘JSON 전용 애노테이션’으로 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — @ResponseBody](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/responsebody.html) — @ResponseBody: serialized to the response body

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-03 · BASIC

상품 등록 폼을 POST로 처리한 뒤 컨트롤러가 등록 완료 화면을 바로 렌더링해 응답했습니다. 사용자가 그 화면에서 새로고침을 누르면 어떤 일이 생기나요? redirect로 응답하면 무엇이 달라지는지, 완료 메시지 같은 값은 어떻게 다음 화면에 넘기는지 설명하세요.

**모범 답안**

POST의 응답으로 화면을 바로 내려 주면 브라우저의 마지막 요청이 그 POST로 남는다. 새로고침은 마지막 요청을 다시 보내므로 같은 등록이 한 번 더 실행될 수 있다. 처리 뒤 상세 화면 주소로 redirect하면 브라우저가 그 주소를 GET으로 다시 요청하고, 이후 새로고침은 조회만 반복한다. 이 흐름을 Post-Redirect-Get이라 한다. redirect는 새 요청이라 이전 요청의 모델이 이어지지 않으므로, 한 번만 보여 줄 메시지는 플래시 속성에 담는다. 플래시 속성은 redirect 전에 잠시 저장됐다가 다음 요청에서 꺼낸 뒤 바로 제거된다.

**필수 개념**

- 새로고침에 의한 POST 재전송 · 가중치 0.50: POST 응답으로 화면을 바로 주면 새로고침이 같은 POST를 다시 보내 중복 처리가 생길 수 있다는 설명.
- redirect 후 GET과 플래시 속성 · 가중치 0.50: redirect로 마지막 요청을 GET으로 바꾸고 일회성 값은 플래시 속성으로 다음 요청에 전달한다는 설명.

**혼동 주의:** PRG만으로 서버의 중복 처리 방지가 완성된다고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Flash Attributes](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/flash-attributes.html) — Flash Attributes: Post-Redirect-Get pattern; saved temporarily before the redirect

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-04 · INTERMEDIATE

폼 객체를 @Valid @ModelAttribute로 받는 컨트롤러 메서드가 있습니다. 숫자 필드에 문자를 입력하면 컨트롤러 본문이 실행되지도 않고 오류 응답이 나갑니다. BindingResult 매개변수를 추가하면 흐름이 어떻게 달라지나요? 선언 위치에 어떤 제약이 있는지도 설명하세요.

**모범 답안**

바인딩이나 검증에서 오류가 나면 기본적으로 예외가 발생해 컨트롤러 메서드가 호출되지 않는다. 검증 대상 인자 바로 다음에 BindingResult를 선언하면 오류가 예외로 던져지는 대신 그 객체에 담기고 메서드 본문이 실행된다. 컨트롤러는 hasErrors()로 확인해 입력값과 오류 메시지를 담은 폼 화면을 다시 보여 줄 수 있다. BindingResult는 타입 변환 실패 같은 바인딩 오류와 검증 애노테이션 위반을 함께 담는다. 반드시 검증되는 인자 바로 뒤에 있어야 하며, 다른 매개변수가 사이에 끼면 어느 객체의 결과인지 연결되지 않는다.

**필수 개념**

- 오류를 예외 대신 결과 객체로 받기 · 가중치 0.50: BindingResult를 선언하면 바인딩·검증 오류가 예외가 아니라 결과 객체에 담겨 컨트롤러가 직접 처리한다는 설명.
- 검증 대상 바로 뒤라는 선언 위치 · 가중치 0.50: BindingResult는 검증되는 인자 바로 다음에 선언해야 그 인자의 결과와 연결된다는 제약.

**혼동 주의:** BindingResult가 있으면 검증 자체가 생략된다고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Method Arguments](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/arguments.html) — Method Arguments: immediately after the validated method argument

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-05 · INTERMEDIATE

주문 요청 DTO의 수량 필드에 @Min(1)을 붙였습니다. ‘주문 수량은 현재 재고를 넘을 수 없다’는 규칙도 같은 방식으로 DTO 애노테이션에 넣으려 합니다. 두 규칙의 성격이 어떻게 다른지, 재고 규칙은 어디에서 확인해야 하는지 설명하세요.

**모범 답안**

@Min(1)은 요청 값 하나만 보고 판단할 수 있는 형식 제약이다. 선언적 제약 애노테이션은 이런 필수 여부·범위·길이 검사에 맞고, 컨트롤러에 들어오는 시점에 걸러 낼 수 있다. 재고 규칙은 요청 값만으로는 판단할 수 없고 현재 재고라는 시스템 상태를 읽어야 한다. 또 확인과 차감 사이에 다른 주문이 끼어들 수 있으므로, 재고를 소유한 도메인 객체나 서비스가 트랜잭션 안에서 확인하고 변경해야 한다. DTO 검증을 통과했다는 사실은 입력 형식이 맞다는 뜻일 뿐 업무 규칙을 지켰다는 보장이 아니다.

**필수 개념**

- 값만으로 판단하는 형식 제약 · 가중치 0.50: 필수·범위·길이처럼 요청 값만으로 판단되는 제약은 선언적 검증 애노테이션에 맞다는 설명.
- 상태에 의존하는 업무 규칙의 위치 · 가중치 0.50: 현재 상태를 읽어야 하는 규칙은 상태를 소유한 도메인·서비스가 트랜잭션 안에서 확인해야 한다는 설명.

**혼동 주의:** Bean Validation을 통과하면 업무 규칙도 지켜진다고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Java Bean Validation](https://docs.spring.io/spring-framework/reference/7.0/core/validation/beanvalidation.html) — Java Bean Validation: Overview of Bean Validation; declarative validation constraints

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-06 · INTERMEDIATE

모든 요청의 처리 시간을 기록하는 기능과, 로그인 여부를 확인해 접근을 막는 기능을 추가하려 합니다. 서블릿 필터와 Spring MVC 인터셉터가 실행되는 위치는 어떻게 다른가요? 인증처럼 접근을 막는 기능을 인터셉터에만 의존할 때의 문제를 설명하세요.

**모범 답안**

서블릿 필터는 서블릿 컨테이너가 관리하며 요청이 DispatcherServlet에 도달하기 전과 처리가 끝나 돌아오는 길에 실행된다. 정적 자원처럼 Spring MVC가 처리하지 않는 요청에도 적용된다. 인터셉터는 DispatcherServlet 안에서 핸들러가 정해진 뒤에 실행되며 핸들러 실행 전과 후, 요청 완료 시점의 콜백을 가진다. 어떤 컨트롤러 메서드가 처리하는지 알 수 있다는 장점이 있다. 그러나 인터셉터의 경로 패턴과 실제 컨트롤러 매핑이 어긋나면 보호해야 할 요청이 빠질 수 있다. 그래서 접근 차단은 필터 체인 쪽에서 가능한 한 일찍 적용하는 것이 권장된다.

**필수 개념**

- 필터와 인터셉터의 실행 위치 · 가중치 0.50: 필터는 DispatcherServlet 앞의 서블릿 컨테이너 단계, 인터셉터는 핸들러가 정해진 뒤 MVC 안에서 실행된다는 구분.
- 인터셉터 기반 접근 차단의 한계 · 가중치 0.50: 경로 매칭 불일치로 요청이 빠질 수 있어 보안 검사는 필터 체인에서 일찍 적용한다는 설명.

**혼동 주의:** 필터와 인터셉터를 ‘이름만 다른 같은 기능’으로 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Interception](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-servlet/handlermapping-interceptor.html) — Interception: callback before the actual handler is run; not ideally suited as a security layer

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-07 · INTERMEDIATE

여러 컨트롤러 메서드가 세션에서 로그인 회원을 꺼내는 코드를 똑같이 반복합니다. 이를 줄이려고 @LoginMember Member member 형태의 매개변수를 직접 지원하려 합니다. HandlerMethodArgumentResolver의 두 메서드가 각각 무엇을 결정하는지, 이 장치에 인증 실패 처리까지 맡길 때 주의할 점을 설명하세요.

**모범 답안**

HandlerMethodArgumentResolver는 컨트롤러 메서드의 매개변수 하나를 현재 요청으로부터 만들어 내는 전략이다. supportsParameter는 주어진 매개변수를 이 리졸버가 처리할지 판단하며, 여기서는 @LoginMember 애노테이션과 Member 타입인지를 확인한다. resolveArgument는 실제 인자 값을 만들어 반환하며 세션에서 회원 정보를 꺼내는 코드가 여기로 모인다. 다만 리졸버는 그 매개변수를 선언한 메서드에서만 실행되므로, 매개변수를 선언하지 않은 메서드는 아무 검사 없이 통과한다. 접근 차단은 필터나 보안 프레임워크에 두고, 리졸버는 이미 인증된 요청에서 값을 꺼내 전달하는 역할로 한정한다.

**필수 개념**

- 지원 여부 판단과 값 생성의 분리 · 가중치 0.50: supportsParameter가 처리 대상 매개변수를 고르고 resolveArgument가 요청에서 인자 값을 만든다는 설명.
- 값 전달 장치와 접근 차단의 구분 · 가중치 0.50: 리졸버는 매개변수를 선언한 메서드에서만 동작하므로 인증 차단은 별도 계층에 둔다는 주의.

**혼동 주의:** ArgumentResolver를 인증 검사를 강제하는 수단으로 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 API — HandlerMethodArgumentResolver](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/web/method/support/HandlerMethodArgumentResolver.html) — HandlerMethodArgumentResolver: Strategy interface for resolving method parameters into argument values

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-08 · INTERMEDIATE

결제 수단마다 PaymentProcessor 구현체가 빈으로 등록되어 있습니다. 서비스가 요청에 담긴 수단 이름으로 구현체를 골라야 합니다. 생성자에서 Map<String, PaymentProcessor>를 주입받으면 스프링은 무엇을 채워 주나요? if 분기로 구현체를 고르는 방식과 비교해 설명하세요.

**모범 답안**

값 타입이 PaymentProcessor이고 키 타입이 String인 Map을 주입 지점에 선언하면, 스프링은 그 타입의 빈을 모두 찾아 빈 이름을 키로, 빈 인스턴스를 값으로 채워 준다. 서비스는 수단 이름에 대응하는 빈을 Map에서 꺼내 호출하면 된다. if 분기 방식은 구현체가 늘 때마다 서비스 코드를 고쳐야 하지만, Map 주입은 새 구현체를 빈으로 등록하는 것만으로 선택 대상에 포함된다. 다만 키가 빈 이름이므로 요청 값과 빈 이름의 대응 규칙을 정해야 하고, 대응하는 빈이 없을 때의 처리도 직접 넣어야 한다.

**필수 개념**

- 타입에 맞는 모든 빈의 Map 주입 · 가중치 0.50: 키가 String인 Map을 선언하면 해당 타입의 모든 빈이 빈 이름을 키로 주입된다는 설명.
- 분기 대신 등록으로 확장 · 가중치 0.50: 구현체 추가 시 분기 수정 없이 빈 등록만으로 선택 대상이 늘고 없는 키 처리는 직접 해야 한다는 설명.

**혼동 주의:** Map의 키가 클래스 이름이나 임의의 문자열이라고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Using @Autowired](https://docs.spring.io/spring-framework/reference/7.0/core/beans/annotation-config/autowired.html) — Using @Autowired: the expected key type is String

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-09 · BASIC

서비스 클래스의 의존 객체를 필드에 @Autowired를 붙여 주입받고 있습니다. 이를 생성자 주입으로 바꾸면 객체의 상태와 테스트 작성에서 무엇이 달라지나요? 필수 의존성과 선택 의존성을 어떤 주입 방식에 맡기는지도 설명하세요.

**모범 답안**

생성자 주입은 객체를 만드는 순간 필요한 의존 객체를 모두 받는다. 필드를 final로 선언할 수 있어 생성 뒤 바뀌지 않고, 의존성이 빠진 채 만들어진 객체가 존재할 수 없다. 테스트에서는 컨테이너 없이 new로 대역 객체를 넘겨 만들 수 있다. 필드 주입은 컨테이너가 리플렉션으로 값을 넣어 주므로 컨테이너 밖에서는 의존성을 채울 방법이 마땅치 않고, 의존성이 늘어도 드러나지 않는다. 필수 의존성은 생성자로 받고, 없어도 동작하는 선택 의존성은 setter 같은 메서드로 받는 것이 일반적인 기준이다.

**필수 개념**

- 생성 시점에 완성되는 객체 · 가중치 0.50: 생성자 주입은 필수 의존성을 생성 시 모두 받아 불변으로 유지하고 누락된 상태의 객체를 막는다는 설명.
- 필수는 생성자, 선택은 setter · 가중치 0.50: 필수 의존성은 생성자로, 선택 의존성은 setter로 받으며 생성자 주입이 컨테이너 없는 테스트를 쉽게 한다는 기준.

**혼동 주의:** 생성자 주입의 이유를 ‘순환 참조를 막기 위해서’ 하나로만 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Dependency Injection](https://docs.spring.io/spring-framework/reference/7.0/core/beans/dependencies/factory-collaborators.html) — Dependency Injection: Constructor-based or setter-based DI?; immutable objects

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-10 · BASIC

서비스 메서드 수십 개에 실행 시간 측정 코드가 똑같이 복사되어 있습니다. 이 코드를 AOP로 옮기려 합니다. 이런 기능을 무엇이라 부르며 왜 일반적인 클래스 분리로는 정리하기 어려운가요? 애스펙트, 조인 포인트, 어드바이스가 각각 무엇을 가리키는지 이 사례에 대입해 설명하세요.

**모범 답안**

실행 시간 측정처럼 여러 클래스에 걸쳐 나타나는 기능을 횡단 관심사라 한다. 측정 코드를 별도 클래스로 빼더라도 각 메서드가 그 클래스를 호출하는 코드는 여전히 흩어져 남는다. AOP는 이 호출 자체를 대상 코드 밖에서 끼워 넣는다. 애스펙트는 횡단 관심사를 모듈로 묶은 것으로, 여기서는 시간 측정 모듈이다. 조인 포인트는 프로그램 실행 중의 한 지점이며 Spring AOP에서는 항상 메서드 실행이다. 어드바이스는 특정 조인 포인트에서 애스펙트가 하는 동작으로, 서비스 메서드 실행 전후에 시간을 재고 기록하는 코드가 여기에 해당한다.

**필수 개념**

- 횡단 관심사와 흩어지는 호출 · 가중치 0.50: 여러 클래스에 걸친 기능은 분리해도 호출 코드가 흩어져 남으며 AOP가 이를 대상 밖에서 끼워 넣는다는 설명.
- 애스펙트·조인 포인트·어드바이스 · 가중치 0.50: 애스펙트는 관심사 모듈, 조인 포인트는 메서드 실행 지점, 어드바이스는 그 지점에서 수행하는 동작이라는 대응.

**혼동 주의:** AOP를 ‘로그를 찍는 기술’로만 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — AOP Concepts](https://docs.spring.io/spring-framework/reference/7.0/core/aop/introduction-defn.html) — AOP Concepts: A modularization of a concern that cuts across multiple classes; a join point always represents a method execution

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-11 · INTERMEDIATE

같은 클래스 안에서 save() 메서드가 @Transactional이 붙은 saveAll()을 this로 호출합니다. 외부에서 save()를 호출했을 때 saveAll()의 트랜잭션 설정이 적용되지 않았습니다. 프록시 기반 AOP의 동작으로 원인을 설명하고 해결 방법을 제시하세요.

**모범 답안**

Spring AOP는 대상 객체를 감싼 프록시를 빈으로 등록하고, 외부 호출이 프록시를 거칠 때 어드바이스를 실행한 뒤 대상 메서드를 부른다. 외부에서 save()를 호출하면 프록시를 거치지만, save() 안에서 this.saveAll()을 부르는 것은 프록시가 아닌 대상 객체 자신에 대한 호출이다. 그래서 saveAll()에 연결된 트랜잭션 어드바이스가 실행되지 않는다. 가장 좋은 해결은 내부 호출이 생기지 않도록 구조를 바꾸는 것이다. 트랜잭션이 필요한 메서드를 다른 빈으로 옮겨 그 빈을 주입받아 호출하면 프록시를 거치게 된다. 자기 자신의 프록시를 주입받는 방법도 있지만 구조를 고치는 편이 덜 침투적이다.

**필수 개념**

- 프록시를 거치는 호출만 적용 · 가중치 0.50: 어드바이스는 프록시를 통한 호출에서만 실행되고 this를 통한 내부 호출은 프록시를 우회한다는 설명.
- 내부 호출을 없애는 구조 변경 · 가중치 0.50: 대상 메서드를 다른 빈으로 분리해 프록시를 거치게 하는 것이 우선이며 자기 프록시 주입은 차선이라는 해결.

**혼동 주의:** @Transactional을 붙이기만 하면 호출 경로와 무관하게 적용된다고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/7.0/core/aop/proxying.html) — Proxying Mechanisms: Understanding AOP Proxies; will bypass the advice

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-SPR-12 · INTERMEDIATE

인터페이스를 구현한 서비스 빈과 인터페이스 없이 클래스만 있는 서비스 빈에 같은 AOP를 적용했습니다. 별도 설정이 없는 Spring Framework의 기본 동작에서 두 빈에 만들어지는 프록시는 어떻게 다른가요? 클래스 기반 프록시에서 final 메서드에 어드바이스가 적용되지 않는 이유도 설명하세요.

**모범 답안**

기본 동작에서 대상 객체가 인터페이스를 하나라도 구현하면 JDK 동적 프록시가 쓰이고, 대상이 구현한 인터페이스들을 프록시가 구현한다. 인터페이스가 없으면 CGLIB으로 대상 클래스의 하위 클래스를 실행 중에 만들어 프록시로 쓴다. 클래스 기반 프록시는 메서드를 재정의해 호출을 가로채므로, 재정의할 수 없는 final 메서드에는 어드바이스를 적용할 수 없고 final 클래스는 프록시 자체를 만들 수 없다. JDK 프록시는 인터페이스만 구현하므로 그 빈을 구현 클래스 타입으로 주입받을 수 없다는 점도 함께 고려한다.

**필수 개념**

- 인터페이스 유무에 따른 프록시 방식 · 가중치 0.50: 인터페이스가 있으면 JDK 동적 프록시, 없으면 대상의 하위 클래스를 만드는 CGLIB 프록시가 쓰인다는 설명.
- 상속 기반 프록시의 final 제약 · 가중치 0.50: 하위 클래스가 재정의로 가로채므로 final 메서드는 어드바이스할 수 없고 final 클래스는 프록시할 수 없다는 설명.

**혼동 주의:** 모든 스프링 프록시가 인터페이스를 필요로 한다고 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Reference — Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/7.0/core/aop/proxying.html) — Proxying Mechanisms: a JDK dynamic proxy is used; runtime-generated subclass of the target type

근거 문서: `r1-spring-framework`. 검수 상태: **PENDING**

### R1-JPA-01 · BASIC

회원과 주문을 JDBC와 SQL로 직접 다루는 코드에서, 주문에서 회원을 얻으려면 주문 테이블의 member_id 값으로 SELECT를 한 번 더 실행해 객체를 조립합니다. 객체의 참조와 테이블의 외래 키는 연관을 표현하는 방식이 어떻게 다른가요? JPA가 이 차이를 메우기 위해 맡는 일을 설명하세요.

**모범 답안**

객체는 다른 객체의 참조를 필드로 가지고, 참조를 따라가면 연관된 객체에 닿는다. 테이블은 외래 키 값을 저장하고, 연관된 행은 조인이나 추가 조회로 찾는다. SQL을 직접 쓰면 개발자가 외래 키 값을 꺼내 조회하고 결과를 객체로 조립하는 코드를 매번 작성해야 한다. JPA에서는 엔티티의 연관 필드에 매핑 정보를 선언하면 영속성 제공자가 외래 키 컬럼과 참조 사이의 변환을 맡는다. 저장할 때는 참조로부터 외래 키 값을 채우고, 조회할 때는 외래 키로 연관 엔티티를 불러와 참조로 연결한다. 그렇다고 SQL이 사라지는 것은 아니므로 실행되는 쿼리는 여전히 확인해야 한다.

**필수 개념**

- 참조와 외래 키의 표현 차이 · 가중치 0.50: 객체는 참조를 따라가고 테이블은 외래 키 값과 조인으로 연관을 찾는다는 차이.
- 매핑을 통한 변환 위임 · 가중치 0.50: 연관 매핑을 선언하면 영속성 제공자가 참조와 외래 키 사이의 변환과 조회를 맡는다는 설명.

**혼동 주의:** ORM을 쓰면 SQL을 몰라도 된다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 2.11 Entity Relationships

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-02 · INTERMEDIATE

한 트랜잭션 안에서 엔티티의 가격을 바꾼 직후 JPQL로 ‘가격이 1만 원 이상인 상품’을 조회했습니다. 아직 커밋하지 않았는데도 방금 바꾼 값이 조회 결과에 반영됩니다. flush가 하는 일과 커밋의 차이를 설명하고, 이 뒤에 롤백하면 어떻게 되는지 답하세요.

**모범 답안**

flush는 영속성 컨텍스트에 쌓인 변경을 SQL로 데이터베이스에 보내는 동기화다. 기본 플러시 모드에서는 쿼리 실행 전에 영속성 컨텍스트의 변경이 쿼리 결과에 보이도록 보장해야 하므로, JPQL 조회 직전에 UPDATE가 먼저 나간다. 그러나 flush는 트랜잭션을 끝내지 않는다. 보낸 SQL은 아직 같은 트랜잭션의 미확정 변경이고, 커밋이 되어야 확정되어 다른 트랜잭션에 보인다. 따라서 flush 뒤에 롤백하면 데이터베이스의 변경은 모두 취소된다. 커밋할 때는 남은 변경을 flush한 다음 트랜잭션을 확정한다.

**필수 개념**

- flush는 동기화, 커밋은 확정 · 가중치 0.50: flush는 변경 SQL을 DB에 보낼 뿐 트랜잭션을 끝내지 않으며 롤백하면 취소된다는 구분.
- 쿼리 전 자동 flush · 가중치 0.50: 기본 플러시 모드에서 쿼리 결과에 미반영 변경이 보이도록 쿼리 실행 전에 flush가 일어난다는 설명.

**혼동 주의:** flush를 하면 다른 트랜잭션에서도 바로 보인다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 3.3.4 Synchronization to the Database; 3.11.2 Queries and Flush Mode

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-03 · INTERMEDIATE

트랜잭션 안에서 조회한 엔티티의 필드를 바꾸면 save 같은 호출 없이도 UPDATE가 실행됩니다. 그런데 트랜잭션이 끝난 뒤 컨트롤러에서 같은 객체의 필드를 바꾸면 아무 SQL도 나가지 않습니다. 두 경우의 엔티티 상태가 어떻게 다른지, 뒤의 변경을 저장하려면 어떻게 해야 하는지 설명하세요.

**모범 답안**

트랜잭션 안에서 조회한 엔티티는 영속성 컨텍스트가 관리하는 영속 상태다. 영속성 제공자는 관리 중인 엔티티의 변경을 flush 시점에 찾아 UPDATE를 만든다. 트랜잭션 범위의 영속성 컨텍스트가 끝나면 엔티티는 식별자는 있지만 더는 관리되지 않는 준영속 상태가 된다. 이 상태의 객체는 평범한 자바 객체와 같아서 필드를 바꿔도 추적되지 않는다. 저장하려면 새 트랜잭션에서 식별자로 다시 조회해 관리되는 엔티티의 값을 바꾸거나, merge로 준영속 객체의 상태를 관리되는 엔티티에 복사한다. merge는 넘긴 객체가 아니라 반환된 객체가 관리 대상이라는 점에 주의한다.

**필수 개념**

- 영속 상태에서만 변경 추적 · 가중치 0.50: 영속성 컨텍스트가 관리하는 엔티티의 변경만 flush 때 반영되고 준영속 엔티티의 변경은 추적되지 않는다는 설명.
- 재조회 후 변경 또는 merge · 가중치 0.50: 새 트랜잭션에서 다시 조회해 바꾸거나 merge로 상태를 복사하며 merge는 반환된 객체가 관리된다는 설명.

**혼동 주의:** merge에 넘긴 객체가 그대로 영속 상태가 된다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 3.3.7 Detached Entities; 3.3.7.1 Merging Detached Entity State

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-04 · INTERMEDIATE

개발 환경에서는 애플리케이션을 시작할 때 엔티티 매핑으로 테이블을 자동 생성하도록 설정해 두었습니다. 같은 설정을 운영 데이터베이스에 그대로 적용하면 어떤 위험이 있나요? 운영 스키마 변경은 어떤 방식으로 관리해야 하는지 설명하세요.

**모범 답안**

스키마 자동 생성은 매핑 정보로 테이블을 만들거나 지우는 기능이라 빈 데이터베이스로 빠르게 시작하는 개발과 테스트에 맞다. 운영에 삭제 후 생성 동작을 적용하면 기존 데이터가 사라진다. 구현체가 제공하는 자동 변경 옵션도 컬럼 이름 변경을 삭제와 추가로 처리하거나, 큰 테이블에 잠금을 거는 변경을 배포 시점에 예고 없이 실행할 수 있다. 무엇이 실행될지 사전에 검토할 수 없다는 점이 핵심 문제다. 운영 스키마는 검토된 변경 스크립트를 버전으로 관리해 순서대로 적용하고, 애플리케이션은 매핑과 스키마가 맞는지 검증만 하도록 둔다.

**필수 개념**

- 자동 생성의 용도와 데이터 손실 위험 · 가중치 0.50: 스키마 자동 생성은 개발·테스트용이며 운영에 적용하면 삭제나 예상 밖 변경으로 데이터가 손실될 수 있다는 설명.
- 검토된 스크립트로 운영 스키마 관리 · 가중치 0.50: 운영 변경은 버전 관리되는 스크립트로 적용하고 애플리케이션은 검증만 하게 둔다는 기준.

**혼동 주의:** 자동 변경 옵션이 컬럼 이름 변경이나 데이터 이전까지 처리해 준다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 9.4 Schema Generation; drop-and-create

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-05 · INTERMEDIATE

엔티티의 이름 필드에 @Column(nullable = false, length = 20)을 선언했습니다. 이름이 null이거나 30자인 엔티티를 저장하려 할 때 이 선언이 값을 검사해 막아 주나요? 이 속성들이 실제로 쓰이는 곳과, 저장 전에 값을 검증하는 수단을 구분해 설명하세요.

**모범 답안**

@Column의 nullable과 length는 컬럼의 정의를 기술하는 매핑 정보다. 스키마를 생성할 때 NOT NULL 제약과 컬럼 길이로 반영될 뿐, 영속성 제공자가 저장 전에 자바 값을 검사하도록 요구하지 않는다. 따라서 잘못된 값은 INSERT가 실행될 때 데이터베이스의 제약 위반으로 드러나고, 스키마를 이 매핑으로 만들지 않았다면 그 제약조차 없을 수 있다. 저장 전에 값을 막으려면 엔티티가 생성자와 변경 메서드에서 규칙을 확인하거나, Bean Validation 제약을 선언해 저장과 수정 시점에 자동 검증되게 한다. 데이터베이스 제약은 이 검증을 우회한 쓰기까지 막는 마지막 방어선으로 함께 둔다.

**필수 개념**

- 컬럼 속성은 스키마 생성용 정보 · 가중치 0.50: nullable·length는 컬럼 정의를 기술해 스키마 생성에 쓰일 뿐 저장 전 자바 값 검사를 뜻하지 않는다는 설명.
- 값 검증과 DB 제약의 역할 구분 · 가중치 0.50: 값은 엔티티 규칙이나 Bean Validation으로 저장 전에 검증하고 DB 제약은 최종 방어로 둔다는 구분.

**혼동 주의:** @Column(nullable = false)를 값 검증 애노테이션과 같은 것으로 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 11.1.9 Column Annotation; 3.7.1 Automatic Validation Upon Lifecycle Events

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-06 · BASIC

회원 엔티티에 city, street, zipCode 필드가 있고 배송지 엔티티에도 같은 세 필드가 있습니다. 이를 Address라는 임베디드 타입으로 묶으면 테이블 구조와 객체 모델이 각각 어떻게 되나요? 임베디드 타입 객체가 엔티티와 달리 갖지 않는 것은 무엇인지 설명하세요.

**모범 답안**

임베디드 타입으로 묶어도 테이블 구조는 그대로다. Address의 필드들은 별도 테이블이 아니라 그것을 소유한 엔티티 테이블의 컬럼으로 매핑된다. 객체 모델에서는 흩어진 세 필드가 하나의 개념으로 묶여, 주소와 관련된 검증이나 비교 로직을 Address에 둘 수 있고 두 엔티티가 같은 타입을 재사용한다. 임베디드 타입 객체는 엔티티와 달리 자신만의 영속 식별자가 없다. 소유한 엔티티의 일부로만 존재하므로 따로 조회하거나 여러 엔티티가 한 인스턴스를 공유하는 대상이 아니다. 같은 인스턴스를 두 엔티티가 참조하면 한쪽의 변경이 다른 쪽에 번지므로 불변으로 만들고 값 전체를 교체하는 방식이 안전하다.

**필수 개념**

- 소유 엔티티 테이블에 매핑 · 가중치 0.50: 임베디드 타입의 필드는 별도 테이블이 아니라 소유 엔티티 테이블의 컬럼으로 매핑된다는 설명.
- 식별자 없는 값으로서의 임베디드 타입 · 가중치 0.50: 임베디드 객체는 영속 식별자가 없고 소유 엔티티의 일부로만 존재해 공유 대상이 아니라는 설명.

**혼동 주의:** 임베디드 타입을 쓰면 테이블이 하나 더 생긴다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 2.7 Embeddable Classes

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-07 · INTERMEDIATE

주문 목록 화면에는 주문 번호와 회원 이름, 총액만 필요합니다. 주문 엔티티를 모두 조회해 화면용 객체로 옮기는 방법과, JPQL의 SELECT 절에서 생성자 표현식으로 필요한 값만 DTO로 받는 방법을 비교하세요. DTO로 받은 결과는 영속성 컨텍스트에서 어떻게 다뤄지나요?

**모범 답안**

엔티티를 조회하면 매핑된 컬럼을 모두 읽고, 결과 엔티티는 영속성 컨텍스트가 관리해 변경 추적 대상이 된다. 화면에 필요 없는 컬럼까지 읽게 되고, 연관을 따라가다 추가 조회가 생길 수 있다. 생성자 표현식은 SELECT NEW 뒤에 클래스 이름과 인자를 적어 조회 결과 한 행마다 그 생성자로 객체를 만든다. 필요한 값만 SELECT 절에 오므로 읽는 데이터가 줄어든다. 엔티티가 아닌 클래스로 만든 이 객체는 영속성 컨텍스트가 관리하지 않으며, 값을 바꿔도 데이터베이스에 반영되지 않는다. 그래서 수정이 목적일 때는 엔티티로 조회하고, 조회 전용 화면에는 DTO 조회를 쓰는 식으로 나눈다.

**필수 개념**

- 필요한 값만 읽는 생성자 표현식 · 가중치 0.50: SELECT NEW로 필요한 값만 조회해 행마다 지정한 생성자로 객체를 만든다는 설명.
- 관리되지 않는 조회 결과 · 가중치 0.50: 엔티티가 아닌 클래스로 받은 결과는 영속성 컨텍스트가 관리하지 않아 변경이 반영되지 않는다는 설명.

**혼동 주의:** DTO 조회 결과를 수정하면 변경 감지가 동작한다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 4.9.2 Constructor Expressions in the SELECT Clause

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-08 · INTERMEDIATE

상품 엔티티 세 개를 조회해 영속성 컨텍스트에 올려 둔 상태에서, JPQL UPDATE 문으로 모든 상품의 가격을 10% 올렸습니다. 그 뒤 이미 조회해 둔 엔티티의 가격을 읽으면 어떤 값이 보이나요? 이런 어긋남이 생기는 이유와 피하는 방법을 설명하세요.

**모범 답안**

이미 조회해 둔 엔티티에는 인상 전 가격이 그대로 보인다. 벌크 UPDATE와 DELETE는 영속성 컨텍스트를 거치지 않고 데이터베이스에 직접 실행되며, 영속성 컨텍스트에 있는 엔티티의 상태를 맞춰 주지 않는다. 그래서 메모리의 엔티티와 데이터베이스의 행이 어긋나고, 같은 식별자로 다시 find를 호출해도 영속성 컨텍스트에 있던 예전 객체가 반환된다. 이를 피하려면 벌크 연산을 엔티티를 조회하기 전에 먼저 실행하거나, 실행 직후 영속성 컨텍스트를 비우고 필요한 엔티티를 다시 조회한다. 이미 올라온 엔티티만 새로 읽으려면 refresh를 쓸 수도 있다.

**필수 개념**

- 영속성 컨텍스트를 우회하는 벌크 연산 · 가중치 0.50: 벌크 UPDATE·DELETE는 DB에 직접 실행되어 이미 관리 중인 엔티티의 상태를 갱신하지 않는다는 설명.
- 벌크 연산 뒤의 상태 맞추기 · 가중치 0.50: 벌크 연산을 조회 전에 실행하거나 실행 후 영속성 컨텍스트를 비우고 다시 조회한다는 대응.

**혼동 주의:** 벌크 연산 뒤에도 조회해 둔 엔티티가 자동으로 최신 값이 된다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 4.11 Bulk Update and Delete Operations

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-09 · INTERMEDIATE

주문 100건을 조회한 뒤 각 주문의 회원 이름을 출력했더니 회원을 조회하는 SELECT가 주문마다 따로 실행됐습니다. 이 현상의 원인을 지연 로딩으로 설명하고, JPQL의 fetch join이 일반 join과 어떻게 달라 이 문제를 줄이는지 답하세요.

**모범 답안**

주문의 회원 연관이 지연 로딩이면 주문 조회 쿼리는 주문만 가져오고, 회원은 처음 접근하는 순간에 조회된다. 주문마다 회원에 접근하면 주문 조회 한 번에 더해 회원 조회가 주문 수만큼 실행될 수 있다. 일반 join은 조인한 연관을 조건이나 정렬에 쓸 뿐, 결과로 가져오는 것은 주문 엔티티뿐이라 연관은 여전히 로딩되지 않은 상태로 남는다. fetch join은 주문을 조회하는 같은 쿼리에서 연관된 회원까지 함께 읽어 주문 객체의 연관을 채워 둔다. 그래서 이후 회원에 접근해도 추가 조회가 생기지 않는다. 연관을 즉시 로딩으로 바꾸면 필요 없는 화면에서도 항상 읽게 되므로 해결책이 되지 못한다.

**필수 개념**

- 지연 로딩과 반복 조회 · 가중치 0.50: 지연 로딩 연관은 접근 시점에 조회되어 목록의 각 원소마다 추가 SELECT가 실행될 수 있다는 설명.
- 연관을 함께 읽는 fetch join · 가중치 0.50: 일반 join은 연관을 로딩하지 않지만 fetch join은 같은 쿼리로 연관 엔티티를 읽어 채운다는 차이.

**혼동 주의:** 즉시 로딩으로 바꾸면 반복 조회 문제가 해결된다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 4.4.5.3 Fetch Joins; results in eager fetching of an association

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-10 · INTERMEDIATE

회원과 팀이 양방향 다대일 관계입니다. team.getMembers().add(member)만 호출하고 member.setTeam(team)은 호출하지 않은 채 커밋했더니, 회원 테이블의 team_id가 null로 저장됐습니다. 양방향 관계의 소유 측 개념으로 원인을 설명하고 올바른 처리 방법을 답하세요.

**모범 답안**

양방향 관계에는 소유 측과 그 반대 측이 있고, 데이터베이스에 반영되는 것은 소유 측의 상태뿐이다. 다대일 양방향 관계에서는 외래 키 컬럼이 있는 회원 쪽의 team 필드가 소유 측이다. mappedBy로 선언된 팀의 members 컬렉션은 반대 측이어서 여기에 회원을 추가해도 외래 키는 바뀌지 않는다. 따라서 member.setTeam(team)을 호출해야 team_id가 저장된다. 객체 그래프도 일관되게 유지하려면 양쪽을 함께 설정해야 하므로, 한 메서드 안에서 회원의 팀을 바꾸고 팀의 컬렉션도 갱신하는 편의 메서드를 엔티티에 둔다.

**필수 개념**

- 소유 측만 DB에 반영 · 가중치 0.50: 양방향 관계에서 소유 측의 상태만 데이터베이스 갱신을 결정하고 mappedBy 쪽 변경은 반영되지 않는다는 설명.
- 양쪽을 함께 맞추는 편의 메서드 · 가중치 0.50: 소유 측을 반드시 설정하고 객체 그래프 일관성을 위해 양쪽을 한 메서드에서 함께 갱신한다는 처리.

**혼동 주의:** mappedBy가 붙은 쪽을 소유 측으로 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 2.11 Entity Relationships; The owning side of a relationship determines the updates

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-JPA-11 · ADVANCED

주문과 주문 항목이 일대다 관계입니다. 주문 목록을 주문 항목 컬렉션과 fetch join으로 한 번에 읽으면서 setFirstResult와 setMaxResults로 10건씩 페이징하려 합니다. 이 조합이 기대대로 동작한다고 보장할 수 없는 이유를 조인 결과의 행 수로 설명하고 대안을 제시하세요.

**모범 답안**

일대다 컬렉션을 조인하면 결과 행은 주문 수가 아니라 주문 항목 수만큼 나온다. 항목이 세 개인 주문은 세 행이 된다. 데이터베이스 수준의 행 제한은 이 늘어난 행을 기준으로 자르므로 10행이 주문 10건을 뜻하지 않고, 한 주문의 항목이 페이지 경계에서 잘릴 수도 있다. 명세는 컬렉션 fetch join이 포함된 쿼리에 결과 범위 지정을 적용했을 때의 효과를 정의하지 않으며, 구현에 따라 전체를 읽어 메모리에서 자르기도 한다. 대안은 페이징 대상인 주문만 먼저 범위를 지정해 조회하고, 그 주문들의 항목은 식별자 목록을 조건으로 한 별도 쿼리로 읽어 묶는 것이다. 다대일 연관은 행 수를 늘리지 않으므로 fetch join과 페이징을 함께 써도 된다.

**필수 개념**

- 컬렉션 조인으로 늘어나는 행 · 가중치 0.50: 일대다 컬렉션을 조인하면 행이 자식 수만큼 늘어 행 기준 제한이 부모 건수와 맞지 않는다는 설명.
- 부모만 페이징 후 자식 별도 조회 · 가중치 0.50: 부모 엔티티만 범위를 지정해 조회하고 컬렉션은 식별자 조건의 별도 쿼리로 읽는다는 대안.

**혼동 주의:** 컬렉션 fetch join과 페이징을 함께 써도 DB에서 정확히 잘린다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 4.4.5.3 Fetch Joins; fetch joins over collections is undefined

근거 문서: `r1-jpa`. 검수 상태: **PENDING**

### R1-DB-01 · BASIC

한 주문에 여러 상품이 담기고 한 상품은 여러 주문에 담깁니다. orders 테이블에 product_id 컬럼 하나를 두는 설계로는 왜 이 관계를 표현할 수 없나요? 연결 테이블의 구성과, 주문 수량 같은 값을 어디에 두는지 설명하세요.

**모범 답안**

orders에 product_id 컬럼 하나를 두면 한 주문 행이 상품 하나만 가리킬 수 있다. 상품마다 주문 행을 반복하면 주문 자체의 정보가 중복되고, 컬럼을 여러 개 두면 담을 수 있는 개수가 고정된다. 다대다 관계는 두 테이블을 각각 참조하는 외래 키 두 개를 가진 연결 테이블로 표현한다. order_items에 order_id와 product_id를 두고 두 컬럼 조합을 기본키로 삼으면 같은 주문에 같은 상품이 두 번 들어가지 않는다. 수량처럼 주문과 상품의 조합에 딸린 값은 어느 한쪽 테이블이 아니라 이 연결 테이블의 컬럼이 된다. 결과적으로 다대다는 일대다 관계 두 개로 풀린다.

**필수 개념**

- 외래 키 두 개를 가진 연결 테이블 · 가중치 0.50: 다대다 관계는 양쪽을 참조하는 외래 키 두 개의 연결 테이블로 풀고 조합에 유일성을 건다는 설명.
- 조합에 딸린 속성의 위치 · 가중치 0.50: 수량처럼 두 엔티티의 조합에 속하는 값은 연결 테이블의 컬럼이 된다는 설명.

**혼동 주의:** 연결 테이블을 ‘조회 성능을 위한 보조 테이블’로 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 5.5 Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — 5.5.5 Foreign Keys; many-to-many relationships between tables

근거 문서: `r1-database`. 검수 상태: **PENDING**

### R1-DB-02 · INTERMEDIATE

주문 상태 컬럼에 허용되는 값이 PENDING, PAID, CANCELLED 세 가지입니다. 이 제한을 CHECK 제약으로 거는 방법과, 상태 코드 테이블을 만들고 외래 키로 참조하는 방법이 있습니다. 허용 값이 바뀔 때 각 방법에서 해야 하는 일과 선택 기준을 설명하세요.

**모범 답안**

CHECK 제약은 컬럼 값이 정해진 조건을 만족하는지 행마다 검사하므로, 세 값 중 하나만 허용하도록 테이블 정의에 적을 수 있다. 값 목록이 스키마의 일부여서 값을 추가하려면 제약을 바꾸는 DDL을 실행해야 한다. 코드 테이블 방식은 허용 값을 행으로 저장하고 외래 키가 그 행의 존재를 보장한다. 값 추가는 INSERT 한 번이면 되고, 표시 이름이나 정렬 순서 같은 부가 정보도 함께 둘 수 있다. 값이 거의 바뀌지 않고 애플리케이션 코드의 분기와 묶여 있다면 CHECK가 단순하다. 운영 중에 값이 늘거나 값마다 속성이 필요하면 코드 테이블이 맞다. 어느 쪽이든 데이터베이스가 잘못된 값을 거부하게 한다는 점은 같다.

**필수 개념**

- CHECK와 참조 테이블의 변경 비용 · 가중치 0.50: CHECK는 허용 값이 스키마에 있어 DDL로 바꾸고 코드 테이블은 행 추가로 바꾼다는 차이.
- 값의 변동성과 부가 속성에 따른 선택 · 가중치 0.50: 값이 고정적이면 CHECK, 운영 중 늘거나 값마다 속성이 필요하면 코드 테이블이라는 기준.

**혼동 주의:** 허용 값 검사를 애플리케이션 enum에만 맡겨도 충분하다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 5.5 Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) — 5.5.1 Check Constraints; 5.5.5 Foreign Keys

근거 문서: `r1-database`. 검수 상태: **PENDING**

### R1-DB-03 · INTERMEDIATE

회원 탈퇴를 행 삭제 대신 deleted_at 컬럼에 시각을 기록하는 방식으로 처리합니다. email 컬럼에는 UNIQUE 제약이 있습니다. 탈퇴한 회원의 이메일로 다시 가입하려 할 때 생기는 문제와, 조회 쿼리에서 추가로 신경 써야 할 점을 설명하세요. PostgreSQL 17에서의 해결 방법도 답하세요.

**모범 답안**

삭제 표시된 행도 테이블에 남아 있으므로 UNIQUE 제약은 탈퇴한 회원의 이메일까지 유일성 검사에 포함한다. 그래서 같은 이메일로 다시 가입하면 제약 위반이 난다. PostgreSQL에서는 deleted_at이 NULL인 행만 대상으로 하는 부분 유일 인덱스를 만들어, 활성 회원 사이에서만 이메일이 유일하도록 할 수 있다. 조회에서는 모든 쿼리가 삭제되지 않은 행만 보도록 조건을 넣어야 하며, 한 곳이라도 빠지면 탈퇴한 회원이 결과에 섞인다. 이 행을 참조하는 다른 테이블의 외래 키는 그대로 유효하므로, 참조하는 쪽에서 삭제된 대상을 어떻게 보여 줄지도 정해야 한다. 지워야 할 의무가 있는 개인정보는 삭제 표시만으로는 요건을 채우지 못한다는 점도 확인한다.

**필수 개념**

- 삭제 표시와 유일성 제약의 충돌 · 가중치 0.50: 삭제 표시된 행도 UNIQUE 검사에 포함되어 재가입이 막히며 활성 행만 대상으로 하는 부분 유일 인덱스로 푼다는 설명.
- 모든 조회에 필요한 삭제 조건 · 가중치 0.50: 삭제되지 않은 행만 보는 조건이 모든 조회에 필요하고 참조하는 쪽의 표시 정책도 정해야 한다는 설명.

**혼동 주의:** 삭제 표시 방식을 ‘복구가 쉬워 항상 더 안전한 방법’으로 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 11.8 Partial Indexes](https://www.postgresql.org/docs/17/indexes-partial.html) — 11.8 Partial Indexes; Setting up a Partial Unique Index

근거 문서: `r1-database`. 검수 상태: **PENDING**

### R1-DB-04 · INTERMEDIATE

요청마다 데이터베이스 커넥션을 새로 만들던 서비스에 커넥션 풀을 도입했습니다. 응답이 느려지자 풀 크기를 10에서 500으로 늘렸더니 오히려 처리량이 떨어졌습니다. 커넥션 풀이 줄여 주는 비용이 무엇인지, 풀을 크게 잡는 것이 해결책이 되지 못하는 이유를 설명하세요.

**모범 답안**

커넥션을 새로 만들려면 네트워크 연결과 인증, 세션 준비를 매번 거쳐야 한다. 풀은 만들어 둔 커넥션을 빌려주고 돌려받아 이 비용을 없애고, 동시에 열리는 커넥션 수의 상한도 정한다. 데이터베이스가 동시에 실제로 처리할 수 있는 쿼리 수는 CPU 코어와 디스크 같은 자원으로 제한된다. 그보다 많은 커넥션이 동시에 일하면 문맥 전환과 자원 경합이 늘어 전체 처리량이 떨어진다. 그래서 풀은 작게 두고, 남는 요청 스레드는 풀 앞에서 커넥션을 기다리게 하는 편이 낫다. 응답이 느릴 때는 풀 크기보다 커넥션을 오래 쥐고 있는 느린 쿼리나 긴 트랜잭션을 먼저 찾는다.

**필수 개념**

- 커넥션 재사용과 동시 접속 상한 · 가중치 0.50: 풀은 커넥션 생성 비용을 없애고 동시에 열리는 커넥션 수를 제한한다는 설명.
- DB 처리 능력에 맞춘 작은 풀 · 가중치 0.50: DB가 동시에 처리할 수 있는 수를 넘는 커넥션은 경합으로 처리량을 떨어뜨리므로 풀을 작게 두고 대기시킨다는 설명.

**혼동 주의:** 풀 크기를 동시 사용자 수에 맞춰야 한다고 설명하지 않기.

**출처 대조 위치**

- [HikariCP Wiki — About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing) — About Pool Sizing: You want a small pool, saturated with threads waiting for connections

근거 문서: `r1-database`. 검수 상태: **PENDING**

### R1-DB-05 · INTERMEDIATE

데이터베이스 서버 한 대에 읽기와 쓰기가 모두 몰려 있고, 이 서버가 멈추면 서비스 전체가 멈춥니다. 대기 서버를 두고 변경 로그를 계속 전달해 복제하려 합니다. 이 구성이 줄여 주는 위험과, 비동기 복제에서 대기 서버로 읽기를 보낼 때 생기는 현상을 설명하세요.

**모범 답안**

한 대뿐인 서버는 단일 장애점이다. 대기 서버가 주 서버의 변경 로그를 계속 받아 반영하고 있으면, 주 서버가 멈췄을 때 대기 서버를 새 주 서버로 승격해 짧은 중단으로 서비스를 이어 갈 수 있다. 대기 서버가 읽기 쿼리를 받을 수 있게 구성하면 조회 부하도 나눌 수 있다. 다만 비동기 복제에서는 주 서버가 커밋을 확정한 뒤에 로그가 전달되므로 대기 서버가 조금 뒤처진다. 방금 쓴 데이터를 곧바로 대기 서버에서 읽으면 아직 보이지 않을 수 있고, 주 서버가 갑자기 멈추면 전달되지 않은 마지막 변경이 유실될 수 있다. 동기 복제는 이 유실을 막는 대신 커밋이 대기 서버의 확인을 기다리므로 쓰기 지연이 늘어난다. 쓰기는 여전히 주 서버 한 곳으로만 가므로 쓰기 처리량은 늘지 않는다.

**필수 개념**

- 대기 서버로 줄이는 단일 장애점 · 가중치 0.50: 변경 로그를 받아 반영하는 대기 서버가 있으면 장애 시 승격해 이어 갈 수 있고 읽기 부하도 나눈다는 설명.
- 비동기 복제의 지연과 유실 가능성 · 가중치 0.50: 비동기 복제에서는 대기 서버가 뒤처져 방금 쓴 값이 안 보일 수 있고 장애 시 마지막 변경이 유실될 수 있다는 설명.

**혼동 주의:** 복제를 구성하면 쓰기 처리량도 함께 늘어난다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 26.1 Comparison of Different Solutions](https://www.postgresql.org/docs/17/different-replication-solutions.html) — 26.1 Comparison of Different Solutions; Write-Ahead Log Shipping

근거 문서: `r1-database`. 검수 상태: **PENDING**

### R1-TX-01 · BASIC

계좌 A에서 10만 원을 빼는 UPDATE와 계좌 B에 10만 원을 더하는 UPDATE를 차례로 실행합니다. 첫 문장만 실행된 뒤 서버가 멈췄습니다. 두 문장을 하나의 트랜잭션으로 묶었을 때와 묶지 않았을 때의 결과를 비교하고, 진행 중인 변경이 다른 트랜잭션에 어떻게 보이는지 설명하세요.

**모범 답안**

묶지 않으면 각 문장이 따로 확정된다. 첫 UPDATE만 반영된 채 멈추면 A에서 빠진 돈이 B에 들어가지 않은 상태가 그대로 남는다. BEGIN과 COMMIT으로 묶으면 두 문장은 전부 반영되거나 전혀 반영되지 않는 하나의 작업이 된다. 커밋 전에 장애가 나면 첫 UPDATE도 없던 일이 된다. 또 커밋되기 전의 변경은 다른 트랜잭션에 보이지 않는다. 그래서 다른 트랜잭션이 전체 잔액을 합산하더라도 A에서는 빠지고 B에는 아직 더해지지 않은 중간 상태를 보지 않는다. 커밋이 완료됐다고 응답한 뒤에는 그 직후 장애가 나더라도 변경이 보존된다.

**필수 개념**

- 전부 반영 또는 전부 취소 · 가중치 0.50: 트랜잭션으로 묶인 문장들은 모두 반영되거나 모두 취소되어 중간 상태가 남지 않는다는 설명.
- 커밋 전 변경의 비가시성 · 가중치 0.50: 커밋되지 않은 변경은 다른 트랜잭션에 보이지 않아 중간 상태가 관찰되지 않는다는 설명.

**혼동 주의:** 트랜잭션을 ‘여러 SQL을 한 번에 보내는 기능’으로 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 3.4 Transactions](https://www.postgresql.org/docs/17/tutorial-transactions.html) — 3.4 Transactions; all-or-nothing operation

근거 문서: `r1-transactions-and-concurrency`. 검수 상태: **PENDING**

### R1-TX-02 · INTERMEDIATE

트랜잭션 1은 계좌 A 행을 수정한 뒤 계좌 B 행을 수정하려 하고, 같은 시각 트랜잭션 2는 B를 수정한 뒤 A를 수정하려 합니다. 두 트랜잭션이 어떤 상태에 빠지는지 설명하세요. PostgreSQL 17이 이 상황을 어떻게 끝내는지와, 애플리케이션이 미리 피하는 방법도 답하세요.

**모범 답안**

트랜잭션 1은 A의 행 잠금을 쥔 채 B의 잠금을 기다리고, 트랜잭션 2는 B의 잠금을 쥔 채 A의 잠금을 기다린다. 서로 상대가 쥔 잠금이 풀리기를 기다리므로 어느 쪽도 진행할 수 없는 교착 상태가 된다. PostgreSQL은 교착 상태를 자동으로 감지해 관련 트랜잭션 가운데 하나를 중단시키고 나머지가 진행하게 한다. 어느 쪽이 중단될지는 예측할 수 없으므로 애플리케이션은 중단된 트랜잭션을 처음부터 다시 시도할 수 있어야 한다. 예방의 기본은 모든 코드가 여러 대상의 잠금을 같은 순서로 얻는 것이다. 두 트랜잭션이 모두 계좌 번호가 작은 행부터 수정했다면 교착은 생기지 않는다.

**필수 개념**

- 서로의 잠금을 기다리는 순환 대기 · 가중치 0.50: 각 트랜잭션이 잠금을 쥔 채 상대가 쥔 잠금을 기다려 어느 쪽도 진행하지 못한다는 설명.
- 감지 후 중단과 일관된 잠금 순서 · 가중치 0.50: DB가 한 트랜잭션을 중단시켜 풀며 애플리케이션은 재시도와 일관된 잠금 순서로 대비한다는 설명.

**혼동 주의:** 교착 상태를 ‘잠금 대기가 오래 걸리는 상태’와 같은 것으로 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 13.3 Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html) — 13.3.4 Deadlocks

근거 문서: `r1-transactions-and-concurrency`. 검수 상태: **PENDING**

### R1-TX-03 · INTERMEDIATE

한 트랜잭션이 주문 테이블의 한 행을 수정하고 아직 커밋하지 않았습니다. 그 사이 다른 트랜잭션이 같은 행을 일반 SELECT로 읽습니다. PostgreSQL 17에서 이 SELECT는 수정이 끝날 때까지 기다리나요? 읽는 쪽이 보는 값과, 같은 행을 수정하려는 세 번째 트랜잭션의 동작을 구분해 설명하세요.

**모범 답안**

기다리지 않는다. PostgreSQL은 다중 버전 동시성 제어를 사용해, 각 SQL 문이 실행 시점보다 얼마 전의 데이터 스냅샷을 본다. 읽기를 위한 잠금과 쓰기를 위한 잠금이 충돌하지 않으므로 읽기는 쓰기를 막지 않고 쓰기도 읽기를 막지 않는다. 일반 SELECT는 아직 커밋되지 않은 수정을 보지 않고, 수정 전의 커밋된 값을 즉시 읽는다. 반면 같은 행을 수정하려는 세 번째 트랜잭션은 쓰기끼리의 충돌이므로, 먼저 수정한 트랜잭션이 커밋하거나 롤백할 때까지 기다린다. 읽은 값을 근거로 이어서 수정할 계획이라면, 일반 SELECT로 읽은 값이 그사이 바뀔 수 있다는 점을 고려해야 한다.

**필수 개념**

- 읽기와 쓰기가 서로 막지 않는 MVCC · 가중치 0.50: 각 문장이 스냅샷을 보므로 읽기는 미커밋 수정을 기다리지 않고 커밋된 이전 값을 읽는다는 설명.
- 쓰기끼리는 대기 · 가중치 0.50: 같은 행을 수정하려는 트랜잭션은 앞선 수정 트랜잭션이 끝날 때까지 기다린다는 구분.

**혼동 주의:** SELECT가 다른 트랜잭션의 미커밋 수정을 기다린다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 13.1 Introduction (Concurrency Control)](https://www.postgresql.org/docs/17/mvcc-intro.html) — 13.1 Introduction; reading never blocks writing and writing never blocks reading

근거 문서: `r1-transactions-and-concurrency`. 검수 상태: **PENDING**

### R1-TX-04 · INTERMEDIATE

주문을 저장하는 트랜잭션 안에서 쿠폰 사용 기록도 함께 남깁니다. 쿠폰 기록에서 오류가 나더라도 주문 저장은 유지하고 싶습니다. 오류가 난 문장 이후에도 같은 트랜잭션을 이어 가려면 SAVEPOINT를 어떻게 써야 하나요? ROLLBACK TO와 전체 ROLLBACK의 차이를 설명하세요.

**모범 답안**

쿠폰 기록을 실행하기 직전에 SAVEPOINT를 정의한다. 쿠폰 기록에서 오류가 나면 ROLLBACK TO로 그 저장점까지만 되돌린다. 저장점 이후의 변경은 버려지고 그 이전에 실행한 주문 저장은 유지되며, 트랜잭션은 계속 열려 있어 이후 문장을 실행하고 커밋할 수 있다. PostgreSQL에서는 트랜잭션 안의 문장이 오류를 내면 트랜잭션 전체가 중단 상태가 되어 이후 명령이 거부되는데, 저장점으로 되돌리는 것이 전체를 버리지 않고 이 상태에서 벗어나는 방법이다. 전체 ROLLBACK은 트랜잭션을 끝내고 시작 이후의 모든 변경을 취소한다. 저장점까지 되돌린 뒤에도 최종 커밋이 되어야 남은 변경이 확정된다.

**필수 개념**

- 저장점까지의 부분 롤백 · 가중치 0.50: ROLLBACK TO는 저장점 이후 변경만 버리고 이전 변경과 트랜잭션은 유지한다는 설명.
- 오류 뒤 트랜잭션을 이어 가는 수단 · 가중치 0.50: 오류로 중단 상태가 된 트랜잭션은 저장점으로 되돌려야 이어 갈 수 있고 전체 ROLLBACK은 모두 취소한다는 구분.

**혼동 주의:** ROLLBACK TO가 트랜잭션을 끝낸다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 3.4 Transactions](https://www.postgresql.org/docs/17/tutorial-transactions.html) — 3.4 Transactions; selectively discard parts of the transaction; regain control of a transaction block

근거 문서: `r1-transactions-and-concurrency`. 검수 상태: **PENDING**

### R1-TX-05 · INTERMEDIATE

쿠폰 행을 SELECT로 읽어 남은 수량을 확인한 뒤 같은 트랜잭션에서 그 행을 수정합니다. 읽을 때 FOR SHARE를 붙인 두 트랜잭션이 동시에 실행되면 어떤 일이 생길 수 있나요? FOR SHARE와 FOR UPDATE가 허용하는 동시 접근의 차이로 설명하고 올바른 잠금 선택을 답하세요.

**모범 답안**

FOR SHARE는 읽은 행에 공유 잠금을 건다. 다른 트랜잭션도 같은 행에 공유 잠금을 걸 수 있지만, 공유 잠금이 걸린 행을 수정하거나 지우려는 시도는 막힌다. 두 트랜잭션이 모두 FOR SHARE로 같은 쿠폰 행을 읽으면 둘 다 잠금을 얻는다. 이어서 각자 수정하려 하면 상대의 공유 잠금 때문에 서로 기다리게 되어 교착 상태가 되고, 한쪽이 중단된다. FOR UPDATE는 배타 잠금이어서 한 트랜잭션만 얻을 수 있고 나머지는 읽는 단계에서부터 기다린다. 따라서 읽은 뒤 수정할 행은 처음부터 필요한 가장 강한 잠금인 FOR UPDATE로 읽는다. 일반 SELECT는 두 잠금 모두에 막히지 않는다.

**필수 개념**

- 공유 잠금과 배타 잠금의 허용 범위 · 가중치 0.50: FOR SHARE는 여러 트랜잭션이 함께 얻지만 수정은 막고, FOR UPDATE는 한 트랜잭션만 얻는다는 차이.
- 처음부터 가장 강한 잠금 얻기 · 가중치 0.50: 공유 잠금 뒤 수정으로 올리면 서로 기다려 교착이 되므로 수정할 행은 처음부터 FOR UPDATE로 읽는다는 설명.

**혼동 주의:** FOR UPDATE가 일반 SELECT까지 막는다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 13.3 Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html) — 13.3.2 Row-Level Locks; the most restrictive mode

근거 문서: `r1-transactions-and-concurrency`. 검수 상태: **PENDING**

### R1-SQL-01 · BASIC

회원이 세 명이고 주문은 A 회원에게 두 건, B 회원에게 한 건 있으며 C 회원에게는 없습니다. 회원과 주문을 회원 ID로 INNER JOIN한 결과와 LEFT OUTER JOIN한 결과의 행 수는 각각 몇인가요? C 회원이 어떻게 나타나는지와 함께 두 조인의 정의를 설명하세요.

**모범 답안**

INNER JOIN은 조인 조건을 만족하는 짝마다 한 행을 만든다. A는 주문 두 건과 각각 짝을 이뤄 두 행, B는 한 행이 되고 짝이 없는 C는 결과에서 빠져 모두 세 행이다. LEFT OUTER JOIN은 먼저 내부 조인을 수행한 뒤, 오른쪽에 짝이 없는 왼쪽 행마다 오른쪽 컬럼을 NULL로 채운 행을 추가한다. 그래서 C가 주문 컬럼이 NULL인 한 행으로 나타나 모두 네 행이 된다. 왼쪽 테이블의 모든 행이 적어도 한 번은 결과에 남는다는 점이 차이다. 두 조인 모두 짝이 여러 개면 왼쪽 행이 그만큼 반복되므로, 조인 결과의 행 수를 회원 수로 읽어서는 안 된다.

**필수 개념**

- 짝이 없는 행의 처리 차이 · 가중치 0.50: INNER JOIN은 짝이 없는 행을 버리고 LEFT OUTER JOIN은 오른쪽을 NULL로 채워 남긴다는 정의.
- 짝의 수만큼 늘어나는 결과 행 · 가중치 0.50: 한 행에 짝이 여러 개면 그만큼 반복되어 조인 결과 행 수가 원래 행 수와 다르다는 설명.

**혼동 주의:** LEFT JOIN의 결과 행 수가 항상 왼쪽 테이블의 행 수와 같다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 7.2 Table Expressions](https://www.postgresql.org/docs/17/queries-table-expressions.html) — 7.2.1.1 Joined Tables; the joined table always has at least one row for each row in T1

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-02 · BASIC

회원 100만 명이 있는 테이블에서 이메일로 한 명을 찾는 쿼리가 느립니다. 이메일 컬럼에 인덱스를 만들면 검색 방식이 어떻게 달라지나요? 그렇다면 모든 컬럼에 인덱스를 만들어 두는 것이 좋은지, 인덱스가 만드는 비용과 함께 설명하세요.

**모범 답안**

인덱스가 없으면 조건에 맞는 행을 찾기 위해 테이블의 모든 행을 차례로 읽어야 한다. 인덱스는 컬럼 값을 정렬된 구조로 따로 유지해 원하는 값의 위치를 몇 단계 만에 찾게 하므로, 한 명을 찾는 쿼리는 전체 행 수와 거의 무관하게 빨라진다. 그러나 인덱스는 테이블과 항상 일치해야 하므로 행을 추가하거나 수정하거나 삭제할 때마다 관련 인덱스도 함께 갱신된다. 인덱스가 많을수록 쓰기가 느려지고 저장 공간도 더 쓴다. 또 조건에 맞는 행이 테이블의 큰 비율을 차지하면 인덱스를 거치는 것보다 전체를 읽는 편이 빨라 인덱스가 쓰이지 않는다. 그래서 자주 쓰이는 검색과 조인, 정렬 조건에 맞춰 필요한 것만 만들고 쓰이지 않는 인덱스는 지운다.

**필수 개념**

- 전체 탐색을 대신하는 인덱스 검색 · 가중치 0.50: 인덱스가 없으면 모든 행을 읽지만 인덱스는 정렬된 구조로 대상 행의 위치를 빠르게 찾는다는 설명.
- 쓰기 부담과 선택적 생성 · 가중치 0.50: 인덱스는 변경마다 함께 갱신되어 쓰기와 공간 비용이 들므로 필요한 것만 만든다는 설명.

**혼동 주의:** 인덱스가 있으면 조회가 항상 빨라진다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 11.1 Introduction (Indexes)](https://www.postgresql.org/docs/17/indexes-intro.html) — 11.1 Introduction; This adds overhead to data manipulation operations

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-03 · INTERMEDIATE

주문 테이블에서 status 컬럼은 값이 세 종류뿐이고 전체의 90%가 COMPLETED입니다. customer_id 컬럼은 고객마다 값이 다릅니다. WHERE status = 'COMPLETED' 조건과 WHERE customer_id = 42 조건 중 어느 쪽이 인덱스로 이득을 보나요? 플래너가 이를 판단하는 근거도 설명하세요.

**모범 답안**

customer_id = 42 조건은 전체 행 가운데 아주 적은 행만 고르므로 인덱스로 위치를 찾아 그 행만 읽는 것이 훨씬 싸다. status = 'COMPLETED' 조건은 전체의 90%를 고른다. 인덱스로 찾은 행을 하나씩 테이블에서 따로 읽는 비용이 전체를 순서대로 읽는 비용보다 커지므로 플래너는 인덱스가 있어도 순차 탐색을 고른다. 다만 같은 컬럼이라도 드문 값인 CANCELLED를 찾는 조건은 인덱스가 유리할 수 있다. 플래너는 컬럼별로 수집한 통계에서 고유 값의 수와 자주 나오는 값의 빈도를 보고 조건이 고를 행 수를 추정해 판단한다. 따라서 인덱스 대상은 고유 값이 많은지만 볼 것이 아니라 실제 쿼리 조건이 얼마나 적은 행을 고르는지로 정한다.

**필수 개념**

- 조건이 고르는 행의 비율 · 가중치 0.50: 조건이 전체의 적은 비율만 고를 때 인덱스가 유리하고 대부분을 고르면 순차 탐색이 싸다는 설명.
- 통계에 기반한 플래너의 추정 · 가중치 0.50: 플래너가 고유 값 수와 자주 나오는 값의 빈도 통계로 대상 행 수를 추정해 접근 방법을 고른다는 설명.

**혼동 주의:** 고유 값이 적은 컬럼에는 인덱스가 절대 쓸모없다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 14.2 Statistics Used by the Planner](https://www.postgresql.org/docs/17/planner-stats.html) — 14.2.1 Single-Column Statistics; most_common_vals

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-04 · INTERMEDIATE

해시 테이블은 값 하나를 찾는 평균 비용이 O(1)이고 B-tree는 O(log n)입니다. 그런데도 데이터베이스의 기본 인덱스는 B-tree입니다. created_at >= :from 같은 범위 조건과 ORDER BY에서 두 구조가 할 수 있는 일을 비교해 그 이유를 설명하세요.

**모범 답안**

해시 인덱스는 컬럼 값의 해시 코드를 저장하므로 값이 정확히 같은지 비교하는 조건에만 쓸 수 있다. 해시 값은 원래 값의 크기 순서를 보존하지 않아, 어떤 값 이상이라는 범위 조건이나 정렬에는 도움이 되지 않는다. B-tree는 값을 정렬된 순서로 유지한다. 동등 조건은 물론 크다, 작다, 사이 같은 범위 조건을 시작 위치를 찾은 뒤 이웃한 항목을 차례로 읽어 처리하고, 정렬된 순서 그대로 꺼낼 수 있어 ORDER BY에도 쓰인다. 실제 쿼리에는 범위 조건과 정렬이 흔하므로 한 구조로 여러 조건을 처리하는 B-tree가 기본이 된다. 값 하나를 찾는 비용의 차이는 트리 높이가 낮아 실제로는 크지 않다.

**필수 개념**

- 해시 인덱스의 동등 비교 한정 · 가중치 0.50: 해시 인덱스는 해시 코드만 저장해 동등 비교에만 쓰이고 범위 조건과 정렬을 지원하지 못한다는 설명.
- 정렬 순서를 유지하는 B-tree · 가중치 0.50: B-tree는 값을 정렬해 유지하므로 동등·범위 조건과 정렬된 순서의 조회를 모두 처리한다는 설명.

**혼동 주의:** B-tree가 기본인 이유를 ‘해시 충돌이 많아서’로만 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 11.2 Index Types](https://www.postgresql.org/docs/17/indexes-types.html) — 11.2.1 B-Tree; 11.2.2 Hash

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-05 · INTERMEDIATE

도시별 평균 기온을 구하되, 2020년 이후 관측만 계산에 넣고 평균이 20도 이상인 도시만 남기려 합니다. 두 조건을 각각 WHERE와 HAVING 중 어디에 써야 하나요? 두 절이 적용되는 시점의 차이로 설명하고, WHERE에 AVG를 쓸 수 없는 이유도 답하세요.

**모범 답안**

관측 연도 조건은 WHERE에, 평균 조건은 HAVING에 쓴다. WHERE는 그룹과 집계가 계산되기 전에 입력 행을 고르므로 어떤 행이 집계에 들어갈지를 정한다. 2020년 이전 관측을 평균 계산에서 빼려면 여기서 걸러야 한다. HAVING은 그룹과 집계가 계산된 뒤에 그룹을 고르므로 평균이 20도 이상이라는 조건은 여기에만 쓸 수 있다. WHERE가 적용되는 시점에는 아직 집계 결과가 없고, 집계에 넣을 행을 정하는 데 집계 결과를 쓴다는 것 자체가 성립하지 않아 WHERE에는 집계 함수를 쓸 수 없다. 집계가 필요 없는 조건을 HAVING에 써도 결과는 같을 수 있지만, 버릴 행까지 그룹으로 묶어 계산하게 되므로 WHERE에 두는 편이 효율적이다.

**필수 개념**

- 집계 전 행 선택과 집계 후 그룹 선택 · 가중치 0.50: WHERE는 집계 전에 입력 행을, HAVING은 집계 후에 그룹을 고른다는 적용 시점의 차이.
- WHERE에 집계를 쓸 수 없는 이유 · 가중치 0.50: WHERE 시점에는 집계 결과가 없어 집계 함수를 쓸 수 없고 집계가 불필요한 조건은 WHERE가 효율적이라는 설명.

**혼동 주의:** HAVING을 ‘GROUP BY와 함께 쓰는 WHERE의 다른 이름’으로 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 2.7 Aggregate Functions](https://www.postgresql.org/docs/17/tutorial-agg.html) — 2.7 Aggregate Functions; The fundamental difference between WHERE and HAVING

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-06 · INTERMEDIATE

WHERE customer_id = 42 조건으로 주문 금액 합계만 구하는 쿼리가 자주 실행됩니다. customer_id 인덱스가 있는데도 조건에 맞는 주문 행을 테이블에서 하나씩 읽습니다. 인덱스에 amount 컬럼을 포함시키면 무엇이 달라지나요? PostgreSQL 17에서 테이블을 여전히 읽게 되는 경우와 이 방법의 비용도 설명하세요.

**모범 답안**

customer_id만 있는 인덱스로는 대상 행의 위치만 알 수 있어, amount 값을 얻으려면 행마다 테이블을 읽어야 한다. 쿼리가 참조하는 컬럼이 모두 인덱스에 들어 있으면 인덱스만 읽어 답할 수 있는데, 이를 인덱스 전용 스캔이라 하고 그렇게 설계한 인덱스를 커버링 인덱스라 한다. PostgreSQL에서는 INCLUDE로 amount를 인덱스에 실을 수 있다. 다만 PostgreSQL은 행이 현재 트랜잭션에 보이는지를 인덱스만으로는 알 수 없어 가시성 맵을 확인하고, 최근에 변경된 페이지의 행은 테이블을 읽어 확인한다. 그래서 변경이 잦은 테이블에서는 효과가 줄어든다. 비용도 있다. 포함한 컬럼만큼 인덱스가 커지고, 그 컬럼이 바뀔 때도 인덱스를 갱신해야 한다.

**필수 개념**

- 인덱스만으로 답하는 조회 · 가중치 0.50: 쿼리가 참조하는 컬럼이 모두 인덱스에 있으면 테이블을 읽지 않고 답할 수 있다는 설명.
- 가시성 확인과 인덱스 크기의 비용 · 가중치 0.50: 최근 변경된 페이지는 테이블을 확인해야 하고 컬럼을 포함한 만큼 인덱스 크기와 갱신 비용이 는다는 한계.

**혼동 주의:** 커버링 인덱스를 쓰면 테이블을 전혀 읽지 않는다고 단정하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 11.9 Index-Only Scans and Covering Indexes](https://www.postgresql.org/docs/17/indexes-index-only-scans.html) — 11.9 Index-Only Scans and Covering Indexes; The query must reference only columns stored in the index

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-07 · BASIC

회원 테이블에 행이 다섯 개 있고 그중 두 행의 phone 컬럼이 NULL입니다. SELECT COUNT(*), COUNT(phone) FROM members의 결과는 각각 얼마인가요? 두 값이 다른 이유와, 전화번호를 등록하지 않은 회원 수를 구하는 방법을 설명하세요.

**모범 답안**

COUNT(*)는 5, COUNT(phone)은 3이다. COUNT(*)는 입력 행의 수를 세고, 컬럼을 인자로 준 COUNT는 그 컬럼 값이 NULL이 아닌 행만 센다. phone이 NULL인 두 행은 COUNT(phone)에서 빠진다. 전화번호를 등록하지 않은 회원 수는 두 값의 차로 구하거나, WHERE phone IS NULL 조건을 건 COUNT(*)로 구한다. NULL은 등호로 비교할 수 없으므로 phone = NULL 조건은 어떤 행도 고르지 못한다. 외부 조인 결과에서 짝이 없는 행을 0으로 세려 할 때도 같은 차이가 쓰인다. COUNT(*)는 NULL로 채워진 행도 한 건으로 세므로, 짝의 수를 세려면 오른쪽 테이블의 NULL이 아닌 컬럼을 COUNT에 넣어야 한다.

**필수 개념**

- 행 수와 NULL이 아닌 값의 수 · 가중치 0.50: COUNT(*)는 입력 행 수를, 컬럼을 준 COUNT는 그 컬럼이 NULL이 아닌 행 수를 센다는 차이.
- NULL 여부를 세는 올바른 조건 · 가중치 0.50: NULL은 등호가 아닌 IS NULL로 판별하며 외부 조인에서 짝의 수는 NULL이 아닌 컬럼으로 센다는 설명.

**혼동 주의:** 컬럼을 준 COUNT가 중복 값을 빼고 센다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 9.21 Aggregate Functions](https://www.postgresql.org/docs/17/functions-aggregate.html) — 9.21 Aggregate Functions; Computes the number of input rows in which the input value is not null

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-08 · BASIC

이번 달 주문 테이블과 지난달 보관 테이블에서 주문 번호를 각각 조회해 하나의 목록으로 합치려 합니다. 두 테이블에 같은 주문 번호가 중복되어 들어 있을 수 없다는 것이 보장됩니다. UNION과 UNION ALL 중 무엇을 써야 하는지, 두 연산이 하는 일의 차이와 비용으로 설명하세요.

**모범 답안**

UNION ALL을 쓴다. 두 연산 모두 두 쿼리의 결과를 이어 붙이지만, UNION은 DISTINCT처럼 결과에서 중복 행을 제거하고 UNION ALL은 그대로 둔다. 중복을 제거하려면 전체 결과를 정렬하거나 해시로 묶어 비교해야 하므로, 행이 많을수록 추가 비용이 커진다. 중복이 없다는 것이 보장된 상황에서 UNION을 쓰면 결과는 같으면서 이 비용만 더 든다. 반대로 같은 행이 양쪽에 있을 수 있고 한 번만 나와야 한다면 UNION이 맞다. 두 쿼리는 컬럼 수가 같고 대응하는 컬럼의 타입이 맞아야 하며, 어느 연산도 결과의 순서를 보장하지 않으므로 순서가 필요하면 전체에 ORDER BY를 붙인다.

**필수 개념**

- 중복 제거 여부의 차이 · 가중치 0.50: UNION은 결과의 중복 행을 제거하고 UNION ALL은 모든 행을 그대로 이어 붙인다는 차이.
- 중복 제거 비용에 따른 선택 · 가중치 0.50: 중복 제거에는 정렬이나 해시 비교 비용이 들므로 중복이 없다고 보장되면 UNION ALL을 쓴다는 판단.

**혼동 주의:** UNION ALL이 결과의 순서를 보장한다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 7.4 Combining Queries](https://www.postgresql.org/docs/17/queries-union.html) — 7.4 Combining Queries; eliminates duplicate rows from its result

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-09 · ADVANCED

느린 쿼리에 EXPLAIN ANALYZE를 실행했더니 한 노드의 예상 행 수는 10인데 실제 행 수는 50,000이었고, 그 위에서 중첩 루프 조인이 선택되어 있었습니다. 이 차이가 계획 선택에 어떤 영향을 주었을지 설명하세요. EXPLAIN과 EXPLAIN ANALYZE의 차이와, 추정이 크게 어긋날 때 먼저 확인할 것도 답하세요.

**모범 답안**

EXPLAIN은 쿼리를 실행하지 않고 플래너가 고른 계획과 추정 비용, 추정 행 수만 보여 준다. EXPLAIN ANALYZE는 쿼리를 실제로 실행해 각 노드의 실제 행 수와 걸린 시간을 추정값과 나란히 보여 준다. 플래너는 추정 행 수로 비용을 계산해 계획을 고른다. 바깥쪽 행이 10개뿐이라고 추정하면 행마다 안쪽을 한 번씩 탐색하는 중첩 루프가 가장 싸 보이지만, 실제로 50,000행이 나오면 안쪽 탐색이 그만큼 반복되어 느려진다. 정확히 추정했다면 해시 조인 같은 다른 계획이 선택됐을 수 있다. 추정이 크게 어긋나면 먼저 통계가 최신인지 확인하고 ANALYZE로 갱신한다. EXPLAIN ANALYZE는 쿼리를 실제로 수행하므로 변경 쿼리에 쓸 때는 트랜잭션 안에서 실행하고 롤백한다.

**필수 개념**

- 추정값과 실측값의 비교 · 가중치 0.50: EXPLAIN은 추정만, EXPLAIN ANALYZE는 실제 실행한 행 수와 시간을 추정과 함께 보여 준다는 차이.
- 행 수 추정 오류가 바꾸는 계획 · 가중치 0.50: 추정 행 수가 실제보다 훨씬 작으면 부적절한 조인 방식이 선택될 수 있어 통계 갱신부터 확인한다는 설명.

**혼동 주의:** 실행 계획에 인덱스 스캔이 보이면 최적이라고 판단하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 14.1 Using EXPLAIN](https://www.postgresql.org/docs/17/using-explain.html) — 14.1.2 EXPLAIN ANALYZE; EXPLAIN actually executes the query

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-SQL-10 · INTERMEDIATE

이메일 컬럼에 B-tree 인덱스가 있습니다. WHERE email LIKE 'kim%' 조건은 인덱스를 사용하는데, WHERE email LIKE '%@example.com' 조건과 WHERE lower(email) = 'kim@example.com' 조건은 테이블 전체를 읽습니다. 정렬된 인덱스 구조에 비추어 세 조건의 차이를 설명하고 뒤의 두 경우의 대안을 제시하세요.

**모범 답안**

B-tree 인덱스는 값을 정렬된 순서로 보관한다. 'kim%'처럼 앞부분이 고정된 패턴은 kim으로 시작하는 값들이 정렬 순서에서 한 구간에 모여 있으므로 그 구간만 읽으면 된다. '%@example.com'처럼 앞이 정해지지 않은 패턴은 해당 값들이 인덱스 전체에 흩어져 있어 구간을 좁힐 수 없다. lower(email) 조건은 인덱스에 저장된 값에 함수를 적용한 결과를 비교하므로 원래 값의 정렬 순서를 쓸 수 없다. 뒤쪽 일치는 문자열을 뒤집은 식에 인덱스를 만들어 앞쪽 일치로 바꾸는 방법이 있고, 함수 조건은 lower(email) 식 자체에 인덱스를 만들면 된다. PostgreSQL에서 LIKE의 앞쪽 일치가 인덱스를 쓰려면 데이터베이스 로케일에 따라 패턴 비교용 연산자 클래스로 인덱스를 만들어야 할 수 있다.

**필수 개념**

- 정렬 구간으로 좁혀지는 조건 · 가중치 0.50: 앞부분이 고정된 패턴은 정렬된 인덱스의 한 구간으로 좁혀지지만 앞이 열린 패턴은 좁힐 수 없다는 설명.
- 가공된 값 비교와 식 인덱스 · 가중치 0.50: 컬럼에 함수를 적용한 조건은 저장된 값의 순서를 쓸 수 없어 그 식에 대한 인덱스가 필요하다는 설명.

**혼동 주의:** LIKE 조건에는 인덱스를 전혀 쓸 수 없다고 설명하지 않기.

**출처 대조 위치**

- [PostgreSQL 17 Documentation — 11.2 Index Types](https://www.postgresql.org/docs/17/indexes-types.html) — 11.2.1 B-Tree; anchored to the beginning of the string

근거 문서: `r1-sql-and-indexes`. 검수 상태: **PENDING**

### R1-DS-01 · INTERMEDIATE

스택 두 개만으로 큐를 구현하려 합니다. 넣을 때는 항상 첫 번째 스택에 push하고, 꺼낼 때는 두 번째 스택에서 pop합니다. 두 번째 스택이 비었을 때 해야 하는 일과 그 결과 먼저 넣은 원소가 먼저 나오는 이유를 설명하세요. 꺼내기 한 번의 최악 비용과 평균 비용도 구분하세요.

**모범 답안**

두 번째 스택이 비어 있으면 첫 번째 스택의 원소를 모두 pop해 두 번째 스택에 push한다. 스택은 나중에 넣은 것이 먼저 나오므로, 옮기는 과정에서 순서가 한 번 뒤집혀 가장 먼저 넣은 원소가 두 번째 스택의 맨 위에 온다. 그 뒤로는 두 번째 스택에서 pop하면 넣은 순서대로 나온다. 두 번째 스택에 원소가 남아 있는 동안에는 옮기지 않아야 순서가 유지된다. 옮기는 일이 일어나는 꺼내기는 첫 번째 스택의 크기에 비례하는 비용이 든다. 그러나 각 원소는 첫 번째 스택에 한 번 들어가고, 한 번 옮겨지고, 한 번 나갈 뿐이므로 연산 전체로 나누면 꺼내기 한 번의 평균 비용은 상수다.

**필수 개념**

- 옮기며 뒤집히는 순서 · 가중치 0.50: 입력 스택을 비워 출력 스택으로 옮기면 순서가 뒤집혀 먼저 넣은 원소가 먼저 나온다는 설명.
- 최악 선형, 상환 상수 비용 · 가중치 0.50: 옮기는 꺼내기는 선형이지만 원소마다 이동이 한 번뿐이라 평균 비용은 상수라는 구분.

**혼동 주의:** 출력 스택에 원소가 남아 있는데도 매번 옮긴다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 1.3 Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/) — 1.3 Bags, Queues, and Stacks

근거 문서: `r1-data-structures`. 검수 상태: **PENDING**

### R1-DS-02 · INTERMEDIATE

이진 탐색 트리에 키를 1, 2, 3, 4, 5 순서로 넣었을 때와 3, 1, 4, 2, 5 순서로 넣었을 때 트리의 모양은 어떻게 달라지나요? 값 5를 찾는 데 필요한 비교 횟수로 차이를 설명하고, 탐색 비용이 무엇에 의해 결정되는지 답하세요.

**모범 답안**

이진 탐색 트리는 새 키를 루트부터 비교해 작으면 왼쪽, 크면 오른쪽으로 내려가 빈 자리에 넣는다. 1부터 5까지 오름차순으로 넣으면 매번 오른쪽 끝에 붙어 한 줄로 늘어선 트리가 되고, 5를 찾으려면 다섯 번 비교해야 한다. 3, 1, 4, 2, 5 순서로 넣으면 3이 루트가 되고 1과 4가 그 자식, 2와 5가 그 아래에 놓여 5를 세 번의 비교로 찾는다. 같은 키 집합이라도 넣는 순서에 따라 모양이 달라지며, 탐색 비용은 키의 개수가 아니라 트리의 높이로 결정된다. 높이는 균형이 잡히면 키 개수의 로그에 비례하지만 한쪽으로 치우치면 키 개수에 비례한다. 그래서 정렬된 입력이 들어올 수 있는 곳에는 삽입 때 스스로 균형을 맞추는 트리를 쓴다.

**필수 개념**

- 삽입 순서가 정하는 트리 모양 · 가중치 0.50: 같은 키라도 삽입 순서에 따라 모양이 달라지고 정렬된 순서로 넣으면 한 줄로 치우친다는 설명.
- 높이에 비례하는 탐색 비용 · 가중치 0.50: 탐색 비용은 트리 높이로 정해지며 균형이면 로그, 치우치면 선형이라는 설명.

**혼동 주의:** 이진 탐색 트리의 탐색이 항상 O(log n)이라고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 3.2 Binary Search Trees](https://algs4.cs.princeton.edu/32bst/) — 3.2 Binary Search Trees; depends on the order in which keys are inserted

근거 문서: `r1-data-structures`. 검수 상태: **PENDING**

### R1-DS-03 · BASIC

이진 탐색 트리의 모든 키를 오름차순으로 출력하려 합니다. 왼쪽 서브트리, 자기 자신, 오른쪽 서브트리 순서로 방문하는 중위 순회가 정렬된 순서를 내는 이유를 트리의 성질로 설명하세요. 노드가 n개일 때 걸리는 시간과, 치우친 트리에서 재귀 호출 깊이가 어떻게 되는지도 답하세요.

**모범 답안**

이진 탐색 트리에서는 어떤 노드든 왼쪽 서브트리의 키가 모두 그 노드의 키보다 작고 오른쪽 서브트리의 키는 모두 크다. 중위 순회는 한 노드를 방문하기 전에 왼쪽 서브트리를 전부 방문하고, 방문한 뒤에 오른쪽 서브트리를 방문한다. 따라서 어떤 키보다 작은 키들이 항상 먼저, 큰 키들이 나중에 출력되고, 이 성질이 모든 서브트리에 재귀적으로 적용되어 전체가 오름차순이 된다. 각 노드를 정확히 한 번씩 방문하므로 시간은 n에 비례한다. 재귀 호출의 깊이는 트리의 높이와 같아서, 균형 잡힌 트리에서는 로그 수준이지만 한쪽으로 치우친 트리에서는 n까지 깊어져 호출 스택을 많이 쓴다.

**필수 개념**

- 순서 성질과 중위 순회 · 가중치 0.50: 왼쪽은 작고 오른쪽은 크다는 성질 때문에 왼쪽·노드·오른쪽 순서의 방문이 오름차순을 낸다는 설명.
- 방문 횟수와 재귀 깊이 · 가중치 0.50: 모든 노드를 한 번씩 방문해 선형 시간이고 재귀 깊이는 트리 높이만큼이라는 설명.

**혼동 주의:** 전위 순회나 후위 순회도 정렬된 순서를 낸다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 3.2 Binary Search Trees](https://algs4.cs.princeton.edu/32bst/) — 3.2 Binary Search Trees; inorder traversal

근거 문서: `r1-data-structures`. 검수 상태: **PENDING**

### R1-DS-04 · INTERMEDIATE

이진 트리가 이진 탐색 트리인지 검사하는 함수를 작성했습니다. 각 노드에서 왼쪽 자식의 키가 자신보다 작고 오른쪽 자식의 키가 자신보다 큰지만 확인합니다. 루트가 10이고 왼쪽 자식이 5, 그 5의 오른쪽 자식이 12인 트리로 이 검사가 틀리는 이유를 설명하고 올바른 검사 방법을 제시하세요.

**모범 답안**

이 트리에서 5는 10보다 작고 12는 5보다 크므로 부모와 자식만 비교하는 검사는 통과한다. 그러나 12는 루트 10의 왼쪽 서브트리 안에 있으면서 10보다 크다. 이진 탐색 트리의 조건은 자식 하나가 아니라 서브트리 전체에 대한 것이어서, 왼쪽 서브트리의 모든 키가 그 노드보다 작아야 한다. 이 트리에서 12를 찾으면 루트에서 오른쪽으로 내려가므로 찾지 못한다. 올바른 검사는 각 노드가 가질 수 있는 키의 범위를 위에서 아래로 전달하는 것이다. 왼쪽으로 내려갈 때는 상한을, 오른쪽으로 내려갈 때는 하한을 현재 노드의 키로 좁히고, 노드의 키가 범위를 벗어나면 실패로 판정한다. 중위 순회를 하며 키가 계속 커지는지 확인하는 방법도 같은 결과를 준다.

**필수 개념**

- 서브트리 전체에 대한 순서 조건 · 가중치 0.50: BST 조건은 부모와 자식 사이가 아니라 서브트리의 모든 키에 적용되어 직계 비교만으로는 위반을 놓친다는 설명.
- 범위 전달 또는 중위 순회 검사 · 가중치 0.50: 허용 범위를 내려보내며 확인하거나 중위 순회가 오름차순인지 확인한다는 올바른 방법.

**혼동 주의:** 부모와 자식의 대소 관계만 맞으면 이진 탐색 트리라고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 3.2 Binary Search Trees](https://algs4.cs.princeton.edu/32bst/) — 3.2 Binary Search Trees; larger than the keys in all nodes in that node's left subtree

근거 문서: `r1-data-structures`. 검수 상태: **PENDING**

### R1-DS-05 · INTERMEDIATE

용량이 정해진 캐시에서 공간이 부족하면 가장 오래 사용되지 않은 항목을 버리려 합니다. 조회와 삽입, 가장 오래된 항목 제거를 모두 평균 O(1)에 처리하려면 해시 맵 하나만으로는 왜 부족한가요? 어떤 구조를 함께 써야 하는지와 조회할 때 해야 하는 일을 설명하세요.

**모범 답안**

해시 맵은 키로 값을 찾는 일은 평균 상수 시간에 하지만 항목들이 사용된 순서를 알지 못한다. 가장 오래 사용되지 않은 항목을 찾으려면 모든 항목의 마지막 사용 시각을 훑어야 해 선형 시간이 든다. 그래서 사용 순서를 유지하는 이중 연결 리스트를 함께 둔다. 해시 맵은 키에서 리스트 노드를 가리키고, 리스트는 한쪽 끝에 가장 최근에 쓴 항목을, 반대쪽 끝에 가장 오래된 항목을 둔다. 조회할 때는 값을 돌려주는 것에 더해 그 노드를 리스트에서 떼어 최근 쪽 끝으로 옮겨야 한다. 이중 연결 리스트는 노드의 앞뒤 참조가 있어 떼어 내고 붙이는 일이 상수 시간이다. 용량을 넘으면 오래된 쪽 끝의 노드를 리스트와 해시 맵에서 함께 지운다.

**필수 개념**

- 해시 맵만으로는 알 수 없는 사용 순서 · 가중치 0.50: 해시 맵은 사용 순서를 유지하지 않아 가장 오래된 항목을 찾는 데 선형 시간이 든다는 설명.
- 해시 맵과 이중 연결 리스트의 결합 · 가중치 0.50: 해시 맵이 리스트 노드를 가리키고 조회 때 노드를 최근 쪽으로 옮기며 오래된 쪽 끝을 제거한다는 구조.

**혼동 주의:** 접근 순서 캐시에서 조회가 구조를 바꾸지 않는 읽기 전용 연산이라고 설명하지 않기.

**출처 대조 위치**

- [Java SE 21 API — java.util.LinkedHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html) — LinkedHashMap: access-order; removeEldestEntry

근거 문서: `r1-data-structures`. 검수 상태: **PENDING**

### R1-DS-06 · ADVANCED

정렬된 순서로 들어오는 키를 저장해야 하는데 탐색과 삽입이 최악의 경우에도 로그 시간에 끝나야 합니다. 일반 이진 탐색 트리 대신 레드-블랙 트리 같은 균형 탐색 트리를 쓰면 무엇이 보장되나요? 그 보장을 유지하기 위해 삽입 때 추가로 하는 일과, 해시 테이블 대신 이 구조를 고르는 경우를 설명하세요.

**모범 답안**

일반 이진 탐색 트리는 정렬된 입력에서 한 줄로 늘어나 탐색과 삽입이 선형 시간이 된다. 균형 탐색 트리는 어떤 순서로 키가 들어와도 트리의 높이를 키 개수의 로그 수준으로 유지해, 탐색과 삽입, 삭제의 최악 비용을 로그 시간으로 보장한다. 이 보장을 지키기 위해 삽입이나 삭제로 균형 조건이 깨지면 노드의 색을 바꾸거나 회전이라는 국소적인 재배치를 수행한다. 재배치는 삽입 경로 위에서만 일어나므로 비용도 로그 수준에 머문다. 해시 테이블은 평균 상수 시간에 찾지만 키의 순서를 유지하지 않는다. 가장 작은 키, 어떤 값 이상인 첫 키, 범위 안의 키처럼 순서에 기반한 조회가 필요하면 균형 탐색 트리를 고른다.

**필수 개념**

- 입력 순서와 무관한 로그 높이 · 가중치 0.50: 균형 탐색 트리는 어떤 삽입 순서에서도 높이를 로그 수준으로 유지해 최악 비용을 로그 시간으로 보장한다는 설명.
- 재균형 비용과 순서 기반 조회 · 가중치 0.50: 색 변경과 회전으로 균형을 유지하며 최소·범위 같은 순서 기반 조회가 필요할 때 해시 대신 선택한다는 설명.

**혼동 주의:** 균형 트리의 삽입 비용이 재균형 때문에 선형이 된다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 3.3 Balanced Search Trees](https://algs4.cs.princeton.edu/33balanced/) — 3.3 Balanced Search Trees; costs are guaranteed to be logarithmic

근거 문서: `r1-data-structures`. 검수 상태: **PENDING**

### R1-ALG-01 · BASIC

길이 n인 정수 배열에 중복된 값이 있는지 확인하는 방법이 두 가지 있습니다. 하나는 모든 쌍을 이중 반복문으로 비교하고, 다른 하나는 본 값을 해시 집합에 넣어 가며 확인합니다. 두 방법의 시간 복잡도와 추가 공간 복잡도를 비교하고, n이 10배가 될 때 실행 시간이 각각 어떻게 변할지 설명하세요.

**모범 답안**

이중 반복문은 모든 쌍을 비교하므로 비교 횟수가 n의 제곱에 비례하고, 입력 외에 쓰는 공간은 상수다. 해시 집합 방법은 원소마다 포함 여부 확인과 삽입을 한 번씩 하므로 평균적으로 n에 비례하는 시간이 들지만, 최대 n개의 값을 따로 저장해야 해 추가 공간도 n에 비례한다. 시간을 줄이는 대신 공간을 더 쓰는 교환이다. n이 10배가 되면 제곱 시간 방법은 약 100배 느려지고 선형 시간 방법은 약 10배 느려진다. 복잡도는 이렇게 입력이 커질 때 비용이 늘어나는 비율을 나타낼 뿐 실제 실행 시간 자체를 말해 주지 않는다. n이 작을 때는 상수 배수가 작은 이중 반복문이 더 빠를 수도 있다.

**필수 개념**

- 시간과 공간의 증가율 비교 · 가중치 0.50: 쌍 비교는 제곱 시간·상수 공간, 해시 집합은 평균 선형 시간·선형 공간이라는 비교.
- 증가율로 읽는 복잡도 · 가중치 0.50: 복잡도는 입력이 커질 때의 비용 증가 비율을 나타내며 입력 10배에 제곱은 100배, 선형은 10배라는 해석.

**혼동 주의:** 복잡도가 낮은 알고리즘이 모든 입력 크기에서 더 빠르다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 1.4 Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/) — 1.4 Analysis of Algorithms; order of growth

근거 문서: `r1-algorithms`. 검수 상태: **PENDING**

### R1-ALG-02 · INTERMEDIATE

사용자 100만 명 사이에 친구 관계가 계속 추가되는 중에 ‘두 사용자가 친구 관계를 따라 연결되어 있는가’를 반복해서 물어야 합니다. 질문마다 그래프를 탐색하는 방법 대신 유니온-파인드를 쓰면 무엇을 저장하고 어떻게 답하나요? 트리가 한쪽으로 길어지는 것을 막는 두 가지 기법도 설명하세요.

**모범 답안**

질문마다 그래프를 탐색하면 한 번 답할 때마다 연결된 사용자를 모두 방문할 수 있어 질문이 많아질수록 느리다. 유니온-파인드는 연결된 사용자들을 하나의 집합으로 관리한다. 각 원소가 부모 하나를 가리키는 트리로 집합을 표현하고, 부모를 따라 올라가 닿는 루트가 그 집합의 대표다. 두 사용자가 연결되어 있는지는 두 사용자의 루트가 같은지로 답하고, 친구 관계가 추가되면 한쪽 루트를 다른 쪽 루트에 붙여 두 집합을 합친다. 트리가 길어지면 루트를 찾는 비용이 커지므로, 합칠 때 항상 작은 트리를 큰 트리 아래에 붙이고, 루트를 찾으면서 지나간 노드들을 루트에 가깝게 다시 연결하는 경로 압축을 쓴다. 두 기법을 함께 쓰면 연산 한 번의 비용이 사실상 상수에 가깝다.

**필수 개념**

- 루트 비교로 답하는 연결 여부 · 가중치 0.50: 원소가 부모를 가리키는 트리로 집합을 표현하고 두 원소의 루트가 같은지로 연결 여부를 답한다는 설명.
- 크기 기준 합치기와 경로 압축 · 가중치 0.50: 작은 트리를 큰 트리에 붙이고 루트 탐색 경로를 압축해 트리 높이를 낮게 유지한다는 기법.

**혼동 주의:** 유니온-파인드로 두 원소 사이의 경로까지 알 수 있다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 1.5 Case Study: Union-Find](https://algs4.cs.princeton.edu/15uf/) — 1.5 Case Study: Union-Find; Weighted quick-union with path compression

근거 문서: `r1-algorithms`. 검수 상태: **PENDING**

### R1-ALG-03 · INTERMEDIATE

간선 비용이 모두 0 이상인 그래프에서 다익스트라 알고리즘은 아직 확정되지 않은 정점 가운데 거리가 가장 짧은 정점을 꺼내 그 거리를 확정합니다. 이 확정이 옳은 근거가 무엇인가요? 음수 비용 간선이 하나라도 있으면 왜 그 근거가 깨지는지 설명하고, 그때 쓸 수 있는 방법을 답하세요.

**모범 답안**

간선 비용이 음수가 아니면 경로에 간선을 더할수록 거리는 줄지 않는다. 확정되지 않은 정점 가운데 거리가 가장 짧은 정점으로 가는 다른 경로는 반드시 그보다 멀거나 같은 미확정 정점을 거쳐야 하므로, 지금의 거리보다 짧아질 수 없다. 그래서 꺼낸 순간의 거리를 최단 거리로 확정해도 된다. 음수 간선이 있으면 더 먼 정점을 거쳐 돌아오는 경로가 음수 간선 덕분에 더 짧아질 수 있어, 이미 확정한 거리가 틀릴 수 있다. 이 경우에는 모든 간선의 완화를 정점 수보다 하나 적은 횟수만큼 반복하는 벨만-포드 알고리즘을 쓴다. 시작점에서 닿을 수 있는 음수 사이클이 있으면 돌 때마다 거리가 줄어 최단 경로 자체가 정의되지 않는다.

**필수 개념**

- 비음수 간선과 거리 확정의 근거 · 가중치 0.50: 간선이 음수가 아니면 다른 경로가 더 짧아질 수 없어 가장 가까운 미확정 정점의 거리를 확정할 수 있다는 설명.
- 음수 간선에서의 실패와 벨만-포드 · 가중치 0.50: 음수 간선은 확정된 거리를 뒤집을 수 있어 벨만-포드를 쓰고 음수 사이클이면 최단 경로가 정의되지 않는다는 설명.

**혼동 주의:** 모든 간선에 같은 값을 더해 음수를 없애면 다익스트라를 쓸 수 있다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 4.4 Shortest Paths](https://algs4.cs.princeton.edu/44sp/) — 4.4 Shortest Paths; Negative weights introduce complications

근거 문서: `r1-algorithms`. 검수 상태: **PENDING**

### R1-ALG-04 · BASIC

지도 격자에서 상하좌우로 이어진 땅 칸들을 하나의 섬으로 볼 때 섬의 개수를 세려 합니다. 이 문제를 그래프의 연결 요소 개수로 바꿔 깊이 우선 탐색으로 푸는 절차를 설명하세요. 방문 표시가 왜 필요한지와, 칸이 V개이고 인접 관계가 E개일 때의 시간 복잡도를 답하세요.

**모범 답안**

땅 칸 하나를 정점으로, 상하좌우로 이웃한 두 땅 칸 사이를 간선으로 보면 섬은 서로 닿을 수 있는 정점들의 묶음인 연결 요소가 된다. 모든 칸을 차례로 보면서 아직 방문하지 않은 땅 칸을 만나면 섬의 수를 하나 올리고, 그 칸에서 깊이 우선 탐색을 시작해 닿을 수 있는 모든 땅 칸에 방문 표시를 한다. 탐색 한 번이 섬 하나를 통째로 표시하므로, 탐색을 시작한 횟수가 섬의 개수다. 방문 표시가 없으면 같은 칸을 이웃끼리 서로 다시 찾아가 끝나지 않고, 한 섬을 여러 번 세게 된다. 각 정점은 한 번만 방문되고 각 간선은 양 끝에서 한 번씩 확인되므로 시간은 V와 E의 합에 비례한다. 재귀로 구현하면 큰 섬에서 호출 깊이가 칸 수만큼 깊어질 수 있어 명시적 스택으로 바꾸기도 한다.

**필수 개념**

- 연결 요소로의 모델링 · 가중치 0.50: 칸을 정점, 인접을 간선으로 보고 탐색을 새로 시작한 횟수가 연결 요소의 수가 된다는 설명.
- 방문 표시와 선형 시간 · 가중치 0.50: 방문 표시로 재방문과 중복 집계를 막아 각 정점과 간선을 상수 번만 처리하므로 V+E에 비례한다는 설명.

**혼동 주의:** 탐색을 새로 시작할 때마다 방문 표시를 초기화한다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 4.1 Undirected Graphs](https://algs4.cs.princeton.edu/41graph/) — 4.1 Undirected Graphs; maximal connected subgraphs

근거 문서: `r1-algorithms`. 검수 상태: **PENDING**

### R1-ALG-05 · INTERMEDIATE

정렬되지 않은 n개의 회원 ID 배열에서 특정 ID가 있는지 확인해야 합니다. 한 번만 확인하는 경우와 같은 배열에 대해 수백만 번 확인하는 경우에, 선형 탐색과 ‘정렬한 뒤 이분 탐색’ 중 어느 쪽이 유리한지 비용으로 비교하세요. 이분 탐색이 한 단계마다 버리는 범위와 전제 조건도 설명하세요.

**모범 답안**

이분 탐색은 배열이 정렬되어 있다는 전제에서, 가운데 원소와 비교해 찾는 값이 있을 수 없는 절반을 버린다. 한 번 비교할 때마다 남은 범위가 절반이 되므로 비교 횟수는 n의 로그에 비례한다. 정렬되지 않은 배열에서는 가운데 값과의 대소 비교가 어느 쪽에 값이 있는지 알려 주지 못해 쓸 수 없다. 한 번만 확인한다면 선형 탐색은 최대 n번 비교로 끝나지만, 정렬에는 n과 n의 로그를 곱한 만큼의 비용이 먼저 들어 정렬 후 탐색이 더 느리다. 같은 배열에 대해 여러 번 확인한다면 정렬은 한 번만 하고 이후 질의마다 로그 비용만 들므로, 질의 수가 많아질수록 이분 탐색이 유리하다. 배열이 계속 바뀐다면 정렬 상태를 유지하는 비용도 계산에 넣어야 한다.

**필수 개념**

- 정렬 전제와 절반씩 버리기 · 가중치 0.50: 정렬된 배열에서 가운데와 비교해 절반을 버리므로 로그 횟수 비교로 끝나고 정렬이 없으면 쓸 수 없다는 설명.
- 전처리 비용과 질의 횟수의 균형 · 가중치 0.50: 정렬 비용은 한 번이고 질의마다 로그 비용이므로 질의가 많을수록 유리하다는 판단.

**혼동 주의:** 한 번만 찾는 경우에도 정렬 후 이분 탐색이 항상 빠르다고 설명하지 않기.

**출처 대조 위치**

- [Algorithms 4th Edition — 1.1 Programming Model](https://algs4.cs.princeton.edu/11model/) — 1.1 Programming Model; Binary search

근거 문서: `r1-algorithms`. 검수 상태: **PENDING**

### R1-OS-01 · BASIC

실행 중이던 프로세스가 파일 읽기를 요청하자 CPU는 다른 프로세스를 실행했고, 읽기가 끝난 뒤에도 원래 프로세스가 곧바로 실행되지는 않았습니다. 프로세스의 실행·준비·대기 세 상태로 이 과정을 설명하세요. 대기 상태의 프로세스가 입출력 완료 후 실행 상태가 아닌 준비 상태로 가는 이유도 답하세요.

**모범 답안**

실행 상태는 프로세스가 CPU에서 명령을 수행 중인 상태다. 파일 읽기를 요청하면 그 결과가 올 때까지 할 일이 없으므로 프로세스는 대기 상태가 되고, 운영체제는 준비 상태의 다른 프로세스에 CPU를 준다. 준비 상태는 실행할 수 있지만 운영체제가 지금은 다른 프로세스를 실행하기로 한 상태다. 읽기가 끝나면 원래 프로세스는 다시 실행할 수 있게 되지만, 그 순간 CPU는 다른 프로세스가 쓰고 있다. 누구를 실행할지는 스케줄러가 정하므로 완료된 프로세스는 준비 상태로 옮겨져 차례를 기다린다. 대기는 프로세스 자신의 사건을 기다리는 것이고, 준비는 CPU를 받기만 기다리는 것이라는 점이 다르다.

**필수 개념**

- 입출력 요청에 따른 대기 전환 · 가중치 0.50: 입출력을 요청한 프로세스는 완료까지 대기 상태가 되고 CPU는 준비 상태의 다른 프로세스에 넘어간다는 설명.
- 완료 후 준비 상태로 가는 이유 · 가중치 0.50: 입출력이 끝나도 실행 여부는 스케줄러가 정하므로 준비 상태에서 차례를 기다린다는 설명.

**혼동 주의:** 입출력이 끝난 프로세스가 즉시 CPU를 받는다고 설명하지 않기.

**출처 대조 위치**

- [OSTEP 4: The Abstraction: The Process](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-intro.pdf) — 4.4 Process States

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-02 · INTERMEDIATE

웹 서버가 요청마다 새 프로세스를 만드는 방식과 한 프로세스 안에서 요청마다 스레드를 만드는 방식이 있습니다. 두 방식에서 실행 단위들이 공유하는 것과 각자 따로 갖는 것을 비교하세요. 그 차이가 데이터 공유의 편의와 장애 격리에 어떤 영향을 주는지도 설명하세요.

**모범 답안**

프로세스는 저마다 독립된 주소 공간을 가진다. 한 프로세스의 메모리를 다른 프로세스가 직접 읽거나 쓸 수 없으므로, 데이터를 주고받으려면 파이프나 공유 메모리 같은 별도 수단이 필요하다. 대신 한 프로세스가 잘못된 메모리 접근으로 죽어도 다른 프로세스는 영향을 받지 않는다. 같은 프로세스의 스레드들은 주소 공간을 공유해 코드와 힙, 전역 데이터를 함께 쓰고, 스레드마다 따로 갖는 것은 실행 위치를 나타내는 레지스터와 스택이다. 그래서 스레드 사이의 데이터 공유는 변수를 읽고 쓰는 것으로 충분하고 생성과 전환 비용도 프로세스보다 작다. 그러나 공유 데이터에 동시에 접근하면 경쟁 상태가 생기므로 동기화가 필요하고, 한 스레드의 오류가 프로세스 전체를 종료시킬 수 있다.

**필수 개념**

- 주소 공간의 독립과 공유 · 가중치 0.50: 프로세스는 주소 공간이 독립이고 스레드는 주소 공간을 공유하며 레지스터와 스택만 따로 가진다는 구분.
- 공유의 편의와 격리의 교환 · 가중치 0.50: 스레드는 공유가 쉽고 가볍지만 동기화가 필요하고 오류가 번지며 프로세스는 격리되지만 통신 수단이 필요하다는 설명.

**혼동 주의:** 스레드가 스택까지 공유한다고 설명하지 않기.

**출처 대조 위치**

- [OSTEP 26: Concurrency: An Introduction](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-intro.pdf) — 26.1 Why Use Threads?; share the same address space

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-03 · INTERMEDIATE

서로 다른 두 프로세스가 데이터를 주고받아야 합니다. 파이프를 쓰는 방법과 공유 메모리를 쓰는 방법에서 데이터가 전달되는 경로는 어떻게 다른가요? 각각 운영체제가 대신 해 주는 일과 프로그램이 직접 책임져야 하는 일을 구분해 설명하세요.

**모범 답안**

파이프는 운영체제가 관리하는 단방향 바이트 통로다. 보내는 프로세스가 쓴 데이터는 커널의 버퍼로 복사되고, 받는 프로세스가 읽을 때 다시 그 프로세스의 메모리로 복사된다. 읽을 데이터가 없으면 읽는 쪽이 기다리고 버퍼가 가득 차면 쓰는 쪽이 기다리므로, 순서와 흐름을 운영체제가 맞춰 준다. 다만 바이트의 흐름일 뿐이어서 메시지의 경계는 프로그램이 정해야 한다. 공유 메모리는 두 프로세스가 같은 물리 메모리 영역을 각자의 주소 공간에 연결해 쓰는 방식이다. 한쪽이 쓴 값을 다른 쪽이 복사 없이 바로 읽으므로 큰 데이터를 주고받는 데 빠르다. 대신 운영체제는 접근 순서를 조율해 주지 않으므로, 세마포어 같은 수단으로 동시 접근을 직접 동기화해야 한다.

**필수 개념**

- 커널 버퍼를 거치는 파이프 · 가중치 0.50: 파이프는 커널 버퍼를 통한 단방향 바이트 통로이며 대기와 순서를 운영체제가 맞춰 준다는 설명.
- 복사 없는 공유와 동기화 책임 · 가중치 0.50: 공유 메모리는 같은 영역을 함께 연결해 복사 없이 빠르지만 동시 접근 동기화는 프로그램 책임이라는 설명.

**혼동 주의:** 공유 메모리를 쓰면 동기화가 필요 없다고 설명하지 않기.

**출처 대조 위치**

- [Linux man-pages — pipe(7)](https://man7.org/linux/man-pages/man7/pipe.7.html) — pipe(7): unidirectional interprocess communication channel

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-04 · INTERMEDIATE

동시에 접속할 수 있는 데이터베이스 커넥션이 세 개뿐이어서 네 번째 스레드는 기다려야 합니다. 또 다른 곳에서는 공유 카운터를 한 번에 한 스레드만 수정해야 합니다. 두 상황에 각각 세마포어와 락 중 무엇이 맞는지, 세마포어의 값이 무엇을 뜻하는지와 함께 설명하세요.

**모범 답안**

세마포어는 정수 값을 가진 객체이고 대기와 신호 두 연산으로 다룬다. 대기 연산은 값을 하나 줄이고 값이 음수가 되면 호출한 스레드를 재우며, 신호 연산은 값을 하나 올리고 기다리는 스레드가 있으면 하나를 깨운다. 초기값을 3으로 두면 세 스레드까지 통과하고 네 번째부터 기다리므로, 개수가 정해진 자원을 나눠 쓰는 커넥션 제한에 맞다. 공유 카운터처럼 한 번에 한 스레드만 들어가야 하는 임계 영역에는 락이 맞다. 초기값이 1인 세마포어도 같은 역할을 할 수 있어 이를 이진 세마포어라 한다. 다만 락은 잠근 스레드가 풀어야 한다는 소유 개념이 있지만 세마포어는 다른 스레드가 신호를 보낼 수 있어, 한 스레드가 끝나기를 다른 스레드가 기다리는 순서 맞추기에도 쓰인다.

**필수 개념**

- 세마포어 값과 통과 가능 수 · 가중치 0.50: 세마포어는 정수 값으로 통과할 수 있는 스레드 수를 제어하며 대기는 값을 줄이고 신호는 올린다는 설명.
- 상호 배제용 락과 개수 제한용 세마포어 · 가중치 0.50: 한 스레드만 들어갈 임계 영역에는 락, 정해진 개수의 자원 제한에는 세마포어가 맞고 초기값 1이면 락처럼 쓴다는 구분.

**혼동 주의:** 세마포어와 뮤텍스를 이름만 다른 같은 것으로 설명하지 않기.

**출처 대조 위치**

- [OSTEP 31: Semaphores](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-sema.pdf) — 31.2 Binary Semaphores (Locks)

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-05 · INTERMEDIATE

스레드 1은 락 A를 잡은 뒤 락 B를 잡으려 하고, 스레드 2는 락 B를 잡은 뒤 락 A를 잡으려 해서 둘 다 멈췄습니다. 교착 상태가 성립하려면 동시에 만족해야 하는 네 조건을 이 사례에 대입해 설명하세요. 그중 순환 대기 조건을 깨는 방법도 제시하세요.

**모범 답안**

네 조건은 상호 배제, 점유 대기, 비선점, 순환 대기다. 상호 배제는 락을 한 번에 한 스레드만 가질 수 있다는 것이다. 점유 대기는 스레드 1이 락 A를 쥔 채로 락 B를 기다리는 것처럼, 가진 자원을 놓지 않고 다른 자원을 기다리는 것이다. 비선점은 다른 스레드가 쥔 락을 강제로 빼앗을 수 없다는 것이다. 순환 대기는 스레드 1이 스레드 2의 락을, 스레드 2가 스레드 1의 락을 기다려 대기 관계가 고리를 이루는 것이다. 네 조건이 모두 성립해야 교착이 생기므로 하나만 깨도 막을 수 있다. 순환 대기는 모든 락에 순서를 정하고 모든 스레드가 그 순서대로만 잡게 하면 깨진다. 두 스레드가 모두 A를 먼저 잡는다면 한쪽이 A에서 기다릴 뿐 고리는 만들어지지 않는다.

**필수 개념**

- 교착의 네 가지 성립 조건 · 가중치 0.50: 상호 배제·점유 대기·비선점·순환 대기가 모두 성립해야 교착이 생긴다는 설명과 사례 대입.
- 락 순서로 깨는 순환 대기 · 가중치 0.50: 모든 락에 전체 순서를 정해 그 순서로만 획득하면 대기의 고리가 생기지 않는다는 예방법.

**혼동 주의:** 네 조건 중 하나만 성립해도 교착이 생긴다고 설명하지 않기.

**출처 대조 위치**

- [OSTEP 32: Common Concurrency Problems](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-bugs.pdf) — OSTEP 32: Conditions for Deadlock; Circular wait

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-06 · INTERMEDIATE

가변 크기의 세그먼트로 메모리를 나눠 주는 방식에서는 빈 공간의 합은 충분한데도 새 세그먼트를 넣지 못하는 일이 생깁니다. 이 현상의 이름과 원인을 설명하세요. 메모리를 고정 크기 페이지로 나누면 이 문제가 어떻게 달라지고 대신 무엇이 필요해지는지 답하세요.

**모범 답안**

이 현상은 외부 단편화다. 세그먼트는 크기가 제각각이어서 할당과 해제를 반복하면 빈 공간이 여러 작은 조각으로 흩어진다. 조각들의 합은 충분해도 연속된 큰 공간이 없으면 새 세그먼트를 배치할 수 없다. 페이징은 가상 주소 공간과 물리 메모리를 같은 크기의 고정 단위로 나눈다. 어떤 빈 물리 프레임에든 어떤 페이지든 넣을 수 있으므로 연속된 공간이 필요 없고 외부 단편화가 생기지 않는다. 대신 가상 페이지가 어느 물리 프레임에 있는지를 기록하는 페이지 테이블이 프로세스마다 필요하고, 메모리 접근마다 이 변환을 거쳐야 한다. 또 마지막 페이지를 다 쓰지 못해 남는 공간처럼 할당 단위 안에서 낭비되는 내부 단편화는 생길 수 있다.

**필수 개념**

- 가변 크기 할당의 외부 단편화 · 가중치 0.50: 크기가 다른 세그먼트를 할당하고 해제하면 빈 공간이 조각나 합은 충분해도 연속 공간이 없어 배치하지 못한다는 설명.
- 고정 크기 페이지와 페이지 테이블 · 가중치 0.50: 고정 크기 페이지는 연속 공간이 필요 없어 외부 단편화를 없애지만 주소 변환용 페이지 테이블이 필요하다는 설명.

**혼동 주의:** 페이징에서는 어떤 단편화도 생기지 않는다고 설명하지 않기.

**출처 대조 위치**

- [OSTEP 16: Segmentation](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-segmentation.pdf) — OSTEP 16: external fragmentation

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-07 · INTERMEDIATE

물리 메모리가 8GB인 컴퓨터에서 프로세스들이 사용하는 가상 메모리의 합이 12GB입니다. 그래도 프로그램이 동작하는 이유를 스왑 공간과 페이지 부재 처리 과정으로 설명하세요. 메모리가 계속 부족할 때 시스템이 극도로 느려지는 현상의 원인도 답하세요.

**모범 답안**

운영체제는 당장 쓰이지 않는 페이지를 디스크의 스왑 공간으로 내보내 물리 메모리보다 큰 가상 메모리를 지원한다. 페이지 테이블 항목에는 그 페이지가 물리 메모리에 있는지를 나타내는 존재 비트가 있다. 프로세스가 메모리에 없는 페이지에 접근하면 페이지 부재가 발생해 운영체제의 처리 코드로 제어가 넘어간다. 운영체제는 빈 프레임을 찾고, 없으면 다른 페이지를 골라 스왑 공간으로 내보낸 뒤, 필요한 페이지를 디스크에서 읽어 와 페이지 테이블을 갱신하고 중단된 명령을 다시 실행한다. 프로세스는 이 과정을 알지 못한 채 계속 실행된다. 디스크 접근은 메모리 접근보다 훨씬 느리므로, 필요한 페이지들이 메모리에 다 들어가지 못해 내보내기와 읽어 오기가 끊임없이 반복되면 대부분의 시간이 디스크 입출력에 쓰여 시스템이 극도로 느려진다.

**필수 개념**

- 스왑 공간과 페이지 부재 처리 · 가중치 0.50: 메모리에 없는 페이지 접근이 페이지 부재를 일으키고 운영체제가 디스크에서 읽어 와 실행을 재개한다는 과정.
- 과도한 페이지 교체의 비용 · 가중치 0.50: 필요한 페이지가 메모리에 다 들어가지 못해 교체가 반복되면 느린 디스크 입출력이 실행 시간을 지배한다는 설명.

**혼동 주의:** 페이지 부재를 프로그램 오류로 인한 비정상 종료와 같은 것으로 설명하지 않기.

**출처 대조 위치**

- [OSTEP 21: Beyond Physical Memory: Mechanisms](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-beyondphys.pdf) — 21.2 The Present Bit; swap space

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-08 · INTERMEDIATE

사용자 프로그램이 파일을 읽으려면 디스크 장치를 직접 다루는 대신 read() 시스템 콜을 호출해야 합니다. 프로세서의 사용자 모드와 커널 모드가 각각 무엇을 허용하는지 설명하세요. 시스템 콜을 호출했을 때 모드가 바뀌고 되돌아오는 과정과, 이 구조가 필요한 이유도 답하세요.

**모범 답안**

사용자 모드에서 실행되는 코드는 할 수 있는 일이 제한되어, 입출력 요청 같은 특권 명령을 직접 실행하려 하면 프로세서가 예외를 일으킨다. 커널 모드에서는 운영체제가 장치 접근을 포함한 모든 명령을 실행할 수 있다. 사용자 프로그램이 시스템 콜을 호출하면 트랩 명령이 실행되어 프로세서가 커널 모드로 바뀌고, 운영체제가 부팅 때 등록해 둔 처리 코드로 제어가 넘어간다. 이때 호출한 프로세스의 레지스터가 저장된다. 운영체제는 요청이 허용되는지 확인하고 작업을 수행한 뒤, 트랩 복귀 명령으로 저장한 상태를 복원하고 사용자 모드로 돌아간다. 이 구조가 없으면 어떤 프로그램이든 디스크 전체를 읽거나 다른 프로세스의 메모리를 건드릴 수 있어 보호가 성립하지 않는다.

**필수 개념**

- 모드에 따른 실행 권한 · 가중치 0.50: 사용자 모드는 특권 명령이 제한되고 커널 모드는 모든 명령을 실행할 수 있다는 구분.
- 트랩을 통한 제어된 진입 · 가중치 0.50: 시스템 콜은 트랩으로 커널 모드에 진입해 등록된 처리 코드를 실행하고 복귀하며 이것이 자원 보호를 가능하게 한다는 설명.

**혼동 주의:** 시스템 콜을 일반 함수 호출과 같은 비용과 방식으로 설명하지 않기.

**출처 대조 위치**

- [OSTEP 6: Mechanism: Limited Direct Execution](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-mechanisms.pdf) — 6.2 Problem #1: Restricted Operations

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-OS-09 · INTERMEDIATE

CPU 코어가 하나인 컴퓨터에서 프로세스 두 개가 동시에 실행되는 것처럼 보입니다. 운영체제가 실행 중인 프로세스로부터 CPU를 되찾는 수단과, 프로세스를 바꿀 때 저장하고 복원하는 것이 무엇인지 설명하세요. 전환이 너무 잦을 때 생기는 비용도 답하세요.

**모범 답안**

운영체제는 타이머 인터럽트로 CPU를 되찾는다. 일정 시간마다 타이머가 인터럽트를 일으키면 실행 중이던 프로세스가 멈추고 운영체제의 처리 코드가 실행된다. 이때 스케줄러가 다른 프로세스를 실행하기로 정하면 문맥 전환이 일어난다. 운영체제는 현재 프로세스의 레지스터 값과 프로그램 카운터, 스택 포인터를 그 프로세스의 커널 자료구조에 저장하고, 다음에 실행할 프로세스의 저장된 값을 복원한 뒤 그 프로세스로 복귀한다. 프로세스는 멈췄던 지점부터 이어서 실행된다. 전환 자체에 드는 저장과 복원 시간 외에, 전환 뒤에는 프로세서 캐시와 주소 변환 캐시에 이전 프로세스의 내용이 남아 있어 한동안 캐시 미스가 늘어난다. 전환이 너무 잦으면 실제 작업보다 전환에 쓰는 시간의 비중이 커진다.

**필수 개념**

- 타이머 인터럽트로 되찾는 제어 · 가중치 0.50: 운영체제는 주기적인 타이머 인터럽트로 실행 중인 프로세스로부터 CPU 제어를 되찾는다는 설명.
- 레지스터 저장·복원과 전환 비용 · 가중치 0.50: 문맥 전환은 레지스터 상태를 저장하고 복원하는 일이며 직접 비용과 캐시 효과로 인한 간접 비용이 있다는 설명.

**혼동 주의:** 문맥 전환의 비용을 레지스터 저장 시간만으로 설명하지 않기.

**출처 대조 위치**

- [OSTEP 6: Mechanism: Limited Direct Execution](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-mechanisms.pdf) — OSTEP 6: context switch; timer interrupt

근거 문서: `r1-operating-system`. 검수 상태: **PENDING**

### R1-NET-01 · BASIC

브라우저가 보낸 HTTP 요청은 TCP 세그먼트, IP 패킷, 이더넷 프레임으로 차례로 감싸여 전송됩니다. 인터넷 프로토콜 계층 구조에서 응용·전송·인터넷·링크 계층이 각각 맡는 일을 설명하세요. 계층을 나눠 두면 와이파이를 유선으로 바꿔도 애플리케이션을 고칠 필요가 없는 이유도 답하세요.

**모범 답안**

링크 계층은 같은 네트워크에 직접 연결된 장치 사이에서 프레임을 전달한다. 인터넷 계층은 IP 주소를 기준으로 여러 네트워크를 거쳐 목적지 호스트까지 패킷을 보내며 전달을 보장하지는 않는다. 전송 계층은 포트 번호로 호스트 안의 어느 프로그램에 줄지를 구분하고, TCP의 경우 순서와 재전송으로 신뢰성 있는 바이트 흐름을 제공한다. 응용 계층은 HTTP처럼 애플리케이션이 주고받는 메시지의 형식과 의미를 정한다. 보낼 때는 위 계층의 데이터에 아래 계층의 헤더를 덧붙여 감싸고, 받을 때는 아래에서부터 헤더를 벗겨 위로 올린다. 각 계층은 바로 아래 계층이 제공하는 서비스만 사용하고 그 내부 구현은 알지 못한다. 그래서 링크 계층이 와이파이에서 유선으로 바뀌어도 그 위의 IP, TCP, HTTP는 그대로 동작한다.

**필수 개념**

- 네 계층의 책임 구분 · 가중치 0.50: 링크는 직접 연결 구간 전달, 인터넷은 호스트 간 경로 전달, 전송은 프로그램 간 전달, 응용은 메시지 의미를 맡는다는 구분.
- 캡슐화와 계층 독립성 · 가중치 0.50: 위 계층 데이터에 아래 계층 헤더를 덧붙여 감싸며 아래 계층의 구현이 바뀌어도 위 계층은 영향받지 않는다는 설명.

**혼동 주의:** OSI 7계층의 이름을 나열하는 것으로 답을 대신하지 않기.

**출처 대조 위치**

- [RFC 1122 — Requirements for Internet Hosts: Communication Layers](https://www.rfc-editor.org/rfc/rfc1122.html) — 1.1.3 Internet Protocol Suite

근거 문서: `r1-networks`. 검수 상태: **PENDING**

### R1-NET-02 · BASIC

파일 전송 프로그램과 실시간 음성 통화 프로그램을 만듭니다. 파일은 한 바이트도 빠지거나 순서가 바뀌면 안 되고, 음성은 늦게 도착한 조각이 쓸모없습니다. 각각 TCP와 UDP 중 무엇이 맞는지, 두 프로토콜이 제공하는 것과 제공하지 않는 것으로 설명하세요.

**모범 답안**

파일 전송에는 TCP가 맞다. TCP는 연결을 맺은 뒤 바이트에 순서 번호를 붙이고, 받은 쪽의 확인 응답이 없으면 재전송하며, 순서가 바뀌어 도착한 데이터를 다시 맞춰 애플리케이션에 순서대로 넘긴다. 애플리케이션은 유실과 순서를 신경 쓰지 않아도 된다. 그 대가로 유실된 조각이 다시 올 때까지 뒤의 데이터가 전달되지 않아 지연이 생길 수 있다. UDP는 연결 없이 데이터그램을 보낼 뿐 전달과 순서, 중복 방지를 보장하지 않는다. 음성 통화에서는 늦게 온 조각을 기다리느니 버리고 다음 조각을 재생하는 편이 나으므로, 재전송으로 지연을 만드는 TCP보다 UDP가 맞다. 유실을 감추거나 순서를 판단하는 일은 애플리케이션이 필요한 만큼만 직접 구현한다.

**필수 개념**

- TCP의 순서 보장과 재전송 · 가중치 0.50: TCP는 순서 번호와 확인 응답, 재전송으로 유실 없이 순서대로 전달하며 그 대가로 지연이 생길 수 있다는 설명.
- UDP의 최소 기능과 적합한 용도 · 가중치 0.50: UDP는 전달과 순서를 보장하지 않아 늦은 데이터가 쓸모없는 실시간 통신에 맞고 필요한 처리는 애플리케이션이 맡는다는 설명.

**혼동 주의:** UDP를 ‘TCP보다 빠른 프로토콜’로만 설명하지 않기.

**출처 대조 위치**

- [RFC 768 — User Datagram Protocol](https://www.rfc-editor.org/rfc/rfc768.html) — User Datagram Protocol: minimum of protocol mechanism

근거 문서: `r1-networks`. 검수 상태: **PENDING**

### R1-NET-03 · INTERMEDIATE

TCP 연결을 맺을 때 클라이언트와 서버는 SYN, SYN+ACK, ACK 세 개의 세그먼트를 주고받습니다. 각 세그먼트가 상대에게 알려 주는 것과 확인해 주는 것을 설명하세요. 두 번의 교환만으로는 왜 부족한지도 초기 순서 번호의 관점에서 답하세요.

**모범 답안**

클라이언트는 SYN으로 연결 요청과 함께 자신이 사용할 초기 순서 번호를 알린다. 서버는 SYN+ACK로 클라이언트의 순서 번호를 받았음을 확인하면서 자신의 초기 순서 번호를 알린다. 클라이언트는 마지막 ACK로 서버의 순서 번호를 받았음을 확인한다. TCP는 양방향으로 데이터를 보내므로 양쪽이 각자의 초기 순서 번호를 알리고 상대의 확인을 받아야 하며, 이를 위해 최소 세 번의 교환이 필요하다. 두 번에서 멈추면 서버는 자신의 순서 번호가 클라이언트에 도착했는지 알 수 없다. 또 네트워크에 남아 있다가 뒤늦게 도착한 옛 SYN을 서버가 새 연결 요청으로 받아들였을 때, 세 번째 확인이 오지 않으므로 잘못된 연결이 성립하는 것을 막을 수 있다.

**필수 개념**

- 양방향 순서 번호의 통지와 확인 · 가중치 0.50: SYN과 SYN+ACK, ACK로 양쪽이 초기 순서 번호를 알리고 상대의 수신 확인을 받는다는 설명.
- 두 번으로 부족한 이유 · 가중치 0.50: 두 번의 교환으로는 서버가 자신의 순서 번호 도착을 확인할 수 없고 지연된 옛 SYN에 의한 잘못된 연결을 막지 못한다는 설명.

**혼동 주의:** 핸드셰이크의 목적을 ‘서버가 켜져 있는지 확인하는 것’으로만 설명하지 않기.

**출처 대조 위치**

- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — 3.5 Establishing a Connection; three-way handshake

근거 문서: `r1-networks`. 검수 상태: **PENDING**

### R1-NET-04 · INTERMEDIATE

TCP 연결을 끝낼 때 한쪽이 FIN을 보낸 뒤에도 상대는 남은 데이터를 계속 보낼 수 있습니다. 연결 종료에 FIN과 ACK가 양쪽에서 각각 필요한 이유를 설명하세요. 먼저 종료를 시작한 쪽이 마지막 ACK를 보낸 뒤 곧바로 닫지 않고 일정 시간 기다리는 이유도 답하세요.

**모범 답안**

TCP 연결은 양방향이고 각 방향이 독립적으로 닫힌다. FIN은 보내는 쪽이 더 보낼 데이터가 없다는 뜻일 뿐, 받는 일까지 끝낸다는 뜻이 아니다. 그래서 한쪽이 FIN을 보내고 상대가 ACK로 확인한 뒤에도 상대는 자신의 데이터를 마저 보낼 수 있고, 이 상태를 절반만 닫힌 연결이라 한다. 상대도 보낼 것이 없어지면 FIN을 보내고, 처음 쪽이 ACK로 확인해 양방향이 모두 닫힌다. 먼저 종료를 시작한 쪽은 마지막 ACK를 보낸 뒤 일정 시간 대기 상태에 머문다. 마지막 ACK가 유실되면 상대가 FIN을 다시 보내는데, 이때 응답할 수 있어야 상대가 정상적으로 닫힌다. 또 이전 연결의 지연된 세그먼트가 같은 주소와 포트 조합의 새 연결에 섞여 들어가는 것을 막는다.

**필수 개념**

- 방향별로 독립적인 종료 · 가중치 0.50: FIN은 한 방향의 송신 종료만 뜻하므로 양쪽이 각각 FIN을 보내고 ACK를 받아야 연결이 완전히 닫힌다는 설명.
- 마지막 확인 뒤의 대기 · 가중치 0.50: 마지막 ACK 유실 시 재전송된 FIN에 응답하고 옛 세그먼트가 새 연결에 섞이지 않도록 일정 시간 기다린다는 설명.

**혼동 주의:** FIN을 보내면 그 즉시 양방향 통신이 끝난다고 설명하지 않기.

**출처 대조 위치**

- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — 3.6 Closing a Connection; 3.6.1 Half-Closed Connections

근거 문서: `r1-networks`. 검수 상태: **PENDING**

### R1-NET-05 · INTERMEDIATE

브라우저 주소창에 https://www.example.com/orders를 입력하고 Enter를 눌렀습니다. 서버의 응답이 도착하기까지 이름 해석, 연결 수립, 요청 전송의 세 단계에서 각각 어떤 일이 일어나는지 순서대로 설명하세요. 같은 사이트에 두 번째로 접속할 때 생략되거나 줄어들 수 있는 단계도 답하세요.

**모범 답안**

먼저 브라우저는 호스트 이름 www.example.com의 IP 주소를 알아낸다. 브라우저와 운영체제의 캐시에 없으면 리졸버가 DNS 서버에 질의하고, 리졸버는 루트부터 해당 도메인의 권한 있는 네임 서버까지 차례로 물어 주소를 받아 온다. 다음으로 그 IP 주소의 443번 포트로 TCP 연결을 맺고, 그 위에서 TLS 핸드셰이크로 서버 인증서를 검증하고 암호화 키를 합의한다. 그 뒤 암호화된 연결로 GET /orders 요청과 헤더를 보내면 서버가 상태 코드와 본문이 담긴 응답을 돌려준다. 두 번째 접속에서는 DNS 응답이 유효 시간 동안 캐시에 남아 있어 이름 해석이 생략되고, 연결이 유지되고 있으면 연결 수립도 건너뛰어 요청만 보낸다. 응답이 캐시에 저장되어 있고 아직 유효하면 요청 자체를 보내지 않을 수도 있다.

**필수 개념**

- 이름 해석·연결·요청의 순서 · 가중치 0.50: DNS로 IP 주소를 얻고 TCP와 TLS 연결을 맺은 뒤 HTTP 요청을 보내 응답을 받는다는 순서.
- 캐시와 연결 재사용으로 줄어드는 단계 · 가중치 0.50: DNS 캐시와 유지된 연결, HTTP 캐시 덕분에 재접속 시 이름 해석과 연결 수립, 요청이 생략될 수 있다는 설명.

**혼동 주의:** DNS 조회가 요청을 보낼 때마다 매번 일어난다고 설명하지 않기.

**출처 대조 위치**

- [RFC 1034 — Domain Names: Concepts and Facilities](https://www.rfc-editor.org/rfc/rfc1034.html) — Domain Names: RESOLVERS are programs that extract information from name servers

근거 문서: `r1-networks`. 검수 상태: **PENDING**

### R1-NET-06 · INTERMEDIATE

서버 한 대의 443번 포트 하나에 클라이언트 1만 명이 동시에 연결되어 있습니다. 서버는 도착한 세그먼트가 어느 연결의 것인지 어떻게 구분하나요? 포트가 하는 역할과 TCP 연결을 식별하는 값의 조합을 설명하고, 같은 클라이언트가 같은 서버에 연결 두 개를 열 수 있는 이유도 답하세요.

**모범 답안**

IP 주소는 호스트를 찾는 데 쓰이고, 포트는 그 호스트 안에서 데이터를 받을 프로그램을 구분한다. TCP 연결 하나는 포트 하나로 식별되는 것이 아니라 양 끝의 소켓 쌍, 곧 출발지 IP와 포트, 목적지 IP와 포트의 조합으로 식별된다. 서버 쪽 주소와 포트가 모두 같아도 클라이언트마다 IP 주소나 출발지 포트가 다르므로 조합은 서로 다르다. 서버는 도착한 세그먼트의 네 값을 보고 해당 연결을 찾는다. 같은 클라이언트가 같은 서버에 연결을 두 개 여는 경우에도 운영체제가 연결마다 다른 출발지 포트를 임시로 배정하므로 구분된다. 따라서 서버가 받을 수 있는 동시 연결 수는 포트 번호의 개수가 아니라 메모리나 파일 디스크립터 같은 자원으로 제한된다.

**필수 개념**

- 호스트 안의 프로그램을 구분하는 포트 · 가중치 0.50: IP 주소는 호스트를, 포트는 그 안에서 데이터를 받을 프로그램을 식별한다는 역할 구분.
- 소켓 쌍으로 식별되는 연결 · 가중치 0.50: TCP 연결은 양 끝의 IP와 포트 조합으로 식별되어 같은 서버 포트에 많은 연결이 공존한다는 설명.

**혼동 주의:** 서버 포트 하나에는 연결을 하나만 맺을 수 있다고 설명하지 않기.

**출처 대조 위치**

- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html) — Transmission Control Protocol: A connection is defined by a pair of sockets

근거 문서: `r1-networks`. 검수 상태: **PENDING**

### R1-HTTP-01 · BASIC

쇼핑몰 서버를 두 대로 늘리고 요청을 번갈아 보냈더니, 장바구니에 담은 상품이 다음 요청에서 보이지 않는 일이 생겼습니다. 장바구니를 각 서버의 메모리에 저장하고 있었습니다. HTTP가 무상태 프로토콜이라는 말의 뜻으로 원인을 설명하고, 서버를 늘려도 문제가 없게 하는 방법을 답하세요.

**모범 답안**

HTTP가 무상태라는 것은 각 요청 메시지를 그 자체만으로 이해할 수 있어야 하고, 서버가 이전 요청을 기억한다고 프로토콜이 전제하지 않는다는 뜻이다. 장바구니를 한 서버의 메모리에 두면 그 내용은 그 서버만 안다. 다음 요청이 다른 서버로 가면 그 서버에는 정보가 없으므로 장바구니가 빈 것처럼 보인다. 해결하려면 요청을 처리하는 데 필요한 상태를 특정 서버의 메모리에 두지 않아야 한다. 장바구니를 데이터베이스나 공유 세션 저장소에 두고 요청마다 식별자로 조회하거나, 필요한 정보를 요청에 실어 보내게 한다. 그러면 어느 서버가 요청을 받아도 같은 결과를 낼 수 있어 서버를 자유롭게 늘리고 교체할 수 있다. 같은 사용자를 항상 같은 서버로 보내는 방법은 그 서버가 죽으면 상태를 잃는다.

**필수 개념**

- 요청 단위로 완결되는 무상태성 · 가중치 0.50: 각 요청이 그 자체로 이해될 수 있어야 하며 서버가 이전 요청을 기억한다고 전제하지 않는다는 뜻.
- 상태의 외부 저장과 수평 확장 · 가중치 0.50: 필요한 상태를 공유 저장소나 요청에 두면 어느 서버가 받아도 처리할 수 있어 서버를 늘릴 수 있다는 설명.

**혼동 주의:** 무상태를 ‘서버가 어떤 데이터도 저장하지 않는다’는 뜻으로 설명하지 않기.

**출처 대조 위치**

- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — HTTP Semantics: HTTP is defined as a stateless protocol

근거 문서: `r1-http`. 검수 상태: **PENDING**

### R1-HTTP-02 · BASIC

회원 탈퇴 기능을 GET /members/7/delete 링크로 만들었더니, 검색 엔진 수집기가 링크를 따라가면서 회원이 삭제됐습니다. GET이 약속하는 성질로 이 사고의 원인을 설명하세요. 상태를 바꾸는 요청에 GET을 쓰면 안 되는 이유와, 요청 데이터가 놓이는 위치에서 GET과 POST가 어떻게 다른지도 답하세요.

**모범 답안**

GET은 안전한 메서드로 정의되어 있다. 안전하다는 것은 요청의 의미가 읽기 전용이어서 클라이언트가 서버의 상태 변경을 요청하거나 기대하지 않는다는 뜻이다. 수집기와 브라우저의 미리 읽기, 캐시는 이 약속을 믿고 GET 요청을 사용자의 의사와 무관하게 자동으로 보내거나 반복한다. 삭제를 GET으로 구현하면 이런 자동 요청이 실제 삭제를 일으킨다. 상태를 바꾸는 요청은 POST나 DELETE 같은 안전하지 않은 메서드로 받아야 중간 장치와 클라이언트가 함부로 반복하지 않는다. 데이터의 위치도 다르다. GET은 조건을 URI의 쿼리에 실으므로 주소창과 방문 기록, 서버 로그에 남고 길이에도 제약이 있다. POST는 데이터를 요청 본문에 실어 보낸다. 다만 본문에 실었다고 암호화되는 것은 아니므로 보호는 HTTPS가 맡는다.

**필수 개념**

- 안전한 메서드라는 약속 · 가중치 0.50: GET은 읽기 전용 의미의 안전한 메서드여서 수집기와 캐시가 자동으로 보내고 반복할 수 있다는 설명.
- 상태 변경과 데이터 위치의 차이 · 가중치 0.50: 상태 변경은 안전하지 않은 메서드로 받아야 하며 GET은 URI에, POST는 본문에 데이터를 싣는다는 차이.

**혼동 주의:** GET과 POST의 차이를 ‘POST가 더 안전하게 암호화된다’로 설명하지 않기.

**출처 대조 위치**

- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — 9.2.1 Safe Methods

근거 문서: `r1-http`. 검수 상태: **PENDING**

### R1-HTTP-03 · INTERMEDIATE

주문 API가 잘못된 입력에도, 서버 내부 오류에도 항상 200 OK를 반환하고 본문의 success 필드로 성공 여부를 알립니다. 이 설계가 클라이언트와 모니터링에 주는 문제를 설명하세요. 4xx와 5xx 상태 코드가 각각 누구의 문제를 뜻하는지와, 클라이언트의 재시도 판단에 어떻게 쓰이는지도 답하세요.

**모범 답안**

상태 코드는 응답의 결과를 표준화된 방식으로 알리는 값이다. 항상 200을 반환하면 캐시와 프록시, 모니터링 도구, HTTP 클라이언트 라이브러리가 실패를 성공으로 취급한다. 실패한 응답이 캐시에 저장될 수 있고, 오류율 지표는 0으로 보이며, 클라이언트는 응용마다 다른 본문 형식을 해석해야만 실패를 알 수 있다. 4xx는 요청에 문제가 있다는 뜻이다. 같은 요청을 그대로 다시 보내도 같은 결과가 나오므로 클라이언트는 요청을 고쳐야 한다. 5xx는 요청은 유효해 보이지만 서버가 처리에 실패했다는 뜻이다. 일시적인 원인일 수 있어 시간을 두고 재시도해 볼 수 있으나, 재시도해도 안전한 요청인지는 메서드의 멱등성에 따라 따로 판단해야 한다.

**필수 개념**

- 상태 코드로 드러나는 결과 · 가중치 0.50: 항상 200을 반환하면 캐시·모니터링·클라이언트가 실패를 성공으로 취급하게 된다는 문제.
- 4xx와 5xx의 책임과 재시도 · 가중치 0.50: 4xx는 요청의 문제라 고쳐야 하고 5xx는 서버의 실패라 재시도 여지가 있으나 멱등성을 따로 판단한다는 설명.

**혼동 주의:** 5xx 응답은 언제나 그대로 재시도해도 된다고 설명하지 않기.

**출처 대조 위치**

- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html) — 15.5 Client Error 4xx; 15.6 Server Error 5xx

근거 문서: `r1-http`. 검수 상태: **PENDING**

### R1-HTTP-04 · INTERMEDIATE

로그인에 성공한 사용자를 다음 요청에서도 알아보려 합니다. 서버가 Set-Cookie로 값을 내려 주면 브라우저가 이후 요청에서 무엇을 하는지 설명하세요. 쿠키에 회원 번호를 그대로 담는 방식과, 추측하기 어려운 세션 식별자만 담고 회원 정보는 서버에 두는 방식을 비교하세요.

**모범 답안**

서버가 응답의 Set-Cookie 헤더로 이름과 값을 보내면 브라우저는 이를 저장해 두고, 이후 같은 사이트로 보내는 요청마다 Cookie 헤더에 그 값을 실어 보낸다. 서버는 요청마다 실려 오는 값으로 이전 요청과의 관계를 알아낸다. 쿠키에 회원 번호를 그대로 담으면 쿠키는 클라이언트가 보관하고 마음대로 바꿀 수 있는 값이므로, 번호를 다른 사람의 것으로 바꿔 보내는 것만으로 그 사람 행세를 할 수 있다. 세션 방식은 쿠키에 의미 없는 무작위 식별자만 담고, 그 식별자에 대응하는 회원 정보는 서버가 보관한다. 값을 바꿔도 유효한 식별자를 맞힐 수 없고, 서버가 세션을 지워 즉시 로그아웃시킬 수도 있다. 대신 서버가 세션 저장소를 유지해야 하고, 식별자를 탈취당하지 않도록 쿠키에 Secure와 HttpOnly 속성을 붙인다.

**필수 개념**

- 쿠키의 저장과 자동 전송 · 가중치 0.50: 서버가 Set-Cookie로 내린 값을 브라우저가 저장해 이후 요청의 Cookie 헤더에 실어 보낸다는 동작.
- 변조 가능한 쿠키 값과 세션 식별자 · 가중치 0.50: 쿠키 값은 클라이언트가 바꿀 수 있어 의미 있는 값 대신 추측 불가능한 식별자만 담고 정보는 서버에 둔다는 설명.

**혼동 주의:** 쿠키와 세션을 서로 대체하는 두 가지 저장소로만 설명하지 않기.

**출처 대조 위치**

- [RFC 6265 — HTTP State Management Mechanism](https://www.rfc-editor.org/rfc/rfc6265.html) — 8.4 Session Identifiers; state information

근거 문서: `r1-http`. 검수 상태: **PENDING**

### R1-HTTP-05 · INTERMEDIATE

한 페이지가 이미지 60개를 불러옵니다. HTTP/1.1에서 브라우저는 서버 하나에 연결을 여러 개 열어 나눠 받습니다. 연결 하나에서 응답이 요청 순서대로만 돌아와야 하는 제약이 어떤 지연을 만드는지 설명하세요. HTTP/2가 연결 하나로 이 문제를 줄이는 방식과 그래도 남는 한계를 답하세요.

**모범 답안**

HTTP/1.1은 연결을 재사용할 수 있지만, 한 연결에서 응답은 요청을 받은 순서대로 돌려보내야 한다. 앞선 요청의 응답이 느리면 뒤의 요청들은 이미 준비됐더라도 그 뒤에서 기다리게 되며, 이를 응용 계층의 선두 차단이라 한다. 브라우저는 이를 피하려고 서버마다 연결을 여러 개 열지만, 연결마다 수립 비용이 들고 수에도 한계가 있다. HTTP/2는 한 연결 안에 스트림이라는 독립된 흐름을 여러 개 두고, 각 요청과 응답을 프레임으로 잘라 서로 섞어 보낸다. 프레임마다 스트림 식별자가 있어 받는 쪽이 다시 조립하므로, 느린 응답이 다른 응답을 막지 않는다. 다만 모든 스트림이 TCP 연결 하나를 공유하므로, 패킷 하나가 유실되면 TCP가 재전송을 기다리는 동안 모든 스트림이 함께 멈추는 전송 계층의 선두 차단은 남는다.

**필수 개념**

- 순서대로 응답해야 하는 제약 · 가중치 0.50: HTTP/1.1의 한 연결에서는 응답이 요청 순서를 따라야 해 느린 응답이 뒤의 응답을 막는다는 설명.
- 스트림 다중화와 남는 TCP 한계 · 가중치 0.50: HTTP/2는 한 연결에서 스트림별 프레임을 섞어 보내 이를 줄이지만 TCP 유실 시 모든 스트림이 멈춘다는 한계.

**혼동 주의:** HTTP/2를 쓰면 선두 차단이 완전히 사라진다고 설명하지 않기.

**출처 대조 위치**

- [RFC 9113 — HTTP/2](https://www.rfc-editor.org/rfc/rfc9113.html) — 5 Streams and Multiplexing; application-layer head-of-line blocking

근거 문서: `r1-http`. 검수 상태: **PENDING**

### R1-TEST-01 · INTERMEDIATE

주문 서비스 테스트에서 메일 발송기를 대역으로 바꿉니다. 한 테스트는 대역이 정해진 값을 돌려주게만 하고 주문 결과를 검증합니다. 다른 테스트는 ‘발송 메서드가 정확히 한 번, 이 인자로 호출됐는가’를 검증합니다. 두 대역의 종류와 검증 방식의 차이를 설명하고, 뒤의 방식에 지나치게 의존할 때의 문제를 답하세요.

**모범 답안**

앞의 대역은 스텁이다. 스텁은 테스트 중의 호출에 미리 준비한 답을 돌려줄 뿐이고, 테스트는 실행이 끝난 뒤 대상 객체나 협력 객체의 상태를 확인해 결과를 검증한다. 이를 상태 검증이라 한다. 뒤의 대역은 목이다. 목에는 어떤 호출이 어떤 인자로 몇 번 와야 하는지 기대를 먼저 설정하고, 실제 호출이 기대와 맞는지를 검증한다. 이를 행위 검증이라 한다. 메일 발송처럼 결과로 남는 상태가 없고 호출 자체가 요구사항인 경우에는 행위 검증이 맞다. 그러나 모든 협력을 행위로 검증하면 테스트가 대상의 구현 방식에 묶인다. 결과는 같은데 호출 순서나 횟수만 바꾸는 리팩터링에도 테스트가 깨지고, 반대로 호출은 맞지만 결과가 틀린 버그는 놓칠 수 있다.

**필수 개념**

- 스텁의 상태 검증과 목의 행위 검증 · 가중치 0.50: 스텁은 준비된 답을 주고 결과 상태를 검증하며 목은 기대한 호출이 일어났는지를 검증한다는 차이.
- 행위 검증 과용과 구현 결합 · 가중치 0.50: 호출 방식까지 검증하면 테스트가 구현에 묶여 리팩터링에 깨지고 결과 오류를 놓칠 수 있다는 문제.

**혼동 주의:** 테스트 대역을 모두 ‘목’이라 부르며 같은 것으로 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — Mocks Aren't Stubs](https://martinfowler.com/articles/mocksArentStubs.html) — Mocks Aren't Stubs: state verification; behavior verification

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-TEST-02 · INTERMEDIATE

JaCoCo 리포트에서 한 메서드의 라인 커버리지는 100%인데 분기 커버리지는 50%로 나왔습니다. 그 메서드에는 else가 없는 if 문이 하나 있습니다. 두 지표가 각각 무엇을 세는지 설명하고, 이런 차이가 나는 테스트가 어떤 경우를 실행하지 않았는지 답하세요. 도구가 실행 여부를 알아내는 방식도 설명하세요.

**모범 답안**

라인 커버리지는 실행된 소스 줄의 비율이고, 분기 커버리지는 if나 switch의 각 갈래 가운데 실행된 갈래의 비율이다. else가 없는 if 문에서 조건이 참인 경우만 테스트하면 if 줄과 그 안의 줄이 모두 실행되어 라인 커버리지는 100%가 된다. 그러나 조건이 거짓이어서 본문을 건너뛰는 갈래는 한 번도 실행되지 않았으므로 두 갈래 중 하나만 실행되어 분기 커버리지는 50%다. 테스트는 조건이 거짓인 입력을 다루지 않은 것이다. JaCoCo는 클래스의 바이트코드에 실행 여부를 기록하는 탐침을 끼워 넣고, 테스트가 실행되는 동안 어느 탐침이 지나갔는지 모아 리포트를 만든다. 소스가 아니라 바이트코드를 기준으로 하므로 컴파일러가 만들어 낸 분기가 지표에 나타나기도 한다.

**필수 개념**

- 라인과 분기 지표의 차이 · 가중치 0.50: 라인 커버리지는 실행된 줄을, 분기 커버리지는 조건의 각 갈래 실행 여부를 세어 else 없는 if의 거짓 갈래 누락을 드러낸다는 설명.
- 바이트코드 탐침에 의한 측정 · 가중치 0.50: 도구가 바이트코드에 탐침을 삽입해 실행 중 지나간 지점을 기록하는 방식으로 측정한다는 설명.

**혼동 주의:** 라인 커버리지 100%를 모든 경우를 테스트했다는 뜻으로 설명하지 않기.

**출처 대조 위치**

- [JaCoCo Documentation — Coverage Counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html) — Coverage Counters: Branches (C1 Coverage)

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-TEST-03 · INTERMEDIATE

팀이 ‘커버리지 90% 미만이면 병합 금지’ 규칙을 도입하자, 검증문이 없는 테스트와 getter만 호출하는 테스트가 늘어났습니다. 커버리지 수치가 알려 주는 것과 알려 주지 못하는 것을 구분해 설명하세요. 이 지표를 어떻게 쓰는 것이 적절한지도 답하세요.

**모범 답안**

커버리지는 테스트가 실행하는 동안 지나간 코드의 비율이다. 수치가 낮은 부분은 테스트가 한 번도 실행하지 않은 코드이므로, 테스트가 빠진 곳을 찾는 데에는 확실한 근거가 된다. 그러나 수치가 높다고 해서 테스트가 좋다는 뜻은 아니다. 코드를 실행하기만 하고 결과를 검증하지 않는 테스트도 커버리지를 올리며, 실행된 줄이 올바르게 동작하는지는 수치에 드러나지 않는다. 수치를 목표로 강제하면 사람들은 목표를 채우기 쉬운 테스트를 쓰게 되어, 지표는 오르지만 결함을 잡는 능력은 그대로다. 그래서 커버리지는 통과 기준보다는 테스트되지 않은 영역을 찾아 검토하는 도구로 쓴다. 테스트가 충분한지는 결함이 운영에 얼마나 새어 나가는지와 코드를 고칠 때 망설임이 없는지로 판단한다.

**필수 개념**

- 미실행 코드를 찾는 지표 · 가중치 0.50: 커버리지는 테스트가 실행하지 않은 코드를 찾는 근거가 되지만 높은 수치가 검증 품질을 보장하지 않는다는 구분.
- 목표로 삼을 때의 왜곡 · 가중치 0.50: 수치를 강제 목표로 두면 검증 없는 테스트가 늘어 지표만 오르므로 검토 도구로 써야 한다는 설명.

**혼동 주의:** 커버리지가 높으면 버그가 없다는 뜻이라고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — TestCoverage](https://martinfowler.com/bliki/TestCoverage.html) — TestCoverage: 100% coverage

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-TEST-04 · BASIC

할인 금액을 계산하는 기능을 테스트 주도 개발로 만들려 합니다. 실패하는 테스트 작성, 통과시키기, 리팩터링의 세 단계에서 각각 무엇을 하고 무엇을 하지 않는지 설명하세요. 처음에 테스트가 실패하는 것을 직접 확인해야 하는 이유와, 리팩터링 단계를 건너뛸 때 생기는 문제도 답하세요.

**모범 답안**

첫 단계에서는 다음에 추가할 동작 하나를 정하고 그 동작을 요구하는 테스트를 먼저 쓴다. 아직 구현이 없으므로 테스트는 실패해야 한다. 실패를 직접 확인하지 않으면 테스트가 아무것도 검증하지 못해 항상 통과하는 것인지 알 수 없다. 둘째 단계에서는 그 테스트를 통과시키는 데 필요한 만큼만 구현하고, 아직 테스트가 요구하지 않는 기능은 넣지 않는다. 셋째 단계에서는 테스트가 통과하는 상태를 유지하면서 중복을 없애고 이름과 구조를 다듬는다. 이때 동작은 바꾸지 않는다. 리팩터링을 건너뛰면 통과에만 맞춘 코드 조각이 쌓여 구조가 빠르게 나빠진다. 테스트가 있어 고치기 안전한 바로 그 시점에 정리하는 것이 이 방식의 핵심이다.

**필수 개념**

- 실패 확인 후 최소 구현 · 가중치 0.50: 동작 하나를 요구하는 테스트를 먼저 써 실패를 확인하고 통과에 필요한 만큼만 구현한다는 절차.
- 통과 상태에서의 리팩터링 · 가중치 0.50: 테스트가 통과하는 상태에서 동작을 바꾸지 않고 구조를 정리하며 이를 생략하면 코드가 지저분하게 쌓인다는 설명.

**혼동 주의:** TDD를 ‘구현을 끝낸 뒤 테스트를 많이 쓰는 것’으로 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — TestDrivenDevelopment](https://martinfowler.com/bliki/TestDrivenDevelopment.html) — TestDrivenDevelopment: Write a test for the next bit of functionality

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-TEST-05 · ADVANCED

테스트가 없는 정산 메서드가 내부에서 new로 외부 결제 클라이언트를 직접 만들어 호출합니다. 이 메서드를 고치기 전에 테스트를 붙이고 싶지만, 테스트에서 실제 결제가 일어나면 안 됩니다. 이음새라는 개념으로 이 코드의 문제를 설명하고, 동작을 바꾸지 않으면서 대역을 끼워 넣을 수 있게 만드는 방법을 답하세요.

**모범 답안**

이음새는 그 자리의 코드를 직접 고치지 않고도 프로그램의 동작을 바꿀 수 있는 지점이다. 메서드가 안에서 결제 클라이언트를 직접 생성하면 테스트가 그 호출을 다른 것으로 바꿔 끼울 지점이 없다. 테스트를 실행하면 실제 결제가 일어나므로 테스트를 쓸 수 없고, 테스트가 없으니 안전하게 고칠 수도 없는 막힌 상태가 된다. 먼저 동작을 바꾸지 않는 최소한의 변경으로 이음새를 만든다. 결제 클라이언트를 생성하는 부분을 생성자 매개변수나 재정의할 수 있는 메서드로 빼내, 기본값은 기존과 같은 실제 객체로 두고 테스트에서는 대역을 넘길 수 있게 한다. 그런 다음 현재 동작을 그대로 기록하는 테스트를 붙이고, 그 테스트의 보호 아래에서 원래 하려던 수정을 진행한다.

**필수 개념**

- 대체 지점이 없는 직접 생성 · 가중치 0.50: 메서드 안에서 의존 객체를 직접 생성하면 테스트가 호출을 바꿔 끼울 이음새가 없어 테스트를 붙일 수 없다는 설명.
- 동작 보존 변경으로 이음새 만들기 · 가중치 0.50: 생성 지점을 매개변수나 재정의 가능한 메서드로 빼 대역을 넣고 현재 동작을 고정한 뒤 수정한다는 절차.

**혼동 주의:** 레거시 코드는 먼저 전부 다시 쓴 뒤에 테스트를 붙여야 한다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — LegacySeam](https://martinfowler.com/bliki/LegacySeam.html) — LegacySeam: without editing in that place

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-TEST-06 · INTERMEDIATE

어떤 테스트가 단독으로 실행하면 통과하는데 전체 테스트를 함께 실행하면 가끔 실패합니다. 실행 순서를 바꾸면 결과가 달라집니다. 테스트 사이의 격리가 깨졌을 때 나타나는 이 증상의 원인을 설명하고, 격리를 되찾는 두 가지 접근을 비교하세요.

**모범 답안**

테스트들이 데이터베이스 행이나 static 필드, 파일 같은 상태를 공유하고 있고, 앞서 실행된 테스트가 남긴 상태가 뒤 테스트의 전제를 바꾸기 때문이다. 단독 실행에서는 깨끗한 상태에서 시작하므로 통과하지만, 함께 실행하면 어떤 테스트가 먼저 돌았느냐에 따라 결과가 달라진다. 테스트 프레임워크는 실행 순서를 보장하지 않는 경우가 많아 실패가 간헐적으로 보인다. 격리를 되찾는 한 방법은 각 테스트가 필요한 상태를 매번 처음부터 새로 만드는 것이다. 가장 확실하지만 준비 비용이 커질 수 있다. 다른 방법은 각 테스트가 끝날 때 자신이 바꾼 상태를 원래대로 정리하는 것이다. 빠르지만 정리 코드에 빠진 부분이 있으면 그 테스트가 아니라 다음 테스트가 실패하므로 원인을 찾기 어렵다. 어느 쪽이든 테스트가 다른 테스트의 결과물에 기대지 않게 한다.

**필수 개념**

- 공유 상태가 만드는 순서 의존 · 가중치 0.50: 테스트가 상태를 공유하면 앞선 테스트가 남긴 상태에 따라 결과가 달라져 순서에 의존하는 간헐적 실패가 생긴다는 설명.
- 매번 새로 만들기와 사후 정리 · 가중치 0.50: 매 테스트가 상태를 새로 만드는 방법은 확실하지만 느리고 사후 정리는 빠르지만 누락 시 다른 테스트가 실패한다는 비교.

**혼동 주의:** 간헐적으로 실패하는 테스트를 재실행으로 통과시키면 해결됐다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — Eradicating Non-Determinism in Tests](https://martinfowler.com/articles/nonDeterminism.html) — Eradicating Non-Determinism in Tests: Lack of Isolation

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-TEST-07 · INTERMEDIATE

‘쿠폰은 발급일로부터 30일 동안 유효하다’는 규칙의 테스트가 코드 안의 LocalDateTime.now()에 의존합니다. 이 테스트는 자정 무렵이나 월말에 가끔 실패합니다. 시스템 시계를 직접 읽는 코드가 테스트를 불안정하게 만드는 이유를 설명하고, 시간을 테스트가 제어할 수 있게 만드는 설계를 답하세요.

**모범 답안**

시스템 시계는 호출할 때마다 다른 값을 돌려주는 입력이다. 대상 코드가 시계를 직접 읽으면 테스트는 그 입력을 정할 수 없어 실행 시각에 따라 결과가 달라진다. 테스트가 기대값을 만들 때 읽은 시각과 대상 코드가 읽은 시각 사이에 날짜가 바뀌면 실패하고, 만료 직전이나 직후 같은 경계 상황은 재현할 수조차 없다. 해결은 현재 시각을 코드가 스스로 구하지 않고 밖에서 받게 하는 것이다. 시각을 메서드 인자로 넘기거나, 생성자로 Clock을 주입받아 그 시계에서 시각을 읽는다. 운영에서는 시스템 시계를, 테스트에서는 고정된 시각을 돌려주는 시계를 넣는다. 그러면 발급 후 정확히 30일이 되는 순간과 그 1초 뒤를 원하는 대로 만들어 검증할 수 있다.

**필수 개념**

- 제어할 수 없는 입력인 시계 · 가중치 0.50: 코드가 시스템 시계를 직접 읽으면 테스트가 입력을 정할 수 없어 실행 시각에 따라 결과가 달라진다는 설명.
- 시각을 밖에서 주입받는 설계 · 가중치 0.50: 시각을 인자나 주입된 시계에서 얻게 해 테스트가 고정된 시각으로 경계 상황을 재현하게 한다는 해결.

**혼동 주의:** 시간 의존 테스트를 대기 시간을 넣어 맞추면 된다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — Eradicating Non-Determinism in Tests](https://martinfowler.com/articles/nonDeterminism.html) — Eradicating Non-Determinism in Tests: a call to the system clock

근거 문서: `r1-testing`. 검수 상태: **PENDING**

### R1-SEC-01 · INTERMEDIATE

로그인 전에 발급한 세션 식별자를 로그인에 성공한 뒤에도 그대로 사용하는 서비스가 있습니다. 공격자가 자신이 받은 세션 식별자를 피해자의 브라우저에 미리 심어 둘 수 있다면 어떤 일이 벌어지나요? 이 공격의 이름과 막는 방법을 설명하고, 세션 식별자를 담은 쿠키에 붙여야 할 속성도 답하세요.

**모범 답안**

이 공격은 세션 고정이다. 공격자는 서비스에서 세션 식별자를 하나 발급받아 피해자의 브라우저가 그 식별자를 쓰도록 만든다. 피해자가 그 상태로 로그인하면 서버는 같은 식별자의 세션에 로그인 정보를 연결한다. 공격자는 이미 알고 있는 그 식별자로 요청을 보내 피해자로 인증된 세션을 그대로 쓴다. 막는 방법은 로그인처럼 권한 수준이 바뀌는 순간에 기존 세션 식별자를 버리고 새 식별자를 발급하는 것이다. 그러면 로그인 전에 심어 둔 식별자는 인증된 세션과 연결되지 않는다. 식별자를 담은 쿠키에는 스크립트가 읽지 못하도록 HttpOnly를, 암호화된 연결로만 전송되도록 Secure를, 다른 사이트에서 시작된 요청에 실리는 것을 제한하도록 SameSite를 붙인다. 유휴 시간과 최대 수명에 따른 만료도 서버에서 강제한다.

**필수 개념**

- 세션 고정 공격의 성립 · 가중치 0.50: 로그인 전후로 세션 식별자가 유지되면 공격자가 미리 심은 식별자로 인증된 세션을 쓸 수 있다는 설명.
- 권한 변경 시 식별자 재발급과 쿠키 속성 · 가중치 0.50: 로그인 시 식별자를 새로 발급하고 HttpOnly·Secure·SameSite로 탈취와 오용 경로를 줄인다는 방어.

**혼동 주의:** 세션 식별자가 충분히 길면 로그인 뒤 재발급이 필요 없다고 설명하지 않기.

**출처 대조 위치**

- [OWASP Cheat Sheet Series — Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) — Session Management: Renew the Session ID After Any Privilege Level Change

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-02 · INTERMEDIATE

회원 비밀번호를 SHA-256으로 한 번 해시해 저장하고 있습니다. 데이터베이스가 유출됐을 때 이 방식이 버티지 못하는 이유를 설명하세요. 솔트가 막아 주는 공격과, bcrypt나 Argon2 같은 비밀번호 전용 해시 함수가 일반 해시 함수와 다른 점을 답하세요. 암호화해서 저장하는 방식과도 비교하세요.

**모범 답안**

SHA-256 같은 일반 해시 함수는 빠르게 계산되도록 설계됐다. 유출된 해시를 두고 공격자는 흔한 비밀번호 후보를 초당 수십억 개씩 해시해 대조할 수 있고, 솔트가 없으면 같은 비밀번호는 같은 해시가 되어 미리 계산해 둔 표로 한꺼번에 찾을 수 있다. 솔트는 비밀번호마다 따로 생성해 함께 해시하는 무작위 값이다. 같은 비밀번호도 서로 다른 해시가 되므로 미리 계산한 표가 쓸모없어지고, 공격자는 계정마다 따로 대입해야 한다. 비밀번호 전용 해시 함수는 일부러 느리고 자원을 많이 쓰게 설계되어, 작업 계수로 계산 비용을 조절할 수 있다. 한 번의 로그인 검증에는 부담이 없지만 대량 대입은 비현실적으로 느려진다. 암호화는 키만 있으면 원문으로 되돌릴 수 있어 키가 함께 유출되면 모든 비밀번호가 드러나므로, 되돌릴 수 없는 해시를 써야 한다.

**필수 개념**

- 빠른 해시와 솔트 없는 저장의 취약점 · 가중치 0.50: 일반 해시는 빨라 대량 대입이 가능하고 솔트가 없으면 같은 비밀번호가 같은 해시가 되어 미리 계산한 표에 뚫린다는 설명.
- 느린 전용 해시와 단방향성 · 가중치 0.50: 비밀번호 전용 해시는 작업 계수로 계산을 느리게 하며 암호화와 달리 원문으로 되돌릴 수 없다는 설명.

**혼동 주의:** 비밀번호를 ‘암호화해서 저장한다’고 해시와 구분 없이 설명하지 않기.

**출처 대조 위치**

- [OWASP Cheat Sheet Series — Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) — Password Storage: Hashing vs Encryption; Salting

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-03 · INTERMEDIATE

사용자가 올린 파일을 업로드 요청의 원래 파일 이름 그대로 서버 디렉터리에 저장합니다. 파일 이름이 ../../app/config.yml이거나 report.jsp일 때 각각 어떤 위험이 있나요? 저장용 이름을 서버가 따로 만들고 원래 이름은 별도로 보관하는 방식이 이를 어떻게 줄이는지 설명하세요.

**모범 답안**

원래 파일 이름은 클라이언트가 보낸 값이어서 공격자가 마음대로 정할 수 있다. 이름에 상위 디렉터리를 뜻하는 경로 조각이 들어 있으면 저장 위치가 업로드 디렉터리 밖으로 벗어나, 설정 파일 같은 기존 파일을 덮어쓸 수 있다. 확장자가 서버에서 실행되는 형식이고 저장 위치가 웹으로 접근 가능한 곳이면, 올린 파일이 서버에서 코드로 실행될 수 있다. 같은 이름의 파일을 다른 사용자가 올리면 서로 덮어쓰는 충돌도 생긴다. 저장할 때 서버가 무작위 식별자로 이름을 새로 만들면 경로 조각과 위험한 확장자가 저장 경로에 들어갈 길이 없어지고 충돌도 사라진다. 사용자에게 보여 줄 원래 이름은 데이터베이스에 따로 보관한다. 이와 함께 허용할 확장자를 목록으로 제한하고, 업로드 파일은 웹 서버가 직접 실행하지 않는 위치에 둔다.

**필수 개념**

- 클라이언트가 정한 파일 이름의 위험 · 가중치 0.50: 원래 파일 이름은 공격자가 정할 수 있어 경로 이탈로 파일을 덮어쓰거나 실행 가능한 파일을 올릴 수 있다는 설명.
- 서버가 생성한 저장 이름과 분리 보관 · 가중치 0.50: 저장 이름을 서버가 무작위로 만들고 원래 이름은 따로 보관하며 확장자 제한과 실행 불가 위치를 함께 쓴다는 방어.

**혼동 주의:** 확장자만 검사하면 업로드 기능이 안전하다고 설명하지 않기.

**출처 대조 위치**

- [OWASP Cheat Sheet Series — File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html) — File Upload: Change the filename to something generated by the application

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-04 · BASIC

회원 조회 쿼리를 문자열 SELECT * FROM members WHERE email = ' 뒤에 사용자 입력과 닫는 작은따옴표를 이어 붙여 만듭니다. 입력란에 ' OR '1'='1을 넣으면 어떤 쿼리가 실행되나요? 이 취약점이 생기는 근본 원인과, 파라미터 바인딩이 이를 막는 원리를 설명하세요.

**모범 답안**

이어 붙인 결과는 WHERE email = '' OR '1'='1'이 된다. 뒤의 조건이 항상 참이므로 모든 회원 행이 조회된다. 원인은 사용자 입력이 데이터로만 쓰이지 않고 SQL 문장의 일부로 해석된다는 데 있다. 입력에 든 작은따옴표가 문자열 값을 끝내 버리고, 그 뒤의 내용이 명령의 구조를 바꾼다. 파라미터 바인딩은 값이 들어갈 자리를 표시만 한 SQL 문을 먼저 데이터베이스에 보내 구조를 확정하고, 값은 그 뒤에 따로 전달한다. 데이터베이스는 전달된 값을 언제나 데이터로만 다루므로 값 안에 따옴표나 SQL 키워드가 있어도 문장의 구조가 바뀌지 않는다. 입력에서 위험한 문자를 걸러 내는 방식은 빠뜨리는 경우가 생기기 쉬워 주된 방어로 삼지 않는다. 테이블 이름이나 정렬 컬럼처럼 바인딩할 수 없는 자리는 허용 목록에 있는 값만 쓰게 한다.

**필수 개념**

- 입력이 SQL 구조로 해석되는 원인 · 가중치 0.50: 문자열을 이어 붙이면 입력의 따옴표가 값의 경계를 끝내 그 뒤가 SQL 명령의 일부로 해석된다는 설명.
- 구조와 값을 분리하는 파라미터 바인딩 · 가중치 0.50: SQL 구조를 먼저 확정하고 값을 따로 전달해 값이 항상 데이터로만 다뤄지게 한다는 원리.

**혼동 주의:** 특수문자를 걸러 내는 필터만으로 SQL 인젝션을 막을 수 있다고 설명하지 않기.

**출처 대조 위치**

- [OWASP Cheat Sheet Series — SQL Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html) — SQL Injection Prevention: Parameterized Queries

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-05 · INTERMEDIATE

서버가 로그인한 사용자에게 서명된 JWT를 발급하고, 이후 요청에서는 세션 저장소를 조회하지 않고 토큰의 서명만 검증해 사용자를 식별합니다. 토큰의 페이로드에 담은 내용을 클라이언트가 읽거나 바꿀 수 있는지 설명하세요. 이 방식에서 이미 발급한 토큰을 즉시 무효화하기 어려운 이유와 대응책도 답하세요.

**모범 답안**

서명된 JWT의 페이로드는 인코딩되어 있을 뿐 암호화된 것이 아니어서, 토큰을 가진 사람은 누구나 내용을 읽을 수 있다. 따라서 비밀번호나 민감한 개인정보를 담으면 안 된다. 내용을 바꾸면 서명이 맞지 않게 되므로 서버가 서명을 검증하는 한 변조는 탐지된다. 다만 서버는 토큰에 적힌 알고리즘 값을 그대로 따르지 말고 자신이 허용한 알고리즘인지 확인해야 한다. 서버가 저장소 없이 서명과 만료 시각만 보고 판단하므로, 탈취되거나 로그아웃된 토큰도 만료 전까지는 유효한 것으로 통과한다. 이것이 세션 방식과의 가장 큰 차이다. 대응책은 접근 토큰의 수명을 짧게 두고 재발급용 토큰을 서버에서 관리하거나, 폐기된 토큰 식별자의 목록을 조회하는 것이다. 뒤의 방법은 결국 서버 상태를 다시 들여오는 것이므로 무상태의 이점과 맞바꾸게 된다.

**필수 개념**

- 읽을 수 있지만 변조는 탐지되는 페이로드 · 가중치 0.50: 서명된 JWT의 내용은 누구나 읽을 수 있고 변조는 서명 검증으로 탐지되며 허용 알고리즘을 확인해야 한다는 설명.
- 즉시 폐기의 어려움과 대응 · 가중치 0.50: 저장소 없이 검증하므로 발급된 토큰은 만료까지 유효하며 짧은 수명이나 폐기 목록으로 대응한다는 설명.

**혼동 주의:** JWT의 내용이 암호화되어 있어 클라이언트가 읽을 수 없다고 설명하지 않기.

**출처 대조 위치**

- [RFC 8725 — JSON Web Token Best Current Practices](https://www.rfc-editor.org/rfc/rfc8725.html) — 3.1 Perform Algorithm Verification

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-06 · INTERMEDIATE

‘구글 계정으로 로그인’ 기능을 OAuth 2.0 권한 부여 코드 방식으로 구현합니다. 사용자의 브라우저를 거쳐 우리 서버로 돌아오는 것은 접근 토큰이 아니라 일회용 코드입니다. 코드를 받은 뒤 서버가 하는 일을 설명하고, 토큰을 브라우저로 직접 전달하지 않고 이렇게 두 단계로 나누는 이유를 답하세요.

**모범 답안**

사용자가 인가 서버에서 로그인하고 동의하면, 인가 서버는 브라우저를 우리 서버의 등록된 주소로 되돌려 보내면서 권한 부여 코드를 붙인다. 우리 서버는 이 코드를 받아 브라우저를 거치지 않는 직접 통신으로 인가 서버의 토큰 엔드포인트에 보낸다. 이때 클라이언트 식별자와 비밀 값으로 자신을 인증하고, 인가 서버는 코드가 유효하고 이 클라이언트에 발급된 것인지 확인한 뒤 접근 토큰을 내준다. 두 단계로 나누는 이유는 브라우저를 지나는 값을 최소화하기 위해서다. 주소창과 방문 기록, 중간 로그에 남을 수 있는 경로에는 수명이 짧고 한 번만 쓸 수 있는 코드만 지나가고, 실제 권한을 가진 토큰은 서버 사이의 통신으로만 전달된다. 코드가 노출되더라도 클라이언트 비밀 값이 없으면 토큰으로 바꿀 수 없다.

**필수 개념**

- 코드를 토큰으로 바꾸는 서버 간 교환 · 가중치 0.50: 서버가 받은 코드를 클라이언트 인증과 함께 토큰 엔드포인트에 보내 접근 토큰을 받는다는 절차.
- 브라우저에 토큰을 노출하지 않는 이유 · 가중치 0.50: 브라우저 경로에는 일회용 단명 코드만 지나고 토큰은 서버 간 통신으로만 전달되어 노출 위험을 줄인다는 설명.

**혼동 주의:** 권한 부여 코드와 접근 토큰을 같은 것으로 설명하지 않기.

**출처 대조 위치**

- [RFC 6749 — The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749.html) — 4.1 Authorization Code Grant

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-07 · ADVANCED

모바일 앱은 클라이언트 비밀 값을 안전하게 숨길 수 없습니다. 이런 앱이 권한 부여 코드 방식을 쓸 때, 같은 기기의 악성 앱이 되돌아오는 코드를 가로채면 어떤 일이 가능한가요? PKCE가 code_verifier와 code_challenge 두 값으로 이 공격을 막는 과정을 설명하세요.

**모범 답안**

공개 클라이언트는 토큰을 요청할 때 자신을 증명할 비밀 값이 없다. 그래서 악성 앱이 되돌아오는 권한 부여 코드를 가로채면, 그 코드만으로 토큰 엔드포인트에서 접근 토큰을 받아 낼 수 있다. PKCE에서는 앱이 인가 요청을 시작할 때마다 무작위 값인 code_verifier를 새로 만들고, 그것을 해시한 code_challenge만 인가 요청에 실어 보낸다. 인가 서버는 이 값을 발급하는 코드와 함께 기억해 둔다. 앱은 토큰을 요청할 때 코드와 함께 원래의 code_verifier를 보내고, 인가 서버는 그것을 해시한 결과가 기억해 둔 code_challenge와 같은지 확인한다. 코드를 가로챈 쪽은 해시 값만 볼 수 있을 뿐 원래 값을 알 수 없으므로 토큰을 받을 수 없다. 인가 요청을 시작한 쪽과 토큰을 요청하는 쪽이 같다는 것을 비밀 값 없이 증명하는 장치다.

**필수 개념**

- 비밀 값 없는 클라이언트의 코드 탈취 위험 · 가중치 0.50: 공개 클라이언트는 코드만으로 토큰을 받을 수 있어 가로챈 코드가 곧 토큰으로 이어진다는 설명.
- 검증 값과 그 해시의 대조 · 가중치 0.50: 인가 요청에는 해시만 보내고 토큰 요청에 원래 값을 보내 서버가 대조함으로써 요청자가 같음을 증명한다는 과정.

**혼동 주의:** PKCE를 클라이언트 비밀 값을 암호화해 숨기는 방법으로 설명하지 않기.

**출처 대조 위치**

- [RFC 7636 — Proof Key for Code Exchange by OAuth Public Clients](https://www.rfc-editor.org/rfc/rfc7636.html) — Proof Key for Code Exchange: authorization code interception attack

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-SEC-08 · INTERMEDIATE

https://app.example.com의 화면에서 스크립트가 https://api.example.com으로 JSON 본문의 PUT 요청을 보내자, 브라우저 콘솔에 CORS 오류가 나고 실제 요청보다 먼저 OPTIONS 요청이 나간 것이 보입니다. 이 사전 요청의 목적을 설명하세요. CORS가 서버를 보호하는 장치인지, 같은 요청을 curl로 보내면 어떻게 되는지도 답하세요.

**모범 답안**

브라우저는 기본적으로 한 출처의 스크립트가 다른 출처의 응답을 읽지 못하게 막는다. CORS는 서버가 응답 헤더로 특정 출처에 이 제한을 풀어 주는 방법이다. PUT이나 JSON 본문처럼 단순하지 않은 요청은 서버 상태를 바꿀 수 있으므로, 브라우저가 실제 요청을 보내기 전에 OPTIONS 사전 요청으로 이 출처와 메서드, 헤더를 허용하는지 먼저 묻는다. 서버가 허용 헤더로 답하면 실제 요청을 보내고, 그렇지 않으면 보내지 않는다. CORS는 브라우저가 사용자를 보호하려고 적용하는 정책이지 서버의 접근 통제가 아니다. curl 같은 도구는 이 정책을 적용하지 않으므로 같은 요청이 그대로 서버에 도달하고 응답도 받는다. 따라서 누가 무엇을 할 수 있는지는 서버가 인증과 인가로 따로 검사해야 한다. 허용 출처를 모든 출처로 열어 두면 브라우저의 보호만 사라진다.

**필수 개념**

- 사전 요청으로 묻는 허용 여부 · 가중치 0.50: 단순하지 않은 교차 출처 요청은 브라우저가 OPTIONS 사전 요청으로 출처·메서드·헤더 허용을 먼저 확인한다는 설명.
- 브라우저 정책이지 서버 접근 통제가 아님 · 가중치 0.50: CORS는 브라우저가 적용하는 정책이어서 다른 클라이언트는 영향받지 않으며 서버는 인증과 인가를 따로 해야 한다는 설명.

**혼동 주의:** CORS 설정이 서버 API를 외부 호출로부터 막아 준다고 설명하지 않기.

**출처 대조 위치**

- [WHATWG Fetch Standard](https://fetch.spec.whatwg.org/) — Fetch Standard: CORS-preflight request; Access-Control-Allow-Origin

근거 문서: `r1-security`. 검수 상태: **PENDING**

### R1-DEL-01 · BASIC

같은 서버에서 애플리케이션 열 개를 서로 영향을 주지 않게 실행하려 합니다. 가상 머신 열 대로 나누는 방법과 컨테이너 열 개로 나누는 방법에서, 각 실행 단위가 따로 갖는 것과 호스트와 공유하는 것을 비교하세요. 그 차이가 시작 시간과 자원 사용, 격리 수준에 주는 영향을 설명하세요.

**모범 답안**

가상 머신은 가상화된 하드웨어 위에서 저마다 완전한 운영체제를 따로 실행한다. 커널까지 분리되므로 격리가 강하지만, 운영체제마다 메모리와 디스크를 차지하고 부팅에 시간이 걸린다. 컨테이너는 호스트 운영체제의 커널을 공유하고, 파일 시스템과 프로세스 목록, 네트워크 같은 보이는 범위만 분리한다. 운영체제를 따로 띄우지 않으므로 프로세스를 시작하는 정도의 시간에 뜨고, 같은 서버에 훨씬 많이 올릴 수 있다. 애플리케이션과 그 실행에 필요한 라이브러리를 이미지로 묶어 두므로 어느 환경에서도 같은 구성으로 실행된다. 대신 커널을 공유하므로 격리는 가상 머신보다 느슨하고, 커널의 취약점은 모든 컨테이너에 영향을 줄 수 있다.

**필수 개념**

- 운영체제 전체와 커널 공유의 차이 · 가중치 0.50: 가상 머신은 각자 완전한 운영체제를 갖고 컨테이너는 호스트 커널을 공유하며 보이는 범위만 분리한다는 구분.
- 가벼움과 격리 수준의 교환 · 가중치 0.50: 컨테이너는 빠르게 뜨고 자원을 덜 쓰지만 커널 공유로 격리가 가상 머신보다 느슨하다는 설명.

**혼동 주의:** 컨테이너를 ‘가벼운 가상 머신’이라고만 설명하지 않기.

**출처 대조 위치**

- [Kubernetes Documentation — Overview](https://kubernetes.io/docs/concepts/overview/) — Kubernetes Overview: Container deployment era

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-02 · INTERMEDIATE

애플리케이션 서버 앞에 리버스 프록시를 두자, 접근 로그에 찍히는 클라이언트 IP가 모두 프록시의 주소가 됐고 애플리케이션이 만드는 절대 URL이 http로 시작합니다. 이런 일이 생기는 이유를 설명하세요. 프록시가 원래 요청 정보를 전달하는 헤더와, 애플리케이션이 그 헤더를 아무 조건 없이 믿으면 안 되는 이유도 답하세요.

**모범 답안**

리버스 프록시는 클라이언트의 연결을 받아 끝내고, 애플리케이션 서버로는 자신이 새 연결을 맺어 요청을 전달한다. 애플리케이션이 보는 연결의 상대는 프록시이므로 원격 주소는 프록시의 주소가 된다. 프록시에서 TLS를 끝내고 내부로는 평문으로 전달하면 애플리케이션은 요청이 http로 왔다고 판단한다. 이렇게 사라지는 정보를 프록시가 헤더에 담아 전달한다. 표준 Forwarded 헤더나 널리 쓰이는 X-Forwarded-For, X-Forwarded-Proto가 원래 클라이언트 주소와 프로토콜을 알려 준다. 그러나 이 헤더는 클라이언트도 직접 써서 보낼 수 있다. 애플리케이션이 누가 보낸 요청이든 이 헤더를 믿으면, 공격자가 IP를 속여 접근 제한을 피하거나 로그를 오염시킬 수 있다. 그래서 신뢰하는 프록시가 보낸 요청에서만 헤더를 해석하고, 프록시는 외부에서 온 같은 이름의 헤더를 지우거나 덮어써야 한다.

**필수 개념**

- 프록시가 끊는 연결과 사라지는 정보 · 가중치 0.50: 프록시가 연결을 새로 맺으므로 애플리케이션은 원래 클라이언트 주소와 프로토콜을 직접 볼 수 없다는 설명.
- 전달 헤더와 신뢰 경계 · 가중치 0.50: Forwarded 계열 헤더로 원래 정보를 전달하되 클라이언트가 위조할 수 있어 신뢰하는 프록시의 값만 해석한다는 주의.

**혼동 주의:** X-Forwarded-For의 첫 값을 항상 실제 클라이언트 IP로 믿는다고 설명하지 않기.

**출처 대조 위치**

- [RFC 7239 — Forwarded HTTP Extension](https://www.rfc-editor.org/rfc/rfc7239.html) — Forwarded HTTP Extension: X-Forwarded-For

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-03 · INTERMEDIATE

레플리카 네 개로 운영 중인 서비스를 Kubernetes Deployment의 롤링 업데이트로 새 버전으로 바꿉니다. maxUnavailable과 maxSurge 설정이 교체 과정에서 각각 무엇을 제한하는지 설명하세요. 교체 도중 구버전과 신버전이 함께 요청을 받는다는 점이 애플리케이션에 요구하는 조건도 답하세요.

**모범 답안**

롤링 업데이트는 구버전 파드를 한꺼번에 내리지 않고 신버전 파드를 조금씩 올리면서 구버전을 줄여 간다. maxUnavailable은 교체 중에 원하는 개수보다 모자라도 되는 파드 수의 상한이다. 0으로 두면 신버전이 준비된 뒤에야 구버전을 내려 처리 용량이 줄지 않는다. maxSurge는 교체 중에 원하는 개수를 넘어 추가로 띄울 수 있는 파드 수의 상한이다. 값을 키우면 교체가 빨라지지만 그동안 자원을 더 쓴다. 신버전 파드는 준비 상태 검사를 통과해야 트래픽을 받으므로 이 검사가 실제 준비 여부를 반영해야 한다. 교체가 끝날 때까지는 두 버전이 동시에 요청을 처리한다. 따라서 두 버전이 같은 데이터베이스 스키마와 메시지 형식에서 함께 동작해야 하고, 한 사용자의 연속된 요청이 서로 다른 버전에 도달해도 문제가 없어야 한다.

**필수 개념**

- 가용 수와 초과 수의 상한 · 가중치 0.50: maxUnavailable은 교체 중 모자라도 되는 파드 수, maxSurge는 추가로 띄울 수 있는 파드 수를 제한한다는 설명.
- 두 버전의 공존 조건 · 가중치 0.50: 교체 중 구버전과 신버전이 함께 요청을 받으므로 스키마와 형식이 양쪽에서 호환되어야 한다는 조건.

**혼동 주의:** 롤링 업데이트 중에는 한 시점에 한 버전만 요청을 받는다고 설명하지 않기.

**출처 대조 위치**

- [Kubernetes Documentation — Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) — Deployments: maxUnavailable; maxSurge

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-04 · INTERMEDIATE

운영 환경과 똑같은 환경을 하나 더 만들어 새 버전을 올려 두고, 검증이 끝나면 라우터가 트래픽을 한 번에 새 환경으로 돌리는 블루-그린 배포를 하려 합니다. 이 방식이 전환 시간과 롤백에서 주는 이점을 설명하세요. 두 환경이 데이터베이스 하나를 함께 쓸 때 주의할 점도 답하세요.

**모범 답안**

블루-그린 배포는 현재 운영 중인 환경과 새 버전을 올린 환경을 동시에 유지한다. 새 환경은 실제 트래픽을 받기 전에 운영과 같은 조건에서 최종 검증을 할 수 있다. 전환은 라우터가 요청을 보내는 대상을 바꾸는 일이라 순간적으로 끝나고, 사용자에게 보이는 중단이 거의 없다. 새 버전에 문제가 생기면 라우터를 이전 환경으로 되돌리기만 하면 되어 롤백이 빠르다. 대신 전환 전후로 환경 두 벌을 유지하는 비용이 든다. 데이터베이스를 함께 쓴다면 스키마 변경이 두 버전 모두와 호환되어야 한다. 새 버전에만 맞게 스키마를 바꾸면 이전 환경으로 되돌렸을 때 동작하지 않고, 전환 뒤에 쌓인 데이터도 이전 버전이 읽을 수 있어야 한다. 그래서 스키마는 먼저 두 버전을 모두 지원하는 형태로 바꾸고 애플리케이션 전환은 그 뒤에 한다.

**필수 개념**

- 대상 전환에 의한 빠른 교체와 롤백 · 가중치 0.50: 두 환경을 유지하다 라우팅 대상만 바꾸므로 전환이 순간적이고 문제 시 되돌리기가 빠르다는 설명.
- 공유 데이터베이스의 양방향 호환 · 가중치 0.50: DB를 함께 쓰면 스키마와 데이터가 두 버전 모두에서 동작해야 롤백이 가능하다는 주의.

**혼동 주의:** 블루-그린 배포를 하면 데이터베이스 변경도 자동으로 롤백된다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — BlueGreenDeployment](https://martinfowler.com/bliki/BlueGreenDeployment.html) — BlueGreenDeployment: two production environments

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-05 · INTERMEDIATE

새 버전을 전체 사용자에게 한 번에 내보내는 대신, 먼저 요청의 5%만 새 버전으로 보내는 카나리 배포를 하려 합니다. 이 방식이 줄이려는 위험이 무엇인지 설명하세요. 처음 구간에서 무엇을 관찰해 확대 여부를 판단하는지와, 이 방식이 가능하려면 시스템이 갖춰야 할 조건도 답하세요.

**모범 답안**

카나리 배포는 새 버전의 문제가 모든 사용자에게 동시에 닿는 위험을 줄인다. 테스트 환경에서 드러나지 않던 결함이나 성능 저하가 실제 트래픽에서 나타나더라도 영향 범위가 일부 사용자로 제한되고, 그 요청을 다시 구버전으로 돌리면 곧바로 수습된다. 새 버전으로 간 요청과 구버전으로 간 요청의 오류율과 응답 시간, 자원 사용량을 같은 시간대에서 비교하고, 주문 성공률 같은 업무 지표도 함께 본다. 차이가 허용 범위 안이면 비율을 단계적으로 늘린다. 이 방식이 가능하려면 트래픽을 비율이나 사용자 집단으로 나눠 보낼 수 있는 라우팅과, 버전별로 구분해 볼 수 있는 지표가 있어야 한다. 또 두 버전이 오랫동안 함께 동작하므로 데이터와 인터페이스가 양쪽에서 호환되어야 한다. 표본이 적으면 드문 오류가 보이지 않을 수 있다는 점도 판단에 넣는다.

**필수 개념**

- 영향 범위를 제한하는 단계적 노출 · 가중치 0.50: 새 버전을 일부 요청에만 먼저 적용해 문제의 영향 범위를 줄이고 빠르게 되돌린다는 목적.
- 버전별 지표 비교와 전제 조건 · 가중치 0.50: 두 버전의 오류율·응답 시간·업무 지표를 비교해 확대를 판단하며 비율 라우팅과 버전별 관측, 호환성이 필요하다는 설명.

**혼동 주의:** 카나리 배포를 ‘테스트 환경에 먼저 배포하는 것’으로 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — CanaryRelease](https://martinfowler.com/bliki/CanaryRelease.html) — CanaryRelease: small subset of users

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-06 · ADVANCED

운영 중인 회원 테이블의 name 컬럼을 full_name으로 바꾸려 합니다. 무중단 배포에서는 구버전과 신버전 애플리케이션이 한동안 함께 실행됩니다. 컬럼 이름을 한 번에 바꾸는 마이그레이션이 장애를 일으키는 이유를 설명하고, 확장·이전·축소의 세 단계로 나눠 안전하게 바꾸는 순서를 답하세요.

**모범 답안**

컬럼 이름을 한 번에 바꾸면 그 순간부터 name을 읽고 쓰는 구버전 애플리케이션의 쿼리가 모두 실패한다. 스키마 변경과 모든 서버의 코드 교체를 같은 순간에 끝낼 수 없으므로, 두 변경 사이에 반드시 어긋나는 구간이 생긴다. 안전한 방법은 호환되지 않는 변경을 호환되는 작은 단계들로 나누는 것이다. 확장 단계에서는 full_name 컬럼을 새로 추가하고, 애플리케이션이 두 컬럼에 모두 쓰게 하며 기존 행의 값을 새 컬럼으로 복사한다. 이 시점에는 구버전과 신버전이 모두 동작한다. 이전 단계에서는 읽는 쪽을 새 컬럼으로 차례로 옮긴다. 모든 코드가 새 컬럼만 쓰는 것을 확인한 뒤 축소 단계에서 name 컬럼에 쓰는 코드와 컬럼 자체를 제거한다. 각 단계는 따로 배포하고 따로 되돌릴 수 있다.

**필수 개념**

- 동시에 끝낼 수 없는 스키마와 코드 변경 · 가중치 0.50: 스키마 변경과 코드 교체는 같은 순간에 끝나지 않아 이름을 한 번에 바꾸면 구버전 쿼리가 실패한다는 설명.
- 확장·이전·축소의 단계적 변경 · 가중치 0.50: 새 컬럼을 추가해 함께 쓰고 읽기를 옮긴 뒤 옛 컬럼을 제거해 각 단계에서 두 버전이 동작하게 한다는 순서.

**혼동 주의:** 배포 순서만 잘 맞추면 컬럼 이름 변경을 한 번에 해도 된다고 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — ParallelChange](https://martinfowler.com/bliki/ParallelChange.html) — ParallelChange: expand and contract

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-07 · INTERMEDIATE

Dockerfile에서 소스 전체를 COPY한 다음 의존성 설치 명령을 실행합니다. 소스 파일 한 줄만 고쳐도 빌드할 때마다 의존성을 처음부터 다시 받습니다. 이미지 레이어와 빌드 캐시가 무효화되는 규칙으로 원인을 설명하고, 명령 순서를 어떻게 바꾸면 되는지 답하세요.

**모범 답안**

Dockerfile의 각 명령은 이미지 레이어 하나를 만들고, 빌드는 명령과 그 입력이 이전과 같으면 캐시된 레이어를 다시 쓴다. COPY 명령은 복사 대상 파일의 내용이 바뀌면 캐시가 무효화된다. 그리고 한 레이어가 무효화되면 그 뒤에 오는 레이어는 모두 다시 만들어진다. 소스 전체를 먼저 복사하면 소스 한 줄의 변경이 COPY 레이어를 무효화하고, 그 뒤에 있는 의존성 설치 레이어도 따라서 다시 실행된다. 해결은 자주 바뀌지 않는 것을 앞에, 자주 바뀌는 것을 뒤에 두는 것이다. 의존성 목록 파일만 먼저 복사해 의존성을 설치하고, 그다음에 나머지 소스를 복사한다. 그러면 의존성 목록이 바뀌지 않는 한 설치 레이어는 캐시에서 재사용되고, 소스 변경은 마지막 레이어들만 다시 만든다.

**필수 개념**

- 레이어 캐시의 연쇄 무효화 · 가중치 0.50: 한 레이어가 바뀌면 그 뒤의 레이어가 모두 다시 만들어지며 COPY는 파일 내용이 바뀌면 무효화된다는 규칙.
- 변경 빈도에 따른 명령 순서 · 가중치 0.50: 의존성 목록만 먼저 복사해 설치하고 소스는 뒤에 복사해 자주 바뀌는 것이 앞 레이어를 무효화하지 않게 한다는 해결.

**혼동 주의:** Dockerfile의 명령 순서는 결과 이미지가 같으면 상관없다고 설명하지 않기.

**출처 대조 위치**

- [Docker Docs — Docker build cache](https://docs.docker.com/build/cache/) — Docker build cache: all other layers that come after it are also affected

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-08 · INTERMEDIATE

개발자들이 각자 기능 브랜치에서 몇 주씩 작업한 뒤 릴리스 직전에 한꺼번에 병합하자, 충돌과 통합 오류를 고치는 데 며칠이 걸렸습니다. 지속적 통합이 이 문제를 줄이는 원리를 설명하세요. 이 실천이 성립하려면 병합 빈도와 자동 빌드, 깨진 빌드의 처리에 어떤 규칙이 필요한지 답하세요.

**모범 답안**

통합을 미룰수록 각 브랜치와 주 브랜치의 차이가 벌어지고, 서로 다른 변경이 만나는 지점이 한꺼번에 드러난다. 무엇이 무엇과 충돌하는지 가려내기 어려워 통합에 드는 시간을 예측할 수 없다. 지속적 통합은 통합을 자주, 작게 해서 이 비용을 평소에 조금씩 치르게 한다. 모든 개발자가 적어도 하루에 한 번 자신의 변경을 주 브랜치에 합치면, 어긋난 부분이 생겨도 범위가 하루치 변경에 그쳐 원인을 찾기 쉽다. 이를 위해 주 브랜치에 변경이 들어올 때마다 자동으로 빌드하고 테스트를 실행해 통합 결과가 동작하는지 바로 확인해야 한다. 빌드가 깨지면 새 작업보다 복구를 우선해 주 브랜치를 항상 배포할 수 있는 상태로 유지한다. 아직 완성되지 않은 기능은 긴 브랜치로 숨기지 않고, 기능을 꺼 둔 채로 작은 단위로 합친다.

**필수 개념**

- 작고 잦은 통합으로 줄이는 위험 · 가중치 0.50: 통합을 미루면 차이가 누적되어 충돌이 한꺼번에 드러나므로 매일 작게 합쳐 문제 범위를 줄인다는 원리.
- 자동 빌드와 깨진 빌드 우선 복구 · 가중치 0.50: 주 브랜치에 들어온 변경마다 자동 빌드·테스트로 확인하고 깨진 빌드는 가장 먼저 고친다는 규칙.

**혼동 주의:** CI를 ‘빌드 서버를 설치해 두는 것’으로 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — Continuous Integration](https://martinfowler.com/articles/continuousIntegration.html) — Continuous Integration: Everyone Pushes Commits To the Mainline Every Day

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

### R1-DEL-09 · INTERMEDIATE

운영 서버 설정을 담당자가 서버에 접속해 손으로 바꿔 왔습니다. 같은 역할의 서버 두 대가 미묘하게 다르게 동작하고, 누가 언제 무엇을 바꿨는지 알 수 없습니다. 인프라를 코드로 정의해 관리하는 방식이 이 문제를 어떻게 다루는지 설명하세요. 코드로 정의한 뒤에도 서버를 직접 고치면 생기는 문제도 답하세요.

**모범 답안**

손으로 바꾼 설정은 기록이 남지 않고 서버마다 적용 순서와 내용이 조금씩 달라져, 시간이 지나면 같은 역할의 서버가 서로 다른 상태가 된다. 인프라를 코드로 정의하면 서버와 네트워크의 원하는 구성이 파일로 남는다. 이 파일을 버전 관리에 두면 누가 언제 무엇을 왜 바꿨는지 추적하고, 변경을 리뷰하고, 문제가 생기면 이전 정의로 되돌릴 수 있다. 같은 정의를 자동화 도구로 적용하므로 서버 여러 대와 테스트 환경, 운영 환경을 같은 구성으로 반복해 만들 수 있다. 장애로 서버를 잃어도 정의에서 다시 만들면 된다. 그러나 정의를 두고도 서버에 직접 접속해 고치면 실제 상태가 정의와 어긋난다. 다음에 정의를 적용할 때 그 변경이 지워지거나 예상치 못한 차이로 적용이 실패한다. 그래서 모든 변경은 정의를 고쳐 같은 경로로 적용한다.

**필수 개념**

- 정의 파일과 버전 관리로 얻는 추적과 재현 · 가중치 0.50: 인프라 구성을 코드로 두면 변경 이력을 추적·리뷰·복원하고 같은 환경을 반복해 만들 수 있다는 설명.
- 수동 변경이 만드는 정의와 실제의 어긋남 · 가중치 0.50: 정의 밖에서 직접 고치면 실제 상태가 정의와 달라져 다음 적용 때 변경이 사라지거나 실패한다는 문제.

**혼동 주의:** 코드형 인프라를 ‘설치 스크립트를 몇 개 작성해 두는 것’으로 설명하지 않기.

**출처 대조 위치**

- [Martin Fowler — InfrastructureAsCode](https://martinfowler.com/bliki/InfrastructureAsCode.html) — InfrastructureAsCode: source control

근거 문서: `r1-delivery-and-migration`. 검수 상태: **PENDING**

## 검색에 제공할 근거 문서

### r1-java

Java 보강 근거 · r1-v1 · Java 21

Java 학습 근거 · r1-v1
기준: Java 21
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-JAVA-01]
제네릭 클래스는 타입 매개변수를 선언하고, 사용하는 쪽이 타입 인자를 지정해 매개변수화 타입을 만든다. 컴파일러는 이 타입 인자를 기준으로 대입과 반환을 검사하고 필요한 형변환을 대신 넣는다. 타입 인자 없이 Object로 다루면 검사는 호출자의 형변환으로 미뤄지고, 틀린 형변환은 실행 시점의 ClassCastException이 된다.
근거: jls-4 — 4.5 Parameterized Types

[R1-JAVA-02]
매개변수화 타입끼리의 하위 타입 관계는 타입 인자가 서로 포함 관계일 때만 성립한다. List<Integer>와 List<Number>는 타입 인자가 다르므로 서로 대입할 수 없다. 와일드카드 ? extends T는 T와 그 하위 타입을 인자로 가진 타입을 모두 받는다. 실제 원소 타입을 알 수 없으므로 읽은 값은 T로 다룰 수 있지만 T 타입 값을 넣는 호출은 허용되지 않는다.
근거: jls-4 — 4.5.1 Type Arguments of Parameterized Types; 4.10.2 Subtyping among Class and Interface Types

[R1-JAVA-03]
타입 소거는 매개변수화 타입과 타입 변수를 타입 인자가 없는 형태로 바꾸는 대응이다. 실행 시점에 완전한 타입 정보가 남는 타입을 구체화 가능 타입이라 하며, List<String>이나 타입 변수 T는 여기에 속하지 않는다. 배열 생성과 instanceof 검사는 실행 시점의 타입 정보를 필요로 하므로, 이런 타입에는 Class 객체를 전달하거나 비한정 와일드카드 타입으로 검사하는 식으로 접근한다.
근거: jls-4 — 4.6 Type Erasure; 4.7 Reifiable Types

[R1-JAVA-04]
List는 원소의 순서를 유지하고 같은 값을 여러 번 담을 수 있다. Set은 equals로 같다고 판단되는 원소를 둘 이상 담지 않는다. HashSet은 해시 테이블로 구현되어 원소가 고르게 분산될 때 추가와 포함 확인이 상수 시간이지만 순서를 보장하지 않는다. 원소 클래스의 equals와 hashCode가 어긋나면 같은 값이 중복 저장되거나 넣은 값을 찾지 못한다.
근거: api-set — Set 인터페이스 설명: contains no duplicate elements

[R1-JAVA-05]
static으로 선언한 메서드는 클래스 메서드이며 특정 객체를 참조하지 않고 호출된다. 클래스 메서드 본문에서 this나 super를 쓰거나 인스턴스 변수를 단순 이름으로 참조하면 컴파일 오류다. static 필드는 클래스가 초기화될 때 하나만 만들어져 모든 객체가 공유하고, 인스턴스 필드는 객체가 만들어질 때마다 새로 생긴다.
근거: jls-8 — 8.4.3.2 static Methods; 8.3.1.1 static Fields

[R1-JAVA-06]
부호화 문자 집합은 추상 문자 집합과 정수 집합 사이의 대응이다. 문자 인코딩 방식은 그 정수들을 바이트열로 옮기는 규칙이며, Java의 Charset은 문자열과 바이트열 사이의 변환을 정의한다. 쓰기와 읽기에 쓰인 Charset이 다르면 같은 바이트가 다른 문자로 복원된다. Charset을 지정하지 않으면 JVM의 기본 Charset이 쓰이고, 이 값은 구현에 따라 바꾸지 않는 한 UTF-8이다.
근거: api-charset — Charset 클래스 설명: coded character set; character-encoding scheme; defaultCharset: default charset is UTF-8

[R1-JAVA-07]
BufferedInputStream은 생성될 때 내부 버퍼 배열을 만들고, 읽기 요청이 오면 감싼 스트림에서 여러 바이트를 한 번에 채워 둔 뒤 그 배열에서 돌려준다. 감싼 스트림이 파일이라면 운영체제에 대한 읽기 요청은 버퍼를 채울 때만 일어난다. 호출 횟수가 줄어드는 효과는 버퍼가 작을 때 크고, 일정 크기를 넘으면 장치 속도와 메모리 사용이 더 큰 요인이 된다.
근거: api-bis — BufferedInputStream 클래스 설명: many bytes at a time

[R1-JAVA-08]
Files.readAllBytes는 파일의 모든 바이트를 배열로 읽어 돌려주며, 간단한 경우를 위한 메서드이지 큰 파일을 읽기 위한 메서드가 아니다. Files.lines는 줄을 스트림으로 제공하되 스트림이 소비될 때 읽어 나가므로 전체를 메모리에 올리지 않는다. 이 스트림은 열린 파일을 붙잡고 있어 사용이 끝나면 닫아야 한다.
근거: api-files — Files.readAllBytes: not intended for reading in large files; Files.lines: try-with-resources

[R1-JAVA-09]
리플렉션은 로드된 클래스의 필드·메서드·생성자 정보를 프로그램에서 얻고, 그 정보를 이용해 실제 필드를 읽고 쓰거나 메서드와 생성자를 호출하게 한다. 이 사용은 캡슐화와 접근 제한의 범위 안에서 허용되며, 제한을 넘으려면 별도 요청이 필요하다. 이름과 인자를 실행 중에 맞추므로 잘못된 사용은 컴파일이 아니라 실행 시점의 예외로 나타난다.
근거: api-reflect — java.lang.reflect 패키지 설명: reflective information about classes and objects

[R1-JAVA-10]
애노테이션은 선언에 정보를 연결하는 표식이며 프로그램의 실행 의미를 직접 바꾸지 않는다. 보존 정책은 그 정보가 소스에만 남는지, 클래스 파일까지 남는지, 실행 중 리플렉션으로 조회할 수 있는지를 정한다. Retention을 지정하지 않으면 CLASS로 취급되어 실행 중에는 보이지 않는다. 실행 시점에 읽어 동작을 붙이는 프레임워크는 RUNTIME 보존을 전제로 한다.
근거: jls-9 — 9.6.4.2 @Retention

[R1-JAVA-11]
Object.equals는 null이 아닌 참조에 대해 반사적·대칭적·추이적이고 일관된 동치 관계를 구현해야 한다. 대칭성은 x.equals(y)가 true이면 y.equals(x)도 true여야 한다는 요구다. 상위 클래스와 하위 클래스가 서로 다른 기준으로 비교하면 호출 방향에 따라 결과가 달라져 이 요구를 어긴다. equals를 재정의하면 같다고 판단되는 객체가 같은 해시 코드를 갖도록 hashCode도 함께 맞춰야 한다.
근거: api-object — Object.equals: symmetric

[R1-JAVA-12]
문자열 리터럴은 String 객체에 대한 참조이며, 같은 내용의 리터럴은 항상 같은 String 객체를 가리킨다. 실행 중 계산된 문자열은 내용이 같아도 별도의 객체로 만들어진다. 참조 타입에 대한 ==는 두 참조가 같은 객체를 가리킬 때만 true이므로, 내용이 같은지는 equals로 비교한다.
근거: jls-3 — 3.10.5 String Literals

[R1-JAVA-13]
String 객체는 만들어진 뒤 값이 바뀌지 않으므로 연결 연산의 결과는 새 String이다. 반복문에서 누적 연결을 하면 매번 지금까지의 내용이 새 객체로 복사된다. StringBuilder는 변경 가능한 문자열 버퍼이며 append는 버퍼 끝에 문자를 더하고, 용량이 부족하면 자동으로 늘린다. 누적이 끝난 뒤 toString을 호출하면 그 시점의 내용으로 String이 만들어진다.
근거: api-stringbuilder — StringBuilder 클래스 설명: mutable sequence of characters

[R1-JAVA-14]
박싱 변환은 기본형 값을 대응하는 래퍼 객체로 바꾼다. -128에서 127 사이의 정수 상수를 박싱한 결과는 항상 같은 객체이지만, 그 밖의 값은 같은 객체라는 보장이 없다. 언박싱 변환은 래퍼 객체에서 기본형 값을 꺼내며, 참조가 null이면 NullPointerException을 던진다. 래퍼끼리의 ==는 참조를 비교하므로 값 비교에는 equals나 기본형 비교를 쓴다.
근거: jls-5 — 5.1.7 Boxing Conversion; 5.1.8 Unboxing Conversion

[R1-JAVA-15]
enum 클래스는 이름 붙은 상수의 고정된 집합을 정의하며, 선언된 상수 외에는 인스턴스를 만들 수 없다. 따라서 enum 타입의 변수는 그 상수들 가운데 하나이거나 null이다. enum은 클래스이므로 필드와 메서드를 가질 수 있어 상수마다 필요한 데이터와 동작을 함께 둘 수 있다. 정수 상수는 타입이 int일 뿐이어서 범위 밖의 값이나 다른 의미의 값을 구분하지 못한다.
근거: jls-8 — 8.9 Enum Classes

[R1-JAVA-16]
Instant는 타임라인 위의 한 순간을 나타내는 숫자형 타임스탬프다. LocalDateTime은 날짜와 시각을 담지만 시간대나 오프셋이 없어 그 자체로는 타임라인의 어느 순간인지 정해지지 않는다. ZonedDateTime은 날짜·시각에 시간대를 더해 순간을 지역 시각으로 표현하고, 일광 절약 시간 같은 규칙 변화를 반영한다. 서로 다른 지역에서 같은 사건을 다루려면 순간을 기준으로 저장하고 표시 단계에서 지역 시각으로 바꾼다.
근거: api-time — java.time 패키지 설명: Dates and Times

[R1-JAVA-17]
Duration은 타임라인을 따라 나노초 단위로 잰 시간의 양이다. Period는 연·월·일처럼 사람에게 의미 있는 단위로 표현한 양이다. 날짜에 Period를 더하면 달력 규칙에 따라 계산되어 월의 길이가 달라도 같은 날짜로 이동하고, Duration을 더하면 고정된 길이만큼 이동한다. 시간대가 있는 값에서는 하루의 길이가 달라지는 날이 있어 두 계산의 결과가 다를 수 있다.
근거: api-time — java.time 패키지 설명: Duration and Period

[R1-JAVA-18]
Throwable은 생성 시점의 실행 스택을 담고, 자신을 만들게 한 다른 Throwable을 원인으로 가질 수 있다. 하위 계층의 예외를 상위 계층의 예외로 바꿀 때 원인을 연결하면, 상위 예외를 받은 쪽에서도 원래 실패의 종류와 위치를 확인할 수 있다. 원인을 넘기지 않으면 이 연결이 끊긴다. 예외를 잡는 코드는 그 실패에 대해 할 수 있는 일이 있을 때 의미가 있으며, 없다면 호출자에게 알리는 편이 실패를 숨기지 않는다.
근거: api-throwable — Throwable 클래스 설명: chained exception

[R1-JAVA-19]
어떤 메서드가 다른 메서드를 재정의할 때, 재정의하는 쪽의 throws 절에 적힌 체크 예외는 재정의되는 쪽의 throws 절이 허용하는 범위 안에 있어야 한다. 그렇지 않으면 컴파일 오류다. 이 규칙 덕분에 상위 타입의 선언만 보고 작성한 호출 코드가 하위 구현에서도 안전하다. 선언에 체크 예외가 없는 메서드를 구현할 때는 본문에서 예외를 잡아 처리하거나 비체크 예외로 바꿔야 한다.
근거: jls-8 — 8.4.8.3 Requirements in Overriding and Hiding

[R1-JAVA-20]
람다 식은 메서드처럼 매개변수와 본문을 갖지만 이름이 없고, 함수형 인터페이스의 인스턴스로 평가된다. 메서드가 함수형 인터페이스 타입의 매개변수를 받으면 호출자는 실행할 코드를 인자로 넘길 수 있다. 이렇게 하면 공통 절차는 한 번만 작성하고, 절차 안에서 달라지는 판단이나 변환만 호출마다 바꿀 수 있다.
근거: jls-15 — 15.27 Lambda Expressions

[R1-JAVA-21]
함수형 인터페이스는 Object의 메서드를 제외하고 추상 메서드가 하나인 인터페이스다. 람다 식은 대입·호출·형변환 문맥에서만 쓸 수 있고, 그 문맥이 요구하는 대상 타입이 함수형 인터페이스여야 한다. 람다의 매개변수와 본문은 대상 타입에서 유도한 함수 타입과 호환되어야 하며, 호환되면 그 인터페이스의 인스턴스로 평가된다. 따라서 같은 람다 식도 문맥에 따라 서로 다른 타입의 값이 된다.
근거: jls-15 — 15.27.3 Type of a Lambda Expression

[R1-JAVA-22]
메서드 참조 식은 메서드 본문을 적지 않고 이미 있는 메서드를 가리켜 함수형 인터페이스의 인스턴스를 만든다. 형태는 타입 이름으로 정적 메서드를 가리키는 것, 식으로 얻은 객체의 인스턴스 메서드를 가리키는 것, 타입 이름으로 인스턴스 메서드를 가리키는 것 등이 있다. 마지막 형태에서는 함수 타입의 첫 매개변수가 호출 대상이 되고 나머지가 메서드의 인자가 된다.
근거: jls-15 — 15.13 Method Reference Expressions

[R1-JAVA-23]
스트림 파이프라인은 소스, 0개 이상의 중간 연산, 하나의 최종 연산으로 이루어진다. 중간 연산은 항상 지연되어 새 스트림을 돌려줄 뿐이고, 소스 순회는 최종 연산이 실행될 때 시작된다. 지연 덕분에 여러 단계를 한 번의 순회로 합쳐 처리할 수 있고, 단락 연산이 있으면 필요한 만큼만 원소를 살펴본 뒤 끝낼 수 있다.
근거: api-stream — java.util.stream 패키지 설명: Stream operations and pipelines

[R1-JAVA-24]
IntStream은 int 값의 순차를 다루는, Stream의 기본형 특화 버전이다. 객체 스트림에서 수치를 다루면 원소가 래퍼 객체이므로 연산 때마다 기본형으로 꺼냈다가 다시 감싸는 변환이 끼어든다. 기본형 특화 스트림은 값을 그대로 전달하므로 이 변환과 객체 할당이 없고, 합계·평균·최댓값 같은 집계를 바로 제공한다. 객체 스트림과는 mapToInt, boxed 같은 연산으로 오갈 수 있다.
근거: api-intstream — IntStream 인터페이스 설명: int primitive specialization of Stream

[R1-JAVA-25]
Optional.orElse는 값이 있으면 그 값을, 없으면 인자로 받은 값을 돌려준다. 인자는 메서드 호출 규칙에 따라 호출 전에 이미 계산되어 있다. orElseGet은 값이 없을 때에만 공급 함수를 호출해 그 결과를 돌려준다. 두 메서드의 반환값은 같아도 대체 값을 구하는 코드가 언제 실행되는지가 다르다.
근거: api-optional — Optional.orElseGet: supplying function

[R1-JAVA-26]
Optional은 값이 있을 수도 없을 수도 있는 컨테이너이며, 주로 결과 없음을 나타낼 필요가 분명한 메서드의 반환 타입으로 쓰도록 의도되었다. get은 값이 없으면 NoSuchElementException을 던진다. 값의 유무에 따른 처리는 orElse, orElseGet, orElseThrow, map, ifPresent 같은 메서드로 표현할 수 있어 호출자가 부재 시의 동작을 명시하게 된다.
근거: api-optional — Optional 클래스 설명: method return type

[R1-JAVA-27]
ForkJoinPool은 작업 훔치기를 쓰는 실행기다. 풀의 스레드는 풀에 제출된 작업뿐 아니라 실행 중인 작업이 만들어 낸 하위 작업도 찾아 실행한다. 공용 풀은 별도 풀을 지정하지 않은 작업이 함께 쓰며, 병렬도는 기본적으로 사용 가능한 프로세서 수를 기준으로 한다. 스레드가 블로킹되면 그만큼 병렬도가 줄어들기 때문에, 대기 시간이 긴 작업을 공용 풀에 넣으면 풀을 공유하는 다른 작업이 진행되지 못한다.
근거: api-fjp — ForkJoinPool 클래스 설명: work-stealing; common pool

[R1-JAVA-28]
병렬 스트림은 소스를 여러 조각으로 나눠 각 조각을 따로 처리하고 결과를 결합한다. 이 과정의 비용 때문에 병렬 실행이 순차 실행보다 항상 빠른 것은 아니며, 소스를 얼마나 쉽게 나눌 수 있는지와 원소당 작업량이 결과를 좌우한다. 스트림 연산에 넘기는 함수가 외부 상태를 읽거나 바꾸면 실행 순서와 스레드에 따라 결과가 달라질 수 있으므로, 부수 효과 없이 작성하고 결과는 축약 연산으로 모은다.
근거: api-stream — java.util.stream 패키지 설명: Parallelism; Side-effects

[R1-JAVA-29]
함수가 호출 밖의 상태를 읽거나 바꾸면 결과가 입력만으로 정해지지 않고, 실행 순서와 다른 코드의 동작에 좌우된다. 스트림 연산에 전달하는 함수는 상태에 의존하지 않고 부수 효과가 없어야 실행 방식이 달라져도 같은 결과를 낸다. 입력을 수정하지 않고 결과를 반환값으로만 전달하는 함수는 따로 떼어 검증하기 쉽고, 여러 스레드에서 함께 호출해도 동기화가 필요 없다.
근거: api-stream — java.util.stream 패키지 설명: Stateless behaviors; Side-effects

출처
jls-4: https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html
api-set: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html
jls-8: https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html
api-charset: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html
api-bis: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/BufferedInputStream.html
api-files: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html
api-reflect: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/package-summary.html
jls-9: https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html
api-object: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html
jls-3: https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html
api-stringbuilder: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html
jls-5: https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html
api-time: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html
api-throwable: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html
jls-15: https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html
api-stream: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html
api-intstream: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html
api-optional: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html
api-fjp: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ForkJoinPool.html

### r1-jvm-and-concurrency

JVM·동시성 보강 근거 · r1-v1 · Java 21 (java.util.concurrent, JLS 17장, HotSpot)

JVM·동시성 학습 근거 · r1-v1
기준: Java 21 (java.util.concurrent, JLS 17장, HotSpot)
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-JVM-01]
Thread.join은 대상 스레드가 종료할 때까지 호출한 스레드를 기다리게 한다. 시간을 인자로 주면 그 시간까지만 기다리며, 이때는 반환 뒤에도 대상이 아직 실행 중일 수 있다. sleep은 현재 스레드를 정해진 시간 동안 멈출 뿐 다른 스레드의 진행 상태와는 관계가 없다. 다른 스레드의 완료에 의존하는 코드는 시간이 아니라 완료 자체를 기다려야 결과가 일정하다.
근거: api-thread — Thread.join: Waits for this thread to terminate

[R1-JVM-02]
Thread.yield는 현재 스레드가 프로세서 사용을 양보할 수 있다는 힌트를 스케줄러에 전달한다. 스케줄러는 이 힌트를 무시해도 되므로 동작은 플랫폼과 부하에 따라 달라지고, 동시성 제어의 정확성을 yield에 의존해서는 안 된다. 조건이 만족될 때까지 기다리는 코드는 스레드를 대기 상태로 두었다가 상태가 바뀔 때 깨우는 구조여야 CPU를 낭비하지 않는다.
근거: api-thread — Thread.yield: A hint to the scheduler

[R1-JVM-03]
여러 스레드가 공유 변수를 읽고 그 값에 근거해 다시 쓰는 과정은 여러 단계로 이루어지므로, 한 스레드의 단계 사이에 다른 스레드의 단계가 끼어들 수 있다. synchronized는 객체의 모니터를 잠가 한 번에 한 스레드만 그 블록을 실행하게 한다. 보호가 의미를 가지려면 하나의 불변식에 관여하는 읽기와 쓰기 전체가 같은 모니터 아래에 있어야 하고, 그 상태에 접근하는 모든 경로가 같은 모니터를 사용해야 한다.
근거: jls-17 — 17.1 Synchronization

[R1-JVM-04]
ReentrantLock은 synchronized가 쓰는 암묵적 모니터 락과 기본 동작이 같으면서 확장 기능을 제공하는 재진입 상호 배제 락이다. 락 획득을 시도만 하거나 시간 제한을 두거나 인터럽트에 반응하도록 할 수 있다. 락의 획득과 해제가 블록 구조에 묶이지 않으므로, lock 호출 바로 뒤에 try 블록을 두고 finally에서 unlock을 호출하는 것이 권장되는 사용법이다.
근거: api-lock — ReentrantLock 클래스 설명: extended capabilities; try block

[R1-JVM-05]
wait를 호출한 스레드는 그 객체의 대기 집합에 들어가 모니터를 놓고 기다린다. notify는 대기 집합의 스레드 하나를 깨우며 어느 스레드가 선택될지는 정해져 있지 않다. 깨어난 스레드가 기다리던 조건이 실제로 만족됐다는 보장은 없고 알림 없이 깨어나는 경우도 있으므로, wait는 조건을 검사하는 반복문 안에서 호출해야 한다. 서로 다른 조건을 기다리는 스레드가 한 대기 집합에 있으면 notifyAll로 모두 깨워 각자 조건을 다시 확인하게 한다.
근거: api-object — Object.notify: The choice is arbitrary; Object.wait: spurious wakeup

[R1-JVM-06]
Condition은 Object의 wait·notify·notifyAll을 별도 객체로 분리해, 하나의 락에 여러 대기 집합을 둘 수 있게 한다. await는 락을 원자적으로 놓고 대기하며, 돌아오기 전에 반드시 그 락을 다시 얻는다. 조건마다 Condition을 따로 두면 그 조건을 기다리는 스레드에게만 신호를 보낼 수 있다. 신호와 실제 실행 사이에 상태가 바뀔 수 있으므로 await는 조건을 검사하는 반복문 안에서 호출한다.
근거: api-condition — Condition 인터페이스 설명: multiple wait-sets per object

[R1-JVM-07]
BlockingQueue의 메서드는 연산을 즉시 수행할 수 없을 때의 처리 방식에 따라 네 형태로 나뉜다. 예외를 던지는 형태, null이나 false 같은 특수 값을 돌려주는 형태, 가능해질 때까지 현재 스레드를 대기시키는 형태, 정해진 시간까지만 대기하는 형태다. 삽입은 add·offer·put·시간 제한 offer, 제거는 remove·poll·take·시간 제한 poll이 각각 대응한다. 용량이 제한된 큐에서 대기형 삽입은 생산자의 속도를 소비자에 맞추는 역할을 한다.
근거: api-bq — BlockingQueue 인터페이스 설명: Throws exception; Special value; Blocks; Times out

[R1-JVM-08]
atomic 패키지의 클래스는 단일 변수에 대한 락 없는 스레드 안전 갱신을 지원한다. compareAndSet은 변수의 현재 값이 기대 값과 같으면 새 값으로 원자적으로 바꾸고 성공 여부를 알려 준다. 읽기·계산·교체 시도를 성공할 때까지 반복하는 방식으로 증가 같은 연산을 구현한다. 스레드를 멈추지 않는 대신 충돌이 잦으면 재시도가 늘어나며, 여러 변수에 걸친 불변식은 이 방식만으로 보호할 수 없다.
근거: api-atomic — java.util.concurrent.atomic 패키지 설명: lock-free thread-safe programming on single variables

[R1-JVM-09]
Collections.synchronizedList가 돌려주는 리스트는 각 메서드 호출을 리스트 객체의 락으로 보호한다. 반복자로 순회할 때는 사용자가 직접 그 리스트 객체에 대해 동기화해야 하며, 그렇지 않으면 결과가 보장되지 않는다. 동시성 컬렉션은 내부에서 동시 접근을 처리하도록 설계되어 순회 중 변경이 있어도 예외를 던지지 않는 반복자를 제공한다. 어느 쪽이든 여러 호출에 걸친 불변식은 호출하는 쪽이 따로 보호해야 한다.
근거: api-collections — Collections.synchronizedList: manually synchronize on the returned list

[R1-JVM-10]
ThreadPoolExecutor는 실행 중인 스레드가 코어 수보다 적으면 새 스레드를 만들고, 코어 수 이상이면 작업을 큐에 넣는다. 큐에 넣을 수 없을 때에만 최대 수 한도 안에서 새 스레드를 만들고, 그것도 불가능하면 작업을 거부한다. 용량 제한이 없는 큐를 쓰면 코어 스레드가 모두 바쁠 때 새 작업이 끝없이 대기하고 최대 수 값은 영향을 주지 못한다. 용량이 있는 큐와 거부 처리기는 자원 고갈을 막는 대신 넘친 작업을 어떻게 다룰지 정하게 한다.
근거: api-tpe — ThreadPoolExecutor 클래스 설명: Core and maximum pool sizes; Queuing; Rejected tasks

[R1-JVM-11]
Callable은 결과를 반환하고 예외를 던질 수 있는 작업이며, Runnable은 결과를 반환하지 않고 체크 예외를 던질 수 없다. 실행기에 Callable을 제출하면 아직 끝나지 않은 계산의 결과를 나타내는 Future를 받는다. Future의 get은 계산이 끝날 때까지 필요한 만큼 대기한 뒤 결과를 돌려주고, 계산이 예외를 던졌다면 그 예외를 원인으로 가진 ExecutionException을 던진다.
근거: api-callable — Callable 인터페이스 설명: returns a result and may throw an exception

[R1-JVM-12]
shutdown은 이미 제출된 작업은 실행하되 새 작업은 받지 않는 순차적 종료를 시작한다. shutdownNow는 실행 중인 작업을 멈추려고 시도하고, 대기 중인 작업의 처리를 중단해 그 목록을 돌려준다. 실행 중인 작업을 멈추는 것은 최선의 시도일 뿐이어서, 보통 인터럽트로 구현되며 인터럽트에 응답하지 않는 작업은 끝나지 않을 수 있다. 두 메서드 모두 작업이 끝나기를 기다리지 않으므로 완료를 기다리려면 awaitTermination을 쓴다.
근거: api-executor — ExecutorService.shutdown: previously submitted tasks are executed; ExecutorService.shutdownNow: Attempts to stop all actively executing tasks

[R1-JVM-13]
객체는 실행 중인 프로그램의 살아 있는 다른 객체로부터 더 이상 도달할 수 없게 되면 쓰레기로 간주되어 그 메모리를 재사용할 수 있다. 대부분의 객체가 짧은 시간만 살아남는다는 관찰을 약한 세대 가설이라 한다. HotSpot의 수집기는 이 가설에 따라 힙을 세대로 나누고, 새 객체가 할당되는 젊은 세대를 따로 수집한다. 젊은 세대 수집에서 살아남은 객체는 결국 늙은 세대로 옮겨지고, 늙은 세대는 덜 자주 수집된다.
근거: gc-tuning — Generational Garbage Collection; weak generational hypothesis

출처
api-thread: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html
jls-17: https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html
api-lock: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html
api-object: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html
api-condition: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/Condition.html
api-bq: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html
api-atomic: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/package-summary.html
api-collections: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html
api-tpe: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html
api-callable: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Callable.html
api-executor: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html
gc-tuning: https://docs.oracle.com/en/java/javase/21/gctuning/garbage-collector-implementation1.html

### r1-object-oriented-design

객체 지향·도메인 설계 보강 근거 · r1-v1 · Java 21; 명시한 업무 계약의 설계 사례

객체 지향·도메인 설계 학습 근거 · r1-v1
기준: Java 21; 명시한 업무 계약의 설계 사례
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-OOD-01]
어떤 클래스가 인터페이스를 통해 협력 객체를 사용하더라도, 그 구현을 직접 생성하면 인터페이스와 구현 클래스 양쪽에 의존하게 된다. 의존성 주입은 구현을 찾고 만드는 일을 별도의 조립자에게 맡기고, 사용하는 객체에는 생성자 등으로 전달하는 방식이다. 이렇게 하면 사용하는 코드는 계약에만 의존하고, 어떤 구현이 연결될지는 조립 단계에서 정해진다. 구현 교체와 테스트용 대역 사용이 사용 코드의 수정 없이 가능해진다.
근거: fowler-injection — Inversion of Control; Constructor Injection

[R1-OOD-02]
객체 지향은 데이터와 그 데이터를 다루는 함수를 함께 묶는 방식이다. 객체에서 데이터를 꺼내 바깥에서 판단하고 다시 넣는 코드는 판단 로직을 객체 밖에 두게 되어, 같은 규칙이 여러 호출 지점에 반복된다. 객체에게 무엇을 할지 요청하고 판단은 그 데이터를 가진 객체가 하도록 하면 규칙이 한 곳에 모인다. 접근 제어자는 이 구조를 돕는 수단일 뿐이며, 모든 필드에 읽기·쓰기 메서드를 여는 것은 은닉이 아니다.
근거: fowler-telldontask — TellDontAsk: bundling data with the functions that operate on that data

[R1-OOD-03]
객체에서 데이터를 요청해 받은 뒤 그 데이터로 일을 하기보다, 객체에게 할 일을 시키는 편이 데이터와 동작을 함께 두게 한다. 어떤 동작에 필요한 정보를 가장 많이 가진 객체가 그 동작을 맡으면, 다른 객체는 그 내부 구조를 알 필요가 없다. 내부 데이터의 형태가 바뀌어도 변경은 그 객체의 메서드 안에서 끝난다. 반대로 데이터를 꺼내 쓰는 코드가 많을수록 구조 변경이 여러 곳으로 번진다.
근거: fowler-telldontask — TellDontAsk: Rather than asking an object for data and acting on that data

[R1-OOD-04]
한 객체가 다른 객체를 거쳐 그 뒤의 객체에 접근하는 호출이 이어지면, 호출하는 쪽은 중간 객체들의 연결 구조를 모두 알게 된다. 위임 숨기기는 중간 객체에 위임 메서드를 두어 호출자가 뒤쪽 객체의 존재를 모르게 하는 정리 방법이다. 호출자가 아는 범위가 직접 협력하는 객체의 계약으로 줄어들면, 그 뒤의 구조가 바뀌어도 호출자는 영향을 받지 않는다. 다만 위임 메서드가 지나치게 늘면 중간 객체가 단순 전달자가 되므로 필요한 정보에 한해 적용한다.
근거: refactoring-hidedelegate — Hide Delegate

[R1-OOD-05]
같은 이유로 바뀌는 것은 한데 모으고, 다른 이유로 바뀌는 것은 떼어 놓는다는 것이 단일 책임 원칙의 요지다. 변경 요구는 대개 특정한 역할의 사람이나 업무에서 나오므로, 한 모듈이 여러 출처의 요구를 함께 받으면 한쪽의 변경이 다른 쪽의 동작을 건드리게 된다. 모듈의 요소들이 하나의 변경 이유를 공유할수록 수정 범위를 예측하기 쉽고, 관련 없는 부분을 다시 검증할 필요가 줄어든다.
근거: martin-srp — Gather together the things that change for the same reasons

[R1-OOD-06]
의존관계 역전 원칙은 상위 수준 모듈이 하위 수준 모듈에 의존하지 않고 둘 다 추상화에 의존해야 하며, 추상화가 세부 사항에 의존하지 않아야 한다는 원칙이다. 상위 모듈이 필요로 하는 동작을 추상화로 정의하고 그 추상화를 상위 모듈과 함께 두면, 하위 모듈은 그 추상화를 구현하는 쪽이 된다. 실행 중의 호출은 여전히 상위에서 하위로 가지만 소스 코드의 의존은 하위에서 상위의 추상화를 향한다.
근거: fowler-dip — DIP in the Wild: Dependency Inversion Principle

[R1-OOD-07]
Properties는 Hashtable을 상속하므로 put과 putAll을 호출할 수 있지만, 이 메서드들은 문자열이 아닌 키나 값을 넣을 수 있게 하므로 사용하지 않도록 강하게 권고된다. 상속은 상위 클래스의 모든 공개 연산을 하위 타입의 연산으로 만들기 때문에, 하위 타입이 지키려는 제약을 상위 연산이 우회할 수 있다. 다른 객체를 내부에 두고 필요한 연산만 위임하는 구성은 공개할 연산과 타입을 직접 정할 수 있게 한다.
근거: api-properties — Properties 클래스 설명: Their use is strongly discouraged

[R1-OOD-08]
컬렉션 필드를 반환하는 접근자가 원본 컬렉션을 그대로 내주면, 그 컬렉션을 가진 객체가 모르는 사이에 내용이 바뀔 수 있다. 컬렉션 캡슐화는 추가와 삭제를 소유 객체의 메서드로 제공하고, 접근자는 원본을 변경할 수 없는 형태로 돌려주게 하는 정리 방법이다. 변경이 반드시 소유 객체를 거치게 되므로 개수 제한이나 중복 금지 같은 규칙을 한 곳에서 검사할 수 있다.
근거: refactoring-encapcoll — Encapsulate Collection

[R1-OOD-09]
여러 곳에서 같은 두세 개의 값이 함께 나타난다면 그 값들은 하나의 객체가 되어야 한다는 신호다. 값들을 클래스로 묶으면 그 값에 대한 검증과 계산을 옮겨 올 자리가 생기고, 매개변수 목록이 짧아진다. 묶음에서 값 하나를 뺐을 때 나머지가 의미를 잃는다면 그것들은 한 개념에 속한다고 볼 수 있다. 이렇게 찾아낸 개념은 생성 시점에 규칙을 검사해 유효하지 않은 상태가 퍼지는 것을 막는다.
근거: fowler-dataclump — Data Clump

출처
fowler-injection: https://martinfowler.com/articles/injection.html
fowler-telldontask: https://martinfowler.com/bliki/TellDontAsk.html
refactoring-hidedelegate: https://refactoring.com/catalog/hideDelegate.html
martin-srp: https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html
fowler-dip: https://martinfowler.com/articles/dipInTheWild.html
api-properties: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Properties.html
refactoring-encapcoll: https://refactoring.com/catalog/encapsulateCollection.html
fowler-dataclump: https://martinfowler.com/bliki/DataClump.html

### r1-software-engineering

변경·협업·빌드 보강 근거 · r1-v1 · 언어 중립 설계 사례; 코드 예시는 Java 21

변경·협업·빌드 학습 근거 · r1-v1
기준: 언어 중립 설계 사례; 코드 예시는 Java 21
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-SWE-01]
조건문에는 두 분기가 모두 정상 동작인 경우와, 한쪽이 정상이고 다른 쪽이 드문 예외 상황인 경우가 있다. 후자라면 예외 상황을 먼저 검사해 곧바로 반환하는 보호 구문으로 바꾸는 편이 의도를 잘 드러낸다. 보호 구문은 이 경우가 본 흐름이 아니어서 여기서 끝낸다는 뜻을 전하고, 남은 코드의 중첩을 없앤다. 두 분기가 대등할 때는 if와 else로 나란히 두는 것이 그 대등함을 표현한다.
근거: refactoring-guard — Replace Nested Conditional with Guard Clauses

[R1-SWE-02]
함수를 나누는 기준은 길이보다 의도와 구현의 분리다. 코드 조각이 무엇을 하는지 알아내려고 들여다봐야 한다면, 그 조각을 함수로 추출하고 하는 일을 이름으로 붙인다. 그러면 호출하는 쪽을 읽을 때 이름만으로 뜻이 전달되고, 구현이 궁금할 때만 안으로 들어가면 된다. 한 함수 안의 문장들이 같은 수준의 일을 말하도록 맞추면 전체 흐름이 순서대로 읽힌다.
근거: fowler-functionlength — FunctionLength: intention and implementation

[R1-SWE-03]
메서드는 결과를 돌려주되 시스템의 관찰 가능한 상태를 바꾸지 않는 조회와, 상태를 바꾸는 명령으로 나눌 수 있다. 조회 메서드는 부수 효과가 없으므로 어디서 몇 번을 호출해도 안전하고 순서를 바꿔도 된다. 상태를 바꾸는 메서드를 조회처럼 보이게 만들면 이 기대가 깨진다. 스택의 pop처럼 값을 돌려주면서 상태를 바꾸는 편이 유용한 경우도 있으므로, 그런 예외에서는 이름이 변경을 분명히 드러내야 한다.
근거: fowler-cqs — CommandQuerySeparation: do not change the observable state of the system

[R1-SWE-04]
코드에 직접 적힌 숫자나 문자열은 그 값이 왜 그 값인지 말해 주지 않는다. 매직 리터럴 교체는 값에 의미를 설명하는 이름의 상수를 붙여, 읽는 사람이 값이 아니라 뜻을 보게 하는 정리 방법이다. 같은 뜻의 값이 여러 곳에 흩어져 있을 때는 한 번의 정의로 모아 변경 지점을 줄인다. 값 자체가 이미 뜻을 충분히 전하는 경우에는 이름이 정보를 더하지 못하므로 바꾸지 않는다.
근거: refactoring-magic — Replace Magic Literal

[R1-SWE-05]
코드는 시스템이 실제로 하는 일을 가장 정확하고 상세하게 담은 문서이며, 그래서 코드를 읽기 쉽게 만드는 노력이 필요하다. 코드가 하는 일을 설명하는 별도 서술은 코드가 바뀔 때 함께 바뀌지 않으면 틀린 정보가 된다. 이름과 구조로 의도를 전달할 수 있는 부분은 코드 자체로 표현하고, 코드로 표현할 수 없는 배경과 결정 이유는 따로 기록해 보완한다.
근거: fowler-codeasdoc — Code As Documentation

[R1-SWE-06]
지금 필요하지 않은 기능을 위한 능력을 미리 넣으면 그것을 만드는 비용이 들고, 그동안 필요한 기능이 늦어지며, 추가된 복잡도를 계속 안고 가야 한다. 예상한 기능이 실제로 필요해지더라도 그 시점에는 요구에 대한 이해가 달라져 다시 손봐야 하는 경우가 많다. 이 원칙은 필요해졌을 때 고칠 수 있다는 전제에 기대므로, 코드를 고치기 쉽게 유지하는 리팩터링과 테스트는 미루는 대상이 아니다.
근거: fowler-yagni — Yagni: cost of build; cost of delay; cost of carry

[R1-SWE-07]
작은 변경은 더 빨리, 더 꼼꼼히 리뷰되고 결함이 들어갈 가능성이 낮으며, 거절되거나 되돌려도 버리는 작업이 적다. 적절한 크기의 변경은 한 가지 일만 다루는 자기 완결적인 변경이고, 관련 테스트를 포함하며, 병합된 뒤에도 시스템이 정상 동작한다. 리팩터링은 기능 변경이나 버그 수정과 분리해 올리는 편이 각 변경을 이해하기 쉽게 한다. 줄 수에 절대 기준은 없지만, 리뷰어가 한 번에 이해할 수 있는 범위를 넘으면 나눌 방법을 찾는다.
근거: google-smallcl — Small CLs: Why Write Small CLs?; What is Small?

출처
refactoring-guard: https://refactoring.com/catalog/replaceNestedConditionalWithGuardClauses.html
fowler-functionlength: https://martinfowler.com/bliki/FunctionLength.html
fowler-cqs: https://martinfowler.com/bliki/CommandQuerySeparation.html
refactoring-magic: https://refactoring.com/catalog/replaceMagicLiteral.html
fowler-codeasdoc: https://martinfowler.com/bliki/CodeAsDocumentation.html
fowler-yagni: https://martinfowler.com/bliki/Yagni.html
google-smallcl: https://google.github.io/eng-practices/review/developer/small-cls.html

### r1-spring-framework

Spring Framework 보강 근거 · r1-v1 · Spring Framework 7.0.x

Spring Framework 학습 근거 · r1-v1
기준: Spring Framework 7.0.x
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-SPR-01]
Spring MVC는 프런트 컨트롤러 패턴으로 설계되어 DispatcherServlet이 요청 처리의 공통 흐름을 제공하고 실제 작업은 설정 가능한 위임 컴포넌트가 수행한다. HandlerMapping은 요청을 핸들러와 전후 처리용 인터셉터 목록에 대응시키며 대응 기준은 구현마다 다르다. HandlerAdapter는 핸들러가 실제로 어떻게 호출되는지와 무관하게 DispatcherServlet이 핸들러를 호출하도록 돕는다. 애노테이션 컨트롤러 호출에 필요한 애노테이션 해석 같은 세부를 DispatcherServlet으로부터 가려 준다.
근거: sf-mvc-special — Special Bean Types: Map a request to a handler along with a list of interceptors; Help the DispatcherServlet to invoke a handler mapped to a request

[R1-SPR-02]
@ResponseBody를 메서드에 붙이면 반환값이 HttpMessageConverter를 통해 응답 본문으로 직렬화된다. 클래스에 붙이면 모든 컨트롤러 메서드에 적용되며, @RestController는 @Controller와 @ResponseBody를 합친 애노테이션이다. 이 표시가 없는 컨트롤러 메서드의 문자열 반환값은 뷰 이름으로 다뤄져 뷰 리졸버가 실제 뷰를 결정하고, 뷰가 모델을 사용해 응답을 만든다. 요청 본문을 객체로 읽는 @RequestBody도 같은 컨버터 체계를 사용한다.
근거: sf-mvc-responsebody — @ResponseBody: serialized to the response body

[R1-SPR-03]
플래시 속성은 한 요청이 다른 요청에서 쓸 속성을 저장하는 방법이며 redirect할 때 주로 필요하다. 대표적인 경우가 Post-Redirect-Get 패턴이다. 플래시 속성은 redirect 전에 보통 세션에 잠시 저장되고, redirect 뒤의 요청에서 사용할 수 있게 된 다음 즉시 제거된다. 변경 요청을 처리한 뒤 조회 주소로 redirect하면 브라우저의 마지막 요청이 GET이 되어, 화면을 다시 불러와도 변경 요청이 반복되지 않는다.
근거: sf-mvc-redirect — Flash Attributes: Post-Redirect-Get pattern; saved temporarily before the redirect

[R1-SPR-04]
Errors나 BindingResult 인자는 명령 객체의 검증과 데이터 바인딩에서 생긴 오류, 또는 @RequestBody 인자 검증의 오류에 접근하게 해 준다. 이 인자는 검증되는 메서드 인자 바로 다음에 선언해야 한다. 데이터 바인딩이나 검증에서 오류가 생기면 기본 동작은 예외를 발생시키는 것이지만, 바로 옆에 BindingResult를 두면 컨트롤러 메서드 안에서 오류를 직접 다룰 수 있다. 이때 컨트롤러는 오류 여부를 확인해 폼을 다시 보여 줄지 다음 단계로 넘어갈지 결정한다.
근거: sf-mvc-args — Method Arguments: immediately after the validated method argument

[R1-SPR-05]
Bean Validation은 제약 선언과 메타데이터를 통해 검증하는 공통 방법을 제공한다. 도메인 모델의 프로퍼티에 선언적 검증 제약을 붙이면 런타임이 그 제약을 적용한다. 내장 제약 외에 사용자 정의 제약도 만들 수 있다. 이런 제약은 대상 객체가 가진 값으로 판단하는 검사에 맞는다. 저장소 조회 결과나 다른 요청과의 경쟁에 좌우되는 규칙은 값 검사만으로 보장할 수 없으므로, 상태를 변경하는 유스케이스 안에서 확인해야 한다.
근거: sf-validation — Java Bean Validation: Overview of Bean Validation; declarative validation constraints

[R1-SPR-06]
HandlerInterceptor는 핸들러 실행 전에 불리는 preHandle, 핸들러 실행 뒤의 postHandle, 요청이 모두 끝난 뒤의 afterCompletion을 구현할 수 있다. preHandle이 false를 반환하면 나머지 실행 체인을 건너뛰고 핸들러도 호출되지 않는다. 인터셉터는 애노테이션 컨트롤러의 경로 매칭과 어긋날 가능성이 있어 보안 계층으로는 적합하지 않다. Spring Security를 쓰거나 서블릿 필터 체인에 통합된 방식으로 가능한 한 일찍 적용하는 것이 권장된다.
근거: sf-mvc-interceptor — Interception: callback before the actual handler is run; not ideally suited as a security layer

[R1-SPR-07]
HandlerMethodArgumentResolver는 주어진 요청의 맥락에서 메서드 매개변수를 인자 값으로 풀어내는 전략 인터페이스다. supportsParameter는 해당 매개변수를 이 리졸버가 지원하는지를 반환하고, resolveArgument는 요청으로부터 매개변수에 넣을 인자 값을 만들어 낸다. 컨트롤러 메서드가 선언한 각 매개변수는 그것을 지원하는 리졸버가 처리한다. 리졸버는 매개변수 단위로 동작하므로, 특정 요청 전체에 적용해야 하는 검사는 요청 처리 흐름의 앞 단계에서 수행해야 누락되지 않는다.
근거: sf-argresolver-api — HandlerMethodArgumentResolver: Strategy interface for resolving method parameters into argument values

[R1-SPR-08]
배열이나 컬렉션 타입의 필드와 생성자 인자에 @Autowired를 쓰면 해당 타입의 빈을 모두 주입받을 수 있다. 키 타입이 String인 Map도 주입받을 수 있으며, 이때 값은 기대한 타입의 모든 빈이고 키는 각 빈의 이름이다. 주입되는 대상은 컨테이너에 등록된 빈 전체에서 타입으로 고르므로, 구현 클래스를 새로 등록하면 주입 결과에도 포함된다. 순서가 필요한 컬렉션은 Ordered 인터페이스나 @Order로 정렬 기준을 지정한다.
근거: sf-autowired — Using @Autowired: the expected key type is String

[R1-SPR-09]
생성자 기반 주입과 setter 기반 주입은 함께 쓸 수 있다. 필수 의존성에는 생성자를, 선택 의존성에는 setter나 설정 메서드를 쓰는 것이 좋은 기준이다. 생성자 주입은 컴포넌트를 불변 객체로 구현하게 하고 필수 의존성이 null이 아님을 보장한다. 생성자로 주입받은 컴포넌트는 항상 완전히 초기화된 상태로 사용하는 코드에 전달된다. 생성자 인자가 지나치게 많다면 클래스가 너무 많은 책임을 가졌다는 신호이므로 책임을 나누는 리팩터링을 검토한다.
근거: sf-di — Dependency Injection: Constructor-based or setter-based DI?; immutable objects

[R1-SPR-10]
애스펙트는 여러 클래스에 걸친 관심사를 모듈화한 것이며 트랜잭션 관리가 대표적인 횡단 관심사다. 조인 포인트는 메서드 실행이나 예외 처리 같은 프로그램 실행 중의 한 지점이고, Spring AOP에서 조인 포인트는 항상 메서드 실행을 나타낸다. 어드바이스는 특정 조인 포인트에서 애스펙트가 취하는 동작이며 around, before, after 같은 종류가 있다. 포인트컷은 조인 포인트를 고르는 조건으로, 어드바이스는 포인트컷 표현식과 연결되어 그에 맞는 조인 포인트에서 실행된다.
근거: sf-aop-intro — AOP Concepts: A modularization of a concern that cuts across multiple classes; a join point always represents a method execution

[R1-SPR-11]
Spring AOP는 프록시 기반이다. 클라이언트가 프록시 참조를 통해 메서드를 호출하면 프록시가 해당 호출에 관련된 어드바이스를 실행한 뒤 대상 객체로 위임한다. 그러나 호출이 대상 객체에 도달한 뒤 대상 객체가 자기 자신에게 하는 호출은 프록시가 아닌 this 참조에 대해 실행된다. 따라서 명시적이든 암묵적이든 this를 통한 자기 호출은 어드바이스를 우회한다. 가장 좋은 대응은 자기 호출이 일어나지 않도록 코드를 리팩터링하는 것이고, 자기 참조를 주입받거나 현재 프록시를 직접 얻는 방법은 그다음 선택지다.
근거: sf-aop-proxying — Proxying Mechanisms: Understanding AOP Proxies; will bypass the advice

[R1-SPR-12]
Spring AOP는 대상 객체의 프록시를 만들 때 JDK 동적 프록시나 CGLIB을 사용한다. 대상 객체가 인터페이스를 하나 이상 구현하면 JDK 동적 프록시가 쓰이고 대상 타입이 구현한 모든 인터페이스가 프록시된다. 인터페이스를 구현하지 않으면 대상 타입의 하위 클래스를 실행 중에 생성하는 CGLIB 프록시가 만들어진다. CGLIB 프록시에서는 final 클래스를 확장할 수 없어 프록시할 수 없고, final 메서드는 재정의할 수 없어 어드바이스를 적용할 수 없다.
근거: sf-aop-proxying — Proxying Mechanisms: a JDK dynamic proxy is used; runtime-generated subclass of the target type

출처
sf-mvc-special: https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-servlet/special-bean-types.html
sf-mvc-responsebody: https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/responsebody.html
sf-mvc-redirect: https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/flash-attributes.html
sf-mvc-args: https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/arguments.html
sf-validation: https://docs.spring.io/spring-framework/reference/7.0/core/validation/beanvalidation.html
sf-mvc-interceptor: https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-servlet/handlermapping-interceptor.html
sf-argresolver-api: https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/web/method/support/HandlerMethodArgumentResolver.html
sf-autowired: https://docs.spring.io/spring-framework/reference/7.0/core/beans/annotation-config/autowired.html
sf-di: https://docs.spring.io/spring-framework/reference/7.0/core/beans/dependencies/factory-collaborators.html
sf-aop-intro: https://docs.spring.io/spring-framework/reference/7.0/core/aop/introduction-defn.html
sf-aop-proxying: https://docs.spring.io/spring-framework/reference/7.0/core/aop/proxying.html

### r1-jpa

Jakarta Persistence 보강 근거 · r1-v1 · Jakarta Persistence 3.2

Jakarta Persistence 학습 근거 · r1-v1
기준: Jakarta Persistence 3.2
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-JPA-01]
엔티티 사이의 관계는 일대일, 일대다, 다대일, 다대다일 수 있고 연관 필드나 프로퍼티에 관계 애노테이션으로 선언한다. 관계는 양방향이거나 단방향이며, 관계의 소유 측이 데이터베이스에 반영될 관계의 갱신을 결정한다. 다대일과 일대일 관계는 보통 외래 키 컬럼으로 매핑된다. 영속성 제공자는 이 매핑 정보를 사용해 객체 참조와 외래 키 값을 서로 변환하므로, 애플리케이션은 참조를 통해 연관 엔티티를 다룰 수 있다.
근거: jpa-spec — 2.11 Entity Relationships

[R1-JPA-02]
영속성 컨텍스트의 상태는 트랜잭션 커밋 시점에 데이터베이스와 동기화되며, 그 전에도 flush 호출이나 플러시 모드 설정에 따라 쓰기가 일어날 수 있다. 플러시 모드가 AUTO이면 영속성 제공자는 쿼리 결과에 영향을 줄 수 있는 엔티티 상태 변경이 그 쿼리에 보이도록 보장해야 한다. COMMIT 모드에서는 커밋 시점에 flush가 일어난다. flush로 보낸 변경도 트랜잭션이 롤백되면 함께 취소된다.
근거: jpa-spec — 3.3.4 Synchronization to the Database; 3.11.2 Queries and Flush Mode

[R1-JPA-03]
관리되는 엔티티 인스턴스는 영속 식별자를 가지고 현재 영속성 컨텍스트에 연결된 인스턴스다. 준영속 엔티티 인스턴스는 영속 식별자는 있지만 영속성 컨텍스트에 연결되어 있지 않다. 트랜잭션 범위의 영속성 컨텍스트가 끝나거나 엔티티를 분리하면 준영속 상태가 된다. 준영속 엔티티의 변경은 데이터베이스와 동기화되지 않는다. merge 연산은 준영속 엔티티의 상태를 같은 식별자의 관리되는 엔티티에 복사하고 그 관리되는 인스턴스를 반환하며, 인자로 넘긴 객체는 관리 상태가 되지 않는다.
근거: jpa-spec — 3.3.7 Detached Entities; 3.3.7.1 Merging Detached Entity State

[R1-JPA-04]
영속성 제공자는 매핑 메타데이터로부터 데이터베이스 스키마를 생성하거나, 생성과 삭제용 DDL 스크립트를 만들어 낼 수 있다. 데이터베이스에 대한 동작은 none, create, drop-and-create, drop, validate 가운데 지정하며, 지정하지 않으면 스키마 생성 동작을 수행하지 않는다. 삭제를 포함하는 동작은 기존 테이블과 데이터를 지운다. 보존해야 할 데이터가 있는 환경에서는 스크립트를 생성해 검토한 뒤 별도로 적용하고, 실행 시에는 validate로 매핑과 스키마의 일치만 확인하는 구성이 안전하다.
근거: jpa-spec — 9.4 Schema Generation; drop-and-create

[R1-JPA-05]
Column 애노테이션은 영속 필드나 프로퍼티가 매핑되는 컬럼을 지정한다. nullable 요소는 데이터베이스 컬럼이 null을 허용하는지를, length 요소는 길이로 매개변수화되는 타입의 컬럼 길이를 나타낸다. 한편 영속성 제공자는 엔티티의 저장 전, 수정 전, 삭제 전 생명주기 이벤트에 Bean Validation을 통한 자동 검증을 지원한다. 이 검증은 엔티티에 선언된 제약 애노테이션을 대상으로 하며 컬럼 매핑 요소와는 별개의 장치다.
근거: jpa-spec — 11.1.9 Column Annotation; 3.7.1 Automatic Validation Upon Lifecycle Events

[R1-JPA-06]
엔티티는 자신만의 영속 식별자를 갖지 않는 세분화된 클래스로 상태를 표현할 수 있다. 이런 임베디드 클래스의 인스턴스는 엔티티와 달리 영속 식별자가 없고, 자신이 속한 엔티티 상태의 일부로만 존재한다. 임베디드 객체는 소유 엔티티에 엄격히 속하며 영속 엔티티 사이에서 공유할 수 없다. 여러 엔티티가 임베디드 객체를 공유하려 할 때의 동작은 정의되어 있지 않으므로 인스턴스를 나눠 쓰지 않도록 설계한다.
근거: jpa-spec — 2.7 Embeddable Classes

[R1-JPA-07]
SELECT 목록에 생성자를 쓰면 조회 결과로 자바 클래스의 인스턴스를 반환받을 수 있다. 지정한 클래스는 엔티티일 필요가 없고 데이터베이스에 매핑되어 있지 않아도 되며, 생성자 이름은 완전한 클래스 이름으로 적는다. 엔티티가 아닌 클래스의 인스턴스는 영속성 컨텍스트의 관리 대상이 아니다. 엔티티 클래스를 생성자 이름으로 지정하더라도 만들어진 인스턴스는 관리 상태가 아니라 새 상태나 준영속 상태가 된다. 식별 변수로 엔티티를 조회한 결과는 영속성 컨텍스트가 관리하므로 이후의 변경이 동기화 대상이 된다.
근거: jpa-spec — 4.9.2 Constructor Expressions in the SELECT Clause

[R1-JPA-08]
벌크 수정은 데이터베이스의 수정 연산에 직접 대응하며 낙관적 잠금 검사를 거치지 않는다. 영속성 컨텍스트는 벌크 수정이나 삭제의 결과와 동기화되지 않는다. 따라서 벌크 연산은 데이터베이스와 활성 영속성 컨텍스트의 엔티티 사이에 불일치를 만들 수 있다. 일반적으로 벌크 연산은 새 영속성 컨텍스트의 트랜잭션 안에서만 수행하거나, 영향을 받을 수 있는 엔티티를 조회하거나 접근하기 전에 수행해야 한다. 버전 컬럼 값도 자동으로 올라가지 않으므로 필요하면 직접 갱신한다.
근거: jpa-spec — 4.11 Bulk Update and Delete Operations

[R1-JPA-09]
쿼리의 FETCH JOIN 절은 쿼리 실행의 부수 효과로 연관이나 요소 컬렉션을 즉시 가져오게 한다. fetch join은 내부 조인이거나 왼쪽 외부 조인이어야 한다. 일반 조인은 연관된 엔티티를 조건에 사용할 수 있게 할 뿐 로딩 여부를 바꾸지 않는다. LAZY 전략은 데이터에 처음 접근할 때 가져오라는 영속성 제공자에 대한 힌트이고, EAGER 전략은 반드시 즉시 가져와야 한다는 요구다. 지연 로딩 연관을 반복해서 접근하면 접근할 때마다 조회가 일어날 수 있다.
근거: jpa-spec — 4.4.5.3 Fetch Joins; results in eager fetching of an association

[R1-JPA-10]
양방향 관계는 소유 측과 반대 측을 가지며 단방향 관계는 소유 측만 가진다. 관계의 소유 측이 데이터베이스에서 그 관계의 갱신을 결정한다. 양방향 관계의 반대 측은 mappedBy 요소로 소유 측의 필드를 가리켜야 한다. 일대다와 다대일의 양방향 관계에서는 다 쪽이 반드시 소유 측이므로 mappedBy는 ManyToOne 쪽에 지정할 수 없다. 실행 중 객체 사이의 참조를 일관되게 유지하는 책임은 애플리케이션에 있으므로, 양쪽 참조를 함께 바꾸는 코드를 직접 작성해야 한다.
근거: jpa-spec — 2.11 Entity Relationships; The owning side of a relationship determines the updates

[R1-JPA-11]
컬렉션에 대한 fetch join이 포함된 쿼리에 setMaxResults나 setFirstResult를 적용한 효과는 정의되어 있지 않다. 컬렉션을 조인한 쿼리의 결과에는 부모 엔티티가 자식 원소 수만큼 반복해 나타나기 때문에, 행 단위의 범위 지정은 부모 엔티티의 개수와 일치하지 않는다. 단일 값 연관에 대한 fetch join은 결과 행 수를 바꾸지 않는다. 따라서 범위를 지정하는 쿼리는 부모 엔티티와 단일 값 연관만 다루고, 컬렉션은 조회된 부모들의 식별자를 조건으로 다시 읽는 구성이 예측 가능하다.
근거: jpa-spec — 4.4.5.3 Fetch Joins; fetch joins over collections is undefined

출처
jpa-spec: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html

### r1-database

DB 모델·기반 보강 근거 · r1-v1 · 관계형 모델; PostgreSQL 17

DB 모델·기반 학습 근거 · r1-v1
기준: 관계형 모델; PostgreSQL 17
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-DB-01]
한 테이블은 외래 키 제약을 둘 이상 가질 수 있고, 이는 테이블 사이의 다대다 관계를 구현하는 데 쓰인다. 상품과 주문이 있을 때 한 주문이 여러 상품을 포함하게 하려면, 주문 번호와 상품 번호를 각각 참조하는 주문 항목 테이블을 두고 두 컬럼을 함께 기본키로 삼는다. 외래 키는 참조하는 행이 반드시 존재하도록 보장하므로 없는 주문이나 없는 상품을 가리키는 항목은 만들 수 없다. 참조되는 행을 지울 때의 동작은 삭제 금지나 연쇄 삭제 같은 정책으로 지정한다.
근거: pg-constraints — 5.5.5 Foreign Keys; many-to-many relationships between tables

[R1-DB-02]
CHECK 제약은 가장 일반적인 제약으로, 컬럼 값이 불리언 식을 만족해야 함을 지정한다. 제약을 위반하는 값을 넣으려 하면 오류가 발생한다. 외래 키 제약은 한 컬럼의 값이 다른 테이블의 어떤 행에 나타나는 값과 일치해야 함을 지정하며, 이를 두 테이블 사이의 참조 무결성이라 한다. 허용 값을 별도 테이블의 행으로 두고 외래 키로 참조하면, 허용 값의 집합을 데이터로 관리하면서도 그 밖의 값은 데이터베이스가 거부한다.
근거: pg-constraints — 5.5.1 Check Constraints; 5.5.5 Foreign Keys

[R1-DB-03]
부분 인덱스는 테이블의 일부 행에 대해서만 만든 인덱스이며 대상 행은 조건식으로 정한다. 유일 인덱스를 부분 인덱스로 만들면 조건을 만족하는 행들 사이에서만 유일성이 강제되고, 조건을 만족하지 않는 행은 제약을 받지 않는다. 이 방법으로 여러 상태의 행이 섞여 있는 테이블에서 특정 상태의 행에 대해서만 중복을 막을 수 있다. 조건에 해당하지 않는 행은 인덱스에 들어가지 않으므로, 그 행을 찾는 쿼리에는 이 인덱스가 쓰이지 않는다.
근거: pg-partial — 11.8 Partial Indexes; Setting up a Partial Unique Index

[R1-DB-04]
CPU 코어 하나는 한 번에 하나의 스레드만 실행하므로, 코어 수보다 많은 커넥션이 동시에 쿼리를 실행하면 시분할에 따른 문맥 전환 비용이 생긴다. 디스크와 네트워크 대기 때문에 코어 수보다 조금 많은 커넥션이 유리할 수는 있지만 그 여유에는 한계가 있다. 그래서 풀은 작게 유지하고, 커넥션을 기다리는 스레드로 포화시키는 구성이 권장된다. 한 스레드가 여러 커넥션을 동시에 얻어야 하는 구조는 풀 고갈로 인한 교착을 만들 수 있으므로 애플리케이션 수준에서 먼저 고친다.
근거: hikari-pool — About Pool Sizing: You want a small pool, saturated with threads waiting for connections

[R1-DB-05]
대기 서버는 주 서버의 쓰기 전 로그 기록 흐름을 읽어 최신 상태를 유지할 수 있다. 주 서버에 장애가 나면 대기 서버는 주 서버의 데이터 대부분을 가지고 있어 빠르게 새 주 서버가 될 수 있다. 이 방식은 동기식이나 비동기식으로 구성할 수 있고 데이터베이스 서버 전체 단위로만 적용된다. 동기식은 커밋이 다른 서버에도 반영된 뒤에야 완료로 간주되어 유실이 없지만 응답이 느려지고, 비동기식은 커밋과 전파 사이에 지연이 있어 장애 조치 때 일부 트랜잭션이 유실될 수 있다.
근거: pg-ha — 26.1 Comparison of Different Solutions; Write-Ahead Log Shipping

출처
pg-constraints: https://www.postgresql.org/docs/17/ddl-constraints.html
pg-partial: https://www.postgresql.org/docs/17/indexes-partial.html
hikari-pool: https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
pg-ha: https://www.postgresql.org/docs/17/different-replication-solutions.html

### r1-transactions-and-concurrency

DB 트랜잭션·경쟁 보강 근거 · r1-v1 · PostgreSQL 17

DB 트랜잭션·경쟁 학습 근거 · r1-v1
기준: PostgreSQL 17
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-TX-01]
트랜잭션의 핵심은 여러 단계를 전부 아니면 전무인 하나의 연산으로 묶는 것이다. 단계 사이의 중간 상태는 동시에 실행되는 다른 트랜잭션에 보이지 않고, 완료를 막는 장애가 생기면 어느 단계도 데이터베이스에 영향을 주지 않는다. 트랜잭션이 완료되어 확인 응답을 받은 뒤에는 그 직후에 장애가 나더라도 변경이 영구적으로 기록되어 있다. PostgreSQL에서는 SQL 명령들을 BEGIN과 COMMIT으로 둘러싸 트랜잭션을 만들고, 중간에 취소하려면 ROLLBACK을 쓴다.
근거: pg-tutorial-tx — 3.4 Transactions; all-or-nothing operation

[R1-TX-02]
명시적 잠금을 쓰면 둘 이상의 트랜잭션이 서로 상대가 원하는 잠금을 쥐고 있는 교착 상태가 생길 수 있다. PostgreSQL은 교착 상태를 자동으로 감지하고 관련된 트랜잭션 중 하나를 중단시켜 나머지가 완료되게 한다. 어느 트랜잭션이 중단될지는 예측하기 어렵다. 교착은 행 수준 잠금에서도 생긴다. 가장 좋은 방어는 데이터베이스를 쓰는 모든 애플리케이션이 여러 객체의 잠금을 일관된 순서로 얻도록 하는 것이다. 미리 확인할 수 없는 경우에는 교착으로 중단된 트랜잭션을 재시도해 처리한다.
근거: pg-locking — 13.3.4 Deadlocks

[R1-TX-03]
PostgreSQL은 다중 버전 모델로 데이터 일관성을 유지한다. 각 SQL 문은 기반 데이터의 현재 상태와 무관하게 얼마 전 시점의 데이터 스냅샷, 곧 데이터베이스의 한 버전을 본다. 이 방식은 같은 행을 동시에 갱신하는 다른 트랜잭션이 만든 일관되지 않은 데이터를 문장이 보지 않게 막아 준다. 잠금 대신 이 모델을 쓰는 가장 큰 이점은 조회를 위한 잠금과 쓰기를 위한 잠금이 충돌하지 않아 읽기가 쓰기를 막지 않고 쓰기가 읽기를 막지 않는다는 점이다. 행 수준 잠금은 같은 행에 대한 쓰기와 잠금 요청만 막는다.
근거: pg-mvcc-intro — 13.1 Introduction; reading never blocks writing and writing never blocks reading

[R1-TX-04]
저장점은 트랜잭션의 일부만 선택적으로 버리고 나머지는 커밋할 수 있게 한다. SAVEPOINT로 저장점을 정의한 뒤 필요하면 ROLLBACK TO로 그 지점까지 되돌릴 수 있다. 저장점 정의와 되돌리기 사이의 변경은 모두 버려지고 저장점 이전의 변경은 유지된다. 되돌린 뒤에도 저장점은 계속 정의되어 있어 여러 번 되돌릴 수 있다. 이 모든 일은 트랜잭션 블록 안에서 일어나므로 다른 세션에는 보이지 않는다. 오류 때문에 시스템이 중단 상태로 만든 트랜잭션 블록에서 전체를 롤백하지 않고 제어를 되찾는 방법은 ROLLBACK TO뿐이다.
근거: pg-tutorial-tx — 3.4 Transactions; selectively discard parts of the transaction; regain control of a transaction block

[R1-TX-05]
행 수준 잠금은 데이터 조회에는 영향을 주지 않고 같은 행에 대한 쓰기와 잠금 요청만 막는다. FOR UPDATE는 조회한 행을 수정할 때처럼 잠가, 현재 트랜잭션이 끝날 때까지 다른 트랜잭션이 그 행을 잠그거나 수정하거나 지우지 못하게 한다. FOR SHARE는 배타 잠금 대신 공유 잠금을 얻으며, 다른 트랜잭션의 수정과 삭제, 배타 잠금 요청을 막지만 다른 공유 잠금 요청은 허용한다. 교착을 피하려면 한 트랜잭션이 어떤 객체에 처음 얻는 잠금이 그 객체에 필요하게 될 가장 제한적인 모드가 되게 한다.
근거: pg-locking — 13.3.2 Row-Level Locks; the most restrictive mode

출처
pg-tutorial-tx: https://www.postgresql.org/docs/17/tutorial-transactions.html
pg-locking: https://www.postgresql.org/docs/17/explicit-locking.html
pg-mvcc-intro: https://www.postgresql.org/docs/17/mvcc-intro.html

### r1-sql-and-indexes

SQL·인덱스·쿼리 보강 근거 · r1-v1 · PostgreSQL 17

SQL·인덱스·쿼리 학습 근거 · r1-v1
기준: PostgreSQL 17
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-SQL-01]
INNER JOIN의 결과에는 왼쪽 테이블의 각 행에 대해 조인 조건을 만족하는 오른쪽 행마다 한 행이 들어간다. LEFT OUTER JOIN은 먼저 내부 조인을 수행하고, 그다음 오른쪽의 어떤 행과도 조인 조건을 만족하지 못한 왼쪽 행마다 오른쪽 컬럼이 NULL인 행을 추가한다. 따라서 결과에는 왼쪽 테이블의 각 행이 적어도 한 번 나타난다. RIGHT OUTER JOIN은 그 반대이고, FULL OUTER JOIN은 양쪽 모두에 대해 짝 없는 행을 추가한다.
근거: pg-table-expr — 7.2.1.1 Joined Tables; the joined table always has at least one row for each row in T1

[R1-SQL-02]
인덱스가 없으면 조건에 맞는 행을 찾기 위해 테이블 전체를 행 단위로 훑어야 한다. 조건 컬럼에 인덱스가 있으면 시스템은 검색 트리의 몇 단계만 내려가 대상 행을 찾을 수 있다. 인덱스가 만들어진 뒤에는 시스템이 테이블 변경에 맞춰 인덱스를 갱신하고, 순차 탐색보다 효율적이라고 판단할 때 쿼리에 인덱스를 사용한다. 인덱스를 테이블과 동기화해 두는 일은 데이터 변경 연산에 부담을 더한다. 그러므로 쿼리에 거의 쓰이지 않는 인덱스는 제거해야 한다.
근거: pg-indexes-intro — 11.1 Introduction; This adds overhead to data manipulation operations

[R1-SQL-03]
쿼리 플래너는 좋은 계획을 고르기 위해 쿼리가 가져올 행 수를 추정해야 한다. 이를 위해 테이블의 전체 행 수와 컬럼별 통계를 사용한다. 컬럼 통계에는 고유 값의 수, 가장 자주 나오는 값들과 그 빈도, 값의 분포를 나타내는 히스토그램 경계가 들어 있다. 조건에 맞는 행의 비율이 낮다고 추정되면 인덱스 탐색이, 높다고 추정되면 순차 탐색이 더 싼 계획이 된다. 통계는 ANALYZE로 갱신되며 표본을 바탕으로 한 근사값이다.
근거: pg-planner-stats — 14.2.1 Single-Column Statistics; most_common_vals

[R1-SQL-04]
B-tree는 어떤 순서로 정렬할 수 있는 데이터에 대한 동등 조건과 범위 조건을 처리한다. 플래너는 인덱스 컬럼이 작다, 작거나 같다, 같다, 크거나 같다, 크다 연산자로 비교될 때 B-tree 인덱스 사용을 고려한다. B-tree 인덱스는 데이터를 정렬된 순서로 가져오는 데에도 쓸 수 있다. 해시 인덱스는 인덱스 컬럼 값에서 얻은 32비트 해시 코드를 저장하므로 단순한 동등 비교만 처리할 수 있다. CREATE INDEX는 기본으로 B-tree 인덱스를 만든다.
근거: pg-index-types — 11.2.1 B-Tree; 11.2.2 Hash

[R1-SQL-05]
WHERE와 HAVING의 근본적인 차이는 적용 시점이다. WHERE는 그룹과 집계가 계산되기 전에 입력 행을 선택해 어떤 행이 집계 계산에 들어갈지를 제어하고, HAVING은 그룹과 집계가 계산된 뒤에 그룹 행을 선택한다. 따라서 WHERE 절에는 집계 함수가 올 수 없다. 반대로 HAVING 절은 거의 언제나 집계 함수를 포함한다. 집계를 쓰지 않는 조건을 HAVING에 쓸 수도 있지만, 같은 조건을 WHERE 단계에서 적용하면 조건에 맞지 않는 행을 그룹화하고 집계하는 일을 피할 수 있어 더 효율적이다.
근거: pg-tutorial-agg — 2.7 Aggregate Functions; The fundamental difference between WHERE and HAVING

[R1-SQL-06]
인덱스 전용 스캔은 테이블을 읽지 않고 인덱스만으로 쿼리에 답하는 방식이다. 이를 위해서는 쿼리가 인덱스에 저장된 컬럼만 참조해야 한다. PostgreSQL에서는 행의 가시성 정보가 인덱스가 아닌 테이블에 있으므로, 인덱스 전용 스캔은 해당 테이블 페이지의 가시성 맵 비트를 확인한다. 비트가 설정되어 있으면 테이블을 읽지 않고 값을 반환하고, 그렇지 않으면 테이블 항목을 방문해 확인한다. 자주 실행하는 쿼리에 필요한 컬럼을 포함하도록 설계한 인덱스를 커버링 인덱스라 하며, INCLUDE 절로 검색 키가 아닌 컬럼을 덧붙일 수 있다.
근거: pg-index-only — 11.9 Index-Only Scans and Covering Indexes; The query must reference only columns stored in the index

[R1-SQL-07]
count(*)는 입력 행의 수를 계산한다. 인자를 가진 count는 입력 값이 NULL이 아닌 입력 행의 수를 계산한다. 대부분의 집계 함수는 NULL 입력을 무시하므로 한 컬럼에 NULL이 있으면 행의 수와 값의 수가 달라진다. NULL은 알 수 없는 값을 나타내어 등호 비교의 결과가 참이 되지 않으므로, NULL인 행을 고르려면 IS NULL 술어를 쓴다. 선택된 행이 하나도 없을 때 count는 0을 반환하지만 sum 같은 다른 집계 함수는 NULL을 반환한다는 점도 구분해야 한다.
근거: pg-aggregate — 9.21 Aggregate Functions; Computes the number of input rows in which the input value is not null

[R1-SQL-08]
UNION은 두 번째 쿼리의 결과를 첫 번째 쿼리의 결과에 이어 붙이며, 행이 실제로 그 순서로 반환된다는 보장은 없다. 또한 UNION ALL을 쓰지 않는 한 DISTINCT와 같은 방식으로 결과에서 중복 행을 제거한다. 두 쿼리의 합집합을 계산하려면 두 쿼리가 같은 수의 컬럼을 반환하고 대응하는 컬럼이 호환되는 데이터 타입을 가져야 한다. 중복 제거는 결과 전체를 비교해야 하는 추가 작업이므로, 중복이 생길 수 없거나 남아도 되는 경우에는 UNION ALL이 같은 결과를 더 적은 비용으로 낸다.
근거: pg-union — 7.4 Combining Queries; eliminates duplicate rows from its result

[R1-SQL-09]
EXPLAIN의 ANALYZE 옵션으로 플래너 추정의 정확도를 확인할 수 있다. 이 옵션을 주면 EXPLAIN이 쿼리를 실제로 실행한 뒤, 일반 EXPLAIN이 보여 주는 추정값과 함께 각 계획 노드에서 누적된 실제 행 수와 실제 실행 시간을 표시한다. 플래너는 추정한 전체 비용이 가장 낮은 계획을 고르므로, 추정 행 수와 실제 행 수의 큰 차이는 잘못된 계획 선택의 단서가 된다. EXPLAIN ANALYZE는 쿼리를 실제로 실행하므로 부수 효과가 그대로 발생한다. 데이터를 바꾸는 쿼리는 트랜잭션 안에서 실행한 뒤 롤백해 분석할 수 있다.
근거: pg-explain — 14.1.2 EXPLAIN ANALYZE; EXPLAIN actually executes the query

[R1-SQL-10]
B-tree 인덱스는 정렬 가능한 데이터의 동등 조건과 범위 조건에 쓰인다. 패턴이 상수이고 문자열의 시작에 고정되어 있으면 LIKE 같은 패턴 일치 연산에도 B-tree 인덱스를 쓸 수 있다. 앞부분이 정해진 패턴은 범위 조건으로 바꿔 볼 수 있지만, 임의의 문자로 시작하는 패턴은 그렇지 않다. 데이터베이스가 C 로케일을 쓰지 않으면 패턴 일치를 지원하는 별도의 연산자 클래스로 인덱스를 만들어야 한다. 컬럼 값에 함수나 식을 적용한 결과로 검색하는 경우에는 그 식을 인덱스 대상으로 삼는 식 인덱스를 만든다.
근거: pg-index-types — 11.2.1 B-Tree; anchored to the beginning of the string

출처
pg-table-expr: https://www.postgresql.org/docs/17/queries-table-expressions.html
pg-indexes-intro: https://www.postgresql.org/docs/17/indexes-intro.html
pg-planner-stats: https://www.postgresql.org/docs/17/planner-stats.html
pg-index-types: https://www.postgresql.org/docs/17/indexes-types.html
pg-tutorial-agg: https://www.postgresql.org/docs/17/tutorial-agg.html
pg-index-only: https://www.postgresql.org/docs/17/indexes-index-only-scans.html
pg-aggregate: https://www.postgresql.org/docs/17/functions-aggregate.html
pg-union: https://www.postgresql.org/docs/17/queries-union.html
pg-explain: https://www.postgresql.org/docs/17/using-explain.html

### r1-data-structures

자료구조 보강 근거 · r1-v1 · Princeton Algorithms 4th Edition; 코드 예시는 Java 21

자료구조 학습 근거 · r1-v1
기준: Princeton Algorithms 4th Edition; 코드 예시는 Java 21
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-DS-01]
스택은 가장 나중에 넣은 원소를 먼저 꺼내는 후입선출 구조이고, 큐는 가장 먼저 넣은 원소를 먼저 꺼내는 선입선출 구조다. 스택에 넣은 원소들을 모두 꺼내 다른 스택에 차례로 넣으면 꺼내는 순서가 처음 넣은 순서와 같아진다. 입력용 스택과 출력용 스택을 두고 출력용이 비었을 때만 한꺼번에 옮기면, 각 원소는 스택 연산을 일정한 횟수만 거친다. 따라서 연산을 여러 번 수행한 전체 비용은 연산 수에 비례하고, 한 번의 연산이 오래 걸리는 경우가 있어도 평균 비용은 상수로 유지된다.
근거: algs-stacks — 1.3 Bags, Queues, and Stacks

[R1-DS-02]
이진 탐색 트리에서 알고리즘의 실행 시간은 트리의 모양에 달려 있고, 모양은 키가 삽입되는 순서에 달려 있다. 가장 좋은 경우 트리는 완전히 균형이 잡혀 루트에서 가장 먼 노드까지의 거리가 키 수의 로그에 비례한다. 가장 나쁜 경우에는 탐색 경로에 모든 노드가 놓일 수 있다. 키가 무작위 순서로 삽입되면 평균적인 탐색 비용은 로그에 비례한다. 탐색과 삽입은 모두 루트에서 시작해 한 경로를 따라 내려가므로, 필요한 비교 횟수는 그 경로의 길이에 1을 더한 값을 넘지 않는다.
근거: algs-bst — 3.2 Binary Search Trees; depends on the order in which keys are inserted

[R1-DS-03]
이진 탐색 트리는 각 노드의 키가 왼쪽 서브트리의 모든 키보다 크고 오른쪽 서브트리의 모든 키보다 작다는 조건을 만족한다. 중위 순회는 왼쪽 서브트리의 키를 모두 출력하고, 루트의 키를 출력한 다음, 오른쪽 서브트리의 키를 모두 출력하는 재귀적 방법이다. 이 순서 조건 덕분에 중위 순회는 키를 정렬된 순서로 방문한다. 순회는 노드마다 일정한 작업만 하므로 전체 시간은 노드 수에 비례하고, 재귀 호출이 쌓이는 깊이는 루트에서 가장 깊은 노드까지의 경로 길이와 같다.
근거: algs-bst — 3.2 Binary Search Trees; inorder traversal

[R1-DS-04]
이진 탐색 트리는 어떤 노드의 키든 그 노드의 왼쪽 서브트리에 있는 모든 노드의 키보다 크고 오른쪽 서브트리에 있는 모든 노드의 키보다 작다는 제한을 만족하는 이진 트리다. 이 조건은 직계 자식뿐 아니라 서브트리에 속한 모든 노드에 적용된다. 탐색은 이 조건을 믿고 매 단계에서 한쪽 서브트리를 버리므로, 조건이 깨진 트리에서는 존재하는 키를 찾지 못할 수 있다. 조건을 검사하려면 각 노드의 키가 조상들로부터 정해지는 하한과 상한 사이에 있는지 확인해야 한다.
근거: algs-bst — 3.2 Binary Search Trees; larger than the keys in all nodes in that node's left subtree

[R1-DS-05]
LinkedHashMap은 모든 항목을 잇는 이중 연결 리스트를 유지하는 해시 테이블 구현이며 이 리스트가 순회 순서를 정한다. 생성자 인자로 순서를 접근 순서로 지정하면 가장 오래전에 접근한 항목부터 가장 최근에 접근한 항목 순으로 유지되어 최근 사용 순서 기반 캐시를 만드는 데 알맞다. 이 모드에서는 조회도 순서를 바꾸는 구조 변경이다. removeEldestEntry 메서드를 재정의하면 새 항목이 추가될 때 가장 오래된 항목을 자동으로 제거하는 정책을 넣을 수 있다. 이 구현은 동기화되어 있지 않으므로 여러 스레드가 함께 쓰려면 외부에서 동기화해야 한다.
근거: api-lhm — LinkedHashMap: access-order; removeEldestEntry

[R1-DS-06]
균형 탐색 트리는 비용이 로그 수준임을 보장하는 이진 탐색 트리의 한 종류다. 2-3 탐색 트리는 모든 빈 링크까지의 거리가 같도록 완전한 균형을 유지하고, 레드-블랙 트리는 이를 이진 트리로 표현해 구현한다. 삽입 뒤에 균형 조건이 깨지면 회전과 색 뒤집기 같은 국소 변환으로 조건을 복원한다. 그 결과 키가 어떤 순서로 삽입되더라도 트리의 높이는 키 수의 로그에 비례하는 값을 넘지 않는다. 순서를 유지하는 구조이므로 최솟값, 순위, 범위 조회 같은 순서 기반 연산도 같은 수준의 비용으로 지원한다.
근거: algs-balanced — 3.3 Balanced Search Trees; costs are guaranteed to be logarithmic

출처
algs-stacks: https://algs4.cs.princeton.edu/13stacks/
algs-bst: https://algs4.cs.princeton.edu/32bst/
api-lhm: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html
algs-balanced: https://algs4.cs.princeton.edu/33balanced/

### r1-algorithms

알고리즘 보강 근거 · r1-v1 · Princeton Algorithms 4th Edition

알고리즘 학습 근거 · r1-v1
기준: Princeton Algorithms 4th Edition
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-ALG-01]
알고리즘의 실행 시간은 입력 크기의 함수로 나타내며, 그 함수에서 가장 빠르게 커지는 항이 증가 차수를 정한다. 증가 차수가 선형이면 입력이 두 배가 될 때 시간도 두 배가 되고, 제곱이면 네 배가 된다. 이중 반복문으로 모든 쌍을 처리하는 프로그램은 제곱 차수의 전형적인 예다. 증가 차수는 입력 크기가 클 때의 경향을 예측하게 해 주지만, 상수 계수와 낮은 차수 항은 무시하므로 작은 입력에서의 실제 시간 비교에는 측정이 필요하다. 메모리 사용량도 같은 방식으로 입력 크기의 함수로 분석한다.
근거: algs-analysis — 1.4 Analysis of Algorithms; order of growth

[R1-ALG-02]
동적 연결성 문제는 두 원소를 연결하는 연산과 두 원소가 연결되어 있는지 묻는 연산으로 이루어진다. 퀵 유니온은 각 원소가 같은 집합의 다른 원소를 가리키게 해 트리를 만들고, 루트가 집합을 대표한다. 가중 퀵 유니온은 두 트리를 합칠 때 항상 작은 트리를 큰 트리에 연결해 트리의 높이가 원소 수의 로그를 넘지 않게 한다. 여기에 경로 압축을 더하면 탐색하며 지나간 노드를 루트에 직접 연결해 트리가 거의 평평해지고, 연산당 상환 비용이 실용적으로는 상수에 가까워진다.
근거: algs-uf — 1.5 Case Study: Union-Find; Weighted quick-union with path compression

[R1-ALG-03]
다익스트라 알고리즘은 간선 가중치가 음수가 아닌 그래프에서 단일 출발점 최단 경로를 구한다. 트리에 속하지 않은 정점 가운데 출발점에서 가장 가까운 정점을 차례로 트리에 추가하고 그 정점에서 나가는 간선을 완화한다. 가중치가 음수가 아니라는 조건은 이미 추가된 정점의 거리가 이후에 줄어들지 않음을 보장한다. 음수 가중치는 이 보장을 깨뜨린다. 일반적인 가중치의 그래프에는 모든 간선을 반복해 완화하는 벨만-포드 알고리즘을 쓰며, 출발점에서 도달 가능한 음수 사이클이 있으면 최단 경로는 존재하지 않는다.
근거: algs-sp — 4.4 Shortest Paths; Negative weights introduce complications

[R1-ALG-04]
연결되지 않은 그래프는 여러 연결 요소로 이루어지며, 연결 요소는 더 넓힐 수 없는 연결된 부분 그래프다. 깊이 우선 탐색은 한 정점을 방문하면서 방문 표시를 하고, 아직 표시되지 않은 이웃 정점을 재귀적으로 방문한다. 한 정점에서 시작한 탐색은 그 정점과 연결된 모든 정점을 표시한다. 표시되지 않은 정점마다 탐색을 새로 시작하면 각 시작이 연결 요소 하나에 대응한다. 인접 리스트 표현에서 깊이 우선 탐색은 각 정점을 한 번, 각 간선을 두 번 확인하므로 걸리는 시간은 정점 수와 간선 수의 합에 비례한다.
근거: algs-graph — 4.1 Undirected Graphs; maximal connected subgraphs

[R1-ALG-05]
이분 탐색은 정렬된 배열에서 키를 찾는다. 배열의 가운데 원소와 키를 비교해, 키가 작으면 왼쪽 절반에서, 크면 오른쪽 절반에서 탐색을 계속하고 같으면 위치를 반환한다. 한 번의 비교로 탐색 범위가 절반으로 줄어들므로 필요한 비교 횟수는 배열 크기의 로그에 비례한다. 이 방법은 배열이 정렬되어 있을 때만 올바르다. 정렬되지 않은 데이터에 적용하려면 먼저 정렬해야 하고, 그 비용은 탐색 한 번의 비용보다 크다. 따라서 같은 데이터에 대해 탐색을 여러 번 수행할 때 정렬의 비용이 상쇄된다.
근거: algs-bsearch — 1.1 Programming Model; Binary search

출처
algs-analysis: https://algs4.cs.princeton.edu/14analysis/
algs-uf: https://algs4.cs.princeton.edu/15uf/
algs-sp: https://algs4.cs.princeton.edu/44sp/
algs-graph: https://algs4.cs.princeton.edu/41graph/
algs-bsearch: https://algs4.cs.princeton.edu/11model/

### r1-operating-system

운영체제 보강 근거 · r1-v1 · OSTEP (Unix 계열 개념); Linux man-pages

운영체제 학습 근거 · r1-v1
기준: OSTEP (Unix 계열 개념); Linux man-pages
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-OS-01]
프로세스는 세 상태 가운데 하나에 있다. 실행 상태에서는 프로세서에서 명령을 수행하고 있다. 준비 상태에서는 실행할 준비가 되어 있지만 운영체제가 어떤 이유로 지금은 실행하지 않기로 했다. 대기 상태에서는 다른 사건이 일어날 때까지 실행할 수 없게 만드는 연산을 수행한 상태이며, 디스크 입출력을 시작한 프로세스가 대표적이다. 준비에서 실행으로 옮겨지는 것을 스케줄된다고 하고 반대를 디스케줄된다고 한다. 입출력이 완료되면 프로세스는 대기에서 준비로 돌아가며, 어느 프로세스를 실행할지는 스케줄러가 결정한다.
근거: ostep-process — 4.4 Process States

[R1-OS-02]
스레드는 별도의 프로세스와 매우 비슷하지만 한 가지가 다르다. 스레드들은 같은 주소 공간을 공유하므로 같은 데이터에 접근할 수 있다. 각 스레드는 자신만의 프로그램 카운터와 레지스터 집합을 가지며, 한 프로세서에서 두 스레드를 번갈아 실행하려면 문맥 전환이 필요하지만 주소 공간은 그대로 유지된다. 다중 스레드 프로세스의 주소 공간에는 스레드마다 하나씩 스택이 있다. 스레드를 쓰는 이유는 여러 프로세서를 활용하는 병렬 처리와, 느린 입출력 때문에 프로그램 전체가 멈추는 일을 피하는 데 있다. 공유 데이터에 대한 접근은 서로 조율되어야 한다.
근거: ostep-threads — 26.1 Why Use Threads?; share the same address space

[R1-OS-03]
파이프와 FIFO는 단방향 프로세스 간 통신 통로를 제공한다. 파이프에는 읽는 쪽 끝과 쓰는 쪽 끝이 있고, 쓰는 쪽에 쓴 데이터를 읽는 쪽에서 읽을 수 있다. 빈 파이프에서 읽으려는 프로세스는 데이터가 올 때까지 대기하고, 가득 찬 파이프에 쓰려는 프로세스는 공간이 생길 때까지 대기한다. 파이프의 통신은 바이트 스트림이어서 메시지 경계라는 개념이 없다. 공유 메모리는 여러 프로세스가 같은 메모리 영역을 자신의 주소 공간에 대응시켜 쓰는 방식으로, 커널을 거치는 복사가 없는 대신 접근의 동기화를 프로세스들이 직접 해야 한다.
근거: man-pipe — pipe(7): unidirectional interprocess communication channel

[R1-OS-04]
세마포어는 정수 값을 가진 객체로, 두 루틴으로 조작한다. 대기 루틴은 세마포어 값을 하나 줄이고 값이 음수이면 호출자를 기다리게 한다. 신호 루틴은 값을 하나 올리고 기다리는 스레드가 하나 이상 있으면 하나를 깨운다. 값이 음수일 때 그 크기는 기다리는 스레드의 수와 같다. 초기값을 1로 두고 임계 영역 앞뒤에서 대기와 신호를 호출하면 락처럼 동작하며 이를 이진 세마포어라 한다. 초기값을 0으로 두면 한 스레드가 다른 스레드의 완료를 기다리는 순서 맞추기에 쓸 수 있고, 더 큰 값으로 두면 동시에 진입하는 스레드 수를 제한한다.
근거: ostep-sema — 31.2 Binary Semaphores (Locks)

[R1-OS-05]
교착 상태가 발생하려면 네 조건이 모두 성립해야 한다. 상호 배제는 스레드가 필요한 자원을 배타적으로 차지한다는 것이고, 점유 대기는 할당받은 자원을 쥔 채 추가 자원을 기다린다는 것이다. 비선점은 자원을 쥔 스레드로부터 강제로 빼앗을 수 없다는 것이며, 순환 대기는 각 스레드가 다음 스레드가 요청한 자원을 쥐고 있는 고리가 존재한다는 것이다. 넷 중 하나라도 성립하지 않으면 교착은 생기지 않는다. 가장 실용적인 예방법은 락 획득에 전체 순서를 정해 순환 대기가 생기지 않게 하는 것이다.
근거: ostep-bugs — OSTEP 32: Conditions for Deadlock; Circular wait

[R1-OS-06]
세그먼트는 크기가 가변적이어서 물리 메모리가 금세 작은 빈 구멍들로 채워지고, 새 세그먼트를 할당하거나 기존 세그먼트를 키우기 어려워진다. 이 문제를 외부 단편화라 한다. 빈 공간을 모으려고 세그먼트를 재배치하는 압축은 비용이 크다. 페이징은 공간을 고정 크기 단위로 나누는 다른 접근이다. 가상 주소 공간의 각 단위를 페이지, 물리 메모리의 각 단위를 페이지 프레임이라 하며, 운영체제는 프로세스마다 가상 페이지의 주소 변환을 저장하는 페이지 테이블을 유지한다. 고정 크기 단위를 쓰면 빈 프레임 목록에서 필요한 수만큼 꺼내 쓰면 되어 할당이 단순해진다.
근거: ostep-segmentation — OSTEP 16: external fragmentation

[R1-OS-07]
운영체제는 디스크에 페이지를 내보내고 다시 들여올 공간을 마련해 두며 이를 스왑 공간이라 한다. 스왑 공간 덕분에 운영체제는 동시에 실행되는 여러 프로세스에 실제보다 큰 가상 메모리가 있는 것처럼 보이게 할 수 있다. 페이지 테이블 항목의 존재 비트는 페이지가 물리 메모리에 있는지를 나타내며, 메모리에 없는 페이지에 접근하면 페이지 부재가 발생한다. 운영체제의 페이지 부재 처리기는 디스크에서 페이지를 읽어 와 페이지 테이블을 갱신한 뒤 명령을 재시도한다. 메모리가 가득 차 있으면 먼저 내보낼 페이지를 고르는 교체 정책이 실행된다.
근거: ostep-swap — 21.2 The Present Bit; swap space

[R1-OS-08]
프로세서는 사용자 모드와 커널 모드를 제공한다. 사용자 모드에서 실행되는 코드는 할 수 있는 일이 제한되어 입출력 요청을 직접 내릴 수 없고, 시도하면 프로세서가 예외를 일으킨다. 운영체제는 커널 모드에서 실행된다. 사용자 프로그램이 특권 연산을 수행하려면 시스템 콜을 호출해 트랩 명령을 실행하며, 이 명령은 커널로 뛰어드는 동시에 권한 수준을 커널 모드로 올린다. 작업이 끝나면 운영체제가 트랩 복귀 명령으로 사용자 모드로 되돌린다. 커널은 부팅 때 트랩 테이블을 설정해 어떤 사건에 어떤 코드를 실행할지 하드웨어에 알려 둔다.
근거: ostep-lde — 6.2 Problem #1: Restricted Operations

[R1-OS-09]
운영체제가 CPU 제어를 되찾는 방법은 타이머 인터럽트다. 타이머 장치가 일정 간격으로 인터럽트를 일으키면 실행 중이던 프로세스가 중단되고 미리 설정된 운영체제의 인터럽트 처리기가 실행된다. 운영체제가 다른 프로세스로 바꾸기로 하면 문맥 전환이라는 저수준 코드를 실행한다. 문맥 전환은 현재 실행 중인 프로세스의 레지스터 값들을 저장하고 곧 실행할 프로세스의 값들을 복원하는 일이다. 이렇게 하면 트랩 복귀 명령이 실행될 때 원래 프로세스가 아니라 다른 프로세스의 실행이 재개된다.
근거: ostep-lde — OSTEP 6: context switch; timer interrupt

출처
ostep-process: https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-intro.pdf
ostep-threads: https://pages.cs.wisc.edu/~remzi/OSTEP/threads-intro.pdf
man-pipe: https://man7.org/linux/man-pages/man7/pipe.7.html
ostep-sema: https://pages.cs.wisc.edu/~remzi/OSTEP/threads-sema.pdf
ostep-bugs: https://pages.cs.wisc.edu/~remzi/OSTEP/threads-bugs.pdf
ostep-segmentation: https://pages.cs.wisc.edu/~remzi/OSTEP/vm-segmentation.pdf
ostep-swap: https://pages.cs.wisc.edu/~remzi/OSTEP/vm-beyondphys.pdf
ostep-lde: https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-mechanisms.pdf

### r1-networks

네트워크 보강 근거 · r1-v1 · RFC 1122·9293·768·1034

네트워크 학습 근거 · r1-v1
기준: RFC 1122·9293·768·1034
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-NET-01]
인터넷 프로토콜 모음은 응용 계층, 전송 계층, 인터넷 계층, 링크 계층으로 이루어진다. 전송 계층은 애플리케이션에 종단 간 통신 서비스를 제공하며 TCP와 UDP가 여기에 속한다. 인터넷 계층의 IP는 비연결형 데이터그램 서비스로, 종단 간 전달을 보장하지 않아 데이터그램이 손상되거나 중복되거나 순서가 바뀌거나 아예 도착하지 않을 수 있다. 신뢰성이 필요하면 그 위의 계층이 책임진다. 링크 계층은 호스트가 직접 연결된 네트워크와 통신하기 위한 프로토콜을 담당한다. 각 계층은 아래 계층이 제공하는 서비스 위에서 자신의 기능을 구현한다.
근거: rfc1122 — 1.1.3 Internet Protocol Suite

[R1-NET-02]
UDP는 응용 프로그램이 최소한의 프로토콜 장치만으로 다른 프로그램에 메시지를 보내는 절차를 제공한다. 이 프로토콜은 트랜잭션 지향적이며 전달과 중복 방지를 보장하지 않는다. 데이터의 순서 있는 신뢰성 있는 전달이 필요한 응용은 TCP를 써야 한다. TCP는 연결 지향 프로토콜로, 바이트에 순서 번호를 매기고 수신 확인을 받으며 확인되지 않은 데이터를 재전송해 순서가 보존된 바이트 스트림을 제공한다. 신뢰성을 위한 재전송과 순서 맞추기는 지연을 만들 수 있으므로, 실시간성이 더 중요한 응용은 그 기능 없이 UDP 위에서 필요한 처리만 직접 한다.
근거: rfc768 — User Datagram Protocol: minimum of protocol mechanism

[R1-NET-03]
연결을 맺는 절차는 3방향 핸드셰이크다. 보통 한쪽 TCP가 시작하고 다른 쪽이 응답한다. 핸드셰이크의 주된 목적은 이전 연결에서 온 오래된 중복 세그먼트가 혼란을 일으키는 것을 막는 데 있다. 각 방향의 데이터 전송에는 초기 순서 번호가 쓰이므로 양쪽은 서로의 초기 순서 번호를 동기화해야 한다. 한쪽이 자신의 순서 번호를 담은 SYN을 보내고, 상대는 이를 확인하면서 자신의 순서 번호를 담아 보내며, 처음 쪽이 다시 이를 확인한다. 각자 보낸 순서 번호에 대한 확인을 받아야 연결이 성립한다.
근거: rfc9293 — 3.5 Establishing a Connection; three-way handshake

[R1-NET-04]
CLOSE는 더 보낼 데이터가 없다는 뜻의 연산이다. 연결을 닫은 쪽도 상대가 닫을 때까지는 계속 수신할 수 있으며, 이렇게 한 방향만 닫힌 상태를 절반 닫힌 연결이라 한다. 양쪽이 각각 FIN을 보내고 상대의 확인을 받아야 연결이 완전히 종료된다. 먼저 닫기를 시작한 쪽은 상대의 FIN에 대한 확인을 보낸 뒤 TIME-WAIT 상태로 들어가 최대 세그먼트 수명의 두 배 동안 기다렸다가 연결을 삭제한다. 이 대기는 상대가 종료 요청에 대한 확인을 받았음을 충분히 보장하고, 이전 연결의 세그먼트가 사라질 시간을 준다.
근거: rfc9293 — 3.6 Closing a Connection; 3.6.1 Half-Closed Connections

[R1-NET-05]
도메인 이름 시스템은 도메인 이름 공간과 자원 레코드, 네임 서버, 리졸버로 이루어진다. 네임 서버는 이름 공간의 일부에 대한 완전한 정보를 가지며 그 부분에 대해 권한이 있다고 한다. 리졸버는 클라이언트의 요청에 응답해 네임 서버로부터 정보를 추출하는 프로그램이다. 리졸버는 답을 직접 알지 못하면 다른 네임 서버에 대한 참조를 따라가며 질의를 이어 간다. 받은 레코드는 유효 시간 동안 캐시에 보관해 같은 질의를 반복하지 않는다. 이름으로 주소를 얻은 뒤에야 전송 계층 연결을 맺고 응용 프로토콜의 요청을 보낼 수 있다.
근거: rfc1034 — Domain Names: RESOLVERS are programs that extract information from name servers

[R1-NET-06]
한 호스트 안에서 여러 프로세스가 동시에 TCP를 쓸 수 있도록 TCP는 포트라는 주소를 제공한다. 포트 번호를 인터넷 계층의 호스트 주소와 이어 붙인 것이 소켓이다. 연결은 한 쌍의 소켓으로 정의되며, 한 소켓은 여러 연결에 동시에 쓰일 수 있다. 서버가 잘 알려진 포트에서 연결을 기다리면 여러 클라이언트가 그 소켓으로 연결을 맺고, 각 연결은 상대 쪽 소켓이 달라 서로 구분된다. 연결을 시작하는 쪽의 포트는 보통 그때그때 임시로 배정된다.
근거: rfc9293 — Transmission Control Protocol: A connection is defined by a pair of sockets

출처
rfc1122: https://www.rfc-editor.org/rfc/rfc1122.html
rfc768: https://www.rfc-editor.org/rfc/rfc768.html
rfc9293: https://www.rfc-editor.org/rfc/rfc9293.html
rfc1034: https://www.rfc-editor.org/rfc/rfc1034.html

### r1-http

HTTP 보강 근거 · r1-v1 · RFC 9110·9112·9113·6265

HTTP 학습 근거 · r1-v1
기준: RFC 9110·9112·9113·6265
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-HTTP-01]
HTTP는 무상태 프로토콜로 정의되어 있어 각 요청 메시지의 의미를 다른 요청과 분리해서 이해할 수 있다. 서버는 같은 연결의 여러 요청이 같은 사용자의 것이라고 가정해서는 안 된다. 무상태성 덕분에 요청은 어떤 서버로든 보낼 수 있고 중간 장치가 요청을 분산하기 쉽다. 여러 요청에 걸친 상태가 필요한 응용은 쿠키 같은 장치로 요청마다 식별 정보를 실어 보내고, 서버는 그 식별자로 저장된 상태를 찾는다. 이 상태를 서버 한 대의 메모리에만 두면 다른 서버는 그 요청을 올바르게 처리할 수 없다.
근거: rfc9110 — HTTP Semantics: HTTP is defined as a stateless protocol

[R1-HTTP-02]
요청 메서드는 정의된 의미가 본질적으로 읽기 전용이면 안전하다고 본다. 클라이언트는 안전한 메서드를 적용한 결과로 원 서버의 상태 변경을 요청하지도 기대하지도 않는다. GET, HEAD, OPTIONS, TRACE가 안전한 메서드다. 이 구분의 목적은 수집기와 캐시 성능 최적화 같은 자동화된 조회 과정이 해를 끼칠 걱정 없이 동작하게 하는 데 있다. 자원 소유자는 안전한 메서드의 요청을 처리하면서 안전하지 않은 동작이 수행되지 않도록 해야 한다. GET은 대상 자원의 현재 표현을 전송해 달라는 요청이며 POST는 요청에 담긴 표현을 대상 자원이 처리하도록 요청한다.
근거: rfc9110 — 9.2.1 Safe Methods

[R1-HTTP-03]
응답의 상태 코드는 요청의 결과와 응답의 의미를 나타내는 세 자리 정수다. 첫 자리는 응답의 부류를 정한다. 2xx는 요청이 성공적으로 수신되고 이해되고 받아들여졌음을, 4xx는 클라이언트가 잘못한 것으로 보임을, 5xx는 서버가 오류를 인지했거나 요청을 수행할 수 없음을 나타낸다. 클라이언트가 등록된 모든 상태 코드를 알 필요는 없지만 첫 자리로 부류는 이해해야 한다. 캐시와 중간 장치도 상태 코드를 근거로 응답을 다루므로, 결과에 맞지 않는 상태 코드를 반환하면 이들이 잘못 동작한다.
근거: rfc9110 — 15.5 Client Error 4xx; 15.6 Server Error 5xx

[R1-HTTP-04]
원 서버는 Set-Cookie 헤더로 사용자 에이전트에 상태 정보를 보내고, 사용자 에이전트는 이후 요청의 Cookie 헤더로 그 정보를 돌려보낸다. 서버는 쿠키에 세션 정보를 직접 저장하는 대신 임의의 값인 세션 식별자를 저장하는 것이 보통이며, 이렇게 하면 쿠키 내용이 노출되었을 때의 피해가 제한된다. Secure 속성은 쿠키를 보안 채널로만 보내도록 범위를 제한하고, HttpOnly 속성은 쿠키를 HTTP 요청에만 쓰도록 제한해 스크립트가 읽지 못하게 한다. 쿠키는 사용자가 수정할 수 있으므로 서버는 쿠키 값의 무결성을 그대로 믿어서는 안 된다.
근거: rfc6265 — 8.4 Session Identifiers; state information

[R1-HTTP-05]
HTTP/1.1은 요청 파이프라이닝을 추가했지만 이는 요청의 동시성을 부분적으로만 다루었고 여전히 응용 계층의 선두 차단을 겪는다. 그래서 HTTP/1.1 클라이언트는 동시성을 얻고 지연을 줄이려고 서버에 여러 연결을 맺는다. HTTP/2는 요청과 응답 메시지를 같은 연결 위에서 서로 섞어 보낼 수 있게 한다. 스트림은 연결 안에서 클라이언트와 서버가 주고받는 독립적인 양방향 프레임 흐름이며, 한 연결이 동시에 여러 스트림을 가질 수 있다. 다중화된 스트림들이 같은 TCP 연결을 쓰므로 TCP 수준의 패킷 유실은 그 연결의 모든 스트림에 영향을 준다.
근거: rfc9113 — 5 Streams and Multiplexing; application-layer head-of-line blocking

출처
rfc9110: https://www.rfc-editor.org/rfc/rfc9110.html
rfc6265: https://www.rfc-editor.org/rfc/rfc6265.html
rfc9113: https://www.rfc-editor.org/rfc/rfc9113.html

### r1-testing

테스트 보강 근거 · r1-v1 · 테스트 설계 일반; 측정 도구 사례는 JaCoCo

테스트 학습 근거 · r1-v1
기준: 테스트 설계 일반; 측정 도구 사례는 JaCoCo
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-TEST-01]
테스트 대역에는 여러 종류가 있다. 스텁은 테스트 중에 걸려 오는 호출에 미리 준비된 답을 제공한다. 목은 받을 것으로 기대하는 호출의 명세를 미리 프로그램해 둔 객체다. 스텁을 쓰는 테스트는 실행 뒤 객체의 상태를 살펴 결과를 판단하는 상태 검증을 하고, 목을 쓰는 테스트는 대상이 협력 객체에 올바른 호출을 했는지 확인하는 행위 검증을 한다. 행위 검증 테스트는 대상 메서드의 구현과 결합되므로, 협력 객체에 대한 호출의 성격을 바꾸면 테스트가 깨지기 쉽다.
근거: fowler-mocks — Mocks Aren't Stubs: state verification; behavior verification

[R1-TEST-02]
JaCoCo는 여러 커버리지 지표를 계산한다. 가장 작은 단위는 자바 바이트코드 명령 하나이며, 명령 커버리지는 실행된 코드의 양을 나타낸다. 분기 커버리지는 모든 if 문과 switch 문에 대해 메서드 안의 분기 총수와 그중 실행된 수, 놓친 수를 센다. 라인 커버리지는 디버그 정보가 있는 클래스에서 한 줄에 대응하는 명령이 하나라도 실행되면 그 줄을 실행된 것으로 센다. 실행 여부는 클래스 파일의 제어 흐름에 삽입된 탐침이 기록하며, 이 계측은 클래스가 로드될 때나 빌드 단계에서 이루어진다.
근거: jacoco-counters — Coverage Counters: Branches (C1 Coverage)

[R1-TEST-03]
테스트 커버리지는 코드베이스에서 테스트되지 않은 부분을 찾는 데 유용한 도구다. 그러나 테스트가 얼마나 좋은지를 나타내는 수치로는 거의 쓸모가 없다. 높은 커버리지 수치는 얻기 쉬우면서도 품질이 낮은 테스트로 달성할 수 있고, 검증문이 없는 테스트가 그 극단적인 예다. 특정 수치를 목표로 요구하면 그 수치를 채우기 위한 테스트가 만들어져 정작 필요한 검증에서 주의가 멀어진다. 사려 깊게 테스트를 작성하면 커버리지는 자연히 높게 나오므로, 수치는 결과로 확인하고 낮은 영역을 살펴보는 데 쓴다.
근거: fowler-coverage — TestCoverage: 100% coverage

[R1-TEST-04]
테스트 주도 개발은 테스트를 작성하며 소프트웨어 개발을 이끄는 기법이며 세 단계를 반복한다. 추가하려는 다음 기능에 대한 테스트를 작성하고, 테스트가 통과할 때까지 기능 코드를 작성하고, 새 코드와 기존 코드를 잘 구조화되도록 리팩터링한다. 테스트를 먼저 쓰면 구현보다 인터페이스를 먼저 생각하게 되고, 모든 기능이 테스트를 갖게 된다. 가장 흔한 실수는 셋째 단계를 소홀히 하는 것이다. 코드를 깨끗하게 정리하지 않으면 테스트는 남더라도 지저분한 코드 조각이 뒤섞여 쌓인다.
근거: fowler-tdd — TestDrivenDevelopment: Write a test for the next bit of functionality

[R1-TEST-05]
레거시 시스템을 다룰 때에는 시스템의 이음새를 찾는 것이 유용하다. 이음새는 그 자리를 편집하지 않고도 프로그램의 동작을 바꿀 수 있는 곳이다. 이음새가 있으면 그 지점에서 호출을 가로채 테스트를 위한 관찰 지점을 넣거나 다른 구현으로 바꿔 끼울 수 있다. 의존 대상을 직접 호출하는 코드에는 이음새가 없으므로, 함수 호출을 매개변수로 받게 하거나 재정의할 수 있는 메서드로 감싸는 식의 작은 변경으로 이음새를 만든다. 이음새를 만드는 변경 자체는 동작을 바꾸지 않아야 하며, 그 뒤에 테스트를 추가해 이후의 수정을 보호한다.
근거: fowler-legacyseam — LegacySeam: without editing in that place

[R1-TEST-06]
테스트가 서로 격리되어 있으면 한 테스트의 실행이 다른 테스트의 결과에 영향을 주지 않는다. 격리가 깨지면 테스트가 실행 순서에 의존하게 되어 같은 코드에서 통과와 실패가 뒤바뀌는 비결정적 실패가 생긴다. 격리를 얻는 한 방법은 테스트마다 초기 상태를 처음부터 다시 만드는 것이고, 다른 방법은 테스트가 끝난 뒤 스스로 정리하는 것이다. 정리 방식은 더 빠를 수 있지만, 정리가 잘못되었을 때 문제를 일으킨 테스트가 아니라 그 뒤의 테스트가 실패하므로 원인을 찾기가 더 어렵다.
근거: fowler-nondeterminism — Eradicating Non-Determinism in Tests: Lack of Isolation

[R1-TEST-07]
시스템 시계 호출만큼 비결정적인 것도 드물다. 호출할 때마다 새로운 결과가 나오므로 그 결과에 의존하는 테스트는 실행할 때마다 달라질 수 있다. 해결책은 시계를 감싸는 객체를 두고 모든 코드가 시스템 시계 대신 그 객체에서 시각을 얻게 하는 것이다. 테스트에서는 이 객체를 원하는 시각을 돌려주는 대역으로 바꾼다. 이렇게 하면 테스트가 시간의 흐름을 직접 정할 수 있어, 특정 날짜나 시각에만 나타나는 동작도 언제든 재현할 수 있다. 시계 대역을 쓸 때는 다른 테스트에 영향을 주지 않도록 테스트마다 초기화해야 한다.
근거: fowler-nondeterminism — Eradicating Non-Determinism in Tests: a call to the system clock

출처
fowler-mocks: https://martinfowler.com/articles/mocksArentStubs.html
jacoco-counters: https://www.jacoco.org/jacoco/trunk/doc/counters.html
fowler-coverage: https://martinfowler.com/bliki/TestCoverage.html
fowler-tdd: https://martinfowler.com/bliki/TestDrivenDevelopment.html
fowler-legacyseam: https://martinfowler.com/bliki/LegacySeam.html
fowler-nondeterminism: https://martinfowler.com/articles/nonDeterminism.html

### r1-security

보안·인증·인가 보강 근거 · r1-v1 · OWASP Cheat Sheet Series; RFC 6749·7636·7519·8725; WHATWG Fetch

보안·인증·인가 학습 근거 · r1-v1
기준: OWASP Cheat Sheet Series; RFC 6749·7636·7519·8725; WHATWG Fetch
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-SEC-01]
세션 식별자가 노출되거나 예측되거나 고정되면 공격자가 피해자의 세션을 가로채 그 사용자로 행세할 수 있다. 웹 애플리케이션은 사용자 세션 안에서 권한 수준이 바뀔 때마다 세션 식별자를 갱신해야 하며, 가장 흔한 경우가 인증되지 않은 사용자가 인증된 사용자로 바뀌는 로그인 과정이다. 이전 식별자는 무시하고 새 식별자를 발급해 세션 고정 공격을 막는다. 세션 쿠키에는 Secure 속성으로 암호화된 연결에서만 전송되게 하고, HttpOnly 속성으로 스크립트의 접근을 막으며, SameSite 속성으로 교차 사이트 요청에 실리는 것을 제한한다. 세션에는 유휴 만료와 절대 만료를 함께 둔다.
근거: owasp-session — Session Management: Renew the Session ID After Any Privilege Level Change

[R1-SEC-02]
해싱은 단방향 함수여서 해시에서 원래 값을 복원할 수 없고, 암호화는 키로 원문을 되찾을 수 있는 양방향 함수다. 비밀번호는 거의 모든 경우에 암호화가 아닌 해싱으로 저장해야 한다. 솔트는 해싱 과정에서 각 비밀번호에 더하는 고유하고 무작위로 생성된 문자열이다. 솔트가 사용자마다 다르므로 공격자는 해시를 한 번에 하나씩 깨야 하고, 같은 비밀번호를 쓰는 사용자들을 해시만으로 알아낼 수 없다. 작업 계수는 비밀번호마다 해시 알고리즘을 반복하는 횟수로, 해시 계산을 느리게 만들어 공격자의 대입 속도를 떨어뜨린다. 빠른 범용 해시 함수는 비밀번호 저장에 적합하지 않다.
근거: owasp-password — Password Storage: Hashing vs Encryption; Salting

[R1-SEC-03]
파일 업로드 기능에서는 허용하는 확장자를 목록으로 정해 업무에 필요한 것만 받는다. Content-Type 헤더는 위조할 수 있으므로 그대로 믿지 않는다. 파일 이름은 애플리케이션이 생성한 값으로 바꾸고, 이름의 길이와 허용 문자를 제한한다. 업로드된 파일은 가능하면 다른 서버에 두거나 웹 루트 밖에 저장해 직접 실행되지 않게 한다. 사용자가 제공한 이름을 경로에 그대로 쓰면 경로를 조작하는 문자열로 의도하지 않은 위치에 파일이 쓰일 수 있다. 원래 이름이 필요하면 저장 경로와 무관한 별도 데이터로 보관한다.
근거: owasp-upload — File Upload: Change the filename to something generated by the application

[R1-SEC-04]
SQL 인젝션은 애플리케이션이 문자열 연결로 동적 쿼리를 만들면서 사용자 입력을 그대로 포함할 때 발생한다. 가장 효과적인 방어는 파라미터화된 쿼리를 쓰는 준비된 문장이다. 준비된 문장은 개발자가 먼저 SQL 코드를 모두 정의하고 각 매개변수를 나중에 전달하게 하므로, 데이터베이스가 어떤 입력이 오든 코드와 데이터를 구분할 수 있다. 공격자가 SQL 명령을 값에 넣어도 쿼리의 의도는 바뀌지 않는다. 테이블 이름이나 정렬 방향처럼 바인딩 변수를 쓸 수 없는 위치에는 허용 목록 검증을 적용한다. 문자를 직접 이스케이프하는 방법은 권장되지 않는다.
근거: owasp-sqli — SQL Injection Prevention: Parameterized Queries

[R1-SEC-05]
JWT는 두 당사자 사이에 전달할 클레임을 표현하는 간결하고 URL에 안전한 수단이다. 클레임은 서명이나 메시지 인증 코드로 무결성을 보호할 수 있고 별도로 암호화할 수도 있다. 서명만 한 토큰의 내용은 암호화되지 않으므로 기밀 정보를 담는 용도로 쓰지 않는다. 토큰을 검증하는 쪽은 토큰 헤더의 알고리즘 값을 그대로 신뢰하지 말고, 사용할 알고리즘을 직접 지정해 그 알고리즘으로만 검증해야 한다. 토큰의 유효성은 서명과 만료 시각 같은 클레임으로 판단되므로, 발급 뒤의 상태 변화를 반영하려면 수명을 짧게 하거나 별도의 폐기 확인을 둔다.
근거: rfc8725 — 3.1 Perform Algorithm Verification

[R1-SEC-06]
권한 부여 코드 방식에서 클라이언트는 자원 소유자를 인가 서버로 보내고, 인가 서버는 자원 소유자를 인증하고 동의를 받은 뒤 권한 부여 코드와 함께 클라이언트로 되돌려 보낸다. 클라이언트는 받은 코드를 토큰 엔드포인트에 제시해 접근 토큰을 요청하며, 이때 인가 서버에 자신을 인증한다. 인가 서버는 클라이언트를 인증하고 코드를 검증하며 되돌림 주소가 처음 요청과 일치하는지 확인한 뒤 토큰을 발급한다. 이 방식은 접근 토큰을 자원 소유자의 사용자 에이전트를 거치지 않고 클라이언트에 직접 전달하므로 토큰이 다른 쪽에 노출될 가능성을 줄인다. 코드는 수명이 짧고 한 번만 사용할 수 있어야 한다.
근거: rfc6749 — 4.1 Authorization Code Grant

[R1-SEC-07]
권한 부여 코드 방식을 쓰는 공개 클라이언트는 권한 부여 코드 가로채기 공격에 취약하다. 이 공격에서 공격자는 인가 엔드포인트가 돌려주는 코드를 전송 계층 보안으로 보호되지 않는 경로에서 가로챈다. 이를 막기 위해 클라이언트는 인가 요청마다 코드 검증자라는 암호학적 무작위 값을 만들고, 그것을 변환한 코드 챌린지를 인가 요청에 실어 보낸다. 토큰 요청에서는 받은 코드와 함께 코드 검증자를 보내고, 서버는 이를 변환해 앞서 받은 챌린지와 비교한다. 가로챈 쪽은 검증자를 갖고 있지 않으므로 코드를 토큰으로 바꿀 수 없다.
근거: rfc7636 — Proof Key for Code Exchange: authorization code interception attack

[R1-SEC-08]
CORS 프로토콜은 응답을 다른 출처와 공유할 수 있는지를 헤더로 나타낸다. 서버는 Access-Control-Allow-Origin 헤더로 응답을 공유할 출처를 알린다. 안전 목록에 없는 메서드나 헤더를 쓰는 요청에 대해서는 브라우저가 CORS 사전 요청을 먼저 보내 서버가 이 프로토콜을 이해하고 해당 메서드와 헤더를 허용하는지 확인한다. 사전 요청은 OPTIONS 메서드를 쓰며 실제 요청에 쓰일 메서드와 헤더 이름을 실어 보낸다. 이 검사는 브라우저가 수행하므로, 브라우저가 아닌 클라이언트의 요청을 서버가 거부하려면 별도의 인증과 인가가 필요하다.
근거: fetch-cors — Fetch Standard: CORS-preflight request; Access-Control-Allow-Origin

출처
owasp-session: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
owasp-password: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
owasp-upload: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
owasp-sqli: https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html
rfc8725: https://www.rfc-editor.org/rfc/rfc8725.html
rfc6749: https://www.rfc-editor.org/rfc/rfc6749.html
rfc7636: https://www.rfc-editor.org/rfc/rfc7636.html
fetch-cors: https://fetch.spec.whatwg.org/

### r1-delivery-and-migration

배포·변경·복구 보강 근거 · r1-v1 · 배포 방식 일반; 사례는 Docker·Kubernetes 문서 기준

배포·변경·복구 학습 근거 · r1-v1
기준: 배포 방식 일반; 사례는 Docker·Kubernetes 문서 기준
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[R1-DEL-01]
가상화는 한 물리 서버의 CPU 위에 여러 가상 머신을 실행하게 해 애플리케이션을 서로 격리한다. 각 가상 머신은 가상화된 하드웨어 위에 자체 운영체제를 포함한 모든 구성 요소를 실행하는 완전한 기계다. 컨테이너는 가상 머신과 비슷하지만 격리 속성을 완화해 애플리케이션들이 운영체제를 공유한다. 그래서 컨테이너는 가볍다고 여겨진다. 컨테이너도 자체 파일 시스템과 CPU 할당, 메모리, 프로세스 공간을 가지며, 기반 인프라와 분리되어 있어 여러 클라우드와 운영체제 배포판 사이에서 옮길 수 있다.
근거: k8s-overview — Kubernetes Overview: Container deployment era

[R1-DEL-02]
프록시를 거치면 원래 클라이언트의 IP 주소 같은 정보가 사라진다. Forwarded 헤더는 프록시 구성 요소가 프록시 과정에서 잃어버리거나 바뀐 정보를 공개할 수 있게 하는 HTTP 확장이다. 이전에는 X-Forwarded-For와 X-Forwarded-Proto 같은 비표준 헤더가 같은 목적으로 쓰였다. 이 헤더에는 요청을 보낸 쪽의 주소와 사용된 프로토콜, 원래의 호스트 정보를 담을 수 있다. 헤더 값은 요청 경로에 있는 어느 쪽이든 넣거나 바꿀 수 있으므로, 받는 쪽은 신뢰할 수 있는 프록시가 추가한 부분만 근거로 삼아야 한다.
근거: rfc7239 — Forwarded HTTP Extension: X-Forwarded-For

[R1-DEL-03]
Deployment는 파드 템플릿이 바뀌면 롤아웃을 시작해 새 레플리카셋을 늘리고 기존 레플리카셋을 줄여 간다. 롤링 업데이트 전략에서 maxUnavailable은 업데이트 과정에서 사용할 수 없어도 되는 파드의 최대 수를, maxSurge는 원하는 파드 수를 넘어 생성할 수 있는 파드의 최대 수를 지정한다. 두 값은 절대 수나 비율로 줄 수 있다. 사용 가능한 파드 수는 원하는 수에서 maxUnavailable을 뺀 값과 maxSurge를 더한 값 사이에 유지된다. 새 파드가 준비 상태가 되어야 기존 파드를 줄이므로, 준비 상태 검사가 잘못되면 롤아웃이 멈추거나 준비되지 않은 파드가 트래픽을 받는다.
근거: k8s-deployment — Deployments: maxUnavailable; maxSurge

[R1-DEL-04]
블루-그린 배포는 가능한 한 동일한 운영 환경 두 벌을 유지한다. 어느 시점에든 한쪽이 실제 트래픽을 받고, 새 릴리스는 다른 쪽에 배포해 최종 테스트를 한다. 준비가 끝나면 라우터를 전환해 들어오는 요청이 모두 새 환경으로 가게 한다. 문제가 생기면 라우터를 되돌려 빠르게 롤백할 수 있다. 데이터베이스는 이 기법에서 다루기 어려운 부분으로, 스키마를 바꿔야 할 때에는 먼저 데이터베이스가 두 버전을 모두 지원하도록 리팩터링해 배포하고 정상 동작을 확인한 다음 새 버전의 애플리케이션을 배포한다.
근거: fowler-bluegreen — BlueGreenDeployment: two production environments

[R1-DEL-05]
카나리 릴리스는 변경을 전체 인프라에 적용하기 전에 일부 사용자에게만 천천히 내보내 새 버전 도입의 위험을 줄이는 기법이다. 새 버전을 사용자가 쓰지 않는 일부 인프라에 먼저 배포하고, 그다음 소수의 사용자를 그쪽으로 보낸다. 새 버전에 대한 확신이 커지면 더 많은 사용자에게 내보내고, 문제가 발견되면 사용자를 이전 버전으로 되돌려 롤백한다. 이 방식은 실제 운영 환경에서 용량과 동작을 확인할 수 있게 하지만, 여러 버전을 동시에 운영해야 하므로 데이터베이스 변경은 두 버전이 함께 쓸 수 있는 방식으로 진행해야 한다.
근거: fowler-canary — CanaryRelease: small subset of users

[R1-DEL-06]
병렬 변경은 확장과 축소라고도 하며, 인터페이스에 하위 호환되지 않는 변경을 안전하게 적용하는 패턴이다. 변경을 확장, 이전, 축소의 세 단계로 나눈다. 확장 단계에서는 기존 형태와 새 형태를 모두 지원하도록 인터페이스를 넓힌다. 이전 단계에서는 기존 형태를 쓰던 쪽을 새 형태로 옮긴다. 모든 사용처가 옮겨진 뒤 축소 단계에서 기존 형태를 제거한다. 이 방식은 변경을 여러 번에 걸쳐 점진적으로 내보낼 수 있게 하지만, 이전이 끝날 때까지 두 형태를 함께 유지해야 하고 축소 단계를 끝내지 않으면 쓰이지 않는 코드가 남는다.
근거: fowler-parallelchange — ParallelChange: expand and contract

[R1-DEL-07]
Dockerfile의 각 명령은 최종 이미지의 레이어 하나에 대응한다. 빌드를 다시 실행하면 빌더는 이전 빌드의 레이어를 재사용하려 하며, 명령이나 명령이 쓰는 파일이 바뀌지 않았다면 그 레이어는 캐시에서 가져온다. 어떤 레이어가 바뀌면 그 뒤에 오는 다른 레이어도 모두 영향을 받아 다시 빌드된다. 예를 들어 COPY 명령으로 복사하는 파일이 바뀌면 그 레이어의 캐시가 무효화되고 뒤따르는 명령들도 다시 실행된다. 따라서 비용이 큰 단계와 자주 바뀌지 않는 단계를 앞쪽에 두면 캐시를 더 많이 재사용할 수 있다.
근거: docker-cache — Docker build cache: all other layers that come after it are also affected

[R1-DEL-08]
지속적 통합은 팀의 각 구성원이 자신의 변경을 적어도 하루에 한 번 코드베이스의 주 흐름에 병합하는 소프트웨어 개발 실천이다. 각 통합은 테스트를 포함한 자동 빌드로 검증해 통합 오류를 가능한 한 빨리 찾아낸다. 통합 간격이 짧으면 충돌이 생기더라도 그 사이의 변경이 적어 원인을 좁히기 쉽다. 주 흐름으로의 모든 푸시는 빌드를 일으켜야 하고, 빌드가 깨지면 즉시 고쳐 주 흐름을 건강한 상태로 유지한다. 자주 통합하면 통합에 따르는 위험이 줄고, 배포할 수 있는 상태의 소프트웨어를 언제나 갖게 된다.
근거: fowler-ci — Continuous Integration: Everyone Pushes Commits To the Mainline Every Day

[R1-DEL-09]
코드형 인프라는 컴퓨팅과 네트워크 인프라를 소스 코드로 정의하고, 그 코드를 다른 소프트웨어처럼 다루는 접근이다. 이런 코드는 소스 관리에 두어 감사 추적과 재현 가능한 빌드를 가능하게 하고, 테스트와 지속적 배포의 실천을 적용할 수 있다. 정의 파일로부터 인프라를 만들면 같은 구성을 여러 번 일관되게 재현할 수 있어, 손으로 관리할 때 서버마다 구성이 조금씩 달라지는 문제를 피한다. 서버에 직접 접속해 설정을 바꾸는 일은 정의와 실제의 차이를 만들기 때문에 피하고, 변경은 정의 파일을 수정해 자동화된 절차로 적용한다.
근거: fowler-iac — InfrastructureAsCode: source control

출처
k8s-overview: https://kubernetes.io/docs/concepts/overview/
rfc7239: https://www.rfc-editor.org/rfc/rfc7239.html
k8s-deployment: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
fowler-bluegreen: https://martinfowler.com/bliki/BlueGreenDeployment.html
fowler-canary: https://martinfowler.com/bliki/CanaryRelease.html
fowler-parallelchange: https://martinfowler.com/bliki/ParallelChange.html
docker-cache: https://docs.docker.com/build/cache/
fowler-ci: https://martinfowler.com/articles/continuousIntegration.html
fowler-iac: https://martinfowler.com/bliki/InfrastructureAsCode.html

## 출처 이용 조건 검수

원문·코드·그림 미포함. 아래 메모는 법률 검토나 공개 승인 결과가 아님.

- [Algorithms 4th Edition — 1.4 Analysis of Algorithms](https://algs4.cs.princeton.edu/14analysis/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 3.3 Balanced Search Trees](https://algs4.cs.princeton.edu/33balanced/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 1.1 Programming Model](https://algs4.cs.princeton.edu/11model/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 3.2 Binary Search Trees](https://algs4.cs.princeton.edu/32bst/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 4.1 Undirected Graphs](https://algs4.cs.princeton.edu/41graph/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 4.4 Shortest Paths](https://algs4.cs.princeton.edu/44sp/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 1.3 Bags, Queues, and Stacks](https://algs4.cs.princeton.edu/13stacks/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Algorithms 4th Edition — 1.5 Case Study: Union-Find](https://algs4.cs.princeton.edu/15uf/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — package java.util.concurrent.atomic](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/package-summary.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.io.BufferedInputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/BufferedInputStream.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.BlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.Callable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Callable.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.nio.charset.Charset](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.locks.Condition](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/Condition.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.ExecutorService](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.nio.file.Files](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.ForkJoinPool](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ForkJoinPool.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.stream.IntStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.LinkedHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.locks.ReentrantLock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.lang.Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.Properties](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Properties.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — package java.lang.reflect](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/package-summary.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.Set](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — package java.util.stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.lang.StringBuilder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.lang.Thread](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.lang.Throwable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — package java.time](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 API — java.util.concurrent.ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Docker Docs — Docker build cache](https://docs.docker.com/build/cache/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [WHATWG Fetch Standard](https://fetch.spec.whatwg.org/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — BlueGreenDeployment](https://martinfowler.com/bliki/BlueGreenDeployment.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — CanaryRelease](https://martinfowler.com/bliki/CanaryRelease.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — Continuous Integration](https://martinfowler.com/articles/continuousIntegration.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — CodeAsDocumentation](https://martinfowler.com/bliki/CodeAsDocumentation.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — TestCoverage](https://martinfowler.com/bliki/TestCoverage.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — CommandQuerySeparation](https://martinfowler.com/bliki/CommandQuerySeparation.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — DataClump](https://martinfowler.com/bliki/DataClump.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — DIP in the Wild](https://martinfowler.com/articles/dipInTheWild.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — FunctionLength](https://martinfowler.com/bliki/FunctionLength.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — InfrastructureAsCode](https://martinfowler.com/bliki/InfrastructureAsCode.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — Inversion of Control Containers and the Dependency Injection pattern](https://martinfowler.com/articles/injection.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — LegacySeam](https://martinfowler.com/bliki/LegacySeam.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — Mocks Aren't Stubs](https://martinfowler.com/articles/mocksArentStubs.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — Eradicating Non-Determinism in Tests](https://martinfowler.com/articles/nonDeterminism.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — ParallelChange](https://martinfowler.com/bliki/ParallelChange.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — TestDrivenDevelopment](https://martinfowler.com/bliki/TestDrivenDevelopment.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — TellDontAsk](https://martinfowler.com/bliki/TellDontAsk.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Martin Fowler — Yagni](https://martinfowler.com/bliki/Yagni.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java SE 21 HotSpot VM Garbage Collection Tuning Guide — Garbage Collector Implementation](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-collector-implementation1.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Google Engineering Practices — Small CLs](https://google.github.io/eng-practices/review/developer/small-cls.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [HikariCP Wiki — About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JaCoCo Documentation — Coverage Counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 15. Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 17. Threads and Locks](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 3. Lexical Structure](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 4. Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 5. Conversions and Contexts](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 8. Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS Java SE 21 — Chapter 9. Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Kubernetes Documentation — Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Kubernetes Documentation — Overview](https://kubernetes.io/docs/concepts/overview/): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Linux man-pages — pipe(7)](https://man7.org/linux/man-pages/man7/pipe.7.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Robert C. Martin — The Single Responsibility Principle](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 32: Common Concurrency Problems](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-bugs.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 6: Mechanism: Limited Direct Execution](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-mechanisms.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 4: The Abstraction: The Process](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-intro.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 16: Segmentation](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-segmentation.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 31: Semaphores](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-sema.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 21: Beyond Physical Memory: Mechanisms](https://pages.cs.wisc.edu/~remzi/OSTEP/vm-beyondphys.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 26: Concurrency: An Introduction](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-intro.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OWASP Cheat Sheet Series — Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OWASP Cheat Sheet Series — Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OWASP Cheat Sheet Series — SQL Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OWASP Cheat Sheet Series — File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 9.21 Aggregate Functions](https://www.postgresql.org/docs/17/functions-aggregate.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 5.5 Constraints](https://www.postgresql.org/docs/17/ddl-constraints.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 14.1 Using EXPLAIN](https://www.postgresql.org/docs/17/using-explain.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 26.1 Comparison of Different Solutions](https://www.postgresql.org/docs/17/different-replication-solutions.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 11.9 Index-Only Scans and Covering Indexes](https://www.postgresql.org/docs/17/indexes-index-only-scans.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 11.2 Index Types](https://www.postgresql.org/docs/17/indexes-types.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 11.1 Introduction (Indexes)](https://www.postgresql.org/docs/17/indexes-intro.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 13.3 Explicit Locking](https://www.postgresql.org/docs/17/explicit-locking.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 13.1 Introduction (Concurrency Control)](https://www.postgresql.org/docs/17/mvcc-intro.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 11.8 Partial Indexes](https://www.postgresql.org/docs/17/indexes-partial.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 14.2 Statistics Used by the Planner](https://www.postgresql.org/docs/17/planner-stats.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 7.2 Table Expressions](https://www.postgresql.org/docs/17/queries-table-expressions.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 2.7 Aggregate Functions](https://www.postgresql.org/docs/17/tutorial-agg.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 3.4 Transactions](https://www.postgresql.org/docs/17/tutorial-transactions.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [PostgreSQL 17 Documentation — 7.4 Combining Queries](https://www.postgresql.org/docs/17/queries-union.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Refactoring catalog — Encapsulate Collection](https://refactoring.com/catalog/encapsulateCollection.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Refactoring catalog — Replace Nested Conditional with Guard Clauses](https://refactoring.com/catalog/replaceNestedConditionalWithGuardClauses.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Refactoring catalog — Hide Delegate](https://refactoring.com/catalog/hideDelegate.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Refactoring catalog — Replace Magic Literal](https://refactoring.com/catalog/replaceMagicLiteral.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 1034 — Domain Names: Concepts and Facilities](https://www.rfc-editor.org/rfc/rfc1034.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 1122 — Requirements for Internet Hosts: Communication Layers](https://www.rfc-editor.org/rfc/rfc1122.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 6265 — HTTP State Management Mechanism](https://www.rfc-editor.org/rfc/rfc6265.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 6749 — The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 7239 — Forwarded HTTP Extension](https://www.rfc-editor.org/rfc/rfc7239.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 7636 — Proof Key for Code Exchange by OAuth Public Clients](https://www.rfc-editor.org/rfc/rfc7636.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 768 — User Datagram Protocol](https://www.rfc-editor.org/rfc/rfc768.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 8725 — JSON Web Token Best Current Practices](https://www.rfc-editor.org/rfc/rfc8725.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 9113 — HTTP/2](https://www.rfc-editor.org/rfc/rfc9113.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [RFC 9293 — Transmission Control Protocol (TCP)](https://www.rfc-editor.org/rfc/rfc9293.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — AOP Concepts](https://docs.spring.io/spring-framework/reference/7.0/core/aop/introduction-defn.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/7.0/core/aop/proxying.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 API — HandlerMethodArgumentResolver](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/web/method/support/HandlerMethodArgumentResolver.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Using @Autowired](https://docs.spring.io/spring-framework/reference/7.0/core/beans/annotation-config/autowired.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Dependency Injection](https://docs.spring.io/spring-framework/reference/7.0/core/beans/dependencies/factory-collaborators.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Method Arguments](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/arguments.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Interception](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-servlet/handlermapping-interceptor.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Flash Attributes](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/flash-attributes.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — @ResponseBody](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-controller/ann-methods/responsebody.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Special Bean Types](https://docs.spring.io/spring-framework/reference/7.0/web/webmvc/mvc-servlet/special-bean-types.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Reference — Java Bean Validation](https://docs.spring.io/spring-framework/reference/7.0/core/validation/beanvalidation.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
