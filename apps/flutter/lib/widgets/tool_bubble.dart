import 'package:flutter/material.dart';

import '../state/chat_controller.dart';
import '../theme.dart';

/// Compact tool-call bubble shown inside the assistant run.
class ToolBubbleView extends StatelessWidget {
  const ToolBubbleView({super.key, required this.bubble});

  final ToolBubble bubble;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final status = context.status;
    final done = bubble.status == 'done' || bubble.status == 'completed';
    final error = bubble.status == 'error' || bubble.status == 'failed';
    final color = error ? status.danger : (done ? status.success : scheme.primary);
    return Container(
      margin: const EdgeInsets.only(top: 6, bottom: 2),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withValues(alpha: 0.45)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.extension_outlined, size: 15, color: color),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  bubble.name,
                  style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: scheme.onSurface),
                ),
                if (bubble.arguments.trim().isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      bubble.arguments,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 11, color: scheme.onSurfaceVariant, fontFamily: 'monospace'),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Icon(
            error ? Icons.error_outline : (done ? Icons.check_circle_outline : Icons.more_horiz),
            size: 15,
            color: color,
          ),
        ],
      ),
    );
  }
}
