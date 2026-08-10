import 'dart:async';

import 'chat_streamer.dart';
import 'models.dart';

/// Canned multi-round agent run for standalone UI development. Exercises the
/// round-splitting and tool-card interleaving code paths without a backend.
class MockChatStreamer implements ChatStreamer {
  @override
  Future<ChatRunResult> streamChat(
    Map<String, dynamic> body, {
    required void Function(String text) onChunk,
    required void Function(String text) onReasoning,
    required void Function(ToolCallPart call) onToolCall,
    required void Function(String phase, String tool, int round) onPhase,
  }) async {
    Future<void> pause([int ms = 40]) => Future.delayed(Duration(milliseconds: ms));
    Future<void> emit(String text) async {
      for (final char in text.split('')) {
        onChunk(char);
        await pause(6);
      }
    }

    onPhase('round', '', 0);
    await pause();
    onReasoning('用户要求先查询运行状态，应该调用 runtime_status 工具。');
    await pause(120);
    await emit('我先看一下运行状态。');
    onToolCall(const ToolCallPart(key: '1', id: 'call_1', index: 0, name: 'runtime_status', arguments: '{}'));
    await pause();
    onPhase('tool_running', 'runtime_status', 0);
    await pause(120);
    onPhase('tool_done', '', 0);

    onPhase('round', '', 1);
    await pause();
    await emit('运行正常，再看一下模型列表。');
    onToolCall(const ToolCallPart(key: '2', id: 'call_2', index: 0, name: 'model_list', arguments: '{}'));
    await pause();
    onPhase('tool_running', 'model_list', 1);
    await pause(120);
    onPhase('tool_done', '', 1);

    onPhase('round', '', 2);
    await pause();
    await emit('总结：服务运行中，模型列表已获取。');

    return const ChatRunResult(
      text: '总结：服务运行中，模型列表已获取。',
      reasoning: '用户要求先查询运行状态，应该调用 runtime_status 工具。',
      toolCalls: [
        ToolCallPart(key: '1', id: 'call_1', index: 0, name: 'runtime_status', arguments: '{}'),
        ToolCallPart(key: '2', id: 'call_2', index: 0, name: 'model_list', arguments: '{}'),
      ],
    );
  }
}
