# 실제 GPT 연결 검증

- 상태: 오프라인 후보 지표 통과, PostgreSQL 실제 GPT 사용자 흐름 확인, 시연 영상 생성. 공개 서비스 인증·파일럿은 미실행
- 작업 브랜치: `feat/openai-completion`, 별도 worktree `.worktrees/openai-completion`
- 비밀정보: 키는 1Password에서 cmux `workspace:2 / surface:12`로 숨김 입력. 키 값·원문 답변은 보고서에 기록하지 않음
- 비용 보호: OpenAI 프로젝트 월 $2 강제 한도 확인. `postgres` profile의 내부 월 예산 기본값 $2, 라이브 러너 1건/$0.25 기본값. 내부 추정액은 실제 청구액과 다를 수 있고 실패 호출·후속 질문을 포함하지 않음

## 확인된 실행

| 검증 | 결과 | 의미 |
|---|---|---|
| `./gradlew test --tests com.example.crackcs.evaluation.domain.EvaluationBudgetScopeTest --console=plain` | RED 1/1 실패 → GREEN 1/1 통과 | 비과금 평가 토큰이 GPT 월 예산에 섞이던 문제 재현·수정 |
| `env -u OPENAI_API_KEY ./gradlew openAiLiveEvaluation --console=plain` | 예상대로 실패, 유료 호출 0건 | 키 없는 실행은 요청 전에 중단 |
| `./gradlew openAiLiveEvaluation --console=plain` | 1/1 성공, $0.005546 추정, 8.918초 | 실제 Responses API·schema·근거·usage 파싱 확인 |
| `./gradlew openAiLiveEvaluation -PopenaiCaseLimit=4 -PopenaiReport=build/reports/evaluation/openai-development-4.json --console=plain` | 4/4 성공, 상세 일치 4/4, false-correct 0/1, 근거 4/4, p95 6.656초, $0.021268 추정 | 작은 개발 표본만 확인. 전체 품질 인증 아님 |
| `./gradlew openAiLiveEvaluation -PopenaiCaseLimit=138 -PopenaiMaxSpendUsd=1.0 -PopenaiReport=build/reports/evaluation/openai-development-full.json --console=plain` | `os-evaluator-v1` 개발 138/138 완료, 상세 128/138(92.75%), 이진 82/92(89.13%), false-correct 0/46, 근거·schema 138/138, p95 7.611초, $0.817682 추정 | 이진 90% 목표 미달. 정답 10건을 부분 정답으로 감점 |
| 개발 분할의 불일치 정답 10건 재호출 | `v1` 재실행에서 6건 감점, $0.06497 추정 | 응답 변동 확인. 모델 피드백은 로컬 `build/`에만 보관 |
| `os-evaluator-v2` 개발 정답·짝 오답 20건 | 13/20 일치, false-correct 0/10, $0.135902 추정 | 개선 근거 부족으로 미채택 |
| `os-evaluator-v3` 같은 개발 20건 | 20/20 일치, false-correct 0/10, p95 7.585초, $0.122842 추정 | 개발 사례 조정 결과. 별도 후보 검증 필요 |
| `./gradlew openAiLiveEvaluation -PopenaiSplit=evaluation-candidate -PopenaiCaseLimit=42 -PopenaiMaxSpendUsd=0.4 -PopenaiReport=build/reports/evaluation/openai-candidate-v3.json --console=plain` | `v3` 후보 42/42 완료, 상세 41/42(97.62%), 이진 27/28(96.43%), false-correct 0/14, 근거·schema 42/42, p95 10.859초, $0.254184 추정 | 사전 지정 85/90/5/100/99%·20초 목표 통과. 정답 1건 부분 감점 |
| `./gradlew test --console=plain` | 백엔드 508/508, 실패·건너뜀 0 | Ollama 전용 코드·테스트 제거 후 전체 회귀 |
| `./gradlew postgresTest --console=plain` | PostgreSQL 통합 65/65, 실패·건너뜀 0 | Testcontainers PostgreSQL 17 경로 회귀 |
| `npm test`, `npm run build` (Node 24.21.0) | 프런트 326/326, 타입 검사·빌드 성공 | 새 worktree에 `npm ci` 후 실행 |
| `python3 scripts/check_stack_docs.py` | 공식 스택 문서 18항목 통과 | 버전·공식 호스트 일치 |

