import 'package:flutter/material.dart';

import '../app.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import '../widgets/anchored_popup.dart';
import 'edge_overview_screen.dart';
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

  static const _pages = [
    EdgeOverviewScreen(),
    ModelsScreen(),
    NodesScreen(),
    SettingsScreen(),
  ];

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
                color: Theme.of(
                  context,
                ).colorScheme.outlineVariant.withValues(alpha: 0.35),
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
              const SizedBox(width: 8),
              Text(
                index == 0 ? l10n.edgeOverviewTitle : _title(l10n, index),
                style: const TextStyle(fontWeight: FontWeight.w700),
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

  String _title(AppLocalizations l10n, int index) => switch (index) {
    1 => l10n.modelsTitle,
    2 => l10n.nodesTitle,
    _ => l10n.settingsTitle,
  };
}

class _NavPopup extends StatelessWidget {
  const _NavPopup({required this.current, required this.onSelect});

  final int current;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final items = [
      (Icons.dashboard_outlined, l10n.navHome),
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
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 14,
                vertical: 2,
              ),
              leading: Icon(items[i].$1, size: 21),
              title: Text(items[i].$2, style: const TextStyle(fontSize: 14.5)),
              selected: i == current,
              selectedTileColor: Theme.of(
                context,
              ).colorScheme.primaryContainer.withValues(alpha: 0.35),
              onTap: () => onSelect(i),
            ),
        ],
      ),
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
    final color = running
        ? status.success
        : (starting ? status.warning : status.danger);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 7,
            height: 7,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 7),
          Text(text, style: const TextStyle(fontSize: 12)),
        ],
      ),
    );
  }
}
