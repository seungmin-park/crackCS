# 화면 복구와 초기 콘텐츠 구현 계획

> 실행 방식: `superpowers:executing-plans`. 기존 worktree에서 직접 구현, 마지막 독립 검토.

**Goal:** 오류 뒤 다시 사용할 수 있는 화면과 검수 가능한 초기 25문항 준비.

**Architecture:** HTTP 오류 분류는 presentation 함수, 표시 위치·보존할 입력·재조회 정책은 화면과 유스케이스 소유. 콘텐츠 원본은 버전별 JSON, 기존 관리자 API로 DRAFT 등록. 검수·공개는 별도 상태 전이.

**Tech Stack:** Vue 3·TypeScript·Vitest, Python 표준 라이브러리, Spring 관리자 API·PostgreSQL.

**Spec:** [작업 목록](../../planning/tasks.md)의 프런트 마무리·P8-T07, [콘텐츠 정책](../../product/content-and-ai-policy.md)의 OQ-006.

## 공통 제약

- 추가 유료 호출·호스팅 없음
- reference-v1 불변, 평가 표본을 공개 콘텐츠로 복사 금지
- 실제 사람 검수 없이 reviewedAt·PUBLISHED 생성 금지
- E2E: 호출 cmux workspace의 보조 터미널·브라우저에서 실제 흐름 검증
- 실패 테스트 → 최소 구현 → 회귀; 문서와 체크 표시는 증거 기준

## 검토 초점

- 인증 복원 실패 뒤 원래 URL 복구, 잘못된 redirect 차단
- 권한 거부 뒤 이전 보호 데이터 비표시
- 429 자동 재조회 중단, 수동 복구·입력 보존
- 콘텐츠 입력 재실행의 중복·사용자 수정 덮어쓰기 방지
- 출처 버전, 필수 개념과 문서의 대응, 공개 승인과 기술 검증의 구분

## 1. 화면 상태

대상: `front/src/presentation/requestErrorPresentation.ts`, `RequestFailure.vue`, auth·학습·관리자 화면과 composable.

- [x] RED: 인증 복원 실패, validation, 5xx 내부 메시지 차단
- [x] GREEN: 오류 분류·연결 복구·입력별 오류
- [x] RED: 403 재시도 차단·이전 회원 비표시·빈 목록·429·관리자 상세 실패
- [x] GREEN: 공통 표시 연결, 조회 세대 유지, 자동/수동 재시도 구분
- [x] 전체 Vitest·type-check·build: 326개 통과
- [x] cmux 실제 연결 실패→복구, 관리자 validation·빈 상태 확인
- [ ] 독립 검토 및 커밋

## 2. 초기 콘텐츠

대상: `docs/content/initial-v1/`, `scripts/content_bundle.py`, 관련 도구 테스트.

- [x] 공식 출처 본문·버전 확인, reference-v1 60문항과 주제 비교
- [x] 운영체제·Java·Spring Framework·Spring Boot·JPA 각 5문항, 개념·가중치·문서 작성
- [x] RED: 잘못된 참조·가중치·공개 상태·중복·재실행 충돌
- [x] GREEN: 번들 검사와 기존 관리자 API의 DRAFT 등록
- [x] 실제 로컬 DB 등록·재실행·미검수/비공개 상태 확인
- [ ] 기준 문서·tasks·검증 기록 갱신, 독립 검토 및 커밋

## 완료 경계

- 기술적 준비와 사람 검수·공개 완료를 별도 보고
- 실제 모델 품질·provider 성공 흐름은 이 작업 밖의 기존 미완료 항목 유지
