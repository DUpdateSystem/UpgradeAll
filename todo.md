# UpgradeAll rewrite next-step audit and plan

Date: 2026-06-22 14:36 CST
Completion update: 2026-06-22 15:15 CST
Repo: `DUpdateSystem/UpgradeAll`
Branch checked: `rewrite/flutter-getter-spine`
Superproject HEAD checked before this document: `80e1eb60 fix(app): use Flutter-compatible AGP`
Superproject HEAD after completing the immediate CI fix: `a1c43f43 fix(app): use Flutter-compatible Kotlin plugin`
Getter submodule checked: `core-getter/src/main/rust/getter` -> `3b7613d709b405cb7229f2fbbf546c2d29ee96e6`

This document is the canonical next-step plan after reviewing:

- `todo-next-step.md`
- `AGENTS.md`
- `docs/README.md`
- `docs/implementation/coding-agent-handoff.md`
- `docs/architecture/upgradeall-getter-rewrite-wiki.md`
- `docs/architecture/adr/0001..0006`
- `docs/migration/legacy-room-mapping.md`
- `docs/app/flutter-ui-feature-parity-and-testing.md`
- current superproject diff/status/log
- current getter submodule diff/status/log
- current GitHub Actions state for UpgradeAll PR #514 and getter PR #54

## 1. Audit conclusion

There is no major architecture drift from the original rewrite plan.

Completion update: the immediate CI blocker described in this document has been fixed. The Kotlin Gradle Plugin was upgraded to `2.0.0`, the fix was pushed in `a1c43f43`, and both UpgradeAll PR checks are now green:

```text
Android CI / Build: success
UpgradeAll Rewrite Validation / Rewrite validation: success
```

The completed work is broadly aligned with the intended direction:

```text
Flutter shell / platform adapter only
  -> no product decisions in Flutter yet
Rust getter core
  -> product/domain/storage/repository/Lua/update/migration logic
Lua package repositories
  -> JSON-like Lua package tables validated by Rust
SQLite main.db + cache.db
  -> durable state split from rebuildable cache
```

The earlier caveat that the branch was not merge-ready because rewrite validation CI was red is now resolved. The remaining caveat is process discipline: the Flutter shell has been created, but it must stay a shell until the real getter bridge is designed and wired. Do not add more product UI behavior that duplicates getter logic.

## 2. Current state evidence

### Superproject

```text
branch: rewrite/flutter-getter-spine
HEAD after completing the immediate CI fix: a1c43f43505924ce55095d8f342d699d4d470a2a
PR:     https://github.com/DUpdateSystem/UpgradeAll/pull/514
status after cleanup should be clean except this document update until committed
```

Recent superproject commits:

```text
a1c43f43 fix(app): use Flutter-compatible Kotlin plugin
384aee6c docs: add rewrite next-step audit plan
80e1eb60 fix(app): use Flutter-compatible AGP
35e6c3d1 ci: restrict Telegram notifications to master pushes
3201d92d fix(app): use Flutter-compatible Gradle wrapper
59c1a0df fix(getter): keep Android proxy off Lua deps
a5730a98 ci: add rewrite validation workflow
4756f7c2 feat(getter): wire package-centric getter submodule
ae0d72c2 feat(app): add Flutter shell scaffold
95272873 chore: add rewrite agent guardrails
64611200 docs: add rewrite architecture records
```

### Getter submodule

```text
path:   core-getter/src/main/rust/getter
mode:   160000 gitlink, not vendored source
branch: rewrite/package-cli-spine
HEAD:   3b7613d709b405cb7229f2fbbf546c2d29ee96e6
PR:     https://github.com/DUpdateSystem/getter/pull/54
```

Submodule integrity evidence:

```bash
git ls-files -s core-getter/src/main/rust/getter
# expected/current: mode 160000 at 3b7613d709b405cb7229f2fbbf546c2d29ee96e6
```

Getter PR checks were green at review time:

