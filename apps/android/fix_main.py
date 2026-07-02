import re

p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\MainActivity.kt'
c = open(p, 'r', encoding='utf-8').read()

# Remove imports
c = c.replace('import com.mnnode.app.scene.SceneManager\n', '')
c = c.replace('import com.mnnode.app.scene.ScenePackInstaller\n', '')
c = c.replace('import com.mnnode.app.runtime.TriggerEngine\n', '')

# Remove fields
c = c.replace('    private lateinit var sceneManager: SceneManager\n', '')
c = c.replace('    private lateinit var scenePackInstaller: ScenePackInstaller\n', '')
c = c.replace('    private lateinit var triggerEngine: TriggerEngine\n', '')

# Remove init lines
c = c.replace('        sceneManager = MNNodeRuntime.sceneManager(this)\n', '')
c = c.replace('        scenePackInstaller = MNNodeRuntime.scenePackInstaller(this)\n', '')
c = c.replace('        triggerEngine = MNNodeRuntime.triggerEngine(this)\n', '')

# Remove scene path handler
c = c.replace('.addPathHandler("/installed-scenes/', '// removed .addPathHandler("/installed-scenes/')

# Remove shell commands
c = c.replace('"scene.install" -> { installScenePackage.launch(PACKAGE_MIME_TYPES); runtimeSummaryWithWindow() }\n', '')
c = c.replace('"scene.list" -> { runtimeSummaryWithWindow().put("scenes", apiServerController.command("status", payload).optJSONArray("scenes")) }\n', '')
c = c.replace('"scene.delete" -> { apiServerController.command("scene.delete", payload); runtimeSummaryWithWindow() }\n', '')

# Remove installScenePackage block
c = re.sub(r'    private val installScenePackage = registerForActivityResult.*?handleScenePackage\(uri\)\n    \}\n', '', c, flags=re.DOTALL)

# Remove handleScenePackage method
c = re.sub(r'    private fun handleScenePackage\(uri: Uri\).*?\n    \}\n', '', c, flags=re.DOTALL)

# Remove notifySceneInstallResult method
c = re.sub(r'    private fun notifySceneInstallResult.*?\n    \}\n', '', c, flags=re.DOTALL)

# Remove triggerEngine.snapshot
c = c.replace('            .put("triggers", triggerEngine.snapshot())\n', '')

# Remove agent commands
c = c.replace('"agent.status", "agent.saveNode", "agent.selectNode", "agent.connect", "agent.disconnect",\n            "agent.session.create", "agent.session.select", "agent.prompt" -> apiServerController.command(command, payload).withRuntimeState()\n', '')

open(p, 'w', encoding='utf-8').write(c)
print('OK')
