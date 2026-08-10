import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

import 'app.dart';
import 'core/api_client.dart';
import 'core/mock_chat_streamer.dart';
import 'platform/mock_platform_service.dart';
import 'platform/platform_service.dart';
import 'state/chat_controller.dart';
import 'state/locale_controller.dart';
import 'state/runtime_controller.dart';
import 'state/theme_controller.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  const useMock = bool.fromEnvironment('LOCIANT_MOCK');
  final api = ApiClient();
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
  runApp(LociantApp(runtime: runtime, chat: chat, locale: locale, theme: theme));
}
