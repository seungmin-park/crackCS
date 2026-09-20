# CrackCS 프런트

Vue 기반 학습자·관리자 화면.

- 실행·검증 명령: [루트 README](../README.md#로컬-실행)
- API 계약: [OpenAPI](../openapi.yml)
- UI 변경 근거·검증: [UI 변경 기록](../docs/changes/2026-09-06-ui/verification.md)

| 위치 | 책임 |
|---|---|
| `src/views/` | 페이지·관리자 화면 |
| `src/components/` | 공유 UI |
| `src/api/` | 학습자 API 호출과 공용 HTTP client |
| `src/api/admin/` | Topic·Concept·KnowledgeDocument·Question·Member·Evaluation별 관리자 API와 wire type |
| `src/presentation/` | 여러 화면이 공유하는 서버 상태의 표시 의미 |
| `src/styles/` | 공통 스타일·테마 |
| `src/router/` | 화면 경로·접근 제어 |
| `vite.config.ts` | 개발 서버·`/api` proxy·`@` 경로 별칭 |
| `package.json` | 실행·테스트·빌드 명령 원본 |

개발 요청 흐름: 브라우저 → Vite `/api` proxy → 백엔드 `localhost:8080`.

Phase 4 화면: 문제 상세의 답변 입력 → `/answers/:answerId` 평가 상태 → `/answers` 이력·페이지 이동.
미확정 제출: 회원·문제별 sessionStorage에 키와 원문 보존. 동일 탭 새로고침 후 재시도 가능, 로그아웃 시 제거.

Phase 7 화면: 일반 답변 평가 완료 → 같은 답변의 후속 질문 상태 조회 → READY 질문 인라인 제출 → 새 `/answers/:answerId` 평가 화면.
후속 질문은 공개 문제 상세 API가 아닌 답변 상세에서 직접 표시. 생성 조회는 2초 간격·최대 15회, 일시 오류는 최대 3회이며 화면 이탈과 답변 변경 시 기존 요청 결과와 타이머를 폐기.
