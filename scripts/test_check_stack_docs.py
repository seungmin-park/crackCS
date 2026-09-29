import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


REPOSITORY = Path(__file__).resolve().parents[1]
CHECKER = REPOSITORY / "scripts/check_stack_docs.py"
SOURCES = (
    "build.gradle",
    "gradle/wrapper/gradle-wrapper.properties",
    "front/.nvmrc",
    "front/package.json",
    "front/package-lock.json",
    "docs/engineering/stack-docs.json",
    "docs/engineering/stack-docs.md",
)


class StackDocsCheckTest(unittest.TestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary_directory.cleanup)
        self.root = Path(self.temporary_directory.name)
        for source in SOURCES:
            destination = self.root / source
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(REPOSITORY / source, destination)

    def check(self):
        return subprocess.run(
            [sys.executable, str(CHECKER), "--root", str(self.root)],
            text=True, capture_output=True, check=False,
        )

    def test_current_stack_docs_match_manifests(self):
        result = self.check()

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("PASS", result.stdout)

    def test_lockfile_upgrade_requires_document_review(self):
        path = self.root / "front/package-lock.json"
        lockfile = json.loads(path.read_text())
        lockfile["packages"]["node_modules/vue"]["version"] = "3.5.99"
        path.write_text(json.dumps(lockfile))

        result = self.check()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("vue", result.stdout + result.stderr)
        self.assertIn("3.5.99", result.stdout + result.stderr)

    def test_unknown_document_host_is_rejected(self):
        path = self.root / "docs/engineering/stack-docs.json"
        catalog = json.loads(path.read_text())
        catalog["components"][0]["officialUrl"] = "https://example.com/unverified"
        path.write_text(json.dumps(catalog))

        result = self.check()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("example.com", result.stdout + result.stderr)

    def test_boot_upgrade_requires_managed_document_review(self):
        path = self.root / "build.gradle"
        path.write_text(path.read_text().replace("version '4.1.1'", "version '4.1.2'", 1))

        result = self.check()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("spring-boot", result.stdout + result.stderr)

    def test_human_index_must_follow_catalog(self):
        path = self.root / "docs/engineering/stack-docs.md"
        path.write_text(path.read_text().replace("Vue 3.5.42", "Vue 3.5.41"))

        result = self.check()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("vue", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
