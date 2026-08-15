import 'package:flutter/material.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'baby_monitor_screen.dart';

/// Installed edge extensions. Extensions are currently bundled capabilities;
/// the page gives each one a clear lifecycle, permission scope and entrypoint
/// before we introduce signed, downloadable packages.
class ExtensionsScreen extends StatefulWidget {
  const ExtensionsScreen({super.key});

  @override
  State<ExtensionsScreen> createState() => _ExtensionsScreenState();
}

class _ExtensionsScreenState extends State<ExtensionsScreen> {
  Map<String, dynamic>? _babyState;
  String? _error;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    if (_loading || !mounted) return;
    setState(() => _loading = true);
    try {
      final response = await AppScope.of(
        context,
      ).runtime.api.get('/api/v1/baby/state');
      final state = asMap(response);
      if (state['state'] is! String) {
        throw StateError('invalid baby monitor state');
      }
      if (!mounted) return;
      setState(() {
        _babyState = state;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _babyState = null;
        _error = error.toString();
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final status = _resolveStatus();
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
          children: [
            _ExtensionsHeader(
              title: l10n.extensionsTitle,
              subtitle: l10n.extensionsSubtitle,
              onRefresh: _loading ? null : _load,
            ),
            const SizedBox(height: 22),
            Text(
              l10n.extensionsInstalledTitle,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            Text(
              l10n.extensionsBuiltInHint,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 12),
            _BabyMonitorExtensionCard(
              status: status,
              currentState: _babyState?['state'] as String?,
              onOpen: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const BabyMonitorScreen()),
              ),
              onRefresh: _loading ? null : _load,
              onSettings: () => homeShellKey.currentState?.switchTo(4),
            ),
            if (_error != null && status == _ExtensionStatus.unavailable) ...[
              const SizedBox(height: 10),
              Text(
                l10n.extensionsUnavailableHint,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.outline,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  _ExtensionStatus _resolveStatus() {
    if (_loading && _babyState == null) return _ExtensionStatus.checking;
    if (_babyState != null) return _ExtensionStatus.enabled;
    if (_error?.toLowerCase().contains('not enabled') == true) {
      return _ExtensionStatus.notConfigured;
    }
    return _ExtensionStatus.unavailable;
  }
}

class _ExtensionsHeader extends StatelessWidget {
  const _ExtensionsHeader({
    required this.title,
    required this.subtitle,
    required this.onRefresh,
  });

  final String title;
  final String subtitle;
  final VoidCallback? onRefresh;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: scheme.primaryContainer,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Icon(Icons.extension_outlined, color: scheme.primary),
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
                style: Theme.of(
                  context,
                ).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
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

class _BabyMonitorExtensionCard extends StatelessWidget {
  const _BabyMonitorExtensionCard({
    required this.status,
    required this.currentState,
    required this.onOpen,
    required this.onRefresh,
    required this.onSettings,
  });

  final _ExtensionStatus status;
  final String? currentState;
  final VoidCallback onOpen;
  final VoidCallback? onRefresh;
  final VoidCallback onSettings;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final statusColor = _statusColor(context);
    return Card(
      child: InkWell(
        onTap: onOpen,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 46,
                    height: 46,
                    decoration: BoxDecoration(
                      color: scheme.tertiaryContainer,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Icon(
                      Icons.child_care_outlined,
                      color: scheme.onTertiaryContainer,
                      size: 25,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.babyTitle,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          l10n.babySubtitle,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: scheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  _StatusChip(label: _statusLabel(l10n), color: statusColor),
                ],
              ),
              const SizedBox(height: 16),
              Text(
                l10n.extensionsBabyDescription,
                style: theme.textTheme.bodyMedium,
              ),
              const SizedBox(height: 12),
              _ExtensionInfoRow(
                icon: Icons.camera_alt_outlined,
                text: l10n.extensionsBabyPermissions,
              ),
              if (status == _ExtensionStatus.enabled &&
                  currentState != null) ...[
                const SizedBox(height: 7),
                _ExtensionInfoRow(
                  icon: Icons.monitor_heart_outlined,
                  text: '${l10n.babyState}: $currentState',
                ),
              ],
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  FilledButton.tonalIcon(
                    onPressed: onOpen,
                    icon: const Icon(Icons.open_in_new_outlined, size: 18),
                    label: Text(l10n.extensionsOpen),
                  ),
                  OutlinedButton.icon(
                    onPressed: onSettings,
                    icon: const Icon(Icons.tune_outlined, size: 18),
                    label: Text(l10n.extensionsOpenSettings),
                  ),
                  IconButton(
                    onPressed: onRefresh,
                    tooltip: l10n.extensionsRefresh,
                    icon: const Icon(Icons.sync_outlined),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _statusLabel(AppLocalizations l10n) => switch (status) {
    _ExtensionStatus.checking => l10n.extensionsStatusChecking,
    _ExtensionStatus.enabled => l10n.extensionsStatusEnabled,
    _ExtensionStatus.notConfigured => l10n.extensionsStatusNotConfigured,
    _ExtensionStatus.unavailable => l10n.extensionsStatusUnavailable,
  };

  Color _statusColor(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return switch (status) {
      _ExtensionStatus.checking => context.status.warning,
      _ExtensionStatus.enabled => context.status.success,
      _ExtensionStatus.notConfigured => context.status.warning,
      _ExtensionStatus.unavailable => scheme.outline,
    };
  }
}

class _ExtensionInfoRow extends StatelessWidget {
  const _ExtensionInfoRow({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Icon(icon, size: 18, color: scheme.outline),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ),
      ],
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.13),
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
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: color == scheme.outline ? scheme.onSurfaceVariant : color,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

enum _ExtensionStatus { checking, enabled, notConfigured, unavailable }
