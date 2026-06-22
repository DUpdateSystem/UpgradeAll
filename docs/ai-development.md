# AI Development Workflow

This repository is being prepared for a test-driven Flutter + getter rewrite.

## Baseline protection

- Preserve user work before syncing or rewriting.
- Current planning baseline: superproject `4a1aae1d44a418989b0d3d28528cacff0cc066c0`, getter submodule `f011d9b4b9a15f83cd39c86e781ad8830a8ecae6`.
- The canonical 06-20 plan is copied at `docs/refactor/2026-06-20-upgradeall-flutter-getter-rewrite-complete-plan.md`.
- The pre-sync implementation stash is historical context, not accepted architecture.
- Do not apply stash contents wholesale without a fresh review against the ADRs and the canonical plan.

## Required loop

For every behavior change:

1. Identify whether the behavior is user-facing or internal.
2. User-facing App/CLI behavior: add or update a Cucumber/Gherkin scenario first.
3. Internal behavior: add or update the smallest native unit/integration test first.
4. Confirm the test fails for the expected reason.
5. Implement the smallest change.
6. Run focused validation.
7. Run `just verify` before reporting completion.

## User-facing BDD scope

Complete BDD coverage is required for:

- UpgradeAll App workflows.
- Getter CLI commands, outputs, errors, and exit codes.
- User-visible migration success/failure/recovery behavior.

BDD is not required for every private function or algorithm. Internal behavior still requires automated tests through the appropriate native framework.

## Planning rules

- Update `CONTEXT.md` immediately when domain terms become clear.
- Add ADRs only for costly, surprising, trade-off decisions.
- Keep getter product behavior out of UI-only code.
- Keep stable test IDs in UI contracts.
- Do not start Flutter screen work before getter contracts and acceptance scenarios exist.

## Commands

Use `just --list` to see available commands.

Phase 0 command expectations:

- `just status` checks branch/submodule state.
- `just cargo-metadata` checks Rust manifests stay loadable.
- `just gradle-projects` checks Gradle can configure the current project graph.
- `just verify` runs the current lightweight verification skeleton.

Later phases must extend `just verify` to include the real Cucumber, Rust, Flutter, migration, and Android release checks.
