# UpgradeAll Flutter + getter Rust Core Rewrite Implementation Plan

> **For Hermes:** Use `subagent-driven-development` skill to implement this plan task-by-task after the user explicitly asks for execution.

**Goal:** Rewrite `DUpdateSystem/UpgradeAll` as a Flutter app shell whose durable logic lives in `DUpdateSystem/getter` as a Rust-first core, while preserving existing Android users' Room database data through a tested upgrade path.

**Architecture:** `getter` becomes the headless product engine: storage, migrations, providers, downloads, version comparison, update orchestration, plugin registry, event streams, CLI/TUI API. `UpgradeAll` becomes a Flutter UI/platform shell with source-level customizable page modules, typed generated contracts, stable test IDs, and Android platform adapters. Android legacy migration is treated as a first-class compatibility subsystem, not a best-effort startup hack.

**Tech Stack:** Rust workspace (`getter-core`, `getter-storage`, `getter-provider`, `getter-downloader`, `getter-plugin-api`, `getter-ffi`, `getter-rpc`, `getter-cli`), Rust-managed SQLite, Flutter/Dart, `flutter_rust_bridge` v2 or equivalent Dart FFI generator, Flutter `integration_test`, Maestro for black-box semantic UI flows, Patrol only for native OS automation, Android legacy Room migrator module for old installed users.

---

## 0. Source and docs basis

User-selected decision:

- UI framework: **Flutter**.
- Distribution philosophy: source-level downstream customization. Users can fork, ask AI to modify pages, merge upstream, compile their own build, and rely on strong module boundaries, type checks, tests, and compile-time failures.
- Development posture: CLI/opencode/Emacs first; do not assume Android Studio.

Read-only source inspection used:

- `DUpdateSystem/UpgradeAll`
- `DUpdateSystem/getter`

Relevant current-code facts:

- `UpgradeAll/settings.gradle:13-25` defines modules: `:app`, `:core`, `:core-websdk`, `:core-utils`, `:core-shell`, `:core-downloader`, `:core-installer`, `:core-android-utils`, `:app-backup`, `:core-getter`, `:core-websdk:data`, `:core-getter:provider`, `:core-getter:rpc`.
- `UpgradeAll/app/build.gradle:71-74` still enables `dataBinding` and `viewBinding`; `app/build.gradle:131-143` already has Compose deps, but we are now choosing Flutter for the rewrite.
- `UpgradeAll/core-getter/build.gradle:37-52` already builds a Rust `api_proxy` for Android ABIs via an Android Rust Gradle plugin.
- `UpgradeAll/core-getter/src/main/java/net/xzos/upgradeall/getter/NativeLib.kt:17-26` loads `api_proxy` via `System.loadLibrary("api_proxy")` and exposes JNI `runServer`.
- `UpgradeAll/core-getter/src/main/java/net/xzos/upgradeall/getter/GetterPort.kt:25-35` starts the Rust service and creates a Kotlin `GetterService` client.
- `GetterPort.kt:147-168` already exposes `registerProvider` and `registerDownloader`.
- `UpgradeAll/core-getter/rpc/.../GetterService.kt:13-187` already defines a broad async service API for init, release lookup, cloud config, provider/downloader registration, download tasks, app manager, hub manager, extra records, Android API, notification, and cloud config manager.
- `getter/src/websdk/repo/provider.rs:22-40` has built-in Rust provider registry for GitHub, F-Droid, GitLab, and LSPosed.
- `getter/src/websdk/repo/provider.rs:48-55` supports dynamic `add_provider`.
- `getter/src/rpc/server.rs:71-85` starts JSON-RPC server; `server.rs:173-180` registers an external provider; `server.rs:187-220` handles download info and URL replacement.
- `UpgradeAll/core/src/main/java/net/xzos/upgradeall/core/database/MetaDatabase.kt:21-24` declares Room `MetaDatabase` with entities `AppEntity`, `HubEntity`, `ExtraAppEntity`, `ExtraHubEntity`, version `17`.
- `MetaDatabase.kt:55-77` registers migrations `6->7`, `7->8`, `8->9`, `9->10`, `8->10`, `10->11`, ..., `16->17`, and uses database name `app_metadata_database.db`.
- Current legacy Room v17 tables contain:
  - `app`: `name`, `app_id`, `invalid_version_number_field_regex`, `include_version_number_field_regex`, `ignore_version_number`, `cloud_config`, `enable_hub_list`, `star`, `id`.
  - `hub`: `uuid`, `hub_config`, `auth`, `ignore_app_id_list`, `applications_mode`, `user_ignore_app_id_list`, `sort_point`.
  - `extra_app`: `id`, `app_id`, `mark_version_number`.
  - `extra_hub`: `id`, `enable_global`, `url_replace_search`, `url_replace_string`.
- `UpgradeAll/core/src/main/java/net/xzos/upgradeall/core/database/migration/RustMigration.kt` already attempts Room -> Rust JSONL migration, but it currently migrates apps, hubs, and extra hubs only; it does not migrate `extra_app`, skips if `apps.jsonl` exists, and lets Rust assign random new app UUIDs. This is not enough for a safe official Flutter rewrite migration.
- `getter/src/database/mod.rs` currently uses JSONL stores: `apps.jsonl`, `hubs.jsonl`, `extra_apps.jsonl`, `extra_hubs.jsonl`.
- `getter/src/database/store.rs` rewrites whole JSONL files under file locks. This is simple, but it lacks a formal schema migration system and is not ideal as the long-term compatibility storage for old installed Android users.

