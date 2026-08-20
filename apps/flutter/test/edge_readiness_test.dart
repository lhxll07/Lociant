import 'package:flutter_test/flutter_test.dart';
import 'package:lociant_flutter/core/edge_readiness.dart';
import 'package:lociant_flutter/core/models.dart';

void main() {
  test('a configured node is ready without a local model', () {
    final readiness = EdgeReadiness.evaluate(
      runtime: _runtime(
        running: true,
        authToken: 'local-token',
        notificationPermissionGranted: true,
        batteryOptimizationIgnored: true,
      ),
      healthOk: true,
      toolsOk: true,
      toolCount: 2,
      android: true,
    );

    expect(readiness.overall, EdgeReadinessStatus.ready);
    expect(readiness.readyCount, readiness.requiredCount);
    expect(readiness.checks.last.status, EdgeReadinessStatus.optional);
  });

  test('missing Android notification permission blocks readiness', () {
    final readiness = EdgeReadiness.evaluate(
      runtime: _runtime(
        running: true,
        authToken: 'local-token',
        notificationPermissionGranted: false,
        batteryOptimizationIgnored: true,
      ),
      healthOk: true,
      toolsOk: true,
      toolCount: 1,
      android: true,
    );

    expect(readiness.overall, EdgeReadinessStatus.blocked);
    expect(
      readiness.checks
          .firstWhere((check) => check.kind == EdgeReadinessKind.permissions)
          .status,
      EdgeReadinessStatus.blocked,
    );
  });

  test(
    'a selected but unloaded model needs attention without blocking the node',
    () {
      final readiness = EdgeReadiness.evaluate(
        runtime: _runtime(
          running: true,
          authToken: 'local-token',
          modelId: 'qwen-local',
          notificationPermissionGranted: true,
          batteryOptimizationIgnored: false,
        ),
        healthOk: true,
        toolsOk: true,
        toolCount: 1,
        android: true,
      );

      expect(readiness.overall, EdgeReadinessStatus.attention);
      expect(
        readiness.checks
            .firstWhere((check) => check.kind == EdgeReadinessKind.model)
            .status,
        EdgeReadinessStatus.attention,
      );
    },
  );

  test('a desktop node does not require Android permissions', () {
    final readiness = EdgeReadiness.evaluate(
      runtime: _runtime(running: true, authToken: 'local-token'),
      healthOk: true,
      toolsOk: true,
      toolCount: 1,
      android: false,
    );

    expect(
      readiness.checks
          .firstWhere((check) => check.kind == EdgeReadinessKind.permissions)
          .status,
      EdgeReadinessStatus.ready,
    );
  });

  test('a failed tool call blocks tool readiness', () {
    final readiness = EdgeReadiness.evaluate(
      runtime: _runtime(running: true, authToken: 'local-token'),
      healthOk: true,
      toolsOk: true,
      toolCallOk: false,
      toolCount: 1,
      android: false,
    );

    expect(readiness.overall, EdgeReadinessStatus.blocked);
    expect(
      readiness.checks
          .firstWhere((check) => check.kind == EdgeReadinessKind.tools)
          .status,
      EdgeReadinessStatus.blocked,
    );
  });
}

RuntimeUiState _runtime({
  bool running = false,
  bool starting = false,
  String authToken = '',
  String modelId = '',
  bool modelLoaded = false,
  bool modelLoading = false,
  bool notificationPermissionGranted = false,
  bool batteryOptimizationIgnored = false,
}) {
  return RuntimeUiState.fromJson({
    'running': running,
    'starting': starting,
    'authToken': authToken,
    'modelId': modelId,
    'modelLoaded': modelLoaded,
    'modelLoading': modelLoading,
    'notificationPermissionGranted': notificationPermissionGranted,
    'batteryOptimizationIgnored': batteryOptimizationIgnored,
  });
}
