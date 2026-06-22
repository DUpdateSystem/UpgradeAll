# 0002: Rust-managed SQLite storage

- Date: 2026-06-20
- Status: Accepted for the refactor plan

## Context

The rewrite needs a durable storage model that is owned by the headless engine rather than by a specific UI host. The 2026-06-20 plan rejects ad-hoc JSONL as the long-term store and requires a tested migration path from legacy Android Room data.

## Decision

Getter will own the new canonical SQLite storage. Legacy Android Room is a migration source, not the long-term source of truth. JSON/JSONL may exist only as import/export, diagnostics, fixtures, or alpha compatibility data, not as the official durable store for the rewritten product.

## Consequences

- Storage migrations can be tested at the getter layer without a UI.
- Flutter, CLI, and other hosts share the same durable model.
- A legacy import path must preserve supported existing Android data before the official Flutter Android release.
- Storage schema and canonical ID rules require tests before implementation changes.

## Alternatives considered

- Keep Room as the primary store. This preserves existing Android implementation but conflicts with a reusable getter engine.
- Keep JSONL initially and migrate later. This reduces early work but creates a second migration and risks shipping unstable persistence semantics.
