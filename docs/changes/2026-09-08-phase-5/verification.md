# Phase 5 구현·검증

- 상태: 구현 기반 완료, 출시 품질 측정 대기
- 기준 결정: [ADR-0005](../../adr/0005-phase-5-evaluation-runtime.md)
- 진행 상태: [tasks.md](../../planning/tasks.md)
- 갱신 계기: 평가 계약·검색·모델·비용·검증 결과 변경

## 구현 흐름

```text
PUBLISHED KnowledgeDocument
        ↓ 문단 우선 1,000자 / 150자 overlap
KnowledgeChunk ── Topic·Concept + 키워드 점수 ──▶ 상위 5개 근거
                                                     ↓
Answer + Evaluation ── DB lease worker ──▶ Responses API
                                                     ↓
                        strict schema + Chunk ID 검증
                                                     ↓
             EVALUATED + Evidence / NEEDS_REVIEW / FAILED
```

## 확인 결과

- Chunk 동기 생성의 200 응답, 원문 offset·순서·유일 제약·재요청 재사용
- DRAFT·RETIRED 검색 제외, 결정적 점수·순서
- 필수 Concept·추가 필드·잘못된 타입·미제공 Chunk 거부
- 서버 verdict 점수 계산, 모델·평가기 버전·token·처리 시간 저장
- lease 만료 재수령, 최대 3회 지수 backoff, 소유 worker만 확정
- 월 token 비용 상한 도달 전 provider 호출 차단
- 학습자 Evidence 표시와 관리자 실패·검토 목록·상세
- 운영체제 독립 작성 골든 사례 60개와 품질 메트릭 계산기
- H2 기반 schema·query 자동 검증. PostgreSQL 실제 동작은 현재 검증 범위에서 제외

## 자동 검증

| 명령 | 결과 |
|---|---|
| `./gradlew test` | 233개 성공, 실패 0, 건너뜀 0 |
| `npm test -- --run` | 19개 파일, 108개 성공 |
| `npm run build` | type-check + production build 성공 |
| OpenAPI YAML·schema `$ref` 검사 | 35 paths, 44 schemas, 누락 0 |
| Markdown 로컬 링크 검사 | 누락 0 |
| `git diff --check` | 오류 0 |

PostgreSQL Testcontainers와 Flyway는 현재 개발 단계에서 제거. 최초 persistent staging 전에 schema·잠금·query 검증 재도입 필요.

## 남은 출시 검증

- 검색 정답 dataset의 Recall@K·무관 Chunk 비율 실측
- Terra·Luna 동일 골든 세트 비교
- 실제 모델 결과의 상세 85%, 이진 90%, false-correct 5% 이하 확인
- 300회 schema 99%, 최종 실패 1% 미만, p95 20초 이하 확인
- AC-002·003의 Knowledge State 반영: Phase 6 구현 뒤 확인
- AC-007 전체 버전 흐름: 실제 PostgreSQL 통합 시나리오 확인

미측정 항목은 `tasks.md`에서 미완료 상태 유지.
