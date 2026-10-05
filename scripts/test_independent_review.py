"""Independent review contracts; tiny invented inputs are tooling tests only."""
import copy
from datetime import datetime, timezone, timedelta
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from independent_review import prepare_review, freeze_review, compare_results, review_page, save_human_review


def sample():
    return {"version": "test-v1", "topicCode": "OPERATING_SYSTEM", "sampling": {
        "population": "Tooling test only", "method": "Invented test input", "limitations": "Not learners"},
        "cases": [{"caseId": "TEST-1", "provenance": {"kind": "UNCONFIRMED", "source": "test DB", "sourceAnswerId": 1},
                   "question": {"id": 1, "version": 1, "content": "What changes?", "referenceAnswer": "The program changes.",
                                "concepts": [{"code": "EXEC", "name": "exec", "description": "Program replacement", "weight": 1.0, "required": True}]},
                   "answer": "The program changes."}]}


def reviewed(samples):
    labels = prepare_review(samples)["labels"]
    labels.update({"status": "REVIEWED", "reviewer": "Human reviewer", "reviewedAt": (datetime.now(timezone.utc) - timedelta(minutes=1)).isoformat(),
                   "blindReviewConfirmed": True})
    labels["cases"][0].update({"sourceConfirmedHuman": True, "sourceEvidence": "Reviewer confirms learner origin",
                              "category": "SHORT", "expectedVerdict": "CORRECT", "reason": "Core meaning complete",
                              "expectedConcepts": {"EXEC": {"verdict": "CORRECT", "reason": "Replacement explained"}}})
    return labels


def observed(verdict="CORRECT", status="EVALUATED"):
    return [{"caseId": "TEST-1", "status": status, "verdict": verdict, "observedAt": datetime.now(timezone.utc).isoformat(),
             "concepts": {"EXEC": verdict}, "modelName": "test-model", "evaluatorVersion": "test-evaluator", "evidenceCount": 1}]


class PrepareReviewTest(unittest.TestCase):
    def test_blinded_packet_has_no_model_outputs_and_unfilled_labels(self):
        samples = sample()
        samples["cases"][0]["evaluation"] = {"verdict": "INCORRECT", "feedback": "Hidden model feedback"}
        packet = prepare_review(samples)
        self.assertEqual(set(packet), {"inputs", "labels", "reviewMarkdown"})
        self.assertNotIn("evaluation", packet["inputs"]["cases"][0])
        self.assertNotIn("Hidden model feedback", packet["reviewMarkdown"])
        self.assertIsNone(packet["labels"]["cases"][0]["expectedVerdict"])

    def test_empty_collection_is_rejected(self):
        samples = sample()
        samples["cases"] = []
        with self.assertRaisesRegex(ValueError, "empty"):
            prepare_review(samples)

    def test_duplicate_case_is_rejected(self):
        samples = sample()
        samples["cases"].append(copy.deepcopy(samples["cases"][0]))
        with self.assertRaisesRegex(ValueError, "duplicate"):
            prepare_review(samples)

    def test_question_weights_are_checked(self):
        samples = sample()
        samples["cases"][0]["question"]["concepts"][0]["weight"] = 0.5
        with self.assertRaisesRegex(ValueError, "weight"):
            prepare_review(samples)


class FreezeReviewTest(unittest.TestCase):
    def test_unreviewed_labels_cannot_be_frozen(self):
        samples = sample()
        with self.assertRaisesRegex(ValueError, "human review"):
            freeze_review(samples, prepare_review(samples)["labels"])

    def test_complete_human_review_is_bound_to_inputs_and_labels(self):
        samples = sample()
        labels = reviewed(samples)
        frozen = freeze_review(samples, labels)
        self.assertEqual(frozen["status"], "FROZEN")
        self.assertEqual(len(frozen["inputsSha256"]), 64)
        self.assertEqual(len(frozen["labelsSha256"]), 64)

    def test_agent_authored_answers_cannot_become_independent_learners(self):
        samples = sample()
        samples["cases"][0]["provenance"]["kind"] = "AGENT_AUTHORED"
        with self.assertRaisesRegex(ValueError, "agent"):
            freeze_review(samples, reviewed(samples))

    def test_unconfirmed_learner_origin_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        labels["cases"][0]["sourceConfirmedHuman"] = False
        with self.assertRaisesRegex(ValueError, "origin"):
            freeze_review(samples, labels)

    def test_missing_concept_verdict_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        labels["cases"][0]["expectedConcepts"] = {}
        with self.assertRaisesRegex(ValueError, "concept"):
            freeze_review(samples, labels)

    def test_review_after_seeing_model_result_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        labels["blindReviewConfirmed"] = False
        with self.assertRaisesRegex(ValueError, "blind"):
            freeze_review(samples, labels)

    def test_missing_reason_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        labels["cases"][0]["expectedConcepts"]["EXEC"]["reason"] = ""
        with self.assertRaisesRegex(ValueError, "reason"):
            freeze_review(samples, labels)

    def test_review_timestamp_requires_timezone(self):
        samples = sample()
        labels = reviewed(samples)
        labels["reviewedAt"] = "2026-10-05T17:00:00"
        with self.assertRaisesRegex(ValueError, "timezone"):
            freeze_review(samples, labels)


