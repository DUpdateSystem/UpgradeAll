# ADR-0006: Package-centric getter CLI command contract

> Status: Draft / implementation slice accepted
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

The getter CLI is a first-class user-facing interface for exercising Rust getter behavior without Flutter.

The supported rewrite CLI vocabulary is package/repository-centric. New commands should use `repo`, `package`, `app`, `storage`, and `legacy` nouns. The old `hub` noun is not a new domain model; it is kept only as a temporary Phase 1a compatibility command for legacy/background plans and must not grow into a hub-app architecture.

Initial implemented grammar:

```text
getter --data-dir <path> init
getter --data-dir <path> app list
getter --data-dir <path> repo list
getter --data-dir <path> repo add <repo-id> <path> [--priority <n>]
getter --data-dir <path> repo eval <repo-id>
getter --data-dir <path> repo validate <path>
getter --data-dir <path> package eval <package-id> [--repo <repo-id>]
getter --data-dir <path> storage validate
getter --data-dir <path> update check --fixture <fixture.json>
getter --data-dir <path> autogen installed preview --inventory <installed.json>
getter --data-dir <path> autogen installed apply --preview <preview.json> (--accept-all|--accept <package-id>...)
getter --data-dir <path> autogen cleanup preview --inventory <installed.json>
getter --data-dir <path> autogen cleanup apply --preview <preview.json> (--accept-all|--accept <package-id>...)
getter --data-dir <path> legacy import-room-bundle <bundle.json>
getter --data-dir <path> legacy import-room-db <db.sqlite>
getter --data-dir <path> legacy report-list
getter --data-dir <path> hub list   # temporary compatibility only
```

Global conventions:

- `--data-dir <path>` is mandatory in early development and BDD tests.
- JSON is the default output contract.
- Successful command payloads go to stdout.
- Structured command failures go to stdout as JSON error envelopes when possible.
- Invalid CLI usage may additionally use stderr/help text and exit code `2`.
- The CLI must call Rust getter/storage behavior; it must not duplicate product logic outside getter.
- CLI scenarios must invoke the built binary as an external process.
- `package eval <package-id>` without `--repo` evaluates the package from the highest-priority registered repository that contains that package id. `--repo <repo-id>` evaluates that exact repository and bypasses overlay resolution.

Success envelope shape:

```json
{
  "ok": true,
  "command": "repo list",
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

The first supported `legacy import-room-bundle` slice accepts a JSON bridge bundle with this shape:

```json
{
  "format": "upgradeall-legacy-room-bundle",
  "version": 17,
  "apps": [
    {
      "kind": "android",
      "installed_id": "org.fdroid.fdroid",
      "official_package_available": true,
      "common_conversion_available": false,
      "ignored_version": "1.20.0",
      "favorite": true
    }
  ]
}
```

It maps `apps[]` into getter tracked package state in `main.db`, writes a sanitized report under `migration-reports/`, and records `legacy-room-v17` migration completion. Malformed JSON uses `migration.invalid_bundle`; wrong format/version uses `migration.unsupported_bundle`.

`legacy import-room-db <db.sqlite>` is the first direct Room database import slice. It opens a copied/checkpointed legacy SQLite database read-only, requires `PRAGMA user_version = 17`, reads `app` and `extra_app` rows, maps known legacy app-id keys to readable package ids (`android/<packageName>` and `magisk/<moduleId>`), writes tracked package state and the `legacy-room-v17` migration record in one transaction, and emits sanitized report counts/warnings. Unsupported DB versions use `migration.unsupported_db`; unreadable or malformed DBs use `migration.invalid_db`. A DB with a mix of valid and invalid app rows imports valid rows and reports skipped-row warnings; a DB with app rows but zero importable app rows is treated as `migration.invalid_db` so migration completion is not recorded silently. This command does not import legacy `hub` as a new domain model; current hub/extra_hub rows are counted/dropped with warnings until a later accepted mapping exists.

`legacy report-list` returns sanitized migration report summaries through the same JSON envelope so app/test adapters do not need to inspect getter's data-directory layout directly.

The first installed-app autogen slice accepts an Android/platform-provided inventory DTO, computes generated fallback packages in Rust, previews before writing, and applies only after explicit `--accept-all` or `--accept <package-id>` confirmation. Getter owns the canonical repository path `<data-dir>/repositories/local_autogen`, fixed repo id `local_autogen`, default priority `-1`, deterministic package file path `packages/<kind>/<name>.lua`, and `autogen-manifest.json`. Candidates are skipped when any registered repository with priority higher than `local_autogen` already provides that package id. Applying installed autogen writes/registers `local_autogen` package files and tracks accepted packages in `main.db` when they are not already resolved; existing user state (`enabled`, `favorite`, `ignored_version`) and existing non-missing resolution metadata are preserved. Cleanup preview/apply only targets manifest-managed `local_autogen` packages missing from the current installed inventory. Cleanup refuses stale/tampered previews that do not match the current manifest and deletes tracked state only for rows still owned by `local_autogen` generated packages. If an existing autogen file was modified, getter preserves that content into the user-authored `local` repo before regenerating or deleting the managed autogen file.

`update check --fixture <fixture.json>` is the first Phase D offline update-check slice. The fixture contract is explicitly offline and uses `format = "getter-offline-update-check"`, `version = 1`, `package_id`, optional `installed_version`, optional `ignored_version`, and normalized candidate/artifact DTOs. The command returns `network_required = false`, a getter-owned status (`update_available`, `up_to_date`, `no_candidates`, or `ignored`), the selected update when one exists, and generated download/install action DTOs. An update-selected result must have an actionable artifact; a selected candidate without artifacts is a structured update-check error rather than `update_available` with no actions. It reuses Rust getter update selection and version comparison; it does not run providers, perform downloads, persist download tasks, stream events, or call Android installers.

`repo validate <path>` validates a repository path offline without requiring it to be registered first. It returns `valid`, `diagnostics`, `package_count`, and `network_required = false`; diagnostics are getter-owned structured records with stable codes, message, severity, source path, and optional package id/field.

Exit-code classes:

- `0`: success.
- `1`: generic structured command failure.
- `2`: invalid CLI usage.
- `10`: data/storage error.
- `20`: migration/import error.
- `30`: future network/provider error.
- `40`: future download error.

## Context

The rewrite architecture requires getter core to be independently exercisable before Flutter UI work. A CLI-first spine proves that storage, repository loading, Lua package evaluation, migration error reporting, and later update workflows can run without platform/UI code.

Older Phase 1a docs accepted `getter hub list` as a temporary smoke command. Newer architecture docs reject hub-app as the future model. This ADR reconciles those facts: `hub list` may remain as a no-op compatibility smoke while package/repository commands become the forward path.

## Consequences

Positive:

- The CLI can be used for BDD/runtime evidence and AI/operator workflows.
- Flutter cannot hide missing getter behavior behind UI code.
- Package/repository terminology stays aligned with ADR-0001.
- Legacy import failures can be tested non-destructively before full Room import exists.

Costs:

- Command grammar changes must be documented and covered by Gherkin tests.
- The temporary `hub list` compatibility command must be removed or clearly deprecated later.
- CLI output schemas become a supported automation contract.

## Non-goals

- No old hub-app model revival.
- No live network provider behavior in the initial CLI smoke slice.
- No Android/platform DB copy or WAL checkpoint implementation in the CLI contract itself; platform adapters prepare a consistent DB file and getter owns import semantics.
- No Flutter UI behavior in CLI tests.
