/// Getter-facing UI bridge contracts for the Flutter shell.
///
/// These DTOs are transport/rendering shapes. Product decisions such as
/// repository overlay resolution, update selection, Lua validation, migration
/// mapping, and storage behavior belong in Rust getter.
abstract interface class GetterAdapter {
  bool get supportsLegacyRoomImport;

  bool get supportsInstalledAutogen;

  void initialize();

  List<RepositorySummary> listRepositories();

  List<TrackedPackageSummary> listTrackedPackages();

  PackageEvaluation evaluatePackage(String packageId, {String? repositoryId});

  Future<List<MigrationReportSummary>> readMigrationReports();

  Future<LegacyMigrationImportResult> importLegacyRoomDatabase(
      String databasePath);

  List<DownloadTaskSummary> listDownloadTasks();

  TaskEventPage listTaskEvents({required int after, required int limit});

  Future<InstalledAutogenPreview> previewInstalledAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  });

  Future<InstalledAutogenApplyResult> applyInstalledAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  });

  Future<RuntimeUpdateCheckResult> checkPackageForUpdate(
    String packageId, {
    String? repositoryId,
    String? installedVersion,
    String? pinVersion,
  });

  Future<RuntimeTaskSnapshot> submitRuntimeAction(String actionId);

  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  });

  Future<RuntimeTaskSnapshot> getRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> startRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> pauseRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> resumeRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> cancelRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> retryRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> removeRuntimeTask(String taskId);

  Future<RuntimeTaskSnapshot> sendRuntimeUserResult(
    String taskId,
    RuntimeUserResult result, {
    String? reason,
  });

  Future<List<RuntimeTaskSnapshot>> cleanRuntimeTasks({
    RuntimeTaskCleanMode mode = RuntimeTaskCleanMode.defaultMode,
  });

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
  bool get supportsInstalledAutogen => true;

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
        pinVersion: null,
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
  Future<List<MigrationReportSummary>> readMigrationReports() async {
    return const <MigrationReportSummary>[];
  }

  @override
  Future<LegacyMigrationImportResult> importLegacyRoomDatabase(
      String databasePath) async {
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
  Future<InstalledAutogenPreview> previewInstalledAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    return InstalledAutogenPreview.fromJson(const <String, Object?>{
      'operation': 'installed.preview',
      'target_repo_id': 'local_autogen',
      'target_repo_path': '/fake/getter/repositories/local_autogen',
      'scan': <String, Object?>{
        'stats': <String, Object?>{
          'total_seen': 3,
          'returned': 1,
          'filtered_system': 1,
          'filtered_self': 1,
        },
        'diagnostics': <Object?>[],
      },
      'summary': <String, Object?>{
        'candidate_count': 1,
        'skipped_count': 1,
        'write_count': 1,
        'delete_count': 0,
      },
      'candidates': <Object?>[
        <String, Object?>{
          'package_id': 'android/com.example.autogen',
          'kind': 'android',
          'display_name': 'Example Autogen',
          'installed_target': <String, Object?>{
            'kind': 'android_package',
            'package_name': 'com.example.autogen',
          },
          'action': 'create',
          'output_relative_path': 'packages/android/com.example.autogen.lua',
          'content_hash': 'fnv1a64:fake',
          'content': '-- fake generated content',
        },
      ],
      'skipped': <Object?>[
        <String, Object?>{
          'package_id': 'android/org.fdroid.fdroid',
          'reason': 'covered_by_higher_priority_repo',
          'covering_repo_id': 'official',
        },
      ],
      'diagnostics': <Object?>[],
    });
  }

  @override
  Future<InstalledAutogenApplyResult> applyInstalledAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    return InstalledAutogenApplyResult.fromJson(const <String, Object?>{
      'target_repo_id': 'local_autogen',
      'target_repo_path': '/fake/getter/repositories/local_autogen',
      'applied_count': 1,
      'applied': <Object?>[
        <String, Object?>{
          'package_id': 'android/com.example.autogen',
          'output_relative_path': 'packages/android/com.example.autogen.lua',
        },
      ],
      'preserved_to_local': <Object?>[],
    });
  }

  @override
  Future<RuntimeUpdateCheckResult> checkPackageForUpdate(
    String packageId, {
    String? repositoryId,
    String? installedVersion,
    String? pinVersion,
  }) async {
    return RuntimeUpdateCheckResult.fromJson(<String, Object?>{
      'package': <String, Object?>{
        'id': packageId,
        'name': 'F-Droid',
        'repository': repositoryId ?? 'official',
        'permissions': <String, Object?>{'free_network': false},
      },
      'update': <String, Object?>{
        'network_required': false,
        'package_id': packageId,
        'installed_version': installedVersion,
        'effective_local_version': pinVersion ?? installedVersion,
        'policy': <String, Object?>{'pin_version': pinVersion},
        'status': 'update_available',
        'selected': <String, Object?>{
          'package_id': packageId,
          'candidate': <String, Object?>{
            'version': '1.2.0',
            'artifacts': <Object?>[
              <String, Object?>{
                'name': 'app.apk',
                'url': 'https://example.invalid/app.apk',
                'file_name': 'app.apk',
              },
            ],
          },
          'artifact': <String, Object?>{
            'name': 'app.apk',
            'url': 'https://example.invalid/app.apk',
            'file_name': 'app.apk',
          },
        },
        'actions': <Object?>[
          <String, Object?>{
            'type': 'download',
            'url': 'https://example.invalid/app.apk',
            'file_name': 'app.apk',
          },
        ],
      },
      'action': <String, Object?>{
        'action_id': 'action-fake',
        'package_id': packageId,
      },
    });
  }

  @override
  Future<RuntimeTaskSnapshot> submitRuntimeAction(String actionId) async {
    return RuntimeTaskSnapshot.fromJson(_runtimeTaskJson('task-1'));
  }

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async {
    return <RuntimeTaskSnapshot>[
      RuntimeTaskSnapshot.fromJson(_runtimeTaskJson('task-1')),
    ];
  }

  @override
  Future<RuntimeTaskSnapshot> getRuntimeTask(String taskId) async {
    return RuntimeTaskSnapshot.fromJson(_runtimeTaskJson(taskId));
  }

  @override
  Future<RuntimeTaskSnapshot> startRuntimeTask(String taskId) =>
      getRuntimeTask(taskId);

  @override
  Future<RuntimeTaskSnapshot> pauseRuntimeTask(String taskId) =>
      getRuntimeTask(taskId);

  @override
  Future<RuntimeTaskSnapshot> resumeRuntimeTask(String taskId) =>
      getRuntimeTask(taskId);

  @override
  Future<RuntimeTaskSnapshot> cancelRuntimeTask(String taskId) =>
      getRuntimeTask(taskId);

  @override
  Future<RuntimeTaskSnapshot> retryRuntimeTask(String taskId) =>
      getRuntimeTask(taskId);

  @override
  Future<RuntimeTaskSnapshot> removeRuntimeTask(String taskId) =>
      getRuntimeTask(taskId);

  @override
  Future<RuntimeTaskSnapshot> sendRuntimeUserResult(
    String taskId,
    RuntimeUserResult result, {
    String? reason,
  }) =>
      getRuntimeTask(taskId);

  @override
  Future<List<RuntimeTaskSnapshot>> cleanRuntimeTasks({
    RuntimeTaskCleanMode mode = RuntimeTaskCleanMode.defaultMode,
  }) async {
    return <RuntimeTaskSnapshot>[];
  }

  static Map<String, Object?> _runtimeTaskJson(String taskId) {
    return <String, Object?>{
      'task_id': taskId,
      'package_id': 'android/org.fdroid.fdroid',
      'status': 'queued',
      'phase': <String, Object?>{'category': 'queued'},
      'progress': null,
      'capabilities': <String, Object?>{
        'cancel': true,
        'pause': false,
        'resume': false,
        'retry': false,
      },
      'current_diagnostic': null,
      'updated_at': 1,
    };
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
    required this.pinVersion,
    required this.repositoryId,
    required this.packageResolution,
  });

  factory TrackedPackageSummary.fromJson(Map<String, Object?> json) {
    return TrackedPackageSummary(
      id: _jsonString(json['id'], 'tracked.id'),
      enabled: _jsonBool(json['enabled'], 'tracked.enabled'),
      favorite: _jsonBool(json['favorite'], 'tracked.favorite'),
      pinVersion: _jsonOptionalString(
        json['pin_version'],
        'tracked.pin_version',
      ),
      repositoryId:
          _jsonOptionalString(json['repository_id'], 'tracked.repository_id'),
      packageResolution: _jsonString(
        json['package_resolution'],
        'tracked.package_resolution',
      ),
    );
  }

  final String id;
  final bool enabled;
  final bool favorite;
  final String? pinVersion;
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

  factory MigrationReportSummary.fromJson(Map<String, Object?> json) {
    return MigrationReportSummary(
      ok: _jsonBool(json['ok'], 'migration.ok'),
      code: _jsonString(json['code'], 'migration.code'),
      message: _jsonString(json['message'], 'migration.message'),
      importedRecords: _jsonInt(json['imported_records'], 'migration.imported'),
      trackedRecords: _jsonInt(json['tracked_records'], 'migration.tracked'),
    );
  }

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

  factory LegacyMigrationImportResult.fromJson(Map<String, Object?> json) {
    final warningsValue = json['warnings'];
    final sourceCountsValue = json['source_counts'];
    return LegacyMigrationImportResult(
      alreadyImported: _jsonOptionalBool(
            json['already_imported'],
            'migration.already_imported',
          ) ??
          false,
      importedRecords: _jsonInt(json['imported_records'], 'migration.imported'),
      trackedPackages: _jsonList(json['apps'], 'migration.apps')
          .map((tracked) => TrackedPackageSummary.fromJson(
                _jsonMap(tracked, 'migration.tracked_package'),
              ))
          .toList(growable: false),
      warnings: warningsValue == null
          ? const <MigrationWarningSummary>[]
          : _jsonList(warningsValue, 'migration.warnings')
              .map((warning) => MigrationWarningSummary.fromJson(
                    _jsonMap(warning, 'migration.warning'),
                  ))
              .toList(growable: false),
      sourceCounts: sourceCountsValue == null
          ? null
          : MigrationSourceCounts.fromJson(
              _jsonMap(sourceCountsValue, 'migration.source_counts'),
            ),
    );
  }

  final bool alreadyImported;
  final int importedRecords;
  final List<TrackedPackageSummary> trackedPackages;
  final List<MigrationWarningSummary> warnings;
  final MigrationSourceCounts? sourceCounts;
}

