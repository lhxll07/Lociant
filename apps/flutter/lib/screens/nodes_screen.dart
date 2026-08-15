import 'package:flutter/material.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'baby_monitor_screen.dart';

/// Top-level "Nodes" page: this device plus every discovered Lociant peer
/// on the LAN. Peer models (with a `peer:` prefix) show up in the Models
/// page automatically.
class NodesScreen extends StatefulWidget {
  const NodesScreen({super.key});

  @override
  State<NodesScreen> createState() => _NodesScreenState();
}

class _NodesScreenState extends State<NodesScreen> {
  List<Map<String, dynamic>>? _nodes;
  String? _error;
  bool _loadedOnce = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_loadedOnce) {
      _loadedOnce = true;
      _load();
    }
  }

  Future<void> _load() async {
    final api = AppScope.of(context).runtime.api;
    try {
      final response = await api.get('/api/v1/nodes');
      if (!mounted) return;
      final payload = asMap(response);
      final nodes = asList(
        payload['nodes'],
      ).map(asMap).where((node) => node.isNotEmpty).toList(growable: false);
      setState(() {
        _nodes = nodes;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = error.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    return Scaffold(
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 8, 4),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.nodesTitle,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        l10n.nodesSubtitle,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.refresh),
                  tooltip: l10n.nodesRefresh,
                  onPressed: _load,
                ),
                IconButton(
                  icon: const Icon(Icons.add_link),
                  tooltip: l10n.nodesAdd,
                  onPressed: () => _showAddNodeDialog(l10n),
                ),
                IconButton(
                  icon: const Icon(Icons.help_outline),
                  tooltip: l10n.nodesHelp,
                  onPressed: () => _showGuideDialog(l10n),
                ),
              ],
            ),
          ),
          Expanded(
            child: _error != null
                ? _Message(text: l10n.nodesError(_error!), onRetry: _load)
                : _nodes == null
                ? const Center(child: CircularProgressIndicator())
                : _nodes!.isEmpty
                ? _NodesGuide(
                    onOpenSettings: () =>
                        homeShellKey.currentState?.switchTo(4),
                    onRefresh: _load,
                  )
                : RefreshIndicator(
                    onRefresh: _load,
                    child: CustomScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      slivers: [
                        SliverPadding(
                          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                          sliver: SliverToBoxAdapter(
                            child: Text(
                              l10n.nodesGridHint,
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                            ),
                          ),
                        ),
                        SliverPadding(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          sliver: SliverGrid(
                            delegate: SliverChildBuilderDelegate((
                              context,
                              index,
                            ) {
                              final node = _nodes![index];
                              return _NodeCard(
                                node: node,
                                onTap: () => _showNodeDetails(node),
                              );
                            }, childCount: _nodes!.length),
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                                  crossAxisCount: 2,
                                  mainAxisExtent: 188,
                                  crossAxisSpacing: 10,
                                  mainAxisSpacing: 10,
                                ),
                          ),
                        ),
                        SliverPadding(
                          padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
                          sliver: SliverToBoxAdapter(
                            child: Text(
                              l10n.nodesPeersHint,
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.outline,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  void _showGuideDialog(AppLocalizations l10n) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.nodesHelp),
        content: const SingleChildScrollView(child: _GuideHeader()),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(l10n.commonCancel),
          ),
          FilledButton(
            onPressed: () {
              Navigator.of(context).pop();
              homeShellKey.currentState?.switchTo(4);
            },
            child: Text(l10n.nodesGuideOpenSettings),
          ),
        ],
      ),
    );
  }

  Future<void> _showAddNodeDialog(AppLocalizations l10n) async {
    final api = AppScope.of(context).runtime.api;
    final host = TextEditingController();
    final port = TextEditingController(text: '11434');
    final name = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.nodesAdd),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: host,
              decoration: InputDecoration(labelText: l10n.nodesAddress),
              keyboardType: TextInputType.number,
            ),
            TextField(
              controller: port,
              decoration: const InputDecoration(labelText: 'Port'),
              keyboardType: TextInputType.number,
            ),
            TextField(
              controller: name,
              decoration: InputDecoration(labelText: l10n.nodesNameOptional),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(l10n.commonCancel),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop('add'),
            child: Text(l10n.nodesAdd),
          ),
        ],
      ),
    );
    if (result == 'add' && host.text.trim().isNotEmpty) {
      try {
        await api.post('/api/v1/peers', {
          'host': host.text.trim(),
          'port': int.tryParse(port.text) ?? 11434,
          if (name.text.trim().isNotEmpty) 'name': name.text.trim(),
        });
        _load();
      } catch (error) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('${l10n.nodesAddFailed}: $error')),
          );
        }
      }
    }
    host.dispose();
    port.dispose();
    name.dispose();
  }

  Future<void> _showNodeDetails(Map<String, dynamic> node) async {
    final isSelf = boolOf(node, 'self');
    final online = boolOf(node, 'online');
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (sheetContext) => _NodeDetailsSheet(
        node: node,
        onMonitor: isSelf || online
            ? () {
                Navigator.of(sheetContext).pop();
                _openBabyMonitor(node);
              }
            : null,
        onOpenModels: () {
          Navigator.of(sheetContext).pop();
          homeShellKey.currentState?.switchTo(1);
        },
        onDelete: isSelf
            ? null
            : () {
                Navigator.of(sheetContext).pop();
                _deleteNode(str(node, 'id'));
              },
      ),
    );
  }

  void _openBabyMonitor(Map<String, dynamic> node) {
    final isSelf = boolOf(node, 'self');
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => isSelf
            ? const BabyMonitorScreen()
            : BabyMonitorScreen(
                nodeId: str(node, 'id'),
                nodeName: str(node, 'name').isEmpty ? null : str(node, 'name'),
              ),
      ),
    );
  }

  Future<void> _deleteNode(String id) async {
    final api = AppScope.of(context).runtime.api;
    try {
      await api.delete('/api/v1/peers/$id');
      _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${AppLocalizations.of(context)!.nodesDeleteFailed}: $error',
            ),
          ),
        );
      }
    }
  }
}

