# 초기 학습 콘텐츠 initial-v1

## 상태와 범위

- 검수용 읽기 자료: [문항·답안·개념·근거를 모은 화면](review.md). 원본 변경 시 `python3 scripts/render_content_review.py`로 재생성
- 기준 원본: [bundle.json](bundle.json)
- 준비: 2026-09-27, AI 작성·출처 본문 대조·자동 구조 검사
- **등록용 DRAFT**: 자동 공개 없음. [OS 승인 기록](../../changes/2026-10-05-os-content/verification.md), [Java 사용자 검수·승인·실행 기록](../../changes/2026-10-05-java-content-flow/verification.md)은 별도 관리. OS·Java 각5문항 로컬 공개 승인 완료, 나머지3개 Topic 승인 대기
- 5개 말단 Topic, Topic당 5문항·1문서, 총 25문항·50개 필수 Concept·5문서
- 이 원본은 기존5개 Topic 등록 템플릿. CS·HTTP/API·객체 설계·테스트·운영의 신규 제작 범위는 [백엔드 커리큘럼](../backend-curriculum/README.md)에서 관리. 확장 계획이 이 번들의 문항·승인 수를 변경하지 않음
- 각 문항: 필수 개념 2개, 가중치 각 0.50, 합계 1.00
- 난이도: BASIC 8, INTERMEDIATE 17. 심화 질문·전 분야를 포괄하는 문제집 아님
- 등록 도구: 기존 관리자 API 사용. DB 직접 삽입·검수 필드 조작 없음

| Topic | 버전 기준 | 질문 범위 |
|---|---|---|
| OPERATING_SYSTEM | OSTEP·xv6 RISC-V rev5 | fork/exec, wait, 파일 디스크립터, 조건 변수, 라운드 로빈 |
| JAVA | Java 21 | record의 가변 참조, 오버로드, 얕은 복사, BigDecimal, interrupt |
| SPRING_FRAMEWORK | Spring Framework 7.0.x | prototype, qualifier, 동기 이벤트, 비동기 예외, readOnly |
| SPRING_BOOT | Spring Boot 4.1.x | 자동 설정, 외부 설정 순위, 생존·트래픽 수용 검사, 정상 종료, HTTP 테스트 |
| JPA | Jakarta Persistence 3.2 | persist 시점, 잠금 힌트, enum 저장, 값 컬렉션, 롤백 후 객체 |

```text
출처 본문 → 개념별 설명·문제·모범 답안
                      ↓ 구조 검사
               관리자 API → DRAFT
                      ↓ 사람 검수
               검수 → 공개 → 검색 문단 생성
```

## 검사와 등록

저장소 루트, Python 3 표준 라이브러리만 사용:

```bash
python3 -m unittest discover -s scripts -p 'test_content_bundle.py'
python3 scripts/content_bundle.py --report build/reports/content/audit.json
```

[로컬 PostgreSQL 실행법](../../changes/2026-09-27-release-readiness/local-runbook.md)으로 서버를 시작한 뒤:

```bash
python3 scripts/content_bundle.py --apply --report build/reports/content/import.json
```

- 기본 대상: `http://127.0.0.1:8080`, 기본 관리자 `admin@crackcs.local`
- 비밀번호: 터미널 비표시 입력. 인수·로그에 기록 금지
- 기존 로컬 관리자 예시 비밀번호는 로컬 실행법 참조
- 다른 로컬 포트: `--base-url http://127.0.0.1:포트`
- 다른 관리자: `--admin-email 이메일`
- 원격 대상 거부, HTTP redirect 거부
- 조회·생성 권한은 서버의 기존 ADMIN 검사 사용
- 동일한 내용의 미검수 초안: 기존 ID 재사용
- 내용·가중치 충돌, 비활성 분류, 검수·공개된 콘텐츠: 중단, 자동 덮어쓰기 없음
- 중도 실패: 완료된 생성은 유지. 로그 원인 해결 후 재실행으로 나머지 이어 등록
- 번들 전체 원자적 transaction 및 동시 importer 실행은 미지원. 한 명이 한 번씩 실행
- 초안의 제목·질문 본문은 재실행 식별 기준. 등록 후 이를 수정했다면 같은 번들 자동 재실행 금지; 저장한 `registeredIds`로 기존 초안 확인

