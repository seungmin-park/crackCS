# 운영 콘텐츠 검토 대기 등록

- 독자·질문: 콘텐츠 검수자·운영자 / 준비한 문항이 어디에 어떤 상태로 등록됐는가?
- 상태: 2026-10-13 초기 등록의 이력. 이후 교체·추가는 [외부 자료 처리 기록](../2026-10-14-source-content/verification.md) 참조
- 보존 이유: 관리자 권한 변경·최초 85문항 등록·초기 백업의 당시 증거
- 실행일: 2026-10-13. 대상: deploy-vm의 crackcs PostgreSQL·실제 운영 앱
- 사용자 요청: 본인 계정 관리자 승격, 준비된 문항 전체를 직접 검토 후 공개 결정
- 관리자 권한: 지정 LOCAL 계정 1건 USER → ADMIN. ACTIVE 유지, 인증 버전 증가·updatedAt 갱신. 재로그인 필요
- 개인 이메일·비밀번호·세션·DB 비밀값은 이 공개 문서에 기록하지 않음

## 등록 결과

| 원본 | 문항 | 개념 | 근거 문서 | 운영 상태 |
|---|---:|---:|---:|---|
| initial-v1 | 25 | 50 | 5 | 미검수 DRAFT |
| a1-v1 | 30 | 64 | 6 | 미검수 DRAFT |
| a2-v1 | 30 | 60 | 6 | 미검수 DRAFT |
| 합계 | 85 | 174 | 17 | 공개 0건 |

- 등록 전: 주제·개념·문항·문서 모두 0건
- 등록 후: 17개 주제, 174개 개념, 85문항, 17문서
- 관리자 API 재조회 assertion: 문항 85개·문서 17개, 모든 status DRAFT·reviewedAt null
- 별도 DB 재조회: 검수된 문항 0건, 공개 문항 0건, 임시 작업 계정 0건
- 기존 로컬 OS·Java 승인 이력과 운영의 미검수 등록은 별개

```text
버전화된 3개 bundle → 기존 관리자 API → DRAFT
                                          ↓ 사용자 판단
                                       검수 → 공개
```

## 실행·실패 경계

- 원본 구조 검사 후 VM loopback의 기존 관리자 API로 등록. 콘텐츠를 DB에 직접 삽입하거나 검수 필드를 조작하지 않음
- 운영에서 재사용 가능한 역할 변경 API는 없음. 계정 1건만 명시해 DB transaction으로 관리자 승격
- 임시 무작위 인증정보는 메모리에서만 사용. 등록 작업 종료 시 문항·문서 작성자 FK를 요청한 관리자에 연결하고 임시 AuthAccount·Member 삭제
- 최초 API 등록 시 Secure 쿠키가 loopback HTTP에 전달되지 않아 403. 공개 서비스 설정 유지, VM 내부 일회성 클라이언트에서만 쿠키 전달 조정 후 성공
- DB 백업: VM `/tmp/crackcs-content-import/before-content-and-admin.dump`, 권한 0600. 최초 권한 변경 전 pg_dump 성공. 공개 저장소에 백업 포함 금지
- 등록 ID 보고서: VM `/tmp/crackcs-content-import/import-report.json`
- 일회성 실행 스크립트: VM `/tmp/crackcs-content-import/crackcs-content-maintenance.py`, 종료 코드 0. 암호를 인수·로그·파일에 기록하지 않음
- 이후 등록 실패: 기존 importer가 완료한 초안은 유지. 자동 덮어쓰기·자동 공개 없음
- 실제 운영 관리자 API로 등록·재조회 확인. 본인 비밀번호를 요구하거나 사용자 대신 로그인하지 않음. 본인 브라우저 로그인 후 검수·공개 조작은 사용자에게 남김
- 진단 사례·rubric 상세는 bundle/review 자료에 유지. 기존 importer 범위인 질문·답안·개념 가중치·근거 문서 등록
- reference-v1은 평가용 표본이므로 학습 문제로 등록하지 않음. 미제작 커리큘럼 범위도 포함하지 않음

## 원본 식별

- initial-v1 SHA-256: `cba7aae00b69c39fd4a4b6a0659acd50f121e5d889e5ae14cadc0b41c6295333`
- a1-v1 SHA-256: `4c08549e6563d13fd92dea9a1fded94eab675d283d28c1079ab18d0599e67beb`
- a2-v1 SHA-256: `8077c1a2388e9e454ad72d98b8db6f71dd92f7b2e3213c5786bf038b5f648292`
- 후속: 관리자 문제·근거 문서에서 사실·답안·근거 확인 → 승인한 항목만 검수·공개 → 문서 검색 문단 생성
