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

class ModelInfo {
  const ModelInfo({
    required this.id,
    required this.name,
    required this.runtime,
    required this.type,
    required this.ready,
    required this.installed,
    required this.missingFiles,
  });

  factory ModelInfo.fromJson(Map<String, dynamic> json) => ModelInfo(
    id: str(json, 'id'),
    name: str(json, 'name') == '' ? str(json, 'id') : str(json, 'name'),
    runtime: str(json, 'runtime'),
    type: str(json, 'type'),
    ready: boolOf(json, 'ready'),
    installed: boolOf(json, 'installed'),
    missingFiles: asList(json['missingFiles']).map((e) => '$e').toList(),
  );

  final String id;
  final String name;
  final String runtime;
  final String type;
  final bool ready;
  final bool installed;
  final List<String> missingFiles;

  bool get isChatModel =>
      runtime == 'llama' || runtime == 'rkllm' || type == 'chat' || type == 'llm';
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
    required this.toolExposure,
    required this.autoStart,
    required this.requestCount,
    required this.recentRequests,
    required this.lastError,
    required this.message,
    required this.cameraPermissionGranted,
    required this.notificationPermissionGranted,
    required this.sensorPermissionGranted,
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
      toolExposure: str(json, 'toolExposure', 'action'),
      autoStart: boolOf(json, 'autoStart'),
      requestCount: intOf(json, 'requestCount'),
      recentRequests: asList(json['recentRequests']).toList(),
      lastError: json['lastError'] is String ? json['lastError'] as String : '',
      message: str(json, 'message'),
      cameraPermissionGranted: boolOf(json, 'cameraPermissionGranted'),
      notificationPermissionGranted: boolOf(
        json,
        'notificationPermissionGranted',
      ),
      sensorPermissionGranted: boolOf(json, 'sensorPermissionGranted', true),
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
  final String toolExposure;
  final bool autoStart;
  final int requestCount;
  final List<dynamic> recentRequests;
  final String lastError;
  final String message;
  final bool cameraPermissionGranted;
  final bool notificationPermissionGranted;
  final bool sensorPermissionGranted;
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
}

String prettyJson(Object value) =>
    const JsonEncoder.withIndent('  ').convert(value);
