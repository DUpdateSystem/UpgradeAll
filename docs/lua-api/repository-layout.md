# Lua Repository Layout

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

Recommended getter data directory layout:

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

## Repository root metadata

`repo/metadata.jsonc` is getter-owned repository registry/config state, not part of any one package repository. It stores repository-related local settings only, currently repository ordering and generated-output target rules:

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

The user may change priority through UI, CLI, or this getter-owned metadata file. Higher priority wins for unqualified package atoms. If `repo/metadata.jsonc` is missing, getter uses built-in priority defaults: `local` = 100, `autogen` = -1, every other alias = 0, with same-priority aliases resolved in lexicographic order. If present, the priority map is lookup-only: getter discovers actual repository alias directories first, then queries the map by alias; entries for nonexistent aliases are inert and do not warn, create repositories, display repositories, or participate in sorting. The generated repository target defaults to `generated_repository = "autogen"` when the field is omitted. When an autogen operation runs with target `autogen`, getter creates `repo/autogen/` if it does not already exist. If `generated_repository` is set to any alias other than `autogen`, that target repository directory must already exist; otherwise autogen apply reports a configuration error instead of creating it. `generated_repository` only decides autogen output target and does not participate in package resolution except through the normal priority map. If `repo/metadata.jsonc` exists but cannot be parsed, getter reports a configuration diagnostic instead of silently falling back. Upstream repository updates must not overwrite this file. Hook/runtime policy is not stored here.

## Runtime configuration root

`rc/` is the getter local runtime/config policy root. It is a top-level sibling of `repo/` under the getter data directory, beside storage files such as `main.db` and `cache.db`. It is not a repository root and does not participate in repository or package discovery. Current defined content is `rc/hook/*.lua` for runtime hook policy. Future runtime/local policy such as environment, credential, or network behavior belongs under `rc/`, not in `repo/metadata.jsonc`, which remains repository-related registry/config.

## Runtime hooks

Runtime/local hooks live under `rc/hook/`, not under `repo/`. Hooks are runtime policy, while `repo/metadata.jsonc` stays repository-related configuration. Hooks are discovered only from the filesystem: getter lists enabled `rc/hook/*.lua` files, excludes basenames starting with `.`, sorts them deterministically, then loads them before every Lua execution environment. There is no hook registry, metadata map, or persistent disabled-hook state. Enabled hooks can wrap getter-exposed Lua host functions such as `http_get()` for transparent URL replacement or similar local policy. A dot-prefixed Lua file is outside getter management and is not a hook entry.

## Repository alias

`repo/metadata.jsonc` is the only current repo-root reserved entry. Every direct child directory of `repo/` is a local repository alias. For example, `repo/official` has alias `official`. If the user clones or renames the official repository to `repo/a`, the local alias is `a`. Renaming is an intentional local rule change that lets users fork, replace, or interpose repository layers. Future repo-root reserved entries require an explicit design/ADR because they occupy alias namespace; no names such as `hook` or `rc` are pre-reserved under `repo/`.

Repository self-metadata lives at `.metadata/metadata.jsonc` and records schema version, upstream hosting URL, description, maintainers/co-maintainers, and repository signature/trust information. Security/trust checks use this verified repository metadata, not the local alias. If `.metadata/metadata.jsonc` is missing, the repository may still be used as unverified/local-source content, but repo update, signature, and trust operations are unavailable. If `.metadata/metadata.jsonc` exists but cannot be parsed, getter reports a repository metadata diagnostic. A directory named `official` is not trusted merely because of its name.

Inside a repository alias directory, getter only considers explicit entries: reserved repository-root directories such as `.metadata/` and `luaclass/`, plus directory chains that form package paths. Reserved directories are handled only by their own responsibility and never participate in package discovery; package paths cannot begin with reserved names such as `.metadata` or `luaclass`. Future repository-root reserved directories follow the same rule so the repo layout remains organizable. A directory that directly contains `metadata.jsonc` declares a package boundary. `.autogen.jsonc` does not declare a package boundary; it is only a generated-package ownership record inside a package directory that already has `metadata.jsonc`. If `metadata.jsonc` parses correctly as package metadata, the directory is a valid package directory; if parsing fails, getter reports an invalid package metadata diagnostic for that package path. In both cases, that directory is the package path endpoint and getter does not discover nested packages below it. Other files or directories such as `README.md`, `docs/`, or random helper files are outside getter domain entirely: they are not parsed, validated, displayed, warned about, or modeled as ignored managed objects. The primary reason is clear responsibility boundaries; a smaller getter-core attack surface is a beneficial side effect.

