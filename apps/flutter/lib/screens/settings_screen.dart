import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../app.dart';
import '../l10n/app_localizations.dart';
import 'onboarding_screen.dart';
import '../theme.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  String _view = 'home';

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _view == 'home'
              ? l10n.settingsTitle
              : _view == 'runtime'
              ? l10n.settingsRuntimeTitle
              : _view == 'server'
              ? l10n.settingsServerTitle
              : _view == 'model'
              ? l10n.settingsModelTitle
              : _view == 'agent'
              ? l10n.settingsAgentTitle
              : l10n.settingsAdvancedTitle,
        ),
        leading: _view == 'home'
            ? null
            : IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _view = 'home'),
              ),
      ),
      body: switch (_view) {
        'home' => _homeGrid(l10n),
        'runtime' => const _RuntimePanel(),
        'server' => const _ServerPanel(),
        'model' => const _ModelPanel(),
        'agent' => const _AgentPanel(),
        _ => const _AdvancedPanel(),
      },
    );
  }

  Widget _homeGrid(AppLocalizations l10n) {
    final locale = AppScope.of(context).locale;
    final theme = AppScope.of(context).theme;
    return _SettingsList(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.settingsTheme,
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 4),
                Text(
                  l10n.settingsThemeSub,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 10),
                SegmentedButton<String>(
                  segments: [
                    ButtonSegment(
                      value: 'dark',
                      label: Text(l10n.settingsThemeDark),
                      icon: const Icon(Icons.dark_mode_outlined),
                    ),
                    ButtonSegment(
                      value: 'pink',
                      label: Text(l10n.settingsThemePink),
                      icon: const Icon(Icons.brightness_high_outlined),
                    ),
                  ],
                  selected: {theme.mode},
                  onSelectionChanged: (selection) =>
                      theme.setMode(selection.first),
                ),
              ],
            ),
          ),
        ),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.settingsLanguage,
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 4),
                Text(
                  l10n.settingsLanguageSub,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 10),
                SegmentedButton<String>(
                  segments: [
                    ButtonSegment(
                      value: 'system',
                      label: Text(l10n.settingsFollowSystem),
                    ),
                    const ButtonSegment(value: 'zh', label: Text('中文')),
                    const ButtonSegment(value: 'en', label: Text('EN')),
                  ],
                  selected: {locale.mode},
                  onSelectionChanged: (selection) =>
                      locale.setMode(selection.first),
                ),
              ],
            ),
          ),
        ),
        _tile(
          l10n.settingsRuntimeTitle,
          l10n.settingsRuntimeSub,
          Icons.play_circle_outline,
          'runtime',
        ),
        _tile(
          l10n.settingsServerTitle,
          l10n.settingsServerSub,
          Icons.dns_outlined,
          'server',
        ),
        _tile(
          l10n.settingsModelTitle,
          l10n.settingsModelSub,
          Icons.tune_outlined,
          'model',
        ),
        _tile(
          l10n.settingsAgentTitle,
          l10n.settingsAgentSub,
          Icons.psychology_outlined,
          'agent',
        ),
        _tile(
          l10n.settingsAdvancedTitle,
          l10n.settingsAdvancedSub,
          Icons.insights_outlined,
          'advanced',
        ),
        Card(
          child: ListTile(
            leading: const Icon(Icons.travel_explore_outlined),
            title: Text(l10n.settingsOnboardingTitle),
            subtitle: Text(l10n.settingsOnboardingSub),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) =>
                    OnboardingScreen(onDone: () => Navigator.of(context).pop()),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _tile(String title, String sub, IconData icon, String view) {
    return Card(
      child: ListTile(
        leading: Icon(icon),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(sub),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => setState(() => _view = view),
      ),
    );
  }
}

