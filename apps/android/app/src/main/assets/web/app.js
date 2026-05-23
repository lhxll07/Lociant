/* === 01-i18n.js === */
/* ── Lociant WebUI — i18n dictionary ── */

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
    'settings.runtimeIntro': 'Control Lociant as a local AI capability runtime in the background.',
    'settings.modelServer': 'Model Server',
    'settings.runtimeDefaultMessage': 'Foreground service exposes the LAN API; foreground window keeps inference visible.',
    'settings.serverTitle': 'Server',
    'settings.serverSub': 'Port, output tokens, API token, and URL.',
    'settings.serverIntro': 'Configure the local OpenAI/Ollama-compatible API server.',
    'settings.capabilitiesTitle': 'Capabilities',
    'settings.capabilitiesSub': 'Vision, runtime window, and remote tool exposure.',
    'settings.capabilitiesIntro': 'Manage phone-side capabilities exposed to scenes, agents, and the floating runtime window.',
    'settings.visionTitle': 'Vision',
    'settings.visionSub': 'Camera analysis and object detection.',
    'settings.visionState': 'Vision',
    'settings.visionReady': 'Ready',
    'settings.visionStarting': 'Starting',
    'settings.visionUnavailable': 'Unavailable',
    'settings.port': 'Port',
    'settings.portSub': 'Changing the port requires restarting the server.',
    'settings.windowTitle': 'Runtime Window',
    'settings.windowSub': 'Show a small runtime window over other apps while the server runs.',
    'settings.windowAuto': 'Auto',
    'settings.windowShow': 'Show',
    'settings.windowHide': 'Hide',
    'settings.windowPermission': 'Permission',
    'settings.windowAllowed': 'Allowed',
    'settings.windowPermissionRequired': 'Floating window permission required',
    'settings.windowStateVisible': 'Visible',
    'settings.windowStateCollapsed': 'Collapsed',
    'settings.windowStateHidden': 'Hidden',
    'settings.windowStateError': 'Error',
    'settings.windowStateWindow': 'Window',
    'settings.defaultModelTitle': 'Default Model',
    'settings.defaultModelSub': 'Choose the default model used by the model server.',
    'settings.defaultModelIntro': 'Default model used for OpenAI and Ollama requests when no model is specified.',
    'settings.defaultModelNote': 'Only ready models are shown here.',
    'settings.selected': 'Selected',
    'settings.cpuThreads': 'CPU Threads',
    'settings.cpuThreadsSub': 'Override thread_num in model config.json. Changing this reloads the model.',
    'settings.cpuThreadsShort': 'CPU Threads',
    'settings.outputTokens': 'Output Tokens',
    'settings.outputTokensSub': 'Default cap for requests that do not specify max_tokens.',
    'settings.apiToken': 'API Token',
    'settings.apiTokenSub': 'Require Authorization: Bearer token for LAN chat, tools, and MCP.',
    'settings.generate': 'Generate',
    'settings.clear': 'Clear',
    'settings.toolExposure': 'Remote Tools',
    'settings.toolExposureSub': 'Choose how much phone capability LAN tools may see.',
    'settings.toolRead': 'Read',
    'settings.toolSensor': 'Sensor',
    'settings.toolAction': 'Action',
    'settings.defaultTokens': 'Default',
    'settings.modelTokens': 'Model cap',
    'settings.effectiveTokens': 'Effective',
    'settings.sessionsTitle': 'Sessions',
    'settings.sessionsSub': 'Current API chat session and recent sessions.',
    'settings.sessionsIntro': 'Choose which persistent session new API chat turns write to.',
    'settings.currentSession': 'Current Session',
    'settings.newSession': 'New',
    'settings.diagnosticsTitle': 'Diagnostics',
    'settings.diagnosticsSub': 'Queue, requests, token caps, and recent errors.',
    'settings.diagnosticsIntro': 'Runtime state and recent API request history.',
    'settings.noSessions': 'No sessions',
    'settings.noRequests': 'No request records',
    'settings.battery': 'Battery Optimization',
    'settings.checking': 'Checking status...',
    'settings.runtimeScope': 'For stable inference, keep the runtime window visible while the screen is on. Lock-screen behavior still depends on Android policy.',
    'settings.batteryAllowed': 'Allowed. Background runtime is less likely to be stopped by the system.',
    'settings.batteryRestricted': 'Restricted. Some Android vendor systems may aggressively stop background runtime.',
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
    'models.localTitle': 'Local Models',
    'models.localSub': 'Manage installed models and import local packages.',
    'models.runtimeTitle': 'Runtime Settings',
    'models.runtimeSub': 'Default model, CPU threads, and API server settings.',
    'models.marketTitle': 'Model Market',
    'models.marketSub': 'Search ModelScope MNN models.',
    'models.marketSearch': 'Search models',
    'models.install': 'Install',
    'models.installed': 'Installed',
    'models.installing': 'Installing model',
    'nodes.placeholder': 'Multi-node discovery, connection status, and collaborative tasks will live here. Placeholder for this release.',
    'toast.modelsReloaded': 'Model list refreshed',
    'toast.modelMarketLoaded': 'Model market loaded',
    'toast.modelMarketFailed': 'Model market load failed',
    'toast.scenesReloaded': 'Scene list refreshed',
    'toast.sceneInstalled': 'Scene pack installed',
    'toast.installFailed': 'Install failed',
    'toast.modelImported': 'Model imported',
    'toast.modelImportFailed': 'Model import failed',
    'empty.scenes': 'No scene packs available',
    'empty.models': 'No models available',
    'models.delete': 'Delete',
    'toast.modelDeleted': 'Model deleted',
    'toast.modelDeleteFailed': 'Model delete failed',
    'toast.sceneUninstalled': 'Scene pack uninstalled',
    'toast.sceneUninstallFailed': 'Uninstall failed',
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
    'state.background': '后台运行',
    'settings.language': '语言',
    'settings.languageSub': '跟随系统或选择显示语言。',
    'settings.followSystem': '系统',
    'settings.runtimeTitle': 'Runtime 与后台',
    'settings.runtimeSub': '前台服务、悬浮窗和模型服务。',
    'settings.runtimeIntro': '控制 Lociant 作为后台本地 AI 能力 runtime。',
    'settings.modelServer': '模型服务',
    'settings.runtimeDefaultMessage': '前台服务暴露 LAN API；悬浮窗保持推理可见。',
    'settings.serverTitle': '服务器',
    'settings.serverSub': '端口、输出 Tokens、API Token 和 URL。',
    'settings.serverIntro': '配置本地 OpenAI/Ollama-compatible API 服务。',
    'settings.capabilitiesTitle': '能力',
    'settings.capabilitiesSub': '视觉、悬浮窗和远程工具暴露。',
    'settings.capabilitiesIntro': '管理场景、agent 和 Runtime Window 可见的手机端能力。',
    'settings.visionTitle': '视觉',
    'settings.visionSub': '摄像头分析与物体检测。',
    'settings.visionState': '视觉',
    'settings.visionReady': '就绪',
    'settings.visionStarting': '启动中',
    'settings.visionUnavailable': '不可用',
    'settings.port': '端口',
    'settings.portSub': '修改端口需要重启服务。',
    'settings.windowTitle': '悬浮窗',
    'settings.windowSub': '服务运行时在其他应用上方显示迷你悬浮窗。',
    'settings.windowAuto': '自动',
    'settings.windowShow': '显示',
    'settings.windowHide': '隐藏',
    'settings.windowPermission': '权限',
    'settings.windowAllowed': '已允许',
    'settings.windowPermissionRequired': '需要悬浮窗权限',
    'settings.windowStateVisible': '已显示',
    'settings.windowStateCollapsed': '已折叠',
    'settings.windowStateHidden': '已隐藏',
    'settings.windowStateError': '错误',
    'settings.windowStateWindow': '窗口',
    'settings.defaultModelTitle': '默认模型',
    'settings.defaultModelSub': '选择模型服务默认使用的模型。',
    'settings.defaultModelIntro': '这里设置 OpenAI 和 Ollama 请求在未显式指定 model 时使用的默认模型。',
    'settings.defaultModelNote': '这里只显示已就绪的本地模型。',
    'settings.selected': '当前',
    'settings.cpuThreads': 'CPU 线程数',
    'settings.cpuThreadsSub': '覆盖模型 config.json 中的 thread_num。修改后会重新加载模型。',
    'settings.cpuThreadsShort': 'CPU 线程',
    'settings.outputTokens': '输出 Tokens',
    'settings.outputTokensSub': '请求未指定 max_tokens 时使用的默认上限。',
    'settings.apiToken': 'API Token',
    'settings.apiTokenSub': '局域网 chat、tools 和 MCP 需要 Authorization: Bearer token。',
    'settings.generate': '生成',
    'settings.clear': '清除',
    'settings.toolExposure': '远程工具',
    'settings.toolExposureSub': '选择局域网工具可见的手机能力范围。',
    'settings.toolRead': '只读',
    'settings.toolSensor': '感知',
    'settings.toolAction': '动作',
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
    'settings.runtimeScope': '稳定推理建议在亮屏时使用悬浮窗。锁屏继续运行仍取决于系统策略。',
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
    'models.localTitle': '本地模型',
    'models.localSub': '管理已安装模型、导入本地包。',
    'models.runtimeTitle': 'Runtime 设置',
    'models.runtimeSub': '默认模型、线程数和 API 服务配置。',
    'models.marketTitle': '魔塔模型市场',
    'models.marketSub': '搜索 ModelScope MNN 模型。',
    'models.marketSearch': '搜索模型',
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
    'toast.sceneUninstalled': '场景包已卸载',
    'toast.sceneUninstallFailed': '卸载失败',
    'toast.visionStarted': '视觉分析已启动'
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


