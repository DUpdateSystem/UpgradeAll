import 'android_package_installer.dart';
import 'getter_adapter.dart';

class InstallCoordinator {
  InstallCoordinator({
    required GetterAdapter getter,
    required AndroidPackageInstaller packageInstaller,
  }) : this._(getter, packageInstaller);

  InstallCoordinator._(this._getter, this._packageInstaller);

  final GetterAdapter _getter;
  final AndroidPackageInstaller _packageInstaller;
  bool _installing = false;

  Future<InstallCoordinatorResult> install(String runtimeTaskId) async {
    if (_installing) throw const InstallCoordinatorBusyException();
    _installing = true;
    try {
      final handoff = await _getter.prepareInstallTask(runtimeTaskId);
      if (handoff.taskId != runtimeTaskId) {
        throw FormatException(
          'Getter install handoff does not match runtime task "$runtimeTaskId"',
        );
      }
      final platform = await _packageInstaller.install(handoff);
      if (!platform.isTerminal) {
        return InstallCoordinatorResult(platform: platform);
      }

      final accepted =
          platform.outcome == AndroidPackageInstallOutcome.succeeded;
      final runtimeTask = await _getter.sendRuntimeUserResult(
        runtimeTaskId,
        accepted ? RuntimeUserResult.accepted : RuntimeUserResult.rejected,
        reason: accepted ? null : _failureReason(platform),
      );
      final snapshot = await _getter.loadSnapshot();
      return InstallCoordinatorResult(
        platform: platform,
        runtimeTask: runtimeTask,
        snapshot: snapshot,
      );
    } finally {
      _installing = false;
    }
  }

  String _failureReason(AndroidPackageInstallResult platform) {
    final detail = platform.message?.trim();
    if (detail != null && detail.isNotEmpty) {
      return 'package_installer.${platform.outcome.name}: $detail';
    }
    return 'package_installer.${platform.outcome.name}';
  }
}

class InstallCoordinatorResult {
  const InstallCoordinatorResult({
    required this.platform,
    this.runtimeTask,
    this.snapshot,
  });

  final AndroidPackageInstallResult platform;
  final RuntimeTaskSnapshot? runtimeTask;
  final GetterSnapshot? snapshot;
}

class InstallCoordinatorBusyException implements Exception {
  const InstallCoordinatorBusyException();

  @override
  String toString() => 'Another installation is already active';
}
