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

The second accepted API surface adds read-only task lifecycle DTO consumption for the already accepted offline/fake getter lifecycle:

```text
listDownloadTasks()
listTaskEvents(after, limit)
```

The third accepted API surface adds the first legacy migration action boundary:

```text
importLegacyRoomDatabase(databasePath)
```

The Android platform adapter may prepare a copied/checkpointed legacy Room SQLite file and return its path to Flutter, but getter still owns the actual `legacy import-room-db` import semantics. Flutter starts the flow and renders getter reports; it must not inspect or map Room tables directly.

The fourth accepted API surface adds the production installed-autogen bridge boundary and must follow ADR-0009's Rust-active platform adapter direction rather than a Flutter-led inventory scan:

```text
previewInstalledAutogen(scanOptions)
applyInstalledAutogen(preview, acceptedPackages)
```

The Android product APK packages a slim `:getter_bridge` library under `app_flutter/android/getter_bridge`. It builds the Rust `api_proxy` cdylib and includes only the no-UI native bridge / installed-inventory provider classes needed by the Flutter product path, avoiding the legacy native `:app` UI and old `GetterPort` hub/RPC wrapper surface. `MainActivity` exposes a no-UI `net.xzos.upgradeall/getter_bridge` MethodChannel that derives the app-private getter data directory and forwards preview/apply requests to JNI entrypoints returning getter-style JSON envelopes.

Internally, Rust/native bridge code scans Android inventory through the platform adapter, then asks getter-owned shared autogen operations to plan/apply `local_autogen`. `MethodChannelGetterAdapter` consumes the returned getter-style JSON envelopes. Flutter renders getter-owned preview/apply DTOs and scan diagnostics, then passes the package ids from displayed accepted preview candidates back to getter on apply; it must not expose a product Dart `InstalledInventoryPlatform` scanner or convert Android package names into package ids.

`loadSnapshot()` composes the smaller getter-owned operations into the UI shell's first snapshot DTO. It must not perform repository resolution, version comparison, migration mapping, or update selection in Dart. `readMigrationReports()` must go through a getter operation such as `legacy report-list`; Flutter must not inspect getter's data-directory layout directly. `listDownloadTasks()` and `listTaskEvents()` render getter-owned task/event DTOs; Flutter must not synthesize task states, retry policy, installer behavior, or update decisions.

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
DownloadTaskSummary
TaskEventPage
TaskEventSummary
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

The first bridge slice is snapshot-only. Streaming events, progress, cancellation, backpressure, foreground services, notification lifecycle, and installer handoff are explicitly deferred to the update/download/install lifecycle ADR/work.

The first Phase D lifecycle slice defines getter-owned task/event/handoff DTOs through the CLI only: task state is persisted in getter `main.db`, task events are pollable with `after` cursor plus `limit`, and fake executor progress is command-driven rather than background-streamed. This pollable CLI/dev contract is not the final native stream API. Flutter should not maintain its own task state machine; future Flutter/bridge work must render getter task/event DTOs or ask getter for richer fields.

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

- The first bridge is executable in CI without waiting for full mobile FFI.
- CLI output remains the headless test oracle.
- Flutter can start consuming real getter data while preserving the Rust-owned domain boundary.
- Future native bridge work has a concrete DTO/error contract to preserve.

Costs:

- The CLI adapter is development/test infrastructure, not the final mobile path.
- Snapshot-only UI cannot yet represent long-running update/download/install flows.
- Getter output schemas must evolve carefully because they are now a cross-boundary contract.

## Validation

The first implementation slice must provide:

- Flutter widget tests that continue to use `FakeGetterAdapter`.
- Flutter widget tests for the migration flow using fake platform/getter adapters.
- A Flutter/Dart integration test that builds or receives a real `getter-cli` binary, initializes a real getter data directory, and reads repositories, tracked packages, package evaluation output, migration reports, direct Room import output, and task lifecycle DTOs through `CliGetterAdapter`.
- `just verify` coverage for the bridge integration test.

## Non-goals

- No full FFI/native bridge implementation beyond the first installed-autogen preview/apply JNI/MethodChannel operation slice.
- No update/download/install event stream.
- No Android-owned legacy Room mapping/import semantics; Android only prepares a copied DB file for getter.
- No product-complete Flutter UI.
- No product/domain decisions in Dart.
