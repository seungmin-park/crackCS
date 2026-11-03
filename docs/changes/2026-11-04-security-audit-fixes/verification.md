# 보안 감사 후속 수정

- 독자·질문: CrackCS 작업자 / 2026-11-04 소스 감사에서 나온 항목 중 무엇을 고쳤고 무엇으로 확인했는가?
- 감사: 저장소 밖 `security-audit-skill/crackCS/run-1`(로컬 보관). 확정 0, 확인 필요 9, 기각 1. 대상 코드 실행 불가 환경에서 수행한 소스 추적 결과
- 전달: 목적 하나당 PR 하나. 각 PR이 아래 자기 절을 갱신

## 변경 단위와 상태

| 대상 | 상태 | 검증 목표 |
|---|---|---|
| 로컬 서버의 LAN 노출·DNS 리바인딩 | 구현·로컬 검증 완료 | `local` 프로필 loopback 바인딩, 허용 밖 Host 400 |
| 로그인 뒤 CSRF 토큰 유지 | 계획 | 로그인 전 토큰 거부, 로그인 뒤 새 토큰 사용 |
| 포크 PR CI 완료의 운영 배포 대기 취소 | 계획 | 배포 job만 동시 실행 그룹 참여 |
| 예상 밖 500의 원인 로그 누락 | 계획 | requestId와 예외를 서버 로그에 기록 |
| 세션에 직렬화되는 비밀번호 해시 | 계획 | 인증 뒤 principal의 해시 제거 |

## 로컬 서버 Host·바인딩 보호

```text
브라우저(rebind.example → 127.0.0.1) ─→ :8080
                                        ↓ StrictHttpFirewall
                      Host ∉ {localhost, 127.0.0.1, [::1]} → 400
LAN 다른 기기 ─→ 192.168.x.x:8080 → 수신 안 함(127.0.0.1 바인딩)
```

- 현상: H2 단독 `local` 프로필은 모든 인터페이스에서 수신. 두 로컬 프로필 모두 Host 제한 없음. 공개 데모 관리자 계정·실제 OpenAI 키가 같은 프로세스에 존재
- 상태 소유자: `crackcs.security.allowed-hosts` 설정값. `SecurityConfiguration.httpFirewall`이 Spring Security 방화벽에 허용 이름으로 전달
- 동작: 목록이 비면 Host 제한 없음(운영 기본값, 외부 프록시 책임). 목록이 있으면 그 밖의 Host를 필터 체인 진입 전에 거부
- `application-local.yaml`: `server.address`·`management.server.address` 127.0.0.1, 허용 Host `localhost,127.0.0.1,[::1]`. `local-postgres`는 `local`과 조합되어 같은 값 상속
- Vite 개발 서버 proxy는 기본 설정상 브라우저 Host(`127.0.0.1:5173`)를 유지하므로 허용 범위 안
- 남은 경계: 브라우저 자체의 Local Network Access 차단 여부와 무관하게 서버에서 거부. 운영 Host 정책은 nginx 설정 소유

### TDD 증거

- RED(빈 껍데기 bean 상태): `./gradlew test --tests '*AllowedHost*' --tests '*LocalProfileNetworkBoundaryTest'` 7건 중 4건 실패
  - 허용 밖 Host 미거부(`Expecting code to raise a throwable`), HTTP 400 기대·200 실제, 로컬 프로필 주소·허용 목록 null
- GREEN: 같은 명령 7건 통과
- 백엔드 전체: 아래 결과 참조

### 실행 결과

- 기준: 브랜치 `fix/local-loopback-host-guard`, main `3dfc131` 기반
- `bash scripts/verify.sh backend`: clean build·ArchUnit 포함 564건 통과(기존 557 + 신규 7), 실패·오류·skip 0, 종료 0
- `bash scripts/verify.sh docs`: Python 61건 통과, 스택 문서 19항목 PASS, 종료 0
- 필수 suite 추가: `AllowedHostFlowTest`, `LocalProfileNetworkBoundaryTest`(`docs/engineering/required-tests.json`). `check_test_reports.py backend` PASS
- 프런트: 변경 없음. 로컬 Node 26이라 Node 24 gate는 CI 결과로 판정
- PostgreSQL: schema·세션 저장소 변경 없음. CI `postgres` job 결과로 판정
- 미검증: 실제 브라우저의 DNS 리바인딩 재현, 로컬 앱 기동 후 LAN 기기 접속 거부 관찰
