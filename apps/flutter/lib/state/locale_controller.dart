import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

const String kLocaleModeKey = 'lociant.locale';

/// UI language selection: follow the system, or force Chinese / English.
/// The choice is persisted locally so it survives restarts.
class LocaleController extends ChangeNotifier {
  LocaleController() {
    _load();
  }

  String _mode = 'system';

  String get mode => _mode;

  /// null means "follow the system locale".
  Locale? get locale => switch (_mode) {
        'zh' => const Locale('zh'),
        'en' => const Locale('en'),
        _ => null,
      };

  Future<void> _load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final stored = prefs.getString(kLocaleModeKey);
      if (stored == 'zh' || stored == 'en' || stored == 'system') {
        _mode = stored!;
        notifyListeners();
      }
    } catch (_) {
      // Defaults to system language.
    }
  }

  Future<void> setMode(String mode) async {
    if (mode != 'zh' && mode != 'en' && mode != 'system') return;
    _mode = mode;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(kLocaleModeKey, mode);
    } catch (_) {
      // Best effort persistence.
    }
  }
}
