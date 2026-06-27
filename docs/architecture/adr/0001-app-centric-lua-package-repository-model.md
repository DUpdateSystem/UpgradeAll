# ADR-0001: App-centric Lua package repository model

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

UpgradeAll will replace the old hub-app model with an app/package-centric repository model.

- The primary user-facing object is an App/package, not a Hub.
- Package paths are readable UpgradeAll namespaces, not UUIDs.
- Examples: `android/app/org.fdroid.fdroid`, `android/f-droid/app/org.fdroid.fdroid`, `android/magisk/zygisk-next`.
- Package references use Gentoo-style atoms: `<package-path>` resolves by repository priority, while `<package-path>::<repo-name>` selects a local repository alias explicitly.
- The local repository alias is the directory name under `repo/`; if a user clones or renames the official repository to `repo/a`, the local alias is `a`.
- Local aliases are user-controlled reference/priority names, not repository security identities.
- GitHub, F-Droid, Google Play, CoolApk and similar systems are providers/sources/backends, not repository identities.
- A single package may have multiple sources.
- Package definitions are directories stored in repositories/overlays.
- Package identity is derived from the package directory path, like emerge/ebuild category-package identity, not duplicated as an `id` field inside Lua.
- Package metadata lives in `metadata.jsonc`; Android app install identity uses `android.package_name`, not an ambiguous `package_id` field.
- Package metadata declares Lua script permissions per file, e.g. which script needs `allow_free_network`.
- Package version scripts live directly in the package directory as direct child files named `<version>.lua`; basename must end with `.lua` and must not start with `.`; removing `.lua` yields the literal version string with no SemVer requirement; live/floating scripts use `9999.lua`; every enabled Lua script must start with an explicit `#!/bin/upa-lua v1` API-version line; dot-prefixed Lua files are excluded from Lua discovery. This dot-prefix exclusion is only for Lua files; non-Lua package files are governed by the explicit getter file whitelist, so `.autogen.jsonc` is managed when present in a generated package.
- External network/dynamic-download response-body hashes for package version scripts are recorded in a package `Manifest` as `<sha512> [optional-name]`; repository source files are protected by the repository Git/signing/maintainer trust model, not by package `Manifest`.
- Repositories have priorities; higher priority wins.
- getter only sees the top-level resolved package for a given package path unless the caller specifies `::repo-name`.

## Context

The previous model represented update logic as App + enabled Hub list. This became insufficient because providers describe where metadata comes from, not what the package is; projects publish artifacts in many different layouts; and different sources for the same installed app should normally be sources of one package.

The new model takes inspiration from Portage/emerge overlays and Funtoo Metatools/autogen, but does not copy ebuild syntax. It uses Lua as an embedded package definition language via Rust getter.

## Repository layout

```text
<data-dir>/
  main.db
  cache.db
  repo/
    metadata.jsonc
    official/
      .metadata/
        metadata.jsonc
        autogen/
          metadata.jsonc
          android.lua
      luaclass/
        android.lua
        github_android_apk.lua
      android/
        app/
          org.fdroid.fdroid/
            metadata.jsonc
            Manifest
            1.20.0.lua
            9999.lua
            files/
              helper-data.json
        f-droid/
          app/
            org.fdroid.fdroid/
              metadata.jsonc
              Manifest
              1.20.0.lua
    local/
      android/
        app/
          org.example.app/
            metadata.jsonc
            Manifest
            1.2.3.lua
  rc/
    hook/
      http.lua
```

The repository root `repo/metadata.jsonc` contains repository-related local registry/config rules such as repository priority. It is part of getter's local state, not part of any one package repository and not something an upstream repository update should overwrite. Hook/runtime policy is not stored in `repo/metadata.jsonc`; local hooks live under `rc/hook/`.

`rc/` is the getter runtime/local policy root. It is a top-level sibling of `repo/` under the getter data directory, beside storage files such as `main.db` and `cache.db`. It is not a repository root and does not participate in repository or package discovery. Current defined content is `rc/hook/*.lua`; future runtime/local policy such as environment, credential, or network behavior belongs under `rc/`, not in `repo/metadata.jsonc`.