Package version Lua resolves `require("luaclass.<name>")` from the active package repository's `luaclass/` directory first, then from getter-shipped built-in standard modules. Repository-local modules intentionally override built-in standard modules. Generated repositories do not need to copy shared standard modules into repository-root `luaclass/`, and getter does not introduce repo-level autogen ownership records for shared modules; `.autogen.jsonc` remains package-local.

## Package directories

Package directories are final package definitions consumed by getter. UpgradeAll/getter domain strings, including package paths and aliases, are treated as UTF-8. Getter does not detect or convert other filesystem/text encodings; inputs in other encodings are still interpreted as UTF-8.

Package path is derived from the directory path relative to the repository:

```text
repo/official/android/app/org.fdroid.fdroid -> android/app/org.fdroid.fdroid
repo/official/android/f-droid/magisk/hello -> android/f-droid/magisk/hello
```

Package references use qualified atoms:

```text
android/f-droid/magisk/hello
android/f-droid/magisk/hello::official
```

If `::repo-name` is omitted, getter resolves by `repo/metadata.jsonc` priority. If it is present, getter resolves only that local repository alias.

## Package metadata

`metadata.jsonc` stores package-level metadata. Android install identity uses `android.package_name`, not an ambiguous `package_id` field. Lua script permissions are declared per file:

```jsonc
{
  "type": "android:app",
  "android": {
    "package_name": "com.example.app"
  },
  "homepage": "https://example.com",
  "description": "...",
  "lua": {
    "9999.lua": {
      "permission": ["allow_free_network"]
    }
  }
}
```

`allow_free_network` is the source of the user-visible high-risk warning. The `lua` map is lookup-only: getter first discovers an enabled Lua file from the filesystem, then queries this map by basename. Getter does not enumerate the map to discover scripts or warnings. `allow_free_network` can apply to `9999.lua` or to fixed-version scripts. `9999.lua` commonly needs it, but the filename alone does not grant free network or force the warning if metadata explicitly does not grant that permission. A version script omitted from the `lua` map defaults to `permission: []`. Entries for nonexistent files or dot-prefixed Lua files are inert and do not enable or display anything.

Package metadata has no separate schema version field; the repository metadata version covers the repository content schema.

## Version scripts

Fixed package versions live directly in the package directory as `<version>.lua`:

```text
1.2.3.lua
1.2.3-r1.lua
v1.2.3.lua
2026.06.25.lua
```

Getter discovers package version scripts only from direct child files of the package directory whose basename ends with `.lua` and does not start with `.`. Removing the `.lua` suffix yields the literal version string; getter does not require SemVer or otherwise constrain the version syntax at discovery time.

Live/floating package behavior uses the special script name:

```text
9999.lua
```

Every enabled Lua script must specify the interpreter/API version on the first line. This version is required and has no implicit default. Lua file discovery excludes any Lua file whose basename starts with `.`, for example `.9999.lua`: getter does not parse, validate, execute, display, or apply permission metadata to it as Lua. This dot-prefix rule is only for Lua file discovery; non-Lua package files are governed by the explicit getter file whitelist, so `.autogen.jsonc` is managed when present in a generated package. Script permissions still come from package `metadata.jsonc`, not from the shebang:

```lua
#!/bin/upa-lua v1
```

## Package-local files

A package directory may contain a `files/` subdirectory for package-local helper data:

```text
files/
  helper-data.json
  patch.diff
```

Package Lua may read files under its own package directory's `files/` subtree through a package-scoped getter host API such as:

```lua
local body = read_package_file("helper-data.json")
```

