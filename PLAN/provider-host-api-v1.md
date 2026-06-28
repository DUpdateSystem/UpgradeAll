# PLAN: Stable Lua provider host API v1

> Status: stable host, provenance, provider module promotion, and cache-backed product runtime update-check slices validated; generated-output migration pending
> Scope: ADR-0012 provider-host API design for F-Droid and GitHub standard Lua modules
> Current boundary: provider modules are promoted to built-in fallbacks, and `update_check_package_issue_action` can evaluate provider-backed packages against Manifest-compatible provider cache entries; generated F-Droid output has not yet been migrated to require them

## Progress

- [x] Consume prior-context brief and temporary context-builder handoff.
- [x] Re-check current docs/code seams relevant to provider host APIs.
- [x] Draft v1 API boundary, names, cache/HTTP/hook/Manifest relationships, and non-goals.
- [x] Run read-only oracle/reviewer review of this design.
- [x] Revise this plan and copy durable decisions into ADR-0012 / glossary docs.
- [x] Implement and validate Slice 1 fixture-backed stable namespace harness.
- [x] Implement and validate Slice 2 provider cache provenance storage and Manifest-compatible cache hits.
- [x] Implement and validate Slice 4 standard provider module promotion tests.
- [x] Add pre-adoption regression coverage locking generated F-Droid output as self-contained/plain-evaluable.
- [x] Add product provider-backed package evaluation/update-check operation before generated F-Droid provider-module adoption.
- [ ] Next slice: generated F-Droid provider-module adoption, keeping generated packages usable through the provider-backed update-check path.

## Current evidence and constraints

- `getter-core` owns constrained Lua evaluation, package/module resolution, JSON-like table conversion, schema/domain validation, `read_package_file`, and the generic `http_get` host seam. It must not depend on provider/cache/storage crates.
- `getter-operations` owns provider/cache orchestration today (`fdroid_catalog`, `github_releases`, `github_latest_commit`, `provider_cache`) and hosts the fixture-backed Lua provider operation harness (`lua_provider_host`) plus private `getter_dev.*` compatibility shims for existing development tests.
- Plain package evaluation does **not** install `http_get` or provider APIs. Provider-backed operations install host functions deliberately.
- `http_get(url, { headers = ..., cache = true|false })` is the accepted narrow generic HTTP request shape; `cache` defaults false, and unsupported options are rejected.
- Runtime hooks live under `<data-dir>/rc/hook/*.lua`, load after host functions are installed in the current operation harness, and call originals through `getter_builtin.*`.
- Package `Manifest` constrains external network/dynamic response bodies for scripts without `allow_free_network`; missing `Manifest` means an empty allow-list. Hooks and cache must not bypass this.
- Current provider-named modules `luaclass.fdroid_android` and `luaclass.github_android_apk` are always-on getter-shipped built-in fallback modules that call operation-installed `getter.provider.*` host functions. Plain package evaluation still does not install provider APIs and fails with a stable host-unavailable error if a package calls a provider module there.
- Product runtime update checks deliberately install the stable provider host and runtime hooks before package evaluation. The bridge request shape remains narrow (`package_id`, optional `repository_id`, `installed_version`, and `pin_version`); provider fixture bodies, cache mode, endpoint URLs, and live transport controls are not Flutter/native request fields in this slice.
- Until live provider transport is accepted and implemented, the product runtime path is cache-backed: provider-backed packages can use existing provider cache rows only when Manifest-compatible provenance permits them, while cache miss/refresh remains a later provider operation concern.
- Repository-local `luaclass/` modules must continue to override getter-shipped built-ins; cross-repository `luaclass` lookup stays unsupported.

## Design goals

1. Give standard provider modules a stable host namespace that is not `getter_dev.*`.
2. Keep Rust getter/provider operations as the owner of F-Droid/GitHub parsing, cache refresh, diagnostics, and candidate normalization.
3. Keep `getter-core` provider-agnostic: it may ship Lua source that calls a named host API, but the Rust host implementation is installed by operations.
4. Preserve user hookability without adding protective denylists: stable host functions that are public to package Lua should also have `getter_builtin.*` originals for hooks.
5. Keep v1 small enough to test with fixture-backed transports before live HTTP/auth/UI exposure.
6. Avoid forcing generated F-Droid output to depend on provider modules until the stable host API and validation story are tested.

