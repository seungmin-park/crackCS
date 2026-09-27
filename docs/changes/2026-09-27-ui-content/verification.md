# 화면 복구·초기 콘텐츠 검증

## 결과

- 화면 오류 분류·로딩·빈 결과·입력 오류·수동 재조회 구현 완료
- 인증 복원 실패의 빈 화면 대신 원래 주소를 보존하는 연결 복구 경로
- 초기 콘텐츠: 25문항·50개 필수 개념·5개 문서, 로컬 PostgreSQL 미검수 초안 등록 완료
- 사람의 사실·출처·이용 조건 검수 및 공개 승인: 미완료
- 실제 모델 품질·provider 실패 뒤 성공 복구: 기존 미완료 상태 유지, 이번 검증은 모델 호출 없음

## 책임과 동작

```text
HTTP 오류 → presentRequestError
              ├─ kind / 안전한 문구 / 입력 오류 / 수동 재시도 여부
              └─ 화면·유스케이스
                    ├─ 조회 실패 → RequestFailure
                    ├─ 권한 거부 → 이전 보호 데이터 숨김
                    ├─ 일시 장애 → 답변·입력 보존
                    └─ 429 → 자동 polling 중단, 수동 재조회

bundle.json → 구조·중복 검사 → 기존 관리자 API → DRAFT
                                                    ↓
                                              사람 검수·공개 대기
```

- HTTP 상태 숫자 해석: presentation 한 곳
- 인증 상태의 401 처리: `useAuth`의 별도 세션 계약 유지
- 자동 재시도 횟수·제출 결과 불확실성: 해당 유스케이스 소유
- 오류 표시 컴포넌트가 API 호출·도메인 변경을 직접 수행하지 않음
- 관리자 상세 실패·로딩에는 이전 항목의 폼을 새 선택 결과처럼 표시하지 않음
- 콘텐츠 importer는 도메인 검증을 우회하지 않고 기존 API 사용
- 재실행 시 같은 미검수 초안 재사용, 변경 충돌은 중단. 전체 번들 원자성·동시 실행 미지원

## 자동 검증

| 범위 | 결과 | 실행 |
|---|---:|---|
| 프런트 전체 | 326 성공, 실패·skip 0 | `npm --prefix front test` |
| TypeScript·production build | 성공 | `npm --prefix front run build` |
| 콘텐츠 도구 | 14 성공 | `python3 -m unittest discover -s scripts -p 'test_content_bundle.py'` |
| 콘텐츠 구조 | 25문항·5문서·50개 Concept·20출처 통과 | `python3 scripts/content_bundle.py --report build/reports/ui-content/content-audit.json` |
| 실제 관리자 API·DB | 89 assertion 성공 | 아래 등록·검증 흐름 |

실행 환경: Java 21.0.7, Node 24.21.0, PostgreSQL 17.11, `local,local-postgres` profile. 백엔드 Java 변경 없음. 기존 H2 492·PostgreSQL 56 회귀는 [직전 변경의 증거](../2026-09-27-release-readiness/verification.md)이며 이번 신규 실행으로 계산하지 않음.

### RED → GREEN

- 인증·로그인·관리자 오류: 5개 실패 확인 후 관련 24개 통과
- HTTP 분류·실제 router 경로: 8개 실패 확인 후 관련 29개 통과
- 학습 화면 403·이전 회원 비표시·빈 회원 목록·가입 서버 오류: 5개 실패 후 관련 26개 통과
- 429 polling·관리자 상세 복구: 3개 실패 후 전체 회귀에 포함
- 관리자 입력 오류 이유: 1개 실패 후 공통 alert 반영
- 콘텐츠 도구: 검사 없는 최소 함수에서 중복·가중치·출처·공개 상태·충돌 테스트 실패 확인 후 구현
- 리팩터링 회귀 중 2건: boolean→오류 객체 계약과 CSS 선택자 변경. 오류 없음은 undefined, 화면 검증은 role=alert 기준으로 갱신

## cmux 실제 E2E

