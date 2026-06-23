# ADR-0002: getter / Flutter / platform boundary

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

All product and domain logic belongs in the Rust getter core. Flutter is the only product UI and product APK entry for the rewrite. The legacy Android native UI may remain as reference code during migration, but it is not a shipped rewrite entry path. Android-native code is limited to non-UI platform adapter responsibilities.

Getter remains a separate reusable git submodule at `core-getter/src/main/rust/getter`, tracking `https://github.com/DUpdateSystem/getter`. UpgradeAll records a gitlink to a getter commit; getter CLI/core implementation belongs in that submodule, not as vendored superproject files.

The Flutter Android app embeds getter as a Rust library / FFI-style core. The app does not use a standalone getter daemon as the primary path.

Platform-specific APIs are exposed to getter through documented platform adapter seams so that thread management and platform complexity remain isolated. For Android installed inventory, ADR-0009 supersedes the earlier MethodChannel-led scan idea: Rust/native bridge code is the active caller, initializes JVM/context/classloader handles, and calls Android implementation classes for raw PackageManager facts.

## getter owns

- Package/repository model.
- Lua package evaluation.
- Provider/source orchestration.
- Version normalization and comparison.
- Release/artifact selection.
- Update status calculation.
- Download request/action generation.
- Download task state machine.
- SQLite main DB and cache DB.
- Legacy migration/import.
- Diagnostics and event streams.
- CLI behavior.

## Flutter APP owns

- UI rendering and navigation.
- Android permission prompts and user-facing permission explanations.
- User confirmation flows.
- Rendering getter-owned DTOs, platform diagnostics, and recovery states.

## Platform adapters own

- Raw Android PackageManager installed-package facts exposed through the Rust-active platform adapter accepted in ADR-0009.
- Installed version lookup through platform APIs.
- APK install / Shizuku/root/system installer adapters after installer semantics are accepted.
- Notifications / foreground service integration after background-runtime semantics are accepted.
- SAF/file picker and URI permissions.

Platform adapters expose facts/capabilities to Rust getter/native bridge code. They must not perform package-id normalization, repository resolution, Lua validation, autogen candidate selection, migration mapping, download retry policy, or storage writes.

## Boundary rule

If a workflow should be possible from getter CLI without Flutter UI, it belongs in getter.

If a workflow requires Android APIs or user-interface rendering, it belongs in the Flutter/platform adapter and is exposed to getter as a platform capability.

## Testing consequence

- Rust getter behavior is TDD-tested with unit/integration tests.
- Flutter UI and platform flows are BDD-tested through user-visible scenarios.
- Platform adapters get focused integration tests or fake adapter tests.
