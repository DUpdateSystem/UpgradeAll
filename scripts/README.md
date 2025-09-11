# UpgradeAll 测试脚本

本目录包含用于快速测试 UpgradeAll 应用的脚本。

## 脚本列表

### test-oneline.sh
最简单的测试脚本，一行命令完成构建、安装和测试。

**前提条件**：
- Android 模拟器或设备已连接
- Android SDK 环境已配置

**使用方法**：
```bash
./scripts/test-oneline.sh
```

### test-quick.sh
智能测试脚本，自动处理模拟器启动和资源清理。

**特性**：
- 🚀 自动检测并启动模拟器
- 📱 智能使用已有模拟器或启动新的
- 🧪 运行冒烟测试验证应用稳定性
- 📊 生成并显示测试报告位置
- 🧹 自动清理测试资源

**使用方法**：
```bash
# 本地开发（有界面模拟器）
./scripts/test-quick.sh

# CI/CD 环境（无界面模式）
./scripts/test-quick.sh --headless
```

## 测试内容

这些脚本运行 `SimpleGetterTest`，它会：
1. 启动应用主界面
2. 等待 5 秒确保应用稳定运行
3. 验证应用没有崩溃
4. 确认 Rust Getter 核心正常工作

## 环境要求

- Android SDK（设置 `ANDROID_HOME` 环境变量）
- Android 构建工具
- ADB（Android Debug Bridge）
- 至少一个 Android AVD（虚拟设备）或连接的物理设备

## 故障排除

### 找不到 AVD
如果没有可用的 AVD，创建一个：
```bash
avdmanager create avd -n test_avd -k "system-images;android-33;google_apis;x86_64"
```

### 模拟器启动失败
检查 KVM 支持（Linux）：
```bash
egrep -c '(vmx|svm)' /proc/cpuinfo
```

### 测试失败
查看详细日志：
```bash
adb logcat -d | grep -E "GetterPort|RPC|Exception"
```

## CI/CD 集成

这些测试已集成到 GitHub Actions 工作流中（`.github/workflows/android.yml`），会在每次推送和 PR 时自动运行。