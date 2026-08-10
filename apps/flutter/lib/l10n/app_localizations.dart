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
  /// **'Home'**
  String get navHome;

  /// No description provided for @navModels.
  ///
  /// In en, this message translates to:
  /// **'Models'**
  String get navModels;

  /// No description provided for @navSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get navSettings;

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

  /// No description provided for @homePlaceholder.
  ///
  /// In en, this message translates to:
  /// **'Ask Lociant, or describe a tool task'**
  String get homePlaceholder;

  /// No description provided for @homeSend.
  ///
  /// In en, this message translates to:
  /// **'Send'**
  String get homeSend;

  /// No description provided for @homeNewChat.
  ///
  /// In en, this message translates to:
  /// **'New chat'**
  String get homeNewChat;

  /// No description provided for @homeHistory.
  ///
  /// In en, this message translates to:
  /// **'Recent chats'**
  String get homeHistory;

  /// No description provided for @homeEmptyReply.
  ///
  /// In en, this message translates to:
  /// **'No reply'**
  String get homeEmptyReply;

  /// No description provided for @homeThinking.
  ///
  /// In en, this message translates to:
  /// **'Thinking…'**
  String get homeThinking;

  /// No description provided for @homeThought.
  ///
  /// In en, this message translates to:
  /// **'Thought'**
  String get homeThought;

  /// No description provided for @homeRunStatusTool.
  ///
  /// In en, this message translates to:
  /// **'Running tool {tool} (round {round})…'**
  String homeRunStatusTool(Object round, Object tool);

  /// No description provided for @homeRunStatusRound.
  ///
  /// In en, this message translates to:
  /// **'Calling model (round {round})…'**
  String homeRunStatusRound(Object round);

  /// No description provided for @homeRunStatusRetry.
  ///
  /// In en, this message translates to:
  /// **'Retrying…'**
  String get homeRunStatusRetry;

  /// No description provided for @homeRoundLabel.
  ///
  /// In en, this message translates to:
  /// **'Round {n}'**
  String homeRoundLabel(Object n);

  /// No description provided for @homeToolRunDone.
  ///
  /// In en, this message translates to:
  /// **'Tools completed without a text reply.'**
  String get homeToolRunDone;

  /// No description provided for @homeImageAttached.
  ///
  /// In en, this message translates to:
  /// **'Image attached'**
  String get homeImageAttached;

  /// No description provided for @homeRemoveImage.
  ///
  /// In en, this message translates to:
  /// **'Remove image'**
  String get homeRemoveImage;

  /// No description provided for @homeUploadImage.
  ///
  /// In en, this message translates to:
  /// **'Upload photo'**
  String get homeUploadImage;

  /// No description provided for @homeDeleteChat.
  ///
  /// In en, this message translates to:
  /// **'Delete chat'**
  String get homeDeleteChat;

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
  /// **'Install, select and manage local inference'**
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
  /// **'ModelScope MNN models'**
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

  /// No description provided for @modelsCloudTitle.
  ///
  /// In en, this message translates to:
  /// **'Cloud model'**
  String get modelsCloudTitle;

  /// No description provided for @modelsCloudSub.
  ///
  /// In en, this message translates to:
  /// **'OpenAI-compatible cloud API'**
  String get modelsCloudSub;

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
  /// **'Model and CPU threads'**
  String get settingsModelSub;

  /// No description provided for @settingsAgentTitle.
  ///
  /// In en, this message translates to:
  /// **'Agent'**
  String get settingsAgentTitle;

  /// No description provided for @settingsAgentSub.
  ///
  /// In en, this message translates to:
  /// **'Tool-loop behavior'**
  String get settingsAgentSub;

  /// No description provided for @settingsAdvancedTitle.
  ///
  /// In en, this message translates to:
  /// **'Advanced'**
  String get settingsAdvancedTitle;

  /// No description provided for @settingsAdvancedSub.
  ///
  /// In en, this message translates to:
  /// **'Sessions and diagnostics'**
  String get settingsAdvancedSub;

  /// No description provided for @settingsAgentRounds.
  ///
  /// In en, this message translates to:
  /// **'Max tool rounds'**
  String get settingsAgentRounds;

  /// No description provided for @settingsAgentRoundsSub.
  ///
  /// In en, this message translates to:
  /// **'Model↔tool iterations per task ({min}–{max})'**
  String settingsAgentRoundsSub(Object max, Object min);

  /// No description provided for @settingsToolCalls.
  ///
  /// In en, this message translates to:
  /// **'Tool call cap'**
  String get settingsToolCalls;

  /// No description provided for @settingsPermissionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Permissions'**
  String get settingsPermissionsTitle;

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

  /// No description provided for @settingsSessions.
  ///
  /// In en, this message translates to:
  /// **'Sessions'**
  String get settingsSessions;

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

  /// No description provided for @settingsInferenceBackend.
  ///
  /// In en, this message translates to:
  /// **'Inference backend'**
  String get settingsInferenceBackend;

  /// No description provided for @settingsBackendModel.
  ///
  /// In en, this message translates to:
  /// **'Follow model'**
  String get settingsBackendModel;

  /// No description provided for @settingsBackendAuto.
  ///
  /// In en, this message translates to:
  /// **'Auto'**
  String get settingsBackendAuto;

  /// No description provided for @settingsBackendCpu.
  ///
  /// In en, this message translates to:
  /// **'CPU'**
  String get settingsBackendCpu;

  /// No description provided for @settingsBackendOpencl.
  ///
  /// In en, this message translates to:
  /// **'OpenCL (GPU)'**
  String get settingsBackendOpencl;

  /// No description provided for @settingsBackendVulkan.
  ///
  /// In en, this message translates to:
  /// **'Vulkan (GPU)'**
  String get settingsBackendVulkan;

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

  /// No description provided for @settingsCloudTitle.
  ///
  /// In en, this message translates to:
  /// **'Cloud model'**
  String get settingsCloudTitle;

  /// No description provided for @settingsCloudBaseUrl.
  ///
  /// In en, this message translates to:
  /// **'API base URL'**
  String get settingsCloudBaseUrl;

  /// No description provided for @settingsCloudApiKey.
  ///
  /// In en, this message translates to:
  /// **'API key'**
  String get settingsCloudApiKey;

  /// No description provided for @settingsCloudModel.
  ///
  /// In en, this message translates to:
  /// **'Model name'**
  String get settingsCloudModel;

  /// No description provided for @settingsCloudResponseLengthSub.
  ///
  /// In en, this message translates to:
  /// **'Max output tokens (0 = provider default)'**
  String get settingsCloudResponseLengthSub;

  /// No description provided for @settingsCloudContextWindow.
  ///
  /// In en, this message translates to:
  /// **'Context window'**
  String get settingsCloudContextWindow;

  /// No description provided for @settingsCloudHistoryLimit.
  ///
  /// In en, this message translates to:
  /// **'History messages'**
  String get settingsCloudHistoryLimit;

  /// No description provided for @connectionOpenaiUrl.
  ///
  /// In en, this message translates to:
  /// **'OpenAI Base URL'**
  String get connectionOpenaiUrl;

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

  /// No description provided for @settingsPermissionAccessibility.
  ///
  /// In en, this message translates to:
  /// **'Accessibility'**
  String get settingsPermissionAccessibility;

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