class MigrationWarningSummary {
  const MigrationWarningSummary({required this.code, required this.message});

  factory MigrationWarningSummary.fromJson(Map<String, Object?> json) {
    return MigrationWarningSummary(
      code: _jsonString(json['code'], 'migration.warning.code'),
      message: _jsonString(json['message'], 'migration.warning.message'),
    );
  }

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

  factory MigrationSourceCounts.fromJson(Map<String, Object?> json) {
    return MigrationSourceCounts(
      appRows: _jsonInt(json['app_rows'], 'migration.source_counts.app_rows'),
      extraAppRows: _jsonInt(
        json['extra_app_rows'],
        'migration.source_counts.extra_app_rows',
      ),
      hubRows: _jsonInt(json['hub_rows'], 'migration.source_counts.hub_rows'),
      extraHubRows: _jsonInt(
        json['extra_hub_rows'],
        'migration.source_counts.extra_hub_rows',
      ),
    );
  }

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

class RuntimeUpdateCheckResult {
  const RuntimeUpdateCheckResult({
    required this.package,
    required this.update,
    required this.action,
  });

  factory RuntimeUpdateCheckResult.fromJson(Map<String, Object?> json) {
    return RuntimeUpdateCheckResult(
      package: RuntimePackageSummary.fromJson(
        _jsonMap(json['package'], 'runtime.package'),
      ),
      update: RuntimeUpdateSummary.fromJson(
        _jsonMap(json['update'], 'runtime.update'),
      ),
      action: json['action'] == null
          ? null
          : RuntimeIssuedAction.fromJson(
              _jsonMap(json['action'], 'runtime.action'),
            ),
    );
  }

