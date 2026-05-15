const app = document.getElementById('app')
    const clock = document.getElementById('clock')
    const stateText = document.getElementById('stateText')
    const navItems = Array.from(document.querySelectorAll('.nav-item'))
    const panels = Array.from(document.querySelectorAll('.panel'))
    const sceneHost = document.getElementById('sceneHost')
    const sceneFrame = document.getElementById('sceneFrame')
    const sceneList = document.getElementById('sceneList')
    const modelList = document.getElementById('modelList')
    const modelProgress = document.getElementById('modelProgress')
    const modelProgressText = document.getElementById('modelProgressText')
    const modelProgressPercent = document.getElementById('modelProgressPercent')
    const modelProgressFill = document.getElementById('modelProgressFill')
    const installButton = document.getElementById('installButton')
    const reloadButton = document.getElementById('reloadButton')
    const modelReloadButton = document.getElementById('modelReloadButton')
    const modelImportButton = document.getElementById('modelImportButton')
    const modelMarketButton = document.getElementById('modelMarketButton')
    const backButton = document.getElementById('backButton')
    const sceneSettingsButton = document.getElementById('sceneSettingsButton')
    const runtimeStrip = document.getElementById('runtimeStrip')
    const runtimeSceneName = document.getElementById('runtimeSceneName')
    const runtimeEventText = document.getElementById('runtimeEventText')
    const runtimeStateText = document.getElementById('runtimeStateText')
    const runtimeElapsedText = document.getElementById('runtimeElapsedText')
    const alertBanner = document.getElementById('alertBanner')
    const alertSceneName = document.getElementById('alertSceneName')
    const alertTitle = document.getElementById('alertTitle')
    const alertMessage = document.getElementById('alertMessage')
    const alertOpenButton = document.getElementById('alertOpenButton')
    const alertCloseButton = document.getElementById('alertCloseButton')
    const settingsList = document.getElementById('settingsList')
    const runtimeSettingsButton = document.getElementById('runtimeSettingsButton')
    const runtimeSettingsState = document.getElementById('runtimeSettingsState')
    const runtimeSettingsPanel = document.getElementById('runtimeSettingsPanel')
    const runtimeSettingsBack = document.getElementById('runtimeSettingsBack')
    const runtimeServiceToggle = document.getElementById('runtimeServiceToggle')
    const runtimeServiceMessage = document.getElementById('runtimeServiceMessage')
    const runtimeWindowAutoInput = document.getElementById('runtimeWindowAutoInput')
    const runtimeWindowButton = document.getElementById('runtimeWindowButton')
    const runtimeWindowPermissionButton = document.getElementById('runtimeWindowPermissionButton')
    const runtimeCpuThreadsInput = document.getElementById('runtimeCpuThreadsInput')
    const runtimeModelButton = document.getElementById('runtimeModelButton')
    const runtimeModelState = document.getElementById('runtimeModelState')
    const runtimeModelPanel = document.getElementById('runtimeModelPanel')
    const runtimeModelBack = document.getElementById('runtimeModelBack')
    const runtimeModelList = document.getElementById('runtimeModelList')
    const runtimeModelNote = document.getElementById('runtimeModelNote')
    const batteryOptimizationText = document.getElementById('batteryOptimizationText')
    const batteryOptimizationButton = document.getElementById('batteryOptimizationButton')
    const languageControl = document.getElementById('languageControl')
    const diagService = document.getElementById('diagService')
    const diagApi = document.getElementById('diagApi')
    const diagUrl = document.getElementById('diagUrl')
    const diagCpuThreads = document.getElementById('diagCpuThreads')
    const diagDefaultTokens = document.getElementById('diagDefaultTokens')
    const diagModelTokens = document.getElementById('diagModelTokens')
    const diagEffectiveTokens = document.getElementById('diagEffectiveTokens')
    const diagError = document.getElementById('diagError')
    const toast = document.getElementById('toast')
    let scenes = []
    let activeScene = null
    let runtimeSnapshot = null
    let runtimeServiceState = null
    let runtimeModels = []
    let localeSetting = { mode: 'system' }
    let currentLocale = 'en'
    let activeAlert = null
    let apiServerPollTimer = null

    function native(method, ...args) {
      try {
        const bridge = window.MNNode
        if (bridge && typeof bridge[method] === 'function') return bridge[method](...args)
      } catch (error) {}
      return null
    }

    function nativeJson(method, fallback, ...args) {
      const raw = native(method, ...args)
      if (!raw) return fallback
      try { return JSON.parse(raw) } catch (error) { return fallback }
    }

    function el(tag, className, text) {
      const node = document.createElement(tag)
      if (className) node.className = className
      if (text !== undefined) node.textContent = text
      return node
    }

    function emptyCard(text) {
      return el('div', 'empty-card', text)
    }

    const i18n = {
      en: {
        'nav.scenes': 'Scenes',
        'nav.settings': 'Settings',
        'nav.models': 'Models',
        'nav.nodes': 'Nodes',
        'common.back': 'Back',
        'common.open': 'Open',
        'common.refresh': 'Refresh',
        'common.install': '+ Install',
        'state.idle': 'Idle',
        'state.running': 'Running',
        'state.background': 'Running in background',
        'settings.language': 'Language',
        'settings.languageSub': 'Follow system or choose a display language.',
        'settings.followSystem': 'System',
        'settings.runtimeTitle': 'Runtime & Background',
        'settings.runtimeSub': 'Foreground service, foreground window, and model server.',
        'settings.runtimeIntro': 'Control MNNode as a local AI node in the background.',
        'settings.modelServer': 'Model Server',
        'settings.runtimeDefaultMessage': 'Foreground service exposes the LAN API; foreground window keeps inference visible.',
        'settings.pipTitle': 'Runtime Window',
        'settings.pipSub': 'Show a small runtime window over other apps while the server runs.',
        'settings.windowAuto': 'Auto',
        'settings.windowShow': 'Show',
        'settings.windowHide': 'Hide',
        'settings.windowPermission': 'Permission',
        'settings.windowAllowed': 'Allowed',
        'settings.windowPermissionRequired': 'Floating window permission required',
        'settings.defaultModelTitle': 'Default Model',
        'settings.defaultModelSub': 'Choose the default model used by the model server.',
        'settings.defaultModelIntro': 'Pick the default model for OpenAI and Ollama requests that do not override model explicitly.',
        'settings.defaultModelNote': 'Only ready local models are shown here.',
        'settings.selected': 'Selected',
        'settings.cpuThreads': 'CPU Threads',
        'settings.cpuThreadsSub': 'Override the model config thread_num for MNN inference. Changing this reloads the model.',
        'settings.cpuThreadsShort': 'CPU Threads',
        'settings.outputTokens': 'Output Tokens',
        'settings.defaultTokens': 'Default',
        'settings.modelTokens': 'Model Cap',
        'settings.effectiveTokens': 'Effective Cap',
        'settings.battery': 'Battery Optimization',
        'settings.checking': 'Checking status...',
        'settings.runtimeScope': 'For stable inference, use the floating window while the screen is on. Locked-screen execution still depends on system policy.',
        'settings.batteryAllowed': 'Allowed. Background runtime is less likely to be stopped by the system.',
        'settings.batteryRestricted': 'Restricted. Some Android vendors may stop background runtime aggressively.',
        'settings.allowed': 'Allowed',
        'settings.allow': 'Allow',
        'common.start': 'Start',
        'common.stop': 'Stop',
        'status.starting': 'Starting',
        'status.running': 'Running',
        'status.stopped': 'Stopped',
        'models.rescan': 'Rescan',
        'models.import': 'Import',
        'models.market': 'Model Market · Soon',
        'nodes.placeholder': 'Multi-node discovery, connection status, and collaborative tasks will appear here. This version keeps the page as a placeholder.',
        'toast.modelsReloaded': 'Model list refreshed',
        'toast.modelMarketSoon': 'Model Market will be available in a later version',
        'toast.scenesReloaded': 'Scene list refreshed',
        'toast.sceneInstalled': 'Scene pack installed',
        'toast.installFailed': 'Install failed',
        'toast.modelImported': 'Model imported',
        'toast.modelImportFailed': 'Model import failed',
        'empty.scenes': 'No available Scene Packs',
        'empty.models': 'No available models',
        'models.delete': 'Delete',
        'toast.modelDeleted': 'Model deleted',
        'toast.modelDeleteFailed': 'Model delete failed',
        'toast.sceneUninstalled': 'Scene Pack uninstalled',
        'toast.sceneUninstallFailed': 'Uninstall failed',
        'toast.cameraRequested': 'Camera request received',
        'toast.visionRequested': 'Vision request received',
        'toast.cameraStarted': 'Camera preview started',
        'toast.visionStarted': 'Vision analysis started'
      },
      zh: {
        'nav.scenes': '场景',
        'nav.settings': '设置',
        'nav.models': '模型',
        'nav.nodes': '多节点',
        'common.back': '返回',
        'common.open': '打开',
        'common.refresh': '刷新',
        'common.install': '+ 安装',
        'state.idle': '待机',
        'state.running': '运行中',
        'state.background': '后台运行中',
        'settings.language': '语言',
        'settings.languageSub': '跟随系统，或手动选择显示语言。',
        'settings.followSystem': '系统',
        'settings.runtimeTitle': 'Runtime 与后台运行',
        'settings.runtimeSub': '前台服务、前台小窗和模型服务',
        'settings.runtimeIntro': '控制 MNNode 作为本地 AI 节点在后台运行。',
        'settings.modelServer': 'Model Server',
        'settings.runtimeDefaultMessage': '前台服务暴露局域网 API；前台小窗用于保持推理可见运行。',
        'settings.pipTitle': 'Runtime 小窗',
        'settings.pipSub': '运行服务时在其他 App 上方显示 runtime 小窗。',
        'settings.windowAuto': '自动',
        'settings.windowShow': '显示',
        'settings.windowHide': '隐藏',
        'settings.windowPermission': '权限',
        'settings.windowAllowed': '已允许',
        'settings.windowPermissionRequired': '需要悬浮窗权限',
        'settings.defaultModelTitle': '默认模型',
        'settings.defaultModelSub': '选择 model-server 默认使用的模型。',
        'settings.defaultModelIntro': '这里设置的是 OpenAI 和 Ollama 请求在未显式指定 model 时所使用的默认模型。',
        'settings.defaultModelNote': '这里只显示已就绪的本地模型。',
        'settings.selected': '当前',
        'settings.cpuThreads': 'CPU 线程数',
        'settings.cpuThreadsSub': '覆盖模型 config.json 的 thread_num。修改后会重新加载模型。',
        'settings.cpuThreadsShort': 'CPU 线程',
        'settings.outputTokens': '输出 Tokens',
        'settings.defaultTokens': '默认值',
        'settings.modelTokens': '模型上限',
        'settings.effectiveTokens': '生效上限',
        'settings.battery': '电池优化',
        'settings.checking': '正在检查状态...',
        'settings.runtimeScope': '稳定推理建议在亮屏时使用悬浮小窗。锁屏继续运行仍取决于系统策略。',
        'settings.batteryAllowed': '已允许。后台 runtime 更不容易被系统停止。',
        'settings.batteryRestricted': '受限制。部分 Android 厂商系统可能会积极停止后台 runtime。',
        'settings.allowed': '已允许',
        'settings.allow': '允许',
        'common.start': '启动',
        'common.stop': '停止',
        'status.starting': '启动中',
        'status.running': '运行中',
        'status.stopped': '已停止',
        'models.rescan': '重新扫描',
        'models.import': '导入',
        'models.market': '模型市场 · Soon',
        'nodes.placeholder': '多节点发现、连接状态和协同任务会放在这里。当前版本仅保留界面占位。',
        'toast.modelsReloaded': '模型列表已刷新',
        'toast.modelMarketSoon': '模型市场会在后续版本开放',
        'toast.scenesReloaded': '场景列表已刷新',
        'toast.sceneInstalled': '场景包已安装',
        'toast.installFailed': '安装失败',
        'toast.modelImported': '模型已导入',
        'toast.modelImportFailed': '模型导入失败',
        'empty.scenes': '暂无可用场景包',
        'empty.models': '暂无可用模型',
        'models.delete': '删除',
        'toast.modelDeleted': '模型已删除',
        'toast.modelDeleteFailed': '模型删除失败',
        'toast.sceneUninstalled': '已卸载场景包',
        'toast.sceneUninstallFailed': '卸载失败',
        'toast.cameraRequested': 'Camera request received',
        'toast.visionRequested': 'Vision request received',
        'toast.cameraStarted': 'Camera preview started',
        'toast.visionStarted': 'Vision analysis started'
      }
    }

    function systemLocale() {
      return ((navigator.language || '').toLowerCase().startsWith('zh')) ? 'zh' : 'en'
    }

    function resolveLocale(setting) {
      return setting && setting.mode && setting.mode !== 'system' ? setting.mode : systemLocale()
    }

    function t(key) {
      return (i18n[currentLocale] && i18n[currentLocale][key]) || (i18n.en && i18n.en[key]) || key
    }

    let cameraPreviewRect = null

    function resizeSceneFrame() {
      try {
        const doc = sceneFrame.contentDocument || sceneFrame.contentWindow.document
        if (!doc) return
        const height = Math.max(
          sceneHost.clientHeight,
          doc.documentElement.scrollHeight,
          doc.body ? doc.body.scrollHeight : 0
        )
        sceneFrame.style.height = height + 'px'
        syncCameraPreviewRect()
      } catch (error) {
        sceneFrame.style.height = '100%'
      }
    }

    function syncCameraPreviewRect(rect) {
      if (rect) cameraPreviewRect = rect
      if (!cameraPreviewRect || !sceneHost.classList.contains('active')) return

      const frameRect = sceneFrame.getBoundingClientRect()
      const cssX = Math.max(0, frameRect.left + cameraPreviewRect.x)
      const cssY = Math.max(0, frameRect.top + cameraPreviewRect.y - sceneHost.scrollTop)
      const scale = nativeViewportScale()
      const x = Math.round(cssX * scale.x)
      const y = Math.round(cssY * scale.y)
      const width = Math.max(1, Math.round(cameraPreviewRect.width * scale.x))
      const height = Math.max(1, Math.round(cameraPreviewRect.height * scale.y))
      native('setCameraPreviewRect', x, y, width, height)
    }

    function nativeViewportScale() {
      const cssWidth = Math.max(1, document.documentElement.clientWidth || window.innerWidth || 1)
      const cssHeight = Math.max(1, document.documentElement.clientHeight || window.innerHeight || 1)
      const metrics = nativeJson('getViewportMetrics', null)
      if (metrics && metrics.width > 0 && metrics.height > 0) {
        return { x: metrics.width / cssWidth, y: metrics.height / cssHeight }
      }
      const ratio = window.devicePixelRatio || 1
      return { x: ratio, y: ratio }
    }

    function loadLocaleSetting() {
      const result = nativeJson('storeGet', null, 'runtime/settings', 'locale')
      localeSetting = result && result.ok && result.value ? result.value : { mode: 'system' }
      applyLocale()
    }

    function saveLocaleSetting(mode) {
      localeSetting = { mode: mode || 'system' }
      native('storeSet', 'runtime/settings', 'locale', JSON.stringify(localeSetting))
      applyLocale()
    }

    function applyLocale() {
      currentLocale = resolveLocale(localeSetting)
      document.documentElement.lang = currentLocale === 'zh' ? 'zh-CN' : 'en'
      document.querySelectorAll('[data-i18n]').forEach(node => {
        node.textContent = t(node.dataset.i18n)
      })
      Array.from(languageControl.querySelectorAll('.segmented-option')).forEach(button => {
        button.classList.toggle('active', button.dataset.langMode === (localeSetting.mode || 'system'))
      })
      updateRuntimeServiceState(runtimeServiceState || {})
      broadcastLocale()
    }

    function broadcastLocale() {
      postToScene({
        type: 'runtime.locale',
        language: currentLocale,
        mode: localeSetting.mode || 'system',
        fallback: 'en'
      })
    }

    function startCameraFromScene(rect) {
      syncCameraPreviewRect(rect)
      if (!native('startCamera')) showToast('Camera start failed')
    }

    function stopCamera() {
      native('stopCamera')
      postToScene({ type: 'camera.stop' })
    }

    function startVisionFromScene(config) {
      stopCamera()
      try {
        const payload = JSON.stringify(config || { modelId: 'yolov8n', backend: 'auto' })
        native('startVision', payload)
      } catch (error) {
        showToast('Vision start failed')
      }
    }

    function stopVision(options) {
      const notifyScene = !(options && options.notifyScene === false)
      native('stopVision')
      if (notifyScene) {
        postToScene({ type: 'vision.state', state: 'idle' })
      }
    }

    function unloadSceneFrame() {
      postToScene({ type: 'scene.dispose', keepVision: true })
      sceneFrame.removeAttribute('src')
      sceneFrame.style.height = '100%'
    }

    function fmtDuration(ms) {
      const total = Math.max(0, Math.floor((Number(ms) || 0) / 1000))
      const h = Math.floor(total / 3600)
      const m = Math.floor((total % 3600) / 60)
      const s = total % 60
      if (h > 0) return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
      return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
    }

    function runtimeScene() {
      if (!runtimeSnapshot || !runtimeSnapshot.sceneId) return null
      return scenes.find(scene => scene.id === runtimeSnapshot.sceneId) || null
    }

    function runtimeStateLabel(state) {
      if (state === 'away') return 'Away'
      if (state === 'distracted' || state === 'phone') return 'Distracted'
      if (state === 'focused') return 'Focused'
      return state || t('state.idle')
    }

    function sceneById(sceneId) {
      return scenes.find(scene => scene.id === sceneId) || null
    }

    function updateRuntimeStrip() {
      const scene = runtimeScene()
      const visible = !!(runtimeSnapshot && runtimeSnapshot.active && scene && !activeScene)
      runtimeStrip.classList.toggle('visible', visible)
      runtimeStrip.setAttribute('aria-hidden', visible ? 'false' : 'true')
      if (!visible) return

      const events = Array.isArray(runtimeSnapshot.events) ? runtimeSnapshot.events : []
      const latest = events[0]
      runtimeSceneName.textContent = scene.name || scene.id
      runtimeStateText.textContent = runtimeStateLabel(runtimeSnapshot.state)
      runtimeElapsedText.textContent = fmtDuration(runtimeSnapshot.elapsedMs)
      runtimeEventText.textContent = latest
        ? ((latest.name || latest.ruleId || 'event') + ' · ' + new Date(latest.timestamp || Date.now()).toLocaleTimeString('zh-CN', { hour12: false }))
        : ((runtimeSnapshot.running || runtimeSnapshot.sessionState === 'running') ? t('state.background') : 'Paused')
    }

    function showAlert(event) {
      const alert = event && event.alert
      if (!alert) return
      const scene = sceneById(event.sceneId)
      activeAlert = { event, alert }
      alertSceneName.textContent = (scene ? scene.name : event.sceneId || 'Scene') + ' · ' + (alert.level || 'alert')
      alertTitle.textContent = alert.title || event.name || 'Alert'
      alertMessage.textContent = alert.message || 'A scene alert was triggered.'
      alertBanner.classList.add('visible')
      alertBanner.setAttribute('aria-hidden', 'false')
    }

    function clearAlert() {
      activeAlert = null
      alertBanner.classList.remove('visible')
      alertBanner.setAttribute('aria-hidden', 'true')
    }

    function openAlertScene() {
      const sceneId = activeAlert && activeAlert.event && activeAlert.event.sceneId
      const scene = sceneById(sceneId)
      if (scene) openScene(scene)
      clearAlert()
    }

    function sceneHasSettings(scene) {
      const capabilities = Array.isArray(scene && scene.capabilities) ? scene.capabilities : []
      return capabilities.includes('settings') || capabilities.includes('vision-settings')
    }

    function setSceneSettingsVisible(visible) {
      sceneSettingsButton.classList.toggle('visible', !!visible)
    }

    function postToScene(message) {
      try {
        if (!sceneFrame.contentWindow) return false
        sceneFrame.contentWindow.postMessage(message, '*')
        return true
      } catch (error) {
        return false
      }
    }

    const reliableTimers = new Map()

    function postToSceneReliable(message, attempts = 6, key = '') {
      if (!activeScene) return
      if (key && reliableTimers.has(key)) {
        reliableTimers.get(key).forEach(timer => window.clearTimeout(timer))
        reliableTimers.delete(key)
      }
      let count = 0
      const timers = []
      const send = () => {
        count += 1
        postToScene(message)
        if (count < attempts) {
          timers.push(window.setTimeout(send, count === 1 ? 80 : 220))
        } else if (key) {
          reliableTimers.delete(key)
        }
      }
      if (key) reliableTimers.set(key, timers)
      send()
    }

    function toggleSceneSettings() {
      if (!activeScene || !sceneHasSettings(activeScene)) return
      postToScene({ type: 'scene.settings.toggle', sceneId: activeScene.id })
    }

    function modelChatFromScene(request) {
      try {
        const payload = Object.assign({}, request || {}, {
          sceneId: (request && request.sceneId) || (activeScene && activeScene.id) || ''
        })
        native('modelChat', JSON.stringify(payload))
      } catch (error) {
        postToScene({
          type: 'model.chat.result',
          ok: false,
          requestId: request && request.requestId,
          message: 'Model chat request failed'
        })
      }
    }

    function postApiServerState(state, reliable = true) {
      const message = Object.assign({ type: 'api.server.state' }, state || {})
      if (reliable) postToSceneReliable(message, 3, 'api.server.state')
      else postToScene(message)
      scheduleApiServerPoll(message)
    }

    function scheduleApiServerPoll(state) {
      window.clearTimeout(apiServerPollTimer)
      if (!activeScene || activeScene.id !== 'model-server') return
      const delay = state && state.starting ? 350 : (state && state.running ? 1200 : 0)
      if (delay > 0) {
        apiServerPollTimer = window.setTimeout(() => runtimeApiCommand('status', { sceneId: 'model-server' }), delay)
      }
    }

    function runtimeApiCommand(command, payload) {
      try {
        const body = Object.assign({}, payload || {}, {
          sceneId: (payload && payload.sceneId) || (activeScene && activeScene.id) || ''
        })
        const raw = native('runtimeServiceCommand', command, JSON.stringify(body))
        const state = raw ? JSON.parse(raw) : { running: false, message: 'API server bridge unavailable' }
        postApiServerState(state, command !== 'settings')
        if (raw) updateRuntimeServiceState(state)
      } catch (error) {
        postApiServerState({ running: false, message: 'API server command failed' })
      }
    }

    function runtimeServiceCommand(command, payload) {
      try {
        const raw = native('runtimeServiceCommand', command, JSON.stringify(payload || {}))
        updateRuntimeServiceState(raw ? JSON.parse(raw) : { running: false, message: 'Runtime service bridge unavailable' })
      } catch (error) {
        updateRuntimeServiceState({ running: false, message: 'Runtime service command failed' })
      }
    }

    function updateRuntimeServiceState(state) {
      runtimeServiceState = state || runtimeServiceState || {}
      const running = !!runtimeServiceState.running
      const starting = !!runtimeServiceState.starting
      runtimeSettingsState.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
      runtimeSettingsState.classList.toggle('running', running || starting)
      runtimeServiceToggle.textContent = running || starting ? t('common.stop') : t('common.start')
      runtimeServiceMessage.textContent = runtimeServiceState.message || t('settings.runtimeDefaultMessage')
      runtimeWindowAutoInput.checked = !!runtimeServiceState.windowAutoShow
      runtimeWindowAutoInput.disabled = !runtimeServiceState.windowSupported
      runtimeWindowButton.disabled = !runtimeServiceState.windowSupported
      runtimeWindowButton.textContent = runtimeServiceState.windowVisible ? t('settings.windowHide') : t('settings.windowShow')
      runtimeWindowPermissionButton.disabled = !runtimeServiceState.windowSupported || !!runtimeServiceState.windowAllowed
      runtimeWindowPermissionButton.textContent = runtimeServiceState.windowAllowed ? t('settings.windowAllowed') : t('settings.windowPermission')
      const maxCpuThreads = Number(runtimeServiceState.maxCpuThreads) || 16
      const cpuThreads = Number(runtimeServiceState.cpuThreads) || 4
      runtimeCpuThreadsInput.max = String(maxCpuThreads)
      if (document.activeElement !== runtimeCpuThreadsInput) runtimeCpuThreadsInput.value = String(cpuThreads)
      const modelSuffix = runtimeServiceState.modelLoading ? ' · loading' : (runtimeServiceState.modelLoaded ? ' · loaded' : '')
      runtimeModelState.textContent = (runtimeServiceState.modelId || runtimeModelState.textContent || '--') + modelSuffix
      diagService.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
      diagApi.textContent = running ? t('status.running') : t('status.stopped')
      diagUrl.textContent = runtimeServiceState.lanUrl || runtimeServiceState.url || '--'
      diagCpuThreads.textContent = cpuThreads + ' / ' + maxCpuThreads
      diagDefaultTokens.textContent = runtimeServiceState.defaultOutputTokens !== undefined && runtimeServiceState.defaultOutputTokens !== null
        ? String(runtimeServiceState.defaultOutputTokens)
        : '--'
      diagModelTokens.textContent = runtimeServiceState.modelMaxOutputTokens !== undefined && runtimeServiceState.modelMaxOutputTokens !== null
        ? String(runtimeServiceState.modelMaxOutputTokens)
        : '--'
      diagEffectiveTokens.textContent = runtimeServiceState.effectiveMaxOutputTokens !== undefined && runtimeServiceState.effectiveMaxOutputTokens !== null
        ? String(runtimeServiceState.effectiveMaxOutputTokens)
        : String(runtimeServiceState.maxOutputTokens || '--')
      const overlayHint = runtimeServiceState.windowSupported && !runtimeServiceState.windowAllowed
        ? t('settings.windowPermissionRequired')
        : ''
      diagError.textContent = overlayHint || (runtimeServiceState.lastError && runtimeServiceState.lastError !== null ? String(runtimeServiceState.lastError) : '--')
      const batteryAllowed = !!runtimeServiceState.batteryOptimizationIgnored
      batteryOptimizationText.textContent = batteryAllowed
        ? t('settings.batteryAllowed')
        : t('settings.batteryRestricted')
      batteryOptimizationButton.textContent = batteryAllowed ? t('settings.allowed') : t('settings.allow')
      batteryOptimizationButton.disabled = batteryAllowed
      renderRuntimeModelChoices(runtimeModels)
    }

    function refreshRuntimeServiceState() {
      runtimeServiceCommand('status', {})
    }

    function openRuntimeSettings() {
      settingsList.classList.add('hidden')
      runtimeSettingsPanel.classList.add('active')
      runtimeSettingsPanel.setAttribute('aria-hidden', 'false')
      runtimeModelPanel.classList.remove('active')
      runtimeModelPanel.setAttribute('aria-hidden', 'true')
      refreshRuntimeServiceState()
      loadModels()
    }

    function closeRuntimeSettings() {
      settingsList.classList.remove('hidden')
      runtimeSettingsPanel.classList.remove('active')
      runtimeSettingsPanel.setAttribute('aria-hidden', 'true')
    }

    function openRuntimeModelSettings() {
      settingsList.classList.add('hidden')
      runtimeSettingsPanel.classList.remove('active')
      runtimeSettingsPanel.setAttribute('aria-hidden', 'true')
      runtimeModelPanel.classList.add('active')
      runtimeModelPanel.setAttribute('aria-hidden', 'false')
      loadModels()
      refreshRuntimeServiceState()
    }

    function closeRuntimeModelSettings() {
      settingsList.classList.add('hidden')
      runtimeModelPanel.classList.remove('active')
      runtimeModelPanel.setAttribute('aria-hidden', 'true')
      runtimeSettingsPanel.classList.add('active')
      runtimeSettingsPanel.setAttribute('aria-hidden', 'false')
    }

    function activateRuntime(scene) {
      if (!scene || !scene.runtime) return
      if (runtimeSnapshot && runtimeSnapshot.sceneId === scene.id && runtimeSnapshot.active) {
        updateRuntimeStrip()
        postToSceneReliable(runtimeSnapshot)
        return
      }
      try {
        runtimeSnapshot = nativeJson('activateSceneRuntime', null, scene.id)
        updateRuntimeStrip()
        if (runtimeSnapshot && runtimeSnapshot.active) {
          postToSceneReliable(runtimeSnapshot)
        }
      } catch (error) {}
    }

    function syncRuntimeSnapshot(options) {
      const notifyScene = !(options && options.notifyScene === false)
      try {
        runtimeSnapshot = nativeJson('getRuntimeSnapshot', runtimeSnapshot)
        updateRuntimeStrip()
        if (notifyScene && runtimeSnapshot && runtimeSnapshot.active) postToSceneReliable(runtimeSnapshot)
      } catch (error) {}
    }

    function handleRuntimeMessage(message) {
      if (!message || !message.type) return
      if (message.sceneId && activeScene && message.sceneId !== activeScene.id) return
      if (message.type === 'runtime.snapshot') runtimeSnapshot = message
      if (message.type === 'runtime.state' && runtimeSnapshot) {
        runtimeSnapshot.state = message.state
        runtimeSnapshot.stateStartedAt = message.stateStartedAt
        runtimeSnapshot.sessionState = message.sessionState || runtimeSnapshot.sessionState
        runtimeSnapshot.elapsedMs = message.elapsedMs || runtimeSnapshot.elapsedMs
        runtimeSnapshot.durations = message.durations || runtimeSnapshot.durations
      }
      if (message.type === 'runtime.event' && runtimeSnapshot && message.event) {
        const events = Array.isArray(runtimeSnapshot.events) ? runtimeSnapshot.events : []
        runtimeSnapshot.events = [message.event].concat(events).slice(0, 50)
      }
      updateRuntimeStrip()

      if (message.type === 'runtime.state' && activeScene) {
        stateText.textContent = activeScene.name + ' · ' + message.state
      }
      if (message.type === 'runtime.event' && message.event) {
        showAlert(message.event)
        showToast((activeScene ? activeScene.name + ' · ' : '') + message.event.name)
      }
      postToScene(message)
    }

    function sendRuntimeCommand(sceneId, command, payload) {
      try {
        const snapshot = nativeJson('runtimeCommand', null, sceneId, command, JSON.stringify(payload || {}))
        if (snapshot) handleRuntimeMessage(snapshot)
      } catch (error) {
        showToast('Runtime command failed')
      }
    }

    sceneFrame.addEventListener('load', () => {
      resizeSceneFrame()
      window.setTimeout(resizeSceneFrame, 80)
      window.setTimeout(resizeSceneFrame, 300)
      window.setTimeout(syncRuntimeSnapshot, 120)
    })

    window.addEventListener('resize', resizeSceneFrame)
    sceneHost.addEventListener('scroll', () => syncCameraPreviewRect(), { passive: true })
    app.addEventListener('transitionend', () => syncCameraPreviewRect())

    const menuButton = document.getElementById('menuButton')
    let sidebarBusy = false

    function clearPress(target) {
      if (target) target.classList.remove('is-pressed')
    }

    function toggleSidebar(event) {
      if (event) event.preventDefault()
      if (sidebarBusy) return
      sidebarBusy = true
      menuButton.classList.add('is-busy')
      app.classList.toggle('collapsed')
      window.setTimeout(() => {
        sidebarBusy = false
        menuButton.classList.remove('is-busy')
      }, 290)
    }

    menuButton.addEventListener('pointerdown', event => {
      event.preventDefault()
      menuButton.classList.add('is-pressed')
      toggleSidebar(event)
    })
    menuButton.addEventListener('pointerup', () => clearPress(menuButton))
    menuButton.addEventListener('pointercancel', () => clearPress(menuButton))
    menuButton.addEventListener('mouseleave', () => clearPress(menuButton))

    document.addEventListener('pointerdown', event => {
      const target = event.target.closest('.pressable')
      if (target && target !== menuButton) target.classList.add('is-pressed')
    }, { passive: true })

    document.addEventListener('pointerup', event => {
      const target = event.target.closest('.pressable')
      clearPress(target)
    }, { passive: true })

    document.addEventListener('pointercancel', event => {
      const target = event.target.closest('.pressable')
      clearPress(target)
    }, { passive: true })

    navItems.forEach(item => {
      item.addEventListener('click', () => {
        const page = item.dataset.page
        navItems.forEach(i => i.classList.toggle('active', i === item))
        stopCamera()
        unloadSceneFrame()
        sceneHost.classList.remove('active')
        backButton.classList.remove('active')
        activeScene = null
        setSceneSettingsVisible(false)
        panels.forEach(panel => panel.classList.toggle('active', panel.id === 'page-' + page))
        const activePanel = document.getElementById('page-' + page)
        if (activePanel) activePanel.scrollTop = 0
        if (page === 'models') loadModels()
        stateText.textContent = t('state.idle')
        updateRuntimeStrip()
      })
    })

    function showToast(text) {
      toast.textContent = text
      toast.classList.add('active')
      window.clearTimeout(showToast.timer)
      showToast.timer = window.setTimeout(() => toast.classList.remove('active'), 2200)
    }

    function sceneIconSvg() {
      return '<svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H20v17H7.5A3.5 3.5 0 0 0 4 22V5.5z"/><path d="M4 5.5A3.5 3.5 0 0 1 7.5 9H20"/><path d="M8 13h7"/><path d="M8 16h5"/></svg>'
    }

    function loadScenes() {
      scenes = nativeJson('getScenes', [])
      renderScenes()
    }

    function renderScenes() {
      sceneList.innerHTML = ''
      updateRuntimeStrip()
      if (activeAlert) showAlert(activeAlert.event)
      if (!scenes.length) {
        sceneList.appendChild(emptyCard(t('empty.scenes')))
        return
      }

      scenes.forEach(scene => {
        const card = document.createElement('button')
        card.className = 'scene-card pressable'
        card.type = 'button'
        card.dataset.sceneId = scene.id

        const icon = document.createElement('div')
        icon.className = 'scene-icon'
        icon.innerHTML = sceneIconSvg()

        const name = el('div', 'scene-name', scene.name || scene.id)

        const text = document.createElement('div')
        const desc = el('div', 'scene-desc', 'v' + (scene.version || '0.0.0'))
        text.appendChild(name)
        text.appendChild(desc)

        const manage = document.createElement('div')
        manage.className = 'scene-manage'

        const source = el('span', 'scene-source', scene.source || 'scene')
        manage.appendChild(source)

        if (scene.source === 'installed') {
          const uninstall = document.createElement('button')
          uninstall.className = 'scene-mini-button pressable'
          uninstall.type = 'button'
          uninstall.textContent = t('models.delete')
          uninstall.addEventListener('click', event => {
            event.stopPropagation()
            uninstallScene(scene)
          })
          manage.appendChild(uninstall)
        }

        card.appendChild(icon)
        card.appendChild(text)
        card.appendChild(manage)
        card.addEventListener('click', () => openScene(scene))
        sceneList.appendChild(card)
      })
    }

    function modelIconSvg() {
      return '<svg viewBox="0 0 24 24"><rect x="5" y="5" width="14" height="14" rx="2"/><path d="M9 2v3"/><path d="M15 2v3"/><path d="M9 19v3"/><path d="M15 19v3"/><path d="M2 9h3"/><path d="M2 15h3"/><path d="M19 9h3"/><path d="M19 15h3"/><path d="M9 9h6v6H9z"/></svg>'
    }

    function loadModels() {
      let models = nativeJson('getModels', [])
      runtimeModels = Array.isArray(models) ? models.slice() : []
      renderModels(models)
    }

    function renderModels(models) {
      modelList.innerHTML = ''
      if (!models.length) {
        modelList.appendChild(emptyCard(t('empty.models')))
        renderRuntimeModelChoices(runtimeModels)
        return
      }

      models.forEach(model => {
        const row = document.createElement('div')
        row.className = 'model-row'

        const icon = document.createElement('div')
        icon.className = 'model-icon'
        icon.innerHTML = modelIconSvg()

        const body = document.createElement('div')
        const name = el('div', 'model-name', model.name || model.id)

        const meta = el('div', 'model-meta')
        const missing = Array.isArray(model.missingFiles) && model.missingFiles.length
          ? ' · missing ' + model.missingFiles.slice(0, 2).join(', ')
          : ''
        meta.textContent = [model.runtime, model.type, model.source].filter(Boolean).join(' · ') + ' · ' + (model.path || 'not installed') + missing

        body.appendChild(name)
        body.appendChild(meta)

        const actions = document.createElement('div')
        actions.className = 'model-actions'

        const status = el('div', 'model-status ' + (model.ready ? 'ready' : 'missing'), model.ready ? 'Ready' : (model.installed ? 'Incomplete' : 'Missing'))
        actions.appendChild(status)

        if (model.source === 'external' && model.installed) {
          const remove = document.createElement('button')
          remove.className = 'model-delete-button pressable'
          remove.type = 'button'
          remove.textContent = t('models.delete')
          remove.addEventListener('click', event => {
            event.stopPropagation()
            deleteModel(model)
          })
          actions.appendChild(remove)
        }

        row.appendChild(icon)
        row.appendChild(body)
        row.appendChild(actions)
        modelList.appendChild(row)
      })

      renderRuntimeModelChoices(runtimeModels)
    }

    function renderRuntimeModelChoices(models) {
      if (!runtimeModelList) return
      runtimeModelList.innerHTML = ''

      const readyModels = (Array.isArray(models) ? models : []).filter(model => model && model.ready && isRuntimeModel(model))
      const currentModelId = (runtimeServiceState && runtimeServiceState.modelId) || (readyModels[0] && readyModels[0].id) || '--'
      runtimeModelState.textContent = currentModelId
      runtimeModelNote.textContent = readyModels.length ? t('settings.defaultModelNote') : t('empty.models')

      if (!readyModels.length) {
        runtimeModelList.appendChild(emptyCard(t('empty.models')))
        return
      }

      readyModels.forEach(model => {
        const row = document.createElement('button')
        row.type = 'button'
        row.className = 'model-choice-row pressable'
        row.classList.toggle('active', (runtimeServiceState && runtimeServiceState.modelId) === model.id)

        const body = document.createElement('div')
        body.className = 'model-choice-body'
        const name = el('div', 'model-choice-title', model.name || model.id)
        const meta = el('div', 'model-choice-sub')
        meta.textContent = [model.runtime, model.type].filter(Boolean).join(' · ')
        body.appendChild(name)
        body.appendChild(meta)

        const state = el('span', 'settings-row-state', (runtimeServiceState && runtimeServiceState.modelId) === model.id ? t('settings.selected') : model.id)

        row.appendChild(body)
        row.appendChild(state)
        row.addEventListener('click', () => {
          runtimeApiCommand('settings', { sceneId: 'model-server', modelId: model.id })
          runtimeModelState.textContent = model.id
          showToast(model.name || model.id)
        })
        runtimeModelList.appendChild(row)
      })
    }

    function isRuntimeModel(model) {
      return model && (model.runtime === 'mnn' || model.type === 'vlm' || model.type === 'chat' || model.type === 'llm')
    }

    function deleteModel(model) {
      if (!model || model.source !== 'external') return
      const name = model.name || model.id
      if (!window.confirm('删除模型「' + name + '」？')) return

      try {
        const result = nativeJson('deleteModel', { ok: false }, model.id)
        if (result.ok) {
          showToast(t('toast.modelDeleted'))
          loadModels()
        } else {
          showToast(result.message || t('toast.modelDeleteFailed'))
        }
      } catch (error) {
        showToast(t('toast.modelDeleteFailed'))
      }
    }

    function setModelProgress(result) {
      window.clearTimeout(setModelProgress.timer)

      if (!result || result.state === 'idle') {
        modelProgress.classList.remove('active', 'indeterminate')
        modelProgressFill.style.width = '0%'
        modelProgressPercent.textContent = ''
        return
      }

      const progress = Number(result.progress)
      const hasProgress = Number.isFinite(progress)
      modelProgress.classList.add('active')
      modelProgress.classList.toggle('indeterminate', !hasProgress && result.state === 'installing')
      modelProgressText.textContent = result.message || (result.state === 'installing' ? '模型正在导入' : '模型导入完成')

      if (hasProgress) {
        const percent = Math.max(0, Math.min(100, Math.round(progress * 100)))
        modelProgressFill.style.width = percent + '%'
        modelProgressPercent.textContent = percent + '%'
      } else {
        modelProgressFill.style.width = ''
        modelProgressPercent.textContent = ''
      }

      if (result.state === 'done' || result.state === 'error') {
        setModelProgress.timer = window.setTimeout(() => setModelProgress({ state: 'idle' }), 1800)
      }
    }

    function goHome() {
      window.clearTimeout(apiServerPollTimer)
      stopCamera()
      unloadSceneFrame()
      sceneHost.classList.remove('active')
      backButton.classList.remove('active')
      activeScene = null
      setSceneSettingsVisible(false)
      panels.forEach(panel => panel.classList.toggle('active', panel.id === 'page-scenes'))
      navItems.forEach(item => item.classList.toggle('active', item.dataset.page === 'scenes'))
      stateText.textContent = runtimeSnapshot && runtimeSnapshot.running ? t('state.background') : t('state.idle')
      updateRuntimeStrip()
    }

    function openScene(scene) {
      if (!scene || !scene.entryUrl) return
      panels.forEach(panel => panel.classList.remove('active'))
      backButton.classList.add('active')
      activeScene = scene
      setSceneSettingsVisible(sceneHasSettings(scene))
      sceneHost.classList.add('active')
      stopCamera()
      sceneHost.scrollTop = 0
      cameraPreviewRect = null
      sceneFrame.style.height = '100%'
      sceneFrame.src = scene.entryUrl
      activateRuntime(scene)
      if (scene.id === 'model-server') {
        window.clearTimeout(apiServerPollTimer)
        apiServerPollTimer = window.setTimeout(() => runtimeApiCommand('status', { sceneId: scene.id }), 180)
      }
      stateText.textContent = t('state.running')
      updateRuntimeStrip()
    }

    function uninstallScene(scene) {
      if (!scene || scene.source !== 'installed') return
      try {
        const result = nativeJson('uninstallScene', { ok: false }, scene.id)
        if (result.ok) {
          showToast(t('toast.sceneUninstalled'))
          loadScenes()
        } else {
          showToast(result.message || t('toast.sceneUninstallFailed'))
        }
      } catch (error) {
        showToast(t('toast.sceneUninstallFailed'))
      }
    }

    backButton.addEventListener('click', goHome)
    sceneSettingsButton.addEventListener('click', toggleSceneSettings)
    alertOpenButton.addEventListener('click', openAlertScene)
    alertCloseButton.addEventListener('click', clearAlert)
    runtimeStrip.addEventListener('click', () => {
      const scene = runtimeScene()
      if (scene) openScene(scene)
    })
    runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
    runtimeSettingsBack.addEventListener('click', closeRuntimeSettings)
    runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
    runtimeModelBack.addEventListener('click', closeRuntimeModelSettings)
    runtimeServiceToggle.addEventListener('click', () => {
      const running = runtimeServiceState && (runtimeServiceState.running || runtimeServiceState.starting)
      runtimeServiceCommand(running ? 'stop' : 'start', {})
    })
    runtimeWindowAutoInput.addEventListener('change', () => {
      runtimeServiceCommand('window.settings', { autoShow: !!runtimeWindowAutoInput.checked })
    })
    runtimeWindowButton.addEventListener('click', () => {
      runtimeServiceCommand(runtimeServiceState && runtimeServiceState.windowVisible ? 'window.hide' : 'window.show', {})
    })
    runtimeWindowPermissionButton.addEventListener('click', () => {
      runtimeServiceCommand('window.permission', {})
    })
    runtimeCpuThreadsInput.addEventListener('change', () => {
      const max = Number(runtimeServiceState && runtimeServiceState.maxCpuThreads) || 16
      const value = Math.max(1, Math.min(max, Math.round(Number(runtimeCpuThreadsInput.value) || 4)))
      runtimeCpuThreadsInput.value = String(value)
      runtimeServiceCommand('settings', { cpuThreads: value })
    })
    batteryOptimizationButton.addEventListener('click', () => {
      runtimeServiceCommand('battery.requestExemption', {})
    })
    languageControl.addEventListener('click', event => {
      const button = event.target.closest('.segmented-option')
      if (button && button.dataset.langMode) saveLocaleSetting(button.dataset.langMode)
    })
    modelReloadButton.addEventListener('click', () => {
      loadModels()
      showToast(t('toast.modelsReloaded'))
    })
    modelImportButton.addEventListener('click', () => {
      native('installModelPackage')
    })
    modelMarketButton.addEventListener('click', () => showToast(t('toast.modelMarketSoon')))
    reloadButton.addEventListener('click', () => {
      loadScenes()
      showToast(t('toast.scenesReloaded'))
    })

    window.MNNodeShell = { goHome }

    installButton.addEventListener('click', () => {
      native('installScenePack')
    })

    window.MNNodeEvents = {
      onSceneInstallResult(result) {
        if (result && result.ok) {
          loadScenes()
          showToast(t('toast.sceneInstalled'))
          stateText.textContent = t('state.idle')
        } else if (result) {
          showToast(result.message || t('toast.installFailed'))
        }
      },
      onModelInstallResult(result) {
        if (result && result.state === 'installing') {
          setModelProgress(result)
        } else if (result && result.ok) {
          setModelProgress(result)
          loadModels()
          showToast(t('toast.modelImported'))
        } else if (result) {
          setModelProgress(Object.assign({ state: 'error' }, result))
          showToast(result.message || t('toast.modelImportFailed'))
        }
      },
      onCameraResult(result) {
        if (result && result.ok) {
          postToScene({ type: 'camera.state', state: 'previewing' })
          showToast(t('toast.cameraStarted'))
        } else if (result) {
          postToScene({ type: 'camera.state', state: 'error', message: result.message || '' })
          showToast(result.message || 'Camera failed')
        }
      },
      onVisionState(result) {
        postToScene(Object.assign({ type: 'vision.state' }, result || {}))
        if (result && result.state === 'running') showToast(t('toast.visionStarted'))
        if (result && result.state === 'error') showToast(result.message || 'Vision failed')
      },
      onVisionFrame(frame) {
        postToScene(Object.assign({ type: 'vision.frame' }, frame || {}))
      },
      onRuntimeMessage(message) {
        handleRuntimeMessage(message)
      },
      onModelChatResult(result) {
        const message = Object.assign({ type: 'model.chat.result' }, result || {})
        postToSceneReliable(message)
      }
    }

    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) refreshRuntimeServiceState()
    })

    const messageHandlers = {
      'scene.ready': () => {
        stateText.textContent = t('state.running')
        broadcastLocale()
        syncRuntimeSnapshot()
      },
      'runtime.subscribe': () => syncRuntimeSnapshot(),
      'runtime.command': data => sendRuntimeCommand(data.sceneId || (activeScene && activeScene.id) || '', data.command || '', data.payload || {}),
      'camera.preview.rect': data => syncCameraPreviewRect(data.rect),
      'camera.start': data => {
        showToast(t('toast.cameraRequested'))
        startCameraFromScene(data.rect)
      },
      'camera.stop': () => stopCamera(),
      'vision.start': data => {
        showToast(t('toast.visionRequested'))
        startVisionFromScene({
          modelId: data.modelId || 'yolov8n',
          backend: data.backend || 'auto',
          inferenceIntervalMs: data.inferenceIntervalMs || 250,
          previewIntervalMs: data.previewIntervalMs || 250,
          confidenceThreshold: data.confidenceThreshold || 0.5
        })
      },
      'vision.stop': () => stopVision(),
      'model.chat': data => modelChatFromScene(data),
      'api.server.status': data => runtimeApiCommand('status', data),
      'api.server.settings': data => runtimeApiCommand('settings', data),
      'api.server.start': data => runtimeApiCommand('start', data),
      'api.server.stop': data => runtimeApiCommand('stop', data),
      'api.server.session.create': data => runtimeApiCommand('session.create', data),
      'api.server.session.select': data => runtimeApiCommand('session.select', data),
      'api.server.session.delete': data => runtimeApiCommand('session.delete', data)
    }

    window.addEventListener('message', event => {
      const data = event.data || {}
      const handler = messageHandlers[data.type]
      if (handler) handler(data)
    })

    function tick() {
      clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
      syncRuntimeSnapshot({ notifyScene: false })
      updateRuntimeStrip()
    }

    loadScenes()
    loadModels()
    loadLocaleSetting()
    refreshRuntimeServiceState()
    tick()
    setInterval(tick, 1000)
