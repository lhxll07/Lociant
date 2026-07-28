# OpenAI API

Lociant implements a deliberately small OpenAI-compatible data plane. It does not place model management, sessions, key-value storage or runtime commands under `/v1`.

## Connection

```text
Base URL: http://PHONE_IP:11434/v1
API key:  token configured in the Lociant app
Model:    one id returned by GET /v1/models
```

When a token is configured, both endpoints require one of:

```http
Authorization: Bearer TOKEN
X-Lociant-Token: TOKEN
```

`GET /health` is public. `GET /v1/models` is protected because every endpoint except health follows the same authentication rule.

## Models

```http
GET /v1/models
```

The response is the standard list shape and contains only ready chat-capable models:

```json
{
  "object": "list",
  "data": [
    {
      "id": "qwen3.5-2b-mnn",
      "object": "model",
      "created": 0,
      "owned_by": "mnn"
    }
  ]
}
```

Complete model status and management are available through `/api/v1/models`, not an OpenAI extension.

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

Supported message content includes plain strings and OpenAI content arrays containing `text` and base64/data-URL `image_url` items. Image input requires a ready VLM model.

The response includes standard `choices` and `usage`. Additional runtime measurements are placed in `lociant`; no retired diagnostics field is emitted.

## Streaming

Set `"stream": true`. Lociant returns `text/event-stream`, emits Chat Completion chunks and ends with:

```text
data: [DONE]
```

To request a final usage-only chunk:

```json
{
  "stream": true,
  "stream_options": {"include_usage": true}
}
```

## Tool Calls

OpenAI `tools` and `tool_choice` are accepted. A forced or model-generated call is returned as a standard assistant `tool_calls` response.

Lociant does not execute a client-declared tool merely because its name appears in the request. Execution additionally requires the tool to exist in the shared registry and pass exposure and remote policy.

The Lociant extension `execute_tools: true` asks the phone to execute an allowed local tool and continue the model turn. Clients that need portable OpenAI behavior should omit it and execute returned tool calls themselves.

## Sessions

OpenAI has no standard persistent-session resource. Lociant offers an explicit opt-in extension:

```http
X-Lociant-Session-Id: chat_0123456789abcdef
```

The same value may be supplied as `sessionId` in the request body. A body value takes precedence over the header.

The session must already exist through `POST /api/v1/sessions`. Invalid IDs return `400`; unknown IDs return `404`. Omitting a session runs a stateless request and does not create a database row.

## Asynchronous Requests

`"async": true` returns `202` and a request ID. Inspect it through:

```text
GET /api/v1/chat-requests/{requestId}
```

Queue state is available at `GET /api/v1/chat-requests`.

## Errors

OpenAI endpoint errors use an OpenAI error object:

```json
{
  "error": {
    "message": "...",
    "type": "invalid_request_error",
    "code": "invalid_request"
  }
}
```

HTTP status remains authoritative. Clients must not treat a JSON body alone as success.

## Removed Interfaces

Lociant 1.0 does not implement Ollama `/api/chat`, old session headers, or product-control routes under `/v1`. Clients must use the paths documented above and in [control-api.md](control-api.md).
