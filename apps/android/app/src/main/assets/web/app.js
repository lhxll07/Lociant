/* === 01-i18n.js === */
/* Lociant WebUI - i18n dictionary */

const i18n = {
  en: {
    'nav.home': 'Home',
    'nav.scenes': 'Scenes',
    'nav.settings': 'Settings',
    'nav.models': 'Models',
    'nav.nodes': 'Nodes',
    'common.back': 'Back',
    'common.open': 'Open',
    'common.refresh': 'Refresh',
    'common.install': 'Install',
    'common.start': 'Start',
    'common.stop': 'Stop',
    'state.idle': 'Idle',
    'state.running': 'Running',
    'state.background': 'Background',
    'status.starting': 'Starting',
    'status.running': 'Running',
    'status.stopped': 'Stopped',

    'home.quickDiagnostics': 'Run diagnostics',
    'home.quickConnection': 'Copy connection',
    'home.quickScenes': 'Open scenes',
    'home.placeholder': 'Ask Lociant, or describe a tool task',
    'home.send': 'Send',
    'home.runtimeLabel': 'Runtime',
    'home.modelLabel': 'Model',
    'home.nodeLabel': 'Node',
    'home.historyTitle': 'Recent chats',
    'home.readyModels': 'ready models',
    'home.emptyReply': 'No reply.',
    'home.thinking': 'Thinking...',
    'home.deleteChat': 'Delete chat',
    'home.uploadImage': 'Upload photo',
    'home.removeImage': 'Remove photo',
    'home.imageAttached': 'Photo attached',

    'page.scenesTitle': 'Scenes',
    'page.scenesSub': 'Run phone-side workflows and capability packs.',
    'page.settingsTitle': 'Settings',
    'page.settingsSub': 'Keep runtime, permissions, and model behavior in one place.',
    'page.modelsTitle': 'Models',
    'page.modelsSub': 'Install, choose, and manage local inference.',
    'page.nodesTitle': 'Nodes',
    'page.nodesSub': 'Manage this device first. Multi-device collaboration can plug in later.',

    'settings.language': 'Language',
    'settings.languageSub': 'Display language',
    'settings.followSystem': 'System',
    'settings.runtimeTitle': 'Runtime',
    'settings.runtimeSub': 'Headless service and window',
    'settings.runtimeIntro': 'Local capability runtime.',
    'settings.modelServer': 'Service',
    'settings.runtimeDefaultMessage': 'Local API is ready when the service is running.',
    'settings.autoStart': 'Start on boot',
    'settings.autoStartSub': 'Restore runtime after restart',
    'settings.serverTitle': 'Server',
    'settings.serverSub': 'Port, token, endpoint',
    'settings.serverIntro': 'OpenAI/Ollama compatible API.',
    'settings.capabilitiesTitle': 'Capabilities',
    'settings.capabilitiesSub': 'Vision and remote tools',
    'settings.capabilitiesIntro': 'Phone capabilities for scenes and agents.',
    'settings.visionTitle': 'Vision',
    'settings.visionSub': 'Camera analysis',
    'settings.visionReady': 'Ready',
    'settings.visionStarting': 'Starting',
    'settings.visionUnavailable': 'Unavailable',
    'settings.port': 'Port',
    'settings.portSub': 'Restart service to apply',
    'settings.windowTitle': 'Floating window',
    'settings.windowSub': 'Optional status surface',
    'settings.windowAuto': 'Auto show',
    'settings.windowAutoSub': 'Show when app leaves foreground',
    'settings.windowShow': 'Show',
    'settings.windowHide': 'Hide',
    'settings.windowRequest': 'Grant',
    'settings.windowPermission': 'Overlay access',
    'settings.windowAllowed': 'Allowed',
    'settings.windowStateVisible': 'Visible',
    'settings.windowStateCollapsed': 'Collapsed',
    'settings.windowStateHidden': 'Hidden',
    'settings.windowStateError': 'Error',
    'settings.windowStateWindow': 'Window',
    'settings.defaultModelTitle': 'Default model',
    'settings.defaultModelSub': 'Model and CPU threads',
    'settings.defaultModelIntro': 'Used when requests omit model.',
    'settings.defaultModelNote': 'Ready models only.',
    'settings.selected': 'Selected',
    'settings.cpuThreads': 'CPU threads',
    'settings.cpuThreadsSub': 'Reloads current model',
    'settings.cpuThreadsShort': 'Threads',
    'settings.outputTokens': 'Output tokens',
    'settings.outputTokensSub': 'Default response cap',
    'settings.apiToken': 'API token',
    'settings.apiTokenSub': 'Bearer auth for LAN access',
    'settings.generate': 'Generate',
    'settings.clear': 'Clear',
    'settings.toolExposure': 'Remote tools',
    'settings.toolExposureSub': 'Visible capability level',
    'settings.toolRead': 'Read',
    'settings.toolSensor': 'Sensor',
    'settings.toolAction': 'Action',
    'settings.defaultTokens': 'Default',
    'settings.modelTokens': 'Model',
    'settings.effectiveTokens': 'Effective',
    'settings.advancedTitle': 'Advanced',
    'settings.advancedSub': 'Sessions and diagnostics',
    'settings.advancedIntro': 'Runtime records and API history.',
    'settings.permissionsTitle': 'Permissions',
    'settings.cameraPermission': 'Camera',
    'settings.notificationPermission': 'Notifications',
    'settings.overlayPermission': 'Overlay',
    'settings.battery': 'Background power',
    'settings.noSessions': 'No sessions',
    'settings.noRequests': 'No requests',
    'settings.checking': 'Checking',
    'settings.batteryAllowed': 'Allowed',
    'settings.batteryRestricted': 'Needs action',
    'settings.cameraPermissionGranted': 'Ready',
    'settings.cameraPermissionNeeded': 'Required',
    'settings.notificationPermissionGranted': 'Ready',
    'settings.notificationPermissionNeeded': 'Required',
    'settings.overlayPermissionGranted': 'Ready',
    'settings.overlayPermissionNeeded': 'Required',
    'settings.allowed': 'Allowed',
    'settings.allow': 'Allow',
    'settings.grant': 'Grant',
    'settings.manage': 'Manage',
    'settings.open': 'Open',
    'settings.request': 'Grant',
    'settings.currentSession': 'Current',
    'settings.newSession': 'New',
    'diagnostics.title': 'Agent Diagnostics',
    'diagnostics.agentTitle': 'Agent connection',
    'diagnostics.agentSub': 'Check runtime, tools, model, and vision readiness',
    'diagnostics.run': 'Run check',
    'diagnostics.running': 'Checking...',
    'diagnostics.ready': 'Ready',
    'diagnostics.issue': 'Needs action',
    'diagnostics.history': 'Recent API',
    'diagnostics.runtime': 'Runtime',
    'diagnostics.tools': 'Tools',
    'diagnostics.model': 'Model',
    'diagnostics.vision': 'Vision',
    'diagnostics.mcp': 'MCP',
    'connection.title': 'Connection',
    'connection.openaiUrl': 'OpenAI Base URL',
    'connection.openaiUrlSub': 'Use this as the client base URL',
    'connection.mcpUrl': 'MCP URL',
    'connection.mcpUrlSub': 'Streamable HTTP endpoint',
    'connection.authHeader': 'Auth header',
    'connection.authHeaderSub': 'Bearer token for protected clients',
    'connection.mcpConfig': 'MCP config',
    'connection.mcpConfigSub': 'Paste into an MCP client profile',
    'connection.testPrompt': 'Test prompt',
    'connection.testPromptSub': 'Quick tool-call smoke test',
    'connection.copy': 'Copy',

    'models.rescan': 'Rescan',
    'models.import': 'Import',
    'models.market': 'Market',
    'models.localTitle': 'Local models',
    'models.localSub': 'Installed packages',
    'models.runtimeTitle': 'Runtime',
    'models.runtimeSub': 'Default model and API',
    'models.marketTitle': 'Model market',
    'models.marketSub': 'ModelScope MNN models',
    'models.marketSearch': 'Search models',
    'models.install': 'Install',
    'models.installed': 'Installed',
    'models.installing': 'Installing',
    'models.delete': 'Delete',

    'nodes.localNode': 'Local node',
    'nodes.localSub': 'This phone is the active capability node.',
    'nodes.connectionTitle': 'Connection endpoint',
    'nodes.connectionSub': 'Start the runtime to expose MCP and OpenAI endpoints.',
    'nodes.placeholder': 'Multi-node discovery and collaboration will expand here.',
    'empty.scenes': 'No scenes yet',
    'empty.models': 'No models yet',
    'toast.modelsReloaded': 'Models refreshed',
    'toast.modelMarketLoaded': 'Market loaded',
    'toast.modelMarketFailed': 'Market failed',
    'toast.scenesReloaded': 'Scenes refreshed',
    'toast.sceneInstalled': 'Scene installed',
    'toast.installFailed': 'Install failed',
    'toast.modelImported': 'Model imported',
    'toast.modelImportFailed': 'Import failed',
    'toast.modelDeleted': 'Model deleted',
    'toast.modelDeleteFailed': 'Delete failed',
    'toast.sceneUninstalled': 'Scene removed',
    'toast.sceneUninstallFailed': 'Remove failed',
    'toast.visionStarted': 'Vision started',
    'toast.copied': 'Copied',
    'toast.copyFailed': 'Copy failed',
  },

  zh: {
    'nav.home': '主页',
    'nav.scenes': '场景',
    'nav.settings': '设置',
    'nav.models': '模型',
    'nav.nodes': '节点',
    'common.back': '返回',
    'common.open': '打开',
    'common.refresh': '刷新',
    'common.install': '安装',
    'common.start': '启动',
    'common.stop': '停止',
    'state.idle': '待机',
    'state.running': '运行中',
    'state.background': '后台',
    'status.starting': '启动中',
    'status.running': '运行中',
    'status.stopped': '已停止',

    'home.quickDiagnostics': '运行诊断',
    'home.quickConnection': '复制连接配置',
    'home.quickScenes': '打开场景',
    'home.placeholder': '问问 Lociant，或输入一个工具调用任务',
    'home.send': '发送',
    'home.runtimeLabel': '运行时',
    'home.modelLabel': '模型',
    'home.nodeLabel': '节点',
    'home.historyTitle': '最近对话',
    'home.readyModels': '个就绪模型',
    'home.emptyReply': '没有回复。',
    'home.thinking': '思考中...',
    'home.deleteChat': '删除对话',
    'home.uploadImage': '上传照片',
    'home.removeImage': '移除照片',
    'home.imageAttached': '已添加照片',

    'page.scenesTitle': '场景',
    'page.scenesSub': '运行手机侧工作流与能力包',
    'page.settingsTitle': '设置',
    'page.settingsSub': '集中管理运行时、权限和模型行为',
    'page.modelsTitle': '模型',
    'page.modelsSub': '安装、选择和管理本地推理',
    'page.nodesTitle': '节点',
    'page.nodesSub': '当前先管理本机节点，后续接入多设备协同。',

    'settings.language': '语言',
    'settings.languageSub': '显示语言',
    'settings.followSystem': '系统',
    'settings.runtimeTitle': '运行时',
    'settings.runtimeSub': '后台服务与悬浮窗',
    'settings.runtimeIntro': '本地能力运行时',
    'settings.modelServer': '服务',
    'settings.runtimeDefaultMessage': '服务运行后即可提供本地 API',
    'settings.autoStart': '开机启动',
    'settings.autoStartSub': '重启后恢复运行时',
    'settings.serverTitle': '服务',
    'settings.serverSub': '端口、令牌、地址',
    'settings.serverIntro': 'OpenAI/Ollama 兼容 API',
    'settings.capabilitiesTitle': '能力',
    'settings.capabilitiesSub': '视觉与远程工具',
    'settings.capabilitiesIntro': '提供给场景和 Agent 的手机能力',
    'settings.visionTitle': '视觉',
    'settings.visionSub': '摄像头分析',
    'settings.visionReady': '就绪',
    'settings.visionStarting': '启动中',
    'settings.visionUnavailable': '不可用',
    'settings.port': '端口',
    'settings.portSub': '重启服务后生效',
    'settings.windowTitle': '悬浮窗',
    'settings.windowSub': '可选状态界面',
    'settings.windowAuto': '自动显示',
    'settings.windowAutoSub': '离开前台时显示',
    'settings.windowShow': '显示',
    'settings.windowHide': '隐藏',
    'settings.windowRequest': '授权',
    'settings.windowPermission': '悬浮窗权限',
    'settings.windowAllowed': '已允许',
    'settings.windowStateVisible': '可见',
    'settings.windowStateCollapsed': '折叠',
    'settings.windowStateHidden': '隐藏',
    'settings.windowStateError': '错误',
    'settings.windowStateWindow': '窗口',
    'settings.defaultModelTitle': '默认模型',
    'settings.defaultModelSub': '模型与 CPU 线程',
    'settings.defaultModelIntro': '请求未指定模型时使用',
    'settings.defaultModelNote': '仅显示已就绪模型',
    'settings.selected': '已选',
    'settings.cpuThreads': 'CPU 线程',
    'settings.cpuThreadsSub': '会重新加载当前模型',
    'settings.cpuThreadsShort': '线程',
    'settings.outputTokens': '输出 Tokens',
    'settings.outputTokensSub': '默认回复上限',
    'settings.apiToken': 'API 令牌',
    'settings.apiTokenSub': '局域网访问认证',
    'settings.generate': '生成',
    'settings.clear': '清除',
    'settings.toolExposure': '远程工具',
    'settings.toolExposureSub': '可见能力等级',
    'settings.toolRead': '只读',
    'settings.toolSensor': '感知',
    'settings.toolAction': '动作',
    'settings.defaultTokens': '默认',
    'settings.modelTokens': '模型',
    'settings.effectiveTokens': '生效',
    'settings.advancedTitle': '高级',
    'settings.advancedSub': '会话与诊断',
    'settings.advancedIntro': '运行记录与 API 历史',
    'settings.permissionsTitle': '权限',
    'settings.cameraPermission': '摄像头',
    'settings.notificationPermission': '通知',
    'settings.overlayPermission': '悬浮窗',
    'settings.battery': '后台电量',
    'settings.noSessions': '暂无会话',
    'settings.noRequests': '暂无请求',
    'settings.checking': '检查中',
    'settings.batteryAllowed': '已允许',
    'settings.batteryRestricted': '需处理',
    'settings.cameraPermissionGranted': '就绪',
    'settings.cameraPermissionNeeded': '需要授权',
    'settings.notificationPermissionGranted': '就绪',
    'settings.notificationPermissionNeeded': '需要授权',
    'settings.overlayPermissionGranted': '就绪',
    'settings.overlayPermissionNeeded': '需要授权',
    'settings.allowed': '已允许',
    'settings.allow': '允许',
    'settings.grant': '授权',
    'settings.manage': '管理',
    'settings.open': '打开',
    'settings.request': '授权',
    'settings.currentSession': '当前',
    'settings.newSession': '新建',
    'diagnostics.title': 'Agent 诊断',
    'diagnostics.agentTitle': 'Agent 连接',
    'diagnostics.agentSub': '检查运行时、工具、模型与视觉状态',
    'diagnostics.run': '开始检查',
    'diagnostics.running': '检查中...',
    'diagnostics.ready': '就绪',
    'diagnostics.issue': '需处理',
    'diagnostics.history': '最近 API',
    'diagnostics.runtime': '运行时',
    'diagnostics.tools': '工具',
    'diagnostics.model': '模型',
    'diagnostics.vision': '视觉',
    'diagnostics.mcp': 'MCP',
    'connection.title': '连接',
    'connection.openaiUrl': 'OpenAI Base URL',
    'connection.openaiUrlSub': '客户端 base_url',
    'connection.mcpUrl': 'MCP URL',
    'connection.mcpUrlSub': 'Streamable HTTP 入口',
    'connection.authHeader': 'Auth Header',
    'connection.authHeaderSub': '受保护客户端使用',
    'connection.mcpConfig': 'MCP 配置',
    'connection.mcpConfigSub': '粘贴到 MCP 客户端',
    'connection.testPrompt': '测试提示词',
    'connection.testPromptSub': '快速验证工具调用',
    'connection.copy': '复制',

    'models.rescan': '扫描',
    'models.import': '导入',
    'models.market': '市场',
    'models.localTitle': '本地模型',
    'models.localSub': '已安装模型包',
    'models.runtimeTitle': '运行时',
    'models.runtimeSub': '默认模型与 API',
    'models.marketTitle': '模型市场',
    'models.marketSub': 'ModelScope MNN 模型',
    'models.marketSearch': '搜索模型',
    'models.install': '安装',
    'models.installed': '已安装',
    'models.installing': '安装中',
    'models.delete': '删除',

    'nodes.localNode': '本机节点',
    'nodes.localSub': '当前设备作为能力节点运行',
    'nodes.connectionTitle': '连接入口',
    'nodes.connectionSub': '启动运行时后暴露 MCP 和 OpenAI 入口',
    'nodes.placeholder': '多节点发现与协同会在这里扩展。',
    'empty.scenes': '暂无场景',
    'empty.models': '暂无模型',
    'toast.modelsReloaded': '模型已刷新',
    'toast.modelMarketLoaded': '市场已加载',
    'toast.modelMarketFailed': '市场加载失败',
    'toast.scenesReloaded': '场景已刷新',
    'toast.sceneInstalled': '场景已安装',
    'toast.installFailed': '安装失败',
    'toast.modelImported': '模型已导入',
    'toast.modelImportFailed': '导入失败',
    'toast.modelDeleted': '模型已删除',
    'toast.modelDeleteFailed': '删除失败',
    'toast.sceneUninstalled': '场景已移除',
    'toast.sceneUninstallFailed': '移除失败',
    'toast.visionStarted': '视觉已启动',
    'toast.copied': '已复制',
    'toast.copyFailed': '复制失败',
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
const topNodeButton = document.getElementById('topNodeButton')
const topNodeText = document.getElementById('topNodeText')
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
const runtimeAutoStartInput = document.getElementById('runtimeAutoStartInput')
const cameraPermissionState = document.getElementById('cameraPermissionState')
const notificationPermissionState = document.getElementById('notificationPermissionState')
const overlayPermissionState = document.getElementById('overlayPermissionState')
const batteryPermissionState = document.getElementById('batteryPermissionState')
const cameraPermissionButton = document.getElementById('cameraPermissionButton')
const notificationPermissionButton = document.getElementById('notificationPermissionButton')
const overlayPermissionButton = document.getElementById('overlayPermissionButton')
const batteryPermissionButton = document.getElementById('batteryPermissionButton')
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
const copyOpenAiUrlButton = document.getElementById('copyOpenAiUrlButton')
const copyMcpUrlButton = document.getElementById('copyMcpUrlButton')
const copyAuthHeaderButton = document.getElementById('copyAuthHeaderButton')
const copyMcpConfigButton = document.getElementById('copyMcpConfigButton')
const copyTestPromptButton = document.getElementById('copyTestPromptButton')
const runtimeCapabilitiesButton = document.getElementById('runtimeCapabilitiesButton')
const runtimeCapabilitiesState = document.getElementById('runtimeCapabilitiesState')
const runtimeCapabilitiesPanel = document.getElementById('runtimeCapabilitiesPanel')
const runtimeCapabilitiesBack = document.getElementById('runtimeCapabilitiesBack')
const runtimeVisionText = document.getElementById('runtimeVisionText')
const runtimeVisionButton = document.getElementById('runtimeVisionButton')
const runtimeToolExposureInput = document.getElementById('runtimeToolExposureInput')
const runtimeWindowAutoInput = document.getElementById('runtimeWindowAutoInput')
const runtimeWindowButton = document.getElementById('runtimeWindowButton')
const runtimeModelButton = document.getElementById('runtimeModelButton')
const runtimeModelPanel = document.getElementById('runtimeModelPanel')
const runtimeModelBack = document.getElementById('runtimeModelBack')
const runtimeModelList = document.getElementById('runtimeModelList')
const runtimeModelState = document.getElementById('runtimeModelState')
const runtimeModelNote = document.getElementById('runtimeModelNote')
const runtimeAdvancedButton = document.getElementById('runtimeAdvancedButton')
const runtimeAdvancedState = document.getElementById('runtimeAdvancedState')
const runtimeAdvancedPanel = document.getElementById('runtimeAdvancedPanel')
const runtimeAdvancedBack = document.getElementById('runtimeAdvancedBack')
const runtimeSessionCurrent = document.getElementById('runtimeSessionCurrent')
const runtimeSessionList = document.getElementById('runtimeSessionList')
const runtimeSessionNewButton = document.getElementById('runtimeSessionNewButton')
const runtimeDiagRunButton = document.getElementById('runtimeDiagRunButton')
const runtimeDiagSummary = document.getElementById('runtimeDiagSummary')
const runtimeDiagSummaryText = document.getElementById('runtimeDiagSummaryText')
const runtimeCpuThreadsInput = document.getElementById('runtimeCpuThreadsInput')
const languageControl = document.getElementById('languageControl')
const toast = document.getElementById('toast')
const runtimeDefaultTokens = document.getElementById('runtimeDefaultTokens')
const runtimeModelTokens = document.getElementById('runtimeModelTokens')
const runtimeEffectiveTokens = document.getElementById('runtimeEffectiveTokens')
const runtimeDeviceState = document.getElementById('runtimeDeviceState')
const settingsHomePage = document.getElementById('settingsList')
const homeRuntimePill = document.getElementById('homeRuntimePill')
const homeRailToggle = document.getElementById('homeRailToggle')
const homeSidebar = document.getElementById('homeSidebar')
const homeSessionCount = document.getElementById('homeSessionCount')
const homeNewChatButton = document.getElementById('homeNewChatButton')
const homeSessionList = document.getElementById('homeSessionList')
const homeChatForm = document.getElementById('homeChatForm')
const homeChatInput = document.getElementById('homeChatInput')
const homeChatSendButton = document.getElementById('homeChatSendButton')
const homeChatFeed = document.getElementById('homeChatFeed')
const homeImageInput = document.getElementById('homeImageInput')
const homeImagePreview = document.getElementById('homeImagePreview')
const homeImagePreviewImg = document.getElementById('homeImagePreviewImg')
const homeImagePreviewName = document.getElementById('homeImagePreviewName')
const homeImageRemoveButton = document.getElementById('homeImageRemoveButton')
const nodeCopyMcpButton = document.getElementById('nodeCopyMcpButton')
const nodeOpenServerButton = document.getElementById('nodeOpenServerButton')
const nodeLocalState = document.getElementById('nodeLocalState')
const nodeLocalSub = document.getElementById('nodeLocalSub')
const nodeConnectionText = document.getElementById('nodeConnectionText')

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
let modelProgressLastPercent = 0
let modelProgressHideTimer = null
let localeSetting = { mode: 'system' }
let currentLocale = 'en'
let activeAlert = null
const localeStorePath = '/v1/store/runtime-settings/locale'
let scenes = []
let activeScene = null
let cameraPreviewRect = null
let homeAttachedImage = null

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

function runtimeDetails() {
  return [
    runtimeSettingsPanel,
    runtimeServerPanel,
    runtimeCapabilitiesPanel,
    runtimeModelPanel,
    runtimeAdvancedPanel,
  ].filter(Boolean)
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
  if (page !== 'settings') {
    closeSettingsDetails()
    if (settingsHomePage) {
      settingsHomePage.classList.remove('active')
      settingsHomePage.setAttribute('aria-hidden', 'true')
    }
  }
  showPanel(document.getElementById('page-' + page))
  if (page === 'settings' && settingsHomePage) {
    settingsHomePage.classList.add('active')
    settingsHomePage.setAttribute('aria-hidden', 'false')
  }
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

function publicRuntimeUrl(state) {
  const current = state || runtimeServiceState || {}
  const raw = current.lanUrl || current.url || ('http://127.0.0.1:' + (current.port || 11434))
  return String(raw).replace(/\/$/, '')
}

function openAiBaseUrl() {
  return publicRuntimeUrl(runtimeServiceState) + '/v1'
}

function mcpEndpointUrl() {
  return publicRuntimeUrl(runtimeServiceState) + '/mcp'
}

function runtimeAuthToken() {
  return (runtimeAuthTokenInput && runtimeAuthTokenInput.value.trim()) ||
    (runtimeServiceState && runtimeServiceState.authToken) ||
    ''
}

function authHeaderText() {
  const token = runtimeAuthToken()
  return token ? ('Authorization: Bearer ' + token) : 'Authorization disabled'
}

function mcpConfigText() {
  const server = {
    type: 'streamable-http',
    url: mcpEndpointUrl()
  }
  const token = runtimeAuthToken()
  if (token) server.headers = { Authorization: 'Bearer ' + token }
  return JSON.stringify({ mcpServers: { lociant: server } }, null, 2)
}

function testPromptText() {
  return 'Call runtime_status. Then call model_list. If vision is available, call vision_status.'
}

function copyText(text) {
  const value = String(text || '')
  if (navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(value)
  }
  const input = document.createElement('textarea')
  input.value = value
  input.setAttribute('readonly', '')
  input.style.position = 'fixed'
  input.style.left = '-9999px'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  input.setSelectionRange(0, input.value.length)
  const ok = document.execCommand('copy')
  document.body.removeChild(input)
  return ok ? Promise.resolve() : Promise.reject(new Error('copy failed'))
}

function copyConnectionText(factory) {
  try {
    copyText(factory()).then(() => {
      showToast(t('toast.copied'))
    }).catch(() => {
      showToast(t('toast.copyFailed'))
    })
  } catch (error) {
    showToast(t('toast.copyFailed'))
  }
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

function runtimeState() {
  try {
    return shellCommand('status', {})
  } catch (error) {
    return runtimeServiceState || { running: false }
  }
}

async function runtimeFetchJson(path) {
  const state = runtimeServiceState || runtimeState()
  const base = (state && (state.lanUrl || state.url)) ? String(state.lanUrl || state.url).replace(/\/$/, '') : localApiBaseUrl()
  const headers = {}
  if (state && state.authToken) headers.Authorization = 'Bearer ' + state.authToken
  const response = await fetch(base + path, { headers })
  const json = await response.json()
  if (!response.ok) throw new Error(path + ': ' + ((json.error && json.error.message) || json.message || 'API request failed'))
  return json
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
    const runShell = ['start', 'stop', 'status', 'settings', 'battery.requestExemption',
      'window.show', 'window.hide', 'window.collapse', 'window.expand',
      'window.settings', 'window.permission', 'vision.start', 'vision.stop', 'vision.status',
      'session.create', 'session.select', 'session.delete', 'session.details'
    ].includes(command)
    if (runShell) {
      const next = shellCommand(command, body)
      updateRuntimeServiceState(next)
      return next
    }
    const promise = apiPost('/v1/runtime/' + encodeURIComponent(command), body)
    promise.then(state => {
      updateRuntimeServiceState(state)
    }).catch(() => updateRuntimeServiceState({ running: false, message: 'API server command failed' }))
    return promise
  } catch (error) {
    updateRuntimeServiceState({ running: false, message: 'API server command failed' })
    return Promise.reject(error)
  }
}

function runtimeServiceCommand(command, payload) {
  try {
    const next = shellCommand(command, payload)
    updateRuntimeServiceState(next)
    return next
  } catch (error) {
    updateRuntimeServiceState({ running: false, message: 'Runtime service command failed' })
    return null
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
  const cameraGranted = runtimeServiceState.cameraPermissionGranted === true
  const notificationGranted = runtimeServiceState.notificationPermissionGranted === true
  const overlayGranted = runtimeServiceState.windowAllowed === true
  const batteryGranted = !!runtimeServiceState.batteryOptimizationIgnored
  if (stateDot) stateDot.classList.toggle('running', running || starting)
  if (stateText) stateText.classList.toggle('running', running || starting)
  runtimeSettingsState.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
  runtimeSettingsState.classList.toggle('running', running || starting)
  runtimeServiceToggle.textContent = running || starting ? t('common.stop') : t('common.start')
  runtimeServiceMessage.textContent = runtimeServiceStatusText(runtimeServiceState, running, starting)
  if (runtimeAutoStartInput) runtimeAutoStartInput.checked = !!runtimeServiceState.autoStart
  runtimeServerState.textContent = runtimeServiceState.port ? String(runtimeServiceState.port) : '--'
  if (cameraPermissionState) cameraPermissionState.textContent = devicePermissionLabel(cameraGranted, t('settings.cameraPermissionGranted'), t('settings.cameraPermissionNeeded'))
  if (notificationPermissionState) notificationPermissionState.textContent = devicePermissionLabel(notificationGranted, t('settings.notificationPermissionGranted'), t('settings.notificationPermissionNeeded'))
  if (overlayPermissionState) overlayPermissionState.textContent = devicePermissionLabel(overlayGranted, t('settings.overlayPermissionGranted'), t('settings.overlayPermissionNeeded'))
  if (batteryPermissionState) batteryPermissionState.textContent = devicePermissionLabel(batteryGranted, t('settings.batteryAllowed'), t('settings.batteryRestricted'))
  setPermissionButton(cameraPermissionButton, cameraGranted)
  setPermissionButton(notificationPermissionButton, notificationGranted)
  setPermissionButton(overlayPermissionButton, overlayGranted)
  setPermissionButton(batteryPermissionButton, batteryGranted)
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
  if (runtimeServiceState.port && (runtimeServiceState.lanUrl || runtimeServiceState.url)) {
    diagUrl.textContent = runtimeServiceState.lanUrl || publicRuntimeUrl(runtimeServiceState)
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
  if (runtimeDeviceState) {
    const device = runtimeServiceState.device || {}
    const parts = []
    parts.push(device.interactive ? 'interactive' : 'idle')
    if (device.keyguardLocked) parts.push('locked')
    if (!device.activityForeground) parts.push('background')
    runtimeDeviceState.textContent = parts.join(' · ')
  }
  if (runtimeServiceState.sessions) renderSessions(runtimeServiceState.sessions)
  if (runtimeAdvancedState) {
    const sessions = Array.isArray(runtimeServiceState.sessions) ? runtimeServiceState.sessions.length : 0
    const requests = Number(runtimeServiceState.requestCount) || 0
    runtimeAdvancedState.textContent = sessions + ' / ' + requests
  }
  if (runtimeServiceState.requestCount !== undefined || runtimeServiceState.recentRequests) {
    updateDiagnostics(runtimeServiceState)
  }
  updateHomeState()
  updateNodeState()
  updateRuntimeStrip()
}

function updateHomeState() {
  const running = !!(runtimeServiceState && runtimeServiceState.running)
  const starting = !!(runtimeServiceState && runtimeServiceState.starting)
  const runtimeLabel = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
  const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions) ? runtimeServiceState.sessions : []
  if (homeRuntimePill) {
    homeRuntimePill.textContent = runtimeLabel
    homeRuntimePill.classList.toggle('running', running || starting)
  }
  if (homeSessionCount) homeSessionCount.textContent = String(sessions.length)
  renderHomeSessions(sessions)
}

function updateNodeState() {
  const running = !!(runtimeServiceState && runtimeServiceState.running)
  const starting = !!(runtimeServiceState && runtimeServiceState.starting)
  const label = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
  if (topNodeText) topNodeText.textContent = t('nodes.localNode')
  if (topNodeButton) topNodeButton.classList.toggle('running', running || starting)
  if (nodeLocalState) {
    nodeLocalState.textContent = label
    nodeLocalState.classList.toggle('running', running || starting)
  }
  if (nodeLocalSub) nodeLocalSub.textContent = running ? publicRuntimeUrl(runtimeServiceState) : t('nodes.localSub')
  if (nodeConnectionText) nodeConnectionText.textContent = running ? mcpEndpointUrl() : t('nodes.connectionSub')
}

function runtimeServiceStatusText(state, running, starting) {
  if (starting) return t('status.starting')
  if (running) return publicRuntimeUrl(state)
  return t('settings.runtimeDefaultMessage')
}

function visionState() {
  return (runtimeServiceState && runtimeServiceState.vision) || {}
}

function devicePermissionLabel(granted, okText, badText) {
  return granted ? okText : badText
}

function setPermissionButton(button, granted) {
  if (!button) return
  button.textContent = granted ? t('settings.manage') : t('settings.grant')
  button.dataset.permissionGranted = granted ? 'true' : 'false'
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
  showPagePanel('home')
  const homePanel = document.getElementById('page-home')
  if (homePanel) homePanel.scrollTop = 0
  navItems.forEach(item => item.classList.toggle('active', item.dataset.page === 'home'))
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
  updateHomeState()
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
  modelProgressLastPercent = 0
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
  let retries = 0
  marketInstallTimer = window.setInterval(() => {
    apiGet('/v1/models/market/' + encodeURIComponent(modelId) + '/progress')
      .then(data => {
        const next = normalizeMarketProgress(data, modelId)
        if (next.state === 'done') {
          marketInstallingModelId = ''
          setModelProgress(next)
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
          loadModels()
          showToast(t('toast.modelImported'))
          return
        }
        if (next.state === 'error') {
          marketInstallingModelId = ''
          setModelProgress(next)
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
          return
        }
        retries = 0
        setModelProgress(next)
      })
      .catch(() => {
        retries += 1
        setModelProgress({
          state: 'installing',
          active: true,
          modelId: modelId,
          progress: modelProgressLastPercent || 0,
          message: t('models.installing') + ': ' + (marketInstallingModelId || modelId),
        })
        if (retries >= 20) {
          marketInstallingModelId = ''
          setModelProgress({ state: 'error', message: t('toast.modelImportFailed'), progress: modelProgressLastPercent || null })
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
        }
      })
  }, 800)
}

function setModelProgress(data) {
  if (!modelProgress) return
  const normalized = normalizeMarketProgress(data, marketInstallingModelId)
  const state = normalized.state
  window.clearTimeout(modelProgressHideTimer)
  modelProgress.classList.toggle('active', state === 'installing' || state === 'downloading' || state === 'done')
  modelProgress.classList.toggle('error', state === 'error')
  modelProgress.classList.toggle('done', state === 'done')
  modelProgressText.textContent = normalized.message || ''
  const percent = Number(normalized.percent)
  if (Number.isFinite(percent)) {
    modelProgressLastPercent = Math.max(modelProgressLastPercent, percent)
  }
  const showPercent = Number.isFinite(percent) && percent > 0
  modelProgressPercent.textContent = showPercent ? Math.round(percent) + '%' : ''
  const width = Number.isFinite(percent) ? percent : modelProgressLastPercent
  modelProgressFill.style.width = Math.max(0, Math.min(100, Math.round(width || 0))) + '%'
  if (state === 'installing' || state === 'downloading') {
    marketInstallingModelId = marketInstallingModelId || normalized.modelId || ''
  } else if (state === 'done') {
    modelProgressHideTimer = window.setTimeout(() => {
      if (!modelProgress) return
      modelProgress.classList.remove('active')
      modelProgress.classList.remove('done')
    }, 1400)
  } else if (state === 'error') {
    modelProgressHideTimer = window.setTimeout(() => {
      if (!modelProgress) return
      modelProgress.classList.remove('active')
      modelProgress.classList.remove('error')
    }, 2400)
  }
}

function normalizeMarketProgress(data, fallbackModelId) {
  const payload = data || {}
  const rawState = String(payload.state || '').toLowerCase()
  const active = payload.active !== undefined ? !!payload.active : null
  const ok = !!payload.ok
  const modelId = payload.modelId || fallbackModelId || marketInstallingModelId || ''
  const rawPercent = Number(payload.percent ?? payload.progress)
  const hasPercent = Number.isFinite(rawPercent)
  const percent = hasPercent ? (rawPercent <= 1 ? rawPercent * 100 : rawPercent) : null

  let state = rawState
  if (!state) {
    if (ok && !active) state = 'done'
    else if (active === false && hasPercent && percent >= 100) state = 'done'
    else if (active === false && !hasPercent) state = 'installing'
    else state = 'installing'
  }

  if (state === 'done' || (ok && percent !== null && percent >= 100)) {
    state = 'done'
  } else if (!['error', 'done', 'downloading', 'installing'].includes(state)) {
    state = 'installing'
  }

  return {
    state,
    active: state === 'installing' || state === 'downloading',
    modelId,
    message: payload.message || (state === 'done' ? t('toast.modelImported') : t('models.installing') + ': ' + (modelId || marketInstallingModelId || '')),
    percent: percent,
    ok,
  }
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
  document.querySelectorAll('[data-i18n-placeholder]').forEach(node => {
    node.setAttribute('placeholder', t(node.dataset.i18nPlaceholder))
  })
  document.querySelectorAll('[data-i18n-aria-label]').forEach(node => {
    node.setAttribute('aria-label', t(node.dataset.i18nAriaLabel))
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

function renderHomeSessions(sessions) {
  if (!homeSessionList) return
  homeSessionList.innerHTML = ''
  const policyLimit = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.recentLimit)
  const items = Array.isArray(sessions) && policyLimit > 0 ? sessions.slice(0, policyLimit) : (Array.isArray(sessions) ? sessions : [])
  if (!items.length) return
  items.forEach(session => {
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'chat-session-item pressable'
    row.classList.toggle('active', session.id === (runtimeServiceState && runtimeServiceState.currentSessionId))
    const body = el('span', 'chat-session-body')
    const title = el('strong', '', session.title || session.id || '--')
    const sub = el('span', '', (session.modelId || '--') + ' · ' + (session.messageCount || 0))
    const remove = el('span', 'chat-session-delete', 'x')
    remove.setAttribute('role', 'button')
    remove.setAttribute('tabindex', '0')
    remove.setAttribute('aria-label', t('home.deleteChat'))
    body.appendChild(title)
    body.appendChild(sub)
    row.appendChild(body)
    row.appendChild(remove)
    row.addEventListener('click', () => {
      Promise.resolve(runtimeApiCommand('session.select', { sessionId: session.id }))
        .then(state => {
          updateRuntimeServiceState(Object.assign({}, state || {}, { currentSessionId: session.id }))
          loadHomeConversation(session.id)
          if (homeSidebar && homeSidebar.classList.contains('open')) {
            homeSidebar.classList.remove('open')
            if (homeRailToggle) homeRailToggle.setAttribute('aria-expanded', 'false')
          }
        })
        .catch(error => showToast((error && error.message) || t('toast.modelImportFailed')))
    })
    const deleteSession = event => {
      event.preventDefault()
      event.stopPropagation()
      const deletingCurrent = session.id === (runtimeServiceState && runtimeServiceState.currentSessionId)
      Promise.resolve(runtimeApiCommand('session.delete', { sessionId: session.id }))
        .then(state => {
          updateRuntimeServiceState(state || {})
          restoreHomeConversation({ forceLatest: deletingCurrent })
          showToast(t('home.deleteChat'))
        })
        .catch(error => showToast((error && error.message) || t('toast.modelDeleteFailed')))
    }
    remove.addEventListener('click', deleteSession)
    remove.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') deleteSession(event)
    })
    homeSessionList.appendChild(row)
  })
}

function upsertHomeSessionPreview(sessionId, titleText, lastRole) {
  if (!sessionId) return
  const now = Date.now()
  const policyLastTextLimit = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.lastTextLimit)
  const lastTextLimit = policyLastTextLimit > 0 ? policyLastTextLimit : Number.POSITIVE_INFINITY
  const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions)
    ? runtimeServiceState.sessions.slice()
    : []
  const index = sessions.findIndex(session => session && session.id === sessionId)
  const existing = index >= 0 ? sessions[index] : {}
  const next = Object.assign({}, existing, {
    id: sessionId,
    title: existing.title || String(titleText || '').trim() || sessionId,
    modelId: existing.modelId || (runtimeServiceState && runtimeServiceState.modelId) || '--',
    updatedAt: now,
    messageCount: Math.max(Number(existing.messageCount) || 0, 1) + (lastRole === 'assistant' ? 1 : 0),
    lastRole: lastRole || existing.lastRole || 'user',
    lastText: String(titleText || existing.lastText || '').slice(0, lastTextLimit),
  })
  if (index >= 0) sessions.splice(index, 1)
  sessions.unshift(next)
  runtimeServiceState = Object.assign({}, runtimeServiceState || {}, {
    currentSessionId: sessionId,
    sessions,
  })
  updateHomeState()
}

function homeCurrentSessionId() {
  return (runtimeServiceState && runtimeServiceState.currentSessionId) ||
    (runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.defaultSessionId) ||
    ''
}

function appendChatBubble(role, text, meta) {
  if (!homeChatFeed || !text) return null
  const node = document.createElement('div')
  node.className = 'chat-message ' + (role || 'assistant')
  if (meta && meta.active) node.dataset.activeSession = 'true'
  renderChatMarkdown(node, text)
  homeChatFeed.appendChild(node)
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return node
}

function renderChatMarkdown(target, text) {
  const source = String(text || '')
  if (!window.marked || !window.DOMPurify) {
    target.textContent = source
    return
  }
  window.marked.setOptions({
    breaks: true,
    gfm: true,
    mangle: false,
    headerIds: false,
  })
  const html = window.marked.parse(source)
  target.innerHTML = window.DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code', 'pre', 'ul', 'ol', 'li', 'blockquote', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ALLOWED_ATTR: ['href', 'target', 'rel'],
  })
  target.querySelectorAll('a[href]').forEach(link => {
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
  })
}

