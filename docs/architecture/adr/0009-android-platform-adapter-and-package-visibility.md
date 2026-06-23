# ADR-0009: Android platform adapter and package visibility

> Status: Accepted for first implementation slice
> Date: 2026-06-23
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Decision

UpgradeAll will use a Rust-active platform adapter for Android platform capabilities.

Rust/getter-side native code defines the platform interface and actively calls the Android implementation. Android/Kotlin code supplies raw platform facts only. Flutter remains the product UI and renders getter-owned DTOs; it does not lead installed-app inventory scanning or turn Android package names into UpgradeAll package ids.

The first accepted platform capability is installed Android package inventory for `local_autogen` preview/apply workflows. The product Flutter APK declares:

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

This is an explicit product/distribution decision: UpgradeAll is an app updater and installed-app tracker, so broad installed-package visibility is core functionality rather than incidental implementation convenience.

## Rust-active adapter pattern

The Android implementation follows the same architectural pattern as `rustls-platform-verifier`:

1. A JNI entrypoint initializes platform access with the current JVM, application `Context`, and app `ClassLoader`.
2. Rust stores process-lifetime global references.
3. When Rust needs a platform capability, it attaches the current thread to the JVM, loads Android implementation classes through the app classloader, and calls static Kotlin/Java methods.
4. Kotlin/Android code returns data facts in a stable transport shape.
5. Rust validates/deserializes those facts before passing them to getter-owned workflows.

Rust must not use Android `FindClass` from arbitrary background threads for app classes. App classes are loaded through the stored app classloader.

## Installed inventory contract

The platform adapter returns a wrapper result:

```json
{
  "inventory": {
    "format": "upgradeall-installed-inventory",
    "version": 1,
    "items": [
      {
        "kind": "android_package",
        "package_name": "org.fdroid.fdroid",
        "label": "F-Droid",
        "version_name": "1.20.0",
        "version_code": 1020000
      }
    ]
  },
  "stats": {
    "total_seen": 123,
    "returned": 42,
    "filtered_system": 80,
    "filtered_self": 1
  },
  "diagnostics": []
}
```

The installed inventory is getter-compatible, but it remains raw platform fact data:

- Android supplies `package_name`, label, version name, and version code.
- Android/Flutter must not generate `android/<package>` package ids.
- Android/Flutter must not decide repository coverage, autogen candidates, generated Lua file paths, or tracking-state writes.
- Magisk modules are not part of this PackageManager capability. They require a separate root/Shizuku/Magisk capability decision.

## Scan options

The first scan options are:

```json
{
  "include_system_apps": false,
  "include_self": false
}
```

Defaults exclude system apps and the UpgradeAll application itself. Disabled-app filtering is not part of the first Rust interface; disabled packages are treated as installed PackageManager facts until a later product decision defines user-facing semantics.

## Getter and Flutter responsibilities

The product operation shape is:

```text
Flutter UI
  -> getter/native bridge: preview installed autogen
    -> Rust platform adapter: scan installed inventory facts
    -> getter core: plan local_autogen candidates/skips
  <- getter-owned preview DTO
```

Flutter may ask getter for preview/apply operations and render scan stats/diagnostics returned by getter. Flutter must not implement a separate Dart `InstalledInventoryPlatform` scanner or MethodChannel-led inventory flow for the product path.

CLI/dev workflows remain fixture-based:

```text
getter autogen installed preview --inventory installed.json
getter autogen installed apply --preview preview.json --accept-all
```

The CLI has no Android PackageManager, so fixtures remain the headless oracle for getter domain behavior.

## Permission policy

`QUERY_ALL_PACKAGES` is declared only in the Flutter product APK manifest path (`app_flutter`). The legacy native `:app` module remains reference-only and is not the rewrite product APK path.

The permission may have distribution-policy implications on app stores. The project accepts that trade-off for the rewrite product because full installed-app visibility is necessary for UpgradeAll's app-updater inventory and autogen workflows.

If lint/build tooling flags `QUERY_ALL_PACKAGES`, the manifest may suppress that lint with an inline comment and `tools:ignore="QueryAllPackagesPermission"`; this suppression must remain documented as policy, not treated as a generic lint cleanup.

## First implementation slice

The first slice is intentionally narrow:

- document this ADR and update existing boundary docs;
- add `QUERY_ALL_PACKAGES` to the Flutter product manifest;
- add a superproject Rust crate for platform adapter DTOs, errors, a `NoopPlatformAdapter`, and Android runtime/JNI initialization skeleton;
- add validation for that crate.

The first slice does not:

- make the reusable getter submodule depend on superproject-only crates;
- add Kotlin PackageManager scanner behavior;
- wire product native bridge operations;
- add Flutter installed-autogen UX;
- add Magisk scanning;
- add live downloads, background worker policy, installer URI/SAF semantics, or notification behavior.

## Consequences

Positive:

- Rust remains the active caller and owner of platform interface shape.
- The platform seam is testable with host DTO tests before device integration exists.
- Flutter cannot accidentally become the owner of inventory/autogen decisions.
- The model can later support other Android capabilities using the same runtime initialization pattern.

Costs and risks:

- The first slice is not yet product-complete; production bridge packaging into the Flutter APK still needs a later accepted implementation.
- The Rust platform DTOs must stay compatible with getter's installed inventory contract.
- JNI/runtime bugs require Android build/device validation beyond host unit tests.
- Broad package visibility is now an explicit product policy with distribution implications.
