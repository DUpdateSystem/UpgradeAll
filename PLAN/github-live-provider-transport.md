# GitHub releases live provider transport/cache refresh

> Status: implemented / reviewed / committed; delivery in progress
> Branches: superproject `work/github-live-provider-transport`, getter `work/github-live-provider-transport`

## Goal

Implement the first getter-owned live provider transport/cache refresh policy for GitHub releases. The getter runtime/provider host should be able to refresh GitHub release snapshots through Rust-owned transport/cache code, while tests remain local mock/snapshot-backed and product Flutter/native surfaces stay narrow.

## Scope

- GitHub REST releases list only, starting from the existing `github-releases` provider path.
- Runtime/provider-backed package evaluation and provider operation cache refresh policy.
- Getter CLI/dev seam as needed to exercise explicit refresh without making Flutter/Kotlin carry provider controls.
- Mock/snapshot-backed tests only; no tests call the live GitHub API.
- Existing provider cache semantics are preserved: successful refresh replaces cache; explicit stale fallback is diagnostic-backed; cache provenance stays in Rust getter.

## Non-goals

- No Flutter GitHub UI/bridge in this slice.
- No custom generated endpoints exposed to product Flutter/native requests.
- No GitHub latest-commit live package generation.
- No global GitHub search/catalog.
- No downloader/background-worker/recovery or installer semantics.
- No provider/cache/storage/domain logic in Flutter/Dart/Kotlin.
- No raw provider payloads, cache-mode controls, endpoint URLs, or live transport controls in product Flutter/native request fields.

## Public seams under TDD

1. `getter-operations::github_releases`: live-refreshable GitHub releases read/refresh operation using a transport abstraction and mock HTTP responses.
2. Provider-backed runtime/Lua host: a generated GitHub Android package can update-check from cache miss via getter-owned GitHub refresh source; stale cache on refresh failure returns getter-owned diagnostics.
3. Getter CLI/dev command, if needed, can trigger GitHub refresh with getter-owned endpoint/transport policy for local/manual workflows without introducing Flutter/native product controls.

## Planned red/green slices

1. Red test: `read_or_refresh_github_releases` can refresh from a mock GitHub releases endpoint using the committed `DUpdateSystem/UpgradeAll` REST snapshot and stores cache/provenance.
2. Red test: forced refresh failure with a valid cached snapshot returns stale cache plus explicit provider diagnostics.
3. Red test: provider-backed runtime update-check on cache miss uses getter-owned refresh source and returns the old UpgradeAll GitHub release candidate without passing `releases_json` through the runtime request.
4. Red test: rate-limit/network/HTTP failure without stale cache is surfaced as a getter-owned provider operation error/diagnostic, not as Flutter/native-requested fixture or endpoint behavior.
5. Green implementation: add minimal transport abstraction/dependency, wire GitHub release refresh, update docs/ADR/CLI as needed.

## Bootstrap docs and skills already read in this task line

The current session/brief records that the AGENTS-listed docs, ADR-0010/0011/0012, and relevant skills (`grill-with-docs` wrapper, `grilling`, `domain-modeling`, `pi-subagents`, `tdd`, `conventional-commits`) were read before this slice. Reopen only if these files change or current instructions require fresh proof.

## Baseline

- Superproject branch: `work/github-live-provider-transport` from clean `rewrite/flutter-getter-spine`.
- Getter submodule branch: `work/github-live-provider-transport` from clean `rewrite/package-cli-spine`.
- Initial status at branch creation and after compaction resume: both working trees clean.
- Async read-only scout: `d06f435a-eb78-418a-9e13-326ed5eb9bdf`, output planned at `/tmp/upgradeall-github-live-provider-scout.md`.

## Progress

- [x] Created work branches from clean rewrite branches.
- [x] Resumed after compaction and confirmed both working trees are on `work/github-live-provider-transport` with clean status.
- [x] Started read-only scout run `d06f435a-eb78-418a-9e13-326ed5eb9bdf`.
- [x] Created this plan artifact.
- [x] Inspect provider cache, GitHub release operations, runtime/provider host, CLI, Cargo dependencies, and Android/no-default build constraints.
- [x] Choose the minimal live transport seam/dependency strategy: synchronous `ureq` 3.x in getter-operations, default rustls with platform-verifier feature wiring for the Android/root getter feature, plus a trait-based transport seam for mock tests.
- [x] Write failing mock/snapshot-backed tests for the first vertical slice.
- [x] Implement minimal green path for GitHub releases transport, cache provenance/freshness, runtime cache-miss refresh, stale fallback diagnostics, and optional CLI live refresh.
- [x] Repeat TDD slices for stale fallback, runtime cache-miss refresh, and error diagnostics.
- [x] Update docs/ADR for live provider transport/cache policy.
- [x] Run focused validation and full getter validation from the submodule.
  - `cargo fmt --check`
  - `cargo test -p getter-operations github_releases -- --nocapture`
  - `cargo test -p getter-operations --features lua registered_package_update_check_ -- --nocapture`
  - `cargo test -p getter-cli --lib parses_provider_github_releases_command_with_optional_fixture -- --nocapture`
  - `cargo test -p getter-operations github_autogen --features lua -- --nocapture`
  - `cargo test -p getter-cli --test bdd_cli -- --name "User applies GitHub Android APK autogen"`
  - `cargo test --workspace --features lua`
  - `cargo test --workspace --no-default-features`
  - `cargo test -p getter-operations --features rustls-platform-verifier github_releases::tests::default_transport_fetches_releases_from_mock_http_server -- --nocapture`
  - `just verify-workspace-skeleton` after exporting the local Android NDK toolchain variables; the first attempt without `ANDROID_NDK_HOME` failed at the expected `aarch64-linux-android-clang` lookup.
- [x] Review functional changes with a reviewer: `/tmp/upgradeall-github-live-provider-review.md` reported **APPROVE** with no blockers; only non-blocking future transport hardening and the expected gitlink-update reminder.
- [x] Commit getter unsigned and verify `%G? = N`: `1f7b181a127527fce53f10381502d8b9f600cc1a N feat(provider): refresh GitHub releases through live transport`.
- [x] Commit superproject docs/gitlink unsigned and verify `%G? = N` (verified with `git log -1 --format='%H %G? %s'` before delivery; exact final hash is recorded in delivery notes because amending this checklist changes the hash).
- [ ] Merge/push rewrite branches, wait for PR/CI, delete work branches, confirm both repos clean.
