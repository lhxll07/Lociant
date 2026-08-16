import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

class EdgeOverviewScreen extends StatefulWidget {
  const EdgeOverviewScreen({super.key});

  @override
  State<EdgeOverviewScreen> createState() => _EdgeOverviewScreenState();
}

class _EdgeOverviewScreenState extends State<EdgeOverviewScreen> {
  int _modelCount = 0;
  int _nodeCount = 0;
  List<Map<String, dynamic>> _nodes = const [];
  List<Map<String, dynamic>> _tools = const [];
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _refresh());
  }

  Future<void> _refresh() async {
    if (_loading || !mounted) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    final api = AppScope.of(context).runtime.api;
    try {
      final results = await Future.wait<dynamic>([
        api.get('/api/v1/models'),
        api.get('/api/v1/nodes'),
        api.get('/api/v1/tools'),
      ]);
      if (!mounted) return;
      final models = asMap(results[0]);
      final nodes = asMap(results[1]);
      final tools = asMap(results[2]);
      final nextNodes = asList(
        nodes['nodes'],
      ).map(asMap).where((node) => node.isNotEmpty).toList(growable: false);
      final nextTools = asList(
        tools['data'],
      ).map(asMap).where((tool) => tool.isNotEmpty).toList(growable: false);
      setState(() {
        _modelCount = asList(models['models']).length;
        _nodeCount = nextNodes.length;
        _nodes = nextNodes;
        _tools = nextTools;
      });
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final runtime = AppScope.of(context).runtime;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final state = runtime.state;
        final loopback = state?.url.isNotEmpty == true
            ? state!.url
            : 'http://127.0.0.1:${state?.port ?? 11434}';
        final lan = state?.lanUrl.isNotEmpty == true ? state!.lanUrl : loopback;
        return RefreshIndicator(
          onRefresh: () async {
            await runtime.refresh();
            await _refresh();
          },
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
            children: [
              Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 920),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _RuntimePanel(
                        state: state,
                        title: l10n.edgeOverviewTitle,
                        subtitle: l10n.edgeOverviewSubtitle,
                        onRefresh: _loading ? null : _refresh,
                      ),
                      const SizedBox(height: 12),
                      _StatsStrip(
                        modelCount: _modelCount,
                        nodeCount: _nodeCount,
                        toolCount: _tools.length,
                        onModels: () => _openTab(1),
                        onNodes: () => _openTab(2),
                      ),
                      const SizedBox(height: 26),
                      _SectionHeader(
                        icon: Icons.hub_outlined,
                        title: l10n.edgeNodesTitle,
                        action: TextButton.icon(
                          onPressed: () => _openTab(2),
                          icon: const Icon(
                            Icons.arrow_forward_outlined,
                            size: 17,
                          ),
                          label: Text(l10n.edgeViewAll),
                        ),
                      ),
                      const SizedBox(height: 10),
                      if (_nodes.isEmpty)
                        _InlineMessage(message: l10n.edgeNodesEmpty)
                      else
                        _OverviewNodeGrid(
                          nodes: _nodes,
                          onOpen: () => _openTab(2),
                        ),
                      const SizedBox(height: 26),
                      _SectionHeader(
                        icon: Icons.link_outlined,
                        title: l10n.edgeEndpointsTitle,
                      ),
                      const SizedBox(height: 10),
                      _EndpointPanel(
                        apiUrl: '$lan/api/v1',
                        mcpUrl: '$lan/mcp',
                        authHeader: runtime.api.authToken.isEmpty
                            ? l10n.connectionAuthDisabled
                            : l10n.connectionAuthHeaderValue(
                                runtime.api.authToken,
                              ),
                        authEnabled: runtime.api.authToken.isNotEmpty,
                        onCopy: _copy,
                      ),
                      const SizedBox(height: 26),
                      _SectionHeader(
                        icon: Icons.build_outlined,
                        title: l10n.edgeToolsTitle,
                        action: TextButton.icon(
                          onPressed: () => _openTab(3),
                          icon: const Icon(Icons.settings_outlined, size: 17),
                          label: Text(l10n.edgeOpenSettings),
                        ),
                      ),
                      const SizedBox(height: 10),
                      if (_error != null)
                        _InlineMessage(
                          message: _error!,
                          action: TextButton(
                            onPressed: _refresh,
                            child: Text(l10n.commonRefresh),
                          ),
                        )
                      else
                        _ToolsOverview(tools: _tools),
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _openTab(int index) => homeShellKey.currentState?.switchTo(index);

  Future<void> _copy(String value) async {
    await Clipboard.setData(ClipboardData(text: value));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppLocalizations.of(context)!.toastCopied)),
    );
  }
}

