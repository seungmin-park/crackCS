"""Bundle tests use their own tiny data; no dependency on local SQL seed."""
import copy
import unittest
from content_bundle import validate_bundle, reuse_or_create


class BundleValidationTest(unittest.TestCase):
    def setUp(self):
        self.bundle = {
            "version": "test-v1", "status": "DRAFT", "reviewedBy": None,
            "sources": [{"id": "s", "url": "https://docs.oracle.com/a", "title": "Spec", "checkedAt": "2026-09-27", "bodySha256": "a" * 64, "licenseNote": "Link only"}],
            "topics": [{"code": "JAVA", "name": "Java", "technologyVersion": "Java 21"}],
            "concepts": [{"code": "J_ONE", "name": "One", "description": "Meaning", "topicCode": "JAVA"}],
            "documents": [{"key": "doc", "topicCode": "JAVA", "title": "Doc", "content": "Independent summary", "technologyVersion": "Java 21", "sourceIds": ["s"], "conceptCodes": ["J_ONE"]}],
            "questions": [{"key": "q", "topicCode": "JAVA", "difficulty": "BASIC", "content": "A question?", "referenceAnswer": "An answer", "documentKey": "doc", "sourceRefs": [{"sourceId": "s", "locator": "section 1"}], "concepts": [{"code": "J_ONE", "weight": "1.00", "required": True}]}],
        }

    def test_valid_draft_is_accepted(self):
        self.assertEqual(validate_bundle(self.bundle, [], minimum_questions=1)["questionCount"], 1)

    def test_missing_required_concept_evidence_is_rejected(self):
        self.bundle["documents"][0]["conceptCodes"] = []
        with self.assertRaisesRegex(ValueError, "coverage"):
            validate_bundle(self.bundle, [], minimum_questions=1)

    def test_invalid_weight_is_rejected(self):
        self.bundle["questions"][0]["concepts"][0]["weight"] = "0.9"
        with self.assertRaisesRegex(ValueError, "weight"):
            validate_bundle(self.bundle, [], minimum_questions=1)

    def test_publication_is_never_inferred_from_generated_content(self):
        self.bundle["status"] = "PUBLISHED"
        with self.assertRaisesRegex(ValueError, "DRAFT"):
            validate_bundle(self.bundle, [], minimum_questions=1)

    def test_missing_source_is_rejected(self):
        self.bundle["questions"][0]["sourceRefs"][0]["sourceId"] = "unknown"
        with self.assertRaisesRegex(ValueError, "source"):
            validate_bundle(self.bundle, [], minimum_questions=1)

    def test_exact_reference_question_is_rejected_despite_spacing(self):
        with self.assertRaisesRegex(ValueError, "duplicate"):
            validate_bundle(self.bundle, [{"id": "gold", "content": " A  question? "}], minimum_questions=1)

    def test_missing_topic_minimum_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "minimum"):
            validate_bundle(self.bundle, [], minimum_questions=5)

    def test_concept_from_another_topic_is_rejected(self):
        self.bundle["topics"].append({"code": "OS", "name": "OS", "technologyVersion": "OS"})
        self.bundle["concepts"][0]["topicCode"] = "OS"
        with self.assertRaisesRegex(ValueError, "topic"):
            validate_bundle(self.bundle, [], minimum_questions=1)

    def test_duplicate_question_inside_bundle_is_rejected(self):
        other = copy.deepcopy(self.bundle["questions"][0]); other["key"] = "other"
        self.bundle["questions"].append(other)
        with self.assertRaisesRegex(ValueError, "duplicate"):
            validate_bundle(self.bundle, [], minimum_questions=1)


class ResumeTest(unittest.TestCase):
    def test_matching_draft_is_reused_without_another_post(self):
        writes = []
        row = {"id": 7, "content": "Draft", "topicId": 1, "referenceAnswer": "Own answer", "status": "DRAFT"}
        actual = reuse_or_create([row], {"content": "Draft", "topicId": 1, "referenceAnswer": "Own answer"}, ["topicId", "content"], lambda payload: writes.append(payload), draft_only=True)
        self.assertEqual(actual["id"], 7)
        self.assertEqual(writes, [])

    def test_user_edit_is_not_overwritten(self):
        row = {"id": 7, "content": "Draft", "topicId": 1, "referenceAnswer": "User edit", "status": "DRAFT"}
        with self.assertRaisesRegex(ValueError, "conflict"):
            reuse_or_create([row], {"content": "Draft", "topicId": 1, "referenceAnswer": "Original"}, ["topicId", "content"], lambda _: self.fail("Must not write"), draft_only=True)

    def test_published_match_is_not_modified_or_duplicated(self):
        with self.assertRaisesRegex(ValueError, "DRAFT"):
            reuse_or_create([{"id": 7, "content": "Same", "status": "PUBLISHED"}], {"content": "Same"}, ["content"], lambda _: self.fail("Must not write"), draft_only=True)

    def test_ambiguous_matches_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "ambiguous"):
            reuse_or_create([{"id": 1, "code": "SAME"}, {"id": 2, "code": "SAME"}], {"code": "SAME"}, ["code"], lambda _: self.fail("Must not write"))

    def test_missing_record_is_created_once(self):
        writes = []
        def create(payload):
            writes.append(payload)
            return {"id": 3, **payload}
        result = reuse_or_create([], {"code": "NEW"}, ["code"], create)
        self.assertEqual(result["id"], 3)
        self.assertEqual(writes, [{"code": "NEW"}])


if __name__ == "__main__":
    unittest.main()