  final RuntimePackageSummary package;
  final RuntimeUpdateSummary update;
  final RuntimeIssuedAction? action;
}

class RuntimePackageSummary {
  const RuntimePackageSummary({
    required this.id,
    required this.name,
    required this.repositoryId,
  });

  factory RuntimePackageSummary.fromJson(Map<String, Object?> json) {
    return RuntimePackageSummary(
      id: _jsonString(json['id'], 'runtime.package.id'),
      name: _jsonString(json['name'], 'runtime.package.name'),
      repositoryId:
          _jsonString(json['repository'], 'runtime.package.repository'),
    );
  }

  final String id;
  final String name;
  final String repositoryId;
}

class RuntimeUpdateSummary {
  const RuntimeUpdateSummary({
    required this.packageId,
    required this.status,
    required this.installedVersion,
    required this.effectiveLocalVersion,
    required this.selectedVersion,
    required this.actions,
  });

  factory RuntimeUpdateSummary.fromJson(Map<String, Object?> json) {
    final selected =
        _jsonMapOrNull(json['selected'], 'runtime.update.selected');
    final candidate = selected == null
        ? null
        : _jsonMap(selected['candidate'], 'runtime.update.selected.candidate');
    return RuntimeUpdateSummary(
      packageId: _jsonString(json['package_id'], 'runtime.update.package_id'),
      status: _jsonString(json['status'], 'runtime.update.status'),
      installedVersion: _jsonOptionalString(
        json['installed_version'],
        'runtime.update.installed_version',
      ),
      effectiveLocalVersion: _jsonOptionalString(
        json['effective_local_version'],
        'runtime.update.effective_local_version',
      ),
      selectedVersion: candidate == null
          ? null
          : _jsonString(
              candidate['version'], 'runtime.update.selected.version'),
      actions: _jsonList(json['actions'], 'runtime.update.actions')
          .map((action) => _jsonMap(action, 'runtime.update.action'))
          .toList(growable: false),
    );
  }

