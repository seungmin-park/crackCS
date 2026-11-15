# R1 제작·검증 기록

- 독자·질문: 제작자·검수자 / 어떤 원본을 무엇으로 확인했고 아직 무엇을 확인하지 않았는가?
- 확인일: 2026-11-15
- 브랜치: `docs/content-rebuild`(17개 Topic·156문항), `docs/r1-remaining-topics`(4개 Topic·20문항 추가)
- 기준 원본: [bundle.json](bundle.json), SHA-256 `bc92cc020642431e91a64795e7be7efe1c96d04f2ea871038b823f9831dbe0cd`
- 산출물·검수 절차: [README](README.md), [읽기 사본](review.md)

## 제작 결과

- 21개 Topic·176문항·352개 Concept·21문서·135개 출처
- BASIC 37, INTERMEDIATE 127, ADVANCED 12
- 문항마다 필수 개념 2개, 가중치 0.50+0.50, 근거 문단 1개, 출처 위치 1곳 이상
- 앱·importer·renderer 변경 없음

## 실행 명령·판정

| 명령·검사 | 결과 | 범위 |
|---|---|---|
| `python3 -m unittest discover -s scripts -p 'test_*.py'` | 63/63, exit 0 | 기존 Python 도구 테스트 |
| `python3 scripts/content_bundle.py --bundle docs/content/r1-v1/bundle.json` | exit 0, 176·21·352 | Topic 최소량·가중치·출처·문서·Topic 연결·문장 중복 |
| `python3 scripts/render_content_review.py --bundle docs/content/r1-v1/bundle.json` | exit 0, 176문항·21문서 | 읽기 사본 생성 |
| `python3 scripts/check_stack_docs.py` | 19개 PASS | 기술 스택 문서 목록. 의존성 변경 없음 |
| 출처 본문 수집·해시 | 135/135 HTTP 200, SHA-256 기록 | 2026-11-15 수집본 기준 |
| 출처 위치 대조 | 176문항의 절 제목·인용 문구가 수집 본문에 존재 | 위치 확인. 답안과 원문의 의미 대조는 아님 |
| 문장 검사 | 0건 | 질문 합쇼체·답안 평서체, 한글 뒤 숫자·영문 띄어쓰기, 조사, 길이, 개념명 중복 |
| reference-v1 문자 유사도 | 최대 0.575 (R1-JPA-11 ↔ JPA-08) | 문자 비교. 의미 중첩은 [README](README.md#기존-자료와의-중첩) |
| `git diff --check` | 공백 오류 0 | diff 형식 |

- 출처 수집본과 대조 도구는 로컬 보존, Git 제외
- OSTEP PDF 7개는 본문 텍스트를 추출해 대조. 추출 과정에서 합자·줄바꿈 하이픈이 섞여 해당 문구는 인용에서 제외
- Spring Framework 7.0 참조 문서 URL은 현재 안정판 경로로 이동됨. 수집 시점 표시 버전 7.0.10
- Amazon Builders' Library 문서 URL은 builder.aws.com으로 이동됨. Kafka 설계 문서는 4.1 판 경로 사용

## 확인하지 않은 것

- 답안·근거 문단의 사실 정확도에 대한 사람 검수
- 출처별 이용 조건 검수
- 관리자 API 등록, 검색 문단 생성, 실제 모델 판정
- `bash scripts/verify.sh backend|frontend|postgres`: 앱 코드 변경이 없어 실행하지 않음
- rubric·진단 사례 작성

## 남은 범위

- `API_DESIGN`, `SPRING_BOOT` 보강 문항
- 관측·성능, 캐시, 메시징, 시스템 설계의 나머지 개념: 커리큘럼 B·C 묶음
