# Lociant Architecture

Lociant is an edge-device runtime. The Rust backend owns the control plane,
model inventory, peer networking and tool policy. Android supplies device-only
capabilities such as accessibility, sensors, windows, camera and NCNN vision.
GGUF inference is provided by a local llama.cpp process. The Flutter UI is a
control console, not a chat client.

## Components

```text
Flutter UI (apps/flutter)
  ├─ Android: hybrid PlatformService
  │           HTTP control + method-channel device operations
  └─ Linux desktop: HTTP PlatformService
        │ HTTP (control /api/v1, MCP /mcp, auth)
        ▼
Rust backend (apps/rust-backend)
  ├─ crates/core    runtime and tool JSON contracts
  ├─ crates/store   SQLite settings, model index and legacy data tables
  ├─ crates/tools   ToolRegistry and policy enforcement
  ├─ crates/server  axum control API, MCP, model and peer routes
  └─ crates/rkllm   in-process RKLLM NPU runtime
        │ device IPC (localhost TCP, token-gated)
        ▼
Android device layer (apps/android, Kotlin)
  ├─ foreground service spawns the Rust server subprocess
  ├─ phone tools (accessibility, sensors, window, camera)
  └─ NCNN vision runtime
```

There is intentionally no `crates/agent` and no general-purpose server-side
Agent loop. A client may orchestrate calls through MCP, while each tool call
is executed and authorized by the runtime that owns the device.

## HTTP Contract

The backend implements the external contract once, in Rust:

- `/api/v1` provides runtime state, settings, model management, nodes and
  direct tool calls;
- `/mcp` provides stateless MCP Streamable HTTP for tool discovery and calls;
- `/health` is the public liveness endpoint.

The former OpenAI `/v1` data plane and session routes are removed. SQLite keeps
the old sessions/messages tables for database compatibility, but no current
HTTP route creates or serves those records.

## Device IPC

The Rust server never reaches into Android APIs directly. The
`IpcDeviceAdapter` is the single capability seam and talks to Kotlin's
`DeviceAdapterServer` over localhost TCP JSON:

- `tools.list` and `tools.call` expose Android capabilities;
- `models.list` and `models.invalidate` synchronize the model inventory;
- `settings.sync` pushes Rust-owned runtime settings to Kotlin;
- model inventory and settings synchronization keep GGUF files and the device
  runtime aligned; llama.cpp inference stays behind the local runtime boundary.

The random IPC token is passed through the Rust process environment so another
local process cannot call the phone tools. Linux uses `LinuxDevice` for the
desktop filesystem and process capabilities.

`ToolRegistry` is the policy owner. It filters descriptors by exposure
(`read`, `sensor`, `action`) and enforces `remoteAllowed` before every HTTP or
MCP call. Peer adapters are attached dynamically, but peer-plane listing and
calls are restricted to local adapters so requests cannot recurse through the
mesh.

## Data Ownership

- Rust Store owns settings and the installed-model index.
- The legacy sessions/messages tables remain readable by the Store for
  migration compatibility; they are no longer part of the product surface.
- Android owns permissions, floating-window state, vision state and native
  runtime caches.
- Model files are shared between Rust and Android. On Android,
  `LOCIANT_MODELS_DIR` points to the directory scanned by `ModelManager`, so
  Rust catalog installs become visible after cache invalidation.

## Lifecycle

- Android `LociantRuntimeService` starts and stops the Rust server subprocess.
  `MainActivity` requests service lifecycle and Android-only permissions.
- Linux desktop starts the bundled `lociant-server` sidecar from Flutter and
  stops it with the app.
- Headless boards run `lociant-server` directly under systemd and use the TUI
  or a remote Flutter console.

## Headless and Board Deployment

The backend runs headless on Linux boards such as RK3588/RK3576 Armbian.
Important environment settings are:

- `LOCIANT_HOST`: bind address, default `127.0.0.1`;
- `LOCIANT_CONFIG`: JSON bootstrap file merged over stored settings;
- `LOCIANT_DATA_DIR`: persistent data directory;
- `LOCIANT_MODELS_DIR`: model file directory.

The bootstrap file contains local runtime and mesh settings, for example:

```json
{
  "host": "0.0.0.0",
  "port": 11434,
  "authToken": "replace-me",
  "peerToken": "shared-lan-token",
  "rkllmModelPath": "/opt/models/qwen.rkllm",
  "rkllmModelName": "qwen-local"
}
```

Local RKLLM inference is loaded through `crates/rkllm` and `libloading`.
The optional llama.cpp process manager follows the same local-runtime model
and reports its model in the control inventory.

## Status

- [x] Rust control API, MCP transport, auth and model catalog/install flow.
- [x] Shared ToolRegistry with exposure and remote-call policy.
- [x] Android device layer over token-gated IPC with NCNN vision support.
- [x] Headless RKLLM runtime and terminal edge-node console.
- [x] Opt-in peer networking for tools and model inventory.
- [x] Desktop device adapter for filesystem and process tools.
- [x] Flutter edge overview for runtime, models, tools and nodes.
- [x] Removal of the generic Agent crate, chat home and server-side Agent loop.

## Run

Linux desktop:

```bash
cd apps/rust-backend && cargo run
cd apps/flutter && flutter run -d linux
```

Android development:

```bash
bash scripts/dev-install.sh
```

The Android build bundles the Rust server and the Kotlin device layer.
