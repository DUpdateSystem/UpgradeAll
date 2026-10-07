# Contributing to UpgradeAll

Thank you for your interest in contributing! This project thrives on community contributions.

## Ways to Contribute

### 1. Add New Software Sources (High Impact)
The easiest and most valuable contribution is adding new software sources to [`UpgradeAll-rules`](https://github.com/DUpdateSystem/UpgradeAll-rules):
- **F-Droid repositories** (IzzyOnDroid, Guardian, etc.)
- **GitHub/GitLab/Codeberg releases** 
- **Custom APK sources**
- Just add a JSON file — no code changes needed

### 2. Code Contributions
- **Bug fixes** — Check [open issues](https://github.com/DUpdateSystem/UpgradeAll/issues)
- **Feature requests** — Discuss in [Discussions](https://github.com/DUpdateSystem/UpgradeAll/discussions)
- **Performance improvements** — Profile and optimize

### 3. Documentation
- Improve [ARM build guide](docs/ARM_BUILD.md)
- Translate to your language
- Add examples/screenshots

### 4. Testing
- Test on different Android versions/devices
- Report bugs with device info + logs
- Test beta releases

## Development Setup

### Prerequisites
- Android Studio / Gradle 9.x
- JDK 21
- Android SDK/NDK 29+
- Rust 1.80+ (for getter module)

### Quick Start
```bash
git clone --recurse-submodules https://github.com/DUpdateSystem/UpgradeAll.git
cd UpgradeAll

# Debug build
./gradlew assembleDebug -Pfree

# Run tests
./gradlew test
```

## Pull Request Guidelines

1. **One feature/fix per PR**
2. **Write clear commit messages** — follow [Conventional Commits](https://www.conventionalcommits.org/)
3. **Add tests** for new functionality
4. **Update docs** if behavior changes
5. **Run lint/tests** before submitting:
   ```bash
   ./gradlew lint test
   ```

## Code Style

- **Kotlin**: Follow [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- **XML**: 4-space indent, consistent attribute ordering
- **Rust**: `rustfmt` (run `cargo fmt`)

## Reporting Issues

Use the issue templates:
- **Bug Report** — Device, Android version, steps to reproduce, logs
- **Feature Request** — Use case, alternatives considered
- **Source Request** — App name, source URL, why it's needed

## Sponsor

This project is maintained by volunteers. If you find it useful, please consider sponsoring:

[![GitHub Sponsors](https://img.shields.io/github/sponsors/DUpdateSystem?style=for-the-badge)](https://github.com/sponsors/DUpdateSystem)
[![Patreon](https://img.shields.io/badge/Patreon-Support-orange?style=for-the-badge&logo=patreon)](https://patreon.com/DUpdateSystem)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support-blue?style=for-the-badge&logo=ko-fi)](https://ko-fi.com/DUpdateSystem)

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Be respectful, inclusive, and constructive.

## License

GPL-3.0 — see [LICENSE](LICENSE) for details.