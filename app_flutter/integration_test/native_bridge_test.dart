import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:upgradeall/android_package_installer.dart';
import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/legacy_migration_platform.dart';
import 'package:upgradeall/native_getter_adapter.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('native bridge imports a copied legacy Room database', (
    tester,
  ) async {
    await _resetAppData();
    await _installLegacyRoomFixture();

    final candidate = await const MethodChannelLegacyMigrationPlatform()
        .prepareLegacyRoomImport();

    expect(candidate.found, isTrue);
    expect(candidate.databasePath, isNotNull);
    expect(candidate.databasePath, contains('/getter-imports/legacy-room/'));
    expect(File(candidate.databasePath!).existsSync(), isTrue);

    const adapter = MethodChannelGetterAdapter();
    final result = await adapter.importLegacyRoomDatabase(
      candidate.databasePath!,
    );

    expect(result.alreadyImported, isFalse);
    expect(result.importedRecords, 1);
    expect(result.trackedPackages, hasLength(1));
    expect(result.trackedPackages.single.id, 'android/org.fdroid.fdroid');
    expect(result.trackedPackages.single.favorite, isTrue);
    expect(result.trackedPackages.single.pinVersion, '1.20.0');
    expect(result.sourceCounts?.appRows, 1);
    expect(result.sourceCounts?.extraAppRows, 1);

    final reports = await adapter.readMigrationReports();
    expect(
      reports.map((report) => report.code),
      contains('migration.imported'),
    );
  });

  testWidgets('native bridge previews and applies installed autogen for self', (
    tester,
  ) async {
    await _resetAppData();

    const adapter = MethodChannelGetterAdapter();
    final preview = await adapter.previewInstalledAutogen(
      options: const InstalledAutogenScanOptions(includeSelf: true),
    );

    expect(preview.scanStats, isNotNull);
    expect(preview.scanStats!.totalSeen, greaterThan(0));
    expect(
      preview.candidates.map((candidate) => candidate.packageId),
      contains(_selfPackageId),
    );

    final result = await adapter.applyInstalledAutogen(
      preview,
      acceptedPackageIds: const <String>[_selfPackageId],
    );

    expect(result.appliedCount, 1);
    expect(
      result.applied.map((package) => package.packageId),
      contains(_selfPackageId),
    );
  });

  testWidgets(
    'typed PackageInstaller installs Getter handoff on Android',
    (tester) async {
      const getter = MethodChannelGetterAdapter();
      const installer = MethodChannelAndroidPackageInstaller();
      final initialSnapshot = await getter.loadSnapshot();
      final initialApp = initialSnapshot.apps.singleWhere(
        (candidate) => candidate.id == _installFixturePackageId,
      );
      final update = await getter.checkPackageForUpdate(
        _installFixturePackageId,
        repositoryId: initialApp.repositoryId,
        installedVersion: initialApp.installedVersion,
      );
      final action = update.action;
      expect(action, isNotNull, reason: 'Getter must issue the fixture action');
      final task = await getter.submitRuntimeAction(action!.actionId);
      expect(task.isInstallReady, isTrue);
      final handoff = await getter.prepareInstallTask(task.taskId);
      expect(handoff.taskId, task.taskId);
      const channel = MethodChannel(_androidPackageInstallerChannel);

      var result = await installer.install(handoff);
      if (result.outcome ==
          AndroidPackageInstallOutcome.authorizationRequired) {
        final deadline = DateTime.now().add(const Duration(minutes: 2));
        var authorized = false;
        while (!authorized && DateTime.now().isBefore(deadline)) {
          await Future<void>.delayed(const Duration(milliseconds: 500));
          final status = Map<String, Object?>.from(
            await channel.invokeMapMethod<String, Object?>(
                  'authorizationStatus',
                ) ??
                const <String, Object?>{},
          );
          authorized = status['authorized'] == true;
        }
        expect(authorized, isTrue, reason: 'unknown-app-source authorization');
        await Future<void>.delayed(const Duration(seconds: 1));
        result = await installer.install(handoff);
      }

      const expectedStatus = String.fromEnvironment(
        'INSTALL_EXPECTED_STATUS',
        defaultValue: 'succeeded',
      );
      final expectedOutcome = switch (expectedStatus) {
        'succeeded' => AndroidPackageInstallOutcome.succeeded,
        'aborted' => AndroidPackageInstallOutcome.aborted,
        _ => throw ArgumentError.value(
          expectedStatus,
          'INSTALL_EXPECTED_STATUS',
        ),
      };
      expect(result.outcome, expectedOutcome);

      final terminalTask = await getter.sendRuntimeUserResult(
        task.taskId,
        expectedOutcome == AndroidPackageInstallOutcome.succeeded
            ? RuntimeUserResult.accepted
            : RuntimeUserResult.rejected,
        reason: expectedOutcome == AndroidPackageInstallOutcome.succeeded
            ? null
            : 'package_installer.aborted',
      );
      if (expectedOutcome == AndroidPackageInstallOutcome.succeeded) {
        expect(terminalTask.status, 'completed');
      } else {
        expect(terminalTask.status, 'failed');
        final retried = await getter.retryRuntimeTask(task.taskId);
        expect(retried.taskId, task.taskId);
        expect(retried.isInstallReady, isTrue);
      }

      final snapshot = await getter.loadSnapshot();
      final app = snapshot.apps.singleWhere(
        (candidate) => candidate.id == _installFixturePackageId,
      );
      if (expectedOutcome == AndroidPackageInstallOutcome.succeeded) {
        expect(app.installedVersion, handoff.packageVersion);
      } else {
        expect(app.installedVersion, isNull);
      }
    },
    skip: !_runAndroidInstallAcceptance,
  );
}

const _runAndroidInstallAcceptance = bool.fromEnvironment(
  'RUN_ANDROID_INSTALL_ACCEPTANCE',
);
const _debugPackageName = 'net.xzos.upgradeall.debug';
const _selfPackageId = 'android/app/$_debugPackageName';
const _legacyDbName = 'app_metadata_database.db';
const _legacyFixtureDir = 'integration_test/fixtures/legacy_room_v17_wal';
const _installFixturePackageId = 'android/acceptance/net.xzos.upgradeall';
const _androidPackageInstallerChannel = 'net.xzos.upgradeall/package_installer';

Directory get _packageDataDir => Directory('/data/user/0/$_debugPackageName');
Directory get _databasesDir => Directory('${_packageDataDir.path}/databases');
Directory get _filesDir => Directory('${_packageDataDir.path}/files');
File get _legacyDatabase => File('${_databasesDir.path}/$_legacyDbName');

Future<void> _resetAppData() async {
  for (final path in <String>[
    '${_filesDir.path}/getter',
    '${_filesDir.path}/getter-imports',
  ]) {
    final dir = Directory(path);
    if (await dir.exists()) {
      await dir.delete(recursive: true);
    }
  }
  for (final file in <File>[
    _legacyDatabase,
    File('${_legacyDatabase.path}-wal'),
    File('${_legacyDatabase.path}-shm'),
  ]) {
    if (await file.exists()) {
      await file.delete();
    }
  }
}

Future<void> _installLegacyRoomFixture() async {
  await _databasesDir.create(recursive: true);
  for (final suffix in <String>['', '-wal', '-shm']) {
    final asset = await rootBundle.load(
      '$_legacyFixtureDir/$_legacyDbName$suffix',
    );
    await File('${_legacyDatabase.path}$suffix').writeAsBytes(
      asset.buffer.asUint8List(asset.offsetInBytes, asset.lengthInBytes),
      flush: true,
    );
  }
}