## 실제 사용자 흐름과 시연

- 현재 cmux `workspace:2` 보조 pane의 Spring·Vite 터미널과 브라우저에서 수행. PostgreSQL 17.11, `local,local-postgres` profile, 평가·후속 질문 스위치 켜기. 실제 API 키는 숨김 입력
- 회원가입·로그인 → 공개 문제 답변 → 비동기 평가 완료 → 판정·근거·후속 질문 표시 → 후속 답변 재평가 → 지식 지도 확인. 모의 평가로 대체하지 않은 최초 흐름
- 최초 답변과 후속 답변 모두 화면 `정답 · 100점`; 저장 DB의 평가 ID 3·4가 `EVALUATED / CORRECT / gpt-5.6-terra / os-evaluator-v3`. 토큰은 각각 756/236, 844/271 입력/출력. 지식 지도 `프로세스와 스레드` 평가 2회·숙련도 100·신뢰도 50. 브라우저 오류 목록 0건
- [32초 무음 시연 영상](demo.mp4): 위 실제 흐름의 **완료 후 화면 7장을 cmux 브라우저에서 다시 열어 순차 편집한 캡처본**. 실시간 화면 녹화·클릭 애니메이션이 아님. macOS `screencapture`는 현재 세션에서 `could not create image from display`로 실패. 영상은 H.264 MP4, 1228×1666, 15fps이며 ffmpeg 디코딩 480프레임 성공
- 영상 장면: 학습 홈 → 문제 → 답변 이력 → GPT 판정 → 버전·구간이 있는 근거와 후속 질문 → 후속 답변 판정 → 지식 지도. 기존 저장 결과를 다시 열어 촬영해 추가 API 비용 없음

## 동일 사례 모델 비교

- 이전 로컬 모델의 `evaluation-candidate` 첫 12건: 상세 9/12(75%), p95 156.701초. [당시 실측](../2026-09-27-release-readiness/verification.md)과 [연결 검증](../2026-09-26-local-ollama/verification.md)에 보존
- GPT `v3`의 **동일 caseId 12건**: 상세 12/12, false-correct 0/4, p95 6.463초, 표준 요금 기준 $0.066062 추정
- 입력 caseId는 같지만 로컬 하드웨어 추론과 외부 API의 실행 환경·동시 부하가 달라 지연 차이를 모델 자체의 순수 효과로 해석하지 않음. 12건 결과만으로 일반 성능을 보장하지 않음
- 보존 보고서: [v1 개발 138건](../../evaluation/reference-v1/benchmarks/gpt-5.6-terra-development-baseline.json), [v3 개발 진단 20건](../../evaluation/reference-v1/benchmarks/gpt-5.6-terra-development-v3-paired.json), [v3 후보 42건](../../evaluation/reference-v1/benchmarks/gpt-5.6-terra-candidate-v3.json). 보고서는 caseId·판정·근거 ID 유효성·토큰·시간만 포함
- 유료 호출에서 timeout·429·5xx는 관측 0건. 401·결제 한도·일시 429 분기는 로컬 HTTP 계약 테스트로 확인. 실제 청구액은 OpenAI Platform 사용량 화면으로 대조 필요

## 아직 검증하지 않은 경계

- 실제 OpenAI 429·5xx·timeout 발생 시 브라우저 복구와 `AC-003`, 실제 GPT 평가 뒤 콘텐츠 교체의 `AC-007`: 계약·통합 테스트만 확인. 실서비스 장애나 교체 사건으로 바꾸어 보고하지 않음
- 실제 청구액: 추정 토큰 비용과 OpenAI Platform 사용량 대조 필요. 프로젝트 월 $2 강제 한도는 사용자가 설정 완료했다고 확인
- 공개 콘텐츠의 출처·필수 개념 사람 승인, 대표 사용자 답안·실제 참가자 파일럿, 인터넷 배포·운영 부하: 로컬 시연 범위 밖의 공개 출시 조건

```text
1Password → cmux 숨김 입력 → Gradle 라이브 러너 → Responses API
              └─ 키 미기록          └─ 판정·근거·usage만 보고서
OpenAI Platform 월 $2 강제 한도 ────────┘
```
