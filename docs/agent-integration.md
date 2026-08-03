# Agent And MCP Integration

Lociant is a phone-side capability runtime. Planning, workspace access and long-lived agent orchestration remain in the external agent; the APK provides model inference and explicit Android capabilities.

## Preferred Connection Order

1. Use native MCP Streamable HTTP at `http://PHONE_IP:11434/mcp` for tools.
2. Use OpenAI Chat Completions at `http://PHONE_IP:11434/v1` for direct model inference.
3. Use `/api/v1` only for Lociant management resources.

Do not scrape the WebView or depend on `LociantBridge`. The bridge is an internal Android UI boundary, not a remote integration contract.

## MCP Configuration

`POST /mcp` is a Streamable HTTP MCP endpoint implemented with the official
MCP Kotlin SDK (`io.modelcontextprotocol:kotlin-sdk`, same SDK RikkaHub uses
client-side). The SDK handles JSON-RPC framing, protocol-version negotiation,
session management (`Mcp-Session-Id`) and JSON/SSE response selection, so no
protocol logic is hand-rolled in Lociant.

Client requirements (MCP Streamable HTTP spec):

- `Content-Type: application/json` on POST bodies;
- `Accept: application/json, text/event-stream` (both values);
- Bearer auth via `Authorization: Bearer TOKEN` when a token is configured.

### RikkaHub (recommended on the phone)

设置 → MCP → 导入，粘贴以下 JSON（Streamable HTTP 的 `type` 固定为
`streamable_http`；`headers` 是 `[name, value]` 对数组）：

```json
{
  "type": "streamable_http",
  "commonOptions": {
    "name": "Lociant 手机工具",
    "enable": true,
    "headers": [
      ["Authorization", "Bearer TOKEN"]
    ]
  },
  "url": "http://PHONE_IP:11434/mcp"
}
```

导入后在“助手设置 → MCP 服务器”中勾选该服务器，工具才会出现在该助手
的工具列表；对会改动手机状态的工具建议开启“需要审批”。

### OpenCode (desktop agent)

`opencode.json` 的远程 MCP 类型固定为 `remote`。OpenCode 默认 MCP 请求
超时只有 5 秒，而手机工具（如 `llm_chat`、`ui_screen_state`）经常超过，
必须显式调大 `timeout`（毫秒）；老版本 OpenCode 不会自动发送
`Accept` 头，也请显式带上：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "lociant": {
      "type": "remote",
      "url": "http://PHONE_IP:11434/mcp",
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

`PHONE_IP` 和 `TOKEN` 可在 Lociant 主页/设置里看到（`lanUrl` 与 API Token）。

Clients limited to stdio may run:

```bash
python scripts/lociant_mcp_server.py \
  --base-url http://PHONE_IP:11434 \
  --api-key TOKEN
```

The adapter is a transport translator; Android remains the source of truth for manifests, policy and execution.

## Tool Discovery

MCP `tools/list` is preferred. A non-MCP client may read:

```http
GET /api/v1/tools
Authorization: Bearer TOKEN
```

Tool definitions include Lociant policy metadata. The visible list depends on the configured exposure level:

- `read`: passive status and model information;
- `sensor`: read plus interactive sensor/screen context;
- `action`: read, sensor and phone-changing actions.

The registry repeats the policy check during `tools/call`. A stale or forged manifest therefore cannot bypass exposure or `remoteAllowed`.

## Core Tool Families

| Family | Examples | Notes |
|---|---|---|
| Runtime/model | `runtime_status`, `model_list`, `llm_status`, `llm_chat` | Local runtime and phone model inference |
| Device | `device_status`, `clipboard_read`, `clipboard_write`, `app_open` | Android privacy rules still apply |
| Sensors | `sensor_status`, `sensor_read`, `sensor_start`, `sensor_stop` | Aggregated summaries only; continuous monitoring is a side effect and should be stopped after use |
| Screen/UI | `ui_screen_state`, `ui_click_node`, `ui_tap`, `ui_swipe`, `ui_paste`, `ui_set_text`, navigation actions | Requires accessibility and usually an interactive device |
| Vision | `vision_status`, `vision_start`, `camera_capture`, `vision_stop` | Requires camera permission and unlocked interactive state |

Tool names are current 1.0 contracts. They are not aliases for a retired capability system.

## Cloud Model Provider

Lociant is local-first but can opt into an OpenAI-compatible cloud model as an additional model backend. Configure `cloudBaseUrl`, `cloudApiKey`, `cloudModel` and `cloudEnabled` in settings; the cloud model then appears in the model list and `llm_chat`/chat requests routed to it work through the same tool-call pipeline. The API key is stored on the phone only.

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

Optional image inputs include `image`, `images`, `useCameraFrame` and `useScreenFrame`. Camera/screen shortcuts fail explicitly when the required runtime or accessibility service is unavailable.

Supplying `sessionId` requires an existing Lociant session. Omitting it keeps the call stateless.

## Operational Expectations

- Android may suspend background work according to device-vendor policy; keep the foreground notification visible for long inference.
- Camera and accessibility operations depend on the screen, lock state, permissions and current Activity/service availability.
- A tool success response means the handler completed, not that an external UI remained unchanged afterward.
- Agents should re-read `ui_screen_state` after UI actions instead of assuming the previous node tree is still valid.
- Sensors return aggregated summaries (mean/variance/min/max per axis), not raw streams. `sensor_read` is one-shot; `sensor_start` enables a bounded rolling window readable via `sensor_status`, and `sensor_stop` ends it. Raw high-frequency samples are not meaningful to an LLM.
- To enter text into another app: `clipboard_write` the text, focus the target input with `ui_tap`/`ui_click_node`, then call `ui_paste` (uses `ACTION_PASTE` on the focused field, no context menu needed). `ui_set_text` writes text directly to an editable `nodeId` and bypasses the clipboard.
- Destructive and open-world MCP hints come from explicit tool policy fields, not from whether the client is remote.

## Authentication And Network

Only `/health` is public. Configure a non-empty token before binding Lociant to an untrusted LAN. MCP, OpenAI and control requests accept bearer auth or `X-Lociant-Token`.

Lociant currently uses cleartext HTTP for local-network interoperability. Treat the local network and token as security boundaries; do not expose the port directly to the public Internet.

## Verification

```bash
python scripts/lociant_test.py full \
  --base-url http://PHONE_IP:11434 \
  --api-key TOKEN \
  --expect-auth
```

The full probe verifies health, authenticated model/tool discovery, direct tool calls, MCP initialization/listing, OpenAI non-streaming Chat, forced tool calls and SSE termination.
