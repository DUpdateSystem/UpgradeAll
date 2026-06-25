import 'dart:convert';

import 'package:flutter/services.dart';

import 'getter_adapter.dart';

/// First Android production bridge slice.
///
/// Until the full native getter bridge replaces every CLI/fake surface, this
/// adapter inherits the deterministic shell data from [FakeGetterAdapter] and
/// overrides only installed-autogen operations with the Rust/native bridge.
/// The bridge returns getter-owned JSON envelopes; Dart parses and renders them
/// but does not scan PackageManager or make autogen/package decisions.
class MethodChannelGetterAdapter extends FakeGetterAdapter {
  const MethodChannelGetterAdapter({
    MethodChannel channel = const MethodChannel(
      'net.xzos.upgradeall/getter_bridge',
    ),
    EventChannel runtimeNotificationChannel = const EventChannel(
      'net.xzos.upgradeall/runtime_notifications',
    ),
  })  : _channel = channel,
        _runtimeNotificationChannel = runtimeNotificationChannel;

  final MethodChannel _channel;
  final EventChannel _runtimeNotificationChannel;

  @override
  bool get supportsLegacyRoomImport => true;

  @override
  bool get supportsInstalledAutogen => true;

  @override
  void initialize() {
    // The installed-autogen bridge initializes lazily when preview is called.
  }

  @override
  Future<List<MigrationReportSummary>> readMigrationReports() async {
    final data = await _invokeGetterData(
      'legacyReportList',
      const <String, Object?>{},
    );
    final reports = _asList(data['reports'], 'legacy reports');
    return reports
        .map((report) => MigrationReportSummary.fromJson(
              _asMap(report, 'legacy report'),
            ))
        .toList(growable: false);
  }

  @override
  Future<LegacyMigrationImportResult> importLegacyRoomDatabase(
    String databasePath,
  ) async {
    final data = await _invokeGetterData(
      'importLegacyRoomDatabase',
      <String, Object?>{'database_path': databasePath},
    );
    return LegacyMigrationImportResult.fromJson(data);
  }

