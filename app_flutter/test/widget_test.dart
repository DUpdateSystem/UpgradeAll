import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/legacy_migration_platform.dart';
import 'package:upgradeall/main.dart';

void main() {
  testWidgets('fresh launch exposes home route and getter state', (
    tester,
  ) async {
    await tester.pumpWidget(const UpgradeAllApp());
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.homeRoute), findsOneWidget);
    expect(find.byKey(AppKeys.updateSummary), findsOneWidget);
    expect(find.byKey(AppKeys.getterStatus), findsOneWidget);
    expect(find.text('0 updates available'), findsOneWidget);
    expect(find.text('Fake getter ready'), findsOneWidget);
  });

  testWidgets('getter facts render on home, apps, and app detail', (
    tester,
  ) async {
    const getter = _StartupFactsGetterAdapter();
    await tester.pumpWidget(const UpgradeAllApp(getter: getter));
    await tester.pumpAndSettle();

    expect(find.text('1 updates available'), findsOneWidget);

    await tester.tap(find.byKey(AppKeys.openApps));
    await tester.pumpAndSettle();
    expect(find.text('2.0 installed • 3.0 available'), findsOneWidget);
    expect(find.text('Update available'), findsOneWidget);

    await tester.tap(find.byKey(AppKeys.appRow('android/app/com.example')));
    await tester.pumpAndSettle();
    expect(find.text('Installed: 2.0'), findsOneWidget);
    expect(find.text('Latest: 3.0'), findsOneWidget);
    expect(find.text('Update: available'), findsOneWidget);
    expect(find.text('Cache miss'), findsOneWidget);
  });

  testWidgets('startup diagnostics render once on home', (tester) async {
    const getter = _StartupDiagnosticGetterAdapter();
    await tester.pumpWidget(const UpgradeAllApp(getter: getter));
    await tester.pumpAndSettle();

    expect(find.text('Startup diagnostics'), findsOneWidget);
    expect(find.text('One package could not be inspected'), findsOneWidget);
  });

  testWidgets('app list and detail routes use stable keys', (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openApps));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.appsList), findsOneWidget);
    expect(
      find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')),
      findsOneWidget,
    );
    expect(find.text('Network'), findsOneWidget);

    await tester.tap(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appDetailRoute), findsOneWidget);
    expect(find.text('android/org.fdroid.fdroid'), findsOneWidget);
    expect(find.text('Installed: 1.20.0'), findsOneWidget);
    expect(find.text('Latest: 1.20.0'), findsOneWidget);
    expect(find.text('Network access required'), findsOneWidget);
  });

  testWidgets('app detail submits getter-issued update action to runtime', (
    tester,
  ) async {
    final getter = _UpdateCheckRecordingGetterAdapter();
    await tester.pumpWidget(UpgradeAllApp(getter: getter));

    await tester.tap(find.byKey(AppKeys.openApps));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(AppKeys.checkPackageUpdate('android/org.fdroid.fdroid')),
    );
    await tester.pumpAndSettle();

    expect(getter.checkedPackageId, 'android/org.fdroid.fdroid');
    expect(getter.checkedInstalledVersion, '1.20.0');
    expect(getter.submittedActionId, 'action-from-getter');
    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(
      find.byKey(AppKeys.downloadTaskRow('task-from-action')),
      findsOneWidget,
    );
  });

  testWidgets('app detail reports update checks without runtime action', (
    tester,
  ) async {
    await tester.pumpWidget(
      UpgradeAllApp(getter: _NoUpdateActionGetterAdapter()),
    );

    await tester.tap(find.byKey(AppKeys.openApps));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(AppKeys.checkPackageUpdate('android/org.fdroid.fdroid')),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appDetailRoute), findsOneWidget);
    expect(find.byKey(AppKeys.updateCheckStatus), findsOneWidget);
    expect(find.text('No update task available: up_to_date'), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsRoute), findsNothing);
  });

  testWidgets('repository route lists priority ordered repository IDs', (
    tester,
  ) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openRepositories));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.repositoriesRoute), findsOneWidget);
    expect(find.byKey(AppKeys.repositoriesList), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('local')), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('official')), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('autogen')), findsOneWidget);
  });

  testWidgets('downloads route renders runtime task snapshots read-only', (
    tester,
  ) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsList), findsOneWidget);
    expect(find.byKey(AppKeys.downloadTaskRow('task-1')), findsOneWidget);
    expect(find.text('queued • queued'), findsOneWidget);
    expect(find.text('Cancel'), findsOneWidget);
  });

  testWidgets('downloads route refreshes after runtime notification', (
    tester,
  ) async {
    final getter = _NotificationRefreshingGetterAdapter();
    await tester.pumpWidget(UpgradeAllApp(getter: getter));

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();
    expect(find.text('queued • queued'), findsOneWidget);

    getter.emitRunningTaskNotification();
    await tester.pumpAndSettle();

    expect(find.text('running • download'), findsOneWidget);
    expect(getter.listCallCount, 2);
  });

  testWidgets('downloads route renders getter-owned downloaded file metadata', (
    tester,
  ) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _DownloadedTaskGetterAdapter()),
    );

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(
      find.byKey(AppKeys.downloadTaskRow('task-downloaded')),
      findsOneWidget,
    );
    expect(
      find.text('completed • completed • app.apk (12 bytes)'),
      findsOneWidget,
    );
  });

  testWidgets('downloads route exposes getter empty task state', (
    tester,
  ) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _NoTaskGetterAdapter()),
    );

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsEmpty), findsOneWidget);
  });

  testWidgets('migration route imports prepared legacy DB through getter', (
    tester,
  ) async {
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

  testWidgets(
    'migration route reports missing legacy DB from platform adapter',
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
    },
  );

  testWidgets('installed autogen route previews and applies getter DTOs', (
    tester,
  ) async {
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
      find.byKey(
        AppKeys.autogenCandidateRow('android/app/com.example.autogen'),
      ),
      findsOneWidget,
    );
    expect(
      find.byKey(AppKeys.autogenSkipRow('android/org.fdroid.fdroid')),
      findsOneWidget,
    );

    await tester.tap(find.byKey(AppKeys.applyInstalledAutogen));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.installedAutogenConfirmDialog), findsOneWidget);
    expect(
      find.byKey(
        AppKeys.autogenConfirmCandidateRow('android/app/com.example.autogen'),
      ),
      findsOneWidget,
    );

    await tester.tap(find.byKey(AppKeys.confirmInstalledAutogenApply));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.installedAutogenApplied), findsOneWidget);
    expect(
      find.byKey(AppKeys.autogenAppliedRow('android/app/com.example.autogen')),
      findsOneWidget,
    );
    expect(getter.acceptedPackageIds, <String>[
      'android/app/com.example.autogen',
    ]);
  });

  testWidgets(
    'installed autogen route previews and applies GitHub getter DTOs',
    (tester) async {
      final getter = _AutogenRecordingGetterAdapter();
      await tester.pumpWidget(UpgradeAllApp(getter: getter));

      await tester.drag(find.byType(ListView).first, const Offset(0, -240));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(AppKeys.githubAutogenOwnerField),
        'DUpdateSystem',
      );
      await tester.enterText(
        find.byKey(AppKeys.githubAutogenRepoField),
        'UpgradeAll',
      );
      await tester.enterText(
        find.byKey(AppKeys.githubAutogenAndroidPackageField),
        'net.xzos.upgradeall',
      );
      await tester.enterText(
        find.byKey(AppKeys.githubAutogenDisplayNameField),
        'UpgradeAll',
      );
      await tester.tap(find.byKey(AppKeys.previewGithubAutogen));
      await tester.pumpAndSettle();

      expect(getter.githubInput, isNotNull);
      expect(getter.githubInput!.owner, 'DUpdateSystem');
      expect(getter.githubInput!.repo, 'UpgradeAll');
      expect(getter.githubInput!.androidPackage, 'net.xzos.upgradeall');
      expect(getter.githubInput!.displayName, 'UpgradeAll');
      expect(find.byKey(AppKeys.installedAutogenPreview), findsOneWidget);
      final githubCandidate = find.byKey(
        AppKeys.autogenCandidateRow(
          'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
        ),
      );
      await tester.scrollUntilVisible(
        githubCandidate,
        200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.pumpAndSettle();
      expect(githubCandidate, findsOneWidget);

      await tester.scrollUntilVisible(
        find.byKey(AppKeys.applyInstalledAutogen),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(AppKeys.applyInstalledAutogen));
      await tester.pumpAndSettle();
      expect(find.byKey(AppKeys.installedAutogenConfirmDialog), findsOneWidget);

      await tester.tap(find.byKey(AppKeys.confirmInstalledAutogenApply));
      await tester.pumpAndSettle();

      expect(getter.usedGithubApply, isTrue);
      expect(getter.acceptedPackageIds, <String>[
        'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
      ]);
      expect(
        find.byKey(
          AppKeys.autogenAppliedRow(
            'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
          ),
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'installed autogen route previews and applies installed F-Droid getter DTOs',
    (tester) async {
      final getter = _AutogenRecordingGetterAdapter();
      await tester.pumpWidget(UpgradeAllApp(getter: getter));

      await tester.drag(find.byType(ListView).first, const Offset(0, -240));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(AppKeys.previewInstalledFdroidAutogen));
      await tester.pumpAndSettle();

      expect(find.byKey(AppKeys.installedAutogenPreview), findsOneWidget);
      expect(
        find.byKey(
          AppKeys.autogenCandidateRow('android/f-droid/app/org.fdroid.fdroid'),
        ),
        findsOneWidget,
      );

      await tester.tap(find.byKey(AppKeys.applyInstalledAutogen));
      await tester.pumpAndSettle();

      expect(find.byKey(AppKeys.installedAutogenConfirmDialog), findsOneWidget);
      expect(find.text('Target repository: autogen'), findsOneWidget);
      expect(
        find.byKey(
          AppKeys.autogenConfirmCandidateRow(
            'android/f-droid/app/org.fdroid.fdroid',
          ),
        ),
        findsOneWidget,
      );
      expect(getter.usedInstalledFdroidApply, isFalse);

      await tester.tap(find.byKey(AppKeys.confirmInstalledAutogenApply));
      await tester.pumpAndSettle();

      expect(getter.usedInstalledFdroidApply, isTrue);
      expect(getter.acceptedPackageIds, <String>[
        'android/f-droid/app/org.fdroid.fdroid',
      ]);
      expect(
        find.byKey(
          AppKeys.autogenAppliedRow('android/f-droid/app/org.fdroid.fdroid'),
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets('installed autogen route refreshes F-Droid catalog cache', (
    tester,
  ) async {
    final getter = _AutogenRecordingGetterAdapter();
    await tester.pumpWidget(UpgradeAllApp(getter: getter));

    await tester.drag(find.byType(ListView).first, const Offset(0, -240));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(AppKeys.refreshDefaultFdroidCatalogCache));
    await tester.pumpAndSettle();

    expect(getter.refreshedFdroidCatalog, isTrue);
    expect(find.byKey(AppKeys.fdroidCatalogRefreshStatus), findsOneWidget);
    expect(
      find.text('F-Droid catalog refreshed: 3 apps, 4 releases'),
      findsOneWidget,
    );
    expect(
      find.byKey(AppKeys.fdroidCatalogRefreshDiagnosticRow(0)),
      findsOneWidget,
    );
  });

  testWidgets('installed autogen route renders F-Droid refresh error detail', (
    tester,
  ) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _FdroidRefreshErrorGetterAdapter()),
    );

    await tester.drag(find.byType(ListView).first, const Offset(0, -240));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(AppKeys.refreshDefaultFdroidCatalogCache));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.installedAutogenError), findsOneWidget);
    expect(
      find.textContaining('provider.fdroid_catalog.error'),
      findsOneWidget,
    );
    expect(
      find.textContaining('bootstrap source parse failed'),
      findsOneWidget,
    );
  });

  testWidgets('installed autogen confirmation cancel does not apply', (
    tester,
  ) async {
    final getter = _AutogenRecordingGetterAdapter();
    await tester.pumpWidget(UpgradeAllApp(getter: getter));

    await tester.drag(find.byType(ListView).first, const Offset(0, -240));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(AppKeys.previewInstalledFdroidAutogen));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.applyInstalledAutogen));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.cancelInstalledAutogenApply));
    await tester.pumpAndSettle();

    expect(getter.usedInstalledFdroidApply, isFalse);
    expect(getter.acceptedPackageIds, isNull);
    expect(find.byKey(AppKeys.installedAutogenApplied), findsNothing);
  });

  testWidgets('installed autogen route renders bridge error detail', (
    tester,
  ) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _AutogenErrorGetterAdapter()),
    );

    await tester.drag(find.byType(ListView).first, const Offset(0, -240));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openInstalledAutogen));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(AppKeys.previewInstalledFdroidAutogen));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.installedAutogenError), findsOneWidget);
    expect(find.textContaining('autogen.error'), findsOneWidget);
    expect(find.textContaining('refresh provider cache'), findsOneWidget);
  });

  testWidgets('installed autogen route disables actions without bridge', (
    tester,
  ) async {
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

  testWidgets('migration route disables import when getter bridge is absent', (
    tester,
  ) async {
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

  testWidgets('placeholder routes expose stable empty-state keys', (
    tester,
  ) async {
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

class _UpdateCheckRecordingGetterAdapter extends FakeGetterAdapter {
  String? checkedPackageId;
  String? checkedInstalledVersion;
  String? submittedActionId;
  final _tasks = <RuntimeTaskSnapshot>[];

  @override
  Future<RuntimeUpdateCheckResult> checkPackageForUpdate(
    String packageId, {
    String? repositoryId,
    String? installedVersion,
    String? pinVersion,
  }) async {
    checkedPackageId = packageId;
    checkedInstalledVersion = installedVersion;
    return RuntimeUpdateCheckResult.fromJson(<String, Object?>{
      'package': <String, Object?>{
        'id': packageId,
        'name': 'F-Droid',
        'repository': repositoryId ?? 'official',
      },
      'update': <String, Object?>{
        'package_id': packageId,
        'status': 'update_available',
        'installed_version': installedVersion,
        'effective_local_version': installedVersion,
        'selected': <String, Object?>{
          'candidate': <String, Object?>{'version': '1.21.0'},
        },
        'actions': <Object?>[
          <String, Object?>{'type': 'download'},
        ],
      },
      'action': <String, Object?>{
        'action_id': 'action-from-getter',
        'package_id': packageId,
      },
    });
  }

  @override
  Future<RuntimeTaskSnapshot> submitRuntimeAction(String actionId) async {
    submittedActionId = actionId;
    final task = RuntimeTaskSnapshot.fromJson(const <String, Object?>{
      'task_id': 'task-from-action',
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
      'downloaded_file': null,
      'updated_at': 42,
    });
    _tasks
      ..clear()
      ..add(task);
    return task;
  }

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async => List<RuntimeTaskSnapshot>.unmodifiable(_tasks);
}

class _NoUpdateActionGetterAdapter extends FakeGetterAdapter {
  const _NoUpdateActionGetterAdapter();

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
      },
      'update': <String, Object?>{
        'package_id': packageId,
        'status': 'up_to_date',
        'installed_version': installedVersion,
        'effective_local_version': installedVersion,
        'selected': null,
        'actions': <Object?>[],
      },
      'action': null,
    });
  }
}

class _NotificationRefreshingGetterAdapter extends FakeGetterAdapter {
  final _notifications =
      StreamController<RuntimeNotificationEnvelope>.broadcast();
  var _running = false;
  var listCallCount = 0;

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async {
    listCallCount += 1;
    return <RuntimeTaskSnapshot>[_task(_running ? 'running' : 'queued')];
  }

  @override
  Stream<RuntimeNotificationEnvelope> runtimeNotificationEnvelopes() {
    return _notifications.stream;
  }

  void emitRunningTaskNotification() {
    _running = true;
    _notifications.add(
      RuntimeNotificationEnvelope(kind: 'task_changed', task: _task('running')),
    );
  }

  RuntimeTaskSnapshot _task(String status) {
    return RuntimeTaskSnapshot.fromJson(<String, Object?>{
      'task_id': 'task-refresh',
      'package_id': 'android/org.fdroid.fdroid',
      'status': status,
      'phase': <String, Object?>{
        'category': status == 'running' ? 'download' : 'queued',
      },
      'progress': null,
      'capabilities': <String, Object?>{
        'cancel': true,
        'pause': false,
        'resume': false,
        'retry': false,
      },
      'current_diagnostic': null,
      'downloaded_file': null,
      'updated_at': status == 'running' ? 2 : 1,
    });
  }
}

class _NoTaskGetterAdapter extends FakeGetterAdapter {
  const _NoTaskGetterAdapter();

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async => const <RuntimeTaskSnapshot>[];
}

class _DownloadedTaskGetterAdapter extends FakeGetterAdapter {
  const _DownloadedTaskGetterAdapter();

  @override
  Future<List<RuntimeTaskSnapshot>> listRuntimeTasks({
    bool active = false,
    String? packageId,
  }) async {
    return <RuntimeTaskSnapshot>[
      RuntimeTaskSnapshot.fromJson(const <String, Object?>{
        'task_id': 'task-downloaded',
        'package_id': 'android/org.fdroid.fdroid',
        'status': 'completed',
        'phase': <String, Object?>{'category': 'completed'},
        'progress': null,
        'capabilities': <String, Object?>{
          'cancel': false,
          'pause': false,
          'resume': false,
          'retry': false,
        },
        'current_diagnostic': null,
        'downloaded_file': <String, Object?>{
          'file_name': 'app.apk',
          'local_path': '/getter/downloads/task-downloaded/app.apk',
          'size_bytes': 12,
          'sha256': 'sha256-test',
        },
        'updated_at': 3,
      }),
    ];
  }
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

class _AutogenErrorGetterAdapter extends FakeGetterAdapter {
  const _AutogenErrorGetterAdapter();

  @override
  Future<InstalledAutogenPreview> previewInstalledFdroidAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    throw const GetterBridgeException(
      GetterError(
        code: 'autogen.error',
        message: 'Getter autogen operation failed',
        detail:
            'F-Droid catalog cache is empty; refresh provider cache before installed F-Droid autogen preview',
      ),
    );
  }
}

class _FdroidRefreshErrorGetterAdapter extends FakeGetterAdapter {
  const _FdroidRefreshErrorGetterAdapter();

  @override
  Future<FdroidCatalogCacheRefreshResult>
  refreshDefaultFdroidCatalogCache() async {
    throw const GetterBridgeException(
      GetterError(
        code: 'provider.fdroid_catalog.error',
        message: 'F-Droid catalog cache refresh failed',
        detail: 'bootstrap source parse failed',
      ),
    );
  }
}

class _AutogenRecordingGetterAdapter extends FakeGetterAdapter {
  List<String>? acceptedPackageIds;
  GithubAutogenPreviewInput? githubInput;

  var usedInstalledFdroidApply = false;
  var usedGithubApply = false;
  var refreshedFdroidCatalog = false;

  @override
  Future<InstalledAutogenPreview> previewInstalledFdroidAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    return InstalledAutogenPreview.fromJson(const <String, Object?>{
      'operation': 'fdroid.autogen.preview',
      'provider': 'fdroid',
      'endpoint_id': 'official',
      'endpoint_url': 'https://f-droid.org/repo',
      'target_repo_id': 'autogen',
      'target_repo_path': '/fake/getter/repo/autogen',
      'scan': <String, Object?>{
        'stats': <String, Object?>{
          'total_seen': 2,
          'returned': 1,
          'filtered_system': 1,
          'filtered_self': 0,
        },
        'diagnostics': <Object?>[],
      },
      'summary': <String, Object?>{
        'candidate_count': 1,
        'skipped_count': 0,
        'write_count': 1,
        'delete_count': 0,
      },
      'candidates': <Object?>[
        <String, Object?>{
          'package_id': 'android/f-droid/app/org.fdroid.fdroid',
          'kind': 'android',
          'display_name': 'F-Droid',
          'installed_target': <String, Object?>{
            'kind': 'android_package',
            'package_name': 'org.fdroid.fdroid',
          },
          'action': 'create',
          'output_relative_path': 'android/f-droid/app/org.fdroid.fdroid',
          'content_hash': 'sha512:fake-fdroid',
          'content': '-- fake generated F-Droid content',
        },
      ],
      'skipped': <Object?>[],
      'diagnostics': <Object?>[],
    });
  }

  @override
  Future<InstalledAutogenPreview> previewGithubAutogen(
    GithubAutogenPreviewInput input,
  ) async {
    githubInput = input;
    return InstalledAutogenPreview.fromJson(const <String, Object?>{
      'operation': 'github.autogen.preview',
      'provider': 'github',
      'api_base_url': 'https://api.github.com',
      'owner': 'DUpdateSystem',
      'repo': 'UpgradeAll',
      'cache_key': 'github:github-releases-v1:fake:DUpdateSystem/UpgradeAll',
      'source': 'cache',
      'target_repo_id': 'autogen',
      'target_repo_path': '/fake/getter/repo/autogen',
      'summary': <String, Object?>{
        'candidate_count': 1,
        'skipped_count': 0,
        'write_count': 1,
        'delete_count': 0,
      },
      'candidates': <Object?>[
        <String, Object?>{
          'package_id':
              'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
          'kind': 'android',
          'display_name': 'UpgradeAll',
          'installed_target': <String, Object?>{
            'kind': 'android_package',
            'package_name': 'net.xzos.upgradeall',
          },
          'action': 'create',
          'output_relative_path':
              'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
          'content_hash': 'sha512:fake-github',
          'content': '-- fake generated GitHub content',
        },
      ],
      'skipped': <Object?>[],
      'diagnostics': <Object?>[],
    });
  }

  @override
  Future<FdroidCatalogCacheRefreshResult>
  refreshDefaultFdroidCatalogCache() async {
    refreshedFdroidCatalog = true;
    return FdroidCatalogCacheRefreshResult.fromJson(const <String, Object?>{
      'operation': 'fdroid.catalog.refresh',
      'provider': 'fdroid',
      'endpoint_id': 'official',
      'endpoint_url': 'https://f-droid.org/repo',
      'cache_key': 'fdroid:fdroid-index-v1:official:fake',
      'source': 'refreshed',
      'app_count': 3,
      'release_count': 4,
      'source_response_sha512': <Object?>['sha512:fake'],
      'provenance_schema_version': 'provider-response-provenance-v1',
      'diagnostics': <Object?>[
        <String, Object?>{
          'code': 'provider.note',
          'message': 'cached bundled catalog',
          'cache_key': 'fdroid:fdroid-index-v1:official:fake',
          'provider': 'fdroid',
          'stale_fetched_at_unix': null,
        },
      ],
    });
  }

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

  @override
  Future<InstalledAutogenApplyResult> applyGithubAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    usedGithubApply = true;
    this.acceptedPackageIds = acceptedPackageIds;
    return InstalledAutogenApplyResult.fromJson(const <String, Object?>{
      'target_repo_id': 'autogen',
      'target_repo_path': '/fake/getter/repo/autogen',
      'applied_count': 1,
      'applied': <Object?>[
        <String, Object?>{
          'package_id':
              'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
          'output_relative_path':
              'android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall',
        },
      ],
    });
  }

  @override
  Future<InstalledAutogenApplyResult> applyInstalledFdroidAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    usedInstalledFdroidApply = true;
    this.acceptedPackageIds = acceptedPackageIds;
    return InstalledAutogenApplyResult.fromJson(const <String, Object?>{
      'target_repo_id': 'autogen',
      'target_repo_path': '/fake/getter/repo/autogen',
      'applied_count': 1,
      'applied': <Object?>[
        <String, Object?>{
          'package_id': 'android/f-droid/app/org.fdroid.fdroid',
          'output_relative_path': 'android/f-droid/app/org.fdroid.fdroid',
        },
      ],
    });
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

class _StartupDiagnosticGetterAdapter extends FakeGetterAdapter {
  const _StartupDiagnosticGetterAdapter();

  @override
  Future<GetterSnapshot> loadSnapshot() async => const GetterSnapshot(
    status: 'Getter initialized',
    updateCount: 0,
    apps: <AppSummary>[],
    repositories: <RepositorySummary>[],
    diagnostics: <GetterDiagnostic>[
      GetterDiagnostic(
        code: 'platform.partial_inventory',
        message: 'One package could not be inspected',
      ),
    ],
  );
}

class _StartupFactsGetterAdapter extends FakeGetterAdapter {
  const _StartupFactsGetterAdapter();

  @override
  Future<GetterSnapshot> loadSnapshot() async => const GetterSnapshot(
    status: 'Getter already initialized',
    updateCount: 1,
    repositories: <RepositorySummary>[
      RepositorySummary(id: 'official', priority: 0),
    ],
    apps: <AppSummary>[
      AppSummary(
        id: 'android/app/com.example',
        name: 'Example',
        installedVersion: '2.0',
        latestVersion: '3.0',
        updateStatus: 'available',
        hasFreeNetworkWarning: false,
        diagnostics: <GetterDiagnostic>[
          GetterDiagnostic(code: 'provider.cache_miss', message: 'Cache miss'),
        ],
      ),
    ],
  );
}
