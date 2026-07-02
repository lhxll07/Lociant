import re

# MNNodeRuntime.kt
path = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\runtime\MNNodeRuntime.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('import com.mnnode.app.agent.AcpAgentManager\n', '')
c = c.replace('import com.mnnode.app.scene.SceneManager\n', '')
c = c.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')

for m in ['fun sceneManager(context: Context): SceneManager = get(context).sceneManager',
    'fun scenePackInstaller(context: Context): ScenePackInstaller = get(context).scenePackInstaller',
    'fun triggerEngine(context: Context): TriggerEngine = get(context).triggerEngine',
    'fun acpAgent(context: Context): AcpAgentManager = get(context).acpAgent']:
    c = c.replace(m + '\n\n', '')

c = c.replace('            val sceneManager = SceneManager(appContext)\n', '')
c = c.replace('            val scenePackInstaller = ScenePackInstaller(appContext)\n', '')
c = c.replace('            val triggerEngine = TriggerEngine()\n', '')
c = c.replace('            val acpAgent = AcpAgentManager(localStore, sessionStore)\n', '')
c = c.replace('triggerEngine.setCallTool { name, args -> apiServer.callTool(name, args) }\n', '')

old_ctor = 'val apiServer = ApiServerController(\n                appContext,\n                modelManager,\n                sceneManager,\n                chatCapability,\n                localStore,\n                sessionStore,\n                triggerEngine,\n                acpAgent,\n                startVisionRuntime = { payload -> MNNodeRuntimeService.startRuntime(appContext, payload) },\n            )'
new_ctor = 'val apiServer = ApiServerController(\n                appContext,\n                modelManager,\n                chatCapability,\n                localStore,\n                sessionStore,\n                startVisionRuntime = { payload -> MNNodeRuntimeService.startRuntime(appContext, payload) },\n            )'
c = c.replace(old_ctor, new_ctor)

old_holder = 'val modelManager: ModelManager,\n        val localStore: LocalStore,\n        val sessionStore: SessionStore,\n        val sceneManager: SceneManager,\n        val scenePackInstaller: ScenePackInstaller,\n        val triggerEngine: TriggerEngine,\n        val acpAgent: AcpAgentManager,\n        val mnnRuntime: MnnRuntime,\n        val chatCapability: ChatCapability,\n        val apiServer: ApiServerController,'
new_holder = 'val modelManager: ModelManager,\n        val localStore: LocalStore,\n        val sessionStore: SessionStore,\n        val mnnRuntime: MnnRuntime,\n        val chatCapability: ChatCapability,\n        val apiServer: ApiServerController,'
c = c.replace(old_holder, new_holder)
c = re.sub(r'import com\.mnnode\.app\.runtime\.TriggerEngine\n', '', c)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print('MNNodeRuntime OK')
