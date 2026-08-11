import 'package:flutter/material.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import '../widgets/anchored_popup.dart';
import 'home_screen.dart';
import 'models_screen.dart';
import 'nodes_screen.dart';
import 'settings_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => HomeShellState();
}

class HomeShellState extends State<HomeShell> {
  int _index = 0;

  static const _pages = [HomeScreen(), ModelsScreen(), NodesScreen(), SettingsScreen()];

  /// Switches the top-level tab (used by other screens, e.g. Nodes -> Home).
  void switchTo(int index) {
    if (index < 0 || index >= _pages.length) return;
    setState(() => _index = index);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _TopBar(
              index: _index,
              onMenuSelect: (index) => setState(() => _index = index),
            ),
            Expanded(child: _pages[_index]),
          ],
        ),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({required this.index, required this.onMenuSelect});

  final int index;
  final ValueChanged<int> onMenuSelect;

  @override
  Widget build(BuildContext context) {
    final runtime = AppScope.of(context).runtime;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final l10n = AppLocalizations.of(context)!;
        final state = runtime.state;
        return Container(
          height: 58,
          padding: const EdgeInsets.symmetric(horizontal: 8),
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
              AnchoredOverlay(
                popupWidth: 220,
                builder: (context, toggle) => IconButton(
                  icon: const Icon(Icons.menu),
                  tooltip: l10n.navMenu,
                  onPressed: toggle,
                ),
                popupBuilder: (context, close) => _NavPopup(
                  current: index,
                  onSelect: (value) {
                    close();
                    onMenuSelect(value);
                  },
                ),
              ),
              if (index == 0)
                AnchoredOverlay(
                  popupWidth: 344,
                  maxHeight: 500,
                  builder: (context, toggle) => InkWell(
                    onTap: toggle,
                    customBorder: const StadiumBorder(),
                    child: _HistoryButton(count: state?.sessions.length ?? 0),
                  ),
                  popupBuilder: (context, close) => _SessionsPopup(close: close),
                ),
              const Spacer(),
              _StatusPill(
                running: state?.running ?? false,
                starting: state?.starting ?? false,
              ),
              const SizedBox(width: 10),
            ],
          ),
        );
      },
    );
  }
}

class _NavPopup extends StatelessWidget {
  const _NavPopup({required this.current, required this.onSelect});

  final int current;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final items = [
      (Icons.chat_bubble_outline, l10n.navHome),
      (Icons.memory_outlined, l10n.navModels),
      (Icons.hub_outlined, l10n.navNodes),
      (Icons.settings_outlined, l10n.navSettings),
    ];
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 6),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 0; i < items.length; i++)
            ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
              leading: Icon(items[i].$1, size: 21),
              title: Text(items[i].$2, style: const TextStyle(fontSize: 14.5)),
              selected: i == current,
              selectedTileColor: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.35),
              onTap: () => onSelect(i),
            ),
        ],
      ),
    );
  }
}

class _HistoryButton extends StatelessWidget {
  const _HistoryButton({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Container(
      height: 38,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.history, size: 16),
          const SizedBox(width: 6),
          Text(l10n.homeHistory, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
          const SizedBox(width: 6),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text('$count', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
          ),
        ],
      ),
    );
  }
}

class _SessionsPopup extends StatelessWidget {
  const _SessionsPopup({required this.close});

  final VoidCallback close;

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final runtime = scope.runtime;
    final chat = scope.chat;
    final l10n = AppLocalizations.of(context)!;
    final sessions = runtime.state?.sessions ?? const <SessionSummary>[];
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 10, 8),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  l10n.homeHistory,
                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w800),
                ),
              ),
              TextButton.icon(
                onPressed: chat.streaming
                    ? null
                    : () async {
                        await chat.newChat();
                        close();
                      },
                icon: const Icon(Icons.add, size: 16),
                label: Text(l10n.homeNewChat, style: const TextStyle(fontSize: 12)),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
        Flexible(
          child: sessions.isEmpty
              ? const Padding(padding: EdgeInsets.all(24), child: Text('--'))
              : ListView.builder(
                  shrinkWrap: true,
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  itemCount: sessions.length,
                  itemBuilder: (context, index) {
                    final session = sessions[index];
                    final active = session.id == (runtime.state?.currentSessionId ?? '');
                    return ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 2),
                      selected: active,
                      title: Text(
                        session.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
                      ),
                      subtitle: Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(
                          '${session.modelId} · ${session.messageCount}',
                          style: TextStyle(
                            fontSize: 11.5,
                            color: Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ),
                      trailing: Padding(
                        padding: const EdgeInsets.only(left: 4),
                        child: IconButton(
                          icon: const Icon(Icons.delete_outline, size: 20),
                          tooltip: l10n.homeDeleteChat,
                          visualDensity: VisualDensity.compact,
                          onPressed: chat.streaming ? null : () => runtime.deleteSession(session.id),
                        ),
                      ),
                      onTap: chat.streaming
                          ? null
                          : () async {
                              await runtime.selectSession(session.id);
                              await chat.loadSession(session.id);
                              close();
                            },
                    );
                  },
                ),
        ),
      ],
    );
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.running, required this.starting});

  final bool running;
  final bool starting;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final text = starting
        ? l10n.statusStarting
        : running
            ? l10n.statusRunning
            : l10n.statusStopped;
    final status = context.status;
    final color = running ? status.success : (starting ? status.warning : status.danger);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(width: 7, height: 7, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
          const SizedBox(width: 7),
          Text(text, style: const TextStyle(fontSize: 12)),
        ],
      ),
    );
  }
}
