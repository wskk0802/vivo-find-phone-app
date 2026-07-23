#!/bin/bash
# 小米手机一键编译脚本（在 Termux 中运行）
# 使用方法：把此脚本上传到 GitHub 仓库，在 Termux 中下载执行

set -e

echo "=========================================="
echo "  亲友位置查找App - 手机一键编译"
echo "=========================================="
echo ""

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err() { echo -e "${RED}[✗]${NC} $1"; }

# 检查是否为Termux
if [ -z "$PREFIX" ]; then
    err "请在 Termux 中运行此脚本"
    echo "安装方法见 手机编译教程.txt"
    exit 1
fi

log "检测到 Termux 环境"

# Step 1: 更新和安装基础工具
echo ""
echo "--- Step 1/7: 安装基础工具 ---"
pkg update -y 2>/dev/null
pkg install -y openjdk-17 wget unzip git 2>/dev/null
log "JDK + 工具安装完成"
java -version 2>&1 | head -1

# Step 2: 安装 Android SDK
echo ""
echo "--- Step 2/7: 安装 Android SDK ---"
mkdir -p $HOME/android-sdk/cmdline-tools

if [ ! -f $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
    cd /tmp
    warn "下载 Android SDK 命令行工具（约100MB，需几分钟）..."
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O cmdtools.zip
    unzip -q cmdtools.zip -d $HOME/android-sdk/cmdline-tools/
    mkdir -p $HOME/android-sdk/cmdline-tools/latest
    mv $HOME/android-sdk/cmdline-tools/cmdline-tools/* $HOME/android-sdk/cmdline-tools/latest/ 2>/dev/null || true
    rm -f cmdtools.zip
    log "Android SDK 安装完成"
else
    log "Android SDK 已存在，跳过"
fi

# Step 3: 设置环境变量
echo ""
echo "--- Step 3/7: 配置环境变量 ---"
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
export JAVA_HOME=$PREFIX/lib/jvm/openjdk-17
export PATH=$JAVA_HOME/bin:$PATH
log "环境变量配置完成"

# Step 4: 安装 SDK 组件
echo ""
echo "--- Step 4/7: 安装 SDK 组件 ---"
if [ ! -d $ANDROID_HOME/platforms/android-34 ]; then
    warn "接受许可证并下载 SDK 组件（约300MB）..."
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" 2>&1 | tail -3
    log "SDK 组件安装完成"
else
    log "SDK 组件已安装，跳过"
fi

# Step 5: 安装 Gradle
echo ""
echo "--- Step 5/7: 安装 Gradle ---"
if [ ! -d $HOME/gradle-home/gradle-8.5 ]; then
    cd /tmp
    warn "下载 Gradle（约150MB）..."
    wget -q "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -O gradle.zip
    mkdir -p $HOME/gradle-home
    unzip -q gradle.zip -d $HOME/gradle-home/
    rm -f gradle.zip
    log "Gradle 安装完成"
else
    log "Gradle 已安装，跳过"
fi
export PATH=$HOME/gradle-home/gradle-8.5/bin:$PATH
gradle --version 2>&1 | head -2

# Step 6: 获取项目代码
echo ""
echo "--- Step 6/7: 获取项目代码 ---"
cd $HOME
if [ ! -d vivo-find-phone-app ]; then
    warn "克隆 GitHub 仓库..."
    git clone https://github.com/wskk0802/vivo-find-phone-app.git 2>&1 | tail -3
    log "代码下载完成"
else
    log "项目已存在，更新代码..."
    cd vivo-find-phone-app
    git pull 2>&1 | tail -2
    cd ..
fi

# Step 7: 编译 APK
echo ""
echo "--- Step 7/7: 编译 APK ---"
cd $HOME/vivo-find-phone-app

# 创建配置
echo "sdk.dir=$HOME/android-sdk" > local.properties

mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8
org.gradle.daemon=false
android.useAndroidX=true
android.enableJetifier=true
EOF

warn "开始编译（约5-10分钟，手机可能会发热）..."
echo ""

# 使用系统gradle（不用gradlew，避免wrapper问题）
gradle clean assembleDebug --no-daemon --console=plain 2>&1

# 检查输出
if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    echo ""
    log "🎉🎉🎉 APK 编译成功！🎉🎉🎉"
    
    APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "   大小: $APK_SIZE"
    
    # 复制到Download目录
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/亲友位置查找.apk 2>/dev/null || \
    cp app/build/outputs/apk/debug/app-debug.apk $HOME/亲友位置查找.apk
    
    echo ""
    echo "=========================================="
    echo -e "  ${GREEN}APK 已保存到手机 Download 文件夹${NC}"
    echo "  文件名: 亲友位置查找.apk"
    echo "=========================================="
    echo ""
    echo "下一步："
    echo "  1. 打开手机文件管理"
    echo "  2. 进入 Download 文件夹"
    echo "  3. 点击 亲友位置查找.apk 安装"
    echo "  4. 如被拦截：设置→授权管理→开启安装未知应用"
    
    # 尝试发送通知
    termux-notification --title "编译成功！" --content "APK已保存到Download文件夹" 2>/dev/null || true
else
    echo ""
    err "编译失败，未找到 APK 文件"
    echo ""
    echo "调试信息："
    echo "--- 项目结构 ---"
    find . -maxdepth 3 -name "*.gradle" -not -path "*/build/*"
    echo ""
    echo "--- SDK 目录 ---"
    ls $ANDROID_HOME/platforms/ 2>/dev/null || echo "无platforms"
    echo ""
    echo "--- build 目录 ---"
    ls app/build/ 2>/dev/null || echo "无build目录"
    echo ""
    echo "请截图以上信息发给我，我帮你排查"
    exit 1
fi
