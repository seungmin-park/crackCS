# 기술 스택 공식 문서 동기화 검증

- 범위: `feat/agent-engineering-docs` 작업 환경의 빌드 버전 ↔ 공식 문서 목록 ↔ CI 검사 경로
- 확인일: 2026-09-29
- 앱 동작 변경: 없음. 사용자 화면 E2E 대상 아님

## 실행 결과

| 검증 | 실제 결과 |
|---|---|
| RED: 문서 검사기 구현 전 `python3 -m unittest discover -s scripts -p 'test_check_stack_docs.py' -v` | 검사기 파일 부재로 5개 중 4개 실패. 요구한 버전/호스트 경계의 누락 확인 |
| GREEN: 같은 명령 재실행 | 5개 실행, 5개 통과, 종료 코드 0. 정상 목록과 Vue lockfile 변경, Spring Boot 변경, 비공식 호스트, 사람이 읽는 표의 누락 검증 |
| `python3 scripts/check_stack_docs.py` | `PASS: 18 stack documentation entries match repository selections and official hosts`, 종료 코드 0 |
| cmux 화면에서 같은 두 명령 실행 | 호출 workspace `workspace:2`, 보조 `surface:9`; 5개 통과·18개 PASS·`VERIFY_EXIT:0` 확인. pane 유지 |
| `git diff --check` | 공백 오류 없음 |

검사 원리:

```text
build.gradle · Gradle wrapper · .nvmrc · package-lock.json
                ↓ 선택 버전
docs/engineering/stack-docs.json · stack-docs.md
                ↓ 불일치·비공식 호스트 차단
scripts/check_stack_docs.py → CI stack-docs job
```

- 문서 확인: Java 21, Spring Boot 4.1.1 및 Boot BOM의 핵심 라이브러리, PostgreSQL 17, Gradle 9.7.1, Node 24, Vue 3.5, Router 5, TypeScript 6.0, Vite 8, Vitest 4, Element Plus 2, Bootstrap 5.3의 공식 문서 범위 확인. [현재 버전·출처](../../engineering/stack-docs.md)
- Gradle `current` 문서가 9.8.0으로 이동한 것을 발견해 wrapper 9.7.1의 [버전 고정 공식 문서](https://docs.gradle.org/9.7.1/userguide/)로 변경. HTTP 200 확인
- 미검증: 오프라인 CI는 외부 사이트의 향후 내용·링크 생존성을 확인하지 않음. Boot BOM의 managed 버전 값은 공식 BOM에서 확인했지만 CI가 resolved graph 자체를 조회하지는 않음. 앱 브라우저·실제 모델 호출은 이 변경의 검증 대상이 아님
