p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\server\ApiServerController.kt'
c = open(p, 'r', encoding='utf-8').read()

# Remove imports
c = c.replace('import com.mnnode.app.agent.AcpAgentManager\n', '')
c = c.replace('import com.mnnode.app.runtime.TriggerEngine\n', '')
c = c.replace('import com.mnnode.app.scene.SceneManager\n', '')
c = c.replace('import com.mnnode.app.model.ModelMarket\n', '')

# Remove constructor params
c = c.replace('    private val sceneManager: SceneManager,\n', '')
c = c.replace('    private val triggerEngine: TriggerEngine,\n', '')
c = c.replace('    private val acpAgentManager: AcpAgentManager,\n', '')

# Remove modelMarket lazy init
c = c.replace('    private val modelMarket by lazy { ModelMarket(context, modelManager) }\n', '')

# Remove agent command block
old = """    fun command(command: String, payload: JSONObject = JSONObject()): JSONObject {
        if (command.startsWith("agent.")) {
            val agentResult = acpAgentManager.command(command, payload)
            val merged = state()
            agentResult.keys().forEach { key ->
                merged.put(if (key == "currentSessionId") "agentCurrentSessionId" else key, agentResult.opt(key))
            }
            val freshState = acpAgentManager.state()
            freshState.optJSONObject("agent")?.optString("sessionId")?.takeIf { it.isNotBlank() }
                ?.let { merged.put("agentCurrentSessionId", it) }
            return merged.put("agentNetwork", freshState)
        }
        synchronized(this) {"""
new = """    fun command(command: String, payload: JSONObject = JSONObject()): JSONObject {
        synchronized(this) {"""
c = c.replace(old, new)

# Remove scene routes (simple one-liners)
import re
c = re.sub(r'                        get\("/v1/scenes"\) \{ call\.withCors\(\); call\.respondText\(sceneManager\.listScenesJson\(\), JsonContentType\) \}\n', '', c)
c = re.sub(r'                        post\("/v1/scenes/{sceneId}/load"\) \{ call\.withCors\(\); handleSceneLoad\(call\) \}\n', '', c)
c = re.sub(r'                        post\("/v1/scenes/{sceneId}/delete"\) \{ call\.withCors\(\); handleSceneDelete\(call\) \}\n', '', c)
c = re.sub(r'                        get\("/v1/events/{sceneId}"\) \{ call\.withCors\(\); handleEvents\(call\) \}\n', '', c)

# Remove store routes
c = re.sub(r'                        (?:get|post)\("/v1/store/[^"]*"\) \{ call\.withCors\(\); handleStore[A-Z][a-z]+\(call\) \}\n', '', c)

# Remove /api/chat route
c = re.sub(r'                        post\("/api/chat"\) \{ call\.withCors\(\); if \(!call\.authorized\(\)\) call\.respondUnauthorized\(\) else handleChat\(call, ChatProtocol\.OLLAMA\) \}\n', '', c)

# Remove agent prompt stream route
c = re.sub(r'                        post\("/v1/runtime/agent\.prompt\.stream"\) \{ call\.withCors\(\); if \(!call\.authorized\(\)\) call\.respondUnauthorized\(\) else handleAgentPromptStream\(call\) \}\n', '', c)

# Remove scene handler methods
for h in ['handleSceneLoad', 'handleSceneDelete', 'handleEvents']:
    c = re.sub(r'    private suspend fun ' + h + r'\(call: ApplicationCall\).*?\n    \}\n', '', c, flags=re.DOTALL)

# Remove store handlers
for h in ['handleStoreGet', 'handleStoreSet', 'handleStoreRemove', 'handleStoreList']:
    c = re.sub(r'    private suspend fun ' + h + r'\(call: ApplicationCall\).*?\n    \}\n', '', c, flags=re.DOTALL)

# Remove store helpers
c = re.sub(r'    private fun storeNamespace.*?\n    private fun storeKey.*?\n', '', c)

# Remove agent stream handlers
for h in ['handleAgentPromptStream', 'agentPromptStreamContent', 'writeAgentPromptStream', 'writeSse']:
    c = re.sub(r'    private (suspend )?fun ' + h + r'.*?\n    \}\n', '', c, flags=re.DOTALL)

# Remove acpAgentManager references in deleteSession
c = c.replace('            acpAgentManager.clearSessionIfMatches(deletedId)\n', '')

# Remove Ollama from enum
c = c.replace('        OPENAI("openai"),\n        OLLAMA("ollama"),', '        OPENAI("openai"),')

# Fix ChatProtocol references 
c = c.replace('ChatProtocol.OLLAMA', 'ChatProtocol.OPENAI')

open(p, 'w', encoding='utf-8').write(c)
print('OK')
