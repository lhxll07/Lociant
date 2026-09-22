import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:lociant_flutter/core/api_client.dart';
import 'package:lociant_flutter/platform/platform_service.dart';
import 'package:lociant_flutter/state/runtime_controller.dart';

void main() {
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

  @override
  Future<Map<String, dynamic>> call(
    String method, [
    Map<String, dynamic>? payload,
  ]) async {
    return method == 'runtimeState' ? state : const {};
  }

  @override
  Stream<Map<String, dynamic>> events() => const Stream.empty();
}
