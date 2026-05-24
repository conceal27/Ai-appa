# ============================================
# Android SDK 一键自动安装脚本（Windows国内镜像版）
# ============================================
# 功能：
#   1. 自动下载 commandline-tools (腾讯云镜像)
#   2. 自动解压并配置目录结构
#   3. 自动安装必需的SDK组件
#   4. 自动配置环境变量
# ============================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Android SDK 自动安装工具" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 创建目录
$SDK_PATH = "C:\Android\Sdk"
$CMD_TOOLS_PATH = "$SDK_PATH\cmdline-tools\latest"

Write-Host "[1/6] 创建目录..." -ForegroundColor Yellow
if (-not (Test-Path $SDK_PATH)) {
    New-Item -ItemType Directory -Path $SDK_PATH -Force | Out-Null
}
if (-not (Test-Path $CMD_TOOLS_PATH)) {
    New-Item -ItemType Directory -Path $CMD_TOOLS_PATH -Force | Out-Null
}
Write-Host "  ✓ 目录创建完成" -ForegroundColor Green

# 下载 commandline-tools
Write-Host ""
Write-Host "[2/6] 下载 Android Command-line Tools..." -ForegroundColor Yellow
$DOWNLOAD_URL = "https://mirrors.cloud.tencent.com/AndroidSDK/cmdline-tools/11076708/commandlinetools-win-11076708_latest.zip"
$ZIP_PATH = "$env:TEMP\commandlinetools-win.zip"

Write-Host "  下载地址: $DOWNLOAD_URL"
Write-Host "  保存到: $ZIP_PATH"

try {
    Invoke-WebRequest -Uri $DOWNLOAD_URL -OutFile $ZIP_PATH -UseBasicParsing
    Write-Host "  ✓ 下载完成" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 下载失败，尝试备用地址..." -ForegroundColor Red
    # 备用下载地址
    $BACKUP_URL = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
    try {
        Invoke-WebRequest -Uri $BACKUP_URL -OutFile $ZIP_PATH -UseBasicParsing
        Write-Host "  ✓ 备用地址下载成功" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ 下载失败，请手动下载" -ForegroundColor Red
        Write-Host "  下载地址: https://developer.android.com/studio#command-tools"
        Read-Host "按回车键退出"
        exit 1
    }
}

# 解压文件
Write-Host ""
Write-Host "[3/6] 解压文件..." -ForegroundColor Yellow
Expand-Archive -Path $ZIP_PATH -DestinationPath $env:TEMP\sdk_temp -Force
Get-ChildItem -Path "$env:TEMP\sdk_temp\cmdline-tools" -Recurse | Move-Item -Destination $CMD_TOOLS_PATH -Force
Write-Host "  ✓ 解压完成" -ForegroundColor Green

# 配置环境变量
Write-Host ""
Write-Host "[4/6] 配置环境变量..." -ForegroundColor Yellow
$env:ANDROID_HOME = $SDK_PATH
$env:PATH += ";$SDK_PATH\platform-tools;$CMD_TOOLS_PATH\bin"

# 永久设置环境变量（用户级）
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SDK_PATH, "User")
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$SDK_PATH*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$SDK_PATH\platform-tools;$CMD_TOOLS_PATH\bin", "User")
}
Write-Host "  ✓ 环境变量配置完成" -ForegroundColor Green

# 接受协议并安装组件
Write-Host ""
Write-Host "[5/6] 安装SDK组件 (android-34)..." -ForegroundColor Yellow
Set-Location $CMD_TOOLS_PATH\bin

# 先接受所有协议
Write-Host "  接受所有许可协议..."
echo "y" | .\sdkmanager.bat --licenses 2>$null | Out-Null

# 安装必需组件
Write-Host "  安装 platforms;android-34..."
.\sdkmanager.bat "platforms;android-34" 2>&1 | Out-Null

Write-Host "  安装 build-tools;34.0.0..."
.\sdkmanager.bat "build-tools;34.0.0" 2>&1 | Out-Null

Write-Host "  安装 platform-tools..."
.\sdkmanager.bat "platform-tools" 2>&1 | Out-Null

Write-Host "  ✓ SDK组件安装完成" -ForegroundColor Green

# 清理临时文件
Write-Host ""
Write-Host "[6/6] 清理临时文件..." -ForegroundColor Yellow
Remove-Item -Path $ZIP_PATH -Force
Remove-Item -Path "$env:TEMP\sdk_temp" -Recurse -Force
Write-Host "  ✓ 清理完成" -ForegroundColor Green

# 完成
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  ✓ Android SDK 安装完成！" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "SDK 路径: $SDK_PATH" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Yellow
Write-Host "  1. 重启 Android Studio"
Write-Host "  2. 在 SDK Manager 中确认 SDK Location: $SDK_PATH"
Write-Host "  3. 打开项目等待 Gradle 同步"
Write-Host ""
Write-Host "按回车键退出..."
Read-Host
