# ADR-0012: Getter-owned provider modules, autogen, and metadata refresh

> Status: Draft
> Date: 2026-06-25
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Context

ADR-0001 replaces the old hub-app model with package-centric Lua repositories and overlays. ADR-0002 keeps product/domain logic in Rust getter and limits Flutter/Kotlin to UI and platform-adapter roles. ADR-0010 accepts `cache.db` package metadata/provider-source caching, live/floating version rules, and `pin_version` semantics. ADR-0011 accepts getter-owned update runtime actions/tasks and requires package Lua to use getter-owned provider host APIs by default rather than arbitrary raw HTTP.

The first accepted Phase D runtime implementation uses static Lua `updates` through a mock provider boundary. That path is useful scaffolding but is not the real live-provider design.

Old-code archaeology shows that the legacy Android/Kotlin implementation had a generic Hub/WebSDK/cloud-config/RPC provider shape rather than local handwritten F-Droid or GitHub parser classes. The old local Kotlin `BaseHub` supported both a batch latest-update shape and a per-app release-list shape:

```kotlin
getUpdate(hub, appList)      // batch-ish, many apps
getReleases(hub, app)        // one app/project release list
```

The old shared DTOs were similarly generic:

```text
ReleaseGson(version_number, changelog, assets, extra)
AssetGson(file_name, file_type, download_url)
DownloadItem(name, url, headers, cookies)
```

Those historical abstractions are useful evidence, but the rewrite must not revive the old Hub UUID/map model. The rewrite needs a package-centric provider model that supports two first target families:

- **F-Droid**: a structured catalog/index provider where one provider endpoint can discover metadata for many Android apps.
- **GitHub**: a project/release provider where a package usually names one upstream project and may need package-authored asset/version rules.

The product goal for F-Droid is autogen-first: F-Droid apps should normally appear as ordinary generated package directories/version scripts, whether the user explicitly chooses one F-Droid app or getter auto-discovers installed apps covered by F-Droid. Upstream repositories and local users may still hand-write F-Droid package directories/version scripts when they need richer behavior or overrides.

## Decision

UpgradeAll will implement live provider support through **getter-owned standard provider modules** plus **getter-owned autogen pipelines**.

F-Droid and GitHub are providers/sources/backends, not package identities and not UpgradeAll repositories.

### Core decision

1. Getter owns live provider execution, provider/source caching, package metadata normalization, update selection, action issuance, and autogen/package-path decisions.
2. Reusable Lua provider modules/classes under `luaclass/` provide high-level package-authoring APIs for common provider families.
3. Those Lua modules call getter-owned host APIs by default. Getter-shipped standard provider modules use provider-specific host functions such as `getter.provider.fdroid.update_candidates(...)` and `getter.provider.github.release_candidates(...)` so Rust owns provider parsing, cache consistency, diagnostics, and candidate normalization. Generic/custom Lua HTTP remains available through a getter-managed host function such as `http_get(url, { headers = ..., cache = true|false })`; `cache` defaults to `false`, and Lua opts individual generic HTTP requests into HTTP source caching by passing `cache = true`. Plain package evaluation does not install `http_get` or provider host APIs; the getter operation/runtime evaluating provider-backed Lua deliberately installs the transport/host functions and owns permission, Manifest, provider, cache, and diagnostic policy for that execution. This remains getter-owned network/cache execution, not Flutter/Kotlin HTTP and not a Lua standard-library network primitive.
4. F-Droid support is **autogen-first**:
   - an F-Droid app is represented as an ordinary package directory with metadata and version scripts;
   - explicit user selection of an F-Droid app uses a getter autogen preview/apply operation that generates a package directory/version script;
   - automatic installed-app discovery uses the same autogen machinery;
   - generated F-Droid package directories normally live in the configured generated repository alias, defaulting to `autogen`, and obey normal repository priority/overlay rules;
   - official/community/local repositories may still contain hand-written F-Droid package directories/version scripts.
5. GitHub support is **standard-module-first, hand-authored package by default**:
   - package authors normally write one package file per GitHub project;
   - a standard GitHub Lua module should require only typed project coordinates for common cases, such as `owner` and `repo`;
   - getter may provide a narrow assisted Android APK autogen path that writes those standard-module package directories from explicit typed project coordinates and a provider snapshot/cache, but this is not a global GitHub catalog/search model;
   - the module provides release, tag/resource, and live latest-commit helper behavior through getter provider host APIs;
   - GitHub global search/catalog autogen is not part of the first accepted provider model.
6. Both families normalize into the same getter-owned candidate/artifact metadata model before version comparison and action issuance.
7. Provider/source caches and normalized package metadata caches live in `cache.db`; generated package directories/version scripts, package `files/` helper data, and package-local `.autogen.jsonc` generation records are repository source artifacts, not cache entries.
8. Runtime action/task state remains process-memory only per ADR-0011 and is not stored in `main.db` or `cache.db`.

## Terminology

To avoid drifting back into the old hub-app model, ADR-0012 uses these terms strictly:

- **Repository root**: the local `repo/` directory that contains all enabled UpgradeAll repositories and repository-related local registry/config metadata in the sole current repo-root reserved file, `repo/metadata.jsonc`. Every direct child directory of `repo/` is a repository alias. Future repo-root reserved entries require an explicit design/ADR because they occupy alias namespace. Runtime/local policy lives outside `repo/` under `rc/`.
- **Runtime configuration root**: the local `rc/` directory for getter runtime/local policy. It is a top-level sibling of `repo/` under the getter data directory, beside storage files such as `main.db` and `cache.db`. It is not a repository root and does not participate in package/repository discovery. Current defined content is `rc/hook/*.lua`; future runtime/local policy such as environment, credential, or network behavior belongs under `rc/`, not in `repo/metadata.jsonc`.
- **UpgradeAll repository**: a package repository rooted at `repo/<repo-name>`, where `<repo-name>` is the local repository alias. Getter considers explicit repository entries: reserved repository-root directories such as `.metadata/` and `luaclass/`, plus directory chains that form package paths. Reserved directories are handled only by their own responsibility and never participate in package discovery; future reserved directories follow the same rule so the repo layout remains organizable. Other repository contents are outside getter domain entirely, not ignored managed objects. The local alias is a user-controlled reference/priority name, not the repository's security identity. Repository self metadata lives at `.metadata/metadata.jsonc`; if it is missing, the repository may still be used as unverified/local-source content, but repo update, signature, and trust operations are unavailable; if it exists but cannot be parsed, getter reports a repository metadata diagnostic. Repository source files are protected by the repository Git/signing/maintainer trust model, not by package `Manifest`; package `Manifest` only manages external network/dynamic-download content fetched at package-version execution time.
- **Package path**: a repository-local package identity derived from package directory hierarchy, such as `android/app/org.fdroid.fdroid` or `android/f-droid/magisk/hello`. UpgradeAll/getter domain strings, including package paths and aliases, are treated as UTF-8; getter does not detect or convert other filesystem/text encodings, and inputs in other encodings are still interpreted as UTF-8.
- **Qualified package atom**: `<package-path>[::repo-name]`; omitting `::repo-name` resolves by repository priority, specifying it resolves only that local alias. If `repo/metadata.jsonc` is missing, built-in priority defaults are `local` = 100, `autogen` = -1, all other aliases = 0, with same-priority aliases resolved in lexicographic order. If present, the priority map is lookup-only: getter discovers actual repository alias directories first, then queries the map by alias; entries for nonexistent aliases are inert and do not warn, create repositories, display repositories, or participate in sorting. `generated_repository` defaults to `autogen` when omitted, and starter config should include that default as a comment users may uncomment/change. When autogen runs with target `autogen`, getter creates `repo/autogen/` if needed; any non-`autogen` target must already exist or autogen apply reports a configuration error. `generated_repository` only decides autogen output target and does not participate in package resolution except through the normal priority map. If `repo/metadata.jsonc` exists but cannot be parsed, getter reports a configuration diagnostic instead of silently falling back.
- **Provider endpoint/catalog**: an upstream service or index, such as the official F-Droid catalog endpoint or the GitHub API endpoint for a repository. It is not an UpgradeAll repository.
- **Package source**: a source declaration inside one package definition/version script that uses a provider module to discover candidates/artifacts.
- **Reusable Lua provider module/class**: a Lua helper under `luaclass/`, such as `luaclass.fdroid_android` or `luaclass.github_android_apk`, that fills common lifecycle behavior and calls getter provider host APIs. Getter may ship standard `luaclass.*` modules as a built-in fallback module root; repository-local `repo/<alias>/luaclass/` modules resolve first so repositories can override or extend the shipped defaults without generated repositories owning shared module files.
- **Autogen pipeline**: a getter operation and repository-level `.metadata/autogen/` Lua helper that previews and writes package directories/version scripts, Manifests, optional package-local `files/` helper data, and a package-local `.autogen.jsonc` generation record from structured inputs. Autogen output is ordinary repository content.
- **Provider/source cache**: cache.db entries for upstream facts, API responses, indexes, freshness tokens, and parsed provider facts.
- **Package metadata cache**: cache.db entries for normalized package metadata/candidates/artifacts produced by evaluating a package's Lua dependency closure.

## F-Droid model

F-Droid is treated as a structured Android catalog provider endpoint. Its product support is autogen-first.

### Standard `luaclass` module resolution

Package version Lua resolves `require("luaclass.<name>")` in this order:

1. the active package repository's `luaclass/` directory, e.g. `repo/official/luaclass/fdroid_android.lua`; then
2. getter-shipped built-in standard modules.

Repository-local modules deliberately win over getter-shipped modules. This gives trusted repositories and `local` overlays a normal source-level override path while keeping generated repositories boring: generated package directories do not need to copy shared standard modules, and `.autogen.jsonc` remains package-local ownership proof rather than a repository-root ownership system. Built-in modules are part of the getter binary/source distribution and are not repository source files; repository trust/signing still applies only to repository-provided files. Cross-repository module lookup is not accepted in this slice because it would make package behavior depend on unrelated repository priority and trust boundaries.

Implementation status: getter ships `luaclass.android`, `luaclass.fdroid_android`, and `luaclass.github_android_apk` as built-in fallback modules. The provider-named modules call the stable `getter.provider.*` host namespace and require a provider-backed operation to install those host functions; plain package evaluation does not install them and fails with a stable host-unavailable error if a package calls a provider module there. The product runtime update-check operation `update_check_package_issue_action` installs the stable provider host and runtime hooks, then evaluates package Lua before update selection, so provider-backed packages can issue normal getter-owned update actions from Manifest-compatible provider cache entries. The Flutter/native payload remains package/update oriented (`package_id`, optional `repository_id`, `installed_version`, and `pin_version`); provider fixture bodies, cache mode, endpoint URLs, and live transport controls are not bridge request fields. Generated F-Droid output now uses `luaclass.fdroid_android` for the default official endpoint, writes provider source response SHA-512 entries into `Manifest`, and remains usable through provider-backed update checks rather than plain read-model package evaluation. GitHub releases now have getter-owned live REST transport/cache refresh at the provider/runtime host layer in addition to snapshot/fixture refresh for deterministic tests. F-Droid product cache bootstrap still refreshes the default official F-Droid provider cache from a getter/api-proxy-owned bundled catalog source behind a narrow `data_dir`-only bridge request until a later accepted F-Droid live transport slice; Dart/Kotlin do not supply `index_xml`, endpoint URL, cache mode, raw provider payloads, or live transport controls.

