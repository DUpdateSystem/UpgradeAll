# Lua Package Lifecycle

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

UpgradeAll uses an app/update lifecycle inspired by Gentoo ebuild phases, but does not copy source-build phase names.

## Phases

```text
preflight(ctx)
setup(ctx)
match(ctx, installed_item)
discover(ctx)
prepare(ctx, candidates)
select(ctx, candidates, installed, user_state)
resolve(ctx, selected)
post_update(ctx, result)
```

`resolve` is the current recommended replacement for the rejected name `plan`. It means: convert selected candidate/artifact into executable update actions.

## preflight

Validate whether the package can be evaluated on this platform and with current permissions/settings.

## setup

Resolve package/provider setup such as default source priority, credential availability and provider config.

## match

Match installed inventory items to this package.

## discover

Query sources/providers and return release candidates.

## prepare

Normalize, filter and enrich release candidates.

## select

Choose the candidate/artifact to update to, using installed version and user state.

The first getter-core selection helper uses deterministic tokenized version comparison: digit runs compare numerically, text suffixes compare case-insensitively, separators are ignored, and a prerelease-like text suffix (for example `beta`/`rc`) sorts before the final release with the same numeric prefix. The selector skips the user's ignored version and returns the highest candidate newer than the installed version.

## resolve

Return executable update actions:

```lua
return {
  actions = {
    { type = "download", url = "https://...", file_name = "app.apk" },
    { type = "install", installer = "android_package", file = "app.apk" },
  },
  warnings = {},
}
```

As the first offline/mock-provider bridge toward this lifecycle, package Lua may also declare static `updates` candidates in the package table. Getter validates this table, performs Rust-owned selection/version comparison, and issues opaque runtime `action_id`s from the selected candidate; Flutter must still return only the getter-issued `action_id` and must not assemble download/install action payloads.

```lua
return package_def {
  id = "android/org.fdroid.fdroid",
  name = "F-Droid",
  updates = {
    {
      version = "1.2.0",
      channel = "stable",
      source = "fixture",
      artifacts = {
        {
          name = "app.apk",
          url = "https://example.invalid/app.apk",
          file_name = "fdroid.apk",
        },
      },
    },
  },
}
```

The first Phase D implementation slice exposes this boundary only through an offline CLI fixture command: `getter --data-dir <path> update check --fixture <fixture.json>`. The fixture is normalized JSON, not live provider output, and the command returns `network_required = false`, update-check status, selected candidate/artifact, and generated download/install action DTOs. It does not execute network providers, download files, persist download tasks, stream progress events, or invoke Android installers.

ADR-0011 supersedes the earlier persisted fake task scaffold. The accepted Phase D runtime consumes getter-issued actions through an in-memory process-lifetime runtime: task state is not stored in SQLite, `action_id` is single-use, task submission binds a sealed action plan plus package-version Lua object, mock download/install executors simulate task state, and `RuntimeNotification.task_changed` is pushed to Flutter as a best-effort current snapshot. CLI coverage for this model should use Rust runtime tests or a single-process scripted/debug command rather than pretending separate CLI invocations share task memory.

## post_update

Optional post-update hook. Most persistent state changes should remain in Rust core, not Lua.

## Offline validation

`getter --data-dir <path> repo validate <repo-path>` validates repository layout and package schema without network access. The command evaluates local package Lua files with the same constrained `lib/` module loading used by `repo eval`/`package eval`, then returns a getter-owned diagnostic report:

```json
{
  "valid": false,
  "network_required": false,
  "package_count": 0,
  "diagnostics": [
    {
      "severity": "error",
      "code": "package.schema",
      "message": "required string field 'name' is missing",
      "package_id": "android/org.fdroid.fdroid",
      "location": {
        "path": "repo/packages/android/org.fdroid.fdroid.lua"
      }
    }
  ]
}
```

Initial stable diagnostic codes include:

- `repository.read_repo_toml`
- `repository.parse_repo_toml`
- `repository.invalid_id`
- `repository.unsupported_api_version`
- `repository.missing_directory`
- `repository.read_packages_dir`
- `repository.invalid_package_path`
- `repository.invalid_package_id`
- `repository.hash_package_file`
- `package.read_file`
- `package.lua_runtime`
- `package.not_a_table`
- `package.unsupported_value`
- `package.schema`
- `package.domain`

The validation command is intentionally offline. Provider/network validation belongs to later provider/update workflow commands, not repository schema validation.
