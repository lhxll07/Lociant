package io.lociant.runtime.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.lociant.core.model.*
import io.lociant.core.config.RuntimeDefaults
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

class MnnRuntime(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    @Volatile private var handle: Long = if (nativeAvailable) runCatching { nativeCreate() }.getOrDefault(0L) else 0L
    private var loadedConfigPath: String? = null
    @Volatile private var cpuThreads = DEFAULT_CPU_THREADS
    @Volatile private var inferenceBackend = DEFAULT_INFERENCE_BACKEND

    /** Changes whenever a native prompt cache must no longer be reused. */
    fun cacheConfigurationKey(): String = "$cpuThreads:$inferenceBackend"

    @Synchronized
    fun configureCpuThreads(value: Int): Boolean {
        val next = value.coerceIn(MIN_CPU_THREADS, MAX_CPU_THREADS)
        if (cpuThreads == next) return false
        cancel()
        cpuThreads = next
        releaseHandle()
        return true
    }

    fun configureBackend(value: String): Boolean {
        val next = normalizeBackend(value)
        if (inferenceBackend == next) return false
        cancel()
        inferenceBackend = next
        releaseHandle()
        return true
    }

    @Synchronized fun preload(modelDir: File): NativeChatResult {
        val (ok, message) = prepareModel(modelDir)
        return NativeChatResult(ok = ok, message = message, modelInstalled = true)
    }

    @Synchronized
    fun chatText(modelDir: File, messages: List<NativeChatMessage>, toolsJson: String = "", maxTokens: Int = 128,
                 sessionId: String = "", useSessionCache: Boolean = false): NativeChatResult {
        val (ready, errorMsg) = prepareModel(modelDir)
        if (!ready) return NativeChatResult(ok = false, message = errorMsg, modelInstalled = true)
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.content }.toTypedArray()
        val raw = nativeChatText(handle, roles, contents, clampMaxTokens(maxTokens),
            sessionId, useSessionCache, runtimeConfigJson(toolsJson))
        return parseResult(raw, modelInstalled = true)
    }

    @Synchronized
    fun chatTextStream(modelDir: File, messages: List<NativeChatMessage>, toolsJson: String = "", maxTokens: Int = 128,
                       sessionId: String = "", useSessionCache: Boolean = false,
                       onChunk: (text: String, done: Boolean) -> Unit): NativeChatResult {
        val (ready, errorMsg) = prepareModel(modelDir)
        if (!ready) return NativeChatResult(ok = false, message = errorMsg, modelInstalled = true)
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.content }.toTypedArray()
        val emit = onChunk
        val raw = nativeChatTextStream(handle, roles, contents,
            clampMaxTokens(maxTokens),
            sessionId, useSessionCache, runtimeConfigJson(toolsJson),
            object : StreamCallback { override fun onChunk(text: String, done: Boolean) = emit(text, done) })
        return parseResult(raw, modelInstalled = true)
    }

    @Synchronized
    fun chatImage(modelDir: File, imageBytes: ByteArray, prompt: String, maxTokens: Int = 128): NativeChatResult {
        val (ready, errorMsg) = prepareModel(modelDir)
        if (!ready) return NativeChatResult(ok = false, message = errorMsg, modelInstalled = true)
        val bitmap = decodeChatBitmap(imageBytes) ?: return NativeChatResult(ok = false, message = "Cannot decode chat image")
        return try {
            val raw = nativeChatImage(handle, bitmap, prompt, clampMaxTokens(maxTokens), runtimeConfigJson())
            parseResult(raw, modelInstalled = true)
        } finally { bitmap.recycle() }
    }

    @Synchronized
    fun chatImageStream(modelDir: File, imageBytes: ByteArray, prompt: String, maxTokens: Int = 128,
                        onChunk: (text: String, done: Boolean) -> Unit): NativeChatResult {
        val (ready, errorMsg) = prepareModel(modelDir)
        if (!ready) return NativeChatResult(ok = false, message = errorMsg, modelInstalled = true)
        val bitmap = decodeChatBitmap(imageBytes) ?: return NativeChatResult(ok = false, message = "Cannot decode chat image")
        return try {
            val emit = onChunk
            val raw = nativeChatImageStream(handle, bitmap, prompt,
                clampMaxTokens(maxTokens), runtimeConfigJson(),
                object : StreamCallback { override fun onChunk(text: String, done: Boolean) = emit(text, done) })
            parseResult(raw, modelInstalled = true)
        } finally { bitmap.recycle() }
    }

    fun cancel() {
        val currentHandle = handle
        if (currentHandle != 0L) nativeCancel(currentHandle)
    }

    fun resetSessionCache() {
        val currentHandle = handle
        if (currentHandle != 0L) nativeResetSessionCache(currentHandle)
    }

    @Synchronized
    override fun close() { releaseHandle() }

    // ---- private ----

    private fun parseResult(raw: String, modelInstalled: Boolean): NativeChatResult {
        val json = JSONObject(raw)
        val tokens = json.optJSONObject("tokens")
        val cache = json.optJSONObject("cache")
        val promptTokens = tokens?.optInt("prompt", 0) ?: 0
        val cacheHit = cache?.optBoolean("hit", false) ?: false
        return NativeChatResult(
            ok = json.optBoolean("ok", false),
            text = json.optString("text", ""),
            message = json.optString("message", ""),
            modelInstalled = modelInstalled,
            promptTokens = promptTokens,
            generatedTokens = tokens?.optInt("generated", 0) ?: 0,
            cachedTokens = tokens?.optInt("cached", 0) ?: 0,
            cacheEnabled = cache?.optBoolean("enabled", false) ?: false,
            cacheHit = cacheHit,
            cancelled = json.optBoolean("cancelled", false),
            firstTokenMs = tokens?.optDouble("firstTokenMs", 0.0)?.toLong() ?: 0L,
            prefillUs = json.optLong("prefillUs", 0),
            decodeUs = json.optLong("decodeUs", 0),
        )
    }

    private fun clampMaxTokens(value: Int): Int = value.coerceIn(MIN_OUTPUT_TOKENS, HARD_MAX_OUTPUT_TOKENS)

    private fun prepareModel(modelDir: File): Pair<Boolean, String> {
        if (!nativeAvailable) return false to "MNN native runtime unavailable"
        if (handle == 0L) {
            handle = nativeCreate()
            if (handle == 0L) return false to "MNN native runtime unavailable"
        }
        val configFile = File(modelDir, "config.json")
        if (!configFile.isFile) return false to "MNN model config not found: ${configFile.absolutePath}"

        val configPath = effectiveConfigFile(modelDir, configFile).absolutePath
        if (handle != 0L && loadedConfigPath == configPath) return true to "already loaded"

        val raw = nativeLoad(handle, configPath,
            File(appContext.cacheDir, RuntimeDefaults.NativeRuntime.CHAT_CACHE_DIR).apply { mkdirs() }.absolutePath,
            runtimeConfigJson())
        val loaded = JSONObject(raw).optBoolean("ok", false)
        if (loaded) loadedConfigPath = configPath
        return loaded to JSONObject(raw).optString("message", "")
    }

    private fun runtimeConfigJson(toolsJson: String = ""): String {
        val context = JSONObject().put("enable_thinking", RuntimeDefaults.NativeRuntime.THINKING_ENABLED)
        runCatching {
            if (toolsJson.isNotBlank()) context.put("tools", org.json.JSONArray(toolsJson))
        }
        return JSONObject()
            .put("async", false)
            .put("prompt_cache", RuntimeDefaults.NativeRuntime.PROMPT_CACHE_ENABLED)
            .put("thread_num", cpuThreads)
            .put("min_output_tokens", MIN_OUTPUT_TOKENS)
            .put("max_output_tokens", HARD_MAX_OUTPUT_TOKENS)
            .put("mllm", JSONObject().put("thread_num", cpuThreads))
            .put("jinja", JSONObject()
                .put("context", context))
            .toString()
    }

    private fun effectiveConfigFile(modelDir: File, source: File): File {
        val config = JSONObject(source.readText(Charsets.UTF_8))
        val mllm = config.optJSONObject("mllm") ?: JSONObject().also { config.put("mllm", it) }
        config
            .put("base_dir", modelDir.absolutePath.trimEnd('/', File.separatorChar) + File.separator)
            .put("thread_num", cpuThreads)
        applyVisualConfig(modelDir, config)
        mllm.put("thread_num", cpuThreads)
        if (inferenceBackend != DEFAULT_INFERENCE_BACKEND) {
            config.put("backend_type", inferenceBackend)
            mllm.put("backend_type", inferenceBackend)
        }
        val jinja = config.optJSONObject("jinja") ?: JSONObject().also { config.put("jinja", it) }
        val context = jinja.optJSONObject("context") ?: JSONObject().also { jinja.put("context", it) }
        context.put("enable_thinking", RuntimeDefaults.NativeRuntime.THINKING_ENABLED)

        val name = "config-${sha1(modelDir.absolutePath)}-$cpuThreads.json"
        val target = File(File(appContext.cacheDir, RuntimeDefaults.NativeRuntime.CONFIG_CACHE_DIR).apply { mkdirs() }, name)
        val text = config.toString()
        if (!target.isFile || target.readText(Charsets.UTF_8) != text) {
            target.writeText(text, Charsets.UTF_8)
        }
        Log.i(TAG, "effective MNN config model=${modelDir.name} cpuThreads=$cpuThreads path=${target.absolutePath}")
        return target
    }

    private fun applyVisualConfig(modelDir: File, config: JSONObject) {
        val visual = config.optString("visual_model").trim().replace('\\', '/').trimStart('/')
            .ifBlank { "visual.mnn" }
        val visualFile = File(modelDir, visual)
        val fallbackVisualFile = File(modelDir, "visual.mnn")
        val selected = when {
            visualFile.isFile -> visual
            fallbackVisualFile.isFile -> "visual.mnn"
            else -> ""
        }
        if (selected.isBlank()) return
        config.put("visual_model", selected)
        config.put("is_visual", true)
    }

    private fun sha1(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(12)
    }

    private fun releaseHandle() {
        val currentHandle = handle
        if (currentHandle != 0L) nativeRelease(currentHandle)
        handle = 0L
        loadedConfigPath = null
    }

    private fun decodeChatBitmap(bytes: ByteArray, maxSide: Int = 512): Bitmap? {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val max = maxOf(source.width, source.height)
        if (max <= maxSide && source.config == Bitmap.Config.ARGB_8888) return source
        val scale = maxSide.toFloat() / max
        val scaled = Bitmap.createScaledBitmap(source,
            (source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1), true)
        source.recycle()
        return if (scaled.config == Bitmap.Config.ARGB_8888) scaled
        else scaled.copy(Bitmap.Config.ARGB_8888, false).also { scaled.recycle() }
    }

    companion object {
        @JvmStatic private external fun nativeCreate(): Long
        @JvmStatic private external fun nativeRelease(handle: Long)
        @JvmStatic private external fun nativeLoad(handle: Long, configPath: String, tmpPath: String, configJson: String): String
        @JvmStatic private external fun nativeChatText(handle: Long, roles: Array<String>, contents: Array<String>, maxTokens: Int, sessionId: String, useSessionCache: Boolean, configJson: String): String
        @JvmStatic private external fun nativeChatTextStream(handle: Long, roles: Array<String>, contents: Array<String>, maxTokens: Int, sessionId: String, useSessionCache: Boolean, configJson: String, callback: StreamCallback): String
        @JvmStatic private external fun nativeChatImage(handle: Long, bitmap: Bitmap, prompt: String, maxTokens: Int, configJson: String): String
        @JvmStatic private external fun nativeChatImageStream(handle: Long, bitmap: Bitmap, prompt: String, maxTokens: Int, configJson: String, callback: StreamCallback): String
        @JvmStatic private external fun nativeState(handle: Long): String
        @JvmStatic private external fun nativeCancel(handle: Long)
        @JvmStatic private external fun nativeResetSessionCache(handle: Long)

        const val DEFAULT_CPU_THREADS = 4
        const val MIN_CPU_THREADS = 1
        const val MAX_CPU_THREADS = 16

        const val DEFAULT_INFERENCE_BACKEND = "model"
        val INFERENCE_BACKENDS = setOf("model", "auto", "cpu", "opencl", "vulkan")
        val RISKY_INFERENCE_BACKENDS = setOf("auto", "opencl", "vulkan")

        fun normalizeBackend(value: String): String =
            value.trim().lowercase().takeIf { it in INFERENCE_BACKENDS } ?: DEFAULT_INFERENCE_BACKEND

        fun isRiskyBackend(value: String): Boolean = normalizeBackend(value) in RISKY_INFERENCE_BACKENDS
        private const val TAG = "LociantMnnRuntime"

private val nativeAvailable: Boolean = runCatching {
    System.loadLibrary("c++_shared")
    System.loadLibrary("MNN")
    System.loadLibrary("MNN_Express")
    System.loadLibrary("llm")
    System.loadLibrary("lociant_mnn")
    true
}.getOrDefault(false)
    }

    private interface StreamCallback { fun onChunk(text: String, done: Boolean) }
}
