import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lociant_flutter/core/api_client.dart';
import 'package:lociant_flutter/platform/platform_service.dart';

void main() {
  test('ApiClient syncPort preserves a configured remote host', () {
    final api = ApiClient(baseUrl: 'http://192.168.10.103:11434');

    api.syncPort(12001);

    expect(api.baseUrl, 'http://192.168.10.103:12001');
  });

  test('ApiClient rejects legacy JSON error responses', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((request) async {
      request.response
        ..headers.contentType = ContentType.json
        ..write(jsonEncode({'error': 'peer node offline'}));
      await request.response.close();
    });

    final api = ApiClient(baseUrl: 'http://127.0.0.1:${server.port}');
    await expectLater(
      api.get('/api/v1/peers/board/baby/state'),
      throwsA(
        isA<ApiException>().having(
          (error) => error.message,
          'message',
          contains('peer node offline'),
        ),
      ),
    );
    await server.close(force: true);
  });

  test('HttpPlatformService maps methods to control-plane resources', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final requests = <String>[];
    server.listen((request) async {
      requests.add('${request.method} ${request.uri.path}');
      final body = <String, dynamic>{
        'running': true,
        'port': 11434,
        'url': 'http://127.0.0.1:11434',
      };
      request.response
        ..headers.contentType = ContentType.json
        ..write(jsonEncode(body));
      await request.response.close();
    });

    final api = ApiClient(baseUrl: 'http://127.0.0.1:${server.port}');
    final platform = HttpPlatformService(api);

    final runtime = await platform.call('runtimeState');
    expect(runtime['running'], true);

    await platform.call('updateRuntimeSettings', {'modelId': 'm'});
    await platform.call('startRuntime');
    await platform.call('startVision');

    expect(requests, contains('GET /api/v1/runtime'));
    expect(requests, contains('PUT /api/v1/settings'));
    await server.close(force: true);
  });
}
