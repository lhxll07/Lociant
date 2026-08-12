/// Typed views over the JSON shapes the Lociant server and platform channel
/// already expose. Parsing is deliberately defensive: unknown or missing
/// fields degrade to safe defaults instead of throwing.
library;

import 'dart:convert';

Map<String, dynamic> asMap(dynamic value) =>
    value is Map<String, dynamic> ? value : const {};

List<dynamic> asList(dynamic value) => value is List ? value : const [];

String str(Map<String, dynamic> map, String key, [String fallback = '']) {
  final value = map[key];
  return value is String ? value : fallback;
}

int intOf(Map<String, dynamic> map, String key, [int fallback = 0]) {
  final value = map[key];
  return value is num ? value.toInt() : fallback;
}

bool boolOf(Map<String, dynamic> map, String key, [bool fallback = false]) {
  final value = map[key];
  return value is bool ? value : fallback;
}

double doubleOf(Map<String, dynamic> map, String key, [double fallback = 0]) {
  final value = map[key];
  return value is num ? value.toDouble() : fallback;
}

class SessionSummary {
  const SessionSummary({
    required this.id,
    required this.title,
    required this.modelId,
    required this.updatedAt,
    required this.messageCount,
    required this.lastRole,
    required this.lastText,
  });

  factory SessionSummary.fromJson(Map<String, dynamic> json) => SessionSummary(
    id: str(json, 'id'),
    title: str(json, 'title'),
    modelId: str(json, 'modelId'),
    updatedAt: intOf(json, 'updatedAt'),
    messageCount: intOf(json, 'messageCount'),
    lastRole: str(json, 'lastRole'),
    lastText: str(json, 'lastText'),
  );

  final String id;
  final String title;
  final String modelId;
  final int updatedAt;
  final int messageCount;
  final String lastRole;
  final String lastText;
}

class SessionMessage {
  const SessionMessage({
    required this.id,
    required this.role,
    required this.text,
  });

  factory SessionMessage.fromJson(Map<String, dynamic> json) => SessionMessage(
    id: intOf(json, 'id').toString(),
    role: str(json, 'role', 'assistant'),
    text: str(json, 'text') == '' ? str(json, 'content') : str(json, 'text'),
  );

  final String id;
  final String role;
  final String text;
}

class ModelInfo {
  const ModelInfo({
    required this.id,
    required this.name,
    required this.runtime,
    required this.type,
    required this.ready,
    required this.installed,
    required this.missingFiles,
    required this.cloud,
  });

  factory ModelInfo.fromJson(Map<String, dynamic> json) => ModelInfo(
    id: str(json, 'id'),
    name: str(json, 'name') == '' ? str(json, 'id') : str(json, 'name'),
    runtime: str(json, 'runtime'),
    type: str(json, 'type'),
    ready: boolOf(json, 'ready'),
    installed: boolOf(json, 'installed'),
    missingFiles: asList(json['missingFiles']).map((e) => '$e').toList(),
    cloud: boolOf(json, 'cloud'),
  );

  final String id;
  final String name;
  final String runtime;
  final String type;
  final bool ready;
  final bool installed;
  final List<String> missingFiles;
  final bool cloud;

  bool get isChatModel =>
      runtime == 'mnn' || type == 'vlm' || type == 'chat' || type == 'llm';
}

class MarketModel {
  const MarketModel({
    required this.id,
    required this.repo,
    required this.name,
    required this.description,
    required this.vendor,
    required this.sizeGb,
    required this.fileSize,
    required this.tags,
    required this.installed,
  });

  factory MarketModel.fromJson(Map<String, dynamic> json) => MarketModel(
    id: str(json, 'id'),
    repo: str(json, 'repo'),
    name: str(json, 'name'),
    description: str(json, 'description'),
    vendor: str(json, 'vendor'),
    sizeGb: doubleOf(json, 'sizeGb'),
    fileSize: intOf(json, 'fileSize'),
    tags: asList(json['tags']).map((e) => '$e').toList(),
    installed: boolOf(json, 'installed'),
  );

  final String id;
  final String repo;
  final String name;
  final String description;
  final String vendor;
  final double sizeGb;
  final int fileSize;
  final List<String> tags;
  final bool installed;
}

class InstallProgress {
  const InstallProgress({
    required this.jobId,
    required this.modelId,
    required this.state,
    required this.active,
    required this.progress,
    required this.message,
  });

