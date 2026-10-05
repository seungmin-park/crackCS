# 문서 지도

확인일: 2026-10-05

이 파일은 문서 내용을 반복하지 않는다. 독자가 가진 질문과 그 답을 소유한 기준 문서를 연결한다.

## 구조

```text
docs/
├── product/       제품 요구와 콘텐츠 정책
├── architecture/  현재 도메인·데이터 구조
├── adr/           선택한 결정과 이유
├── planning/      남은 작업과 구현 순서
├── changes/       완료 작업의 최종 검증 증거
├── content/      초기 학습 콘텐츠·출처·검수 상태
├── evaluation/    버전화된 평가 정답·도구·결과
├── engineering/   프로젝트 검증 경로·현재 스택 공식 문서
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
| 초기 학습 콘텐츠·출처·등록 | DRAFT 등록 템플릿, OS·Java 로컬 승인·나머지3개 Topic 검수 대기 | [initial-v1](content/initial-v1/README.md), [원본](content/initial-v1/bundle.json), [검수용 읽기 사본](content/initial-v1/review.md) | 콘텐츠·출처·검수 상태 변경 |
| 평가 정답의 구성·실행법 | v1 확정 | [평가 정답 기준](evaluation/reference-v1/README.md) | 원본·manifest·도구 계약 변경 |
| OS 실제 답변 독립 검수 방법·도구 | 준비 완료·사용자 위임 대리 진단, 독립 대표 표본 미완료 | [독립 검수 안내](evaluation/independent-review/README.md), [실행·진단](changes/2026-10-05-learner-review/verification.md) | 표본 출처·사람 판정·동결·비교 계약 변경 |
| provider 장애 때 원문·화면·재시도·지식 반영 | 통제 timeout·429·503의 cmux 9시나리오 완료, 실제 외부 장애 별도 | [검증·실행 결과](changes/2026-10-05-provider-fault-flow/verification.md) | 장애 주입·재시도·멱등·화면·저장 계약 변경 |
| 평가 기준 버전·사람 검수·해시 | v1.0.0 확정 | [manifest](evaluation/reference-v1/manifest.json) | 원본 재검수 또는 새 버전 확정 |
| retrieval 측정 이력과 현재 재측정 | 기존 측정·충돌 수정 후 재측정, 출시 성능 인증 아님 | [개선 전](evaluation/reference-v1/benchmarks/retrieval-baseline.json), [현재](evaluation/reference-v1/benchmarks/retrieval-improved.json), [현재 재측정](changes/2026-10-05-evidence-conflict/verification.md) | 검색 정책·자료·DB 환경 변경 |
| 구현 순서와 의존성 | 계획 | [개발 계획](planning/plan.md) | 순서·의존성 변경 |
| 현재 남은 작업 | 진행 기준 | [작업 목록](planning/tasks.md) | 작업 시작·완료·검증 결과 변경 |
| 에이전트 작업·검증 경로 | 현재 기준 | [기능 지도·검증 경계](engineering/agent-workflow.md), [verify-crackcs](../.agents/skills/verify-crackcs/SKILL.md) | 사용자 경로·검증 명령·CI 변경 |
| 사용 중인 기술 스택의 공식 문서 | 2026-09-29 확인 | [문서 안내](engineering/stack-docs.md), [버전 목록](engineering/stack-docs.json) | build/lockfile·BOM·공식 문서 버전 변경 |
| CI·자동 PR·머지 검증 | 구현·로컬 검증 완료, 원격 결과는 연결 PR | [검증 기록](changes/2026-10-05-ci-delivery/verification.md) | 검증 job·필수 테스트·main 보호·전달 절차 변경 |
| 기술 스택 문서 동기화 검증 | 2026-09-29 실행 | [검증 기록](changes/2026-09-29-agent-engineering/verification.md) | 검사 경로·테스트·CI 변경 |
| AI 연결 전 서버·로컬 실행 검증 | 당시 구현·로컬 검증·독립 검토 완료 | [계획](changes/2026-09-27-service-completion/plan.md), [검증](changes/2026-09-27-service-completion/verification.md) | 보안·다중 앱·실행 근거 변경 |
| 화면 오류 복구·초기 콘텐츠 준비 | 구현·초안 등록 완료, 사람 검수 대기 | [계획](changes/2026-09-27-ui-content/plan.md), [검증](changes/2026-09-27-ui-content/verification.md) | 화면 상태·콘텐츠 준비 범위 변경 |
| 로컬 PostgreSQL 실행·복구 | 기존 검증 이력 | [실행 안내](changes/2026-09-27-release-readiness/local-runbook.md), [당시 검증](changes/2026-09-27-release-readiness/verification.md) | DB·복구·기동 경로 변경 |
| 운영체제 콘텐츠 검수·공개·버전 보존 | OS 수정·사람 승인·로컬 공개·실제 GPT·AC-007 완료 | [검증·승인 기록](changes/2026-10-05-os-content/verification.md) | OS 콘텐츠·승인·실행 결과 변경 |
| Java 사용자 검수·로컬 공개·학습 상태 | 5문항·10개 개념·문서1개 공개, cmux46개 PASS·합성 provider | [검수·검증](changes/2026-10-05-java-content-flow/verification.md), [승인 기록](changes/2026-10-05-java-content-flow/review-record.json) | Java 검수·공개·실행 결과 변경 |
| 공개 운영체제 정답·부분 정답·오답 판정 | 합성 진단15/15·후속·지식 상태·cmux 검증 완료, 독립 대표 품질 별도 | [검증](changes/2026-10-05-os-verdict-flow/verification.md), [사례](changes/2026-10-05-os-verdict-flow/cases.json), [결과](changes/2026-10-05-os-verdict-flow/runtime-results.json) | 판정 사례·모델 결과·학습 상태 검증 변경 |
| 실제 GPT 연결·로컬 시연 | 로컬 시연 범위 완료, 공개 운영 별도 | [계획](changes/2026-09-29-openai-completion/plan.md), [검증](changes/2026-09-29-openai-completion/verification.md), [영상](changes/2026-09-29-openai-completion/demo.mp4) | 모델·비용 한도·품질 지표·시연 경로 변경 |
| 도메인 네이밍·메서드 책임 개선 | 변경 범위 검증 완료 | [fix.md](../fix.md) | 관련 코드·상시 점검 기준 변경 |
| Phase 8 운영 안정화 범위·실행 순서 | 부분 완료 | [설계](changes/2026-09-21-phase-8/design.md), [구현 계획](changes/2026-09-21-phase-8/implementation-plan.md), [검증](changes/2026-09-21-phase-8/verification.md) | Phase 8 범위·Gate·책임·순서·증거 변경 |
| 운영 경계·평가 처리 책임 개선 | 구현 완료 | [설계](changes/2026-09-22-operability/design.md), [구현 계획](changes/2026-09-22-operability/implementation-plan.md), [검증](changes/2026-09-22-operability/verification.md) | Actuator·상관 ID·평가 관측·처리 책임 변경 |
| 출시 이후 확장 후보 | 초안 | [확장 기능](planning/extension-features.md) | 후보 채택·보류·폐기 |

## 결정 기록

| 답할 질문 | 상태 | 기준 문서 | 갱신 계기 |
|---|---|---|---|
| 인증 상태 유지 방식 | 승인 | [ADR-0001](adr/0001-session-based-authentication.md) | 인증 방식 변경 |
| Security Cloud 발견 항목 개선·검증 | 세션 회수·인증 요청 제한·가입 응답 숨김 로컬 검증 완료, provider 제한 조사 | [검증 기록](changes/2026-10-06-security-hardening/verification.md) | 보안 계약·회귀·PR 결과 변경 |
| LOCAL 비밀번호 정책 | 승인 | [ADR-0002](adr/0002-password-policy.md) | 비밀번호 정책 변경 |
| 인증 보안 최소 기준 | 승인 | [ADR-0003](adr/0003-authentication-security-baseline.md) | 보안 기준 변경 |
| DB migration 도구 도입 시점 | 도구 보류 유지, 로컬 절차 ADR-0006으로 대체 | [ADR-0004](adr/0004-defer-versioned-database-migrations.md) | persistent DB·배포 절차 확정 |
| 로컬 PostgreSQL schema·변경·복구 | 승인, 로컬 범위 | [ADR-0006](adr/0006-local-postgres-schema.md) | schema·복구 정책 변경 |
| 평가 실행·검색·worker 선택 | 현재 결정 | [ADR-0005](adr/0005-phase-5-evaluation-runtime.md) | DB·검색·provider·worker 기준 변경 |

결정 변경 시 후속 ADR과 대체 관계 기록. 기존 결정 이력 보존.

ADR 작성 기준: 독자가 판단할 결정 하나, 대안과 선택 이유, 비용·한계, 검증·재검토 조건 포함.

```text
# ADR-NNNN: 결정 제목
- 상태: 제안 / 승인 / 대체
- 결정일:
- 관련·대체 문서:
## 문제와 제약
## 검토한 대안과 선택 이유
## 결정
## 결과·한계·재검토 조건
## 검증 근거
```

## 완료 증거

| 답할 질문 | 상태 | 기준 문서 | 갱신 계기 |
|---|---|---|---|
| 빈 환경 PostgreSQL 실행·복구 | 로컬 실행 기준 | [로컬 실행·복구](changes/2026-09-27-release-readiness/local-runbook.md) | 실행 명령·schema·profile 변경 |
| 이전 실행·정합성 검증 | 당시 로컬 모델 품질·지연 미달, 현행 GPT 결과는 별도 | [당시 실행 검증](changes/2026-09-27-release-readiness/verification.md), [현행 GPT 검증](changes/2026-09-29-openai-completion/verification.md) | 실행 환경·버전·회귀 범위 변경 |
| UI 개편의 목표 | 당시 결정 | [UI 제안](changes/2026-09-06-ui/proposal.md) | 제안 해석 오류 정정 |
| UI·테마 구현 결과 | 완료 증거 | [UI 검증](changes/2026-09-06-ui/verification.md) | 같은 변경 범위 재검증 |
| 근거 충돌 오탐·문장 단위 검사 | 구현·전체 검증·실제 GPT 후속 답변 완료 | [검증 기록](changes/2026-10-05-evidence-conflict/verification.md) | 충돌 정책·검색 회귀·실제 학습 흐름 변경 |
| 테마 메뉴·포커스 표시 | 구현·로컬 검증 완료 | [검증·화면](changes/2026-10-05-theme-menu/verification.md) | 테마 컨트롤·키보드·배치 변경 |
| Phase 4 답변·평가 골격 | 완료 증거 | [Phase 4 검증](changes/2026-09-07-phase-4/verification.md) | 답변·평가 계약 변경 |
| Phase 5 평가 실행 기반 | 당시 구현 증거, 현행 GPT 품질은 별도 측정 | [Phase 5 검증](changes/2026-09-08-phase-5/verification.md), [현행 GPT 검증](changes/2026-09-29-openai-completion/verification.md) | 평가·검색·품질 결과 변경 |
| 이전 로컬 모델 실측 | 보존된 비교 자료, 현재 실행 경로 아님 | [Ollama 후보 검증](changes/2026-09-26-local-ollama/verification.md), [당시 후보 표본](changes/2026-09-27-release-readiness/verification.md) | 모델 대안 검토·동일 사례 비교 |
| Phase 6 개인화·구조 정리 | 완료 증거 | [Phase 6 검증](changes/2026-09-13-phase-6/verification.md) | 상태·추천·구조·검증 변경 |
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
