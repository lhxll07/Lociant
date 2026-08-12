# Lociant Architecture

Lociant is a local-first agent runtime: a Rust backend owns the service
surface, one shared Flutter UI talks to it everywhere, and Android only keeps
the device layer the backend cannot reach directly.

## Components

```text
Flutter UI (apps/flutter)
  ├─ Android: hybrid PlatformService (HTTP core + method channel device ops)
  └─ Linux desktop: HTTP PlatformService
        │ HTTP (OpenAI /v1, control /api/v1, MCP /mcp, auth)
        ▼
Rust backend (apps/rust-backend) ── the single server core
  ├─ crates/core    domain types + JSON contract
  ├─ crates/store   SQLite: sessions, messages, settings, installed models
  ├─ crates/tools   tool contract + registry (single policy owner)
  ├─ crates/agent   multi-round agent loop + cloud/local chat backends
  └─ crates/server  axum HTTP server: control plane, OpenAI plane, MCP
        │ device IPC (localhost TCP, token-gated)
        ▼
Android device layer (apps/android, Kotlin)
  ├─ foreground service spawns the Rust server subprocess
  ├─ phone tools (accessibility, sensors, window, camera)
  ├─ local MNN inference + vision
  └─ persisted device settings
```

## Seams

### HTTP contract

The backend implements the documented contract exactly once, in Rust:

- `/v1` — OpenAI-compatible chat completions (SSE), models;
- `/api/v1` — sessions, settings, models, model installs, tools, chat requests;
- `/mcp` — MCP Streamable HTTP (stateless JSON responses);
- `/health` — the only public endpoint.

The complete external contract is documented in `agent-integration.md`.
The Flutter UI and external agents only speak this contract.

### Device IPC

The Rust server never touches a device directly. `lociant-tools::DeviceAdapter`
is the only capability seam:

- Android: `IpcDeviceAdapter` talks to Kotlin's `DeviceAdapterServer` over
  localhost TCP JSON (`tools.list`, `tools.call`, `models.list`,
  `chat.invoke`). A random token is passed through the spawn environment so
  other local processes cannot call phone tools.
- Linux desktop: no-op until a desktop device adapter exists.

`ToolRegistry` is the single policy owner: exposure levels (`read` < `sensor`
< `action`) and `remote_allowed` are checked before any adapter call. The
agent loop runs in-process (`call_local`); HTTP/MCP callers use
`call_remote`. Kotlin's registry executes without re-deciding policy.

### Data ownership

- Sessions, messages, settings and the installed-model index live in the Rust
  backend's SQLite.
- Android device state (permissions, floating window, vision) is merged into
  the runtime snapshot from Kotlin (`deviceState`).
- Model files are shared: on Android `LOCIANT_MODELS_DIR` points at the same
  external directory the Kotlin model manager scans, so catalog installs are
  immediately visible to MNN.

## Lifecycle

- Android: `LociantRuntimeService` (foreground service) spawns the Rust
  server as a subprocess (`RustServerProcess`) on port 11434 and stops it with
  the service. The old Ktor HTTP server is retired.
- Linux desktop: `flutter build linux` bundles `lociant-server` under `bin/`;
  the Flutter app spawns it as a sidecar on startup and stops it on exit
  (`DesktopServerProcess`), so the desktop app is self-contained.

## Headless / Board Deployment

The backend runs headless on a Linux board (e.g. RK3588/Armbian) as a
systemd service; the Flutter UI or MCP/OpenAI clients connect over the LAN.

Environment knobs:

- `LOCIANT_HOST` — bind address (default `127.0.0.1`; use `0.0.0.0` for LAN);
- `LOCIANT_CONFIG` — JSON bootstrap file (`authToken`, cloud settings, port)
  merged over stored settings on every start, so a board can be configured
  without a UI;
- `LOCIANT_DATA_DIR` — persistent data location;
- `LOCIANT_MODELS_DIR` — model files location.

Deployment assets live in `deploy/`: `lociant.service` (systemd unit),
`config.example.json` and `install.sh`. Cross-compile for the board:

```bash
rustup target add aarch64-unknown-linux-gnu
CC_aarch64_unknown_linux_gnu=aarch64-linux-gnu-gcc \
CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc \
cargo build --release --target aarch64-unknown-linux-gnu
bash deploy/install.sh   # on the board, with sudo
```

Local NPU inference is built into the Rust backend (`crates/rkllm`): it loads
`librkllmrt.so` at runtime via `libloading` and runs the `.rkllm` model
in-process, so a board needs one binary, one systemd service and one model
file — no Python/venv, no extra port. Configure `rkllmModelPath` (plus
optional `rkllmLibPath`) in the headless config or pick the `rkllm` backend
in `lociant-server --init`. Verified on the board with Qwen3.5-0.8B. W4A16/G128
quantization is verified by the load log (`model_dtype: W4A16_G128`,
`max_context_limit: 8192`); converting with `optimization_level=1` silently
falls back to W8A8, so export with `optimization_level=0`. The `.rkllm` file
size alone does not distinguish the two (both are ~1.3 GB).

## Status

- [x] Rust backend: sessions/messages/settings (SQLite), auth, model catalog
  + install, cloud chat (SSE), agent loop (rounds, tool execution, phase
  events), MCP, tool registry with policy.
- [x] Flutter UI on Android and Linux desktop (HTTP `PlatformService`).
- [x] Android host: Rust server in the foreground service, Kotlin as the
  device layer over IPC, Ktor server retired.
- [x] Headless board deployment: aarch64 Linux cross-build, systemd service,
  `LOCIANT_CONFIG` bootstrap, LAN bind + auth. Verified on an RK3576/Armbian
  board.
- [x] Board local inference: RKLLM runtime integrated in-process
  (`crates/rkllm`, libloading, W4A16 verified).
- [x] Peer networking: optional UDP broadcast discovery (`11435`,
  `peerDiscovery`, enabled by default), shared peer
  token, remote tools over `/api/v1/peer/*` (provider enforces its own
  exposure), peer model forwarding (`peer:<node>:<model>`) via the OpenAI
  plane, `/api/v1/nodes` for the UI. Manual peers remain available when
  discovery is disabled.
- [ ] Desktop local inference (llama.cpp, x86_64 machines without RKLLM).
- [ ] Desktop device adapter (filesystem/process/camera tools).
- [x] Bundle the Rust server as a sidecar in the Flutter desktop app.

## Run

Linux desktop (two terminals):

```bash
cd apps/rust-backend && cargo run          # http://127.0.0.1:11434
cd apps/flutter && flutter run -d linux     # UI; LOCIANT_BASE_URL overridable
```

Android: `bash scripts/dev-install.sh` builds the APK (including the Rust
server via `cargo ndk`), installs and launches it.
