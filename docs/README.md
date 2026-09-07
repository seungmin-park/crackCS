# 문서 지도

확인일: 2026-09-06. 존재 여부는 파일 기준. 문서 존재·계획 수립은 구현 완료의 근거가 아님.

## 찾는 질문

- 무엇을 만들었나? → 제품 명세
- 어떤 순서로, 어디까지 진행했나? → 계획·작업 목록
- 구조와 계약은? → 도메인 모델·OpenAPI
- 왜 이 선택인가? → ADR·변경 기록·회고


## 관리 기준

- 기준 정보: 한 문서가 소유. 다른 문서는 요약·링크
- 유지 책임: 해당 요구·코드·결정을 변경한 작업자
- 생성 기준: 독자·질문·갱신 계기가 명확하고 기존 문서로 충분하지 않을 때
- 폴더 기준: 목적별 분류. 개별 변경의 계획·검증·시안은 함께 배치
- 미작성 문서: 필요가 확정된 경우만 `작성 예정`과 예정 경로 기록. 빈 파일 생성 금지
- 이동·통합·삭제: 같은 작업에서 목록·링크·도구 참조 점검
- 기록 보존: 현재 기준과 분리. 과거 테스트 결과는 현재 통과의 증거가 아님
- 점검: 파일 존재·목록 누락·상대 링크·이전 경로 잔존 확인

## 문서 목록

| 문서의 책임 | 존재 여부 | 상태 | 위치 | 갱신 계기 |
|---|---|---|---|---|
| 프로젝트 실행·검증 입구 | 있음 | 현재 안내 | [README.md](../README.md) | 실행 환경·명령 변경 |
| 협업·설계·테스트·문서 규칙 | 있음 | 현재 기준 | [AGENTS.md](../AGENTS.md) | 작업 규칙 변경 |
| HTTP 요청·응답 계약 | 있음 | 현재 계약 | [openapi.yml](../openapi.yml) | API 계약 변경 |
| 프런트 구조·설정 위치 | 있음 | 현재 안내 | [front/README.md](../front/README.md) | 프런트 구조·설정 변경 |
| 문서 탐색·책임·위치 | 있음 | 현재 안내 | [docs/README.md](README.md) | 문서 생성·이동·통합·삭제 |
| 제품 범위·요구사항·인수 조건 | 있음 | 목표 명세 | [docs/product/spec.md](product/spec.md) | 제품 요구 변경 |
| 콘텐츠 검수·평가 정책 | 있음 | 정책·일부 도입 계획 | [docs/product/content-and-ai-policy.md](product/content-and-ai-policy.md) | 검수·평가 정책 변경 |
| 도메인 관계·불변식·테이블 | 있음 | 설계·구현 여부는 tasks 참조 | [docs/architecture/domain-model-and-erd.md](architecture/domain-model-and-erd.md) | 도메인·schema 변경 |
| 개발 순서·의존성·실패 신호 | 있음 | 개발 계획 | [docs/planning/plan.md](planning/plan.md) | 순서·의존성 변경 |
| 구현·검증·품질 개선 진행 | 있음 | 작업 현황 | [docs/planning/tasks.md](planning/tasks.md) | 작업 시작·완료·검증 결과 |
| 출시 이후 확장 후보 | 있음 | 초안 | [docs/planning/extension-features.md](planning/extension-features.md) | 확장 채택·보류·폐기 |
| Phase 4 결정·실행·검증 | 있음 | 구현·코드 리뷰 개선 완료·Gate 통과 | [docs/changes/2026-09-07-phase-4/verification.md](changes/2026-09-07-phase-4/verification.md) | 답변·평가 계약 또는 검증 변경 |
| UI 개편 선택지·제품 분석 | 있음 | 제안 당시 기록 | [docs/changes/2026-09-06-ui/proposal.md](changes/2026-09-06-ui/proposal.md) | 제안 정정·후속 결정 연결 |
| UI·테마 구현 범위 | 있음 | 작업 계획 기록 | [docs/changes/2026-09-06-ui/plan.md](changes/2026-09-06-ui/plan.md) | 후속 검증 연결 |
| UI 변경·검증 증거 | 있음 | 검증 당시 기록 | [docs/changes/2026-09-06-ui/verification.md](changes/2026-09-06-ui/verification.md) | 검증 정정·후속 검증 추가 |
| 시안 재현 프롬프트 | 있음 | 시안 생성 기록 | [docs/changes/2026-09-06-ui/assets/prompts.md](changes/2026-09-06-ui/assets/prompts.md) | 시안 변경 |
| 품질 진단 근거 | 있음 | 진단 당시 기록 | [docs/reviews/2026-09-03-code-quality.md](reviews/2026-09-03-code-quality.md) | 오류 정정; 진행은 tasks |
| 모델 후보·비용 비교 | 있음 | 재검증 필요 | [docs/reviews/ai-model-selection.md](reviews/ai-model-selection.md) | AI 구현·모델 교체 전 |
| 초기 제품 범위·학습 흐름 | 있음 | 대체됨: product/spec.md | [docs/archive/functional-specification.md](archive/functional-specification.md) | 현재 요구 수정 금지; 이력 오류만 정정 |
| Phase 1 선택 이유·학습 | 있음 | 로컬 전용·커밋 제외 | `docs/retrospectives/phase-1-decisions-and-insights.md` | 사실 정정·현재 기준 링크 변경 |
| Phase 2 선택 이유·학습 | 있음 | 로컬 전용·커밋 제외 | `docs/retrospectives/phase-2-decisions-and-insights.md` | 사실 정정·현재 기준 링크 변경 |
| Phase 3 선택 이유·학습 | 있음 | 로컬 전용·커밋 제외 | `docs/retrospectives/phase-3-decisions-and-insights.md` | 사실 정정·현재 기준 링크 변경 |
| ADR-0001: 동일 출처 웹의 서버 세션 인증 | 있음 | 승인된 결정 이력 | [docs/adr/0001-session-based-authentication.md](adr/0001-session-based-authentication.md) | 결정 변경 시 후속 ADR·대체 관계 기록 |
| ADR-0002: LOCAL 계정 비밀번호 정책 | 있음 | 승인된 결정 이력 | [docs/adr/0002-password-policy.md](adr/0002-password-policy.md) | 결정 변경 시 후속 ADR·대체 관계 기록 |
| ADR-0003: Phase 2 인증 보안 최소 기준 | 있음 | 승인된 결정 이력 | [docs/adr/0003-authentication-security-baseline.md](adr/0003-authentication-security-baseline.md) | 결정 변경 시 후속 ADR·대체 관계 기록 |
| ADR-0004: 버전 기반 DB migration 도구 도입 보류 | 있음 | 승인된 결정 이력 | [docs/adr/0004-defer-versioned-database-migrations.md](adr/0004-defer-versioned-database-migrations.md) | 결정 변경 시 후속 ADR·대체 관계 기록 |