function clearHomeMessages() {
  if (!homeChatFeed) return
  Array.from(homeChatFeed.querySelectorAll('.chat-message')).forEach(node => node.remove())
}

function renderHomeConversation(sessionId, messages) {
  if (!homeChatFeed) return
  clearHomeMessages()
  const items = Array.isArray(messages) ? messages : []
  if (!items.length) return
  items.forEach(item => {
    const role = item && item.role ? item.role : 'assistant'
    const text = item && (item.text || item.content || item.message || '')
    if (text) appendChatBubble(role, text, { active: sessionId === homeCurrentSessionId() })
  })
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
}

function latestHomeSession(state) {
  const sessions = Array.isArray(state && state.sessions) ? state.sessions : []
  return sessions.find(session => session && session.id && Number(session.messageCount || 0) > 0) ||
    sessions.find(session => session && session.id) ||
    null
}

function shouldPreferLatestHomeSession(state, currentId) {
  if (!currentId) return true
  const sessions = Array.isArray(state && state.sessions) ? state.sessions : []
  const current = sessions.find(session => session && session.id === currentId)
  return !current || Number(current.messageCount || 0) <= 0
}

function restoreHomeConversation(options) {
  const forceLatest = !!(options && options.forceLatest)
  const state = runtimeServiceState || runtimeState()
  updateRuntimeServiceState(state)
  const latest = latestHomeSession(state)
  const currentId = state && state.currentSessionId
  const target = forceLatest || shouldPreferLatestHomeSession(state, currentId)
    ? (latest && latest.id)
    : currentId
  if (!target) {
    clearHomeMessages()
    return null
  }
  if (target !== currentId) {
    const selected = runtimeApiCommand('session.select', { sessionId: target })
    updateRuntimeServiceState(selected || Object.assign({}, state, { currentSessionId: target }))
  }
  loadHomeConversation(target, { silent: true })
  return target
}