```text
static-code-check: pass
test: pass
clippy-sarif: skipped as expected
```

### UpgradeAll CI state

At review time the state was:

```text
Android CI: success
UpgradeAll Rewrite Validation: failure
```

The failure in rewrite validation was:

```text
Error: Your project's Kotlin version (1.9.22) is lower than Flutter's minimum supported version of 2.0.0. Please upgrade your Kotlin version.
```

Completion update: the Kotlin compatibility fix was committed and pushed, and the current PR checks are now:

```text
Android CI / Build: success
UpgradeAll Rewrite Validation / Rewrite validation: success
```

Relevant files:

```text
app_flutter/android/build.gradle
app_flutter/android/settings.gradle
app_flutter/android/app/build.gradle
```

Current Kotlin source after the fix:

```groovy
// app_flutter/android/build.gradle
ext.kotlin_version = '2.0.0'
```

Also observed from the failed CI log:

```text
Flutter support for Gradle 8.7.0 will soon be dropped; future minimum likely 8.14.0.
Flutter support for Android Gradle Plugin 8.6.0 will soon be dropped; future minimum likely 8.11.1.
```

Do not jump to AGP 9 as part of the immediate fix unless the minimal Kotlin fix proves impossible. The current failure is KGP < 2.0.0.

## 3. Completed work vs original plan

| Area | Plan expectation | Current implementation | Judgment |
|---|---|---|---|
| Docs / ADR first | Architecture, ADRs, AGENTS, handoff before broad coding | Present under `docs/architecture/**`, `docs/implementation/**`, `AGENTS.md` | Aligned |
| Getter as reusable core | `core-getter/src/main/rust/getter` remains a real submodule | Restored `.gitmodules`; gitlink is `160000`; getter PR exists | Aligned |
| CLI before real UI | Getter must be exercisable headlessly before product UI | CLI commands exist; BDD CLI tests exist; Flutter is still fake shell | Mostly aligned |
| Package-centric model | Avoid reviving old hub-app model | `repo/package/app/storage/legacy` CLI nouns; `hub list` documented compatibility-only | Aligned |
| SQLite storage | Use main DB + cache DB, not JSONL product store | `MainDb` and `CacheDb` implemented; `init` creates `main.db` and `cache.db` | Aligned |
| Lua package repositories | Lua files return JSON-like tables; Rust validates | `getter-core/src/lua.rs` and repository loader implemented; hardened lib search path | Aligned |
| Legacy migration | Automatic migration eventually; initial slice may be JSON bridge | JSON bridge bundle import exists; direct Room reader deferred | Partial but acceptable |
| ExtraApp preservation | Do not repeat old bug of skipping `extra_app` state | Current mapping preserves `ignored_version` and `favorite` from extra app slice | Aligned for current slice |
| Flutter UI | Flutter owns UI/platform only | `FakeGetterAdapter`, route keys, placeholder pages; no real product logic | Acceptable shell; freeze scope until bridge |
| Mixed TDD/BDD | TDD for Rust/domain, BDD for user-facing/integration | Rust unit tests + CLI BDD + Flutter widget tests | Aligned |
| Verification | `just verify` should be the main gate | `just verify` exists, passes locally, and passes in the rewrite validation workflow | Aligned |

## 4. Deviations / risks to control

### 4.1 Resolved Kotlin Gradle Plugin CI blocker

The immediate CI blocker has been resolved. The fix was intentionally minimal:

```text
app_flutter/android/build.gradle: ext.kotlin_version = '2.0.0'
```

This clears Flutter stable's Kotlin >= 2.0.0 dependency validation without changing architecture or feature scope.

### 4.2 Flutter shell exists before real bridge

This is acceptable only because it is still a shell:

- fake in-memory getter adapter
- stable route/action/state keys
- no repository/update/storage decisions in Dart
- placeholders for downloads/logs/settings/migration

Risk: if future work keeps adding screens using fake data, the project will drift into UI-first implementation and violate the original plan.