Docs checked / used as design constraints:

- Flutter official integration testing docs: Flutter supports unit/widget/integration tests; integration tests can be run with `flutter test integration_test` on supported targets.
- Flutter official native-code binding docs: Flutter can bind to native code through Dart FFI; for Rust, a binding generator such as `flutter_rust_bridge` is the practical high-level path.
- Android Room migration docs: Room migration errors can crash users; migrations should preserve user data, rely on exported schemas, and be tested. Manual migrations are needed for complex schema changes. Exported schema JSON files should be version-controlled and used in migration tests.
- Maestro Flutter docs/search result: Maestro tests Flutter apps through the Flutter Semantics Tree; use semantic labels / `Semantics` / semantic identifiers instead of brittle localized text.
- Patrol docs: Flutter `integration_test` cannot interact with the OS itself; Patrol native automation is useful for permissions, notifications, and other native OS interactions.

Note: the requested `grab-docs` skill is not installed in this Hermes profile. I used the closest available workflow: source-code audit + official documentation lookup.

---

## 1. Non-negotiable architecture decisions

### Decision 1: `getter` owns product logic

`getter` owns:

- providers and provider registry;
- download-info extraction;
- downloader task management;
- version comparison and filtering;
- update status calculation;
- app/hub/extra record storage;
- cloud config parsing/application;
- plugin manifests and plugin runtime;
- event stream;
- legacy import and new storage migrations;
- CLI/TUI command API.

Flutter owns:

- navigation;
- page rendering;
- platform widgets;
- source-level customizable page modules;
- Android/iOS/desktop platform adapters;
- user interaction and accessibility/semantics identifiers.

Flutter must not own provider logic, downloader logic, version comparison, URL replacement, durable update state, or DB migration semantics.

### Decision 2: official Android upgrade keeps package identity

For users updating from old UpgradeAll to the Flutter rewrite:

- Keep Android `applicationId = "net.xzos.upgradeall"` for official releases.
- Use the same signing key lineage for official upgrade builds.
- If application ID or signing key changes, the new app cannot access the old app-private Room DB path. In that case, a separate migration bridge/export release is required.

### Decision 3: Rust storage should move from ad-hoc JSONL to Rust-managed SQLite

Current `getter` JSONL storage is useful for early extraction but is not ideal for long-lived mobile app compatibility.

Recommended v1 storage for Flutter rewrite:

- Rust-managed SQLite database, e.g. `getter.db`.
- Embedded Rust migrations, versioned by `PRAGMA user_version` plus a `schema_migrations` / `migration_runs` table.
- Access through `getter-storage`, not through Dart Drift/sqflite.
- Android legacy Room DB is imported into `getter.db` exactly once.
- Existing `apps.jsonl` / `hubs.jsonl` / `extra_*.jsonl` alpha data gets its own importer.

Rationale:

- Old app data is already SQLite.
- SQLite has transactionality and schema migration semantics.
- Flutter/Dart storage would split ownership away from Rust core.
- JSONL whole-file rewrite becomes fragile as the data model grows.

### Decision 4: source-level page customization, not runtime UI plugins

The user-customization model is:

```text
upstream source release
  -> downstream user fork
  -> AI modifies page modules
  -> user merges upstream later
  -> compiler/tests reveal breakages
  -> user builds their own APK/desktop app
```

So the app must provide:

- stable typed `ui_contract`;
- stable `ui_kit` components;
- upstream-owned default pages;
- downstream-owned custom page package/registry;
- strict analyzer settings;
- generated API bindings that users do not edit;
- one-command verification.

### Decision 5: UI testability is a product requirement

Every public page/action must have:

- stable route ID;
- stable semantic identifier/test ID;
- loading/empty/error/content state IDs;
- widget tests where possible;
- integration tests for primary flows;
- Maestro flows for black-box AI/manual clicking;
- Patrol only where native OS automation is needed.

---

## 2. Target repository layout

Keep the two public repos conceptually separate, but make local development easy.

### `DUpdateSystem/getter`

```text
getter/
  Cargo.toml                       # workspace
  crates/
    getter-core/                   # pure domain: apps/hubs/releases/status/version logic
    getter-storage/                # Rust SQLite, migrations, legacy imports
    getter-providers/              # built-in providers + provider traits
    getter-downloader/             # downloader tasks and backend routing
    getter-plugin-api/             # plugin manifest, permissions, schema, ABI
    getter-rpc/                    # JSON-RPC/WebSocket for external plugins/automation
    getter-ffi/                    # Flutter-facing facade for flutter_rust_bridge
    getter-cli/                    # headless CLI; proves core is UI-independent
    getter-tui/                    # optional later; ratatui/crossterm
  migrations/
    getter/                        # new Rust SQLite schema migrations
    legacy-room/                   # docs/schema snapshots for import reference
  fixtures/
    legacy-room/                   # old DB fixtures for v6-v17 migration tests
    providers/                     # GitHub/GitLab/F-Droid/LSPosed fixtures
  docs/
    adr/
    api/
    migration/
```

### `DUpdateSystem/UpgradeAll`

