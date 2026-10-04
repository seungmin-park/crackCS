# 테마 메뉴와 포커스 표시 수정

- 독자·질문: 사용자·UI 작업자 / 테마 메뉴와 선택 후 테두리가 어떻게 개선됐는가?
- 상태: 구현·현재 브라우저·공용 검증 완료. PR 전달 결과는 GitHub 기록으로 확인
- 범위: 헤더의 화면 테마 컨트롤. 화이트·다크·시스템 선택과 기존 저장·시스템 연동 유지
- 기준: main `1869a8c`, 브랜치 `fix/theme-switch`
- 작업 위치: `/Users/seungmin/.codex/worktrees/theme-switch/crackCS`
- 표시: 이번 사용자 요청에 따라 cmux 대신 현재 Codex 브라우저 `http://127.0.0.1:5174/admin`

## 원인과 변경

- 기존 구조: 바깥 테두리와 내부 native select의 전역 3px·offset 4px 초록색 포커스가 겹침. 선택 영역과 바깥 컨트롤 크기 불일치
- 새 UI: 104px × 36px 버튼, 172px 메뉴, 동일한 40px 선택 행 3개, 아이콘·선택 체크
- 클릭: 별도 포커스 테두리 없음. 키보드: 중립색 2px 테두리·offset -2px, 컨트롤 안쪽 표시
- 작은 화면: 헤더 줄바꿈 시 메뉴를 왼쪽 기준으로 정렬해 화면 잘림 방지
- 기존 theme token·글꼴 사용. 의존성·전역 포커스 규칙 변경 없음

```text
ThemeSwitch: 선택값·메뉴 열림 상태 소유
  ├─ 선택 → document의 data-theme → 기존 CSS token 적용
  ├─ 선택 → localStorage (차단되면 현재 화면 적용 유지)
  └─ 시스템 선택 → 기존 matchMedia 변경 이벤트 반영

방향키·Home·End → 위치 이동 / Enter·Space → 선택
Escape·바깥 pointerdown·Tab → 닫기
Escape·선택 → 버튼 포커스 복원 / 바깥 클릭·Tab → 이동 위치 유지
```

## RED → GREEN

- 새 메뉴 동작 12개: 기존 native select에서 실패, exit 1. 새 버튼·선택 메뉴가 없어 요구사항 불충족
- 최소 구현 뒤 12/12 성공, exit 0. `/private/tmp/theme-switch-red.log`, `/private/tmp/theme-switch-green.log`
- 기존 App 테마·헤더 테스트를 새 선택 UI 조작으로 연결. 로그아웃 테스트는 주요 메뉴 버튼 명시
- 화면 RED: 화이트 101.890625px·다크 89.921875px로 폭 변화. 고정 폭 적용 뒤 두 테마 104px, GREEN
- 화면 RED: 최소 화면에서 메뉴 left -48px로 잘림. 작은 화면 정렬 보완 뒤 left 20px, GREEN

## 실제 브라우저 검증

| 흐름 | 결과 |
|---|---|
| 관리자 로그인·메뉴 열기 | 화이트·다크 각각 현재 테마·선택 체크 확인 |
| 클릭 선택 | 테마 적용·메뉴 닫힘. focus-visible false, outline-style none |
| 크기 | 두 테마 버튼 104 × 36px, 메뉴 172px, 세 행 40px |
| 새로고침 | 선택한 다크·버튼 표시 유지 |
| 방향키·Enter | 위치 이동 후 화이트 선택·메뉴 닫힘·버튼 포커스 복원 |
| 키보드 포커스 | 2px solid·offset -2px. 다크 RGB(191,203,191), 선택 행 안쪽 |
| Escape | 테마 유지·메뉴 닫힘·버튼 포커스 복원 |
| Tab | 메뉴 닫힘, 다음 관리 링크로 포커스 이동 |
| 바깥 클릭 | 메뉴 닫힘, 누른 링크에 포커스 유지 |
| 너비 280 / client 265 | 메뉴 경계 20~192px, 화면 안쪽 |
| 너비 390 / client 375 | 메뉴 경계 168.734375~340.734375px, 화면 안쪽 |

- 임시 viewport는 기본 크기로 복원. 사용자의 원래 화이트 선택 복원
- 화면: [화이트](assets/white-menu.jpg), [다크](assets/dark-menu.jpg), [작은 화면](assets/mobile-menu.jpg)
- 검증용 백엔드: 18084·관리 포트 18085, local PostgreSQL. GPT 호출 비활성
- Vite: 이 브랜치의 front 코드 사용. OS 콘텐츠 검증 브랜치 변경 제외

## 공용 검증·전달

- 최초 공용 검증: Python 31·backend 516·frontend 338·PostgreSQL 65개, 스택 문서 19항목·타입·빌드 성공, exit 0
- 최종 CSS 보완 후 `bash scripts/verify.sh all`: Python 31·backend 516·frontend 338·PostgreSQL 65개 모두 성공, 총 950개. 타입·빌드·스택 문서 19항목 성공, exit 0. `.firecrawl/theme/verify-all.log`
- 현재 main 보호: PR·최신 main 요구, Actions(app 15368)의 CrackCS verify 필수, 관리자 우회·force push·삭제 금지
- 전달 결과 조회: `gh pr list --head fix/theme-switch --state all --json number,url,state,mergeCommit`. 실제 MERGED와 merge SHA의 main CI 성공을 완료 조건으로 사용

## 검증 경계

- 실제 화면·입력: 현재 Codex in-app browser. Safari·Firefox·모바일 실기기 미실행
- 시스템 변경·저장 차단: 기존 App 단위 테스트로 검증. 실제 OS 설정 변경 없음
- 모델 품질·OS 콘텐츠 3·4단계: 이번 테마 변경 범위 제외. 별도 키 연결 대기 작업 유지
