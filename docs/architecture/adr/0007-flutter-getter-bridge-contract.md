# ADR-0007: Flutter / getter bridge contract

> Status: Draft / first implementation slice accepted
> Date: 2026-06-22
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

Flutter talks to getter through getter-owned DTOs and JSON envelopes. The initial bridge contract is read-only and snapshot-oriented so Flutter can display real getter state without copying product/domain logic into Dart.

The CLI JSON envelope from ADR-0006 is the first executable bridge oracle. It is used for development, integration/dev tests, and contract validation. Android production embedding still follows ADR-0002: the app embeds getter as a Rust library / native bridge rather than depending on a standalone long-lived getter daemon as the primary mobile path.

The first bridge implementation in Flutter therefore has two adapters:

- `FakeGetterAdapter` for deterministic widget tests and UI shell work.
- `CliGetterAdapter` for development/integration tests against a real getter data directory and the built `getter-cli` binary.

The CLI adapter is not the final Android production bridge. It exists to make the contract executable before the FFI/native bridge is stabilized.

## First bridge API surface

The first accepted API surface is intentionally read-only:

```text
initialize()
listRepositories()
listTrackedPackages()
evaluatePackage(packageId, repositoryId?)
readMigrationReports()
loadSnapshot()
```

The second accepted API surface adds the first legacy migration action boundary:

```text
importLegacyRoomDatabase(databasePath)
```

The Android platform adapter may prepare a copied/checkpointed legacy Room SQLite file and return its path to Flutter, but getter still owns the actual `legacy import-room-db` import semantics. The production Android bridge exposes `importLegacyRoomDatabase` and `legacyReportList` through JNI/MethodChannel by delegating to getter-owned `getter-operations` legacy Room code. Flutter starts the flow and renders getter reports; it must not inspect or map Room tables directly.

The third accepted API surface adds the production installed-autogen bridge boundary and must follow ADR-0009's Rust-active platform adapter direction rather than a Flutter-led inventory scan:

```text
previewInstalledAutogen(scanOptions)
applyInstalledAutogen(preview, acceptedPackages)
```

The Android product APK packages a slim `:getter_bridge` library under `app_flutter/android/getter_bridge`. It builds the Rust `api_proxy` cdylib and includes only the no-UI native bridge / installed-inventory provider classes needed by the Flutter product path, avoiding the legacy native `:app` UI and old `GetterPort` hub/RPC wrapper surface. `MainActivity` exposes a no-UI `net.xzos.upgradeall/getter_bridge` MethodChannel that derives the app-private getter data directory and forwards migration and installed-autogen requests to JNI entrypoints returning getter-style JSON envelopes.

Internally, Rust/native bridge code scans Android inventory through the platform adapter, then asks getter-owned shared autogen operations to plan/apply `local_autogen`. `MethodChannelGetterAdapter` consumes the returned getter-style JSON envelopes. Flutter renders getter-owned preview/apply DTOs and scan diagnostics, then passes the package ids from displayed accepted preview candidates back to getter on apply; it must not expose a product Dart `InstalledInventoryPlatform` scanner or convert Android package names into package ids.

`loadSnapshot()` composes smaller getter-owned read-model operations into the UI shell's first snapshot DTO. In the Android/native product path, `MethodChannelGetterAdapter` calls `readOperation` for `repository_list`, `tracked_package_list`, and `package_eval`; Rust getter reads SQLite repository/tracked-package state and evaluates registered Lua packages. Flutter may parse and combine those returned DTOs for rendering, but must not perform repository resolution, Lua validation/evaluation, version comparison, migration mapping, or update selection in Dart. `readMigrationReports()` must go through a getter operation such as `legacy report-list`; Flutter must not inspect getter's data-directory layout directly. Runtime task rendering must use ADR-0011 runtime task snapshot APIs and opaque action/task controls; Flutter must not synthesize task states, retry policy, installer behavior, or update decisions.

The first product click-through update flow is: App detail calls a typed getter update-check operation, receives a getter-issued `action_id`, submits only that `action_id` to the process-lifetime runtime, and opens Downloads to query authoritative task snapshots. Flutter may refresh the Downloads page after `RuntimeNotification.task_changed`, but the notification is only a trigger; `task_list`/equivalent runtime queries remain the source of truth.

## Flutter DTOs

The Flutter shell may use DTOs that mirror getter output for rendering:

```text
GetterSnapshot
AppSummary
RepositorySummary
TrackedPackageSummary
PackageEvaluation
MigrationReportSummary
LegacyMigrationImportResult
MigrationWarningSummary
MigrationSourceCounts
RuntimeUpdateCheckResult
RuntimePackageSummary
RuntimeUpdateSummary
RuntimeIssuedAction
RuntimeTaskSnapshot
RuntimeTaskPhase
RuntimeTaskProgress
RuntimeTaskCapabilities
RuntimeTaskDiagnostic
RuntimeNotificationEnvelope
GetterError
InstalledAutogenPreview
InstalledAutogenCandidate
InstalledAutogenSkip
InstalledAutogenScanStats
InstalledAutogenApplyResult
```

