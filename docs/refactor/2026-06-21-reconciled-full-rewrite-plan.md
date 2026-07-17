# 2026-06-21 Reconciled Full Rewrite Plan

> Status: implementation-grade plan, not implementation completion
> Scope: UpgradeAll rewrite toward **Flutter APP + Rust getter core + Lua package repository**
> Basis: `AGENTS.md`, `docs/README.md`, `docs/architecture/**`, `docs/app/flutter-ui-feature-parity-and-testing.md`, current source inspection, and context-builder/oracle findings from 2026-06-21.

## 0. Purpose

The user asked that the work must not stop at passing tests: the CLI and APP must actually run, and the result must be cross-platform. After clarification, the selected deliverable for this pass is **Full rewrite plan**.

Therefore this document does **not** claim the Flutter UI, CLI, migration, or cross-platform runtime are already complete. It defines the implementation sequence and acceptance gates required before anyone may claim completion.

## 1. Source-of-truth reconciliation

### 1.1 Authoritative docs for future implementation

Implementation must follow these files first:

1. `AGENTS.md`
2. `docs/README.md`
3. `docs/architecture/upgradeall-getter-rewrite-wiki.md`
4. `docs/architecture/adr/0001-app-centric-lua-package-repository-model.md`
5. `docs/architecture/adr/0002-getter-flutter-platform-boundary.md`
6. `docs/architecture/adr/0003-legacy-room-migration.md`
7. `docs/architecture/adr/0004-sqlite-main-db-and-cache-db.md`
8. `docs/architecture/adr/0005-lua-package-api.md`
9. `docs/app/flutter-ui-feature-parity-and-testing.md`

Older files under `docs/adr/**` and `docs/refactor/2026-06-20-*` are useful background, but where they conflict with the current architecture docs they must be revised or superseded.

### 1.2 Conflicts to resolve before coding

| Conflict | Current rule | Required action |
|---|---|---|
| Older docs use `hub` as a new CLI/domain concept (`getter hub list`, Hub Manager, hub tables). | Do not reintroduce the old hub-app model. Providers/sources/backends are not package identity. | Supersede old CLI ADR with package/repository/source vocabulary. Keep `hub` only as legacy migration input terminology. |
| Older plan uses a single `getter.db`. | Current ADR-0004 requires SQLite **main DB + cache DB** split. | Implement two DBs from the beginning of the new getter storage path. |
| Older plan describes source-level page customization and plugin ideas. | Runtime UI customization/plugin framework is explicitly not allowed for v1. | Only source fork/patch-stack customization is allowed. Runtime provider extensibility must be gated separately and must not become UI plugins. |
| Older plan leans toward a specific FFI generator. | Current docs require embedded Rust library / FFI-style boundary; generator is not fixed. | Choose FFI approach through an explicit gate before Flutter integration. |
| Older BDD plan sounds exhaustive. | Current testing rules say BDD is for meaningful user-visible flows; do not over-test with BDD. | Use BDD for CLI/App/migration flows; use TDD/unit/integration tests for domain algorithms. |

## 2. Current implementation baseline

As of this planning pass:

- The repository is still mainly the legacy Android/Kotlin app.
- There is no Flutter project (`pubspec.yaml`/Dart entry point absent).
- A partial Rust getter workspace exists under `core-getter/src/main/rust/getter`.
- `getter-core` currently has package id, repository layout, and minimal Lua table validation tests.
- `getter-storage` currently has main/cache SQLite skeleton and pure legacy mapping helper tests.
- `getter-cli` is only a library skeleton; no runnable binary exists.
- The Android JNI/RPC path currently binds a placeholder local TCP endpoint and parks forever; it is not a full getter RPC surface.
- Existing legacy Kotlin Room → Rust migration writes toward old JSONL/RPC concepts and must not be treated as the new migration implementation.
- The git worktree is already dirty/staged from prior work, including a staged deletion of the old getter gitlink and untracked replacement workspace/docs. Before implementation work, reconcile the baseline deliberately.

## 3. Completion definition: “actually runs”

Passing unit tests is insufficient. A milestone may be called complete only when it provides runtime evidence.

### 3.1 Required runtime evidence types

1. **CLI runtime evidence**
   - The `getter` CLI is invoked as an external process.
   - It creates/opens real SQLite main/cache DB files under a temp data directory.
   - It loads/evaluates real Lua package files from a fixture repository.
   - It emits stable JSON stdout for success/failure envelopes.
   - Its output is saved as test artifacts for at least smoke scenarios.