The path is relative to `files/`. The original built-in implementation, `getter_builtin.read_package_file`, does not expose real filesystem paths or a general `io.open` escape hatch. It rejects absolute paths, `..`, directory reads, cross-package reads, and arbitrary repository reads. `read_package_file(path)` returns a Lua string; getter does not interpret encoding, MIME type, JSON, or text-vs-binary mode. Hook code may still wrap the public `read_package_file()` name as local user policy; getter core/CLI does not maintain a protective denylist of hookable public functions. Getter does not assign product semantics to file names or formats inside `files/`; the package owns them.

This keeps repository layout structured while preserving the repo-trust model: repository source review/signing covers these helper files, and users who do not trust a repository should not use it except by copying/authoring content into a repository they control.

Package directory contents outside getter's explicit discovery set are outside getter domain entirely. Getter only considers `metadata.jsonc`, optional generated-package `.autogen.jsonc`, `Manifest`, enabled direct-child `*.lua`, and `files/`. Other files or directories are not parsed, validated, displayed, warned about, or modeled as ignored managed objects. The primary reason is clear responsibility boundaries; a smaller getter-core attack surface is a beneficial side effect.

## Manifest

`Manifest` stores allowed external network/dynamic-download response-body hashes for package version scripts:

```text
<sha512> [optional-name]
```

The hash is authoritative. The optional name is for humans/debugging only; a URL may not reveal the actual returned file name, so getter validates by response-body SHA-512 membership, not by file name.

A missing `Manifest` is equivalent to an empty hash set, not an invalid package. For package version scripts without `allow_free_network`, any external network/dynamic-download data file or API response body used by that script must hash to one of the entries in that package's `Manifest`. If the response hash is absent or mismatched, getter rejects the download/use, so a missing/empty `Manifest` means such network fetches cannot succeed. Scripts that do not fetch external network content, or only read package-local `files/`, do not need a `Manifest`. Scripts with `allow_free_network` are not blocked by `Manifest` membership but remain high-risk.

`Manifest` belongs only to package directories and package version Lua execution. Repository-level autogen scripts under `.metadata/autogen/` do not have a `Manifest`, but an autogen script that creates package directories must also generate correct package `Manifest` files for generated packages that are expected to work without `allow_free_network`.

`Manifest` is not a repository source manifest. It does not protect `metadata.jsonc`, Lua version scripts, package `files/`, sibling `.autogen.jsonc`, `luaclass/`, repository metadata, or autogen scripts; those source files are protected by the repository Git/signing/maintainer trust model. Since `Manifest` and `.autogen.jsonc` are same-level package files, `Manifest` cannot architecturally protect `.autogen.jsonc`. It also does not make `allow_free_network` or `9999.lua` live upstream behavior reproducible or safe.

## luaclass/

Reusable Lua modules. These are conceptually similar to eclasses but are plain Lua modules.

```lua
local github_android = require("luaclass.github_android_apk")
```

Package version Lua resolves `require("luaclass.<name>")` from the active package repository's `luaclass/` directory first, then from getter-shipped built-in standard modules. Repository-local modules intentionally override built-in standard modules, so a trusted repository or `local` overlay can replace the shipped default behavior in normal source form.

Cross-repository `luaclass` imports are not supported in this model. A package in `repo/official` does not load modules from `repo/local`, `repo/autogen`, or another alias by priority or by explicit alias. If shared behavior is needed for generated packages, it should either live in getter-shipped built-in modules or be copied/authored into the active repository's own `luaclass/` tree.

## Getter hook scripts

Getter preserves user-controlled transparent URL replacement through local hook scripts under `rc/hook/`, analogous to an emerge bashrc-style hook and UpgradeAll's older URL replacement behavior.

Hooks are global getter-local runtime policy. Getter discovers hooks only from the filesystem: list enabled `rc/hook/*.lua` files, exclude basenames starting with `.`, sort deterministically, then load before every Lua execution environment. For example, enabled files load as `00-env.lua`, `10-http-rewrite.lua`, then `20-headers.lua`. A Lua file whose basename starts with `.` is excluded from hook Lua discovery, so `.10-http-rewrite.lua` is not parsed, validated, loaded, displayed, or treated as a hook entry. There is no hook registry, metadata map, or disabled-hook state.

A hook can wrap visible Lua host functions such as `http_get()` or `read_package_file()` and call the original getter-internal entrypoint after rewriting the URL or applying local policy. Getter core/CLI does not maintain a protective denylist of hookable public functions; if extra guardrails are needed, they belong in UI/UX policy rather than the getter core. Original unhooked host entrypoints are available to hook code as `getter_builtin.<name>`, for example:

