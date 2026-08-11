import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../core/api_client.dart';
import '../core/chat_streamer.dart';
import '../core/sse_chat_client.dart';
import 'runtime_controller.dart';

class ChatItem {
  ChatItem({
    required this.role,
    required this.text,
    this.imageDataUrl,
    this.isError = false,
  });

  final String role;
  String text;
  String reasoning = '';
  bool reasoningExpanded = false;
  int round = 0;
  final List<ToolBubble> tools = [];
  final String? imageDataUrl;
  bool isError;
}

class ToolBubble {
  ToolBubble({required this.name, required this.arguments, required this.status});

  final String name;
  String arguments;
  String status;
}

/// Drives the home chat: sessions, message history and the streaming agent
/// loop. The transport is the same OpenAI-compatible SSE contract the web UI
/// used, so nothing on the server side changes.
class ChatController extends ChangeNotifier {
  ChatController(this.runtime, this.api, {ChatStreamer? streamer})
      : sse = streamer ?? SseChatClient(api);

  final RuntimeController runtime;
  final ApiClient api;
  final ChatStreamer sse;

  final List<ChatItem> messages = [];
  List<Map<String, dynamic>> _toolManifest = [];
  Future<List<Map<String, dynamic>>>? _manifestLoad;
  bool streaming = false;
  String runStatus = '';
  String? lastError;
  int _round = -1;
  ChatItem? _roundItem;

  String? get currentSessionId => runtime.state?.currentSessionId;

  Future<List<Map<String, dynamic>>> _loadToolManifest() {
    // One in-flight fetch shared by warmup and send; the result is cached so
    // the chat hot path never waits on the manifest twice.
    return _manifestLoad ??= _fetchToolManifest().whenComplete(() => _manifestLoad = null);
  }

  Future<List<Map<String, dynamic>>> _fetchToolManifest() async {
    if (_toolManifest.isNotEmpty) return _toolManifest;
    try {
      final data = await api.get('/api/v1/tools');
      final list = (data is Map && data['data'] is List) ? data['data'] as List : const [];
      final next = list.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
      if (next.isNotEmpty) _toolManifest = next;
      return next;
    } catch (_) {
      return _toolManifest;
    }
  }

  /// Pre-warms the tool manifest in the background (e.g. once the runtime is
  /// ready) so the first send does not wait on the control plane.
  Future<void> warmTools() async {
    await _loadToolManifest();
  }

  Future<void> send(String prompt, {String? imageDataUrl}) async {
    if (streaming) return;
    final text = prompt.trim();
    if (text.isEmpty && imageDataUrl == null) return;
    final sessionId = currentSessionId ?? '';
    lastError = null;
    messages.add(ChatItem(role: 'user', text: text, imageDataUrl: imageDataUrl));
    _round = -1;
    _roundItem = null;
    runStatus = '';
    streaming = true;
    runtime.chatInFlight = true;
    notifyListeners();

    final modelId = runtime.state?.modelId ?? '';
    final body = <String, dynamic>{
      'model': modelId,
      'stream': true,
      'stream_options': {'include_usage': true},
      'sessionId': sessionId,
      'messages': _requestMessages(text, imageDataUrl),
    };
    try {
      final tools = await _loadToolManifest();
      if (tools.isNotEmpty) {
        body['tools'] = tools;
        body['execute_tools'] = true;
      }

      final result = await sse.streamChat(
        body,
        onChunk: (chunk) {
          final item = _itemForRound();
          item.text += chunk;
          notifyListeners();
        },
        onReasoning: (chunk) {
          final item = _itemForRound();
          item.reasoning += chunk;
          notifyListeners();
        },
        onToolCall: (call) {
          final item = _itemForRound();
          final existing = item.tools.where((b) => b.name == call.name).toList();
          final bubble = existing.isEmpty
              ? ToolBubble(name: call.name, arguments: call.arguments, status: 'call')
              : existing.last;
          if (existing.isEmpty) item.tools.add(bubble);
          bubble.arguments = call.arguments;
          bubble.status = 'call';
          notifyListeners();
        },
        onPhase: (phase, tool, round) {
          if (phase == 'round') {
            _round = round;
            _itemForRound();
          }
          if (phase == 'tool_running') {
            runStatus = 'tool:$tool:$round';
            _round = round;
            _itemForRound();
          } else if (phase == 'round') {
            runStatus = 'round:$round';
          } else if (phase == 'retry') {
            runStatus = 'retry';
          } else if (phase == 'tool_done') {
            runStatus = '';
          }
          notifyListeners();
        },
      );
      if (!result.ok) {
        lastError = result.error;
        final assistant = _roundItem ?? _itemForRound();
        assistant.isError = true;
        assistant.text = result.error ?? '';
      } else if (result.text.isNotEmpty || result.reasoning.isNotEmpty) {
        final assistant = _roundItem ?? _itemForRound();
        if (assistant.text.isEmpty) assistant.text = result.text;
        if (result.reasoning.isNotEmpty && assistant.reasoning.isEmpty) {
          assistant.reasoning = result.reasoning;
        }
      }
    } catch (error) {
      lastError = error.toString();
      final last = messages.isNotEmpty ? messages.last : null;
      if (last != null && last.role == 'assistant') {
        last.isError = true;
        last.text = error.toString();
      }
    } finally {
      streaming = false;
      runStatus = '';
      runtime.chatInFlight = false;
      notifyListeners();
      unawaited(runtime.refresh());
    }
  }

