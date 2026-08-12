import 'dart:async';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../core/api_client.dart';
import '../core/models.dart';
import '../platform/platform_service.dart';

/// Owns the runtime state shared by the whole UI.
///
/// State comes from the native host (`runtimeState()`, pushed through the
/// platform channel) and is refreshed on a short poll plus whenever an event
/// arrives. The API client base URL and auth token follow the state so the
/// rest of the app never has to know where the server lives.
class RuntimeController extends ChangeNotifier {
  RuntimeController(this.platform, this.api, this.prefs) {
    _subscription = platform.events().listen(_onEvent);
  }

  final PlatformService platform;
  final ApiClient api;
  final SharedPreferences prefs;

  RuntimeUiState? state;
  bool chatInFlight = false;
  Timer? _pollTimer;
  StreamSubscription<Map<String, dynamic>>? _subscription;
  bool _refreshing = false;
  final _modelInstallEvents =
      StreamController<Map<String, dynamic>>.broadcast();

  Stream<Map<String, dynamic>> get modelInstallEvents =>
      _modelInstallEvents.stream;

  void startPolling() {
    _pollTimer ??= Timer.periodic(const Duration(seconds: 2), (_) => refresh());
    refresh();
  }

  Future<void> refresh() async {
    if (_refreshing) return;
    _refreshing = true;
    try {
      final next = await platform.call('runtimeState');
      if (next.isEmpty) return;
      if (chatInFlight && next.containsKey('sessions')) {
        // Keep live session previews while a chat is streaming; the in-flight
        // turn is not persisted yet.
        next.remove('sessions');
      }
      final parsed = RuntimeUiState.fromJson(next);
      await _adoptLocalToken(parsed.authToken);
      if (state == null || _changed(state!, parsed)) {
        state = parsed;
        api.baseUrl = parsed.baseUrl;
        notifyListeners();
      }
    } catch (error) {
      debugPrint('runtime refresh failed: $error');
    } finally {
      _refreshing = false;
    }
  }

  void _onEvent(Map<String, dynamic> envelope) {
    final type = envelope['type'];
    final payload = envelope['payload'];
    if (payload is! Map) return;
    if (type == 'runtimeMessage') {
      final map = _stringMap(payload);
      if (chatInFlight && map.containsKey('sessions')) {
        map.remove('sessions');
      }
      final parsed = RuntimeUiState.fromJson(map);
      unawaited(_adoptLocalToken(parsed.authToken));
      if (state == null || _changed(state!, parsed)) {
        state = parsed;
        api.baseUrl = parsed.baseUrl;
        notifyListeners();
      }
    } else if (type == 'modelInstallResult') {
      _modelInstallEvents.add(_stringMap(payload));
    }
  }

  Map<String, dynamic> _stringMap(Map payload) => {
    for (final entry in payload.entries) '${entry.key}': entry.value,
  };

  Future<void> _adoptLocalToken(String token) async {
    if (token.isEmpty || token == api.authToken) return;
    api.authToken = token;
    await prefs.setString('api_auth_token', token);
  }

  bool _changed(RuntimeUiState before, RuntimeUiState after) {
    return before.raw.toString() != after.raw.toString();
  }

  // ---- Platform actions ----

  Future<void> callAndRefresh(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    await platform.call(method, payload);
    await refresh();
  }

  Future<void> startRuntime() => callAndRefresh('startRuntime');
  Future<void> stopRuntime() => callAndRefresh('stopRuntime');
  Future<void> updateSettings(Map<String, dynamic> payload) async {
    final nextToken = payload['authToken'];
    await platform.call('updateRuntimeSettings', payload);
    if (nextToken is String) {
      api.authToken = nextToken.trim();
      if (api.authToken.isEmpty) {
        await prefs.remove('api_auth_token');
      } else {
        await prefs.setString('api_auth_token', api.authToken);
      }
    }
    await refresh();
  }

  Future<void> generateAuthToken() async {
    final random = Random.secure();
    final token = List<int>.generate(
      32,
      (_) => random.nextInt(256),
    ).map((byte) => byte.toRadixString(16).padLeft(2, '0')).join();
    await updateSettings({'authToken': token});
  }

  Future<void> releaseModel() => callAndRefresh('releaseRuntimeModel');
  Future<void> createSession() => callAndRefresh('createSession');
  Future<void> selectSession(String sessionId) =>
      callAndRefresh('selectSession', {'sessionId': sessionId});
  Future<void> deleteSession(String sessionId) =>
      callAndRefresh('deleteSession', {'sessionId': sessionId});
  Future<void> startVision([Map<String, dynamic>? payload]) =>
      callAndRefresh('startVision', payload);
  Future<void> stopVision() => callAndRefresh('stopVision');
  Future<void> showWindow() => callAndRefresh('showRuntimeWindow');
  Future<void> hideWindow() => callAndRefresh('hideRuntimeWindow');
  Future<void> updateWindow(Map<String, dynamic> payload) =>
      callAndRefresh('updateRuntimeWindow', payload);
  Future<void> requestCamera() => callAndRefresh('requestCameraPermission');
  Future<void> requestNotification() =>
      callAndRefresh('requestNotificationPermission');
  Future<void> requestOverlay() => callAndRefresh('requestOverlayPermission');
  Future<void> requestBattery() =>
      callAndRefresh('requestBatteryOptimizationExemption');
  Future<void> requestAccessibility() =>
      callAndRefresh('requestAccessibilityPermission');
  Future<void> openAppSettings() => callAndRefresh('openAppSettings');
  Future<void> openExternalUrl(String url) =>
      callAndRefresh('openExternalUrl', {'url': url});
  Future<void> openPermissionSettings(String kind) =>
      callAndRefresh('openPermissionSettings', {'kind': kind});
  Future<void> openModelPackagePicker() =>
      callAndRefresh('installModelPackage');

  Future<Map<String, dynamic>> sessionDetails(String sessionId) async {
    return platform.call('sessionDetails', {'sessionId': sessionId});
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _subscription?.cancel();
    _modelInstallEvents.close();
    super.dispose();
  }
}