/* === 02-dom.js === */
/* ── Lociant WebUI — DOM references and state variables ── */

// ---- DOM refs ----
const app = document.getElementById('app')
const clock = document.getElementById('clock')
const stateText = document.getElementById('stateText')
const stateDot = document.getElementById('stateDot')
const navItems = Array.from(document.querySelectorAll('.nav-item'))
const panels = Array.from(document.querySelectorAll('.panel'))
const sceneHost = document.getElementById('sceneHost')
const sceneFrame = document.getElementById('sceneFrame')
const sceneList = document.getElementById('sceneList')
const modelHomeView = document.getElementById('modelHomeView')
const modelLocalView = document.getElementById('modelLocalView')
const modelLocalButton = document.getElementById('modelLocalButton')
const modelLocalBack = document.getElementById('modelLocalBack')
const modelLocalState = document.getElementById('modelLocalState')
const modelRuntimeButton = document.getElementById('modelRuntimeButton')
const modelRuntimeState = document.getElementById('modelRuntimeState')
const modelList = document.getElementById('modelList')
const modelMarketPanel = document.getElementById('modelMarketPanel')
const modelMarketBack = document.getElementById('modelMarketBack')
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
const runtimeWindowText = document.getElementById('runtimeWindowText')
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
const runtimeServiceToggle =
  document.getElementById('runtimeServiceToggle')
const runtimeServiceMessage =
  document.getElementById('runtimeServiceMessage')
