import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/native_getter_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('test/getter_bridge');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
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
