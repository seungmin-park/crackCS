# Java 사용자 검수·로컬 공개·학습 흐름

- 독자·질문: 콘텐츠 담당자·프로젝트 소유자 / OS에서 확인한 검수·공개·근거·학습 흐름이 Java에서도 이어지는가?
- 상태: Java21 5문항·10개 Concept·문서1개 사용자 검수·로컬 공개 완료. cmux 등록·공개·학습 assertion46/46 PASS
- 선행: [기존 답변 사용자 검수](../2026-10-05-learner-review/verification.md) → [통제 장애 복구](../2026-10-05-provider-fault-flow/verification.md)
- 기준: main `c4595f2` / `test/java-content-flow`, worktree `.worktrees/learner-review`
- 검수 기록: [review-record.json](review-record.json). 표시명 `사용자 검수`, 사용자 위임에 따른 실행자·방법·승인 범위 별도 기록
- 실행 도구: [verify_java.py](verify_java.py), [실제 결과](runtime-results.json). 운영 코드·원본 DRAFT 번들 변경 없음

## 검수와 공개 범위

| 문항 | 확인한 사실과 경계 | 공식 근거 |
|---|---|---|
| JAVA-101 | final 컴포넌트 참조와 대상 리스트의 가변성 구분. 생성·반환 경계의 변경 경로 확인 | [JLS21 §8.10.3·8.10.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html) |
| JAVA-102 | Object 정적 인수 타입으로 print(Object) 선택. 선택된 시그니처의 수신 객체 dispatch와 구분 | [JLS21 §15.12.2·15.12.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) |
| JAVA-103 | Object.clone 기본 얕은 복사. 복사본 필드 재대입과 공유 리스트 변경의 차이 | [Object.clone API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) |
| JAVA-104 | 2.0·2.00의 equals false·compareTo 0. 수치와 scale 정책 구분 | [BigDecimal API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html) |
| JAVA-105 | sleep의 InterruptedException에서 상태 소거. 취소 책임에 따른 종료·전파·복구 | [Thread API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html) |

- 문항·모범 답안·오개념 메모·필수 개념2개·각 가중치0.50 대응 확인
- 출처5개 본문 재확인: 2026-10-05, `.firecrawl/java-review/`에 수집본 보관. 원본 번들의 2026-09-27 출처 해시 덮어쓰기 없음
- 원문·코드·그림 재배포 없이 직접 작성한 한국어 설명과 출처 링크만 공개. 이용 권리 확보·법률 검토 완료 주장 없음
- reference-v1과 JAVA-101/103/105의 공유 개념 확인. 원문 완전 중복0, 최근접 문장 유사도0.313~0.369. 의미적 독립 모델 표본으로 사용 없음
- 검수 고정: 18:23:02 KST, Java 전용 DRAFT payload SHA `e91d4a1bbe4bef1ab6092d51dab8cad49a80212d0296865f250353dcabe0cc24`
- 공개 승인: 사용자의 순차 진행·검수 위임 지시 범위에서 이번 로컬 Java 콘텐츠만 승인. 공개 서비스·전체 파일럿 승인 별도
- 공개 전 수정: 문서의 승인 대기 문구와 license 메모만 완료 표시로 변경. 사실·문항·모범 답안·개념 기준 유지

## 상태 소유자와 실행

```text
원본 DRAFT + 공식 근거 → 사용자 검수 기록 고정
                               ↓
기존 관리자 API → 미검수 DRAFT → 실제 화면 검수 → 실제 화면 공개
                                                      ↓
                                       KnowledgeDocument v1 → Chunk7
                                                      ↓
학습자 실제 제출 → Answer 보존 → Retrieval → 로컬 HTTP 합성 응답
                                                      ↓
                        Evaluation 완료 → Java v1 Evidence → 개념당 지식 반영1회
```

- 문서·문제: 검수자·검수 시각·공개 상태 소유. 직접 DB 쓰기·검수 필드 조작 없음
- Chunk: 공개 문서의 본문 offset·checksum 소유. 승인 내용과 문단5개 대응 확인
- Answer·Evaluation: 제출 원문·평가 상태·근거 소유. 실제 Service·HTTP adapter·PostgreSQL 사용
- KnowledgeApplication: EvaluationConcept별 unique 적용 소유. 기존 OS 상태 보존·Java10개 각1회 확인
- 외부 응답: 앞 단계의 로컬 provider만 대체. 모델명 `controlled-openai-contract`, 실제 GPT 품질 검증과 구분