class _RuntimePanel extends StatelessWidget {
  const _RuntimePanel({
    required this.state,
    required this.title,
    required this.subtitle,
    required this.onRefresh,
  });

  final RuntimeUiState? state;
  final String title;
  final String subtitle;
  final VoidCallback? onRefresh;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _OverviewHeader(
              state: state,
              title: title,
              subtitle: subtitle,
              onRefresh: onRefresh,
            ),
            const SizedBox(height: 16),
            _RuntimeActions(state: state),
          ],
        ),
      ),
    );
  }
}

class _OverviewHeader extends StatelessWidget {
  const _OverviewHeader({
    required this.state,
    required this.title,
    required this.subtitle,
    required this.onRefresh,
  });

  final RuntimeUiState? state;
  final String title;
  final String subtitle;
  final VoidCallback? onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final active = state?.running == true;
    final starting = state?.starting == true;
    final l10n = AppLocalizations.of(context)!;
    final statusText = starting
        ? l10n.statusStarting
        : active
        ? l10n.statusRunning
        : l10n.statusStopped;
    final statusColor = starting
        ? context.status.warning
        : active
        ? context.status.success
        : context.status.danger;
    final message = state?.lastError.isNotEmpty == true
        ? state!.lastError
        : state?.message ?? '';
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 52,
          height: 52,
          decoration: BoxDecoration(
            color: statusColor.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: statusColor.withValues(alpha: 0.24)),
          ),
          child: Icon(Icons.router_outlined, color: statusColor, size: 25),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 10),
              Align(
                alignment: Alignment.centerLeft,
                child: _RuntimeStatusBadge(
                  text: statusText,
                  color: statusColor,
                ),
              ),
              if (message.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  message,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ],
          ),
        ),
        IconButton(
          tooltip: l10n.commonRefresh,
          icon: const Icon(Icons.refresh),
          onPressed: onRefresh,
          visualDensity: VisualDensity.compact,
        ),
      ],
    );
  }
}