const runtimeServerButton = document.getElementById('runtimeServerButton')
const runtimeServerState = document.getElementById('runtimeServerState')
const runtimeServerPanel = document.getElementById('runtimeServerPanel')
const runtimeServerBack = document.getElementById('runtimeServerBack')
const runtimePortInput = document.getElementById('runtimePortInput')
const runtimeMaxTokensInput = document.getElementById('runtimeMaxTokensInput')
const runtimeAuthTokenInput = document.getElementById('runtimeAuthTokenInput')
const runtimeAuthGenerateButton = document.getElementById('runtimeAuthGenerateButton')
const runtimeAuthClearButton = document.getElementById('runtimeAuthClearButton')
const diagUrl = document.getElementById('diagUrl')
const diagApi = document.getElementById('diagApi')
const runtimeCapabilitiesButton = document.getElementById('runtimeCapabilitiesButton')
const runtimeCapabilitiesState = document.getElementById('runtimeCapabilitiesState')
const runtimeCapabilitiesPanel = document.getElementById('runtimeCapabilitiesPanel')
const runtimeCapabilitiesBack = document.getElementById('runtimeCapabilitiesBack')
const runtimeVisionText = document.getElementById('runtimeVisionText')
const runtimeVisionButton = document.getElementById('runtimeVisionButton')
const runtimeToolExposureInput = document.getElementById('runtimeToolExposureInput')
const runtimeWindowAutoInput = document.getElementById('runtimeWindowAutoInput')
const runtimeWindowButton = document.getElementById('runtimeWindowButton')
const runtimeWindowPermissionButton = document.getElementById('runtimeWindowPermissionButton')
const runtimeModelButton = document.getElementById('runtimeModelButton')
const runtimeModelPanel = document.getElementById('runtimeModelPanel')
const runtimeModelBack = document.getElementById('runtimeModelBack')
const runtimeModelList = document.getElementById('runtimeModelList')
const runtimeModelState = document.getElementById('runtimeModelState')
const runtimeModelNote = document.getElementById('runtimeModelNote')
const runtimeSessionsButton = document.getElementById('runtimeSessionsButton')
const runtimeSessionsState = document.getElementById('runtimeSessionsState')
const runtimeSessionsPanel = document.getElementById('runtimeSessionsPanel')
const runtimeSessionsBack = document.getElementById('runtimeSessionsBack')
const runtimeSessionCurrent = document.getElementById('runtimeSessionCurrent')
const runtimeSessionList = document.getElementById('runtimeSessionList')
const runtimeSessionNewButton = document.getElementById('runtimeSessionNewButton')
const runtimeDiagnosticsButton = document.getElementById('runtimeDiagnosticsButton')
const runtimeDiagnosticsState = document.getElementById('runtimeDiagnosticsState')
const runtimeDiagnosticsPanel = document.getElementById('runtimeDiagnosticsPanel')
const runtimeDiagnosticsBack = document.getElementById('runtimeDiagnosticsBack')
const runtimeCpuThreadsInput = document.getElementById('runtimeCpuThreadsInput')
const languageControl = document.getElementById('languageControl')
const batteryOptimizationButton = document.getElementById('batteryOptimizationButton')
const batteryOptimizationText = document.getElementById('batteryOptimizationText')
const toast = document.getElementById('toast')
const runtimeDefaultTokens = document.getElementById('runtimeDefaultTokens')
const runtimeModelTokens = document.getElementById('runtimeModelTokens')
const runtimeEffectiveTokens = document.getElementById('runtimeEffectiveTokens')

// ---- State variables ----
let runtimeServiceState = null
let runtimeSnapshot = null
let runtimeModels = []
let modelView = 'home'
let marketModels = []
let marketQuery = ''
let marketInstallTimer = null
let marketInstallingModelId = ''
let marketSearchTimer = null
let localeSetting = { mode: 'system' }
let currentLocale = 'en'
let activeAlert = null
const localeStorePath = '/v1/store/runtime-settings/locale'
let scenes = []
let activeScene = null
let cameraPreviewRect = null

// ---- DOM helpers ----
function el(tag, className, text) {
  const node = document.createElement(tag)
  if (className) node.className = className
  if (text !== undefined) node.textContent = text
  return node
}

function emptyCard(text) {
  return el('div', 'empty-card', text)
}

const reliableTimers = new Map()

// ---- Surface switching ----
function animateSurface(node) {
  if (!node) return
  node.classList.remove('animate-in')
  void node.offsetWidth
  node.classList.add('animate-in')
}

function showPanel(panel) {
  if (!panel) return
  sceneHost.classList.remove('active', 'animate-in')
  panels.forEach(item => {
    const active = item === panel
    item.classList.toggle('active', active)
    if (!active) item.classList.remove('animate-in')
  })
  animateSurface(panel)
}

function showPagePanel(page) {
  showPanel(document.getElementById('page-' + page))
}

function hidePanels() {
  panels.forEach(panel => panel.classList.remove('active', 'animate-in'))
}

function showSceneHost() {
  hidePanels()
  sceneHost.classList.add('active')
  animateSurface(sceneHost)
}


/* === 03-api.js === */
/* ── Lociant WebUI — API client and native bridge ── */

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
  const headers = { 'Content-Type': 'application/json' }
  if (runtimeServiceState && runtimeServiceState.authToken) headers.Authorization = 'Bearer ' + runtimeServiceState.authToken
  const response = await fetch(apiUrl(path), {
    method,
    headers,
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

function sceneApiClient() {
  const baseUrl = localApiBaseUrl()
  const headers = () => {
    const output = { 'Content-Type': 'application/json' }
    if (runtimeServiceState && runtimeServiceState.authToken) output.Authorization = 'Bearer ' + runtimeServiceState.authToken
    return output
  }
  const request = (path, body) => fetch(baseUrl + path, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify(body || {})
  }).then(response => response.json().then(json => {
    if (!response.ok || !json.ok) throw new Error((json.error && json.error.message) || json.message || 'API request failed')
    return json.result || json
  }))
  return {
    baseUrl,
    get authToken() {
      return (runtimeServiceState && runtimeServiceState.authToken) || ''
    },
    get(path) {
      return fetch(baseUrl + path, { headers: headers() }).then(response => response.json())
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
        headers: headers(),
        body: JSON.stringify(requestBody || {})
      }).then(response => response.json())
    },
    runtime(command, payload) {
      return shellCommand(command, payload || {})
    },
    window(command) {
      return runtimeWindowCommand(command || 'show')
    }
  }
}

function publishSceneApiClient() {
  const api = sceneApiClient()
  window.MNNodeAPI = api
  return api
}


/* === 04-runtime.js === */
/* ── Lociant WebUI — Runtime state management and commands ── */