`repo/metadata.jsonc` is the only current repo-root reserved entry. Every direct child directory of `repo/` is an UpgradeAll repository alias. The alias is the local directory name, not necessarily the upstream repository's advertised name. Renaming or cloning a repository to a different child directory intentionally changes its local alias, giving users freedom to fork or maintain an intermediate repository layer. Future repo-root reserved entries require an explicit design/ADR because they occupy alias namespace; no names such as `hook` or `rc` are pre-reserved under `repo/`.

Package directories are final package definitions consumed by getter. UpgradeAll/getter domain strings, including package paths and aliases, are treated as UTF-8. Getter does not detect or convert other filesystem/text encodings; inputs in other encodings are still interpreted as UTF-8. A directory that directly contains `metadata.jsonc` declares a package boundary. `.autogen.jsonc` does not declare a package boundary; it is only a generated-package ownership record inside a package directory that already has `metadata.jsonc`. If `metadata.jsonc` parses correctly as package metadata, the directory is a valid package directory; if parsing fails, getter reports an invalid package metadata diagnostic for that package path. A package path is the directory path relative to a repository, for example `android/app/org.fdroid.fdroid`. Once a package boundary is found, it is the package path endpoint and getter does not discover nested packages below it.

Repository self metadata under `.metadata/metadata.jsonc` stores publishable repository facts such as schema version, upstream URL, description, maintainers/co-maintainers, and signing/trust metadata. Security/trust checks use this verified metadata, not the local alias. If `.metadata/metadata.jsonc` is missing, the repository may still be used as unverified/local-source content, but repo update, signature, and trust operations are unavailable. If `.metadata/metadata.jsonc` exists but cannot be parsed, getter reports a repository metadata diagnostic. Inside a repository alias directory, getter only considers explicit entries: reserved repository-root directories such as `.metadata/` and `luaclass/`, plus directory chains that form package paths. Reserved directories are handled only by their own responsibility and never participate in package discovery; package paths cannot begin with reserved names such as `.metadata` or `luaclass`. Future repository-root reserved directories follow the same rule so the repo layout remains organizable. Other repository contents such as `README.md`, `docs/`, or random helper files are outside getter domain entirely: they are not parsed, validated, displayed, warned about, or modeled as ignored managed objects.

Package `metadata.jsonc` stores package-level metadata such as `type`, platform install identity, maintainers, description, homepage, and other user-facing/source facts. Android app install identity is `android.package_name`.

`Manifest` stores allowed external network/dynamic-download response-body hashes for package version scripts in `<sha512> [optional-name]` format. The hash is authoritative; the optional name is for humans/debugging because a URL may not reveal the actual returned filename. A missing `Manifest` is equivalent to an empty hash set, not an invalid package. For package version scripts without `allow_free_network`, getter accepts externally fetched data files or API response bodies only when their SHA-512 hash appears in that package's `Manifest`; with a missing/empty `Manifest`, such network fetches cannot succeed. Scripts that do not fetch external network content, or only read package-local `files/`, do not need a `Manifest`. Scripts with `allow_free_network` are not blocked by `Manifest` membership but remain high-risk. `Manifest` belongs only to package directories; repository-level autogen scripts under `.metadata/autogen/` do not have a Manifest. However, autogen scripts that create package directories must also generate correct package `Manifest` files for generated packages expected to work without `allow_free_network`. It is not a repository source manifest; metadata files, Lua scripts, `files/`, sibling `.autogen.jsonc`, `luaclass/`, autogen scripts, and repository metadata are protected by the repository Git/signing/maintainer trust model. Since `Manifest` and `.autogen.jsonc` are same-level package files, `Manifest` cannot architecturally protect `.autogen.jsonc`.

`<version>.lua` files are fixed-version package scripts. Getter discovers them only from direct child files of the package directory whose basename ends with `.lua` and does not start with `.`. Removing the `.lua` suffix yields the literal version string; getter does not require SemVer or otherwise constrain the version syntax at discovery time. Examples include `1.2.3.lua`, `1.2.3-r1.lua`, `v1.2.3.lua`, and `2026.06.25.lua`. `9999.lua` is the live/floating package script. Every enabled Lua script must declare its interpreter/API version on the first line, for example `#!/bin/upa-lua v1`; the version is required and has no implicit default. A Lua file whose basename starts with `.`, for example `.9999.lua`, is excluded from Lua discovery: getter does not parse, validate, execute, display, or apply permission metadata to it as Lua. This dot-prefix exclusion is only for Lua files; non-Lua package files are governed by the explicit getter file whitelist, so `.autogen.jsonc` is managed when present in a generated package.

