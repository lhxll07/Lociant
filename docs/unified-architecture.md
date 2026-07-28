# Lociant 1.0 Unified Architecture

## Goals

Lociant follows two related constraints:

1. Each component has one visible owner and communicates through a narrow contract.
2. A caller should be able to predict behavior from the method, HTTP verb, path and protocol it selected.

Version 1.0 applies those constraints without a pre-1.0 compatibility layer.

## Component Flow

```text
Android UI
  |
  | explicit LociantBridge methods
  v
LociantRuntimeService ---- RuntimeWindowController
  |
  | owns lifecycle
  v
LociantServer
  |---- OpenAI adapter ---- ChatController ---- ChatRequestQueue
  |                                  |              |
  |                                  v              v
  |                           SessionStore    ChatCapability
  |                                                  |
  |                                                  v
  |                                             MnnRuntime
  |
  |---- MCP adapter ------ ToolRegistry ------ Tool providers
  |
  `---- Control API ------ stores/models/sessions/settings
```

`LociantRuntime` creates the process-scoped objects. Modules do not create alternate stores, model managers, tool registries or HTTP servers.

## Protocol Separation

### OpenAI data plane

Only supported OpenAI resources use `/v1`:

```text
GET  /v1/models
POST /v1/chat/completions
```

Responses and errors use OpenAI shapes. Streaming uses `text/event-stream` and terminates with `data: [DONE]`.

### MCP

MCP uses one Streamable HTTP route:

```text
POST /mcp
```

The adapter translates MCP tool descriptors and calls to the shared `ToolRegistry`. It does not own a second capability catalog.

### Lociant control plane

Product-specific resources use `/api/v1`. They follow HTTP resource semantics and return Problem Details for request failures. Control routes never appear as undocumented OpenAI extensions.

## Lifecycle

The foreground service is the only server lifecycle owner. This prevents Activity recreation, boot handling and notification actions from racing to start separate server instances.

The Activity can:

- request foreground-service start or stop;
- open Android permission/settings screens;
- control the floating window;
- import a model through the Android document picker.

It cannot bind a server port directly. The HTTP API exposes runtime state but cannot start a server that is not running or bypass Android foreground-service rules.

## State Ownership

| Owner | State | Invariant |
|---|---|---|
| `SessionStore` / Room | sessions, messages, events | IDs are validated; reads never create rows |
| `LocalStore` / `AtomicFile` | runtime and window settings | memory changes only after durable commit |
| `ModelManager` | model filesystem index | readers use an immutable cached snapshot |
| `ChatRequestQueue` | queued/running inference | one bounded queue owns cancellation and timeout |
| Native prompt cache | active model session | selecting another session resets the cache |
| `ToolRegistry` | executable capabilities | one policy check precedes every handler call |

The Room schema contains no Scene abstraction. API request telemetry is stored as events, not as a synthetic chat session.

## Tool Security

`ToolPolicy` separates distinct questions:

- `local`: can this process execute it?
- `remoteAllowed`: may HTTP/MCP callers execute it?
- `requiresActivity`: does it depend on interactive Android state?
- `sideEffect`: can it change phone state?
- `destructive`: can it irreversibly remove or overwrite meaningful state?
- `openWorld`: does it interact with entities outside the server's closed data set?

Exposure levels (`read`, `sensor`, `action`) filter the manifest and execution. `remoteAllowed` is enforced again at execution, so metadata cannot diverge from behavior.

## Performance Rules

- Ordinary model reads never scan the external model directory.
- GET model/tool discovery does not insert telemetry events.
- `LocalStore` does not reparse JSON from disk per read.
- Server start uses a single daemon executor and fails explicitly on port conflict.
- Chat uses one bounded inference queue rather than spawning a thread per request.
- Large image payloads are stripped from MCP structured results after being emitted as MCP image content.

Performance changes must report a before/after measurement for cold start, idle memory, first-token latency, decode throughput, API P95 or APK size. Structural cleanup alone is not a performance result.

## Failure Semantics

- Invalid JSON: `400`, never `{}` fallback.
- Invalid identifier: `400`, never normalized to another resource.
- Missing resource: `404`, never an empty success object.
- Accepted asynchronous work: `202`.
- Occupied port: startup failure visible in runtime state; no random port substitution.
- OpenAI failures: OpenAI error object.
- MCP failures: JSON-RPC error when the message reached MCP processing.
- Control failures: `application/problem+json`.

## Breaking Identity

The Android identity is `io.lociant.android`. Source packages, Native targets, JNI exports, thread names, notification channel and database use Lociant names.

There is no migration for earlier application IDs, databases, settings, model directories, headers, routes, JavaScript objects or client scripts. OpenAI and MCP remain because they are current product protocols, not compatibility aliases.

## Acceptance Gates

1. Source scan contains no retired identifiers or routes.
2. JVM contract tests and Android Room test compilation pass.
3. Debug APK assembly and lint pass.
4. Native libraries load for every packaged ABI and the APK passes 16 KB alignment verification.
5. On-device probes verify auth, models, tools, MCP, non-streaming Chat and SSE Chat.
