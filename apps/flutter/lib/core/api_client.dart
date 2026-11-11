import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

class ApiException implements Exception {
  const ApiException(this.message, [this.status]);

  final String message;
  final int? status;

  @override
  String toString() => message;
}

/// Thin HTTP client for the Lociant control plane.
///
/// [baseUrl] and [authToken] are refreshed by the runtime controller whenever
/// the native runtime state changes, so this client works both when the server
/// is running and when it is
/// not.
class ApiClient {
  ApiClient({
    String? baseUrl,
    this.requestTimeout = const Duration(seconds: 30),
  }) : baseUrl = baseUrl ?? defaultBaseUrl;

  /// Local server by default; override with
  /// `--dart-define=LOCIANT_BASE_URL=http://host:port` to point the UI at a
  /// remote Lociant backend (e.g. the RK3588 board).
  static const String defaultBaseUrl = String.fromEnvironment(
    'LOCIANT_BASE_URL',
    defaultValue: 'http://127.0.0.1:11434',
  );

  final http.Client client = http.Client();

  /// Time allowed for sending the request and reading its entire response.
  final Duration requestTimeout;
  String baseUrl;
  String authToken = '';

  /// Whether the control plane is addressed through the local loopback
  /// interface. Remote runtimes intentionally redact their token from
  /// `runtimeState`, so callers must not treat an empty response as a token
  /// deletion when this endpoint is remote.
  bool get isLocalEndpoint {
    final host = Uri.tryParse(baseUrl)?.host;
    return host == '127.0.0.1' || host == 'localhost' || host == '::1';
  }

  /// Keeps an explicitly configured remote host while following a runtime
  /// port change reported by the backend.
  void syncPort(int port) {
    if (port <= 0) return;
    final current = Uri.tryParse(baseUrl);
    if (current == null || !current.hasScheme || current.host.isEmpty) return;
    try {
      baseUrl = current.replace(port: port).toString();
    } catch (_) {
      // Keep the existing URL when it cannot be safely rewritten.
    }
  }

  Uri _uri(String path) => Uri.parse('$baseUrl$path');

  Map<String, String> _headers({bool json = false}) => {
    if (json) 'Content-Type': 'application/json',
    if (authToken.isNotEmpty) 'Authorization': 'Bearer $authToken',
  };

  Future<dynamic> get(String path) => _send('GET', path);

  Future<dynamic> post(String path, [Object? body]) =>
      _send('POST', path, body);

  Future<dynamic> put(String path, [Object? body]) => _send('PUT', path, body);

  Future<dynamic> delete(String path) => _send('DELETE', path);

  Future<dynamic> _send(String method, String path, [Object? body]) async {
    final abort = Completer<void>();
    final request = http.AbortableRequest(
      method,
      _uri(path),
      abortTrigger: abort.future,
    );
    request.headers.addAll(_headers(json: body != null));
    if (body != null) request.body = jsonEncode(body);
    // Headers alone do not complete the request. Cancel a stalled transfer so
    // runtime polling can recover without keeping abandoned streams alive.
    final response = await client
        .send(request)
        .then(http.Response.fromStream)
        .timeout(
          requestTimeout,
          onTimeout: () {
            abort.complete();
            throw TimeoutException(
              '$path: API request timed out',
              requestTimeout,
            );
          },
        );
    final text = utf8.decode(response.bodyBytes);
    dynamic json;
    if (text.isNotEmpty) {
      try {
        json = jsonDecode(text);
      } catch (_) {
        // Leave null; the error path below falls back to a generic message.
      }
    }
    // A few legacy routes return an error object with HTTP 200. Treat the
    // same payload as a failed request so callers cannot render it as data.
    if (response.statusCode >= 400 || _isErrorPayload(json)) {
      throw ApiException(_errorMessage(json, path), response.statusCode);
    }
    return json;
  }

  bool _isErrorPayload(dynamic json) =>
      json is Map &&
      json['error'] is String &&
      (json['error'] as String).trim().isNotEmpty;

  String _errorMessage(dynamic json, String path) {
    if (json is Map) {
      final map = json as Map<String, dynamic>;
      final problem = map['detail'];
      final openAi = map['error'];
      if (problem is String && problem.isNotEmpty) return '$path: $problem';
      if (openAi is String && openAi.isNotEmpty) return '$path: $openAi';
      if (openAi is Map) {
        final message = openAi['message'];
        if (message is String && message.isNotEmpty) return '$path: $message';
      }
      final message = map['message'];
      if (message is String && message.isNotEmpty) return '$path: $message';
    }
    return '$path: API request failed';
  }
}
