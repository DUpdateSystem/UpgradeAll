# PLAN: F-Droid catalog cache refresh bootstrap

> Status: completed and reviewed
> Branch: `work/fdroid-cache-refresh-bootstrap`
> Scope: one small functional slice that lets the Flutter/native product path refresh/bootstrap the default F-Droid provider catalog cache before installed F-Droid autogen preview/apply.

## Boundary

Rust getter/native bridge owns provider cache refresh, F-Droid catalog parsing, provider diagnostics, and cache writes. Flutter renders getter-owned DTOs/status and only triggers a narrow default refresh operation.

The product refresh method must not expose `index_xml`, endpoint URLs, cache-mode controls, raw provider payloads, package-path derivation, provider parsing, or generated-content decisions to Dart/Kotlin.

Because true live network transport is still not accepted for this slice, the bootstrap source is a getter/api-proxy-owned bundled default F-Droid catalog fixture. If implementing real live HTTP becomes necessary, stop for architecture/ADR collaboration instead of smuggling endpoint/network controls into Flutter.

## Implementation path

- [x] Add a typed native/api_proxy operation that refreshes the default F-Droid catalog cache from a getter-owned bundled default catalog source and returns getter-owned provider/cache diagnostics.
- [x] Add Kotlin `NativeLib`, `MainActivity`, and request-builder plumbing with only `data_dir` in the product request.
- [x] Add Dart `GetterAdapter` / `MethodChannelGetterAdapter` product method and a CLI adapter unsupported boundary.
- [x] Add an Installed Autogen UI action to refresh/bootstrap F-Droid catalog cache and display the getter-owned refresh result/diagnostics before preview.
- [x] Add focused Rust, Kotlin, Dart adapter, and widget tests for the narrow request and UI behavior.
- [x] Update docs for the sixth bridge surface / cache bootstrap boundary.
- [x] Validate and review the functional branch.
- [ ] Commit unsigned, merge back to `rewrite/flutter-getter-spine`, push, and delete this branch.

## Validation and review

- `git diff --check`
- `cd core-getter/src/main/rust/api_proxy && cargo fmt --check`
- `cd core-getter/src/main/rust/api_proxy && cargo test default_fdroid_catalog_refresh`
- `cd core-getter/src/main/rust/api_proxy && cargo test installed_fdroid_preview`
- `cd core-getter/src/main/rust/api_proxy && cargo test`
- `cd app_flutter && dart format --output=none --set-exit-if-changed lib/cli_getter_adapter.dart lib/getter_adapter.dart lib/main.dart lib/native_getter_adapter.dart test/native_getter_adapter_test.dart test/widget_test.dart`
- `cd app_flutter && flutter analyze`
- `cd app_flutter && flutter test`
- `cd app_flutter/android && ./gradlew :app:testDebugUnitTest --tests 'net.xzos.upgradeall.GetterBridgeRequestBuilderTest.fdroidCatalogRefreshRequestCarriesOnlyDataDir'`
- `just verify-workspace-skeleton` with the local Android SDK/NDK environment exported.
- Reviewer run `e2505263` approved the branch for commit/merge with no blockers.

## Stop conditions

Stop for human collaboration if the slice requires live HTTP transport, endpoint configuration, provider fixture input in product Flutter/native requests, cache-mode controls in product requests, custom F-Droid endpoint product semantics, or another ADR-level decision not already covered by ADR-0002/0007/0012.
