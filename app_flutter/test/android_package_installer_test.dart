import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:upgradeall/android_package_installer.dart';
import 'package:upgradeall/getter_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('test/package_installer');
  const eventChannel = EventChannel('test/package_installer_events');
  const eventMethodChannel = MethodChannel('test/package_installer_events');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventMethodChannel, null);
  });

  test('authorization settings are shown before creating a session', () async {
    final calls = <MethodCall>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          calls.add(call);
          return switch (call.method) {
            'authorizationStatus' => <String, Object?>{'authorized': false},
            'requestAuthorization' => <String, Object?>{
              'authorized': false,
              'settings_shown': true,
            },
            _ => fail('unexpected call ${call.method}'),
          };
        });

    const installer = MethodChannelAndroidPackageInstaller(
      channel: channel,
      eventChannel: eventChannel,
    );
    final result = await installer.install(_handoff);

    expect(calls.map((call) => call.method), <String>[
      'authorizationStatus',
      'requestAuthorization',
    ]);
    expect(result.outcome, AndroidPackageInstallOutcome.authorizationRequired);
  });

  test('typed Getter target and APK path produce terminal success', () async {
    MethodCall? installCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          return switch (call.method) {
            'authorizationStatus' => <String, Object?>{'authorized': true},
            'install' => () {
              installCall = call;
              return <String, Object?>{
                'session_id': 42,
                'package_name': 'com.example.app',
              };
            }(),
            _ => fail('unexpected call ${call.method}'),
          };
        });
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventMethodChannel, (call) async {
          if (call.method == 'listen') {
            await TestDefaultBinaryMessengerBinding
                .instance
                .defaultBinaryMessenger
                .handlePlatformMessage(
                  eventChannel.name,
                  const StandardMethodCodec()
                      .encodeSuccessEnvelope(<String, Object?>{
                        'session_id': 42,
                        'status': 'succeeded',
                        'status_code': 0,
                        'message': null,
                      }),
                  (_) {},
                );
          }
          return null;
        });

    const installer = MethodChannelAndroidPackageInstaller(
      channel: channel,
      eventChannel: eventChannel,
    );
    final result = await installer.install(_handoff);

    expect(installCall!.arguments, <String, Object?>{
      'package_name': 'com.example.app',
      'apk_path': '/getter/downloads/app.apk',
    });
    expect(result.outcome, AndroidPackageInstallOutcome.succeeded);
    expect(result.sessionId, 42);
    expect(result.statusCode, 0);
  });

  test('aborted callback stays distinct from generic failure', () {
    final result = AndroidPackageInstallResult.fromPlatformCallback(
      const <String, Object?>{
        'session_id': 9,
        'status': 'aborted',
        'status_code': 3,
        'message': 'User declined',
      },
    );

    expect(result.outcome, AndroidPackageInstallOutcome.aborted);
    expect(result.message, 'User declined');
  });
}

const _handoff = PlatformInstallHandoff(
  kind: PlatformInstallKind.androidApk,
  packageId: 'android/app/com.example.app',
  repositoryId: 'official',
  target: AndroidInstallTarget(packageName: 'com.example.app'),
  packageVersion: '1.2.3',
  artifact: PlatformInstallArtifact(
    name: 'app.apk',
    path: '/getter/downloads/app.apk',
    sha256: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    status: 'downloaded',
  ),
);