```text
UpgradeAll/
  AGENTS.md
  justfile
  pubspec.yaml                     # Flutter app workspace root if desired
  native/
    getter/                        # git submodule or pinned workspace checkout of DUpdateSystem/getter
  apps/
    upgradeall_flutter/
      pubspec.yaml
      lib/
        main.dart
        app_shell.dart
        bootstrap.dart
        platform/
        routing/
      android/                     # same applicationId for official upgrade
      ios/
      linux/
      windows/
      macos/
      integration_test/
      test/
  packages/
    upgradeall_contract/           # generated typed Dart DTO/client facade; do not edit manually
    upgradeall_ui_contract/        # PageContext, RouteSpec, UiId, PageDescriptor
    upgradeall_ui_kit/             # reusable widgets/components
    upgradeall_pages_default/      # upstream maintained default pages
    upgradeall_pages_custom/       # downstream/user maintained page overlay; upstream touches minimally
    upgradeall_pages_examples/     # examples/templates; safe for upstream edits
  tools/
    gen_contract/
    verify_custom_pages/
    migrate_contract/
    ai_review/
  docs/
    adr/
    architecture/
    migration/
    ai-development.md
    custom-pages.md
    testing.md
```

Important downstream merge rule:

- Upstream should avoid editing `packages/upgradeall_pages_custom/` after initial skeleton creation.
- Upstream examples/templates go under `packages/upgradeall_pages_examples/`.
- Users should modify `pages_custom`, not `app_shell`, not `getter`, not generated bindings.

---

## 3. Flutter app architecture

### 3.1 Runtime layers

```text
Flutter main()
  -> bootstrap platform paths
  -> Android legacy migration check/import if needed
  -> getter_ffi.init(data_dir, cache_dir, platform_capabilities)
  -> AppShell
  -> PageRegistry(default pages + custom pages)
  -> PageContext(getter client, event stream, navigation, theme, platform services)
```

### 3.2 Dart package responsibilities

`upgradeall_contract`:

- Generated from `getter-ffi` / Rust DTO declarations.
- Contains `GetterClient`, DTOs, event models, error models.
- Do not manually edit.

`upgradeall_ui_contract`:

- Source-stable API for custom pages.
- Contains:

```dart
abstract interface class UpgradeAllPage {
  RouteSpec get route;
  UiText get title;
  Widget build(PageContext ctx);
}

final class PageContext {
  final GetterClient getter;
  final AppNavigator nav;
  final Stream<GetterEvent> events;
  final PlatformServices platform;
  final UpgradeAllTheme theme;
}

final class UiId {
  final String value;
  const UiId(this.value);
}
```

`upgradeall_ui_kit`:

- App list widget.
- Release list widget.
- Hub selector widget.
- Plugin config schema renderer.
- Download task card.
- Error panel.
- Loading/empty state components.
- Test ID / semantics helpers.

`upgradeall_pages_default`:

- Home page.
- App list page.
- App detail page.
- Release/download page.
- Hub manager page.
- Discover/cloud config page.
- Download task manager page.
- Settings page.
- Logs/diagnostics page.
- Migration status page.

`upgradeall_pages_custom`:

- User-owned replacement/additional pages.
- Custom page registry.
- Optional theme overrides.
- Must depend only on `upgradeall_ui_contract`, `upgradeall_ui_kit`, and `upgradeall_contract`.

### 3.3 State management

Keep state management simple and AI-readable.

Recommended v1:

- Use plain typed service classes + `ValueNotifier`/`StreamBuilder` where sufficient.
- If app complexity requires provider injection, use `flutter_riverpod` without codegen initially.
- Do not add heavy code generation in UI packages except generated Rust bindings.

Rule:

- Domain state comes from `getter` snapshots/events.
- Flutter state is view state only: selected tab, visible filter, form draft, scroll state, local animation state.

---

## 4. Rust API and FFI plan

### 4.1 Use a narrow Flutter-facing facade

Do not expose internal Rust modules directly to Dart.

Create `getter-ffi` facade:

```rust
pub struct GetterHandle { /* opaque */ }

pub async fn init(config: InitConfig) -> Result<GetterHandle>;
pub async fn list_apps(handle: &GetterHandle, query: AppQuery) -> Result<AppPage>;
pub async fn get_app_detail(handle: &GetterHandle, app_id: AppRecordId) -> Result<AppDetail>;
pub async fn renew_all(handle: &GetterHandle) -> Result<TaskId>;
pub async fn renew_app(handle: &GetterHandle, app_id: AppRecordId) -> Result<TaskId>;
pub async fn list_hubs(handle: &GetterHandle) -> Result<Vec<HubSummary>>;
pub async fn save_hub(handle: &GetterHandle, draft: HubDraft) -> Result<HubSummary>;
pub async fn submit_download(handle: &GetterHandle, req: DownloadRequest) -> Result<TaskId>;
pub fn event_stream(handle: &GetterHandle) -> impl Stream<Item = GetterEvent>;
```

Expose only DTOs that are stable and serializable.

### 4.2 Keep JSON-RPC for external extensibility

`getter-rpc` remains useful for:

- external provider plugins;
- external downloader plugins;
- CLI/debug automation;
- eventual local daemon mode;
- integration tests independent of Flutter.

But Flutter should normally use direct FFI bindings, not local WebSocket JSON-RPC for every UI operation.

### 4.3 Error model

Define typed errors in Rust and generated Dart:

```rust
pub enum GetterError {
    Storage(StorageError),
    Network(NetworkError),
    Provider(ProviderError),
    Migration(MigrationError),
    Platform(PlatformError),
    Permission(PermissionError),
    InvalidInput(ValidationError),
}
```

Each error must include:

- stable code;
- human-readable message;
- optional recoverability flag;
- optional diagnostic ID;
- optional source record ID.

Do not pass raw panics/strings across FFI.

---

## 5. Storage design

### 5.1 New Rust SQLite schema v1

Recommended core tables:

```text
meta(
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
)

schema_migrations(
  version INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  applied_at INTEGER NOT NULL,
  checksum TEXT NOT NULL
)

migration_runs(
  id TEXT PRIMARY KEY,
  source_kind TEXT NOT NULL,          -- legacy_room, legacy_jsonl, fresh
  source_version TEXT,
  source_hash TEXT,
  status TEXT NOT NULL,               -- started, completed, failed
  started_at INTEGER NOT NULL,
  completed_at INTEGER,
  report_json TEXT
)

apps(
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  app_id_json TEXT NOT NULL,
  app_id_hash TEXT NOT NULL,
  invalid_version_number_field_regex TEXT,
  include_version_number_field_regex TEXT,
  ignore_version_number TEXT,
  cloud_config_json TEXT,
  enable_hub_list_json TEXT,
  star INTEGER,
  legacy_room_id INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)

hubs(
  uuid TEXT PRIMARY KEY,
  hub_config_json TEXT NOT NULL,
  auth_json TEXT NOT NULL,
  ignore_app_id_list_json TEXT NOT NULL,
  applications_mode INTEGER NOT NULL,
  user_ignore_app_id_list_json TEXT NOT NULL,
  sort_point INTEGER NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)

extra_apps(
  id TEXT PRIMARY KEY,
  app_id_json TEXT NOT NULL,
  app_id_hash TEXT NOT NULL,
  mark_version_number TEXT,
  legacy_room_id INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)

extra_hubs(
  id TEXT PRIMARY KEY,
  enable_global INTEGER NOT NULL,
  url_replace_search TEXT,
  url_replace_string TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)

download_tasks(...)
provider_plugins(...)
downloader_plugins(...)
event_log(...)              -- optional, bounded/rotated
```

For v1, JSON columns are acceptable for compatibility with current UpgradeAll model. Normalize only when there is a real query/index need.

### 5.2 Deterministic IDs for migrated records

Do not assign random app IDs during legacy migration.

Use deterministic IDs:

```text
new_app_id = UUIDv5(UPGRADEALL_LEGACY_NAMESPACE, "room-app:{legacy_room_id}:{canonical_app_id_json}")
new_extra_app_id = UUIDv5(UPGRADEALL_LEGACY_NAMESPACE, "room-extra-app:{legacy_room_id}:{canonical_app_id_json}")
```

Rationale:

- migration is repeatable;
- tests are deterministic;
- logs and support reports are stable;
- migration can be retried safely.

For hubs, preserve existing `uuid`.
For extra hubs, preserve existing text `id` (`GLOBAL` or hub UUID).

### 5.3 Canonical JSON

All maps/lists used as identity must be canonicalized before hashing:

- sort object keys;
- preserve null vs missing where semantically meaningful;
- remove blank values only if legacy behavior did so;
- no whitespace;
- UTF-8.

Write tests for canonicalization.

---

## 6. Legacy Android migration strategy

### 6.1 Supported source states

Support these startup cases:

1. Fresh Flutter install: no old Room DB, no getter DB.
2. Old Android UpgradeAll install with Room DB schema v6-v17.
3. Old Android UpgradeAll install with Room DB plus WAL/SHM files.
4. Intermediate alpha install with current getter JSONL store.
5. Partially completed previous migration attempt.
6. Failed migration with preserved backup.

### 6.2 Official Android upgrade invariant

Official upgrade can only read app-private old DB if:

- package name/applicationId remains `net.xzos.upgradeall`;
- signing key lineage permits app update;
- Android system treats it as the same app data directory.

If either changes, the plan must include a migration bridge release before the Flutter rewrite:

```text
old Kotlin UpgradeAll bridge release
  -> exports encrypted/signed migration bundle through SAF or app-private backup
  -> Flutter rewrite imports bundle on first launch
```

### 6.3 Use an Android-only legacy migrator module

Create a tiny Android library in the Flutter app, not a product logic dependency:

```text
apps/upgradeall_flutter/android/legacy_migrator/
  src/main/kotlin/net/xzos/upgradeall/legacy_migration/
    LegacyMetaDatabase.kt
    LegacyEntities.kt
    LegacyConverters.kt
    LegacyMigrations.kt
    LegacyExportBundle.kt
    LegacyMigrationRunner.kt
```

This module exists only to:

- open/copy old Room DB;
- apply existing Room migrations to v17;
- export a typed migration bundle;
- never serve runtime product logic.

Why not direct Rust import from every old schema only?

- The existing Room migration chain already encodes legacy quirks from v6-v17.
- Room exported schema docs and `room-testing` make migration verification possible.
- Implementing every old schema conversion directly in Rust would be more error-prone.

Long-term: after several major releases, this module can be removed only if the project formally drops direct migration from old Kotlin releases.

### 6.4 Migration flow

First Flutter Android launch:

```text
1. Flutter bootstrap calls Android LegacyMigrationRunner.checkNeeded().
2. If getter.db exists and migration_runs has completed legacy_room import, skip.
3. If old Room DB does not exist, create fresh getter.db.
4. If old Room DB exists:
   a. create migration session ID;
   b. copy app_metadata_database.db, -wal, -shm into private backup directory;
   c. copy same files into a working DB name, e.g. legacy_migration_work.db;
   d. open working DB with LegacyMetaDatabase + migrations 6->17;
   e. force checkpoint on working DB;
   f. export LegacyExportBundle v1;
   g. close Room DB;
   h. pass bundle path/hash to Rust getter-storage;
   i. Rust imports bundle into getter.db inside a transaction;
   j. Rust validates counts, canonical hashes, required fields;
   k. mark migration_runs completed;
   l. keep backup for at least N releases or until user explicitly deletes it.
```