  factory InstallProgress.fromJson(Map<String, dynamic> json) {
    final rawState = str(json, 'state').toLowerCase();
    final active = json['active'] is bool ? boolOf(json, 'active') : true;
    final raw = json['percent'] ?? json['progress'];
    double? percent;
    if (raw is num) {
      percent = raw <= 1 ? raw * 100 : raw.toDouble();
    }
    var state = rawState;
    if (state.isEmpty) state = 'installing';
    if (state == 'done' || (!active && (percent ?? 0) >= 100)) state = 'done';
    return InstallProgress(
      jobId: str(json, 'jobId'),
      modelId: str(json, 'modelId'),
      state: state,
      active: active && state != 'done' && state != 'error',
      progress: percent,
      message: str(json, 'message'),
    );
  }

  final String jobId;
  final String modelId;
  final String state;
  final bool active;
  final double? progress;
  final String message;
}

class ToolCallPart {
  const ToolCallPart({
    required this.key,
    required this.id,
    required this.index,
    required this.name,
    required this.arguments,
  });

  final String key;
  final String id;
  final int? index;
  final String name;
  final String arguments;
}

class ChatRunResult {
  const ChatRunResult({
    required this.text,
    required this.reasoning,
    required this.toolCalls,
    this.error,
  });

  final String text;
  final String reasoning;
  final List<ToolCallPart> toolCalls;
  final String? error;

  bool get ok => error == null;
}

/// Runtime state as reported by `runtimeState()` through the platform channel.
/// Unmodeled fields stay available in [raw] for forward compatibility.
class RuntimeUiState {
  const RuntimeUiState({
    required this.raw,
    required this.running,
    required this.starting,
    required this.port,
    required this.lanUrl,
    required this.url,
    required this.authToken,
    required this.modelId,
    required this.modelLoaded,
    required this.modelLoading,
    required this.maxOutputTokens,
    required this.hardMaxOutputTokens,
    required this.cpuThreads,
    required this.maxCpuThreads,
    required this.inferenceBackend,
    required this.contextProfile,
    required this.historyLimit,
    required this.agentMaxRounds,
    required this.agentPolicy,
    required this.cloudEnabled,
    required this.cloudModel,
    required this.cloudBaseUrl,
    required this.cloudApiKey,
    required this.cloudMaxOutputTokens,
    required this.cloudContextWindow,
    required this.cloudHistoryLimit,
    required this.toolExposure,
    required this.autoStart,
    required this.currentSessionId,
    required this.sessions,
    required this.requestCount,
    required this.recentRequests,
    required this.lastError,
    required this.message,
    required this.cameraPermissionGranted,
    required this.notificationPermissionGranted,
    required this.windowAllowed,
    required this.windowVisible,
    required this.windowState,
    required this.windowAutoShow,
    required this.batteryOptimizationIgnored,
    required this.accessibilityPermissionGranted,
    required this.vision,
    required this.device,
  });

  factory RuntimeUiState.fromJson(Map<String, dynamic> json) {
    final policy = asMap(json['agentPolicy']);
    return RuntimeUiState(
      raw: json,
      running: boolOf(json, 'running'),
      starting: boolOf(json, 'starting'),
      port: intOf(json, 'port', 11434),
      lanUrl: str(json, 'lanUrl'),
      url: str(json, 'url'),
      authToken: str(json, 'authToken'),
      modelId: str(json, 'modelId'),
      modelLoaded: boolOf(json, 'modelLoaded'),
      modelLoading: boolOf(json, 'modelLoading'),
      maxOutputTokens: intOf(json, 'maxOutputTokens', 512),
      hardMaxOutputTokens: intOf(json, 'hardMaxOutputTokens', 32768),
      cpuThreads: intOf(json, 'cpuThreads', 4),
      maxCpuThreads: intOf(json, 'maxCpuThreads', 16),
      inferenceBackend: str(json, 'inferenceBackend', 'model'),
      contextProfile: str(json, 'contextProfile', 'balanced'),
      historyLimit: intOf(json, 'historyLimit', 64),
      agentMaxRounds: intOf(json, 'agentMaxRounds', 32),
      agentPolicy: policy,
      cloudEnabled: boolOf(json, 'cloudEnabled'),
      cloudModel: str(json, 'cloudModel'),
      cloudBaseUrl: str(json, 'cloudBaseUrl'),
      cloudApiKey: str(json, 'cloudApiKey'),
      cloudMaxOutputTokens: intOf(json, 'cloudMaxOutputTokens'),
      cloudContextWindow: intOf(json, 'cloudContextWindow', 131072),
      cloudHistoryLimit: intOf(json, 'cloudHistoryLimit', 256),
      toolExposure: str(json, 'toolExposure', 'action'),
      autoStart: boolOf(json, 'autoStart'),
      currentSessionId: str(json, 'currentSessionId'),
      sessions: asList(
        json['sessions'],
      ).whereType<Map>().map((e) => SessionSummary.fromJson(asMap(e))).toList(),
      requestCount: intOf(json, 'requestCount'),
      recentRequests: asList(json['recentRequests']).toList(),
      lastError: json['lastError'] is String ? json['lastError'] as String : '',
      message: str(json, 'message'),
      cameraPermissionGranted: boolOf(json, 'cameraPermissionGranted'),
      notificationPermissionGranted: boolOf(
        json,
        'notificationPermissionGranted',
      ),
      windowAllowed: boolOf(json, 'windowAllowed'),
      windowVisible: boolOf(json, 'windowVisible'),
      windowState: str(json, 'windowState', 'hidden'),
      windowAutoShow: boolOf(json, 'windowAutoShow'),
      batteryOptimizationIgnored: boolOf(json, 'batteryOptimizationIgnored'),
      accessibilityPermissionGranted: boolOf(
        json,
        'accessibilityPermissionGranted',
      ),
      vision: asMap(json['vision']),
      device: asMap(json['device']),
    );
  }

