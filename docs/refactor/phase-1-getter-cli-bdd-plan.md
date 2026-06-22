# Phase 1a Plan: Getter CLI BDD Spine

Date: 2026-06-20

## Purpose

Phase 1a creates the first executable TDD spine for the rewrite without starting Flutter screen work. It is the entry spine for canonical Phase 1, not a replacement for the full getter workspace refactor. The goal is to make `getter` usable as a CLI and library-backed engine through behavior-first development.

This phase follows the clarified testing rule:

- User-facing interfaces require Cucumber/Gherkin BDD coverage.
- Getter CLI is a user-facing interface and needs complete BDD coverage for supported commands.
- Getter internals use traditional Rust unit/integration/property tests.

## Strict review of the plan

### Assumption: Start with the CLI before Flutter UI

Verdict: keep it.

Reason: the canonical 06-20 plan says `getter` owns product logic. A CLI-first slice exercises getter behavior without hiding engine mistakes behind UI scaffolding.

### Assumption: Use Cucumber/Gherkin for every Rust test

Verdict: reject it.

Reason: the user clarified that BDD is for user-facing integration/acceptance behavior. Internal Rust behavior should keep fast traditional tests.

### Assumption: Current `src/main.rs` means the CLI already exists

Verdict: reject it.

Reason: `src/main.rs` currently prints `Hello, world!`. The binary exists structurally, but the supported command contract does not exist yet.

### Assumption: The stashed direct-JNI rewrite can be resumed as implementation

Verdict: reject for Phase 1.

Reason: Phase 1 is CLI/library test spine work. Stash mining is allowed only after comparing each piece against ADRs and the canonical plan.

## Proposed test/tooling shape

### Getter CLI BDD

Initial runner direction: Rust Cucumber (`cucumber-rs`) for `.feature` files that invoke the `getter` binary.

Target-aligned layout for the future getter workspace:

```text
getter/
  crates/
    getter-cli/
      features/
        cli/
          init.feature
          app_list.feature
          hub_list.feature
          legacy_import_room_bundle_failure.feature
      tests/
        bdd_cli.rs
        support/
          cli_world.rs
          fixtures.rs
```

If implementation starts before the repository is moved to this target workspace, the temporary path under `core-getter/src/main/rust/getter/` must be treated as transitional. The test language and command contracts should still match the target layout.

Step definitions should:

- create an isolated temporary data directory per scenario;
- invoke the compiled `getter` binary as an external process;
- assert exit code, stdout/stderr, output schema, and filesystem/database side effects;
- avoid depending on network unless the scenario explicitly needs a mocked provider/server;
- preserve sanitized failure artifacts for debugging.

### Internal Rust tests

Use traditional Rust tests for:

- command parser units;
- output schema serialization;
- storage initialization;
- canonical IDs;
- legacy import mapping;
- migration report creation;
- provider parsing;
- version comparison;
- download orchestration edge cases.

## CLI command contract

The executable CLI contract must be accepted before feature files are implemented. The proposed contract is recorded in [`../adr/0007-getter-cli-command-contract.md`](../adr/0007-getter-cli-command-contract.md).

This Phase 1a plan uses that proposed grammar consistently:

```text
getter --data-dir <path> init
getter --data-dir <path> app list
getter --data-dir <path> hub list
getter --data-dir <path> legacy import-room-bundle <bundle.json>
```

Until ADR 0007 is accepted or revised, these commands are planning placeholders rather than executable supported contracts.

## First behavior slices

### Slice 1: CLI initializes an empty data directory

Feature:

```gherkin
@getter-cli @smoke
Feature: Getter CLI initialization
  Scenario: User initializes a new getter data directory
    Given an empty getter data directory
    When I run getter init for that directory
    Then the command succeeds
    And the output is valid JSON
    And the getter data directory is usable
```

Implementation work allowed by this slice:

- Replace `Hello, world!` with minimal CLI parsing.
- Create or open canonical getter-owned SQLite storage with minimal metadata and empty app/hub tables.
- Add JSON success/error output envelope.
- Add internal tests for SQLite storage init and output serialization.

Implementation work not allowed by this slice:

- Full provider registry.
- Flutter UI.
- Android migration.
- Downloader implementation.