  @override
  Future<InstalledAutogenPreview> previewInstalledAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    final data = await _invokeGetterData(
      'previewInstalledAutogen',
      <String, Object?>{'scan_options': options.toJson()},
    );
    return InstalledAutogenPreview.fromJson(data);
  }

  @override
  Future<InstalledAutogenApplyResult> applyInstalledAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    final data = await _invokeGetterData(
      'applyInstalledAutogen',
      <String, Object?>{
        'preview_json': jsonEncode(preview.rawJson),
        'acceptance': acceptedPackageIds == null
            ? const <String, Object?>{'mode': 'all'}
            : <String, Object?>{
                'mode': 'packages',
                'package_ids': acceptedPackageIds,
              },
      },
    );
    return InstalledAutogenApplyResult.fromJson(data);
  }

  /// Invoke a getter runtime operation through the native bridge.
  ///
  /// This is an internal/debug bridge primitive for ADR-0011 wiring. Product UI
  /// should use typed getter operations and getter-issued `action_id`s rather
  /// than assembling runtime action plans in Dart.
  Stream<Map<String, Object?>> runtimeNotifications() {
    return _runtimeNotificationChannel.receiveBroadcastStream().map((event) {
      if (event is String) {
        return _asMap(jsonDecode(event), 'runtime notification');
      }
      return _asMap(event, 'runtime notification');
    });
  }

  Stream<RuntimeNotificationEnvelope> runtimeNotificationEnvelopes() {
    return runtimeNotifications().map(RuntimeNotificationEnvelope.fromJson);
  }

  Future<Map<String, Object?>> invokeRuntimeOperation(
    String operation, {
    Map<String, Object?> payload = const <String, Object?>{},
  }) {
    return _invokeGetterData(
      'runtimeOperation',
      <String, Object?>{
        'operation': operation,
        'payload': payload,
      },
    );
  }

  @override
  Future<RuntimeUpdateCheckResult> checkPackageForUpdate(
    String packageId, {
    String? repositoryId,
    String? installedVersion,
    String? pinVersion,
  }) async {
    final payload = <String, Object?>{
      'package_id': packageId,
      if (repositoryId != null) 'repository_id': repositoryId,
      if (installedVersion != null) 'installed_version': installedVersion,
      if (pinVersion != null) 'pin_version': pinVersion,
    };
    final data = await invokeRuntimeOperation(
      'update_check_package_issue_action',
      payload: payload,
    );
    return RuntimeUpdateCheckResult.fromJson(data);
  }

  @override
  Future<RuntimeTaskSnapshot> submitRuntimeAction(String actionId) {
    return _runtimeTaskOperation(
      'task_submit',
      <String, Object?>{'action_id': actionId},
    );
  }

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async {
    final data = await invokeRuntimeOperation(
      'task_list',
      payload: <String, Object?>{
        'active': active,
        if (packageId != null) 'package_id': packageId,
      },
    );
    return _runtimeTasksFromData(data);
  }

  @override
  Future<RuntimeTaskSnapshot> getRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_get', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> startRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_start', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> pauseRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_pause', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> resumeRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_resume', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> cancelRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_cancel', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> retryRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_retry', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> removeRuntimeTask(String taskId) {
    return _runtimeTaskOperation('task_remove', _taskIdPayload(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> sendRuntimeUserResult(
    String taskId,
    RuntimeUserResult result, {
    String? reason,
  }) {
    return _runtimeTaskOperation(
      'task_user_result',
      <String, Object?>{
        'task_id': taskId,
        'result': result.wireName,
        if (reason != null) 'reason': reason,
      },
    );
  }

  @override
  Future<List<RuntimeTaskSnapshot>> cleanRuntimeTasks({
    RuntimeTaskCleanMode mode = RuntimeTaskCleanMode.defaultMode,
  }) async {
    final data = await invokeRuntimeOperation(
      'task_clean',
      payload: <String, Object?>{'mode': mode.wireName},
    );
    return _runtimeTasksFromData(data);
  }

  Future<RuntimeTaskSnapshot> _runtimeTaskOperation(
    String operation,
    Map<String, Object?> payload,
  ) async {
    final data = await invokeRuntimeOperation(operation, payload: payload);
    return RuntimeTaskSnapshot.fromJson(data);
  }

  List<RuntimeTaskSnapshot> _runtimeTasksFromData(Map<String, Object?> data) {
    return _asList(data['tasks'], 'runtime tasks')
        .map((task) => RuntimeTaskSnapshot.fromJson(_asMap(task, 'task')))
        .toList(growable: false);
  }

  Map<String, Object?> _taskIdPayload(String taskId) {
    return <String, Object?>{'task_id': taskId};
  }

  Future<Map<String, Object?>> _invokeGetterData(
    String method,
    Map<String, Object?> arguments,
  ) async {
    try {
      final response = await _channel.invokeMethod<String>(method, arguments);
      if (response == null || response.isEmpty) {
        throw const GetterBridgeException(
          GetterError(
            code: 'bridge.empty_response',
            message: 'Getter native bridge returned an empty response',
          ),
        );
      }
      final envelope = _asMap(jsonDecode(response), 'getter bridge response');
      if (envelope['ok'] != true) {
        throw GetterBridgeException(_errorFromEnvelope(envelope));
      }
      return _asMap(envelope['data'], 'getter bridge data');
    } on PlatformException catch (error) {
      throw GetterBridgeException(
        GetterError(
          code: error.code,
          message: error.message ?? 'Getter native bridge call failed',
          detail: error.details?.toString(),
        ),
      );
    }
  }
}

GetterError _errorFromEnvelope(Map<String, Object?> envelope) {
  final error = _asMap(envelope['error'], 'getter bridge error');
  return GetterError(
    code: _asString(error['code'], 'getter bridge error.code'),
    message: _asString(error['message'], 'getter bridge error.message'),
    detail: error['detail']?.toString(),
  );
}

Map<String, Object?> _asMap(Object? value, String name) {
  if (value is Map<String, Object?>) return value;
  if (value is Map) return value.cast<String, Object?>();
  throw FormatException('$name should be a JSON object');
}

List<Object?> _asList(Object? value, String name) {
  if (value is List<Object?>) return value;
  if (value is List) return value.cast<Object?>();
  throw FormatException('$name should be a JSON array');
}

String _asString(Object? value, String name) {
  if (value is String) return value;
  throw FormatException('$name should be a string');
}
