import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';
import '../state/runtime_controller.dart';

const _appVersion = '2.0.1';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  String _page = 'home';

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final isHome = _page == 'home';
    return Scaffold(
      appBar: AppBar(
        title: Text(_title(l10n)),
        leading: isHome
            ? null
            : IconButton(
                tooltip: l10n.commonBack,
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _page = 'home'),
              ),
      ),
      body: switch (_page) {
        'security' => const _SecuritySettingsPage(),
        'model' => const _LocalModelSettingsPage(),
        'about' => const _AboutSettingsPage(),
        _ => _SettingsHome(onOpen: (page) => setState(() => _page = page)),
      },
    );
  }

  String _title(AppLocalizations l10n) => switch (_page) {
    'security' => l10n.settingsSecurityTitle,
    'model' => l10n.settingsLocalModelTitle,
    'about' => l10n.aboutTitle,
    _ => l10n.settingsTitle,
  };
}

class _SettingsHome extends StatelessWidget {
  const _SettingsHome({required this.onOpen});

  final ValueChanged<String> onOpen;

  @override
  Widget build(BuildContext context) {
    final scope = AppScope.of(context);
    final l10n = AppLocalizations.of(context)!;
    return ListenableBuilder(
      listenable: Listenable.merge([scope.locale, scope.theme]),
      builder: (context, _) => ListView(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
        children: [
          Text(
            l10n.settingsSubtitle,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 18),
          _SectionLabel(label: l10n.settingsAppearanceTitle),
          const SizedBox(height: 8),
          _ChoiceCard(
            icon: Icons.translate_outlined,
            title: l10n.settingsLanguage,
            subtitle: l10n.settingsLanguageSub,
            child: SegmentedButton<String>(
              segments: [
                ButtonSegment(
                  value: 'system',
                  label: Text(l10n.settingsFollowSystem),
                ),
                ButtonSegment(
                  value: 'zh',
                  label: Text(l10n.settingsLanguageChinese),
                ),
                ButtonSegment(
                  value: 'en',
                  label: Text(l10n.settingsLanguageEnglish),
                ),
              ],
              selected: {scope.locale.mode},
              onSelectionChanged: (value) {
                if (value.isNotEmpty) scope.locale.setMode(value.first);
              },
            ),
          ),
          const SizedBox(height: 10),
          _ChoiceCard(
            icon: Icons.palette_outlined,
            title: l10n.settingsTheme,
            subtitle: l10n.settingsThemeSub,
            child: SegmentedButton<String>(
              segments: [
                ButtonSegment(
                  value: 'dark',
                  icon: const Icon(Icons.dark_mode_outlined),
                  label: Text(l10n.settingsThemeDark),
                ),
                ButtonSegment(
                  value: 'pink',
                  icon: const Icon(Icons.light_mode_outlined),
                  label: Text(l10n.settingsThemePink),
                ),
              ],
              selected: {scope.theme.mode},
              onSelectionChanged: (value) {
                if (value.isNotEmpty) scope.theme.setMode(value.first);
              },
            ),
          ),
          const SizedBox(height: 20),
          _SectionLabel(label: l10n.settingsSectionsTitle),
          const SizedBox(height: 8),
          _SettingsEntry(
            icon: Icons.shield_outlined,
            title: l10n.settingsSecurityTitle,
            subtitle: l10n.settingsSecuritySub,
            onTap: () => onOpen('security'),
          ),
          _SettingsEntry(
            icon: Icons.memory_outlined,
            title: l10n.settingsLocalModelTitle,
            subtitle: l10n.settingsLocalModelSub,
            onTap: () => onOpen('model'),
          ),
          _SettingsEntry(
            icon: Icons.info_outline,
            title: l10n.aboutTitle,
            subtitle: l10n.settingsAboutSub,
            onTap: () => onOpen('about'),
          ),
        ],
      ),
    );
  }
}

class _SecuritySettingsPage extends StatefulWidget {
  const _SecuritySettingsPage();

  @override
  State<_SecuritySettingsPage> createState() => _SecuritySettingsPageState();
}

class _SecuritySettingsPageState extends State<_SecuritySettingsPage> {
  late final TextEditingController _authToken;
  late final TextEditingController _peerToken;
  bool _obscureAuth = true;
  bool _obscurePeer = true;

  @override
  void initState() {
    super.initState();
    final scope = AppScope.maybeOf(context);
    _authToken = TextEditingController(
      text: scope?.runtime.api.authToken ?? '',
    );
    _peerToken = TextEditingController();
  }