### Slice 2: CLI lists empty app and hub catalogs

Feature:

```gherkin
@getter-cli @smoke
Feature: Getter CLI app listing
  Scenario: User lists apps before adding any app records
    Given an initialized getter data directory
    When I run getter app list for that directory
    Then the command succeeds
    And the output contains an empty app list

Feature: Getter CLI hub listing
  Scenario: User lists hubs before adding any hub records
    Given an initialized getter data directory
    When I run getter hub list for that directory
    Then the command succeeds
    And the output contains an empty hub list
```

Implementation work allowed:

- Minimal read path through getter core/library.
- Stable app-list and hub-list output DTOs.
- Internal tests for empty app and hub listing.

### Slice 3: CLI reports non-destructive legacy import failure

Feature:

```gherkin
@getter-cli @migration
Feature: Legacy import failure recovery
  Scenario: User receives a non-destructive report when legacy import fails
    Given a corrupted legacy export bundle
    And an initialized getter data directory
    When I run getter legacy import-room-bundle for that bundle
    Then the command fails with a documented migration error
    And no partially usable imported state is created
    And a sanitized migration report is available

  Scenario: User receives a not-implemented failure when a valid bundle is supplied
    Given a syntactically valid but unsupported legacy export bundle
    And an initialized getter data directory
    When I run getter legacy import-room-bundle for that bundle
    Then the command fails because import is not implemented yet
```

Implementation work allowed:

- Malformed-bundle detection.
- Unsupported/Not-Implemented classification for syntactically valid bundles.
- Import error classification for `migration.invalid_bundle` and `migration.unsupported_bundle`.
- Non-destructive transaction boundary for failed import.
- Minimal sanitized JSON migration report for malformed and unsupported bundles.
- Internal tests for report redaction and no-state-change semantics.

Implementation work not allowed:

- Full Room export implementation.
- Full Flutter migration page.
- Real legacy schema mapping beyond malformed/corrupted bundle rejection and unsupported valid bundle handling.

## Commit-sized sequence

1. Add Cucumber runner dependencies and a failing `init.feature` with step skeleton.
2. Add minimal CLI parser and JSON output envelope to make `init.feature` pass.
3. Add internal Rust tests for storage init and output serialization.
4. Add failing `app_list.feature` and `hub_list.feature` for empty catalog listing.
5. Implement minimal library/core read paths to make empty app/hub listing pass.
6. Add failing `legacy_import_room_bundle_failure.feature` for malformed bundle behavior.
7. Implement migration report/error skeleton and no-state-change semantics for malformed bundles only.
8. Extend `just verify` to run getter CLI BDD and internal Rust tests.

## Verification targets to add in Phase 1

Proposed future just targets:

```make
test-getter-unit:
    cargo test --manifest-path core-getter/src/main/rust/getter/Cargo.toml --lib --tests

test-getter-bdd:
    cargo test --manifest-path core-getter/src/main/rust/getter/Cargo.toml --test bdd_cli

verify: status cargo-metadata gradle-projects test-getter-unit test-getter-bdd bdd-plan-check
```

## Mapping to canonical Phase 1 acceptance

Canonical Phase 1 requires more than this CLI spine. Phase 1a contributes the first executable behavior spine, then the broader Phase 1 must still complete:

- target getter workspace split (`getter-core`, `getter-storage`, `getter-providers`, `getter-downloader`, `getter-plugin-api`, `getter-ffi`, `getter-rpc`, `getter-cli` or accepted equivalents);
- no Android/JNI dependency inside Getter Core;
- CLI can initialize canonical storage;
- CLI can list empty apps and hubs;
- provider fixture tests for core behavior;
- `cargo test --workspace` or transitional equivalent passes.

## Phase 1a decisions now captured

1. ADR 0007 is accepted for the Phase 1a CLI contract; future CLI changes must explicitly extend or revise it.
2. `getter init` creates/opens SQLite immediately, not JSONL durable storage.
3. The malformed-bundle scenario is only a migration failure skeleton, not full legacy import implementation.
4. The supported legacy schema range and bundle version remain deferred until real import mapping starts.
5. Migration reports are JSON-first. Markdown support summaries can be generated later for issue templates/support.
