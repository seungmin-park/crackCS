# 문서 지도

확인일: 2026-09-21

이 파일은 문서 내용을 반복하지 않는다. 독자가 가진 질문과 그 답을 소유한 기준 문서를 연결한다.

## 구조

```text
docs/
├── product/       제품 요구와 콘텐츠 정책
├── architecture/  현재 도메인·데이터 구조
├── adr/           선택한 결정과 이유
├── planning/      남은 작업과 구현 순서
├── changes/       완료 작업의 최종 검증 증거
├── evaluation/    버전화된 평가 정답·도구·결과
├── superpowers/   승인된 구조 변경 설계·실행 계획
└── retrospectives/ 로컬 회고, 커밋 제외
```

```text
현재 동작을 알고 싶음 → product / architecture / evaluation
왜 이렇게 정했나      → adr
다음에 무엇을 하나    → planning/tasks.md
완료를 어떻게 검증했나 → changes
```

## 현재 기준

| 답할 질문 | 상태 | 기준 문서 | 갱신 계기 |
|---|---|---|---|
| 프로젝트 실행·검증 방법 | 현재 안내 | [루트 README](../README.md) | 도구 버전·실행 명령·profile 변경 |
| 협업·설계·테스트 규칙 | 현재 기준 | [AGENTS.md](../AGENTS.md) | 저장소 공통 규칙 변경 |
| HTTP 요청·응답 계약 | 현재 계약 | [OpenAPI](../openapi.yml) | endpoint·상태 코드·schema 변경 |
| 프런트 구조·실행 | 현재 안내 | [front README](../front/README.md) | 프런트 구조·설정 변경 |
| 제품 범위·기능·인수 조건 | 현재 기준 | [제품 명세](product/spec.md) | 요구사항·출시 범위 변경 |
| 콘텐츠 검수·평가 정책 | 현재 기준 | [콘텐츠 정책](product/content-and-ai-policy.md) | 검수·평가·공개 정책 변경 |
| 도메인 관계·불변식·테이블 | 현재 기준 | [도메인·ERD](architecture/domain-model-and-erd.md) | 엔티티·관계·schema 변경 |
| 평가 정답의 구성·실행법 | v1 확정 | [평가 정답 기준](evaluation/reference-v1/README.md) | 원본·manifest·도구 계약 변경 |
| 평가 기준 버전·사람 검수·해시 | v1.0.0 확정 | [manifest](evaluation/reference-v1/manifest.json) | 원본 재검수 또는 새 버전 확정 |
| retrieval 기준과 현재 결과 | 측정 완료, 출시 성능 인증 아님 | [개선 전](evaluation/reference-v1/benchmarks/retrieval-baseline.json), [현재](evaluation/reference-v1/benchmarks/retrieval-improved.json) | 검색 정책·자료·DB 환경 변경 |
| 구현 순서와 의존성 | 계획 | [개발 계획](planning/plan.md) | 순서·의존성 변경 |
| 현재 남은 작업 | 진행 기준 | [작업 목록](planning/tasks.md) | 작업 시작·완료·검증 결과 변경 |
| 출시 이후 확장 후보 | 초안 | [확장 기능](planning/extension-features.md) | 후보 채택·보류·폐기 |

## 결정 기록

| 답할 질문 | 상태 | 기준 문서 | 갱신 계기 |
|---|---|---|---|
| 인증 상태 유지 방식 | 승인 | [ADR-0001](adr/0001-session-based-authentication.md) | 인증 방식 변경 |
| LOCAL 비밀번호 정책 | 승인 | [ADR-0002](adr/0002-password-policy.md) | 비밀번호 정책 변경 |
| 인증 보안 최소 기준 | 승인 | [ADR-0003](adr/0003-authentication-security-baseline.md) | 보안 기준 변경 |
| DB migration 도구 도입 시점 | 현재 보류 | [ADR-0004](adr/0004-defer-versioned-database-migrations.md) | persistent DB·배포 절차 확정 |
| 평가 실행·검색·worker 선택 | 현재 결정 | [ADR-0005](adr/0005-phase-5-evaluation-runtime.md) | DB·검색·provider·worker 기준 변경 |

결정이 바뀌면 기존 ADR을 조용히 덮어쓰지 않는다. 후속 ADR을 추가하고 대체 관계를 기록한다.

## 완료 증거

| 답할 질문 | 상태 | 기준 문서 | 갱신 계기 |
|---|---|---|---|
| UI 개편의 목표 | 당시 결정 | [UI 제안](changes/2026-09-06-ui/proposal.md) | 제안 해석 오류 정정 |
| UI·테마 구현 결과 | 완료 증거 | [UI 검증](changes/2026-09-06-ui/verification.md) | 같은 변경 범위 재검증 |
| Phase 4 답변·평가 골격 | 완료 증거 | [Phase 4 검증](changes/2026-09-07-phase-4/verification.md) | 답변·평가 계약 변경 |
| Phase 5 평가 실행 기반 | 구현 증거, 실제 모델 품질 미완료 | [Phase 5 검증](changes/2026-09-08-phase-5/verification.md) | 평가·검색·품질 결과 변경 |
| Phase 6 개인화·구조 정리 | 완료 증거 | [Phase 6 검증](changes/2026-09-13-phase-6/verification.md) | 상태·추천·구조·검증 변경 |
| 문서·평가 하네스 재구성 이유 | 승인된 설계 | [설계](superpowers/specs/2026-09-21-living-documentation-and-evaluation-harness-design.md) | 구조 결정 변경 |
| 문서·평가 하네스 재구성 절차 | 실행 계획 | [계획](superpowers/plans/2026-09-21-living-documentation-and-evaluation-harness.md) | 계획 오류 정정 |

`changes/`의 과거 테스트 수는 당시 증거다. 현재 통과 여부는 새 실행 결과로 판단한다.

## 로컬 전용 자료

- `docs/retrospectives/`: 결정 당시 질문·트레이드오프
- Git staging과 커밋에서 제외
- 현재 계약의 근거로 사용하지 않음
- 회고가 현재 기준과 충돌하면 현재 기준 문서 우선

## 관리 규칙

- 주제별 기준 문서 한 곳
- 다른 문서는 짧은 요약과 링크만 유지
- 코드·요구·결정 변경자가 관련 기준 문서도 같은 작업에서 갱신
- 생성물은 원본과 생성 명령이 있으면 `build/reports/`에 출력
- 완료 계획은 최종 검증에 고유 정보만 병합
- 단순 작업 일지는 Git 이력 사용
- 이동·삭제 시 이 목록, 상대 링크, Gradle·테스트 경로 함께 점검
- 완료 표시는 코드와 새 테스트 결과 확인 후 적용
- 미확정 항목은 질문과 다음 행동을 함께 기록

## 관리 근거

- [Codex AGENTS.md](https://developers.openai.com/codex/agent-configuration/agents-md): 계층별 지침과 제한된 프로젝트 컨텍스트
- [Claude Code memory](https://code.claude.com/docs/en/memory): 간결한 프로젝트 지침과 범위별 규칙
- [OpenAI Evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices): 사람 기준, 대표 데이터, 지속 평가
- [Google README 지침](https://google.github.io/styleguide/docguide/READMEs.html): 디렉터리 탐색을 위한 짧은 README
- [Diátaxis](https://diataxis.fr/): 독자 목적에 따른 문서 책임 분리
