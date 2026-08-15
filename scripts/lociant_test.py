#!/usr/bin/env python3
"""Probe the Lociant edge runtime control and MCP interfaces.

The script has no third-party dependencies. It deliberately tests the edge
contract only: health, authentication, model inventory, tools and MCP.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


DEFAULT_BASE_URL = "http://127.0.0.1:11434"
DEFAULT_TOOL = "device_status"
MCP_PROTOCOL_VERSION = "2025-03-26"


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
    for suffix in ("/api/v1", "/v1"):
        if base.endswith(suffix):
            base = base[: -len(suffix)]
            break
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
    request_headers = {"Accept": "application/json", **(headers or {})}
    if payload is not None:
        request_headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=request_headers, method=method)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            return HttpResult(
                response.status,
                dict(response.headers.items()),
                response.read(),
                int((time.monotonic() - started) * 1000),
            )
    except urllib.error.HTTPError as error:
        return HttpResult(
            error.code,
            dict(error.headers.items()),
            error.read(),
            int((time.monotonic() - started) * 1000),
        )
    except OSError as error:
        fail(f"{method} {url} failed: {error}")
        raise AssertionError("unreachable")


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def ok(name: str, detail: str = "") -> None:
    suffix = f": {detail}" if detail else ""
    print(f"[OK] {name}{suffix}")


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


def tool_name(item: dict[str, Any]) -> str:
    direct = item.get("name")
    if isinstance(direct, str) and direct:
        return direct
    function = item.get("function")
    if isinstance(function, dict):
        return str(function.get("name") or "")
    return ""


def list_models(base_url: str, headers: dict[str, str], timeout: int) -> list[str]:
    result = request("GET", api_url(base_url, "/api/v1/models"), headers=headers, timeout=timeout)
    data = expect_json("models", result)
    models = data.get("models")
    if not isinstance(models, list):
        fail(f"/api/v1/models missing models list: {data}")
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
    ok("tools", f"{result.elapsed_ms}ms count={len(names)}")
    return tools, names


def call_tool(base_url: str, headers: dict[str, str], timeout: int, tool: str) -> dict[str, Any]:
    result = request(
        "POST",
        api_url(base_url, f"/api/v1/tools/{tool}/calls"),
        payload={"arguments": {}},
        headers=headers,
        timeout=timeout,
    )
    data = expect_json(f"tool {tool}", result)
    if data.get("ok") is not True:
        fail(f"tool {tool} returned not ok: {data}")
    ok("tool call", f"{result.elapsed_ms}ms tool={tool}")
    return data


def mcp_call(
    base_url: str,
    headers: dict[str, str],
    timeout: int,
    method: str,
    params: dict[str, Any] | None = None,
) -> dict[str, Any]:
    result = request(
        "POST",
        api_url(base_url, "/mcp"),
        payload={
            "jsonrpc": "2.0",
            "id": method,
            "method": method,
            "params": params or {},
        },
        headers={
            "MCP-Protocol-Version": MCP_PROTOCOL_VERSION,
            "Accept": "application/json, text/event-stream",
            **headers,
        },
        timeout=timeout,
    )
    data = expect_json(f"mcp {method}", result)
    if "error" in data:
        fail(f"MCP {method} returned error: {data}")
    return data


def run_quick(args: argparse.Namespace) -> int:
    base_url = args.base_url.rstrip("/")
    headers = bearer(args.api_key)

    health_result = request("GET", api_url(base_url, "/health"), timeout=args.timeout)
    health = expect_json("health", health_result)
    ok("health", f"{health_result.elapsed_ms}ms status={health.get('status')}")
    if "authToken" in health:
        fail("/health leaked authToken")

    if args.expect_auth:
        unauthenticated = request("GET", api_url(base_url, "/api/v1/tools"), timeout=args.timeout)
        if unauthenticated.status != 401:
            fail(f"expected unauthenticated /api/v1/tools to return 401, got {unauthenticated.status}")
        ok("auth", "unauthenticated control request returned 401")

    list_models(base_url, headers, args.timeout)
    _, names = list_tools(base_url, headers, args.timeout)
    tool = args.tool if args.tool in names else next(iter(names), "")
    if not tool:
        fail("no visible tool is available")
    if tool != args.tool:
        print(f"[WARN] requested tool {args.tool!r} is unavailable; using {tool!r}")
    call_tool(base_url, headers, args.timeout, tool)

    initialized = mcp_call(
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
    server_info = (initialized.get("result") or {}).get("serverInfo")
    ok("mcp initialize", f"server={(server_info or {}).get('name', 'unknown')}")

    listed = mcp_call(base_url, headers, args.timeout, "tools/list")
    mcp_tools = (listed.get("result") or {}).get("tools")
    if not isinstance(mcp_tools, list):
        fail(f"MCP tools/list missing tools array: {listed}")
    mcp_names = [item.get("name") for item in mcp_tools if isinstance(item, dict)]
    if tool not in mcp_names:
        fail(f"MCP tools/list missing {tool!r}")
    ok("mcp tools/list", f"count={len(mcp_names)} selected={tool}")

    print("[DONE] edge runtime probe passed.")
    return 0


def add_common(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--api-key", default="", help="optional Lociant API token")
    parser.add_argument("--timeout", type=int, default=60, help="HTTP timeout seconds")
    parser.add_argument("--expect-auth", action="store_true", help="verify an unauthenticated control request")
    parser.add_argument("--tool", default=DEFAULT_TOOL, help=f"preferred tool, default: {DEFAULT_TOOL}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Probe the Lociant edge runtime API and MCP endpoint.")
    sub = parser.add_subparsers(dest="command")
    quick = sub.add_parser("quick", help="test health, models, tools and MCP")
    add_common(quick)
    quick.set_defaults(func=run_quick)
    full = sub.add_parser("full", help="alias for the edge runtime probe")
    add_common(full)
    full.set_defaults(func=run_quick)

    args = parser.parse_args()
    if not hasattr(args, "func"):
        parser.print_help()
        return 2
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
