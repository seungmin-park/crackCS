"""Validate the evaluation reference; does not run or grade an AI model.

Run with --render after editing the authoritative JSON/JSONL files.
"""
import argparse
import hashlib
import json
import re
from collections import Counter
from datetime import date
from decimal import Decimal
from pathlib import Path
from urllib.parse import unquote, urlparse

BASE = Path(__file__).resolve().parent
REFERENCE_ROOT = BASE.parent
DATA_DIRECTORY = REFERENCE_ROOT / "data"
REPOSITORY_ROOT = REFERENCE_ROOT.parents[2]
RENDER_PATH = REPOSITORY_ROOT / "build/reports/evaluation/reference-v1/questions.md"


def read_jsonl(name):
    return [json.loads(line) for line in (DATA_DIRECTORY / name).read_text().splitlines()
            if line.strip()]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate_reference_standard(manifest, artifacts, question_ids, source_ids):
    """Reject edits that no longer match the reviewed reference snapshot."""
    require(manifest["status"] == "FINALIZED", "Reference not finalized")
    require(manifest["independentHumanReview"] is True,
            "Independent human review required")
    require(manifest["reviewer"] == {"kind": "HUMAN", "name": "PROJECT_OWNER"},
            "Independent human reviewer metadata")
    require(set(manifest["artifacts"]) == set(artifacts), "Reference artifact coverage")
    for name, body in artifacts.items():
        require(hashlib.sha256(body).hexdigest() == manifest["artifacts"][name],
                "Reference artifact changed; review and version again: " + name)
    reviews = manifest["questionReviews"]
    require(len(reviews) == len(question_ids)
            and {review["questionId"] for review in reviews} == question_ids,
            "Review coverage mismatch")
    for review in reviews:
        require(review["sourceIds"] and set(review["sourceIds"]) <= source_ids,
                "Review source missing or unknown")
        require(review["decision"] == "ACCEPT" and review["note"].strip(),
                "Unresolved reference review")


def validate_human_review(rows):
    """Require the same completed human-review metadata on every source row."""
    for row in rows:
        require("authorship" not in row, "Authorship provenance must not be stored")
        require(row["reviewStatus"] == "REVIEWED"
                and row["reviewedBy"] == "PROJECT_OWNER"
                and row["reviewedAt"] == "2026-09-21",
                "Independent human review metadata")


def select_questions(questions, split):
    manifest = json.loads((DATA_DIRECTORY / "experiment-splits.json").read_text())
    groups = manifest["groups"]
    assigned = [key for group in groups for key in group["questionIds"]]
    require(len(assigned) == len(set(assigned)), "Question occurs in multiple split groups")
    require(set(assigned) == {question["id"] for question in questions}, "Split coverage mismatch")
    require(all(group["split"] in {"development", "evaluation-candidate"} for group in groups),
            "Invalid split assignment")
    require(split in {"all", "development", "evaluation-candidate"}, "Unknown split")
    selected = {key for group in groups if split == "all" or group["split"] == split
                for key in group["questionIds"]}
    return [question for question in questions if question["id"] in selected]


