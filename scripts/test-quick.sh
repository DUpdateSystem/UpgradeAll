#!/bin/bash

# UpgradeAll 快速测试脚本
# 用法: ./scripts/test-quick.sh [--headless]
# 
# 选项:
#   --headless  无界面模式运行（适用于 CI/CD）
#
# 示例:
#   ./scripts/test-quick.sh           # 本地开发（有界面）
#   ./scripts/test-quick.sh --headless # CI/CD 环境（无界面）

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 解析参数
HEADLESS=false
if [[ "$1" == "--headless" ]]; then
    HEADLESS=true
fi

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}   UpgradeAll Quick Test Runner${NC}"
echo -e "${GREEN}=========================================${NC}"

# 设置环境变量
export ANDROID_HOME=${ANDROID_HOME:-$HOME/.local/share/Google/Android/Sdk}
export ANDROID_AVD_HOME=${ANDROID_AVD_HOME:-$HOME/.config/.android/avd}
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

# 切换到项目根目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
cd "$PROJECT_ROOT"

# 检查 Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
    echo -e "${RED}❌ Error: Android SDK not found at $ANDROID_HOME${NC}"
    echo "Please set ANDROID_HOME environment variable"
    exit 1
fi

echo -e "${YELLOW}📱 Android SDK: $ANDROID_HOME${NC}"

# 检查是否有运行中的模拟器
RUNNING_DEVICE=$(adb devices | grep -E "emulator-[0-9]+" | head -1 | cut -f1 || true)

if [ -z "$RUNNING_DEVICE" ]; then
    echo -e "${YELLOW}🚀 Starting emulator...${NC}"
    
    # 获取第一个可用的 AVD
    AVD_NAME=$($ANDROID_HOME/cmdline-tools/latest/bin/avdmanager list avd -c | head -1)
    
    if [ -z "$AVD_NAME" ]; then
        echo -e "${RED}❌ No AVD found. Please create one first.${NC}"
        echo "Run: avdmanager create avd -n test_avd -k 'system-images;android-33;google_apis;x86_64'"
        exit 1
    fi
    
    echo -e "${YELLOW}📱 Using AVD: $AVD_NAME${NC}"
    
    # 启动模拟器
    if [ "$HEADLESS" = true ]; then
        $ANDROID_HOME/emulator/emulator -avd "$AVD_NAME" \
            -no-window -no-audio -no-boot-anim \
            -gpu swiftshader_indirect &
    else
        $ANDROID_HOME/emulator/emulator -avd "$AVD_NAME" \
            -gpu host &
    fi
    
    EMULATOR_PID=$!
    
    # 等待模拟器启动
    echo -e "${YELLOW}⏳ Waiting for emulator to boot...${NC}"
    adb wait-for-device
    
    # 等待系统完全启动
    while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
        sleep 2
        echo -n "."
    done
    echo ""
    
    # 解锁屏幕
    adb shell input keyevent 82
    sleep 1
    
    echo -e "${GREEN}✅ Emulator is ready!${NC}"
else
    echo -e "${GREEN}✅ Using existing emulator: $RUNNING_DEVICE${NC}"
fi

# 构建和测试
echo -e "${YELLOW}🔨 Building and testing...${NC}"

# 构建 APK
echo -e "${YELLOW}📦 Building Debug APK...${NC}"
./gradlew assembleDebug

# 安装 APK
echo -e "${YELLOW}📲 Installing APK...${NC}"
APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
adb install -r "$APK_PATH"

# 运行简单测试
echo -e "${YELLOW}🧪 Running smoke test...${NC}"
./gradlew connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=net.xzos.upgradeall.SimpleGetterTest \
    --quiet

# 检查测试结果
TEST_RESULT=$?

# 生成报告路径
REPORT_PATH="app/build/reports/androidTests/connected/index.html"

echo -e "${GREEN}=========================================${NC}"
if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
else
    echo -e "${RED}❌ TESTS FAILED!${NC}"
fi
echo -e "${GREEN}=========================================${NC}"

# 显示报告位置
if [ -f "$REPORT_PATH" ]; then
    echo -e "${YELLOW}📊 Test report: file://$(pwd)/$REPORT_PATH${NC}"
fi

# 清理（如果启动了新模拟器）
if [ -n "$EMULATOR_PID" ]; then
    echo -e "${YELLOW}🧹 Cleaning up...${NC}"
    adb emu kill 2>/dev/null || true
    kill $EMULATOR_PID 2>/dev/null || true
fi

exit $TEST_RESULT