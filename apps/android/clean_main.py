import re

# == Fix MainActivity.kt ==
p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\MainActivity.kt'
c = open(p, 'r', encoding='utf-8').read()

# Fix sceneManager path handler
c = c.replace('.addPathHandler("/removed-scenes/', '// .addPathHandler("/removed-scenes/')

# Fix VisionRuntime usage - add import back
if 'import com.mnnode.app.runtime.VisionRuntime' not in c:
    c = c.replace('import com.mnnode.app.runtime.DeviceInteraction', 'import com.mnnode.app.runtime.VisionRuntime\nimport com.mnnode.app.runtime.DeviceInteraction')

# Fix triggerEngine reference  
c = c.replace('triggerEngine', '/* triggerEngine removed */')

# Fix SceneManager references at line 185
c = c.replace('SceneManager', '/* SceneManager removed */')

# Fix handleScenePackage reference
c = c.replace('handleScenePackage', '/* handleScenePackage removed */')

# Fix notifySceneInstallResult reference
c = c.replace('notifySceneInstallResult', '/* notifySceneInstallResult removed */')

open(p, 'w', encoding='utf-8').write(c)

# == Fix MNNodeRuntime.kt ==
p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\runtime\MNNodeRuntime.kt'
c = open(p, 'r', encoding='utf-8').read()

# Remove any remaining sceneManager reference in the Holder construction
c = c.replace('                sceneManager = sceneManager,\n', '')
c = c.replace('                scenePackInstaller = scenePackInstaller,\n', '')
c = c.replace('                triggerEngine = triggerEngine,\n', '')
c = c.replace('                acpAgent = acpAgent,\n', '')
c = c.replace('                mnnRuntime = MnnRuntime(appContext),\n', '')

# Fix the mnnRuntime/chatCapability lines
c = c.replace('            val mnnRuntime = MnnRuntime(appContext)\n            val chatCapability = ChatCapability(modelManager, mnnRuntime)\n', '            val mnnRuntime = MnnRuntime(appContext)\n            val chatCapability = ChatCapability(modelManager, mnnRuntime)\n')

open(p, 'w', encoding='utf-8').write(c)

# == Fix ApiServerController.kt ==
p = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\app\src\main\java\com\mnnode\app\server\ApiServerController.kt'
c = open(p, 'r', encoding='utf-8').read()

# Fix the broken acpAgentManager comment replacements
# Find any line with "/* acpAgentManager removed */" followed by method calls
c = c.replace('/* acpAgentManager removed */.clearSessionIfMatches(deletedId)', '// acpAgentManager.clearSessionIfMatches removed')
c = c.replace('/* acpAgentManager removed */', 'null/* acpAgentManager removed */')

# Fix modelMarket comments  
c = c.replace('/* modelMarket removed */)', 'null/* modelMarket removed */')
c = c.replace('/* modelMarket removed */.', '/* modelMarket removed */')

open(p, 'w', encoding='utf-8').write(c)

print('ALL FIXED')
