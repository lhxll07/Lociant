import 'package:flutter/material.dart';

import '../app.dart';
import '../l10n/app_localizations.dart';
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

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = AppScope.of(context).runtime.api;
    try {
      final response = await api.get('/api/v1/nodes');
      if (!mounted) return;
      setState(() {
        _nodes = (response['nodes'] as List? ?? const [])
            .whereType<Map<String, dynamic>>()
            .toList();
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
                        style: theme.textTheme.titleLarge
                            ?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      Text(
                        l10n.nodesSubtitle,
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
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
              ],
            ),
          ),
          Expanded(
            child: _error != null
                ? _Message(text: l10n.nodesError(_error!), onRetry: _load)
                : _nodes == null
                    ? const Center(child: CircularProgressIndicator())
                    : _nodes!.isEmpty
                        ? _Message(text: l10n.nodesEmpty, onRetry: _load)
                        : RefreshIndicator(
                            onRefresh: _load,
                            child: ListView(
                              padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
                              children: [
                                Card(
                                  child: ListTile(
                                    leading: const Icon(Icons.child_care),
                                    title: Text(
                                      l10n.babyTitle,
                                      style: const TextStyle(fontWeight: FontWeight.w600),
                                    ),
                                    subtitle: Text(l10n.babySubtitle),
                                    trailing: const Icon(Icons.chevron_right),
                                    onTap: () => Navigator.of(context).push(
                                      MaterialPageRoute(
                                        builder: (_) => const BabyMonitorScreen(),
                                      ),
                                    ),
                                  ),
                                ),
                                const SizedBox(height: 10),
                                for (final node in _nodes!)
                                  _NodeCard(
                                    node: node,
                                    onMonitor: node['self'] == true
                                        ? null
                                        : () => Navigator.of(context).push(
                                            MaterialPageRoute(
                                              builder: (_) => BabyMonitorScreen(
                                                nodeId: node['id'] as String,
                                              ),
                                            ),
                                          ),
                                    onDelete: node['self'] == true
                                        ? null
                                        : () => _deleteNode(node['id'] as String),
                                    onChat: node['self'] == true
                                        ? null
                                        : () => _showChatModelPicker(node['id'] as String),
                                  ),
                                const SizedBox(height: 8),
                                Padding(
                                  padding: const EdgeInsets.symmetric(horizontal: 4),
                                  child: Text(
                                    l10n.nodesPeersHint,
                                    style: theme.textTheme.bodySmall
                                        ?.copyWith(color: theme.colorScheme.outline),
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
              decoration: const InputDecoration(labelText: 'IP'),
              keyboardType: TextInputType.number,
            ),
            TextField(
              controller: port,
              decoration: const InputDecoration(labelText: 'Port'),
              keyboardType: TextInputType.number,
            ),
            TextField(
              controller: name,
              decoration: const InputDecoration(labelText: 'Name (optional)'),
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
  }

  Future<void> _deleteNode(String id) async {
    final api = AppScope.of(context).runtime.api;
    try {
      await api.delete('/api/v1/peers/$id');
      _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${AppLocalizations.of(context)!.nodesDeleteFailed}: $error')),
        );
      }
    }
  }

  Future<void> _showChatModelPicker(String nodeId) async {
    final api = AppScope.of(context).runtime.api;
    List<Map<String, dynamic>> peerModels;
    try {
      final response = await api.get('/api/v1/models');
      peerModels = (response['models'] as List? ?? const [])
          .whereType<Map<String, dynamic>>()
          .where((m) => (m['id'] as String? ?? '').startsWith('peer:$nodeId:'))
          .toList();
    } catch (_) {
      peerModels = const [];
    }
    if (peerModels.isEmpty || !mounted) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context)!.nodesChatNoModel)),
        );
      }
      return;
    }
    final selected = await showModalBottomSheet<String>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(12),
              child: Text(
                AppLocalizations.of(context)!.nodesChatSelectModel,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
            for (final model in peerModels)
              ListTile(
                title: Text(model['name'] as String? ?? model['id'] as String),
                onTap: () => Navigator.of(context).pop(model['id'] as String),
              ),
          ],
        ),
      ),
    );
    if (selected != null && mounted) {
      await AppScope.of(context).runtime.updateSettings({'modelId': selected});
      homeShellKey.currentState?.switchTo(0);
    }
  }
}

class _NodeCard extends StatelessWidget {
  const _NodeCard({
    required this.node,
    required this.onMonitor,
    this.onDelete,
    this.onChat,
  });

  final Map<String, dynamic> node;
  final VoidCallback? onMonitor;
  final VoidCallback? onDelete;
  final VoidCallback? onChat;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final isSelf = node['self'] == true;
    final online = node['online'] == true;
    final host = node['host'] as String? ?? '-';
    final port = node['port'] as int? ?? 0;
    final platform = node['platform'] as String? ?? '';
    final name = node['name'] as String? ?? node['id'] as String? ?? '-';
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: online ? Colors.green : theme.colorScheme.outlineVariant,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Flexible(
                        child: Text(
                          name,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      if (isSelf) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                          decoration: BoxDecoration(
                            color: theme.colorScheme.primaryContainer,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            l10n.nodesSelf,
                            style: theme.textTheme.labelSmall
                                ?.copyWith(color: theme.colorScheme.onPrimaryContainer),
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    '$host:$port${platform.isEmpty ? '' : '  ·  $platform'}',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  online ? l10n.nodesOnline : l10n.nodesOffline,
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: online ? Colors.green : theme.colorScheme.outline,
                  ),
                ),
                if (onMonitor != null)
                  TextButton.icon(
                    onPressed: onMonitor,
                    icon: const Icon(Icons.child_care, size: 16),
                    label: Text(l10n.babyTitle, style: const TextStyle(fontSize: 12)),
                  ),
                if (onDelete != null)
                  IconButton(
                    icon: const Icon(Icons.delete_outline, size: 18),
                    tooltip: l10n.nodesDelete,
                    onPressed: onDelete,
                    visualDensity: VisualDensity.compact,
                  ),
                if (onChat != null)
                  IconButton(
                    icon: const Icon(Icons.chat_bubble_outline, size: 18),
                    tooltip: l10n.nodesChat,
                    onPressed: onChat,
                    visualDensity: VisualDensity.compact,
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

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