Never delete old DB during the first successful migration. It can be ignored after success, but keep it for recovery.

### 6.5 Legacy export bundle

Use JSON for auditability initially. If size becomes an issue, add CBOR later.

```json
{
  "format": "upgradeall.legacy.room.export.v1",
  "source": {
    "database_name": "app_metadata_database.db",
    "room_schema_version": 17,
    "identity_hash": "...",
    "source_sha256": "...",
    "exported_at": 1234567890,
    "app_version_name": "...",
    "app_version_code": 105
  },
  "apps": [ ... ],
  "hubs": [ ... ],
  "extra_apps": [ ... ],
  "extra_hubs": [ ... ],
  "warnings": [ ... ]
}
```

Include all four legacy tables. Current `RustMigration.kt` omits `extra_app`; the new migration must not repeat that omission.

### 6.6 Mapping rules

Legacy `app` -> Rust `apps`:

- `name` -> `name`
- `app_id` JSON string -> canonical map -> `app_id_json`, `app_id_hash`
- `invalid_version_number_field_regex` -> same
- `include_version_number_field_regex` -> same
- `ignore_version_number` -> same
- `cloud_config` -> same JSON, validated against AppConfig DTO if possible
- `enable_hub_list` space-separated string -> ordered list JSON, while preserving original string if needed for compatibility
- `star` integer/null -> bool/null
- `id` long -> `legacy_room_id`
- new `id` -> deterministic UUIDv5

Legacy `hub` -> Rust `hubs`:

- preserve `uuid`
- `hub_config` -> same JSON, validate against HubConfig DTO
- `auth` -> auth JSON; do not log tokens
- `ignore_app_id_list` -> canonical list JSON
- `applications_mode` -> integer/bool semantic
- `user_ignore_app_id_list` -> canonical list JSON
- `sort_point` -> integer

Legacy `extra_app` -> Rust `extra_apps`:

- old `id` long -> `legacy_room_id`
- `app_id` -> canonical map/hash
- `mark_version_number` -> same
- new `id` -> deterministic UUIDv5

Legacy `extra_hub` -> Rust `extra_hubs`:

- preserve `id` (`GLOBAL` or hub UUID)
- `enable_global` -> bool/integer
- `url_replace_search` -> same
- `url_replace_string` -> same

### 6.7 Migration failure behavior

If migration fails:

- Do not create a partially usable app state.
- Show Migration Recovery page.
- Save:
  - migration session ID;
  - error code;
  - sanitized log;
  - backup path;
  - source DB hash;
  - failed phase.
- Offer actions:
  - retry migration;
  - export migration report;
  - start fresh only after explicit user confirmation;
  - open issue template with sanitized details.

No destructive fallback by default.

### 6.8 Migration tests

Create fixtures for at least:

- v6 database with sample app/hub.
- v8 database after major table rewrite.
- v10 database without unique app index.
- v13 database with `extra_app` table.
- v16 database with `extra_hub` but without `include_version_number_field_regex`.
- v17 database with all fields.
- DB with WAL/SHM uncheckpointed writes.
- DB with malformed optional JSON field.
- DB with auth token; verify logs redact it.
- Existing JSONL store; import to SQLite.
- Partial migration run; retry idempotently.

Commands:

```bash
just test-migration
cargo test -p getter-storage legacy_room
./gradlew :legacy_migrator:testDebugUnitTest   # Android side, if kept as Gradle module
flutter test test/migration_bootstrap_test.dart
```

---

## 7. Flutter UI pages and source customization

### 7.1 Page registry

Define page registry composition:

```dart
final pages = <UpgradeAllPage>[
  ...defaultPages,
  ...customPages,
];
```

Conflict rule:

- Custom page with same route ID overrides default page only if explicitly declared.
- Otherwise duplicate route IDs are compile/test failures.

### 7.2 Stable UI IDs

Create a single source of truth:

```dart
abstract final class UiIds {
  static const homePage = UiId('home.page');
  static const homeCheckUpdates = UiId('home.check_updates');
  static const homeOpenApps = UiId('home.open_apps');
  static const appListPage = UiId('app_list.page');
  static const appListItemPrefix = 'app_list.item.';
  static const appDetailPage = UiId('app_detail.page');
  static const migrationPage = UiId('migration.page');
  static const migrationRetry = UiId('migration.retry');
}
```

Every interactive widget must use semantic identifiers/labels through helper widgets:

```dart
Widget testableButton({
  required UiId id,
  required VoidCallback? onPressed,
  required Widget child,
}) {
  return Semantics(
    identifier: id.value,       // Flutter 3.19+ where available
    label: id.value,            // fallback for tools using labels
    button: true,
    child: ElevatedButton(
      key: ValueKey(id.value),
      onPressed: onPressed,
      child: child,
    ),
  );
}
```

Avoid localized visible text as the only selector.

### 7.3 Custom page guardrails

`AGENTS.md` and custom-page docs must instruct AI agents:

```text
Allowed to edit:
- packages/upgradeall_pages_custom/**
- custom theme files
- tests under packages/upgradeall_pages_custom/test/**

Do not edit unless explicitly requested:
- native/getter/**
- generated bindings
- platform adapters
- migration code
- app_shell bootstrap
- storage schema migrations
```

Never silence type errors with `dynamic`, unchecked casts, or broad `catch (_) {}`.

---

## 8. AI-friendly CLI workflow

