import 'models.dart';

/// Abstraction over the chat transport so the UI can run against a mock
/// streamer during standalone UI development.
abstract class ChatStreamer {
  Future<ChatRunResult> streamChat(
    Map<String, dynamic> body, {
    required void Function(String text) onChunk,
    required void Function(String text) onReasoning,
    required void Function(ToolCallPart call) onToolCall,
    required void Function(String phase, String tool, int round) onPhase,
  });
}