Package Lua may read package-local helper files under its own package directory's `files/` subtree through a package-scoped getter host API such as `read_package_file(path)`, where `path` is relative to `files/`. The original built-in implementation, `getter_builtin.read_package_file`, does not expose real filesystem paths or a general `io.open` escape hatch; it rejects absolute paths, `..`, directory reads, cross-package reads, and arbitrary repository reads. `read_package_file(path)` returns a Lua string; getter does not interpret encoding, MIME type, JSON, or text-vs-binary mode. Hook code may still wrap the public `read_package_file()` name as local user policy; getter core/CLI does not maintain a protective denylist of hookable public functions. Getter does not assign product semantics to names or formats inside `files/`; the package owns them. Package directory contents outside getter's explicit discovery set (`metadata.jsonc`, optional generated-package `.autogen.jsonc`, `Manifest`, enabled direct-child `*.lua`, and `files/`) are outside getter domain entirely: getter does not parse, validate, display, warn about, or model them as ignored managed objects. The primary reason is clear responsibility boundaries; a smaller getter-core attack surface is a beneficial side effect. This keeps repository layout structured while relying on the repository trust boundary: users should only use repositories they trust, or copy/author content into repositories they control.

Package `metadata.jsonc` declares permissions per enabled Lua file, for example that `9999.lua` or a fixed-version script needs `allow_free_network`. The permission system is used when getter runs Lua and when getter/UI displays enabled version Lua to the user. The `lua` map is lookup-only: getter first discovers an enabled Lua file from the filesystem, then queries this map by basename. Getter does not enumerate the map to discover scripts or warnings. Dot-prefixed Lua files are excluded from Lua discovery, so permission metadata for them is inert and cannot enable them. Entries for nonexistent files are also inert. `9999.lua` commonly has that permission, but it is not high-risk merely by filename if metadata does not grant it; fixed-version scripts can also be high-risk when metadata grants free network. A version script omitted from the `lua` map defaults to `permission: []`.

`luaclass/` contains reusable Lua modules. These are conceptually like eclasses, but the project does not introduce an `eclass` keyword or syntax.

`.metadata/autogen/` contains repository-level autogen metadata and scripts. Autogen scripts may use `luaclass/` helpers and can generate or refresh package directories from structured upstream inputs. Each generated package directory stores its own getter-managed generation record as `.autogen.jsonc`, listing generated files, file hashes, generator/template identity, and input facts needed to decide whether the package can be refreshed or cleaned. Hashes recorded in `.autogen.jsonc` are generated-output ownership/tamper-detection facts only: they answer whether a file is still the file getter generated earlier, not whether it is trusted, repository-signed, or valid as external-download content. The `.autogen.jsonc` `files` map covers getter-written generated output such as `metadata.jsonc`, `Manifest`, generated Lua scripts, and generated `files/...` helper files; it does not include `.autogen.jsonc` itself, avoiding self-referential hashing. The generated repository is generated output: getter may overwrite package directories it previously generated only when matching `.autogen.jsonc` proves ownership. When refresh/overwrite ownership checks pass, getter clears the existing generated package directory contents, then writes the new generated contents into the same package directory, without preserving old unlisted extra files. If clearing any old file or subdirectory fails, the whole refresh/overwrite fails rather than being ignored. If writing new generated contents fails after clearing, the operation fails directly without rollback; the directory may be empty or partially written, and the next refresh continues by clearing and rewriting again. If a target package directory exists without a matching generation record, apply reports a conflict and does not overwrite it. A generated-repo package directory missing `.autogen.jsonc` is a conflict rather than something getter automatically claims. If `.autogen.jsonc` exists but is malformed or schema-invalid, ordinary package discovery/evaluation is still decided by `metadata.jsonc`, but ownership-dependent autogen refresh/apply/cleanup/overwrite reports a conflict and does not auto-fix, overwrite, or delete it. When cleanup ownership checks pass, cleanup clears the generated package directory contents directly, including `.autogen.jsonc` and any unlisted extra files inside it, but does not delete the package directory itself. If clearing any file or subdirectory fails, the whole cleanup/update fails rather than being ignored. Getter does not classify or preserve unlisted extra files in generated package directories because they are outside getter's domain; direct directory-content clearing is simpler and more stable for generated output. User-authored overrides belong in `repo/local/...`, not by hand-editing `repo/autogen/...`.

