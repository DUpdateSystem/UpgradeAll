# 0007: Getter CLI command contract

- Date: 2026-06-20
- Status: Accepted for the Phase 1a CLI contract

## Context

Getter CLI is a user-facing interface. Once Cucumber/Gherkin scenarios and step assertions are written, command names, output schemas, error schemas, side effects, and exit codes become supported behavior. ADR 0006 says the CLI needs explicit design before implementation.

The canonical 06-20 plan gives examples such as `getter app list`, `getter hub list`, and `getter legacy import-room-bundle <bundle.json>`. The refactor plan is AI/operator/CLI-first, so machine-readable output should be stable from the first slice. Phase 1a implemented this contract in the committed BDD-backed CLI spine.

## Decision

Getter CLI uses domain-noun subcommands and machine-readable JSON by default during the rewrite.

Initial supported command grammar:

```text
getter --data-dir <path> init
getter --data-dir <path> app list
getter --data-dir <path> hub list
getter --data-dir <path> legacy import-room-bundle <bundle.json>
```

Global conventions:

- `--data-dir <path>` is mandatory in early development and all BDD scenarios.
- JSON is the default output for supported commands.
- Human-readable output can be added later behind an explicit flag, but is not the first automation contract.
- Success payloads go to stdout.
- Error envelopes go to stdout when the command can run far enough to emit structured JSON; invalid CLI usage may use stderr/help text.
- Unstructured diagnostics must not be mixed into JSON stdout.

Success envelope shape:

```json
{
  "ok": true,
  "command": "app list",
  "data": {},
  "warnings": []
}
```

Error envelope shape:

```json
{
  "ok": false,
  "command": "legacy import-room-bundle",
  "error": {
    "code": "migration.invalid_bundle",
    "message": "Legacy Room export bundle is invalid",
    "report_path": "/path/to/report.json"
  }
}
```

Initial exit-code classes:

- `0`: success.
- `1`: generic failure not covered by a more specific class.
- `2`: invalid CLI usage.
- `10`: data/storage error.
- `20`: migration/import error.
- `30`: network/provider error.
- `40`: download error.

Storage convention:

- `getter init` creates or opens the canonical getter-owned SQLite storage. It must not initialize JSONL as durable product storage.
- `legacy import-room-bundle` returns a stable unsupported/not-implemented failure for syntactically valid bundles until the real Room import phase is implemented.
- Minimal Phase 1 storage may contain only metadata and empty app/hub tables, but it must be compatible with the accepted Rust-managed SQLite direction.

## Consequences

- BDD scenarios can assert stable JSON fields instead of vague text.
- AI/operator workflows get deterministic output from the beginning.
- Early development avoids accidentally treating platform defaults as part of the contract.
- Human-friendly CLI output remains possible later, but it must not destabilize automation.

## Alternatives considered

- Human-readable output by default with `--json` opt-in. Friendlier for terminals, but risks making prose the accidental contract.
- Plural commands such as `apps list`. This is common in some CLIs, but the canonical plan already uses singular `app list` and `hub list`.
- Platform-default data directory from the start. This is convenient for users but makes early BDD tests less isolated and can hide state leakage.

## Implementation note

Phase 1a executable CLI feature files are now implemented. Future changes should extend this contract explicitly rather than treating it as provisional.
