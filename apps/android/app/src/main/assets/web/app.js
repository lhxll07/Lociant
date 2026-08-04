/* === 01-i18n.js === */
/* Lociant WebUI - i18n dictionary */

const i18n = {
  en: {
    'nav.home': 'Home',
    'nav.settings': 'Settings',
    'nav.models': 'Models',
    'common.back': 'Back',
    'common.open': 'Open',
    'common.refresh': 'Refresh',
    'common.install': 'Install',
    'common.save': 'Save',
    'settings.custom': 'Custom',
    'common.start': 'Start',
    'common.stop': 'Stop',
    'state.idle': 'Idle',
    'state.running': 'Running',
    'state.background': 'Background',
    'status.starting': 'Starting',
    'status.running': 'Running',
    'status.stopped': 'Stopped',

    'home.placeholder': 'Ask Lociant, or describe a tool task',
    'home.send': 'Send',
    'home.runtimeLabel': 'Runtime',
    'home.modelLabel': 'Model',
    'home.historyTitle': 'Recent chats',
    'home.newChat': 'New chat',
    'home.today': 'Today',
    'home.yesterday': 'Yesterday',
    'home.earlier': 'Earlier',
    'home.activeSession': 'Current',
    'home.localChatMeta': 'Local model',
    'home.readyModels': 'ready models',
    'home.emptyReply': 'No reply.',
    'home.runStatusTool': 'Running tool %s (round %s)\u2026',
    'home.runStatusRound': 'Calling model (round %s)\u2026',
    'home.runStatusRetry': 'Network hiccup, retrying\u2026',
    'home.runStatusStall': 'Still working (waited %s s)\u2026',
    'home.toolRunDone': 'Tools executed successfully',
    'home.thinking': 'Thinking...',
    'home.thought': 'Thought',
    'home.deleteChat': 'Delete chat',
    'home.uploadImage': 'Upload photo',
    'home.removeImage': 'Remove photo',
    'home.imageAttached': 'Photo attached',

    'page.settingsTitle': 'Settings',
    'page.settingsSub': 'Keep runtime, permissions, and model behavior in one place.',
    'page.modelsTitle': 'Models',
    'page.modelsSub': 'Install, choose, and manage local inference.',

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
    'settings.serverIntro': 'OpenAI and MCP APIs.',
    'settings.capabilitiesTitle': 'Capabilities',
    'settings.capabilitiesSub': 'Vision and remote tools',
    'settings.capabilitiesIntro': 'Phone capabilities for local services and remote clients.',
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
    'settings.defaultModelSub': 'Model experience',
    'settings.defaultModelIntro': 'Used when requests omit model.',
    'settings.defaultModelNote': 'Ready models only.',
    'settings.selected': 'Selected',
    'settings.modelExperience': 'Model experience',
    'settings.performanceMode': 'Performance mode',
    'settings.performanceEco': 'Eco',
    'settings.performanceBalanced': 'Balanced',
    'settings.performanceFast': 'Fast',
    'settings.cloudTitle': 'Cloud model',
    'settings.cloudSub': 'Use an OpenAI-compatible cloud API',
    'settings.cloudBaseUrl': 'API base URL',
    'settings.cloudBaseUrlSub': 'https://api.openai.com/v1',
    'settings.cloudApiKey': 'API key',
    'settings.cloudApiKeySub': 'Stored locally on this phone',
    'settings.cloudModel': 'Model name',
    'settings.cloudModelSub': 'e.g. gpt-4o-mini, qwen-max',
    'settings.cloudResponseLength': 'Cloud response length',
    'settings.cloudResponseLengthSub': '0 = follow the provider default',
    'settings.cloudLengthUnlimited': 'Unlimited (provider default)',
    'settings.cloudContextWindow': 'Cloud context window',
    'settings.cloudContextWindowSub': 'History trimming budget for cloud models',
    'settings.cloudHistoryLimit': 'Cloud history messages',
    'settings.cloudHistoryLimitSub': 'Conversation turns kept in cloud context',
    'settings.inferenceBackend': 'Inference backend',
    'settings.inferenceBackendSub': 'Engine used for model inference',
    'settings.backendModel': 'Follow model',
    'settings.backendAuto': 'Auto',
    'settings.backendCpu': 'CPU',
    'settings.backendOpencl': 'OpenCL (GPU)',
    'settings.backendVulkan': 'Vulkan (GPU)',
    'settings.performanceEcoSub': 'Lower heat and longer battery life',
    'settings.performanceBalancedSub': 'Balanced speed and battery',
    'settings.performanceFastSub': 'Maximum local inference speed',
    'settings.responseLength': 'Response length',
    'settings.responseLengthCustom': 'Preset or custom token cap',
    'settings.lengthShort': 'Short',
    'settings.lengthNormal': 'Normal',
    'settings.lengthLong': 'Long',
    'settings.contextMemory': 'Context memory',
    'settings.contextMemoryCustom': 'Preset or custom history depth',
    'settings.contextLight': 'Light',
    'settings.contextBalanced': 'Balanced',
    'settings.contextDeep': 'Deep',
    'settings.contextLightSub': 'Keeps recent turns only',
    'settings.contextBalancedSub': 'Default memory depth',
    'settings.contextDeepSub': 'Keeps more conversation history',
    'settings.promptCache': 'Session acceleration',
    'settings.promptCacheOnSub': 'Native prompt cache is active for continuous chats',
    'settings.promptCacheOffSub': 'Prompt cache is not active',
    'settings.releaseModel': 'Release model memory',
    'settings.releaseModelSub': 'Unload current model and clear active KV cache',
    'settings.release': 'Release',
    'settings.perModelConfig': 'Per-model preferences',
    'settings.perModelConfigSub': 'Prepared for model-specific defaults',
    'settings.soon': 'Soon',
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
    'onboarding.settingsTitle': 'Getting started',
    'onboarding.settingsSub': 'Finish the first setup step by step',
    'onboarding.open': 'Start',
    'onboarding.kicker': 'Getting started',
    'onboarding.close': 'Close guide',
    'onboarding.welcomeTitle': 'Let us prepare your phone',
    'onboarding.welcomeBody': 'Lociant lets a cloud model use apps, the screen, and system abilities on your phone. Follow these steps and your first run will be straightforward.',
    'onboarding.welcomeNote': 'Cloud models are recommended first: setup is simpler, while your phone does the actual work.',
    'onboarding.cloudTitle': 'Connect a cloud model',
    'onboarding.cloudBody': 'DeepSeek is a good place to start. Create an API Key, paste it below, and it will stay on this phone.',
    'onboarding.deepseekHint': 'A clear, pay-as-you-go starting point',
    'onboarding.usePreset': 'Use preset',
    'onboarding.apiKeyLabel': 'DeepSeek API Key',
    'onboarding.cloudConfigured': 'Cloud model is ready.',
    'onboarding.cloudPresetApplied': 'DeepSeek address and model filled in. Add your API Key.',
    'onboarding.cloudRequired': 'Add an API Key before continuing.',
    'onboarding.skipCloud': 'Skip for now and use a local model',
    'onboarding.runtimeTitle': 'Start the runtime',
    'onboarding.runtimeBody': 'The runtime is a small service on your phone. The cloud model understands your request; the runtime clicks, types, and reads the page.',
    'onboarding.runtimeHint': 'Keep it running so the Agent can operate the phone.',
    'onboarding.runtimeStopped': 'Not running',
    'onboarding.runtimeStarting': 'Starting...',
    'onboarding.runtimeRunning': 'Runtime is running',
    'onboarding.startRuntime': 'Start runtime',
    'onboarding.waitRuntime': 'Waiting for runtime',
    'onboarding.permissionsTitle': 'Grant the important permission',
    'onboarding.permissionsBody': 'Accessibility is required to click, type, and read the current page. Overlay, notifications, and camera are optional and can be enabled when needed.',
    'onboarding.accessibility': 'Accessibility service',
    'onboarding.accessibilityHint': 'Required: operate and read apps',
    'onboarding.overlay': 'Overlay',
    'onboarding.overlayHint': 'Optional: show runtime status',
    'onboarding.granted': 'Ready',
    'onboarding.needsGrant': 'Needs access',
    'onboarding.optional': 'Optional',
    'onboarding.grantAccessibility': 'Enable accessibility',
    'onboarding.openAppPermissions': 'Open app permissions',
    'onboarding.mcpTitle': 'Connect the phone to a desktop Agent',
    'onboarding.mcpBody': 'MCP is a standard connection for Codex, Claude, or another client to discover and call phone tools. The phone executes; the computer handles conversation and orchestration.',
    'onboarding.mcpEndpointLabel': 'MCP endpoint',
    'onboarding.copyMcpConfig': 'Copy MCP config',
    'onboarding.mcpHint': 'You can also copy the full MCP config and API token from Settings → Runtime → Server. Keep the computer and phone on the same network.',
    'onboarding.localTitle': 'Local models are experimental',
    'onboarding.localBody': 'Local models keep requests on the phone, but packages are larger and depend more on memory and chip support. New users should start with DeepSeek and explore later.',
    'onboarding.localNote': 'Older phones can try them too; speed and available models depend on the device.',
    'onboarding.viewLocalModels': 'View local models',
    'onboarding.viewModelMarket': 'Open model market',
    'onboarding.doneTitle': 'You are ready to try it',
    'onboarding.doneBody': 'Go back home and say what you want done. Start small, then hand over more involved tasks.',
    'onboarding.exampleWechat': '“Open WeChat and sort my unread messages”',
    'onboarding.exampleBilibili': '“Show me what is new on Bilibili”',
    'onboarding.exampleApp': '“Open this new app and test its login flow”',
    'onboarding.rerunHint': 'Want to see this again later? Open Settings → Getting started to run the guide again.',
    'onboarding.previous': 'Back',
    'onboarding.skip': 'Later',
    'onboarding.start': 'Start setup',
    'onboarding.next': 'Next',
    'onboarding.finish': 'Go to home',
    'onboarding.stepCount': 'Step %s of %s',
    'about.title': 'About Lociant',
    'about.subtitle': 'Version, source, and acknowledgements',
    'about.version': '1.1.1',
    'about.versionLabel': 'App version',
    'about.versionSub': 'Application version',
    'about.repository': 'Source repository',
    'about.license': 'License',
    'about.licenseSub': 'MIT License',
    'about.open': 'Open',
    'about.creditsTitle': 'With thanks to open source',
    'about.mnnSub': 'On-device neural network inference',
    'about.ncnnSub': 'Mobile vision inference',
    'about.ktorSub': 'Kotlin server framework',
    'about.dompurifySub': 'HTML sanitization',
    'settings.permissionsTitle': 'Permissions',
    'settings.cameraPermission': 'Camera',
    'settings.notificationPermission': 'Notifications',
    'settings.overlayPermission': 'Overlay',
    'settings.battery': 'Background power',
    'settings.accessibilityPermission': 'Accessibility',
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
    'settings.accessibilityPermissionGranted': 'Ready',
    'settings.accessibilityPermissionNeeded': 'Required',
    'settings.allowed': 'Allowed',
    'settings.allow': 'Allow',
    'settings.grant': 'Grant',
    'settings.manage': 'Manage',
    'settings.open': 'Open',
    'settings.request': 'Grant',
    'settings.currentSession': 'Current',
    'settings.newSession': 'New',
    'diagnostics.title': 'Runtime Diagnostics',
    'diagnostics.summaryTitle': 'Runtime readiness',
    'diagnostics.summarySub': 'Check runtime, tools, model, and vision readiness',
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
    'models.cloudTitle': 'Cloud model',
    'models.cloudSub': 'OpenAI-compatible cloud API',
    'models.cloudHintTitle': 'Make it the default',
    'models.cloudHint': 'After saving, select it as the default in the runtime model list',
    'models.cloudHintEnabled': 'Ready. Select it as the default model in runtime settings',
    'models.marketTitle': 'Model market',
    'models.marketSub': 'ModelScope MNN models',
    'models.marketSearch': 'Search models',
    'models.install': 'Install',
    'models.installed': 'Installed',
    'models.installing': 'Installing',
    'models.delete': 'Delete',
    'models.marketNeedsRuntime': 'Start the runtime to connect the model market',
    'models.marketStartRuntime': 'Start runtime',

    'empty.models': 'No models yet',
    'toast.modelsReloaded': 'Models refreshed',
    'toast.modelMarketLoaded': 'Market loaded',
    'toast.modelMarketFailed': 'Market failed',
    'toast.modelImported': 'Model imported',
    'toast.modelImportFailed': 'Import failed',
    'toast.modelDeleted': 'Model deleted',
    'toast.modelReleased': 'Model released',
    'toast.modelDeleteFailed': 'Delete failed',
    'toast.copied': 'Copied',
    'toast.backendFallback': 'Previous exit was abnormal; inference backend reset to default',
    'toast.copyFailed': 'Copy failed',
  },

  zh: {
    'nav.home': '主页',
    'nav.settings': '设置',
    'nav.models': '模型',
    'common.back': '返回',
    'common.open': '打开',
    'common.refresh': '刷新',
    'common.install': '安装',
    'common.save': '保存',
    'settings.custom': '自定义',
    'common.start': '启动',
    'common.stop': '停止',
    'state.idle': '待机',
    'state.running': '运行中',
    'state.background': '后台',
    'status.starting': '启动中',
    'status.running': '运行中',
    'status.stopped': '已停止',

    'home.placeholder': '问问 Lociant，或输入一个工具调用任务',
    'home.send': '发送',
    'home.runtimeLabel': '运行时',
    'home.modelLabel': '模型',
    'home.historyTitle': '最近对话',
    'home.newChat': '新对话',
    'home.today': '今天',
    'home.yesterday': '昨天',
    'home.earlier': '更早',
    'home.activeSession': '当前',
    'home.localChatMeta': '本地模型',
    'home.readyModels': '个就绪模型',
    'home.emptyReply': '没有回复。',
    'home.runStatusTool': '正在执行工具 %s（第 %s 轮）…',
    'home.runStatusRound': '正在调用模型（第 %s 轮）…',
    'home.runStatusRetry': '网络波动，正在重试…',
    'home.runStatusStall': '仍在处理中（已等待 %s 秒）…',
    'home.toolRunDone': '工具已执行完成',
    'home.thinking': '思考中...',
    'home.thought': '已思考',
    'home.deleteChat': '删除对话',
    'home.uploadImage': '上传照片',
    'home.removeImage': '移除照片',
    'home.imageAttached': '已添加照片',

    'page.settingsTitle': '设置',
    'page.settingsSub': '集中管理运行时、权限和模型行为',
    'page.modelsTitle': '模型',
    'page.modelsSub': '安装、选择和管理本地推理',

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
    'settings.serverIntro': 'OpenAI 与 MCP API',
    'settings.capabilitiesTitle': '能力',
    'settings.capabilitiesSub': '视觉与远程工具',
    'settings.capabilitiesIntro': '提供给本地服务和远程客户端的手机能力',
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
    'settings.defaultModelSub': '模型体验',
    'settings.defaultModelIntro': '请求未指定模型时使用',
    'settings.defaultModelNote': '仅显示已就绪模型',
    'settings.selected': '已选',
    'settings.modelExperience': '模型体验',
    'settings.performanceMode': '性能模式',
    'settings.performanceEco': '省电',
    'settings.performanceBalanced': '均衡',
    'settings.performanceFast': '极速',
    'settings.cloudTitle': '云端模型',
    'settings.cloudSub': '使用 OpenAI 兼容的云端 API',
    'settings.cloudBaseUrl': 'API 地址',
    'settings.cloudBaseUrlSub': 'https://api.openai.com/v1',
    'settings.cloudApiKey': 'API 密钥',
    'settings.cloudApiKeySub': '仅保存在本机',
    'settings.cloudModel': '模型名称',
    'settings.cloudModelSub': '例如 gpt-4o-mini、qwen-max',
    'settings.cloudResponseLength': '云端回复长度',
    'settings.cloudResponseLengthSub': '0 = 跟随云端默认，不限制',
    'settings.cloudLengthUnlimited': '不限制（跟随云端默认）',
    'settings.cloudContextWindow': '云端上下文窗口',
    'settings.cloudContextWindowSub': '云端模型的历史裁剪预算',
    'settings.cloudHistoryLimit': '云端历史条数',
    'settings.cloudHistoryLimitSub': '云端上下文中保留的对话轮数',
    'settings.inferenceBackend': '推理后端',
    'settings.inferenceBackendSub': '模型推理使用的引擎',
    'settings.backendModel': '跟随模型',
    'settings.backendAuto': '自动',
    'settings.backendCpu': 'CPU',
    'settings.backendOpencl': 'OpenCL（GPU）',
    'settings.backendVulkan': 'Vulkan（GPU）',
    'settings.performanceEcoSub': '降低发热，延长续航',
    'settings.performanceBalancedSub': '兼顾速度与续航',
    'settings.performanceFastSub': '最大化本地推理速度',
    'settings.responseLength': '回复长度',
    'settings.responseLengthCustom': '可选预设，也可自定义 token 上限',
    'settings.lengthShort': '简短',
    'settings.lengthNormal': '标准',
    'settings.lengthLong': '较长',
    'settings.contextMemory': '上下文记忆',
    'settings.contextMemoryCustom': '可选预设，也可自定义历史深度',
    'settings.contextLight': '轻量',
    'settings.contextBalanced': '均衡',
    'settings.contextDeep': '深度',
    'settings.contextLightSub': '仅保留最近对话',
    'settings.contextBalancedSub': '默认记忆深度',
    'settings.contextDeepSub': '保留更多对话历史',
    'settings.promptCache': '会话加速',
    'settings.promptCacheOnSub': '连续对话会启用本地提示词缓存',
    'settings.promptCacheOffSub': '提示词缓存未启用',
    'settings.releaseModel': '释放模型内存',
    'settings.releaseModelSub': '卸载当前模型并清空活跃 KV 缓存',
    'settings.release': '释放',
    'settings.perModelConfig': '单模型偏好',
    'settings.perModelConfigSub': '预留单模型默认值入口',
    'settings.soon': '待接入',
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
    'onboarding.settingsTitle': '新手引导',
    'onboarding.settingsSub': '一步一步完成首次设置',
    'onboarding.open': '开始',
    'onboarding.kicker': '新手引导',
    'onboarding.close': '关闭引导',
    'onboarding.welcomeTitle': '先把手机准备好',
    'onboarding.welcomeBody': 'Lociant 可以让云端模型调用手机上的应用、屏幕和系统能力。跟着这几步走，第一次使用也不会迷路。',
    'onboarding.welcomeNote': '推荐先用云端模型，配置简单，手机只负责执行动作。',
    'onboarding.cloudTitle': '先接入云端模型',
    'onboarding.cloudBody': '推荐 DeepSeek。注册并复制 API Key 后填到下面，密钥只保存在这台手机上。',
    'onboarding.deepseekHint': '适合第一次使用，按量计费，配置清楚',
    'onboarding.usePreset': '使用预设',
    'onboarding.apiKeyLabel': 'DeepSeek API Key',
    'onboarding.cloudConfigured': '云端模型已经准备好。',
    'onboarding.cloudPresetApplied': 'DeepSeek 地址和模型已填好，请补上 API Key。',
    'onboarding.cloudRequired': '请先填入 API Key，再继续。',
    'onboarding.skipCloud': '暂时跳过，使用本地模型',
    'onboarding.runtimeTitle': '开启运行时',
    'onboarding.runtimeBody': '运行时是手机里的小服务。云端模型负责理解你的话，它负责真正点击、输入、读取页面。',
    'onboarding.runtimeHint': '保持运行时开启，Agent 才能操作手机。',
    'onboarding.runtimeStopped': '未启动',
    'onboarding.runtimeStarting': '启动中...',
    'onboarding.runtimeRunning': '运行时已开启',
    'onboarding.startRuntime': '启动运行时',
    'onboarding.waitRuntime': '等待运行时启动',
    'onboarding.permissionsTitle': '给它必要的权限',
    'onboarding.permissionsBody': '无障碍权限是必需的，用来点击、输入和读取当前页面。悬浮窗、通知和摄像头按需要开启。',
    'onboarding.accessibility': '无障碍服务',
    'onboarding.accessibilityHint': '必需：操作和读取应用',
    'onboarding.overlay': '悬浮窗',
    'onboarding.overlayHint': '可选：显示运行状态',
    'onboarding.granted': '已就绪',
    'onboarding.needsGrant': '需要授权',
    'onboarding.optional': '可选',
    'onboarding.grantAccessibility': '去开启无障碍',
    'onboarding.openAppPermissions': '打开应用权限',
    'onboarding.mcpTitle': '把手机接给电脑上的 Agent',
    'onboarding.mcpBody': 'MCP 是一条标准连接，让 Codex、Claude 或其他客户端发现并调用手机工具。手机负责执行，电脑负责对话和编排。',
    'onboarding.mcpEndpointLabel': 'MCP 地址',
    'onboarding.copyMcpConfig': '复制 MCP 配置',
    'onboarding.mcpHint': '在“设置 → 运行时 → 服务”中，也可以复制完整 MCP 配置和 API 令牌。电脑和手机需要在同一个局域网。',
    'onboarding.localTitle': '本地模型，实验性功能',
    'onboarding.localBody': '本地模型不需要把请求发到云端，但安装包更大，对内存和芯片要求也更高。新手建议先用 DeepSeek，之后再慢慢试。',
    'onboarding.localNote': '旧手机也可以试试，只是速度和可用模型会因设备而异。',
    'onboarding.viewLocalModels': '查看本地模型',
    'onboarding.viewModelMarket': '打开模型市场',
    'onboarding.doneTitle': '可以开始了',
    'onboarding.doneBody': '回到主页，直接说出你想做的事。先从简单任务开始，成功后再交给它更复杂的工作。',
    'onboarding.exampleWechat': '“打开微信，帮我整理未读消息”',
    'onboarding.exampleBilibili': '“看看 B 站有什么新动态”',
    'onboarding.exampleApp': '“打开这个新 App，帮我试试登录流程”',
    'onboarding.rerunHint': '以后想重新看？进入“设置 → 新手引导”即可再次运行。',
    'onboarding.previous': '上一步',
    'onboarding.skip': '以后再看',
    'onboarding.start': '开始设置',
    'onboarding.next': '下一步',
    'onboarding.finish': '回到主页',
    'onboarding.stepCount': '第 %s 步，共 %s 步',
    'about.title': '关于 Lociant',
    'about.subtitle': '版本、源码与致谢',
    'about.version': '1.1.1',
    'about.versionLabel': '软件版本',
    'about.versionSub': '应用版本',
    'about.repository': '项目仓库',
    'about.license': '许可证',
    'about.licenseSub': 'MIT License',
    'about.open': '打开',
    'about.creditsTitle': '感谢开源项目',
    'about.mnnSub': '端侧神经网络推理',
    'about.ncnnSub': '移动端视觉推理',
    'about.ktorSub': 'Kotlin 服务端框架',
    'about.dompurifySub': 'HTML 安全清理',
    'settings.permissionsTitle': '权限',
    'settings.cameraPermission': '摄像头',
    'settings.notificationPermission': '通知',
    'settings.overlayPermission': '悬浮窗',
    'settings.battery': '后台电量',
    'settings.accessibilityPermission': '无障碍',
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
    'settings.accessibilityPermissionGranted': '就绪',
    'settings.accessibilityPermissionNeeded': '需要授权',
    'settings.allowed': '已允许',
    'settings.allow': '允许',
    'settings.grant': '授权',
    'settings.manage': '管理',
    'settings.open': '打开',
    'settings.request': '授权',
    'settings.currentSession': '当前',
    'settings.newSession': '新建',
    'diagnostics.title': '运行时诊断',
    'diagnostics.summaryTitle': '运行时就绪状态',
    'diagnostics.summarySub': '检查运行时、工具、模型与视觉状态',
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
    'models.cloudTitle': '云端模型',
    'models.cloudSub': 'OpenAI 兼容云端 API',
    'models.cloudHintTitle': '设为默认模型',
    'models.cloudHint': '保存后，在运行时的模型列表中选择它作为默认模型',
    'models.cloudHintEnabled': '已就绪。到运行时设置中把它设为默认模型即可使用',
    'models.marketTitle': '模型市场',
    'models.marketSub': 'ModelScope MNN 模型',
    'models.marketSearch': '搜索模型',
    'models.install': '安装',
    'models.installed': '已安装',
    'models.installing': '安装中',
    'models.delete': '删除',
    'models.marketNeedsRuntime': '启动运行时即可连接模型市场',
    'models.marketStartRuntime': '启动运行时',

    'empty.models': '暂无模型',
    'toast.modelsReloaded': '模型已刷新',
    'toast.modelMarketLoaded': '市场已加载',
    'toast.modelMarketFailed': '市场加载失败',
    'toast.modelImported': '模型已导入',
    'toast.modelImportFailed': '导入失败',
    'toast.modelDeleted': '模型已删除',
    'toast.modelReleased': '模型已释放',
    'toast.modelDeleteFailed': '删除失败',
    'toast.copied': '已复制',
    'toast.backendFallback': '上次异常退出，推理后端已回退为默认',
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
const navItems = Array.from(document.querySelectorAll('.nav-item'))
const panels = Array.from(document.querySelectorAll('.panel'))
const modelHomeView = document.getElementById('modelHomeView')
const modelLocalView = document.getElementById('modelLocalView')
const modelLocalButton = document.getElementById('modelLocalButton')
const modelLocalBack = document.getElementById('modelLocalBack')
const modelLocalState = document.getElementById('modelLocalState')
const modelRuntimeButton = document.getElementById('modelRuntimeButton')
const modelCloudButton = document.getElementById('modelCloudButton')
const modelCloudBack = document.getElementById('modelCloudBack')
const modelCloudState = document.getElementById('modelCloudState')
const modelCloudHintText = document.getElementById('modelCloudHintText')
const modelCloudView = document.getElementById('modelCloudView')
const modelRuntimeState = document.getElementById('modelRuntimeState')
const modelList = document.getElementById('modelList')
const modelMarketPanel = document.getElementById('modelMarketPanel')
const modelMarketBack = document.getElementById('modelMarketBack')
const modelMarketList = document.getElementById('modelMarketList')
const modelMarketSearch = document.getElementById('modelMarketSearch')
const modelMarketRefreshButton = document.getElementById('modelMarketRefreshButton')
const modelMarketRuntimeHint = document.getElementById('modelMarketRuntimeHint')
const modelMarketStartButton = document.getElementById('modelMarketStartButton')
const modelProgress = document.getElementById('modelProgress')
const modelProgressText = document.getElementById('modelProgressText')
const modelProgressPercent = document.getElementById('modelProgressPercent')
const modelProgressFill = document.getElementById('modelProgressFill')
const modelReloadButton = document.getElementById('modelReloadButton')
const modelImportButton = document.getElementById('modelImportButton')
const modelMarketButton = document.getElementById('modelMarketButton')
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
const accessibilityPermissionState = document.getElementById('accessibilityPermissionState')
const cameraPermissionButton = document.getElementById('cameraPermissionButton')
const notificationPermissionButton = document.getElementById('notificationPermissionButton')
const overlayPermissionButton = document.getElementById('overlayPermissionButton')
const batteryPermissionButton = document.getElementById('batteryPermissionButton')
const accessibilityPermissionButton = document.getElementById('accessibilityPermissionButton')
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
const runtimePerformanceModeInput = document.getElementById('runtimePerformanceModeInput')
const runtimePerformanceText = document.getElementById('runtimePerformanceText')
const runtimeBackendInput = document.getElementById('runtimeBackendInput')
const runtimeBackendText = document.getElementById('runtimeBackendText')
const runtimeCloudEnabledInput = document.getElementById('runtimeCloudEnabledInput')
const runtimeCloudBaseUrlInput = document.getElementById('runtimeCloudBaseUrlInput')
const runtimeCloudApiKeyInput = document.getElementById('runtimeCloudApiKeyInput')
const runtimeCloudModelInput = document.getElementById('runtimeCloudModelInput')
const runtimeResponseLengthInput = document.getElementById('runtimeResponseLengthInput')
const runtimeResponseLengthText = document.getElementById('runtimeResponseLengthText')
const runtimeResponseTokensInput = document.getElementById('runtimeResponseTokensInput')
const runtimeContextMemoryInput = document.getElementById('runtimeContextMemoryInput')
const runtimeContextText = document.getElementById('runtimeContextText')
const runtimeHistoryLimitInput = document.getElementById('runtimeHistoryLimitInput')
const runtimeCacheState = document.getElementById('runtimeCacheState')
const runtimeCacheBadge = document.getElementById('runtimeCacheBadge')
const runtimeReleaseModelButton = document.getElementById('runtimeReleaseModelButton')
const runtimeAdvancedButton = document.getElementById('runtimeAdvancedButton')
const runtimeAdvancedState = document.getElementById('runtimeAdvancedState')
const runtimeAdvancedPanel = document.getElementById('runtimeAdvancedPanel')
const runtimeAdvancedBack = document.getElementById('runtimeAdvancedBack')
const runtimeAboutButton = document.getElementById('runtimeAboutButton')
const aboutPanel = document.getElementById('aboutPanel')
const aboutBack = document.getElementById('aboutBack')
const aboutVersion = document.getElementById('aboutVersion')
const runtimeSessionCurrent = document.getElementById('runtimeSessionCurrent')
const runtimeSessionList = document.getElementById('runtimeSessionList')
const runtimeSessionNewButton = document.getElementById('runtimeSessionNewButton')
const runtimeDiagRunButton = document.getElementById('runtimeDiagRunButton')
const runtimeDiagSummary = document.getElementById('runtimeDiagSummary')
const runtimeDiagSummaryText = document.getElementById('runtimeDiagSummaryText')
const languageControl = document.getElementById('languageControl')
const toast = document.getElementById('toast')
const runtimeDefaultTokens = document.getElementById('runtimeDefaultTokens')
const runtimeModelTokens = document.getElementById('runtimeModelTokens')
const runtimeEffectiveTokens = document.getElementById('runtimeEffectiveTokens')
const runtimeDeviceState = document.getElementById('runtimeDeviceState')
const settingsHomePage = document.getElementById('settingsList')
const onboardingSettingsButton = document.getElementById('onboardingSettingsButton')
const onboardingModal = document.getElementById('onboardingModal')
const onboardingDialog = document.querySelector('#onboardingModal .onboarding-dialog')
const onboardingCloseButton = document.getElementById('onboardingCloseButton')
const onboardingBackButton = document.getElementById('onboardingBackButton')
const onboardingSkipButton = document.getElementById('onboardingSkipButton')
const onboardingPrimaryButton = document.getElementById('onboardingPrimaryButton')
const onboardingStepCount = document.getElementById('onboardingStepCount')
const onboardingSteps = Array.from(document.querySelectorAll('[data-onboarding-step]'))
const onboardingProgress = Array.from(document.querySelectorAll('.onboarding-progress span'))
const onboardingDeepSeekButton = document.getElementById('onboardingDeepSeekButton')
const onboardingApiKeyInput = document.getElementById('onboardingApiKeyInput')
const onboardingCloudStatus = document.getElementById('onboardingCloudStatus')
const onboardingCloudSkipButton = document.getElementById('onboardingCloudSkipButton')
const onboardingMcpUrl = document.getElementById('onboardingMcpUrl')
const onboardingMcpCopyButton = document.getElementById('onboardingMcpCopyButton')
const onboardingRuntimeDot = document.getElementById('onboardingRuntimeDot')
const onboardingRuntimeStatus = document.getElementById('onboardingRuntimeStatus')
const onboardingAccessibilityStatus = document.getElementById('onboardingAccessibilityStatus')
const onboardingOverlayStatus = document.getElementById('onboardingOverlayStatus')
const onboardingAccessibilityButton = document.getElementById('onboardingAccessibilityButton')
const onboardingAppPermissionButton = document.getElementById('onboardingAppPermissionButton')
const onboardingLocalButton = document.getElementById('onboardingLocalButton')
const onboardingMarketButton = document.getElementById('onboardingMarketButton')
const homeRailToggle = document.getElementById('homeRailToggle')
const homeSidebar = document.getElementById('homeSidebar')
const homeSessionCount = document.getElementById('homeSessionCount')
const homeNewChatButton = document.getElementById('homeNewChatButton')
const homeSessionList = document.getElementById('homeSessionList')
const homeChatForm = document.getElementById('homeChatForm')
const homeChatInput = document.getElementById('homeChatInput')
const homeChatSendButton = document.getElementById('homeChatSendButton')
const homeChatFeed = document.getElementById('homeChatFeed')
const homeChatTitle = document.getElementById('homeChatTitle')
const homeChatMeta = document.getElementById('homeChatMeta')
const homeChatState = document.getElementById('homeChatState')
const homeImageInput = document.getElementById('homeImageInput')
const homeImagePreview = document.getElementById('homeImagePreview')
const homeImagePreviewImg = document.getElementById('homeImagePreviewImg')
const homeImagePreviewName = document.getElementById('homeImagePreviewName')
const homeImageRemoveButton = document.getElementById('homeImageRemoveButton')
// ---- State variables ----
let runtimeServiceState = null
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
let currentLocale = systemLocale()
const localeStorePath = '/api/v1/store/runtime-settings/locale'
let homeAttachedImage = null
let activePage = 'home'

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
    runtimeModelPanel,
    runtimeAdvancedPanel,
    aboutPanel,
  ].filter(Boolean)
}

