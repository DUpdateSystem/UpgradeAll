# GitHub autogen product seam

> Status: reviewed local implementation; pending branch delivery/CI confirmation
> Branches: superproject `work/github-autogen-product-seam`, getter `work/github-autogen-product-seam`

## Goal

Create the next getter-first product seam for GitHub package generation so the already-implemented `luaclass.github_android_apk` runtime/provider update-check behavior can be reached from generated package directories, without adding live GitHub calls or moving provider logic into Flutter/Kotlin.

## Non-goals

- No live GitHub transport/auth/rate-limit implementation.
- No Flutter UI form in this slice unless needed by a narrow bridge follow-up.
- No GitHub global catalog/search/autodiscovery.
- No old Hub/app UUID model revival.
- No provider fixture bodies or endpoint/cache controls in product Flutter/native requests.

## Plan

1. Map existing F-Droid autogen, GitHub provider, runtime, CLI, and fixtures.
2. Define the smallest getter-owned GitHub autogen preview/apply request shape.
   - Required: `owner`, `repo`, `android_package`.
   - Optional: display name, asset include/exclude, include prereleases.
   - Package path should be deterministic and package-directory based.
3. TDD in getter operations:
   - preview emits generated `metadata.jsonc`, `Manifest`, `9999.lua`, `.autogen.jsonc` for `DUpdateSystem/UpgradeAll` using existing real GitHub API snapshot fixture/provenance.
   - apply writes accepted package directory into generated repository with ownership checks.
   - generated package can be update-checked by the runtime using provider cache seeded from the same snapshot.
   - skip/conflict behavior respects higher-priority package coverage and `.autogen.jsonc` ownership.
4. Add CLI/dev command if the getter operation is clean: `autogen github preview/apply` with fixture-backed releases input for tests/dev.
5. Update docs/ADR/CLI/lua-api as needed.
6. Validate from getter submodule; use reviewer for functional changes; commit getter unsigned; update superproject gitlink/plan/docs; merge/push and wait for jobs.

## Bootstrap docs read

- `docs/README.md`
- `docs/architecture/upgradeall-getter-rewrite-wiki.md`
- `docs/architecture/adr/0001-app-centric-lua-package-repository-model.md`
- `docs/architecture/adr/0002-getter-flutter-platform-boundary.md`
- `docs/architecture/adr/0003-legacy-room-migration.md`
- `docs/architecture/adr/0004-sqlite-main-db-and-cache-db.md`
- `docs/architecture/adr/0005-lua-package-api.md`
- `docs/architecture/adr/0006-package-centric-cli-command-contract.md`
- `docs/architecture/adr/0007-flutter-getter-bridge-contract.md`
- `docs/architecture/adr/0012-getter-owned-provider-modules-and-autogen-refresh.md`
- `docs/lua-api/repository-layout.md`
- `docs/lua-api/permissions.md`
- `docs/lua-api/templates.md`
- `docs/app/flutter-ui-feature-parity-and-testing.md`

## Progress

- [x] Created work branches.
- [x] Started async context-builder/scout run `e6fbe498-cec5-4d08-b33e-35741018fdff` for read-only implementation context.
- [x] Checked async run `e6fbe498-cec5-4d08-b33e-35741018fdff`; both children failed before useful output because the prompts exceeded the model context window.
- [x] Established dirty baseline: only this plan was untracked before source edits.
- [x] Completed code mapping of F-Droid autogen, common autogen apply/ownership, GitHub release provider cache, runtime provider host, CLI BDD grammar, and docs.
- [x] Wrote red getter-operations tests for GitHub autogen preview/apply/runtime update-check against the committed `DUpdateSystem/UpgradeAll` GitHub REST snapshot.
- [x] Implemented green getter path: `github_autogen` preview/apply generates `android/github/<owner>/<repo>/<android-package>`, `metadata.jsonc`, `Manifest`, `9999.lua` using `luaclass.github_android_apk`, and `.autogen.jsonc` with generator `github-releases`.
- [x] Added getter CLI/dev `autogen github preview/apply` plumbing and a BDD scenario validating generated output plus provider-backed runtime update-check.
- [x] Updated ADR/lua-api docs for GitHub Android APK autogen semantics and non-goals.
- [x] Pre-review validation passed: superproject/getter `git diff --check`, getter `cargo fmt --check`, focused `cargo test -p getter-operations --features lua github_autogen -- --nocapture`, focused GitHub CLI BDD scenario, `cargo test --workspace --features lua`, and `cargo test --workspace --no-default-features`.
- [x] Post-parser validation passed after adding CLI parser coverage and moving package-id validation before provider cache refresh: `git diff --check`, getter `cargo fmt --check`, focused CLI parser test, focused `github_autogen` tests, focused GitHub CLI BDD scenario, `cargo test --workspace --features lua`, and `cargo test --workspace --no-default-features` ended with `POST_PARSER_FULL_VALIDATION_OK`.
- [x] Review completed via `/tmp/upgradeall-github-autogen-product-review-2.md`: `Blocker: none found`; no non-blocking suggestions. The wrapper reported acceptance rejected because it did not detect the structured report, but the review artifact contains file/line evidence and an acceptance-report block.
- [x] Getter submodule committed unsigned as `4d49476471a6567dfa5e59aa01c02a230c93cd00` (`%G? = N`) with subject `feat(autogen): add GitHub Android package generation`.
- [ ] Commit superproject docs/gitlink unsigned.
- [ ] Merge/push getter and superproject rewrite branches; delete work branches; confirm CI/jobs.