class CompareReviewTest(unittest.TestCase):
    def test_matching_verdicts_report_counts_and_uncalculable_denominator(self):
        samples = sample()
        labels = reviewed(samples)
        report = compare_results(samples, labels, freeze_review(samples, labels), observed())
        self.assertEqual(report["overallAgreement"], {"matched": 1, "total": 1, "rate": 1.0})
        self.assertEqual(report["falseCorrect"], {"count": 0, "total": 0, "rate": None})
        self.assertEqual(report["conceptAgreement"]["matched"], 1)
        self.assertEqual(report["assessment"], "DESCRIPTIVE_ONLY")

    def test_answer_change_after_freeze_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        frozen = freeze_review(samples, labels)
        samples["cases"][0]["answer"] = "Edited after freeze"
        with self.assertRaisesRegex(ValueError, "changed"):
            compare_results(samples, labels, frozen, observed())

    def test_label_change_after_freeze_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        frozen = freeze_review(samples, labels)
        labels["cases"][0]["expectedVerdict"] = "INCORRECT"
        with self.assertRaisesRegex(ValueError, "changed"):
            compare_results(samples, labels, frozen, observed())

    def test_missing_results_stay_in_denominator(self):
        samples = sample()
        labels = reviewed(samples)
        report = compare_results(samples, labels, freeze_review(samples, labels), [])
        self.assertEqual(report["overallAgreement"], {"matched": 0, "total": 1, "rate": 0.0})
        self.assertEqual(report["missingCount"], 1)
        self.assertEqual(report["conceptAgreement"]["total"], 1)

    def test_failed_result_is_not_dropped_or_counted_correct(self):
        samples = sample()
        labels = reviewed(samples)
        report = compare_results(samples, labels, freeze_review(samples, labels), observed(None, "FAILED"))
        self.assertEqual(report["failedCount"], 1)
        self.assertEqual(report["overallAgreement"]["rate"], 0.0)
        self.assertEqual(report["binaryAgreement"]["total"], 1)

    def test_false_correct_denominator_only_uses_expected_incorrect(self):
        samples = sample()
        labels = reviewed(samples)
        labels["cases"][0]["expectedVerdict"] = "INCORRECT"
        report = compare_results(samples, labels, freeze_review(samples, labels), observed())
        self.assertEqual(report["falseCorrect"], {"count": 1, "total": 1, "rate": 1.0})

    def test_expected_needs_review_is_reported_separately(self):
        samples = sample()
        labels = reviewed(samples)
        labels["cases"][0]["expectedVerdict"] = "NEEDS_REVIEW"
        labels["cases"][0]["expectedConcepts"]["EXEC"]["verdict"] = "NEEDS_REVIEW"
        report = compare_results(samples, labels, freeze_review(samples, labels), observed("NEEDS_REVIEW", "NEEDS_REVIEW"))
        self.assertIsNone(report["overallAgreement"]["rate"])
        self.assertEqual(report["needsReviewAgreement"], {"matched": 1, "total": 1, "rate": 1.0})

    def test_duplicate_observed_result_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        with self.assertRaisesRegex(ValueError, "duplicate"):
            compare_results(samples, labels, freeze_review(samples, labels), observed() + observed())

    def test_unknown_observed_case_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        results = observed()
        results[0]["caseId"] = "UNKNOWN"
        with self.assertRaisesRegex(ValueError, "unknown"):
            compare_results(samples, labels, freeze_review(samples, labels), results)

    def test_existing_model_output_before_human_freeze_is_rejected(self):
        samples = sample()
        labels = reviewed(samples)
        old_results = observed()
        frozen = freeze_review(samples, labels)
        with self.assertRaisesRegex(ValueError, "predates"):
            compare_results(samples, labels, frozen, old_results)


class HumanReviewPageTest(unittest.TestCase):
    def test_user_answer_html_is_escaped(self):
        samples = sample()
        samples["cases"][0]["answer"] = "</script><img src=x onerror=alert(1)>"
        page = review_page(samples, "test-nonce")
        self.assertIn("&lt;img", page)
        self.assertNotIn("<img src=x", page)

    def test_form_has_no_preselected_verdict_or_attestation(self):
        page = review_page(sample(), "test-nonce")
        self.assertIn('<option value="">판정 선택</option>', page)
        self.assertNotIn(" checked", page)
        self.assertIn("sourceConfirmedHuman", page)
        self.assertIn("blindReviewConfirmed", page)

    def test_unreviewed_form_cannot_write_a_manifest(self):
        samples = sample()
        with TemporaryDirectory() as directory:
            output = Path(directory)
            with self.assertRaisesRegex(ValueError, "human review"):
                save_human_review(samples, prepare_review(samples)["labels"], output)
            self.assertEqual(list(output.iterdir()), [])

    def test_saved_review_is_frozen_and_never_overwritten(self):
        samples = sample()
        labels = reviewed(samples)
        with TemporaryDirectory() as directory:
            output = Path(directory)
            save_human_review(samples, labels, output)
            self.assertTrue((output / "labels-final.json").exists())
            self.assertTrue((output / "frozen.json").exists())
            with self.assertRaisesRegex(ValueError, "already saved"):
                save_human_review(samples, labels, output)


if __name__ == "__main__":
    unittest.main()
