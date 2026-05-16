const app = document.getElementById('app')
    const clock = document.getElementById('clock')
    const stateText = document.getElementById('stateText')
    const navItems = Array.from(document.querySelectorAll('.nav-item'))
    const panels = Array.from(document.querySelectorAll('.panel'))
    const sceneHost = document.getElementById('sceneHost')
    const sceneFrame = document.getElementById('sceneFrame')
    const sceneList = document.getElementById('sceneList')
    const modelList = document.getElementById('modelList')
    const modelMarketPanel = document.getElementById('modelMarketPanel')
    const modelMarketList = document.getElementById('modelMarketList')
    const modelMarketSearch = document.getElementById('modelMarketSearch')
    const modelMarketRefreshButton = document.getElementById('modelMarketRefreshButton')
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
    const runtimeServerButton = document.getElementById('runtimeServerButton')
    const runtimeServerState = document.getElementById('runtimeServerState')
    const runtimeServerPanel = document.getElementById('runtimeServerPanel')
    const runtimeServerBack = document.getElementById('runtimeServerBack')
    const runtimePortInput = document.getElementById('runtimePortInput')
    const runtimeMaxTokensInput = document.getElementById('runtimeMaxTokensInput')
    const runtimeCpuThreadsInput = document.getElementById('runtimeCpuThreadsInput')
    const runtimeModelButton = document.getElementById('runtimeModelButton')
    const runtimeModelState = document.getElementById('runtimeModelState')
    const runtimeModelPanel = document.getElementById('runtimeModelPanel')
    const runtimeModelBack = document.getElementById('runtimeModelBack')
    const runtimeModelList = document.getElementById('runtimeModelList')
    const runtimeModelNote = document.getElementById('runtimeModelNote')
    const runtimeSessionsButton = document.getElementById('runtimeSessionsButton')
    const runtimeSessionsState = document.getElementById('runtimeSessionsState')
    const runtimeSessionsPanel = document.getElementById('runtimeSessionsPanel')
    const runtimeSessionsBack = document.getElementById('runtimeSessionsBack')
    const runtimeCurrentSession = document.getElementById('runtimeCurrentSession')
    const runtimeSessionNewButton = document.getElementById('runtimeSessionNewButton')
    const runtimeSessionList = document.getElementById('runtimeSessionList')
    const runtimeDiagnosticsButton = document.getElementById('runtimeDiagnosticsButton')
    const runtimeDiagnosticsState = document.getElementById('runtimeDiagnosticsState')
    const runtimeDiagnosticsPanel = document.getElementById('runtimeDiagnosticsPanel')
    const runtimeDiagnosticsBack = document.getElementById('runtimeDiagnosticsBack')
    const runtimeRequestList = document.getElementById('runtimeRequestList')
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
    const diagRequestCount = document.getElementById('diagRequestCount')
    const diagError = document.getElementById('diagError')
    const toast = document.getElementById('toast')
    let scenes = []
    let activeScene = null
    let runtimeSnapshot = null
    let runtimeServiceState = null
    let runtimeModels = []
    let marketVisible = false
    let marketModels = []
    let marketQuery = ''
    let marketInstallTimer = null
    let marketInstallingModelId = ''
    let marketSearchTimer = null
    let localeSetting = { mode: 'system' }
    let currentLocale = 'en'
    let activeAlert = null
    const localeStorePath = '/v1/store/runtime-settings/locale'

    function native(method, ...args) {
      try {
        const bridge = window.MNNodeShell
        if (bridge && typeof bridge[method] === 'function') return bridge[method](...args)
      } catch (error) {}
      return null
    }

    function nativeJson(method, fallback, ...args) {
      const raw = native(method, ...args)
      if (!raw) return fallback
      try { return JSON.parse(raw) } catch (error) { return fallback }
    }

    function localApiBaseUrl() {
      const state = runtimeServiceState || {}
      const raw = state.url || 'http://127.0.0.1:11434'
      return String(raw).replace('0.0.0.0', '127.0.0.1').replace(/\/$/, '')
    }

    function apiUrl(path) {
      return localApiBaseUrl() + path
    }

    async function apiGet(path) {
      return apiRequest('GET', path)
    }

    async function apiPost(path, body) {
      return apiRequest('POST', path, body)
    }

    async function apiRequest(method, path, body) {
      const response = await fetch(apiUrl(path), {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: method === 'GET' ? undefined : JSON.stringify(body || {})
      })
      const json = await response.json()
      if (!response.ok) throw new Error(path + ': ' + ((json.error && json.error.message) || json.message || 'API request failed'))
      return json
    }

    function retryApi(task, fallback, attempts = 8) {
      let index = 0
      const run = () => task().catch(error => {
        index += 1
        if (index >= attempts) return fallback(error)
        return new Promise(resolve => window.setTimeout(resolve, 250 * index)).then(run)
      })
      return run()
    }

    function shellCommand(command, payload) {
      const raw = native('runtimeShellCommand', command, JSON.stringify(payload || {}))
      return raw ? JSON.parse(raw) : { running: false, message: 'Runtime shell unavailable' }
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

    function runtimeDetails() {
      return [runtimeSettingsPanel, runtimeServerPanel, runtimeModelPanel, runtimeSessionsPanel, runtimeDiagnosticsPanel].filter(Boolean)
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
        'settings.serverTitle': 'Server',
        'settings.serverSub': 'Port, output tokens, URL, and visible window.',
        'settings.serverIntro': 'Configure the local OpenAI/Ollama-compatible API server.',
        'settings.port': 'Port',
        'settings.portSub': 'Changing the port requires restarting the server.',
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
        'settings.outputTokensSub': 'Default cap for requests that do not specify max_tokens.',
        'settings.defaultTokens': 'Default',
        'settings.modelTokens': 'Model Cap',
        'settings.effectiveTokens': 'Effective Cap',
        'settings.sessionsTitle': 'Sessions',
        'settings.sessionsSub': 'Current API chat session and recent sessions.',
        'settings.sessionsIntro': 'Choose which durable API chat session receives new turns.',
        'settings.currentSession': 'Current Session',
        'settings.newSession': 'New',
        'settings.diagnosticsTitle': 'Diagnostics',
        'settings.diagnosticsSub': 'Queue, requests, token caps, and recent errors.',
        'settings.diagnosticsIntro': 'Runtime state and recent API request history.',
        'settings.noSessions': 'No sessions yet',
        'settings.noRequests': 'No recent requests',
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
        'models.market': 'Market',
        'models.marketTitle': 'Model Market',
        'models.marketSub': 'ModelScope MNN catalog.',
        'models.marketSearch': 'Search models',
        'models.install': 'Install',
        'models.installed': 'Installed',
        'models.installing': 'Installing model',
        'nodes.placeholder': 'Multi-node discovery, connection status, and collaborative tasks will appear here. This version keeps the page as a placeholder.',
        'toast.modelsReloaded': 'Model list refreshed',
        'toast.modelMarketLoaded': 'Model market loaded',
        'toast.modelMarketFailed': 'Model market failed',
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
        'settings.serverTitle': '服务',
        'settings.serverSub': '端口、输出 tokens、URL 和可见小窗。',
        'settings.serverIntro': '配置本地 OpenAI/Ollama-compatible API 服务。',
        'settings.port': '端口',
        'settings.portSub': '修改端口后需要重启服务。',
        'settings.pipTitle': 'Runtime 小窗',
        'settings.pipSub': '运行服务时在其他 App 上方显示 runtime 小窗。',
        'settings.windowAuto': '自动',
        'settings.windowShow': '显示',
        'settings.windowHide': '隐藏',
        'settings.windowPermission': '权限',
        'settings.windowAllowed': '已允许',
        'settings.windowPermissionRequired': '需要悬浮窗权限',
        'settings.defaultModelTitle': '默认模型',
        'settings.defaultModelSub': '选择 Runtime API 默认使用的模型。',
        'settings.defaultModelIntro': '这里设置的是 OpenAI 和 Ollama 请求在未显式指定 model 时所使用的默认模型。',
        'settings.defaultModelNote': '这里只显示已就绪的本地模型。',
        'settings.selected': '当前',
        'settings.cpuThreads': 'CPU 线程数',
        'settings.cpuThreadsSub': '覆盖模型 config.json 的 thread_num。修改后会重新加载模型。',
        'settings.cpuThreadsShort': 'CPU 线程',
        'settings.outputTokens': '输出 Tokens',
        'settings.outputTokensSub': '请求未指定 max_tokens 时使用的默认上限。',
        'settings.defaultTokens': '默认值',
        'settings.modelTokens': '模型上限',
        'settings.effectiveTokens': '生效上限',
        'settings.sessionsTitle': 'Sessions',
        'settings.sessionsSub': '当前 API 对话 session 和最近 sessions。',
        'settings.sessionsIntro': '选择新的 API 对话轮次写入哪个持久 session。',
        'settings.currentSession': '当前 Session',
        'settings.newSession': '新建',
        'settings.diagnosticsTitle': '诊断',
        'settings.diagnosticsSub': '队列、请求、token 上限和最近错误。',
        'settings.diagnosticsIntro': 'Runtime 状态和最近 API 请求历史。',
        'settings.noSessions': '暂无 sessions',
        'settings.noRequests': '暂无请求记录',
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
        'models.market': '模型市场',
        'models.marketTitle': '魔塔模型市场',
        'models.marketSub': '精选 ModelScope MNN 模型。',
        'models.install': '安装',
        'models.installed': '已安装',
        'models.installing': '正在安装模型',
        'nodes.placeholder': '多节点发现、连接状态和协同任务会放在这里。当前版本仅保留界面占位。',
        'toast.modelsReloaded': '模型列表已刷新',
        'toast.modelMarketLoaded': '模型市场已加载',
        'toast.modelMarketFailed': '模型市场加载失败',
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
      void { x, y, width, height }
    }

    function nativeViewportScale() {
      const cssWidth = Math.max(1, document.documentElement.clientWidth || window.innerWidth || 1)
      const cssHeight = Math.max(1, document.documentElement.clientHeight || window.innerHeight || 1)
      const ratio = window.devicePixelRatio || 1
      return { x: ratio, y: ratio }
    }

    function loadLocaleSetting() {
      apiGet(localeStorePath)
        .then(result => {
          localeSetting = result && result.ok && result.value ? result.value : { mode: 'system' }
          applyLocale()
        })
        .catch(() => applyLocale())
    }

    function saveLocaleSetting(mode) {
      localeSetting = { mode: mode || 'system' }
      apiPost(localeStorePath, { value: localeSetting }).catch(() => {})
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

    function sceneEntryUrl(scene) {
      return scene && scene.entryUrl
    }

    function sceneApiClient() {
      const baseUrl = localApiBaseUrl()
      const request = (path, body) => fetch(baseUrl + path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body || {})
      }).then(response => response.json().then(json => {
        if (!response.ok || !json.ok) throw new Error((json.error && json.error.message) || json.message || 'API request failed')
        return json.result || json
      }))
      return {
        baseUrl,
        get(path) {
          return fetch(baseUrl + path).then(response => response.json())
        },
        post(path, body) {
          return request(path, body)
        },
        tool(name, args) {
          return request('/v1/tools/' + encodeURIComponent(name) + '/call', { arguments: args || {} })
        },
        chat(requestBody) {
          return fetch(baseUrl + '/v1/chat/completions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody || {})
          }).then(response => response.json())
        }
      }
    }

    function publishSceneApiClient() {
      const api = sceneApiClient()
      window.MNNodeAPI = api
      return api
    }

    function installSceneApiClient() {
      const api = publishSceneApiClient()
      try {
        if (sceneFrame.contentWindow) {
          sceneFrame.contentWindow.MNNodeAPI = api
          postToScene({ type: 'api.ready', baseUrl: api.baseUrl, sameOrigin: true })
        }
      } catch (error) {
        postToScene({ type: 'api.ready', baseUrl: api.baseUrl, sameOrigin: false })
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

    function stopCamera() {
      apiPost('/v1/tools/stop_vision_rules/call', { arguments: {} }).catch(() => {})
    }

    function runtimeApiCommand(command, payload) {
      try {
        const body = Object.assign({}, payload || {}, {
          sceneId: (payload && payload.sceneId) || (activeScene && activeScene.id) || ''
        })
        const runShell = ['start', 'stop', 'status', 'battery.requestExemption', 'window.show', 'window.hide', 'window.settings', 'window.permission'].includes(command)
        if (runShell) {
          updateRuntimeServiceState(shellCommand(command, body))
          return
        }
        const promise = apiPost('/v1/runtime/' + encodeURIComponent(command), body)
        promise.then(state => {
          updateRuntimeServiceState(state)
        }).catch(() => updateRuntimeServiceState({ running: false, message: 'API server command failed' }))
      } catch (error) {
        updateRuntimeServiceState({ running: false, message: 'API server command failed' })
      }
    }

    function runtimeServiceCommand(command, payload) {
      try {
        updateRuntimeServiceState(shellCommand(command, payload))
      } catch (error) {
        updateRuntimeServiceState({ running: false, message: 'Runtime service command failed' })
      }
    }

    function updateRuntimeServiceState(state) {
      runtimeServiceState = state || runtimeServiceState || {}
      publishSceneApiClient()
      const running = !!runtimeServiceState.running
      const starting = !!runtimeServiceState.starting
      runtimeSettingsState.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
      runtimeSettingsState.classList.toggle('running', running || starting)
      runtimeServiceToggle.textContent = running || starting ? t('common.stop') : t('common.start')
      runtimeServiceMessage.textContent = runtimeServiceState.message || t('settings.runtimeDefaultMessage')
      runtimeServerState.textContent = runtimeServiceState.port ? String(runtimeServiceState.port) : '--'
      runtimeWindowAutoInput.checked = !!runtimeServiceState.windowAutoShow
      runtimeWindowAutoInput.disabled = !runtimeServiceState.windowSupported
      runtimeWindowButton.disabled = !runtimeServiceState.windowSupported
      runtimeWindowButton.textContent = runtimeServiceState.windowVisible ? t('settings.windowHide') : t('settings.windowShow')
      runtimeWindowPermissionButton.disabled = !runtimeServiceState.windowSupported || !!runtimeServiceState.windowAllowed
      runtimeWindowPermissionButton.textContent = runtimeServiceState.windowAllowed ? t('settings.windowAllowed') : t('settings.windowPermission')
      if (document.activeElement !== runtimePortInput) runtimePortInput.value = String(runtimeServiceState.port || 11434)
      if (document.activeElement !== runtimeMaxTokensInput) runtimeMaxTokensInput.value = String(runtimeServiceState.maxOutputTokens || runtimeServiceState.defaultOutputTokens || 512)
      const maxCpuThreads = Number(runtimeServiceState.maxCpuThreads) || 16
      const cpuThreads = Number(runtimeServiceState.cpuThreads) || 4
      runtimeCpuThreadsInput.max = String(maxCpuThreads)
      if (document.activeElement !== runtimeCpuThreadsInput) runtimeCpuThreadsInput.value = String(cpuThreads)
      const modelSuffix = runtimeServiceState.modelLoading ? ' · loading' : (runtimeServiceState.modelLoaded ? ' · loaded' : '')
      runtimeModelState.textContent = (runtimeServiceState.modelId || runtimeModelState.textContent || '--') + modelSuffix
      const sessions = Array.isArray(runtimeServiceState.sessions) ? runtimeServiceState.sessions : []
      runtimeSessionsState.textContent = sessions.length ? String(sessions.length) : '--'
      runtimeCurrentSession.textContent = runtimeServiceState.currentSessionId || '--'
      runtimeDiagnosticsState.textContent = String(runtimeServiceState.requestCount || 0)
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
      diagRequestCount.textContent = String(runtimeServiceState.requestCount || 0)
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
      renderRuntimeSessions()
      renderRuntimeRequests()
    }

    function refreshRuntimeServiceState() {
      runtimeApiCommand('status', {})
    }

    function showRuntimePanel(panel) {
      settingsList.classList.add('hidden')
      runtimeDetails().forEach(item => {
        const active = item === panel
        item.classList.toggle('active', active)
        item.setAttribute('aria-hidden', active ? 'false' : 'true')
      })
    }

    function openRuntimeSettings() {
      showRuntimePanel(runtimeSettingsPanel)
      refreshRuntimeServiceState()
      loadModels()
    }

    function closeRuntimeSettings() {
      settingsList.classList.remove('hidden')
      runtimeDetails().forEach(item => {
        item.classList.remove('active')
        item.setAttribute('aria-hidden', 'true')
      })
    }

    function openRuntimeServerSettings() {
      showRuntimePanel(runtimeServerPanel)
      refreshRuntimeServiceState()
    }

    function openRuntimeModelSettings() {
      showRuntimePanel(runtimeModelPanel)
      loadModels()
      refreshRuntimeServiceState()
    }

    function openRuntimeSessionsSettings() {
      showRuntimePanel(runtimeSessionsPanel)
      refreshRuntimeServiceState()
    }

    function openRuntimeDiagnosticsSettings() {
      showRuntimePanel(runtimeDiagnosticsPanel)
      refreshRuntimeServiceState()
    }

    function backToRuntimeSettings() {
      showRuntimePanel(runtimeSettingsPanel)
    }

    function activateRuntime(scene) {
      if (!scene || !scene.runtime) return
      if (runtimeSnapshot && runtimeSnapshot.sceneId === scene.id && runtimeSnapshot.active) {
        updateRuntimeStrip()
        postToSceneReliable(runtimeSnapshot)
        return
      }
      try {
        apiPost('/v1/scenes/' + encodeURIComponent(scene.id) + '/load', {})
          .then(result => {
            runtimeSnapshot = {
              type: 'runtime.snapshot',
              active: !!(result && result.ok),
              running: !!(result && result.ok),
              sceneId: scene.id,
              state: 'loaded',
              elapsedMs: 0,
              events: [],
              triggersLoaded: result && result.triggersLoaded
            }
            updateRuntimeStrip()
            if (runtimeSnapshot.active) postToSceneReliable(runtimeSnapshot)
          })
      } catch (error) {}
    }

    function syncRuntimeSnapshot(options) {
      const notifyScene = !(options && options.notifyScene === false)
      updateRuntimeStrip()
      if (notifyScene && runtimeSnapshot && runtimeSnapshot.active) postToSceneReliable(runtimeSnapshot)
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
      if (message.type === 'runtime.event' && runtimeSnapshot) {
        const events = Array.isArray(runtimeSnapshot.events) ? runtimeSnapshot.events.slice() : []
        events.unshift(message.event || message)
        runtimeSnapshot.events = events.slice(0, 20)
        if (message.event && message.event.alert) showAlert(message)
      }
      updateRuntimeStrip()
      postToSceneReliable(message)
    }

    function sendRuntimeCommand(sceneId, command, payload) {
      try {
        if (command === 'stop') {
          runtimeSnapshot = null
          stopCamera()
          updateRuntimeStrip()
          return
        }
        if (command === 'start' || command === 'sync' || command === 'reset') {
          const scene = sceneById(sceneId)
          if (scene) activateRuntime(scene)
          return
        }
        showToast('Runtime command not supported')
      } catch (error) {
        showToast('Runtime command failed')
      }
    }

    sceneFrame.addEventListener('load', () => {
      installSceneApiClient()
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
        if (page === 'models') {
          modelMarketPanel.classList.toggle('active', marketVisible)
          loadModels()
          if (marketVisible) loadModelMarket()
        }
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
      retryApi(() => apiGet('/v1/scenes'), () => {
        scenes = []
        renderScenes()
      })
        .then(result => {
          scenes = Array.isArray(result) ? result : []
          renderScenes()
        })
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
      retryApi(() => apiGet('/v1/models/full'), () => {
        runtimeModels = []
        renderModels([])
      })
        .then(models => {
          runtimeModels = Array.isArray(models) ? models.slice() : []
          renderModels(runtimeModels)
          if (marketVisible) loadModelMarket()
        })
    }

    function loadModelMarket(refresh) {
      if (!modelMarketList) return Promise.resolve()
      marketVisible = true
      modelMarketPanel.classList.add('active')
      modelMarketButton.classList.add('active')
      return retryApi(() => apiGet('/v1/models/market?q=' + encodeURIComponent(marketQuery || '') + (refresh ? '&refresh=true' : '')), () => {
        marketModels = []
        renderModelMarket([])
      })
        .then(payload => {
          marketModels = Array.isArray(payload && payload.models) ? payload.models.slice() : []
          renderModelMarket(marketModels)
          showToast(t('toast.modelMarketLoaded'))
        })
        .catch(() => {
          marketModels = []
          renderModelMarket([])
          showToast(t('toast.modelMarketFailed'))
        })
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

    function renderModelMarket(models) {
      if (!modelMarketList) return
      modelMarketList.innerHTML = ''
      if (!marketVisible) return

      if (!models.length) {
        modelMarketList.appendChild(emptyCard(t('empty.models')))
        return
      }

      models.forEach(model => {
        const row = document.createElement('div')
        row.className = 'market-row'

        const body = document.createElement('div')
        body.className = 'market-body'
        body.appendChild(el('div', 'market-name', model.name || model.id))
        body.appendChild(el('div', 'market-meta', [model.repo, model.runtime, model.source].filter(Boolean).join(' · ')))
        body.appendChild(el('div', 'market-desc', model.description || ''))

        const actions = document.createElement('div')
        actions.className = 'market-actions'
        const status = el('span', 'settings-row-state', isMarketModelInstalled(model) ? t('models.installed') : (marketInstallingModelId === model.id ? t('models.installing') : t('models.install')))
        actions.appendChild(status)

        const install = document.createElement('button')
        install.className = 'install-button pressable'
        install.type = 'button'
        install.textContent = isMarketModelInstalled(model) ? t('models.installed') : (marketInstallingModelId === model.id ? t('models.installing') : t('models.install'))
        install.disabled = isMarketModelInstalled(model) || marketInstallingModelId === model.id
        install.addEventListener('click', () => installMarketModel(model))
        actions.appendChild(install)

        row.appendChild(body)
        row.appendChild(actions)
        modelMarketList.appendChild(row)
      })
    }

    function isMarketModelInstalled(model) {
      return runtimeModels.some(item => item && item.id === model.id && item.ready)
    }

    function installMarketModel(model) {
      if (!model || !model.id || isMarketModelInstalled(model)) return
      marketInstallingModelId = model.id
      setModelProgress({ state: 'installing', message: t('models.installing') + ': ' + (model.name || model.id) })
      apiPost('/v1/models/market/' + encodeURIComponent(model.id) + '/install', {})
        .then(result => {
          if (result && result.ok) {
            pollMarketInstall(model.id)
            loadModels()
          } else {
            marketInstallingModelId = ''
            setModelProgress(Object.assign({ state: 'error' }, result || {}))
            showToast((result && result.message) || t('toast.modelImportFailed'))
          }
        })
        .catch(() => {
          marketInstallingModelId = ''
          setModelProgress({ state: 'error', message: t('toast.modelImportFailed') })
          showToast(t('toast.modelImportFailed'))
        })
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
          runtimeApiCommand('settings', { modelId: model.id })
          runtimeModelState.textContent = model.id
          showToast(model.name || model.id)
        })
        runtimeModelList.appendChild(row)
      })
    }

    function renderRuntimeSessions() {
      if (!runtimeSessionList) return
      runtimeSessionList.innerHTML = ''
      const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions) ? runtimeServiceState.sessions : []
      const currentId = runtimeServiceState && runtimeServiceState.currentSessionId
      if (!sessions.length) {
        runtimeSessionList.appendChild(emptyCard(t('settings.noSessions')))
        return
      }
      sessions.forEach(session => {
        const row = document.createElement('button')
        row.type = 'button'
        row.className = 'model-choice-row pressable'
        row.classList.toggle('active', session.id === currentId)

        const body = document.createElement('div')
        body.className = 'model-choice-body'
        const title = el('div', 'model-choice-title', session.title || session.id)
        const meta = el('div', 'model-choice-sub', [session.id, session.messageCount ? session.messageCount + ' messages' : '0 messages'].filter(Boolean).join(' · '))
        body.appendChild(title)
        body.appendChild(meta)

        const actions = document.createElement('div')
        actions.className = 'settings-actions'
        const state = el('span', 'settings-row-state', session.id === currentId ? t('settings.selected') : (session.modelId || ''))
        actions.appendChild(state)
        const remove = document.createElement('button')
        remove.className = 'scene-mini-button pressable'
        remove.type = 'button'
        remove.textContent = t('models.delete')
        remove.addEventListener('click', event => {
          event.stopPropagation()
          runtimeApiCommand('session.delete', { sessionId: session.id })
        })
        actions.appendChild(remove)

        row.appendChild(body)
        row.appendChild(actions)
        row.addEventListener('click', () => runtimeApiCommand('session.select', { sessionId: session.id }))
        runtimeSessionList.appendChild(row)
      })
    }

    function renderRuntimeRequests() {
      if (!runtimeRequestList) return
      runtimeRequestList.innerHTML = ''
      const requests = Array.isArray(runtimeServiceState && runtimeServiceState.recentRequests) ? runtimeServiceState.recentRequests : []
      if (!requests.length) {
        runtimeRequestList.appendChild(emptyCard(t('settings.noRequests')))
        return
      }
      requests.slice(0, 8).forEach(request => {
        const row = document.createElement('div')
        row.className = 'model-choice-row'
        const body = document.createElement('div')
        body.className = 'model-choice-body'
        const status = request.status || '--'
        body.appendChild(el('div', 'model-choice-title', [request.method, request.endpoint, status].filter(Boolean).join(' · ')))
        body.appendChild(el('div', 'model-choice-sub', [request.modelId, request.elapsedMs !== undefined ? request.elapsedMs + ' ms' : '', request.time ? new Date(request.time).toLocaleTimeString('zh-CN', { hour12: false }) : ''].filter(Boolean).join(' · ')))
        row.appendChild(body)
        row.appendChild(el('span', 'settings-row-state', String(status)))
        runtimeRequestList.appendChild(row)
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
        apiPost('/v1/models/' + encodeURIComponent(model.id) + '/delete', {})
          .then(result => {
            if (result.ok) {
          showToast(t('toast.modelDeleted'))
          loadModels()
        } else {
          showToast(result.message || t('toast.modelDeleteFailed'))
        }
          })
          .catch(() => showToast(t('toast.modelDeleteFailed')))
      } catch (error) {
        showToast(t('toast.modelDeleteFailed'))
      }
    }

    function pollMarketInstall(modelId) {
      window.clearInterval(marketInstallTimer)
      marketInstallTimer = window.setInterval(() => {
        apiGet('/v1/models/market/' + encodeURIComponent(modelId) + '/progress')
          .then(result => {
            if (!result) return
            setModelProgress(result)
            if (result.active === false || result.progress >= 1) {
              window.clearInterval(marketInstallTimer)
              marketInstallTimer = null
              marketInstallingModelId = ''
              loadModels()
            }
          })
          .catch(() => {})
      }, 700)
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

      if (result.state === 'done' || result.state === 'error' || result.active === false || progress >= 1) {
        setModelProgress.timer = window.setTimeout(() => setModelProgress({ state: 'idle' }), 1800)
      }
    }

    function goHome() {
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
      updateRuntimeServiceState(shellCommand('status', {}))
      panels.forEach(panel => panel.classList.remove('active'))
      backButton.classList.add('active')
      activeScene = scene
      setSceneSettingsVisible(sceneHasSettings(scene))
      sceneHost.classList.add('active')
      stopCamera()
      sceneHost.scrollTop = 0
      cameraPreviewRect = null
      sceneFrame.style.height = '100%'
      sceneFrame.src = sceneEntryUrl(scene)
      activateRuntime(scene)
      stateText.textContent = t('state.running')
      updateRuntimeStrip()
    }

    function uninstallScene(scene) {
      if (!scene || scene.source !== 'installed') return
      try {
        apiPost('/v1/scenes/' + encodeURIComponent(scene.id) + '/delete', {})
          .then(result => {
            if (result.ok) {
          showToast(t('toast.sceneUninstalled'))
          loadScenes()
        } else {
          showToast(result.message || t('toast.sceneUninstallFailed'))
        }
          })
          .catch(() => showToast(t('toast.sceneUninstallFailed')))
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
    runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
    runtimeServerBack.addEventListener('click', backToRuntimeSettings)
    runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
    runtimeModelBack.addEventListener('click', backToRuntimeSettings)
    runtimeSessionsButton.addEventListener('click', openRuntimeSessionsSettings)
    runtimeSessionsBack.addEventListener('click', backToRuntimeSettings)
    runtimeDiagnosticsButton.addEventListener('click', openRuntimeDiagnosticsSettings)
    runtimeDiagnosticsBack.addEventListener('click', backToRuntimeSettings)
    runtimeServiceToggle.addEventListener('click', () => {
      const running = runtimeServiceState && (runtimeServiceState.running || runtimeServiceState.starting)
      runtimeApiCommand(running ? 'stop' : 'start', {})
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
    runtimePortInput.addEventListener('change', () => {
      const value = Math.max(1024, Math.min(65535, Math.round(Number(runtimePortInput.value) || 11434)))
      runtimePortInput.value = String(value)
      runtimeApiCommand('settings', { port: value })
    })
    runtimeMaxTokensInput.addEventListener('change', () => {
      const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
      const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeMaxTokensInput.value) || 512)))
      runtimeMaxTokensInput.value = String(value)
      runtimeApiCommand('settings', { maxOutputTokens: value })
    })
    runtimeCpuThreadsInput.addEventListener('change', () => {
      const max = Number(runtimeServiceState && runtimeServiceState.maxCpuThreads) || 16
      const value = Math.max(1, Math.min(max, Math.round(Number(runtimeCpuThreadsInput.value) || 4)))
      runtimeCpuThreadsInput.value = String(value)
      runtimeApiCommand('settings', { cpuThreads: value })
    })
    runtimeSessionNewButton.addEventListener('click', () => {
      runtimeApiCommand('session.create', {})
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
    modelMarketButton.addEventListener('click', () => {
      marketVisible = !marketVisible
      modelMarketPanel.classList.toggle('active', marketVisible)
      modelMarketButton.classList.toggle('active', marketVisible)
      renderModelMarket(marketModels)
      if (marketVisible && !marketModels.length) loadModelMarket()
    })
    modelMarketRefreshButton.addEventListener('click', () => loadModelMarket(true))
    modelMarketSearch.addEventListener('input', () => {
      window.clearTimeout(marketSearchTimer)
      marketSearchTimer = window.setTimeout(() => {
        marketQuery = modelMarketSearch.value.trim()
        loadModelMarket()
      }, 250)
    })
    reloadButton.addEventListener('click', () => {
      loadScenes()
      showToast(t('toast.scenesReloaded'))
    })

    window.MNNodeShellUi = { goHome }

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

    refreshRuntimeServiceState()
    loadScenes()
    loadModels()
    loadLocaleSetting()
    tick()
    setInterval(tick, 1000)
