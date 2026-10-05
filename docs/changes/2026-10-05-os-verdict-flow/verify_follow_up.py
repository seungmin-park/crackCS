#!/usr/bin/env python3
"""Verify one remedial follow-up and audit stored results without submitting them again."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

from verify_flow import ROOT, LOCAL_PASSWORD, LABELS, browser, check, collect_answer, knowledge
sys.path.insert(0, str(ROOT / "scripts"))
from content_bundle import LocalAdminClient


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--surface", required=True)
    parser.add_argument("--email", required=True)
    parser.add_argument("--results", type=Path, default=ROOT / ".firecrawl/os-verdict-flow/results")
    parser.add_argument("--container", default="crackcs-os-validation-postgres-1")
    args = parser.parse_args()
    path = args.results
    run = json.loads((path / "results.json").read_text())
    if len(run["results"]) != 15:
        raise ValueError("All 15 frozen cases must be observed before the follow-up")
    client = LocalAdminClient("http://127.0.0.1:18080")
    client.request("POST", "/api/auth/login", {"email": args.email, "password": LOCAL_PASSWORD})
    admin = LocalAdminClient("http://127.0.0.1:18080")
    admin.request("POST", "/api/auth/login", {"email": "admin@crackcs.local", "password": "local admin passphrase"})
    source = next(r for r in run["results"] if r["caseId"] == "OS-101-P")
    follow_up = source["followUp"]["question"]
    # Admin question endpoints intentionally expose NORMAL questions only.
    # Read the generated question's FK for the supplementary target assertion.
    target_query = f"select qc.concept_id from question q join question_concept qc on q.id=qc.question_id where q.id={int(follow_up['id'])} and q.type='FOLLOW_UP' and q.source_answer_id={int(source['answer']['answerId'])};"
    target_rows = subprocess.run(["docker", "exec", args.container, "psql", "-U", "crackcs_local", "-d", "crackcs_local", "-Atc", target_query],
                                 text=True, capture_output=True, check=True).stdout
    expected_exec_id = next(c["id"] for c in admin.all_pages("/api/admin/concepts") if c["code"] == "OS_101_EXEC")
    weaker_ids = {c["conceptId"] for c in source["answer"]["evaluation"]["concepts"] if c["verdict"] != "CORRECT"}
    targeted_ids = {int(row) for row in target_rows.splitlines()}
    if weaker_ids != {expected_exec_id} or targeted_ids != weaker_ids:
        raise AssertionError("Remedial follow-up did not target the missing exec Concept")
    print("PASS follow-up targets exec Concept 3, not the already-correct fork Concept", flush=True)
    case = {
        "id": "FOLLOW-UP-EXEC-C", "sourceCaseId": "OS-101-P", "questionId": follow_up["id"],
        "question": follow_up["content"], "expectedVerdict": "CORRECT",
        "expectedConcepts": {"OS_101_EXEC": "CORRECT"},
        "answer": "성공한 exec는 이미 존재하는 자식 프로세스의 프로그램 이미지를 새 프로그램으로 교체합니다. 자식의 PID는 그대로이고 exec 자체가 새 프로세스를 추가로 만들지 않습니다. 기존 프로그램은 교체되므로 성공한 exec는 이전 프로그램의 호출 다음 문장으로 돌아오지 않습니다.",
        "labelStatus": "AGENT_AUTHORED_NOT_HUMAN_REVIEWED",
        "frozenAt": datetime.now(timezone.utc).isoformat(),
    }
    encoded = (json.dumps(case, ensure_ascii=False, indent=2) + "\n").encode()
    (path / "follow-up-case.json").write_bytes(encoded)
    print("Pre-submit follow-up case SHA-256:", hashlib.sha256(encoded).hexdigest(), flush=True)
    histories = {}
    for result in run["results"]:
        for concept in result["answer"]["evaluation"]["concepts"]:
            if result["answer"]["evaluation"]["status"] == "EVALUATED" and concept["score"] is not None:
                histories.setdefault(concept["conceptId"], []).append(concept["score"])
    before = knowledge(client)
    browser(args.surface, "navigate", f"http://127.0.0.1:5174/answers/{source['answer']['answerId']}")
    selector = f"#follow-up-answer-{follow_up['id']}"
    browser(args.surface, "wait", "--selector", selector, "--timeout-ms", "15000")
    browser(args.surface, "fill", selector, case["answer"])
    browser(args.surface, "click", '.follow-up-panel button[type="submit"]')
    browser(args.surface, "wait", "--function", f"/\\/answers\\/\\d+$/.test(location.pathname) && !location.pathname.endsWith('/{source['answer']['answerId']}')", "--timeout-ms", "15000")
    url = browser(args.surface, "get", "url")
    answer_id = int(re.search(r"/answers/(\d+)", url)[1])
    result = collect_answer(client, args.surface, answer_id, follow_up["id"], case["answer"], case,
                            {"OS_101_EXEC": expected_exec_id}, histories, before, path, expected_follow_up="UNAVAILABLE")
    checks = []
    check(checks, "follow-up of follow-up blocked", result["followUp"]["reason"] == "FOLLOW_UP_LIMIT", "FOLLOW_UP_LIMIT", result["followUp"]["reason"])
    browser(args.surface, "wait", "--text", "후속 학습을 마쳤어요", "--timeout-ms", "15000")
    browser(args.surface, "click", '.follow-up-panel a[href="/questions"]')
    browser(args.surface, "wait", "--url-contains", "/questions", "--timeout-ms", "15000")
    check(checks, "UI links to next basic questions", "/questions" in browser(args.surface, "get", "url"))
    audit = []
    for original in run["results"]:
        answer = original["answer"]
        browser(args.surface, "navigate", f"http://127.0.0.1:5174/answers/{answer['answerId']}")
        browser(args.surface, "wait", "--selector", ".evaluation-panel h2", "--timeout-ms", "15000")
        heading = " ".join(browser(args.surface, "get", "text", "--selector", ".evaluation-panel h2").split())
        evaluation = answer["evaluation"]
        expected = f"{LABELS[evaluation['verdict']]} · {evaluation['score']}점"
        check(audit, original["caseId"] + " exact UI verdict/score", heading == expected, expected, heading)
        concept_text = browser(args.surface, "get", "text", "--selector", ".concept-results")
        check(audit, original["caseId"] + " UI Concept verdicts", all(f"{c['conceptName']} · {LABELS[c['verdict']]}" in concept_text for c in evaluation["concepts"]))
        refreshed = client.request("GET", f"/api/answers/{answer['answerId']}")
        check(audit, original["caseId"] + " stored answer unchanged after reload", refreshed == answer)
    final_states = knowledge(client)
    check(audit, "UI reloads do not apply knowledge twice", final_states == knowledge(client) and final_states == {c["conceptId"]: c for c in result["knowledgeAfter"]})
    browser(args.surface, "navigate", "http://127.0.0.1:5174/")
    browser(args.surface, "wait", "--selector", ".learning-recommendation", "--timeout-ms", "15000")
    progress = client.request("GET", "/api/members/me/progress")
    recommendation = progress["recommendation"]
    if recommendation["questionId"] is not None:
        check(audit, "recommendation UI matches API question", f"/questions/{recommendation['questionId']}" in browser(args.surface, "get", "attr", "--selector", ".recommendation-link", "--attr", "href"))
    browser(args.surface, "screenshot", "--out", str(path / "recommendation.png"))
    browser(args.surface, "navigate", "http://127.0.0.1:5174/knowledge-map")
    browser(args.surface, "wait", "--text", "학습 중", "--timeout-ms", "15000")
    browser(args.surface, "screenshot", "--out", str(path / "knowledge-map-final.png"))
    report = {"case": case, "result": result, "checks": checks, "uiAudit": audit,
              "finalKnowledge": list(final_states.values()), "progress": progress}
    (path / "follow-up-and-ui-audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    failures = sum(not c["passed"] for c in checks + audit + result["behaviorChecks"] + result["diagnosticComparisons"])
    print(f"RESULT follow-up and exact UI audit failures: {failures}", flush=True)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