// ---- Surface switching ----
function animateSurface(node) {
  if (!node) return
  node.classList.remove('animate-in')
  void node.offsetWidth
  node.classList.add('animate-in')
}

function showPanel(panel) {
  if (!panel) return
  panels.forEach(item => {
    const active = item === panel
    item.classList.toggle('active', active)
    if (!active) item.classList.remove('animate-in')
  })
  animateSurface(panel)
}

function showPagePanel(page) {
  activePage = page || 'home'
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


/* === 03-api.js === */
/* ── Lociant WebUI — API client and native bridge ── */

function native(method, ...args) {
  try {
    const bridge = window.LociantBridge
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

async function apiPut(path, body) {
  return apiRequest('PUT', path, body)
}

async function apiDelete(path) {
  return apiRequest('DELETE', path)
}

async function apiRequest(method, path, body) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (runtimeServiceState && runtimeServiceState.authToken) headers.Authorization = 'Bearer ' + runtimeServiceState.authToken
  const response = await fetch(apiUrl(path), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  // Control resources may return an empty 204, while errors use Problem Details.
  const text = await response.text()
  const json = text ? JSON.parse(text) : null
  if (!response.ok) {
    const message = (json && (json.detail || (json.error && json.error.message) || json.message)) || 'API request failed'
    throw new Error(path + ': ' + message)
  }
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

function runtimeState() {
  return nativeJson('runtimeState', runtimeServiceState || { running: false })
}


/* === 04-runtime.js === */
/* ── Lociant WebUI — Runtime state management and commands ── */

function invokeRuntimeBridge(method, ...args) {
  try {
    const raw = native(method, ...args)
    const next = raw ? JSON.parse(raw) : { running: false, message: 'Runtime bridge unavailable' }
    updateRuntimeServiceState(next)
    return next
  } catch (error) {
    updateRuntimeServiceState({ running: false, message: 'Runtime bridge call failed' })
    return Promise.reject(error)
  }
}

function startRuntime(payload) { return invokeRuntimeBridge('startRuntime', JSON.stringify(payload || {})) }
function stopRuntime() { return invokeRuntimeBridge('stopRuntime') }
function updateRuntimeSettings(payload) { return invokeRuntimeBridge('updateRuntimeSettings', JSON.stringify(payload || {})) }
function releaseRuntimeModel() { return invokeRuntimeBridge('releaseRuntimeModel') }
function createRuntimeSession() { return invokeRuntimeBridge('createSession') }
function selectRuntimeSession(sessionId) { return invokeRuntimeBridge('selectSession', String(sessionId || '')) }
function deleteRuntimeSession(sessionId) { return invokeRuntimeBridge('deleteSession', String(sessionId || '')) }
function loadRuntimeSession(sessionId) { return invokeRuntimeBridge('sessionDetails', String(sessionId || '')) }
function startRuntimeVision(payload) { return invokeRuntimeBridge('startVision', JSON.stringify(payload || {})) }
function stopRuntimeVision() { return invokeRuntimeBridge('stopVision') }
function updateRuntimeWindow(payload) { return invokeRuntimeBridge('updateRuntimeWindow', JSON.stringify(payload || {})) }

function runtimeWindowCommand(command) {
  const next = command || ((runtimeServiceState && runtimeServiceState.windowVisible) ? 'hide' : 'show')
  const state = invokeRuntimeBridge(next === 'hide' ? 'hideRuntimeWindow' : 'showRuntimeWindow')
  return state.window || state
}

function updateRuntimeServiceState(state) {
  runtimeServiceState = Object.assign({}, runtimeServiceState || {}, state || {})
  const running = !!runtimeServiceState.running
  const starting = !!runtimeServiceState.starting
  const cameraGranted = runtimeServiceState.cameraPermissionGranted === true
  const notificationGranted = runtimeServiceState.notificationPermissionGranted === true
  const overlayGranted = runtimeServiceState.windowAllowed === true
  const batteryGranted = !!runtimeServiceState.batteryOptimizationIgnored
  const accessibilityGranted = runtimeServiceState.accessibilityPermissionGranted === true
  syncTopStatus()
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
  if (accessibilityPermissionState) accessibilityPermissionState.textContent = devicePermissionLabel(accessibilityGranted, t('settings.accessibilityPermissionGranted'), t('settings.accessibilityPermissionNeeded'))
  setPermissionButton(cameraPermissionButton, cameraGranted)
  setPermissionButton(notificationPermissionButton, notificationGranted)
  setPermissionButton(overlayPermissionButton, overlayGranted)
  setPermissionButton(batteryPermissionButton, batteryGranted)
  setPermissionButton(accessibilityPermissionButton, accessibilityGranted)
  const vision = visionState()
  const visionRunning = !!vision.running
  const visionStarting = String(vision.state || '').toLowerCase() === 'starting'
  const visionLabel = visionStateLabel(vision)
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
  updateModelExperienceState()
  if (runtimeServiceState.authToken !== undefined) {
    if (runtimeAuthTokenInput.value !== runtimeServiceState.authToken) {
      runtimeAuthTokenInput.value = runtimeServiceState.authToken || ''
    }
  }
  if (runtimeServiceState.toolExposure) runtimeToolExposureInput.value = runtimeServiceState.toolExposure
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
  if (typeof updateModelMarketHint === 'function') updateModelMarketHint()
  if (typeof updateModelCloudState === 'function') updateModelCloudState()
  if (state.inferenceBackendFallback && !window.__lociantBackendFallbackNotified) {
    window.__lociantBackendFallbackNotified = true
    showToast(t('toast.backendFallback'))
  }
}

function syncTopStatus() {
  if (!stateText && !stateDot) return
  const running = !!(runtimeServiceState && runtimeServiceState.running)
  const starting = !!(runtimeServiceState && runtimeServiceState.starting)
  const active = running || starting
  if (stateText) {
    stateText.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('state.idle'))
    stateText.classList.toggle('running', active)
  }
  if (stateDot) stateDot.classList.toggle('running', active)
}

function updateModelExperienceState() {
  const state = runtimeServiceState || {}
  const maxThreads = Number(state.maxCpuThreads) || 16
  const threads = Number(state.cpuThreads) || 4
  const performance = performanceModeFromThreads(threads, maxThreads)
  if (runtimePerformanceModeInput) runtimePerformanceModeInput.value = performance
  if (runtimePerformanceText) runtimePerformanceText.textContent = performanceSubText(performance)

  const backend = String(state.inferenceBackend || 'model')
  if (runtimeBackendInput) runtimeBackendInput.value = backend
  if (runtimeBackendText) runtimeBackendText.textContent = ({
    model: t('settings.backendModel'),
    auto: t('settings.backendAuto'),
    cpu: t('settings.backendCpu'),
    opencl: t('settings.backendOpencl'),
    vulkan: t('settings.backendVulkan')
  })[backend] || t('settings.backendModel')

  const cloudEnabled = !!state.cloudEnabled
  if (runtimeCloudEnabledInput) runtimeCloudEnabledInput.checked = cloudEnabled
  if (runtimeCloudBaseUrlInput && document.activeElement !== runtimeCloudBaseUrlInput) runtimeCloudBaseUrlInput.value = String(state.cloudBaseUrl || '')
  if (runtimeCloudApiKeyInput && document.activeElement !== runtimeCloudApiKeyInput) runtimeCloudApiKeyInput.value = String(state.cloudApiKey || '')
  if (runtimeCloudModelInput && document.activeElement !== runtimeCloudModelInput) runtimeCloudModelInput.value = String(state.cloudModel || '')
  const cloudTokens = Number(state.cloudMaxOutputTokens) || 0
  if (runtimeCloudResponseLengthInput && document.activeElement !== runtimeCloudResponseLengthInput) {
    const cloudPreset = [0, 1024, 4096, 8192].includes(cloudTokens) ? String(cloudTokens) : 'custom'
    runtimeCloudResponseLengthInput.value = cloudPreset
    if (runtimeCloudResponseTokensInput) {
      runtimeCloudResponseTokensInput.value = String(cloudTokens)
      runtimeCloudResponseTokensInput.classList.toggle('is-hidden', cloudPreset !== 'custom')
    }
  }
  if (runtimeCloudResponseLengthText) {
    runtimeCloudResponseLengthText.textContent = cloudTokens > 0
      ? (cloudTokens + ' tokens')
      : t('settings.cloudLengthUnlimited')
  }
  const cloudContext = Number(state.cloudContextWindow) || 131072
  if (runtimeCloudContextWindowInput && document.activeElement !== runtimeCloudContextWindowInput) {
    const ctxPreset = [16384, 32768, 65536, 131072, 262144].includes(cloudContext) ? String(cloudContext) : 'custom'
    runtimeCloudContextWindowInput.value = ctxPreset
    if (runtimeCloudContextWindowTokensInput) {
      runtimeCloudContextWindowTokensInput.value = String(cloudContext)
      runtimeCloudContextWindowTokensInput.classList.toggle('is-hidden', ctxPreset !== 'custom')
    }
  }
  if (runtimeCloudHistoryLimitInput && document.activeElement !== runtimeCloudHistoryLimitInput) {
    runtimeCloudHistoryLimitInput.value = String(Number(state.cloudHistoryLimit) || 256)
  }

  const tokens = Number(state.maxOutputTokens) || 512
  const responseMode = responsePresetForTokens(tokens)
  if (runtimeResponseLengthInput) runtimeResponseLengthInput.value = responseMode || 'custom'
  if (runtimeResponseLengthText) runtimeResponseLengthText.textContent = tokens + ' tokens'
  if (runtimeResponseTokensInput && document.activeElement !== runtimeResponseTokensInput) {
    runtimeResponseTokensInput.max = String(Number(state.hardMaxOutputTokens) || 32768)
    runtimeResponseTokensInput.value = String(tokens)
  }
  if (runtimeResponseTokensInput) {
    runtimeResponseTokensInput.classList.toggle('is-hidden', !!responseMode)
  }

  const historyLimit = Number((state.sessionPolicy && state.sessionPolicy.historyLimit) || state.historyLimit) || 64
  const contextProfile = state.contextProfile || (state.sessionPolicy && state.sessionPolicy.contextProfile) || contextPresetForHistoryLimit(historyLimit)
  const contextMode = contextSelectValue(contextProfile, historyLimit)
  if (runtimeContextMemoryInput) runtimeContextMemoryInput.value = contextMode
  if (runtimeContextText) runtimeContextText.textContent = contextSubText(contextProfile) + (historyLimit ? (' / ' + historyLimit) : '')
  if (runtimeHistoryLimitInput && document.activeElement !== runtimeHistoryLimitInput) {
    const maxHistory = Number(state.sessionPolicy && state.sessionPolicy.maxHistoryLimit) || 256
    runtimeHistoryLimitInput.max = String(maxHistory)
    runtimeHistoryLimitInput.value = String(historyLimit)
  }
  if (runtimeHistoryLimitInput) {
    runtimeHistoryLimitInput.classList.toggle('is-hidden', contextMode !== 'custom')
  }

  const cache = state.sessionPolicy && state.sessionPolicy.cache
  const cacheOn = !!(cache && cache.promptCache)
  if (runtimeCacheState) runtimeCacheState.textContent = cacheOn ? t('settings.promptCacheOnSub') : t('settings.promptCacheOffSub')
  if (runtimeCacheBadge) {
    runtimeCacheBadge.textContent = cacheOn ? t('diagnostics.ready') : t('status.stopped')
    runtimeCacheBadge.classList.toggle('running', cacheOn)
  }
}

function performanceModeFromThreads(threads, maxThreads) {
  const max = Math.max(1, maxThreads)
  if (threads <= Math.max(1, Math.floor(max / 3))) return 'eco'
  if (threads >= Math.max(2, Math.ceil(max * 0.75))) return 'fast'
  return 'balanced'
}

function threadsForPerformanceMode(mode) {
  const max = Number(runtimeServiceState && runtimeServiceState.maxCpuThreads) || 16
  if (mode === 'eco') return Math.max(1, Math.floor(max / 3))
  if (mode === 'fast') return Math.max(1, max)
  return Math.max(1, Math.min(max, Math.round(max / 2)))
}

function responsePresetForTokens(tokens) {
  const value = Number(tokens)
  if (value === 256 || value === 512 || value === 1024) return String(value)
  return ''
}

function contextPresetForHistoryLimit(limit) {
  if (limit <= 24) return 'light'
  if (limit >= 96) return 'deep'
  return 'balanced'
}

function historyLimitForContextProfile(profile) {
  return ({ light: 16, balanced: 64, deep: 128 })[profile] || 64
}

function contextSelectValue(profile, historyLimit) {
  const normalized = ['light', 'balanced', 'deep'].includes(profile) ? profile : contextPresetForHistoryLimit(historyLimit)
  return Number(historyLimit) === historyLimitForContextProfile(normalized) ? normalized : 'custom'
}

function performanceSubText(mode) {
  return ({
    eco: t('settings.performanceEcoSub'),
    fast: t('settings.performanceFastSub'),
  })[mode] || t('settings.performanceBalancedSub')
}

function contextSubText(profile) {
  return ({
    light: t('settings.contextLightSub'),
    deep: t('settings.contextDeepSub'),
  })[profile] || t('settings.contextBalancedSub')
}

function updateHomeState() {
  const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions) ? runtimeServiceState.sessions : []
  const visibleSessions = typeof activeHomeSessions === 'function' ? activeHomeSessions(runtimeServiceState) : sessions
  syncTopStatus()
  if (homeSessionCount) homeSessionCount.textContent = String(visibleSessions.length)
  renderHomeSessions(sessions)
  if (typeof updateHomeChatContext === 'function') updateHomeChatContext()
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


/* === 06-models.js === */
/* ── Lociant WebUI — Model management ── */

function setModelView(view) {
  modelView = view || 'home'
  const views = { home: modelHomeView, local: modelLocalView, market: modelMarketPanel, cloud: modelCloudView }
  Object.keys(views).forEach(key => {
    const node = views[key]
    if (!node) return
    const active = key === modelView
    node.classList.toggle('active', active)
    node.setAttribute('aria-hidden', active ? 'false' : 'true')
  })
  if (modelView === 'local') loadModels()
  if (modelView === 'market') {
    updateModelMarketHint()
    renderModelMarket(marketModels)
    if (!marketModels.length) loadModelMarket()
  }
  if (modelView === 'cloud') {
    updateModelCloudState()
  }
}

function updateModelCloudState() {
  if (!modelCloudState || !modelCloudHintText) return
  const state = runtimeServiceState || {}
  const enabled = !!state.cloudEnabled && !!state.cloudModel
  modelCloudState.textContent = enabled ? String(state.cloudModel) : '--'
  modelCloudState.classList.toggle('running', enabled)
  modelCloudHintText.textContent = enabled
    ? t('models.cloudHintEnabled')
    : t('models.cloudHint')
}

function loadModels(refresh) {
  const path = refresh ? '/api/v1/models?refresh=true' : '/api/v1/models'
  return retryApi(() => apiGet(path), () => ({ models: [] })).then(data => {
    runtimeModels = data && Array.isArray(data.models) ? data.models : []
    renderModels(runtimeModels)
    updateModelHomeState()
    return runtimeModels
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
  apiDelete('/api/v1/models/' + encodeURIComponent(modelId))
    .then(() => {
      loadModels()
      showToast(t('toast.modelDeleted'))
    })
    .catch(() => showToast(t('toast.modelDeleteFailed')))
}

// ---- Model Market ----
function loadModelMarket(forceRefresh) {
  const query = marketQuery
  const url = '/api/v1/catalog/models' + (query ? '?q=' + encodeURIComponent(query) : '') + (forceRefresh ? (query ? '&refresh=true' : '?refresh=true') : '')
  apiGet(url)
    .then(data => {
      marketModels = (data && Array.isArray(data.models)) ? data.models : []
      renderModelMarket(marketModels)
      updateModelMarketHint()
      showToast(t('toast.modelMarketLoaded'))
    })
    .catch(() => {
      updateModelMarketHint()
      const state = runtimeServiceState || {}
      if (state.running || state.starting) showToast(t('toast.modelMarketFailed'))
    })
}

function updateModelMarketHint() {
  if (!modelMarketRuntimeHint) return
  const state = runtimeServiceState || {}
  const needsRuntime = !(state.running || state.starting)
  modelMarketRuntimeHint.classList.toggle('is-hidden', !needsRuntime)
}

function startRuntimeForMarket() {
  startRuntime({})
  updateModelMarketHint()
  let attempts = 0
  const timer = window.setInterval(() => {
    const state = runtimeServiceState || {}
    attempts += 1
    if (state.running) {
      window.clearInterval(timer)
      updateModelMarketHint()
      loadModelMarket(true)
    } else if (attempts >= 30) {
      window.clearInterval(timer)
      updateModelMarketHint()
    } else {
      updateModelMarketHint()
    }
  }, 1000)
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
  apiPost('/api/v1/model-installations', { modelId: model.id })
    .then(result => {
      pollMarketInstall(result.jobId, model.id)
      loadModels()
    })
    .catch(() => {
      marketInstallingModelId = ''
      setModelProgress({ state: 'error', message: t('toast.modelImportFailed') })
      showToast(t('toast.modelImportFailed'))
    })
}

function pollMarketInstall(jobId, modelId) {
  if (marketInstallTimer) window.clearInterval(marketInstallTimer)
  let retries = 0
  marketInstallTimer = window.setInterval(() => {
    apiGet('/api/v1/model-installations/' + encodeURIComponent(jobId))
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
  const modelId = payload.modelId || fallbackModelId || marketInstallingModelId || ''
  const rawPercent = Number(payload.percent ?? payload.progress)
  const hasPercent = Number.isFinite(rawPercent)
  const percent = hasPercent ? (rawPercent <= 1 ? rawPercent * 100 : rawPercent) : null

  let state = rawState
  if (!state) {
    if (active === false && hasPercent && percent >= 100) state = 'done'
    else if (active === false && !hasPercent) state = 'installing'
    else state = 'installing'
  }

  if (state === 'done' || (active === false && percent !== null && percent >= 100)) {
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
  }
}

// ---- Model choice (settings panel) ----
function renderRuntimeModelChoices(models) {
  if (!runtimeModelList) return
  runtimeModelList.innerHTML = ''
  const readyModels = (Array.isArray(models) ? models : []).filter(model => model && model.ready && isRuntimeModel(model))
  const state = runtimeServiceState || {}
  const cloudId = String(state.cloudModel || '').trim()
  const cloudIdLower = cloudId.toLowerCase()
  if (state.cloudEnabled && cloudId && !readyModels.some(m => m && String(m.id || '').toLowerCase() === cloudIdLower)) {
    readyModels.push({ id: cloudId, name: cloudId, runtime: 'cloud', type: 'chat', ready: true, installed: true, cloud: true })
  }
  const currentModelId = state.modelId || (readyModels[0] && readyModels[0].id) || '--'
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
      updateRuntimeSettings({ modelId: model.id })
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
const LOCALE_STORAGE_KEY = 'lociant.locale'

function storedLocaleMode() {
  try {
    const raw = window.localStorage.getItem(LOCALE_STORAGE_KEY)
    return raw ? String(raw) : ''
  } catch (error) {
    return ''
  }
}

function persistLocaleMode(mode) {
  try {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, mode || 'system')
  } catch (error) {}
}

function loadLocaleSetting() {
  const stored = storedLocaleMode()
  if (stored) {
    localeSetting = { mode: stored }
    applyLocale()
    return
  }
  apiGet(localeStorePath)
    .then(result => {
      localeSetting = result && result.value ? result.value : { mode: 'system' }
      persistLocaleMode(localeSetting.mode || 'system')
      applyLocale()
    })
    .catch(() => applyLocale())
}

function saveLocaleSetting(mode) {
  localeSetting = { mode: mode || 'system' }
  persistLocaleMode(localeSetting.mode)
  // Best-effort sync to the control-plane store for remote/API consistency.
  apiPut(localeStorePath, { value: localeSetting }).catch(() => {})
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
  if (aboutVersion) aboutVersion.textContent = t('about.version')
  Array.from(languageControl.querySelectorAll('.segmented-option')).forEach(button => {
    button.classList.toggle('active', button.dataset.langMode === (localeSetting.mode || 'system'))
  })
  updateRuntimeServiceState(runtimeServiceState || {})
  if (typeof updateOnboardingState === 'function') updateOnboardingState()
}

// ---- Sessions ----
function activeHomeSessions(state) {
  return Array.isArray(state && state.sessions) ? state.sessions : []
}

function homeSessionIdFromState(state) {
  return (state && state.currentSessionId) || ''
}

function markHomeSessionActive(state, sessionId) {
  const next = Object.assign({}, state || {})
  if (sessionId) next.currentSessionId = sessionId
  return next
}

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
      if (homeChatInFlight) return
      selectRuntimeSession(session.id)
    })
    runtimeSessionList.appendChild(row)
  })
}

function renderHomeSessions(sessions) {
  if (!homeSessionList) return
  homeSessionList.innerHTML = ''
  const policyLimit = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.recentLimit)
  const filtered = activeHomeSessions({ sessions: Array.isArray(sessions) ? sessions : [] })
  const items = policyLimit > 0 ? filtered.slice(0, policyLimit) : filtered
  if (!items.length) return
  let lastGroup = ''
  items.forEach(session => {
    const group = sessionDateGroup(session.updatedAt)
    if (group !== lastGroup) {
      lastGroup = group
      homeSessionList.appendChild(el('div', 'chat-session-date', group))
    }
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'chat-session-item pressable'
    const active = session.id === homeCurrentSessionId()
    row.classList.toggle('active', active)
    const body = el('span', 'chat-session-body')
    const title = el('strong', '', session.title || session.id || '--')
    const sub = el('span', '', sessionDisplayMeta(session) + ' · ' + (session.messageCount || 0))
    const remove = el('span', 'chat-session-delete', '×')
    remove.setAttribute('role', 'button')
    remove.setAttribute('tabindex', '0')
    remove.setAttribute('aria-label', t('home.deleteChat'))
    body.appendChild(title)
    body.appendChild(sub)
    row.appendChild(body)
    row.appendChild(remove)
    row.addEventListener('click', () => {
      if (homeChatInFlight) return
      Promise.resolve(selectRuntimeSession(session.id))
        .then(state => {
          updateRuntimeServiceState(markHomeSessionActive(state, session.id))
          loadHomeConversation(session.id)
          if (homeSidebar && homeSidebar.classList.contains('open')) {
            homeSidebar.classList.remove('open')
            if (homeRailToggle) homeRailToggle.setAttribute('aria-expanded', 'false')
          }
        })
        .catch(error => showToast((error && error.message) || t('toast.modelImportFailed')))
    })
    const deleteSession = event => {
      if (homeChatInFlight) return
      event.preventDefault()
      event.stopPropagation()
      const deletingCurrent = session.id === homeCurrentSessionId()
      Promise.resolve(deleteRuntimeSession(session.id))
        .then(state => {
          updateRuntimeServiceState(markHomeSessionActive(state || {}, deletingCurrent ? '' : homeCurrentSessionId()))
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

function sessionDateGroup(updatedAt) {
  const value = Number(updatedAt) || 0
  if (!value) return t('home.earlier')
  const date = new Date(value)
  const today = new Date()
  const startToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime()
  const startDate = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const diffDays = Math.round((startToday - startDate) / 86400000)
  if (diffDays <= 0) return t('home.today')
  if (diffDays === 1) return t('home.yesterday')
  return t('home.earlier')
}

function sessionDisplayMeta(session) {
  if (!session) return '--'
  return session.modelId || t('home.localChatMeta')
}

function currentHomeSession() {
  const currentId = homeCurrentSessionId()
  const sessions = activeHomeSessions(runtimeServiceState)
  return sessions.find(session => session && session.id === currentId) || latestHomeSession(runtimeServiceState)
}

function updateHomeChatContext() {
  const session = currentHomeSession()
  const model = (runtimeServiceState && runtimeServiceState.modelId) || t('home.localChatMeta')
  if (homeChatTitle) homeChatTitle.textContent = (session && session.title) || t('home.newChat')
  if (homeChatMeta) homeChatMeta.textContent = model
  if (homeChatState) {
    const active = !!(runtimeServiceState && runtimeServiceState.running)
    homeChatState.textContent = active ? t('status.running') : t('status.stopped')
    homeChatState.classList.toggle('running', active)
  }
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
    kind: existing.kind || 'model-chat',
    modelId: existing.modelId || ((runtimeServiceState && runtimeServiceState.modelId) || '--'),
    updatedAt: now,
    messageCount: Math.max(Number(existing.messageCount) || 0, 1) + (lastRole === 'assistant' ? 1 : 0),
    lastRole: lastRole || existing.lastRole || 'user',
    lastText: String(titleText || existing.lastText || '').slice(0, lastTextLimit),
  })
  if (index >= 0) sessions.splice(index, 1)
  sessions.unshift(next)
  runtimeServiceState = markHomeSessionActive(Object.assign({}, runtimeServiceState || {}, { sessions }), sessionId)
  updateHomeState()
  updateHomeChatContext()
}

function homeCurrentSessionId() {
  const current = runtimeServiceState && runtimeServiceState.currentSessionId
  if (current) return current
  return (runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.defaultSessionId) || ''
}

function appendChatBubble(role, text, meta) {
  if (!homeChatFeed) return null
  const node = document.createElement('div')
  node.className = 'chat-message ' + (role || 'assistant')
  if (meta && meta.active) node.dataset.activeSession = 'true'
  renderChatMarkdown(node, text)
  homeChatFeed.appendChild(node)
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return node
}

function appendAssistantRun() {
  if (!homeChatFeed) return null
  const node = document.createElement('div')
  node.className = 'chat-message assistant assistant-run'
  const chain = el('div', 'chat-tool-chain')
  const content = el('div', 'chat-run-content')
  const status = el('div', 'chat-run-status')
  node.appendChild(chain)
  node.appendChild(content)
  node.appendChild(status)
  homeChatFeed.appendChild(node)
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return { node, chain, content, status }
}

function updateRunStatus(target, text) {
  const status = target && target.status ? target.status : null
  if (!status) return
  if (text) {
    status.textContent = text
    status.classList.add('active')
  } else {
    status.textContent = ''
    status.classList.remove('active')
  }
}

function runStatusText(key, args) {
  const parts = Array.isArray(args) ? args.slice() : []
  return t(key).replace(/%s/g, () => (parts.length ? String(parts.shift()) : ''))
}

function chatTextTarget(target) {
  return target && target.content ? target.content : target
}

function chatNodeTarget(target) {
  return target && target.node ? target.node : target
}

function appendToolBubble(toolCall, target, seq) {
  if (!homeChatFeed || !toolCall) return null
  const scope = target && target.chain ? target.chain : homeChatFeed
  const id = toolCall.id || toolCall.toolCallId || ''
  const existing = id ? scope.querySelector('.tool-call-item[data-tool-call-id="' + cssEscape(id) + '"]') : null
  const node = existing || document.createElement('div')
  node.className = 'tool-call-item'
  if (id) node.dataset.toolCallId = id
  if (seq !== undefined) node.dataset.toolSeq = String(seq)
  const info = normalizeToolBubbleInfo(toolCall)
  node.classList.toggle('done', info.status === 'completed')
  node.classList.toggle('error', info.status === 'failed')
  node.replaceChildren()
  node.appendChild(el('span', 'tool-call-dot'))
  const main = el('span', 'tool-call-main')
  main.appendChild(el('span', 'tool-call-label', info.name))
  main.appendChild(el('span', 'tool-call-meta', info.meta))
  if (info.args) main.appendChild(el('code', '', info.args))
  node.appendChild(main)
  node.appendChild(el('span', 'tool-call-state', info.statusLabel))
  if (!existing) {
    scope.appendChild(node)
    // Keep bubbles in chronological (top-to-bottom) order even if events
    // arrive out of sequence.
    if (seq !== undefined && scope.querySelectorAll('.tool-call-item').length > 1) {
      const items = Array.from(scope.querySelectorAll('.tool-call-item'))
      items.sort((a, b) => Number(a.dataset.toolSeq || 0) - Number(b.dataset.toolSeq || 0))
      items.forEach(item => scope.appendChild(item))
    }
  }
  if (target && target.chain) {
    chatNodeTarget(target).classList.add('has-tools')
  }
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return node
}

function normalizeToolBubbleInfo(toolCall) {
  const rawName = toolCall.name || toolCall.functionName || toolCall.title || toolCall.kind || 'tool'
  const status = String(toolCall.status || '').toLowerCase()
  const args = toolCall.arguments || toolCall.args || ''
  const metaParts = []
  if (toolCall.kind && toolCall.kind !== rawName) metaParts.push(toolCall.kind)
  if (toolCall.type && toolCall.type !== toolCall.kind) metaParts.push(toolCall.type)
  return {
    name: rawName,
    status,
    statusLabel: toolStatusLabel(status),
    meta: metaParts.join(' · ') || 'tool call',
    args: compactJsonText(args)
  }
}

function toolStatusLabel(status) {
  if (status === 'completed' || status === 'success' || status === 'done') return 'done'
  if (status === 'failed' || status === 'error') return 'error'
  if (status === 'pending') return 'pending'
  return status || 'call'
}

function renderChatMarkdown(target, text) {
  if (!target) return
  const source = String(text || '')
  if (target) target.dataset.rawText = source
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

function createChatTextStream(target) {
  let source = ''
  let shown = ''
  let timer = null
  let closed = false
  const step = () => {
    timer = null
    if (!target || closed) return
    if (shown.length >= source.length) return
    shown = source.slice(0, shown.length + 1)
    renderChatMarkdown(target, shown)
    if (homeChatFeed) homeChatFeed.scrollTop = homeChatFeed.scrollHeight
    if (shown.length < source.length) timer = window.setTimeout(step, 12)
  }
  return {
    push(text) {
      source += String(text || '')
      if (!timer) timer = window.setTimeout(step, 8)
    },
    finish(text) {
      if (text !== undefined && String(text) !== source) source = String(text || '')
      if (timer) window.clearTimeout(timer)
      timer = null
      shown = source
      if (target) renderChatMarkdown(target, shown)
      closed = true
    },
    text() {
      return source
    }
  }
}

function compactJsonText(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  try { return JSON.stringify(JSON.parse(text)) } catch (error) { return text }
}

function cssEscape(value) {
  if (window.CSS && typeof window.CSS.escape === 'function') return window.CSS.escape(String(value || ''))
  return String(value || '').replace(/["\\]/g, '\\$&')
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
    // Tool messages carry raw result JSON; render them as a compact tool note
    // instead of dumping code into the conversation.
    if (role === 'tool') {
      const name = item && (item.name || item.toolName)
      if (name) appendToolBubble(Object.assign({}, item, { name: name, status: 'completed' }))
      return
    }
    const text = item && (item.text || item.content || item.message || '')
    if (text) appendChatBubble(role, text, { active: sessionId === homeCurrentSessionId() })
  })
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
}

function latestHomeSession(state) {
  const sessions = activeHomeSessions(state)
  return sessions.find(session => session && session.id && Number(session.messageCount || 0) > 0) ||
    sessions.find(session => session && session.id) ||
    null
}

function shouldPreferLatestHomeSession(state, currentId) {
  if (!currentId) return true
  const sessions = activeHomeSessions(state)
  const current = sessions.find(session => session && session.id === currentId)
  return !current || Number(current.messageCount || 0) <= 0
}

function restoreHomeConversation(options) {
  // A chat is still streaming into the live feed; do not reload or switch
  // sessions while it is in flight.
  if (homeChatInFlight) return
  const forceLatest = !!(options && options.forceLatest)
  const state = runtimeServiceState || runtimeState()
  updateRuntimeServiceState(state)
  const latest = latestHomeSession(state)
  const currentId = homeCurrentSessionId()
  let target = forceLatest || shouldPreferLatestHomeSession(state, currentId)
    ? (latest && latest.id)
    : currentId
  // If the last turn was interrupted, restore that session so the failure is
  // visible instead of silently switching to another conversation. Only do so
  // while the session still exists in the current list.
  if (homeChatFailure && homeChatFailure.sessionId &&
      (homeChatFailure.sessionId === currentId ||
       activeHomeSessions(state).some(session => session && session.id === homeChatFailure.sessionId))) {
    target = homeChatFailure.sessionId
  }
  if (!target) {
    clearHomeMessages()
    return null
  }
  if (target !== currentId) {
    const selected = selectRuntimeSession(target)
    updateRuntimeServiceState(markHomeSessionActive(selected || state, target))
  }
  loadHomeConversation(target, { silent: true })
  if (homeChatFailure && homeChatFailure.sessionId === target) {
    const failure = homeChatFailure
    homeChatFailure = null
    if (failure.prompt) appendChatBubble('user', failure.prompt)
    appendChatBubble('assistant', failure.message || t('toast.modelImportFailed'))
  }
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
    const main = document.createElement('span')
    main.className = 'settings-section-main'
    main.appendChild(el('span', 'settings-section-title', item.title))
    main.appendChild(el('span', 'settings-section-sub diagnostic-card-text', item.text || '--'))
    card.appendChild(main)
    card.appendChild(el('span', 'status-pill diagnostic-card-state', item.ok ? t('diagnostics.ready') : t('diagnostics.issue')))
    runtimeDiagSummary.appendChild(card)
  })
}

function toolResultText(result, fallback) {
  if (!result) return fallback
  if (result.ok === false) return result.message || fallback
  return fallback
}

function runRuntimeDiagnostics() {
  if (!runtimeDiagRunButton) return
  runtimeDiagRunButton.disabled = true
  runtimeDiagRunButton.textContent = t('diagnostics.running')
  renderDiagnosticsSummary({ running: true })
  const state = runtimeState()
  updateRuntimeServiceState(state)
  Promise.allSettled([
    apiGet('/api/v1/tools'),
    apiPost('/api/v1/tools/runtime_status/calls', { arguments: {} }),
    apiPost('/api/v1/tools/model_list/calls', { arguments: {} }),
    apiPost('/api/v1/tools/vision_status/calls', { arguments: {} })
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
    row.appendChild(el('span', 'diag-method', req.method || ''))
    row.appendChild(el('span', 'diag-path', req.endpoint || ''))
    row.appendChild(el('span', 'diag-status diag-' + (req.status < 400 ? 'ok' : 'err'), req.status || ''))
    row.appendChild(el('span', 'diag-time', (req.elapsedMs || 0) + 'ms'))
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

function openRuntimeModelSettings() {
  showSettingsDetail(runtimeModelPanel)
  if (typeof loadModels === 'function') {
    loadModels().then(() => renderRuntimeModelChoices(runtimeModels))
  } else {
    renderRuntimeModelChoices(runtimeModels)
  }
}

function openRuntimeAdvancedSettings() {
  showSettingsDetail(runtimeAdvancedPanel)
}

function openAboutSettings() {
  showSettingsDetail(aboutPanel)
}

function closeAboutSettings() {
  showSettingsHome()
}


/* === 08-ui.js === */
/* ── Lociant WebUI — UI components ── */

// ---- Sidebar ----
let sidebarBusy = false
let nativeKeyboardInset = 0
let homeChatInFlight = false
let homeChatFailure = null

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
  showPagePanel(page)
  const activePanel = document.getElementById('page-' + page)
  if (activePanel) activePanel.scrollTop = 0
  if (page === 'models') {
    setModelView(modelView)
    loadModels()
  }
  syncTopStatus()
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

let homeToolManifest = null
async function loadHomeToolManifest() {
  // Cache only a successful non-empty manifest. A failed or empty first fetch
  // (for example before the runtime is started) must not permanently disable
  // tool passing for later chats.
  if (Array.isArray(homeToolManifest) && homeToolManifest.length) return homeToolManifest
  try {
    const data = await apiGet('/api/v1/tools')
    const next = (data && Array.isArray(data.data)) ? data.data : []
    if (next.length) homeToolManifest = next
    return next
  } catch (error) {
    return homeToolManifest || []
  }
}

async function homeChatRequestBody(modelId, sessionId, prompt, image) {
  const body = {
    model: modelId,
    stream: true,
    stream_options: { include_usage: true },
    sessionId,
    messages: homeChatMessages(prompt, image)
  }
  const tools = await loadHomeToolManifest()
  if (Array.isArray(tools) && tools.length) {
    body.tools = tools
    body.execute_tools = true
  }
  return body
}

function submitHomeChat(text) {
  if (homeChatInFlight) return
  const prompt = String(text || '').trim()
  const image = homeAttachedImage
  if (!prompt && !image) return
  homeChatFailure = null
  appendChatBubble('user', image ? ((prompt || t('home.imageAttached')) + ' · ' + t('home.imageAttached')) : prompt)
  if (homeChatInput) homeChatInput.value = ''
  clearHomeImageAttachment()
  if (homeChatSendButton) homeChatSendButton.disabled = true
  const pending = appendAssistantRun()
  const modelId = (runtimeServiceState && runtimeServiceState.modelId) || ''
  const sessionId = homeCurrentSessionId()
  upsertHomeSessionPreview(sessionId, prompt || t('home.imageAttached'), 'user')
  homeChatInFlight = true
  Promise.resolve(homeChatRequestBody(modelId, sessionId, prompt, image))
    .then(body => streamOpenAiHomeChat(body, pending))
    .then(result => {
      const toolCalls = Array.isArray(result && result.toolCalls) ? result.toolCalls : []
      const text = (result && result.text) || ''
      const reply = text || t('home.emptyReply')
      if (pending && !text && !toolCalls.length) renderChatMarkdown(chatTextTarget(pending), reply)
      upsertHomeSessionPreview(sessionId, reply || prompt || t('home.imageAttached'), 'assistant')
      refreshRuntimeServiceState()
    }).catch(error => {
      const message = (error && error.message) || t('toast.modelImportFailed')
      // Remember interrupted turns so returning to the app restores them
      // instead of silently dropping the conversation.
      homeChatFailure = { sessionId: sessionId, message: message, prompt: prompt || t('home.imageAttached') }
      if (!document.hidden) {
        if (pending) renderChatMarkdown(chatTextTarget(pending), message)
        else appendChatBubble('assistant', message)
      }
    }).finally(() => {
      homeChatInFlight = false
      if (homeChatSendButton) homeChatSendButton.disabled = false
    })
}

async function streamOpenAiHomeChat(body, target) {
  const headers = { 'Content-Type': 'application/json' }
  if (runtimeServiceState && runtimeServiceState.authToken) headers.Authorization = 'Bearer ' + runtimeServiceState.authToken
  const response = await fetch(apiUrl('/v1/chat/completions'), {
    method: 'POST',
    headers,
    body: JSON.stringify(body || {})
  })
  if (!response.ok) {
    const errorJson = await response.json().catch(() => ({}))
    throw new Error((errorJson.error && errorJson.error.message) || errorJson.message || 'API request failed')
  }
  if (!response.body || !response.body.getReader) {
    const json = await response.json()
    const text = chatResponseText(json)
    const toolCalls = collectToolCallsFromMessage(json)
    toolCalls.forEach(call => appendToolBubble(call, target))
    if (target) renderChatMarkdown(chatTextTarget(target), text || t('home.emptyReply'))
    return { text, toolCalls }
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  const writer = target ? createChatTextStream(chatTextTarget(target)) : null
  let buffer = ''
  let text = ''
  let reasoningDone = false
  let reasoningText = ''
  let reasoningNode = null
  let thinkingNode = null
  const toolAccumulator = createToolCallAccumulator()
  const toolCalls = []
  let toolSeq = 0
  const toolSeqByKey = new Map()
  // Live run status: the server emits lociant phase/ping events so the UI can
  // show progress instead of looking hung while a tool runs or the model thinks.
  let lastProgressAt = Date.now()
  let streamFinished = false
  const watchdog = window.setInterval(() => {
    if (streamFinished) return
    const idleMs = Date.now() - lastProgressAt
    if (idleMs > 45000) {
      updateRunStatus(target, runStatusText('home.runStatusStall', [Math.round(idleMs / 1000)]))
    }
  }, 1000)
  function ensureReasoningUi() {
    if (reasoningNode || !target) return
    const scope = chatTextTarget(target)
    if (!scope) return
    reasoningNode = el('div', 'chat-reasoning')
    thinkingNode = el('span', 'chat-thinking', t('home.thinking'))
    const body = el('span', 'chat-reasoning-text')
    reasoningNode.appendChild(thinkingNode)
    reasoningNode.appendChild(body)
    scope.insertBefore(reasoningNode, scope.firstChild)
  }
  function finishReasoningUi() {
    if (reasoningDone || !thinkingNode) return
    reasoningDone = true
    thinkingNode.textContent = t('home.thought')
    thinkingNode.classList.add('done')
  }
  try {
    while (true) {
      const read = await reader.read()
      if (read.done) break
      buffer += decoder.decode(read.value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      for (const event of events) {
        const lines = event.split('\n').map(line => line.trim()).filter(Boolean)
        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const data = line.slice(5).trim()
          if (!data || data === '[DONE]') continue
          const json = JSON.parse(data)
          if (json && json.error) throw new Error((json.error.message) || 'API request failed')
          if (json && json.lociant) {
            const info = json.lociant
            if (info.type === 'ping') {
              continue
            }
            if (info.type === 'phase') {
              lastProgressAt = Date.now()
              if (info.phase === 'tool_running') {
                updateRunStatus(target, runStatusText('home.runStatusTool', [info.tool || '', info.round || '']))
              } else if (info.phase === 'round') {
                updateRunStatus(target, runStatusText('home.runStatusRound', [info.round || '']))
              } else if (info.phase === 'retry') {
                updateRunStatus(target, runStatusText('home.runStatusRetry'))
              } else if (info.phase === 'tool_done') {
                updateRunStatus(target, '')
              }
              continue
            }
          }
          const delta = json.choices && json.choices[0] && json.choices[0].delta
          const reasoning = delta && typeof delta.reasoning_content === 'string' ? delta.reasoning_content : ''
          if (reasoning) {
            lastProgressAt = Date.now()
            reasoningText += reasoning
            ensureReasoningUi()
            if (reasoningNode) reasoningNode.querySelector('.chat-reasoning-text').textContent = reasoningText
          }
          if (delta && typeof delta.content === 'string') {
            lastProgressAt = Date.now()
            if (reasoningText) finishReasoningUi()
            text += delta.content
            if (writer) writer.push(delta.content)
          }
          const calls = delta ? normalizeToolCalls(delta.tool_calls) : []
          if (calls.length) lastProgressAt = Date.now()
          calls.forEach(call => {
            const merged = toolAccumulator.push(call)
            const key = merged._key
            if (!toolSeqByKey.has(key)) toolSeqByKey.set(key, toolSeq++)
            appendToolBubble(merged, target, toolSeqByKey.get(key))
          })
        }
      }
    }
  } finally {
    streamFinished = true
    window.clearInterval(watchdog)
    updateRunStatus(target, '')
  }
  if (reasoningText && !reasoningDone) finishReasoningUi()
  if (writer) writer.finish(text)
  toolCalls.push.apply(toolCalls, toolAccumulator.values())
  if (!text && target && !toolCalls.length) {
    renderChatMarkdown(chatTextTarget(target), t('home.emptyReply'))
  } else if (!text && target && toolCalls.length) {
    // Tools ran but the model produced no final text; show a completion note
    // instead of looking like the session died silently.
    renderChatMarkdown(chatTextTarget(target), t('home.toolRunDone'))
  }
  return { text, toolCalls }
}

function normalizeToolCalls(raw) {
  if (!Array.isArray(raw)) return []
  return raw.map((call, index) => {
    const fn = call && call.function ? call.function : {}
    return {
      id: (call && call.id) || String((call && call.index) !== undefined ? call.index : index),
      index: (call && call.index) !== undefined ? call.index : index,
      type: call && call.type,
      name: fn.name || (call && call.name) || 'tool',
      arguments: fn.arguments || (call && call.arguments) || ''
    }
  })
}

function createToolCallAccumulator() {
  const calls = []
  return {
    push(part) {
      const partIndex = (part && part.index !== undefined && part.index !== null) ? part.index : calls.length
      const key = (part && part.id) || String(partIndex)
      let current = calls.find(call => call._key === key)
      if (!current) {
        current = { _key: key, id: part && part.id, index: part && part.index, type: part && part.type, name: '', arguments: '' }
        calls.push(current)
      }
      if (part && part.id) current.id = part.id
      if (part && part.type) current.type = part.type
      if (part && part.name) current.name = part.name
      if (part && part.arguments) current.arguments += part.arguments
      return current
    },
    values() {
      return calls.map(call => ({
        id: call.id || call._key,
        index: call.index,
        type: call.type,
        name: call.name || 'tool',
        arguments: call.arguments || ''
      }))
    }
  }
}

function collectToolCallsFromMessage(result) {
  const choice = result && result.choices && result.choices[0]
  const message = choice && choice.message
  return normalizeToolCalls(message && message.tool_calls)
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
    const state = loadRuntimeSession(target)
    const payload = state && state.session ? state.session : state
    const messages = payload && Array.isArray(payload.messages) ? payload.messages : []
    updateRuntimeServiceState(markHomeSessionActive(state || {}, target))
    renderHomeConversation(target, messages)
    if (typeof updateHomeChatContext === 'function') updateHomeChatContext()
  } catch (error) {
    clearHomeMessages()
    if (!silent) appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
  }
}

// ---- Clock tick ----
function tick() {
  clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

// ---- Runtime polling ----
let runtimePollTimer = null
let runtimePollSignature = ''

function runtimeStateSignature(state) {
  const current = state || {}
  const vision = current.vision || {}
  const detectionCount = Array.isArray(vision.lastDetection && vision.lastDetection.detections)
    ? vision.lastDetection.detections.length
    : 0
  const device = current.device || {}
  const sessions = Array.isArray(current.sessions) ? current.sessions : []
  const requests = Array.isArray(current.recentRequests) ? current.recentRequests : []
  const sessionSignature = sessions.map(item => [item.id, item.updatedAt, item.messageCount].join(':')).join(',')
  const requestSignature = requests.map(item => [item.method, item.endpoint, item.status, item.elapsedMs].join(':')).join(',')
  return [
    current.running, current.starting, current.message, current.port, current.lanUrl, current.url,
    current.modelId, current.modelLoaded, current.maxOutputTokens, current.cpuThreads,
    current.contextProfile, current.historyLimit, current.toolExposure, current.authToken,
    current.autoStart, current.currentSessionId, current.requestCount,
    current.cameraPermissionGranted, current.notificationPermissionGranted,
    current.windowAllowed, current.windowVisible, current.windowState, current.windowAutoShow,
    current.batteryOptimizationIgnored, current.accessibilityPermissionGranted,
    vision.state, vision.running, vision.message, vision.fps, detectionCount,
    device.interactive, device.keyguardLocked, device.activityForeground,
    sessionSignature, requestSignature,
  ].join('\x1f')
}

function refreshRuntimeServiceState() {
  const state = runtimeState()
  if (homeChatInFlight && state && Array.isArray(state.sessions)) {
    // While a chat is streaming, the in-flight turn is not persisted yet;
    // keep the live session previews so the current session stays in the list.
    delete state.sessions
  }
  const signature = runtimeStateSignature(state)
  if (signature !== runtimePollSignature) {
    runtimePollSignature = signature
    updateRuntimeServiceState(state)
  }
  if (!runtimePollTimer) {
    runtimePollTimer = window.setInterval(() => {
      const next = runtimeState()
      if (homeChatInFlight && next && Array.isArray(next.sessions)) {
        // Do not let the two-second runtime poll replace the live preview while
        // the server is still processing the current turn.
        delete next.sessions
      }
      const nextSignature = runtimeStateSignature(next)
      if (nextSignature !== runtimePollSignature) {
        runtimePollSignature = nextSignature
        updateRuntimeServiceState(next)
      }
    }, 2000)
  }
}

// ---- Runtime message handler ----
function handleRuntimeMessage(message) {
  if (!message) return
  updateRuntimeServiceState(message)
  runtimePollSignature = runtimeStateSignature(runtimeServiceState)
}


/* === 09-init.js === */
/* ── Lociant WebUI — Event binding and initialization ── */

window.addEventListener('resize', syncKeyboardOffset)
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', syncKeyboardOffset)
  window.visualViewport.addEventListener('scroll', syncKeyboardOffset)
}
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
    const next = createRuntimeSession()
    Promise.resolve(next).then(state => {
      clearHomeMessages()
      const sessionId = homeSessionIdFromState(state) || homeCurrentSessionId()
      updateRuntimeServiceState(markHomeSessionActive(state || {}, sessionId))
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
// ---- Settings navigation ----
runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
runtimeSettingsBack.addEventListener('click', closeRuntimeSettingsBack)
runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
runtimeServerBack.addEventListener('click', backToRuntimeSettings)
runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
runtimeModelBack.addEventListener('click', backToRuntimeSettings)
runtimeAdvancedButton.addEventListener('click', openRuntimeAdvancedSettings)
runtimeAdvancedBack.addEventListener('click', backToRuntimeSettings)
if (runtimeAboutButton) runtimeAboutButton.addEventListener('click', openAboutSettings)
aboutBack.addEventListener('click', closeAboutSettings)

document.querySelectorAll('[data-about-link]').forEach(link => {
  link.addEventListener('click', event => {
    event.preventDefault()
    native('openExternalUrl', link.href)
  })
})

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
  if (running) stopRuntime()
  else startRuntime({})
})
runtimeAutoStartInput.addEventListener('change', () => {
  updateRuntimeSettings({ autoStart: !!runtimeAutoStartInput.checked })
})
runtimeVisionButton.addEventListener('click', () => {
  const vision = visionState()
  if (vision && vision.running) stopRuntimeVision()
  else startRuntimeVision({})
})
runtimeWindowAutoInput.addEventListener('change', () => {
  updateRuntimeWindow({ autoShow: !!runtimeWindowAutoInput.checked })
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
if (accessibilityPermissionButton) {
  accessibilityPermissionButton.addEventListener('click', () => {
    handlePermissionAction(accessibilityPermissionButton, 'requestAccessibilityPermission', 'accessibility')
  })
}
// ---- Server settings ----
runtimePortInput.addEventListener('change', () => {
  const value = Math.max(1024, Math.min(65535, Math.round(Number(runtimePortInput.value) || 11434)))
  runtimePortInput.value = String(value)
  updateRuntimeSettings({ port: value })
})
runtimeMaxTokensInput.addEventListener('change', () => {
  const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
  const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeMaxTokensInput.value) || 512)))
  runtimeMaxTokensInput.value = String(value)
  updateRuntimeSettings({ maxOutputTokens: value })
})
runtimeAuthTokenInput.addEventListener('change', () => {
  updateRuntimeSettings({ authToken: runtimeAuthTokenInput.value.trim() })
})
runtimeAuthGenerateButton.addEventListener('click', () => {
  updateRuntimeSettings({ generateAuthToken: true })
})
runtimeAuthClearButton.addEventListener('click', () => {
  runtimeAuthTokenInput.value = ''
  updateRuntimeSettings({ authToken: '' })
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
  updateRuntimeSettings({ toolExposure: runtimeToolExposureInput.value || 'action' })
})
if (runtimePerformanceModeInput) {
  runtimePerformanceModeInput.addEventListener('change', () => {
    updateRuntimeSettings({ cpuThreads: threadsForPerformanceMode(runtimePerformanceModeInput.value) })
  })
}
if (runtimeBackendInput) {
  runtimeBackendInput.addEventListener('change', () => {
    updateRuntimeSettings({ inferenceBackend: runtimeBackendInput.value || 'model' })
  })
}
if (runtimeCloudEnabledInput) {
  runtimeCloudEnabledInput.addEventListener('change', () => {
    updateRuntimeSettings({ cloudEnabled: !!runtimeCloudEnabledInput.checked })
  })
}
function cloudSettingsPayload() {
  return {
    cloudBaseUrl: runtimeCloudBaseUrlInput ? runtimeCloudBaseUrlInput.value.trim() : '',
    cloudApiKey: runtimeCloudApiKeyInput ? runtimeCloudApiKeyInput.value.trim() : '',
    cloudModel: runtimeCloudModelInput ? runtimeCloudModelInput.value.trim() : ''
  }
}
if (runtimeCloudBaseUrlInput) {
  runtimeCloudBaseUrlInput.addEventListener('change', () => updateRuntimeSettings(cloudSettingsPayload()))
}
if (runtimeCloudApiKeyInput) {
  runtimeCloudApiKeyInput.addEventListener('change', () => updateRuntimeSettings(cloudSettingsPayload()))
}
if (runtimeCloudModelInput) {
  runtimeCloudModelInput.addEventListener('change', () => updateRuntimeSettings(cloudSettingsPayload()))
}
if (runtimeCloudResponseLengthInput) {
  runtimeCloudResponseLengthInput.addEventListener('change', () => {
    if (runtimeCloudResponseLengthInput.value === 'custom') {
      if (runtimeCloudResponseTokensInput) {
        runtimeCloudResponseTokensInput.classList.remove('is-hidden')
        runtimeCloudResponseTokensInput.focus()
      }
      return
    }
    if (runtimeCloudResponseTokensInput) runtimeCloudResponseTokensInput.classList.add('is-hidden')
    updateRuntimeSettings({ cloudMaxOutputTokens: Number(runtimeCloudResponseLengthInput.value) || 0 })
  })
}
if (runtimeCloudResponseTokensInput) {
  runtimeCloudResponseTokensInput.addEventListener('change', () => {
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(0, Math.min(hardMax, Math.round(Number(runtimeCloudResponseTokensInput.value) || 0)))
    runtimeCloudResponseTokensInput.value = String(value)
    updateRuntimeSettings({ cloudMaxOutputTokens: value })
  })
}
if (runtimeCloudContextWindowInput) {
  runtimeCloudContextWindowInput.addEventListener('change', () => {
    if (runtimeCloudContextWindowInput.value === 'custom') {
      if (runtimeCloudContextWindowTokensInput) {
        runtimeCloudContextWindowTokensInput.classList.remove('is-hidden')
        runtimeCloudContextWindowTokensInput.focus()
      }
      return
    }
    if (runtimeCloudContextWindowTokensInput) runtimeCloudContextWindowTokensInput.classList.add('is-hidden')
    updateRuntimeSettings({ cloudContextWindow: Number(runtimeCloudContextWindowInput.value) || 131072 })
  })
}
if (runtimeCloudContextWindowTokensInput) {
  runtimeCloudContextWindowTokensInput.addEventListener('change', () => {
    const value = Math.max(16384, Math.min(524288, Math.round(Number(runtimeCloudContextWindowTokensInput.value) || 131072)))
    runtimeCloudContextWindowTokensInput.value = String(value)
    updateRuntimeSettings({ cloudContextWindow: value })
  })
}
if (runtimeCloudHistoryLimitInput) {
  runtimeCloudHistoryLimitInput.addEventListener('change', () => {
    const value = Math.max(1, Math.min(1024, Math.round(Number(runtimeCloudHistoryLimitInput.value) || 256)))
    runtimeCloudHistoryLimitInput.value = String(value)
    updateRuntimeSettings({ cloudHistoryLimit: value })
  })
}
if (runtimeResponseLengthInput) {
  runtimeResponseLengthInput.addEventListener('change', () => {
    if (runtimeResponseLengthInput.value === 'custom') {
      if (runtimeResponseTokensInput) {
        runtimeResponseTokensInput.classList.remove('is-hidden')
        runtimeResponseTokensInput.focus()
      }
      return
    }
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(1, Math.min(hardMax, Number(runtimeResponseLengthInput.value) || 512))
    updateRuntimeSettings({ maxOutputTokens: value })
  })
}
if (runtimeResponseTokensInput) {
  runtimeResponseTokensInput.addEventListener('change', () => {
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeResponseTokensInput.value) || 512)))
    runtimeResponseTokensInput.value = String(value)
    updateRuntimeSettings({ maxOutputTokens: value })
  })
}
if (runtimeContextMemoryInput) {
  runtimeContextMemoryInput.addEventListener('change', () => {
    if (runtimeContextMemoryInput.value === 'custom') {
      if (runtimeHistoryLimitInput) {
        runtimeHistoryLimitInput.classList.remove('is-hidden')
        runtimeHistoryLimitInput.focus()
      }
      return
    }
    const profile = runtimeContextMemoryInput.value || 'balanced'
    updateRuntimeSettings({ contextProfile: profile, historyLimit: historyLimitForContextProfile(profile) })
  })
}
if (runtimeHistoryLimitInput) {
  runtimeHistoryLimitInput.addEventListener('change', () => {
    const maxHistory = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.maxHistoryLimit) || 256
    const value = Math.max(1, Math.min(maxHistory, Math.round(Number(runtimeHistoryLimitInput.value) || 64)))
    runtimeHistoryLimitInput.value = String(value)
    updateRuntimeSettings({ contextProfile: contextPresetForHistoryLimit(value), historyLimit: value })
  })
}
if (runtimeReleaseModelButton) {
  runtimeReleaseModelButton.addEventListener('click', () => {
    releaseRuntimeModel()
    showToast(t('toast.modelReleased'))
  })
}

