# AI 陪伴 APP - 项目结构总览

## 📁 完整目录结构

```
AI_Companion_App/
├── 📄 README.md                           # 项目总览
├── 📄 FINAL_DELIVERY.md                  # 最终交付清单
├── 📄 INSTALL_GUIDE.md                  # 安装说明+FAQ
├── 📄 TEST_REPORT.md                     # 测试报告
├── 📄 PHASE_2_SUMMARY.md                # 第二阶段总结
├── 📄 PROJECT_STRUCTURE.md               # 本文件
├── 📄 build.gradle                     # 项目级Gradle
├── 📄 settings.gradle                  # Gradle设置
├── 📄 gradle.properties                # Gradle属性
├── 📄 keystore.properties              # 签名配置
│
├── app/
│   ├── 📄 build.gradle                 # App级Gradle
│   ├── 📄 proguard-rules.pro          # 混淆规则
│   │
│   └── src/main/
│       ├── 📄 AndroidManifest.xml     # 清单文件
│       │
│       ├── java/com/ai/companion/
│       │   ├── 📄 AICompanionApp.kt     # Application类
│       │   │
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── 📄 AppDatabase.kt           # Room数据库
│       │   │   │   ├── 📄 AppPreferences.kt        # DataStore
│       │   │   │   └── dao/
│       │   │   │       ├── 📄 ChatMessageDao.kt     # 消息DAO
│       │   │   │       └── 📄 MemoryDao.kt         # 记忆DAO
│       │   │   │
│       │   │   ├── remote/api/
│       │   │   │   ├── 📄 DeepSeekApi.kt         # DeepSeek API（流式SSE
│       │   │   │   ├── 📄 VolcengineApi.kt      # 火山引擎API
│       │   │   │   └── 📄 DoubaoApi.kt        # 豆包API
│       │   │   │
│       │   │   └── repository/
│       │   │       └── 📄 ChatRepositoryImpl.kt  # 仓库实现
│       │   │
│       │   ├── domain/
│       │   │   ├── model/
│       │   │   │   ├── 📄 ChatMessage.kt      # 消息模型
│       │   │   │   ├── 📄 CharacterConfig.kt  # 人设+API配置
│       │   │   │   └── 📄 Memory.kt         # 记忆模型
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   └── 📄 ChatRepository.kt  # 仓库接口
│       │   │   │
│       │   │   └── usecase/
│       │   │       ├── 📄 ChatUseCases.kt      # 用例
│       │   │       ├── 📄 MemoryService.kt    # 记忆服务
│       │   │       └── 📄 HumanizeService.kt  # 人性化服务
│       │   │
│       │   ├── di/
│       │   │   └── 📄 AppModule.kt            # Hilt依赖注入
│       │   │
│       │   └── presentation/
│       │       ├── 📄 MainActivity.kt     # 主Activity
│       │       ├── 📄 Navigation.kt        # 底部导航
│       │       │
│       │       ├── base/
│       │       │   └── 📄 Theme.kt          # Compose主题
│       │       │
│       │       ├── chat/
│       │       │   ├── 📄 ChatScreen.kt      # 聊天界面
│       │       │   └── 📄 ChatViewModel.kt   # 聊天ViewModel
│       │       │
│       │       ├── settings/
│       │       │   ├── 📄 SettingsScreen.kt   # 设置界面
│       │       │   └── 📄 SettingsViewModel.kt # 设置ViewModel
│       │       │
│       │       └── memory/
│       │           ├── 📄 MemoryScreen.kt     # 记忆界面
│       │           └── 📄 MemoryViewModel.kt   # 记忆ViewModel
│       │
│       └── res/
│           └── values/
│               ├── 📄 colors.xml            # 颜色资源
│               ├── 📄 strings.xml           # 字符串资源
│               └── 📄 themes.xml            # 主题配置
│
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties     # Gradle Wrapper配置
```

---

## 🎯 各层职责说明

### 1. Presentation层 (表现层)
- **职责**: UI展示和用户交互
- **包含**: Activity、Screen、ViewModel
- **特点**: Compose声明式UI
- **文件**:
  - MainActivity.kt - 入口
  - Navigation.kt - 底部导航路由
  - ChatScreen + ChatViewModel - 聊天页面
  - SettingsScreen + SettingsViewModel - 设置页面
  - MemoryScreen + MemoryViewModel - 记忆页面
  - Theme.kt - 主题配色