Getter preserves user-controlled transparent URL replacement through local hook scripts under `rc/hook/`, analogous to an emerge bashrc-style hook and UpgradeAll's older URL replacement behavior. Hooks are getter-local runtime policy discovered only from the filesystem: list enabled `rc/hook/*.lua`, exclude dot-prefixed basenames, sort deterministically, and load before package version scripts, repository-level autogen scripts, and `luaclass/` code. There is no hook registry, metadata map, or disabled-hook state. Dot-prefixed Lua files are excluded from hook Lua discovery and are not hook entries. Hook scripts can wrap getter-exposed Lua host entrypoints such as `http_get()` or `read_package_file()` and call the original unhooked getter-internal entrypoint through `getter_builtin.<name>`, for example `getter_builtin.http_get()` or `getter_builtin.read_package_file()`, after rewriting the URL or applying local policy. Plain package evaluation does not install `http_get`; provider/runtime operations that need network access must deliberately install an HTTP transport and own permission, Manifest, provider, cache, and diagnostic policy for that execution. Getter core/CLI does not maintain a protective denylist of hookable public functions; if extra guardrails are needed, they belong in UI/UX policy rather than the getter core. `getter_builtin.*` is an internal escape hatch for hook code; ordinary package/autogen Lua should use the public hooked names instead. Hooks are an execution overlay and must not mutate repository source files. Hook loading is fail-closed for enabled hooks: parse/load/initialization failure fails the current Lua execution instead of silently falling back to unhooked functions. URL rewrites can point requests at mirrors, proxies, or local replacement endpoints, but for package version scripts without `allow_free_network` the returned body still must match a package `Manifest` hash.

## Repository priority

Default priority convention in `repo/metadata.jsonc`:

```jsonc
{
  "version": 1,
  // Autogen writes to "autogen" by default. Uncomment and change this
  // if generated packages should target another existing repository alias.
  // "generated_repository": "autogen",
  "priority": {
    "local": 100,
    "official": 0,
    "autogen": -1
  }
}
```

The user may edit priorities through UI, CLI, or the getter-owned root metadata file. The only hard rule is: higher priority wins when resolving an unqualified package path. If `repo/metadata.jsonc` is missing, getter uses built-in priority defaults: `local` = 100, `autogen` = -1, every other alias = 0, with same-priority aliases resolved in lexicographic order. If present, the priority map is lookup-only: getter discovers actual repository alias directories first, then queries the map by alias; entries for nonexistent aliases are inert and do not warn, create repositories, display repositories, or participate in sorting. The generated repository target defaults to `generated_repository = "autogen"` when omitted, and starter config should show this default as a comment users can uncomment/change. When autogen runs with target `autogen`, getter creates `repo/autogen/` if needed. If `generated_repository` is set to any other alias, that repository directory must already exist or autogen apply reports a configuration error. `generated_repository` only decides autogen output target and normal package resolution still uses repository priority. If `repo/metadata.jsonc` exists but cannot be parsed, getter reports a configuration diagnostic instead of silently falling back. A qualified atom such as `android/app/org.fdroid.fdroid::official` resolves only the named local repository alias.

## Import and override

Reusable Lua modules should use Lua import helpers where practical:

```lua
local github_android = require("luaclass.github_android_apk")
```

`luaclass.*` imports resolve only from the active package repository's own `luaclass/` directory and then from getter-shipped built-in fallback modules. Cross-repository `luaclass` lookup is intentionally unsupported: a package in `repo/official` does not load modules from `repo/local`, `repo/autogen`, or another alias by priority or by explicit alias. Shared behavior needed by generated packages should either live in getter-shipped built-ins or be authored/copied into that package's active repository.

Parent package imports should use package atoms rather than raw file paths:

```lua
local base = package_from("android/app/org.fdroid.fdroid::official")
```

Override is a Lua helper/metatable concern, not a Rust API concern. Rust validates only the final returned data object.

## Consequences

Positive:

- App identity is readable and user-supportable.
- Multiple sources become package internals rather than top-level user confusion.
- Users can maintain patch stacks by overriding individual package directories/version scripts.
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
