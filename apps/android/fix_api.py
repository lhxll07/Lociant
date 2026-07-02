import re

p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\server\ApiServerController.kt'
c = open(p, 'r', encoding='utf-8').read()

# Remove imports
c = c.replace('import com.mnnode.app.runtime.TriggerEngine\n', '')
c = c.replace('import com.mnnode.app.scene.SceneManager\n', '')

# Remove constructor params  
c = c.replace('    private val sceneManager: SceneManager,\n', '')
c = c.replace('    private val triggerEngine: TriggerEngine,\n', '')

# Remove scene routes
c = re.sub(r'                        get\("/v1/scenes"\) \{ call\.withCors\(\); call\.respondText\(sceneManager\.listScenesJson\(\), JsonContentType\) \}\n', '', c)
c = re.sub(r'                        post\("/v1/scenes/{sceneId}/load"\) \{ call\.withCors\(\); handleSceneLoad\(call\) \}\n', '', c)
c = re.sub(r'                        post\("/v1/scenes/{sceneId}/delete"\) \{ call\.withCors\(\); handleSceneDelete\(call\) \}\n', '', c)
c = re.sub(r'                        get\("/v1/events/{sceneId}"\) \{ call\.withCors\(\); handleEvents\(call\) \}\n', '', c)

# Remove scene handler methods
c = re.sub(r'    private suspend fun handleSceneLoad\(call: ApplicationCall\).*?\n    \}\n', '', c, flags=re.DOTALL)
c = re.sub(r'    private suspend fun handleSceneDelete\(call: ApplicationCall\).*?\n    \}\n', '', c, flags=re.DOTALL)
c = re.sub(r'    private suspend fun handleEvents\(call: ApplicationCall\).*?\n    \}\n', '', c, flags=re.DOTALL)
c = re.sub(r'    private suspend fun handlePreview\(call: ApplicationCall\).*?\n    \}\n', '', c, flags=re.DOTALL)
c = re.sub(r'    private suspend fun handlePreviewStream\(call: ApplicationCall\).*?\n        \}\n    \}\n', '', c, flags=re.DOTALL)

# Remove preview routes
c = re.sub(r'                        get\("/v1/preview"\) \{ call\.withCors\(\); handlePreview\(call\) \}\n', '', c)
c = re.sub(r'                        get\("/v1/preview/stream"\) \{ call\.withCors\(\); handlePreviewStream\(call\) \}\n', '', c)

open(p, 'w', encoding='utf-8').write(c)
print('DONE')
