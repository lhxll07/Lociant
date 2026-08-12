import 'package:flutter/material.dart';

/// Semantic status colors used across the UI. Accessed through
/// `Theme.of(context).extension<LociantStatus>()`.
class LociantStatus extends ThemeExtension<LociantStatus> {
  const LociantStatus({
    required this.success,
    required this.warning,
    required this.danger,
  });

  final Color success;
  final Color warning;
  final Color danger;

  @override
  LociantStatus copyWith({Color? success, Color? warning, Color? danger}) {
    return LociantStatus(
      success: success ?? this.success,
      warning: warning ?? this.warning,
      danger: danger ?? this.danger,
    );
  }

  @override
  LociantStatus lerp(ThemeExtension<LociantStatus>? other, double t) {
    if (other is! LociantStatus) return this;
    return LociantStatus(
      success: Color.lerp(success, other.success, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      danger: Color.lerp(danger, other.danger, t)!,
    );
  }
}

/// Official Material Theme Builder output for seed #5C9DFF in dark mode
/// (tonal spot scheme, generated with @material/material-color-utilities).
/// Hardcoded instead of fromSeed() so the palette never drifts with SDK
/// algorithm changes and matches the official baseline exactly.
const ColorScheme _darkBlueScheme = ColorScheme.dark(
  primary: Color(0xffa9c7ff),
  onPrimary: Color(0xff09305f),
  primaryContainer: Color(0xff274777),
  onPrimaryContainer: Color(0xffd6e3ff),
  secondary: Color(0xffbec7dc),
  onSecondary: Color(0xff283141),
  secondaryContainer: Color(0xff3e4759),
  onSecondaryContainer: Color(0xffdae2f9),
  tertiary: Color(0xffdcbce1),
  onTertiary: Color(0xff3f2845),
  tertiaryContainer: Color(0xff563e5c),
  onTertiaryContainer: Color(0xfff9d8fd),
  error: Color(0xffffb4ab),
  onError: Color(0xff690005),
  errorContainer: Color(0xff93000a),
  onErrorContainer: Color(0xffffdad6),
  surface: Color(0xff111318),
  onSurface: Color(0xffe2e2e9),
  surfaceDim: Color(0xff111318),
  surfaceBright: Color(0xff37393e),
  surfaceContainerLowest: Color(0xff0c0e13),
  surfaceContainerLow: Color(0xff191c20),
  surfaceContainer: Color(0xff1d2024),
  surfaceContainerHigh: Color(0xff282a2f),
  surfaceContainerHighest: Color(0xff33353a),
  onSurfaceVariant: Color(0xffc4c6cf),
  outline: Color(0xff8e9099),
  outlineVariant: Color(0xff44474e),
  inverseSurface: Color(0xffe2e2e9),
  onInverseSurface: Color(0xff2e3036),
  inversePrimary: Color(0xff405f90),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  surfaceTint: Color(0xffa9c7ff),
);

/// Official Material Theme Builder output for seed #F8BBD0 (Material Pink
/// 100) in light mode: a soft pink-white background with rose accents.
const ColorScheme _pinkLightScheme = ColorScheme.light(
  primary: Color(0xff8a4a64),
  onPrimary: Color(0xffffffff),
  primaryContainer: Color(0xffffd9e4),
  onPrimaryContainer: Color(0xff6f334c),
  secondary: Color(0xff735760),
  onSecondary: Color(0xffffffff),
  secondaryContainer: Color(0xffffd9e4),
  onSecondaryContainer: Color(0xff5a3f49),
  tertiary: Color(0xff7d5637),
  onTertiary: Color(0xffffffff),
  tertiaryContainer: Color(0xffffdcc4),
  onTertiaryContainer: Color(0xff633e22),
  error: Color(0xffba1a1a),
  onError: Color(0xffffffff),
  errorContainer: Color(0xffffdad6),
  onErrorContainer: Color(0xff93000a),
  surface: Color(0xfffff8f8),
  onSurface: Color(0xff21191c),
  surfaceDim: Color(0xffe6d6da),
  surfaceBright: Color(0xfffff8f8),
  surfaceContainerLowest: Color(0xffffffff),
  surfaceContainerLow: Color(0xfffff0f3),
  surfaceContainer: Color(0xfffaeaed),
  surfaceContainerHigh: Color(0xfff4e4e8),
  surfaceContainerHighest: Color(0xffeedfe2),
  onSurfaceVariant: Color(0xff514347),
  outline: Color(0xff837377),
  outlineVariant: Color(0xffd5c2c7),
  inverseSurface: Color(0xff372e31),
  onInverseSurface: Color(0xfffdedf0),
  inversePrimary: Color(0xffffb0cd),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  surfaceTint: Color(0xff8a4a64),
);

/// Default theme: the dark M3 baseline in the app's blue.
ThemeData buildLociantDarkTheme() => _buildLociantTheme(
  scheme: _darkBlueScheme,
  status: const LociantStatus(
    // Official M3 dark supporting tones.
    success: Color(0xff81c995),
    warning: Color(0xfffdd663),
    danger: Color(0xfff2b8b5),
  ),
);

/// Light pink variant: soft pink-white surfaces with rose accents.
ThemeData buildLociantPinkTheme() => _buildLociantTheme(
  scheme: _pinkLightScheme,
  status: const LociantStatus(
    // Deep, accessible tones that stay legible on light surfaces.
    success: Color(0xff2e7d32),
    warning: Color(0xffb26a00),
    danger: Color(0xffc62828),
  ),
);

/// Shared builder so both themes keep identical component styling; only the
/// color roles differ. Surfaces follow Material 3 tone roles
/// (surface / surfaceContainer / High / Highest) so cards, bubbles, popups
/// and insets stay consistent everywhere.
ThemeData _buildLociantTheme({
  required ColorScheme scheme,
  required LociantStatus status,
}) {
  final border = scheme.outlineVariant.withValues(alpha: 0.4);

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: scheme.surface,
    extensions: [status],
    appBarTheme: AppBarTheme(
      backgroundColor: scheme.surfaceContainer,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: scheme.onSurface,
        fontSize: 17,
        fontWeight: FontWeight.w600,
      ),
    ),
    cardTheme: CardThemeData(
      color: scheme.surfaceContainerHigh,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: border),
      ),
    ),
    dividerTheme: DividerThemeData(color: border, thickness: 1),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: scheme.surfaceContainerHighest,
      isDense: true,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide.none,
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: scheme.primary, width: 1.4),
      ),
    ),
    listTileTheme: ListTileThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: scheme.surfaceContainerHigh,
      contentTextStyle: TextStyle(color: scheme.onSurface),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        minimumSize: const Size(48, 40),
        padding: const EdgeInsets.symmetric(horizontal: 16),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        minimumSize: const Size(48, 40),
        padding: const EdgeInsets.symmetric(horizontal: 12),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        minimumSize: const Size(48, 40),
        padding: const EdgeInsets.symmetric(horizontal: 16),
      ),
    ),
    segmentedButtonTheme: SegmentedButtonThemeData(
      style: ButtonStyle(
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
    ),
  );
}

extension LociantThemeX on BuildContext {
  LociantStatus get status => Theme.of(this).extension<LociantStatus>()!;
}