Create one-command verification through `justfile`.

Example:

```make
setup:
    flutter doctor
    cargo --version
    rustup target list --installed

gen:
    cargo run -p getter-codegen
    flutter_rust_bridge_codegen generate

format:
    cargo fmt --all
    dart format apps packages tools

check:
    cargo clippy --workspace --all-targets -- -D warnings
    flutter analyze --fatal-infos

test:
    cargo test --workspace
    flutter test

test-migration:
    cargo test -p getter-storage legacy
    flutter test test/migration_bootstrap_test.dart

test-ui:
    flutter test integration_test

build-android-debug:
    flutter build apk --debug

e2e-android:
    maestro test e2e/maestro/android

verify: gen format check test test-migration build-android-debug
```

AI agents should usually run:

```bash
just verify
```

For page-only changes:

```bash
just format
just check
flutter test packages/upgradeall_pages_custom
just test-ui
```

---

## 9. UI testing plan

### 9.1 Test layers

Layer 1: Rust core tests

- provider fixtures;
- version comparison;
- update status;
- storage migrations;
- legacy import;
- downloader task state transitions;
- plugin permission validation.

Layer 2: Flutter widget tests

- page renders loading/empty/error/content states;
- page actions call typed fake `GetterClient`;
- custom page registry override works;
- semantics IDs exist.

Layer 3: Flutter integration tests

- app boots fresh;
- app boots after migration success;
- home -> app list -> app detail -> release list;
- renew all progress event updates UI;
- download task flow with fake backend.

Layer 4: Maestro black-box flows

- uses semantic IDs, not localized text;
- verifies app can be clicked by external automation;
- good for AI/manual click testing.

Layer 5: Patrol native automation, only where needed

- Android notification permission;
- file picker/SAF;
- install permission/system dialogs;
- notification tray interactions.

### 9.2 Required Maestro flows

```text
e2e/maestro/android/
  001_fresh_launch.yaml
  002_migration_success.yaml
  003_open_app_list.yaml
  004_open_app_detail.yaml
  005_renew_all.yaml
  006_download_task.yaml
  007_migration_failure_recovery.yaml
```

Every flow should prefer:

```yaml
- tapOn:
    id: home.check_updates
```

not:

```yaml
- tapOn: "Check updates"
```

### 9.3 Screenshot/visual tests

Use screenshots for regression, not as primary selectors.

- Golden tests for stable widgets.
- Mask dynamic data: time, progress, network text.
- Store baselines per theme/locale if needed.

---

## 10. Plugin and extension plan

### 10.1 Plugin layers

Separate:

1. Provider plugins: release source logic.
2. Downloader plugins: download backend logic.
3. UI configuration: declarative schemas rendered by Flutter/TUI.
4. Source-level page customizations: user-owned Flutter page modules.

Do not conflate runtime provider plugins with source-level UI customizations.

### 10.2 V1 plugins

V1 should support:

- built-in Rust providers;
- external JSON-RPC provider registration, continuing current concept;
- external JSON-RPC downloader registration;
- plugin manifest;
- config schema;
- permission declaration.

Example manifest:

```toml
id = "github"
kind = "provider"
version = "1.0.0"
api_version = "getter.plugin.v1"

[permissions]
network = ["api.github.com", "github.com"]
filesystem = false

[ui]
config_schema = "schemas/github-config.schema.json"
```

V2 can add Wasm/WASI sandbox plugins after the core rewrite stabilizes.

---

## 11. Implementation phases

### Phase 0: Freeze legacy baseline and document decisions

Objective: establish known-good source points before rewriting.

Tasks:

1. Tag current Android/Kotlin state in `UpgradeAll`, e.g. `legacy-android-room-v17-baseline`.
2. Tag current `getter` state before storage rewrite.
3. Create ADRs:
   - `docs/adr/0001-flutter-shell-rust-core.md`
   - `docs/adr/0002-rust-sqlite-storage.md`
   - `docs/adr/0003-source-level-page-customization.md`
   - `docs/adr/0004-legacy-room-migration.md`
4. Create `docs/architecture/target-architecture.md`.
5. Create `docs/ai-development.md` and root `AGENTS.md`.

Verification:

```bash
git status --short
```

Expected: only docs/plan changes in planning stage; no code changes until execution begins.

### Phase 1: Refactor `getter` into a Rust workspace

Objective: isolate core logic before Flutter integration.

Tasks:

1. Create Cargo workspace.
2. Move storage code into `getter-storage`.
3. Move provider code into `getter-providers`.
4. Move manager/version/update logic into `getter-core`.
5. Move downloader code into `getter-downloader`.
6. Move JSON-RPC into `getter-rpc`.
7. Add `getter-cli` with minimal commands.
8. Add `getter-ffi` facade crate.

Verification:

```bash
cargo fmt --all --check
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
```

Acceptance:

- No Android/JNI dependency in `getter-core`.
- CLI can initialize storage and list empty apps/hubs.
- Existing provider fixture tests still pass.

### Phase 2: Replace JSONL storage with Rust SQLite

Objective: create migration-capable storage foundation.

Tasks:

1. Add `getter-storage` SQLite backend.
2. Add embedded migrations.
3. Add schema metadata table.
4. Add models for apps, hubs, extra apps, extra hubs.
5. Add JSONL importer for existing alpha data.
6. Keep JSONL reader as compatibility-only module.
7. Update managers to use storage trait rather than direct JSONL store.

Verification:

```bash
cargo test -p getter-storage
cargo test -p getter-core
```

