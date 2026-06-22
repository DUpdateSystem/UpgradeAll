# Target Architecture

Date: 2026-06-20

## Source basis

This document is based on the copied root 2026-06-20 rewrite plan at `docs/refactor/2026-06-20-upgradeall-flutter-getter-rewrite-complete-plan.md`, the synced repository state, current code inspection, Cucumber documentation lookup, and the user's clarified testing rule.

Canonical plan hash:

- SHA-256: `a9d02ce7fb88112506580a6e5e723494016ff75cc950083f66ab93701bbc3a0a`
- Copied from `xz@100.65.231.22:/home/xz/.hermes/plans/2026-06-20_181038-upgradeall-flutter-getter-rewrite-complete-plan.md`
- Matches the plan captured in the pre-sync stash untracked parent.

> All user-facing functions/interfaces need BDD Cucumber coverage. The main user-facing surfaces are the UpgradeAll App and Getter CLI. Internal interfaces use unit/integration/traditional tests because BDD fits integration behavior better than algorithm-level unit tests.

## Exact repository baseline

Superproject:

- Branch used for planning: `refactor/phase0-planning-20260620`
- Synced upstream branch: `master` / `origin/master`
- Baseline commit: `4a1aae1d44a418989b0d3d28528cacff0cc066c0`
- Baseline commit subject: `feat: hub authentication UI with auth_keywords support`
- Pre-sync local backup branch: `backup/pre-sync-master-20260620-183445` at `8a820a76bfee22228272912e4e10127b63284583`

Getter submodule:

- Path: `core-getter/src/main/rust/getter`
- Baseline commit: `f011d9b4b9a15f83cd39c86e781ad8830a8ecae6`
- Baseline subject: `feat: add auth_keywords to HubItem and manager_update_hub_auth RPC`
- Pre-sync submodule backup branch: `backup/pre-sync-20260620-183445` at `73a5fc921ef4644346f8b984ac4f10394b7ba291`

Stash backups:

- Superproject WIP backup: `stash@{1}` / `b9462fb0c8f15b1ffddd2cd36125e21e2a4b9a09`, message `backup before 2026-06-20 refactor planning 20260620-183445 (superproject)`
- Submodule WIP backup: `core-getter/src/main/rust/getter` `stash@{0}` / `ac6c76288d069b047a784df6aceb82536e870e49`, message `backup before 2026-06-20 refactor planning 20260620-183445 (submodule getter)`
- Agent artifact backup: `stash@{0}` / `7d668a1e0514972c23911f29ec11b08763db222a` in the superproject, message `agent artifacts after refactor planning context 20260620-185313`

Current Android app identity:

- `applicationId`: `net.xzos.upgradeall`
- `namespace`: `net.xzos.upgradeall`
- `versionCode`: `105`
- `versionName`: `0.20-alpha.4`
- `compileSdk`: `36`
- `targetSdk`: `36`
- `minSdk`: `23`

Current module graph:

- `:app`
- `:core`
- `:core-websdk`
- `:core-utils`
- `:core-shell`
- `:core-downloader`
- `:core-installer`
- `:core-android-utils`
- `:app-backup`
- `:core-getter`
- `:core-websdk:data`
- `:core-getter:provider`
- `:core-getter:rpc`

Current build facts:

- Gradle wrapper: `9.3.1`
- AGP: `9.0.1`
- Kotlin: `2.3.10`
- Android Rust Gradle plugin: `0.6.0`
- Java/Kotlin toolchain: `21`
- `core-getter` builds Rust `api_proxy` for Android ABIs through the Android Rust Gradle plugin.
- Top-level Gradle configuration runs Cargo metadata for `core-getter/src/main/rust/api_proxy/Cargo.toml`; breaking Cargo metadata can break Gradle configuration before tests run.

Current getter facts:

- The Rust getter crate already has `src/lib.rs` and `src/main.rs`.
- `src/main.rs` currently only prints `Hello, world!`, so the CLI exists structurally but not as a supported interface.
- Existing Rust tests use traditional Rust test tooling and fixtures; no Cucumber/Gherkin dependency is present yet.

## Target runtime layers

1. **Getter Core** owns product behavior and durable state.
2. **Getter Library** exposes the embeddable engine contract used by app/platform adapters.
3. **Getter CLI** exposes the command-line user interface for automation, diagnostics, and AI/operator workflows.
4. **UpgradeAll App** is the graphical shell and platform integration layer.
5. **Legacy Migrator** preserves supported Android user data during official upgrade.
6. **Source-level page modules** provide downstream UI customization through typed contracts and stable test IDs.

## Testing architecture

Testing is layered by audience and feedback speed:

- **BDD Cucumber/Gherkin acceptance tests**: required for user-facing UpgradeAll App behavior and Getter CLI behavior.
- **UI/widget tests**: required for page states, stable IDs, and rendering contracts.
- **Getter traditional tests**: required for algorithms, parsers, provider behavior, storage, migration, download orchestration, and library contracts.
- **Migration tests**: required before Android release, including success and failure recovery paths.
- **Black-box UI flows**: required for primary app flows using stable semantic/test IDs; these may be generated from or mapped to Gherkin scenarios.

## Phase gates

### Phase 0: Planning and verification skeleton

- Record glossary and ADRs.
- Create a single verification entrypoint.
- Do not revive stashed implementation work as accepted architecture.
- Do not implement product behavior before the first failing test is defined.

### Phase 1: Getter workspace and API seams

- Split getter by domain boundaries only after ADRs are accepted.
- Preserve Cargo metadata compatibility for Gradle during transitions.
- Define library and CLI contracts before filling behavior.

### Phase 2: Storage and migration foundation

- Implement Rust-managed SQLite behind getter tests.
- Create legacy import fixtures and failure semantics.

### Phase 3: CLI-first behavior slices

- Use Getter CLI Cucumber scenarios to drive headless product behavior.
- Reuse the same core behavior from library and CLI.

### Phase 4: Flutter app shell and UI BDD

- Build UI around getter contracts.
- Every public route/action/state receives stable IDs.
- App behavior scenarios drive integration tests.

### Phase 5: Android migration release readiness

- End-to-end migration tests on supported legacy states.
- Official Android identity preserved for direct upgrade.
- Recovery/reporting behavior verified.

## Non-goals for Phase 0

- No production code rewrite.
- No choice to delete `:core-getter:rpc` unless an ADR explicitly replaces that boundary.
- No assumption that the stashed direct-JNI work is the approved direction.
- No Flutter screen implementation before getter contracts and behavior tests exist.
