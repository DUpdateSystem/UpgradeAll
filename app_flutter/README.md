# UpgradeAll Flutter app

This is the new Flutter shell for the UpgradeAll rewrite. It must remain a UI and platform adapter around the Rust getter core; product logic, repository resolution, storage, and migration behavior belong in getter.

## Current slice

- Android application identity: `net.xzos.upgradeall`
- Stable route/action/state keys for widget and future integration tests
- Placeholder routes for apps, repositories, downloads, logs, settings, and legacy migration
- Fake in-memory getter adapter until the Rust getter FFI/RPC binding is wired

## Verification

```bash
flutter analyze
flutter test
```

From the repository root, `just verify` also runs the Flutter analyzer and widget tests.
