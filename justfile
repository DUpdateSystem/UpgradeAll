set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

GETTER_MANIFEST := "core-getter/src/main/rust/getter/Cargo.toml"
API_PROXY_MANIFEST := "core-getter/src/main/rust/api_proxy/Cargo.toml"
PLATFORM_ADAPTER_MANIFEST := "core-getter/src/main/rust/platform_adapter/Cargo.toml"

verify:
    just test-getter-unit
    just test-getter-bdd
    just test-flutter-widget
    just verify-workspace-skeleton
    just test-android-platform-adapter
    just test-flutter-android-platform-adapter
    just test-flutter-getter-cli-integration
    just build-flutter-android-debug

verify-fast:
    just test-getter-unit
    just test-getter-bdd
    just test-flutter-widget

test-getter-unit:
    cargo test --manifest-path {{ GETTER_MANIFEST }} --workspace --lib --bins

test-getter-bdd:
    cargo test --manifest-path {{ GETTER_MANIFEST }} -p getter-cli --test bdd_cli

test-flutter-widget:
    cd app_flutter && flutter test

test-flutter-getter-cli-integration:
    cargo build --manifest-path {{ GETTER_MANIFEST }} -p getter-cli --bin getter-cli
    cd app_flutter && GETTER_CLI_BIN="../core-getter/src/main/rust/getter/target/debug/getter-cli" flutter test dev_test/cli_getter_adapter_test.dart

test-android-platform-adapter:
    ./gradlew --no-daemon ':core-getter:buildDebugApi_proxyRust[arm64-v8a]' ':core-getter:buildDebugApi_proxyRust[armeabi-v7a]' ':core-getter:buildDebugApi_proxyRust[x86_64]' :core-getter:testDebugUnitTest --tests 'net.xzos.upgradeall.getter.platform.InstalledInventoryCollectorTest' :core-getter:assembleDebug

test-flutter-android-platform-adapter:
    cd app_flutter/android && ./gradlew --no-daemon :app:testDebugUnitTest --tests 'net.xzos.upgradeall.LegacyRoomImportPreparerTest'

test-flutter-device-bridge device="emulator-5554":
    cd app_flutter && flutter test integration_test/native_bridge_test.dart -d {{ device }}

test-flutter-device-install-acceptance device="emulator-5554" expected="succeeded":
    cd app_flutter && flutter test integration_test/native_bridge_test.dart -d {{ device }} --plain-name 'typed PackageInstaller installs Getter handoff on Android' --dart-define=RUN_ANDROID_INSTALL_ACCEPTANCE=true --dart-define=INSTALL_EXPECTED_STATUS={{ expected }}

build-flutter-android-debug:
    cd app_flutter && flutter build apk --debug
    python3 tools/verify_flutter_apk_bridge.py app_flutter/build/app/outputs/flutter-apk/app-debug.apk

verify-workspace-skeleton:
    test "$(git ls-files -s core-getter/src/main/rust/getter | awk '{print $1}')" = "160000"
    cargo metadata --manifest-path {{ GETTER_MANIFEST }} --no-deps --format-version 1 >/tmp/upgradeall-getter-metadata.json
    cargo metadata --manifest-path {{ API_PROXY_MANIFEST }} --no-deps --format-version 1 >/tmp/upgradeall-api-proxy-metadata.json
    cargo metadata --manifest-path {{ PLATFORM_ADAPTER_MANIFEST }} --no-deps --format-version 1 >/tmp/upgradeall-platform-adapter-metadata.json
    cargo fmt --manifest-path {{ GETTER_MANIFEST }} --all --check
    cargo fmt --manifest-path {{ PLATFORM_ADAPTER_MANIFEST }} --all --check
    cargo check --manifest-path {{ GETTER_MANIFEST }} --workspace --all-targets
    cargo check --manifest-path {{ API_PROXY_MANIFEST }}
    if [ -n "${ANDROID_NDK_HOME:-}" ]; then export CC_aarch64_linux_android="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android23-clang"; export AR_aarch64_linux_android="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"; fi; cargo check --manifest-path {{ API_PROXY_MANIFEST }} --target aarch64-linux-android
    cargo test --manifest-path {{ PLATFORM_ADAPTER_MANIFEST }}
    cargo check --manifest-path {{ PLATFORM_ADAPTER_MANIFEST }} --target aarch64-linux-android
    cd app_flutter && flutter analyze
    ./gradlew --no-daemon projects
