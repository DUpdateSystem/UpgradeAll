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
7. `architecture/adr/0006-package-centric-cli-command-contract.md` — getter CLI automation contract.
8. `architecture/adr/0007-flutter-getter-bridge-contract.md` — Flutter/getter DTO and bridge contract.
9. `architecture/adr/0008-flutter-product-apk-entry.md` — Flutter app as the sole product APK entry.
10. `architecture/adr/0009-android-platform-adapter-and-package-visibility.md` — Rust-active Android platform adapter and package visibility policy.
11. `architecture/adr/0010-package-metadata-cache-and-version-baseline.md` — accepted package metadata cache, live-version, installed-version, and `pin_version` rules.
12. `architecture/adr/0011-lua-update-runtime-side-effects-and-events.md` — accepted Phase D Lua runtime, task/action lifecycle, mock side-effect executor, and RuntimeNotification bridge rules.
13. `lua-api/` — practical Lua package authoring docs, including offline `repo validate` diagnostics.
14. `migration/legacy-room-mapping.md` — old data mapping rules.
15. `app/flutter-ui-feature-parity-and-testing.md` — Flutter feature parity and BDD/TDD test boundary.
16. `implementation/coding-agent-handoff.md` — coding-agent / pi-agent handoff instructions.

Canonical architecture ADRs live in `docs/architecture/adr/*`. The `docs/adr/*` directory is kept for historical/refactor-phase ADRs and transition notes.

Documentation policy:

- Every major decision must be captured in the wiki or an ADR.
- Every cross-boundary API must have schema documentation before implementation stabilizes.
- Every migration must have source/target mapping and failure behavior documented.
- Every coding agent must read `../AGENTS.md` and this docs index before implementation.
