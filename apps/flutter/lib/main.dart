import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';
import 'core/api_client.dart';
import 'core/mock_chat_streamer.dart';
import 'platform/desktop_server.dart';
import 'platform/mock_platform_service.dart';
import 'platform/platform_service.dart';
import 'state/chat_controller.dart';
import 'state/locale_controller.dart';
import 'state/runtime_controller.dart';
import 'state/theme_controller.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  const useMock = bool.fromEnvironment('LOCIANT_MOCK');
  final api = ApiClient();
  final server = DesktopServerProcess(api: api);
  // Desktop bundles ship the Rust backend as a sidecar; start it before the
  // UI connects (no-op when pointing at a remote backend or when the server
  // is already running). Android spawns the server in the Kotlin host.
  if (!useMock && !kIsWeb && defaultTargetPlatform == TargetPlatform.linux) {
    await server.start();
  }
  final PlatformService platform;
  if (useMock) {
    platform = MockPlatformService();
  } else if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
    platform = AndroidPlatformService(api);
  } else {
    platform = HttpPlatformService(api);
  }
  final runtime = RuntimeController(platform, api);
  final chat = ChatController(runtime, api, streamer: useMock ? MockChatStreamer() : null);
  final locale = LocaleController();
  final theme = ThemeController();
  final prefs = await SharedPreferences.getInstance();
  final onboardingDone = prefs.getBool('onboarding_done') ?? false;
  runApp(LociantApp(
    runtime: runtime,
    chat: chat,
    locale: locale,
    theme: theme,
    server: server,
    onboardingDone: onboardingDone,
    prefs: prefs,
  ));
}
