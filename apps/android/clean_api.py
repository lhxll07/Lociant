import re

p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\server\ApiServerController.kt'
with open(p, 'r', encoding='utf-8') as f:
    c = f.read()

# 1. Remove imports
for imp in ['AcpAgentManager', 'ModelMarket', 'TriggerEngine', 'SceneManager']:
    c = c.replace(f'import com.mnnode.app.{imp}\n', '')

# 2. Remove constructor params
c = c.replace('    private val sceneManager: SceneManager,\n', '')
c = c.replace('    private val triggerEngine: TriggerEngine,\n', '')
c = c.replace('    private val acpAgentManager: AcpAgentManager,\n', '')

# 3. Remove modelMarket and NotificationTools
c = c.replace('    private val modelMarket by lazy { ModelMarket(context, modelManager) }\n', '')
c = c.replace('    private val notificationTools by lazy { NotificationTools(context) }\n', '')

# 4. Fix toolRegistry
c = c.replace('RuntimeTools(context, runtimeState = { runtimeSummary() })', 'RuntimeTools(runtimeState = { runtimeSummary() })')
c = c.replace('ModelTools(\n                    modelManager = modelManager,\n                    preloadModel = { chatController.preload(it.ifBlank { modelId }) },\n                    cancelChat = { chatController.cancelCurrent() },\n                )', 'ModelTools(modelManager = modelManager)')
c = c.replace('                StorageTools(sessionStore, localStore),\n                notificationTools,', '')

# 5. Remove ACP command block
old = '        if (isAgentCommand(command)) return withAgentState(acpAgentManager.command(command, payload))\n        synchronized(this) {'
c = c.replace(old, '        synchronized(this) {')

# 6. Remove store helpers
c = re.sub(r'    private fun storeNamespace.*?\n    private fun storeKey.*?\n', '', c)

# 7. Remove withAgentState and isAgentCommand methods
c = re.sub(r'\n    private fun withAgentState\(agent: JSONObject\).*?\n    \}', '', c, flags=re.DOTALL)
c = re.sub(r'\n    private fun isAgentCommand.*?[}]\n', '', c, flags=re.DOTALL)

# 8. Remove acpAgentManager references
c = c.replace('            acpAgentManager.clearSessionIfMatches(deletedId)\n', '')
c = c.replace('        json.put("agentNetwork", acpAgentManager.state())\n', '')

# 9. Remove Ollama from ChatProtocol
c = c.replace('        OPENAI("openai"),\n        OLLAMA("ollama"),', '        OPENAI("openai"),')
c = c.replace('ChatProtocol.OLLAMA', 'ChatProtocol.OPENAI')

# 10. Remove routes (one-liners and multi-line)
routes_to_remove = [
    'get("/v1/scenes") { call.withCors(); call.respondText(sceneManager.listScenesJson(), JsonContentType) }',
    'post("/v1/scenes/{sceneId}/load") { call.withCors(); handleSceneLoad(call) }',
    'post("/v1/scenes/{sceneId}/delete") { call.withCors(); handleSceneDelete(call) }',
    'get("/v1/events/{sceneId}") { call.withCors(); handleEvents(call) }',
    'get("/v1/store/{namespace}/{key}") { call.withCors(); handleStoreGet(call) }',
    'get("/v1/store/{namespace}") { call.withCors(); handleStoreList(call) }',
    'post("/v1/store/{namespace}/{key}") { call.withCors(); handleStoreSet(call) }',
    'post("/v1/store/{namespace}/{key}/delete") { call.withCors(); handleStoreRemove(call) }',
    'post("/api/chat") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleChat(call, ChatProtocol.OLLAMA) }',
    'post("/v1/runtime/agent.prompt.stream") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleAgentPromptStream(call) }',
    'get("/v1/models/market") { call.withCors(); handleModelMarket(call) }',
    'get("/v1/models/market/{modelId}/progress") { call.withCors(); handleModelMarketProgress(call) }',
    'post("/v1/models/market/{modelId}/install") { call.withCors(); handleModelMarketInstall(call) }',
    'post("/v1/models/{modelId}/delete") { call.withCors(); handleModelDelete(call) }',
    'get("/v1/models/full") { call.withCors(); call.respondText(modelManager.listModelsJson(), JsonContentType) }',
    'get("/v1/preview") { call.withCors(); handlePreview(call) }',
    'get("/v1/preview/stream") { call.withCors(); handlePreviewStream(call) }',
]
for route in routes_to_remove:
    pattern = re.escape('                        ' + route)
    c = re.sub(pattern + r'\n', '', c)

# 11. Remove handler methods
handlers_to_remove = [
    'handleSceneLoad', 'handleSceneDelete', 'handleEvents',
    'handleStoreGet', 'handleStoreSet', 'handleStoreRemove', 'handleStoreList',
    'handleAgentPromptStream', 'agentPromptStreamContent', 'writeAgentPromptStream', 'writeSse',
    'handleModelMarket', 'handleModelMarketProgress', 'handleModelMarketInstall', 'handleModelDelete',
    'handlePreview', 'handlePreviewStream',
]
for h in handlers_to_remove:
    c = re.sub(r'    private (suspend )?fun ' + h + r'.*?\n    \}\n', '', c, flags=re.DOTALL)

# 12. Clean blank lines
c = re.sub(r'\n{4,}', '\n\n\n', c)

with open(p, 'w', encoding='utf-8') as f:
    f.write(c)
print('DONE')
