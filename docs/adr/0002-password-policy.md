# ADR-0002: LOCAL 계정 비밀번호 정책

- 상태: 승인
- 결정일: 2026-08-31
- 관련 항목: OQ-008, P2-T01

## 배경

Phase 2에는 MFA와 유출 비밀번호 목록 조회가 없다. 짧은 비밀번호를 복잡도 조합 규칙만으로 보완하면 사용자가 예측 가능한 대문자·숫자·특수문자 패턴을 반복하기 쉽다.

## 결정

- 최소 15자, 최대 64자
- 영문 대문자, 숫자와 특수문자의 특정 조합은 강제하지 않는다.
- 공백과 한글을 포함한 passphrase를 허용한다.
- 제어 문자는 허용하지 않는다.
- bcrypt 입력 한계를 넘지 않도록 UTF-8 기준 72 byte 이하로 제한한다.
- 비밀번호를 trim, lowercase 또는 Unicode 정규화하지 않고 사용자가 입력한 원문 그대로 검증·해시한다.
- 저장은 Spring Security `DelegatingPasswordEncoder`의 현재 기본 단방향 hash를 사용한다.
- 비밀번호 원문, session ID와 CSRF token을 애플리케이션 로그에 기록하지 않는다.

## 동작

```text
request DTO password
      ↓
길이·제어문자·UTF-8 byte 검증
      ↓
PasswordEncoder.encode(rawPassword)
      ↓
{algorithm}encodedHash만 AuthAccount에 저장
      ↓
rawPassword 참조를 필드나 로그에 보관하지 않음
```

## 결과와 한계

- 긴 passphrase를 사용할 수 있고 조합 규칙으로 인한 예측 가능한 패턴을 줄인다.
- 가입 단계에서 유출 비밀번호 blocklist는 아직 확인하지 않는다.
- MFA 도입, 유출 비밀번호 검사와 hash 비용 측정은 운영 보안 강화 단계에서 추가한다.
- password hash 형식에 algorithm ID를 포함하므로 이후 더 강한 encoder로 점진적으로 교체할 수 있다.

