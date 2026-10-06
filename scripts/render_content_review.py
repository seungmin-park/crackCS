#!/usr/bin/env python3
"""Render the human reading copy; bundle.json remains authoritative."""
import argparse
import hashlib
import json
import os
import shlex
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--bundle', type=Path, default=root / 'docs/content/initial-v1/bundle.json')
parser.add_argument('--output', type=Path)
args = parser.parse_args()
source = args.bundle.resolve()
target = args.output.resolve() if args.output else source.with_name('review.md')
if target == source:
    parser.error('--output must differ from the authoritative bundle')
bundle_link = Path(os.path.relpath(source, target.parent)).as_posix()
readme_link = Path(os.path.relpath(source.with_name('README.md'), target.parent)).as_posix()
regenerate = 'python3 scripts/render_content_review.py'
if source != root / 'docs/content/initial-v1/bundle.json' or args.output:
    regenerate += ' --bundle ' + shlex.quote(os.path.relpath(source, root))
    if args.output:
        regenerate += ' --output ' + shlex.quote(os.path.relpath(target, root))
bundle = json.loads(source.read_text())
concepts = {item['code']: item for item in bundle['concepts']}
sources = {item['id']: item for item in bundle['sources']}
lines = [
    f"# {bundle['version']} 검수용 읽기 자료", '',
    f"> 자동 생성 읽기 사본. 수정 기준은 [bundle.json]({bundle_link if ' ' not in bundle_link else '<' + bundle_link + '>'}). 사람 검수·공개 승인 대기.", '',
    f"- 원본 SHA-256: `{hashlib.sha256(source.read_bytes()).hexdigest()}`",
    f'- 재생성: 저장소 루트에서 `{regenerate}`',
    f'- 검수 절차·승인 기록: [콘텐츠 안내]({readme_link}#사람-검수공개-순서)',
    '- 이 자료를 읽었다는 사실만으로 앱의 검수·공개 상태가 바뀌지 않음', '',
    '## 문항과 판정 기준', '',
]
for question in bundle['questions']:
    lines += [f"### {question['key']} · {question['difficulty']}", '', question['content'], '', '**모범 답안**', '', question['referenceAnswer'], '', '**필수 개념**', '']
    for criterion in question['concepts']:
        concept = concepts[criterion['code']]
        lines += [f"- {concept['name']} · 가중치 {criterion['weight']}: {concept['description']}"]
    if question['reviewNotes'].get('rubric'):
        lines += ['', '**개념별 판정 경계**', '']
        for rubric in question['reviewNotes']['rubric']:
            lines += [f"- `{rubric['code']}`", f"  - CORRECT: {rubric['fullCredit']}", f"  - PARTIALLY_CORRECT: {rubric['partialCredit']}", f"  - INCORRECT: {rubric['zeroCredit']}", f"  - NEEDS_REVIEW: {rubric['needsReview']}"]
    if question['reviewNotes'].get('diagnosticCases'):
        lines += ['', '**작성자가 만든 진단 사례 — 독립 평가 표본·모델 실행 결과가 아님**', '']
        for case in question['reviewNotes']['diagnosticCases']:
            verdicts = ', '.join(f'{code}: {verdict}' for code, verdict in case['expectedConcepts'].items())
            lines += [f"- `{case['id']}` · 근거 조건 `{case['evidenceMode']}` · 예상 전체 `{case['expectedOverall']}`", f"  - 답변: {case['learnerAnswer']}", f"  - 예상 개념 판정: {verdicts}", f"  - 이유: {case['reason']}"]
    lines += ['', f"**혼동 주의:** {question['reviewNotes']['commonMistake']}", '', '**출처 대조 위치**', '']
    for ref in question['sourceRefs']:
        source_info = sources[ref['sourceId']]
        lines += [f"- [{source_info['title']}]({source_info['url']}) — {ref['locator']}"]
    lines += ['', f"근거 문서: `{question['documentKey']}`. 검수 상태: **{question['reviewNotes']['humanReview']}**", '']
lines += ['## 검색에 제공할 근거 문서', '']
for document in bundle['documents']:
    lines += [f"### {document['key']}", '', f"{document['title']} · {document['technologyVersion']}", '', document['content'], '']
lines += ['## 출처 이용 조건 검수', '', '원문·코드·그림 미포함. 아래 메모는 법률 검토나 공개 승인 결과가 아님.', '']
for source_info in bundle['sources']:
    lines += [f"- [{source_info['title']}]({source_info['url']}): {source_info['licenseNote']}"]
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text('\n'.join(lines) + '\n')
print(f'Rendered {len(bundle["questions"])} questions and {len(bundle["documents"])} documents: {os.path.relpath(target, root)}')
