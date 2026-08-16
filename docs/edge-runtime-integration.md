# Lociant Edge Runtime Integration

Lociant is an edge-device runtime. It exposes local models, device tools,
sensors and peer-node capabilities through a small authenticated HTTP surface.
It does not plan tasks or run a general-purpose Agent loop. An external
client may use MCP to orchestrate the capabilities, while the runtime keeps
execution and policy enforcement on the device.

The Rust backend listens on port `11434` on Linux, headless boards and Android
when the foreground runtime is enabled.

## Interfaces

| Interface | Purpose |
|---|---|
| `POST /mcp` | MCP Streamable HTTP for tool discovery and calls |
| `/api/v1` | Runtime status, settings, models, peers and direct tool calls |
| `GET /health` | Unauthenticated liveness check |
| Android device IPC | Internal localhost bridge between Rust and Kotlin |

There is no OpenAI chat-completions data plane and no `/api/v1/sessions`
resource. The old `/v1/models` and `/v1/chat/completions` paths are removed.
Use `GET /api/v1/models` for model inventory and MCP or the control tool route
for execution.

Do not scrape the UI or depend on the Android method channel. The method
channel is an internal boundary for permissions, lifecycle, windows and
vision; remote integrations use HTTP or MCP.

## MCP

`POST /mcp` implements the stateless JSON-RPC subset needed by MCP clients:
`initialize`, `ping`, `tools/list`, `tools/call` and notifications. Responses
are JSON. Notifications receive HTTP `202` with no body.

Requests should send:

```http
Content-Type: application/json
Accept: application/json, text/event-stream
Authorization: Bearer TOKEN
```

The authorization header is optional only when the runtime has no
`authToken`. `X-Lociant-Token` is also accepted.

Example MCP initialization:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {},
    "clientInfo": {"name": "my-client", "version": "1.0"}
  }
}
```

Example tool call:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "runtime_status",
    "arguments": {}
  }
}
```

For OpenCode, use a remote MCP entry and set a timeout suitable for native
device operations:

```json
{
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

## Tool Policy

```http
GET /api/v1/tools
Authorization: Bearer TOKEN
```

The response uses `{ "data": [...] }`. Each descriptor includes the tool
schema and policy metadata. The configured exposure level is cumulative:

- `read`: passive status and read-only tools;
- `sensor`: read tools plus sensor/context tools;
- `action`: all exposed device operations.

`ToolRegistry` enforces the exposure level and `remoteAllowed` again when a
call arrives. A client cannot bypass policy by altering a cached descriptor.
Peer tools are namespaced as `peer:<nodeId>:<toolName>`.

Direct calls use the same registry:

```http
POST /api/v1/tools/runtime_status/calls
Content-Type: application/json
Authorization: Bearer TOKEN
```

```json
{"arguments": {}}
```

The result is a `ToolResult` envelope with `ok`, `content`, `structured` and
an optional `error`.

## Local Models

Model inventory is exposed through the control API. Android imports a single
`.gguf` file, or a package containing one `.gguf`, into its shared model
directory. When an ARM64 `llama-server` bundle is installed, the Rust backend
starts it locally and reports the model with `runtime: llama`.

```http
GET /api/v1/models
```

The llama.cpp process is private to the runtime. External clients use the
control API and MCP for device tools; they do not call the child server
directly. Android's NCNN runtime remains dedicated to camera and vision
operations, while headless boards can load RKLLM models in process.

## Control API

All routes below use the same optional bearer authentication.

### Runtime and settings

```http
GET /api/v1/runtime
GET /api/v1/settings
PUT /api/v1/settings
```

The runtime snapshot reports the process, selected model, device information,
endpoint URLs and tool exposure. Android-specific
permission and window state is merged into the UI snapshot through the local
platform service.

Settings are merged at the top level and persisted by the Rust Store. Common
fields are `modelId`, `maxOutputTokens`, `authToken`, `toolExposure`,
`autoStart`, `peerToken`, `peerDiscovery`, `peerName`, `rkllmModelPath` and the
optional llama.cpp settings. Cloud-provider and general-agent settings are
ignored as legacy input.

### Models

```http
GET    /api/v1/models
DELETE /api/v1/models/{modelId}
GET    /api/v1/catalog/models?q=QUERY
POST   /api/v1/model-installations
GET    /api/v1/model-installations/{jobId}
```

Model installation accepts `{"modelId":"MODEL_ID"}` and returns a job
resource. Progress contains `state`, `active`, `progress` and `message`; the
terminal states are `done` and `error`.

### Nodes and peers

```http
GET    /api/v1/nodes
POST   /api/v1/peers
DELETE /api/v1/peers/{nodeId}
GET    /api/v1/peer/tools
POST   /api/v1/peer/tools/{toolName}/calls
GET    /api/v1/peer/models
```

Peer networking is disabled until every node has a shared `peerToken`. UDP
discovery is controlled by `peerDiscovery`; manually added peers remain
available when discovery is disabled.

## Authentication and Network

`/health` is public. If `authToken` is empty, control and MCP routes are open
for trusted local development. Set a token before binding to a LAN that is not
fully trusted. Lociant uses cleartext HTTP for local-network interoperability;
do not expose port `11434` directly to the public Internet.

## Verification

The probe script checks the current edge contract: health, authentication,
model inventory, tool discovery, direct tool calls and MCP:

```bash
python scripts/lociant_test.py quick \
  --base-url http://HOST:11434 \
  --api-key TOKEN \
  --expect-auth
```
