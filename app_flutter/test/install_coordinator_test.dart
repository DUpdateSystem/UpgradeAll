import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/android_package_installer.dart';
import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/install_coordinator.dart';

void main() {
  test('install readiness matches Getter waiting-user handoff contract', () {
    final ready = _runtimeTaskJson(
      status: 'running',
      phaseCategory: 'waiting_user',
      phaseReason: 'install_handoff',
    );
    final wrongStatus = _runtimeTaskJson(
      status: 'queued',
      phaseCategory: 'waiting_user',
      phaseReason: 'install_handoff',
    );
    final wrongReason = _runtimeTaskJson(
      status: 'running',
      phaseCategory: 'waiting_user',
      phaseReason: 'package_locked',
    );

    expect(RuntimeTaskSnapshot.fromJson(ready).isInstallReady, isTrue);
    expect(RuntimeTaskSnapshot.fromJson(wrongStatus).isInstallReady, isFalse);
    expect(RuntimeTaskSnapshot.fromJson(wrongReason).isInstallReady, isFalse);
  });

  test(
    'successful install accepts runtime handoff and refreshes Getter',
    () async {
      final getter = _RecordingGetterAdapter();
      final platform = _FakeAndroidPackageInstaller(
        const AndroidPackageInstallResult(
          outcome: AndroidPackageInstallOutcome.succeeded,
          sessionId: 7,
          statusCode: 0,
        ),
      );
      final coordinator = InstallCoordinator(
        getter: getter,
        packageInstaller: platform,
      );

      final result = await coordinator.install('task-1');

      expect(getter.preparedTaskId, 'task-1');
      expect(platform.handoff!.taskId, 'task-1');
      expect(platform.handoff!.artifact, _handoffFor('task-1').artifact);
      expect(getter.userResult, RuntimeUserResult.accepted);
      expect(getter.userResultTaskId, 'task-1');
      expect(getter.snapshotLoads, 1);
      expect(result.snapshot!.apps.single.installedVersion, '1.2.3');
    },
  );

  test(
    'aborted install rejects runtime handoff and remains retryable',
    () async {
      final getter = _RecordingGetterAdapter();
      final coordinator = InstallCoordinator(
        getter: getter,
        packageInstaller: _FakeAndroidPackageInstaller(
          const AndroidPackageInstallResult(
            outcome: AndroidPackageInstallOutcome.aborted,
            sessionId: 8,
            statusCode: 3,
            message: 'User declined',
          ),
        ),
      );

      final result = await coordinator.install('task-2');

      expect(result.platform.outcome, AndroidPackageInstallOutcome.aborted);
      expect(getter.userResult, RuntimeUserResult.rejected);
      expect(getter.userResultReason, contains('aborted'));
      expect(getter.snapshotLoads, 1);
    },
  );

  test('authorization request leaves Getter runtime task waiting', () async {
    final getter = _RecordingGetterAdapter();
    final coordinator = InstallCoordinator(
      getter: getter,
      packageInstaller: _FakeAndroidPackageInstaller(
        const AndroidPackageInstallResult(
          outcome: AndroidPackageInstallOutcome.authorizationRequired,
        ),
      ),
    );

    final result = await coordinator.install('task-3');

    expect(
      result.platform.outcome,
      AndroidPackageInstallOutcome.authorizationRequired,
    );
    expect(getter.userResult, isNull);
    expect(getter.snapshotLoads, 0);
  });

  test('shared coordinator rejects a handoff for another task', () async {
    final coordinator = InstallCoordinator(
      getter: _MismatchedGetterAdapter(),
      packageInstaller: _FakeAndroidPackageInstaller(
        const AndroidPackageInstallResult(
          outcome: AndroidPackageInstallOutcome.succeeded,
        ),
      ),
    );

    await expectLater(
      coordinator.install('task-1'),
      throwsA(isA<FormatException>()),
    );
  });

  test('one shared coordinator rejects overlapping installs', () async {
    final getter = _RecordingGetterAdapter();
    final platform = _BlockingAndroidPackageInstaller();
    final coordinator = InstallCoordinator(
      getter: getter,
      packageInstaller: platform,
    );

    final first = coordinator.install('task-1');
    await platform.started.future;

    await expectLater(
      coordinator.install('task-2'),
      throwsA(isA<InstallCoordinatorBusyException>()),
    );
    platform.finish();
    await first;
  });
}

const _packageId = 'android/app/com.example.app';

