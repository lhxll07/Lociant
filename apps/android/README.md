# Lociant Android Development

This directory contains the complete Android runtime. Version 1.0 intentionally starts a new application identity, database, package tree, JNI surface, and HTTP contract.

## Modules

| Module | Package owner | Responsibility |
|---|---|---|
| `:app` | `io.lociant.android` | Activity, WebView, foreground service, server lifecycle and routes |
| `:core` | `io.lociant.core` | Stable API paths, session ID rules, model contracts and tool policy |
| `:data` | `io.lociant.data` | Room sessions/messages/events and AtomicFile JSON storage |
| `:local-runtime` | `io.lociant.runtime` | MNN/NCNN integration, model installation/catalog and vision pipeline |
| `:phone-tools` | `io.lociant.tools` | Android device capabilities, accessibility and tool providers |
| `:mcp` | `io.lociant.mcp` | MCP JSON-RPC adapter over the shared tool registry |

Dependencies flow toward `:core`. `:phone-tools` does not depend on `:data`; persistence is owned by the application composition root.

## Runtime Ownership

`LociantRuntimeService` is the only owner of HTTP server start and stop. `MainActivity` may request service lifecycle through explicit Android intents, but it never starts `LociantServer` directly.

`LociantRuntime` is the process-level composition root. It creates one immutable set of long-lived dependencies:

```text
LocalStore + SessionStore + ModelManager + MnnRuntime
                         |
                  ChatCapability
                         |
                    LociantServer
```

The WebView interface is named `LociantBridge`. Every exported method represents one concrete operation. Do not restore a generic `command: String` method or route arbitrary JSON through the bridge.

## HTTP Contracts

Canonical paths live in `io.lociant.core.api.ApiContract`. Route code must use these constants for fixed public paths.

- `/v1/*` is reserved for supported OpenAI endpoints.
- `/mcp` is reserved for MCP Streamable HTTP POST.
- `/api/v1/*` contains Lociant-owned resources.
- `/health` is the only public endpoint.

Control errors use `application/problem+json`. Invalid JSON must produce `400`; missing resources must produce `404`. Never convert malformed input to an empty object or invent a resource ID.

The server fails startup if its configured port is occupied. It must not choose and persist a random replacement port.

## Persistence

Room schema version 1 contains only:

- `sessions`: explicit chat resources
- `messages`: persisted conversation turns
- `events`: runtime and request telemetry

The database is `lociant.db`. There are no legacy migrations and no Scene fields. Session reads and deletes validate the ID and require an existing row.

`LocalStore` loads `store/local-store.json` once. Reads return defensive copies from memory. Writes clone the root, commit through `AtomicFile`, and publish the new in-memory root only after the disk commit succeeds.

## Models And Native Runtime

Imported models are stored under:

```text
Android/data/io.lociant.android/files/models/<model-id>/
```

`ModelManager` maintains a locked immutable directory snapshot. Import, successful catalog installation, delete, and explicit management refresh invalidate it. Ordinary model listing must not rescan the filesystem.

Native libraries are `lociant_mnn` and `lociant_ncnn`. JNI symbols target `io_lociant_runtime_model_*`. When changing package or class names, update Kotlin load calls, JNI exports and CMake targets together.

## Tool Policy

All HTTP, OpenAI tool execution and MCP calls use one `ToolRegistry`.

Policy is enforced before the handler runs:

- exposure level must allow the tool;
- `local` must permit in-process execution;
- remote calls must satisfy `remoteAllowed`;
- MCP annotations derive from explicit `destructive` and `openWorld` fields.

Do not infer `openWorldHint` from network reachability. It describes interaction with the world outside the server, not whether a remote client may call the tool.

## Web UI

Editable source is under `app/src/main/web-src`. `build.py` combines it into `app/src/main/assets/web`; Gradle runs that build before Android compilation.

The UI may call explicit Bridge methods for operations that require an Activity or Android service lifecycle. Data-plane and remote-client behavior belongs in HTTP/MCP, not in a hidden parallel JavaScript API.

## Build And Test

```bash
bash gradlew testDebugUnitTest \
  :data:compileDebugAndroidTestKotlin \
  :app:assembleDebug \
  :app:lintDebug
```

Run connected Room tests when a device or emulator is available:

```bash
bash gradlew :data:connectedDebugAndroidTest
```

After installation, start Runtime from the UI and probe it:

```bash
python ../../scripts/lociant_test.py quick \
  --base-url http://PHONE_IP:11434 \
  --api-key TOKEN \
  --expect-auth
```

Native changes additionally require both ABI load tests and 16 KB alignment verification:

```bash
$ANDROID_SDK_ROOT/build-tools/36.0.0/zipalign -c -P 16 4 \
  app/build/outputs/apk/debug/app-debug.apk
```

## Review Checklist

- No `com.mnnode`, `MNNode`, old request headers, Scene schema, or Ollama routes.
- No Activity-owned server lifecycle or string command dispatcher.
- New control routes are under `/api/v1` and documented.
- GET discovery does not record database events.
- Input errors have correct HTTP status and protocol-specific shape.
- Remote tool policy is checked by `ToolRegistry.call`.
- Unit tests, Android test compilation, APK assembly and lint pass.
