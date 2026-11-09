import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:lociant_flutter/core/api_client.dart';
import 'package:lociant_flutter/platform/platform_service.dart';
import 'package:lociant_flutter/state/runtime_controller.dart';

void main() {
  test('reports an initial runtime connection failure', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final api = ApiClient();
    addTearDown(api.client.close);
    final platform = _RuntimePlatform({})
      ..error = const ApiException('runtime connection refused');
    final controller = RuntimeController(platform, api, prefs);
    addTearDown(controller.dispose);

    await controller.refresh();

    expect(controller.state, isNotNull);
    expect(controller.state!.running, isFalse);
    expect(controller.state!.starting, isFalse);
    expect(controller.state!.lastError, 'runtime connection refused');
  });

  test(
    'marks a disconnected runtime offline and recovers on refresh',
    () async {
      SharedPreferences.setMockInitialValues({'api_auth_token': 'saved-token'});
      final prefs = await SharedPreferences.getInstance();
      final api = ApiClient(baseUrl: 'http://192.168.10.103:11434')
        ..authToken = 'saved-token';
      addTearDown(api.client.close);
      final platform = _RuntimePlatform({
        'running': true,
        'port': 11434,
        'lanUrl': 'http://192.168.10.103:11434',
        'authToken': '',
        'modelId': 'local-model',
        'modelLoaded': true,
        'modelLoading': true,
        'lastError': '',
      });
      final controller = RuntimeController(platform, api, prefs);
      addTearDown(controller.dispose);
      var notifications = 0;
      controller.addListener(() => notifications++);

      await controller.refresh();
      expect(controller.state!.running, isTrue);
      final onlineState = controller.state!;

      platform.error = const ApiException('runtime connection refused');
      await controller.refresh();

      final offlineState = controller.state!;
      expect(offlineState.running, isFalse);
      expect(offlineState.starting, isFalse);
      expect(offlineState.modelLoaded, isFalse);
      expect(offlineState.modelLoading, isFalse);
      expect(offlineState.lastError, 'runtime connection refused');
      expect(offlineState.modelId, 'local-model');
      expect(offlineState.lanUrl, 'http://192.168.10.103:11434');
      expect(api.baseUrl, 'http://192.168.10.103:11434');
      expect(api.authToken, 'saved-token');
      expect(prefs.getString('api_auth_token'), 'saved-token');
      expect(onlineState.running, isTrue);
      expect(platform.state['running'], isTrue);
      expect(notifications, 2);

      await controller.refresh();
      expect(notifications, 2);

      platform.error = null;
      await controller.refresh();

      expect(controller.state!.running, isTrue);
      expect(controller.state!.modelLoaded, isTrue);
      expect(controller.state!.lastError, isEmpty);
      expect(api.authToken, 'saved-token');
      expect(notifications, 3);
    },
  );

  for (final fails in [false, true]) {
    test(
      'ignores a ${fails ? 'failed' : 'successful'} refresh after disposal',
      () async {
        SharedPreferences.setMockInitialValues({
          'api_auth_token': 'saved-token',
        });
        final prefs = await SharedPreferences.getInstance();
        final api = ApiClient()..authToken = 'saved-token';
        addTearDown(api.client.close);
        final response = Completer<Map<String, dynamic>>();
        final platform = _RuntimePlatform({})..pendingResponse = response;
        final controller = RuntimeController(platform, api, prefs);

        final refresh = controller.refresh();
        controller.dispose();
        if (fails) {
          response.completeError(
            const ApiException('runtime connection refused'),
          );
        } else {
          response.complete({'running': true, 'authToken': 'late-token'});
        }
        await refresh;

        expect(controller.state, isNull);
        expect(api.authToken, 'saved-token');
        expect(prefs.getString('api_auth_token'), 'saved-token');
      },
    );
  }

  test('keeps a saved token when a remote runtime redacts it', () async {
    SharedPreferences.setMockInitialValues({'api_auth_token': 'saved-token'});
    final prefs = await SharedPreferences.getInstance();
    final api = ApiClient(baseUrl: 'http://192.168.10.103:11434')
      ..authToken = 'saved-token';
    final controller = RuntimeController(
      _RuntimePlatform({'running': true, 'port': 11434, 'authToken': ''}),
      api,
      prefs,
    );

    await controller.refresh();

    expect(api.authToken, 'saved-token');
    expect(prefs.getString('api_auth_token'), 'saved-token');
    controller.dispose();
  });

  test('clears a token when the local runtime reports it disabled', () async {
    SharedPreferences.setMockInitialValues({'api_auth_token': 'saved-token'});
    final prefs = await SharedPreferences.getInstance();
    final api = ApiClient()..authToken = 'saved-token';
    final controller = RuntimeController(
      _RuntimePlatform({'running': true, 'port': 11434, 'authToken': ''}),
      api,
      prefs,
    );

    await controller.refresh();

    expect(api.authToken, isEmpty);
    expect(prefs.getString('api_auth_token'), isNull);
    controller.dispose();
  });
}

class _RuntimePlatform implements PlatformService {
  _RuntimePlatform(this.state);

  final Map<String, dynamic> state;
  Object? error;
  Completer<Map<String, dynamic>>? pendingResponse;

  @override
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    final pending = pendingResponse;
    if (pending != null) return pending.future;
    final failure = error;
    if (failure != null) throw failure;
    return method == 'runtimeState' ? state : const {};
  }

  @override
  Stream<Map<String, dynamic>> events() => const Stream.empty();
}
