import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


CHECKER = Path(__file__).with_name('check_test_reports.py')


class TestReportCheckTest(unittest.TestCase):
    def setUp(self):
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        self.root = Path(temporary_directory.name)
        catalog = self.root / 'docs/engineering/required-tests.json'
        catalog.parent.mkdir(parents=True)
        catalog.write_text(json.dumps({
            'backend': ['example.RequiredTest'],
            'postgres': ['example.RequiredTest'],
            'frontend': ['src/required.test.ts'],
        }))

    def write_java_report(self, content, task='test'):
        path = self.root / f'build/test-results/{task}/TEST-example.RequiredTest.xml'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        return path

    def write_frontend_report(self, result):
        path = self.root / 'front/test-results/vitest.json'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(result))
        return path

    def run_check(self, kind='backend'):
        return subprocess.run([sys.executable, str(CHECKER), kind, '--root', str(self.root)],
                              capture_output=True, text=True, check=False)

    def test_successful_required_java_test_is_accepted(self):
        self.write_java_report('<testsuite name="example.RequiredTest" tests="1" failures="0" errors="0" skipped="0"><testcase name="works"/></testsuite>')
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn('1 tests', result.stdout)

    def test_missing_reports_are_rejected(self):
        result = self.run_check()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('report', result.stdout)

    def test_other_passing_test_does_not_replace_required_test(self):
        self.write_java_report('<testsuite name="example.OtherTest" tests="1" failures="0" errors="0" skipped="0"><testcase name="works"/></testsuite>')
        result = self.run_check()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('example.RequiredTest', result.stdout)

    def test_zero_test_suite_is_rejected(self):
        self.write_java_report('<testsuite name="example.RequiredTest" tests="0" failures="0" errors="0" skipped="0"/>')
        result = self.run_check()
        self.assertNotEqual(result.returncode, 0)

    def test_java_failure_error_and_skip_are_rejected(self):
        for outcome in ('failure', 'error', 'skipped'):
            with self.subTest(outcome=outcome):
                self.write_java_report(f'<testsuite name="example.RequiredTest" tests="1" failures="0" errors="0" skipped="0"><testcase name="works"><{outcome}/></testcase></testsuite>')
                result = self.run_check()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(outcome, result.stdout)

    def test_malformed_xml_is_rejected(self):
        self.write_java_report('<testsuite')
        result = self.run_check()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('invalid', result.stdout)

    def test_postgres_uses_separate_task_results(self):
        self.write_java_report('<testsuite name="example.RequiredTest" tests="1" failures="0" errors="0" skipped="0"><testcase name="works"/></testsuite>')
        result = self.run_check('postgres')
        self.assertNotEqual(result.returncode, 0)
        self.write_java_report('<testsuite name="example.RequiredTest" tests="1" failures="0" errors="0" skipped="0"><testcase name="works"/></testsuite>', 'postgresTest')
        self.assertEqual(self.run_check('postgres').returncode, 0)

    def test_successful_required_frontend_file_is_accepted(self):
        self.write_frontend_report({'success': True, 'numTotalTests': 1, 'numPassedTests': 1,
            'testResults': [{'name': str(self.root / 'front/src/required.test.ts'), 'status': 'passed',
                             'assertionResults': [{'status': 'passed'}]}]})
        result = self.run_check('frontend')
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_missing_required_frontend_file_is_rejected(self):
        self.write_frontend_report({'success': True, 'numTotalTests': 1, 'numPassedTests': 1,
            'testResults': [{'name': str(self.root / 'front/src/other.test.ts'), 'status': 'passed',
                             'assertionResults': [{'status': 'passed'}]}]})
        result = self.run_check('frontend')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('src/required.test.ts', result.stdout)

    def test_pending_frontend_assertion_is_rejected_even_when_run_succeeds(self):
        self.write_frontend_report({'success': True, 'numTotalTests': 2, 'numPassedTests': 1,
            'testResults': [{'name': str(self.root / 'front/src/required.test.ts'), 'status': 'passed',
                             'assertionResults': [{'status': 'passed'}, {'status': 'pending'}]}]})
        result = self.run_check('frontend')
        self.assertNotEqual(result.returncode, 0)

    def test_empty_required_catalog_is_rejected(self):
        catalog = self.root / 'docs/engineering/required-tests.json'
        catalog.write_text(json.dumps({'backend': [], 'frontend': [], 'postgres': []}))
        result = self.run_check()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('catalog', result.stdout)


if __name__ == '__main__':
    unittest.main()
