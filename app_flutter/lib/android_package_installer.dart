import 'dart:async';

import 'package:flutter/services.dart';

import 'getter_adapter.dart';

abstract interface class AndroidPackageInstaller {
  Future<AndroidPackageInstallResult> install(PlatformInstallHandoff handoff);
}

class NoopAndroidPackageInstaller implements AndroidPackageInstaller {
  const NoopAndroidPackageInstaller();

  @override
  Future<AndroidPackageInstallResult> install(
    PlatformInstallHandoff handoff,
  ) async {
    return const AndroidPackageInstallResult(
      outcome: AndroidPackageInstallOutcome.failed,
      message: 'Android PackageInstaller is unavailable',
    );
  }
}

class MethodChannelAndroidPackageInstaller implements AndroidPackageInstaller {
  const MethodChannelAndroidPackageInstaller({
    MethodChannel channel = const MethodChannel(
      'net.xzos.upgradeall/package_installer',
    ),
    EventChannel eventChannel = const EventChannel(
      'net.xzos.upgradeall/package_installer_events',
    ),
  }) : this._(channel, eventChannel);

  const MethodChannelAndroidPackageInstaller._(
    this._channel,
    this._eventChannel,
  );

  final MethodChannel _channel;
  final EventChannel _eventChannel;

  @override
  Future<AndroidPackageInstallResult> install(
    PlatformInstallHandoff handoff,
  ) async {
    final authorization = await _invokeMap('authorizationStatus');
    if (_bool(authorization['authorized'], 'authorization.authorized') !=
        true) {
      await _invokeMap('requestAuthorization');
      return const AndroidPackageInstallResult(
        outcome: AndroidPackageInstallOutcome.authorizationRequired,
      );
    }

    int? sessionId;
    final bufferedEvents = <Map<String, Object?>>[];
    final terminal = Completer<Map<String, Object?>>();
    late final StreamSubscription<Object?> subscription;
    subscription = _eventChannel.receiveBroadcastStream().listen(
      (event) {
        try {
          final callback = _map(event, 'PackageInstaller callback');
          if (sessionId == null) {
            bufferedEvents.add(callback);
          } else if (_matchesSession(callback, sessionId) &&
              !terminal.isCompleted) {
            terminal.complete(callback);
          }
        } catch (error, stackTrace) {
          if (!terminal.isCompleted) {
            terminal.completeError(error, stackTrace);
          }
        }
      },
      onError: (Object error, StackTrace stackTrace) {
        if (!terminal.isCompleted) {
          terminal.completeError(error, stackTrace);
        }
      },
    );

    try {
      final submitted = await _invokeMap('install', <String, Object?>{
        'package_name': handoff.target.packageName,
        'apk_path': handoff.artifact.path,
      });
      sessionId = _int(submitted['session_id'], 'install.session_id');
      for (final callback in bufferedEvents) {
        if (_matchesSession(callback, sessionId) && !terminal.isCompleted) {
          terminal.complete(callback);
          break;
        }
      }
      return AndroidPackageInstallResult.fromPlatformCallback(
        await terminal.future.timeout(const Duration(minutes: 5)),
      );
    } finally {
      await subscription.cancel();
    }
  }

  bool _matchesSession(Map<String, Object?> callback, int sessionId) {
    return _int(callback['session_id'], 'callback.session_id') == sessionId;
  }

  Future<Map<String, Object?>> _invokeMap(
    String method, [
    Map<String, Object?> arguments = const <String, Object?>{},
  ]) async {
    try {
      final response = await _channel.invokeMapMethod<String, Object?>(
        method,
        arguments,
      );
      return _map(response, 'PackageInstaller $method response');
    } on PlatformException catch (error) {
      throw AndroidPackageInstallerException(
        code: error.code,
        message: error.message ?? 'Android PackageInstaller call failed',
      );
    }
  }
}

enum AndroidPackageInstallOutcome {
  authorizationRequired,
  succeeded,
  aborted,
  failed,
}

class AndroidPackageInstallResult {
  const AndroidPackageInstallResult({
    required this.outcome,
    this.sessionId,
    this.statusCode,
    this.message,
  });

  factory AndroidPackageInstallResult.fromPlatformCallback(
    Map<String, Object?> callback,
  ) {
    final status = callback['status'];
    if (status is! String) {
      throw const FormatException(
        'PackageInstaller callback.status is required',
      );
    }
    final outcome = switch (status) {
      'succeeded' => AndroidPackageInstallOutcome.succeeded,
      'aborted' => AndroidPackageInstallOutcome.aborted,
      'failed' => AndroidPackageInstallOutcome.failed,
      _ => throw FormatException(
        'Unsupported PackageInstaller callback status "$status"',
      ),
    };
    return AndroidPackageInstallResult(
      outcome: outcome,
      sessionId: _int(callback['session_id'], 'callback.session_id'),
      statusCode: _int(callback['status_code'], 'callback.status_code'),
      message: callback['message']?.toString(),
    );
  }

  final AndroidPackageInstallOutcome outcome;
  final int? sessionId;
  final int? statusCode;
  final String? message;

  bool get isTerminal =>
      outcome != AndroidPackageInstallOutcome.authorizationRequired;
}

class AndroidPackageInstallerException implements Exception {
  const AndroidPackageInstallerException({
    required this.code,
    required this.message,
  });

  final String code;
  final String message;

  @override
  String toString() => '$code: $message';
}

Map<String, Object?> _map(Object? value, String name) {
  if (value is Map<String, Object?>) return value;
  if (value is Map) return value.cast<String, Object?>();
  throw FormatException('$name should be a map');
}

int _int(Object? value, String name) {
  if (value is int) return value;
  throw FormatException('$name should be an integer');
}

bool _bool(Object? value, String name) {
  if (value is bool) return value;
  throw FormatException('$name should be a boolean');
}
