# ADR-0001: App-centric Lua package repository model

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

UpgradeAll will replace the old hub-app model with an app/package-centric repository model.

- The primary user-facing object is an App/package, not a Hub.
- Package IDs are readable UpgradeAll namespaces, not UUIDs.
- Examples: `android/org.fdroid.fdroid`, `android/com.termux`, `magisk/zygisk-next`.
- GitHub, F-Droid, Google Play, CoolApk and similar systems are providers/sources/backends, not package identity.
- A single package may have multiple sources.
- Package definitions are Lua files stored in repositories/overlays.
- Repositories have priorities; higher priority wins.
- getter only sees the top-level resolved package for a given package id.

## Context

The previous model represented update logic as App + enabled Hub list. This became insufficient because providers describe where metadata comes from, not what the package is; projects publish artifacts in many different layouts; and different sources for the same installed app should normally be sources of one package.

The new model takes inspiration from Portage/emerge overlays and Funtoo Metatools/autogen, but does not copy ebuild syntax. It uses Lua as an embedded package definition language via Rust getter.

## Repository layout

```text
repo/
  repo.toml
  packages/
    android/
      org.fdroid.fdroid.lua
    magisk/
      zygisk-next.lua
  lib/
    github.lua
    android.lua
    github_android_apk.lua
  templates/
    android_installed_app.lua
    github_android_apk.lua
```

`packages/` contains final package definitions consumed by getter.

`lib/` contains reusable Lua modules. These are conceptually like eclasses, but the project does not introduce an `eclass` keyword or syntax.

`templates/` contains Lua generators that output new package Lua file content. Templates are for autogen workflows, not runtime package evaluation.

## Repository priority

Default priority convention:

```text
local              100   user-written overrides, default highest priority
community/official   0   normal remote repositories
local_autogen   -1   generated fallback packages from installed inventory
```

The user may edit priorities manually. The only hard rule is: higher priority wins.

## Import and override

Reusable Lua modules should use native Lua `require` where practical:

```lua
local github_android = require("lib.github_android_apk")
```

Parent package import uses a host helper because package ids contain slashes/dots and repo id must be explicit:

```lua
local base = package_from("official", "android/org.fdroid.fdroid")
```

Override is a Lua helper/metatable concern, not a Rust API concern. Rust validates only the final returned data object.

## Consequences

Positive:

- App identity is readable and user-supportable.
- Multiple sources become package internals rather than top-level user confusion.
- Users can maintain patch stacks by overriding individual package files.
- Autogen can create fallback local package definitions without contaminating user-authored `local` overrides.

Costs:

- getter must implement repository resolution, priority, package loading, Lua execution, validation and cache invalidation.
- Package authors need documentation and examples.
- Lua outputs must be strictly validated by Rust.

## Non-goals

- No UUID primary identity for packages.
- No runtime UI customization framework.
- No static-template-only system.
- No guarantee that arbitrary user forks never require rebasing.
