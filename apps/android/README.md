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
- `:phone-tools` - Android capability tools, accessibility screen control, local model tools, and vision tools.
 - `:mcp` - MCP Streamable HTTP protocol adapter.

## Notes

 Long-running model inference is designed as a visible Android runtime: foreground notification first, Runtime Window overlay when the user allows it.
