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
10. `docs/app/flutter-ui-feature-parity-and-testing.md`

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
- Package definitions are Lua files in repositories.
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
   - `repo.toml`
   - `packages/`
   - `lib/`
   - `templates/`
4. Integrate `mlua` minimally:
   - load a Lua package file;
   - expose `require` search path for repo `lib/`;
   - expose `package_from(repo, id)` later;
   - return JSON-like Lua table;
   - validate into Rust structs.
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
- `local_autogen` is the generated fallback repository used by ordinary installed-app autogen.
- Legacy migration is special and may generate `local` package files once for compatibility.
- Cleanup of missing generated apps only touches `local_autogen`.

## Open questions to resolve before implementation hardens

- Final name for the `resolve`/`make_actions` lifecycle phase.
- Template conflict behavior when generated target already exists.
- Concrete main DB/cache DB schema.
- Android repo sync mechanism: bundled snapshot vs archive download vs git/libgit2.
- URL rewrite hook schema.
