import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/legacy_migration_platform.dart';
import 'package:upgradeall/main.dart';

void main() {
  testWidgets('fresh launch exposes home route and getter state',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    expect(find.byKey(AppKeys.homeRoute), findsOneWidget);
    expect(find.byKey(AppKeys.updateSummary), findsOneWidget);
    expect(find.byKey(AppKeys.getterStatus), findsOneWidget);
    expect(find.text('0 updates available'), findsOneWidget);
    expect(find.text('Fake getter ready'), findsOneWidget);
  });

  testWidgets('app list and detail routes use stable keys', (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openApps));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.appsList), findsOneWidget);
    expect(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')),
        findsOneWidget);
    expect(find.text('Network'), findsOneWidget);

    await tester.tap(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appDetailRoute), findsOneWidget);
    expect(find.text('android/org.fdroid.fdroid'), findsOneWidget);
    expect(find.text('Installed: 1.20.0'), findsOneWidget);
    expect(find.text('Latest: 1.20.0'), findsOneWidget);
    expect(find.text('Network access required'), findsOneWidget);
  });

  testWidgets('repository route lists priority ordered repository IDs',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openRepositories));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.repositoriesRoute), findsOneWidget);
    expect(find.byKey(AppKeys.repositoriesList), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('local')), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('official')), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('local_autogen')), findsOneWidget);
  });

  testWidgets('downloads route renders runtime task snapshots read-only',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsList), findsOneWidget);
    expect(find.byKey(AppKeys.downloadTaskRow('task-1')), findsOneWidget);
    expect(find.text('queued • queued'), findsOneWidget);
    expect(find.text('Cancel'), findsOneWidget);
  });

  testWidgets('downloads route exposes getter empty task state',
      (tester) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _NoTaskGetterAdapter()),
    );

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsEmpty), findsOneWidget);
  });

  testWidgets('migration route imports prepared legacy DB through getter',
      (tester) async {
    final getter = _MigrationGetterAdapter();
    await tester.pumpWidget(
      UpgradeAllApp(
        getter: getter,
        legacyMigrationPlatform: const _PreparedLegacyMigrationPlatform(
          '/tmp/app_metadata_database.db',
        ),
      ),
    );

    await tester.tap(find.byKey(AppKeys.openMigration));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.startLegacyMigration));
    await tester.pumpAndSettle();

    expect(getter.importedDatabasePath, '/tmp/app_metadata_database.db');
    expect(find.byKey(AppKeys.migrationStatus), findsOneWidget);
    expect(find.text('Legacy migration imported 1 records'), findsOneWidget);
    expect(find.byKey(AppKeys.migrationImported), findsOneWidget);
    expect(find.byKey(AppKeys.migrationReportsList), findsOneWidget);
    expect(find.text('migration.imported'), findsOneWidget);
  });

  testWidgets('migration route reports missing legacy DB from platform adapter',
      (tester) async {
    await tester.pumpWidget(
      const UpgradeAllApp(
        getter: _LegacyMigrationCapableGetterAdapter(),
        legacyMigrationPlatform: _MissingLegacyMigrationPlatform(),
      ),
    );

    await tester.tap(find.byKey(AppKeys.openMigration));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.startLegacyMigration));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.migrationStatus), findsOneWidget);
    expect(find.text('No legacy Room database found'), findsOneWidget);
    expect(find.byKey(AppKeys.migrationImported), findsNothing);
  });

  testWidgets('installed autogen route previews and applies getter DTOs',
      (tester) async {
    final getter = _AutogenRecordingGetterAdapter();
    await tester.pumpWidget(UpgradeAllApp(getter: getter));

    await tester.drag(find.byType(ListView).first, const Offset(0, -240));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.installedAutogenRoute), findsOneWidget);
    expect(find.byKey(AppKeys.installedAutogenReady), findsOneWidget);

    await tester.tap(find.byKey(AppKeys.previewInstalledAutogen));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.installedAutogenPreview), findsOneWidget);
    expect(find.byKey(AppKeys.installedAutogenScanStats), findsOneWidget);
    expect(
      find.byKey(AppKeys.autogenCandidateRow('android/com.example.autogen')),
      findsOneWidget,
    );
    expect(
      find.byKey(AppKeys.autogenSkipRow('android/org.fdroid.fdroid')),
      findsOneWidget,
    );

    await tester.tap(find.byKey(AppKeys.applyInstalledAutogen));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.installedAutogenApplied), findsOneWidget);
    expect(
      find.byKey(AppKeys.autogenAppliedRow('android/com.example.autogen')),
      findsOneWidget,
    );
    expect(getter.acceptedPackageIds, <String>['android/com.example.autogen']);
  });

  testWidgets('installed autogen route disables actions without bridge',
      (tester) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _NoInstalledAutogenGetterAdapter()),
    );

    await tester.drag(find.byType(ListView).first, const Offset(0, -240));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
    await tester.pumpAndSettle();

    final button = tester.widget<ElevatedButton>(
      find.byKey(AppKeys.previewInstalledAutogen),
    );
    expect(button.onPressed, isNull);
    expect(
      find.byKey(AppKeys.installedAutogenBridgeUnavailable),
      findsOneWidget,
    );
  });

  testWidgets('migration route disables import when getter bridge is absent',
      (tester) async {
    await tester.pumpWidget(
      const UpgradeAllApp(
        legacyMigrationPlatform: _PreparedLegacyMigrationPlatform(
          '/tmp/app_metadata_database.db',
        ),
      ),
    );

    await tester.tap(find.byKey(AppKeys.openMigration));
    await tester.pumpAndSettle();

    final button = tester.widget<ElevatedButton>(
      find.byKey(AppKeys.startLegacyMigration),
    );
    expect(button.onPressed, isNull);
    expect(find.byKey(AppKeys.migrationBridgeUnavailable), findsOneWidget);
  });

  testWidgets('placeholder routes expose stable empty-state keys',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openLogs));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.logsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.logsEmpty), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openSettings));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.settingsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.settingsShell), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openMigration));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.migrationRoute), findsOneWidget);
    expect(find.byKey(AppKeys.migrationReady), findsOneWidget);
  });
}

