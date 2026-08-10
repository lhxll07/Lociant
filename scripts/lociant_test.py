#!/usr/bin/env python3
"""Unified desktop-side test and debug tool for Lociant.

No third-party dependencies. Use it to test the phone runtime, OpenAI-compatible
chat, tools, MCP, streaming, and to proxy/log real agent traffic.
"""

from __future__ import annotations

import argparse
import base64
import json
import mimetypes
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


DEFAULT_BASE_URL = "http://10.238.125.4:11434"
DEFAULT_LISTEN = "127.0.0.1"
DEFAULT_PROXY_PORT = 11435
DEFAULT_TOOL = "runtime_status"
MCP_PROTOCOL_VERSION = "2025-06-18"

# MCP Streamable HTTP is session-scoped: initialize returns Mcp-Session-Id and
# every later request must echo it back. The script tracks it globally because
# the whole run is a single linear session.
MCP_SESSION_ID: str | None = None


@dataclass
class HttpResult:
    status: int
    headers: dict[str, str]
    body: bytes
    elapsed_ms: int


def json_bytes(payload: Any) -> bytes:
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def api_url(base_url: str, path: str) -> str:
    base = base_url.rstrip("/")
    if base.endswith("/v1") and (path == "/health" or path.startswith("/v1/") or path.startswith("/api/") or path.startswith("/mcp")):
        base = base[:-3]
    return f"{base}{path}"