class _RuntimeStatusBadge extends StatelessWidget {
  const _RuntimeStatusBadge({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
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
          const SizedBox(width: 6),
          Text(
            text,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: color,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _RuntimeActions extends StatelessWidget {
  const _RuntimeActions({required this.state});

  final RuntimeUiState? state;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final runtime = AppScope.of(context).runtime;
    final android = Theme.of(context).platform == TargetPlatform.android;
    final active = state?.running == true;
    final starting = state?.starting == true;
    final canStop = android && active && !starting;
    final canStart = !active && !starting;
    final enabled = canStop || canStart;
    return Row(
      children: [
        Expanded(
          child: FilledButton.icon(
            onPressed: enabled
                ? canStop
                      ? runtime.stopRuntime
                      : runtime.startRuntime
                : null,
            icon: Icon(
              canStop ? Icons.stop_outlined : Icons.play_arrow_outlined,
            ),
            label: Text(
              canStop
                  ? l10n.commonStop
                  : active
                  ? l10n.statusRunning
                  : l10n.commonStart,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: OutlinedButton.icon(
            onPressed: () => homeShellKey.currentState?.switchTo(3),
            icon: const Icon(Icons.tune_outlined),
            label: Text(l10n.navSettings),
          ),
        ),
      ],
    );
  }
}

class _StatsStrip extends StatelessWidget {
  const _StatsStrip({
    required this.modelCount,
    required this.nodeCount,
    required this.toolCount,
    required this.onModels,
    required this.onNodes,
  });

  final int modelCount;
  final int nodeCount;
  final int toolCount;
  final VoidCallback onModels;
  final VoidCallback onNodes;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: SizedBox(
        height: 88,
        child: Row(
          children: [
            Expanded(
              child: _StatItem(
                icon: Icons.memory_outlined,
                label: AppLocalizations.of(context)!.edgeMetricModels,
                value: '$modelCount',
                onTap: onModels,
              ),
            ),
            const _StatsDivider(),
            Expanded(
              child: _StatItem(
                icon: Icons.hub_outlined,
                label: AppLocalizations.of(context)!.edgeMetricNodes,
                value: '$nodeCount',
                onTap: onNodes,
              ),
            ),
            const _StatsDivider(),
            Expanded(
              child: _StatItem(
                icon: Icons.build_outlined,
                label: AppLocalizations.of(context)!.edgeMetricTools,
                value: '$toolCount',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatItem extends StatelessWidget {
  const _StatItem({
    required this.icon,
    required this.label,
    required this.value,
    this.onTap,
  });

  final IconData icon;
  final String label;
  final String value;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 3),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, size: 15, color: scheme.primary),
                const SizedBox(width: 5),
                Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _StatsDivider extends StatelessWidget {
  const _StatsDivider();

  @override
  Widget build(BuildContext context) {
    return VerticalDivider(
      width: 1,
      indent: 18,
      endIndent: 18,
      color: Theme.of(
        context,
      ).colorScheme.outlineVariant.withValues(alpha: 0.45),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.icon, required this.title, this.action});

  final IconData icon;
  final String title;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 30,
          height: 30,
          decoration: BoxDecoration(
            color: Theme.of(
              context,
            ).colorScheme.primaryContainer.withValues(alpha: 0.7),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(
            icon,
            size: 17,
            color: Theme.of(context).colorScheme.primary,
          ),
        ),
        const SizedBox(width: 9),
        Expanded(
          child: Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        ?action,
      ],
    );
  }
}

class _OverviewNodeGrid extends StatelessWidget {
  const _OverviewNodeGrid({required this.nodes, required this.onOpen});

  final List<Map<String, dynamic>> nodes;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final visible = nodes.take(2).toList(growable: false);
    return LayoutBuilder(
      builder: (context, constraints) {
        final gap = visible.length > 1 ? 10.0 : 0.0;
        final width = visible.length > 1
            ? (constraints.maxWidth - gap) / 2
            : constraints.maxWidth;
        return Wrap(
          spacing: gap,
          runSpacing: gap,
          children: [
            for (final node in visible)
              SizedBox(
                width: width,
                child: _OverviewNodeCard(node: node, onTap: onOpen),
              ),
          ],
        );
      },
    );
  }
}

class _OverviewNodeCard extends StatelessWidget {
  const _OverviewNodeCard({required this.node, required this.onTap});

  final Map<String, dynamic> node;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final name = str(node, 'name', str(node, 'id', '-'));
    final host = str(node, 'host', '-');
    final port = intOf(node, 'port');
    final online = boolOf(node, 'online');
    final isSelf = boolOf(node, 'self');
    final platform = str(node, 'platform');
    final endpoint = port > 0 ? '$host:$port' : host;
    final statusColor = online
        ? context.status.success
        : theme.colorScheme.outline;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          height: 154,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    _NodeIcon(platform: platform),
                    const Spacer(),
                    _RuntimeStatusBadge(
                      text: online ? l10n.nodesOnline : l10n.nodesOffline,
                      color: statusColor,
                    ),
                  ],
                ),
                const SizedBox(height: 11),
                SizedBox(
                  height: 36,
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  isSelf
                      ? l10n.nodesSelf
                      : platform.isEmpty
                      ? l10n.nodesDeviceOther
                      : platform,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const Spacer(),
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
                          fontWeight: FontWeight.w500,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                    Icon(
                      Icons.arrow_forward_outlined,
                      size: 17,
                      color: theme.colorScheme.outline,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NodeIcon extends StatelessWidget {
  const _NodeIcon({required this.platform});

  final String platform;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: 38,
      height: 38,
      decoration: BoxDecoration(
        color: scheme.primaryContainer.withValues(alpha: 0.72),
        borderRadius: BorderRadius.circular(11),
      ),
      child: Icon(
        _nodeIconForPlatform(platform),
        size: 20,
        color: scheme.primary,
      ),
    );
  }
}

IconData _nodeIconForPlatform(String platform) {
  final value = platform.toLowerCase();
  if (value.contains('android') ||
      value.contains('ios') ||
      value.contains('mobile')) {
    return Icons.smartphone_outlined;
  }
  if (value.contains('rk') ||
      value.contains('board') ||
      value.contains('embedded')) {
    return Icons.developer_board_outlined;
  }
  if (value.contains('linux') ||
      value.contains('windows') ||
      value.contains('macos') ||
      value.contains('desktop')) {
    return Icons.computer_outlined;
  }
  return Icons.devices_other_outlined;
}

class _EndpointPanel extends StatelessWidget {
  const _EndpointPanel({
    required this.apiUrl,
    required this.mcpUrl,
    required this.authHeader,
    required this.authEnabled,
    required this.onCopy,
  });

  final String apiUrl;
  final String mcpUrl;
  final String authHeader;
  final bool authEnabled;
  final Future<void> Function(String value) onCopy;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          _EndpointRow(
            icon: Icons.code_outlined,
            label: l10n.edgeControlApi,
            value: apiUrl,
            onCopy: () => onCopy(apiUrl),
          ),
          const Divider(height: 1),
          _EndpointRow(
            icon: Icons.hub_outlined,
            label: l10n.connectionMcpUrl,
            value: mcpUrl,
            onCopy: () => onCopy(mcpUrl),
          ),
          const Divider(height: 1),
          _EndpointRow(
            icon: Icons.key_outlined,
            label: l10n.connectionAuthHeader,
            value: authHeader,
            onCopy: authEnabled ? () => onCopy(authHeader) : null,
          ),
        ],
      ),
    );
  }
}

class _EndpointRow extends StatelessWidget {
  const _EndpointRow({
    required this.icon,
    required this.label,
    required this.value,
    this.onCopy,
  });

  final IconData icon;
  final String label;
  final String value;
  final VoidCallback? onCopy;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context)!;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: scheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, size: 17, color: scheme.onSurfaceVariant),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 4),
                SelectableText(
                  value,
                  maxLines: 2,
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontFamily: 'monospace',
                    color: scheme.onSurface,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 4),
          IconButton(
            tooltip: l10n.commonCopy,
            icon: const Icon(Icons.copy_outlined, size: 18),
            onPressed: onCopy,
            padding: EdgeInsets.zero,
            visualDensity: VisualDensity.compact,
            constraints: const BoxConstraints.tightFor(width: 36, height: 36),
          ),
        ],
      ),
    );
  }
}

