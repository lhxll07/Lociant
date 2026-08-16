**English** | [简体中文](README.zh-CN.md)

<div align="center">

# Lociant

### Turn an old Android phone into a local edge device

*Run models and device capabilities where the hardware is.*

[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/lhxll07/Lociant.svg?style=social&label=Star)](https://github.com/lhxll07/Lociant)

<img src="docs/media/social-preview.png" alt="Lociant" width="720">

</div>

Lociant is a local edge runtime built around old Android phones. It runs local
GGUF models, exposes hardware-backed capabilities, reports runtime state, and
connects nearby Lociant nodes over an authenticated LAN. Linux desktops and
Rockchip boards can join the same system as control consoles, compute nodes, or
headless edge nodes.

Lociant is not a chat client and does not run a general-purpose Agent loop.
External clients may use MCP or the control API to orchestrate individual
capability calls, while execution and policy remain on the device that owns the
hardware.

## The core idea

- **Give old hardware a useful role.** A phone can become a local model host,
  a screen and accessibility node, a sensor and camera node, or a combination
  of all four.
- **Keep execution local.** GGUF inference and device operations stay on the
  owning device instead of passing through a hosted chatbot or cloud service.
- **Connect devices without centralizing them.** Phones, desktops, and boards
  can share model inventory and selected tools across a trusted LAN.
- **Make the boundary explicit.** Tool exposure levels and remote-call policy
  are checked by the runtime before a capability runs.

## What you can do

| Capability | Examples |
|---|---|
| Local models | Import GGUF models and run them with llama.cpp on the device |
| Phone tools | Read the screen, tap and type through accessibility, open apps |
| Device sensing | Read sensors and capture camera frames when permissions allow |
| Local networking | Discover peers and expose selected models and tools over LAN |
| External access | Connect through MCP Streamable HTTP or the control API |

## Supported roles

| Device | Role | Runtime |
|---|---|---|
| Android phone | Primary device node and local model host | Flutter console + Rust backend + Kotlin device layer |
| Linux desktop | Control console and filesystem/process node | Flutter console + bundled Rust backend |
| Rockchip board | Low-power headless edge node | Rust service + RKLLM NPU runtime + terminal TUI |

All roles use the same control API and MCP surface. The board can run without a
graphical UI, and a desktop or phone can inspect and control another node over
the LAN.

## Get started

### Android phone (recommended)

1. Download and install the [Android APK (arm64-v8a)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk).
2. Open Lociant and complete the short setup flow. On Android 13+, allow
   notifications so the foreground runtime can start.
3. Grant only the capabilities you need under **Settings > Security**:
   accessibility, camera, sensors, floating window, and unrestricted
   background operation are optional and capability-specific.
4. Set an API token before exposing the runtime to a network you do not fully
   trust. The runtime starts with the app; its LAN address and copyable MCP/API
   endpoints are shown on the overview page.
5. Import a `.gguf` model from **Models** when you want local inference.

For Linux, headless boards, model formats, peer networking, and the complete
permission matrix, see the **[setup guide](docs/setup-guide.md)**.

Current v2.0.1 packages:

- [Android APK (arm64-v8a)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk)
- [Linux desktop (x86_64 tar.gz)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-x86_64.tar.gz)
- [Linux board (aarch64 tar.gz)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-aarch64.tar.gz)
- [Debian desktop package (amd64)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant_2.0.1_amd64.deb)
- [Debian headless node package (arm64)](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-node_2.0.1_arm64.deb)

## Connect to a node

When the runtime is running, the Rust backend exposes one local HTTP surface on
port `11434`:

- **MCP Streamable HTTP:** `http://HOST:11434/mcp`
- **Control API:** `http://HOST:11434/api/v1`
- **Health check:** `http://HOST:11434/health`

MCP is the integration surface for external clients. The control API is useful
for runtime state, settings, models, nodes, health checks, and direct tool
calls. Set `authToken` before binding to a shared or untrusted network; do not
expose port `11434` directly to the public Internet.

Tool exposure is cumulative:

- `read` for passive status and read-only tools;
- `sensor` for sensor and contextual capabilities;
- `action` for device operations such as screen input.

The runtime enforces both the configured exposure level and each tool's
`remoteAllowed` policy before an HTTP or MCP call reaches a device adapter.
See the **[edge runtime integration guide](docs/edge-runtime-integration.md)** for
MCP examples, API routes, and client configuration.

## Architecture

The Rust backend owns the shared control plane, model inventory, peer
networking, and tool policy. Android supplies capabilities that require the
Android framework; Linux supplies desktop capabilities; headless boards run
the backend directly.

```text
Flutter console (Android / Linux desktop)
        | HTTP: /api/v1, /mcp
        v
Rust backend (shared control plane)
  |- model inventory and local runtimes
  |- ToolRegistry and policy enforcement
  |- control API, MCP transport, and peer networking
  `- RKLLM NPU runtime on supported boards
        | token-gated localhost IPC on Android
        v
Android device layer (Kotlin)
  |- foreground service and lifecycle
  |- accessibility, sensors, window, and camera tools
  `- NCNN vision runtime
```

GGUF inference uses a local llama.cpp process. NCNN remains dedicated to
Android camera and vision operations; RKLLM is used by supported headless
Rockchip nodes.

## Documentation

- [Setup guide](docs/setup-guide.md) - install, configure, and connect every platform
- [Edge runtime integration](docs/edge-runtime-integration.md) - MCP, control API, and tool policy
- [Architecture](docs/architecture.md) - ownership boundaries and deployment
- [Android development](apps/android/README.md) - Android host, native runtime, and build flow

## Development

- Flutter UI: `apps/flutter`
- Rust backend: `apps/rust-backend`
- Android device layer: `apps/android`

Run the Linux desktop console from two terminals:

```bash
cd apps/rust-backend && cargo run
cd apps/flutter && flutter run -d linux
```

Build, install, and launch the Android app with:

```bash
bash scripts/dev-install.sh
```

The Android build includes the Rust backend and requires the Rust Android
target plus `cargo-ndk`. See [Android development](apps/android/README.md) for
the full toolchain and test commands.

## Scope

Lociant stays focused on local models, device capabilities, controlled
connections, and edge-node networking. Chat UI, a general-purpose Agent loop,
NAS/file-server features, and dedicated monitoring applications are outside
the core runtime.

## License

[MIT](LICENSE)
