import 'dart:convert';

import 'package:flutter/services.dart';

import 'getter_adapter.dart';

/// First Android production bridge slice.
///
/// Until the full native getter bridge replaces every CLI/fake surface, this
/// adapter inherits the deterministic shell data from [FakeGetterAdapter] and
/// overrides only installed-autogen operations with the Rust/native bridge.
/// The bridge returns getter-owned JSON envelopes; Dart parses and renders them
/// but does not scan PackageManager or make autogen/package decisions.
class MethodChannelGetterAdapter extends FakeGetterAdapter {
  const MethodChannelGetterAdapter({
    MethodChannel channel = const MethodChannel(
      'net.xzos.upgradeall/getter_bridge',
    ),
  }) : _channel = channel;

  final MethodChannel _channel;

  @override
  bool get supportsLegacyRoomImport => true;

  @override
  bool get supportsInstalledAutogen => true;

  @override
  void initialize() {
    // The installed-autogen bridge initializes lazily when preview is called.
  }

  @override
  Future<List<MigrationReportSummary>> readMigrationReports() async {
    final data = await _invokeGetterData(
      'legacyReportList',
      const <String, Object?>{},
    );
    final reports = _asList(data['reports'], 'legacy reports');
    return reports
        .map((report) => MigrationReportSummary.fromJson(
              _asMap(report, 'legacy report'),
            ))
        .toList(growable: false);
  }

  @override
  Future<LegacyMigrationImportResult> importLegacyRoomDatabase(
    String databasePath,
  ) async {
    final data = await _invokeGetterData(
      'importLegacyRoomDatabase',
      <String, Object?>{'database_path': databasePath},
    );
    return LegacyMigrationImportResult.fromJson(data);
  }

  @override
  Future<InstalledAutogenPreview> previewInstalledAutogen({
    InstalledAutogenScanOptions options = const InstalledAutogenScanOptions(),
  }) async {
    final data = await _invokeGetterData(
      'previewInstalledAutogen',
      <String, Object?>{'scan_options': options.toJson()},
    );
    return InstalledAutogenPreview.fromJson(data);
  }

  @override
  Future<InstalledAutogenApplyResult> applyInstalledAutogen(
    InstalledAutogenPreview preview, {
    List<String>? acceptedPackageIds,
  }) async {
    final data = await _invokeGetterData(
      'applyInstalledAutogen',
      <String, Object?>{
        'preview_json': jsonEncode(preview.rawJson),
        'acceptance': acceptedPackageIds == null
            ? const <String, Object?>{'mode': 'all'}
            : <String, Object?>{
                'mode': 'packages',
                'package_ids': acceptedPackageIds,
              },
      },
    );
    return InstalledAutogenApplyResult.fromJson(data);
  }

  /// Invoke a getter runtime operation through the native bridge.
  ///
  /// This is an internal/debug bridge primitive for ADR-0011 wiring. Product UI
  /// should use typed getter operations and getter-issued `action_id`s rather
  /// than assembling runtime action plans in Dart.
  Future<Map<String, Object?>> invokeRuntimeOperation(
    String operation, {
    Map<String, Object?> payload = const <String, Object?>{},
  }) {
    return _invokeGetterData(
      'runtimeOperation',
      <String, Object?>{
        'operation': operation,
        'payload': payload,
      },
    );
  }

  Future<Map<String, Object?>> _invokeGetterData(
    String method,
    Map<String, Object?> arguments,
  ) async {
    try {
      final response = await _channel.invokeMethod<String>(method, arguments);
      if (response == null || response.isEmpty) {
        throw const GetterBridgeException(
          GetterError(
            code: 'bridge.empty_response',
            message: 'Getter native bridge returned an empty response',
          ),
        );
      }
      final envelope = _asMap(jsonDecode(response), 'getter bridge response');
      if (envelope['ok'] != true) {
        throw GetterBridgeException(_errorFromEnvelope(envelope));
      }
      return _asMap(envelope['data'], 'getter bridge data');
    } on PlatformException catch (error) {
      throw GetterBridgeException(
        GetterError(
          code: error.code,
          message: error.message ?? 'Getter native bridge call failed',
          detail: error.details?.toString(),
        ),
      );
    }
  }
}

GetterError _errorFromEnvelope(Map<String, Object?> envelope) {
  final error = _asMap(envelope['error'], 'getter bridge error');
  return GetterError(
    code: _asString(error['code'], 'getter bridge error.code'),
    message: _asString(error['message'], 'getter bridge error.message'),
    detail: error['detail']?.toString(),
  );
}

Map<String, Object?> _asMap(Object? value, String name) {
  if (value is Map<String, Object?>) return value;
  if (value is Map) return value.cast<String, Object?>();
  throw FormatException('$name should be a JSON object');
}

List<Object?> _asList(Object? value, String name) {
  if (value is List<Object?>) return value;
  if (value is List) return value.cast<Object?>();
  throw FormatException('$name should be a JSON array');
}

String _asString(Object? value, String name) {
  if (value is String) return value;
  throw FormatException('$name should be a string');
}
