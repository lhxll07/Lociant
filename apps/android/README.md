# Lociant Android Development

This directory contains the complete Android runtime. Version 1.0 intentionally starts a new application identity, database, package tree, JNI surface, and HTTP contract.

## Modules

| Module | Package owner | Responsibility |
|---|---|---|
| `:app` | `io.lociant.android` | Flutter UI host, foreground service, device-layer composition root |
| `:core` | `io.lociant.core` | Stable API paths, session ID rules, model contracts and tool policy |
| `:data` | `io.lociant.data` | AtomicFile JSON storage (device settings) |
| `:local-runtime` | `io.lociant.runtime` | GGUF model import, NCNN vision runtime and model inventory |
| `:phone-tools` | `io.lociant.tools` | Android device capabilities, accessibility and tool providers |

The HTTP server and MCP adapter moved to the Rust backend (`apps/rust-backend`);
the Android app is now the device layer: foreground service, phone tools, local
NCNN vision, exposed to Rust over the device IPC. GGUF inference is supervised
by the Rust backend through an optional llama.cpp `llama-server` bundle
(`DeviceAdapterServer`). Dependencies flow toward `:core`; `:phone-tools` does
not depend on `:data`; persistence is owned by the application composition root.

## Runtime Ownership

`LociantRuntimeService` owns the foreground service lifecycle and spawns the
Rust server subprocess (`RustServerProcess`). `MainActivity` may request the
service lifecycle through explicit Android intents.

`LociantRuntime` is the process-level composition root. It creates one immutable set of long-lived dependencies:

```text
LocalStore + ModelManager
            |
       LociantServer
```

The Flutter UI talks to the Rust backend over the control API (`/api/v1`) and
MCP (`/mcp`); the method channel only carries Android-only device operations
(permissions, floating window, vision, lifecycle) and `deviceState`. Data-plane
behavior belongs in HTTP/MCP, not in the channel. See
[architecture.md](../../docs/architecture.md) and
[agent-integration.md](../../docs/agent-integration.md).

## HTTP Contracts

The HTTP contract is implemented by the Rust backend (port 11434): `/api/v1`
control plane, `/mcp` MCP, and `/health` public. The old OpenAI `/v1` data
plane is not part of the runtime.
Control errors use `application/problem+json`.

## Persistence

Runtime settings and the installed-model index live in the Rust backend's
SQLite; this module only keeps `LocalStore` (AtomicFile JSON) for the Android
boot flag and device settings.
`LocalStore` loads `store/local-store.json` once. Reads return defensive
copies from memory. Writes clone the root, commit through `AtomicFile`, and
publish the new in-memory root only after the disk commit succeeds.

## Models And Native Runtime

Imported models are stored under:

```text
Android/data/io.lociant.android/files/models/<model-id>/
```

`ModelManager` maintains a locked immutable directory snapshot. Import, successful catalog installation, delete, and explicit management refresh invalidate it. Ordinary model listing must not rescan the filesystem.

The Android JNI library is `lociant_ncnn`; the Rust server is packaged as
`liblociant_server.so`, and an optional llama.cpp bundle is packaged as
`libllama_server.so`. JNI symbols target `io_lociant_runtime_model_*`. When
changing package or class names, update Kotlin load calls, JNI exports and
CMake targets together.

## Tool Policy

All HTTP and MCP tool calls use one `ToolRegistry`.

Policy is enforced before the handler runs:

- exposure level must allow the tool;
- `local` must permit in-process execution;
- remote calls must satisfy `remoteAllowed`;
- MCP annotations derive from explicit `destructive` and `openWorld` fields.

Do not infer `openWorldHint` from network reachability. It describes interaction with the world outside the server, not whether a remote client may call the tool.

## Flutter UI

The UI is a Flutter module under `apps/flutter/`, embedded into this Android
app via add-to-app. The same Dart codebase is intended to run on other hosts
(for example the RK3588/Armbian backend) by swapping the `PlatformService`
implementation. The Android host exposes `LociantPlatformChannel` (method +
event channels) for operations that require an Activity or Android service
lifecycle; everything data-plane goes over HTTP/MCP.

## Build And Test

Building the APK now requires the Rust Android toolchain because the app
bundles the Rust backend server (`apps/rust-backend`):

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk   # or: yay -S cargo-ndk
```

The Gradle task `rustServerBinary` runs `cargo ndk build` automatically and
stages the binary into `jniLibs` before `preBuild`; `useLegacyPackaging`
extracts it to `nativeLibraryDir` so the app process can exec it (the default
no-extract layout is dlopen-only and the app domain cannot exec app data
files on this device).

```bash
bash gradlew testDebugUnitTest :app:assembleDebug :app:lintDebug
```

`testDebugUnitTest` requires JDK 21 (Robolectric needs it for SDK 36).

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
