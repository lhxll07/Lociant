import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'l10n/app_localizations.dart';
import 'screens/home_shell.dart';
import 'screens/onboarding_screen.dart';
import 'platform/desktop_server.dart';
import 'state/chat_controller.dart';
import 'state/locale_controller.dart';
import 'state/runtime_controller.dart';
import 'state/theme_controller.dart';
import 'theme.dart';

/// Global key for the main shell so other screens (e.g. Nodes) can switch
/// the top-level tab programmatically.
final homeShellKey = GlobalKey<HomeShellState>();

/// Provides the process-scoped controllers to every screen.
class AppScope extends InheritedWidget {
  const AppScope({
    super.key,
    required this.runtime,
    required this.chat,
    required this.locale,
    required this.theme,
    required this.server,
    required super.child,
  });

  final RuntimeController runtime;
  final ChatController chat;
  final LocaleController locale;
  final ThemeController theme;
  final DesktopServerProcess server;

  static AppScope of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AppScope>();
    assert(scope != null, 'AppScope missing from widget tree');
    return scope!;
  }

  /// Reads the scope without registering a dependency; safe in [initState].
  static AppScope? maybeOf(BuildContext context) =>
      context.getInheritedWidgetOfExactType<AppScope>();

  @override
  bool updateShouldNotify(AppScope oldWidget) =>
      runtime != oldWidget.runtime ||
      chat != oldWidget.chat ||
      locale != oldWidget.locale ||
      theme != oldWidget.theme ||
      server != oldWidget.server;
}

class LociantApp extends StatefulWidget {
  const LociantApp({
    super.key,
    required this.runtime,
    required this.chat,
    required this.locale,
    required this.theme,
    required this.server,
    required this.onboardingDone,
    required this.prefs,
  });

  final RuntimeController runtime;
  final ChatController chat;
  final LocaleController locale;
  final ThemeController theme;
  final DesktopServerProcess server;
  final bool onboardingDone;
  final SharedPreferences prefs;

  @override
  State<LociantApp> createState() => _LociantAppState();
}

class _LociantAppState extends State<LociantApp> {
  late bool _onboardingDone;

  @override
  void initState() {
    super.initState();
    _onboardingDone = widget.onboardingDone;
    widget.runtime.startPolling();
    widget.runtime.addListener(_onRuntimeChanged);
    widget.locale.addListener(_onLocaleChanged);
    widget.theme.addListener(_onThemeChanged);
    widget.server.start(); // 桌面端：确保 sidecar 已启动（幂等）
  }

  @override
  void dispose() {
    widget.server.stop();
    widget.runtime.removeListener(_onRuntimeChanged);
    widget.locale.removeListener(_onLocaleChanged);
    widget.theme.removeListener(_onThemeChanged);
    widget.runtime.dispose();
    widget.chat.dispose();
    super.dispose();
  }

  void _onLocaleChanged() => setState(() {});
  void _onThemeChanged() => setState(() {});

  void _onRuntimeChanged() {
    // The server is up: warm the tool manifest in the background so the first
    // chat send already has the agent's tool list cached.
    if (widget.runtime.state != null) {
      unawaited(widget.chat.warmTools());
    }
  }

  void _finishOnboarding() {
    widget.prefs.setBool('onboarding_done', true);
    setState(() => _onboardingDone = true);
  }

  @override
  Widget build(BuildContext context) {
    return AppScope(
      runtime: widget.runtime,
      chat: widget.chat,
      locale: widget.locale,
      theme: widget.theme,
      server: widget.server,
      child: MaterialApp(
        title: 'Lociant',
        debugShowCheckedModeBanner: false,
        theme: buildLociantPinkTheme(),
        darkTheme: buildLociantDarkTheme(),
        themeMode: widget.theme.mode == 'pink'
            ? ThemeMode.light
            : ThemeMode.dark,
        locale: widget.locale.locale,
        supportedLocales: const [Locale('en'), Locale('zh')],
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        home: _onboardingDone
            ? HomeShell(key: homeShellKey)
            : OnboardingScreen(onDone: _finishOnboarding),
      ),
    );
  }
}
