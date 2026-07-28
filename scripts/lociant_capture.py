#!/usr/bin/env python3
"""Capture the latest Lociant camera frame to a local JPEG file."""

from __future__ import annotations

import argparse
import base64
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = "http://127.0.0.1:11434"


def json_bytes(payload: dict[str, Any]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def api_url(base_url: str, path: str) -> str:
    base = base_url.rstrip("/")
    if base.endswith("/v1") and path.startswith(("/v1/", "/api/")):
        base = base[:-3]
    if base.endswith("/v1") and path == "/health":
        base = base[:-3]
    return f"{base}{path}"


def request_json(
    method: str,
    url: str,
    payload: dict[str, Any] | None = None,
    timeout: int = 30,
    api_key: str = "",
) -> dict[str, Any]:
    data = json_bytes(payload) if payload is not None else None
    headers = {"Accept": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
    if payload is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read()
    except urllib.error.HTTPError as error:
        body = error.read()
    try:
        return json.loads(body.decode("utf-8"))
    except json.JSONDecodeError as error:
        raise RuntimeError(f"invalid JSON response from {url}: {body[:300]!r}") from error


def call_tool(base_url: str, name: str, args: dict[str, Any] | None, timeout: int, api_key: str) -> dict[str, Any]:
    return request_json(
        "POST",
        api_url(base_url, f"/api/v1/tools/{name}/calls"),
        {"arguments": args or {}},
        timeout,
        api_key,
    )


def extract_result(response: dict[str, Any], tool: str) -> dict[str, Any]:
    if not response.get("ok", False):
        raise RuntimeError(json.dumps(response.get("error", response), ensure_ascii=False))
    result = response.get("result")
    if not isinstance(result, dict):
        raise RuntimeError(f"{tool} returned no result object: {response}")
    if not result.get("ok", True):
        raise RuntimeError(json.dumps(result.get("error", result), ensure_ascii=False))
    return result


def decode_data_url(value: str) -> tuple[str, bytes]:
    prefix, _, payload = value.partition(",")
    if not prefix.startswith("data:") or not payload:
        raise RuntimeError("camera_capture returned an invalid data URL")
    mime = prefix.removeprefix("data:").split(";", 1)[0] or "application/octet-stream"
    return mime, base64.b64decode(payload)


def run(args: argparse.Namespace) -> int:
    base_url = args.base_url.rstrip("/")
    if args.start:
        start = extract_result(call_tool(base_url, "vision_start", {}, args.timeout, args.api_key), "vision_start")
        print(f"[OK] vision_start state={start.get('state')} frameCount={start.get('frameCount')}")
        if args.wait_ms > 0:
            time.sleep(args.wait_ms / 1000.0)

    capture = extract_result(call_tool(base_url, "camera_capture", {}, args.timeout, args.api_key), "camera_capture")
    mime, image = decode_data_url(str(capture.get("image", "")))
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(image)
    print(
        "[OK] saved "
        f"{out} mime={mime} bytes={len(image)} "
        f"size={capture.get('width')}x{capture.get('height')} "
        f"frame={capture.get('frameCount')} fps={capture.get('fps')}"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Capture the latest Lociant camera frame.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"Lociant base URL, default: {DEFAULT_BASE_URL}")
    parser.add_argument("--out", default="capture.jpg", help="output JPEG path")
    parser.add_argument("--start", action="store_true", help="start vision runtime before capturing")
    parser.add_argument("--wait-ms", type=int, default=700, help="wait after --start before capture")
    parser.add_argument("--timeout", type=int, default=30, help="HTTP timeout in seconds")
    parser.add_argument("--api-key", default="", help="Lociant API token")
    args = parser.parse_args()
    try:
        return run(args)
    except Exception as error:
        print(f"[FAIL] {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