DTOs are a UI transport shape, not a new product model. Any field whose value requires domain interpretation must be supplied by getter or by a platform capability explicitly documented in a later ADR.

## JSON envelope contract

The CLI bridge consumes the ADR-0006 envelope shape:

```json
{
  "ok": true,
  "command": "repo list",
  "data": {},
  "warnings": []
}
```

and structured error envelopes:

```json
{
  "ok": false,
  "command": "package eval",
  "error": {
    "code": "package.eval_error",
    "message": "Getter package evaluation failed",
    "detail": "..."
  }
}
```

Flutter adapter code may parse and display these fields, but it must not infer missing domain state from them. If the UI needs a richer field, add it to getter output first and cover it with getter tests.

For installed-autogen flows, CLI/dev tests may continue to pass fixture inventory JSON to `getter autogen installed preview/apply`. The Android product bridge does not expose that fixture boundary as a Flutter-owned scanning API; it wraps scan + getter autogen planning behind a getter/native bridge operation.

## Error model

The bridge maps getter errors into `GetterError`:

- `code`: stable machine-readable getter/platform code.
- `message`: short user/log-facing message.
- `detail`: optional diagnostic detail.

Flutter may choose presentation, but the source classification belongs to getter or a documented platform adapter.

## Legacy migration platform adapter

Flutter owns the migration screen and user-visible flow. Android-native code exposes a no-UI platform adapter over `net.xzos.upgradeall/legacy_migration` with `prepareLegacyRoomImport`.

That adapter may:

- locate `app_metadata_database.db` in the app database directory;
- copy the SQLite triplet (`.db`, `-wal`, `-shm`) into an app-private getter-import path;
- checkpoint/canonicalize the copied database into a standalone SQLite file;
- return `{ found, database_path, message }` to Flutter.

That adapter must not:

- show Android-native UI;
- map legacy rows into package IDs;
- decide what fields are dropped/imported;
- write getter storage directly.

Flutter then calls a getter bridge operation equivalent to `legacy import-room-db <database_path>` and renders getter-owned reports.

## Event model

The initial bridge slice was snapshot-only. ADR-0011 supersedes the old persisted fake-task CLI scaffold for product task flow: runtime task state is process-memory only in the native getter singleton, `RuntimeNotification.task_changed` is pushed over the bridge, and current-state task query operations remain authoritative. The remaining `debug fake-task ...` CLI commands are development scaffolding, not a Flutter/product task API. CLI runtime task coverage uses `runtime script --script <script.json>`, which executes within one process and intentionally drops runtime task state after the command exits.

Flutter should not maintain its own task state machine; it renders getter-owned runtime task snapshots and invokes getter-owned task controls/update operations using opaque `action_id`s.

Android platform install remains a handoff boundary. Getter may request/record an abstract install handoff, but Android permissions, notifications, PackageInstaller/Shizuku/root execution, and path-versus-URI/SAF semantics belong to platform adapter work and remain outside this bridge slice.

## Android production bridge direction

The Android production path should embed getter through a native bridge once the DTO contract is stable. The native bridge should expose getter-owned operations and platform callbacks/capabilities; it should not force all in-app UI calls through a heavyweight local JSON-RPC server unless a future ADR accepts that lifecycle cost.

Local RPC remains acceptable for debug tooling, external integration, and development workflows.

## APIs forbidden in Flutter UI code

Flutter UI code must not implement:

- repository priority/overlay resolution
- Lua package validation or evaluation semantics
- version comparison/update selection
- legacy Room mapping decisions
- cache invalidation rules
- provider/source selection
- download task state machines
- package ID normalization beyond display-safe handling

If a feature requires one of these decisions, add or extend a getter operation instead.

## Consequences

Positive:

- The early bridge was executable in CI before the native bridge stabilized.
- CLI output remains a headless oracle for storage/repository/migration coverage.
- Flutter can consume real getter data while preserving the Rust-owned domain boundary.
- The native bridge now has a concrete DTO/error/runtime notification contract to preserve.

Costs:

- The CLI adapter is development/test infrastructure, not the final mobile path.
- Runtime task UI still exposes only the first read-only snapshot rendering slice until live provider/downloader/installer ADRs are accepted.
- Getter output schemas must evolve carefully because they are now a cross-boundary contract.

## Validation

The first implementation slice must provide:

- Flutter widget tests that continue to use `FakeGetterAdapter`.
- Flutter widget tests for the migration flow using fake platform/getter adapters.
- A Flutter/Dart integration test that builds or receives a real `getter-cli` binary, initializes a real getter data directory, and reads repositories, tracked packages, package evaluation output, migration reports, and direct Room import output through `CliGetterAdapter`.
- `just verify` coverage for the bridge integration test.

## Non-goals

- No product-complete live provider/downloader/installer execution beyond the ADR-0011 in-memory runtime operation and notification skeleton.
- No durable update/download/install event log or cross-process task recovery.
- No Android-owned legacy Room mapping/import semantics; Android only prepares a copied DB file for getter.
- No product-complete Flutter UI.
- No product/domain decisions in Dart.
