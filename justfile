set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

GETTER_MANIFEST := "core-getter/src/main/rust/getter/Cargo.toml"
API_PROXY_MANIFEST := "core-getter/src/main/rust/api_proxy/Cargo.toml"

verify:
    just test-getter-unit
    just test-getter-bdd
    just test-flutter-widget
    just verify-workspace-skeleton
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

build-flutter-android-debug:
    cd app_flutter && flutter build apk --debug

verify-workspace-skeleton:
    test "$(git ls-files -s core-getter/src/main/rust/getter | awk '{print $1}')" = "160000"
    cargo metadata --manifest-path {{ GETTER_MANIFEST }} --no-deps --format-version 1 >/tmp/upgradeall-getter-metadata.json
    cargo metadata --manifest-path {{ API_PROXY_MANIFEST }} --no-deps --format-version 1 >/tmp/upgradeall-api-proxy-metadata.json
    cargo fmt --manifest-path {{ GETTER_MANIFEST }} --all --check
    cargo check --manifest-path {{ GETTER_MANIFEST }} --workspace --all-targets
    cargo check --manifest-path {{ API_PROXY_MANIFEST }}
    cd app_flutter && flutter analyze
    ./gradlew --no-daemon projects
