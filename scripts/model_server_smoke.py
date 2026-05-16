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
DEFAULT_TOOL = "get_runtime_status"
DEFAULT_SCENE = "study-desk"


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


def api_url(base_url: str, path: str) -> str:
    base = base_url.rstrip("/")
    if base.endswith("/v1") and path.startswith("/v1/"):
        base = base[:-3]
    if base.endswith("/v1") and (path == "/health" or path.startswith("/api/")):
        base = base[:-3]
    return f"{base}{path}"


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def print_step(name: str, detail: str) -> None:
    print(f"[OK] {name}: {detail}")


def require_chat_completion(value: dict[str, Any], *, allow_tool_calls: bool = False) -> dict[str, Any]:
    if value.get("object") != "chat.completion":
        fail(f"response object is not chat.completion: {value}")
    if not value.get("id") or not isinstance(value.get("created"), int) or not value.get("model"):
        fail(f"chat completion missing id/created/model: {value}")
    choices = value.get("choices")
    if not isinstance(choices, list) or not choices:
        fail(f"chat completion missing choices: {value}")
    choice = choices[0]
    message = choice.get("message", {})
    if message.get("role") != "assistant":
        fail(f"chat completion message role is not assistant: {value}")
    if allow_tool_calls:
        calls = message.get("tool_calls")
        if not isinstance(calls, list) or not calls:
            fail(f"chat completion missing tool_calls: {value}")
    elif not message.get("content"):
        fail(f"chat completion has empty content: {value}")
    return choice


