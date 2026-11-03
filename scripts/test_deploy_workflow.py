"""Contract checks for the Production CD workflow structure."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parents[1] / ".github" / "workflows" / "deploy.yml"


def deploy_job_lines(lines):
    start = lines.index("  deploy:")
    job = []
    for line in lines[start + 1:]:
        if line and not line.startswith("    "):
            break
        job.append(line)
    return job


class DeployWorkflowConcurrencyTest(unittest.TestCase):
    def setUp(self):
        self.lines = WORKFLOW.read_text(encoding="utf-8").splitlines()

    def test_ineligible_runs_do_not_join_production_group_at_workflow_level(self):
        # A workflow-level group would let skipped fork-triggered runs cancel a pending deployment.
        self.assertNotIn("concurrency:", self.lines)

    def test_deploy_job_owns_the_production_concurrency_group(self):
        job = deploy_job_lines(self.lines)
        self.assertIn("    concurrency:", job)
        block = job[job.index("    concurrency:") + 1:job.index("    concurrency:") + 3]
        self.assertEqual(block, ["      group: crackcs-production", "      cancel-in-progress: false"])


if __name__ == "__main__":
    unittest.main()
