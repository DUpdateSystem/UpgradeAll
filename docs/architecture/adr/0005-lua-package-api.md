# ADR-0005: Lua package API and Rust validation boundary

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

getter embeds Lua for package definitions, reusable package helpers and autogen templates.

The Lua/Rust boundary is treated as an RPC/serialization boundary: Lua returns JSON-like tables; Rust validates and deserializes them into typed structs.

Lua scripts do not receive mutable Rust domain objects.

## Language

Use Lua via `mlua` unless implementation evidence later proves a blocker.

## Boundaries

Lua can use normal Lua tables/functions/metatables, reusable modules via `require`, package import helper for parent packages, and host-provided provider/network APIs based on permissions.

Rust owns schema validation, typed domain model, persistence, event dispatch, download task state and platform callback dispatch.

## Lifecycle phases

App-centric phase names:

```text
preflight
setup
match
discover
prepare
select
resolve        # name still open; alternative: make_actions
post_update
```

`plan` is rejected because it is too vague.

## Network permission model

Lua has no direct network API by default.

If a package declares free network permission, getter exposes a direct network host API to that Lua environment and Flutter displays a yellow warning tag in App detail source/version UI.

This tag is informative and does not block use.

## Templates

Templates under `templates/` are Lua generators that output Lua package file content. They are distinct from runtime package modules.

## Validation

Rust validates package id/path consistency, known package kind, required fields, installed target schema, phase function presence/type where required, permission schema, action schema and URL/action validity.

Errors must distinguish Lua runtime errors, schema validation errors and domain validation errors.
