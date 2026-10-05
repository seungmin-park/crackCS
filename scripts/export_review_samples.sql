-- Caller selects IDs below. Read only; excludes identities and all model outputs.
BEGIN READ ONLY;
SELECT jsonb_build_object(
    'version', 'os-independent-local-v1',
    'topicCode', 'OPERATING_SYSTEM',
    'sampling', jsonb_build_object(
        'population', 'User-selected local DB Answer 1-4',
        'method', 'Existing local history; all four selected before independent review',
        'limitations', 'Four answers, two members, three related questions. Previous demos overlap. Learner origin and blind-review eligibility require human confirmation; no representative sample claim.'),
    'cases', jsonb_agg(jsonb_build_object(
        'caseId', 'LOCAL-' || a.id,
        'provenance', jsonb_build_object('kind', 'UNCONFIRMED', 'source', 'crackcs-local-postgres-1/crackcs_local', 'sourceAnswerId', a.id),
        'question', jsonb_build_object(
            'id', q.id, 'version', q.question_version, 'content', q.content, 'referenceAnswer', q.reference_answer,
            'concepts', (SELECT jsonb_agg(jsonb_build_object(
                'code', c.code, 'name', c.name, 'description', c.description, 'weight', qc.weight, 'required', qc.is_required) ORDER BY c.code)
                FROM question_concept qc JOIN concept c ON c.id=qc.concept_id WHERE qc.question_id=q.id)),
        'answer', a.content) ORDER BY a.id))
FROM answer a JOIN question q ON q.id=a.question_id JOIN topic t ON t.id=q.topic_id
WHERE a.id IN (1, 2, 3, 4) AND t.code='OPERATING_SYSTEM';
COMMIT;
