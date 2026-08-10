import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:url_launcher/url_launcher_string.dart';

import '../app.dart';

/// Markdown body with selectable text and external link handling.
class ChatMarkdown extends StatelessWidget {
  const ChatMarkdown({super.key, required this.data, this.isError = false});

  final String data;
  final bool isError;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return MarkdownBody(
      data: data,
      selectable: true,
      styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)).copyWith(
        p: TextStyle(
          fontSize: 15,
          height: 1.45,
          color: isError ? scheme.error : scheme.onSurface,
        ),
        code: TextStyle(
          fontSize: 13,
          color: scheme.onSurface,
          backgroundColor: scheme.surfaceContainerHighest,
        ),
        codeblockDecoration: BoxDecoration(
          color: scheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(10),
        ),
        blockquoteDecoration: BoxDecoration(
          color: scheme.surfaceContainerHigh,
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      onTapLink: (text, href, title) {
        final url = href ?? '';
        if (url.startsWith('http')) {
          AppScope.of(context).runtime.openExternalUrl(url);
        } else {
          launchUrlString(url, mode: LaunchMode.externalApplication);
        }
      },
    );
  }
}
