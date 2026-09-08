# ADR-0005: Phase 5 평가 실행 기반

- 상태: 승인
- 결정일: 2026-09-08
- 범위: 운영체제 평가, 검색, 운영 DB 후보, AI adapter, 작업 실행, 비용 상한

## 상황

- 첫 평가 범위: 운영체제
- 평가 입력: 공개·검수된 KnowledgeDocument와 독립 작성 골든 사례
- 목표: 근거 추적 가능한 서술형 판정
- 제약: 한 서버에서 시작, 외부 AI 장애와 재시작에도 Answer 보존

## 결정

```text
PostgreSQL 목표·현재 H2 검증
       ↓ Topic·Concept 필터
키워드 기준선 ── 품질 미달 시 ──▶ pgvector 비교
       ↓ 근거 Chunk
DB lease worker → OpenAI Responses API / Terra
       ↓
strict schema 검증 → Evidence 포함 결과 확정
```

### 검색과 DB

- 운영 DB 목표: PostgreSQL 17
- 현재 schema 변경: local `update`, test `create-drop`, 기본 profile `validate`
- version migration과 PostgreSQL 통합 테스트: 최초 persistent staging 전에 재도입 검토
- 초기 검색: 관계형 Topic 필터 + 애플리케이션 키워드 점수
- embedding 실험 조건: Recall@K 85% 미만 또는 무관 Chunk 비율 20% 초과
- pgvector: 조건 충족 전 미도입
- 기존 문서의 Chunk: 분할 정책 변경 후에도 재생성하지 않음. 새 문서 버전에 새 정책 적용

PostgreSQL 선택 이유:

- 향후 pgvector를 같은 transaction·백업·운영 경계에 배치 가능
- 향후 Testcontainers로 실제 query와 schema 변경 절차 검증 가능
- 전문 검색과 JSON 기능을 별도 검색 서버 없이 확장 가능

MySQL과의 트레이드오프:

| 기준 | PostgreSQL | MySQL |
|---|---|---|
| 현재 키워드 기준선 | 충분 | 충분 |
| 벡터 확장 | pgvector 생태계와 SQL 결합 용이 | 별도 검색 계층 또는 제공 기능 검토 필요 |
| 운영 친숙도 | 팀 경험이 적으면 학습 비용 | 기존 MySQL 운영 경험이 있으면 초기 비용 절감 |
| 이식성 | PostgreSQL 전용 migration·query 발생 | MySQL 전용 문법과 인덱스 선택 발생 |

선택의 대가: 현재 H2 자동화 테스트는 PostgreSQL의 SQL, 잠금, 인덱스와 동시성을 보장하지 않음. 최초 persistent staging 전 PostgreSQL 통합 검증과 schema 배포 절차 결정 필요.

### AI 모델과 fallback

- API: OpenAI Responses API
- 기본 모델: `gpt-5.6-terra`
- 비교 후보: `gpt-5.6-luna`
- 자동 fallback: 없음
- 모델명과 평가기 버전: Evaluation에 저장

Terra 선택 이유:

- 한국어 서술형의 누락·오개념 구분이 필요한 초기 품질 우선 구간
- 공식 포지션상 지능과 비용의 균형 모델
- Luna 전환 여부를 동일 골든 세트로 비교 가능

자동 fallback을 두지 않는 이유:

- 모델이 바뀌면 동일 답변의 판정 기준도 달라질 수 있음
- 장애 시 싼 모델 결과를 조용히 저장하면 재현성과 원인 분석 저하
- 실패는 재시도 후 FAILED, 근거 부족·충돌·예산 초과는 NEEDS_REVIEW로 명시

### 데이터와 보안

- API 요청 저장: `store=false`
- provider 입력: 질문·모범 답안·사용자 답변·선택된 근거만
- prompt: 지시와 사용자 데이터를 별도 메시지/데이터 구획으로 분리
- 일반 로그: API key, 답변 원문, provider 응답 본문 제외
- Evidence: 실제 전달 Chunk ID의 부분집합만 허용

필요한 데이터만 보내는 이유: 평가 정확도에 불필요한 회원 정보와 전체 문서가 유출 범위·token 비용·prompt 공격 면적만 확대.

### 작업 실행

- 방식: DB 상태 + lease 기반 단일 서버 worker
- 작업 원본: Answer와 Evaluation row
- claim: pessimistic lock, lease owner·만료 시각·시도 횟수 저장
- 재시도: 최대 3회, 지수 backoff
- 재시작: 만료된 PROCESSING lease 재수령

DB lease 선택 이유:

- Answer commit 뒤 Evaluation row가 작업 자체이므로 별도 in-memory queue 유실 방지
- 현재 한 서버 규모에서 메시지 broker 운영 비용 불필요
- 중복 실행 시 lease 소유자만 결과 확정 가능

한계: 다중 서버 처리량과 긴 작업 가시성은 제한적. worker 수평 확장이나 대량 적체 발생 시 broker와 outbox 비교 필요.

### 품질과 비용 기준

- 상세 판정 일치율: 85% 이상
- 정답/비정답 이진 일치율: 90% 이상
- false-correct: 5% 이하
- Evidence ID 유효성: 100%
- 근거 적합률: 95% 이상
- schema 준수: 300회 중 99% 이상
- 최종 실패율: 1% 미만
- p95 처리 시간: 20초 이하
- 개발 월 상한: $30
- 비공개 파일럿 월 상한: $50

비용 계산:

```text
월 추정 비용 = 입력 token / 1,000,000 × 입력 단가
             + 출력 token / 1,000,000 × 출력 단가
```

- 2026-09-08 Terra 표준 단가: 입력 $2/1M, 출력 $12/1M
- 평가 1건을 입력 3,000 + 출력 500 token으로 가정: 약 $0.012
- $30: 약 2,500건, $50: 약 4,166건
- 실제 제어: 저장된 월 input/output token 합계와 설정 단가로 계산, 상한 도달 시 provider 호출 전 NEEDS_REVIEW
- 단가 변경 대응: 환경 설정 갱신. 코드 재배포 없이 반영

월 상한 근거: 초기 품질 측정과 소규모 파일럿에 충분한 수천 건 확보, 비정상 반복 호출의 손실 제한. 실제 평균 token과 재시도율 확인 후 조정.

## 검증과 다음 행동

- 현재 자동 검증: chunk 멱등성, 검색 필터·점수, strict schema, lease 재수령, retry, Evidence 연결, 비용 계산
- 환경 의존 검증: 현재 보류. 최초 persistent staging 전 PostgreSQL schema·잠금·query 검증 재도입
- 출시 전 필수: Terra와 Luna의 동일 60개 골든 세트 실측, 300회 schema·지연 측정
- 가격 확인: [OpenAI 모델 문서](https://developers.openai.com/api/docs/models/gpt-5.6-terra)