class _ToolsOverview extends StatelessWidget {
  const _ToolsOverview({required this.tools});

  final List<Map<String, dynamic>> tools;

  @override
  Widget build(BuildContext context) {
    if (tools.isEmpty) {
      return _InlineMessage(
        message: AppLocalizations.of(context)!.edgeToolsEmpty,
      );
    }
    final l10n = AppLocalizations.of(context)!;
    final counts = {'read': 0, 'sensor': 0, 'action': 0};
    for (final tool in tools) {
      final exposure = tool['exposure']?.toString() ?? 'read';
      final key = counts.containsKey(exposure) ? exposure : 'read';
      counts[key] = counts[key]! + 1;
    }
    return Card(
      clipBehavior: Clip.antiAlias,
      child: SizedBox(
        height: 112,
        child: Row(
          children: [
            Expanded(
              child: _ToolScopeItem(
                icon: Icons.visibility_outlined,
                label: l10n.settingsToolRead,
                value: '${counts['read']}',
              ),
            ),
            const _StatsDivider(),
            Expanded(
              child: _ToolScopeItem(
                icon: Icons.sensors_outlined,
                label: l10n.settingsToolSensor,
                value: '${counts['sensor']}',
              ),
            ),
            const _StatsDivider(),
            Expanded(
              child: _ToolScopeItem(
                icon: Icons.bolt_outlined,
                label: l10n.settingsToolAction,
                value: '${counts['action']}',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ToolScopeItem extends StatelessWidget {
  const _ToolScopeItem({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: scheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, size: 17, color: scheme.primary),
          ),
          const SizedBox(height: 7),
          Text(
            value,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 1),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.labelSmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _InlineMessage extends StatelessWidget {
  const _InlineMessage({required this.message, this.action});

  final String message;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 18),
      child: Row(
        children: [
          Icon(
            Icons.info_outline,
            size: 19,
            color: Theme.of(context).colorScheme.outline,
          ),
          const SizedBox(width: 10),
          Expanded(child: Text(message)),
          ?action,
        ],
      ),
    );
  }
}
