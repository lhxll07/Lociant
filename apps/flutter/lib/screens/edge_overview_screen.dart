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
              _RuntimePanel(
                state: state,
                title: l10n.edgeOverviewTitle,
                subtitle: l10n.edgeOverviewSubtitle,
                onRefresh: _loading ? null : _refresh,
              ),
              const SizedBox(height: 14),
              _MetricGrid(
                modelCount: _modelCount,
                nodeCount: _nodeCount,
                toolCount: _tools.length,
                state: state,
                l10n: l10n,
                onModels: () => _openTab(1),
                onNodes: () => _openTab(2),
              ),
              const SizedBox(height: 24),
              _SectionTitle(
                title: l10n.edgeNodesTitle,
                action: TextButton.icon(
                  onPressed: () => _openTab(2),
                  icon: const Icon(Icons.arrow_forward_outlined, size: 17),
                  label: Text(l10n.edgeViewAll),
                ),
              ),
              const SizedBox(height: 4),
              if (_nodes.isEmpty)
                _InlineMessage(message: l10n.edgeNodesEmpty)
              else
                _NodeSummary(nodes: _nodes, onOpen: () => _openTab(2)),
              const SizedBox(height: 22),
              _SectionTitle(
                title: l10n.edgeEndpointsTitle,
                action: IconButton(
                  tooltip: l10n.commonCopy,
                  icon: const Icon(Icons.copy_outlined, size: 19),
                  onPressed: () => _copy('$lan/api/v1'),
                ),
              ),
              _EndpointRow(label: l10n.edgeControlApi, value: '$lan/api/v1'),
              _EndpointRow(label: l10n.connectionMcpUrl, value: '$lan/mcp'),
              _EndpointRow(
                label: l10n.connectionAuthHeader,
                value: runtime.api.authToken.isEmpty
                    ? l10n.connectionAuthDisabled
                    : l10n.connectionAuthHeaderValue(runtime.api.authToken),
              ),
              const SizedBox(height: 24),
              _SectionTitle(
                title: l10n.edgeToolsTitle,
                action: TextButton.icon(
                  onPressed: () => _openTab(3),
                  icon: const Icon(Icons.settings_outlined, size: 17),
                  label: Text(l10n.edgeOpenSettings),
                ),
              ),
              if (_error != null)
                _InlineMessage(
                  message: _error!,
                  action: TextButton(
                    onPressed: _refresh,
                    child: Text(l10n.commonRefresh),
                  ),
                )
              else if (_tools.isEmpty)
                _InlineMessage(message: l10n.edgeToolsEmpty)
              else
                _ToolsList(tools: _tools),
              const SizedBox(height: 20),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => _openTab(1),
                      icon: const Icon(Icons.memory_outlined),
                      label: Text(l10n.edgeOpenModels),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => _openTab(2),
                      icon: const Icon(Icons.hub_outlined),
                      label: Text(l10n.edgeOpenNodes),
                    ),
                  ),
                ],
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
      child: Padding(
        padding: const EdgeInsets.all(16),
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
    final colorScheme = Theme.of(context).colorScheme;
    final active = state?.running == true;
    final starting = state?.starting == true;
    final statusText = starting
        ? AppLocalizations.of(context)!.statusStarting
        : active
        ? AppLocalizations.of(context)!.statusRunning
        : AppLocalizations.of(context)!.statusStopped;
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
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: colorScheme.primaryContainer,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Icon(Icons.router_outlined, color: colorScheme.primary),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 3),
              Text(
                subtitle,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: statusColor,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 7),
                  Text(
                    statusText,
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: statusColor,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  if (message.isNotEmpty) ...[
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        message,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
        IconButton(
          tooltip: AppLocalizations.of(context)!.commonRefresh,
          icon: const Icon(Icons.refresh),
          onPressed: onRefresh,
          visualDensity: VisualDensity.compact,
        ),
      ],
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
            onPressed: () => homeShellKey.currentState?.switchTo(4),
            icon: const Icon(Icons.tune_outlined),
            label: Text(l10n.navSettings),
          ),
        ),
      ],
    );
  }
}

class _MetricGrid extends StatelessWidget {
  const _MetricGrid({
    required this.modelCount,
    required this.nodeCount,
    required this.toolCount,
    required this.state,
    required this.l10n,
    required this.onModels,
    required this.onNodes,
  });

  final int modelCount;
  final int nodeCount;
  final int toolCount;
  final RuntimeUiState? state;
  final AppLocalizations l10n;
  final VoidCallback onModels;
  final VoidCallback onNodes;

  @override
  Widget build(BuildContext context) {
    final status = state?.starting == true
        ? l10n.statusStarting
        : state?.running == true
        ? l10n.statusRunning
        : l10n.statusStopped;
    return GridView(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisExtent: 78,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
      ),
      children: [
        _MetricTile(
          icon: Icons.memory_outlined,
          label: l10n.edgeMetricModels,
          value: '$modelCount',
          onTap: onModels,
        ),
        _MetricTile(
          icon: Icons.hub_outlined,
          label: l10n.edgeMetricNodes,
          value: '$nodeCount',
          onTap: onNodes,
        ),
        _MetricTile(
          icon: Icons.build_outlined,
          label: l10n.edgeMetricTools,
          value: '$toolCount',
        ),
        _MetricTile(
          icon: Icons.power_settings_new_outlined,
          label: l10n.edgeMetricStatus,
          value: status,
        ),
      ],
    );
  }
}

