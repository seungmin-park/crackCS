#!/usr/bin/env python3
"""Register Java drafts, publish with visible cmux clicks, then verify the local pipeline."""
import argparse
from datetime import datetime, timezone
import hashlib
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
from content_bundle import LocalAdminClient, import_drafts, validate_bundle
from verify_flow import browser, knowledge, poll

ADMIN = {"email": "admin@crackcs.local", "password": "local admin passphrase"}
LEARNER = {"email": "fault-flow-20261005@crackcs.local", "password": "local fault flow passphrase"}


def client(credentials):
    api = LocalAdminClient("http://127.0.0.1:18080")
    member = api.request("POST", "/api/auth/login", credentials)
    return api, member


def bundle():
    original = json.loads((ROOT / "docs/content/initial-v1/bundle.json").read_text())
    result = {"version": original["version"], "status": "DRAFT", "reviewedBy": None}
    for kind in ("topics", "concepts", "documents", "questions"):
        result[kind] = [row for row in original[kind] if row.get("topicCode", row.get("code")) == "JAVA"]
    used_sources = {source for document in result["documents"] for source in document["sourceIds"]}
    result["sources"] = [source for source in original["sources"] if source["id"] in used_sources]
    return result


def sql(query):
    result = subprocess.run(["docker", "exec", "crackcs-os-validation-postgres-1", "psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1", "-U", "crackcs_local", "-d", "crackcs_local", "-c", "BEGIN READ ONLY; " + query + "; COMMIT;"], capture_output=True, text=True, check=True)
    return result.stdout.strip()


def ui_login(surface, credentials):
    browser(surface, "navigate", "http://127.0.0.1:5174/")
    browser(surface, "wait", "--selector", ".header-actions", "--timeout-ms", "15000")
    if "로그아웃" in browser(surface, "get", "text", ".header-actions"):
        browser(surface, "click", ".header-text-button")
        browser(surface, "wait", "--url-contains", "/login", "--timeout-ms", "15000")
    browser(surface, "navigate", "http://127.0.0.1:5174/login")
    browser(surface, "wait", "--selector", 'input[name="email"]', "--timeout-ms", "15000")
    browser(surface, "fill", 'input[name="email"]', credentials["email"])
    browser(surface, "fill", 'input[name="password"]', credentials["password"])
    browser(surface, "click", 'button[type="submit"]')
    browser(surface, "wait", "--selector", ".header-text-button", "--timeout-ms", "15000")


def click_button(surface, name):
    found = browser(surface, "snapshot", "--interactive")
    match = re.search(r'button "' + re.escape(name) + r'" \[ref=(e\d+)\]', found)
    if match is None:
        raise RuntimeError("Missing UI action: " + name + ": " + found)
    browser(surface, "click", match.group(1))


