import 'dart:convert';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';
import '../state/chat_controller.dart';
import '../theme.dart';
import '../widgets/chat_markdown.dart';
import '../widgets/tool_bubble.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _inputController = TextEditingController();
  final _scrollController = ScrollController();
  String? _imageDataUrl;
  bool _loaded = false;

  @override
  void dispose() {
    _inputController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final chat = scope.chat;
    final runtime = scope.runtime;
    return ListenableBuilder(
      listenable: Listenable.merge([chat, runtime]),
      builder: (context, _) {
        // The runtime state arrives asynchronously through the platform
        // channel; only restore the last conversation once it is available.
        if (!_loaded && runtime.state != null) {
          _loaded = true;
          WidgetsBinding.instance.addPostFrameCallback((_) => _restoreConversation());
        }
        final state = runtime.state;
        final sessionTitle = _sessionTitle(state);
        return Column(
          children: [
            _ChatContextHeader(
              title: sessionTitle,
              model: state?.modelId ?? '--',
              state: state,
            ),
            Expanded(
              child: Column(
                children: [
                  Expanded(
                    child: ListView(
                      controller: _scrollController,
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      children: [
                        if (chat.messages.isEmpty && !chat.streaming && chat.lastError == null)
                          const _EmptyChatHint()
                        else
                          ..._buildMessages(chat, context),
                      ],
                    ),
                  ),
                  if (chat.streaming || chat.runStatus.isNotEmpty) _RunStatus(chat: chat),
                  _AttachmentPreview(
                    imageDataUrl: _imageDataUrl,
                    onRemove: () => setState(() => _imageDataUrl = null),
                  ),
                  _Composer(
                    controller: _inputController,
                    enabled: !chat.streaming,
                    onAttach: _attachImage,
                    onSend: _send,
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  String _sessionTitle(RuntimeUiState? state) {
    final sessions = state?.sessions ?? const <SessionSummary>[];
    if (sessions.isEmpty) return AppLocalizations.of(context)!.homeNewChat;
    final current = state?.currentSessionId ?? '';
    SessionSummary? found;
    for (final session in sessions) {
      if (session.id == current) {
        found = session;
        break;
      }
    }
    return found?.title ?? (sessions.isNotEmpty ? sessions.first.title : AppLocalizations.of(context)!.homeNewChat);
  }

  List<Widget> _buildMessages(ChatController chat, BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final widgets = <Widget>[];
    for (final item in chat.messages) {
      final isAssistant = item.role == 'assistant';
      final displayText = isAssistant && item.text.trim().isEmpty
          ? (chat.streaming && item.reasoning.isNotEmpty
              ? ''
              : (item.isError
                  ? l10n.errorApiRequest
                  : (item.tools.isNotEmpty ? l10n.homeToolRunDone : l10n.homeEmptyReply)))
          : item.text;
      widgets.add(
        Align(
          alignment: isAssistant ? Alignment.centerLeft : Alignment.centerRight,
          child: Container(
            constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.86),
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
            decoration: BoxDecoration(
              color: isAssistant
                  ? Theme.of(context).colorScheme.surfaceContainerHigh
                  : Theme.of(context).colorScheme.primaryContainer,
              borderRadius: BorderRadius.circular(14),
            ),
            child: isAssistant
                ? Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (item.round > 0 || item.tools.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 4),
                          child: Text(
                            l10n.homeRoundLabel(item.round),
                            style: TextStyle(
                              fontSize: 10.5,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 0.5,
                              color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.85),
                            ),
                          ),
                        ),
                      if (item.reasoning.isNotEmpty ||
                          (chat.streaming && item.text.trim().isEmpty && !item.isError))
                        _ReasoningView(
                          reasoning: item.reasoning,
                          thinking: chat.streaming && item.text.trim().isEmpty,
                          expanded: item.reasoningExpanded,
                          onToggle: () =>
                              setState(() => item.reasoningExpanded = !item.reasoningExpanded),
                        ),
                      ChatMarkdown(data: displayText, isError: item.isError),
                      if (item.tools.isNotEmpty)
                        ...item.tools.map((bubble) => ToolBubbleView(bubble: bubble)),
                    ],
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      if (item.imageDataUrl != null)
                        ClipRRect(
                          borderRadius: BorderRadius.circular(10),
                          child: Image.memory(base64Decode(_dataUrlBase64(item.imageDataUrl!)), width: 180),
                        ),
                      if (item.text.isNotEmpty)
                        SelectableText(item.text)
                      else if (item.imageDataUrl != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            l10n.homeImageAttached,
                            style: TextStyle(
                              fontSize: 11,
                              color: Theme.of(context).colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                    ],
                  ),
          ),
        ),
      );
    }
    return widgets;
  }

  String _dataUrlBase64(String dataUrl) => dataUrl.substringAfter('base64,', dataUrl);

  Future<void> _attachImage() async {
    try {
      final XFile? file;
      if (!kIsWeb &&
          (defaultTargetPlatform == TargetPlatform.android ||
              defaultTargetPlatform == TargetPlatform.iOS)) {
        final picker = ImagePicker();
        file = await picker.pickImage(
          source: ImageSource.gallery,
          maxWidth: 1440,
          imageQuality: 82,
        );
      } else {
        file = await openFile(acceptedTypeGroups: const [
          XTypeGroup(
            label: 'images',
            extensions: ['jpg', 'jpeg', 'png', 'webp', 'gif'],
          ),
        ]);
      }
      if (file == null) return;
      final bytes = await file.readAsBytes();
      if (!mounted) return;
      setState(() => _imageDataUrl = 'data:image/jpeg;base64,${base64Encode(bytes)}');
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppLocalizations.of(context)!.toastImagePickerUnavailable)),
      );
    }
  }

  void _send() {
    final text = _inputController.text;
    final image = _imageDataUrl;
    if (text.trim().isEmpty && image == null) return;
    _inputController.clear();
    setState(() => _imageDataUrl = null);
    AppScope.of(context).chat.send(text, imageDataUrl: image).then((_) {
      _scrollToBottom();
    });
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _restoreConversation() async {
    final scope = AppScope.of(context);
    final state = scope.runtime.state;
    final sessions = state?.sessions ?? const <SessionSummary>[];
    if (sessions.isEmpty) {
      scope.chat.clear();
      return;
    }
    final current = state?.currentSessionId ?? '';
    var withMessages = false;
    for (final session in sessions) {
      if (session.id == current) {
        withMessages = true;
        break;
      }
    }
    String target;
    if (withMessages) {
      target = current;
    } else {
      String? firstWithMessages;
      for (final session in sessions) {
        if (session.messageCount > 0) {
          firstWithMessages = session.id;
          break;
        }
      }
      target = firstWithMessages ?? sessions.first.id;
    }
    await scope.chat.loadSession(target);
  }
}

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.enabled,
    required this.onAttach,
    required this.onSend,
  });

  final TextEditingController controller;
  final bool enabled;
  final VoidCallback onAttach;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 6, 12, 12),
      child: Row(
        children: [
          IconButton(
            tooltip: l10n.homeUploadImage,
            icon: const Icon(Icons.image_outlined),
            onPressed: enabled ? onAttach : null,
          ),
          Expanded(
            child: TextField(
              controller: controller,
              enabled: enabled,
              minLines: 1,
              maxLines: 4,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => onSend(),
              decoration: InputDecoration(hintText: l10n.homePlaceholder, isDense: true),
            ),
          ),
          const SizedBox(width: 8),
          FilledButton(
            onPressed: enabled ? onSend : null,
            child: Text(l10n.homeSend),
          ),
        ],
      ),
    );
  }
}

