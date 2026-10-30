# 목업 디자인의 실제 앱 적용

- 독자·질문: UI 검토자 / 승인한 목업을 실제 학습 흐름에 어떻게 연결했고 무엇을 검증했는가?
- 상태: 구현·로컬 검증 완료, 원격 CI·병합은 아래 전달 기록 참조
- 기준: 같은 작업에서 제작한 독립 목업. Pretendard·밝은 배경·올리브 강조·카드·좌측 학습 메뉴 적용
- 범위: 공개 소개, 학습 홈, 문제집, 답변 작성, 평가 결과, 지식 지도, 답변 이력. 로그인·가입·관리자에도 공통 폰트와 테마 적용

## 책임과 인수 기준

```text
공개 /about → /questions → 기존 인증 가드 → 로그인·가입
                                       ↓
학습 홈 → 실제 문제 → 기존 답변 제출 → 비동기 평가·근거
   ↑                                      ↓
주제별 상태 ← 지식 지도 ← 답변 이력 ← 저장된 답변
```

- `App.vue`: 반응형 학습 공간·모바일 메뉴·현재 위치·기존 테마·로그아웃
- `LandingView.vue`: 소개·실제 기능 설명·명시적인 평가 예시·FAQ, 화면 이탈 시 observer 해제
- View/API/composable: 기존 서버 조회·pagination·멱등 제출·polling·실패 복구 소유권 유지
- 대시보드 안정 개념: 서버의 Topic별 `stableCount` 합계. 목업의 가상 학습 수치 미사용
- `/`·권한 가드 유지. `/about`과 브랜드 404만 인증 복원 없이 열리는 공개 경로
- 폰트와 OFL 라이선스 자체 제공. 새 JS 패키지·API·DB 변경 없음
- CSS 책임: 공통·학습·인증 / 소개 / 관리자 분리

## 목업에서 실제 데이터로 바꾼 부분

- 일별 차트: 현재 API에 일별 집계가 없어 생략
- 문제 검색·주제 필터: 페이지 일부만 검색하는 오해를 피하기 위해 기존 서버 난이도 필터·pagination 유지
- 개념 노드: 가짜 문제 링크 대신 실제 숙련도·신뢰도·평가 횟수 표시
- 평가: 실제 점수·판정·문서 버전·근거·실패·검토 필요·후속 질문 유지
- 랜딩 예시: 실제 후기·성능·실적처럼 제시하지 않고 예시 표시
- 보조 글자: 기존 대비 7:1 기준에 맞춰 목업보다 진하게 조정. 입력 테두리 3:1 기준 유지

## TDD 증거

- 1차 RED: 기존 31개 통과, 신규 5개 실패. 모바일 메뉴 없음 2개, 공개 소개·404 경로 없음 2개, 안정 개념 집계 없음 1개
- GREEN: 동일 36개 통과
- Reveal RED: observer 호출이 없어 1개 실패. reduced motion의 기본 가시성 검사는 기존 정적 화면에서도 통과
- Reveal GREEN: observer 해제·reduced motion 2개 통과
- 브랜드 RED: 비로그인 브랜드 링크가 `/about` 대신 `/`여서 실패
- 브랜드 GREEN: 신규·기존 헤더 검사와 전체 프런트 검사에서 재확인
- 리뷰 RED: 모바일 메뉴를 연 채 desktop breakpoint로 전환하면 열린 상태가 남는 재현 테스트 실패
- 리뷰 GREEN: matchMedia 변화 시 메뉴·스크롤 잠금 해제, listener 정리. App 테스트 14개 통과
- 전체 검사 중 대비 실패: 6자리 색상·기존 selector를 읽는 검사와 실제 색상 기준 확인 후 운영 토큰 수정. 테스트 기준 완화 없음

## 실행 환경과 로컬 검증

