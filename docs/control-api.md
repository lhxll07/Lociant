# Lociant Control API

The control plane manages Lociant-owned resources. Every path starts with
`/api/v1` and requires authentication when a token is configured. These
endpoints are implemented by the Rust backend (`apps/rust-backend`); see
`architecture.md` for the component layout.

## Errors

Failures use `application/problem+json`:

```json
{
  "type": "https://lociant.io/problems/not-found",
  "title": "Resource not found",
  "status": 404,
  "detail": "Session not found: chat_example",
  "code": "not-found",
  "instance": "/api/v1/sessions/chat_example"
}
```

Success responses do not add a redundant top-level `ok`. Tool calls retain their own execution result envelope because tool failure is domain output.

## Runtime And Settings

```text
GET /api/v1/runtime
GET /api/v1/settings
PUT /api/v1/settings
```

`GET runtime` reports server, model, queue and device state. Runtime
start/stop is intentionally absent: on Android the foreground service owns
the runtime process, and on desktop the server is always running.

`PUT settings` merges supplied fields into current settings. Supported fields include `port`, `modelId`, `maxOutputTokens`, `cpuThreads`, `inferenceBackend`, `cloudBaseUrl`, `cloudApiKey`, `cloudModel`, `cloudEnabled`, `contextProfile`, `historyLimit`, `agentMaxRounds`, `authToken`, `toolExposure`, `autoStart` and `currentSessionId`. When `cloudEnabled` is true and `cloudBaseUrl`/`cloudModel` are set, Lociant registers the cloud model as a ready model and routes chat requests for it to the OpenAI-compatible endpoint; the API key is stored in the local settings store. `inferenceBackend` selects the MNN engine backend (`model` follows the model config, or `cpu`, `opencl`, `vulkan`, `auto`); a leftover crash marker left by a previous abnormal exit automatically resets a non-CPU backend to `model`. `agentMaxRounds` bounds the model↔tool loop for home chat and `execute_tools` requests (default `32`, range `8`–`64`); the total tool-call budget for a task is fixed at `64`.

Changing `port` takes effect on the next service start. An occupied port produces a visible startup error and is never replaced automatically.

## Models

```text
GET    /api/v1/models?refresh=false
DELETE /api/v1/models/{modelId}
GET    /api/v1/catalog/models?q=QUERY&refresh=false
POST   /api/v1/model-installations
GET    /api/v1/model-installations/{jobId}
```

Installation body:

```json
{"modelId": "vendor/repository"}
```

Starting installation returns `202`. The current catalog model ID is also the job ID used by the progress resource:

```json
{
  "jobId": "qwen3.5-2b-mnn",
  "modelId": "qwen3.5-2b-mnn",
  "message": "installing"
}
```

Progress returns the same IDs plus `state`, `active`, `progress`, and `message`. Terminal states are `done` and `error`.

`refresh=true` is for filesystem/catalog changes made outside Lociant. Normal imports, successful installations and deletes invalidate model snapshots themselves.

## Sessions

```text
GET    /api/v1/sessions
POST   /api/v1/sessions
GET    /api/v1/sessions/{sessionId}
DELETE /api/v1/sessions/{sessionId}
```

Creation returns `201` and the created session. IDs are generated only by the create operation. Reads and deletes never generate or normalize IDs.

Valid IDs contain 1-96 ASCII letters, digits, dots, underscores or hyphens. A valid but unknown ID returns `404`.

## JSON Store

```text
GET    /api/v1/store/{namespace}
GET    /api/v1/store/{namespace}/{key}
PUT    /api/v1/store/{namespace}/{key}
DELETE /api/v1/store/{namespace}/{key}
```

Write body:

```json
{"value": {"theme": "dark"}}
```

Namespace and key allow 1-96 letters, digits and `._:/-`. Writes are atomic copy-on-write commits. A missing key is represented by JSON `null`; deletion reports whether a value existed.

## Tools

```text
GET  /api/v1/tools
POST /api/v1/tools/{name}/calls
```

Call body:

```json
{"arguments": {"package": "com.example.app"}}
```

The route uses the same registry and policy as MCP and OpenAI tool execution.

## Chat Requests

```text
GET /api/v1/chat-requests
GET /api/v1/chat-requests/{requestId}
```

These resources expose bounded inference-queue state for requests submitted with the OpenAI extension `async: true`.
