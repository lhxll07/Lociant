package com.mnnode.app

import android.webkit.JavascriptInterface
import com.mnnode.app.model.ModelManager
import com.mnnode.app.scene.SceneManager
import com.mnnode.app.scene.SceneRuntimeManager
import com.mnnode.app.storage.LocalStore
import org.json.JSONObject

class MNNodeBridge(
    private val host: Host,
    private val sceneManager: SceneManager,
    private val sceneRuntimeManager: SceneRuntimeManager,
    private val modelManager: ModelManager,
    private val localStore: LocalStore,
) {
    interface Host {
        fun openScenePackPicker()
        fun openModelPackagePicker()
        fun setCameraPreviewRect(x: Int, y: Int, width: Int, height: Int)
        fun viewportMetrics(): JSONObject
        fun startCamera(): String
        fun stopCamera(): String
        fun cameraState(): String
        fun startVision(configJson: String?): String
        fun stopVision(): String
        fun visionState(): String
        fun modelChat(requestJson: String?): String
        fun runtimeServiceCommand(command: String, payloadJson: String?): String
    }

    @JavascriptInterface
    fun getScenes(): String = sceneManager.listScenesJson()

    @JavascriptInterface
    fun activateSceneRuntime(sceneId: String): String {
        val scene = sceneManager.findScene(sceneId) ?: return sceneRuntimeManager.snapshot().toString()
        return sceneRuntimeManager.activate(scene).toString()
    }

    @JavascriptInterface
    fun getRuntimeSnapshot(): String = sceneRuntimeManager.snapshot().toString()

    @JavascriptInterface
    fun runtimeCommand(sceneId: String, command: String, payloadJson: String?): String {
        return sceneRuntimeManager.command(sceneId, command, parseObject(payloadJson)).toString()
    }

    @JavascriptInterface
    fun getModels(): String = modelManager.listModelsJson()

    @JavascriptInterface
    fun installScenePack(): String {
        host.openScenePackPicker()
        return JSONObject().put("ok", true).put("state", "picker_opened").toString()
    }

    @JavascriptInterface
    fun installModelPackage(): String {
        host.openModelPackagePicker()
        return JSONObject().put("ok", true).put("state", "picker_opened").toString()
    }

    @JavascriptInterface
    fun deleteModel(modelId: String): String {
        return jsonResult { modelManager.deleteModel(modelId) }
    }

    @JavascriptInterface
    fun uninstallScene(sceneId: String): String {
        return jsonResult {
            JSONObject()
                .put("ok", sceneManager.uninstallScene(sceneId))
                .put("id", sceneId)
        }
    }

    @JavascriptInterface
    fun setCameraPreviewRect(x: Int, y: Int, width: Int, height: Int): String {
        host.setCameraPreviewRect(x, y, width, height)
        return JSONObject().put("ok", true).toString()
    }

    @JavascriptInterface
    fun getViewportMetrics(): String = host.viewportMetrics().toString()

    @JavascriptInterface
    fun startCamera(): String = host.startCamera()

    @JavascriptInterface
    fun stopCamera(): String = host.stopCamera()

    @JavascriptInterface
    fun getCameraState(): String = host.cameraState()

    @JavascriptInterface
    fun startVision(configJson: String?): String = host.startVision(configJson)

    @JavascriptInterface
    fun stopVision(): String = host.stopVision()

    @JavascriptInterface
    fun getVisionState(): String = host.visionState()

    @JavascriptInterface
    fun modelChat(requestJson: String?): String = host.modelChat(requestJson)

    @JavascriptInterface
    fun runtimeServiceCommand(command: String, payloadJson: String?): String {
        return host.runtimeServiceCommand(command, payloadJson)
    }

    @JavascriptInterface
    fun storeGet(namespace: String, key: String): String = jsonResult { localStore.get(namespace, key) }

    @JavascriptInterface
    fun storeSet(namespace: String, key: String, valueJson: String?): String {
        return jsonResult { localStore.set(namespace, key, localStore.parseValue(valueJson)) }
    }

    @JavascriptInterface
    fun storeRemove(namespace: String, key: String): String = jsonResult { localStore.remove(namespace, key) }

    @JavascriptInterface
    fun storeList(namespace: String): String = jsonResult { localStore.list(namespace) }

    @JavascriptInterface
    fun getNodeInfo(): String {
        return JSONObject()
            .put("name", "MNNode Android")
            .put("version", "0.3.0")
            .put("runtime", "webview-shell")
            .put("features", listOf("webview", "scene-pack", "camera-preview", "camerax-analysis", "ncnn", "mnn-chat"))
            .toString()
    }

    private fun jsonResult(block: () -> JSONObject): String {
        return runCatching { block().toString() }
            .getOrElse { error ->
                JSONObject()
                    .put("ok", false)
                    .put("message", error.message ?: "operation failed")
                    .toString()
            }
    }

    private fun parseObject(raw: String?): JSONObject {
        return runCatching { JSONObject(raw ?: "{}") }.getOrDefault(JSONObject())
    }
}
