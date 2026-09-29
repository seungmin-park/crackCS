# 작업자 검증 경로

- 독자·질문: CrackCS 작업자·에이전트 / 버전이 바뀔 때 공식 문서 기준과 실제 동작을 어떻게 함께 확인할 것인가?
- 범위: 기술 스택 문서 동기화의 한 경로. 앱 전체 기능 지도·전체 E2E 인증은 아님

## 기능 지도: 의존성 변경 뒤 공식 문서 검토

| 항목 | 현재 경로 |
|---|---|
| 목적 | 새 Java/Spring/Vue/TS API를 사용할 때 다른 버전의 문서를 따라 코드를 바꾸지 않도록 버전 변화 감지 |
| 선행 조건 | Python 3, repo checkout. 앱 서버·계정·시드·Docker 불필요 |
| 입력 | `build.gradle`, Gradle wrapper, `front/.nvmrc`, `front/package.json`, `front/package-lock.json` |
| 시작점 | `python3 scripts/check_stack_docs.py` |
| 성공 관찰 | 18개 항목 PASS·종료 코드 0. 사용자 데이터 변경 없음 |
| 실패 관찰 | manifest·lockfile 버전 불일치, 목록 누락·중복, 미검증 문서 호스트, Spring Boot 관리 버전 검토 누락이면 FAIL·종료 코드 1 |
| 상태 소유자 | build/lockfile이 설치 버전 소유. `stack-docs.json`은 사람이 확인한 공식 문서 주소와 적용 범위 소유 |
| 의존 경계 | 앱 runtime은 문서 목록에 의존하지 않음. 문서 검사는 빌드 설정을 읽고 CI에서 변경을 차단 |
| 검증 명령 | `python3 -m unittest discover -s scripts -p 'test_check_stack_docs.py'` 및 `python3 scripts/check_stack_docs.py` |
| 증거 | [이번 실행](../changes/2026-09-29-agent-engineering/verification.md), [문서 기준](stack-docs.md) |

```text
build.gradle / package-lock.json → 문서 목록 → 검사 → CI
            실제 버전          확인 기록      불일치 거부
```

- Boot BOM 관리 버전의 정확한 resolved graph는 Gradle 의존성 조회의 책임. 로컬 목록은 4.1.1 BOM에서 확인한 값을 기록. Boot 플러그인 버전 변경 시 모든 managed 항목에 재검토 요구
- 문서 링크의 도메인·버전 표기를 기계적으로 확인. 문서 내용과 코드 사용법의 적합성은 해당 변경의 테스트·리뷰 책임
- 예외 처리: 공식 문서가 리다이렉트하거나 `current`가 새 버전으로 움직이면 적용 범위를 확인한 뒤 목록에 기록. 다른 도메인을 예외로 추가할 때는 `check_stack_docs.py`의 host 규칙과 출처를 함께 리뷰

검증 스킬: [verify-crackcs](../../.agents/skills/verify-crackcs/SKILL.md). 앱 사용자 흐름은 [기존 실행 안내](../changes/2026-09-27-release-readiness/local-runbook.md)와 현재 cmux E2E 규칙 사용.
