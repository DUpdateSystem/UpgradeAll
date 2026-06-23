# Legacy Room Migration Mapping

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Source

Legacy Room DB:

```text
app_metadata_database.db
version 17
```

Tables:

- `app`
- `hub`
- `extra_app`
- `extra_hub`

## Target

- getter main SQLite DB user state.
- `local` repository package Lua files when migration needs compatibility stubs.
- migration record table.

## Principles

- Migration must be automatic for normal users.
- Migration is limited and simple.
- Complex API keys/auth may be discarded.
- Per-app mapping failures should not block the entire app.

## App mapping

Legacy app -> new package id:

```text
Android package -> android/<packageName>
Magisk module   -> magisk/<moduleId>
```

If bundled official repo contains a matching package, link user state to it.

If no official match but common conversion is possible, generate local package Lua.

If no conversion is possible, preserve installed/tracked id and mark missing package definition.

## Hub mapping

Legacy Hub does not map to a top-level new object.

Its semantics are split into:

- provider/source config;
- package source priority;
- credentials/auth settings;
- URL rewrite policy;
- migration diagnostics.

Complex auth may be dropped.

## ExtraApp mapping

Map mark/ignore version state when possible. In the direct Room DB importer, `extra_app.mark_version_number` wins over `app.ignore_version_number` when both exist for the same package id because it is the more specific extra-app state.

## ExtraHub mapping

Map URL replace semantics into global download rewrite policy if safe. Otherwise drop and record warning.

## Current CLI direct DB import

The host-side CLI can import a copied/checkpointed Room SQLite database directly:

```text
getter --data-dir <path> legacy import-room-db <db.sqlite>
```

Current direct import scope:

- requires `PRAGMA user_version = 17`;
- reads `app.app_id`, `app.ignore_version_number`, `app.star`;
- reads `extra_app.app_id` and `extra_app.mark_version_number`;
- maps app-id key `android_app_package` to `android/<packageName>`;
- maps app-id key `android_magisk_module` to `magisk/<moduleId>`;
- writes getter `tracked_packages` plus the `legacy-room-v17` migration record in one transaction;
- imports valid app rows while reporting skipped-row warnings when other app rows are malformed or unsupported;
- treats a DB with app rows but zero importable app rows as `migration.invalid_db` and does not record migration completion;
- emits sanitized counts/warnings and never embeds raw DB contents, auth, or tokens in reports.

Currently dropped with warnings:

- `hub` rows;
- `extra_hub` rows and URL replacement policy;
- hub auth/API keys/provider credentials;
- app regex/cloud config fields whose new package equivalent is not accepted yet.

The direct CLI reader expects Android/platform code to provide a WAL/SHM-consistent DB copy; it does not perform Android Room checkpointing itself. The first Flutter APK migration-adapter slice prepares that input with a no-UI Android MethodChannel adapter that copies the SQLite triplet (`.db`, `-wal`, `-shm`), checkpoints/canonicalizes the copy in app-private storage, and returns the copied DB path for Flutter to pass to getter. The default product migration action remains disabled until the production getter import bridge is connected.

## Current CLI bridge bundle

The host-side CLI implementation also accepts a deterministic JSON bridge bundle:

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

Each app maps to `tracked_packages` in getter main DB. Success reports are sanitized and include counts only; raw bundles are not copied into reports.

## Completion

After successful migration, write a migration record so the same migration does not rerun.
