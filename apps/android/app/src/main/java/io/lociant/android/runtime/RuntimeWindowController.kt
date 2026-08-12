package io.lociant.android.runtime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import io.lociant.core.config.RuntimeDefaults
import io.lociant.tools.runtime.VisionRuntime
import org.json.JSONObject
import java.util.concurrent.Executors

class RuntimeWindowController private constructor(
    private val context: Context,
) {
    private var view: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var collapsed = false
    private var error = ""
    private val eventExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lociant-companion-events").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val refreshTicker = object : Runnable {
        override fun run() {
            val current = view ?: return
            render(current)
            mainHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    fun show(): JSONObject {
        if (!isAllowed()) return setError("Companion window permission is not granted")
        view?.let {
            render(it)
            startRefreshLoop()
            return state()
        }

        val saved = savedState()
        collapsed = saved.optBoolean("collapsed", false)
        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = saved.optInt("x", 24)
            y = saved.optInt("y", 96)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp, 10.dp, 12.dp, 10.dp)
            background = panelBackground()
            elevation = 12f
            setOnTouchListener(DragListener(windowParams))
        }
        render(root)
        return runCatching {
            windowManager().addView(root, windowParams)
            view = root
            params = windowParams
            error = ""
            persist(windowParams)
            startRefreshLoop()
            state()
        }.getOrElse { setError(it.message ?: "Unable to show companion window") }
    }

    fun hide(): JSONObject {
        val current = view ?: return state()
        stopRefreshLoop()
        params?.let { persist(it) }
        return runCatching {
            windowManager().removeView(current)
            view = null
            params = null
            error = ""
            state()
        }.getOrElse { setError(it.message ?: "Unable to hide companion window") }
    }

    fun collapse(): JSONObject = setCollapsed(true)

    fun expand(): JSONObject = setCollapsed(false)

    fun refresh(): JSONObject {
        val current = view ?: return state()
        if (Looper.myLooper() == Looper.getMainLooper()) render(current)
        else mainHandler.post { view?.let { render(it) } }
        return state()
    }

    fun state(): JSONObject = JSONObject()
        .put("supported", true)
        .put("allowed", isAllowed())
        .put("visible", isVisible())
        .put("state", when {
            error.isNotBlank() -> "error"
            view == null -> "hidden"
            collapsed -> "collapsed"
            else -> "expanded"
        })
        .put("collapsed", collapsed)
        .put("error", error)

    private fun setCollapsed(value: Boolean): JSONObject {
        collapsed = value
        params?.let { persist(it) }
        view?.let { render(it) }
        return state()
    }

    private fun setError(message: String): JSONObject {
        error = message
        Log.w("RuntimeWindow", "window error: $message")
        view?.let { render(it) }
        return state()
    }

    private fun startRefreshLoop() {
        mainHandler.removeCallbacks(refreshTicker)
        mainHandler.postDelayed(refreshTicker, REFRESH_INTERVAL_MS)
    }

    private fun stopRefreshLoop() {
        mainHandler.removeCallbacks(refreshTicker)
    }

    private fun render(root: LinearLayout) {
        val runtime = LociantRuntime.runtimeSummary(context)
        root.removeAllViews()
        root.background = if (collapsed) pillBackground() else panelBackground()
        if (collapsed) {
            root.orientation = LinearLayout.HORIZONTAL
            root.gravity = Gravity.CENTER_VERTICAL
            root.setPadding(11.dp, 8.dp, 11.dp, 8.dp)
            root.addView(dotView(runtime))
            root.addView(label("Lociant", 13f, Color.WHITE, Typeface.BOLD).withStartMargin(8.dp))
            return
        }

        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.NO_GRAVITY
        root.setPadding(12.dp, 10.dp, 12.dp, 10.dp)
        root.addView(row().apply {
            addView(dotView(runtime))
            addView(label(statusLabel(runtime), 14f, Color.WHITE, Typeface.BOLD).withStartMargin(8.dp, weight = 1f))
            addView(action(if (runtime.optBoolean("running") || runtime.optBoolean("starting")) "Stop" else "Start") { toggleRuntime() })
            addView(action("Min") { collapse() }.withStartMargin(6.dp))
        })
        root.addView(label(runtime.optString("lanUrl", "").ifBlank { "LAN API unavailable" }, 11f, 0xff8fb8ff.toInt(), Typeface.NORMAL).withTopMargin(6.dp))
        val vision = VisionRuntime.status()
        root.addView(row().withTopMargin(7.dp).apply {
            addView(label(visionLabel(vision), 11f, visionColor(vision), Typeface.NORMAL).withEndMargin(8.dp, weight = 1f))
            addView(action(if (vision.optBoolean("running")) "Stop" else "Start") { toggleVision() })
        })
        val message = error.ifBlank { runtime.optString("lastError", "") }
        if (message.isNotBlank()) root.addView(label(message, 11f, 0xffff8a8a.toInt(), Typeface.NORMAL).withTopMargin(6.dp))
    }

    private fun statusLabel(runtime: JSONObject): String = when {
        runtime.optBoolean("starting") -> "Starting"
        runtime.optBoolean("running") -> "Runtime On"
        error.isNotBlank() || runtime.optString("lastError").isNotBlank() -> "Runtime Error"
        else -> "Runtime Off"
    }

    private fun visionLabel(vision: JSONObject): String {
        val state = vision.optString("state", "idle")
        val fps = vision.optDouble("fps", 0.0)
        val detections = vision.optJSONObject("lastDetection")
            ?.optJSONArray("detections")
            ?.length() ?: 0
        return when (state) {
            "running" -> "Vision · ${"%.1f".format(java.util.Locale.US, fps)} fps · $detections"
            "starting" -> "Vision starting"
            "error" -> "Vision error"
            "unavailable" -> "Vision unavailable"
            else -> "Vision Off"
        }
    }

    private fun visionColor(vision: JSONObject): Int = when (vision.optString("state", "idle")) {
        "running" -> 0xff7ce7a5.toInt()
        "starting" -> 0xffffd166.toInt()
        "error" -> 0xffff8a8a.toInt()
        "unavailable" -> 0xffa8a8a8.toInt()
        else -> 0xffd6d6d6.toInt()
    }

    private fun toggleRuntime() {
        val runtime = LociantRuntime.runtimeSummary(context)
        if (runtime.optBoolean("running") || runtime.optBoolean("starting")) {
            LociantRuntimeService.stopRuntime(context)
        } else {
            LociantRuntimeService.startRuntime(context)
        }
        refresh()
    }

    private fun toggleVision() {
        val tool = if (VisionRuntime.status().optBoolean("running")) "vision_stop" else "vision_start"
        LociantRuntime.server(context).callToolResult(tool)
        refresh()
    }

    private fun savedState(): JSONObject =
        LociantRuntime.localStore(context).getObject(SETTINGS_NAMESPACE, SETTINGS_KEY)

    private fun persist(windowParams: WindowManager.LayoutParams) {
        LociantRuntime.localStore(context).set(SETTINGS_NAMESPACE, SETTINGS_KEY, JSONObject()
            .put("x", windowParams.x)
            .put("y", windowParams.y)
            .put("collapsed", collapsed))
    }

    private fun isVisible() = view != null

    private fun isAllowed() = Settings.canDrawOverlays(context)

    private fun overlayType() = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun windowManager() = context.getSystemService(WindowManager::class.java)

    private fun row() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun label(text: String, sp: Float, color: Int, style: Int) = TextView(context).apply {
        this.text = text
        setTextColor(color)
        textSize = sp
        typeface = Typeface.create(Typeface.DEFAULT, style)
        includeFontPadding = false
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    private fun action(text: String, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        setPadding(9.dp, 6.dp, 9.dp, 6.dp)
        background = buttonBackground()
        setOnClickListener { onClick() }
    }

    private fun dotView(runtime: JSONObject) = View(context).apply {
        val running = runtime.optBoolean("running")
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (running) 0xff41d675.toInt() else 0xffffb84d.toInt())
        }
        layoutParams = LinearLayout.LayoutParams(9.dp, 9.dp)
    }

    private fun panelBackground() = GradientDrawable().apply {
        cornerRadius = 16.dp.toFloat()
        setColor(0xee151515.toInt())
        setStroke(1.dp, 0x335c9dff)
    }

    private fun pillBackground() = GradientDrawable().apply {
        cornerRadius = 999f
        setColor(0xee151515.toInt())
        setStroke(1.dp, 0x335c9dff)
    }

    private fun buttonBackground() = GradientDrawable().apply {
        cornerRadius = 10.dp.toFloat()
        setColor(0xff2a2a2a.toInt())
        setStroke(1.dp, 0x224f9cff)
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun <T : View> T.withStartMargin(value: Int, weight: Float = 0f): T = apply {
        layoutParams = LinearLayout.LayoutParams(
            if (weight > 0f) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight,
        ).apply { marginStart = value }
    }

    private fun <T : View> T.withEndMargin(value: Int, weight: Float = 0f): T = apply {
        layoutParams = LinearLayout.LayoutParams(
            if (weight > 0f) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight,
        ).apply { marginEnd = value }
    }

    private fun <T : View> T.withTopMargin(value: Int): T = apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = value }
    }

    private inner class DragListener(
        private val windowParams: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var downAt = 0L
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = windowParams.x
                    startY = windowParams.y
                    downAt = System.currentTimeMillis()
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (kotlin.math.abs(dx) < 4.dp && kotlin.math.abs(dy) < 4.dp) return true
                    moved = true
                    windowParams.x = startX + dx.toInt()
                    windowParams.y = startY + dy.toInt()
                    runCatching { windowManager().updateViewLayout(view, windowParams) }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        persist(windowParams)
                        return true
                    }
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        if (System.currentTimeMillis() - downAt > LONG_PRESS_MS) hide()
                        else if (collapsed) expand()
                    }
                    return true
                }
            }
            return false
        }
    }

    companion object {
        private const val SETTINGS_NAMESPACE = RuntimeDefaults.Settings.WINDOW_NAMESPACE
        private const val SETTINGS_KEY = RuntimeDefaults.Settings.FLOATING_WINDOW_KEY
        private const val LONG_PRESS_MS = 550L
        private const val REFRESH_INTERVAL_MS = 1000L
        @Volatile private var instance: RuntimeWindowController? = null

        fun get(context: Context): RuntimeWindowController {
            val current = instance
            if (current != null) return current
            synchronized(this) {
                val existing = instance
                if (existing != null) return existing
                return RuntimeWindowController(context.applicationContext).also { instance = it }
            }
        }
    }
}
