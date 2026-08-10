import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'l10n/app_localizations.dart';
import 'screens/home_shell.dart';
import 'state/chat_controller.dart';
import 'state/locale_controller.dart';
import 'state/runtime_controller.dart';
import 'state/theme_controller.dart';
import 'theme.dart';

/// Provides the process-scoped controllers to every screen.
class AppScope extends InheritedWidget {
  const AppScope({
    super.key,
    required this.runtime,
    required this.chat,
    required this.locale,
    required this.theme,
    required super.child,
  });

  final RuntimeController runtime;
  final ChatController chat;
  final LocaleController locale;
  final ThemeController theme;

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
      theme != oldWidget.theme;
}

class LociantApp extends StatefulWidget {
  const LociantApp({
    super.key,
    required this.runtime,
    required this.chat,
    required this.locale,
    required this.theme,
  });

  final RuntimeController runtime;
  final ChatController chat;
  final LocaleController locale;
  final ThemeController theme;

  @override
  State<LociantApp> createState() => _LociantAppState();
}

class _LociantAppState extends State<LociantApp> {
  @override
  void initState() {
    super.initState();
    widget.runtime.startPolling();
    widget.locale.addListener(_onLocaleChanged);
    widget.theme.addListener(_onThemeChanged);
  }

  @override
  void dispose() {
    widget.locale.removeListener(_onLocaleChanged);
    widget.theme.removeListener(_onThemeChanged);
    widget.runtime.dispose();
    widget.chat.dispose();
    super.dispose();
  }

  void _onLocaleChanged() => setState(() {});
  void _onThemeChanged() => setState(() {});

  @override
  Widget build(BuildContext context) {
    return AppScope(
      runtime: widget.runtime,
      chat: widget.chat,
      locale: widget.locale,
      theme: widget.theme,
      child: MaterialApp(
        title: 'Lociant',
        debugShowCheckedModeBanner: false,
        theme: buildLociantPinkTheme(),
        darkTheme: buildLociantDarkTheme(),
        themeMode:
            widget.theme.mode == 'pink' ? ThemeMode.light : ThemeMode.dark,
        locale: widget.locale.locale,
        supportedLocales: const [Locale('en'), Locale('zh')],
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        home: const HomeShell(),
      ),
    );
  }
}
