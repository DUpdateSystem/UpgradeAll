/// Getter-facing UI bridge contracts for the Flutter shell.
///
/// These DTOs are transport/rendering shapes. Product decisions such as
/// repository overlay resolution, update selection, Lua validation, migration
/// mapping, and storage behavior belong in Rust getter.
abstract interface class GetterAdapter {
  bool get supportsLegacyRoomImport;

  void initialize();

  List<RepositorySummary> listRepositories();

  List<TrackedPackageSummary> listTrackedPackages();

  PackageEvaluation evaluatePackage(String packageId, {String? repositoryId});

  List<MigrationReportSummary> readMigrationReports();

  LegacyMigrationImportResult importLegacyRoomDatabase(String databasePath);

  List<DownloadTaskSummary> listDownloadTasks();

  TaskEventPage listTaskEvents({required int after, required int limit});

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
  bool get supportsLegacyRoomImport => false;

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

  static const _downloadTasks = <DownloadTaskSummary>[
    DownloadTaskSummary(
      id: 'task-1',
      packageId: 'android/org.fdroid.fdroid',
      status: 'succeeded',
      executor: 'fake',
      actions: <Map<String, Object?>>[
        <String, Object?>{
          'type': 'download',
          'url': 'https://example.invalid/app.apk',
          'file_name': 'app.apk',
        },
        <String, Object?>{
          'type': 'install',
          'installer': 'android_package',
          'file': 'app.apk',
        },
      ],
      downloadFileName: 'app.apk',
      downloadedFile: 'app.apk',
      failureMessage: null,
      installHandoffId: 'handoff-1',
    ),
  ];

  static const _taskEvents = TaskEventPage(
    events: <TaskEventSummary>[
      TaskEventSummary(
        cursor: 1,
        taskId: 'task-1',
        kind: 'task_created',
        status: 'queued',
        message: 'Task created',
      ),
      TaskEventSummary(
        cursor: 2,
        taskId: 'task-1',
        kind: 'task_succeeded',
        status: 'succeeded',
        message: 'Task succeeded',
      ),
      TaskEventSummary(
        cursor: 3,
        taskId: 'task-1',
        kind: 'install_handoff_requested',
        status: 'succeeded',
        message: 'Install handoff requested',
      ),
    ],
    nextCursor: 3,
    hasMore: false,
  );

  @override
  List<MigrationReportSummary> readMigrationReports() {
    return const <MigrationReportSummary>[];
  }

  @override
  LegacyMigrationImportResult importLegacyRoomDatabase(String databasePath) {
    throw const GetterBridgeException(
      GetterError(
        code: 'bridge.not_connected',
        message: 'Getter migration import bridge is not connected',
      ),
    );
  }

  @override
  List<DownloadTaskSummary> listDownloadTasks() => _downloadTasks;

  @override
  TaskEventPage listTaskEvents({required int after, required int limit}) {
    final events = _taskEvents.events
        .where((event) => event.cursor > after)
        .take(limit)
        .toList(growable: false);
    final nextCursor = events.isEmpty ? after : events.last.cursor;
    return TaskEventPage(
      events: events,
      nextCursor: nextCursor,
      hasMore: _taskEvents.events.any((event) => event.cursor > nextCursor),
    );
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

class LegacyMigrationImportResult {
  const LegacyMigrationImportResult({
    required this.alreadyImported,
    required this.importedRecords,
    required this.trackedPackages,
    required this.warnings,
    required this.sourceCounts,
  });

  final bool alreadyImported;
  final int importedRecords;
  final List<TrackedPackageSummary> trackedPackages;
  final List<MigrationWarningSummary> warnings;
  final MigrationSourceCounts? sourceCounts;
}

class MigrationWarningSummary {
  const MigrationWarningSummary({required this.code, required this.message});

  final String code;
  final String message;
}

class MigrationSourceCounts {
  const MigrationSourceCounts({
    required this.appRows,
    required this.extraAppRows,
    required this.hubRows,
    required this.extraHubRows,
  });

  final int appRows;
  final int extraAppRows;
  final int hubRows;
  final int extraHubRows;
}

class DownloadTaskSummary {
  const DownloadTaskSummary({
    required this.id,
    required this.packageId,
    required this.status,
    required this.executor,
    required this.actions,
    required this.downloadFileName,
    required this.downloadedFile,
    required this.failureMessage,
    required this.installHandoffId,
  });

  final String id;
  final String packageId;
  final String status;
  final String executor;
  final List<Map<String, Object?>> actions;
  final String downloadFileName;
  final String? downloadedFile;
  final String? failureMessage;
  final String? installHandoffId;
}

class TaskEventPage {
  const TaskEventPage({
    required this.events,
    required this.nextCursor,
    required this.hasMore,
  });

  final List<TaskEventSummary> events;
  final int nextCursor;
  final bool hasMore;
}

class TaskEventSummary {
  const TaskEventSummary({
    required this.cursor,
    required this.taskId,
    required this.kind,
    required this.status,
    required this.message,
  });

  final int cursor;
  final String taskId;
  final String kind;
  final String? status;
  final String? message;
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
