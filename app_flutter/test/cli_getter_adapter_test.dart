import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/cli_getter_adapter.dart';

void main() {
  test(
    'CLI adapter reports platform install preparation unsupported',
    () async {
      const adapter = CliGetterAdapter(
        executable: '/unused/getter',
        dataDir: '/unused/data',
      );

      expect(adapter.supportsPlatformInstallPreparation, isFalse);
      await expectLater(
        adapter.prepareInstall('android/app/com.example.app'),
        throwsA(isA<UnsupportedError>()),
      );
    },
  );

  test('CLI snapshot consumes the single getter startup response', () async {
    final temp = await Directory.systemTemp.createTemp('getter-cli-startup-');
    addTearDown(() => temp.delete(recursive: true));
    final calls = File('${temp.path}/calls');
    final executable = File('${temp.path}/getter-fixture')
      ..writeAsStringSync('''#!/bin/sh
printf '%s\\n' "\$*" >> '${calls.path}'
cat <<'JSON'
{"ok":true,"command":"startup","data":{"format":"getter-startup-snapshot","version":1,"bootstrap":{"lifecycle":"initialized","data_dir":"/getter","main_db":{"path":"/getter/main.db","contract_version":1,"created_this_call":true},"cache_db":{"path":"/getter/cache.db","contract_version":1,"created_this_call":true},"repo":"/getter/repo","rc":"/getter/rc","repo_metadata":"/getter/repo/metadata.jsonc","diagnostics":[]},"repositories":[{"id":"local","name":"Local","priority":100,"api_version":"v1","path":null,"revision":null}],"apps":[{"package_id":"android/app/com.example","repository_id":"local","name":"Example","favorite":false,"pin_version":null,"installed_target":null,"installed_version":null,"effective_installed_version":null,"latest_version":null,"update_status":"not_installed","warning":{"free_network":false},"diagnostics":[]}],"update_count":0,"diagnostics":[]},"warnings":[]}
JSON
''');
    await Process.run('chmod', <String>['+x', executable.path]);

    final snapshot = await CliGetterAdapter(
      executable: executable.path,
      dataDir: '${temp.path}/data',
    ).loadSnapshot();

    expect(calls.readAsLinesSync(), <String>[
      '--data-dir ${temp.path}/data startup',
    ]);
    expect(snapshot.status, 'Getter initialized');
    expect(snapshot.apps.single.installedVersion, isNull);
    expect(snapshot.apps.single.updateStatus, 'not_installed');
  });
}