def require_usage(value: dict[str, Any]) -> None:
    usage = value.get("usage")
    if not isinstance(usage, dict):
        fail(f"response missing usage: {value}")
    for key in ("prompt_tokens", "completion_tokens", "total_tokens"):
        if not isinstance(usage.get(key), int):
            fail(f"usage.{key} is not an integer: {usage}")
    details = usage.get("prompt_tokens_details", {})
    if not isinstance(details, dict) or not isinstance(details.get("cached_tokens"), int):
        fail(f"usage.prompt_tokens_details.cached_tokens missing: {usage}")
    metrics = value.get("mnnode")
    if not isinstance(metrics, dict) or "elapsed_ms" not in metrics or "tokens_per_second" not in metrics:
        fail(f"response missing mnnode runtime metrics: {value}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Smoke test MNNode model server endpoints.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"default: {DEFAULT_BASE_URL}")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"default: {DEFAULT_MODEL}")
    parser.add_argument("--session-id", default="", help="optional session id for persistence tests")
    parser.add_argument("--timeout", type=int, default=120, help="request timeout in seconds")
    parser.add_argument("--skip-chat", action="store_true", help="only test health and models")
    parser.add_argument("--skip-scenes", action="store_true", help="skip scene/trigger tests")
    parser.add_argument("--skip-stream", action="store_true", help="skip OpenAI streaming test")
    parser.add_argument("--skip-tools", action="store_true", help="skip OpenAI tool calling compatibility checks")
    parser.add_argument("--scene", default=DEFAULT_SCENE, help=f"scene id for trigger load test (default: {DEFAULT_SCENE})")
    parser.add_argument("--tool", default=DEFAULT_TOOL, help=f"default: {DEFAULT_TOOL}")
    parser.add_argument("--prompt", default="Reply with exactly: background ok")
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    session_headers = {"X-MNNode-Session-Id": args.session_id} if args.session_id else {}

    health = request("GET", api_url(base_url, "/health"), timeout=args.timeout)
    if health.status != 200:
        fail(f"/health returned HTTP {health.status}: {health.body[:300]!r}")
    health_json = parse_json(health)
    if not health_json.get("ok"):
        fail(f"/health ok field is not true: {health_json}")
    print_step("health", f"{health.elapsed_ms}ms model={health_json.get('model')}")

    models = request("GET", api_url(base_url, "/v1/models"), timeout=args.timeout)
    if models.status != 200:
        fail(f"/v1/models returned HTTP {models.status}: {models.body[:300]!r}")
    models_json = parse_json(models)
    model_ids = [item.get("id") for item in models_json.get("data", [])]
    if args.model not in model_ids:
        print(f"[WARN] requested model {args.model!r} not in /v1/models: {model_ids}")
    print_step("models", f"{models.elapsed_ms}ms ids={', '.join(str(item) for item in model_ids)}")

    if not args.skip_scenes:
        scenes = request("GET", api_url(base_url, "/v1/scenes"), timeout=args.timeout)
        scenes_json = parse_json(scenes) if scenes.status == 200 else None
        if scenes.status != 200 or not isinstance(scenes_json, list):
            fail(f"/v1/scenes returned HTTP {scenes.status}: {scenes.body[:300]!r}")
        scene_ids = [s.get("id") for s in scenes_json]
        print_step("scenes", f"{scenes.elapsed_ms}ms scenes={scene_ids}")

        target_scene = next((s for s in scenes_json if s.get("id") == args.scene), None)
        if target_scene is None:
            fail(f"scene {args.scene!r} not found in /v1/scenes: {scene_ids}")
        triggers = target_scene.get("triggers", [])
        if not isinstance(triggers, list) or len(triggers) == 0:
            fail(f"scene {args.scene!r} has no triggers: {target_scene}")
        print_step("scene triggers", f"{args.scene}: {len(triggers)} triggers={[t.get('id') for t in triggers]}")

        load = request("POST", api_url(base_url, f"/v1/scenes/{args.scene}/load"), timeout=args.timeout)
        load_json = parse_json(load)
        if load.status != 200 or not load_json.get("ok"):
            fail(f"/v1/scenes/{args.scene}/load failed HTTP {load.status}: {load_json}")
        loaded = load_json.get("triggersLoaded", 0)
        if loaded != len(triggers):
            fail(f"triggersLoaded={loaded} expected={len(triggers)}: {load_json}")
        print_step("scene load", f"{load.elapsed_ms}ms {args.scene} triggersLoaded={loaded}")

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
        api_url(base_url, "/v1/chat/completions"),
        payload=openai_payload,
        headers=session_headers,
        timeout=args.timeout,
    )
    chat_json = parse_json(chat)
    if chat.status != 200:
        fail(f"/v1/chat/completions returned HTTP {chat.status}: {chat_json}")
    choice = require_chat_completion(chat_json)
    require_usage(chat_json)
    content = choice.get("message", {}).get("content", "")
    usage = chat_json["usage"]
    metrics = chat_json["mnnode"]
    print_step(
        "openai chat",
        f"{chat.elapsed_ms}ms prompt={usage['prompt_tokens']} cached={usage['prompt_tokens_details']['cached_tokens']} "
        f"out={usage['completion_tokens']} speed={metrics['tokens_per_second']:.2f}t/s text={content[:80]!r}",
    )

    if not args.skip_tools:
        tools = request("GET", api_url(base_url, "/v1/tools"), timeout=args.timeout)
        if tools.status != 200:
            fail(f"/v1/tools returned HTTP {tools.status}: {tools.body[:300]!r}")
        tools_json = parse_json(tools)
        tool_defs = tools_json.get("data", [])
        tool_names = [
            item.get("function", {}).get("name")
            for item in tool_defs
            if item.get("type") == "function"
        ]
        for item in tool_defs:
            name = item.get("function", {}).get("name")
            policy = item.get("x_policy")
            if item.get("x_execution") != "local" or not isinstance(policy, dict):
                fail(f"tool {name!r} missing MNNode policy metadata: {item}")
            for key in ("local", "remoteAllowed", "requiresActivity", "sideEffect"):
                if not isinstance(policy.get(key), bool):
                    fail(f"tool {name!r} has invalid x_policy.{key}: {item}")
        if args.tool not in tool_names:
            fail(f"tool {args.tool!r} not listed by /v1/tools: {tool_names}")
        print_step("tools manifest", f"{tools.elapsed_ms}ms tools={', '.join(tool_names)}")

        for expected_tool in ("notify_user", "record_event", "call_webhook"):
            if expected_tool not in tool_names:
                fail(f"new tool {expected_tool!r} not in /v1/tools: {tool_names}")
        print_step("new tools", f"notify_user/record_event/call_webhook present")

        direct_tool = request(
            "POST",
            api_url(base_url, f"/v1/tools/{args.tool}/call"),
            payload={"arguments": {}},
            timeout=args.timeout,
        )
        direct_tool_json = parse_json(direct_tool)
        if direct_tool.status != 200 or not direct_tool_json.get("ok"):
            fail(f"direct tool call failed HTTP {direct_tool.status}: {direct_tool_json}")
        print_step("direct tool", f"{direct_tool.elapsed_ms}ms tool={direct_tool_json.get('tool')}")

        tool_payload = {
            "model": args.model,
            "messages": [{"role": "user", "content": "Use the requested tool."}],
            "tools": [item for item in tool_defs if item.get("function", {}).get("name") == args.tool],
            "tool_choice": {"type": "function", "function": {"name": args.tool}},
            "stream": False,
        }
        tool_call = request(
            "POST",
            api_url(base_url, "/v1/chat/completions"),
            payload=tool_payload,
            headers=session_headers,
            timeout=args.timeout,
        )
        tool_call_json = parse_json(tool_call)
        if tool_call.status != 200:
            fail(f"forced tool_choice returned HTTP {tool_call.status}: {tool_call_json}")
        tool_choice = require_chat_completion(tool_call_json, allow_tool_calls=True)
        if tool_choice.get("finish_reason") != "tool_calls":
            fail(f"forced tool_choice finish_reason is not tool_calls: {tool_call_json}")
        call = tool_choice["message"]["tool_calls"][0]
        if call.get("type") != "function" or call.get("function", {}).get("name") != args.tool:
            fail(f"forced tool_choice returned wrong tool call: {tool_call_json}")
        print_step("forced tool_choice", f"{tool_call.elapsed_ms}ms id={call.get('id')} name={args.tool}")

        tool_followup_payload = {
            "model": args.model,
            "messages": [
                {"role": "user", "content": "Summarize the tool result in one short sentence."},
                tool_choice["message"],
                {
                    "role": "tool",
                    "tool_call_id": call.get("id"),
                    "content": json.dumps(direct_tool_json, ensure_ascii=False),
                },
            ],
            "tool_choice": "none",
            "stream": False,
            "max_tokens": 48,
        }
        tool_followup = request(
            "POST",
            api_url(base_url, "/v1/chat/completions"),
            payload=tool_followup_payload,
            headers=session_headers,
            timeout=args.timeout,
        )
        tool_followup_json = parse_json(tool_followup)
        if tool_followup.status != 200:
            fail(f"role=tool follow-up returned HTTP {tool_followup.status}: {tool_followup_json}")
        followup_choice = require_chat_completion(tool_followup_json)
        followup_content = followup_choice.get("message", {}).get("content", "")
        print_step("tool follow-up", f"{tool_followup.elapsed_ms}ms text={followup_content[:80]!r}")

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
        api_url(base_url, "/api/chat"),
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
    if not isinstance(ollama_json.get("eval_count"), int) or not isinstance(ollama_json.get("prompt_eval_count"), int):
        fail(f"Ollama response missing token counts: {ollama_json}")
    print_step("ollama chat", f"{ollama.elapsed_ms}ms session={ollama_json.get('session_id', '')} text={ollama_text[:80]!r}")

    if args.skip_stream:
        return 0

    stream_payload = {
        "model": args.model,
        "messages": [{"role": "user", "content": args.prompt}],
        "stream": True,
        "stream_options": {"include_usage": True},
        "max_tokens": 32,
    }
    if args.session_id:
        stream_payload["sessionId"] = args.session_id
    status, headers, events, elapsed_ms = request_stream(
        api_url(base_url, "/v1/chat/completions"),
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
    usage_seen = any('"usage"' in line and '"choices":[]' in line.replace(" ", "") for _, line in events)
    if not usage_seen:
        fail(f"stream did not include final usage chunk: last={events[-5:]}")
    print_step(
        "openai stream",
        f"{elapsed_ms}ms first_content={first_content_ms}ms events={len(events)} content_type={content_type}",
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
