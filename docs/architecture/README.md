# Architecture Documentation

This directory records the architecture decisions and design notes for the UpgradeAll rewrite.

Start here:

- `upgradeall-getter-rewrite-wiki.md` — main living wiki for the Flutter + Rust getter + Lua package repository redesign.

Planned / active ADRs:

- `adr/0001-app-centric-lua-package-repository-model.md`
- `adr/0002-getter-flutter-platform-boundary.md`
- `adr/0003-legacy-room-migration.md`
- `adr/0004-sqlite-main-db-and-cache-db.md`
- `adr/0005-lua-package-api.md`
- `adr/0006-package-centric-cli-command-contract.md`
- `adr/0007-flutter-getter-bridge-contract.md`

Documentation policy:

- Every important architecture decision should be recorded in this wiki or an ADR.
- Every new module should have a documented responsibility boundary.
- Every cross-boundary API should have a schema document.
- Every migration step should have source/target mapping documentation.
- `docs/architecture/adr/*` is the canonical architecture ADR set.
- `docs/adr/*` is historical/refactor-phase material kept for transition context unless a doc explicitly says otherwise.