### Stable provider host API v1 direction

The stable provider host namespace is rooted at `getter.provider.*`:

```lua
getter.provider.fdroid.update_candidates(spec)
getter.provider.github.release_candidates(spec)
-- reserved for an explicit live/floating operation, not installed by the default release module:
getter.provider.github.latest_commit(spec)
```

Host functions installed for package Lua are also exposed to runtime hooks through matching originals under `getter_builtin.provider.*`. Ordinary package Lua and `luaclass/` code should call the public `getter.provider.*` functions; `getter_builtin.*` is an escape hatch for local `rc/hook/*.lua` policy.

F-Droid `update_candidates` takes a required `package_name` and optional `endpoint_id`. GitHub `release_candidates` takes required typed `owner` and `repo`, optional Rust-regex asset filters (`asset.include` / `asset.exclude`), optional `include_prereleases`, and optional `endpoint_id`. The package Lua API keeps typed coordinates even if a Lua helper later accepts shorthand authoring syntax. If package Lua supplies an `endpoint_id` that the evaluating operation has not installed or resolved, the provider host call fails instead of silently using a default endpoint.

Candidate-returning provider functions return an envelope containing non-empty `candidates`, provider cache `source` (`cache`, `refreshed`, or `stale`), `cache_key`, and getter-owned `diagnostics`. Zero-candidate provider results use `candidates = nil`/omitted plus diagnostics rather than an empty Lua table, because the current Lua-to-JSON boundary serializes empty Lua tables as objects, not arrays. Standard modules pass `result.candidates` through to `package_version { updates = ... }`, so nil/omitted candidates become an omitted `updates` field and validate as no updates.

The GitHub `latest_commit` host shape is reserved because latest-commit checks are live/floating behavior. The default GitHub release/APK helper must not install or call it by default, and latest-commit results must not be silently treated as ordinary release candidates. A later explicit live operation/helper may install and use it after live-version UI/CLI semantics are implemented.

Provider functions must not bypass Manifest policy. For package scripts without `allow_free_network`, every external response body used to produce provider facts must match a package `Manifest` SHA-512 entry. Parsed provider cache hits are usable for non-free scripts only when cache provenance records the source response digest(s) and proves Manifest compatibility; missing provenance must fail closed or refetch/revalidate. Provider refreshes store source response SHA-512 digest(s), a provenance schema version, and freshness metadata with parsed provider cache entries so Manifest-compatible cache hits can be accepted while legacy/missing-provenance rows still fail closed. GitHub release live refresh uses the same rule: non-free package scripts may consume a live-refreshed response only if the response body hash is already in that package Manifest; scripts declaring `allow_free_network` may use the live response without Manifest membership but remain high-risk. Runtime update-check bridge payloads still do not carry provider fixture bodies, cache modes, endpoint URLs, or transport controls; provider refresh/cache population is owned by getter operations rather than Flutter/Kotlin.

### F-Droid reusable module

The common package-authoring API should be intentionally small. The default case should need only the Android/F-Droid package name:

```lua
local fdroid = require("luaclass.fdroid_android")

return fdroid.package {
  package_name = "org.fdroid.fdroid",
}
```

The package path is not duplicated inside the Lua table. Like emerge/ebuilds, getter derives package identity from the package directory path, such as `repo/official/android/f-droid/app/org.fdroid.fdroid` -> `android/f-droid/app/org.fdroid.fdroid`. A directory that directly contains `metadata.jsonc` declares a package boundary. `.autogen.jsonc` does not declare a package boundary; it is only a generated-package ownership record inside a package directory that already has `metadata.jsonc`. If `metadata.jsonc` parses correctly as package metadata, the directory is a valid package directory; if parsing fails, getter reports an invalid package metadata diagnostic for that package path. In both cases that directory is the package path endpoint, so getter does not discover nested packages below it. Package version scripts are discovered from direct child files named `<version>.lua` whose basename does not start with `.`; removing `.lua` yields the literal version string with no SemVer requirement at discovery time. Package version Lua may read helper data under its own package directory's `files/` subtree through a package-scoped host API such as `read_package_file(path)`; the original built-in rejects paths outside that subtree and returns a Lua string without encoding/MIME/JSON/text-vs-binary interpretation, but hook code may still wrap the public `read_package_file()` name because getter core/CLI does not maintain a protective denylist of hookable public functions. File names/formats inside `files/` are package-owned. Package directory contents outside getter's explicit discovery set (`metadata.jsonc`, optional generated-package `.autogen.jsonc`, `Manifest`, enabled direct-child `*.lua`, and `files/`) are outside getter domain entirely, not ignored managed objects; the primary reason is clear responsibility boundaries, with smaller getter-core attack surface as a beneficial side effect. F-Droid display metadata such as name and description comes from the F-Droid catalog; generated metadata/version scripts should not duplicate it unless a hand-written override intentionally does so. Generated content should stay boring: default generated F-Droid output is a minimal `metadata.jsonc`, `Manifest` entries for the provider source response digest(s), and a `9999.lua` that calls `luaclass.fdroid_android` with `package_name`. Large provider behavior belongs in reusable Lua classes plus getter host APIs, not in giant generated scripts.