## Stable Lua namespace

### Stable public namespace

Use one stable table rooted at `getter`:

```lua
getter.provider.fdroid.update_candidates(spec)
getter.provider.github.release_candidates(spec)
-- reserved for an explicit live/floating operation, not installed by the default release module:
getter.provider.github.latest_commit(spec)
```

The first installed v1 candidate APIs are F-Droid `update_candidates` and GitHub `release_candidates`. The GitHub `latest_commit` shape is reserved here because ADR-0012 names live/latest-commit behavior, but it should not be installed by the default release-check module or treated as ordinary versioned update behavior until live package semantics are implemented.

Rationale:

- `getter` clearly marks host-owned functionality, unlike free globals such as `fdroid_update_candidates`.
- `provider` groups provider-specific host APIs and leaves room for non-provider getter APIs later.
- Provider and function names are explicit enough for hooks and diagnostics.
- The dev-only `getter_dev.*` compatibility namespace remains private and should not appear in stable package docs.

### Hook originals

When an operation installs the v1 provider host, it installs both public and original paths:

```lua
getter.provider.fdroid.update_candidates = <hookable function>
getter_builtin.provider.fdroid.update_candidates = <original function>

getter.provider.github.release_candidates = <hookable function>
getter_builtin.provider.github.release_candidates = <original function>

-- when a live/latest-commit operation installs the reserved function:
getter.provider.github.latest_commit = <hookable function>
getter_builtin.provider.github.latest_commit = <original function>
```

Hooks may wrap provider functions the same way they wrap `http_get` or `read_package_file`:

```lua
local upstream = getter_builtin.provider.github.release_candidates

function getter.provider.github.release_candidates(spec)
  spec = shallow_copy(spec)
  spec.endpoint_id = spec.endpoint_id or "github-mirror"
  return upstream(spec)
end
```

Ordinary package/version Lua should call the public `getter.provider.*` functions, usually through `luaclass.*`, and should not depend on `getter_builtin.*`.

### Generic HTTP remains separate

`http_get(url, { headers = ..., cache = true|false })` remains the generic HTTP host seam.

This v1 plan intentionally clarifies ADR-0012's earlier broad wording that Lua/provider modules opt individual HTTP requests into cache with `http_get(cache = true)`: generic/custom Lua HTTP still uses `http_get`, but the getter-shipped standard F-Droid/GitHub modules should use provider-specific Rust host functions. Those host functions opt into provider/source cache through getter operation policy rather than by making Lua parse provider payloads.

Standard F-Droid/GitHub modules should **not** parse provider payloads in Lua by calling `http_get` directly. They should call provider-specific host functions so Rust owns parsing, cache consistency, diagnostics, and normalization.

`http_get` hooks affect Lua code that calls `http_get`. Provider-specific host functions are separate hook seams; v1 should not promise that a Lua wrapper around `http_get` intercepts Rust-internal provider requests. If a user wants to rewrite a provider endpoint in v1, they wrap `getter.provider.<provider>.*` or configure the provider endpoint when that configuration exists.

## Provider function inputs

Package Lua passes source coordinates and package-authored selection rules. Operation context supplies cache mode, fixture/live transport, credentials, endpoint config, and refresh policy.

Package Lua should not pass raw fixture JSON/XML or force-refresh mode in the stable API. Those remain test/operation request inputs.

### F-Droid

```lua
local result = getter.provider.fdroid.update_candidates {
  package_name = "org.fdroid.fdroid",
  endpoint_id = "official", -- optional; default is operation/getter default
}
```

Rules:

- `package_name` is required and non-empty.
- `endpoint_id` is optional and resolves through getter/provider endpoint configuration; omitting it uses the default F-Droid endpoint, initially official F-Droid.
- `endpoint_url` is not a normal package authoring field in v1. Test fixtures may still inject endpoint URL through operation context.
- Channel/archive/anti-feature/localized metadata fields are deferred until their semantics are accepted.

### GitHub releases

```lua
local result = getter.provider.github.release_candidates {
  owner = "f-droid",
  repo = "fdroidclient",
  asset = {
    include = "[.]apk$", -- Rust regex syntax, not Lua pattern syntax
    exclude = "debug",
  },
  include_prereleases = false,
  endpoint_id = "github", -- optional; default is operation/getter default
}
```

Rules:

