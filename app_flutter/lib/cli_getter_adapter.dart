import 'dart:convert';
import 'dart:io';

import 'getter_adapter.dart';

class CliGetterAdapter implements GetterAdapter {
  const CliGetterAdapter({
    required this.executable,
    required this.dataDir,
    this.environment = const <String, String>{},
  });

  final String executable;
  final String dataDir;
  final Map<String, String> environment;

  @override
  bool get supportsLegacyRoomImport => true;

  @override
  bool get supportsInstalledAutogen => false;

  @override
  void initialize() {
    _runGetter(const <String>['init']);
  }

  @override
  List<RepositorySummary> listRepositories() {
    final json = _runGetter(const <String>['repo', 'list']);
    final repositories = _asList(_data(json)['repositories'], 'repositories');
    return repositories.map(_repositoryFromJson).toList(growable: false);
  }

  @override
  List<TrackedPackageSummary> listTrackedPackages() {
    final json = _runGetter(const <String>['app', 'list']);
    final apps = _asList(_data(json)['apps'], 'apps');
    return apps.map(_trackedPackageFromJson).toList(growable: false);
  }

  @override
  PackageEvaluation evaluatePackage(String packageId, {String? repositoryId}) {
    final args = <String>['package', 'eval', packageId];
    if (repositoryId != null) {
      args.addAll(<String>['--repo', repositoryId]);
    }
    final json = _runGetter(args);
    final package = _asMap(_data(json)['package'], 'package');
    return _packageEvaluationFromJson(package);
  }

  @override
  Future<List<MigrationReportSummary>> readMigrationReports() async {
    final json = _runGetter(const <String>['legacy', 'report-list']);
    final reports = _asList(_data(json)['reports'], 'reports');
    return reports
        .map(
          (report) => MigrationReportSummary.fromJson(_asMap(report, 'report')),
        )
        .toList(growable: false);
  }

  @override
  Future<LegacyMigrationImportResult> importLegacyRoomDatabase(
    String databasePath,
  ) async {
    final json = _runGetter(<String>['legacy', 'import-room-db', databasePath]);
    return LegacyMigrationImportResult.fromJson(_data(json));
  }

  @override
  Future<InstalledAutogenPreview> previewInstalledAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter cannot scan Android installed inventory',
      ),
    );
  }

  @override
  Future<InstalledAutogenApplyResult> applyInstalledAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter cannot apply Android installed autogen previews',
      ),
    );
  }

  @override
  Future<RuntimeUpdateCheckResult> checkPackageForUpdate(
    String packageId, {
    String? repositoryId,
    String? installedVersion,
    String? pinVersion,
  }) async {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter does not host a process-lifetime runtime',
      ),
    );
  }

  @override
  Future<RuntimeTaskSnapshot> submitRuntimeAction(String actionId) {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter does not host a process-lifetime runtime',
      ),
    );
  }

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter does not host a process-lifetime runtime',
      ),
    );
  }

  @override
  Future<RuntimeTaskSnapshot> getRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> startRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> pauseRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> resumeRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> cancelRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> retryRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> removeRuntimeTask(String taskId) =>
      _unsupportedRuntimeTask();

  @override
  Future<RuntimeTaskSnapshot> sendRuntimeUserResult(
    String taskId,
    RuntimeUserResult result, {
    String? reason,
  }) => _unsupportedRuntimeTask();

  @override
  Future<List<RuntimeTaskSnapshot>> cleanRuntimeTasks({
    RuntimeTaskCleanMode mode = RuntimeTaskCleanMode.defaultMode,
  }) {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter does not host a process-lifetime runtime',
      ),
    );
  }

  Future<RuntimeTaskSnapshot> _unsupportedRuntimeTask() {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.unsupported',
        message: 'CLI adapter does not host a process-lifetime runtime',
      ),
    );
  }

  @override
  Future<GetterSnapshot> loadSnapshot() async {
    initialize();
    final repositories = listRepositories();
    final trackedPackages = listTrackedPackages();
    final apps = trackedPackages
        .map((tracked) {
          final evaluated = evaluatePackage(
            tracked.id,
            repositoryId: tracked.repositoryId,
          );
          return AppSummary(
            id: tracked.id,
            name: evaluated.name,
            installedVersion: 'unknown',
            latestVersion: 'unknown',
            hasFreeNetworkWarning: evaluated.hasFreeNetworkWarning,
          );
        })
        .toList(growable: false);

    return GetterSnapshot(
      status: 'Getter CLI ready',
      updateCount: 0,
      apps: apps,
      repositories: repositories,
    );
  }

  Map<String, Object?> _runGetter(List<String> commandArgs) {
    final result = Process.runSync(executable, <String>[
      '--data-dir',
      dataDir,
      ...commandArgs,
    ], environment: environment.isEmpty ? null : environment);
    final stdoutText = result.stdout.toString();
    final decoded = stdoutText.trim().isEmpty
        ? <String, Object?>{}
        : _asMap(jsonDecode(stdoutText), 'getter stdout');
    if (result.exitCode != 0 || decoded['ok'] != true) {
      final error = _errorFromEnvelope(decoded);
      throw GetterBridgeException(error, exitCode: result.exitCode);
    }
    return decoded;
  }
}

Map<String, Object?> _data(Map<String, Object?> envelope) {
  return _asMap(envelope['data'], 'data');
}

GetterError _errorFromEnvelope(Map<String, Object?> envelope) {
  final error = _asMap(envelope['error'], 'error');
  return GetterError(
    code: _asString(error['code'], 'error.code'),
    message: _asString(error['message'], 'error.message'),
    detail: error['detail'] as String?,
  );
}

RepositorySummary _repositoryFromJson(Object? value) {
  final json = _asMap(value, 'repository');
  return RepositorySummary(
    id: _asString(json['id'], 'repository.id'),
    priority: _asInt(json['priority'], 'repository.priority'),
  );
}

TrackedPackageSummary _trackedPackageFromJson(Object? value) {
  final json = _asMap(value, 'tracked package');
  return TrackedPackageSummary(
    id: _asString(json['id'], 'tracked.id'),
    enabled: _asBool(json['enabled'], 'tracked.enabled'),
    favorite: _asBool(json['favorite'], 'tracked.favorite'),
    pinVersion: json['pin_version'] as String?,
    repositoryId: json['repository_id'] as String?,
    packageResolution: _asString(
      json['package_resolution'],
      'tracked.package_resolution',
    ),
  );
}

PackageEvaluation _packageEvaluationFromJson(Object? value) {
  final json = _asMap(value, 'package');
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

Map<String, Object?> _asMap(Object? value, String name) {
  if (value is Map<String, Object?>) {
    return value;
  }
  if (value is Map) {
    return value.cast<String, Object?>();
  }
  throw FormatException('$name should be a JSON object');
}

List<Object?> _asList(Object? value, String name) {
  if (value is List<Object?>) {
    return value;
  }
  if (value is List) {
    return value.cast<Object?>();
  }
  throw FormatException('$name should be a JSON array');
}

String _asString(Object? value, String name) {
  if (value is String) {
    return value;
  }
  throw FormatException('$name should be a string');
}

int _asInt(Object? value, String name) {
  if (value is int) {
    return value;
  }
  throw FormatException('$name should be an integer');
}

bool _asBool(Object? value, String name) {
  if (value is bool) {
    return value;
  }
  throw FormatException('$name should be a boolean');
}
