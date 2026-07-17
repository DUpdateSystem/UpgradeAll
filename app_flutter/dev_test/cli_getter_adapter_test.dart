import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/cli_getter_adapter.dart';

void main() {
  test('CliGetterAdapter imports a direct legacy Room database', () async {
    final getterCli = Platform.environment['GETTER_CLI_BIN'];
    if (getterCli == null || getterCli.isEmpty) {
      fail('GETTER_CLI_BIN must point to the built getter-cli binary');
    }

    final temp = Directory.systemTemp.createTempSync('upgradeall-getter-cli-');
    addTearDown(() => temp.deleteSync(recursive: true));

    final dataDir = Directory('${temp.path}/data')..createSync();
    final legacyDb = _createLegacyRoomDatabase(temp);
    final adapter = CliGetterAdapter(
      executable: getterCli,
      dataDir: dataDir.path,
    );

    adapter.initialize();
    final result = await adapter.importLegacyRoomDatabase(legacyDb.path);

    expect(result.alreadyImported, isFalse);
    expect(result.importedRecords, 1);
    expect(result.sourceCounts?.appRows, 1);
    expect(result.sourceCounts?.extraAppRows, 1);
    final tracked = result.trackedPackages.singleWhere(
      (package) => package.id == 'android/org.fdroid.fdroid',
    );
    expect(tracked.favorite, isTrue);
    expect(tracked.pinVersion, '1.20.0');
    expect(tracked.packageResolution, 'missing_package_definition');

    final reports = await adapter.readMigrationReports();
    expect(
      reports.singleWhere((report) => report.code == 'migration.imported').ok,
      isTrue,
    );
  });

  test(
    'CliGetterAdapter reads real getter repository and tracked state',
    () async {
      final getterCli = Platform.environment['GETTER_CLI_BIN'];
      if (getterCli == null || getterCli.isEmpty) {
        fail('GETTER_CLI_BIN must point to the built getter-cli binary');
      }

      final temp = Directory.systemTemp.createTempSync(
        'upgradeall-getter-cli-',
      );
      addTearDown(() => temp.deleteSync(recursive: true));

      final dataDir = Directory('${temp.path}/data')..createSync();
      final repoDir = _createFixtureRepository(temp, 'official');
      final bundle = _createLegacyBundle(temp);
      final legacyDb = _createLegacyRoomDatabase(temp);
      final adapter = CliGetterAdapter(
        executable: getterCli,
        dataDir: dataDir.path,
      );

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
      final repositories = adapter.listRepositories();
      expect(repositories.map((repo) => repo.id), contains('official'));
      expect(
        repositories.singleWhere((repo) => repo.id == 'official').priority,
        0,
      );

      final trackedPackages = adapter.listTrackedPackages();
      final tracked = trackedPackages.singleWhere(
        (package) => package.id == 'android/org.fdroid.fdroid',
      );
      expect(tracked.favorite, isTrue);
      expect(tracked.pinVersion, '1.20.0');
      expect(tracked.packageResolution, 'official_repository_package');

      final evaluated = adapter.evaluatePackage(
        'android/org.fdroid.fdroid',
        repositoryId: 'official',
      );
      expect(evaluated.name, 'F-Droid');
      expect(evaluated.repositoryId, 'official');
      expect(evaluated.hasFreeNetworkWarning, isTrue);

      final reports = await adapter.readMigrationReports();
      expect(
        reports.singleWhere((report) => report.code == 'migration.imported').ok,
        isTrue,
      );

      final alreadyImported = await adapter.importLegacyRoomDatabase(
        legacyDb.path,
      );
      expect(alreadyImported.alreadyImported, isTrue);
      expect(alreadyImported.importedRecords, 0);
      expect(
        alreadyImported.trackedPackages.map((package) => package.id),
        contains('android/org.fdroid.fdroid'),
      );

      final snapshot = await adapter.loadSnapshot();
      expect(snapshot.status, 'Getter already initialized');
      expect(
        snapshot.repositories.map((repo) => repo.id),
        contains('official'),
      );
      final app = snapshot.apps.singleWhere(
        (app) => app.id == 'android/org.fdroid.fdroid',
      );
      expect(
        app.name,
        'F-Droid',
        reason: app.diagnostics
            .map((d) => '${d.code}: ${d.message}')
            .join('\n'),
      );
      expect(app.installedVersion, isNull);
      expect(app.updateStatus, 'not_installed');
      expect(app.hasFreeNetworkWarning, isTrue);
    },
  );
}

Directory _createFixtureRepository(Directory temp, String repoId) {
  final repoDir = Directory('${temp.path}/repo-$repoId')..createSync();
  final packageDir = Directory('${repoDir.path}/android/org.fdroid.fdroid')
    ..createSync(recursive: true);
  File('${packageDir.path}/metadata.jsonc').writeAsStringSync('''
{
  "type": "android:app",
  "display_name": "F-Droid",
  "android": { "package_name": "org.fdroid.fdroid" },
  "lua": {
    "9999.lua": { "permission": ["allow_free_network"] }
  }
}
''');
  File('${packageDir.path}/Manifest').writeAsStringSync('');
  File('${packageDir.path}/9999.lua').writeAsStringSync('''
#!/bin/upa-lua v1
return package_version {}
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
      "pin_version": "1.20.0",
      "favorite": true
    }
  ]
}
''');
}

File _createLegacyRoomDatabase(Directory temp) {
  final db = File('${temp.path}/app_metadata_database.db');
  final result = Process.runSync('python3', <String>[
    '-c',
    r'''
import sqlite3
import sys
path = sys.argv[1]
conn = sqlite3.connect(path)
conn.execute('PRAGMA user_version = 17')
conn.execute('CREATE TABLE app (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, app_id TEXT NOT NULL, ignore_version_number TEXT, star INTEGER)')
conn.execute('CREATE TABLE extra_app (id INTEGER PRIMARY KEY AUTOINCREMENT, app_id TEXT NOT NULL, mark_version_number TEXT)')
app_id = '{"android_app_package":"org.fdroid.fdroid"}'
conn.execute(
    'INSERT INTO app(id, name, app_id, ignore_version_number, star) VALUES (1, ?, ?, ?, ?)',
    ('F-Droid', app_id, '1.10.0', 1),
)
conn.execute(
    'INSERT INTO extra_app(id, app_id, mark_version_number) VALUES (1, ?, ?)',
    (app_id, '1.20.0'),
)
conn.commit()
conn.close()
''',
    db.path,
  ]);
  if (result.exitCode != 0) {
    fail(
      'failed to create legacy Room DB fixture\n'
      'stdout:\n${result.stdout}\n'
      'stderr:\n${result.stderr}',
    );
  }
  return db;
}

void _runGetter(String getterCli, String dataDir, List<String> args) {
  final result = Process.runSync(getterCli, <String>[
    '--data-dir',
    dataDir,
    ...args,
  ]);
  if (result.exitCode != 0) {
    fail(
      'getter ${args.join(' ')} failed with ${result.exitCode}\n'
      'stdout:\n${result.stdout}\n'
      'stderr:\n${result.stderr}',
    );
  }
}
