#!/usr/bin/env python3
"""MCP stdio adapter for Lociant's phone-side HTTP tools.

The Android app remains the source of truth. This process only translates MCP
JSON-RPC over stdio into Lociant's control-plane tools API.
"""

from __future__ import annotations

import argparse
import base64
import json
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


DEFAULT_BASE_URL = "http://127.0.0.1:11434"
DEFAULT_PROTOCOL_VERSION = "2025-06-18"


class McpError(Exception):
    def __init__(self, code: int, message: str, data: Any = None) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.data = data


@dataclass(frozen=True)
class Config:
    base_url: str
    timeout: int
    api_key: str
    allow: set[str]
    deny: set[str]


def log(message: str) -> None:
    print(f"[lociant-mcp] {message}", file=sys.stderr, flush=True)


def json_bytes(payload: dict[str, Any]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def api_url(base_url: str, path: str) -> str:
    base = base_url.rstrip("/")
    if base.endswith("/v1") and (path == "/health" or path.startswith(("/v1/", "/api/", "/mcp"))):
        base = base[:-3]
    return f"{base}{path}"


def request_json(method: str, url: str, payload: dict[str, Any] | None, config: Config) -> dict[str, Any]:
    data = json_bytes(payload) if payload is not None else None
    headers = {"Accept": "application/json"}
    if config.api_key:
        headers["Authorization"] = f"Bearer {config.api_key}"
    if payload is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=config.timeout) as resp:
            body = resp.read()
    except urllib.error.HTTPError as error:
        body = error.read()
    except OSError as error:
        raise McpError(-32000, f"Lociant HTTP request failed: {error}") from error
    try:
        decoded = json.loads(body.decode("utf-8"))
    except json.JSONDecodeError as error:
        raise McpError(-32000, f"Lociant returned invalid JSON from {url}", body[:300].decode("utf-8", "replace")) from error
    if not isinstance(decoded, dict):
        raise McpError(-32000, f"Lociant returned non-object JSON from {url}", decoded)
    return decoded


def tool_name(item: dict[str, Any]) -> str:
    function = item.get("function") if isinstance(item.get("function"), dict) else {}
    return str(function.get("name") or item.get("name") or "")


def tool_description(item: dict[str, Any]) -> str:
    function = item.get("function") if isinstance(item.get("function"), dict) else {}
    return str(function.get("description") or item.get("description") or "")


def tool_schema(item: dict[str, Any]) -> dict[str, Any]:
    function = item.get("function") if isinstance(item.get("function"), dict) else {}
    parameters = function.get("parameters") or item.get("inputSchema") or item.get("parameters")
    return parameters if isinstance(parameters, dict) else {"type": "object", "properties": {}}


def allowed_tool(name: str, config: Config) -> bool:
    if not name:
        return False
    if config.allow and name not in config.allow:
        return False
    return name not in config.deny


def list_lociant_tools(config: Config) -> list[dict[str, Any]]:
    manifest = request_json("GET", api_url(config.base_url, "/api/v1/tools"), None, config)
    items = manifest.get("data") or manifest.get("tools") or []
    if not isinstance(items, list):
        raise McpError(-32000, "Lociant /api/v1/tools returned no tool list", manifest)
    tools: list[dict[str, Any]] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        name = tool_name(item)
        if not allowed_tool(name, config):
            continue
        policy = item.get("x_policy") if isinstance(item.get("x_policy"), dict) else {}
        tool: dict[str, Any] = {
            "name": name,
            "description": tool_description(item),
            "inputSchema": tool_schema(item),
        }
        if policy:
            tool["annotations"] = {
                "readOnlyHint": not bool(policy.get("sideEffect", False)),
                "destructiveHint": False,
                "openWorldHint": bool(policy.get("remoteAllowed", False)),
            }
        tools.append(tool)
    return tools


def parse_data_url(value: str) -> tuple[str, str] | None:
    prefix, sep, payload = value.partition(",")
    if sep != "," or not prefix.startswith("data:") or ";base64" not in prefix:
        return None
    mime_type = prefix.removeprefix("data:").split(";", 1)[0] or "application/octet-stream"
    try:
        base64.b64decode(payload, validate=True)
    except Exception:
        return None
    return mime_type, payload


def compact_for_text(value: Any) -> Any:
    if isinstance(value, dict):
        out: dict[str, Any] = {}
        for key, item in value.items():
            if isinstance(item, str) and parse_data_url(item):
                mime_type, payload = parse_data_url(item) or ("application/octet-stream", "")
                out[key] = f"<{mime_type} data url, {len(payload)} base64 chars>"
            else:
                out[key] = compact_for_text(item)
        return out
    if isinstance(value, list):
        return [compact_for_text(item) for item in value]
    return value


def strip_large_media(value: Any) -> Any:
    if isinstance(value, dict):
        out: dict[str, Any] = {}
        for key, item in value.items():
            if isinstance(item, str) and parse_data_url(item):
                mime_type, payload = parse_data_url(item) or ("application/octet-stream", "")
                out[f"{key}MimeType"] = mime_type
                out[f"{key}Base64Bytes"] = len(payload)
            else:
                out[key] = strip_large_media(item)
        return out
    if isinstance(value, list):
        return [strip_large_media(item) for item in value]
    return value


