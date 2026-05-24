# 📦 Android SDK 一键安装说明

## 使用方法

### 方法1：使用自动脚本（推荐）

1. **右键** `install_android_sdk.ps1`
2. 选择 **"使用 PowerShell 运行"**
3. 等待脚本自动完成（约5-10分钟）
4. **重启 Android Studio**

---

### 方法2：手动执行脚本（如果右键不行）

1. 打开 **PowerShell**（管理员权限）
2. 进入项目目录：
   ```powershell
   cd 你的项目路径\AI_Companion_App
   ```
3. 执行脚本：
   ```powershell
   .\install_android_sdk.ps1
   ```

---

### 如果遇到"无法加载文件"的错误

执行前先运行：
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

然后再执行脚本。

---

## 脚本完成后的操作

1. **重启 Android Studio**
2. 欢迎界面 → **More Actions** → **SDK Manager**
3. 确认 **Android SDK Location** 是：`C:\Android\Sdk`
4. 点击 **Apply** → **OK**
5. 打开项目，等待 Gradle 同步

---

## 构建APK

同步完成后：
1. 菜单栏 → **Build** → **Generate Signed Bundle / APK**
2. 选择 **APK** → **Next**
3. 选择 **release** → **Finish**
4. APK 输出路径：`app\release\AI_Companion.apk`

---

## 常见问题

### Q: 脚本下载失败怎么办？
A: 手动下载 commandline-tools：
- 下载地址：https://mirrors.cloud.tencent.com/AndroidSDK/
- 找到 `commandlinetools-win-xxx_latest.zip` 下载
- 解压到 `C:\Android\Sdk\cmdline-tools\latest\`

### Q: Gradle 还是报错？
A: 配置国内镜像源：
在项目的 `gradle.properties` 中添加：
```properties
systemProp.http.proxyHost=mirrors.cloud.tencent.com
systemProp.http.proxyPort=80
systemProp.https.proxyHost=mirrors.cloud.tencent.com
systemProp.https.proxyPort=80
```
