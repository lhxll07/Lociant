package io.lociant.android.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * Spawns the bundled Rust server (`liblociant_server.so`) as a subprocess.
 *
 * The binary ships in jniLibs; Android forbids executing from the native
 * library directory directly, so it is copied to private storage first. Data
 * lives under `filesDir/lociant/rust-data`. It listens on port 11434 for both
 * the local Flutter UI and authenticated peers on the LAN.
 */
object RustServerProcess {
    private const val TAG = "LociantRustServer"
    private const val PORT = 11434

    private val lock = Any()
    @Volatile private var process: Process? = null
    @Volatile private var intentionalStop = false

    fun isRunning(): Boolean = process?.isAlive == true

    fun start(
        context: Context,
        deviceToken: String? = null,
        devicePort: Int = DeviceAdapterServer.DEVICE_PORT,
        onExit: ((Int) -> Unit)? = null,
    ): Boolean {
        return synchronized(lock) {
            if (process?.isAlive == true) return true
            intentionalStop = false
            try {
                // 优先用 nativeLibraryDir 里的库（legacy packaging 会落地到那里，
                // 文件上下文是 apk_data_file，untrusted_app 域允许执行）。
                val binary = resolveExecutable(context) ?: run {
                    Log.w(TAG, "bundled rust server missing or unreadable in APK")
                    return false
                }
                binary.setExecutable(true, false)

                val dataDir = File(context.filesDir, "lociant/rust-data").apply { mkdirs() }
                val builder = ProcessBuilder(binary.absolutePath)
                builder.environment()["LOCIANT_DATA_DIR"] = dataDir.absolutePath
                builder.environment()["LOCIANT_HOST"] = "0.0.0.0"
                builder.environment()["LOCIANT_PORT"] = PORT.toString()
                builder.environment()["LOCIANT_MODELS_DIR"] =
                    File(context.getExternalFilesDir(null), "models").absolutePath
                if (deviceToken != null) {
                    builder.environment()[DeviceAdapterServer.TOKEN_ENV] = deviceToken
                    builder.environment()[DeviceAdapterServer.PORT_ENV] = devicePort.toString()
                }
                LlamaServerProcess.configure(context, builder)
                builder.redirectErrorStream(true)
                val proc = builder.start()
                process = proc
                Thread {
                    proc.inputStream.bufferedReader().forEachLine { line -> Log.i(TAG, line) }
                }.apply {
                    name = "lociant-rust-log"
                    isDaemon = true
                    start()
                }
                Thread {
                    val exitCode = runCatching { proc.waitFor() }.getOrDefault(-1)
                    val notify = synchronized(lock) {
                        if (process !== proc) {
                            false
                        } else {
                            process = null
                            !intentionalStop
                        }
                    }
                    if (notify) {
                        Log.w(TAG, "rust server exited code=$exitCode")
                        runCatching { onExit?.invoke(exitCode) }
                    }
                }.apply {
                    name = "lociant-rust-watchdog"
                    isDaemon = true
                    start()
                }
                Log.i(TAG, "started data=$dataDir port=$PORT device=${deviceToken != null}")
                true
            } catch (error: Throwable) {
                process = null
                Log.e(TAG, "start failed", error)
                false
            }
        }
    }

    fun stop() {
        val proc = synchronized(lock) {
            intentionalStop = true
            process.also { process = null }
        }
        proc ?: return
        runCatching { proc.destroy() }
        if (runCatching { proc.waitFor(2, TimeUnit.SECONDS) }.getOrDefault(false)) return
        runCatching { proc.destroyForcibly() }
    }

    private fun resolveExecutable(context: Context): File? {
        val inNativeLibDir = File(context.applicationInfo.nativeLibraryDir, "liblociant_server.so")
        if (inNativeLibDir.exists()) return inNativeLibDir
        val binDir = File(context.filesDir, "lociant/rust").apply { mkdirs() }
        val extracted = File(binDir, "lociant-server")
        return if (extractBundledBinary(context, extracted)) extracted else null
    }

    /**
     * AGP keeps native libs compressed inside the APK (extractNativeLibs=false),
     * so they are not present in nativeLibraryDir. Read the binary straight
     * from the APK zip instead.
     */
    private fun extractBundledBinary(context: Context, target: File): Boolean {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return false
        val entryName = "lib/$abi/liblociant_server.so"
        return runCatching {
            ZipFile(context.applicationInfo.sourceDir).use { zip ->
                val entry = zip.getEntry(entryName) ?: return false
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            true
        }.getOrElse { error ->
            Log.w(TAG, "extract bundled server failed", error)
            false
        }
    }
}