// ---- Diagnostics ----
let runtimeDiagLastResults = null

function diagnosticCardsFromState(state, check) {
  const current = state || {}
  const vision = current.vision || {}
  const tools = check && check.tools
  const model = check && check.model
  return [
    {
      key: 'runtime',
      title: t('diagnostics.runtime'),
      ok: !!current.running,
      text: current.running ? publicRuntimeUrl(current) : (current.message || t('status.stopped'))
    },
    {
      key: 'tools',
      title: t('diagnostics.tools'),
      ok: tools ? tools.ok : current.toolExposure !== undefined,
      text: tools ? tools.text : ((current.toolExposure || 'action') + ' exposure')
    },
    {
      key: 'model',
      title: t('diagnostics.model'),
      ok: model ? model.ok : !!current.modelLoaded,
      text: model ? model.text : ((current.modelId || '--') + (current.modelLoaded ? ' ready' : ' not loaded'))
    },
    {
      key: 'vision',
      title: t('diagnostics.vision'),
      ok: vision.running || String(vision.state || '').toLowerCase() === 'idle',
      text: vision.message || visionStateLabel(vision)
    },
    {
      key: 'mcp',
      title: t('diagnostics.mcp'),
      ok: !!current.running,
      text: current.running ? (publicRuntimeUrl(current) + '/mcp') : t('status.stopped')
    }
  ]
}