Acceptance:

- Fresh `getter.db` creates schema v1.
- JSONL import test passes.
- Re-running import is idempotent.
- Storage transaction tests pass.

### Phase 3: Build Android legacy Room export module

Objective: support old installed UpgradeAll users.

Tasks:

1. Create Android legacy migrator module under Flutter Android host.
2. Copy/minimize legacy Room entities, converters, and migrations v6-v17.
3. Add legacy DB work-copy logic.
4. Add checkpoint logic for WAL/SHM.
5. Export `LegacyExportBundle` containing apps, hubs, extra apps, extra hubs.
6. Redact sensitive auth tokens in logs.
7. Add migration status/error DTOs for Flutter.

Verification:

```bash
./gradlew :legacy_migrator:testDebugUnitTest
```

Acceptance:

- Can open sample v17 DB and export all four tables.
- Can open older fixture DB and migrate/export to v17 bundle.
- ExtraApp is included.
- Auth fields are present in bundle but redacted in logs.

### Phase 4: Implement Rust legacy import

Objective: import legacy Room export bundle into Rust SQLite.

Tasks:

1. Define `LegacyRoomExportBundle` Rust DTO.
2. Validate bundle format/version/hash.
3. Canonicalize app IDs and app ID lists.
4. Generate deterministic IDs.
5. Import apps/hubs/extra apps/extra hubs in one transaction.
6. Record migration run.
7. Add rollback/failed migration reporting.

Verification:

```bash
cargo test -p getter-storage legacy_room_import
```

Acceptance:

- v17 export imports into `getter.db`.
- v6-v17 fixture exports import correctly.
- Count and field parity tests pass.
- Re-import same bundle does not duplicate records.
- Failed import leaves no partial DB state.

### Phase 5: Create Flutter app shell

Objective: minimal Flutter app booting against `getter`.

Tasks:

1. Create `apps/upgradeall_flutter`.
2. Preserve Android `applicationId = net.xzos.upgradeall`.
3. Add `native/getter` checkout/submodule.
4. Add `flutter_rust_bridge` or selected FFI generator.
5. Generate minimal Dart bindings.
6. Implement `bootstrap.dart`:
   - platform paths;
   - legacy migration check;
   - getter init;
   - error handling.
7. Implement basic AppShell and route host.

Verification:

```bash
flutter analyze --fatal-infos
flutter test
flutter build apk --debug
```

Acceptance:

- Fresh app launches to Home page.
- `getter` initializes.
- No domain logic in Flutter shell.

### Phase 6: Implement page contracts and default pages

Objective: make page customization safe and typed.

Tasks:

1. Create `upgradeall_ui_contract`.
2. Create `upgradeall_ui_kit`.
3. Create `upgradeall_pages_default`.
4. Create `upgradeall_pages_custom` skeleton.
5. Add `UiIds` constants.
6. Add semantic/testable widget wrappers.
7. Implement default pages:
   - Home.
   - App list.
   - App detail.
   - Hub manager.
   - Discover/cloud config.
   - Download tasks.
   - Settings.
   - Migration status.

Verification:

```bash
flutter analyze --fatal-infos
flutter test packages/upgradeall_ui_kit
flutter test packages/upgradeall_pages_default
```

Acceptance:

- Default pages compile only through `ui_contract` and `getter` client.
- Custom package can override a route.
- Widget tests verify semantic IDs.

### Phase 7: Implement feature parity through getter API

Objective: migrate current UpgradeAll flows to Rust-backed Flutter UI.

Feature slices:

1. App list and status.
2. App detail and release list.
3. Renew all / renew one.
4. Hub manager and auth editing.
5. Cloud config discover/apply.
6. Download info and download tasks.
7. URL replacement and extra hub settings.
8. Extra app mark version.
9. Settings and logs.
10. Android platform installed app scanning.
11. Android installer adapter.
12. Backup/export/import if still required.

For each slice:

- Write Rust core tests first.
- Add/extend FFI DTO.
- Add fake `GetterClient` for Flutter tests.
- Implement UI page.
- Add widget test.
- Add integration/Maestro flow if user-visible.

Verification:

```bash
just verify
just e2e-android
```

Acceptance:

- Core flow works without Flutter through `getter-cli`.
- Flutter UI only renders/calls commands.

### Phase 8: Migration end-to-end testing on Android

Objective: prove real upgrade path.

Tasks:

1. Build old legacy APK with test fixture data.
2. Install old APK on emulator.
3. Seed app/hub/extra data.
4. Upgrade in-place to Flutter APK with same applicationId/signing.
5. Verify migration screen.
6. Verify data appears in Flutter UI.
7. Verify `getter.db` has imported records.
8. Verify old DB backup exists.
9. Repeat for v6/v8/v13/v16/v17 fixtures.

Commands:

```bash
just build-legacy-fixture-apk
just install-legacy-fixture
just seed-legacy-db-v17
just build-android-debug
just upgrade-to-flutter-debug
just e2e-migration-android
```

Acceptance:

- No data loss for apps/hubs/extra apps/extra hubs.
- WAL/SHM fixture migrates.
- Failed migration shows recovery page, not crash.
- Migration report is exportable and sanitized.

### Phase 9: CLI/TUI proof

Objective: prove `getter` is truly headless.

CLI commands:

```text
getter init
getter app list
getter app detail <id>
getter app renew <id>
getter renew-all
getter hub list
getter hub save <file>
getter download submit <app-id> <asset-id>
getter task list
getter plugin list
getter plugin register <manifest>
getter legacy import-room-bundle <bundle.json>
```

