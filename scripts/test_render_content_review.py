"""Exercise the review renderer CLI in disposable directories."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class RenderContentReviewTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.script = self.root / 'scripts/render_content_review.py'
        self.script.parent.mkdir()
        shutil.copyfile(ROOT / 'scripts/render_content_review.py', self.script)
        self.bundle = json.loads((ROOT / 'docs/content/initial-v1/bundle.json').read_text())
        self.default = self.root / 'docs/content/initial-v1/bundle.json'
        self.default.parent.mkdir(parents=True)
        self.default.write_text(json.dumps(self.bundle, ensure_ascii=False))

    def run_renderer(self, *arguments):
        return subprocess.run([sys.executable, str(self.script), *map(str, arguments)],
                              cwd=self.root, capture_output=True, text=True)

    def test_custom_bundle_and_output_leave_default_review_untouched(self):
        self.bundle['version'] = 'custom-v1'
        self.bundle['questions'][0]['content'] = '선택한 묶음의 새 문항'
        source = self.root / 'custom.json'
        source.write_text(json.dumps(self.bundle, ensure_ascii=False))
        target = self.root / 'selected-review.md'
        default_review = self.default.with_name('review.md')
        default_review.write_text('기존 검수 기록')

        result = self.run_renderer('--bundle', source, '--output', target)

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(target.exists(), '선택한 출력 경로에 검수 자료가 생성되어야 함')
        self.assertIn('# custom-v1 검수용 읽기 자료', target.read_text())
        self.assertIn('선택한 묶음의 새 문항', target.read_text())
        self.assertEqual('기존 검수 기록', default_review.read_text())

    def test_default_command_still_renders_initial_bundle(self):
        result = self.run_renderer()

        self.assertEqual(0, result.returncode, result.stderr)
        review = self.default.with_name('review.md').read_text()
        self.assertIn('# initial-v1 검수용 읽기 자료', review)
        self.assertIn(self.bundle['questions'][0]['content'], review)
        self.assertIn('python3 scripts/render_content_review.py`', review)

    def test_output_cannot_overwrite_authoritative_bundle(self):
        original = self.default.read_bytes()

        result = self.run_renderer('--bundle', self.default, '--output', self.default)

        self.assertNotEqual(0, result.returncode)
        self.assertEqual(original, self.default.read_bytes())

    def test_custom_bundle_defaults_output_to_its_directory_and_renders_diagnostics(self):
        self.bundle['version'] = 'a1-v1'
        question = self.bundle['questions'][0]
        code = question['concepts'][0]['code']
        question['reviewNotes']['rubric'] = [{
            'code': code, 'fullCredit': '조건과 이유를 모두 설명',
            'partialCredit': '조건 일부 생략', 'zeroCredit': '핵심을 반대로 설명',
            'needsReview': '근거가 없어 대조 불가',
        }]
        question['reviewNotes']['diagnosticCases'] = [{
            'id': 'case-1', 'learnerAnswer': '부분 답안 사례',
            'evidenceMode': 'DOCUMENT', 'expectedOverall': 'PARTIALLY_CORRECT',
            'expectedConcepts': {code: 'PARTIALLY_CORRECT'},
            'reason': '필수 조건 하나가 빠짐',
        }]
        source = self.root / 'new-content/bundle.json'
        source.parent.mkdir()
        source.write_text(json.dumps(self.bundle, ensure_ascii=False))

        result = self.run_renderer('--bundle', source)

        self.assertEqual(0, result.returncode, result.stderr)
        target = source.with_name('review.md')
        self.assertTrue(target.exists(), '다른 묶음의 읽기 자료를 덮어쓰면 안 됨')
        review = target.read_text()
        for expected in ('조건과 이유를 모두 설명', '근거가 없어 대조 불가',
                         '부분 답안 사례', 'PARTIALLY_CORRECT', code, '필수 조건 하나가 빠짐'):
            with self.subTest(expected=expected):
                self.assertIn(expected, review)
        self.assertIn('작성자가 만든 진단 사례', review)
        self.assertIn('모델 실행 결과가 아님', review)


if __name__ == '__main__':
    unittest.main()