The default `fdroid.package` class may infer common Android behavior:

- installed target is Android package `package_name`;
- the autogen generator writes to a package directory such as `android/f-droid/app/<package_name>/`, which derives the package path `android/f-droid/app/<package_name>`, unless an accepted future schema says otherwise;
- discovery reads F-Droid provider facts through getter host APIs;
- preparation normalizes F-Droid version name/code, changelog/metadata, and APK artifact descriptors;
- selection uses getter-owned version comparison and `pin_version` semantics.

The common class may allow optional typed fields when needed, for example:

```lua
return fdroid.package {
  package_name = "org.example",
  endpoint_id = "fdroid-official",
  channel = "stable",
}
```

`package_name` remains the common default because F-Droid is highly structured and self-describing. F-Droid provider endpoint names come from endpoint ids/directories controlled by getter/repository configuration; the endpoint URL defaults to official F-Droid but can be customized. The first generated-output migration emits only the default official endpoint shape and rejects custom generated `endpoint_id`/`endpoint_url` output until non-default endpoint configuration is accepted through the provider-backed runtime path. The model must not make third-party F-Droid endpoints, archive variants, signatures, anti-feature metadata, localized metadata, or channel-like preferences impossible to express later.

### F-Droid provider endpoint/catalog operations

Getter should expose provider operations equivalent to these conceptual capabilities:

1. **Catalog/index refresh/cache**: fetch, revalidate, or bootstrap F-Droid endpoint facts and store provider/source cache in `cache.db` with freshness metadata. The current product bootstrap for the default official endpoint uses a getter/api-proxy-owned bundled catalog source until live HTTP transport is accepted.
2. **Catalog query/lookup**: query cached/refreshed F-Droid catalog facts by package name, installed Android package names, or user search input. Getter owns the query semantics and DTOs.
3. **Autogen preview**: turn selected or discovered F-Droid package names into deterministic package-directory/package-output preview DTOs.
4. **Autogen apply**: write accepted package directories/version scripts to the configured generated repository alias (`generated_repository`, default `autogen`) and write a package-local `.autogen.jsonc` generation record. If the target is the default `autogen`, getter creates `repo/autogen/` at autogen runtime if needed; non-`autogen` targets must already exist.
5. **Package update check**: evaluate the generated or hand-written package Lua, call the F-Droid provider host API, normalize candidates/artifacts, compare versions, and issue getter-owned update actions.

The exact CLI/native operation names may be chosen during implementation, but stable product operations must preserve these boundaries.

### Explicit user selection flow

When a user explicitly wants an F-Droid app that is not already covered by a higher-priority package:

1. Flutter may pass a user search query, a user-entered upstream package name, or a getter-provided catalog item identifier to a getter operation.
2. Getter queries/refreshes the F-Droid catalog as needed and returns getter-owned result/preview DTOs.
3. Flutter renders those DTOs and asks for user confirmation.
4. Flutter submits only the accepted preview selection/package atoms back to getter.
5. Getter writes package directories/version scripts into the generated repository alias and writes a package-local `.autogen.jsonc` generation record.
6. Normal repository priority resolution decides which package definition is active.

The generated repository is generated output. Getter may overwrite package directories it previously generated only when matching `.autogen.jsonc` proves ownership. If a target package directory exists without a matching generation record, apply reports a conflict and does not overwrite it. A generated-repo package directory missing `.autogen.jsonc` is a conflict rather than something getter automatically claims. If `.autogen.jsonc` exists but is malformed or schema-invalid, ordinary package discovery/evaluation is still decided by `metadata.jsonc`, but ownership-dependent autogen refresh/apply/cleanup/overwrite reports a conflict and does not auto-fix, overwrite, or delete it. Users who want to hand-author or override generated behavior should create or edit `repo/local/...`, not hand-edit `repo/autogen/...`.

Flutter must not generate Lua, map F-Droid ids to UpgradeAll package paths/atoms, perform provider HTTP, parse F-Droid indexes, or decide whether an existing package should be shadowed.

If a higher-priority package already provides the target package path, the F-Droid autogen preview should report that the package is already covered or would be shadowed. If the user wants to override upstream behavior, they should create or edit a `local` package directory rather than expecting the generated repository to outrank official/community packages.

### Installed-app discovery flow

Installed-app autogen may use the F-Droid catalog as an enrichment source:

1. Rust/native bridge collects raw installed Android package facts through the platform adapter accepted in ADR-0009.
2. Getter matches installed package names against F-Droid catalog facts.
3. Getter previews generated F-Droid package directories/version scripts for accepted candidates.
4. Flutter renders getter-owned preview DTOs and returns user-accepted package atoms.
5. Getter writes ordinary generated package directories/version scripts and tracks accepted packages in `main.db` as already accepted by ADR-0006/ADR-0007.

F-Droid catalog matching must not move package-path normalization or autogen decisions into Flutter/Kotlin.

## GitHub model

GitHub is treated as a project/release provider, not a catalog-autogen source in the first ADR-0012 scope.

### GitHub reusable module

The standard GitHub module should make the common package easy to author with typed project coordinates:

```lua
local github_android = require("luaclass.github_android_apk")

return github_android.package {
  name = "F-Droid",
  android_package = "org.fdroid.fdroid",
  owner = "f-droid",
  repo = "fdroidclient",
  asset = {
    include = "[.]apk$",
    exclude = "debug",
  },
}
```

A common shorthand may accept `repo = "owner/name"`, but the normalized schema should keep typed `owner` and `repo` fields internally so validation, diagnostics, cache keys, and auth/rate-limit behavior are explicit.

The GitHub module should provide common capabilities through getter provider host APIs:

- release listing and latest release lookup;
- tag/resource lookup when a package chooses tag-based behavior;
- release asset discovery and filtering;
- changelog/release notes extraction;
- optional authenticated API access through getter-managed provider endpoint configuration;
- live latest-commit lookup for packages that explicitly opt into live/floating behavior.

### GitHub Android APK autogen

The first assisted GitHub package-generation path is explicit, project-scoped, and Android APK-specific. It takes typed project coordinates (`owner`, `repo`), the Android installed package name, optional display name, asset-name regex filters, and prerelease preference. It then writes an ordinary generated package directory such as:

```text
repo/autogen/android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall/
  metadata.jsonc
  Manifest
  9999.lua
  .autogen.jsonc
```

The generated `9999.lua` stays small and calls the standard module:

```lua
#!/bin/upa-lua v1
-- @generated by UpgradeAll getter autogen (GitHub release provider module)
local github_android = require("luaclass.github_android_apk")

return github_android.package {
  name = "UpgradeAll",
  android_package = "net.xzos.upgradeall",
  owner = "DUpdateSystem",
  repo = "UpgradeAll",
  asset = {
    include = "UpgradeAll_.*[.]apk$",
  },
}
```

The generated `Manifest` records the GitHub release source-response SHA-512 digest(s) that populated the provider cache, and `.autogen.jsonc` records package-local ownership with generator `github-releases`. Applying the preview uses the same generated-repository/ownership rules as installed and F-Droid autogen, including higher-priority repository suppression and conflict checks before overwriting generated output.

This is a snapshot/cache-backed generation seam; the generated package source remains ordinary small Lua and Manifest content. Product runtime update-checks for generated GitHub packages use `getter.provider.github.release_candidates`; getter may refresh the GitHub releases provider cache through Rust-owned live transport when Manifest/free-network policy allows it. Flutter/native product requests stay package/update oriented and do not carry GitHub fixture bodies, API base URLs, endpoint controls, raw provider payloads, cache modes, transport controls, or Lua source. GitHub global repository search, latest-commit live package generation, downloads, and installer semantics remain later slices.

### Release checks vs latest commit checks

GitHub release checks are ordinary versioned update checks. They produce stable candidates/artifacts when release metadata and asset descriptors are known.

GitHub latest-commit checks are **live/floating** behavior under ADR-0010. A latest commit id is not silently treated as an ordinary versioned release unless a package explicitly models it as stable release metadata. Live checks must be opt-in, surfaced to UI/CLI before task submission, and use the live-version semantics from ADR-0010. Free-network/high-risk status is declared per Lua script in package metadata using permissions such as `allow_free_network`; `9999.lua` commonly declares it, but the filename alone is not the permission source.

### Asset selection

The standard GitHub class can provide useful defaults, especially for Android APK projects, but it cannot guarantee every GitHub project works with only `owner` and `repo`.

Package-authored filters/overrides remain valid and expected:

- include/exclude Rust-regex filters;
- artifact naming rules;
- prerelease handling;
- ABI/channel/flavor selection;
- fallback from releases to tags;
- checksum/signature sidecar matching when supported.

Those rules belong in Lua modules/package definitions and getter validation, not Flutter.

## Normalized provider candidate model

Provider-specific facts should normalize into a shared candidate/artifact model before selection/action issuance. The exact Rust structs may evolve, but the domain shape should include at least:

```text
ProviderCandidate
  package_path
  repo_name?
  source_id
  provider_kind              # fdroid, github, static_mock, ...
  upstream_id                # package name, owner/repo, etc.
  version_name
  version_code?              # important for Android/F-Droid
  revision?                  # commit id or live revision when applicable
  channel?
  published_at?
  changelog?
  artifacts[]
  diagnostics[]
  provider_metadata_digest?
```

```text
ArtifactDescriptor
  id/name
  file_name?
  locator/url
  content_type?
  size?
  sha256?/signature?
  headers?/auth_reference?
  metadata_digest?
```

For Android/F-Droid candidates, version code is a first-class comparison/input fact when available. For GitHub release candidates, version name/tag is normally the primary version input unless the package/module extracts structured Android version facts from assets or metadata.

Artifact descriptors inside package metadata remain package-management contracts as defined by ADR-0010. For package version scripts without `allow_free_network`, external network/dynamic-download data files or API response bodies must match that package's `Manifest` hashes where applicable. A missing `Manifest` is equivalent to an empty hash set, not an invalid package; scripts that do not fetch external network content, or only read package-local `files/`, do not need a `Manifest`, while network fetches from scripts without `allow_free_network` cannot succeed when `Manifest` is missing or empty. Scripts with `allow_free_network` are not blocked by `Manifest` membership but remain high-risk. Repository-level autogen scripts under `.metadata/autogen/` do not have a package Manifest, but autogen scripts that create package directories must generate correct package Manifests for generated packages expected to work without `allow_free_network`. Package `Manifest` is not a repository source manifest and cannot protect same-level source files such as `.autogen.jsonc`; those are protected by repository trust/signing. Refreshing metadata may discover a new valid release/artifact descriptor, but a downloaded artifact mismatch is a download/validation failure, not a cache refresh shortcut.

