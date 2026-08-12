package io.lociant.android.runtime

import android.content.Context
import io.lociant.runtime.model.ChatCapability
import io.lociant.runtime.model.ModelManager
import io.lociant.runtime.model.MnnRuntime
import io.lociant.android.server.LociantServer
import io.lociant.data.storage.LocalStore

object LociantRuntime {
    @Volatile
    private var holder: Holder? = null

    fun server(context: Context): LociantServer = get(context).server

    fun mnnRuntime(context: Context): MnnRuntime = get(context).mnnRuntime

    fun modelManager(context: Context): ModelManager = get(context).modelManager

    fun localStore(context: Context): LocalStore = get(context).localStore

    fun serviceState(context: Context) = get(context).server.serviceState()

    fun runtimeSummary(context: Context) = get(context).server.runtimeSummary()

    private fun get(context: Context): Holder {
        val current = holder
        if (current != null) return current
        synchronized(this) {
            val existing = holder
            if (existing != null) return existing
            val appContext = context.applicationContext
            val modelManager = ModelManager(appContext)
            val localStore = LocalStore(appContext)
            val mnnRuntime = MnnRuntime(appContext)
            val chatCapability = ChatCapability(modelManager, mnnRuntime)
            val server = LociantServer(
                appContext,
                modelManager,
                chatCapability,
                localStore,
                startVisionRuntime = { payload -> LociantRuntimeService.startRuntime(appContext, payload) },
            )
            return Holder(
                modelManager = modelManager,
                localStore = localStore,
                mnnRuntime = mnnRuntime,
                chatCapability = chatCapability,
                server = server,
            ).also { holder = it }
        }
    }

    private class Holder(
        val modelManager: ModelManager,
        val localStore: LocalStore,
        val mnnRuntime: MnnRuntime,
        val chatCapability: ChatCapability,
        val server: LociantServer,
    ) {
        fun close() { mnnRuntime.close() }
    }
}