def wait_value(read, predicate, label):
    deadline = time.monotonic() + 20
    while time.monotonic() < deadline:
        value = read()
        if predicate(value):
            return value
        time.sleep(0.25)
    raise TimeoutError(label)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=("register", "publish", "learn"), required=True)
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--surface", required=True)
    parser.add_argument("--output", type=Path, default=ROOT / ".firecrawl/java-review/runtime")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    subprocess.run(["cmux", "identify", "--workspace", args.workspace, "--surface", args.surface, "--json"], check=True)
    checks = []

    def check(name, passed):
        checks.append({"name": name, "passed": bool(passed)})
        print(("PASS " if passed else "FAIL ") + name, flush=True)
        if not passed:
            raise AssertionError(name)

    topic_bundle = bundle()
    admin, reviewer = client(ADMIN)
    ids_path = args.output / "registered.json"
    if args.phase == "register":
        references = [json.loads(line) for line in (ROOT / "docs/evaluation/reference-v1/data/questions.jsonl").read_text().splitlines() if line.strip()]
        audit = validate_bundle(topic_bundle, references)
        check("Java draft scope: 5 questions, 10 concepts, 1 document", (audit["questionCount"], audit["conceptCount"], audit["documentCount"]) == (5, 10, 1))
        ids = import_drafts(topic_bundle, admin)
        ids_path.write_text(json.dumps(ids, ensure_ascii=False, indent=2) + "\n")
        for key, question_id in ids["questions"].items():
            question = admin.request("GET", f"/api/admin/questions/{question_id}")
            check(key + " remains unreviewed DRAFT", question["status"] == "DRAFT" and question["reviewedAt"] is None)
        document = admin.request("GET", f"/api/admin/knowledge-documents/{ids['documents']['java-v1']}")
        check("Java document remains unreviewed DRAFT", document["status"] == "DRAFT" and document["reviewedAt"] is None)
        results = {"registeredIds": ids, "structure": audit}
    elif args.phase == "publish":
        ids = json.loads(ids_path.read_text())
        approval = json.loads((Path(__file__).parent / "review-record.json").read_text())
        expected_hash = hashlib.sha256(json.dumps(topic_bundle, ensure_ascii=False, indent=2).encode() + b"\n").hexdigest()
        check("review fixed before publication with exact Java scope", approval["bundleSha256"] == expected_hash and approval["reviewLabel"] == "사용자 검수" and approval["localPublicationAuthorized"] and len(approval["questions"]) == 5)
        ui_login(args.surface, ADMIN)
        browser(args.surface, "navigate", "http://127.0.0.1:5174/admin/knowledge-documents")
        document_id = ids["documents"]["java-v1"]
        selector = f'button[data-document-id="{document_id}"]'
        browser(args.surface, "wait", "--selector", selector, "--timeout-ms", "15000")
        browser(args.surface, "click", selector)
        browser(args.surface, "wait", "--text", "Java 핵심 동작과 경계 · initial-v1 · v1", "--timeout-ms", "15000")
        original_document = topic_bundle["documents"][0]
        content = original_document["content"].replace("직접 작성한 한국어 설명. 사람 검수와 공개 승인 대기.", "직접 작성한 한국어 설명. 사용자 검수·로컬 공개 승인 완료.")
        license_note = "직접 작성한 한국어 요약. 원문·코드·그림 재배포 없음. 출처별 이용 조건은 initial-v1 bundle.json 참조. 사용자 검수·로컬 공개 승인 완료. 공개 서비스·파일럿 승인 별도."
        browser(args.surface, "fill", '.admin-form label:nth-of-type(6) textarea', license_note)
        browser(args.surface, "fill", '.admin-form label:nth-of-type(7) textarea', content)
        click_button(args.surface, "초안 수정")
        wait_value(lambda: admin.request("GET", f"/api/admin/knowledge-documents/{document_id}"), lambda d: d["content"] == content, "draft metadata save")
        click_button(args.surface, "검수")
        reviewed_document = wait_value(lambda: admin.request("GET", f"/api/admin/knowledge-documents/{document_id}"), lambda d: d["reviewedAt"] is not None, "document review")
        check("document reviewed by local admin before publish", reviewed_document["status"] == "DRAFT" and reviewed_document["reviewedByMemberId"] == reviewer["id"])
        click_button(args.surface, "공개")
        document = wait_value(lambda: admin.request("GET", f"/api/admin/knowledge-documents/{document_id}"), lambda d: d["status"] == "PUBLISHED", "document publish")
        browser(args.surface, "wait", "--text", "검색 문단 생성", "--timeout-ms", "15000")
        click_button(args.surface, "검색 문단 생성")
        chunks = wait_value(lambda: admin.request("GET", f"/api/admin/knowledge-documents/{document_id}/chunks"), bool, "chunk generation")
        check("published Java chunks preserve approved offsets and cover all five sections", all(content[c["startOffset"]:c["endOffset"]] == c["content"] for c in chunks) and all(any("[" + q["key"] + "]" in c["content"] for c in chunks) for q in topic_bundle["questions"]))
        check("Java publication metadata and checksum preserved", document["technologyVersion"] == "Java 21" and document["checksum"] == hashlib.sha256(content.encode()).hexdigest() and document["licenseNote"] == license_note)
        browser(args.surface, "screenshot", "--out", str(args.output / "java-document.png"))
        published_questions = []
        for source in topic_bundle["questions"]:
            question_id = ids["questions"][source["key"]]
            browser(args.surface, "navigate", "http://127.0.0.1:5174/admin/questions")
            selector = f'button[data-question-id="{question_id}"]'
            browser(args.surface, "wait", "--selector", selector, "--timeout-ms", "15000")
            browser(args.surface, "click", selector)
            browser(args.surface, "wait", "--text", f"Question #{question_id} · v1", "--timeout-ms", "15000")
            draft = admin.request("GET", f"/api/admin/questions/{question_id}")
            expected_criteria = {(ids["concepts"][c["code"]], float(c["weight"]), c["required"]) for c in source["concepts"]}
            check(source["key"] + " exact reviewed question and criteria", draft["status"] == "DRAFT" and draft["content"] == source["content"] and draft["referenceAnswer"] == source["referenceAnswer"] and {(c["conceptId"], c["weight"], c["required"]) for c in draft["concepts"]} == expected_criteria)
            click_button(args.surface, "검수")
            reviewed_question = wait_value(lambda: admin.request("GET", f"/api/admin/questions/{question_id}"), lambda q: q["reviewedAt"] is not None, "question review")
            check(source["key"] + " review metadata saved before publication", reviewed_question["status"] == "DRAFT" and reviewed_question["reviewedByMemberId"] == reviewer["id"])
            click_button(args.surface, "공개")
            question = wait_value(lambda: admin.request("GET", f"/api/admin/questions/{question_id}"), lambda q: q["status"] == "PUBLISHED", "question publish")
            published_questions.append({"key": source["key"], "id": question_id, "status": question["status"], "reviewedAt": question["reviewedAt"], "reviewedByMemberId": question["reviewedByMemberId"]})
        browser(args.surface, "screenshot", "--out", str(args.output / "java-questions.png"))
        check("five Java questions visible to learner", admin.request("GET", f"/api/questions?topicId={ids['topics']['JAVA']}&page=0&size=100")["totalElements"] == 5)
        results = {"registeredIds": ids, "document": {key: document[key] for key in ("id", "status", "checksum", "documentVersion", "reviewedAt", "reviewedByMemberId")}, "chunkCount": len(chunks), "questions": published_questions}
    else:
        ids = json.loads(ids_path.read_text())
        learner, member = client(LEARNER)
        baseline = knowledge(learner)
        java_ids = set(ids["concepts"].values())
        java_chunks = {c["id"] for c in admin.request("GET", f"/api/admin/knowledge-documents/{ids['documents']['java-v1']}/chunks")}
        check("all ten Java concepts initially UNKNOWN", len(java_ids) == 10 and all(baseline[c]["status"] == "UNKNOWN" and baseline[c]["attemptCount"] == 0 for c in java_ids))
        ui_login(args.surface, LEARNER)
        results = []
        for source in topic_bundle["questions"]:
            before = knowledge(learner)
            with urlopen(Request("http://127.0.0.1:18082/control", data=json.dumps({"mode": "success", "failures": 0}).encode(), headers={"Content-Type": "application/json"}), timeout=10) as response:
                json.load(response)
            question_id = ids["questions"][source["key"]]
            browser(args.surface, "navigate", f"http://127.0.0.1:5174/questions/{question_id}")
            browser(args.surface, "wait", "--selector", "#answer-content", "--timeout-ms", "15000")
            browser(args.surface, "fill", "#answer-content", source["referenceAnswer"])
            browser(args.surface, "click", '.answer-form button[type="submit"]')
            browser(args.surface, "wait", "--url-contains", "/answers/", "--timeout-ms", "15000")
            answer_id = int(re.search(r"/answers/(\d+)", browser(args.surface, "get", "url")).group(1))
            evaluation = poll(lambda: learner.request("GET", f"/api/answers/{answer_id}")["evaluation"], {"PROCESSING", "EVALUATING"}, source["key"])
            answer = learner.request("GET", f"/api/answers/{answer_id}")
            evaluation_id = answer["evaluationId"]
            browser(args.surface, "reload")
            browser(args.surface, "wait", "--selector", ".evaluation-panel", "--timeout-ms", "15000")
            text = browser(args.surface, "get", "text", "body")
            after = knowledge(learner)
            required = {ids["concepts"][c["code"]] for c in source["concepts"]}
            stored = json.loads(sql(f"SELECT jsonb_build_object('modelName',model_name,'attemptCount',attempt_count) FROM evaluation WHERE id={evaluation_id}"))
            check(source["key"] + " original and synthetic verdict visible", source["referenceAnswer"] in text and "정답" in text and evaluation["status"] == "EVALUATED" and evaluation["score"] == 100 and stored == {"modelName": "controlled-openai-contract", "attemptCount": 1})
            evidence = evaluation["evidence"]
            check(source["key"] + " published Java v1 evidence only", bool(evidence) and all(e["chunkId"] in java_chunks and e["documentVersion"] == 1 for e in evidence))
            check(source["key"] + " required concepts applied exactly once", all(after[c]["attemptCount"] == before[c]["attemptCount"] + 1 and after[c]["status"] == "LEARNING" for c in required))
            check(source["key"] + " all other states preserved", all(after[c] == before[c] for c in set(before) - required))
            browser(args.surface, "screenshot", "--out", str(args.output / (source["key"] + ".png")))
            results.append({"key": source["key"], "questionId": question_id, "answerId": answer_id, "evaluationId": evaluation_id, "evidence": evidence, "requiredConceptIds": sorted(required), "stored": stored})
        final = knowledge(learner)
        check("Java ten concepts each one learning application", all(final[c]["attemptCount"] == 1 and final[c]["status"] == "LEARNING" for c in java_ids))
        check("OS states preserved across new Topic", all(final[c] == baseline[c] for c in set(baseline) - java_ids))
        counts = json.loads(sql(f"SELECT jsonb_build_object('answers',count(DISTINCT a.id),'evaluations',count(DISTINCT e.id),'concepts',count(DISTINCT ec.id),'applications',count(DISTINCT ka.id)) FROM answer a JOIN question q ON q.id=a.question_id LEFT JOIN evaluation e ON e.answer_id=a.id LEFT JOIN evaluation_concept ec ON ec.evaluation_id=e.id LEFT JOIN knowledge_application ka ON ka.evaluation_concept_id=ec.id WHERE a.member_id={member['id']} AND q.topic_id={ids['topics']['JAVA']}"))
        check("DB five Java answers and ten unique applications", counts == {"answers": 5, "evaluations": 5, "concepts": 10, "applications": 10})
        browser(args.surface, "navigate", "http://127.0.0.1:5174/knowledge-map")
        browser(args.surface, "wait", "--text", "Java", "--timeout-ms", "15000")
        browser(args.surface, "scroll-into-view", ".knowledge-topic:nth-of-type(2) h3")
        browser(args.surface, "screenshot", "--out", str(args.output / "java-knowledge.png"))
        results = {"memberId": member["id"], "answers": results, "dbCounts": counts, "finalJavaKnowledge": [final[c] for c in sorted(java_ids)]}
    report = {"phase": args.phase, "completedAt": datetime.now(timezone.utc).isoformat(), "scope": "LOCAL_CONTENT_PIPELINE_WITH_CONTROLLED_SYNTHETIC_PROVIDER", "externalOpenAiCalls": 0, "checks": checks, "results": results}
    (args.output / (args.phase + ".json")).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(f"Completed {args.phase}: {len(checks)} assertions PASS; external calls 0", flush=True)


if __name__ == "__main__":
    main()