Verification:

```bash
cargo run -p getter-cli -- app list
cargo run -p getter-cli -- legacy import-room-bundle fixtures/legacy-room/v17/export.json
```

Acceptance:

- Main update check and migration import can run without Flutter.

### Phase 10: Release strategy

Objective: minimize risk for existing users.

Stages:

1. Internal migration test builds.
2. Public alpha with manual export/import only.
3. Beta with automatic Room migration but opt-in.
4. Release candidate with automatic migration by default.
5. Stable Flutter release.

Release rules:

- Same applicationId/signing for official Android upgrade.
- No destructive migration fallback.
- Keep old DB backup for at least two stable releases.
- Keep legacy migrator for enough versions to cover direct upgrades from last Kotlin release.
- Publish migration known-issues doc.

---

## 12. Validation matrix

Rust:

```bash
cargo fmt --all --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
```

Flutter:

```bash
flutter analyze --fatal-infos
flutter test
flutter test integration_test
flutter build apk --debug
```

Android legacy migration:

```bash
./gradlew :legacy_migrator:testDebugUnitTest
just e2e-migration-android
```

Maestro:

```bash
maestro test e2e/maestro/android
```

Patrol, only for native OS flows:

```bash
patrol test -t integration_test/native_permissions_test.dart
```

Migration invariants:

- Every legacy app row maps to exactly one Rust app row.
- Every legacy hub row maps to exactly one Rust hub row.
- Every legacy extra_app row maps to exactly one Rust extra_app row.
- Every legacy extra_hub row maps to exactly one Rust extra_hub row.
- Auth values are preserved in storage, redacted in logs.
- Migration is idempotent.
- Failed migration is recoverable.
- Old DB backup is kept.

UI/testability invariants:

- Every route has a stable route ID.
- Every primary action has a stable UI ID.
- No Maestro flow relies only on localized text.
- Custom pages compile against `ui_contract` only.
- Generated bindings are not manually edited.

---

## 13. Risks and mitigations

Risk: Flutter rewrite loses access to old app-private DB.

- Mitigation: keep same applicationId and signing key. If not possible, ship bridge export release.

Risk: current Room -> Rust migration misses data.

- Mitigation: replace `RustMigration.kt` approach with explicit export bundle including all four tables; add fixture tests for `extra_app`.

Risk: JSONL storage cannot support long-term schema evolution.

- Mitigation: move to Rust SQLite before official Flutter release; keep JSONL importer only for alpha compatibility.

Risk: AI/user custom pages create merge conflicts.

- Mitigation: stable `ui_contract`, `ui_kit`, and downstream-owned `pages_custom`; upstream avoids touching custom package.

Risk: AI UI tests become brittle.

- Mitigation: semantic identifiers/test IDs, Maestro flows by ID, widget tests by `ValueKey`, screenshot tests only for visual regression.

Risk: generated FFI code becomes confusing to AI.

- Mitigation: `AGENTS.md` says never edit generated bindings; run `just gen`.

Risk: platform-specific Android features leak into core.

- Mitigation: define `PlatformServices` / Rust platform callback traits; keep PackageManager, installer, notifications, SAF in Flutter Android platform adapter.

Risk: migration failure bricks startup.

- Mitigation: migration recovery page, retry, backup, sanitized report, explicit fresh-start option only.

---

## 14. Open questions to settle before execution

1. Are official Flutter Android builds guaranteed to keep `applicationId = net.xzos.upgradeall` and signing key lineage?
   - Recommended answer: yes, required for direct migration.

2. Should `getter` use Rust SQLite immediately, or first keep current JSONL and migrate later?
   - Recommended answer: Rust SQLite before official Flutter release. JSONL only as alpha compatibility import.

3. How long should the legacy Room migrator remain in the Flutter app?
   - Recommended answer: at least two stable release cycles, or until analytics/support indicates old Kotlin direct upgrades are negligible.

4. What is the minimum old DB schema version to support?
   - Recommended answer: support v6-v17 because current code has migrations from v6; below v6 requires manual bridge export or unsupported warning.

5. Should the first Flutter release include desktop targets?
   - Recommended answer: use Linux desktop as a development/test target, but Android is the official migration target first.

6. Should user custom pages be tracked in upstream?
   - Recommended answer: upstream provides skeleton and examples; after initial skeleton, upstream avoids changes in `pages_custom` except major contract migration.

7. Should plugin runtime use Wasm in v1?
   - Recommended answer: no. Use built-in Rust + external JSON-RPC first; add Wasm after core storage/migration/UI stabilizes.

---

## 15. First execution batch recommendation

Do not start by writing Flutter screens.

Start with this order:

1. ADRs + AGENTS.md + justfile skeleton.
2. `getter` workspace split.
3. Rust SQLite storage and migration framework.
4. Legacy Room export/import tests.
5. Minimal Flutter app shell + getter init.
6. Migration status page.
7. Home/AppList feature slice.

Reason: if migration and headless core are wrong, Flutter page work will hide architectural mistakes.

First concrete task after approval:

```text
Create ADRs and an executable repo verification skeleton:
- docs/adr/0001-flutter-shell-rust-core.md
- docs/adr/0002-rust-sqlite-storage.md
- docs/adr/0003-source-level-page-customization.md
- docs/adr/0004-legacy-room-migration.md
- AGENTS.md
- justfile
```

Then run:

```bash
just verify
```

Expected initially: verify may only check available existing pieces, but it becomes the single AI/operator entrypoint for the rest of the rewrite.
