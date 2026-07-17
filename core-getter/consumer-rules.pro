# Flutter/Kotlin calls these JNI entrypoints by their native method names.
-keep class net.xzos.upgradeall.getter.NativeLib { *; }

# Rust JNI loads this provider reflectively through the app classloader.
-keep class net.xzos.upgradeall.getter.platform.** { *; }
