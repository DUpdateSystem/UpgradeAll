# ADR-0014: Prepare-only Android APK install handoff

> Status: Accepted for implementation
> Date: 2026-07-17
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Context

ADR-0013 defines Manifest-backed artifact staging and the structured `installer.command` declaration for environments that can execute an external program directly. The Android Flutter product cannot assume that commands such as `adb` or `pm` exist, and Android installation ultimately requires a platform API rather than a generic child process.

The first Android-specific slice needs to preserve the architecture boundary before any installation UI or `PackageInstaller` lifecycle is designed. Getter must continue to own package evaluation, update refresh and selection, artifact integrity, and installer validation. JNI, Kotlin, and Dart must only transport the resulting typed request.

## Decision

A package version may declare one Android APK installer:

```lua
local installer = require("luaclass.installer")

return package_version {
  updates = {
    {
      version = "1.2.3",
      artifacts = {
        {
          name = "app.apk",
          file_name = "Example.apk",
          url = release_url,
        },
      },
      install = installer.android_apk {
        artifact = installer.artifact("app.apk"),
      },
    },
  },
}
```

The preserved JSON-like Lua boundary is tagged and structured:

```json
{
  "kind": "android_apk",
  "artifact": { "artifact": "app.apk" }
}
```

Getter owns active provider refresh, candidate selection, Manifest-backed staging and integrity verification, and validation of the installer declaration. It accepts this installer only for exactly one Android package target and exactly one referenced staged artifact whose declared file is an `.apk`. Missing, duplicate, unknown, malformed, non-APK, or non-Android inputs fail in Getter before a platform request is returned.

A successful prepare operation returns this versioned, Getter-owned Android APK install handoff:

```json
{
  "format": "getter-platform-install-handoff",
  "version": 1,
  "package_id": "android/example/com.example.app",
  "repository_id": "local",
  "package_version": "1.2.3",
  "request": {
    "kind": "android_apk",
    "target": {
      "kind": "android",
      "package_name": "com.example.app"
    },
    "artifact": {
      "name": "app.apk",
      "path": "/absolute/getter/downloads/<digest>-Example.apk",
      "sha256": "<64 lowercase hexadecimal characters>",
      "status": "downloaded"
    }
  }
}
```

The DTO identifies the selected package, repository, version, validated Android package target, and single verified staged APK artifact needed by the later platform adapter. The artifact status truthfully describes staging (`downloaded` or `reused`). The handoff itself has no lifecycle status: it is a prepare-only value, not an installation task, queued request, or completion record.

The handoff crosses the product boundary through JNI, Kotlin, and Dart as transport data only. Kotlin and Dart do not refresh providers, select releases, resolve package policy, inspect the Manifest, choose artifacts, or reinterpret Getter validation. Flutter may consume the typed handoff in a later UI slice, but this decision adds no UI.

Existing `installer.command` syntax, JSON representation, validation, and direct no-shell CLI execution remain unchanged. An `android_apk` declaration is not converted into a command and is not executed by the command installer path.

## Runtime refinement in ADR-0015

ADR-0015 preserves this declaration and handoff version while tightening product runtime continuity. Package-scoped prepare remains compatible, but App detail and Downloads do not call it. Getter instead seals the selected Android target, version, artifact filename, and Manifest digest into the issued action; the resulting runtime task owns that plan and its task-scoped downloaded file. Strict task preparation accepts only the exact waiting install-handoff task, performs no repository re-evaluation or second download, revalidates the staged file, and adds `"task_id": "task-…"` to the version-1 handoff. Its artifact path is the Getter-owned `downloads/<task-id>/<filename>` path. The shared coordinator requires the returned task id to match before invoking Android.

The prepare-only package handoff shown above and the task-scoped handoff are therefore two compatible uses of the same typed DTO: only the task-scoped form is allowed to complete a product runtime task.

## Explicitly excluded

This slice does not include:

- Android `PackageInstaller` execution or session management;
- install buttons, confirmation UI, or other Flutter product UI;
- durable handoff persistence;
- split APK, APK set, or multi-artifact Android installation;
- `FileProvider`, `content://` URI generation, or URI grants;
- installation completion callbacks, results, or status state machines;
- reuse of the legacy `core-installer` module;
- physical-device or emulator installation tests.

These concerns require separate decisions after the prepare-only contract is implemented and validated.

## Consequences

Package authors gain a platform-specific declaration without embedding Android commands in package policy. Getter remains the single owner of the selected and integrity-verified install input, while the product bridge remains a narrow transport seam. The contract can later feed an Android platform adapter without committing this slice to a particular `PackageInstaller`, URI-sharing, UI, persistence, or result model.
