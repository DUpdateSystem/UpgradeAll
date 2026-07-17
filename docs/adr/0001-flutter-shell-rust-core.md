# 0001: Flutter shell with getter-owned product logic

- Date: 2026-06-20
- Status: Accepted for the refactor plan

## Context

UpgradeAll is currently an Android/Kotlin multi-module application with a Rust `getter` submodule already integrated through native Android build tooling. The 2026-06-20 rewrite plan chooses Flutter for the new app shell and moves durable product logic into `getter`.

The key trade-off is whether the application remains Android/Kotlin-centered or becomes a thin cross-platform shell around a reusable headless engine.

## Decision

The rewritten UpgradeAll App will be a Flutter UI/platform shell. `getter` is the headless product engine and owns durable product behavior: source interpretation, update checks, release discovery, download orchestration, provider/downloader registration, storage, migrations, and event streams.

Flutter must not grow a second copy of getter product logic. UI code may adapt presentation, navigation, platform permissions, and source-level pages, but product decisions must flow through getter contracts.

## Consequences

- The app can become cross-platform without duplicating update logic per UI host.
- Getter contracts must be intentionally designed, versioned, documented, and tested.
- UI work cannot start by drawing screens around mock logic; it must be driven by getter-facing behavior scenarios and DTO contracts.
- Android compatibility work remains important because existing installed users must migrate safely.

## Alternatives considered

- Keep Android/Kotlin as the product center and call Rust only for selected helpers. This preserves current shape but keeps logic split across platform code and makes Flutter a risky rewrite.
- Make Flutter own product logic and use getter only as a library of utilities. This weakens the reusable engine goal and makes CLI/library support secondary.
