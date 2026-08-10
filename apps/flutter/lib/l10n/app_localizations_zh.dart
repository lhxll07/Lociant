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
  String get navSettings => '设置';

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