  final String packageId;
  final String status;
  final String? installedVersion;
  final String? effectiveLocalVersion;
  final String? selectedVersion;
  final List<Map<String, Object?>> actions;
}

class RuntimeIssuedAction {
  const RuntimeIssuedAction({required this.actionId, required this.packageId});

  factory RuntimeIssuedAction.fromJson(Map<String, Object?> json) {
    return RuntimeIssuedAction(
      actionId: _jsonString(json['action_id'], 'runtime.action.action_id'),
      packageId: _jsonString(json['package_id'], 'runtime.action.package_id'),
    );
  }

  final String actionId;
  final String packageId;
}

class RuntimeTaskSnapshot {
  const RuntimeTaskSnapshot({
    required this.taskId,
    required this.packageId,
    required this.status,
    required this.phase,
    required this.progress,
    required this.capabilities,
    required this.currentDiagnostic,
    required this.updatedAt,
  });

  factory RuntimeTaskSnapshot.fromJson(Map<String, Object?> json) {
    return RuntimeTaskSnapshot(
      taskId: _jsonString(json['task_id'], 'runtime.task.task_id'),
      packageId: _jsonString(json['package_id'], 'runtime.task.package_id'),
      status: _jsonString(json['status'], 'runtime.task.status'),
      phase: RuntimeTaskPhase.fromJson(
        _jsonMap(json['phase'], 'runtime.task.phase'),
      ),
      progress: json['progress'] == null
          ? null
          : RuntimeTaskProgress.fromJson(
              _jsonMap(json['progress'], 'runtime.task.progress'),
            ),
      capabilities: RuntimeTaskCapabilities.fromJson(
        _jsonMap(json['capabilities'], 'runtime.task.capabilities'),
      ),
      currentDiagnostic: json['current_diagnostic'] == null
          ? null
          : RuntimeTaskDiagnostic.fromJson(
              _jsonMap(
                json['current_diagnostic'],
                'runtime.task.current_diagnostic',
              ),
            ),
      updatedAt: _jsonInt(json['updated_at'], 'runtime.task.updated_at'),
    );
  }

