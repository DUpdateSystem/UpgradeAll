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

## post_update

Optional post-update hook. Most persistent state changes should remain in Rust core, not Lua.
