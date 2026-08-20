# llama.cpp Local Runtime

Lociant can supervise a local `llama-server` process on Linux or a headless
node. The process is treated as a local runtime resource, not as a cloud
provider and not as a replacement for the Lociant control API.

## Managed Process

Configure `LOCIANT_CONFIG` or the settings API:

```json
{
  "llamaEnabled": true,
  "llamaModelPath": "/opt/models/model.gguf",
  "llamaModelName": "local-llama",
  "llamaCtxSize": 4096,
  "llamaPredict": 512,
  "llamaThreads": 8,
  "llamaServerPath": "llama-server",
  "llamaPort": 0
}
```

`llamaPort=0` chooses a free loopback port. Lociant waits for the child
process health endpoint and terminates the child when the runtime stops. After
startup, a small supervisor checks both the child process and its health
endpoint; an unexpected exit or unhealthy process is restarted with bounded
exponential backoff. The model appears in `GET /api/v1/models` with
`runtime: llama` and is marked unavailable while it recovers.

The underlying `llama-server` remains private to the local runtime. External
clients should use Lociant's control API and MCP surfaces rather than relying
on an OpenAI-compatible `/v1` route from Lociant.

## Manual Process

To run llama.cpp independently:

```bash
bash scripts/run-llama-server.sh /opt/models/model.gguf 11502 local-llama 8
```

The script prints a local `llama*` settings fragment. Merge it into the
Lociant settings if the process should be registered in the runtime inventory.

## Android

Android uses the same llama.cpp subprocess lifecycle when an ARM64
`llama-server` bundle is present. Import a GGUF model from the Models page; the
Rust server receives the model path from the Android host and keeps the
llama.cpp process private to the device runtime.

The current scaffold is:

- `LlamaServerProcess.kt` prepares `LOCIANT_LLAMA_*` environment variables;
- `RustServerProcess` starts and watches the Rust backend subprocess;
- `LociantRuntimeService` keeps the Android device adapter alive across an
  unexpected Rust exit and restores the process with bounded backoff;
- Gradle can copy an optional binary from
  `tools/llama-android/arm64-v8a/llama-server`.

The optional binary is a packaging input, not a source-controlled model or
runtime dependency. Without it, Android can still use the NCNN vision runtime,
but local GGUF inference will remain unavailable until the bundle is supplied.
