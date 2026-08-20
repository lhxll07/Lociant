import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_zh.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('zh'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'Lociant'**
  String get appTitle;

  /// No description provided for @navMenu.
  ///
  /// In en, this message translates to:
  /// **'Menu'**
  String get navMenu;

  /// No description provided for @navHome.
  ///
  /// In en, this message translates to:
  /// **'Overview'**
  String get navHome;

  /// No description provided for @navModels.
  ///
  /// In en, this message translates to:
  /// **'Models'**
  String get navModels;

  /// No description provided for @navNodes.
  ///
  /// In en, this message translates to:
  /// **'Nodes'**
  String get navNodes;

  /// No description provided for @navSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get navSettings;

  /// No description provided for @nodesTitle.
  ///
  /// In en, this message translates to:
  /// **'Nodes'**
  String get nodesTitle;

  /// No description provided for @nodesSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Lociant edge nodes on your LAN'**
  String get nodesSubtitle;

  /// No description provided for @nodesRefresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get nodesRefresh;

  /// No description provided for @nodesSelf.
  ///
  /// In en, this message translates to:
  /// **'This device'**
  String get nodesSelf;

  /// No description provided for @nodesOnline.
  ///
  /// In en, this message translates to:
  /// **'Online'**
  String get nodesOnline;

  /// No description provided for @nodesOffline.
  ///
  /// In en, this message translates to:
  /// **'Offline'**
  String get nodesOffline;

  /// No description provided for @nodesGridHint.
  ///
  /// In en, this message translates to:
  /// **'Tap a device card for details and available actions'**
  String get nodesGridHint;

  /// No description provided for @nodesAddress.
  ///
  /// In en, this message translates to:
  /// **'Address'**
  String get nodesAddress;

  /// No description provided for @nodesPlatform.
  ///
  /// In en, this message translates to:
  /// **'Platform'**
  String get nodesPlatform;

  /// No description provided for @nodesNodeId.
  ///
  /// In en, this message translates to:
  /// **'Node ID'**
  String get nodesNodeId;

  /// No description provided for @nodesNameOptional.
  ///
  /// In en, this message translates to:
  /// **'Name (optional)'**
  String get nodesNameOptional;

  /// No description provided for @nodesAvailableActions.
  ///
  /// In en, this message translates to:
  /// **'Available actions'**
  String get nodesAvailableActions;

  /// No description provided for @nodesOpenModels.
  ///
  /// In en, this message translates to:
  /// **'Open models'**
  String get nodesOpenModels;

  /// No description provided for @nodesSelfHint.
  ///
  /// In en, this message translates to:
  /// **'This is the current device and its local runtime.'**
  String get nodesSelfHint;

  /// No description provided for @nodesDevicePhone.
  ///
  /// In en, this message translates to:
  /// **'Phone'**
  String get nodesDevicePhone;

  /// No description provided for @nodesDeviceComputer.
  ///
  /// In en, this message translates to:
  /// **'Computer'**
  String get nodesDeviceComputer;

  /// No description provided for @nodesDeviceBoard.
  ///
  /// In en, this message translates to:
  /// **'Board'**
  String get nodesDeviceBoard;

  /// No description provided for @nodesDeviceOther.
  ///
  /// In en, this message translates to:
  /// **'Device'**
  String get nodesDeviceOther;

  /// No description provided for @nodesEmpty.
  ///
  /// In en, this message translates to:
  /// **'No other nodes discovered yet. Devices sharing your peer token will appear here automatically.'**
  String get nodesEmpty;

  /// No description provided for @nodesPeersHint.
  ///
  /// In en, this message translates to:
  /// **'Peer models appear in Models with a peer: prefix and can be selected directly.'**
  String get nodesPeersHint;

  /// No description provided for @nodesGuideTitle.
  ///
  /// In en, this message translates to:
  /// **'Connect your devices'**
  String get nodesGuideTitle;

  /// No description provided for @nodesGuideBody.
  ///
  /// In en, this message translates to:
  /// **'Give your phones, boards and other Lociant nodes the same token; on a trusted LAN they discover each other and share selected models and tools.'**
  String get nodesGuideBody;

  /// No description provided for @nodesGuideStep1.
  ///
  /// In en, this message translates to:
  /// **'Enter the same node token under Settings → Server on every device'**
  String get nodesGuideStep1;

  /// No description provided for @nodesGuideStep2.
  ///
  /// In en, this message translates to:
  /// **'Stay on the same LAN and wait a moment, or tap + in the corner to add manually'**
  String get nodesGuideStep2;

  /// No description provided for @nodesGuideStep3.
  ///
  /// In en, this message translates to:
  /// **'Open a node card to share models (peer: prefix) or inspect its connection details'**
  String get nodesGuideStep3;

  /// No description provided for @nodesGuideOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Open settings'**
  String get nodesGuideOpenSettings;

  /// No description provided for @nodesHelp.
  ///
  /// In en, this message translates to:
  /// **'Interconnect guide'**
  String get nodesHelp;

  /// No description provided for @nodesError.
  ///
  /// In en, this message translates to:
  /// **'Failed to load nodes: {error}'**
  String nodesError(Object error);

  /// No description provided for @nodesAdd.
  ///
  /// In en, this message translates to:
  /// **'Add node'**
  String get nodesAdd;

  /// No description provided for @nodesAddFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to add node'**
  String get nodesAddFailed;

  /// No description provided for @nodesDelete.
  ///
  /// In en, this message translates to:
  /// **'Delete node'**
  String get nodesDelete;

  /// No description provided for @nodesDeleteFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to delete node'**
  String get nodesDeleteFailed;

  /// No description provided for @commonCancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get commonCancel;

  /// No description provided for @settingsPeerToken.
  ///
  /// In en, this message translates to:
  /// **'Peer token (LAN networking)'**
  String get settingsPeerToken;

  /// No description provided for @settingsPeerTokenSave.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get settingsPeerTokenSave;

  /// No description provided for @settingsPeerTokenHint.
  ///
  /// In en, this message translates to:
  /// **'Nodes sharing this token appear automatically on the Nodes page; restart the service after changing it.'**
  String get settingsPeerTokenHint;

  /// No description provided for @settingsModelServerDesktop.
  ///
  /// In en, this message translates to:
  /// **'Desktop service runs automatically with the app (built-in Rust backend)'**
  String get settingsModelServerDesktop;

  /// No description provided for @statusIdle.
  ///
  /// In en, this message translates to:
  /// **'Idle'**
  String get statusIdle;

  /// No description provided for @statusRunning.
  ///
  /// In en, this message translates to:
  /// **'Running'**
  String get statusRunning;

  /// No description provided for @statusStarting.
  ///
  /// In en, this message translates to:
  /// **'Starting'**
  String get statusStarting;

  /// No description provided for @statusStopped.
  ///
  /// In en, this message translates to:
  /// **'Stopped'**
  String get statusStopped;

  /// No description provided for @edgeOverviewTitle.
  ///
  /// In en, this message translates to:
  /// **'Edge runtime'**
  String get edgeOverviewTitle;

  /// No description provided for @edgeOverviewSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Local control for this edge node'**
  String get edgeOverviewSubtitle;

  /// No description provided for @edgeMetricModels.
  ///
  /// In en, this message translates to:
  /// **'Models'**
  String get edgeMetricModels;

  /// No description provided for @edgeMetricNodes.
  ///
  /// In en, this message translates to:
  /// **'Nodes'**
  String get edgeMetricNodes;

  /// No description provided for @edgeMetricTools.
  ///
  /// In en, this message translates to:
  /// **'Tools'**
  String get edgeMetricTools;

  /// No description provided for @edgeMetricStatus.
  ///
  /// In en, this message translates to:
  /// **'Status'**
  String get edgeMetricStatus;

  /// No description provided for @edgeNodesTitle.
  ///
  /// In en, this message translates to:
  /// **'Connected devices'**
  String get edgeNodesTitle;

  /// No description provided for @edgeNodesEmpty.
  ///
  /// In en, this message translates to:
  /// **'No devices to show yet.'**
  String get edgeNodesEmpty;

  /// No description provided for @edgeViewAll.
  ///
  /// In en, this message translates to:
  /// **'View all'**
  String get edgeViewAll;

  /// No description provided for @edgeEndpointsTitle.
  ///
  /// In en, this message translates to:
  /// **'Control endpoints'**
  String get edgeEndpointsTitle;

  /// No description provided for @edgeControlApi.
  ///
  /// In en, this message translates to:
  /// **'Control API'**
  String get edgeControlApi;

  /// No description provided for @edgeToolsTitle.
  ///
  /// In en, this message translates to:
  /// **'Available tools'**
  String get edgeToolsTitle;

  /// No description provided for @edgeToolsEmpty.
  ///
  /// In en, this message translates to:
  /// **'No tools are available at the current exposure level'**
  String get edgeToolsEmpty;

  /// No description provided for @readinessTitle.
  ///
  /// In en, this message translates to:
  /// **'Node readiness'**
  String get readinessTitle;

  /// No description provided for @readinessSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Check the runtime before connecting a desktop or cloud client.'**
  String get readinessSubtitle;

  /// No description provided for @readinessRefresh.
  ///
  /// In en, this message translates to:
  /// **'Run checks'**
  String get readinessRefresh;

  /// No description provided for @readinessSummary.
  ///
  /// In en, this message translates to:
  /// **'{ready} of {total} required checks ready'**
  String readinessSummary(Object ready, Object total);

  /// No description provided for @readinessReady.
  ///
  /// In en, this message translates to:
  /// **'Ready'**
  String get readinessReady;

  /// No description provided for @readinessAttention.
  ///
  /// In en, this message translates to:
  /// **'Attention'**
  String get readinessAttention;

  /// No description provided for @readinessBlocked.
  ///
  /// In en, this message translates to:
  /// **'Blocked'**
  String get readinessBlocked;

  /// No description provided for @readinessChecking.
  ///
  /// In en, this message translates to:
  /// **'Checking'**
  String get readinessChecking;

  /// No description provided for @readinessOptional.
  ///
  /// In en, this message translates to:
  /// **'Optional'**
  String get readinessOptional;

  /// No description provided for @readinessRuntime.
  ///
  /// In en, this message translates to:
  /// **'Runtime'**
  String get readinessRuntime;

  /// No description provided for @readinessRuntimeHint.
  ///
  /// In en, this message translates to:
  /// **'Foreground service and Rust backend'**
  String get readinessRuntimeHint;

  /// No description provided for @readinessConnection.
  ///
  /// In en, this message translates to:
  /// **'Connection'**
  String get readinessConnection;

  /// No description provided for @readinessConnectionHint.
  ///
  /// In en, this message translates to:
  /// **'Health endpoint and local control plane'**
  String get readinessConnectionHint;

  /// No description provided for @readinessTools.
  ///
  /// In en, this message translates to:
  /// **'Device tools'**
  String get readinessTools;

  /// No description provided for @readinessToolsHint.
  ///
  /// In en, this message translates to:
  /// **'A safe read-only capability responds'**
  String get readinessToolsHint;

  /// No description provided for @readinessPermissions.
  ///
  /// In en, this message translates to:
  /// **'Permissions'**
  String get readinessPermissions;

  /// No description provided for @readinessPermissionsHint.
  ///
  /// In en, this message translates to:
  /// **'Background operation and device access'**
  String get readinessPermissionsHint;

  /// No description provided for @readinessSecurity.
  ///
  /// In en, this message translates to:
  /// **'Network security'**
  String get readinessSecurity;

  /// No description provided for @readinessSecurityHint.
  ///
  /// In en, this message translates to:
  /// **'API token protects remote calls'**
  String get readinessSecurityHint;

  /// No description provided for @readinessModel.
  ///
  /// In en, this message translates to:
  /// **'Local model'**
  String get readinessModel;

  /// No description provided for @readinessModelHint.
  ///
  /// In en, this message translates to:
  /// **'Optional local inference'**
  String get readinessModelHint;

  /// No description provided for @toolDescriptionGeneric.
  ///
  /// In en, this message translates to:
  /// **'Device capability available to controllers.'**
  String get toolDescriptionGeneric;

  /// No description provided for @toolDescriptionRuntimeStatus.
  ///
  /// In en, this message translates to:
  /// **'Check runtime status.'**
  String get toolDescriptionRuntimeStatus;

  /// No description provided for @toolDescriptionModelList.
  ///
  /// In en, this message translates to:
  /// **'List available models.'**
  String get toolDescriptionModelList;

  /// No description provided for @toolDescriptionDeviceStatus.
  ///
  /// In en, this message translates to:
  /// **'Check basic device information.'**
  String get toolDescriptionDeviceStatus;

  /// No description provided for @toolDescriptionClipboardRead.
  ///
  /// In en, this message translates to:
  /// **'Read the clipboard.'**
  String get toolDescriptionClipboardRead;

  /// No description provided for @toolDescriptionClipboardWrite.
  ///
  /// In en, this message translates to:
  /// **'Write text to the clipboard.'**
  String get toolDescriptionClipboardWrite;

  /// No description provided for @toolDescriptionAppOpen.
  ///
  /// In en, this message translates to:
  /// **'Open an app.'**
  String get toolDescriptionAppOpen;

  /// No description provided for @toolDescriptionUiScreenState.
  ///
  /// In en, this message translates to:
  /// **'Read the current screen structure.'**
  String get toolDescriptionUiScreenState;

  /// No description provided for @toolDescriptionUiClickNode.
  ///
  /// In en, this message translates to:
  /// **'Click a screen element.'**
  String get toolDescriptionUiClickNode;

  /// No description provided for @toolDescriptionUiTap.
  ///
  /// In en, this message translates to:
  /// **'Tap a screen position.'**
  String get toolDescriptionUiTap;

  /// No description provided for @toolDescriptionUiSwipe.
  ///
  /// In en, this message translates to:
  /// **'Swipe across the screen.'**
  String get toolDescriptionUiSwipe;

  /// No description provided for @toolDescriptionUiWait.
  ///
  /// In en, this message translates to:
  /// **'Wait for the screen to change.'**
  String get toolDescriptionUiWait;

  /// No description provided for @toolDescriptionUiPaste.
  ///
  /// In en, this message translates to:
  /// **'Paste text into the current field.'**
  String get toolDescriptionUiPaste;

  /// No description provided for @toolDescriptionUiSetText.
  ///
  /// In en, this message translates to:
  /// **'Fill in a text field.'**
  String get toolDescriptionUiSetText;

  /// No description provided for @toolDescriptionVisionStatus.
  ///
  /// In en, this message translates to:
  /// **'Check vision service status.'**
  String get toolDescriptionVisionStatus;

  /// No description provided for @toolDescriptionVisionStart.
  ///
  /// In en, this message translates to:
  /// **'Start the vision service.'**
  String get toolDescriptionVisionStart;

  /// No description provided for @toolDescriptionCameraCapture.
  ///
  /// In en, this message translates to:
  /// **'Take a camera photo.'**
  String get toolDescriptionCameraCapture;

  /// No description provided for @toolDescriptionVisionStop.
  ///
  /// In en, this message translates to:
  /// **'Stop the vision service.'**
  String get toolDescriptionVisionStop;

  /// No description provided for @toolDescriptionSensorStatus.
  ///
  /// In en, this message translates to:
  /// **'Check sensor status.'**
  String get toolDescriptionSensorStatus;

  /// No description provided for @toolDescriptionSensorRead.
  ///
  /// In en, this message translates to:
  /// **'Read sensor data.'**
  String get toolDescriptionSensorRead;

  /// No description provided for @toolDescriptionSensorStart.
  ///
  /// In en, this message translates to:
  /// **'Start sensor collection.'**
  String get toolDescriptionSensorStart;

  /// No description provided for @toolDescriptionSensorStop.
  ///
  /// In en, this message translates to:
  /// **'Stop sensor collection.'**
  String get toolDescriptionSensorStop;

  /// No description provided for @toolDescriptionFileList.
  ///
  /// In en, this message translates to:
  /// **'List files in a directory.'**
  String get toolDescriptionFileList;

  /// No description provided for @toolDescriptionFileRead.
  ///
  /// In en, this message translates to:
  /// **'Read a text file.'**
  String get toolDescriptionFileRead;

  /// No description provided for @toolDescriptionFileWrite.
  ///
  /// In en, this message translates to:
  /// **'Write a text file.'**
  String get toolDescriptionFileWrite;

  /// No description provided for @toolDescriptionProcessList.
  ///
  /// In en, this message translates to:
  /// **'List running processes.'**
  String get toolDescriptionProcessList;

  /// No description provided for @toolDescriptionProcessRun.
  ///
  /// In en, this message translates to:
  /// **'Run a system command.'**
  String get toolDescriptionProcessRun;

  /// No description provided for @edgeOpenModels.
  ///
  /// In en, this message translates to:
  /// **'Open models'**
  String get edgeOpenModels;

  /// No description provided for @edgeOpenNodes.
  ///
  /// In en, this message translates to:
  /// **'Open nodes'**
  String get edgeOpenNodes;

  /// No description provided for @edgeOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get edgeOpenSettings;

  /// No description provided for @onboardingSkip.
  ///
  /// In en, this message translates to:
  /// **'Skip'**
  String get onboardingSkip;

  /// No description provided for @onboardingNext.
  ///
  /// In en, this message translates to:
  /// **'Next'**
  String get onboardingNext;

  /// No description provided for @onboardingStart.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get onboardingStart;

  /// No description provided for @onboardingWelcomeTitle.
  ///
  /// In en, this message translates to:
  /// **'Welcome to Lociant'**
  String get onboardingWelcomeTitle;

  /// No description provided for @onboardingWelcomeBody.
  ///
  /// In en, this message translates to:
  /// **'Give an overlooked device a useful role at the edge: run local models, expose hardware capabilities and connect through a controlled API or MCP.'**
  String get onboardingWelcomeBody;

  /// No description provided for @onboardingLocalTitle.
  ///
  /// In en, this message translates to:
  /// **'Add local compute'**
  String get onboardingLocalTitle;

  /// No description provided for @onboardingLocalBody.
  ///
  /// In en, this message translates to:
  /// **'Import a GGUF model under Models when this node needs local inference. Device tools can run without a local model.'**
  String get onboardingLocalBody;

  /// No description provided for @onboardingPermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'Enable hardware capabilities'**
  String get onboardingPermissionTitle;

  /// No description provided for @onboardingPermissionBody.
  ///
  /// In en, this message translates to:
  /// **'Grant only what this node needs. Android permissions unlock screen, camera, sensor and background capabilities; other platforms expose their own tools.'**
  String get onboardingPermissionBody;

  /// No description provided for @onboardingNodesTitle.
  ///
  /// In en, this message translates to:
  /// **'Connect edge nodes'**
  String get onboardingNodesTitle;

  /// No description provided for @onboardingNodesBody.
  ///
  /// In en, this message translates to:
  /// **'Use a shared node token to connect phones, boards and other Lociant nodes on a trusted LAN. The desktop can inspect and control them.'**
  String get onboardingNodesBody;

  /// No description provided for @onboardingReadyTitle.
  ///
  /// In en, this message translates to:
  /// **'Ready'**
  String get onboardingReadyTitle;

  /// No description provided for @onboardingReadyBody.
  ///
  /// In en, this message translates to:
  /// **'Start the runtime, verify this node\'s capabilities and connect a desktop or cloud client through the control API or MCP.'**
  String get onboardingReadyBody;

  /// No description provided for @settingsOnboardingTitle.
  ///
  /// In en, this message translates to:
  /// **'Onboarding'**
  String get settingsOnboardingTitle;

  /// No description provided for @settingsOnboardingSub.
  ///
  /// In en, this message translates to:
  /// **'Replay the setup walkthrough and node interconnection guide'**
  String get settingsOnboardingSub;

  /// No description provided for @commonBack.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get commonBack;

  /// No description provided for @commonRefresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get commonRefresh;

  /// No description provided for @commonStart.
  ///
  /// In en, this message translates to:
  /// **'Start'**
  String get commonStart;

  /// No description provided for @commonStop.
  ///
  /// In en, this message translates to:
  /// **'Stop'**
  String get commonStop;

  /// No description provided for @commonInstall.
  ///
  /// In en, this message translates to:
  /// **'Install'**
  String get commonInstall;

  /// No description provided for @commonSave.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get commonSave;

  /// No description provided for @commonOpen.
  ///
  /// In en, this message translates to:
  /// **'Open'**
  String get commonOpen;

  /// No description provided for @commonCopy.
  ///
  /// In en, this message translates to:
  /// **'Copy'**
  String get commonCopy;

  /// No description provided for @modelsTitle.
  ///
  /// In en, this message translates to:
  /// **'Models'**
  String get modelsTitle;

  /// No description provided for @modelsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Manage optional local inference'**
  String get modelsSubtitle;

  /// No description provided for @modelsLocalTitle.
  ///
  /// In en, this message translates to:
  /// **'Local models'**
  String get modelsLocalTitle;

  /// No description provided for @modelsLocalSub.
  ///
  /// In en, this message translates to:
  /// **'Installed model packages'**
  String get modelsLocalSub;

  /// No description provided for @modelsMarketTitle.
  ///
  /// In en, this message translates to:
  /// **'Model market'**
  String get modelsMarketTitle;

  /// No description provided for @modelsMarketSub.
  ///
  /// In en, this message translates to:
  /// **'ModelScope GGUF models'**
  String get modelsMarketSub;

  /// No description provided for @modelsRuntimeTitle.
  ///
  /// In en, this message translates to:
  /// **'Runtime'**
  String get modelsRuntimeTitle;

  /// No description provided for @modelsRuntimeSub.
  ///
  /// In en, this message translates to:
  /// **'Default model and API'**
  String get modelsRuntimeSub;

  /// No description provided for @modelsImport.
  ///
  /// In en, this message translates to:
  /// **'Import'**
  String get modelsImport;

  /// No description provided for @modelsRescan.
  ///
  /// In en, this message translates to:
  /// **'Rescan'**
  String get modelsRescan;

  /// No description provided for @modelsInstalled.
  ///
  /// In en, this message translates to:
  /// **'Installed'**
  String get modelsInstalled;

  /// No description provided for @modelsInstalling.
  ///
  /// In en, this message translates to:
  /// **'Installing'**
  String get modelsInstalling;

  /// No description provided for @modelsInstall.
  ///
  /// In en, this message translates to:
  /// **'Install'**
  String get modelsInstall;

  /// No description provided for @modelsDelete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get modelsDelete;

  /// No description provided for @emptyModels.
  ///
  /// In en, this message translates to:
  /// **'No models yet'**
  String get emptyModels;

  /// No description provided for @settingsTitle.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settingsTitle;

  /// No description provided for @settingsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Runtime, permissions and model behavior'**
  String get settingsSubtitle;

  /// No description provided for @settingsLanguage.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get settingsLanguage;

  /// No description provided for @settingsLanguageSub.
  ///
  /// In en, this message translates to:
  /// **'Display language'**
  String get settingsLanguageSub;

  /// No description provided for @settingsTheme.
  ///
  /// In en, this message translates to:
  /// **'Theme'**
  String get settingsTheme;

  /// No description provided for @settingsThemeSub.
  ///
  /// In en, this message translates to:
  /// **'UI color style'**
  String get settingsThemeSub;

  /// No description provided for @settingsThemeDark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get settingsThemeDark;

  /// No description provided for @settingsThemePink.
  ///
  /// In en, this message translates to:
  /// **'Pink'**
  String get settingsThemePink;

  /// No description provided for @settingsFollowSystem.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get settingsFollowSystem;

  /// No description provided for @settingsLanguageChinese.
  ///
  /// In en, this message translates to:
  /// **'Chinese'**
  String get settingsLanguageChinese;

  /// No description provided for @settingsLanguageEnglish.
  ///
  /// In en, this message translates to:
  /// **'English'**
  String get settingsLanguageEnglish;

  /// No description provided for @settingsAppearanceTitle.
  ///
  /// In en, this message translates to:
  /// **'Appearance'**
  String get settingsAppearanceTitle;

  /// No description provided for @settingsSectionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settingsSectionsTitle;

  /// No description provided for @settingsSecurityTitle.
  ///
  /// In en, this message translates to:
  /// **'Security'**
  String get settingsSecurityTitle;

  /// No description provided for @settingsSecuritySub.
  ///
  /// In en, this message translates to:
  /// **'API access, peer connections and tool permissions'**
  String get settingsSecuritySub;

  /// No description provided for @settingsLocalModelTitle.
  ///
  /// In en, this message translates to:
  /// **'Local model'**
  String get settingsLocalModelTitle;

  /// No description provided for @settingsLocalModelSub.
  ///
  /// In en, this message translates to:
  /// **'Default model and local inference behavior'**
  String get settingsLocalModelSub;

  /// No description provided for @settingsAboutSub.
  ///
  /// In en, this message translates to:
  /// **'Version and runtime information'**
  String get settingsAboutSub;

  /// No description provided for @settingsRuntimeTitle.
  ///
  /// In en, this message translates to:
  /// **'Runtime'**
  String get settingsRuntimeTitle;

  /// No description provided for @settingsRuntimeSub.
  ///
  /// In en, this message translates to:
  /// **'Background service and floating window'**
  String get settingsRuntimeSub;

  /// No description provided for @settingsServerTitle.
  ///
  /// In en, this message translates to:
  /// **'Server'**
  String get settingsServerTitle;

  /// No description provided for @settingsServerSub.
  ///
  /// In en, this message translates to:
  /// **'Port, token, address'**
  String get settingsServerSub;

  /// No description provided for @settingsModelTitle.
  ///
  /// In en, this message translates to:
  /// **'Default model'**
  String get settingsModelTitle;

  /// No description provided for @settingsModelSub.
  ///
  /// In en, this message translates to:
  /// **'Installed model and response length'**
  String get settingsModelSub;

  /// No description provided for @settingsAdvancedTitle.
  ///
  /// In en, this message translates to:
  /// **'Advanced'**
  String get settingsAdvancedTitle;

  /// No description provided for @settingsPermissionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Permissions'**
  String get settingsPermissionsTitle;

  /// No description provided for @settingsPermissionsHint.
  ///
  /// In en, this message translates to:
  /// **'These permissions control device tools; remote tool scope is set by the level below.'**
  String get settingsPermissionsHint;

  /// No description provided for @settingsWindowVision.
  ///
  /// In en, this message translates to:
  /// **'Window & vision'**
  String get settingsWindowVision;

  /// No description provided for @settingsWindowAuto.
  ///
  /// In en, this message translates to:
  /// **'Auto show window'**
  String get settingsWindowAuto;

  /// No description provided for @settingsVisionTitle.
  ///
  /// In en, this message translates to:
  /// **'Vision'**
  String get settingsVisionTitle;

  /// No description provided for @settingsToolExposure.
  ///
  /// In en, this message translates to:
  /// **'Remote tools'**
  String get settingsToolExposure;

  /// No description provided for @settingsToolRead.
  ///
  /// In en, this message translates to:
  /// **'Read'**
  String get settingsToolRead;

  /// No description provided for @settingsToolSensor.
  ///
  /// In en, this message translates to:
  /// **'Sensor'**
  String get settingsToolSensor;

  /// No description provided for @settingsToolAction.
  ///
  /// In en, this message translates to:
  /// **'Action'**
  String get settingsToolAction;

  /// No description provided for @settingsToolExposureHint.
  ///
  /// In en, this message translates to:
  /// **'Choose which device capabilities remote clients can use.'**
  String get settingsToolExposureHint;

  /// No description provided for @settingsGenerate.
  ///
  /// In en, this message translates to:
  /// **'Generate'**
  String get settingsGenerate;

  /// No description provided for @settingsClear.
  ///
  /// In en, this message translates to:
  /// **'Clear'**
  String get settingsClear;

  /// No description provided for @settingsPerformanceMode.
  ///
  /// In en, this message translates to:
  /// **'Performance mode'**
  String get settingsPerformanceMode;

  /// No description provided for @settingsPerformanceEco.
  ///
  /// In en, this message translates to:
  /// **'Eco'**
  String get settingsPerformanceEco;

  /// No description provided for @settingsPerformanceBalanced.
  ///
  /// In en, this message translates to:
  /// **'Balanced'**
  String get settingsPerformanceBalanced;

  /// No description provided for @settingsPerformanceFast.
  ///
  /// In en, this message translates to:
  /// **'Fast'**
  String get settingsPerformanceFast;

  /// No description provided for @settingsResponseLength.
  ///
  /// In en, this message translates to:
  /// **'Response length'**
  String get settingsResponseLength;

  /// No description provided for @settingsLengthShort.
  ///
  /// In en, this message translates to:
  /// **'Short'**
  String get settingsLengthShort;

  /// No description provided for @settingsLengthNormal.
  ///
  /// In en, this message translates to:
  /// **'Normal'**
  String get settingsLengthNormal;

  /// No description provided for @settingsLengthLong.
  ///
  /// In en, this message translates to:
  /// **'Long'**
  String get settingsLengthLong;

  /// No description provided for @settingsContextMemory.
  ///
  /// In en, this message translates to:
  /// **'Context memory'**
  String get settingsContextMemory;

  /// No description provided for @settingsContextLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get settingsContextLight;

  /// No description provided for @settingsContextBalanced.
  ///
  /// In en, this message translates to:
  /// **'Balanced'**
  String get settingsContextBalanced;

  /// No description provided for @settingsContextDeep.
  ///
  /// In en, this message translates to:
  /// **'Deep'**
  String get settingsContextDeep;

  /// No description provided for @settingsReleaseModel.
  ///
  /// In en, this message translates to:
  /// **'Release model memory'**
  String get settingsReleaseModel;

  /// No description provided for @settingsRelease.
  ///
  /// In en, this message translates to:
  /// **'Release'**
  String get settingsRelease;

  /// No description provided for @settingsReleaseModelHint.
  ///
  /// In en, this message translates to:
  /// **'Free memory used by the loaded model.'**
  String get settingsReleaseModelHint;

  /// No description provided for @connectionMcpUrl.
  ///
  /// In en, this message translates to:
  /// **'MCP URL'**
  String get connectionMcpUrl;

  /// No description provided for @connectionAuthHeader.
  ///
  /// In en, this message translates to:
  /// **'Auth header'**
  String get connectionAuthHeader;

  /// No description provided for @connectionAuthDisabled.
  ///
  /// In en, this message translates to:
  /// **'Authorization disabled'**
  String get connectionAuthDisabled;

  /// No description provided for @connectionAuthHeaderValue.
  ///
  /// In en, this message translates to:
  /// **'Authorization: Bearer {token}'**
  String connectionAuthHeaderValue(Object token);

  /// No description provided for @aboutTitle.
  ///
  /// In en, this message translates to:
  /// **'About Lociant'**
  String get aboutTitle;

  /// No description provided for @aboutVersionLine.
  ///
  /// In en, this message translates to:
  /// **'Version {version} · Flutter UI'**
  String aboutVersionLine(Object version);

  /// No description provided for @settingsPort.
  ///
  /// In en, this message translates to:
  /// **'Port'**
  String get settingsPort;

  /// No description provided for @settingsOutputTokens.
  ///
  /// In en, this message translates to:
  /// **'Output tokens'**
  String get settingsOutputTokens;

  /// No description provided for @settingsApiToken.
  ///
  /// In en, this message translates to:
  /// **'API token'**
  String get settingsApiToken;

  /// No description provided for @settingsApiTokenHint.
  ///
  /// In en, this message translates to:
  /// **'Protects control API and remote tool calls.'**
  String get settingsApiTokenHint;

  /// No description provided for @settingsShowToken.
  ///
  /// In en, this message translates to:
  /// **'Show token'**
  String get settingsShowToken;

  /// No description provided for @settingsHideToken.
  ///
  /// In en, this message translates to:
  /// **'Hide token'**
  String get settingsHideToken;

  /// No description provided for @settingsModelStatus.
  ///
  /// In en, this message translates to:
  /// **'Model status'**
  String get settingsModelStatus;

  /// No description provided for @settingsModelLoading.
  ///
  /// In en, this message translates to:
  /// **'Loading'**
  String get settingsModelLoading;

  /// No description provided for @settingsModelReady.
  ///
  /// In en, this message translates to:
  /// **'Ready'**
  String get settingsModelReady;

  /// No description provided for @settingsModelNotLoaded.
  ///
  /// In en, this message translates to:
  /// **'Not loaded'**
  String get settingsModelNotLoaded;

  /// No description provided for @settingsModelHint.
  ///
  /// In en, this message translates to:
  /// **'Use an installed model ID as the default.'**
  String get settingsModelHint;

  /// No description provided for @settingsOutputTokensHint.
  ///
  /// In en, this message translates to:
  /// **'Maximum {max} tokens per response.'**
  String settingsOutputTokensHint(Object max);

  /// No description provided for @settingsAboutBody.
  ///
  /// In en, this message translates to:
  /// **'Lociant is a local runtime for low-power, always-on devices close to the physical world. It provides local execution, hardware capabilities and controlled connections.'**
  String get settingsAboutBody;

  /// No description provided for @settingsAboutRuntime.
  ///
  /// In en, this message translates to:
  /// **'Edge runtime'**
  String get settingsAboutRuntime;

  /// No description provided for @settingsAboutRuntimeSub.
  ///
  /// In en, this message translates to:
  /// **'Local execution and hardware capability control'**
  String get settingsAboutRuntimeSub;

  /// No description provided for @settingsAutoStart.
  ///
  /// In en, this message translates to:
  /// **'Start on boot'**
  String get settingsAutoStart;

  /// No description provided for @settingsModelServer.
  ///
  /// In en, this message translates to:
  /// **'Service'**
  String get settingsModelServer;

  /// No description provided for @settingsPermissionCamera.
  ///
  /// In en, this message translates to:
  /// **'Camera'**
  String get settingsPermissionCamera;

  /// No description provided for @settingsPermissionNotification.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get settingsPermissionNotification;

  /// No description provided for @settingsPermissionOverlay.
  ///
  /// In en, this message translates to:
  /// **'Floating window'**
  String get settingsPermissionOverlay;

  /// No description provided for @settingsPermissionBattery.
  ///
  /// In en, this message translates to:
  /// **'Background power'**
  String get settingsPermissionBattery;

  /// No description provided for @settingsPermissionBackground.
  ///
  /// In en, this message translates to:
  /// **'Background operation'**
  String get settingsPermissionBackground;

  /// No description provided for @settingsPermissionAccessibility.
  ///
  /// In en, this message translates to:
  /// **'Accessibility'**
  String get settingsPermissionAccessibility;

  /// No description provided for @settingsPermissionSensor.
  ///
  /// In en, this message translates to:
  /// **'Sensors'**
  String get settingsPermissionSensor;

  /// No description provided for @settingsPermissionNotificationHint.
  ///
  /// In en, this message translates to:
  /// **'Keep the runtime visible and active in the background.'**
  String get settingsPermissionNotificationHint;

  /// No description provided for @settingsPermissionBackgroundHint.
  ///
  /// In en, this message translates to:
  /// **'Reduce system limits on the background service.'**
  String get settingsPermissionBackgroundHint;

  /// No description provided for @settingsPermissionAccessibilityHint.
  ///
  /// In en, this message translates to:
  /// **'Read screen structure and perform UI actions.'**
  String get settingsPermissionAccessibilityHint;

  /// No description provided for @settingsPermissionCameraHint.
  ///
  /// In en, this message translates to:
  /// **'Let vision tools capture camera frames.'**
  String get settingsPermissionCameraHint;

  /// No description provided for @settingsPermissionSensorHint.
  ///
  /// In en, this message translates to:
  /// **'Let sensor tools read motion, light and other data.'**
  String get settingsPermissionSensorHint;

  /// No description provided for @settingsPermissionOverlayHint.
  ///
  /// In en, this message translates to:
  /// **'Let the runtime display a floating control.'**
  String get settingsPermissionOverlayHint;

  /// No description provided for @settingsPermissionFileRead.
  ///
  /// In en, this message translates to:
  /// **'File reading'**
  String get settingsPermissionFileRead;

  /// No description provided for @settingsPermissionFileReadHint.
  ///
  /// In en, this message translates to:
  /// **'Only paths accessible to the current system user can be read.'**
  String get settingsPermissionFileReadHint;

  /// No description provided for @settingsPermissionSystemManaged.
  ///
  /// In en, this message translates to:
  /// **'System managed'**
  String get settingsPermissionSystemManaged;

  /// No description provided for @settingsPermissionAllowed.
  ///
  /// In en, this message translates to:
  /// **'Allowed'**
  String get settingsPermissionAllowed;

  /// No description provided for @settingsPermissionRequired.
  ///
  /// In en, this message translates to:
  /// **'Needs access'**
  String get settingsPermissionRequired;

  /// No description provided for @settingsPermissionChecking.
  ///
  /// In en, this message translates to:
  /// **'Checking'**
  String get settingsPermissionChecking;

  /// No description provided for @settingsPermissionManage.
  ///
  /// In en, this message translates to:
  /// **'Manage'**
  String get settingsPermissionManage;

  /// No description provided for @settingsDesktopPermissionsHint.
  ///
  /// In en, this message translates to:
  /// **'Desktop has no device permissions. To use screen, sensor or camera tools, connect an Android phone or board node.'**
  String get settingsDesktopPermissionsHint;

  /// No description provided for @settingsGrant.
  ///
  /// In en, this message translates to:
  /// **'Grant'**
  String get settingsGrant;

  /// No description provided for @toastCopied.
  ///
  /// In en, this message translates to:
  /// **'Copied'**
  String get toastCopied;

  /// No description provided for @toastCopyFailed.
  ///
  /// In en, this message translates to:
  /// **'Copy failed'**
  String get toastCopyFailed;

  /// No description provided for @toastImagePickerUnavailable.
  ///
  /// In en, this message translates to:
  /// **'Image picking is not supported on this platform'**
  String get toastImagePickerUnavailable;

  /// No description provided for @toastModelImportFailed.
  ///
  /// In en, this message translates to:
  /// **'Model import failed'**
  String get toastModelImportFailed;

  /// No description provided for @toastModelDeleted.
  ///
  /// In en, this message translates to:
  /// **'Model deleted'**
  String get toastModelDeleted;

  /// No description provided for @toastModelDeleteFailed.
  ///
  /// In en, this message translates to:
  /// **'Model delete failed'**
  String get toastModelDeleteFailed;

  /// No description provided for @toastModelsReloaded.
  ///
  /// In en, this message translates to:
  /// **'Models reloaded'**
  String get toastModelsReloaded;

  /// No description provided for @toastModelImported.
  ///
  /// In en, this message translates to:
  /// **'Model imported'**
  String get toastModelImported;

  /// No description provided for @toastModelMarketLoaded.
  ///
  /// In en, this message translates to:
  /// **'Market loaded'**
  String get toastModelMarketLoaded;

  /// No description provided for @toastModelMarketFailed.
  ///
  /// In en, this message translates to:
  /// **'Market unavailable'**
  String get toastModelMarketFailed;

  /// No description provided for @errorApiRequest.
  ///
  /// In en, this message translates to:
  /// **'API request failed'**
  String get errorApiRequest;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'zh'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'zh':
      return AppLocalizationsZh();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
