#!/usr/bin/env python3
"""Validate a versioned draft bundle; optionally register it through local admin APIs."""
import argparse
from collections import Counter
from datetime import date
from decimal import Decimal, InvalidOperation
import difflib
import getpass
import hashlib
from http.cookiejar import CookieJar
import json
from pathlib import Path
import re
import sys
from urllib.error import HTTPError
from urllib.parse import urlparse
from urllib.request import build_opener, HTTPCookieProcessor, HTTPRedirectHandler, Request

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BUNDLE = ROOT / "docs/content/initial-v1/bundle.json"
REFERENCE = ROOT / "docs/evaluation/reference-v1/data/questions.jsonl"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def normalized(text):
    return " ".join(text.split()).casefold()


def index_unique(rows, field):
    result = {}
    for row in rows:
        value = row[field]
        require(isinstance(value, str) and value.strip(), f"empty {field}")
        require(value not in result, f"duplicate {field}: {value}")
        result[value] = row
    return result


def validate_bundle(bundle, reference_questions, minimum_questions=5):
    require(bundle["status"] == "DRAFT" and bundle["reviewedBy"] is None, "Only unreviewed DRAFT bundles are accepted")
    topics = index_unique(bundle["topics"], "code")
    concepts = index_unique(bundle["concepts"], "code")
    sources = index_unique(bundle["sources"], "id")
    documents = index_unique(bundle["documents"], "key")
    questions = index_unique(bundle["questions"], "key")
    require(topics and documents and questions, "empty bundle")
    for source in sources.values():
        url = urlparse(source["url"])
        require(url.scheme == "https" and url.hostname and not url.username, "source URL must be HTTPS")
        require(source["title"].strip() and source["licenseNote"].strip(), "source metadata missing")
        date.fromisoformat(source["checkedAt"])
        require(re.fullmatch(r"[0-9a-f]{64}", source["bodySha256"]), "source body hash missing")
    for concept in concepts.values():
        require(concept["topicCode"] in topics, "unknown concept topic")
        require(concept["name"].strip() and concept["description"].strip(), "concept explanation missing")
    for document in documents.values():
        topic = topics.get(document["topicCode"])
        require(topic is not None, "unknown document topic")
        require(document["technologyVersion"] == topic["technologyVersion"], "document version mismatch")
        require(document["sourceIds"] and set(document["sourceIds"]) <= sources.keys(), "unknown document source")
        require(document["title"].strip() and document["content"].strip(), "empty document")
        for code in document["conceptCodes"]:
            require(code in concepts and concepts[code]["topicCode"] == document["topicCode"], "document concept topic mismatch")
    seen = {normalized(q["content"]) for q in reference_questions}
    coverage = Counter()
    similarities = []
    for question in questions.values():
        text = normalized(question["content"])
        require(text and text not in seen, f"duplicate question: {question['key']}")
        seen.add(text)
        require(question["referenceAnswer"].strip(), "empty reference answer")
        require(question["difficulty"] in {"BASIC", "INTERMEDIATE", "ADVANCED"}, "invalid difficulty")
        document = documents.get(question["documentKey"])
        require(document and document["topicCode"] == question["topicCode"], "question document topic mismatch")
        criteria = question["concepts"]
        require(criteria and len({c["code"] for c in criteria}) == len(criteria), "duplicate or empty criteria")
        try:
            weights = [Decimal(str(c["weight"])) for c in criteria]
            require(all(w.is_finite() and 0 < w <= 1 and w.as_tuple().exponent >= -2 for w in weights) and sum(weights) == 1, "invalid weight sum or precision")
        except InvalidOperation as error:
            raise ValueError("invalid weight") from error
        require(all(type(c["required"]) is bool for c in criteria) and any(c["required"] for c in criteria), "required concept missing")
        for criterion in criteria:
            code = criterion["code"]
            require(code in concepts and concepts[code]["topicCode"] == question["topicCode"], "question concept topic mismatch")
            require(code in document["conceptCodes"], "document concept coverage missing")
        require(question["sourceRefs"], "question source missing")
        for source in question["sourceRefs"]:
            require(source["sourceId"] in document["sourceIds"] and source["sourceId"] in sources and source["locator"].strip(), "question source or locator missing")
        coverage[question["topicCode"]] += 1
        if reference_questions:
            closest = max(reference_questions, key=lambda q: difflib.SequenceMatcher(None, text, normalized(q["content"])).ratio())
            ratio = difflib.SequenceMatcher(None, text, normalized(closest["content"])).ratio()
            similarities.append({"questionKey": question["key"], "referenceId": closest.get("id", closest.get("questionId")), "textSimilarity": round(ratio, 3)})
    require(all(coverage[code] >= minimum_questions for code in topics), "topic minimum not met")
    return {"version": bundle["version"], "status": "DRAFT", "questionCount": len(questions), "documentCount": len(documents),
            "conceptCount": len(concepts), "questionsByTopic": dict(coverage), "nearestReferenceQuestions": similarities,
            "similarityLimit": "Text comparison only; human semantic/bias review remains necessary."}


def reuse_or_create(rows, payload, identity_fields, create, draft_only=False):
    matches = [row for row in rows if all(row.get(key) == payload[key] for key in identity_fields)]
    require(len(matches) <= 1, "ambiguous existing records; resolve manually")
    if not matches:
        return create(payload)
    match = matches[0]
    require(not draft_only or match.get("status") == "DRAFT", "existing content is not DRAFT")
    require(all(match.get(key) == value for key, value in payload.items()), "existing content conflict; no overwrite")
    require(match.get("active", True), "inactive taxonomy conflict")
    require(not draft_only or match.get("reviewedAt") is None, "reviewed draft conflict; no overwrite")
    return match


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("API redirects are not allowed")


