# 0003: Source-level page customization

- Date: 2026-06-20
- Status: Accepted for the refactor plan

## Context

UpgradeAll users may want customized pages and flows. Runtime UI plugins would increase app complexity, safety risk, test surface, and compatibility burden. The rewrite plan instead emphasizes source-level downstream customization.

## Decision

UpgradeAll will support page customization through source-level modules and typed contracts, not through a v1 runtime UI plugin system. Upstream should provide stable page contracts, default pages, examples, and compile/test failures when custom pages drift from contracts.

## Consequences

- Downstream builders can fork, modify pages, run tests, and rebuild.
- Runtime app complexity stays lower than a plugin UI framework.
- Stable route IDs, semantic/test IDs, and page contracts become product requirements.
- Upstream should avoid needless churn in customization surfaces.

## Alternatives considered

- Runtime UI plugins. More flexible for installed apps, but much harder to secure, test, and keep compatible during the rewrite.
- No customization boundary. Simpler initially, but conflicts with the selected distribution philosophy.
