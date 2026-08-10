import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

const String kThemeModeKey = 'lociant.theme';

/// App theme selection: the dark M3 baseline or the light pink variant.
/// The choice is persisted locally so it survives restarts.
class ThemeController extends ChangeNotifier {
  ThemeController() {
    _load();
  }

  String _mode = 'dark';

  String get mode => _mode;

  Future<void> _load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final stored = prefs.getString(kThemeModeKey);
      if (stored == 'dark' || stored == 'pink') {
        _mode = stored!;
        notifyListeners();
      }
    } catch (_) {
      // Defaults to the dark theme.
    }
  }

  Future<void> setMode(String mode) async {
    if (mode != 'dark' && mode != 'pink') return;
    _mode = mode;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(kThemeModeKey, mode);
    } catch (_) {
      // Best effort persistence.
    }
  }
}
