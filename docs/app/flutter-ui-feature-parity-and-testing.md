# Flutter UI Feature Parity and Testing Strategy

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## UI feature parity

The Flutter UI should preserve these user-visible product capabilities unless explicitly deferred:

- Home module entry and update summary.
- Apps list and Magisk list.
- App detail with version/source/artifact selection.
- App settings/editing.
- Repository/source visibility.
- Installed-app autogen preview and confirmation.
- Download task view and controls.
- Settings.
- Logs.
- Migration/recovery status.
- Yellow warning tag for free-network Lua scripts.

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
    Then getter writes package scripts to local_autogen
    And the apps appear in the app list as generated fallback packages
```

## Current Flutter shell slice

The first Flutter implementation slice is intentionally a shell, not product logic:

- App project lives under `app_flutter/`.
- Android identity remains `net.xzos.upgradeall` for future direct upgrade work.
- `UpgradeAllApp` exposes stable route/action/state keys such as `route.home`, `action.open_apps`, `state.apps_list`, and `state.migration_ready`.
- The temporary `GetterAdapter` is fake in-memory data only. It exists to keep UI routes testable until the Rust getter FFI/RPC binding is wired.
- Product decisions such as repository resolution, updates, migrations, storage, and downloads still belong in Rust getter.

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