class _MetricTile extends StatelessWidget {
  const _MetricTile({
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
    final colorScheme = Theme.of(context).colorScheme;
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Icon(icon, size: 19, color: colorScheme.primary),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      value,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
              if (onTap != null)
                Icon(Icons.chevron_right, size: 19, color: colorScheme.outline),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title, this.action});

  final String title;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
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

class _NodeSummary extends StatelessWidget {
  const _NodeSummary({required this.nodes, required this.onOpen});

  final List<Map<String, dynamic>> nodes;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final visible = nodes.take(3).toList(growable: false);
    return Column(
      children: [
        for (var i = 0; i < visible.length; i++) ...[
          _NodeSummaryRow(node: visible[i], onTap: onOpen),
          if (i < visible.length - 1) const Divider(height: 1),
        ],
      ],
    );
  }
}

class _NodeSummaryRow extends StatelessWidget {
  const _NodeSummaryRow({required this.node, required this.onTap});

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
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(vertical: 2),
      leading: _NodeIcon(platform: str(node, 'platform')),
      title: Text(
        isSelf ? '$name · ${l10n.nodesSelf}' : name,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: const TextStyle(fontWeight: FontWeight.w600),
      ),
      subtitle: Text(
        '$host:$port',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              color: online
                  ? context.status.success
                  : theme.colorScheme.outline,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            online ? l10n.nodesOnline : l10n.nodesOffline,
            style: theme.textTheme.labelSmall?.copyWith(
              color: online
                  ? context.status.success
                  : theme.colorScheme.outline,
            ),
          ),
        ],
      ),
      onTap: onTap,
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
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(11),
      ),
      child: Icon(_nodeIconForPlatform(platform), size: 20),
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

class _EndpointRow extends StatelessWidget {
  const _EndpointRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          Expanded(
            child: SelectableText(
              value,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

class _ToolsList extends StatelessWidget {
  const _ToolsList({required this.tools});

  final List<Map<String, dynamic>> tools;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return ConstrainedBox(
      constraints: const BoxConstraints(maxHeight: 280),
      child: ListView.separated(
        shrinkWrap: true,
        itemCount: tools.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, index) {
          final tool = tools[index];
          final name = tool['name']?.toString() ?? '--';
          final exposure = tool['exposure']?.toString() ?? 'read';
          return ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.build_circle_outlined, size: 20),
            title: Text(name, maxLines: 1, overflow: TextOverflow.ellipsis),
            subtitle: Text(
              _toolDescription(l10n, name),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            trailing: Text(
              _toolExposureLabel(l10n, exposure),
              style: Theme.of(context).textTheme.labelSmall,
            ),
          );
        },
      ),
    );
  }
}

String _toolDescription(AppLocalizations l10n, String rawName) {
  final name = rawName.split(':').last;
  return switch (name) {
    'runtime_status' => l10n.toolDescriptionRuntimeStatus,
    'model_list' => l10n.toolDescriptionModelList,
    'device_status' => l10n.toolDescriptionDeviceStatus,
    'clipboard_read' => l10n.toolDescriptionClipboardRead,
    'clipboard_write' => l10n.toolDescriptionClipboardWrite,
    'app_open' => l10n.toolDescriptionAppOpen,
    'ui_screen_state' => l10n.toolDescriptionUiScreenState,
    'ui_click_node' => l10n.toolDescriptionUiClickNode,
    'ui_tap' => l10n.toolDescriptionUiTap,
    'ui_swipe' => l10n.toolDescriptionUiSwipe,
    'ui_wait' => l10n.toolDescriptionUiWait,
    'ui_paste' => l10n.toolDescriptionUiPaste,
    'ui_set_text' => l10n.toolDescriptionUiSetText,
    'vision_status' => l10n.toolDescriptionVisionStatus,
    'vision_start' => l10n.toolDescriptionVisionStart,
    'camera_capture' => l10n.toolDescriptionCameraCapture,
    'vision_stop' => l10n.toolDescriptionVisionStop,
    'sensor_status' => l10n.toolDescriptionSensorStatus,
    'sensor_read' => l10n.toolDescriptionSensorRead,
    'sensor_start' => l10n.toolDescriptionSensorStart,
    'sensor_stop' => l10n.toolDescriptionSensorStop,
    'file_list' => l10n.toolDescriptionFileList,
    'file_read' => l10n.toolDescriptionFileRead,
    'file_write' => l10n.toolDescriptionFileWrite,
    'process_list' => l10n.toolDescriptionProcessList,
    'process_run' => l10n.toolDescriptionProcessRun,
    _ => l10n.toolDescriptionGeneric,
  };
}

String _toolExposureLabel(AppLocalizations l10n, String exposure) =>
    switch (exposure) {
      'sensor' => l10n.settingsToolSensor,
      'action' => l10n.settingsToolAction,
      _ => l10n.settingsToolRead,
    };

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
