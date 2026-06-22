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

1. User clicks generate.
2. getter computes candidate list.
3. Flutter shows preview list.
4. User confirms yes/no.
5. getter writes files.

Cleanup flow:

1. User clicks clear missing generated apps.
2. getter computes deletion list.
3. Flutter shows preview list.
4. User confirms yes/no.
5. getter deletes only autogen-managed files/state.

## Repositories

Ordinary installed-app autogen writes to `local_autogen`.

Legacy migration may generate `local` files once as a special compatibility path.
