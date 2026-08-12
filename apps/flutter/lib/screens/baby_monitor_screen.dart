import 'dart:async';

import 'package:flutter/material.dart';

import '../app.dart';
import '../l10n/app_localizations.dart';

/// 眠安智护监控页：展示当前后端（如板子）的婴儿监控状态与事件。
class BabyMonitorScreen extends StatefulWidget {
  const BabyMonitorScreen({super.key, this.nodeId});

  /// 指定查看某个 peer 节点的监控；null 表示当前连接的服务器。
  final String? nodeId;

  @override
  State<BabyMonitorScreen> createState() => _BabyMonitorScreenState();
}

class _BabyMonitorScreenState extends State<BabyMonitorScreen> {
  Map<String, dynamic>? _data;
  String? _error;
  Timer? _timer;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _load();
    _scheduleNextLoad();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    if (_loading) return;
    _loading = true;
    final api = AppScope.of(context).runtime.api;
    try {
      final path = widget.nodeId == null
          ? '/api/v1/baby/state'
          : '/api/v1/peers/${widget.nodeId}/baby/state';
      final response = await api.get(path);
      if (!mounted) return;
      setState(() {
        _data = response is Map<String, dynamic> ? response : null;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = error.toString());
    } finally {
      _loading = false;
      if (mounted) _scheduleNextLoad();
    }
  }

  void _scheduleNextLoad() {
    _timer?.cancel();
    _timer = Timer(const Duration(seconds: 2), _load);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.babyTitle)),
      body: _error != null
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.child_care, size: 56, color: Colors.grey),
                    const SizedBox(height: 12),
                    Text(l10n.babyNotEnabled, textAlign: TextAlign.center),
                    const SizedBox(height: 8),
                    Text(
                      _error!,
                      style: Theme.of(context).textTheme.bodySmall,
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            )
          : _data == null
          ? const Center(child: CircularProgressIndicator())
          : _buildBody(l10n),
    );
  }

  Widget _buildBody(AppLocalizations l10n) {
    final theme = Theme.of(context);
    final state = _data!['state'] as String? ?? 'Idle';
    final latest = _data!['latest'] as Map<String, dynamic>?;
    final events = (_data!['events'] as List? ?? const [])
        .whereType<Map<String, dynamic>>()
        .toList();
    final motion = (latest?['motion'] as num?)?.toDouble() ?? 0.0;

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _StateCard(state: state, motion: motion, latest: latest, l10n: l10n),
          const SizedBox(height: 16),
          Text(
            l10n.babyEvents,
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          if (events.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                l10n.babyNoEvents,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
            )
          else
            Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Column(
                  children: [
                    for (final event in events.take(15))
                      _EventRow(event: event, l10n: l10n),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _StateCard extends StatelessWidget {
  const _StateCard({
    required this.state,
    required this.motion,
    required this.latest,
    required this.l10n,
  });

  final String state;
  final double motion;
  final Map<String, dynamic>? latest;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final info = _stateInfo(state, l10n);
    return Card(
      color: info.color.withValues(alpha: 0.12),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(info.icon, color: info.color, size: 28),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(l10n.babyState, style: theme.textTheme.bodySmall),
                      Text(
                        info.label,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: info.color,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Text(l10n.babyMotion, style: theme.textTheme.bodySmall),
                const SizedBox(width: 10),
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: motion.clamp(0.0, 1.0),
                      minHeight: 8,
                      backgroundColor:
                          theme.colorScheme.surfaceContainerHighest,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Text(
                  (motion * 100).round().toString(),
                  style: theme.textTheme.titleMedium,
                ),
              ],
            ),
            if (latest?['reason'] != null) ...[
              const SizedBox(height: 10),
              Text(
                latest!['reason'] as String,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _EventRow extends StatelessWidget {
  const _EventRow({required this.event, required this.l10n});

  final Map<String, dynamic> event;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final info = _stateInfo(event['state'] as String? ?? 'Idle', l10n);
    final timestamp = (event['timestamp'] as num?)?.toDouble() ?? 0;
    return ListTile(
      dense: true,
      leading: Icon(info.icon, color: info.color, size: 20),
      title: Text(info.label, style: const TextStyle(fontSize: 14)),
      subtitle: Text(event['reason'] as String? ?? ''),
      trailing: Text(
        _formatTime(timestamp),
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.outline,
        ),
      ),
    );
  }
}

class _StateInfo {
  const _StateInfo(this.label, this.color, this.icon);

  final String label;
  final Color color;
  final IconData icon;
}

_StateInfo _stateInfo(String state, AppLocalizations l10n) {
  switch (state) {
    case 'Candidate':
      return _StateInfo(
        l10n.babyStateCandidate,
        Colors.orange,
        Icons.visibility_outlined,
      );
    case 'Soothing1':
      return _StateInfo(
        l10n.babyStateSoothing1,
        Colors.lightBlue,
        Icons.volume_up_outlined,
      );
    case 'Soothing2':
      return _StateInfo(
        l10n.babyStateSoothing2,
        Colors.indigo,
        Icons.volume_up,
      );
    case 'NotifyParent':
      return _StateInfo(
        l10n.babyStateNotify,
        Colors.red,
        Icons.notification_important_outlined,
      );
    case 'Cooldown':
      return _StateInfo(
        l10n.babyStateCooldown,
        Colors.blueGrey,
        Icons.timer_outlined,
      );
    default:
      return _StateInfo(
        l10n.babyStateIdle,
        Colors.green,
        Icons.child_care_outlined,
      );
  }
}

String _formatTime(double timestamp) {
  if (timestamp <= 0) return '';
  final time = DateTime.fromMillisecondsSinceEpoch((timestamp * 1000).round());
  final now = DateTime.now();
  final diff = now.difference(time);
  if (diff.inSeconds < 60) return '${diff.inSeconds}s';
  if (diff.inMinutes < 60) return '${diff.inMinutes}m';
  return '${time.hour}:${time.minute.toString().padLeft(2, '0')}';
}
