package com.mnnode.app.runtime

import android.content.Context
import com.mnnode.app.model.ChatCapability
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.MnnRuntime
import com.mnnode.app.server.ApiServerController
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore

object MNNodeRuntime {
    @Volatile
    private var holder: Holder? = null

    fun apiServer(context: Context): ApiServerController = get(context).apiServer

    fun mnnRuntime(context: Context): MnnRuntime = get(context).mnnRuntime

    fun modelManager(context: Context): ModelManager = get(context).modelManager

    fun sessionStore(context: Context): SessionStore = get(context).sessionStore

    fun localStore(context: Context): LocalStore = get(context).localStore

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
            val mnnRuntime = MnnRuntime(appContext)
            val chatCapability = ChatCapability(modelManager, mnnRuntime)
            val apiServer = ApiServerController(
                appContext,
                modelManager,
                chatCapability,
                localStore,
                sessionStore,
                startVisionRuntime = { payload -> MNNodeRuntimeService.startRuntime(appContext, payload) },
            )
            return Holder(
                modelManager = modelManager,
                localStore = localStore,
                sessionStore = sessionStore,
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
        
        val mnnRuntime: MnnRuntime,
        val chatCapability: ChatCapability,
        val apiServer: ApiServerController,
    ) {
        fun close() { apiServer.close(); mnnRuntime.close() }
    }
}