### 2. Domain层 (领域层)
- **职责**: 业务逻辑和数据模型
- **包含**: Model、Repository接口、UseCase
- **特点**: 纯Kotlin，不依赖Android
- **文件**:
  - 数据模型: ChatMessage、CharacterConfig、Memory
  - 仓库接口: ChatRepository
  - 业务服务: MemoryService、HumanizeService

### 3. Data层 (数据层)
- **职责**: 数据获取和存储
- **包含**: Local本地、Remote远程、Repository实现
- **特点**: 数据源对Domain层透明
- **文件**:
  - Local: Room数据库 + DataStore
  - Remote: 各平台API
  - Repository: 仓库实现类

### 4. DI层 (依赖注入)
- **职责**: 对象创建和生命周期管理
- **包含**: Hilt Module
- **特点**: 单例注入
- **文件**: AppModule.kt

---

## 🔄 数据流说明

### 聊天数据流

```
用户输入
    ↓
ChatViewModel → sendMessage()
    ↓
ChatRepository
    ↓
    ├─→ 保存用户消息 (Room)
    ├─→ 加载人设配置 (DataStore)
    ├─→ 加载相关记忆 (MemoryService)
    ├─→ 构建上下文提示词
    └─→ 调用DeepSeek流式API
        ↓
流式收集SSE事件
    ↓
实时更新消息显示
    ↓
HumanizeService处理（去AI化
    ↓
更新最终消息
    ↓
[每10条消息触发记忆提取
```

### 配置数据流

```
用户输入配置
    ↓
SettingsViewModel → saveConfig()
    ↓
ChatRepository
    ↓
DataStore加密存储
    ↓
配置持久化
```

---

## 📊 核心类关系图

```
MainActivity
    ↓
Navigation (NavHost
    ├─→ ChatScreen ← ChatViewModel
    │       ↓
    │   ChatRepository
    │       ├── ChatMessageDao (Room)
    │       ├── AppPreferences (DataStore)
    │       ├── MemoryService
    │       ├── HumanizeService
    │       └── DeepSeekApi (OkHttp SSE)
    │
    ├─→ SettingsScreen ← SettingsViewModel
    │       ↓
    │   ChatRepository
    │       └── AppPreferences
    │
    └─→ MemoryScreen ← MemoryViewModel
            ↓
        ChatRepository
            └── MemoryService
                └── MemoryDao (Room)
```

---

## ✨ 项目特点总结

| 特性 | 说明
|------|-----
| **架构
| 架构模式 | MVVM + Clean Architecture
| 依赖注入 | Hilt
| 响应式 | Kotlin Flow
|
| **数据
| 本地数据库 | Room
| 配置存储 | DataStore
| 网络请求 | OkHttp + SSE
|
| **UI
| UI框架 | Jetpack Compose
| 导航 | Navigation Compose
| 图片加载 | Coil
|
| **功能
| 聊天API | DeepSeek V4流式
| 语音API | 火山引擎 ASR/TTS
| 图像API | 豆包图像生成
| 记忆系统 | 自动摘要+关联记忆
| 去AI化 | 5种风格+语气词

---

## 📏 代码统计

| 类型 | 文件数 | 预估行数
|------|--------|---------
| Kotlin源码 | 22个 | ~3500+
| 资源文件 | 6个 | ~200
| 配置文件 | 8个 | ~500
| 文档 | 7个 | ~5000+
| **总计** | **43个** | **~9200行**

---

## 🎓 学习要点

本项目涵盖了现代Android开发的最佳实践：

1. **架构清晰**: Clean Architecture分层
2. **现代UI**: Jetpack Compose声明式UI
3. **依赖注入**: Hilt全面应用
4. **响应式**: Kotlin Flow数据流
5. **本地存储**: Room + DataStore
6. **网络**: SSE流式协议
7. **性能**: 协程+正确线程切换
8. **安全**: 无硬编码，本地加密

---

**项目结构完整清晰！✅
