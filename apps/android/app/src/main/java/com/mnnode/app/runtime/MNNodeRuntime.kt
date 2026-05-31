package com.mnnode.app.runtime

import android.content.Context
import com.mnnode.app.agent.AcpAgentManager
import com.mnnode.app.model.ChatCapability
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.MnnRuntime
import com.mnnode.app.scene.SceneManager
import com.mnnode.app.scene.ScenePackInstaller
import com.mnnode.app.server.ApiServerController
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore

object MNNodeRuntime {
    @Volatile
    private var holder: Holder? = null

    fun apiServer(context: Context): ApiServerController = get(context).apiServer

    fun mnnRuntime(context: Context): MnnRuntime = get(context).mnnRuntime

    fun modelManager(context: Context): ModelManager = get(context).modelManager

    fun sceneManager(context: Context): SceneManager = get(context).sceneManager

    fun scenePackInstaller(context: Context): ScenePackInstaller = get(context).scenePackInstaller

    fun triggerEngine(context: Context): TriggerEngine = get(context).triggerEngine

    fun sessionStore(context: Context): SessionStore = get(context).sessionStore

    fun localStore(context: Context): LocalStore = get(context).localStore

    fun acpAgent(context: Context): AcpAgentManager = get(context).acpAgent

    fun serviceState(context: Context) = get(context).apiServer.serviceState()

    fun runtimeSummary(context: Context) = get(context).apiServer.runtimeSummary()

    private fun get(context: Context): Holder {
        val current = holder
        if (current != null) return current
        synchronized(this) {
            val existing = holder
            if (existing != null) return existing
            val appContext = context.applicationContext
            val modelManager = ModelManager(appContext)
            val localStore = LocalStore(appContext)
            val sessionStore = SessionStore(appContext)
            val sceneManager = SceneManager(appContext)
            val scenePackInstaller = ScenePackInstaller(appContext)
            val mnnRuntime = MnnRuntime(appContext)
            val chatCapability = ChatCapability(modelManager, mnnRuntime)
            val triggerEngine = TriggerEngine()
            val acpAgent = AcpAgentManager(localStore, sessionStore)
            val apiServer = ApiServerController(
                appContext,
                modelManager,
                sceneManager,
                chatCapability,
                localStore,
                sessionStore,
                triggerEngine,
                acpAgent,
            )
            triggerEngine.setCallTool { name, args -> apiServer.callTool(name, args) }
            return Holder(
                modelManager = modelManager,
                localStore = localStore,
                sessionStore = sessionStore,
                sceneManager = sceneManager,
                scenePackInstaller = scenePackInstaller,
                triggerEngine = triggerEngine,
                acpAgent = acpAgent,
                mnnRuntime = mnnRuntime,
                chatCapability = chatCapability,
                apiServer = apiServer,
            ).also { holder = it }
        }
    }

    private class Holder(
        val modelManager: ModelManager,
        val localStore: LocalStore,
        val sessionStore: SessionStore,
        val sceneManager: SceneManager,
        val scenePackInstaller: ScenePackInstaller,
        val triggerEngine: TriggerEngine,
        val acpAgent: AcpAgentManager,
        val mnnRuntime: MnnRuntime,
        val chatCapability: ChatCapability,
        val apiServer: ApiServerController,
    ) {
        fun close() { apiServer.close(); mnnRuntime.close() }
    }
}
