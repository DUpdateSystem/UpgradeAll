# Lua Templates / Autogen

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

Templates are Lua generators that output package Lua file content.

They are inspired by Funtoo Metatools/autogen, where autogen code produces ebuilds from upstream or structured inputs.

## Template role

Templates are used for:

- generating package files from installed Android apps;
- generating package files from Magisk modules;
- repository maintainer batch generation;
- assisted package creation from GitHub/F-Droid metadata.

Templates are not runtime package definitions.

## Example

```lua
return template {
  id = "android_installed_app",

  generate = function(ctx, input)
    return {
      path = "packages/android/" .. input.package_name .. ".lua",
      content = [[
local android = require("lib.android")

return android.local_app {
  id = "android/]] .. input.package_name .. [[",
  name = "]] .. input.label .. [[",
  package_name = "]] .. input.package_name .. [[",
}
]]
    }
  end
}
```

## UX contract

Generation flow:

1. Android/platform adapter writes an installed-inventory DTO.
2. User clicks generate.
3. getter computes candidate list through `autogen installed preview --inventory <installed.json>`.
4. Flutter shows preview list.
5. User confirms yes/no.
6. getter applies the accepted preview through `autogen installed apply --preview <preview.json> --accept-all` or repeated `--accept <package-id>`.
7. getter writes files under `<data-dir>/repositories/local_autogen`, registers the repo, records `autogen-manifest.json`, and tracks accepted packages in `main.db`.

Cleanup flow:

1. Android/platform adapter writes the current installed-inventory DTO.
2. User clicks clear missing generated apps.
3. getter computes deletion list through `autogen cleanup preview --inventory <installed.json>`.
4. Flutter shows preview list.
5. User confirms yes/no.
6. getter deletes only accepted manifest-managed `local_autogen` files/state.

Cleanup apply refuses stale/tampered previews that do not match the current manifest, and guarded tracked-state deletion only removes rows still owned by `local_autogen` generated packages. Installed apply preserves existing user state (`enabled`, `favorite`, `ignored_version`) and existing non-missing resolution metadata when a package is already tracked. If a managed autogen file has been edited, getter preserves that content into the user-authored `local` repo before regenerating or deleting the generated file. Ordinary autogen cleanup never deletes `local`.

## Repositories

Ordinary installed-app autogen writes to `local_autogen`, using fixed repo id `local_autogen`, default priority `-1`, and deterministic paths such as `packages/android/com.example.app.lua`. Candidates are skipped when any registered repository with priority higher than `local_autogen` already provides the same package id.

Legacy migration may generate `local` files once as a special compatibility path.
