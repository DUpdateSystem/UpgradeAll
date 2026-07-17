# ADR-0010: Package metadata cache and version baseline

> Status: Accepted
> Date: 2026-06-24
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

UpgradeAll's rewrite uses getter-owned package metadata caching and getter-owned version-baseline semantics. The cache is persisted in `cache.db`; user version override state is persisted in `main.db`; package Lua/templates own local-version acquisition and normalization through the complete lifecycle contract.

## Package metadata cache

The runtime caches software metadata produced by running package Lua/provider logic. This cache is analogous in spirit to Gentoo `eix` package metadata caching: it supports fast query/display/update planning over reusable package metadata such as identity, description, homepage/source information, available versions/candidates, changelog or release notes when supplied by sources, artifact descriptors, licenses/tags, and source/provider diagnostics.

The cache model has two layers:

1. **Provider/source cache**: getter host API responses keyed by provider id, request parameters, executor/cache policy, auth/permission mode, and other provider-context inputs. Lua/provider modules opt individual HTTP host API calls into this cache explicitly, e.g. `http_get(url, { headers = ..., cache = true })`, while the default is `cache = false`. Plain package evaluation does not install HTTP by default; provider/runtime operations deliberately install it when they own the execution policy.
2. **Package metadata cache**: normalized package metadata produced by Lua/package logic from provider/source data.

Package metadata cache entries are persisted in `cache.db` from the first runtime implementation.

### Cache key and Lua dependency closure

Package metadata cache entries must be keyed by the Lua dependency closure and runtime context that can affect metadata, including at least:

- package Lua file hash;
- loaded template/base class hashes;
- loaded helper module hashes;
- parent package imports and their dependency closure digests;
- Lua API/schema/runtime version;
- platform target and permission/network mode when they can affect produced metadata;
- provider/source cache keys or content digests used to produce the metadata.

The runtime should automatically track the Lua dependency closure from actual loaded modules/templates/package imports. Explicit dependency declarations may exist only as a supplement or escape hatch for dependencies that the loader cannot otherwise observe.

If provider/source validation proves data unchanged, the runtime may update checked-at/freshness metadata without replacing the provider body. Package metadata normalization may be skipped only when the Lua dependency closure digest and other package metadata key inputs are unchanged. If the Lua package/template/helper dependency closure changes, the runtime may reuse unchanged provider/source cache as input, but it must rerun Lua normalization and create/update the current PackageMetadata entry for the new closure digest. If provider/source data changes, the runtime updates provider/source cache and reruns package metadata normalization for affected packages.

### Freshness and refresh

Freshness should be determined by provider/source freshness tokens when available, with TTL as a fallback revalidation hint. Examples include ETag, Last-Modified, source cursor, upstream index revision, or response digest. TTL expiry means the entry should be revalidated; it does not by itself mean the old cache must be deleted.

A forced refresh bypasses cached reads for the refreshed scope and, on success, updates or replaces the relevant `cache.db` entries with newly observed source facts. `--refresh` is not a read-only cache bypass mode. If the runtime has successfully observed newer actual provider/package metadata, keeping stale cache entries as the effective cache value is a consistency bug.

If forced refresh fails, the runtime must not delete still-usable old cache entries merely because the refresh failed. Instead, the operation must report refresh failure and staleness explicitly. If an operation elects to fall back to old cache, the result must make that fallback visible through diagnostics such as `cache.refresh_failed`, `used_stale_cache`, and stale age/cursor metadata. Old cache must not be presented as a successful fresh synchronization.

`cache.db` is not an audit log. Product semantics only require the current effective cache entry for a package/context. Old provider or package metadata entries may be retained temporarily for debugging or transaction safety, but they can be garbage-collected without preserving a product-visible history. Future metadata history/diff features require a separate design.

## Artifact descriptors and live versions

Artifact descriptors inside PackageMetadata are package-management contracts, not mutable cache truth. For a normal versioned release, the artifact URL/locator, size, checksum, signature, and content identity describe the expected file. If upstream changes the file behind the same declared release, or if the downloaded file's metadata/hash/signature does not match, getter must treat it as an invalid artifact/download failure rather than silently accepting the new file or treating the mismatch as a cache refresh. Refreshing metadata may discover a new valid release/artifact descriptor, but it must not launder a mismatched downloaded file into correctness.

Explicitly live/floating packages, analogous to Gentoo `9999` live ebuilds, are different. Live/floating behavior is a package/Lua-level flag, not an artifact-level flag. Getter/UI must surface live versions before download/task submission because artifact-stage detection is too late for user awareness.

Live version checks are opt-in and require a separate live flag such as `--live`; live packages do not participate in the ordinary versioned update check by default. The live update rule is intentionally simple: run the live Lua path to obtain the current live version string, compare it with the local baseline, and report an available update when they differ. A live version is an arbitrary valid UTF-8 string; getter does not parse, order, or validate it as a semantic version. A live package may allow Lua to resolve arbitrary/latest upstream artifacts at execution time, but those results are not cacheable as stable artifact metadata because upstream may change at any time and downstream cannot continuously refresh.

