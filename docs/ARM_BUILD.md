# Building UpgradeAll on ARM (Termux / Linux ARM64)

This guide documents how to build UpgradeAll on ARM64 Android devices using Termux, and on ARM64 Linux hosts.

## The Core Problem

Google's Android SDK **only ships x86-64 Linux binaries** for build tools:
- `aapt2` — requires `/lib64/ld-linux-x86-64.so.2` (fails on ARM with `cannot execute: required file not found`)
- NDK, CMake, emulator, skia parser — all x86-64 only

**proot cannot fix this** — proot translates syscalls/paths, **not CPU instructions**.

## The Solution: Termux's Native ARM64 `aapt2`

Termux provides a **native ARM64 `aapt2`** built for Android's bionic libc:
- Package: `aapt2` (API 36, version `2.20-android-16.0.0_r4`)
- Installs to: `/data/data/com.termux/files/usr/bin/aapt2`
- Works because **Gradle runs on Termux's bionic JVM** — same ABI, same loader

All other build tools are Java-based and work natively:
- `d8` / `r8` — Java
- `zipalign` — Java
- `apksigner` — Java
- Kotlin compiler — JVM bytecode
- Rust toolchain — Termux provides `rustc`/`cargo` for `aarch64-linux-android`

**`aapt2` is the ONLY native binary in the entire chain.**

---

## Prerequisites (Termux on Android ARM64)

```bash
# Install Termux (F-Droid or GitHub release, NOT Play Store)
# Then in Termux:

pkg update && pkg upgrade
pkg install git openjdk-21 gradle kotlin rust cargo android-tools aapt2

# Verify versions
java -version          # openjdk 21
kotlinc -version       # kotlin 2.x
gradle -version        # gradle 9.x
rustc --version        # rust 1.80+
aapt2 version          # 2.20-android-16.0.0_r4
```

---

## Gradle Configuration

Create or update `local.properties`:

```properties
# Termux Android SDK path (if using android-tools pkg)
sdk.dir=/data/data/com.termux/files/home/Android/sdk

# CRITICAL: Override aapt2 to use Termux's ARM64 binary
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Or pass via command line:
```bash
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

---

## Build Commands

```bash
# Clone with submodules (getter Rust crate)
git clone --recurse-submodules https://github.com/DUpdateSystem/UpgradeAll.git
cd UpgradeAll

# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

---

## Signal-36 Hardening (Critical for Reliability)

Android's `traced_perf` profiler sends **Signal 36 (SIGRTMIN+2)** to short-lived processes, killing them at ~15% rate. This affects:
- `git` operations
- Gradle daemon spawns
- Rust compilation (`rustc`, `cargo`)
- Any short-lived child process

**Fix: Add `trap '' 36` to shell scripts and CI.**

### In Shell Scripts
```bash
#!/bin/bash
trap '' 36  # Must be FIRST line after shebang
# SIG_IGN is inherited across fork+exec and CANNOT be reset by children
./gradlew assembleDebug
```

### In GitHub Actions (`.github/workflows/android.yml`)
```yaml
- name: Build with Gradle (debug)
  run: |
    trap '' 36
    ./gradlew -PappVerName=${{ env.VERSION }} assembleDebug
  env:
    ANDROID_NDK_HOME: ${{ steps.setup-ndk.outputs.ndk-path }}
```

**Lab verification**: 300 trials, 0 kills with trap vs 10-15% without.

---

## Rust Submodule (getter)

The `getter` crate is a Git submodule at `core-getter/src/main/rust/getter/`.

```bash
# Initialize submodule
git submodule update --init --recursive

# Build Rust components (handled automatically by Gradle via androidRust plugin)
# But you can also build manually:
cd core-getter/src/main/rust/getter
cargo build --target aarch64-linux-android --release
```

### Rust Targets Required
```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
```

---

## Known Limitations

| Issue | Workaround |
|-------|------------|
| No Android SDK manager in Termux | Use `android-tools` pkg or download cmdline-tools manually |
| No emulator on ARM | Test on real device via ADB |
| `ndk-build` not in Termux | Use CMake via Gradle (NDK bundled) |
| Firebase/Play services need x86_64 libs | Build `free` flavor: `./gradlew assembleDebug -Pfree` |

---

## Building the `free` Flavor (No Firebase/Play)

```bash
./gradlew assembleDebug -Pfree
./gradlew assembleRelease -Pfree
```

Removes: Firebase Crashlytics, Analytics, Performance, Google Play licensing.

---

## Verification Checklist

After build, verify APK:

```bash
# Check architecture
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | grep native-code

# Should show: native-code: 'arm64-v8a' (and optionally armeabi-v7a, x86, x86_64)

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Why This Works (Technical Details)

| Component | Architecture | Why It Works |
|-----------|--------------|--------------|
| Gradle | JVM (bionic) | Termux OpenJDK 21 runs natively on ARM64 Android |
| Kotlin | JVM bytecode | Compiles to `.class`, runs on same JVM |
| aapt2 | **ARM64 ELF (bionic)** | Termux builds for Android's linker (`/system/bin/linker64`) |
| d8/r8/zipalign/apksigner | Java | Pure JVM, no native code |
| Rust (getter) | ARM64 ELF (bionic) | `cargo build --target aarch64-linux-android` |
| NDK tools | ARM64 ELF | Termux NDK or Google NDK (both have ARM64) |

**Key insight**: Gradle + JVM + Termux `aapt2` = **entire build chain runs on bionic ARM64**. No x86_64 emulation, no proot syscall translation for the build itself.

---

## References

- Lab discovery: [PURESHELL-ANDROID.md](../wiki/PURESHELL-ANDROID.md) — ARM Android builds, `aapt2` barrier, JNI without NDK
- Signal-36 root cause: [SIGNAL-36.md](../wiki/SIGNAL-36.md) — `traced_perf` profiler, `trap '' 36` fix
- Termux `aapt2` package: https://packages.termux.dev/apt/termux-main/aarch64/aapt2/

---

## Contributing ARM Fixes

If you encounter ARM-specific build issues:
1. Check if it's an `aapt2` path issue (override not picked up)
2. Verify Rust targets installed (`rustup target list --installed`)
3. Add `trap '' 36` to any shell wrapper scripts
4. Report with: device arch, Termux version, Gradle output, `aapt2 version`

---

*Documented from live Termux-on-ARM64 build environment (Samsung S24 FE, Exynos 2400, Android 16, kernel 6.1).*