  final String taskId;
  final String packageId;
  final String status;
  final RuntimeTaskPhase phase;
  final RuntimeTaskProgress? progress;
  final RuntimeTaskCapabilities capabilities;
  final RuntimeTaskDiagnostic? currentDiagnostic;
  final int updatedAt;
}

class RuntimeTaskPhase {
  const RuntimeTaskPhase({required this.category, required this.reason});

  factory RuntimeTaskPhase.fromJson(Map<String, Object?> json) {
    return RuntimeTaskPhase(
      category: _jsonString(json['category'], 'runtime.task.phase.category'),
      reason: _jsonOptionalString(json['reason'], 'runtime.task.phase.reason'),
    );
  }

  final String category;
  final String? reason;
}

class RuntimeTaskProgress {
  const RuntimeTaskProgress({
    required this.unit,
    required this.current,
    required this.total,
  });

  factory RuntimeTaskProgress.fromJson(Map<String, Object?> json) {
    return RuntimeTaskProgress(
      unit: _jsonString(json['unit'], 'runtime.task.progress.unit'),
      current: _jsonInt(json['current'], 'runtime.task.progress.current'),
      total: json['total'] == null
          ? null
          : _jsonInt(json['total'], 'runtime.task.progress.total'),
    );
  }

  final String unit;
  final int current;
  final int? total;
}

class RuntimeTaskCapabilities {
  const RuntimeTaskCapabilities({
    required this.cancel,
    required this.pause,
    required this.resume,
    required this.retry,
  });

  factory RuntimeTaskCapabilities.fromJson(Map<String, Object?> json) {
    return RuntimeTaskCapabilities(
      cancel: _jsonBool(json['cancel'], 'runtime.task.capabilities.cancel'),
      pause: _jsonBool(json['pause'], 'runtime.task.capabilities.pause'),
      resume: _jsonBool(json['resume'], 'runtime.task.capabilities.resume'),
      retry: _jsonBool(json['retry'], 'runtime.task.capabilities.retry'),
    );
  }

  final bool cancel;
  final bool pause;
  final bool resume;
  final bool retry;
}

class RuntimeTaskDiagnostic {
  const RuntimeTaskDiagnostic({
    required this.code,
    required this.message,
    required this.severity,
  });

  factory RuntimeTaskDiagnostic.fromJson(Map<String, Object?> json) {
    return RuntimeTaskDiagnostic(
      code: _jsonString(json['code'], 'runtime.task.diagnostic.code'),
      message: _jsonString(json['message'], 'runtime.task.diagnostic.message'),
      severity:
          _jsonString(json['severity'], 'runtime.task.diagnostic.severity'),
    );
  }

  final String code;
  final String message;
  final String severity;
}

enum RuntimeUserResult {
  accepted,
  rejected;

  String get wireName => switch (this) {
        RuntimeUserResult.accepted => 'accepted',
        RuntimeUserResult.rejected => 'rejected',
      };
}

enum RuntimeTaskCleanMode {
  defaultMode,
  failed,
  allInactive;

