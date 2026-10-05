#!/usr/bin/env python3
"""Drive the published OS diagnostic in a visible cmux browser; read APIs for assertions."""
import argparse
from datetime import datetime, timezone
from decimal import Decimal
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts"))
from content_bundle import LocalAdminClient

SCORES = {"CORRECT": 100, "PARTIALLY_CORRECT": 50, "INCORRECT": 0, "NEEDS_REVIEW": None}
LABELS = {"CORRECT": "정답", "PARTIALLY_CORRECT": "부분 정답", "INCORRECT": "오답", "NEEDS_REVIEW": "검토 필요"}
LOCAL_PASSWORD = "local verdict verification passphrase"


def browser(surface, *arguments):
    # Real cmux navigation/input/clicks; no fetch-based submission or DOM event simulation.
    command = ["cmux", "browser", "--surface", surface, *arguments]
    visible_arguments = arguments[:2] if arguments and arguments[0] == "fill" else arguments
    print("UI:", " ".join(visible_arguments), flush=True)
    result = subprocess.run(command, capture_output=True, text=True, timeout=35)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    if arguments and arguments[0] in {"navigate", "reload"}:
        # A selector from the previous document can match before navigation commits.
        # Wait for the new document's load lifecycle before filling/clicking its form.
        browser(surface, "wait", "--load-state", "complete", "--timeout-ms", "15000")
        browser(surface, "frame", "main")
    return result.stdout.strip()


def poll(read, pending, label, timeout=105):
    deadline = time.monotonic() + timeout
    previous = None
    while True:
        result = read()
        state = result["status"]
        if state != previous:
            print(label + ": " + state, flush=True)
            previous = state
        if state not in pending:
            return result
        if time.monotonic() >= deadline:
            raise TimeoutError(label + " deadline exceeded")
        time.sleep(1)


def knowledge(client):
    return {c["conceptId"]: c for t in client.request("GET", "/api/members/me/knowledge-states")["topics"] for c in t["concepts"]}


def check(checks, name, condition, expected=None, actual=None):
    item = {"name": name, "passed": bool(condition)}
    if expected is not None:
        item["expected"] = expected
    if actual is not None:
        item["actual"] = actual
    checks.append(item)
    print(("PASS " if condition else "FAIL ") + name, flush=True)