```lua
local upstream_http_get = getter_builtin.http_get

function http_get(url, opts)
  local rewritten = rewrite_url(url)
  return upstream_http_get(rewritten, opts)
end
```

Other host functions can use the same pattern when getter exposes a stable hook seam. `getter_builtin.*` is an internal escape hatch for hook code; ordinary package/autogen Lua should use the public hooked names instead. The hook layer affects package version scripts, repository-level autogen scripts, and `luaclass/` code through their calls to wrapped host functions, but it is not a mechanism for modifying repository source files.

Hook loading is fail-closed for enabled hooks. If any enabled hook fails to parse, load, or initialize, getter fails the current Lua execution instead of warning and continuing with unhooked functions. Dot-prefixed Lua files are excluded from Lua discovery, not modeled as disabled hook objects. This intentionally fails “stupidly” rather than pretending success when a user proxy/security policy did not load.

Hook rewriting does not by itself trust the returned content. For package version scripts without `allow_free_network`, the fetched response body still must match a `Manifest` hash. For scripts with `allow_free_network`, getter/UI surfaces the configured high-risk permission.

## .metadata/autogen/

Repository-level autogen metadata and Lua scripts live under `.metadata/autogen/`.

`metadata.jsonc` describes the autogen scripts, for example which file handles Android app discovery or Magisk module discovery. Autogen scripts can use `luaclass/` helpers and can include trust/signature verification logic such as pinned GPG public keys where appropriate.

Each generated package directory stores its own getter-managed generation record as `.autogen.jsonc`. That package-local record lists the generated files, file hashes, generator/template identity, and input facts needed to decide whether the package can be refreshed or cleaned. The recorded file hashes are ownership/tamper-detection facts for generated output: they answer whether a file is still the file getter generated earlier, not whether it is trusted, repository-signed, or valid as external-download content. The `files` map covers getter-written generated output such as `metadata.jsonc`, `Manifest`, generated Lua scripts, and generated `files/...` helper files; it does not include `.autogen.jsonc` itself, avoiding self-referential hashing. Keeping the generation record beside the generated package avoids slow repository-level reverse lookup during cleanup. A generated-repo package directory without `.autogen.jsonc` is treated as a conflict: getter does not automatically claim, overwrite, or delete it; without `.autogen.jsonc`, there is no ownership proof. If `.autogen.jsonc` exists but is malformed or schema-invalid, package discovery/evaluation is still decided by `metadata.jsonc`, but autogen refresh/apply/cleanup/overwrite reports a generated-ownership conflict and does not auto-fix, overwrite, or delete it. When refresh/overwrite ownership checks pass, getter clears the existing generated package directory contents, then writes the new generated contents into the same package directory, without preserving old unlisted extra files. If clearing any old file or subdirectory fails, the whole refresh/overwrite fails rather than being ignored. If writing new generated contents fails after clearing, the operation fails directly without rollback; the directory may be empty or partially written, and the next refresh continues by clearing and rewriting again. When cleanup ownership checks pass, cleanup clears the generated package directory contents, including `.autogen.jsonc` and any unlisted extra files inside it, but does not delete the package directory itself. If clearing any file or subdirectory fails, the whole cleanup/update fails rather than being ignored. Getter does not classify or preserve unlisted extra files in generated package directories because they are outside getter's domain; direct directory-content clearing/replacement is simpler and more stable for generated output.

## Offline validation

Use getter's structured validator before publishing or registering a repository:

```bash
getter --data-dir /tmp/ua-getter repo validate /path/to/repo
```

The command does not require the repository to be registered and does not use the network. It checks the local layout, repository metadata, package path derivation, absence of duplicate package `id` declarations, required Lua API-version shebangs, constrained Lua evaluation, manifests, and Rust schema/domain validation. Results are returned as JSON with `valid`, `package_count`, `network_required`, and getter-owned `diagnostics`.

Common diagnostic codes include `repository.missing_directory`, `repository.unsupported_api_version`, `package.lua_runtime`, `package.schema`, and `package.domain`.
