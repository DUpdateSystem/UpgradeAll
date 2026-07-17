# GitHub Android APK autogen product bridge plan

## Scope

Implement a narrow Flutter/native product entry for GitHub Android APK autogen.

The product bridge accepts only user-facing package-generation inputs:

- GitHub owner
- GitHub repository
- Android package name
- optional display name

Rust getter remains responsible for GitHub release transport, provider cache, provenance, generated package paths/files, repository coverage, diagnostics, and apply semantics. The bridge must not expose `releases_json`, endpoint/API-base controls, cache modes, raw provider payloads, fixture bodies, or live transport controls.

## Non-goals

- No GitHub search/catalog UI.
- No latest-commit package generation UI.
- No downloader/background worker/recovery/installer semantics beyond existing runtime flows.
- No Flutter/Dart/Kotlin provider parsing, cache policy, transport policy, package path derivation, Lua generation, or repository coverage decisions.

## TDD/BDD seams

1. `getter-operations::github_autogen` public preview seam
   - A request without `releases_json` refreshes through getter-owned GitHub release transport on cache miss.
   - Existing fixture-backed tests remain valid for CLI/dev usage.

2. `api_proxy` native product seam
   - Product request is `deny_unknown_fields` and rejects provider controls.
   - Cached provider data can drive preview/apply without live network in tests.

3. Android Kotlin request builder seam
   - `previewGithubAutogen` JSON carries only product fields and drops provider controls.

4. Dart `GetterAdapter` / `MethodChannelGetterAdapter` seam
   - MethodChannel call sends only typed product fields.
   - Apply uses a GitHub-specific typed bridge method and package acceptance.

5. Flutter widget seam
   - User can enter owner/repo/android package/display name, preview candidates, confirm apply, and the adapter receives only accepted package IDs.

## Validation checklist

- [x] Focused getter operation tests for `github_autogen`.
- [x] Focused api_proxy tests for GitHub autogen bridge.
- [x] Kotlin unit test for request builder.
- [x] Dart native adapter tests.
- [x] Flutter widget test for GitHub autogen flow.
- [x] `cargo fmt --check` from getter submodule and api_proxy as applicable.
- [x] `cargo test --workspace --features lua` from getter submodule if getter changes.
- [x] `cargo test --workspace --no-default-features` from getter submodule if getter changes.
- [x] api_proxy Cargo tests from `core-getter/src/main/rust/api_proxy`.
- [x] `flutter test` / focused tests from `app_flutter`.
- [x] Gradle Kotlin tests from `app_flutter/android`.
- [x] `just verify-workspace-skeleton` with Android NDK environment.
- [x] `git diff --check` in getter and superproject.
- [x] Reviewer pass for functional changes.

## Validation log

- `cargo test -p getter-operations github_autogen --features lua -- --nocapture` passed: 6 tests.
- `cargo test -p getter-cli --test bdd_cli -- --name 'User applies GitHub Android APK autogen'` passed: 1 scenario / 17 steps.
- `cargo test github_autogen_ -- --nocapture` from `core-getter/src/main/rust/api_proxy` passed: 2 focused tests.
- `flutter test test/native_getter_adapter_test.dart --plain-name 'native GitHub autogen uses typed product fields and apply method'` passed.
- `flutter test test/widget_test.dart --plain-name 'installed autogen route previews and applies GitHub getter DTOs'` passed.
- `./gradlew --quiet :app:testDebugUnitTest --tests 'net.xzos.upgradeall.GetterBridgeRequestBuilderTest.githubAutogenPreviewRequestCarriesOnlyProductFields'` passed from `app_flutter/android`.
- `cargo fmt --check`, `cargo test --workspace --features lua`, and `cargo test --workspace --no-default-features` passed from `core-getter/src/main/rust/getter`.
- `cargo fmt --check` and `cargo test` passed from `core-getter/src/main/rust/api_proxy`: 15 tests.
- `flutter test` passed from `app_flutter`: 34 tests.
- `./gradlew --quiet :app:testDebugUnitTest` passed from `app_flutter/android`.
- `just verify-workspace-skeleton` passed from the superproject with Android SDK/NDK environment exports.
- `git diff --check` passed in both superproject and getter submodule.
- Reviewer `/tmp/upgradeall-github-autogen-product-bridge-review.md` reported `Blocker: none found`; no follow-up code changes were required.

## Delivery checklist

- [ ] Commit getter submodule first if changed, unsigned (`%G? = N`).
- [ ] Commit superproject gitlink/app/docs/PLAN second, unsigned (`%G? = N`).
- [ ] Fast-forward merge getter work branch to `rewrite/package-cli-spine` and push.
- [ ] Fast-forward merge superproject work branch to `rewrite/flutter-getter-spine` and push.
- [ ] Wait for PR/CI checks to pass.
- [ ] Delete work branches.
- [ ] Confirm both working trees are clean.

## Scout note

Parallel scout run `1c1cf7de-1863-4708-92c3-1606559ed647` failed before producing usable artifacts: one child hit a connection error and the other exceeded context. The implementation proceeds from direct inspection of the same files named in the scout prompts.
