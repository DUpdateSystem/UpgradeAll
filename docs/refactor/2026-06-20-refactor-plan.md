# 2026-06-20 Refactor Plan

## Objective

Prepare the UpgradeAll Flutter + getter rewrite from a clean, synced master while preserving all temporary work in stashes/backup branches.

## Canonical source plan

The detailed 06-20 plan has been copied into this repository at:

- `docs/refactor/2026-06-20-upgradeall-flutter-getter-rewrite-complete-plan.md`

Provenance:

- Remote source: `xz@100.65.231.22:/home/xz/.hermes/plans/2026-06-20_181038-upgradeall-flutter-getter-rewrite-complete-plan.md`
- SHA-256: `a9d02ce7fb88112506580a6e5e723494016ff75cc950083f66ab93701bbc3a0a`
- The hash matches the plan that was preserved inside the pre-sync stash's untracked parent.

## Completed preparation

- Superproject WIP was stashed before sync.
- Getter submodule WIP was stashed before sync.
- Local pre-sync commits were preserved on backup branches.
- `master` was synced to upstream `origin/master` commit `4a1aae1d44a418989b0d3d28528cacff0cc066c0`.
- Getter submodule was synced to recorded commit `f011d9b4b9a15f83cd39c86e781ad8830a8ecae6`.
- Planning branch created: `refactor/phase0-planning-20260620`.

## User clarification captured

BDD Cucumber coverage is required for all user-facing functions/interfaces. The complete BDD coverage targets are the UpgradeAll App and Getter CLI. Internal interfaces should use unit tests, integration tests, and other traditional test frameworks because BDD fits integration/acceptance behavior better than algorithm-level tests.

## Phase 0 deliverables

- Glossary: `CONTEXT.md`.
- ADRs: `docs/adr/0001` through `0006`.
- Target architecture: `docs/architecture/target-architecture.md`.
- BDD/TDD plan: `docs/testing/bdd-plan.md`.
- Agent workflow: `docs/ai-development.md` and root `AGENTS.md`.
- Verification skeleton: `justfile`.

## Phase 1 recommendation

Detailed Phase 1a plan: [`phase-1-getter-cli-bdd-plan.md`](phase-1-getter-cli-bdd-plan.md). Phase 1a is the Getter CLI BDD spine inside the broader canonical Phase 1 getter workspace refactor.

Detailed Phase 1b plan: [`phase-1b-getter-workspace-skeleton-plan.md`](phase-1b-getter-workspace-skeleton-plan.md). Phase 1b is the transitional workspace skeleton that keeps behavior in the root getter package while introducing the split-crate scaffold. The single current verification entrypoint is `just verify`, which includes Phase 1a focused behavior tests plus Phase 1b structural workspace checks.

Do not start by implementing Flutter screens.

Start with a testable headless slice:

1. ADR 0007 is accepted for the Phase 1a Getter CLI command contract; future CLI changes must explicitly extend or revise that ADR.
2. Define the first Getter CLI Gherkin scenarios for initialization, app listing, hub listing, and malformed legacy bundle failure reporting.
3. Wire a minimal Cucumber runner for Getter CLI.
4. Implement the smallest CLI contract needed to make the first scenario pass.
5. Add internal Rust tests for the core behavior behind that CLI scenario.
6. Only then expose the same behavior through the app shell.

## Decision gates before implementation

- Choose the concrete Cucumber runner strategy for Flutter App scenarios.
- Choose the concrete command/output/error contract for the first Getter CLI slice.
- Decide whether to mine, split, or discard each part of the stashed direct-JNI/RPC rewrite.
- Confirm the first supported legacy DB schema range for migration fixtures.

## First proposed BDD scenarios

### Getter CLI smoke

```gherkin
@getter-cli @smoke
Feature: Getter CLI initialization
  Scenario: User initializes a new getter data directory
    Given an empty getter data directory
    When I run getter init for that directory
    Then the command succeeds
    And the getter data directory is usable
```

### Getter CLI migration recovery

```gherkin
@getter-cli @migration
Feature: Legacy import failure recovery
  Scenario: User receives a non-destructive report when legacy import fails
    Given a corrupted legacy export bundle
    When I run getter legacy import for that bundle
    Then the command fails with a documented migration error
    And no partially usable getter state is created
    And a sanitized migration report is available
```

### UpgradeAll App migration recovery

```gherkin
@app @migration
Feature: App migration recovery
  Scenario: User can retry or report a failed migration
    Given the app starts with a legacy database that cannot be imported
    When migration fails
    Then the app shows the migration recovery screen
    And the user can retry migration
    And the user can export a sanitized report
    And starting fresh requires explicit confirmation
```