def experiment_rows(questions, goldens, documents, kind):
    """Export offline adapter inputs or labels, never publish or call a provider.

    Numeric IDs belong only to this dataset snapshot, not to an application DB.
    Evidence scores are fixture values, not measured retrieval scores.
    """
    require(kind in {"inputs", "labels", "retrieval", "corpus"}, "Unknown export kind")
    question_map = {question["id"]: question for question in questions}
    topic_ids = {key: index for index, key in enumerate(
        sorted({question["topicKey"] for question in questions}), 1)}
    concept_ids = {key: index for index, key in enumerate(
        sorted(concept["id"] for question in questions for concept in question["concepts"]), 1)}
    chunk_ids = {key: index for index, key in enumerate(
        sorted(chunk["id"] for document in documents for chunk in document["chunks"]), 1)}
    evidence = {}
    for document_id, document in enumerate(sorted(documents, key=lambda row: row["id"]), 1):
        offset = 0
        for chunk in document["chunks"]:
            length = len(chunk["content"].encode("utf-16-le")) // 2
            evidence[chunk["id"]] = {
                "chunkId": chunk_ids[chunk["id"]], "documentId": document_id,
                "documentTitle": document["title"], "documentVersion": document["documentVersion"],
                "startOffset": offset, "endOffset": offset + length,
                "content": chunk["content"], "relevanceScore": 1.0,
            }
            offset += length + 1  # Offline document body: chunks joined with one newline.
    if kind == "corpus":
        return [{"documentKey": document["id"], "topicKey": document["topicKey"],
                 "technologyVersion": document["technologyVersion"],
                 "content": "\n".join(chunk["content"] for chunk in document["chunks"]),
                 "chunks": [{"evidenceKey": chunk["id"], **evidence[chunk["id"]]}
                            for chunk in document["chunks"]]}
                for document in sorted(documents, key=lambda row: row["id"])]
    rows = []
    for golden in goldens:
        require(golden["questionId"] in question_map, "Unknown question")
        require(all(key in evidence for key in golden["providedEvidenceIds"]), "Unknown evidence")
        question = question_map[golden["questionId"]]
        require(all(key.startswith(question["id"] + "-K") for key in golden["providedEvidenceIds"]),
                "Evidence scope mismatch")
        if kind == "retrieval":
            # G04 intentionally removes evidence; it is a generation control, not a search label.
            if golden["caseType"] == "INSUFFICIENT_EVIDENCE":
                continue
            keys = [question["id"] + "-K1", question["id"] + "-K2"]
            rows.append({
                "caseId": golden["id"], "questionId": question["id"], "topicKey": question["topicKey"],
                "questionContent": question["content"], "referenceAnswer": question["referenceAnswer"],
                "answerContent": golden["answer"], "relevantEvidenceKeys": keys,
                "relevantEvidenceIds": [chunk_ids[key] for key in keys],
                "reviewStatus": golden["reviewStatus"], "benchmarkEligible": golden["benchmarkEligible"],
            })
        elif kind == "labels":
            rows.append({
                "caseId": golden["id"], "questionId": golden["questionId"],
                "caseType": golden["caseType"], "expectedVerdict": golden["expectedVerdict"],
                "expectedScore": golden["expectedScore"], "expectedReason": golden["expectedReason"],
                "providedEvidenceIds": [chunk_ids[key] for key in golden["providedEvidenceIds"]],
                "reviewStatus": golden["reviewStatus"], "benchmarkEligible": golden["benchmarkEligible"],
            })
        else:
            rows.append({"caseId": golden["id"], "request": {
                "topicId": topic_ids[question["topicKey"]],
                "questionContent": question["content"], "referenceAnswer": question["referenceAnswer"],
                "answerContent": golden["answer"],
                "concepts": [{"conceptId": concept_ids[concept["id"]], "name": concept["acceptance"],
                              "required": concept["required"]} for concept in question["concepts"]],
                "evidence": [evidence[key] for key in golden["providedEvidenceIds"]],
            }})
    return rows