- 기준 브랜치: `feat/mockup-ui`, 최신 `main`에서 분기
- Java: Temurin 21.0.13, `/private/tmp/crackcs-jdk21/Contents/Home`
- Node: 저장소 지정 24.20.0, 임시 npm cache 실행. 기본 시스템 Node 26은 검증 기준으로 사용하지 않음
- Gradle cache: `/private/tmp/crackcs-gradle`
- 공용 검사: `bash scripts/verify.sh all`, cmux `surface:1000100015`
- 문서: Python 61개 통과, 스택 19개 PASS
- 백엔드: 557개 통과, clean build 성공
- PostgreSQL: Docker Desktop·실제 PostgreSQL 17, 77개 통과
- 프런트: 공용 전체 실행 당시 350개, 브랜드·breakpoint 회귀 검사 추가 후 최종 352개 통과
- 보고서 검사: `python3 scripts/check_test_reports.py frontend`, `backend`, `postgres` 모두 PASS
- 추가 build: `npm run build` 타입 검사·production build 성공
- 최종 프런트·문서 재검증: `npm test -- --reporter=default --reporter=json --outputFile=test-results/vitest.json`, `npm run build`, frontend 보고서 검사, `bash scripts/verify.sh docs` 모두 성공. cmux 검증 탭 `UI_VERIFY_EXIT=0` 확인

## 실제 cmux 사용자 흐름

- 작업 공간: `workspace:1000100001`, 오른쪽 pane `pane:1000100006`
- 실제 Vite: `surface:1000100013`, `http://127.0.0.1:5173`, Node 24
- 실제 Spring: `surface:1000100014`, Java 21, `local`, 독립 `jdbc:h2:mem:crackcs-ui`
- 브라우저: `surface:1000100006`. 본 작업이 띄운 서버·DB를 대상으로 검증
- 계정: 이 검증에서 새로 만든 로컬 전용 학습자. 기존 사용자 계정·데이터 미사용
- 확인한 흐름:
  - 회원가입 입력 → 가입 처리 → 실제 로그인 → 학습 홈
  - 문제집 → 공개 운영체제 문제 → 서술형 답변 입력·제출
  - 평가 중 → 정답 100점·문서 버전·근거 표시. 기본 stub 평가, 유료 OpenAI 호출 없음
  - 지식 지도: 숙련도 100·신뢰도 25·평가 1회. 안정으로 과장하지 않고 학습 중 표시
  - 모바일 메뉴 → 답변 이력 → 방금 작성한 원문과 실제 판정 조회
  - 390px 메뉴 높이 844px·첫 메뉴 포커스·Escape 닫기 확인
  - 740×390 가로 화면: 첫 링크 상단 101px, 메뉴 scrollHeight > clientHeight 확인. 화면 확대 시 상태·스크롤 복구는 자동화 재현 테스트로 확인
  - 1440px·390px 가로 넘침 없음, 로컬 Pretendard 실제 로딩 1개 확인
  - 브라우저 오류 목록 비어 있음
- 화면 증거: [랜딩](assets/landing.png), [대시보드](assets/dashboard.png), [모바일 지식 지도](assets/knowledge-mobile.png), [실제 평가·근거](assets/evaluation.png)

## 한계·실패 경계

- 실제 모델 품질·유료 OpenAI·공개 운영 배포·전체 운영 콘텐츠 검수는 이번 검증 범위 밖
- 동시 요청·권한·상태 복구는 기존 자동화 테스트 결과, 모든 장애를 새 브라우저 세션에서 재주입한 것은 아님
- 문구 reveal은 IntersectionObserver 단위 관찰과 reduced motion 대응. 화면을 떠나면 해제
- 개인정보·이용 약관 등 공개 서비스 출시 정책은 이번 UI 변경으로 새로 확정하지 않음
- 기존 의존성의 npm audit 7건(중간 2·높음 4·심각 1) 관찰. 잠금 파일·의존성 변경 없는 UI 작업으로 별도 업그레이드 미수행

## 전달 기록

- 별도 읽기 전용 코드 리뷰: breakpoint 상태 잔류·짧은 화면 메뉴 지적 수정, 재검토에서 추가 결함 없음
- PR·원격 CI·실제 병합 결과: 연결 PR의 최신 상태 참조. 로컬 기록으로 원격 성공을 대신하지 않음