class _NodeCard extends StatelessWidget {
  const _NodeCard({required this.node, required this.onTap});

  final Map<String, dynamic> node;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final isSelf = boolOf(node, 'self');
    final online = boolOf(node, 'online');
    final kind = _deviceKind(str(node, 'platform'));
    final name = str(node, 'name', str(node, 'id', '-'));
    final host = str(node, 'host', '-');
    final port = intOf(node, 'port');
    final platform = str(node, 'platform');
    final endpoint = port > 0 ? '$host:$port' : host;
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _DeviceIcon(kind: kind),
                  const Spacer(),
                  _StatusLabel(online: online),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 4),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      _deviceKindLabel(l10n, kind),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                  if (isSelf) _SelfLabel(text: l10n.nodesSelf),
                ],
              ),
              const Spacer(),
              Text(
                platform.isEmpty ? endpoint : platform,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
              const SizedBox(height: 2),
              Row(
                children: [
                  Icon(
                    Icons.lan_outlined,
                    size: 15,
                    color: theme.colorScheme.outline,
                  ),
                  const SizedBox(width: 5),
                  Expanded(
                    child: Text(
                      endpoint,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                  Icon(
                    Icons.chevron_right,
                    size: 19,
                    color: theme.colorScheme.outline,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NodeDetailsSheet extends StatelessWidget {
  const _NodeDetailsSheet({
    required this.node,
    required this.onOpenModels,
    this.onMonitor,
    this.onDelete,
  });

  final Map<String, dynamic> node;
  final VoidCallback onOpenModels;
  final VoidCallback? onMonitor;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final kind = _deviceKind(str(node, 'platform'));
    final isSelf = boolOf(node, 'self');
    final online = boolOf(node, 'online');
    final name = str(node, 'name', str(node, 'id', '-'));
    final host = str(node, 'host', '-');
    final port = intOf(node, 'port');
    final endpoint = port > 0 ? '$host:$port' : host;
    final platform = str(node, 'platform', l10n.nodesDeviceOther);
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                _DeviceIcon(kind: kind, large: true),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        _deviceKindLabel(l10n, kind),
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                _StatusLabel(online: online),
              ],
            ),
            const SizedBox(height: 18),
            _InfoRow(
              icon: Icons.lan_outlined,
              label: l10n.nodesAddress,
              value: endpoint,
            ),
            _InfoRow(
              icon: Icons.devices_other_outlined,
              label: l10n.nodesPlatform,
              value: platform,
            ),
            _InfoRow(
              icon: Icons.badge_outlined,
              label: l10n.nodesNodeId,
              value: str(node, 'id', '-'),
            ),
            const SizedBox(height: 18),
            Text(
              l10n.nodesAvailableActions,
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            if (onMonitor != null) ...[
              SizedBox(
                width: double.infinity,
                child: FilledButton.tonalIcon(
                  onPressed: onMonitor,
                  icon: const Icon(Icons.child_care_outlined),
                  label: Text(l10n.babyTitle),
                ),
              ),
              const SizedBox(height: 8),
            ],
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: onOpenModels,
                icon: const Icon(Icons.memory_outlined),
                label: Text(l10n.nodesOpenModels),
              ),
            ),
            if (onDelete != null) ...[
              const SizedBox(height: 8),
              TextButton.icon(
                onPressed: onDelete,
                icon: const Icon(Icons.delete_outline),
                label: Text(l10n.nodesDelete),
                style: TextButton.styleFrom(
                  foregroundColor: theme.colorScheme.error,
                ),
              ),
            ],
            const SizedBox(height: 10),
            Text(
              isSelf ? l10n.nodesSelfHint : l10n.nodesPeersHint,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.outline,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 19, color: theme.colorScheme.outline),
          const SizedBox(width: 10),
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          Expanded(
            child: SelectableText(
              value,
              maxLines: 2,
              style: theme.textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusLabel extends StatelessWidget {
  const _StatusLabel({required this.online});

  final bool online;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final color = online ? context.status.success : theme.colorScheme.outline;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 5),
        Text(
          online ? l10n.nodesOnline : l10n.nodesOffline,
          style: theme.textTheme.labelSmall?.copyWith(
            color: color,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}

class _SelfLabel extends StatelessWidget {
  const _SelfLabel({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: scheme.primaryContainer,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Text(
        text,
        style: Theme.of(
          context,
        ).textTheme.labelSmall?.copyWith(color: scheme.onPrimaryContainer),
      ),
    );
  }
}

class _DeviceIcon extends StatelessWidget {
  const _DeviceIcon({required this.kind, this.large = false});

  final _DeviceKind kind;
  final bool large;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final size = large ? 52.0 : 42.0;
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: scheme.primaryContainer,
        borderRadius: BorderRadius.circular(large ? 15 : 12),
      ),
      child: Icon(
        _deviceIcon(kind),
        size: large ? 27 : 22,
        color: scheme.primary,
      ),
    );
  }
}

enum _DeviceKind { phone, computer, board, other }

_DeviceKind _deviceKind(String platform) {
  final value = platform.toLowerCase();
  if (value.contains('android') ||
      value.contains('ios') ||
      value.contains('mobile')) {
    return _DeviceKind.phone;
  }
  if (value.contains('rk') ||
      value.contains('board') ||
      value.contains('embedded')) {
    return _DeviceKind.board;
  }
  if (value.contains('linux') ||
      value.contains('windows') ||
      value.contains('macos') ||
      value.contains('desktop')) {
    return _DeviceKind.computer;
  }
  return _DeviceKind.other;
}

IconData _deviceIcon(_DeviceKind kind) => switch (kind) {
  _DeviceKind.phone => Icons.smartphone_outlined,
  _DeviceKind.computer => Icons.computer_outlined,
  _DeviceKind.board => Icons.developer_board_outlined,
  _DeviceKind.other => Icons.devices_other_outlined,
};

String _deviceKindLabel(AppLocalizations l10n, _DeviceKind kind) =>
    switch (kind) {
      _DeviceKind.phone => l10n.nodesDevicePhone,
      _DeviceKind.computer => l10n.nodesDeviceComputer,
      _DeviceKind.board => l10n.nodesDeviceBoard,
      _DeviceKind.other => l10n.nodesDeviceOther,
    };

class _Message extends StatelessWidget {
  const _Message({required this.text, required this.onRetry});

  final String text;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(text, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: Text(AppLocalizations.of(context)!.nodesRefresh),
            ),
          ],
        ),
      ),
    );
  }
}

