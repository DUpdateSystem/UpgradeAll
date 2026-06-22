# 0004: First-class legacy Room migration

- Date: 2026-06-20
- Status: Accepted for the refactor plan

## Context

Existing Android users have data in the legacy UpgradeAll Room database. The official Android upgrade path must preserve package identity and user data. Migration failure must be visible and recoverable rather than silently destructive.

## Decision

Legacy Android Room migration is a first-class compatibility subsystem. The official Flutter Android upgrade must keep the existing application identity and use a tested import flow from supported legacy Room schemas into getter-owned storage.

Migration must be transactional from the user's perspective: a failure must not leave a partially usable new app state. The app must provide recovery actions such as retry, report export, and explicit start-fresh confirmation.

## Consequences

- The project needs migration fixtures and end-to-end migration tests before release.
- Legacy schema support boundaries must be explicit.
- The legacy migrator can be removed only after a separately documented support decision.
- Android signing/package identity is part of the migration contract.

## Alternatives considered

- Best-effort startup migration. Easier to implement but risky for user data.
- Manual export/import only. Avoids direct migration complexity but breaks the official upgrade expectation.