Rule: after CI is green, the next product step must be bridge contract + real getter-backed data, not more fake UI.

### 4.3 Getter rewrite is large and destructive by diff size

Getter branch replaces a lot of old code:

```text
getter diff vs master: ~4.7k insertions, ~14k deletions
```

This is acceptable for a rewrite branch, but PR review must explicitly call out deferred old capabilities:

- downloader runtime
- provider implementations
- old RPC surface
- old websdk/cloud config machinery
- full migration/import/export

Do not describe this PR as product-complete.

### 4.4 Legacy migration is still a bridge slice, not full migration

Current implementation accepts a deterministic JSON bridge bundle and maps `apps[]` to getter tracked package state.

Still missing:

- direct Android Room DB reader/exporter
- complete `hub`, `extra_app`, `extra_hub` ingestion
- WAL/SHM-safe DB copy/checkpoint path
- idempotence and partial-failure recovery
- Flutter migration UX beyond placeholder

This is acceptable now, but must be called out in PR notes.

### 4.5 Validation environment note

The Kotlin fix was validated in the active agent environment with:

```text
cd app_flutter && flutter build apk --debug
just verify
./gradlew --no-daemon ':core-getter:buildDebugApi_proxyRust[armeabi-v7a]'
```

CI also validated the branch with Java 21 and the current Flutter stable action. Future agents should still report the actual local toolchain used when claiming local validation, because Flutter stable's minimum Gradle/AGP/Kotlin checks can move over time.

## 5. Completed immediate plan: make rewrite validation CI green

Status: completed in `a1c43f43 fix(app): use Flutter-compatible Kotlin plugin`.

What changed:

```diff
// app_flutter/android/build.gradle
- ext.kotlin_version = '1.9.22'
+ ext.kotlin_version = '2.0.0'
```

Why this was enough:

- The latest failing Rewrite Validation log reported only Flutter's Kotlin Gradle Plugin minimum-version gate.
- The existing Flutter Android template remained coherent with the minimal `buildscript` Kotlin classpath bump.
- No architecture, feature, AGP, or Gradle wrapper scope was expanded in this fix.

Validation completed after the fix:

```text
cd app_flutter && flutter build apk --debug
just verify
./gradlew --no-daemon ':core-getter:buildDebugApi_proxyRust[armeabi-v7a]'
```

GitHub Actions on PR #514 after the fix:

```text
Android CI / Build: success
UpgradeAll Rewrite Validation / Rewrite validation: success
```

No committed workaround uses `--android-skip-build-dependency-validation`.

## 6. Completed PR stabilization checklist

### 6.1 Submodule integrity confirmed

```text
160000 3b7613d709b405cb7229f2fbbf546c2d29ee96e6 0 core-getter/src/main/rust/getter
3b7613d709b405cb7229f2fbbf546c2d29ee96e6 core-getter/src/main/rust/getter (heads/rewrite/package-cli-spine)
```

The getter remains a real `160000` gitlink and is not vendored into the UpgradeAll superproject.

### 6.2 Local scratch notes cleaned

Temporary local scratch/review artifacts were removed after their useful content was folded into this tracked `todo.md`:

```text
todo-next-step.md
subagent-artifacts/review-kotlin-todo.md
```

### 6.3 PR descriptions updated

UpgradeAll PR #514 now states:

- this is a rewrite spine, not a product-complete release
- docs/ADR/AGENTS were added
- getter is a submodule and points to getter PR #54 / `3b7613d709b405cb7229f2fbbf546c2d29ee96e6`
- Flutter shell is intentionally fake-adapter only
- Gradle/AGP/Kotlin compatibility fixes are included
- current CI validation is green
- deferred work includes real bridge, direct Room migration, `local_autogen`, provider/downloader/update lifecycle

Getter PR #54 now states:

- package-centric CLI/core rewrite
- old hub-app model is not restored
- old provider/downloader/RPC behavior is deferred, not silently retained
- Android JNI/API proxy consumers can depend on getter without pulling Lua/domain dependencies
- checks are green except optional SARIF skip

## 7. Completed first architecture gate: Flutter-to-getter bridge contract

Status: first implementation slice completed after the CI fix.

What landed:

- Added `docs/architecture/adr/0007-flutter-getter-bridge-contract.md`.
- Added Flutter bridge DTO/interface file: `app_flutter/lib/getter_adapter.dart`.
- Split fake test adapter export: `app_flutter/lib/fake_getter_adapter.dart`.
- Added `CliGetterAdapter` in `app_flutter/lib/cli_getter_adapter.dart`.
- Added a real getter-backed Flutter dev test: `app_flutter/dev_test/cli_getter_adapter_test.dart`.
- Added `just test-flutter-getter-cli-integration` and included it in `just verify`.
- Added getter CLI `legacy report-list` so Flutter/test adapters consume sanitized migration reports through the getter JSON envelope instead of reading getter's data-directory layout directly.

Bridge direction accepted:

- `FakeGetterAdapter` remains for deterministic widget tests.
- `CliGetterAdapter` is a development/integration bridge and test oracle against `getter-cli`; it is not the final Android production path.
- Android production should still embed getter through a native/FFI-style bridge after DTOs stabilize.
- The shared `GetterAdapter` interface now exposes the first read-only bridge surface:
  - `initialize()`
  - `listRepositories()`
  - `listTrackedPackages()`
  - `evaluatePackage(packageId, repositoryId?)`
  - `readMigrationReports()`
  - `loadSnapshot()`

Validation completed:

```text
just verify
```

Result:

```text
getter unit/bin tests: pass
getter CLI BDD: 8 features, 9 scenarios, 65 steps passed
Flutter widget tests: pass
Flutter analyze: pass
Flutter getter CLI integration test: pass
Gradle project check: pass
Flutter Android debug APK build: pass
```

Important boundary note:

- Flutter parses getter envelopes and renders DTOs.
- Flutter still must not implement repository resolution, Lua validation/evaluation semantics, version comparison, migration mapping, provider/source selection, cache invalidation, or download task state machines.
- If Flutter needs richer state, extend getter output first and cover it with getter tests.

## 8. Next product phases after bridge

### Phase A: direct legacy Room migration

Goal: replace bridge-only JSON import with the Android upgrade path.

Status: first getter-owned direct DB slice completed. The getter CLI now supports `legacy import-room-db <db.sqlite>` for copied/checkpointed Room v17 SQLite files. It reads `app` and `extra_app`, maps known legacy app-id keys, writes `tracked_packages` plus `legacy-room-v17` in one transaction, prevents rerun, emits sanitized reports, and documents dropped hub/extra_hub fields. Android-side WAL/SHM copy/checkpoint and Flutter migration UX remain future work.

Completed tasks:

1. Rust direct importer opens copied legacy Room DB read-only and requires `PRAGMA user_version = 17`.
2. Rust reads durable `app` and `extra_app` fields needed for tracked package/user state.
3. Rust imports into `main.db` in one transaction.
4. Migration record prevents rerun.
5. Reports are sanitized and visible through `legacy report-list`.
6. Dropped `hub`/`extra_hub` fields are documented in `docs/migration/legacy-room-mapping.md`.

Remaining tasks:

1. Android migrator copies old DB plus `-wal` and `-shm` safely before invoking getter.
2. Android/platform adapter opens/checkpoints/canonicalizes old Room schema to latest supported legacy version.
3. Extend accepted mapping if future ADR accepts direct `hub`/`extra_hub` semantics.
4. Flutter migration page starts the adapter flow and renders getter reports.

Acceptance progress:

- Supported old DB fixture: done.
- Malformed/unsupported DB recovery reports: done.
- Partial prior migration/idempotence: done across direct DB and bridge bundle paths.
- Malformed optional JSON becomes warning: covered in Rust storage tests for `extra_app`.
- Mixed valid/invalid app rows import valid rows and warn: done.
- DBs with app rows but zero importable rows fail with recovery report: done.
- Report sanitization for dropped `hub`/`extra_hub` secrets and URL rewrite data: done.
- WAL/SHM pending writes: pending Android adapter slice.
- Per-app failures become warnings; global unreadable DB becomes recovery state, not crash: done for the getter-owned direct importer.

### Phase B: `local_autogen` generation

Goal: convert installed/legacy state into generated fallback Lua packages without mixing with user-authored overrides.

Rules:

```text
local        = user-authored, highest priority, never overwritten silently
local_autogen = generated fallback, safe to regenerate/clean after preview
```

Tasks:

1. Define autogen output path and deterministic package file naming.
2. Generate Lua package stubs for installed apps not covered by official/local repos.
3. Add preview report before writing.
4. Add cleanup preview for missing generated apps.
5. Add invalidation rules when installed apps or repo metadata changes.

Acceptance:

- BDD for preview/confirm/cancel cleanup UX.
- TDD for deterministic Lua generation and no overwrite of `local`.
- Yellow/free-network warning tagging remains getter-driven metadata, not hardcoded UI behavior.

### Phase C: repository tooling and diagnostics

Goal: make Lua package repositories maintainable.

Tasks:

1. Add `repo validate` command.
2. Add clearer package eval diagnostics with path/location.
3. Add schema docs for package lifecycle phases.
4. Add fixture repositories for success and common failure cases.
5. Add cache invalidation rules for repo changes.

Acceptance:

- `getter --data-dir <tmp> repo validate <path>` returns structured JSON.
- Invalid Lua/schema/domain errors point to file and field.
- No network is required for repository validation unless explicitly requested.

### Phase D: update/download/install lifecycle

Goal: move from static app/repo display to real update workflows.

Tasks:

1. Expand getter core update task model.
2. Implement provider/downloader crate behavior beyond placeholders.
3. Add event stream/backpressure model.
4. Add download task state and cancellation.
5. Add platform install handoff contract.
6. Add Flutter BDD for update/download user flows only after getter behavior exists.

Acceptance:

- CLI can run an offline fixture update check.
- Flutter displays getter events rather than calculating status itself.
- Android platform adapter owns permissions/notifications/installer handoff.

## 9. Do-not-do list for the next agent

- Do not add more fake product screens before fixing CI and defining the bridge.
- Do not move provider/update/storage/migration logic into Flutter.
- Do not vendor getter source into the UpgradeAll superproject.
- Do not revive old hub-app architecture; `hub list` is compatibility-only.
- Do not use random UUIDs as primary package identity.
- Do not claim migration complete while direct Room DB ingestion is missing.
- Do not bypass Flutter dependency validation as a committed workaround.
- Do not commit generated build outputs:
  - `target/`
  - `build/`
  - `.dart_tool/`
  - `.gradle/`
  - APK/AAB/SO/class/object outputs
  - `local.properties`
  - `.pi/`
  - `context-build/`

## 10. Quick commands for the next session

```bash
cd ~/Code/DUpdateSystem/UpgradeAll

git status --short --branch --untracked-files=all
git submodule status --recursive

gh run list --repo DUpdateSystem/UpgradeAll --branch rewrite/flutter-getter-spine --limit 10
gh pr checks 514 --repo DUpdateSystem/UpgradeAll
gh pr checks 54 --repo DUpdateSystem/getter

# after Kotlin fix
cd app_flutter && flutter build apk --debug
cd ..
just verify

# if CI needs manual dispatch
gh workflow run upgradeall-rewrite-validation.yml --repo DUpdateSystem/UpgradeAll --ref rewrite/flutter-getter-spine
gh workflow run android.yml --repo DUpdateSystem/UpgradeAll --ref rewrite/flutter-getter-spine
```
