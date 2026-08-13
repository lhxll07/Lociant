**English** | [简体中文](README.zh-CN.md)

<div align="center">

# Lociant

### Turn every device around you into an agent

*Your next AI device might not be a new computer — it might be the old phone in your drawer.*

[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/lhxll07/Lociant.svg?style=social&label=Star)](https://github.com/lhxll07/Lociant)

</div>

Lociant turns an ordinary device — an old Android phone, a Linux desktop, or a Rockchip dev board — into an agent that actually *does things*: it runs models, reads its own screen, taps buttons, fills forms, and connects with your other devices over the LAN. External agents (Claude, Codex, OpenCode) can call its device capabilities over MCP. Data stays on your devices by default, and every capability is granted one at a time.

## What it does

- **Turn an old phone into a 7×24 personal assistant.** It opens apps, reads unread messages, taps buttons, fills forms — it acts, it doesn't just chat.
- **Works offline.** Models run locally: MNN on phones, NPU (RKLLM) on Rockchip boards. Not a wrapper around an online chatbot.
- **One device is not enough? Mesh them.** Phones, desktops and dev boards discover each other on the LAN automatically and borrow each other's models and tools — a phone can use the big model running on your dev board.
- **Callable from any agent.** Claude, Codex and OpenCode reach its device capabilities through a standard MCP endpoint; an OpenAI-compatible API is also provided.
- **Perceives the real world.** Light, proximity, sensors, camera, screen state — the agent knows where it is and what it is doing.
- **Optional cloud brain.** Point it at any OpenAI-compatible cloud model. Local-first, cloud on demand.

Example: ask the agent to open QQ and see who messaged you, summarize your Bilibili feed, or fill in a form — or check whether the phone is in your pocket or on the desk before deciding to touch the screen.

## Three ways to run

| Device | What you get |
|---|---|
| Android phone | Full agent: cloud or local model + phone tools (screen, tap, sensors, camera) |
| Linux desktop (x86_64) | Self-contained tarball: Flutter UI + bundled Rust backend, runs out of the box |
| RK board (Armbian, headless) | systemd service + RKLLM NPU inference + terminal TUI, 7×24 at low power |

## Quick start

Pick your device and follow the matching chapter of the **[setup guide (from zero)](docs/setup-guide.md)**:

- **Android phone** — full agent (cloud/local model + phone tools)
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

## Works with any agent

Lociant exposes one HTTP surface (port `11434`) from its Rust backend:

- **MCP Streamable HTTP** — `http://HOST:11434/mcp` for tools. Works with Claude Desktop/Code, Codex, OpenCode and RikkaHub (phone clients).
- **OpenAI-compatible API** — `http://HOST:11434/v1` for direct model inference.
- **Control API** — `http://HOST:11434/api/v1` for sessions, settings and model management.

Tool families exposed to agents:

| Family | Examples |
|---|---|
| Runtime & model | `runtime_status`, `model_list`, `llm_chat` |
| Device | `device_status`, `clipboard_read/write`, `app_open` |
| Sensors | `sensor_status`, `sensor_read`, `sensor_start/stop` |
| Screen & UI | `ui_screen_state`, `ui_click_node`, `ui_tap`, `ui_swipe`, `ui_paste`, `ui_set_text` |
| Vision | `vision_status`, `camera_capture` |

Every tool carries a policy: exposure levels (`read` < `sensor` < `action`) and `remote_allowed` are enforced by the backend before anything touches your device. Configure a token before binding Lociant to an untrusted network — see the [agent integration docs](docs/agent-integration.md) for client configs and the full API.

## How it works

One Flutter UI + one Rust backend on every platform. Android keeps only the device layer the backend cannot reach directly; headless boards run the backend alone.

```text
Flutter UI (Android / Linux desktop)
        │ HTTP (OpenAI /v1, control /api/v1, MCP /mcp)
        ▼
Rust backend (axum) ── the single server core
  ├─ core    domain types + JSON contract
  ├─ store   SQLite: sessions, messages, settings, models
  ├─ tools   tool contract + registry (single policy owner)
  ├─ agent   multi-round agent loop (cloud + local backends)
  ├─ server  HTTP: control plane, OpenAI plane, MCP
  └─ rkllm   in-process RKLLM NPU inference (libloading)
        │ device IPC (localhost TCP, token-gated)
        ▼
Android device layer (Kotlin)
  ├─ foreground service spawns the Rust server subprocess
  ├─ phone tools (accessibility, sensors, window, camera)
  └─ local MNN inference + vision
```

Peer mesh: nodes discover each other via UDP broadcast (port `11435`) and share tools and models (`peer:<node>:<model>`) over the OpenAI plane. Details in [architecture](docs/architecture.md).

## Documentation

- [Setup guide (from zero)](docs/setup-guide.md) — install, configure and connect all platforms
- [Agent integration & HTTP API](docs/agent-integration.md) — MCP, OpenAI and control APIs
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
- [x] Android device layer over IPC, MNN local inference
- [x] Headless RK board deployment (aarch64, systemd) + RKLLM NPU inference
- [x] Peer mesh: LAN discovery, shared tools, model forwarding
- [ ] Desktop local inference (llama.cpp on x86_64)
- [ ] Desktop device adapter (filesystem / process / camera tools)

## License

[MIT](LICENSE)

---

Star us if Lociant is useful — it tells us this direction matters. ⭐