function runtimeApiCommand(command, payload) {
  try {
    const body = Object.assign({}, payload || {}, {
      sceneId: (payload && payload.sceneId) || (activeScene && activeScene.id) || ''
    })
    const runShell = ['start', 'stop', 'status', 'battery.requestExemption',
      'window.show', 'window.hide', 'window.collapse', 'window.expand',
      'window.settings', 'window.permission', 'vision.start', 'vision.stop', 'vision.status'
    ].includes(command)
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

function runtimeWindowCommand(command) {
  const next = command || ((runtimeServiceState && runtimeServiceState.windowVisible) ? 'hide' : 'show')
  const state = shellCommand('window.' + next, {})
  updateRuntimeServiceState(state)
  return state.window || state
}

function updateRuntimeServiceState(state) {
  runtimeServiceState = Object.assign({}, runtimeServiceState || {}, state || {})
  publishSceneApiClient()
  const running = !!runtimeServiceState.running
  const starting = !!runtimeServiceState.starting
  if (stateDot) stateDot.classList.toggle('running', running || starting)
  if (stateText) stateText.classList.toggle('running', running || starting)
  runtimeSettingsState.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
  runtimeSettingsState.classList.toggle('running', running || starting)
  runtimeServiceToggle.textContent = running || starting ? t('common.stop') : t('common.start')
  runtimeServiceMessage.textContent = runtimeServiceState.message || t('settings.runtimeDefaultMessage')
  runtimeServerState.textContent = runtimeServiceState.port ? String(runtimeServiceState.port) : '--'
  const vision = visionState()
  const visionRunning = !!vision.running
  const visionStarting = String(vision.state || '').toLowerCase() === 'starting'
  const visionLabel = visionStateLabel(vision)
  runtimeCapabilitiesState.textContent = visionLabel + ' · ' + runtimeWindowLabel()
  runtimeCapabilitiesState.classList.toggle('running', visionRunning || visionStarting)
  runtimeVisionText.textContent = vision.message || (
    visionRunning
      ? (Math.round(Number(vision.fps) || 0) + ' fps · ' + ((((vision.lastDetection || {}).detections) || []).length || 0) + ' detections')
      : t('settings.visionSub')
  )
  runtimeVisionButton.textContent = visionRunning ? t('common.stop') : t('common.start')
  runtimeWindowAutoInput.checked = !!runtimeServiceState.windowAutoShow
  runtimeWindowAutoInput.disabled = !running || !runtimeServiceState.port
  runtimeWindowButton.textContent =
    runtimeServiceState.windowVisible ? t('settings.windowHide') : t('settings.windowShow')
  if (runtimeServiceState.windowAllowed !== undefined) {
    runtimeWindowPermissionButton.style.display = runtimeServiceState.windowAllowed ? 'none' : ''
  }
  runtimeModelState.textContent = (runtimeServiceState && runtimeServiceState.modelId) || '--'
  runtimePortInput.value = runtimeServiceState.port || ''
  runtimeMaxTokensInput.value = runtimeServiceState.maxOutputTokens || ''
  if (runtimeServiceState.authToken !== undefined) {
    if (runtimeAuthTokenInput.value !== runtimeServiceState.authToken) {
      runtimeAuthTokenInput.value = runtimeServiceState.authToken || ''
    }
  }
  if (runtimeServiceState.toolExposure) runtimeToolExposureInput.value = runtimeServiceState.toolExposure
  if (runtimeServiceState.cpuThreads) runtimeCpuThreadsInput.value = runtimeServiceState.cpuThreads
  if (runtimeServiceState.currentSessionId) {
    runtimeSessionCurrent.textContent = runtimeServiceState.currentSessionId
  }
  if (runtimeServiceState.port && runtimeServiceState.url) {
    diagUrl.textContent = runtimeServiceState.url.replace('0.0.0.0', localApiBaseUrl().split(':')[0] || '127.0.0.1')
  }
  if (runtimeServiceState.running !== undefined) {
    diagApi.textContent = running ? t('status.running') : (starting ? t('status.starting') : t('status.stopped'))
  }
  const hard = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
  const effective = runtimeServiceState.effectiveMaxOutputTokens || '--'
  const defaultVal = runtimeServiceState.defaultOutputTokens || '--'
  const modelVal = runtimeServiceState.modelMaxOutputTokens || '--'
  if (runtimeDefaultTokens) runtimeDefaultTokens.textContent = String(defaultVal)
  if (runtimeModelTokens) runtimeModelTokens.textContent = String(modelVal)
  if (runtimeEffectiveTokens) runtimeEffectiveTokens.textContent = String(effective)
  if (runtimeServiceState.sessions) renderSessions(runtimeServiceState.sessions)
  if (runtimeServiceState.requestCount !== undefined || runtimeServiceState.recentRequests) {
    updateDiagnostics(runtimeServiceState)
  }
  batteryAction(runtimeServiceState)
  updateRuntimeStrip()
}

function visionState() {
  return (runtimeServiceState && runtimeServiceState.vision) || {}
}

function visionStateLabel(vision) {
  const current = vision || {}
  const state = String(current.state || '').toLowerCase()
  if (current.running || state === 'running') return t('status.running')
  if (state === 'starting') return t('settings.visionStarting')
  if (state === 'unavailable') return t('settings.visionUnavailable')
  if (state === 'idle') return t('status.stopped')
  return state || t('status.stopped')
}

function runtimeWindowLabel(state) {
  const value = state || (runtimeServiceState && runtimeServiceState.windowState) || 'hidden'
  return ({
    expanded: t('settings.windowStateVisible'),
    collapsed: t('settings.windowStateCollapsed'),
    hidden: t('settings.windowStateHidden'),
    error: t('settings.windowStateError')
  })[value] || t('settings.windowStateWindow')
}

function runtimeStateLabel(state) {
  if (state === 'away') return 'Away'
  if (state === 'distracted' || state === 'phone') return 'Distracted'
  if (state === 'focused') return 'Focused'
  return state || t('state.idle')
}

function fmtDuration(ms) {
  const total = Math.max(0, Math.floor((Number(ms) || 0) / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  if (h > 0) return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
  return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
}

function sceneById(sceneId) {
  return scenes.find(scene => scene.id === sceneId) || null
}

function runtimeScene() {
  if (!runtimeSnapshot || !runtimeSnapshot.sceneId) return null
  return scenes.find(scene => scene.id === runtimeSnapshot.sceneId) || null
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
  runtimeWindowText.textContent = runtimeWindowLabel()
  runtimeWindowText.classList.toggle('active', !!(runtimeServiceState && runtimeServiceState.windowVisible))
  runtimeStateText.textContent = runtimeStateLabel(runtimeSnapshot.state)
  runtimeElapsedText.textContent = fmtDuration(runtimeSnapshot.elapsedMs)
  runtimeEventText.textContent = latest
    ? ((latest.name || latest.ruleId || 'event') + ' · ' + new Date(latest.timestamp || Date.now()).toLocaleTimeString('zh-CN', { hour12: false }))
    : ((runtimeSnapshot.running || runtimeSnapshot.sessionState === 'running') ? t('state.background') : 'Paused')
}

// ---- Battery ----
function batteryAction(state) {
  if (!batteryOptimizationText) return
  if (!state || !state.running) {
    batteryOptimizationText.textContent = t('settings.checking')
    batteryOptimizationButton.style.display = 'none'
    return
  }
  if (state.batteryExempted) {
    batteryOptimizationText.textContent = t('settings.batteryAllowed')
    batteryOptimizationButton.style.display = 'none'
    return
  }
  batteryOptimizationText.textContent = t('settings.batteryRestricted')
  batteryOptimizationButton.style.display = ''
}


/* === 05-scenes.js === */
/* ── Lociant WebUI — Scene management ── */

function loadScenes() {
  retryApi(() => apiGet('/v1/scenes'), () => []).then(data => {
    scenes = Array.isArray(data) ? data : (Array.isArray(data && data.scenes) ? data.scenes : [])
    renderScenes(scenes)
  })
}

function renderScenes(scenes) {
  sceneList.innerHTML = ''
  if (!scenes.length) {
    sceneList.appendChild(emptyCard(t('empty.scenes')))
    return
  }
  scenes.forEach(scene => {
    const card = document.createElement('button')
    card.type = 'button'
    card.className = 'scene-card pressable'
    card.addEventListener('click', () => openScene(scene))

    const icon = el('div', 'scene-icon')
    icon.innerHTML = sceneIconSvg()
    const body = document.createElement('div')
    body.className = 'scene-body'
    const name = el('div', 'scene-name', scene.name || scene.id)
    const source = el('div', 'scene-source', scene.source || '')

    const actions = document.createElement('div')
    actions.className = 'scene-actions'
    if (scene.source === 'installed') {
      const uninstall = el('button', 'scene-uninstall', '✕')
      uninstall.type = 'button'
      uninstall.title = t('toast.sceneUninstalled')
      uninstall.addEventListener('click', event => {
        event.stopPropagation()
        uninstallScene(scene)
      })
      actions.appendChild(uninstall)
    }

    body.appendChild(name)
    body.appendChild(source)
    card.appendChild(icon)
    card.appendChild(body)
    card.appendChild(actions)
    sceneList.appendChild(card)
  })
}

function sceneIconSvg() {
  return '<svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H20v17H7.5A3.5 3.5 0 0 0 4 22V5.5z"/><path d="M4 5.5A3.5 3.5 0 0 1 7.5 9H20"/></svg>'
}

function sceneEntryUrl(scene) {
  return scene && scene.entryUrl
}

function sceneHasSettings(scene) {
  const capabilities = Array.isArray(scene && scene.capabilities) ? scene.capabilities : []
  return capabilities.includes('settings') || capabilities.includes('vision-settings')
}

function setSceneSettingsVisible(visible) {
  sceneSettingsButton.classList.toggle('visible', !!visible)
}

function openScene(scene) {
  if (!scene || !scene.entryUrl) return
  updateRuntimeServiceState(shellCommand('status', {}))
  backButton.classList.add('active')
  activeScene = scene
  setSceneSettingsVisible(sceneHasSettings(scene))
  showSceneHost()
  sceneHost.scrollTop = 0
  cameraPreviewRect = null
  sceneFrame.style.height = '100%'
  sceneFrame.src = sceneEntryUrl(scene)
  activateRuntime(scene)
  stateText.textContent = t('state.running')
  updateRuntimeStrip()
}

function goHome() {
  unloadSceneFrame()
  backButton.classList.remove('active')
  activeScene = null
  setSceneSettingsVisible(false)
  showPagePanel('scenes')
  const scenesPanel = document.getElementById('page-scenes')
  if (scenesPanel) scenesPanel.scrollTop = 0
  navItems.forEach(item => item.classList.toggle('active', item.dataset.page === 'scenes'))
  stateText.textContent = runtimeSnapshot && runtimeSnapshot.running ? t('state.background') : t('state.idle')
  updateRuntimeStrip()
}

function activateRuntime(scene) {
  const triggers = Array.isArray(scene && scene.triggers) ? scene.triggers : []
  if (triggers.length) {
    apiPost('/v1/scenes/' + encodeURIComponent(scene.id) + '/load', {}).catch(() => {})
  }
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

function toggleSceneSettings() {
  if (!activeScene || !sceneHasSettings(activeScene)) return
  postToScene({ type: 'scene.settings.toggle', sceneId: activeScene.id })
}

// ---- Alert ----
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

// ---- Scene iframe communication ----
function postToScene(message) {
  try {
    if (!sceneFrame.contentWindow) return false
    sceneFrame.contentWindow.postMessage(message, '*')
    return true
  } catch (error) {
    return false
  }
}

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
  void { x: Math.round(cssX * scale.x), y: Math.round(cssY * scale.y),
         width: Math.max(1, Math.round(cameraPreviewRect.width * scale.x)),
         height: Math.max(1, Math.round(cameraPreviewRect.height * scale.y)) }
}

function nativeViewportScale() {
  const ratio = window.devicePixelRatio || 1
  return { x: ratio, y: ratio }
}


/* === 06-models.js === */
/* ── Lociant WebUI — Model management ── */

function setModelView(view) {
  modelView = view || 'home'
  const views = { home: modelHomeView, local: modelLocalView, market: modelMarketPanel }
  Object.keys(views).forEach(key => {
    const node = views[key]
    if (!node) return
    const active = key === modelView
    node.classList.toggle('active', active)
    node.setAttribute('aria-hidden', active ? 'false' : 'true')
  })
  if (modelView === 'local') loadModels()
  if (modelView === 'market') {
    renderModelMarket(marketModels)
    if (!marketModels.length) loadModelMarket()
  }
}

function loadModels() {
  retryApi(() => apiGet('/v1/models/full'), []).then(data => {
    runtimeModels = Array.isArray(data) ? data : []
    renderModels(runtimeModels)
    updateModelHomeState()
  })
}

function renderModels(models) {
  modelList.innerHTML = ''
  if (!Array.isArray(models) || !models.length) {
    modelList.appendChild(emptyCard(t('empty.models')))
    return
  }
  models.forEach(model => {
    const name = model.name || model.id || '--'
    const id = model.id || ''
    const runtime = model.runtime || ''
    const type = model.type || ''
    const ready = model.ready
    const installed = model.installed

    const row = document.createElement('div')
    row.className = 'model-row'

    const icon = el('div', 'model-icon', ready ? '✓' : '○')
    const body = document.createElement('div')
    body.className = 'model-body'
    const title = el('div', 'model-title', name)
    const tags = document.createElement('div')
    tags.className = 'model-tags'
    if (runtime) tags.appendChild(el('span', 'model-tag', runtime))
    if (type) tags.appendChild(el('span', 'model-tag', type))
    if (!installed) tags.appendChild(el('span', 'model-tag', 'built-in'))

    const actions = document.createElement('div')
    actions.className = 'model-actions'
    if (ready) {
      actions.appendChild(el('span', 'model-ready', t('settings.visionReady')))
    } else if (installed) {
      const missing = model.missingFiles
      if (Array.isArray(missing) && missing.length) {
        actions.appendChild(el('span', 'model-missing', missing.join(', ')))
      } else {
        actions.appendChild(el('span', 'model-missing', t('empty.models')))
      }
    } else {
      actions.appendChild(el('span', 'model-not-ready', ''))
    }
    if (installed) {
      const del = document.createElement('button')
      del.type = 'button'
      del.className = 'model-delete pressable'
      del.textContent = t('models.delete')
      del.addEventListener('click', event => {
        event.stopPropagation()
        if (model.id) deleteModel(model.id)
      })
      row.appendChild(del)
    }

    body.appendChild(title)
    body.appendChild(tags)
    row.appendChild(icon)
    row.appendChild(body)
    row.appendChild(actions)
    modelList.appendChild(row)
  })
}

function updateModelHomeState() {
  const readyModels = runtimeModels.filter(model => model && model.ready)
  if (modelLocalState) modelLocalState.textContent = String(readyModels.length)
  if (modelRuntimeState) modelRuntimeState.textContent = (runtimeServiceState && runtimeServiceState.modelId) || '--'
}

function deleteModel(modelId) {
  if (!modelId) return
  apiPost('/v1/models/' + encodeURIComponent(modelId) + '/delete', {})
    .then(result => {
      if (result.ok) {
        loadModels()
        showToast(t('toast.modelDeleted'))
      } else {
        showToast(result.message || t('toast.modelDeleteFailed'))
      }
    })
    .catch(() => showToast(t('toast.modelDeleteFailed')))
}

// ---- Model Market ----
function loadModelMarket(forceRefresh) {
  const query = marketQuery
  const url = '/v1/models/market' + (query ? '?q=' + encodeURIComponent(query) : '') + (forceRefresh ? (query ? '&refresh=true' : '?refresh=true') : '')
  apiGet(url)
    .then(data => {
      marketModels = (data && Array.isArray(data.models)) ? data.models : []
      renderModelMarket(marketModels)
      showToast(t('toast.modelMarketLoaded'))
    })
    .catch(() => showToast(t('toast.modelMarketFailed')))
}

function renderModelMarket(models) {
  modelMarketList.innerHTML = ''
  if (!Array.isArray(models) || !models.length) {
    modelMarketList.appendChild(emptyCard(t('empty.models')))
    return
  }
  models.forEach(model => {
    const row = document.createElement('div')
    row.className = 'model-market-row'
    const body = document.createElement('div')
    body.className = 'model-market-body'
    const name = el('div', 'model-market-name', model.name || model.id || '--')
    const desc = el('div', 'model-market-desc', (model.description || '').slice(0, 120))
    const actions = document.createElement('div')
    actions.className = 'model-market-actions'
    const install = document.createElement('button')
    install.type = 'button'
    install.className = 'model-market-install pressable'
    install.textContent = isMarketModelInstalled(model)
      ? t('models.installed')
      : (marketInstallingModelId === model.id ? t('models.installing') : t('models.install'))
    install.disabled = isMarketModelInstalled(model) || marketInstallingModelId === model.id
    install.addEventListener('click', () => installMarketModel(model))
    actions.appendChild(install)
    body.appendChild(name)
    body.appendChild(desc)
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

function pollMarketInstall(modelId) {
  if (marketInstallTimer) window.clearInterval(marketInstallTimer)
  marketInstallTimer = window.setInterval(() => {
    apiGet('/v1/models/market/' + encodeURIComponent(modelId) + '/progress')
      .then(data => {
        if (data && data.active) {
          setModelProgress(data)
        } else {
          marketInstallingModelId = ''
          setModelProgress({ state: 'done', message: t('toast.modelImported') })
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
          loadModels()
          showToast(t('toast.modelImported'))
        }
      })
      .catch(() => {
        marketInstallingModelId = ''
        window.clearInterval(marketInstallTimer)
        marketInstallTimer = null
      })
  }, 2000)
}

function setModelProgress(data) {
  if (!modelProgress) return
  const state = data && data.state
  modelProgress.classList.toggle('active', state === 'installing' || state === 'downloading')
  modelProgress.classList.toggle('error', state === 'error')
  modelProgress.classList.toggle('done', state === 'done')
  modelProgressText.textContent = (data && data.message) || ''
  modelProgressPercent.textContent = data && data.percent ? Math.round(data.percent) + '%' : ''
  modelProgressFill.style.width = (data && data.percent ? Math.min(100, Math.round(data.percent)) : 0) + '%'
}

// ---- Model choice (settings panel) ----
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
    const check = el('div', 'model-choice-check', '✓')
    row.appendChild(body)
    row.appendChild(check)
    row.addEventListener('click', () => {
      runtimeApiCommand('settings', { modelId: model.id })
    })
    runtimeModelList.appendChild(row)
  })
}

function isRuntimeModel(model) {
  const type = (model.type || '').toLowerCase()
  const runtime = (model.runtime || '').toLowerCase()
  return runtime === 'mnn' || type === 'vlm' || type === 'chat' || type === 'llm'
}


/* === 07-settings.js === */
/* ── Lociant WebUI — Settings panels ── */

// ---- Locale ----
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

// ---- Sessions ----
function renderSessions(sessions) {
  if (!runtimeSessionList) return
  runtimeSessionList.innerHTML = ''
  const items = Array.isArray(sessions) ? sessions : []
  if (!items.length) {
    runtimeSessionList.appendChild(emptyCard(t('settings.noSessions')))
    return
  }
  items.forEach(session => {
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'model-choice-row pressable'
    row.classList.toggle('active', session.id === (runtimeServiceState && runtimeServiceState.currentSessionId))
    const body = document.createElement('div')
    body.className = 'model-choice-body'
    const title = el('div', 'model-choice-title', session.title || session.id || '--')
    const sub = el('div', 'model-choice-sub', (session.modelId || '--') + ' · ' + (session.messageCount || 0) + ' messages')
    body.appendChild(title)
    body.appendChild(sub)
    const check = el('div', 'model-choice-check', '✓')
    row.appendChild(body)
    row.appendChild(check)
    row.addEventListener('click', () => {
      runtimeApiCommand('session.select', { sessionId: session.id })
    })
    runtimeSessionList.appendChild(row)
  })
}

// ---- Diagnostics ----
function updateDiagnostics(state) {
  const log = document.getElementById('runtimeDiagLog')
  if (!log) return
  log.innerHTML = ''
  const requests = Array.isArray(state.recentRequests) ? state.recentRequests : []
  if (!requests.length) {
    log.appendChild(emptyCard(t('settings.noRequests')))
    return
  }
  requests.forEach(req => {
    const row = document.createElement('div')
    row.className = 'diag-row'
    row.innerHTML = '<span class="diag-method">' + (req.method || '') + '</span>' +
      '<span class="diag-path">' + (req.endpoint || '') + '</span>' +
      '<span class="diag-status diag-' + (req.status < 400 ? 'ok' : 'err') + '">' + (req.status || '') + '</span>' +
      '<span class="diag-time">' + (req.elapsedMs || 0) + 'ms</span>'
    log.appendChild(row)
  })
}

// ---- Settings navigation ----
function showSettingsHome() {
  ensureSettingsDetailClosed()
  settingsList.setAttribute('aria-hidden', 'false')
}

function showSettingsDetail(panel) {
  ensureSettingsDetailClosed()
  settingsList.setAttribute('aria-hidden', 'true')
  panel.classList.add('active')
  panel.setAttribute('aria-hidden', 'false')
}

function openRuntimeSettings() {
  showSettingsDetail(runtimeSettingsPanel)
}

function closeRuntimeSettings() {
  showSettingsHome()
  updateRuntimeServiceState(runtimeServiceState || {})
}

function closeRuntimeSettingsBack() {
  closeRuntimeSettings()
}

function ensureSettingsDetailClosed() {
  runtimeDetails().forEach(p => { p.classList.remove('active'); p.setAttribute('aria-hidden', 'true') })
}

function openRuntimeServerSettings() {
  showSettingsDetail(runtimeServerPanel)
}

function backToRuntimeSettings() {
  showSettingsHome()
  updateRuntimeServiceState(runtimeServiceState || {})
}

function openRuntimeCapabilitiesSettings() {
  showSettingsDetail(runtimeCapabilitiesPanel)
}

function openRuntimeModelSettings() {
  showSettingsDetail(runtimeModelPanel)
  renderRuntimeModelChoices(runtimeModels)
}

function openRuntimeSessionsSettings() {
  showSettingsDetail(runtimeSessionsPanel)
}

function openRuntimeDiagnosticsSettings() {
  showSettingsDetail(runtimeDiagnosticsPanel)
}


/* === 08-ui.js === */
/* ── Lociant WebUI — UI components ── */

// ---- Sidebar ----
let sidebarBusy = false

function clearPress(target) {
  if (target) target.classList.remove('is-pressed')
}

function toggleSidebar(event) {
  if (event) event.preventDefault()
  if (window.matchMedia('(orientation: portrait), (max-width: 760px)').matches) return
  if (sidebarBusy) return
  sidebarBusy = true
  menuButton.classList.add('is-busy')
  app.classList.toggle('collapsed')
  window.setTimeout(() => {
    sidebarBusy = false
    menuButton.classList.remove('is-busy')
  }, 290)
}

// ---- Toast ----
function showToast(text) {
  toast.textContent = text
  toast.classList.add('active')
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => toast.classList.remove('active'), 2200)
}

// ---- Navigation ----
function navigateTo(page) {
  navItems.forEach(i => i.classList.toggle('active', i.dataset.page === page))
  unloadSceneFrame()
  backButton.classList.remove('active')
  activeScene = null
  setSceneSettingsVisible(false)
  showPagePanel(page)
  const activePanel = document.getElementById('page-' + page)
  if (activePanel) activePanel.scrollTop = 0
  if (page === 'models') {
    setModelView(modelView)
    loadModels()
  }
  stateText.textContent = t('state.idle')
  updateRuntimeStrip()
}

// ---- Clock tick ----
function tick() {
  clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  syncRuntimeSnapshot({ notifyScene: false })
  updateRuntimeStrip()
}

// ---- Runtime snapshot polling ----
let runtimePollTimer = null

function refreshRuntimeServiceState() {
  const state = shellCommand('status', {})
  updateRuntimeServiceState(state)
  runtimeSnapshot = state
  if (!runtimePollTimer) {
    runtimePollTimer = window.setInterval(() => {
      const next = shellCommand('status', {})
      const changed = JSON.stringify(next) !== JSON.stringify(runtimeSnapshot)
      if (changed || activeScene) {
        updateRuntimeServiceState(next)
        runtimeSnapshot = next
      }
    }, 4000)
  }
}

function syncRuntimeSnapshot(options) {
  const notify = options && options.notifyScene !== false
  const next = shellCommand('status', {})
  const changed = JSON.stringify(next) !== JSON.stringify(runtimeSnapshot)
  if (changed) {
    updateRuntimeServiceState(next)
    runtimeSnapshot = next
  }
  if (notify && activeScene) {
    postToSceneReliable({ type: 'runtime.snapshot', snapshot: runtimeSnapshot }, 3, 'runtime-snapshot')
  }
}

// ---- Runtime message handler ----
function handleRuntimeMessage(message) {
  if (!message) return
  if (message.type === 'alert') {
    showAlert(message)
    return
  }
  if (message.type === 'runtime.snapshot' && message.snapshot) {
    runtimeSnapshot = message.snapshot
    updateRuntimeServiceState(runtimeSnapshot)
    return
  }
  updateRuntimeServiceState(message)
}

function sendRuntimeCommand(sceneId, command, payload) {
  runtimeServiceCommand(command, Object.assign({}, payload, { sceneId: sceneId || undefined }))
}


/* === 09-init.js === */
/* ── Lociant WebUI — Event binding and initialization ── */

// ---- Scene frame events ----
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

// ---- Sidebar ----
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

// ---- Navigation ----
navItems.forEach(item => {
  item.addEventListener('click', () => {
    navigateTo(item.dataset.page)
  })
})

// ---- Buttons ----
backButton.addEventListener('click', goHome)
sceneSettingsButton.addEventListener('click', toggleSceneSettings)
alertOpenButton.addEventListener('click', openAlertScene)
alertCloseButton.addEventListener('click', clearAlert)
runtimeStrip.addEventListener('click', () => {
  const scene = runtimeScene()
  if (scene) openScene(scene)
})
runtimeWindowText.addEventListener('click', event => {
  event.stopPropagation()
  runtimeWindowCommand()
})

// ---- Settings navigation ----
runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
runtimeSettingsBack.addEventListener('click', closeRuntimeSettingsBack)
runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
runtimeServerBack.addEventListener('click', backToRuntimeSettings)
runtimeCapabilitiesButton.addEventListener('click', openRuntimeCapabilitiesSettings)
runtimeCapabilitiesBack.addEventListener('click', backToRuntimeSettings)
runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
runtimeModelBack.addEventListener('click', backToRuntimeSettings)
runtimeSessionsButton.addEventListener('click', openRuntimeSessionsSettings)
runtimeSessionsBack.addEventListener('click', backToRuntimeSettings)
runtimeDiagnosticsButton.addEventListener('click', openRuntimeDiagnosticsSettings)
runtimeDiagnosticsBack.addEventListener('click', backToRuntimeSettings)

// ---- Runtime controls ----
runtimeServiceToggle.addEventListener('click', () => {
  const running = runtimeServiceState && (runtimeServiceState.running || runtimeServiceState.starting)
  runtimeApiCommand(running ? 'stop' : 'start', {})
})
runtimeVisionButton.addEventListener('click', () => {
  const vision = visionState()
  runtimeServiceCommand(vision && vision.running ? 'vision.stop' : 'vision.start', {})
})
runtimeWindowAutoInput.addEventListener('change', () => {
  runtimeServiceCommand('window.settings', { autoShow: !!runtimeWindowAutoInput.checked })
})
runtimeWindowButton.addEventListener('click', () => {
  runtimeWindowCommand()
})
runtimeWindowPermissionButton.addEventListener('click', () => {
  runtimeServiceCommand('window.permission', {})
})

// ---- Server settings ----
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
runtimeAuthTokenInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { authToken: runtimeAuthTokenInput.value.trim() })
})
runtimeAuthGenerateButton.addEventListener('click', () => {
  runtimeApiCommand('settings', { generateAuthToken: true })
})
runtimeAuthClearButton.addEventListener('click', () => {
  runtimeAuthTokenInput.value = ''
  runtimeApiCommand('settings', { authToken: '' })
})

