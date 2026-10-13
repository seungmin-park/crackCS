# 회원가입 비밀번호 확인·제출 문구

- 독자·질문: 작업자·리뷰어 / 비밀번호 오입력과 제출 동작 혼동을 어떻게 차단했는가?
- 상태: 구현·로컬 검증 완료. PR·main CI 결과는 GitHub에서 별도 확인
- 기준: `fix/signup-password-confirmation`, base `27bf876`
- 상태 소유자: SignUpView가 확인 값·오류 소유. 가입 API는 기존 이메일·비밀번호·닉네임 계약 유지
- 실패 조건: 확인 미입력·원문과 불일치면 API 호출 전 중단. 공백 제거 없이 정확히 비교
- 제출 문구: `회원가입하기`. 성공 후 기존 로그인 안내로 이동

```text
확인 입력 → 화면 일치 검사 → 가입 API → 로그인 안내
              ↓ 실패
          입력 아래 오류
```

## 실행 증거 · 2026-10-13

- RED: 가입 테스트 11개 실패. 기존 버튼 문구 및 확인 입력 부재 확인
- GREEN: 가입 화면 11개 통과. 확인 값 제외 payload, 오류 해소·재제출, 기존 중복/화면 이탈/서버 오류 검증
- `bash scripts/verify.sh all`: 종료 0. Python 61, backend 557, frontend 343, PostgreSQL 77 통과. 스택 문서 19개 일치, 타입·production build 성공
- Node 24.21.0, Java 21.0.7. `.nvmrc` 지정 24.20.0과 같은 Node 24 계열 사용
- 최종 테스트 준비 코드 정리 후 가입 화면 11개 재실행 통과
- 로그: `/private/tmp/crackcs-signup-verify.log`, `/private/tmp/crackcs-signup-tests.log`, `/private/tmp/crackcs-signup-backend.log`
- cmux: caller workspace `workspace:1000050004`, terminal `surface:1000050022`, browser `surface:1000050023`. 포커스 이동 없이 실행
- 실제 화면: 확인 미입력 → 오류, 서로 다른 값 → 오류, 동일 값 → 로그인 안내 이동. 새 계정 실제 로그인 후 학습 홈 `/`·로그아웃 버튼 확인
- 로그인 후 `/questions`를 기다린 첫 관찰은 timeout. 실제 정상 목적지는 `/`로 확인. 가입·로그인 실패가 아님
- 로컬 API: 별도 메모리 H2, 실제 HTTP. 운영 DB 사용·네트워크 mock·유료 모델 호출 없음
- 한계: 로컬 가입 저장은 H2. 운영 프런트 반영은 PR 머지와 Production CD 결과로 별도 확인
