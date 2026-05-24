# AI 情感陪伴 App

一个基于 Android Jetpack Compose 的 AI 情感陪伴应用，集成 DeepSeek、火山引擎、豆包等多平台 AI 能力。

## 项目架构

采用 **MVVM + Clean Architecture** 架构设计，分层清晰，易于维护和扩展：

```
app/src/main/java/com/ai/companion/
├── data/
│   ├── local/          # 本地数据层
│   │   ├── dao/        # Room 数据库 DAO
│   │   ├── AppDatabase.kt
│   │   └── AppPreferences.kt
│   ├── remote/         # 远程 API 层
│   │   └── api/        # API 接口定义
│   └── repository/     # 数据仓库实现
├── domain/             # 领域层
│   ├── model/          # 数据模型
│   ├── repository/     # 仓库接口
│   └── usecase/        # 业务用例
├── presentation/       # 表现层
│   ├── base/           # 基础组件（主题等）
│   ├── chat/           # 聊天页面
│   ├── settings/       # 设置页面
│   ├── memory/         # 记忆页面
│   ├── MainActivity.kt
│   └── Navigation.kt
└── di/                 # 依赖注入模块
```

## 技术栈

| 技术 | 用途 | 版本 |
|------|------|------|
| Kotlin | 开发语言 | 1.9.20 |
| Jetpack Compose | UI 框架 | BOM 2024.02.00 |
| Hilt | 依赖注入 | 2.48 |
| Room | 本地数据库 | 2.6.1 |
| DataStore | 配置存储 | 1.0.0 |
| Retrofit | 网络请求 | 2.9.0 |
| OkHttp | 网络客户端 | 4.12.0 |
| Navigation | 页面导航 | 2.7.6 |
| Coil | 图片加载 | 2.5.0 |

## 配置要求

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34
- **Java Version**: 17

## 功能特性

### 1. 聊天界面
- 极简风格设计，参考微信聊天界面
- 白底 + 绿色点缀的清新配色
- 实时消息气泡展示
- AI 打字动画效果
- 自动滚动到最新消息

### 2. 人设系统
- 可配置的角色名称
- 自定义性格描述
- 丰富的背景故事设定
- 个性化语气风格
- 头像配置支持

### 3. API 配置系统
**所有 API Key 均由用户在 APP 内配置，本地存储，无硬编码：**

| API 平台 | 功能 | 配置项 |
|----------|------|--------|
| DeepSeek V4 | 聊天对话 | API Key |
| 火山引擎 | ASR 语音识别、TTS 语音合成、图片识别 | APP ID、Access Key ID、Secret Access Key |
| 豆包 | 图像生成 | API Key |

每个 API 均支持：
- 输入框配置密钥
- 一键测试连接
- 测试结果实时反馈

### 4. 记忆管理
- Room 数据库持久化存储
- 按会话分类管理
- 支持继续历史对话
- 单会话删除
- 一键清空所有记忆

## 快速开始

### 1. 环境准备
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 2. 导入项目
1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `AI_Companion_App` 目录

### 3. 构建项目
- 等待 Gradle 同步完成
- 点击 Build -> Make Project

### 4. 配置 API
运行 APP 后，进入「设置」页面配置：
1. DeepSeek API Key（必需，用于聊天）
2. 其他 API Key 根据需要配置

## API Key 获取方式

### DeepSeek V4
- 访问: https://platform.deepseek.com/
- 注册账号后在控制台获取 API Key

### 火山引擎
- 访问: https://www.volcengine.com/
- 开通「智能语音服务」
- 在控制台获取 APP ID、Access Key、Secret Key

### 豆包图像生成
- 访问: https://console.volcengine.com/ark/
- 创建豆包模型应用
- 获取 API Key

## 代码规范

### 架构原则
- **单向依赖**: Presentation → Domain → Data
- **接口隔离**: 领域层定义接口，数据层实现
- **ViewModel 隔离**: 不持有 Context 引用

### 命名规范
- 类名: PascalCase (ChatViewModel)
- 函数名: camelCase (sendMessage)
- 变量名: camelCase (messageText)
- 常量: UPPER_SNAKE_CASE (BASE_URL)

### 代码风格
- 采用官方 Kotlin 代码风格
- 使用 Compose 最佳实践
- 所有字符串资源化
- 所有颜色资源化

## 扩展开发

### 添加新的 API 支持
1. 在 `data/remote/api` 下创建新的 API 接口
2. 在 `AppModule.kt` 中提供 Retrofit 实例
3. 在 `ApiConfig` 中添加配置字段
4. 在设置页面添加输入表单

### 添加新的功能页面
1. 在 `presentation` 下创建页面目录
2. 实现 ViewModel 和 Screen
3. 在 `Navigation.kt` 中添加路由

## 注意事项

1. **API 密钥安全**: 所有密钥本地存储在 DataStore 中，不会上传
2. **网络权限**: 应用需要 INTERNET 权限才能调用 API
3. **数据清除**: 清除应用数据会同时清除所有 API 配置和聊天记录
4. **API 配额**: 各平台 API 有调用次数限制，注意控制使用

## License

MIT License
