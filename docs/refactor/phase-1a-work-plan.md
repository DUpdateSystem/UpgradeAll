# Phase 1a Work Plan: Getter CLI BDD Spine

Date: 2026-06-20
Status: Approved to start implementation

## Goal

Create the first executable TDD/BDD spine for the rewrite through the Getter CLI, without starting Flutter screen work and without reviving the stashed direct-JNI/RPC rewrite as accepted architecture.

This work implements the first user-facing CLI behavior slices from `docs/refactor/phase-1-getter-cli-bdd-plan.md` and follows the CLI contract in `docs/adr/0007-getter-cli-command-contract.md`.

## Approved contract for this slice

Initial supported commands:

```text
getter --data-dir <path> init
getter --data-dir <path> app list
getter --data-dir <path> hub list
getter --data-dir <path> legacy import-room-bundle <bundle.json>
```

Phase 1a constraints:

- JSON output is the default machine-readable CLI contract.
- `--data-dir <path>` is mandatory in tests and early development.
- `getter init` creates/opens canonical getter-owned SQLite storage, not JSONL durable storage.
- `app list` and `hub list` return empty collections for newly initialized storage.
- `legacy import-room-bundle` in Phase 1a only covers malformed/corrupted bundle rejection plus explicit unsupported/not-implemented handling for syntactically valid bundles; no full Room import mapping yet.
- All user-facing CLI behavior added here needs Cucumber/Gherkin BDD coverage.
- Internal storage/output/parser behavior should use traditional Rust tests where appropriate.

## Validation contract

A successful Phase 1a implementation must provide evidence for:

1. A Cucumber/Gherkin CLI BDD runner exists for getter CLI scenarios.
2. A failing `init` scenario was added first and is made green.
3. `getter --data-dir <tmp> init` succeeds and emits valid JSON with `ok: true`.
4. `getter --data-dir <tmp> app list` succeeds after init and emits an empty app list.
5. `getter --data-dir <tmp> hub list` succeeds after init and emits an empty hub list.
6. `getter --data-dir <tmp> legacy import-room-bundle <bad.json>` fails non-destructively with a structured migration error and a sanitized JSON report path.
7. `getter --data-dir <tmp> legacy import-room-bundle <valid.json>` fails with a stable unsupported/not-implemented migration error and does not mutate the initialized store.
8. SQLite is used for the durable getter store initialized in this slice.
9. Traditional Rust tests cover core/internal pieces that are not best expressed as Gherkin.
10. `just verify` is extended to include the new getter CLI BDD/internal tests or a transitional target that proves them.

## Non-goals

- Do not implement Flutter UI.
- Do not implement full legacy Room export/import mapping.
- Do not implement provider registry, update checks, or downloads beyond what empty list scenarios require.
- Do not delete or replace Android RPC/JNI integration as part of this slice.
- Do not use JSONL as the durable product store.
- Do not apply the pre-sync stash wholesale.

## Expected implementation order

1. Make sure the getter submodule is on a working branch rather than detached HEAD.
2. Add Rust CLI test dependencies and the Cucumber runner skeleton.
3. Add the first Gherkin feature for `getter init` and observe it fail.
4. Implement minimal CLI parsing/output/storage init to pass `init`.
5. Add internal tests for SQLite init and output envelope serialization.
6. Add `app list` and `hub list` scenarios and implementation.
7. Add malformed legacy bundle failure scenario and minimal non-destructive report implementation.
8. Extend `just verify` with the new getter test command(s).
9. Run focused validation and report changed files, commands, failures, and residual risks.

## Handoff requirements

The worker must report:

- changed files in the superproject and getter submodule;
- tests/features added;
- commands run with exit codes;
- whether SQLite storage is actually initialized;
- whether each BDD scenario passes;
- any blocked items or decisions needed before continuing.
