# Phase 1b Plan: Getter Workspace Skeleton

## Goal

Create the Cargo workspace shape for the Getter rewrite without moving or rewriting existing behavior. Phase 1b is a transitional skeleton milestone, not completion of canonical Phase 1.

## Scope

- Add a Cargo workspace inside `core-getter/src/main/rust/getter`.
- Keep the existing root package named `getter` and keep its current CLI behavior in place.
- Add skeleton crates under `core-getter/src/main/rust/getter/crates/`:
  - `getter-core`
  - `getter-storage`
  - `getter-providers`
  - `getter-downloader`
  - `getter-plugin-api`
  - `getter-rpc`
  - `getter-cli`
  - `getter-ffi`
- Keep `api_proxy` compatible with `getter = { path = "../getter", features = ["rustls-platform-verifier-android"] }`.
- Resolve ADR 0007 status drift so the committed Phase 1a CLI contract is no longer treated as provisional.

## Non-goals

- No behavior/module moves from `core-getter/src/main/rust/getter/src/`.
- No change to supported CLI behavior.
- No claim that canonical Phase 1 is complete.
- No clippy `-D warnings` gate.
- No `cargo test --workspace` gate for this milestone.

## Validation

Phase 1b should validate the new workspace shape while preserving Phase 1a behavior:

- `cargo metadata --manifest-path core-getter/src/main/rust/getter/Cargo.toml --no-deps --format-version 1`
- `cargo metadata --manifest-path core-getter/src/main/rust/api_proxy/Cargo.toml --no-deps --format-version 1`
- `cargo fmt --manifest-path core-getter/src/main/rust/getter/Cargo.toml --all --check`
- `cargo check --manifest-path core-getter/src/main/rust/getter/Cargo.toml --workspace --all-targets`
- `cargo check --manifest-path core-getter/src/main/rust/api_proxy/Cargo.toml`
- `just verify-workspace-skeleton`
- `just verify`
- `./gradlew --no-daemon projects` if not already covered by `just verify-workspace-skeleton`

`just verify` is the single current verification entrypoint. It runs the scoped Phase 1a behavior/storage gates and the Phase 1b workspace skeleton checks without adding known-red broad getter tests, `cargo test --workspace`, or clippy `-D warnings`.

## Notes

This milestone creates the split-crate scaffold only. The root `getter` package remains the transitional monolith until a later approved behavior move. The `getter-core` Android/JNI guard is a structural metadata/text check for the new crate boundary; it prevents obvious dependency/reference drift but does not prove that product logic has already been isolated.
