# MNNode Android

This is the Android application for MNNode.

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

- Kotlin Android app
- WebView shell and JavaScript bridge
- CameraX preview and analysis
- NCNN and MNN through JNI/CMake
- Ktor embedded server
- Room persistence
- Foreground runtime service and Runtime Window overlay

## Notes

The app packages built-in scene sources from the repository root `scenes/` directory as Android assets. External models are expected under app storage and are managed by the in-app model manager.

Long-running model inference is designed as a visible Android runtime: foreground notification first, Runtime Window overlay when the user allows it.