  String get wireName => switch (this) {
        RuntimeTaskCleanMode.defaultMode => 'default',
        RuntimeTaskCleanMode.failed => 'failed',
        RuntimeTaskCleanMode.allInactive => 'all_inactive',
      };
}

class RuntimeNotificationEnvelope {
  const RuntimeNotificationEnvelope({required this.kind, required this.task});

  factory RuntimeNotificationEnvelope.fromJson(Map<String, Object?> json) {
    final kind = _jsonString(json['kind'], 'runtime.notification.kind');
    return RuntimeNotificationEnvelope(
      kind: kind,
      task: kind == 'task_changed'
          ? RuntimeTaskSnapshot.fromJson(
              _jsonMap(json['task'], 'runtime.notification.task'),
            )
          : null,
    );
  }

  final String kind;
  final RuntimeTaskSnapshot? task;
}

class InstalledAutogenScanOptions {
  const InstalledAutogenScanOptions({
    this.includeSystemApps = false,
    this.includeSelf = false,
  });

  final bool includeSystemApps;
  final bool includeSelf;

  Map<String, Object?> toJson() => <String, Object?>{
        'include_system_apps': includeSystemApps,
        'include_self': includeSelf,
      };
}

class InstalledAutogenPreview {
  InstalledAutogenPreview({
    required this.operation,
    required this.targetRepoId,
    required this.targetRepoPath,
    required this.summary,
    required this.candidates,
    required this.skipped,
    required this.diagnostics,
    required this.scanStats,
    required this.rawJson,
  });

  factory InstalledAutogenPreview.fromJson(Map<String, Object?> json) {
    final scan = _jsonMapOrNull(json['scan'], 'autogen.scan');
    return InstalledAutogenPreview(
      operation: _jsonString(json['operation'], 'autogen.operation'),
      targetRepoId:
          _jsonString(json['target_repo_id'], 'autogen.target_repo_id'),
      targetRepoPath: _jsonOptionalString(
        json['target_repo_path'],
        'autogen.target_repo_path',
      ),
      summary: AutogenSummary.fromJson(
        _jsonMap(json['summary'], 'autogen.summary'),
      ),
      candidates: _jsonList(json['candidates'], 'autogen.candidates')
          .map((candidate) => InstalledAutogenCandidate.fromJson(
                _jsonMap(candidate, 'autogen.candidate'),
              ))
          .toList(growable: false),
      skipped: _jsonList(json['skipped'], 'autogen.skipped')
          .map((skip) => InstalledAutogenSkip.fromJson(
                _jsonMap(skip, 'autogen.skip'),
              ))
          .toList(growable: false),
      diagnostics: _jsonList(
        scan?['diagnostics'] ?? json['diagnostics'],
        'autogen.diagnostics',
      )
          .map((diagnostic) => PlatformDiagnosticSummary.fromJson(
                _jsonMap(diagnostic, 'autogen.diagnostic'),
              ))
          .toList(growable: false),
      scanStats: scan == null || scan['stats'] == null
          ? null
          : InstalledAutogenScanStats.fromJson(
              _jsonMap(scan['stats'], 'autogen.scan.stats'),
            ),
      rawJson: Map<String, Object?>.unmodifiable(json),
    );
  }

  final String operation;
  final String targetRepoId;
  final String? targetRepoPath;
  final AutogenSummary summary;
  final List<InstalledAutogenCandidate> candidates;
  final List<InstalledAutogenSkip> skipped;
  final List<PlatformDiagnosticSummary> diagnostics;
  final InstalledAutogenScanStats? scanStats;
  final Map<String, Object?> rawJson;
}

class AutogenSummary {
  const AutogenSummary({
    required this.candidateCount,
    required this.skippedCount,
    required this.writeCount,
    required this.deleteCount,
  });