PlatformInstallHandoff _handoffFor(String taskId) => PlatformInstallHandoff(
  kind: PlatformInstallKind.androidApk,
  taskId: taskId,
  packageId: _packageId,
  repositoryId: 'official',
  target: const AndroidInstallTarget(packageName: 'com.example.app'),
  packageVersion: '1.2.3',
  artifact: const PlatformInstallArtifact(
    name: 'app.apk',
    path: '/getter/downloads/app.apk',
    sha256: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    status: 'downloaded',
  ),
);

class _RecordingGetterAdapter extends FakeGetterAdapter {
  String? preparedTaskId;
  String? userResultTaskId;
  RuntimeUserResult? userResult;
  String? userResultReason;
  int snapshotLoads = 0;

  @override
  Future<PlatformInstallHandoff> prepareInstall(String packageId) async {
    throw StateError(
      'runtime installs must not use package-scoped preparation',
    );
  }

  @override
  Future<PlatformInstallHandoff> prepareInstallTask(String taskId) async {
    preparedTaskId = taskId;
    return _handoffFor(taskId);
  }

  @override
  Future<RuntimeTaskSnapshot> sendRuntimeUserResult(
    String taskId,
    RuntimeUserResult result, {
    String? reason,
  }) async {
    userResultTaskId = taskId;
    userResult = result;
    userResultReason = reason;
    return _runtimeTask(taskId, result == RuntimeUserResult.accepted);
  }

  @override
  Future<GetterSnapshot> loadSnapshot() async {
    snapshotLoads += 1;
    return const GetterSnapshot(
      status: 'Getter refreshed',
      updateCount: 0,
      repositories: <RepositorySummary>[],
      apps: <AppSummary>[
        AppSummary(
          id: _packageId,
          name: 'Example',
          installedVersion: '1.2.3',
          latestVersion: '1.2.3',
          hasFreeNetworkWarning: false,
        ),
      ],
    );
  }
}

class _MismatchedGetterAdapter extends _RecordingGetterAdapter {
  @override
  Future<PlatformInstallHandoff> prepareInstallTask(String taskId) async {
    return _handoffFor('task-other');
  }
}

class _FakeAndroidPackageInstaller implements AndroidPackageInstaller {
  _FakeAndroidPackageInstaller(this.result);

  final AndroidPackageInstallResult result;
  PlatformInstallHandoff? handoff;

  @override
  Future<AndroidPackageInstallResult> install(
    PlatformInstallHandoff handoff,
  ) async {
    this.handoff = handoff;
    return result;
  }
}

class _BlockingAndroidPackageInstaller implements AndroidPackageInstaller {
  final started = Completer<void>();
  final _result = Completer<AndroidPackageInstallResult>();

  @override
  Future<AndroidPackageInstallResult> install(PlatformInstallHandoff handoff) {
    if (!started.isCompleted) started.complete();
    return _result.future;
  }

  void finish() {
    _result.complete(
      const AndroidPackageInstallResult(
        outcome: AndroidPackageInstallOutcome.succeeded,
        sessionId: 9,
        statusCode: 0,
      ),
    );
  }
}

Map<String, Object?> _runtimeTaskJson({
  required String status,
  required String phaseCategory,
  String? phaseReason,
}) {
  return <String, Object?>{
    'task_id': 'task-contract',
    'package_id': _packageId,
    'status': status,
    'phase': <String, Object?>{
      'category': phaseCategory,
      'reason': phaseReason,
    },
    'progress': null,
    'capabilities': const <String, Object?>{
      'cancel': true,
      'pause': false,
      'resume': false,
      'retry': false,
    },
    'current_diagnostic': null,
    'downloaded_file': null,
    'updated_at': 1,
  };
}

RuntimeTaskSnapshot _runtimeTask(String taskId, bool completed) {
  return RuntimeTaskSnapshot.fromJson(<String, Object?>{
    'task_id': taskId,
    'package_id': _packageId,
    'status': completed ? 'completed' : 'failed',
    'phase': <String, Object?>{
      'category': completed ? 'completed' : 'failed',
      if (!completed) 'reason': 'user_rejected',
    },
    'progress': null,
    'capabilities': <String, Object?>{
      'cancel': false,
      'pause': false,
      'resume': false,
      'retry': !completed,
    },
    'current_diagnostic': null,
    'downloaded_file': null,
    'updated_at': 1,
  });
}
