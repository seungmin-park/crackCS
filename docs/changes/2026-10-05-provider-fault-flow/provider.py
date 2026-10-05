#!/usr/bin/env python3
"""Local injected faults and synthetic Responses API success; never calls OpenAI."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import threading
import time

lock = threading.Lock()
state = {"mode": "success", "failures": 0, "attempts": 0, "events": []}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

    def respond(self, status, body):
        encoded = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        try:
            self.wfile.write(encoded)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def do_GET(self):
        with lock:
            self.respond(200, state)

    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if self.path == "/control":
            assert body["mode"] in {"success", "timeout", "429", "503"}
            assert type(body["failures"]) is int and 0 <= body["failures"] <= 3
            with lock:
                state.update({"mode": body["mode"], "failures": body["failures"], "attempts": 0, "events": []})
            self.respond(200, {"scope": "CONTROLLED_LOCAL_ONLY"})
            return
        if self.path != "/v1/responses":
            self.respond(404, {})
            return
        with lock:
            state["attempts"] += 1
            attempt = state["attempts"]
            mode = state["mode"] if attempt <= state["failures"] else "success"
            state["events"].append({"attempt": attempt, "injected": mode, "synthetic": True})
        print(f"Controlled attempt {attempt}: {mode}", flush=True)
        if mode == "timeout":
            time.sleep(4)  # Application timeout is 2s; exercises JDK HttpTimeoutException.
            self.respond(503, {"error": {"code": "injected_timeout"}})
            return
        if mode in {"429", "503"}:
            self.respond(int(mode), {"error": {"code": "rate_limit_exceeded" if mode == "429" else "injected_503"}})
            return
        text = body["input"][1]["content"][0]["text"]
        request = json.loads(text.split("<DATA>\n", 1)[1].rsplit("\n</DATA>", 1)[0])
        evaluation = {"verdict": "CORRECT", "feedback": "통제된 HTTP 계약 검증 응답. 실제 GPT 채점이 아닙니다.",
                      "strengths": [], "omissions": [], "misconceptions": [],
                      "concepts": [{"conceptId": c["conceptId"], "verdict": "CORRECT", "feedback": "통제된 합성 판정"} for c in request["concepts"]],
                      "evidenceChunkIds": [e["chunkId"] for e in request["evidence"]]}
        self.respond(200, {"output": [{"type": "message", "content": [{"type": "output_text", "text": json.dumps(evaluation, ensure_ascii=False)}]}],
                           "usage": {"input_tokens": 0, "output_tokens": 0}})


print("Controlled local provider: 127.0.0.1:18082; external calls 0", flush=True)
ThreadingHTTPServer(("127.0.0.1", 18082), Handler).serve_forever()