def collect_answer(client, surface, answer_id, question_id, content, expected, concept_ids, histories, before, output, expected_follow_up="READY"):
    evaluation = poll(lambda: client.request("GET", f"/api/answers/{answer_id}")["evaluation"],
                      {"EVALUATING", "PROCESSING"}, f"Evaluation {answer_id}")
    answer = client.request("GET", f"/api/answers/{answer_id}")
    evaluation = answer["evaluation"]
    follow_up = poll(lambda: client.request("GET", f"/api/answers/{answer_id}/follow-up-question"),
                     {"PENDING", "PROCESSING"}, f"Follow-up {answer_id}")
    browser(surface, "reload")
    browser(surface, "wait", "--selector", ".evaluation-panel", "--timeout-ms", "15000")
    ui_text = browser(surface, "get", "text", "body")
    ui_heading = " ".join(browser(surface, "get", "text", "--selector", ".evaluation-panel h2").split())
    after = knowledge(client)
    checks = []
    check(checks, "submitted answer and question preserved", answer["questionId"] == question_id and answer["content"] == content)
    check(checks, "evaluation completes successfully", evaluation["status"] == "EVALUATED", "EVALUATED", evaluation["status"])
    check(checks, "API overall score follows actual verdict", evaluation["score"] == SCORES.get(evaluation["verdict"]))
    check(checks, "published document v2 evidence attached", bool(evaluation["evidence"]) and all(e["documentVersion"] == 2 and 9 <= e["chunkId"] <= 15 for e in evaluation["evidence"]))
    check(checks, "result visible in browser", content in ui_text and LABELS.get(evaluation["verdict"], "평가 실패") in ui_text)
    expected_heading = f"{LABELS.get(evaluation['verdict'], '평가 실패')} · {evaluation['score']}점"
    check(checks, "UI result heading matches API verdict and score", ui_heading == expected_heading, expected_heading, ui_heading)
    actual_concepts = {c["conceptId"]: c for c in evaluation["concepts"]}
    expected_ids = set(concept_ids.values())
    check(checks, "required Concept IDs preserved", set(actual_concepts) == expected_ids)
    for concept_id, value in actual_concepts.items():
        check(checks, f"Concept {concept_id} score follows actual verdict", value["score"] == SCORES[value["verdict"]])
        if evaluation["status"] == "EVALUATED" and value["score"] is not None:
            histories.setdefault(concept_id, []).append(value["score"])
    for concept_id, values in histories.items():
        state = after[concept_id]
        mastery = Decimal(sum(values) + values[-1]) / Decimal(len(values) + 1)
        confidence = min(100, len(values) * 25)
        status = "STABLE" if mastery >= 80 and confidence >= 75 else "LEARNING"
        check(checks, f"Concept {concept_id} exactly-once count/confidence/mastery/status",
              state["attemptCount"] == len(values) and state["confidenceScore"] == confidence
              and abs(Decimal(str(state["masteryScore"])) - mastery) <= Decimal("0.01") and state["status"] == status,
              {"count": len(values), "confidence": confidence, "mastery": float(mastery), "status": status},
              {"count": state["attemptCount"], "confidence": state["confidenceScore"], "mastery": state["masteryScore"], "status": state["status"]})
    untouched = set(before) - expected_ids
    check(checks, "unrelated Concept states unchanged", all(before[c] == after[c] for c in untouched))
    check(checks, "follow-up status matches source type", follow_up["status"] == expected_follow_up, expected_follow_up, follow_up["status"])
    comparisons = []
    if expected:
        check(comparisons, "overall diagnostic verdict", evaluation["verdict"] == expected["expectedVerdict"], expected["expectedVerdict"], evaluation["verdict"])
        for code, verdict in expected["expectedConcepts"].items():
            actual = actual_concepts.get(concept_ids[code], {}).get("verdict")
            check(comparisons, code + " diagnostic verdict", actual == verdict, verdict, actual)
    output.mkdir(parents=True, exist_ok=True)
    browser(surface, "screenshot", "--out", str(output / f"answer-{answer_id}.png"))
    result = {"caseId": expected["id"] if expected else "FOLLOW-UP", "answer": answer, "followUp": follow_up,
              "knowledgeBefore": list(before.values()), "knowledgeAfter": list(after.values()),
              "uiHeading": ui_heading, "behaviorChecks": checks, "diagnosticComparisons": comparisons}
    (output / f"answer-{answer_id}.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--surface", required=True)
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--email", required=True, help="new local-only diagnostic member")
    parser.add_argument("--existing-member", action="store_true", help="resume a UI-created member only if all OS Concepts remain UNKNOWN")
    parser.add_argument("--output", type=Path, default=ROOT / ".firecrawl/os-verdict-flow/results")
    parser.add_argument("--case-limit", type=int, default=15)
    args = parser.parse_args()
    if args.case_limit != 15:
        raise ValueError("The frozen diagnostic contains exactly 15 cases; partial execution is not the full run")
    cases_path = Path(__file__).with_name("cases.json")
    frozen_sha = hashlib.sha256(cases_path.read_bytes()).hexdigest()
    previous_sha = json.loads(Path(__file__).with_name("runtime-results.json").read_text())["casesSha256"]
    if frozen_sha != previous_sha:
        raise ValueError("Cases changed after pre-run freeze")
    print("Frozen cases SHA-256:", frozen_sha, flush=True)
    context = subprocess.run(["cmux", "identify", "--workspace", args.workspace, "--surface", args.surface, "--json"], capture_output=True, text=True, check=True)
    print("Explicit cmux context:", context.stdout, flush=True)
    cases = json.loads(cases_path.read_text())["cases"]
    bundle = json.loads((ROOT / "docs/content/initial-v1/bundle.json").read_text())
    bundle_questions = {q["key"]: q for q in bundle["questions"]}
    admin = LocalAdminClient("http://127.0.0.1:18080")
    admin.request("POST", "/api/auth/login", {"email": "admin@crackcs.local", "password": "local admin passphrase"})
    published = admin.all_pages("/api/admin/questions")
    taxonomy = admin.all_pages("/api/admin/concepts")
    code_ids = {c["code"]: c["id"] for c in taxonomy}
    question_ids = {}
    for key in {c["questionKey"] for c in cases}:
        matches = [q for q in published if q["content"] == bundle_questions[key]["content"] and q["status"] == "PUBLISHED"]
        if len(matches) != 1:
            raise ValueError("Published question not unique: " + key)
        question_ids[key] = matches[0]["id"]
    if args.existing_member:
        browser(args.surface, "navigate", "http://127.0.0.1:5174/login")
    else:
        browser(args.surface, "navigate", "http://127.0.0.1:5174/sign-up")
        browser(args.surface, "wait", "--selector", 'input[name="email"]', "--timeout-ms", "15000")
        browser(args.surface, "fill", 'input[name="email"]', args.email)
        browser(args.surface, "fill", 'input[name="password"]', LOCAL_PASSWORD)
        browser(args.surface, "fill", 'input[name="nickname"]', "OS 판정 검증")
        browser(args.surface, "click", 'button[type="submit"]')
        browser(args.surface, "wait", "--url-contains", "/login", "--timeout-ms", "15000")
    browser(args.surface, "wait", "--selector", 'input[name="email"]', "--timeout-ms", "15000")
    browser(args.surface, "fill", 'input[name="email"]', args.email)
    browser(args.surface, "fill", 'input[name="password"]', LOCAL_PASSWORD)
    browser(args.surface, "click", 'button[type="submit"]')
    browser(args.surface, "wait", "--selector", 'a[href="/knowledge-map"]', "--timeout-ms", "15000")
    client = LocalAdminClient("http://127.0.0.1:18080")
    member = client.request("POST", "/api/auth/login", {"email": args.email, "password": LOCAL_PASSWORD})
    before = knowledge(client)
    os_ids = {code_ids[code] for c in cases for code in c["expectedConcepts"]}
    if not all(before[c]["attemptCount"] == 0 and before[c]["masteryScore"] is None and before[c]["status"] == "UNKNOWN" for c in os_ids):
        raise ValueError("Fresh learner did not begin with UNKNOWN OS Concepts")
    print("PASS fresh learner:", member["id"], "all 10 OS Concepts UNKNOWN", flush=True)
    args.output.mkdir(parents=True, exist_ok=True)
    history = {}
    results = []
    started = datetime.now(timezone.utc).isoformat()
    for index, case in enumerate(cases, start=1):
        print(f"\n=== {index}/15 {case['id']} expected {case['expectedVerdict']} ===", flush=True)
        question_id = question_ids[case["questionKey"]]
        before = knowledge(client)
        browser(args.surface, "navigate", f"http://127.0.0.1:5174/questions/{question_id}")
        browser(args.surface, "wait", "--selector", "#answer-content", "--timeout-ms", "15000")
        browser(args.surface, "fill", "#answer-content", case["answer"])
        browser(args.surface, "click", '.answer-form button[type="submit"]')
        browser(args.surface, "wait", "--url-contains", "/answers/", "--timeout-ms", "15000")
        url = browser(args.surface, "get", "url")
        match = re.search(r"/answers/(\d+)", url)
        if not match:
            raise ValueError("UI submission did not navigate to persisted answer")
        concept_ids = {code: code_ids[code] for code in case["expectedConcepts"]}
        result = collect_answer(client, args.surface, int(match[1]), question_id, case["answer"], case, concept_ids, history, before, args.output)
        results.append(result)
        if hashlib.sha256(cases_path.read_bytes()).hexdigest() != frozen_sha:
            raise ValueError("Frozen cases changed during execution")
        (args.output / "results.json").write_text(json.dumps({"startedAt": started, "casesSha256": frozen_sha,
            "memberId": member["id"], "questionIds": question_ids, "results": results}, ensure_ascii=False, indent=2) + "\n")
    browser(args.surface, "navigate", "http://127.0.0.1:5174/knowledge-map")
    browser(args.surface, "wait", "--text", "학습 중", "--timeout-ms", "15000")
    browser(args.surface, "screenshot", "--out", str(args.output / "knowledge-map.png"))
    overall_matches = sum(all(c["passed"] for c in r["diagnosticComparisons"] if c["name"] == "overall diagnostic verdict") for r in results)
    behavior_failures = [f"{r['caseId']}: {c['name']}" for r in results for c in r["behaviorChecks"] if not c["passed"]]
    diagnostic_failures = [f"{r['caseId']}: {c['name']}" for r in results for c in r["diagnosticComparisons"] if not c["passed"]]
    print(f"\nRESULT overall diagnostic agreement {overall_matches}/15; behavior failures {len(behavior_failures)}; diagnostic comparison failures {len(diagnostic_failures)}", flush=True)
    print(json.dumps({"behaviorFailures": behavior_failures, "diagnosticFailures": diagnostic_failures}, ensure_ascii=False), flush=True)
    return 1 if behavior_failures or diagnostic_failures else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as failure:
        print("RUN FAILED:", str(failure), file=sys.stderr, flush=True)
        raise
