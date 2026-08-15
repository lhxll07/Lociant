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
  String get navHome => 'Overview';

  @override
  String get navModels => 'Models';

  @override
  String get navNodes => 'Nodes';

  @override
  String get navExtensions => 'Extensions';

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
  String get nodesGridHint =>
      'Tap a device card for details and available actions';

  @override
  String get nodesAddress => 'Address';

  @override
  String get nodesPlatform => 'Platform';

  @override
  String get nodesNodeId => 'Node ID';

  @override
  String get nodesNameOptional => 'Name (optional)';

  @override
  String get nodesAvailableActions => 'Available actions';

  @override
  String get nodesOpenModels => 'Open models';

  @override
  String get nodesSelfHint =>
      'This is the current device and its local runtime.';

  @override
  String get nodesDevicePhone => 'Phone';

  @override
  String get nodesDeviceComputer => 'Computer';

  @override
  String get nodesDeviceBoard => 'Board';

  @override
  String get nodesDeviceOther => 'Device';

  @override
  String get nodesEmpty =>
      'No other nodes discovered yet. Devices sharing your peer token will appear here automatically.';

  @override
  String get nodesPeersHint =>
      'Peer models appear in Models with a peer: prefix and can be selected directly.';

  @override
  String get nodesGuideTitle => 'Connect your devices';

  @override
  String get nodesGuideBody =>
      'Give your phone, desktop and board the same node token; on the same LAN they discover each other automatically and share models and tools.';

  @override
  String get nodesGuideStep1 =>
      'Enter the same node token under Settings → Server on every device';

  @override
  String get nodesGuideStep2 =>
      'Stay on the same LAN and wait a moment, or tap + in the corner to add manually';

  @override
  String get nodesGuideStep3 =>
      'Open a node card to share models (peer: prefix) or view baby monitoring';

  @override
  String get nodesGuideOpenSettings => 'Open settings';

  @override
  String get nodesHelp => 'Interconnect guide';

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
  String get extensionsTitle => 'Extensions';

  @override
  String get extensionsSubtitle =>
      'Manage edge capabilities available on this device';

  @override
  String get extensionsInstalledTitle => 'Installed extensions';

  @override
  String get extensionsBuiltInHint =>
      'These extensions are bundled with Lociant and use this device\'s permissions.';

  @override
  String get extensionsBabyDescription =>
      'Monitor baby activity, record events and notify parents when attention is needed.';

  @override
  String get extensionsBabyPermissions =>
      'Requires camera, notifications and background operation';

  @override
  String get extensionsStatusChecking => 'Checking';

  @override
  String get extensionsStatusEnabled => 'Enabled';

  @override
  String get extensionsStatusNotConfigured => 'Not configured';

  @override
  String get extensionsStatusUnavailable => 'Temporarily unavailable';

  @override
  String get extensionsUnavailableHint =>
      'Unable to reach the current node. Make sure the service is running.';

  @override
  String get extensionsOpen => 'Open';

  @override
  String get extensionsRefresh => 'Refresh status';

  @override
  String get extensionsOpenSettings => 'Manage permissions';

  @override
  String get statusIdle => 'Idle';

  @override
  String get statusRunning => 'Running';

  @override
  String get statusStarting => 'Starting';

  @override
  String get statusStopped => 'Stopped';

  @override
  String get edgeOverviewTitle => 'Edge runtime';

  @override
  String get edgeOverviewSubtitle => 'Control plane for this device';

  @override
  String get edgeMetricModels => 'Models';

  @override
  String get edgeMetricNodes => 'Nodes';

  @override
  String get edgeMetricTools => 'Tools';

  @override
  String get edgeMetricStatus => 'Status';

  @override
  String get edgeNodesTitle => 'Connected devices';

  @override
  String get edgeNodesEmpty => 'No devices to show yet.';

  @override
  String get edgeViewAll => 'View all';

  @override
  String get edgeEndpointsTitle => 'Control endpoints';

  @override
  String get edgeControlApi => 'Control API';

  @override
  String get edgeToolsTitle => 'Available tools';

  @override
  String get edgeToolsEmpty =>
      'No tools are available at the current exposure level';

  @override
  String get toolDescriptionGeneric =>
      'Device capability available to controllers.';

  @override
  String get toolDescriptionRuntimeStatus => 'Check runtime status.';

  @override
  String get toolDescriptionModelList => 'List available models.';

  @override
  String get toolDescriptionDeviceStatus => 'Check basic device information.';

  @override
  String get toolDescriptionClipboardRead => 'Read the clipboard.';

  @override
  String get toolDescriptionClipboardWrite => 'Write text to the clipboard.';

  @override
  String get toolDescriptionAppOpen => 'Open an app.';

  @override
  String get toolDescriptionUiScreenState =>
      'Read the current screen structure.';

  @override
  String get toolDescriptionUiClickNode => 'Click a screen element.';

  @override
  String get toolDescriptionUiTap => 'Tap a screen position.';

  @override
  String get toolDescriptionUiSwipe => 'Swipe across the screen.';

  @override
  String get toolDescriptionUiWait => 'Wait for the screen to change.';

  @override
  String get toolDescriptionUiPaste => 'Paste text into the current field.';

  @override
  String get toolDescriptionUiSetText => 'Fill in a text field.';

  @override
  String get toolDescriptionVisionStatus => 'Check vision service status.';

  @override
  String get toolDescriptionVisionStart => 'Start the vision service.';

  @override
  String get toolDescriptionCameraCapture => 'Take a camera photo.';

  @override
  String get toolDescriptionVisionStop => 'Stop the vision service.';

  @override
  String get toolDescriptionSensorStatus => 'Check sensor status.';

  @override
  String get toolDescriptionSensorRead => 'Read sensor data.';

  @override
  String get toolDescriptionSensorStart => 'Start sensor collection.';

  @override
  String get toolDescriptionSensorStop => 'Stop sensor collection.';

  @override
  String get toolDescriptionFileList => 'List files in a directory.';

  @override
  String get toolDescriptionFileRead => 'Read a text file.';

  @override
  String get toolDescriptionFileWrite => 'Write a text file.';

  @override
  String get toolDescriptionProcessList => 'List running processes.';

  @override
  String get toolDescriptionProcessRun => 'Run a system command.';

  @override
  String get edgeOpenModels => 'Open models';

  @override
  String get edgeOpenNodes => 'Open nodes';

  @override
  String get edgeOpenSettings => 'Settings';

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
      'Turn any device into an edge runtime that runs local models, exposes device tools and senses its environment through a controlled API and MCP.';

  @override
  String get onboardingLocalTitle => 'Prepare a local model';

  @override
  String get onboardingLocalBody =>
      'Import or install a local model under Models so inference stays on the edge device.';

  @override
  String get onboardingPermissionTitle => 'Enable device capabilities';

  @override
  String get onboardingPermissionBody =>
      'Enable permissions in Settings: accessibility provides screen and UI tools; notifications keep the runtime alive; camera powers vision; set battery to unrestricted for reliable background operation.';

  @override
  String get onboardingNodesTitle => 'Connect your devices';

  @override
  String get onboardingNodesBody =>
      'Phones, desktops and boards on the same LAN with the same node token discover each other automatically. Use the Nodes page to view devices and share models and tools — e.g. let your phone use the RKLLM model running on the board.';

  @override
  String get onboardingReadyTitle => 'Ready';

  @override
  String get onboardingReadyBody =>
      'Start the runtime from the overview, inspect the available tools and connect external clients through the control API or MCP.';

  @override
  String get settingsOnboardingTitle => 'Onboarding';

  @override
  String get settingsOnboardingSub =>
      'Replay the setup walkthrough and node interconnection guide';

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
  String get modelsMarketSub => 'ModelScope GGUF models';

  @override
  String get modelsRuntimeTitle => 'Runtime';

  @override
  String get modelsRuntimeSub => 'Default model and API';

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
  String get settingsLanguageChinese => 'Chinese';

  @override
  String get settingsLanguageEnglish => 'English';

  @override
  String get settingsAppearanceTitle => 'Appearance';

  @override
  String get settingsSectionsTitle => 'Settings';

  @override
  String get settingsSecurityTitle => 'Security';

  @override
  String get settingsSecuritySub =>
      'API access, peer connections and tool permissions';

  @override
  String get settingsLocalModelTitle => 'Local model';

  @override
  String get settingsLocalModelSub =>
      'Default model and local inference behavior';

  @override
  String get settingsAboutSub => 'Version and runtime information';

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
  String get settingsModelSub => 'Installed model and response length';

  @override
  String get settingsAdvancedTitle => 'Advanced';

  @override
  String get settingsPermissionsTitle => 'Permissions';

  @override
  String get settingsPermissionsHint =>
      'These permissions control device tools; remote tool scope is set by the level below.';

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
  String get settingsToolExposureHint =>
      'Choose which device capabilities remote clients can use.';

  @override
  String get settingsGenerate => 'Generate';

  @override
  String get settingsClear => 'Clear';

  @override
  String get settingsPerformanceMode => 'Performance mode';

  @override
  String get settingsPerformanceEco => 'Eco';

  @override
  String get settingsPerformanceBalanced => 'Balanced';

  @override
  String get settingsPerformanceFast => 'Fast';

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
  String get settingsReleaseModelHint =>
      'Free memory used by the loaded model.';

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
  String get settingsApiTokenHint =>
      'Protects control API and remote tool calls.';

  @override
  String get settingsShowToken => 'Show token';

  @override
  String get settingsHideToken => 'Hide token';

  @override
  String get settingsModelStatus => 'Model status';

  @override
  String get settingsModelLoading => 'Loading';

  @override
  String get settingsModelReady => 'Ready';

  @override
  String get settingsModelNotLoaded => 'Not loaded';

  @override
  String get settingsModelHint => 'Use an installed model ID as the default.';

  @override
  String settingsOutputTokensHint(Object max) {
    return 'Maximum $max tokens per response.';
  }

  @override
  String get settingsAboutBody =>
      'Lociant turns phones, desktops and boards into edge runtimes for local models, device tools and controlled connections.';

  @override
  String get settingsAboutRuntime => 'Edge runtime';

  @override
  String get settingsAboutRuntimeSub =>
      'Local model inference and device capability control';

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
  String get settingsPermissionBackground => 'Background operation';

  @override
  String get settingsPermissionAccessibility => 'Accessibility';

  @override
  String get settingsPermissionSensor => 'Sensors';

  @override
  String get settingsPermissionNotificationHint =>
      'Keep the runtime visible and active in the background.';

  @override
  String get settingsPermissionBackgroundHint =>
      'Reduce system limits on the background service.';

  @override
  String get settingsPermissionAccessibilityHint =>
      'Read screen structure and perform UI actions.';

  @override
  String get settingsPermissionCameraHint =>
      'Let vision tools capture camera frames.';

  @override
  String get settingsPermissionSensorHint =>
      'Let sensor tools read motion, light and other data.';

  @override
  String get settingsPermissionOverlayHint =>
      'Let the runtime display a floating control.';

  @override
  String get settingsPermissionFileRead => 'File reading';

  @override
  String get settingsPermissionFileReadHint =>
      'Only paths accessible to the current system user can be read.';

  @override
  String get settingsPermissionSystemManaged => 'System managed';

  @override
  String get settingsPermissionAllowed => 'Allowed';

  @override
  String get settingsPermissionRequired => 'Needs access';

  @override
  String get settingsPermissionChecking => 'Checking';

  @override
  String get settingsPermissionManage => 'Manage';

  @override
  String get settingsDesktopPermissionsHint =>
      'Desktop has no device permissions. To use screen, sensor or camera tools, connect an Android phone or board node.';

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
