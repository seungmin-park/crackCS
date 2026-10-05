"""Prepare blinded review inputs, freeze human labels, and compare observed results.

No provider calls or application writes. Origin and blind-review attestations
are human claims; hashes detect later edits, not the truth of those claims.
"""
import argparse
from collections import Counter
from datetime import datetime, timezone
from decimal import Decimal
import hashlib
from html import escape
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import secrets
import sys

VERDICTS = {"CORRECT", "PARTIALLY_CORRECT", "INCORRECT", "NEEDS_REVIEW"}
CATEGORIES = {"SHORT", "TERMINOLOGY", "MIXED", "OTHER"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def nonempty(value, name):
    require(isinstance(value, str) and bool(value.strip()), f"missing {name}")


def digest(value):
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()
    return hashlib.sha256(raw).hexdigest()


def unique(rows, name):
    result = {}
    for row in rows:
        key = row[name]
        nonempty(key, name)
        require(key not in result, f"duplicate {name}: {key}")
        result[key] = row
    return result


def prepare_review(samples):
    nonempty(samples["version"], "version")
    require(samples["topicCode"] == "OPERATING_SYSTEM", "OS topic required")
    for field in ("population", "method", "limitations"):
        nonempty(samples["sampling"][field], field)
    require(samples["cases"], "empty collection")
    unique(samples["cases"], "caseId")
    inputs = {"version": samples["version"], "topicCode": samples["topicCode"],
              "sampling": {field: samples["sampling"][field] for field in ("population", "method", "limitations")}, "cases": []}
    labels = {"version": samples["version"], "status": "DRAFT", "reviewer": None, "reviewedAt": None,
              "blindReviewConfirmed": False, "cases": []}
    lines = ["# OS 독립 검수 자료", "", "- 기존 모델 판정·점수·피드백 제외", "- 원문 그대로 보존. 사람 판정은 labels.json에 입력", "",
             "표본 범위: " + inputs["sampling"]["population"], "", "한계: " + inputs["sampling"]["limitations"], ""]
    for case in samples["cases"]:
        nonempty(case["answer"], "answer")
        question = case["question"]
        nonempty(question["content"], "question")
        nonempty(question["referenceAnswer"], "reference answer")
        require(type(question["id"]) is int and question["id"] > 0 and type(question["version"]) is int and question["version"] > 0, "invalid question identity")
        concepts = unique(question["concepts"], "code")
        require(concepts and any(c["required"] for c in concepts.values()), "required concept missing")
        weights = [Decimal(str(c["weight"])) for c in concepts.values()]
        require(all(w.is_finite() and 0 < w <= 1 for w in weights) and sum(weights) == 1, "invalid weight")
        safe_concepts = []
        for concept in concepts.values():
            for field in ("name", "description"):
                nonempty(concept[field], "concept " + field)
            require(type(concept["required"]) is bool, "invalid required flag")
            safe_concepts.append({field: concept[field] for field in ("code", "name", "description", "weight", "required")})
        provenance = case["provenance"]
        require(provenance["kind"] in {"UNCONFIRMED", "HUMAN_LEARNER", "AGENT_AUTHORED"}, "invalid provenance")
        nonempty(provenance["source"], "source")
        safe_case = {"caseId": case["caseId"], "provenance": {field: provenance[field] for field in ("kind", "source", "sourceAnswerId")},
                     "question": {field: question[field] for field in ("id", "version", "content", "referenceAnswer")}, "answer": case["answer"]}
        safe_case["question"]["concepts"] = safe_concepts
        inputs["cases"].append(safe_case)
        labels["cases"].append({"caseId": case["caseId"], "sourceConfirmedHuman": False, "sourceEvidence": "",
                                "category": "UNCLASSIFIED", "expectedVerdict": None, "reason": "",
                                "expectedConcepts": {code: {"verdict": None, "reason": ""} for code in concepts}})
        lines += ["## " + case["caseId"], "", question["content"], "", "### 학습자 답변 원문", "", case["answer"], "",
                  "### 기준 답안", "", question["referenceAnswer"], "", "### 개념별 판정", ""]
        lines += [f"- {c['code']} / {c['name']} / 가중치 {c['weight']}: {c['description']} — 판정·이유 작성 필요" for c in safe_concepts]
        lines += ["", "- 표본 유형: SHORT / TERMINOLOGY / MIXED / OTHER", "- 실제 사람 작성·모델 결과 열람 전 판정 여부 확인 필요", ""]
    labels["inputsSha256"] = digest(inputs)
    return {"inputs": inputs, "labels": labels, "reviewMarkdown": "\n".join(lines) + "\n"}


def freeze_review(samples, labels):
    inputs = prepare_review(samples)["inputs"]
    require(labels["status"] == "REVIEWED", "human review not complete")
    require(labels["version"] == inputs["version"] and labels["inputsSha256"] == digest(inputs), "inputs changed before review freeze")
    nonempty(labels["reviewer"], "human reviewer")
    reviewed_at = timestamp(labels["reviewedAt"])
    require(reviewed_at <= datetime.now(timezone.utc), "human review time is in the future")
    require(labels["blindReviewConfirmed"] is True, "blind human review must precede model output inspection")
    indexed_labels = unique(labels["cases"], "caseId")
    require(set(indexed_labels) == {case["caseId"] for case in inputs["cases"]}, "review case coverage mismatch")
    for case in inputs["cases"]:
        label = indexed_labels[case["caseId"]]
        require(case["provenance"]["kind"] != "AGENT_AUTHORED", "agent authored sample is not an independent learner")
        require(label["sourceConfirmedHuman"] is True, "learner origin unconfirmed")
        nonempty(label["sourceEvidence"], "learner origin evidence")
        require(label["category"] in CATEGORIES, "sample category unclassified")
        require(label["expectedVerdict"] in VERDICTS, "overall human verdict missing")
        nonempty(label["reason"], "overall reason")
        codes = {concept["code"] for concept in case["question"]["concepts"]}
        require(set(label["expectedConcepts"]) == codes, "human concept coverage mismatch")
        for concept in label["expectedConcepts"].values():
            require(concept["verdict"] in VERDICTS, "human concept verdict missing")
            nonempty(concept["reason"], "concept reason")
    return {"version": inputs["version"], "status": "FROZEN", "inputsSha256": digest(inputs), "labelsSha256": digest(labels),
            "frozenAt": datetime.now(timezone.utc).isoformat(), "reviewer": labels["reviewer"], "reviewedAt": labels["reviewedAt"]}


def timestamp(value):
    nonempty(value, "timestamp")
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    require(parsed.tzinfo is not None, "timestamp timezone required")
    return parsed


def metric(matched, total, count_name="matched"):
    return {count_name: matched, "total": total, "rate": matched / total if total else None}


def compare_results(samples, labels, frozen, results):
    inputs = prepare_review(samples)["inputs"]
    require(frozen["status"] == "FROZEN" and frozen["version"] == inputs["version"], "frozen review required")
    require(frozen["inputsSha256"] == digest(inputs) and frozen["labelsSha256"] == digest(labels), "inputs or labels changed after freeze")
    # Recheck origin/review contracts even when a manifest was supplied by hand.
    freeze_review(samples, labels)
    frozen_at = timestamp(frozen["frozenAt"])
    require(timestamp(labels["reviewedAt"]) <= frozen_at, "freeze precedes human review")
    observations = unique(results, "caseId")
    indexed_labels = unique(labels["cases"], "caseId")
    require(set(observations) <= set(indexed_labels), "unknown observed case")
    model_versions = set()
    for result in results:
        require(timestamp(result["observedAt"]) > frozen_at, "model result predates review freeze")
        require(result["status"] in {"EVALUATED", "NEEDS_REVIEW", "FAILED"}, "observed result is not terminal")
        if result["status"] != "FAILED":
            require(result["verdict"] in VERDICTS, "invalid model verdict")
            codes = set(indexed_labels[result["caseId"]]["expectedConcepts"])
            require(set(result["concepts"]) == codes and all(v in VERDICTS for v in result["concepts"].values()), "invalid observed concepts")
            nonempty(result["modelName"], "model name")
            nonempty(result["evaluatorVersion"], "evaluator version")
            require(type(result["evidenceCount"]) is int and result["evidenceCount"] >= 0, "invalid evidence count")
            model_versions.add((result["modelName"], result["evaluatorVersion"]))
    require(len(model_versions) <= 1, "mixed model or evaluator versions")
    counts = Counter()
    rows = []
    for label in labels["cases"]:
        actual = observations.get(label["caseId"])
        verdict = actual["verdict"] if actual and actual["status"] != "FAILED" else None
        expected = label["expectedVerdict"]
        counts["missing"] += actual is None
        counts["failed"] += bool(actual and actual["status"] == "FAILED")
        group = "needsReview" if expected == "NEEDS_REVIEW" else "overall"
        counts[group + "Total"] += 1
        counts[group + "Matched"] += expected == verdict
        if expected in {"CORRECT", "INCORRECT"}:
            counts["binaryTotal"] += 1
            counts["binaryMatched"] += expected == verdict
        if expected == "INCORRECT":
            counts["falseCorrectTotal"] += 1
            counts["falseCorrect"] += verdict == "CORRECT"
        concept_rows = []
        for code, concept in label["expectedConcepts"].items():
            concept_actual = actual["concepts"].get(code) if actual and actual["status"] != "FAILED" else None
            concept_group = "conceptNeedsReview" if concept["verdict"] == "NEEDS_REVIEW" else "concept"
            counts[concept_group + "Total"] += 1
            counts[concept_group + "Matched"] += concept["verdict"] == concept_actual
            concept_rows.append({"code": code, "expected": concept["verdict"], "actual": concept_actual,
                                 "matched": concept["verdict"] == concept_actual, "humanReason": concept["reason"]})
        if actual and actual["status"] == "EVALUATED":
            counts["evaluated"] += 1
            counts["withEvidence"] += actual["evidenceCount"] > 0
        rows.append({"caseId": label["caseId"], "category": label["category"], "expected": expected, "actual": verdict,
                     "status": actual["status"] if actual else "MISSING", "matched": expected == verdict, "concepts": concept_rows})
    categories = Counter(label["category"] for label in labels["cases"])
    return {"assessment": "DESCRIPTIVE_ONLY", "version": frozen["version"], "inputsSha256": frozen["inputsSha256"],
            "labelsSha256": frozen["labelsSha256"], "caseCount": len(labels["cases"]), "observedCount": len(results),
            "missingCount": counts["missing"], "failedCount": counts["failed"], "sampling": inputs["sampling"],
            "questionCount": len({case["question"]["id"] for case in inputs["cases"]}),
            "categories": {category: categories[category] for category in sorted(CATEGORIES)},
            "missingCategories": sorted({"SHORT", "TERMINOLOGY", "MIXED"} - set(categories)),
            "modelVersions": [{"modelName": model, "evaluatorVersion": evaluator} for model, evaluator in sorted(model_versions)],
            "overallAgreement": metric(counts["overallMatched"], counts["overallTotal"]),
            "binaryAgreement": metric(counts["binaryMatched"], counts["binaryTotal"]),
            "falseCorrect": metric(counts["falseCorrect"], counts["falseCorrectTotal"], "count"),
            "needsReviewAgreement": metric(counts["needsReviewMatched"], counts["needsReviewTotal"]),
            "conceptAgreement": metric(counts["conceptMatched"], counts["conceptTotal"]),
            "conceptNeedsReviewAgreement": metric(counts["conceptNeedsReviewMatched"], counts["conceptNeedsReviewTotal"]),
            "evidenceCoverage": metric(counts["withEvidence"], counts["evaluated"]), "comparisons": rows}


def read_json(path):
    return json.loads(path.read_text())


def write_new(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x") as destination:
        destination.write(value if isinstance(value, str) else json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False) + "\n")


def review_page(samples, nonce):
    packet = prepare_review(samples)
    select_verdict = '<option value="">판정 선택</option>' + ''.join(f'<option value="{v}">{v}</option>' for v in ("CORRECT", "PARTIALLY_CORRECT", "INCORRECT", "NEEDS_REVIEW"))
    sections = []
    for index, case in enumerate(packet["inputs"]["cases"]):
        question = case["question"]
        concepts = ''.join(f'<label>{escape(c["code"])} · {escape(c["description"])}<select data-code="{escape(c["code"], quote=True)}" required>{select_verdict}</select><textarea data-reason="{escape(c["code"], quote=True)}" placeholder="개념 판정 이유" required></textarea></label>' for c in question["concepts"])
        sections.append(f'''<section data-index="{index}"><h2>{escape(case["caseId"])}</h2>
<p>{escape(question["content"])}</p><h3>답변 원문</h3><pre>{escape(case["answer"])}</pre>
<details><summary>기준 답안</summary><p>{escape(question["referenceAnswer"])}</p></details>
<label>종합 판정<select name="expectedVerdict" required>{select_verdict}</select></label>
<label>종합 판정 이유<textarea name="reason" required></textarea></label>{concepts}
<label>유형<select name="category" required><option value="">유형 선택</option><option>SHORT</option><option>TERMINOLOGY</option><option>MIXED</option><option>OTHER</option></select></label>
<label><input type="checkbox" name="sourceConfirmedHuman" required> 실제 학습자가 작성한 답변임을 확인</label>
<label>출처 확인 근거<input name="sourceEvidence" required placeholder="작성자·수집 과정 확인 근거 (개인정보 제외)"></label></section>''')
    serialized = json.dumps(packet["labels"], ensure_ascii=False).replace("<", "\\u003c")
    return '''<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>OS 독립 사람 검수</title><style>body{font:16px/1.65 system-ui;background:#f7f5ef;color:#172b26;margin:0}main{max-width:900px;margin:40px auto;padding:24px}section{background:white;border:1px solid #b7c7bf;border-radius:12px;padding:24px;margin:28px 0}h1,h2{color:#155746}label{display:block;margin:18px 0}select,input:not([type=checkbox]),textarea{box-sizing:border-box;display:block;width:100%;font:inherit;padding:10px;border:1px solid #869a91;border-radius:6px}textarea{min-height:85px}pre{white-space:pre-wrap;background:#edf4ef;padding:20px;font:inherit}button{font:inherit;background:#155746;color:white;border:0;border-radius:8px;padding:14px 24px}details{padding:12px;background:#f3f3ed}#result{white-space:pre-wrap}</style>
<main><h1>OS 독립 사람 검수</h1><p>원문 4개를 읽고 개념별 판정을 확정하세요. 기존 GPT 결과는 이 화면에 제공하지 않습니다.</p>
<p>이전 결과를 이미 열람했거나 실제 학습자 원문인지 확인할 수 없으면 해당 확인란을 선택하지 마세요. 표본 4개는 대표성 인증에 충분하지 않습니다.</p>
<form id="review"><label>검수자 이름·역할<input name="reviewer" required></label>''' + ''.join(sections) + '''
<label><input type="checkbox" name="blindReviewConfirmed" required> 기존 GPT 결과를 보기 전에 이번 판정과 이유를 확정했음을 확인</label>
<button type="submit">사람 판정 확정·해시 동결</button></form><p id="result" role="status"></p></main>
<script>const draft=''' + serialized + ''';const nonce=''' + json.dumps(nonce) + ''';
document.querySelector('form').addEventListener('submit',async event=>{event.preventDefault();
const labels=JSON.parse(JSON.stringify(draft));const form=event.target;labels.status='REVIEWED';labels.reviewer=form.elements.reviewer.value;
labels.reviewedAt=new Date().toISOString();labels.blindReviewConfirmed=form.elements.blindReviewConfirmed.checked;
for(const section of document.querySelectorAll('[data-index]')){const row=labels.cases[Number(section.dataset.index)];
for(const field of ['expectedVerdict','reason','category','sourceEvidence'])row[field]=section.querySelector('[name="'+field+'"]').value;
row.sourceConfirmedHuman=section.querySelector('[name="sourceConfirmedHuman"]').checked;
for(const select of section.querySelectorAll('[data-code]'))row.expectedConcepts[select.dataset.code]={verdict:select.value,reason:section.querySelector('[data-reason="'+select.dataset.code+'"]').value};}
try{const response=await fetch('/review',{method:'POST',headers:{'Content-Type':'application/json','X-Review-Nonce':nonce},body:JSON.stringify(labels)});
const result=await response.json();document.querySelector('#result').textContent=result.message;
if(response.ok){document.querySelector('button').disabled=true;document.querySelector('#review').hidden=true;}}
catch(error){document.querySelector('#result').textContent='저장 연결 실패. 입력은 유지됩니다. 다시 시도하세요.';}});</script></html>'''


def save_human_review(samples, labels, output):
    frozen = freeze_review(samples, labels)
    require(not (output / "frozen.json").exists() and not (output / "labels-final.json").exists(), "human review already saved")
    write_new(output / "labels-final.json", labels)
    write_new(output / "frozen.json", frozen)
    return frozen


def serve_review(samples, output, port):
    nonce = secrets.token_urlsafe(32)
    page = review_page(samples, nonce).encode()
    origin = f"http://127.0.0.1:{port}"

    class ReviewHandler(BaseHTTPRequestHandler):
        def log_message(self, format, *args):
            pass  # No submitted answers, labels, or headers in HTTP logs.

        def respond(self, status, body, content_type="application/json"):
            encoded = body if isinstance(body, bytes) else json.dumps(body, ensure_ascii=False).encode()
            self.send_response(status)
            self.send_header("Content-Type", content_type + "; charset=utf-8")
            self.send_header("Content-Length", str(len(encoded)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Referrer-Policy", "same-origin")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(encoded)

        def do_GET(self):
            if self.headers.get("Host") != f"127.0.0.1:{port}":
                self.respond(403, {"message": "Local origin required"})
            elif self.path == "/":
                self.respond(200, page, "text/html")
            elif self.path == "/status":
                self.respond(200, {"frozen": (output / "frozen.json").exists()})
            else:
                self.respond(404, {"message": "Not found"})

        def do_POST(self):
            if self.path != "/review" or self.headers.get("Host") != f"127.0.0.1:{port}" or self.headers.get("Origin") != origin or self.headers.get("X-Review-Nonce") != nonce:
                self.respond(403, {"message": "Local review form required"})
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                require(0 < length <= 262144, "invalid request length")
                labels = json.loads(self.rfile.read(length))
                frozen = save_human_review(samples, labels, output)
                self.respond(200, {"message": "사람 판정 저장·동결 완료. GPT 비교 실행 전까지 이 기록을 보존합니다.", "inputsSha256": frozen["inputsSha256"]})
                print("Human review frozen; no provider call performed.", flush=True)
            except (ValueError, KeyError, TypeError, OSError) as error:
                self.respond(400, {"message": f"저장 중단: {error}"})

    print(f"Human review form: {origin} / outputs: {output}", flush=True)
    with ThreadingHTTPServer(("127.0.0.1", port), ReviewHandler) as server:
        server.serve_forever()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subcommands = parser.add_subparsers(dest="command", required=True)
    prepare = subcommands.add_parser("prepare")
    prepare.add_argument("--samples", type=Path, required=True)
    prepare.add_argument("--output", type=Path, required=True)
    freeze = subcommands.add_parser("freeze")
    compare = subcommands.add_parser("compare")
    serve = subcommands.add_parser("serve")
    serve.add_argument("--inputs", type=Path, required=True)
    serve.add_argument("--output", type=Path, required=True)
    serve.add_argument("--port", type=int, default=18084)
    for command in (freeze, compare):
        command.add_argument("--inputs", type=Path, required=True)
        command.add_argument("--labels", type=Path, required=True)
        command.add_argument("--output", type=Path, required=True)
    compare.add_argument("--frozen", type=Path, required=True)
    compare.add_argument("--results", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "prepare":
        packet = prepare_review(read_json(args.samples))
        require(not args.output.exists(), "output directory exists; preserve reviewer edits")
        for name, value in (("inputs.json", packet["inputs"]), ("labels.json", packet["labels"]), ("review.md", packet["reviewMarkdown"])):
            write_new(args.output / name, value)
        print(f"Prepared {len(packet['inputs']['cases'])} blinded cases; human labels remain DRAFT: {args.output}")
    elif args.command == "serve":
        serve_review(read_json(args.inputs), args.output, args.port)
    elif args.command == "freeze":
        frozen = freeze_review(read_json(args.inputs), read_json(args.labels))
        write_new(args.output, frozen)
        print("Frozen human review:", frozen["inputsSha256"], frozen["labelsSha256"])
    else:
        report = compare_results(read_json(args.inputs), read_json(args.labels), read_json(args.frozen), read_json(args.results))
        write_new(args.output, report)
        print(f"Compared {report['observedCount']}/{report['caseCount']} cases; missing={report['missingCount']}, failed={report['failedCount']}; DESCRIPTIVE_ONLY")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError) as error:
        print(f"Independent review stopped: {error}", file=sys.stderr)
        sys.exit(1)