class _NoTaskGetterAdapter extends FakeGetterAdapter {
  const _NoTaskGetterAdapter();

  @override
  List<DownloadTaskSummary> listDownloadTasks() =>
      const <DownloadTaskSummary>[];

  @override
  TaskEventPage listTaskEvents({required int after, required int limit}) {
    return const TaskEventPage(
      events: <TaskEventSummary>[],
      nextCursor: 0,
      hasMore: false,
    );
  }

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async =>
      const <RuntimeTaskSnapshot>[];
}

class _LegacyMigrationCapableGetterAdapter extends FakeGetterAdapter {
  const _LegacyMigrationCapableGetterAdapter();

  @override
  bool get supportsLegacyRoomImport => true;
}

class _NoInstalledAutogenGetterAdapter extends FakeGetterAdapter {
  const _NoInstalledAutogenGetterAdapter();

  @override
  bool get supportsInstalledAutogen => false;
}

class _AutogenRecordingGetterAdapter extends FakeGetterAdapter {
  List<String>? acceptedPackageIds;

  @override
  Future<InstalledAutogenApplyResult> applyInstalledAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) {
    this.acceptedPackageIds = acceptedPackageIds;
    return super.applyInstalledAutogen(
      preview,
      acceptedPackageIds: acceptedPackageIds,
    );
  }
}

class _MigrationGetterAdapter extends FakeGetterAdapter {
  String? importedDatabasePath;
  @override
  bool get supportsLegacyRoomImport => true;
  var _reports = const <MigrationReportSummary>[];

  @override
  Future<LegacyMigrationImportResult> importLegacyRoomDatabase(
    String databasePath,
  ) async {
    importedDatabasePath = databasePath;
    _reports = const <MigrationReportSummary>[
      MigrationReportSummary(
        ok: true,
        code: 'migration.imported',
        message: 'Legacy Room database imported',
        importedRecords: 1,
        trackedRecords: 1,
      ),
    ];
    return const LegacyMigrationImportResult(
      alreadyImported: false,
      importedRecords: 1,
      trackedPackages: <TrackedPackageSummary>[
        TrackedPackageSummary(
          id: 'android/org.fdroid.fdroid',
          enabled: true,
          favorite: true,
          pinVersion: '1.20.0',
          repositoryId: null,
          packageResolution: 'missing_package_definition',
        ),
      ],
      warnings: <MigrationWarningSummary>[],
      sourceCounts: MigrationSourceCounts(
        appRows: 1,
        extraAppRows: 1,
        hubRows: 0,
        extraHubRows: 0,
      ),
    );
  }

  @override
  Future<List<MigrationReportSummary>> readMigrationReports() async => _reports;
}

class _PreparedLegacyMigrationPlatform implements LegacyMigrationPlatform {
  const _PreparedLegacyMigrationPlatform(this.databasePath);

  final String databasePath;

  @override
  Future<LegacyRoomImportCandidate> prepareLegacyRoomImport() async {
    return LegacyRoomImportCandidate(
      found: true,
      databasePath: databasePath,
      message: 'Legacy Room database prepared',
    );
  }
}

class _MissingLegacyMigrationPlatform implements LegacyMigrationPlatform {
  const _MissingLegacyMigrationPlatform();

  @override
  Future<LegacyRoomImportCandidate> prepareLegacyRoomImport() async {
    return const LegacyRoomImportCandidate(
      found: false,
      databasePath: null,
      message: 'No legacy Room database found',
    );
  }
}
