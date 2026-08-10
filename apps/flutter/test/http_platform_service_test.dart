import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lociant_flutter/core/api_client.dart';
import 'package:lociant_flutter/platform/platform_service.dart';

void main() {
  test('HttpPlatformService maps methods to control-plane resources', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final requests = <String>[];
    server.listen((request) async {
      requests.add('${request.method} ${request.uri.path}');
      final body = <String, dynamic>{
        'running': true,
        'port': 11434,
        'url': 'http://127.0.0.1:11434',
        'sessions': <dynamic>[],
      };
      if (request.uri.path.startsWith('/api/v1/sessions/')) {
        body['messages'] = <dynamic>[];
      }
      request.response
        ..headers.contentType = ContentType.json
        ..write(jsonEncode(body));
      await request.response.close();
    });

    final api = ApiClient(baseUrl: 'http://127.0.0.1:${server.port}');
    final platform = HttpPlatformService(api);

    final runtime = await platform.call('runtimeState');
    expect(runtime['running'], true);

    final details = await platform.call('sessionDetails', {'sessionId': 'abc'});
    expect(details['session'], isA<Map<String, dynamic>>());
    expect((details['session'] as Map)['messages'], isA<List<dynamic>>());

    await platform.call('selectSession', {'sessionId': 'abc'});
    await platform.call('updateRuntimeSettings', {'modelId': 'm'});
    await platform.call('deleteSession', {'sessionId': 'abc'});
    await platform.call('startRuntime');
    await platform.call('startVision');

    expect(requests, contains('GET /api/v1/runtime'));
    expect(requests, contains('GET /api/v1/sessions/abc'));
    expect(requests, contains('PUT /api/v1/settings'));
    expect(requests, contains('DELETE /api/v1/sessions/abc'));
    await server.close(force: true);
  });
}
