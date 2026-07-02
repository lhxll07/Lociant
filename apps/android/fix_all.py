import re

# === FIX 1: MNNodeRuntime.kt ===
p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\runtime\MNNodeRuntime.kt'
c = open(p, 'r', encoding='utf-8').read()

c = c.replace('import com.mnnode.app.agent.AcpAgentManager\n', '')
c = c.replace('import com.mnnode.app.scene.SceneManager\n', '')
c = c.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')

c = c.replace('    fun sceneManager(context: Context): SceneManager = get(context).sceneManager\n\n', '')
c = c.replace('    fun scenePackInstaller(context: Context): ScenePackInstaller = get(context).scenePackInstaller\n\n', '')
c = c.replace('    fun triggerEngine(context: Context): TriggerEngine = get(context).triggerEngine\n\n', '')
c = c.replace('    fun acpAgent(context: Context): AcpAgentManager = get(context).acpAgent\n\n', '')

c = c.replace('            val sceneManager = SceneManager(appContext)\n', '')
c = c.replace('            val scenePackInstaller = ScenePackInstaller(appContext)\n', '')
c = c.replace('            val triggerEngine = TriggerEngine()\n', '')
c = c.replace('            val acpAgent = AcpAgentManager(localStore, sessionStore)\n', '')
c = c.replace('            triggerEngine.setCallTool { name, args -> apiServer.callTool(name, args) }\n', '')

c = c.replace('                sceneManager = sceneManager,\n', '')
c = c.replace('                scenePackInstaller = scenePackInstaller,\n', '')
c = c.replace('                triggerEngine = triggerEngine,\n', '')
c = c.replace('                acpAgent = acpAgent,\n', '')

old = 'sceneManager,\n                chatCapability,\n                localStore,\n                sessionStore,\n                triggerEngine,\n                acpAgent,'
new = 'chatCapability,\n                localStore,\n                sessionStore,'
c = c.replace(old, new)

old = 'val sceneManager: SceneManager,\n        val scenePackInstaller: ScenePackInstaller,\n        val triggerEngine: TriggerEngine,\n        val acpAgent: AcpAgentManager,'
c = c.replace(old, '')

c = c.replace('import com.mnnode.app.runtime.TriggerEngine\n', '')

open(p, 'w', encoding='utf-8').write(c)
print('MNNodeRuntime DONE')

# === FIX 2: ChatController.kt ===
p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\server\ChatController.kt'
c = open(p, 'r', encoding='utf-8').read()

c = c.replace('private data class StreamJob(val id: String, val future: CompletableFuture<ModelChatResult>)\n\n', '')
idx = c.find('private class ChatRequestQueue(')
if idx > 0:
    c = c[:c.rfind('\n', 0, idx)] + '\n'

open(p, 'w', encoding='utf-8').write(c)
print('ChatController DONE')

# === FIX 3: MainActivity.kt ===
p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\MainActivity.kt'
c = open(p, 'r', encoding='utf-8').read()

c = c.replace('import com.mnnode.app.scene.SceneManager\n', '')
c = c.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')
c = c.replace('import com.mnnode.app.runtime.TriggerEngine\n', '')
c = c.replace('import com.mnnode.app.runtime.MNNodeRuntime\n', '')
c = c.replace('import com.mnnode.app.config.RuntimeDefaults\n', '')

c = c.replace('    private lateinit var sceneManager: SceneManager\n', '')
c = c.replace('    private lateinit var scenePackInstaller: ScenePackInstaller\n', '')
c = c.replace('    private lateinit var triggerEngine: TriggerEngine\n', '')

c = c.replace('        sceneManager = MNNodeRuntime.sceneManager(this)\n', '')
c = c.replace('        scenePackInstaller = MNNodeRuntime.scenePackInstaller(this)\n', '')
c = c.replace('        triggerEngine = MNNodeRuntime.triggerEngine(this)\n', '')

c = c.replace('.addPathHandler("/installed-scenes/', '.addPathHandler("/removed-scenes/')

c = c.replace('    private val installScenePackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->\n        if (uri == null) notifySceneInstallResult(false, "cancelled", null) else handleScenePackage(uri)\n    }\n', '')

c = c.replace('"scene.install" -> { installScenePackage.launch(PACKAGE_MIME_TYPES); runtimeSummaryWithWindow() }\n', '')
c = c.replace('"scene.list" -> { runtimeSummaryWithWindow().put("scenes", apiServerController.command("status", payload).optJSONArray("scenes")) }\n', '')
c = c.replace('"scene.delete" -> { apiServerController.command("scene.delete", payload); runtimeSummaryWithWindow() }\n', '')

c = c.replace('            .put("triggers", triggerEngine.snapshot())\n', '')

c = c.replace('"agent.status", "agent.saveNode", "agent.selectNode", "agent.connect", "agent.disconnect",\n            "agent.session.create", "agent.session.select", "agent.prompt" -> apiServerController.command(command, payload).withRuntimeState()\n', '')

# Stub for chooseGadgetbridgeExportFolder
c = c.replace('    override fun openPermissionSettings(kind: String) {',
    '    override fun chooseGadgetbridgeExportFolder() {}\n\n    override fun openPermissionSettings(kind: String) {')

# Fix SceneManager.LOCAL_ORIGIN
c = c.replace('SceneManager.LOCAL_ORIGIN', '"https://appassets.androidplatform.net"')

open(p, 'w', encoding='utf-8').write(c)
print('MainActivity DONE')