  /// Returns the assistant [ChatItem] for the current round, creating a new
  /// one when a round boundary is crossed so multi-round agent runs render as
  /// separate blocks with their tool calls interleaved.
  ChatItem _itemForRound() {
    final round = _round < 0 ? 0 : _round;
    if (_roundItem != null && _roundItem!.round == round) return _roundItem!;
    final item = ChatItem(role: 'assistant', text: '');
    item.round = round;
    messages.add(item);
    _roundItem = item;
    _round = round;
    return item;
  }

  List<Map<String, dynamic>> _requestMessages(String text, String? imageDataUrl) {
    final content = <dynamic>[];
    if (text.trim().isNotEmpty) {
      content.add({'type': 'text', 'text': text});
    }
    if (imageDataUrl != null) {
      content.add({'type': 'image_url', 'image_url': {'url': imageDataUrl}});
    }
    return [
      {'role': 'user', 'content': content},
    ];
  }

  Future<void> loadSession(String sessionId) async {
    messages.clear();
    lastError = null;
    notifyListeners();
    try {
      final result = await runtime.sessionDetails(sessionId);
      final session = result['session'];
      if (session is Map) {
        final list = session['messages'];
        if (list is List) {
          for (final raw in list) {
            if (raw is! Map) continue;
            final map = raw as Map<String, dynamic>;
            final role = map['role']?.toString() ?? 'assistant';
            final text = map['text']?.toString() ?? '';
            if (role == 'tool') {
              final name = map['name']?.toString() ?? 'tool';
              final target = messages.isNotEmpty ? messages.last : null;
              if (target != null && target.role == 'assistant') {
                target.tools.add(ToolBubble(name: name, arguments: text, status: 'done'));
              }
            } else if (text.isNotEmpty) {
              final item = ChatItem(role: role, text: text);
              final contentJson = map['contentJson'];
              if (contentJson is Map) {
                final reasoning = contentJson['reasoning'];
                if (reasoning is String && reasoning.isNotEmpty) {
                  item.reasoning = reasoning;
                }
              }
              messages.add(item);
            }
          }
        }
      }
    } catch (error) {
      lastError = error.toString();
    }
    notifyListeners();
  }

  Future<void> newChat() async {
    await runtime.createSession();
    messages.clear();
    lastError = null;
    notifyListeners();
  }

  void clear() {
    messages.clear();
    lastError = null;
    notifyListeners();
  }

  String compactArguments(String raw) {
    final value = raw.trim();
    if (value.isEmpty) return '';
    try {
      return jsonEncode(jsonDecode(value));
    } catch (_) {
      return value;
    }
  }
}