  factory AutogenSummary.fromJson(Map<String, Object?> json) {
    return AutogenSummary(
      candidateCount:
          _jsonInt(json['candidate_count'], 'autogen.summary.candidate_count'),
      skippedCount:
          _jsonInt(json['skipped_count'], 'autogen.summary.skipped_count'),
      writeCount: _jsonInt(json['write_count'], 'autogen.summary.write_count'),
      deleteCount:
          _jsonInt(json['delete_count'], 'autogen.summary.delete_count'),
    );
  }

  final int candidateCount;
  final int skippedCount;
  final int writeCount;
  final int deleteCount;
}

class InstalledAutogenCandidate {
  const InstalledAutogenCandidate({
    required this.packageId,
    required this.kind,
    required this.displayName,
    required this.action,
    required this.outputRelativePath,
    required this.contentHash,
    required this.installedTarget,
  });

  factory InstalledAutogenCandidate.fromJson(Map<String, Object?> json) {
    return InstalledAutogenCandidate(
      packageId:
          _jsonString(json['package_id'], 'autogen.candidate.package_id'),
      kind: _jsonString(json['kind'], 'autogen.candidate.kind'),
      displayName:
          _jsonString(json['display_name'], 'autogen.candidate.display_name'),
      action: _jsonString(json['action'], 'autogen.candidate.action'),
      outputRelativePath: _jsonString(
        json['output_relative_path'],
        'autogen.candidate.output_relative_path',
      ),
      contentHash:
          _jsonString(json['content_hash'], 'autogen.candidate.content_hash'),
      installedTarget: _jsonMap(
        json['installed_target'],
        'autogen.candidate.installed_target',
      ),
    );
  }

  final String packageId;
  final String kind;
  final String displayName;
  final String action;
  final String outputRelativePath;
  final String contentHash;
  final Map<String, Object?> installedTarget;
}

class InstalledAutogenSkip {
  const InstalledAutogenSkip({
    required this.packageId,
    required this.reason,
    required this.coveringRepoId,
  });

  factory InstalledAutogenSkip.fromJson(Map<String, Object?> json) {
    return InstalledAutogenSkip(
      packageId: _jsonString(json['package_id'], 'autogen.skip.package_id'),
      reason: _jsonString(json['reason'], 'autogen.skip.reason'),
      coveringRepoId: _jsonOptionalString(
        json['covering_repo_id'],
        'autogen.skip.covering_repo_id',
      ),
    );
  }

  final String packageId;
  final String reason;
  final String? coveringRepoId;
}

class InstalledAutogenScanStats {
  const InstalledAutogenScanStats({
    required this.totalSeen,
    required this.returned,
    required this.filteredSystem,
    required this.filteredSelf,
  });

  factory InstalledAutogenScanStats.fromJson(Map<String, Object?> json) {
    return InstalledAutogenScanStats(
      totalSeen: _jsonInt(json['total_seen'], 'autogen.scan.total_seen'),
      returned: _jsonInt(json['returned'], 'autogen.scan.returned'),
      filteredSystem:
          _jsonInt(json['filtered_system'], 'autogen.scan.filtered_system'),
      filteredSelf:
          _jsonInt(json['filtered_self'], 'autogen.scan.filtered_self'),
    );
  }

  final int totalSeen;
  final int returned;
  final int filteredSystem;
  final int filteredSelf;
}

class PlatformDiagnosticSummary {
  const PlatformDiagnosticSummary({
    required this.code,
    required this.message,
    required this.detail,
  });

  factory PlatformDiagnosticSummary.fromJson(Map<String, Object?> json) {
    return PlatformDiagnosticSummary(
      code: _jsonString(json['code'], 'autogen.diagnostic.code'),
      message: _jsonString(json['message'], 'autogen.diagnostic.message'),
      detail: _jsonOptionalString(json['detail'], 'autogen.diagnostic.detail'),
    );
  }

  final String code;
  final String message;
  final String? detail;
}

class InstalledAutogenApplyResult {
  InstalledAutogenApplyResult({
    required this.targetRepoId,
    required this.targetRepoPath,
    required this.appliedCount,
    required this.applied,
    required this.preservedToLocal,
  });