// ---- Session ----
runtimeSessionNewButton.addEventListener('click', () => {
  if (homeChatInFlight) return
  createRuntimeSession()
})
if (runtimeDiagRunButton) {
  runtimeDiagRunButton.addEventListener('click', runRuntimeDiagnostics)
}
// ---- Language ----
languageControl.addEventListener('click', event => {
  const button = event.target.closest('.segmented-option')
  if (button && button.dataset.langMode) saveLocaleSetting(button.dataset.langMode)
})

// ---- Model actions ----
modelReloadButton.addEventListener('click', () => {
  loadModels(true)
  showToast(t('toast.modelsReloaded'))
})
modelImportButton.addEventListener('click', () => {
  native('installModelPackage')
})
modelLocalButton.addEventListener('click', () => setModelView('local'))
modelLocalBack.addEventListener('click', () => setModelView('home'))
modelMarketButton.addEventListener('click', () => setModelView('market'))
modelMarketBack.addEventListener('click', () => setModelView('home'))
modelCloudButton.addEventListener('click', () => setModelView('cloud'))
modelCloudBack.addEventListener('click', () => setModelView('home'))
modelRuntimeButton.addEventListener('click', () => {
  navigateTo('settings')
  openRuntimeSettings()
})
modelMarketRefreshButton.addEventListener('click', () => loadModelMarket(true))
if (modelMarketStartButton) {
  modelMarketStartButton.addEventListener('click', startRuntimeForMarket)
}
modelMarketSearch.addEventListener('input', () => {
  window.clearTimeout(marketSearchTimer)
  marketSearchTimer = window.setTimeout(() => {
    marketQuery = modelMarketSearch.value.trim()
    loadModelMarket()
  }, 250)
})
// ---- PostMessage handlers ----
window.LociantEvents = {
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

// ---- Bootstrap ----
loadLocaleSetting()
refreshRuntimeServiceState()
restoreHomeConversation()
loadModels()
tick()
window.setInterval(tick, 1000)


/* === 10-onboarding.js === */
/* Lociant WebUI - first-run setup guide */

const ONBOARDING_STORAGE_KEY = 'lociant.onboarding.1.1.0'
const ONBOARDING_STEP_TOTAL = 7
const DEEPSEEK_BASE_URL = 'https://api.deepseek.com/v1'
const DEEPSEEK_MODEL = 'deepseek-chat'
let onboardingStep = 0
let onboardingPollTimer = null
let onboardingTransitionTimer = null

function onboardingFormat(key, ...parts) {
  const values = parts.slice()
  return t(key).replace(/%s/g, () => (values.length ? String(values.shift()) : ''))
}

function onboardingHasSeen() {
  try {
    return window.localStorage.getItem(ONBOARDING_STORAGE_KEY) === 'done'
  } catch (error) {
    return false
  }
}

function markOnboardingSeen() {
  try {
    window.localStorage.setItem(ONBOARDING_STORAGE_KEY, 'done')
  } catch (error) {}
}

function onboardingCloudReady() {
  const state = runtimeServiceState || {}
  return !!(state.cloudEnabled && String(state.cloudBaseUrl || '').trim() &&
    String(state.cloudApiKey || '').trim() && String(state.cloudModel || '').trim())
}

function onboardingRuntimeRunning() {
  return !!(runtimeServiceState && runtimeServiceState.running)
}

function setOnboardingStep(step) {
  const previousStep = onboardingStep
  const nextStep = Math.max(0, Math.min(ONBOARDING_STEP_TOTAL - 1, Number(step) || 0))
  onboardingStep = nextStep
  if (onboardingDialog) {
    onboardingDialog.classList.remove('is-forward', 'is-backward')
    onboardingDialog.classList.add(nextStep < previousStep ? 'is-backward' : 'is-forward')
    window.clearTimeout(onboardingTransitionTimer)
    onboardingTransitionTimer = window.setTimeout(() => {
      onboardingDialog.classList.remove('is-forward', 'is-backward')
    }, 340)
  }
  onboardingSteps.forEach((node, index) => {
    const active = index === onboardingStep
    node.classList.toggle('active', active)
    node.setAttribute('aria-hidden', active ? 'false' : 'true')
  })
  onboardingProgress.forEach((node, index) => {
    node.classList.toggle('active', index <= onboardingStep)
  })
  if (onboardingStepCount) {
    onboardingStepCount.textContent = onboardingFormat('onboarding.stepCount', onboardingStep + 1, ONBOARDING_STEP_TOTAL)
  }
  if (onboardingBackButton) onboardingBackButton.classList.toggle('is-hidden', onboardingStep === 0)
  updateOnboardingState()
}

function updateOnboardingState() {
  if (!onboardingModal || onboardingModal.hidden) return
  const state = runtimeServiceState || {}
  const cloudReady = onboardingCloudReady()
  const running = onboardingRuntimeRunning()
  const starting = !!state.starting
  const accessibilityGranted = state.accessibilityPermissionGranted === true
  const overlayGranted = state.windowAllowed === true

  if (onboardingApiKeyInput && document.activeElement !== onboardingApiKeyInput) {
    onboardingApiKeyInput.value = String(state.cloudApiKey || '')
  }
  if (onboardingCloudStatus) {
    onboardingCloudStatus.textContent = cloudReady
      ? t('onboarding.cloudConfigured')
      : (state.cloudBaseUrl && state.cloudModel ? t('onboarding.cloudPresetApplied') : '')
    onboardingCloudStatus.classList.toggle('is-ready', cloudReady)
  }
  if (onboardingRuntimeStatus) {
    onboardingRuntimeStatus.textContent = running
      ? t('onboarding.runtimeRunning')
      : (starting ? t('onboarding.runtimeStarting') : t('onboarding.runtimeStopped'))
  }
  if (onboardingRuntimeDot) onboardingRuntimeDot.classList.toggle('is-ready', running)
  if (onboardingAccessibilityStatus) {
    onboardingAccessibilityStatus.textContent = accessibilityGranted ? t('onboarding.granted') : t('onboarding.needsGrant')
    onboardingAccessibilityStatus.classList.toggle('is-ready', accessibilityGranted)
  }
  if (onboardingOverlayStatus) {
    onboardingOverlayStatus.textContent = overlayGranted ? t('onboarding.granted') : t('onboarding.optional')
    onboardingOverlayStatus.classList.toggle('is-ready', overlayGranted)
  }
  if (onboardingMcpUrl) {
    onboardingMcpUrl.textContent = state.port ? mcpEndpointUrl() : '--'
  }
  updateOnboardingPrimaryButton(cloudReady, running, starting, accessibilityGranted)
}

function updateOnboardingPrimaryButton(cloudReady, running, starting, accessibilityGranted) {
  if (!onboardingPrimaryButton) return
  const label = onboardingStep === 0
    ? t('onboarding.start')
    : onboardingStep === 1
      ? (cloudReady ? t('onboarding.next') : t('common.save'))
      : onboardingStep === 2
        ? (running ? t('onboarding.next') : (starting ? t('onboarding.waitRuntime') : t('onboarding.startRuntime')))
        : onboardingStep === 3
          ? (accessibilityGranted ? t('onboarding.next') : t('onboarding.grantAccessibility'))
          : onboardingStep === 6 ? t('onboarding.finish') : t('onboarding.next')
  onboardingPrimaryButton.textContent = label
}

function openOnboarding() {
  if (!onboardingModal) return
  onboardingStep = 0
  onboardingModal.hidden = false
  onboardingModal.setAttribute('aria-hidden', 'false')
  app.classList.add('onboarding-open')
  setOnboardingStep(0)
  window.clearInterval(onboardingPollTimer)
  onboardingPollTimer = window.setInterval(() => {
    refreshRuntimeServiceState()
    updateOnboardingState()
  }, 1000)
  window.setTimeout(() => {
    if (onboardingStep === 0 && onboardingPrimaryButton) onboardingPrimaryButton.focus()
  }, 80)
}

function closeOnboarding(markSeen) {
  if (!onboardingModal) return
  if (markSeen) markOnboardingSeen()
  window.clearInterval(onboardingPollTimer)
  onboardingPollTimer = null
  onboardingModal.hidden = true
  onboardingModal.setAttribute('aria-hidden', 'true')
  app.classList.remove('onboarding-open')
}

function finishOnboarding() {
  markOnboardingSeen()
  closeOnboarding(false)
  navigateTo('home')
}

function saveOnboardingCloudSettings() {
  const key = onboardingApiKeyInput ? onboardingApiKeyInput.value.trim() : ''
  const baseUrl = (runtimeCloudBaseUrlInput && runtimeCloudBaseUrlInput.value.trim()) ||
    String(runtimeServiceState && runtimeServiceState.cloudBaseUrl || '').trim() || DEEPSEEK_BASE_URL
  const model = (runtimeCloudModelInput && runtimeCloudModelInput.value.trim()) ||
    String(runtimeServiceState && runtimeServiceState.cloudModel || '').trim() || DEEPSEEK_MODEL
  if (!key) {
    if (onboardingCloudStatus) onboardingCloudStatus.textContent = t('onboarding.cloudRequired')
    if (onboardingApiKeyInput) onboardingApiKeyInput.focus()
    return false
  }
  updateRuntimeSettings({
    cloudEnabled: true,
    cloudBaseUrl: baseUrl,
    cloudApiKey: key,
    cloudModel: model,
  })
  return onboardingCloudReady()
}

function useDeepSeekPreset() {
  if (runtimeCloudBaseUrlInput) runtimeCloudBaseUrlInput.value = DEEPSEEK_BASE_URL
  if (runtimeCloudModelInput) runtimeCloudModelInput.value = DEEPSEEK_MODEL
  updateRuntimeSettings({ cloudBaseUrl: DEEPSEEK_BASE_URL, cloudModel: DEEPSEEK_MODEL })
  if (onboardingCloudStatus) onboardingCloudStatus.textContent = t('onboarding.cloudPresetApplied')
  if (onboardingApiKeyInput) onboardingApiKeyInput.focus()
}

function runOnboardingPrimaryAction() {
  if (onboardingStep === 0) {
    setOnboardingStep(1)
    return
  }
  if (onboardingStep === 1) {
    if (saveOnboardingCloudSettings()) setOnboardingStep(2)
    return
  }
  if (onboardingStep === 2) {
    if (onboardingRuntimeRunning()) {
      setOnboardingStep(3)
    } else if (!(runtimeServiceState && runtimeServiceState.starting)) {
      startRuntime({})
    }
    updateOnboardingState()
    return
  }
  if (onboardingStep === 3) {
    if (runtimeServiceState && runtimeServiceState.accessibilityPermissionGranted === true) {
      setOnboardingStep(4)
    } else {
      native('requestAccessibilityPermission')
    }
    updateOnboardingState()
    return
  }
  if (onboardingStep === 4) {
    setOnboardingStep(5)
    return
  }
  if (onboardingStep === 5) {
    setOnboardingStep(6)
    return
  }
  finishOnboarding()
}

function leaveOnboardingToModels(view) {
  closeOnboarding(false)
  navigateTo('models')
  setModelView(view)
}

if (onboardingSettingsButton) onboardingSettingsButton.addEventListener('click', openOnboarding)
if (onboardingCloseButton) onboardingCloseButton.addEventListener('click', () => closeOnboarding(true))
if (onboardingSkipButton) onboardingSkipButton.addEventListener('click', () => closeOnboarding(true))
if (onboardingBackButton) onboardingBackButton.addEventListener('click', () => setOnboardingStep(onboardingStep - 1))
if (onboardingPrimaryButton) onboardingPrimaryButton.addEventListener('click', runOnboardingPrimaryAction)
if (onboardingDeepSeekButton) onboardingDeepSeekButton.addEventListener('click', useDeepSeekPreset)
if (onboardingApiKeyInput) onboardingApiKeyInput.addEventListener('input', updateOnboardingState)
if (onboardingCloudSkipButton) onboardingCloudSkipButton.addEventListener('click', () => setOnboardingStep(2))
if (onboardingMcpCopyButton) onboardingMcpCopyButton.addEventListener('click', () => copyConnectionText(mcpConfigText))
if (onboardingAccessibilityButton) {
  onboardingAccessibilityButton.addEventListener('click', () => {
    if (runtimeServiceState && runtimeServiceState.accessibilityPermissionGranted === true) {
      native('openPermissionSettings', 'accessibility')
    } else {
      native('requestAccessibilityPermission')
    }
  })
}
if (onboardingAppPermissionButton) {
  onboardingAppPermissionButton.addEventListener('click', () => native('openPermissionSettings', 'app'))
}
if (onboardingLocalButton) onboardingLocalButton.addEventListener('click', () => leaveOnboardingToModels('local'))
if (onboardingMarketButton) onboardingMarketButton.addEventListener('click', () => leaveOnboardingToModels('market'))

document.addEventListener('visibilitychange', () => {
  if (!document.hidden && onboardingModal && !onboardingModal.hidden) {
    refreshRuntimeServiceState()
    updateOnboardingState()
  }
})

window.setTimeout(() => {
  if (!onboardingHasSeen()) openOnboarding()
}, 180)
