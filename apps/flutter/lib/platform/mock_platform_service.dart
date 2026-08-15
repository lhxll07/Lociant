import 'dart:async';

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
      case 'updateRuntimeSettings':
        final next = payload ?? const {};
        _state = Map<String, dynamic>.from(_state)..addAll(next);
        return _state;
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
      case 'requestSensorPermission':
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
    'autoStart': false,
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
    'sensorPermissionGranted': true,
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
