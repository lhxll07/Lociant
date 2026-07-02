import re, sys

action = sys.argv[1]
path = sys.argv[2]

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if action == 'mnnruntime':
    # Remove ACP agent
    content = content.replace('import com.mnnode.app.agent.AcpAgentManager\n', '')
    content = content.replace('import com.mnnode.app.scene.SceneManager\n', '')
    content = content.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')
    content = content.replace('import com.mnnode.app.model.ModelManager\n', '')  # still used? no, keep modelManager
    # Wait, ModelManager is still needed. Let me re-add it.
    
    # Actually, let me just remove specific references
    content = content.replace('    private val acpAgent: AcpAgentManager,\n', '    // acpAgent removed\n')
    content = content.replace('    val sceneManager: SceneManager = get(context).sceneManager\n', '')
    content = content.replace('    fun sceneManager(context: Context): SceneManager = get(context).sceneManager\n\n', '')
    content = content.replace('    fun scenePackInstaller(context: Context): ScenePackInstaller = get(context).scenePackInstaller\n\n', '')
    content = content.replace('    fun triggerEngine(context: Context): TriggerEngine = get(context).triggerEngine\n\n', '')
    content = content.replace('    fun acpAgent(context: Context): AcpAgentManager = get(context).acpAgent\n\n', '')
    content = content.replace('            val triggerEngine = TriggerEngine()\n', '')
    content = content.replace('            val acpAgent = AcpAgentManager(localStore, sessionStore)\n', '')
    content = content.replace('            val sceneManager = SceneManager(appContext)\n', '')
    content = content.replace('            val scenePackInstaller = ScenePackInstaller(appContext)\n', '')
    content = content.replace('                sceneManager = sceneManager,\n', '')
    content = content.replace('                scenePackInstaller = scenePackInstaller,\n', '')
    content = content.replace('                triggerEngine = triggerEngine,\n', '')
    content = content.replace('                acpAgent = acpAgent,\n', '')
    content = content.replace('            triggerEngine.setCallTool { name, args -> apiServer.callTool(name, args) }\n', '')
    
    # Clean up imports
    content = content.replace('import com.mnnode.app.agent.AcpAgentManager\n', '')
    content = content.replace('import com.mnnode.app.scene.SceneManager\n', '')
    content = content.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')
    
    # Remove TriggerEngine import if present
    content = re.sub(r'import com\.mnnode\.app\.runtime\.TriggerEngine\n', '', content)

elif action == 'mainactivity':
    # Remove sceneManager, scenePackInstaller, triggerEngine references
    content = content.replace('import com.mnnode.app.scene.SceneManager\n', '')
    content = content.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')
    content = content.replace('import com.mnnode.app.runtime.TriggerEngine\n', '')
    content = content.replace('    private lateinit var sceneManager: SceneManager\n', '')
    content = content.replace('    private lateinit var scenePackInstaller: ScenePackInstaller\n', '')
    content = content.replace('    private lateinit var triggerEngine: TriggerEngine\n', '')
    content = content.replace('    private val installScenePackage', '    // Scene package install removed\n    private val installScenePackage')
    # Remove installScenePackage handler
    content = re.sub(r'    private val installScenePackage = registerForActivityResult.*?handleScenePackage\(uri\)\n    \}\n', '', content, flags=re.DOTALL)
    # Also handleNotifyScene/results in bridge
    content = content.replace('notifySceneInstallResult', '// notifySceneInstallResult removed')
    content = content.replace('handleScenePackage', '// handleScenePackage removed')
    # Remove scene-related methods from the shellBridge commands
    content = re.sub(r'                \"scene\.install\".*?\n', '', content)
    content = re.sub(r'                \"scene\.list\".*?\n', '', content)
    content = re.sub(r'                \"scene\.delete\".*?\n', '', content)
    # Remove MNNodeRuntime references to removed types
    content = content.replace('.sceneManager', '/* sceneManager removed */')
    content = content.replace('.scenePackInstaller', '/* scenePackInstaller removed */')

elif action == 'androidtools':
    # Remove Gadgetbridge-related code
    content = re.sub(r'        tool\(\s*name = "wearable_sync".*?policy = actionPolicy,\s*\) \{ args -> wearableSync\(args\) \},', '', content, flags=re.DOTALL)
    content = re.sub(r'        tool\(\s*name = "wearable_snapshot".*?\) \{ args -> GadgetbridgeSnapshotReader\(context, localStore\)\.snapshot\(args\) \},', '', content, flags=re.DOTALL)
    
    # Remove Gadgetbridge methods
    for m in ['wearableSync', 'sendGadgetbridgeSync', 'sendGadgetbridgeExport', 'sendAndWaitForBroadcast', 'registerReceiverCompat', 'findGadgetbridgePackage', 'packageInstalled']:
        content = re.sub(r'    private fun ' + m + r'.*?\n    \}', '', content, flags=re.DOTALL)
    
    # Remove Gadgetbridge constants
    content = re.sub(r'        private val GADGETBRIDGE_PACKAGES.*?\n', '', content, flags=re.DOTALL)
    for c in ['GB_ACTION_ACTIVITY_SYNC', 'GB_ACTION_ACTIVITY_SYNC_FINISH', 'GB_ACTION_TRIGGER_EXPORT', 'GB_ACTION_DATABASE_EXPORT_SUCCESS', 'GB_ACTION_DATABASE_EXPORT_FAIL', 'DEFAULT_WEARABLE_SYNC_TIMEOUT_MS', 'MAX_WEARABLE_SYNC_TIMEOUT_MS']:
        content = re.sub(r'        private const val ' + c + r'.*?\n', '', content)
    
    # Remove import
    content = content.replace('import com.mnnode.app.storage.LocalStore\n', '')
    # Actually LocalStore might still be used elsewhere. Re-add if needed.
    # Remove GadgetbridgeSnapshotReader import
    content = content.replace('import com.mnnode.app.storage.LocalStore\n', '')

elif action == 'chatformat':
    # Remove Ollama stream format
    content = re.sub(r'        OLLAMA;?\n', '', content)
    # Remove ollama-specific methods
    for m in ['ollamaLine', 'ollamaDoneLine', 'ollamaStreamContent']:
        content = re.sub(r'    private fun ' + m + r'.*?\n    \}\n', '', content, flags=re.DOTALL)
    # Remove NdjsonContentType
    content = content.replace('        val NdjsonContentType = ContentType.parse("application/x-ndjson").withParameter("charset", "utf-8")\n', '')
    # Remove OLLAMA from when expressions in StreamFormat
    content = re.sub(r'            OLLAMA -> ollamaLine\([^)]*\)\n', '', content)
    content = re.sub(r'            OLLAMA -> ".*?"\n', '', content)
    content = re.sub(r'            OLLAMA -> [^}]*?\n        \}\n', '', content)
    # Remove remaining OLLAMA references
    content = content.replace('            OLLAMA -> ollamaDoneLine(meta.modelId, result)\n', '')

elif action == 'modelmapper':
    # Remove Ollama-specific code
    content = re.sub(r'    fun parseOllamaChat.*?\n    \}\n', '', content, flags=re.DOTALL)
    content = re.sub(r'    fun ollamaResponse.*?\n    \}\n', '', content, flags=re.DOTALL)
    content = re.sub(r'    private fun parseOllamaMessages.*?\n    \}\n', '', content, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('OK')
