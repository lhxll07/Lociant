import 'dart:async';
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
  bool _stopping = false;
  bool _launching = false;
  bool _restarting = false;
  int _failedHealthChecks = 0;
  int _restartAttempt = 0;
  Future<void>? _startFuture;
  Timer? _watchdog;
  Timer? _recoveryTimer;

  static const _healthTimeout = Duration(seconds: 10);
  static const _healthInterval = Duration(milliseconds: 300);
  static const _watchdogInterval = Duration(seconds: 5);
  static const _healthFailuresBeforeRestart = 2;

  /// True when this app spawned the server process (and should stop it).
  bool get startedHere => _startedHere;

  Future<void> start() async {
    if (_isRemoteBaseUrl) return;
    final pending = _startFuture;
    if (pending != null) {
      await pending;
      return;
    }
    if (_process != null || _startedHere) return;
    _stopping = false;
    final future = _startInternal();
    _startFuture = future;
    try {
      await future;
    } finally {
      if (identical(_startFuture, future)) _startFuture = null;
    }
  }

  /// Stops the server only if this app spawned it.
  Future<void> stop() async {
    _stopping = true;
    _watchdog?.cancel();
    _watchdog = null;
    _recoveryTimer?.cancel();
    _recoveryTimer = null;
    final process = _process;
    if (_startedHere && process != null) await _terminate(process);
    _process = null;
    _startedHere = false;
  }

  Future<void> _startInternal() async {
    if (_stopping) return;
    if (await _isHealthy()) return;
    if (_stopping) return;
    final binary = _findBinary();
    if (binary == null) {
      debugPrint('lociant-server binary not found; UI will show it offline');
      return;
    }
    _launching = true;
    try {
      if (!await _launchAndWait(binary)) {
        debugPrint('lociant-server did not become healthy in time');
        _scheduleRecovery();
      }
    } finally {
      _launching = false;
    }
  }

  Future<bool> _launchAndWait(String binary) async {
    if (_stopping) return false;
    try {
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
      _failedHealthChecks = 0;
      _watchProcess(process);
      // Drain pipes so the child never blocks on a full buffer.
      process.stdout.drain<void>();
      process.stderr.drain<void>();
      final deadline = DateTime.now().add(_healthTimeout);
      while (DateTime.now().isBefore(deadline)) {
        if (!identical(_process, process)) return false;
        if (await _isHealthy()) {
          _restartAttempt = 0;
          _startWatchdog();
          return true;
        }
        await Future<void>.delayed(_healthInterval);
      }
      if (identical(_process, process)) await _terminate(process);
      return false;
    } catch (error) {
      debugPrint('lociant-server start failed: $error');
      return false;
    }
  }

  void _watchProcess(Process process) {
    unawaited(process.exitCode.then<void>((code) {
      if (!identical(_process, process)) return;
      _process = null;
      if (_stopping) {
        _startedHere = false;
        return;
      }
      debugPrint('lociant-server exited with code $code');
      _watchdog?.cancel();
      _watchdog = null;
      if (!_launching && !_restarting) _scheduleRecovery();
    }));
  }

  void _startWatchdog() {
    _watchdog?.cancel();
    _watchdog = Timer.periodic(_watchdogInterval, (_) {
      unawaited(_watchdogTick());
    });
  }

  Future<void> _watchdogTick() async {
    if (_stopping || !_startedHere || _restarting || _launching) return;
    final process = _process;
    if (process == null) return;
    if (await _isHealthy()) {
      _failedHealthChecks = 0;
      return;
    }
    _failedHealthChecks++;
    if (_failedHealthChecks < _healthFailuresBeforeRestart) return;
    debugPrint('lociant-server health check failed; scheduling recovery');
    _failedHealthChecks = 0;
    _watchdog?.cancel();
    _watchdog = null;
    await _terminate(process);
    if (identical(_process, process)) _process = null;
    _scheduleRecovery();
  }

  void _scheduleRecovery() {
    if (_stopping || !_startedHere || _recoveryTimer != null) return;
    final exponent = _restartAttempt.clamp(0, 4).toInt();
    final delay = Duration(seconds: 1 << exponent);
    _restartAttempt = (_restartAttempt + 1).clamp(0, 4).toInt();
    _recoveryTimer = Timer(delay, () {
      _recoveryTimer = null;
      unawaited(_recover());
    });
  }

  Future<void> _recover() async {
    if (_stopping || !_startedHere || _restarting) return;
    _restarting = true;
    try {
      if (await _isHealthy()) {
        // Another process took over the local port. Do not claim ownership or
        // stop a process this application did not spawn.
        _startedHere = false;
        return;
      }
      final binary = _findBinary();
      if (binary == null) {
        debugPrint('lociant-server binary disappeared; recovery will retry');
        return;
      }
      if (!await _launchAndWait(binary)) {
        debugPrint('lociant-server recovery attempt failed');
      }
    } finally {
      _restarting = false;
      if (!_stopping && _startedHere && _process == null) _scheduleRecovery();
    }
  }

  Future<void> _terminate(Process process) async {
    process.kill();
    final exited = await process.exitCode.timeout(
      const Duration(seconds: 3),
      onTimeout: () => -1,
    );
    if (exited == -1) {
      process.kill(ProcessSignal.sigkill);
    }
  }

  bool get _isRemoteBaseUrl {
    final host = Uri.parse(api.baseUrl).host;
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
    final bundled = File(
      '${appDir.path}${Platform.pathSeparator}bin${Platform.pathSeparator}lociant-server',
    );
    if (bundled.existsSync()) return bundled.path;
    // Debug/dev layout: flutter run from apps/flutter.
    final dev = File(
      '..${Platform.pathSeparator}rust-backend${Platform.pathSeparator}target${Platform.pathSeparator}release${Platform.pathSeparator}lociant-server',
    );
    if (dev.existsSync()) return dev.path;
    return null;
  }
}
