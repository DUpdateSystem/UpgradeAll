import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/cli_getter_adapter.dart';

void main() {
  test('CliGetterAdapter reads real getter repository and tracked state', () {
    final getterCli = Platform.environment['GETTER_CLI_BIN'];
    if (getterCli == null || getterCli.isEmpty) {
      fail('GETTER_CLI_BIN must point to the built getter-cli binary');
    }

    final temp = Directory.systemTemp.createTempSync('upgradeall-getter-cli-');
    addTearDown(() => temp.deleteSync(recursive: true));

    final dataDir = Directory('${temp.path}/data')..createSync();
    final repoDir = _createFixtureRepository(temp, 'official');
    final bundle = _createLegacyBundle(temp);
    final taskRequest = _createDownloadTaskRequest(temp);
    final adapter =
        CliGetterAdapter(executable: getterCli, dataDir: dataDir.path);

    adapter.initialize();
    _runGetter(getterCli, dataDir.path, <String>[
      'repo',
      'add',
      'official',
      repoDir.path,
      '--priority',
      '0',
    ]);
    _runGetter(getterCli, dataDir.path, <String>[
      'legacy',
      'import-room-bundle',
      bundle.path,
    ]);
    _runGetter(getterCli, dataDir.path, <String>[
      'task',
      'submit',
      '--request',
      taskRequest.path,
    ]);
    _runGetter(getterCli, dataDir.path, <String>['task', 'run', 'task-1']);

    final repositories = adapter.listRepositories();
    expect(repositories.map((repo) => repo.id), contains('official'));
    expect(
        repositories.singleWhere((repo) => repo.id == 'official').priority, 0);

    final trackedPackages = adapter.listTrackedPackages();
    final tracked = trackedPackages.singleWhere(
      (package) => package.id == 'android/org.fdroid.fdroid',
    );
    expect(tracked.favorite, isTrue);
    expect(tracked.ignoredVersion, '1.20.0');
    expect(tracked.packageResolution, 'official_repository_package');

    final evaluated = adapter.evaluatePackage(
      'android/org.fdroid.fdroid',
      repositoryId: 'official',
    );
    expect(evaluated.name, 'F-Droid');
    expect(evaluated.repositoryId, 'official');
    expect(evaluated.hasFreeNetworkWarning, isTrue);

    final reports = adapter.readMigrationReports();
    expect(
        reports.singleWhere((report) => report.code == 'migration.imported').ok,
        isTrue);

    final tasks = adapter.listDownloadTasks();
    final task = tasks.singleWhere((task) => task.id == 'task-1');
    expect(task.packageId, 'android/org.fdroid.fdroid');
    expect(task.status, 'succeeded');
    expect(task.downloadFileName, 'app.apk');
    expect(task.installHandoffId, 'handoff-1');

    final eventPage = adapter.listTaskEvents(after: 0, limit: 10);
    expect(eventPage.hasMore, isFalse);
    expect(eventPage.nextCursor, greaterThanOrEqualTo(4));
    expect(
      eventPage.events.map((event) => event.kind),
      containsAll(<String>[
        'task_created',
        'task_started',
        'task_succeeded',
        'install_handoff_requested',
      ]),
    );

    final snapshot = adapter.loadSnapshot();
    expect(snapshot.status, 'Getter CLI ready');
    expect(snapshot.repositories.map((repo) => repo.id), contains('official'));
    final app = snapshot.apps.singleWhere(
      (app) => app.id == 'android/org.fdroid.fdroid',
    );
    expect(app.name, 'F-Droid');
    expect(app.installedVersion, 'unknown');
    expect(app.hasFreeNetworkWarning, isTrue);
  });
}

Directory _createFixtureRepository(Directory temp, String repoId) {
  final repoDir = Directory('${temp.path}/repo-$repoId')..createSync();
  Directory('${repoDir.path}/packages/android').createSync(recursive: true);
  Directory('${repoDir.path}/lib').createSync();
  Directory('${repoDir.path}/templates').createSync();
  File('${repoDir.path}/repo.toml').writeAsStringSync('''
id = "$repoId"
name = "Fixture $repoId"
priority = 0
api_version = "getter.repo.v1"
''');
  File('${repoDir.path}/packages/android/org.fdroid.fdroid.lua')
      .writeAsStringSync('''
return package_def {
  id = "android/org.fdroid.fdroid",
  name = "F-Droid",
  installed = {
    { kind = "android_package", package_name = "org.fdroid.fdroid" },
  },
  permissions = { free_network = true },
}
''');
  return repoDir;
}

File _createLegacyBundle(Directory temp) {
  return File('${temp.path}/legacy-bundle.json')..writeAsStringSync('''
{
  "format": "upgradeall-legacy-room-bundle",
  "version": 17,
  "apps": [
    {
      "kind": "android",
      "installed_id": "org.fdroid.fdroid",
      "official_package_available": true,
      "ignored_version": "1.20.0",
      "favorite": true
    }
  ]
}
''');
}

File _createDownloadTaskRequest(Directory temp) {
  return File('${temp.path}/download-request.json')..writeAsStringSync('''
{
  "format": "getter-download-request",
  "version": 1,
  "package_id": "android/org.fdroid.fdroid",
  "executor": "fake",
  "actions": [
    {
      "type": "download",
      "url": "https://example.invalid/app.apk",
      "file_name": "app.apk"
    },
    {
      "type": "install",
      "installer": "android_package",
      "file": "app.apk"
    }
  ]
}
''');
}

void _runGetter(String getterCli, String dataDir, List<String> args) {
  final result = Process.runSync(
    getterCli,
    <String>['--data-dir', dataDir, ...args],
  );
  if (result.exitCode != 0) {
    fail('getter ${args.join(' ')} failed with ${result.exitCode}\n'
        'stdout:\n${result.stdout}\n'
        'stderr:\n${result.stderr}');
  }
}
