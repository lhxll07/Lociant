# Agent And MCP Integration

Lociant is a phone-side capability runtime. Planning, workspace access and long-lived agent orchestration remain in the external agent; the APK provides model inference and explicit Android capabilities.

## Preferred Connection Order

1. Use native MCP Streamable HTTP at `http://PHONE_IP:11434/mcp` for tools.
2. Use OpenAI Chat Completions at `http://PHONE_IP:11434/v1` for direct model inference.
3. Use `/api/v1` only for Lociant management resources.

Do not scrape the WebView or depend on `LociantBridge`. The bridge is an internal Android UI boundary, not a remote integration contract.

## MCP Configuration

Example client configuration:

```json
{
  "mcpServers": {
    "lociant": {
      "type": "streamable-http",
      "url": "http://PHONE_IP:11434/mcp",
      "headers": {
        "Authorization": "Bearer TOKEN"
      }
    }
  }
}
```

The endpoint accepts JSON-RPC `initialize`, `ping`, `tools/list`, `tools/call`, `resources/list` and `prompts/list`. Resources and prompts currently return empty lists.

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
| Screen/UI | `ui_screen_state`, `ui_click_node`, `ui_tap`, `ui_swipe`, navigation actions | Requires accessibility and usually an interactive device |
| Vision | `vision_status`, `vision_start`, `camera_capture`, `vision_stop` | Requires camera permission and unlocked interactive state |

Tool names are current 1.0 contracts. They are not aliases for a retired capability system.

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