/// Beginner guide shown when no peers are visible: what interconnection is
/// for and the three steps to make devices find each other.
class _NodesGuide extends StatelessWidget {
  const _NodesGuide({required this.onOpenSettings, required this.onRefresh});

  final VoidCallback onOpenSettings;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _GuideHeader(),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: FilledButton.tonalIcon(
                        onPressed: onOpenSettings,
                        icon: const Icon(Icons.settings_outlined, size: 18),
                        label: Text(l10n.nodesGuideOpenSettings),
                      ),
                    ),
                    const SizedBox(width: 10),
                    OutlinedButton.icon(
                      onPressed: onRefresh,
                      icon: const Icon(Icons.refresh, size: 18),
                      label: Text(l10n.nodesRefresh),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// Title, explanation and numbered steps shared by the empty-state guide and
/// the help dialog.
class _GuideHeader extends StatelessWidget {
  const _GuideHeader();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final steps = [
      l10n.nodesGuideStep1,
      l10n.nodesGuideStep2,
      l10n.nodesGuideStep3,
    ];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(Icons.hub_outlined, color: theme.colorScheme.primary),
            const SizedBox(width: 8),
            Text(
              l10n.nodesGuideTitle,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          l10n.nodesGuideBody,
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 12),
        for (var i = 0; i < steps.length; i++) ...[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 11,
                backgroundColor: theme.colorScheme.primaryContainer,
                child: Text(
                  '${i + 1}',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onPrimaryContainer,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(top: 1),
                  child: Text(steps[i], style: theme.textTheme.bodyMedium),
                ),
              ),
            ],
          ),
          if (i < steps.length - 1) const SizedBox(height: 10),
        ],
      ],
    );
  }
}
