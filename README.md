**English** | [简体中文](README.zh-CN.md)

<div align="center">

# Lociant

### Turn every device around you into an edge runtime

*Your next AI device might not be a new computer — it might be the old phone in your drawer.*

[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/lhxll07/Lociant.svg?style=social&label=Star)](https://github.com/lhxll07/Lociant)

<img src="docs/media/social-preview.png" alt="Lociant" width="720">

</div>

Lociant turns an ordinary device — an old Android phone, a Linux desktop, or a Rockchip dev board — into a controllable edge runtime. It runs local models, exposes device capabilities, reports its state, and connects with other nodes over the LAN. External clients such as Claude, Codex and OpenCode can orchestrate those capabilities through MCP, while execution and policy stay on the device.

## What it does

- **Make an old phone useful 24/7.** It can expose screen, accessibility, sensor, camera and app capabilities to a trusted client.
- **Works at the edge.** Phones can run local GGUF models through llama.cpp, while Rockchip boards use the RKLLM NPU runtime. The runtime does not depend on a hosted chatbot.
- **Connect devices into a local mesh.** Phones, desktops and boards can share model inventory and device tools over an authenticated LAN.
- **Connect any orchestrator.** MCP and the control API provide stable discovery and execution surfaces for Claude, Codex, OpenCode or your own client.
- **Keep policy close to hardware.** Exposure levels and remote-call permissions are enforced by the owning runtime before a tool runs.

The result is a small, inspectable runtime that a client can use to open an app, read a screen, inspect sensors, capture a camera frame, or invoke one local model turn.

## Three ways to run

| Device | What you get |
|---|---|
| Android phone | Device runtime: local GGUF/llama.cpp + phone tools (screen, tap, sensors, camera) |
| Linux desktop (x86_64) | Flutter control console + bundled Rust edge backend |
| RK board (Armbian, headless) | systemd edge node + RKLLM NPU runtime + terminal TUI |

## Quick start

Pick your device and follow the matching chapter of the **[setup guide (from zero)](docs/setup-guide.md)**:

- **Android phone** — device runtime (local model + phone tools)
- **Linux desktop** — Flutter UI + built-in Rust backend, one tarball
- **RK board (headless)** — systemd service + RKLLM NPU inference + TUI

Current release (v2.0.1):

- [Android APK (arm64-v8a)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk)
- [Linux x86_64 desktop (tar.gz)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-x86_64.tar.gz)
- [Linux aarch64 board (tar.gz)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-aarch64.tar.gz)

On Debian/Ubuntu, install the [x86_64 desktop DEB](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant_2.0.1_amd64.deb) or the [arm64 headless node DEB](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-node_2.0.1_arm64.deb).

## Demo

<video src="docs/media/lociant-1.1.0-demo.mp4" controls width="100%"></video>

*Demo from v1.1.0 — the UI has moved on, but this shows the phone taking real actions.*

## Connect to the runtime

Lociant exposes one HTTP surface (port `11434`) from its Rust backend:

- **MCP Streamable HTTP** — `http://HOST:11434/mcp` for device tools. Works with Claude Desktop/Code, Codex, OpenCode and RikkaHub.
- **Control API** — `http://HOST:11434/api/v1` for runtime state, settings, models, nodes and direct tool calls.
- **Health** — `http://HOST:11434/health` for liveness checks.

Tool families exposed to external clients:

| Family | Examples |
|---|---|
| Runtime & model | `runtime_status`, `model_list` |
| Device | `device_status`, `clipboard_read/write`, `app_open` |
| Sensors | `sensor_status`, `sensor_read`, `sensor_start/stop` |
| Screen & UI | `ui_screen_state`, `ui_click_node`, `ui_tap`, `ui_swipe`, `ui_paste`, `ui_set_text` |
| Vision | `vision_status`, `camera_capture` |

Every tool carries a policy: exposure levels (`read` < `sensor` < `action`) and `remote_allowed` are enforced by the backend before anything touches your device. Configure a token before binding Lociant to an untrusted network. See the [edge runtime integration docs](docs/agent-integration.md) for client configs and the full API.

## How it works

One Flutter UI + one Rust backend on every platform. Android keeps only the device layer the backend cannot reach directly; headless boards run the backend alone.

```text
Flutter UI (Android / Linux desktop)
        │ HTTP (control /api/v1, MCP /mcp)
        ▼
Rust backend (axum) ── the single server core
  ├─ core    domain types + JSON contract
  ├─ store   SQLite: settings, models, legacy data tables
  ├─ tools   tool contract + registry (single policy owner)
  ├─ server  HTTP: control plane, MCP, peers
  └─ rkllm   in-process RKLLM NPU inference (libloading)
        │ device IPC (localhost TCP, token-gated)
        ▼
Android device layer (Kotlin)
  ├─ foreground service spawns the Rust server subprocess
  ├─ phone tools (accessibility, sensors, window, camera)
  └─ llama.cpp GGUF models + NCNN vision
```

Peer mesh (opt-in with a shared peer token): nodes discover each other via UDP broadcast (port `11435`) and share tools and model inventory through the control plane. Details in [architecture](docs/architecture.md).

## Documentation

- [Setup guide (from zero)](docs/setup-guide.md) — install, configure and connect all platforms
- [Edge runtime integration](docs/agent-integration.md) — MCP, control API and tool policy
- [Architecture](docs/architecture.md) — components, seams, headless deployment
- [Android development](apps/android/README.md)

## Development

- UI: Flutter (`apps/flutter`)
- Backend: Rust (`apps/rust-backend`)
- Android device layer: Kotlin (`apps/android`)

Run on Linux desktop (two terminals):

```bash
cd apps/rust-backend && cargo run          # http://127.0.0.1:11434
cd apps/flutter && flutter run -d linux     # UI; LOCIANT_BASE_URL overridable
```

Android: `bash scripts/dev-install.sh` builds the APK (Rust server included via `cargo ndk`), installs and launches it.

## Roadmap

- [x] Rust backend, Flutter UI (Android + Linux desktop)
- [x] Android device layer over IPC, llama.cpp GGUF models and NCNN vision
- [x] Headless RK board deployment (aarch64, systemd) + RKLLM NPU inference
- [x] Peer mesh: LAN discovery, shared tools, model forwarding
- [x] Desktop local inference via first-class llama.cpp backend
- [x] Desktop device adapter (filesystem / process tools)

## License

[MIT](LICENSE)

---

Star us if Lociant is useful — it tells us this direction matters. ⭐
