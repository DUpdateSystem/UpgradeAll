#!/bin/bash
# 一行命令快速测试 - 适合复制粘贴使用
# 用法: ./scripts/test-oneline.sh

# 切换到项目根目录
cd "$(dirname "$0")/.."

# 运行测试
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/*.apk && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.xzos.upgradeall.SimpleGetterTest