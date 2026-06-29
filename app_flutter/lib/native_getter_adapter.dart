// ignore_for_file: prefer_initializing_formals

import 'dart:convert';

import 'package:flutter/services.dart';

import 'getter_adapter.dart';

/// Android production getter bridge.
///
/// The bridge returns getter-owned JSON envelopes; Dart parses and renders them
/// but does not scan PackageManager, resolve repositories, evaluate Lua, or make
/// autogen/update/runtime decisions.
class MethodChannelGetterAdapter extends FakeGetterAdapter {
  // Keep public parameter names stable for tests and injected bridges.
  const MethodChannelGetterAdapter({
    MethodChannel channel = const MethodChannel(
      'net.xzos.upgradeall/getter_bridge',
    ),
    EventChannel runtimeNotificationChannel = const EventChannel(
      'net.xzos.upgradeall/runtime_notifications',
    ),
  }) : _channel = channel,
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
        .map(
          (report) =>
              MigrationReportSummary.fromJson(_asMap(report, 'legacy report')),
        )
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
      _autogenApplyArguments(preview, acceptedPackageIds),
    );
    return InstalledAutogenApplyResult.fromJson(data);
  }

  @override
  Future<InstalledAutogenPreview> previewInstalledFdroidAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    final data = await _invokeGetterData(
      'previewInstalledFdroidAutogen',
      <String, Object?>{'scan_options': options.toJson()},
    );
    return InstalledAutogenPreview.fromJson(data);
  }

  @override
  Future<InstalledAutogenPreview> previewFdroidAutogen(
    Map<String, Object?> payload,
  ) async {
    final data = await _invokeGetterData(
      'previewFdroidAutogen',
      <String, Object?>{'payload': payload},
    );
    return InstalledAutogenPreview.fromJson(data);
  }

  @override
  Future<InstalledAutogenApplyResult> applyFdroidAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    final data = await _invokeGetterData(
      'applyFdroidAutogen',
      _autogenApplyArguments(preview, acceptedPackageIds),
    );
    return InstalledAutogenApplyResult.fromJson(data);
  }

  @override
  Future<InstalledAutogenApplyResult> applyInstalledFdroidAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    final data = await _invokeGetterData(
      'applyInstalledFdroidAutogen',
      _autogenApplyArguments(preview, acceptedPackageIds),
    );
    return InstalledAutogenApplyResult.fromJson(data);
  }

  Future<Map<String, Object?>> invokeReadOperation(
    String operation, {
    Map<String, Object?> payload = const <String, Object?>{},
  }) {
    return _invokeGetterData('readOperation', <String, Object?>{
      'operation': operation,
      'payload': payload,
    });
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

  @override
  Stream<RuntimeNotificationEnvelope> runtimeNotificationEnvelopes() {
    return runtimeNotifications().map(RuntimeNotificationEnvelope.fromJson);
  }

  Future<Map<String, Object?>> invokeRuntimeOperation(
    String operation, {
    Map<String, Object?> payload = const <String, Object?>{},
  }) {
    return _invokeGetterData('runtimeOperation', <String, Object?>{
      'operation': operation,
      'payload': payload,
    });
  }

  @override
  Future<GetterSnapshot> loadSnapshot() async {
    final repositoriesData = await invokeReadOperation('repository_list');
    final trackedData = await invokeReadOperation('tracked_package_list');
    final repositories = _asList(
      repositoriesData['repositories'],
      'repositories',
    ).map(_repositoryFromJson).toList(growable: false);
    final trackedPackages = _asList(trackedData['packages'], 'tracked packages')
        .map(
          (tracked) => TrackedPackageSummary.fromJson(
            _asMap(tracked, 'tracked package'),
          ),
        )
        .toList(growable: false);
    final apps = <AppSummary>[];
    for (final tracked in trackedPackages) {
      try {
        final package = await _evaluatePackageFromGetter(
          tracked.id,
          repositoryId: tracked.repositoryId,
        );
        apps.add(
          AppSummary(
            id: tracked.id,
            name: package.name,
            installedVersion: 'unknown',
            latestVersion: 'unknown',
            hasFreeNetworkWarning: package.hasFreeNetworkWarning,
          ),
        );
      } catch (_) {
        apps.add(
          AppSummary(
            id: tracked.id,
            name: tracked.id,
            installedVersion: 'unknown',
            latestVersion: 'unknown',
            hasFreeNetworkWarning: false,
          ),
        );
      }
    }
    return GetterSnapshot(
      status: 'Getter native bridge ready',
      updateCount: 0,
      apps: apps,
      repositories: repositories,
    );
  }

  Future<PackageEvaluation> _evaluatePackageFromGetter(
    String packageId, {
    String? repositoryId,
  }) async {
    final data = await invokeReadOperation(
      'package_eval',
      payload: <String, Object?>{
        'package_id': packageId,
        'repository_id': ?repositoryId,
      },
    );
    return _packageEvaluationFromJson(_asMap(data['package'], 'package'));
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
      'repository_id': ?repositoryId,
      'installed_version': ?installedVersion,
      'pin_version': ?pinVersion,
    };
    final data = await invokeRuntimeOperation(
      'update_check_package_issue_action',
      payload: payload,
    );
    return RuntimeUpdateCheckResult.fromJson(data);
  }

  @override
  Future<RuntimeTaskSnapshot> submitRuntimeAction(String actionId) {
    return _runtimeTaskOperation('task_submit', <String, Object?>{
      'action_id': actionId,
    });
  }

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async {
    final data = await invokeRuntimeOperation(
      'task_list',
      payload: <String, Object?>{'active': active, 'package_id': ?packageId},
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
    return _runtimeTaskOperation('task_user_result', <String, Object?>{
      'task_id': taskId,
      'result': result.wireName,
      'reason': ?reason,
    });
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

  Map<String, Object?> _autogenApplyArguments(
    InstalledAutogenPreview preview,
    List<String>? acceptedPackageIds,
  ) {
    return <String, Object?>{
      'preview_json': jsonEncode(preview.rawJson),
      'acceptance': acceptedPackageIds == null
          ? const <String, Object?>{'mode': 'all'}
          : <String, Object?>{
              'mode': 'packages',
              'package_ids': acceptedPackageIds,
            },
    };
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

RepositorySummary _repositoryFromJson(Object? value) {
  final json = _asMap(value, 'repository');
  return RepositorySummary(
    id: _asString(json['id'], 'repository.id'),
    priority: _asInt(json['priority'], 'repository.priority'),
  );
}

PackageEvaluation _packageEvaluationFromJson(Map<String, Object?> json) {
  final permissions = _asMap(json['permissions'], 'package.permissions');
  return PackageEvaluation(
    id: _asString(json['id'], 'package.id'),
    repositoryId: _asString(json['repository'], 'package.repository'),
    name: _asString(json['name'], 'package.name'),
    hasFreeNetworkWarning: _asBool(
      permissions['free_network'],
      'package.permissions.free_network',
    ),
  );
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

int _asInt(Object? value, String name) {
  if (value is int) return value;
  throw FormatException('$name should be an integer');
}

bool _asBool(Object? value, String name) {
  if (value is bool) return value;
  throw FormatException('$name should be a boolean');
}
