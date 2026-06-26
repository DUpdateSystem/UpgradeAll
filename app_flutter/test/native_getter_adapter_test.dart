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

  test(
    'native preview sends scan options and parses getter envelope',
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
      expect(
        preview.candidates.single.packageId,
        'android/app/com.example.autogen',
      );
    },
  );

  test('native apply forwards preview JSON and package acceptance', () async {
    MethodCall? captured;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          captured = call;
          return jsonEncode(<String, Object?>{
            'ok': true,
            'command': 'autogen installed apply',
            'data': <String, Object?>{
              'target_repo_id': 'autogen',
              'target_repo_path': '/getter/repo/autogen',
              'applied_count': 1,
              'applied': <Object?>[
                <String, Object?>{
                  'package_id': 'android/app/com.example.autogen',
                  'output_relative_path': 'android/app/com.example.autogen',
                },
              ],
            },
            'warnings': <Object?>[],
          });
        });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final preview = InstalledAutogenPreview.fromJson(_previewJson());
    final result = await adapter.applyInstalledAutogen(
      preview,
      acceptedPackageIds: const <String>['android/app/com.example.autogen'],
    );

    expect(captured!.method, 'applyInstalledAutogen');
    final args = (captured!.arguments as Map<Object?, Object?>)
        .cast<String, Object?>();
    expect(jsonDecode(args['preview_json']! as String), preview.rawJson);
    expect(args['acceptance'], <String, Object?>{
      'mode': 'packages',
      'package_ids': <String>['android/app/com.example.autogen'],
    });
    expect(result.applied.single.packageId, 'android/app/com.example.autogen');
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
                      'pin_version': '1.20.0',
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
    final importResult = await adapter.importLegacyRoomDatabase(
      '/tmp/legacy.db',
    );
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

  test(
    'native snapshot reads repositories and package data through getter',
    () async {
      final calls = <MethodCall>[];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            calls.add(call);
            final args = (call.arguments as Map<Object?, Object?>)
                .cast<String, Object?>();
            switch (args['operation']) {
              case 'repository_list':
                return jsonEncode(<String, Object?>{
                  'ok': true,
                  'command': 'read operation',
                  'data': <String, Object?>{
                    'repositories': <Object?>[
                      <String, Object?>{'id': 'official', 'priority': 0},
                    ],
                  },
                  'warnings': <Object?>[],
                });
              case 'tracked_package_list':
                return jsonEncode(<String, Object?>{
                  'ok': true,
                  'command': 'read operation',
                  'data': <String, Object?>{
                    'packages': <Object?>[
                      <String, Object?>{
                        'id': 'android/org.fdroid.fdroid',
                        'enabled': true,
                        'favorite': false,
                        'pin_version': null,
                        'repository_id': 'official',
                        'package_resolution': 'official_repository_package',
                      },
                    ],
                  },
                  'warnings': <Object?>[],
                });
              case 'package_eval':
                expect(args['payload'], <String, Object?>{
                  'package_id': 'android/org.fdroid.fdroid',
                  'repository_id': 'official',
                });
                return jsonEncode(<String, Object?>{
                  'ok': true,
                  'command': 'read operation',
                  'data': <String, Object?>{
                    'package': <String, Object?>{
                      'id': 'android/org.fdroid.fdroid',
                      'name': 'F-Droid',
                      'repository': 'official',
                      'permissions': <String, Object?>{'free_network': true},
                    },
                  },
                  'warnings': <Object?>[],
                });
              default:
                fail('unexpected read operation ${args['operation']}');
            }
          });

      const adapter = MethodChannelGetterAdapter(channel: channel);
      final snapshot = await adapter.loadSnapshot();

      expect(calls.map((call) => call.method), <String>[
        'readOperation',
        'readOperation',
        'readOperation',
      ]);
      expect(snapshot.status, 'Getter native bridge ready');
      expect(snapshot.repositories.single.id, 'official');
      expect(snapshot.apps.single.id, 'android/org.fdroid.fdroid');
      expect(snapshot.apps.single.name, 'F-Droid');
      expect(snapshot.apps.single.hasFreeNetworkWarning, isTrue);
    },
  );

  test('runtime notification stream decodes pushed JSON events', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventMethodChannel, (call) async {
          if (call.method == 'listen') {
            await TestDefaultBinaryMessengerBinding
                .instance
                .defaultBinaryMessenger
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

  test('typed runtime update check returns getter-issued action id', () async {
    final calls = <MethodCall>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          calls.add(call);
          if (call.method != 'runtimeOperation') {
            fail('unexpected method ${call.method}');
          }
          final args = (call.arguments as Map<Object?, Object?>)
              .cast<String, Object?>();
          if (args['operation'] == 'update_check_package_issue_action') {
            return jsonEncode(<String, Object?>{
              'ok': true,
              'command': 'runtime operation',
              'data': <String, Object?>{
                'package': <String, Object?>{
                  'id': 'android/org.fdroid.fdroid',
                  'name': 'F-Droid',
                  'repository': 'official',
                  'permissions': <String, Object?>{'free_network': false},
                },
                'update': <String, Object?>{
                  'network_required': false,
                  'package_id': 'android/org.fdroid.fdroid',
                  'installed_version': '1.0.0',
                  'effective_local_version': '1.0.0',
                  'policy': <String, Object?>{'pin_version': null},
                  'status': 'update_available',
                  'selected': <String, Object?>{
                    'package_id': 'android/org.fdroid.fdroid',
                    'candidate': <String, Object?>{
                      'version': '1.2.0',
                      'artifacts': <Object?>[],
                    },
                  },
                  'actions': <Object?>[
                    <String, Object?>{
                      'type': 'download',
                      'url': 'https://example.invalid/app.apk',
                      'file_name': 'app.apk',
                    },
                  ],
                },
                'action': <String, Object?>{
                  'action_id': 'action-1',
                  'package_id': 'android/org.fdroid.fdroid',
                },
              },
              'warnings': <Object?>[],
            });
          }
          if (args['operation'] == 'task_submit') {
            return jsonEncode(<String, Object?>{
              'ok': true,
              'command': 'runtime operation',
              'data': _runtimeTaskJson('task-1', status: 'queued'),
              'warnings': <Object?>[],
            });
          }
          fail('unexpected runtime operation ${args['operation']}');
        });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final update = await adapter.checkPackageForUpdate(
      'android/org.fdroid.fdroid',
      repositoryId: 'official',
      installedVersion: '1.0.0',
    );
    final task = await adapter.submitRuntimeAction(update.action!.actionId);

    expect(update.action!.actionId, 'action-1');
    expect(update.update.selectedVersion, '1.2.0');
    expect(task.taskId, 'task-1');
    expect(calls.first.arguments, <String, Object?>{
      'operation': 'update_check_package_issue_action',
      'payload': <String, Object?>{
        'package_id': 'android/org.fdroid.fdroid',
        'repository_id': 'official',
        'installed_version': '1.0.0',
      },
    });
    expect(calls.last.arguments, <String, Object?>{
      'operation': 'task_submit',
      'payload': <String, Object?>{'action_id': 'action-1'},
    });
  });

  test('typed runtime task controls parse task snapshots', () async {
    final operations = <String>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          final args = (call.arguments as Map<Object?, Object?>)
              .cast<String, Object?>();
          operations.add(args['operation']! as String);
          return jsonEncode(<String, Object?>{
            'ok': true,
            'command': 'runtime operation',
            'data':
                args['operation'] == 'task_list' ||
                    args['operation'] == 'task_clean'
                ? <String, Object?>{
                    'tasks': <Object?>[
                      _runtimeTaskJson('task-1', status: 'running'),
                    ],
                  }
                : _runtimeTaskJson('task-1', status: 'running'),
            'warnings': <Object?>[],
          });
        });

    const adapter = MethodChannelGetterAdapter(channel: channel);
    final tasks = await adapter.listRuntimeTasks(active: true);
    final canceled = await adapter.cancelRuntimeTask('task-1');
    final cleaned = await adapter.cleanRuntimeTasks();

    expect(tasks.single.status, 'running');
    expect(canceled.taskId, 'task-1');
    expect(cleaned.single.taskId, 'task-1');
    expect(operations, <String>['task_list', 'task_cancel', 'task_clean']);
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

  test(
    'native adapter maps getter error envelope to bridge exception',
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
    },
  );
}

Map<String, Object?> _runtimeTaskJson(
  String taskId, {
  required String status,
}) => <String, Object?>{
  'task_id': taskId,
  'package_id': 'android/org.fdroid.fdroid',
  'status': status,
  'phase': <String, Object?>{'category': status},
  'progress': null,
  'capabilities': <String, Object?>{
    'cancel': true,
    'pause': false,
    'resume': false,
    'retry': false,
  },
  'current_diagnostic': null,
  'updated_at': 1,
};

Map<String, Object?> _previewJson() => <String, Object?>{
  'operation': 'installed.preview',
  'target_repo_id': 'autogen',
  'target_repo_path': '/getter/repo/autogen',
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
      'package_id': 'android/app/com.example.autogen',
      'kind': 'android',
      'display_name': 'Example Autogen',
      'installed_target': <String, Object?>{
        'kind': 'android_package',
        'package_name': 'com.example.autogen',
      },
      'action': 'create',
      'output_relative_path': 'android/app/com.example.autogen',
      'content_hash': 'sha512:fake',
      'content': '-- fake generated content',
    },
  ],
  'skipped': <Object?>[],
  'diagnostics': <Object?>[],
};