class _AttachmentPreview extends StatelessWidget {
  const _AttachmentPreview({required this.imageDataUrl, required this.onRemove});

  final String? imageDataUrl;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final data = imageDataUrl;
    if (data == null) return const SizedBox.shrink();
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 12),
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Image.memory(
              base64Decode(data.substringAfter('base64,', data)),
              width: 48,
              height: 48,
              fit: BoxFit.cover,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              AppLocalizations.of(context)!.homeImageAttached,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 18),
            onPressed: onRemove,
          ),
        ],
      ),
    );
  }
}

class _RunStatus extends StatelessWidget {
  const _RunStatus({required this.chat});

  final ChatController chat;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    String text;
    if (chat.runStatus.startsWith('tool:')) {
      final parts = chat.runStatus.split(':');
      text = l10n.homeRunStatusTool(parts[1], parts.length > 2 ? int.tryParse(parts[2]) ?? 0 : 0);
    } else if (chat.runStatus.startsWith('round:')) {
      text = l10n.homeRunStatusRound(int.tryParse(chat.runStatus.substring(6)) ?? 0);
    } else if (chat.runStatus == 'retry') {
      text = l10n.homeRunStatusRetry;
    } else {
      text = l10n.homeThinking;
    }
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          const SizedBox(
            width: 12,
            height: 12,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(text, style: Theme.of(context).textTheme.bodySmall),
          ),
        ],
      ),
    );
  }
}

