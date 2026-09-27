#!/usr/bin/env python3
"""Render the human reading copy; bundle.json remains authoritative."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / 'docs/content/initial-v1/bundle.json'
bundle = json.loads(source.read_text())
concepts = {item['code']: item for item in bundle['concepts']}
sources = {item['id']: item for item in bundle['sources']}
lines = [
    '# initial-v1 검수용 읽기 자료', '',
    '> 자동 생성 읽기 사본. 수정 기준은 [bundle.json](bundle.json). 사람 검수·공개 승인 대기.', '',
    f"- 원본 SHA-256: `{hashlib.sha256(source.read_bytes()).hexdigest()}`",
    '- 재생성: 저장소 루트에서 `python3 scripts/render_content_review.py`',
    '- 검수 절차·승인 기록: [콘텐츠 안내](README.md#사람-검수공개-순서)',
    '- 이 자료를 읽었다는 사실만으로 앱의 검수·공개 상태가 바뀌지 않음', '',
    '## 문항과 판정 기준', '',
]
for question in bundle['questions']:
    lines += [f"### {question['key']} · {question['difficulty']}", '', question['content'], '', '**모범 답안**', '', question['referenceAnswer'], '', '**필수 개념**', '']
    for criterion in question['concepts']:
        concept = concepts[criterion['code']]
        lines += [f"- {concept['name']} · 가중치 {criterion['weight']}: {concept['description']}"]
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
target = root / 'docs/content/initial-v1/review.md'
target.write_text('\n'.join(lines) + '\n')
print(f'Rendered {len(bundle["questions"])} questions and {len(bundle["documents"])} documents: {target.relative_to(root)}')
