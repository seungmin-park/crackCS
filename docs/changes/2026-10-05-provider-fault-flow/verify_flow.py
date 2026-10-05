#!/usr/bin/env python3
"""Visible app flow with local injected timeout/429/503; no external model calls."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import time
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts"))
sys.path.insert(0, str(ROOT / "docs/changes/2026-10-05-os-verdict-flow"))
from content_bundle import LocalAdminClient
from verify_flow import browser, knowledge

PASSWORD = "local fault flow passphrase"


def provider(payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    path = "/events" if payload is None else "/control"
    with urlopen(Request("http://127.0.0.1:18082" + path, data=data, headers={"Content-Type": "application/json"}), timeout=10) as response:
        return json.load(response)


def sql(query):
    result = subprocess.run(["docker", "exec", "crackcs-os-validation-postgres-1", "psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1", "-U", "crackcs_local", "-d", "crackcs_local", "-c", "BEGIN READ ONLY; " + query + "; COMMIT;"],
                            text=True, capture_output=True, check=True)
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--surface", required=True)
    parser.add_argument("--email", required=True)
    parser.add_argument("--existing-member", action="store_true")
    parser.add_argument("--output", type=Path, default=ROOT / ".firecrawl/provider-fault-flow/results")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    subprocess.run(["cmux", "identify", "--workspace", args.workspace, "--surface", args.surface, "--json"], check=True)
    checks = []
    results = []

    def check(name, condition):
        checks.append({"name": name, "passed": bool(condition)})
        print(("PASS " if condition else "FAIL ") + name, flush=True)
        if not condition:
            raise AssertionError(name)

    if not args.existing_member:
        browser(args.surface, "navigate", "http://127.0.0.1:5174/sign-up")
        browser(args.surface, "wait", "--selector", 'input[name="email"]', "--timeout-ms", "15000")
        browser(args.surface, "fill", 'input[name="email"]', args.email)
        browser(args.surface, "fill", 'input[name="password"]', PASSWORD)
        browser(args.surface, "fill", 'input[name="nickname"]', "통제 장애 검증")
        browser(args.surface, "click", 'button[type="submit"]')
        browser(args.surface, "wait", "--url-contains", "/login", "--timeout-ms", "15000")
    else:
        browser(args.surface, "navigate", "http://127.0.0.1:5174/login")
    browser(args.surface, "fill", 'input[name="email"]', args.email)
    browser(args.surface, "fill", 'input[name="password"]', PASSWORD)
    browser(args.surface, "click", 'button[type="submit"]')
    browser(args.surface, "wait", "--selector", 'a[href="/knowledge-map"]', "--timeout-ms", "15000")
    client = LocalAdminClient("http://127.0.0.1:18080")
    member = client.request("POST", "/api/auth/login", {"email": args.email, "password": PASSWORD})
    member_id = member["id"]
    check("fresh fault learner starts with no answers", client.request("GET", "/api/members/me/answers")["totalElements"] == 0)
    admin = LocalAdminClient("http://127.0.0.1:18080")
    admin.request("POST", "/api/auth/login", {"email": "admin@crackcs.local", "password": "local admin passphrase"})
    bundle = json.loads((ROOT / "docs/content/initial-v1/bundle.json").read_text())
    question = next(q for q in bundle["questions"] if q["key"] == "OS-101")
    matches = [q for q in admin.all_pages("/api/admin/questions") if q["content"] == question["content"] and q["status"] == "PUBLISHED"]
    check("approved OS-101 is uniquely published", len(matches) == 1)
    question_id = matches[0]["id"]
    content = question["referenceAnswer"]
    details = admin.request("GET", f"/api/admin/questions/{question_id}")
    concept_ids = {c["conceptId"] for c in details["concepts"]}

    def submit(mode, failures, label):
        before = knowledge(client)
        provider({"mode": mode, "failures": failures})
        print(f"SCENARIO {label}: injected {mode} x {failures}; synthetic recovery only", flush=True)
        browser(args.surface, "navigate", f"http://127.0.0.1:5174/questions/{question_id}")
        browser(args.surface, "wait", "--selector", "#answer-content", "--timeout-ms", "15000")
        browser(args.surface, "fill", "#answer-content", content)
        browser(args.surface, "click", '.answer-form button[type="submit"]')
        browser(args.surface, "wait", "--url-contains", "/answers/", "--timeout-ms", "15000")
        answer_id = int(re.search(r"/answers/(\d+)", browser(args.surface, "get", "url")).group(1))
        check(label + " submitted original visible during evaluation", content in browser(args.surface, "get", "text", "body"))
        deadline = time.monotonic() + 80
        previous = None
        while True:
            answer = client.request("GET", f"/api/answers/{answer_id}")
            evaluation = answer["evaluation"]
            if evaluation["status"] != previous:
                print(f"Answer {answer_id}: {evaluation['status']}", flush=True)
                previous = evaluation["status"]
            if evaluation["status"] not in {"PROCESSING", "EVALUATING"}:
                break
            if time.monotonic() >= deadline:
                raise TimeoutError(label)
            time.sleep(0.5)
        browser(args.surface, "reload")
        browser(args.surface, "wait", "--selector", ".evaluation-panel", "--timeout-ms", "15000")
        ui_text = browser(args.surface, "get", "text", "body")
        after = knowledge(client)
        events = provider()
        stored = json.loads(sql(f"SELECT jsonb_build_object('attemptCount',e.attempt_count,'modelName',e.model_name) FROM evaluation e JOIN answer a ON a.id=e.answer_id WHERE a.id={answer_id} AND a.member_id={member_id}"))
        check(label + " API original preserved", answer["content"] == content and answer["questionId"] == question_id)
        check(label + " original restored after refresh", content in ui_text)
        expected_attempts = 3 if failures else 1
        check(label + " bounded attempts", stored["attemptCount"] == expected_attempts and events["attempts"] == expected_attempts)
        check(label + " exact injected transport attempts", [e["injected"] for e in events["events"]] == [mode] * failures + (["success"] if failures < 3 else []))
        if failures == 3:
            reason = "PROVIDER_TIMEOUT" if mode == "timeout" else "PROVIDER_ERROR"
            check(label + " final failure is not an incorrect verdict", evaluation["status"] == "FAILED" and evaluation["verdict"] is None and evaluation["score"] is None and evaluation["failureReason"] == reason)
            check(label + " failure guidance visible", "지금은 평가를 완료하지 못했어요" in ui_text and "새 답변으로 다시 제출" in ui_text)
            check(label + " no evidence attached to failure", evaluation["evidence"] == [])
            check(label + " no knowledge state changed", before == after)
        else:
            check(label + " controlled synthetic recovery evaluated", evaluation["status"] == "EVALUATED" and stored["modelName"] == "controlled-openai-contract")
            check(label + " published v2 evidence preserved", bool(evaluation["evidence"]) and all(e["documentVersion"] == 2 for e in evaluation["evidence"]))
            check(label + " exactly one application per required concept", all(after[c]["attemptCount"] == before[c]["attemptCount"] + 1 for c in concept_ids))
            check(label + " unrelated concepts preserved", all(after[c] == before[c] for c in set(before) - concept_ids))
        browser(args.surface, "screenshot", "--out", str(args.output / f"{label}.png"))
        results.append({"scenario": label, "answerId": answer_id, "evaluationId": answer["evaluationId"], "status": evaluation["status"],
                        "attemptCount": stored["attemptCount"], "failureReason": evaluation["failureReason"], "events": events["events"]})
        # Original request ID is read only for this new member's submission.
        request_id = sql(f"SELECT request_id FROM answer WHERE id={answer_id} AND member_id={member_id}")
        csrf = client.request("GET", "/api/auth/csrf")
        headers = {"Content-Type": "application/json", csrf["headerName"]: csrf["token"], "Idempotency-Key": request_id}
        request = Request(client.base_url + f"/api/questions/{question_id}/answers", data=json.dumps({"content": content}).encode(), headers=headers, method="POST")
        with client.opener.open(request, timeout=15) as response:
            repeated = json.load(response)
        check(label + " repeated request returns same answer and evaluation", repeated["answerId"] == answer_id and repeated["evaluationId"] == answer["evaluationId"])
        check(label + " repeated request cannot reapply knowledge", knowledge(client) == after)
        check(label + " repeated request cannot call provider again", provider()["attempts"] == expected_attempts)
        return answer_id

    for mode in ("timeout", "429", "503"):
        submit(mode, 2, mode + "-recover")
        failed_id = submit(mode, 3, mode + "-exhausted")
        # The UI advertises a new submission, not mutation/requeue of failed Evaluation.
        retried_id = submit("success", 0, mode + "-new-submission")
        check(mode + " retry creates new answer and preserves old failure", retried_id != failed_id and client.request("GET", f"/api/answers/{failed_id}")["evaluation"]["status"] == "FAILED")
    check("all nine originals persist in answer history", client.request("GET", "/api/members/me/answers")["totalElements"] == 9)
    counts = json.loads(sql(f"SELECT jsonb_build_object('answers',(SELECT count(*) FROM answer WHERE member_id={member_id}),'evaluations',(SELECT count(*) FROM evaluation e JOIN answer a ON a.id=e.answer_id WHERE a.member_id={member_id}),'applications',(SELECT count(*) FROM knowledge_application ka JOIN evaluation_concept ec ON ec.id=ka.evaluation_concept_id JOIN evaluation e ON e.id=ec.evaluation_id JOIN answer a ON a.id=e.answer_id WHERE a.member_id={member_id}),'concepts',(SELECT count(*) FROM evaluation_concept ec JOIN evaluation e ON e.id=ec.evaluation_id JOIN answer a ON a.id=e.answer_id WHERE a.member_id={member_id}))"))
    check("DB unique evaluations and applications", counts == {"answers": 9, "evaluations": 9, "applications": 12, "concepts": 12})
    check("provider requests bounded to 21 total", sum(row["attemptCount"] for row in results) == 21)
    browser(args.surface, "navigate", "http://127.0.0.1:5174/knowledge-map")
    browser(args.surface, "wait", "--text", "학습", "--timeout-ms", "15000")
    browser(args.surface, "screenshot", "--out", str(args.output / "knowledge-final.png"))
    report = {"scope": "CONTROLLED_LOCAL_FAULTS_AND_SYNTHETIC_RECOVERY", "externalOpenAiCalls": 0,
              "memberId": member_id, "questionId": question_id, "checks": checks, "results": results, "dbCounts": counts}
    (args.output / "results.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(f"Completed: {len(checks)} assertions PASS, 9 UI submissions, 21 local transport requests, external calls 0", flush=True)


if __name__ == "__main__":
    main()
