#!/usr/bin/env python3
"""Probe Lociant as a generic OpenAI-compatible agent backend.

The script is intentionally desktop-side and standard-library only. Use
``smoke`` to verify the protocol surface, or ``proxy`` to put a logging proxy
between an agent client and Lociant so you can see what the client actually
sends.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


DEFAULT_BASE_URL = "http://127.0.0.1:11434"
DEFAULT_LISTEN = "127.0.0.1"
DEFAULT_PROXY_PORT = 11435
DEFAULT_TOOL = "runtime_status"


@dataclass
class HttpResult:
    status: int
    headers: dict[str, str]
    body: bytes
    elapsed_ms: int


def json_bytes(payload: dict[str, Any]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def api_url(base_url: str, path: str) -> str:
    base = base_url.rstrip("/")
    if base.endswith("/v1") and path.startswith("/v1/"):
        base = base[:-3]
    if base.endswith("/v1") and (path == "/health" or path.startswith("/api/")):
        base = base[:-3]
    return f"{base}{path}"


def request(
    method: str,
    url: str,
    *,
    payload: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
    timeout: int = 90,
) -> HttpResult:
    data = json_bytes(payload) if payload is not None else None
    req_headers = {"Accept": "application/json", **(headers or {})}
    if payload is not None:
        req_headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=req_headers, method=method)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read()
            status = resp.status
            resp_headers = dict(resp.headers.items())
    except urllib.error.HTTPError as error:
        body = error.read()
        status = error.code
        resp_headers = dict(error.headers.items())
    elapsed_ms = int((time.monotonic() - started) * 1000)
    return HttpResult(status, resp_headers, body, elapsed_ms)


def parse_json(result: HttpResult) -> dict[str, Any]:
    try:
        return json.loads(result.body.decode("utf-8"))
    except json.JSONDecodeError as error:
        raise RuntimeError(f"invalid JSON response: {error}: {result.body[:300]!r}") from error


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def ok(name: str, detail: str) -> None:
    print(f"[OK] {name}: {detail}")


def choose_model(base_url: str, requested: str, timeout: int) -> str:
    if requested:
        return requested
    result = request("GET", api_url(base_url, "/v1/models"), timeout=timeout)
    if result.status != 200:
        fail(f"/v1/models returned HTTP {result.status}: {result.body[:300]!r}")
    models = parse_json(result).get("data", [])
    model = next((item.get("id") for item in models if item.get("id")), "")
    if not model:
        fail("/v1/models returned no model ids")
    return model


def require_completion(value: dict[str, Any], *, allow_tool_calls: bool = False) -> dict[str, Any]:
    if value.get("object") != "chat.completion":
        fail(f"not an OpenAI chat.completion response: {value}")
    choices = value.get("choices")
    if not isinstance(choices, list) or not choices:
        fail(f"response missing choices: {value}")
    choice = choices[0]
    message = choice.get("message") or {}
    if message.get("role") != "assistant":
        fail(f"assistant message missing: {value}")
    calls = message.get("tool_calls")
    if allow_tool_calls and not isinstance(calls, list):
        fail(f"response missing tool_calls: {value}")
    if not allow_tool_calls and not message.get("content"):
        fail(f"response content is empty: {value}")
    return choice


def run_smoke(args: argparse.Namespace) -> int:
    base_url = args.base_url.rstrip("/")
    model = choose_model(base_url, args.model, args.timeout)
    auth_headers = {"Authorization": f"Bearer {args.api_key}"} if args.api_key else {}

    health = request("GET", api_url(base_url, "/health"), headers=auth_headers, timeout=args.timeout)
    if health.status != 200:
        fail(f"/health returned HTTP {health.status}: {health.body[:300]!r}")
    ok("health", f"{health.elapsed_ms}ms")

    tools = request("GET", api_url(base_url, "/v1/tools"), headers=auth_headers, timeout=args.timeout)
    if tools.status != 200:
        fail(f"/v1/tools returned HTTP {tools.status}: {tools.body[:300]!r}")
    tool_defs = parse_json(tools).get("data", [])
    tool_names = [item.get("function", {}).get("name") for item in tool_defs if item.get("type") == "function"]
    tool = args.tool if args.tool in tool_names else next((name for name in tool_names if name), "")
    if not tool:
        fail("/v1/tools returned no function tools")
    tool_def = next(item for item in tool_defs if item.get("function", {}).get("name") == tool)
    ok("tools manifest", f"{tools.elapsed_ms}ms count={len(tool_names)} selected={tool}")

    normal_payload = {
        "model": model,
        "messages": [{"role": "user", "content": "Reply with exactly: agent api ok"}],
        "stream": False,
        "max_tokens": 24,
        "metadata": {"probe": "openai-agent"},
        "parallel_tool_calls": True,
        "store": False,
    }
    normal = request(
        "POST",
        api_url(base_url, "/v1/chat/completions"),
        payload=normal_payload,
        headers=auth_headers,
        timeout=args.timeout,
    )
    normal_json = parse_json(normal)
    if normal.status != 200:
        fail(f"ordinary chat with common agent fields failed HTTP {normal.status}: {normal_json}")
    require_completion(normal_json)
    ok("ordinary chat", f"{normal.elapsed_ms}ms unknown OpenAI fields tolerated")

    forced_payload = {
        "model": model,
        "messages": [{"role": "user", "content": "Call the selected tool."}],
        "tools": [tool_def],
        "tool_choice": {"type": "function", "function": {"name": tool}},
        "stream": False,
    }
    forced = request(
        "POST",
        api_url(base_url, "/v1/chat/completions"),
        payload=forced_payload,
        headers=auth_headers,
        timeout=args.timeout,
    )
    forced_json = parse_json(forced)
    if forced.status != 200:
        fail(f"forced tool_choice failed HTTP {forced.status}: {forced_json}")
    forced_choice = require_completion(forced_json, allow_tool_calls=True)
    if forced_choice.get("finish_reason") != "tool_calls":
        fail(f"forced tool_choice did not finish with tool_calls: {forced_json}")
    call = forced_choice["message"]["tool_calls"][0]
    if call.get("function", {}).get("name") != tool:
        fail(f"forced tool_choice returned wrong tool: {forced_json}")
    ok("forced tool_choice", f"{forced.elapsed_ms}ms id={call.get('id')} name={tool}")

    required_payload = dict(forced_payload)
    required_payload["tool_choice"] = "required"
    required = request(
        "POST",
        api_url(base_url, "/v1/chat/completions"),
        payload=required_payload,
        headers=auth_headers,
        timeout=args.timeout,
    )
    required_json = parse_json(required)
    if required.status != 200:
        fail(f"required tool_choice failed HTTP {required.status}: {required_json}")
    required_choice = require_completion(required_json, allow_tool_calls=True)
    if required_choice.get("finish_reason") != "tool_calls":
        fail(f"required tool_choice did not finish with tool_calls: {required_json}")
    ok("required tool_choice", f"{required.elapsed_ms}ms")

    auto_payload = {
        "model": model,
        "messages": [{"role": "user", "content": "Please read main.cpp and summarize it."}],
        "tools": [tool_def],
        "tool_choice": "auto",
        "stream": False,
    }
    auto = request(
        "POST",
        api_url(base_url, "/v1/chat/completions"),
        payload=auto_payload,
        headers=auth_headers,
        timeout=args.timeout,
    )
    auto_json = parse_json(auto)
    if auto.status != 200:
        fail(f"auto tool_choice failed HTTP {auto.status}: {auto_json}")
    auto_choice = require_completion(auto_json, allow_tool_calls=True)
    if auto_choice.get("finish_reason") != "tool_calls":
        fail(f"auto tool_choice did not finish with tool_calls: {auto_json}")
    ok("auto tool_choice", f"{auto.elapsed_ms}ms")

    direct_tool = request(
        "POST",
        api_url(base_url, f"/v1/tools/{tool}/call"),
        payload={"arguments": {}},
        headers=auth_headers,
        timeout=args.timeout,
    )
    direct_tool_json = parse_json(direct_tool)
    if direct_tool.status != 200 or not direct_tool_json.get("ok"):
        fail(f"direct tool call failed HTTP {direct_tool.status}: {direct_tool_json}")

    followup_payload = {
        "model": model,
        "messages": [
            {"role": "user", "content": "Summarize the tool result briefly."},
            forced_choice["message"],
            {
                "role": "tool",
                "tool_call_id": call.get("id"),
                "content": json.dumps(direct_tool_json, ensure_ascii=False),
            },
        ],
        "tool_choice": "none",
        "stream": False,
        "max_tokens": 40,
    }
    followup = request(
        "POST",
        api_url(base_url, "/v1/chat/completions"),
        payload=followup_payload,
        headers=auth_headers,
        timeout=args.timeout,
    )
    followup_json = parse_json(followup)
    if followup.status != 200:
        fail(f"role=tool follow-up failed HTTP {followup.status}: {followup_json}")
    followup_choice = require_completion(followup_json)
    ok("role=tool follow-up", f"{followup.elapsed_ms}ms text={followup_choice['message']['content'][:80]!r}")

    print("[DONE] Lociant passes the generic OpenAI agent protocol probe.")
    return 0


def summarize_request(method: str, path: str, body: bytes) -> str:
    if not body:
        return f"{method} {path}"
    try:
        payload = json.loads(body.decode("utf-8"))
    except Exception:
        return f"{method} {path} body={len(body)} bytes"
    if path.rstrip("/").endswith("/chat/completions"):
        messages = payload.get("messages") if isinstance(payload, dict) else []
        roles = [item.get("role", "?") for item in messages if isinstance(item, dict)]
        tools = payload.get("tools") if isinstance(payload, dict) else None
        tool_names = [
            item.get("function", {}).get("name", "")
            for item in tools
            if isinstance(item, dict)
        ] if isinstance(tools, list) else []
        tool_choice = payload.get("tool_choice") if isinstance(payload, dict) else None
        last_user = next((item.get("content", "") for item in reversed(messages) if isinstance(item, dict) and item.get("role") == "user"), "")
        return (
            f"{method} {path} model={payload.get('model')} stream={payload.get('stream')} "
            f"messages={roles} tools={len(tools) if isinstance(tools, list) else 0} "
            f"tool_names={tool_names[:8]} tool_choice={tool_choice!r} last_user={str(last_user)[:80]!r}"
        )
    return f"{method} {path} json_keys={list(payload.keys()) if isinstance(payload, dict) else type(payload).__name__}"


def summarize_response(path: str, body: bytes) -> str:
    if not body:
        return "empty response"
    text = body.decode("utf-8", errors="replace")
    if "data:" in text and path.rstrip("/").endswith("/chat/completions"):
        has_tool = '"tool_calls"' in text
        done = "[DONE]" in text
        return f"sse done={done} tool_calls={has_tool} {summarize_sse_tool_call(text)} bytes={len(body)}"
    try:
        payload = json.loads(text)
    except Exception:
        return f"body={len(body)} bytes"
    if path.rstrip("/").endswith("/chat/completions"):
        choices = payload.get("choices") or []
        choice = choices[0] if choices else {}
        message = choice.get("message") or {}
        calls = message.get("tool_calls")
        tool_detail = summarize_tool_calls(calls)
        return (
            f"finish={choice.get('finish_reason')} content={bool(message.get('content'))} "
            f"tool_calls={len(calls) if isinstance(calls, list) else 0} {tool_detail}"
        )
    return f"json_keys={list(payload.keys()) if isinstance(payload, dict) else type(payload).__name__}"


def summarize_sse_tool_call(text: str) -> str:
    for line in text.splitlines():
        if not line.startswith("data: ") or '"tool_calls"' not in line:
            continue
        try:
            payload = json.loads(line[6:])
        except Exception:
            continue
        choices = payload.get("choices") or []
        delta = (choices[0].get("delta") if choices else {}) or {}
        calls = delta.get("tool_calls")
        detail = summarize_tool_calls(calls)
        if detail:
            return detail
    return ""


def summarize_tool_calls(calls: Any) -> str:
    if not isinstance(calls, list) or not calls:
        return ""
    function = (calls[0] or {}).get("function") or {}
    name = function.get("name", "")
    raw_args = function.get("arguments", "")
    try:
        parsed_args = json.loads(raw_args) if isinstance(raw_args, str) else raw_args
    except Exception:
        parsed_args = raw_args
    return f"tool={name} args={str(parsed_args)[:180]!r}"


class ProxyHandler(BaseHTTPRequestHandler):
    upstream = DEFAULT_BASE_URL
    timeout = 300

    def do_GET(self) -> None:
        self.forward()

    def do_POST(self) -> None:
        self.forward()

    def do_OPTIONS(self) -> None:
        self.forward()

    def log_message(self, format: str, *args: Any) -> None:
        return

    def forward(self) -> None:
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length > 0 else b""
        print(f"[REQ] {summarize_request(self.command, self.path, body)}", flush=True)

        headers = {
            key: value for key, value in self.headers.items()
            if key.lower() not in {"host", "content-length", "connection", "accept-encoding"}
        }
        target = api_url(self.upstream, self.path)
        req = urllib.request.Request(target, data=body if body else None, headers=headers, method=self.command)
        started = time.monotonic()
        status = 502
        response_headers: dict[str, str] = {}
        response_body = b""
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                status = resp.status
                response_headers = dict(resp.headers.items())
                response_body = resp.read()
        except urllib.error.HTTPError as error:
            status = error.code
            response_headers = dict(error.headers.items())
            response_body = error.read()
        except Exception as error:
            status = 502
            response_headers = {"Content-Type": "application/json"}
            response_body = json_bytes({"error": {"message": str(error)}})

        self.send_response(status)
        for key, value in response_headers.items():
            if key.lower() not in {"transfer-encoding", "connection", "content-encoding", "content-length"}:
                self.send_header(key, value)
        self.send_header("Content-Length", str(len(response_body)))
        self.end_headers()
        self.wfile.write(response_body)
        elapsed_ms = int((time.monotonic() - started) * 1000)
        print(f"[RESP] {status} {elapsed_ms}ms {summarize_response(self.path, response_body)}", flush=True)


def run_proxy(args: argparse.Namespace) -> int:
    ProxyHandler.upstream = args.base_url.rstrip("/")
    ProxyHandler.timeout = args.timeout
    server = ThreadingHTTPServer((args.listen, args.port), ProxyHandler)
    print(f"[PROXY] listen http://{args.listen}:{args.port}/v1 -> {ProxyHandler.upstream}")
    print("[PROXY] configure your agent base_url to the listen URL above. Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[PROXY] stopped")
    finally:
        server.server_close()
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Probe Lociant OpenAI agent compatibility from a desktop.")
    sub = parser.add_subparsers(dest="command")

    smoke = sub.add_parser("smoke", help="actively test the OpenAI agent protocol surface")
    smoke.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"default: {DEFAULT_BASE_URL}")
    smoke.add_argument("--model", default="", help="model id; defaults to first /v1/models id")
    smoke.add_argument("--tool", default=DEFAULT_TOOL, help=f"default: {DEFAULT_TOOL}")
    smoke.add_argument("--api-key", default="", help="optional Authorization bearer token")
    smoke.add_argument("--timeout", type=int, default=120)
    smoke.set_defaults(func=run_smoke)

    proxy = sub.add_parser("proxy", help="log and forward a real agent client's OpenAI requests")
    proxy.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"upstream Lociant URL, default: {DEFAULT_BASE_URL}")
    proxy.add_argument("--listen", default=DEFAULT_LISTEN, help=f"default: {DEFAULT_LISTEN}")
    proxy.add_argument("--port", type=int, default=DEFAULT_PROXY_PORT, help=f"default: {DEFAULT_PROXY_PORT}")
    proxy.add_argument("--timeout", type=int, default=300)
    proxy.set_defaults(func=run_proxy)

    args = parser.parse_args()
    if not hasattr(args, "func"):
        parser.print_help()
        return 2
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
