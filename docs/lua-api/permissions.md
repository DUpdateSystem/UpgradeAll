# Lua Permissions

> Status: Draft / living design record
> Date: 2026-06-21
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Default

Lua package scripts do not receive direct network access by default.

They can use getter-provided provider/source APIs.

## Free network permission

A package may declare free network access for live/9999-like logic or unusual upstreams.

When declared:

- getter exposes a direct Lua network host API;
- Flutter displays a yellow warning tag at App detail source/version level;
- use is not blocked.

## Timeouts

Network operations use normal network timeouts.

Lua script runtime itself does not use a timeout/fuel limit.

## v1 verification policy

v1 does not enforce repo/script/artifact verification.

Schema fields may exist for future verification, but enforcement is not a v1 requirement.
