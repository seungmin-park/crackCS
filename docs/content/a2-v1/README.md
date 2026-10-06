# A2 CS·객체 설계 콘텐츠

- 독자·질문: 콘텐츠 제작자·검수자 / 자료구조·알고리즘·네트워크·객체 설계와 그 기반을 어떤 사례로 검수할 것인가?
- 상태: **DRAFT 초안 제작**. 관리자 등록·사람 검수·공개·검색·실제 모델 실행 미실행
- A2: [확장 커리큘럼](../backend-curriculum/README.md#콘텐츠-제작-순서)의 작업 묶음. 난이도 등급과 별개
- 수정 원본: [bundle.json](bundle.json). 사람이 읽을 자료: [review.md](review.md). 실행 결과: [verification.md](verification.md)
- 공개 기준: [콘텐츠 정책](../../product/content-and-ai-policy.md). 승인된 기존 콘텐츠는 [initial-v1](../initial-v1/README.md)에서 유지

## 이번 묶음의 범위

| Topic | 문항·개념·문서 | 답변에서 확인할 직관 |
|---|---|---|
| `CS_FOUNDATIONS` | 5·10·1 | 단위 변환·정수 범위·논리 반례·로그 증가·평균과 p95 |
| `COMPUTER_ARCHITECTURE` | 5·10·1 | 직렬 비용·지역성·캐시 지연·주소 변환·interrupt와 DMA |
| `DATA_STRUCTURES` | 5·10·1 | 탐색과 변경 비용·FIFO 상태·충돌·상위 K개·그래프 표현 |
| `ALGORITHMS` | 5·10·1 | 첫 일치 탐색·안정 병합·BFS 조건·greedy 반례와 DP·상환 비용 |
| `NETWORKS` | 5·10·1 | TCP 메시지 경계·UDP 최신성·흐름/혼잡 제어·DNS TTL·TLS 종료 |
| `OBJECT_ORIENTED_DESIGN` | 5·10·1 | 불변식·정책 협력·행동 계약·업무/전송 경계·값과 신원 |
| 합계 | **30·60·6** | BASIC 9·INTERMEDIATE 21 |

각 문항의 필수 개념 2개, 가중치 각각 0.50. 난이도는 문항의 설명·적용 깊이이며 학습자의 연차를 인증하는 등급이 아님. 주제당 5문항은 검수할 첫 묶음. 재귀·DFS·IP 라우팅·컴퓨터 명령 실행·DDD 전체 등 커리큘럼의 모든 개념을 완성한 결과가 아님. 심화·보조 개념은 다음 제작에서 별도 확정.

```text
수학·표현 → 자료구조 → 알고리즘
     └────→ 컴퓨터구조 → 메모리·I/O 비용
네트워크 → 연결·전송의 보장 → 앱 프로토콜의 책임
객체 설계 → 상태 소유자 → 검증·정책·외부 협력
```

선수 지식을 보여주는 안내. 강제 학습 순서나 코드 실행·온라인 저지 기능을 추가한 것은 아님.

## 원본·근거·진단의 책임

- `bundle.json`: 6개 Topic·60개 Concept·30문항·6문서·26개 출처·판정 기준의 단일 원본
- `review.md`: 기존 renderer로 만든 읽기 사본. 원본 수정 후 재생성
- 근거 문서: 사례 전제 → 설명 → 실패 조건 → 개념별 핵심 → 출처 절과 작은 텍스트 도식
- 출처: MIT·Princeton 교재, Cornell CS3410·OSTEP, NIST, Google SRE, Java 21 명세, RFC 원문, Fowler 원저자 글, Liskov·Wing 논문
- 출처 기록: URL·확인일·UTF-8 수집 markdown의 SHA-256·절 위치·이용 메모. 수집 원문은 git 제외 `.firecrawl/a2-20261007/`에 보관
- 원문·코드·도표 미배포. 상황·한국어 설명 직접 작성. 사람의 사실·출처·이용 조건 검수는 별도
- 설계 제안: 원형 큐 상태, lower_bound, 앱 framing/버전, 할인 정책·port/adapter는 명시한 계약의 자체 사례. 문서가 특정 구현을 유일한 정답으로 명령하는 것으로 해석하지 않음

```text
출처 본문 + 명시한 사례 계약
           ↓
질문·답안·필수 개념·판정 경계
           ↓
구조·계산 예시 확인 → 사람이 읽는 검수본
           ↓ 다음 단계
사람 검수 → 선택한 Topic 등록·공개 → 검색·실제 모델·독립 표본
```

문항당 5개, 총 **150개 작성자 진단 사례**:

| 사례 | 작성자 예상 판정 |
|---|---|
| 정답 | 모든 필수 개념 CORRECT |
| 다른 표현·허용 대안 | 문구·해법이 달라도 계약을 충족하면 CORRECT |
| 일부 누락 | 첫 개념 CORRECT, 마지막 개념 PARTIALLY_CORRECT |
| 핵심 모순 | 두 개념의 핵심을 각각 뒤집어 INCORRECT |
| 근거 누락 | 정답과 같은 답변, 필요한 근거 전부 미제공 → NEEDS_REVIEW |

예상값은 작성자 개발 자료. 독립 사람 라벨·실제 모델 응답·채점 정확도 결과가 아님. 기존 앱과 importer는 이 진단·상세 rubric을 모델 입력으로 자동 실행하지 않음. 모델 진단 runner 연결은 후속 작업. 근거 누락을 답변 오답으로 바꾸어 표기하지 않음.

## 기존 자료와의 중첩

정규화한 문장 완전 중복과 의미·개념 중첩은 별개. 이 묶음과 합성 답변을 독립 golden·대표 학습자 표본으로 재사용하지 않음.

| A2 문항 | 기존 자료 | 공유하는 판단 |
|---|---|---|
| A2-DS-01 | reference-v1 CS-10 | 배열 접근·리스트 위치 탐색과 삽입 비용 |
| A2-DS-03 | reference-v1 CS-08 | 해시의 평균/최악 비용·충돌 |
| A2-ALG-03 | reference-v1 CS-09 | BFS의 단위 가중치 조건 |
| A2-NET-01 | reference-v1 CS-04 | TCP write/read와 framing |
| A2-ARCH-04 | reference-v1 CS-03 | 가상 주소·fault와 디스크 읽기의 구별 |
| A2-OOD-05 | reference-v1 JAVA-03, initial-v1 JAVA-101/103 | 참조 고정·가변 내부 상태·값 공유 |
| A2-NET-02/05 | A1 API_DESIGN·HTTP, 기존 Security 시나리오 | 전송과 업무 중복 효과·사용자 인가의 다른 책임 |

공통 기초를 학습 범위에 다시 포함한 선택. 기존 OS의 모델 평가 결과로 새 A2 품질을 추정하지 않음. 문자 유사도와 완전 중복 수는 [실행 기록](verification.md)에서 관리.

## 검사·읽기 자료 재생성

저장소 루트, Python 3 표준 라이브러리와 Java 21:

```bash
python3 scripts/content_bundle.py --bundle docs/content/a2-v1/bundle.json --report build/reports/a2-v1/bundle.json
python3 scripts/render_content_review.py --bundle docs/content/a2-v1/bundle.json
python3 docs/content/a2-v1/worked-examples.py
java --source 21 docs/content/a2-v1/JavaExamples.java
bash scripts/verify.sh all
```

- 구조 검사: 같은 Topic의 문항·문서·개념 연결, 필수 근거 연결, 문항 최소량, 중복 문장, 가중치
- [Python 예제](worked-examples.py): 45개 assertion. 단위·p95·캐시·힙·lower_bound 경계·병합·BFS·동전 DP의 작은 입력·상환 복사량·윈도/TTL 계산
- [Java 예제](JavaExamples.java): 실제 Java 21 정수 연산·확대 시점 3개 assertion
- 예제는 독립 학습 코드. 앱 도메인·DB·네트워크의 실행 검사, benchmark, 복잡도 증명, 전체 입력에 대한 정확성 증명과 구분
- `--apply` 없는 구조 검사는 앱 등록을 하지 않음. 이번 작업에서 `--apply` 미사용
- 공용 테스트와 CI 통과는 콘텐츠의 사실·의미 독립성·사람 승인·검색·모델 품질을 대신하지 않음

## 사람 검수·공개 순서

1. [review.md](review.md)에서 전제·답안·반례·허용 대안·개념별 경계 확인
2. 26개 출처의 해당 절·버전·이용 조건 대조. 설계 제안과 명세의 사실 구별
3. 150개 작성자 예상값을 사람 기준으로 수정·승인. 맞은 개념을 전체 판정에 맞춰 낮추지 않음
4. 출시 Topic·노출 범위 기록. taxonomy 활성 등록도 지식 지도에 영향을 줄 수 있으므로 함께 검토
5. 기존 관리자 API로 DRAFT 등록 → 사람 검수 → 공개 → 검색 문단 생성. 기존 승인 버전·과거 평가 근거 보존
6. 검색으로 필수 근거가 실제 제공되는지 확인하고 독립 대표 답변·실제 모델·후속 학습으로 품질 검증. 로컬 E2E는 현재 cmux에 표시

승인 기록: **미확정**. 실제 검수자·일시·원본 hash·승인/수정/보류 결과는 승인 이후 기록. PR 머지는 콘텐츠 공개 승인이 아님. 기존 승인 Topic·A1/A2 검수·B/C 제작·운영 준비 상태는 [tasks.md](../../planning/tasks.md)에서 관리.