## Installed version entrypoint

The installed/local version source is part of the completed Lua lifecycle contract. Getter uses an installed version entrypoint/template method to resolve the current baseline and to produce display data.

For non-live update checks, the effective local baseline is `pin_version` when the user has set one; otherwise getter uses this entrypoint.

The installed version entrypoint returns a structured value such as:

```lua
return {
  status = "present",
  version = "1.2.3",
  extra = {
    version_code = 123,
  },
}
```

or:

```lua
return {
  status = "not_installed",
}
```

Platform/API failures use Lua errors such as `error("reason")`, not `not_installed` values.

Without a `pin_version` override, getter must have a `present` local version to compare. If the entrypoint reports `not_installed`, there is no local baseline to display or compare. If it raises an error, getter reports the Lua/platform version-source error.

With a `pin_version` override, getter may still call the installed version entrypoint for display. If that call fails, getter reports a local-version diagnostic but continues comparison against `pin_version`. If it reports `not_installed`, UI omits the local version row and still shows/uses `pin_version`.

For Android apps with a standard version source, the default Lua template can call the getter/platform host API that reads platform-specific package facts such as version name/code, return `not_installed` when the app is absent, and raise a Lua error if the platform call itself fails. Special packages can override or inherit a different Lua implementation.

For live checks, the installed version entrypoint may return `{ status = "not_installed" }` to mean no local baseline is semantically available; getter then falls back to the last successfully installed/accepted live version recorded in getter state. If the live package's installed version entrypoint raises a Lua error because a platform/API call failed, getter must report that error and must not fall back unless a `pin_version` override supplies the effective baseline for that check.

## Version model and `pin_version`

The rewrite does not preserve the old Kotlin version-number stack wholesale. Lua packages/templates own local-version acquisition and normalization through lifecycle inheritance/override. Getter supplies host/platform APIs and small helper tools for common extraction/comparison tasks, such as regex-based extraction and platform facts like Android version name/code, but Lua/template code decides when and how to use them. Legacy invalid/include regex fields are migration inputs or Lua-template helper parameters, not global getter-owned version behavior.

New rewrite domain language uses `pin_version`, not `ignore_version`: `pin_version` is a persisted user-selected local version override stored in `main.db` tracked package state, not a transient update-check parameter and not cache data.

In the first implementation, `pin_version` is a scalar UTF-8 string so CLI usage stays simple, e.g.:

```bash
getter version pin <package-id> <version>
getter version unpin <package-id>
```

Pin/unpin commands mutate durable getter state. When set, getter compares upstream candidates against `pin_version` as the effective local version instead of the platform/Lua-installed version result. Other version comparison behavior remains the normal package/version comparison behavior.

UI/CLI display should still show both observed local version and `pin_version` when an observed local version exists:

- Flutter shows local version above and bold pin version below, with latest version on the right.
- CLI compact display uses `version: <local_version>(~<pin_version>~)` where the tilde-marked value is the pin override.

If local version acquisition errors while `pin_version` is set, the check may still proceed using `pin_version`, but the error must be visible as a diagnostic. If the package is explicitly not installed/no-local, UI omits the local version row instead of showing an error.

Legacy Room `ignore_version_number` and transitional `ignored_version` inputs map into rewrite `pin_version`; new rewrite storage, DTOs, and Flutter UI should emit/use `pin_version`. Legacy migration reports must emit an informational rename note such as `migration.renamed_ignored_version_to_pin_version` so reviewers/users can see that the setting was preserved under the new name.

If structured pin metadata or extra fields are needed later, CLI must expose ergonomic flags or a separate advanced command rather than requiring users to type raw JSON.

## Non-authoritative cache boundary

The package metadata cache must not be the authoritative store for:

- tracked/enabled packages;
- favorites;
- pin_version override state;
- final user-state-dependent selected update status;
- download task state;
- installer handoff results;
- Flutter UI state.

Those remain main DB or operation-specific state.

## Consequences

Positive:

- Cache invalidation follows the effective Lua dependency closure instead of fragile manual cache-clearing.
- Forced refresh semantics preserve cache consistency while keeping stale cache available after failed refreshes.
- Version baseline logic becomes explicit and user-visible.
- `pin_version` naming better matches the actual behavior than legacy `ignore_version` terminology.
- CLI remains ergonomic for pin/unpin workflows.

Costs:

- The runtime loader must track actual Lua/template/module/package imports.
- Cache keys become more complex than a package file hash.
- Tests must distinguish provider/source cache behavior from package metadata cache behavior.
- Existing rewrite code using `ignored_version` must be renamed before these DTOs/storage names become stable product API.

## Follow-up implementation notes

- Rename rewrite-facing `ignored_version` fields, storage columns, fixtures, and DTOs to `pin_version`.
- Keep accepting legacy names only at migration/import boundaries when needed.
- Add migration report notice `migration.renamed_ignored_version_to_pin_version`.
- Add getter CLI commands for durable pin/unpin.
- Add Lua/helper API for version extraction/comparison without preserving the old Kotlin version stack wholesale.
