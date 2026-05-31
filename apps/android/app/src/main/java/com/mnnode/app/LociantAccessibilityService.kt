package com.mnnode.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LociantAccessibilityService : AccessibilityService() {
    private val tag = "LociantAccessibility"
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

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

    fun readScreenText(maxDepth: Int = DEFAULT_TEXT_DEPTH): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        return try {
            val lines = JSONArray()
            collectText(root, 0, maxDepth.coerceIn(1, MAX_TREE_DEPTH), lines)
            JSONObject()
                .put("ok", true)
                .put("text", linesToText(lines))
                .put("lines", lines)
                .put("lineCount", lines.length())
                .put("capturedAt", System.currentTimeMillis())
                .put("lastEvent", lastEventJson())
        } finally {
            root.recycle()
        }
    }

    fun readScreenStructure(maxDepth: Int = DEFAULT_TREE_DEPTH): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        val counter = Counter()
        return try {
            JSONObject()
                .put("ok", true)
                .put("root", snapshotNode(root, 0, maxDepth.coerceIn(1, MAX_TREE_DEPTH), counter))
                .put("nodeCount", counter.value)
                .put("capturedAt", System.currentTimeMillis())
                .put("lastEvent", lastEventJson())
        } finally {
            root.recycle()
        }
    }

    fun readClickableElements(maxDepth: Int = DEFAULT_ACTION_DEPTH): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        val elements = JSONArray()
        return try {
            collectActionable(root, 0, maxDepth.coerceIn(1, MAX_TREE_DEPTH), elements)
            JSONObject()
                .put("ok", true)
                .put("elements", elements)
                .put("count", elements.length())
                .put("capturedAt", System.currentTimeMillis())
        } finally {
            root.recycle()
        }
    }

    fun findText(query: String, exact: Boolean = false, maxDepth: Int = DEFAULT_ACTION_DEPTH): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        val results = JSONArray()
        return try {
            collectTextMatches(root, query, exact, 0, maxDepth.coerceIn(1, MAX_TREE_DEPTH), results)
            JSONObject()
                .put("ok", true)
                .put("query", query)
                .put("exact", exact)
                .put("results", results)
                .put("count", results.length())
                .put("capturedAt", System.currentTimeMillis())
        } finally {
            root.recycle()
        }
    }

    fun clickText(query: String, exact: Boolean = false, maxDepth: Int = DEFAULT_ACTION_DEPTH): JSONObject {
        val root = rootInActiveWindow ?: return unavailable("No active accessibility window.")
        return try {
            clickFirstTextMatch(root, query, exact, 0, maxDepth.coerceIn(1, MAX_TREE_DEPTH))
                ?: JSONObject()
                    .put("ok", false)
                    .put("action", "find_and_click")
                    .put("query", query)
                    .put("exact", exact)
                    .put("message", "No matching visible text found.")
        } finally {
            root.recycle()
        }
    }

    fun gestureClick(x: Int, y: Int): Boolean = dispatchGesture(buildTap(x, y, TAP_DURATION_MS))

    fun gestureLongClick(x: Int, y: Int): Boolean = dispatchGesture(buildTap(x, y, LONG_PRESS_DURATION_MS))

    fun gestureSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = SWIPE_DURATION_MS): Boolean =
        dispatchGesture(buildSwipe(x1, y1, x2, y2, durationMs))

    fun gestureSwipeUp(): Boolean {
        val bounds = screenBounds()
        return gestureSwipe(bounds.centerX(), (bounds.height() * 0.72f).toInt(), bounds.centerX(), (bounds.height() * 0.32f).toInt())
    }

    fun gestureSwipeDown(): Boolean {
        val bounds = screenBounds()
        return gestureSwipe(bounds.centerX(), (bounds.height() * 0.32f).toInt(), bounds.centerX(), (bounds.height() * 0.72f).toInt())
    }

    fun pressBack(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_HOME)
    fun pressRecentApps(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalActionOnMain(GLOBAL_ACTION_NOTIFICATIONS)

    fun openQuickSettings(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && performGlobalActionOnMain(GLOBAL_ACTION_QUICK_SETTINGS)

    private fun snapshotNode(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int, counter: Counter): JSONObject {
        counter.value += 1
        val item = nodeSummary(node, depth)
        if (depth < maxDepth && node.childCount > 0) {
            val children = JSONArray()
            forEachChild(node) { child ->
                children.put(snapshotNode(child, depth + 1, maxDepth, counter))
            }
            if (children.length() > 0) item.put("children", children)
        }
        return item
    }

    private fun collectText(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int, lines: JSONArray) {
        if (depth > maxDepth) return
        val label = labelOf(node)
        if (label.isNotBlank() && node.isVisibleToUser) lines.put(label)
        forEachChild(node) { child -> collectText(child, depth + 1, maxDepth, lines) }
    }

    private fun collectActionable(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int, result: JSONArray) {
        if (depth > maxDepth) return
        if (isActionable(node)) {
            val item = nodeSummary(node, depth).put("actions", actionLabels(node))
            val bounds = boundsOf(node)
            if (node.isClickable) item.put("tap", boundsJson(bounds))
            if (node.isLongClickable) item.put("longPress", boundsJson(bounds))
            result.put(item)
        }
        forEachChild(node) { child -> collectActionable(child, depth + 1, maxDepth, result) }
    }

    private fun collectTextMatches(
        node: AccessibilityNodeInfo,
        query: String,
        exact: Boolean,
        depth: Int,
        maxDepth: Int,
        result: JSONArray,
    ) {
        if (depth > maxDepth) return
        if (matches(node, query, exact)) {
            val item = nodeSummary(node, depth)
            clickableBounds(node)?.let { item.put("tap", boundsJson(it)) }
            result.put(item)
        }
        forEachChild(node) { child -> collectTextMatches(child, query, exact, depth + 1, maxDepth, result) }
    }

    private fun clickFirstTextMatch(
        node: AccessibilityNodeInfo,
        query: String,
        exact: Boolean,
        depth: Int,
        maxDepth: Int,
    ): JSONObject? {
        if (depth > maxDepth) return null
        if (matches(node, query, exact)) {
            val summary = nodeSummary(node, depth)
            val actionClick = performClickAction(node)
            if (actionClick.optBoolean("ok")) {
                return actionClick
                    .put("query", query)
                    .put("exact", exact)
                    .put("target", summary)
            }
            val tapBounds = clickableBounds(node) ?: boundsOf(node).takeIf { !it.isEmpty }
            if (tapBounds != null) {
                val success = gestureClick(tapBounds.centerX(), tapBounds.centerY())
                return JSONObject()
                    .put("ok", success)
                    .put("action", "find_and_click")
                    .put("method", "gesture")
                    .put("query", query)
                    .put("exact", exact)
                    .put("target", summary)
                    .put("tap", boundsJson(tapBounds))
                    .put("message", if (success) "Tapped matching text." else "Matching text found, but tap failed.")
            }
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try {
                val result = clickFirstTextMatch(child, query, exact, depth + 1, maxDepth)
                if (result != null) return result
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun performClickAction(node: AccessibilityNodeInfo): JSONObject {
        var current: AccessibilityNodeInfo? = node
        var recycleCurrent = false
        try {
            while (current != null) {
                if (isClickable(current)) {
                    val bounds = boundsOf(current)
                    val success = performNodeActionOnMain(current, AccessibilityNodeInfo.ACTION_CLICK)
                    return JSONObject()
                        .put("ok", success)
                        .put("action", "find_and_click")
                        .put("method", "accessibility_action")
                        .put("tap", boundsJson(bounds))
                        .put("message", if (success) "Clicked matching accessibility node." else "Accessibility click action failed.")
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

    private fun dispatchGesture(description: GestureDescription): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val completed = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                completed.set(true)
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                completed.set(false)
                latch.countDown()
            }
        }
        val started = runOnMainBoolean(ACTION_TIMEOUT_MS) {
            dispatchGesture(description, callback, null)
        }
        if (!started) return false
        if (Looper.myLooper() == Looper.getMainLooper()) return true
        return latch.await(GESTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS) && completed.get()
    }

    private fun performGlobalActionOnMain(action: Int): Boolean =
        runOnMainBoolean(ACTION_TIMEOUT_MS) { performGlobalAction(action) }

    private fun performNodeActionOnMain(node: AccessibilityNodeInfo, action: Int): Boolean =
        runOnMainBoolean(ACTION_TIMEOUT_MS) { node.performAction(action) }

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

    private fun nodeSummary(node: AccessibilityNodeInfo, depth: Int): JSONObject {
        val bounds = boundsOf(node)
        return JSONObject()
            .put("text", node.text?.toString().orEmpty())
            .put("contentDescription", node.contentDescription?.toString().orEmpty())
            .put("label", labelOf(node))
            .put("className", node.className?.toString().orEmpty())
            .put("packageName", node.packageName?.toString().orEmpty())
            .put("viewId", node.viewIdResourceName.orEmpty())
            .put("depth", depth)
            .put("visible", node.isVisibleToUser)
            .put("enabled", node.isEnabled)
            .put("clickable", node.isClickable)
            .put("longClickable", node.isLongClickable)
            .put("scrollable", node.isScrollable)
            .put("editable", node.isEditable)
            .put("checked", node.isChecked)
            .put("selected", node.isSelected)
            .put("bounds", boundsJson(bounds))
    }

    private fun forEachChild(node: AccessibilityNodeInfo, block: (AccessibilityNodeInfo) -> Unit) {
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try {
                block(child)
            } finally {
                child.recycle()
            }
        }
    }

    private fun matches(node: AccessibilityNodeInfo, query: String, exact: Boolean): Boolean {
        val target = query.trim()
        if (target.isBlank() || !node.isVisibleToUser) return false
        val values = listOf(node.text?.toString().orEmpty(), node.contentDescription?.toString().orEmpty())
        return if (exact) values.any { it.trim() == target }
        else values.any { it.contains(target, ignoreCase = true) }
    }

    private fun isActionable(node: AccessibilityNodeInfo): Boolean =
        node.isEnabled && node.isVisibleToUser && (node.isClickable || node.isLongClickable)

    private fun isClickable(node: AccessibilityNodeInfo): Boolean =
        node.isEnabled && node.isVisibleToUser && node.isClickable

    private fun clickableBounds(node: AccessibilityNodeInfo): Rect? {
        var current: AccessibilityNodeInfo? = node
        var recycleCurrent = false
        try {
            while (current != null) {
                val bounds = boundsOf(current)
                if (isClickable(current) && !bounds.isEmpty) return Rect(bounds)
                val parent = current.parent
                if (recycleCurrent) current.recycle()
                current = parent
                recycleCurrent = true
            }
        } finally {
            if (recycleCurrent) current?.recycle()
        }
        return null
    }

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

    private fun screenBounds(): Rect {
        val metrics = resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun labelOf(node: AccessibilityNodeInfo): String =
        listOf(node.text, node.contentDescription)
            .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
            .joinToString(" ")

    private fun actionLabels(node: AccessibilityNodeInfo): JSONArray = JSONArray().apply {
        if (node.isClickable) put("click")
        if (node.isLongClickable) put("long_click")
        if (node.isScrollable) put("scroll")
    }

    private fun linesToText(lines: JSONArray): String =
        (0 until lines.length()).joinToString("\n") { lines.optString(it) }

    private fun unavailable(message: String): JSONObject = JSONObject()
        .put("ok", false)
        .put("code", "accessibility_unavailable")
        .put("message", message)
        .put("lastEvent", lastEventJson())

    private fun lastEventJson(): JSONObject = JSONObject()
        .put("type", lastEventType)
        .put("timeMs", lastEventTimeMs)

    private class Counter(var value: Int = 0)

    companion object {
        private const val DEFAULT_TEXT_DEPTH = 8
        private const val DEFAULT_TREE_DEPTH = 5
        private const val DEFAULT_ACTION_DEPTH = 8
        private const val MAX_TREE_DEPTH = 12
        private const val TAP_DURATION_MS = 50L
        private const val LONG_PRESS_DURATION_MS = 600L
        private const val SWIPE_DURATION_MS = 260L
        private const val ACTION_TIMEOUT_MS = 1200L
        private const val GESTURE_TIMEOUT_MS = 5000L

        @Volatile
        var instance: LociantAccessibilityService? = null
            private set

        @Volatile
        private var lastEventType = 0

        @Volatile
        private var lastEventTimeMs = 0L
    }
}