- `owner` and `repo` are required non-empty typed fields.
- The stable host input keeps typed `owner`/`repo`; any `"owner/name"` shorthand may exist in a Lua helper, but the host receives normalized typed fields.
- `asset.include` and `asset.exclude` are optional Rust `regex`-syntax filters interpreted by Rust provider code. Examples should use Rust regex forms such as `[.]apk$` or `\\.apk$`, not Lua pattern syntax such as `%.apk$`.
- `include_prereleases` defaults false.
- API base URL, auth identity, rate-limit mode, and live/fixture transport are operation/provider configuration, not package script fixture fields.

### GitHub latest commit / live revision

```lua
local result = getter.provider.github.latest_commit {
  owner = "DUpdateSystem",
  repo = "UpgradeAll",
  ref = "HEAD", -- optional; default HEAD
  endpoint_id = "github", -- optional
}
```

Rules:

- This is a **live/floating** provider API, not a normal release-candidate API.
- The shape is reserved in the v1 design because ADR-0012 names GitHub latest-commit behavior and Rust already has a fixture-backed provider operation.
- The default v1 release-check host/module should not install or call `latest_commit`. A later explicit live operation/helper/module may install it and map the result into live package behavior only after the live-update UI/CLI semantics are ready.

## Provider function result envelopes

Provider host functions return structured envelopes. Standard `luaclass` modules map envelopes into the existing `package_version { updates = ... }` shape, while operations also capture provider call traces/diagnostics outside Lua for DTOs and cache metadata.

### Candidate result envelope

F-Droid `update_candidates` and GitHub `release_candidates` return:

```lua
{
  -- Non-empty candidate sequence. Omit/set nil when there are no candidates;
  -- do not return an empty Lua table here until the Lua JSON boundary has
  -- explicit array support.
  candidates = {
    {
      version = "1.20.0",
      version_code = 1020000,      -- optional
      channel = "stable",         -- optional
      source = "fdroid",          -- optional, provider id for display/diagnostics
      artifacts = {
        {
          name = "apk",
          url = "https://...",
          file_name = "app.apk",  -- optional
          sha256 = "...",         -- optional
          size = 12345,            -- optional bytes
        },
      },
    },
  },
  source = "cache" | "refreshed" | "stale",
  cache_key = "provider-cache-key",
  diagnostics = {
    {
      code = "cache.refresh_failed",
      message = "...",
      provider = "fdroid" | "github",
      cache_key = "provider-cache-key", -- optional when not cache-related
      source = "stale",                 -- optional
      stale_fetched_at_unix = 123,       -- optional
      -- provider-specific coordinates may be included:
      endpoint_id = "official",
      package_name = "org.fdroid.fdroid",
      owner = "f-droid",
      repo = "fdroidclient",
    },
  },
}
```

Zero-candidate provider results use `candidates = nil`/omitted plus diagnostics. This avoids the existing Lua JSON ambiguity where an empty Lua table serializes as `{}` rather than `[]`, which would fail the current `updates` array schema if passed through directly.

The non-empty `candidates[]` item shape is intentionally the current `getter_core::UpdateCandidate` / `UpdateArtifact` shape. Richer provider-candidate fields from ADR-0012 (`published_at`, `changelog`, `metadata_digest`, etc.) are future extension fields, not required to stabilize v1.

### Latest-commit result envelope

GitHub `latest_commit` returns:

```lua
{
  live = true,
  version = "0123456",       -- display/comparison string for live semantics
  revision = "0123456789...", -- full commit id when known
  source = "cache" | "refreshed" | "stale",
  cache_key = "provider-cache-key",
  latest_commit = {           -- normalized Rust-owned live revision details
    version = "0123456",
    revision = "0123456789...",
    html_url = "https://github.com/...", -- optional
    message = "...",                   -- optional
  },
  diagnostics = { ... },
}
```

It should not be silently converted into ordinary versioned release candidates by the default GitHub APK helper.

## Standard Lua modules

### `luaclass.fdroid_android`

Stable author API should stay the accepted small F-Droid shape:

```lua
local fdroid = require("luaclass.fdroid_android")

return fdroid.package {
  package_name = "org.fdroid.fdroid",
  endpoint_id = "official", -- optional
}
```

Implementation sketch:

