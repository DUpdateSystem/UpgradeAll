# Lua Repository Layout

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

Recommended layout:

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
```

## repo.toml

```toml
id = "official"
name = "UpgradeAll Official"
priority = 0
api_version = "getter.repo.v1"
```

## packages/

Package files are final package definitions consumed by getter.

Path-derived package id:

```text
packages/android/org.fdroid.fdroid.lua -> android/org.fdroid.fdroid
```

The file should declare the same id. getter validates consistency.

## lib/

Reusable Lua modules. These are conceptually similar to eclasses but are plain Lua modules.

```lua
local github_android = require("lib.github_android_apk")
```

## templates/

Lua generators that output package Lua file content.

## Offline validation

Use getter's structured validator before publishing or registering a repository:

```bash
getter --data-dir /tmp/ua-getter repo validate /path/to/repo
```

The command does not require the repository to be registered and does not use the network. It checks the local layout, `repo.toml`, package path-derived ids, constrained Lua evaluation, and Rust schema/domain validation. Results are returned as JSON with `valid`, `package_count`, `network_required`, and getter-owned `diagnostics`.

Common diagnostic codes include `repository.missing_directory`, `repository.unsupported_api_version`, `package.lua_runtime`, `package.schema`, and `package.domain`.
