# UpgradeAll Flutter app

This is the new Flutter shell for the UpgradeAll rewrite. It must remain a UI and platform adapter around the Rust getter core; product logic, repository resolution, storage, and migration behavior belong in getter.

## Current slice

- Android application identity: `net.xzos.upgradeall`
- Stable route/action/state keys for widget and future integration/dev tests
- Placeholder routes for apps, repositories, downloads, logs, settings, and legacy migration
- `FakeGetterAdapter` for deterministic widget tests
- `CliGetterAdapter` as a development/integration bridge against the real `getter-cli` JSON envelope

`CliGetterAdapter` is not the final Android production bridge. It exists to keep the getter-owned DTO and error contract executable while the native bridge is designed in ADR-0007.

## Verification

```bash
flutter analyze
flutter test
GETTER_CLI_BIN=/path/to/getter-cli flutter test dev_test/cli_getter_adapter_test.dart
```

From the repository root, `just verify` also runs the Flutter analyzer, widget tests, getter CLI integration/dev test, and Android debug build.