```lua
function fdroid.package(spec)
  local result = getter.provider.fdroid.update_candidates {
    package_name = require_string(spec, "package_name"),
    endpoint_id = optional_string(spec, "endpoint_id"),
  }
  return package_version {
    source_priority = { "fdroid" },
    -- nil when there are no candidates, so the final package omits `updates`
    -- instead of returning an empty Lua table through the JSON boundary.
    updates = result.candidates,
  }
end
```

It should not require or duplicate display `name`; F-Droid catalog metadata is self-describing, and package identity is path-derived.

### `luaclass.github_android_apk`

Stable author API shape:

```lua
local github_android = require("luaclass.github_android_apk")

return github_android.package {
  name = "F-Droid",
  android_package = "org.fdroid.fdroid",
  owner = "f-droid",
  repo = "fdroidclient",
  asset = {
    include = "[.]apk$", -- Rust regex syntax, not Lua pattern syntax
    exclude = "debug",
  },
  include_prereleases = false,
}
```

Implementation sketch:

```lua
function github_android.package(spec)
  local result = getter.provider.github.release_candidates {
    owner = require_string(spec, "owner"),
    repo = require_string(spec, "repo"),
    asset = optional_table(spec, "asset"),
    include_prereleases = optional_boolean(spec, "include_prereleases"),
    endpoint_id = optional_string(spec, "endpoint_id"),
  }
  local package = {
    name = spec.name,
    source_priority = { "github" },
    -- nil when there are no candidates, so the final package omits `updates`
    -- instead of returning an empty Lua table through the JSON boundary.
    updates = result.candidates,
  }
  if spec.android_package ~= nil then
    package.installed = {
      { kind = "android_package", package_name = require_string(spec, "android_package") },
    }
  end
  return package_version(package)
end
```

The module may later grow explicit live helpers, but release checks and latest-commit checks must stay semantically distinct.

### Promotion path

Implemented promotion state:

1. Stable host installation exists in `getter-operations`; the old `getter_dev.*` compatibility shims delegate through the stable host functions for development harness callers.
2. `luaclass.fdroid_android` and `luaclass.github_android_apk` call `getter.provider.*`, not `getter_dev.*`.
3. Tests cover provider-backed operation success, repository-local override precedence, and plain package evaluation failing with a stable host-unavailable diagnostic when provider modules are called without installed provider host functions.
4. Provider modules are always-on getter-shipped built-in fallback modules.
5. Generated F-Droid output still does not require `luaclass.fdroid_android`; changing generated output remains a later generator slice.

## Cache and refresh semantics

### Operation-owned cache mode

The operation evaluating package Lua decides the cache mode:

- normal update/read: use cached provider facts when valid, refresh on cache miss or stale according to provider policy;
- forced refresh: bypass cached reads for the requested provider/package scope, replace cache on success, and use stale cache only with explicit diagnostics on refresh failure.

Lua package authors do not pass `mode = "force_refresh"` to stable provider host functions. The first product runtime slice keeps `update_check_package_issue_action` in normal cache-backed mode only and does not add provider fixture/cache-mode fields to the Flutter/native bridge payload. Live refresh and user-triggered force-refresh semantics remain separate provider-operation work.

### Provider cache contents

Provider/source cache remains in `cache.db`. For standard provider functions, cache participation is operation-owned: Lua passes provider coordinates/selection rules to `getter.provider.*`, then Rust provider code decides which upstream request(s), parsed facts, freshness tokens, and cache entries are involved. Generic/custom Lua HTTP remains the path where Lua calls `http_get(..., { cache = true })` directly.

Provider-specific operations may store parsed facts (current behavior) rather than raw HTTP bodies, but stable v1 must record enough provenance to enforce package Manifest rules for cache hits. At minimum, provider cache/provider-call metadata needs the source response body digest(s), provider/parser/cache schema version, and freshness tokens when available.

Current fixture cache keys are acceptable for internal tracers, but a stable live implementation must make key inputs explicit:

- provider id and provider operation/cache schema version;
- endpoint/API base URL digest and endpoint id;
- provider coordinates (`package_name`, `owner/repo`, `ref`, request type);
- parser/provider implementation version when interpretation changes;
- auth/rate-limit identity when auth exists, without storing secrets;
- freshness tokens such as ETag, Last-Modified, source timestamp, index revision, API cursor, or response digest when available.

