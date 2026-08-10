# Agent Integration (MCP + OpenAI API)

Lociant is a local agent runtime. Planning and long-lived orchestration stay
in the external agent; Lociant provides model inference and explicit device
capabilities through one HTTP surface served by the Rust backend on port
`11434` (the Android phone or the Linux desktop).

## Preferred Connection Order

1. Use MCP Streamable HTTP at `http://HOST:11434/mcp` for tools.
2. Use OpenAI Chat Completions at `http://HOST:11434/v1` for direct model
   inference.
3. Use `/api/v1` only for management resources (see `control-api.md`).

Do not scrape the app UI or depend on the Android method channel; the channel
is an internal UI boundary, not a remote integration contract.

## MCP Configuration

`POST /mcp` is an MCP Streamable HTTP endpoint implemented by the Rust backend
as a stateless JSON-RPC server. It supports `initialize`, `ping`,
`tools/list`, `tools/call` and notifications (202 no body); responses are
JSON. Client requirements:

- `Content-Type: application/json` on POST bodies;
- `Accept: application/json` (both JSON and `text/event-stream` accepted);
- Bearer auth via `Authorization: Bearer TOKEN` (or `X-Lociant-Token`) when a
  token is configured.

### RikkaHub (phone client)

设置 → MCP → 导入，粘贴以下 JSON（Streamable HTTP 的 `type` 固定为
`streamable_http`；`headers` 是 `[name, value]` 对数组）：

```json
{
  "type": "streamable_http",
  "commonOptions": {
    "name": "Lociant 设备工具",
    "enable": true,
    "headers": [
      ["Authorization", "Bearer TOKEN"]
    ]
  },
  "url": "http://HOST:11434/mcp"
}
```

导入后在“助手设置 → MCP 服务器”中勾选该服务器；对会改动设备状态的工具
建议开启“需要审批”。

### OpenCode (desktop agent)

`opencode.json` 的远程 MCP 类型固定为 `remote`。OpenCode 默认 MCP 请求
超时只有 5 秒，而本地工具（如 `llm_chat`、`ui_screen_state`）经常超过，
必须显式调大 `timeout`（毫秒）；老版本 OpenCode 不会自动发送
`Accept` 头，也请显式带上：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "lociant": {
      "type": "remote",
      "url": "http://HOST:11434/mcp",
      "enabled": true,
      "timeout": 120000,
      "headers": {
        "Authorization": "Bearer TOKEN",
        "Accept": "application/json, text/event-stream"
      }
    }
  }
}
```

`HOST` 和 `TOKEN` 可在 Lociant 主页/设置里看到（`lanUrl` 与 API Token）。
Clients limited to stdio may run a transport translator against the same URL.

## Tool Discovery

```http
GET /api/v1/tools
Authorization: Bearer TOKEN
```

Tool definitions include Lociant policy metadata. The visible list depends on
the configured exposure level:

- `read`: passive status and model information;
- `sensor`: read plus interactive sensor/screen context;
- `action`: read, sensor and device-changing actions.

The Rust `ToolRegistry` repeats the policy check during execution
(`remote_allowed` for HTTP/MCP callers). A stale or forged manifest cannot
bypass exposure or `remoteAllowed`.

## Core Tool Families

| Family | Examples | Notes |
|---|---|---|
| Runtime/model | `runtime_status`, `model_list`, `llm_status`, `llm_chat` | Local runtime and model inference |
| Device | `device_status`, `clipboard_read`, `clipboard_write`, `app_open` | Android privacy rules still apply |
| Sensors | `sensor_status`, `sensor_read`, `sensor_start`, `sensor_stop` | Aggregated summaries only |
| Screen/UI | `ui_screen_state`, `ui_click_node`, `ui_tap`, `ui_swipe`, `ui_paste`, `ui_set_text` | Requires accessibility on Android |
| Vision | `vision_status`, `vision_start`, `camera_capture`, `vision_stop` | Requires camera permission and unlocked interactive state |

Tool names are current 1.0 contracts.

## Cloud Model Provider

Lociant is local-first but can opt into an OpenAI-compatible cloud model.
Configure `cloudBaseUrl`, `cloudApiKey`, `cloudModel` and `cloudEnabled` in
settings; the cloud model then appears in the model list and chat requests
for it are routed to that endpoint. The API key stays in local settings.

## LLM Tool

`llm_chat` accepts either a prompt or message list:

```json
{
  "name": "llm_chat",
  "arguments": {
    "prompt": "Describe the current task briefly.",
    "model": "qwen3.5-2b-mnn",
    "maxTokens": 128
  }
}
```

Optional image inputs include `image`, `images`, `useCameraFrame` and
`useScreenFrame`. Supplying `sessionId` requires an existing Lociant session;
omitting it keeps the call stateless.

## OpenAI-Compatible API

A deliberately small OpenAI data plane. Model management, sessions, storage
and runtime commands stay under `/api/v1`, never under `/v1`.

## Connection

```text
Base URL: http://HOST:11434/v1
API key:  token configured in the Lociant app
Model:    one id returned by GET /v1/models
```

When a token is configured, endpoints require `Authorization: Bearer TOKEN`
or `X-Lociant-Token`. `GET /health` is public.

## Models

```http
GET /v1/models
```

Standard list shape, containing only ready chat-capable models (installed
local models plus the configured cloud model). Complete model status and
management live at `/api/v1/models`.

## Chat Completions

```http
POST /v1/chat/completions
Content-Type: application/json
```

```json
{
  "model": "qwen3.5-2b-mnn",
  "messages": [
    {"role": "system", "content": "Answer briefly."},
    {"role": "user", "content": "Hello"}
  ],
  "max_tokens": 128,
  "stream": false
}
```

Message content accepts plain strings and OpenAI content arrays containing
`text` and base64/data-URL `image_url` items (VLM required for images). The
response includes standard `choices` and `usage`.

## Streaming

Set `"stream": true` for `text/event-stream` chunks ending with
`data: [DONE]`. Request a final usage chunk with
`"stream_options": {"include_usage": true}`.

## Tool Calls

OpenAI `tools` and `tool_choice` are accepted. The Lociant extension
`execute_tools: true` runs the multi-round agent loop: allowed tools execute
through the shared registry and the model continues. Clients that need
portable OpenAI behavior should omit it and execute returned tool calls
themselves.

## Sessions

OpenAI has no standard session resource. Lociant offers an explicit opt-in:
pass an existing session id as `sessionId` in the request body (or the
`X-Lociant-Session-Id` header). Unknown IDs return `404`. Omitting a session
runs a stateless request.

## Async Requests

`"async": true` returns `202` and a request id; inspect it via
`GET /api/v1/chat-requests/{requestId}`.

## Errors

OpenAI endpoint errors use an OpenAI error object; HTTP status remains
authoritative. Control errors use `application/problem+json`
(see `control-api.md`).

## Removed Interfaces

Lociant does not implement Ollama `/api/chat`, old session headers, or
product-control routes under `/v1`.

## Authentication And Network

Only `/health` is public. Configure a non-empty token before binding Lociant
to an untrusted LAN. MCP, OpenAI and control requests accept bearer auth or
`X-Lociant-Token`.

Lociant uses cleartext HTTP for local-network interoperability. Treat the
local network and token as security boundaries; do not expose the port
directly to the public Internet.

## Verification

```bash
python scripts/lociant_test.py full \
  --base-url http://HOST:11434 \
  --api-key TOKEN \
  --expect-auth
```

The full probe verifies health, authenticated model/tool discovery, direct
tool calls, MCP initialization/listing, OpenAI non-streaming chat, forced
tool calls and SSE termination.