회고 경로: 로컬 파일 위치만 기록. 저장소 체크아웃에는 미포함.

## 이미지 자료

- [Phase 4 평가 화면](changes/2026-09-07-phase-4/assets/): 데스크톱·모바일 검증 증거
- [ERD 이미지](architecture/images/crackcs-erd-illustrated.png): 도메인 문서 부속 자료. 모델 변경 시 함께 확인
- [UI 시안·검증 화면](changes/2026-09-06-ui/assets/): 해당 UI 변경 기록 부속 자료

## 통합·제거 이력

| 이전 문서 | 현재 처리 | 이유 |
|---|---|---|
| 루트 품질 개선 체크리스트 | 작업 목록의 ‘품질 개선 작업’에 통합 | 진행 상태의 소유 지점 통일. 체크 상태 보존 |
| Spring 생성 도움말 | 제거 | 프로젝트 고유 정보 없이 외부 가이드 링크만 포함 |
| 필수 기능 초안 | archive로 이동, 현재 제품 명세 연결 | 초기 맥락 보존; 중복 요구사항 갱신 중단 |
| 프런트 생성 README | 프런트 고유 구조 안내로 교체 | 공통 실행 명령 중복 제거 |

## 기준 근거

- [Google 문서 관리](https://google.github.io/styleguide/docguide/best_practices.html): 짧고 정확한 문서·코드와 함께 갱신·중복 제거
- [Diátaxis](https://diataxis.fr/): 독자 목적에 따른 문서 책임 구분
- [Google Cloud ADR](https://docs.cloud.google.com/architecture/architecture-decision-records): 선택지·결정 이유·이력 보존
