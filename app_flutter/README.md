# UpgradeAll Flutter app

This is the new Flutter shell and product APK entry for the UpgradeAll rewrite. It must remain a UI and platform adapter around the Rust getter core; product logic, repository resolution, storage, and migration behavior belong in getter. The legacy Android `:app` UI is kept only as reference code during migration.

## Current slice

- Android release application identity: `net.xzos.upgradeall`
- Android debug application identity: `net.xzos.upgradeall.debug`
- Stable route/action/state keys for widget and future integration/dev tests
- Placeholder routes for apps, repositories, downloads, logs, settings, and legacy migration
- `FakeGetterAdapter` for deterministic widget tests
- `CliGetterAdapter` as a development/integration bridge against the real `getter-cli` JSON envelope
- Product manifest permissions include `QUERY_ALL_PACKAGES` per ADR-0009 so the future Rust-active Android platform adapter can provide complete installed package inventory facts to getter.

`CliGetterAdapter` is not the final Android production bridge. It exists to keep the getter-owned DTO and error contract executable while the native bridge is designed in ADR-0007. Installed-autogen product flows must use getter/native bridge operations backed by the Rust-active platform adapter from ADR-0009, not a Flutter-led MethodChannel scanner.

## Verification

```bash
flutter analyze
flutter test
GETTER_CLI_BIN=/path/to/getter-cli flutter test dev_test/cli_getter_adapter_test.dart
```

From the repository root, `just verify` also runs the Flutter analyzer, widget tests, getter CLI integration/dev test, and Android debug build.

Android CI/release artifacts are built from this Flutter project with `flutter build apk`; the root Gradle `:app` module is no longer the rewrite product APK path.
