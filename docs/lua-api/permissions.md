# Lua Permissions

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Default

Lua package scripts do not receive Lua-native or Flutter/Kotlin-owned network access by default. Plain package evaluation does not install an HTTP function.

Provider-backed getter operations may deliberately install getter-provided provider/source host APIs for that execution. Getter-shipped standard provider modules such as `luaclass.fdroid_android` and `luaclass.github_android_apk` call provider-specific host functions under `getter.provider.*`; plain package evaluation does not install those host functions and therefore cannot run provider-backed modules by itself. Generic/custom HTTP requests go through getter-managed functions such as:

```lua
local body = http_get(url, {
  headers = { Accept = "application/json" },
  cache = true,
})
```

`cache` defaults to `false`. Passing `cache = true` opts that request into getter-owned provider/source caching; getter owns cache keys, persistence, revalidation, stale diagnostics, permissions, and secret redaction. The v1 request shape is intentionally narrow: a URL string plus an optional options table with string-to-string `headers` and boolean `cache`; unsupported options should be rejected rather than silently accepted.

## Free network permission

A package declares free network access per Lua script in `metadata.jsonc`, for example:

```jsonc
{
  "lua": {
    "9999.lua": {
      "permission": ["allow_free_network"]
    }
  }
}
```

`allow_free_network` may be attached to `9999.lua` or to any fixed-version script. The `lua` map is lookup-only: getter first discovers an enabled Lua file from the filesystem, then queries this map by basename. Getter does not enumerate the map to discover scripts or warnings. `9999.lua` commonly needs it, but the filename alone does not grant free network or force the warning if metadata does not grant that permission. A version script omitted from the `lua` map defaults to `permission: []`. Entries for nonexistent files or dot-prefixed Lua files are inert and do not enable, display, validate, or otherwise bring those files under getter management.

Without `allow_free_network`, any external network/dynamic-download data file or API response body used by that script must have a SHA-512 hash listed in the package `Manifest`. This includes getter-owned provider refreshes such as live GitHub releases responses used by `luaclass.github_android_apk`: the provider host may fetch and cache the response, but the script can only consume it when the response digest is already listed in that package's `Manifest`. If the response hash is not listed or does not match, getter rejects the download/use. A missing `Manifest` is equivalent to an empty hash set, not an invalid package, so network fetches from scripts without `allow_free_network` cannot succeed when `Manifest` is missing or empty. Scripts that do not fetch external network content, or only read package-local `files/`, do not need a `Manifest`. Scripts with `allow_free_network` are not blocked by `Manifest` membership but remain high-risk.

Package-local reads from the script's own package `files/` subtree through `read_package_file(path)` are not free-network access. The path is relative to `files/`; the original built-in does not expose real filesystem paths or a general `io.open` escape hatch and returns a Lua string without encoding/MIME/JSON/text-vs-binary interpretation. Hook code may still wrap the public `read_package_file()` name as local user policy because getter core/CLI does not maintain a protective denylist of hookable public functions. These files are repository source artifacts covered by repository review/signing/trust, and getter does not assign product semantics to their file names or formats.

When declared:

- getter exposes the relevant host HTTP API to that Lua environment;
- Flutter displays a yellow warning tag at App detail source/version level;
- use is not blocked by Manifest membership, but normal diagnostics/download validation still apply.

## Timeouts

Network operations use normal network timeouts.

Lua script runtime itself does not use a timeout/fuel limit.

## v1 verification policy

v1 does not enforce repo/script/artifact verification.

Schema fields may exist for future verification, but enforcement is not a v1 requirement.
