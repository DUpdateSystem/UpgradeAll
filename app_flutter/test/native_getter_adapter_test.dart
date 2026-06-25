import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/native_getter_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('test/getter_bridge');
  const eventChannel = EventChannel('test/runtime_notifications');
  const eventMethodChannel = MethodChannel('test/runtime_notifications');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventMethodChannel, null);
  });

  test('native preview sends scan options and parses getter envelope',
      () async {
    MethodCall? captured;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      captured = call;
      return jsonEncode(<String, Object?>{
        'ok': true,
        'command': 'autogen installed preview',
        'data': _previewJson(),
        'warnings': <Object?>[],
      });
    });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final preview = await adapter.previewInstalledAutogen(
      options: const InstalledAutogenScanOptions(
        includeSystemApps: true,
        includeSelf: true,
      ),
    );

    expect(captured!.method, 'previewInstalledAutogen');
    expect(captured!.arguments, <String, Object?>{
      'scan_options': <String, Object?>{
        'include_system_apps': true,
        'include_self': true,
      },
    });
    expect(preview.summary.candidateCount, 1);
    expect(preview.scanStats!.returned, 1);
    expect(preview.candidates.single.packageId, 'android/com.example.autogen');
  });

  test('native apply forwards preview JSON and package acceptance', () async {
    MethodCall? captured;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      captured = call;
      return jsonEncode(<String, Object?>{
        'ok': true,
        'command': 'autogen installed apply',
        'data': <String, Object?>{
          'target_repo_id': 'local_autogen',
          'target_repo_path': '/getter/repositories/local_autogen',
          'applied_count': 1,
          'applied': <Object?>[
            <String, Object?>{
              'package_id': 'android/com.example.autogen',
              'output_relative_path':
                  'packages/android/com.example.autogen.lua',
            },
          ],
          'preserved_to_local': <Object?>[],
        },
        'warnings': <Object?>[],
      });
    });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final preview = InstalledAutogenPreview.fromJson(_previewJson());
    final result = await adapter.applyInstalledAutogen(
      preview,
      acceptedPackageIds: const <String>['android/com.example.autogen'],
    );

    expect(captured!.method, 'applyInstalledAutogen');
    final args =
        (captured!.arguments as Map<Object?, Object?>).cast<String, Object?>();
    expect(jsonDecode(args['preview_json']! as String), preview.rawJson);
    expect(args['acceptance'], <String, Object?>{
      'mode': 'packages',
      'package_ids': <String>['android/com.example.autogen'],
    });
    expect(result.applied.single.packageId, 'android/com.example.autogen');
  });

  test('native legacy import and reports parse getter envelopes', () async {
    final calls = <MethodCall>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      switch (call.method) {
        case 'importLegacyRoomDatabase':
          return jsonEncode(<String, Object?>{
            'ok': true,
            'command': 'legacy import-room-db',
            'data': <String, Object?>{
              'imported_records': 1,
              'apps': <Object?>[
                <String, Object?>{
                  'id': 'android/org.fdroid.fdroid',
                  'enabled': true,
                  'favorite': true,
                  'ignored_version': '1.20.0',
                  'repository_id': null,
                  'package_resolution': 'missing_package_definition',
                },
              ],
              'warnings': <Object?>[],
              'source_counts': <String, Object?>{
                'app_rows': 1,
                'extra_app_rows': 1,
                'hub_rows': 0,
                'extra_hub_rows': 0,
              },
            },
            'warnings': <Object?>[],
          });
        case 'legacyReportList':
          return jsonEncode(<String, Object?>{
            'ok': true,
            'command': 'legacy report-list',
            'data': <String, Object?>{
              'reports': <Object?>[
                <String, Object?>{
                  'ok': true,
                  'code': 'migration.imported',
                  'message': 'Legacy Room data imported',
                  'imported_records': 1,
                  'tracked_records': 1,
                },
              ],
            },
            'warnings': <Object?>[],
          });
        default:
          fail('unexpected method ${call.method}');
      }
    });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final importResult =
        await adapter.importLegacyRoomDatabase('/tmp/legacy.db');
    final reports = await adapter.readMigrationReports();

    expect(calls.map((call) => call.method), <String>[
      'importLegacyRoomDatabase',
      'legacyReportList',
    ]);
    expect(calls.first.arguments, <String, Object?>{
      'database_path': '/tmp/legacy.db',
    });
    expect(importResult.importedRecords, 1);
    expect(importResult.trackedPackages.single.id, 'android/org.fdroid.fdroid');
    expect(reports.single.code, 'migration.imported');
  });

  test('runtime notification stream decodes pushed JSON events', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventMethodChannel, (call) async {
      if (call.method == 'listen') {
        await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .handlePlatformMessage(
          'test/runtime_notifications',
          const StandardMethodCodec().encodeSuccessEnvelope(
            jsonEncode(<String, Object?>{
              'kind': 'task_changed',
              'task': <String, Object?>{
                'task_id': 'task-1',
                'package_id': 'android/org.fdroid.fdroid',
                'status': 'completed',
              },
            }),
          ),
          (_) {},
        );
      }
      return null;
    });

    const adapter = MethodChannelGetterAdapter(
      channel: channel,
      runtimeNotificationChannel: eventChannel,
    );

    final notification = await adapter.runtimeNotifications().first;

    expect(notification['kind'], 'task_changed');
    expect(
      (notification['task'] as Map<Object?, Object?>)['task_id'],
      'task-1',
    );
  });

  test('native runtime operation forwards operation and payload', () async {
    MethodCall? captured;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      captured = call;
      return jsonEncode(<String, Object?>{
        'ok': true,
        'command': 'runtime operation',
        'data': <String, Object?>{
          'task_id': 'task-1',
          'package_id': 'android/org.fdroid.fdroid',
          'status': 'completed',
          'phase': <String, Object?>{'category': 'completed'},
          'capabilities': <String, Object?>{
            'cancel': false,
            'pause': false,
            'resume': false,
            'retry': false,
          },
          'updated_at': 1,
        },
        'warnings': <Object?>[],
      });
    });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final data = await adapter.invokeRuntimeOperation(
      'task_get',
      payload: const <String, Object?>{'task_id': 'task-1'},
    );

    expect(captured!.method, 'runtimeOperation');
    expect(captured!.arguments, <String, Object?>{
      'operation': 'task_get',
      'payload': <String, Object?>{'task_id': 'task-1'},
    });
    expect(data['status'], 'completed');
  });

  test('native adapter maps getter error envelope to bridge exception',
      () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      return jsonEncode(<String, Object?>{
        'ok': false,
        'command': call.method,
        'error': <String, Object?>{
          'code': 'autogen.preview_error',
          'message': 'Preview failed',
          'detail': 'bad inventory',
        },
      });
    });

    const adapter = MethodChannelGetterAdapter(channel: channel);

    await expectLater(
      adapter.previewInstalledAutogen(),
      throwsA(
        isA<GetterBridgeException>().having(
          (error) => error.error.code,
          'code',
          'autogen.preview_error',
        ),
      ),
    );
  });
}

Map<String, Object?> _previewJson() => <String, Object?>{
      'operation': 'installed.preview',
      'target_repo_id': 'local_autogen',
      'target_repo_path': '/getter/repositories/local_autogen',
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
          'package_id': 'android/com.example.autogen',
          'kind': 'android',
          'display_name': 'Example Autogen',
          'installed_target': <String, Object?>{
            'kind': 'android_package',
            'package_name': 'com.example.autogen',
          },
          'action': 'create',
          'output_relative_path': 'packages/android/com.example.autogen.lua',
          'content_hash': 'fnv1a64:fake',
          'content': '-- fake generated content',
        },
      ],
      'skipped': <Object?>[],
      'diagnostics': <Object?>[],
    };