```bash
# loopback 앱18080·frontend5174·로컬 provider18082·기존 OS PostgreSQL 준비
python3 -u docs/changes/2026-10-05-java-content-flow/verify_java.py --phase register --workspace workspace:2 --surface surface:26
python3 -u docs/changes/2026-10-05-java-content-flow/verify_java.py --phase publish --workspace workspace:2 --surface surface:26
python3 -u docs/changes/2026-10-05-java-content-flow/verify_java.py --phase learn --workspace workspace:2 --surface surface:26
bash scripts/verify.sh all
```

- 이번 세션의 surface 번호. 재실행 전 환경 workspace·identify·tree로 확인, 발견한 번호 사용
- 원본에서 Java만 선택하여 기존 등록 도구 사용. `register`는 미검수 DRAFT만 허용, `publish`는 고정 payload hash 확인
- `learn`은 Java10개 모두 UNKNOWN·attempt0인 회원만 허용. 완료한 회원으로 반복 실행 금지
- 등록 ID: Topic2·Concept12~21·Document4 v1·Question34~38
- 문서·문제 검수자 DB ADMIN1, 18:24:24~29 KST. 표시·권한·실행자 관계는 검수 기록 참조
- 공개 문서 checksum `ae960a8881485cd4e9ad8616282678f3c90595587054445de6312aa7106c13d0`, Chunk7개
- 학습자: 기존 통제 검증 회원4, Java Answer/Evaluation34~38. 다른 회원 기존 답변 수정 없음
- 호출 workspace2/surface2 확인, pane10 재사용. surface25 러너, surface26 실제 브라우저, surface27 개발 서버·전체 검사 로그
- 정상 응답5회는 로컬 합성. 실제 OpenAI 호출0, 후속 생성은 기존 무료 stub
- 앱 JAR의 운영 소스는 이전 `43e4649`와 현재 `c4595f2` 사이 동일. 차이는 이전 장애 검증 도구·문서뿐

## 검증 결과

| 실행 | 결과 |
|---|---|
| 등록 | 5문항·10개 개념·문서1개 구조 검사, 미검수 DRAFT 유지. assertion7/7·exit0 |
| 검수·공개 | 검수 기록 hash, 관리자 검수 메타데이터, 승인 본문·checksum·문단 offset, 공개5문항. assertion15/15·exit0 |
| 학습 | 실제 UI 제출5개, 원문·정답 화면, Java v1 근거만 사용, 지식 상태1회, OS 보존. assertion24/24·exit0 |
| DB 재조회 | Java Answer5·Evaluation5·EvaluationConcept10·KnowledgeApplication10 |
| 최종 지식 | Java Concept12~21 각 attempt1·LEARNING·mastery100·confidence25 |
| 전체 공용 검사 | Python57 + backend518 + frontend338 + PostgreSQL65 = 978개 성공. 실패·오류·skip0·exit0, 타입·production build·스택19항목 PASS |

- 화면: [검수·공개 문서](assets/java-document.png), [학습 답변](assets/java-answer.png), [최종 지식 지도](assets/java-knowledge.png)
- 로컬 로그: `.firecrawl/java-review/{register,publish,learn,verify-all}.log`와 대응 `.exit`, 원시 결과 `runtime/`
- 러너 준비 오류1회: `cmux find`의 출력은 `OK`이므로 버튼 ref 파싱 실패. 저장·검수·공개 전 중단, 현재 snapshot에서 버튼 ref를 읽도록 수정 후 전체 공개 흐름 통과. 실패 로그 별도 보존
- 환경 복구: 단계2 후 기존 OS DB 컨테이너가 종료되어 connection refused 발생. 같은 볼륨으로 재시작, 기존 답변9개 유지·health UP 확인. 종료 원인 미확정, OpenAI 장애로 기록 없음
- 전체 검사 뒤 개발 서버 화면 준비가 지연된 관찰 별도 보존. 개발 서버 재시작 후 실제 DOM·모든 클릭 확인, 운영 코드 수정 없음

## 남은 경계

- 이번 Java 결과는 콘텐츠 공개·근거·학습 파이프라인 검증. 실제 GPT의 Java 채점 정확도·혼합 오류 대응·후속 질문 품질 미검증
- OS 기존4개 사용자 검수 완료와 독립 대표 학습자 표본 검증은 구분. 짧은 표현·용어 오류·혼합 오류를 포함한 독립 대표 표본 Gate 미완료
- initial-v1 로컬 승인 범위 OS·Java 10문항/20개 Concept, 나머지3개 Topic15문항 검수·승인 대기
- 파일럿 진입·공개 배포 보류 상태 유지. 이번 작업 프로세스만 종료, DB volume·원문·검수 메타데이터·cmux 결과 pane 보존