function renderDiagnosticsSummary(check) {
  if (!runtimeDiagSummary) return
  runtimeDiagSummary.innerHTML = ''
  const cards = diagnosticCardsFromState(runtimeServiceState || {}, check || runtimeDiagLastResults)
  const issues = cards.filter(item => !item.ok).length
  if (runtimeDiagSummaryText) {
    runtimeDiagSummaryText.textContent = check && check.running
      ? t('diagnostics.running')
      : (issues ? (issues + ' ' + t('diagnostics.issue')) : t('diagnostics.ready'))
  }
  cards.forEach(item => {
    const card = document.createElement('div')
    card.className = 'settings-section diagnostic-card' + (item.ok ? ' ok' : ' issue')
    card.innerHTML = '<span class="settings-section-main">' +
      '<span class="settings-section-title">' + item.title + '</span>' +
      '<span class="settings-section-sub diagnostic-card-text">' + (item.text || '--') + '</span>' +
      '</span>' +
      '<span class="status-pill diagnostic-card-state">' + (item.ok ? t('diagnostics.ready') : t('diagnostics.issue')) + '</span>'
    runtimeDiagSummary.appendChild(card)
  })
}

function toolResultText(result, fallback) {
  if (!result) return fallback
  if (result.ok === false) return result.message || fallback
  return fallback
}