2. **APP runtime evidence**
   - Flutter app boots through the real app entry point, not just widget tests.
   - At least one desktop/dev target and Android debug target are launched in smoke gates.
   - UI flows use stable route/action/state IDs, not localized text-only selectors.
   - App interacts with a fake/offline getter backend first, then the real FFI getter when ready.

3. **Cross-platform evidence**
   - Rust getter/core/CLI tests and smoke commands run on a host CI matrix.
   - Flutter builds and smoke-runs on explicitly approved app targets.
   - Path handling, data-dir handling, and fixture loading use platform-neutral temp dirs.

4. **Migration evidence**
   - Legacy Room fixture bundles import into new getter main DB inside a transaction.
   - Dropped fields are documented.
   - Per-app failures do not block whole-app migration.
   - Global migration failure reaches a recovery UI, not a crash.

### 3.2 Recommended first target matrix

This matrix should be confirmed before implementation:

| Layer | Required first target | Later expansion |
|---|---|---|
| Rust getter core/CLI | Linux host now; CI matrix Linux/macOS/Windows before release | Additional Android target builds through Gradle/NDK |
| Flutter APP | Android debug + Linux desktop dev smoke | Windows/macOS desktop smoke if they are official supported targets |
| Legacy migration | Android official upgrade path | Manual import/export recovery path for non-official builds |

## 4. Decision gates before implementation

Do not launch broad implementation until these decisions are recorded:

1. **Cross-platform target scope**: Android + Linux dev smoke, or Android/Linux/Windows/macOS as release targets?
2. **Android upgrade identity**: Will official Flutter builds keep `applicationId = net.xzos.upgradeall` and signing key lineage?
3. **CLI vocabulary**: Supersede old `hub` CLI commands with `repo/source/provider/package` commands.
4. **FFI approach**: `flutter_rust_bridge`, manual C ABI, or a staged temporary JSON/RPC dev bridge.
5. **Main DB/cache DB schema**: exact v1 tables and migration mechanism.
6. **Legacy migration range**: which old Room schema versions are supported directly; which fields are dropped.
7. **Provider extensibility**: v1 built-in providers only, external JSON-RPC providers, or deferred plugin runtime.
8. **Repository layout in this repo**: keep transitional `core-getter/src/main/rust/getter` or move toward a cleaner workspace path.
9. **Baseline cleanup**: resolve staged deletion/untracked replacement workspace before code-writing subagents start.

## 5. New CLI contract direction

The older `getter hub list` contract must be revised. The new CLI should be package/repository-centric.

Recommended initial grammar:

```text
getter --data-dir <path> init
getter --data-dir <path> repo list
getter --data-dir <path> repo add <repo-id> <path> [--priority <n>]
getter --data-dir <path> repo eval <repo-id>
getter --data-dir <path> package eval <package-id> [--repo <repo-id>]
getter --data-dir <path> app list
getter --data-dir <path> app show <package-id>
getter --data-dir <path> app check <package-id|--all> [--offline-fixtures]
getter --data-dir <path> template list [--repo <repo-id>]
getter --data-dir <path> template run <template-id> --input <json>
getter --data-dir <path> legacy import-room-bundle <bundle.json>
getter --data-dir <path> storage validate
getter --data-dir <path> diagnostics
```

Conventions:

- JSON stdout is the default automation contract.
- Invalid CLI usage may use stderr/help text and exit code `2`.
- Structured command failures should emit JSON error envelopes on stdout.
- No command should require Flutter/Android APIs unless it explicitly declares a platform adapter/mock.

Success envelope:

```json
{
  "ok": true,
  "command": "repo list",
  "data": {},
  "warnings": []
}
```

Error envelope:

```json
{
  "ok": false,
  "command": "legacy import-room-bundle",
  "error": {
    "code": "migration.invalid_bundle",
    "message": "Legacy Room export bundle is invalid",
    "report_path": "/path/to/report.json"
  }
}
```

## 6. Implementation phases

### Phase 0 — Baseline, docs reconciliation, and verification skeleton

Goal: start from a known, reviewable baseline.

Tasks:

1. Resolve the current git/submodule/workspace state deliberately.
2. Add/supersede ADR for the package-centric CLI contract.
3. Mark older hub-oriented docs as legacy background or update terminology.
4. Add a root verification entrypoint (`justfile` or equivalent) that can run available checks.
5. Document the target platform matrix.

Validation:

```bash
git status --short
cargo test --manifest-path core-getter/src/main/rust/getter/Cargo.toml --workspace
./gradlew projects
```

Acceptance:

- No hidden dirty baseline.
- New docs state that `hub` is legacy migration terminology only.
- Verification command is present even if later targets are initially skipped.

### Phase 1 — Getter CLI executable spine

Goal: make getter independently runnable before Flutter UI work.

Tasks:

1. Add a real `getter-cli` binary target.
2. Implement minimal CLI parser and JSON envelopes.
3. Implement `init`, `repo list`, `app list`, `storage validate`, and structured errors.
4. Add BDD/Gherkin CLI smoke scenarios that invoke the binary as an external process.
5. Add internal unit tests for output serialization and data-dir handling.

Validation:

```bash
cargo test --manifest-path core-getter/src/main/rust/getter/Cargo.toml --workspace
cargo run --manifest-path core-getter/src/main/rust/getter/Cargo.toml -p getter-cli -- --data-dir /tmp/ua-getter-smoke init
cargo run --manifest-path core-getter/src/main/rust/getter/Cargo.toml -p getter-cli -- --data-dir /tmp/ua-getter-smoke repo list
cargo run --manifest-path core-getter/src/main/rust/getter/Cargo.toml -p getter-cli -- --data-dir /tmp/ua-getter-smoke app list
cargo test --manifest-path core-getter/src/main/rust/getter/Cargo.toml -p getter-cli --test bdd_cli
```

Acceptance:

- CLI creates real main/cache DB files.
- CLI returns stable JSON.
- No Android/JNI dependency appears in `getter-core` or `getter-cli`.

### Phase 2 — Repository overlay and Lua package evaluation

Goal: prove the app/package-centric repository model with real Lua files.

Tasks:

1. Implement multi-repository registry and priority resolution.
2. Implement resolved view: highest-priority package wins by package id.
3. Complete Lua evaluation boundary for JSON-like tables.
4. Add `package_from(repo, id)` with explicit repo id.
5. Add Lua override helper support through repo `lib` modules.
6. Add template listing/running skeleton.
7. Add fixture repositories: `official`, `local`, `local_autogen`.

Validation:

```bash
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke repo add official fixtures/repos/official --priority 0
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke repo add local_autogen fixtures/repos/local_autogen --priority -1
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke repo eval official
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke package eval android/org.fdroid.fdroid --repo official
cargo test -p getter-core repository lua
```

Acceptance:

- `local` > `official` > `local_autogen` priority behavior is tested.
- Path-derived package id must match declared id.
- Lua runtime/schema/domain errors are distinct.
- Free-network permission is surfaced as metadata, not executed by default.

### Phase 3 — SQLite main/cache DB foundation

Goal: replace skeleton storage with a real package/repository/user-state schema.

Main DB v1 should store:

- repositories registry and priorities;
- tracked packages and enabled/favorite state;
- user source priority overrides;
- ignored versions, pins, and per-package user state;
- migration records;
- settings and credential references;
- download task persistent state.

Cache DB v1 should store:

- evaluated package metadata;
- Lua validation result;
- provider responses;
- release candidates;
- selected latest version;
- search/cache indexes where needed.

Tasks:

1. Define schema migrations for main DB and cache DB.
2. Add storage traits used by CLI/core.
3. Add cache key calculation tests including repo id/revision/package hash/API version/getter version/platform/permission mode.
4. Add fail-fast corruption/error behavior with clear diagnostics.

Validation:

```bash
cargo test -p getter-storage
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke storage validate
sqlite3 /tmp/ua-getter-smoke/main.db '.schema'
sqlite3 /tmp/ua-getter-smoke/cache.db '.schema'
```

Acceptance:

- Main DB and cache DB are separate files.
- Storage operations are transactional.
- Cache can be cleared without losing user state.

### Phase 4 — Update lifecycle and offline provider/download proof

Goal: prove update behavior without relying on flaky live network.

Tasks:

1. Implement lifecycle validation for `preflight`, `setup`, `match`, `discover`, `prepare`, `select`, `resolve`, `post_update` where applicable.
2. Add fake/offline provider fixture responses.
3. Implement version comparison and candidate selection in Rust getter.
4. Implement update action generation (`Download`, `Install`, `OpenUrl`) with schema validation.
5. Implement download task state machine skeleton.
6. Keep direct network disabled unless package permission allows and user warning is visible.

Validation:

```bash
cargo test -p getter-core version repository lua lifecycle
cargo test -p getter-providers --features fixtures
cargo test -p getter-downloader
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke app check android/org.fdroid.fdroid --offline-fixtures
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke task list
```

Acceptance:

