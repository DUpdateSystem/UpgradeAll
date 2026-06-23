# UpgradeAll Flutter app

This is the new Flutter shell and product APK entry for the UpgradeAll rewrite. It must remain a UI and platform adapter around the Rust getter core; product logic, repository resolution, storage, and migration behavior belong in getter. The legacy Android `:app` UI is kept only as reference code during migration.

## Current slice

- Android release application identity: `net.xzos.upgradeall`
- Android debug application identity: `net.xzos.upgradeall.debug`
- Stable route/action/state keys for widget and future integration/dev tests
- Placeholder routes for apps, repositories, downloads, logs, settings, and legacy migration
- `FakeGetterAdapter` for deterministic widget tests
- `CliGetterAdapter` as a development/integration bridge against the real `getter-cli` JSON envelope
- A slim Android `:getter_bridge` library inside `app_flutter/android/getter_bridge` packages the Rust `api_proxy` native library and the no-UI installed-inventory provider classes into the Flutter product APK without depending on the legacy native `:app` UI or old `GetterPort` RPC wrapper surface.
- `MainActivity` exposes a no-UI `net.xzos.upgradeall/getter_bridge` MethodChannel for native bridge plumbing. The installed-autogen methods derive the app-private getter data directory on Android, call Rust JNI entrypoints, and return getter-style JSON envelopes consumed by `MethodChannelGetterAdapter`.
- Product manifest permissions include `QUERY_ALL_PACKAGES` per ADR-0009 so the Rust-active Android platform adapter can provide complete installed package inventory facts to getter.

`CliGetterAdapter` is not the final Android production bridge. It exists to keep the getter-owned DTO and error contract executable. `MethodChannelGetterAdapter` is the current production bridge slice for installed-autogen preview/apply: Flutter renders getter-owned DTOs and passes accepted package ids back to getter, but PackageManager scanning, package-id decisions, and `local_autogen` writes remain in Rust/native getter code.

## Verification

```bash
flutter analyze
flutter test
GETTER_CLI_BIN=/path/to/getter-cli flutter test dev_test/cli_getter_adapter_test.dart
```

From the repository root, `just verify` also runs the Flutter analyzer, widget tests, getter CLI integration/dev test, Android debug build, and an APK inspection that verifies the Flutter APK contains `libapi_proxy.so`, `NativeLib`, and `InstalledInventoryProvider`.

Android CI/release artifacts are built from this Flutter project with `flutter build apk`; the root Gradle `:app` module is no longer the rewrite product APK path.