## Cache and freshness model

ADR-0012 preserves ADR-0010 cache consistency and makes the cache layers explicit.

### Provider/source cache

Provider/source cache entries live in `cache.db` and store upstream facts or parsed provider facts plus provenance for the source response bodies used to produce those facts.

Getter-shipped standard provider modules call provider-specific host functions such as `getter.provider.fdroid.update_candidates(...)` and `getter.provider.github.release_candidates(...)`; Rust provider operations decide which upstream request(s), parsed facts, source response digest(s), freshness tokens, and provider/source cache entries are involved.

Generic/custom Lua can still opt into HTTP source caching per request through getter's host HTTP API, for example:

```lua
local index = http_get(fdroid_index_url, {
  headers = { Accept = "application/json" },
  cache = true,
})
```

`cache = false` is the default so ordinary one-off HTTP calls do not silently become durable provider cache. The v1 host request shape is intentionally narrow: a URL string plus an optional options table with string-to-string `headers` and boolean `cache`; unsupported options are rejected rather than silently accepted. When `cache = true`, getter owns cache key construction, storage, revalidation, stale diagnostics, and secret redaction; Lua chooses that the generic HTTP request should participate in HTTP/source caching but does not write cache entries itself.

F-Droid provider cache keys must include inputs such as:

- provider kind (`fdroid`);
- endpoint id and endpoint URL/config digest;
- auth/permission mode when applicable;
- index format/schema/parser version;
- freshness tokens such as ETag, Last-Modified, index revision, source timestamp, or response digest;
- getter/provider implementation version when it changes interpretation.

GitHub provider cache keys must include inputs such as:

- provider kind (`github`);
- endpoint/API base URL;
- owner/repo;
- request type (`releases`, `tags`, `latest_commit`, asset lookup, etc.);
- auth token identity/reference and rate-limit-relevant mode, without storing secrets in cache keys/logs;
- ETag/Last-Modified/API cursor/response digest;
- getter/provider implementation version when it changes interpretation.

Current GitHub releases live refresh uses a Rust-owned transport seam in getter operations. The production default is a small blocking GitHub REST client with the GitHub JSON `Accept`, API-version, and UpgradeAll getter `User-Agent` headers, a normal network timeout, rustls TLS, and optional platform root verification through the getter `rustls-platform-verifier` feature for Android/native builds. Tests inject mock transports, committed API snapshots, or a local loopback HTTP server; they must not depend on live GitHub API calls. A successful refresh stores parsed release facts, source-response SHA-512 provenance, and freshness metadata such as `ETag`/`Last-Modified`. A no-cache transport/HTTP/rate-limit failure is a getter-owned provider error; a forced refresh failure with usable existing cache returns stale cache explicitly with `cache.refresh_failed` and `used_stale_cache` diagnostics. This live transport slice covers GitHub releases only; latest-commit live package generation, global GitHub search/catalog, downloads, and installer behavior remain separate non-goals.

### Generated package records

Generated F-Droid package directories/version scripts, package-local `files/` helper data, package Manifests, and package-local `.autogen.jsonc` generation records are repository source artifacts, not metadata cache entries. Repository trust/signing protects these source artifacts. Package `Manifest` only constrains external network/dynamic-download response bodies and cannot protect sibling source files such as `.autogen.jsonc`. Hashes inside `.autogen.jsonc` are generated-output ownership/tamper-detection facts only, not security trust, repository signing, or Manifest/download validation. The `.autogen.jsonc` `files` map covers getter-written generated output such as `metadata.jsonc`, `Manifest`, generated Lua scripts, and generated `files/...` helper files; it does not include `.autogen.jsonc` itself, avoiding self-referential hashing.

Each generated package directory stores its own `.autogen.jsonc`. The generation record should record enough information for safe preview/regeneration/cleanup, such as:

- generator id and generator/template version;
- provider kind and endpoint id;
- upstream package name;
- generated package path and output directory;
- input catalog revision/digest when known;
- generated file list and file hashes, excluding `.autogen.jsonc` itself and used only to decide whether files still match getter-generated output for refresh/cleanup/overwrite ownership checks;
- ownership state for refresh/cleanup conflict detection.

If `.autogen.jsonc` is malformed or schema-invalid, it is not accepted as ownership proof; autogen refresh/apply/cleanup/overwrite reports a generated-ownership conflict rather than repairing, replacing, or deleting the directory automatically. Ordinary package discovery/evaluation remains governed by `metadata.jsonc`.

If a reusable F-Droid module changes, ADR-0010's Lua dependency-closure cache invalidation reruns package metadata normalization. Regenerating the small package file is separate and should be previewed when generator/template identity in `.autogen.jsonc` changes materially.

### Package metadata cache

Package metadata cache entries live in `cache.db` and are keyed by:

- package metadata and version-script hashes;
- loaded Lua provider module/template/helper hashes;
- parent package imports and dependency closure digest;
- Lua API/schema/runtime version;
- platform target and permission/network mode;
- provider/source cache keys or content digests used by the package evaluation;
- relevant user-independent source configuration.

User state such as tracked/enabled/favorite/pin_version is not package metadata cache authority. Final user-state-dependent update status is computed by getter operations using main DB state plus cached/evaluated metadata.

### Runtime task state

Runtime actions/tasks stay process-memory only per ADR-0011. Provider cache entries, generated package source artifacts, and package metadata cache entries must not be used as a hidden task database or downloader resume mechanism.

