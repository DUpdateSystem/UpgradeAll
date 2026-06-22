# AGENTS.md — UpgradeAll rewrite coding agent bootstrap

This repository is being rewritten toward a Flutter APP + Rust getter core + Lua package repository architecture.

Before coding, every agent MUST read:

1. `docs/README.md`
2. `docs/architecture/upgradeall-getter-rewrite-wiki.md`
3. `docs/architecture/adr/0001-app-centric-lua-package-repository-model.md`
4. `docs/architecture/adr/0002-getter-flutter-platform-boundary.md`
5. `docs/architecture/adr/0003-legacy-room-migration.md`
6. `docs/architecture/adr/0004-sqlite-main-db-and-cache-db.md`
7. `docs/architecture/adr/0005-lua-package-api.md`
8. `docs/architecture/adr/0006-package-centric-cli-command-contract.md`
9. `docs/architecture/adr/0007-flutter-getter-bridge-contract.md`
10. `docs/app/flutter-ui-feature-parity-and-testing.md`

## Core architecture rules

- Rust getter owns all product/domain logic.
- Rust getter lives in the `core-getter/src/main/rust/getter` git submodule (`https://github.com/DUpdateSystem/getter`) so it remains independently reusable; implement getter CLI/core changes inside that submodule and update the superproject gitlink, do not vendor getter source into the UpgradeAll superproject.
- Flutter owns UI and platform adapter only.
- Do not reintroduce the old hub-app model.
- Use readable package ids such as `android/org.fdroid.fdroid`, not UUID primary ids.
- Lua package files return JSON-like tables; Rust validates/deserializes them.
- Backend state uses SQLite main DB plus separate cache DB.
- Package Lua source files live in repository folders.
- `local` is user-authored override repo.
- `local_autogen` is generated fallback repo.
- Do not add runtime UI customization/plugin framework unless a later ADR changes this.

## Testing rules

Use mixed BDD and TDD.

TDD is for function/domain behavior:

- Rust functions.
- repository resolution.
- Lua validation.
- migration mapping.
- cache invalidation.
- version comparison.

BDD is for UI/integration behavior:

- Flutter flows.
- migration UX.
- installed autogen confirmation.
- yellow network warning tag.
- update/download task flow.

BDD scenarios are self-explaining documentation tests. Do not over-test with BDD; keep scenarios meaningful and user-visible.

## Implementation discipline

- Make small, reviewable changes.
- Update docs/ADR when behavior or architecture changes.
- Do not edit generated files manually.
- Do not silently drop migration fields; document dropped fields.
- Do not put Android-specific APIs into getter core.
- Do not put provider/update/version/storage logic into Flutter UI.
- If uncertain, add a small ADR or update the architecture wiki before coding.

## Suggested first implementation order

1. Create Rust workspace skeleton for getter.
2. Define package id, repository, and Lua validation structs.
3. Implement repository layout loader.
4. Add mlua evaluation returning JSON-like tables.
5. Implement Rust schema validation.
6. Implement main DB/cache DB skeleton.
7. Implement legacy migration mapping tests.
8. Build minimal Flutter shell only after getter core can be exercised by CLI.
