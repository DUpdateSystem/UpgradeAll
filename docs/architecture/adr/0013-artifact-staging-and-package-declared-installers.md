# ADR-0013: Artifact staging and package-declared installer commands

> Status: Accepted for implementation
> Date: 2026-07-14
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Context

Getter can actively inspect/check a tracked package, issue a current-runtime action, and download one artifact through an in-memory task. That seam is insufficient for normal package-manager semantics:

- a user may explicitly download the latest available release even when that version is already installed;
- one release may contain multiple artifacts;
- downloads should survive the CLI process and be reusable by a later install command;
- installation is platform/package policy and may require an external executable;
- package source is trusted through the repository trust model, while Getter must still avoid shell interpolation and validate all artifact references.

The user model follows the separation found in source package managers such as Gentoo Portage: fetching prepares declared source/artifact files; installing consumes already prepared files. Install does not infer what to download, and the download operation does not parse installer arguments.

## Decision

### Commands

The normal CLI gains:

```text
getter --data-dir <path> app download <package-id>
getter --data-dir <path> app install <package-id>
```

Both commands actively refresh the package through getter-owned Provider policy and select the highest candidate allowed by package policy. They do not use the installed version or `pin_version` as the target baseline. An explicitly requested download/install is allowed even when the same version is already installed.

The product CLI does not accept artifact URLs, output paths, endpoint/provider payloads, fixture bodies, transports, cache modes, executable names, or raw command arguments.

### Download and install remain separate operations

`app download` prepares every artifact declared by the selected candidate. It does not read or validate the installer command.

`app install` first invokes the same artifact preparation operation, then independently validates and executes the selected candidate's installer declaration. The installer may reference only artifacts declared by that candidate. A missing or unknown artifact reference is an installer error; install never silently adds a download inferred from argv.

### Artifact integrity and staging

The package-level `Manifest` beside version Lua is the authoritative checksum list. Getter/autogen generates and refreshes it from trusted Provider checksum facts; package authors do not duplicate artifact SHA-256 values in version Lua merely to support downloading.

The existing simple Manifest line format is preserved:

```text
<hex-digest> <file-name>
```

No entry-type keyword or second manifest schema is introduced. Existing 128-hex SHA-512 provider/source-response entries and 64-hex SHA-256 artifact entries may coexist; Getter infers the algorithm from digest length. Multiple checksum memberships may share one filename because different release versions commonly reuse filenames.

Artifact lookup uses the candidate's declared full filename. When a standard Provider supplies a valid upstream SHA-256 fact, Getter requires that exact digest to be a Manifest membership for the filename and uses it; the Provider fact is a matching hint while Manifest membership remains authoritative. When the candidate has no SHA-256 fact, exactly one SHA-256 Manifest membership for the filename resolves it. Zero matches is missing and multiple matches are ambiguous. Malformed Manifest syntax or unresolved artifact integrity fails before artifact-byte transport; Provider metadata refresh may occur first because it is needed to select the candidate and its checksum facts.

Final files live directly under:

```text
<data-dir>/downloads/<sha256>-<sanitized-full-file-name>
```

A new transfer writes:

```text
<data-dir>/downloads/<sha256>-<sanitized-full-file-name>.download
```

The temporary path is formed by literally appending `.download` to the complete final path, not by replacing a filename extension. Before artifact-byte transport, Getter checks the complete final and temporary path namespace for final/final, temporary/temporary, and final/temporary cross-collisions across all selected artifacts.

Getter computes SHA-256 while downloading, compares it with the package Manifest entry, flushes/syncs the file, then atomically renames the `.download` file to the final path. A checksum mismatch never creates the final file.

If the final path already exists, Getter reports the artifact as reused and does not rehash it. This deliberately treats the getter-owned final filename as the completed-download marker; manual replacement by the user is outside Getter's guarantee. A retry overwrites the `.download` path. Resume/range transfer is deferred.

Artifact filenames are always sanitized by Getter. The digest prefix is lowercase hexadecimal.

### Lua installer schema

Installer policy is returned by the package version Lua result and may be factored into reusable `luaclass` modules. Each update candidate has at most one installer command.

The accepted v1 shape uses mixed structured argv:

```lua
local installer = require("luaclass.installer")

return package_version {
  updates = {
    {
      version = "1.2.3",
      artifacts = {
        {
          name = "app.apk",
          file_name = "UpgradeAll.apk",
          url = release_url,
        },
      },
      install = installer.command {
        executable = "adb",
        args = {
          "install",
          "-r",
          installer.artifact("app.apk"),
        },
      },
    },
  },
}
```

The JSON-like boundary is:

```json
{
  "executable": "adb",
  "args": [
    "install",
    "-r",
    { "artifact": "app.apk" }
  ]
}
```

A string argv entry is literal. An object entry must be exactly one artifact reference. Shell command strings, string placeholders, pipes, redirects, command substitution, conditional pipelines, and arbitrary environment declarations are not supported.

The installer declaration is trusted repository content and does not require a separate `allow_execute` permission. Getter resolves artifact references to absolute staged paths, locates the executable through cross-platform PATH lookup, displays the resolved command before execution, and invokes it directly with structured argv without a shell.

If the executable is not found, `app install` exits nonzero with stable code `installer.command_not_found`. If the child exits unsuccessfully, it returns `installer.command_failed`. Downloaded artifacts remain staged after either error. Captured stdout/stderr is bounded before it enters a response or diagnostic.

### Provider and Manifest implications

Provider-normalized artifacts preserve upstream SHA-256 facts when available so getter-owned autogen/refresh can generate the package Manifest. F-Droid already supplies APK hashes. GitHub release assets use the official asset `digest` when it is a valid `sha256:<hex>` value. The generated Manifest keeps the existing `<digest> <filename>` syntax and may also retain provider/source-response SHA-512 provenance lines.

`app download` and `app install` resolve artifact integrity from the package Manifest, not from an inline Lua checksum. If Getter cannot generate or find a valid SHA-256 Manifest entry for a selected artifact, that artifact remains inspectable/checkable but cannot be staged.

### Runtime scope

Artifact staging is durable filesystem state, not a durable runtime task. This ADR does not add task DB persistence, WorkManager, process-restart task recovery, pause/resume, or background notification behavior. Existing in-memory runtime download behavior remains compatible and may later converge on the staging primitive.

## Consequences

- Downloaded artifacts can be reused safely by path identity across CLI invocations.
- Explicit download/install is decoupled from installed-version update checks.
- Packages can express platform-specific installers without moving installation policy into Flutter/Kotlin or shell strings.
- Multi-artifact releases are supported from the first staging contract.
- Artifacts without a matching package Manifest SHA-256 entry are intentionally rejected for durable staging.
- Repository trust now includes installer command review; signing/review UX remains the repository trust mechanism.

## Deferred

- user-selected versions or artifacts;
- signature formats beyond SHA-256;
- resume/range downloads;
- parallel transfer policy;
- installer pipelines or multiple commands;
- environment-variable declarations;
- sandboxing external installers;
- Android Package Installer bridge and install-result callbacks;
- persistent tasks and background/restart recovery.
