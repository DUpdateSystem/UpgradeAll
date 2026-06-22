/// Getter-facing UI bridge contracts for the Flutter shell.
///
/// These DTOs are transport/rendering shapes. Product decisions such as
/// repository overlay resolution, update selection, Lua validation, migration
/// mapping, and storage behavior belong in Rust getter.
abstract interface class GetterAdapter {
  void initialize();

  List<RepositorySummary> listRepositories();

  List<TrackedPackageSummary> listTrackedPackages();

  PackageEvaluation evaluatePackage(String packageId, {String? repositoryId});

  List<MigrationReportSummary> readMigrationReports();

  GetterSnapshot loadSnapshot();
}

class FakeGetterAdapter implements GetterAdapter {
  const FakeGetterAdapter();

  static const _snapshot = GetterSnapshot(
    status: 'Fake getter ready',
    updateCount: 0,
    apps: <AppSummary>[
      AppSummary(
        id: 'android/org.fdroid.fdroid',
        name: 'F-Droid',
        installedVersion: '1.20.0',
        latestVersion: '1.20.0',
        hasFreeNetworkWarning: true,
      ),
    ],
    repositories: <RepositorySummary>[
      RepositorySummary(id: 'local', priority: 100),
      RepositorySummary(id: 'official', priority: 0),
      RepositorySummary(id: 'local_autogen', priority: -1),
    ],
  );

  @override
  void initialize() {}

  @override
  List<RepositorySummary> listRepositories() => _snapshot.repositories;

  @override
  List<TrackedPackageSummary> listTrackedPackages() {
    return const <TrackedPackageSummary>[
      TrackedPackageSummary(
        id: 'android/org.fdroid.fdroid',
        enabled: true,
        favorite: false,
        ignoredVersion: null,
        repositoryId: 'official',
        packageResolution: 'official_repository_package',
      ),
    ];
  }

  @override
  PackageEvaluation evaluatePackage(String packageId, {String? repositoryId}) {
    if (packageId != 'android/org.fdroid.fdroid') {
      throw const GetterBridgeException(
        GetterError(
          code: 'package.not_found',
          message: 'Fake package not found',
        ),
      );
    }
    return const PackageEvaluation(
      id: 'android/org.fdroid.fdroid',
      repositoryId: 'official',
      name: 'F-Droid',
      hasFreeNetworkWarning: true,
    );
  }

  @override
  List<MigrationReportSummary> readMigrationReports() {
    return const <MigrationReportSummary>[];
  }

  @override
  GetterSnapshot loadSnapshot() => _snapshot;
}

class GetterSnapshot {
  const GetterSnapshot({
    required this.status,
    required this.updateCount,
    required this.apps,
    required this.repositories,
  });

  final String status;
  final int updateCount;
  final List<AppSummary> apps;
  final List<RepositorySummary> repositories;
}

class AppSummary {
  const AppSummary({
    required this.id,
    required this.name,
    required this.installedVersion,
    required this.latestVersion,
    required this.hasFreeNetworkWarning,
  });

  final String id;
  final String name;
  final String installedVersion;
  final String latestVersion;
  final bool hasFreeNetworkWarning;
}

class RepositorySummary {
  const RepositorySummary({required this.id, required this.priority});

  final String id;
  final int priority;
}

class TrackedPackageSummary {
  const TrackedPackageSummary({
    required this.id,
    required this.enabled,
    required this.favorite,
    required this.ignoredVersion,
    required this.repositoryId,
    required this.packageResolution,
  });

  final String id;
  final bool enabled;
  final bool favorite;
  final String? ignoredVersion;
  final String? repositoryId;
  final String packageResolution;
}

class PackageEvaluation {
  const PackageEvaluation({
    required this.id,
    required this.repositoryId,
    required this.name,
    required this.hasFreeNetworkWarning,
  });

  final String id;
  final String repositoryId;
  final String name;
  final bool hasFreeNetworkWarning;
}

class MigrationReportSummary {
  const MigrationReportSummary({
    required this.ok,
    required this.code,
    required this.message,
    required this.importedRecords,
    required this.trackedRecords,
  });

  final bool ok;
  final String code;
  final String message;
  final int importedRecords;
  final int trackedRecords;
}

class GetterError {
  const GetterError({required this.code, required this.message, this.detail});

  final String code;
  final String message;
  final String? detail;
}

class GetterBridgeException implements Exception {
  const GetterBridgeException(this.error, {this.exitCode});

  final GetterError error;
  final int? exitCode;

  @override
  String toString() {
    final detail = error.detail == null ? '' : ': ${error.detail}';
    final exit = exitCode == null ? '' : ' (exit $exitCode)';
    return 'GetterBridgeException$exit: ${error.code}: ${error.message}$detail';
  }
}
