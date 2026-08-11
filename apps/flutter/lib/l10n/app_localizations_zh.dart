// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appTitle => 'Lociant';

  @override
  String get navMenu => '菜单';

  @override
  String get navHome => '主页';

  @override
  String get navModels => '模型';

  @override
  String get navNodes => '节点';

  @override
  String get navSettings => '设置';

  @override
  String get nodesTitle => '节点';

  @override
  String get nodesSubtitle => '局域网内的 Lociant 设备';

  @override
  String get nodesRefresh => '刷新';

  @override
  String get nodesSelf => '本机';

  @override
  String get nodesOnline => '在线';

  @override
  String get nodesOffline => '离线';

  @override
  String get nodesEmpty => '还没有发现其他节点。在其他设备上配置相同的节点令牌后，它们会自动出现在这里。';

  @override
  String get nodesPeersHint => '其他节点的模型会自动以 peer: 前缀出现在模型页，可以直接选用。';

  @override
  String get nodesGuideTitle => '让设备们互联';

  @override
  String get nodesGuideBody => '手机、电脑、开发板配置相同的节点令牌，在同一局域网内会自动互相发现，互借模型和工具。';

  @override
  String get nodesGuideStep1 => '在每台设备的“设置 → 服务器”里填同一个节点令牌';

  @override
  String get nodesGuideStep2 => '保持同一局域网，稍等片刻自动出现；也可以点右上角 + 手动添加';

  @override
  String get nodesGuideStep3 => '点开节点卡片：互借模型（peer: 前缀）、查看婴儿监控、直接对话';

  @override
  String get nodesGuideOpenSettings => '去设置';

  @override
  String get nodesHelp => '节点互联说明';

  @override
  String nodesError(Object error) {
    return '加载节点失败：$error';
  }

  @override
  String get babyTitle => '婴儿监控';

  @override
  String get babySubtitle => '眠安智护：状态与事件';

  @override
  String get babyNotEnabled => '当前节点未启用婴儿监控\n（在配置中设置 babyCamera 后启用）';

  @override
  String get babyState => '状态';

  @override
  String get babyMotion => '运动强度';

  @override
  String get babyEvents => '最近事件';

  @override
  String get babyNoEvents => '暂无事件';

  @override
  String get babyStateIdle => '待机';

  @override
  String get babyStateCandidate => '观察中';

  @override
  String get babyStateSoothing1 => '一级安抚';

  @override
  String get babyStateSoothing2 => '二级安抚';

  @override
  String get babyStateNotify => '提醒家长';

  @override
  String get babyStateCooldown => '冷却中';

  @override
  String get nodesAdd => '添加节点';

  @override
  String get nodesAddFailed => '添加节点失败';

  @override
  String get nodesDelete => '删除节点';

  @override
  String get nodesDeleteFailed => '删除节点失败';

  @override
  String get nodesChat => '对话';

  @override
  String get nodesChatSelectModel => '选择要使用的模型';

  @override
  String get nodesChatNoModel => '该节点暂无可用模型';

  @override
  String get commonCancel => '取消';

  @override
  String get settingsPeerToken => '节点令牌（局域网互联）';

  @override
  String get settingsPeerTokenSave => '保存';

  @override
  String get settingsPeerTokenHint =>
      '与其他 Lociant 节点共享此令牌后，它们会自动出现在节点页；修改后需重启服务生效。';

  @override
  String get settingsModelServerDesktop => '桌面端服务随应用自动运行（内置 Rust 后端）';

  @override
  String get statusIdle => '待机';

  @override
  String get statusRunning => '运行中';

  @override
  String get statusStarting => '启动中';

  @override
  String get statusStopped => '已停止';

  @override
  String get homePlaceholder => '问问 Lociant，或输入一个工具调用任务';

  @override
  String get homeSend => '发送';

  @override
  String get homeNewChat => '新建对话';

  @override
  String get homeHistory => '最近对话';

  @override
  String get homeEmptyReply => '没有回复';

  @override
  String get homeThinking => '思考中…';

  @override
  String get homeThought => '已思考';

  @override
  String homeRunStatusTool(Object round, Object tool) {
    return '正在运行工具 $tool（第 $round 轮）…';
  }

  @override
  String homeRunStatusRound(Object round) {
    return '正在调用模型（第 $round 轮）…';
  }

  @override
  String get homeRunStatusRetry => '正在重试…';

  @override
  String homeRoundLabel(Object n) {
    return '第 $n 轮';
  }

  @override
  String get homeToolRunDone => '工具已执行完成，没有文本回复。';

  @override
  String get homeImageAttached => '已附加图片';

  @override
  String get homeRemoveImage => '移除图片';

  @override
  String get homeUploadImage => '上传照片';

  @override
  String get homeDeleteChat => '删除对话';

  @override
  String get onboardingSkip => '跳过';

  @override
  String get onboardingNext => '下一步';

  @override
  String get onboardingStart => '开始使用';

  @override
  String get onboardingWelcomeTitle => '欢迎使用 Lociant';

  @override
  String get onboardingWelcomeBody =>
      '让一台设备成为真正能干活的本地 Agent：本地运行模型、读取屏幕、操作界面、感知环境，也能通过 MCP 被 Claude、Codex 等 Agent 调用。';

  @override
  String get onboardingCloudTitle => '先配一个云端模型';

  @override
  String get onboardingCloudBody =>
      '在“设置 → 云端模型”填入服务地址、API Key 和模型名（例如 DeepSeek），马上就能对话。想断网使用，再到“模型”页安装本地模型。';

  @override
  String get onboardingPermissionTitle => '给 Agent 一双手';

  @override
  String get onboardingPermissionBody =>
      '在“设置”里开启权限：无障碍让它可以看屏幕、点击和滑动；通知保持后台运行；相机用于拍照和视觉分析；电池设为“不限制”，避免锁屏后服务被暂停。';

  @override
  String get onboardingNodesTitle => '把设备们连起来';

  @override
  String get onboardingNodesBody =>
      '手机、电脑、开发板在同一局域网并配置相同的节点令牌后，会自动互相发现。在“节点”页查看设备、互借模型和工具——比如手机直接用板子上的 RKLLM 模型。';

  @override
  String get onboardingReadyTitle => '准备就绪';

  @override
  String get onboardingReadyBody =>
      '回到首页启动运行时，先发一条消息确认模型正常，再让它干活。之后还可以用 Claude、OpenCode 等支持 MCP 的客户端，直接调用这台设备的能力。';

  @override
  String get settingsOnboardingTitle => '新手引导';

  @override
  String get settingsOnboardingSub => '重新查看使用步骤与节点互联说明';

  @override
  String get commonBack => '返回';

  @override
  String get commonRefresh => '刷新';

  @override
  String get commonStart => '启动';

  @override
  String get commonStop => '停止';

  @override
  String get commonInstall => '安装';

  @override
  String get commonSave => '保存';

  @override
  String get commonOpen => '打开';

  @override
  String get commonCopy => '复制';

  @override
  String get modelsTitle => '模型';

  @override
  String get modelsSubtitle => '安装、选择和管理本地推理';

  @override
  String get modelsLocalTitle => '本地模型';

  @override
  String get modelsLocalSub => '已安装模型包';

  @override
  String get modelsMarketTitle => '模型市场';

  @override
  String get modelsMarketSub => 'ModelScope MNN 模型';

  @override
  String get modelsRuntimeTitle => '运行时';

  @override
  String get modelsRuntimeSub => '默认模型与 API';

  @override
  String get modelsCloudTitle => '云端模型';

  @override
  String get modelsCloudSub => 'OpenAI 兼容云端 API';

  @override
  String get modelsImport => '导入';

  @override
  String get modelsRescan => '扫描';

  @override
  String get modelsInstalled => '已安装';

  @override
  String get modelsInstalling => '安装中';

  @override
  String get modelsInstall => '安装';

  @override
  String get modelsDelete => '删除';

  @override
  String get emptyModels => '还没有模型';

  @override
  String get settingsTitle => '设置';

  @override
  String get settingsSubtitle => '集中管理运行时、权限和模型行为';

  @override
  String get settingsLanguage => '语言';

  @override
  String get settingsLanguageSub => '显示语言';

  @override
  String get settingsTheme => '主题';

  @override
  String get settingsThemeSub => '界面配色风格';

  @override
  String get settingsThemeDark => '深色';

  @override
  String get settingsThemePink => '浅粉';

  @override
  String get settingsFollowSystem => '系统';

  @override
  String get settingsRuntimeTitle => '运行时';

  @override
  String get settingsRuntimeSub => '后台服务与悬浮窗';

  @override
  String get settingsServerTitle => '服务';

  @override
  String get settingsServerSub => '端口、令牌、地址';

  @override
  String get settingsModelTitle => '默认模型';

  @override
  String get settingsModelSub => '模型与 CPU 线程';

  @override
  String get settingsAgentTitle => 'Agent';

  @override
  String get settingsAgentSub => '工具调用行为';

  @override
  String get settingsAdvancedTitle => '高级';

  @override
  String get settingsAdvancedSub => '会话与诊断';

  @override
  String get settingsAgentRounds => '最大工具轮数';

  @override
  String settingsAgentRoundsSub(Object max, Object min) {
    return '每个任务里模型与工具交替的轮数（$min–$max）';
  }

  @override
  String get settingsToolCalls => '工具调用上限';

  @override
  String get settingsPermissionsTitle => '权限';

  @override
  String get settingsWindowVision => '悬浮窗与视觉';

  @override
  String get settingsWindowAuto => '自动显示悬浮窗';

  @override
  String get settingsVisionTitle => '视觉';

  @override
  String get settingsToolExposure => '远程工具';

  @override
  String get settingsToolRead => '只读';

  @override
  String get settingsToolSensor => '感知';

  @override
  String get settingsToolAction => '动作';

  @override
  String get settingsGenerate => '生成';

  @override
  String get settingsClear => '清除';

  @override
  String get settingsSessions => '会话';

  @override
  String get settingsPerformanceMode => '性能模式';

  @override
  String get settingsPerformanceEco => '省电';

  @override
  String get settingsPerformanceBalanced => '均衡';

  @override
  String get settingsPerformanceFast => '高性能';

  @override
  String get settingsInferenceBackend => '推理后端';

  @override
  String get settingsBackendModel => '跟随模型';

  @override
  String get settingsBackendAuto => '自动';

  @override
  String get settingsBackendCpu => 'CPU';

  @override
  String get settingsBackendOpencl => 'OpenCL（GPU）';

  @override
  String get settingsBackendVulkan => 'Vulkan（GPU）';

  @override
  String get settingsResponseLength => '回复长度';

  @override
  String get settingsLengthShort => '短';

  @override
  String get settingsLengthNormal => '常规';

  @override
  String get settingsLengthLong => '长';

  @override
  String get settingsContextMemory => '上下文记忆';

  @override
  String get settingsContextLight => '轻量';

  @override
  String get settingsContextBalanced => '均衡';

  @override
  String get settingsContextDeep => '深度';

  @override
  String get settingsReleaseModel => '释放模型内存';

  @override
  String get settingsRelease => '释放';

  @override
  String get settingsCloudTitle => '云端模型';

  @override
  String get settingsCloudBaseUrl => 'API Base URL';

  @override
  String get settingsCloudApiKey => 'API Key';

  @override
  String get settingsCloudModel => '模型名称';

  @override
  String get settingsCloudResponseLengthSub => '最大输出 Tokens（0 = 跟随提供商默认）';

  @override
  String get settingsCloudContextWindow => '上下文窗口';

  @override
  String get settingsCloudHistoryLimit => '历史消息数';

  @override
  String get connectionOpenaiUrl => 'OpenAI Base URL';

  @override
  String get connectionMcpUrl => 'MCP URL';

  @override
  String get connectionAuthHeader => '认证头';

  @override
  String get connectionAuthDisabled => '认证已停用';

  @override
  String connectionAuthHeaderValue(Object token) {
    return 'Authorization: Bearer $token';
  }

  @override
  String get aboutTitle => '关于 Lociant';

  @override
  String aboutVersionLine(Object version) {
    return '版本 $version · Flutter UI';
  }

  @override
  String get settingsPort => '端口';

  @override
  String get settingsOutputTokens => '输出 Tokens';

  @override
  String get settingsApiToken => 'API 令牌';

  @override
  String get settingsAutoStart => '开机启动';

  @override
  String get settingsModelServer => '服务';

  @override
  String get settingsPermissionCamera => '摄像头';

  @override
  String get settingsPermissionNotification => '通知';

  @override
  String get settingsPermissionOverlay => '悬浮窗';

  @override
  String get settingsPermissionBattery => '后台电量';

  @override
  String get settingsPermissionAccessibility => '无障碍';

  @override
  String get settingsDesktopPermissionsHint =>
      '桌面端没有设备权限。要使用屏幕操作、传感器、相机等工具，请连接安卓手机或开发板节点。';

  @override
  String get settingsGrant => '授权';

  @override
  String get toastCopied => '已复制';

  @override
  String get toastCopyFailed => '复制失败';

  @override
  String get toastImagePickerUnavailable => '当前平台不支持选择图片';

  @override
  String get toastModelImportFailed => '模型导入失败';

  @override
  String get toastModelDeleted => '模型已删除';

  @override
  String get toastModelDeleteFailed => '模型删除失败';

  @override
  String get toastModelsReloaded => '模型已刷新';

  @override
  String get toastModelImported => '模型导入成功';

  @override
  String get toastModelMarketLoaded => '市场已加载';

  @override
  String get toastModelMarketFailed => '模型市场不可用';

  @override
  String get errorApiRequest => 'API 请求失败';
}