function runAgentDiagnostics() {
  if (!runtimeDiagRunButton) return
  runtimeDiagRunButton.disabled = true
  runtimeDiagRunButton.textContent = t('diagnostics.running')
  renderDiagnosticsSummary({ running: true })
  const state = shellCommand('status', {})
  updateRuntimeServiceState(state)
  Promise.allSettled([
    apiGet('/v1/tools'),
    apiPost('/v1/tools/runtime_status/call', { arguments: {} }),
    apiPost('/v1/tools/model_list/call', { arguments: {} }),
    apiPost('/v1/tools/vision_status/call', { arguments: {} })
  ]).then(results => {
    const toolsResponse = results[0].status === 'fulfilled' ? results[0].value : null
    const runtimeResponse = results[1].status === 'fulfilled' ? results[1].value : null
    const modelResponse = results[2].status === 'fulfilled' ? results[2].value : null
    const visionResponse = results[3].status === 'fulfilled' ? results[3].value : null
    const toolCount = toolsResponse && toolsResponse.data ? toolsResponse.data.length : 0
    const models = modelResponse && modelResponse.result && Array.isArray(modelResponse.result.models)
      ? modelResponse.result.models
      : []
    const readyModels = models.filter(model => model.ready !== false).length
    const vision = visionResponse && visionResponse.result ? visionResponse.result : null
    runtimeDiagLastResults = {
      tools: {
        ok: toolCount > 0 && !!runtimeResponse,
        text: toolCount ? (toolCount + ' tools exposed') : 'No tools visible'
      },
      model: {
        ok: readyModels > 0 || !!(runtimeServiceState && runtimeServiceState.modelLoaded),
        text: readyModels ? (readyModels + ' ready models') : ((runtimeServiceState && runtimeServiceState.modelId) || '--')
      },
      vision: {
        ok: !!vision,
        text: toolResultText(vision, vision ? visionStateLabel(vision) : '--')
      }
    }
    renderDiagnosticsSummary(runtimeDiagLastResults)
  }).catch(() => {
    runtimeDiagLastResults = {
      tools: { ok: false, text: 'Tool check failed' },
      model: { ok: false, text: 'Model check failed' }
    }
    renderDiagnosticsSummary(runtimeDiagLastResults)
  }).finally(() => {
    runtimeDiagRunButton.disabled = false
    runtimeDiagRunButton.textContent = t('diagnostics.run')
  })
}

