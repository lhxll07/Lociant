#!/usr/bin/env python3
"""Smoke test MNNode's local model server.

This script intentionally uses only Python's standard library so it behaves the
same on Windows, macOS, and Linux without shell-specific JSON quoting issues.
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
DEFAULT_MODEL = "qwen3.5-2b-mnn"


@dataclass
class HttpResult:
    status: int
    headers: dict[str, str]
    body: bytes
    elapsed_ms: int


def json_bytes(payload: dict[str, Any]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def request(
    method: str,
    url: str,
    *,
    payload: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
    timeout: int = 90,
) -> HttpResult:
    data = json_bytes(payload) if payload is not None else None
    request_headers = {"Accept": "application/json", **(headers or {})}
    if payload is not None:
        request_headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=request_headers, method=method)
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
    elapsed_ms = int((time.monotonic() - started) * 1000)
    return HttpResult(status=status, headers=response_headers, body=body, elapsed_ms=elapsed_ms)


def request_stream(
    url: str,
    *,
    payload: dict[str, Any],
    headers: dict[str, str],
    timeout: int,
    max_events: int,
) -> tuple[int, dict[str, str], list[tuple[int, str]], int]:
    req = urllib.request.Request(
        url,
        data=json_bytes(payload),
        headers={"Content-Type": "application/json", "Accept": "text/event-stream", **headers},
        method="POST",
    )
    started = time.monotonic()
    events: list[tuple[int, str]] = []
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        for raw_line in resp:
            now_ms = int((time.monotonic() - started) * 1000)
            line = raw_line.decode("utf-8", errors="replace").strip()
            if not line:
                continue
            events.append((now_ms, line))
            if line == "data: [DONE]" or len(events) >= max_events:
                break
        elapsed_ms = int((time.monotonic() - started) * 1000)
        return resp.status, dict(resp.headers.items()), events, elapsed_ms


def parse_json(result: HttpResult) -> dict[str, Any]:
    try:
        return json.loads(result.body.decode("utf-8"))
    except json.JSONDecodeError as error:
        raise RuntimeError(f"invalid JSON response: {error}: {result.body[:300]!r}") from error


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def print_step(name: str, detail: str) -> None:
    print(f"[OK] {name}: {detail}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Smoke test MNNode model server endpoints.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"default: {DEFAULT_BASE_URL}")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"default: {DEFAULT_MODEL}")
    parser.add_argument("--session-id", default="", help="optional session id for persistence tests")
    parser.add_argument("--timeout", type=int, default=120, help="request timeout in seconds")
    parser.add_argument("--skip-chat", action="store_true", help="only test health and models")
    parser.add_argument("--skip-stream", action="store_true", help="skip OpenAI streaming test")
    parser.add_argument("--prompt", default="Reply with exactly: background ok")
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    session_headers = {"X-MNNode-Session-Id": args.session_id} if args.session_id else {}

    health = request("GET", f"{base_url}/health", timeout=args.timeout)
    if health.status != 200:
        fail(f"/health returned HTTP {health.status}: {health.body[:300]!r}")
    health_json = parse_json(health)
    if not health_json.get("ok"):
        fail(f"/health ok field is not true: {health_json}")
    print_step("health", f"{health.elapsed_ms}ms model={health_json.get('model')}")

    models = request("GET", f"{base_url}/v1/models", timeout=args.timeout)
    if models.status != 200:
        fail(f"/v1/models returned HTTP {models.status}: {models.body[:300]!r}")
    models_json = parse_json(models)
    model_ids = [item.get("id") for item in models_json.get("data", [])]
    if args.model not in model_ids:
        print(f"[WARN] requested model {args.model!r} not in /v1/models: {model_ids}")
    print_step("models", f"{models.elapsed_ms}ms ids={', '.join(str(item) for item in model_ids)}")

    if args.skip_chat:
        return 0

    openai_payload = {
        "model": args.model,
        "messages": [{"role": "user", "content": args.prompt}],
        "stream": False,
        "max_tokens": 32,
    }
    if args.session_id:
        openai_payload["sessionId"] = args.session_id
    chat = request(
        "POST",
        f"{base_url}/v1/chat/completions",
        payload=openai_payload,
        headers=session_headers,
        timeout=args.timeout,
    )
    chat_json = parse_json(chat)
    if chat.status != 200:
        fail(f"/v1/chat/completions returned HTTP {chat.status}: {chat_json}")
    content = chat_json.get("choices", [{}])[0].get("message", {}).get("content", "")
    if not content:
        fail(f"OpenAI chat response has empty content: {chat_json}")
    print_step("openai chat", f"{chat.elapsed_ms}ms session={chat_json.get('sessionId', '')} text={content[:80]!r}")

    ollama_payload = {
        "model": args.model,
        "messages": [{"role": "user", "content": args.prompt}],
        "stream": False,
        "options": {"num_predict": 32},
    }
    if args.session_id:
        ollama_payload["session_id"] = args.session_id
    ollama = request(
        "POST",
        f"{base_url}/api/chat",
        payload=ollama_payload,
        headers=session_headers,
        timeout=args.timeout,
    )
    ollama_json = parse_json(ollama)
    if ollama.status != 200:
        fail(f"/api/chat returned HTTP {ollama.status}: {ollama_json}")
    ollama_text = ollama_json.get("message", {}).get("content", "")
    if not ollama_text:
        fail(f"Ollama chat response has empty content: {ollama_json}")
    print_step("ollama chat", f"{ollama.elapsed_ms}ms session={ollama_json.get('session_id', '')} text={ollama_text[:80]!r}")

    if args.skip_stream:
        return 0

    stream_payload = {
        "model": args.model,
        "messages": [{"role": "user", "content": args.prompt}],
        "stream": True,
        "max_tokens": 32,
    }
    if args.session_id:
        stream_payload["sessionId"] = args.session_id
    status, headers, events, elapsed_ms = request_stream(
        f"{base_url}/v1/chat/completions",
        payload=stream_payload,
        headers=session_headers,
        timeout=args.timeout,
        max_events=80,
    )
    if status != 200:
        fail(f"stream returned HTTP {status}: events={events[:5]}")
    content_type = headers.get("Content-Type", "")
    first_content_ms = next((ms for ms, line in events if '"content"' in line), None)
    done_seen = any(line == "data: [DONE]" for _, line in events)
    if first_content_ms is None:
        fail(f"stream produced no content chunk: events={events[:8]}")
    if not done_seen:
        fail(f"stream did not finish with [DONE]: last={events[-3:]}")
    print_step(
        "openai stream",
        f"{elapsed_ms}ms first_content={first_content_ms}ms events={len(events)} content_type={content_type}",
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
