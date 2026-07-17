# Flutter UI Feature Parity and Testing Strategy

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Toolchain baseline

The rewrite's Flutter UI/test baseline is Flutter stable `>=3.44.4` with Dart `>=3.12.2 <4.0.0`. The Android build baseline is Gradle `9.3.1`, Android Gradle Plugin `9.0.1`, and Kotlin Gradle Plugin `2.3.20`. Local validation should use the same current-stable Flutter generation as CI; older Flutter tester/Impeller builds are not an acceptable validation baseline for this rewrite. The Flutter product APK's Android `minSdkVersion` follows the active stable Flutter SDK's `flutter.minSdkVersion` (Flutter 3.44 currently uses API 24), rather than pinning an older product APK baseline below Flutter's supported default.

## UI feature parity

The Flutter UI should preserve these user-visible product capabilities unless explicitly deferred:

- Home module entry and update summary.
- Apps list and Magisk list.
- App detail with version/source/artifact selection.
- App settings/editing.
- Repository/source visibility.
- Installed-app autogen preview and confirmation, including the cache-backed installed F-Droid autogen path and the narrow GitHub Android APK autogen path.
- Download task view and controls.
- Settings.
- Logs.
- Migration/recovery status.
- Yellow warning tag for free-network Lua scripts.

Home, Apps, and App detail consume one getter-owned startup snapshot. Rust actively requests raw installed-app inventory from the platform adapter, initializes/migrates the getter data layout idempotently, resolves tracked packages through repository priority, and performs cache-only update evaluation. Flutter renders nullable installed/latest facts, exact update status, warnings, diagnostics, and Getter-owned package-setup readiness from that snapshot; it must not synthesize versions or update counts, select repositories, parse provider data, or trigger live refresh during startup.

A fresh installation whose inventory has actionable apps and no enabled tracked packages exposes a nonblocking Home setup action. Explicit setup preview scans inventory once, refreshes the signed official F-Droid catalog, prefers F-Droid matches, and offers remaining installed apps through the generated fallback repository. Flutter only renders the unified candidates and diagnostics, submits selected package ids with Getter's opaque preview id, and reloads startup after apply. If F-Droid refresh fails, a stale cache remains usable; without cache the fallback candidates remain available with a warning.

## BDD vs TDD boundary

Use mixed BDD and TDD.

### TDD

Use TDD for function/domain behavior:

- Rust functions.
- repository resolution.
- Lua validation.
- migration mapping.
- cache invalidation.
- version comparison.
- download action generation.
- error classification.

TDD tests should be small, deterministic and focused.

### BDD

Use BDD for UI and integration behavior:

- Flutter flows.
- migration UX.
- installed autogen confirmation.
- yellow network warning tag.
- update/download task flow.

BDD scenarios act as self-explaining documentation tests. Do not over-test BDD: each scenario should document a meaningful user behavior or integration boundary.

## Suggested BDD style

```gherkin
Feature: Installed app autogen

  Scenario: Generate package scripts for installed apps
    Given the device has installed apps not covered by official repository
    When the user opens Installed Autogen
    And confirms the generated list
    Then getter writes package scripts to autogen
    And the apps appear in the app list as generated fallback packages
```

## Current Flutter shell slice

The first Flutter implementation slice is intentionally a shell, not product logic:

- Product APK entry lives under `app_flutter/`; the legacy Android `:app` UI is reference-only during migration.
- Android release identity remains `net.xzos.upgradeall` for future direct upgrade work.
- Android debug identity is `net.xzos.upgradeall.debug` so Flutter debug snapshots can install beside release builds.
- `UpgradeAllApp` exposes stable route/action/state keys such as `route.home`, `action.open_apps`, `state.apps_list`, and `state.migration_ready`.
- `FakeGetterAdapter` keeps UI routes deterministic for widget tests.
- `CliGetterAdapter` exercises a real getter data directory through the `getter-cli` JSON envelope for development/integration tests.
- ADR-0007 documents the bridge contract and explicitly treats the CLI adapter as a test/development bridge, not the final Android production path.
- Product decisions such as repository resolution, updates, migrations, storage, and downloads still belong in Rust getter.
- Installed-autogen product flows must call getter/native bridge operations that use the Rust-active Android platform adapter from ADR-0009; Flutter should not lead PackageManager inventory scanning through a Dart MethodChannel API. The installed F-Droid autogen product flow forwards only scan options and accepted package ids; F-Droid catalog cache refresh/bootstrap, cache lookup, package-path derivation, repository coverage, generated content, and cache-miss diagnostics stay in getter/native bridge code. The GitHub Android APK autogen product flow forwards only owner, repository, Android package name, optional display name, preview JSON, and accepted package ids; GitHub release refresh/cache/provenance, asset matching defaults, package-path derivation, repository coverage, generated content, and diagnostics stay in getter/native bridge code. The UI may trigger the narrow default F-Droid catalog cache refresh and render getter-owned refresh status/diagnostics, but it must not expose provider fixture XML/JSON, endpoint/API-base URLs, cache-mode controls, raw provider payloads, asset filters, prerelease toggles, or live transport settings. GitHub release refresh for package update checks is likewise a getter-owned provider/runtime concern; Flutter still submits only package/update fields and renders returned diagnostics/actions.
- CI/release APK artifacts must be built from `app_flutter`, not from the legacy `:app` module.
- The app detail update button may call getter's typed update-check operation, receive a getter-issued opaque `action_id`, submit that `action_id`, and open Downloads. Flutter must not assemble or echo action payloads.
- The downloads route may render getter task/event DTOs read-only, including getter-owned downloaded-file metadata when present, and refresh after `RuntimeNotification.task_changed`, but it must not implement a Dart download task state machine, retry policy, file writing, transport/cache controls, or installer semantics. Current-state runtime queries remain authoritative.

## Test pyramid

- Many Rust unit tests.
- Moderate Rust integration tests for Lua/package/repository behavior.
- Focused Flutter widget tests for component states.
- Few BDD end-to-end scenarios for critical user flows.

## Anti-goals

- Do not use BDD for every function branch.
- Do not test Flutter UI by asserting brittle localized visible strings only.
- Do not duplicate Rust unit coverage in UI tests.
- Do not make migration tests depend on network.
