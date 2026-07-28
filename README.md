# Lociant

Lociant turns an Android phone into a visible, local-first AI runtime. It serves local MNN language or vision-language models, exposes Android capabilities as policy-controlled tools, and supports OpenAI HTTP and MCP clients over the local network.

Lociant 1.0 is a clean break from every pre-1.0 build. The Android application ID is `io.lociant.android`; old packages, databases, settings, routes, headers, JavaScript bridges, JNI symbols, and Ollama compatibility are not migrated or accepted.

## What It Provides

- OpenAI-compatible model discovery and Chat Completions, including SSE streaming and tool calls.
- MCP Streamable HTTP at a single `POST /mcp` endpoint.
- A versioned Lociant control API under `/api/v1`.
- MNN-backed LLM/VLM inference and NCNN-backed continuous vision.
- Android status, clipboard, app launch, camera, screen context, and explicit UI-action tools.
- A foreground service and optional floating window for Android-compliant long-running work.
- A WebView application for runtime configuration, models, sessions, diagnostics, and permissions.

## Protocol Boundaries

The server has three deliberately separate surfaces:

| Surface | Paths | Contract |
|---|---|---|
| OpenAI data plane | `GET /v1/models`, `POST /v1/chat/completions` | OpenAI request, response, error, and SSE shapes |
| MCP | `POST /mcp` | JSON-RPC 2.0 and MCP Streamable HTTP |
| Lociant control plane | `/api/v1/*` | Resource-oriented JSON and Problem Details errors |

`GET /health` is the only unauthenticated endpoint. When an API token is configured, every OpenAI, MCP, and control endpoint requires `Authorization: Bearer <token>` or `X-Lociant-Token: <token>`.

The old `/api/chat`, `/v1/runtime/{command}`, `/v1/tools`, `/v1/models/full`, and similar mixed-purpose routes do not exist.

## Control API

Common operations:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/runtime` | Read runtime and inference state |
| `GET`, `PUT` | `/api/v1/settings` | Read settings or merge supplied updates |
| `GET` | `/api/v1/models` | List complete installed/built-in model state |
| `DELETE` | `/api/v1/models/{modelId}` | Delete an imported model |
| `GET` | `/api/v1/catalog/models` | Search the model catalog |
| `POST` | `/api/v1/model-installations` | Start installation with `{ "modelId": "..." }` |
| `GET` | `/api/v1/model-installations/{jobId}` | Read installation progress |
| `GET`, `POST` | `/api/v1/sessions` | List or create sessions |
| `GET`, `DELETE` | `/api/v1/sessions/{sessionId}` | Read or delete one session |
| `GET` | `/api/v1/store/{namespace}` | List a namespace |
| `GET`, `PUT`, `DELETE` | `/api/v1/store/{namespace}/{key}` | Read, set, or delete one JSON value |
| `GET` | `/api/v1/tools` | List tools allowed by current exposure policy |
| `POST` | `/api/v1/tools/{name}/calls` | Invoke a tool |
| `GET` | `/api/v1/chat-requests` | Inspect the inference queue |
| `GET` | `/api/v1/chat-requests/{requestId}` | Inspect one asynchronous request |

See [docs/control-api.md](docs/control-api.md) for exact behavior.

## Quick Probe

Start Runtime from the Android app, then run:

```bash
python scripts/lociant_test.py quick \
  --base-url http://PHONE_IP:11434 \
  --api-key YOUR_TOKEN \
  --expect-auth
```

OpenAI example:

```bash
curl http://PHONE_IP:11434/v1/chat/completions \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "qwen3.5-2b-mnn",
    "messages": [{"role": "user", "content": "Hello"}],
    "stream": false
  }'
```

MCP clients should connect directly to `http://PHONE_IP:11434/mcp`. `scripts/lociant_mcp_server.py` remains available only for clients that require a stdio MCP process.

## Android Build

Requirements:

- JDK 17
- Android SDK 36
- Android NDK `28.2.13676358`
- CMake 3.22.1

Build and verify:

```bash
cd apps/android
bash gradlew testDebugUnitTest :data:compileDebugAndroidTestKotlin :app:assembleDebug :app:lintDebug
```

The APK is written to `apps/android/app/build/outputs/apk/debug/app-debug.apk`.

## Source Layout

```text
apps/android/app/            Android UI, foreground service, HTTP server
apps/android/core/           API contract, model types, tool policy
apps/android/data/           Room sessions/events and AtomicFile store
apps/android/local-runtime/  MNN/NCNN model and vision implementations
apps/android/phone-tools/    Android capabilities and tool providers
apps/android/mcp/            MCP protocol adapter
docs/                        Protocol and architecture documentation
scripts/                     Probes, capture utility, stdio MCP adapter
tools/                       Native headers and prebuilt dependencies
```

Kotlin packages reflect those owners: `io.lociant.android`, `io.lociant.core`, `io.lociant.data`, `io.lociant.runtime`, `io.lociant.tools`, and `io.lociant.mcp`.

## Data And Upgrade Policy

Lociant 1.0 uses a new `lociant.db` Room database and new application-specific storage:

```text
Android/data/io.lociant.android/files/models/<model-id>/
```

There is intentionally no migration from old application IDs or databases. Upgrading requires a fresh installation, re-granting Android permissions, configuring a new API token, and importing models again.

Session IDs must contain 1-96 ASCII letters, digits, dots, underscores, or hyphens. Invalid or unknown IDs are rejected; they are never silently repaired or generated during reads.

## Design Rules

- The foreground service exclusively owns HTTP server lifecycle.
- The WebView bridge exposes explicit native methods, not a string command bus.
- OpenAI, MCP, and control errors keep their own protocol-appropriate shapes.
- Read-only discovery endpoints do not write API history.
- Tool exposure and `remoteAllowed` are enforced at execution time.
- Model directory scans use an immutable snapshot and explicit invalidation.
- Store reads use an in-memory snapshot; writes use copy-on-write `AtomicFile` commits.
- No pre-1.0 compatibility aliases may be added without defining a new versioned contract.

Detailed documents:

- [Android development](apps/android/README.md)
- [Unified architecture](docs/unified-architecture.md)
- [OpenAI API](docs/openai-compatible.md)
- [Control API](docs/control-api.md)
- [Agent and MCP integration](docs/agent-integration.md)
- [Native development tools](tools/README.md)

## License

[Apache License 2.0](LICENSE)
