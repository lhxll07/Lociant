package io.lociant.android.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.util.zip.ZipFile

/**
 * Prepares an Android ARM64 `llama-server` binary and advertises it to the
 * Rust backend through environment variables.
 *
 * The Rust backend owns the child process once started; this class only makes
 * the executable available and selects the first GGUF model found in the
 * shared models directory. Build llama.cpp for Android with the NDK and place
 * the result at:
 *
 *   app/src/main/jniLibs/arm64-v8a/libllama_server.so
 *
 * (the Gradle packaging already treats it like the Rust server binary).
 */
object LlamaServerProcess {
    private const val TAG = "LociantLlamaServer"

    @Volatile private var cachedModel: File? = null

    fun configure(context: Context, builder: ProcessBuilder): Boolean {
        return runCatching {
            val executable = resolveExecutable(context) ?: return false
            executable.setExecutable(true, false)
            val model = findGgufModel(context) ?: return false
            cachedModel = model

            builder.environment()["LOCIANT_LLAMA_ENABLED"] = "true"
            builder.environment()["LOCIANT_LLAMA_SERVER_PATH"] = executable.absolutePath
            builder.environment()["LOCIANT_LLAMA_LIB_DIR"] =
                executable.parentFile?.absolutePath ?: executable.absolutePath
            builder.environment()["LOCIANT_LLAMA_MODEL_PATH"] = model.absolutePath
            builder.environment()["LOCIANT_LLAMA_MODEL_NAME"] =
                model.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9.\\-_]"), "-")
            builder.environment()["LOCIANT_LLAMA_CTX_SIZE"] = "4096"
            builder.environment()["LOCIANT_LLAMA_PREDICT"] = "512"
            builder.environment()["LOCIANT_LLAMA_THREADS"] = "4"
            Log.i(TAG, "llama-server enabled: ${executable.absolutePath} model=${model.name}")
            true
        }.getOrElse { error ->
            Log.w(TAG, "llama-server configure failed", error)
            false
        }
    }

    fun modelFile(context: Context): File? = cachedModel ?: findGgufModel(context)

    private fun resolveExecutable(context: Context): File? {
        val inNativeLibDir = File(context.applicationInfo.nativeLibraryDir, "libllama_server.so")
        if (inNativeLibDir.exists()) return inNativeLibDir
        val binDir = File(context.filesDir, "lociant/llama").apply { mkdirs() }
        val extracted = File(binDir, "llama-server")
        return if (extractBundledBinary(context, extracted)) extracted else null
    }

    private fun extractBundledBinary(context: Context, target: File): Boolean {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return false
        val entryName = "lib/$abi/libllama_server.so"
        return runCatching {
            ZipFile(context.applicationInfo.sourceDir).use { zip ->
                val entry = zip.getEntry(entryName) ?: return false
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            true
        }.getOrElse { error ->
            Log.w(TAG, "extract bundled llama-server failed", error)
            false
        }
    }

    private fun findGgufModel(context: Context): File? {
        val roots = listOfNotNull(
            File(context.filesDir, "models"),
            context.getExternalFilesDir(null)?.let { File(it, "models") },
        )

        fun search(dir: File, depth: Int): File? {
            if (depth > 4) return null
            val files = dir.listFiles() ?: return null
            for (file in files) {
                if (file.isFile && file.extension.equals("gguf", ignoreCase = true)) {
                    return file
                }
                if (file.isDirectory) {
                    search(file, depth + 1)?.let { return it }
                }
            }
            return null
        }

        for (root in roots) {
            search(root, 0)?.let { return it }
        }
        return null
    }
}
