import 'package:flutter/material.dart';

import '../app.dart';
import '../l10n/app_localizations.dart';

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
                                for (final node in _nodes!)
                                  _NodeCard(node: node),
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
}

class _NodeCard extends StatelessWidget {
  const _NodeCard({required this.node});

  final Map<String, dynamic> node;

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
            Text(
              online ? l10n.nodesOnline : l10n.nodesOffline,
              style: theme.textTheme.labelMedium?.copyWith(
                color: online ? Colors.green : theme.colorScheme.outline,
              ),
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
