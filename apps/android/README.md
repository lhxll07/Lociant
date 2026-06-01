# Lociant Android

This is the Android application for Lociant.

## Build

Windows:

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

macOS / Linux:

```bash
cd apps/android
./gradlew :app:assembleDebug
```

APK output:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

## Stack

- `:app` - WebView shell, Android entry points, foreground service, Runtime Window, and HTTP composition.
- `:core` - shared constants, chat/tool data types, and the protocol-neutral `ToolRegistry`.
- `:data` - Room sessions/events plus the local JSON key-value store.
- `:local-runtime` - MNN/NCNN runtime, model manager/market/installer, CameraX vision analysis, and native CMake code.
- `:phone-tools` - Android capability tools, accessibility screen control, notifications, storage tools, and vision tools.
- `:mcp` / `:acp` - protocol adapters for MCP Streamable HTTP and desktop ACP nodes.

## Notes

The app packages built-in scene sources from the repository root `scenes/` directory as Android assets. External models are expected under app storage and are managed by the in-app model manager.

Long-running model inference is designed as a visible Android runtime: foreground notification first, Runtime Window overlay when the user allows it.