class LocalAdminClient:
    def __init__(self, base_url):
        parsed = urlparse(base_url)
        require(parsed.scheme == "http" and parsed.hostname in {"127.0.0.1", "localhost", "::1"}
                and not parsed.username and parsed.path in {"", "/"} and not parsed.query and not parsed.fragment,
                "only local HTTP origins are supported")
        self.base_url = base_url.rstrip("/")
        self.opener = build_opener(HTTPCookieProcessor(CookieJar()), NoRedirect())

    def request(self, method, path, payload=None):
        headers = {"Accept": "application/json"}
        if method != "GET":
            csrf = self.request("GET", "/api/auth/csrf")
            headers[csrf["headerName"]] = csrf["token"]
        data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode()
        if data is not None:
            headers["Content-Type"] = "application/json"
        try:
            with self.opener.open(Request(self.base_url + path, data=data, headers=headers, method=method), timeout=30) as response:
                body = response.read()
                return json.loads(body) if body else None
        except HTTPError as error:
            # Do not print response bodies, cookies or credentials.
            raise ValueError(f"{method} {path}: HTTP {error.code}") from None

    def all_pages(self, path):
        rows = []
        page = 0
        while True:
            result = self.request("GET", f"{path}?page={page}&size=100&sort=id,asc")
            rows.extend(result["content"])
            page += 1
            if page >= result["totalPages"]:
                return rows


def import_drafts(bundle, client):
    member = client.request("GET", "/api/members/me")
    require(member["role"] == "ADMIN" and member["status"] == "ACTIVE", "active administrator required")
    paths = {"topics": "/api/admin/topics", "concepts": "/api/admin/concepts", "documents": "/api/admin/knowledge-documents", "questions": "/api/admin/questions"}
    existing = {kind: client.all_pages(path) for kind, path in paths.items()}
    # Read all matching question details before writes; list API intentionally omits reference answers.
    question_texts = {q["content"] for q in bundle["questions"]}
    existing["questions"] = [client.request("GET", f"{paths['questions']}/{q['id']}") for q in existing["questions"] if q["content"] in question_texts]
    ids = {kind: {} for kind in paths}

    def put_draft(kind, key, payload, identity, draft_only=False):
        def create(body):
            created = client.request("POST", paths[kind], body)
            existing[kind].append(created)
            return created
        row = reuse_or_create(existing[kind], payload, identity, create, draft_only)
        ids[kind][key] = row["id"]
        return row

    for topic in bundle["topics"]:
        put_draft("topics", topic["code"], {"code": topic["code"], "name": topic["name"], "parentId": None}, ["code"])
    for concept in bundle["concepts"]:
        put_draft("concepts", concept["code"], {"topicId": ids["topics"][concept["topicCode"]], "code": concept["code"], "name": concept["name"], "description": concept["description"]}, ["code"])
    sources = {source["id"]: source for source in bundle["sources"]}
    for document in bundle["documents"]:
        source_list = [sources[key] for key in document["sourceIds"]]
        put_draft("documents", document["key"], {
            "topicId": ids["topics"][document["topicCode"]], "title": document["title"],
            "sourceType": "INTERNAL_SUMMARY", "sourceUrl": source_list[0]["url"],
            "technologyVersion": document["technologyVersion"],
            "licenseNote": "직접 작성한 한국어 요약. 원문·코드·그림 재배포 없음. 출처별 이용 조건은 bundle.json 참조. 사람 검수 및 공개 승인 대기.",
            "content": document["content"],
        }, ["topicId", "title"], True)
    for question in bundle["questions"]:
        row = put_draft("questions", question["key"], {
            "topicId": ids["topics"][question["topicCode"]], "difficulty": question["difficulty"],
            "content": question["content"], "referenceAnswer": question["referenceAnswer"],
        }, ["topicId", "content"], True)
        expected = [{"conceptId": ids["concepts"][c["code"]], "weight": float(c["weight"]), "required": c["required"]} for c in question["concepts"]]
        actual = [{key: c[key] for key in ("conceptId", "weight", "required")} for c in row["concepts"]]
        if actual:
            require(sorted(actual, key=lambda c: c["conceptId"]) == sorted(expected, key=lambda c: c["conceptId"]), "existing criteria conflict; no overwrite")
        else:
            client.request("PUT", f"{paths['questions']}/{row['id']}/concepts", {"concepts": expected})
    return ids


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bundle", type=Path, default=DEFAULT_BUNDLE)
    parser.add_argument("--apply", action="store_true", help="Register unreviewed DRAFTs only; never publish")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--admin-email", default="admin@crackcs.local")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    raw = args.bundle.read_bytes()
    bundle = json.loads(raw)
    report = validate_bundle(bundle, [json.loads(line) for line in REFERENCE.read_text().splitlines() if line.strip()])
    report["bundleSha256"] = hashlib.sha256(raw).hexdigest()
    if args.apply:
        client = LocalAdminClient(args.base_url)
        password = getpass.getpass("Local administrator password: ")
        client.request("POST", "/api/auth/login", {"email": args.admin_email, "password": password})
        del password
        try:
            report["registeredIds"] = import_drafts(bundle, client)
        finally:
            client.request("POST", "/api/auth/logout")
    encoded = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(encoded)
    print(encoded)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError) as error:
        print(f"Content import stopped: {error}. Existing drafts are preserved; resolve conflicts before rerunning.", file=sys.stderr)
        sys.exit(1)
