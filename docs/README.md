# UpgradeAll Rewrite Documentation

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

This documentation set records the design decisions for the UpgradeAll rewrite. It exists so coding agents and human maintainers can trace every major implementation choice back to a written decision.

Start here:

1. `architecture/upgradeall-getter-rewrite-wiki.md` — main living architecture wiki.
2. `architecture/adr/0001-app-centric-lua-package-repository-model.md` — package/repository/Lua model.
3. `architecture/adr/0002-getter-flutter-platform-boundary.md` — getter vs Flutter/platform adapter boundary.
4. `architecture/adr/0003-legacy-room-migration.md` — old Room DB migration strategy.
5. `architecture/adr/0004-sqlite-main-db-and-cache-db.md` — storage and cache split.
6. `architecture/adr/0005-lua-package-api.md` — Lua package API and Rust validation boundary.
7. `lua-api/` — practical Lua package authoring docs.
8. `migration/legacy-room-mapping.md` — old data mapping rules.
9. `app/flutter-ui-feature-parity-and-testing.md` — Flutter feature parity and BDD/TDD test boundary.
10. `implementation/coding-agent-handoff.md` — coding-agent / pi-agent handoff instructions.

Canonical architecture ADRs live in `docs/architecture/adr/*`. The `docs/adr/*` directory is kept for historical/refactor-phase ADRs and transition notes.

Documentation policy:

- Every major decision must be captured in the wiki or an ADR.
- Every cross-boundary API must have schema documentation before implementation stabilizes.
- Every migration must have source/target mapping and failure behavior documented.
- Every coding agent must read `../AGENTS.md` and this docs index before implementation.