- 호출 확인: `CMUX_WORKSPACE_ID=9DAB6064-4C08-4296-BFF8-44946D90A5E0`, `CMUX_SURFACE_ID` 없음
- `cmux identify --json`: caller workspace:1 / surface:1 / pane:1
- 다른 workspace에 있던 시각적 포커스를 작업 대상으로 사용하지 않음
- 기존 보조 pane:11의 surface:14에서 DB·Spring·Vite 명령과 로그 실행
- 같은 pane의 브라우저 surface:21에서 실제 클릭·입력·이동
- 일시 러너 터미널 surface:22에서 Python 등록·재등록·검증 실행
- 검증 후 surface:21·22 정리. 기존 결과 터미널 surface:14 유지

| 실제 흐름 | 확인 결과 |
|---|---|
| Spring 미실행, `/questions?page=2` 새 진입 | `/connection-error?redirect=/questions?page=2`, 오류·다시 연결 버튼 |
| 서버가 계속 꺼진 상태에서 다시 연결 | 오류 화면 유지, 중복 제출·빈 화면 없음 |
| DB·Spring 실행 후 다시 연결 | 로그인 화면에 원래 redirect 유지 |
| 로컬 관리자 로그인 | 문제 목록 복귀, 범위 밖 page=2는 실제 마지막 page=1로 정규화 |
| 25문항 초안 등록 후 공개 문제 조회 | 기존 공개 문제만 표시 |
| 관리자 JPA-105 선택 | DRAFT, 모범 답안, 필수 개념 2개·각 0.50 표시 |
| 기존 문제 본문을 공백으로 입력해 초안 수정 | 400, 공통 alert에 `content는 공백일 수 없습니다.` 표시, DB 내용 유지 |
| 같은 문제 재선택 | 저장된 원문·평가 기준 복원 |
| 관리자 근거 문서 조회 | 5개 initial-v1 문서의 DRAFT·각 기술 버전 표시 |
| RETIRED 문서 필터 | `조건에 맞는 문서가 없습니다.` 표시 |

실제 UI의 server-off·400·empty·정상 복구 검증과 자동 테스트의 403·429·늦은 응답·폴링 검증을 구분. 브라우저에서 네트워크 mock을 사용한 것처럼 보고하지 않음.

## 콘텐츠 실제 저장 확인

```bash
python3 scripts/content_bundle.py --apply --report build/reports/ui-content/import-first.json
python3 scripts/content_bundle.py --apply --report build/reports/ui-content/import-second.json
```

- 두 실행 모두 종료 성공, 두 보고서 `registeredIds` 일치
- 문제 25개: DRAFT·검수자 null·검수 시각 null, 원문·모범 답안 일치, 필수 Concept 2개·0.50
- 문서 5개: DRAFT·검수자 null·검수 시각 null, 본문·버전 일치
- 공개 문제 조회: 신규 25개 ID 미포함
- 변경된 모범 답안을 가진 번들 재등록: conflict 거절, 원래 DB 답안 유지
- assertion 89개, 실패 0
- source 원문을 공개 라이선스로 전환하거나 이용 허가를 추정하지 않음. 검수 경계는 [콘텐츠 기준](../../content/initial-v1/README.md)
- 출처 절 번호 재점검: xv6 rev5의 Scheduling은 8장, Sleep/Wakeup은 9장; OSTEP Round Robin은 7.7절. 초안 locator 정정.
- reference-v1 완전 문장 중복 0. 최대 문장 유사도 0.408; 의미 공유 7항목과 주제 편향 별도 기록

## 증거 위치

로컬 `build/reports/ui-content/`는 git 제외:

- `front-red.log`, `contracts-red.log`, `screens-red.log`, `polling-red.log`, `validation-red.log`
- `front-all.log`, `front-build.log`, `content-red.log`, `content-green.log`, `content-audit.json`
- `import-first.json`, `import-second.json`, `import-verification.json`, 실제 assertion 러너 `verify-import.py`
- `backend.log`, `vite.log`
- `connection-error.png`, `admin-validation.png`, `draft-document.png`, `empty-documents.png`

## 미검증·실패 조건

- 실제 콘텐츠 사람 검수·공개 0건. OQ-006 공개 최소량 충족으로 계산 금지
- 새 25문항의 실제 AI 판정 정확도·retrieval 품질 측정 없음
- importer 중간 실패는 완료된 초안 유지. 동시 실행·등록 후 식별 문구 변경은 수동 확인 필요
- 로컬 E2E와 단위 회귀의 범위. 실제 원격 다중 인스턴스·인터넷 환경 검증 아님