  @override
  void dispose() {
    _authToken.dispose();
    _peerToken.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final runtime = AppScope.of(context).runtime;
    final l10n = AppLocalizations.of(context)!;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final exposure = runtime.state?.toolExposure ?? 'action';
        return ListView(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
          children: [
            _PageIntro(
              icon: Icons.shield_outlined,
              title: l10n.settingsSecurityTitle,
              subtitle: l10n.settingsSecuritySub,
            ),
            const SizedBox(height: 16),
            _SettingsCard(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.settingsApiToken,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l10n.settingsApiTokenHint,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: _authToken,
                      obscureText: _obscureAuth,
                      decoration: InputDecoration(
                        isDense: true,
                        suffixIcon: IconButton(
                          tooltip: _obscureAuth
                              ? l10n.settingsShowToken
                              : l10n.settingsHideToken,
                          icon: Icon(
                            _obscureAuth
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                          onPressed: () =>
                              setState(() => _obscureAuth = !_obscureAuth),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        FilledButton.tonalIcon(
                          onPressed: () => runtime.updateSettings({
                            'authToken': _authToken.text.trim(),
                          }),
                          icon: const Icon(Icons.save_outlined),
                          label: Text(l10n.commonSave),
                        ),
                        OutlinedButton.icon(
                          onPressed: () async {
                            await runtime.generateAuthToken();
                            if (mounted) {
                              _authToken.text = runtime.api.authToken;
                            }
                          },
                          icon: const Icon(Icons.autorenew_outlined),
                          label: Text(l10n.settingsGenerate),
                        ),
                        TextButton.icon(
                          onPressed: () {
                            _authToken.clear();
                            runtime.updateSettings({'authToken': ''});
                          },
                          icon: const Icon(Icons.clear_outlined),
                          label: Text(l10n.settingsClear),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            _DevicePermissionsCard(
              runtime: runtime,
              state: runtime.state,
              isAndroid: Theme.of(context).platform == TargetPlatform.android,
            ),
            const SizedBox(height: 10),
            _SettingsCard(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.settingsPeerToken,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l10n.settingsPeerTokenHint,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: _peerToken,
                      obscureText: _obscurePeer,
                      decoration: InputDecoration(
                        isDense: true,
                        suffixIcon: IconButton(
                          tooltip: _obscurePeer
                              ? l10n.settingsShowToken
                              : l10n.settingsHideToken,
                          icon: Icon(
                            _obscurePeer
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                          onPressed: () =>
                              setState(() => _obscurePeer = !_obscurePeer),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    FilledButton.tonalIcon(
                      onPressed: () => runtime.updateSettings({
                        'peerToken': _peerToken.text.trim(),
                      }),
                      icon: const Icon(Icons.save_outlined),
                      label: Text(l10n.settingsPeerTokenSave),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            _SettingsCard(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.settingsToolExposure,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l10n.settingsToolExposureHint,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 10),
                    DropdownButtonFormField<String>(
                      initialValue: exposure == 'read' || exposure == 'sensor'
                          ? exposure
                          : 'action',
                      decoration: InputDecoration(
                        isDense: true,
                        labelText: l10n.settingsToolExposure,
                      ),
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
                      onChanged: (value) => runtime.updateSettings({
                        'toolExposure': value ?? 'action',
                      }),
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _DevicePermissionsCard extends StatelessWidget {
  const _DevicePermissionsCard({
    required this.runtime,
    required this.state,
    required this.isAndroid,
  });

  final RuntimeController runtime;
  final RuntimeUiState? state;
  final bool isAndroid;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return _SettingsCard(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.settingsPermissionsTitle,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            Text(
              l10n.settingsPermissionsHint,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 8),
            if (isAndroid) ...[
              _PermissionTile(
                icon: Icons.notifications_none_outlined,
                title: l10n.settingsPermissionNotification,
                subtitle: l10n.settingsPermissionNotificationHint,
                granted: state?.notificationPermissionGranted,
                onAction: _permissionAction(
                  granted: state?.notificationPermissionGranted,
                  request: runtime.requestNotification,
                  kind: 'notification',
                ),
              ),
              _PermissionTile(
                icon: Icons.battery_saver_outlined,
                title: l10n.settingsPermissionBackground,
                subtitle: l10n.settingsPermissionBackgroundHint,
                granted: state?.batteryOptimizationIgnored,
                onAction: _permissionAction(
                  granted: state?.batteryOptimizationIgnored,
                  request: runtime.requestBattery,
                  kind: 'battery',
                ),
              ),
              _PermissionTile(
                icon: Icons.accessibility_new_outlined,
                title: l10n.settingsPermissionAccessibility,
                subtitle: l10n.settingsPermissionAccessibilityHint,
                granted: state?.accessibilityPermissionGranted,
                onAction: _permissionAction(
                  granted: state?.accessibilityPermissionGranted,
                  request: runtime.requestAccessibility,
                  kind: 'accessibility',
                ),
              ),
              _PermissionTile(
                icon: Icons.camera_alt_outlined,
                title: l10n.settingsPermissionCamera,
                subtitle: l10n.settingsPermissionCameraHint,
                granted: state?.cameraPermissionGranted,
                onAction: _permissionAction(
                  granted: state?.cameraPermissionGranted,
                  request: runtime.requestCamera,
                  kind: 'camera',
                ),
              ),
              _PermissionTile(
                icon: Icons.sensors_outlined,
                title: l10n.settingsPermissionSensor,
                subtitle: l10n.settingsPermissionSensorHint,
                granted: state?.sensorPermissionGranted,
                onAction: _permissionAction(
                  granted: state?.sensorPermissionGranted,
                  request: runtime.requestSensor,
                  kind: 'sensor',
                ),
              ),
              _PermissionTile(
                icon: Icons.open_in_new_outlined,
                title: l10n.settingsPermissionOverlay,
                subtitle: l10n.settingsPermissionOverlayHint,
                granted: state?.windowAllowed,
                onAction: _permissionAction(
                  granted: state?.windowAllowed,
                  request: runtime.requestOverlay,
                  kind: 'overlay',
                ),
              ),
            ] else ...[
              _PermissionTile(
                icon: Icons.folder_open_outlined,
                title: l10n.settingsPermissionFileRead,
                subtitle: l10n.settingsPermissionFileReadHint,
                status: l10n.settingsPermissionSystemManaged,
              ),
            ],
          ],
        ),
      ),
    );
  }

  VoidCallback? _permissionAction({
    required bool? granted,
    required VoidCallback request,
    required String kind,
  }) {
    if (granted == null) return null;
    return granted ? () => runtime.openPermissionSettings(kind) : request;
  }
}

class _PermissionTile extends StatelessWidget {
  const _PermissionTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.granted,
    this.status,
    this.onAction,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool? granted;
  final String? status;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final resolvedStatus =
        status ??
        (granted == null
            ? l10n.settingsPermissionChecking
            : granted!
            ? l10n.settingsPermissionAllowed
            : l10n.settingsPermissionRequired);
    final statusColor = granted == true ? colorScheme.primary : null;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      dense: true,
      leading: Icon(icon),
      title: Text(title),
      subtitle: Text(subtitle),
      trailing: ConstrainedBox(
        constraints: const BoxConstraints(minWidth: 76),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              resolvedStatus,
              textAlign: TextAlign.end,
              style: TextStyle(
                color: statusColor,
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
            if (onAction != null)
              TextButton(
                onPressed: onAction,
                style: TextButton.styleFrom(
                  minimumSize: Size.zero,
                  padding: const EdgeInsets.only(top: 2),
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                child: Text(
                  granted == true
                      ? l10n.settingsPermissionManage
                      : l10n.settingsGrant,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _LocalModelSettingsPage extends StatefulWidget {
  const _LocalModelSettingsPage();

  @override
  State<_LocalModelSettingsPage> createState() =>
      _LocalModelSettingsPageState();
}

class _LocalModelSettingsPageState extends State<_LocalModelSettingsPage> {
  late final TextEditingController _model;
  late final TextEditingController _outputTokens;

  @override
  void initState() {
    super.initState();
    final state = AppScope.maybeOf(context)?.runtime.state;
    _model = TextEditingController(text: state?.modelId ?? '');
    _outputTokens = TextEditingController(
      text: '${state?.maxOutputTokens ?? 512}',
    );
  }

  @override
  void dispose() {
    _model.dispose();
    _outputTokens.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final runtime = AppScope.of(context).runtime;
    final l10n = AppLocalizations.of(context)!;
    return ListenableBuilder(
      listenable: runtime,
      builder: (context, _) {
        final state = runtime.state;
        final status = state?.modelLoading == true
            ? l10n.settingsModelLoading
            : state?.modelLoaded == true
            ? l10n.settingsModelReady
            : l10n.settingsModelNotLoaded;
        return ListView(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
          children: [
            _PageIntro(
              icon: Icons.memory_outlined,
              title: l10n.settingsLocalModelTitle,
              subtitle: l10n.settingsLocalModelSub,
            ),
            const SizedBox(height: 16),
            _SettingsCard(
              child: ListTile(
                leading: Icon(
                  state?.modelLoaded == true
                      ? Icons.check_circle_outline
                      : Icons.hourglass_empty_outlined,
                  color: state?.modelLoaded == true
                      ? Theme.of(context).colorScheme.primary
                      : null,
                ),
                title: Text(l10n.settingsModelStatus),
                subtitle: Text(status),
                trailing: Text(
                  state?.modelId.isNotEmpty == true ? state!.modelId : '--',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ),
            const SizedBox(height: 10),
            _SettingsCard(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.settingsModelTitle,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l10n.settingsModelHint,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: _model,
                      decoration: InputDecoration(
                        isDense: true,
                        labelText: l10n.settingsModelTitle,
                      ),
                    ),
                    const SizedBox(height: 8),
                    FilledButton.tonalIcon(
                      onPressed: () => runtime.updateSettings({
                        'modelId': _model.text.trim(),
                      }),
                      icon: const Icon(Icons.save_outlined),
                      label: Text(l10n.commonSave),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            _SettingsCard(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.settingsOutputTokens,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l10n.settingsOutputTokensHint(
                        state?.hardMaxOutputTokens ?? 32768,
                      ),
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: _outputTokens,
                      keyboardType: TextInputType.number,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      decoration: InputDecoration(
                        isDense: true,
                        labelText: l10n.settingsOutputTokens,
                      ),
                    ),
                    const SizedBox(height: 8),
                    FilledButton.tonalIcon(
                      onPressed: () => runtime.updateSettings({
                        'maxOutputTokens':
                            int.tryParse(_outputTokens.text) ?? 512,
                      }),
                      icon: const Icon(Icons.save_outlined),
                      label: Text(l10n.commonSave),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            _SettingsCard(
              child: ListTile(
                leading: const Icon(Icons.memory_outlined),
                title: Text(l10n.settingsReleaseModel),
                subtitle: Text(l10n.settingsReleaseModelHint),
                trailing: OutlinedButton.icon(
                  onPressed: state?.modelLoaded == true
                      ? runtime.releaseModel
                      : null,
                  icon: const Icon(Icons.cleaning_services_outlined),
                  label: Text(l10n.settingsRelease),
                ),
              ),
            ),
          ],
        );
      },
    );
  }

}

class _AboutSettingsPage extends StatelessWidget {
  const _AboutSettingsPage();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return ListView(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
      children: [
        _PageIntro(
          icon: Icons.info_outline,
          title: l10n.aboutTitle,
          subtitle: l10n.settingsAboutSub,
        ),
        const SizedBox(height: 16),
        _SettingsCard(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.router_outlined,
                      size: 34,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 12),
                    Text(
                      'Lociant',
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Text(l10n.aboutVersionLine(_appVersion)),
                const SizedBox(height: 12),
                Text(
                  l10n.settingsAboutBody,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 10),
        _SettingsCard(
          child: ListTile(
            leading: const Icon(Icons.hub_outlined),
            title: Text(l10n.settingsAboutRuntime),
            subtitle: Text(l10n.settingsAboutRuntimeSub),
          ),
        ),
      ],
    );
  }
}

class _PageIntro extends StatelessWidget {
  const _PageIntro({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  final IconData icon;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 28, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 3),
              Text(
                subtitle,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) => Text(
    label,
    style: Theme.of(context).textTheme.labelLarge?.copyWith(
      color: Theme.of(context).colorScheme.primary,
      fontWeight: FontWeight.w700,
    ),
  );
}

class _ChoiceCard extends StatelessWidget {
  const _ChoiceCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.child,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) => _SettingsCard(
    child: Padding(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 20),
              const SizedBox(width: 9),
              Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
            ],
          ),
          const SizedBox(height: 3),
          Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 10),
          child,
        ],
      ),
    ),
  );
}

class _SettingsEntry extends StatelessWidget {
  const _SettingsEntry({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(bottom: 10),
    child: ListTile(
      leading: Icon(icon),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    ),
  );
}

class _SettingsCard extends StatelessWidget {
  const _SettingsCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) =>
      Card(margin: EdgeInsets.zero, child: child);
}