def render(questions, goldens, source_map, reference_version):
    counts = Counter(question["topicKey"] for question in questions)
    lines = [
        '# 문제·골든 셋 검수본',
        '',
        f'상태: 출시 회귀 정답 기준 v{reference_version} 확정 · 프로젝트 소유자 독립 검수 완료 · 운영 공개·실모델 품질 인증은 별도.',
        '',
        '대상: 주니어~미들 개발자. AX: AI Transformation. 모든 문항은 서술형.',
        '',
        '기준 데이터: [questions.jsonl](../../../../docs/evaluation/reference-v1/data/questions.jsonl), [golden-set.jsonl](../../../../docs/evaluation/reference-v1/data/golden-set.jsonl), [knowledge-documents.jsonl](../../../../docs/evaluation/reference-v1/data/knowledge-documents.jsonl). 이 문서는 세 파일에서 필요할 때 생성하는 검수용 출력본.',
        '',
        '[확정 버전·판정 정책·문항별 검토 기록](../../../../docs/evaluation/reference-v1/manifest.json) · [활용 안내](../../../../docs/evaluation/reference-v1/README.md) · [출처 목록](../../../../docs/evaluation/reference-v1/data/sources.json)',
        '',
        '## 목차',
        '',
        '| 분야 | 범위 | 문제 | 판정 사례 |',
        '|---|---|---:|---:|',
        *[f'| {topic} | {scope} | {counts[topic]} | {counts[topic] * 4} |'
          for topic, scope in [
              ('CS', 'OS·네트워크·DB·자료구조·알고리즘'),
              ('JAVA', '객체·제네릭·예외·컬렉션·동시성·자원 관리'),
              ('SPRING', 'DI·트랜잭션·HTTP·보안·설정·테스트'),
              ('JPA', '영속성·조회·매핑·동시성·값 타입'),
              ('AX', '업무 선정·RAG·평가·안전·운영 효과')]],
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
    parser.add_argument("--export", choices=["inputs", "labels", "retrieval", "corpus"],
                        help="Validate then print offline experiment JSONL; no network calls")
    parser.add_argument("--split", choices=["all", "development", "evaluation-candidate"], default="all",
                        help="Select query/input/label families; corpus always contains the full snapshot")
    args = parser.parse_args()
    require(args.split == "all" or args.export in {"inputs", "labels", "retrieval"},
            "Split filtering requires an input, label or retrieval export")
    questions = read_jsonl("questions.jsonl")
    goldens = read_jsonl("golden-set.jsonl")
    documents = read_jsonl("knowledge-documents.jsonl")
    sources = json.loads((DATA_DIRECTORY / "sources.json").read_text())
    passed = ["JSON/JSONL parsing"]
    source_map = {s["id"]: s for s in sources}
    qmap = {q["id"]: q for q in questions}
    chunks = {c["id"]: c for d in documents for c in d["chunks"]}
    require(len(questions) == 60 and len(goldens) == 240 and len(documents) == 60, "Artifact counts")
    require(Counter(q["topicKey"] for q in questions) == {"CS": 12, "JAVA": 14, "SPRING": 13, "JPA": 11, "AX": 10}, "Domain coverage")
    passed.append("Artifact counts and domain coverage")
    for rows in [questions, goldens, documents, sources, [c for d in documents for c in d["chunks"]], [c for q in questions for c in q["concepts"]]]:
        require(len({r["id"] for r in rows}) == len(rows), "Duplicate IDs")
    require(len({q["content"] for q in questions}) == len(questions), "Duplicate question texts")
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
        require(urlparse(s["url"]).scheme == "https", "Invalid source URL")
        date.fromisoformat(s["checkedAt"])
        require(re.fullmatch(r"[0-9a-f]{64}", s["retrievedBodySha256"]), "Invalid source body fingerprint")
    passed.append("Source mapping and metadata (no live network recheck)")
    require(len(chunks) == len(questions) * 2, "Evidence chunk count")
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
    validate_human_review([*questions, *goldens, *documents])
    require(all(r["status"] == "DRAFT" for r in [*questions, *documents]), "Premature publishing")
    require(all(r["split"] == "UNASSIGNED" and r["benchmarkEligible"] is False for r in [*questions, *goldens]), "Premature benchmark eligibility")
    passed.append("Draft status and unassigned benchmark split")
    standard = json.loads((REFERENCE_ROOT / "manifest.json").read_text())
    artifacts = {name: (DATA_DIRECTORY / name).read_bytes() for name in (
        "questions.jsonl", "golden-set.jsonl", "knowledge-documents.jsonl",
        "sources.json", "experiment-splits.json")}
    validate_reference_standard(standard, artifacts, set(qmap), set(source_map))
    passed.append("Finalized reference fingerprints and per-question review coverage")
    selected_ids = {question["id"] for question in select_questions(questions, args.split)}
    passed.append("Exploratory split coverage and no cross-split question duplicates")
    expected = render(questions, goldens, source_map, standard["version"])
    if args.render:
        RENDER_PATH.parent.mkdir(parents=True, exist_ok=True)
        RENDER_PATH.write_text(expected)
    passed.append("Review view can be rendered from authoritative data")
    for markdown_file in [REFERENCE_ROOT / "README.md"]:
        for target in re.findall(r"\]\(([^)]+)\)", markdown_file.read_text()):
            if urlparse(target).scheme or target.startswith("#"):
                continue
            require((markdown_file.parent / unquote(target.split("#")[0])).exists(),
                    "Broken local link: " + target)
    passed.append("Local artifact links")
    if args.export:
        case_questions = {golden["id"]: golden["questionId"] for golden in goldens}
        for row in experiment_rows(questions, goldens, documents, args.export):
            if args.export == "corpus" or case_questions[row["caseId"]] in selected_ids:
                print(json.dumps(row, ensure_ascii=False))
    else:
        print(json.dumps({"status": "PASS", "checkGroups": len(passed), "checks": passed, "questions": len(questions), "goldenCasesValidatedAsData": len(goldens), "documents": len(documents), "chunks": len(chunks), "sourceURLs": len(sources), "aiGradingRuns": 0}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
