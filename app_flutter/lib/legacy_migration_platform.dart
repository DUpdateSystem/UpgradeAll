// ignore_for_file: prefer_initializing_formals

import 'package:flutter/services.dart';

import 'getter_adapter.dart';

/// Android platform boundary for preparing legacy Room databases for getter.
///
/// This adapter is intentionally non-UI. Android code may locate, checkpoint,
/// and copy the legacy Room SQLite files, but Rust getter still owns migration
/// mapping/import semantics and Flutter owns all user-visible screens.
abstract interface class LegacyMigrationPlatform {
  Future<LegacyRoomImportCandidate> prepareLegacyRoomImport();
}

class LegacyRoomImportCandidate {
  const LegacyRoomImportCandidate({
    required this.found,
    required this.databasePath,
    required this.message,
  });

  final bool found;
  final String? databasePath;
  final String? message;
}

class MethodChannelLegacyMigrationPlatform implements LegacyMigrationPlatform {
  // Keep the public `channel` parameter name for tests/callers.
  const MethodChannelLegacyMigrationPlatform({
    MethodChannel channel = const MethodChannel(
      'net.xzos.upgradeall/legacy_migration',
    ),
  }) : _channel = channel;

  final MethodChannel _channel;

  @override
  Future<LegacyRoomImportCandidate> prepareLegacyRoomImport() async {
    final Map<String, Object?>? result;
    try {
      result = await _channel.invokeMapMethod<String, Object?>(
        'prepareLegacyRoomImport',
      );
    } on PlatformException catch (error) {
      throw GetterBridgeException(
        GetterError(
          code: error.code,
          message: error.message ?? 'Legacy migration platform adapter failed',
          detail: error.details?.toString(),
        ),
      );
    }
    if (result == null) {
      throw const FormatException('legacy migration platform returned null');
    }
    return _candidateFromJson(result);
  }
}

class NoopLegacyMigrationPlatform implements LegacyMigrationPlatform {
  const NoopLegacyMigrationPlatform();

  @override
  Future<LegacyRoomImportCandidate> prepareLegacyRoomImport() async {
    return const LegacyRoomImportCandidate(
      found: false,
      databasePath: null,
      message: 'Legacy migration platform adapter is not connected',
    );
  }
}

LegacyRoomImportCandidate _candidateFromJson(Map<String, Object?> json) {
  final found = json['found'];
  final databasePath = json['database_path'];
  final message = json['message'];
  if (found is! bool) {
    throw const FormatException('legacy migration found should be a boolean');
  }
  if (databasePath != null && databasePath is! String) {
    throw const FormatException(
      'legacy migration database_path should be a string or null',
    );
  }
  if (message != null && message is! String) {
    throw const FormatException(
      'legacy migration message should be a string or null',
    );
  }
  return LegacyRoomImportCandidate(
    found: found,
    databasePath: databasePath as String?,
    message: message as String?,
  );
}
