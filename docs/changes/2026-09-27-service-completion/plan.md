# 서버 운영·로컬 실행 검증 계획

> 실행: `superpowers:executing-plans`, 직접 구현 후 독립 검토. 사용자 지시: AI 모델·외부 AI API 연결을 제외한 전체 마무리.

**Goal:** 추가 비용 없이 서버 보안·다중 앱 정합성·로컬 실행 근거 완성.

**Architecture:** 로그인 제한 상태는 DB aggregate, 시간 창·차단 규칙은 도메인 소유. 기존 관리자·평가 API와 통제 provider로 AI 없는 사용자 흐름 검증. 외부 모델 품질·실제 사람 검수·참가자 파일럿은 실행 사실과 구분.

**Tech Stack:** Java 21, Spring Boot 4.1.1, JPA, PostgreSQL 17, Vue 3, Python 표준 라이브러리.

**Spec:** [제품 명세](../../product/spec.md), [미완료 목록](../../planning/tasks.md), 사용자 2026-09-27 추가 지시.

## 제약

- 실제 AI 호출·모델 설치·유료 서비스 제외
- 기존 회원·답변·문서·회고 보존, schema 변경은 명시적 추가 migration
- 사용자 실패 기록의 로그인 ID·IP 원문 저장 금지
- 기존 정책: 정규화 계정+접속 주소 기준 10분 내 5회 실패, 15분 차단
- 코드 변경은 RED→GREEN→전체 회귀
- 현재 cmux workspace의 기존 보조 pane에서 E2E 실행·결과 표시

## 검토 초점

- 두 인스턴스에서 처음 실패를 동시에 기록해도 손실 없음
- 창·차단 만료 시각 경계, 성공 후 초기화
- DB 실패를 차단 성공이나 로그인 허용으로 숨기지 않음
- schema 추가가 기존 local DB·다른 작업트리를 깨뜨리지 않음
- 통제 provider 결과를 실제 AI 품질이나 사용자 파일럿 결과로 표시하지 않음

## Task 1: 로그인 제한의 공유 저장

파일: `auth/domain/LoginAttempt`, `auth/repository/LoginAttemptRepository`, `DefaultLoginAttemptService`, 도메인·Service 테스트, 명시적 V002 SQL, DB 실행 문서.

- [x] 같은 DB를 쓰는 독립 Service 인스턴스 간 실패 기록 공유 테스트 RED
- [x] 실패 창·차단 시간·성공 초기화·동시 최초 생성·만료 기록 정리 테스트
- [x] DB 잠금·최초 생성 UNIQUE 경쟁 재시도, 규칙을 도메인으로 이동
- [x] H2·PostgreSQL 관련 테스트, 기존 인증 회귀
- [x] 명시적 schema 추가·기존 DB 적용·문서 갱신

## Task 2: AI 없는 실제 운영 흐름

- [x] 두 로컬 앱·한 PostgreSQL에서 로그인 차단 공유·학습 상태 경쟁 검증
- [x] 통제 provider 실패→재시도→성공과 화면 복구, 관리자 실패 추적
- [x] 공개 API·인가·개인정보·로그·rate limit 점검 결과·수용 경계 기록
- [x] 수정한 initial-v1 근거 문서 동기화, 미검수·비공개 유지 확인

## Task 3: 실행 자료와 최종 증거

- [x] P0 요구사항·AC·API·schema·테스트 대응표와 미검증 범위 정리
- [x] README를 바로 실행하고 핵심 설계·검증을 읽는 순서로 정리
- [x] 사람 검수용 콘텐츠 자료와 제한 파일럿 절차 준비; 실행하지 않은 사람 검수를 표시하지 않음
- [x] 전체 backend/frontend·PostgreSQL·콘텐츠 도구 회귀, 문서 링크 점검
- [x] 독립 검토·수정·커밋. 작업 브랜치 보존
