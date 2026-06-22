# ADR-0003: Legacy Room migration

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

Old UpgradeAll user data must migrate automatically and without normal-user manual export/import.

Migration is intentionally limited and simple. It preserves core user-visible app tracking state, but does not attempt to migrate every complex legacy behavior.

Complex legacy data such as API keys, auth tokens and unusual Hub configuration may be dropped.

## Source data

Legacy Room database:

```text
app_metadata_database.db
version = 17
entities = app, hub, extra_app, extra_hub
```

## Target data

Migration writes to:

- getter main SQLite user state.
- `local` repository package Lua files when necessary.
- migration records table.

Normal installed-app autogen writes to `local_autogen`, but legacy migration is special: it may generate `local` package files once to preserve explicit old user data.

## Package ID mapping

- Android apps: `android/<packageName>`.
- Magisk modules: `magisk/<moduleId>`.

## Mapping strategy

1. Detect legacy Room DB.
2. Use bundled official repository snapshot for matching; do not require network at first launch.
3. For common cases, convert legacy app/cloud config to the new package/user state model.
4. If a package is covered by official repository, point user state at that package.
5. If not covered but common conversion exists, generate a `local` package Lua file.
6. Rare/complex cases migrate installed id/tracked state and surface a missing-package diagnostic.
7. Record migration completion.

## What can be dropped

- API keys.
- Provider auth tokens.
- Complex or ambiguous Hub auth.
- Legacy settings whose meaning no longer exists.
- Exotic URL replacement rules that cannot be safely mapped.

## Implemented CLI bridge-bundle slice

The current Rust CLI implementation does not read Android Room files directly yet. It accepts a JSON bridge bundle for deterministic host-side tests:

```json
{
  "format": "upgradeall-legacy-room-bundle",
  "version": 17,
  "apps": [
    {
      "kind": "android",
      "installed_id": "org.fdroid.fdroid",
      "official_package_available": true,
      "common_conversion_available": false,
      "ignored_version": "1.20.0",
      "favorite": true
    }
  ]
}
```

This slice maps `apps[]` into getter tracked package state in `main.db`, writes a sanitized report under `migration-reports/`, and records `legacy-room-v17` completion. Unsupported bundle formats/versions still fail with a sanitized recovery report.

## Failure behavior

A single unmapped app must not block the whole app. Global migration failure should lead to a migration/recovery page. A per-app mapping failure should be visible on that app or diagnostics page.
