# BDD and TDD Plan

Date: 2026-06-20

## Rule

Every behavior-changing implementation starts with a failing automated test.

Cucumber/Gherkin is required for user-facing behavior. The mandatory user-facing coverage surfaces are:

1. UpgradeAll App workflows.
2. Getter CLI commands and contracts.
3. Migration success/failure/recovery behavior visible to users.

Internal interfaces use traditional unit/integration/property tests unless they become supported user-facing contracts.

## Why this split

BDD is strongest at integration and acceptance behavior. It is not the best tool for every low-level algorithm test. Therefore:

- Use Gherkin for observable workflows and supported command behavior.
- Use Rust/Kotlin/Dart native tests for internal logic, parsing, storage invariants, migration units, DTO serialization, and edge-case algorithms.
- Use widget/UI tests for rendering states and stable IDs.

## Cucumber conventions

Feature files should use product language from `CONTEXT.md`.

Required tags:

- `@app` for UpgradeAll App scenarios.
- `@getter-cli` for Getter CLI scenarios.
- `@migration` for legacy migration scenarios.
- `@smoke` for scenarios that must run in the fastest acceptance pass.
- `@regression` for scenarios created from bug fixes.

Scenario naming should describe behavior, not implementation. Prefer:

```gherkin
Scenario: User sees recoverable migration failure
```

not:

```gherkin
Scenario: Rust importer returns error code 17
```

## Planned suites

### Getter CLI BDD

Purpose: drive headless user-facing behavior before Flutter UI depends on it.

Coverage examples:

- Initialize a new data directory.
- Import a legacy bundle successfully.
- Report migration failure without destructive fallback.
- List apps in stable JSON output.
- Renew one app and report progress/events.
- Submit a download and report task state.
- Return documented non-zero exit codes for invalid input, network failure, and migration failure.

Implementation direction:

- Use Rust Cucumber for CLI behavior where practical.
- Step definitions invoke the built CLI binary and assert stdout/stderr/exit status and resulting state.
- Lower-level getter behavior stays covered by native Rust tests.

### UpgradeAll App BDD

Purpose: cover user-visible app behavior with stable route/action/state IDs.

Coverage examples:

- Fresh launch reaches the home route.
- Legacy migration success reaches the migrated app list.
- Legacy migration failure reaches recovery actions.
- User opens app list, app detail, and renew-all flow.
- User submits a download and sees task progress/failure/success state.
- Empty/loading/error/content states are addressable by stable IDs.

Implementation direction:

- Feature files are the acceptance source of truth.
- UI automation must use stable IDs, not localized text, wherever possible.
- The concrete runner can be implemented through Cucumber-compatible step definitions over Flutter integration tests and/or black-box automation, but the scenarios remain Gherkin.

### Internal traditional tests

Required for:

- Version comparison.
- Provider parsing.
- Storage migrations and canonical IDs.
- Download orchestration edge cases.
- DTO serialization compatibility.
- Library API contract behavior that is not directly a CLI/App workflow.

## Red-green-refactor loop

1. Select a user-facing behavior or internal behavior.
2. Write the smallest failing test:
   - Gherkin scenario for App/CLI behavior.
   - Native unit/integration test for internal behavior.
3. Run the focused target and confirm failure for the expected reason.
4. Implement the smallest change.
5. Run focused tests until green.
6. Refactor with tests green.
7. Run `just verify` before handing off.

## Phase 0 acceptance

Phase 0 is complete when docs and verification skeleton exist. It does not need the full Cucumber runner wired yet, but it must prevent feature implementation from proceeding without a test plan and a failing-test entrypoint.
