#!/usr/bin/env python3
"""Verify the local human-review form with one invented tooling case in cmux."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--surface", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    checks = []

    def browser(*arguments):
        print("UI:", " ".join(arguments[:2]), flush=True)
        result = subprocess.run(["cmux", "browser", "--surface", args.surface, *arguments],
                                text=True, capture_output=True, check=True, timeout=30)
        return result.stdout

    def check(name, condition):
        checks.append({"name": name, "passed": bool(condition)})
        print(("PASS " if condition else "FAIL ") + name, flush=True)
        if not condition:
            raise AssertionError(name)

    context = subprocess.run(["cmux", "identify", "--workspace", args.workspace, "--surface", args.surface, "--json"],
                             text=True, capture_output=True, check=True)
    print("Verified explicit context:", context.stdout, flush=True)
    browser("navigate", "http://127.0.0.1:18085")
    browser("wait", "--load-state", "complete", "--timeout-ms", "15000")
    browser("snapshot", "--interactive")
    check("tooling fixture is visible", "TEST-1" in browser("get", "text", "body"))
    check("overall verdict starts empty", not browser("get", "value", "select[name=expectedVerdict]").strip())
    browser("click", "button[type=submit]")
    check("empty form cannot save human review", not (args.output / "saved/frozen.json").exists())
    browser("fill", "input[name=reviewer]", "UI_TOOLING_TEST_ONLY")
    browser("select", "select[name=expectedVerdict]", "--value", "CORRECT")
    browser("fill", "textarea[name=reason]", "Invented tooling input; not a learner quality claim")
    browser("select", "select[data-code=EXEC]", "--value", "CORRECT")
    browser("fill", "textarea[data-reason=EXEC]", "Program replacement described in tooling input")
    browser("select", "select[name=category]", "--value", "SHORT")
    browser("check", "input[name=sourceConfirmedHuman]")
    browser("fill", "input[name=sourceEvidence]", "Invented fixture; checkbox only tests form mechanics")
    browser("click", "button[type=submit]")
    check("missing blind-review attestation cannot save", not (args.output / "saved/frozen.json").exists())
    browser("check", "input[name=blindReviewConfirmed]")
    browser("click", "button[type=submit]")
    browser("wait", "--text", "사람 판정 저장·동결 완료", "--timeout-ms", "10000")
    frozen_path = args.output / "saved/frozen.json"
    labels_path = args.output / "saved/labels-final.json"
    frozen = json.loads(frozen_path.read_text())
    labels = json.loads(labels_path.read_text())
    check("UI-entered reviewer stored", labels["reviewer"] == "UI_TOOLING_TEST_ONLY")
    check("UI-entered verdict stored", labels["cases"][0]["expectedVerdict"] == "CORRECT")
    check("manifest frozen with input and label hashes", frozen["status"] == "FROZEN" and len(frozen["inputsSha256"]) == 64 and len(frozen["labelsSha256"]) == 64)
    check("original learner packet is separate", args.output.name == "ui-test")
    browser("screenshot", "--out", str(args.output / "saved-form.png"))
    check("form hidden after successful save", "true" in browser("eval", "document.getElementById('review').hidden"))
    result = {"scope": "INVENTED_UI_TOOLING_CASE_ONLY", "workspace": args.workspace, "surface": args.surface,
              "assertions": checks, "labelsFileSha256": hashlib.sha256(labels_path.read_bytes()).hexdigest(), "providerCalls": 0}
    (args.output / "results.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(f"Form assertions: {len(checks)} PASS; exit 0; no learner verdict authored.", flush=True)


if __name__ == "__main__":
    main()
