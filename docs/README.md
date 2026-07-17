# UpgradeAll Rewrite Documentation

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

This documentation set records the design decisions for the UpgradeAll rewrite. It exists so coding agents and human maintainers can trace every major implementation choice back to a written decision.

## Toolchain baseline

The rewrite should be validated on current stable toolchains, not old local SDKs:

- Flutter stable `>=3.44.4` with Dart `>=3.12.2 <4.0.0`.
- Rust stable; latest local validated baseline is `rustc 1.96.0` / `cargo 1.96.0`.
- Android Gradle Plugin `9.0.1`, Gradle `9.3.1`, Kotlin Gradle Plugin `2.3.20`.
- Android product APK `minSdkVersion` follows the active stable Flutter SDK's `flutter.minSdkVersion`.

Start here:

1. `architecture/upgradeall-getter-rewrite-wiki.md` — main living architecture wiki.
2. `architecture/adr/0001-app-centric-lua-package-repository-model.md` — package/repository/Lua model.
3. `architecture/adr/0002-getter-flutter-platform-boundary.md` — getter vs Flutter/platform adapter boundary.
4. `architecture/adr/0003-legacy-room-migration.md` — old Room DB migration strategy.
5. `architecture/adr/0004-sqlite-main-db-and-cache-db.md` — storage and cache split.
6. `architecture/adr/0005-lua-package-api.md` — Lua package API and Rust validation boundary.
7. `architecture/adr/0006-package-centric-cli-command-contract.md` — getter CLI automation contract.
8. `architecture/adr/0007-flutter-getter-bridge-contract.md` — Flutter/getter DTO and bridge contract.
9. `architecture/adr/0008-flutter-product-apk-entry.md` — Flutter app as the sole product APK entry.
10. `architecture/adr/0009-android-platform-adapter-and-package-visibility.md` — Rust-active Android platform adapter and package visibility policy.
11. `architecture/adr/0010-package-metadata-cache-and-version-baseline.md` — accepted package metadata cache, live-version, installed-version, and `pin_version` rules.
12. `architecture/adr/0011-lua-update-runtime-side-effects-and-events.md` — accepted Phase D Lua runtime, task/action lifecycle, mock side-effect executor, and RuntimeNotification bridge rules.
13. `architecture/adr/0012-getter-owned-provider-modules-and-autogen-refresh.md` — draft live provider design for getter-owned F-Droid autogen, standard GitHub/F-Droid Lua modules, provider cache refresh, and stale-cache semantics.
14. `architecture/adr/0013-artifact-staging-and-package-declared-installers.md` — accepted content-verified multi-artifact staging and structured package-version installer commands.
15. `architecture/adr/0014-prepare-only-android-apk-install-handoff.md` — accepted Getter-owned, prepare-only Android APK handoff transported through JNI/Kotlin/Dart.
16. `lua-api/` — practical Lua package authoring docs, including offline `repo validate` diagnostics.
17. `migration/legacy-room-mapping.md` — old data mapping rules.
18. `app/flutter-ui-feature-parity-and-testing.md` — Flutter feature parity and BDD/TDD test boundary.
19. `implementation/coding-agent-handoff.md` — coding-agent / pi-agent handoff instructions.

Canonical architecture ADRs live in `docs/architecture/adr/*`. The `docs/adr/*` directory is kept for historical/refactor-phase ADRs and transition notes.

Documentation policy:

- Every major decision must be captured in the wiki or an ADR.
- Every cross-boundary API must have schema documentation before implementation stabilizes.
- Every migration must have source/target mapping and failure behavior documented.
- Every coding agent must read `../AGENTS.md` and this docs index before implementation.
