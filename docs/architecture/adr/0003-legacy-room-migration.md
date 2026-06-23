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

## Implemented direct DB and bridge-bundle slices

The Rust CLI now has a direct SQLite import slice for copied/checkpointed Room v17 databases:

```text
getter --data-dir <path> legacy import-room-db <db.sqlite>
```

The direct importer opens the DB read-only, requires `PRAGMA user_version = 17`, reads legacy `app` and `extra_app` rows, maps known app-id keys to `android/<packageName>` or `magisk/<moduleId>`, writes getter tracked package state plus the `legacy-room-v17` migration record in one transaction, and emits sanitized report counts/warnings. Current `hub` and `extra_hub` rows are not imported as top-level objects; they are counted/dropped with warnings until a later accepted mapping exists.

The first Flutter/Android migration UX slice adds a no-UI Android platform adapter that locates `app_metadata_database.db`, copies the SQLite triplet (`.db`, `-wal`, `-shm`) into an app-private getter-import path, checkpoints/canonicalizes the copy, and returns that copied DB path to Flutter. Flutter starts the flow and renders getter-owned reports. Getter still owns the actual import operation; the default product APK keeps the action disabled until the production getter import bridge is connected.

The host-side CLI also keeps the deterministic JSON bridge bundle for tests and non-Android fixtures:

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

Both slices map app state into getter tracked package state in `main.db`, write sanitized reports under `migration-reports/`, and record `legacy-room-v17` completion. Unsupported bundle formats/versions and unsupported/malformed databases fail with sanitized recovery reports.

## Failure behavior

A single unmapped app must not block the whole app. Global migration failure should lead to a migration/recovery page. A per-app mapping failure should be visible on that app or diagnostics page. The direct DB importer treats malformed optional rows and mixed valid/invalid app rows as warnings, but unreadable DBs, unsupported `user_version`, missing required `app` table, and databases with app rows but zero importable app rows are global failures.
