# UpgradeAll rewrite next-step audit and plan

Date: 2026-06-22 14:36 CST
Repo: `DUpdateSystem/UpgradeAll`
Branch checked: `rewrite/flutter-getter-spine`
Superproject HEAD checked before this document: `80e1eb60 fix(app): use Flutter-compatible AGP`
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

The important caveat is that the branch is not merge-ready yet because UpgradeAll rewrite validation CI is red. The immediate blocker is Flutter/Kotlin Gradle Plugin compatibility, not a design issue.

The second caveat is process discipline: the Flutter shell has been created, but it must stay a shell until the real getter bridge is designed and wired. Do not add more product UI behavior that duplicates getter logic.

## 2. Current state evidence

### Superproject

```text
branch: rewrite/flutter-getter-spine
HEAD:   80e1eb60bed05eaac7875c477d377ed046111f19
PR:     https://github.com/DUpdateSystem/UpgradeAll/pull/514
status before this todo.md: only untracked todo-next-step.md
```

Recent superproject commits:

```text
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

At review time:

```text
Android CI: success
UpgradeAll Rewrite Validation: failure
```

Latest failure in rewrite validation:

```text
Error: Your project's Kotlin version (1.9.22) is lower than Flutter's minimum supported version of 2.0.0. Please upgrade your Kotlin version.
```

Relevant files:

```text
app_flutter/android/build.gradle
app_flutter/android/settings.gradle
app_flutter/android/app/build.gradle
```

Current Kotlin source:

```groovy
// app_flutter/android/build.gradle
ext.kotlin_version = '1.9.22'
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
| Verification | `just verify` should be the main gate | `just verify` exists and is used by workflow | Aligned, but currently red in CI |

## 4. Deviations / risks to control

### 4.1 CI is red due Kotlin Gradle Plugin

This is the immediate blocker. Do not continue feature work before making rewrite validation green.

Current issue:

```text
app_flutter/android/build.gradle: ext.kotlin_version = '1.9.22'
Flutter stable in CI requires Kotlin >= 2.0.0
```

This is not an architecture deviation. It is a build compatibility issue.

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

### 4.5 Plain non-interactive SSH shell did not expose Flutter

From this review shell, `flutter` was not found when running a simple SSH command. CI installs Flutter and previous local validation may have used a different shell/toolchain environment.

Before claiming local validation on genx, the next agent must either:

1. run from the environment where Flutter is actually on PATH, or
2. locate/source the Flutter installation explicitly, or
3. rely on GitHub Actions and report that local Flutter was unavailable.

Do not claim `just verify` passed locally unless the command actually ran in the current environment.

## 5. Immediate next plan: make rewrite validation CI green

This must be the next implementation task.

### Objective

Make `UpgradeAll Rewrite Validation` pass for PR #514 without changing architecture or adding feature scope.

### Files likely touched

```text
app_flutter/android/build.gradle
app_flutter/android/settings.gradle
app_flutter/android/app/build.gradle
```

### Step 1: try the minimal Kotlin fix first

Edit:

```diff
// app_flutter/android/build.gradle
- ext.kotlin_version = '1.9.22'
+ ext.kotlin_version = '2.0.0'
```

Do not change AGP or Gradle wrapper in the same commit unless the Kotlin-only fix fails. Keeping the diff small makes the failure mode obvious.

### Step 2: run focused local checks

Preferred local commands:

```bash
cd ~/Code/DUpdateSystem/UpgradeAll
cd app_flutter
flutter build apk --debug
cd ..
just verify
```

If `flutter` is not on PATH in the current SSH shell, first locate or source the Flutter environment. If that is not practical, push the minimal change and use GitHub Actions as the verification source, but report that local Flutter was unavailable.

Do not commit a workaround that uses:

```text
--android-skip-build-dependency-validation
```

That flag is diagnostic only, not the real fix.

### Step 3: if minimal fix fails, modernize Kotlin plugin declaration

If Flutter/Gradle still complains after `ext.kotlin_version = '2.0.0'`, switch to the modern plugin DSL.

Likely shape:

```groovy
// app_flutter/android/settings.gradle
plugins {
    id "dev.flutter.flutter-plugin-loader" version "1.0.0"
    id "com.android.application" version "8.6.0" apply false
    id "org.jetbrains.kotlin.android" version "2.0.0" apply false
}
```

Then update:

```diff
// app_flutter/android/app/build.gradle
 plugins {
     id "com.android.application"
-    id "kotlin-android"
+    id "org.jetbrains.kotlin.android"
     id "dev.flutter.flutter-gradle-plugin"
 }
```

If this works, remove obsolete top-level `buildscript` Kotlin classpath only after verifying the Flutter template still builds.

### Step 4: commit and push the CI fix

```bash
git status --short --branch --untracked-files=all
git add app_flutter/android/build.gradle app_flutter/android/settings.gradle app_flutter/android/app/build.gradle
git commit --no-gpg-sign -m "fix(app): use Flutter-compatible Kotlin plugin"
git push
```

