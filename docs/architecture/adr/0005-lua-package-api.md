# ADR-0005: Lua package API and Rust validation boundary

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

getter embeds Lua for package version scripts, reusable package helpers, and repository autogen scripts.

The Lua/Rust boundary is treated as an RPC/serialization boundary: Lua returns JSON-like tables; Rust validates and deserializes them into typed structs.

Lua scripts do not receive mutable Rust domain objects.

Package version Lua may read package-local helper data under its own package directory's `files/` subtree through a package-scoped getter host API such as `read_package_file(path)`, where `path` is relative to `files/`. The original built-in implementation, `getter_builtin.read_package_file`, does not expose real filesystem paths or a general `io.open` escape hatch; it rejects absolute paths, `..`, directory reads, cross-package reads, and arbitrary repository reads. `read_package_file(path)` returns a Lua string; getter does not interpret encoding, MIME type, JSON, or text-vs-binary mode. Hook code may still wrap the public `read_package_file()` name as local user policy; getter core/CLI does not maintain a protective denylist of hookable public functions. Getter does not assign product semantics to file names or formats inside `files/`. Package directory contents outside getter's explicit discovery set are outside getter domain entirely, not ignored managed objects. Repository source trust is handled by repository review/signing and by users choosing trusted repositories or authoring/copying content into their own repositories.

## Language

Use Lua via `mlua` unless implementation evidence later proves a blocker.

## Boundaries

Lua can use normal Lua tables/functions/metatables, reusable modules via `require`, package import helper for parent packages, and host-provided provider/network APIs based on permissions.

Package identity is provided by the repository directory path (`repo/<repo-name>/<package-path>/` -> `<package-path>`), not by an `id` field inside returned Lua data. Lua returns package/version content; Rust attaches and validates the path-derived package path and optional repository alias from the selected package atom.

Rust owns schema validation, typed domain model, persistence, event dispatch, download task state and platform callback dispatch.

## Lifecycle phases

App-centric phase names:

```text
preflight
setup
match
discover
prepare
select
resolve        # name still open; alternative: make_actions
post_update
```

`plan` is rejected because it is too vague.

## Network permission model

Lua has no native or standard-library network API by default. Network access, when allowed by the package/provider mode, goes through getter host APIs such as `http_get(url, { headers = ..., cache = true|false })`.

Plain package evaluation does not install `http_get` by default. The getter operation/runtime that evaluates a provider-backed script must deliberately install an HTTP transport and own permission checks, Manifest validation, provider policy, cache behavior, and diagnostics for that execution.

`cache` defaults to `false`. Passing `cache = true` opts that HTTP request into getter-owned provider/source caching; Lua chooses cache participation per request, but getter owns cache keys, persistence, revalidation, stale diagnostics, and secret redaction. The initial stable request shape is deliberately small: URL string plus an optional options table containing only string-to-string `headers` and boolean `cache`.

Free network permission is declared per enabled Lua script in package `metadata.jsonc` using a filename-keyed map, for example `lua: { "9999.lua": { permission: ["allow_free_network"] } }`. The `lua` map is lookup-only: getter first discovers an enabled Lua file from the filesystem, then queries this map by basename. Getter does not enumerate the map to discover scripts or warnings. Entries for nonexistent files or dot-prefixed Lua files are inert. The permission can apply to `9999.lua` or to a fixed-version script. `9999.lua` commonly needs free network, but the filename alone does not grant the permission or force the warning if metadata does not declare it. A version script omitted from the `lua` map defaults to `permission: []`.

If a package version script does not declare `allow_free_network`, any external network/dynamic-download data file or API response body used by that script must have a SHA-512 hash listed in that package's `Manifest`; an unlisted or mismatched body is rejected. A missing `Manifest` is equivalent to an empty hash set, not an invalid package, so network fetches from scripts without `allow_free_network` cannot succeed when `Manifest` is missing or empty. Scripts that do not fetch external network content, or only read package-local `files/`, do not need a `Manifest`. Scripts with `allow_free_network` are not blocked by `Manifest` membership but remain high-risk. Repository-level autogen scripts under `.metadata/autogen/` do not have a package Manifest, but autogen scripts that create package directories must generate correct package Manifests for generated packages expected to work without `allow_free_network`. Package `Manifest` is not a repository source manifest and cannot protect same-level repository source files such as `.autogen.jsonc`; those are protected by the repository Git/signing/maintainer trust model.

If a script declares free network permission for arbitrary upstream access beyond Manifest-bound fixed-version content, getter exposes the host HTTP API to that Lua environment and Flutter displays a yellow warning tag in App detail source/version UI.

This tag is informative and does not block use.

Getter local hook scripts under `rc/hook/` are runtime/local policy, not repository registry state. They are discovered only from the filesystem: getter lists enabled `rc/hook/*.lua`, excludes basenames starting with `.`, sorts deterministically, then loads them before every Lua execution environment. There is no hook registry, metadata map, or disabled-hook state. Enabled hooks may wrap visible Lua host functions such as `http_get()` or `read_package_file()` to implement local policy, then call the original getter-internal entrypoint through `getter_builtin.<name>`, for example `getter_builtin.http_get()` or `getter_builtin.read_package_file()`. Getter core/CLI does not maintain a protective denylist of hookable public functions; extra protection belongs in UI/UX policy rather than the getter core. `getter_builtin.*` is an internal escape hatch for hook code; ordinary package/autogen Lua should use the public hooked names instead. The hook layer affects package version scripts, repository-level autogen scripts, and `luaclass/` code through their calls to wrapped host functions, but it is an execution overlay rather than a mechanism for modifying repository source files. Enabled hook loading is fail-closed. Hooks do not bypass Manifest validation for package version scripts without `allow_free_network`.

## Autogen scripts and Lua classes

Reusable Lua helpers live under repository `luaclass/` directories. Repository-level autogen scripts live under `.metadata/autogen/` and output ordinary package directories/version scripts from structured inputs. They are distinct from runtime package version scripts, although both can import shared `luaclass/` helpers.

## Validation

Rust derives package path from the package directory path and validates known package kind, package metadata fields, version-script API version line, required fields, installed target schema, phase function presence/type where required, permission schema, action schema and URL/action validity. Returned Lua tables should not contain a duplicate package `id` field.

Errors must distinguish Lua runtime errors, schema validation errors and domain validation errors.