## Refresh semantics

Normal refresh may use fresh provider/source cache. When freshness tokens or TTL indicate revalidation is needed, getter may revalidate provider facts before rerunning or reusing package metadata normalization.

A forced refresh has stronger semantics:

- it bypasses cached reads for the requested provider/package scope;
- on success, it updates or replaces relevant provider/source cache entries in `cache.db`;
- if source facts changed, affected package metadata entries must be invalidated or recomputed so later reads do not present old facts as current;
- if source facts are unchanged but freshness metadata is updated, getter may update checked-at/freshness metadata without replacing the provider body;
- it must not leave old provider/package metadata as the effective fresh value after newer facts were successfully observed.

If forced refresh fails:

- getter must not delete still-usable old cache merely because refresh failed;
- if old cache is used, the result must report staleness/fallback explicitly;
- diagnostics should include stable codes such as `cache.refresh_failed` and `used_stale_cache`, plus stale age/cursor/freshness details when available;
- old cache must not be presented as a successful fresh synchronization.

These semantics apply to both F-Droid catalog/index refresh and GitHub per-project/API refresh.

A bulk F-Droid catalog refresh may update facts for many packages, but that does not create batch download/install tasks and does not imply a parent/child task API. Batch update task semantics remain deferred.

## Operation contract implications

### Update check

A live-provider update check proceeds conceptually as:

1. Resolve the active package definition through normal repository priority.
2. Load and validate its complete Lua lifecycle contract.
3. Invoke the appropriate update-check lifecycle entrypoint.
4. Lua/provider modules call getter provider host APIs for F-Droid/GitHub facts.
5. Getter validates provider output and normalizes candidates/artifacts.
6. Getter obtains the effective local baseline using installed-version entrypoint plus `pin_version` rules from ADR-0010.
7. Getter selects the update candidate/artifact.
8. Getter issues an opaque `action_id` bound to the loaded package/Lua context and sealed action plan per ADR-0011.
9. Flutter may render the getter DTO and submit only the `action_id`.

Flutter must not assemble or echo full action payloads, raw URLs, checksums, package paths/atoms, selected versions, source configs, or provider request parameters as task-submission input.

### Autogen preview/apply

Autogen operations produce preview DTOs before writing files. Applying a preview writes ordinary package directories/version scripts, Manifests, optional package-local `files/` helper data, and package-local `.autogen.jsonc` generation records through getter. Autogen apply may replace existing generated output only when matching `.autogen.jsonc` proves ownership; target directories without a matching generation record are conflicts. When refresh/overwrite ownership checks pass, getter clears the existing generated package directory contents, then writes the new generated contents into the same package directory, without preserving old unlisted extra files. If clearing any old file or subdirectory fails, the whole refresh/overwrite fails rather than being ignored. If writing new generated contents fails after clearing, the operation fails directly without rollback; the directory may be empty or partially written, and the next refresh continues by clearing and rewriting again. A generated-repo package directory missing `.autogen.jsonc` is a conflict rather than something getter automatically claims. A malformed/schema-invalid `.autogen.jsonc` is also a generated-ownership conflict for refresh/apply/cleanup/overwrite, while package discovery/evaluation remains governed by `metadata.jsonc`. Autogen apply must preserve existing user state as already accepted for installed autogen, and must respect repository priority/ownership rules.

Autogen cleanup is allowed only after `.autogen.jsonc` ownership checks pass. If generated content no longer matches the package-local generation record, cleanup reports a conflict instead of deleting or copying it into `local`. When ownership checks pass, cleanup clears the generated package directory contents directly, including `.autogen.jsonc` and any unlisted extra files inside it, but does not delete the package directory itself. If clearing any file or subdirectory fails, the whole cleanup/update fails rather than being ignored. Getter does not classify or preserve unlisted extra files in generated package directories because they are outside getter's domain; direct directory-content clearing is simpler and more stable for generated output. Users who want to keep or override edited generated content should move it to `repo/local/...` explicitly.

## Flutter/native bridge consequences

Flutter may:

- request provider catalog search/lookup/autogen preview operations;
- render getter-owned catalog/autogen/update-check DTOs;
- ask users for confirmation;
- pass accepted preview ids/package atoms/action ids back to getter;
- subscribe to runtime notifications and query current task state.

Flutter and Kotlin must not:

- parse F-Droid indexes or GitHub API responses;
- decide provider/source selection;
- generate Lua package text;
- map F-Droid/GitHub upstream ids into UpgradeAll package paths/atoms;
- implement version comparison/update selection;
- manage provider/package metadata cache invalidation;
- construct task/action payloads;
- implement downloader/installer task state machines.

Android/Kotlin remains allowed to expose raw platform facts and platform capabilities through documented platform-adapter seams, such as PackageManager installed inventory or future installer handoffs.

## CLI consequences

Getter CLI should expose provider/autogen/update behaviors as getter operations, not as Flutter-only features. Exact command names are implementation details, but the CLI should be able to exercise:

- F-Droid catalog refresh/query against fixtures or controlled test endpoints;
- F-Droid autogen preview/apply from explicit package names and installed-inventory fixtures;
- GitHub provider module update checks against fixtures or mocked provider host APIs;
- forced refresh success/failure and stale cache diagnostics;
- live latest-commit checks separately from normal release checks.

CLI tests must not require cross-invocation runtime task persistence. Runtime action/task coverage remains single-process per ADR-0011.

## Error and diagnostic model

Provider diagnostics should be getter-owned and stable enough for UI/tests. Examples:

```text
provider.fdroid.index_unavailable
provider.fdroid.package_not_found
provider.fdroid.parse_error
provider.github.rate_limited
provider.github.repository_not_found
provider.github.asset_not_found
provider.github.auth_required
cache.refresh_failed
used_stale_cache
autogen.already_covered
autogen.preview_stale
autogen.ownership_mismatch
package.source_invalid
```

Auth/rate-limit diagnostics must not print secrets. Cache keys and logs may reference credential identities or configured auth labels, but not raw tokens.

## Testing strategy

Use TDD for getter/provider behavior:

- F-Droid provider endpoint cache key/freshness behavior;
- F-Droid catalog lookup from controlled fixtures;
- F-Droid autogen preview/apply output paths, package paths, package-local `.autogen.jsonc` generation records, and repository-priority skip/shadow behavior;
- generated F-Droid package update checks through `luaclass.fdroid_android` using only `package_name` in the common case, evaluated by provider-backed runtime operations rather than plain package evaluation;
- GitHub release response normalization from controlled fixtures;
- GitHub asset filter/default behavior;
- GitHub latest-commit live/floating semantics;
- forced refresh success replacing cache and failure preserving stale cache with diagnostics;
- version comparison using Android version code when available and version name fallback otherwise;
- no task persistence across CLI/runtime process boundaries.

Use BDD for user-visible Flutter/product flows:

- user explicitly chooses an F-Droid app, sees preview, confirms, and the app appears as a generated package;
- F-Droid catalog/search UI passes only raw user input or getter-issued catalog/preview ids to getter and never interprets provider results in Dart;
- installed-autogen discovers F-Droid-covered installed apps and writes the generated repository only after confirmation;
- a hand-authored GitHub package checks releases and surfaces asset/filter diagnostics;
- stale provider cache warnings are visible without crashing or pretending success;
- update-check action submission still uses getter-issued opaque `action_id` only.

BDD scenarios should stay focused and not duplicate getter unit coverage.

## Non-goals

ADR-0012 does not accept or implement:

- Flutter-owned F-Droid/GitHub provider logic, HTTP calls, package-path/atom mapping, Lua generation, version comparison, cache invalidation, or action construction.
- A revival of the old Hub/app UUID model.
- Raw arbitrary Lua HTTP as the standard provider path; free network remains a per-script package metadata permission such as `allow_free_network`.
- Removal of user-controlled transparent URL rewrite/mirror/proxy policy; global getter-local hook scripts under `rc/hook/` are runtime/local policy discovered only from the filesystem, dot-prefixed Lua files are excluded from hook Lua discovery, there is no hook registry/metadata/disabled state, enabled hooks can wrap public host functions such as `http_get()` or `read_package_file()`, call original unhooked entrypoints through `getter_builtin.<name>`, and affect package version scripts, repository-level autogen scripts, and `luaclass/` calls, but content trust still depends on package Manifest hashes or explicit free-network permission.
- GitHub global catalog/search/autodiscovery as a first-class F-Droid-like autogen source.
- A guarantee that every GitHub project works with only `owner` and `repo`; package-authored asset/version filters remain valid.
- Real downloader implementation.
- Android installer implementation, PackageInstaller semantics, intent/URI/SAF/FileProvider policy, Shizuku/root behavior, or install-result platform details.
- Android foreground/background service policy.
- Android system notifications.
- Runtime task persistence, app-restart recovery, downloader-style resume/recovery, or daemon behavior.
- Batch parent/child update/download/install task APIs.
- A product UI commitment for full F-Droid catalog browsing/search in the first implementation slice. Getter-owned catalog query can exist as an autogen input; product UI breadth can be staged later.

## Consequences

Positive:

- F-Droid's structured catalog is used where it is strongest: generating ordinary package definitions from package names/catalog facts.
- GitHub remains simple for the common hand-authored project case while still allowing package-specific asset rules.
- Generated packages obey the same repository/overlay model as all other packages.
- Provider execution, cache consistency, version selection, and action issuance remain in getter.
- Flutter can expose useful provider/autogen UX without owning product logic.

Costs:

- Getter needs a real provider/source cache layer before live provider behavior is product-complete.
- Package-local `.autogen.jsonc` generation records must be robust enough for regeneration and cleanup.
- F-Droid catalog refresh can affect many packages, requiring careful invalidation/lazy recomputation.
- GitHub rate limits/auth and asset selection need explicit diagnostics.
- Documentation must consistently distinguish UpgradeAll repositories from upstream provider endpoints.

## Follow-up implementation notes

Suggested first implementation slices after this ADR is accepted:

1. Add typed provider/source cache storage primitives in getter `cache.db` with forced-refresh/stale diagnostics tests.
2. Add fixture-backed F-Droid catalog provider and `luaclass.fdroid_android` package evaluation tests.
3. Add F-Droid autogen preview/apply from explicit package names, writing small package directories/version scripts, package Manifests, optional package-local `files/` helper data, and package-local `.autogen.jsonc` generation records without embedded duplicate `id` fields.
4. Update package schema/evaluation so package path is directory-derived rather than a required Lua table field.
5. Integrate F-Droid catalog matching into installed-autogen preview/apply.
6. Add fixture-backed GitHub release provider and standard `luaclass.github_android_apk` tests for release/asset normalization.
7. Add GitHub latest-commit as live/floating check only.
8. Expose native/Flutter DTOs for F-Droid autogen preview and provider diagnostics without moving provider decisions into Dart/Kotlin.