  factory InstalledAutogenApplyResult.fromJson(Map<String, Object?> json) {
    return InstalledAutogenApplyResult(
      targetRepoId:
          _jsonString(json['target_repo_id'], 'autogen.apply.target_repo_id'),
      targetRepoPath: _jsonOptionalString(
        json['target_repo_path'],
        'autogen.apply.target_repo_path',
      ),
      appliedCount:
          _jsonInt(json['applied_count'], 'autogen.apply.applied_count'),
      applied: _jsonList(json['applied'], 'autogen.apply.applied')
          .map((applied) => InstalledAutogenAppliedPackage.fromJson(
                _jsonMap(applied, 'autogen.apply.applied_item'),
              ))
          .toList(growable: false),
      preservedToLocal: _jsonList(
        json['preserved_to_local'],
        'autogen.apply.preserved_to_local',
      )
          .map((preserved) => InstalledAutogenPreservedPackage.fromJson(
                _jsonMap(preserved, 'autogen.apply.preserved_item'),
              ))
          .toList(growable: false),
    );
  }

  final String targetRepoId;
  final String? targetRepoPath;
  final int appliedCount;
  final List<InstalledAutogenAppliedPackage> applied;
  final List<InstalledAutogenPreservedPackage> preservedToLocal;
}

class InstalledAutogenAppliedPackage {
  const InstalledAutogenAppliedPackage({
    required this.packageId,
    required this.outputRelativePath,
  });

  factory InstalledAutogenAppliedPackage.fromJson(Map<String, Object?> json) {
    return InstalledAutogenAppliedPackage(
      packageId: _jsonString(json['package_id'], 'autogen.apply.package_id'),
      outputRelativePath: _jsonString(
        json['output_relative_path'],
        'autogen.apply.output_relative_path',
      ),
    );
  }

  final String packageId;
  final String outputRelativePath;
}

class InstalledAutogenPreservedPackage {
  const InstalledAutogenPreservedPackage({
    required this.packageId,
    required this.repositoryId,
    required this.relativePath,
  });

  factory InstalledAutogenPreservedPackage.fromJson(Map<String, Object?> json) {
    return InstalledAutogenPreservedPackage(
      packageId:
          _jsonString(json['package_id'], 'autogen.preserved.package_id'),
      repositoryId:
          _jsonString(json['repository_id'], 'autogen.preserved.repository_id'),
      relativePath:
          _jsonString(json['relative_path'], 'autogen.preserved.relative_path'),
    );
  }

  final String packageId;
  final String repositoryId;
  final String relativePath;
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

Map<String, Object?> _jsonMap(Object? value, String name) {
  if (value is Map<String, Object?>) return value;
  if (value is Map) return value.cast<String, Object?>();
  throw FormatException('$name should be a JSON object');
}

Map<String, Object?>? _jsonMapOrNull(Object? value, String name) {
  if (value == null) return null;
  return _jsonMap(value, name);
}

List<Object?> _jsonList(Object? value, String name) {
  if (value is List<Object?>) return value;
  if (value is List) return value.cast<Object?>();
  throw FormatException('$name should be a JSON array');
}

String _jsonString(Object? value, String name) {
  if (value is String) return value;
  throw FormatException('$name should be a string');
}

String? _jsonOptionalString(Object? value, String name) {
  if (value == null || value is String) return value as String?;
  throw FormatException('$name should be a string or null');
}

int _jsonInt(Object? value, String name) {
  if (value is int) return value;
  throw FormatException('$name should be an integer');
}

bool _jsonBool(Object? value, String name) {
  if (value is bool) return value;
  throw FormatException('$name should be a boolean');
}

bool? _jsonOptionalBool(Object? value, String name) {
  if (value == null || value is bool) return value as bool?;
  throw FormatException('$name should be a boolean or null');
}