def content_from_result(response: dict[str, Any]) -> tuple[list[dict[str, Any]], dict[str, Any], bool]:
    ok = bool(response.get("ok", False))
    result = response.get("result") if isinstance(response.get("result"), dict) else response
    is_error = not ok or (isinstance(result, dict) and result.get("ok") is False)
    content: list[dict[str, Any]] = []

    if isinstance(result, dict):
        for key, value in result.items():
            if isinstance(value, str):
                parsed = parse_data_url(value)
                if parsed:
                    mime_type, data = parsed
                    content.append({"type": "image", "data": data, "mimeType": mime_type})
                    continue

    text_payload = compact_for_text(result)
    content.append({
        "type": "text",
        "text": json.dumps(text_payload, ensure_ascii=False, indent=2),
    })
    structured = strip_large_media(result)
    return content, structured if isinstance(structured, dict) else {"value": structured}, is_error


def call_lociant_tool(config: Config, name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    if not allowed_tool(name, config):
        raise McpError(-32602, f"Tool is not exposed by this Lociant MCP adapter: {name}")
    response = request_json(
        "POST",
        api_url(config.base_url, f"/api/v1/tools/{name}/calls"),
        {"arguments": arguments},
        config,
    )
    content, structured, is_error = content_from_result(response)
    return {
        "content": content,
        "structuredContent": structured,
        "isError": is_error,
    }


def handle_request(message: dict[str, Any], config: Config) -> dict[str, Any] | None:
    method = message.get("method")
    message_id = message.get("id")
    params = message.get("params") if isinstance(message.get("params"), dict) else {}

    if message_id is None:
        return None

    try:
        if method == "initialize":
            client_version = params.get("protocolVersion")
            return result(message_id, {
                "protocolVersion": client_version or DEFAULT_PROTOCOL_VERSION,
                "capabilities": {"tools": {"listChanged": False}},
                "serverInfo": {"name": "lociant", "version": "1.0.0"},
                "instructions": "Use Lociant tools for Android-native sensing, screen context, local phone models, camera frames, and explicit phone UI actions.",
            })
        if method == "ping":
            return result(message_id, {})
        if method == "tools/list":
            return result(message_id, {"tools": list_lociant_tools(config)})
        if method == "tools/call":
            name = str(params.get("name") or "")
            arguments = params.get("arguments") if isinstance(params.get("arguments"), dict) else {}
            if not name:
                raise McpError(-32602, "tools/call requires params.name")
            return result(message_id, call_lociant_tool(config, name, arguments))
        if method in {"resources/list", "prompts/list"}:
            key = "resources" if method == "resources/list" else "prompts"
            return result(message_id, {key: []})
        raise McpError(-32601, f"Unsupported MCP method: {method}")
    except McpError as error:
        return failure(message_id, error.code, error.message, error.data)
    except Exception as error:
        return failure(message_id, -32000, str(error))


def result(message_id: Any, payload: dict[str, Any]) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": message_id, "result": payload}


def failure(message_id: Any, code: int, message: str, data: Any = None) -> dict[str, Any]:
    error: dict[str, Any] = {"code": code, "message": message}
    if data is not None:
        error["data"] = data
    return {"jsonrpc": "2.0", "id": message_id, "error": error}


def write_message(message: Any) -> None:
    sys.stdout.write(json.dumps(message, ensure_ascii=False, separators=(",", ":")) + "\n")
    sys.stdout.flush()


def read_loop(config: Config) -> int:
    log(f"stdio server -> {config.base_url}")
    for raw in sys.stdin:
        raw = raw.strip()
        if not raw:
            continue
        try:
            message = json.loads(raw)
        except json.JSONDecodeError as error:
            write_message(failure(None, -32700, "Parse error", str(error)))
            continue
        messages = message if isinstance(message, list) else [message]
        responses: list[dict[str, Any]] = []
        for item in messages:
            if not isinstance(item, dict):
                responses.append(failure(None, -32600, "Invalid JSON-RPC message"))
                continue
            response = handle_request(item, config)
            if response is not None:
                responses.append(response)
        if isinstance(message, list):
            if responses:
                write_message(responses)
        elif responses:
            write_message(responses[0])
    return 0


def split_csv(value: str) -> set[str]:
    return {item.strip() for item in value.split(",") if item.strip()}


def main() -> int:
    parser = argparse.ArgumentParser(description="Expose Lociant phone tools through MCP stdio.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"Lociant HTTP base URL, default: {DEFAULT_BASE_URL}")
    parser.add_argument("--timeout", type=int, default=30, help="HTTP timeout in seconds")
    parser.add_argument("--api-key", default="", help="optional Lociant API token")
    parser.add_argument("--allow", default="", help="comma-separated tool allowlist")
    parser.add_argument("--deny", default="", help="comma-separated tool denylist")
    args = parser.parse_args()
    config = Config(
        base_url=args.base_url.rstrip("/"),
        timeout=args.timeout,
        api_key=args.api_key.strip(),
        allow=split_csv(args.allow),
        deny=split_csv(args.deny),
    )
    return read_loop(config)


if __name__ == "__main__":
    raise SystemExit(main())