- Main update flow works from CLI without Flutter.
- Offline provider fixture can produce a selected update and actions.
- Live network is not required for smoke gates.

### Phase 5 — Getter platform boundary and FFI facade

Goal: expose getter to hosts without leaking domain logic into Flutter.

Tasks:

1. Choose and document FFI approach.
2. Define narrow stable DTOs for Flutter-facing facade.
3. Define platform capability traits/callbacks for PackageManager inventory, installer, notifications, SAF/file picker, and installed version lookup.
4. Provide fake platform adapter for desktop/integration tests.
5. Keep JSON-RPC/local daemon path as optional/dev/external plugin path, not main Flutter path.

Validation:

```bash
cargo test -p getter-ffi
cargo test -p getter-rpc
cargo metadata --format-version 1 --manifest-path core-getter/src/main/rust/api_proxy/Cargo.toml
./gradlew projects
```

Acceptance:

- Flutter/UI hosts call facade DTOs, not internal storage/provider modules.
- Android-only APIs are behind platform capabilities.
- Existing Gradle Cargo metadata path remains intact or is intentionally replaced with docs and working build files.

### Phase 6 — Minimal Flutter app shell

Goal: create a real cross-platform UI shell that boots.

Tasks:

1. Create Flutter project/workspace in the chosen repo layout.
2. Add packages for:
   - app shell;
   - getter contract/generated bindings;
   - UI contract;
   - UI kit;
   - default pages;
   - user/source-fork custom pages skeleton if desired, but no runtime UI plugin framework.
3. Add stable route/action/state IDs.
4. Implement bootstrap with fake getter first, then real getter init.
5. Implement minimal Home, App list, Repositories, Downloads, Logs, Settings, Migration status shell pages.

Validation:

```bash
flutter pub get
flutter analyze --fatal-infos
flutter test
flutter test integration_test
flutter build linux --debug
flutter build apk --debug
```

Runtime smoke:

```bash
flutter run -d linux --debug
adb install -r build/app/outputs/flutter-apk/app-debug.apk
adb shell am start -W -n net.xzos.upgradeall.debug/<flutter-main-activity>
adb shell pidof net.xzos.upgradeall.debug
```

Acceptance:

- App boots on Linux desktop dev target and Android debug target.
- UI tests use stable IDs.
- Flutter contains no provider/update/version/storage logic.

### Phase 7 — Flutter UI feature parity slices

Goal: implement user-visible flows through getter APIs.

Implement one vertical slice at a time:

1. Home summary and update count.
2. App/package list.
3. App detail with source/version/artifact information.
4. Repository/source visibility.
5. Free-network yellow warning tag.
6. Installed autogen preview and confirmation.
7. Download task view and controls.
8. Settings.
9. Logs/diagnostics.
10. Migration/recovery page.

For each slice:

- Write BDD scenario for user-visible behavior.
- Add/extend getter CLI/core test if logic is new.
- Add Flutter widget/integration tests.
- Run a real UI smoke flow when the slice affects navigation or launch.

Acceptance:

- Every route and primary action has stable IDs.
- App pages render loading/empty/error/content states.
- BDD scenarios are meaningful and not duplicated low-level unit tests.

### Phase 8 — Android legacy Room migration

Goal: automatic migration for normal official Android users.

Tasks:

1. Confirm official app id/signing lineage.
2. Implement Android-only legacy migrator that exports Room DB v6-v17 to a sanitized bundle.
3. Include `app`, `hub`, `extra_app`, and `extra_hub` legacy tables in the export.
4. Import bundle into getter main DB transactionally.
5. Generate `local` Lua packages only for legacy migration cases where needed.
6. Preserve mapped user state; document dropped fields.
7. Implement migration success/warning/failure UI.

Validation:

```bash
cargo test -p getter-storage legacy_room
./gradlew :legacy_migrator:testDebugUnitTest
flutter test test/migration_bootstrap_test.dart
flutter test integration_test/migration_recovery_test.dart
```

End-to-end Android evidence:

```bash
# outline; exact names depend on fixture tooling
./gradlew :app:installLegacyFixtureDebug
adb shell am start -W -n net.xzos.upgradeall/<legacy-main-activity>
./gradlew :upgradeall_flutter:installDebug
adb shell am start -W -n net.xzos.upgradeall.debug/<flutter-main-activity>
adb shell run-as net.xzos.upgradeall.debug ls files
```

Acceptance:

- Single unmapped package does not block migration.
- Global migration failure reaches recovery UI and exportable report.
- Old DB backup is retained.
- No auth/token secret leaks in logs/reports.

