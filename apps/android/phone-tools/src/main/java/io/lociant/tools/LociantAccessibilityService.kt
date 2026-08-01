package io.lociant.tools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.lociant.core.util.jsonError
import io.lociant.core.util.jsonOk
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

class LociantAccessibilityService : AccessibilityService() {
    private val tag = "LociantAccessibility"
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val nodeCache = linkedMapOf<String, CachedNode>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "Accessibility service connected")
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        lastEventType = event.eventType
        lastEventTimeMs = System.currentTimeMillis()
    }

    override fun onInterrupt() {
        Log.i(tag, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        Log.i(tag, "Accessibility service destroyed")
        super.onDestroy()
    }

    fun readScreenState(
        maxDepth: Int = DEFAULT_ACTION_DEPTH,
        maxNodes: Int = DEFAULT_STATE_NODES,
        query: String = "",
        exact: Boolean = false,
        includeScreenshot: Boolean = false,
        screenshotMaxWidth: Int = DEFAULT_SCREENSHOT_MAX_WIDTH,
        screenshotQuality: Int = DEFAULT_SCREENSHOT_QUALITY,
    ): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        val nodes = JSONArray()
        val lines = mutableListOf<String>()
        val cache = linkedMapOf<String, CachedNode>()
        val budget = TraversalBudget(maxNodes = MAX_SNAPSHOT_NODES)
        val filter = query.trim()
        val capturedAt = System.currentTimeMillis()
        return try {
            collectStateNodes(
                node = root,
                depth = 0,
                maxDepth = maxDepth.coerceIn(1, MAX_TREE_DEPTH),
                maxNodes = maxNodes.coerceIn(1, MAX_STATE_NODES),
                query = filter,
                exact = exact,
                nodes = nodes,
                lines = lines,
                cache = cache,
                budget = budget,
            )
            replaceNodeCache(cache)
            val result = JSONObject()
                .put("ok", true)
                .put("capturedAt", capturedAt)
                .put("lastEvent", lastEventJson())
                .put("nodeCount", budget.visited)
                .put("truncated", budget.truncated)
                .put("lines", JSONArray(lines))
                .put("text", lines.joinToString("\n"))
                .put("nodes", nodes)
                .put("count", nodes.length())
                .put("query", filter)
                .put("exact", exact)
                .put("cacheSize", cache.size)
                .put("message", "Use nodeId with ui_click_node for semantic actions; use ui_gesture only as a coordinate fallback.")
            if (includeScreenshot) {
                result.put("screenshot", takeScreenShot(screenshotMaxWidth, screenshotQuality))
            }
            result
        } finally {
            root.recycle()
        }
    }

    fun clickNode(nodeId: String, longClick: Boolean = false): JSONObject {
        require(nodeId.isNotBlank()) { "nodeId is required" }
        val cached = synchronized(nodeCache) { nodeCache[nodeId] }
            ?: return JSONObject()
                .put("ok", false)
                .put("code", "node_not_found")
                .put("nodeId", nodeId)
                .put("message", "Node is not in the latest screen_context cache. Call screen_context again.")
        if (longClick) {
            return gestureLongClick(cached.bounds.centerX(), cached.bounds.centerY())
                .put("nodeId", nodeId)
                .put("target", cached.toJson())
        }
        val root = rootInActiveWindow
        if (root != null) {
            try {
                findNode(root, cached.path)?.let { node ->
                    try {
                        val action = performClickAction(node, "click_node")
                        if (action.optBoolean("ok")) {
                            return action
                                .put("nodeId", nodeId)
                                .put("target", cached.toJson())
                        }
                    } finally {
                        if (node !== root) node.recycle()
                    }
                }
            } finally {
                root.recycle()
            }
        }
        return gestureClick(cached.bounds.centerX(), cached.bounds.centerY())
            .put("nodeId", nodeId)
            .put("target", cached.toJson())
            .put("method", "gesture_fallback")
    }

    fun setNodeText(nodeId: String, text: String, submit: Boolean = false): JSONObject {
        require(nodeId.isNotBlank()) { "nodeId is required" }
        val cached = synchronized(nodeCache) { nodeCache[nodeId] }
            ?: return JSONObject()
                .put("ok", false)
                .put("code", "node_not_found")
                .put("nodeId", nodeId)
                .put("message", "Node is not in the latest screen_context cache. Call screen_context again.")
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.").put("nodeId", nodeId)
        try {
            val node = findNode(root, cached.path)
                ?: return JSONObject()
                    .put("ok", false)
                    .put("code", "node_not_found")
                    .put("nodeId", nodeId)
                    .put("message", "Node is no longer present. Call screen_context again.")
            try {
                val target = editableTarget(node)
                    ?: return JSONObject()
                        .put("ok", false)
                        .put("code", "node_not_editable")
                        .put("nodeId", nodeId)
                        .put("target", cached.toJson())
                        .put("message", "Target node is not editable.")
                try {
                    val focused = performNodeActionOnMain(target, AccessibilityNodeInfo.ACTION_FOCUS)
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                    }
                    val success = performNodeActionOnMain(target, AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    return JSONObject()
                        .put("ok", success)
                        .put("action", "ui_set_text")
                        .put("method", "accessibility_set_text")
                        .put("nodeId", nodeId)
                        .put("focused", focused)
                        .put("submitRequested", submit)
                        .put("submitted", false)
                        .put("length", text.length)
                        .put("target", cached.toJson())
                        .put("message", if (success) "Text set on editable node." else "Android rejected ACTION_SET_TEXT.")
                } finally {
                    if (target !== node) target.recycle()
                }
            } finally {
                node.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    fun pasteIntoFocusedText(textLength: Int = 0): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        try {
            val focused = findPasteTarget(root)
                ?: return JSONObject()
                    .put("ok", false)
                    .put("code", "focused_input_not_found")
                    .put("message", "No focused input field is available for paste.")
            try {
                val target = editableTarget(focused)
                    ?: return JSONObject()
                        .put("ok", false)
                        .put("code", "focused_node_not_editable")
                        .put("message", "Focused node is not editable.")
                try {
                    val pasted = performNodeActionOnMain(target, AccessibilityNodeInfo.ACTION_PASTE)
                    return JSONObject()
                        .put("ok", pasted)
                        .put("action", "ui_paste_text")
                        .put("method", "accessibility_paste")
                        .put("length", textLength)
                        .put("message", if (pasted) "Clipboard text pasted into focused input." else "Android rejected ACTION_PASTE.")
                } finally {
                    if (target !== focused) target.recycle()
                }
            } finally {
                focused.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    private fun findPasteTarget(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocus != null) {
            try {
                if (editableTarget(inputFocus) != null) return inputFocus
            } finally {
                inputFocus.recycle()
            }
        }
        val accessibilityFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (accessibilityFocus != null) {
            try {
                if (editableTarget(accessibilityFocus) != null) return accessibilityFocus
            } finally {
                accessibilityFocus.recycle()
            }
        }
        return findFocusedEditableNode(root)
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.isEnabled && current.isVisibleToUser && current.isEditable && current.isFocused) {
                return current
            }
            var index = 0
            while (true) {
                val child = current.getChild(index) ?: break
                queue.add(child)
                index += 1
            }
            current.recycle()
        }
        return null
    }

    fun waitForUi(
        text: String = "",
        exact: Boolean = false,
        timeoutMs: Long = DEFAULT_WAIT_TIMEOUT_MS,
        idleMs: Long = DEFAULT_IDLE_MS,
        maxDepth: Int = DEFAULT_ACTION_DEPTH,
    ): JSONObject {
        val startedAt = SystemClock.uptimeMillis()
        val deadline = startedAt + timeoutMs.coerceIn(100L, MAX_WAIT_TIMEOUT_MS)
        val target = text.trim()
        while (SystemClock.uptimeMillis() <= deadline) {
            if (target.isNotBlank()) {
                val state = readScreenState(maxDepth = maxDepth, maxNodes = DEFAULT_STATE_NODES, query = target, exact = exact)
                if (state.optJSONArray("nodes")?.length() ?: 0 > 0) {
                    return jsonOk("mode" to "text", "text" to target, "elapsedMs" to SystemClock.uptimeMillis() - startedAt, "state" to state)
                }
            } else if (System.currentTimeMillis() - lastEventTimeMs >= idleMs) {
                return jsonOk("mode" to "idle", "idleMs" to idleMs, "elapsedMs" to SystemClock.uptimeMillis() - startedAt, "lastEvent" to lastEventJson())
            }
            SystemClock.sleep(WAIT_POLL_MS)
        }
        return jsonError("ui_wait_timeout", "UI wait timed out", "mode" to if (target.isBlank()) "idle" else "text", "text" to target, "timeoutMs" to timeoutMs, "elapsedMs" to SystemClock.uptimeMillis() - startedAt, "lastEvent" to lastEventJson())
    }

    fun takeScreenShot(
        maxWidth: Int = DEFAULT_SCREENSHOT_MAX_WIDTH,
        quality: Int = DEFAULT_SCREENSHOT_QUALITY,
    ): JSONObject {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return jsonError("screenshot_unavailable", "Screen capture requires Android 11 or later.", "minSdk" to Build.VERSION_CODES.R, "sdk" to Build.VERSION.SDK_INT, "lastEvent" to lastEventJson())
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return jsonError("screenshot_main_thread", "Screen capture cannot block the Android main thread.", "lastEvent" to lastEventJson())
        }

        val result = AtomicReference<JSONObject>()
        val latch = CountDownLatch(1)
        val callback = object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                result.set(encodeScreenShot(screenshot, maxWidth, quality))
                latch.countDown()
            }

            override fun onFailure(errorCode: Int) {
                result.set(jsonError("screenshot_failed", "Android rejected the screen capture request. Secure windows and system policy can block screenshots.", "errorCode" to errorCode))
                latch.countDown()
            }
        }

        val started = runOnMainBoolean(ACTION_TIMEOUT_MS) {
            runCatching {
                takeScreenshot(Display.DEFAULT_DISPLAY, screenshotExecutor, callback)
                true
            }.getOrDefault(false)
        }
        if (!started) {
            return jsonError("screenshot_start_failed", "Screen capture request could not be started.", "lastEvent" to lastEventJson())
        }
        if (!latch.await(SCREENSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            return jsonError("screenshot_timeout", "Screen capture timed out.", "timeoutMs" to SCREENSHOT_TIMEOUT_MS, "lastEvent" to lastEventJson())
        }
        return result.get() ?: jsonError("screenshot_empty", "Screen capture returned no result.", "lastEvent" to lastEventJson())
    }

    fun gestureClick(x: Int, y: Int): JSONObject =
        dispatchGesture("click", buildTap(x, y, TAP_DURATION_MS), TAP_DURATION_MS)
            .put("x", x)
            .put("y", y)

    fun gestureLongClick(x: Int, y: Int): JSONObject =
        dispatchGesture("long_click", buildTap(x, y, LONG_PRESS_DURATION_MS), LONG_PRESS_DURATION_MS)
            .put("x", x)
            .put("y", y)

    fun gestureSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = SWIPE_DURATION_MS): JSONObject =
        dispatchGesture("swipe", buildSwipe(x1, y1, x2, y2, durationMs), durationMs)
            .put("from", JSONObject().put("x", x1).put("y", y1))
            .put("to", JSONObject().put("x", x2).put("y", y2))

    fun pressBack(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_HOME)
    fun pressRecentApps(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_NOTIFICATIONS)

    fun openQuickSettings(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && performGlobalActionOnMain(GLOBAL_ACTION_QUICK_SETTINGS)

    private fun collectStateNodes(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int,
        maxNodes: Int,
        query: String,
        exact: Boolean,
        nodes: JSONArray,
        lines: MutableList<String>,
        cache: LinkedHashMap<String, CachedNode>,
        budget: TraversalBudget,
        path: String = "",
    ) {
        if (depth > maxDepth || !budget.enter()) return
        val label = labelOf(node)
        if (label.isNotBlank()) lines += label
        val visible = node.isVisibleToUser
        val bounds = boundsOf(node)
        val include = visible && !bounds.isEmpty && labelMatches(label, query, exact) && isUsefulStateNode(node, label)
        if (include && nodes.length() < maxNodes) {
            val nodeId = nodeIdFor(path)
            val cached = CachedNode(
                id = nodeId,
                path = path,
                label = label,
                text = node.text?.toString().orEmpty(),
                contentDescription = node.contentDescription?.toString().orEmpty(),
                className = node.className?.toString().orEmpty(),
                packageName = node.packageName?.toString().orEmpty(),
                viewId = node.viewIdResourceName.orEmpty(),
                bounds = Rect(bounds),
                clickable = node.isClickable,
                longClickable = node.isLongClickable,
                scrollable = node.isScrollable,
                editable = node.isEditable,
                checked = node.isChecked,
                selected = node.isSelected,
            )
            cache[nodeId] = cached
            nodes.put(cached.toJson().put("depth", depth))
        }
        if (depth >= maxDepth || nodes.length() >= maxNodes) return
        for (index in 0 until node.childCount) {
            if (!budget.canContinue()) return
            val child = node.getChild(index) ?: continue
            try {
                collectStateNodes(child, depth + 1, maxDepth, maxNodes, query, exact, nodes, lines, cache, budget, childPath(path, index))
            } finally {
                child.recycle()
            }
        }
    }

    private fun performClickAction(node: AccessibilityNodeInfo, action: String): JSONObject {
        var current: AccessibilityNodeInfo? = node
        var recycleCurrent = false
        try {
            while (current != null) {
                if (isClickableStateNode(current)) {
                    val bounds = boundsOf(current)
                    val success = performNodeActionOnMain(current, AccessibilityNodeInfo.ACTION_CLICK)
                    return jsonOk("action" to action, "method" to "accessibility_action", "tap" to boundsJson(bounds), "message" to if (success) "Clicked accessibility node." else "Accessibility click action failed.").apply { put("ok", success) }
                }
                val parent = current.parent
                if (recycleCurrent) current.recycle()
                current = parent
                recycleCurrent = true
            }
        } finally {
            if (recycleCurrent) current?.recycle()
        }
        return JSONObject().put("ok", false)
    }

    private fun editableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        var recycleCurrent = true
        while (current != null) {
            if (current.isEnabled && current.isVisibleToUser && current.isEditable) {
                return current
            }
            val parent = current.parent
            if (recycleCurrent) current.recycle()
            current = parent
            recycleCurrent = true
        }
        return null
    }

    private fun dispatchGesture(action: String, description: GestureDescription, durationMs: Long): JSONObject {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return gestureDispatchResult(
                action = action,
                ok = false,
                accepted = false,
                elapsedMs = 0,
                durationMs = durationMs,
                code = "gesture_unsupported",
                message = "Gesture dispatch requires Android 7 or later.",
            )
        }
        val startedAt = SystemClock.uptimeMillis()
        val result = AtomicReference<JSONObject>()
        val latch = CountDownLatch(1)
        val dispatchOnMain = {
            val accepted = runCatching {
                dispatchGesture(description, null, null)
            }.getOrDefault(false)
            val elapsedMs = SystemClock.uptimeMillis() - startedAt
            result.set(gestureDispatchResult(
                action = action,
                ok = accepted,
                accepted = accepted,
                elapsedMs = elapsedMs,
                durationMs = durationMs,
                code = if (accepted) "" else "gesture_rejected",
                message = if (accepted) "Gesture accepted by Android and will run asynchronously." else "Android rejected the gesture dispatch request.",
            ))
            latch.countDown()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) dispatchOnMain() else mainHandler.post(dispatchOnMain)
        if (!latch.await(GESTURE_DISPATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            return gestureDispatchResult(
                action = action,
                ok = false,
                accepted = false,
                elapsedMs = SystemClock.uptimeMillis() - startedAt,
                durationMs = durationMs,
                code = "gesture_dispatch_timeout",
                message = "Timed out before Android accepted the gesture. The request was not waited on for completion.",
            )
        }
        return result.get() ?: gestureDispatchResult(
            action = action,
            ok = false,
            accepted = false,
            elapsedMs = SystemClock.uptimeMillis() - startedAt,
            durationMs = durationMs,
            code = "gesture_dispatch_empty",
            message = "Android gesture dispatch returned no result.",
        )
    }

    private fun gestureDispatchResult(
        action: String,
        ok: Boolean,
        accepted: Boolean,
        elapsedMs: Long,
        durationMs: Long,
        code: String,
        message: String,
    ): JSONObject = JSONObject()
        .put("ok", ok)
        .put("action", action)
        .put("accepted", accepted)
        .put("dispatched", accepted)
        .put("completionKnown", false)
        .put("dispatchElapsedMs", elapsedMs)
        .put("durationMs", durationMs)
        .put("settleMs", (durationMs + GESTURE_SETTLE_PADDING_MS).coerceAtMost(MAX_GESTURE_SETTLE_MS))
        .put("message", message)
        .also {
            if (code.isNotBlank()) it.put("code", code)
        }

    private fun encodeScreenShot(screenshot: ScreenshotResult, maxWidth: Int, quality: Int): JSONObject {
        val hardwareBuffer = screenshot.hardwareBuffer
        var source: Bitmap? = null
        var image: Bitmap? = null
        return try {
            val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                ?: return jsonError("screenshot_decode_failed", "Android returned a screenshot buffer that could not be decoded.")
            source = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                ?: return jsonError("screenshot_copy_failed", "Android screenshot buffer could not be copied.")
            val requestedMaxWidth = maxWidth.coerceIn(MIN_SCREENSHOT_WIDTH, MAX_SCREENSHOT_WIDTH)
            image = if (source.width > requestedMaxWidth) {
                val scale = requestedMaxWidth.toFloat() / source.width.toFloat()
                Bitmap.createScaledBitmap(source, requestedMaxWidth, (source.height * scale).roundToInt().coerceAtLeast(1), true)
            } else {
                source
            }
            val bytes = ByteArrayOutputStream().use { stream ->
                image.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(MIN_SCREENSHOT_QUALITY, MAX_SCREENSHOT_QUALITY), stream)
                stream.toByteArray()
            }
            val data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            jsonOk("mimeType" to "image/jpeg", "width" to image.width, "height" to image.height, "sourceWidth" to source.width, "sourceHeight" to source.height, "quality" to quality.coerceIn(MIN_SCREENSHOT_QUALITY, MAX_SCREENSHOT_QUALITY), "bytes" to bytes.size, "image" to "data:image/jpeg;base64,$data", "capturedAt" to System.currentTimeMillis()).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.put("hardwareTimestamp", screenshot.timestamp)
                }
            }
        } catch (error: Throwable) {
            jsonError("screenshot_encode_failed", error.message ?: "Failed to encode Android screenshot.")
        } finally {
            image?.takeIf { it !== source }?.recycle()
            source?.recycle()
            hardwareBuffer.close()
        }
    }

    private fun performGlobalActionOnMain(action: Int): Boolean =
        runOnMainBoolean(ACTION_TIMEOUT_MS) { performGlobalAction(action) }

    private fun performNodeActionOnMain(node: AccessibilityNodeInfo, action: Int): Boolean =
        runOnMainBoolean(ACTION_TIMEOUT_MS) { node.performAction(action) }

    private fun performNodeActionOnMain(node: AccessibilityNodeInfo, action: Int, args: Bundle): Boolean =
        runOnMainBoolean(ACTION_TIMEOUT_MS) { node.performAction(action, args) }

    private fun runOnMainBoolean(timeoutMs: Long, block: () -> Boolean): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return runCatching { block() }.getOrDefault(false)
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)
        mainHandler.post {
            result.set(runCatching { block() }.getOrDefault(false))
            latch.countDown()
        }
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS) && result.get()
    }

    private fun buildTap(x: Int, y: Int, durationMs: Long): GestureDescription {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
    }

    private fun buildSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): GestureDescription {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
    }

    private fun labelMatches(label: String, query: String, exact: Boolean): Boolean {
        val target = query.trim()
        if (target.isBlank()) return true
        return if (exact) label.trim() == target else label.contains(target, ignoreCase = true)
    }

    private fun isUsefulStateNode(node: AccessibilityNodeInfo, label: String): Boolean =
        label.isNotBlank() || node.isClickable || node.isLongClickable || node.isScrollable || node.isEditable

    private fun isClickableStateNode(node: AccessibilityNodeInfo): Boolean =
        node.isEnabled && node.isVisibleToUser && node.isClickable

    private fun boundsOf(node: AccessibilityNodeInfo): Rect =
        Rect().also { node.getBoundsInScreen(it) }

    private fun boundsJson(rect: Rect): JSONObject = JSONObject()
        .put("left", rect.left)
        .put("top", rect.top)
        .put("right", rect.right)
        .put("bottom", rect.bottom)
        .put("centerX", rect.centerX())
        .put("centerY", rect.centerY())
        .put("width", rect.width())
        .put("height", rect.height())

    private fun labelOf(node: AccessibilityNodeInfo): String =
        listOf(node.text, node.contentDescription)
            .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
            .joinToString(" ")

    private fun replaceNodeCache(next: LinkedHashMap<String, CachedNode>) {
        synchronized(nodeCache) {
            nodeCache.clear()
            next.entries.take(MAX_CACHED_NODES).forEach { (id, node) -> nodeCache[id] = node }
        }
    }

    private fun findNode(root: AccessibilityNodeInfo, path: String): AccessibilityNodeInfo? {
        if (path.isBlank()) return AccessibilityNodeInfo.obtain(root)
        var current = AccessibilityNodeInfo.obtain(root)
        path.split('.').forEach { part ->
            val index = part.toIntOrNull() ?: run {
                current.recycle()
                return null
            }
            val child = current.getChild(index) ?: run {
                current.recycle()
                return null
            }
            current.recycle()
            current = child
        }
        return current
    }

    private fun childPath(parent: String, index: Int): String =
        if (parent.isBlank()) index.toString() else "$parent.$index"

    private fun nodeIdFor(path: String): String =
        "n" + path.ifBlank { "root" }.replace('.', '_')

    private fun unavailable(message: String): JSONObject = JSONObject()
        .put("ok", false)
        .put("code", "accessibility_unavailable")
        .put("message", message)
        .put("lastEvent", lastEventJson())

    private fun lastEventJson(): JSONObject = JSONObject()
        .put("type", lastEventType)
        .put("timeMs", lastEventTimeMs)

    private class TraversalBudget(
        private val maxNodes: Int = MAX_SNAPSHOT_NODES,
        private val timeoutMs: Long = SNAPSHOT_TIMEOUT_MS,
    ) {
        private val deadlineMs = SystemClock.uptimeMillis() + timeoutMs
        var visited: Int = 0
            private set
        var truncated: Boolean = false
            private set

        fun enter(): Boolean {
            if (truncated) return false
            if (visited >= maxNodes || SystemClock.uptimeMillis() > deadlineMs) {
                truncated = true
                return false
            }
            visited += 1
            return true
        }

        fun canContinue(): Boolean {
            if (truncated) return false
            if (visited >= maxNodes || SystemClock.uptimeMillis() > deadlineMs) {
                truncated = true
                return false
            }
            return true
        }
    }

    private data class CachedNode(
        val id: String,
        val path: String,
        val label: String,
        val text: String,
        val contentDescription: String,
        val className: String,
        val packageName: String,
        val viewId: String,
        val bounds: Rect,
        val clickable: Boolean,
        val longClickable: Boolean,
        val scrollable: Boolean,
        val editable: Boolean,
        val checked: Boolean,
        val selected: Boolean,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("nodeId", id)
            .put("label", label)
            .put("text", text)
            .put("contentDescription", contentDescription)
            .put("className", className)
            .put("packageName", packageName)
            .put("viewId", viewId)
            .put("bounds", JSONObject()
                .put("left", bounds.left)
                .put("top", bounds.top)
                .put("right", bounds.right)
                .put("bottom", bounds.bottom)
                .put("centerX", bounds.centerX())
                .put("centerY", bounds.centerY())
                .put("width", bounds.width())
                .put("height", bounds.height()))
            .put("clickable", clickable)
            .put("longClickable", longClickable)
            .put("scrollable", scrollable)
            .put("editable", editable)
            .put("checked", checked)
            .put("selected", selected)
    }

    companion object {
        private const val DEFAULT_ACTION_DEPTH = 8
        private const val MAX_TREE_DEPTH = 12
        private const val DEFAULT_STATE_NODES = 80
        private const val MAX_STATE_NODES = 200
        private const val MAX_CACHED_NODES = 240
        private const val TAP_DURATION_MS = 50L
        private const val LONG_PRESS_DURATION_MS = 600L
        private const val SWIPE_DURATION_MS = 260L
        private const val ACTION_TIMEOUT_MS = 1200L
        private const val GESTURE_DISPATCH_TIMEOUT_MS = 350L
        private const val GESTURE_SETTLE_PADDING_MS = 120L
        private const val MAX_GESTURE_SETTLE_MS = 1500L
        private const val SNAPSHOT_TIMEOUT_MS = 900L
        private const val MAX_SNAPSHOT_NODES = 650
        private const val SCREENSHOT_TIMEOUT_MS = 2500L
        private const val DEFAULT_WAIT_TIMEOUT_MS = 1600L
        private const val DEFAULT_IDLE_MS = 220L
        private const val MAX_WAIT_TIMEOUT_MS = 15000L
        private const val WAIT_POLL_MS = 80L
        private const val DEFAULT_SCREENSHOT_MAX_WIDTH = 720
        private const val MIN_SCREENSHOT_WIDTH = 320
        private const val MAX_SCREENSHOT_WIDTH = 1440
        private const val DEFAULT_SCREENSHOT_QUALITY = 82
        private const val MIN_SCREENSHOT_QUALITY = 45
        private const val MAX_SCREENSHOT_QUALITY = 95

        private val screenshotExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
            Thread(task, "LociantScreenshot").apply { isDaemon = true }
        }

        @Volatile
        var instance: LociantAccessibilityService? = null
            private set

        @Volatile
        private var lastEventType = 0

        @Volatile
        private var lastEventTimeMs = 0L
    }
}
