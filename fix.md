# Code Quality Fix Checklist

기준 보고서: `quality-20260903-025705.md`

```text
품질 보고서의 발견
        ↓
검증 가능한 개선 작업
        ↓
테스트와 구현이 모두 완료된 항목만 [x]
```

- [x] **Question이 Concept 불변식을 최종 방어하도록 개선**
  - [x] `addConcept`가 비활성 Concept을 거부한다.
  - [x] `addConcept`가 다른 Topic의 Concept을 거부한다.
  - [x] `replaceConcepts`가 비활성 또는 다른 Topic의 Concept을 거부한다.
  - [x] 교체 검증 실패 시 기존 Concept과 `updatedAt`을 유지한다.
  - [x] 순수 도메인 테스트와 전체 테스트가 통과한다.
- [ ] **Question과 KnowledgeDocument의 버전 생성·공개 전환을 동시 요청에도 원자적으로 보장**
  - [ ] series/version 중복을 DB 제약으로 차단한다.
  - [ ] 동시 버전 생성 충돌을 명시적인 애플리케이션 오류로 변환한다.
  - [ ] series당 공개본 하나를 잠금 또는 DB 모델로 보장한다.
  - [ ] 동시성 통합 테스트로 경쟁 조건을 검증한다.
- [ ] **로그인 시도 제한 저장소의 메모리와 다중 인스턴스 경계를 개선**
  - [ ] 만료와 최대 크기가 있는 저장소로 무제한 메모리 증가를 막는다.
  - [ ] 4회/5회, 10분 window, 14분 59초/15분 경계를 검증한다.
  - [ ] 성공 초기화, 계정/IP 격리와 로그인 ID 정규화를 검증한다.
  - [ ] 다중 인스턴스 도입 전 공유 저장소 전환 조건을 문서화한다.
- [ ] **관리자 UI의 책임을 분리하고 비동기 실패에서도 상태를 복구**
  - [ ] 초기 로드가 실패해도 loading 상태가 종료된다.
  - [ ] Question 폼과 criteria 상태를 composable 또는 하위 컴포넌트로 분리한다.
  - [ ] 관리자 API를 Topic, Concept, KnowledgeDocument, Question, Member 단위로 나눈다.
  - [ ] 생성·수정·검수·공개·폐기·새 버전의 실패 흐름을 테스트한다.
- [ ] **인증과 재시도 UI의 상태 전이·실패·경계 테스트를 보강**
  - [ ] `useAuth`의 401, 예상 밖 오류, 동시 restore, login/logout, CSRF 초기화를 검증한다.
  - [ ] 모듈 전역 인증 상태를 테스트마다 격리한다.
  - [ ] Question 목록·상세의 실패 → 재시도 → 복구 흐름을 검증한다.
  - [ ] 운영 DB 동시성, 다중 인스턴스와 브라우저 E2E의 미검증 경계를 유지한다.