function updateDiagnostics(state) {
  const log = document.getElementById('runtimeDiagLog')
  renderDiagnosticsSummary()
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
  closeSettingsDetails()
  if (settingsList) {
    settingsList.classList.add('active')
    settingsList.setAttribute('aria-hidden', 'false')
  }
}

function showSettingsDetail(panel) {
  closeSettingsDetails()
  if (settingsList) {
    settingsList.classList.remove('active')
    settingsList.setAttribute('aria-hidden', 'true')
  }
  if (panel) {
    panel.classList.add('active')
    panel.setAttribute('aria-hidden', 'false')
  }
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
  closeSettingsDetails()
  if (settingsList) {
    settingsList.classList.add('active')
    settingsList.setAttribute('aria-hidden', 'false')
  }
}

function closeSettingsDetails() {
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

function openRuntimeAdvancedSettings() {
  showSettingsDetail(runtimeAdvancedPanel)
}


/* === 08-ui.js === */
/* ── Lociant WebUI — UI components ── */

// ---- Sidebar ----
let sidebarBusy = false
let nativeKeyboardInset = 0

function clearPress(target) {
  if (target) target.classList.remove('is-pressed')
}

function toggleSidebar(event) {
  if (event) event.preventDefault()
  if (window.matchMedia('(orientation: portrait), (max-width: 760px)').matches) {
    app.classList.toggle('mobile-nav-open')
    menuButton.classList.toggle('is-active', app.classList.contains('mobile-nav-open'))
    if (app.classList.contains('mobile-nav-open') && homeSidebar && homeRailToggle) {
      homeSidebar.classList.remove('open')
      homeRailToggle.setAttribute('aria-expanded', 'false')
      homeRailToggle.classList.remove('is-active')
    }
    return
  }
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
  app.classList.remove('mobile-nav-open')
  menuButton.classList.remove('is-active')
  setKeyboardOffset(0)
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
  stateText.textContent = runtimeSnapshot && runtimeSnapshot.running ? t('state.background') : t('state.idle')
  updateRuntimeStrip()
}

function openRuntimeServerFromHome() {
  navigateTo('settings')
  openRuntimeServerSettings()
}

function syncKeyboardOffset() {
  const chatFocused = document.activeElement === homeChatInput
  if (!document.documentElement || !chatFocused) {
    setKeyboardOffset(0)
    return
  }
  const viewport = window.visualViewport
  const viewportHidden = viewport
    ? Math.max(0, window.innerHeight - viewport.height - viewport.offsetTop)
    : 0
  const hidden = Math.max(nativeKeyboardInset, viewportHidden)
  const offset = hidden > 80 ? Math.min(hidden, 320) : 0
  setKeyboardOffset(offset)
  if (offset && homeChatFeed) homeChatFeed.scrollTop = homeChatFeed.scrollHeight
}

function setKeyboardOffset(offset) {
  document.documentElement.style.setProperty('--keyboard-offset', offset + 'px')
  app.classList.toggle('keyboard-active', offset > 0)
}

window.__lociantKeyboardInset = function(insetPx) {
  nativeKeyboardInset = Math.max(0, Number(insetPx) || 0)
  syncKeyboardOffset()
}

function showHomeConversationLoading(text) {
  clearHomeMessages()
  appendChatBubble('assistant', text || t('home.thinking'))
}

function handleHomeAction(action) {
  if (action === 'diagnostics') {
    navigateTo('settings')
    openRuntimeAdvancedSettings()
    runAgentDiagnostics()
    return
  }
  if (action === 'copy-config') {
    openRuntimeServerFromHome()
    return
  }
  if (action === 'scenes') {
    navigateTo('scenes')
    return
  }
}

function setHomeImageAttachment(file, dataUrl) {
  homeAttachedImage = file && dataUrl ? {
    name: file.name || t('home.imageAttached'),
    url: dataUrl,
  } : null
  if (!homeImagePreview) return
  const active = !!homeAttachedImage
  homeImagePreview.classList.toggle('active', active)
  homeImagePreview.setAttribute('aria-hidden', active ? 'false' : 'true')
  if (homeImagePreviewImg) homeImagePreviewImg.src = active ? homeAttachedImage.url : ''
  if (homeImagePreviewName) homeImagePreviewName.textContent = active ? homeAttachedImage.name : ''
}

function clearHomeImageAttachment() {
  setHomeImageAttachment(null, '')
  if (homeImageInput) homeImageInput.value = ''
}

function readHomeImage(file) {
  if (!file || !file.type || !file.type.startsWith('image/')) return
  const reader = new FileReader()
  reader.onload = () => setHomeImageAttachment(file, String(reader.result || ''))
  reader.onerror = () => showToast(t('toast.modelImportFailed'))
  reader.readAsDataURL(file)
}

function homeChatMessages(prompt, image) {
  if (!image) return [{ role: 'user', content: prompt }]
  const content = []
  if (prompt) content.push({ type: 'text', text: prompt })
  content.push({ type: 'image_url', image_url: { url: image.url } })
  return [{ role: 'user', content }]
}

function submitHomeChat(text) {
  const prompt = String(text || '').trim()
  const image = homeAttachedImage
  if (!prompt && !image) return
  appendChatBubble('user', image ? ((prompt || t('home.imageAttached')) + ' · ' + t('home.imageAttached')) : prompt)
  if (homeChatInput) homeChatInput.value = ''
  clearHomeImageAttachment()
  if (homeChatSendButton) homeChatSendButton.disabled = true
  const pending = appendChatBubble('assistant', t('home.thinking'))
  const modelId = (runtimeServiceState && runtimeServiceState.modelId) || ''
  const sessionId = homeCurrentSessionId()
  upsertHomeSessionPreview(sessionId, prompt || t('home.imageAttached'), 'user')
  apiPost('/v1/chat/completions', {
    model: modelId,
    stream: false,
    sessionId,
    messages: homeChatMessages(prompt, image)
  }).then(result => {
    const reply = chatResponseText(result)
    if (pending) renderChatMarkdown(pending, reply || t('home.emptyReply'))
    else appendChatBubble('assistant', reply || t('home.emptyReply'))
    if (result && result.sessionId) {
      runtimeServiceState = Object.assign({}, runtimeServiceState || {}, { currentSessionId: result.sessionId })
    }
    upsertHomeSessionPreview((result && result.sessionId) || sessionId, reply || prompt || t('home.imageAttached'), 'assistant')
    refreshRuntimeServiceState()
  }).catch(error => {
    if (pending) renderChatMarkdown(pending, (error && error.message) || t('toast.modelImportFailed'))
    else appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
  }).finally(() => {
    if (homeChatSendButton) homeChatSendButton.disabled = false
  })
}

function chatResponseText(result) {
  if (!result) return ''
  const choice = result.choices && result.choices[0]
  const message = choice && choice.message
  return String(
    (message && message.content) ||
    (result.message && result.message.content) ||
    result.response ||
    result.text ||
    ''
  ).trim()
}

function loadHomeConversation(sessionId, options) {
  const target = sessionId || homeCurrentSessionId()
  const silent = !!(options && options.silent)
  if (!silent) showHomeConversationLoading(t('home.thinking'))
  try {
    const state = runtimeApiCommand('session.details', { sessionId: target })
    const payload = state && state.session ? state.session : state
    const messages = payload && Array.isArray(payload.messages) ? payload.messages : []
    updateRuntimeServiceState(Object.assign({}, state || {}, { currentSessionId: target }))
    renderHomeConversation(target, messages)
  } catch (error) {
    clearHomeMessages()
    if (!silent) appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
  }
}

// ---- Clock tick ----
function tick() {
  clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
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
window.addEventListener('resize', syncKeyboardOffset)
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', syncKeyboardOffset)
  window.visualViewport.addEventListener('scroll', syncKeyboardOffset)
}
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
  if (app.classList.contains('mobile-nav-open') && !event.target.closest('.sidebar')) {
    app.classList.remove('mobile-nav-open')
    menuButton.classList.remove('is-active')
  }
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
if (topNodeButton) {
  topNodeButton.addEventListener('click', () => navigateTo('nodes'))
}
if (homeRailToggle && homeSidebar) {
  homeRailToggle.setAttribute('aria-expanded', 'false')
  homeRailToggle.addEventListener('click', () => {
    const open = homeSidebar.classList.toggle('open')
    homeRailToggle.setAttribute('aria-expanded', open ? 'true' : 'false')
    homeRailToggle.classList.toggle('is-active', open)
    if (open) {
      app.classList.remove('mobile-nav-open')
      menuButton.classList.remove('is-active')
    }
    if (open) refreshRuntimeServiceState()
  })
  document.addEventListener('pointerdown', event => {
    if (!homeSidebar.classList.contains('open')) return
    if (!homeSidebar.contains(event.target) && !homeRailToggle.contains(event.target)) {
      homeSidebar.classList.remove('open')
      homeRailToggle.setAttribute('aria-expanded', 'false')
      homeRailToggle.classList.remove('is-active')
    }
  })
}
if (homeNewChatButton) {
  homeNewChatButton.addEventListener('click', () => {
    const next = runtimeApiCommand('session.create', {})
    Promise.resolve(next).then(state => {
      const sessionId = state && state.currentSessionId ? state.currentSessionId : homeCurrentSessionId()
      clearHomeMessages()
      updateRuntimeServiceState(state || {})
      loadHomeConversation(sessionId)
    }).catch(error => {
      showToast((error && error.message) || t('toast.modelImportFailed'))
    })
  })
}
if (homeChatForm) {
  homeChatForm.addEventListener('submit', event => {
    event.preventDefault()
    submitHomeChat(homeChatInput && homeChatInput.value)
  })
}
if (homeChatInput) {
  homeChatInput.addEventListener('focus', () => {
    window.setTimeout(syncKeyboardOffset, 80)
    window.setTimeout(syncKeyboardOffset, 260)
  })
  homeChatInput.addEventListener('blur', () => {
    setKeyboardOffset(0)
    window.setTimeout(syncKeyboardOffset, 120)
  })
}
if (homeImageInput) {
  homeImageInput.addEventListener('change', () => {
    const file = homeImageInput.files && homeImageInput.files[0]
    readHomeImage(file)
  })
}
if (homeImageRemoveButton) {
  homeImageRemoveButton.addEventListener('click', clearHomeImageAttachment)
}
if (homeChatFeed) {
  homeChatFeed.addEventListener('click', event => {
    const button = event.target.closest('[data-home-action]')
    if (button) handleHomeAction(button.dataset.homeAction)
  })
}

// ---- Settings navigation ----
runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
runtimeSettingsBack.addEventListener('click', closeRuntimeSettingsBack)
runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
runtimeServerBack.addEventListener('click', backToRuntimeSettings)
runtimeCapabilitiesButton.addEventListener('click', openRuntimeCapabilitiesSettings)
runtimeCapabilitiesBack.addEventListener('click', backToRuntimeSettings)
runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
runtimeModelBack.addEventListener('click', backToRuntimeSettings)
runtimeAdvancedButton.addEventListener('click', openRuntimeAdvancedSettings)
runtimeAdvancedBack.addEventListener('click', backToRuntimeSettings)

// ---- Runtime controls ----
function isPermissionGranted(button) {
  return button && button.dataset.permissionGranted === 'true'
}

function handlePermissionAction(button, requestMethod, settingsKind) {
  if (isPermissionGranted(button)) native('openPermissionSettings', settingsKind || 'app')
  else native(requestMethod)
}

runtimeServiceToggle.addEventListener('click', () => {
  const running = runtimeServiceState && (runtimeServiceState.running || runtimeServiceState.starting)
  runtimeApiCommand(running ? 'stop' : 'start', {})
})
runtimeAutoStartInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { autoStart: !!runtimeAutoStartInput.checked })
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
cameraPermissionButton.addEventListener('click', () => {
  handlePermissionAction(cameraPermissionButton, 'requestCameraPermission', 'app')
})
notificationPermissionButton.addEventListener('click', () => {
  handlePermissionAction(notificationPermissionButton, 'requestNotificationPermission', 'app')
})
overlayPermissionButton.addEventListener('click', () => {
  handlePermissionAction(overlayPermissionButton, 'requestOverlayPermission', 'overlay')
})
batteryPermissionButton.addEventListener('click', () => {
  handlePermissionAction(batteryPermissionButton, 'requestBatteryOptimizationExemption', 'battery')
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
if (copyOpenAiUrlButton) {
  copyOpenAiUrlButton.addEventListener('click', () => copyConnectionText(openAiBaseUrl))
}
if (copyMcpUrlButton) {
  copyMcpUrlButton.addEventListener('click', () => copyConnectionText(mcpEndpointUrl))
}
if (copyAuthHeaderButton) {
  copyAuthHeaderButton.addEventListener('click', () => copyConnectionText(authHeaderText))
}
if (copyMcpConfigButton) {
  copyMcpConfigButton.addEventListener('click', () => copyConnectionText(mcpConfigText))
}
if (copyTestPromptButton) {
  copyTestPromptButton.addEventListener('click', () => copyConnectionText(testPromptText))
}

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
if (runtimeDiagRunButton) {
  runtimeDiagRunButton.addEventListener('click', runAgentDiagnostics)
}
if (nodeCopyMcpButton) {
  nodeCopyMcpButton.addEventListener('click', () => copyConnectionText(mcpConfigText))
}
if (nodeOpenServerButton) {
  nodeOpenServerButton.addEventListener('click', openRuntimeServerFromHome)
}

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
  if (!document.hidden) {
    refreshRuntimeServiceState()
    restoreHomeConversation()
  }
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
restoreHomeConversation()
loadScenes()
loadModels()
loadLocaleSetting()
tick()
window.setInterval(tick, 1000)
