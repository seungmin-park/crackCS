"""Offline experiment exports must not leak their answer labels into model input."""
import json
import hashlib
import subprocess
import sys
import unittest

import bundle


class ReferenceStandardTest(unittest.TestCase):
    def setUp(self):
        self.artifacts = {"questions.jsonl": b"frozen reference"}
        self.manifest = {
            "version": "1.0.0", "status": "FINALIZED",
            "reviewer": {"kind": "HUMAN", "name": "PROJECT_OWNER"},
            "independentHumanReview": True,
            "artifacts": {name: hashlib.sha256(body).hexdigest()
                          for name, body in self.artifacts.items()},
            "questionReviews": [{"questionId": "CS-01", "sourceIds": ["C02"],
                                 "decision": "ACCEPT", "note": "공유 자원과 개별 상태 구분 확인"}],
        }

    def test_accepts_frozen_reference_without_claiming_human_review(self):
        bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, {"C02"})

    def test_rejects_changed_content_until_review_is_updated(self):
        self.artifacts["questions.jsonl"] = b"changed answer"
        with self.assertRaisesRegex(ValueError, "Reference artifact changed"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, {"C02"})

    def test_rejects_missing_question_review(self):
        with self.assertRaisesRegex(ValueError, "Review coverage"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01", "CS-02"}, {"C02"})

    def test_rejects_unknown_review_source(self):
        with self.assertRaisesRegex(ValueError, "Review source"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, set())

    def test_rejects_missing_artifact_from_manifest(self):
        self.manifest["artifacts"] = {}
        with self.assertRaisesRegex(ValueError, "Reference artifact coverage"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, {"C02"})

    def test_rejects_unresolved_review(self):
        self.manifest["questionReviews"][0]["decision"] = "UNRESOLVED"
        with self.assertRaisesRegex(ValueError, "Unresolved reference review"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, {"C02"})

    def test_rejects_non_final_manifest(self):
        self.manifest["status"] = "DRAFT"
        with self.assertRaisesRegex(ValueError, "Reference not finalized"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, {"C02"})

    def test_rejects_reference_without_independent_human_review(self):
        self.manifest["independentHumanReview"] = False
        with self.assertRaisesRegex(ValueError, "Independent human review required"):
            bundle.validate_reference_standard(self.manifest, self.artifacts, {"CS-01"}, {"C02"})


class HumanReviewMetadataTest(unittest.TestCase):
    def test_accepts_project_owner_review_metadata(self):
        rows = [{"reviewStatus": "REVIEWED", "reviewedBy": "PROJECT_OWNER",
                 "reviewedAt": "2026-09-21"}]

        bundle.validate_human_review(rows)

    def test_rejects_pending_review_metadata(self):
        rows = [{"reviewStatus": "PENDING_INDEPENDENT_REVIEW", "reviewedBy": None,
                 "reviewedAt": None}]

        with self.assertRaisesRegex(ValueError, "Independent human review metadata"):
            bundle.validate_human_review(rows)

    def test_rejects_authorship_provenance_in_reviewed_data(self):
        rows = [{"authorship": "UNSPECIFIED", "reviewStatus": "REVIEWED",
                 "reviewedBy": "PROJECT_OWNER", "reviewedAt": "2026-09-21"}]

        with self.assertRaisesRegex(ValueError, "Authorship provenance must not be stored"):
            bundle.validate_human_review(rows)


class RenderOutputTest(unittest.TestCase):
    def test_render_path_is_under_build_reports(self):
        self.assertTrue(hasattr(bundle, "REPOSITORY_ROOT"))
        self.assertTrue(hasattr(bundle, "REFERENCE_ROOT"))
        self.assertTrue(hasattr(bundle, "RENDER_PATH"))
        expected = bundle.REPOSITORY_ROOT / "build/reports/evaluation/reference-v1/questions.md"

        self.assertEqual(bundle.RENDER_PATH, expected)
        self.assertFalse((bundle.REFERENCE_ROOT / "questions.md").exists())

    def test_cli_validation_does_not_require_rendered_markdown(self):
        result = subprocess.run(
            [sys.executable, str(bundle.BASE / "bundle.py")],
            check=True,
            capture_output=True,
            text=True,
        )

        self.assertEqual(json.loads(result.stdout)["status"], "PASS")


class ExperimentExportTest(unittest.TestCase):
    def setUp(self):
        self.questions = bundle.read_jsonl("questions.jsonl")
        self.goldens = bundle.read_jsonl("golden-set.jsonl")
        self.documents = bundle.read_jsonl("knowledge-documents.jsonl")

    def test_exports_adapter_input_without_expected_labels(self):
        rows = bundle.experiment_rows(self.questions, self.goldens, self.documents, "inputs")
        self.assertEqual(len(rows), 240)
        first = rows[0]
        self.assertEqual(first["caseId"], "CS-01-G01")
        request = first["request"]
        self.assertEqual(set(request), {
            "topicId", "questionContent", "referenceAnswer", "answerContent", "concepts", "evidence"
        })
        self.assertEqual(request["answerContent"], self.goldens[0]["answer"])
        self.assertEqual(len(request["concepts"]), 2)
        self.assertEqual(len(request["evidence"]), 2)
        self.assertNotIn("expectedVerdict", json.dumps(rows))
        self.assertNotIn("caseType", json.dumps(rows))
        self.assertNotIn("expectedReason", json.dumps(rows))

    def test_preserves_missing_evidence_control(self):
        rows = bundle.experiment_rows(self.questions, self.goldens, self.documents, "inputs")
        first, missing = rows[0]["request"], rows[3]["request"]
        self.assertEqual(first["answerContent"], missing["answerContent"])
        self.assertEqual(len(missing["evidence"]), 1)
        self.assertEqual(missing["evidence"], first["evidence"][:1])

    def test_labels_stay_separate_and_identifiers_are_stable(self):
        inputs = bundle.experiment_rows(self.questions, self.goldens, self.documents, "inputs")
        labels = bundle.experiment_rows(self.questions, self.goldens, self.documents, "labels")
        self.assertEqual(labels[0]["expectedVerdict"], "CORRECT")
        self.assertEqual(labels[3]["expectedVerdict"], "NEEDS_REVIEW")
        self.assertEqual(labels[0]["providedEvidenceIds"],
                         [row["chunkId"] for row in inputs[0]["request"]["evidence"]])
        reversed_inputs = bundle.experiment_rows(
            list(reversed(self.questions)), self.goldens, list(reversed(self.documents)), "inputs")
        self.assertEqual(inputs, reversed_inputs)

    def test_rejects_unknown_evidence_instead_of_exporting_partial_input(self):
        self.goldens[0]["providedEvidenceIds"] = ["MISSING"]
        with self.assertRaisesRegex(ValueError, "Unknown evidence"):
            bundle.experiment_rows(self.questions, self.goldens, self.documents, "inputs")

    def test_rejects_unknown_question(self):
        self.goldens[0]["questionId"] = "MISSING"
        with self.assertRaisesRegex(ValueError, "Unknown question"):
            bundle.experiment_rows(self.questions, self.goldens, self.documents, "inputs")

    def test_retrieval_queries_keep_missing_evidence_controls_out_of_search_labels(self):
        rows = bundle.experiment_rows(self.questions, self.goldens, self.documents, "retrieval")
        self.assertEqual(len(rows), 180)
        self.assertNotIn("CS-01-G04", [row["caseId"] for row in rows])
        self.assertEqual(rows[0]["relevantEvidenceKeys"], ["CS-01-K1", "CS-01-K2"])
        self.assertEqual(rows[0]["topicKey"], "CS")

    def test_rejects_evidence_belonging_to_another_question(self):
        self.goldens[0]["providedEvidenceIds"] = ["CS-02-K1"]
        with self.assertRaisesRegex(ValueError, "Evidence scope"):
            bundle.experiment_rows(self.questions, self.goldens, self.documents, "inputs")

    def test_exported_corpus_contains_retrieval_evidence_and_matching_offsets(self):
        corpus = bundle.experiment_rows(self.questions, self.goldens, self.documents, "corpus")
        self.assertEqual(len(corpus), 60)
        first = next(row for row in corpus if row["documentKey"] == "CS-01-DOC")
        self.assertEqual(len(first["chunks"]), 2)
        second = first["chunks"][1]
        encoded = first["content"].encode("utf-16-le")
        actual = encoded[second["startOffset"] * 2:second["endOffset"] * 2].decode("utf-16-le")
        self.assertEqual(actual, second["content"])

    def test_cli_exports_only_jsonl_and_does_not_send_labels_with_input(self):
        result = subprocess.run([sys.executable, str(bundle.BASE / "bundle.py"), "--export", "inputs"],
                                check=True, capture_output=True, text=True)
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual(len(rows), 240)
        self.assertTrue(all(set(row) == {"caseId", "request"} for row in rows))

    def test_split_selection_keeps_question_cases_together(self):
        development = bundle.select_questions(self.questions, "development")
        evaluation = bundle.select_questions(self.questions, "evaluation-candidate")
        development_ids = {row["id"] for row in development}
        evaluation_ids = {row["id"] for row in evaluation}
        self.assertFalse(development_ids & evaluation_ids)
        self.assertEqual(development_ids | evaluation_ids, {row["id"] for row in self.questions})
        self.assertIn("JAVA-11", evaluation_ids)
        self.assertIn("JAVA-12", evaluation_ids)
        self.assertNotIn("JAVA-11", development_ids)

    def test_cli_split_has_all_four_cases_for_each_selected_question(self):
        result = subprocess.run([sys.executable, str(bundle.BASE / "bundle.py"), "--export", "labels",
                                 "--split", "evaluation-candidate"],
                                check=True, capture_output=True, text=True)
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        counts = bundle.Counter(row["questionId"] for row in rows)
        self.assertTrue(counts)
        self.assertEqual(set(counts.values()), {4})
        self.assertTrue(all(not row["benchmarkEligible"] for row in rows))


if __name__ == "__main__":
    unittest.main()
