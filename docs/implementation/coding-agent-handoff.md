# Coding Agent Handoff: UpgradeAll Rewrite

> Status: Ready for coding-agent bootstrap
> Date: 2026-06-21
> Target agent: pi agent / coding agents running in the UpgradeAll repository

## Read first

Before coding, read these files in order:

1. `AGENTS.md`
2. `docs/README.md`
3. `docs/architecture/upgradeall-getter-rewrite-wiki.md`
4. `docs/architecture/adr/0001-app-centric-lua-package-repository-model.md`
5. `docs/architecture/adr/0002-getter-flutter-platform-boundary.md`
6. `docs/architecture/adr/0003-legacy-room-migration.md`
7. `docs/architecture/adr/0004-sqlite-main-db-and-cache-db.md`
8. `docs/architecture/adr/0005-lua-package-api.md`
9. `docs/architecture/adr/0006-package-centric-cli-command-contract.md`
10. `docs/architecture/adr/0007-flutter-getter-bridge-contract.md`
11. `docs/architecture/adr/0012-getter-owned-provider-modules-and-autogen-refresh.md`
12. `docs/lua-api/repository-layout.md`
13. `docs/lua-api/permissions.md`
14. `docs/lua-api/templates.md`
15. `docs/app/flutter-ui-feature-parity-and-testing.md`

## Mission

Rewrite UpgradeAll from scratch around:

```text
Flutter APP + Rust getter core + Lua package repositories
```

The old hub-app model must not be reintroduced.

## Non-negotiable architecture rules

- Rust getter owns all product/domain logic.
- Flutter owns UI and platform adapter only.
- getter lives in the reusable `core-getter/src/main/rust/getter` git submodule (`https://github.com/DUpdateSystem/getter`); make getter changes in that submodule and update the superproject gitlink.
- getter storage uses SQLite main DB plus separate cache DB.
- Package definitions are package directories in repositories, with `metadata.jsonc`, optional `Manifest`, and direct-child version Lua scripts such as `1.2.3.lua` or `9999.lua`.
- Lua returns JSON-like tables across the Lua/Rust boundary; Rust validates typed structs.
- Package IDs are readable, e.g. `android/org.fdroid.fdroid`, not UUID primary identities.
- Legacy Room migration must be automatic for normal users, but it is intentionally limited/simple.
- Patch stack/source fork is the supported customization model; do not design a runtime UI customization framework.

## First implementation tranche

Do not start with Flutter screens.

Recommended order:

1. Create Rust getter workspace skeleton.
2. Define core Rust types:
   - PackageId
   - RepositoryId
   - RepositoryPriority
   - ResolvedPackage
   - InstalledTarget
   - UpdateCandidate
   - SelectedUpdate
   - UpdateAction
3. Implement repository layout loader:
   - getter data dir `repo/` plus `rc/` roots;
   - `repo/metadata.jsonc` for local repository priority and generated-repository config;
   - `repo/<alias>/` repository aliases;
   - package directories that directly contain `metadata.jsonc`;
   - optional package `Manifest`, optional generated-package `.autogen.jsonc`, direct-child version scripts, and package-local `files/`;
   - repository `luaclass/` helpers and `.metadata/autogen/` generator metadata.
4. Integrate `mlua` minimally:
   - load a package-directory version Lua script;
   - expose `require` search path for repo `luaclass/`;
   - expose `package_from(<package-atom>)` later;
   - return JSON-like Lua table;
   - validate into Rust structs while deriving package identity from the package directory path.
5. Implement repository priority resolution.
6. Implement main DB and cache DB skeleton.
7. Write migration mapping tests before writing migration implementation.
8. Only after getter CLI can evaluate/list packages should Flutter shell begin.

## Testing strategy

Use mixed TDD and BDD.

### TDD

Use TDD for function/domain behavior:

- PackageId parsing/formatting.
- Repository priority resolution.
- Lua table -> Rust validation.
- lifecycle phase output validation.
- cache invalidation key calculation.
- legacy Room mapping functions.
- version comparison and update selection.

### BDD

Use BDD for UI and integration behavior:

- Flutter app list and app detail flows.
- installed autogen preview and confirmation.
- cleanup preview and confirmation.
- yellow network warning tag display.
- legacy migration success/warning UX.
- update/download task flow.

BDD scenarios should be self-explaining documentation tests. Do not over-test BDD.

## Documentation update rule

If coding changes a boundary, model, phase, migration rule, repository layout, or testing rule, update docs in the same patch.

Prefer adding/updating ADRs for decisions rather than burying major changes in code comments.

## Repository naming

- `local` is the default highest-priority user-authored override repository.
- `autogen` is the default generated fallback repository used by ordinary installed-app autogen; `repo/metadata.jsonc` may configure another existing generated repository alias.
- Legacy migration is special and may generate `local` package files once for compatibility.
- Cleanup of missing generated apps only touches the configured generated repository target.

## Open questions to resolve before implementation hardens

- Final name for the `resolve`/`make_actions` lifecycle phase.
- Template conflict behavior when generated target already exists.
- Concrete main DB/cache DB schema.
- Android repo sync mechanism: bundled snapshot vs archive download vs git/libgit2.
- URL rewrite hook schema.
