# 0005: TDD and Cucumber behavior coverage policy

- Date: 2026-06-20
- Status: Accepted for the refactor plan

## Context

The refactor must be test-driven. The user clarified that Cucumber/Gherkin BDD is required for user-facing behavior, especially the UpgradeAll App and Getter CLI. BDD should cover integration-level behavior, while internal algorithms and module boundaries should keep faster traditional tests.

Cucumber documentation defines behavior specs as Gherkin `Feature`, `Scenario`, `Given`, `When`, and `Then` files with tags, data tables, and scenario outlines. Cucumber step definitions bind those phrases to executable code. The Rust Cucumber implementation uses `.feature` files, a per-scenario `World`, and async step functions.

## Decision

Every behavior-changing implementation must start from a failing automated test.

Cucumber/Gherkin is mandatory for supported user-facing interfaces:

- UpgradeAll App workflows.
- Getter CLI commands, output contracts, errors, and exit codes.
- User-visible migration success and recovery behavior.
- Cross-boundary acceptance behavior where a user action depends on getter outcomes.

Internal interfaces do not require Gherkin unless promoted to supported user-facing contracts. They should use the fastest appropriate traditional tests: Rust unit/integration/property tests, storage migration tests, Kotlin/Dart unit tests, widget tests, and focused integration tests.

## Consequences

- BDD scenarios become acceptance contracts, not a replacement for all unit tests.
- Getter CLI must be designed before implementation because its behavior scenarios need stable commands, JSON/human output rules, and exit-code semantics.
- UI screens must expose stable test IDs so scenarios do not depend on localized text.
- CI/verification must separate fast internal tests from slower BDD acceptance tests while keeping both required before release.

## Alternatives considered

- Require Gherkin for every test. This maximizes uniformity but slows feedback and makes low-level Rust/Kotlin/Dart tests verbose.
- Use only native unit/integration tests. Faster initially, but fails the requirement that user-facing behavior be expressed as executable behavior specs.
