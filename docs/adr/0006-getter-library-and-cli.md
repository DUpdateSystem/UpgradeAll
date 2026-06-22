# 0006: Getter as both library and CLI

- Date: 2026-06-20
- Status: Accepted for the refactor plan

## Context

Getter must serve multiple hosts. The UpgradeAll App needs an embeddable engine, while AI/operator workflows need a scriptable command-line surface. The current Rust crate already has a library entrypoint and a placeholder binary, but that does not define a supported library or CLI contract.

## Decision

Getter will be both:

1. A library: the stable embeddable engine surface for UI hosts and integration adapters.
2. A CLI: the supported command-line user interface for verification, automation, diagnostics, and developer workflows.

The CLI is a user-facing interface and therefore requires complete Cucumber/Gherkin coverage for supported commands. The library requires traditional unit/integration tests for internal behavior and contract tests where exposed to supported hosts.

The CLI must not become an unrelated second implementation. It should call the same getter core behavior as the library.

## Consequences

- CLI command shape, output mode, error model, and exit codes need explicit design before implementation.
- Behavior scenarios for CLI can drive core workflow design without needing Flutter first.
- The library/CLI split helps prevent UI code from becoming the only way to exercise product behavior.
- Public module visibility must be distinguished from supported API contract.

## Alternatives considered

- Library only. Simpler, but weaker for AI/operator workflows and headless verification.
- CLI only. Useful for automation, but not sufficient for embedding in the app.
- Separate CLI logic. Faster to prototype but risks drift from app behavior.