Auth/freshness-token design can be implemented incrementally, but the v1 docs must not claim cache keys are final until those inputs exist.

### Diagnostics

Stable provider diagnostics should use stable codes and getter-owned fields. Existing codes carry forward:

- `cache.refresh_failed`
- `used_stale_cache`
- `provider.fdroid.package_not_found`
- `provider.github.asset_not_found`

Required-field and invalid-shape errors are package authoring/schema errors and may fail Lua evaluation. Provider lookup misses and empty asset matches should return no `candidates` (`nil`/omitted) plus diagnostics, not crash the package evaluation.

If refresh fails and no usable cache exists, the provider host call fails the operation with a provider diagnostic. If stale cache exists and the operation policy permits stale fallback, the result returns `source = "stale"`, candidates from stale facts, and explicit diagnostics.

## Manifest and permission semantics

Provider host calls are still package version script execution. Therefore:

- If the current script lacks `allow_free_network`, every external response body used by a provider function must hash to a SHA-512 entry in that package's `Manifest`.
- Missing/empty `Manifest` means provider network/cache use cannot succeed for non-free scripts unless no external body is needed.
- A cache hit for parsed provider facts is usable in a non-free script only when the cache/provider-call provenance proves that the underlying source response body digest(s) are allowed by the current package `Manifest`.
- If provenance is missing, the cache entry is not usable for non-free package evaluation; getter must refetch and validate, or fail closed.
- If the script has `allow_free_network`, Manifest membership does not block provider responses, but diagnostics/high-risk UI still apply.

This keeps provider-specific host calls from bypassing the `http_get`/Manifest policy merely because parsing moved into Rust.

## Hook semantics

For v1, hookability is function-level:

- `http_get` remains a hook seam for Lua-authored generic HTTP.
- `read_package_file` remains a hook seam for package-local file reads.
- `getter.provider.fdroid.update_candidates` and `getter.provider.github.release_candidates` are v1 hook seams for provider-level local policy; the reserved `getter.provider.github.latest_commit` becomes a hook seam only when a later explicit live/latest-commit operation installs it.

Operation install order should be:

1. configure constrained Lua/package path;
2. install helper functions and getter host functions (`read_package_file`, `http_get` when applicable, provider APIs when applicable) plus `getter_builtin` originals;
3. load enabled `rc/hook/*.lua` fail-closed in deterministic order;
4. evaluate package/autogen Lua.

Hooks are trusted local policy for advanced users. Do not add getter-core denylist behavior to prevent users from wrapping provider functions.

## Error model

- Invalid Lua host input (missing `package_name`, missing `owner`, invalid `asset` type) is an authoring/schema failure and should fail package evaluation with a stable diagnostic prefix/code when possible.
- Provider HTTP/parse/cache failures are provider operation failures. They should become getter-owned diagnostics; if no fallback exists, the update check fails rather than silently returning no updates.
- Provider miss/no matching artifact is a successful provider call with zero candidates and diagnostics.
- Hook load/runtime errors fail closed and fail the current Lua execution.
- `getter_dev.*` errors are not stable product errors and should not appear in public docs once v1 is implemented.

## Crate boundary

- `getter-core` may keep shipping Lua source files under `src/luaclass/*.lua`; those files are strings and can refer to stable host names without Rust linking to provider crates.
- `getter-core` must not implement or know F-Droid/GitHub provider host logic.
- `getter-operations` (or a new operations-owned submodule/crate if needed later) installs `getter.provider.*`, opens `cache.db`, applies provider cache/Manifest/hook policy, and calls `getter-providers` parsers.
- Flutter/Dart and Android/Kotlin remain adapters: they may call getter operations and render DTOs, but they must not parse provider payloads, generate Lua, map upstream IDs to package paths, decide cache invalidation, or perform provider HTTP.

## Non-goals for v1

- No old Hub/app UUID model and no legacy flat Lua layout (`repo.toml`, top-level `packages/`, `lib/`, `templates/`).
- No generated F-Droid rewrite to standard modules until v1 host semantics and tests exist.
- No live product HTTP/auth/rate-limit implementation in this design slice.
- No downloader/installer/background worker/recovery semantics.
- No Flutter/Kotlin provider parsing or domain logic.
- No GitHub global catalog/search/autogen.
- No cross-repository `luaclass` lookup.
- No Lua-native networking or default `http_get` in plain package evaluation.
- No broad protective denylist for local hooks.
- No claim that current fixture cache keys are complete for authenticated/live provider stability.