## 출처·라이선스 경계

- 원문 20개: Java/Jakarta 공식 명세·API, Spring 공식 문서, Wisconsin OSTEP·MIT xv6 교재
- 운영체제 문항마다 교재 2종 연결
- 원본별 URL·확인일·본문 SHA-256·이용 메모: `sources`
- 문항별 근거 절 위치: `sourceRefs`; 자료 전체 복사 대신 새 한국어 설명
- 기존 수집 자료 4개 재대조, 나머지 16개 2026-09-27 수집
- 수집 본문은 git 제외 `.firecrawl/`에 보관. 새 수집본과 기존 수집본의 구분은 `retrieval` 참조
- 원문·예제 코드·도표를 배포하지 않음. 원문 이용 권리를 확보했다는 주장이나 법률 검토 완료 표시 없음
- 기술 버전은 해당 문항의 전제. 이동 가능한 패치 버전 문서는 URL·확인일·해시를 함께 확인
- **공개 전 사람의 사실·출처·이용 조건 검수 필요**. 자동 구조 검사 통과로 이를 대체하지 않음

## reference-v1과의 관계

- reference-v1 60문항 원본·manifest 수정 없음
- 문장 정규화 후 완전 중복 0건. `SequenceMatcher` 최근접 문장 유사도 최대 0.408
- 유사도는 단어·문장 형태 비교이며 의미적 독립성을 증명하지 않음
- 공유 개념을 의도적으로 기록하고 모델 평가의 새로운 독립 표본으로 사용하지 않음

| 새 문항 | 기존과 공유하는 개념 | 구분되는 질문 |
|---|---|---|
| JAVA-101 | JAVA-03 컬렉션의 불변성 | record의 생성·접근 경계 |
| JAVA-103 | JAVA-02 참조 재대입, JAVA-03 가변 공유 | Object.clone의 복사 깊이 |
| JAVA-105 | JAVA-13 협력적 취소 | sleep 예외 발생 뒤 상태 소거·전달 책임 |
| SF-103 | SPRING-10 이벤트 처리 | 일반 이벤트의 기본 동기 전달; AFTER_COMMIT 타이밍과 구분 |
| SF-104 | JAVA-13 비동기 결과 | void @Async 실행 예외의 관측 경로 |
| JPA-101 | JPA-02 flush/commit 구분 | persist의 관리 상태 전이와 SQL 시점 |
| JPA-105 | JPA-02 롤백, JPA-09 재시도 | 롤백 뒤 Java 객체 값·준영속 상태 |

- 분포 한계: OS는 프로세스·동기화 중심, DB 질의·네트워크·보안·AX 업무 문제 미포함
- 균등 Topic 수가 실제 채용 빈도나 충분한 학습 범위를 보장하지 않음
- 이번 초안으로 학습한 모델의 품질을 같은 개념의 reference-v1 결과만으로 일반화하지 않음

## 사람 검수·공개 순서

1. 관리자 `분류와 개념`: 주제·설명 확인
2. 관리자 `근거 문서`: initial-v1 문서의 본문·출처·버전·라이선스 메모 대조
3. 관리자 `문제`: 원문·모범 답안·필수 개념 2개·가중치·오개념 메모 확인
4. 수정이 필요하면 DRAFT 수정 후 번들과 등록 ID의 차이 기록
5. 동의한 문서와 문제만 각각 `검수` → `공개`
6. 공개 문서의 `검색 문단 생성`, 공개 질문 수·근거 포함 확인

공개 최소량 OQ-006 충족 여부: [초기 콘텐츠 Gate](../../changes/2026-09-21-phase-8/content-readiness.md). 현재 준비된 초안 수와 승인된 공개 수를 구분.
