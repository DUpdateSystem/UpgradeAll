# PLAN: Installed-app F-Droid autogen Flutter bridge/UI flow

> Status: completed and reviewed
> Branch: `work/flutter-installed-autogen-ui`
> Scope: one small functional slice for exposing cache-backed installed-app F-Droid autogen preview/apply through getter-owned bridge DTOs

## Boundary

Flutter renders getter-owned preview/apply DTOs, asks for user confirmation, and passes accepted package ids from displayed candidates back to getter.

Rust/native bridge owns Android installed inventory scanning, F-Droid catalog/cache lookup, repository coverage, generated package paths/content, diagnostics, and apply semantics. This slice must not add Dart/Kotlin provider parsing, package-id derivation, fixture bodies, endpoint URLs, cache-mode controls, live transport, downloader, or installer behavior to product UI.

## Implementation path

- [x] Add a typed product bridge method for installed F-Droid autogen preview that reuses Rust-active Android inventory scanning and calls getter-owned F-Droid autogen with the scanned installed inventory.
- [x] Add matching Dart/native adapter methods and MethodChannel/Kotlin plumbing; apply uses a typed installed-F-Droid bridge method while preserving getter-owned F-Droid apply semantics.
- [x] Extend the Installed Autogen Flutter page with a separate F-Droid installed preview/confirm/apply action while preserving the existing generic installed-autogen flow and bridge-unavailable state.
- [x] Add focused widget/adapter/Rust bridge tests that verify Flutter forwards only scan options/accepted package ids and renders getter DTOs.
- [x] Update existing architecture/app docs for the new typed bridge surface; no new ADR should be needed unless implementation reveals new product semantics.
- [x] Run changed-area validation and reviewer review of the functional branch.
- [x] Commit, merge into `rewrite/flutter-getter-spine`, push clean unsigned commits, and delete the small-plan branch.

## Validation and review

- `git diff --check`
- `cd app_flutter && dart format --set-exit-if-changed lib test`
- `cd app_flutter && flutter analyze`
- `cd app_flutter && flutter test`
- `cd app_flutter/android && ./gradlew --no-daemon :app:testDebugUnitTest --tests 'net.xzos.upgradeall.GetterBridgeRequestBuilderTest'`
- `cd core-getter/src/main/rust/api_proxy && cargo fmt --check && cargo test installed_fdroid_preview`
- `just verify-workspace-skeleton` with the local Android SDK/NDK environment exported.
- Reviewer run `7f996399-7e29-4a8c-8d66-aac95ebddfa6` found no blockers.

## Stop conditions

Stop for human collaboration only if this requires live provider transport, provider fixture payloads/cache controls in product Flutter requests, new F-Droid endpoint product semantics, or another architecture/ADR decision not already covered by ADR-0002/0007/0009/0012.