def bearer(api_key: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {api_key}"} if api_key else {}


def request(
    method: str,
    url: str,
    *,
    payload: Any | None = None,
    headers: dict[str, str] | None = None,
    timeout: int = 60,
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
            response_headers = dict(resp.headers.items())
    except urllib.error.HTTPError as error:
        body = error.read()
        status = error.code
        response_headers = dict(error.headers.items())
    except OSError as error:
        fail(f"{method} {url} failed: {error}")
    return HttpResult(status, response_headers, body, int((time.monotonic() - started) * 1000))


def request_stream(url: str, *, payload: dict[str, Any], headers: dict[str, str], timeout: int, max_events: int = 120) -> tuple[int, dict[str, str], list[tuple[int, str]], int]:
    req = urllib.request.Request(
        url,
        data=json_bytes(payload),
        headers={"Content-Type": "application/json", "Accept": "text/event-stream", **headers},
        method="POST",
    )
    started = time.monotonic()
    events: list[tuple[int, str]] = []
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            for raw_line in resp:
                line = raw_line.decode("utf-8", errors="replace").strip()
                if not line:
                    continue
                events.append((int((time.monotonic() - started) * 1000), line))
                if line == "data: [DONE]" or len(events) >= max_events:
                    break
            return resp.status, dict(resp.headers.items()), events, int((time.monotonic() - started) * 1000)
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers.items()), [(0, error.read().decode("utf-8", "replace"))], int((time.monotonic() - started) * 1000)


def parse_json(result: HttpResult) -> dict[str, Any]:
    try:
        value = json.loads(result.body.decode("utf-8"))
    except json.JSONDecodeError as error:
        fail(f"invalid JSON HTTP {result.status}: {error}: {result.body[:500]!r}")
    if not isinstance(value, dict):
        fail(f"expected JSON object, got {type(value).__name__}: {value!r}")
    return value


def expect_json(name: str, result: HttpResult, status: int = 200) -> dict[str, Any]:
    if result.status != status:
        fail(f"{name} returned HTTP {result.status}, expected {status}: {result.body[:500]!r}")
    return parse_json(result)


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def ok(name: str, detail: str = "") -> None:
    print(f"[OK] {name}{': ' + detail if detail else ''}")


def warn(name: str, detail: str) -> None:
    print(f"[WARN] {name}: {detail}")


def tool_name(item: dict[str, Any]) -> str:
    function = item.get("function") if isinstance(item.get("function"), dict) else {}
    return str(function.get("name") or "")


def list_models(base_url: str, headers: dict[str, str], timeout: int) -> list[str]:
    result = request("GET", api_url(base_url, "/v1/models"), headers=headers, timeout=timeout)
    data = expect_json("models", result)
    models = data.get("data")
    if not isinstance(models, list):
        fail(f"/v1/models missing data list: {data}")
    ids = [str(item.get("id")) for item in models if isinstance(item, dict) and item.get("id")]
    ok("models", f"{result.elapsed_ms}ms count={len(ids)} first={ids[0] if ids else 'none'}")
    return ids


def list_tools(base_url: str, headers: dict[str, str], timeout: int) -> tuple[list[dict[str, Any]], list[str]]:
    result = request("GET", api_url(base_url, "/api/v1/tools"), headers=headers, timeout=timeout)
    data = expect_json("tools", result)
    items = data.get("data")
    if not isinstance(items, list):
        fail(f"/api/v1/tools missing data list: {data}")
    tools = [item for item in items if isinstance(item, dict)]
    names = [tool_name(item) for item in tools if tool_name(item)]
    levels = sorted({str(item.get("x_lociant_level", "")) for item in tools if item.get("x_lociant_level")})
    ok("tools", f"{result.elapsed_ms}ms count={len(names)} levels={','.join(levels)}")
    return tools, names


def call_tool(base_url: str, headers: dict[str, str], timeout: int, tool: str) -> dict[str, Any]:
    result = request("POST", api_url(base_url, f"/api/v1/tools/{tool}/calls"), payload={"arguments": {}}, headers=headers, timeout=timeout)
    data = expect_json(f"tool {tool}", result)
    if not data.get("ok"):
        fail(f"tool {tool} returned not ok: {data}")
    ok("tool call", f"{result.elapsed_ms}ms tool={tool}")
    return data


def mcp_call(base_url: str, headers: dict[str, str], timeout: int, method: str, params: dict[str, Any] | None = None) -> dict[str, Any]:
    global MCP_SESSION_ID
    session_headers = {"Mcp-Session-Id": MCP_SESSION_ID} if MCP_SESSION_ID else {}
    result = request(
        "POST",
        api_url(base_url, "/mcp"),
        payload={"jsonrpc": "2.0", "id": method, "method": method, "params": params or {}},
        headers={
            "MCP-Protocol-Version": MCP_PROTOCOL_VERSION,
            "Accept": "application/json, text/event-stream",
            **session_headers,
            **headers,
        },
        timeout=timeout,
    )
    for key, value in result.headers.items():
        if key.lower() == "mcp-session-id" and value:
            MCP_SESSION_ID = value
    data = expect_json(f"mcp {method}", result)
    if "error" in data:
        fail(f"MCP {method} returned error: {data}")
    return data


def mcp_tool_call(base_url: str, headers: dict[str, str], timeout: int, name: str, arguments: dict[str, Any] | None = None) -> dict[str, Any]:
    data = mcp_call(base_url, headers, timeout, "tools/call", {"name": name, "arguments": arguments or {}})
    result = data.get("result")
    if not isinstance(result, dict):
        fail(f"MCP tools/call {name} missing result: {data}")
    if result.get("isError"):
        fail(f"MCP tools/call {name} returned error: {result}")
    return result


def chat_once(base_url: str, headers: dict[str, str], timeout: int, model: str, prompt: str, max_tokens: int) -> dict[str, Any]:
    payload = {
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "stream": False,
        "max_tokens": max_tokens,
    }
    result = request("POST", api_url(base_url, "/v1/chat/completions"), payload=payload, headers=headers, timeout=timeout)
    data = expect_json("chat", result)
    choice = require_completion(data)
    usage = data.get("usage") if isinstance(data.get("usage"), dict) else {}
    text = str((choice.get("message") or {}).get("content") or "")
    ok("chat", f"{result.elapsed_ms}ms prompt={usage.get('prompt_tokens')} out={usage.get('completion_tokens')} text={text[:80]!r}")
    return data


def image_data_url(path: str) -> str:
    mime_type = mimetypes.guess_type(path)[0] or "image/jpeg"
    try:
        with open(path, "rb") as file:
            encoded = base64.b64encode(file.read()).decode("ascii")
    except OSError as error:
        fail(f"cannot read image {path!r}: {error}")
    return f"data:{mime_type};base64,{encoded}"


def mcp_llm_once(base_url: str, headers: dict[str, str], timeout: int, prompt: str, max_tokens: int, image_path: str = "") -> dict[str, Any]:
    status = mcp_tool_call(base_url, headers, timeout, "llm_status")
    structured = status.get("structuredContent") if isinstance(status.get("structuredContent"), dict) else {}
    ok("mcp llm_status", f"running={structured.get('running')} model={structured.get('modelId')} loaded={structured.get('modelLoaded')}")

    arguments = {"prompt": prompt, "maxTokens": max_tokens}
    if image_path:
        arguments["image"] = image_data_url(image_path)
    result = mcp_tool_call(
        base_url,
        headers,
        timeout,
        "llm_chat",
        arguments,
    )
    content = result.get("structuredContent") if isinstance(result.get("structuredContent"), dict) else {}
    text = str(content.get("text") or "")
    if not text:
        fail(f"MCP llm_chat returned empty text: {result}")
    ok("mcp llm_chat", f"text={text[:80]!r}")
    return result


def require_completion(value: dict[str, Any], *, allow_tool_calls: bool = False) -> dict[str, Any]:
    if value.get("object") != "chat.completion":
        fail(f"not a chat.completion response: {value}")
    choices = value.get("choices")
    if not isinstance(choices, list) or not choices:
        fail(f"response missing choices: {value}")
    choice = choices[0]
    if not isinstance(choice, dict):
        fail(f"invalid choice: {value}")
    message = choice.get("message")
    if not isinstance(message, dict) or message.get("role") != "assistant":
        fail(f"assistant message missing: {value}")
    calls = message.get("tool_calls")
    if allow_tool_calls and not isinstance(calls, list):
        fail(f"response missing tool_calls: {value}")
    if not allow_tool_calls and not message.get("content"):
        fail(f"response content is empty: {value}")
    return choice


def run_quick(args: argparse.Namespace) -> int:
    run_quick_checks(args, print_done=True)
    return 0


def run_quick_checks(args: argparse.Namespace, *, print_done: bool) -> tuple[list[str], list[dict[str, Any]], list[str]]:
    base_url = args.base_url.rstrip("/")
    headers = bearer(args.api_key)

    health_result = request("GET", api_url(base_url, "/health"), timeout=args.timeout)
    health = expect_json("health", health_result)
    ok("health", f"{health_result.elapsed_ms}ms running={health.get('running')} model={health.get('modelId') or health.get('model')}")
    if "authToken" in health:
        fail("/health leaked authToken")

    if args.expect_auth:
        unauth = request("GET", api_url(base_url, "/api/v1/tools"), timeout=args.timeout)
        if unauth.status != 401:
            fail(f"expected unauthenticated /api/v1/tools to return 401, got {unauth.status}")
        ok("auth", "unauthenticated /api/v1/tools returned 401")

    models = list_models(base_url, headers, args.timeout)
    tools, names = list_tools(base_url, headers, args.timeout)
    if args.tool not in names:
        fail(f"tool {args.tool!r} not listed: {names}")
    call_tool(base_url, headers, args.timeout, args.tool)

    init = mcp_call(
        base_url,
        headers,
        args.timeout,
        "initialize",
        {
            "protocolVersion": MCP_PROTOCOL_VERSION,
            "capabilities": {},
            "clientInfo": {"name": "lociant_test", "version": "1.0"},
        },
    )
    info = (init.get("result") or {}).get("serverInfo") if isinstance(init.get("result"), dict) else {}
    ok("mcp initialize", f"name={(info or {}).get('name')} version={(info or {}).get('version')}")

    listed = mcp_call(base_url, headers, args.timeout, "tools/list")
    mcp_tools = (listed.get("result") or {}).get("tools") if isinstance(listed.get("result"), dict) else []
    if not isinstance(mcp_tools, list):
        fail(f"MCP tools/list missing tools array: {listed}")
    if args.tool not in [item.get("name") for item in mcp_tools if isinstance(item, dict)]:
        fail(f"MCP tools/list missing {args.tool!r}")
    ok("mcp tools/list", f"count={len(mcp_tools)} selected={args.tool}")

    if args.chat:
        model = args.model or (models[0] if models else "")
        if not model:
            fail("chat requested but no model is available")
        chat_once(base_url, headers, args.chat_timeout, model, args.prompt, args.max_tokens)
    else:
        warn("chat", "skipped; pass --chat to test /v1/chat/completions")

    if args.mcp_llm:
        if "llm_status" not in names or "llm_chat" not in names:
            fail(f"MCP LLM tools not listed in /api/v1/tools: {names}")
        mcp_llm_once(base_url, headers, args.chat_timeout, args.prompt, args.max_tokens, args.mcp_llm_image)

    if print_done:
        print("[DONE] quick probe passed.")
    return models, tools, names


def run_full(args: argparse.Namespace) -> int:
    base_url = args.base_url.rstrip("/")
    headers = bearer(args.api_key)
    models, tool_defs, names = run_quick_checks(args, print_done=False)
    model = args.model or (models[0] if models else "")
    if not model:
        fail("no model available")

    tool = args.tool if args.tool in names else (names[0] if names else "")
    if not tool:
        fail("no tool available for OpenAI tool test")
    tool_def = next(item for item in tool_defs if tool_name(item) == tool)

    normal = chat_once(base_url, headers, args.chat_timeout, model, args.prompt, args.max_tokens)
    usage = normal.get("usage")
    if not isinstance(usage, dict) or not isinstance(usage.get("total_tokens"), int):
        fail(f"chat usage missing token counts: {normal}")

    forced_payload = {
        "model": model,
        "messages": [{"role": "user", "content": "Call the selected tool."}],
        "tools": [tool_def],
        "tool_choice": {"type": "function", "function": {"name": tool}},
        "stream": False,
    }
    forced = request("POST", api_url(base_url, "/v1/chat/completions"), payload=forced_payload, headers=headers, timeout=args.timeout)
    forced_json = expect_json("forced tool_choice", forced)
    forced_choice = require_completion(forced_json, allow_tool_calls=True)
    if forced_choice.get("finish_reason") != "tool_calls":
        fail(f"forced tool_choice did not finish with tool_calls: {forced_json}")
    ok("forced tool_choice", f"{forced.elapsed_ms}ms tool={tool}")

    if not args.skip_stream:
        status, stream_headers, events, elapsed = request_stream(
            api_url(base_url, "/v1/chat/completions"),
            payload={"model": model, "messages": [{"role": "user", "content": args.prompt}], "stream": True, "stream_options": {"include_usage": True}, "max_tokens": args.max_tokens},
            headers=headers,
            timeout=args.chat_timeout,
        )
        if status != 200:
            fail(f"stream returned HTTP {status}: {events[:5]}")
        first_content = next((ms for ms, line in events if '"content"' in line), None)
        if first_content is None or not any(line == "data: [DONE]" for _, line in events):
            had_reasoning = any('"reasoning_content"' in line for _, line in events)
            hint = (
                "; the model spent its whole token budget on reasoning_content "
                "(retry with a larger --max-tokens)"
                if had_reasoning
                else ""
            )
            fail(f"stream did not produce content and DONE{hint}: {events[-8:]}")
        ok("openai stream", f"{elapsed}ms first_content={first_content}ms events={len(events)} content_type={stream_headers.get('Content-Type')}")

    print("[DONE] full probe passed.")
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
        tool_names = [item.get("function", {}).get("name", "") for item in tools if isinstance(item, dict)] if isinstance(tools, list) else []
        last_user = next((item.get("content", "") for item in reversed(messages) if isinstance(item, dict) and item.get("role") == "user"), "")
        return (
            f"{method} {path} model={payload.get('model')} stream={payload.get('stream')} "
            f"messages={roles} tools={len(tools) if isinstance(tools, list) else 0} "
            f"tool_names={tool_names[:8]} tool_choice={payload.get('tool_choice')!r} last_user={str(last_user)[:80]!r}"
        )
    return f"{method} {path} json_keys={list(payload.keys()) if isinstance(payload, dict) else type(payload).__name__}"


def summarize_tool_calls(calls: Any) -> str:
    if not isinstance(calls, list) or not calls:
        return ""
    function = (calls[0] or {}).get("function") or {}
    raw_args = function.get("arguments", "")
    try:
        parsed_args = json.loads(raw_args) if isinstance(raw_args, str) else raw_args
    except Exception:
        parsed_args = raw_args
    return f"tool={function.get('name', '')} args={str(parsed_args)[:180]!r}"


def summarize_response(path: str, body: bytes) -> str:
    if not body:
        return "empty"
    text = body.decode("utf-8", errors="replace")
    if "data:" in text and path.rstrip("/").endswith("/chat/completions"):
        return f"sse done={'[DONE]' in text} tool_calls={'\"tool_calls\"' in text} bytes={len(body)}"
    try:
        payload = json.loads(text)
    except Exception:
        return f"body={len(body)} bytes"
    if path.rstrip("/").endswith("/chat/completions"):
        choices = payload.get("choices") or []
        choice = choices[0] if choices else {}
        message = choice.get("message") or {}
        calls = message.get("tool_calls")
        return f"finish={choice.get('finish_reason')} content={bool(message.get('content'))} tool_calls={len(calls) if isinstance(calls, list) else 0} {summarize_tool_calls(calls)}"
    return f"json_keys={list(payload.keys()) if isinstance(payload, dict) else type(payload).__name__}"


class ProxyHandler(BaseHTTPRequestHandler):
    upstream = DEFAULT_BASE_URL
    timeout = 300
    api_key = ""

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
        if self.api_key and "Authorization" not in headers:
            headers["Authorization"] = f"Bearer {self.api_key}"
        target = api_url(self.upstream, self.path)
        req = urllib.request.Request(target, data=body if body else None, headers=headers, method=self.command)
        started = time.monotonic()
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
        print(f"[RESP] {status} {int((time.monotonic() - started) * 1000)}ms {summarize_response(self.path, response_body)}", flush=True)


def run_proxy(args: argparse.Namespace) -> int:
    ProxyHandler.upstream = args.base_url.rstrip("/")
    ProxyHandler.timeout = args.timeout
    ProxyHandler.api_key = args.api_key
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


def add_common(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"default: {DEFAULT_BASE_URL}")
    parser.add_argument("--api-key", default="", help="optional Lociant API token")
    parser.add_argument("--timeout", type=int, default=60, help="HTTP timeout seconds")


def main() -> int:
    parser = argparse.ArgumentParser(description="Unified Lociant runtime/API/MCP test tool.")
    sub = parser.add_subparsers(dest="command")

    quick = sub.add_parser("quick", help="test health, models, tools, MCP, and optional chat")
    add_common(quick)
    quick.add_argument("--expect-auth", action="store_true", help="verify unauthenticated /api/v1/tools returns 401")
    quick.add_argument("--tool", default=DEFAULT_TOOL, help=f"default: {DEFAULT_TOOL}")
    quick.add_argument("--chat", action="store_true", help="also test /v1/chat/completions")
    quick.add_argument("--mcp-llm", action="store_true", help="also call llm_status and llm_chat through MCP")
    quick.add_argument("--mcp-llm-image", default="", help="optional local image path for MCP llm_chat")
    quick.add_argument("--model", default="", help="model id for chat; default: first /v1/models item")
    quick.add_argument("--prompt", default="Say OK in one short sentence.")
    quick.add_argument("--max-tokens", type=int, default=32)
    quick.add_argument("--chat-timeout", type=int, default=120)
    quick.set_defaults(func=run_quick)

    full = sub.add_parser("full", help="quick plus OpenAI tools and streaming")
    add_common(full)
    full.add_argument("--expect-auth", action="store_true")
    full.add_argument("--tool", default=DEFAULT_TOOL)
    full.add_argument("--chat", action="store_true", default=True)
    full.add_argument("--mcp-llm", action="store_true", default=True, help="call llm_status and llm_chat through MCP")
    full.add_argument("--mcp-llm-image", default="", help="optional local image path for MCP llm_chat")
    full.add_argument("--model", default="")
    full.add_argument("--prompt", default="Reply with exactly: background ok")
    full.add_argument("--max-tokens", type=int, default=32)
    full.add_argument("--chat-timeout", type=int, default=120)
    full.add_argument("--skip-stream", action="store_true")
    full.set_defaults(func=run_full)

    proxy = sub.add_parser("proxy", help="log and forward a real agent client's OpenAI requests")
    add_common(proxy)
    proxy.add_argument("--listen", default=DEFAULT_LISTEN, help=f"default: {DEFAULT_LISTEN}")
    proxy.add_argument("--port", type=int, default=DEFAULT_PROXY_PORT, help=f"default: {DEFAULT_PROXY_PORT}")
    proxy.set_defaults(func=run_proxy)

    args = parser.parse_args()
    if not hasattr(args, "func"):
        parser.print_help()
        return 2
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
