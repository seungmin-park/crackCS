# CrackCS UI and Themes Implementation Plan

**Goal:** 승인된 문제집 디자인을 현재 화면에 적용하고 화이트·다크 양쪽의 읽기와 조작 대비를 보장한다.
**Architecture:** 테마 선택은 전용 컴포넌트가 소유하고 문서 루트의 data-theme으로 CSS 의미 색상을 선택한다. 질문 목록은 서버 페이지 정보를 사용한다. 기존 인증과 콘텐츠 도메인을 유지한다.
**Tech Stack:** Vue 3, TypeScript, SCSS, Vitest.
**Spec:** [UI 개편 제안](proposal.md) + 사용자 화이트/다크 가독성 요구.

## Scope

- 현재 체크아웃에서 관련 없는 사용자 변경을 보존한다.
- 답변 평가 API는 이번 UI 교체에 포함하지 않는다. 존재하지 않는 제출·점수·학습 기록을 노출하지 않는다.
- 본문·보조 문구 7:1, 상태·버튼 글자 4.5:1, 입력 경계·포커스 3:1을 색상 검증 기준으로 사용한다.

## Tasks

- [x] App.test.ts: 테마 선택, 저장·복구, 시스템 변경, 저장 차단에서도 전환되는 행동 RED → ThemeSwitch.vue 구현 → GREEN.
- [x] tests/contrast.test.ts: 두 테마의 실제 CSS 색상 조합 명암 대비 RED → 의미 색상 구성 → GREEN.
- [x] QuestionListView.test.ts + api/questions.test.ts: 전체 건수, 다음 페이지, 난이도 변경과 첫 페이지 복귀, 오래된 응답 무시 RED → API 옵션과 목록 구현 → GREEN.
- [x] 공통 헤더, 행형 문제집, 집중형 문제 상세, 인증·관리자 스타일 개편. 지원하지 않는 기능 문구 제거. 모바일, 긴 제목, 오류 상태를 함께 처리.
- [x] npm run test, npm run type-check, npm run build-only 및 가능한 백엔드 전체 테스트.
- [x] 실제 브라우저에서 두 테마·모바일·폼·관리자 확인, 스크린샷 저장. 변경과 검증 수, 경계 보고.

## Ownership

ThemeSwitch: 사용자 테마 선택과 OS 변경 구독/해제. public/theme-init.js: 최초 페인트 전 설정 복원. CSS: 의미별 색상과 배치. QuestionListView: 현재 요청과 페이지·필터. questions.ts: URL 생성. 서버: 조회 권한과 공개 여부.

검증 결과: ../../ui-redesign-verification-2026-09-06.md
