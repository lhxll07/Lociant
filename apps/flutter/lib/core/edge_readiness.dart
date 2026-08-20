import 'models.dart';

enum EdgeReadinessStatus { checking, ready, attention, blocked, optional }

enum EdgeReadinessKind {
  runtime,
  connection,
  tools,
  permissions,
  security,
  model,
}

class EdgeReadinessCheck {
  const EdgeReadinessCheck({
    required this.kind,
    required this.status,
    required this.required,
  });

  final EdgeReadinessKind kind;
  final EdgeReadinessStatus status;
  final bool required;
}

class EdgeReadiness {
  const EdgeReadiness(this.checks);

  final List<EdgeReadinessCheck> checks;

  EdgeReadinessStatus get overall {
    final requiredChecks = checks.where((check) => check.required);
    if (requiredChecks.any(
      (check) => check.status == EdgeReadinessStatus.blocked,
    )) {
      return EdgeReadinessStatus.blocked;
    }
    if (requiredChecks.any(
      (check) => check.status == EdgeReadinessStatus.checking,
    )) {
      return EdgeReadinessStatus.checking;
    }
    if (requiredChecks.any(
      (check) => check.status == EdgeReadinessStatus.attention,
    )) {
      return EdgeReadinessStatus.attention;
    }
    return EdgeReadinessStatus.ready;
  }

  int get readyCount => checks
      .where(
        (check) => check.required && check.status == EdgeReadinessStatus.ready,
      )
      .length;

  int get requiredCount => checks.where((check) => check.required).length;

  static EdgeReadiness evaluate({
    required RuntimeUiState? runtime,
    required bool? healthOk,
    required bool? toolsOk,
    required int toolCount,
    bool? toolCallOk,
    required bool android,
  }) {
    final runtimeStatus = runtime == null
        ? EdgeReadinessStatus.checking
        : runtime.starting
        ? EdgeReadinessStatus.checking
        : runtime.running
        ? EdgeReadinessStatus.ready
        : EdgeReadinessStatus.blocked;

    final connectionStatus = healthOk == null
        ? EdgeReadinessStatus.checking
        : healthOk
        ? EdgeReadinessStatus.ready
        : EdgeReadinessStatus.blocked;

    final toolsStatus = toolsOk == null
        ? EdgeReadinessStatus.checking
        : !toolsOk
        ? EdgeReadinessStatus.blocked
        : toolCallOk == false
        ? EdgeReadinessStatus.blocked
        : toolCount > 0
        ? EdgeReadinessStatus.ready
        : EdgeReadinessStatus.attention;

    final permissionsStatus = !android
        ? EdgeReadinessStatus.ready
        : runtime == null
        ? EdgeReadinessStatus.checking
        : !runtime.notificationPermissionGranted
        ? EdgeReadinessStatus.blocked
        : !runtime.batteryOptimizationIgnored
        ? EdgeReadinessStatus.attention
        : EdgeReadinessStatus.ready;

    final securityStatus = runtime == null
        ? EdgeReadinessStatus.checking
        : runtime.authToken.isEmpty
        ? EdgeReadinessStatus.attention
        : EdgeReadinessStatus.ready;

    final modelStatus = runtime == null || runtime.modelLoading
        ? EdgeReadinessStatus.checking
        : runtime.modelLoaded
        ? EdgeReadinessStatus.ready
        : runtime.modelId.isEmpty
        ? EdgeReadinessStatus.optional
        : EdgeReadinessStatus.attention;

    return EdgeReadiness([
      EdgeReadinessCheck(
        kind: EdgeReadinessKind.runtime,
        status: runtimeStatus,
        required: true,
      ),
      EdgeReadinessCheck(
        kind: EdgeReadinessKind.connection,
        status: connectionStatus,
        required: true,
      ),
      EdgeReadinessCheck(
        kind: EdgeReadinessKind.tools,
        status: toolsStatus,
        required: true,
      ),
      EdgeReadinessCheck(
        kind: EdgeReadinessKind.permissions,
        status: permissionsStatus,
        required: true,
      ),
      EdgeReadinessCheck(
        kind: EdgeReadinessKind.security,
        status: securityStatus,
        required: true,
      ),
      EdgeReadinessCheck(
        kind: EdgeReadinessKind.model,
        status: modelStatus,
        required: false,
      ),
    ]);
  }
}
