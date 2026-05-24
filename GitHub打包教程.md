# 🚀 GitHub Actions 云端自动打包APK教程

**零本地环境，完全云端构建！**

---

## 📋 准备工作

1. 一个 GitHub 账号（免费注册：https://github.com）
2. 我们的项目代码包

---

## 🔧 步骤1：创建GitHub仓库

1. 登录 GitHub
2. 点击右上角 **+** → **New repository**
3. 仓库名：`AI-Companion-App`（随便起）
4. 选择 **Public** 或 **Private** 都可以
5. 不要勾选任何初始化选项
6. 点击 **Create repository**

---

## 📤 步骤2：上传代码

### 方法A：网页直接上传（最简单）

1. 创建仓库后，点击 **uploading an existing file**
2. 把我们项目里的 **所有文件和文件夹** 拖进去
3. 确保包含：
   - `app/` 文件夹
   - `gradle/` 文件夹
   - `build.gradle`
   - `settings.gradle`
   - `gradlew` / `gradlew.bat`
   - `.github/workflows/build.yml` （最重要！）
4. 点击 **Commit changes**

### 方法B：用Git命令行

```bash
# 进入项目文件夹
cd AI_Companion_App

# 初始化Git
git init
git add .
git commit -m "Initial commit"

# 关联GitHub仓库（替换成你的仓库地址）
git remote add origin https://github.com/你的用户名/AI-Companion-App.git

# 推送代码
git branch -M main
git push -u origin main
```

---

## 🏗️ 步骤3：触发自动构建

### 方法1：手动触发（推荐）

1. 进入你的GitHub仓库
2. 点击顶部的 **Actions** 标签
3. 左侧选择 **Build APK**
4. 点击 **Run workflow** → 再点击 **Run workflow**（绿色按钮）
5. 等待构建完成（约3-5分钟）

### 方法2：自动触发

每次你推送代码到 `main` 或 `master` 分支，都会自动触发构建。

---

## 📥 步骤4：下载APK

1. 构建完成后（显示绿色 ✅）
2. 点击进入那个构建任务
3. 页面底部 **Artifacts** 区域
4. 点击 **AI-Companion-APK** 下载
5. 解压下载的zip，得到APK安装包！

---

## 💡 常见问题

### Q: 构建失败怎么办？
A: 点击失败的任务，查看日志（Log），常见问题：
- 缺少某个文件 → 确保所有代码都上传了
- 签名问题 → 我们用的是debug签名，应该没问题

### Q: 构建太慢？
A: GitHub Actions免费配额足够个人使用，一般3-5分钟完成。

### Q: 可以打包Release签名版吗？
A: 可以，需要在GitHub Secrets中配置签名密钥，教程：
https://developer.android.com/studio/publish/app-signing

---

## 🎯 完整流程总结

```
创建GitHub仓库 → 上传代码 → Actions手动触发 → 等待3分钟 → 下载APK
```

**全程无需任何本地开发环境！** 🚀
