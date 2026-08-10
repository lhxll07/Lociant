import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api_client.dart';
import 'chat_streamer.dart';
import 'models.dart';

/// Parses the SSE stream produced by Lociant's `/v1/chat/completions`
/// agent loop. Mirrors the events the web UI consumed: content chunks,
/// `reasoning_content`, fragmented `tool_calls`, `lociant` ping/phase events
/// and the terminating `[DONE]`.
class SseChatClient implements ChatStreamer {
  const SseChatClient(this.api);

  final ApiClient api;

  @override
  Future<ChatRunResult> streamChat(
    Map<String, dynamic> body, {
    required void Function(String text) onChunk,
    required void Function(String text) onReasoning,
    required void Function(ToolCallPart call) onToolCall,
    required void Function(String phase, String tool, int round) onPhase,
  }) async {
    final request = http.Request('POST', Uri.parse('${api.baseUrl}/v1/chat/completions'));
    request.headers['Content-Type'] = 'application/json';
    if (api.authToken.isNotEmpty) request.headers['Authorization'] = 'Bearer ${api.authToken}';
    request.body = jsonEncode(body);

    final response = await api.client.send(request).timeout(const Duration(seconds: 30));
    if (response.statusCode >= 400) {
      final text = await response.stream.bytesToString();
      dynamic json;
      try {
        json = jsonDecode(text);
      } catch (_) {}
      var message = 'API request failed';
      if (json is Map && json['error'] is Map) {
        message = (json['error'] as Map)['message']?.toString() ?? message;
      }
      throw ApiException(message, response.statusCode);
    }

    final text = StringBuffer();
    final reasoning = StringBuffer();
    final accumulator = _ToolCallAccumulator();
    final lines = response.stream.transform(utf8.decoder).transform(const LineSplitter());
    String? error;

    await for (final line in lines) {
      final trimmed = line.trim();
      if (!trimmed.startsWith('data:')) continue;
      final data = trimmed.substring(5).trim();
      if (data.isEmpty || data == '[DONE]') continue;

      final dynamic json;
      try {
        json = jsonDecode(data);
      } catch (_) {
        continue;
      }
      if (json is! Map) continue;
      final map = json as Map<String, dynamic>;

      final openAiError = map['error'];
      if (openAiError is Map) {
        error = openAiError['message']?.toString() ?? 'API request failed';
        break;
      }

      final lociant = map['lociant'];
      if (lociant is Map) {
        final type = lociant['type'];
        if (type == 'phase') {
          final phase = lociant['phase']?.toString() ?? '';
          final tool = lociant['tool']?.toString() ?? '';
          final round = lociant['round'] is num ? (lociant['round'] as num).toInt() : 0;
          onPhase(phase, tool, round);
        }
        continue;
      }

      final choices = map['choices'];
      if (choices is! List || choices.isEmpty) continue;
      final choice = choices.first;
      if (choice is! Map) continue;
      final delta = choice['delta'];
      if (delta is! Map) continue;

      final content = delta['content'];
      if (content is String && content.isNotEmpty) {
        text.write(content);
        onChunk(content);
      }
      final reason = delta['reasoning_content'];
      if (reason is String && reason.isNotEmpty) {
        reasoning.write(reason);
        onReasoning(reason);
      }
      final calls = delta['tool_calls'];
      if (calls is List) {
        for (final raw in calls) {
          if (raw is! Map) continue;
          final part = accumulator.push(raw as Map<String, dynamic>);
          if (part != null) onToolCall(part);
        }
      }
    }

    return ChatRunResult(
      text: text.toString(),
      reasoning: reasoning.toString(),
      toolCalls: accumulator.values,
      error: error,
    );
  }
}

class _ToolCallAccumulator {
  final Map<String, Map<String, dynamic>> _calls = {};

  ToolCallPart? push(Map<String, dynamic> raw) {
    final index = raw['index'] is num ? (raw['index'] as num).toInt() : null;
    final id = raw['id']?.toString() ?? '';
    final key = id.isNotEmpty ? id : '${index ?? _calls.length}';
    final existing = _calls.putIfAbsent(
      key,
      () => {'name': '', 'arguments': '', 'index': index},
    );
    if (id.isNotEmpty) existing['id'] = id;
    if (raw['type'] is String) existing['type'] = raw['type'];
    final function = raw['function'];
    if (function is Map) {
      final name = function['name'];
      if (name is String && name.isNotEmpty) existing['name'] = name;
      final args = function['arguments'];
      if (args is String && args.isNotEmpty) {
        existing['arguments'] = '${existing['arguments']}$args';
      }
    }
    final name = existing['name']?.toString() ?? '';
    if (name.isEmpty) return null;
    return ToolCallPart(
      key: key,
      id: existing['id']?.toString() ?? key,
      index: existing['index'] as int?,
      name: name,
      arguments: existing['arguments']?.toString() ?? '',
    );
  }

  List<ToolCallPart> get values {
    final entries = _calls.entries.toList()
      ..sort((a, b) {
        final ai = a.value['index'] as int? ?? 0;
        final bi = b.value['index'] as int? ?? 0;
        return ai.compareTo(bi);
      });
    return entries
        .map((e) => ToolCallPart(
              key: e.key,
              id: e.value['id']?.toString() ?? e.key,
              index: e.value['index'] as int?,
              name: e.value['name']?.toString() ?? 'tool',
              arguments: e.value['arguments']?.toString() ?? '',
            ))
        .toList();
  }
}
