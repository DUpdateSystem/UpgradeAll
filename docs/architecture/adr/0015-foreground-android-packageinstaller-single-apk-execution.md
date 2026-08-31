# ADR-0015: Foreground single-APK installation through Android PackageInstaller

> Status: Accepted for implementation
> Date: 2026-08-31
> Project: UpgradeAll rewrite — Flutter APP + Rust getter core + Lua package repository model

## Context

ADR-0014 defines a Getter-owned, typed Android APK handoff. The first prepare-only operation evaluated a package by package id, but the product runtime now has a stronger continuity requirement: once Getter issues an update action and downloads its artifact, installation must consume that exact sealed runtime task. Re-evaluating the package at click time could select a different repository candidate, version, target, or artifact while the original task is later marked accepted.

## Decision

The Android product installs one foreground, user-requested base APK with `PackageInstaller`.

Getter remains the sole owner of refresh, candidate selection, Manifest-backed staging and integrity, Android target validation, and the versioned `PlatformInstallHandoff`. When Getter issues a registered-package update action, it seals the selected repository id, package version, Android package target, installer artifact name, staged filename, and Manifest SHA-256 into that action. The submitted runtime task owns the sealed plan and its `downloaded_file` record.

App detail and Downloads call the same Flutter install coordinator with only the exact runtime task id. The coordinator calls the strict native `prepareInstallTask` operation; Flutter never invokes package-scoped preparation for a runtime install. Getter accepts only a task in `running / waiting_user / install_handoff`, requires its sealed Android APK plan and task-owned downloaded file, verifies the Getter-owned `downloads/<task-id>/<filename>` path, recorded size and digest, and current file digest, and returns a typed handoff containing the same `task_id`. This preparation performs no repository evaluation, candidate reselection, or second download. The earlier package-scoped prepare operation remains compatible for non-runtime callers, but it is not the product runtime path.

Flutter passes the resulting typed handoff to the Android adapter. Kotlin consumes only the Getter-supplied Android package name and absolute staged APK path; it must not derive identity or installability from a filename, task title, path shape, requested version, or duplicated package policy.

The Android adapter:

1. declares `android.permission.REQUEST_INSTALL_PACKAGES`;
2. on API 26 and newer, checks `PackageManager.canRequestPackageInstalls()` before creating a session;
3. when authorization is absent, opens `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` for the UpgradeAll package and returns an authorization-required outcome; after the app resumes, the same shared coordinator rechecks and lets the user retry;
4. creates one `PackageInstaller.SessionParams(MODE_FULL_INSTALL)` session and sets the Getter-supplied target package name;
5. streams the staged file to `openWrite("base.apk", 0, apkLength)`, calls `fsync`, closes the stream, and abandons the session if local staging fails;
6. commits with a mutable explicit broadcast `PendingIntent` targeting a non-exported receiver;
7. treats `STATUS_PENDING_USER_ACTION` as intermediate, immediately launches only the framework-provided `Intent.EXTRA_INTENT`, and continues waiting for a terminal callback;
8. treats `STATUS_SUCCESS` as terminal success, `STATUS_FAILURE_ABORTED` as a terminal aborted/cancellation outcome, and every other failure status as terminal failure while preserving the numeric platform status and optional raw message for diagnostics only.

Session correlation is process-memory only. One foreground install may be active at a time. The adapter delivers one terminal result for a session and then clears its correlation state. It does not infer success from returning to Flutter, from launching confirmation, or from a version comparison.

After a terminal installer result, the shared coordinator reports the result against the same Getter runtime task before reloading Getter startup state. Success sends `accepted` and completes that task. Cancellation or platform failure sends `rejected`, fails that task with Getter's stable user-rejected result, and exposes retry; retry resumes the same task at `waiting_user / install_handoff` and reuses its staged artifact. Authorization-required is nonterminal: no runtime user result is sent, the task remains waiting, and retry rechecks authorization and prepares the same task again. Getter's refreshed installed inventory is authoritative; the UI does not infer installation success from the requested version.

## Verified basis

The Getter public seams verify that repository metadata, package Lua, and Manifest mutation after action issuance cannot change the task-scoped target, version, artifact path, or digest; altered staged bytes are rejected before a handoff is returned. Flutter and Kotlin tests verify strict task-only bridge requests, handoff task correlation, one shared coordinator, authorization retry, cancellation/failure retry, terminal runtime user results, overlap rejection, and exactly-once inventory refresh.

On emulator `upgradeall_api34` (`emulator-5554`, Android API 34), the signed `app/release/UpgradeAll_0.20-alpha.4.apk` fixture produced Getter's handoff for package `net.xzos.upgradeall` with SHA-256 `deea14848c1b274c8a5884726d545309386fc0f884933cab1808e0d4e79b2f73`.

The debug tracer verified:

- per-app unknown-source authorization changed `canRequestPackageInstalls()` from false to true and allowed retry;
- commit delivered `STATUS_PENDING_USER_ACTION` and the framework confirmation activity;
- pressing Cancel delivered `STATUS_FAILURE_ABORTED` and left the target package absent;
- pressing Install delivered `STATUS_SUCCESS`, after which PackageManager reported package `net.xzos.upgradeall`, version `0.20-alpha.4`, version code `105`;
- the explicit mutable receiver received both intermediate and terminal callbacks for the same session.

Final product acceptance on the same emulator exercised both public UI entries against the updated task-scoped bridge. App detail and Downloads each launched the signed fixture, received Cancel as `STATUS_FAILURE_ABORTED`, exposed retry for the same Getter task, then launched that task again and received `STATUS_SUCCESS`. Each run asserted the original task became failed after cancellation and completed after retry, and Getter's refreshed startup inventory reported `0.20-alpha.4`; PackageManager independently reported version code `105`.

## Explicitly excluded

- split APKs, APK sets, and multi-package sessions;
- silent, device-owner, root, or Shizuku installation;
- `FileProvider`, install intents built from file paths, or URI grants;
- background queues, durable installer state/history, session recovery, or install notifications;
- treating runtime download filenames or paths as install eligibility;
- parsing raw `PackageInstaller` messages as stable product logic;
- claiming terminal callback delivery after force-stop, process death, reboot, or OEM/device-policy interruption;
- re-evaluating a package, selecting another candidate, or downloading again while preparing an existing runtime task.

## Consequences

The first user-usable install path stays small and platform-native while preserving Getter's ownership boundary and exact action → task → staged artifact → platform handoff continuity. Authorization denial, user cancellation, and platform failure remain retryable foreground outcomes rather than dead ends. Process loss can make the final callback and in-memory task result unknown; because this slice deliberately has no durable installer state, the next startup refreshes Getter inventory. If the target is installed, inventory reflects it; otherwise the user can issue a fresh update task and retry.