### Phase 9 — Installed autogen and local/local_autogen behavior

Goal: implement user-visible generated fallback packages without corrupting user overrides.

Tasks:

1. Android adapter scans installed inventory.
2. getter computes autogen candidates.
3. UI shows confirmation list.
4. Confirm writes package files to `local_autogen`.
5. Cleanup only removes missing generated packages from `local_autogen`, never `local`.

Validation:

```bash
cargo test -p getter-core autogen
cargo run -p getter-cli -- --data-dir /tmp/ua-getter-smoke template run android_installed_app --input fixtures/installed/fdroid.json
flutter test integration_test/installed_autogen_test.dart
```

Acceptance:

- Generated files are visible and can be evaluated by CLI.
- `local` remains untouched by ordinary cleanup.

### Phase 10 — Cross-platform release readiness

Goal: prove the project can be built, tested, and run on selected platforms.

Required before release candidate:

```bash
cargo fmt --all --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace --all-targets
cargo run -p getter-cli -- --data-dir "$TMPDIR/ua-getter" init
cargo run -p getter-cli -- --data-dir "$TMPDIR/ua-getter" repo eval official
flutter analyze --fatal-infos
flutter test
flutter test integration_test
flutter build apk --debug
flutter build linux --debug
```

CI matrix:

- Linux: full Rust + Flutter Linux + Android APK build.
- macOS: Rust core/CLI + Flutter tests/build where available.
- Windows: Rust core/CLI + Flutter tests/build where available.
- Android emulator lane: install/launch smoke and critical migration/autogen/download flows.

Acceptance:

- Cross-platform means explicit matrix rows are green, not an informal claim.
- Any unsupported platform is named as unsupported or not-yet-release-gated.

## 7. BDD scenario inventory

Use Gherkin for these user-visible behaviors:

### Getter CLI

- Initialize a new getter data directory.
- List repositories in JSON.
- Add/evaluate a local repository.
- Evaluate a package from a fixture Lua repo.
- List tracked apps before/after adding state.
- Check one app through offline provider fixtures.
- Submit/list a download task through fake downloader.
- Reject malformed legacy import bundle without partial state.
- Report unsupported valid legacy bundle until full import is implemented.

### Flutter APP

- Fresh launch reaches Home.
- Home opens App list.
- App list opens App detail.
- App detail displays source/version/artifact data from fake getter.
- Free-network package displays yellow warning tag.
- Installed autogen preview asks confirmation before writing.
- Cleanup preview only targets `local_autogen`.
- Download task flow shows queued/running/succeeded/failed states.
- Migration success reaches migrated App list.
- Migration failure reaches recovery page.

### Migration

- Legacy v17 export imports apps and user state.
- Legacy export with extra_app preserves ignored/marked version where mapped.
- Auth/token values are preserved where supported but redacted from reports.
- Unmapped package creates warning/missing-package state, not global failure.

## 8. Documentation updates required with implementation

Update docs in the same patch when implementation changes any of these:

- package/repository/Lua schema;
- CLI command grammar or JSON envelope;
- main/cache DB schema;
- migration mapping/dropped fields;
- FFI/platform capability boundary;
- UI route/action/state IDs;
- validation matrix/CI gates.

Prefer new ADRs for costly decisions:

- `0006-package-centric-cli-command-contract.md` under `docs/architecture/adr/`.
- `0007-ffi-binding-approach.md` if/when FFI generator is chosen.
- `0008-platform-target-matrix.md` once cross-platform targets are fixed.

## 9. Stop rules

Stop and ask for a decision if any implementation requires:

- changing official Android application id or signing assumptions;
- introducing runtime UI customization/plugin framework;
- reusing old hub-app model as the new product model;
- dropping legacy migration fields not documented in migration docs;
- adding Android-specific APIs to getter core;
- putting provider/update/version/storage logic in Flutter;
- claiming cross-platform support without a runnable gate for that platform.

## 10. First recommended implementation batch

Do not start with Flutter screens.

Recommended first batch:

1. Reconcile docs and supersede `getter hub list` with package/repo CLI contract.
2. Resolve git/submodule dirty baseline.
3. Add getter CLI binary.
4. Add CLI BDD smoke for `init`, `repo list`, `app list`, and malformed legacy import failure.
5. Make CLI create real main/cache DB files and return stable JSON.
6. Add fixture repository and package evaluation CLI smoke.
7. Only then start minimal Flutter shell.

This sequence keeps the core honest: if the CLI cannot perform the domain workflow, Flutter must not paper over the missing getter behavior.