  final Map<String, dynamic> raw;
  final bool running;
  final bool starting;
  final int port;
  final String lanUrl;
  final String url;
  final String authToken;
  final String modelId;
  final bool modelLoaded;
  final bool modelLoading;
  final int maxOutputTokens;
  final int hardMaxOutputTokens;
  final int cpuThreads;
  final int maxCpuThreads;
  final String inferenceBackend;
  final String contextProfile;
  final int historyLimit;
  final int agentMaxRounds;
  final Map<String, dynamic> agentPolicy;
  final bool cloudEnabled;
  final String cloudModel;
  final String cloudBaseUrl;
  final String cloudApiKey;
  final int cloudMaxOutputTokens;
  final int cloudContextWindow;
  final int cloudHistoryLimit;
  final String toolExposure;
  final bool autoStart;
  final String currentSessionId;
  final List<SessionSummary> sessions;
  final int requestCount;
  final List<dynamic> recentRequests;
  final String lastError;
  final String message;
  final bool cameraPermissionGranted;
  final bool notificationPermissionGranted;
  final bool windowAllowed;
  final bool windowVisible;
  final String windowState;
  final bool windowAutoShow;
  final bool batteryOptimizationIgnored;
  final bool accessibilityPermissionGranted;
  final Map<String, dynamic> vision;
  final Map<String, dynamic> device;

  bool get active => running || starting;
  String get baseUrl => 'http://127.0.0.1:$port';

  RuntimeUiState copyWith({List<SessionSummary>? sessions}) => RuntimeUiState(
    raw: sessions == null
        ? raw
        : {...raw, 'sessions': sessions.map((e) => e.toJson())},
    running: running,
    starting: starting,
    port: port,
    lanUrl: lanUrl,
    url: url,
    authToken: authToken,
    modelId: modelId,
    modelLoaded: modelLoaded,
    modelLoading: modelLoading,
    maxOutputTokens: maxOutputTokens,
    hardMaxOutputTokens: hardMaxOutputTokens,
    cpuThreads: cpuThreads,
    maxCpuThreads: maxCpuThreads,
    inferenceBackend: inferenceBackend,
    contextProfile: contextProfile,
    historyLimit: historyLimit,
    agentMaxRounds: agentMaxRounds,
    agentPolicy: agentPolicy,
    cloudEnabled: cloudEnabled,
    cloudModel: cloudModel,
    cloudBaseUrl: cloudBaseUrl,
    cloudApiKey: cloudApiKey,
    cloudMaxOutputTokens: cloudMaxOutputTokens,
    cloudContextWindow: cloudContextWindow,
    cloudHistoryLimit: cloudHistoryLimit,
    toolExposure: toolExposure,
    autoStart: autoStart,
    currentSessionId: currentSessionId,
    sessions: sessions ?? this.sessions,
    requestCount: requestCount,
    recentRequests: recentRequests,
    lastError: lastError,
    message: message,
    cameraPermissionGranted: cameraPermissionGranted,
    notificationPermissionGranted: notificationPermissionGranted,
    windowAllowed: windowAllowed,
    windowVisible: windowVisible,
    windowState: windowState,
    windowAutoShow: windowAutoShow,
    batteryOptimizationIgnored: batteryOptimizationIgnored,
    accessibilityPermissionGranted: accessibilityPermissionGranted,
    vision: vision,
    device: device,
  );
}

extension SessionSummaryJson on SessionSummary {
  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'modelId': modelId,
    'updatedAt': updatedAt,
    'messageCount': messageCount,
    'lastRole': lastRole,
    'lastText': lastText,
  };
}

String prettyJson(Object value) =>
    const JsonEncoder.withIndent('  ').convert(value);
