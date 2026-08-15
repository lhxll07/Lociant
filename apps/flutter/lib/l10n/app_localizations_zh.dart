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
  String get navHome => '概览';

  @override
  String get navModels => '模型';

  @override
  String get navNodes => '节点';

  @override
  String get navExtensions => '扩展';

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
  String get nodesGridHint => '点击设备卡片查看详情和可用操作';

  @override
  String get nodesAddress => '地址';

  @override
  String get nodesPlatform => '平台';

  @override
  String get nodesNodeId => '节点 ID';

  @override
  String get nodesNameOptional => '名称（可选）';

  @override
  String get nodesAvailableActions => '可用操作';

  @override
  String get nodesOpenModels => '查看模型';

  @override
  String get nodesSelfHint => '这是当前设备，提供本地运行时能力。';

  @override
  String get nodesDevicePhone => '手机';

  @override
  String get nodesDeviceComputer => '电脑';

  @override
  String get nodesDeviceBoard => '开发板';

  @override
  String get nodesDeviceOther => '设备';

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
  String get nodesGuideStep3 => '点开节点卡片：互借模型（peer: 前缀）或查看婴儿监控';

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
  String get extensionsTitle => '扩展';

  @override
  String get extensionsSubtitle => '管理这台设备提供的边缘能力';

  @override
  String get extensionsInstalledTitle => '已安装扩展';

  @override
  String get extensionsBuiltInHint => '当前扩展随 Lociant 内置，并按设备权限提供能力。';

  @override
  String get extensionsBabyDescription => '持续观察婴儿状态，在需要时记录事件并提醒家长。';

  @override
  String get extensionsBabyPermissions => '需要摄像头、通知和后台运行权限';

  @override
  String get extensionsStatusChecking => '检查中';

  @override
  String get extensionsStatusEnabled => '已启用';

  @override
  String get extensionsStatusNotConfigured => '未配置';

  @override
  String get extensionsStatusUnavailable => '暂不可用';

  @override
  String get extensionsUnavailableHint => '无法连接到当前节点，请确认服务已启动。';

  @override
  String get extensionsOpen => '打开';

  @override
  String get extensionsRefresh => '刷新状态';

  @override
  String get extensionsOpenSettings => '管理权限';

  @override
  String get statusIdle => '待机';

  @override
  String get statusRunning => '运行中';

  @override
  String get statusStarting => '启动中';

  @override
  String get statusStopped => '已停止';

  @override
  String get edgeOverviewTitle => '边缘运行时';

  @override
  String get edgeOverviewSubtitle => '这台设备的控制台';

  @override
  String get edgeMetricModels => '模型';

  @override
  String get edgeMetricNodes => '节点';

  @override
  String get edgeMetricTools => '工具';

  @override
  String get edgeMetricStatus => '状态';

  @override
  String get edgeNodesTitle => '设备概览';

  @override
  String get edgeNodesEmpty => '暂时没有可显示的设备。';

  @override
  String get edgeViewAll => '查看全部';

  @override
  String get edgeEndpointsTitle => '控制接口';

  @override
  String get edgeControlApi => '控制 API';

  @override
  String get edgeToolsTitle => '可用工具';

  @override
  String get edgeToolsEmpty => '当前暴露级别下没有可用工具';

  @override
  String get toolDescriptionGeneric => '可由控制端调用的设备能力。';

  @override
  String get toolDescriptionRuntimeStatus => '查看运行时状态。';

  @override
  String get toolDescriptionModelList => '列出可用模型。';

  @override
  String get toolDescriptionDeviceStatus => '查看设备基本信息。';

  @override
  String get toolDescriptionClipboardRead => '读取剪贴板。';

  @override
  String get toolDescriptionClipboardWrite => '写入剪贴板。';

  @override
  String get toolDescriptionAppOpen => '打开应用。';

  @override
  String get toolDescriptionUiScreenState => '读取当前屏幕结构。';

  @override
  String get toolDescriptionUiClickNode => '点击屏幕元素。';

  @override
  String get toolDescriptionUiTap => '点击屏幕位置。';

  @override
  String get toolDescriptionUiSwipe => '滑动屏幕。';

  @override
  String get toolDescriptionUiWait => '等待界面变化。';

  @override
  String get toolDescriptionUiPaste => '把文字粘贴到当前输入框。';

  @override
  String get toolDescriptionUiSetText => '填写文本输入框。';

  @override
  String get toolDescriptionVisionStatus => '查看视觉服务状态。';

  @override
  String get toolDescriptionVisionStart => '启动视觉服务。';

  @override
  String get toolDescriptionCameraCapture => '拍摄一张照片。';

  @override
  String get toolDescriptionVisionStop => '停止视觉服务。';

  @override
  String get toolDescriptionSensorStatus => '查看传感器状态。';

  @override
  String get toolDescriptionSensorRead => '读取传感器数据。';

  @override
  String get toolDescriptionSensorStart => '开始采集传感器数据。';

  @override
  String get toolDescriptionSensorStop => '停止采集传感器数据。';

  @override
  String get toolDescriptionFileList => '列出目录中的文件。';

  @override
  String get toolDescriptionFileRead => '读取文本文件。';

  @override
  String get toolDescriptionFileWrite => '写入文本文件。';

  @override
  String get toolDescriptionProcessList => '查看运行中的进程。';

  @override
  String get toolDescriptionProcessRun => '执行系统命令。';

  @override
  String get edgeOpenModels => '打开模型';

  @override
  String get edgeOpenNodes => '打开节点';

  @override
  String get edgeOpenSettings => '设置';

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
      '让一台设备成为边缘运行时：运行本地模型、暴露设备工具、感知环境，并通过受控 API 与 MCP 接入。';

  @override
  String get onboardingLocalTitle => '准备本地模型';

  @override
  String get onboardingLocalBody => '在“模型”中导入或安装本地模型，让设备在边缘侧完成推理。';

  @override
  String get onboardingPermissionTitle => '启用设备能力';

  @override
  String get onboardingPermissionBody =>
      '在“设置”里开启权限：无障碍提供屏幕和界面工具；通知保持运行时存活；相机用于视觉；电池设为“不限制”以保证后台稳定运行。';

  @override
  String get onboardingNodesTitle => '把设备们连起来';

  @override
  String get onboardingNodesBody =>
      '手机、电脑、开发板在同一局域网并配置相同的节点令牌后，会自动互相发现。在“节点”页查看设备、互借模型和工具——比如手机直接用板子上的 RKLLM 模型。';

  @override
  String get onboardingReadyTitle => '准备就绪';

  @override
  String get onboardingReadyBody => '从概览启动运行时，检查可用工具，并通过控制 API 或 MCP 接入外部客户端。';

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
  String get modelsMarketSub => 'ModelScope GGUF 模型';

  @override
  String get modelsRuntimeTitle => '运行时';

  @override
  String get modelsRuntimeSub => '默认模型与 API';

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
  String get settingsLanguageChinese => '中文';

  @override
  String get settingsLanguageEnglish => '英文';

  @override
  String get settingsAppearanceTitle => '外观';

  @override
  String get settingsSectionsTitle => '设置分类';

  @override
  String get settingsSecurityTitle => '安全';

  @override
  String get settingsSecuritySub => 'API 访问、节点连接和工具权限';

  @override
  String get settingsLocalModelTitle => '本地模型';

  @override
  String get settingsLocalModelSub => '默认模型与本地推理设置';

  @override
  String get settingsAboutSub => '版本与运行时信息';

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
  String get settingsModelSub => '已安装模型与回复长度';

  @override
  String get settingsAdvancedTitle => '高级';

  @override
  String get settingsPermissionsTitle => '权限';

  @override
  String get settingsPermissionsHint => '这些权限决定设备工具能否工作；远程工具范围仍由下方级别控制。';

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
  String get settingsToolExposureHint => '选择远程客户端可以使用的设备能力范围。';

  @override
  String get settingsGenerate => '生成';

  @override
  String get settingsClear => '清除';

  @override
  String get settingsPerformanceMode => '性能模式';

  @override
  String get settingsPerformanceEco => '省电';

  @override
  String get settingsPerformanceBalanced => '均衡';

  @override
  String get settingsPerformanceFast => '高性能';

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
  String get settingsReleaseModelHint => '释放已加载模型占用的内存。';

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
  String get settingsApiTokenHint => '保护控制 API 和远程工具调用。';

  @override
  String get settingsShowToken => '显示令牌';

  @override
  String get settingsHideToken => '隐藏令牌';

  @override
  String get settingsModelStatus => '模型状态';

  @override
  String get settingsModelLoading => '加载中';

  @override
  String get settingsModelReady => '已就绪';

  @override
  String get settingsModelNotLoaded => '未加载';

  @override
  String get settingsModelHint => '使用已安装模型的 ID 作为默认模型。';

  @override
  String settingsOutputTokensHint(Object max) {
    return '每次回复最多 $max Tokens。';
  }

  @override
  String get settingsAboutBody =>
      'Lociant 将手机、电脑和开发板变成边缘运行时，用于运行本地模型、控制设备能力，并提供受控连接。';

  @override
  String get settingsAboutRuntime => '边缘运行时';

  @override
  String get settingsAboutRuntimeSub => '本地模型推理与设备能力控制';

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
  String get settingsPermissionBackground => '后台运行';

  @override
  String get settingsPermissionAccessibility => '无障碍';

  @override
  String get settingsPermissionSensor => '传感器';

  @override
  String get settingsPermissionNotificationHint => '让运行时在后台保持通知并显示状态。';

  @override
  String get settingsPermissionBackgroundHint => '减少系统对后台服务的限制，保障持续运行。';

  @override
  String get settingsPermissionAccessibilityHint => '读取屏幕结构并执行界面操作。';

  @override
  String get settingsPermissionCameraHint => '允许视觉工具采集摄像头画面。';

  @override
  String get settingsPermissionSensorHint => '允许传感器工具读取运动、光线等数据。';

  @override
  String get settingsPermissionOverlayHint => '允许运行时显示悬浮控件。';

  @override
  String get settingsPermissionFileRead => '文件读取';

  @override
  String get settingsPermissionFileReadHint => '只能读取当前系统用户有权限访问的路径。';

  @override
  String get settingsPermissionSystemManaged => '系统控制';

  @override
  String get settingsPermissionAllowed => '已允许';

  @override
  String get settingsPermissionRequired => '需要授权';

  @override
  String get settingsPermissionChecking => '检查中';

  @override
  String get settingsPermissionManage => '管理';

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
