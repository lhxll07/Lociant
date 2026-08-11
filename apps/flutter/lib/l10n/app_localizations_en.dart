// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Lociant';

  @override
  String get navMenu => 'Menu';

  @override
  String get navHome => 'Home';

  @override
  String get navModels => 'Models';

  @override
  String get navNodes => 'Nodes';

  @override
  String get navSettings => 'Settings';

  @override
  String get nodesTitle => 'Nodes';

  @override
  String get nodesSubtitle => 'Lociant devices on your LAN';

  @override
  String get nodesRefresh => 'Refresh';

  @override
  String get nodesSelf => 'This device';

  @override
  String get nodesOnline => 'Online';

  @override
  String get nodesOffline => 'Offline';

  @override
  String get nodesEmpty =>
      'No other nodes discovered yet. Devices sharing your peer token will appear here automatically.';

  @override
  String get nodesPeersHint =>
      'Peer models appear in Models with a peer: prefix and can be selected directly.';

  @override
  String nodesError(Object error) {
    return 'Failed to load nodes: $error';
  }

  @override
  String get babyTitle => 'Baby Monitor';

  @override
  String get babySubtitle => 'SlumberGuard: status & events';

  @override
  String get babyNotEnabled =>
      'Baby monitor not enabled on this node\n(set babyCamera in config to enable)';

  @override
  String get babyState => 'State';

  @override
  String get babyMotion => 'Motion';

  @override
  String get babyEvents => 'Recent events';

  @override
  String get babyNoEvents => 'No events yet';

  @override
  String get babyStateIdle => 'Idle';

  @override
  String get babyStateCandidate => 'Observing';

  @override
  String get babyStateSoothing1 => 'Soothing 1';

  @override
  String get babyStateSoothing2 => 'Soothing 2';

  @override
  String get babyStateNotify => 'Notify parent';

  @override
  String get babyStateCooldown => 'Cooling down';

  @override
  String get nodesAdd => 'Add node';

  @override
  String get nodesAddFailed => 'Failed to add node';

  @override
  String get nodesDelete => 'Delete node';

  @override
  String get nodesDeleteFailed => 'Failed to delete node';

  @override
  String get commonCancel => 'Cancel';

  @override
  String get settingsPeerToken => 'Peer token (LAN networking)';

  @override
  String get settingsPeerTokenSave => 'Save';

  @override
  String get settingsPeerTokenHint =>
      'Nodes sharing this token appear automatically on the Nodes page; restart the service after changing it.';

  @override
  String get settingsModelServerDesktop =>
      'Desktop service runs automatically with the app (built-in Rust backend)';

  @override
  String get statusIdle => 'Idle';

  @override
  String get statusRunning => 'Running';

  @override
  String get statusStarting => 'Starting';

  @override
  String get statusStopped => 'Stopped';

  @override
  String get homePlaceholder => 'Ask Lociant, or describe a tool task';

  @override
  String get homeSend => 'Send';

  @override
  String get homeNewChat => 'New chat';

  @override
  String get homeHistory => 'Recent chats';

  @override
  String get homeEmptyReply => 'No reply';

  @override
  String get homeThinking => 'Thinking…';

  @override
  String get homeThought => 'Thought';

  @override
  String homeRunStatusTool(Object round, Object tool) {
    return 'Running tool $tool (round $round)…';
  }

  @override
  String homeRunStatusRound(Object round) {
    return 'Calling model (round $round)…';
  }

  @override
  String get homeRunStatusRetry => 'Retrying…';

  @override
  String homeRoundLabel(Object n) {
    return 'Round $n';
  }

  @override
  String get homeToolRunDone => 'Tools completed without a text reply.';

  @override
  String get homeImageAttached => 'Image attached';

  @override
  String get homeRemoveImage => 'Remove image';

  @override
  String get homeUploadImage => 'Upload photo';

  @override
  String get homeDeleteChat => 'Delete chat';

  @override
  String get onboardingSkip => 'Skip';

  @override
  String get onboardingNext => 'Next';

  @override
  String get onboardingStart => 'Get started';

  @override
  String get onboardingWelcomeTitle => 'Welcome to Lociant';

  @override
  String get onboardingWelcomeBody =>
      'Turn any device into a local agent that runs models, reads the screen, controls the UI and senses its environment — callable from Claude, Codex and other agents over MCP.';

  @override
  String get onboardingServerTitle => 'Local service';

  @override
  String get onboardingServerBody =>
      'The desktop app starts its bundled Rust service (127.0.0.1:11434) automatically. To connect a board or phone on your LAN, change the server address in Settings.';

  @override
  String get onboardingReadyTitle => 'Ready';

  @override
  String get onboardingReadyBody =>
      'Before you start: on Android, grant accessibility and other permissions in Settings; on headless Linux, run lociant-server --init first.';

  @override
  String get commonBack => 'Back';

  @override
  String get commonRefresh => 'Refresh';

  @override
  String get commonStart => 'Start';

  @override
  String get commonStop => 'Stop';

  @override
  String get commonInstall => 'Install';

  @override
  String get commonSave => 'Save';

  @override
  String get commonOpen => 'Open';

  @override
  String get commonCopy => 'Copy';

  @override
  String get modelsTitle => 'Models';

  @override
  String get modelsSubtitle => 'Install, select and manage local inference';

  @override
  String get modelsLocalTitle => 'Local models';

  @override
  String get modelsLocalSub => 'Installed model packages';

  @override
  String get modelsMarketTitle => 'Model market';

  @override
  String get modelsMarketSub => 'ModelScope MNN models';

  @override
  String get modelsRuntimeTitle => 'Runtime';

  @override
  String get modelsRuntimeSub => 'Default model and API';

  @override
  String get modelsCloudTitle => 'Cloud model';

  @override
  String get modelsCloudSub => 'OpenAI-compatible cloud API';

  @override
  String get modelsImport => 'Import';

  @override
  String get modelsRescan => 'Rescan';

  @override
  String get modelsInstalled => 'Installed';

  @override
  String get modelsInstalling => 'Installing';

  @override
  String get modelsInstall => 'Install';

  @override
  String get modelsDelete => 'Delete';

  @override
  String get emptyModels => 'No models yet';

  @override
  String get settingsTitle => 'Settings';

  @override
  String get settingsSubtitle => 'Runtime, permissions and model behavior';

  @override
  String get settingsLanguage => 'Language';

  @override
  String get settingsLanguageSub => 'Display language';

  @override
  String get settingsTheme => 'Theme';

  @override
  String get settingsThemeSub => 'UI color style';

  @override
  String get settingsThemeDark => 'Dark';

  @override
  String get settingsThemePink => 'Pink';

  @override
  String get settingsFollowSystem => 'System';

  @override
  String get settingsRuntimeTitle => 'Runtime';

  @override
  String get settingsRuntimeSub => 'Background service and floating window';

  @override
  String get settingsServerTitle => 'Server';

  @override
  String get settingsServerSub => 'Port, token, address';

  @override
  String get settingsModelTitle => 'Default model';

  @override
  String get settingsModelSub => 'Model and CPU threads';

  @override
  String get settingsAgentTitle => 'Agent';

  @override
  String get settingsAgentSub => 'Tool-loop behavior';

  @override
  String get settingsAdvancedTitle => 'Advanced';

  @override
  String get settingsAdvancedSub => 'Sessions and diagnostics';

  @override
  String get settingsAgentRounds => 'Max tool rounds';

  @override
  String settingsAgentRoundsSub(Object max, Object min) {
    return 'Model↔tool iterations per task ($min–$max)';
  }

  @override
  String get settingsToolCalls => 'Tool call cap';

  @override
  String get settingsPermissionsTitle => 'Permissions';

  @override
  String get settingsWindowVision => 'Window & vision';

  @override
  String get settingsWindowAuto => 'Auto show window';

  @override
  String get settingsVisionTitle => 'Vision';

  @override
  String get settingsToolExposure => 'Remote tools';

  @override
  String get settingsToolRead => 'Read';

  @override
  String get settingsToolSensor => 'Sensor';

  @override
  String get settingsToolAction => 'Action';

  @override
  String get settingsGenerate => 'Generate';

  @override
  String get settingsClear => 'Clear';

  @override
  String get settingsSessions => 'Sessions';

  @override
  String get settingsPerformanceMode => 'Performance mode';

  @override
  String get settingsPerformanceEco => 'Eco';

  @override
  String get settingsPerformanceBalanced => 'Balanced';

  @override
  String get settingsPerformanceFast => 'Fast';

  @override
  String get settingsInferenceBackend => 'Inference backend';

  @override
  String get settingsBackendModel => 'Follow model';

  @override
  String get settingsBackendAuto => 'Auto';

  @override
  String get settingsBackendCpu => 'CPU';

  @override
  String get settingsBackendOpencl => 'OpenCL (GPU)';

  @override
  String get settingsBackendVulkan => 'Vulkan (GPU)';

  @override
  String get settingsResponseLength => 'Response length';

  @override
  String get settingsLengthShort => 'Short';

  @override
  String get settingsLengthNormal => 'Normal';

  @override
  String get settingsLengthLong => 'Long';

  @override
  String get settingsContextMemory => 'Context memory';

  @override
  String get settingsContextLight => 'Light';

  @override
  String get settingsContextBalanced => 'Balanced';

  @override
  String get settingsContextDeep => 'Deep';

  @override
  String get settingsReleaseModel => 'Release model memory';

  @override
  String get settingsRelease => 'Release';

  @override
  String get settingsCloudTitle => 'Cloud model';

  @override
  String get settingsCloudBaseUrl => 'API base URL';

  @override
  String get settingsCloudApiKey => 'API key';

  @override
  String get settingsCloudModel => 'Model name';

  @override
  String get settingsCloudResponseLengthSub =>
      'Max output tokens (0 = provider default)';

  @override
  String get settingsCloudContextWindow => 'Context window';

  @override
  String get settingsCloudHistoryLimit => 'History messages';

  @override
  String get connectionOpenaiUrl => 'OpenAI Base URL';

  @override
  String get connectionMcpUrl => 'MCP URL';

  @override
  String get connectionAuthHeader => 'Auth header';

  @override
  String get connectionAuthDisabled => 'Authorization disabled';

  @override
  String connectionAuthHeaderValue(Object token) {
    return 'Authorization: Bearer $token';
  }

  @override
  String get aboutTitle => 'About Lociant';

  @override
  String aboutVersionLine(Object version) {
    return 'Version $version · Flutter UI';
  }

  @override
  String get settingsPort => 'Port';

  @override
  String get settingsOutputTokens => 'Output tokens';

  @override
  String get settingsApiToken => 'API token';

  @override
  String get settingsAutoStart => 'Start on boot';

  @override
  String get settingsModelServer => 'Service';

  @override
  String get settingsPermissionCamera => 'Camera';

  @override
  String get settingsPermissionNotification => 'Notifications';

  @override
  String get settingsPermissionOverlay => 'Floating window';

  @override
  String get settingsPermissionBattery => 'Background power';

  @override
  String get settingsPermissionAccessibility => 'Accessibility';

  @override
  String get settingsGrant => 'Grant';

  @override
  String get toastCopied => 'Copied';

  @override
  String get toastCopyFailed => 'Copy failed';

  @override
  String get toastImagePickerUnavailable =>
      'Image picking is not supported on this platform';

  @override
  String get toastModelImportFailed => 'Model import failed';

  @override
  String get toastModelDeleted => 'Model deleted';

  @override
  String get toastModelDeleteFailed => 'Model delete failed';

  @override
  String get toastModelsReloaded => 'Models reloaded';

  @override
  String get toastModelImported => 'Model imported';

  @override
  String get toastModelMarketLoaded => 'Market loaded';

  @override
  String get toastModelMarketFailed => 'Market unavailable';

  @override
  String get errorApiRequest => 'API request failed';
}
