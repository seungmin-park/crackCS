# initial-v1 검수용 읽기 자료

> 자동 생성 읽기 사본. 수정 기준은 [bundle.json](bundle.json). 사람 검수·공개 승인 대기.

- 원본 SHA-256: `cba7aae00b69c39fd4a4b6a0659acd50f121e5d889e5ae14cadc0b41c6295333`
- 재생성: 저장소 루트에서 `python3 scripts/render_content_review.py`
- 검수 절차·승인 기록: [콘텐츠 안내](README.md#사람-검수공개-순서)
- 이 자료를 읽었다는 사실만으로 앱의 검수·공개 상태가 바뀌지 않음

## 문항과 판정 기준

### OS-101 · BASIC

Unix 계열에서 fork()로 만든 자식 프로세스가 exec()를 호출합니다. 두 호출이 모두 성공한 경우, 프로세스와 실행 중인 프로그램이 각각 어떻게 바뀌는지 설명하세요.

**모범 답안**

fork는 부모를 바탕으로 새 자식 프로세스를 만들며, 성공하면 부모와 자식에서 반환값이 다르다. exec는 호출한 프로세스의 프로그램 이미지를 새 프로그램으로 교체한다. exec 자체가 또 다른 프로세스를 만드는 것은 아니며 성공한 exec는 이전 프로그램의 다음 문장으로 돌아오지 않는다.

**필수 개념**

- fork의 프로세스 생성 · 가중치 0.50: 부모를 바탕으로 새 프로세스를 만들고 부모·자식 양쪽에서 실행을 계속하는 동작.
- exec의 프로그램 교체 · 가중치 0.50: 호출한 프로세스에서 프로그램 이미지를 교체하며 성공하면 이전 코드로 복귀하지 않는 동작.

**혼동 주의:** exec가 항상 새로운 PID의 프로세스를 만든다고 설명하지 않기.

**출처 대조 위치**

- [OSTEP 5: Process API](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-api.pdf) — 5.1 fork, 5.3 exec
- [MIT xv6 book, RISC-V revision 5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf) — 1.1 Processes and memory

근거 문서: `operating_system-v1`. 검수 상태: **PENDING**

### OS-102 · BASIC

부모가 만든 자식 프로세스 하나가 출력 후 종료하고, 부모는 그 자식의 종료를 확인한 뒤 결과를 출력하려고 합니다. sleep(1) 대신 wait 계열 호출을 쓰는 이유를 설명하세요. 대상 자식의 종료 상태를 정상적으로 회수한 경우를 기준으로 답하세요.

**모범 답안**

sleep은 시간의 경과만 기다리므로 대상 자식의 완료를 보장하지 않는다. 실행 시간은 부하와 스케줄링에 따라 달라진다. wait 계열로 대상 자식의 종료를 확인하고 종료 상태를 회수한 뒤 부모의 후속 작업을 진행할 수 있다. 오류·중단 반환을 종료 완료로 취급하면 안 된다.

**필수 개념**

- 자식 종료 대기와 회수 · 가중치 0.50: wait 계열로 자식의 종료를 관찰하고 상태를 회수하는 동기화.
- 조건 기반 실행 순서 · 가중치 0.50: 고정 시간 지연 대신 필요한 사건의 완료로 후속 작업의 시작을 결정하는 방식.

**혼동 주의:** sleep 시간이 길면 완료가 보장된다는 주장 제외. 대상 자식의 종료 확인과 단순 wait 반환을 구분.

**출처 대조 위치**

- [OSTEP 5: Process API](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-api.pdf) — 5.2 The wait() System Call
- [MIT xv6 book, RISC-V revision 5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf) — 1.1 exit and wait; 9.4 Code: Wait, exit, and kill

근거 문서: `operating_system-v1`. 검수 상태: **PENDING**

### OS-103 · INTERMEDIATE

Unix 계열 프로세스가 일반 파일을 한 번 연 뒤 fork()했습니다. 부모와 자식이 상속한 파일 디스크립터로 읽을 때 파일 오프셋은 독립적인가요? 자식의 close가 부모의 디스크립터 사용에 미치는 영향도 설명하세요. 호출은 성공하고 파일을 다시 열지 않은 경우를 기준으로 답하세요.

**모범 답안**

상속된 디스크립터는 같은 열린 파일 설명에 연결되므로 파일 오프셋을 공유한다. 한쪽의 읽기가 다른 쪽이 다음에 읽을 위치에 영향을 줄 수 있으며 실행 순서는 별도 동기화 없이는 확정할 수 없다. 디스크립터 테이블은 프로세스별이므로 자식이 자신의 디스크립터를 닫아도 부모가 가진 참조까지 닫히지는 않는다.

**필수 개념**

- 프로세스별 파일 디스크립터 · 가중치 0.50: 프로세스의 디스크립터 테이블에 있는 열린 파일 참조.
- 상속한 열린 파일의 오프셋 공유 · 가중치 0.50: fork로 상속된 디스크립터들이 같은 열린 파일의 읽기·쓰기 위치를 공유하는 관계.

**혼동 주의:** fork 후 모든 파일 상태가 깊은 복사된다고 설명하지 않기.

**출처 대조 위치**

- [OSTEP 5: Process API](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-api.pdf) — 5.4 redirection and file descriptors; homework 2
- [MIT xv6 book, RISC-V revision 5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf) — 1.2 I/O and File descriptors

근거 문서: `operating_system-v1`. 검수 상태: **PENDING**

### OS-104 · INTERMEDIATE

POSIX 조건 변수를 쓰는 소비자가 큐가 비었는지 if로 검사한 뒤 기다립니다. 깨어나면 바로 항목을 꺼내도 될까요? while 재검사, 같은 mutex를 이용한 상태 보호, wait의 잠금 해제·대기 전환이 각각 필요한 이유를 설명하세요.

**모범 답안**

깨어났다는 알림만으로 큐가 여전히 비어 있지 않음을 보장할 수 없다. 다른 소비자가 먼저 항목을 가져가거나 불필요한 깨움이 생길 수 있어 mutex를 잡은 상태에서 조건을 while로 재검사한다. 조건 검사·변경을 같은 잠금으로 보호하고, wait가 잠금을 놓고 대기하는 동작을 원자적으로 연결해야 검사와 대기 사이의 알림 손실을 막을 수 있다.

**필수 개념**

- 조건 변수의 조건 재검사 · 가중치 0.50: 깨움 이후 공유 상태를 다시 검사해 작업 가능 여부를 판단하는 규칙.
- 잠금과 대기 전환의 원자성 · 가중치 0.50: 조건 검사와 잠금 해제·대기 사이에 깨움이 유실되지 않도록 연결하는 동기화.

**혼동 주의:** signal을 받으면 조건이 참으로 예약된다는 해석 제외.

**출처 대조 위치**

- [OSTEP 30: Condition Variables](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-cv.pdf) — 30.1 Definition and Routines, pp. 2–3; 30.2 Producer/Consumer, pp. 9–14 (Mesa semantics, while, spurious wakeups)
- [MIT xv6 book, RISC-V revision 5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf) — 9.1 Overview, pp. 81–83; 9.2 Code: Sleep and wakeup, pp. 83–84 (condition lock, lost wakeup, predicate recheck)

근거 문서: `operating_system-v1`. 검수 상태: **PENDING**

### OS-105 · INTERMEDIATE

라운드 로빈 스케줄러의 타임 슬라이스를 매우 작게 줄이면 모든 성능 지표가 좋아질까요? 첫 CPU 실행까지의 응답 시간과 문맥 교환 비용을 중심으로 설명하세요.

**모범 답안**

짧은 타임 슬라이스는 여러 실행 가능 작업이 CPU를 빨리 한 번씩 받을 수 있어 첫 응답 시간을 줄이는 데 유리할 수 있다. 그러나 문맥 교환 횟수가 늘어 레지스터 전환과 캐시 등 부가 비용이 커진다. 지나치게 짧으면 실제 작업에 쓰는 시간이 줄어 처리량이나 완료 시간이 나빠질 수 있다. 응답성·공정성과 교환 비용 사이의 절충이다.

**필수 개념**

- 라운드 로빈의 응답성 · 가중치 0.50: 실행 가능한 작업에 시간 조각을 순환 배분해 첫 실행 기회를 제공하는 정책.
- 문맥 교환의 부가 비용 · 가중치 0.50: CPU 실행 상태 전환과 캐시 효과 등으로 실제 작업 외에 소모되는 비용.

**혼동 주의:** 짧은 타임 슬라이스가 처리량·완료 시간까지 항상 개선한다는 주장 제외.

**출처 대조 위치**

- [OSTEP 7: Scheduling](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-sched.pdf) — 7.6 Response Time, pp. 6–7; 7.7 Round Robin, pp. 7–9 (time-slice/context-switch trade-off)
- [MIT xv6 book, RISC-V revision 5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf) — 8.2 Context switch overview, p. 75; 8.3 Code: Context switching, p. 76; 8.6 Real world, p. 79 (round robin policy)

근거 문서: `operating_system-v1`. 검수 상태: **PENDING**

### JAVA-101 · INTERMEDIATE

Java 21에서 List<String>을 컴포넌트로 가진 record를 만들었습니다. record라는 이유만으로 리스트 내용도 불변일까요? 생성자와 접근자에서 고려할 경계를 설명하세요.

**모범 답안**

record의 컴포넌트 필드는 final이지만 참조 대상 객체까지 깊은 불변이 되는 것은 아니다. 전달받은 가변 리스트를 그대로 저장하면 호출자가 그 리스트를 바꿀 수 있고, 같은 리스트를 접근자로 내보내도 외부 변경 경로가 생긴다. 입력에서 방어적 복사와 변경 불가 보관을 적용하고 반환 경로도 점검해야 한다. 원소 자체가 가변 객체라면 그 상태는 별도 정책이 필요하다.

**필수 개념**

- record 컴포넌트의 final 참조 · 가중치 0.50: record 컴포넌트 필드가 재대입되지 않는 성질.
- 참조 대상의 가변성 · 가중치 0.50: 참조가 고정되어도 그 객체의 내부 상태가 변경될 수 있는 성질.

**혼동 주의:** record를 자동으로 깊은 불변 객체라고 보지 않기.

**출처 대조 위치**

- [JLS 21 Chapter 8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) — 8.10.3 Record Members; 8.10.4 Record Constructor Declarations

근거 문서: `java-v1`. 검수 상태: **PENDING**

### JAVA-102 · INTERMEDIATE

Object value = "hello"이고 같은 클래스에 print(Object)와 print(String)이 있습니다. print(value)는 어느 선언을 선택하나요? 선택된 인스턴스 메서드의 오버라이딩과 오버로딩을 구분하세요.

**모범 답안**

오버로드 선택은 호출 지점에서 보이는 인수의 컴파일 시 타입을 바탕으로 하므로 print(Object)가 선택된다. value가 실제 String 객체를 가리킨다는 이유로 print(String)을 다시 선택하지 않는다. 오버라이딩은 이미 선택된 시그니처에 대해 수신 객체의 런타임 타입에 맞는 구현을 찾는 단계다.

**필수 개념**

- 컴파일 시 오버로드 선택 · 가중치 0.50: 호출 지점의 타입과 적용 가능성으로 메서드 선언을 선택하는 과정.
- 인스턴스 메서드의 동적 디스패치 · 가중치 0.50: 선택된 시그니처의 구현을 수신 객체 런타임 타입에 따라 찾는 과정.

**혼동 주의:** 인수의 런타임 타입으로 오버로드가 선택된다는 오해 제외.

**출처 대조 위치**

- [JLS 21 Chapter 15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) — 15.12.2 Compile-Time Step 2; 15.12.4.4 Locate Method to Invoke

근거 문서: `java-v1`. 검수 상태: **PENDING**

### JAVA-103 · BASIC

Object.clone()을 이용해 List 필드를 가진 객체를 얕게 복사했습니다. 복사본의 필드에 새 리스트를 대입하는 것과 기존 리스트에 원소를 추가하는 것은 원본에 어떤 차이를 만드나요?

**모범 답안**

얕은 복사는 필드 값을 복사하므로 참조 필드는 같은 리스트 객체를 가리킨다. 복사본 필드에 다른 리스트 참조를 대입하면 원본의 필드 참조는 바뀌지 않는다. 반면 공유 중인 기존 리스트에 원소를 추가하면 양쪽에서 같은 변경을 본다. 독립적인 변경이 필요하면 소유할 가변 상태의 복사 깊이를 정해야 한다.

**필수 개념**

- 얕은 복사의 참조 공유 · 가중치 0.50: 필드 값만 복사해 참조 대상은 원본과 공유할 수 있는 복사 방식.
- 참조 재대입과 대상 변경 · 가중치 0.50: 필드가 가리키는 대상을 바꾸는 동작과 공유 대상의 상태를 바꾸는 동작의 차이.

**혼동 주의:** 원본과 복사본이 다른 객체라는 이유로 내부 객체도 전부 독립이라고 보지 않기.

**출처 대조 위치**

- [Java 21 Object API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) — Object.clone(): implementation requirements

근거 문서: `java-v1`. 검수 상태: **PENDING**

### JAVA-104 · INTERMEDIATE

BigDecimal("2.0")과 BigDecimal("2.00")을 비교합니다. equals와 compareTo 결과가 왜 다른지, 금액 비교 정책을 정할 때 무엇을 구분해야 하는지 설명하세요.

**모범 답안**

두 값은 수치상 같아 compareTo는 0을 반환하지만 scale이 달라 equals는 false다. equals는 수치뿐 아니라 표현의 scale도 비교한다. 금액의 수치적 동등성과 표시·저장 자릿수 정책을 구분해야 하며, equals 기반 컬렉션과 정렬 순서 기반 컬렉션에서 같은 중복 규칙을 기대하면 안 된다.

**필수 개념**

- BigDecimal의 scale과 표현 · 가중치 0.50: 수치 표현에 소수점 자릿수 정보를 포함하는 구조.
- BigDecimal 동등성과 순서 비교 · 가중치 0.50: equals의 표현 비교와 compareTo의 수치 비교 사이의 차이.

**혼동 주의:** compareTo가 0이면 equals도 반드시 true라는 주장 제외.

**출처 대조 위치**

- [Java 21 BigDecimal API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html) — class description: cohorts; equals(Object); compareTo(BigDecimal)

근거 문서: `java-v1`. 검수 상태: **PENDING**

### JAVA-105 · INTERMEDIATE

작업 스레드의 sleep에서 InterruptedException을 잡았습니다. 그 예외가 발생할 때 인터럽트 상태는 어떻게 되며, 예외를 무시한 채 반복 작업을 계속하면 취소 요청에 어떤 영향을 줄 수 있나요?

**모범 답안**

sleep이 InterruptedException을 던질 때 현재 스레드의 인터럽트 상태는 지워진다. 예외를 무시하고 계속 실행하면 상위 흐름이 취소 요청을 알아차리지 못할 수 있다. 해당 계층이 종료를 책임지면 작업을 정리해 종료하고, 직접 처리하지 못하면 예외를 전파하거나 인터럽트 상태를 복구해 상위 정책이 관찰하도록 한다. interrupt는 모든 코드를 강제 종료하는 명령이 아니다.

**필수 개념**

- 협력적 인터럽트 처리 · 가중치 0.50: 강제 종료 대신 대기 API와 상태 검사를 통해 중단 요청을 처리하는 방식.
- InterruptedException과 상태 소거 · 가중치 0.50: sleep 등이 InterruptedException을 던지며 인터럽트 상태를 지우는 동작.

**혼동 주의:** 예외를 잡은 뒤 인터럽트 상태가 항상 그대로 남는다고 가정하지 않기.

**출처 대조 위치**

- [Java 21 Thread API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html) — interrupt(); sleep(long); interrupted(); isInterrupted()

근거 문서: `java-v1`. 검수 상태: **PENDING**

### SF-101 · INTERMEDIATE

singleton 빈의 생성자로 prototype 빈을 한 번 주입했습니다. singleton의 메서드를 호출할 때마다 새 prototype 인스턴스가 자동으로 들어올까요? 생성과 정리 책임을 설명하세요.

**모범 답안**

생성자 주입은 singleton을 만들 때 수행되므로 같은 주입 인스턴스를 계속 사용한다. 매번 새 인스턴스가 필요하면 호출 시점에 조회하는 provider나 메서드 주입 같은 별도 방식을 선택해야 한다. prototype은 컨테이너가 초기화하지만 사용 이후의 파괴 콜백과 자원 정리는 자동으로 모두 관리하지 않으므로 사용 측 책임을 정해야 한다.

**필수 개념**

- prototype의 주입 시점 · 가중치 0.50: singleton 생성 시 주입한 prototype 참조가 자동 교체되지 않는 성질.
- prototype의 사용 후 정리 · 가중치 0.50: 초기화 이후 자원 해제와 파괴 책임을 사용 측에서 관리하는 경계.

**혼동 주의:** prototype이면 모든 메서드 호출에서 자동 생성된다는 오해 제외.

**출처 대조 위치**

- [Spring Framework 7.0 Bean Scopes](https://docs.spring.io/spring-framework/reference/7.0/core/beans/factory-scopes.html) — The Prototype Scope; Singleton Beans with Prototype-bean Dependencies

근거 문서: `spring_framework-v1`. 검수 상태: **PENDING**

### SF-102 · BASIC

동일한 인터페이스를 구현한 빈이 둘입니다. 특정 구현을 쓰는 생성자 매개변수에 @Qualifier를 붙이는 것은 타입 기반 주입을 어떻게 좁히나요? 같은 qualifier가 붙은 컬렉션 주입도 설명하세요.

**모범 답안**

Qualifier는 타입에 맞는 후보 집합 안에서 추가 조건으로 후보를 좁힌다. 빈 이름 하나를 항상 전역에서 직접 조회한다는 의미와는 다르다. 단일 의존성은 필요한 후보가 유일하게 결정되어야 한다. 컬렉션 주입에는 같은 qualifier를 만족하는 여러 빈을 함께 받을 수 있어 qualifier 값이 반드시 유일한 식별자일 필요는 없다.

**필수 개념**

- 타입 후보의 qualifier 필터 · 가중치 0.50: 타입에 맞는 의존 후보를 추가 메타데이터로 좁히는 동작.
- qualifier가 같은 빈의 집합 · 가중치 0.50: 같은 qualifier의 여러 빈을 컬렉션 의존성으로 선택할 수 있는 관계.

**혼동 주의:** qualifier가 항상 유일한 빈 ID여야 한다는 오해 제외.

**출처 대조 위치**

- [Spring Framework 7.0 Qualifiers](https://docs.spring.io/spring-framework/reference/7.0/core/beans/annotation-config/autowired-qualifiers.html) — Fine-tuning Annotation-based Autowiring with Qualifiers: type matching and typed collections

근거 문서: `spring_framework-v1`. 검수 상태: **PENDING**

### SF-103 · INTERMEDIATE

기본 동기 이벤트 발행을 사용하는 Spring 애플리케이션에서 @EventListener가 오래 걸립니다. 이벤트를 발행한 호출자는 곧바로 다음 작업으로 넘어갈까요? 별도 설정이 없는 경우와 비동기 전환 시의 경계를 설명하세요.

**모범 답안**

기본 이벤트 전달은 동기적이어서 리스너가 발행자의 실행 흐름에서 처리되며 느린 리스너가 호출 지연에 영향을 준다. 이벤트를 쓴다는 이유만으로 별도 스레드나 메시지 브로커가 생기지는 않는다. 비동기로 바꾸면 실행 스레드·예외 전달·트랜잭션 컨텍스트의 경계가 달라지므로 그 정책을 함께 설계해야 한다.

**필수 개념**

- 기본 애플리케이션 이벤트의 동기 실행 · 가중치 0.50: 발행자의 호출 흐름에서 리스너가 처리되는 기본 동작.
- 이벤트 비동기 전환의 경계 · 가중치 0.50: 스레드·예외·트랜잭션 관찰 범위가 바뀌는 설계 경계.

**혼동 주의:** ApplicationEventPublisher를 durable message queue로 간주하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Context Events](https://docs.spring.io/spring-framework/reference/7.0/core/beans/context-introduction.html) — Standard and Custom Events; Asynchronous Listeners; Application Event Multicaster

근거 문서: `spring_framework-v1`. 검수 상태: **PENDING**

### SF-104 · INTERMEDIATE

별도 빈의 @Async 메서드가 void를 반환하다가 예외를 던졌습니다. 호출자의 try/catch로 이 실행 실패를 받을 수 있나요? Future 반환 방식과 비교해 설명하세요.

**모범 답안**

비동기 실행 본문의 예외는 호출자가 이미 반환받은 뒤 다른 실행 흐름에서 발생할 수 있어 호출을 감싼 try/catch로 받을 수 없다. void 반환 메서드에는 AsyncUncaughtExceptionHandler 같은 처리 경로가 필요하다. Future 반환 방식은 결과를 관찰할 때 실패를 확인할 수 있다. 작업 제출 자체가 거절되는 오류와 실행 본문의 오류는 구분한다.

**필수 개념**

- void 비동기 메서드의 실행 예외 · 가중치 0.50: 호출자에게 반환값으로 전달되지 않는 실행 실패의 처리 경로.
- 비동기 결과의 실패 관찰 · 가중치 0.50: Future 같은 결과 핸들을 통해 작업 완료와 실패를 확인하는 책임.

**혼동 주의:** 비동기 본문 실패와 Executor의 제출 거절을 같은 시점의 예외로 설명하지 않기.

**출처 대조 위치**

- [Spring Framework 7.0 Task Execution](https://docs.spring.io/spring-framework/reference/7.0/integration/scheduling.html) — The @Async Annotation; Exception Management with @Async

근거 문서: `spring_framework-v1`. 검수 상태: **PENDING**

### SF-105 · INTERMEDIATE

@Transactional(readOnly = true)를 붙였으므로 어떤 DB에서도 쓰기 SQL이 반드시 거부된다는 설명이 맞나요? 읽기 전용 힌트와 쓰기 금지 보장을 구분하세요.

**모범 답안**

readOnly는 실제 트랜잭션 하위 시스템에 전달하는 힌트이며 모든 구현에서 쓰기 거부를 보장하지 않는다. 트랜잭션 관리자·JPA 구현·DB가 이를 활용하는 방식은 다를 수 있다. 권한이나 업무 규칙으로 쓰기를 금지해야 한다면 그 요구를 별도 경계에서 강제하고 선택한 환경에서 검증해야 한다.

**필수 개념**

- 트랜잭션 readOnly 힌트 · 가중치 0.50: 하위 트랜잭션 시스템이 활용하거나 무시할 수 있는 읽기 전용 의도.
- 쓰기 금지 정책의 강제 경계 · 가중치 0.50: 최적화 힌트와 별개로 변경 권한·규칙을 실제로 강제하는 책임.

**혼동 주의:** readOnly가 모든 DB에서 INSERT를 차단한다는 단정 제외.

**출처 대조 위치**

- [Spring Framework 7.0 Transactional API](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/transaction/annotation/Transactional.html) — readOnly() element documentation

근거 문서: `spring_framework-v1`. 검수 상태: **PENDING**

### BOOT-101 · BASIC

Spring Boot에서 직접 DataSource 빈을 등록했더니 기본 내장 DB 자동 설정이 물러났습니다. 자동 설정과 사용자 설정의 관계, 적용 이유를 확인하는 방법을 설명하세요.

**모범 답안**

자동 설정은 클래스패스와 기존 빈 등 조건에 맞춰 기본 구성을 제공한다. 사용자 DataSource가 있으면 해당 기본 구성이 물러날 수 있어 자동 설정이 사용자 구성을 무조건 덮어쓰는 방식은 아니다. --debug로 조건 평가 보고서를 확인해 어떤 자동 설정이 적용되거나 제외됐는지 조사할 수 있다. 모든 자동 설정이 같은 조건을 가진다고 일반화하지 않는다.

**필수 개념**

- 자동 설정의 조건 평가 · 가중치 0.50: 클래스패스와 빈 등 조건에 따라 기본 구성을 적용하는 방식.
- 사용자 설정에 대한 자동 설정 후퇴 · 가중치 0.50: 사용자 빈이 있는 경우 일부 기본 구성이 물러나는 동작.

**혼동 주의:** 자동 설정은 항상 실행되어 사용자 설정을 덮는다는 해석 제외.

**출처 대조 위치**

- [Spring Boot 4.1 Auto-configuration](https://docs.spring.io/spring-boot/4.1/reference/using/auto-configuration.html) — Gradually Replacing Auto-configuration

근거 문서: `spring_boot-v1`. 검수 상태: **PENDING**

### BOOT-102 · INTERMEDIATE

일반 SpringApplication 실행에서 application.properties의 server.port가 8080이고 OS 환경 변수 SERVER_PORT는 8081, 명령행 인수는 --server.port=8082입니다. 다른 설정 원천이 없고 명령행 처리를 끄지 않았다면 어느 값이 적용되나요?

**모범 답안**

명령행 인수의 8082가 적용된다. 이 경우 명령행 속성은 OS 환경 변수와 설정 파일보다 높은 우선순위를 가진다. OS 환경 변수도 일반 설정 파일보다 우선한다. 실제 값은 선언 위치의 우선순위로 결정되며, 테스트 전용 원천이나 명령행 속성 비활성화 같은 추가 조건이 있으면 별도로 확인해야 한다.

**필수 개념**

- 외부 설정 원천의 우선순위 · 가중치 0.50: 같은 설정 키가 여러 속성 원천에 있을 때 최종 값을 선택하는 규칙.
- 설정 해석의 실행 조건 · 가중치 0.50: 명령행 처리 여부와 테스트 속성 등 우선순위를 해석할 때 필요한 전제.

**혼동 주의:** 파일이 항상 환경 변수보다 우선하거나 나중에 만든 파일이 무조건 이긴다는 주장 제외.

**출처 대조 위치**

- [Spring Boot 4.1 Externalized Configuration](https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html) — PropertySource order; Accessing Command Line Properties; Binding From Environment Variables

근거 문서: `spring_boot-v1`. 검수 상태: **PENDING**

### BOOT-103 · INTERMEDIATE

모든 앱 인스턴스가 공유하는 DB가 잠깐 중단됐습니다. DB 상태를 liveness 검사에 넣으면 어떤 연쇄 문제가 생길 수 있나요? readiness에 넣을지는 어떤 기준으로 판단하나요?

**모범 답안**

liveness 실패를 재시작으로 처리하는 환경에서는 공유 DB 장애가 모든 앱의 재시작을 유발해 장애를 키울 수 있다. readiness는 트래픽을 받을 수 있는지를 판단하므로 DB가 필수인지, 대체 동작이 가능한지, 모든 인스턴스가 동시에 제외되는 결과를 검토해야 한다. Spring Boot는 이 선택을 대신해 외부 시스템 검사를 readiness에 전부 넣지 않는다.

**필수 개념**

- 재시작 판단과 프로세스 생존 상태 · 가중치 0.50: 외부 시스템 장애를 앱 자체의 회복 불가능 상태와 구분하는 검사 책임.
- 트래픽 수용 상태와 외부 의존성 · 가중치 0.50: 요청을 받을 수 있는지와 의존 시스템 장애 시의 대체 동작을 평가하는 책임.

**혼동 주의:** 모든 외부 의존성을 liveness와 readiness 양쪽에 무조건 넣는 규칙 제외.

**출처 대조 위치**

- [Spring Boot 4.1 Actuator Endpoints](https://docs.spring.io/spring-boot/4.1/reference/actuator/endpoints.html) — Kubernetes Probes; Checking External State With Kubernetes Probes

근거 문서: `spring_boot-v1`. 검수 상태: **PENDING**

### BOOT-104 · BASIC

Spring Boot의 정상 종료 대기 시간을 설정하면 실행 중인 HTTP 요청이 아무리 오래 걸려도 반드시 끝까지 처리될까요? 정상 종료 신호와 강제 종료를 구분해 설명하세요.

**모범 답안**

graceful shutdown은 새 요청을 받지 않으면서 진행 중 요청에 종료 유예 시간을 주는 방식이다. 대기 시간에는 한계가 있으며 무제한 완료를 보장하지 않는다. 애플리케이션 컨텍스트가 닫히는 정상 종료 경로가 전제이고, 강제 프로세스 종료나 그보다 짧은 외부 종료 제한은 이 절차를 끝까지 수행하지 못하게 할 수 있다.

**필수 개념**

- 새 요청 차단과 진행 요청 유예 · 가중치 0.50: 정상 종료 시 진행 중인 요청에 완료 기회를 주는 동작.
- 종료 유예의 시간·신호 한계 · 가중치 0.50: 유예 시간과 정상 종료 경로가 충족될 때만 가능한 종료 절차.

**혼동 주의:** graceful 설정만으로 SIGKILL 후 정리까지 보장한다고 설명하지 않기.

**출처 대조 위치**

- [Spring Boot 4.1 Graceful Shutdown](https://docs.spring.io/spring-boot/4.1/reference/web/graceful-shutdown.html) — Graceful Shutdown: timeout and proper SIGTERM requirement

근거 문서: `spring_boot-v1`. 검수 상태: **PENDING**

### BOOT-105 · INTERMEDIATE

@SpringBootTest(webEnvironment = RANDOM_PORT) 테스트에서 실제 HTTP 요청으로 데이터를 저장했습니다. 테스트 메서드의 @Transactional 롤백만으로 서버가 저장한 데이터까지 항상 지워질까요?

**모범 답안**

실제 서버와 HTTP 클라이언트 테스트는 서로 다른 스레드와 트랜잭션에서 실행된다. 테스트 쪽 트랜잭션을 롤백해도 서버에서 커밋한 저장 작업까지 같은 트랜잭션으로 롤백되지 않는다. 서버 저장 결과를 재조회해 검증하고 별도의 데이터 정리나 격리된 DB를 사용해야 한다.

**필수 개념**

- 실제 HTTP 테스트의 트랜잭션 분리 · 가중치 0.50: 클라이언트 테스트와 서버 처리가 서로 다른 트랜잭션에서 실행되는 경계.
- 커밋 데이터의 테스트 정리 · 가중치 0.50: 서버가 커밋한 데이터를 만든 테스트가 정리하거나 DB를 격리하는 책임.

**혼동 주의:** 테스트의 @Transactional이 서버 DB의 모든 커밋을 자동 취소한다고 보지 않기.

**출처 대조 위치**

- [Spring Boot 4.1 Testing](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html) — Testing Spring Boot Applications: RANDOM_PORT/DEFINED_PORT and @Transactional note

근거 문서: `spring_boot-v1`. 검수 상태: **PENDING**

### JPA-101 · BASIC

새 엔티티에 EntityManager.persist를 호출했습니다. 이 메서드가 반환됐다는 사실만으로 INSERT와 트랜잭션 커밋이 모두 끝났다고 볼 수 있나요?

**모범 답안**

persist는 새 엔티티를 관리 상태로 만들고 영속화 대상으로 등록한다. DB 반영은 flush나 커밋 과정에서 이뤄질 수 있고 식별자 전략 등으로 더 일찍 SQL이 실행될 수도 있으므로 실행 시점을 하나로 단정하면 안 된다. persist의 반환이나 식별자 부여 자체는 트랜잭션 커밋 성공을 의미하지 않는다.

**필수 개념**

- persist의 관리 상태 전이 · 가중치 0.50: 새 엔티티를 영속성 컨텍스트의 관리 대상으로 만드는 동작.
- 영속화 SQL 실행과 커밋 시점 · 가중치 0.50: SQL 반영 시점과 트랜잭션 확정 시점을 구분하는 규칙.

**혼동 주의:** persist가 항상 즉시 INSERT하거나 항상 커밋까지 수행한다는 주장 제외.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 3.3.2 Persisting an Entity Instance; 3.3.4 Synchronization to the Database

근거 문서: `jpa-v1`. 검수 상태: **PENDING**

### JPA-102 · INTERMEDIATE

비관적 잠금에 jakarta.persistence.lock.timeout 힌트를 지정했습니다. 모든 JPA 구현·DB에서 정확히 그 시간 안에 실패한다는 이식 가능한 보장으로 사용해도 될까요?

**모범 답안**

lock.timeout은 밀리초 단위의 잠금 대기 힌트이지만 모든 DB와 구현이 동일하게 지원하거나 준수한다고 보장할 수 없다. 구현이 잠금 메커니즘과 DB 기능에 맞게 처리하므로 선택한 환경의 지원과 실제 동작을 검증해야 한다. 잠금 대기 시간과 전체 쿼리·트랜잭션 처리 시간도 같은 제한으로 취급하면 안 된다.

**필수 개념**

- 비관적 잠금 대기 힌트 · 가중치 0.50: 잠금 획득 대기 시간에 관한 표준 힌트와 지원 조건.
- 잠금 힌트의 이식성 한계 · 가중치 0.50: DB·구현별 지원 차이 때문에 실행 환경에서 확인해야 하는 경계.

**혼동 주의:** 표준 힌트 이름이라는 이유만으로 정확한 제한을 모든 DB에 보장한다고 설명하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 3.5.4.3 Lock Mode Properties and Uses

근거 문서: `jpa-v1`. 검수 상태: **PENDING**

### JPA-103 · INTERMEDIATE

별도 converter나 @EnumeratedValue가 없는 enum을 @Enumerated(ORDINAL)로 저장했습니다. 상수를 앞에 추가하면 기존 데이터 해석에 어떤 위험이 있나요? STRING을 선택해도 남는 변경 위험을 설명하세요.

**모범 답안**

ORDINAL은 선언 순서의 번호를 저장하므로 앞에 상수를 넣어 번호가 바뀌면 기존 숫자가 다른 상수로 해석될 수 있다. STRING은 이름을 저장하므로 순서 변경에는 덜 민감하지만 이름 변경·삭제 시 기존 저장 값과의 호환성 문제가 남는다. enum 변경을 코드 수정만으로 보지 말고 저장 데이터와 migration 정책을 함께 검토해야 한다.

**필수 개념**

- enum 순번 저장의 호환성 · 가중치 0.50: 선언 순번을 저장해 상수 순서 변경이 데이터 의미를 바꿀 수 있는 매핑.
- enum 이름 저장의 호환성 · 가중치 0.50: 이름을 저장하므로 이름 변경·삭제에 데이터 이행이 필요한 매핑.

**혼동 주의:** 3.2에서 모든 무설정 enum이 언제나 ORDINAL이라고 일반화하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 11.1.18 Enumerated Annotation; 11.1.19 EnumeratedValue Annotation

근거 문서: `jpa-v1`. 검수 상태: **PENDING**

### JPA-104 · BASIC

회원의 별칭 문자열 목록과 독립 ID·변경 이력을 가진 배송지 엔티티 목록을 매핑하려 합니다. @ElementCollection과 엔티티 연관관계 중 어떤 기준으로 구분하나요?

**모범 답안**

기본 타입이나 임베디드 값의 컬렉션은 ElementCollection으로 매핑할 수 있으며 값은 독립적인 영속 엔티티 식별성을 갖지 않는다. 독립 ID로 조회·참조하고 자체 생명주기나 변경 이력을 관리해야 하는 배송지는 엔티티로 모델링하고 연관관계를 사용한다. 컬렉션이라는 형태만 같다고 값과 엔티티를 같은 방식으로 취급하지 않는다.

**필수 개념**

- 값 타입 컬렉션 · 가중치 0.50: 기본 타입이나 임베디드 값을 컬렉션 테이블에 저장하는 모델.
- 독립적인 엔티티 식별성 · 가중치 0.50: 독립 ID와 참조·생명주기를 가진 대상을 값과 구분하는 기준.

**혼동 주의:** ElementCollection 원소를 독립 엔티티처럼 EntityManager.find로 조회한다고 보지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 2.7 Embeddable Classes; 2.8 Collections of Embeddable Classes and Basic Types

근거 문서: `jpa-v1`. 검수 상태: **PENDING**

### JPA-105 · INTERMEDIATE

트랜잭션에 참여한 영속성 컨텍스트에서 기존 관리 엔티티를 수정한 뒤 롤백했습니다. 가지고 있던 Java 객체의 필드가 자동으로 이전 값으로 되돌아간다고 가정해도 될까요?

**모범 답안**

롤백이 DB 변경을 취소한다고 해서 Java 객체의 필드 값을 이전 값으로 자동 복원하는 것은 아니다. 트랜잭션에 참여한 컨텍스트의 기존 관리·삭제 인스턴스는 준영속 상태가 되며 객체에는 롤백 시점의 상태가 남을 수 있다. 버전·생성 값도 DB와 어긋날 수 있어 같은 객체를 그대로 재시도에 쓰기보다 새 트랜잭션에서 필요한 상태를 다시 조회해야 한다.

**필수 개념**

- 롤백 후 객체 값의 비복원 · 가중치 0.50: DB 롤백이 Java 필드의 이전 상태를 자동 재구성하지 않는 성질.
- 롤백 후 준영속 상태와 재조회 · 가중치 0.50: 참여 컨텍스트의 기존 관리 인스턴스가 분리되어 재시도 시 상태 확인이 필요한 경계.

**혼동 주의:** 트랜잭션에 참여하지 않은 extended context까지 같은 규칙이라고 일반화하지 않기.

**출처 대조 위치**

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html) — 3.4.3 Transaction Rollback

근거 문서: `jpa-v1`. 검수 상태: **PENDING**

## 검색에 제공할 근거 문서

### operating_system-v1

운영체제 핵심 동작과 경계 · initial-v1 · OSTEP / xv6 RISC-V rev5 (Unix 계열 개념)

운영체제 학습 근거 · initial-v1
기준: OSTEP / xv6 RISC-V rev5 (Unix 계열 개념)
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[OS-101]
fork의 프로세스 생성과 exec의 프로그램 교체는 별개의 동작이다. fork가 성공하면 부모와 새 자식이 각각 실행을 이어간다. exec가 성공하면 호출한 자식 프로세스의 프로그램 이미지가 교체되고 이전 코드로 돌아오지 않는다. exec 자체가 새 프로세스를 하나 더 만드는 것은 아니다.
근거: OS-API — 5.1 fork, 5.3 exec; OS-XV6 — 1.1 Processes and memory

[OS-102]
자식 종료 대기와 회수는 시간 지연과 다르다. sleep은 시간의 경과만 기다리므로 대상 자식의 완료를 보장하지 않는다. wait 계열로 대상 자식의 종료를 확인하고 상태를 회수한 뒤 부모의 후속 작업을 진행하는 것이 조건 기반 실행 순서다. 오류·중단 반환을 종료 완료로 취급하면 안 된다.
근거: OS-API — 5.2 The wait() System Call; OS-XV6 — 1.1 exit and wait; 9.4 Code: Wait, exit, and kill

[OS-103]
프로세스별 파일 디스크립터 테이블은 별개다. 일반 파일을 연 뒤 fork로 상속한 디스크립터는 같은 열린 파일 상태를 참조하므로 파일 오프셋을 공유한다. 한쪽의 읽기는 다른 쪽이 다음에 읽을 위치에도 영향을 준다. 자식이 자신의 디스크립터를 close해도 부모의 참조는 유지된다. 부모와 자식이 파일을 각각 다시 연 경우는 이 상속 사례와 구분한다.
근거: OS-XV6 — 1.2 I/O and File descriptors, pp. 13–15; OS-API — 5.4 file descriptors; homework 2 (상속한 디스크립터의 입출력 사례)

[OS-104]
POSIX 조건 변수의 알림은 큐의 항목을 예약하지 않는다. 깨어난 뒤 다른 소비자가 항목을 가져갔거나 불필요한 깨움이 생길 수 있으므로, mutex를 다시 얻은 상태에서 while로 조건을 재검사한다. 잠금과 대기 전환의 원자성도 필요하다. 같은 mutex로 상태 검사·변경을 보호하고, wait는 mutex 해제와 대기 등록을 원자적으로 연결하며 반환 전에 mutex를 다시 얻는다. 검사와 대기 사이의 알림 유실을 막는 원리는 xv6의 condition lock과 sleep/wakeup에서도 확인할 수 있다. POSIX mutex와 xv6 커널 잠금의 구현은 서로 구분한다.
근거: OS-CV — 30.1, pp. 2–3; 30.2, pp. 9–14; OS-XV6 — 9.1–9.2, pp. 81–84

[OS-105]
라운드 로빈의 응답성은 첫 CPU 실행 기회를 얼마나 빨리 얻는가와 연결된다. 여기서 응답 시간은 작업 도착부터 첫 CPU 실행까지다. 짧은 타임 슬라이스는 순환 대기를 줄일 수 있지만 문맥 교환의 부가 비용을 더 자주 지불한다. 레지스터 전환과 캐시 효과 등으로 실제 작업에 쓰는 시간이 줄어, 처리량이나 완료 시간이 나빠질 수 있다. 모든 지표가 함께 개선되는 것은 아니다.
근거: OS-SCHED — 7.6–7.7, pp. 6–9; OS-XV6 — 8.2–8.3, pp. 75–76; 8.6, p. 79

출처
OS-API: https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-api.pdf
OS-CV: https://pages.cs.wisc.edu/~remzi/OSTEP/threads-cv.pdf
OS-SCHED: https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-sched.pdf
OS-XV6: https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf

### java-v1

Java 핵심 동작과 경계 · initial-v1 · Java 21

Java 학습 근거 · initial-v1
기준: Java 21
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[JAVA-101]
record는 고정된 컴포넌트 구조와 접근자·동등성 등의 기본 동작을 제공한다. 필드 참조를 다시 대입할 수 없다는 성질은 객체 그래프 전체의 불변성과 다르다. 생성 경계에서 소유권을 분리하고 외부로 내보내는 참조를 점검해야 변경 경로를 통제할 수 있다.
근거: JAVA-CLASS — 8.10.3 Record Members; 8.10.4 Record Constructor Declarations

[JAVA-102]
메서드 호출에는 어떤 선언이 적용 가능한지 고르는 단계와 실행할 인스턴스 구현을 찾는 단계가 있다. 인수의 정적 타입은 오버로드 해석에 참여한다. 런타임의 동적 디스패치는 오버라이딩된 인스턴스 구현에 관한 것이며, 인수의 실제 객체를 보고 모든 오버로드 후보를 다시 비교하는 방식이 아니다.
근거: JAVA-EXPR — 15.12.2 Compile-Time Step 2; 15.12.4.4 Locate Method to Invoke

[JAVA-103]
Object.clone의 기본 복사는 참조가 가리키는 모든 내부 객체를 재귀적으로 만들지 않는다. 바깥 객체는 구분되더라도 내부 리스트는 공유될 수 있다. 필드의 참조 교체와 공유 객체의 변경을 구분하면 복사 후 생기는 예상 밖의 결합을 설명할 수 있다.
근거: JAVA-OBJECT — Object.clone(): implementation requirements

[JAVA-104]
BigDecimal은 정수 형태의 값과 scale을 함께 갖는다. 수치 순서는 같은 수를 나타내는 표현을 같은 위치로 비교할 수 있지만 객체 동등성은 표현 정보까지 반영한다. 시스템이 필요한 것은 수치 비교인지 동일한 표현인지 먼저 정하고, 정규화 규칙을 쓴다면 저장·조회 경계에서 일관되게 적용한다.
근거: JAVA-DECIMAL — class description: cohorts; equals(Object); compareTo(BigDecimal)

[JAVA-105]
인터럽트는 협력적으로 중단 의사를 전달하는 메커니즘이다. Thread.sleep이 InterruptedException을 던질 때 현재 스레드의 인터럽트 상태는 지워진다. 해당 계층에서 취소를 처리하지 못하면 예외를 전파하거나 Thread.currentThread().interrupt()로 상태를 복구해 상위 흐름이 관찰하도록 한다.  대기 API가 예외로 신호를 전달하는 경우와 코드가 상태를 검사하는 경우가 연결되어야 한다. sleep에서 받은 예외를 처리했다는 이유만으로 작업을 계속해도 되는 것은 아니며, 누가 취소를 책임지는지 정해야 한다.
근거: JAVA-THREAD — interrupt(); sleep(long); interrupted(); isInterrupted()

출처
JAVA-CLASS: https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html
JAVA-EXPR: https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html
JAVA-OBJECT: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html
JAVA-DECIMAL: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html
JAVA-THREAD: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html

### spring_framework-v1

Spring Framework 핵심 동작과 경계 · initial-v1 · Spring Framework 7.0.x

Spring Framework 학습 근거 · initial-v1
기준: Spring Framework 7.0.x
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[SF-101]
스코프는 빈을 요청할 때의 인스턴스 제공 규칙이다. 이미 저장한 참조가 매 호출마다 자동 교체된다는 뜻이 아니다. 반복 생성이 필요하면 반복 조회하는 의존성이 있어야 하며, 수명이 짧은 객체가 파일 같은 자원을 소유하면 그 자원을 닫는 주체도 명시해야 한다.
근거: SF-SCOPE — The Prototype Scope; Singleton Beans with Prototype-bean Dependencies

[SF-102]
타입은 어떤 역할의 객체인지 결정하고 qualifier는 그 역할의 후보 중 필요한 특성을 표시한다. 같은 특성을 가진 후보가 여러 개라면 단일 객체를 주입할 때의 모호성과 컬렉션을 주입할 때의 다중 선택을 구분해야 한다.
근거: SF-QUALIFIER — Fine-tuning Annotation-based Autowiring with Qualifiers: type matching and typed collections

[SF-103]
애플리케이션 이벤트는 직접 의존을 줄이는 통신 방법이지만 실행 시간과 실패 전파가 자동으로 분리되는 것은 아니다. 기본 multicaster는 호출 스레드에서 리스너를 실행한다. 비동기 전달을 선택했다면 호출 완료와 후속 처리 완료를 같은 성공으로 취급하지 않아야 한다.
근거: SF-EVENT — Standard and Custom Events; Asynchronous Listeners; Application Event Multicaster

[SF-104]
비동기 API에서는 제출 성공과 작업 성공이 다른 사건이다. 반환값이 없는 비동기 작업의 예외는 별도 관측·처리 경로로 연결한다. Future 등의 결과 핸들을 제공하면 호출 측이 완료와 실패를 관찰할 수 있지만, 핸들을 받았다는 사실만으로 실제 작업 완료가 보장되는 것은 아니다.
근거: SF-ASYNC — The @Async Annotation; Exception Management with @Async

[SF-105]
읽기 전용 선언은 의도 전달과 최적화에 쓰일 수 있지만 이식 가능한 보안 장벽으로 취급하면 안 된다. 해당 힌트를 해석하지 못하는 관리자는 무시할 수 있다. 데이터 변경을 막아야 하는 요구는 도메인 규칙과 DB 권한 등 실제 강제 수단의 책임으로 구분한다.
근거: SF-TX — readOnly() element documentation

출처
SF-SCOPE: https://docs.spring.io/spring-framework/reference/7.0/core/beans/factory-scopes.html
SF-QUALIFIER: https://docs.spring.io/spring-framework/reference/7.0/core/beans/annotation-config/autowired-qualifiers.html
SF-ASYNC: https://docs.spring.io/spring-framework/reference/7.0/integration/scheduling.html
SF-EVENT: https://docs.spring.io/spring-framework/reference/7.0/core/beans/context-introduction.html
SF-TX: https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/transaction/annotation/Transactional.html

### spring_boot-v1

Spring Boot 핵심 동작과 경계 · initial-v1 · Spring Boot 4.1.x

Spring Boot 학습 근거 · initial-v1
기준: Spring Boot 4.1.x
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[BOOT-101]
Boot의 자동 설정은 조건을 만족할 때 제공되는 기본값이다. 특정 기능의 빈을 직접 정의하면 그 기능에 해당하는 기본 설정이 대체될 수 있다. 예상과 다르게 동작할 때는 의존성 유무뿐 아니라 이미 등록된 빈과 조건 보고서를 함께 확인한다.
근거: BOOT-AUTO — Gradually Replacing Auto-configuration

[BOOT-102]
같은 키의 설정이 여러 곳에 있으면 파일을 읽은 순서만으로 추측하지 않고 속성 원천의 우선순위를 확인한다. 일반 SpringApplication 실행에서 다른 원천이 없고 명령행 처리를 끄지 않았다면 명령행 인수가 OS 환경 변수보다, OS 환경 변수가 일반 application.properties보다 우선한다. 따라서 --server.port=8082, SERVER_PORT=8081, 파일의 server.port=8080이 함께 있으면 8082가 적용된다.  일반 실행의 설정 파일·환경 변수·명령행 인수는 서로 다른 원천이다. 명령행 처리를 비활성화하는 옵션이나 테스트 전용 속성처럼 전제를 바꾸는 조건을 명시하면 재현 가능한 설명이 된다.
근거: BOOT-CONFIG — PropertySource order; Accessing Command Line Properties; Binding From Environment Variables

[BOOT-103]
프로세스 재시작으로 회복 가능한 상태와 외부 의존 시스템의 상태를 구분해야 한다. 공유 DB를 다시 시작하지 못하는 앱만 반복 재시작해도 문제가 해결되지는 않는다. 트래픽 제외 또한 가용한 다른 인스턴스와 대체 기능이 있는지에 따라 서비스 전체의 가용성을 바꿀 수 있다.
근거: BOOT-HEALTH — Kubernetes Probes; Checking External State With Kubernetes Probes

[BOOT-104]
정상 종료는 요청을 마무리할 기회를 주는 절차다. spring.lifecycle.timeout-per-shutdown-phase는 종료 단계의 대기 한도를 정한다. 앱의 유예 시간뿐 아니라 실행 환경이 프로세스에 허용하는 시간도 함께 맞춰야 한다. 이미 접수된 장기 작업은 HTTP 종료와 별도의 복구 정책이 필요할 수 있다.
근거: BOOT-STOP — Graceful Shutdown: timeout and proper SIGTERM requirement

[BOOT-105]
RANDOM_PORT는 실제 서버를 시작하므로 테스트 호출 스택과 서버 요청 처리 스택이 분리된다. 테스트에 붙은 트랜잭션은 그 테스트 실행 범위를 보호할 뿐 네트워크 너머의 서버 트랜잭션을 감싸지 않는다. 반복 실행 가능한 테스트에는 생성 데이터의 정리 책임이 명시되어야 한다.
근거: BOOT-TEST — Testing Spring Boot Applications: RANDOM_PORT/DEFINED_PORT and @Transactional note

출처
BOOT-AUTO: https://docs.spring.io/spring-boot/4.1/reference/using/auto-configuration.html
BOOT-CONFIG: https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html
BOOT-HEALTH: https://docs.spring.io/spring-boot/4.1/reference/actuator/endpoints.html
BOOT-STOP: https://docs.spring.io/spring-boot/4.1/reference/web/graceful-shutdown.html
BOOT-TEST: https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html

### jpa-v1

Jakarta Persistence 핵심 동작과 경계 · initial-v1 · Jakarta Persistence 3.2

Jakarta Persistence 학습 근거 · initial-v1
기준: Jakarta Persistence 3.2
직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.

[JPA-101]
관리 상태 전이, SQL 실행, 트랜잭션 확정은 서로 다른 사건이다. 새 객체를 persist한 뒤 관리 상태가 됐다는 사실과 DB 트랜잭션의 성공 여부는 분리해 확인한다. 구현이나 식별자 전략이 SQL을 일찍 실행해도 이후 롤백 가능성이 사라지는 것은 아니다.
근거: JPA-SPEC — 3.3.2 Persisting an Entity Instance; 3.3.4 Synchronization to the Database

[JPA-102]
표준 이름의 힌트라도 모든 실행 환경에서 강제되는 계약은 아닐 수 있다. 잠금 대기 한도를 요구한다면 표준 문서의 이식성 경고와 사용하는 DB·드라이버·구현의 지원을 함께 확인한다. 서비스 응답 시간 제한은 잠금 외의 실행 단계까지 포함하므로 별도 경계다.
근거: JPA-SPEC — 3.5.4.3 Lock Mode Properties and Uses

[JPA-103]
enum 매핑 방식은 Java 이름과 DB 값 사이의 계약이다. 명시적 ORDINAL과 커스텀 값 필드가 없는 경우에는 선언 순서가 저장 의미에 영향을 준다. 명시적 STRING은 이름을 계약으로 삼는다. Jakarta Persistence 3.2의 EnumeratedValue 같은 별도 매핑 조건이 있는 경우는 이 단순 비교의 전제에서 제외한다.
근거: JPA-SPEC — 11.1.18 Enumerated Annotation; 11.1.19 EnumeratedValue Annotation

[JPA-104]
컬렉션 매핑을 고를 때는 원소가 단순 값인지 독립적으로 식별되는 대상인지 먼저 판단한다. ElementCollection의 원소는 기본 값 또는 임베디드 값으로 컬렉션 테이블에 저장된다. 독립 식별성을 가진 엔티티는 엔티티 간 관계로 표현하며 그 생명주기 정책도 별도로 정한다.
근거: JPA-SPEC — 2.7 Embeddable Classes; 2.8 Collections of Embeddable Classes and Basic Types

[JPA-105]
DB 트랜잭션과 메모리 객체의 변경 기록은 같은 저장소가 아니다. 롤백 이후 참조를 가지고 있어도 그 값이 현재 DB를 대표한다고 보장되지 않는다. 특히 버전과 생성된 값이 불일치할 수 있으므로 재시도는 새로운 실행 경계에서 상태를 다시 읽고 판단하는 흐름으로 구성한다.
근거: JPA-SPEC — 3.4.3 Transaction Rollback

출처
JPA-SPEC: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html

## 출처 이용 조건 검수

원문·코드·그림 미포함. 아래 메모는 법률 검토나 공개 승인 결과가 아님.

- [OSTEP 5: Process API](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-api.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 30: Condition Variables](https://pages.cs.wisc.edu/~remzi/OSTEP/threads-cv.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [OSTEP 7: Scheduling](https://pages.cs.wisc.edu/~remzi/OSTEP/cpu-sched.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [MIT xv6 book, RISC-V revision 5](https://pdos.csail.mit.edu/6.1810/2025/xv6/book-riscv-rev5.pdf): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS 21 Chapter 8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [JLS 21 Chapter 15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java 21 Object API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java 21 BigDecimal API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Java 21 Thread API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Bean Scopes](https://docs.spring.io/spring-framework/reference/7.0/core/beans/factory-scopes.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Qualifiers](https://docs.spring.io/spring-framework/reference/7.0/core/beans/annotation-config/autowired-qualifiers.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Task Execution](https://docs.spring.io/spring-framework/reference/7.0/integration/scheduling.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Context Events](https://docs.spring.io/spring-framework/reference/7.0/core/beans/context-introduction.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Framework 7.0 Transactional API](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/transaction/annotation/Transactional.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Boot 4.1 Auto-configuration](https://docs.spring.io/spring-boot/4.1/reference/using/auto-configuration.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Boot 4.1 Externalized Configuration](https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Boot 4.1 Actuator Endpoints](https://docs.spring.io/spring-boot/4.1/reference/actuator/endpoints.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Boot 4.1 Graceful Shutdown](https://docs.spring.io/spring-boot/4.1/reference/web/graceful-shutdown.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Spring Boot 4.1 Testing](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html): 원문 저작권은 원저작자 소유. 본문·예제 코드·도표 재배포 없이 출처 링크와 새로 작성한 한국어 설명만 포함. 공개 전 사람의 출처·이용 조건 검수 대기.
