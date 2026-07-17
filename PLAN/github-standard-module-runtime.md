# GitHub standard module runtime TDD slice

## Scope

Implement the first GitHub standard Lua module product slice at the getter runtime/provider-backed update-check seam.

This slice is intentionally offline/cache-backed: tests use committed real GitHub API response snapshots from the old UpgradeAll GitHub repository behavior, not live GitHub API calls.

## Constraints

- Keep `core-getter/src/main/rust/getter` as a real git submodule; commit getter changes inside the submodule first, then update the superproject gitlink.
- Use unsigned commits (`--no-gpg-sign`) and verify `%G? = N`.
- Do not move GitHub provider parsing, cache policy, version selection, action issuance, diagnostics, or storage logic into Flutter/Dart/Kotlin.
- Do not add live GitHub HTTP, downloader/installer semantics, custom generated endpoints, GitHub autogen, or latest-commit product behavior in this slice.
- Runtime update-check request remains package-oriented: `package_id`, optional `repository_id`, optional `installed_version`, optional `pin_version`.
- Use committed GitHub API snapshots and provider cache seeding; no live GitHub calls in tests.
- Temporary context/analysis/review/note artifacts must be committed, converted to docs, or removed before final clean status.

## Evidence/read documents

Required bootstrap docs read in this session:

- `docs/README.md`
- `docs/architecture/upgradeall-getter-rewrite-wiki.md`
- ADRs 0001, 0002, 0003, 0004, 0005, 0006, 0007, 0012
- `docs/lua-api/repository-layout.md`
- `docs/lua-api/permissions.md`
- `docs/lua-api/templates.md`
- `docs/app/flutter-ui-feature-parity-and-testing.md`

## TDD plan

1. Inspect existing GitHub provider/runtime seam and legacy UpgradeAll GitHub reference data.
   - Current scaffold: `luaclass.github_android_apk` calls `getter.provider.github.release_candidates`.
   - Current runtime seam: `issue_action_from_registered_package_json` evaluates registered package Lua with provider host and uses cached provider facts.
   - Existing real snapshot: `tests/files/web/github_api_release.json` for `DUpdateSystem/UpgradeAll`, including tag `0.13-beta.4` and asset `UpgradeAll_0.13-beta.4.apk`.
   - Existing old normalized data: `tests/files/data/provider_github_release.json`.
   - Legacy cloud config GitHub hub used `api_keywords = ["owner", "repo"]` and UpgradeAll app URL `https://github.com/DUpdateSystem/UpgradeAll` with Android package `net.xzos.upgradeall`.
2. Red: add a getter runtime/provider-backed update-check test using the real `github_api_release.json` snapshot for `DUpdateSystem/UpgradeAll`.
   - Package Lua uses `luaclass.github_android_apk` with `owner = "DUpdateSystem"`, `repo = "UpgradeAll"`, `android_package = "net.xzos.upgradeall"`, `asset = { include = "[.]apk$" }`.
   - Provider cache is seeded from the committed snapshot with Manifest-compatible provenance.
   - Expected update selection: `0.13-beta.4` with artifact `UpgradeAll_0.13-beta.4.apk` and source `github`.
   - Assert getter-owned provider trace and issued action are produced from the runtime seam.
3. Green: implement only the minimal changes needed for that test.
   - Prefer updating the existing test harness/helpers over adding new product inputs.
   - If parsing/normalization already passes, treat the red failure as missing snapshot-backed runtime coverage and keep implementation changes minimal.
4. Refactor/document.
   - Add concise fixture provenance comments/docs if needed.
   - Update this plan checklist with validation/review/commit records.
5. Validate from the getter submodule, not superproject root.
6. Use a reviewer for functional changes before merge.
7. Commit getter submodule unsigned, update superproject gitlink/plan/docs, commit superproject unsigned, merge/push, delete work branches, and confirm clean statuses.

## Progress

- [x] Created superproject and getter branches: `work/github-standard-module-runtime`.
- [x] Created this plan artifact.
- [x] Consumed async context-builder output from revived run `5be627e6`.
- [x] Recorded original async run `ddf28295-da59-449b-8d87-6aec95c16ae4` as unusable/paused (`524 status code` scout; context-builder ENOENT on stale path).
- [x] Write first red runtime/provider-backed GitHub snapshot test.
- [x] Run focused red test and record failure.
- [x] Implement green path.
- [x] Run focused and changed-area validation.
- [x] Run reviewer and address findings.
- [x] Commit getter submodule unsigned and verify `%G? = N`.
- [x] Commit superproject gitlink/plan/docs unsigned and verify `%G? = N`.
- [x] Merge/push per branch strategy and confirm both statuses clean.

## Validation log

- Focused TDD red evidence:
  - With GitHub release body intentionally not mapped into `UpdateCandidate.changelog`, `cargo test -p getter-operations --features lua registered_package_update_check_matches_old_upgradeall_github_release_snapshot -- --nocapture` failed because selected candidate `changelog` was `Null` instead of the old normalized fixture changelog.
  - With GitHub asset content type intentionally not mapped into `UpdateArtifact.content_type`, the same focused test failed because selected artifact `content_type` was `Null` instead of `application/vnd.android.package-archive`.
- Focused green/cfg checks:
  - `cargo test -p getter-operations --no-default-features --tests -- fdroid_autogen::tests::apply_writes_valid_fdroid_package_directory` passed by compiling with the Lua-only test filtered out.
  - `cargo test -p getter-operations --features lua --tests -- fdroid_autogen::tests::apply_writes_valid_fdroid_package_directory` passed.
  - `cargo test -p getter-operations --features lua registered_package_update_check_matches_old_upgradeall_github_release_snapshot -- --nocapture` passed.
- Changed/full getter validation:
  - `git diff --check` and `git -C core-getter/src/main/rust/getter diff --check` passed.
  - Getter submodule `cargo fmt --check` passed.
  - Getter submodule `cargo test --workspace --features lua` passed.
  - Getter submodule `cargo test --workspace --no-default-features` passed.
- Post-review fix validation:
  - After cfg-gating Lua-only test imports in `fdroid_autogen.rs`, `cargo test -p getter-operations --no-default-features` passed with 46 tests and no warning output.
  - The focused GitHub snapshot runtime test still passed after the post-review import cleanup.
  - Final post-review validation `git diff --check`, getter `cargo fmt --check`, `cargo test --workspace --features lua`, and `cargo test --workspace --no-default-features` passed.
- Getter commit:
  - `969a605c1a9d780548bfa9a4d6cc253bc579092e N feat(provider): preserve GitHub release metadata`.
- Superproject commit:
  - `feat(getter): add GitHub standard module runtime slice`, committed unsigned with `%G? = N`.
- Local delivery state:
  - Getter branch `work/github-standard-module-runtime` was fast-forward merged into `rewrite/package-cli-spine` and deleted.
  - Superproject branch `work/github-standard-module-runtime` was fast-forward merged into `rewrite/flutter-getter-spine` and deleted.
  - Final clean status and origin push were verified after this plan update.

## Review log

- Async reviewer run `070356aa-edc2-4c86-904e-0ea8fd6a18f1` wrote `/tmp/upgradeall-github-standard-module-review.md` and reported `Blocker: None found`.
- Reviewer non-blocking notes:
  - `fdroid_autogen.rs` had Lua-only imports warning in no-default builds; fixed by cfg-gating the Lua-only imports and revalidating no-default `getter-operations`.
  - Plan progress/validation checklist was stale; updated this plan.
  - One reviewer-observed changelog-null run was not reproducible; this was consistent with the intentional red-loop mapping break recorded above, and final focused/full validation is green after restoration.
