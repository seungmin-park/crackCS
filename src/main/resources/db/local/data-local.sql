INSERT INTO member (nickname, role, status, created_at, updated_at)
SELECT 'CrackCS Local Admin', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM auth_account
    WHERE provider = 'LOCAL' AND login_id = 'admin@crackcs.local'
);

INSERT INTO auth_account (member_id, provider, login_id, password_hash, last_login_at, created_at)
SELECT m.id, 'LOCAL', 'admin@crackcs.local',
       '{bcrypt}$2y$10$pCzG2Z6nWNKGzxoMeqWs4.d5Ab8c/rZg89X1RVAZHQ0TBM5gomKdC',
       NULL, CURRENT_TIMESTAMP
FROM member m
WHERE m.nickname = 'CrackCS Local Admin'
  AND NOT EXISTS (
      SELECT 1 FROM auth_account
      WHERE provider = 'LOCAL' AND login_id = 'admin@crackcs.local'
  )
ORDER BY m.id
FETCH FIRST 1 ROW ONLY;

INSERT INTO topic (parent_id, code, name, active)
SELECT NULL, 'OPERATING_SYSTEM', '운영체제', TRUE
WHERE NOT EXISTS (SELECT 1 FROM topic WHERE code = 'OPERATING_SYSTEM');

INSERT INTO concept (topic_id, code, name, description, active)
SELECT t.id, 'PROCESS_THREAD', '프로세스와 스레드', '실행 단위와 자원 공유 방식의 차이', TRUE
FROM topic t
WHERE t.code = 'OPERATING_SYSTEM'
  AND NOT EXISTS (SELECT 1 FROM concept WHERE code = 'PROCESS_THREAD');

INSERT INTO question (
    topic_id, created_by_member_id, reviewed_by_member_id, origin, type, difficulty,
    content, reference_answer, version_series_id, question_version, status,
    reviewed_at, created_at, updated_at
)
SELECT t.id, m.id, m.id, 'ADMIN', 'NORMAL', 'BASIC',
       '프로세스와 스레드의 차이를 설명하세요.',
       '프로세스는 독립된 주소 공간과 자원을 가지며, 스레드는 같은 프로세스 안에서 주소 공간과 자원을 공유합니다.',
       '00000000-0000-0000-0000-000000000001', 1, 'PUBLISHED',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM topic t
JOIN member m ON m.nickname = 'CrackCS Local Admin'
WHERE t.code = 'OPERATING_SYSTEM'
  AND NOT EXISTS (
      SELECT 1 FROM question
      WHERE content = '프로세스와 스레드의 차이를 설명하세요.'
  )
ORDER BY m.id
FETCH FIRST 1 ROW ONLY;

INSERT INTO question_concept (question_id, concept_id, weight, is_required)
SELECT q.id, c.id, 1.00, TRUE
FROM question q
JOIN concept c ON c.code = 'PROCESS_THREAD'
WHERE q.content = '프로세스와 스레드의 차이를 설명하세요.'
  AND NOT EXISTS (
      SELECT 1 FROM question_concept qc
      WHERE qc.question_id = q.id AND qc.concept_id = c.id
  );
