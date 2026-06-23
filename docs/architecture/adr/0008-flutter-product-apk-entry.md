# ADR-0008: Flutter product APK entry

> Status: Accepted
> Date: 2026-06-23
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

`app_flutter/` is the only product Android application entry for the rewrite.

The legacy Android `:app` module and its native Activity/Fragment/XML UI are kept temporarily as reference code only. They must not be treated as the shipped product APK path for the rewrite, and new product UI flows must not be added there.

All user-visible flows migrate into Flutter. Android-native code remains allowed only for non-UI platform adapter responsibilities such as:

- legacy Room database copy/checkpoint handoff;
- installed package inventory collection;
- Android permission prompts and capability adapters;
- SAF/file picker and URI permission plumbing;
- installer handoff adapters;
- notifications/foreground-service integration after the background-runtime design is accepted;
- native/FFI bridge code that exposes getter/platform DTOs to Flutter.

## Build and release consequences

- Android CI and release APK artifacts build from `app_flutter`, not the legacy root Gradle `:app` module.
- `app_flutter` keeps the production package name `net.xzos.upgradeall` for release builds.
- `app_flutter` debug builds use `net.xzos.upgradeall.debug` so debug snapshots can be installed beside the release package.
- Release signing belongs to the Flutter Android project. CI writes `app_flutter/android/key.properties` from repository secrets and runs `flutter build apk --release`.
- The old `:app` Gradle module may still be checked for reference/skeleton integrity, but `./gradlew :app:assembleDebug` or `./gradlew :app:assembleRelease` is no longer the product APK build path.

## Rationale

The rewrite goal is Flutter APP + Rust getter core + Lua package repositories. Keeping the native Android UI as the launcher would preserve the old shell as a product dependency and blur ownership boundaries. Making `app_flutter` the product APK entry lets Flutter own all screens and navigation while Rust getter owns product/domain/storage logic.

Keeping the old native UI source temporarily reduces migration risk: it remains available for parity comparison while individual flows are rebuilt in Flutter.

## Non-goals

This ADR does not delete the legacy `:app` module yet.

This ADR does not approve live provider/downloader/background-worker/installer runtime semantics. Those remain separate Phase D decisions.

This ADR does not claim the current Flutter shell is product-complete. Until the production native/FFI bridge exists, CI can validate getter next to the Flutter APK, but the APK remains a rewrite shell/snapshot rather than a fully wired getter product.

## Follow-up

- Move every user-facing entry and flow into Flutter.
- Add platform adapters only where Android APIs are required.
- Delete or archive legacy native UI code after Flutter feature parity is reached.
- Once the production getter bridge exists, add APK-level validation that the Flutter product APK contains and exercises the intended native getter bridge.