## Later implementation slices

### Slice 1: stable namespace harness (fixture-backed)

- Add operations-owned installer for `getter.provider.*` using fixture-backed F-Droid/GitHub providers.
- Return result envelopes and record provider call traces.
- Keep `github.latest_commit` reserved/not installed by the default release-check host; it may stay covered by the existing provider operation until live-package semantics are implemented.
- Tests:
  - stable namespace exists only in provider-backed operation;
  - `getter_dev.*` is not required by stable modules;
  - F-Droid and GitHub release functions return `{ candidates, source, cache_key, diagnostics }` for non-empty candidates;
  - F-Droid/GitHub zero-candidate cases return `candidates = nil`/omitted plus diagnostics and the standard module does not emit an empty Lua table as `updates`;
  - reserved latest-commit shape is not installed/called by the default release module;
  - operation cache mode, stale diagnostics, and provider miss diagnostics match this plan.

### Slice 2: provider cache provenance schema

- Add provider/source cache provenance storage or a side table that records the source response body digest(s), provider/parser/cache schema version, and freshness tokens used to produce parsed provider facts.
- Teach provider cache reads to report whether provenance is present and Manifest-compatible for the current package script.
- Until this exists, non-free provider cache hits with parsed facts must fail closed or refetch/revalidate; tests must prove missing provenance does not bypass Manifest.
- Keep this in `getter-operations`/storage-facing code, not `getter-core`.

### Slice 3: hooks + Manifest policy for provider functions

- Extend the internal policy harness so provider functions are installed before runtime hooks.
- Prove provider hooks can wrap `getter.provider.*` and call `getter_builtin.provider.*`.
- Prove non-free scripts cannot use provider response/cache facts without Manifest-listed source response digests and accepted provenance.
- Keep transport fixture/in-memory; no live HTTP.

### Slice 4: standard module promotion tests

Done in the implementation branch:

- `luaclass.fdroid_android` and `luaclass.github_android_apk` call `getter.provider.*`.
- Repository-local override precedence remains covered.
- Plain package evaluation resolves the built-in provider modules, but calling them without operation-installed provider host functions fails with a stable host-unavailable diagnostic.
- Provider modules are always-on built-in fallback modules; `getter_dev.*` remains only a private compatibility shim in operation tests.

### Slice 5: product runtime adoption and generated output migration

Done in the implementation branch:

- Updated ADR-0012, `CONTEXT.md`, and this plan with stable provider module/product runtime status.
- Added pre-adoption regression coverage that generated F-Droid output remains self-contained, does not require `luaclass.fdroid_android`, and does not call `getter.provider.*` while normal package evaluation remains plain.
- Added a product-facing provider-backed package evaluation/update-check operation by routing only `update_check_package_issue_action` through the stable provider host; read-model `package_eval` remains plain.
- Kept the Flutter/native bridge request shape narrow: no provider fixture body, cache mode, endpoint URL, or live transport fields are added to the product payload.
- Added runtime tests proving static packages still work, F-Droid/GitHub provider modules work from Manifest-compatible provider cache entries, Manifest-incompatible cache fails closed, and no-update checks do not issue actions.

Still pending:

- Update generated F-Droid output to use `luaclass.fdroid_android` only after the product provider-backed operation exists and tests prove generated packages remain usable through that operation.
- Add generator tests proving generated output does not depend on `getter_dev.*` or repository-local copied modules.

## Review questions for the read-only reviewer

1. Are `getter.provider.*` and `getter_builtin.provider.*` consistent with existing hook and Lua-boundary decisions?
2. Does the plan keep provider/cache/storage logic out of `getter-core` while allowing getter-shipped standard Lua modules?
3. Is the envelope shape small enough for v1 while preserving diagnostics/cache/source metadata outside Lua and avoiding empty-Lua-table/JSON-array ambiguity?
4. Is the Manifest/cache provenance rule concrete enough now that provenance storage is a required implementation slice before Manifest-bound provider cache hits become stable?
5. Should latest-commit remain a reserved shape that is not installed by the default release host until live-package semantics are implemented?
6. Are any proposed names or semantics likely to conflict with ADR-0012, ADR-0005, or the existing repository/layout model?
