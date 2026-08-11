import 'dart:io';

import 'package:flutter/foundation.dart';

import '../core/api_client.dart';

/// Manages the Rust backend sidecar on desktop.
///
/// Release bundles ship `lociant-server` under `bin/`; debug builds find it
/// in the repo's `target/` directory. If a server is already listening (e.g.
/// the user started it manually) or the UI points at a remote backend, the
/// sidecar is left untouched and only the connection is used.
class DesktopServerProcess {
  DesktopServerProcess({ApiClient? api}) : api = api ?? ApiClient();

  final ApiClient api;
  Process? _process;
  bool _startedHere = false;

  static const _healthTimeout = Duration(seconds: 10);
  static const _healthInterval = Duration(milliseconds: 300);

  /// True when this app spawned the server process (and should stop it).
  bool get startedHere => _startedHere;

  Future<void> start() async {
    if (_process != null || _startedHere) return;
    if (_isRemoteBaseUrl) return;
    if (await _isHealthy()) return;
    final binary = _findBinary();
    if (binary == null) {
      debugPrint('lociant-server binary not found; UI will show it offline');
      return;
    }
    final process = await Process.start(
      binary,
      const [],
      environment: {
        // Listen on all interfaces so peers (phone/board) can reach this
        // desktop node over the LAN; access is guarded by the API token.
        'LOCIANT_HOST': Platform.environment['LOCIANT_HOST'] ?? '0.0.0.0',
        'LOCIANT_PORT': Platform.environment['LOCIANT_PORT'] ?? '11434',
        ...Platform.environment,
      },
    );
    _process = process;
    _startedHere = true;
    // Drain pipes so the child never blocks on a full buffer.
    process.stdout.drain<void>();
    process.stderr.drain<void>();
    final deadline = DateTime.now().add(_healthTimeout);
    while (DateTime.now().isBefore(deadline)) {
      if (await _isHealthy()) return;
      await Future<void>.delayed(_healthInterval);
    }
    debugPrint('lociant-server started but /health not reachable in time');
  }

  /// Stops the server only if this app spawned it.
  Future<void> stop() async {
    final process = _process;
    if (!_startedHere || process == null) return;
    process.kill();
    await process.exitCode.timeout(
      const Duration(seconds: 3),
      onTimeout: () => -1,
    );
    _process = null;
    _startedHere = false;
  }

  bool get _isRemoteBaseUrl {
    final host = Uri.parse(ApiClient.defaultBaseUrl).host;
    return host != '127.0.0.1' && host != 'localhost' && host != '::1';
  }

  Future<bool> _isHealthy() async {
    try {
      await api.get('/health').timeout(const Duration(seconds: 2));
      return true;
    } catch (_) {
      return false;
    }
  }

  String? _findBinary() {
    final override = Platform.environment['LOCIANT_SERVER_BIN'];
    if (override != null && File(override).existsSync()) return override;
    // Release bundle layout: <bundle>/lociant_flutter, <bundle>/bin/lociant-server
    final appDir = File(Platform.resolvedExecutable).parent;
    final bundled = File('${appDir.path}${Platform.pathSeparator}bin${Platform.pathSeparator}lociant-server');
    if (bundled.existsSync()) return bundled.path;
    // Debug/dev layout: flutter run from apps/flutter.
    final dev = File(
      '..${Platform.pathSeparator}rust-backend${Platform.pathSeparator}target${Platform.pathSeparator}release${Platform.pathSeparator}lociant-server',
    );
    if (dev.existsSync()) return dev.path;
    return null;
  }
}