class _RuntimePanel extends StatelessWidget {
  const _RuntimePanel();

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final runtime = scope.runtime;
    final l10n = AppLocalizations.of(context)!;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final state = runtime.state;
        final isAndroid =
            !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
        return _SettingsList(
          children: [
            Card(
              child: SwitchListTile(
                title: Text(l10n.settingsModelServer),
                subtitle: Text(
                  isAndroid
                      ? (state?.message ?? '')
                      : l10n.settingsModelServerDesktop,
                ),
                value: state?.running ?? false,
                onChanged: !isAndroid && state?.starting == true
                    ? null
                    : isAndroid
                    ? (_) => (state?.running ?? false)
                          ? runtime.stopRuntime()
                          : runtime.startRuntime()
                    : null,
              ),
            ),
            Card(
              child: SwitchListTile(
                title: Text(l10n.settingsAutoStart),
                value: state?.autoStart ?? false,
                onChanged: (v) => runtime.updateSettings({'autoStart': v}),
              ),
            ),
            if (isAndroid) ...[
              _SectionTitle(l10n.settingsPermissionsTitle),
              _PermissionTile(
                icon: Icons.photo_camera_outlined,
                label: l10n.settingsPermissionCamera,
                granted: state?.cameraPermissionGranted ?? false,
                onGrant: () => runtime.requestCamera(),
              ),
              _PermissionTile(
                icon: Icons.notifications_outlined,
                label: l10n.settingsPermissionNotification,
                granted: state?.notificationPermissionGranted ?? false,
                onGrant: () => runtime.requestNotification(),
              ),
              _PermissionTile(
                icon: Icons.layers_outlined,
                label: l10n.settingsPermissionOverlay,
                granted: state?.windowAllowed ?? false,
                onGrant: () => runtime.requestOverlay(),
              ),
              _PermissionTile(
                icon: Icons.battery_charging_full_outlined,
                label: l10n.settingsPermissionBattery,
                granted: state?.batteryOptimizationIgnored ?? false,
                onGrant: () => runtime.requestBattery(),
              ),
              _PermissionTile(
                icon: Icons.accessibility_new_outlined,
                label: l10n.settingsPermissionAccessibility,
                granted: state?.accessibilityPermissionGranted ?? false,
                onGrant: () => runtime.requestAccessibility(),
              ),
              _SectionTitle(l10n.settingsWindowVision),
              Card(
                child: SwitchListTile(
                  title: Text(l10n.settingsWindowAuto),
                  value: state?.windowAutoShow ?? false,
                  onChanged: (v) => runtime.updateWindow({'autoShow': v}),
                ),
              ),
              Card(
                child: ListTile(
                  title: Text(l10n.settingsVisionTitle),
                  subtitle: Text((state?.vision['message'] ?? '').toString()),
                  trailing: FilledButton.tonal(
                    onPressed: () => (state?.vision['running'] == true)
                        ? runtime.stopVision()
                        : runtime.startVision(),
                    child: Text(
                      state?.vision['running'] == true
                          ? l10n.commonStop
                          : l10n.commonStart,
                    ),
                  ),
                ),
              ),
            ] else
              Card(
                child: ListTile(
                  leading: const Icon(Icons.info_outline),
                  title: Text(l10n.settingsPermissionsTitle),
                  subtitle: Text(l10n.settingsDesktopPermissionsHint),
                ),
              ),
            Card(
              child: ListTile(
                title: Text(l10n.settingsToolExposure),
                trailing: _Dropdown(
                  value: state?.toolExposure ?? 'action',
                  items: [
                    DropdownMenuItem(
                      value: 'read',
                      child: Text(l10n.settingsToolRead),
                    ),
                    DropdownMenuItem(
                      value: 'sensor',
                      child: Text(l10n.settingsToolSensor),
                    ),
                    DropdownMenuItem(
                      value: 'action',
                      child: Text(l10n.settingsToolAction),
                    ),
                  ],
                  onChanged: (v) => runtime.updateSettings({'toolExposure': v}),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _ServerPanel extends StatefulWidget {
  const _ServerPanel();

  @override
  State<_ServerPanel> createState() => _ServerPanelState();
}

class _ServerPanelState extends State<_ServerPanel> {
  late TextEditingController _port;
  late TextEditingController _tokens;
  late TextEditingController _token;
  late TextEditingController _peerToken;

  @override
  void initState() {
    super.initState();
    final state = AppScope.maybeOf(context)?.runtime.state;
    _port = TextEditingController(text: '${state?.port ?? 11434}');
    _tokens = TextEditingController(text: '${state?.maxOutputTokens ?? 512}');
    _token = TextEditingController(
      text: AppScope.maybeOf(context)?.runtime.api.authToken ?? '',
    );
    _peerToken = TextEditingController();
  }

  @override
  void dispose() {
    _port.dispose();
    _tokens.dispose();
    _token.dispose();
    _peerToken.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final runtime = scope.runtime;
    final l10n = AppLocalizations.of(context)!;
    final state = runtime.state;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        return _SettingsList(
          children: [
            _numberField(
              l10n.settingsPort,
              _port,
              1024,
              65535,
              (v) => runtime.updateSettings({'port': v}),
            ),
            _numberField(
              l10n.settingsOutputTokens,
              _tokens,
              1,
              state?.hardMaxOutputTokens ?? 32768,
              (v) => runtime.updateSettings({'maxOutputTokens': v}),
            ),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.settingsApiToken),
                    TextField(
                      controller: _token,
                      decoration: const InputDecoration(isDense: true),
                      onChanged: (_) {},
                    ),
                    Row(
                      children: [
                        FilledButton.tonal(
                          onPressed: () => runtime.updateSettings({
                            'authToken': _token.text.trim(),
                          }),
                          child: Text(l10n.commonSave),
                        ),
                        TextButton(
                          onPressed: () async {
                            await runtime.generateAuthToken();
                            _token.text = runtime.api.authToken;
                          },
                          child: Text(l10n.settingsGenerate),
                        ),
                        TextButton(
                          onPressed: () {
                            _token.clear();
                            runtime.updateSettings({'authToken': ''});
                          },
                          child: Text(l10n.settingsClear),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.settingsPeerToken),
                    TextField(
                      controller: _peerToken,
                      decoration: const InputDecoration(isDense: true),
                      onChanged: (_) {},
                    ),
                    Text(
                      l10n.settingsPeerTokenHint,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    Row(
                      children: [
                        TextButton(
                          onPressed: () => runtime.updateSettings({
                            'peerToken': _peerToken.text.trim(),
                          }),
                          child: Text(l10n.settingsPeerTokenSave),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            _copyRow(l10n.connectionOpenaiUrl, '${state?.baseUrl ?? ''}/v1'),
            _copyRow(l10n.connectionMcpUrl, '${state?.baseUrl ?? ''}/mcp'),
            _copyRow(
              l10n.connectionAuthHeader,
              runtime.api.authToken.isEmpty
                  ? l10n.connectionAuthDisabled
                  : l10n.connectionAuthHeaderValue(runtime.api.authToken),
            ),
          ],
        );
      },
    );
  }

  Widget _numberField(
    String label,
    TextEditingController controller,
    int min,
    int max,
    void Function(int) onChanged,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Row(
          children: [
            Expanded(child: Text(label)),
            SizedBox(
              width: 120,
              child: TextField(
                controller: controller,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                onSubmitted: (raw) {
                  final value = (int.tryParse(raw) ?? min).clamp(min, max);
                  controller.text = '$value';
                  onChanged(value);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _copyRow(String label, String value) {
    return Card(
      child: ListTile(
        title: Text(label, style: const TextStyle(fontSize: 13)),
        subtitle: Text(value, maxLines: 1, overflow: TextOverflow.ellipsis),
        trailing: IconButton(
          icon: const Icon(Icons.copy, size: 18),
          onPressed: () async {
            await Clipboard.setData(ClipboardData(text: value));
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(AppLocalizations.of(context)!.toastCopied),
                ),
              );
            }
          },
        ),
      ),
    );
  }
}

class _ModelPanel extends StatelessWidget {
  const _ModelPanel();

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final runtime = scope.runtime;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final l10n = AppLocalizations.of(context)!;
        final state = runtime.state;
        final threads = state?.cpuThreads ?? 4;
        final maxThreads = state?.maxCpuThreads ?? 16;
        return _SettingsList(
          children: [
            _selectRow(
              l10n.settingsPerformanceMode,
              threads <= maxThreads ~/ 3
                  ? 'eco'
                  : threads >= (maxThreads * 0.75).ceil()
                  ? 'fast'
                  : 'balanced',
              {
                'eco': l10n.settingsPerformanceEco,
                'balanced': l10n.settingsPerformanceBalanced,
                'fast': l10n.settingsPerformanceFast,
              },
              (mode) => runtime.updateSettings({
                'cpuThreads': mode == 'eco'
                    ? maxThreads ~/ 3
                    : mode == 'fast'
                    ? maxThreads
                    : maxThreads ~/ 2,
              }),
            ),
            _selectRow(
              l10n.settingsInferenceBackend,
              state?.inferenceBackend ?? 'model',
              {
                'model': l10n.settingsBackendModel,
                'auto': l10n.settingsBackendAuto,
                'cpu': l10n.settingsBackendCpu,
                'opencl': l10n.settingsBackendOpencl,
                'vulkan': l10n.settingsBackendVulkan,
              },
              (v) => runtime.updateSettings({'inferenceBackend': v}),
            ),
            _selectRow(
              l10n.settingsResponseLength,
              '${state?.maxOutputTokens ?? 512}',
              {
                '256': l10n.settingsLengthShort,
                '512': l10n.settingsLengthNormal,
                '1024': l10n.settingsLengthLong,
              },
              (v) => runtime.updateSettings({'maxOutputTokens': int.parse(v)}),
            ),
            _selectRow(
              l10n.settingsContextMemory,
              state?.contextProfile ?? 'balanced',
              {
                'light': l10n.settingsContextLight,
                'balanced': l10n.settingsContextBalanced,
                'deep': l10n.settingsContextDeep,
              },
              (v) => runtime.updateSettings({
                'contextProfile': v,
                'historyLimit': v == 'light'
                    ? 16
                    : v == 'deep'
                    ? 128
                    : 64,
              }),
            ),
            Card(
              child: ListTile(
                title: Text(l10n.settingsReleaseModel),
                trailing: FilledButton.tonal(
                  onPressed: () => runtime.releaseModel(),
                  child: Text(l10n.settingsRelease),
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _selectRow(
    String label,
    String value,
    Map<String, String> options,
    void Function(String) onChanged,
  ) {
    return Card(
      child: ListTile(
        title: Text(label),
        trailing: _Dropdown(
          value: options.containsKey(value) ? value : options.keys.first,
          items: options.entries
              .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
              .toList(),
          onChanged: onChanged,
        ),
      ),
    );
  }
}

class _AgentPanel extends StatelessWidget {
  const _AgentPanel();

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final runtime = scope.runtime;
    final l10n = AppLocalizations.of(context)!;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final state = runtime.state;
        final policy = state?.agentPolicy ?? const {};
        final min = (policy['roundsMin'] as num?)?.toInt() ?? 8;
        final max = (policy['roundsMax'] as num?)?.toInt() ?? 64;
        final toolCalls = (policy['maxToolCalls'] as num?)?.toInt() ?? 64;
        return _SettingsList(
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 6,
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(l10n.settingsAgentRounds),
                          Text(
                            l10n.settingsAgentRoundsSub(min, max),
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                    SizedBox(
                      width: 110,
                      child: TextField(
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          hintText: '${state?.agentMaxRounds ?? 32}',
                        ),
                        onSubmitted: (raw) {
                          final value = (int.tryParse(raw) ?? 32).clamp(
                            min,
                            max,
                          );
                          runtime.updateSettings({'agentMaxRounds': value});
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Card(
              child: ListTile(
                title: Text(l10n.settingsToolCalls),
                subtitle: Text(AppLocalizations.of(context)!.statusRunning),
                trailing: Text('$toolCalls'),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _AdvancedPanel extends StatelessWidget {
  const _AdvancedPanel();

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final runtime = scope.runtime;
    final chat = scope.chat;
    final l10n = AppLocalizations.of(context)!;
    return ListenableBuilder(
      listenable: Listenable.merge([runtime, chat]),
      builder: (context, _) {
        final sessions = runtime.state?.sessions ?? const [];
        return _SettingsList(
          children: [
            Card(
              child: ListTile(
                title: Text(l10n.settingsSessions),
                trailing: FilledButton.tonal(
                  onPressed: chat.streaming ? null : () => chat.newChat(),
                  child: Text(l10n.homeNewChat),
                ),
              ),
            ),
            ...sessions.map(
              (session) => Card(
                child: ListTile(
                  dense: true,
                  title: Text(
                    session.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  subtitle: Text(
                    '${session.modelId} · ${session.messageCount}',
                  ),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline, size: 18),
                    onPressed: () => runtime.deleteSession(session.id),
                  ),
                  onTap: () async {
                    await runtime.selectSession(session.id);
                    await chat.loadSession(session.id);
                  },
                ),
              ),
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: Text(l10n.aboutTitle),
              subtitle: Text(l10n.aboutVersionLine('2.0.1')),
            ),
          ],
        );
      },
    );
  }
}

/// Settings scroll container: keeps a uniform gap between every card.
class _SettingsList extends StatelessWidget {
  const _SettingsList({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(14),
      itemCount: children.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (_, index) => children[index],
    );
  }
}

/// Dropdown styled like the rest of the selection controls: filled, rounded
/// 12px, no underline. (This Flutter SDK has no global dropdown button
/// theme, so the styling lives here.)
class _Dropdown extends StatelessWidget {
  const _Dropdown({
    required this.value,
    required this.items,
    required this.onChanged,
  });

  final String value;
  final List<DropdownMenuItem<String>> items;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: scheme.outlineVariant.withValues(alpha: 0.4)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: value,
          isDense: true,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: scheme.onSurface,
          ),
          iconEnabledColor: scheme.onSurfaceVariant,
          items: items,
          onChanged: (v) {
            if (v != null) onChanged(v);
          },
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(6, 18, 6, 8),
      child: Text(
        text,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }
}

class _PermissionTile extends StatelessWidget {
  const _PermissionTile({
    required this.icon,
    required this.label,
    required this.granted,
    required this.onGrant,
  });

  final IconData icon;
  final String label;
  final bool granted;
  final VoidCallback onGrant;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Card(
      child: ListTile(
        leading: Icon(icon),
        title: Text(label),
        trailing: granted
            ? Icon(Icons.check_circle, color: context.status.success, size: 20)
            : FilledButton.tonal(
                onPressed: onGrant,
                child: Text(l10n.settingsGrant),
              ),
      ),
    );
  }
}
