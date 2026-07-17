# Download bytes MVP

## Scope

Implement the next UpgradeAll rewrite slice: a getter-owned real-byte download executor for the current in-memory runtime.

Accepted behavior:

- Product callers submit only a getter-issued opaque `action_id`.
- Rust getter consumes the sealed action plan, chooses the download artifact URL from that plan, downloads bytes through getter-owned transport, writes the artifact under getter-owned local files, updates task progress/state, and emits `task_changed` notifications.
- Flutter/Kotlin/native bridge continue to submit `action_id` and render/query task DTOs; they do not receive raw URL, raw payload, provider fixture, cache mode, or transport-control inputs.

## Non-goals for this slice

- No task DB persistence and no cross-process, app-restart, or sleep recovery.
- No WorkManager/background worker/foreground-service/system notification semantics.
- No complex retry/pause/resume download manager.
- No APK installer handoff implementation beyond the existing waiting-user install-handoff phase.
- No provider parsing/cache/transport logic in Flutter/Dart/Kotlin.

## Architecture decisions for this slice

- Keep `getter-core` storage/network agnostic. It owns sealed action/task state and exposes task-state methods/DTOs only.
- Put real-byte transport and file writing in `getter-operations`, because it is the runtime host/product-operation layer.
- Use `<data-dir>/downloads/<task-id>/<safe-file-name>` as the MVP local artifact path, with a sibling temporary `.part` write and rename on success.
- Task state remains current-runtime-only. The downloaded file may remain on disk as a side effect, but it is not a persisted task registry or recovery source.
- Task snapshots may expose downloaded-file metadata (`file_name`, `local_path`, `size_bytes`, `sha256`) as current task DTO output, not as durable task state.
- Progress uses the existing task progress DTO and bit units; byte counts from transport are converted to bits while downloaded-file metadata keeps byte count.

## TDD/BDD seams

Confirmed seams from the accepted scope:

1. Rust operation seam: `getter-operations` submits an issued action, downloads bytes with an injected transport, writes the local file, records task progress/output, and emits notifications without live network in tests.
2. Rust bridge seam: `api_proxy` routes product `task_submit` to the real-byte executor using `data_dir`; focused tests use a local mock HTTP server and assert product payload stays action-id-only.
3. Flutter/native seam: existing typed `submitRuntimeAction(action_id)` and task DTO rendering remain narrow. Add DTO/rendering tests only if new downloaded-file/progress metadata needs UI parsing/display coverage.
4. Documentation seam: ADR-0011 / app docs / this plan must record that real-byte executor now exists while persistence/background/installer remain deferred.

## Scout result

Async scout `ff0ca057-c2a4-4b65-b933-8beeb8321d35` failed with a connection error. Its output artifact was empty. I used local targeted inspection as fallback.

Local inspection found:

- `getter-core/src/runtime.rs` already owns in-memory `GetterRuntime`, `TaskSnapshot`, progress, controls, and `task_changed` notifications.
- `getter-operations/src/runtime.rs` currently maps JSON operations to pure runtime task controls (`task_start`, manual progress, manual complete) but does not write real bytes.
- `api_proxy` already injects `data_dir` into runtime operations from Kotlin and exposes runtime notifications through an `EventChannel`.
- Dart/Kotlin already have narrow `submitRuntimeAction(action_id)`, task listing, and notification DTO plumbing.

## Implementation checklist

- [x] Create work branches `work/download-bytes-mvp` in superproject and getter submodule.
- [x] Record scout failure and local-inspection fallback.
- [x] Add Rust red test for injected transport writing bytes and task notifications.
- [x] Implement getter-core task output/state methods needed by the executor.
- [x] Implement getter-operations download transport abstraction, default ureq transport, file writer, and submit+download JSON operation.
- [x] Wire `api_proxy` product `task_submit` to submit+download with `data_dir` and action-id-only product payload coverage.
- [x] Update Dart/UI DTO/rendering for downloaded-file metadata output.
- [x] Update ADR/docs/context to reflect the real-byte executor and non-goals.
- [x] Focused validation.
- [x] Full validation.
- [x] Blocker review.
- [x] Commit getter unsigned (`d02b86fe5311b1c2762c31f326155bece179f5f7`, `%G? = N`), then superproject unsigned with gitlink/docs/PLAN.
- [ ] Merge/push rewrite branches, wait for PR #514 checks, delete work branches, confirm clean trees.

## Validation plan

Focused:

- `cargo test -p getter-core runtime_exposes_download_plan_and_downloaded_file_snapshot -- --nocapture` — passed.
- `cargo test -p getter-operations download -- --nocapture` — passed, including retrying a failed download task through the same sealed plan.
- `cargo test -p getter-operations json_submit_rejects_product_supplied_download_fields -- --nocapture` — passed.
- `cargo test runtime_dispatcher_submit_with_data_dir_downloads_bytes -- --nocapture` in `core-getter/src/main/rust/api_proxy` — passed.
- `cargo test runtime_dispatcher_retry_with_data_dir_downloads_failed_task_again -- --nocapture` in `core-getter/src/main/rust/api_proxy` — passed.
- `flutter test test/native_getter_adapter_test.dart --plain-name 'runtime'` — passed.
- `flutter test test/widget_test.dart --plain-name 'downloads route'` — passed.

Full before delivery:

- From getter submodule: `cargo fmt --check`, `cargo test --workspace --features lua`, `cargo test --workspace --no-default-features`, and `cargo test -p getter-operations --features rustls-platform-verifier default_transport_fetches_bytes_from_mock_http_server -- --nocapture` — passed after the retry routing edit.
- From api_proxy: `cargo fmt --check && cargo test` — passed after the retry routing edit.
- From app: `flutter test` — passed.
- From superproject root: `./gradlew --quiet :app:testDebugUnitTest` — passed.
- Superproject Android/Rust skeleton: `just verify-workspace-skeleton` with the known Android NDK env exports — passed after the retry routing edit.
- `git diff --check` in both repos — passed.

Review:

- `/tmp/upgradeall-download-bytes-mvp-review.md` reported `Blocker: none found`.
