import 'dart:convert';

import 'package:flutter/services.dart';

import '../core/api_client.dart';

/// The native surface available to the UI. On Android this is implemented by
/// [LociantPlatformChannel]; on desktop it is [HttpPlatformService] talking to
/// the local Rust backend's control plane with the same method names.
abstract class PlatformService {
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]);

  Stream<Map<String, dynamic>> events();
}

/// Talks to the local Rust backend over HTTP. Runtime/session operations map
/// to `/api/v1` resources; Android-only operations (permissions, floating
/// window, vision, lifecycle) are no-ops returning the current state, since
/// on desktop the server is always the runtime.
class HttpPlatformService implements PlatformService {
  HttpPlatformService(this.api);

  final ApiClient api;

  @override
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    switch (method) {
      case 'runtimeState':
        return _map(await api.get('/api/v1/runtime'));
      case 'createSession':
        return _map(await api.post('/api/v1/sessions', payload));
      case 'selectSession':
        final id = payload?['sessionId'] as String? ?? '';
        await api.put('/api/v1/settings', {'currentSessionId': id});
        return _map(await api.get('/api/v1/runtime'));
      case 'deleteSession':
        final id = payload?['sessionId'] as String? ?? '';
        await api.delete('/api/v1/sessions/$id');
        return _map(await api.get('/api/v1/runtime'));
      case 'sessionDetails':
        final id = payload?['sessionId'] as String? ?? '';
        final details = _map(await api.get('/api/v1/sessions/$id'));
        return {'session': details};
      case 'updateRuntimeSettings':
        await api.put('/api/v1/settings', payload);
        return _map(await api.get('/api/v1/runtime'));
      case 'releaseRuntimeModel':
        return _map(await api.get('/api/v1/runtime'));
      case 'startRuntime':
      case 'stopRuntime':
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
      case 'openPermissionSettings':
      case 'openExternalUrl':
      case 'installModelPackage':
        return _map(await api.get('/api/v1/runtime'));
      default:
        return const {};
    }
  }

  @override
  Stream<Map<String, dynamic>> events() => const Stream.empty();

  Map<String, dynamic> _map(dynamic value) =>
      value is Map<String, dynamic> ? value : const {};
}

/// Android hybrid: core server state (runtime, sessions, settings, models)
/// comes from the Rust backend over HTTP; Android-only operations
/// (lifecycle, permissions, floating window, vision, file import) still go to
/// the Kotlin host through the method channel.
class AndroidPlatformService extends HttpPlatformService {
  AndroidPlatformService(super.api);

  static const MethodChannel _methods = MethodChannel(
    'io.lociant.android/platform',
  );

  static const Set<String> _androidOnly = {
    'startRuntime',
    'stopRuntime',
    'startVision',
    'stopVision',
    'showRuntimeWindow',
    'hideRuntimeWindow',
    'updateRuntimeWindow',
    'requestCameraPermission',
    'requestNotificationPermission',
    'requestOverlayPermission',
    'requestBatteryOptimizationExemption',
    'requestAccessibilityPermission',
    'openAppSettings',
    'openPermissionSettings',
    'openExternalUrl',
    'installModelPackage',
  };

  @override
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    if (method == 'runtimeState') {
      // Rust core state + Kotlin device fields, so window/permission/vision
      // status stays accurate while the server core lives in Rust.
      final core = await super.call('runtimeState');
      final device = await _invoke('deviceState');
      return {...core, ...device};
    }
    if (_androidOnly.contains(method)) {
      return _invoke(method, payload);
    }
    return super.call(method, payload);
  }

  @override
  // State refreshes happen through the 2s HTTP poll; the Kotlin event channel
  // is not consulted so its parallel (stale) server state can't leak in.
  Stream<Map<String, dynamic>> events() => const Stream.empty();

  Future<Map<String, dynamic>> _invoke(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    final raw = await _methods.invokeMethod<String>(
      method,
      payload == null ? null : jsonEncode(payload),
    );
    return raw == null || raw.isEmpty ? const {} : _decode(raw);
  }

  Map<String, dynamic> _decode(String raw) =>
      jsonDecode(raw) is Map<String, dynamic>
      ? jsonDecode(raw) as Map<String, dynamic>
      : const {};
}

/// No-op platform service used when no native host is present (e.g. tests).
class HeadlessPlatformService implements PlatformService {
  @override
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]) async => const {};

  @override
  Stream<Map<String, dynamic>> events() => const Stream.empty();
}