### Step 5: watch CI

```bash
gh run list --repo DUpdateSystem/UpgradeAll --branch rewrite/flutter-getter-spine --limit 10
gh pr checks 514 --repo DUpdateSystem/UpgradeAll
```

If PR checks still do not attach automatically, manually dispatch both branch workflows:

```bash
gh workflow run upgradeall-rewrite-validation.yml --repo DUpdateSystem/UpgradeAll --ref rewrite/flutter-getter-spine
gh workflow run android.yml --repo DUpdateSystem/UpgradeAll --ref rewrite/flutter-getter-spine
```

Acceptance:

```text
Android CI: success
UpgradeAll Rewrite Validation: success
Getter PR #54 checks: still green
```

## 6. After CI is green: PR stabilization checklist

Do this before any new feature work.

### 6.1 Confirm submodule integrity

```bash
git ls-files -s core-getter/src/main/rust/getter
git submodule status core-getter/src/main/rust/getter
```

Expected:

```text
core-getter/src/main/rust/getter remains mode 160000
submodule points to getter branch commit 3b7613d or later pushed getter commit
```

### 6.2 Clean or consciously leave local notes

Current local note:

```text
?? todo-next-step.md
```

Decide explicitly:

- keep it untracked as scratch, or
- delete it, or
- replace it with this committed `todo.md`.

Do not accidentally include machine-local scratch files in feature commits.

### 6.3 Update PR descriptions

UpgradeAll PR #514 should say clearly:

- this is a rewrite spine, not a product-complete release
- docs/ADR/AGENTS were added
- getter is a submodule and points to getter PR #54
- Flutter shell is intentionally fake-adapter only
- current validation commands
- known deferred work: real bridge, direct Room migration, local_autogen, provider/downloader/update lifecycle

Getter PR #54 should say clearly:

- package-centric CLI/core rewrite
- old hub-app model is not coming back
- old provider/downloader/RPC behavior is deferred, not silently retained
- CI is green except skipped optional SARIF

## 7. Next architecture gate: real Flutter-to-getter bridge

Do not add more fake Flutter product screens before this gate.

### Objective

Define and implement the first real data path from Flutter shell to getter without moving product logic into Dart.

### New doc / ADR to add

```text
docs/architecture/adr/0007-flutter-getter-bridge-contract.md
```

This ADR should decide:

1. short-term bridge for development and tests
2. Android production bridge path
3. whether the JSON envelope used by CLI is also the app bridge contract
4. error model and event model
5. how Flutter gets paged snapshots and event deltas
6. which APIs are forbidden in Flutter UI code

Recommended default direction:

- Use getter-owned DTOs and JSON envelopes as the stable behavior contract.
- Keep CLI as the headless test oracle.
- For in-app Flutter, prefer a direct generated/native bridge only after the DTO contract is stable.
- Local RPC remains acceptable for debug/external plugins, but do not force every mobile UI call through a heavyweight JSON-RPC server unless an ADR accepts the lifecycle cost.

### First bridge API surface

Start with read-only snapshot APIs. Do not start with downloads/installers.

Minimum getter-facing operations:

```text
initialize(data_dir)
list_repositories()
list_tracked_packages()
evaluate_package(package_id, repo_id?)
read_migration_reports()
```

Minimum Flutter-facing DTOs:

```text
GetterSnapshot
AppSummary
RepositorySummary
MigrationReportSummary
GetterError
```

### Files likely touched

```text
app_flutter/lib/main.dart                 # split only if needed
app_flutter/lib/getter_adapter.dart       # new adapter interface / DTOs
app_flutter/lib/fake_getter_adapter.dart  # keep fake test adapter separate
app_flutter/test/widget_test.dart
core-getter/src/main/rust/getter/crates/getter-cli/src/lib.rs
core-getter/src/main/rust/getter/crates/getter-ffi/src/lib.rs or future bridge crate
```

### Acceptance

- Flutter tests can still run with fake adapter.
- A separate integration/dev test exercises a real getter data directory and returns real repository/app state.
- No repository resolution, update selection, migration mapping, or storage decision is implemented in Dart.
- Docs name the bridge decision and its limitations.

## 8. Next product phases after bridge

### Phase A: direct legacy Room migration

Goal: replace bridge-only JSON import with the Android upgrade path.

Tasks:

1. Android migrator copies old DB plus `-wal` and `-shm` safely.
2. Opens/canonicalizes old Room schema to latest supported legacy version.
3. Exports a typed bundle including all durable tables:
   - `app`
   - `hub`
   - `extra_app`
   - `extra_hub`
4. Rust imports the bundle into `main.db` in one transaction.
5. Migration record prevents rerun.
6. Report is sanitized and visible in Flutter migration page.

Acceptance:

- Fixtures for fresh install, supported old DB, WAL/SHM pending writes, malformed optional JSON, partial prior migration.
- Per-app failures become warnings; global unreadable DB becomes recovery state, not crash.
- Dropped fields are documented in `docs/migration/legacy-room-mapping.md`.

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
