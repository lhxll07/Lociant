package io.lociant.android.runtime

import android.content.Context
import io.lociant.runtime.model.ModelManager
import io.lociant.android.server.LociantServer
import io.lociant.data.storage.LocalStore

object LociantRuntime {
    @Volatile
    private var holder: Holder? = null

    fun server(context: Context): LociantServer = get(context).server

    fun modelManager(context: Context): ModelManager = get(context).modelManager

    fun localStore(context: Context): LocalStore = get(context).localStore


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
            val server = LociantServer(
                appContext,
                modelManager,
                startVisionRuntime = { payload -> LociantRuntimeService.startRuntime(appContext, payload) },
            )
            return Holder(
                modelManager = modelManager,
                localStore = localStore,
                server = server,
            ).also { holder = it }
        }
    }

    private class Holder(
        val modelManager: ModelManager,
        val localStore: LocalStore,
        val server: LociantServer,
    )
}
