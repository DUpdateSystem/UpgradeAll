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
  List<MigrationReportSummary> readMigrationReports() {
    final json = _runGetter(const <String>['legacy', 'report-list']);
    final reports = _asList(_data(json)['reports'], 'reports');
    return reports
        .map((report) => _migrationReportFromJson(_asMap(report, 'report')))
        .toList(growable: false);
  }

  @override
  GetterSnapshot loadSnapshot() {
    initialize();
    final repositories = listRepositories();
    final trackedPackages = listTrackedPackages();
    final apps = trackedPackages.map((tracked) {
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
    }).toList(growable: false);

    return GetterSnapshot(
      status: 'Getter CLI ready',
      updateCount: 0,
      apps: apps,
      repositories: repositories,
    );
  }

  Map<String, Object?> _runGetter(List<String> commandArgs) {
    final result = Process.runSync(
      executable,
      <String>['--data-dir', dataDir, ...commandArgs],
      environment: environment.isEmpty ? null : environment,
    );
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
    ignoredVersion: json['ignored_version'] as String?,
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

MigrationReportSummary _migrationReportFromJson(Map<String, Object?> json) {
  return MigrationReportSummary(
    ok: _asBool(json['ok'], 'migration.ok'),
    code: _asString(json['code'], 'migration.code'),
    message: _asString(json['message'], 'migration.message'),
    importedRecords: _asInt(json['imported_records'], 'migration.imported'),
    trackedRecords: _asInt(json['tracked_records'], 'migration.tracked'),
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
