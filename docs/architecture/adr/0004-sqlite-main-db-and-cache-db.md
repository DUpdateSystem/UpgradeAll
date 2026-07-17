# ADR-0004: SQLite main DB and cache DB

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

getter uses SQLite for backend storage.

The storage is split into:

1. Main DB: authoritative user and getter state.
2. Cache DB: derived/evaluated/provider/cache state.

Users manually corrupting backend storage is considered non-standard usage. getter may fail fast with a clear error.

## Main DB stores

- Repository registry and priority.
- Enabled/tracked apps.
- User source priority overrides.
- Ignored versions, pins, favorites.
- Migration records.
- Settings and credential references.
- Operation-specific durable records accepted by later ADRs. ADR-0011 explicitly excludes runtime task state from main/cache DB persistence.

## Cache DB stores

- Evaluated package metadata.
- Lua validation results.
- Release candidates and selected latest versions.
- Artifact metadata.
- Provider response cache.
- Search index.

## Cache invalidation keys

Cache keys should include repo id, repo revision/hash, package file hash, Lua API version, getter/package schema version, platform target and permissions/network mode.

## Repo source files

Package Lua files live in filesystem repositories, not inside the main DB. SQLite records repository path/revision/priority and evaluated/cache results.

## Rationale

SQLite is chosen over transparent text files for backend state because mobile app data needs atomic updates, reliable migrations, consistent concurrent operations and robust cache/query behavior. Text-like transparency is preserved at the package repository layer through Lua files.
