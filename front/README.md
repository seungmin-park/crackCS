# CrackCS 프런트

Vue 기반 학습자·관리자 화면.

- 실행·검증 명령: [루트 README](../README.md#로컬-실행)
- API 계약: [OpenAPI](../openapi.yml)
- UI 변경 근거·검증: [UI 변경 기록](../docs/changes/2026-09-06-ui/verification.md)

| 위치 | 책임 |
|---|---|
| `src/views/` | 페이지·관리자 화면 |
| `src/components/` | 공유 UI |
| `src/api/` | 서버 API 호출 |
| `src/styles/` | 공통 스타일·테마 |
| `src/router/` | 화면 경로·접근 제어 |
| `vite.config.ts` | 개발 서버·`/api` proxy·`@` 경로 별칭 |
| `package.json` | 실행·테스트·빌드 명령 원본 |

개발 요청 흐름: 브라우저 → Vite `/api` proxy → 백엔드 `localhost:8080`.