class _ReasoningView extends StatelessWidget {
  const _ReasoningView({
    required this.reasoning,
    required this.thinking,
    required this.expanded,
    required this.onToggle,
  });

  final String reasoning;
  final bool thinking;
  final bool expanded;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        InkWell(
          onTap: onToggle,
          borderRadius: BorderRadius.circular(8),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (thinking)
                  const SizedBox(
                    width: 11,
                    height: 11,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                else
                  Icon(Icons.auto_awesome, size: 13, color: scheme.primary),
                const SizedBox(width: 6),
                Text(
                  thinking ? l10n.homeThinking : l10n.homeThought,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: scheme.primary,
                  ),
                ),
                const SizedBox(width: 4),
                Icon(
                  expanded ? Icons.expand_less : Icons.expand_more,
                  size: 16,
                  color: scheme.onSurfaceVariant,
                ),
              ],
            ),
          ),
        ),
        if (expanded)
          Container(
            width: double.infinity,
            margin: const EdgeInsets.only(bottom: 6),
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(10),
            ),
            child: SelectableText(
              reasoning,
              style: TextStyle(fontSize: 12.5, height: 1.45, color: scheme.onSurfaceVariant),
            ),
          ),
      ],
    );
  }
}

class _ChatContextHeader extends StatelessWidget {
  const _ChatContextHeader({required this.title, required this.model, required this.state});

  final String title;
  final String model;
  final RuntimeUiState? state;

  @override
  Widget build(BuildContext context) {
    final running = state?.running ?? false;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).colorScheme.outlineVariant.withValues(alpha: 0.35),
          ),
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                Text(model, style: TextStyle(fontSize: 11, color: Theme.of(context).colorScheme.onSurfaceVariant)),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
              color: running
                  ? context.status.success.withValues(alpha: 0.14)
                  : Theme.of(context).colorScheme.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              running ? AppLocalizations.of(context)!.statusRunning : AppLocalizations.of(context)!.statusStopped,
              style: TextStyle(
                fontSize: 11.5,
                color: running ? context.status.success : Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyChatHint extends StatelessWidget {
  const _EmptyChatHint();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 80),
      child: Column(
        children: [
          Icon(Icons.bolt, size: 42, color: Theme.of(context).colorScheme.primary),
          const SizedBox(height: 10),
          Text(AppLocalizations.of(context)!.homePlaceholder, textAlign: TextAlign.center),
        ],
      ),
    );
  }
}

extension _StringSplit on String {
  String substringAfter(String needle, String fallback) {
    final index = indexOf(needle);
    return index < 0 ? fallback : substring(index + needle.length);
  }
}
