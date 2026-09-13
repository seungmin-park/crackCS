"""Validate content packaging; does not run or grade an AI model.

Run with --render after editing the authoritative JSON/JSONL files.
"""
import argparse
import json
import re
from collections import Counter
from decimal import Decimal
from pathlib import Path
from urllib.parse import unquote, urlparse

BASE = Path(__file__).resolve().parent


def read_jsonl(name):
    return [json.loads(line) for line in (BASE / name).read_text().splitlines() if line.strip()]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def render(questions, goldens, source_map):
    lines = [
        '# 문제·골든 셋 검수본',
        '',
        '상태: AI 작성 초안 · 독립 검수 대기 · 운영 공개 및 모델 품질 인증용 사용 전 검수 필요.',
        '',
        '대상: 주니어~미들 개발자. AX: AI Transformation. 모든 문항은 서술형.',
        '',
        '기준 데이터: [questions.jsonl](questions.jsonl), [golden-set.jsonl](golden-set.jsonl), [knowledge-documents.jsonl](knowledge-documents.jsonl). 이 문서는 세 파일의 검수용 출력본. 수정 시 기준 데이터와 출력본을 함께 갱신.',
        '',
        '[시장 조사·활용 안내](report.md) · [출처 목록](sources.json)',
        '',
        '## 목차',
        '',
        '| 분야 | 범위 | 문제 | 판정 사례 |',
        '|---|---|---:|---:|',
        '| CS | OS·네트워크·DB·자료구조·알고리즘 | 10 | 40 |',
        '| Java | 객체·컬렉션·동시성·자원 관리 | 10 | 40 |',
        '| Spring | DI·트랜잭션·HTTP·보안·이벤트 | 10 | 40 |',
        '| JPA | 영속성·조회·매핑·동시성 | 10 | 40 |',
        '| AX | 업무 선정·RAG·평가·안전·운영 효과 | 10 | 40 |',
        '',
        '공통 판정: 필수 기준 2개 충족 → 정답; 일부 누락 → 부분 정답; 핵심 모순 → 오답; 제공 근거 부족 → 검토 필요. 가중치는 개념 기여도이며 단순 합산 점수로 판정을 덮어쓰지 않음.',
        '',
        '검토 필요 사례: G01과 같은 답변을 사용하되 K2 근거를 제공하지 않은 통제 실험. 학습자의 무지·오답을 검토 필요로 분류하는 예시가 아님.',
        '',
    ]
    for question in questions:
     id=question['id'];cases=[g for g in goldens if g['questionId']==id]
     lines.extend([f"## {id} · {question['title']}",'',f"- 분야: {question['topicKey']} · 난이도: {question['difficulty']} · 우선순위: {question['priority']}",f"- 적용 조건: {question['technologyVersion']}",'', '**질문**', '',question['content'],'','**모범 답안**','',question['referenceAnswer'],'','**필수 평가 기준**',''])
     for c in question['concepts']:lines.append(f"- `{c['id']}` · 가중치 {c['weight']} · {c['acceptance']}")
     lines.extend(['','**판정 사례**',''])
     for g in cases:lines.extend([f"- `{g['id']}` · **{g['expectedVerdict']}** · {g['caseType']}",f"  - 답변: {g['answer']}",f"  - 판정 이유: {g['expectedReason']}",f"  - 제공 근거: {', '.join(g['providedEvidenceIds'])}"])
     lines.extend(['','**꼬리 질문**','',question['followUpQuestion'],'','**첨부용 참고 링크**',''])
     for r in question['sourceRefs']:lines.append(f"- [{source_map[r['sourceId']]['title']}]({r['url']}) — {r['locator']}")
     lines.append('')
    return "\n".join(lines) + "\n"

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--render", action="store_true")
    args = parser.parse_args()
    questions = read_jsonl("questions.jsonl")
    goldens = read_jsonl("golden-set.jsonl")
    documents = read_jsonl("knowledge-documents.jsonl")
    sources = json.loads((BASE / "sources.json").read_text())
    passed = ["JSON/JSONL parsing"]
    source_map = {s["id"]: s for s in sources}
    qmap = {q["id"]: q for q in questions}
    chunks = {c["id"]: c for d in documents for c in d["chunks"]}
    require(len(questions) == 50 and len(goldens) == 200 and len(documents) == 50, "Artifact counts")
    require(Counter(q["topicKey"] for q in questions) == {d: 10 for d in ["CS", "JAVA", "SPRING", "JPA", "AX"]}, "Domain balance")
    passed.append("Artifact counts and domain coverage")
    for rows in [questions, goldens, documents, sources, [c for d in documents for c in d["chunks"]], [c for q in questions for c in q["concepts"]]]:
        require(len({r["id"] for r in rows}) == len(rows), "Duplicate IDs")
    require(len({q["content"] for q in questions}) == 50, "Duplicate question texts")
    require(len({s["url"] for s in sources}) == len(sources), "Duplicate source URLs")
    passed.append("Unique IDs, questions and URLs")
    for q in questions:
        require(q["difficulty"] in {"BASIC", "INTERMEDIATE", "ADVANCED"}, "Invalid difficulty")
        require(q["priority"] in {"P0", "P1"}, "Invalid priority")
        for field in ["content", "title", "referenceAnswer", "technologyVersion", "commonMisconception", "followUpQuestion"]:
            require(isinstance(q[field], str) and q[field].strip(), "Empty question field: " + field)
    passed.append("Required fields and enums")
    for q in questions:
        require(len(q["concepts"]) == 2, "Two concepts required")
        require(sum(Decimal(c["weight"]) for c in q["concepts"]) == 1, "Invalid weight total")
        require(all(c["required"] and c["topicKey"] == q["topicKey"] and c["acceptance"].strip() and Decimal(c["weight"]) == Decimal("0.50") for c in q["concepts"]), "Invalid concept")
    passed.append("Required concepts and weights")
    for row in [*questions, *documents, *chunks.values()]:
        require(row["sourceRefs"], "Missing source refs")
        for ref in row["sourceRefs"]:
            require(ref["sourceId"] in source_map, "Unknown source")
            require(ref["url"] == source_map[ref["sourceId"]]["url"] and ref["locator"].strip(), "Source link mismatch")
    for s in sources:
        require(urlparse(s["url"]).scheme == "https" and s["checkedAt"] == "2026-09-09", "Invalid source metadata")
        require(re.fullmatch(r"[0-9a-f]{64}", s["retrievedBodySha256"]), "Invalid source body fingerprint")
    passed.append("Source mapping and metadata (no live network recheck)")
    require(len(chunks) == 100, "Evidence chunk count")
    for d in documents:
        q = qmap[d["id"].removesuffix("-DOC")]
        require(d["topicKey"] == q["topicKey"] and d["technologyVersion"] == q["technologyVersion"], "Document scope mismatch")
        require(d["sourceType"] == "INTERNAL_SUMMARY" and len(d["chunks"]) == 2, "Document structure")
        for c, concept in zip(d["chunks"], q["concepts"]):
            require(c["conceptId"] == concept["id"] and c["content"] == concept["acceptance"], "Evidence mapping")
    passed.append("Document and evidence mapping")
    score = {"CORRECT": 100, "PARTIALLY_CORRECT": 50, "INCORRECT": 0, "NEEDS_REVIEW": None}
    kinds = {"PARAPHRASE": "CORRECT", "OMISSION": "PARTIALLY_CORRECT", "FLUENT_WRONG": "INCORRECT", "INSUFFICIENT_EVIDENCE": "NEEDS_REVIEW"}
    for q in questions:
        cases = [g for g in goldens if g["questionId"] == q["id"]]
        require(len(cases) == 4 and {g["caseType"] for g in cases} == set(kinds), "Case coverage")
        for g in cases:
            require(g["expectedVerdict"] == kinds[g["caseType"]] and g["expectedScore"] == score[g["expectedVerdict"]], "Verdict/score mismatch")
            require(g["answer"].strip() and g["expectedReason"].strip(), "Empty answer or reason")
    passed.append("Four verdict types and score mapping")
    require(all(g["questionId"] in qmap for g in goldens), "Unknown question reference")
    for q in questions:
        cases = {g["caseType"]: g for g in goldens if g["questionId"] == q["id"]}
        full = [q["id"] + "-K1", q["id"] + "-K2"]
        for kind, g in cases.items():
            require(g["providedEvidenceIds"] == (full[:1] if kind == "INSUFFICIENT_EVIDENCE" else full), "Invalid evidence selection")
            require(all(cid in chunks for cid in g["providedEvidenceIds"]), "Unknown evidence")
            require(g["expectedEvidencePolicy"] == "PROVIDED_CHUNKS_ONLY", "Evidence policy")
        review = cases["INSUFFICIENT_EVIDENCE"]
        require(review["answer"] == cases["PARAPHRASE"]["answer"], "Evidence-only control changed answer")
        require(review["unsupportedConceptIds"] == [q["id"] + "-C2"], "Missing unsupported concept")
        require(cases["OMISSION"]["omittedConceptIds"] == [q["id"] + "-C2"], "Missing omission annotation")
        require(len({cases[k]["answer"] for k in ["PARAPHRASE", "OMISSION", "FLUENT_WRONG"]}) == 3, "Identical regular answers")
    passed.append("Missing-evidence controls and omission annotations")
    for row in [*questions, *goldens, *documents]:
        require(row["reviewStatus"] == "PENDING_INDEPENDENT_REVIEW" and row["reviewedBy"] is None and row["reviewedAt"] is None, "Unverified review status")
    require(all(r["status"] == "DRAFT" for r in [*questions, *documents]), "Premature publishing")
    require(all(r["split"] == "UNASSIGNED" and r["benchmarkEligible"] is False for r in [*questions, *goldens]), "Premature benchmark eligibility")
    passed.append("Draft status and unassigned benchmark split")
    expected = render(questions, goldens, source_map)
    if args.render:
        (BASE / "questions.md").write_text(expected)
    require((BASE / "questions.md").read_text() == expected, "Review view stale; run with --render")
    passed.append("Review view exactly matches authoritative data")
    for filename in ["report.md", "questions.md"]:
        for target in re.findall(r"\]\(([^)]+)\)", (BASE / filename).read_text()):
            if urlparse(target).scheme or target.startswith("#"):
                continue
            require((BASE / unquote(target.split("#")[0])).exists(), "Broken local link: " + target)
    passed.append("Local artifact links")
    print(json.dumps({"status": "PASS", "checkGroups": len(passed), "checks": passed, "questions": len(questions), "goldenCasesValidatedAsData": len(goldens), "documents": len(documents), "chunks": len(chunks), "sourceURLs": len(sources), "aiGradingRuns": 0}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
