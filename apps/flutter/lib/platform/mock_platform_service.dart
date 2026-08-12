import 'dart:async';

import '../core/models.dart';
import 'platform_service.dart';

/// Fake native host for standalone UI development (`flutter run
/// --dart-define=LOCIANT_MOCK=true`). Lets you iterate on the UI with hot
/// reload without building/installing the full Android app.
class MockPlatformService implements PlatformService {
  final _events = StreamController<Map<String, dynamic>>.broadcast();
  Map<String, dynamic> _state = _initialState();

  @override
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    switch (method) {
      case 'runtimeState':
        return _state;
      case 'sessionDetails':
        return _sessionDetails(payload?['sessionId']?.toString() ?? '');
      case 'updateRuntimeSettings':
        final next = payload ?? const {};
        _state = Map<String, dynamic>.from(_state)..addAll(next);
        return _state;
      case 'selectSession':
      case 'createSession':
      case 'deleteSession':
      case 'startRuntime':
      case 'stopRuntime':
      case 'releaseRuntimeModel':
      case 'startVision':
      case 'stopVision':
      case 'showRuntimeWindow':
      case 'hideRuntimeWindow':
      case 'updateRuntimeWindow':
      case 'requestCameraPermission':
      case 'requestNotificationPermission':
      case 'requestOverlayPermission':
      case 'requestBatteryOptimizationExemption':
      case 'requestAccessibilityPermission':
      case 'openAppSettings':
      case 'openExternalUrl':
      case 'openPermissionSettings':
      case 'installModelPackage':
        return {'ok': true};
      default:
        return {'ok': true};
    }
  }

  @override
  Stream<Map<String, dynamic>> events() => _events.stream;

  Map<String, dynamic> _sessionDetails(String sessionId) {
    final session = _state['sessions'] as List;
    final match = session
        .cast<Map>()
        .where((s) => s['id'] == sessionId)
        .toList();
    final id = match.isNotEmpty
        ? sessionId
        : (session.isNotEmpty ? session.first['id'] : 'chat_default');
    return {
      'session': {
        'id': id,
        'title': 'Mock session',
        'modelId': 'deepseek-v4-flash',
        'messages': [
          {
            'id': 1,
            'role': 'user',
            'text': '请先调用 runtime_status，再调用 model_list，然后总结。',
          },
          {
            'id': 2,
            'role': 'assistant',
            'text': '我先看一下运行状态。',
            'contentJson': {
              'reasoning': '用户要求先查询运行状态，我应该调用 runtime_status 工具。',
            },
          },
          {
            'id': 3,
            'role': 'tool',
            'name': 'runtime_status',
            'text': '{"running": true, "port": 11434}',
          },
          {'id': 4, 'role': 'assistant', 'text': '运行正常。再看一下模型列表。'},
          {
            'id': 5,
            'role': 'tool',
            'name': 'model_list',
            'text': '{"models": ["deepseek-v4-flash"]}',
          },
          {'id': 6, 'role': 'assistant', 'text': '总结：服务运行中，模型列表已获取。'},
        ],
      },
    };
  }

  static Map<String, dynamic> _initialState() => {
    'running': true,
    'starting': false,
    'host': '0.0.0.0',
    'port': 11434,
    'url': 'http://0.0.0.0:11434',
    'lanUrl': 'http://192.168.1.100:11434',
    'authEnabled': false,
    'authToken': '',
    'toolExposure': 'action',
    'modelId': 'deepseek-v4-flash',
    'modelLoading': false,
    'modelLoaded': true,
    'maxOutputTokens': 512,
    'hardMaxOutputTokens': 32768,
    'cpuThreads': 4,
    'maxCpuThreads': 8,
    'inferenceBackend': 'model',
    'contextProfile': 'balanced',
    'historyLimit': 64,
    'agentMaxRounds': 32,
    'agentPolicy': {
      'maxRounds': 32,
      'roundsMin': 8,
      'roundsMax': 64,
      'maxToolCalls': 64,
    },
    'cloudEnabled': true,
    'cloudModel': 'deepseek-v4-flash',
    'cloudBaseUrl': 'https://api.deepseek.com/v1',
    'cloudApiKey': 'sk-mock',
    'cloudMaxOutputTokens': 0,
    'cloudContextWindow': 131072,
    'cloudHistoryLimit': 256,
    'autoStart': false,
    'currentSessionId': 'chat_mock_1',
    'sessions': [
      SessionSummary(
        id: 'chat_mock_1',
        title: 'Mock session',
        modelId: 'deepseek-v4-flash',
        updatedAt: DateTime.now().millisecondsSinceEpoch,
        messageCount: 6,
        lastRole: 'assistant',
        lastText: '总结：服务运行中。',
      ).toJson(),
      SessionSummary(
        id: 'chat_mock_2',
        title: '另一个会话',
        modelId: 'qwen3.5-2b-mnn',
        updatedAt: DateTime.now().millisecondsSinceEpoch - 3600000,
        messageCount: 2,
        lastRole: 'user',
        lastText: '你好',
      ).toJson(),
    ],
    'requestCount': 12,
    'recentRequests': [
      {
        'method': 'GET',
        'endpoint': '/health',
        'status': 200,
        'elapsedMs': 3,
        'time': 0,
      },
    ],
    'lastError': '',
    'message': 'Mock runtime is serving',
    'cameraPermissionGranted': true,
    'notificationPermissionGranted': true,
    'windowAllowed': true,
    'windowVisible': false,
    'windowState': 'hidden',
    'windowAutoShow': false,
    'batteryOptimizationIgnored': true,
    'accessibilityPermissionGranted': true,
    'vision': {'state': 'idle', 'running': false, 'message': 'Mock vision'},
    'device': {
      'interactive': true,
      'screenOn': true,
      'keyguardLocked': false,
      'activityForeground': true,
    },
  };
}