// ---- Capabilities settings ----
runtimeToolExposureInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { toolExposure: runtimeToolExposureInput.value || 'action' })
})
runtimeCpuThreadsInput.addEventListener('change', () => {
  const max = Number(runtimeServiceState && runtimeServiceState.maxCpuThreads) || 16
  const value = Math.max(1, Math.min(max, Math.round(Number(runtimeCpuThreadsInput.value) || 4)))
  runtimeCpuThreadsInput.value = String(value)
  runtimeApiCommand('settings', { cpuThreads: value })
})

// ---- Session ----
runtimeSessionNewButton.addEventListener('click', () => {
  runtimeApiCommand('session.create', {})
})

// ---- Battery ----
batteryOptimizationButton.addEventListener('click', () => {
  runtimeServiceCommand('battery.requestExemption', {})
})

// ---- Language ----
languageControl.addEventListener('click', event => {
  const button = event.target.closest('.segmented-option')
  if (button && button.dataset.langMode) saveLocaleSetting(button.dataset.langMode)
})

// ---- Model actions ----
modelReloadButton.addEventListener('click', () => {
  loadModels()
  showToast(t('toast.modelsReloaded'))
})
modelImportButton.addEventListener('click', () => {
  native('installModelPackage')
})
modelLocalButton.addEventListener('click', () => setModelView('local'))
modelLocalBack.addEventListener('click', () => setModelView('home'))
modelMarketButton.addEventListener('click', () => setModelView('market'))
modelMarketBack.addEventListener('click', () => setModelView('home'))
modelRuntimeButton.addEventListener('click', () => {
  navigateTo('settings')
  openRuntimeSettings()
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

// ---- Scene install ----
installButton.addEventListener('click', () => {
  native('installScenePack')
})

// ---- Expose helpers ----
window.MNNodeShellUi = { goHome }

// ---- PostMessage handlers ----
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
  'runtime.command': data => sendRuntimeCommand(data.sceneId || (activeScene && activeScene.id) || '', data.command || '', data.payload || {})
}

window.addEventListener('message', event => {
  const data = event.data || {}
  const handler = messageHandlers[data.type]
  if (handler) handler(data)
})

// ---- Bootstrap ----
refreshRuntimeServiceState()
loadScenes()
loadModels()
loadLocaleSetting()
tick()
